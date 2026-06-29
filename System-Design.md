# System Design: Complete Interview Preparation Guide

A comprehensive guide covering foundational system design concepts and detailed case studies. This guide includes framework for approaching interviews, estimation techniques, scaling strategies, and real-world problem solving.

---

## Table of Contents

### **Part 1: Foundation & Framework**

#### [Chapter 1: Scale from Zero to Millions of Users](#chapter-1-scale-from-zero-to-millions-of-users)
   - Single server setup
   - Scaling web tier  
   - Database replication
   - Caching strategies
   - CDN for static content
   - Stateless architecture
   - Multi-datacenter setup

#### [Chapter 2: Back-of-the-Envelope Estimation](#chapter-2-back-of-the-envelope-estimation)
   - Power of two reference
   - Latency numbers
   - Availability and SLA
   - Estimation techniques
   - Real-world examples

#### [Chapter 3: A Framework for System Design Interviews](#chapter-3-a-framework-for-system-design-interviews)
   - The 4-step framework
   - What interviewers look for
   - Clarification questions
   - High-level design
   - Deep dive strategies
   - Time management
   - Dos and Don'ts

### **Part 2: Case Studies**

#### [Chapter 4: Design a Rate Limiter](#chapter-4-design-a-rate-limiter)
   - Problem statement
   - Five algorithms compared
   - Interview Q&A
   - Implementation guide
   - Distributed systems challenges

#### [Chapter 8: Design a URL Shortener](#chapter-8-design-a-url-shortener)
   - Problem statement
   - Hash-based vs base 62
   - Architecture and scaling
   - Real-world examples

### **Special Sections**

#### [CHUBB Interview Question: Design a Unique Code Generator](#chubb-interview-question-design-a-unique-code-generator)
   - Problem and bottlenecks
   - RDBMS approach (why it fails)
   - Pre-generation + queue solution
   - Performance optimization

---

# Chapter 1: Scale from Zero to Millions of Users

```

### Problem with Single Server

```
✗ No failover: Server goes down = website offline
✗ No scalability: Growth requires upgrading one server (limited)
✗ No redundancy: All data on one machine = disaster if it fails
```

**Next Step:** Separate web and database tiers

---

## Scaling Web Tier

### Vertical vs Horizontal Scaling

**Vertical Scaling (Scale Up):**
```
Add more CPU/RAM to existing server

Pros:
  ✓ Simple

Cons:
  ✗ Hard limit (can't add unlimited CPU/RAM)
  ✗ No failover (one server = single point of failure)
  ✗ High cost ($$$)
```

**Horizontal Scaling (Scale Out):**
```
Add more servers

Pros:
  ✓ No hard limit
  ✓ Failover capability
  ✓ Better cost-effectiveness at scale

Cons:
  ✓ More complexity (distributed system)
```

### Load Balancer

**Problem:**
```
Multiple web servers, but how do requests get distributed?
How do we handle server failures?
```

**Solution: Load Balancer**
```
User → Load Balancer (Public IP)
                ↓
        ┌───────┼───────┐
        ↓       ↓       ↓
    Server1  Server2  Server3
   (Private) (Private) (Private)
```

**How It Works:**
```
1. User connects to public IP of load balancer
2. Load balancer distributes traffic to one of the servers
3. If Server1 fails, load balancer routes to Server2 or Server3
4. Adding new servers automatically spreads load
```

### Benefits

```
✓ Failover: One server down ≠ website down
✓ Horizontal scaling: Add servers as needed
✓ High availability: Users can always access service
```

---

## Scaling Database Tier

### Problem with Single Database

```
✗ No failover: Database down = service down
✗ No read scalability: Single DB handle all read requests
✗ Data loss risk: Single point of failure
```

### Database Replication

**Master-Slave Architecture:**
```
Master Database (Write Operations)
        ↑
        │ Replication
        ↓
Slave1  Slave2  Slave3 (Read Operations)
```

**How It Works:**
```
Write requests (INSERT, UPDATE, DELETE)
        ↓
  Master Database
        ↓
Replicated to Slaves

Read requests (SELECT)
        ↓
Distributed across Slaves
```

### Real-World Example

**Twitter writes:** 3,500 tweets per second
**Twitter reads:** 35,000 requests per second (10:1 ratio)

```
Solution:
  Master: Handles 3,500 writes/sec
  Slaves: Each handles 11,667 reads/sec
  (Multiple slaves share the read load)
```

### Advantages of Replication

```
✓ Better Performance: Reads distributed, writes handled by master
✓ Reliability: Data replicated = survives disasters
✓ High Availability: Service works if one slave fails
```

### Failover Strategy

**If Slave Fails:**
```
Read operations → Master (temporarily)
     ↓
New healthy slave replaces old one
```

**If Master Fails:**
```
One slave promoted to new Master
All operations → New Master
New slave added to replace old master
```

---

## Adding Cache Layer

### Problem

```
Every request hits database
↓
Repetitive database calls for same data
↓
Slow response times, high database load
```

### Solution: Cache Tier

```
Request arrives
        ↓
Check Cache (Redis/Memcached)
        ├─ Cache Hit: Return data immediately (1µs)
        ├─ Cache Miss: Query Database (100µs)
        └─ Store in Cache for next time
```

### Caching Strategy: Read-Through Cache

```
Step 1: Web server checks cache
        ├─ Cache has data? Return it
        └─ Cache miss? Continue to Step 2

Step 2: Query database
        ├─ Get data
        └─ Store in cache

Step 3: Return to client
```

### Real Numbers

**With cache:**
```
Scenario: Popular blog post gets 10,000 reads per hour
  First read: 100µs (from database)
  Remaining 9,999 reads: 1µs each (from cache)
  Total time: 100µs + (9,999 × 1µs) ≈ 10ms
  
Without cache:
  Total time: 10,000 × 100µs = 1000ms = 1 second
  
Speedup: 100X faster!
```

### Cache Considerations

**When to Use Cache:**
```
✓ Data read frequently
✓ Data modified infrequently
✗ Don't use for critical data (RAM = volatile)
```

**Expiration Policy:**
```
Set TTL (Time-to-Live) for cached data

Too short:  System reloads from DB too often (overhead)
Too long:   Data becomes stale (incorrect)
Sweet spot: Balance freshness and performance
```

**Single Point of Failure:**
```
✗ One cache server down = requests to database spike
✓ Solution: Multiple cache servers across regions
```

**Eviction Policy:**
```
Cache is full, new data arrives, which gets removed?

LRU (Least Recently Used): Remove least recently accessed
LFU (Least Frequently Used): Remove least frequently accessed
FIFO (First In First Out): Remove oldest data
```

---

## Content Delivery Network (CDN)

### Problem

```
Static content (images, CSS, JS) served from origin server
↓
Users far from server = slow load
↓
Bandwidth wasted, users frustrated
```

### Solution: CDN (Content Delivery Network)

```
CDN = Network of servers geographically dispersed

User in Los Angeles    User in Europe     User in Tokyo
        ↓                   ↓                  ↓
   CDN in LA          CDN in London      CDN in Tokyo
        └─────────────────┼────────────────┘
                        Origin
                       (S3, etc.)
```

### How CDN Works

```
1. User requests: image.png
   URL: https://mysite.cloudfront.net/logo.jpg

2. CDN server closest to user?
   ├─ Has image in cache? → Return immediately
   └─ Not in cache? → Fetch from origin

3. Origin sends image + TTL (how long to cache)

4. CDN caches image, returns to user

5. Next user in same region gets from cache
```

### Real-World Impact

```
Without CDN:
  User in London requests image from US server
  Distance: ~5000 miles
  Latency: ~150ms per request

With CDN:
  User in London requests from CDN in London
  Distance: ~10 miles
  Latency: ~10ms per request
  
Speedup: 15X faster!
```

### CDN Considerations

**Cost:**
```
Pay for data transferred out of CDN
Infrequently used assets should not be on CDN (waste)
```

**Expiration:**
```
Too short: Reload from origin frequently (high cost)
Too long: Stale content (incorrect)
```

**Fallback:**
```
If CDN fails, request to origin server
Prevents complete blackout
```

---

## Stateless vs Stateful Architecture

### Problem: Server Affinity

```
Stateful (Bad):
  User A connects to Server 1
  User A's session data on Server 1
  User A MUST always go to Server 1
  ↓
  Load balancer must use "sticky sessions"
  ↓
  Hard to scale, fail-over difficult

Example:
  Server 1: User A
  Server 2: User B
  Server 3: User C
  ↓
  If Server 1 down, User A's data lost!
```

### Solution: Stateless Architecture

```
Move session data OUT of web servers
Store in shared persistent storage (database, Redis, etc.)

User A connects to Server 1
        ↓
Server 1 fetches session from shared storage
        ↓
If Server 1 fails, User A can go to Server 2
        ↓
Server 2 fetches same session from shared storage
        ↓
No disruption to user!
```

### Architecture Comparison

**Stateful (Problematic):**
```
Load Balancer
        ↓
    ┌───┼───┬───┐
    ↓   ↓   ↓   ↓
Server1 Server2 Server3 Server4
│ User │ │ User │ │ User │ │ User │
│  A   │ │  B   │ │  C   │ │  D   │
└───────┘ └───────┘ └───────┘ └───────┘

Problem: Each user tied to specific server
```

**Stateless (Solution):**
```
Load Balancer
        ↓
    ┌───┼───┬───┐
    ↓   ↓   ↓   ↓
Server1 Server2 Server3 Server4
(Interchangeable)
        ↓
    Session Store
    (Redis/DB)
        ↓
    User A, B, C, D sessions

Benefit: Any server can handle any user
```

### Benefits

```
✓ Auto-scaling: Add/remove servers without state concerns
✓ Failover: No sticky sessions needed
✓ Horizontal scaling: All servers identical
✓ Easy deployment: New servers immediately useful
```

---

## Multiple Data Centers

### Problem

```
Single data center:
  ✗ Single point of failure
  ✗ Users far away = high latency
  ✗ Natural disasters (earthquake, typhoon)
```

### Solution: Geo-routed Multi-DC Setup

```
User in US    → Route to US Data Center
User in EU    → Route to EU Data Center
User in APAC  → Route to APAC Data Center
```

### How It Works

**Normal Operation:**
```
geoDNS: Resolves domain to nearest data center IP

User location          DNS lookup           Data Center
┌──────────────┐      ┌──────────────┐     ┌──────────────┐
│ Los Angeles  │ ─→ │ geoDNS finds │ ─→ │ US-West DC   │
│              │     │ nearest DC   │     │              │
└──────────────┘     └──────────────┘     └──────────────┘
```

**Failover:**
```
DC1 Offline
        ↓
geoDNS detects outage
        ↓
All traffic redirected to DC2
        ↓
100% traffic to healthy DC
        ↓
service continues
```

### Technical Challenges

**Traffic Redirection:**
```
geoDNS routes based on user location
Detects failures, redirects automatically
```

**Data Synchronization:**
```
Challenge: Data in DC1 might not be in DC2

Solution: Replicate data across DCs
  Synchronous: Wait for all DCs (slow, guaranteed)
  Asynchronous: Send and continue (fast, eventual consistency)

Netflix approach: Asynchronous replication
```

**Testing & Deployment:**
```
Must test across all data centers
Automated deployment tools keep services consistent
```

---

## Message Queue

### Problem: Tight Coupling

```
Direct dependencies between services:
  Web Server → Database (write)
  Web Server → Image Processing (upload)
  Web Server → Email Service (notification)
  
If any service is slow/down, everything stalls
```

### Solution: Message Queue

```
Decouple services with asynchronous messaging

Web Server ─→ Message Queue ─→ Image Processing
                       ↓
                    Email Service
                       ↓
                   Notification Service

Web server doesn't wait for processing!
```

### How It Works

```
1. User uploads image to web server
2. Web server puts message in queue
3. Web server returns response immediately
4. Image processor reads from queue
5. Processes image asynchronously

User gets response in 100ms
Image processing happens in background
```

### Benefits

```
✓ Decoupling: Services independent
✓ Asynchronous: Requests don't wait
✓ Scalability: Add workers independently
✓ Fault tolerance: Service down ≠ message lost
```

---

## Complete Architecture

### Final System Design

```
                        ┌─── CDN ───┐
                        │ (Static)  │
                        └────────┬──┘
                                 │
    Users ─→ geoDNS ─→ Load Balancer
                            │
                ┌───────────┼───────────┐
                ↓           ↓           ↓
            Server1     Server2     Server3
            (Stateless) (Stateless) (Stateless)
                │           │           │
                └───────────┼───────────┘
                            │
            ┌───────────────┼───────────────┐
            ↓               ↓               ↓
        Cache Layer    Session Store    Message Queue
        (Redis)       (Redis/Database)      │
            │               │               ↓
            │               │       ┌───────┼───────┐
            └───────────────┼───────┘       ↓       ↓
                            ↓        Image Service  Email Service
                    Master Database
                            ↓
                    ┌───────┼───────┐
                    ↓       ↓       ↓
                Slave1   Slave2   Slave3
                (Read)   (Read)   (Read)
```

### Data Flow Example

**Write Request (Create Post):**
```
1. User submits post via web form
2. Load balancer routes to available server
3. Server authenticates user (session from session store)
4. Server writes to master database
5. Post replicated to slave databases
6. Put message in queue for image processing
7. Return response to user immediately
8. Image service processes asynchronously
9. Notification service sends updates
```

**Read Request (Load Feed):**
```
1. User requests feed
2. Server checks cache
3. Cache hit? Return from cache (1µs)
4. Cache miss? Query read replica
5. Store result in cache
6. Return to user
```

---

## Key Takeaways

```
✓ Start simple, scale incrementally
✓ Add components based on bottlenecks
✓ Separate concerns (web, cache, database)
✓ Use multiple servers (horizontal scaling)
✓ Replicate data for reliability
✓ Distribute geographically for latency
✓ Decouple services with message queues
✓ Keep state external for stateless servers
✓ Monitor and measure everything
```

---

## References

- System Design Interview by Alex Xu, Chapter 1
- Database Replication Patterns
- Content Delivery Network (CDN) Concepts
- Asynchronous Messaging Patterns

---

# Chapter 2: Back-of-the-Envelope Estimation

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

---

# Chapter 3: A Framework for System Design Interviews

```

---

## What Interviewers Look For

### 1. Collaboration Skills

```
✓ Works with interviewer as a teammate
✓ Asks for feedback actively
✓ Adjusts based on input
✓ Explains reasoning clearly

✗ Works silently without talking
✗ Defensive about ideas
✗ Ignores feedback
```

### 2. Handling Pressure & Ambiguity

```
✓ Stays calm when requirements unclear
✓ Makes reasonable assumptions
✓ Asks clarifying questions
✓ Adapts to changing requirements

✗ Panics when stuck
✗ Gives up easily
✗ Assumes silently without confirming
```

### 3. Communication Ability

```
✓ Explains concepts clearly
✓ Walks interviewer through thinking
✓ Draws diagrams on whiteboard
✓ Labels components clearly
✓ Uses consistent terminology

✗ Stays silent for long periods
✗ Mumbles or unclear speech
✗ Jumps between ideas randomly
```

### 4. Question-Asking Ability

```
✓ Asks to clarify requirements
✓ Questions assumptions
✓ Probes for constraints
✓ Tests understanding

✗ Assumes you know everything
✗ Jumps to implementation
✗ Never asks for clarification
```

### 5. Technical Depth

```
✓ Understands distributed systems concepts
✓ Discusses trade-offs knowledgeably
✓ Considers scalability from start
✓ Identifies bottlenecks

✗ Only knows one approach
✗ Can't explain why choices made
✗ Overengineers unnecessarily
```

### Red Flags to Avoid

```
🚩 Over-engineering: Perfect design that's overkill
   Example: Designing Netflix-scale system for startup

🚩 Narrow-mindedness: Only considers one solution
   Example: "NoSQL is always better" (context dependent!)

🚩 Not listening: Ignores interviewer feedback
   Example: Interviewer says "focus on scalability"
           You spend 30 mins on UI/UX

🚩 No trade-off analysis: Pretends every choice is free
   Example: "We'll use both Kafka and RabbitMQ"
           Without discussing why or trade-offs

🚩 Vague design: Handwavy architecture
   Example: "We'll use the cloud" (which service? how?)
```

---

## The 4-Step Framework

### Overview

```
45-minute interview breakdown:

Step 1: Understand Problem          3-10 minutes
        ↓
Step 2: High-Level Design          10-15 minutes
        ↓
Step 3: Design Deep Dive           10-25 minutes
        ↓
Step 4: Wrap-up                    3-5 minutes
```

**Time is flexible based on problem scope and interviewer feedback!**

---

## Step 1: Understand the Problem and Establish Design Scope

### The Mistake to Avoid

```
❌ Bad: Candidate jumps straight to solution
        "We need load balancer, database replicas, cache..."
        (Without knowing actual requirements!)

✅ Good: Candidate asks clarifying questions first
         "What features are most important?"
         "How many users?"
         "What's the growth timeline?"
```

### Questions to Ask

```
Functional Requirements:
  Q: What specific features must we build?
  Q: Who are the users?
  Q: How do users interact with the system?

Scale and Traffic:
  Q: How many users do we expect?
  Q: What's the traffic volume? (QPS, DAU, etc.)
  Q: What's the expected growth? (3 months, 6 months, 1 year?)

Constraints:
  Q: Any compliance requirements? (GDPR, HIPAA, etc.)
  Q: What's our latency requirement? (< 100ms?)
  Q: What's our availability target? (99.9%?)

Technology Stack:
  Q: What existing services can we leverage?
  Q: Any preferred technologies?
  Q: Cloud or on-premise?
```

### Real Interview Example: Design a News Feed System

```
Candidate: "Is this mobile, web, or both?"
Interviewer: "Both"

Candidate: "What are the key features?"
Interviewer: "Make a post and see friends' posts"

Candidate: "How many friends can a user have?"
Interviewer: "5000"

Candidate: "What's the traffic?"
Interviewer: "10 million DAU"

Candidate: "Can posts have media?"
Interviewer: "Yes, images and videos"

Candidate: "How should the feed be sorted?"
Interviewer: "Reverse chronological order (newest first)"
```

### Document Your Assumptions

```
Write on whiteboard:
  • 10 million daily active users
  • 50% read-to-write ratio
  • Users can have up to 5,000 friends
  • Average session: 30 minutes
  • Spike traffic: 2X average

Why?
  Prevents misunderstandings later
  Gives you reference point during interview
  Shows structured thinking
```

---

## Step 2: Propose High-Level Design and Get Buy-In

### The Goal

```
Create a simple blueprint showing:
  ✓ Major components
  ✓ How they interact
  ✓ Data flow between them
  ✓ Basic architecture
```

### What to Include

**Box Diagram with:**
```
Clients (mobile/web)
    ↓
Load Balancer
    ↓
Web Servers
    ↓
Cache Layer
    ↓
Database
    ↓
Other services
```

### Example: News Feed System High-Level Design

```
Users
  ↓
Load Balancer
  ├─→ Web Server (Timeline API)
  ├─→ Web Server (Feed API)
  └─→ Web Server (Auth API)
  ↓
Database
  └─→ Write to master
  └─→ Read from replicas
  ↓
Cache (Redis)
  └─→ Store frequently accessed feeds
  ↓
Message Queue
  └─→ Async feed updates
```

### Back-of-Envelope Estimation

```
At this step, do rough calculations:

10M DAU × 1 hour average session = 10M requests per hour
= 10M ÷ 3600 = ~2,777 requests per second

Peak: 2,777 × 2 = ~5,500 QPS

How many servers?
Each server handles ~500 QPS
5,500 ÷ 500 = 11 servers (round up to 12)

✓ Store these numbers, reference later
```

### Key Principle: Collaborate

```
✓ "I'm thinking about this architecture. What do you think?"
✓ Draw on whiteboard, explain as you draw
✓ Ask for feedback: "Does this approach work?"
✓ Be open to suggestions: "That's a good point, let me adjust"

✗ "Here's what we're doing" (no discussion)
✗ Draw everything then explain (audience lost)
✗ Defend ideas regardless of feedback
```

### Be Flexible on Detail Level

```
Sometimes interviewer will say:
  "This looks good, but let's focus on X"

Adjust your depth accordingly!
  • Asked about caching? → Deep dive on cache
  • Asked about database? → Deep dive on sharding
  • Asked about scalability? → Deep dive on load handling

Don't waste time on what they don't care about
```

---

## Step 3: Design Deep Dive

### The Goal

```
Take most critical/interesting components and detail them

NOT: Detail every single component equally
YES: Deep dive into interviewer's areas of interest
```

### Time Management

```
You have 10-25 minutes for deep dive
Can't detail everything!

Prioritize based on:
  1. What interviewer explicitly asked about
  2. What shows your strengths
  3. What impacts scalability most
  4. Most interesting technical challenges
```

### Example Deep Dives

**News Feed System Deep Dives:**
```
❌ Don't try to detail:
   - How load balancer works
   - How cache eviction works
   - How database indexing works
   (Unless interviewer asks)

✅ Instead, deep dive into:
   - How to generate feed efficiently
   - How to handle 10M DAU
   - How to reduce latency
   - How to handle hot users
   (Most interesting technical challenges)
```

**URL Shortener Deep Dives:**
```
Option 1: Hash function design
  - Hash + collision resolution
  - Base 62 conversion
  - Trade-offs between approaches

Option 2: Distributed ID generation
  - Snowflake IDs
  - Scalability concerns
  - Failure handling

Option 3: Database optimization
  - Sharding strategy
  - Indexing
  - Query patterns
```

### Structure of Deep Dive

```
For each component:

1. Identify current bottleneck
   "With 1M QPS, single cache server can't keep up"

2. Propose solution
   "Use Redis cluster with sharding"

3. Explain trade-off
   "Faster but adds complexity"

4. Discuss alternatives
   "Could use Memcached, but Redis offers more"

5. Estimate impact
   "Reduces latency from 100ms to 5ms"
```

### Red Flags During Deep Dive

```
🚩 Going too deep on unimportant details
   "Let me explain this obscure configuration..."
   → Focus on what matters for system design

🚩 Not explaining reasoning
   "We use PostgreSQL"
   → Why? Trade-offs vs MySQL? When to choose?

🚩 Oversimplifying
   "We just use AWS, it handles everything"
   → Which AWS services? How? Trade-offs?

🚩 Running out of time
   Plan ahead, don't spend 20 mins on step 2
```

---

## Step 4: Wrap-up

### The Goal

```
Finish strong by showing:
  ✓ System bottlenecks
  ✓ Potential improvements
  ✓ How to handle next scale curve
  ✓ Additional considerations
```

### Topics to Cover

**Identify Bottlenecks:**
```
Q: Where might this system break?
A: "At 100M users:
    - Database reads would slow down
    - Cache hit ratio might drop
    - Network between DCs might saturate"
```

**Discuss Improvements:**
```
Never say "My design is perfect"
Always have ideas for next steps:
  - "If we need 10X capacity, we'd shard the database"
  - "We could move hot data to separate cache"
  - "We could add CDN for static content"
```

**Handle Next Scale Curve:**
```
"If traffic grows 10X:
  - Add read replicas for database
  - Expand cache cluster
  - Implement database sharding
  - Add more data centers"
```

**Error Cases & Resilience:**
```
What if...?
  "Database is down?"
  "Cache server fails?"
  "Network partition between regions?"
  "Sudden traffic spike?"
  
For each: Explain mitigation strategy
```

**Monitoring & Operations:**
```
How would you know if this works?
  - Key metrics to monitor
  - Alerting thresholds
  - Logging strategy
  - Deployment approach
```

### Great Closing Statement

```
"The key trade-off we made is between consistency
and availability. We chose eventual consistency
for better scalability, which is acceptable for
a social media feed. For mission-critical data,
we'd make different choices."

Shows:
  ✓ Thoughtful decision-making
  ✓ Understanding of constraints
  ✓ Maturity in design
  ✓ No false confidence
```

---

## Time Management

### 45-Minute Breakdown (Guide Only)

```
Ideal Case:

Step 1 (5 min):  Understand problem
                 - Ask 3-5 clarifying questions
                 - Document assumptions
                 - Confirm understanding

Step 2 (12 min): High-level design
                 - Draw simple architecture
                 - Do back-of-envelope estimation
                 - Get interviewer buy-in
                 - Identify areas for deep dive

Step 3 (20 min): Design deep dive
                 - Interviewer leads (based on interest)
                 - Detail most important component
                 - Discuss trade-offs
                 - Explain reasoning

Step 4 (4 min):  Wrap-up
                 - Summarize design
                 - Discuss bottlenecks
                 - Mention improvements if time left
                 - Thank interviewer
```

### Adjust Based on Feedback

```
If interviewer says "Focus on scalability":
  Spend more time on Step 3 (deep dive)
  Less time on Step 1 (clarification done)

If interviewer keeps asking questions:
  They want more details
  Go deeper than initially planned

If interviewer looks bored:
  Move faster, less detail
  Jump to next topic
```

### Watch the Clock

```
0:00 - 0:05   Step 1 (understand problem)
0:05 - 0:20   Step 2 (high-level design)
0:20 - 0:40   Step 3 (deep dive)
0:40 - 0:45   Step 4 (wrap-up)

If you're at 0:30 still in Step 1:
  ⚠️ Speed up! You're behind schedule
  Ask fewer questions, propose design
```

---

## Dos and Don'ts

### DOs ✓

```
✓ Always ask for clarification
  "Can you explain what you mean by real-time?"

✓ Make assumptions explicit
  "I'm assuming 1M DAU. Is that correct?"

✓ Communicate constantly
  "I'm thinking about load balancing here..."

✓ Draw diagrams
  Visual communication > words only

✓ Suggest multiple approaches
  "We could use X, Y, or Z. Here's the trade-off..."

✓ Think out loud
  "Let me think... first we need to..."

✓ Listen to interviewer feedback
  "You made a good point about latency..."

✓ Start high-level, then drill down
  "First, broad architecture, then we'll detail..."

✓ Bounce ideas off interviewer
  "What do you think about this approach?"

✓ Never give up
  If stuck: "Let me think about that... any hints?"
```

### DON'Ts ✗

```
✗ Jump to solution without clarifying
  "We need Kafka, Redis, and PostgreSQL"
  (Without understanding what you're building!)

✗ Go deep too early
  "The cache eviction policy should be LRU because..."
  (First, does caching help? How much?)

✗ Pretend to know everything
  "I know exactly how to build this"
  (Red flag! Everyone asks questions)

✗ Be defensive about ideas
  Interviewer: "What about caching?"
  You: "It won't help for this use case"
  (Listen to their idea!)

✗ Ignore time pressure
  "Let me explain one more thing..."
  (Time's up!)

✗ Make up numbers
  Interviewer: "How much storage?"
  You: "Like... 500 TB?"
  (Show your math!)

✗ Assume you understand requirements
  Confirm everything explicitly!

✗ Code or get into implementation details
  "Let me write the algorithm..."
  (System design is architecture, not coding!)

✗ Overcomplicate early
  "We need distributed transactions with Paxos..."
  (Do you really? Justify it!)

✗ Think in silence
  Interviewer gets lost
  "What are you thinking?"
  (Talk out loud!)
```

---

## Example: System Design in Action

### Problem: Design Instagram

**Step 1: Clarify (3 min)**
```
Candidate: "Is this just the mobile app or web too?"
Interviewer: "Both"

Candidate: "What features are most important?"
Interviewer: "Upload photo, see feed, like, comment"

Candidate: "How many users?"
Interviewer: "100M DAU"

Assumptions documented:
  • 100M daily active users
  • Peak traffic: 2X average
  • Photos stored for 10 years
  • Simple feed (reverse chronological)
```

**Step 2: High-Level Design (12 min)**
```
Candidate draws:

                      CDN
                       ↓
                  Load Balancer
                       ↓
    ┌──────────────┬──────────────┬──────────────┐
    ↓              ↓              ↓              ↓
Upload Service  Feed Service  Search Service  Auth Service
    │              │              │              │
    └──────────────┴──────────────┴──────────────┘
                       ↓
                  Cache (Redis)
                       ↓
               Master Database
                       ↓
          Slave1  Slave2  Slave3
                       ↓
               Object Storage (S3)

Candidate: "Back-of-envelope: 100M DAU doing ~1 action/min
           = ~1.6M actions/second peak
           = ~20 servers at 100K QPS each
           = ~50TB storage for 1 day of photos"
```

**Step 3: Deep Dive (20 min)**
```
Interviewer: "Let's focus on photo upload scalability"

Candidate dives into:
  1. Upload flow
     Client → Upload Service → Validation → S3
  2. Thumbnail generation
     Async job after upload
  3. Feed generation
     How to show photos from followed accounts
  4. Scaling at 100M users
     Horizontal scaling strategy
  5. Trade-offs
     Synchronous vs async thumbnail gen
```

**Step 4: Wrap-up (5 min)**
```
Candidate: "Current bottlenecks:
           - Feed generation could be slow
           - Photo storage grows quickly
           
If we scale to 500M users:
  - Database sharding
  - Photo CDN worldwide
  - More async processing
  
Questions?"

Interviewer: "Good job!"
```

---

## Key Takeaways

```
✓ Process matters more than perfect design
✓ Ask questions before jumping to solution
✓ Communicate constantly with interviewer
✓ Use diagrams effectively
✓ Back-of-the-envelope early
✓ Focus deep dive on interesting parts
✓ Discuss trade-offs thoughtfully
✓ Never claim design is perfect
✓ Manage time carefully
✓ Show you can handle feedback
✓ Stay calm under pressure
```

---

## References

- System Design Interview by Alex Xu, Chapter 3
- "Mastering the System Design Interview" talks
- Real interviews from top tech companies (Facebook, Google, Amazon, etc.)

---

# Chapter 4: Design a Rate Limiter

    ↓   ↓   ↓   ↓
Server1 Server2 Server3 Server4
(Interchangeable)
        ↓
    Session Store
    (Redis/DB)
        ↓
    User A, B, C, D sessions

Benefit: Any server can handle any user
```

### Benefits

```
✓ Auto-scaling: Add/remove servers without state concerns
✓ Failover: No sticky sessions needed
✓ Horizontal scaling: All servers identical
✓ Easy deployment: New servers immediately useful
```

---

## Multiple Data Centers

### Problem

```
Single data center:
  ✗ Single point of failure
  ✗ Users far away = high latency
  ✗ Natural disasters (earthquake, typhoon)
```

### Solution: Geo-routed Multi-DC Setup

```
User in US    → Route to US Data Center
User in EU    → Route to EU Data Center
User in APAC  → Route to APAC Data Center
```

### How It Works

**Normal Operation:**
```
geoDNS: Resolves domain to nearest data center IP

User location          DNS lookup           Data Center
┌──────────────┐      ┌──────────────┐     ┌──────────────┐
│ Los Angeles  │ ─→ │ geoDNS finds │ ─→ │ US-West DC   │
│              │     │ nearest DC   │     │              │
└──────────────┘     └──────────────┘     └──────────────┘
```

**Failover:**
```
DC1 Offline
        ↓
geoDNS detects outage
        ↓
All traffic redirected to DC2
        ↓
100% traffic to healthy DC
        ↓
service continues
```

### Technical Challenges

**Traffic Redirection:**
```
geoDNS routes based on user location
Detects failures, redirects automatically
```

**Data Synchronization:**
```
Challenge: Data in DC1 might not be in DC2

Solution: Replicate data across DCs
  Synchronous: Wait for all DCs (slow, guaranteed)
  Asynchronous: Send and continue (fast, eventual consistency)

Netflix approach: Asynchronous replication
```

**Testing & Deployment:**
```
Must test across all data centers
Automated deployment tools keep services consistent
```

---

## Message Queue

### Problem: Tight Coupling

```
Direct dependencies between services:
  Web Server → Database (write)
  Web Server → Image Processing (upload)
  Web Server → Email Service (notification)
  
If any service is slow/down, everything stalls
```

### Solution: Message Queue

```
Decouple services with asynchronous messaging

Web Server ─→ Message Queue ─→ Image Processing
                       ↓
                    Email Service
                       ↓
                   Notification Service

Web server doesn't wait for processing!
```

### How It Works

```
1. User uploads image to web server
2. Web server puts message in queue
3. Web server returns response immediately
4. Image processor reads from queue
5. Processes image asynchronously

User gets response in 100ms
Image processing happens in background
```

### Benefits

```
✓ Decoupling: Services independent
✓ Asynchronous: Requests don't wait
✓ Scalability: Add workers independently
✓ Fault tolerance: Service down ≠ message lost
```

---

## Complete Architecture

### Final System Design

```
                        ┌─── CDN ───┐
                        │ (Static)  │
                        └────────┬──┘
                                 │
    Users ─→ geoDNS ─→ Load Balancer
                            │
                ┌───────────┼───────────┐
                ↓           ↓           ↓
            Server1     Server2     Server3
            (Stateless) (Stateless) (Stateless)
                │           │           │
                └───────────┼───────────┘
                            │
            ┌───────────────┼───────────────┐
            ↓               ↓               ↓
        Cache Layer    Session Store    Message Queue
        (Redis)       (Redis/Database)      │
            │               │               ↓
            │               │       ┌───────┼───────┐
            └───────────────┼───────┘       ↓       ↓
                            ↓        Image Service  Email Service
                    Master Database
                            ↓
                    ┌───────┼───────┐
                    ↓       ↓       ↓
                Slave1   Slave2   Slave3
                (Read)   (Read)   (Read)
```

### Data Flow Example

**Write Request (Create Post):**
```
1. User submits post via web form
2. Load balancer routes to available server
3. Server authenticates user (session from session store)
4. Server writes to master database
5. Post replicated to slave databases
6. Put message in queue for image processing
7. Return response to user immediately
8. Image service processes asynchronously
9. Notification service sends updates
```

**Read Request (Load Feed):**
```
1. User requests feed
2. Server checks cache
3. Cache hit? Return from cache (1µs)
4. Cache miss? Query read replica
5. Store result in cache
6. Return to user
```

---

## Key Takeaways

```
✓ Start simple, scale incrementally
✓ Add components based on bottlenecks
✓ Separate concerns (web, cache, database)
✓ Use multiple servers (horizontal scaling)
✓ Replicate data for reliability
✓ Distribute geographically for latency
✓ Decouple services with message queues
✓ Keep state external for stateless servers
✓ Monitor and measure everything
```

---

## References

- System Design Interview by Alex Xu, Chapter 1
- Database Replication Patterns
- Content Delivery Network (CDN) Concepts
- Asynchronous Messaging Patterns

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

---


## Table of Contents
1. [Executive Summary](#executive-summary)
2. [Understanding the Interview](#understanding-the-interview)
3. [What Interviewers Look For](#what-interviewers-look-for)
4. [The 4-Step Framework](#the-4-step-framework)
5. [Step 1: Understand the Problem](#step-1-understand-the-problem-and-establish-design-scope)
6. [Step 2: High-Level Design](#step-2-propose-high-level-design-and-get-buy-in)
7. [Step 3: Design Deep Dive](#step-3-design-deep-dive)
8. [Step 4: Wrap-up](#step-4-wrap-up)
9. [Time Management](#time-management)
10. [Dos and Don'ts](#dos-and-donts)

---

## Executive Summary

System design interviews simulate **real-world collaboration** where two engineers solve an ambiguous problem together.

**Key Insight:** The final design is LESS important than demonstrating:
- Clear thinking and communication
- Problem-solving approach
- Ability to handle feedback
- Architectural understanding

**The Goal:** Show you can navigate ambiguity, ask good questions, and make sound decisions.

---

## Understanding the Interview

### What It's NOT

```
✗ Not a trivia contest with right/wrong answers
✗ Not about knowing everything
✗ Not about designing Netflix from scratch in 45 minutes
✗ Not about perfect code or detailed implementation
```

### What It IS

```
✓ Collaboration between two engineers
✓ Solving an ambiguous, open-ended problem
✓ Demonstrating design skills and trade-off analysis
✓ Showing communication and problem-solving ability
✓ Proving you can ask the RIGHT questions
```

### Real vs Interview Design

```
Real System (Netflix):
  - Built by 1000+ engineers
  - 10+ years of iteration
  - Billions in infrastructure
  - Unimaginably complex

Interview Design (45 minutes):
  - Focus on core concepts
  - Demonstrate architectural thinking
  - Show scalability patterns
  - Explain trade-offs clearly

You're NOT expected to design the real Netflix!
```

---

## What Interviewers Look For

### 1. Collaboration Skills

```
✓ Works with interviewer as a teammate
✓ Asks for feedback actively
✓ Adjusts based on input
✓ Explains reasoning clearly

✗ Works silently without talking
✗ Defensive about ideas
✗ Ignores feedback
```

### 2. Handling Pressure & Ambiguity

```
✓ Stays calm when requirements unclear
✓ Makes reasonable assumptions
✓ Asks clarifying questions
✓ Adapts to changing requirements

✗ Panics when stuck
✗ Gives up easily
✗ Assumes silently without confirming
```

### 3. Communication Ability

```
✓ Explains concepts clearly
✓ Walks interviewer through thinking
✓ Draws diagrams on whiteboard
✓ Labels components clearly
✓ Uses consistent terminology

✗ Stays silent for long periods
✗ Mumbles or unclear speech
✗ Jumps between ideas randomly
```

### 4. Question-Asking Ability

```
✓ Asks to clarify requirements
✓ Questions assumptions
✓ Probes for constraints
✓ Tests understanding

✗ Assumes you know everything
✗ Jumps to implementation
✗ Never asks for clarification
```

### 5. Technical Depth

```
✓ Understands distributed systems concepts
✓ Discusses trade-offs knowledgeably
✓ Considers scalability from start
✓ Identifies bottlenecks

✗ Only knows one approach
✗ Can't explain why choices made
✗ Overengineers unnecessarily
```

### Red Flags to Avoid

```
🚩 Over-engineering: Perfect design that's overkill
   Example: Designing Netflix-scale system for startup

🚩 Narrow-mindedness: Only considers one solution
   Example: "NoSQL is always better" (context dependent!)

🚩 Not listening: Ignores interviewer feedback
   Example: Interviewer says "focus on scalability"
           You spend 30 mins on UI/UX

🚩 No trade-off analysis: Pretends every choice is free
   Example: "We'll use both Kafka and RabbitMQ"
           Without discussing why or trade-offs

🚩 Vague design: Handwavy architecture
   Example: "We'll use the cloud" (which service? how?)
```

---

## The 4-Step Framework

### Overview

```
45-minute interview breakdown:

Step 1: Understand Problem          3-10 minutes
        ↓
Step 2: High-Level Design          10-15 minutes
        ↓
Step 3: Design Deep Dive           10-25 minutes
        ↓
Step 4: Wrap-up                    3-5 minutes
```

**Time is flexible based on problem scope and interviewer feedback!**

---

## Step 1: Understand the Problem and Establish Design Scope

### The Mistake to Avoid

```
❌ Bad: Candidate jumps straight to solution
        "We need load balancer, database replicas, cache..."
        (Without knowing actual requirements!)

✅ Good: Candidate asks clarifying questions first
         "What features are most important?"
         "How many users?"
         "What's the growth timeline?"
```

### Questions to Ask

```
Functional Requirements:
  Q: What specific features must we build?
  Q: Who are the users?
  Q: How do users interact with the system?

Scale and Traffic:
  Q: How many users do we expect?
  Q: What's the traffic volume? (QPS, DAU, etc.)
  Q: What's the expected growth? (3 months, 6 months, 1 year?)

Constraints:
  Q: Any compliance requirements? (GDPR, HIPAA, etc.)
  Q: What's our latency requirement? (< 100ms?)
  Q: What's our availability target? (99.9%?)

Technology Stack:
  Q: What existing services can we leverage?
  Q: Any preferred technologies?
  Q: Cloud or on-premise?
```

### Real Interview Example: Design a News Feed System

```
Candidate: "Is this mobile, web, or both?"
Interviewer: "Both"

Candidate: "What are the key features?"
Interviewer: "Make a post and see friends' posts"

Candidate: "How many friends can a user have?"
Interviewer: "5000"

Candidate: "What's the traffic?"
Interviewer: "10 million DAU"

Candidate: "Can posts have media?"
Interviewer: "Yes, images and videos"

Candidate: "How should the feed be sorted?"
Interviewer: "Reverse chronological order (newest first)"
```

### Document Your Assumptions

```
Write on whiteboard:
  • 10 million daily active users
  • 50% read-to-write ratio
  • Users can have up to 5,000 friends
  • Average session: 30 minutes
  • Spike traffic: 2X average

Why?
  Prevents misunderstandings later
  Gives you reference point during interview
  Shows structured thinking
```

---

## Step 2: Propose High-Level Design and Get Buy-In

### The Goal

```
Create a simple blueprint showing:
  ✓ Major components
  ✓ How they interact
  ✓ Data flow between them
  ✓ Basic architecture
```

### What to Include

**Box Diagram with:**
```
Clients (mobile/web)
    ↓
Load Balancer
    ↓
Web Servers
    ↓
Cache Layer
    ↓
Database
    ↓
Other services
```

### Example: News Feed System High-Level Design

```
Users
  ↓
Load Balancer
  ├─→ Web Server (Timeline API)
  ├─→ Web Server (Feed API)
  └─→ Web Server (Auth API)
  ↓
Database
  └─→ Write to master
  └─→ Read from replicas
  ↓
Cache (Redis)
  └─→ Store frequently accessed feeds
  ↓
Message Queue
  └─→ Async feed updates
```

### Back-of-Envelope Estimation

```
At this step, do rough calculations:

10M DAU × 1 hour average session = 10M requests per hour
= 10M ÷ 3600 = ~2,777 requests per second

Peak: 2,777 × 2 = ~5,500 QPS

How many servers?
Each server handles ~500 QPS
5,500 ÷ 500 = 11 servers (round up to 12)

✓ Store these numbers, reference later
```

### Key Principle: Collaborate

```
✓ "I'm thinking about this architecture. What do you think?"
✓ Draw on whiteboard, explain as you draw
✓ Ask for feedback: "Does this approach work?"
✓ Be open to suggestions: "That's a good point, let me adjust"

✗ "Here's what we're doing" (no discussion)
✗ Draw everything then explain (audience lost)
✗ Defend ideas regardless of feedback
```

### Be Flexible on Detail Level

```
Sometimes interviewer will say:
  "This looks good, but let's focus on X"

Adjust your depth accordingly!
  • Asked about caching? → Deep dive on cache
  • Asked about database? → Deep dive on sharding
  • Asked about scalability? → Deep dive on load handling

Don't waste time on what they don't care about
```

---

## Step 3: Design Deep Dive

### The Goal

```
Take most critical/interesting components and detail them

NOT: Detail every single component equally
YES: Deep dive into interviewer's areas of interest
```

### Time Management

```
You have 10-25 minutes for deep dive
Can't detail everything!

Prioritize based on:
  1. What interviewer explicitly asked about
  2. What shows your strengths
  3. What impacts scalability most
  4. Most interesting technical challenges
```

### Example Deep Dives

**News Feed System Deep Dives:**
```
❌ Don't try to detail:
   - How load balancer works
   - How cache eviction works
   - How database indexing works
   (Unless interviewer asks)

✅ Instead, deep dive into:
   - How to generate feed efficiently
   - How to handle 10M DAU
   - How to reduce latency
   - How to handle hot users
   (Most interesting technical challenges)
```

**URL Shortener Deep Dives:**
```
Option 1: Hash function design
  - Hash + collision resolution
  - Base 62 conversion
  - Trade-offs between approaches

Option 2: Distributed ID generation
  - Snowflake IDs
  - Scalability concerns
  - Failure handling

Option 3: Database optimization
  - Sharding strategy
  - Indexing
  - Query patterns
```

### Structure of Deep Dive

```
For each component:

1. Identify current bottleneck
   "With 1M QPS, single cache server can't keep up"

2. Propose solution
   "Use Redis cluster with sharding"

3. Explain trade-off
   "Faster but adds complexity"

4. Discuss alternatives
   "Could use Memcached, but Redis offers more"

5. Estimate impact
   "Reduces latency from 100ms to 5ms"
```

### Red Flags During Deep Dive

```
🚩 Going too deep on unimportant details
   "Let me explain this obscure configuration..."
   → Focus on what matters for system design

🚩 Not explaining reasoning
   "We use PostgreSQL"
   → Why? Trade-offs vs MySQL? When to choose?

🚩 Oversimplifying
   "We just use AWS, it handles everything"
   → Which AWS services? How? Trade-offs?

🚩 Running out of time
   Plan ahead, don't spend 20 mins on step 2
```

---

## Step 4: Wrap-up

### The Goal

```
Finish strong by showing:
  ✓ System bottlenecks
  ✓ Potential improvements
  ✓ How to handle next scale curve
  ✓ Additional considerations
```

### Topics to Cover

**Identify Bottlenecks:**
```
Q: Where might this system break?
A: "At 100M users:
    - Database reads would slow down
    - Cache hit ratio might drop
    - Network between DCs might saturate"
```

**Discuss Improvements:**
```
Never say "My design is perfect"
Always have ideas for next steps:
  - "If we need 10X capacity, we'd shard the database"
  - "We could move hot data to separate cache"
  - "We could add CDN for static content"
```

**Handle Next Scale Curve:**
```
"If traffic grows 10X:
  - Add read replicas for database
  - Expand cache cluster
  - Implement database sharding
  - Add more data centers"
```

**Error Cases & Resilience:**
```
What if...?
  "Database is down?"
  "Cache server fails?"
  "Network partition between regions?"
  "Sudden traffic spike?"
  
For each: Explain mitigation strategy
```

**Monitoring & Operations:**
```
How would you know if this works?
  - Key metrics to monitor
  - Alerting thresholds
  - Logging strategy
  - Deployment approach
```

### Great Closing Statement

```
"The key trade-off we made is between consistency
and availability. We chose eventual consistency
for better scalability, which is acceptable for
a social media feed. For mission-critical data,
we'd make different choices."

Shows:
  ✓ Thoughtful decision-making
  ✓ Understanding of constraints
  ✓ Maturity in design
  ✓ No false confidence
```

---

## Time Management

### 45-Minute Breakdown (Guide Only)

```
Ideal Case:

Step 1 (5 min):  Understand problem
                 - Ask 3-5 clarifying questions
                 - Document assumptions
                 - Confirm understanding

Step 2 (12 min): High-level design
                 - Draw simple architecture
                 - Do back-of-envelope estimation
                 - Get interviewer buy-in
                 - Identify areas for deep dive

Step 3 (20 min): Design deep dive
                 - Interviewer leads (based on interest)
                 - Detail most important component
                 - Discuss trade-offs
                 - Explain reasoning

Step 4 (4 min):  Wrap-up
                 - Summarize design
                 - Discuss bottlenecks
                 - Mention improvements if time left
                 - Thank interviewer
```

### Adjust Based on Feedback

```
If interviewer says "Focus on scalability":
  Spend more time on Step 3 (deep dive)
  Less time on Step 1 (clarification done)

If interviewer keeps asking questions:
  They want more details
  Go deeper than initially planned

If interviewer looks bored:
  Move faster, less detail
  Jump to next topic
```

### Watch the Clock

```
0:00 - 0:05   Step 1 (understand problem)
0:05 - 0:20   Step 2 (high-level design)
0:20 - 0:40   Step 3 (deep dive)
0:40 - 0:45   Step 4 (wrap-up)

If you're at 0:30 still in Step 1:
  ⚠️ Speed up! You're behind schedule
  Ask fewer questions, propose design
```

---

## Dos and Don'ts

### DOs ✓

```
✓ Always ask for clarification
  "Can you explain what you mean by real-time?"

✓ Make assumptions explicit
  "I'm assuming 1M DAU. Is that correct?"

✓ Communicate constantly
  "I'm thinking about load balancing here..."

✓ Draw diagrams
  Visual communication > words only

✓ Suggest multiple approaches
  "We could use X, Y, or Z. Here's the trade-off..."

✓ Think out loud
  "Let me think... first we need to..."

✓ Listen to interviewer feedback
  "You made a good point about latency..."

✓ Start high-level, then drill down
  "First, broad architecture, then we'll detail..."

✓ Bounce ideas off interviewer
  "What do you think about this approach?"

✓ Never give up
  If stuck: "Let me think about that... any hints?"
```

### DON'Ts ✗

```
✗ Jump to solution without clarifying
  "We need Kafka, Redis, and PostgreSQL"
  (Without understanding what you're building!)

✗ Go deep too early
  "The cache eviction policy should be LRU because..."
  (First, does caching help? How much?)

✗ Pretend to know everything
  "I know exactly how to build this"
  (Red flag! Everyone asks questions)

✗ Be defensive about ideas
  Interviewer: "What about caching?"
  You: "It won't help for this use case"
  (Listen to their idea!)

✗ Ignore time pressure
  "Let me explain one more thing..."
  (Time's up!)

✗ Make up numbers
  Interviewer: "How much storage?"
  You: "Like... 500 TB?"
  (Show your math!)

✗ Assume you understand requirements
  Confirm everything explicitly!

✗ Code or get into implementation details
  "Let me write the algorithm..."
  (System design is architecture, not coding!)

✗ Overcomplicate early
  "We need distributed transactions with Paxos..."
  (Do you really? Justify it!)

✗ Think in silence
  Interviewer gets lost
  "What are you thinking?"
  (Talk out loud!)
```

---

## Example: System Design in Action

### Problem: Design Instagram

**Step 1: Clarify (3 min)**
```
Candidate: "Is this just the mobile app or web too?"
Interviewer: "Both"

Candidate: "What features are most important?"
Interviewer: "Upload photo, see feed, like, comment"

Candidate: "How many users?"
Interviewer: "100M DAU"

Assumptions documented:
  • 100M daily active users
  • Peak traffic: 2X average
  • Photos stored for 10 years
  • Simple feed (reverse chronological)
```

**Step 2: High-Level Design (12 min)**
```
Candidate draws:

                      CDN
                       ↓
                  Load Balancer
                       ↓
    ┌──────────────┬──────────────┬──────────────┐
    ↓              ↓              ↓              ↓
Upload Service  Feed Service  Search Service  Auth Service
    │              │              │              │
    └──────────────┴──────────────┴──────────────┘
                       ↓
                  Cache (Redis)
                       ↓
               Master Database
                       ↓
          Slave1  Slave2  Slave3
                       ↓
               Object Storage (S3)

Candidate: "Back-of-envelope: 100M DAU doing ~1 action/min
           = ~1.6M actions/second peak
           = ~20 servers at 100K QPS each
           = ~50TB storage for 1 day of photos"
```

**Step 3: Deep Dive (20 min)**
```
Interviewer: "Let's focus on photo upload scalability"

Candidate dives into:
  1. Upload flow
     Client → Upload Service → Validation → S3
  2. Thumbnail generation
     Async job after upload
  3. Feed generation
     How to show photos from followed accounts
  4. Scaling at 100M users
     Horizontal scaling strategy
  5. Trade-offs
     Synchronous vs async thumbnail gen
```

**Step 4: Wrap-up (5 min)**
```
Candidate: "Current bottlenecks:
           - Feed generation could be slow
           - Photo storage grows quickly
           
If we scale to 500M users:
  - Database sharding
  - Photo CDN worldwide
  - More async processing
  
Questions?"

Interviewer: "Good job!"
```

---

## Key Takeaways

```
✓ Process matters more than perfect design
✓ Ask questions before jumping to solution
✓ Communicate constantly with interviewer
✓ Use diagrams effectively
✓ Back-of-the-envelope early
✓ Focus deep dive on interesting parts
✓ Discuss trade-offs thoughtfully
✓ Never claim design is perfect
✓ Manage time carefully
✓ Show you can handle feedback
✓ Stay calm under pressure
```

---

## References

- System Design Interview by Alex Xu, Chapter 3
- "Mastering the System Design Interview" talks
- Real interviews from top tech companies (Facebook, Google, Amazon, etc.)

---

(Interchangeable)
        ↓
    Session Store
    (Redis/DB)
        ↓
    User A, B, C, D sessions

Benefit: Any server can handle any user
```

### Benefits

```
✓ Auto-scaling: Add/remove servers without state concerns
✓ Failover: No sticky sessions needed
✓ Horizontal scaling: All servers identical
✓ Easy deployment: New servers immediately useful
```

---

## Multiple Data Centers

### Problem

```
Single data center:
  ✗ Single point of failure
  ✗ Users far away = high latency
  ✗ Natural disasters (earthquake, typhoon)
```

### Solution: Geo-routed Multi-DC Setup

```
User in US    → Route to US Data Center
User in EU    → Route to EU Data Center
User in APAC  → Route to APAC Data Center
```

### How It Works

**Normal Operation:**
```
geoDNS: Resolves domain to nearest data center IP

User location          DNS lookup           Data Center
┌──────────────┐      ┌──────────────┐     ┌──────────────┐
│ Los Angeles  │ ─→ │ geoDNS finds │ ─→ │ US-West DC   │
│              │     │ nearest DC   │     │              │
└──────────────┘     └──────────────┘     └──────────────┘
```

**Failover:**
```
DC1 Offline
        ↓
geoDNS detects outage
        ↓
All traffic redirected to DC2
        ↓
100% traffic to healthy DC
        ↓
service continues
```

### Technical Challenges

**Traffic Redirection:**
```
geoDNS routes based on user location
Detects failures, redirects automatically
```

**Data Synchronization:**
```
Challenge: Data in DC1 might not be in DC2

Solution: Replicate data across DCs
  Synchronous: Wait for all DCs (slow, guaranteed)
  Asynchronous: Send and continue (fast, eventual consistency)

Netflix approach: Asynchronous replication
```

**Testing & Deployment:**
```
Must test across all data centers
Automated deployment tools keep services consistent
```

---

## Message Queue

### Problem: Tight Coupling

```
Direct dependencies between services:
  Web Server → Database (write)
  Web Server → Image Processing (upload)
  Web Server → Email Service (notification)
  
If any service is slow/down, everything stalls
```

### Solution: Message Queue

```
Decouple services with asynchronous messaging

Web Server ─→ Message Queue ─→ Image Processing
                       ↓
                    Email Service
                       ↓
                   Notification Service

Web server doesn't wait for processing!
```

### How It Works

```
1. User uploads image to web server
2. Web server puts message in queue
3. Web server returns response immediately
4. Image processor reads from queue
5. Processes image asynchronously

User gets response in 100ms
Image processing happens in background
```

### Benefits

```
✓ Decoupling: Services independent
✓ Asynchronous: Requests don't wait
✓ Scalability: Add workers independently
✓ Fault tolerance: Service down ≠ message lost
```

---

## Complete Architecture

### Final System Design

```
                        ┌─── CDN ───┐
                        │ (Static)  │
                        └────────┬──┘
                                 │
    Users ─→ geoDNS ─→ Load Balancer
                            │
                ┌───────────┼───────────┐
                ↓           ↓           ↓
            Server1     Server2     Server3
            (Stateless) (Stateless) (Stateless)
                │           │           │
                └───────────┼───────────┘
                            │
            ┌───────────────┼───────────────┐
            ↓               ↓               ↓
        Cache Layer    Session Store    Message Queue
        (Redis)       (Redis/Database)      │
            │               │               ↓
            │               │       ┌───────┼───────┐
            └───────────────┼───────┘       ↓       ↓
                            ↓        Image Service  Email Service
                    Master Database
                            ↓
# 1. Unique Code Generation at Scale

A practical guide to generating unique referral codes, short URLs, vouchers, or session tokens without race conditions, database bottlenecks, or latency issues.

---

## The Problem

### What's a Referral Code?


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

---

# Chapter 4: Design a Rate Limiter

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

---

# Chapter 8: Design a URL Shortener

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

---

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


---

# Chapter 8: Design a URL Shortener

## Table of Contents
1. [Executive Summary](#executive-summary)
2. [Problem Statement](#problem-statement)
3. [Why URL Shortening Matters](#why-url-shortening-matters)
4. [Back of Envelope Estimation](#back-of-envelope-estimation)
5. [High-Level Design](#high-level-design)
6. [Hash Functions & Algorithms](#hash-functions--algorithms)
7. [Algorithm Decision Matrix](#algorithm-decision-matrix)
8. [Deep Dive Design](#deep-dive-design)
9. [Architecture Decisions](#architecture-decisions)
10. [Interview Q&A](#interview-qa)

---

## Executive Summary

A **URL shortener** converts long URLs into short, memorable aliases. When users click the short URL, they're redirected to the original long URL.

**Example:**
```
Long URL:  https://www.systeminterview.com/q=chatsystem&c=loggedin&v=v3&l=long
Short URL: https://tinyurl.com/y7keocwj
```

**Real-World Examples:** TinyURL, bit.ly, goo.gl, ow.ly

---

## Problem Statement

### Core Requirements

**Functional Requirements:**
1. **URL Shortening** - Given a long URL → return a much shorter URL
2. **URL Redirecting** - Given a short URL → redirect to the original URL
3. **Custom short URLs** (optional) - Users can create custom aliases
4. **URL Expiration** (optional) - Short URLs can expire after a time period

**Non-Functional Requirements:**
1. High availability - System must be always operational
2. Scalability - Must handle billions of URLs
3. Fault tolerance - Handles failures gracefully
4. Low latency - Redirects must be fast (< 100ms)

### Design Scope Questions

```
Q: What is the traffic volume?
A: 100 million URLs are generated per day

Q: How long should the shortened URL be?
A: As short as possible (preferably < 10 characters)

Q: What characters are allowed?
A: 0-9, a-z, A-Z (62 possible characters)

Q: Can shortened URLs be deleted or updated?
A: For simplicity, no (assume immutable)

Q: What is the read-to-write ratio?
A: Typically 10:1 (many more redirects than creations)
```

---

## Why URL Shortening Matters

1. **Shorter URLs** - Easy to share on social media (Twitter, etc.)
2. **Analytics** - Track clicks, sources, user behavior
3. **Bandwidth Saving** - Shorter URLs use less bandwidth in messages
4. **Branded Links** - Custom short URLs for marketing
5. **Obfuscation** - Hide actual URLs from users

---

## Back of Envelope Estimation

### Traffic Estimates

```
Write operations (URL shortening):
  100 million URLs per day
  Per second: 100M / 24 / 3600 = 1,160 requests/sec

Read operations (URL redirecting):
  Assuming 10:1 read-to-write ratio
  Per second: 1,160 × 10 = 11,600 requests/sec

Storage over 10 years:
  URLs per year: 100M × 365 = 36.5 billion
  URLs per 10 years: 36.5B × 10 = 365 billion URLs
  
  Storage needed (assuming 100 bytes per URL):
  365B × 100 bytes = 36.5 TB per year
  365B × 100 bytes × 10 years = 365 TB total
```

### Hash Value Length

```
Hash value uses 62 characters: [0-9, a-z, A-Z]
  0-9: 10 characters
  a-z: 26 characters
  A-Z: 26 characters
  Total: 62 characters

Find smallest n where 62^n ≥ 365 billion:
  62^1 = 62
  62^2 = 3,844
  62^3 = 238,328
  62^4 = 14.7 million
  62^5 = 916 million
  62^6 = 56.8 billion
  62^7 = 3.5 trillion ✓

Required length: 7 characters
(62^7 = 3.5 trillion > 365 billion URLs needed)
```

---

## High-Level Design

### API Endpoints

**1. URL Shortening**
```
POST /api/v1/data/shorten
Request:  { "longUrl": "https://..." }
Response: { "shortUrl": "https://tinyurl.com/y7keocwj" }
```

**2. URL Redirecting**
```
GET /api/v1/shortUrl
Request:  /y7keocwj
Response: 301 or 302 redirect to original URL
```

### URL Redirecting Flow

```
User clicks: https://tinyurl.com/y7keocwj
    ↓
Load Balancer routes to Web Server
    ↓
Web Server checks cache
    ├─ If found: Return 301/302 redirect with original URL
    ├─ If not found: Query database
    ├─ If exists in DB: Cache it, return redirect
    └─ If not exists: Return 404
```

### 301 vs 302 Redirect

```
301 Redirect (Permanent):
  ✓ Browser caches the response
  ✓ Subsequent requests DON'T go to URL shortener service
  ✓ Reduces server load
  ✗ Can't track analytics accurately
  Use when: Server load is top priority

302 Redirect (Temporary):
  ✓ Browser doesn't cache
  ✓ Every request goes to URL shortener service
  ✓ Can track every click for analytics
  ✗ Higher server load
  Use when: Analytics is important
```

---

## Hash Functions & Algorithms

### Algorithm 1: Hash + Collision Resolution

**What It Does:**
```
1. Apply hash function (MD5, SHA-1, CRC32) to long URL
2. Take first 7 characters of hash
3. Check if short URL already exists in database
   ├─ If exists: append character and hash again
   └─ If not exists: use this short URL
```

**Problem Solved:**
```
❌ Previous: How to generate short URLs from long URLs?
✓ Now: Hash functions can map any string to fixed-length hash

Problem Created:
  Hash outputs are longer than 7 characters
  Need to truncate, but causes collisions
```

**How Collision Resolution Works:**

```
Example: https://en.wikipedia.org/wiki/Systems_design

Step 1: Apply MD5 hash
  MD5: 5eb63bbbe01eeed093cb22bb8f5acdc3

Step 2: Take first 7 characters
  Result: 5eb63bb
  Check database: Already exists! (collision!)

Step 3: Append predefined string (e.g., "1") and hash again
  Input: 5eb63bbbe01eeed093cb22bb8f5acdc31
  New hash: abc123... 
  Result: abc1234
  Check database: Not exists! ✓ Use this!
```

**Pros:**
- ✅ Simple to understand
- ✅ Deterministic (same input always gives same output)
- ✅ Works with any long URL

**Cons:**
- ❌ Database lookup on every request (collision check is expensive)
- ❌ Multiple lookups if collisions happen
- ❌ Need Bloom filter to optimize

**When to Use:**
- When collision resolution is acceptable
- When you have good infrastructure (fast DB)
- When you can use Bloom filters for optimization

**Pseudo-code:**
```python
def shorten_url_hash(long_url):
    # Apply hash function
    hash_value = md5(long_url)
    
    # Take first 7 characters
    short_url = hash_value[:7]
    
    # Check if collision exists
    counter = 0
    while db.exists(short_url):
        counter += 1
        # Append counter and hash again
        new_input = hash_value + str(counter)
        hash_value = md5(new_input)
        short_url = hash_value[:7]
    
    # Save to database
    db.save(short_url, long_url)
    return short_url
```

---

### Algorithm 2: Base 62 Conversion ⭐ (Recommended)

**What It Does:**
```
1. Generate globally unique ID (from ID generator service)
2. Convert this ID to base 62 representation
3. Result is short URL
```

**Problem Solved:**
```
❌ Previous (Hash + Collision): 
  - Multiple database lookups for collision resolution
  - Expensive collision detection
  - Non-sequential short URLs

✓ Now (Base 62):
  - No collisions (each ID is unique by definition!)
  - O(1) conversion (no database lookup)
  - Guaranteed unique short URLs
  - Sequential IDs = easier to debug
```

**How Base 62 Conversion Works:**

```
Base 62 uses 62 characters: 0-9, a-z, A-Z

Mapping:
  0-9 → 0-9
  10-35 → a-z
  36-61 → A-Z

Example: Convert ID 2009215674938 to base 62

Step 1: Divide by 62 repeatedly
  2009215674938 ÷ 62 = 32406382825 remainder 2
  32406382825 ÷ 62 = 522357465 remainder 3
  522357465 ÷ 62 = 8425441 remainder 11
  ...continue...
  
Step 2: Map remainders to characters
  [2, 55, 59, ...] → [2, T, X, ...]
  
Step 3: Reverse and use as short URL
  Result: zn9edcu
  
Full URL: https://tinyurl.com/zn9edcu
```

**Pros:**
- ✅ No collisions (unique ID = unique short URL)
- ✅ Very fast (O(1) - just math, no DB lookup)
- ✅ Deterministic (same ID always gives same short URL)
- ✅ Reversible (can convert back to original ID)
- ✅ Sequential IDs (easier debugging/analytics)

**Cons:**
- ❌ Requires reliable unique ID generator
- ❌ ID generator is critical dependency (single point of failure?)
- ❌ Short URLs are not meaningful (random-looking)

**When to Use:**
- ✅ Default choice (best performance)
- ✅ When you need scalability (10K+ req/sec)
- ✅ When collisions must be avoided 100%
- ✅ When you have a working ID generator service

**Pseudo-code:**
```python
def shorten_url_base62(long_url):
    # Step 1: Check if URL already exists
    existing = db.get_short_url_by_long_url(long_url)
    if existing:
        return existing  # Return cached result
    
    # Step 2: Generate unique ID
    unique_id = id_generator.generate()  # Returns 2009215674938
    
    # Step 3: Convert ID to base 62
    short_url = base62_encode(unique_id)  # Returns "zn9edcu"
    
    # Step 4: Save to database
    db.save(short_url, long_url, unique_id)
    
    return short_url

def base62_encode(num):
    characters = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    result = ""
    
    while num > 0:
        result = characters[num % 62] + result
        num = num // 62
    
    return result if result else "0"
```

---

## Algorithm Decision Matrix

```
┌─────────────────────────┬──────────────────┬─────────────────────┐
│ Aspect                  │ Hash + Collision │ Base 62 Conversion  │
├─────────────────────────┼──────────────────┼─────────────────────┤
│ Collision Handling      │ Recursive lookup │ None (ID is unique) │
│ Speed                   │ O(n) - multiple  │ O(1) - just math    │
│                         │ DB lookups       │                     │
│ Database Dependency     │ High (for checks)│ Low (just storage)  │
│ ID Generator Dependency │ None             │ Yes (critical!)     │
│ URL Predictability      │ Good (hash-based)│ Poor (random-like)  │
│ Analytics Tracking      │ Easy (sequential)│ Medium (by ID)      │
│ Scalability             │ Limited          │ Excellent           │
├─────────────────────────┼──────────────────┼─────────────────────┤
│ RECOMMENDATION          │ Use for < 1K/sec│ Use for > 1K/sec   │
└─────────────────────────┴──────────────────┴─────────────────────┘

CHOOSE: Base 62 Conversion (recommended for production)
```

---

## Deep Dive Design

### Data Model

```
Table: url_mapping

Columns:
  id          INT (unique identifier)
  short_url   VARCHAR(7) UNIQUE
  long_url    VARCHAR(2048)
  created_at  TIMESTAMP
  expires_at  TIMESTAMP (optional)
  user_id     INT (optional - for analytics)

Example row:
  id: 2009215674938
  short_url: zn9edcu
  long_url: https://en.wikipedia.org/wiki/Systems_design
  created_at: 2024-06-24 10:30:00
  user_id: 12345
```

### URL Shortening Flow (Complete)

```
Client sends: POST /api/v1/data/shorten
              { "longUrl": "https://en.wikipedia.org/wiki/Systems_design" }

Step 1: Load Balancer routes to Web Server

Step 2: Check if URL already exists
        SELECT short_url WHERE long_url = ?
        If exists: Return it (avoid duplicate work)

Step 3: Generate Unique ID
        unique_id = distributed_id_generator.nextId()
        Result: 2009215674938

Step 4: Convert ID to Base 62
        short_url = base62_encode(2009215674938)
        Result: zn9edcu

Step 5: Save to Database
        INSERT INTO url_mapping (id, short_url, long_url)
        VALUES (2009215674938, 'zn9edcu', 'https://...')

Step 6: Return to Client
        Response: { "shortUrl": "https://tinyurl.com/zn9edcu" }
```

### URL Redirecting Flow (Complete)

```
Client clicks: https://tinyurl.com/zn9edcu

Step 1: Load Balancer routes to Web Server

Step 2: Check Cache (Redis/Memcached)
        cache.get("zn9edcu")
        ├─ Cache Hit: Return longUrl immediately
        │  Latency: ~5ms
        └─ Cache Miss: Continue to Step 3

Step 3: Query Database
        SELECT long_url WHERE short_url = 'zn9edcu'
        Result: https://en.wikipedia.org/wiki/Systems_design

Step 4: Update Cache
        cache.set("zn9edcu", longUrl, ttl=3600)

Step 5: Return Redirect Response
        HTTP 302 (or 301)
        Location: https://en.wikipedia.org/wiki/Systems_design

Step 6: Browser Redirects
        User sees original URL in address bar
```

### Caching Strategy

```
Why Cache?
  Read-to-write ratio: 10:1
  Most requests are redirects, not creations
  Caching dramatically reduces database load

Cache Type: LRU (Least Recently Used)
  Keep most popular URLs in cache
  Evict unused URLs when cache full

Cache Hit Rate: ~99% (very popular URLs)
  Example: Viral link gets millions of clicks
  First click: DB query
  Next million clicks: Cache hits

Cache Configuration:
  Time-to-Live (TTL): 1 hour
  Max size: 1 million entries (fit in memory)
  Eviction policy: LRU
```

---

## Architecture Decisions

### Web Server

```
Stateless Design:
  ✓ Can add/remove servers without state transfer
  ✓ Easy horizontal scaling
  ✓ No server affinity needed

Load Balancing:
  ✓ Distribute requests across multiple web servers
  ✓ Handle 11,600 requests per second
  ✓ Round-robin or consistent hashing

Scaling Strategy:
  Normal: 10 web servers
  Peak: 20+ web servers (auto-scale on demand)
```

### Database

```
Master-Slave Replication:
  Master: Handles all writes (URL shortening)
  Slave 1, 2, 3: Read replicas (URL redirecting)
  
  Benefit:
    ✓ Read requests go to replicas
    ✓ Reduces load on master
    ✓ High availability

Sharding Strategy:
  Shard by: short_url or unique_id
  Example:
    Shard 1 (A-M): Server 1
    Shard 2 (N-Z): Server 2
    
  Benefit:
    ✓ Distribute data across multiple servers
    ✓ Each server handles less data
    ✓ Better performance at scale

Backup & Recovery:
  ✓ Regular backups (daily)
  ✓ Write-ahead logs (WAL)
  ✓ Point-in-time recovery
```

### Unique ID Generator

```
Why Needed:
  - Centralized ID generation for uniqueness
  - Avoid duplicates across distributed system
  - No collision resolution needed

Options:
  1. UUID (universally unique identifier)
     ✓ Simple
     ✗ 128-bit long (need shorter for base62)
     
  2. Snowflake ID (Twitter's approach)
     ✓ 64-bit unique ID
     ✓ Sortable by timestamp
     ✓ Works in distributed systems
     
  3. Auto-increment Database
     ✓ Simple
     ✗ Single point of failure
     ✗ Hard to scale

Recommended: Snowflake ID
  Structure: [Timestamp (41 bits) | Machine ID (10 bits) | Sequence (12 bits)]
  Unique per: millisecond + machine + sequence
  Can generate: 4K IDs per millisecond per machine
```

---

## Interview Q&A

### Design Questions

**Q1: Design URL shortener for 100 million URLs per day**

```
Solution Overview:
1. Algorithm: Base 62 conversion (no collisions, fast)
2. Unique ID Generator: Snowflake ID service
3. Storage: SQL database with replication
4. Cache: Redis for frequently accessed URLs
5. Load Balancing: Distribute traffic across web servers

Key Components:
  - API Gateway (rate limiting, authentication)
  - Web Tier (stateless, horizontally scalable)
  - Service Layer (ID generation, base62 encoding)
  - Cache Layer (Redis, 99% hit rate)
  - Database Tier (master-slave replication)
  - Analytics (track click sources, user behavior)

Handling Peak Traffic:
  - Auto-scale web servers from 10 to 20+
  - Use read replicas to spread query load
  - Cache frequently accessed URLs
  - Circuit breaker if DB goes down

Single Points of Failure:
  - ID Generator: Replicate with master-slave
  - Database: Use replication + backup
  - Cache: Have DB fallback
```

**Q2: How to handle 11,600 read requests per second?**

```
Multi-layer Caching:
  Layer 1: Browser cache (301 redirect)
  Layer 2: CDN cache (geo-distributed)
  Layer 3: Redis cache (in-memory)
  Layer 4: Database (last resort)

Load Distribution:
  - 10 web servers (normal)
  - 20+ servers during peak
  - Each handles ~1,160 req/sec
  - Database replicas (3-5 copies)

Optimization:
  - Cache 99% of reads
  - Use 301 redirect for popular URLs
  - Browser caches response
  - Subsequent requests bypass service

Result: System handles spikes without degradation
```

**Q3: How to generate unique IDs in distributed system?**

```
Snowflake ID (Recommended):
  - 64-bit ID format
  - 41-bit timestamp (covers 69 years)
  - 10-bit machine/worker ID (1024 machines)
  - 12-bit sequence number (4096 per ms per machine)
  
  Capacity: 4,096 IDs per millisecond per machine
           × 1,024 machines = 4.2 billion IDs per second!

Alternative Options:
  1. UUID: Simple but 128-bit (too long)
  2. Auto-increment: Single point of failure
  3. Ticket Server: Central server assigns IDs (bottleneck)
  
Choose: Snowflake ID
```

### Problem-Solving Questions

**Q4: Database cannot keep up with 1,160 writes per second**

```
Solution 1: Write Optimization
  - Batch inserts (insert 100 URLs at once)
  - Reduces transaction overhead
  - ~5-10X improvement

Solution 2: Database Sharding
  - Split data across 5 databases
  - Each handles 230 writes/sec (manageable)
  - Each database: 73 billion URLs (within capacity)

Solution 3: Write Buffer
  - Queue writes in Kafka
  - Background job processes slowly
  - Smooths out traffic spikes

Solution 4: Cache Writes
  - Cache newly created short URLs
  - Don't hit database every read
  - Reduces overall load

Recommended: Combination
  - Use database sharding
  - Add write buffer for spikes
  - Cache new URLs
```

**Q5: How to prevent abuse (same URL shortened million times)?**

```
Prevention Strategy 1: Deduplication
  Check if long URL already exists
  Return existing short URL
  Benefit: One entry in database, many users

Prevention Strategy 2: Rate Limiting
  Limit requests per IP address
  Limit requests per user
  Example: Max 100 new short URLs per hour

Prevention Strategy 3: CAPTCHA
  For suspicious patterns
  Validate human user

Implementation:
  1. Hash long URL as quick lookup
  2. If exists: return existing short URL
  3. If new: Check rate limit
  4. If within limit: Generate short URL
  5. If exceeded: Return 429 Too Many Requests
```

**Q6: How to handle hot URLs (viral links with millions of clicks)?**

```
Problem: One URL gets millions of views
  Database query becomes bottleneck
  Cache hit rate matters

Solution 1: Increase Cache TTL
  Popular URLs: TTL = 24 hours
  Normal URLs: TTL = 1 hour
  Very popular: TTL = ∞ (never expire)

Solution 2: Multiple Cache Layers
  Browser cache (301 redirect)
  CDN cache (geo-distributed)
  Local cache (web server)
  Distributed cache (Redis)

Solution 3: Database Optimization
  Index on short_url column
  Use read replicas
  Shard by popularity

Result: Viral link handled with minimal load
```

### Trade-off Questions

**Q7: 301 vs 302 redirect - which to choose?**

```
301 (Permanent Redirect):
  Pros:
    ✓ Browser caches response
    ✓ Reduces server load
    ✓ Better performance
  
  Cons:
    ✗ Can't track every click
    ✗ Analytics less accurate
    ✗ Can't change destination URL
  
  Use when: Server load is critical

302 (Temporary Redirect):
  Pros:
    ✓ Track every click (analytics)
    ✓ Can change destination
    ✓ More flexible
  
  Cons:
    ✗ Every click hits server
    ✗ Higher server load
    ✗ Slower redirects
  
  Use when: Analytics is important

Recommendation:
  - 302 for tracking (most cases)
  - 301 only for performance critical
  - Consider hybrid: 302 with aggressive caching
```

**Q8: SQL vs NoSQL database - which is better?**

```
SQL Database (Recommended):
  Pros:
    ✓ ACID guarantees (consistency)
    ✓ Structured data
    ✓ Mature (battle-tested)
    ✓ Replication/backup easy
  
  Cons:
    ✗ Vertical scaling limited
    ✗ Sharding is complex
  
  Use when: Need transactions, consistency

NoSQL Database:
  Pros:
    ✓ Horizontal scaling easy
    ✓ High availability
    ✓ Flexible schema
  
  Cons:
    ✗ Eventual consistency (delay)
    ✗ No transactions
    ✗ Harder to query
  
  Use when: Need massive scale, OK with eventual consistency

Recommendation: SQL Database
  Why: URL shortener needs consistency
       Data: short_url → long_url (simple mapping)
       No complex transactions needed
       Replication + sharding solves scalability
```

---

## Additional Considerations

### Rate Limiting

```
Problem: Malicious users send millions of shortening requests

Solution:
  - Limit per IP: 100 URLs/hour
  - Limit per user: 1000 URLs/hour (if authenticated)
  - Use token bucket algorithm (fast, simple)
  - Return 429 Too Many Requests if exceeded

Filter:
  - API Gateway applies rate limit
  - Blocks abusive traffic before reaching servers
  - Protects infrastructure
```

### Analytics

```
Track:
  - How many clicks per short URL
  - When clicks happen (hourly, daily trends)
  - Geographic location of clicks
  - Referrer (where click came from)
  - Device type (mobile, desktop)
  - User agent (browser type)

Storage:
  - Log to distributed analytics system
  - Batch process daily
  - Store in data warehouse
  - Query with analytics tools (Tableau, etc.)

Use Cases:
  - Track campaign performance
  - Detect fraud/abuse
  - Understand user behavior
  - Business intelligence
```

### Availability & Reliability

```
Goal: 99.9% uptime (3 nines)

Strategies:
1. Replication: Master-slave database setup
2. Backup: Daily backups + point-in-time recovery
3. Failover: Automatic failover to replica
4. Monitoring: Alert on failures (email, SMS)
5. Circuit Breaker: Graceful degradation if DB down
6. Data Center Redundancy: Multiple regions

Failure Scenarios:
  Web server down → Load balancer routes to healthy servers
  Cache down → Fall back to database queries
  Database down → Read from replica
  All replicas down → Return cached data from browser
```

---

## Summary

### Recommended Architecture

```
Client Request
    ↓
[API Gateway - Rate Limiting]
    ↓
[Load Balancer]
    ↓
[Web Servers (10-20)] ← Stateless, scales horizontally
    ├─→ [Unique ID Generator] (Snowflake ID)
    ├─→ [Cache Layer] (Redis) ← 99% hit rate for redirects
    └─→ [Database Layer]
         ├─ Master (writes)
         └─ Slaves (reads x3-5) ← Sharded across 5 databases
    
Additional:
    [Analytics System] ← Track clicks, sources
    [Monitoring/Alerting] ← Health checks, failures
```

### Key Decisions Made

| Decision | Choice | Why |
|----------|--------|-----|
| Hash Function | Base 62 Conversion | No collisions, O(1), fast |
| ID Generator | Snowflake ID | Distributed, unique, sortable |
| Database | SQL (Master-Slave) | ACID, reliable, maturity |
| Redirect Type | 302 (with caching) | Analytics + performance |
| Cache Layer | Redis | Memory-efficient, distributed |
| Scaling | Horizontal | Easy to add/remove servers |

---

## References

- System Design Interview by Alex Xu, Chapter 8
- Distributed Unique ID Generation (Snowflake)
- Base 62 Encoding Algorithm
- Database Replication & Sharding Patterns

---

## Navigation

**Table of Contents:** [Back to Top](#table-of-contents)

---

## Single Server Setup

### The Journey Begins

Start with everything on one server - web app, database, cache, everything.

**Architecture:**
```
User → DNS → IP Address (15.125.23.214)
           ↓
       Web Server (Web App + Database)
           ↓
       HTML/JSON Response
```

### Request Flow

```
1. User visits: api.mysite.com
   ↓
2. DNS service returns IP: 15.125.23.214
   ↓
3. HTTP request sent to web server
   ↓
4. Web server processes request
   ↓
5. Returns HTML or JSON response
```

### Traffic Sources

**Web Application:**
- Server-side languages (Java, Python)
- Client-side languages (HTML, JavaScript)

**Mobile Application:**
- HTTP protocol for communication
- JSON format for API responses

**Example API Response:**
```json
GET /users/12
Response: { "id": 12, "name": "Alice", "email": "alice@example.com" }
```

### Problem with Single Server

```
✗ No failover: Server goes down = website offline
✗ No scalability: Growth requires upgrading one server (limited)
✗ No redundancy: All data on one machine = disaster if it fails
```

**Next Step:** Separate web and database tiers

---

## Scaling Web Tier

### Vertical vs Horizontal Scaling

**Vertical Scaling (Scale Up):**
```
Add more CPU/RAM to existing server

Pros:
  ✓ Simple

Cons:
  ✗ Hard limit (can't add unlimited CPU/RAM)
  ✗ No failover (one server = single point of failure)
  ✗ High cost ($$$)
```

**Horizontal Scaling (Scale Out):**
```
Add more servers

Pros:
  ✓ No hard limit
  ✓ Failover capability
  ✓ Better cost-effectiveness at scale

Cons:
  ✓ More complexity (distributed system)
```

### Load Balancer

**Problem:**
```
Multiple web servers, but how do requests get distributed?
How do we handle server failures?
```

**Solution: Load Balancer**
```
User → Load Balancer (Public IP)
                ↓
        ┌───────┼───────┐
        ↓       ↓       ↓
    Server1  Server2  Server3
   (Private) (Private) (Private)
```

**How It Works:**
```
1. User connects to public IP of load balancer
2. Load balancer distributes traffic to one of the servers
3. If Server1 fails, load balancer routes to Server2 or Server3
4. Adding new servers automatically spreads load
```

### Benefits

```
✓ Failover: One server down ≠ website down
✓ Horizontal scaling: Add servers as needed
✓ High availability: Users can always access service
```

---

## Scaling Database Tier

### Problem with Single Database

```
✗ No failover: Database down = service down
✗ No read scalability: Single DB handle all read requests
✗ Data loss risk: Single point of failure
```

### Database Replication

**Master-Slave Architecture:**
```
Master Database (Write Operations)
        ↑
        │ Replication
        ↓
Slave1  Slave2  Slave3 (Read Operations)
```

**How It Works:**
```
Write requests (INSERT, UPDATE, DELETE)
        ↓
  Master Database
        ↓
Replicated to Slaves

Read requests (SELECT)
        ↓
Distributed across Slaves
```

### Real-World Example

**Twitter writes:** 3,500 tweets per second
**Twitter reads:** 35,000 requests per second (10:1 ratio)

```
Solution:
  Master: Handles 3,500 writes/sec
  Slaves: Each handles 11,667 reads/sec
  (Multiple slaves share the read load)
```

### Advantages of Replication

```
✓ Better Performance: Reads distributed, writes handled by master
✓ Reliability: Data replicated = survives disasters
✓ High Availability: Service works if one slave fails
```

### Failover Strategy

**If Slave Fails:**
```
Read operations → Master (temporarily)
     ↓
New healthy slave replaces old one
```

**If Master Fails:**
```
One slave promoted to new Master
All operations → New Master
New slave added to replace old master
```

---

## Adding Cache Layer

### Problem

```
Every request hits database
↓
Repetitive database calls for same data
↓
Slow response times, high database load
```

### Solution: Cache Tier

```
Request arrives
        ↓
Check Cache (Redis/Memcached)
        ├─ Cache Hit: Return data immediately (1µs)
        ├─ Cache Miss: Query Database (100µs)
        └─ Store in Cache for next time
```

### Caching Strategy: Read-Through Cache

```
Step 1: Web server checks cache
        ├─ Cache has data? Return it
        └─ Cache miss? Continue to Step 2

Step 2: Query database
        ├─ Get data
        └─ Store in cache

Step 3: Return to client
```

### Real Numbers

**With cache:**
```
Scenario: Popular blog post gets 10,000 reads per hour
  First read: 100µs (from database)
  Remaining 9,999 reads: 1µs each (from cache)
  Total time: 100µs + (9,999 × 1µs) ≈ 10ms
  
Without cache:
  Total time: 10,000 × 100µs = 1000ms = 1 second
  
Speedup: 100X faster!
```

### Cache Considerations

**When to Use Cache:**
```
✓ Data read frequently
✓ Data modified infrequently
✗ Don't use for critical data (RAM = volatile)
```

**Expiration Policy:**
```
Set TTL (Time-to-Live) for cached data

Too short:  System reloads from DB too often (overhead)
Too long:   Data becomes stale (incorrect)
Sweet spot: Balance freshness and performance
```

**Single Point of Failure:**
```
✗ One cache server down = requests to database spike
✓ Solution: Multiple cache servers across regions
```

**Eviction Policy:**
```
Cache is full, new data arrives, which gets removed?

LRU (Least Recently Used): Remove least recently accessed
LFU (Least Frequently Used): Remove least frequently accessed
FIFO (First In First Out): Remove oldest data
```

---

## Content Delivery Network (CDN)

### Problem

```
Static content (images, CSS, JS) served from origin server
↓
Users far from server = slow load
↓
Bandwidth wasted, users frustrated
```

### Solution: CDN (Content Delivery Network)

```
CDN = Network of servers geographically dispersed

User in Los Angeles    User in Europe     User in Tokyo
        ↓                   ↓                  ↓
   CDN in LA          CDN in London      CDN in Tokyo
        └─────────────────┼────────────────┘
                        Origin
                       (S3, etc.)
```

### How CDN Works

```
1. User requests: image.png
   URL: https://mysite.cloudfront.net/logo.jpg

2. CDN server closest to user?
   ├─ Has image in cache? → Return immediately
   └─ Not in cache? → Fetch from origin

3. Origin sends image + TTL (how long to cache)

4. CDN caches image, returns to user

5. Next user in same region gets from cache
```

### Real-World Impact

```
Without CDN:
  User in London requests image from US server
  Distance: ~5000 miles
  Latency: ~150ms per request

With CDN:
  User in London requests from CDN in London
  Distance: ~10 miles
  Latency: ~10ms per request
  
Speedup: 15X faster!
```

### CDN Considerations

**Cost:**
```
Pay for data transferred out of CDN
Infrequently used assets should not be on CDN (waste)
```

**Expiration:**
```
Too short: Reload from origin frequently (high cost)
Too long: Stale content (incorrect)
```

**Fallback:**
```
If CDN fails, request to origin server
Prevents complete blackout
```

---

## Stateless vs Stateful Architecture

### Problem: Server Affinity

```
Stateful (Bad):
  User A connects to Server 1
  User A's session data on Server 1
  User A MUST always go to Server 1
  ↓
  Load balancer must use "sticky sessions"
  ↓
  Hard to scale, fail-over difficult

Example:
  Server 1: User A
  Server 2: User B
  Server 3: User C
  ↓
  If Server 1 down, User A's data lost!
```

### Solution: Stateless Architecture

```
Move session data OUT of web servers
Store in shared persistent storage (database, Redis, etc.)

User A connects to Server 1
        ↓
Server 1 fetches session from shared storage
        ↓
If Server 1 fails, User A can go to Server 2
        ↓
Server 2 fetches same session from shared storage
        ↓
No disruption to user!
```

### Architecture Comparison

**Stateful (Problematic):**
```
Load Balancer
        ↓
    ┌───┼───┬───┐
    ↓   ↓   ↓   ↓
Server1 Server2 Server3 Server4
│ User │ │ User │ │ User │ │ User │
│  A   │ │  B   │ │  C   │ │  D   │
└───────┘ └───────┘ └───────┘ └───────┘

Problem: Each user tied to specific server
```

**Stateless (Solution):**
```
Load Balancer
        ↓
    ┌───┼───┬───┐
    ↓   ↓   ↓   ↓
Server1 Server2 Server3 Server4

---

## Navigation

**Table of Contents:** [Back to Top](#table-of-contents)

---

# CHUBB Interview Question: Design a Unique Code Generator

---

## Single Server Setup

### The Journey Begins

Start with everything on one server - web app, database, cache, everything.

**Architecture:**
```
User → DNS → IP Address (15.125.23.214)
           ↓
       Web Server (Web App + Database)
           ↓
       HTML/JSON Response
```

### Request Flow

```
1. User visits: api.mysite.com
   ↓
2. DNS service returns IP: 15.125.23.214
   ↓
3. HTTP request sent to web server
   ↓
4. Web server processes request
   ↓
5. Returns HTML or JSON response
```

### Traffic Sources

**Web Application:**
- Server-side languages (Java, Python)
- Client-side languages (HTML, JavaScript)

**Mobile Application:**
- HTTP protocol for communication
- JSON format for API responses

**Example API Response:**
```json
GET /users/12
Response: { "id": 12, "name": "Alice", "email": "alice@example.com" }
```

### Problem with Single Server

```
✗ No failover: Server goes down = website offline
✗ No scalability: Growth requires upgrading one server (limited)
✗ No redundancy: All data on one machine = disaster if it fails
```

**Next Step:** Separate web and database tiers

---

## Scaling Web Tier

### Vertical vs Horizontal Scaling

**Vertical Scaling (Scale Up):**
```
Add more CPU/RAM to existing server

Pros:
  ✓ Simple

Cons:
  ✗ Hard limit (can't add unlimited CPU/RAM)
  ✗ No failover (one server = single point of failure)
  ✗ High cost ($$$)
```

**Horizontal Scaling (Scale Out):**
```
Add more servers

Pros:
  ✓ No hard limit
  ✓ Failover capability
  ✓ Better cost-effectiveness at scale

Cons:
  ✓ More complexity (distributed system)
```

### Load Balancer

**Problem:**
```
Multiple web servers, but how do requests get distributed?
How do we handle server failures?
```

**Solution: Load Balancer**
```
User → Load Balancer (Public IP)
                ↓
        ┌───────┼───────┐
        ↓       ↓       ↓
    Server1  Server2  Server3
   (Private) (Private) (Private)
```

**How It Works:**
```
1. User connects to public IP of load balancer
2. Load balancer distributes traffic to one of the servers
3. If Server1 fails, load balancer routes to Server2 or Server3
4. Adding new servers automatically spreads load
```

### Benefits

```
✓ Failover: One server down ≠ website down
✓ Horizontal scaling: Add servers as needed
✓ High availability: Users can always access service
```

---

## Scaling Database Tier

### Problem with Single Database

```
✗ No failover: Database down = service down
✗ No read scalability: Single DB handle all read requests
✗ Data loss risk: Single point of failure
```

### Database Replication

**Master-Slave Architecture:**
```
Master Database (Write Operations)
        ↑
        │ Replication
        ↓
Slave1  Slave2  Slave3 (Read Operations)
```

**How It Works:**
```
Write requests (INSERT, UPDATE, DELETE)
        ↓
  Master Database
        ↓
Replicated to Slaves

Read requests (SELECT)
        ↓
Distributed across Slaves
```

### Real-World Example

**Twitter writes:** 3,500 tweets per second
**Twitter reads:** 35,000 requests per second (10:1 ratio)

```
Solution:
  Master: Handles 3,500 writes/sec
  Slaves: Each handles 11,667 reads/sec
  (Multiple slaves share the read load)
```

### Advantages of Replication

```
✓ Better Performance: Reads distributed, writes handled by master
✓ Reliability: Data replicated = survives disasters
✓ High Availability: Service works if one slave fails
```

### Failover Strategy

**If Slave Fails:**
```
Read operations → Master (temporarily)
     ↓
New healthy slave replaces old one
```

**If Master Fails:**
```
One slave promoted to new Master
All operations → New Master
New slave added to replace old master
```

---

## Adding Cache Layer

### Problem

```
Every request hits database
↓
Repetitive database calls for same data
↓
Slow response times, high database load
```

### Solution: Cache Tier

```
Request arrives
        ↓
Check Cache (Redis/Memcached)
        ├─ Cache Hit: Return data immediately (1µs)
        ├─ Cache Miss: Query Database (100µs)
        └─ Store in Cache for next time
```

### Caching Strategy: Read-Through Cache

```
Step 1: Web server checks cache
        ├─ Cache has data? Return it
        └─ Cache miss? Continue to Step 2

Step 2: Query database
        ├─ Get data
        └─ Store in cache

Step 3: Return to client
```

### Real Numbers

**With cache:**
```
Scenario: Popular blog post gets 10,000 reads per hour
  First read: 100µs (from database)
  Remaining 9,999 reads: 1µs each (from cache)
  Total time: 100µs + (9,999 × 1µs) ≈ 10ms
  
Without cache:
  Total time: 10,000 × 100µs = 1000ms = 1 second
  
Speedup: 100X faster!
```

### Cache Considerations

**When to Use Cache:**
```
✓ Data read frequently
✓ Data modified infrequently
✗ Don't use for critical data (RAM = volatile)
```

**Expiration Policy:**
```
Set TTL (Time-to-Live) for cached data

Too short:  System reloads from DB too often (overhead)
Too long:   Data becomes stale (incorrect)
Sweet spot: Balance freshness and performance
```

**Single Point of Failure:**
```
✗ One cache server down = requests to database spike
✓ Solution: Multiple cache servers across regions
```

**Eviction Policy:**
```
Cache is full, new data arrives, which gets removed?

LRU (Least Recently Used): Remove least recently accessed
LFU (Least Frequently Used): Remove least frequently accessed
FIFO (First In First Out): Remove oldest data
```

---

## Content Delivery Network (CDN)

### Problem

```
Static content (images, CSS, JS) served from origin server
↓
Users far from server = slow load
↓
Bandwidth wasted, users frustrated
```

### Solution: CDN (Content Delivery Network)

```
CDN = Network of servers geographically dispersed

User in Los Angeles    User in Europe     User in Tokyo
        ↓                   ↓                  ↓
   CDN in LA          CDN in London      CDN in Tokyo
        └─────────────────┼────────────────┘
                        Origin
                       (S3, etc.)
```

### How CDN Works

```
1. User requests: image.png
   URL: https://mysite.cloudfront.net/logo.jpg

2. CDN server closest to user?
   ├─ Has image in cache? → Return immediately
   └─ Not in cache? → Fetch from origin

3. Origin sends image + TTL (how long to cache)

4. CDN caches image, returns to user

5. Next user in same region gets from cache
```

### Real-World Impact

```
Without CDN:
  User in London requests image from US server
  Distance: ~5000 miles
  Latency: ~150ms per request

With CDN:
  User in London requests from CDN in London
  Distance: ~10 miles
  Latency: ~10ms per request
  
Speedup: 15X faster!
```

### CDN Considerations

**Cost:**
```
Pay for data transferred out of CDN
Infrequently used assets should not be on CDN (waste)
```

**Expiration:**
```
Too short: Reload from origin frequently (high cost)
Too long: Stale content (incorrect)
```

**Fallback:**
```
If CDN fails, request to origin server
Prevents complete blackout
```

---

## Stateless vs Stateful Architecture

### Problem: Server Affinity

```
Stateful (Bad):
  User A connects to Server 1
  User A's session data on Server 1
  User A MUST always go to Server 1
  ↓
  Load balancer must use "sticky sessions"
  ↓
  Hard to scale, fail-over difficult

Example:
  Server 1: User A
  Server 2: User B
  Server 3: User C
  ↓
  If Server 1 down, User A's data lost!
```

### Solution: Stateless Architecture

```
Move session data OUT of web servers
Store in shared persistent storage (database, Redis, etc.)

User A connects to Server 1
        ↓
Server 1 fetches session from shared storage
        ↓
If Server 1 fails, User A can go to Server 2
        ↓
Server 2 fetches same session from shared storage
        ↓
No disruption to user!
```

### Architecture Comparison

**Stateful (Problematic):**
```
Load Balancer
        ↓
    ┌───┼───┬───┐
    ↓   ↓   ↓   ↓
Server1 Server2 Server3 Server4
│ User │ │ User │ │ User │ │ User │
│  A   │ │  B   │ │  C   │ │  D   │
└───────┘ └───────┘ └───────┘ └───────┘

Problem: Each user tied to specific server
```

**Stateless (Solution):**
```
Load Balancer
        ↓
    ┌───┼───┬───┐

---

## Navigation

**Table of Contents:** [Back to Top](#table-of-contents)
