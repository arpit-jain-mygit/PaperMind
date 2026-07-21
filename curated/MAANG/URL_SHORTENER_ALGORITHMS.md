# URL Shortener Algorithm Selection Guide

## Table of Contents

1. [Core Recommendation](#core-recommendation)
2. [The Problem We're Solving](#the-problem-were-solving)
3. [Approach 0: Naive Hashing](#approach-0-naive-hashing-the-starting-point---doesnt-work)
4. [Deep Dive: Understanding CRC32](#deep-dive-understanding-crc32-why-it-fails)
   - [What is CRC32?](#what-is-crc32)
   - [How CRC32 Works](#how-crc32-works-simple-example-with-division)
   - [Why Only 4.3 Billion Values?](#why-only-43-billion-values-the-fundamental-limit)
   - [Why Collisions Happen](#why-collisions-happen-pigeonhole-principle)
   - [The Timeline: When Does CRC32 Exhaust?](#the-timeline-when-does-crc32-exhaust)
   - [Why CRC32 Can't Be Fixed](#why-crc32-cant-be-fixed)
   - [Performance Reality](#crc32-performance-reality)
   - [CRC32 Process Flow](#crc32-process-flow-request--response)
5. [Algorithm 1: Hash + Collision Resolution](#algorithm-1-hash--collision-resolution-)
6. [Algorithm 2: Base 62 Conversion](#algorithm-2-base-62-conversion--recommended)
   - [How Base62 Solves CRC32's Problems](#how-base62-solves-crc32s-problems)
   - [Base62 Process Flow](#base62-process-flow-request--response)
   - [Performance Comparison: Day 30 Scenario](#performance-comparison-day-30-scenario)
7. [Algorithm 3: Pre-Generation + Queue](#algorithm-3-pre-generation--queue-chubb-approach-)
8. [Algorithm Comparison Matrix](#algorithm-comparison-matrix)
9. [Recommendation by Scale](#recommendation-by-scale)
10. [Key Takeaways](#key-takeaways)
11. [Implementation Checklist](#implementation-checklist-for-base-62-production-system)

---

## Core Recommendation

For production systems, **Base 62 Conversion** is the optimal choice. It eliminates collisions entirely through mathematics (not luck), provides O(1) performance with no database lookups, and scales linearly with load. Default answer for any system handling >1K req/sec.

---

## The Problem We're Solving

**Back-of-envelope numbers:**
- 100M URLs generated per day → 1,160 writes/sec
- 10:1 read-to-write ratio → 11,600 reads/sec  
- 365 billion URLs over 10 years → need 62^7 ≈ 3.5 trillion combinations
- **Required:** 7-character short codes using alphabet [0-9, a-z, A-Z]

The challenge: **Turn a long URL into a guaranteed-unique 7-character code, fast, without collisions.**

---

## Approach 0: Naive Hashing (The Starting Point - ❌ Doesn't Work)

**The Idea:** Hash the long URL directly using CRC32, MD5, or SHA-1, truncate to 7 chars.

**Why people think it works:**
- Hash functions are fast
- Always produce same output for same input
- Have large output space

**The collision disaster:**

| Hash Func | Output Length | Truncated to 7 | Problem |
|-----------|---------------|---|---------|
| CRC32 | 8 hex chars | "5cb54054"[0:7] | Still 8 chars, truncate more → collision risk |
| MD5 | 32 hex chars | "5eb63bbb..."[0:7] | Throw away 25 chars of uniqueness |
| SHA-1 | 40 hex chars | "0eeae791..."[0:7] | Throw away 33 chars of uniqueness |

**The math:**
```
Without truncation: Hash collisions are statistically near-zero
After truncating to 7 chars: You've eliminated the uniqueness guarantee
Result: Different URLs WILL produce the same 7-char prefix
```

**Verdict:** ❌ Mathematically broken. Never use this approach.

---

## Deep Dive: Understanding CRC32 (Why It Fails)

### What is CRC32?

CRC32 = 32-bit Cyclic Redundancy Check. It's a **compression function** that:
- Takes any input (like a URL)
- Uses **polynomial division** to produce a hash
- Outputs a **32-bit number** (4.3 billion possible values)
- Originally designed for **error detection**, not uniqueness

### How CRC32 Works (Simple Example with Division)

**Step-by-step with decimal numbers:**

```
Input:    "hello"
Divisor:  17 (like the CRC polynomial)

Step 1: Convert to number
        h=104, e=101, l=108, l=108, o=111
        Combined: 104101108108111

Step 2: Divide by divisor
        104101108108111 ÷ 17 = quotient remainder 9
                                           ↑
                                    HASH VALUE!

Step 3: Output the remainder
        Hash = 9

Real CRC32 does the same thing but:
  - Uses binary division (XOR instead of subtraction)
  - Uses polynomial: 0x04C11DB7
  - Returns 32-bit number (0 to 4,294,967,295)
  - Displays as 8 hex characters: 5CB54054
```

### Why Only 4.3 Billion Values? (The Fundamental Limit)

**CRC32 = 32 bits = 2^32 possible combinations**

```
How many combinations with N bits?

1 bit:   0, 1                     = 2^1 = 2 values
2 bits:  00, 01, 10, 11          = 2^2 = 4 values
3 bits:  000, 001, 010, ...      = 2^3 = 8 values
...
32 bits: 00000...11111111        = 2^32 = 4,294,967,296 values
                                        ≈ 4.3 billion

This is MATHEMATICAL LAW!
32 bits can represent exactly 2^32 combinations. No more!
```

**You can't get more values by changing the format:**

```
Same value, different ways to write:
  Hex:       5CB54054 (8 characters)
  Binary:    01011100101101010100000001010100 (32 digits)
  Decimal:   1,557,154,820

All represent the same 32-bit number!
All limited to 4.3 billion maximum values!

(This is NOT 62^8 = base62. That's a different system entirely!
 CRC32 uses hex which is 16^8 = 2^32 = 4.3 billion)
```

### Why Collisions Happen (Pigeonhole Principle)

**The Math:**

```
CRC32 outputs: 4.3 billion possible values
URLs in 10 years: 365 billion

365B > 4.3B

GUARANTEED by math: Collisions MUST happen!
You can't fit 365 billion items into 4.3 billion slots.
```

**When Do Collisions Start?**

```
Mathematical threshold: √(4.3 billion) ≈ 65,536

After 65,536 random inputs: 50% chance of collision
After 100 million inputs: 99%+ guaranteed collision

For URL shortener at 100M/day:
  After 1 hour: 4.5M URLs > 65K threshold
  Result: Collision 100% guaranteed on DAY 1!
```

### The Timeline: When Does CRC32 Exhaust?

**At 100M URLs per day:**

```
CRC32 capacity: 4.3 billion
Daily rate: 100 million URLs

Days to exhaust: 4.3B ÷ 100M = 43 days

Detailed timeline:

Day 1:   Collisions START
         URLs: 100M (2% capacity used)
         Latency: 150ms
         Status: "Working" (but broken internally)

Day 10:  1 billion used (23% capacity)
         Collisions everywhere
         Latency: 300ms
         Database CPU: 80%

Day 20:  2 billion used (47% capacity)
         System degrading
         Latency: 400ms
         Database CPU: 90%

Day 30:  3 billion used (70% capacity)
         System struggling
         Latency: 500ms+
         Database CPU: 100%
         Success rate: 80%

Day 40:  4 billion used (93% capacity)
         System failing
         Latency: 1000ms+
         Success rate: 50%
         Users see timeouts

Day 43:  4.3 billion (100% EXHAUSTED!)
         ALL CODES USED UP
         Cannot create new short URLs
         SERVICE DOWN ❌
         Emergency: Rebuild system!
```

### Why CRC32 Can't Be "Fixed"

**Common misconception:**

```
❌ "Can't we just use more bits to prevent collisions?"

Answer: That's not CRC32 anymore!
  - CRC32 is defined as 32 bits (standard)
  - Changing it makes it CRC40, CRC64, etc.
  - But you're asking about a different algorithm!

❌ "Can't we just pad the output?"

Answer: No!
  - Padding with zeros doesn't create more values
  - 5CB54054 and 000005CB54054 are the SAME number
  - Same 4.3 billion maximum values!

✓ Real solution: Don't use hashing at all!
  Use Base62 + unique ID generator instead
```

### CRC32 Performance Reality

```
100M URLs/day for 43 days:

Day 1:    System seems fine (150ms latency)
Day 10:   Getting slow (300ms latency)
Day 20:   Database struggling (90% CPU)
Day 30:   System barely working (80% success rate)
Day 40:   Mostly broken (50% success rate)
Day 43:   Complete failure (service down)

This is NOT theoretical—it's guaranteed math!
```

### CRC32 Process Flow (Request → Response)

**URL Shortening Flow (from Alex Xu Chapter 8 - Hash + Collision Resolution):**

```
Client Request:
  POST /api/v1/data/shorten
  { "longUrl": "https://en.wikipedia.org/wiki/Systems_design" }
           ↓
   ┌──────────────────────────────────┐
   │   Load Balancer                  │
   │   Routes to Web Server           │
   └──────────────┬───────────────────┘
                  ↓
   ┌──────────────────────────────────┐
   │   Step 1: Check if exists        │
   │   Query: SELECT short_url        │
   │           WHERE long_url = ?     │
   │                                  │
   │   If YES: Return existing code   │
   │   If NO:  Proceed to Step 2      │
   └──────────────┬───────────────────┘
                  ↓
   ┌──────────────────────────────────┐
   │   Step 2: Hash long URL          │
   │   hash_value = CRC32(longUrl)    │
   │   = 5eb63bbbe01eeed093cb22...    │
   └──────────────┬───────────────────┘
                  ↓
   ┌──────────────────────────────────┐
   │   Step 3: Take first 7 chars     │
   │   short_url = hash[0:7]          │
   │   = "5eb63bb"                    │
   └──────────────┬───────────────────┘
                  ↓
   ┌──────────────────────────────────────────┐
   │   Step 4: Check collision (Loop)         │
   │   Query: SELECT * FROM url_mapping      │
   │           WHERE short_url = ?           │
   │                                         │
   │   ╔══════════════════════════════════╗  │
   │   ║ If collision found:              ║  │
   │   ║  1. Append counter (1,2,3...)    ║  │
   │   ║  2. Hash again                   ║  │
   │   ║  3. Take first 7 chars           ║  │
   │   ║  4. Check again (Loop!)          ║  │
   │   ║                                  ║  │
   │   ║ This repeats until NO collision ║  │
   │   ╚══════════════════════════════════╝  │
   │                                         │
   │   If NO collision: Proceed to Step 5    │
   └──────────────┬──────────────────────────┘
                  ↓
   ┌──────────────────────────────────────┐
   │   Step 5: Save to Database          │
   │   INSERT INTO url_mapping (         │
   │     id, short_url, long_url, ...    │
   │   )                                 │
   └──────────────┬──────────────────────┘
                  ↓
   ┌──────────────────────────────────────┐
   │   Step 6: Return Response            │
   │   {                                  │
   │     "shortUrl": "tinyurl.com/5eb63bb"│
   │   }                                  │
   └──────────────────────────────────────┘
```

**Step-by-Step Timeline:**

```
Time  Operation                      Latency  Cumulative
────────────────────────────────────────────────────────
0ms   Load balancer routes           1ms      1ms
1ms   Check if exists (DB query)     10ms     11ms
11ms  Hash URL (CRC32)               0.1ms    11.1ms
11.1ms Take first 7 chars            0.1ms    11.2ms
11.2ms Check collision (DB query)    10ms     21.2ms
       ├─ No collision? Continue
       ├─ Collision? Retry (multiple times!)
       │  └─ Append counter
       │  └─ Hash again (0.1ms)
       │  └─ Check collision (10ms)
       │  └─ Loop again if needed...
       │
21.2ms Save to database              15ms     36.2ms
36.2ms Return response               1ms      37.2ms
────────────────────────────────────────────────────────
                    TOTAL: ~37-50ms (or 150-500ms if collisions!)
```

**Real-World Collision Scenario:**

```
Input: https://en.wikipedia.org/wiki/Systems_design

Step 1: Check if exists in DB
        Query result: Not found ✓

Step 2: Hash (CRC32)
        Result: 5eb63bbbe01eeed093cb22bb8f5acdc3

Step 3: Take first 7
        Result: "5eb63bb"

Step 4a: Check collision
         Query: SELECT * WHERE short_url = '5eb63bb'
         Result: FOUND! (collision with another URL!) ✗
         
Step 4b: Collision detected! Retry with counter=1
         New input: 5eb63bbbe01eeed093cb22bb8f5acdc31
         Hash: abc123... 
         Take first 7: "abc1234"
         Check collision: FOUND AGAIN! ✗
         
Step 4c: Counter=2, retry again
         New input: 5eb63bbbe01eeed093cb22bb8f5acdc32
         Hash: xyz789...
         Take first 7: "xyz7890"
         Check collision: NOT FOUND! ✓

Step 5: Save "xyz7890" to database
        Total latency: 50-100ms (multiple DB round-trips!)
```

**Why This Process Gets Slower Over Time:**

```
Day 1 (100M URLs):
  Collisions: Rare
  Avg retries: 1.0
  Avg latency: 37ms
  
Day 10 (1B URLs):
  Collisions: Common
  Avg retries: 2-3
  Avg latency: 75-100ms
  
Day 20 (2B URLs):
  Collisions: Very common
  Avg retries: 5-10
  Avg latency: 150-300ms
  
Day 30 (3B URLs):
  Collisions: Everywhere
  Avg retries: 20+
  Avg latency: 500ms+
  Database CPU: 100%!
  
Day 43 (4.3B URLs):
  All slots taken!
  No solution possible
  System CRASHES
```

---

## Algorithm 1: Hash + Collision Resolution ⚠️

**The Fix:** Use hashing, but when you get a collision, keep appending to the input and re-hashing until you find a free slot.

**Flow:**
```
Input: https://wikipedia.org/wiki/Systems_design
  ↓
Hash (MD5): 5eb63bbbe01eeed093cb22bb8f5acdc3
  ↓
Take first 7: "5eb63bb"
  ↓
Check DB: Already exists? YES → collision!
  ↓
Append counter "1": "5eb63bbbe01eeed093cb22bb8f5acdc31"
  ↓
Hash again: abc123... → "abc1234"
  ↓
Check DB: Already exists? NO → use it!
  ↓
Save to database ✓
```

### Pros
✅ Simple logic to understand  
✅ Works with any URL (no special ID system needed)  
✅ Can reuse existing hash libraries  
✅ Deterministic (same URL always gets same code if it exists)

### Cons
❌ **Database bottleneck** — every shortening request needs ≥1 DB lookup  
❌ **Multiple lookups** if collisions keep happening  
❌ **Index contention** under load (1000s of req/sec all checking the same index)  
❌ **Expensive** — collision detection is O(n) in worst case  
❌ **Doesn't scale** — 1K req/sec works, but 10K req/sec will cause DB CPU to spike to 100%

### When to Use
- Small hobby projects (<100 req/sec)
- Systems where collision resolution latency is acceptable
- When you have a really fast database and can optimize with Bloom filters

### Performance Impact
```
Normal load:    150-200ms per request
Peak load:      500ms-2s per request (timeouts!)
Success rate:   95% (some fail under contention)
Database CPU:   80-100% (bottleneck!)
```

**Example Pseudo-code:**
```python
def shorten_url_hash(long_url):
    hash_value = md5(long_url)
    short_url = hash_value[:7]
    counter = 0
    
    while db.exists(short_url):  # ← Database lookup on EVERY collision
        counter += 1
        new_input = hash_value + str(counter)
        hash_value = md5(new_input)
        short_url = hash_value[:7]
    
    db.save(short_url, long_url)
    return short_url
```

---

## Algorithm 2: Base 62 Conversion ⭐ (Recommended)

**The Complete Redesign:** Stop deriving the code from the URL. Instead:
1. Get a **pre-generated globally unique ID** (from a distributed ID generator, e.g., Snowflake)
2. Convert that ID to base-62 (same as converting decimal to hex)
3. Done — no collisions possible, no DB lookup needed

**Flow:**
```
Input: https://wikipedia.org/wiki/Systems_design
  ↓
Check DB: Does this long URL already exist? (if yes, return cached code)
  ↓
Call ID Generator: Get 2009215674938 (guaranteed globally unique)
  ↓
Convert to Base 62: 2009215674938 → "zn9edcu" (just math, O(log n))
  ↓
Save to database ✓
```

### How Base62 Solves CRC32's Problems

**The Problem vs Solution:**

```
CRC32 PROBLEMS:                    BASE62 SOLUTION:
─────────────────────────────────────────────────────
Multiple DB lookups              → No collision checks (0 DB queries!)
  (every collision = extra 10ms)

Database bottleneck              → No database involved in code generation
  (100% CPU by day 30)              (just math, <1ms)

Collisions from day 1            → Impossible collisions
  (cascading retries)               (unique ID = unique code)

Exhaustion in 43 days            → Lasts 111+ years
  (4.3 billion limit)               (3.5 trillion capacity)

Latency increases over time      → Constant latency always
  (37ms → 500ms+ over 43 days)      (always 1-5ms)

Success rate degrades            → 99.99%+ success always
  (95% → 50% over 43 days)          (no failures)
```

### Base62 Process Flow (Request → Response)

**URL Shortening Flow with Unique ID Generator:**

```
Client Request:
  POST /api/v1/data/shorten
  { "longUrl": "https://en.wikipedia.org/wiki/Systems_design" }
           ↓
   ┌──────────────────────────────────┐
   │   Load Balancer                  │
   │   Routes to Web Server           │
   └──────────────┬───────────────────┘
                  ↓
   ┌──────────────────────────────────┐
   │   Step 1: Check if exists        │
   │   Query: SELECT short_url        │
   │           WHERE long_url = ?     │
   │                                  │
   │   If YES: Return existing code   │
   │   If NO:  Proceed to Step 2      │
   └──────────────┬───────────────────┘
                  ↓
   ┌──────────────────────────────────────────┐
   │   Step 2: Get Unique ID                  │
   │   Call: id_generator.nextId()            │
   │   Returns: 2009215674938                 │
   │                                          │
   │   ✓ GUARANTEED UNIQUE!                  │
   │   ✓ No checking needed!                 │
   │   ✓ No collisions possible!             │
   └──────────────┬──────────────────────────┘
                  ↓
   ┌──────────────────────────────────────────┐
   │   Step 3: Convert to Base62              │
   │   short_url = base62_encode(2009215...)  │
   │   Result: "zn9edcu"                      │
   │                                          │
   │   ✓ Pure math (no DB!)                  │
   │   ✓ O(1) operation                      │
   │   ✓ 0.05ms latency                      │
   └──────────────┬──────────────────────────┘
                  ↓
   ┌──────────────────────────────────────────┐
   │   Step 4: Save to Database               │
   │   INSERT INTO url_mapping (              │
   │     id, short_url, long_url, ...         │
   │   )                                      │
   │                                          │
   │   ✓ Simple write (no collision checks!)  │
   └──────────────┬──────────────────────────┘
                  ↓
   ┌──────────────────────────────────────────┐
   │   Step 5: Return Response                │
   │   {                                      │
   │     "shortUrl": "tinyurl.com/zn9edcu"   │
   │   }                                      │
   └──────────────────────────────────────────┘
```

**Compare: CRC32 vs Base62 Timeline**

```
OPERATION                  CRC32              BASE62
───────────────────────────────────────────────────────
Load balancer routes       1ms                1ms
Dedup check (DB query)     10ms               10ms
Generate code:
  ├─ Hash (CRC32)          0.1ms              -
  ├─ Take first 7          0.1ms              -
  ├─ Call ID generator     -                  0.5ms
  ├─ Convert to Base62     -                  0.05ms
  └─ Collision check?      10ms + retries     0ms (NO COLLISION POSSIBLE!)

Save to database           15ms               15ms
Return response            1ms                1ms
───────────────────────────────────────────────────────
TOTAL (no collisions)      37ms               27ms ✓
TOTAL (1 collision)        57ms               27ms ✓ (3x faster!)
TOTAL (2 collisions)       77ms               27ms ✓ (4x faster!)
TOTAL (5 collisions)       137ms              27ms ✓ (5x faster!)
```

**Real-World Scenario: Same URL Shortened**

```
CRC32 Approach:
  Input: https://en.wikipedia.org/wiki/Systems_design
  
  Step 1: Hash: 5eb63bbbe01eeed093cb22bb8f5acdc3
  Step 2: Take first 7: "5eb63bb"
  Step 3: Check collision: FOUND! (another URL has this code)
  Step 4: Append counter, hash again: "abc1234"
  Step 5: Check collision: FOUND AGAIN!
  Step 6: Append counter, hash again: "xyz7890"
  Step 7: Check collision: Found!
  Step 8-10: Retry more times...
  
  Result: Multiple DB queries, 100-150ms latency ✗

Base62 Approach:
  Input: https://en.wikipedia.org/wiki/Systems_design
  
  Step 1: Get unique ID: 2009215674938
  Step 2: Convert to Base62: "zn9edcu"
  Step 3: Save to database
  
  Result: No collision checks, 27ms latency ✓
```

**Why Base62 Never Has Collisions:**

```
The Math:
  ID Generator: Produces 2009215674938 (UNIQUE by definition)
  Base62 conversion: Just changes how we write the number
  
  Example:
    2009215674938 in decimal = 2009215674938
    2009215674938 in base62 = "zn9edcu"
    
  These are the SAME number, just written differently!
  
  Result:
    Unique ID → Unique Base62 representation
    No collision possible! (it's just math)

Same ID = Same Base62 code (always!)
Different ID = Different Base62 code (always!)
```

### Performance Comparison: Day 30 Scenario

**CRC32 on Day 30 (3 billion URLs used):**

```
Collisions: Very common
Average retries per request: 20+
Database queries per request: 1 (dedup) + 20+ (collision checks) = 21+ queries

Timeline per request:
  Dedup check: 10ms
  Generate code with retries: 200ms (20 retries × 10ms each)
  Save to database: 15ms
  ───────────────────
  TOTAL: 225ms per request

Database CPU: 100% (maxed out!)
Success rate: 80% (20% timeout)
User experience: Slow and unreliable
```

**Base62 on Day 30 (3 billion URLs used):**

```
Collisions: ZERO (by design)
Average retries per request: 0
Database queries per request: 1 (dedup) + 0 (no collision checks) = 1 query

Timeline per request:
  Dedup check: 10ms
  Generate code: 0.5ms (ID generator) + 0.05ms (Base62) = 0.55ms
  Save to database: 15ms
  ────────────────────
  TOTAL: 25.5ms per request

Database CPU: 5-10% (minimal)
Success rate: 99.99% (no failures)
User experience: Fast and reliable ✓
```

**The Difference at Scale:**

```
CRC32 (Day 30):  225ms latency, 100% CPU, 80% success rate → BREAKING
Base62 (Day 30): 25ms latency, 10% CPU, 99.99% success rate → FINE

Ratio: Base62 is 9x FASTER! ⚡
```

**Why Base 62?**
```
We have 62 possible characters: 0-9 (10) + a-z (26) + A-Z (26)
Convert number to base-62 representation (like decimal → hex):

11157₁₀ = [2, 55, 59] → [2, T, X] → "2TX" in base 62

This is 1-to-1 and reversible: "2TX" ↔ 11157 always, forever.
```

### Pros
✅ **Zero collisions** — unique ID = unique short code (guaranteed by math)  
✅ **O(1) performance** — just division/remainder operations, no DB lookup  
✅ **Massive throughput** — no database lock contention  
✅ **Sequential IDs** — easier debugging and analytics  
✅ **Reversible** — can convert "zn9edcu" back to the original ID  
✅ **99.99% success rate** — no timeouts under load

### Cons
❌ **Depends on ID generator** — if the ID service is down, the whole system fails  
❌ **Non-meaningful codes** — "zn9edcu" looks random (some prefer "hash-like" appearance)  
❌ **ID generator complexity** — must implement or maintain a Snowflake/UUID service  
❌ **Slightly longer codes** — variable length (usually 6-7 chars) vs. fixed 7

### When to Use
✅ **Production default** (1K+ req/sec)  
✅ When you need massive scale (10K-100K+ req/sec)  
✅ When 99.99% success rate is non-negotiable  
✅ When you have or can build a reliable ID generator

### Performance Impact
```
Normal load:      1-5ms per request
Peak load:        1-5ms per request (unchanged!)
Success rate:     99.99%+
Database CPU:     5-10% (minimal, just storage writes)
ID Generator:     Critical dependency (single point of failure?)
```

**Example Pseudo-code:**
```python
def shorten_url_base62(long_url):
    # Check if already shortened (deduplication)
    existing = db.get_short_by_long(long_url)
    if existing:
        return existing
    
    # Get a globally unique ID (no collisions possible)
    unique_id = id_generator.nextId()  # e.g., 2009215674938
    
    # Convert to base 62 (pure math, O(1))
    short_url = base62_encode(unique_id)  # Returns "zn9edcu"
    
    # Save to database
    db.save(short_url, long_url, unique_id)
    return short_url

def base62_encode(num):
    chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    result = ""
    while num > 0:
        result = chars[num % 62] + result
        num //= 62
    return result or "0"
```

---

## Algorithm 3: Pre-Generation + Queue (CHUBB Approach) 🚀

**The Extreme Optimization:** For ultra-high-scale systems, pre-generate ALL possible codes offline, shuffle them randomly, load into a queue, and hand them out atomically with zero contention.

**When would you ever need this?**
- 100K+ codes/sec sustained
- Zero tolerance for any dependency (even ID generator)
- Systems where even 1-5ms latency matters

**Flow (Offline - One-time):**
```
1. Generate all 62^6 or 62^7 codes sequentially
   000000 → 000001 → ... → zzzzzz (56.8 billion or 3.5 trillion codes)
   Time: ~1-5 hours

2. Shuffle the entire file randomly
   $ sort -R codes.txt > shuffled-codes.txt
   Time: ~1-3 hours
   Result: Same codes, completely random order (unpredictable)

3. Load into Kafka (partitioned queue)
   Kafka Topic: url-shortener-codes
   Partitions: 10-100 (scale to load)
```

**Flow (Runtime - Forever):**
```
User shortens a URL
  ↓
Service: Bulk-read 100 codes from Kafka partition (1 Kafka call)
  ↓
Cache them in memory: [xYz9aB, 2kLmOp, Q7rWvX, ...]
  ↓
Hand out one code per request from cache
  ↓
When cache empty: Refill with next batch (100 more)
  ↓
Save URL to database ✓
```

### Pros
✅ **Atomic dequeue** — Kafka guarantees no race conditions  
✅ **Zero DB involvement** in code assignment (just storage)  
✅ **Completely unpredictable** — codes are shuffled, can't guess next one  
✅ **No live ID generator** — eliminates single point of failure  
✅ **Scales linearly** — add Kafka partitions = add throughput  
✅ **0.05ms latency** per code (bulk reads amortize Kafka calls)

### Cons
❌ **Huge one-time batch job** — generating/shuffling billions of codes takes hours  
❌ **Large storage** — 62^7 codes ≈ 3.5TB before compression  
❌ **Operational overhead** — must monitor exhaustion, trigger regeneration  
❌ **Code length upgrade** — when you run low, must expand to 7-8 chars (planned switchover)  
❌ **Overkill for most** — adds complexity that 99% of projects don't need  
❌ **Kafka dependency** — now you depend on Kafka instead of ID generator (not really a win)

### When to Use
- ⚡ Extreme scale (100K+ req/sec sustained)
- ⚡ Payment systems where "zero database contention" is non-negotiable
- ⚡ Referral codes, vouchers, session tokens (exact use case Chubb solves)
- ⚡ When you need to eliminate every possible live dependency

### Performance Comparison
```
Base 62 Conversion:          Pre-gen + Queue (Chubb):
Latency: 1-5ms               Latency: 0.05ms
Throughput: 10K+ req/sec     Throughput: 100K+ req/sec
DB involvement: Storage      DB involvement: Storage only
Dependencies: ID generator   Dependencies: Kafka + periodic batch job
Setup: 1 hour                Setup: 5-10 hours (offline batch)
```

---

## Algorithm Comparison Matrix

| Aspect | Hash + Collision | Base 62 Conv. | Pre-gen + Queue |
|--------|-----------------|---------------|-----------------|
| **Collision risk** | Possible | Impossible | Impossible |
| **Latency** | 150-500ms | 1-5ms | 0.05ms |
| **Throughput** | ~100 req/sec | 10K+ req/sec | 100K+ req/sec |
| **Database lookups** | Multiple per request | 0 (write only) | 0 (write only) |
| **Success rate** | 95% | 99.99%+ | 99.99%+ |
| **Code predictability** | Hash-based (somewhat random) | Sequential IDs (guessable) | Completely random |
| **Setup complexity** | Low | Medium | Very High |
| **Live dependencies** | Fast database | ID generator | Kafka + Batch |
| **Fits in memory** | Cache only | Fast | Bulk-load only |

---

## Recommendation by Scale

```
< 100 req/sec:         Hash + Collision (if you must)
100 - 10K req/sec:     Base 62 Conversion ⭐ (pick this)
10K - 100K req/sec:    Base 62 Conversion (still works great)
100K+ req/sec:         Base 62 Conversion OR Pre-gen + Queue
                       (Base 62 is simpler; Chubb only if you hit limits)
```

---

## Key Takeaways

**The evolution:**
1. **Hashing** works until you truncate and create collisions
2. **Collision resolution** works until database load becomes critical
3. **Base 62** avoids the DB lookup entirely by pre-guaranteeing uniqueness via math
4. **Pre-generation + Queue** takes Base 62's concept and eliminates even the ID generator dependency

**Interview answer:** 
> "I'd use **Base 62 conversion**. Get a unique ID from a distributed ID generator (like Snowflake), convert it mathematically to base 62, and save to the database. This eliminates collisions entirely, requires zero collision-checking DB lookups, and scales linearly. For extreme scale (100K+ req/sec), we could pre-generate and queue all codes, but that adds complexity most systems don't need."

**Real-world examples:**
- **Twitter:** Snowflake IDs + Base 62-like conversion
- **Bit.ly:** Base 62 conversion with deduplication
- **TinyURL:** Combination of hashing and collision resolution (older, slower approach)
- **Internal referral systems (Chubb approach):** Pre-gen + Kafka queue (extreme scale)

---

## Implementation Checklist

For Base 62 production system:
- [ ] Design/build distributed ID generator (Snowflake or UUID + shard)
- [ ] Implement base-62 conversion function (reversible)
- [ ] Add deduplication check (if long URL already shortened, return cached code)
- [ ] Cache layer (Redis) for hot URLs (read-heavy system)
- [ ] Database with master-slave replication for durability
- [ ] Rate limiting (prevent abuse of shortening API)
- [ ] Analytics tracking (which URLs are popular, click sources)
- [ ] 301 vs 302 trade-off decision (load reduction vs. analytics accuracy)
