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

Normally, a system stores only the current state:

```text
Order status = DELIVERED
```

This tells us where the order is now, but it does not show how the order reached that state.

With event sourcing, the system stores every change as an event:

```text
1. OrderCreated
2. PaymentCompleted
3. OrderShipped
4. OrderDelivered
```

The event history becomes the source of truth. The current state is calculated by replaying the events in order:

```text
Start with an empty order
→ Apply OrderCreated
→ Apply PaymentCompleted
→ Apply OrderShipped
→ Apply OrderDelivered

Current status = DELIVERED
```

### Simple analogy: A bank account

A bank does not only store the current balance:

```text
Balance = ₹10,000
```

It also stores every transaction:

```text
+ ₹15,000 salary
- ₹3,000 rent
- ₹2,000 shopping
```

The bank can calculate the current balance from this transaction history. Event sourcing works in a similar way: store everything that happened, then derive the current state from that history.

### Why use event sourcing?

- It provides a complete history of what happened.
- We can see who changed something and when.
- We can reconstruct the state at an earlier point in time.
- We can rebuild the current state if a read database is lost or corrupted.
- We can replay events after fixing a bug in processing logic.
- It is useful for auditing and regulatory requirements.

### What are the downsides?

- It is more complex than storing only the latest state.
- Old event formats must continue to work as the application evolves.
- Replaying a large number of events can be slow.
- Read models may update later, causing eventual consistency.
- Event replay must not accidentally repeat external actions such as charging a payment or sending an email.

### Does event sourcing solve distributed transactions?

No.

- **Event sourcing** records what happened.
- **Saga** coordinates work across services and handles failures.

They solve different problems, although they can be used together.

Also, Kafka does not have to be the authoritative event store. A durable append-only database can be the event store while Kafka distributes events to consumers.

### Interview-ready answer

> Event sourcing stores every state change as an immutable event instead of storing only the latest state. The current state is reconstructed by replaying those events. It provides a complete audit history and replay capability, but it adds complexity and does not by itself solve distributed transactions.

---

## 9. What is idempotency?

An operation is idempotent when repeating it produces the same final result as executing it once.

For example, the same money-transfer message may be delivered twice:

```text
Transfer ₹100 from Account A to Account B
Transfer ID = TX-123
```

Without idempotency, Account A could be debited twice or Account B could be credited twice. With idempotency, each service processes `TX-123` only once.

### Simple debit and credit architecture

```text
transfer-request topic
        ↓
JVM 1: Debit Service
        ↓
debit-completed topic
        ↓
JVM 2: Credit Service
        ↓
transfer-completed topic
```

JVM 1 and JVM 2 run independently. Each service has its own Kafka consumer and producer.

### Kafka delivery modes

#### At-most-once

The consumer commits the offset before processing:

```text
Commit offset → Process message
```

If the consumer crashes after committing the offset but before processing, the message is lost.

```text
Message loss: Possible
Duplicate processing: No
```

#### At-least-once

The consumer processes the message before committing the offset:

```text
Process message → Commit offset
```

If the consumer crashes after processing but before committing the offset, Kafka delivers the message again.

```text
Message loss: Normally avoided
Duplicate processing: Possible
```

This is the most common mode, so consumers should be idempotent.

#### Exactly-once

Exactly-once does not mean the processing code executes only once. The code may run again after a crash.

It means that, for Kafka-to-Kafka processing, only one successful result becomes visible.

### Exactly-once in JVM 1: Debit Service

JVM 1 receives `TransferRequested(TX-123)` and performs one Kafka transaction:

```text
BEGIN KAFKA TRANSACTION

1. Read TransferRequested(TX-123)
2. Produce DebitCompleted(TX-123)
3. Commit the transfer-request offset

COMMIT KAFKA TRANSACTION
```

Kafka commits the output event and consumed offset together.

If JVM 1 crashes before committing:

```text
DebitCompleted visible? No
Input offset committed? No
Input message retried?  Yes
```

If the transaction succeeds:

```text
DebitCompleted visible? Yes
Input offset committed? Yes
```

### Exactly-once in JVM 2: Credit Service

JVM 2 starts a separate Kafka transaction:

```text
BEGIN KAFKA TRANSACTION

1. Read DebitCompleted(TX-123)
2. Produce TransferCompleted(TX-123)
3. Commit the debit-completed offset

COMMIT KAFKA TRANSACTION
```

If JVM 2 crashes before committing, neither its output event nor its consumed offset is committed. Kafka delivers `DebitCompleted(TX-123)` again.

Consumers reading transactional output should use `isolation.level=read_committed` so they do not see aborted records.

### Important limitation: Kafka cannot protect database updates

Suppose JVM 1 stores Account A in PostgreSQL:

```text
1. Debit ₹100 from Account A in PostgreSQL ✅
2. JVM 1 crashes before committing the Kafka offset ❌
3. Kafka delivers TX-123 again
4. Account A could be debited again ❌
```

Kafka cannot atomically roll back or commit a normal database transaction together with its own transaction.

Therefore, the Debit Service must use `TX-123` as an idempotency key:

```sql
BEGIN;

INSERT INTO processed_transfers (transfer_id)
VALUES ('TX-123'); -- UNIQUE constraint

UPDATE accounts
SET balance = balance - 100
WHERE account_id = 'A';

COMMIT;
```

The idempotency record and debit must be saved in the same database transaction.

When Kafka redelivers `TX-123`, the unique constraint shows that the transfer was already processed. JVM 1 skips the second debit.

JVM 2 follows the same approach when crediting Account B:

```text
TX-123 not processed → Credit B and record TX-123
TX-123 already exists → Skip duplicate credit
```

Avoid implementing these as unrelated steps:

```text
Check transfer ID → Update balance → Save transfer ID
```

Two consumer instances could check at the same time and both update the balance.

### Complete practical solution

```text
JVM 1: Debit Service
  1. Receive TX-123
  2. Record TX-123 and debit A in one database transaction
  3. Publish DebitCompleted(TX-123)

JVM 2: Credit Service
  1. Receive DebitCompleted(TX-123)
  2. Record TX-123 and credit B in one database transaction
  3. Publish TransferCompleted(TX-123)
```

Use:

- **Kafka transactions** to atomically commit Kafka output records and consumed offsets.
- **Idempotency keys** to prevent duplicate database debit and credit operations.
- **Saga compensation** to refund Account A if crediting Account B permanently fails.

### Interview-ready answer

> JVM 1 and JVM 2 perform separate Kafka transactions. Kafka exactly-once atomically commits each JVM's output records and consumed offsets, so only one successful Kafka result becomes visible. It does not create one transaction across both JVMs or their databases. Database debit and credit operations still require a stable transfer ID, such as `TX-123`, stored with the balance update in one local transaction. If credit permanently fails, a Saga compensates by refunding the debit.

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

Complex human approval workflow
→ Orchestration + compensating transactions

Audit-sensitive state
→ Event sourcing
```

### Why use choreography for a fast processing pipeline?

Consider a simple automatic pipeline:

```text
Document uploaded
        ↓
Extract text
        ↓
Check quality
        ↓
Create search index
```

Each service can react to the event produced by the previous service:

```text
DocumentUploaded → TextExtracted → QualityChecked → DocumentIndexed
```

Choreography is a good fit because:

- The flow is simple and predictable.
- Services can process many documents in parallel.
- Each service can scale independently.
- A new consumer, such as Analytics Service, can listen without changing the existing services.

The consumers must be idempotent because Kafka may deliver the same event again after a crash or timeout.

```text
First delivery  → Process and record event ID
Second delivery → Event ID already exists, so skip
```

Therefore, choreography gives loose coupling and scalability, while idempotency makes retries safe.

### Does every human workflow require orchestration?

No. A simple human decision can use choreography:

```text
Document submitted
        ↓
Human clicks Approve or Reject
        ↓
DocumentApproved or DocumentRejected event
        ↓
Other services react
```

The presence of a human does not automatically require an orchestrator.

### When does orchestration become useful?

Consider a more complex approval process:

```text
1. Assign Reviewer 1.
2. Wait up to two days.
3. Send a reminder after one day.
4. Escalate to a manager after two days.
5. If approved, assign Reviewer 2.
6. Reviewer 2 may approve, reject or request rework.
7. Rework returns to Reviewer 1.
8. The requester may cancel the process.
9. Operations must see where the document is waiting.
```

This creates several paths:

```text
Reviewer 1
  ├─ No response → Reminder → Escalation
  ├─ Reject      → Finish
  └─ Approve     → Reviewer 2
                      ├─ Approve → Publish
                      ├─ Reject  → Finish
                      └─ Rework  → Reviewer 1
```

Choreography can implement this, but the workflow state becomes scattered across several services:

```text
Review Service     → approvals and rework
Timer Service      → reminders and deadlines
Escalation Service → manager escalation
Document Service   → cancellation
```

For example, when a deadline event arrives, a service must answer:

```text
Was the document already approved?
Was it cancelled?
Is it currently in rework?
Is this an old timer from an earlier review attempt?
Is Reviewer 1 or Reviewer 2 currently responsible?
```

With orchestration, one workflow engine remembers the complete current state:

```text
Workflow ID: DOC-123
Current step: WAITING_FOR_REVIEWER_2
Review attempt: 2
Deadline: 20 June, 5:00 PM
Status: ACTIVE
```

If an old Reviewer 1 timer arrives while the workflow is waiting for Reviewer 2, the orchestrator can see that the timer is stale and ignore it.

An orchestrator is helpful for:

- **Long-running workflows:** The process remains active for hours, days or weeks.
- **Multiple stages:** Steps must happen in a controlled order.
- **Timers:** Reminders or deadline actions must run at the correct time.
- **Escalation:** Work moves to a manager when someone does not respond.
- **Branching:** Approval, rejection and rework have different paths.
- **Rework:** The workflow returns to and repeats an earlier step.
- **Central visibility:** Operations can see where a workflow is currently waiting.

Compensating transactions handle steps that have already completed when a later step permanently fails. For example, if publishing fails after approval, the workflow may revoke the approval, return the document for review, or mark it as `APPROVED_NOT_PUBLISHED`.

### Simple decision rule

```text
One human decision followed by simple reactions
→ Choreography can work well

A managed process that must remember where it is,
handle timers and decide the next valid action
→ Orchestration is usually easier
```

The reason for choosing orchestration is not simply that a human is involved. The reason is the amount of workflow state and decision logic that must be managed.

---

## Interview-ready answer

> In microservices, I would avoid a distributed ACID transaction across service databases. I would model the business transaction as a Saga made of local transactions. For a simple event pipeline, I would use choreography. For a complex workflow involving timeouts, branching or human approval, I would use orchestration. Each completed step would have a compensating action, and every message consumer would be idempotent because duplicate delivery is possible. I would use event sourcing only where complete history, auditing or replay provides enough business value to justify its complexity.
