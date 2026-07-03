# URL Shortener Algorithm Selection Guide

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
