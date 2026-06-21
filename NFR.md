# Non-Functional Requirements (NFRs) for Data Collection Platform (DCP)

## Table of Contents

1. [What are NFRs?](#what-are-nfrs)
2. [DCP NFRs Overview](#dcp-nfrs-overview)
3. [Scalability](#scalability)
4. [Performance & Latency](#performance--latency)
5. [Availability & Reliability](#availability--reliability)
6. [Data Durability & Consistency](#data-durability--consistency)
7. [Security](#security)
8. [Monitoring & Observability](#monitoring--observability)
9. [Cost Efficiency](#cost-efficiency)
10. [DCP NFR Trade-offs](#dcp-nfr-trade-offs)

---

## What are NFRs?

**Functional Requirements (FRs):** What the system does
```text
Extract data from documents
Validate extracted fields
Send documents for human review
Publish approved documents
```

**Non-Functional Requirements (NFRs):** How well it does it
```text
Extract 100 documents/second (Throughput)
Complete extraction within 5 seconds (Latency)
99.9% uptime (Availability)
Zero data loss for financial documents (Durability)
Handle 10TB of documents per day (Scalability)
Data encrypted at rest and in transit (Security)
```

NFRs are about quality, performance, reliability, and constraints.

---

## DCP NFRs Overview

| NFR | Requirement | Why It Matters |
|-----|---|---|
| **Scalability** | Handle 1000s of docs/day, growing to millions | Business grows, can't rebuild system |
| **Throughput** | Process 100-500 documents/second | Daily SLA: ingest all documents same day |
| **Latency** | Extract doc in < 5 seconds (p99) | Users see progress, not stalled |
| **Availability** | 99.9% uptime (8.6 hours down/month) | Financial clients need reliable service |
| **Data Durability** | Zero data loss after "saved" | Financial audits require 100% accuracy |
| **Consistency** | Document processed exactly once | Can't extract twice (duplicate payments) |
| **Security** | Encrypt financial data, RBAC, audit trail | Compliance (GDPR, SOC 2, etc.) |
| **Cost** | < $0.10 per document processed | Profitability threshold |
| **Observability** | Trace any document end-to-end | Debugging production issues quickly |

---

## Scalability

**Requirement:** System must handle growing load without redesign.

### DCP Current State
```
Daily documents: 10,000
Peak throughput: 100 docs/sec
Servers: 5 extraction pods
Storage: 100 GB/day
```

### DCP Future State (2x Growth)
```
Daily documents: 20,000
Peak throughput: 200 docs/sec
Servers: 10 extraction pods (scale horizontally)
Storage: 200 GB/day
```

### How to Achieve Scalability

#### 1. **Horizontal Scaling (Add More Machines)**

```text
BEFORE (Single Server):
  Upload → Extraction → Quality → Approval
  └─ Bottleneck: 1 server, max 100 docs/sec

AFTER (Multiple Servers):
  Upload → Kafka (buffer)
           ↓
     Extraction Pod 1 ──┐
     Extraction Pod 2 ──├─ (partition-based)
     Extraction Pod 3 ──┘
           ↓
  Quality Pods (also replicated)
           ↓
  Approval (humans, can't scale)
```

**Implementation:**
- Use Kafka partitions: 10 partitions for document-sourced topic
- Run 10 extraction pods (Pod A → P0, Pod B → P1, etc.)
- As load grows: add more pods, no code change needed

```python
# Each pod subscribes to same consumer group
from kafka import KafkaConsumer

consumer = KafkaConsumer(
    'document-sourced',
    group_id='extraction-group',  # All pods in same group
    # Kafka automatically assigns partitions to pods
    # Pod 1 gets P0, P2, P4
    # Pod 2 gets P1, P3, P5
)

for message in consumer:
    doc = message.value
    extracted = extract_with_ml(doc)
    # Publish result
```

**Scaling math:**
```
If 1 pod = 100 docs/sec
Need 200 docs/sec?
→ Deploy 2 pods (100 × 2 = 200)

Need 1000 docs/sec?
→ Deploy 10 pods (100 × 10 = 1000)
```

#### 2. **Stateless Services**

All pods must be stateless (no local memory/disk):

```python
# ❌ BAD: Stateful (can't scale)
class ExtractionService:
    def __init__(self):
        self.extraction_cache = {}  # Local memory!
        
    def extract(self, doc_id):
        if doc_id in self.extraction_cache:
            return self.extraction_cache[doc_id]
        
        # If pod crashes, cache is lost!
        # If 2 pods both extract doc-123, different cache!

# ✅ GOOD: Stateless (can scale)
class ExtractionService:
    def extract(self, doc_id):
        # Check Redis (shared)
        cached = redis.get(f"extraction:{doc_id}")
        if cached:
            return cached
        
        extracted = extract_with_ml(doc)
        redis.set(f"extraction:{doc_id}", extracted, ttl=3600)
        return extracted
```

Any pod can handle any document. Scale up/down anytime.

#### 3. **Database Scaling**

```text
MongoDB (Extracted Data):
  Sharding by doc_id
  Shard 1: doc-001 to doc-333,333
  Shard 2: doc-333,334 to doc-666,666
  Shard 3: doc-666,667 to doc-999,999
  
  Grows: add Shard 4, 5, etc.

PostgreSQL (Metadata):
  Read replicas for queries
  Write to primary
  Replicate to read replicas
  
  Grows: add more read replicas
```

---

## Performance & Latency

**Requirement:** Documents process fast enough to be useful.

### DCP Latency SLA
```
Sourcing → Database:     < 100ms
Extraction (ML):         < 3000ms (p50), < 5000ms (p99)
Quality Check:           < 500ms
Approval (human):        < 5 minutes (acceptable for human)
Dissemination:           < 1000ms
Total (start to finish):  < 10 seconds (for automated parts)
```

### How to Achieve Low Latency

#### 1. **Caching**

```python
class ExtractionService:
    def extract(self, doc):
        # Check cache first (Redis)
        cache_key = f"extraction:{doc['hash']}"
        
        cached = redis.get(cache_key)
        if cached:
            return cached  # < 10ms
        
        # Not cached, call ML API (3000ms)
        extracted = sparkair.extract(doc)
        
        # Cache for future (same document uploaded again)
        redis.set(cache_key, extracted, ttl=86400)  # 24 hours
        
        return extracted
```

**Result:** Duplicate documents extracted in 10ms instead of 3000ms.

#### 2. **Asynchronous Processing**

```text
❌ SLOW (Synchronous):
  Upload → Wait for extraction (3 sec) → Wait for quality (500ms) 
         → Return to user (3.5 sec total)
  User sees: "Processing... please wait"

✅ FAST (Asynchronous):
  Upload → Immediately return (< 100ms)
  Backend processes in background:
    Extraction (3 sec) → Quality (500ms) → Approval task (created)
  User sees: "Document received, check status later"
```

**Implementation:**
```python
# Fast API endpoint
@app.post("/upload")
def upload_document(file: UploadFile):
    # Step 1: Quick validation & save
    doc_id = str(uuid.uuid4())
    db.insert("documents", {
        "doc_id": doc_id,
        "status": "QUEUED",
        "file": file.read()
    })
    
    # Step 2: Publish event (instant)
    producer.send("document-sourced", {
        "doc_id": doc_id,
        "content": file_content
    })
    
    # Step 3: Return immediately (< 100ms)
    return {"doc_id": doc_id, "status": "Processing"}
    
    # Background: Kafka consumer extracts, validates, creates tasks
    # (happens in parallel, doesn't block user)
```

#### 3. **Parallel Processing**

```text
Sequential (Slow):
  Document → Extraction (3s) → Quality (500ms) → Total: 3.5s

Parallel (Fast):
  Document → Extraction (3s)    ┐
             Quality starts at  ├─ Total: 3.5s (overlaps!)
             t=1s (after 1s)    ┘
             
             Or better:
             Quality and Audit both start at t=3s (after extraction)
```

**Implementation:**
```python
# Extraction publishes event
def on_document_sourced(event):
    extracted = extract_with_ml(event)
    producer.send("document-extracted", extracted)  # ~100ms to publish

# Quality AND Audit both consume same event (parallel!)
def quality_on_document_sourced(event):
    # Quality can start while Extraction still running
    # if using per-partition consumers
    pass

def audit_on_document_sourced(event):
    # Audit can start immediately
    pass
```

---

## Availability & Reliability

**Requirement:** System is up and working when needed.

### DCP Availability SLA
```
99.9% uptime = 8.6 hours down per month
99.99% uptime = 52 minutes down per month (enterprise tier)
```

### How to Achieve High Availability

#### 1. **Replication**

```text
Single Broker (Down = Service Down):
  Broker 1: document-sourced, document-extracted, ...
  
  If Broker 1 crashes:
    ❌ All topics down
    ❌ Customers can't upload
    ❌ Extraction can't read

3 Brokers with Replication (One Down = Service Up):
  Topic: document-sourced, RF=3
  ├── Leader on Broker 1
  ├── Replica on Broker 2
  └── Replica on Broker 3
  
  If Broker 1 crashes:
    ✅ Broker 2 becomes leader
    ✅ Upload continues
    ✅ Extraction continues
    ✅ No downtime
```

**Configuration:**
```yaml
Kafka:
  replication_factor: 3          # 3 copies of each partition
  min_insync_replicas: 2         # Wait for 2 copies before acking
  
MongoDB:
  replication_set: 3             # Primary + 2 secondaries
  
PostgreSQL:
  primary + 2 read_replicas
```

#### 2. **Health Checks & Failover**

```python
class HealthMonitor:
    def check_sparkair():
        try:
            response = sparkair.health_check()
            if response.status == "healthy":
                circuit_breaker.close()  # Use normally
            else:
                circuit_breaker.open()   # Use fallback
        except Timeout:
            circuit_breaker.open()       # Assume dead
            
        return response.status

# Continuously monitor
schedule.every(10).seconds.do(check_sparkair)

# If SparkAir down:
#   Circuit opens → Use Cognize fallback
#   Customers still upload, extraction still works (slower)
```

#### 3. **Graceful Degradation**

```text
Normal (All systems healthy):
  Upload → Extraction (SparkAir, 3s) → Quality → Disseminate ✅

Degraded (SparkAir down):
  Upload → Extraction (Cognize fallback, 5s) → Quality → Disseminate ✅
  
  Service still works, just slower. Customers don't notice much.

Severely Degraded (Both ML services down):
  Upload → Queue for manual extraction → Quality → Disseminate ✅
  
  Service works, takes longer. Notification sent to L1 users.
  "We're experiencing high load, manual review may take 2 hours"
```

---

## Data Durability & Consistency

**Requirement:** Financial data never lost, never duplicated.

### DCP Durability SLA
```
RTO (Recovery Time Objective): < 1 hour
RPO (Recovery Point Objective): 0 (no data loss)
```

### How to Achieve Durability

#### 1. **Idempotent Processing**

```python
# Every extraction must be idempotent
class ExtractionService:
    def extract(self, doc_id):
        # Check if already extracted
        existing = db.query(f"SELECT * FROM extractions WHERE doc_id = {doc_id}")
        if existing:
            logger.info(f"Already extracted {doc_id}, skipping")
            return existing
        
        # Do extraction
        extracted = sparkair.extract(doc)
        
        # Save atomically
        with db.transaction():
            db.insert("extractions", {
                "doc_id": doc_id,
                "extracted_data": extracted,
                "extracted_at": now()
            })
            db.insert("processed_messages", {
                "message_id": kafka_message_id,
                "doc_id": doc_id
            })
        
        return extracted
```

**Result:** Even if Kafka resends message, extraction happens only once.

#### 2. **Transactional Outbox Pattern**

```python
# Save state and intent ATOMICALLY
with db.transaction():
    # Save extracted data
    db.insert("extracted_documents", {
        "doc_id": doc_id,
        "extracted_data": extracted,
        "status": "EXTRACTED"
    })
    
    # Save intent to publish (outbox)
    db.insert("outbox_events", {
        "event_id": str(uuid.uuid4()),
        "event_type": "DocumentExtracted",
        "doc_id": doc_id,
        "payload": extracted,
        "published": False
    })
    # Both succeed or both fail - no in-between

# Separate process: publish outbox events
def publish_outbox_events():
    events = db.query("SELECT * FROM outbox_events WHERE published = False")
    for event in events:
        producer.send("document-extracted", event)
        db.update("outbox_events", event.id, {"published": True})
```

**Result:** Even if outbox publisher crashes, no events lost.

#### 3. **Multi-Tier Storage**

```text
Operational (7 days, Kafka):
  Real-time extraction, quality, approval
  Fast access, short retention
  
Database (30 days, MongoDB + PostgreSQL):
  Extracted data, state, decisions
  Durable, queryable, backed up
  
Compliance Archive (7 years, S3 + Glacier):
  Financial audit trail, immutable
  Long-term compliance requirement
  Rarely accessed, slow retrieval OK
```

---

## Security

**Requirement:** Financial data protected from theft and misuse.

### DCP Security SLA
```
Data encrypted at rest: ✅ AES-256
Data encrypted in transit: ✅ TLS 1.3
Access control (RBAC): ✅ L1/L2/Admin roles
Audit trail: ✅ Who accessed what when
Compliance: ✅ SOC 2, GDPR, PCI-DSS
```

### How to Achieve Security

#### 1. **Encryption at Rest**

```yaml
MongoDB:
  encryption:
    enabled: true
    engine: AES-256
    keyManagement: AWS KMS (external key server)
    
PostgreSQL:
  encryption:
    enabled: true
    encryption_key: rotate quarterly
    
S3 (Archive):
  encryption:
    enabled: true
    sse_algorithm: AES-256
    key_source: AWS KMS
```

**Result:** Even if disk stolen, data unreadable without key.

#### 2. **Encryption in Transit**

```python
# All APIs use TLS
app = FastAPI()
app.add_middleware(HTTPSRedirectMiddleware)  # Redirect HTTP → HTTPS

# All Kafka connections use TLS
producer = KafkaProducer(
    security_protocol="SSL",
    ssl_cafile="/path/to/ca.pem",
    ssl_certfile="/path/to/cert.pem",
    ssl_keyfile="/path/to/key.pem"
)
```

**Result:** Even if network sniffed, data encrypted.

#### 3. **Role-Based Access Control (RBAC)**

```python
class L1Reviewer:
    # Can view documents, approve/reject, create tasks
    # Cannot edit extraction results, cannot access payments
    
class ExtractionEngineer:
    # Can view extraction logs, tune ML models
    # Cannot view documents, cannot approve
    
class Admin:
    # Can do everything
    # Requires 2FA, additional logging

# Enforce at API level
@app.get("/documents/{doc_id}")
def get_document(doc_id, current_user=Depends(get_current_user)):
    if current_user.role == "L1_REVIEWER":
        return document  # OK
    elif current_user.role == "EXTRACTION_ENGINEER":
        return {"error": "Forbidden"}  # No access
    elif current_user.role == "ADMIN":
        return document  # OK
```

#### 4. **Audit Trail**

```python
class AuditService:
    def log_access(self, user_id, doc_id, action):
        db.insert("audit_logs", {
            "timestamp": now(),
            "user_id": user_id,
            "doc_id": doc_id,
            "action": action,  # "VIEW", "APPROVE", "REJECT"
            "result": "SUCCESS" or "FAILURE",
            "ip_address": request.remote_addr,
            "user_agent": request.user_agent
        })

# Every action logged
def view_document(doc_id, current_user):
    audit.log_access(current_user.id, doc_id, "VIEW")
    return get_document(doc_id)

def approve_document(doc_id, current_user):
    audit.log_access(current_user.id, doc_id, "APPROVE")
    # ... approval logic ...
```

**Result:** Can audit who did what, when, from where.

---

## Monitoring & Observability

**Requirement:** Quickly find and fix production issues.

### DCP Observability SLA
```
Mean time to detect (MTTD): < 1 minute
Mean time to resolve (MTTR): < 5 minutes
Trace any document end-to-end: Yes
Alert on anomalies: Yes
```

### How to Achieve Observability

#### 1. **Distributed Tracing**

```python
import uuid

# Every request gets trace_id
def upload_document(file):
    trace_id = str(uuid.uuid4())
    
    logger.info(f"Upload started", extra={"trace_id": trace_id})
    
    # Pass trace_id through entire pipeline
    producer.send("document-sourced", {
        "doc_id": doc_id,
        "trace_id": trace_id,  # Continue trace
        "content": file
    })

# Extraction Service continues trace
def on_document_sourced(event):
    trace_id = event["trace_id"]
    logger.info(f"Extraction started", extra={"trace_id": trace_id})
    
    extracted = extract_with_ml(event)
    
    logger.info(f"Extraction completed", extra={"trace_id": trace_id})
    
    producer.send("document-extracted", {
        "doc_id": event["doc_id"],
        "trace_id": trace_id,  # Continue trace
        "extracted_data": extracted
    })

# Query: Get all logs for this document
# logs.filter(trace_id="550e8400-e29b-41d4-a716-446655440000")
# Output: Complete timeline of document through system
```

**Result:** "Document doc-789 is slow" → search trace → see it's stuck in Quality service → debug Quality service.

#### 2. **Metrics & Alerting**

```python
from prometheus_client import Counter, Histogram

extraction_duration = Histogram(
    'extraction_duration_seconds',
    'Time to extract document'
)

extraction_errors = Counter(
    'extraction_errors_total',
    'Total extraction failures'
)

@timer(extraction_duration)
def extract(doc):
    try:
        return sparkair.extract(doc)
    except Exception as e:
        extraction_errors.inc()
        raise

# Alerts
alerts = [
    {
        "name": "HighExtractionLatency",
        "condition": "p99(extraction_duration) > 5s",
        "action": "Page on-call"
    },
    {
        "name": "ExtractionErrors",
        "condition": "rate(extraction_errors[5m]) > 0.1",  # >0.1/sec
        "action": "Slack #alerts channel"
    },
    {
        "name": "KafkaLag",
        "condition": "extraction_group_lag > 10000",
        "action": "Page on-call"
    }
]
```

**Result:** Issue detected automatically, team notified before customers notice.

#### 3. **Dashboards**

```yaml
Extraction Dashboard:
  - Documents/second (target: 100)
  - Latency p50, p99 (target: 3s, 5s)
  - Error rate (target: < 0.1%)
  - Kafka lag (target: < 100)
  
Quality Dashboard:
  - Validation pass rate (target: > 95%)
  - Manual review rate (target: < 5%)
  
System Health:
  - Broker health (all 3 up?)
  - MongoDB replication lag (< 100ms?)
  - SparkAir availability (up or down?)
  - Cognize availability (up or down?)
```

---

## Cost Efficiency

**Requirement:** Process documents profitably.

### DCP Cost Breakdown (per 1000 documents)

```
Kafka cluster:           $2
MongoDB storage:         $3
PostgreSQL:              $1
Extraction (SparkAir):   $30 (ML API cost)
Cognize fallback:        $40 (expensive fallback)
Human review:            $20 (L1/L2 salaries allocated)
AWS infrastructure:      $3
Monitoring/logs:         $1
_________________
Total per 1000 docs:     $100

Per document: $0.10
```

### How to Achieve Cost Efficiency

#### 1. **Right-size Infrastructure**

```text
Over-provisioned:
  30 extraction pods for normal 100 docs/sec
  Cost: High
  Usage: 10% (3 pods enough)
  Waste: $18,000/month

Properly-sized:
  3 extraction pods for normal 100 docs/sec
  10 pods for peak (scaling up)
  Cost: $2000/month
  Usage: 100%
  
Auto-scaling:
  Scale to 3 pods at night (low traffic)
  Scale to 10 pods during day
  Saves: $12,000/month
```

#### 2. **Use Cheaper Alternatives**

```text
ML Extraction Cost:
  SparkAir (primary): $30 per 1000 docs
  Cognize (fallback): $40 per 1000 docs (slower, more expensive)
  Manual (last resort): $20 per 1000 docs (slow, human)

Strategy:
  Normal load: Use SparkAir (fastest, cheapest)
  SparkAir down: Switch to Cognize (15% more expensive, tolerable)
  Both down: Queue for manual (expensive, but rare)
  
  Cost during normal: $30/1000
  Cost with outage: $40/1000 (acceptable)
```

#### 3. **Retention Optimization**

```yaml
Kafka (7 days):
  Cost: $2/day
  Purpose: Real-time processing
  
MongoDB (30 days):
  Cost: $3/day
  Purpose: Query-friendly storage
  
S3 (7 years, Glacier):
  Cost: $1/year (cheap cold storage)
  Purpose: Compliance archive
```

---

## DCP NFR Trade-offs

All NFRs have trade-offs. Choose wisely:

| NFR | Cost | Complexity | Trade-off |
|-----|------|-----------|-----------|
| **High Availability (99.99%)** | $$ | High | More servers, replication, failover |
| **Low Latency (< 5s p99)** | $$ | Medium | Caching, parallel processing |
| **Scalability** | $ | Medium | Horizontal scaling (stateless) |
| **Security (Encryption)** | $ | Low | Small CPU/network overhead |
| **Observability (Full tracing)** | $ | Medium | Logging/monitoring tools |
| **Cost Efficiency** | - | High | Wrong-sizing, vendor lock-in |

### DCP Chosen Trade-offs

```
Priority 1: Data Durability
  Cost: $$$ (replication, backups, multi-tier storage)
  Reason: Financial data loss = lawsuits
  
Priority 2: Availability (99.9%)
  Cost: $$ (3-broker cluster, failover)
  Reason: Customers need reliable service
  
Priority 3: Low Latency (< 5s p99)
  Cost: $$ (caching, async processing)
  Reason: Users need feedback
  
Priority 4: Security
  Cost: $ (encryption, RBAC)
  Reason: Compliance requirement
  
Priority 5: Cost Efficiency
  Cost: $ (auto-scaling, cheaper fallbacks)
  Reason: Profitability threshold
```

---

## Summary: DCP NFR Checklist

- [ ] **Scalability** — Horizontal scaling with Kafka partitions + stateless services
- [ ] **Performance** — Caching + async processing + parallel consumers
- [ ] **Availability** — 3-broker Kafka, replication, circuit breaker with fallback
- [ ] **Durability** — Idempotent processing + outbox pattern + multi-tier storage
- [ ] **Security** — Encryption at rest/transit + RBAC + audit trail
- [ ] **Observability** — Distributed tracing + metrics + dashboards
- [ ] **Cost** — Auto-scaling + cheaper fallbacks + retention optimization

**Interview answer ready!** ✅

---
