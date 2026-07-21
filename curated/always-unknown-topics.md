# Always Unknown Topics

Topics that frequently come up in interviews but are often unclear or misunderstood.

---

## Table of Contents

1. [Sharding vs Read Replicas](#sharding-vs-read-replicas)
2. [Read Replicas in NoSQL](#read-replicas-in-nosql)
3. [Caching Strategies](#caching-strategies)
4. [Cache Eviction Strategies](#cache-eviction-strategies)
5. [301 vs 302 Redirects for Data Analytics](#301-vs-302-redirects-for-data-analytics)
6. [CDN Refresh Strategies](#cdn-refresh-strategies)
7. [Time & Space Complexity Analysis](#time--space-complexity-analysis)
8. [Concurrency vs Multithreading](#concurrency-vs-multithreading---core-concept)
9. [Hashtable vs HashMap](#hashtable-vs-hashmap)
10. [ReentrantReadWriteLock](#reentrantreadwritelock)
11. [TTL (Time To Live) Strategies](#ttl-time-to-live-strategies)
12. [TTL vs Cache Eviction](#ttl-vs-cache-eviction)
13. [Item-Level vs Cache-Level TTL](#item-level-vs-cache-level-ttl)
14. [Cache Capacity Boundary Logic](#cache-capacity-boundary-logic)
15. [Calculate Memory Consumed by HashMap](#calculate-memory-consumed-by-hashmap)
16. [Hashing Concept](#hashing-concept)
17. [Consistent Hashing](#consistent-hashing)

---

## Sharding vs Read Replicas

### Sharding

**Sharding** splits your data horizontally across multiple database instances based on a **shard key** (like user ID or region). Each shard holds a different subset of the data.

- **Example:** User IDs 1-1M on Shard A, 1M-2M on Shard B, etc.
- **Benefit:** Scales write capacity and storage — you can handle more data and writes
- **Cost:** Complex routing logic, harder to query across shards, rebalancing is painful

### Read Replicas

**Read Replicas** are exact copies of your entire database that sync from a primary instance. They're for reading only.

- **Example:** Primary DB in US, read replica in EU, another in Asia
- **Benefit:** Distributes read load geographically; low latency for readers in different regions
- **Cost:** Uses more storage (full copy per replica), doesn't help with write scaling

### Key Difference

| Aspect | Sharding | Read Replicas |
|--------|----------|---------------|
| **Data split** | Yes (partial) | No (full copy) |
| **Write scaling** | ✅ Yes | ❌ No (writes still go to primary) |
| **Read scaling** | ✅ Yes | ✅ Yes |
| **Complexity** | High | Low |
| **Query any data** | ❌ Hard (need shard key) | ✅ Easy |

**Real-world rule:** Use **read replicas** for read-heavy workloads across geographies. Use **sharding** when your data is too big to fit on one machine or writes are bottlenecking you.

## Read Replicas in NoSQL

Yes, **read replicas work with NoSQL databases** too. In fact, many NoSQL systems use replication as a core feature.

### How NoSQL Read Replicas Work

**NoSQL databases typically have built-in replication** (unlike traditional SQL where replicas are often a separate feature).

- **MongoDB:** Primary + Secondary replicas (replica sets)
- **Cassandra:** Every node is a replica; data replicates across the cluster
- **DynamoDB:** Automatically creates read replicas across availability zones
- **Redis:** Master → Slave replicas for read scaling

### Key Difference: NoSQL vs SQL Read Replicas

| Aspect | SQL Read Replicas | NoSQL Read Replicas |
|--------|-------------------|-------------------|
| **Setup** | Manual (add replicas) | Often built-in, automatic |
| **Consistency** | Usually eventual | Eventual (configurable) |
| **Failover** | Manual or semi-auto | Often automatic (Cassandra, MongoDB) |
| **Write conflicts** | Single primary | Can have multi-write (Cassandra) |

### Replication Styles in NoSQL

1. **Leader-Follower** (MongoDB)
   - One primary (writes), multiple secondaries (reads)
   - Same as SQL replicas

2. **Leaderless** (Cassandra, DynamoDB)
   - Every node accepts reads AND writes
   - Data spreads across all nodes
   - More resilient but eventual consistency

### When to Use NoSQL Read Replicas

- High read volume + low write volume
- Geographically distributed users (replicas in different regions)
- High availability needs

**Bottom line:** NoSQL databases are actually *better* at replication than SQL — it's baked into their DNA, not bolted on.

## Caching Strategies

Caching strategies determine *how* and *when* to update cached data when the underlying data changes.

### Cache Update Strategies

#### 1. Cache-Aside (Lazy Loading)

Application checks cache first. If miss → fetch from DB and update cache.

```
Request → Check Cache → Hit? Return
                      → Miss? Fetch DB → Update Cache → Return
```

**Pros:** Simple, only caches what's used  
**Cons:** First request is slow (cache miss)  
**Use:** General purpose, web apps

#### 2. Write-Through

Write to cache AND database simultaneously before returning.

```
Write → Update Cache + Update DB (together) → Return
```

**Pros:** Cache always consistent with DB  
**Cons:** Slower writes, extra latency  
**Use:** Critical data (financial, medical)

#### 3. Write-Behind (Write-Back)

Write to cache first, then asynchronously write to DB.

```
Write → Update Cache → Return (immediately)
          → Async update DB (later)
```

**Pros:** Fast writes, high throughput  
**Cons:** Data loss risk if cache crashes before DB sync  
**Use:** Non-critical data, high-volume writes

#### 4. Refresh-Ahead

Proactively refresh cache before it expires.

```
Cache TTL expiring soon → Fetch fresh data → Update Cache
```

**Pros:** Avoids cache misses, always fresh data  
**Cons:** Wastes resources refreshing unused data  
**Use:** Predictable access patterns (leaderboards, recommendations)

### Update Strategies Comparison

| Strategy | Consistency | Write Speed | Read Speed | Best For |
|----------|-------------|-------------|-----------|----------|
| **Cache-Aside** | Eventually | Fast | Slow (miss) | Web apps |
| **Write-Through** | Strong | Slow | Fast | Critical data |
| **Write-Behind** | Very Fast | Very Fast | Fast | High throughput |
| **Refresh-Ahead** | Strong | Fast | Very Fast | Predictable reads |

---

## Cache Eviction Strategies

Eviction strategies determine *which* items to remove when cache is full.

### 1. LRU (Least Recently Used)

Remove the item that hasn't been used for the longest time.

```
Access order: A → B → C → A → B
Cache full, need to evict → Remove C (least recently used)
```

**Pros:** Simple, works well for most cases  
**Cons:** Doesn't consider how *frequently* items are used  
**Use:** Most common choice (Redis default, browser cache)

### 2. LFU (Least Frequently Used)

Remove the item that's been accessed the fewest times.

```
Access counts: A(10 times) → B(5 times) → C(2 times)
Cache full → Remove C (least frequent)
```

**Pros:** Favors "hot" data  
**Cons:** Overhead to track access counts  
**Use:** When you care about frequency over recency

### 3. FIFO (First In, First Out)

Remove the oldest item added to cache, regardless of access.

```
Order added: A → B → C → D
Cache full → Remove A (first added)
```

**Pros:** Very simple  
**Cons:** Ignores usage patterns completely  
**Use:** Simple queues, rarely used now

### 4. LRU-K

Remove based on the K-th most recent access (hybrid of LRU + frequency).

```
Example (K=2): Track last 2 accesses per item
Item A: [5 min ago, 10 min ago]
Item B: [2 min ago, 50 min ago]
Remove item with oldest K-th access
```

**Pros:** Better than LRU, adapts to patterns  
**Cons:** More complex  
**Use:** Database buffer pools (MySQL)

### 5. TTL (Time To Live)

Items expire after a fixed time, regardless of usage.

```
Item A added at 10:00 → TTL=5min → Expires at 10:05 (auto-remove)
```

**Pros:** Simple, predictable expiration  
**Cons:** Doesn't adapt to actual usage  
**Use:** Session data, short-lived data

### Eviction Strategies Comparison

| Strategy | Best For | Overhead | Popular |
|----------|----------|----------|---------|
| **LRU** | General purpose | Low | ⭐⭐⭐ |
| **LFU** | Hot/cold data separation | Medium | ⭐⭐ |
| **FIFO** | Simple cases | Very Low | ⭐ |
| **LRU-K** | Database caching | Medium-High | ⭐⭐ |
| **TTL** | Time-sensitive data | Low | ⭐⭐⭐ |

### Real-World Examples

- **Redis:** LRU (default), can switch to LFU or random
- **Browser cache:** LRU + TTL
- **MySQL buffer pool:** LRU-K
- **CDN caches:** TTL + LRU hybrid
- **Memcached:** LRU

**Golden rule:** **LRU is the safest default.** Use LFU if you have clearly "hot" data, TTL for time-sensitive data.

## 301 vs 302 Redirects for Data Analytics

### The Key Difference

**301 (Permanent):** Tells search engines "this URL moved forever" → they update records and point directly to new URL

**302 (Temporary):** Tells search engines "this URL moved temporarily" → they keep original URL in their records

### Why 302 is Better for Analytics

#### 1. Preserve Analytics History

With **301:**
```
Old URL: /products → gets removed from analytics
         → All traffic now shows under new URL
         → Lost visibility into old URL's performance
```

With **302:**
```
Old URL: /products → stays in analytics
         → Shows: "This redirected to /products-v2"
         → You see complete redirect path + traffic flow
```

#### 2. Track User Journey

**302 lets you see:**
- How much traffic uses the old URL (gives insight into user bookmarks, links)
- Conversion paths through the redirect
- Which traffic sources link to old URL
- User behavior across redirect

**301 hides this** because search engines stop indexing the old URL.

#### 3. Easy A/B Testing & Rollback

```
302 Redirect: Test new URL while keeping original
             → If new URL performs worse, switch back
             → Original URL's analytics intact

301 Redirect: Hard to rollback
             → Search engines already updated
             → Analytics data scattered
```

#### 4. Keep SEO Credit Separate

With **301:**
```
Old URL (100K backlinks) → All credit flows to new URL
                         → Hard to measure which URL performs better
```

With **302:**
```
Old URL (100K backlinks) → Keeps separate in search results
                         → Can compare performance of both URLs
                         → More data = better insights
```

### When to Use What

| Situation | Use | Why |
|-----------|-----|-----|
| **Temporary campaign/test** | 302 | Track separately, easy rollback |
| **URL migration (keep testing)** | 302 | See redirect funnel |
| **Permanent domain change** | 301 | SEO consolidation |
| **Site restructure (final)** | 301 | Clean slate, new URL is permanent |

### Real-World Example

```
Scenario: Moving /old-landing → /new-landing

With 302:
Analytics show:
├── /old-landing: 5K visitors → redirected
└── /new-landing: 5K visitors (via redirect)
You see the complete funnel!

With 301:
Analytics show:
└── /new-landing: 5K visitors
(Where did they come from? Lost!)
```

**Golden rule:** Use 302 by default for anything that's not permanent. You get better analytics + flexibility. Switch to 301 only when you're 100% sure the old URL is gone forever.

## CDN Refresh Strategies

### 1. TTL (Time To Live) - Automatic Expiration

Most common method - CDN automatically refreshes after expiration.

```
File: image.jpg uploaded to CDN

Response header:
Cache-Control: max-age=3600
(= Keep in cache for 1 hour)

Timeline:
├─ 10:00 AM → User downloads image.jpg
├─ 10:15 AM → Another user → Served from CDN cache (fast!)
├─ 11:00 AM → TTL expires
├─ 11:05 AM → User requests image.jpg
├─ → CDN checks origin: "Is this updated?"
└─ → If yes, fetch new version + update cache
```

**TTL Header Examples:**

```
Cache-Control: max-age=86400
(24 hours - good for static files like logos)

Cache-Control: max-age=3600
(1 hour - good for changing content)

Cache-Control: max-age=0
(Immediately stale - always check origin)

Cache-Control: no-cache
(Always validate before serving)
```

### 2. Cache Busting - Version in Filename

Force CDN to get latest without waiting for TTL.

```
Old: image.jpg
New: image_v2.jpg

Or with hash:
image.abc123def.jpg (hash changes when file changes)
```

**How it works:**

```
File updates:
├─ image.jpg (v1) → TTL = 30 days
├─ You update image.jpg
├─ But CDN still serves old version (TTL not expired!)

Solution:
├─ Rename to image_v2.jpg
├─ Update HTML: <img src="image_v2.jpg">
├─ CDN never cached image_v2.jpg
├─ Fetches fresh copy from origin
└─ New users get latest immediately
```

**Real example:**

```html
<!-- Old -->
<script src="app.js"></script>

<!-- New - forces fresh download -->
<script src="app.a1b2c3d4.js"></script>
(hash changes when app.js changes)
```

### 3. Cache Purge/Invalidation - Manual Refresh

Tell CDN to remove file immediately.

```
You update logo.png on origin

Command (CloudFlare example):
curl -X POST "https://api.cloudflare.com/client/v4/zones/{zone_id}/purge_cache" \
  -d '{"files": ["https://example.com/logo.png"]}'

Result:
├─ All CDN edge locations remove logo.png
├─ Next user request → fetches fresh from origin
└─ No waiting for TTL
```

**Purge types:**

```
Single file purge:
├─ Purge: logo.png
└─ Keep: other files cached

Path purge:
├─ Purge: /images/*
└─ Remove all images

Full purge:
├─ Purge everything
└─ Slowest option, last resort
```

### 4. Stale-While-Revalidate

Serve old content while checking for new version in background.

```
Response header:
Cache-Control: max-age=3600, stale-while-revalidate=86400

Timeline:
├─ 10:00 AM → User gets image.jpg (TTL=1 hour)
├─ 11:05 AM → TTL expired
├─ 11:06 AM → User requests image.jpg
├─ CDN: "Send old version immediately"
├─ CDN (background): "Check origin for update"
├─ If update exists → refresh cache for next user
└─ User gets fast response, cache eventually updates

Benefits:
✅ User gets instant response
✅ Cache still gets updated
✅ No need for purge commands
```

### 5. Conditional Requests (E-Tags)

Smart refresh without transferring full file.

```
First request:
GET image.jpg
Response:
  ETag: "abc123"
  (hash of file content)

Later request:
GET image.jpg
If-None-Match: "abc123"
(Has same ETag?)

Response:
├─ If same → 304 Not Modified (use cached)
├─ If different → 200 + new content
└─ Saves bandwidth!
```

### Comparison: When to Use What

| Strategy | TTL | Cache Bust | Purge | Stale-While | ETag |
|----------|-----|-----------|-------|-------------|------|
| **Static files (logo)** | 30 days | ✅ | ❌ | ✅ | ✅ |
| **Dynamic content** | 1 hour | ✅ | ✅ | ✅ | ✅ |
| **Critical update** | ❌ | ✅ | ✅ | ❌ | ✅ |
| **API responses** | 5 min | ✅ | ✅ | ❌ | ✅ |
| **Implementation effort** | None | Code change | API call | Code | Auto |

### Real-World Scenario

**Website deploy with new CSS:**

```
Old CSS: style.css (cached 30 days)

You deploy new CSS:

Option 1 (Cache Bust - Recommended):
├─ Rename: style.abc123.css
├─ Update HTML: <link href="style.abc123.css">
├─ Deploy
├─ Users get latest immediately
└─ No API calls needed

Option 2 (Purge):
├─ Deploy new style.css to origin
├─ Call CDN API: purge style.css
├─ Wait 1-5 sec for all edges to purge
├─ Users get latest in next request
└─ Requires monitoring

Option 3 (TTL only):
├─ Deploy new style.css
├─ Wait 30 days for cache to expire 😱
└─ Not recommended for production!
```

### Best Practice Strategy

```
For static files (images, CSS, JS):
✅ Use cache busting + long TTL (30 days)

For changing content:
✅ Use short TTL (1 hour) + stale-while-revalidate

For critical updates:
✅ Use cache purge (API call)

For everything:
✅ Always use ETag for smart revalidation
```

**Golden rule:** Use cache busting for static assets. It's the fastest and most reliable way to ensure users get updates without waiting for TTL expiration.

## Time & Space Complexity Analysis

### What is Complexity?

**Complexity = How much time/memory does code use as input size (n) grows?**

### Time Complexity - How to Calculate

Count operations as input grows.

#### Example 1: Single Loop

```python
def print_all(arr):
    for i in arr:           # runs n times
        print(i)            # 1 operation
```

**Analysis:** n iterations × 1 operation = **O(n)**

#### Example 2: Nested Loops

```python
def print_pairs(arr):
    for i in arr:           # n times
        for j in arr:       # n times per i
            print(i, j)
```

**Analysis:** n × n = n² → **O(n²)**

#### Example 3: Sequential Operations

```python
def process(arr):
    for i in arr:           # n operations
        print(i)
    
    for j in arr:           # n operations
        print(j)
```

**Analysis:** n + n = 2n → Drop constant → **O(n)**

#### Example 4: Binary Search

```python
def binary_search(arr, target):
    left, right = 0, len(arr) - 1
    while left <= right:            # halves each time
        mid = (left + right) // 2
        if arr[mid] == target:
            return mid
        elif arr[mid] < target:
            left = mid + 1
        else:
            right = mid - 1
```

**Analysis:** Each iteration halves search space
- 1M → 500K → 250K → ... → 1 (about 20 steps)
- **O(log n)**

### Space Complexity - How to Calculate

Count EXTRA memory created (not including input).

#### Example 1: No Extra Space

```python
def sum_array(arr):
    total = 0           # 1 variable
    for num in arr:
        total += num
    return total
```

**Analysis:** Only 1 variable (constant) → **O(1)**

#### Example 2: New Array of Size n

```python
def double_array(arr):
    result = []         # NEW array of size n
    for num in arr:
        result.append(num * 2)
    return result
```

**Analysis:** Creates array of n items → **O(n)**

#### Example 3: Constant Number of Arrays

```python
def func(n):
    arr1 = [0] * n      # n items
    arr2 = [0] * n      # n items
    arr3 = [0] * n      # n items
    # 3 arrays (constant, not scaling)
```

**Analysis:** 3 × n = 3n → Drop constant → **O(n)**

#### Example 4: Variable Number of Arrays (Scales with n)

```python
def create_matrix(n):
    arrays = []
    for i in range(n):              # n times (scales!)
        arr = [0] * n               # n items (scales!)
        arrays.append(arr)
```

**Analysis:** n arrays × n items each = n² → **O(n²)**

#### Example 5: Recursion (Call Stack)

```python
def factorial(n):
    if n <= 1:
        return 1
    return n * factorial(n - 1)     # recursive call
```

**Analysis:** Call stack depth = n → **O(n)**

### Key Insight: Drop Constants

Big O ignores constants - we only care about **growth rate**.

```
O(100n)  → O(n)     (100 is constant)
O(2n)    → O(n)     (2 is constant)
O(100)   → O(1)     (constant, not scaling)
O(100*n) → O(n)     (not O(n²), 100 doesn't scale)
O(n*n)   → O(n²)    (both n's scale with input)
O(5n²)   → O(n²)    (drop the 5)
```

### Big O Notation - Upper Bound

**Big O = Worst case scenario**

```
f(n) = O(g(n))
Meaning: f(n) grows no faster than g(n)

Example:
f(n) = 3n² + 5n + 100

Is it O(n²)?
3n² + 5n + 100 ≤ c × n² for large n? YES
→ Time Complexity: O(n²)
```

### Small O Notation - Strict Upper Bound

**Small O = Grows strictly slower**

```
f(n) = o(g(n))
Meaning: f(n) < c × g(n) for large n

Example:
f(n) = n, g(n) = n²
Is f(n) = o(n²)?
n < c × n² ? YES (strictly slower)

So: O(n) = o(n²) ✓
But: O(n) ≠ o(n) ✗
```

### Common Complexities (Best to Worst)

```
O(1)        - Constant       | array[0], dict lookup
O(log n)    - Logarithmic    | binary search
O(n)        - Linear         | single loop
O(n log n)  - Linearithmic   | merge sort, quick sort
O(n²)       - Quadratic      | nested loops
O(n³)       - Cubic          | triple nested loops
O(2ⁿ)       - Exponential    | recursive fibonacci
O(n!)       - Factorial      | generate permutations
```

### Quick Reference Table

| Code Pattern | Time | Space | Example |
|--------------|------|-------|---------|
| `for i in range(n): x = i` | O(n) | O(1) | Loop, no extra space |
| `for i in range(n): for j in range(n): ...` | O(n²) | O(1) | Nested loops |
| `arr = [0] * n` | O(n) | O(n) | Create new array |
| `binary_search(arr)` | O(log n) | O(1) | Halve search space |
| `def func(n): func(n-1)` | O(n) | O(n) | Recursion depth |
| `for i in range(n): arr = [0]*n` | O(n²) | O(n²) | n arrays of n items |
| `x, y = 0, 1; x+y` | O(1) | O(1) | Fixed operations |

### How to Analyze Code (Step-by-Step)

1. **Identify loops** → Count iterations (n, n-1, etc.)
2. **Identify recursion** → Count depth
3. **Identify nested operations** → Multiply them
4. **Drop constants & lower terms** → Keep dominant
5. **Express as Big O** → O(n), O(n²), etc.

**Golden Rule:** Only the FASTEST GROWING term matters.

```
f(n) = n³ + n² + n + 100
Dominant term: n³
Space Complexity: O(n³)
```

## Concurrency vs Multithreading - Core Concept

### Concurrency on Single Core

**1 Core, Multiple Tasks (Interleaving)**

```
                    CPU
        ┌─────────────────────┐
        │   SINGLE CORE       │
        └─────────────────────┘

Timeline (Interleaving):
┌─────────────────────────────────────────┐
│ Time: 0ms   1ms   2ms   3ms   4ms   5ms │
├─────────────────────────────────────────┤
│ Core: Task1 Task2 Task3 Task1 Task2 Task3│
│       ▰     ▰     ▰     ▰     ▰     ▰    │
└─────────────────────────────────────────┘

At any moment:
├─ Only 1 task running
├─ Others waiting
└─ Core switches between them (context switching)

Result: CONCURRENT but NOT PARALLEL
        (Looks concurrent, runs sequentially)
```

**Example: Node.js Event Loop**

```
                     CPU (1 Core)
        ┌────────────────────────┐
        │  ▰ Task A (running)    │
        │  ○ Task B (waiting)    │
        │  ○ Task C (waiting)    │
        │  ○ Task D (waiting)    │
        └────────────────────────┘
                  ↓ (Context switch every ms)
        ┌────────────────────────┐
        │  ○ Task A (waiting)    │
        │  ▰ Task B (running)    │
        │  ○ Task C (waiting)    │
        │  ○ Task D (waiting)    │
        └────────────────────────┘
```

---

### Multithreading on Multiple Cores

**4 Cores, 4 Threads (True Parallelism)**

```
                    CPU
        ┌─────────────────────────┐
        │ Core1 │ Core2 │ Core3 │ │
        ├───────┼───────┼───────┤ │
        │ Task1 │ Task2 │ Task3 │ │
        │  ▰    │  ▰    │  ▰    │ │
        └───────┴───────┴───────┘ │
            (All running at same time!)
        └─────────────────────────┘

Timeline (Parallel):
┌─────────────────────────────────────────┐
│ Time: 0ms  1ms  2ms  3ms  4ms  5ms      │
├─────────────────────────────────────────┤
│ C1:   ▰▰▰▰▰▰▰▰▰▰  (Task1)             │
│ C2:   ▰▰▰▰▰▰▰▰▰▰  (Task2)             │
│ C3:   ▰▰▰▰▰▰▰▰▰▰  (Task3)             │
│ C4:   ▰▰▰▰▰▰▰▰▰▰  (Task4)             │
└─────────────────────────────────────────┘

At any moment:
├─ All 4 tasks running (different cores)
├─ No waiting
└─ True parallelism

Result: CONCURRENT AND PARALLEL
        (Runs truly simultaneously)
```

**Example: Java Multithreading with 4 Cores**

```
                     CPU (4 Cores)
    ┌──────────┬──────────┬──────────┬──────────┐
    │  Core 1  │  Core 2  │  Core 3  │  Core 4  │
    ├──────────┼──────────┼──────────┼──────────┤
    │ ▰ Task1  │ ▰ Task2  │ ▰ Task3  │ ▰ Task4  │
    │ Running  │ Running  │ Running  │ Running  │
    └──────────┴──────────┴──────────┴──────────┘

All tasks execute simultaneously!
```

---

### Side-by-Side Comparison

```
CONCURRENCY (Single Core)          MULTITHREADING (Multi Core)

   CPU (1 Core)                        CPU (4 Cores)
┌─────────────────┐                ┌──────┬──────┬──────┬──────┐
│ ▰ Task A        │                │ ▰ T1 │ ▰ T2 │ ▰ T3 │ ▰ T4 │
│ ○ Task B        │                │ Run  │ Run  │ Run  │ Run  │
│ ○ Task C        │                └──────┴──────┴──────┴──────┘
│ ○ Task D        │
└─────────────────┘                All 4 tasks run at same time

Only 1 task at a time              Multiple tasks at same time
(Switching between them)           (True parallelism)
```

---

### Execution Timeline Comparison

**Concurrency: Interleaving on 1 Core**

```
Task A: ▰▰▯▯▯▯▯▯▯▯▯▯▯▯▯▯
Task B: ▯▯▰▰▰▯▯▯▯▯▯▯▯▯▯▯
Task C: ▯▯▯▯▯▰▰▰▰▰▯▯▯▯▯▯
Task D: ▯▯▯▯▯▯▯▯▯▯▰▰▰▰▰▰
        └────────────────────→ Time

Tasks take turns on single core
Total time: ~16 time units
```

**Multithreading: Parallel on 4 Cores**

```
Core 1: ▰▰▰▰▰▰▰▰ (Task A)
Core 2: ▰▰▰▰▰▰▰▰ (Task B)
Core 3: ▰▰▰▰▰▰▰▰ (Task C)
Core 4: ▰▰▰▰▰▰▰▰ (Task D)
        └────────────────→ Time

All tasks run simultaneously
Total time: ~8 time units (4x faster!)
```

---

### Key Differences Table

| Aspect | Concurrency (1 Core) | Multithreading (Multi Core) |
|--------|----------------------|------------------------------|
| **Cores Used** | 1 | 4+ |
| **Execution** | Interleaved (take turns) | Parallel (simultaneous) |
| **At any moment** | 1 task running | Multiple tasks running |
| **Speed** | Slower | Faster (4x with 4 cores) |
| **Example** | Node.js, async/await | Python threads, Java threads |
| **Diagram** | ▰▯▰▯▰▯ (switching) | ▰▰▰▰ (all at once) |

---

### TL;DR - Visual Summary

```
Concurrency (1 Core):
   Core: ▰▯▰▯▰▯  (switching between tasks)
   Result: Handles multiple tasks (not truly parallel)

Multithreading (4 Cores):
   C1: ▰▰▰▰▰▰  (Task 1)
   C2: ▰▰▰▰▰▰  (Task 2)
   C3: ▰▰▰▰▰▰  (Task 3)
   C4: ▰▰▰▰▰▰  (Task 4)
   Result: Multiple tasks run at same time (true parallel)
```

## Hashtable vs HashMap

**Both hash-based key-value stores, but different approaches.**

### Key Differences

| Aspect | Hashtable | HashMap |
|--------|-----------|---------|
| **Thread-Safe** | ✅ YES (synchronized) | ❌ NO |
| **Performance** | Slower (locks everything) | Faster (no locks) |
| **Null Keys** | ❌ Not allowed | ✅ Allowed |
| **Null Values** | ❌ Not allowed | ✅ Allowed |
| **Legacy** | ✅ Old (Java 1.0) | ✅ Modern (Java 1.2+) |
| **Best For** | Multi-threaded (rarely) | Single-threaded (common) |

### Why NOT Use Hashtable?

**Hashtable locks entire map for every operation → Only 1 thread at a time → SLOW**

```
Hashtable:          HashMap:
Thread 1: ▰▰▰       Thread 1: ▰▰▰
Thread 2: ▯▯▯       Thread 2: ▰▰▰ (simultaneous!)
Thread 3: ▯▯▯       Thread 3: ▰▰▰ (simultaneous!)
```

### Better Alternatives for Multi-threaded Apps

```
For Maps:      ConcurrentHashMap ✅ (segment-based locking)
For Lists:     CopyOnWriteArrayList ✅ (read-heavy)
For Queues:    ConcurrentLinkedQueue ✅ (lock-free)
For Custom:    ReadWriteLock ✅ (reader-writer sync)

Avoid:         Hashtable ❌ (legacy, slow)
```

---

## ReentrantReadWriteLock

**Allows multiple READERS or single WRITER (but not both simultaneously)**

### Why Do We Need Read Locks?

**Read lock prevents WRITERS, not readers!**

Without read lock:
```
Reader:  Reading balance
Writer:  Changes balance mid-read
Result:  Reader gets corrupted value! ❌

With read lock:
Reader:  ▰▰▰▰▰▰▰▰ (reading safely)
Writer:  ▯▯▯▯▰▰▰▰ (waits for reader, then exclusive access)
Result:  No corruption! ✓
```

### How It Works

```
Read Lock:
├─ Multiple readers: Can acquire simultaneously ✓
└─ Prevent writers: Writer must wait ✓

Write Lock:
├─ Only 1 writer: Exclusive access
└─ Prevent readers: Readers wait ✓
```

### Timeline Example

```
Reader 1: ▰▰▰▰▰▯▯
Reader 2: ▰▰▰▰▰▯▯ (simultaneous with Reader 1!)
Writer:   ▯▯▯▯▯▰▰ (exclusive, after all readers)

Multiple readers together, then exclusive writer!
```

### Code Example

```java
ReadWriteLock lock = new ReentrantReadWriteLock();
int balance = 1000;

// Many threads reading
lock.readLock().lock();
try {
    int myBalance = balance;  // ✓ Safe! Prevents writer
} finally {
    lock.readLock().unlock();
}

// Few threads writing
lock.writeLock().lock();
try {
    balance = 2000;  // ✓ Exclusive! Prevents readers
} finally {
    lock.writeLock().unlock();
}
```

### When to Use

```
Use if:
✅ Many readers, few writers
✅ Read operations are fast
✅ Cache, configurations, profiles

Don't use if:
❌ Frequent writes
❌ Read & write equally common
```

### TL;DR

```
Read Lock = "Prevent writers while I read"
Write Lock = "I need exclusive access"

Multiple readers: No conflict ✓
Reader + Writer: Writer waits ✓
Multiple writers: Only 1 at a time ✓

"Reentrant" = Same thread can acquire lock multiple times
```

## TTL (Time To Live) Strategies

**TTL fundamentally answers: "When should cached data expire?"**

### Two Ways to Express TTL

#### 1. Duration-Based (Relative)

"Expire X seconds/minutes from when cached"

```
cache.put(key, value, TTL=300);  # 5 minutes from now

Item cached at 10:00 → Expires at 10:05
Item cached at 11:00 → Expires at 11:05
Item cached at 12:00 → Expires at 12:05

Same TTL, different expiry times (relative to caching)
```

#### 2. Time-Based (Absolute)

"Expire at specific point in time"

```
cache.put(key, value, expireAt=2024-01-15T23:59:59);

Item cached at 10:00 AM → Expires at midnight
Item cached at 11:00 AM → Expires at midnight
Item cached at 11:30 PM → Expires at midnight

Same expiry time (absolute point in time)
```

### Strategies for Managing TTL

These are STRATEGIES, not different TTL types:

#### Strategy 1: Fixed Duration

Expires after set duration, no reset.

```
TTL=5 min (set once, doesn't change)

10:00  Cached (expires 10:05)
10:02  Access: still expires 10:05 ❌
10:05  Expires (removed)
```

**Use:** API responses, general data

#### Strategy 2: Sliding Window

TTL resets every time item is accessed.

```
TTL=5 min (resets on access)

10:00  Cached (expires 10:05)
10:02  Access: expires resets to 10:07 ✓
10:04  Access: expires resets to 10:09 ✓
10:09  Expires (if no access)

Item stays in cache as long as accessed!
```

**Use:** Sessions, active user data

#### Strategy 3: Proactive Refresh

Refresh cache BEFORE it expires.

```
TTL=5 min, refresh before=30 sec

10:04:30  TTL near expiry → Background refresh
10:05  Cache refreshed with fresh data
10:06  User access: Gets latest data ✓

No cache miss! Always fresh!
```

**Use:** Popular/hot data, leaderboards, critical data

#### Strategy 4: Lazy Check

Expiry checked only when accessed, not removed proactively.

```
10:05  TTL expired (still in memory - stale!)
10:06  Access: Check TTL → Expired! Remove

Advantage: No background cleanup
Disadvantage: Stale data in memory temporarily
```

**Use:** Low-traffic items, memory optimization

#### Strategy 5: Adaptive TTL

Adjust duration based on access frequency.

```
Frequently accessed: TTL=1 min (hot data)
Rarely accessed: TTL=24 hours (cold data)
Medium accessed: TTL=10 min (warm data)

Auto-optimizes based on usage patterns!
```

**Use:** Unknown access patterns, hybrid workloads

#### Strategy 6: Variable TTL by Data Type

Different TTL per data type.

```
Session:          5 min (changes frequently)
User Profile:     1 hour (medium change)
Product Catalog:  24 hours (admin-managed)
System Config:    7 days (rarely changes)
Daily Report:     End of day (time-specific)
```

**Use:** Multi-type caches, mixed workloads

---

## TTL vs Cache Eviction

**TTL and Eviction are INDEPENDENT but COMPLEMENTARY**

| Aspect | TTL | Eviction (LRU/LFU) |
|--------|-----|-------------------|
| **Trigger** | Time-based | Space-based (cache full) |
| **When** | Automatically after X sec | Only when cache FULL |
| **Removes** | Old/stale data | Unused/least-used data |
| **Independent** | ✅ YES (anytime) | ✅ YES (if full) |

### How They Work Together

```
                    CACHE ITEM
                        │
         ┌──────────────┼──────────────┐
         ▼              ▼              ▼
      TTL EXPIRED   CACHE FULL   MANUALLY DELETED
    (Time trigger) (Space trigger) (App trigger)
         │              │              │
         └──────────────┼──────────────┘
                        ▼
                    REMOVE ITEM
```

### Timeline Example

```
Time    | Event                      | Action
--------|----------------------------|-------------------
10:00   | Cache Item X (TTL=10min)   | Add
10:02   | Cache full (100 items)     | Full ✓
10:05   | Item Y TTL expires         | Remove (TTL)
10:05   | Cache: 99 items            | Space freed!
10:12   | Cache full again           | Full ✓
10:12   | New request                | LRU eviction
```

### TL;DR

```
Without TTL:   Stale data lingers ❌
Without Eviction: Cache can overflow ❌
With Both:     Perfect cache! ✓

TTL = What (remove old)
Eviction = How (remove when full using LRU/LFU)
```

---

## Item-Level vs Cache-Level TTL

**Where TTL is applied: Individual items or entire cache?**

### Item-Level TTL (99% of Cases)

Each item has its OWN individual TTL.

```
Cache:
├─ Item A: TTL=5 min (expires 10:05)
├─ Item B: TTL=1 hour (expires 11:00)
├─ Item C: TTL=10 min (expires 10:10)
└─ Item D: TTL=24 hours (expires tomorrow)

Each expires at DIFFERENT times independently!
```

**Code:**

```java
// Item-level TTL (Redis)
SET user:1 data EX 300      # 5 min TTL
SET profile:1 data EX 3600  # 1 hour TTL
SET config data EX 86400    # 24 hours TTL

// Each expires at its own time
```

**Examples:** Redis, Memcached, most caches

**Benefits:**
```
✅ Granular control per item
✅ Different data types = different TTLs
✅ Flexible management
✅ Standard approach
```

---

### Cache-Level TTL (Rare, Sessions Only)

Entire cache/session expires together, ALL items removed.

```
Cache session starts: 10:00
Cache-level TTL: 30 minutes
Cache expires: 10:30 (EVERYTHING cleared!)

Result: ALL items removed (regardless of individual TTL)
```

**Code:**

```java
// Cache-level TTL (Session Timeout)
HttpSession session = request.getSession();
session.setMaxInactiveInterval(1800);  // 30 min total TTL

// After 30 min: ENTIRE cache cleared!
// Even if an item had 24-hour individual TTL
```

**Examples:** Web session timeouts, temporary caches

**Benefits:**
```
✅ Simple: One timeout for whole session
✅ Safe: Guarantees session cleanup
✅ Common in: Web frameworks (JSP, Spring)
```

---

### Comparison

| Aspect | Item-Level | Cache-Level |
|--------|-----------|------------|
| **Scope** | Per item | Entire cache |
| **Individual TTLs** | ✅ YES (each item diff) | ❌ NO (all same) |
| **Flexibility** | High | Low |
| **Common** | ✅ YES (99%) | ❌ Rare (1%, sessions) |
| **Example** | Redis: Each key has TTL | Session: 30 min timeout |

---

### Can You Have BOTH?

**YES! Item-level + Cache-level TTL (Hybrid)**

```
Session expires: 30 min (cache-level)
├─ Item A: TTL=5 min (item-level)
├─ Item B: TTL=10 min (item-level)
├─ Item C: TTL=2 hours (item-level)

Whichever expires FIRST wins:
├─ 5 min: Item A removed
├─ 10 min: Item B removed
├─ 30 min: Session timeout → ALL removed!
└─ Item C never gets to 2 hours
```

**Result:** Items removed by earliest expiry (item or cache level)

---

### TL;DR

```
99% of cases: Item-Level TTL
├─ Each item: put(key, value, TTL)
├─ Different items = different expiry times
└─ Example: Redis (SET key value EX 300)

1% of cases: Cache-Level TTL
├─ Entire cache: setMaxAge(TTL) or setMaxInactiveInterval()
├─ All items: same expiry time
└─ Example: Web session timeout

Best practice: Use Item-level + add Cache-level for safety
```

## Cache Capacity Boundary Logic

**Cache capacity can limit by NUMBER OF ENTRIES or MEMORY SIZE (or both)**

### Approach 1: Entry-Based Capacity

**Cache holds maximum N items**

```
Capacity: 1000 entries (max)

Cache fills to 1000 entries → FULL!
Entry 1001 added → Trigger eviction
```

**Implementation:**

```java
public class LRUCache<K, V> {
    private int capacity;  // Max entries
    private Map<K, V> map;
    
    public void put(K key, V value) {
        if (map.size() >= capacity) {
            evictLRU();  // Remove least recently used
        }
        map.put(key, value);
    }
    
    public int getCurrentSize() {
        return map.size();
    }
    
    public boolean isFull() {
        return map.size() >= capacity;
    }
}

// Usage
LRUCache<String, String> cache = new LRUCache<>(1000);  // Max 1000
cache.put("user:1", data);
System.out.println(cache.getCurrentSize());  // Current entries
System.out.println(cache.isFull());          // Is at capacity?
```

**Pros/Cons:**
```
✅ Simple: Just count items
✅ Predictable: Exactly 1000 items
❌ Ignores memory: Small items waste space, large items exceed RAM
```

---

### Approach 2: Memory-Based Capacity

**Cache holds maximum X MB/GB of memory**

```
Capacity: 1GB memory (max)

Cache fills to 1GB → FULL!
Adding more → Trigger eviction
```

**Implementation:**

```java
public class MemoryBoundCache<K, V> {
    private long maxMemoryBytes;
    private long currentMemoryBytes = 0;
    private Map<K, V> cache;
    private Map<K, Long> sizeMap;  // Track size of each item
    
    public void put(K key, V value) {
        long itemSize = calculateSize(value);
        
        // Check if adding would exceed capacity
        while (currentMemoryBytes + itemSize > maxMemoryBytes) {
            evictLRU();
        }
        
        cache.put(key, value);
        sizeMap.put(key, itemSize);
        currentMemoryBytes += itemSize;
    }
    
    private long calculateSize(V value) {
        if (value instanceof String) {
            return 48 + ((String) value).length() * 2;  // String overhead + chars
        } else if (value instanceof byte[]) {
            return 16 + ((byte[]) value).length;
        }
        return 1024;  // Default estimate
    }
    
    public long getCurrentMemory() {
        return currentMemoryBytes;
    }
    
    public double getCapacityUsage() {
        return (double) currentMemoryBytes / maxMemoryBytes * 100;
    }
}

// Usage
MemoryBoundCache<String, String> cache = 
    new MemoryBoundCache<>(1024 * 1024 * 100);  // 100MB

cache.put("user:1", largeData);
System.out.println(cache.getCurrentMemory() / (1024*1024) + " MB");
System.out.println(cache.getCapacityUsage() + "%");
```

**Pros/Cons:**
```
✅ Realistic: Accounts for actual memory
✅ Safe: Won't exceed RAM limit
✅ Flexible: Small items fit more
❌ Complex: Need to calculate item sizes
❌ Variable: Number of items varies
```

---

### Comparison

| Aspect | Entry-Based | Memory-Based |
|--------|-----------|------------|
| **Limit** | N items (e.g., 1000) | X MB/GB (e.g., 1GB) |
| **Size matters** | ❌ No | ✅ YES |
| **Batch job needed** | ❌ NO (inline eviction) | Depends (inline or background) |
| **Common in** | LRU cache implementations | Custom caches, Redis |

---

## Calculate Memory Consumed by HashMap

**Three approaches with different accuracy/complexity tradeoffs**

### Approach 1: Manual Estimation (Simple, ~70% Accurate)

**Estimate based on object sizes**

```java
public class MemoryEstimator {
    static final long OBJECT_HEADER = 16;
    static final long HASHMAP_OVERHEAD = 48;
    static final long ENTRY_OVERHEAD = 32;
    static final long CHAR_SIZE = 2;
    
    public static long estimateMapMemory(Map<String, String> map) {
        long total = HASHMAP_OVERHEAD;  // HashMap object
        
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            total += ENTRY_OVERHEAD;                    // Entry node
            total += estimateStringSize(key);           // Key
            total += estimateStringSize(value);         // Value
        }
        
        return total;
    }
    
    private static long estimateStringSize(String str) {
        if (str == null) return 0;
        return OBJECT_HEADER + 8 + str.length() * CHAR_SIZE;
    }
}

// Usage
Map<String, String> cache = new HashMap<>();
cache.put("user:1", "John Doe");
cache.put("user:2", "Jane Smith");

long bytes = MemoryEstimator.estimateMapMemory(cache);
System.out.println(bytes + " bytes");
System.out.println(bytes / 1024.0 + " KB");
```

**Pros:** ✅ Fast, no setup  
**Cons:** ❌ ~70% accurate, JVM-dependent

---

### Approach 2: Using JOL Library (Best, ~99% Accurate)

**Most accurate and easiest**

```java
// Add dependency: org.openjdk.jol:jol-core

import org.openjdk.jol.info.GraphLayout;

public class MemoryAnalyzer {
    
    // Get deep size (entire object graph)
    public static long getDeepSize(Map<String, String> map) {
        return GraphLayout.parseInstance(map).totalSize();
    }
    
    // Pretty print memory layout
    public static void printMemoryLayout(Map<String, String> map) {
        System.out.println(GraphLayout.parseInstance(map).toFootprint());
    }
}

// Usage
Map<String, String> cache = new HashMap<>();
cache.put("user:1", "John Doe");

long bytes = MemoryAnalyzer.getDeepSize(cache);
System.out.println("Memory: " + bytes + " bytes");

MemoryAnalyzer.printMemoryLayout(cache);
// Output:
// java.util.HashMap@4d405ef instance layout
// INSTANCE DATA:
//   java.lang.Object
//   java.util.HashMap instance data
// TOTAL SIZE: 512 bytes
```

**Pros:** ✅ Accurate (~99%), Easy  
**Cons:** ❌ Requires external library

---

### Approach 3: Serialization Size (Practical, ~80% Accurate)

**Fallback approach, no dependencies**

```java
public class SerializationSizer {
    
    public static long getSerializedSize(Object obj) throws Exception {
        java.io.ByteArrayOutputStream baos = 
            new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oos = 
            new java.io.ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return baos.size();
    }
}

// Usage
Map<String, String> cache = new HashMap<>();
cache.put("user:1", "John Doe");

long bytes = SerializationSizer.getSerializedSize(cache);
System.out.println("Serialized size: " + bytes + " bytes");
```

**Pros:** ✅ No setup, reasonable accuracy  
**Cons:** ❌ Slightly slower, ~80% accurate

---

### Practical Implementation: Track and Cache Sizes

**Best for production custom cache:**

```java
public class CustomMemoryCache<K, V> {
    private long maxMemoryBytes;
    private long currentMemoryBytes = 0;
    private Map<K, V> cache = new HashMap<>();
    private Map<K, Long> sizeMap = new HashMap<>();  // Cache sizes!
    
    public void put(K key, V value) {
        long itemSize = calculateSize(value);
        
        // Evict if needed
        while (currentMemoryBytes + itemSize > maxMemoryBytes) {
            evictLRU();
        }
        
        // Track size (avoid recalculating!)
        cache.put(key, value);
        sizeMap.put(key, itemSize);
        currentMemoryBytes += itemSize;
    }
    
    // Practical size calculation
    private long calculateSize(V value) {
        if (value instanceof String) {
            return 48 + ((String) value).length() * 2;
        } else if (value instanceof byte[]) {
            return 16 + ((byte[]) value).length;
        }
        return 1024;  // Safe default
    }
    
    public void printStats() {
        double usage = (double) currentMemoryBytes / maxMemoryBytes * 100;
        System.out.println("Used: " + (currentMemoryBytes / 1024.0) + " KB");
        System.out.println("Max: " + (maxMemoryBytes / 1024.0) + " KB");
        System.out.println("Usage: " + usage + "%");
        System.out.println("Entries: " + cache.size());
    }
}
```

---

### Comparison of Approaches

| Approach | Accuracy | Speed | Setup | Production |
|----------|----------|-------|-------|------------|
| **Manual Estimation** | ~70% | ⚡⚡⚡ | None | ✅ YES |
| **JOL Library** | ~99% | ⚡⚡ | Easy | ✅ YES |
| **Serialization** | ~80% | ⚡ | None | ✅ YES |

---

### TL;DR

**For Custom Cache:**

```
Choose approach based on needs:

Speed matters most:
└─ Manual estimation (~70% accurate)

Accuracy matters most:
└─ JOL library (~99% accurate)

Production balanced:
└─ Manual + cache calculated sizes

Implementation tip:
├─ Calculate size once on put()
├─ Store in Map<K, Long> sizeMap
├─ Reuse on eviction
└─ Avoid recalculating every time
```

## Hashing Concept

**Hashing = Convert any input into a fixed-size number**

```
hash("user:1") = 45
hash("user:2") = 123
hash("anything") = some_number
```

**Why?**
```
Need to distribute data across servers.

hash(key) % number_of_servers = which_server

Example (3 servers):
hash("user:1") = 45 → 45 % 3 = 0 → Server 0
hash("user:2") = 46 → 46 % 3 = 1 → Server 1
hash("user:3") = 47 → 47 % 3 = 2 → Server 2
```

**Key Property:**
```
Same input = Same hash = Same server (ALWAYS)

hash("user:123") always returns same value
└─ Deterministic (consistent)
```

---

## Consistent Hashing

### Q1: Why Can't We Use Simple `hash(key) % num_servers`?

**Answer:** When you add a server, the modulo divisor changes, so ALL keys recalculate to different servers.

| Method | Before | After | Issue |
|--------|--------|-------|-------|
| `hash(key) % 3` | product_001 → 523 % 3 = 1 (Server B) | product_001 → 523 % 4 = 3 (Server D) | ❌ MOVED! |

**Real Impact:** Add 1 server to 10 servers → **~80% of keys move** → Cache thrashing → DB hammered 💥

---

### Q2: What's the Magic of Consistent Hashing?

**Answer:** Use a **fixed ring size** instead of modulo by number of servers.

| Aspect | Regular Hash | Consistent Hash |
|--------|-------------|-----------------|
| Hash calculation | `hash(key) % num_servers` | `hash(key) % ring_size` |
| Changes when | Server added → **ALL keys recalculate** | Server added → **Ring size stays same** |
| Redistribution | ~80-100% | ~1/n (e.g., 10% with 10 servers) |
| Better? | ❌ Bad | ✅ Good |

**Why it works:** Keys stay at same position on ring. Only the server nearest to them changes.

---

### Q3: Show Me with Real Numbers

**Setup:** Ring size = 100, 3 Servers at positions A(20), B(40), C(75)

| Product | Hash | Position % 100 | **Before Adding D** | **After Adding D at 50** | Moved? |
|---------|------|---|---------|---------|--------|
| product_001 | 523 | 23 | A (20)* | A (20)* | ✅ NO |
| product_002 | 1047 | 47 | C (75)* | D (50)* | ❌ YES |
| product_003 | 85 | 85 | A (20)* | A (20)* | ✅ NO |
| product_004 | 412 | 12 | A (20)* | A (20)* | ✅ NO |
| product_005 | 654 | 54 | C (75)* | C (75)* | ✅ NO |
| product_006 | 299 | 99 | A (20)* | A (20)* | ✅ NO |
| product_007 | 782 | 82 | A (20)* | A (20)* | ✅ NO |
| product_008 | 165 | 65 | C (75)* | C (75)* | ✅ NO |
| product_009 | 338 | 38 | B (40)* | B (40)* | ✅ NO |
| product_010 | 509 | 9 | A (20)* | A (20)* | ✅ NO |

**Result:** Only **1 out of 10 (10%) moved** ✓  
*= "Next server clockwise" from that position

---

### Q4: Why Only 10% Moved? Explain the Mechanism

**Answer:** Only keys in the ring range of the new server are affected.

| Ring Range | Original | New | Keys Affected |
|-----------|----------|-----|--------------|
| 0-20 | → A | → A | None (unchanged) |
| 20-40 | → B | → B | None (unchanged) |
| 40-50 | → C | → **D (NEW!)** | product_002 (hash=47) ✓ |
| 50-75 | → C | → C | None (unchanged) |
| 75-100 | → A | → A | None (unchanged) |

**Key insight:** Server D at position 50 only steals the [40-50) ring range from C. That's only 10% of the ring.

---

### Q5: What's "Next Server Clockwise"?

**Answer:** Starting from key's ring position, find the first server ≥ that position (circular).

**Example with product_002 (hash=47):**

```
Before: Servers at 20 ──── 40 ──── 75 ──── 100
                                              ↑ wraps to 0
        
product_002 at position 47:
- Is there a server ≥ 47? 
- YES! Server at 75
- product_002 → Server C ✓

After: Servers at 20 ──── 40 -- 50 -- 75 ──── 100

product_002 at position 47:
- Is there a server ≥ 47?
- YES! Server at 50 (NEW D is before 75)
- product_002 → Server D ✓ MOVED!
```

---

### Q6: Comparison - Regular Hash vs Consistent Hash

**Same 10 products, add 1 server to 3 servers:**

**REGULAR HASH: `hash % num_servers` (3 → 4)**

| Product | 3 Servers | 4 Servers | Moved? |
|---------|-----------|-----------|--------|
| product_001 | 523 % 3 = 1 → B | 523 % 4 = 3 → D | ❌ YES |
| product_002 | 1047 % 3 = 0 → A | 1047 % 4 = 3 → D | ❌ YES |
| product_003 | 85 % 3 = 1 → B | 85 % 4 = 1 → B | ✅ NO |
| product_004 | 412 % 3 = 1 → B | 412 % 4 = 0 → A | ❌ YES |
| product_005 | 654 % 3 = 0 → A | 654 % 4 = 2 → C | ❌ YES |
| product_006 | 299 % 3 = 2 → C | 299 % 4 = 3 → D | ❌ YES |
| product_007 | 782 % 3 = 2 → C | 782 % 4 = 2 → C | ✅ NO |
| product_008 | 165 % 3 = 0 → A | 165 % 4 = 1 → B | ❌ YES |
| product_009 | 338 % 3 = 2 → C | 338 % 4 = 2 → C | ✅ NO |
| product_010 | 509 % 3 = 2 → C | 509 % 4 = 1 → B | ❌ YES |

**Total moved: 7 out of 10 (70%)** 💥

---

**CONSISTENT HASH: Ring size 100 (never changes)**

| Product | Position | Before | After D(50) | Moved? |
|---------|----------|--------|------------|--------|
| product_001 | 23 | A(20) | A(20) | ✅ NO |
| product_002 | 47 | C(75) | D(50) | ❌ YES |
| product_003 | 85 | A(20) | A(20) | ✅ NO |
| product_004 | 12 | A(20) | A(20) | ✅ NO |
| product_005 | 54 | C(75) | C(75) | ✅ NO |
| product_006 | 99 | A(20) | A(20) | ✅ NO |
| product_007 | 82 | A(20) | A(20) | ✅ NO |
| product_008 | 65 | C(75) | C(75) | ✅ NO |
| product_009 | 38 | B(40) | B(40) | ✅ NO |
| product_010 | 9 | A(20) | A(20) | ✅ NO |

**Total moved: 1 out of 10 (10%)** ✓

**Difference: 70% vs 10% = 7x better!**

---

### Q7: What Are Virtual Nodes?

**Answer:** Place each server **multiple times** on the ring to improve distribution.

**Without Virtual Nodes:**
```
Ring:  0 -------- 33 -------- 67 -------- 100
      Server A   Server B   Server C

Problem: Uneven distribution, A gets more load
```

**With Virtual Nodes (each server = 3 copies):**
```
Ring:  0--A---B--A---C--B--A--C---B---C---A--100
       S  S  S  S  S  S  S  S  S  S  S  S

Result: More even distribution across ring
```

| Metric | Without VN | With VN (3x) |
|--------|-----------|------------|
| Distribution | Uneven | More uniform |
| Hot spots | Possible | Less likely |
| Per server impact | High | Low |

---

### Q8: Real-World Production Impact

**Scenario:** 1 billion keys, 10 servers → Add 1 new server

| Method | Keys Moved | Network Requests | Cache Hit Rate Impact |
|--------|-----------|------------------|----------------------|
| Regular hash | ~909M (90%) | 909M requests | 😱 Cache thrash |
| Consistent hash | ~91M (10%) | 91M requests | ✓ Mostly intact |
| **Savings** | **818M fewer** | **90% less work** | **~10x better** |

---

### Q9: Key Concepts Summary

| Concept | Explanation |
|---------|------------|
| **Ring Size** | Fixed number (e.g., 100 or 2^32) — NEVER changes |
| **Key Position** | `hash(key) % ring_size` — SAME before & after |
| **Server Position** | `hash(server_id)` — determines where on ring |
| **Next Clockwise** | First server position ≥ key position |
| **Affected Range** | Only keys in [new_server.pos - prev_server.pos) |
| **Why It Works** | Keys stay at same ring position; only servers move |

---

### Q10: When Do We Use Consistent Hashing?

**Use in:**
- ✅ Redis/Memcached clusters (distribute cache across servers)
- ✅ Database sharding (distribute data across databases)
- ✅ Load balancers (distribute requests across servers)
- ✅ CDNs (distribute content across edge nodes)
- ✅ Any distributed system with dynamic membership

**Avoid if:**
- ❌ Single server (no distribution needed)
- ❌ Fixed servers (no scaling planned)

---

## How Zones Work in Consistent Hashing

**Zones = Auto-created ranges by hashing servers**

| Ring Section | Server | Why |
|-------------|--------|-----|
| 0 to hash(Server A) | Server A | Range between 0 and A's hash |
| hash(Server A) to hash(Server B) | Server B | Range between A and B |
| hash(Server B) to 2^32 (wrap) | A or C | Depends on ring positions |

**Key Rule:** "Zone belongs to the server STARTING at that position"

---

## TL;DR: Consistent Hashing

```
Problem:
  Add server → hash % num_servers changes → ~100% keys rehashed → Disaster

Solution:
  Use fixed ring size → hash % ring_size stays same → Only ~1/n keys move → Safe

Magic Formula:
  key_position = hash(key) % ring_size       ← NEVER CHANGES
  server_position = hash(server_id)          ← WHERE SERVER SITS
  next_server = find_first(server ≥ key)     ← "NEXT CLOCKWISE"

Real Impact:
  Without: 1B keys + 1 new server = 909M keys move (90%)
  With: 1B keys + 1 new server = 91M keys move (10%)
  → 10x better!

Used in:
  ├─ Redis/Memcached clusters (distributed cache)
  ├─ Database sharding (distributed storage)
  ├─ Load balancers (distributed requests)
  └─ CDNs (distributed content)
```
