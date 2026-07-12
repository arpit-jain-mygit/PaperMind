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

---

## Revision #2 - Implementation Design (HashMap + DoublyLinkedList)

### Core Approach

**Data Structures Used**:
1. **DoublyLinkedList (DLL)** — Maintains insertion/access ordering for O(1) add/remove/update and eviction
2. **HashMap (HM)** — Fast O(1) key-to-node lookup

**Initial State**:
```
DLL: NULL <--- A <---> B <---> C ---> NULL
HM:  {"A": Node A, "B": Node B, "C": Node C}
```

### Get Operation

**Scenario**: Get value of "B"

Steps:
```
i)   Check in HM, if key "B" exists → get Node "B"
ii)  Fetch Node "B"'s value from DLL
iii) Move Node "B" to the end (mark as most recently used)
```

**After Get("B")**:
```
DLL: NULL <--- A <---> C <---> B ---> NULL  (B moved to end)
HM:  (unchanged, same references)
```

**Complexity**: O(1) for all steps

---

### Put Operation

**Case (a): Key Exists** — Update existing value

**Scenario**: Update key "B" with new value B2 (previously B1)

Steps:
```
i)   Go to DLL, update Node "B"'s value from B1 to B2
ii)  Move Node "B" to the end (mark as most recently used)
iii) HM already points to Node "B", no update needed
```

**After Put("B", B2)**:
```
Old DLL: NULL <--- A <---> C <---> B (value=B1) ---> NULL
New DLL: NULL <--- A <---> C <---> B (value=B2) ---> NULL  (B moved to end)
HM:      (unchanged, reference is same)
```

**Complexity**: O(1)

---

**Case (b): Key Not Present** — Add new entry (with eviction if needed)

**Scenario**: Put key "D" with value D1 when cache is at max capacity

Steps:
```
i)   Check if cache reached max capacity → YES, evict leftmost node (LRU)
ii)  Delete node "A" from DLL
iii) Delete entry "A" from HM
iv)  Add new node "D" at the end of DLL
v)   Add new entry "D" to HM pointing to Node "D"
```

**After Put("D", D1)**:
```
Old DLL: NULL <--- A <---> C <---> B ---> NULL
New DLL: NULL <--- C <---> B <---> D ---> NULL  (A evicted, D added)
HM:      {"C": Node C, "B": Node B, "D": Node D}  (A removed, D added)
```

**Complexity**: O(1)

---

## Honest Feedback on Revision #2

### 🟢 What Will Work

1. ✅ **Data Structure Choice** — HashMap + DLL is the industry-standard LRU implementation
2. ✅ **O(1) Complexity** — All operations (get, put, evict) are truly O(1)
3. ✅ **LRU Logic** — Correctly moves accessed/updated nodes to end (most recent)
4. ✅ **Eviction Strategy** — Correctly evicts from leftmost (least recent)
5. ✅ **Clear Operation Walkthrough** — Step-by-step examples make logic easy to follow
6. ✅ **Simple and Practical** — No external dependencies, purely in-process

### 🔴 Critical Issues - Won't Work As Stated

#### 1. **Put Case (a) Example Has Inconsistency**

You show:
```
Old DLL: NULL <--- A <---> C <---> B (value=B1) ---> NULL
New DLL: NULL <--- A <---> C <---> B (value=B2) ---> NULL
```

**Problem**: Both DLLs are identical — B didn't move!  
If you're updating "B", it should move to the end.

**Should be**:
```
Old DLL: NULL <--- A <---> B <---> C ---> NULL  (B at position 2)
New DLL: NULL <--- A <---> C <---> B ---> NULL  (B moved to end)
```

**Impact**: The logic is correct, but the example creates confusion.

---

#### 2. **HM Update Description Misleading in Case (a)**

You wrote:
> "go to HM and update key/value (node's new pointers)"

**Clarification**:
- HM stores a reference to Node "B"
- When you move Node "B" in DLL, the reference in HM does **NOT** change
- Only Node "B"'s internal `prev` and `next` pointers change
- **HM does NOT need updating in case (a)**

```java
// What actually happens:
Node nodeB = HM.get("B");           // ← Reference unchanged
nodeB.value = newValue;             // ← Node's value updated
moveToDLL_End(nodeB);               // ← Node's prev/next change, not HM
// HM is NOT accessed again
```

In case (b), HM **does** need a new entry since it's a new node.

---

#### 3. **Edge Cases Not Addressed**

Missing scenarios:
- ❌ What if cache size is 1? (Evict, add, only 1 node)
- ❌ What if cache is full and we get an existing key? (Should NOT evict, just move to end)
- ❌ What if DLL becomes empty after eviction? (Should still work)
- ❌ How to handle sentinel nodes (head/tail)? (Simplifies null checks)

**Example edge case**:
```
Cache size = 3, current: A <-> B <-> C (full)
Get("B"):  Should NOT evict, just move to end
Result: A <-> C <-> B  (no eviction)

Put("D"):  Now full again AND has new key, THEN evict
Result: C <-> B <-> D  (A evicted)
```

---

#### 4. **Capacity Boundary Undefined**

You wrote:
> "evict (delete) left most node from DLL and same entry from HM, if cache reached to max capacity"

**Ambiguity**: When exactly to evict?

**Standard approach**:
```
if (size >= maxCapacity) {
    evictLRU();      // Remove least recent
}
addNewNode();        // Then add new node
```

**Current wording** ("if reached") could mean:
- Evict BEFORE adding (size stays at max)
- Evict AFTER adding (size goes over, then corrected)

**Recommend**: Define: `if (size >= maxCapacity) evict(); then add();`

---

#### 5. **Concurrent Access NOT Addressed**

Your Revision #1 requirements stated:
> "Highly concurrent access"
> "Support for multi-threading"

**This revision ignores it completely.**

**Missing**: Synchronization strategy for multi-threaded access

**Options**:
```
Option A: Coarse-grained lock
    ReentrantReadWriteLock lock;
    get(key) { lock.readLock().lock(); try { ... } finally { unlock; } }
    put(key, value) { lock.writeLock().lock(); try { ... } finally { unlock; } }

Option B: ConcurrentHashMap + atomic moves (complex)

Option C: Segment-based sharding (for high concurrency)
```

**Impact**: Without synchronization, concurrent get/put on same key causes:
- Data corruption (DLL pointers)
- Lost updates
- Race conditions

---

#### 6. **TTL Support Missing**

Revision #1 requirement:
> "Support TTL (time-to-live) for cache entries"

**This revision doesn't address TTL at all.**

**Missing**:
- When do entries expire?
- Who deletes expired entries? (Background job? Lazy deletion on get?)
- What's the TTL value? (Configurable? Fixed?)

**Example missing scenarios**:
```
Put("X", value1, TTL=10min)
Wait 5 min
Get("X") → should return value1 ✓

Wait 10 more min (total 15 min > 10 min TTL)
Get("X") → should be expired, cache miss, fetch from DB ✗ (not addressed)
```

---

#### 7. **Clear Cache Operation Not Addressed**

Revision #1 requirement:
> "Clear cache operation"

**Missing**: How to clear all entries?

**Should implement**:
```
clear() {
    DLL.removeAll()     // Remove all nodes
    HM.clear()          // Remove all entries
    size = 0
    hitCount = 0
    missCount = 0
}
```

---

#### 8. **Cache Statistics Not Detailed**

Revision #1 requirement:
> "Cache statistics (hit/miss rate)"

**Missing details**:
- How to track hits and misses?
- Separate counters for each?
- Thread-safe counters? (AtomicInteger for multi-threaded)
- When to reset statistics?

**Should track**:
```
- totalHits (incremented on cache hit)
- totalMisses (incremented on cache miss)
- hitRate = totalHits / (totalHits + totalMisses)
```

---

#### 9. **API Contract Undefined**

Missing clarity on:
- ❌ What does get() return if key not found? (null? Optional? Exception?)
- ❌ What does put() return? (void? old value?)
- ❌ Does put() throw exception if cache is full and eviction fails?
- ❌ What if put() receives null value? (Allow or reject?)

---

#### 10. **Memory Efficiency Not Discussed**

You addressed it in Revision #1 as "not clear", now it's ignored.

**Questions**:
- What's the max cache size? (1GB? 10GB? Number of entries?)
- What happens if cache grows beyond available heap? (OOM exception?)
- How to handle memory pressure?

---

## Summary Table: Revision #2 Status

| Component | Status | Note |
|-----------|--------|------|
| **Data structures** | ✅ Correct | HashMap + DLL is standard |
| **O(1) complexity** | ✅ Correct | All ops are O(1) |
| **Get logic** | ✅ Correct | Move to end works |
| **Put logic (new key)** | ✅ Correct | Evict + add works |
| **Put logic (existing key)** | ⚠️ Unclear | Example is wrong (B doesn't move) |
| **HM behavior** | ⚠️ Confusing | Says "update" but HM unchanged in case (a) |
| **Edge cases** | ❌ Missing | Boundary conditions not handled |
| **Capacity boundary** | ⚠️ Ambiguous | When to evict not clear |
| **Concurrent access** | ❌ Missing | No synchronization strategy |
| **TTL support** | ❌ Missing | Required but not addressed |
| **Clear operation** | ❌ Missing | Required but not addressed |
| **Cache statistics** | ❌ Missing | Required but not addressed |
| **API contract** | ❌ Missing | Return types, null handling unclear |
| **Memory efficiency** | ❌ Missing | Cache size limits not defined |

---

## Verdict

**Core LRU logic is solid** — HashMap + DLL is correct and O(1).

**But Revision #2 is incomplete**:
- ✅ Addresses "in-process cache" fundamental problem from Revision #1
- ✅ Provides correct data structures and complexity
- ❌ Ignores 5 functional requirements (TTL, clear, statistics, multi-threading, capacity limits)
- ❌ Examples have errors (case (a) inconsistency)
- ❌ Edge cases undefined
- ❌ API contract missing

---

## Questions for Revision #3

1. **Concurrency**: How to make this thread-safe? (Lock strategy?)
2. **TTL**: How to implement time-to-live? (Lazy deletion? Background cleanup?)
3. **Clear operation**: How to clear all entries efficiently?
4. **Statistics**: How to track hit/miss counts in O(1)?
5. **Capacity**: What's the max cache size? How to handle overflow?
6. **API**: What should get/put return? How to handle edge cases?
7. **Fix example**: Put case (a) — B should move to end in the example
8. **Clarify HM**: Explain that HM doesn't need update in case (a), only case (b)

---

**Status**: Revision #2 solves "in-process cache" problem but leaves functional requirements incomplete. Revision #3 should address concurrency, TTL, and API contract.
