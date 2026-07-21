# System Design Interview - Chapter 1: Scale from Zero to Millions of Users

## ⚠️ Important Notice

**This document is based on concepts from "System Design Interview" by Alex Xu.**

This is **original content**: summaries, explanations, and real-world examples created to help understand system design concepts. It does not reproduce copyrighted material from the book. All content is restructured and rewritten for clarity and educational purposes.

---

## Table of Contents
1. [Executive Summary](#executive-summary)
2. [Single Server Setup](#single-server-setup)
3. [Scaling Web Tier](#scaling-web-tier)
4. [Scaling Database Tier](#scaling-database-tier)
5. [Adding Cache Layer](#adding-cache-layer)
6. [Content Delivery Network (CDN)](#content-delivery-network-cdn)
7. [Stateless vs Stateful Architecture](#stateless-vs-stateful-architecture)
8. [Multiple Data Centers](#multiple-data-centers)
9. [Message Queue](#message-queue)
10. [Complete Architecture](#complete-architecture)

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
