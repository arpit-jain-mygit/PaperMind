# System Design Interview - Chapter 8: Design a URL Shortener

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
