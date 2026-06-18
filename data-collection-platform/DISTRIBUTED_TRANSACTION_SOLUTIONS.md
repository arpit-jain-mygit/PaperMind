# Distributed Transaction Solutions in Microservices

## The Problem: Distributed Transactions

### Traditional ACID Problem

In a monolith, a transaction is simple:
```sql
BEGIN;
  UPDATE documents SET status = 'APPROVED';
  UPDATE approvals SET approved_by = 'john';
  UPDATE notifications SET status = 'sent';
COMMIT;  -- All succeed or all fail
```

**In microservices, each service owns its database:**
```
Sourcing Service (PostgreSQL)
Extraction Service (MongoDB)
Approval Service (PostgreSQL)
Dissemination Service (S3)
```

**Problem:** If Approval Service updates successfully but Dissemination fails:
- Document marked as APPROVED ✅
- Dissemination never happens ❌
- **Inconsistent state** 🔴

---

## ✅ Solution 1: Event Sourcing + Saga Pattern

### How It Works

**Step 1: Event Sourcing (Source of Truth)**
```
Instead of: UPDATE documents SET status = 'APPROVED'
Do this: INSERT INTO events (type: 'DOCUMENT_APPROVED', ...)
```

Every state change becomes an **immutable event** in Kafka.

**Step 2: Saga Pattern (Distributed Transaction)**

A saga is a sequence of local transactions across multiple services. If a step fails, compensating transactions undo the business effect of the earlier completed steps.

Saga is the overall distributed transaction pattern. It does not run "alone": we must choose how its steps are coordinated:

- **Choreography:** Services coordinate themselves by reacting to events.
- **Orchestration:** A central orchestrator decides and triggers the next step.
- **Hybrid:** Orchestration can manage the main workflow while events handle some downstream processing.

Events are not limited to choreography. An orchestrated saga may use synchronous API calls, asynchronous commands and events, or a combination of them. The important difference is **who decides the next step**, not whether events are present.

```
Approval Service approves doc
         ↓ (publishes event)
APPROVED event in Kafka
         ↓ (Dissemination Service listens)
Dissemination Service publishes data
         ↓ (publishes event)
PUBLISHED event in Kafka
         ↓ (Notification Service listens)
Notification Service sends alert
         ↓ (publishes event)
NOTIFIED event in Kafka
         ↓ (Back to Approval Service)
Mark saga complete
```

### Implementation: Orchestrated Saga (Camunda)

```yaml
# Camunda BPMN Process
Process: ApprovalWorkflow

Start Event: DocumentReadyForApproval
    ↓
Service Task: L2UserReview (human task)
    ↓
Exclusive Gateway: Approved?
    ├─ YES → Service Task: PublishToS3
    │          ↓
    │        Service Task: SendNotification
    │          ↓
    │        End Event: Success
    │
    └─ NO → Service Task: QueueForRework
               ↓
            End Event: Rejected
```

**Java Implementation:**

```java
@Service
public class ApprovalSagaService {
  
  // Step 1: Approve document
  @Transactional
  public void approveDocument(String documentId, String approverId) {
    // Update approval service database (local transaction)
    Document doc = documentRepository.findById(documentId);
    doc.setStatus(DocumentStatus.APPROVED);
    doc.setApprovedBy(approverId);
    documentRepository.save(doc);  // Only this service, ACID guaranteed
    
    // Publish event (triggers next service)
    kafkaTemplate.send("approval-events", 
      new ApprovedEvent(documentId, approverId, Instant.now())
    );
  }
  
  // Step 2: Listen for approval event, disseminate
  @KafkaListener(topics = "approval-events")
  public void onDocumentApproved(ApprovedEvent event) {
    try {
      // Dissemination Service: Local transaction
      disseminationService.publishToS3(event.getDocumentId());
      
      // Publish next event
      kafkaTemplate.send("dissemination-events",
        new PublishedEvent(event.getDocumentId(), Instant.now())
      );
    } catch (Exception e) {
      // If this fails, publish compensating event
      kafkaTemplate.send("compensation-events",
        new ApprovalFailedEvent(event.getDocumentId(), e.getMessage())
      );
    }
  }
  
  // Step 3: Compensating transaction (rollback)
  @KafkaListener(topics = "compensation-events")
  public void onApprovalFailed(ApprovalFailedEvent event) {
    // Undo the approval
    Document doc = documentRepository.findById(event.getDocumentId());
    doc.setStatus(DocumentStatus.PENDING_REVIEW);
    doc.setApprovalError(event.getError());
    documentRepository.save(doc);
    
    // Notify user
    notificationService.sendAlert(
      "Approval failed: " + event.getError()
    );
  }
}
```

### Saga Pattern: Choreography vs Orchestration

> **Simple distinction:** Saga defines the complete business transaction. Choreography and orchestration are two ways to coordinate the steps of that saga.

#### Choreography (Event-Driven, What We Use)
```
Approval Service publishes APPROVED event
    ↓
Dissemination Service (listening) publishes PUBLISHED event
    ↓
Notification Service (listening) publishes NOTIFIED event

Pros: Loose coupling, no central orchestrator
Cons: Hard to see overall flow, debugging difficult
```

Here, the event controls the flow. Each service knows which event it should react to, and there is no central component deciding the complete sequence.

#### Orchestration (Camunda, Centralized)
```
Camunda Workflow Service orchestrates all steps:
  1. Call Approval Service → wait for response
  2. Call Dissemination Service → wait for response
  3. Call Notification Service → wait for response
  4. Mark saga complete

Pros: Clear flow, easy to debug, central visibility
Cons: Tighter coupling, Camunda becomes bottleneck
```

An orchestrator can communicate in either of these ways:

```text
Synchronous:
Orchestrator → POST /publish → Dissemination Service

Asynchronous:
Orchestrator → PublishDocument command
Dissemination Service → DocumentPublished event
Orchestrator receives the event → sends SendNotification command
```

- A **command** asks a specific service to do something: `PublishDocument`.
- An **event** announces a fact that already happened: `DocumentPublished`.

Even when events are used, this is still orchestration because the orchestrator receives the result and decides what should happen next.

| Question | Choreography | Orchestration |
|----------|--------------|---------------|
| Who decides the next step? | Participating services | Central orchestrator |
| Are events used? | Yes, they drive the workflow | Optional; commands, APIs and events can all be used |
| Is there a central workflow owner? | No | Yes |
| Best suited for | Simple, high-volume event pipelines | Complex workflows, timeouts and human tasks |

**Our Choice: Hybrid**
- Choreography for fast pipeline (sourcing → extraction → quality)
- Orchestration (Camunda) for approval workflow (human in loop)

---

## ✅ Solution 2: Event Sourcing (Complete History)

### How It Works

**Instead of storing state, store all events:**

```java
// Bad: State only (lose history)
documents table:
  id | status | updated_at
  1  | APPROVED | 2024-06-18T14:30:00

// Good: Complete event log (can replay)
document_events table:
  id | document_id | event_type | timestamp | payload
  1  | 1 | SOURCED | 2024-06-18T10:00:00 | { ... }
  2  | 1 | EXTRACTED | 2024-06-18T10:05:00 | { confidence: 0.87 }
  3  | 1 | QUALITY_CHECKED | 2024-06-18T10:10:00 | { passed: true }
  4  | 1 | PENDING_REVIEW | 2024-06-18T10:15:00 | { assigned_to: reviewer_1 }
  5  | 1 | APPROVED | 2024-06-18T14:30:00 | { approved_by: john }
  6  | 1 | PUBLISHED | 2024-06-18T14:35:00 | { destination: s3 }
```

### Benefits

✅ **Complete Audit Trail** (regulatory requirement)
- Who changed what, when
- Can answer "status on June 15 at 2 PM?"
- Compliance audit: Just query events

✅ **Event Replay** (bug fix recovery)
- If you fix a bug in rules engine, re-run all events
- Document state recalculates from events
- No data loss, zero downtime fix

✅ **Temporal Queries**
```sql
-- What was status on June 15?
SELECT * FROM document_events 
WHERE document_id = 1 
  AND timestamp <= '2024-06-15T23:59:59' 
ORDER BY timestamp DESC LIMIT 1;
```

✅ **Consistency Without Distributed Locks**
```
Events are ordered per documentId (Kafka partition)
  Event 1: status = SOURCED (version 1)
  Event 2: status = EXTRACTED (version 2)
  Event 3: status = APPROVED (version 3)
  
No race condition: Events applied in order
```

### Implementation

```java
// Event class (immutable)
@Data
public class DocumentEvent {
  String id;
  String documentId;
  String eventType;  // SOURCED, EXTRACTED, APPROVED, etc.
  long version;      // Monotonically increasing
  Instant timestamp;
  Map<String, Object> payload;
  String actor;      // Who triggered this event
  String correlationId;  // For tracing across services
}

// Event store (append-only log)
@Service
public class EventStore {
  
  @Transactional
  public void append(DocumentEvent event) {
    // Get next version
    long nextVersion = mongoTemplate.count(
      new Query(Criteria.where("documentId").is(event.getDocumentId())),
      DocumentEvent.class
    ) + 1;
    
    event.setVersion(nextVersion);
    event.setTimestamp(Instant.now());
    
    // Append to immutable log
    mongoTemplate.insert(event, "document_events");
    
    // Publish to Kafka (other services listen)
    kafkaTemplate.send("document-events", event);
    
    // Update derived state (read model)
    updateDocumentState(event);
  }
  
  // Event replay: Reconstruct state from events
  public DocumentState getState(String documentId) {
    List<DocumentEvent> events = mongoTemplate.find(
      new Query(Criteria.where("documentId").is(documentId))
        .with(Sort.by("version")),
      DocumentEvent.class
    );
    
    DocumentState state = new DocumentState(documentId);
    for (DocumentEvent event : events) {
      state.apply(event);  // State machine
    }
    return state;
  }
}

// State machine (applies events to update state)
@Data
public class DocumentState {
  String documentId;
  DocumentStatus status;
  Instant lastUpdated;
  
  public void apply(DocumentEvent event) {
    switch(event.getEventType()) {
      case "SOURCED":
        this.status = DocumentStatus.SOURCED;
        break;
      case "EXTRACTED":
        this.status = DocumentStatus.EXTRACTED;
        break;
      case "APPROVED":
        this.status = DocumentStatus.APPROVED;
        break;
      // ... etc
    }
    this.lastUpdated = event.getTimestamp();
  }
}
```

---

## ✅ Solution 3: Distributed Saga with Compensating Transactions

### The Problem: What if Step 2 Fails?

```
Step 1: Approval Service approves document ✅
Step 2: Dissemination Service fails to publish ❌
Step 3: Notification Service can't run ❌

Document is APPROVED in DB but not published = BAD
```

### Solution: Compensating Transactions (Rollback)

```
Saga Flow:

Step 1: Approve
  ├─ Success → Publish event APPROVED
  └─ Fail → Publish event APPROVAL_FAILED (compensate)

Step 2: Publish to S3
  ├─ Success → Publish event PUBLISHED
  └─ Fail → Publish event PUBLICATION_FAILED
           → Dissemination Service listens
           → Publishes UNDO_APPROVAL event

Step 3: Compensating (Undo)
  ├─ Receive UNDO_APPROVAL event
  ├─ Update document status back to PENDING_REVIEW
  ├─ Notify user: "Approval failed, please retry"
  └─ Publish event APPROVAL_UNDONE
```

### Implementation

```java
@Service
public class SagaOrchestrator {
  
  private static final String SAGA_TIMEOUT = "5m";
  
  // Saga definition using Spring State Machine
  @Configuration
  @EnableStateMachine
  public static class SagaConfig extends StateMachineConfigurerAdapter<String, String> {
    
    @Override
    public void configure(StateMachineStateConfigurer<String, String> states) 
        throws Exception {
      states
        .withStates()
          .initial("APPROVAL_PENDING")
          .states(new HashSet<>(Arrays.asList(
            "APPROVAL_PENDING",
            "APPROVAL_APPROVED",
            "DISSEMINATION_IN_PROGRESS",
            "DISSEMINATION_COMPLETE",
            "NOTIFICATION_SENT",
            "SAGA_COMPLETE",
            "SAGA_FAILED",
            "COMPENSATING",
            "COMPENSATED"
          )));
    }
    
    @Override
    public void configure(StateMachineTransitionConfigurer<String, String> transitions)
        throws Exception {
      transitions
        .withExternal()
          .source("APPROVAL_PENDING")
          .target("APPROVAL_APPROVED")
          .event("APPROVE")
          .action(context -> approveDocument(context))
        .and()
          .withExternal()
            .source("APPROVAL_APPROVED")
            .target("DISSEMINATION_IN_PROGRESS")
            .event("DISSEMINATE")
            .action(context -> disseminateDocument(context))
        .and()
          .withExternal()
            .source("DISSEMINATION_IN_PROGRESS")
            .target("SAGA_FAILED")
            .event("DISSEMINATION_FAILED")
            .action(context -> compensate(context));
    }
  }
  
  // Approve action
  public void approveDocument(StateContext<String, String> context) {
    String documentId = context.getMessageHeader("documentId");
    
    try {
      approvalService.approve(documentId);
      context.getStateMachine().sendEvent("DISSEMINATE");
    } catch (Exception e) {
      context.getStateMachine().sendEvent("APPROVAL_FAILED");
    }
  }
  
  // Compensating transaction (undo on failure)
  public void compensate(StateContext<String, String> context) {
    String documentId = context.getMessageHeader("documentId");
    
    log.warn("Saga failed for document {}. Starting compensation.", documentId);
    
    // Undo: Go back to PENDING_REVIEW
    Document doc = documentRepository.findById(documentId);
    doc.setStatus(DocumentStatus.PENDING_REVIEW);
    doc.setCompensationReason("Dissemination failed");
    documentRepository.save(doc);
    
    // Notify user
    notificationService.sendAlert(
      "Document approval was undone. Reason: Publishing failed. Please retry."
    );
    
    // Mark saga as compensated
    context.getStateMachine().sendEvent("COMPENSATED");
  }
}
```

---

## ✅ Solution 4: Idempotency (Prevent Duplicates)

### Problem: Message Retry = Duplicate Processing

```
Kafka publishes APPROVED event
    ↓
Network timeout (Dissemination Service doesn't respond)
    ↓
Kafka retries → Publishes APPROVED event again
    ↓
Dissemination Service processes TWICE
    ↓
Document published twice (S3 has duplicates)
```

### Solution: Idempotency Key

```java
@Service
public class DisseminationService {
  
  @KafkaListener(topics = "approval-events")
  public void onApproved(ApprovedEvent event) {
    String idempotencyKey = event.getDocumentId() + "-v" + event.getVersion();
    
    // Check: Already processed?
    if (idempotencyStore.exists(idempotencyKey)) {
      log.info("Skipping duplicate dissemination: {}", idempotencyKey);
      return;  // Idempotent: Safe to ignore
    }
    
    try {
      // Process only once
      publishToS3(event.getDocumentId());
      publishToKafka(event.getDocumentId());
      
      // Mark as processed
      idempotencyStore.mark(idempotencyKey, 
        Instant.now().plus(Duration.ofDays(1))  // TTL: 24 hours
      );
      
      // Publish success event
      kafkaTemplate.send("dissemination-events",
        new PublishedEvent(event.getDocumentId(), Instant.now())
      );
      
    } catch (Exception e) {
      // Don't mark as processed → Allow retry
      throw new RetryableException(e);
    }
  }
}
```

**Idempotency Store Implementation:**

```java
@Component
public class IdempotencyStore {
  
  private final RedisTemplate<String, String> redis;
  private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
  
  public boolean exists(String idempotencyKey) {
    String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
    return redis.hasKey(key);
  }
  
  public void mark(String idempotencyKey, Instant expiresAt) {
    String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
    Duration ttl = Duration.between(Instant.now(), expiresAt);
    redis.opsForValue().set(key, "processed", ttl);
  }
}
```

---

## Comparison Table: Solutions

| Solution | Use Case | Pros | Cons |
|----------|----------|------|------|
| **Event Sourcing** | Data consistency, audit trail | Complete history, can replay, no race conditions | Eventual consistency, complexity |
| **Saga + Choreography** | Fast pipelines | Loose coupling, scalable | Hard to debug, no central visibility |
| **Saga + Orchestration** | Complex workflows | Clear flow, visibility, easy debugging | Tighter coupling, central bottleneck |
| **Idempotency** | Prevent duplicates | Simple to implement, no retry issues | Requires storage (Redis), TTL management |
| **Compensating Txn** | Failure recovery | Can undo partial transactions | Complex logic, hard to get right |

---

## ✅ Best Practice: Combine Multiple Solutions

### Our Architecture (Data Collection Platform)

```
FAST PIPELINE (Choreography + Idempotency):
  Sourcing → Extraction → Quality Check → Results
  (event-driven, fire-and-forget)

COMPLEX WORKFLOW (Orchestration + Compensation):
  L1 Entry → L2 Review → Approval Decision
  (human in loop, needs central control)

CONSISTENCY (Event Sourcing):
  All state changes → Immutable events in Kafka
  (complete audit trail, can replay)

ROBUSTNESS (Idempotency):
  Every service checks idempotency key
  (safe to retry, no duplicates)
```

### Code Pattern

```java
@Service
public class DocumentProcessingService {
  
  // Step 1: Publish immutable event
  @Transactional
  public void processDocument(String documentId) {
    String idempotencyKey = documentId + "-" + UUID.randomUUID();
    
    // Store event (source of truth)
    DocumentEvent event = new DocumentEvent(
      eventType: "DOCUMENT_PROCESSED",
      documentId: documentId,
      version: getNextVersion(documentId),
      idempotencyKey: idempotencyKey
    );
    eventStore.append(event);
    
    // Publish to Kafka (other services listen)
    kafkaTemplate.send("document-events", event);
  }
  
  // Step 2: Listen and process (with idempotency)
  @KafkaListener(topics = "document-events")
  public void onDocumentProcessed(DocumentEvent event) {
    // Check idempotency
    if (idempotencyStore.exists(event.getIdempotencyKey())) {
      return;  // Already processed
    }
    
    try {
      // Local transaction (ACID guaranteed)
      doWork(event);
      
      // Mark as processed
      idempotencyStore.mark(event.getIdempotencyKey());
      
      // Publish next event (chain reaction)
      kafkaTemplate.send("next-stage-events", 
        new NextStageEvent(event.getDocumentId())
      );
      
    } catch (Exception e) {
      // Publish compensation event (orchestrator listens)
      kafkaTemplate.send("compensation-events",
        new CompensationEvent(event.getDocumentId(), e)
      );
    }
  }
  
  // Step 3: Compensation (orchestrator handles)
  @KafkaListener(topics = "compensation-events")
  public void compensate(CompensationEvent event) {
    undoWork(event.getDocumentId());
    notifyUser(event);
  }
}
```

---

## Summary: How We Solve Distributed Transaction Problem

| Layer | Solution | Technology |
|-------|----------|-----------|
| **Consistency** | Event Sourcing | Kafka + MongoDB |
| **Orchestration** | Saga Pattern | Camunda BPMN |
| **Async Communication** | Choreography | Kafka topics |
| **Idempotency** | Idempotency Keys | Redis |
| **Failure Recovery** | Compensating Transactions | Custom logic |
| **Visibility** | Distributed Tracing | Jaeger + correlation IDs |

**Result:** No traditional ACID transactions needed. Instead:
- ✅ Event-driven resilience
- ✅ Complete audit trail
- ✅ Can replay/recover from bugs
- ✅ Independent service scaling
- ✅ No cascading failures

---

This is exactly how the Data Collection Platform handles the "distributed transaction complexity" trade-off! 🚀
