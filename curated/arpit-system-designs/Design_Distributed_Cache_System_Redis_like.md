# Design Distributed Cache System (Redis-like)

## MAANG Interview Readiness Score

| Revision | Score (out of 100) | Assessment | Chances to Clear | Comments |
|----------|-------------------|------------|-----------------|----------|
| #1 | 42/100 | **L4 Weak** - Significant gaps, incomplete thinking | 25% | Good foundation but too vague; needs deeper architectural design |

---

## Functional Requirements:
- Get, set, delete operations
- Support data types (string, list, set, hash, sorted set)
- Expiration/TTL support
- Pub/sub messaging
- Transactions (MULTI/EXEC)
- Persistence options

## Non-Functional Requirements:
- Ultra-high throughput (millions of ops/sec)
- Ultra-low latency (< 5ms)
- High availability with replication
- Fault tolerance and recovery
- Memory efficient
- Support for sharding/partitioning
- Atomic operations

---

## Revision #1 - How to Achieve Requirements

### Functional Requirements:

**a) Get, set, delete operations**
- APIs
  - /get
  - /set
  - /delete

**b) Support data types (string, list, set, hash, sorted set)**
- Support all by allowing them as key

**c) Expiration/TTL support**
- Allow user to config all types of TTL strategies

**d) Pub/sub messaging**
- Use Pub/sub to sync all the microservice instances

**e) Transactions (MULTI/EXEC)**
- ?

**f) Persistence options**
- Save in-memory cache into DB (persistence store)

### Non-Functional Requirements:

**a) Ultra-high throughput (millions of ops/sec)**
- Millions of transactions/requests need to be successfully processed per second. For this we need scalability at each layer:
  - App server with Kubernetes for Microservices
  - In-memory cache
  - DB Sharding
  - Replicas
  - DB connection pool

**b) Ultra-low latency (< 5ms)**
- Use DB indices
- Connection pooling
- In-memory cache
- App server with Kubernetes for Microservices
- DB Sharding
- Replicas

**c) High availability with replication**
- Target 99.99% uptime with AHM:
  - Automating - Automated failover, scaling, remediation, deployments
  - Health checks - Application, Infrastructure, dependencies
  - Multi-region - Web servers, Databases, Cache, DNS

**d) Fault tolerance and recovery**
- To be designed

**e) Memory efficient**
- In-memory cache

**f) Support for sharding/partitioning**
- To add details

**g) Atomic operations**
- To be designed

---

## Feedback on Revision #1

### Strengths:
✅ Good start identifying key components (sharding, replication, connection pooling)  
✅ Clear focus on the 99.99% uptime target with AHM framework  
✅ Recognizing the need for multi-region architecture

### Critical Gaps (Interview Readiness):

**1. Functional Requirements - Major Vagueness:**
- **(b) Data types handling**: "support all by allowing them as key" is circular. What does this mean exactly? You need to explain the data model:
  - How are different types stored? (In-memory data structures per type?)
  - How are they indexed?
  - Example: "String → byte array, List → doubly-linked list or array, Set → hash table" 

- **(d) Pub/Sub scope confusion**: Are you using Pub/Sub for:
  1. Redis feature (message passing between clients)? OR
  2. Internal cache sync between distributed instances?
  
  These are DIFFERENT. Be explicit.

- **(e) Transactions**: Don't leave this as "?". This is critical for interviews. Minimum:
  - How do you ensure atomicity? (Locks? Optimistic locking?)
  - Isolation level? (Read Uncommitted? Serializable?)
  - What happens on failure mid-transaction?

- **(f) Persistence**: Too vague. Need:
  - When do you persist? (Real-time? Periodic snapshots?)
  - RDB vs AOF trade-off?
  - How do you prevent data loss during persistence?

**2. Non-Functional Requirements - Too Generic:**
- **(a & b) Throughput & Latency**: You listed components but NO actual analysis:
  - "millions of ops/sec" - What's your throughput calculation per cache node?
  - "< 5ms latency" - This is aggressive. Network latency alone is 1-2ms. How do you achieve it?
  - Missing: Serialization overhead, network hops, in-memory lookup time estimates

- **(c) High Availability**: No detail on:
  - Master-Slave vs Multi-Master replication?
  - How do replicas stay in sync? (Synchronous? Asynchronous?)
  - Consistency guarantees during failover?
  - Split-brain handling?

- **(d, f, g) "To be designed"**: These need at least skeleton designs:
  - **Fault Tolerance**: Plan for node failures, network partitions, data loss scenarios
  - **Sharding**: Consistent hashing? Hash slot approach? How do you handle resharding?
  - **Atomic Operations**: Locking strategy? CAS (Compare-And-Set)? Pessimistic vs optimistic?

**3. Architecture Missing:**
- No clear component diagram showing:
  - How do cache nodes talk to each other?
  - Where does the Pub/Sub fit?
  - Client connection flow?
- No mention of:
  - Eviction policies (LRU? LFU? TTL-based?)
  - Memory limits and management
  - Consistency model (Strong? Eventual?)

### Interview Red Flags:
🚩 Multiple "to be designed" items - shows incomplete thinking  
🚩 Vague explanations (e.g., "allow them as key") - won't pass scrutiny  
🚩 No trade-off analysis - just lists components  
🚩 Missing concurrency/locking strategy - critical for cache correctness

### Next Steps:
1. Design the data model explicitly (what in-memory structure per data type?)
2. Add replication strategy section (master-slave protocol details)
3. Flesh out at least one "to be designed" section fully (e.g., Atomic Operations)
4. Add consistency model (eventual vs strong) with implications
5. Include memory management & eviction policy design
