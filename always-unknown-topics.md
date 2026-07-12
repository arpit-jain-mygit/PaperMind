# Always Unknown Topics

Topics that frequently come up in interviews but are often unclear or misunderstood.

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
