# Microservices Architect Interview Questions

## 1. What is the distributed transaction problem in microservices?

In a monolith, one database transaction can update multiple tables:

```text
BEGIN
  Approve document
  Publish document
  Record notification
COMMIT
```

If any operation fails, the database rolls back everything.

In microservices, each service normally owns its own database:

```text
Approval Service      → PostgreSQL
Dissemination Service → S3
Notification Service  → MongoDB
```

One ACID transaction cannot easily cover all these systems.

For example:

```text
Approve document ✅
Publish document ❌
Send notification ❌
```

The document is approved but not published. The system is temporarily inconsistent and needs a recovery strategy.

---

## 2. What is the Saga pattern?

A **Saga** treats one business transaction as a sequence of smaller local transactions.

```text
Approve document
       ↓
Publish document
       ↓
Send notification
```

Each service commits its own local transaction. If a later step fails, the Saga runs compensating transactions for the earlier completed steps.

```text
Approve document ✅
Publish document ❌
       ↓
Revoke approval or return document to PENDING_REVIEW
```

A compensating transaction is a new business operation. It is not a technical database rollback because the original transaction has already committed.

### Can Saga be used alone?

Saga is the overall distributed transaction pattern. We must also decide how its steps will be coordinated:

- **Choreography:** Services coordinate by reacting to events.
- **Orchestration:** A central orchestrator decides the next step.
- **Hybrid:** The main workflow is orchestrated while some downstream processing uses choreography.

Therefore:

> Saga defines the transaction and compensation strategy. Choreography and orchestration define how the Saga steps are coordinated.

---

## 3. What is Saga choreography?

In choreography, there is no central workflow controller. Each service reacts to events and publishes another event after completing its work.

```text
Approval Service
  └─ publishes DocumentApproved
                ↓
Dissemination Service
  └─ publishes DocumentPublished
                ↓
Notification Service
  └─ publishes NotificationSent
```

The services collectively create the workflow.

### Advantages

- Loose coupling between services
- Good scalability
- Easy to add another event consumer
- Suitable for simple, high-volume pipelines

### Disadvantages

- The complete workflow is not visible in one place
- Debugging requires tracing events across services
- Long event chains become difficult to understand
- Cyclic dependencies can develop
- Failure and timeout handling can become scattered

### Good use case

```text
Source document → Extract data → Check quality → Index result
```

---

## 4. What is Saga orchestration?

In orchestration, a central component owns the workflow and tells each service what to do.

```text
Saga Orchestrator
  ├─ Tell Approval Service to approve
  ├─ Tell Dissemination Service to publish
  ├─ Tell Notification Service to notify
  └─ Start compensation if a step fails
```

The orchestrator may be implemented using Camunda, Temporal, a state machine, or a custom workflow service.

### Advantages

- Complete workflow is visible in one place
- Easier monitoring and debugging
- Central handling of retries, timeouts and compensation
- Suitable for complex and human-driven workflows

### Disadvantages

- The orchestrator is coupled to the workflow participants
- It must be highly available
- It can become a bottleneck if designed poorly
- Too much business logic can turn it into a "god service"

### Good use case

```text
L1 review → L2 review → Approval → Publication → Notification
```

---

## 5. Are events used only in choreography?

No. Events can be used in both choreography and orchestration.

The real difference is **who decides the next step**.

### Choreography

Events drive the workflow:

```text
DocumentApproved event
       ↓
Dissemination Service reacts
       ↓
DocumentPublished event
       ↓
Notification Service reacts
```

No central component controls the complete sequence.

### Orchestration using synchronous APIs

```text
Orchestrator → POST /approve
Orchestrator → POST /publish
Orchestrator → POST /notify
```

### Orchestration using asynchronous messages

```text
Orchestrator → PublishDocument command
Dissemination Service → DocumentPublished event
Orchestrator receives event → SendNotification command
```

This is still orchestration because the orchestrator receives the result and decides what should happen next.

| Question | Choreography | Orchestration |
|---|---|---|
| Who decides the next step? | Participating services | Central orchestrator |
| Are events used? | Yes, events drive the workflow | Optional; APIs, commands and events can be used |
| Is there a central workflow owner? | No | Yes |
| Best suited for | Simple event pipelines | Complex workflows and human tasks |

---

## 6. What is the difference between a command and an event?

A **command** asks a specific receiver to perform an action.

```text
PublishDocument
ReserveInventory
ChargePayment
```

Commands are usually written in the imperative form. A command can succeed or fail.

An **event** announces a fact that has already happened.

```text
DocumentPublished
InventoryReserved
PaymentCharged
```

Events are usually written in the past tense. An event should not be rejected because it describes something that already occurred.

```text
Command: PublishDocument
Result:  DocumentPublished or DocumentPublicationFailed
```

---

## 7. What is a compensating transaction?

A compensating transaction reverses or neutralizes the business effect of an earlier completed transaction.

Example:

```text
Reserve payment   ✅
Reserve inventory ✅
Arrange shipping  ❌
```

Compensation:

```text
Release inventory
Release or refund payment
```

Common examples:

| Forward action | Compensation |
|---|---|
| Reserve inventory | Release inventory |
| Charge payment | Refund payment |
| Approve document | Revoke approval |
| Create booking | Cancel booking |

Compensation is not always a perfect undo. An email cannot be unsent, so the compensation may be to send a correction email.

Irreversible operations should generally be placed near the end of the Saga.

---

## 8. What is event sourcing?

Event sourcing stores every state change as an immutable event instead of storing only the latest state.

State-only storage:

```text
Document 123 → APPROVED
```

Event-sourced storage:

```text
1. DocumentSourced
2. DocumentExtracted
3. QualityCheckPassed
4. DocumentSubmittedForReview
5. DocumentApproved
```

The current state is rebuilt by replaying the events in order.

### Benefits

- Complete audit history
- Ability to reconstruct an earlier state
- Ability to rebuild read models
- Useful for regulatory and compliance requirements
- Events can be replayed after fixing processing logic

### Challenges

- More implementation complexity
- Event schema evolution
- Eventual consistency
- Replaying a large event history can be expensive
- External side effects must not be repeated accidentally during replay

### Does event sourcing solve distributed transactions?

Not by itself.

Event sourcing records what happened. Saga coordinates the multi-service workflow and handles failures. They solve different problems but can be used together.

Also, Kafka does not have to be the authoritative event store. A durable append-only database can be the event store while Kafka distributes events to consumers.

---

## 9. What is idempotency?

An operation is idempotent when repeating it produces the same final result as executing it once.

Messaging systems commonly provide at-least-once delivery, so the same message may arrive more than once:

```text
DocumentApproved received
       ↓
Document published
       ↓
Acknowledgement is lost
       ↓
DocumentApproved delivered again
```

Without idempotency, the document could be published twice.

The message should contain a stable idempotency key:

```text
document-123-approved-v7
```

The consumer checks whether it has already processed that operation:

```text
Key not found → Process and record the key
Key found     → Skip duplicate processing
```

### Important implementation detail

This flow is unsafe if it is implemented as three unrelated operations:

```text
Check key → Perform work → Save key
```

Two consumers could check at the same time and both perform the work.

Safer approaches include:

- A database unique constraint
- An atomic insert-if-absent operation
- An inbox table in the same local transaction as the business update
- A naturally idempotent target operation

---

## 10. How do these patterns work together?

They solve different parts of the distributed transaction problem:

```text
Saga
└─ Defines the complete business transaction

Orchestration or choreography
└─ Coordinates the Saga steps

Compensating transactions
└─ Recover from partially completed work

Idempotency
└─ Makes retries and duplicate messages safe

Event sourcing
└─ Stores history and allows state reconstruction
```

A practical architecture may use:

```text
Fast processing pipeline
→ Choreography + idempotent consumers

Human approval workflow
→ Orchestration + compensating transactions

Audit-sensitive state
→ Event sourcing
```

---

## Interview-ready answer

> In microservices, I would avoid a distributed ACID transaction across service databases. I would model the business transaction as a Saga made of local transactions. For a simple event pipeline, I would use choreography. For a complex workflow involving timeouts, branching or human approval, I would use orchestration. Each completed step would have a compensating action, and every message consumer would be idempotent because duplicate delivery is possible. I would use event sourcing only where complete history, auditing or replay provides enough business value to justify its complexity.
