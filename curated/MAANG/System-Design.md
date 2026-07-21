# System Design: Complete Interview Preparation Guide

A comprehensive guide covering foundational system design concepts and detailed case studies. This guide includes framework for approaching interviews, estimation techniques, scaling strategies, and real-world problem solving.

---

## Table of Contents

### Chapter 1: Scale from Zero to Millions of Users
1. [Executive Summary](#executive-summary)
2. [Single Server Setup](#single-server-setup)
3. [Scaling Web Tier](#scaling-web-tier)
4. [Scaling Database Tier](#scaling-database-tier)
5. [Adding Cache Layer](#adding-cache-layer)
6. [Content Delivery Network (CDN)](#content-delivery-network-cdn)
7. [Stateless vs Stateful Architecture](#stateless-vs-stateful-architecture)
8. [Multiple Data Centers](#multiple-data-centers)
9. [Message Queue](#message-queue)
10. [Complete Architecture](#complete-architecture-scalable-web-application-social-media-example)
11. [When to Use This Architecture](#when-to-use-this-architecture)
12. [Key Takeaways](#key-takeaways)

### Chapter 2: Back-of-the-Envelope Estimation
1. [Quick Cheat Sheet (Printer-Friendly)](#quick-cheat-sheet-memorize-these-printer-friendly-3-column)
2. [Two Formulas Side-by-Side (Print This!)](#two-formulas---side-by-side-print-this) ⭐⭐
2a. [QPS ↔ Data Generation Bridge (Print This!)](#qps--data-generation-bridge-print-this) ⭐⭐ FLOW
2b. [Quick Revision Sheet (External Link)](#quick-revision-sheet-all-formulas--twitter-example) ⭐⭐ COMPACT
3. [Executive Summary](#executive-summary)
4. [Power of Two](#power-of-two)
5. [Latency Numbers Every Programmer Should Know](#latency-numbers-every-programmer-should-know)
6. [Availability Numbers](#availability-numbers)
7. [Estimation Techniques](#estimation-techniques)
8. [Real-World Example: Twitter](#real-world-example-twitter)
9. [Peak-Adjusted QPS Formula](#peak-adjusted-qps-formula) ⭐
10. [Universal Storage Calculation Formula](#universal-storage-calculation-formula) ⭐
11. [Data Generation from QPS Formula](#data-generation-from-qps-formula-deriving-storage-from-requests) ⭐⭐ BRIDGE
12. [Bandwidth Calculation Formula](#bandwidth-calculation-formula) ⭐
13. [Database Capacity Formula](#database-capacity-formula) ⭐
13a. [Database Formula Explained: QPS → Database Size](#database-formula-explained-qps--database-size) ⭐⭐ DETAILED FLOW
14. [Caching Layer Formula](#caching-layer-formula) ⭐
15. [Tips for Estimation](#tips-for-estimation)

### Chapter 3: A Framework for System Design Interviews
*(Note: Chapter 3.5 below is the primary framework)*

### Chapter 3.5: The Universal System Design Framework ⭐ START HERE
1. [Quick Reference Table (7 Steps)](#quick-reference-table-print--use-in-interviews)
2. [Technical Approaches: When to Use Each](#technical-approaches-when-to-use-each-purpose-based-guide)
3. [STEP 1: Clarify Functional Requirements](#step-1-clarify-functional-requirements-5-min)
4. [STEP 2: Clarify Non-Functional Requirements & Scale](#step-2-clarify-non-functional-requirements--scale-5-min)
5. [STEP 3: Generic Blueprint](#step-3-generic-blueprint-high-level-design---with-technical-approaches)
6. [STEP 4: Customize Blueprint](#step-4-customize-blueprint-for-your-system)
7. [STEP 5: Back-of-Envelope Estimation](#step-5-back-of-envelope-estimation-5-min)
8. [STEP 6: Design Deep-Dives](#step-6-design-deep-dives-nfrs)
9. [STEP 7: Verify Growth & Constraints](#step-7-verify-growth--constraints)
10. [Complete Worked Example: Design Uber](#complete-worked-example-design-uber)

### Chapter 8: Design a URL Shortener
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

### CHUBB Interview Question: Design a Unique Code Generator
1. [The Problem](#the-problem)
2. [Approach 1: RDBMS (Why it Fails)](#approach-1-rdbms--fails)
3. [Approach 2: Pre-Generation + Queue (Works)](#approach-2-pre-generation--queue--works)
4. [How It Really Works](#how-it-really-works)
5. [Performance Comparison](#performance-comparison)

---


# Chapter 1: Scale from Zero to Millions of Users


## ⚠️ Important Notice

**This document is based on concepts from "System Design Interview" by Alex Xu.**

This is **original content**: summaries, explanations, and real-world examples created to help understand system design concepts. It does not reproduce copyrighted material from the book. All content is restructured and rewritten for clarity and educational purposes.

---


---

## Executive Summary

Scaling a system from supporting one user to millions requires a journey of continuous refinement. This chapter builds a system step-by-step, showing you the common techniques and patterns used in production systems.

**Key Principle:** Scale horizontally (add more servers) rather than vertically (upgrade one server).

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

## Complete Architecture: Scalable Web Application (Social Media Example)

### System Type

This is a **general-purpose web application architecture** designed to scale from 1,000 to 1 million+ daily active users. The example focuses on a social media platform with:

- **User authentication & session management** — track who's logged in
- **Post creation/publishing** — write-heavy operations with validation
- **Feed loading & caching** — read-heavy operations with hot data
- **Asynchronous media processing** — upload images without blocking user
- **Geographic distribution** — serve users worldwide with low latency

### Real-World Examples

This pattern is used by:
- ✅ **Facebook, Twitter, Instagram** — user feeds + post creation
- ✅ **Reddit, Hacker News** — community feeds + ranking algorithms
- ✅ **Medium, Substack** — content creation + recommendation
- ✅ **YouTube, TikTok** — media feeds + recommendation engines
- ✅ **Slack, Discord** — messaging + notification systems
- ✅ **Uber, Lyft** — ride-sharing feeds + driver/passenger matching

### Key Characteristics

| Aspect | Pattern |
|--------|---------|
| **Web Servers** | Stateless (can scale horizontally) |
| **Cache Layer** | Handles read amplification (1000x speedup) |
| **Database** | Master-slave replication (read replicas) |
| **Async Processing** | Decouples slow operations (image, email) |
| **Geographic** | CDN for static content + multi-region setup |
| **Bottlenecks Addressed** | CPU, memory, disk I/O, network bandwidth |

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

## When to Use This Architecture

**✓ Good For:**
- Social media platforms (feeds, posts, comments)
- Content sharing platforms (images, videos, documents)
- Community-driven applications (forums, Q&A)
- User-generated content systems
- Real-time messaging applications
- Rapid growth from 1K to 1M+ users

**✗ NOT Good For:**
- Real-time analytics (use stream processing instead)
- Payment systems (use specialized payment platforms)
- High-frequency trading (need sub-millisecond latency)
- IoT sensor data (use time-series databases)
- Machine learning training (use data warehouses)

---

## Key Takeaways

### Scaling Principles
```
✓ Start simple (single server), scale incrementally
✓ Add components based on bottlenecks (measure first!)
✓ Separate concerns (web, cache, database, queue)
✓ Scale horizontally (more servers) not vertically (bigger servers)
✓ Replicate data for reliability (master-slave)
✓ Distribute geographically for latency (CDN + multi-region)
```

### Architecture Decisions
```
✓ Keep web servers STATELESS → can add/remove servers anytime
✓ Cache EVERYTHING that's read frequently → 1000x speedup
✓ Use read replicas → distribute query load
✓ Decouple with message queues → prevent cascading failures
✓ Async processing → unblock user-facing requests
✓ Monitor and measure → know where bottlenecks are
```

### Common Mistakes to Avoid
```
✗ Premature optimization — don't add complexity you don't need
✗ Storing state in servers — kills horizontal scaling
✗ Single point of failure — replicate everything
✗ No monitoring — you can't improve what you don't measure
✗ Ignoring CAP theorem — understand consistency vs availability tradeoffs
```

---

## References

- System Design Interview by Alex Xu, Chapter 1
- Database Replication Patterns
- Content Delivery Network (CDN) Concepts
- Asynchronous Messaging Patterns

---

# Chapter 2: Back-of-the-Envelope Estimation


## ⚠️ Important Notice

**This document is based on concepts from "System Design Interview" by Alex Xu.**

This is **original content**: summaries, explanations, and real-world examples created to help understand system design concepts. It does not reproduce copyrighted material from the book. All content is restructured and rewritten for clarity and educational purposes.

---


---

## 🎯 QUICK CHEAT SHEET (Memorize These!) - Printer-Friendly 3-Column

**PRINT LANDSCAPE MODE - FITS ON 1 PAGE**

| **POWERS & TIME** | **LATENCY & THROUGHPUT** | **SCALE & FORMULAS** |
|---|---|---|
| **Powers of 2/10:** | **Latency (ms):** | **Scale Numbers:** |
| 2^10 = 1 KB | L1: 0.0005 | 1M users = Startup |
| 2^20 = 1 MB | Mem: 0.1 | 100M = Instagram |
| 2^30 = 1 GB | SSD: 0.25 | 1B = Facebook |
| 2^40 = 1 TB | Disk: 10 | 1M records ≈ 1 GB |
| 10^6 = 1M | DC: 0.5 | 1B records ≈ 1 TB |
| 10^9 = 1B | **Redis: 2** | QPS 100-1K (small) |
| 10^12 = 1T | **DB: 100** | QPS 10K-100K (high) |
| | US: 150 | |
| **Time (Memorize!):** | **Throughput:** | **FORMULAS (Write First):** |
| Sec/Day: **100K** | Network: 125 MB/s | **QPS** = (DAU × req/day) ÷ 100K |
| Sec/Year: **32M** | SSD: 100+ MB/s | **Peak** = avg × 2.5 |
| | HDD: 1-10 MB/s | **Servers** = Peak ÷ capacity |
| **Availability (SLA→Down):** | Memory: 10+ GB/s | **Storage** = daily × 365 × yrs × 3 |
| 99% = 3.65 days | QPS/Server: 1K-10K | **Cache Hit** = 80%+ |
| 99.9% = 8.76 hrs | | |
| 99.99% = 53 min | | |
| 99.999% = 5 min | | |

---

## 🎯 TWO FORMULAS - SIDE BY SIDE (Print This!) 

**PRINT IN LANDSCAPE MODE - FITS ON 1 PAGE**

### Left Column: QPS Formula | Right Column: Storage Formula

```
═══════════════════════════════════════════════════════════════════════════════
                      PEAK-ADJUSTED QPS FORMULA
═══════════════════════════════════════════════════════════════════════════════

STEP 1: Off-peak QPS              │  STEP 1: Daily Data Generation
  = (DAU × R) ÷ 100K            │    = DAU × Data_per_user_per_day

STEP 2: Peak QPS                  │  STEP 2: Apply Retention
  = Off-peak × P                  │    = Daily_data × Retention_days

STEP 3: Total Daily Requests      │  STEP 3: Apply Redundancy
  = (Off × 3,600 × Avg_hrs) +     │    = Total_data × Redundancy_factor
    (Peak × 3,600 × Peak_hrs)     │

STEP 4: Servers Needed            │  STEP 4: Apply Compression
  = Peak ÷ Server_capacity        │    = With_redundancy ÷ Compression
  × Redundancy (2-3X)             │

───────────────────────────────────────────────────────────────────────────────
EXAMPLE: Twitter (300M DAU)        │  EXAMPLE: Twitter (300M DAU)
───────────────────────────────────────────────────────────────────────────────

QPS Calculation:                   │  Storage Calculation:
  DAU = 300M                       │    DAU = 300M
  Requests/user = 20/day           │    Data/user = 10 MB/day
  Peak multiplier = 4X             │    Retention = 5 years (1,825 days)
  Peak hours = 4                   │    Redundancy = 2X
  Avg hours = 20                   │    Compression = 1.5X

Off-peak: (300M × 20) ÷ 86K       │  Daily: 300M × 10 MB = 3 PB
        = 60,000 QPS              │  Retention: 3 PB × 1,825 = 5,475 PB
                                  │  Redundancy: 5,475 PB × 2 = 10,950 PB
Peak: 60,000 × 4 = 240,000 QPS   │  Final: 10,950 PB ÷ 1.5 = 7,300 PB
Servers (500 QPS/server):          │
  Off-peak: 60K ÷ 500 = 120       │  Tiered Storage (cost-optimized):
  Peak: 240K ÷ 500 = 480          │    Hot (1 yr, SSD): 1,095 PB @ $0.023/GB/mo
  Auto-scale: +360 servers         │    Warm (4 yrs, HDD): 4,380 PB @ $0.003/GB/mo
                                  │    Total: 7,300 PB ≈ $462M/year

───────────────────────────────────────────────────────────────────────────────
QUICK REFERENCE TABLE
───────────────────────────────────────────────────────────────────────────────

System     │ QPS (Peak) │ Servers   │ Storage      │ Retention
──────────┼────────────┼───────────┼──────────────┼──────────────
Twitter    │ 250-500K   │ 500-1000  │ 50-100 PB    │ 5 years
Instagram  │ 500K-1M    │ 1K-2K     │ 1-2 EB       │ 10 years
Uber       │ 50-100K    │ 100-200   │ 50 PB        │ 3 months
Netflix    │ 200K-500K  │ 400-1K    │ 100-500 PB   │ 2 years
Stripe     │ 10-100K    │ 20-200    │ 100 TB-10PB  │ 10 years

───────────────────────────────────────────────────────────────────────────────
KEY INSIGHTS
───────────────────────────────────────────────────────────────────────────────

QPS Formula:                       │  Storage Formula:
• Peak ≠ Average (it's a multiple) │  • Retention >> Daily data
• Peak hours are 2-5 hours/day     │  • Tiered storage (hot/warm/cold)
• Higher peak multiplier for       │  • Compression only if not already
  interactive apps (4-5X)          │    compressed (video/images are!)
• Redundancy: 2-3X for servers     │  • Redundancy: 2X typical, 3X critical
  (master-slave or multi-region)   │  • Archive very old data (cold storage)

───────────────────────────────────────────────────────────────────────────────
MEMORY CHECKLIST - Memorize these constants!
───────────────────────────────────────────────────────────────────────────────

QPS Calculation:                   │  Storage Calculation:
  100K = seconds per day         │  1 KB = 1,024 bytes
  3,600 = seconds per hour         │  1 MB = 1,024 KB
  Peak factor = 2-5X typical       │  1 GB = 1,024 MB
  Server capacity = 1K-10K QPS     │  1 TB = 1,024 GB
  Redundancy = 2-3X                │  1 PB = 1,024 TB
                                  │  Redundancy = 2-3X
  Peak hours = 2-4 hours (typ 4)   │
                                  │  Storage Cost (per GB/month):
                                  │  SSD (Hot): $0.023/GB/mo ($276K/TB/yr)
                                  │  HDD (Warm): $0.003/GB/mo ($36K/TB/yr)
                                  │  Archive: $0.004/GB/year ($4.8K/TB/yr)

═══════════════════════════════════════════════════════════════════════════════
```

---

## 🎯 QPS ↔ ALL FORMULAS DEPENDENCY FLOW (Print 1 Page!)

**PRINT IN LANDSCAPE MODE - FITS ON 1 PAGE**

```
═══════════════════════════════════════════════════════════════════════════════════════════════════════════════
                              QPS: THE MASTER INPUT FOR ALL FORMULAS
═══════════════════════════════════════════════════════════════════════════════════════════════════════════════

INPUT: Requests/user = 20/day (DAU = 300M)  →  QPS FORMULA  →  Peak QPS = 240,000

      SERVERS (480)           BANDWIDTH (38.4 Gbps)         CACHING (600 PB)      STORAGE (7,300 PB)     DATABASE (1.5 PB)
      Peak/500 QPS            QPS × 2KB × 8 ÷ 10⁹ × 10X   80% hit from cache    10 MB × retention      Records × size
      + 2X redundancy         = 480 MB/s × 10X            Working set 300PB      + 2X + compress        + 1.5X indexes
      = 960 peak              = 38.4 Gbps needed          + 2X = 600 PB          = 7,300 PB total       + 2X replica
                                                                                                          ≈ 1.5 PB

───────────────────────────────────────────────────────────────────────────────────────────────────────────────────

CALCULATION FLOW (Twitter Example):

QPS FORMULA                               DATA DERIVATION (from Write QPS)           DEPENDENT FORMULAS
─────────────────────────────────────     ──────────────────────────────────────    ─────────────────────────
(300M × 20) ÷ 100K = 60K avg QPS         Write_QPS = 240K × (1÷11) = 21.8K writes  Servers: 240K ÷ 500 = 480
60K × 4 = 240K peak QPS                  Data = 21.8K × 130B = 2.83 MB/sec         BW: 240K × 2KB = 480MB/s
Daily: 7.8B requests                        Daily/user: (2.83M × 100K) ÷ 300M         Cache: 300PB × 2 = 600PB
                                          = 10 MB/user/day ← DATA METRIC             DB: 1.5PB (+ indexes)
                                                                                      Storage: 7,300PB (5yr)

───────────────────────────────────────────────────────────────────────────────────────────────────────────────────

QUICK REFERENCE TABLE: How Each Formula Uses QPS

Formula              │ Input                      │ Calculation                              │ Output
─────────────────────┼────────────────────────────┼──────────────────────────────────────────┼─────────────────────
SERVERS              │ Peak QPS (240K)            │ Peak ÷ 500 QPS/server × 2 redundancy    │ 960 servers peak
BANDWIDTH            │ Peak QPS × Response (2KB)  │ (240K × 2KB × 8) ÷ 10⁹ × 10X redundancy │ 38.4 Gbps
CACHING              │ Peak QPS × Hit% × Working% │ Working_set × 2 redundancy              │ 600 PB cache
DATA DERIVATION      │ Write_QPS × Bytes/write    │ (240K ÷ 11) × 130B × 100K ÷ 300M       │ 10 MB/user
STORAGE              │ Data/user × Retention      │ 10M × 1,825 × 2 ÷ 1.5 (compress)       │ 7,300 PB total
DATABASE             │ Records/user × Size        │ Records × 500B × 1.5 × 2                │ 1.5 PB main DB

───────────────────────────────────────────────────────────────────────────────────────────────────────────────────

COMPLETE INFRASTRUCTURE (One Input → Complete System):

QPS Path:        240K peak → 480 servers → 38.4 Gbps → 960 servers (2X) → Auto-scale +360 for 4 hours

Data Path:       240K → 21.8K writes → 10 MB/user → 7,300 PB storage → ~$462M/year
                                                   → 1.5 PB main DB
                                                   → 600 PB cache (80% hit rate saves 80% DB load)
                 Storage Cost Breakdown:
                   ├─ Hot (SSD, 1 yr): 1,095 PB × $0.023/GB/mo × 12 = $303M/year
                   └─ Warm (HDD, 4 yr): 4,380 PB × $0.003/GB/mo × 12 = $158M/year

───────────────────────────────────────────────────────────────────────────────────────────────────────────────────

DEPENDENCY CHAIN (What feeds into what):

Requests/user → QPS Formula → Peak QPS ─┬─→ Servers (÷ capacity)
                                        ├─→ Bandwidth (× response_size × 8 × 10X)
                                        ├─→ Caching (× hit% → working_set × 2)
                                        └─→ Write_QPS ─┬─→ Data/user → Storage & DB formulas
                                                       └─→ Records/user → Database formula

KEY: Peak QPS drives infrastructure; Write_QPS drives storage. Both come from "requests/user".

═══════════════════════════════════════════════════════════════════════════════════════════════════════════════
```

---

## 📋 QUICK REVISION SHEET: All Formulas + Twitter Example

**👉 [View SYSTEM_DESIGN_REVISION_SHEET.md](SYSTEM_DESIGN_REVISION_SHEET.md) — Print this for last-minute interview prep!**

Contains:
- INPUT ASSUMPTIONS for all formulas
- QPS, Storage, Bandwidth, Database, Caching formulas with Twitter calculations
- Key constants & cost breakdown
- Calculation flow & decision matrix
- Perfect for printing (fits on 1-2 pages)

---

## 🎬 COMPLETE TWITTER EXAMPLE - Both Formulas Applied

**System:** Twitter-like social media platform

### Given Assumptions

```
USERS & ACTIVITY:
  Daily Active Users (DAU) = 300 million
  
USER BEHAVIOR:
  Tweets read per user per day = 50 tweets
  Tweets posted per user per day = 1 tweet
  Average tweet length = 280 characters = 280 bytes
  
MEDIA (10% of tweets have images):
  Average image size = 2 MB
  Image posts per user = 0.1 (10% of 1 tweet)
  
TOTAL DATA PER USER PER DAY:
  Text tweets: 1 tweet × 280 bytes = 280 bytes
  Images: 0.1 images × 2 MB = 200 KB
  Timeline metadata: 50 views × 1 KB = 50 KB
  Likes/retweets: 5 interactions × 100 bytes = 500 bytes
  ─────────────────────────────────
  Total: ~250 KB per user per day (or 6 MB with overhead)
```

### INFRASTRUCTURE NEEDED

**USING QPS FORMULA:**

```
Step 1: Off-peak QPS (average throughout day)
  (DAU × Requests_per_user) ÷ 100K
  (300M × 20 requests) ÷ 100K
  = 6,000,000,000 ÷ 100K
  = 60,000 QPS (off-peak average)

Step 2: Peak QPS (evening hours)
  Off-peak × Peak_multiplier
  69,444 × 4 (evening traffic is 4X higher)
  = 240,000 QPS (peak)

Step 3: Total Daily Requests
  Off-peak hours (20 hrs): 60,000 × 3,600 × 20 = 4.32 billion
  Peak hours (4 hrs): 240,000 × 3,600 × 4 = 3.46 billion
  Total daily requests = 7.78 billion ≈ 7.8B

Step 4: Server Capacity
  Assuming 500 QPS per server (typical web server):
  
  Off-peak servers needed: 60,000 ÷ 500 = 120 servers
  Peak servers needed: 240,000 ÷ 500 = 480 servers
  
  WITH 2X REDUNDANCY (master-slave replication):
    Off-peak: 120 × 2 = 240 servers (always running)
    Peak: 480 × 2 = 960 servers (scale up 6-11 PM)
    Auto-scale: Add +720 servers during peak hours
```

**USING STORAGE FORMULA:**

```
Step 1: Daily Data Generation
  DAU × Data_per_user_per_day
  300M × 6 MB
  = 1.8 Exabytes (EB) per day

Step 2: Apply Retention (tweets kept for 5 years)
  Daily_data × Retention_days
  1.8 EB × 1,825 days (5 years)
  = 3,285 Petabytes (PB)

Step 3: Apply Redundancy (2X for master-slave)
  Total_data × Redundancy_factor
  3,285 PB × 2
  = 6,570 PB

Step 4: Apply Compression (gzip ~1.5X for text)
  With_redundancy ÷ Compression_ratio
  6,570 PB ÷ 1.5
  = 4,380 Petabytes (PB)

TIERED STORAGE STRATEGY (cost optimization):
  Hot storage (current 1 year, SSD, expensive):
    1.8 EB/day × 365 days = 657 PB
    Cost: ~$65 million/year
  
  Warm storage (years 2-5, HDD, cheap):
    1.8 EB/day × 365 × 4 = 2,628 PB
    Cost: ~$20 million/year
  
  Total: 3,285 PB stored, ~$85 million/year
```

### SUMMARY TABLE: TWITTER INFRASTRUCTURE

```
┌──────────────────────────────────────────────────────┐
│              TWITTER INFRASTRUCTURE NEEDS             │
├──────────────────────────────────────────────────────┤
│                                                      │
│ COMPUTATION (QPS Formula):                           │
│   Off-peak QPS:          60,000 QPS                 │
│   Peak QPS:              240,000 QPS                │
│   Off-peak servers:      139 (no redundancy)        │
│   Peak servers:          556 (no redundancy)        │
│   With 2X redundancy:    278 base + 960 peak     │
│   Auto-scaling needed:   +720 servers (4 hours)    │
│                                                      │
│ STORAGE (Storage Formula):                           │
│   Daily data:            1.8 EB                     │
│   After 5-year retention: 3,285 PB (uncompressed)  │
│   After compression:     2,190 PB (no redundancy)  │
│   With 2X redundancy:    7,300 PB total            │
│   Hot storage (SSD):     657 PB (1 year)           │
│   Warm storage (HDD):    3,723 PB (4 years)        │
│   Annual storage cost:   ~$85 million              │
│                                                      │
│ BANDWIDTH:                                           │
│   Peak bandwidth:        240,000 QPS × 2 KB/req    │
│                        = 555 GB/sec                 │
│   Network capacity:      Need 10X = 5.5 TB/sec     │
│                        = 44 Pbps (petabits/sec!)   │
│                                                      │
│ DATABASE CAPACITY:                                   │
│   Tweets table size:     ~1 PB (with indexes)      │
│   User profiles:         ~100 TB                    │
│   Timeline cache:        ~500 PB (hot data)        │
│   Total hot DB:          ~600 PB                    │
│                                                      │
│ CACHING LAYER (Redis/Memcached):                    │
│   Cache hit rate: 80% (80% of reads from cache)    │
│   Cache miss rate: 20% (20% hit database)          │
│   DB load if uncached: 240K QPS → 55K after cache │
│   Cache size needed: 200-500 PB (hot data only)   │
│                                                      │
└──────────────────────────────────────────────────────┘
```

### INFRASTRUCTURE DECISIONS

```
SERVERS (QPS-based):
  ├─ Web tier: 278 base servers (always on)
  ├─ Web tier: +834 peak servers (auto-scale 6-11 PM)
  ├─ Load balancer: Distribute across regions
  └─ Total: ~960 servers at peak

STORAGE (Storage-based):
  ├─ Hot SSD storage: 657 PB (expensive, for current data)
  ├─ Warm HDD storage: 3,723 PB (cheap, for archive)
  ├─ Replication: 2X across 2 data centers
  ├─ Compression: gzip for text (1.5X improvement)
  └─ Total annual: ~$85 million

CACHING:
  ├─ Cache popular tweets: 20% of storage
  ├─ Cache user timelines: 30% of storage
  ├─ Cache user profiles: 10% of storage
  ├─ Redis cluster: 500 PB capacity
  └─ Hit rate: 80% (massive database savings)

DATABASE:
  ├─ Master-slave replication: 2 copies
  ├─ Sharding: By tweet ID or user ID
  ├─ Indexes: Tweet creation time, user feed
  ├─ Partitioning: By date (hot vs cold)
  └─ Total DB: 600 PB (with 2X redundancy)

NETWORK:
  ├─ Peak bandwidth: 555 GB/sec
  ├─ Redundancy: 10X capacity = 5.5 TB/sec
  ├─ CDN for images/videos: 5 regions
  └─ Cost: ~$10 million/month

MONITORING:
  ├─ Alert if peak QPS > 300K
  ├─ Alert if storage > 4.5 ZB
  ├─ Alert if cache hit rate < 75%
  ├─ Auto-scale when QPS hits 75% capacity
  └─ Graceful degradation: Read-only mode at 100%
```

---

## 🔥 PEAK-ADJUSTED QPS FORMULA - Universal for Any System

```
╔════════════════════════════════════════════════════════╗
║ PEAK-ADJUSTED QPS FORMULA                              ║
╠════════════════════════════════════════════════════════╣
║                                                        ║
║ INPUTS:                                                ║
║  DAU = Daily Active Users                             ║
║  R = Requests per user per day                        ║
║  P = Peak multiplier (2X, 4X, 5X, etc.)              ║
║  Peak_hrs = Hours at peak rate (typical: 2-5 hours)  ║
║  Avg_hrs = Hours at average rate (24 - Peak_hrs)     ║
║                                                        ║
║ STEP 1: Calculate Off-Peak QPS                        ║
║  Off-peak QPS = (DAU × R) ÷ 100K                   ║
║                                                        ║
║ STEP 2: Calculate Peak QPS                            ║
║  Peak QPS = Off-peak QPS × P                         ║
║                                                        ║
║ STEP 3: Calculate Total Daily Requests                ║
║  Total = (Off-peak × 3,600 × Avg_hrs) +              ║
║          (Peak × 3,600 × Peak_hrs)                    ║
║                                                        ║
║ STEP 4: Calculate Servers Needed                      ║
║  Off-peak servers = Off-peak QPS ÷ Server_capacity   ║
║  Peak servers = Peak QPS ÷ Server_capacity           ║
║  With redundancy: Multiply by 2-3X                    ║
║                                                        ║
╚════════════════════════════════════════════════════════╝
```

**Works for:** Twitter, Instagram, Uber, Netflix, Stripe, or ANY system with users making requests.

**Key Insight:** Peak and average QPS are NOT added together. Peak is a multiplier (4X-5X) that occurs for only part of the day (2-5 hours typically).

---

## 💾 UNIVERSAL STORAGE FORMULA - For Any Data System

```
╔════════════════════════════════════════════════════════╗
║ UNIVERSAL STORAGE CALCULATION FORMULA                  ║
╠════════════════════════════════════════════════════════╣
║                                                        ║
║ STEP 1: Daily Data Generation                         ║
║  Daily_data = DAU × Data_per_user_per_day              ║
║                                                        ║
║ STEP 2: Apply Retention                               ║
║  Total_data = Daily_data × Retention_days             ║
║                                                        ║
║ STEP 3: Apply Redundancy (2X or 3X)                   ║
║  With_redundancy = Total_data × Redundancy_factor     ║
║                                                        ║
║ STEP 4: Apply Compression (if applicable)             ║
║  Final_storage = With_redundancy ÷ Compression_ratio  ║
║                                                        ║
║ EXAMPLE:                                               ║
║  DAU = 300M, Data/user = 6 MB, Retention = 5 years    ║
║  Redundancy = 2X, Compression = 1.5X                  ║
║                                                        ║
║  Daily = 300M × 10 MB = 3 PB                         ║
║  Retention = 3 PB × 1,825 days = 5,475 PB         ║
║  Redundancy = 5,475 PB × 2 = 6,570 PB                ║
║  Compression = 10,950 PB ÷ 1.5 = 7,300 PB      ║
║                                                        ║
╚════════════════════════════════════════════════════════╝
```

**Quick Reference by System Type:**

| System | Data/User/Day | Retention | Redundancy | Typical Storage |
|--------|---------------|-----------|------------|-----------------|
| Twitter | 6 MB | 5 yrs | 2X | 50-100 PB |
| Instagram | 7 MB | 10 yrs | 3X | 1-2 EB |
| Logs (30 days) | 1 MB | 30 days | 2X | 50 PB |
| Database | 1-10 MB | 7 yrs | 2X | 100 TB - 10 PB |

**Key Insight:** Retention period is critical! Even small daily data × 10 years retention = massive storage. Use tiered storage: Hot (SSD/expensive) → Warm (HDD/cheap) → Cold (Archive/very cheap).

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
Formula: Minutes/year × (1 - Uptime%) = Downtime in minutes
         525,600 minutes/year × downtime% = annual downtime

99% uptime (2 nines):
  Downtime per year: 365 × 24 × 60 × 0.01 = 5,256 minutes
                    = 87.6 hours
                    = 3.65 days per year
  
99.9% uptime (3 nines):
  Downtime per year: 365 × 24 × 60 × 0.001 = 525.6 minutes
                    = 8.76 hours per year
  
99.99% uptime (4 nines):
  Downtime per year: 365 × 24 × 60 × 0.0001 = 52.6 minutes per year
  
99.999% uptime (5 nines):
  Downtime per year: 365 × 24 × 60 × 0.00001 = 5.26 minutes per year
                    = 5 minutes 15 seconds per year
```

**Note on the calculation:**
- Each additional "nine" reduces downtime by 90%
- Moving from 99% to 99.9% reduces downtime from 87.6 hours to 8.76 hours
- Cost increases exponentially: each nine costs ~10x more infrastructure
- Most production systems aim for 99.99% (4 nines), not 99.999% (5 nines)

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

#### The Simple Mental Model: The "5 D's"

Each level of uptime requires solving these problems:

```
1. DETECTION — How fast do you find the problem? (seconds vs minutes)
2. DIAGNOSIS — How fast do you know what failed? (automated vs manual)
3. DIVERSION — How fast do you route traffic away? (instant vs delayed)
4. DUPLICATION — How many backups do you have? (1 vs 3+ copies)
5. DISTRIBUTION — How spread out is your system? (1 region vs 5+ regions)
```

---

#### 3 NINES (99.9%) - Basic Redundancy | 8.76 hours downtime/year

**The Goal:** One component can fail, system stays up

**What You're Doing:**
```
BEFORE: 1 database → crashes → data lost → service down (hours)

AFTER:  Master DB + Slave DB → Master crashes → Slave takes over
        (data saved, service back in minutes)
```

**Required Techniques:**

**1️⃣ Database Replication (Master-Slave)**
```
Order arrives → Written to Master → Automatically copied to Slave
If Master dies → Slave has all the data
Recovery time: 2-5 minutes (manual or semi-automatic)
Benefit: Survive database failure
```

**2️⃣ Load Balancing**
```
Users → Load Balancer → [Server 1, Server 2, Server 3]
If Server 1 crashes → Balancer routes to Server 2
No single point of failure in web tier
Benefit: Survive single server failure
```

**3️⃣ Basic Failover**
```
BEFORE: Server crashes → Manual restart takes 30 minutes
AFTER:  Health check every 5 minutes detects crash → Auto-restart
Recovery time: 2-5 minutes
Benefit: No human intervention needed
```

**Why 3 nines?**
```
Can survive: 1 database failure + 1 server failure
Cannot survive: 2 databases failing simultaneously
Detection time: ~5 minutes (health checks every 5 min)
Recovery time: ~2-5 minutes
Total: ~10 minutes downtime per incident
```

**Cost & Examples:**
```
Cost: $50K-$200K/year infrastructure
Used by: Slack, GitHub, most SaaS companies
Pattern: RLF = Replication + Load balancing + Failover
```

---

#### 4 NINES (99.99%) - Geographic Distribution | 52.6 minutes downtime/year

**The Goal:** One entire region can fail, system stays up
**Improvement:** 10x better uptime = need 10x faster detection + recovery

**What You're Adding:**

**1️⃣ Multi-Region Deployment (Biggest Win)**
```
BEFORE: All servers in 1 datacenter
  Earthquake hits → Entire datacenter offline → Service gone for hours

AFTER: Servers in 3 regions (US, EU, Asia)
  US datacenter destroyed → EU + Asia still running
  Users automatically routed to nearest working region
  
Benefit: Survive entire region failure (earthquake, power outage, etc)
```

**2️⃣ Sub-Second Health Checks**
```
BEFORE: Check every 5 minutes
  Server crashes at 10:00 → Detected at 10:05 → Fixed at 10:10 = 10 min down

AFTER:  Check every 10 seconds
  Server crashes at 10:00 → Detected at 10:00:10 → Fixed at 10:00:20 = 20 sec down
  
Benefit: 30x faster detection = 30x better availability
```

**3️⃣ Fully Automated Failover**
```
BEFORE: "Server is down, let me check logs, diagnose, then restart" (manual)
AFTER:  Server crashes → Instantly routed to backup → New server auto-starts

No human intervention needed
Can happen at 2am without waking anyone
```

**4️⃣ Continuous Health Monitoring**
```
Not just "is the server up?" but:
  • CPU usage trending upward?
  • Memory leaks starting?
  • Response time degrading?
  • Network latency increasing?
  
Scale up BEFORE failure instead of reacting to failure
Prevents cascading failures
```

**Why 4 nines?**
```
Can survive: Any 1 region failure + Any 1 server failure
             Simultaneous failures in different regions
Cannot survive: All 3 regions failing at once (unlikely)
Detection time: ~10 seconds
Recovery time: ~10 seconds (automatic rerouting)
Total: ~20 seconds downtime per incident
```

**Cost & Examples:**
```
Cost: $100K-$500K/year infrastructure
Used by: AWS, Google Cloud, Stripe, Netflix, Uber
Pattern: AHM = Automated + Health checks (frequent) + Multi-region
🎯 THIS IS THE SWEET SPOT FOR MOST SYSTEMS
```

---

#### 5 NINES (99.999%) - Extreme Redundancy | 5.26 minutes downtime/year

**The Goal:** Multiple failures simultaneously don't break service
**Reality Check:** Almost never justified for commercial systems

**What You're Adding:**

**1️⃣ Redundant Network Providers (ISPs)**
```
BEFORE: Use 1 ISP
  ISP has backbone failure → You're unreachable → Service down

AFTER: Use 3 different ISPs (Comcast, Verizon, Level3)
  ISP-1 dies → ISP-2 + ISP-3 keep you online
  
Cost per ISP: ~$100K+/month
Benefit: Survive ISP failure (extremely rare)
```

**2️⃣ Zero-Downtime Deployments**
```
BEFORE: Deploy new code
  Stop old servers → Start new servers → 30 second gap = DOWNTIME

AFTER: Canary Deployment
  Deploy to 1% of servers → Monitor → Deploy to 10% → Monitor → 100%
  If something breaks, rollback instantly (0 downtime)
  
No downtime during normal operations
```

**3️⃣ AI-Powered Prediction**
```
BEFORE: "Alert if CPU > 80%"
  Server uses 85% CPU → Hits 95% CPU → Crashes

AFTER: ML model predicts failure
  "This server will crash in 2 hours based on trends"
  Scale up now, prevent the crash entirely
  
No crash = no recovery needed = no downtime
```

**4️⃣ Redundant Everything**
```
BEFORE: 3 database replicas
AFTER:  5 database replicas + redundant cache servers + 
        redundant load balancers + redundant network gear
        
Multiple redundancy at every layer
```

**Why 5 nines?**
```
Can survive: Multiple simultaneous component failures
             Complete region failure + concurrent issues
Cannot survive: Basically everything you can think of

Detection time: <1 second (ML prediction + instant monitoring)
Recovery time: <1 second (automatic, no human involved)
Total: Service essentially never goes down
```

**Cost & Examples:**
```
Cost: $5M-$20M+/year infrastructure
Real examples: 
  ✓ Nuclear power plants (lives depend on it)
  ✓ Hospital life-support systems
  ✓ 911 emergency dispatch
  ✓ Air traffic control
  ✓ Stock exchange (milliseconds = millions)
  
NOT used by: Netflix, Facebook, Google, etc (wastes money)
```

---

#### Memory Trick: The Ladder of Protection

```
                    5 NINES              99.999% (5 min/year)
                      ↑
              Planetary redundancy
              AI-powered prediction
              Zero-downtime updates
              Redundant ISPs
                      ↑
                    4 NINES              99.99% (52 min/year)
                      ↑
              Multi-region
              Sub-second health checks
              Full automation
                      ↑
                    3 NINES              99.9% (8.76 hrs/year)
                      ↑
              Replication
              Load balancing
              Basic failover
```

---

#### Mnemonic: "RLF → AHM → PRZ" (Detailed Breakdown)

**Remember what each level needs:**

---

### **R = REPLICATION (Which components?)**

**Replicating:** Database, Cache, Session Store, Message Queues

**1️⃣ Database Replication (Master-Slave)**

```
SINGLE POINT OF FAILURE (❌):
  User → App → [Master DB] → crashes → DATA LOST, SERVICE DOWN

WITH REPLICATION (✓):
  User → App → [Master DB] → continuously copies to:
                                 └─ Slave DB-1 (backup)
                                 └─ Slave DB-2 (backup)

Example Flow:
  User creates order: INSERT INTO orders VALUES (user=123, amount=$50)
  ↓ Replication
  Master DB: Has the order
  Slave DB-1: Has exact copy
  Slave DB-2: Has exact copy
  
If Master crashes:
  ✓ Promote Slave-1 to Master
  ✓ Data is safe, service back in 2-5 minutes
```

**2️⃣ Cache Replication (Redis)**

```
WITHOUT REPLICATION:
  App → [Redis Cache] crashes → All hot data lost
                                Next query: 5 second latency (slow database)

WITH REPLICATION (Redis Cluster):
  App → Redis Node-1 (master)
         ↓ auto-replicates to
         Redis Node-2 (backup)
         Redis Node-3 (backup)
         
If Node-1 crashes:
  ✓ Nodes 2 & 3 still have all cached data
  ✓ Traffic switches to Node-2
  ✓ Cache hits work, fast queries continue
```

**3️⃣ Session Store Replication**

```
User logs in: Session stored in Session DB
If Session DB crashes and NO replication:
  ✓ All users get logged out (session lost)
  ✗ Angry customers

With replication:
  Session saved to Master Session DB
           ↓ copies to
           Replica Session DB-1
           Replica Session DB-2
           
If Master crashes:
  ✓ User stays logged in (session exists in replicas)
  ✓ Switch to replica automatically
```

---

### **L = LOAD BALANCING (Which components?)**

**Load Balancing:** Web Servers, Database Replicas, Cache, DNS

**1️⃣ Web Server Load Balancing (Primary)**

```
WITHOUT LOAD BALANCER (❌):
  User 1 ──┐
  User 2 ──┼→ [Web Server-1] (100% CPU, crashes)
  User 3 ──┤
  User 4 ──┘
  Result: SERVICE DOWN FOR ALL

WITH LOAD BALANCER (✓):
  User 1 ──┐
  User 2 ──┼→ [Load Balancer] ──→ [Web-1] 25% traffic
  User 3 ──┤      (nginx)         [Web-2] 25% traffic
  User 4 ──┘                       [Web-3] 25% traffic
                                   [Web-4] 25% traffic
                                   
Distribution algorithm:
  Round-robin: User 1→Web-1, User 2→Web-2, User 3→Web-3, User 4→Web-4
  Least connections: Send to server with fewest active connections
  
If Web-1 crashes:
  ✓ Load Balancer routes to Web-2, 3, 4
  ✓ Service still works, just slower
  ✓ Traffic automatically rebalances
  
Real request flow:
  User clicks "Create Post"
  → Load Balancer: "Send to Web-2"
  → Web-2 processes, saves to database
  → Returns response to user
```

**2️⃣ Database Read Replica Load Balancing**

```
WITHOUT LOAD BALANCING (❌):
  Write: "Update user profile" → Master DB
  Read: "Get user profile" → Master DB (SAME SERVER!)
  
Problem: Master DB doing BOTH reads and writes
  CPU: 100%
  No capacity for writes
  
WITH LOAD BALANCING (✓):
  Write: "Update user profile" → [Master DB] (only handles writes)
                                      ↓ auto-copies to:
  Read: "Get user profile" ───→ [Replica-1]
                            ├─→ [Replica-2]
                            └─→ [Replica-3]
  
Example:
  "Get user profile" → Load Balancer → Replica-2 (fast)
  "Create order" → Master DB only (writes must go here)
  "Search products" → Load Balancer → Replica-1 (fast)
  
Benefits:
  Master: 10% CPU (writes only)
  Replicas: 30% CPU each (reads distributed)
```

**3️⃣ Cache Load Balancing**

```
Cache requests distributed across Redis nodes:

User 1 requests "user:123:profile"
  → Consistent Hash → [Redis-1]
  
User 2 requests "user:456:profile"
  → Consistent Hash → [Redis-2]
  
User 3 requests "user:789:profile"
  → Consistent Hash → [Redis-3]
  
No single Redis node gets all the load
Each handles 1/3 of cache requests
```

---

### **F = FAILOVER (Which components? How?)**

**Automatic Failover for:** Database, Web Servers, Cache

**1️⃣ Database Failover (Automatic)**

```
MANUAL FAILOVER (❌ Too slow):
  10:00 - Master DB crashes
  10:00:05 - Alert fires
  10:00:30 - On-call engineer wakes up
  10:02:00 - Engineer logs in
  10:05:00 - Engineer manually promotes Slave to Master
  10:05:30 - Application updated
  Result: 5+ MINUTES OF DOWNTIME ❌

AUTOMATIC FAILOVER (✓ Fast):
  10:00:00 - Master DB crashes
  10:00:05 - Health check: "SELECT 1" times out → Master is DEAD
  10:00:10 - Failover script triggers automatically
  10:00:15 - Slave promoted to Master
  10:00:20 - Applications redirected
  Result: 20 SECONDS OF DOWNTIME ✓

Health Check Mechanism:
  Every 10 seconds:
    Load Balancer → Master DB: "SELECT 1 FROM health_check"
    If timeout (3 failures in a row):
      → Mark Master as UNHEALTHY
      → Trigger: promote_slave_to_master()
      → Update DNS/connection strings
```

**2️⃣ Web Server Failover**

```
WITHOUT FAILOVER (❌):
  [Load Balancer] ──→ [Web-1] responding: 100ms ✓
                   ──→ [Web-2] hanging/crashed ✗
                   ──→ [Web-3] responding: 101ms ✓
  
Load Balancer still sends traffic to Web-2!
Some users get errors, timeouts

WITH FAILOVER (✓):
  Health Check Loop (every 10 seconds):
    - Web-1: responds in 50ms → HEALTHY ✓
    - Web-2: no response → UNHEALTHY ✗
    - Web-3: responds in 51ms → HEALTHY ✓
  
  Load Balancer action:
    "Only send traffic to Web-1 and Web-3"
    Web-2 removed from rotation
  
  Result: No users hit the broken server
  
When Web-2 recovers:
  Health check succeeds → Add back to rotation automatically
```

**3️⃣ Cache Failover (Redis Cluster)**

```
User requests "profile:user:123" from Redis-1

If Redis-1 dies mid-request:
  Redis Cluster automatically:
    ✓ Detects Redis-1 dead
    ✓ Promotes Redis-1's replica to master
    ✓ Client redirects to new master
    ✓ Request retried and succeeds
    
All in ~1 second, completely transparent to application
No downtime, no errors
```

---

### **A = AUTOMATION (What gets automated?)**

**Automating:** Failover, Scaling, Remediation, Deployments

**1️⃣ Automated Failover**

```
MANUAL:
  Database crashes → On-call engineer → Fix → Service back online

AUTOMATED:
  Database crashes → Automatic promotion → Service back online
  Engineer finds out via logs in morning (post-incident)
  
Pseudocode:
  while True:
    if master_db_health_check() fails:
      best_slave = find_healthy_slave()
      promote_to_master(best_slave)
      update_app_config(new_master_address)
      send_alert("Failover completed")
```

**2️⃣ Automated Scaling (Horizontal)**

```
MANUAL SCALING (❌ Too slow):
  10:00 - Flash sale starts, traffic spikes
  10:05 - Engineer on-call notices high CPU
  10:15 - Engineer spins up 5 new servers
  10:25 - Servers added to load balancer
  10:00-10:25 - Service is slow, some users timeout

AUTOMATED SCALING (✓ Instant):
  10:00:00 - CPU reaches 80% on all servers
  10:00:05 - Autoscaler: "Need more capacity"
  10:00:10 - Spin up 5 new servers
  10:00:20 - New servers healthy, added to load balancer
  10:00:25 - CPU drops to 50%, service stays fast
  
  Customer never notices slowness
```

**3️⃣ Automated Remediation**

```
Memory Leak Scenario:

MANUAL:
  Memory 90% → Alert fires → Engineer wakes up → Restart server

AUTOMATED:
  Memory trending up for 2 hours
  Autoremediation predicts: "Will hit 100% in 30 min"
  Automatically:
    1. Drain traffic from this server (redirect to others)
    2. Restart it (while handling traffic elsewhere)
    3. Bring back online when ready
  
  Fixed before customer notice, before memory hits 100%
```

---

### **H = HEALTH CHECKS (What gets checked?)**

**Checking:** Application, Infrastructure, Dependencies

**1️⃣ Application Level Health Checks**

```
Every 10 seconds, load balancer asks each web server:
  GET /healthz HTTP/1.1
  
Server responds:
  HTTP 200 OK
  {
    "status": "healthy",
    "database": "connected",
    "cache": "connected",
    "disk_space": "50%",
    "memory": "65%"
  }

If any component fails:
  {
    "status": "unhealthy",
    "cache": "unreachable_timeout"
  }
  
Load Balancer sees "unhealthy":
  → Remove from rotation
  → Trigger automatic remediation
  → Try to restart cache connection
```

**2️⃣ Infrastructure Level Health Checks**

```
Monitor every 30 seconds:
  ✓ CPU usage (alert if >80%)
  ✓ Memory usage (alert if >85%)
  ✓ Disk usage (alert if >90%)
  ✓ Network latency (alert if >100ms)
  ✓ Packet loss (alert if >0.1%)
  
Example:
  Can reach server? (ping) ✓
  Response time? (<200ms expected) ✓
  Error rate? (<1% expected) ✗ (ERROR RATE 5%!)
  
  Fails 3 checks in a row:
    → Mark server UNHEALTHY
    → Remove from load balancer
    → Automatic remediation: restart/scale up
```

**3️⃣ Dependency Health Checks**

```
Check external dependencies every 30 seconds:
  ✓ Can reach database? (test connection)
  ✓ Can reach cache? (test Redis PING)
  ✓ Can reach message queue? (test Kafka)
  ✓ Can reach payment API? (test API endpoint)
  
If payment API is down:
  App knows: "External dependency unavailable"
  Shows to user: "Checkout temporarily unavailable"
  Logs request to retry queue
  When API recovers: Service auto-recovers
```

---

### **M = MULTI-REGION (What gets distributed?)**

**Distributing:** Web Servers, Databases, Cache, DNS

**1️⃣ Web Servers in Multiple Regions**

```
SINGLE REGION (❌ Fragile):
  US-East Datacenter: [Web-1, Web-2, Web-3, Master DB]
  
  Earthquake hits Virginia:
    ✗ Entire datacenter offline
    ✗ Service completely unavailable
    ✗ All users affected

MULTI-REGION (✓ Resilient):
  US-East:        [Web-1, Web-2, Web-3, Master DB]
  EU-Central:     [Web-4, Web-5, Web-6, Slave DB]
  Asia-Pacific:   [Web-7, Web-8, Web-9, Slave DB]
  
  Earthquake hits Virginia:
    ✓ US-East offline
    ✓ Traffic automatically routed to EU and Asia
    ✓ Service works, users in Europe see ~150ms instead of 20ms
    ✓ No service outage
    
Real companies: Facebook, Netflix, Google do this
```

**2️⃣ Database Replication Across Regions**

```
Master DB (US-East):
  ↓ Continuous replication (100-200ms latency)
Slave DB (EU-Central): Gets all writes within 100ms
Slave DB (Asia-Pacific): Gets all writes within 100ms

Write flow:
  User in US creates post
  → Written to Master in US-East
  → Replicated to EU and Asia within 100ms
  → User in EU can see post within 100ms
  
If US-East fails:
  EU Master still has all data
  Promote EU replica to master
  Service continues in EU and Asia
```

**3️⃣ GeoDNS Routing**

```
User in US:
  DNS query "facebook.com" 
  → Points to US datacenter
  → 10ms latency ✓
  
User in Europe:
  DNS query "facebook.com"
  → Points to EU datacenter
  → 10ms latency ✓
  
User in Asia:
  DNS query "facebook.com"
  → Points to Asia datacenter
  → 10ms latency ✓
  
If US datacenter completely fails:
  DNS automatically updates:
    "US queries now route to EU"
  Users see ~200ms latency (crossing Atlantic)
  Instead of: Service down
```

---

### **P = PREDICTION (What do we predict?)**

**Predicting:** Capacity needs, Failures, Anomalies

**1️⃣ Capacity Prediction (ML-Based)**

```
REACTIVE (❌):
  Load increases → CPU 100% → Service crashes → Users lose money

PREDICTIVE (✓):
  ML model watches CPU trend:
    10:00 - CPU 30%
    10:30 - CPU 40% (+10/30min)
    11:00 - CPU 50% (+10/30min)
    11:30 - CPU 60% (+10/30min)
    Prediction: "CPU hits 100% at 12:00"
  
  At 11:45:
    ✓ System auto-scales: Spin up 5 new servers
    ✓ By 12:00: CPU is 70% (problem solved before it happens)
    
  No crash, no downtime
  User never notices
```

**2️⃣ Failure Prediction**

```
Memory Leak Detection:

Server metrics over 8 hours:
  Tuesday 9am: 50% memory
  Tuesday 2pm: 60% memory (+10% per 5 hours)
  Tuesday 6pm: 70% memory
  Prediction: "Will hit 100% at Wednesday 10am"
  
  Tuesday 8pm (still within 24 hours):
    ✓ Autoremediation triggers
    ✓ Drain traffic from this server
    ✓ Graceful restart (no user impact)
    ✓ Problem solved before failure
```

**3️⃣ Anomaly Detection**

```
Normal behavior (baseline):
  Average response time: 100ms
  P99 response time: 150ms
  Error rate: 0.01%

Anomaly detected:
  Response time: 500ms (5x worse!)
  P99: 1000ms (7x worse!)
  Error rate: 5% (500x worse!)
  
Automated response:
  If critical anomaly:
    Option 1: Auto-rollback last deployment
    Option 2: Redirect traffic to previous version
    Option 3: Alert on-call engineer + auto-remediate
  
  Service restored in 10 seconds instead of 10 minutes
```

---

### **R = REDUNDANT ISPs (Which providers?)**

**Redundancy:** Network Providers, CDN Providers, Cloud Providers

**1️⃣ Dual ISP Setup**

```
SINGLE ISP (❌):
  Your Datacenter → [ISP: Verizon only]
  
  Verizon backbone failure in that region:
    ✗ Your datacenter has no internet
    ✗ Service unreachable
    ✗ Users can't reach you

DUAL ISP (✓):
  Your Datacenter → [ISP-1: Verizon]
                  → [ISP-2: Comcast]
  
  BGP routing: Traffic uses BOTH ISPs simultaneously
  
  If Verizon fails:
    ✓ All traffic routes through Comcast automatically
    ✓ Service still reachable
    ✓ 0 downtime
    
Cost: ~$100K+/month extra (why 5 nines is expensive)
```

**2️⃣ Redundant CDN Providers**

```
SINGLE CDN (❌):
  All static content (images, JS, CSS) → Cloudflare CDN
  
  Cloudflare massive outage:
    ✗ Users can't load images/JavaScript
    ✗ Website broken for all users

DUAL CDN (✓):
  Static content → Cloudflare CDN (primary)
               → Akamai CDN (fallback)
  
  If Cloudflare fails:
    DNS updates → Traffic routes to Akamai
    ✓ Images still load
    ✓ JavaScript still works
    ✓ Website fully functional
```

**3️⃣ Redundant Cloud Providers**

```
SINGLE CLOUD (❌):
  Everything on AWS
  
  AWS regional failure (rare but possible):
    ✗ Service down for that region

MULTI-CLOUD (✓ Extreme):
  Critical systems on:
    - AWS (primary)
    - Google Cloud (backup)
    - Azure (tertiary)
  
  If AWS has issues:
    Failover to Google Cloud
    Service continues
    
Cost: Triple infrastructure cost ($5M-$20M+/year)
Used by: Banks, governments, critical infrastructure
```

---

### **Z = ZERO-DOWNTIME UPDATES (How?)**

**Technique:** Deployments, Code Changes, Config Updates

**1️⃣ Blue-Green Deployment**

```
ROLLING RESTART (❌ Causes downtime):
  [Server-1: v1.0] → Restart with v1.1 → 10 sec down
  [Server-2: v1.0] → Restart with v1.1 → 10 sec down
  [Server-3: v1.0] → Restart with v1.1 → 10 sec down
  
  During each restart: Some traffic interrupted

BLUE-GREEN (✓ 0 downtime):
  BLUE environment (running v1.0):
    [Server-1: v1.0]
    [Server-2: v1.0]
    [Server-3: v1.0]
    Load Balancer sends all traffic here
  
  Deploy v1.1 to GREEN environment (parallel):
    [Server-4: v1.1]
    [Server-5: v1.1]
    [Server-6: v1.1]
  
  Test GREEN thoroughly:
    - Run smoke tests
    - Check error rate
    - Monitor latency
  
  When GREEN is proven good:
    Load Balancer: Switch all traffic from BLUE → GREEN
    (Atomic switch in 1 second)
  
  Result: ZERO downtime
  
  If v1.1 has a critical bug:
    Load Balancer: Switch back to BLUE
    Rollback in 1 second
```

**2️⃣ Canary Deployment**

```
Deploy v1.1 gradually, monitoring for issues:

  Stage 1: Deploy to 1% of servers (Server-1 only)
    ├ 10,000 total users
    ├ ~100 users hit new version
    └ Monitor: error rate, latency, exceptions
  
  Stage 2: If Stage 1 good, deploy to 5% (Servers 1-5)
    ├ ~500 users on new version
    └ Monitor: still looking good?
  
  Stage 3: If Stage 2 good, deploy to 25% (Servers 1-25)
    ├ ~2,500 users on new version
    └ Monitor: larger scale, any issues?
  
  Stage 4: Deploy to 100%
    All servers now v1.1
  
Benefits:
  - Catch bugs early affecting only small % of users
  - If critical bug found: Rollback only 1% initially
  - Gradual rollout = 0 downtime even if issues found
  - Safe to deploy during business hours
```

**3️⃣ Rolling Update**

```
Update web servers without any service interruption:

Initial state:
  [LB] → [Web-1: v1.0] ← serving traffic
      → [Web-2: v1.0] ← serving traffic
      → [Web-3: v1.0] ← serving traffic

Step 1: Update Web-1
  [LB] → Drain Web-1 (let existing requests finish)
      → [Web-2: v1.0] ← serving traffic
      → [Web-3: v1.0] ← serving traffic
  
  Web-1 offline for ~30 seconds
    - Restart with v1.1
    - Run healthchecks
  
  [LB] → [Web-1: v1.1] ← now serving traffic again
      → [Web-2: v1.0] ← serving traffic
      → [Web-3: v1.0] ← serving traffic

Step 2: Update Web-2 (same process)
Step 3: Update Web-3 (same process)

Result:
  Service never went down
  Updated from v1.0 → v1.1 with 0 downtime
  Always at least 2 servers handling traffic
```

---

## **Complete Real-World Example: Netflix Deploys New Feature**

### **Using 3 NINES (RLF):**

```
1. New code deployed to 3 web servers simultaneously
2. Load balancer distributes traffic across all 3
3. If 1 server crashes during deploy:
     Load balancer routes to other 2
   Service degrades but stays up
4. If master database crashes:
     Ops engineer manually promotes slave (takes 5-10 min)
5. No automation, human involvement required

Downtime if failure occurs: 5-10 minutes
```

### **Using 4 NINES (AHM):**

```
1. Canary deploy: First to 1% of servers
   Monitor error rate for 5 min
2. If good, deploy to 10%, 50%, then 100%
3. Health checks every 10 seconds detect anomalies
4. If error rate spikes: Automatic rollback
   Service restored in 10 seconds
5. Multi-region: If US datacenter fails:
     Traffic automatically routed to EU and Asia
6. Automated failover: No human intervention needed
7. Autoscaling: If traffic spikes, servers auto-added

Downtime if failure occurs: <1 minute (automatic)
```

### **Using 5 NINES (PRZ):**

```
1. ML predicts traffic will spike 30 min before it happens
   Auto-scales servers preventively
2. Canary deploy with advanced ML anomaly detection
   Can rollback before customer impact
3. Dual ISP: If ISP-1 fails, traffic routes through ISP-2
4. Blue-green deployment: Switch versions in 1 second
   If bug found: Rollback in 1 second
5. Redundant cloud providers: If AWS region fails, failover to GCP
6. Zero downtime: Everything happens seamlessly

Downtime if failure occurs: 0 minutes (service never goes down)
```

---



---

#### Interview Answer Template

**When asked: "How would you achieve 99.99% uptime?"**

```
"To move from 99.9% (3 nines) to 99.99% (4 nines), I'd add:

1. DETECTION (faster): 
   Health checks every 10 seconds instead of 5 minutes
   → Detects failures 30x faster = 30x better uptime

2. DISTRIBUTION (wider): 
   Multi-region deployment (US, EU, Asia)
   → Survive entire datacenter failure or regional outage

3. DECISION (faster): 
   Fully automated failover, no human intervention
   → System responds instantly at 2am without waking anyone

4. DUPLICATION (redundant): 
   Multiple database replicas, cache servers, load balancers
   → No single point of failure

Example: Netflix achieves 99.99% with these practices
across 5+ regions globally.

If asked about 99.999% (5 nines): 
'That would require $5M+/year and redundant ISPs. 
I'd push back and ask: Why do we need it? 
Usually 99.99% is sufficient for commercial systems.'
"
```

---

### Real-World SLA Targets

| Type | Company | SLA Target | Reality |
|---|---|---|---|
| **Cloud Providers** | AWS, Google Cloud, Azure | 99.99% (4 nines) | Most achieve this |
| **Payments** | Stripe, Square | 99.99% (4 nines) | Financial regulations require high reliability |
| **Social Media** | Facebook, Twitter, Instagram | 99.99% (4 nines) | Design for scale + reliability |
| **Streaming** | Netflix, YouTube | 99.99% (4 nines) | Built for massive scale |
| **Messaging** | Slack, Discord | 99.9% (3 nines) | "Aim for 99.99%, guarantee 99.9%" |
| **Search** | Google Search | 99.99% (4 nines) | Geo-distributed, always available |
| **Ride-sharing** | Uber, Lyft | 99.99% (4 nines) | Safety-critical, but not life-critical |
| **Critical Infrastructure** | Power grids, hospitals, 911 | 99.999%+ (5+ nines) | Life-critical systems |

---

### Interview Insight: When Asked About 5 Nines

**What to say:**
```
"Most commercial systems don't actually need 5 nines because:

1. Cost-benefit is terrible: 10x cost for 1% improvement
2. You can't achieve it alone: ISPs, cloud providers, 
   dependencies must also provide 5 nines
3. Industry standard is 4 nines: This provides 52 minutes 
   downtime/year, which is acceptable for most businesses

I'd first ask: 'What's the actual business requirement?' 
Most times the answer is 99.99%, not 99.999%."
```

**When you DO need 5 nines:**
- Systems where downtime costs millions per minute
- Life-critical systems (hospitals, emergency services)
- National security/military systems
- Power grid or infrastructure
- In interviews: Usually a flag that you should push back and ask WHY

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
1 day = 24 hours × 60 minutes × 60 seconds = 100K seconds
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
  = 300M ÷ 100K
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

## Peak-Adjusted QPS Formula

**Official Name:** Peak-Adjusted QPS Formula (also called "Daily Load Distribution Model")

A universal formula to calculate system requirements for any service with users making requests. Works for social media, APIs, payments, streaming, ride-sharing, or any system where you can estimate user activity patterns.

### The Formula

```
╔════════════════════════════════════════════════════════╗
║ PEAK-ADJUSTED QPS FORMULA                              ║
╠════════════════════════════════════════════════════════╣
║                                                        ║
║ INPUTS:                                                ║
║  DAU = Daily Active Users                             ║
║  R = Requests per user per day                        ║
║  P = Peak multiplier (2X, 4X, 5X, etc.)              ║
║  Peak_hrs = Hours at peak rate (typical: 2-5 hours)  ║
║  Avg_hrs = Hours at average rate (24 - Peak_hrs)     ║
║                                                        ║
║ STEP 1: Calculate Off-Peak QPS                        ║
║  Off-peak QPS = (DAU × R) ÷ 100K                   ║
║                                                        ║
║ STEP 2: Calculate Peak QPS                            ║
║  Peak QPS = Off-peak QPS × P                         ║
║                                                        ║
║ STEP 3: Calculate Total Daily Requests                ║
║  Total = (Off-peak × 3,600 × Avg_hrs) +              ║
║          (Peak × 3,600 × Peak_hrs)                    ║
║                                                        ║
║ STEP 4: Calculate Servers Needed                      ║
║  Off-peak servers = Off-peak QPS ÷ Server_capacity   ║
║  Peak servers = Peak QPS ÷ Server_capacity           ║
║  With redundancy: Multiply by 2-3X                    ║
║                                                        ║
╚════════════════════════════════════════════════════════╝
```

### Real Example: Twitter

```
Given:
  DAU = 300 million
  Requests per user = 20 per day
  Peak multiplier = 4X
  Peak hours = 4 (7-11 PM)
  Average hours = 19
  Server capacity = 500 QPS per server

Calculation:

Step 1: Off-peak QPS
  (300M × 20) ÷ 100K = 60,000 QPS

Step 2: Peak QPS
  69,444 × 4 = 240,000 QPS

Step 3: Total daily requests
  (69,444 × 3,600 × 19) + (277,776 × 3,600 × 5)
  = 4,719,600,000 + 4,968,000,000
  = 9,687,600,000 requests/day

Step 4: Servers needed
  Off-peak: 69,444 ÷ 500 = 120 servers
  Peak: 277,776 ÷ 500 = 480 servers
  With 2X redundancy: 278 base, 960 peak
  Auto-scale: +720 servers during peak hours
```

### Applications by System Type

| System | DAU | Requests/User | Peak Factor | Peak Hours | Example |
|--------|-----|---------------|-----------  |------------|---------|
| Twitter | 300M | 20 | 4X | 5 | 480 servers peak |
| Instagram | 500M | 30 | 4X | 5 | 926 servers peak |
| Uber | 10M | 2 | 5X | 3 | 116 servers peak |
| Netflix | 300M | 5 | 3X | 4 | 52 servers peak |
| Stripe API | 100K | 500 | 2X | 2 | 1,157 servers peak |

### Key Insights

**1. Peak duration matters heavily**
```
Same peak QPS (276K), different peak durations:

2 peak hours → 158 avg servers needed
5 peak hours → 184 avg servers needed
10 peak hours → 236 avg servers needed

Conclusion: Longer peaks require more infrastructure!
```

**2. Peak and average QPS are NOT added**
```
Off-peak:  60,000 QPS  (occurs 19 hours/day)
Peak:      240,000 QPS (occurs 4 hours/day)

WRONG: 69,444 + 277,776 = 347,220 total
RIGHT: Peak is 4X multiplication, not addition
```

**3. This formula applies to ANY request-based system**
```
✓ Works for: Social media, APIs, payments, video, messaging, commerce
✓ Key requirement: Can estimate R (requests per user per day)
✓ Adjust P (peak multiplier) based on traffic patterns
✓ Peak_hrs depends on timezone + user behavior
```

### How to Choose Peak Hours and Multiplier

```
Light Usage System (e.g., expense tracking app):
  Peak hrs: 2-3 hours (lunch break, evening)
  Peak multiplier: 2-3X

Medium Usage System (e.g., Twitter, Instagram):
  Peak hrs: 5-8 hours (evening/night globally)
  Peak multiplier: 4-5X

Heavy Usage System (e.g., gaming, messaging):
  Peak hrs: 10-12 hours (continuous)
  Peak multiplier: 2-3X (less extreme spikes)

Global System (e.g., YouTube, Google):
  Peak hrs: 8-12 hours (offset across timezones)
  Peak multiplier: 2-4X (more smoothed out)
```

### When to Use This Formula

```
✓ Interview setting: Estimate infrastructure for any system
✓ Capacity planning: Know how many servers to buy
✓ Cost forecasting: Calculate cloud infrastructure spend
✓ Scaling strategy: Understand when to auto-scale
✓ Design decisions: Choose architecture based on QPS

✗ Don't use if: 
  - Traffic is completely unpredictable
  - System has 24/7 uniform load (no peak/average split)
  - Request patterns vary drastically by user type
```

---

## Universal Storage Calculation Formula

**Official Name:** Universal Storage Estimation Formula

A generic formula to calculate total storage requirements for any system. Works for databases, file storage, logs, caches, media (photos, videos), messages, or any data you need to persist.

### The Formula

```
╔════════════════════════════════════════════════════════╗
║ UNIVERSAL STORAGE CALCULATION FORMULA                  ║
╠════════════════════════════════════════════════════════╣
║                                                        ║
║ INPUTS:                                                ║
║  DAU = Daily Active Users                             ║
║  D = Data per user per day (in bytes/KB/MB)           ║
║  R = Retention period (in days)                       ║
║  X = Redundancy factor (2X for replication, 3X for   ║
║      distributed systems)                             ║
║  C = Compression ratio (1.0 = no compression,         ║
║      2.0 = 50% reduction, 3.0 = 66% reduction)       ║
║                                                        ║
║ STEP 1: Calculate Daily Data Generation               ║
║  Daily data = DAU × D                                 ║
║                                                        ║
║ STEP 2: Apply Retention Period                        ║
║  Total data = Daily data × R (days)                   ║
║                                                        ║
║ STEP 3: Apply Redundancy/Replication                  ║
║  With redundancy = Total data × X                     ║
║                                                        ║
║ STEP 4: Apply Compression (Optional)                  ║
║  Final storage = With redundancy ÷ C                  ║
║                                                        ║
║ STEP 5: Convert to Human-Readable Units               ║
║  If result > 10^9: Convert to GB                      ║
║  If result > 10^12: Convert to TB                     ║
║  If result > 10^15: Convert to PB                     ║
║                                                        ║
╚════════════════════════════════════════════════════════╝
```

### Real Examples

**Example 1: Twitter (Text + Media)**

```
Given:
  DAU = 300 million
  Data per user per day:
    ├─ 2 tweets × 500 bytes = 1 KB
    ├─ Timeline viewing metadata = 0.5 KB
    ├─ 10% of users post images (2 MB each) = 60 MB
    └─ Total per user ≈ 60 MB / (1/10) = 6 MB average
  Retention = 5 years (1,825 days)
  Redundancy = 2X (master-slave replication)
  Compression = 1.5X (gzip for text)

Calculation:
  Step 1: Daily = 300M × 10 MB = 3 PB (petabytes)
  Step 2: Retention = 1.8 EB × 1,825 = 3.285 ZB
  Step 3: Redundancy = 3.285 ZB × 2 = 6.57 ZB
  Step 4: Compression = 6.57 ZB ÷ 1.5 = 4.38 ZB
  
  Wow! That's huge. Real Twitter uses:
  └─ Tiered storage (hot/warm/cold)
  └─ Data partitioning by time
  └─ Compression ratios of 3-5X
  └─ Actual stored: ~100-500 PB
```

**Example 2: Instagram (Photos Only)**

```
Given:
  DAU = 500 million
  Data per user per day:
    ├─ 3 photos posted × 2 MB = 6 MB
    ├─ Metadata + thumbnails = 1 MB
    └─ Total = 7 MB per user/day
  Retention = 10 years (3,650 days)
  Redundancy = 3X (distributed across 3 data centers)
  Compression = 2.5X (JPEG + advanced compression)

Calculation:
  Step 1: Daily = 500M × 7 MB = 3.5 EB
  Step 2: Retention = 3.5 EB × 3,650 = 12.8 ZB
  Step 3: Redundancy = 12.8 ZB × 3 = 38.4 ZB
  Step 4: Compression = 38.4 ZB ÷ 2.5 = 15.36 ZB
  
  Reality check:
  └─ Instagram stores ~1-2 EB of photos
  └─ Uses heavy compression + CDN caching
  └─ Most data is in CDN, not primary storage
```

**Example 3: Message Queue (Logs)**

```
Given:
  DAU = 100 million
  Data per user per day:
    └─ 50 messages × 500 bytes = 25 KB
  Retention = 30 days (compliance requirement)
  Redundancy = 2X (Kafka replication factor)
  Compression = 3.0X (snappy compression)

Calculation:
  Step 1: Daily = 100M × 25 KB = 2.5 PB
  Step 2: Retention = 2.5 PB × 30 = 75 PB
  Step 3: Redundancy = 75 PB × 2 = 150 PB
  Step 4: Compression = 150 PB ÷ 3.0 = 50 PB
  
  Storage cost = 50 PB ÷ 1,000 = 50,000 TB
  Annual cost @ $0.02/GB/month = $12M
```

**Example 4: Database (Structured Data)**

```
Given:
  DAU = 10 million
  Data per user per day:
    ├─ User profile (1 MB baseline) = 1 MB
    ├─ Activity log (100 events × 1 KB) = 100 KB
    ├─ Transactions (10 × 500 bytes) = 5 KB
    └─ Total = ~1.1 MB per user/day
  Retention = 7 years (2,555 days)
  Redundancy = 2X (master + read replica)
  Compression = 2.0X (columnar storage compression)

Calculation:
  Step 1: Daily = 10M × 1.1 MB = 11 TB
  Step 2: Retention = 11 TB × 2,555 = 28 PB
  Step 3: Redundancy = 28 PB × 2 = 56 PB
  Step 4: Compression = 56 PB ÷ 2.0 = 28 PB
  
  With archival strategy:
  └─ Hot (1 year): 3 PB (expensive SSD)
  └─ Warm (6 years): 25 PB (cheap HDD)
  └─ Total: Cost-optimized at $5M/year
```

### Applications by System Type

| System | DAU | Data/User/Day | Retention | Redundancy | Typical Storage |
|--------|-----|---------------|-----------|------------|-----------------|
| Twitter | 300M | 6 MB | 5 yrs | 2X | 50-100 PB |
| Instagram | 500M | 7 MB | 10 yrs | 3X | 1-2 EB |
| Facebook | 3B | 10 MB | Forever | 3X | 10+ EB |
| Stripe DB | 100K | 100 MB | 10 yrs | 2X | 100 TB |
| Uber Logs | 100M | 1 MB | 30 days | 2X | 50 PB |
| YouTube | 500M | 50 MB | Forever | 2X | 5+ EB |

### Key Insights

**1. Data Per User Varies Hugely**
```
Text-based (tweets):     ~100 KB - 1 MB per day
Photo (Instagram):       ~5-10 MB per day
Video (YouTube):         ~100-500 MB per day
Database records:        ~1-100 MB per day
Logs/events:             ~100 KB - 1 MB per day

Key: Always break down data by component!
```

**2. Retention Period is Critical**
```
Same system, different retention:

30-day retention:  ~5 PB
1-year retention:  ~60 PB
5-year retention:  ~300 PB
Forever retention: ~Infinite PB (archive)

Solution: Implement tiered storage
├─ Hot storage (SSD, expensive): 1 month
├─ Warm storage (HDD, cheap): 1-5 years
└─ Cold storage (Archive, very cheap): 5+ years
```

**3. Redundancy & Replication**
```
No redundancy (1X): Single copy (risky!)
Master-Slave (2X): 1 primary + 1 backup
Multi-region (3X): 3 copies across DCs
Erasure coding (1.5X): Mathematical redundancy

Cost-benefit:
├─ 1X: Cheapest but loses data if failure
├─ 2X: Standard for most systems
└─ 3X: Critical systems (financial, health)
```

**4. Compression Ratios**
```
Text (gzip):         1.5-2.5X compression
JSON (snappy):       1.2-1.5X compression
Images (JPEG):       Already compressed! 1.0-1.2X
Video (H.264/H.265): Already compressed! 1.0-1.1X
Columnar (Parquet):  5-10X compression
Database (custom):   2-3X typical

Pro tip: Don't double-count! If data is already
compressed (JPEG, MP4), don't apply additional
compression factor.
```

### How to Choose Parameters

**Data Per User (D):**
```
LIGHT (Text app):
  └─ 100 KB - 1 MB per user per day

MEDIUM (Social media):
  └─ 1-10 MB per user per day

HEAVY (Photo/Video):
  └─ 10-500 MB per user per day

VERY HEAVY (Backup service):
  └─ 100 MB - 10 GB per user per day
```

**Retention Period (R):**
```
Logs/temp data:        7-30 days
Social media posts:    1-10 years
User profile:          Forever
Financial records:     7-10 years (legal)
Video/streaming:       2-5 years (then archive)
Messages:              30 days - 1 year
```

**Redundancy Factor (X):**
```
Non-critical:          1.0X (no backup)
Standard:              2.0X (master + 1 replica)
High-availability:     2.5-3.0X (multiple regions)
Financial/medical:     3.0X minimum
```

### Quick Estimation Checklist

```
✓ What data needs to be stored? (Be specific!)
✓ How much per user per day? (Estimate D)
✓ For how long? (Retention period R)
✓ How many copies? (Redundancy X)
✓ Is data already compressed?
✓ Can we archive old data?
✓ What's the cost per GB in this region?
✓ Do we need multi-region?
```

### When to Use This Formula

```
✓ Interview: Estimate storage for any system
✓ Capacity planning: Know how much disk to buy
✓ Cost forecasting: Calculate storage budget
✓ Architecture: Decide on tiered storage
✓ Scaling: When to add new storage nodes

✗ Don't use if:
  - Data is compressed (video, image) - don't compress again
  - You have real metrics (use actual data!)
  - System uses deduplication (reduces storage 2-10X)
```

---

## Data Generation from QPS Formula (Deriving Storage from Requests!)

**Official Name:** Write-Based Data Derivation Formula

**The Insight:** You don't need to guess "data per user per day" separately! You can DERIVE it from QPS by calculating how much data each WRITE operation creates. This connects the QPS formula directly to the Storage formula.

### The Formula

```
╔════════════════════════════════════════════════════════╗
║ DATA GENERATION FROM QPS FORMULA                       ║
╠════════════════════════════════════════════════════════╣
║                                                        ║
║ KEY INSIGHT:                                           ║
║  • Reads (90%) create NO storage                       ║
║  • Writes (10%) CREATE storage                         ║
║  • Storage = Write_QPS × Data_per_write × Seconds     ║
║                                                        ║
║ INPUTS:                                                ║
║  Peak_QPS = Peak queries per second (from QPS formula)║
║  Read_write_ratio = Read:Write ratio (e.g., 10:1)    ║
║  Avg_data_per_write = Bytes created per write         ║
║  DAU = Daily Active Users (for converting to /user)   ║
║                                                        ║
║ STEP 1: Split QPS into Read and Write                 ║
║  Read_ratio = Ratio ÷ (Ratio + 1)                     ║
║  Write_ratio = 1 ÷ (Ratio + 1)                        ║
║  Write_QPS = Peak_QPS × Write_ratio                   ║
║                                                        ║
║ STEP 2: Calculate Data Per Second (at peak)           ║
║  Data_per_second = Write_QPS × Avg_data_per_write    ║
║                                                        ║
║ STEP 3: Convert to Daily Data Per User                ║
║  Daily_data_per_user = (Data_per_second × 100K)    ║
║                        ÷ DAU                           ║
║                                                        ║
║ STEP 4: Feed into Storage Formula                     ║
║  (Now you have data_per_user to use in Storage!)      ║
║                                                        ║
╚════════════════════════════════════════════════════════╝
```

### Real Example: Twitter

```
Given:
  Peak_QPS = 277,776 (from QPS formula)
  Read:Write ratio = 10:1 (90% reads, 10% writes)
  Data per write operation:
    • Tweet write (10% of writes) = 500 bytes
    • Like write (50% of writes) = 100 bytes
    • Retweet write (20% of writes) = 100 bytes
    • Other write (20% of writes) = 50 bytes
    Weighted avg = 0.1×500 + 0.5×100 + 0.2×100 + 0.2×50
                 = 50 + 50 + 20 + 10 = 130 bytes/write
  DAU = 300M

Calculation:

  Step 1: Split QPS
    Write_ratio = 1 ÷ (10 + 1) = 0.0909
    Write_QPS = 277,776 × 0.0909 = 25,252 QPS (writes only!)
    Read_QPS = 277,776 - 25,252 = 252,524 QPS (reads only)

  Step 2: Data per second at peak
    Data/sec = 21,818 writes/sec × 130 bytes
             = 3,282,760 bytes/sec
             = 2.84 MB/sec

  Step 3: Daily data per user
    Daily total = 2.84 MB/sec × 100K sec
               = 283 TB/day
    Per user = 283 TB ÷ 300M users
             = 0.94 MB/user/day

  Step 4: Feed into Storage Formula
    (This DERIVED value = 0.94 MB/user/day)
    (Compare to our ASSUMED value = 6 MB/user/day)
    
    Insight: Only 0.94 MB is actual data writes!
    The 6 MB includes overhead, metadata, indexes, etc.

Result:
  This shows writes create LESS data than we assumed!
  Only 16% of the 6 MB is from actual write operations.
  The other 84% is:
    - Metadata & indexes
    - Replication copies
    - Cache duplicates
    - Timeline materialization
    - Search indexes
```

### Why This Matters

```
TRADITIONAL APPROACH (Independent):
  ✓ Simple: Just guess "data per user"
  ✗ Disconnected: QPS and Storage are separate
  ✗ Easy to misalign: Over/underestimate data

NEW APPROACH (Derived):
  ✓ Connected: Storage comes from actual writes
  ✓ Accurate: Based on system behavior
  ✓ Validates assumptions: Can compare derived vs assumed
  ✗ More complex: Need read:write ratio and per-operation size
```

### Comparison Table: Derived vs Assumed Data

```
System    │ Derived/Write │ Assumed/User │ Ratio │ Insight
──────────┼───────────────┼──────────────┼───────┼─────────────────
Twitter   │ 0.94 MB       │ 6 MB         │ 6.4X  │ Most data is metadata
Instagram │ 2 MB          │ 7 MB         │ 3.5X  │ Images are heavy
Uber      │ 50 KB         │ 1 MB         │ 20X   │ Simple location data
Stripe    │ 500 B         │ 100 KB       │ 200X  │ DB records >> raw writes
```

### When to Use Each Approach

```
USE DERIVED (from QPS):
  ✓ You know the read:write ratio
  ✓ You know data per operation type
  ✓ You need to validate assumptions
  ✓ Building a system from requirements

USE ASSUMED (direct estimate):
  ✓ You have real metrics from similar systems
  ✓ You're building from scratch (prototype)
  ✓ You need quick estimates
  ✓ Read-heavy systems (most data doesn't get written)

USE BOTH:
  ✓ Calculate derived value
  ✓ Compare with assumed value
  ✓ If they differ by 5-10X, investigate why!
  ✓ Use the higher value for safe capacity planning
```

### Complete Flow: QPS → Data → Storage

```
INPUT: Requests per user per day = 20

STEP 1: QPS FORMULA
  (20 requests/user × 300M users) ÷ 100K
  = 60,000 QPS average
  = 240,000 QPS peak (× 4)

STEP 2: DATA DERIVATION (NEW!)
  Peak_QPS = 277,776
  Read:Write = 10:1 → Write_QPS = 25,252
  Data/write = 130 bytes
  Daily_data = 25,252 × 130 × 100K ÷ 300M
             = 0.94 MB/user/day

STEP 3: STORAGE FORMULA
  Input the derived data_per_user = 0.94 MB
  Retention = 5 years
  Redundancy = 2X
  Compression = 1.5X
  Result: 7,300 PB total storage

✓ Everything connected through one input (requests/user)!
```

---

## 🎯 DETAILED EXPLANATIONS: Bandwidth, Database & Caching Using QPS

**Using Twitter Numbers (60K avg QPS, 240K peak QPS)**

### 1. BANDWIDTH FORMULA - Network Capacity Needed

**Simple Formula:**
```
Bandwidth (Gbps) = (Peak_QPS × Response_size × 8 bits) ÷ 10^9 × Redundancy
```

**Twitter Example (Peak 240K QPS):**

Given: Peak QPS = 240,000, Response size = 2 KB

```
Step 1: Bytes per second
  240,000 QPS × 2,000 bytes = 480,000,000 bytes/sec = 480 MB/sec

Step 2: Convert to Gbps
  480 MB/sec × 8 bits/byte ÷ 10^9 = 3.84 Gbps

Step 3: Add 10X redundancy (disaster recovery, spikes, overhead)
  3.84 Gbps × 10X = 38.4 Gbps required capacity

Cost: ~$2M/year for 38.4 Gbps global network
```

**Key Insight:** Response size matters as much as QPS!
```
Same 240K peak QPS, different responses:
├─ 1 KB response: 19.2 Gbps
├─ 2 KB response: 38.4 Gbps ← Twitter
└─ 10 KB response: 192 Gbps (why compression matters!)
```

---

### 2. DATABASE CAPACITY FORMULA - Total Storage Size

**Simple Formula:**
```
DB_Size = Write_QPS × Record_size × Retention_days × Redundancy × Index_overhead
```

**Twitter Example (Peak 240K QPS):**

Given: Write ratio = 1/11 (10% writes), Record size = 500B, Retention = 5 years

```
Step 1: Calculate Write QPS
  240,000 QPS × (1 ÷ 11) = 21,818 writes/second

Step 2: Daily data creation
  21,818 writes/sec × 86,400 sec = 1.88 billion records/day
  1.88B records × 500 bytes = 0.94 TB/day

Step 3: Apply 5-year retention
  0.94 TB/day × 365 days × 5 years = 1.7 PB

Step 4: Add index overhead (1.5X)
  1.7 PB × 1.5 = 2.55 PB

Step 5: Add replication (2X)
  2.55 PB × 2 = 5.1 PB (main database with replica)

Cost: ~$150M/year for 5.1 PB storage + replication
```

**Key Insight:** Only WRITES create DB records!
```
Reads DON'T increase DB size:
├─ 240,000 QPS with 10:1 read:write ratio
├─ Still same DB size (only 21,818 writes/sec matter)
├─ Only bandwidth increases for reads
└─ DB growth = Write QPS × Time, not Read QPS × Time
```

---

### 3. CACHING LAYER FORMULA - Reduce Database Load

**Simple Formula:**
```
Cache_size = Working_set_size × Redundancy
Working_set = Total_DB × Working_set_ratio (typically 20%)
```

**Twitter Example (Peak 240K QPS):**

Given: Cache hit rate = 80%, DB size = 5.1 PB, Working set = 20%

```
Step 1: Calculate database hits (misses)
  Cache hit rate = 80% (answered from memory)
  Cache miss rate = 20% (needs database)
  DB_QPS = 240,000 × 0.20 = 48,000 QPS (instead of 240K!)

Step 2: Calculate hot data (working set)
  Hot_data = 5.1 PB × 0.20 = 1.02 PB
  (20% of tweets are hot: celebrity tweets, trending topics)

Step 3: Cache size with redundancy (2X for HA)
  Cache_size = 1.02 PB × 2 = 2.04 PB

Hardware needed:
  512GB RAM per server × 4,000 servers = 2.04 PB

Cost breakdown:
├─ Cache hardware: ~$3M/year
└─ DB savings (5X less load): saves $40M/year in DB costs
```

**Key Insight:** Caching is the most cost-effective optimization!
```
Without cache:
├─ DB load: 240,000 QPS → needs 480 servers
└─ Cost: $50M/year

With 80% cache hit:
├─ DB load: 48,000 QPS → needs 96 servers
├─ Cache cost: $3M/year
└─ Net savings: $47M/year (!!)
```

---

### Complete Flow: How They Connect

```
User Request
    ↓
[BANDWIDTH] ← How fast can we receive the request?
    ↓ (240,000 QPS peak)
    ├─→ Cache: "Do I have this data?" (80% yes)
    │   ├─ YES → Answer from RAM (1ms, FAST)
    │   └─ NO → Query database (10ms, SLOW)
    │       ↓
    │   [DATABASE CAPACITY] ← Storage for 5.1 PB
    │       ↓ (48,000 QPS hit DB)
    │
    └─→ Response (2 KB average)
        ↓
    [BANDWIDTH] ← How fast can we send the response?
        ↓
    Back to user (38.4 Gbps capacity)

Infrastructure cost: ~$455M/year total
├─ Servers (for QPS): $50M
├─ Bandwidth (network): $2M
├─ Database (storage): $150M
├─ Cache (RAM): $3M
└─ Other (monitoring, etc): $250M
```

---

### Quick Reference: Twitter Numbers

```
Input: 300M DAU, 20 requests/user/day (100K secs/day baseline)
       ↓
       QPS Formula: 60K avg, 240K peak
       ├─ Bandwidth: 38.4 Gbps (for 240K peak, 2KB responses)
       ├─ Database: 5.1 PB (for 21.8K writes/sec, 5-year retention)
       └─ Cache: 2.04 PB (for 80% hit rate, 20% hot data)

All three depend on QPS formula as the foundation!
```

---

## Bandwidth Calculation Formula

**Official Name:** Network Bandwidth Estimation Formula

Calculates the network bandwidth needed to handle peak traffic. Essential for CDN, load balancer, and network planning.

### The Formula

```
╔════════════════════════════════════════════════════════╗
║ BANDWIDTH CALCULATION FORMULA                          ║
╠════════════════════════════════════════════════════════╣
║                                                        ║
║ INPUTS:                                                ║
║  Peak_QPS = Peak queries per second (from QPS formula)║
║  Avg_response_size = Average response size (bytes)    ║
║  P = Peak multiplier (2-5X)                           ║
║  B = Bandwidth multiplier (1X for load, 10X for      ║
║      redundancy + overhead)                            ║
║                                                        ║
║ STEP 1: Calculate Bytes Per Second (at peak)          ║
║  Bytes_per_sec = Peak_QPS × Avg_response_size        ║
║                                                        ║
║ STEP 2: Convert to Gigabits Per Second (Gbps)         ║
║  Gbps = (Bytes_per_sec × 8 bits/byte) ÷ 10^9        ║
║                                                        ║
║ STEP 3: Apply Redundancy & Safety Margin              ║
║  Required_Gbps = Gbps × B (typically 10X)             ║
║                                                        ║
║ STEP 4: Convert to Terabits (if > 1000 Gbps)         ║
║  Tbps = Required_Gbps ÷ 1,000                         ║
║                                                        ║
║ QUICK CONVERSION:                                      ║
║  1 Byte = 8 bits                                      ║
║  1 Mbps = 0.125 MB/s                                  ║
║  1 Gbps = 125 MB/s                                    ║
║  1 Tbps = 125 GB/s                                    ║
║                                                        ║
╚════════════════════════════════════════════════════════╝
```

### Real Examples

**Example 1: Twitter (Same Assumptions)**

```
Given:
  Peak QPS = 277,776 (from QPS formula)
  Average response size = 2 KB per request
  Redundancy multiplier = 10X (for safety + DR)

Calculation:
  Step 1: Bytes/sec = 240,000 QPS × 2 KB
                    = 555,552 KB/sec
                    = 555.552 MB/sec

  Step 2: Gbps = (555.552 MB/sec × 8 bits) ÷ 1,000
                = 4,444 Mbps
                = 4.4 Gbps

  Step 3: With redundancy = 4.4 Gbps × 10
                          = 38.4 Gbps (peak capacity needed)

  Infrastructure:
    Primary network: 38.4 Gbps (10GbE cards × 5 servers)
    Backup network: 38.4 Gbps (redundant path)
    CDN capacity: 100 Gbps (handle spikes)
    Cost: ~$1-2 million/year for this capacity
```

**Example 2: Video Streaming (Netflix-like)**

```
Given:
  Peak QPS = 500,000 users watching simultaneously
  Video bitrate = 5 MB/sec per user (1080p)
  This isn't QPS, it's concurrent streams!

Calculation:
  Step 1: Bytes/sec = 500K users × 5 MB/sec
                    = 2.5 Million MB/sec
                    = 2.5 EB/sec (yes, exabytes!)

  Step 2: Gbps = (2.5M MB/sec × 8) ÷ 1,000
                = 20,000 Gbps
                = 20 Tbps

  Step 3: With redundancy = 20 Tbps × 3 (3 regions)
                          = 60 Tbps total

  Reality: Netflix uses ~200 Tbps globally (peak)
  This shows why they need massive CDN network!
```

### Bandwidth by Operation Type

| Operation | Response Size | Peak QPS | Bandwidth |
|-----------|---------------|----------|-----------|
| Read tweet | 2 KB | 250K | 4 Gbps |
| Upload image | 500 B (ACK) | 50K | 0.2 Gbps |
| Stream video | 1 MB/sec per user | 500K concurrent | 4 Tbps |
| API call | 500 B | 100K | 0.4 Gbps |
| Database sync | 10 KB | 50K | 4 Gbps |

---

## Database Capacity Formula

**Official Name:** Database Capacity Estimation Formula

Calculates total database size including indexes, replication, and overhead.

### The Formula

```
╔════════════════════════════════════════════════════════╗
║ DATABASE CAPACITY FORMULA                              ║
╠════════════════════════════════════════════════════════╣
║                                                        ║
║ INPUTS:                                                ║
║  DAU = Daily Active Users                             ║
║  Records_per_user = Records created per user          ║
║  Avg_record_size = Size of each record (bytes)        ║
║  Retention = How long to keep data (days)             ║
║  Index_overhead = Index size (typically 1.5-2X)       ║
║  Redundancy = Replication factor (2X or 3X)           ║
║                                                        ║
║ STEP 1: Calculate Total Records                       ║
║  Total_records = DAU × Records_per_user × Retention   ║
║                                                        ║
║ STEP 2: Calculate Raw Data Size                       ║
║  Raw_size = Total_records × Avg_record_size           ║
║                                                        ║
║ STEP 3: Add Index Overhead                            ║
║  With_indexes = Raw_size × Index_overhead             ║
║                                                        ║
║ STEP 4: Apply Replication                             ║
║  Final_DB_size = With_indexes × Redundancy            ║
║                                                        ║
╚════════════════════════════════════════════════════════╝
```

### Real Examples

**Example 1: Twitter (Same Assumptions)**

```
Given:
  DAU = 300 million
  Tweets per user per day = 1 tweet
  Retention = 5 years (1,825 days)
  Avg tweet size = 500 bytes
  Index overhead = 1.5X (author, timestamp, hashtags)
  Redundancy = 2X (master-slave)

Calculation:
  Step 1: Total records = 300M × 1 × 1,825
                        = 547.5 billion tweets

  Step 2: Raw size = 547.5B tweets × 500 bytes
                   = 273.75 TB

  Step 3: With indexes = 273.75 TB × 1.5
                       = 410.6 TB

  Step 4: With redundancy = 410.6 TB × 2
                          = 821.2 TB (main DB)

  Additional tables:
    User profiles: ~100 TB (with redundancy)
    Interactions (likes, retweets): ~200 TB
    Metadata & indexes: ~100 TB
    ─────────────────────────────
    Total DB: ~1.2 PB

  With 3-way replication: 3.6 PB total
```

**Example 2: E-commerce (Purchase History)**

```
Given:
  DAU = 50 million
  Purchases per user per day = 0.5 (orders)
  Retention = 10 years (3,650 days)
  Avg order size = 1 KB (with items, shipping, etc)
  Index overhead = 2.0X (user_id, date, product_id)
  Redundancy = 2X

Calculation:
  Step 1: Total records = 50M × 0.5 × 3,650
                        = 91.25 billion orders

  Step 2: Raw size = 91.25B × 1 KB
                   = 91.25 TB

  Step 3: With indexes = 91.25 TB × 2.0
                       = 182.5 TB

  Step 4: With redundancy = 182.5 TB × 2
                          = 365 TB

  This fits on modern databases! (much smaller than Twitter)
```

### Database by System Type

| System | Total Records | Raw Size | With Indexes | With 2X Replication |
|--------|---------------|----------|--------------|-------------------|
| Twitter (tweets) | 547B | 273 TB | 410 TB | 821 TB |
| Instagram (photos) | 150B | 300 TB | 450 TB | 900 TB |
| Uber (rides) | 100B | 100 TB | 150 TB | 300 TB |
| Facebook (posts) | 500B | 500 TB | 750 TB | 1.5 PB |

---

## Database Formula Explained: QPS → Database Size

**The Complete Flow: How QPS Drives Database Capacity**

The database formula depends directly on the QPS formula. Understanding this dependency is critical for interviews.

### The Complete Dependency Chain

```
═══════════════════════════════════════════════════════════════════════════════
           DATABASE FORMULA: QPS → Write QPS → Database Size
═══════════════════════════════════════════════════════════════════════════════

STARTING POINT: QPS Formula
┌─────────────────────────────────────────────────────────────────────────────┐
│ QPS = (DAU × Req/Day) ÷ 100K × Peak_mult                                    │
│                                                                             │
│ Twitter: (300M × 20) ÷ 100K × 4 = 240,000 QPS peak                        │
└─────────────────────────────────────────────────────────────────────────────┘

                              ↓ STEP 1

CALCULATE WRITE QPS (Only writes create DB records!)
┌─────────────────────────────────────────────────────────────────────────────┐
│ Write_QPS = Peak_QPS ÷ Read:Write_Ratio                                     │
│                                                                             │
│ Twitter (using 9:1 for simpler mental math):                               │
│   Read:Write ratio = 9:1 (90% reads, 10% writes)                           │
│   Write_QPS = 240,000 ÷ 10 = 24,000 writes/second                          │
│                                                                             │
│ Why divide by 10? Because 9 reads + 1 write = 10 total parts              │
│   Writes = 1/10 of total = 240,000 ÷ 10                                   │
└─────────────────────────────────────────────────────────────────────────────┘

                              ↓ STEP 2

CONVERT TO DAILY WRITES
┌─────────────────────────────────────────────────────────────────────────────┐
│ Records_Per_Day = Write_QPS × Seconds_Per_Day                               │
│                                                                             │
│ Twitter:                                                                    │
│   Records_Per_Day = 24,000 writes/sec × 86,400 sec/day                     │
│                   = 2,073,600,000 records/day (2.07 billion!)              │
│                                                                             │
│ What is this? 2.07B new tweets per day                                     │
└─────────────────────────────────────────────────────────────────────────────┘

                              ↓ STEP 3

CALCULATE DAILY DATA VOLUME
┌─────────────────────────────────────────────────────────────────────────────┐
│ Daily_Data = Records_Per_Day × Record_Size                                  │
│                                                                             │
│ Twitter:                                                                    │
│   Record_Size = 500 bytes (tweet metadata: ID, text, timestamp, etc)       │
│   Daily_Data = 2.07B × 500 bytes                                           │
│              = 1,036.8 billion bytes                                       │
│              = 1,036.8 GB ≈ 1 TB per day                                   │
│                                                                             │
│ Note: This is TINY (only metadata, not the 3MB photos)                     │
└─────────────────────────────────────────────────────────────────────────────┘

                              ↓ STEP 4

APPLY RETENTION PERIOD
┌─────────────────────────────────────────────────────────────────────────────┐
│ Total_With_Retention = Daily_Data × 365 × Retention_Years                   │
│                                                                             │
│ Twitter:                                                                    │
│   Retention = 5 years = 1,825 days                                         │
│   Total = 1 TB/day × 365 days × 5 years                                    │
│         = 1 × 1,825 TB                                                     │
│         = 1.825 PB ≈ 1.8 PB                                                │
│                                                                             │
│ This is the raw data for 5 years of writes                                 │
└─────────────────────────────────────────────────────────────────────────────┘

                              ↓ STEP 5

ADD INDEX OVERHEAD (1.5x)
┌─────────────────────────────────────────────────────────────────────────────┐
│ With_Indexes = Total_With_Retention × Index_Multiplier                      │
│                                                                             │
│ Twitter:                                                                    │
│   Index_Multiplier = 1.5 (indexes are 50% of data size)                    │
│   With_Indexes = 1.8 PB × 1.5                                              │
│                = 2.7 PB                                                    │
│                                                                             │
│ Why indexes? Enable fast queries:                                          │
│   • WHERE user_id = 123                                                    │
│   • WHERE timestamp > NOW() - 1 day                                        │
│   • WHERE text LIKE '%AI%'                                                 │
└─────────────────────────────────────────────────────────────────────────────┘

                              ↓ STEP 6

ADD REPLICATION (2x for HA)
┌─────────────────────────────────────────────────────────────────────────────┐
│ Final_DB_Size = With_Indexes × Redundancy_Factor                            │
│                                                                             │
│ Twitter:                                                                    │
│   Redundancy = 2x (master-slave replication)                               │
│   Final = 2.7 PB × 2                                                       │
│         = 5.4 PB (FINAL ANSWER)                                            │
│                                                                             │
│ This is:                                                                    │
│   • 2.7 PB on Master (primary database)                                    │
│   • 2.7 PB on Slave (backup/read replica)                                 │
│   • Both in sync for failover                                              │
└─────────────────────────────────────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════════════════════
                          COMPLETE FORMULA
═══════════════════════════════════════════════════════════════════════════════

DB_Size = (Peak_QPS ÷ R:W_ratio) × 86,400 × Record_size × 365 × Retention × 
          Index_mult × Redundancy

Twitter (9:1 ratio for clean mental math):
  = (240K ÷ 10) × 86,400 × 500B × 365 × 5 × 1.5 × 2
  = 24,000 × 86,400 × 500B × 1,825 × 1.5 × 2
  = 5.4 PB ✓

═══════════════════════════════════════════════════════════════════════════════
                          KEY INSIGHTS
═══════════════════════════════════════════════════════════════════════════════

1. START WITH QPS
   Peak QPS (240K) is the foundation
   
2. EXTRACT WRITES ONLY
   240K QPS → divide by 10 → 24K writes/sec
   (Reads don't create records, so they don't matter for DB size!)
   
3. MULTIPLY BY TIME
   24K writes/sec × 86,400 sec × 1,825 days = massive volume
   
4. ADD OVERHEAD
   Indexes (1.5x) + Replication (2x) = 3x total multiplier
   Raw data × 3 = Final DB size
   
5. FINAL: 5.4 PB
   ├─ Master: 2.7 PB (live writes)
   └─ Replica: 2.7 PB (read copies + failover)

═══════════════════════════════════════════════════════════════════════════════
                    WHY DIFFERENT FROM STORAGE?
═══════════════════════════════════════════════════════════════════════════════

                Storage          Database
                ──────────────────────────────
Size            7.3 EB           5.4 PB
Reason          All user content  Write records only
Calculation     Daily data ×      Write QPS × 
                retention         record_size × retention
Includes        Photos, videos    Metadata only
What it stores  S3 objects        Indexed records

Storage is 1,350x larger because it includes all the media files!

═══════════════════════════════════════════════════════════════════════════════
```

### Step-by-Step Walkthrough

#### Step 1: Why Start With Peak QPS?

The database formula **depends on QPS** because:
- Database stores records created by **writes**
- Writes are a fraction of total QPS
- Without QPS, we don't know how many writes happen per second

#### Step 2: The Write Ratio (Why 9:1 for Interviews?)

```
Read:Write Ratio = How many reads per write

9:1 ratio means:
  • 9 reads for every 1 write
  • Total parts = 9 + 1 = 10
  • Write QPS = Peak ÷ 10 ← CLEAN MENTAL MATH!

Example (clean division):
  240,000 ÷ 10 = 24,000 (easy!)
  
vs 10:1 ratio (messier):
  240,000 ÷ 11 = 21,818 (harder mental math)
```

**In Interviews:** Use 9:1 for easier numbers unless the interviewer specifies otherwise.

#### Step 3: From Writes/Sec to Records/Day

```
Write_QPS = 24,000 per second
Seconds per day = 86,400

Records per day = 24,000 × 86,400 = 2,073,600,000

Easy way to remember:
  • ~25K writes/sec
  • ~2 billion records per day
  • ~60 billion records per month
```

#### Step 4: Data Volume Calculation

```
Each record (tweet metadata) ≈ 500 bytes

Daily volume = 2.07B records × 500 bytes = ~1 TB/day

Why only 500 bytes?
  ├─ Tweet ID: 8 bytes
  ├─ Author ID: 8 bytes
  ├─ Text (280 chars): 280 bytes
  ├─ Timestamp: 8 bytes
  ├─ Metadata (likes, retweets): 100 bytes
  ├─ Indexes pointers: 100 bytes
  └─ Total: ~500 bytes

Photos stored separately in S3 (different formula!)
```

#### Step 5: Retention Multiplier

```
Twitter retention = 5 years = 1,825 days

Raw database size = 1 TB/day × 1,825 days = 1.8 PB

This is just the data—no indexes or replication yet!
```

#### Step 6: Index Overhead (1.5x)

```
Databases need indexes for fast queries:

Type of index:       Overhead
─────────────────────────────
Single column (user_id)      20% of data
Composite (user_id + date)   30% of data
Full-text (tweets text)      50% of data
All indexes combined:        ~1.5x of data

With_indexes = 1.8 PB × 1.5 = 2.7 PB
```

#### Step 7: Replication (2x)

```
Master database:  2.7 PB
Replica database: 2.7 PB (backup, read-only)
─────────────────────────
Total:            5.4 PB

Why 2 copies?
  ├─ Failover: If master dies, replica takes over
  ├─ Read replicas: Distribute read load
  └─ Geo-redundancy: Keep copies in different data centers
```

### The Complete Table: QPS → Database

| Step | Input | Calculation | Result | Cumulative |
|------|-------|-------------|--------|-----------|
| Start | Peak QPS | 240,000 | 240,000 | 240,000 |
| 1. Write QPS | 9:1 ratio | ÷ 10 | 24,000 /sec | 24,000 |
| 2. Records/day | 86,400 sec | × 86,400 | 2.07B records | 2.07B |
| 3. Daily data | 500B/record | × 500 bytes | 1 TB/day | 1 TB |
| 4. Retention | 1,825 days | × 1,825 | 1.8 PB | 1.8 PB |
| 5. Indexes | 1.5x overhead | × 1.5 | 2.7 PB | 2.7 PB |
| 6. Replication | 2x for HA | × 2 | **5.4 PB** | **5.4 PB** |

### Interview Script

**Interviewer:** "How big is Twitter's database?"

**You:**
> "Let me work through this step by step.
> 
> **Start with QPS:** Peak is 240K requests per second.
> 
> **Extract writes:** Only writes create database records. Twitter's read:write is 9:1, so write QPS = 240K ÷ 10 = 24K writes per second.
> 
> **Convert to daily records:** 24K writes/sec × 86,400 sec = 2 billion records per day.
> 
> **Calculate daily data:** Each tweet record is ~500 bytes. 2B × 500B = 1 TB per day.
> 
> **Apply retention:** Twitter keeps data 5 years = 1,825 days. 1 TB × 1,825 = 1.8 PB raw data.
> 
> **Add indexes:** Databases need 1.5x for indexes to query fast. 1.8 PB × 1.5 = 2.7 PB.
> 
> **Add replication:** For high availability, replicate to backup servers. 2.7 PB × 2 = **5.4 PB total database.**
> 
> This breaks down as:
> - Master database: 2.7 PB (live writes)
> - Replica database: 2.7 PB (backup and read replicas)"

---

## Caching Layer Formula

**Official Name:** Caching Layer Capacity Formula

Calculates cache size needed to achieve target hit rate and reduce database load.

### The Formula

```
╔════════════════════════════════════════════════════════╗
║ CACHING LAYER FORMULA                                  ║
╠════════════════════════════════════════════════════════╣
║                                                        ║
║ INPUTS:                                                ║
║  Peak_QPS = Peak queries per second                   ║
║  Cache_hit_rate = Target hit rate (e.g., 80%)        ║
║  Avg_response_size = Size of cached item (bytes)      ║
║  Working_set_ratio = % of data accessed frequently    ║
║                      (typically 20-30%)               ║
║  Redundancy = Replication (2X for HA)                 ║
║                                                        ║
║ STEP 1: Calculate Cache Hits Per Second               ║
║  Cache_hits_qps = Peak_QPS × Cache_hit_rate           ║
║                                                        ║
║ STEP 2: Calculate Database Hits Per Second            ║
║  DB_hits_qps = Peak_QPS - Cache_hits_qps              ║
║                                                        ║
║ STEP 3: Calculate Working Set Size                    ║
║  (What data to keep in cache)                         ║
║  Working_set = Total_DB_size × Working_set_ratio      ║
║                                                        ║
║ STEP 4: Add Redundancy for Cache Cluster              ║
║  Final_cache_size = Working_set × Redundancy          ║
║                                                        ║
║ SAVINGS CALCULATION:                                   ║
║  DB_load_without_cache = Peak_QPS                     ║
║  DB_load_with_cache = DB_hits_qps                     ║
║  Load_reduction = 1 - (DB_hits_qps ÷ Peak_QPS)       ║
║                 = Cache_hit_rate                      ║
║                                                        ║
╚════════════════════════════════════════════════════════╝
```

### Real Examples

**Example 1: Twitter (Same Assumptions)**

```
Given:
  Peak QPS = 277,776
  Target cache hit rate = 80% (reasonable for social media)
  Avg response size = 2 KB per cached item
  Working set ratio = 20% (20% of tweets are "hot")
  DB size = 1.2 PB
  Redundancy = 2X (for high availability)

Calculation:
  Step 1: Cache hits = 277,776 × 0.80
                     = 222,220 QPS (served from cache)

  Step 2: DB hits = 277,776 - 222,220
                  = 55,556 QPS (hit actual database)

  Step 3: Working set = 1.2 PB × 0.20
                      = 240 PB (hot data)

  Step 4: Cache size = 240 PB × 2
                     = 480 PB (with redundancy)

  Infrastructure:
    Redis cluster: 500 servers × 1 TB each = 500 PB
    Memcached: 100 servers × 100 GB each = 10 TB
    Total cache layer: ~510 TB to 500 PB

  Impact:
    Without cache: 240K QPS hits DB
    With cache: 55K QPS hits DB
    DB load reduction: 80% ✓
    Cost savings: ~$100M/year (fewer DB servers)
```

**Example 2: E-commerce (Shopping)**

```
Given:
  Peak QPS = 100,000
  Target cache hit rate = 90% (product catalog is stable)
  Avg cached item = 10 KB (product details)
  Working set ratio = 10% (only 10% of catalog accessed)
  DB size = 500 TB (from Database formula)
  Redundancy = 2X

Calculation:
  Step 1: Cache hits = 100,000 × 0.90 = 90,000 QPS

  Step 2: DB hits = 100,000 - 90,000 = 10,000 QPS

  Step 3: Working set = 500 TB × 0.10 = 50 TB

  Step 4: Cache size = 50 TB × 2 = 100 TB

  Infrastructure:
    Redis: 5 servers × 20 TB each = 100 TB
    Cost: ~$5M (much cheaper than DB servers)
    
  Result: 90% of requests served from memory (fast!)
```

### Caching Strategy by Data Type

| Data Type | Hit Rate | Working Set | Cache Size | Example |
|-----------|----------|-------------|------------|---------|
| Product catalog | 90% | 10% | 50 TB | E-commerce |
| User timeline | 80% | 20% | 240 PB | Twitter |
| Trending topics | 95% | 1% | 12 PB | Trending feed |
| User sessions | 85% | 30% | 150 TB | Login/auth |
| Geographic data | 99% | 5% | 25 TB | Maps/location |

### Cache Hit Rate Targets

```
  System Type           │ Realistic Hit Rate │ DB Load Reduction
  ──────────────────────┼───────────────────┼──────────────────
  Static content        │ 95-99%            │ 95-99% savings
  User profiles         │ 80-90%            │ 80-90% savings
  Product catalog       │ 85-95%            │ 85-95% savings
  Timeline/feed         │ 70-80%            │ 70-80% savings
  Search results        │ 60-70%            │ 60-70% savings
  Real-time data        │ 40-50%            │ 40-50% savings
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
1 day = 100K seconds (≈ 100,000 seconds)
1 hour = 3,600 seconds
1 minute = 60 seconds

QPS for N days of data:
  N × 100K seconds per day
  
Example:
  1 billion records in 1 year
  = 1 billion ÷ (365 × 100K)
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


## ⚠️ Important Notice

**This document is based on concepts from "System Design Interview" by Alex Xu.**

This is **original content**: summaries, explanations, and real-world examples created to help understand system design concepts. It does not reproduce copyrighted material from the book. All content is restructured and rewritten for clarity and educational purposes.

---

# Chapter 3.5: The Universal System Design Framework

## ⭐ USE THIS FOR ANY SYSTEM DESIGN PROBLEM

This is a step-by-step framework you can apply to **ANY** system design interview question. Follow these steps in order, and you'll have a complete solution.

---

## ⚡ QUICK REFERENCE TABLE (Print & Use in Interviews!)

| Step | Focus | Time | Sub-Steps | Technical Approaches |
|------|-------|------|-----------|----------------------|
| **STEP 1** | Functional Requirements | 5 min | • What features must we build?<br>• Who are the users?<br>• How do users interact? | 🏛️ **Architecture Patterns:**<br>• Monolithic vs Microservices<br>• Event-driven architecture<br>• API gateway pattern<br>• Service-oriented architecture (SOA) |
| **STEP 2** | Scale & Non-Functional Req | 5 min | • DAU, read-write ratio, spike traffic<br>• Latency requirements<br>• Availability target (SLA)<br>• Consistency vs Availability | 📈 **Scaling & Data Strategies:**<br>• Replication: Master-Slave, Master-Master<br>• Sharding: Range, Hash, Geospatial<br>• Read-Write Separation<br>• Database type: SQL vs NoSQL<br>• Consistency model: Strong vs Eventual (CAP theorem) |
| **STEP 3** | Generic Blueprint | 3 min | • Load Balancer<br>• Services Layer<br>• Cache (Redis)<br>• Database (Master-Slave)<br>• Storage (S3, Kafka, Elasticsearch) | 🔧 **Core Components & Tech:**<br>• LB: Round-robin, Consistent hashing<br>• Cache: Redis, Memcached (2ms latency)<br>• DB: Master-Slave replication, Read replicas<br>• Storage: S3, Object store<br>• Queue: Kafka, RabbitMQ (async)<br>• Search: Elasticsearch, Algolia |
| **STEP 5** | Back-of-Envelope Math | 5 min | • Calculate QPS: (DAU × req/day) ÷ 100K<br>• Peak QPS: avg × 2.5<br>• Servers needed: Peak QPS ÷ 1K-10K<br>• Storage calculation<br>• 10X growth scenario | 📊 **Scaling Tactics:**<br>• Horizontal scaling: Add more servers<br>• Vertical scaling: Bigger servers<br>• Auto-scaling: Scale based on metrics<br>• Database partitioning/sharding<br>• Read replicas for scaling reads<br>• Write sharding for scaling writes |
| **STEP 4** | Customize for System | 10 min | • What to ADD (system-specific components)<br>• What to REMOVE (not needed)<br>• What to MODIFY (adjust for requirements) | ⚙️ **Specialized Components:**<br>• **ADD:** Search (Elasticsearch), Real-time (WebSocket, gRPC), Geospatial (PostGIS)<br>• **ADD:** Time-series DB (InfluxDB), Graph DB (Neo4j), Full-text search<br>• **ADD:** Service mesh (Istio), API gateway (Kong)<br>• **REMOVE:** Components not in MVP |
| **STEP 6** | Design Deep-Dives (NFRs) | 10 min | • Resilience: failure scenarios + recovery<br>• Monitoring: metrics, alerts, SLA<br>• Consistency trade-off: Strong vs Eventual | 🛡️ **Resilience & Operations:**<br>• **Resilience:** Circuit breaker, Bulkhead, Retry (exponential backoff), Failover<br>• **Monitoring:** Distributed tracing (Jaeger, DataDog), Metrics (Prometheus), Logs (ELK)<br>• **Consistency:** Transaction logs, Event sourcing, CQRS, Consensus (Raft) |
| **STEP 7** | Verify Growth & Constraints | 7 min | • ✓ Scale met?<br>• ✓ Latency met?<br>• ✓ Availability met?<br>• ✓ Compliance met? | ✅ **Validation & Deployment:**<br>• Load testing, Stress testing<br>• Chaos engineering (fail components)<br>• Canary deployment, Blue-green deployment<br>• Rollback strategy<br>• Performance benchmarking |

---

## 🎯 TECHNICAL APPROACHES: When to Use Each (Purpose-Based Guide)

### **ARCHITECTURE PATTERNS (STEP 1)**

| Approach | Purpose | When to Use | Trade-offs |
|----------|---------|------------|-----------|
| **Monolithic** | Single unified codebase | MVP, small teams (<10 people) | Hard to scale, tight coupling |
| **Microservices** | Independent services | 100M+ DAU, multiple teams, different tech stacks | Complex operations, distributed tracing needed |
| **Event-Driven** | Loose coupling via events | Real-time updates, async processing | Eventual consistency, harder to debug |
| **API Gateway** | Central entry point | Rate limiting, auth, versioning | Single point of failure (needs failover) |

---

### **SCALING & REPLICATION (STEP 2)**

| Approach | Purpose | When to Use | Trade-offs |
|----------|---------|------------|-----------|
| **Master-Slave** | Read scaling | Read-heavy (9:1 ratio) e.g., Instagram feed | Slave lag (eventual consistency) |
| **Master-Master** | Multi-region writes | Global apps, disaster recovery | Conflict resolution complexity |
| **Range Sharding** | Partition by ID range | Time-based data, continuous ranges | Uneven distribution (hot shards) |
| **Hash Sharding** | Distribute uniformly | Even distribution needed | Rebalancing cost when adding shards |
| **Geospatial (PostGIS)** | Location-based queries | Uber (nearby drivers), Maps, Yelp | Slower than numeric indexes |
| **SQL vs NoSQL** | Data model choice | SQL: structured + transactions<br>NoSQL: flexible schema + scale | SQL: harder to scale<br>NoSQL: eventual consistency |

---

### **CORE COMPONENTS (STEP 3)**

| Component | Purpose | When to Use | Trade-offs |
|-----------|---------|------------|-----------|
| **Round-Robin LB** | Simple distribution | Stateless services, equal load | Doesn't account for server capacity |
| **Consistent Hashing LB** | Smart routing | Caching, sessions, stateful | More complex implementation |
| **Redis** | In-memory cache | Hot data, sessions, leaderboards, real-time | Expensive (memory cost), data loss on crash |
| **Memcached** | Distributed cache | Cheaper caching, simple key-value | No persistence, no replication |
| **Master-Slave DB** | Read scaling | 90:10 read-write ratio | Slave lag |
| **Read Replicas** | Scale read load | 100K+ QPS reads | Data consistency delays |
| **Kafka** | High-throughput queue | Event streaming, log aggregation (1000s msgs/sec) | Complex to operate, overkill for small queue |
| **RabbitMQ** | Reliable queue | Task queues, guarantees delivery | Lower throughput than Kafka |
| **Elasticsearch** | Full-text search | Search features, analytics | Memory intensive, complexity |
| **Algolia** | Managed search | Need instant results, don't want to operate Elasticsearch | Cost ($$$) |

---

### **SPECIALIZED COMPONENTS (STEP 4)**

| Component | Purpose | When to Use | Trade-offs |
|-----------|---------|------------|-----------|
| **WebSocket** | Real-time updates | Chat, live notifications, live feed | Stateful connections, harder to scale |
| **gRPC** | Low-latency RPC | Microservices communication, mobile | Not HTTP, harder debugging |
| **PostGIS** | Geospatial queries | "Find drivers within 5km", location search | Slower than standard indexes |
| **InfluxDB** | Time-series data | Metrics, logs, analytics | Not suitable for relational data |
| **Neo4j** | Graph relationships | Social networks, recommendations, knowledge graphs | Overkill for non-graph data |
| **Service Mesh (Istio)** | Microservices management | 50+ services, need observability | Complex to deploy and debug |
| **Kong API Gateway** | API management | Multi-tenant, versioning, rate limiting | Adds latency (middleware) |

---

### **SCALING TACTICS (STEP 5)**

| Tactic | Purpose | When to Use | Trade-offs |
|--------|---------|------------|-----------|
| **Horizontal Scaling** | Add more servers | Most scenarios, cloud-native | Need stateless design |
| **Vertical Scaling** | Bigger server | Quick fix, limited by hardware | Max capacity reached faster |
| **Auto-Scaling** | Dynamic scaling | Traffic spikes, cost optimization | Lag time (takes minutes to scale) |
| **Database Sharding** | Scale writes | Write-heavy (100K+ QPS writes) | Complex queries across shards |
| **Read Replicas** | Scale reads | Read-heavy workloads | Replication lag |
| **Write Sharding** | Distribute writes | Multiple independent writes | Complex consistency |

---

### **RESILIENCE & OPERATIONS (STEP 6)**

| Pattern | Purpose | When to Use | Trade-offs |
|---------|---------|------------|-----------|
| **Circuit Breaker** | Prevent cascading failures | Calling external services, prevent thundering herd | Extra latency on circuit open |
| **Bulkhead** | Resource isolation | Prevent one service killing others | More memory usage |
| **Retry with Backoff** | Handle transient failures | Network timeouts, temporary failures | Slow recovery, retries can amplify load |
| **Failover** | Automatic backup | Master DB dies, need instant recovery | Complex to coordinate |
| **Jaeger (Distributed Tracing)** | Debug production issues | 100+ microservices, slow requests | Overhead, storage cost |
| **Prometheus + Grafana** | Monitor metrics | Alert on CPU, Memory, Latency | Need alerting rules expertise |
| **ELK (Elasticsearch-Logstash-Kibana)** | Centralized logging | Debug issues across services | Storage intensive (logs take space) |
| **Event Sourcing** | Event-based history | Need full audit trail, temporal queries | Complex to implement, eventual consistency |
| **CQRS** | Separate reads/writes | Very different read/write models | Eventually consistent |

---

### **DEPLOYMENT & VALIDATION (STEP 7)**

| Strategy | Purpose | When to Use | Trade-offs |
|----------|---------|------------|-----------|
| **Canary Deployment** | Risk mitigation | Roll out to 5% users first | Slow rollout (1-2 hours) |
| **Blue-Green** | Zero-downtime deploy | Need instant fallback | 2X infrastructure cost |
| **Rolling Update** | No downtime | Stateless services | Temporary mixed versions |
| **Load Testing** | Capacity planning | Verify system can handle peak | Requires production-like data |
| **Chaos Engineering** | Find failure modes | 99.99% availability needed | Can cause real outages if not careful |

---

## 📖 DETAILED GUIDE (Read below for deeper understanding & examples)

---

## **STEP 1: Clarify Functional Requirements (5 min)**

**Ask the interviewer these questions to understand WHAT to build:**

### **What features must we build?**
```
"What are the core features of [system]?"
"Should we support X, Y, Z features?"
"Are there features we should NOT build?"
"What's the MVP (minimum viable product)?"

Example - Twitter:
  ✓ Post tweets
  ✓ See feed (followers' tweets)
  ✓ Like/retweet
  ✗ NOT building: video, stories, live streaming
```

### **Who are the users?**
```
"Who uses this system?"
"Are there different types of users?"
"Any admin/moderation features?"

Example - Uber:
  ✓ Riders (book rides)
  ✓ Drivers (accept rides)
  ✓ Admin (monitoring, payments)
```

### **How do users interact?**
```
"What's the main user journey?"
"Peak times of usage?"
"Typical user behavior?"

Example - Instagram:
  ✓ Upload photo (async, can wait)
  ✓ See feed (real-time, urgent)
  ✓ Like (real-time feedback)
```

---

## **STEP 2: Clarify Non-Functional Requirements & Scale (5 min)**

**Ask these questions to understand HOW MUCH and HOW FAST:**

### **Scale Questions:**
```
Q: How many daily active users (DAU)?
   A: "100M DAU"

Q: What's the read-to-write ratio?
   A: "90:10 (mostly reads, some writes)"

Q: Expected spike traffic?
   A: "2-5X during peak hours"

Q: Growth expectation?
   A: "10X growth in next year"
```

### **Latency Questions:**
```
Q: What's the latency requirement?
   A: "API response < 200ms"
   A: "Page load < 3 seconds"

Q: Is real-time required?
   A: "Yes, live feed updates needed"
   A: "No, 5-minute delay acceptable"
```

### **Availability Questions:**
```
Q: What's the availability target (SLA)?
   A: "99.9% uptime (3 nines)"
   A: "99.99% uptime (4 nines)"

Q: What happens if system goes down?
   A: "Cache outdated data instead"
   A: "Return error message"
```

### **Consistency Questions:**
```
Q: Is data consistency critical?
   A: "Yes, show exact like count"
   A: "No, approximate count OK (counts can be stale)"
```

---

## **STEP 3: Generic Blueprint (High-Level Design) - With Technical Approaches**

**Use this standard blueprint with technologies as your starting point:**

```
┌────────────────────────────────────────────────────────────┐
│                    CLIENT LAYER                            │
│  iOS App  │  Android App  │  Web Browser                   │
│  (HTTP/HTTPS)                                              │
└──────────────────────┬─────────────────────────────────────┘
                       │
                       ↓
┌────────────────────────────────────────────────────────────┐
│                   EDGE LAYER                               │
│  CDN (CloudFront, Cloudflare)  →  Static assets            │
│  API Gateway (Kong, AWS API Gateway) → Rate limiting, Auth │
└──────────────────────┬─────────────────────────────────────┘
                       │
                       ↓
┌────────────────────────────────────────────────────────────┐
│            LOAD BALANCING LAYER                            │
│  Algorithm: Round-robin / Consistent Hashing              │
│  Tech: Nginx, HAProxy, AWS ELB                            │
│  Health checks every 10 sec                               │
└──────────────────────┬─────────────────────────────────────┘
                       │
         ┌─────────────┼─────────────┐
         ↓             ↓             ↓
    [Region 1]   [Region 2]   [Region 3]
         │             │             │
         ↓             ↓             ↓
┌─────────────────────────────────────────────────────────────┐
│         APPLICATION LAYER (Microservices)                  │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │ Auth Service │  │ Feed Service │  │Upload Service│    │
│  │ (JWT tokens) │  │(Timeline gen)│  │(Image proc) │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │Search Service│  │PaymentService│  │NotifService │    │
│  │(Elasticsearch)  │(Stripe)      │  │(WebSocket)  │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
│                                                             │
│  Pattern: Event-driven, API Gateway, Circuit breaker      │
└─────────────────────┬─────────────────────────────────────┘
                      │
      ┌───────────────┼───────────────┐
      ↓               ↓               ↓
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  CACHE       │ │  MONITORING  │ │  MESSAGE    │
│              │ │              │ │  QUEUE      │
│ Redis        │ │ Prometheus   │ │ Kafka       │
│ 2ms latency  │ │ + Grafana    │ │ RabbitMQ    │
│ 80%+ hit     │ │              │ │ Async jobs  │
│              │ │ Distributed  │ │             │
│ Memcached    │ │ Tracing:     │ │ Decouple    │
│ Hot data     │ │ Jaeger       │ │ services    │
└──────────────┘ └──────────────┘ └──────────────┘
      │
      ↓
┌─────────────────────────────────────────────────────────────┐
│                DATABASE LAYER                               │
│                                                             │
│  Read-Heavy Workload:           Write-Heavy Workload:      │
│  Master (1 node) → Read Replicas │ Sharding by user_id    │
│  MySQL                          │ or hash partition        │
│  PostgreSQL                     │ Multiple DB instances    │
│                                 │                          │
│  Replication Strategy:          │ Sharding Strategy:       │
│  Master-Slave (async)           │ Range-based              │
│  Master-Master (conflict mgmt)  │ Hash-based               │
│                                 │ Geospatial (PostGIS)    │
│                                 │                          │
│  Consistency: Strong or Eventual (based on requirements)  │
└─────────────────────┬───────────────────────────────────────┘
                      │
    ┌─────────────────┼─────────────────┐
    ↓                 ↓                 ↓
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ OBJECT       │ │ SEARCH       │ │ TIME-SERIES  │
│ STORAGE      │ │ INDEX        │ │ DATABASE     │
│              │ │              │ │              │
│ AWS S3       │ │ Elasticsearch│ │ InfluxDB     │
│ GCS          │ │ Algolia      │ │ TimescaleDB  │
│ Azure Blob   │ │              │ │              │
│              │ │ Full-text    │ │ Metrics,     │
│ Photos,      │ │ search       │ │ Logs, Events │
│ Videos,      │ │              │ │              │
│ Documents    │ │ Geospatial   │ │ Analytics    │
│              │ │ queries      │ │              │
└──────────────┘ └──────────────┘ └──────────────┘

┌─────────────────────────────────────────────────────────────┐
│         SUPPORTING INFRASTRUCTURE                           │
│                                                             │
│  Deployment:                  Operations:                  │
│  • Canary deployment          • Health checks              │
│  • Blue-green deployment      • Auto-scaling               │
│  • Rolling updates            • Circuit breaker            │
│  • Kubernetes orchestration   • Bulkhead pattern           │
│  • Docker containers          • Retry with backoff         │
│                               • Service mesh (Istio)       │
└─────────────────────────────────────────────────────────────┘
```

**Technical Approaches by Layer:**
```
✓ CLIENT → EDGE: CDN (CloudFront), API Gateway (Kong)
✓ EDGE → LB: Round-robin or Consistent hashing
✓ LB → SERVICES: Microservices, Event-driven architecture
✓ SERVICES → CACHE: Redis (in-memory), Memcached (distributed)
✓ CACHE → DATABASE: Master-Slave or Master-Master replication
✓ DATABASE → STORAGE: Sharding, Read replicas, Object storage
✓ QUEUE: Kafka (high-throughput), RabbitMQ (reliability)
✓ SEARCH: Elasticsearch (full-text), Algolia (real-time)
✓ MONITORING: Prometheus + Grafana, Jaeger (distributed tracing)
✓ RESILIENCE: Circuit breaker, Bulkhead, Retry logic, Failover
```

---

## **STEP 4: Customize Blueprint for YOUR System**

**Remove, modify, or add components based on STEP 1 & 2:**

### **Example 1: Instagram (Photo-heavy app)**
```
Generic → Instagram version

ADDED:
  ✓ Image Processing Service (resize, compress, filter)
  ✓ Image Storage (S3 with CDN)
  ✓ Search Service (Elasticsearch for tags)

REMOVED:
  ✗ Real-time notification (not critical)
  ✗ Complex analytics (not in MVP)

KEPT:
  ✓ Feed Service (core requirement)
  ✓ Cache Layer (critical for performance)
  ✓ Message Queue (for async image processing)
```

### **Example 2: Google Maps (Location-based)**
```
Generic → Maps version

ADDED:
  ✓ Geospatial Database (PostGIS)
  ✓ Real-time Location Service
  ✓ Route Calculation Engine
  ✓ Traffic Data Service

REMOVED:
  ✗ User profiles (not relevant)

KEPT:
  ✓ Cache (caching map tiles, routes)
  ✓ Object Storage (map images)
```

---

## **STEP 5: Back-of-Envelope Estimation (5 min)**

**Answer these questions with quick math:**

### **A. How many requests per second?**

```
Formula:
  (DAU × average requests per day) ÷ 100K seconds = avg QPS
  Peak QPS = avg QPS × 2.5 (typical peak factor)

Example - Instagram:
  100M DAU × 5 requests/day = 500M requests/day
  500M ÷ 100K = 5,787 QPS average
  5,787 × 2.5 = 14,467 QPS peak

Example - Twitter:
  200M DAU × 10 requests/day = 2B requests/day
  2B ÷ 100K = 23,148 QPS average
  23,148 × 2.5 = 57,870 QPS peak (100K QPS to be safe)
```

### **B. How many servers needed?**

```
Formula:
  Peak QPS ÷ (requests per server) = servers needed
  With redundancy: × 2-3
  
  Typical server handles: 1,000-10,000 QPS depending on complexity
  Instagram (complex joins): ~1,000 QPS per server
  Simple API (returns cached data): ~10,000 QPS per server

Example:
  14,467 QPS peak ÷ 1,000 QPS/server = 14.5 servers
  With redundancy: 14.5 × 2.5 = 36 servers
  → Deploy 30-40 servers across datacenters
```

### **C. How much storage?**

```
Formula:
  (Daily Data Generated) × 365 days × years × redundancy = total

Example - Instagram photos:
  10% of 100M users upload per day = 10M photos
  3 MB per photo = 30 TB/day
  10 years = 30 TB × 365 × 10 = 109.5 PB
  3× redundancy = 328.5 PB (expensive!)

Example - Twitter posts (text-heavy):
  10% of 200M users tweet per day = 20M tweets
  0.5 KB per tweet = 10 GB/day
  10 years = 10 GB × 365 × 10 = 36.5 TB (small!)
```

### **D. If traffic grows 10X?**

```
Current: 100M DAU, 14K QPS peak, 30 TB/day
10X growth: 1B DAU, 140K QPS peak, 300 TB/day

Do we need to change architecture?
  140K QPS ÷ 1,000 per server = 140 servers
  → Still manageable with horizontal scaling
  
  300 TB/day storage
  → Need sharding if single DB can't handle
  → Cost increases significantly ($7M+/year)

Decision: When does architecture break?
  ✓ Vertical scaling (bigger servers) works until ~50K QPS
  ✗ Beyond that: Must shard database
  ✗ Need multi-region for 1B+ users
```

---

## **STEP 6: Design Deep-Dives (NFRs)**

**Address non-functional requirements:**

### **Resilience & Failure Handling**

```
Q: What if a server crashes?
A: Load balancer routes around it
   Other servers handle the load
   Auto-scaling spins up replacement

Q: What if database goes down?
A: Read replicas can be promoted to master
   Take 5-10 minutes with automation
   Or use database failover (AWS RDS has this)

Q: What if cache gets corrupted?
A: Clear cache, rebuild from database
   Queries get slower during rebuild
   But system stays up (no crash)
```

### **Monitoring & Operations**

```
Q: What metrics do we monitor?
A: Server metrics:
     • CPU usage (alert > 80%)
     • Memory usage (alert > 85%)
     • Disk space (alert > 90%)
   
   Application metrics:
     • QPS (requests per second)
     • P99 latency (99th percentile response time)
     • Error rate (% of failed requests)
     • Cache hit rate (should be > 80%)
   
   Database metrics:
     • Query latency
     • Connection pool usage
     • Slow query log

Q: What alerting do we set up?
A: PagerDuty alerts for:
     • High error rate (> 5%)
     • High latency (P99 > 500ms)
     • Server down (health check failed)
     • Database replication lag > 1 second
```

### **Consistency vs. Availability Trade-off**

```
Question: "What if different users see different data?"

Example - Like count:
  User A sees 1,000 likes
  User B sees 999 likes
  (Due to replication lag)

Tradeoff:
  Strong Consistency: Always exact number (slower, costs more)
  Eventual Consistency: Approximate, eventually accurate (faster, cheaper)

Answer:
  "For like count, eventual consistency is fine.
   Cache the count for 5 seconds (±50 likes OK).
   For payment amounts, strong consistency is required."
```

---

## **STEP 7: Verify Growth & Constraints**

**Sanity check your design against original requirements:**

### **Verify Scale**
```
Original requirement: 100M DAU
Your design handles: 140K QPS peak
Can it handle 100M DAU? YES
  100M DAU = 14K QPS peak (well within 140K capacity)

Original requirement: 10 years storage
Your design provides: 30 PB with 3× redundancy
Can it store 10 years? YES
  10 years × 30 TB/day = 109 PB (fits in 328 PB budget)
```

### **Verify Latency**
```
Original requirement: < 200ms API response
Your design latency:
  Network: 5ms
  Load Balancer: 2ms
  Web Server: 50ms
  Cache hit: 2ms
  = 59ms (with cache hit)

Without cache:
  Network: 5ms
  Load Balancer: 2ms
  Web Server: 50ms
  Database: 100ms
  = 157ms (still OK, but tight)

Requires: 80% cache hit rate to stay < 200ms
```

### **Verify Availability**
```
Original requirement: 99.9% uptime (3 nines)
= 8.76 hours downtime per year OK

Your design:
  ✓ Multi-region (survive 1 datacenter failure)
  ✓ Database replication (survive master failure)
  ✓ Load balancer failover (survive server failure)
  ✓ Cache layer (degrade gracefully if DB slow)

Expected uptime: 99.95% achievable
(Better than requirement ✓)
```

### **Verify Compliance**
```
Original requirement: GDPR compliance
Your design addresses:
  ✓ User data deletion (remove from all databases)
  ✓ Data portability (export user data)
  ✓ Encryption in transit (HTTPS)
  ✓ Encryption at rest (S3 default)

Need to add:
  • Privacy Policy (legal team)
  • Data Retention Policy
  • DPA (Data Processing Agreement) with vendors
```

---

## **COMPLETE WORKED EXAMPLE: Design Uber**

### **STEP 1: Functional Requirements**
```
Core Features:
  ✓ Rider: Request ride, see driver location, pay
  ✓ Driver: Accept rides, navigate, earn money
  ✓ Admin: Monitor trips, manage disputes, payments

Who: 100M+ riders, 3M+ drivers
Interaction:
  ✓ Rider requests ride (real-time matching)
  ✓ Driver sees request, accepts (real-time notification)
  ✓ Both see live location updates (real-time)
```

### **STEP 2: Non-Functional Requirements**
```
Scale: 500M rides/month = 16M rides/day
Read-Write: 80:20 (mostly viewing, some bookings)
Spike: 2-3X during peak hours (evening rush)

Latency:
  ✓ Location update: < 2 seconds
  ✓ Matching: < 30 seconds
  ✓ Payment: < 5 seconds

Availability: 99.99% (4 nines, critical service)
Consistency: Eventual (OK if rider sees 30-second stale location)
```

### **STEP 3: Generic Blueprint**
```
[Mobile Apps (iOS/Android)]
         ↓
    [CDN for assets]
         ↓
    [Load Balancer]
         ↓
[Auth] [Matching] [Location] [Payment] [User] services
         ↓
    [Cache (Redis)]
         ↓
    [Database (MySQL)]
    [Cache for real-time locations]
         ↓
    [S3 for photos]
    [Kafka for events]
```

### **STEP 4: Customize for Uber**
```
ADD:
  ✓ Real-time Location Service (WebSocket for live tracking)
  ✓ Geospatial Database (PostGIS for matching nearby drivers)
  ✓ Matching Algorithm Service (find best driver)
  ✓ Payment Service (Stripe integration)
  ✓ Notification Service (push notifications to riders/drivers)

REMOVE:
  ✗ Search functionality (not needed)
  ✗ Feed/social features (not core)

MODIFY:
  ✓ Cache: Need Redis for geospatial queries
  ✓ Database: Need time-series DB for trip history
```

### **STEP 5: Back-of-Envelope**
```
16M rides/day × 10 updates per ride = 160M events/day
160M events ÷ 100K sec = 1,852 events/sec average
Peak: 1,852 × 3 = 5,556 events/sec

Servers needed:
  5,556 events/sec ÷ 10,000 events/server = 0.56 servers
  → Just 1 server can handle, but we need redundancy
  → Deploy 3-5 servers for failover

Storage:
  Trip data: 16M rides × 5 KB = 80 GB/day
  10 years: 80 GB × 365 × 10 = 292 TB
  3× redundancy: 876 TB
  Photos (profile + car): 3M drivers × 1MB = 3TB
  Total: ~1 PB storage needed
```

### **STEP 6: Deep-Dives**
```
Resilience:
  Q: What if matching service crashes?
  A: Queue requests in Kafka, replay when service recovers
  
Monitoring:
  • Ride completion time (should be < 1 hour)
  • Driver acceptance rate (% of drivers accepting)
  • Matching latency (< 30 sec)
  • Payment failure rate (should be < 0.1%)

Consistency:
  • Trip status (confirmed, in-progress, completed): strong consistency
  • Driver rating: eventual consistency (can be stale 1 hour)
  • Location: eventual consistency (2-5 second stale OK)
```

### **STEP 7: Verify**
```
Scale: 16M rides/day → 5,556 events/sec peak ✓
Latency: < 2 sec for location update, can deliver in 500ms ✓
Availability: 99.99% achievable with redundancy ✓
Compliance: Payment PCI compliance required ✓
Growth 10X: 50K events/sec, need sharding, still manageable ✓
```

---

## **Key Tips for Using This Framework**

```
✓ Follow steps in order (clarify before designing)
✓ Ask "why" after interviewer's answers
✓ Use the back-of-envelope math to verify feasibility
✓ Communicate as you go (don't present a big surprise at the end)
✓ Be ready to deep-dive on any component
✓ Trade-offs are more important than perfect design

Timing allocation (45 min total):
  5 min - Clarify requirements (Step 1)
  5 min - Clarify scale/latency/availability (Step 2)
  3 min - Draw generic blueprint (Step 3)
  10 min - Customize for your system (Step 4)
  5 min - Back-of-envelope math (Step 5)
  10 min - Deep-dives on NFRs (Step 6)
  7 min - Verify & discuss trade-offs (Step 7)
```

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
  Sliding Log memory: 10 MB/day
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


---

# Chapter 8: Design a URL Shortener




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

Move session data OUT of web servers. Store in shared persistent storage.

---

# CHUBB Interview Question - Design a Unique Code Generator

A practical guide to generating unique referral codes, short URLs, vouchers, or session tokens without race conditions, database bottlenecks, or latency issues.

---

## The Problem

### What's a Referral Code?

A unique code assigned to each user for referrals, such as:
- Referral codes: `FRIEND2024ABC`
- Short URLs: `bit.ly/abc123`
- Vouchers: `SAVE50XMAS`
- Session tokens: `sess_xyz789`

At scale, generating millions of unique codes without collisions is the challenge.

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
Current approach:
  1 code per request → Kafka call per request
  1000 users/sec → 1000 Kafka calls/sec
  Latency: 5ms per request
  Overkill!
```

### The Solution

```
Bulk read 100 codes at once:

Consumer service:
  1. Read 100 codes from Kafka (1 call)
  2. Cache in memory (local list)
  3. Serve 100 requests from cache
  4. When cache empty, refill

Performance:
  1000 users/sec ÷ 100 = 10 Kafka calls/sec
  Latency: 5ms ÷ 100 = 0.05ms per user
  99.5% faster!
```

### Performance Impact

```
Without bulk read:
  Kafka calls: 1000/sec
  Latency: 5ms
  Network overhead: HIGH

With bulk read:
  Kafka calls: 10/sec (100x fewer!)
  Latency: 0.05ms
  Network overhead: MINIMAL
  Cache hit rate: 99%+
```

---

## When Codes Run Out: Regeneration

### The Timeline

```
Starting point:
  Code format: 6 chars
  Total codes: 62^6 = 56.8 billion

At 1000 codes/sec:
  56.8B ÷ 1000/sec ÷ 100K sec/day = 657 days
  ≈ 1.8 YEARS

At 100K codes/sec (peak):
  56.8B ÷ 100K/sec ÷ 100K = 6.5 days
```

### Solution: Expand Code Length

```
Current: 6 characters = 62^6 = 56.8 billion
Next: 7 characters = 62^7 = 3.5 trillion (62x more!)

Timeline with upgrade:
  62^7 codes ÷ 100K/sec = 1111 years (!!)
```

### Regeneration Process

```
Step 1: Monitor exhaustion (track % used)
Step 2: Trigger regeneration at 80% capacity
Step 3: Generate new batch with 7 chars
Step 4: Gradual switchover:
  - New users get 7-char codes
  - Old users still have 6-char codes
  - Both systems coexist (no downtime!)
Step 5: Complete migration after transition period
```

### Automated Monitoring

```python
def monitor_code_exhaustion():
    remaining = kafka.get_queue_depth()
    total = 62^6
    percent_used = (total - remaining) / total * 100
    
    if percent_used > 80:
        trigger_regeneration()
    
    log_metrics(percent_used, remaining)

# Run periodically
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
> 4. **Consumers read:** Bulk read 100 codes, cache locally
>
> **Why it works:**
> - Uniqueness guaranteed offline (no runtime checks)
> - Kafka handles atomicity (no race conditions)
> - No database overhead (simple insert)
> - Scales linearly (add partitions = add throughput)
>
> **Benefits:**
> - Latency: 150ms → 1ms (150x faster)
> - Throughput: 100 req/sec → 100K req/sec (1000x)
> - Success rate: 95% → 99.99%
> - Duplicates: Yes → IMPOSSIBLE
>
> **Applicable to:** Referral codes, short URLs, vouchers, session tokens, any high-volume unique ID generation

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

## Navigation

**Table of Contents:** [Back to Top](#table-of-contents)
