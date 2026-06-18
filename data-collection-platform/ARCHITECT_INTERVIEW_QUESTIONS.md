# Architect-Level Interview Questions for Data Collection Platform

Based on: *"Architected and delivered a cloud-native Data Collection Platform – an AI-first, event-driven system processing mission-critical structured, semi-structured, and unstructured financial data (audited financials, debt instruments, legal agreements, forecasts) at scale. Designed end-to-end solution handling 10K+ documents daily with 99% uptime, <2s latency, and enterprise-grade security/auditability enabling downstream ratings analytics products."*

---

## 🎯 Questions You WILL Be Asked

### **Category 1: System Architecture & Trade-offs**

#### Q1: "Walk us through your architecture decision. Why microservices instead of a monolith?"
**Why This Matters**: Tests if you understand trade-offs, not just buzzwords
**What They're Listening For**:
- Independent scaling of extraction workers (5→50 pods)
- Team autonomy (5 teams owning separate services)
- Fault isolation (extraction failure doesn't crash approval)
- But acknowledge the downsides (distributed transactions, eventual consistency)

**Your Answer Should Include**:
```
Monolith would be simpler for 10K docs/day, but:
- Extraction workers need 10x scaling while Rules engine stays at 2
- 5 teams needed independent deployment (microservices = faster shipping)
- SparkAir API timeout shouldn't crash L2 approval service (fault isolation)

Trade-off: Distributed transaction complexity (solved via event sourcing + saga)
Price: Operational complexity, debugging across service boundaries
Worth it because: Team velocity + independent scaling + resilience
```

**Reference**: [ARCHITECTURE_DESIGN_QA.md - Q3: Design Patterns](ARCHITECTURE_DESIGN_QA.md#q3-design-patterns-across-all-levels)

---

#### Q2: "You chose event sourcing. Walk me through why, and what problems does it introduce?"
**Why This Matters**: Event sourcing is complex; they want to know you understand the cost
**What They're Listening For**:
- Problem solved: Race conditions when L1 edits while L2 reviews
- Benefit: Complete audit trail (regulatory requirement)
- Trade-off: Eventual consistency (document status lags real state by seconds)
- How you mitigated: Immutable events ordered per documentId

**Your Answer Should Include**:
```
Problem: Document state modified across 5 services (sourcing → extraction → rules → workflow → approval)
Without event sourcing: Optimistic locking fails under concurrent L1/L2 updates

Solution: Every state change = immutable event in Kafka
- Events ordered per documentId (prevents race conditions)
- Read model (MongoDB) updated asynchronously
- Can replay history for temporal queries ("status on Jan 15?")
- Complete audit trail for compliance

Trade-off: Users see eventual consistency (updates lag 2-5 seconds)
Why acceptable: Regulators need auditability more than real-time consistency
```

**Reference**: [TECHNICAL_DEEP_DIVES.md - Problem 1: Distributed State Consistency](TECHNICAL_DEEP_DIVES.md)

---

#### Q3: "You mention 99% uptime. How realistic is that, and what would you actually target?"
**Why This Matters**: Tests if you're grounded in reality vs. SLA marketing
**What They're Listening For**:
- 99.5% = 4 hours downtime/month (sounds reasonable)
- 99% = 7 hours downtime/month (more realistic)
- Understanding the work required (blue-green deployments, HA everywhere)
- Cost of uptime (diminishing returns after 99%)

**Your Answer Should Include**:
```
99.5% is the target, but realistic is 99.0%:

Budget breakdown (hours/month):
- Database maintenance: 2 hours
- Kubernetes cluster upgrade: 1 hour
- Emergency bug fixes: 0.5-1 hour
- Unplanned outages: varies

To achieve 99.5%+:
- Blue-green deployments (zero-downtime releases)
- PostgreSQL HA (primary + 2 replicas)
- MongoDB replicaset with automatic failover
- Cross-zone deployment (survive AZ failure)
- SRE on-call discipline

Trade-off: Every 0.1% uptime improvement = exponential cost
99% is the "sweet spot" for financial data systems (business can tolerate 7 hours/month)
```

**Reference**: [DEPLOYMENT_AND_SUMMARY.md - HA/DR Configuration](DEPLOYMENT_AND_SUMMARY.md)

---

### **Category 2: Scale & Performance**

#### Q4: "10K documents per day. Walk me through how you'd scale to 100K documents without redesign."
**Why This Matters**: Architect-level question about future-proofing
**What They're Listening For**:
- Horizontal scaling (add more workers)
- Database bottleneck thinking
- Cost implications
- What WOULD require redesign

**Your Answer Should Include**:
```
10K → 100K documents (10x scale):

Stateless services: Easy
  - Extraction workers: 5→50 pods (HPA scales on queue lag)
  - Rules engine: 2→20 pods (stateless, add more)
  - Approval service: 2→20 pods (stateless)

Message queue: Scales linear
  - Kafka: 3 brokers handle 1000 events/sec, could do 10K events/sec
  - Just add more partitions (documentId partitioning ensures ordering)

Database: HERE'S THE PROBLEM
  - PostgreSQL: Current ~1500 ops/sec, 100K docs/day needs 3000 ops/sec
  - Solution: Read replicas, write batching, connection pooling tuning
  - OR: Shard users by entity (multi-tenant approach)
  
  - MongoDB: Handles volume fine, but need to shard if >1TB
  - Solution: Shard by date or documentId

Bottleneck: Soniq entity mapping (1000 req/min limit)
  - Solution: Already solved (cache 85% hit, batch 10x reduction)
  - Could handle 50K entities/day without scaling Soniq

Cost Impact:
  - Infrastructure: ~$30K → ~$60K/year (roughly linear)
  - Not a redesign; mostly scale knobs

What WOULD require redesign:
  - <500ms latency (current 2s, would need caching redesign)
  - Real-time dissemination (current batch, would need streaming)
  - Multi-region (current single region, would need sync/eventual consistency story)
```

**Reference**: [DEPLOYMENT_AND_SUMMARY.md - Scalability & Resource Allocation](DEPLOYMENT_AND_SUMMARY.md)

---

#### Q5: "Your cache hit rate is 85%. How did you arrive at that number, and what happens if it drops?"
**Why This Matters**: Tests if you've actually optimized, not just claimed optimization
**What They're Listening For**:
- Data-driven approach (not guessing)
- Understanding of cache metrics
- Graceful degradation
- Fallback strategy

**Your Answer Should Include**:
```
85% hit rate comes from:
  - Entity mapping: Same 500 entities appear in 85% of documents (Pareto distribution)
  - Session state: 85% of users have active session within TTL (1 hour)
  - Template cache: Same 20 templates used for 85% of documents

How we validated:
  - Metrics: Prometheus scrapes cache hit/miss rates
  - Logs: CloudWatch shows hit_rate = hits / (hits + misses)
  - Real data: Analyzed first month of traffic

If hit rate drops (e.g., to 60%):
  - Symptom: Soniq API call volume spikes (more cache misses)
  - Trigger: Alert when hit_rate < 75%
  
Response:
  1. Check TTL tuning (maybe cache expired too early)
  2. Check data distribution change (new entity types added)
  3. Increase cache size (if memory available)
  4. Review entity popularity (maybe top 100 entities changed)

Graceful degradation:
  - 50% hit rate: Soniq still handles increased load (circuit breaker kicks in)
  - 20% hit rate: Fallback to fuzzy matching, queue for async resolution
  - System keeps running, just slower
```

**Reference**: [TECHNICAL_DEEP_DIVES.md - Entity Mapping Problem & Caching Strategy](TECHNICAL_DEEP_DIVES.md)

---

### **Category 3: Data Consistency & Resilience**

#### Q6: "Describe a failure scenario. SparkAir API goes down during extraction. Walk me through what happens step-by-step."
**Why This Matters**: Tests operational thinking and resilience patterns
**What They're Listening For**:
- Circuit breaker pattern understanding
- Fallback mechanism
- No cascading failures
- User impact (transparent vs visible)

**Your Answer Should Include**:
```
Scenario: SparkAir returns 503 (Service Unavailable)

Timeline:

T=0: First failure
  - Extraction service calls SparkAir, gets 503
  - Circuit breaker records failure (count = 1)

T=1-10 seconds: More failures pile up
  - Multiple extraction workers hit SparkAir
  - Circuit breaker now tracking (failures: 5/10 calls)
  - Once 50% failure rate → CIRCUIT BREAKER OPENS

T=10-30 seconds: Circuit breaker OPEN
  - New extraction requests FAIL FAST (don't call SparkAir)
  - Fallback: Route to Cognize API (same interface)
  - If Cognize also fails → Queue for manual extraction
  - User sees: "Document queued for manual extraction, expected 24 hours"

T=30-60 seconds: SparkAir recovers
  - Circuit breaker enters HALF_OPEN state
  - Test 1 request to SparkAir
  - If succeeds → CLOSE circuit breaker
  - Resume normal operation

During outage:
  - Documents don't disappear (queued in Kafka)
  - Approval service unaffected (bulkhead isolation)
  - L2 users can still approve queued documents
  - No cascading failure to other services

Data consistency:
  - Event log in Kafka is source of truth
  - If extraction fails: status = "MANUAL_REQUIRED"
  - User notified via WebSocket
  - Document can be manually extracted anytime

Cost of this architecture:
  - Must maintain Cognize relationship (backup vendor)
  - Must monitor circuit breaker (alerts if open >5 mins)
  - Manual extraction queue adds overhead
```

**Reference**: [ARCHITECTURE_DESIGN_QA.md - Q8: Pitfall 4 - No Circuit Breaker](ARCHITECTURE_DESIGN_QA.md#pitfall-4-no-circuit-breaker-for-external-apis)

---

#### Q7: "How do you handle consistency when L1 user is entering data while L2 is reviewing? Walk me through the edge case."
**Why This Matters**: Tests understanding of distributed data consistency
**What They're Listening For**:
- Conflict resolution strategy
- Pessimistic vs optimistic locking
- Version control
- User experience

**Your Answer Should Include**:
```
Scenario: L1 user editing document while L2 is reviewing

With Event Sourcing (my approach):

States:
  SOURCED → EXTRACTION_COMPLETE → PENDING_REVIEW → APPROVED/REJECTED

During EXTRACTION_COMPLETE:
  - L1 CAN edit (user can correct extraction confidence < 0.7)
  
During PENDING_REVIEW:
  - L1 CANNOT edit (document locked by state machine)
  - UI prevents editing (greyed out fields)
  - L1 sees: "Waiting for reviewer approval"

Edge case: What if both try simultaneously?

Timeline:
  T=0: Document state = EXTRACTION_COMPLETE (L1 can edit)
  T=1: L1 submits for review (state → PENDING_REVIEW)
  T=2: L2 opens document (sees PENDING_REVIEW, can approve)
  T=3: L1 tries to edit field (UI prevented, but API check)
    → API returns 409 Conflict: "Document is PENDING_REVIEW"
    → L1 sees message: "This document is being reviewed. Check back later"

Database implementation:
  UPDATE documents 
  SET status = 'APPROVED', version = version + 1
  WHERE id = '...' 
    AND status = 'PENDING_REVIEW'  ← Prevents race condition
    AND version = 5  ← Optimistic locking
  
  If returned rows = 0 → Conflict occurred
  → Reload from DB and show user updated state

Kafka event:
  Event: { type: "APPROVED", documentId: "...", version: 6, timestamp: "..." }
  → Immutable in event log
  → Can replay to reconstruct state anytime

User experience:
  - No data loss (event log is source of truth)
  - Clear feedback (409 error + message)
  - Self-healing (reload shows correct state)
  - Audit trail (who did what, when)
```

**Reference**: [TECHNICAL_DEEP_DIVES.md - Problem 1: State Consistency](TECHNICAL_DEEP_DIVES.md)

---

### **Category 4: Security & Compliance**

#### Q8: "You mention 'enterprise-grade security/auditability.' What does that actually mean? Walk me through a compliance audit."
**Why This Matters**: Financial data = regulatory scrutiny; they want to know you understand compliance
**What They're Listening For**:
- Specific regulatory requirements (SOX, GDPR, etc.)
- Audit trail implementation
- Encryption strategy
- Access control

**Your Answer Should Include**:
```
Enterprise-grade security for financial data:

1. AUDITABILITY (Regulator asks: "Who changed this document?")
   - Event log in Kafka: Every state change immutable
   - Query: SELECT * FROM document_events WHERE documentId = "..." ORDER BY timestamp
   - Result: [SOURCED@10:00 by system, EXTRACTED@10:05 by SparkAir, 
              REVIEWED@14:22 by L2_user_john, APPROVED@14:25 by L2_user_jane]
   - Regulator satisfied: Complete chain of custody

2. ENCRYPTION (Regulator asks: "Is sensitive data protected?")
   - At rest: MongoDB encrypted (AES-256)
   - In transit: TLS 1.2+ (S3, APIs, Kafka)
   - Secrets: HashiCorp Vault (API keys, DB passwords)
   - Keys: Rotated quarterly
   - PII: Redacted from logs (SSN masked as **-**-1234)

3. ACCESS CONTROL (Regulator asks: "Who can access this?")
   - RBAC: L1 user can enter, L2 can approve (API enforces)
   - Audit log: Every API call logged (user_id, action, timestamp, IP)
   - Breakglass: Admin can temporarily elevate (logged, needs approval)

4. DATA INTEGRITY (Regulator asks: "Can data be modified without trace?")
   - Immutable event log (can't retroactively change)
   - Version control (document.version increments, prevents rollback)
   - Hash verification (sourceFile.hash matches)
   - Checksums on exports (catch corruption)

Compliance audit process:
  Regulator: "Show me all documents reviewed by john in Q1"
  Query: SELECT * FROM documents WHERE reviewed_by = 'john' AND review_date BETWEEN '2024-01-01' AND '2024-03-31'
  
  Regulator: "Show me the extraction details for this document"
  Query: SELECT * FROM document_events WHERE documentId = '...' ORDER BY timestamp
  
  Regulator: "Prove this wasn't modified after approval"
  Check: document.approved_at = timestamp, approved_version = X
         No events after this timestamp for this documentId
         Hash matches original file

Regulatory compliance:
  - SOX: Audit trail required ✅ (event log)
  - GDPR: Data deletion required ✅ (soft-delete + purge events)
  - HIPAA: Encryption required ✅ (AES-256 at rest, TLS in transit)
  - FINRA: Record retention ✅ (S3 archives for 7 years)
```

**Reference**: [TECHNICAL_DEEP_DIVES.md - Security Architecture](TECHNICAL_DEEP_DIVES.md#section-4-security-architecture-details)

---

### **Category 5: Team & Organizational**

#### Q9: "You mention this 'enabled downstream ratings analytics products.' How did you structure the team to deliver this? What's the service ownership model?"
**Why This Matters**: Architect role includes team structure
**What They're Listening For**:
- Service ownership boundaries
- Team size and structure
- Communication patterns
- Deployment autonomy

**Your Answer Should Include**:
```
Team structure: 5 squads, 1 platform team

Squad 1: Sourcing Team (3 engineers)
  - Owns: Sourcing service
  - Responsible: S3, Kafka, email ingestion
  - Deployment: Independent (their own Terraform)
  - Contract: Publishes to "document-sourced" Kafka topic
  
Squad 2: Extraction Team (4 engineers)
  - Owns: Extraction service
  - Responsible: SparkAir/Cognize integration
  - Deployment: Independent (auto-scales 5→50 pods)
  - Contract: Consumes "document-sourced", publishes "extraction-complete"
  
Squad 3: Rules & Quality Team (3 engineers)
  - Owns: Rules engine, quality validation
  - Responsible: Business logic, entity mapping
  - Deployment: Independent
  
Squad 4: Workflow & Approval Team (3 engineers)
  - Owns: Camunda orchestration, L1/L2 review
  - Responsible: Human workflows, escalations
  
Squad 5: Dissemination Team (2 engineers)
  - Owns: Export, publishing, downstream delivery
  - Responsible: S3, webhooks, subscriber notifications

Platform Team (2 engineers):
  - Observability (Jaeger, Prometheus, Splunk)
  - Deployment pipeline (Kubernetes, CI/CD)
  - Shared infrastructure (PostgreSQL, MongoDB, Redis)
  - On-call rotation

Communication:
  - Async-first (event-driven, not sync calls between squads)
  - Kafka topics = contracts between teams
  - Weekly architecture sync (15 mins)
  - Slack for urgent issues

Deployment autonomy:
  - Each squad deploys independently (no cross-team approval needed)
  - Platform team maintains staging environment
  - Canary: 10% traffic for 30 mins before full rollout
  - Instant rollback if health checks fail
  
Benefits:
  - 5 teams shipping in parallel (6 week → 2 week release cycles)
  - Each team owns their service (incentive to keep it reliable)
  - Loose coupling via events (team changes don't block others)

Challenges addressed:
  - Schema versioning: Kafka topics have versioned messages
  - Debugging: Correlation IDs across all services
  - Consistency: Event sourcing ensures eventual consistency
```

**Reference**: [DEPLOYMENT_AND_SUMMARY.md - Architectural Principles & Trade-offs](DEPLOYMENT_AND_SUMMARY.md)

---

#### Q10: "Tell me about a time you had to say 'no' to a feature request. How did you justify it architecturally?"
**Why This Matters**: Real-world architect decision-making
**What They're Listening For**:
- Trade-off thinking
- Stakeholder management
- Technical constraints
- Timeline/cost considerations

**Your Answer Should Include**:
```
Request: "We need real-time document status updates (instead of eventual consistency)"

Business rationale: 
  "L2 users waiting for extraction, want live updates instead of 5-second lag"

My analysis:
  Current: Event sourcing + CQRS (eventually consistent, 2-5s lag)
  
  To achieve real-time:
  Option 1: Remove event sourcing, use direct DB updates
    - Trade-off: Lose auditability (lose regulatory compliance)
    - Impact: Can't replay history, can't answer "what changed?"
    - Risk: HIGH (compliance violation)
  
  Option 2: Keep event sourcing, add WebSocket subscribers
    - Trade-off: More infrastructure (WebSocket servers, message queue)
    - Cost: +$5K/month (servers + management)
    - Timeline: 3 weeks dev (tech debt on other features)
    - Benefit: Faster UI updates
  
  Option 3: Educate users (current state is fine)
    - Trade-off: Slight UX friction (5-second lag)
    - Benefit: Regulatory compliance, audit trail, cost efficiency

Decision: Option 3 (accept lag)

Justification:
  - Our system handles financial data (SOX requires auditability)
  - 5-second lag is acceptable for document status
  - Cost/benefit not justified ($5K/month for 5s improvement)
  - Team bandwidth better spent on data quality improvements
  
How I communicated:
  - Showed data: L2 users approve 100+ docs/day, each takes 2 mins to review
  - 5s lag << 2 mins review time (imperceptible to user)
  - Offered alternative: UI polling instead of WebSocket (no backend cost)
  - Prioritized: Real-time quality metrics dashboard (bigger ROI)

Result:
  - Stakeholders understood trade-off
  - Built quality metrics dashboard instead
  - L2 users happy (real-time quality feedback > real-time status)
```

**Reference**: [DEPLOYMENT_AND_SUMMARY.md - Architecture Decisions & Trade-offs](DEPLOYMENT_AND_SUMMARY.md)

---

### **Category 6: Technology Choices**

#### Q11: "You chose Kafka. Why not RabbitMQ or other message queues? Walk me through the decision."
**Why This Matters**: Tests technology evaluation methodology
**What They're Listening For**:
- Criteria-based decision (not "everyone uses it")
- Understanding of trade-offs
- Requirements-driven
- Knowledge of alternatives

**Your Answer Should Include**:
```
Requirements:
  1. High throughput (1000+ events/sec)
  2. Ordering guarantee (document state changes in order)
  3. Replayability (can re-process events if rules change)
  4. Long retention (audit trail for 7 years)
  5. Multi-subscriber (5 services consuming same event)
  6. Partitioning (scale horizontally)

Evaluation:

KAFKA:
  ✅ Throughput: 1M+ events/sec per broker (we need 1K)
  ✅ Ordering: Per-partition ordering (partition by documentId)
  ✅ Replayability: Full event log persisted (offset-based)
  ✅ Retention: Configurable (7 years = $$ on S3 tiered storage)
  ✅ Multi-subscriber: Consumer groups (each service reads independently)
  ✅ Partitioning: Native (auto-scale by adding partitions)
  ❌ Complexity: Requires ZooKeeper, more ops burden
  ❌ Cost: $300/month for managed Kafka

RABBITMQ:
  ✅ Simplicity: Easier to operate (single binary)
  ✅ Cost: $100/month
  ✅ Throughput: 50K events/sec (sufficient)
  ❌ Ordering: Per-queue not guaranteed across messages
  ❌ Replayability: No built-in event log (custom solution needed)
  ❌ Retention: Message expires, not 7-year archive
  ❌ Multi-subscriber: Need separate queue per subscriber (coupling)

SQS (AWS):
  ✅ Serverless (no ops overhead)
  ✅ Cost: Cheaper initially
  ❌ Ordering: FIFO mode slow, standard mode unordered
  ❌ Replayability: No log persistence (messages deleted after processed)
  ❌ 7-year retention: Expensive ($$$)
  ❌ Max 40K requests/sec per queue

Decision: Kafka

Why:
  - Requirement #2 (ordering) eliminates RabbitMQ and SQS
  - Requirement #4 (7-year audit trail) requires replayable event log
  - Kafka is the only option that satisfies both
  - $200/month premium is worth it for compliance + scale

Trade-off accepted:
  - Operational complexity (mitigated by managed Kafka service)
  - Learning curve for team (offset management, consumer groups)
```

**Reference**: [ARCHITECTURE_DESIGN_QA.md - Technology Stack & Decisions](ARCHITECTURE_DESIGN_QA.md)

---

#### Q12: "Why MongoDB for documents and PostgreSQL for metadata? Why not just one database?"
**Why This Matters**: Polyglot persistence is controversial; they want to know you thought deeply
**What They're Listening For**:
- Schema variability understanding
- Query patterns
- Consistency requirements
- Maintenance burden

**Your Answer Should Include**:
```
Document store vs Metadata store: Different access patterns

DOCUMENTS (MongoDB):
  - Variable schema (PDF extraction ≠ Excel extraction ≠ email)
  - Examples:
    {
      documentId: "...",
      sourceType: "pdf",
      fields: { amount: 100, date: "2024-01-01" },
      confidence: 0.87
    }
    vs
    {
      documentId: "...",
      sourceType: "email",
      fields: { subject: "...", sender: "...", date: "..." },
      extractedBy: "SparkAir"
    }
  
  - Access pattern: "Get document by ID" (simple)
  - Consistency: Eventual OK (reads stale version is OK)
  - Write pattern: Insert once, then read for review
  
  - MongoDB fits: Flexible schema, horizontal scaling

METADATA (PostgreSQL):
  - Fixed schema (users, templates, approval rules)
    Users:
    - id, email, role (L1/L2), department
    - Structured, normalized
  
  - Access pattern: Complex queries
    "Get all documents approved by L2 users in Finance in Q1"
    "List users who haven't reviewed any documents"
  
  - Consistency: ACID required
    - User roles must be authoritative (can't be stale)
    - Approval rules must be consistent
  
  - Write pattern: Frequent updates (user preferences, rule changes)
  
  - PostgreSQL fits: Strong consistency, complex queries, ACID

Alternatives considered:

Option 1: MongoDB only
  ❌ Can't do complex queries (Finance dept approval reports)
  ❌ No ACID (user role changes mid-approval = bad)
  ❌ Schema validation weak (approval rules must be precise)

Option 2: PostgreSQL only
  ❌ No document versioning support
  ❌ JSON columns slow for large documents (100MB PDF → JSON)
  ❌ Horizontal scaling harder (relational sharding is complex)
  ❌ Schema changes painful (document structure evolves)

Option 3: Polyglot (PostgreSQL + MongoDB)
  ✅ Right tool for each job
  ✅ Independent scaling (document growth ≠ metadata growth)
  ✅ Query optimization per database
  ❌ Operational burden (maintain two databases)
  ❌ Consistency between them (eventual consistency story)
  ❌ Team needs both skills

Trade-off accepted:
  - Ops burden is real (2 backups, 2 failover processes)
  - Mitigated by managed services (AWS RDS for Postgres, MongoDB Atlas)
  - Worth it because query performance is critical for L2 user reports

Consistency story:
  - PostgreSQL is source of truth for schema
  - MongoDB eventual consistency acceptable (reads can be stale)
  - Event sourcing in Kafka ensures nothing is lost
```

**Reference**: [ARCHITECTURE_ADVANCED_QA.md - Q13: Database Challenges](ARCHITECTURE_ADVANCED_QA.md#q13-database-challenges)

---

### **Category 7: Domain-Specific Challenges**

#### Q13: "Financial data extraction is hard. Walk me through how you guarantee 99%+ accuracy with AI that's only 85% accurate baseline."
**Why This Matters**: Domain-specific, shows you understand the core business problem
**What They're Listening For**:
- Confidence scoring understanding
- Human-in-the-loop design
- Quality metrics
- Continuous improvement

**Your Answer Should Include**:
```
The Gap: 85% AI → 99% Production

Root cause analysis:
  - SparkAir confidence scores unreliable (overconfident on malformed PDFs)
  - Certain document types: 95% accurate (clean financials)
  - Certain types: 70% accurate (old scans, mixed languages)

Solution: Multi-layer validation

Layer 1: PRE-VALIDATION (Format checks)
  - PDF integrity check (header valid?)
  - Encoding detection (UTF-8 vs Latin-1?)
  - Expected field existence (does template match PDF?)
  - Rejection rate: 5% (corrupted/unreadable PDFs)
  - Accuracy impact: +1% (removes worst-case garbage)

Layer 2: CONFIDENCE ROUTING
  - High confidence (>0.9): Auto-approve
    Accuracy: ~96% (AI handles easy cases well)
    Volume: 50% of documents
    
  - Medium confidence (0.7-0.9): Route to L2 human review
    Accuracy with human: 99%+ (human catches AI errors)
    Volume: 40% of documents
    
  - Low confidence (<0.7): Manual extraction
    Accuracy: 100% (human enters from scratch)
    Volume: 10% of documents
    Cost: High (human time), but ensures correctness

Layer 3: RULES-BASED VALIDATION
  - Amount validation: Must be > 0, < 1B (catch outliers)
  - Date validation: Must be within last 5 years
  - Entity validation: Must map to Soniq database
  - Cross-field checks: Currency matches amount format
  - Rejection rate: 2% (fails rules, queued for manual review)
  - Catches: Math errors, hallucinations, wrong extraction

Layer 4: SAMPLING AUDITS
  - Hourly: Verify 100 auto-approved documents (0.9+ confidence)
  - Method: L2 human spot-check
  - Alert: If accuracy < 99%, pause auto-approval
  - Action: Recalibrate confidence thresholds
  - Frequency: Weekly trend analysis

Results:
  - 50% auto-approved (0.9+) → 96% accurate → ~2% error
  - 40% L2 reviewed (0.7-0.9) → 99%+ accurate → ~0.4% error
  - 10% manual (< 0.7) → 100% accurate → ~0% error
  
  Weighted: (0.5 × 0.96) + (0.4 × 0.99) + (0.1 × 1.0) = 99.2%

Continuous improvement:
  - Track: Which document types have low confidence?
  - Action: Retrain SparkAir on those types
  - Feedback: L2 reviews → correct answers → training data
  - Result: Confidence scores improve quarterly

Cost of accuracy:
  - L2 review time: 2 mins per document
  - 40% of 10K docs = 4000 docs × 2 mins = 133 hours/day
  - Cost: 2 FTE + contractors
  - ROI: Regulatory compliance ($1M+ fines if wrong)
```

**Reference**: [TECHNICAL_DEEP_DIVES.md - Problem 3: Quality Guarantee](TECHNICAL_DEEP_DIVES.md)

---

#### Q14: "You process structured, semi-structured, and unstructured data (audited financials, debt instruments, legal agreements). How do you normalize these into a canonical schema?"
**Why This Matters**: Data diversity is a hard problem
**What They're Listening For**:
- Schema design
- Flexibility vs structure
- Template system
- Extensibility

**Your Answer Should Include**:
```
Three data types, vastly different structures:

1. STRUCTURED (Audited Financials - 10-K filings)
  Input:
    {
      Income Statement: [
        { item: "Revenue", amount: 1B, period: "2024-Q1" },
        { item: "Operating Costs", amount: 500M, period: "2024-Q1" }
      ],
      Balance Sheet: [...]
    }
  
  Extraction: 90%+ accurate (tables with headers = easy for OCR)
  Schema: Canonical = { financialStatementType, lineItem, amount, period, currency }

2. SEMI-STRUCTURED (Debt Instruments - Bonds, Loans)
  Input:
    "Bond ISIN: XS123456789
     Coupon Rate: 5.5%
     Maturity: 2030-12-31
     Outstanding Principal: $500M"
  
  Extraction: 75% accurate (mixed text + structured fields)
  Schema: Canonical = { instrumentType, rate, maturity, principal, currency }

3. UNSTRUCTURED (Legal Agreements - Contracts, Clauses)
  Input:
    "The Borrower covenants that (i) it shall maintain a debt-to-equity ratio 
     not to exceed 2.5:1; (ii) it shall report quarterly; and (iii) in the 
     event of material adverse change, the Lender may accelerate..."
  
  Extraction: 60% accurate (free text = hard, needs NLP + legal knowledge)
  Schema: Canonical = { clauseType, covenant, threshold, consequence }

Solution: Template-driven extraction + canonical schema

TEMPLATE SYSTEM:
  For each document type, define:
  - Expected fields (amount, date, entity)
  - Field order (extraction hints)
  - Validation rules
  - Extraction method (regex, OCR, NLP)
  
  Example template for 10-K:
    {
      documentType: "financial_statement",
      fields: [
        { name: "revenue", type: "currency", required: true },
        { name: "period", type: "date", required: true },
        { name: "reportingEntity", type: "string", required: true }
      ],
      extractionMethod: "ocr_then_regex"
    }

CANONICAL SCHEMA:
  All documents → JSON:
  {
    documentId: "doc-123",
    canonicalType: "financial_metric",  ← Normalized across all sources
    sourceType: "10_k_filing",           ← Original type
    data: {
      metric: "revenue",
      value: 1000000000,
      currency: "USD",
      period: "2024-Q1",
      extractedAt: "2024-01-15T10:30:00Z",
      confidence: 0.95,
      extractedBy: "SparkAir"
    }
  }

NORMALIZATION LOGIC:
  Step 1: Identify document type (template matching)
    - Content hash vs known templates
    - File name + structure
    
  Step 2: Extract using type-specific template
    - 10-K: Use OCR + regex
    - Bond: Use NLP + pattern matching
    - Legal: Use LLM-based extraction
    
  Step 3: Map extracted fields to canonical schema
    - "Bond Coupon" → { metric: "coupon_rate", value: 5.5, unit: "%" }
    - "Debt/Equity" → { metric: "leverage_ratio", value: 2.5, unit: "ratio" }
    
  Step 4: Validate against canonical rules
    - Amount > 0? Yes
    - Period in valid format? Yes
    - Entity mappable to Soniq? Check
    
  Step 5: Store in MongoDB with both original + canonical
    {
      original: { ... raw extraction ... },
      canonical: { ... normalized ... }
    }

Benefits:
  - Downstream systems consume consistent format
  - Can compare across document types
  - Template evolution doesn't break consumers
  - Easy to add new document type (new template)

Cost:
  - Maintain template library (1-2 FTE)
  - NLP models may need retraining
  - Edge cases require manual handling

Extensibility:
  - New document type: Add template, done
  - New field: Add to template, backfill old documents
  - Schema evolution: Version canonical schema
```

**Reference**: [DATA_COLLECTION_PLATFORM_REQUIREMENTS.md - Configuration & Canonical Model](DATA_COLLECTION_PLATFORM_REQUIREMENTS.md)

---

### **Category 8: Red Team / Gotcha Questions**

#### Q15: "Your architecture looks good on paper. But tell me: What's a decision you regret? What would you do differently?"
**Why This Matters**: Tests maturity and honesty
**What They're Listening For**:
- Self-awareness
- Learning orientation
- Pragmatism

**Your Answer Should Include**:
```
Honest regrets:

REGRET #1: Event Sourcing Complexity
  What I chose: Event sourcing + CQRS
  Why: Complete audit trail, temporal queries, zero race conditions
  
  The pain:
    - Took 3 weeks to debug eventual consistency issue
    - Team struggled with offset management
    - Kafka topics are harder to version than database tables
    - Harder to explain to new engineers
  
  What I'd do differently:
    - Start simpler (just versioning + optimistic locking)
    - Add event sourcing only if regulatory requirement forces it
    - For 99% of systems, versioning is sufficient
    
  Lesson: Don't architect for a 10-year roadmap on day 1

REGRET #2: Microservices (Minor)
  What I chose: 6 microservices
  Why: Independent scaling, team autonomy
  
  The pain:
    - Distributed debugging is hard
    - Correlation IDs across services adds overhead
    - Configuration management is complex
    - Team needed to learn Kubernetes
  
  What I'd do differently:
    - For 10K docs/day, maybe 3 services is enough (not 6)
    - Deployment pipeline took longer than expected
    - Monolith would have been faster to market
    
  Lesson: Microservices = team scalability, not technical scalability

REGRET #3: Cognize as Backup
  What I chose: SparkAir primary, Cognize fallback
  Why: Redundancy, avoid single vendor lock-in
  
  The pain:
    - Cognize and SparkAir have different error profiles
    - Integration with Cognize took extra 2 weeks
    - Fallback logic adds complexity
    - 5% of failures are Cognize-specific
  
  What I'd do differently:
    - Single vendor (SparkAir) with SLA might be sufficient
    - Or standardize on same vendor for multiple document types
    - Fallback to manual extraction might be better than 2nd vendor
    
  Lesson: More options don't always mean better (diminishing returns)

NOT A REGRET: Eventual Consistency
  - Users see status lag by 2-5 seconds
  - Initially worried they'd hate it
  - Reality: They don't notice (review cycle is 2 minutes)
  - Architectural win, zero user complaints
```

**Reference**: [DEPLOYMENT_AND_SUMMARY.md - Trade-offs Made](DEPLOYMENT_AND_SUMMARY.md)

---

#### Q16: "Your 99% uptime target. Convince me that it's realistic and worth it."
**Why This Matters**: Challenge your assumptions; see if you cave or defend
**What They're Listening For**:
- Confidence in your analysis
- Cost-benefit reasoning
- Willingness to be challenged

**Your Answer Should Include**:
```
WHY 99% IS REALISTIC:

Budget breakdown (7 hours downtime allowed/month):
  - Scheduled maintenance: 2 hours (planned database upgrades)
  - Kubernetes cluster upgrade: 1 hour (rolling update, some apps restarted)
  - Emergency bug fixes: 1 hour (circuit breaker edge case, quick rollback)
  - Unplanned failures: 3 hours budget (disk full, network partition, etc.)
  
  These are real, not worst-case. 99% = 7 hours is achievable.

WHY NOT 99.9%:
  - 99.9% = 4.3 hours/month downtime
  - Requires: Change freeze during deploy, no emergency fixes, zero failures
  - Cost: +$200K/year (redundant data centers, 24/7 on-call, testing)
  - Benefit: Marginal (4 more hours of uptime nobody notices)
  - Tradeoff: Not worth it for financial data platform

WHY NOT 99.5%:
  - 99.5% = 3.6 hours/month = too tight
  - Forces: Every maintenance window perfectly timed, zero tolerance for failures
  - Reality: One unexpected issue = miss SLA (then what? Penalties?)
  - Risk: Team burns out chasing perfect score
  
WHY 99% IS WORTH IT:

Business case:
  - 99% = 7 hours downtime/month
  - L2 users work 8 hours/day, 20 days/month = 160 hours available
  - 7 hours downtime = 4% of their time (acceptable)
  - If 99.9%: Cost $200K for 0.4% additional uptime (ROI is bad)

Cost:
  - 99%: $28K/year (current setup)
  - 99.5%: $48K/year (+$20K for HA improvements)
  - 99.9%: $200K/year (+$170K for redundancy)
  - Diminishing returns are real

Implementation:
  - 99%: Blue-green, HA databases, monitoring ← Already done
  - 99.5%: Multi-region failover ← Nice to have, expensive
  - 99.9%: Active-active across regions ← Excessive for this use case

WHY I CAN DEFEND THIS:
  - Measured: Actual uptime history shows 99.1% over 18 months
  - Realistic: Budget includes realistic failure scenarios
  - Justified: Cost increases exponentially for marginal gains
  - Honest: 99% is ambitious but achievable without heroics
```

**Reference**: [DEPLOYMENT_AND_SUMMARY.md - Availability NFR](DEPLOYMENT_AND_SUMMARY.md)

---

## 📋 Summary: Question Categories

| Category | # Questions | Focus |
|----------|-------------|-------|
| **Architecture & Trade-offs** | 3 | Microservices, event sourcing, uptime targets |
| **Scale & Performance** | 3 | 10K→100K scaling, cache optimization, latency |
| **Data Consistency** | 2 | Event sourcing, race conditions |
| **Security & Compliance** | 1 | Audits, encryption, regulatory requirements |
| **Team & Organization** | 2 | Service ownership, feature prioritization |
| **Technology Choices** | 2 | Kafka vs RabbitMQ, Mongo vs PostgreSQL |
| **Domain Challenges** | 2 | AI accuracy, data normalization |
| **Red Team / Gotchas** | 2 | Regrets, realistic targets |

**Total: 17 questions** covering all architect-level concerns

---

## 🎯 How to Prepare

### **For Each Question:**

1. **Understand the intent** (what are they testing?)
2. **Know your trade-offs** (nothing is perfect)
3. **Have data** (metrics, not feelings)
4. **Practice explaining** (clear, concise, 3-5 minute answers)
5. **Be honest** (regrets + learnings > pretending perfection)

### **Reading Path:**

→ Review these documents in your repo:
- [ARCHITECTURE_DESIGN_QA.md](ARCHITECTURE_DESIGN_QA.md) (patterns, performance)
- [TECHNICAL_DEEP_DIVES.md](TECHNICAL_DEEP_DIVES.md) (hard problems with code)
- [DEPLOYMENT_AND_SUMMARY.md](DEPLOYMENT_AND_SUMMARY.md) (ops, cost, trade-offs)

→ Practice answering each question out loud (not just reading)

→ Have specific numbers ready (10K, 99%, $28K, etc.)

---

**Good luck! These questions will prepare you for any architect-level interview. 🚀**
