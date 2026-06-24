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

**From Token Bucket:**
```
❌ Token Bucket: Hard to tune (bucket size vs refill rate)
✅ Fixed Window: Simple - just set window size and limit
   Example: "100 requests per minute" is clear and simple
            No complex parameter tuning needed
```

**From Leaking Bucket:**
```
❌ Leaking Bucket: High latency (requests wait in queue)
   - Email example: 6 hours waiting for email to send!
   - Chat example: Message delayed 5 seconds
   - Trading example: Order delayed = wrong price
   
✅ Fixed Window: O(1) Response Time, NO Queueing

   HOW IT WORKS (No Queue):
   ─────────────────────────
   Request arrives → Check counter
     ├─ If counter < limit → ACCEPT (instant) ✓
     └─ If counter >= limit → REJECT (instant) ✗
   
   NO WAITING. Either accepted or rejected immediately.
   
   COMPARISON: Leaking vs Fixed Window
   ────────────────────────────────────
   Leaking Bucket (Email Service):
     Request 1: T=0s → Queued, wait 3.6s → Response at T=3.6s
     Request 2: T=0s → Queued, wait 7.2s → Response at T=7.2s
     Request 100: T=0s → Queued, wait 360s → Response at T=360s
     
     User experience: "Why is email taking 6 minutes?"
     Reality: Queue is processing at fixed rate, not instant
   
   Fixed Window Counter (Same Email Service):
     Request 1: T=0s → Check counter (O(1)) → Response at T=0.001s ✓
     Request 2: T=0s → Check counter (O(1)) → Response at T=0.001s ✓
     Request 100: T=0s → Check counter (O(1)) → Response at T=0.001s ✓
     Request 101: T=0s → Check counter (O(1)) → Response at T=0.001s (REJECTED) ✗
     
     User experience: "Instant feedback - email accepted or rejected"
     Reality: Microsecond-level latency, no queue
   
   IMPACT:
   - Leaking: Users think service is slow/broken
   - Fixed Window: Users get instant feedback on accept/reject
```

**Why This Solves Leaking Bucket's Problems:**

```
Leaking Bucket pain points:

1. LONG WAITS in queue
   Fixed Window: No queue → No waits! ✓

2. BAD FOR REAL-TIME (chat, trading)
   Fixed Window: Instant decision → Works for real-time ✓

3. CAN'T PRIORITIZE
   Fixed Window: Different limits per endpoint → Can prioritize ✓
   
4. UNPREDICTABLE LATENCY
   Fixed Window: Always O(1) → Predictable latency ✓
```

#### Problems It Creates
⚠️ **CRITICAL FLAW: Exploitable edge cases!**

But here's the trade-off: This speed comes at a cost...
```
Fixed Window gains: Speed & simplicity
Fixed Window loses: Security at boundary edges (exploitable!)

The edge case vulnerability is the price for O(1) response time.
```

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

#### Core Idea (Simplest Possible)

```
PROBLEM with Sliding Window Log:
  Storing all timestamps = 2000+ bytes per user = TOO EXPENSIVE!

SOLUTION - Sliding Window Counter:
  What if we DON'T store all timestamps?
  What if we just store TWO numbers and estimate?
  
  Number 1: Requests in previous minute
  Number 2: Requests in current minute
  
  Then: ESTIMATE total using math!
  
RESULT: Only 24 bytes per user instead of 2000+!
```

---

#### Step 1: The Three Values We Store

```
Let's say it's 12:05:30 (5 minutes and 30 seconds)

We store exactly 3 things:

1️⃣  prev_count = 8
    "In the PREVIOUS minute (12:04:30-12:05:30), 
     we had 8 requests"

2️⃣  curr_count = 5
    "In the CURRENT minute (12:05:30-12:06:30, 
     we have 5 requests so far"

3️⃣  window_start = 12:05:30
    "The current minute started at 12:05:30"

That's it! Just 3 numbers!
```

---

#### Step 2: Understanding "Rolling Window"

```
ROLLING WINDOW = Last 60 seconds from RIGHT NOW

Current time: 12:05:30

Rolling window (last 60 seconds):
  [12:04:30 -------- 12:05:30]
   ↑                 ↑
   60 seconds        Now
   ago               
   
This window overlaps BOTH minutes:
  ✓ Part of previous minute (12:04:30 to 12:05:30) = 60 seconds
  ✓ Part of current minute (nothing yet, just started)

BUT WAIT - time moves forward!

Current time: 12:05:35 (5 seconds later)

Rolling window (last 60 seconds):
  [12:04:35 -------- 12:05:35]
   ↑                 ↑
   60 seconds        Now
   ago               
   
This window overlaps BOTH minutes:
  ✓ Part of previous minute (12:04:35 to 12:05:30) = 55 seconds
  ✓ Part of current minute (12:05:30 to 12:05:35) = 5 seconds
```

---

#### Step 3: The Overlap Percentage

```
WHAT IS OVERLAP?

As time moves forward in the current minute, 
more of the previous minute "expires" (falls out of rolling window)

Let me show this visually:

Time: 12:05:30 (just started current minute)

  Previous min    │ Current min
  12:04:30─12:05:30│12:05:30─12:06:30
        [PREV 8]  │   [CURR 5]
        
Rolling window (last 60 sec): [12:04:30 to 12:05:30]
  All previous requests in rolling window!
  
Overlap = 60 / 60 = 100% of previous window is relevant
          (all 60 seconds of previous window fit in rolling window)

────────────────────────────────────────────────────

Time: 12:05:35 (5 seconds into current minute)

  Previous min    │ Current min
  12:04:30─12:05:30│12:05:30─12:05:35
        [PREV 8]  │ [CURR 5]
        
Rolling window (last 60 sec): [12:04:35 to 12:05:35]
                              ↑
                              Requests before 12:04:35 are OUT!
  
Only PART of previous window in rolling window!

Overlap = 55 / 60 = 91.7% of previous window is relevant
          (only 55 seconds of previous window fit in rolling window)

────────────────────────────────────────────────────

Time: 12:05:50 (20 seconds into current minute)

Rolling window (last 60 sec): [12:04:50 to 12:05:50]
                              ↑
                              Requests before 12:04:50 are OUT!
  
Overlap = 40 / 60 = 66.7% of previous window is relevant
          (only 40 seconds of previous window fit in rolling window)

────────────────────────────────────────────────────

Time: 12:06:30 (current minute ends, becomes previous minute)

Rolling window (last 60 sec): [12:05:30 to 12:06:30]
  
Overlap = 0 / 60 = 0% of previous window is relevant
          (the old "previous window" is completely outside rolling window!)
```

---

#### Step 4: The Overlap Formula

```
HOW TO CALCULATE OVERLAP PERCENTAGE?

Data we have:
  window_start = 12:05:30 (when current minute started)
  now = 12:05:35 (current time)
  window_size = 60 seconds

Formula:
  overlap = (window_start + window_size - now) / window_size
          = (12:05:30 + 60 - 12:05:35) / 60
          = (12:06:30 - 12:05:35) / 60
          = 55 seconds / 60 seconds
          = 0.917 (or 91.7%)

IN ENGLISH:
  "How much longer until current window ends?" / "Window size"
  = "Time left in window" / "Total window size"
```

---

#### Step 5: The Complete Estimation

```
FORMULA:
  estimate = curr_count + (prev_count × overlap)

EXAMPLE:

Time: 12:05:35
  window_start = 12:05:30
  curr_count = 5 requests
  prev_count = 8 requests
  
Calculate overlap:
  overlap = (12:05:30 + 60 - 12:05:35) / 60
          = 55 / 60
          = 0.917
  
Estimate:
  estimate = 5 + (8 × 0.917)
           = 5 + 7.33
           = 12.33
           ≈ 12 requests in rolling window
  
Limit: 10 requests
Decision: 12 > 10 → REJECT THIS NEW REQUEST! ✗

⚠️ IMPORTANT CLARIFICATION:
───────────────────────────

We're NOT rejecting all 12 requests!
We're rejecting THIS ONE NEW REQUEST that just arrived!

What happens:
  ✓ The 12 existing requests stay in system
  ✓ THIS new request is rejected (not added to counter)
  ✓ User must wait until some old requests expire

Timeline:
  Time 12:05:04 - User sends request #11
  System: "estimate = 12, limit = 10, 12 > 10"
  Action: REJECT request #11 (don't add to counter)
  Result: curr_count stays at 5
  
  Time 12:05:10 - User sends request #12
  System recalculates overlap = (12:05:00 + 60 - 12:05:10) / 60 = 50/60 = 0.833
  estimate = 5 + (8 × 0.833) = 5 + 6.67 = 11.67 ≈ 11
  System: "estimate = 11, limit = 10, 11 > 10"
  Action: REJECT request #12 (don't add to counter)
  Result: curr_count stays at 5
  
  Time 12:05:30 - Window boundary!
  Old previous window (12:04:00-12:05:00) with 8 requests EXPIRES
  prev_count becomes 0!
  
  Time 12:05:30 - User sends request #13
  System: prev_count = 0, curr_count = 5, overlap = 60/60 = 1.0
  estimate = 5 + (0 × 1.0) = 5
  System: "estimate = 5, limit = 10, 5 < 10"
  Action: ACCEPT request #13 ✓
  Result: curr_count = 6
  
SUMMARY:
  We reject requests ONE AT A TIME (not in batches)
  As old requests expire from rolling window, we accept new ones
  Counter only increases when we ACCEPT a request
```

---

#### Step 6: Real-World Example With Timeline

```
SCENARIO: Rate limit = 10 requests per minute
          User making requests every 2 seconds

TIME 12:04:00 - Window starts
  prev_count = 0
  curr_count = 0
  window_start = 12:04:00

TIME 12:04:06 - 3 requests arrived
  prev_count = 0
  curr_count = 3
  window_start = 12:04:00
  
  overlap = (12:04:00 + 60 - 12:04:06) / 60 = 54/60 = 0.9
  estimate = 3 + (0 × 0.9) = 3
  3 < 10? YES → ACCEPT ✓

TIME 12:04:30 - 8 more requests
  prev_count = 0
  curr_count = 8
  window_start = 12:04:00
  
  overlap = (12:04:00 + 60 - 12:04:30) / 60 = 30/60 = 0.5
  estimate = 8 + (0 × 0.5) = 8
  8 < 10? YES → ACCEPT ✓

TIME 12:05:00 - Window boundary! Current becomes previous
  Previous window (12:04:00-12:05:00) had: 8 requests
  Current window starts: 12:05:00
  
  prev_count = 8 (the old 8 requests)
  curr_count = 0 (new window just started)
  window_start = 12:05:00

TIME 12:05:02 - 2 requests arrive
  prev_count = 8
  curr_count = 2
  window_start = 12:05:00
  
  overlap = (12:05:00 + 60 - 12:05:02) / 60 = 58/60 = 0.967
  estimate = 2 + (8 × 0.967) = 2 + 7.73 = 9.73 ≈ 9
  9 < 10? YES → ACCEPT ✓

TIME 12:05:04 - 1 more request
  prev_count = 8
  curr_count = 3
  window_start = 12:05:00
  
  overlap = (12:05:00 + 60 - 12:05:04) / 60 = 56/60 = 0.933
  estimate = 3 + (8 × 0.933) = 3 + 7.46 = 10.46 ≈ 10
  10 >= 10? YES → REJECT ✗ (exactly at limit)
```

---

#### Step 7: Why It Works

```
KEY INSIGHT: We're estimating based on AVERAGE distribution

Assumption:
  Previous window had 8 requests spread evenly over 60 seconds
  = About 0.13 requests per second
  
If previous window had requests at:
  12:04:05, 12:04:15, 12:04:25, 12:04:35, 12:04:45, 12:04:55...
  
Rolling window at 12:05:04 includes:
  [12:04:04 to 12:05:04]
  
From previous: 12:04:05, 12:04:15, 12:04:25, 12:04:35, 12:04:45, 12:04:55
               = 6 requests in rolling window (out of 8)
               
Overlap = 6/8 = 75% ≈ Our calculation of 93.3%
                      (Close enough for practical purposes!)

The 93.3% is OVERESTIMATE (conservative) because:
  We assume even distribution
  Real distribution might be clumpy
  Better to overestimate → Reject early → Safer!
```

---

#### Step 8: Why Only 24 Bytes?

```
Token Bucket:
  prev_count:         8 bytes (integer)
  curr_count:         8 bytes (integer)
  window_start:       8 bytes (timestamp)
  ────────────────────────────
  Total:             24 bytes per user ✓

Compared to:
  Sliding Window Log: 2000+ bytes (100 timestamps × 20 bytes each)
  
SAVINGS: 83X more efficient! 🎉
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
```

#### Redis Usage by Algorithm

**1️⃣ Token Bucket**

```
Data Structure: String counter
Redis Key: "bucket:{user_id}"
Value: Current token count (integer)

Operations:
  GET bucket:user123
    → Returns: 8 (tokens available)
  
  DECR bucket:user123
    → Atomically reduces by 1
    → Returns: 7
    → If < 0, request rejected
  
  EXPIRE bucket:user123 3600
    → Auto-delete after 1 hour (cleanup)

```python
def allow_request(user_id):
  # Check available tokens
  tokens = redis.GET(f"bucket:{user_id}")
  
  # No tokens available?
  if tokens < 1:
    return False  # REJECT request
  
  # Consume one token
  redis.DECR(f"bucket:{user_id}")
  
  # Auto-expire bucket
  redis.EXPIRE(f"bucket:{user_id}", 3600)
  
  return True  # ACCEPT request

# Background Job (refill tokens)
while True:
  for user_id in all_users:
    # Add one token per second
    redis.INCR(f"bucket:{user_id}")
    
    # But don't exceed max
    current_tokens = redis.GET(f"bucket:{user_id}")
    if current_tokens > MAX_TOKENS:
      redis.SET(f"bucket:{user_id}", MAX_TOKENS)
  
  # Wait 1 second, then refill again
  sleep(1)
```

**Performance:**
- Time: O(1) per request (check + decrement)
- Memory: 8 bytes per user (one counter)
- Refill: 1 token per second (background job)

**Simplicity:**
- ⭐⭐⭐ Very simple implementation
- Requires background refill job
```

**2️⃣ Leaking Bucket**

```
Data Structure: List (queue)
Redis Key: "queue:{user_id}"
Values: Request timestamps or IDs

Operations:
  LPUSH queue:user123 "req_12345"
    → Add request to queue (O(1))
  
  LLEN queue:user123
    → Check queue size
    → If >= max_capacity, reject
  
  RPOP queue:user123
    → Remove and process request
    → Done in background worker
  
  EXPIRE queue:user123 3600
    → Auto-delete old queues

```python
def allow_request(user_id, request_id):
  # Check current queue size
  queue_size = redis.LLEN(f"queue:{user_id}")
  
  # Is queue at capacity?
  if queue_size >= MAX_QUEUE:
    return False  # Queue full - REJECT request
  
  # Add request to queue (FIFO)
  redis.LPUSH(f"queue:{user_id}", request_id)
  
  # Auto-expire old queues
  redis.EXPIRE(f"queue:{user_id}", 3600)
  
  return True  # Request QUEUED (not processed yet)

# Background Worker (separate thread/process)
while True:
  for user_id in all_users:
    # Remove request from queue (FIFO order)
    request_id = redis.RPOP(f"queue:{user_id}")
    
    # If request exists, process it
    if request_id:
      process_request(request_id)
  
  # Wait 1 second before next drain cycle
  sleep(1)
```

**Performance:**
- Time: O(1) per request (add to queue), O(1) per drain (remove from queue)
- Memory: 100+ bytes per queued request
- Wait time: Could be 10+ seconds (queue processing delay)

**Architecture:**
- ⚠️ Requires background worker (separate thread/service)
- ⚠️ Adds operational complexity
- Drains queue at fixed rate (1 request per second)
```

**3️⃣ Fixed Window Counter**

```
Data Structure: String counter + Timestamp
Redis Keys: 
  - "window:{user_id}:{timestamp}"
  - "count:{user_id}"

Operations:
  GET count:user123
    → Current count (integer)
  
  INCR count:user123
    → Atomically increment by 1
    → Returns new count
  
  EXPIRE count:user123 60
    → Auto-reset after 60 seconds

```python
def allow_request(user_id):
  # Get current count in this window
  current_count = redis.GET(f"count:{user_id}")
  
  # Check if we've hit the limit
  if current_count >= LIMIT:  # Example: LIMIT = 10 requests
    return False  # REJECT request
  
  # Increment counter
  redis.INCR(f"count:{user_id}")
  
  # Set expiration for window reset (60 seconds)
  redis.EXPIRE(f"count:{user_id}", 60)
  
  return True  # ACCEPT request
```

**Performance:**
- Time: O(1) per request (get + increment)
- Memory: 8 bytes per user (one counter)
- Window reset: Automatic after 60 seconds (EXPIRE)

**Simplicity:**
- ⭐⭐⭐ Simplest implementation!
- No background jobs needed
- No complex logic

**Security Weakness:**
- ⚠️ Exploitable at window boundaries (2X limit possible!)
```

**4️⃣ Sliding Window Log**

```
Data Structure: Sorted Set (for timestamps)
Redis Key: "log:{user_id}"
Values: Timestamps (scores for sorting)

Operations:
  ZADD log:user123 1719072000 "req_1"
    → Add timestamp to sorted set
    → Score = Unix timestamp
  
  ZCOUNT log:user123 min max
    → Count requests in [now-60s, now]
    → min = "now - 60", max = "now"
  
  ZREMRANGEBYSCORE log:user123 0 (now-60)
    → Remove old timestamps (cleanup)
  
  EXPIRE log:user123 60
    → Auto-delete old logs

Pseudo-code:
```python
def allow_request(user_id):
  # Current time
  now = current_timestamp()
  
  # Rolling window: last 60 seconds
  window_start = now - 60
  
  # Step 1: Remove old timestamps outside window (cleanup)
  redis.ZREMRANGEBYSCORE(f"log:{user_id}", 0, window_start)
  
  # Step 2: Count requests within rolling window [window_start, now]
  count = redis.ZCOUNT(f"log:{user_id}", window_start, now)
  
  # Step 3: Check if we've exceeded limit
  if count >= LIMIT:  # Example: LIMIT = 50 requests
    return False  # REJECT this request
  
  # Step 4: Request accepted - add timestamp to sorted set
  redis.ZADD(f"log:{user_id}", now, f"req_{uuid}")
  
  # Step 5: Set expiration for automatic cleanup
  redis.EXPIRE(f"log:{user_id}", 60)
  
  return True  # ACCEPT this request
```

**Performance:**
- Time: O(n log n) per request (scan + insert sorted set)
- Memory: 2000+ bytes per user (all timestamps stored!)
- Accuracy: Perfect ✓ (every timestamp tracked)

**Tradeoffs:**
- ✅ Perfect accuracy, no edge cases
- ❌ Memory-intensive, slow at scale
- ❌ Complex sorted set operations
```

**5️⃣ Sliding Window Counter**

```
Data Structure: String (multiple fields in one key)
Redis Keys: "counter:{user_id}"
Values: "prev_count|curr_count|window_start"

Operations:
  GET counter:user123
    → Returns: "25|15|1719072000"
  
  SETEX counter:user123 60 "0|30|1719072060"
    → Set with expiration (60 sec)

```python
def allow_request(user_id):
  # Current time
  now = current_timestamp()
  
  # Step 1: Get current state from Redis
  state = redis.GET(f"counter:{user_id}")
  if not state:
    # First time: initialize
    state = "0|0|{now}"
  
  # Parse the three values
  prev_count, curr_count, window_start = parse(state)
  
  # Step 2: Check if we've moved to a new window
  if now - window_start >= 60:
    # New window started! Shift counts.
    prev_count = curr_count  # Old current becomes previous
    curr_count = 1           # New request is first in new window
    window_start = now       # Mark new window start
  else:
    # Still in same window - estimate rolling window
    
    # How much of previous window overlaps current rolling window?
    overlap = (window_start + 60 - now) / 60
    
    # Estimate: current requests + (previous requests × overlap)
    estimated = curr_count + (prev_count * overlap)
    
    # Step 3: Check if limit exceeded
    if estimated >= LIMIT:  # Example: LIMIT = 10 requests
      return False  # REJECT this request
    
    # Request accepted - increment current count
    curr_count += 1
  
  # Step 4: Save updated state back to Redis
  new_state = f"{prev_count}|{curr_count}|{window_start}"
  redis.SETEX(f"counter:{user_id}", 60, new_state)
  
  return True  # ACCEPT this request
```

Cost: O(1) per request, 24 bytes per user (efficient!)
Simplicity: ⭐⭐ (moderate logic)
Accuracy: 99.997% (acceptable for most systems)
Best for: Production systems (best balance!)
```

#### Redis Decision Matrix

```
Algorithm           │ Data Structure │ Complexity │ Cost/User │ Speed
────────────────────┼────────────────┼────────────┼───────────┼──────
Token Bucket        │ String         │ Simple     │ 8 bytes   │ O(1)
Leaking Bucket      │ List           │ Moderate   │ 100 B/req │ O(1)
Fixed Window        │ String         │ Simplest   │ 8 bytes   │ O(1)
Sliding Window Log  │ Sorted Set     │ Complex    │ 2000+ B   │ O(n)
Sliding Window Counter │ String      │ Moderate   │ 24 bytes  │ O(1)
```

#### Redis Lua Scripts for Atomicity

```
Why Lua scripts?
  Without Lua: Race condition between GET and INCR
  
  Server 1: GET counter → 9
  Server 2: GET counter → 9
  Server 1: INCR → 10
  Server 2: INCR → 10
  
  Both allowed! Bug! 😱

With Lua script (atomic):
  redis.eval(script, keys, args)
  → Entire script runs without interruption
  → Can't be interleaved with other requests

Lua example for Sliding Window Counter:
```
local count = redis.call('GET', KEYS[1])
if not count or tonumber(count) < tonumber(ARGV[1]) then
  redis.call('SETEX', KEYS[1], ARGV[2], ARGV[3])
  return 1  -- allowed
else
  return 0  -- rejected
end
```
```

### Rate Limit Headers

**YES - Set by SERVICE-SIDE rate limiter, sent in response headers**

#### How It Works

```
REQUEST FLOW:
─────────────

1. Client sends request
   GET /api/users HTTP/1.1
   Host: api.twitter.com
   Authorization: Bearer token123

2. Server-side rate limiter processes
   ├─ Get user_id from request
   ├─ Check rate limit for that user
   ├─ Calculate remaining quota
   ├─ Determine reset time
   └─ Add headers to response

3. Server sends response WITH headers
   HTTP/1.1 200 OK
   X-RateLimit-Limit: 300
   X-RateLimit-Remaining: 287
   X-RateLimit-Reset: 1719072060
   
4. Client reads headers
   ├─ Knows max allowed: 300
   ├─ Knows remaining: 287
   ├─ Knows when resets: 1719072060
   └─ Can plan next requests

5. Client implements smart retry logic
   if remaining < 10:
     wait until reset time
   else:
     send next request
```

#### Headers Set by Rate Limiter

```
X-RateLimit-Limit: 300
  Set by: Rate limiter algorithm
  Calculated from: Configuration (e.g., 300 per hour)
  Value: Fixed per user tier
  Example: GitHub free tier = 60, authenticated = 5000

X-RateLimit-Remaining: 287
  Set by: Rate limiter algorithm
  Calculated from: Current count vs limit
  Value: Changes with each request
  Example: 
    - After 1st request: 299 remaining
    - After 2nd request: 298 remaining
    - After 13th request: 287 remaining

X-RateLimit-Reset: 1719072060
  Set by: Rate limiter algorithm
  Calculated from: Window start/end time
  Value: Unix timestamp when quota resets
  Example: "Reset happens at 2024-06-23 15:00:00 UTC"

X-Retry-After: 45
  Set by: Rate limiter (when rejecting request)
  Calculated from: Time until next available slot
  Value: Seconds to wait before retrying
  Example: If rate limited, tell client "wait 45 seconds"
```

#### Per-Algorithm Example

**Token Bucket (100 tokens/minute):**
```
State before request:
  Tokens: 47 (out of 100)
  Refill rate: 1 per second
  Last refill: T=0s

T=5s: Request arrives
  Service-side calculation:
    ├─ Tokens available: 47 + 5 = 52 (refilled!)
    ├─ After request: 52 - 1 = 51
    ├─ Remaining in response: 51
    ├─ Next refill in: 1 second (at 51, will become 52)
    └─ Reset at: T=60s (full bucket)

Response headers:
  X-RateLimit-Limit: 100
  X-RateLimit-Remaining: 51
  X-RateLimit-Reset: {unix_timestamp_of_T=60s}
```

**Fixed Window Counter (100 per minute):**
```
State before request:
  Current count: 87 (out of 100)
  Window started: T=0:00
  Window ends: T=1:00

T=0:45: Request arrives
  Service-side calculation:
    ├─ Count: 87 + 1 = 88
    ├─ Remaining: 100 - 88 = 12
    ├─ Window resets in: 15 seconds (at T=1:00)

Response headers:
  X-RateLimit-Limit: 100
  X-RateLimit-Remaining: 12
  X-RateLimit-Reset: {unix_timestamp_of_T=1:00}
```

**Sliding Window Counter (100 per minute):**
```
State before request:
  Current window count: 45
  Previous window count: 0
  Overlap: 30% (30 seconds into current window)
  Estimated total: 45 + (0 × 0.3) = 45

T=0:30: Request arrives
  Service-side calculation:
    ├─ Estimated: 45 + 1 = 46
    ├─ Remaining: 100 - 46 = 54
    ├─ Window resets in: 30 seconds (at T=1:00)

Response headers:
  X-RateLimit-Limit: 100
  X-RateLimit-Remaining: 54
  X-RateLimit-Reset: {unix_timestamp_of_T=1:00}
```

#### When Request is REJECTED (429 Status)

```
Request arrives but user exceeds limit
Service rejects: HTTP 429 Too Many Requests

Response headers:
  X-RateLimit-Limit: 300
  X-RateLimit-Remaining: 0  (No more quota!)
  X-RateLimit-Reset: 1719072060
  X-Retry-After: 45  (Wait 45 seconds, then retry)

Body:
  {
    "error": "rate_limit_exceeded",
    "message": "You have exceeded the rate limit",
    "retry_after_seconds": 45
  }
```

#### Client Usage (How Client Uses Headers)

```
Pseudocode: Client reads rate limit headers

response = make_request()

if response.status == 200:
  remaining = response.headers['X-RateLimit-Remaining']
  reset_time = response.headers['X-RateLimit-Reset']
  
  if remaining < 10:
    wait_until(reset_time)  # Smart backoff
  else:
    schedule_next_request_immediately()

elif response.status == 429:
  retry_after = response.headers['X-Retry-After']
  sleep(retry_after)
  retry_request()  # Retry after waiting
```

#### Real-World Example: GitHub API

```
Request to GitHub API:
GET https://api.github.com/user

Response:
HTTP/1.1 200 OK
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 59
X-RateLimit-Reset: 1719072060
X-RateLimit-Used: 1
X-RateLimit-Resource: core

Client reads these and knows:
  ✓ Max 60 requests per hour
  ✓ 59 more requests available
  ✓ Resets in ~45 minutes
  ✓ Can safely send 59 more requests

After 60 requests:
  X-RateLimit-Remaining: 0
  Response includes: 403 Forbidden
  Client: "OK, I'll wait until reset time"
```

#### Why This Matters

```
Without headers:
  Client has no idea about rate limit
  Sends requests blindly
  Gets surprised by 429 errors
  Bad UX, inefficient retries

With headers:
  Client knows remaining quota
  Knows exactly when quota resets
  Can plan ahead
  Implements smart backoff
  Better UX, efficient API usage
```

---

### Server-Side vs API Gateway: Where to Place Rate Limiter?

#### Option 1: Server-Side (In Application Code)

```
Architecture:
  Client → Network → Server (with rate limiter inside code)
                     ↑
                  Application code
                  checks rate limit
```

**Pros:**

```
✅ GRANULAR CONTROL
   Can rate limit per:
   - Specific endpoint
   - User tier (free vs paid)
   - User type (admin vs regular)
   - Request content (batch vs single)
   
   Example: GitHub API
     GET /user → 5000 req/hour
     GET /search → 30 req/minute (more expensive!)
     POST /repos/{owner}/{repo}/issues → 10 req/minute

✅ ACCESS TO BUSINESS LOGIC
   Can make smart decisions:
   - Premium users get higher limits
   - During off-peak: allow more traffic
   - Based on user reputation/history
   - Based on content/request type

✅ NO EXTRA NETWORK HOP
   Request reaches server immediately
   Slight latency advantage

✅ FINE-TUNED LIMITS
   Can adjust per endpoint without redeploying gateway
```

**Cons:**

```
❌ MUST IMPLEMENT IN EVERY SERVICE
   If you have 10 microservices:
   - Implement rate limiter 10 times
   - Update limit logic 10 times
   - Each service must have Redis connection
   - Hard to keep consistent across services
   
   Code duplication:
     Service 1: Sliding Window Counter implementation
     Service 2: Sliding Window Counter implementation (copy-paste!)
     Service 3: Sliding Window Counter implementation (copy-paste!)
     
   Nightmare to maintain!

❌ DOESN'T PROTECT BEFORE EXPENSIVE OPERATIONS
   Bad request still hits your database:
   - Malicious user sends 10K requests
   - All 10K reach your server
   - All 10K hit your database
   - Rate limit rejects after checking DB
   - DB hammered! 💥
   
   Example: Search endpoint without gateway protection
     Each search query hits Elasticsearch
     Rate limiter only checks AFTER Elasticsearch query
     Resource wasted!

❌ HARD TO UPDATE LIMITS
   To change rate limit:
   - Modify code
   - Rebuild image
   - Deploy to all services
   - Takes 10+ minutes
   
   Can't react quickly to traffic spikes!

❌ DOESN'T PROTECT AGAINST DDoS
   Millions of requests arrive:
   - All hit your servers
   - All consume resources
   - Rate limiter only helps after resources spent
   - Cascading failure!

❌ VULNERABLE TO INTERNAL BAD ACTORS
   Microservice calling another internally:
   - No rate limit between services
   - Buggy code sends 1M internal requests
   - Brings down other services
   - Internal DDoS!
```

---

#### Option 2: API Gateway (Separate Layer)

```
Architecture:
  Client → Network → API Gateway (rate limiter) → Server
                     ↑
                  Dedicated layer
                  blocks bad traffic
```

**Pros:**

```
✅ SINGLE POINT OF CONTROL
   One place to enforce all limits:
   - Update limit → Immediately takes effect
   - No redeployment needed
   - No code duplication
   - Consistent across all services
   
   Example: Kong, AWS API Gateway, Nginx
     Update limit in config
     Live in seconds!

✅ PROTECTS BEFORE EXPENSIVE OPERATIONS
   Bad requests stopped before reaching database:
   - Malicious user sends 10K requests
   - API Gateway rejects after 100 (your limit)
   - Only 100 reach your server
   - Database protected! ✓
   
   Resource savings:
     Without gateway: 10K requests hit DB
     With gateway: 100 requests hit DB
     100X savings in DB load!

✅ DDoS PROTECTION
   Handles traffic spikes:
   - 1M requests arrive
   - Gateway limits to 1K per second
   - Your server stays stable
   - Network bandwidth used efficiently
   
   Protects entire infrastructure!

✅ EASY TO UPDATE
   Change limit in gateway config:
   - No code deployment
   - Takes 1-2 seconds
   - Affects all services immediately
   - React to traffic spikes in real-time

✅ PROTECTS AGAINST INTERNAL BAD ACTORS
   Limits between services:
   - Service A calls Service B 1M times (buggy code)
   - Gateway limits to 1K/sec
   - Service B stays healthy
   - Bug doesn't cause cascade failure

✅ CENTRALIZED MONITORING/LOGGING
   All rate limit events in one place:
   - Easy to detect attacks
   - Easy to analyze traffic patterns
   - Unified alerting
```

**Cons:**

```
❌ LESS GRANULAR CONTROL
   Gateway doesn't know business logic:
   - Can't distinguish endpoint types
   - Can't know user tier/reputation
   - Harder to implement smart limits
   
   Limited to:
   - IP-based limits
   - User ID limits
   - Simple rules
   
   Can't do:
   - Different limits per endpoint
   - Dynamic limits based on load
   - Request content-based limits

❌ EXTRA NETWORK HOP
   One more network round-trip:
   Client → Gateway (extra hop!) → Server
   
   Latency impact:
   - Without gateway: 50ms
   - With gateway: 50ms + gateway latency (5-10ms)
   - 10-20% slower

❌ ANOTHER SYSTEM TO MAINTAIN
   Gateway is another component:
   - Must keep it updated
   - Must monitor its health
   - Must handle its failures
   - More operational complexity

❌ SINGLE POINT OF FAILURE
   If gateway goes down:
   - All traffic blocked
   - ALL services unavailable
   - No bypass option
   - Entire API down!
   
   Example: AWS outage in 2015
     API Gateway in one region failed
     All services in that region unreachable
     3-hour outage
     Millions in lost revenue

❌ GATEWAY DOESN'T KNOW ENDPOINT DETAILS
   Example: GitHub API
     Gateway: "100 requests per hour"
     But reality:
       - Simple reads: can handle 5000/hour
       - Complex searches: only 30/hour
       - Batch operations: only 10/hour
   
   Gateway can only do average limit!
```

---

#### Comparison Table

```
┌─────────────────────┬──────────────────┬─────────────────────┐
│ Aspect              │ Server-Side      │ API Gateway         │
├─────────────────────┼──────────────────┼─────────────────────┤
│ Granular control    │ ⭐⭐⭐ Excellent │ ⭐ Limited          │
│ Implementation      │ ❌ Duplicated    │ ✅ Single point     │
│ Updates            │ ❌ Slow (deploy) │ ✅ Fast (seconds)   │
│ DDoS protection    │ ❌ No            │ ✅ Yes              │
│ Resource protection │ ❌ No            │ ✅ Yes              │
│ Latency            │ ✅ Lower         │ ❌ Higher           │
│ Operational burden │ ✅ Lower         │ ❌ Higher           │
│ Single point of    │ ❌ No            │ ✅ Yes              │
│ failure            │                  │                     │
│ Internal protection│ ❌ No            │ ✅ Yes              │
└─────────────────────┴──────────────────┴─────────────────────┘
```

---

#### Best Practice: HYBRID Approach ⭐

```
USE BOTH!

Level 1: API Gateway (Coarse-grained)
  ├─ Block obvious DDoS attacks
  ├─ Simple IP/user rate limits
  ├─ Protect infrastructure
  └─ 1000 req/sec per user (generous)

Level 2: Server-Side (Fine-grained)
  ├─ Business logic based limits
  ├─ Per-endpoint limits
  ├─ User tier based limits
  └─ 100 req/sec for search, 5000 for reads

Architecture:
  Client
    ↓
  API Gateway
    ├─ Is this a DDoS attack? Block at gateway
    ├─ Malicious IP? Block at gateway
    └─ Otherwise: Forward to server
    ↓
  Server-Side Rate Limiter
    ├─ What endpoint? Check specific limit
    ├─ What user tier? Apply that limit
    └─ What time of day? Adjust limits dynamically

Benefits:
  ✅ DDoS protection (gateway)
  ✅ Granular control (server)
  ✅ Resource efficiency (gateway blocks bad traffic)
  ✅ Smart limits (server knows business logic)
  ✅ Defense in depth (two layers)
```

---

#### Real-World Examples

**Twitter (Likely Hybrid):**
```
Gateway Level: Blocks obvious attacks, 10K req/sec per IP
Server Level: 300 tweets/3 hours per user (sliding window counter)

Why both?
  - Gateway: Stops botnets, DDoS
  - Server: Enforces actual business limit
```

**GitHub (Likely Hybrid):**
```
Gateway Level: IP-based DDoS protection
Server Level: 
  - 60 requests/hour (public)
  - 5000 requests/hour (authenticated)
  - Different per endpoint

Why both?
  - Gateway: Infrastructure protection
  - Server: Fine-grained business rules
```

**Stripe (Likely Hybrid):**
```
Gateway Level: DDoS protection, malicious IP blocking
Server Level:
  - Per-merchant rate limits
  - Per-endpoint limits (different for payments vs queries)
  - Premium vs standard merchant different limits

Why both?
  - Gateway: Protects payment infrastructure
  - Server: Ensures fair usage per customer
```

---

#### Decision Guide: Where to Place Rate Limiter?

```
ONLY SERVER-SIDE if:
  ✓ Single monolithic application
  ✓ Low traffic (< 100 req/sec)
  ✓ Internal API (no DDoS risk)
  ✓ All endpoints have same limit
  ✓ No security concerns

ONLY API GATEWAY if:
  ✓ Simple limits (IP-based, generic)
  ✓ Multiple backend services
  ✓ High traffic (> 10K req/sec)
  ✓ DDoS protection critical
  ✓ Easy deployment needed

BOTH (HYBRID) if:
  ✓ Multiple services ← YES for most companies
  ✓ Complex business rules ← YES for most companies
  ✓ High traffic ← YES for most companies
  ✓ DDoS concerns ← YES for most companies
  ✓ Different limits per endpoint ← YES for most companies
  
  → Use BOTH! It's the best practice!
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

