# System Design Interview - Chapter 4: Design a Rate Limiter

## Table of Contents
1. [Problem Statement](#problem-statement)
2. [Benefits of Rate Limiting](#benefits-of-rate-limiting)
3. [Requirements](#requirements)
4. [Different Solutions/Algorithms](#different-solutionsalgorithms)
5. [Architecture Considerations](#architecture-considerations)
6. [Challenges in Distributed Systems](#challenges-in-distributed-systems)
7. [Additional Considerations](#additional-considerations)

---

## Problem Statement

A **rate limiter** is used to control the rate of traffic sent by a client or service. In the HTTP world, it limits the number of client requests allowed to be sent over a specified period. If the API request count exceeds the threshold defined by the rate limiter, all excess calls are blocked.

### Real-World Examples:
- A user can write no more than 2 posts per second
- You can create a maximum of 10 accounts per day from the same IP address
- You can claim rewards no more than 5 times per week from the same device

### Real-World Rate Limits:
- **Twitter**: 300 tweets per 3 hours
- **Google Docs APIs**: 300 per user per 60 seconds for read requests

---

## Benefits of Rate Limiting

### 1. **Prevent DoS Attacks**
- Almost all APIs published by large tech companies enforce some form of rate limiting
- Prevents both intentional and unintentional Denial of Service attacks
- Protects resources from being starved by malicious users

### 2. **Reduce Costs**
- Limiting excess requests means fewer servers and resources allocated
- Extremely important for companies using paid third-party APIs
- External APIs (check credit, make payment, retrieve health records, etc.) are charged on a per-call basis

### 3. **Prevent Server Overload**
- Filters out excess requests caused by bots or users' misbehavior
- Reduces server load during traffic spikes
- Ensures stable performance for legitimate users

---

## Requirements

### Functional Requirements:
- **Accurately limit excessive requests** - Must prevent requests exceeding the threshold
- **Flexible throttling** - Support different throttle rules (IP, user ID, or other properties)
- **Distributed** - Work across multiple servers/processes
- **Clear feedback** - Inform users when they are throttled (HTTP 429 status code)

### Non-Functional Requirements:
- **Low latency** - Rate limiter should not slow down HTTP response time
- **Memory efficient** - Use minimal memory
- **High fault tolerance** - If a component fails, it doesn't affect the entire system
- **Large scale** - Support high number of requests in a distributed environment

---

## Different Solutions/Algorithms

### 1. Token Bucket Algorithm ⭐ (Most Popular)

**How It Works:**
- A token bucket has pre-defined capacity
- Tokens are added to the bucket at preset rates periodically
- Each request consumes one token
- If there are enough tokens, request goes through; otherwise, it's dropped

**Example:**
```
Bucket Capacity: 4 tokens
Refill Rate: 2 tokens per second

Request arrives:
- Check if tokens available
- If yes: consume 1 token, allow request
- If no: drop request
```

**Parameters:**
- **Bucket size**: Maximum number of tokens allowed in the bucket
- **Refill rate**: Number of tokens added per second

**How Many Buckets?**
- Different bucket per API endpoint (e.g., 1 post/sec, 150 friends/day, 5 likes/sec → 3 buckets per user)
- Different bucket per IP address
- Global bucket for entire system

**Pros:**
- ✅ Easy to implement
- ✅ Memory efficient
- ✅ Allows burst traffic for short periods
- ✅ Used by Amazon and Stripe

**Cons:**
- ❌ Challenging to tune bucket size and refill rate properly
- ❌ Two parameters to optimize

#### **Detailed Example: Handling Burst Traffic**

**Configuration:**
```
Bucket Capacity: 10 tokens
Refill Rate: 1 token per second
Scenario: [3 requests] → 6 seconds quiet → [9 requests burst]
```

**Phase 1: Initial Requests (T=0s)**
```
Bucket state: 10/10 tokens (full)

3 requests arrive at T=0s:
  Request #1: Takes 1 token (9 remain) ✓
  Request #2: Takes 1 token (8 remain) ✓
  Request #3: Takes 1 token (7 remain) ✓

Result: 100% success (3/3 accepted)
After: 7 tokens remain
```

**Phase 2: Quiet Period (T=0s to T=9s) - THE GENIUS PART!**
```
No new requests, but tokens keep refilling:

T=1s:  Tokens: 7 → 8   (+1 per second)
T=2s:  Tokens: 8 → 9
T=3s:  Tokens: 9 → 10 (FULL! Capacity reached)
T=4-9s: Tokens: 10 → 10 (cannot exceed capacity)

KEY INSIGHT: During quiet period, tokens ACCUMULATE
This builds a RESERVE for upcoming burst!
```

**Phase 3: BURST Handling (T=9s)**
```
9 requests arrive suddenly!

Bucket state: 10/10 tokens (fully charged from quiet period)

Processing:
  Request #1: Takes 1 token (9 remain) ✓
  Request #2: Takes 1 token (8 remain) ✓
  Request #3: Takes 1 token (7 remain) ✓
  Request #4: Takes 1 token (6 remain) ✓
  Request #5: Takes 1 token (5 remain) ✓
  Request #6: Takes 1 token (4 remain) ✓
  Request #7: Takes 1 token (3 remain) ✓
  Request #8: Takes 1 token (2 remain) ✓
  Request #9: NO TOKEN! ✗ REJECTED

Result: 8/9 accepted (89% success rate!)
After: 2 tokens remain
```

#### **Why Token Bucket Excels at Bursts:**

```
┌─────────────────────────────────────┐
│  The Token Bucket Strategy:          │
├─────────────────────────────────────┤
│ 1. During quiet times → Save tokens  │
│ 2. During busy times → Spend tokens  │
│                                      │
│ Like a SAVINGS ACCOUNT:              │
│ - Deposit during good times          │
│ - Withdraw during emergency          │
└─────────────────────────────────────┘
```

**Key Advantages:**
- ✅ **Burst Absorption** - Can handle 10 requests at once if capacity=10
- ✅ **Low Latency** - Responses near-instant (not queued)
- ✅ **Rewards Fast Users** - No waiting in queue
- ✅ **Reserve Building** - Accumulates during quiet periods
- ✅ **Natural Traffic Fit** - Web traffic IS naturally bursty!

**Companies Using Token Bucket:**
- Amazon (AWS API Gateway)
- Google (Google APIs, Docs)
- Stripe (Payment APIs)
- GitHub (GitHub API)

---

### 2. Leaking Bucket Algorithm

#### **Core Concept**

Leaking Bucket is like a **water tank with a small hole at the bottom**:

```
┌─────────────────┐
│ Incoming Water  │ ← Water flowing in (requests arriving)
│ (requests)      │
├─────────────────┤
│                 │
│   QUEUE OF      │ ← Water stored in tank (requests waiting)
│   REQUESTS      │
│                 │
├─────────────────┤
│        ~        │ ← Small hole at bottom (leak)
└─────────────────┘
        ↓
   PROCESSED      ← Water flowing out at FIXED RATE
   REQUESTS         (constant, predictable)
```

**Key Characteristic:** Requests leak out at a **constant, predictable rate**, no matter how many requests flow in!

#### **How It Works:**

1. When a request arrives, the system checks if the queue is full
2. If queue is NOT full, the request is added to queue (FIFO order)
3. If queue IS full, the request is **dropped/rejected**
4. A background process removes requests from the queue at a **fixed rate**
5. Requests are processed in order (First-In-First-Out)

#### **Parameters:**

1. **Queue Size (Bucket Capacity)** - How many requests can wait?
   - Example: Maximum 10 requests in queue

2. **Outflow Rate (Leak Rate)** - How fast are requests processed?
   - Example: 1 request per 6 seconds (or 10 requests per minute)

3. **FIFO (First In, First Out)** - Processing order
   - First request added to queue is processed first
   - Fair to all requests

#### **Real-World Usage:**
- **Shopify** uses this for rate-limiting
- Email systems, job queues, database protection

#### **Phase-by-Phase Example**

**Configuration:**
```
Queue Capacity: 10 requests
Outflow Rate: 1 request per 6 seconds
Scenario: [3 requests] → 6 seconds quiet → [9 requests arrive]
```

**Phase 1: Initial Requests (T=0s)**
```
3 requests arrive at T=0s

Queue state: Empty (0/10)

Action:
  Request #1 enters → Queue: [R1]          (1/10) ✓
  Request #2 enters → Queue: [R1, R2]      (2/10) ✓
  Request #3 enters → Queue: [R1, R2, R3]  (3/10) ✓

Result: All 3 ACCEPTED and added to queue
Status: PROCESSING at fixed rate

Timeline:
  T=0s:  R1 at front, will exit at T=6s
  T=6s:  R1 exits ✓, R2 moves to front, will exit at T=12s
  T=12s: R2 exits ✓, R3 moves to front, will exit at T=18s
  T=18s: R3 exits ✓
```

**Phase 2: Quiet Period (T=0s to T=9s)**
```
No new requests, but processing continues at FIXED RATE

T=0s:  Queue: [R1, R2, R3]  (3 items, processing)
T=3s:  Queue: [R1, R2, R3]  (processing R1, will exit at T=6s)
T=6s:  Queue: [R2, R3]      (R1 exits! ✓ Done)
T=9s:  Queue: [R2, R3]      (R2 still processing, will exit at T=12s)

Key insight: Queue DRAINS steadily to 2 items
No "accumulation" like Token Bucket - constant leak!
```

**Phase 3: THE BURST (T=9s)**
```
9 requests arrive suddenly!

Queue state BEFORE: [R2, R3]  (2 items, can hold 10 max)

Processing incoming burst:
  R4 arrives  → Queue: [R2, R3, R4]        (3/10) ✓
  R5 arrives  → Queue: [R2, R3, R4, R5]    (4/10) ✓
  R6 arrives  → Queue: [..., R4, R5, R6]   (5/10) ✓
  R7 arrives  → Queue: [..., R5, R6, R7]   (6/10) ✓
  R8 arrives  → Queue: [..., R6, R7, R8]   (7/10) ✓
  R9 arrives  → Queue: [..., R7, R8, R9]   (8/10) ✓
  R10 arrives → Queue: [..., R8, R9, R10]  (9/10) ✓
  R11 arrives → Queue: [..., R9, R10, R11] (10/10) ✓ FULL!
  R12 arrives → ✗ REJECTED (queue full)

Result: 8 requests ACCEPTED
        1 request REJECTED (dropped)
```

**The Critical Difference: Wait Times!**

```
Token Bucket: R4 processed FAST (near-instant)

Leaking Bucket: R4 has to WAIT!
  R2: Exits at T=12s (wait: 3 seconds remaining)
  R3: Exits at T=18s (wait: 9 seconds from arrival at T=9s)
  R4: Exits at T=24s (wait: 15 seconds!)
  R5: Exits at T=30s (wait: 21 seconds!)
  ...
  R11: Exits at T=66s (wait: 57 seconds!) 😱
  R12: DROPPED immediately (no wait, just rejected)
```

#### **Comparison: Token Bucket vs Leaking Bucket**

| Scenario | Token Bucket | Leaking Bucket |
|----------|--------------|----------------|
| **3 initial requests** | Accepted immediately, fast response | Accepted, but wait in queue |
| **Quiet 6 seconds** | Tokens accumulate (recover capacity) | Queue drains at fixed rate |
| **9 requests at T=9s** | 8 accepted immediately (low latency) | 8 queued (high latency), 1 dropped |
| **Last request wait** | Near-instant | 57 seconds! |
| **User experience** | Fast, responsive | Slow, but predictable |

#### **Wait Time Formula**

```
For a request at position N in queue:
  wait_time = (N × time_per_request)
  
Example with 1 req/6sec:
  Position 1: Wait = 1 × 6 = 6 seconds
  Position 5: Wait = 5 × 6 = 30 seconds
  Position 10: Wait = 10 × 6 = 60 seconds
```

#### **Pros:**
- ✅ **Predictable output rate** - Always processes at fixed speed
- ✅ **Fair processing** - FIFO order (first request gets processed first)
- ✅ **Memory efficient** - Limited queue size
- ✅ **Protects backend** - Steady load on DB/workers
- ✅ **No token tuning** - Just set queue size and processing rate

**Cons:**
- ❌ **High latency** - Requests wait in queue (not ideal for APIs)
- ❌ **Burst handling poor** - Recent requests queued behind old ones
- ❌ **Not suitable for user-facing APIs** - Users hate waiting
- ❌ **Queue fills during bursts** - Excess requests dropped

#### **When to Use Leaking Bucket:**

✅ **Job Queue Processing**
```
Example: Background task processor
  - Limit: 10 tasks queued at a time
  - Rate: Process 1 task per second
  - Use: Protects database from overload
```

✅ **Email Sending System**
```
Example: Send marketing emails
  - Limit: Queue 1000 emails
  - Rate: Send 100 emails per minute
  - Use: Email server can handle steady load
```

✅ **Database Write Protection**
```
Example: Protect database from write spikes
  - Limit: 50 write operations in queue
  - Rate: 5 writes per second
  - Use: DB can process writes steadily
```

✅ **Resource-Constrained Systems**
```
Example: Limited worker pool, print queue, video encoding
  - When backend has limited processing capacity
  - Need predictable, steady output
  - Can tolerate wait times
```

#### **When NOT to Use Leaking Bucket:**

❌ **Public REST APIs**
- Users expect instant responses
- High latency is unacceptable

❌ **Real-time Systems**
- Need low latency responses
- Can't queue requests

❌ **Bursty Traffic**
- Natural traffic spikes (peak hours)
- Users will experience long waits

---

#### **Deep Comparison: Token Bucket vs Leaking Bucket**

**Same Scenario Applied to Both:**
```
Limit: 60 requests per minute (1 per second or 10 per 10 seconds)
Traffic: [3 requests at T=0s] → quiet 6 seconds → [9 requests at T=9s]
```

**TOKEN BUCKET TIMELINE:**
```
T=0s:   3 requests arrive
        Bucket: 10/10 tokens
        ✓ Accept 3, take 3 tokens
        Bucket now: 7/10

T=1-3s: Quiet, tokens refill
        +1 token per second
        Bucket: 8/10 → 9/10 → 10/10 (full!)

T=4-8s: Quiet continues
        Bucket: 10/10 (capped, cannot exceed)
        
T=9s:   9 requests arrive
        Bucket: 10/10 tokens ready!
        ✓ Accept 8, take 8 tokens
        ✗ Reject 1 (no tokens)
        Bucket now: 2/10

Result: 8/9 requests processed IMMEDIATELY
        Latency: ~0ms (instant)
        Success rate: 89%
        User experience: Fast, responsive!
```

**LEAKING BUCKET TIMELINE:**
```
T=0s:   3 requests arrive
        Queue: [R1, R2, R3] (3/10)
        ✓ Accept all 3
        Outflow begins: 1 request every 6 seconds

T=6s:   R1 processed and exits
        R2 at front of queue, will exit at T=12s
        Queue: [R2, R3] (2/10)

T=9s:   9 new requests arrive
        Queue before: [R2, R3] (2/10)
        Queue after burst:
          ✓ R4-R11 added (8 requests fit)
          ✗ R12 rejected (queue full at 10)
        Queue: [R2, R3, R4, R5, R6, R7, R8, R9, R10, R11]
        (all 10 slots filled)

T=12s:  R2 processed and exits
        Queue: [R3, R4, ..., R11] (9 items)
        Next exit at T=18s

T=18s:  R3 exits
        Queue: [R4, R5, ..., R11] (8 items)
        
...continuing at 1 per 6 seconds...

T=24s:  R4 exits (waited 15 seconds after arriving at T=9s!)
T=30s:  R5 exits (waited 21 seconds!)
T=36s:  R6 exits (waited 27 seconds!)
...
T=66s:  R11 exits (waited 57 SECONDS!)

Result: 8/9 requests processed, but with LONG WAITS
        Latency: R4 = 15s, R5 = 21s, R11 = 57s!
        Success rate: 89% (same as Token Bucket)
        User experience: Slow, frustrating!
```

**Head-to-Head Comparison:**

| Metric | Token Bucket | Leaking Bucket |
|--------|--------------|----------------|
| **Requests Accepted** | 8/9 (89%) | 8/9 (89%) |
| **Requests Rejected** | 1 | 1 |
| **Processing Speed** | FAST (instant) | SLOW (queue delays) |
| **Latency** | ~0-10ms | 15-57 seconds |
| **User Experience** | Excellent | Poor |
| **Burst Handling** | Great | Terrible |
| **Predictability** | Variable | Constant |
| **Best Use Case** | Web APIs | Job queues |

---

### **Decision Framework: Which to Use?**

```
┌─────────────────────────────────────────────────────┐
│ Ask These Questions:                                │
├─────────────────────────────────────────────────────┤
│                                                     │
│ 1. Is this for a USER-FACING API?                  │
│    YES → Token Bucket ✓ (need fast response)        │
│    NO  → Leaking Bucket ✓ (background processing)  │
│                                                     │
│ 2. Do users expect FAST responses?                 │
│    YES → Token Bucket ✓ (milliseconds matter)       │
│    NO  → Leaking Bucket ✓ (seconds fine)            │
│                                                     │
│ 3. Is traffic naturally BURSTY?                    │
│    YES → Token Bucket ✓ (handles spikes)            │
│    NO  → Leaking Bucket ✓ (steady flow)             │
│                                                     │
│ 4. Is backend resource-constrained?                │
│    YES → Leaking Bucket ✓ (steady load)             │
│    NO  → Token Bucket ✓ (more flexible)             │
│                                                     │
│ 5. Need to process JOBS in queue?                  │
│    YES → Leaking Bucket ✓ (FIFO processing)         │
│    NO  → Token Bucket ✓ (immediate processing)      │
│                                                     │
│ DEFAULT: Token Bucket (widely used, more flexible) │
└─────────────────────────────────────────────────────┘
```

**Real-World Analogy:**

```
TOKEN BUCKET = Restaurant Reservation System
├─ You book a table in advance (accumulate slots during quiet hours)
├─ When party arrives, they sit immediately (tokens ready)
├─ Everyone gets good service without waiting
└─ Handles dinner rush gracefully

LEAKING BUCKET = Bank Teller Queue
├─ Customers join a line (queue up)
├─ Teller serves one customer at fixed rate (1 every 6 minutes)
├─ Last customer might wait hours
└─ Steady, predictable processing but terrible customer experience
```

---

### 3. Fixed Window Counter Algorithm

**How It Works:**
- Timeline divided into fixed-sized windows
- Each window has a counter
- Request increments counter
- Once counter reaches threshold, new requests dropped until new window starts

**Example:**
```
Window size: 1 second
Limit: 3 requests per second

Window [1:00-1:01): Counter reaches 3 → excess requests dropped
Window [1:01-1:02): Counter resets to 0
```

**Pros:**
- ✅ Memory efficient
- ✅ Easy to understand
- ✅ Quota resets at end of time window

**Cons:**
- ❌ **Major Issue**: Burst traffic at window edges allows more requests than quota
- ❌ Example: With 5 requests/minute limit:
  - 5 requests between 2:00:00-2:01:00
  - 5 requests between 2:01:00-2:02:00
  - Between 2:00:30-2:01:30 window: 10 requests go through! (2x allowed)

---

### 4. Sliding Window Log Algorithm

**How It Works:**
- Keeps track of request timestamps (usually in Redis sorted sets)
- When new request arrives, removes outdated timestamps (older than current window start)
- Adds new request timestamp to log
- If log size ≤ allowed count, request accepted; otherwise rejected

**Example:**
```
Rate limit: 2 requests per minute

1:00:01 - Request arrives, log empty → ACCEPT (log size = 1)
1:00:30 - Request arrives → ACCEPT (log size = 2)
1:00:50 - Request arrives → REJECT (log size = 3, exceeds limit of 2)
1:01:40 - Request arrives
  - Remove outdated timestamps before 1:00:40 (1:00:01, 1:00:30)
  - Add new timestamp → ACCEPT (log size = 2)
```

**Pros:**
- ✅ Very accurate in any rolling window
- ✅ Requests never exceed rate limit

**Cons:**
- ❌ **High memory consumption** - Stores every rejected request timestamp
- ❌ Not scalable for high-traffic systems

---

### 5. Sliding Window Counter Algorithm (Hybrid Approach)

**How It Works:**
- Combines fixed window counter and sliding window log
- Uses formula to estimate requests in rolling window:
  ```
  Requests in current window + 
  (Requests in previous window × overlap percentage of rolling window)
  ```

**Example:**
```
Rate limit: 7 requests per minute
Previous minute requests: 5
Current minute requests: 3
New request arrives at 30% position in current minute:

Calculation: 3 + (5 × 0.7) = 3 + 3.5 = 6.5 → round down to 6
Since 6 < 7 limit: REQUEST ACCEPTED
```

**Pros:**
- ✅ Smooths out traffic spikes
- ✅ Based on average rate of previous window
- ✅ Memory efficient

**Cons:**
- ❌ Only works for not-so-strict lookback windows
- ❌ Approximation (assumes even distribution)
- ❌ Cloudflare found only 0.003% error rate among 400M requests (acceptable)

---

## Algorithm Comparison Table

| Algorithm | Memory | Accuracy | Complexity | Use Case |
|-----------|--------|----------|-----------|----------|
| Token Bucket | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | Easy | Most common, burst traffic |
| Leaking Bucket | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | Medium | Stable outflow needed |
| Fixed Window | ⭐⭐⭐⭐⭐ | ⭐⭐ | Easy | Simple, but edge issues |
| Sliding Window Log | ⭐⭐ | ⭐⭐⭐⭐⭐ | Complex | High accuracy needed |
| Sliding Window Counter | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | Medium | Good balance |

---

## Architecture Considerations

### Where to Place the Rate Limiter?

#### 1. **Client-Side Implementation** ❌
- **Not recommended** - Clients are unreliable
- Client requests can be forged by malicious actors
- No control over client implementation

#### 2. **Server-Side Implementation** ✅
- Rate limiter placed directly on server
- Added latency to requests
- Coupled with application code

#### 3. **API Gateway/Middleware** ✅ (Recommended)
- Rate limiter as separate middleware
- Decoupled from application
- Supports multiple algorithms
- Popular in microservices architecture

### Storage for Counters

**Why Not Database?**
- ❌ Too slow - disk access latency
- ❌ Not suitable for real-time counters

**Why Redis?** ✅
- ✅ In-memory - fast access
- ✅ Supports time-based expiration
- ✅ Two key commands:
  - `INCR`: Increment counter by 1
  - `EXPIRE`: Set timeout for counter (auto-delete)

### High-Level Architecture Flow

```
1. Client sends request to rate limiter middleware
2. Rate limiter fetches counter from Redis
3. Check if limit reached
   - If YES: Return HTTP 429 (Too Many Requests)
   - If NO: Forward to API servers, increment counter, save to Redis
4. Return response with rate limit headers to client
```

---

## Challenges in Distributed Systems

### 1. Race Condition Problem

**Scenario:**
```
Redis counter value: 3
Two concurrent requests read counter simultaneously

Thread 1: Read 3 → Check (3+1 ≤ limit) → Write 4
Thread 2: Read 3 → Check (3+1 ≤ limit) → Write 4

Result: Counter is 4 (WRONG! Should be 5)
```

**Solutions:**
- **Lua Script** - Atomic operation in Redis
- **Sorted Sets** - Redis data structure for atomic operations
- Avoid using simple locks (too slow)

### 2. Synchronization Issue in Multi-Server Setup

**Problem:**
```
Client 1 → Rate Limiter Server 1 (has client 1's counter)
Client 2 → Rate Limiter Server 2 (has client 2's counter)

Due to stateless design, clients might switch servers:
Client 1 → Rate Limiter Server 2 (doesn't have client 1's data!)
Rate limiter doesn't work properly
```

**Solution:**
- Use **centralized data store** (Redis)
- All rate limiter servers fetch/update counters from same Redis
- ❌ Avoid sticky sessions (not scalable, not flexible)

---

## Additional Considerations

### 1. Rate Limiter Response Headers

Inform clients about their rate limit status:

```
X-Ratelimit-Limit: The max requests allowed per time window
X-Ratelimit-Remaining: Remaining requests in current window
X-Ratelimit-Retry-After: When to retry after being rate limited
```

### 2. Handling Rate-Limited Requests

**Option A: Drop Request**
- Return HTTP 429 immediately
- Client must retry

**Option B: Queue for Later**
- Example: Keep orders in queue to process later
- Prevents loss during overload

### 3. Performance Optimization

#### Multi-Data Center Setup
- Deploy rate limiters in geographically distributed edge servers
- Route traffic to closest edge server
- Reduces latency for global users
- Example: Cloudflare has 194+ edge servers worldwide

#### Eventual Consistency
- Synchronize data across data centers with eventual consistency model
- Trade absolute consistency for performance
- Acceptable for rate limiting use cases

### 4. Monitoring & Analytics

**Track:**
- Is the rate limiting algorithm effective?
- Are the rate limiting rules effective?

**Scenarios:**
- Rules too strict → Many valid requests dropped → Relax rules
- Algorithm ineffective during flash sales → Use Token Bucket for burst traffic

### 5. Hard vs Soft Rate Limiting

- **Hard**: Request count cannot exceed threshold (strict)
- **Soft**: Requests can exceed threshold for short period (flexible)

### 6. Rate Limiting at Different OSI Layers

Not just application level (HTTP):
- **Layer 7 (Application)**: HTTP rate limiting (this chapter)
- **Layer 3 (Network)**: IP address rate limiting using Iptables

### 7. Client Best Practices to Avoid Rate Limiting

- Use client-side cache to avoid frequent API calls
- Understand the limit and don't send too many requests in short time
- Implement exception handling for graceful recovery
- Add sufficient backoff time to retry logic

---

## Summary

### **Algorithm Selection Guide**

| Aspect | Key Takeaway |
|--------|--------------|
| **Best for APIs** | Token Bucket (low latency, burst handling) |
| **Best for Jobs** | Leaking Bucket (steady rate, predictable) |
| **Most Popular** | Token Bucket (Amazon, Google, Stripe, GitHub) |
| **Most Fair** | Leaking Bucket (FIFO queue, first-come-first-served) |
| **Best Placement** | API Gateway/Middleware (decoupled, flexible) |
| **Best Storage** | Redis (fast, TTL support) |
| **Distributed Challenge** | Use Redis for centralized counters + Lua scripts for atomicity |
| **Optimization** | Multi-data center setup + eventual consistency |

### **When to Use Each Algorithm**

**Token Bucket - Use When:** ⭐ (Most Common)
- Building public REST APIs
- Need low latency responses (< 100ms)
- Traffic is naturally bursty
- Want to handle traffic spikes gracefully
- User experience matters
- Examples: Twitter, GitHub, Google APIs

**Leaking Bucket - Use When:**
- Processing background jobs
- Need predictable, constant output rate
- Backend resources are limited
- Processing database operations
- Email sending, batch processing
- Users can tolerate wait times
- Examples: Shopify, email systems, job queues

**Fixed Window - Use When:**
- Simple implementation needed
- Quota resets at specific times
- Edge case handling acceptable
- Not for strict rate limiting

**Sliding Window Log - Use When:**
- Need perfectly accurate rate limiting
- Memory cost acceptable
- No missed requests allowed
- Examples: Strict financial limits

**Sliding Window Counter - Use When:**
- Need good accuracy without huge memory cost
- Want balanced approach
- 0.003% error rate acceptable

### **Quick Comparison Table**

| Algorithm | Speed | Fairness | Memory | Complexity | Best For |
|-----------|-------|----------|--------|-----------|----------|
| **Token Bucket** | 🚀 Very Fast | By speed | ⭐⭐⭐⭐⭐ | Easy | APIs |
| **Leaking Bucket** | 🐢 Slow | By order | ⭐⭐⭐⭐⭐ | Medium | Jobs |
| **Fixed Window** | 🚀 Very Fast | By time | ⭐⭐⭐⭐⭐ | Very Easy | Simple |
| **Sliding Window Log** | ⚡ Medium | Perfect | ⭐⭐ | Complex | Strict |
| **Sliding Window Counter** | ⚡ Medium | Good | ⭐⭐⭐⭐ | Medium | Balanced |

---

## References
- Rate-limiting strategies: https://cloud.google.com/solutions/rate-limiting-strategies-techniques
- Twitter rate limits: https://developer.twitter.com/en/docs/basics/rate-limits
- Google docs API limits: https://developers.google.com/docs/api/limits
- AWS API Gateway throttling: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-request-throttling.html
- Stripe rate limiters: https://stripe.com/blog/rate-limiters
- Shopify rate limits: https://help.shopify.com/en/api/reference/rest-admin-api-rate-limits
- Better Rate Limiting with Redis: https://engineering.classdojo.com/blog/2015/02/06/rolling-rate-limiter/
- Lyft rate limiting: https://github.com/lyft/ratelimit
