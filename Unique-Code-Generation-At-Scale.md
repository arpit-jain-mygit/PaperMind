# Unique Code Generation at Scale

A practical guide to generating unique referral codes, short URLs, vouchers, or session tokens without race conditions, database bottlenecks, or latency issues.

---

## Table of Contents

1. [The Problem](#the-problem)
2. [Why It's Hard](#why-its-hard)
3. [Approach 1: RDBMS (❌ Fails)](#approach-1-rdbms-fails)
4. [Approach 2: Pre-Generation + Queue (✅ Works)](#approach-2-pre-generation--queue-works)
5. [How It Really Works](#how-it-really-works)
6. [Optimization: Bulk Reading](#optimization-bulk-reading)
7. [When Codes Run Out: Regeneration](#when-codes-run-out-regeneration)
8. [Interview Answer](#interview-answer)

---

## The Problem

### What's a Referral Code?

```
User signs up → Gets unique code: ARPIT123
Shares it → Friend uses it → Referral credit earned

Requirement: Each user needs a DIFFERENT code
Problem: Two users can't have code ARPIT123
```

### Why It's Hard at Scale

```
At 10 users/sec: Easy (check database, assign)
At 1000 users/sec: Breaks (database bottleneck + race conditions)
At 10,000 users/sec: Impossible (cascading failures)
```

---

## Why It's Hard

### The Race Condition

```
Timeline:

t=0:   User 1: Generate code ABC123
       User 2: Generate code ABC123  ← SAME!

t=50ms: User 1: "Is ABC123 taken?" → DB query
        User 2: "Is ABC123 taken?" → DB query

t=100ms: DB response to User 1: "No, it's free"
         DB response to User 2: "No, it's free"  ← BOTH think it's free!

t=150ms: User 1: Assign ABC123
         User 2: Assign ABC123

Result: COLLISION! Two users have same code.
```

### The Bottleneck

```
At 1000 req/sec:
  Each signup = 1 database query
  Query time = 50-100ms
  Database CPU: 100%
  System collapses!
```

---

## Approach 1: RDBMS (❌ Fails)

### The Naive Solution

```sql
CREATE TABLE referral_codes (
    id INT PRIMARY KEY,
    code VARCHAR(10) UNIQUE NOT NULL,
    is_used ENUM('Yes', 'No') DEFAULT 'No',
    assigned_to_user_id INT
);

-- When user signs up:
SELECT code FROM referral_codes WHERE is_used='No' LIMIT 1;
UPDATE referral_codes SET is_used='Yes' WHERE id=1;
INSERT INTO users VALUES (...);
```

### Why It Fails

```
Problem 1: RACE CONDITION
  Multiple threads SELECT same code
  Both think it's free
  Both UPDATE it
  → COLLISION!

Problem 2: DEADLOCK
  Using SELECT FOR UPDATE to prevent race conditions
  Causes circular wait
  Transactions roll back
  → TIMEOUT!

Problem 3: INDEX CONTENTION
  All 1000 queries/sec hit same index
  Database lock on the index
  Latency increases exponentially
  → DATABASE CRASH!

Result:
  ❌ Latency: 50-500ms
  ❌ Success rate: 95%
  ❌ Duplicates: Yes
  ❌ Scaling: Impossible
```

---

## Approach 2: Pre-Generation + Queue (✅ Works)

### The Genius Solution

Instead of checking uniqueness at runtime (slow, error-prone), **guarantee uniqueness before runtime**.

### How It Works (4 Steps)

#### **Step 1: Generate ALL Possible Codes Offline**

```
Code format: 6 characters, alphanumeric (0-9, A-Z, a-z)
Total combinations: 62^6 = 56.8 billion codes

Enough for:
  World population: 8 billion
  Codes per person: 7x over!
  Never run out (for years)

Generate sequentially:
  000000 → 000001 → 000002 → ... → zzzzzz
  Time: ~1 hour (one-time job)
  Output: sequential-ref-code.txt (400GB)
```

#### **Step 2: Randomize**

```
Why? Sequential codes are predictable (000001, 000002...).
     Someone could guess the next user's code.

Solution: Shuffle the file!

$ sort -R sequential-ref-code.txt > randomised-ref-code.txt

Result: Same codes, different random order
```

#### **Step 3: Load into Kafka Queue**

```
Why Kafka?
  ✅ Atomic dequeue (no race conditions)
  ✅ Fault-tolerant (persisted)
  ✅ Horizontal scaling (multiple partitions)
  ✅ No database involved (zero contention)

Architecture:

        Kafka Topic: referral-codes
       /      |      |      \
      P0      P1     P2      P3
     /        |      |       \
  xYz9aB   2kLmOp  Q7rWvX   abc123
   Q7rWv    def456   ghi789   jkl012
   ...      ...      ...      ...
```

#### **Step 4: Consumers Read & Assign**

```
User 1 signs up:
  Consumer reads from P0: Get xYz9aB
  Kafka removes xYz9aB from queue (atomic!)
  Assign to User 1
  Done! (<1ms)

User 2 signs up:
  Consumer reads from P0: Get 2kLmOp (next code)
  Assign to User 2
  Done! (<1ms)

Result:
  ✅ No checking needed (already unique)
  ✅ No database involved (queue handles it)
  ✅ No race conditions (Kafka is atomic)
  ✅ Constant latency (<1ms)
  ✅ Scales to millions/sec
```

---

## How It Really Works

### The Complete Flow

```
[Offline - Run Once]

Generate → Randomize → Load to Kafka
  ↓          ↓           ↓
1 hour    30 min      2 hours
  ↓          ↓           ↓
56.8B     56.8B       Kafka
codes     codes       ready!

[Production - Runs Forever]

User 1 → Kafka P0 → Read xYz9aB → Insert user table ✓
User 2 → Kafka P1 → Read 2kLmOp → Insert user table ✓
User 3 → Kafka P0 → Read Q7rWvX → Insert user table ✓
...all parallel, zero contention!
```

### Marking Codes as "Used"

```
RDBMS approach (❌):
  is_used column in database
  UPDATE query needed
  Race conditions, deadlocks
  Database bottleneck

Pre-gen approach (✅):
  No "is_used" column needed!
  Code is in ONE of two states:
    1. In Kafka queue (not assigned yet)
    2. In user table (assigned)
  
  When code is read from Kafka:
    Kafka automatically removes it (atomic!)
    No database query needed!
  
  Marking = Dequeuing from Kafka!
  No race conditions possible!
```

---

## Optimization: Bulk Reading

### The Problem

```
Naive approach:
  User 1 signup → Read 1 code from Kafka (5ms)
  User 2 signup → Read 1 code from Kafka (5ms)
  ...
  At 1000 req/sec = 1000 Kafka calls/sec!
  
  Latency per user: 5ms
  Network overhead: High
```

### The Solution

```
Read 100 codes at once, cache locally!

Server startup:
  Batch read 100 codes from Kafka (5ms)
  Cache in memory: [xYz9aB, 2kLmOp, Q7rWvX, ...]

User 1 signup: Get from cache: xYz9aB (<1ms)
User 2 signup: Get from cache: 2kLmOp (<1ms)
...
User 100 signup: Get from cache: (last code from batch)

Cache empty? Auto-refill: Read next 100 codes (5ms)

User 101 signup: Get from cache: (first of new batch) (<1ms)
```

### Performance Impact

```
Without bulk reading:
  Kafka calls/sec: 1000
  Latency per user: 5ms

With bulk reading (batch=100):
  Kafka calls/sec: 10
  Latency per user: 0.5ms (5ms amortized over 100 users)
  
Result: 100x fewer Kafka calls!
        10x lower latency!
```

---

## When Codes Run Out: Regeneration

### The Timeline

```
Consumption rate: 1000 users/sec (constant)

Codes per year: 31.536 billion
Total codes: 56.8 billion
Time to exhaust: 56.8B ÷ 31.536B = 1.8 years ≈ 2 years

⚠️ After 2 years, Kafka queue is EMPTY!
```

### Solution: Expand Code Length

```
Current: 6-character codes
  62^6 = 56.8 billion
  Lasts: 2 years

New: 7-character codes
  62^7 = 3.5 trillion
  Lasts: 111 years!

Even better: 8-character codes
  62^8 = 218 trillion
  Lasts: 6900 years (basically infinite)
```

### Regeneration Process

```
Timeline: At ~80% capacity (after 1.5 years)

Month 18: Start regeneration
  Generate 7-char codes: 100 hours
  Randomize: 50 hours
  Validate: 20 hours

Month 19: Load to Kafka
  Load 3.5T codes: 20 hours
  Test: 10 hours

Month 20: Gradual switchover
  Week 1: Route 10% to v2 (7-char), 90% to v1 (6-char)
  Week 2: Route 30% to v2, 70% to v1
  Week 3: Route 50% to v2, 50% to v1
  Week 4: Route 100% to v2, 0% to v1
  
  Zero downtime! Smooth transition!

Month 22: Decommission old codes
  Keep v1 running for 30 days (fallback)
  After 30 days: Safe to delete v1
```

### Automated Monitoring

```python
def monitor_code_exhaustion():
    codes_remaining = kafka_topic.size()
    daily_consumption = calculate_daily_consumption()
    days_remaining = codes_remaining / daily_consumption
    
    if days_remaining > 365:
        status = "🟢 OK"
    elif days_remaining > 180:
        status = "🟡 WARNING - Start planning"
        alert_engineering_team()
    elif days_remaining > 30:
        status = "🔴 CRITICAL - Start now"
        page_on_call()
    else:
        status = "🔥 DISASTER - Emergency codes"
        failover_to_emergency_pool()
    
    log_metrics(days_remaining, status)

# Run daily
schedule.every().day.at("02:00").do(monitor_code_exhaustion)
```

---

## Performance Comparison

### Before vs After

```
BEFORE (RDBMS approach):
  Latency: 150ms (50ms query + 10ms insert)
  Database CPU: 80% (bottleneck)
  Success rate: 95% (timeouts)
  Duplicates: Yes (race conditions)
  Throughput: ~100 req/sec (database limited)

AFTER (Pre-gen + Queue):
  Latency: 1ms (just dequeue)
  Database CPU: 0% (not involved)
  Success rate: 99.99%
  Duplicates: IMPOSSIBLE
  Throughput: 100,000+ req/sec (Kafka limited)

Improvement: 150x faster, 1000x more throughput!
```

---

## Interview Answer

When asked: "How would you design referral code generation at scale?"

> **The Problem:**
> - Generate unique codes for millions of users
> - At high throughput (1000+ req/sec)
> - Without database bottlenecks or race conditions
>
> **Why Runtime Generation Fails:**
> - Check uniqueness → Race conditions (duplicate codes)
> - Check uniqueness → Deadlocks (SELECT FOR UPDATE)
> - Check uniqueness → Database bottleneck (100% CPU)
> - Result: Latency 50-500ms, success rate 95%
>
> **The Solution: Pre-Generation + Queue**
>
> 1. **Offline generation:** Generate all 62^6 (56B) codes
> 2. **Randomize:** Shuffle them (prevent predictability)
> 3. **Load to Kafka:** Multi-partition queue with atomic dequeue
> 4. **Consume:** Read from queue, assign to user (instant, <1ms)
>
> **Why it works:**
> - No uniqueness checking (already guaranteed)
> - No database involved (zero contention)
> - No race conditions (Kafka is atomic)
> - Constant latency (<1ms)
> - Scales linearly (more partitions = more throughput)
>
> **Optimization:** Bulk read 100 codes at once, cache locally
> - Reduces Kafka calls: 1000/sec → 10/sec
> - Reduces latency: 5ms → 0.5ms per user
>
> **When codes run out (after ~2 years):**
> - Expand code length (6 → 7 characters)
> - Pre-generate new batch (3.5 trillion codes)
> - Gradual switchover (zero downtime)
> - New codes last 111 years
>
> **Real numbers:**
> - Latency: 150ms → 1ms (150x faster!)
> - Throughput: 100 req/sec → 100K req/sec
> - Duplicates: Yes → IMPOSSIBLE
> - Cost: Database 100% → 0% CPU
>
> **Applicable to:** Referral codes, short URLs, vouchers, session tokens, any high-volume unique ID generation"

---

## Key Takeaways

```
✅ Pre-generate all codes offline
✅ Randomize to prevent predictability
✅ Use queue (Kafka) for atomic delivery
✅ No database involved in assignment
✅ Bulk read to optimize throughput
✅ Monitor and regenerate before exhaustion
✅ Gradual switchover for zero downtime

Result: Scalable, fast, zero-contention unique code generation!
```

---
