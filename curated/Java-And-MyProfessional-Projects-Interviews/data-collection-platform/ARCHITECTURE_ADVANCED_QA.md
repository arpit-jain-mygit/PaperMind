# Data Collection Platform - Advanced Q&A (Q9-Q17)

## Q10: QA Pitfalls & Mitigation

### 10.1 Testing the Extraction Pipeline

**Pitfall 1: Unit Tests Mock SparkAir → Production Fails**
```
Bad: Test with mocked SparkAir always succeeds
Good: Use contract testing + separate integration tests
```

```java
// Contract test (runs in CI but against mock Soniq service)
@Test
public void testExtractionServiceContract() {
  WireMock.stubFor(post(urlEqualTo("/api/extract"))
    .willReturn(aResponse()
      .withStatus(200)
      .withBody(json)
    )
  );
  
  ExtractedData result = extractionService.extract(document);
  assert result.getConfidence() >= 0.0;
  assert result.getConfidence() <= 1.0;
}

// Integration test (separate suite, runs nightly)
@Test
@Integration
public void testRealSparkAirIntegration() {
  // Uses real SparkAir test endpoint
  ExtractedData result = extractionService.extract(realPDF);
  assert result.getConfidence() > 0.5;
}
```

**Pitfall 2: No Test Coverage for Concurrency**
```java
// Bad: Single-threaded test passes, race condition in prod

// Good: Data-driven concurrency testing
@Test
public void testConcurrentDocumentApproval() throws Exception {
  Document doc = createTestDocument("sourced");
  
  ExecutorService executor = Executors.newFixedThreadPool(10);
  List<Future<?>> futures = new ArrayList<>();
  
  for (int i = 0; i < 10; i++) {
    futures.add(executor.submit(() -> {
      assertThrows(OptimisticLockingFailureException.class, () -> {
        documentService.approve(doc.getId(), "reviewer-" + UUID.randomUUID());
      });
    }));
  }
  
  executor.awaitTermination(5, TimeUnit.SECONDS);
  // Only 1 approval succeeds, others fail with optimistic locking
}
```

**Pitfall 3: No Negative Test Cases**

```java
// Bad: Only test happy path
// Good: Test all failure scenarios

@ParameterizedTest
@ValueSource(strings = {
  "corrupted.pdf",      // Invalid PDF
  "encrypted.pdf",      // Password-protected
  "tooLarge_500MB.pdf", // Exceeds 100MB limit
  "empty.pdf",          // No extractable text
  "corrupted_header.xlsx" // Malformed Excel
})
public void testInvalidDocuments(String filename) {
  Document doc = loadTestDocument(filename);
  
  assertThrows(InvalidDocumentException.class, () -> {
    extractionService.extract(doc);
  });
  
  // Verify document marked for manual review
  Document stored = mongoTemplate.findById(doc.getId(), Document.class);
  assertEquals(DocumentStatus.MANUAL_REQUIRED, stored.getStatus());
}
```

**Pitfall 4: No Chaos Testing**

```java
// Good: Simulate failures in test environment
@Test
public void testExtractionWithNetworkFailure() {
  // Use chaos engineering tools (e.g., toxiproxy)
  chaosProxy.addToxic("sparkair-latency", 
    new LatencyToxic()
      .stream(ToxicDirection.DOWNSTREAM)
      .latency(5000)  // Add 5s delay
  );
  
  // Extraction should timeout and fallback
  assertThrows(TimeoutException.class, () -> {
    extractionService.extract(largeDocument);
  });
  
  // Verify fallback to manual extraction
  Document stored = mongoTemplate.findById(largeDocument.getId(), Document.class);
  assertEquals(DocumentStatus.MANUAL_REQUIRED, stored.getStatus());
}
```

**Pitfall 5: Data Quality Validation Not Tested**

```java
@Test
public void testDataQualityRules() {
  ExtractedData data = new ExtractedData();
  data.setEntityName("ACME Corp");
  data.setConfidence(0.85);
  data.setAmount(null);  // Required field
  
  List<ValidationResult> results = qualityEngine.validate(data);
  
  assertTrue(results.stream()
    .anyMatch(r -> r.getRule().equals("REQUIRED_FIELD") 
                   && r.getField().equals("amount")));
}
```

### 10.2 Approval Workflow Testing

**Pitfall: Race Conditions Between L1 & L2**

```java
@Test
public void testL2ApprovalWhileL1Modifies() throws Exception {
  Document doc = createDocument("pending_review");
  
  CountDownLatch latch = new CountDownLatch(2);
  
  // Thread 1: L1 tries to modify
  Thread t1 = new Thread(() -> {
    try {
      documentService.updateExtractedData(doc.getId(), newData);
      fail("Should not allow modification in PENDING_REVIEW state");
    } catch (InvalidStateTransition e) {
      // Expected
    }
    latch.countDown();
  });
  
  // Thread 2: L2 approves
  Thread t2 = new Thread(() -> {
    try {
      documentService.approve(doc.getId(), "reviewer-1");
    } finally {
      latch.countDown();
    }
  });
  
  t1.start();
  t2.start();
  latch.await();
  
  // Verify final state
  Document stored = mongoTemplate.findById(doc.getId(), Document.class);
  assertEquals(DocumentStatus.APPROVED, stored.getStatus());
}
```

### 10.3 Integration Test Strategy

```yaml
# test-pyramid.yml - Allocation of test types
Unit Tests (70%):
  - Individual service methods
  - No external dependencies
  - Fast execution (~5 mins)

Contract Tests (15%):
  - Service boundaries
  - Mock external APIs (WireMock)
  - Verify request/response format
  - Run in CI pipeline (~10 mins)

Integration Tests (10%):
  - Real PostgreSQL, MongoDB, Kafka
  - Docker Compose setup
  - Nightly runs or manual trigger (~30 mins)

E2E Tests (5%):
  - Full workflow in staging
  - Manual extraction → approval
  - Pre-production validation
```

---

## Q11: Apache Spark, ELT, Data Lake Usage

### 11.1 Apache Spark Applications

**Use Case 1: Batch Data Quality Rules**
```
Raw Zone (Bronze) → Spark ETL → Quality Checks → Silver Zone
```

```scala
// Spark job to validate extracted data
val documentsDF = spark.read.mongodb("extracted_documents")

val qualityReport = documentsDF
  .filter($"status" === "EXTRACTION_COMPLETE")
  .withColumn("confidence_valid", 
    when($"confidence" >= 0.0 && $"confidence" <= 1.0, 1).otherwise(0))
  .withColumn("entity_valid", 
    when($"entityName".isNotNull && length($"entityName") > 0, 1).otherwise(0))
  .groupBy("extractionEngine")
  .agg(
    sum("confidence_valid") / count("*") as "quality_score",
    collect_list(when($"confidence_valid" === 0, $"_id")) as "failed_docs"
  )

qualityReport.write.mode("overwrite").parquet("s3://dcp-data-lake/quality-reports/")
```

**Use Case 2: Day-0 Bulk Load**

```scala
// Load 1M historical documents from legacy system
val legacyData = spark.read.option("header", "true").csv("s3://legacy/all-data.csv")

val transformed = legacyData
  .withColumn("sourceSystem", lit("LEGACY"))
  .withColumn("ingestedAt", current_timestamp())
  .withColumn("confidence", lit(0.95))  // Manual data, high confidence
  .repartition(200, $"ingestedDate")  // Partition by date for query optimization

transformed.write
  .mode("overwrite")
  .partitionBy("ingestedDate")
  .parquet("s3://dcp-data-lake/bronze/")
```

**Use Case 3: Reconciliation Reports**

```scala
// Compare extracted data vs manual validation
val extracted = spark.read.mongodb("extracted_documents")
  .where($"status" === "APPROVED")

val manual = spark.read.mongodb("manual_documents")
  .where($"status" === "APPROVED")

val reconciliation = extracted.join(manual, 
  extracted("entityName") === manual("entityName") && 
  extracted("month") === manual("month"),
  "full_outer")
  .withColumn("variance", $"extracted.amount" - $"manual.amount")
  .filter(abs($"variance") > 0.01)

reconciliation.write.parquet("s3://dcp-data-lake/reconciliation/")
```

### 11.2 ELT Pipeline

```
Extract (S3/Kafka) → Load (MongoDB/Parquet) → Transform (Spark)
                           ↓
                    Data Lake (Medallion Architecture)
```

**Bronze Layer** (Raw, unprocessed)
- All extracted documents
- Encryption key: document.sourceId
- Retention: 2 years
- Format: Parquet (for compression)

**Silver Layer** (Cleaned, deduplicated)
- Validated documents
- Entity mapping resolved
- Removed duplicates (by sourceId + hash)
- Quality score > 0.5
- Format: Parquet, partitioned by date/entity

**Gold Layer** (Business-ready)
- Aggregated financials by entity/period
- Derived metrics (confidence trends, extraction success rate)
- Pre-calculated reports
- Consumed by BI tools (Tableau, Power BI)

```scala
// Silver layer creation
val bronze = spark.read.parquet("s3://dcp-data-lake/bronze/")

val silver = bronze
  .dropDuplicates("sourceId", "documentHash")
  .filter($"quality_score" > 0.5)
  .join(entityMapping, "entityId")
  .select("documentId", "entityName", "amount", "currency", "date", "quality_score")
  .repartition(50, $"date")

silver.write.mode("overwrite")
  .partitionBy("date")
  .parquet("s3://dcp-data-lake/silver/")

// Gold layer creation (business aggregates)
val gold = silver
  .groupBy($"entityName", month($"date") as "month")
  .agg(
    sum($"amount") as "total_amount",
    avg($"quality_score") as "avg_confidence",
    count("*") as "document_count"
  )

gold.write.mode("overwrite")
  .parquet("s3://dcp-data-lake/gold/financial-summary/")
```

### 11.3 Data Lineage Tracking

```java
@Service
public class DataLineageService {
  
  public void recordTransformation(String documentId, 
                                   String sourceLayer,
                                   String targetLayer,
                                   String transformation) {
    DataLineageEvent event = DataLineageEvent.builder()
      .documentId(documentId)
      .sourceLayer(sourceLayer)  // bronze, silver, gold
      .targetLayer(targetLayer)
      .transformation(transformation)  // transformation SQL/job
      .timestamp(Instant.now())
      .build();
    
    mongoTemplate.insert(event, "data_lineage");
  }
  
  public List<DataLineageEvent> getLineage(String documentId) {
    // Trace document journey through data lake
    return mongoTemplate.find(
      new Query(Criteria.where("documentId").is(documentId))
                .with(Sort.by("timestamp")),
      DataLineageEvent.class
    );
  }
}
```

---

## Q12: Logical Architecture with Technologies

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                           │
├─────────────────────────────────────────────────────────────────┤
│  Angular UI (Data Entry, Review, Reports)                       │
│  - Login: OAuth 2.0                                             │
│  - State Mgmt: NgRx                                             │
│  - Websocket: Real-time notifications                           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│               API GATEWAY & SECURITY LAYER                      │
├─────────────────────────────────────────────────────────────────┤
│  Spring Cloud Gateway                                            │
│  - Authentication (JWT validation)                              │
│  - Rate Limiting (5 req/sec per user)                          │
│  - CORS, SSL termination                                        │
│  - Request logging, audit                                       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│         MICROSERVICES LAYER (Spring Boot)                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │ Sourcing    │  │ Extraction   │  │ Rules Engine │           │
│  │ Service     │  │ Service      │  │              │           │
│  ├─────────────┤  ├──────────────┤  ├──────────────┤           │
│  │ - S3 client │  │ - SparkAir   │  │ - Validation │           │
│  │ - Kafka src │  │ - Cognize    │  │ - Transform  │           │
│  │ - Email src │  │ - Deepmine   │  │ - Mapping    │           │
│  │ - Dedup     │  │ - OCR        │  │              │           │
│  └─────────────┘  └──────────────┘  └──────────────┘           │
│         ↓                  ↓                  ↓                   │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │ Workflow     │  │ Approval     │  │ Dissemination│           │
│  │ Service      │  │ Service      │  │ Service      │           │
│  ├──────────────┤  ├──────────────┤  ├──────────────┤           │
│  │ - Camunda    │  │ - L1/L2      │  │ - Publish    │           │
│  │ - State mgmt │  │ - Review     │  │ - Schedule   │           │
│  │ - Assign     │  │ - Reject     │  │ - Format     │           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│         MESSAGE QUEUE & EVENT BUS                               │
├─────────────────────────────────────────────────────────────────┤
│  Apache Kafka                                                    │
│  - Topics: sourcing, extraction, approval, dissemination        │
│  - Partitions: By documentId for ordering                       │
│  - Replication: 3 replicas                                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────┬──────────────────┬──────────────────────┐
│   DATA STORAGE       │   CACHING LAYER  │  EXTERNAL SYSTEMS    │
├──────────────────────┼──────────────────┼──────────────────────┤
│ PostgreSQL (metadata)│ Redis (session)  │ SparkAir API         │
│  - Users             │ Redis (cache)    │ Cognize API          │
│  - Templates         │ Hazelcast (dist) │ Soniq Entity Mapping │
│  - Taxonomy          │                  │ Delta Sharing        │
│  - Approval rules    │                  │ S3 (file storage)    │
│                      │                  │ Outlook/Gmail        │
│ MongoDB (documents)  │                  │ Elasticsearch        │
│  - Raw extracted     │                  │ (search)             │
│  - Pending review    │                  │                      │
│  - Approved          │                  │                      │
│                      │                  │                      │
│ Parquet (data lake)  │                  │ Splunk (logs)        │
│  - Bronze (raw)      │                  │ Prometheus (metrics) │
│  - Silver (cleaned)  │                  │                      │
│  - Gold (aggregate)  │                  │                      │
└──────────────────────┴──────────────────┴──────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│     PROCESSING ENGINE (Apache Spark)                            │
├─────────────────────────────────────────────────────────────────┤
│  - Batch ETL jobs (nightly)                                     │
│  - Data quality checks                                          │
│  - Aggregation & reporting                                      │
│  - Deduplication                                                │
└─────────────────────────────────────────────────────────────────┘
```

---

## Q13: Database Challenges

### 13.1 PostgreSQL Challenges

**Challenge 1: Connection Pool Exhaustion**
```
Problem: 500+ concurrent users × 3 DB connections each = needs 1500+ connections
Solution: HikariCP with max 50 connections + request queuing
```

```properties
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

**Challenge 2: Large Table Scans**

```sql
-- Bad: Selecting all 10M documents
SELECT * FROM documents WHERE status = 'PENDING_REVIEW';

-- Good: Partitioning + indexes
CREATE TABLE documents_2024 PARTITION OF documents
  FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');

CREATE INDEX idx_documents_status_date 
  ON documents(status, created_at DESC);

SELECT * FROM documents 
  WHERE status = 'PENDING_REVIEW' 
  AND created_at > CURRENT_DATE - INTERVAL '30 days';
```

**Challenge 3: Approval Workflow Deadlocks**

```sql
-- Risk: Two transactions lock same rows in different order
UPDATE documents SET status = 'APPROVED' WHERE id = 1;
UPDATE workflow_state SET current_stage = 'APPROVED' WHERE doc_id = 1;

-- Good: Consistent locking order
BEGIN;
  SELECT * FROM documents WHERE id = 1 FOR UPDATE;  -- Lock 1st
  SELECT * FROM workflow_state WHERE doc_id = 1 FOR UPDATE;  -- Lock 2nd
  UPDATE documents SET status = 'APPROVED' WHERE id = 1;
  UPDATE workflow_state SET current_stage = 'APPROVED' WHERE doc_id = 1;
COMMIT;
```

### 13.2 MongoDB Challenges

**Challenge 1: Document Size Limits (16MB)**

```
Problem: Large extracted documents exceed 16MB limit
Solution: Split into sub-documents
```

```java
@Document
public class ExtractedDocument {
  String id;
  String documentId;
  int partNumber;  // Document 1 of 3
  List<ExtractedField> fields;
  LocalDateTime createdAt;
}

// Query all parts
List<ExtractedDocument> parts = mongoTemplate.find(
  new Query(Criteria.where("documentId").is("doc-123"))
    .with(Sort.by("partNumber")),
  ExtractedDocument.class
);
```

**Challenge 2: Transaction Complexity**

```
Problem: MongoDB transactions across multiple collections need careful handling
Solution: Use event sourcing instead of distributed transactions
```

```java
@Service
public class DocumentApprovalService {
  
  @Transactional
  public void approveDocument(String documentId) {
    // Single document update (atomic)
    mongoTemplate.updateFirst(
      new Query(Criteria.where("_id").is(documentId)),
      new Update()
        .set("status", "APPROVED")
        .set("approvedAt", Instant.now())
        .set("approver", getCurrentUser()),
      Document.class
    );
    
    // Publish event (separate operation, okay if fails)
    kafkaTemplate.send("approval-events", 
      new ApprovalEvent(documentId, "APPROVED"));
  }
}
```

**Challenge 3: Indexing for Query Performance**

```java
@Document
@Indexed  // Create index on this field
public class ExtractedData {
  @Id
  String id;
  
  @Indexed
  String documentId;
  
  @Indexed(expireAfterSeconds = 86400*30)  // TTL: 30 days
  LocalDateTime createdAt;
  
  @Indexed
  Double confidence;
}

// Compound index for common query
db.extracted_data.createIndex(
  { "documentId": 1, "status": 1, "createdAt": -1 }
);
```

### 13.3 Polyglot Persistence Strategy

| Data Type | Store | Reason |
|-----------|-------|--------|
| Metadata (templates, taxonomy, users) | PostgreSQL | Strong ACID, complex queries |
| Extracted documents | MongoDB | Flexible schema, horizontal scaling |
| Time-series metrics | InfluxDB | Optimized for time queries |
| Search (entity names) | Elasticsearch | Full-text search capabilities |
| Session state | Redis | Fast, TTL support |
| Data lake | Parquet (S3) | Cost-effective, Spark integration |

---

## Q15: Event-Driven Architecture Use Cases

### 15.1 Applicable Scenarios

**Use Case 1: Document State Transitions**
```
SOURCED → EXTRACTION_TRIGGERED → EXTRACTED → PENDING_REVIEW → APPROVED
   ↓           ↓                      ↓            ↓              ↓
Publish   Update queue      Notify L2 users  Auto-escalate  Publish final
```

```java
@Service
public class DocumentEventPublisher {
  
  public void publishDocumentEvent(String documentId, DocumentStatus newStatus) {
    DocumentStateChangeEvent event = new DocumentStateChangeEvent(
      documentId, newStatus, Instant.now()
    );
    kafkaTemplate.send("document-events", event);
  }
}

// Multiple subscribers listen independently
@Service
@KafkaListener(topics = "document-events")
public class L2UserNotificationService {
  
  @PayloadParam
  public void onDocumentReady(DocumentStateChangeEvent event) {
    if (event.getStatus() == DocumentStatus.PENDING_REVIEW) {
      notificationService.sendToL2Users(event.getDocumentId());
    }
  }
}
```

**Use Case 2: Approval Escalation**
```
Event: Document pending review > 4 hours
Action: Auto-assign to different L2 user + send alert
```

```java
@Service
public class EscalationService {
  
  @Scheduled(fixedDelay = 300000)  // Every 5 mins
  public void checkPendingEscalation() {
    List<Document> stale = mongoTemplate.find(
      new Query(Criteria.where("status").is("PENDING_REVIEW")
                                       .and("createdAt")
                                       .lt(Instant.now().minus(4, ChronoUnit.HOURS))),
      Document.class
    );
    
    stale.forEach(doc -> {
      kafkaTemplate.send("escalation-events",
        new EscalationEvent(doc.getId(), "REASSIGN_TO_SUPERVISOR")
      );
    });
  }
}
```

**Use Case 3: Real-time Quality Metrics**
```
Events: extraction_complete → quality_check → aggregate metrics
Result: Dashboard shows live extraction quality in real-time
```

**Use Case 4: Audit Trail**
```
Every action (extract, approve, reject, modify) publishes event
→ Immutable event log in Kafka
→ Reconstruct document history anytime
```

### 15.2 Event-Driven vs Request-Response

| Use Case | Pattern | Why |
|----------|---------|-----|
| Manual extraction | Sync (request-response) | User waiting for response |
| AI extraction | Async (event-driven) | Long-running, workers scale independently |
| Approval notification | Async (event-driven) | L2 user doesn't block requester |
| Data dissemination | Async (event-driven) | Batch exports, multiple destinations |
| Soniq entity lookup | Sync (with cache) | Required during extraction |
| Dashboard metrics | Async (event stream) | Real-time aggregation |

---

## Q16: Non-Functional Requirements - Pitfalls & Realistic Achievement

### 16.1 Performance NFR: <2 sec UI Response

**Pitfall:** Assuming all backend calls complete in <1 sec
```
Reality: DB queries (50ms) + cache miss (200ms) + network (100ms) = 350ms
But if cache cold and DB slow: 50ms + 200ms + 300ms + 100ms = 650ms (under budget)
If concurrent queries cause lock contention: 650ms + 500ms lock wait = way over!
```

**Realistic Achievement:**
- P50: 800ms (optimal conditions)
- P95: 1.5s (normal load)
- P99: 2.5s (peak load, acceptable for occasional)
- **Accept occasional breaches** (e.g., "2s for 95% of requests")

**Implementation:**
```java
@Configuration
public class PerformanceMonitoring {
  
  @Bean
  public MeterBinder apiLatencyMetrics() {
    return (registry) -> {
      Timer timer = Timer.builder("api.response.time")
        .publishPercentiles(0.5, 0.95, 0.99)
        .serviceLevelObjectives(1000, 1500, 2000)  // SLOs in ms
        .register(registry);
    };
  }
}
```

### 16.2 Availability NFR: 99.5% Uptime

**Pitfall:** "99.5% = 4 hours downtime/month"
```
Reality: 
- Database maintenance: 2 hours
- Kubernetes upgrade: 1 hour
- Emergency bug fix: 0.5 hours
- Unplanned outage: ?? (can't predict)
= Already at limit before any real incident!
```

**Realistic Achievement:**
- Plan for 99.0% (7 hours downtime/month)
- Stretch goal: 99.5% with discipline
- Require: Automated failover, blue-green deployments, canary releases

```yaml
# Kubernetes Rolling Update for zero-downtime
apiVersion: apps/v1
kind: Deployment
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0       # Always have min replicas running
      maxSurge: 2             # Can temporarily run 2 extra pods
  replicas: 5
  minReadySeconds: 30        # Wait 30s after pod starts to mark ready
  progressDeadlineSeconds: 600  # If update takes >10m, rollback
```

### 16.3 Data Quality NFR: <1% Quality Issues

**Pitfall:** "Ensure 99% accuracy" without defining accuracy
```
Accuracy of what?
- Field extraction? (extractedValue == expected value) → 85% realistic
- Entity mapping? (mapped correctly to Soniq ID) → 90% realistic
- Amount correctness? (±0.01% tolerance) → 95% realistic
- All fields correct? (0 errors in document) → 60% realistic
```

**Realistic Achievement:**
- Define per-field SLA (Amount: 98%, Entity: 92%, Date: 99%)
- Confidence scoring as quality proxy
- Implement human-in-the-loop for low-confidence extractions
- Flag outliers (±3 standard deviations) for review

```python
# Quality scoring logic
def calculate_quality_score(extracted_doc, validation_rules):
  score = 0
  penalties = []
  
  # Field-level accuracy
  for field, expected in validation_rules.items():
    if extracted_doc[field] == expected:
      score += 10
    elif is_within_tolerance(extracted_doc[field], expected):
      score += 5
      penalties.append(f"{field}: value mismatch")
    else:
      penalties.append(f"{field}: critical error")
  
  # Entity mapping validation
  soniq_id = extracted_doc["soniq_id"]
  if not soniq_id or soniq_id not in entityMappingCache:
    penalties.append("entity_not_mapped")
    score *= 0.8
  
  # Confidence score (from AI model)
  ai_confidence = extracted_doc["confidence"]
  if ai_confidence < 0.7:
    penalties.append(f"low_confidence: {ai_confidence}")
    score *= 0.9
  
  return min(score, 100), penalties
```

### 16.4 Security NFR: Encrypt Sensitive Data

**Pitfall:** "Use AES-256 encryption" → unspecified where/how
```
At rest: Encrypt in MongoDB? In S3? In transit to Spark?
In flight: TLS 1.2? TLS 1.3? What about internal service calls?
Key management: Who rotates keys? How often? What's the fallback?
```

**Realistic Achievement:**

```java
// At rest encryption
@Configuration
public class EncryptionConfiguration {
  
  @Bean
  public MongoEncryptionProperties mongoEncryption() {
    return MongoEncryptionProperties.builder()
      .keyVaultNamespace("dcp", "encryption-keys")
      .masterKeyProvider(ClientSideFLEOptions.builder()
        .keyVaultMongoClient(mongoClient)
        .build()
      )
      .build();
  }
}

// In flight encryption (Spring Security)
@Configuration
@EnableWebSecurity
public class SecurityConfig {
  
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) {
    http.requiresChannel()
      .anyRequest()
      .requiresSecure();  // Force HTTPS
    
    http.headers()
      .contentSecurityPolicy("default-src 'self'");
    
    return http.build();
  }
}

// Key rotation (quarterly)
@Scheduled(cron = "0 0 1 1,4,7,10 * ?")  // 1st day of Q
public void rotateEncryptionKeys() {
  encryptionService.rotateDataEncryptionKeys();
  // Old key kept for decryption of historical data
}
```

### 16.5 Scalability NFR: Support 500+ Concurrent Users

**Pitfall:** "Add more servers" → infinite cost
```
Reality: Each extraction worker costs money
If 500 users × 1 extraction request pending = expensive
```

**Realistic Achievement:**

```
Concurrent Users | Active Extractions | Worker Pods | Cost/Month
500              | 50 (10% active)    | 5-10 (auto) | $2K
500              | 100 (20% active)   | 10-20      | $3K
500              | 200 (40% active)   | 20-50      | $5K
```

**Mitigation:**
- Queue management: Show estimated wait time
- Resource pooling: Share extraction workers across tenants
- Rate limiting: 2 concurrent extractions per user

---

## Q17: Orchestration vs Choreography

### 17.1 Document Workflow Decision

**Scenario:** Sourcing → Extraction → Quality Check → Approval → Dissemination

```
                ORCHESTRATION
                (Centralized Camunda)
                
    Camunda → [1. Source] → [2. Extract] → [3. Quality] 
                                                   ↓
    Camunda ← [Approve] ← [Review] ← [Notify L2]

vs

                CHOREOGRAPHY
                (Event-driven, Decentralized)
                
    Kafka Event: "document.sourced"
         ↓
    Extraction Service listens → publishes "extraction.completed"
         ↓
    Quality Service listens → publishes "quality.passed" or "quality.failed"
         ↓
    Approval Service listens → publishes "document.approved"
         ↓
    Dissemination Service listens → publishes "data.shared"
```

### 17.2 Comparison Matrix

| Aspect | Orchestration | Choreography |
|--------|---|---|
| **Visibility** | Complete flow in Camunda UI ✅ | Events scattered across topics ❌ |
| **Debugging** | Easy: follow Camunda flow | Hard: reconstruct from events |
| **Failure Handling** | Centralized compensation logic | Must implement per service |
| **Coupling** | Services know Camunda | Services only know events (loose) ✅ |
| **Scalability** | Camunda becomes bottleneck ❌ | Linearly scales with services ✅ |
| **Latency** | Synchronous (higher) | Asynchronous (lower) ✅ |
| **Overhead** | Moderate (Camunda CPU) | Minimal ✅ |

### 17.3 Recommended Hybrid Approach

```
                    HYBRID SOLUTION
                    
Use Camunda for:
  - Complex approval workflows (conditional routing, human tasks)
  - Long-running processes with many states
  
Use Choreography for:
  - Fast, event-driven reactions (sourcing → extraction)
  - Real-time notifications
  - Data quality checks
  
Integration:
  Camunda process → publishes to Kafka
  Event listeners → update Camunda via REST
```

```java
@Service
public class WorkflowOrchestration {
  
  // Camunda handles approval workflow
  @PostMapping("/workflows/approval")
  public void startApprovalWorkflow(String documentId) {
    processEngine.getRuntimeService()
      .createProcessInstanceByKey("approval-workflow")
      .setVariable("documentId", documentId)
      .start();
  }
  
  // Kafka handles fast extraction pipeline
  @KafkaListener(topics = "document-sourced")
  public void onDocumentSourced(SourcedEvent event) {
    extractionService.extract(event.getDocumentId());
    // No Camunda involved, fast and async
  }
}
```

### 17.4 Process Definition (Camunda)

```xml
<bpmn:process id="approval-workflow" isExecutable="true">
  
  <!-- Start: Document submitted -->
  <bpmn:startEvent id="start-approval" name="Document Ready for Review">
    <bpmn:outgoing>flow-to-assign</bpmn:outgoing>
  </bpmn:startEvent>
  
  <!-- Assign to L2 user -->
  <bpmn:serviceTask id="assign-reviewer" name="Assign L2 Reviewer">
    <bpmn:incoming>flow-to-assign</bpmn:incoming>
    <bpmn:outgoing>flow-to-review</bpmn:outgoing>
  </bpmn:serviceTask>
  
  <!-- Human review task -->
  <bpmn:userTask id="review-document" name="L2 Review Document">
    <bpmn:incoming>flow-to-review</bpmn:incoming>
    <bpmn:outgoing>flow-to-decision</bpmn:outgoing>
    <bpmn:potentialAssignee>${assignedReviewerId}</bpmn:potentialAssignee>
  </bpmn:userTask>
  
  <!-- Decision point -->
  <bpmn:exclusiveGateway id="approval-decision" name="Approved?">
    <bpmn:incoming>flow-to-decision</bpmn:incoming>
    <bpmn:outgoing>flow-approved</bpmn:outgoing>
    <bpmn:outgoing>flow-rejected</bpmn:outgoing>
  </bpmn:exclusiveGateway>
  
  <!-- Approved path -->
  <bpmn:serviceTask id="publish-approved" name="Publish Approval Event">
    <bpmn:incoming>flow-approved</bpmn:incoming>
    <bpmn:outgoing>flow-to-end</bpmn:outgoing>
  </bpmn:serviceTask>
  
  <!-- Rejected path -->
  <bpmn:serviceTask id="notify-rejection" name="Notify Requester">
    <bpmn:incoming>flow-rejected</bpmn:incoming>
    <bpmn:outgoing>flow-to-end</bpmn:outgoing>
  </bpmn:serviceTask>
  
  <!-- End -->
  <bpmn:endEvent id="end-approval">
    <bpmn:incoming>flow-to-end</bpmn:incoming>
  </bpmn:endEvent>
  
</bpmn:process>
```

---

## Q18: Interview Q&A by Role & Technology

### 18.1 SAFe ART Lead Architect Interview Questions

**Q: How would you structure this platform for 5 teams working in parallel?**

A: 
- Team 1: Sourcing & Ingest (S3, Email, Kafka)
- Team 2: Extraction Services (SparkAir integration, quality checks)
- Team 3: Approval Workflow (Camunda, L1/L2 logic)
- Team 4: Data Lake & Analytics (Spark ETL, reporting)
- Team 5: Platform & Operations (DevOps, observability)

Each team owns their microservice end-to-end (code, tests, deployment).

**Q: How do you manage dependencies between teams?**

A:
- API contracts (AsyncAPI for Kafka, OpenAPI for REST)
- Contract testing in CI pipeline
- Shared services (PostgreSQL, Kafka) managed by Platform team
- Feature flags for coordinated releases

**Q: What's your strategy for distributed tracing across 5 microservices?**

A:
- Spring Cloud Sleuth + Jaeger for distributed tracing
- Correlation ID propagated across service calls
- Trace visualization in Jaeger UI
- Alert on P99 latency > 2s

**Q: How do you handle a critical bug that needs coordination across 3 teams?**

A:
- Hotfix branch from main
- All 3 teams build & deploy in parallel (independence)
- Blue-green deployment for zero-downtime rollout
- Automatic rollback if health checks fail

### 18.2 Senior Developer / IC Level Questions

**Spring Boot Specialist:**

Q: Design the extraction service to handle 100MB PDFs without OOM.

A:
```java
@Service
public class StreamingExtractionService {
  
  public void extractLargeDocument(InputStream pdfStream, String documentId) {
    // Stream-based processing, never load full PDF in memory
    
    BufferedInputStream buffered = new BufferedInputStream(pdfStream, 8192);
    PDDocument document = PDDocument.load(buffered);  // Lazy loading
    
    PDFTextStripper stripper = new PDFTextStripper();
    
    for (int i = 1; i <= document.getNumberOfPages(); i++) {
      stripper.setStartPage(i);
      stripper.setEndPage(i);
      
      String pageText = stripper.getText(document);
      
      // Process page independently
      ExtractedPage page = processPage(pageText, i);
      mongoTemplate.insert(page, "extracted_pages");
      
      // Clear reference
      page = null;
    }
    
    document.close();
  }
}
```

Q: Implement idempotent Kafka message processing.

A:
```java
@Service
@KafkaListener(topics = "extraction-requests")
public class ExtractionWorker {
  
  public void handleExtractionRequest(ExtractionJob job) {
    // Idempotency key: document ID + version
    String idempotencyKey = job.getDocumentId() + "-v" + job.getVersion();
    
    // Check if already processed
    if (idempotencyStore.get(idempotencyKey) != null) {
      log.info("Skipping duplicate: {}", idempotencyKey);
      return;  // Idempotent: safe to skip
    }
    
    try {
      // Do work
      ExtractedData data = sparkairClient.extract(job);
      mongoTemplate.insert(data, "extracted_documents");
      
      // Mark as processed
      idempotencyStore.put(idempotencyKey, data.getId());
      
    } catch (TransientException e) {
      // Transient error: don't mark as processed, allow retry
      throw e;
    } catch (PermanentException e) {
      // Permanent error: mark and move to DLQ
      idempotencyStore.put(idempotencyKey, "ERROR: " + e.getMessage());
      kafkaTemplate.send("extraction-dlq", job);
    }
  }
}
```

**PostgreSQL Specialist:**

Q: How do you optimize the approval workflow query that scans 10M documents?

A:
```sql
-- Problem: Full table scan
SELECT COUNT(*) FROM documents WHERE status = 'PENDING_REVIEW';

-- Solution: Composite index + partition pruning
CREATE INDEX idx_status_created 
  ON documents(status, created_at DESC) 
  WHERE status IN ('PENDING_REVIEW', 'EXTRACTION_COMPLETE');

-- Leverage partition (if date-partitioned)
SELECT COUNT(*) FROM documents_2024 
  WHERE status = 'PENDING_REVIEW' 
  AND created_at > CURRENT_DATE - INTERVAL '7 days';

-- Result: Index seek instead of table scan (100x faster)
```

Q: Design a solution for concurrent approval that prevents race conditions.

A:
```sql
-- Option 1: Pessimistic locking
BEGIN;
  SELECT * FROM documents WHERE id = 123 FOR UPDATE NOWAIT;
  UPDATE documents SET status = 'APPROVED' WHERE id = 123 AND version = 5;
COMMIT;

-- Option 2: Optimistic locking (preferred)
UPDATE documents 
SET status = 'APPROVED', version = version + 1
WHERE id = 123 
  AND status = 'PENDING_REVIEW'
  AND version = 5;  -- Only update if version hasn't changed

-- Check if update was successful
IF ROW_COUNT = 0 THEN
  RAISE EXCEPTION 'Conflict: Document was modified elsewhere';
END IF;
```

**Kubernetes / DevOps:**

Q: Design auto-scaling for the extraction worker service.

A:
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: extraction-worker-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: extraction-worker
  minReplicas: 5
  maxReplicas: 50
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  - type: Pods
    pods:
      metric:
        name: kafka_consumer_lag_sum
        selector:
          matchLabels:
            topic: "extraction-requests"
      target:
        type: AverageValue
        averageValue: "30000"  # Scale if avg lag > 30K messages per pod
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50  # Scale down at most 50% at a time
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100  # Double size
        periodSeconds: 30
      - type: Pods
        value: 4  # Add 4 pods
        periodSeconds: 30
      selectPolicy: Max  # Pick whichever is larger
```

Q: Implement blue-green deployment for zero-downtime release.

A:
```bash
#!/bin/bash
# Deploy to "green" cluster while "blue" handles traffic

# 1. Deploy new version to green
kubectl set image deployment/extraction-worker-green \
  extraction-worker=acr.azurecr.io/extraction-worker:v1.2.3

# 2. Wait for green to be ready
kubectl wait --for=condition=Available --timeout=600s \
  deployment/extraction-worker-green

# 3. Run smoke tests against green
./run-smoke-tests.sh http://extraction-worker-green:8080

if [ $? -eq 0 ]; then
  # 4. Switch traffic from blue to green
  kubectl patch service extraction-worker \
    -p '{"spec":{"selector":{"version":"green"}}}'
  
  # 5. Keep blue as instant rollback
  # If issues detected, immediately: kubectl patch ... version: blue
else
  echo "Smoke tests failed, rolling back"
  exit 1
fi
```

### 18.3 Apache Spark Specialist

Q: Design a Spark job to deduplicate 1B documents across multiple data sources.

A:
```scala
def deduplicateDocuments(): Unit = {
  val spark = SparkSession.builder()
    .appName("document-deduplication")
    .getOrCreate()
  
  // Read from multiple sources
  val s3Docs = spark.read.parquet("s3://dcp/bronze/documents/")
  val mongoData = spark.read.format("mongodb")
    .option("database", "dcp")
    .option("collection", "extracted_documents")
    .load()
  
  // Combine and deduplicate
  val allDocs = s3Docs.unionByName(mongoData)
  
  // Group by content hash + document size (approximate dedup)
  val deduplicated = allDocs
    .withColumn("content_hash", sha2(col("content"), 256))
    .withColumn("size_bucket", floor(col("size") / 1000) * 1000)
    .dropDuplicates("content_hash", "size_bucket")
    .repartition(1000)  // For parallel write
  
  // Write deduplicated to silver layer
  deduplicated.write
    .mode("overwrite")
    .parquet("s3://dcp/silver/documents-dedup/")
  
  // Log statistics
  println(s"Original: ${allDocs.count()}")
  println(s"Deduplicated: ${deduplicated.count()}")
}
```

Q: Optimize a Spark job processing 1TB of data in 4 hours.

A:
```scala
spark.conf.set("spark.sql.adaptive.enabled", "true")  // Query optimization
spark.conf.set("spark.sql.adaptive.coalescePartitions.enabled", "true")

// Use columnar formats (Parquet) over row-based (CSV)
val documents = spark.read.parquet("s3://dcp/bronze/")
  // Projection pushdown: only read required columns
  .select("documentId", "amount", "entityName", "date")
  // Predicate pushdown: filter at read time
  .filter(col("date") >= "2024-01-01")

// Cache intermediate results for reuse
documents.cache()

// Proper partitioning for joins
val entityMappings = spark.read.parquet("s3://dcp/entity-mappings/")
  .repartition(100, col("entityName"))

val joined = documents.repartition(100, col("entityName"))
  .join(entityMappings, Seq("entityName"), "left")

// Broadcast small dimensions
val amountRanges = spark.read.parquet("s3://amount-ranges/").collect()
val bcRanges = spark.broadcast(amountRanges)

joined.mapPartitions { partition =>
  val ranges = bcRanges.value
  partition.map { row =>
    val amountBucket = ranges.find(_.contains(row.getDouble("amount")))
    (row, amountBucket)
  }
}

joined.write.mode("overwrite").parquet("s3://dcp/silver/processed/")

println(s"Job completed in ${System.currentTimeMillis() - start} ms")
```

---

### 18.4 Architecture & System Design

Q: Design a real-time dashboard showing extraction metrics (success rate, confidence, entities processed).

A:
```
Real-time Pipeline:
  Kafka (extraction events)
    ↓
  Flink / Spark Streaming (aggregate per minute)
    ↓
  Time-series DB (InfluxDB)
    ↓
  WebSocket API (push to dashboard)
    ↓
  Angular Dashboard (real-time chart)
```

```java
@Configuration
public class MetricsStreamingConfig {
  
  @Bean
  public KafkaStream<String, ExtractionEvent> metricsStream(
      StreamsBuilder streamsBuilder) {
    
    KStream<String, ExtractionEvent> events = 
      streamsBuilder.stream("extraction-events");
    
    // Aggregate per minute
    KStream<Windowed<String>, ExtractionMetrics> metrics =
      events.groupByKey()
        .windowedBy(TimeWindows.of(Duration.ofMinutes(1)))
        .aggregate(
          ExtractionMetrics::new,
          (key, event, metrics) -> {
            metrics.recordExtraction(event);
            return metrics;
          },
          Materialized.as("metrics-store")
        )
        .toStream();
    
    // Publish to WebSocket topic
    metrics.to("metrics-websocket");
    
    return events;
  }
}

@RestController
public class MetricsController {
  
  @GetMapping("/metrics/live")
  public SseEmitter streamMetrics() {
    SseEmitter emitter = new SseEmitter();
    
    kafkaListener.listen("metrics-websocket", event -> {
      try {
        emitter.send(event);
      } catch (IOException e) {
        emitter.completeWithError(e);
      }
    });
    
    return emitter;
  }
}
```

End of Q&A document. Continue for system diagrams and additional resources...
