# Complete Rate Limiter System Design - Consolidated Prompt

## Executive Summary

You need to design a rate limiter that limits API requests to prevent abuse, protect resources, and ensure fair usage. Choose the right algorithm based on your priorities: speed, accuracy, memory, or simplicity.

---

## Problem Statement

A **rate limiter** controls the rate of traffic by limiting the number of requests allowed within a specific time period. If requests exceed the threshold, excess calls are blocked.

### Why Rate Limiting Matters
1. **Prevent DoS Attacks** - Block malicious users from overwhelming the system
2. **Reduce Costs** - Limit usage of paid third-party APIs
3. **Ensure Fair Usage** - Protect from resource starvation by individual users
4. **Maintain System Stability** - Prevent cascading failures during traffic spikes

### Rate Limiting for BOTH Free AND Paid APIs
- Infrastructure costs are **non-linear** - each request costs money
- Without limits: One heavy user can cost 100X their subscription
- **Example**: Payment gateway customer sending unlimited requests costs $10K/month to process but pays only $99/month
- **Solution**: Tiered pricing with rate limits keeps everyone profitable

---

## Testing Scenarios (Use These for Comparison)

### Scenario A: Web API Rate Limiting
```
Limit: 10 requests per minute per user
Pattern: 3 requests at T=0s, then 6s quiet, then 9 requests at T=9s
Context: Public REST API, bursty traffic expected
```

### Scenario B: Payment Processing
```
Limit: 50 transactions per minute per merchant
Pattern: 30 transactions steady, sudden burst of 40 at peak hour
Context: Critical system, errors expensive, need high accuracy
```

### Scenario C: Email Service
```
Limit: 1000 emails per hour per account
Pattern: 500 emails sent, quiet period, then 800 attempt
Context: Background processing, steady rate preferred
```

---

## The 5 Algorithms Ranked by Use

### 🥇 1️⃣ Sliding Window Counter ⭐ (RECOMMENDED - 99% of cases)

**What It Does:**
```
Store only 3 things:
  1. How many requests in PREVIOUS 60-second period
  2. How many requests in CURRENT 60-second period
  3. When did CURRENT period start

Then ESTIMATE rolling window: estimate = curr + (prev × overlap%)
```

**How Overlap Works:**
```
At T=0:35 (35 seconds into current window):
  Rolling window covers last 60 seconds
  Previous window is 60% still relevant (36 seconds overlap)
  
  overlap = (window_start + 60 - now) / 60
          = (0:00 + 60 - 0:35) / 60
          = 25 / 60 = 0.417 (41.7%)

Estimate = 7 (current) + (20 × 0.417) = 15 requests
Decision: If 15 < limit → ACCEPT, else REJECT
```

**Pros:**
- ✅ Fast (O(1) per request)
- ✅ Memory efficient (24 bytes per user vs 2000+ for others)
- ✅ Accurate (99.997% - good enough)
- ✅ No edge case exploits
- ✅ Easy to update

**Cons:**
- ❌ Not 100% perfect accuracy (0.003% error)
- ❌ Assumes even distribution (real traffic is bursty)
- ❌ Edge cases with short/long windows

**When to Use:**
- Default choice for 99% of systems
- Twitter, GitHub, Google APIs use this
- Best balance of speed, cost, accuracy

**Redis Implementation:**
```python
def allow_request(user_id):
  now = current_timestamp()
  state = redis.GET(f"counter:{user_id}")
  if not state:
    state = "0|0|{now}"
  
  prev_count, curr_count, window_start = parse(state)
  
  if now - window_start >= 60:
    prev_count = curr_count
    curr_count = 1
    window_start = now
  else:
    overlap = (window_start + 60 - now) / 60
    estimated = curr_count + (prev_count * overlap)
    
    if estimated >= LIMIT:
      return False
    
    curr_count += 1
  
  new_state = f"{prev_count}|{curr_count}|{window_start}"
  redis.SETEX(f"counter:{user_id}", 60, new_state)
  return True
```

---

### 🥈 2️⃣ Token Bucket ⭐ (Use if bursts are OK)

**What It Does:**
```
A bucket that refills with tokens at a fixed rate

Request arrives:
  ├─ Has token available? → ACCEPT (consume token)
  └─ No token? → REJECT

Background job refills at 1 token per second
```

**Pros:**
- ✅ Handles burst traffic gracefully (reserves tokens during quiet)
- ✅ Simple to understand
- ✅ Low memory (8 bytes per user)
- ✅ Industry standard (Amazon, Stripe, Google)

**Cons:**
- ❌ Hard to tune (bucket size vs refill rate)
- ❌ Unpredictable bursts if misconfigured
- ❌ Different configs needed per endpoint

**When to Use:**
- When burst traffic is a feature, not a bug
- Real-time systems (video, chat)
- When users naturally send sporadic requests

**Real-World Case:**
- GitHub API: Allows 60 requests to be sent rapidly within the hour

---

### 3️⃣ Sliding Window Log (Use only if money is no object)

**What It Does:**
```
Store EVERY request timestamp in a sorted set

On each request:
  1. Remove timestamps older than 60 seconds
  2. Count remaining timestamps
  3. If count < limit → ACCEPT and add timestamp
  4. Else → REJECT
```

**Pros:**
- ✅ Perfect accuracy (no edge cases)
- ✅ True rolling window
- ✅ Impossible to exploit

**Cons:**
- ❌ Extremely expensive (2000+ bytes per user vs 24 for Counter!)
- ❌ Slow (O(n) per request vs O(1))
- ❌ Not scalable (breaks at 1M+ req/sec)
- ❌ Complex implementation

**Cost Analysis:**
```
1M users:
  Token Bucket: 8 MB total
  Sliding Counter: 24 MB total
  Sliding Window Log: 2 GB total → $1000/day in storage!
```

**When to Use:**
- Only payment systems where perfect accuracy > cost
- Only when compliance requires it
- Only for small traffic (< 10K req/sec)

---

### 4️⃣ Fixed Window Counter (Use only for non-critical)

**What It Does:**
```
Divide time into equal windows (1-minute buckets)
Count requests per window
When window ends: reset to 0
```

**The Critical Flaw:**
```
Can allow 2X limit at boundaries!

T=0:59: User sends 10 requests ✓
T=1:00: Window resets!
T=1:01: User sends 10 MORE requests ✓

In 2 seconds: 20 requests (2X the 10/minute limit!)
Discovery time: Only 5 minutes
Attacker gets permanent 6X advantage!
```

**Pros:**
- ✅ Simplest to implement (just 1 counter)
- ✅ O(1) response time
- ✅ 8 bytes per user

**Cons:**
- ❌ Can allow 2X limit at boundaries (MAJOR!)
- ❌ Vulnerable to timing attacks
- ❌ Not secure for sensitive operations

**When to Use:**
- ONLY for non-critical quotas
- ONLY for internal APIs
- ONLY when accuracy doesn't matter ("You viewed 100 articles today")

**NOT for:**
- Payment systems
- Security-critical operations
- APIs exposed to internet

---

### 5️⃣ Leaking Bucket (Use only for background jobs)

**What It Does:**
```
FIFO queue that drains at fixed rate

Request arrives:
  ├─ Queue not full? → Queue it (return immediately)
  ├─ Queue full? → REJECT
  
Background worker:
  Every 1 second: Remove and process 1 request from queue
```

**Pros:**
- ✅ Protects backend from spikes
- ✅ Predictable output rate
- ✅ Fair (FIFO order)

**Cons:**
- ❌ HIGH LATENCY (users wait 15-57 seconds!)
- ❌ Bad UX for web APIs
- ❌ Can't handle sudden spikes
- ❌ Requires background worker (complexity!)

**Real Cost Example:**
```
Email service with 0.28/sec outflow (1000/hour):
  500 emails arrive at T=0
  Queue holds only 100
  Last email processes at T=360 seconds = 6 HOURS! 😱
```

**When to Use:**
- Background jobs (email, logging, analytics)
- When steady processing rate is critical
- When latency doesn't matter

**NOT for:**
- Web APIs (too slow)
- Real-time systems (chat, trading)
- User-facing operations

---

## Algorithm Decision Matrix

```
┌──────────────────┬──────────┬────────────┬──────────┬─────────┐
│ Algorithm        │ Speed    │ Memory     │ Accuracy │ Secure? │
├──────────────────┼──────────┼────────────┼──────────┼─────────┤
│ Token Bucket     │ O(1) ✓   │ 8 bytes    │ 90%      │ Yes ✓   │
│ Leaking Bucket   │ O(1) ✓   │ 100 B/req  │ 100%     │ Yes ✓   │
│ Fixed Window     │ O(1) ✓   │ 8 bytes    │ 50%      │ No ✗✗   │
│ Sliding Log      │ O(n) ✗   │ 2000+ B    │ 100%     │ Yes ✓   │
│ Sliding Counter  │ O(1) ✓   │ 24 bytes   │ 99.997%  │ Yes ✓   │
└──────────────────┴──────────┴────────────┴──────────┴─────────┘

CHOOSE:
  Most systems → Sliding Window Counter (best balance)
  Bursts OK → Token Bucket
  Background jobs → Leaking Bucket
  Perfect accuracy only → Sliding Window Log
  Internal only → Fixed Window (but avoid!)
```

---

## Architecture Decisions

### Server-Side vs API Gateway

**Server-Side (in application code):**
```
Pros:
  ✅ Granular control (per endpoint, per user tier)
  ✅ Access to business logic
  ✅ Different limits for different endpoints

Cons:
  ❌ Must implement in every service (code duplication)
  ❌ Doesn't protect before expensive DB operations
  ❌ Hard to update (requires redeployment)
  ❌ No DDoS protection
```

**API Gateway (separate layer):**
```
Pros:
  ✅ Single point of control
  ✅ Protects before reaching expensive services
  ✅ DDoS protection
  ✅ Easy to update (instant, no deployment)
  ✅ Protects against internal bad actors

Cons:
  ❌ Less granular control
  ❌ Extra network hop (latency)
  ❌ Single point of failure
  ❌ Can't know business logic
```

**BEST PRACTICE: HYBRID**
```
Level 1: API Gateway
  ├─ Block obvious DDoS attacks
  ├─ IP-based limiting
  └─ 1000 req/sec per user (generous)

Level 2: Server-Side
  ├─ Business logic limits
  ├─ 100 req/sec for searches
  ├─ 5000 req/sec for reads
  └─ Different per user tier
```

---

## Rate Limit Headers

**Service-side calculates and sends:**

```
X-RateLimit-Limit: 300
  → Maximum requests allowed

X-RateLimit-Remaining: 287
  → How many left in current window

X-RateLimit-Reset: 1719072060
  → Unix timestamp when resets

X-Retry-After: 45
  → If rejected: wait 45 seconds before retry
```

**Client uses headers to:**
- Know remaining quota
- Implement smart backoff
- Avoid 429 surprises

---

## Storage: Why Redis?

```
Options:
  Database: ✗ Too slow (10-100ms per request)
  Memory cache: ✓ Fast but not distributed
  Redis: ✓ Perfect (in-memory + distributed)

Why Redis:
  ✅ In-memory (1µs operations)
  ✅ Atomic operations (INCR, EXPIRE)
  ✅ Distributed (shared across servers)
  ✅ Expiration support (auto-cleanup)
  ✅ Sorted sets for Window Log
  ✅ Built-in pub/sub for monitoring

Cost comparison (per user per month):
  Token Bucket: $0.000001 (8 bytes)
  Sliding Counter: $0.000003 (24 bytes)
  Sliding Log: $0.10+ (2000 bytes)
```

---

## Distributed System Challenges

### Challenge 1: Race Conditions
```
Problem:
  Request 1: GET counter → 9
  Request 2: GET counter → 9
  Request 1: INCR → 10
  Request 2: INCR → 10
  Both allowed! (should be 11, one rejected)

Solution: Lua scripts (atomic operations)
  redis.eval(script) runs without interruption
  Can't be interleaved with other requests
```

### Challenge 2: Multi-Region Sync
```
Problem:
  User in US: Requests to US data center
  User in EU: Requests to EU data center
  Same user can double their quota!

Solutions:
  1. Centralized Redis (single source of truth)
     - All DCs → One Redis
     - Consistent but higher latency
  
  2. Eventual consistency
     - Each DC has local cache (10 sec TTL)
     - Sync periodically
     - 0.1% overdraft acceptable
```

### Challenge 3: Cascading Failures
```
Problem:
  Millions of requests arrive
  All hit rate limiter
  Rate limiter becomes bottleneck

Solutions:
  1. Local caching (reduce Redis load)
  2. Batch operations (group updates)
  3. Multiple Redis instances (shard by user_id)
  4. Circuit breaker (bypass limiter if Redis down)
```

---

## Edge Cases & Gotchas

### Clock Skew
```
Problem: Servers have different times
  Server A: 12:00:00
  Server B: 11:59:50 (5 seconds behind)
  Window resets at different times!

Solution:
  Use NTP (Network Time Protocol)
  Accept 5-second skew in logic
  Use server timestamp from central authority
```

### Traffic Spikes
```
Problem: Flash sale or viral moment
  Normal: 100 req/sec
  Spike: 50,000 req/sec

Solution:
  Over-provision (limit should be 2X expected peak)
  Use Token Bucket for burst handling
  Circuit breaker for temporary overrides
  Alert and auto-scale
```

### User Identity Issues
```
Problem:
  Rate limit by IP? Shared corporate proxy blocks everyone
  Rate limit by user_id? Can't identify during auth

Solution: Use multiple keys
  Anonymous: IP-based
  Logged in: User ID based
  Admin: No limit
```

---

## Real-World Company Examples

| Company | Algorithm | Approach | Scale |
|---------|-----------|----------|-------|
| Twitter | Token Bucket → Sliding Counter | Burst-friendly, per tier | 10M DAU |
| GitHub | Sliding Window Counter | 60 public, 5K auth | Enterprise |
| Google | Token Bucket | Varies by service | Global |
| Stripe | Token Bucket | Per endpoint customization | Billions/day |
| Shopify | Leaking Bucket | Steady order processing | Millions |
| AWS | Sliding Window Counter | Per-service customization | Largest |
| CloudFlare | Sliding Window Counter | DDoS multi-layer | Billions req/day |

---

## Interview Q&A

### Design Questions

**Q1: Design rate limiter for Twitter (10M DAU)**
```
Answer approach:
1. Algorithm: Sliding Window Counter (best balance)
2. Storage: Redis (distributed)
3. Architecture: API Gateway + Server-side hybrid
4. Per-user: Use user_id as key
5. Per-tier: Different limits for free/paid
6. Distributed: Eventual consistency across DCs
7. Monitoring: Log all limit hits, alert on spikes
```

**Q2: Your API is using Fixed Window and getting exploited**
```
Solution: Switch to Sliding Window Counter
  ✓ Eliminates edge case exploitation
  ✓ Maintains O(1) performance
  ✓ Minimal cost increase
  ✓ Easy migration
  
Migration: Canary 10% → 25% → 50% → 100%
```

**Q3: Rate limiter uses 500GB memory, optimize**
```
Current: Probably Sliding Window Log (2000 bytes/user)

Solution: Switch to Sliding Window Counter
  500GB / 2000 bytes × 24 bytes = 6GB (83X reduction!)
```

**Q4: Distributed rate limiting across 3 data centers**
```
Challenges:
  1. Race conditions: Use Lua scripts
  2. Sync: Use centralized Redis or eventual consistency
  3. Failover: Circuit breaker if Redis down

Best: Redis cluster + Lua + eventual consistency
```

### Problem-Solving

**Q5: How to handle different limits per endpoint?**
```
Design:
  Multiple rate limiters per user
  Different limits for different endpoints
  
  user:123:timeline → 300/hour
  user:123:search → 30/minute
  user:123:post → 10/minute
  
Redis keys:
  limit:{user_id}:timeline
  limit:{user_id}:search
  limit:{user_id}:post
```

**Q6: How to migrate from Token Bucket to Sliding Window Counter?**
```
Strategy:
1. Deploy Sliding Counter alongside Token Bucket
2. Canary 10% traffic
3. Gradual rollout: 10% → 25% → 50% → 100%
4. Monitor accuracy
5. Easy fallback if needed

Timeline: 2-4 weeks for safe migration
```

### Trade-off Questions

**Q7: Why not always use Sliding Window Log?**
```
Trade-offs:
  ✓ Pros: Perfect accuracy
  ✗ Cons:
    - 125X more memory
    - 100X slower
    - Doesn't scale
    - Complex to implement

Answer: 99.997% accuracy is good enough!
        Sliding Counter is "good enough" 99% of the time
```

**Q8: Token Bucket vs Sliding Window Counter?**
```
Token Bucket:
  ✓ Burst handling (feature, not bug)
  ✓ Real-time responsiveness
  Example: Video streaming

Sliding Counter:
  ✓ Steady, predictable behavior
  ✓ No burst spikes
  Example: Payment APIs

Default: Sliding Counter (safe choice)
```

---

## Implementation Checklist

**Pre-Deployment:**
- [ ] Choose algorithm based on requirements
- [ ] Estimate storage needs (multiply by 10)
- [ ] Design Redis schema
- [ ] Write Lua scripts for atomicity
- [ ] Plan for clock skew (NTP)
- [ ] Design monitoring/alerts
- [ ] Plan for cascade failures

**Deployment:**
- [ ] Deploy to canary (10%)
- [ ] Monitor error rates
- [ ] Check CPU/memory usage
- [ ] Verify rate limit headers
- [ ] Test client retry logic
- [ ] Load test (1K, 10K, 100K req/sec)

**Post-Deployment:**
- [ ] Monitor rejection rate
- [ ] Alert on anomalies
- [ ] Log all violations
- [ ] Track customer complaints
- [ ] Plan upgrades/optimizations
- [ ] Document limits per tier

---

## Quick Decision Tree

```
Start here:

1. What's your traffic?
   < 100 req/sec → Any algorithm works
   > 10K req/sec → Need Sliding Counter or Token Bucket

2. Do you care about burst?
   Yes → Token Bucket
   No → Sliding Counter

3. Do you need perfect accuracy?
   Yes (payment) → Sliding Log (pay the cost)
   No → Sliding Counter

4. Are you in a hurry?
   Yes → Fixed Window (but accept risks!)
   No → Sliding Counter

5. Where to place?
   Multiple services → API Gateway + Server-side
   Single service → Server-side only
   High security → Both layers

DECISION: ___________________________
(Usually: Sliding Window Counter)
```

---

## Summary: Recommended Stack

```
Algorithm:        Sliding Window Counter
Storage:          Redis (cluster for HA)
Placement:        API Gateway (DDoS) + Server-side (business logic)
Replication:      Eventual consistency (acceptable 0.1% overdraft)
Atomicity:        Lua scripts for race condition prevention
Monitoring:       Alert on rejection rate > 5%
Migration:        Canary rollout (10%, 25%, 50%, 100%)
Backup:           Circuit breaker if Redis down (allow some overflow)

This setup:
  ✅ Handles 1M+ req/sec
  ✅ Cost-efficient (24 bytes/user)
  ✅ Fast (O(1) per request)
  ✅ Accurate (99.997%)
  ✅ Secure (no exploitable edge cases)
  ✅ Easy to operate
```

---

## References

- System Design Interview by Alex Xu
- Rate Limiting Strategies (Google Cloud)
- Stripe Rate Limiters
- Redis Sorted Sets for Rate Limiting
- Cloudflare Rate Limiting
- Twitter API Documentation
