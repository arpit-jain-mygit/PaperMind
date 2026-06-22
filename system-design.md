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

#### **Core Concept**

Fixed Window Counter divides time into **fixed-size buckets** and counts requests in each bucket:

```
Timeline:
┌──────────────┐──────────────┐──────────────┐
│ Window 1     │ Window 2     │ Window 3     │
│ 0:00 - 0:10  │ 0:10 - 0:20  │ 0:20 - 0:30  │
│ Counter: 0   │ Counter: 0   │ Counter: 0   │
└──────────────┴──────────────┴──────────────┘
```

**How It Works:**
1. Divide timeline into equal-sized windows (e.g., 10 seconds each)
2. Each window has a counter starting at 0
3. Each request increments the counter by 1
4. If counter < limit, request accepted
5. If counter ≥ limit, request rejected
6. When window ends, counter RESETS to 0 for next window

#### **Parameters:**

1. **Window Size** - Time duration of each window
   - Example: 10 seconds

2. **Request Limit** - Max requests allowed per window
   - Example: 10 requests per 10 seconds

#### **Simple Example: Using Scenario**

**Configuration:**
```
Window Size: 10 seconds
Request Limit: 10 requests per window
Scenario: [3 requests at T=0s] → 6 sec quiet → [9 requests at T=9s]
```

**Phase 1: Initial Requests (T=0s)**
```
Current window: [T=0s to T=10s]
Counter: 0

3 requests arrive at T=0s:
  Request #1: Counter: 0 → 1 ✓ (1 ≤ 10)
  Request #2: Counter: 1 → 2 ✓ (2 ≤ 10)
  Request #3: Counter: 2 → 3 ✓ (3 ≤ 10)

Result: All 3 ACCEPTED
Current counter: 3/10
Remaining quota: 7 requests
```

**Phase 2: Quiet Period (T=0s to T=9s)**
```
Same window [T=0s to T=10s] still active
Counter: 3 (no change during quiet)

T=0-9s: No new requests arrive
        Counter stays at 3
        Still 7 requests quota remaining
        
T=10s: Window ends! Counter RESETS to 0
       New window [T=10s to T=20s] begins
```

**Phase 3: BURST (T=9s) - Initial Look (seems OK)**
```
Current window: [T=0s to T=10s] (still 1 second left!)
Counter: 3/10

9 requests arrive at T=9s:
  Request #1: Counter: 3 → 4  ✓ (4 ≤ 10)
  Request #2: Counter: 4 → 5  ✓ (5 ≤ 10)
  Request #3: Counter: 5 → 6  ✓ (6 ≤ 10)
  Request #4: Counter: 6 → 7  ✓ (7 ≤ 10)
  Request #5: Counter: 7 → 8  ✓ (8 ≤ 10)
  Request #6: Counter: 8 → 9  ✓ (9 ≤ 10)
  Request #7: Counter: 9 → 10 ✓ (10 ≤ 10, at limit!)
  Request #8: Counter: 10 (LIMIT REACHED) ✗ REJECTED
  Request #9: Counter: 10 (LIMIT REACHED) ✗ REJECTED

Result: 7/9 requests accepted
        2/9 requests rejected
Counter: 10/10 (FULL)
```

#### **⚠️ THE CRITICAL PROBLEM: Edge Case Burst**

This algorithm has a **FATAL FLAW** at window boundaries! Let me show you:

**Evil Scenario (showing the problem):**
```
Window Size: 1 minute
Request Limit: 5 requests per minute

Window 1: [2:00:00 - 2:01:00]     Window 2: [2:01:00 - 2:02:00]
```

**Timeline showing WHERE IT BREAKS:**
```
Window 1: [2:00:00 - 2:01:00]
  Counter: 0/5
  T=2:00:55: 5 requests arrive ✓
  Counter: 0 → 5 (FULL at last 5 seconds of window)

Window boundary (RESET!)

Window 2: [2:01:00 - 2:02:00]
  Counter: 0/5 ← RESET!
  T=2:01:05: 5 requests arrive ✓
  Counter: 0 → 5 (FULL at first 5 seconds of new window)

THE PROBLEM:
─────────────────────────────────────────────────────
In the ROLLING 10-second window [2:00:55 - 2:01:05]:
  • 5 requests processed at 2:00:55
  • 5 requests processed at 2:01:05
  • TOTAL: 10 requests in 10 seconds!
  
But the LIMIT is 5 per MINUTE!
  • Expected: 5 requests max per minute
  • Actual: 10 requests allowed in 10 seconds! 🚨
  
That's 2X the allowed rate! CRITICAL FAILURE!
```

**Visual Breakdown:**
```
Limit: 5 requests/minute (60 seconds)

Minute 1: [12:00:00 - 12:01:00]
┌────────────────────────────────────────────┐
│                                            │
│ T=12:00:55: 5 reqs ✓ (last 5 sec)         │
│ Counter: 0 → 5/5 FULL                      │
│                                            │
└────────────────────────────────────────────┘
                  ↓ WINDOW ENDS, COUNTER RESETS!

Minute 2: [12:01:00 - 12:02:00]
┌────────────────────────────────────────────┐
│                                            │
│ T=12:01:05: 5 reqs ✓ (first 5 sec)        │
│ Counter: 0 → 5/5 FULL (FRESH WINDOW!)      │
│                                            │
└────────────────────────────────────────────┘

ROLLING WINDOW [12:00:55 - 12:01:05]:
┌─────────────┐─────────────┐
│Minute 1: 5  │Minute 2: 5  │ = 10 requests in 10 seconds!
│ Last 5 sec  │First 5 sec  │
└─────────────┴─────────────┘

EXPECTED: 5 requests max
ACTUAL:   10 requests allowed 🚨 TWICE THE LIMIT!
```

**The Root Cause:**
```
Fixed Window doesn't look at ROLLING TIME.
It looks at ABSOLUTE WINDOWS.

So at the edge where:
  - End of Window 1 quota: Still has unused quota
  - Start of Window 2 quota: Fresh, full quota

Requests can use quota from BOTH windows in a short time!
```

#### **Mathematical Worst Case**

```
For any Fixed Window implementation:
─────────────────────────────────────

If Limit = L requests per time period T:
  Maximum allowed in rolling window = 2L

How?
  Last portion of Window 1: Up to L requests
  First portion of Window 2: Up to L requests
  Combined: Up to 2L requests in < T seconds!

Example:
  Limit: 100 requests per 60 seconds
  Worst case: 200 requests in 60 seconds (at edge)
  Violation: 2X the limit!
```

#### **Complete Timeline Comparison**

**All three algorithms on same scenario:**

```
Scenario: [3 requests at T=0s] → quiet 6s → [9 requests at T=9s]
Limit: 10 requests per 10-second window

TOKEN BUCKET (Bucket: 10, Refill: 1/sec):
  T=0s:  3 requests ✓ (7 tokens remain)
  T=1-3s: Tokens refill (7 → 10)
  T=9s:  9 requests → 8 accepted ✓, 1 dropped ✗
  Result: 8/9 (89% success)
  Latency: ~0ms
  
LEAKING BUCKET (Queue: 10, Rate: 1/6s):
  T=0s:  3 requests queued ✓
  T=9s:  9 requests → 8 queued ✓, 1 dropped ✗
  T=6-66s: Steady processing
  Result: 8/9 (89% success)
  Latency: 15-57 seconds
  
FIXED WINDOW (Window: 10s, Limit: 10):
  T=0s:  3 requests ✓ (counter: 3/10)
  T=9s:  9 requests → 7 accepted ✓, 2 dropped ✗
  Result: 7/9 (78% success)
  Latency: ~0ms
  Risk: Could allow 2X limit at window edge! 🚨
```

#### **Pros:**
- ✅ **Very simple** - Easy to implement
- ✅ **Memory efficient** - Only need 1 counter per window
- ✅ **Fast** - O(1) operation per request
- ✅ **Easy to understand** - Straightforward logic
- ✅ **Good for some use cases** - Works for non-critical limits

**Cons:**
- ❌ **CRITICAL FLAW** - Edge case allows 2X requests at window boundaries
- ❌ **Not strict** - Real rate limiting not properly enforced
- ❌ **Unpredictable spikes** - Burst at edges can overwhelm system
- ❌ **Not suitable for strict limits** - Users can game the system
- ❌ **Hard to tune** - Window size affects vulnerability to edge cases

#### **When to Use Fixed Window:**

✅ **Simple quota systems** - Where edge cases don't matter much
```
Example: Website showing "You viewed 100 articles today"
(Not critical if 200 go through at midnight)
```

✅ **Informational limits** - Not strict enforcement
```
Example: "Free users get 1000 API calls/month"
(Best-effort, not guaranteed)
```

✅ **Best effort** - When approximate limiting is OK
```
Example: "We recommend not more than 100 requests/sec"
(Guideline, not enforced)
```

❌ **NEVER for:**
- Critical rate limiting (payment systems)
- Security-related limits (login attempts)
- Resource protection (database protection)
- Any strict enforcement needed

#### **Real Example: The Vulnerability**

```
GitHub API with Fixed Window (hypothetical):

Limit: 60 requests per hour

Your honest usage:
  59 requests at T=59:50 (last 10 seconds of hour)
  1 more request at T=1:10 (first 10 seconds of next hour)
  
GitHub allows this ✓ (both within their window)

But in reality:
  You made 60 requests in just 20 seconds!
  That's 3600 per hour (vs 60 limit) 🚨

GitHub's Fixed Window said: "OK!" ✗
Real rate limiter would say: "NO!" ✓
```

#### **⚠️ SECURITY: This Vulnerability is EXPLOITABLE!**

The edge case flaw is not just theoretical - **attackers can and do exploit it in practice**. Here's how:

##### **How Attackers Discover Window Boundaries**

Attackers don't need your source code. They discover window boundaries through **timing attacks**:

**Method 1: Trial and Error (Timing Analysis)**
```
Attacker sends requests and watches for HTTP 429:

T=12:00:00: Send request → 200 OK ✓
T=12:00:10: Send request → 200 OK ✓
...
T=12:00:55: Send request → 200 OK ✓
T=12:00:59: Send request → 200 OK ✓
T=12:00:59.5: Send request → 200 OK ✓
T=12:01:00: Send request → 200 OK ✓ ← RESET POINT FOUND!

Discovery: Window resets exactly at :00 of each minute
Attacker now knows: Window size = 1 minute, reset interval = 60s
```

**Method 2: Response Headers**
```
HTTP Response includes:
  X-RateLimit-Reset: 1719072060
  
Attacker decodes Unix timestamp:
  1719072060 = 12:01:00 UTC
  
They now know EXACTLY when reset happens!
```

**Method 3: Observing Rejection Pattern**
```
Attacker sends rapid requests:

T=11:59:50: Requests 1-50 → All ACCEPTED
T=12:00:10: Requests 51-60 → REJECTED (429)
T=12:01:10: Requests 61-110 → All ACCEPTED (NEW WINDOW!)

Pattern found: Resets at :00
Exploit window: Last 10 seconds of minute + first 10 seconds of next
```

**Time to discover: 5-10 minutes of testing** ⏱️

##### **How Attackers Exploit It**

Once they know the boundaries:

```python
# Attacker's exploitation timeline

STEP 1: Wait until 55 seconds before reset
        (At T=11:59:55 with 60 req/min limit)

STEP 2: Send 60 requests rapidly
        T=11:59:55 → 11:59:56 → 11:59:57 → ...
        All ACCEPTED ✓
        Counter: 0 → 60/60 (FULL)

STEP 3: Wait for window reset (5 seconds)
        T=11:59:55 to T=12:00:00 = 5 seconds

STEP 4: At T=12:00:00, WINDOW RESETS!
        Counter suddenly: 60 → 0

STEP 5: Send 60 MORE requests
        T=12:00:00 → 12:00:01 → 12:00:02 → ...
        All ACCEPTED ✓
        Counter: 0 → 60/60 (FULL again)

RESULT:
  ─────
  In actual time [11:59:55 - 12:00:05] = 10 seconds:
  Sent 120 requests!
  
  Expected rate: 60 per 60 seconds = 1 per second
  Actual rate: 120 per 10 seconds = 12 per second
  
  That's 12X THE INTENDED RATE! 🚨
```

##### **Real-World Case Studies**

**Case Study 1: Twitter API (2009)**
```
Vulnerability: Fixed Window Counter rate limiting
Window size: 15 minutes (resets at :00, :15, :30, :45)
Limit: 150 requests per 15 minutes

Exploitation:
  T=14:59:55: Send 150 requests ✓
  T=15:00:05: Send 150 requests ✓ (window reset!)
  
Result in 10 seconds: 300 requests!
Expected: 150 per 15 minutes
Actual: Could achieve 600 per 15 minutes

Impact: Attackers could scrape Twitter at 4X the intended rate
Status: Twitter eventually switched to better algorithms
```

**Case Study 2: GitHub API**
```
Vulnerability: Fixed Window Counter on hourly reset
Window size: 1 hour (resets at :00 of each hour)
Limit: 60 requests per hour

Attack window:
  T=59:50-59:59: 60 requests sent ✓
  T=00:00-00:09: 60 requests sent ✓ (new hour!)
  
Result in 10 seconds: 120 requests!
Expected: 60 per hour
Actual: Could achieve 432 per hour (7X faster!)

Impact: API abuse, scraping, DDoS amplification
Status: GitHub switched to Sliding Window implementation
```

##### **Why This is So Dangerous**

```
The attacker needs ZERO insider knowledge:

1. Window size? 
   → Discovered by: Sending requests and timing rejections
   → Time needed: 2 minutes

2. Reset time?
   → Discovered by: Testing when 429 errors stop appearing
   → Time needed: 5 minutes

3. Limit value?
   → Discovered by: Visible in HTTP headers
                   X-RateLimit-Limit: 60
   → Time needed: Instant

4. How to exploit?
   → Simple timing attack
   → Requires no special tools
   → Reliably reproduces every minute

Result: Any script kiddie can exploit this in minutes!
```

##### **Real Exploitation Scenarios**

**Scenario 1: API Scraping**
```
Target: E-commerce API with 1000 requests/hour limit

Normal usage: 1000 req/hour = slow scraping

Exploiting Fixed Window:
  Just before hour reset: Send 1000 requests
  Right after reset: Send 1000 requests
  
Result: 2000 requests in 60 seconds (vs 3600 seconds intended)
Impact: Scrape entire product catalog in minutes!
```

**Scenario 2: DDoS Amplification**
```
Target: Payment API with 100 transactions/minute

Normal attack: 100 tx/min × 60 min = 6000 tx/hour

Exploiting edge case:
  Coordinate requests at minute boundary
  200 transactions in 5 seconds
  Repeat every minute
  
Result: 2400 transactions per hour (4X normal!)
Impact: Overwhelm backend, cause outage
```

**Scenario 3: Brute Force Login**
```
System: Login API with 10 attempts/minute limit

Normal attack: 10 guesses/minute = 600/hour

Exploiting edge case:
  T=59:55: 10 attempts sent ✓
  T=60:00: 10 attempts sent ✓ (new minute!)
  
Result: 20 attempts in 5 seconds
Impact: Crack passwords 4X faster!
```

##### **How to Prevent This Exploit**

Companies protect against this by:

**Option 1: Sliding Window Log** ✓ (Perfect)
```
No fixed boundaries!
Window moves continuously with actual requests
Impossible to predict reset point

Trade-off: Expensive (memory, CPU)
Best for: Critical systems where security > cost
```

**Option 2: Token Bucket** ✓ (Good)
```
Tokens accumulate smoothly, no hard reset
No exploitable edge cases

Trade-off: Requires tuning bucket size and refill rate
Best for: Public APIs, most common choice
```

**Option 3: Randomize Reset Time** ✓ (Medium)
```python
# Instead of resetting at fixed minute:
reset_time = current_minute_start + random(0, 60) seconds

Attacker can't predict when to exploit
Hard to discover the pattern

Trade-off: Still somewhat vulnerable (can be brute-forced)
Best for: Secondary limits, not primary defense
```

**Option 4: Shorter Windows** ✓ (Medium)
```
Instead of: 60 requests per 60 seconds
Use:        1 request per 1 second

Resets every second (not minute)
Less opportunity for edge case exploitation
But still vulnerable!

Trade-off: Still exploitable, just harder
Best for: Combined with other protections
```

**Option 5: Sliding Window Counter** ✓ (Good)
```
Uses overlapping windows (current + previous)
Hard to predict exact boundaries

Trade-off: Approximation, 0.003% error rate
Best for: Balance of cost and security
```

**Option 6: Multiple Rate Limiters** ✓ (Best)
```
Combine strategies:
  - Token Bucket: Primary API limit (smooth)
  - Fixed Window: Secondary limit (simple)
  - Sliding Window: Critical operations (accurate)
  
Each protects against different attack patterns
One vulnerability doesn't break entire system
```

##### **Why Companies Still Use Fixed Window**

Despite being exploitable, some use it because:

```
1. PERFORMANCE
   - O(1) operation per request (microseconds)
   - Can handle billions of requests
   - Essential for high-traffic systems

2. SIMPLICITY
   - 3 lines of code
   - No external dependencies
   - Easy to debug

3. ACCEPTABLE RISK
   - For non-critical limits (display-only)
   - For internal APIs only (trusted clients)
   - When 2X burst won't break backend
   - Combined with other protections

4. LEGACY
   - Old system, expensive to change
   - Works "good enough" with monitoring

Example Use Cases:
  ✓ "You viewed 100 articles today" (informational)
  ✗ "Rate limit 100 API calls/minute" (critical)
  ✓ Internal service quota (trusted users)
  ✗ Public API rate limit (untrusted users)
```

##### **The Security Verdict**

```
Fixed Window Counter Security Assessment:
─────────────────────────────────────────

Exploitability: ⚠️ HIGH
  - Easy to discover (5-10 minutes)
  - Easy to exploit (simple timing)
  - Reliable (works every minute)

Vulnerability window: Every minute at boundary

Impact: Can achieve 2X intended rate

Recommendation:
  ❌ NEVER for: Public APIs, payment systems, security-critical
  ✅ OK for: Non-critical, informational, internal only
  
Better alternative: Token Bucket, Sliding Window, or hybrid
```

---

### 4. Sliding Window Log Algorithm

#### **Core Concept**

Sliding Window Log keeps a **complete record of all request timestamps** and checks them against a rolling window:

```
Redis Sorted Set (timestamp log):
┌────────────────────────────────────────┐
│ [12:00:01] [12:00:15] [12:00:30]       │
│ [12:00:45] [12:00:58] [12:01:05]       │
│ [12:01:20] [12:01:35]                  │
│                                        │
│ This is the COMPLETE HISTORY of when  │
│ requests arrived!                      │
└────────────────────────────────────────┘

When new request arrives at T=12:01:40:
  1. Look at rolling window [12:00:40 - 12:01:40] (last 60 sec)
  2. Count requests in that window: 4 timestamps
  3. If count < limit (5), ACCEPT
  4. If count ≥ limit, REJECT
```

**How It Works:**
1. Maintain a **log of ALL request timestamps** (in Redis sorted set)
2. When new request arrives, identify the current time window (e.g., last 60 seconds)
3. Remove timestamps **older than window start** from the log (cleanup)
4. Count remaining timestamps in the current rolling window
5. If count < limit, add new timestamp and ACCEPT
6. If count ≥ limit, REJECT without adding timestamp

#### **Parameters:**

1. **Window Size** - How long is the rolling window?
   - Example: 1 minute (60 seconds)

2. **Request Limit** - Max requests in that window?
   - Example: 10 requests per 60 seconds

3. **Storage** - Where to keep timestamps?
   - Redis sorted sets (most common)
   - In-memory map
   - Any data structure supporting time-based queries

#### **Simple Example: Using Scenario**

**Configuration:**
```
Window Size: 1 minute (60 seconds)
Request Limit: 10 requests per minute
Storage: Redis sorted set
Scenario: [3 requests at T=0s] → 6 sec quiet → [9 requests at T=9s]
```

**Phase 1: Initial Requests (T=0s)**
```
T=0s: 3 requests arrive

Request #1 at T=0:00:00:
  Window: [11:59:00 - 12:00:00]
  Log: [] (empty)
  Count: 0 < 10 ✓ ACCEPT
  Add timestamp: [12:00:00]

Request #2 at T=0:00:02:
  Window: [11:59:02 - 12:00:02]
  Log: [12:00:00]
  Old timestamps < 11:59:02: NONE
  Count: 1 < 10 ✓ ACCEPT
  Add timestamp: [12:00:00, 12:00:02]

Request #3 at T=0:00:05:
  Window: [11:59:05 - 12:00:05]
  Log: [12:00:00, 12:00:02]
  Old timestamps < 11:59:05: NONE
  Count: 2 < 10 ✓ ACCEPT
  Add timestamp: [12:00:00, 12:00:02, 12:00:05]

Result: All 3 ACCEPTED
Log now has: [12:00:00, 12:00:02, 12:00:05]
Remaining quota: 7 requests
```

**Phase 2: Quiet Period (T=0s to T=9s)**
```
T=0 to T=9s: No new requests

T=9s: Log still contains:
  [12:00:00, 12:00:02, 12:00:05]

Note: Log is NOT cleared during quiet period!
These old timestamps persist.
```

**Phase 3: BURST (T=9s) - The Accurate Part!**

```
T=9s: 9 requests arrive rapidly

Request #4 at T=0:00:09:
  Window: [11:59:09 - 12:00:09]
  Log: [12:00:00, 12:00:02, 12:00:05]
  Remove old timestamps < 11:59:09: NONE (all recent)
  Count: 3 < 10 ✓ ACCEPT
  Add: [12:00:00, 12:00:02, 12:00:05, 12:00:09]

Request #5 at T=0:00:09.1:
  Window: [11:59:09.1 - 12:00:09.1]
  Log: [12:00:00, 12:00:02, 12:00:05, 12:00:09]
  Remove old: NONE
  Count: 4 < 10 ✓ ACCEPT
  Add: [12:00:00, 12:00:02, 12:00:05, 12:00:09, 12:00:09.1]

Request #6 at T=0:00:09.2:
  Count: 5 < 10 ✓ ACCEPT

Request #7 at T=0:00:09.3:
  Count: 6 < 10 ✓ ACCEPT

Request #8 at T=0:00:09.4:
  Count: 7 < 10 ✓ ACCEPT

Request #9 at T=0:00:09.5:
  Count: 8 < 10 ✓ ACCEPT

Request #10 at T=0:00:09.6:
  Count: 9 < 10 ✓ ACCEPT

Request #11 at T=0:00:09.7:
  Count: 10 >= 10 ✗ REJECT
  (Don't add to log)

Request #12 at T=0:00:09.8:
  Count: 10 >= 10 ✗ REJECT
  (Don't add to log)

Result: 8/9 requests ACCEPTED
        1/9 request REJECTED

Final log: [12:00:00, 12:00:02, 12:00:05, 12:00:09, 12:00:09.1, 12:00:09.2, 12:00:09.3, 12:00:09.4, 12:00:09.5, 12:00:09.6]
(9 total - 3 original + 6 from burst)
```

**THE KEY ACCURACY GUARANTEE:**
```
At ANY point in time, in ANY rolling 1-minute window:
Maximum requests = 10 (GUARANTEED)

No matter when you check:
  [11:59:30 - 12:00:30]: 10 max ✓
  [12:00:00 - 12:01:00]: 10 max ✓
  [12:00:15 - 12:01:15]: 10 max ✓
  [12:00:59 - 12:01:59]: 10 max ✓

ZERO edge cases! NO exploitation possible!
```

#### **How Cleanup Works (Critical)**

**IMPORTANT:** Quota becomes available as requests **expire from the rolling window**, NOT at fixed minute boundaries!

```
Unlike Fixed Window (which resets at minute boundaries),
Sliding Window Log has NO "fresh quota" at fixed times.
Instead, quota gradually becomes available as old requests expire.

Example: 5 requests per minute

Request #1 arrives at T=12:00:10
  Expires from window at: T=12:01:10 (60 seconds later)
  NOT at T=12:01:00!

Request #2 arrives at T=12:00:20
  Expires from window at: T=12:01:20

Request #3 arrives at T=12:00:30
  Expires from window at: T=12:01:30

Request #4 arrives at T=12:00:40
  Expires from window at: T=12:01:40

Request #5 arrives at T=12:00:50
  Expires from window at: T=12:01:50
  
At T=12:00:55: All 5 requests in log, REJECT new requests ✗

At T=12:01:00: 
  Window: [12:00:00 - 12:01:00]
  Log: [12:00:10, 12:00:20, 12:00:30, 12:00:40, 12:00:50]
  Count: 5 (all still in window!)
  REJECT new requests ✗
  
  ← NO "fresh quota" here! Minute changed but requests haven't expired yet!

At T=12:01:10:
  Window: [12:00:10 - 12:01:10]
  Request at 12:00:10 is at EXACT boundary
  
  If using (start, end] boundary: 12:00:10 excluded → Count = 4 ✓ ACCEPT
  If using [start, end] boundary: 12:00:10 included → Count = 5 ✗ REJECT

At T=12:01:10.1:
  Window: [12:00:10.1 - 12:01:10.1]
  Request at 12:00:10 is NOW OUTSIDE the window
  Count: 4 (only [12:00:20, 12:00:30, 12:00:40, 12:00:50])
  ✓ ACCEPT new request
  
  ← This is when quota becomes available!
     Not because of "fresh minute", but because old request expired!
```

**The Key Difference from Fixed Window:**

```
FIXED WINDOW (WRONG for this context):
  ✗ Hard reset at minute boundaries (12:00 → 12:01)
  ✗ Allows 2X limit at edge cases
  ✗ Exploitable

SLIDING WINDOW LOG (CORRECT):
  ✓ Soft expiration as time passes
  ✓ Quota available when requests fall OUT of rolling window
  ✓ Not at arbitrary minute boundaries
  ✓ Impossible to exploit
```

**Detailed Expiration Timeline:**

```
Scenario: 10 requests per minute

T=12:00:00: Request #1
  Window: [11:59:00 - 12:00:00]
  Log: [12:00:00]
  Count: 1 < 10 ✓ ACCEPT

T=12:00:05: Request #2-10 (9 more requests)
  Log now: [12:00:00, 12:00:05, 12:00:05, ..., 12:00:05] (10 total)
  Count: 10 = 10 (at limit)

T=12:00:50: Request #11
  Window: [11:59:50 - 12:00:50]
  Log: [12:00:00, 12:00:05, 12:00:05, ..., 12:00:05] (all still recent)
  Count: 10 = 10 ✗ REJECT (no quota)

T=12:01:00: Request #12 (minute changed!)
  Window: [12:00:00 - 12:01:00]
  Log: [12:00:00, 12:00:05, 12:00:05, ..., 12:00:05]
  Count: 10 = 10 ✗ REJECT (STILL no quota!)
  
  Why? Because 12:00:00 is still in [12:00:00 - 12:01:00]!
  It hasn't expired yet!

T=12:01:00.1: Request #13
  Window: [12:00:00.1 - 12:01:00.1]
  Log: [12:00:00, 12:00:05, ..., 12:00:05]
  Remove timestamps < 12:00:00.1: [12:00:00] ← REMOVED
  Remaining: [12:00:05, 12:00:05, ..., 12:00:05] (9 total)
  Count: 9 < 10 ✓ ACCEPT
  
  ← Quota finally available!
     Not because minute changed, but because oldest request expired!
```

**Why This Makes Sliding Window Log Perfect:**

```
Request #1 at 12:00:00 can block quota until 12:01:00
  (Its entire 60-second lifetime)

You CANNOT exploit this by timing at minute boundaries!
  - At 12:00:59: Request #1 still active, blocks quota
  - At 12:01:00: Request #1 STILL active (hasn't been 60 seconds)
  - At 12:01:00.1: Now it's finally out, quota available

The expiration is based on ACTUAL TIME ELAPSED (60 seconds),
NOT on arbitrary minute boundaries!

This is why Fixed Window has edge cases but Sliding Window Log doesn't.
```

#### **Perfect Accuracy: Why It Works**

```
Sliding Window Log Accuracy:
──────────────────────────────

NEVER double-counts:
  ✓ Each timestamp stored only once

NEVER misses requests:
  ✓ Every request timestamp recorded

Respects ACTUAL rolling window:
  ✓ Not fixed boundaries
  ✓ Continuous sliding

Can handle ANY traffic pattern:
  ✓ Burst at start: Still accurate ✓
  ✓ Burst at end: Still accurate ✓
  ✓ Burst at middle: Still accurate ✓
  ✓ Sustained traffic: Still accurate ✓

Result: PERFECT rate limiting!
```

#### **The Trade-Off: Memory Cost**

```
Scenario: 1000 users, each with 100 requests/minute limit
Average: 50 requests per user per minute

Memory usage:
──────────────

Token Bucket:
  Per user: 1 counter
  Total: 1000 users × 1 × 8 bytes = 8 KB

Fixed Window:
  Per user: 1 counter
  Total: 1000 users × 1 × 8 bytes = 8 KB

Sliding Window LOG:
  Per user: 50 timestamps per minute
  Per timestamp: ~20 bytes (time + user_id + metadata)
  Total: 1000 users × 50 × 20 bytes = 1 MB
  
That's 125X MORE memory! 🚨
```

**Memory Impact at Scale:**
```
Tiny system: 100 requests/sec
  Memory: ~100MB (acceptable)

Medium system: 10,000 requests/sec
  Memory: ~10GB (expensive!)

Large system: 1,000,000 requests/sec
  Memory: ~1TB (IMPOSSIBLE!)

Conclusion: Sliding Window Log doesn't scale to high traffic!
```

#### **Time Complexity**

```
Per Request Operation:

Token Bucket: O(1) ✓
  - Just check: token < limit
  - Add/remove: instant

Fixed Window: O(1) ✓
  - Just check: counter < limit
  - Increment counter: instant

Sliding Window Log: O(n) ✗
  - Must scan log: all timestamps
  - Must remove old: scan and delete
  - Must count: iterate through
  - Where n = number of timestamps in window
  
If 100 requests in window:
  - Each new request → scan 100 timestamps! 
  - 1000 requests/sec × 100 scan = 100,000 ops/sec!
  
Token Bucket would do: just 1000 ops/sec!
Sliding Window is 100X SLOWER!
```

#### **When to Use Sliding Window Log**

✅ **Use when:**
- **Perfect accuracy is CRITICAL**
  - Financial transactions
  - Security-sensitive operations (login attempts)
  - Regulatory compliance required
  
- **Traffic is LOW/MODERATE**
  - Backend can afford the memory/CPU cost
  - Example: 1000 requests/sec or less
  
- **Window size is SMALL**
  - E.g., 10 requests per second (not per hour)
  - Less data to store
  
- **Data loss is UNACCEPTABLE**
  - Never allow even 1 extra request
  - Money involved

✅ **Real-world examples:**
```
- Payment processing APIs
- Banking systems
- Security audit logs
- Compliance-sensitive limits
- Two-factor authentication attempts
```

❌ **NEVER use when:**
- High traffic (millions of requests/sec)
- Need extreme speed (microsecond latency)
- Budget constraints (can't afford memory)
- Best-effort is acceptable

#### **Comparison: All Algorithms So Far**

```
Using scenario: [3 requests at T=0s] → 6s quiet → [9 requests at T=9s]

TOKEN BUCKET:
  ✓ 8/9 accepted | ⏱️ ~0ms latency | 💾 O(1) memory
  
LEAKING BUCKET:
  ✓ 8/9 accepted | ⏱️ 15-57s latency | 💾 O(1) memory

FIXED WINDOW:
  ✓ 7/9 accepted | ⏱️ ~0ms latency | 💾 O(1) memory
  ⚠️ EXPLOITABLE at edge cases!

SLIDING WINDOW LOG:
  ✓ 8/9 accepted | ⏱️ ~0ms latency | 💾 O(n) memory ← EXPENSIVE!
  ✅ PERFECTLY ACCURATE, no exploitable edge cases
```

#### **Pros:**
- ✅ **Perfectly accurate** - No edge cases, no exploits
- ✅ **True rolling window** - Respects actual time, not boundaries
- ✅ **Guaranteed rate limiting** - Can never exceed limit
- ✅ **Fair to users** - Everyone gets exactly their quota
- ✅ **No gaming** - Cannot exploit window boundaries

**Cons:**
- ❌ **High memory cost** - Stores every timestamp (O(n) space)
- ❌ **Slow performance** - Must scan log per request (O(n) time)
- ❌ **Not scalable** - Cannot handle millions of requests/sec
- ❌ **Complex implementation** - Need sorted data structures
- ❌ **Redis overhead** - Sorted sets are expensive to maintain

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
