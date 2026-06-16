# Data Collection Platform - Visual Reference & Quick Tables

## 1. Service Interaction Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    ANGULAR UI (Browser)                     │
│  ├─ Login                                                    │
│  ├─ Document List (React/Virtual Scroll)                    │
│  ├─ Manual Extraction Form                                  │
│  ├─ Approval Dashboard (L2)                                 │
│  └─ Reports & Analytics                                     │
└───────────────────┬─────────────────────────────────────────┘
                    │ (HTTP/JWT)
                    ↓
┌─────────────────────────────────────────────────────────────┐
│           API Gateway (Spring Cloud Gateway)                │
│  ├─ Rate Limiting (5 req/sec per user)                     │
│  ├─ JWT Validation & RBAC                                   │
│  ├─ Request/Response Logging                                │
│  └─ CORS, SSL Termination                                   │
└───────────────────┬─────────────────────────────────────────┘
                    │
        ┌───────────┴────────────┬─────────────────┬────────────────┐
        ↓                        ↓                 ↓                ↓
    ┌──────────┐          ┌──────────────┐   ┌──────────┐    ┌──────────┐
    │ Sourcing │          │ Extraction   │   │ Approval │    │Disseminate
    │ Service  │          │ Service      │   │ Service  │    │Service
    └──────────┘          └──────────────┘   └──────────┘    └──────────┘
         │                       │                 │               │
         │                       │                 │               │
    ┌────┴─────────┐        ┌─────┴──────┐  ┌────┴─────┐    ┌─────┴──────┐
    │ Kafka Topics │        │Kafka Topics │  │Kafka Topic   │Kafka Topics
    ├─ sourced     │        ├─ extracted   │  ├─ approved    ├─ disseminated
    ├─ pending     │        ├─ quality_...│  ├─ rejected    ├─ scheduled
    └─ dedup_check │        └─ failed      │  └─ escalated   └─ failed
         │                       │                 │               │
         └───────────┬───────────┴────────────┬────┴──────────────┘
                     │                        │
                     ↓                        ↓
         ┌──────────────────────┬─────────────────────┐
         │   MongoDB Documents  │  PostgreSQL Metadata│
         ├──────────────────────┼─────────────────────┤
         │ - Raw extracted data │ - Users & roles    │
         │ - Pending review     │ - Templates        │
         │ - Approved docs      │ - Taxonomies       │
         │ - Event log          │ - Rules            │
         │ - Data lineage       │ - Approvals audit  │
         └──────────────────────┴─────────────────────┘
                     │                        │
         ┌───────────┴────────────────────────┘
         │
         ├─→ Elasticsearch (Entity search)
         ├─→ Redis (Session cache, Entity cache)
         └─→ S3 Data Lake (Bronze/Silver/Gold)
              └─→ Apache Spark (Batch ETL, Dedup, Analytics)
```

---

## 2. Data Flow Diagram (Happy Path)

```
1. SOURCING STAGE
   ┌──────────────┐
   │ S3 file      │ ──→ Extract metadata
   │ Kafka msg    │     (filename, size, date, hash)
   │ Email attach │     ↓
   └──────────────┘     Dedup check (hash → MongoDB)
                        ↓
                   ┌─────────────────┐
                   │ DOCUMENT_SOURCED│ (Kafka event)
                   │ Status: SOURCED │ (MongoDB)
                   └────────┬────────┘

2. WORK ASSIGNMENT
   Assign to L1 user
   → Notification via WebSocket
   → L1 sees in "My Work" dashboard

3. EXTRACTION STAGE
   ┌──────────────────────────────────────────┐
   │ Option A: Manual Entry (L1 user)        │
   │  └─ User fills form                     │
   │     → Validation (rules engine)          │
   │     → Confidence: 1.0 (manual)          │
   │                                          │
   │ Option B: AI Extraction                 │
   │  └─ Call SparkAir / Cognize API         │
   │     → Stream PDF (avoid memory spike)    │
   │     → Extract fields → Confidence score │
   │     → If confidence < 70% → flag manual │
   └──────────────────────────────────────────┘
                     ↓
   ┌─────────────────────────────────┐
   │ EXTRACTION_COMPLETE             │ (Kafka event)
   │ Status: EXTRACTED               │ (MongoDB)
   │ Confidence: 0.87 (from SparkAir)│
   └────────────┬────────────────────┘

4. RULES & VALIDATION
   ┌──────────────────────────────────┐
   │ Rules Engine (Spring service)    │
   │ ├─ Business rules validation     │
   │ ├─ Data quality checks           │
   │ ├─ Entity mapping (Soniq)        │
   │ └─ Cross-field consistency       │
   └────────────┬─────────────────────┘
                ↓
   ┌─────────────────────────────────┐
   │ QUALITY_PASSED or QUALITY_FAILED│ (Kafka event)
   │ Status: QUALITY_CHECKED         │ (MongoDB)
   │ Quality Score: 85/100           │
   └────────────┬─────────────────────┘

5. APPROVAL STAGE
   ┌──────────────────────────────────┐
   │ Workflow Service (Camunda)       │
   │ ├─ Assign to L2 user             │
   │ ├─ Set deadline (24 hours)       │
   │ └─ Monitor for escalation        │
   └────────────┬─────────────────────┘
                ↓
   ┌─────────────────────────────────┐
   │ L2 USER REVIEW                  │
   │ ├─ Opens document in UI          │
   │ ├─ Approves or rejects           │
   │ ├─ Can request modifications     │
   │ └─ Adds comments                 │
   └────────────┬─────────────────────┘
                ↓
   ┌─────────────────────────────────┐
   │ DOCUMENT_APPROVED or REJECTED    │ (Kafka event)
   │ Status: APPROVED or NEEDS_REWORK│ (MongoDB)
   │ Approved By: reviewer-id        │ (Audit)
   └────────────┬─────────────────────┘

6. DISSEMINATION STAGE
   If approved:
   ┌──────────────────────────────────┐
   │ Dissemination Service            │
   │ ├─ Format transformation          │
   │ │  (JSON → CSV → Excel → Parquet)│
   │ ├─ Scheduled export              │
   │ └─ Publish to subscribers        │
   │    (S3, API, Kafka, Email)       │
   └────────────┬─────────────────────┘
                ↓
   ┌─────────────────────────────────┐
   │ DOCUMENT_PUBLISHED              │ (Kafka event)
   │ Status: PUBLISHED               │ (MongoDB)
   │ Destination: S3 + Subscriber API│
   └─────────────────────────────────┘

7. DATA LAKE (Overnight Batch)
   ┌─────────────────────────────────┐
   │ Apache Spark Job                │
   │ ├─ Read: approved docs from DB  │
   │ ├─ Dedup: on (hash, sourceId)  │
   │ ├─ Transform: canonical format  │
   │ └─ Write: Data Lake             │
   │    Bronze (raw)                 │
   │    → Silver (cleaned)           │
   │    → Gold (aggregate)           │
   └─────────────────────────────────┘
```

---

## 3. Technology Decision Matrix

```
Layer          | Primary          | Secondary         | Reason
---------------|------------------|-------------------|------------------
UI             | Angular 14+      | React (future)    | Enterprise features
Gateway        | Spring Cloud GW  | Kong              | Spring ecosystem
Services       | Spring Boot 2.7+ | Quarkus           | Maturity, team skills
Database       | PostgreSQL 12+   | MySQL             | Complex queries, ACID
Document Store | MongoDB 4.4+     | CouchDB           | Flexible schema
Cache          | Redis 6+         | Memcached         | Pub/sub, TTL support
Search         | Elasticsearch 7+ | Solr              | Full-text, aggregations
Queue          | Kafka 2.8+       | RabbitMQ          | High throughput, partitions
Batch          | Apache Spark 3+  | Flink             | Maturity, ML libraries
Workflow       | Camunda 7.15+    | Temporal          | Visual UI, BPMN 2.0
Orchestration  | Kubernetes 1.20+ | Docker Swarm      | Cloud-native ecosystem
Service Mesh   | Istio            | Linkerd           | Maturity, rate limiting
Tracing        | Jaeger           | Zipkin            | Cloud-native, scalable
Metrics        | Prometheus       | Datadog           | Open source, cost
Logging        | Splunk           | ELK Stack         | Compliance, features
IaC            | Terraform        | CloudFormation    | Multi-cloud support
Secrets        | HashiCorp Vault  | Azure Key Vault   | Multi-cloud, rotation
CI/CD          | Azure DevOps     | GitLab CI         | Enterprise integration
```

---

## 4. Failure Recovery Comparison

```
Failure Scenario           │ Component   │ Fallback Strategy     │ RTO  │ RPO
───────────────────────────┼─────────────┼──────────────────────┼──────┼─────
Pod crashes                │ K8s         │ Auto-restart (30s)   │ 30s  │ 0
Node fails                 │ Infra       │ Evict to other node  │ 2m   │ 0
SparkAir API unavailable   │ Extraction  │ Queue for manual     │ 1h   │ 0
Soniq rate limited         │ Entity map  │ Cache + async queue  │ N/A  │ 0
MongoDB shard down         │ Data        │ ReplicaSet failover  │ 10s  │ 0
PostgreSQL replica lag     │ Metadata    │ Promote standby      │ 5m   │ 15m
Kafka broker failure       │ Queue       │ Replica takes over   │ 30s  │ 0
Complete zone outage       │ Region      │ Failover to backup   │ 1h   │ 15m
Ransomware / data loss     │ All        │ Restore from S3 backup│ 4h   │ 1h
```

---

## 5. Microservice Boundaries & Responsibilities

```
┌──────────────────────────────────────────────────────────────────────┐
│ SOURCING SERVICE                                                     │
├──────────────────────────────────────────────────────────────────────┤
│ Responsibility: Ingest documents from multiple sources               │
│                                                                      │
│ Inputs:                  Outputs:                                   │
│ ├─ S3 files            ├─ DOCUMENT_SOURCED event                   │
│ ├─ Kafka messages      ├─ Metadata in PostgreSQL                   │
│ ├─ Email attachments   └─ Document in MongoDB                      │
│ └─ FTP files                                                        │
│                                                                      │
│ Technologies: S3 SDK, Kafka consumer, Spring Mail                  │
│ Replicas: 3 (1 per source type)                                    │
│ SLA: 99.9% (source availability)                                   │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│ EXTRACTION SERVICE                                                   │
├──────────────────────────────────────────────────────────────────────┤
│ Responsibility: Extract data from documents using AI                │
│                                                                      │
│ Inputs:                  Outputs:                                   │
│ ├─ DOCUMENT_SOURCED  ├─ EXTRACTION_COMPLETE event                  │
│ │  (Kafka)           ├─ Extracted fields in MongoDB                │
│ ├─ Manual trigger    ├─ Confidence scores                          │
│ └─ Retry from DLQ    └─ Failure events (if applicable)             │
│                                                                      │
│ Technologies: SparkAir/Cognize API, Spark (stream processing)      │
│ Replicas: 5-20 (auto-scale on queue lag)                          │
│ SLA: 99.0% (with fallback)                                         │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│ RULES ENGINE SERVICE                                                 │
├──────────────────────────────────────────────────────────────────────┤
│ Responsibility: Apply business rules and data quality checks        │
│                                                                      │
│ Inputs:                  Outputs:                                   │
│ ├─ EXTRACTION_COMPLETE├─ QUALITY_PASSED or QUALITY_FAILED         │
│ │  (Kafka)            ├─ Validation errors in MongoDB              │
│ ├─ Rules from DB      └─ Quality score (0-100)                    │
│ └─ Entity mapping                                                    │
│    (Soniq)                                                           │
│                                                                      │
│ Technologies: Spring, Drools (rules), Soniq API, Redis (cache)    │
│ Replicas: 2 (stateless)                                            │
│ SLA: 99.5%                                                          │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│ WORKFLOW SERVICE                                                     │
├──────────────────────────────────────────────────────────────────────┤
│ Responsibility: Orchestrate approval workflows (Camunda BPMN)       │
│                                                                      │
│ Inputs:                  Outputs:                                   │
│ ├─ QUALITY_PASSED    ├─ PENDING_APPROVAL event                    │
│ ├─ Manual submissions ├─ Reassignment events                       │
│ └─ Escalation timer  └─ Escalation alerts                         │
│                                                                      │
│ Technologies: Camunda BPMN engine, Spring, PostgreSQL (state)      │
│ Replicas: 2 (Camunda ha-pooled)                                    │
│ SLA: 99.9%                                                          │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│ APPROVAL SERVICE                                                     │
├──────────────────────────────────────────────────────────────────────┤
│ Responsibility: L1/L2 user reviews, approval/rejection logic        │
│                                                                      │
│ Inputs:                  Outputs:                                   │
│ ├─ User approval     ├─ DOCUMENT_APPROVED event                   │
│ │  action            ├─ DOCUMENT_REJECTED event                   │
│ ├─ Review comments   └─ Audit trail in MongoDB                    │
│ └─ Reassignment                                                     │
│    requests                                                          │
│                                                                      │
│ Technologies: Spring, Kafka producer, PostgreSQL, MongoDB          │
│ Replicas: 2 (stateless)                                            │
│ SLA: 99.5%                                                          │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│ DISSEMINATION SERVICE                                                │
├──────────────────────────────────────────────────────────────────────┤
│ Responsibility: Publish approved data to multiple destinations       │
│                                                                      │
│ Inputs:                  Outputs:                                   │
│ ├─ DOCUMENT_APPROVED ├─ S3 export (CSV, Parquet)                  │
│ ├─ Dissemination    ├─ API call to subscribers                    │
│ │  config           ├─ Email delivery                              │
│ └─ Scheduler        └─ Kafka topic (downstream systems)            │
│    events                                                            │
│                                                                      │
│ Technologies: Spring Cloud Task, S3 SDK, Email, Apache Parquet    │
│ Replicas: 2 (with message dedup)                                   │
│ SLA: 99.0%                                                          │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 6. Quality Metrics Dashboard (Prometheus)

```
┌────────────────────────────────────────────────────────────┐
│ REAL-TIME METRICS (Updated every 30 seconds)              │
├────────────────────────────────────────────────────────────┤
│                                                            │
│ 📊 Throughput                                             │
│  ├─ Documents sourced today: 8,542 / 10,000 target       │
│  ├─ Documents extracted/hour: 356 (steady state)         │
│  ├─ Approvals/hour: 242 (88% auto-approved)             │
│  └─ Quality checks failed: 1.2%                          │
│                                                            │
│ ⏱️  Response Times (P95)                                  │
│  ├─ UI page load: 1.2s (target: <2s) ✅                  │
│  ├─ Extraction (100MB): 4.5 min (target: <5min) ✅       │
│  ├─ Approval action: 800ms (target: <1s) ✅              │
│  └─ Entity lookup (Soniq): 450ms (cached: 50ms) ✅       │
│                                                            │
│ 🔄 Service Health                                        │
│  ├─ Sourcing: 3/3 pods healthy                           │
│  ├─ Extraction: 12/12 pods healthy (8 spare)             │
│  ├─ Rules Engine: 2/2 pods healthy                       │
│  ├─ PostgreSQL: Primary + 2 replicas (lag: 12ms)        │
│  ├─ MongoDB: 3-node replicaset (sync: OK)               │
│  ├─ Kafka: 3 brokers healthy (ISR: 3/3)                 │
│  └─ Redis: Master + 2 replicas (evictions: 0)           │
│                                                            │
│ 🎯 Quality Metrics                                       │
│  ├─ Extraction accuracy: 87.3% (manual verified)         │
│  ├─ Entity mapping success: 92.1% (Soniq + fallback)    │
│  ├─ Auto-approval rate: 68% (confidence > 0.75)         │
│  ├─ Audit sampling errors: 0.8% (spot-check L2)        │
│  ├─ Rework rate: 1.2% (rejected for modification)       │
│  └─ Average approval time: 2.3 hours (L1 + L2)         │
│                                                            │
│ 💰 Cost & Resource Utilization                           │
│  ├─ CPU usage: 62% (max: 70% threshold)                 │
│  ├─ Memory: 48% (max: 80% threshold)                     │
│  ├─ Storage (MongoDB): 85GB / 200GB allocated           │
│  ├─ Network egress: 450 GB/day                           │
│  ├─ Estimated monthly cost: $2,380 (within budget)      │
│  └─ Cost per document: $0.238 (target: <$0.30)         │
│                                                            │
│ 📈 Errors & Failures                                    │
│  ├─ HTTP 4xx errors: 0.2% (user input errors)           │
│  ├─ HTTP 5xx errors: 0.01% (infrastructure)              │
│  ├─ Circuit breaker trips: 0 in last 24h                │
│  ├─ Kafka message loss: 0                                │
│  ├─ Database connection timeouts: 0                      │
│  └─ DLQ messages waiting: 12 (from SparkAir failures)   │
│                                                            │
│ 🔐 Security Events (Last 24 hours)                       │
│  ├─ Failed login attempts: 23 (rate-limited)            │
│  ├─ Unauthorized API calls: 4 (RBAC enforcement)        │
│  ├─ Data access audit: 100% logged                       │
│  └─ SSL/TLS errors: 0                                    │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## 7. Cost Breakdown Table

```
Component              │ Unit    │ Price   │ Quantity│ Monthly Cost
───────────────────────┼─────────┼─────────┼────────┼──────────────
AKS Cluster (always-on)│ cluster │ $0.10/hr│ 1      │ $72
Compute Nodes (avg 20) │ vCPU    │ $0.05/hr│ 40     │ $288
PostgreSQL (managed)   │ month   │ $400    │ 1      │ $400
MongoDB (2TB)          │ month   │ $250    │ 1      │ $250
Redis Cache (20GB)     │ month   │ $150    │ 1      │ $150
Elasticsearch (100GB)  │ month   │ $200    │ 1      │ $200
S3 Storage (500GB)     │ GB      │ $0.023  │ 500    │ $125
Data Transfer (500GB)  │ GB      │ $0.09   │ 500    │ $45
Kafka Cluster (3 nodes)│ month   │ $300    │ 1      │ $300
Splunk (2TB/day logs)  │ month   │ $600    │ 1      │ $600
Vault (licenses)       │ month   │ $100    │ 1      │ $100
Jaeger / Monitoring    │ month   │ $50     │ 1      │ $50
CI/CD (Azure DevOps)   │ month   │ $0      │ 1      │ $0 (10K free)
Miscellaneous (DNS, ...) month   │ $100    │ 1      │ $100
───────────────────────┴─────────┴─────────┴────────┼──────────────
TOTAL MONTHLY COST:                                   │ $2,480

Per Document Cost: $2,480 / 10,000 = $0.248/doc
Annual: $2,480 × 12 = $29,760

Break-even with 20K docs/day: $29,760 / 20,000 = $0.149/doc
```

---

## 8. Decision Tree: Which Service to Call?

```
User Action
    │
    ├─ Upload document
    │  └─→ Sourcing Service
    │
    ├─ Manually enter data
    │  └─→ Approval Service (create manual entry)
    │
    ├─ View document status
    │  └─→ Read from MongoDB (read model)
    │      └─ Populated by event stream
    │
    ├─ Approve / Reject
    │  └─→ Approval Service
    │      └─ Publishes APPROVED event
    │      └─ Triggers Dissemination Service
    │
    ├─ Search for entity
    │  └─→ Elasticsearch (entity search)
    │      └─ Fallback: MongoDB text search
    │
    ├─ Get extraction template
    │  └─→ PostgreSQL (cached in Redis)
    │
    ├─ Get approved documents report
    │  └─→ Spark job results (S3)
    │      └─ Run nightly batch
    │
    └─ View audit trail
       └─→ MongoDB (document_events collection)
          └─ Complete immutable event log
```

---

## 9. Load Testing Targets & Success Criteria

```
Scenario                          │ Target            │ Success Criteria
──────────────────────────────────┼───────────────────┼─────────────────────
Ramp up to 500 concurrent users   │ 2 minutes         │ <2s P95 latency
                                  │                   │ 0 errors
──────────────────────────────────┼───────────────────┼─────────────────────
Sustain 500 users for 1 hour      │ Steady state      │ <1.5s P95 latency
                                  │                   │ CPU <70%
                                  │                   │ 0 connection pool exhaust
──────────────────────────────────┼───────────────────┼─────────────────────
Spike test: 1000 users for 5 min  │ Peak load         │ <3s P95 latency
                                  │                   │ Auto-scale to 20 pods
                                  │                   │ <1% error rate
──────────────────────────────────┼───────────────────┼─────────────────────
Bulk extraction: 10K docs/hour    │ 10K sustained     │ Kafka lag <10K msgs
                                  │                   │ CPU 60-75%
                                  │                   │ Extraction <5min each
──────────────────────────────────┼───────────────────┼─────────────────────
Database failover: Kill primary   │ 2 minutes         │ Promote standby
PostgreSQL                        │                   │ 0 data loss
                                  │                   │ <30s downtime
──────────────────────────────────┼───────────────────┼─────────────────────
SparkAir API failure: 503 error   │ Immediate         │ Circuit breaker opens
                                  │ (5 min recovery)  │ Fallback to Cognize
                                  │                   │ Queue for manual
──────────────────────────────────┼───────────────────┼─────────────────────
Network latency: Add 200ms        │ P95 + 200ms       │ Still <3s P95
                                  │                   │ Timeouts: <0.1%
```

---

## 10. Deployment Checklist

```
PRE-DEPLOYMENT
  [ ] Code review: Approval from 2 architects
  [ ] Test coverage: >80% (unit + integration)
  [ ] Security scan: No critical vulnerabilities
  [ ] Performance test: Load test results attached
  [ ] Database migration: Tested in staging
  [ ] Secrets rotated: All API keys fresh
  [ ] Feature flags: Ready for rollback

DEPLOYMENT (Blue-Green)
  [ ] Deploy to green cluster (10 mins)
  [ ] Wait for pods to be ready (health checks pass)
  [ ] Smoke tests against green (5 mins)
  [ ] Canary: Route 10% traffic to green (30 mins)
  [ ] Monitor: No increase in error rate
  [ ] Full cutover: Route 100% to green
  [ ] Verify: All metrics normal

POST-DEPLOYMENT
  [ ] Blue cluster ready for instant rollback
  [ ] Monitor for 1 hour (watch dashboard)
  [ ] Database connections stable
  [ ] No spike in error rate (0.01% threshold)
  [ ] No increase in latency (P95 within 10%)
  [ ] Team notified: Slack #deployments
  [ ] Update runbook: Lessons learned

ROLLBACK (if needed)
  [ ] Revert traffic to blue (2 mins)
  [ ] Verify blue is healthy
  [ ] Investigate failure
  [ ] Post-mortem: Root cause analysis
```

---

End of Visual Reference. Use these tables alongside the detailed documents for complete understanding.
