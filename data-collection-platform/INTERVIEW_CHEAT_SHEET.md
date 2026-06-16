# Data Collection Platform - Interview Preparation Cheat Sheet

## Numbers to Memorize

```
SCALE                          PERFORMANCE                BUSINESS
10K docs/day                   <2s UI (P95: 1.5s)         $0.25/doc cost
50K entities/day               3min extraction (P95: 3m)   $28K/year for 10K
500+ concurrent users          800ms approval (P95)        $500K+ annual savings
1000 Kafka events/sec          50ms entity lookup (cached) $2M+ new products
5-20 extraction workers        99.0% uptime               60% manual effort reduction
6 microservices                4-hour RTO/1-hour RPO      0 critical incidents/18mo
```

## The Three Hardest Problems (Know Cold)

### Problem 1: Distributed State Consistency
**Q**: "How do you prevent data loss when L1 modifies and L2 simultaneously reviews?"

**A**: Event sourcing. Every state change → immutable event in Kafka. Read model (MongoDB) updates async.
- Events ordered per documentId (prevent race conditions)
- Complete audit trail (regulators can replay)
- Temporal queries (what was status on date X?)
- Trade-off: eventual consistency (seconds lag), but worth it

**Code memory**:
```java
// Every state change = event
DocumentEvent event = new DocumentEvent(docId, "APPROVED", timestamp);
mongoTemplate.insert(event, "document_events");
kafkaTemplate.send("document-events", event);
```

---

### Problem 2: Entity Mapping at Scale (50K/day vs 1000 req/min limit)
**Q**: "Soniq API rate limit is 1000 req/min but you need 50K entities/day. How?"

**A**: Layered strategy
1. **Cache**: Redis L1 cache, 85% hit rate (only 7.5K misses from 50K)
2. **Batch**: Batch 10 entities per API call (7.5K → 750 calls, 10x reduction)
3. **Fallback**: Circuit breaker → fuzzy match when Soniq down
4. **Async**: Background job retries unresolved entities every 5 minutes

**Result**: 92% success, 90% fewer API calls, zero bottleneck

**Code memory**:
```java
@Cacheable(value = "entity-mapping", key = "#entityName")
public Optional<EntityMapping> getMapping(String entityName) { /* batch + fallback */ }
```

---

### Problem 3: AI Quality Guarantee (85% baseline → 99.2% final)
**Q**: "SparkAir is only 85% accurate. How do you guarantee <1% error rate?"

**A**: Multi-layer validation
1. **Pre-validation**: Format/structure checks, PDF integrity
2. **Confidence routing**: 
   - >0.9 → auto-approve (log for audit)
   - 0.7-0.9 → L2 review
   - <0.7 → manual extraction
3. **Rules validation**: Business logic (amount > 0, date valid, entity mappable)
4. **Sampling audit**: Hourly verify 100 auto-approved docs, alert if <99% accuracy
5. **Feedback loops**: Recalibrate thresholds based on actual vs predicted

**Result**: 99.2% end-to-end accuracy, 60% less manual review

---

## One-Sentence Explanations (Memorize These)

| Topic | Answer |
|-------|--------|
| **Why microservices?** | Each team owns 1-2 services, no shared code ownership, fault isolation |
| **Why Kafka?** | 1000 events/sec throughput, partitioning ensures order (per docId), replication for reliability |
| **Why PostgreSQL + MongoDB?** | Postgres for ACID metadata/templates, Mongo for flexible document schema |
| **Why event sourcing?** | Complete audit trail, temporal queries, zero race conditions |
| **Why circuit breaker?** | Fail fast when external API down (SparkAir), fallback to Cognize |
| **Why Kubernetes?** | Auto-scale workers based on queue depth (5→50 pods), cloud-agnostic |
| **Why Redis cache?** | Sub-50ms latency, 85% hit rate on entity mappings, reduces API calls |
| **Why Splunk logging?** | 2TB/day logs searchable in seconds, 99% incident resolution via correlation IDs |
| **Why blue-green deployment?** | Zero-downtime releases, instant rollback if issues detected |
| **Why RBAC?** | L1 users can't approve (only L2), audit log captures who did what |

---

## Pattern References (When Asked About Design Patterns)

```
Creational:
- Factory: Extract engines (SparkAir vs Cognize vs Deepmine)
- Builder: Complex extraction requests with optional fields
- Singleton: ConfigService, Soniq client

Structural:
- Adapter: Normalize different file parsers (PDF, Excel, CSV)
- Facade: DataExtractionFacade orchestrates sourcing→extraction→storage
- Decorator: Add logging/caching/retry to services (AOP)
- Proxy: Lazy load Soniq client

Behavioral:
- Strategy: Multiple extraction strategies (manual vs AI)
- State: Document lifecycle (SOURCED → EXTRACTED → APPROVED → PUBLISHED)
- Observer: Notify services when document status changes (Kafka listeners)
- Chain of Responsibility: Validation chain (format → business rules → quality)
- Template Method: AbstractExtractionTemplate (common steps, algorithm variations)
- Command: ExtractionJob encapsulated for queue

Microservices:
- API Gateway: Rate limiting, RBAC, request logging
- Circuit Breaker: SparkAir failure → fallback to Cognize
- Saga: Multi-service approval workflow (distributed transaction)
- Event Sourcing: Immutable event log = source of truth
- CQRS: Separate read model (documents view) from write model (events)
- Bulkhead: Thread pool isolation per extraction engine
```

---

## Technology Stack Decision Tree

```
QUESTION                               ANSWER                    WHY
-----------------------------------    -----------------------   -----
How to handle 1000 events/sec?         Apache Kafka             Partitioning, replication
Multiple document sources?             S3, Kafka, Email, APIs   Multi-channel sourcing
Flexible document schema?              MongoDB                  No fixed schema per doc type
Complex metadata queries?              PostgreSQL               ACID, complex joins
Sub-50ms cache latency?                Redis                    In-memory, TTL support
Entity search across 50K+ docs?        Elasticsearch            Full-text, aggregations
Distributed extraction jobs?           Apache Spark             Parallel processing, resilience
Multi-step approval workflow?          Camunda BPMN             Visual process design
Orchestrate 6 services?                Kubernetes               Auto-scale, declarative
Secure communication between svcs?     Istio mTLS               Service mesh encryption
Observability across services?         Jaeger + Prometheus      Distributed tracing + metrics
```

---

## Common Interview Questions & 30-Second Answers

**Q: "Walk me through your architecture"**

A: 6 microservices on Kubernetes. Users upload documents → Sourcing service ingests from S3/Kafka/email → Extraction service calls SparkAir AI (with Cognize fallback) → Rules engine applies business logic and entity mapping → Workflow service assigns to L2 user for approval → Approval service captures decision → Dissemination publishes results. All state changes are immutable events in Kafka. Read model in MongoDB updated asynchronously. This gives us complete audit trail and zero race conditions.

---

**Q: "How do you handle 10K documents simultaneously?"**

A: Kafka queue absorbs the spike. Sourcing service drops 10K documents into Kafka topic partitioned by documentId. Extraction workers (5-20 pods, auto-scaled based on queue lag) consume in parallel. Each service is stateless so we can scale independently. Database scales via read replicas and connection pooling. Bottleneck is usually Soniq entity mapping, but we've solved that with caching and batching.

---

**Q: "What if SparkAir goes down?"**

A: Circuit breaker opens, we immediately fallback to Cognize API (same interface). If both fail, document gets queued for manual extraction and L1 user is notified. All failures go to dead-letter queue for analysis. Meanwhile, other services keep running—extraction failure doesn't affect approval or dissemination services (fault isolation via bulkhead pattern).

---

**Q: "How do you ensure data quality?"**

A: Multi-layer validation. Pre-validation checks (PDF integrity, format). Then confidence routing: high-confidence extracted data auto-approved (logged for audit), medium-confidence goes to L2 review, low-confidence queues for manual extraction. Rules engine validates business logic (amount > 0, dates valid, entities resolvable). Hourly sampling audits verify auto-approved documents. If accuracy drops below 99%, we alert and recalibrate thresholds.

---

**Q: "How do you secure sensitive financial data?"**

A: TLS 1.2+ for all communication. AES-256 encryption at rest (MongoDB). RBAC at API gateway (L1 vs L2 permissions). Immutable event log for audit trail. Secrets in Vault with quarterly rotation. PII masking in logs. Data classification, access control logs. Complete lineage tracking so regulators can audit any document's history.

---

**Q: "How do you deploy without downtime?"**

A: Blue-green deployment. New version deploys to "green" cluster while "blue" handles traffic. Once green is healthy (health checks pass, smoke tests succeed), we switch traffic gradually (canary: 10% → 50% → 100% over 30 minutes). If issues detected, instant rollback to blue. Zero downtime, and blue remains ready for instant rollback.

---

**Q: "What's your biggest achievement on this project?"**

A: Entity mapping solution. Soniq API bottleneck could've killed the entire project, but I designed a layered approach: local cache (85% hit), batch API calls (10x reduction), circuit breaker fallback, async resolution. Went from 0 capability (rate-limited) to 92% success rate, 90% fewer API calls. That unblocked the whole pipeline.

---

**Q: "What would you do differently?"**

A: Nothing major. Maybe stronger eventual consistency guarantees from the start to reduce user confusion (they see status lag), but event sourcing trade-off is correct. Maybe use Temporal instead of Camunda for workflow (more cloud-native), but Camunda's BPMN visual tools are great for non-technical stakeholders.

---

## Red Flags to Avoid

❌ **Don't say**: "We used microservices because they're trendy"
✅ **Instead**: "Each service scaled independently based on load—extraction workers 5→50 pods, others stayed at 2-3"

❌ **Don't say**: "Event sourcing for perfect auditability"
✅ **Instead**: "Event sourcing trades strong consistency for auditability and temporal queries—data lags seconds but is immutable"

❌ **Don't say**: "We scaled to 20K docs/day"
✅ **Instead**: "We designed for 2x capacity without architectural changes. Theory says 20K is doable; we've validated 10K in production"

❌ **Don't say**: "We use Redis for everything"
✅ **Instead**: "Redis for sub-50ms cache on entity mappings (85% hit rate) and session state. PostgreSQL for consistent metadata"

❌ **Don't say**: "No incidents in production"
✅ **Instead**: "Zero critical incidents in 18 months. Had 2 minor incidents (resolved <30 mins) that led to improved monitoring"

---

## Pre-Interview Checklist

- [ ] Can draw architecture diagram from memory (6 services, Kafka, databases)
- [ ] Know the 3 hardest problems cold (entity mapping, state consistency, AI quality)
- [ ] Explain trade-offs: eventual consistency, single vs polyglot DB, orchestration vs choreography
- [ ] Memorize key numbers: 10K docs/day, 500 users, <2s latency, 99% uptime, $28K/year
- [ ] Know why each technology choice (Kafka not RabbitMQ, Mongo not Cassandra, etc.)
- [ ] Can discuss design patterns (event sourcing, circuit breaker, saga, CQRS)
- [ ] Ready to deep-dive on 1-2 topics (caching, distributed tracing, deployment)
- [ ] Prepared for "what would you do differently" question
- [ ] Have examples of how you handled trade-offs (eventual consistency, cost vs feature)
- [ ] Practice 2-minute summary without notes

---

## Talking Points by Interview Round

### Round 1 (Recruiter/Screening - 30 mins)
Focus: Scope and impact
- Platform handles 10K+ financial docs/day
- 6 microservices on Kubernetes
- Achieved 99% uptime, <2s latency
- Enabled $2M+ in downstream products
- Reduced manual effort by 60%

### Round 2 (Technical Screen - 1 hour)
Focus: Design and implementation
- Deep dive on 1 of the 3 hardest problems
- Technology stack justification
- Performance optimization (caching, batching)
- Resilience patterns (circuit breaker, fallback)

### Round 3 (Architect Interview - 2 hours)
Focus: Trade-offs and reasoning
- All 3 hardest problems with detailed solutions
- Architecture decisions (why microservices, why event sourcing)
- Trade-offs (eventual vs strong consistency, cost vs feature)
- How to scale to 2x (would you change anything?)
- Security and compliance considerations

### Round 4 (Hiring Manager - 30 mins)
Focus: Impact and future
- Business results ($2M+ products, cost savings)
- Team collaboration (5 teams independently shipping)
- Operational excellence (0 critical incidents)
- Growth and learning opportunities
- How you'd approach different scaling challenges

---

## Glossary (Quick Reference)

| Term | Meaning |
|------|---------|
| **Event Sourcing** | Store all state changes as immutable events → can replay history |
| **CQRS** | Command Query Responsibility Segregation = separate write/read models |
| **Circuit Breaker** | Fail fast when external API down (SparkAir) → fallback (Cognize) |
| **Bulkhead** | Isolate thread pools so one service failure doesn't cascade |
| **Eventual Consistency** | Replicas catch up after seconds (not instant) |
| **HPA** | Horizontal Pod Autoscaler = Kubernetes auto-scales pods |
| **Kafka Lag** | Messages waiting in queue (metric for scaling) |
| **Dead Letter Queue** | Place for failed messages that can't be retried |
| **Blue-Green** | Two identical environments, switch traffic between them |
| **RTO/RPO** | Recovery Time/Recovery Point Objectives (disaster recovery metrics) |
| **Idempotency** | Same request twice = same result (safe to retry) |
| **Saga** | Multi-service transaction with compensation (distributed 2PC) |

---

## Final Confidence Check

Before the interview, test yourself:

**Can you explain...**
✓ Why entity mapping was hard and how you solved it?
✓ What event sourcing is and why it matters?
✓ How distributed systems stay consistent without locks?
✓ How circuit breakers prevent cascading failures?
✓ Why you chose Kafka over RabbitMQ?
✓ What eventual consistency means and when it's okay?
✓ How Kubernetes auto-scales based on metrics?
✓ What CQRS is and why you used it?
✓ How blue-green deployments achieve zero downtime?
✓ What the three hardest problems were and your solutions?

If you can articulate any 5 of these clearly, you're ready. If you can do all 10, you'll crush the interview. 🚀

---

## Last-Minute Tips

**Minute before interview:**
- Deep breath. You've shipped a complex, production system.
- Remember: They want to hear about YOUR thinking, not perfect answers
- Technical depth matters more than breadth—go deep on 2-3 topics
- It's okay to say "I don't know" followed by "here's how I'd figure it out"

**During interview:**
- Draw diagrams if they ask for architecture
- Use numbers (10K docs/day, 99% uptime) to anchor discussion
- Explain trade-offs (eventual consistency, cost vs feature)
- Show humility about what could be better

**After interview:**
- Follow up with thank you
- Ask about observability/reliability culture (you care about production)
- Ask about scale challenges (you think big)
