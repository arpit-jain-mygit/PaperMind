# System Design Interview - Chapter 4: Design a Rate Limiter

## Table of Contents

### Core Concepts
1. [Problem Statement](#problem-statement)
   - [Real-World Examples](#real-world-examples)
   - [Why Rate Limiting Matters](#why-rate-limiting-matters)
   - [Rate Limiting for BOTH Free AND Paid APIs](#rate-limiting-for-both-free-and-paid-apis)
2. [Testing Scenarios](#testing-scenarios)
   - [Scenario A: Web API Rate Limiting](#scenario-a-web-api-rate-limiting)
   - [Scenario B: Payment Processing](#scenario-b-payment-processing)
   - [Scenario C: Email Service](#scenario-c-email-service)

### Algorithm Deep Dive
3. [Algorithm Comparison](#algorithm-comparison)
   - [1️⃣ Token Bucket Algorithm](#️-token-bucket-algorithm-)
     * [How It Works](#how-it-works)
     * [Using Scenario A](#using-scenario-a-web-api)
     * [Advantages](#advantages)
     * [Limitations](#limitations)
     * [When to Use](#when-to-use)
     * [Real-World Case: GitHub API](#real-world-case-github-api)
   - [2️⃣ Leaking Bucket Algorithm](#️-leaking-bucket-algorithm)
     * [How It Works](#how-it-works-1)
     * [Problems It Solves](#problems-it-solves-from-token-bucket)
     * [Using Scenario C](#using-scenario-c-email-service)
     * [Advantages](#advantages-1)
     * [Limitations](#limitations-1)
     * [When to Use](#when-to-use-1)
     * [Real-World Case: Shopify](#real-world-case-shopify)
   - [3️⃣ Fixed Window Counter](#️-fixed-window-counter)
     * [How It Works](#how-it-works-2)
     * [Problems It Solves](#problems-it-solves)
     * [Using Scenario A](#using-scenario-a-web-api-1)
     * [Advantages](#advantages-2)
     * [Limitations](#limitations-2)
     * [When to Use](#when-to-use-2)
     * [Real-World Case: NOT RECOMMENDED](#real-world-case-not-recommended)
     * [Security: How Attackers Exploit This](#security-how-attackers-exploit-this)
   - [4️⃣ Sliding Window Log](#️-sliding-window-log)
     * [How It Works](#how-it-works-3)
     * [Problems It Solves](#problems-it-solves-from-fixed-window)
     * [Using Scenario B](#using-scenario-b-payment-processing)
     * [Advantages](#advantages-3)
     * [Limitations](#limitations-3)
     * [Storage Reality](#storage-reality)
     * [When to Use](#when-to-use-3)
     * [Real-World Case: Payment Systems](#real-world-case-payment-systems)
   - [5️⃣ Sliding Window Counter](#️-sliding-window-counter-)
     * [How It Works](#how-it-works-4)
     * [Problems It Solves](#problems-it-solves-1)
     * [Using Scenario A](#using-scenario-a-web-api-2)
     * [Advantages](#advantages-4)
     * [Limitations](#limitations-4)
     * [Storage Breakdown](#storage-breakdown)
     * [When to Use](#when-to-use-4)
     * [Real-World Case: Most Companies](#real-world-case-most-companies)
   - [Side-by-Side Using Scenario A](#side-by-side-using-scenario-a)
   - [Decision Matrix](#decision-matrix)

### Interview Preparation
4. [Interview Q&A](#interview-qa)
   - [Design Questions](#design-questions)
   - [Problem-Solving Questions](#problem-solving-questions)
   - [Trade-off Questions](#trade-off-questions)

### Implementation & Operations
5. [Implementation Guide](#implementation-guide)
   - [Choosing Where to Place Rate Limiter](#choosing-where-to-place-rate-limiter)
   - [Storage: Why Redis?](#storage-why-redis)
   - [Rate Limit Headers](#rate-limit-headers)
   - [Client Retry Strategy](#client-retry-strategy)

6. [Distributed Systems Challenges](#distributed-systems-challenges)
   - [Challenge 1: Race Conditions](#challenge-1-race-conditions)
   - [Challenge 2: Synchronization Across Data Centers](#challenge-2-synchronization-across-data-centers)
   - [Challenge 3: Eventual Consistency](#challenge-3-eventual-consistency)

7. [Edge Cases & Gotchas](#edge-cases--gotchas)
   - [Edge Case 1: Clock Skew](#edge-case-1-clock-skew)
   - [Edge Case 2: Sudden Traffic Spike](#edge-case-2-sudden-traffic-spike)
   - [Edge Case 3: User Identity Issues](#edge-case-3-user-identity-issues)

### Reference
8. [Real-World Companies & Their Approaches](#real-world-companies--their-approaches)
9. [Summary: Quick Reference](#summary-quick-reference)
10. [References](#references)

---

## Problem Statement

A **rate limiter** controls the rate of traffic by limiting the number of requests allowed within a specific time period. If requests exceed the threshold, excess calls are blocked.

### Real-World Examples
- **Twitter**: 300 tweets per 3 hours
- **GitHub API**: 60 API calls per hour (unauthenticated)
- **Google Docs**: 300 per user per 60 seconds (read requests)

### Why Rate Limiting Matters

1. **Prevent DoS Attacks** - Block malicious users from overwhelming the system
2. **Reduce Costs** - Limit usage of paid third-party APIs
3. **Ensure Fair Usage** - Protect from resource starvation by individual users
4. **Maintain System Stability** - Prevent cascading failures during traffic spikes

### Rate Limiting for BOTH Free AND Paid APIs

**Counterintuitive Truth:** Rate limiting is required even for paid APIs, not just free ones.

**Why restrict paid APIs when more requests = more revenue?**

```
Infrastructure costs are NON-LINEAR:

Payment Gateway Example:
  Free tier:     10 req/min, cost = $0.01 per request
  Paid tier:     "Unlimited" (no limits), customer sends 1M req/month
  
  Revenue: Customer pays $99/month
  Cost: 1M × $0.01 = $10,000/month infrastructure
  
  Result: Company LOSES $9,901/month on one customer! 🚨

Without rate limits: Company goes bankrupt
With rate limits: Each customer costs predictable amount
```

**Real-World Example: Stripe**
```
Stripe charges $0.01-0.05 per request for payment processing.
One customer sending unlimited requests would cost Stripe 
$500,000/month to serve, but pays only $99/month subscription.

Solution: Tiered pricing with rate limits
  Starter:    $25/month  → 1,000 req/min
  Business:   $100/month → 10,000 req/min
  Enterprise: Custom     → Custom limits at custom price

Heavy users PAY MORE, protecting the system and profitability.
```

**The Business Math:**
- Each API request costs money (processing, storage, bandwidth)
- One customer can monopolize shared resources
- Without limits: 1 heavy user breaks SLA for all other customers
- With limits: All customers get guaranteed uptime (SLA) + profit for company

**Interview Insight:** Rate limiting enables tiered pricing and sustainable profitability. It's not about "preventing users" - it's about "making the service sustainable and profitable."

---

## Testing Scenarios

We'll use **three consistent scenarios** to compare all algorithms:

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

## Algorithm Comparison

### 1️⃣ Token Bucket Algorithm ⭐

#### How It Works

```
Concept: A bucket that refills with tokens at a fixed rate

Bucket capacity: 10 tokens
Refill rate: 1 token per second

Each request costs 1 token
- Token available? REQUEST ACCEPTED ✓
- No token? REQUEST REJECTED ✗
```

#### Using Scenario A (Web API)

```
Configuration: Bucket size = 10, Refill = 1 per second

T=0s:    3 requests arrive
         Tokens: 10 → 9 → 8 → 7
         Status: ✓ All accepted

T=1-6s:  Quiet period
         Tokens: 7 → 8 → 9 → 10 (refill during quiet)
         Status: Building reserve

T=9s:    9 requests arrive (burst!)
         Tokens: 10 available
         Requests 1-8: ✓ Accepted (8 tokens used)
         Request 9: ✗ Rejected (no tokens left)
         
Result: 8/9 accepted (89% success rate)
Latency: ~0ms (instant)
Memory: O(1) - just 1 counter
```

#### Advantages
✅ Handles burst traffic gracefully (reserves tokens during quiet)
✅ Simple to implement and understand
✅ Low memory footprint
✅ Industry standard (Amazon, Stripe, Google)

#### Limitations

**❌ Hard to Tune Parameters (Bucket Size vs Refill Rate)**

```
Problem: Two interdependent parameters, no "right" answer

Example 1: Too Large Bucket (Bucket=100, Refill=1/sec)
  Advertise: "100 requests per minute"
  Reality: User can burst 100 requests in 1 second!
  Impact: Backend gets hammered
  
  T=0s:   User sends 100 requests
          All accepted immediately (all tokens consumed)
          Database: 100 simultaneous writes! 😱
  T=1s:   System recovers, 1 new token added
  
  User experience: Worked, but infrastructure suffered

Example 2: Too Small Bucket (Bucket=1, Refill=1/sec)
  Advertise: "1 request per second"
  Reality: User can't burst at all
  Impact: Legitimate bursts get rejected
  
  User scenario: Checkout flow sends 10 requests
    - Request 1: ✓ Accepted
    - Request 2-10: ✗ Rejected
  Result: Checkout fails! Lost customer! 💥

How to tune?
  - Too large: Overload backend
  - Too small: Reject legitimate traffic
  - No formula: Trial and error, monitoring, adjust
```

**❌ Unpredictable Bursts If Misconfigured**

```
Real-world case: Twitter's burst handling gone wrong

Configuration: Bucket=300, Refill=1 per 3 hours
  Goal: 300 tweets per 3 hours

User behavior:
  T=0:    User tweets 300 times (uses all tokens!)
  T=1:    System slows down, other users affected
  T=2hr:  Still waiting for 1 token to be added (3hr timer)
  T=3hr:  Finally, 300 more tokens added

Problem: All traffic is bursty!
  - Desktop users: Tweet sporadically (long quiet, then burst)
  - Mobile users: Scheduled posts (batched at midnight)
  - Influencers: Reply storms (single moment, many posts)

Mismatched configuration causes:
  - Some users exhaust quota immediately
  - Others get better experience (spread out over time)
  - Unfair burst advantage
```

**❌ Different Configurations Needed Per Use Case**

```
Real problem: One size doesn't fit all

API Example: Social media with multiple endpoints

Timeline endpoint:
  Limit: 300 requests per 60 seconds (reading timeline)
  Bucket: 300 tokens, Refill: 5/sec
  Pattern: Users scroll fast, many rapid requests

Post creation endpoint:
  Limit: 10 posts per hour
  Bucket: 10 tokens, Refill: 1 per 360 seconds
  Pattern: Users think between posts, slow

Search endpoint:
  Limit: 30 searches per minute
  Bucket: 30 tokens, Refill: 0.5/sec
  Pattern: Users explore, moderate burst

Reality at 3:00 AM:
  - Night shift developers testing API
  - Legitimate heavy usage pattern
  - All 3 endpoints need different tuning!

Maintenance nightmare:
  Every new endpoint = new configuration
  Customer complaint: "Your limit is too low"
  → Need to adjust bucket for that endpoint
  → Might affect others
  → Cascading problems
```

#### When to Use
- **Best for**: Public APIs, web services, real-time systems
- **Examples**: Twitter API, GitHub API, Google Cloud APIs
- **Key requirement**: Burst traffic acceptable, low latency critical

#### Real-World Case: GitHub API
```
GitHub uses Token Bucket for rate limiting:
- 60 requests per hour (unauthenticated)
- 5000 requests per hour (authenticated)
- Allows bursts: Can send 60 req rapidly within the hour
```

---

### 2️⃣ Leaking Bucket Algorithm

#### How It Works

```
Concept: FIFO queue that drains at fixed rate

Queue capacity: 10 requests
Outflow rate: 1 request per 6 seconds

Process: Request added to queue if not full
         Requests processed at steady rate
```

#### Problems It Solves from Token Bucket
❌ Token Bucket: Unpredictable bursts can happen
✅ Leaking Bucket: **Guaranteed steady processing rate**

❌ Token Bucket: Hard to tune parameters
✅ Leaking Bucket: **Simple - just set queue size and rate**

#### Using Scenario C (Email Service)

```
Configuration: 
  Limit: 1000 emails/hour = 0.28 emails/sec (1 every 3.6 seconds)
  Queue capacity: 100 emails
  Outflow rate: 0.28/sec

T=0s:    500 emails arrive suddenly
         Queue capacity: 100 max
         Accepted: 100 (fill queue)
         Rejected: 400 ✗ (LOST - client must retry elsewhere)
         Queue: [E1, E2, ..., E100]

T=360s:  First email processed (1 ÷ 0.28/sec = 3.6 seconds)
         Queue: [E2, E3, ..., E100, E101] (if more arrived)
         But NOTHING new arrived (quiet period)
         Queue: [E2, E3, ..., E100]

T=720s:  Second email processed
         Queue: [E3, E4, ..., E100]

T=36,000s: All 100 emails finally processed!
           (100 emails ÷ 0.28/sec ≈ 357 minutes = 6 hours!)

T=36,100s: Burst of 800 emails arrives
           Queue is now empty
           Accepted: 100 (fill queue)
           Rejected: 700 ✗
           
Wait time: Up to 6 HOURS for last email in queue! 😱
Result: Leaking Bucket is TERRIBLE for email!
Memory: O(1) - just queue size
```

#### Advantages
✅ Predictable, constant output rate
✅ Protects backend from spikes
✅ Memory efficient
✅ Fair processing (FIFO order)

#### Limitations

**❌ High Latency - Requests Wait in Queue**

```
Real problem: Every request gets delayed

Example: Email API with 100 emails/sec limit, but 500 arrive

T=0s:   500 emails arrive
        Queue: [E1, E2, E3, ..., E500]
        Capacity: 100 max
        
T=1s:   100 processed, 400 remaining
        E1-E100 done (waited 1 second)
        E101-E500 still waiting
        
T=5s:   E101-E500 still waiting
        Progress: 500 processed so far
        Remaining: 400
        
T=10s:  E1-E500 done
        E501 onward processed
        Last email waited 10 seconds! 😱

Real impact: Email sent 10 seconds late
  - User thinks email didn't send
  - Resends it (duplicate email!)
  - Company reputation: "Broken API"

Scenario where this kills business:
  E-commerce checkout flow
  T=0: Customer submits order (5 requests needed)
  T=3: Queue processing, customer still waiting
  T=5: Customer closes browser (checkout timeout)
  Result: Lost sale! 💥
```

**❌ Poor User Experience for Web APIs**

```
What users expect: Response in <100ms
What Leaking Bucket gives: Response in 5-30 seconds

Real-world example: GitHub API using Leaking Bucket

User workflow:
  T=0s:   GET /user → ✓ Accepted, queued
          Waiting...
  T=6s:   Response arrives (was in queue for 6 seconds)
          User: "Why so slow?" 😞
          
  T=6s:   GET /repos → ✓ Accepted, queued
  T=12s:  Response arrives
          User: "This API is broken!"
          
  T=12s:  User switches to Stripe API (faster)
          GitHub loses customer 📉

Comparison:
  Token Bucket: Response in ~5ms (instant)
  Leaking Bucket: Response in ~6s (queue wait)
  
  60X slower = bad UX = lost customers
```

**❌ Not Suitable for Real-Time Systems**

```
Real-time requirements: <100ms response time

Examples that fail with Leaking Bucket:

1. Chat application
   User types message: "Hello world"
   Expected: Appears instantly to other users
   With Leaking Bucket: Appears 5 seconds later
   User: "Why is my message delayed?"
   Problem: Looks like message didn't send
   Result: User resends, duplicate messages

2. Stock trading API
   User clicks "BUY 100 shares"
   Expected: Order placed immediately
   With Leaking Bucket: Queued for 10 seconds
   Market moves in those 10 seconds
   Order placed at wrong price
   Trader loses money 💸

3. Multiplayer game
   User moves character: "Jump"
   Expected: Character jumps immediately
   With Leaking Bucket: Jump delayed 2 seconds
   Player gets shot (they moved too late)
   Player quits game 😤
```

**❌ Can't Handle Sudden Spikes Gracefully**

```
Problem: All requests get delayed equally, no prioritization

Scenario: Email service during marketing campaign

Normal load: 100 emails/sec
Suddenly: 1000 emails/sec (10X spike!)

With Leaking Bucket:
  Queue fills instantly (capacity: 100)
  Remaining 900: Rejected ✗
  
  Priority: Regular emails wait in queue
            High-priority emails also wait in queue
            No way to prioritize
            
  Impact: Transactional emails (password resets) delayed
          Marketing emails delayed
          No difference!
          
  Better approach: Marketing emails rejected, transactional go through
                  But Leaking Bucket can't distinguish

Real cost:
  Password reset email delayed 15 minutes
  User can't access account
  Support tickets 📞
  Customer churn 📉
```

#### When to Use
- **Best for**: Background jobs, batch processing, database protection
- **Examples**: Email queuing systems, Shopify API, job schedulers
- **Key requirement**: Steady processing rate more important than latency

#### Real-World Case: Shopify
```
Shopify uses Leaking Bucket approach:
- Maintains steady load on database
- Processes orders at predictable rate
- Accepts offline orders into queue
- Processes them when capacity available
```

---

### 3️⃣ Fixed Window Counter

#### How It Works

```
Concept: Divide time into equal windows, count requests per window

Window size: 1 minute
Limit: 10 requests per window

When window ends: Counter resets to 0
```

#### Problems It Solves
❌ Token Bucket: Parameters hard to tune
✅ Fixed Window: **Simple - just set window size and limit**

❌ Leaking Bucket: High latency issues
✅ Fixed Window: **O(1) response time, no queueing**

#### Problems It Creates
⚠️ **CRITICAL FLAW: Exploitable edge cases!**

#### Using Scenario A (Web API)

```
Configuration: Window = 1 minute, Limit = 10 requests

Normal usage:
T=0:00-0:01:  10 requests accepted, then rejected
T=1:00-1:01:  Fresh window, 10 more accepted

EXPLOIT at edge case:
T=0:59:       User sends 10 requests ✓
              Counter: 0 → 10/10

T=1:00:       WINDOW RESETS!

T=1:01:       User sends 10 MORE requests ✓
              Counter: 0 → 10/10

In actual time [0:59-1:01] = 2 minutes:
  Sent 20 requests! (2X the limit of 10/minute)
  
Result: EXPLOITED! 🚨
```

#### Advantages
✅ Extremely simple to implement
✅ O(1) time per request
✅ O(1) memory - just 1 counter

#### Limitations

**❌ Can Allow 2X Limit at Window Boundaries (Exploitable!)**

```
Real exploitation example: GitHub API

GitHub limit: 60 requests per hour

Attacker discovers window boundary:
  T=59:50 (10 seconds before window resets)
  Sends 60 requests: ✓ All accepted
  Counter: 0 → 60/60 (full)
  
  (Wait 10 seconds)
  
  T=60:00 (Window resets!)
  Counter suddenly: 60 → 0
  
  Sends 60 MORE requests: ✓ All accepted
  Counter: 0 → 60/60
  
Result in 20 seconds: 120 requests processed!
But limit is 60/hour!

Impact: In 1 hour, attacker makes 60×6=360 requests
        Legitimate limit: 60 requests
        Attacker gets: 6X the limit! 🚨

What attacker can do:
  - Scrape entire API (6X faster)
  - DDoS single endpoint
  - Brute force password (6X more attempts)
  - Enumerate users/data (6X larger dataset)
```

**❌ Vulnerable to Timing Attacks**

```
Real-world discovery process: Takes only 5 minutes

Step 1: Discover window size (2 minutes)
  Send request at T=0:00 → ✓ Accepted
  Send request at T=0:01 → ✓ Accepted
  Send request at T=1:00 → ✓ Accepted (WINDOW RESET!)
  Discovery: 1-minute windows

Step 2: Discover window alignment (3 minutes)
  Send requests at T=0:59 → ✓ Accepted
  Send requests at T=1:00 → ✓ Accepted (MORE ACCEPTED!)
  Discovery: Reset happens at :00 mark

Step 3: Exploit (trivial)
  Set up bot that sends at T=59:55
  Set up bot that sends at T=0:05
  Done! 6X rate limit achieved

Real-world impact:
  Attacker spends 5 minutes, gets permanent 6X advantage
  Can't be detected (no pattern analysis needed)
  Can't be stopped without algorithm change
```

**❌ Not Suitable for Strict Rate Limiting**

```
Use case where this catastrophically fails: Payment processing

Fixed Window Counter on merchant API:
  Limit: 10 transactions per minute

T=0:59:55
  Merchant processes 10 transactions
  Counter: 0 → 10/10 (limit reached)

T=1:00:00 (window resets!)
  Merchant processes 10 MORE transactions
  Counter: 0 → 10/10

In 5 seconds: 20 transactions!
Expected: 10 per minute
Actual: 20 per minute (2X)

Compliance issue:
  - Merchant agreement: "10 per minute"
  - Actual processed: 20 per minute
  - Auditor finds: Limit violated! 📋
  - Regulatory penalty: $5000+
  - Merchant churn: "Your system doesn't work"

This is why payment systems DON'T use Fixed Window
```

**❌ Catastrophic for Critical Systems**

```
Real case study: Payment fraud detection

Fraudster discovery: Fixed Window vulnerable
  Limit: 50 fraud checks per minute (per API)
  
  T=0:59:50
    Sends 50 fraudulent cards: ✓ All checked
    
  T=1:00:00 (reset!)
    Sends 50 MORE fraudulent cards: ✓ All checked
    
  In 10 seconds: 100 fraudulent attempts
  
Impact:
  - 100 stolen credit cards tested
  - 90 succeed (fraud)
  - $100K+ fraud loss
  - Company liable
  - Bank sues company 💸

Root cause: Weak rate limiting
Prevention: Use Sliding Window Counter or Log
```

#### When to Use
- **Only for**: Non-critical quotas, internal APIs, informational limits
- **Examples**: "You viewed 100 articles today", "Free tier: 1000/month"
- **Key requirement**: Accuracy not critical, simplicity important

#### Real-World Case: NOT RECOMMENDED
```
Twitter initially used Fixed Window
Attackers exploited edge cases:
- Sent requests at 14:59:00
- Sent more requests at 15:00:00
- Achieved 2X rate limit in 60 seconds

Twitter switched to Sliding Window implementation
```

#### Security: How Attackers Exploit This

```
1. DISCOVERY (5 minutes):
   Send requests, observe when 429 errors stop appearing
   → Identify window boundary (e.g., every minute at :00)

2. EXPLOITATION (trivial):
   T=59:50: Send 10 requests (last 10 sec of window)
   T=60:00: Window resets!
   T=60:10: Send 10 more requests (first 10 sec of new window)
   
3. RESULT:
   In 20 seconds, sent 20 requests (2X limit!)
   Can repeat every minute for sustained attack

4. IMPACT:
   - API scraping: 4X faster
   - DDoS amplification: 2X attack power
   - Fraud: 2X password attempts
```

---

### 4️⃣ Sliding Window Log

#### How It Works

```
Concept: Keep timestamp of every request in rolling window

Store: All request timestamps (usually in Redis sorted set)
Check: Count timestamps in [now-60s, now]
Limit: If count < limit, accept and add timestamp
```

#### Problems It Solves from Fixed Window
❌ Fixed Window: Exploitable at boundaries
✅ Sliding Window Log: **No fixed boundaries, perfect accuracy!**

❌ Fixed Window: Can allow 2X limit
✅ Sliding Window Log: **Impossible to exceed limit in any rolling window**

#### Using Scenario B (Payment Processing)

```
Configuration: Window = 1 minute, Limit = 50 transactions

T=0:00:   Transaction T1 arrives
          Log: [0:00:00]
          Window [59:00-0:00]: 1 transaction ✓ Accept

T=0:30:   30 transactions arrive
          Log: [0:00, 0:30, 0:30, ...]
          Window [59:30-0:30]: 31 transactions < 50 ✓ Accept all

T=0:59:   19 more arrive
          Log: [0:00, 0:30x30, 0:59x19]
          Window [59:59-0:59]: 50 transactions = 50 ✓ Accept all

T=0:59:55: Burst of 40 arrives
          Log: [0:00, 0:30x30, 0:59x19, 0:59:55x40]
          Window [59:55-0:55]: 99 transactions >= 50 ✗ Reject 40

T=1:00:05: Some transactions now outside window
          Remove timestamps < 1:00:05
          Log now: [0:59x19, 0:59:55x40]
          Window [0:05-1:05]: 59 transactions >= 50 ✗ Still reject

Result: Burst rejected until oldest transactions expire
Memory: 100+ timestamps stored per user (expensive!)
```

#### Advantages
✅ **Perfect accuracy** - No edge cases, no exploits
✅ True rolling window - Respects actual time
✅ Secure - Impossible to game the system
✅ Fair - Everyone gets exactly their quota

#### Limitations

**❌ Extreme Memory Cost - 125X More Than Token Bucket**

```
Real-world math: Payment API with 1M users

Token Bucket:
  Per user: 1 counter (8 bytes)
  1M users: 1M × 8 = 8 MB total
  Daily cost: ~$0.0001

Sliding Window Log:
  Per user: 100 timestamps per minute × 20 bytes
  1M users: 1M × 100 × 20 = 2 GB per minute
  Per hour: 120 GB
  Per day: 2.88 TB
  
  Daily cloud storage cost: ~$1,000-$5,000 just for memory! 💸

Scale impact:
  Startup: 100K users
    Memory needed: 200 GB
    Cost: $500-$1000/day
    Entire engineering budget gone on rate limiting! 😱
    
  Mid-scale: 10M users
    Memory needed: 20 TB
    Cost: $50,000-$100,000/day
    More than employee salary budget!
    
  Enterprise: 100M users
    Memory needed: 200 TB
    Cost: $500K-$1M/day
    Bigger than entire ops budget!
```

**❌ Extremely Slow - O(n) Per Request**

```
Real impact: Sliding Window Log is 100X slower

Example: API with 100 requests in window

Per request operation:

Token Bucket:
  Time: 1. Check token available: 1μs
        Total: 1μs

Sliding Window Log:
  Time: 1. Scan all timestamps: 100 × 1μs = 100μs
        2. Remove old: 100 × 1μs = 100μs
        3. Count: 100 × 1μs = 100μs
        Total: 300μs
  
  300X slower! 🚨

Impact at scale:

1000 requests/sec arriving:
  Token Bucket: 1000 × 1μs = 1ms overhead
  Sliding Log: 1000 × 300μs = 300ms overhead per second!
  
  That's 30% of CPU just for rate limiting!
  
At 1M requests/sec:
  Token Bucket: ~1ms overhead
  Sliding Log: 300 seconds per second (IMPOSSIBLE!)
  Server would need 300 concurrent processors just for rate limiting!
```

**❌ Not Scalable - Fails at High Traffic**

```
Real-world breakdown points:

Low traffic (100 req/sec):
  Sliding Log memory: 6 MB/day
  Cost: $0.10/day
  Status: Works ✓

Medium traffic (10K req/sec):
  Sliding Log memory: 600 MB/day
  Cost: $10/day
  Status: Works but expensive

High traffic (100K req/sec):
  Sliding Log memory: 6 GB/day
  Cost: $100/day
  Status: Expensive, but works

Extreme traffic (1M req/sec):
  Sliding Log memory: 60 GB/day
  Cost: $1000/day
  CPU overhead: 300+ seconds per second (impossible!)
  Status: BREAKS! ❌

Example: Twitter shutdown
  Twitter gets 100K tweets per second (spike)
  Memory needed: 200 GB for just rate limiting
  Redis max memory: 256 GB (shared for all features)
  Result: System OOM error, Twitter goes down!
  
  Real impact: Twitter outage affects 300M users
              Market cap loss: $10B+
              One payment: To use Sliding Window Log
```

**❌ Complex Implementation - Requires Advanced Data Structures**

```
Token Bucket implementation: 5 lines
```
if (tokens >= 1) {
  tokens--
  allow()
}
```

Sliding Window Log implementation: 50+ lines
```
sorted_set = sorted_timestamps()
remove_old_timestamps(window_start)
add_new_timestamp(now)
if (count(sorted_set) < limit) {
  allow()
}
// Plus Redis commands, locking, cleanup, expiration management
```

Complexity problems:

1. Redis sorted set operations are complex
   - ZCARD: Count items (O(1) but slow in practice)
   - ZREM: Remove items (O(n log n) worst case)
   - ZADD: Add item (O(log n))
   - Expiration: Manual cleanup or TTL management
   
2. Race conditions harder to prevent
   - Lua script must be atomic
   - More code = more bugs
   
3. Operational complexity
   - Monitoring memory growth
   - Setting TTLs on timestamps
   - Debugging timestamp issues
   - Handling Redis memory limits

Real impact:
  Startup with 5 engineers
  Estimate: "2 days to implement"
  Reality: "2 weeks to get it right"
  Cost: Engineer time × 10X more than Token Bucket
```

#### Storage Reality
```
Per user with 100 requests in window:

Token Bucket: 1 counter = 8 bytes
Fixed Window: 1 counter = 8 bytes
Leaking Bucket: 1 queue = 8 bytes

Sliding Window Log: 100 timestamps × 20 bytes = 2000 bytes
                   That's 250X MORE!

At 1M requests/sec:
  Token Bucket: 8MB total
  Sliding Window Log: 200MB per SECOND!
  
Cost: $200,000+/month for cloud storage!
```

#### When to Use
- **Only for**: Critical systems where cost is irrelevant
- **Examples**: Payment systems, security audit logs, fraud detection
- **Key requirement**: Perfect accuracy > performance/cost

#### Real-World Case: Payment Systems
```
Some payment processors use Sliding Window Log:
- Financial transactions can't have exploitable edge cases
- Cost of errors: Customer double-charged, fraud, etc.
- Memory and CPU cost is acceptable
- Accuracy is non-negotiable
```

---

### 5️⃣ Sliding Window Counter ⭐ (Recommended)

#### How It Works

```
Concept: Use TWO counters to estimate rolling window (not store all timestamps!)

Store: 
  - Previous window count
  - Current window count
  - Window start timestamp (just 1!)

Calculate:
  requests_in_rolling_window ≈ current_count + (previous_count × overlap%)
  
If estimate < limit: ACCEPT
Else: REJECT
```

#### Problems It Solves

From Token Bucket:
❌ Hard to tune parameters
✅ **Just set window size - math handles the rest!**

From Leaking Bucket:
❌ High latency
✅ **O(1) response time, no queueing!**

From Fixed Window:
❌ Exploitable edge cases
✅ **Uses rolling window concept, impossible to exploit!**

From Sliding Window Log:
❌ Too expensive (memory/CPU)
✅ **Just 24 bytes per user (vs 2000 for Log)!**

#### Using Scenario A (Web API)

```
Configuration: Window = 1 min, Limit = 10, Position = 30% into current

T=0:00:  3 requests arrive
         prev_count = 0
         curr_count = 3
         Status: ✓ Accept all (3 < 10)

T=0:06:  Quiet period (no new requests)
         Window change at 1:00
         Tokens built up mentally

T=0:09:  9 requests arrive (burst!)
         Current window: 3 requests
         Previous window: 0 requests
         Overlap: 51/60 = 85%
         
         Estimate: 3 + (0 × 0.85) = 3
         Status: 3 < 10 ✓ Accept request #1
         curr_count = 4
         
         Estimate: 4 + (0 × 0.85) = 4
         Status: 4 < 10 ✓ Accept request #2
         ... continue ...
         
         Estimate: 10 + (0 × 0.85) = 10
         Status: 10 >= 10 ✗ Reject requests #9, #10

Result: 8/9 accepted (89% success)
Latency: ~0ms (just one calculation!)
Memory: 24 bytes per user (3 small values)
Accuracy: 99.997% (Cloudflare tested on 400M requests)
```

#### Advantages
✅ **Fast** - O(1) calculation
✅ **Memory efficient** - 24 bytes (vs 2000 for Log)
✅ **Accurate** - 99.997% accuracy
✅ **Secure** - No exploitable boundaries
✅ **Balanced** - Perfect combination of speed, cost, accuracy

#### Limitations

**❌ Not 100% Perfect Accuracy (0.003% Error Rate)**

```
Real limitation: The algorithm estimates, doesn't count exact

Example: Payment API with 50 transactions/minute limit

Previous minute: 50 transactions at T=0:00 (all at start!)
Current minute at T=1:00: 50 transactions at T=1:00 (all at start!)

Actual rolling window [0:30-1:30]:
  0:00-0:30 window: 25 transactions (assumption)
  1:00-1:30 window: 50 transactions
  Total: 75 transactions in 60 seconds!
  Limit: 50 per minute
  EXCEEDED by 25! ✗

Sliding Window Counter calculation:
  Current: 50
  Previous: 50
  Overlap: 50% (at 30-second mark)
  Estimate: 50 + (50 × 0.5) = 75 ✓ Matches!
  
But assumption was WRONG:
  - Assumed: Evenly distributed (25 per 30 seconds)
  - Reality: All 50 in first second!
  - Result: Burst that exceeds limit

Cloudflare finding:
  Tested on 400 million requests
  Error rate: 0.003%
  = 12,000 wrongly allowed/rejected out of 400M
  
  For most systems: Acceptable
  For payment: Might be critical (12K fraud attempts!)
```

**❌ Assumes Even Distribution of Previous Window**

```
Real-world traffic isn't evenly distributed

E-commerce API example:

Scenario 1: Normal traffic
  Previous minute: 50 requests spread throughout
  Sliding Counter: Accurate! ✓
  
Scenario 2: Marketing event
  Previous minute: 50 requests (ALL in first 10 seconds!)
  Current minute at T=30s: New 50 requests
  
  Reality [0:20-1:20]:
    0:20-0:30: 10 requests (tail of event)
    1:00-1:20: 50 requests (current) 
    Total: 60 requests in 60 seconds (exceeds limit!)
    
  Sliding Counter assumption:
    Overlap = 50%
    Estimate: 50 + (50 × 0.5) = 75 (way over!)
    Result: REJECTED when should've been ACCEPTED
    
Impact: Legitimate traffic rejected!
        "During our flash sale, API rate limit blocked orders!"
        Customer: "Your API is broken during peak time!"

Scenario 3: Traffic cliff
  Previous minute: 0 requests
  Current minute: 100 requests suddenly
  
  Sliding Counter at T=30s:
    Overlap = 50%
    Estimate: 100 + (0 × 0.5) = 100
    Correct! ✓
    
But what if previous was 100?
    Estimate: 100 + (100 × 0.5) = 150
    Exceeds limit by 2X!
    All requests rejected even though previous is expiring!
```

**❌ Only Works for Reasonable Window Sizes**

```
Edge case 1: Very short windows (1-5 seconds)

Window: 2 seconds
Requests: 10 per second

At 1-second mark:
  Previous window: 10 requests in 2 seconds
  Overlap at 50%: Assume 5 in the overlapping second
  Current: 10 new requests
  Estimate: 10 + (10 × 0.5) = 15
  
Problem: Very inaccurate!
  Reality depends on exact millisecond of previous distribution
  Could be 10-20 (varies widely)
  
At 0.5-second mark:
  Overlap at 25%: Assume 2.5 in overlap
  Estimate: 10 + (10 × 0.25) = 12.5
  
  Huge variance in accuracy for short windows!

Edge case 2: Very long windows (1 hour)

Window: 1 hour
Traffic pattern: Lunch rush (12:00-13:00) vs night (2:00-3:00)

At 2:30 AM (30 minutes into night window):
  Previous hour: 1000 requests (from lunch!)
  Overlap at 50%: Assume 500 from lunch hour
  Current: 10 requests (night)
  Estimate: 10 + (1000 × 0.5) = 510
  
  Reality: Lunch traffic ancient, shouldn't count!
  Should be: 10
  But estimate: 510 (50X over!)
  
  Result: All legitimate night traffic rejected!

Long windows have:
  - High variance in traffic patterns
  - Wrong overlap percentages
  - Inaccurate estimates
```

#### Storage Breakdown
```
Per user:
  Token Bucket:            8 bytes
  Fixed Window:            8 bytes
  Leaking Bucket:          8 bytes
  Sliding Window Counter: 24 bytes (3 values: prev, curr, start_time)
  Sliding Window Log:    2000 bytes
  
Comparison:
  Counter vs Log: 83X more efficient! ✓
  Counter vs others: Only 3X more storage (acceptable!)
```

#### When to Use
- **Best for**: Most production systems (99% of use cases!)
- **Examples**: Twitter, GitHub, Google APIs, Stripe, most SaaS
- **Key requirement**: Good balance of speed, cost, accuracy

#### Real-World Case: Most Companies
```
Why Sliding Window Counter is most popular:

1. SPEED: No scanning, just 1 calculation
2. MEMORY: Minimal cost at scale
3. ACCURACY: Good enough (99.997%)
4. SECURITY: No exploitable edges
5. SIMPLICITY: Easy to understand and tune

Result: Default choice for most production APIs!
```

---

## Algorithm Comparison

### Side-by-Side Using Scenario A

```
Scenario: Web API, 10 requests/min, 3 at T=0, 9 at T=9

┌──────────────────┬──────────┬──────────┬──────────┬──────────┐
│ Algorithm        │ Accepted │ Latency  │ Memory   │ Secure?  │
├──────────────────┼──────────┼──────────┼──────────┼──────────┤
│ Token Bucket     │ 8/9 ✓    │ ~0ms ✓   │ 8 bytes  │ Yes ✓    │
│ Leaking Bucket   │ 8/9 ✓    │ 15-57s ✗ │ 8 bytes  │ Yes ✓    │
│ Fixed Window     │ 7/9      │ ~0ms ✓   │ 8 bytes  │ No ✗✗    │
│ Sliding Log      │ 8/9 ✓    │ ~0ms ✓   │ 2000 B ✗ │ Yes ✓    │
│ Sliding Counter  │ 8/9 ✓    │ ~0ms ✓   │ 24 bytes │ Yes ✓    │
└──────────────────┴──────────┴──────────┴──────────┴──────────┘
```

### Decision Matrix

```
Choose algorithm based on:

Fast response needed?     YES → Token Bucket or Sliding Counter
                         NO  → Leaking Bucket

Burst traffic expected?   YES → Token Bucket
                         NO  → Leaking Bucket

Perfect accuracy?         YES → Sliding Window Log
                         NO  → Token Bucket or Sliding Counter

Memory constrained?       YES → Token Bucket or Fixed Window
                         NO  → Sliding Window Log

Public API?              YES → Token Bucket or Sliding Counter
                         NO  → Any

Background jobs?         YES → Leaking Bucket
                         NO  → Token Bucket or Sliding Counter

Critical system?         YES → Sliding Window Log
                         NO  → Sliding Counter
```

---

## Interview Q&A

### Design Questions

**Q1: Design a rate limiter for Twitter**

```
Requirements:
- 10M daily active users
- Burst traffic (tweets spike during news events)
- Different limits for different user tiers
- Need to handle global distribution

Answer approach:
1. Choose Sliding Window Counter (balance of speed/cost/accuracy)
2. Use per-user buckets (multiple limits per user)
3. Store in Redis (distributed, fast)
4. Handle distributed synchronization with Redis
5. Deploy in multiple data centers
6. Use eventual consistency for geo-sync
```

**Q2: Your API is using Fixed Window and attackers are exploiting it**

```
Problem: Requests at window boundaries allow 2X limit

Solutions in order of preference:
1. Switch to Sliding Window Counter (best balance)
2. Switch to Sliding Window Log (if money no object)
3. Randomize window boundaries (hacky, partial fix)
4. Use shorter windows (reduced exploit window)
5. Token Bucket (if burst OK)

Why Sliding Counter best:
- Eliminates edge case exploitation
- Maintains performance
- Minimal cost increase
- Easy to migrate to
```

**Q3: Rate limiter uses 500GB memory for 1M req/sec, how to optimize?**

```
Current: Probably Sliding Window Log (2000 bytes per user)
Need: Reduce from 500GB

Solutions:
1. Switch to Sliding Window Counter (24 bytes per user)
   = 500GB / 2000 * 24 = 6GB (83X reduction!)
   
2. Reduce precision (store 10-sec windows instead of 1-sec)
   
3. Aggregate users into buckets
   
4. Use time-decay (old data less important)
   
Best: Switch algorithm + increase batch aggregation
Result: 6GB storage, same accuracy!
```

**Q4: Handle distributed rate limiting across 3 data centers**

```
Challenges:
1. Race condition: Two servers increment same counter
2. Synchronization: Counter values not in sync

Solutions:
1. Centralized Redis: All servers query same Redis
   - Single source of truth
   - Network latency trade-off

2. Lua scripts: Atomic operations (read-check-write)
   - Prevents race condition in single op
   - Can't be interrupted

3. Eventual consistency: Accept brief inconsistency
   - Each DC has local counters
   - Sync periodically
   - 0.1% occasional overdraft OK

Best approach: Redis + Lua + eventual consistency for sync
```

**Q5: How would you migrate from Token Bucket to Sliding Window Counter?**

```
Strategy:
1. Deploy Sliding Counter alongside existing Token Bucket
   - New code: Use Sliding Counter
   - Old code: Keep Token Bucket
   
2. Canary rollout (10% traffic)
   - Monitor accuracy
   - Verify no issues
   
3. Gradual migration (10% → 25% → 50% → 100%)
   - Keep both systems in sync initially
   - Fall back easily if needed
   
4. Deprecate Token Bucket
   - Remove after successful migration
   
Timeline: 2-4 weeks for safe migration
Risk: Low (can always fall back)
```

### Problem-Solving Questions

**Q6: API has legitimate users hitting rate limits, how to debug?**

```
Root cause analysis:
1. Is it Fixed Window? Check for edge case exploitation
   - Monitor request timing patterns
   - Look for clusters at :00 seconds

2. Is Token Bucket? Parameters might be wrong
   - Check bucket size vs expected bursts
   - Analyze traffic distribution

3. Is Leaking Bucket? Legitimate users waiting
   - Check average queue time
   - Compare to SLA

Solution depends on algorithm!
For Sliding Counter: User might genuinely exceed limit
→ Adjust limit, not algorithm
```

**Q7: How would you handle different rate limits for different user tiers?**

```
Design:
1. Multiple buckets/counters per user
   Free tier: 10 req/min
   Pro tier: 1000 req/min
   Enterprise: 100,000 req/min

2. Check user tier on each request
   user_tier = fetch_from_cache(user_id)
   bucket = get_bucket(user_id, user_tier)
   check_limit(bucket)

3. Update on tier change
   Need to reset counter or migrate gracefully

Implementation:
- Separate Redis keys per tier
- Cache tier info locally
- Periodic sync of tier changes
```

### Trade-off Questions

**Q8: Why not always use Sliding Window Log for perfect accuracy?**

```
Trade-offs:
✓ Pros: Perfect accuracy (100%)
✗ Cons: 
  - 125X more memory
  - 100X slower (O(n) vs O(1))
  - Complex to implement
  - Doesn't scale to large traffic

When to accept trade-off:
- Money is no object
- Security/compliance requires it
- Traffic is low (<10K req/sec)
- Example: Payment system

Reality:
- 99.9% of systems: Sliding Counter is fine
- 0.1% of systems: Need Window Log
```

**Q9: Token Bucket vs Sliding Window Counter - which to choose?**

```
Token Bucket better when:
✓ Burst traffic is feature, not bug
✓ Want to give users bonus capacity
✓ Real-time responsiveness critical
Example: Video streaming, social media

Sliding Counter better when:
✓ Want steady, predictable behavior
✓ Burst traffic is attack, not feature
✓ Slightly less perfect latency OK
Example: Payment APIs, resource limits

In practice:
- 60% use Sliding Counter (best balance)
- 30% use Token Bucket (burst handling)
- 10% use others (specific requirements)
```

---

## Implementation Guide

### Choosing Where to Place Rate Limiter

```
1. CLIENT-SIDE: ✗ Don't do this!
   - Unreliable (users can spoof)
   - No security value

2. APPLICATION CODE: ✓ OK for small systems
   - Part of your code
   - Direct access to request
   - Hard to update without redeployment

3. API GATEWAY/MIDDLEWARE: ✓ RECOMMENDED
   - Separate service
   - Works for all backend services
   - Can update independently
   - Example: Kong, AWS API Gateway, Nginx
```

### Storage: Why Redis?

```
Options:
1. Database:     ✗ Too slow (disk access = 10-100ms)
2. Memory cache: ✓ Fast (in-memory = 1µs)
3. Redis:        ✓ Perfect (in-memory + distributed)

Why Redis:
✓ Fast (in-memory operations)
✓ Atomic operations (INCR, EXPIRE)
✓ Distributed (shared across servers)
✓ Expiration support (auto-cleanup of old data)
✓ Sorted sets (for Sliding Window Log)

Commands needed:
- INCR: Increment counter atomically
- EXPIRE: Set auto-expiration time
- DEL: Delete expired counters
- ZINCRBY: For sliding window log
```

### Rate Limit Headers

```
HTTP Response should include:

X-RateLimit-Limit: 100
  → Maximum requests allowed in window

X-RateLimit-Remaining: 37
  → How many requests left in window

X-RateLimit-Reset: 1719072060
  → Unix timestamp when limit resets

X-Retry-After: 45
  → If rate limited: seconds to wait before retry

Client can use these to:
- Know when limit resets
- Implement smart backoff
- Display remaining quota to users
```

### Client Retry Strategy

```
When client gets 429 (Too Many Requests):

1. EXPONENTIAL BACKOFF: Don't retry immediately
   Retry 1: Wait 1 second
   Retry 2: Wait 2 seconds
   Retry 3: Wait 4 seconds
   (Don't hammer the server!)

2. USE X-RETRY-AFTER: Server tells you when to retry
   Wait until X-Retry-After timestamp
   Then retry

3. MAX RETRIES: Don't retry forever
   Give up after 3-5 attempts
   Return error to user

Pseudo-code:
```
retry_count = 0
while retry_count < 3:
    response = make_request()
    if response.status == 429:
        wait_time = response.headers['X-Retry-After']
        sleep(wait_time)
        retry_count += 1
    else:
        return response
return error("Max retries exceeded")
```
```

---

## Distributed Systems Challenges

### Challenge 1: Race Conditions

**Problem:**
```
Two requests arrive simultaneously
Both read counter = 3
Both check: 3 < 10? YES
Both write: counter = 4
Both allowed!

But counter should be 5!
Off by 1 error, multiplied = overflow!
```

**Solution: Lua Scripts**
```
Redis Lua script (atomic):

local current = redis.call('GET', key)
if current < limit then
    redis.call('INCR', key)
    return true
else
    return false
end

Lua runs atomically:
- Read, check, write happen together
- Can't be interrupted
- Perfect for race condition prevention
```

### Challenge 2: Synchronization Across Data Centers

**Problem:**
```
User in US: Requests go to US data center
User in EU: Requests go to EU data center

US DC has counter: 5 requests
EU DC has counter: 3 requests

But both serve same user!
User is rate limited in US, not in EU
Inconsistent!
```

**Solution: Centralized Redis**
```
All data centers → Single Redis instance

Benefits:
✓ Single source of truth
✓ Consistent across DCs

Trade-off:
✗ Network latency (100-200ms for distant DC)
✗ Single point of failure

Mitigation:
- Redis cluster with replication
- Fall back to local counters if central fails
- Sync eventually when connection restored
```

### Challenge 3: Eventual Consistency

**Problem:**
```
Want fast response (local processing)
But want consistent counters (central storage)
Can't have both!
```

**Solution: Accept Brief Inconsistency**
```
Strategy:
1. Each DC has local cache of counters
2. Cache expires every 10 seconds
3. Sync with central Redis periodically
4. Small overdraft OK (0.1% of requests)

Example:
- User sent 9 requests to US DC
- Cache expires
- User sends 2 more requests in EU DC
- EU: Doesn't know about 9 from US
- Both get through (11 total, limit was 10)
- Eventually consistent: Counter syncs after 10s

Acceptable because:
- 0.1% error rate is OK for most systems
- Performance gain worth it
- Real-time systems can't wait for sync
```

---

## Edge Cases & Gotchas

### Edge Case 1: Clock Skew

**Problem:**
```
Server A time: 12:00:00
Server B time: 11:59:50 (5 seconds behind)

Window reset happens at different times!
A: Window ends at 12:00:00
B: Window ends at 11:59:50

Possible overflow!
```

**Solution:**
```
1. Use NTP (Network Time Protocol)
   - Keep all servers synchronized
   - Usually within 1ms

2. Use server timestamp in requests
   - Accept timestamp from central server
   - Not from local clock

3. Build in time buffer
   - Accept 5-second skew
   - Conservative limits
```

### Edge Case 2: Sudden Traffic Spike

**Problem:**
```
Normal traffic: 100 req/sec
Sudden spike: 50,000 req/sec (flash sale!)

Rate limiter can't keep up!
Legitimate requests rejected!
```

**Solution:**
```
1. Over-provision capacity
   - Limit should be 2X expected peak
   
2. Use Token Bucket
   - Allows burst during spike
   
3. Implement circuit breaker
   - Temporary override during crisis
   - Manual approval for critical traffic
   
4. Alert monitoring
   - Detect spikes early
   - Scale up before hitting limit
```

### Edge Case 3: User Identity Issues

**Problem:**
```
Rate limiter key: IP address
User: Shared IP (corporate proxy)
Result: All employees share same limit!

Rate limiter key: User ID
User: Not logged in yet
Result: Can't identify during auth
```

**Solution:**
```
Use multiple keys:
1. IP address: First line of defense
2. User ID (if logged in): Per-user limit
3. Session ID: Prevent anonymous abuse

Combine intelligently:
- Anonymous: IP-based only
- Logged in: User ID based
- Admin: No limit (whitelisted)
```

---

## Real-World Companies & Their Approaches

| Company | Algorithm | Details | Scale |
|---------|-----------|---------|-------|
| **Twitter** | Token Bucket (now Sliding Counter) | 300 tweets per 3 hours, burst-friendly | 10M DAU |
| **GitHub** | Sliding Window Counter | 60 req/hr (public), 5000 (auth) | Enterprise |
| **Google** | Token Bucket | Varies by API, generous burst | Global |
| **Stripe** | Token Bucket | Customizable per endpoint | Enterprise |
| **Shopify** | Leaking Bucket | Steady order processing | Millions |
| **AWS** | Sliding Window Counter | Per-service customization | Largest |
| **CloudFlare** | Sliding Window Counter | DDoS protection, multi-layer | Billions req/day |

---

## Summary: Quick Reference

```
SIMPLE QUESTION: "Which algorithm should I use?"

SIMPLE ANSWER:
1. Use Sliding Window Counter ⭐ (default, 99% of cases)
2. Use Token Bucket if burst traffic is desired feature
3. Use Leaking Bucket if background processing
4. Use Fixed Window only for non-critical quotas
5. Use Sliding Window Log only if money is no object

ARCHITECTURE:
- Place in API Gateway/Middleware
- Store in Redis (distributed)
- Use Lua scripts for atomicity
- Implement monitoring and alerts
- Sync across DCs eventually

CLIENT SIDE:
- Respect X-RateLimit headers
- Implement exponential backoff
- Use X-Retry-After for retry timing
- Cache locally when possible
```

---

## References

### Original Material
- System Design Interview by Alex Xu, Chapter 4

### Further Reading
- [Rate Limiting Strategies (Google Cloud)](https://cloud.google.com/solutions/rate-limiting-strategies-techniques)
- [Stripe Rate Limiters](https://stripe.com/blog/rate-limiters)
- [Redis Sorted Sets for Rate Limiting](https://engineering.classdojo.com/blog/2015/02/06/rolling-rate-limiter/)
- [Cloudflare Rate Limiting](https://blog.cloudflare.com/counting-things-a-lot-of-different-things/)
- [Twitter API Documentation](https://developer.twitter.com/en/docs/basics/rate-limits)

