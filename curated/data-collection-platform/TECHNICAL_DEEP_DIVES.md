# Data Collection Platform - Technical Deep Dives & Interview Talking Points

## Section 1: Most Challenging Technical Problems & Solutions

### Challenge 1: Distributed Document State Management

**Problem:** 
Document state changes across multiple services:
- Sourcing service marks as "SOURCED"
- Extraction worker marks as "EXTRACTED"
- Quality engine marks as "QUALITY_CHECKED"
- Approval service marks as "APPROVED"

With concurrent updates, how do you prevent:
- Lost updates? (two workers both try to change state)
- Inconsistent reads? (L1 user sees old state)
- Orphaned documents? (stuck in intermediate state)

**Navigation Strategy:**

```
1. Problem Analysis
   - Root cause: Mutable shared state across services
   - Failed approach: "Let database handle it" (optimistic locking still fails for complex workflows)
   
2. Solution: Event Sourcing + CQRS
   - Every state change becomes immutable event
   - Event store (Kafka) is source of truth
   - Multiple read models (derived from events)
   
3. Implementation in Code
```

```java
// Event Sourcing Approach
@Document
public class DocumentEvent {
  String id;
  String documentId;
  String eventType;  // SOURCED, EXTRACTED, QUALITY_PASSED, APPROVED
  long version;      // Monotonically increasing
  Instant timestamp;
  Map<String, Object> payload;
  String actor;
}

@Service
public class DocumentStateService {
  
  // Append-only event log
  public void recordEvent(String documentId, String eventType, Map<String, Object> payload) {
    DocumentEvent event = new DocumentEvent();
    event.setDocumentId(documentId);
    event.setEventType(eventType);
    event.setPayload(payload);
    event.setTimestamp(Instant.now());
    event.setVersion(getNextVersion(documentId));  // Strictly increasing
    
    mongoTemplate.insert(event, "document_events");
    
    // Publish event for other services
    kafkaTemplate.send("document-events", event);
    
    // Update read model (derived document state)
    updateDocumentReadModel(documentId);
  }
  
  // Rebuild state from events (event replay)
  public DocumentState getState(String documentId) {
    List<DocumentEvent> events = mongoTemplate.find(
      new Query(Criteria.where("documentId").is(documentId))
        .with(Sort.by("version")),
      DocumentEvent.class
    );
    
    DocumentState state = new DocumentState(documentId);
    for (DocumentEvent event : events) {
      state.apply(event);  // State machine: apply each event in order
    }
    return state;
  }
  
  // Prevent concurrent modifications via optimistic locking
  public void transitionState(String documentId, DocumentStatus from, DocumentStatus to) {
    List<DocumentEvent> events = getEvents(documentId);
    long currentVersion = events.size();
    
    // Only allow transition if from state is correct
    DocumentStatus currentState = deriveState(events);
    if (!currentState.equals(from)) {
      throw new InvalidStateTransitionException(
        "Expected " + from + " but was " + currentState
      );
    }
    
    // Record event with version
    recordEvent(documentId, to.name(), 
      Map.of("fromVersion", currentVersion, "toVersion", currentVersion + 1)
    );
  }
}

// Read Model (derived from events, updated asynchronously)
@Document("documents")
public class DocumentReadModel {
  String id;
  String documentId;
  DocumentStatus status;
  long version;
  Instant lastUpdated;
}

@Component
@KafkaListener(topics = "document-events")
public class DocumentReadModelUpdater {
  
  public void updateReadModel(DocumentEvent event) {
    DocumentReadModel doc = mongoTemplate.findById(event.getDocumentId(), DocumentReadModel.class);
    if (doc == null) {
      doc = new DocumentReadModel();
      doc.setDocumentId(event.getDocumentId());
    }
    
    // Update state based on event
    doc.setStatus(event.getEventType());
    doc.setVersion(event.getVersion());
    doc.setLastUpdated(event.getTimestamp());
    
    mongoTemplate.save(doc);
  }
}
```

**Benefits:**
- ✅ Complete audit trail (all state changes in Kafka)
- ✅ No lost updates (immutable events)
- ✅ Easy event replay (recover from bugs)
- ✅ Temporal queries ("what was state on 2024-01-15?")
- ✅ GDPR compliance (complete data lineage)

**Trade-offs:**
- ❌ Eventual consistency (document status may lag real events by seconds)
- ❌ Increased complexity (two storage layers)
- ❌ Higher storage usage (storing all events)

---

### Challenge 2: High-Confidence Entity Mapping at Scale

**Problem:**
- 10K documents/day
- Each document has 5-10 entities (50K entities/day)
- Soniq API: 1000 requests/minute (rate limited)
- Soniq latency: 500ms average
- Offline = whole pipeline blocks

**Navigation Strategy:**

```
1. Identify Constraints
   - Rate limit: 1000 req/min = 16.7 req/sec
   - But peak load: 50K entities/day ÷ 16 hours = 52 entities/sec needed
   - Gap: Need 52/16.7 = 3.1x more capacity!
   
2. Solution Layers (Cache → Batch → Fallback → Async Resolution)

3. Implementation
```

```java
@Service
public class EntityMappingService {
  
  // Layer 1: Local L1 cache (entity name → Soniq ID)
  @Cacheable(value = "entity-mapping", 
             key = "#entityName", 
             cacheManager = "redisCacheManager",
             unless = "#result == null")
  public Optional<EntityMapping> getMapping(String entityName) {
    // Layer 2: Batch lookup (amortize API calls)
    return mappingRepository.findByEntityName(entityName);
  }
  
  // Batch API call (10 entities in 1 request)
  @Transactional
  public Map<String, EntityMapping> batchGetMappings(List<String> entityNames) {
    // Check cache first
    Map<String, EntityMapping> cached = entityNames.stream()
      .collect(Collectors.toMap(
        Function.identity(),
        name -> cacheManager.getCache("entity-mapping").get(name, EntityMapping.class),
        (a, b) -> a == null ? b : a  // Prefer non-null
      ));
    
    // Find cache misses
    List<String> misses = entityNames.stream()
      .filter(name -> cached.get(name) == null)
      .collect(Collectors.toList());
    
    if (misses.isEmpty()) {
      return cached;
    }
    
    // Batch Soniq API call (reduce from 50 calls to 5)
    Map<String, EntityMapping> apiFetch = soniqClient.batchGetEntities(misses);
    
    // Update cache and DB
    apiFetch.forEach((name, mapping) -> {
      cacheManager.getCache("entity-mapping").put(name, mapping);
      mappingRepository.save(mapping);
    });
    
    // Handle unmapped entities (rate limit or not found)
    List<String> unmapped = misses.stream()
      .filter(name -> !apiFetch.containsKey(name))
      .collect(Collectors.toList());
    
    if (!unmapped.isEmpty()) {
      // Queue for async resolution (retry later)
      unmapped.forEach(name -> {
        queueAsyncResolution(name);
      });
    }
    
    return Stream.concat(
      cached.entrySet().stream(),
      apiFetch.entrySet().stream()
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
  
  // Layer 3: Fallback for failures
  @CircuitBreaker(
    name = "soniq-mapping",
    fallbackMethod = "fallbackEntityMapping"
  )
  public EntityMapping getEntityWithFallback(String entityName) {
    return getMapping(entityName)
      .orElseThrow(() -> new EntityNotMappedException(entityName));
  }
  
  private EntityMapping fallbackEntityMapping(String entityName, Exception e) {
    log.warn("Soniq unavailable, using fuzzy match for: {}", entityName);
    
    // Fuzzy match against cache (entity names similar to requested)
    String best = findSimilarCachedEntity(entityName);
    if (best != null) {
      return getMapping(best).orElse(null);
    }
    
    // Last resort: create stub mapping
    return EntityMapping.builder()
      .entityName(entityName)
      .status("UNMAPPED")
      .confidence(0.0)
      .requiresManualReview(true)
      .build();
  }
  
  // Layer 4: Async batch resolution (runs every 5 minutes)
  @Scheduled(fixedDelay = 300000)
  public void resolveUnmappedEntities() {
    List<EntityMapping> pending = mappingRepository
      .findByStatus("UNMAPPED")
      .stream()
      .limit(100)  // Process 100 at a time
      .collect(Collectors.toList());
    
    if (pending.isEmpty()) return;
    
    try {
      Map<String, EntityMapping> resolved = soniqClient.batchGetEntities(
        pending.stream().map(EntityMapping::getEntityName).collect(Collectors.toList())
      );
      
      resolved.forEach((name, mapping) -> {
        mapping.setStatus("MAPPED");
        mappingRepository.save(mapping);
        cacheManager.getCache("entity-mapping").put(name, mapping);
      });
      
      log.info("Resolved {} unmapped entities", resolved.size());
      
    } catch (Exception e) {
      log.error("Batch resolution failed", e);
      // Retry next cycle
    }
  }
}
```

**Metrics to Track:**
```java
@Component
public class EntityMappingMetrics {
  
  Timer cacheHitRate = Metrics.timer("entity.cache.hit.rate");
  Timer apiCallDuration = Metrics.timer("soniq.api.call.duration");
  AtomicInteger pendingMappings = Metrics.gauge("entity.pending.mappings", new AtomicInteger());
  
  public void recordCacheHit() {
    cacheHitRate.record(Duration.ZERO);
  }
  
  public void recordSoniqCall(long duration) {
    apiCallDuration.record(Duration.ofMillis(duration));
  }
}
```

**Result:** Handles 50K entities/day with 16.7 req/sec limit through:
- Cache hit rate: 85% (only 7.5K cache misses)
- Batch calls: Reduce 7.5K → 750 calls (10x reduction)
- Circuit breaker: Fallback instead of cascading failure

---

### Challenge 3: Quality Assurance for AI-Extracted Data

**Problem:**
- SparkAir extraction: 85% accuracy (missing edge cases)
- Confidence scores unreliable (overconfident on malformed PDFs)
- Manual review bottleneck (L2 users can only review 100 docs/day)
- Q: How to ensure <1% error rate in production?

**Navigation Strategy:**

```
1. Multi-layer Quality Strategy
   - Layer 1: Pre-validation (format, structure)
   - Layer 2: AI confidence thresholding
   - Layer 3: Automated rules-based validation
   - Layer 4: Sampling-based human review
   
2. Implementation
```

```java
@Service
public class DataQualityEngine {
  
  // Layer 1: Schema validation (pre-extraction check)
  public List<ValidationError> validateFormat(Document doc) {
    List<ValidationError> errors = new ArrayList<>();
    
    // Check file integrity
    if (!isPdfValid(doc.getContent())) {
      errors.add(new ValidationError("CORRUPT_PDF", "PDF header invalid"));
    }
    
    // Check expected fields exist
    List<String> expectedFields = templateService.getExpectedFields(doc.getTemplate());
    PDDocument pdf = PDDocument.load(doc.getContent());
    List<String> foundFields = extractFieldNames(pdf);
    
    expectedFields.stream()
      .filter(field -> !foundFields.contains(field))
      .forEach(field -> errors.add(
        new ValidationError("MISSING_FIELD", field)
      ));
    
    return errors;
  }
  
  // Layer 2: Confidence-based routing
  public void routeExtractionByConfidence(ExtractedData data) {
    if (data.getConfidence() >= 0.9) {
      // High confidence → Auto-approve (skip L2 review)
      approvalService.autoApprove(data);
      
    } else if (data.getConfidence() >= 0.7) {
      // Medium confidence → L2 review
      workflowService.assignToL2User(data);
      
    } else if (data.getConfidence() >= 0.5) {
      // Low confidence → Require manual extraction
      workflowService.assignManualExtraction(data);
      
    } else {
      // Very low → Quarantine for investigation
      quarantineService.quarantine(data, "CONFIDENCE_TOO_LOW");
    }
  }
  
  // Layer 3: Rules-based validation (domain logic)
  @Transactional
  public DataQualityReport validateRules(ExtractedData data) {
    DataQualityReport report = new DataQualityReport();
    
    // Business rule 1: Amount must be positive
    if (data.getAmount() != null && data.getAmount() <= 0) {
      report.addError(
        new ValidationError("INVALID_AMOUNT", "Amount must be > 0")
      );
    }
    
    // Business rule 2: Date must be within last 5 years
    if (data.getDate() != null) {
      if (data.getDate().isBefore(LocalDate.now().minusYears(5))) {
        report.addWarning(
          new ValidationError("OLD_DATE", "Document date is >5 years old")
        );
      }
    }
    
    // Business rule 3: Entity must be resolvable via Soniq
    EntityMapping mapping = entityMappingService.getMapping(data.getEntityName());
    if (mapping == null || mapping.getStatus().equals("UNMAPPED")) {
      report.addError(
        new ValidationError("UNMAPPED_ENTITY", data.getEntityName())
      );
    }
    
    // Business rule 4: Cross-field consistency
    if (data.getCurrency().equals("USD") && data.getAmount() > 1_000_000_000) {
      report.addWarning(
        new ValidationError("OUTLIER_AMOUNT", 
          "Amount > $1B, please verify")
      );
    }
    
    return report;
  }
  
  // Layer 4: Sampling-based human review
  @Scheduled(fixedDelay = 3600000)  // Hourly
  public void performQualityAudit() {
    // Sample 100 auto-approved documents from last hour
    List<Document> sample = documentRepository.findAutoApprovedLastHour(100);
    
    for (Document doc : sample) {
      // Assign to L2 for spot check
      workflowService.assignAuditReview(doc, "QUALITY_AUDIT");
    }
    
    // Track audit results
    AuditResult result = new AuditResult();
    result.setTotalSampled(sample.size());
    result.setErrorsFound(countErrorsInAudit(sample));
    result.setAccuracyRate((sample.size() - result.getErrorsFound()) / 
                           (double) sample.size());
    
    auditRepository.save(result);
    
    // Alert if accuracy < 99%
    if (result.getAccuracyRate() < 0.99) {
      alertService.sendAlert(
        "Quality audit failed: accuracy only " + 
        result.getAccuracyRate() * 100 + "%"
      );
    }
  }
}

// Quality Report
@Document
public class DataQualityReport {
  String documentId;
  List<ValidationError> errors = new ArrayList<>();
  List<ValidationError> warnings = new ArrayList<>();
  double qualityScore;  // 0-100
  LocalDateTime createdAt;
  
  public double calculateScore() {
    // 100 - (errors * 10 + warnings * 1)
    return Math.max(0, 100.0 - 
      (errors.size() * 10.0 + warnings.size() * 1.0)
    );
  }
}
```

**Q&A for Interview:**

Q: "Your confidence scores were unreliable. How did you detect this?"

A: We implemented automated quality audits:
1. Sample 100 auto-approved documents hourly
2. Have L2 users manually verify them
3. Calculate actual accuracy vs AI confidence
4. Found correlation gap: AI said 95% confident but actually only 85% accurate
5. Adjusted confidence thresholds downward (0.95 → 0.75 for auto-approval)

Q: "How do you prevent manual extraction bottleneck?"

A:
1. Prioritization: High-value documents (large amounts) reviewed first
2. UI optimization: Pre-filled fields from extraction, L2 only reviews uncertain fields
3. Template expertise: L2 users only review documents within their domain
4. Parallel review: Show multiple documents per user session
5. Escalation: If queue > 500, automatically reduce confidence thresholds to auto-approve more

---

## Section 2: Distributed Tracing, Sidecar, Circuit Breaker Implementation

### Distributed Tracing (Spring Cloud Sleuth + Jaeger)

```java
@Configuration
public class TracingConfiguration {
  
  @Bean
  public JaegerTracer tracer() {
    return new JaegerTracer.Builder("extraction-service")
      .registerExtractor(Format.HTTP_HEADERS, new HttpCodec())
      .registerInjector(Format.HTTP_HEADERS, new HttpCodec())
      .build();
  }
}

@Service
@Slf4j
public class DocumentExtractionService {
  
  @Autowired
  private Tracer tracer;
  
  public ExtractedData extract(String documentId) {
    // Spans are automatically created by Spring Cloud Sleuth
    // But we can create custom spans for business operations
    
    try (Scope scope = tracer.buildSpan("document-extraction")
      .withTag("documentId", documentId)
      .withTag("component", "extraction-service")
      .startActive(true)) {
      
      Span span = scope.span();
      
      // Span 1: Download document
      try (Scope downloadScope = tracer.buildSpan("download-document")
        .asChildOf(span)
        .startActive(true)) {
        
        Document doc = s3Client.download(documentId);
        downloadScope.span().setTag("fileSize", doc.getSize());
      }
      
      // Span 2: Call SparkAir
      try (Scope sparkairScope = tracer.buildSpan("sparkair-extraction")
        .asChildOf(span)
        .startActive(true)) {
        
        ExtractedData result = sparkairClient.extract(doc);
        sparkairScope.span().setTag("confidence", result.getConfidence());
        sparkairScope.span().setTag("fieldsExtracted", result.getFields().size());
        
        return result;
      }
      
    } catch (Exception e) {
      scope.span().setTag("error", true);
      scope.span().log(Map.of("event", "error", "message", e.getMessage()));
      throw e;
    }
  }
}

// Result in Jaeger UI:
/*
  Trace: document-extraction [40ms total]
    ├─ download-document [8ms]
    │  └─ tag: fileSize=50000000 (50MB)
    └─ sparkair-extraction [30ms]
       ├─ tag: confidence=0.87
       └─ tag: fieldsExtracted=12
*/
```

### Sidecar Pattern (Istio)

```yaml
# Injection of Envoy sidecar proxy
apiVersion: v1
kind: Pod
metadata:
  name: extraction-worker
spec:
  containers:
  - name: extraction-worker
    image: extraction-worker:v1.2.3
    ports:
    - containerPort: 8080
      
  # Automatically injected by Istio
  - name: istio-proxy  # Sidecar
    image: proxyv2:latest
    ports:
    - containerPort: 15000  # Admin
    - containerPort: 15001  # Outbound
    env:
    - name: POD_NAME
      valueFrom:
        fieldRef:
          fieldPath: metadata.name
```

**Sidecar Benefits:**
1. **Traffic Management**: Circuit breaker, retry, timeout
2. **Security**: mTLS encryption, authorization policies
3. **Observability**: Automatic request tracing, metrics
4. **No code changes**: Works transparently

```yaml
# VirtualService: Define timeouts, retries (in sidecar, not app code)
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: sparkair-connector
spec:
  hosts:
  - sparkair-api
  http:
  - timeout: 30s  # Kill request after 30s
    retries:
      attempts: 3
      perTryTimeout: 10s
    route:
    - destination:
        host: sparkair-api
        port:
          number: 443
```

### Circuit Breaker Pattern (Resilience4j)

```java
@Service
public class ResilientExtractionService {
  
  @CircuitBreaker(name = "sparkair-extraction")
  @Retry(name = "sparkair-retry")
  @Timeout(name = "sparkair-timeout")
  @Bulkhead(name = "sparkair-bulkhead")
  public ExtractedData extractWithSparkAir(Document doc) {
    return sparkairClient.extract(doc);
  }
}

@Configuration
public class ResilienceConfiguration {
  
  @Bean
  public CircuitBreakerRegistry circuitBreakerRegistry() {
    CircuitBreakerConfig config = CircuitBreakerConfig.custom()
      .registerHealthIndicator(true)
      .slidingWindowSize(100)  // Last 100 calls
      .failureRateThreshold(50.0)  // Open if >50% fail
      .slowCallRateThreshold(100.0)  // Open if >100% slow
      .slowCallDurationThreshold(Duration.ofSeconds(10))
      .waitDurationInOpenState(Duration.ofMinutes(1))  // Try again after 1 min
      .minimumNumberOfCalls(10)  // Need 10 calls before evaluating
      .build();
    
    return CircuitBreakerRegistry.of(config);
  }
  
  @Bean
  public RetryRegistry retryRegistry() {
    RetryConfig config = RetryConfig.custom()
      .maxAttempts(3)
      .waitDuration(Duration.ofMillis(500))
      .intervalFunction(
        IntervalFunction.ofExponentialBackoff(500, 2)  // 500ms, 1s, 2s
      )
      .retryOnException(e -> e instanceof TimeoutException)
      .build();
    
    return RetryRegistry.of(config);
  }
  
  @Bean
  public BulkheadRegistry bulkheadRegistry() {
    BulkheadConfig config = BulkheadConfig.custom()
      .maxConcurrentCalls(20)  // Max 20 concurrent extraction requests
      .maxWaitDuration(Duration.ofSeconds(10))
      .build();
    
    return BulkheadRegistry.of(config);
  }
}

// Monitoring circuit breaker state
@Component
@Scheduled(fixedDelay = 60000)  // Every minute
public class CircuitBreakerMonitor {
  
  @Autowired
  private CircuitBreakerRegistry registry;
  
  public void logCircuitBreakerState() {
    CircuitBreaker cb = registry.circuitBreaker("sparkair-extraction");
    
    log.info("CircuitBreaker: {} | State: {} | Metrics: failure-rate={}%, " +
      "avg-duration={}ms",
      cb.getName(),
      cb.getState(),  // CLOSED, OPEN, HALF_OPEN
      cb.getMetrics().getFailureRate(),
      cb.getMetrics().getAverageDuration()
    );
    
    // Alert if open
    if (cb.getState() == OPEN) {
      alertService.sendAlert("SparkAir extraction circuit breaker is OPEN");
    }
  }
}
```

**State Transitions:**
```
                    CLOSED (normal)
                       ↓ (failure rate > 50%)
                    OPEN (reject calls, fail fast)
                       ↓ (wait 1 min)
                   HALF_OPEN (test with 1 call)
                       ↓
            ┌───────────┴───────────┐
            ↓ (success)            ↓ (fail)
         CLOSED                   OPEN
```

---

## Section 3: Caching Strategy (Applicability Analysis)

### Caching Applicability Matrix

| Data | Cacheable? | TTL | Invalidation | Reason |
|------|---|---|---|---|
| **User Session** | ✅ YES | 1 hour | Logout | Frequent access, slow to compute |
| **Templates** | ✅ YES | 1 day | Template update | Rarely changes, heavy file |
| **Entity Mapping** | ✅ YES | 1 hour | Soniq refresh | Expensive API call, frequent access |
| **Document Status** | ❌ NO | N/A | N/A | Changes every second, critical for approval |
| **Extraction Result** | ✅ YES | 24 hours | Manual correction | Immutable once extracted |
| **User Permissions** | ✅ YES | 5 minutes | Role assignment | Checked on every API call |
| **Quality Rules** | ✅ YES | 12 hours | Rule update | Rarely changes, reduces DB lookups |
| **Spark Job Results** | ✅ YES | 7 days | Data refresh | Expensive to recompute |

### Multi-Layer Caching Architecture

```java
@Service
public class MultiLayerCachingService {
  
  // Layer 1: Browser cache (static assets)
  @GetMapping("/api/templates/{id}")
  public ResponseEntity<?> getTemplate(@PathVariable String id) {
    HttpHeaders headers = new HttpHeaders();
    headers.setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic());
    headers.setETag("\"" + template.getVersion() + "\"");  // For 304 Not Modified
    
    return ResponseEntity.ok()
      .headers(headers)
      .body(template);
  }
  
  // Layer 2: Redis (session-scoped)
  @Cacheable(
    value = "user-preferences",
    key = "#userId",
    cacheManager = "sessionCacheManager",
    unless = "#result == null"
  )
  public UserPreferences getUserPreferences(String userId) {
    return userService.getPreferences(userId);  // DB only if cache miss
  }
  
  // Layer 3: Distributed cache (Hazelcast)
  @Cacheable(
    value = "entity-mappings",
    key = "#entityName",
    cacheManager = "distributedCacheManager",
    cacheResolver = "hazelcastResolver"
  )
  public Optional<EntityMapping> getEntityMapping(String entityName) {
    return soniqClient.resolveEntity(entityName);  // Expensive API call
  }
  
  // Layer 4: Database query cache (via Hibernate)
  // @Cacheable at entity level
  @Entity
  @Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
  public class ExtractionTemplate {
    // Cached at entity level, shared across queries
  }
  
  // Cache invalidation on update
  @CacheEvict(value = "entity-mappings", key = "#entityName")
  public void invalidateEntityMapping(String entityName) {
    log.info("Invalidating cache for entity: {}", entityName);
  }
  
  // Bulk invalidation
  @CacheEvict(value = "entity-mappings", allEntries = true)
  public void refreshAllMappings() {
    // Called after Soniq data refresh
  }
  
  // Cache warming (preload on startup)
  @PostConstruct
  public void warmCache() {
    List<String> commonEntities = entityMappingService.getTopEntities(1000);
    commonEntities.forEach(this::getEntityMapping);  // Pre-populate cache
  }
}
```

### Cache-Aside Pattern Implementation

```java
@Service
public class CacheAsidePattern {
  
  public ExtractedData getExtractedData(String documentId) {
    String cacheKey = "extraction:" + documentId;
    
    // Step 1: Check cache
    ExtractedData cached = redisTemplate.opsForValue()
      .get(cacheKey);
    
    if (cached != null) {
      metrics.recordCacheHit();
      return cached;
    }
    
    metrics.recordCacheMiss();
    
    // Step 2: Cache miss → fetch from DB
    ExtractedData data = mongoTemplate.findById(
      documentId, 
      ExtractedData.class
    );
    
    if (data == null) {
      throw new DocumentNotFoundException(documentId);
    }
    
    // Step 3: Update cache
    redisTemplate.opsForValue().set(
      cacheKey, 
      data,
      Duration.ofHours(24)  // TTL
    );
    
    return data;
  }
}
```

---

## Section 4: Security Architecture Details

### Secrets Management (HashiCorp Vault)

```java
@Configuration
public class VaultSecretConfig {
  
  @Bean
  public VaultTemplate vaultTemplate(VaultOperations ops) {
    return new VaultTemplate(ops);
  }
  
  @Bean
  @RefreshScope  // Reload on properties change
  public ExternalServiceCredentials externalServiceCreds(
      VaultTemplate vault) {
    
    // Fetch from Vault
    VaultResponse response = vault.read("secret/data/external-services");
    
    String sparkairUrl = (String) response.getData().get("sparkair_url");
    String sparkairApiKey = (String) response.getData().get("sparkair_api_key");
    
    return ExternalServiceCredentials.builder()
      .sparkairUrl(sparkairUrl)
      .sparkairApiKey(sparkairApiKey)
      .build();
  }
}

@Service
public class ExternalServiceConnector {
  
  @Autowired
  private ExternalServiceCredentials creds;
  
  public ExtractedData callSparkAir(Document doc) {
    // Credentials never in logs or code
    String authHeader = "Bearer " + creds.getSparkairApiKey();
    
    return RestTemplate.postForObject(
      creds.getSparkairUrl() + "/extract",
      doc,
      ExtractedData.class,
      Map.of("Authorization", authHeader)
    );
  }
}
```

### Data Encryption at Rest & In Transit

```java
@Configuration
public class EncryptionConfig {
  
  @Bean
  public DataEncryptor dataEncryptor() {
    byte[] secretKey = vaultTemplate.read("secret/data/encryption-key");
    
    return new AES256Encryptor(secretKey);
  }
}

@Service
public class SensitiveDataEncryption {
  
  @Autowired
  private DataEncryptor encryptor;
  
  public void storeDocument(ExtractedData data) {
    // Encrypt sensitive fields
    ExtractedData encrypted = ExtractedData.builder()
      .documentId(data.getDocumentId())
      .entityName(encryptor.encrypt(data.getEntityName()))  // Encrypt
      .amount(encryptor.encrypt(data.getAmount().toString()))  // Encrypt
      .confidence(data.getConfidence())  // Not sensitive
      .build();
    
    mongoTemplate.insert(encrypted);
  }
  
  public ExtractedData retrieveDocument(String documentId) {
    ExtractedData encrypted = mongoTemplate.findById(documentId, ExtractedData.class);
    
    // Decrypt sensitive fields
    ExtractedData decrypted = ExtractedData.builder()
      .documentId(encrypted.getDocumentId())
      .entityName(encryptor.decrypt(encrypted.getEntityName()))
      .amount(new BigDecimal(encryptor.decrypt(encrypted.getAmount())))
      .confidence(encrypted.getConfidence())
      .build();
    
    return decrypted;
  }
}
```

---

End of technical deep dives. Continue with deployment topology and final summary...
