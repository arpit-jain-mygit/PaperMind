# Local/In-Process Cache System (LRU / LFU Cache) - Design

## Revision #1

### Functional Requirements
a) Get and put operations for cache  
b) Eviction policy when cache is full (LRU, LFU, FIFO)  
c) Support TTL (time-to-live) for cache entries  
d) Clear cache operation  
e) Cache statistics (hit/miss rate)

### Non-Functional Requirements
- O(1) time complexity for get and put operations
- Memory efficient
- Highly concurrent access
- Support for multi-threading/distributed cache
- Scalable to millions of entries

---

## Design (Revision #1)

### DB Choice and Tables Design, Storage Requirements
- **Database**: Postgres RDBMS - id as primary key, value as JSON
- **Storage Calculation**: 
  - Key size: 10 bytes
  - Value size: 1 KB
  - Total entries: 100M
  - Required storage: 100M × (10 + 1000) bytes = 110 GB

### APIs Design

#### Get and Put Operations
- `/put` - store in NoSQL
- `/get` - fetch value by key

#### Eviction Policy
- Evict keys/values based on **LRU policy** when cache is full

#### TTL (Time-to-Live)
- TTL set to 1 hour per entry

#### Cache Statistics
- Target: 80% hit rate

### How to Achieve Functional Requirements

a) **Get and Put Operations**
   - `/put` - store in NoSQL
   - `/get` - fetch value by key

b) **Eviction Policy**
   - LRU policy: evict least recently used key/value

c) **Support TTL**
   - TTL = 1 hour

d) **Clear Cache Operation**
   - Requirement not clear

e) **Cache Statistics**
   - Requirement not clear

### How to Achieve Non-Functional Requirements

| Requirement | Approach |
|-------------|----------|
| **O(1) get/put** | Index on key column → O(1) fetch |
| **Memory efficient** | Requirement not clear |
| **Highly concurrent access** | Requirement not clear |
| **Multi-threading/distributed cache** | Requirement not clear |
| **Scalable to 100M+ entries** | Support 100M entries before eviction |

---

## Honest Feedback

### 🟢 What Will Work

1. **Identifying eviction policies** — LRU/LFU are valid, real-world choices
2. **Recognizing TTL need** — Time-to-live is essential for cache correctness
3. **Storage capacity calculation** — Math is sound for database-scale thinking
4. **80% hit rate target** — Reasonable goal for cache performance

### 🔴 Critical Issues - Won't Work As Stated

#### 1. **Wrong Technology for "In-Process" Cache**
- **Issue**: PostgreSQL is a *separate database server*, not "in-process"
- **Impact**: Every get/put requires network I/O + disk I/O
- **Reality**: Not truly in-process; you're designing a *cache client* to a remote DB
- **For in-process**: Data should live in application memory (HashMap, heap)

#### 2. **O(1) Claim is Unrealistic with PostgreSQL**
- **Get operation**: 
  - Index lookup = O(log n) B-tree traversal (not O(1))
  - Network round-trip ≈ 1-10ms per query
  - Deserialization overhead
- **Put operation**: Same issues + write latency
- **LRU tracking**: Every access needs an UPDATE to `last_accessed` timestamp (another write)
- **Reality**: This is O(log n) + network latency, not O(1)

#### 3. **Eviction Trigger Undefined**
- **Missing**: What's the actual cache size limit?
- **Missing**: Does it evict at 100M entries, 50GB, or when RAM pressure hits?
- **Issue**: Without a trigger, eviction policy can't be implemented
- **Current state**: Unclear when eviction starts

#### 4. **LRU + PostgreSQL = Contention Problem**
- **Issue**: Every read requires an UPDATE statement (last_accessed timestamp)
- **Problem**: 
  - Read becomes a write (locks, disk flush, replication)
  - Concurrent reads contend for locks
  - Cache hit rate ↓ due to write bottlenecks
- **Alternative needed**: Either lazy deletion or different eviction tracking

#### 5. **TTL at 1 Hour is Hardcoded**
- **Issue**: No configurability — real systems need flexible TTL
- **Missing**: How to handle expiration?
  - Active cleanup? (Background job scans every 1 hour — O(n))
  - Lazy deletion? (Check expiry on read — race conditions)
- **Current state**: Unclear mechanism

#### 6. **Concurrency Requirements Marked "Not Clear" — This is a Deal-Breaker**
| Scenario | Impact | Complexity |
|----------|--------|-----------|
| Single-threaded only | Can skip locking, simpler | But: not "highly concurrent" |
| Multi-threaded, same JVM | Need locks or atomic structures | Medium complexity |
| Distributed across machines | Need consensus, sharding, replication | Very high complexity |
| **Current state** | Undefined | **Can't proceed** |

- Without this, impossible to architect thread-safety

#### 7. **Storage Capacity Math Doesn't Match Use Case**
- 110GB is reasonable for a database
- But for "in-process" (in-memory), 110GB on a single JVM is:
  - Unrealistic (most servers: 32–256GB total RAM)
  - Would require aggressive garbage collection
  - Would leave no room for app code/other caches
- **Better approach**: Define realistic in-memory capacity (e.g., 1–10GB), then scale

#### 8. **"NoSQL" Backend Not Specified**
- Issue: Design says "store in NoSQL" but chooses PostgreSQL (SQL database)
- Inconsistency: What NoSQL DB? Why?
- Impact: Different databases have different latency profiles

#### 9. **Missing Critical Implementation Details**
- No mention of:
  - How to handle cache invalidation (on data update)
  - Memory pressure/GC impact
  - Retry logic for DB failures
  - Metrics/monitoring beyond hit rate
  - Warm-up/cold-start behavior

---

## Summary Table: Works vs. Won't Work

| Component | Status | Reason |
|-----------|--------|--------|
| Eviction concept (LRU/LFU) | ✅ Good | Valid policies, recognized as necessary |
| TTL concept | ✅ Good | Correct to include expiration |
| Storage math | ⚠️ Partial | Math is sound, but capacity unrealistic for in-process |
| O(1) guarantee | ❌ Won't work | PostgreSQL is O(log n) + network |
| "In-process" + PostgreSQL | ❌ Contradiction | These terms don't match |
| Concurrent access design | ❌ Won't work | Not addressed at all |
| Clear cache operation | ⚠️ Unclear | Implementation needed |
| Cache statistics | ⚠️ Partial | Only hit rate mentioned, no eviction counts/distribution |

---

## Questions to Resolve Before Next Revision

1. **Single-threaded or multi-threaded?** (Drives entire concurrency model)
2. **What's the actual memory limit?** (100M entries @ 1KB = unrealistic for one machine)
3. **In-process vs. distributed?** (Different architectures)
4. **Eviction trigger:** Evict when size > X or memory > Y?
5. **Database choice:** Why Postgres? Why not in-memory HashMap for true in-process?
6. **TTL cleanup:** Lazy deletion on get, or background job?
7. **LRU tracking:** How to update access order without write contention?

---

**Status**: Revision #1 captures good concepts but has fundamental architecture mismatches. Ready for Revision #2 once above questions are clarified.
