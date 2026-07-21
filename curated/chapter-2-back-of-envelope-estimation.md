# System Design Interview - Chapter 2: Back-of-the-Envelope Estimation

## ⚠️ Important Notice

**This document is based on concepts from "System Design Interview" by Alex Xu.**

This is **original content**: summaries, explanations, and real-world examples created to help understand system design concepts. It does not reproduce copyrighted material from the book. All content is restructured and rewritten for clarity and educational purposes.

---

## Table of Contents
1. [Executive Summary](#executive-summary)
2. [Power of Two](#power-of-two)
3. [Latency Numbers](#latency-numbers-every-programmer-should-know)
4. [Availability Numbers](#availability-numbers)
5. [Estimation Techniques](#estimation-techniques)
6. [Real-World Example: Twitter](#real-world-example-twitter)
7. [Tips for Estimation](#tips-for-estimation)

---

## Executive Summary

**Back-of-the-Envelope Estimation** = Rough calculations to understand system requirements using common performance numbers and basic math.

**Why It Matters:**
- Estimate if design meets requirements
- Identify bottlenecks early
- Make architectural decisions based on data, not guessing
- Impress interviewer with structured thinking

**Key Principle:** Process matters more than perfect accuracy. Interviewers want to see your problem-solving approach.

---

## Power of Two

### Why Power of Two?

Data grows exponentially in distributed systems. Understanding powers of 2 helps you quickly estimate:
- Storage capacity
- Memory needed
- Request handling

### Quick Reference Table

```
2^10 = 1 Thousand          (1K)
2^20 = 1 Million           (1M)
2^30 = 1 Billion           (1B)
2^40 = 1 Trillion          (1T)

Byte Units:
1 Byte = 8 bits
1 KB = 1,024 bytes         ≈ 1K
1 MB = 1,024 KB            ≈ 1M
1 GB = 1,024 MB            ≈ 1G
1 TB = 1,024 GB            ≈ 1T
1 PB = 1,024 TB            ≈ 1P
```

### Examples

**ASCII Character:**
```
1 character = 1 byte

"Hello" = 5 bytes

Tweet (280 characters) = 280 bytes
```

**Storage Calculation:**
```
1M users × 1M data per user = 1T storage
1M users × 1G data per user = 1P storage
```

**Memory Needed:**
```
Cache 1M entries × 1KB each = 1GB RAM
Cache 100M entries × 1KB each = 100GB RAM
```

---

## Latency Numbers Every Programmer Should Know

### What Are These Numbers?

Latency = How long an operation takes (in nanoseconds, microseconds, milliseconds)

### Reference Table (As of 2020)

```
Operation                           Time
─────────────────────────────────────────
L1 cache reference                  0.5 ns
Branch mispredict                   5 ns
L2 cache reference                  7 ns
Mutex lock/unlock                   25 ns
Main memory reference               100 ns
Compress 1K with Snappy             2,000 ns
Send 1K bytes over 1 Gbps network   10,000 ns
Read 1 MB from memory               250,000 ns
Round trip within same data center  500,000 ns
Disk seek                           10,000,000 ns
Read 1 MB from disk                 20,000,000 ns
Send packet US East to US West      150,000,000 ns
Send packet US East to Europe       150,000,000 ns
Send packet US East to Asia         150,000,000 ns
```

### Time Units Conversion

```
1 ns (nanosecond)   = 10^-9 seconds
1 µs (microsecond)  = 10^-6 seconds = 1,000 ns
1 ms (millisecond)  = 10^-3 seconds = 1,000 µs
1 s (second)        = 1,000 ms
```

### Key Insights

```
Memory is FAST:
  L1 cache:      0.5 ns
  L2 cache:      7 ns
  Main memory:   100 ns
  
Disk is SLOW:
  Disk seek:     10,000,000 ns
  Disk read:     20,000,000 ns
  
Network is SLOW:
  Same DC:       500,000 ns (0.5 ms)
  Different DC:  150,000,000 ns (150 ms)
```

### Practical Implications

**1. Avoid Disk Seeks**
```
One disk seek = 10 million ns
One memory read = 100 ns

Disk seek is 100,000X slower than memory!
```

**2. Compress Data Before Sending**
```
Compress 1K:      2,000 ns
Send 1K (1Gbps):  10,000 ns
Total:            12,000 ns

Uncompressed 1K:  10,000 ns
Send 1K (1Gbps):  10,000 ns
Total:            20,000 ns

Compression saves 40%! Worth the CPU cost.
```

**3. Network Distance Matters**
```
Same data center: 0.5 ms
Different region: 150 ms (300X slower!)

Solution: Replicate data geographically
```

### Real-World Example: Google Search

```
User types query
        ↓
Query goes to nearest data center: 50ms
        ↓
Search index lookup: 1-5 ms
        ↓
Result aggregation: 10-20 ms
        ↓
Formatting and compression: 5 ms
        ↓
Send back to user: 50 ms
        ↓
Total: ~200 ms
(Feels instant to user!)
```

---

## Availability Numbers

### Definition

**Availability** = Ability of system to stay operational for extended periods.

Measured in **uptime percentage**. Higher = better.

### SLA (Service Level Agreement)

Promise to customers about uptime:

```
99% SLA       = "System will be up 99% of the time"
99.9% SLA     = "System will be up 99.9% of the time" ✓ Industry standard
99.99% SLA    = "System will be up 99.99% of the time"
99.999% SLA   = "System will be up 99.999% of the time" ✓ Only for critical
```

### Uptime vs Downtime

```
99% uptime:
  Downtime per year: 365 × 24 × 60 × 0.01 = 52 minutes
  
99.9% uptime (3 nines):
  Downtime per year: 365 × 24 × 60 × 0.001 = 5.26 minutes
  
99.99% uptime (4 nines):
  Downtime per year: 365 × 24 × 60 × 0.0001 = 31.5 seconds
  
99.999% uptime (5 nines):
  Downtime per year: 365 × 24 × 60 × 0.00001 = 3.15 seconds
```

### Real-World SLAs

```
Amazon AWS:   99.99% (4 nines)
Google Cloud: 99.99% (4 nines)
Microsoft Azure: 99.99% (4 nines)

Netflix relies on multiple services:
  Each at 99.99% → Combined availability lower
  Solution: Circuit breakers, fallbacks, redundancy
```

### How to Achieve High Availability

```
99.9% (3 nines) - Achievable:
  ✓ Database replication (master-slave)
  ✓ Load balancing
  ✓ Basic failover

99.99% (4 nines) - Requires:
  ✓ Multi-region deployment
  ✓ Automated failover
  ✓ Health monitoring
  ✓ Regular backup and recovery testing

99.999% (5 nines) - Extreme:
  ✓ Planetary-scale redundancy
  ✓ Sophisticated monitoring
  ✓ Zero-downtime updates
  ✗ Very expensive ($$$)
```

---

## Estimation Techniques

### Step-by-Step Process

**1. Break Down the Problem**
```
Don't estimate whole system at once
Break into components:
  - Write operations per second
  - Read operations per second
  - Storage needs
  - Cache needs
  - Network bandwidth
```

**2. Make Assumptions Clear**
```
Write them down!
  "Assuming 100 million DAU"
  "Assuming 10:1 read-to-write ratio"
  "Assuming 5 years data retention"
```

**3. Start with Known Numbers**
```
1 billion seconds ≈ 31 years
1 year = 365 days
1 day = 24 hours × 60 minutes × 60 seconds = 86,400 seconds
```

**4. Use Approximation**
```
Don't try to be precise!
99,987 / 9.1 = ?
Approximate as: 100,000 / 10 = 10,000

Close enough for estimation!
```

---

## Real-World Example: Twitter

### Problem Statement

Estimate Twitter's capacity needs:

```
Assumptions:
  - 300 million monthly active users
  - 50% use Twitter daily (DAU)
  - Users post 2 tweets per day on average
  - 10% of tweets contain media (images/videos)
  - Media is stored for 5 years

Questions to answer:
  1. What is tweet posting rate (QPS)?
  2. What is tweet reading rate (QPS)?
  3. How much storage needed?
```

### Estimation: Write Operations

```
Daily Active Users (DAU):
  300M monthly × 50% daily rate = 150M DAU

Tweets per second:
  150M DAU × 2 tweets/day ÷ 24 hours ÷ 3600 seconds
  = 300M ÷ 86,400
  ≈ 3,500 tweets/second

Peak QPS (usually 2× average):
  3,500 × 2 = 7,000 tweets/second
```

### Estimation: Read Operations

```
Assuming 10:1 read-to-write ratio (common for social media)

Read QPS:
  3,500 writes/sec × 10 = 35,000 reads/second

This means:
  Read load is 10× greater than write load
  Need read replicas and caching!
```

### Estimation: Storage

```
Tweets with media per day:
  150M DAU × 2 tweets × 10% with media
  = 30M tweets with media per day

Average media size:
  Image: ~1-2 MB
  Video: ~5-100 MB
  Average: ~1 MB

Storage per day:
  30M tweets × 1 MB = 30 TB per day

Storage for 5 years:
  30 TB/day × 365 days/year × 5 years
  = 30 × 365 × 5
  = 54,750 TB
  ≈ 55 PB (Petabytes)
```

### Real Numbers Analysis

```
55 PB over 5 years
  Cost (assuming $0.20 per GB per month):
  55 × 10^6 GB × $0.20/GB/month × 12 months × 5 years
  ≈ $660 billion
  
This shows why video storage is expensive!
Solution: Use CDN, compress, tiered storage
```

---

## Tips for Estimation

### 1. Rounding and Approximation

```
Don't do exact math. Estimate!

Exact:   99,987 / 9.1 = 10,987.09...
Approx:  100,000 / 10 = 10,000

Error: 1.3% (Acceptable!)
```

### 2. Label Your Units

```
Bad: "System needs 5"
     5 what? KB? MB? GB? TB?

Good: "System needs 5 TB"
      Clear units prevent confusion
```

### 3. Document Assumptions

```
Write down explicitly:
  ✓ Number of users
  ✓ Growth rate
  ✓ Data retention period
  ✓ Ratios (read:write)
  ✓ Geographic distribution
```

### 4. Practice Common Calculations

Memorize these:
```
1 year = 365 days
1 day = 86,400 seconds (≈ 100,000 seconds)
1 hour = 3,600 seconds
1 minute = 60 seconds

QPS for N days of data:
  N × 86,400 seconds per day
  
Example:
  1 billion records in 1 year
  = 1 billion ÷ (365 × 86,400)
  ≈ 31 records per second
```

### 5. Common Bottlenecks to Estimate

When analyzing a system, estimate:
```
✓ QPS (Queries Per Second)
  Peak QPS usually 2X average

✓ Storage
  Per user, per day, multiply by retention period

✓ Cache hit rate
  Affects database load significantly

✓ Network bandwidth
  Required to send data between components

✓ Number of servers needed
  QPS ÷ capacity per server

✓ Memory required
  For cache, sessions, etc.
```

### 6. Sanity Check Your Numbers

```
Does 55 PB for 5 years seem reasonable?
  ✓ 150M DAU × 30MB per user per year = reasonable
  ✓ Price estimate matches industry standards
  ✓ Similar to Netflix, YouTube scale
  
If number seems way off:
  ✓ Re-check calculations
  ✓ Re-examine assumptions
  ✓ Ask interviewer for feedback
```

---

## Quick Reference Estimation Checklist

```
□ Break problem into components
□ Write down assumptions clearly
□ Know power of 2 (2^10, 2^20, 2^30)
□ Know latency numbers (memory vs disk vs network)
□ Calculate QPS for peak traffic (2X average)
□ Estimate storage for retention period
□ Consider read-to-write ratio
□ Use approximation, not exact math
□ Label all units (KB, MB, GB, TB, PB)
□ Sanity check final numbers
□ Identify potential bottlenecks
□ Propose scaling solutions
```

---

## Key Takeaways

```
✓ Estimation is about process, not precision
✓ Know power of 2 and latency numbers by heart
✓ Break problems into manageable pieces
✓ Make assumptions explicit
✓ Use approximation fearlessly
✓ Sanity check your numbers
✓ Think about bottlenecks early
✓ Understand availability implications
✓ Practice, practice, practice
```

---

## References

- System Design Interview by Alex Xu, Chapter 2
- Latency Numbers Every Programmer Should Know: colin-scott.github.io
- Google Pro Tip: Back-of-the-Envelope Calculations
- Cloud Provider SLA Documentation (AWS, Google Cloud, Azure)
