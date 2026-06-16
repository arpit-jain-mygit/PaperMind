# Data Collection Platform - Deployment Topology & Summary

## Section 1: Production Deployment Topology

### 1.1 Network Architecture

```
                        ┌─────────────────┐
                        │  Azure DevOps   │
                        │  (CI/CD)        │
                        └────────┬────────┘
                                 ↓
                    ┌────────────────────────┐
                    │  Docker Registry (ACR) │
                    └────────────┬───────────┘
                                 ↓
        ┌────────────────────────────────────────┐
        │        KUBERNETES CLUSTER (AKS)        │
        │                                        │
        │  ┌──────────────────────────────────┐ │
        │  │  Ingress (NGINX / Istio Gateway) │ │
        │  │  - TLS termination               │ │
        │  │  - Rate limiting                 │ │
        │  └──────────────────────────────────┘ │
        │                 ↓                      │
        │  ┌──────────────────────────────────┐ │
        │  │   Service Mesh (Istio)           │ │
        │  │   - mTLS between services        │ │
        │  │   - Circuit breaker              │ │
        │  │   - Distributed tracing          │ │
        │  └──────────────────────────────────┘ │
        │                 ↓                      │
        │  ┌──────────────────────────────────┐ │
        │  │  Spring Boot Microservices       │ │
        │  │  ┌─────────────┐                │ │
        │  │  │ Sourcing    │ ×3 replicas   │ │
        │  │  ├─────────────┤                │ │
        │  │  │ Extraction  │ ×5-20 (HPA)   │ │
        │  │  ├─────────────┤                │ │
        │  │  │ Rules       │ ×2 replicas   │ │
        │  │  ├─────────────┤                │ │
        │  │  │ Workflow    │ ×2 replicas   │ │
        │  │  ├─────────────┤                │ │
        │  │  │ Approval    │ ×2 replicas   │ │
        │  │  ├─────────────┤                │ │
        │  │  │ Disseminate │ ×2 replicas   │ │
        │  │  └─────────────┘                │ │
        │  └──────────────────────────────────┘ │
        │                                        │
        │  ┌──────────────────────────────────┐ │
        │  │  Data Storage                    │ │
        │  │  ├─ PostgreSQL StatefulSet       │ │
        │  │  │  (Primary + Read Replicas)    │ │
        │  │  ├─ MongoDB ReplicaSet           │ │
        │  │  ├─ Redis Sentinel               │ │
        │  │  └─ Elasticsearch Cluster        │ │
        │  └──────────────────────────────────┘ │
        │                                        │
        │  ┌──────────────────────────────────┐ │
        │  │  Message Queue                   │ │
        │  │  Kafka Cluster (3 brokers)       │ │
        │  │  Zookeeper (3 nodes)             │ │
        │  └──────────────────────────────────┘ │
        │                                        │
        │  ┌──────────────────────────────────┐ │
        │  │  Batch Processing                │ │
        │  │  Spark Driver + Executor Pods    │ │
        │  │  (On-demand via Spark Operator)  │ │
        │  └──────────────────────────────────┘ │
        │                                        │
        │  ┌──────────────────────────────────┐ │
        │  │  Observability                   │ │
        │  │  ├─ Prometheus (scrape metrics)  │ │
        │  │  ├─ Jaeger (distributed tracing) │ │
        │  │  ├─ Splunk Agent (log shipping)  │ │
        │  │  └─ Grafana (dashboards)         │ │
        │  └──────────────────────────────────┘ │
        │                                        │
        └────────────────────────────────────────┘
                         ↓
        ┌───────────────────────────────────────┐
        │  External Storage (S3 / Azure Blob)   │
        │  - Documents                          │
        │  - Data Lake (Bronze/Silver/Gold)     │
        │  - Backup & Archive                   │
        └───────────────────────────────────────┘
                         ↓
        ┌───────────────────────────────────────┐
        │  External Integrations                │
        │  - SparkAir (Gen AI extraction)      │
        │  - Soniq (Entity mapping)             │
        │  - Delta Sharing (Databricks)         │
        │  - Outlook (Email source)             │
        │  - Vault (Secrets)                    │
        └───────────────────────────────────────┘
```

### 1.2 High Availability Configuration

```yaml
# PostgreSQL HA Setup
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: dcp-postgres
spec:
  instances: 3  # Primary + 2 standbys
  bootstrap:
    initdb:
      database: dcp
  
  # Automatic failover
  primaryUpdateStrategy: unsupervised
  failoverDelay: 300  # Wait 5 mins before failover
  
  # Backup strategy
  backup:
    barmanObjectStore:
      wal:
        s3Credentials:
          accessKeyId:
            name: aws-s3
            key: access-key-id
      data:
        compression: gzip
    retentionPolicy: "30d"  # Keep 30 days of backups

---
# MongoDB HA Setup (ReplicaSet)
apiVersion: mongodb.com/v1
kind: MongoDB
metadata:
  name: dcp-mongo
spec:
  members: 3
  
  # Automatic failover enabled
  replication:
    enabled: true
  
  # Persistent storage
  persistent: true
  persistentVolume:
    size: 200Gi
  
  # Backup
  backup:
    enabled: true
    s3:
      bucket: dcp-mongo-backups
    schedule: "0 2 * * *"  # Daily at 2 AM
```

### 1.3 Disaster Recovery Plan

| Scenario | RTO | RPO | Strategy |
|----------|-----|-----|----------|
| **Pod Crash** | 30s | 0 | Kubernetes restart |
| **Node Failure** | 2 min | 0 | Evict pods to another node |
| **Zone Outage** | 5 min | 0 | Multi-AZ deployment |
| **Database Corruption** | 1 hour | 15 min | Point-in-time restore from S3 backup |
| **Cluster Outage** | 4 hours | 1 hour | Restore to secondary cluster |
| **Data Center Failure** | 24 hours | 1 hour | Restore from geo-redundant S3 |

**Backup & Recovery Script:**
```bash
#!/bin/bash
# Daily backup (2 AM)
0 2 * * * /scripts/backup-databases.sh

# Weekly integrity check (Sunday 3 AM)
0 3 * * 0 /scripts/test-restore-backup.sh

# Backup retention: Keep 30 days + 12 monthly backups
find /backups -mtime +30 -delete
```

---

## Section 2: Comprehensive Summary

### 2.1 Architecture Decision Summary

| Decision | Trade-offs | Rationale |
|----------|-----------|-----------|
| **Microservices** | Complexity vs Independence | Each team owns service end-to-end |
| **Kafka** | Operational overhead vs Scalability | Decouple services, handle 10K docs/day |
| **MongoDB** | Schema flexibility vs Joins | Flexible document structure for varied PDFs |
| **PostgreSQL** | Complexity vs Consistency | ACID for metadata & configuration |
| **Event Sourcing** | Storage overhead vs Auditability | Complete data lineage for compliance |
| **Hybrid Orch+Choreography** | Complexity vs Flexibility | Orchestration for approval, choreography for extraction |
| **Kubernetes** | Ops complexity vs Scalability | Auto-scale workers based on load |
| **Istio Service Mesh** | Additional latency vs Observability | mTLS, circuit breaking, distributed tracing |

### 2.2 Performance Targets vs Reality

```
Target                    | Realistic | Gap | Mitigation
--------------------------|-----------|-----|-------------
<2s UI response           | 1.5s P95  | ✅  | Query optimization, Redis cache
10K docs/day              | 12K docs  | ✅  | Spark parallel processing
500 concurrent users      | 450 users | ✅  | Connection pooling, rate limiting
<5 min doc extraction     | 3 min avg | ✅  | Async processing, stream parsing
99.5% uptime              | 99.0%     | ⚠️  | Requires blue-green deployment, SRE discipline
<1% data quality issues   | 1.5%      | ⚠️  | Human-in-the-loop for low confidence
```

### 2.3 Cost Estimation (Monthly)

```
Component              | Quantity  | Cost/Unit  | Monthly Cost
-----------------------|-----------|------------|-------------
AKS Cluster            | 1         | $0.10/hr   | $72 (always on)
VM Instances (compute) | 20 avg    | $0.40/hr   | $288 (avg 20 nodes)
PostgreSQL (managed)   | 4 vCPU    | -          | $400
MongoDB (managed)      | 2 TB      | -          | $250
Redis Cache            | 20GB      | -          | $150
S3 Storage             | 500GB     | -          | $125
Kafka (managed)        | 3 brokers | -          | $300
Data Transfer (out)    | 500GB     | $0.09/GB   | $45
Splunk Logging         | 2TB/day   | -          | $600
Other (CI/CD, DNS)     | -         | -          | $100

TOTAL (AWS/Azure)      |           |            | ~$2,330/month
                       |           |            | ~$28K/year
```

---

## Section 3: Quick Reference - Interview Topics by Depth

### 3.1 5-Minute Elevator Pitch

**For Non-Technical:**
"We're building a platform that extracts financial data from thousands of PDFs and spreadsheets daily. It uses AI to automatically extract key numbers (amounts, dates, entities), but humans review the uncertain ones before approval. The data then flows to our analytics system."

**For Engineering Manager:**
"6 microservices (sourcing, extraction, rules, workflow, approval, dissemination) running on Kubernetes. Uses Kafka for async processing and handles 10K documents/day with 99% accuracy. PostgreSQL for metadata, MongoDB for documents, Spark for batch analytics."

**For Architect:**
"Event-sourced workflow with CQRS patterns. Hybrid orchestration (Camunda for approval) + choreography (Kafka for extraction). Multi-layer caching, circuit breakers for external APIs (SparkAir), distributed tracing with Jaeger. 99.0% uptime with blue-green deployments."

### 3.2 Deep Dive Topics (for 45-min Interview)

**Ask Me About:**

1. **Handling 100MB PDFs** (Performance & Memory)
   - Stream-based processing
   - Partitioned extraction across services
   - Timeout protection

2. **50K Entities/Day with Soniq Rate Limit** (Caching & Fallback)
   - Local cache + batch API + fallback
   - Confidence scoring + manual review

3. **Document State Consistency** (Distributed Systems)
   - Event sourcing + CQRS
   - Optimistic locking
   - State machine transitions

4. **Auto-Approval vs Manual Review** (QA & Workflow)
   - Confidence thresholding
   - Sampling-based audits
   - L2 user prioritization

5. **Scaling Extraction Workers** (Kubernetes & Monitoring)
   - HPA based on Kafka lag
   - Custom metrics from Prometheus
   - Resource allocation

6. **Data Quality Guarantee** (Testing & Validation)
   - Multi-layer validation (format, rules, ML confidence)
   - Automated quality audits
   - Feedback loops

7. **Integration with SparkAir** (3rd Party APIs)
   - Abstraction via factory pattern
   - Fallback to Cognize / Deepmine
   - Parameter-driven AI model selection

8. **Compliance & Audit Trail** (Security & Governance)
   - Event sourcing for immutable log
   - Encryption at rest + in transit
   - PII masking in logs

### 3.3 Common Follow-Up Questions & Answers

**Q: How do you handle extraction failures?**

A: 3-layer approach:
1. **Retry with exponential backoff** (up to 3 times)
2. **Fallback to different vendor** (SparkAir → Cognize → Deepmine)
3. **Queue for manual extraction** (L1 user reviews original document)

All failures are tracked in DLQ (Dead Letter Queue) for post-mortem analysis.

---

**Q: How do you scale the approval workflow when L2 users are bottleneck?**

A: Multiple strategies:
1. **Parallelization**: Increase L2 user team size
2. **Prioritization**: High-value documents reviewed first (amount-based)
3. **Auto-approval threshold**: If confidence > 95%, skip L2 review (with sampling audit)
4. **Domain specialization**: Route documents to L2 users based on expertise
5. **Bulk review UI**: Show 10 documents per screen, reduce review time/doc

---

**Q: How do you ensure consistency between extracted data and approval comments?**

A: Using pessimistic locking:
```
When L2 opens document for review:
  1. Lock document (SELECT FOR UPDATE)
  2. L1 can't modify data while locked
  3. L2 reviews locked data
  4. L2 approves → document locked until dissemination
```

---

**Q: How do you handle SLA breaches (e.g., extraction taking >5 minutes)?**

A: Monitoring + Escalation:
1. **Prometheus alert** when extraction > 4.5 mins (alert threshold before SLA)
2. **Auto-escalation**: If > 5 mins, mark for priority handling
3. **Root cause analysis**: Was it Soniq latency? File size? Concurrent load?
4. **Capacity planning**: If consistently hitting limits, scale extraction workers

---

**Q: How do you prevent duplicate processing of documents?**

A:
1. **Idempotency key**: documentId + version hash
2. **Idempotency store**: Redis cache of processed keys
3. **Kafka deduplication**: topic-wide dedup if enabled
4. **Database unique constraint**: On (documentId, extractionTimestamp)

If same document sent twice:
- 1st time: Processed normally
- 2nd time: Skipped (already in idempotency store)

---

### 3.4 Technical Talking Points (by Technology)

**Spring Boot:**
- Microservice patterns (Circuit breaker, Bulkhead, Timeout)
- Spring Data JPA + MongoDB repository patterns
- Spring Cloud Stream for Kafka integration
- Actuator endpoints for health checks

**PostgreSQL:**
- Query optimization (indexes, explain plans)
- Connection pooling (HikariCP)
- Replication for HA
- Partitioning for large tables

**Kafka:**
- Topic design (partition by documentId for ordering)
- Consumer groups and lag monitoring
- Exactly-once semantics with idempotency
- Dead letter queue pattern

**Kubernetes:**
- Deployment strategies (Rolling, Blue-Green, Canary)
- StatefulSet for databases
- Custom Resource Definitions (CRDs)
- Resource requests/limits

**Apache Spark:**
- Distributed processing of 1B+ documents
- RDD vs DataFrame vs Dataset APIs
- Partitioning strategies
- Caching and optimization

**Design Patterns:**
- Event Sourcing (immutable event log)
- CQRS (separate read/write models)
- Saga (distributed transactions)
- Circuit Breaker (fault tolerance)

---

## Section 4: Final Architectural Principles

### 4.1 Trade-Offs Made

| Complexity | Scalability | Observability | Maintainability |
|-----------|-------------|---------------|-----------------|
| Moderate | High | High | High |

We chose **Microservices + Event-Driven** because:
- ✅ Scalability: Each service scales independently
- ✅ Resilience: Failure in one service doesn't cascade
- ✅ Observability: Event log provides complete audit trail
- ✅ Flexibility: Easy to add new extraction vendors
- ❌ Trade-off: More operational complexity (Kubernetes, Kafka)

### 4.2 Future Enhancements (Not in Scope)

1. **Machine Learning (ML) confidence calibration**
   - Train model to predict actual vs reported confidence
   - Use feedback from L2 reviews to improve thresholds

2. **Multi-tenancy**
   - Isolate data per tenant (different PostgreSQL schemas)
   - Tenant-specific extraction templates & rules

3. **Real-time dissemination**
   - Webhook delivery instead of batch
   - Event-driven updates to subscribers

4. **GenAI agentic workflow (RVT Use Case)**
   - Document → Extract rules → Apply rules → Generate formula
   - LLM-powered rule synthesis from handbook

---

## Section 5: 30-Second Summaries for Different Audiences

### For CTO
"Event-sourced microservices architecture handling 10K financial documents/day. Cloud-native (Kubernetes, Kafka). Multi-vendor AI extraction with fallback. Auditable (complete event history). Scales to 50K docs/day with auto-scaling workers. 99% uptime, <2s UI response."

### For Business
"Processes financial PDFs 5x faster than manual entry. 85-95% auto-extracted, rest reviewed by humans. Audit trail for compliance. Cost: ~$28K/year for 10K docs/day. Scales as needed."

### For Individual Contributor
"Explore the code: microservices in Spring Boot, events in Kafka, data in MongoDB/PostgreSQL, batch jobs in Spark. Contributing guide in docs/CONTRIBUTING.md. Onboarding Slack channel: #dcp-platform."

---

## Conclusion

This platform architecture demonstrates:

✅ **Non-functional Requirements Management**: Balanced performance, scalability, availability
✅ **Pattern Mastery**: Event sourcing, CQRS, saga, circuit breaker, event choreography
✅ **Technology Depth**: Spring Boot, Kafka, Kubernetes, Spark, PostgreSQL, MongoDB
✅ **Production Readiness**: HA/DR, security, observability, incident response
✅ **Team Scalability**: Microservices allow independent teams to move fast

**Interview Readiness**: This design covers all major architect-level topics (distributed systems, scaling, resilience, security, cost optimization). Practice explaining trade-offs clearly and be ready to deep-dive any component.
