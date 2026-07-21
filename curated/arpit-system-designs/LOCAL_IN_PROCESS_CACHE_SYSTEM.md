# Local/In-Process Cache System (LRU / LFU Cache) - Design

## Table of Contents

### Revision #1 - High-Level Design (Postgres + Multi-Instance)
- [Functional Requirements](#functional-requirements)
- [Non-Functional Requirements](#non-functional-requirements)
- [Design (Revision #1)](#design-revision-1)
  - [DB Choice and Storage Requirements](#db-choice-and-tables-design-storage-requirements)
  - [APIs Design](#apis-design)
  - [How to Achieve Functional Requirements](#how-to-achieve-functional-requirements)
  - [How to Achieve Non-Functional Requirements](#how-to-achieve-non-functional-requirements)
- [Honest Feedback on Revision #1](#honest-feedback)
- [Summary Table: Works vs. Won't Work](#summary-table-works-vs-wont-work)
- [Questions to Resolve Before Next Revision](#questions-to-resolve-before-next-revision)

### Revision #2 - Implementation Design (HashMap + DoublyLinkedList)
- [Core Approach](#core-approach)
- [Get Operation](#get-operation)
- [Put Operation](#put-operation)
  - [Case (a): Key Exists](#case-a-key-exists--update-existing-value)
  - [Case (b): Key Not Present](#case-b-key-not-present--add-new-entry-with-eviction-if-needed)
- [Honest Feedback on Revision #2](#honest-feedback-on-revision-2)
- [Summary Table: Revision #2 Status](#summary-table-revision-2-status)
- [Verdict](#verdict)
- [Questions for Revision #3](#questions-for-revision-3)

### Revision #3 - NFRs Implementation Approach
- [Core Approach (Same as R2)](#core-approach-same-as-r2)
- [Get Operation (Same as R2)](#get-operation-same-as-r2)
- [Put Operation (Same as R2)](#put-operation-same-as-r2)
- [NFRs Implementation](#nfrs-implementation)
- [Changes from Revision #2](#changes-from-revision-2)
- [Honest Feedback on Revision #3](#honest-feedback-on-revision-3)
- [Summary: R3 Status](#summary-r3-status)

---

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

---

## Revision #3 - NFRs Implementation Approach

### Core Approach (Same as R2)

**Data Structures Used**: DoublyLinkedList (DLL) + HashMap (ConcurrentHashMap)

**Initial State**:
```
DLL: NULL <--- A <---> B <---> C ---> NULL
HM:  {"A": Node A, "B": Node B, "C": Node C}
```

---

### Get Operation (Same as R2)

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
HM:  (unchanged)
```

**Complexity**: O(1)

---

### Put Operation (Same as R2)

#### Case (a): Key Exists — Update existing value

**Scenario**: Update key "B" with new value B2

Steps:
```
i)   Update Node "B"'s value from B1 to B2 in DLL
ii)  Move Node "B" to the end
iii) HM already points to Node "B" (no update needed)
```

**After Put("B", B2)**:
```
Old DLL: NULL <--- A <---> B <---> C ---> NULL
New DLL: NULL <--- A <---> C <---> B ---> NULL  (B moved to end)
```

**Complexity**: O(1)

---

#### Case (b): Key Not Present — Add new entry with eviction if needed

**Scenario**: Put key "D" when cache is at max capacity

Steps:
```
i)   Check if size >= maxCapacity → YES, evict leftmost node (LRU)
ii)  Delete node "A" from DLL and from HM
iii) Add new node "D" at end of DLL
iv)  Add new entry "D" to HM pointing to Node "D"
```

**After Put("D", D1)**:
```
Old DLL: NULL <--- A <---> C <---> B ---> NULL
New DLL: NULL <--- C <---> B <---> D ---> NULL  (A evicted, D added)
HM:      {"C": Node C, "B": Node B, "D": Node D}
```

**Complexity**: O(1)

---

## NFRs Implementation

### 1. O(1) Time Complexity for Get and Put Operations

**Approach**: 
- DLL and ConcurrentHashMap used for O(1) get/put time
- HashMap lookup: O(1)
- DLL node movement: O(1) (updating prev/next pointers)
- Eviction from front: O(1) (direct pointer access)

**Result**: All operations achieve O(1) ✅

---

### 2. Multi-threaded Support (Concurrent Access)

**Approach**:
- ConcurrentHashMap is used to support thread-safe HashMap operations
- HashMap operations (get, put, remove) are thread-safe

**Result**: HashMap operations are thread-safe ✅

---

### 3. Memory Efficient

**Approach**:
- DLL used to maintain only required nodes in memory
- Eviction triggered when cache reaches maxCapacity
- Each entry: ~1KB (8 bytes prev pointer + 8 bytes next pointer + 1KB value + 10 bytes key ~ 1040 bytes)

**Storage Calculation**:
- Per entry: ~1KB
- 10M entries: 10M × 1KB = 10GB
- Total RAM required per instance: ~32GB (with overhead)

**Result**: Memory usage is bounded and predictable ✅

---

### 4. Scalable to Millions of Entries

**Approach**:
- Can support 10M entries with 10GB memory
- DLL allows O(1) eviction without scanning
- ConcurrentHashMap handles distributed hash bucket allocation

**Capacity**: Up to 10M entries before requiring larger machines

**Result**: Scalable architecture ✅

---

## Changes from Revision #2

| Aspect | R2 | R3 | Change |
|--------|----|----|--------|
| **Data Structures** | HashMap + DLL | HashMap + DLL | No change |
| **Get Operation** | ✅ Defined | ✅ Same | Reaffirmed |
| **Put Operation** | ✅ Defined | ✅ Same | Reaffirmed |
| **O(1) Complexity** | ⚠️ Claimed but not detailed | ✅ Explained | Clarified HOW it's O(1) |
| **Thread-safety** | ❌ Missing | ✅ ConcurrentHashMap | Added thread-safety approach |
| **Memory Efficiency** | ❌ Poorly explained | ✅ Bounded cache | Clarified with capacity limits |
| **Scalability** | ⚠️ Vague math | ✅ Concrete numbers | Calculated 10M → 10GB → 32GB |
| **Eviction trigger** | ⚠️ Undefined | ✅ Defined | When size >= maxCapacity |
| **NFRs addressing** | ❌ Incomplete | ✅ Explicit | Map NFRs to implementation |

---

## Honest Feedback on Revision #3

### 🟢 What Works

1. ✅ **Core LRU Logic** — HashMap + DLL is correct and proven
2. ✅ **O(1) Complexity** — All operations achieve O(1) time
3. ✅ **Memory Calculation** — Concrete numbers (10M entries = 10GB = reasonable)
4. ✅ **Eviction Clear** — Triggers at maxCapacity, removes LRU
5. ✅ **Scalability Addressed** — Defined capacity boundary
6. ✅ **NFRs Explicitly Mapped** — Each NFR has implementation approach

### 🔴 Critical Issues - Still Won't Work As Stated

#### 1. **ConcurrentHashMap Alone is NOT Sufficient for Thread-Safety**

You stated:
> "ConcurrentHashMap is used to support thread-safe HashMap operations"

**Problem**: ConcurrentHashMap only protects HashMap. DLL node movements are NOT thread-safe.

**Race condition example**:
```
Thread 1: Get("B") - moving B to end
  - Read: B.prev = A, B.next = C
  - Update: A.next = C, C.prev = A, B.prev = C, B.next = null
  - [PAUSE during pointer updates]

Thread 2: Get("C") - moving C to end
  - Read: C.prev = B (from Thread 1)
  - Update: B.next = null, C.prev = B, C.next = null
  - [Context switches back to Thread 1]

Result: Corrupted DLL pointers! ❌
```

**Missing**: ReentrantReadWriteLock for DLL node movements

```java
ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

public get(key) {
    lock.readLock().lock();  // Multiple readers
    try {
        Node node = HM.get(key);           // HashMap is thread-safe
        if (node != null) {
            moveToEnd(node);  // DLL update is PROTECTED by lock
        }
    } finally {
        lock.readLock().unlock();
    }
}

public put(key, value) {
    lock.writeLock().lock();  // Exclusive access
    try {
        // ... put + evict logic
    } finally {
        lock.writeLock().unlock();
    }
}
```

**Current state**: Thread-safe HashMap ≠ Thread-safe Cache

---

#### 2. **TTL Not Addressed**

Revision #1 requirement: **"Support TTL (time-to-live) for cache entries"**

You: **No mention of TTL at all**

**Missing**:
- When do entries expire?
- Who removes expired entries? (Lazy deletion on Get? Background job?)
- What's the TTL value? (Configurable?)

**Should add**:
```
Option A: Lazy deletion (simplest)
  - Store: Node.createdAt = System.currentTimeMillis()
  - On Get: if (now - createdAt > TTL) return cache miss

Option B: Background cleanup (resource-intensive)
  - Periodic thread scans DLL for expired entries
  - Removes expired entries
  - Problem: O(n) scan

Option C: Expiration queue (complex but efficient)
  - Maintain priority queue of expiration times
  - Clean on fixed schedule
  - Most complex to implement
```

**Currently**: ❌ Missing

---

#### 3. **Clear Operation Not Addressed**

Revision #1 requirement: **"Clear cache operation"**

You: **No mention**

**Missing**:
```java
public void clear() {
    lock.writeLock().lock();
    try {
        HM.clear();
        DLL.removeAll();  // How? No method defined
        size = 0;
    } finally {
        lock.writeLock().unlock();
    }
}
```

---

#### 4. **Cache Statistics Not Addressed**

Revision #1 requirement: **"Cache statistics (hit/miss rate)"**

You: **No mention**

**Missing**:
```java
// How to track?
AtomicLong hits = new AtomicLong(0);
AtomicLong misses = new AtomicLong(0);

public get(key) {
    // ... logic
    if (found) {
        hits.incrementAndGet();
    } else {
        misses.incrementAndGet();
    }
}

public double getHitRate() {
    long total = hits.get() + misses.get();
    return (double) hits.get() / total;
}
```

---

#### 5. **Variable Value Sizes Not Addressed**

You assumed: **"value will be 1KB"**

**Reality**: Values vary (100 bytes to 100MB)

**Current approach**: 
- Assumes all values = 1KB
- If actual values are 10KB, cache holds 1M entries = 10GB (not 10M)
- Should handle variable sizes

**Missing**:
- Define: Is 1KB per value a hard limit?
- Or maxCapacity in bytes (e.g., 10GB total)?
- If variable sizes, how to enforce maxCapacity?

---

#### 6. **Capacity Boundary Logic Incomplete**

You stated:
> "evict when size >= maxCapacity"

**Missing details**:
- Is maxCapacity = 10M entries (as calculated)?
- Or maxCapacity = 10GB memory?
- What if value sizes vary?
- Should multiple evictions occur if new entry is larger than evicted entry?

**Example edge case**:
```
maxCapacity = 10GB
Current: 5M entries × 2KB each = 10GB (full)
New Put: 4MB value
Should evict how many entries to fit 4MB?
→ Evict 2 LRU entries (4MB < 4MB)? Or 3 to be safe?
```

---

#### 7. **API Contract Missing**

No clarity on:
- ❌ Get(key) returns what if key not found? (null? Optional? throw exception?)
- ❌ Put(key, value) returns void? old value?
- ❌ How to set maxCapacity? (Constructor? Configuration?)
- ❌ How to set TTL? (Global? Per entry?)
- ❌ Thread-safety guarantees? (Already covered by ReentrantReadWriteLock)

---

#### 8. **Concurrent Eviction Not Addressed**

**Scenario**:
```
Thread 1: Put("X", value) - cache full, evicts LRU
Thread 2: Put("Y", value) - cache full, evicts same LRU?
→ Evict same node twice?
```

**With ReentrantReadWriteLock**: write-lock is exclusive, so only one thread evicts at a time ✅
**But not mentioned in R3** ⚠️

---

#### 9. **GC Impact Not Mentioned**

10M Node objects in JVM heap = millions of GC events
- Long GC pauses (stop-the-world) → latency spikes
- Affects p99 latency under GC pressure

**Not addressed** ❌

---

## Summary: R3 Status

| Component | Status | Change from R2 |
|-----------|--------|----------------|
| **Data structures** | ✅ | No change (still correct) |
| **Get operation** | ✅ | No change (reaffirmed) |
| **Put operation** | ✅ | No change (reaffirmed) |
| **O(1) complexity** | ✅ | Better explained |
| **Thread-safety** | ⚠️ | ConcurrentHashMap mentioned, but incomplete |
| **Memory efficiency** | ✅ | Bounded with concrete numbers |
| **Scalability** | ✅ | Defined capacity (10M entries) |
| **TTL support** | ❌ | Still missing |
| **Clear operation** | ❌ | Still missing |
| **Cache statistics** | ❌ | Still missing |
| **API contract** | ❌ | Still missing |
| **Eviction edge cases** | ⚠️ | Partially addressed (trigger defined) |
| **Variable value sizes** | ❌ | Not addressed |

---

## Verdict

**Revision #3 Progress**:
- ✅ **Better than R2**: NFRs explicitly mapped to implementation
- ✅ **Concrete**: Defines capacity (10M = 10GB = 32GB)
- ✅ **Clearer**: How O(1) is achieved explained
- ⚠️ **Still Incomplete**: Missing thread-safety details (ReentrantReadWriteLock not mentioned)
- ❌ **Still Missing**: TTL, Clear, Statistics, Variable sizes, API contract

---

## Questions for Revision #4 (If Needed)

1. **Thread-safety**: Will you use ReentrantReadWriteLock for DLL protection?
2. **TTL**: Which approach? (Lazy deletion, background cleanup, or expiration queue?)
3. **Clear operation**: How to efficiently clear all entries?
4. **Statistics**: Atomic counters for hits/misses?
5. **Variable sizes**: How to handle different value sizes under maxCapacity?
6. **API**: Define return types, null handling, configuration options
7. **Edge cases**: Handle concurrent evictions, capacity boundary precision

---

**Status**: Revision #3 adds concrete numbers and NFR mapping, but critical features (TTL, Clear, Stats) and thread-safety details (ReentrantReadWriteLock) still need implementation.

---

## Comprehensive Assessment: Revision #3 Deep Dive

### 🎯 Honest Feedback on Final R3

#### ✅ Excellent Additions (What You Got Right)

1. **✅ SMART Assumptions** 
   - "No sync needed, accept out-of-sync caches + session stickiness"
   - Shows understanding of distributed system trade-offs
   - Realistic for microservices architecture

2. **✅ API Contracts DEFINED**
   - GET /get(key) → return value or exception
   - POST /put(key, value) → return entry or error
   - Clear error handling semantics

3. **✅ TTL Strategies COMPREHENSIVE**
   - Time-based, Access-based (sliding window), Adaptive, Lazy, Refresh-ahead
   - Shows deep understanding of cache patterns
   - Different TTLs per data type (5min sessions, 1hr catalog, 1day profile, 1week config)
   - **This is excellent production thinking**

4. **✅ Config Strategy CLEAR**
   - Configurable at startup via app config
   - Separate TTLs per data type
   - Realistic for production deployments

5. **✅ Edge Cases HANDLED**
   - Cache size = 1 ✓
   - Empty cache Get ✓
   - Empty DLL after eviction ✓
   - Concurrent get+put on same key (ReentrantReadWriteLock) ✓

6. **✅ Thread-Safety DEFINED** — ReentrantReadWriteLock for DLL operations

7. **✅ Memory Efficiency BOUNDED** — 10M entries = 10GB = 32GB RAM per instance

---

#### 🔴 Critical Issues - What's Still Missing

##### 1. **Lazy TTL Implementation Details**

You mentioned "Lazy TTL" but no pseudo-code:

```java
// MISSING: How is createdAt/accessedAt stored?
// Should be in Node class:
class Node {
    String key;
    Object value;
    long createdAt;          // For time-based TTL
    long lastAccessedAt;     // For access-based TTL
    Node prev, next;
}

// MISSING: Check logic on Get
public get(key) {
    lock.readLock().lock();
    try {
        Node node = HM.get(key);
        if (node == null) return "not found";
        
        // Check if expired (MISSING LOGIC)
        if (isExpired(node)) {
            // Remove from cache
            // Return cache miss
        }
        
        moveToEnd(node);
        return node.value;
    } finally {
        lock.readLock().unlock();
    }
}

private boolean isExpired(Node node) {
    long ttl = getTTL(node.key);  // Get TTL for this key's data type
    return (System.currentTimeMillis() - node.createdAt) > ttl;
}
```

**Currently**: ❌ Pseudo-code missing

---

##### 2. **"Refresh-Ahead" Implementation Missing**

You mentioned "refresh before TTL expiry" but how?

```java
// MISSING: Background task for refresh-ahead
// Should have scheduled executor:
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

scheduler.scheduleAtFixedRate(() -> {
    long now = System.currentTimeMillis();
    for each entry in DLL {
        long ttl = getTTL(entry.key);
        if ((now - entry.createdAt) > (ttl * 0.8)) {  // 80% of TTL
            // Refresh from DB
            freshValue = fetchFromDB(entry.key);
            entry.value = freshValue;
            entry.createdAt = now;  // Reset TTL
        }
    }
}, 0, 1, TimeUnit.MINUTES);  // Run every 1 minute
```

**Currently**: ❌ No background job mechanism

---

##### 3. **Clear Operation Not Detailed**

You said "Clear operation implied" but:

```java
// MISSING: Pseudo-code for clear()
public void clear() {
    lock.writeLock().lock();
    try {
        HM.clear();
        // How to clear DLL efficiently?
        // Option A: Iterate and remove each node (O(n))
        // Option B: Reset head/tail pointers (O(1), wastes memory)
        DLL.removeAll();
        stats.reset();  // Reset hit/miss/eviction counters
    } finally {
        lock.writeLock().unlock();
    }
}
```

**Currently**: ⚠️ Partially addressed

---

##### 4. **Cache Statistics Not Detailed**

You said "cache statistics" but no implementation:

```java
// MISSING: How to track atomic counters
class CacheStats {
    AtomicLong totalHits = new AtomicLong(0);
    AtomicLong totalMisses = new AtomicLong(0);
    AtomicLong totalEvictions = new AtomicLong(0);
    AtomicLong totalExpiredEntriesRemoved = new AtomicLong(0);
    
    public void recordHit() {
        totalHits.incrementAndGet();
    }
    
    public void recordMiss() {
        totalMisses.incrementAndGet();
    }
    
    public double getHitRate() {
        long total = totalHits.get() + totalMisses.get();
        return total == 0 ? 0 : (double) totalHits.get() / total;
    }
    
    public void recordEviction() {
        totalEvictions.incrementAndGet();
    }
}
```

**Currently**: ⚠️ Mentioned but no implementation

---

##### 5. **Batch TTL Cleanup Job Not Detailed**

You said "batch job for TTL cleanup" but no design:

```java
// MISSING: How does batch cleanup work concurrently?
// Critical questions:
// - How often? Every 1 min? 5 min? 1 hour?
// - Does it hold write-lock entire time? (Blocks all reads/puts)
// - How to handle concurrent Put during cleanup?
// - What if cleanup takes longer than scheduled interval?

// Better approach - use read-lock for scanning, write-lock only for removal:
ScheduledExecutorService batchCleanup = Executors.newScheduledThreadPool(1);

batchCleanup.scheduleAtFixedRate(() -> {
    // Step 1: Identify expired entries (can use read-lock)
    lock.readLock().lock();
    List<String> expiredKeys = new ArrayList<>();
    try {
        Node current = DLL.head.next;
        while (current != null) {
            if (isExpired(current)) {
                expiredKeys.add(current.key);
            }
            current = current.next;
        }
    } finally {
        lock.readLock().unlock();
    }
    
    // Step 2: Remove expired entries (need write-lock)
    if (!expiredKeys.isEmpty()) {
        lock.writeLock().lock();
        try {
            for (String key : expiredKeys) {
                Node node = HM.get(key);
                if (node != null && isExpired(node)) {
                    removeNode(node);
                    HM.remove(key);
                    stats.recordExpiredRemoval();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}, 5, 5, TimeUnit.MINUTES);  // Run every 5 minutes
```

**Currently**: ❌ Missing design, doesn't address lock contention

---

##### 6. **Variable Value Sizes Edge Case**

You assumed "1KB per value" but:

```java
// MISSING: How to handle variable sizes?
// Example: maxCapacity = 10GB
// Current: 5M entries × 2KB each = 10GB (full)
// New entry: 100MB value
// How many entries to evict?

// Should define behavior:
public put(key, largeValue) {
    lock.writeLock().lock();
    try {
        long requiredSpace = estimateSize(largeValue);
        
        // Option A: Evict ONE LRU at a time (might need many iterations)
        // Option B: Evict until freed space >= new entry size
        
        while (currentMemoryUsage + requiredSpace > maxCapacity) {
            evictLRU();
            currentMemoryUsage -= lastEvictedSize;
        }
        
        // Option C: Reject if larger than maxCapacity
        if (requiredSpace > maxCapacity) {
            throw new CacheException("Value too large");
        }
        
        addNode(key, largeValue);
        currentMemoryUsage += requiredSpace;
    } finally {
        lock.writeLock().unlock();
    }
}
```

**Currently**: ⚠️ Not addressed, assumes fixed size

---

##### 7. **Monitoring & Observability Missing**

MAANG expects discussion of:
```
- Cache hit rate trending (% over time)
- Eviction rates (entries/sec being evicted)
- Lock contention signals (write-lock wait times)
- GC impact (stop-the-world pauses with 10M objects)
- Memory usage patterns (growth over time)
- TTL distribution (how many entries expire vs. being accessed?)
- Cache size vs. maxCapacity utilization
```

**Currently**: ❌ Not mentioned

---

##### 8. **Failure Recovery Not Addressed**

Missing scenarios:
```
- What if cleanup is slow and falls behind?
  → Cache fills with expired entries
  → Performance degradation
  
- What if value > maxCapacity?
  → Error? Reject? Store partially?
  
- What if HM and DLL get out of sync?
  → Corruption detection?
  → Recovery mechanism?
  
- Application crash?
  → Cache lost (OK, it's in-process)
  → But: How to warm cache on restart?
  
- Memory exhaustion (OOM)?
  → Fail gracefully or aggressive eviction?
```

**Currently**: ❌ Not addressed

---

##### 9. **No Trade-Off Discussion**

Missing analysis:
```
- Lazy TTL vs. Batch cleanup trade-offs
  → Lazy: CPU overhead on every Get, but clean immediately
  → Batch: Stale entries in cache, but less CPU
  
- ConcurrentHashMap vs. ReentrantReadWriteLock
  → ConcurrentHashMap: Fine-grained locking, complex
  → ReentrantReadWriteLock: Coarse-grained, simpler, contention?
  
- Read-lock for Get vs. Write-lock for Put
  → Read-lock allows parallelism
  → But still mutual exclusion with Put
  
- Memory efficiency vs. CPU overhead
  → Tracking TTL costs CPU
  → But saves memory with cleanup
```

**Currently**: ❌ Not discussed

---

##### 10. **No Alternatives Discussion**

MAANG expects:
```
- Why HashMap + DLL vs. TreeMap + LinkedHashMap?
  → HashMap: O(1) vs. TreeMap: O(log n)
  → DLL: Explicit control vs. LinkedHashMap: Built-in order
  
- Why ReentrantReadWriteLock vs. ConcurrentHashMap alone?
  → ConcurrentHashMap: Only protects HashMap, not DLL
  → ReentrantReadWriteLock: Protects entire operation
  
- Why in-process vs. Redis?
  → In-process: Fast (local memory), no network
  → Redis: Distributed, high availability, persistence
  → Use case dependent
  
- Why no persistence?
  → Cache is ephemeral, OK to lose on restart
  → If persistence needed, use Redis or custom RDB
```

**Currently**: ❌ Not addressed

---

### 📊 Summary: What's Missing in R3

| Component | Status | Impact | MAANG Expectation |
|-----------|--------|--------|-------------------|
| **Data structures** | ✅ | - | ✅ Correct |
| **O(1) complexity** | ✅ | - | ✅ Demonstrated |
| **LRU eviction** | ✅ | - | ✅ Implemented |
| **TTL strategies** | ⚠️ | Mentioned only | ✅ Need pseudo-code |
| **Clear operation** | ⚠️ | Implied | ⚠️ Need details |
| **Cache stats** | ⚠️ | Mentioned only | ⚠️ Need implementation |
| **Thread-safety** | ✅ | - | ✅ ReentrantReadWriteLock |
| **API contracts** | ✅ | - | ✅ Defined |
| **Edge cases** | ✅ | - | ✅ Handled |
| **Batch cleanup** | ❌ | Critical gap | ❌ Missing design |
| **Variable sizes** | ❌ | Edge case | ⚠️ Not addressed |
| **Monitoring** | ❌ | Production gap | ❌ Missing |
| **Failure recovery** | ❌ | Production gap | ❌ Missing |
| **Trade-offs** | ❌ | Interview signal | ❌ Missing |
| **Alternatives** | ❌ | Interview signal | ❌ Missing |

---

## 📈 MAANG Readiness Score by Revision

### **Revision #1: 5-10/100** 🔴 FAIL

**What It Has**:
- ✅ Thinking about distributed aspects
- ✅ Storage calculation

**What It Lacks**:
- ❌ Wrong approach (Postgres for in-process)
- ❌ Not truly in-process
- ❌ O(1) claims false (O(log n) with network latency)
- ❌ No implementation details
- ❌ No thread-safety
- ❌ No TTL mechanism
- ❌ Confusion between in-process and distributed

**Interviewer Feedback**:
> "This is fundamentally wrong. Postgres is not an in-process cache. The whole architecture is misaligned. Let's restart with a correct approach."

**MAANG Verdict**: ❌ **REJECTED** — Start over, wrong problem solved

---

### **Revision #2: 40-50/100** 🟡 INCOMPLETE

**What It Has**:
- ✅ Correct data structures (HashMap + DLL)
- ✅ O(1) complexity achieved and explained
- ✅ LRU eviction logic sound
- ✅ Step-by-step operations clear
- ⚠️ Mentions ConcurrentHashMap (partial thread-safety)

**What It Lacks**:
- ❌ Thread-safety incomplete (ConcurrentHashMap ≠ DLL safety)
- ❌ No TTL implementation
- ❌ No Clear operation design
- ❌ No Cache statistics tracking
- ❌ No API contracts defined
- ❌ No edge case handling
- ❌ No config/setup strategy
- ❌ No trade-off discussion

**Interview Experience**:
- Get through initial round
- Fail when asked deeper questions
- "How do you handle TTL?" → Blank stare
- "How do clients use this?" → No API defined
- "What about concurrent access?" → "ConcurrentHashMap" (incomplete)

**MAANG Verdict**: ⚠️ **CONDITIONAL PASS** — Address missing pieces quickly or fail follow-up

---

### **Revision #3: 75-85/100** 🟢 STRONG, Interview-Ready

**What It Has**:
- ✅ Correct data structures + O(1) complexity
- ✅ Thread-safety defined (ReentrantReadWriteLock)
- ✅ TTL strategies outlined (5 different types)
- ✅ API contracts detailed (GET/POST with error handling)
- ✅ Config strategy defined (app config, per-data-type TTLs)
- ✅ Edge cases handled (size=1, empty cache, concurrent access)
- ✅ Clear operation addressed
- ✅ Cache statistics mentioned
- ✅ Smart assumptions (no sync, session stickiness)
- ✅ Capacity bounds quantified (10M entries, 32GB RAM)
- ✅ Memory efficiency strategy (bounded cache with eviction)

**What It Lacks**:
- ⚠️ No pseudo-code for TTL cleanup
- ⚠️ No batch cleanup job concurrency design
- ⚠️ No statistics implementation details
- ⚠️ No variable value size handling
- ⚠️ No monitoring/observability strategy
- ⚠️ No failure recovery discussion
- ⚠️ No trade-off analysis
- ⚠️ No alternatives comparison

**Interview Experience**:
- Pass phone screen with flying colors
- Interviewer is impressed with depth
- "Let me dig into implementation. How does TTL cleanup work exactly?"
- Ready to handle follow-up questions if prepared
- Likely progresses to on-site

**MAANG Verdict**: ✅ **STRONG PASS** — Design is solid, interview-ready, ready for on-site

---

### **Revision #3 + Implementation Details: 90-95/100** 🟢 EXCELLENT

To reach this level, add:

```
1. Pseudo-code for:
   ✅ Lazy TTL check logic
   ✅ Batch cleanup job (scheduler + locking)
   ✅ Clear operation implementation
   ✅ Statistics tracking

2. Edge case handling:
   ✅ Cleanup falling behind solution
   ✅ Value > maxCapacity rejection
   ✅ HM/DLL sync detection

3. Design discussions:
   ✅ Why ReentrantReadWriteLock vs. ConcurrentHashMap
   ✅ Lazy vs. batch TTL trade-offs
   ✅ Why this vs. Redis
   ✅ Scalability limits

4. Monitoring:
   ✅ Hit rate tracking
   ✅ Eviction metrics
   ✅ Lock contention signals

5. Failure scenarios:
   ✅ Crash recovery
   ✅ Memory exhaustion
   ✅ Corruption detection
```

**MAANG Verdict**: ✅ **EXCELLENT PASS** — Get offer, strong technical depth

---

## 🎤 What to Say in MAANG Interview

### Opening Statement (Start with R3)

> "I'll design a local, in-process LRU cache using HashMap for O(1) lookups and DoublyLinkedList for O(1) eviction. It will be thread-safe with ReentrantReadWriteLock and support configurable TTL with multiple strategies. Each microservice instance maintains its own cache—they don't sync. To maximize hit rate, we use session stickiness so users hit the same instance repeatedly."

### When Asked "How Does TTL Work?"

> "I use a hybrid approach: lazy deletion on every Get plus a background batch job every 5 minutes. On Get, I check if the entry is expired (now - createdAt > TTL). If expired, I remove it and return cache miss. For cleanup, I run a separate scheduled task that scans the DLL under read-lock to identify expired entries, then upgrades to write-lock to remove them. This prevents the cache from filling with stale data."

### When Asked "What About Thread-Safety?"

> "I use ReentrantReadWriteLock, not just ConcurrentHashMap. ConcurrentHashMap protects the HashMap bucket, but the DLL node movements (updating prev/next pointers) aren't atomic. Multiple threads moving nodes simultaneously would corrupt pointers. ReentrantReadWriteLock allows multiple readers (Get operations) but exclusive access for writers (Put operations). This minimizes contention under read-heavy workloads."

### When Asked "What Are the Trade-Offs?"

> "Lazy TTL has CPU overhead on every Get but cleans immediately. Batch cleanup reduces CPU but allows stale entries briefly. I chose hybrid because it balances both. For thread-safety, fine-grained locking (ConcurrentHashMap per bucket) would be faster under extreme contention, but ReentrantReadWriteLock is simpler and sufficient for typical workloads."

### When Asked "Why Not Redis?"

> "Redis is great for distributed scenarios and high availability, but it adds network latency (5-20ms per call). For in-process cache, local memory is 100x faster. We accept eventual inconsistency between instances—session stickiness ensures users see consistent data within their session."

### When Asked "What About Monitoring?"

> "I track hit rate (hits / (hits + misses)), eviction rate (evictions/sec), and lock wait times. If hit rate drops, we might increase capacity. If lock contention spikes, we'd profile to see if a different locking strategy helps."

---

## Final Verdict

| Revision | Score | Status | Action |
|----------|-------|--------|--------|
| **#1** | 5-10 | ❌ REJECTED | Restart with correct approach |
| **#2** | 40-50 | ⚠️ INCOMPLETE | Needs major work |
| **#3** | 75-85 | ✅ READY | Interview-prepared |
| **#3 + Details** | 90-95 | ✅ EXCELLENT | Strong offer signal |

---

**Bottom Line**: Your R3 is **INTERVIEW-READY for MAANG phone screen**. You'll likely pass and move to on-site. To guarantee offer, add pseudo-code for batch cleanup, monitoring strategy, and trade-off discussions. You're **80% there**.

---

---

## LRU Cache - Revision #3 (With HEAD & TAIL Sentinel Pointers)

### **Data Structures**

```
1. HashMap<String, Node> - O(1) key lookup
   Example: {"A": Node(A), "B": Node(B), "C": Node(C)}

2. DoublyLinkedList with HEAD and TAIL sentinel nodes
   - HEAD: dummy node pointing to oldest (LRU) node
   - TAIL: dummy node pointing to newest (MRU) node
   
   Structure: HEAD → [oldest] ↔ ... ↔ [newest] → TAIL
```

---

### **Initial State: Cache Capacity = 3**

```
DLL Structure:
  HEAD → A ↔ B ↔ C → TAIL
  
  HEAD points to A (A is least recently used)
  TAIL points to C (C is most recently used)

HashMap:
  {"A": Node(A), "B": Node(B), "C": Node(C)}
```

---

### **Get Operation: Get("B")**

```
BEFORE:
  DLL: HEAD → A ↔ B ↔ C → TAIL
  HEAD pointer = A
  TAIL pointer = C

PROCESS:
  i)   Check in HashMap: "B" exists ✓
  ii)  Fetch Node B value from DLL
  iii) Remove B from middle position
       DLL: HEAD → A ↔ C → TAIL
  iv)  Move B to end (before TAIL)
       DLL: HEAD → A ↔ C ↔ B → TAIL
  v)   Update HEAD and TAIL pointers
       HEAD pointer = A (unchanged)
       TAIL pointer = B (changed)

AFTER:
  DLL: HEAD → A ↔ C ↔ B → TAIL
  A is still LRU (pointed by HEAD)
  B is now MRU (pointed by TAIL)
```

---

### **Put Operation - Case (a): Update Existing Key**

```
BEFORE:
  DLL: HEAD → A ↔ C ↔ B → TAIL
  Node B: value = B1

Put("B", B2):
  i)   Check HashMap: "B" exists ✓
  ii)  Update node value in DLL: B1 → B2
  iii) Remove B from current position
       DLL: HEAD → A ↔ C → TAIL
  iv)  Move B to end
       DLL: HEAD → A ↔ C ↔ B → TAIL
  v)   Update HEAD and TAIL pointers
       HEAD pointer = A (unchanged)
       TAIL pointer = B (unchanged, already at tail)

AFTER:
  DLL: HEAD → A ↔ C ↔ B → TAIL
  Node B: value = B2 (updated)
```

---

### **Put Operation - Case (b): Add New Key (Cache Full)**

```
BEFORE:
  DLL: HEAD → A ↔ C ↔ B → TAIL  (FULL 3/3)
  Cache size = 3

Put("D", 40):
  
  EVICTION:
  i)   Check if cache full: YES
  ii)  Get node after HEAD: A (least recently used)
       A is the first node after HEAD sentinel
  iii) Remove A from DLL
       DLL: HEAD → C ↔ B → TAIL
  iv)  Remove from HashMap: cache.remove("A")
       HashMap: {"B": ..., "C": ..., "D": ...}
  
  ADD NEW NODE:
  v)   Create new Node D with value 40
  vi)  Add D at end (before TAIL)
       DLL: HEAD → C ↔ B ↔ D → TAIL
  vii) Add to HashMap: cache.put("D", Node(D))
  viii) Update HEAD and TAIL pointers
        HEAD pointer = C (no change, still points to oldest)
        TAIL pointer = D (changed)

AFTER:
  DLL: HEAD → C ↔ B ↔ D → TAIL
  HashMap: {"B": Node(B), "C": Node(C), "D": Node(D)}
  Cache size = 3/3
  HEAD.next = C (LRU, will be evicted next)
  TAIL.prev = D (MRU, most recently added)
```

---

### **Why HEAD and TAIL Pointers Matter**

```
Advantage 1: O(1) Eviction
  - Evict node = HEAD.next (always first real node)
  - No need to search
  - No need for min/max tracking

Advantage 2: O(1) Access Update
  - Move to MRU = move before TAIL
  - No need to know current position
  - Just update prev/next pointers

Advantage 3: No Null Checks
  - HEAD always exists
  - TAIL always exists
  - No "if (head == null)" checks needed

Advantage 4: Clear Boundaries
  - HEAD marks oldest boundary
  - TAIL marks newest boundary
  - Easy to understand and debug
```

---

### **Node Class Structure**

```java
class Node {
    String key;
    int value;
    Node prev;      // Link to previous node
    Node next;      // Link to next node
}

// Sentinel nodes (can have dummy key/value)
Node HEAD = new Node("HEAD", -1);   // Dummy
Node TAIL = new Node("TAIL", -1);   // Dummy

// Initialize empty DLL
HEAD.next = TAIL;
TAIL.prev = HEAD;
// Now: HEAD ↔ TAIL (empty list, ready to add nodes)
```

---

### **Critical Operations with HEAD/TAIL**

#### **Eviction (O(1))**

```java
private void evict() {
    Node lru = HEAD.next;  // ← Always LRU node after HEAD
    
    // Remove from DLL
    lru.prev.next = lru.next;
    lru.next.prev = lru.prev;
    
    // Remove from HashMap
    cache.remove(lru.key);
}
```

#### **Move to End (O(1))**

```java
private void moveToTail(Node node) {
    // Remove from current position
    node.prev.next = node.next;
    node.next.prev = node.prev;
    
    // Add before TAIL (at end)
    node.prev = TAIL.prev;
    node.next = TAIL;
    TAIL.prev.next = node;
    TAIL.prev = node;
}
```

#### **Get Operation**

```java
public int get(String key) {
    lock.readLock().lock();
    try {
        if (!cache.containsKey(key)) {
            return -1;  // not found
        }
        
        Node node = cache.get(key);
        
        // Upgrade to write lock for moving
        lock.readLock().unlock();
        lock.writeLock().lock();
        try {
            moveToTail(node);  // Move to most recent
            return node.value;
        } finally {
            lock.writeLock().unlock();
        }
    } finally {
        if (lock.readLock().tryLock()) {
            lock.readLock().unlock();
        }
    }
}
```

---

### **Example: Step-by-Step with HEAD/TAIL**

```
Step 1: Put("A", 10)
  DLL: HEAD ↔ A ↔ TAIL
  HEAD.next = A (LRU)
  TAIL.prev = A (MRU)

Step 2: Put("B", 20)
  DLL: HEAD ↔ A ↔ B ↔ TAIL
  HEAD.next = A (LRU)
  TAIL.prev = B (MRU)

Step 3: Get("A")
  Remove A from middle: HEAD ↔ B ↔ TAIL
  Move A to end: HEAD ↔ B ↔ A ↔ TAIL
  HEAD.next = B (LRU)
  TAIL.prev = A (MRU)

Step 4: Put("C", 30) [Cache Full, capacity=3]
  Evict HEAD.next = B
  DLL: HEAD ↔ A ↔ TAIL
  Add C: HEAD ↔ A ↔ C ↔ TAIL
  HEAD.next = A (LRU)
  TAIL.prev = C (MRU)
```

---

### **Summary: HEAD and TAIL Benefits**

| Feature | Without Sentinels | With HEAD/TAIL |
|---------|-------------------|----------------|
| **Eviction** | Search for oldest | HEAD.next (O(1)) |
| **Move to Recent** | Track current pos | Before TAIL (O(1)) |
| **Empty check** | if (head == null) | Never null |
| **Code clarity** | Confusing boundaries | Clear (HEAD/TAIL) |
| **Edge cases** | Many null checks | Fewer checks |

---

### **Honest Feedback on LRU R3**

#### ✅ What's Correct

1. ✅ **HEAD and TAIL sentinels** — Simplifies implementation
2. ✅ **O(1) eviction** — Always evict HEAD.next
3. ✅ **O(1) access** — Move to before TAIL
4. ✅ **Clear boundaries** — No null confusion
5. ✅ **All NFRs addressed** — Thread-safety, TTL, stats, capacity

#### 🔴 Still Missing (for 90%+ score)

1. ⚠️ Lock upgrade logic (read→write) not detailed
2. ⚠️ Pseudo-code for moveToTail not provided
3. ⚠️ Edge case: empty DLL handling
4. ⚠️ Monitoring strategy incomplete
5. ⚠️ Failure recovery (corruption detection) missing

---

### **Final Verdict on LRU R3**

```
Score: 80-85/100

✅ READY FOR INTERVIEW
   - Correct data structures with sentinels
   - O(1) operations clearly explained
   - Thread-safety addressed
   - TTL strategies outlined
   - Edge cases handled

🎯 TO REACH 90%+
   - Add pseudo-code for moveToTail
   - Detail lock upgrade strategy
   - Add monitoring metrics
   - Discuss failure scenarios

Status: STRONG PASS for phone screen ✓
```
