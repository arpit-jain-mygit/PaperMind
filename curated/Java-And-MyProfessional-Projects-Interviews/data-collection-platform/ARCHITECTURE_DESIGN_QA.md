# Data Collection Platform - Architecture Design Q&A

## Q2: Retry Strategy - Areas Requiring Retry

### 2.1 Sourcing Stage
**Why retry?** External sources (S3, Outlook) may be temporarily unavailable.

```
Scenario                    | Retry Policy              | Max Attempts | Backoff
----------------------------|--------------------------|--------------|---------
S3 file download fails      | Exponential (2^n * 1s)  | 5            | 1s, 2s, 4s, 8s, 16s
Email connector timeout     | Linear (n * 2s)         | 3            | 2s, 4s, 6s
Database connection fails   | Exponential              | 4            | 500ms intervals
Kafka producer timeout      | Exponential (jitter)    | 3            | 100ms base
```

**Implementation Pattern:**
```java
@Transient
@Retryable(
  value = IOException.class,
  maxAttempts = 5,
  backoff = @Backoff(delay = 1000, multiplier = 2.0)
)
public void downloadSourceFile(String sourceId) {
  // retry logic with circuit breaker
}
```

### 2.2 Extraction Stage
**Why?** External AI services (SparkAir, Cognize) may throttle or fail.

- **Status**: Poison pill if fails after retries
- **Fallback**: Queue for manual entry
- **Metrics**: Track extraction success rate per vendor

### 2.3 Processing & Transformation
- **Rules engine failures**: Retry with different rule version
- **Data quality checks**: Quarantine → manual review queue
- **Spark jobs**: Automatic rerun with cluster restart

### 2.4 Dissemination
- **API delivery failures**: Exponential backoff (max 7 days retention)
- **File export failures**: Retry with different compression/format
- **Queue publishing**: Idempotent operations with deduplication key

### 2.5 Database Operations
- **Write conflicts**: Pessimistic locking or optimistic versioning
- **Connection pool exhaustion**: Circuit breaker + queue backlog

**Bad Design:** Fixed retry with same parameters → cascading failures
**Good Design:** Adaptive retry with jitter + circuit breaker + dead letter queue

---

## Q3: Design Patterns Across All Levels

### 3.1 Gang of Four Patterns

#### Creational
| Pattern | Usage | Example |
|---------|-------|---------|
| **Factory** | Create extraction engines (SparkAir, Cognize) | `ExtractionEngineFactory.getEngine("SparkAir")` |
| **Builder** | Complex extraction request construction | `ExtractionRequest.builder().withTemplate(...).build()` |
| **Singleton** | Configuration loader, Soniq client | `ConfigService.getInstance()` |
| **Prototype** | Clone extraction templates for variation testing | `template.clone().withModifiedRules()` |

#### Structural
| Pattern | Usage | Example |
|---------|-------|---------|
| **Adapter** | Normalize different file parsers (PDF, Excel, CSV) | `FileParserAdapter` wraps vendor parsers |
| **Facade** | Simplify complex extraction pipeline | `DataExtractionFacade` orchestrates sourcing→extraction→storage |
| **Decorator** | Add logging, caching, retry to extraction service | `CachedExtractionDecorator(BaseExtractionService)` |
| **Proxy** | Lazy load Soniq entity mapping | `LazyLoadingEntityProxy` |
| **Bridge** | Decouple extraction algorithm from data source type | `ExtractionAlgorithm` ↔ `DataSourceAdapter` |

#### Behavioral
| Pattern | Usage | Example |
|---------|-------|---------|
| **Strategy** | Multiple extraction strategies (manual, SparkAir, Cognize) | `ExtractionStrategy` interface + implementations |
| **State** | Document workflow states (sourced→extracted→reviewed→approved) | `DocumentState` state machine |
| **Observer** | Notify downstream services of document state change | `DocumentStateChangeListener` |
| **Chain of Responsibility** | Data quality validation chain | `ValidationRule1 → ValidationRule2 → ValidationRule3` |
| **Template Method** | Common extraction steps with algorithm variations | `AbstractExtractionTemplate` |
| **Command** | Encapsulate extraction jobs for queuing | `ExtractionCommand` + `CommandQueue` |
| **Iterator** | Traverse large document batches | `DocumentBatchIterator` |

### 3.2 Microservices Patterns

| Pattern | Implementation | Benefit |
|---------|---|---|
| **API Gateway** | Spring Cloud Gateway | Single entry point, auth, rate limiting |
| **Service Discovery** | Kubernetes DNS / Consul | Dynamic service registration |
| **Circuit Breaker** | Resilience4j, Hystrix | Prevent cascading failures (SparkAir timeout) |
| **Bulkhead** | Thread pool isolation | Limit threads per extraction engine |
| **Saga (Orchestrated)** | Apache Camunda | Multi-service transaction (sourcing→extraction→approval) |
| **CQRS** | Separate read model for reporting | Query optimization |
| **Event Sourcing** | Store document state changes as events | Complete audit trail, event replay |
| **Strangler Fig** | Gradually replace legacy extraction system | Zero-downtime migration |

### 3.3 Enterprise Patterns

| Pattern | Usage |
|---------|-------|
| **Message Queue (Async)** | Kafka for sourcing → extraction → dissemination decoupling |
| **Dead Letter Queue** | Poison pills from extraction failures |
| **Idempotency Key** | Prevent duplicate document processing |
| **Request-Reply** | Synchronous extraction requests with timeout |
| **Publish-Subscribe** | Document approval events to multiple subscribers |
| **Data Lake** | Bronze (raw) → Silver (cleaned) → Gold (curated) |
| **Master Data Mgmt** | Entity mapping via Soniq |

### 3.4 UI Patterns (Angular)

| Pattern | Usage |
|---------|-------|
| **Container/Presentational** | Smart `DocumentListContainer` wraps dumb `DocumentCard` |
| **State Management** | NgRx for document work queue state |
| **Smart Forms** | Reactive forms with dynamic field generation from templates |
| **Progressive Enhancement** | Show confidence scores alongside extracted data |
| **Infinite Scroll** | Large document lists |

---

## Q4: Performance Requirements

### 4.1 Response Time SLAs
```
Use Case                    | Target P95    | Target P99    | Rationale
----------------------------|---------------|---------------|-----------
UI Page Load                | <1s           | <2s           | User experience
Data Entry (per field)      | <500ms        | <1s           | Form responsiveness
List 1000 documents         | <2s           | <3s           | Pagination/virtual scroll
Manual extraction (per doc) | 30s-2m        | 5m            | Human task
AI extraction (per doc)     | 30s (100MB)   | 5m            | Async + poll status
Batch report generation     | <5m (10K docs)| <10m          | Overnight jobs
Search by entity name       | <500ms        | <1s           | Elasticsearch
Approval workflow           | <2s           | <3s           | Critical path
```

### 4.2 Throughput Targets
- **Concurrent Users**: 500 (50 simultaneous extractions)
- **Documents/Day**: 10,000 → 115 docs/hour → ~2 docs/minute steady state
- **Kafka**: 1,000 events/second (500 sourcing + 300 extraction + 200 approval)
- **Database**: 1,500 ops/second (reads), 500 ops/second (writes)
- **API Calls**: 5,000 req/sec at peak

### 4.3 Resource Allocation
```
Service              | CPU (min/max)  | Memory (min/max) | Replicas | Reason
---------------------|----------------|------------------|----------|--------
API Gateway          | 0.5/2          | 512M/1G          | 3        | Load distribution
Extraction Workers   | 2/8            | 2G/4G            | 5-20*    | Auto-scale on queue depth
Rules Engine         | 0.5/1          | 512M/1G          | 2        | Low CPU
Approval Workflow    | 0.5/1          | 256M/512M        | 2        | Stateless
PostgreSQL           | 4/8            | 8G/16G           | 1        | Single master
MongoDB              | 2/4            | 4G/8G            | 3        | Replication set
Kafka                | 2/4            | 4G/8G            | 3        | Broker cluster
Spark Driver         | 4/16           | 8G/32G           | 1-5*     | Batch jobs
Redis Cache          | 1/2            | 2G/4G            | 2        | HA cache
```
*Auto-scales based on load

### 4.4 Caching Strategy
**Cache Layers:**
1. **Browser Cache**: Static assets (30 days), Angular bundles (1 hour)
2. **Redis (Session)**: User preferences, template metadata (TTL: 30 mins)
3. **Redis (Data)**: Extracted data pending review (TTL: 24 hours)
4. **Database Query Cache**: Entity mappings (Soniq), rules (TTL: 1 hour)
5. **Hazelcast (Distributed)**: Shared template cache across instances

**Invalid Candidates for Caching:**
- Real-time document status (query DB directly)
- Approval workflow state (source of truth: database)
- User permissions (cache with short TTL: 5 mins)

---

## Q5: Performance Challenges & Solutions

### 5.1 General Challenges

**Challenge 1: Large File Processing (100MB+ PDFs)**
```
Problem: Single extraction request exhausts memory
Solution:
- Stream-based processing (chunked reading)
- Spark distributed processing for large batches
- Timeout protection (max 5 mins per doc)
```

**Challenge 2: Concurrent Extraction Requests**
```
Problem: All requests hit same SparkAir → rate limiting
Solution:
- Request queuing with priority (Level 2 users first)
- Multiple vendor fallback (SparkAir → Cognize → Deepmine)
- Rate limiting per tenant (5 concurrent requests)
```

**Challenge 3: Database Bottleneck (Postgres)**
```
Problem: 1,500 op/sec exceeds typical Postgres
Solution:
- Read replicas for reporting queries
- Connection pooling (HikariCP, max 50 connections)
- Batch writes (insert 100 documents at once)
- Partitioning documents table by date
```

### 5.2 Domain-Specific Challenges

**Challenge 1: Entity Name Mapping (Soniq API)**
```
Problem: Every extraction calls Soniq → external API latency + rate limit
Solution:
- Local entity cache (Redis) + Soniq fallback
- Batch API calls (map 10 entities in 1 request)
- Async mapping (flag unmapped entities, process later)
- Confidence scoring: if score <60%, require manual confirmation
```

**Challenge 2: State Machine Complexity (Document Lifecycle)**
```
Problem: Concurrent state transitions (L1 submits while L2 reviews)
Solution:
- Pessimistic locking: `SELECT ... FOR UPDATE` during review
- Optimistic locking: Version field + conflict resolution
- Event sourcing: Immutable event log + derived state
```

**Challenge 3: PDF Extraction Accuracy vs Speed**
```
Problem: Better OCR (slower) vs fast extraction (lower confidence)
Solution:
- Multi-pass approach:
  1. Fast extraction (native PDF text) → confidence score
  2. If score <70%, trigger OCR in background
  3. Notify user of improved extraction (push notification)
```

**Challenge 4: Handling Malformed Excel/CSV**
```
Problem: Encoding issues, missing headers, irregular structure
Solution:
- Detection + quarantine (flag for manual review)
- Fallback parsers (Apache POI → manual, charset auto-detection)
- Partial success (parse what you can, mark skipped rows)
```

---

## Q6: Horizontal & Cross-Cutting Concerns

### 6.1 Horizontal Concerns (System-Wide)

| Concern | Implementation | Tool |
|---------|---|---|
| **Security** | RBAC, encryption, audit logging | Spring Security, HashiCorp Vault |
| **Observability** | Distributed tracing, metrics, logs | Jaeger, Prometheus, Splunk |
| **Caching** | Multi-layer cache strategy | Redis, Hazelcast, browser cache |
| **Error Handling** | Global exception handler, error codes | Spring `@ControllerAdvice` |
| **Rate Limiting** | Token bucket per user/tenant | Spring Cloud Gateway |
| **Validation** | Input validation at API boundary | Spring Validation framework |

### 6.2 Cross-Cutting Concerns (Aspects)

```java
// AOP implementation in Spring
@Aspect
@Component
public class CrossCuttingConcerns {
  
  @Around("@annotation(Timed)")
  public Object timedExecution(ProceedingJoinPoint pjp) {
    // Distributed tracing + metrics collection
  }
  
  @Around("@annotation(Secured)")
  public Object securityCheck(ProceedingJoinPoint pjp) {
    // RBAC enforcement
  }
  
  @Around("@annotation(Cached)")
  public Object cacheAside(ProceedingJoinPoint pjp) {
    // Cache-aside pattern
  }
  
  @Around("execution(* com.dcp.service.*.*(..))")
  public Object auditLogging(ProceedingJoinPoint pjp) {
    // Log all service method calls
  }
}
```

### 6.3 Resilience Patterns (Across Services)

```
Pattern                | Application
-----------------------|--------
Circuit Breaker        | SparkAir timeout → fallback to Cognize
Bulkhead               | Limit extraction thread pool to 20
Retry                  | S3 download: exponential backoff
Timeout                | All external calls: 30s default
Fallback               | Soniq unavailable → use cached entity mappings
Rate Limiter           | User extraction requests: 5/minute
```

---

## Q7: Deployment Strategy (Kubernetes + Docker)

### 7.1 Container Strategy

```dockerfile
# Multi-stage build for Spring Boot service
FROM maven:3.8-openjdk-17 AS build
COPY . /app
WORKDIR /app
RUN mvn clean package -DskipTests

FROM openjdk:17-slim
RUN useradd -m appuser
USER appuser
COPY --from=build /app/target/service.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
```

### 7.2 Kubernetes Deployment

```yaml
# deployment.yaml - Extraction Worker
apiVersion: apps/v1
kind: Deployment
metadata:
  name: extraction-worker
spec:
  replicas: 5  # Start with 5, scale to 20 based on queue depth
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 2
      maxUnavailable: 0  # Zero-downtime
  selector:
    matchLabels:
      app: extraction-worker
  template:
    metadata:
      labels:
        app: extraction-worker
    spec:
      containers:
      - name: extraction-worker
        image: dcp/extraction-worker:v1.2.3
        resources:
          requests:
            cpu: 2
            memory: 2Gi
          limits:
            cpu: 8
            memory: 4Gi
        livenessProbe:
          httpGet:
            path: /health/live
            port: 8080
          initialDelaySeconds: 40
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health/ready
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
        env:
        - name: SPARK_AIR_URL
          valueFrom:
            secretKeyRef:
              name: extraction-secrets
              key: sparkair-url
        volumeMounts:
        - name: config-volume
          mountPath: /etc/config
      volumes:
      - name: config-volume
        configMap:
          name: extraction-config

---
# HPA - Auto-scaling based on queue depth
apiVersion: autoscaling.custom.io/v1
kind: CustomMetricAutoscaler
metadata:
  name: extraction-worker-scaler
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: extraction-worker
  minReplicas: 5
  maxReplicas: 20
  metrics:
  - type: External
    external:
      metricName: kafka_lag_sum{queue="extraction"}
      targetValue: "10000"  # Scale if lag > 10K messages
```

### 7.3 Service Mesh (Optional - Istio)

```yaml
# Virtual Service for extraction worker
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: extraction-worker
spec:
  hosts:
  - extraction-worker
  http:
  - match:
    - headers:
        user-type:
          exact: "LEVEL_2"
    route:
    - destination:
        host: extraction-worker
        port:
          number: 8080
      weight: 70  # Priority: L2 users get 70% capacity
    - destination:
        host: extraction-worker-backup
        port:
          number: 8080
      weight: 30

---
# Fault injection for resilience testing
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: sparkair-connector
spec:
  hosts:
  - sparkair-api
  http:
  - fault:
      abort:
        percentage: 10  # Simulate 10% failures for testing
        grpc: "UNAVAILABLE"
      delay:
        percentage: 20  # Simulate 200ms delay for 20% requests
        fixedDelay: 200ms
    route:
    - destination:
        host: sparkair-api
        port:
          number: 443
```

### 7.4 CI/CD Pipeline (Azure DevOps)

```yaml
# azure-pipelines.yml
trigger:
  - main
  - develop

pool:
  vmImage: 'ubuntu-latest'

variables:
  buildConfiguration: 'Release'
  dockerRegistry: 'acr.azurecr.io'

stages:
- stage: Build
  jobs:
  - job: CompileAndTest
    steps:
    - task: Maven@3
      inputs:
        mavenPomFile: 'pom.xml'
        mavenOptions: '-Xmx3072m'
        javaHomeOption: 'JDKVersion'
        jdkVersionOption: '17'
        publishJUnitResults: true
        testResultsFiles: '**/TEST-*.xml'
    
    - task: SonarCloudPrepare@1
      inputs:
        SonarCloud: 'SonarCloud'
        organization: 'myorg'
        scannerMode: 'MSBuild'
        projectKey: 'dcp-platform'
        projectName: 'Data Collection Platform'

    - task: SonarCloudAnalyze@1

    - task: PublishCodeCoverageResults@1
      inputs:
        codeCoverageTool: Cobertura
        summaryFileLocation: '**/coverage.xml'
        reportDirectory: '$(System.DefaultWorkingDirectory)/coverage'
        failIfCoverageEmpty: false

- stage: IntegrationTest
  dependsOn: Build
  condition: succeeded()
  jobs:
  - job: RunIntegrationTests
    steps:
    - script: |
        docker-compose -f docker-compose.test.yml up --build
        docker-compose -f docker-compose.test.yml exec -T app ./gradlew integrationTest
        docker-compose -f docker-compose.test.yml down
      displayName: 'Integration Tests'

- stage: SecurityScan
  dependsOn: Build
  jobs:
  - job: ContainerScan
    steps:
    - task: AzureSecurityDevOpsTask@1
      inputs:
        azureSubscription: 'Azure Subscription'
        scanType: 'container'
        image: '$(dockerRegistry)/extraction-worker:$(Build.BuildId)'

- stage: BuildAndPush
  dependsOn: [Build, IntegrationTest, SecurityScan]
  condition: succeeded()
  jobs:
  - job: DockerBuild
    steps:
    - task: Docker@2
      displayName: Build and push image
      inputs:
        command: buildAndPush
        repository: '$(dockerRegistry)/extraction-worker'
        dockerfile: '**/Dockerfile'
        containerRegistry: 'ACRConnection'
        tags: |
          $(Build.BuildId)
          latest

- stage: DeployDev
  dependsOn: BuildAndPush
  condition: succeeded()
  jobs:
  - deployment: Deploy
    displayName: Deploy to Dev
    environment: 'development'
    strategy:
      runOnce:
        deploy:
          steps:
          - task: KubernetesManifest@0
            inputs:
              action: 'deploy'
              kubernetesServiceConnection: 'dev-k8s'
              namespace: 'dcp'
              manifests: |
                $(Pipeline.Workspace)/manifests/deployment.yaml
              imagePullSecrets: 'acr-secret'

- stage: DeployProd
  dependsOn: DeployDev
  condition: succeeded()
  jobs:
  - deployment: Deploy
    displayName: Deploy to Production (Canary)
    environment: 'production'
    strategy:
      runOnce:
        deploy:
          steps:
          - task: KubernetesManifest@0
            inputs:
              action: 'deploy'
              kubernetesServiceConnection: 'prod-k8s'
              namespace: 'dcp'
              manifests: |
                $(Pipeline.Workspace)/manifests/deployment.yaml
          # Canary: route 10% traffic to new version for 30 mins
          - task: KubernetesManifest@0
            inputs:
              action: 'deploy'
              kubernetesServiceConnection: 'prod-k8s'
              manifests: |
                $(Pipeline.Workspace)/manifests/virtual-service.yaml
```

---

## Q8: Design & Implementation Pitfalls

### Pitfall 1: Synchronous Extraction Pipeline

**Bad Design:**
```
API Request → Extract → Validate → Transform → Store → Return Result
(All in single request, timeout after 30s)
```

**Problems:**
- Large PDFs timeout
- User blocked waiting
- Failed extraction blocks approval workflow
- No resilience to SparkAir outage

**Good Design:**
```
API Request → Queue Message (Kafka) → Return Job ID
                        ↓
         Worker: Extract → Validate → Store
                        ↓
         WebSocket: Notify client "Extraction complete"
```

**Implementation:**
```java
@PostMapping("/documents/{id}/extract")
public ResponseEntity<?> triggerExtraction(@PathVariable String id) {
  String jobId = UUID.randomUUID().toString();
  ExtractionJob job = new ExtractionJob(id, jobId);
  kafkaTemplate.send("extraction-requests", job);
  return ResponseEntity.accepted().body(new JobResponse(jobId));
}

// Client polls for status or receives WebSocket notification
```

---

### Pitfall 2: No Idempotency in Extraction Workers

**Bad Design:**
```
Kafka Message arrives → Extract → Store Document
(No idempotency key → duplicate extraction on retry)
```

**Problems:**
- Duplicate data in MongoDB
- Inconsistent confidence scores
- Approval workflow confusion

**Good Design:**
```java
@KafkaListener(topics = "extraction-requests")
public void processExtraction(ExtractionJob job) {
  String idempotencyKey = job.getId() + "-" + job.getVersion();
  
  // Check if already processed
  if (idempotencyStore.exists(idempotencyKey)) {
    log.info("Skipping duplicate extraction: {}", idempotencyKey);
    return;
  }
  
  try {
    ExtractedData data = sparkairClient.extract(job.getDocument());
    mongoTemplate.insert(data, "extracted_documents");
    idempotencyStore.mark(idempotencyKey);
  } catch (Exception e) {
    // Leave idempotency key unmarked → retry will re-process
    throw new RetryableException(e);
  }
}
```

---

### Pitfall 3: Storing Sensitive Data in Logs

**Bad Design:**
```java
log.info("Extracted data: {}", extractedData);  // Logs PII!
kafkaTemplate.send("logs", json);  // Unencrypted logs
```

**Good Design:**
```java
@Bean
public Filter sensitiveDataFilter() {
  return new Filter() {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, 
                        FilterChain chain) {
      // Mask PII before logging
      String sanitized = request.getParameter("ssn").replaceAll("\\d(?=\\d{4})", "*");
    }
  };
}

// Use structured logging
log.info("extraction_completed", 
         kv("documentId", doc.getId()),
         kv("confidence", data.getConfidence()),
         // Skip: extractedValue (contains PII)
);
```

---

### Pitfall 4: No Circuit Breaker for External APIs

**Bad Design:**
```java
ExtractedData extract(Document doc) {
  return sparkairClient.extract(doc);  // Throws exception if SparkAir down
}
// Cascading failures across all extraction requests
```

**Good Design:**
```java
@CircuitBreaker(
  name = "sparkair-extraction",
  fallbackMethod = "fallbackExtraction"
)
@Retry(maxAttempts = 3, delay = 1000)
@Timeout(value = 30, unit = ChronoUnit.SECONDS)
public ExtractedData extract(Document doc) {
  return sparkairClient.extract(doc);
}

private ExtractedData fallbackExtraction(Document doc, Exception e) {
  log.warn("SparkAir unavailable, marking for manual extraction: {}", doc.getId());
  mongoTemplate.updateFirst(
    new Query(Criteria.where("_id").is(doc.getId())),
    new Update().set("status", "MANUAL_REQUIRED").set("reason", e.getMessage()),
    Document.class
  );
  return null;
}
```

---

### Pitfall 5: Weak Entity Mapping Strategy

**Bad Design:**
```java
// Inline entity mapping, repeated queries
public void enrichExtractedData(ExtractedData data) {
  String soniqId = soniqClient.getEntityId(data.getEntityName());
  data.setSoniqId(soniqId);
}
```

**Problems:**
- Every extraction calls Soniq API
- High latency, rate-limited
- Cascading failures

**Good Design:**
```java
public class EntityEnrichmentService {
  
  @Cacheable(value = "entity-cache", key = "#entityName", 
             cacheManager = "redisCacheManager")
  public EntityMapping getEntityMapping(String entityName) {
    try {
      return soniqClient.getEntityId(entityName);
    } catch (RateLimitException e) {
      return EntityMapping.builder()
        .status("PENDING")
        .entityName(entityName)
        .build();  // Async retry later
    }
  }
  
  // Background job to resolve pending mappings
  @Scheduled(fixedDelay = 300000)  // Every 5 mins
  public void resolvePendingMappings() {
    List<EntityMapping> pending = mongoTemplate.find(
      new Query(Criteria.where("status").is("PENDING")),
      EntityMapping.class
    );
    // Batch resolve: soniqClient.getEntityIds(pending.stream()...)
  }
}
```

---

### Pitfall 6: Unbounded Queue Growth

**Bad Design:**
```java
@KafkaListener(topics = "extraction-requests")
public void processExtraction(ExtractionJob job) {
  // Slow extraction (5 mins) → messages pile up in queue
  // OOM after 1M messages
}
```

**Good Design:**
```java
// Kubernetes HPA scales workers based on queue lag
# metrics-server collects custom metrics from Kafka
apiVersion: custom.metrics.k8s.io/v1beta1
kind: PodMetricSource
metadata:
  name: kafka_lag
spec:
  resource: "kafka_lag_sum{topic=extraction-requests}"

---
# HPA uses this metric
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: extraction-worker-scaler
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
  - type: Pods
    pods:
      metricName: kafka_lag_sum
      metricSelector:
        matchLabels:
          topic: extraction-requests
      targetValue: "5000"  # Keep queue lag < 5K messages
```

---

### Pitfall 7: No Data Lineage or Audit Trail

**Bad Design:**
```java
// Extract → Store → Approve → Done
// No record of who extracted, when, with what confidence
```

**Good Design - Event Sourcing:**
```java
@Document
public class DocumentEvent {
  String documentId;
  String eventType;  // SOURCED, EXTRACTED, REVIEWED, APPROVED
  String actor;
  LocalDateTime timestamp;
  Map<String, Object> payload;
  String extractionEngine;  // Which engine used
  Double confidence;
}

@Service
public class DocumentEventStore {
  
  public void recordEvent(DocumentEvent event) {
    mongoTemplate.insert(event, "document_events");
    // Also update derived state document
    updateDocumentState(event);
  }
  
  public List<DocumentEvent> getLineage(String documentId) {
    return mongoTemplate.find(
      new Query(Criteria.where("documentId").is(documentId))
                .with(Sort.by("timestamp")),
      DocumentEvent.class
    );
  }
}
```

---

### Pitfall 8: Race Conditions in Approval Workflow

**Bad Design:**
```
L1 submits for review → L2 reviews → L1 modifies submitted data
```

**Problems:**
- L2 approved stale data
- Inconsistent state

**Good Design - State Machine:**
```java
@Document
public class Document {
  String id;
  DocumentStatus status;  // SOURCED, EXTRACTION_COMPLETE, PENDING_REVIEW, APPROVED
  Long version;  // Optimistic locking
  
  @DBRef
  User assignedTo;
}

@Service
public class DocumentApprovalService {
  
  @Transactional
  public void approveDocument(String documentId, String reviewerId) {
    Document doc = mongoTemplate.findById(documentId, Document.class);
    
    if (!doc.getStatus().equals(DocumentStatus.PENDING_REVIEW)) {
      throw new InvalidStateTransition(
        "Cannot approve: document is " + doc.getStatus()
      );
    }
    
    if (!doc.getAssignedTo().getId().equals(reviewerId)) {
      throw new UnauthorizedException("Not assigned to this reviewer");
    }
    
    // Optimistic locking: compare-and-swap
    Update update = new Update()
      .set("status", DocumentStatus.APPROVED)
      .inc("version", 1);
    
    FindAndModifyOptions options = new FindAndModifyOptions()
      .returnNew(true)
      .upsert(false);
    
    Document updated = mongoTemplate.findAndModify(
      new Query(Criteria.where("_id").is(documentId)
                                     .and("version").is(doc.getVersion())),
      update,
      options,
      Document.class
    );
    
    if (updated == null) {
      throw new OptimisticLockingFailureException(
        "Document was modified elsewhere"
      );
    }
  }
}
```

---

## Next Section: Q9-Q17

Continue reading for performance challenges, QA pitfalls, event-driven architecture, NFRs, and orchestration strategies...
