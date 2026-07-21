# Microservices Architect Interview Questions

## Table of Contents

1. [What is the distributed transaction problem in microservices?](#1-what-is-the-distributed-transaction-problem-in-microservices)
2. [What is the Saga pattern?](#2-what-is-the-saga-pattern)
3. [What is Saga choreography?](#3-what-is-saga-choreography)
4. [What is Saga orchestration?](#4-what-is-saga-orchestration)
5. [Are events used only in choreography?](#5-are-events-used-only-in-choreography)
6. [What is the difference between a command and an event?](#6-what-is-the-difference-between-a-command-and-an-event)
7. [What is a compensating transaction?](#7-what-is-a-compensating-transaction)
8. [What is event sourcing?](#8-what-is-event-sourcing)
9. [What is idempotency?](#9-what-is-idempotency)
10. [How do these patterns work together?](#10-how-do-these-patterns-work-together)
11. [Microservices design patterns and their DCP applicability](#11-microservices-design-patterns-and-their-dcp-applicability)
12. [Interview-ready answer](#interview-ready-answer-2)

---

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

## 11. Microservices design patterns and their DCP applicability

The Data Collection Platform (DCP) receives financial documents from sources such as S3, email and APIs. It extracts data using external AI providers, validates the result, sends uncertain documents for human review and publishes approved data to downstream systems.

Each pattern below solves a specific business or technical problem in that flow.

### Circuit breaker: How does it help the business user?

Suppose the Extraction Service calls the external SparkAir AI API.

```text
DCP Extraction Service → SparkAir
```

If SparkAir becomes unavailable and there is no circuit breaker, every extraction worker continues calling it:

```text
Document 1 → wait 30 seconds → fail
Document 2 → wait 30 seconds → fail
Document 3 → wait 30 seconds → fail
```

Workers remain occupied waiting for a service that is already known to be unhealthy. The Kafka backlog grows and users see documents stuck in `EXTRACTION_IN_PROGRESS`.

A circuit breaker watches the recent calls:

```text
SparkAir calls are healthy
→ Circuit CLOSED
→ Calls are allowed

Failure rate crosses the configured limit
→ Circuit OPEN
→ Stop calling SparkAir temporarily

After a waiting period
→ Circuit HALF-OPEN
→ Allow a small test call

Test succeeds
→ Close circuit and resume normal traffic
```

When the circuit opens, DCP can immediately take another business path:

```text
SparkAir unavailable
        ↓
Try Cognize fallback
        ↓
If Cognize also fails
        ↓
Queue document for manual extraction
        ↓
Notify the L1 user
```

#### Business benefit

The circuit breaker does not repair SparkAir. It prevents one failing dependency from making the whole DCP slow or unavailable.

For the business user, this means:

- The upload request can return quickly instead of hanging.
- The user sees an honest status such as `QUEUED_FOR_RETRY` or `MANUAL_EXTRACTION_REQUIRED`.
- Documents continue through the fallback or manual path.
- Healthy parts of DCP, such as review and dissemination, remain responsive.
- Recovery is faster because the system does not create a huge pile of timed-out calls.

#### Why not just use retries?

Retries help with a short, temporary error:

```text
One network call fails → Retry after a short delay → Success
```

They make a long outage worse:

```text
2,000 documents × 3 retries = 6,000 calls to an unhealthy provider
```

Use retries for small transient failures. Open the circuit when failures continue.

### Pattern applicability in DCP

#### Service and data boundaries

| Pattern | Simple meaning | DCP business use case | Why it fits |
|---|---|---|---|
| Decompose by business capability | Create services around business responsibilities | Sourcing, Extraction, Rules/Quality, Workflow, Approval and Dissemination services | Each area has different rules, scaling needs and ownership |
| Database per service | A service owns its data and other services use its API or events | Approval owns review decisions; Extraction owns extracted document data; Workflow owns task state | Prevents one service from changing another service's data behind its back |
| Polyglot persistence | Use the database that best fits each type of data | PostgreSQL for structured metadata, MongoDB for flexible extracted documents, Elasticsearch for entity search, Redis for short-lived cache | DCP handles structured and highly variable document data |

#### Communication and API patterns

| Pattern | Simple meaning | DCP business use case | Why it fits |
|---|---|---|---|
| Synchronous API | Call another service and wait for an immediate answer | Approval UI loads the current document details; template validation returns immediately | The user needs an immediate response |
| Asynchronous messaging | Publish work and process it later | Sourcing publishes `DocumentSourced`; extraction workers consume documents from Kafka | Upload does not need to wait for slow AI extraction |
| API Gateway | One controlled entry point for clients | Routes L1/L2 UI requests and applies authentication, RBAC and rate limits | Keeps internal services private and applies common security rules |
| Backend for Frontend | API designed for a specific client | A Review UI BFF returns document image, extracted fields, confidence scores and validation errors in one response | Reviewers should not make many service calls from the browser |
| API composition / Aggregator | Fetch live information from several services when a request arrives | Operations screen requests live extraction status, workflow assignment and dissemination status | Useful when the screen needs fresh data from only a few healthy services |

#### Workflow and transaction patterns

| Pattern | Simple meaning | DCP business use case | Why it fits |
|---|---|---|---|
| Saga | Break one business transaction into local transactions | Approve document → publish data → notify downstream users | No single database transaction can cover Approval, S3 and Notification |
| Choreography | Services react to events without a central controller | `DocumentSourced` → `DocumentExtracted` → `QualityChecked` | The automatic pipeline is simple, high-volume and easy to parallelize |
| Orchestration | A central workflow component decides the next step | L1 assignment → L2 review → reminder → escalation → rework → final approval | The process must remember stages, deadlines, branches and reviewer state |
| Compensating transaction | Perform a business-level undo after partial failure | Publication fails after approval, so mark `APPROVED_NOT_PUBLISHED`, retry publication or revoke approval based on policy | The completed approval cannot be technically rolled back across services |

#### Reliable messaging and data patterns

| Pattern | Simple meaning | DCP business use case | Why it fits |
|---|---|---|---|
| Transactional outbox | Save business data and an event in one database transaction | Sourcing saves document metadata and an outbox `DocumentSourced` record together | Prevents a saved document from being lost to the pipeline because Kafka publication failed |
| Idempotent consumer | Safely ignore a duplicate message | Extraction uses `documentId + version/hash`; Dissemination uses a publication ID | Kafka redelivery must not extract or publish the same document twice |
| Dead-letter queue | Move repeatedly failing messages aside | Corrupt PDFs, unsupported formats or permanently invalid extraction events go to a DLQ | One bad document should not block the complete topic; operations can inspect and replay it |
| Event sourcing | Store every important state change as an immutable event | Record `SOURCED`, `EXTRACTED`, `QUALITY_CHECKED`, `APPROVED` and `PUBLISHED` | Financial data requires lineage, auditing and historical reconstruction |
| CQRS | Use a write model for changes and a read model optimized for queries | Events update a `DocumentSummary` view used by reviewer and operations dashboards | Users get a fast single view without calling every service |
| API composition | Combine live responses during the user request | Display a low-volume administrative detail screen using live service calls | Simpler than CQRS when traffic and fan-out are small and freshness is more important |

#### Resilience patterns

| Pattern | Simple meaning | DCP business use case | Business benefit |
|---|---|---|---|
| Timeout | Stop waiting after a safe limit | Stop waiting when SparkAir, Soniq or a destination API does not respond | The user request and worker do not hang forever |
| Retry with backoff and jitter | Retry temporary failures with increasing delays | Retry an S3 timeout or temporary destination API error | Handles short failures without creating an immediate traffic storm |
| Circuit breaker | Temporarily stop calls to a repeatedly failing dependency | Open the SparkAir circuit, use Cognize, then route to manual extraction if required | Users receive a quick alternative path instead of seeing every document stuck |
| Fallback | Use an alternative result or provider | SparkAir → Cognize → manual extraction | DCP continues delivering business value during provider outages |
| Bulkhead | Isolate resources used by different dependencies or workloads | Separate worker pools for AI extraction, Soniq entity mapping and dissemination | Slow entity mapping cannot consume all workers and stop document review |
| Rate limiting | Restrict how much work one caller can submit | Limit bulk upload or partner API traffic per tenant | One source cannot overload DCP and delay every other business user |
| Cache-aside | Read from cache first and load on a miss | Cache templates, extraction rules and Soniq entity mappings | Review and extraction become faster while expensive external calls decrease |

#### Scaling, discovery and operational patterns

| Pattern | Simple meaning | DCP business use case | Why it fits |
|---|---|---|---|
| Competing consumers | Multiple workers consume from the same topic | Scale extraction workers from 5 to 20 pods based on Kafka lag | Documents are processed in parallel while each partition is owned by one consumer |
| Service discovery | Find healthy service instances dynamically | Kubernetes Service DNS locates Extraction, Workflow and Approval pods | Pod addresses change during scaling and deployment |
| Sidecar | Run a supporting component beside the application | A telemetry or proxy sidecar exports logs, metrics and traces | Adds common operational behavior without duplicating it in every service |
| Service mesh | Apply common service-to-service network policies | Mutual TLS, traffic policies and observability across many DCP services | Useful when the number of services and security policies justifies the operational cost |

#### Migration patterns

| Pattern | Simple meaning | DCP business use case | Why it fits |
|---|---|---|---|
| Strangler Fig | Replace a legacy system one capability at a time | Route new document types to DCP while older types continue through the legacy collection platform | Reduces the risk of a full rewrite and allows gradual business migration |
| Anti-corruption layer | Translate between the new and legacy models | Convert legacy status codes and document schemas into DCP's canonical events | Prevents legacy concepts from spreading into new services |

### Detailed DCP business examples

The tables above are useful for quick revision. The following examples explain how each pattern affects the actual DCP workflow and its business users.

#### 1. Decompose by business capability

Split DCP according to business responsibilities:

```text
Sourcing Service
Extraction Service
Quality/Rules Service
Workflow Service
Approval Service
Dissemination Service
```

This fits DCP because each capability behaves differently:

- Extraction needs high compute and independent scaling.
- Approval contains human-review and authorization rules.
- Workflow manages assignments, deadlines and escalation.
- Dissemination supports different external destinations and formats.

Each capability can be owned, changed, deployed and scaled independently.

#### 2. Database per service

Each service owns its data:

```text
Extraction Service → extracted document data
Approval Service   → decisions and comments
Workflow Service   → assignments and deadlines
```

For example, Approval Service should not directly update Workflow Service tables. It should send a command or publish an event.

This protects service boundaries and prevents one team's database change from silently breaking another service.

#### 3. Synchronous API

Use synchronous communication when the user needs an immediate answer.

DCP examples:

```text
Reviewer opens a document
→ Fetch document details immediately

Administrator validates an extraction template
→ Return validation errors immediately
```

Do not make the complete extraction pipeline synchronous. AI extraction may take seconds or minutes, and the upload request should not remain open for the entire process.

#### 4. Asynchronous messaging

Use Kafka for slow or background processing:

```text
DocumentUploaded
      ↓
DocumentSourced
      ↓
DocumentExtracted
      ↓
QualityChecked
```

The user receives an upload acknowledgement without waiting for extraction to finish.

This helps DCP:

- Absorb large upload spikes.
- Process documents in parallel.
- Scale extraction workers independently.
- Continue accepting documents while a downstream stage is temporarily slow.

#### 5. API Gateway

The review applications use one controlled entry point:

```text
L1/L2 Review UI
       ↓
API Gateway
  ├── Document Service
  ├── Workflow Service
  └── Approval Service
```

The gateway handles:

- Authentication
- L1/L2 role permissions
- Rate limiting
- Routing
- Request logging

The business receives consistent security, while internal services remain private.

Core approval or extraction rules should remain inside their business services rather than being placed in the gateway.

#### 6. Backend for Frontend

A Review BFF returns everything needed by one reviewer screen:

```text
Document image
Extracted fields
Confidence scores
Validation errors
Reviewer assignment
Comments
```

Without a BFF, the browser may need to call many services and combine the results itself.

The BFF gives reviewers a faster and simpler user experience while hiding internal service boundaries from the UI.

#### 7. API composition

For a low-volume operations screen, an aggregator can fetch live data:

```text
Operations UI
      ↓
Aggregator
  ├── Extraction status
  ├── Workflow status
  └── Dissemination status
```

Use API composition when:

- Only a few services are involved.
- Live data is more important than consistently low latency.
- The query volume is moderate.

The trade-off is that the response becomes slow or incomplete when a participating service is slow or unavailable.

#### 8. CQRS

DCP events can maintain a pre-built document summary:

```text
DocumentSourced
DocumentExtracted
QualityChecked
DocumentApproved
DocumentPublished
        ↓
DocumentSummary read model
```

The dashboard performs one fast query:

```text
GET /documents/DOC-123
```

It does not call Sourcing, Extraction, Quality, Approval and Dissemination every time a reviewer opens the page.

Use CQRS when:

- The dashboard is requested frequently.
- Fast and predictable responses matter.
- The read view combines data owned by several services.
- A small delay before the view reflects the latest event is acceptable.

CQRS gives fast reads but introduces eventual consistency and requires monitoring of the projection consumers.

#### 9. Saga

A complete DCP business transaction may be:

```text
Approve document
→ Publish approved data
→ Notify downstream users
```

Each step belongs to a different service or external system. A single database transaction cannot cover all of them.

If publishing fails, the Saga applies the business recovery policy:

```text
Retry publication
or
Mark APPROVED_NOT_PUBLISHED
or
Revoke approval and return for review
```

Saga provides coordination and business-level recovery instead of a cross-service database rollback.

#### 10. Choreography

Use choreography for DCP's simple automatic pipeline:

```text
DocumentSourced
→ DocumentExtracted
→ QualityChecked
```

Each service reacts to the previous event and publishes its result.

This fits because the pipeline is:

- High volume
- Mostly sequential
- Easy to parallelize
- Suitable for independent service scaling

If the pipeline develops many branches, timers, rework loops or central visibility requirements, orchestration may become easier to manage.

#### 11. Orchestration

Use orchestration for a complex review workflow:

```text
Assign L1 reviewer
→ Wait for review
→ Send reminder
→ Escalate if overdue
→ Assign L2 reviewer
→ Handle approval, rejection or rework
```

The orchestrator remembers:

```text
Current reviewer
Current stage
Deadline
Review attempt
Previous decisions
```

The presence of a human is not itself the reason for orchestration. The reason is the amount of workflow state, timing and decision logic that must be managed.

#### 12. Transactional outbox

Consider this failure:

```text
Save document metadata ✅
Publish DocumentSourced ❌
```

The document exists in DCP but never enters extraction.

With an outbox, the service saves both records in one database transaction:

```text
BEGIN

Save document metadata
Save DocumentSourced in outbox

COMMIT
```

A background publisher later sends the outbox event to Kafka.

The business benefit is that an accepted document is not silently lost from the processing pipeline.

#### 13. Idempotent consumer

Kafka may deliver `DocumentSourced` more than once.

DCP can use a stable identifier:

```text
documentId + documentVersion or contentHash
```

The consumer records that identifier in the same local transaction as its business update:

```text
First delivery  → Extract and record the identifier
Second delivery → Identifier already exists, so skip
```

This prevents duplicate extraction, publication and notification.

#### 14. Dead-letter queue

A corrupt PDF may repeatedly fail extraction:

```text
Document event
→ Retry
→ Retry
→ Still fails
→ Dead-letter topic
```

The remaining documents continue processing.

Operations can inspect the failed message, correct its data or configuration and replay it.

A DLQ must have alerts, ownership and a recovery process. Otherwise, it becomes a place where failed business documents are forgotten.

#### 15. Event sourcing

Store the document's complete history:

```text
DocumentSourced
DocumentExtracted
QualityChecked
AssignedForReview
DocumentApproved
DocumentPublished
```

This is useful in DCP because financial-data systems need:

- Complete audit history
- Data lineage
- Historical state reconstruction
- Evidence of who approved a document and when

Event sourcing is valuable where these requirements justify the additional storage, schema-versioning and replay complexity.

#### 16. Timeout

DCP should not wait indefinitely for SparkAir, Soniq or a destination API:

```text
Call SparkAir
→ Stop waiting after the configured timeout
```

The document can then move to retry, fallback or manual handling instead of remaining stuck forever.

A timeout is the foundation for retries and circuit breakers. Without it, a call may consume a worker indefinitely.

#### 17. Retry with backoff

For a temporary S3 or network failure:

```text
Attempt 1 → fail
Wait 1 second
Attempt 2 → fail
Wait 2 seconds
Attempt 3 → success
```

Backoff and jitter prevent all workers from retrying at exactly the same time.

Use retries only for temporary failures and safe or idempotent operations. Validation failures, corrupt files and permission errors usually require correction rather than retry.

#### 18. Fallback

DCP can define a sequence of alternative extraction paths:

```text
SparkAir
→ Cognize
→ Manual extraction
```

The fallback may provide less automation, but it allows the business process to continue instead of stopping completely.

The user should see which path is being used and whether manual action is required.

#### 19. Bulkhead

Use separate worker pools or concurrency limits:

```text
AI extraction workers
Soniq entity-mapping workers
Dissemination workers
```

If Soniq becomes slow, it consumes only the resources assigned to entity mapping.

Reviewers can continue approving existing documents, and dissemination can continue publishing completed work.

The pattern is named after ship compartments: flooding one compartment should not sink the entire ship.

#### 20. Rate limiting

Suppose one partner uploads 50,000 documents at once.

Without rate limiting, that partner may use all available capacity and delay every other source.

With rate limiting:

```text
Partner A → controlled submission rate
Partner B → retains fair access to DCP
```

Rate limits can be applied per tenant, partner, API key or expensive operation.

The business benefit is predictable and fair service for all users.

#### 21. Cache-aside

Cache frequently reused data:

```text
Extraction templates
Validation rules
Soniq entity mappings
```

The service checks the cache first:

```text
Check cache
├─ Found   → Return quickly
└─ Missing → Load source data and cache it
```

This reduces database and external API calls while making extraction and review faster.

The cache needs an expiry or invalidation strategy so users do not continue seeing obsolete templates or rules.

#### 22. Competing consumers

Several Extraction Service pods consume from the same Kafka consumer group:

```text
Extraction topic
  ├── Worker 1
  ├── Worker 2
  ├── Worker 3
  └── Worker 20
```

Kafka assigns partitions among the active consumers.

When the backlog grows, Kubernetes can add workers. When it falls, workers can scale down.

The number of active consumers that can process simultaneously is limited by the number of topic partitions.

#### 23. Strangler Fig

During gradual migration:

```text
New document types → DCP
Old document types → Legacy platform
```

Capabilities move to DCP one at a time instead of replacing the complete legacy platform in one risky release.

The business benefit is lower migration risk and the ability to learn from each migrated capability before moving the next one.

#### 24. Anti-corruption layer

The legacy platform may use unclear models:

```text
Status = 17
Document type = FIN_USPF_V2
```

An anti-corruption layer translates them into DCP concepts:

```text
Status = APPROVED
Document type = AuditedFinancialStatement
```

The anti-corruption layer prevents legacy terminology and data structures from contaminating the new DCP domain model.

### Quick DCP decision guide

| DCP situation | Prefer |
|---|---|
| Upload should return before extraction finishes | Kafka asynchronous messaging |
| SparkAir fails briefly | Timeout and retry with backoff |
| SparkAir keeps failing | Circuit breaker and Cognize fallback |
| Both AI providers fail | Manual extraction queue and user notification |
| One malformed document repeatedly fails | Dead-letter queue |
| The same Kafka event is delivered again | Idempotent consumer |
| Save document metadata and reliably publish an event | Transactional outbox |
| Simple automated sourcing-to-quality pipeline | Choreography |
| L1/L2 review with timers, escalation and rework | Orchestration |
| Publication fails after approval | Saga compensation and controlled retry |
| Reviewer dashboard needs a fast combined document view | CQRS read model |
| Low-volume screen requires the freshest live status | API composition |
| Regulators need the complete document history | Event sourcing |
| Soniq becomes slow but extraction must continue | Bulkhead plus cache |
| One partner sends a very large traffic spike | Rate limiting |

### Architect interview summary

> For DCP, I use choreography and competing consumers for the high-volume automatic pipeline, and orchestration for the stateful L1/L2 workflow. Outbox and idempotency prevent lost and duplicate processing. CQRS gives reviewers a fast document summary, while event sourcing provides regulatory lineage. External providers are protected with timeout, controlled retry, circuit breaker, fallback and bulkhead. Each pattern is selected for a specific business failure or scaling concern, not simply because it is popular.

---

## Interview-ready answer

> In microservices, I would avoid a distributed ACID transaction across service databases. I would model the business transaction as a Saga made of local transactions. For a simple event pipeline, I would use choreography. For a complex workflow involving timeouts, branching or human approval, I would use orchestration. Each completed step would have a compensating action, and every message consumer would be idempotent because duplicate delivery is possible. I would use event sourcing only where complete history, auditing or replay provides enough business value to justify its complexity.
