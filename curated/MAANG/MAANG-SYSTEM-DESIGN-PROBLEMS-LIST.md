# MAANG System Design Problems List

> Ordered by frequency (high to low) | Backlog for system design practice
> **Data Source:** LeetCode, Blind, Glassdoor, IGotAnOffer, SystemDesignHandbook (2025-2026)

---

## Interview Checklist: HLD & LLD Coverage

### HLD (High-Level Design) — Start Here

**1. Requirements Clarification**
- Functional requirements (what features?)
- Non-functional requirements (scale, latency, availability, consistency)
- Example: "How many users? QPS? Read/write ratio? Geographic distribution?"

**2. Capacity Estimation**
- DAU/MAU → QPS calculations
- Storage estimates
- Bandwidth requirements

**3. Architecture Components**
- Client layer (web, mobile, APIs)
- API Gateway / Load Balancer
- Application servers / microservices
- Database (SQL vs NoSQL choice + reasoning)
- Cache layer (Redis, Memcached where)
- Message queue (Kafka, RabbitMQ if async needed)
- Search (Elasticsearch if needed)
- CDN / static storage

**4. Data Layer**
- Database choice & schema design
- Partitioning/sharding strategy (if needed)
- Replication & failover

**5. Scalability & Performance**
- Horizontal scaling approach
- Caching strategy (what to cache, TTL)
- Rate limiting / throttling
- Monitoring & alerting

**6. Tradeoffs**
- Why this choice over alternatives
- Consistency vs Availability tradeoffs
- Latency vs Durability

---

### LLD (Low-Level Design) — Deep Dive When Asked

**1. Data Models / Entities**
- Class/object definitions
- Relationships (1:1, 1:N, M:N)
- Primary/foreign keys

**2. API Contracts**
- Endpoints (REST, gRPC)
- Request/response format
- Status codes & error handling

**3. Database Schema**
- Normalized vs denormalized approach
- Indexes (what to index, why)
- Partitioning keys if sharded

**4. Core Logic / Algorithms**
- Key operations (read, write, search, delete)
- Algorithm complexity
- Edge cases handled

**5. Concurrency & Consistency**
- Thread safety (locks, atomic operations, ConcurrentHashMap)
- Transaction handling
- Consistency guarantees per operation

**6. Error Handling**
- Exception types
- Retry logic
- Graceful degradation

**7. Optimization**
- Query optimization
- Caching at code level
- Connection pooling

---

### Pro Tips
- **HLD first, then LLD** — start broad, drill down only when asked
- **Explain tradeoffs**, not just choices — "I chose X because Y, tradeoff is Z"
- **Be ready to scale deeper** — if they push, have LLD details ready
- **Time management** — spend 60-70% on HLD, 30-40% on LLD (unless they ask otherwise)

---

## Table of Contents

### 🔴 Highest Priority (70%+)

| # | Problem | Frequency | Companies | Key Concepts |
|---|---------|-----------|-----------|--------------|
| [1](#1-design-localin-process-cache-system-lru--lfu-cache) | Local Cache (LRU/LFU) | 76% | Google • Meta • Amazon • Apple • Netflix | Hash Map • Linked List • LRU/LFU • TTL |
| [2](#2-design-recommendation-system) | Recommendation System | 73% | Netflix • Amazon • Google • Meta • Apple | Collaborative Filtering • Content-Based • ML |
| [3](#3-design-netflix--video-on-demand-vod) | Netflix/VOD | 72% | Netflix • Amazon • Google | Video Encoding • ABR • CDN • HLS/DASH |
| [4](#4-design-youtube--video-streaming) | YouTube Streaming | 70% | Google • Netflix • Meta • Amazon | Video Transcoding • ABR • CDN • Sharding |
| [5](#5-design-e-commerce-platform) | E-commerce | 68% | Amazon • Google • Apple • Meta | ACID • CAP • Inventory • Sharding |
| [6](#6-url-shortener) | URL Shortener | 68% | Meta • Google • Amazon | Base Encoding • Hashing • Distributed ID • TTL |
| [7](#7-design-instagram--photo-sharing-social-network) | Instagram | 68% | Meta • Google • Amazon | Sharding • Consistent Hashing • Denormalization • Cache-Aside • CDN |

### 🟠 High Priority (60-69%)

| # | Problem | Frequency | Companies | Key Concepts |
|---|---------|-----------|-----------|--------------|
| [8](#8-design-twitter--social-media-feed) | Twitter/Social Feed | 65% | Meta • Google • Amazon | Fan-Out • Denormalization • Consistent Hashing • Timeline Ranking |
| [9](#9-design-distributed-cache-system-redis-like) | Distributed Cache (Redis) | 65% | Google • Amazon • Meta • Netflix • Apple | In-Memory DS • Consistent Hashing • Persistence • Replication • Pub/Sub |
| [10](#10-design-database-sql--nosql) | Distributed Database System | 65% | Google • Amazon • Meta • Apple • Netflix | ACID • CAP • Consistent Hashing • Indexing • Replication • Sharding |
| [11](#11-design-content-delivery-network-cdn) | CDN | 62% | Google • Netflix • Amazon • Meta • Apple | Consistent Hashing • DNS Routing • Edge Servers • Cache Invalidation |
| [12](#12-design-notification-system) | Notifications | 62% | Amazon • Google • Meta • Apple • Netflix | Message Queues • Pub/Sub • Consistent Hashing • Delivery Semantics |
| [13](#13-design-news-feed--timeline) | News Feed/Timeline | 62% | Meta • Google • Amazon • Apple | Fan-Out • Timeline Ranking • Consistent Hashing • Cache-Aside |
| [14](#14-design-consistent-hashing) | Consistent Hashing | 62% | Google • Amazon • Meta • Netflix • Apple | Hash Ring • Virtual Nodes • Load Distribution • Rebalancing |
| [15](#15-design-metrics--analytics-system) | Metrics/Analytics | 58% | Google • Amazon • Meta • Netflix • Apple | Time-Series DB • Consistent Hashing • Aggregation • Compression |
| [16](#16-design-messaging-system-whatsapp--facebook-messenger) | Messaging | 58% | Meta • Apple • Amazon • Google | Message Ordering • Consistent Hashing • Encryption • Persistence |
| [17](#17-design-rate-limiter) | Rate Limiter | 58% | Google • Amazon • Meta • Apple | Token Bucket • Consistent Hashing • Sliding Window • Redis |
| [18](#18-design-distributed-unique-id-generator-uuid--snowflake) | UUID Generator | 58% | Google • Amazon • Meta • Netflix • Apple | Distributed ID • Consistent Hashing • Timestamp • Machine ID |

### 🟡 Medium-High Priority (50-59%)

| # | Problem | Frequency | Companies | Key Concepts |
|---|---------|-----------|-----------|--------------|
| [19](#19-design-file-storage-system-dropbox--google-drive) | File Storage | 57% | Google • Apple • Amazon • Meta | Distributed Storage • Consistent Hashing • Delta Sync • Versioning |
| [20](#20-design-search-engine-elasticsearch-like) | Search Engine | 55% | Google • Meta • Amazon • Apple | Consistent Hashing • Inverted Index • TF-IDF • Ranking |
| [21](#21-design-search-autocomplete-typeahead) | Autocomplete | 52% | Google • Meta • Amazon • Apple | Consistent Hashing • Trie • Fuzzy Matching • Frequency Tracking |
| [22](#22-design-distributed-web-crawler) | Web Crawler | 52% | Google • Meta • Amazon | Consistent Hashing • URL Frontier • Politeness • Duplicate Detection |
| [23](#23-design-payment-system) | Payment System | 52% | Amazon • Apple • Google • Meta | ACID • Consistent Hashing • Idempotency • Encryption • Reconciliation |
| [24](#24-design-distributed-job-queue--task-scheduler) | Job Queue | 52% | Google • Amazon • Meta • Netflix • Apple | Queues • Consistent Hashing • Priority • Retry Logic • Scheduling |
| [25](#25-design-uber--ride-sharing) | Uber | 48% | Amazon • Google • Meta | Consistent Hashing • Geospatial Indexing • Real-Time Tracking • ETA |
| [26](#26-design-api-gateway--load-balancer) | API Gateway & Load Balancer | 48% | Google • Amazon • Meta • Apple | Consistent Hashing • Routing • Load Balancing • Circuit Breaker |
| [27](#27-design-booking-system-hotel--airbnb--flight) | Booking System | 48% | Amazon • Google • Meta • Apple | Consistent Hashing • Inventory • Concurrency Control • Strong Consistency |
| [28](#28-design-chat-system-slack-like) | Chat System | 48% | Meta • Apple • Amazon • Google | Message Ordering • Encryption • Consistent Hashing • WebSocket |

### 🟢 Medium Priority (30-49%)

| # | Problem | Frequency | Companies | Key Concepts |
|---|---------|-----------|-----------|--------------|
| [29](#29-design-real-time-analytics-dashboard) | Analytics Dashboard | 42% | Google • Amazon • Meta • Netflix • Apple | Time-Series • Stream Processing • Consistent Hashing • Caching |
| [30](#30-design-parking-lot-system) | Parking Lot | 35% | Amazon • Google • Meta | Spatial Indexing • Consistent Hashing • Inventory • Concurrency |
| [31](#31-design-tiktok--short-form-video-platform) | TikTok | 32% | Meta • Google • Amazon | Consistent Hashing • Recommendation • Video Encoding • Feed Ranking |

---

## 1. Design Local/In-Process Cache System (LRU / LFU Cache)

**Frequency:** 76% | **Asked by:** Google (90%) • Meta (80%) • Amazon (75%) • Apple (70%) • Netflix (60%)

**Key Concepts:**
Hash Map • Doubly Linked List • LRU/LFU Eviction • TTL • Memory Management • O(1) Operations

**Functional Requirements:**
- Get and put operations for cache
- Eviction policy when cache is full (LRU, LFU, FIFO)
- Support TTL (time-to-live) for cache entries
- Clear cache operation
- Cache statistics (hit/miss rate)

**Non-Functional Requirements:**
- O(1) time complexity for get and put operations
- Memory efficient
- Multi-threaded support (concurrent access from multiple threads)
- Scalable to millions of entries

---

## Problem-Specific Checklist: In-Memory Cache

| ⚠️ CRITICAL | 1. Requirements Clarification | Max capacity, throughput (ops/sec), eviction policy (LRU/LFU/TTL), thread concurrency, performance targets (hit rate %, latency SLA) |
| ⚠️ CRITICAL | 2. Capacity Estimation | Memory per entry (key + value + metadata overhead), hit rate targets, throughput expectations, total memory calculation |
| ✗ NOT APPLICABLE | 3. Architecture Components | ~~Consistent Hashing~~ • ~~API Gateway~~ • ~~Load Balancer~~ • ~~Database~~ • ~~CDN~~ (single-process only) |
| ✗ NOT APPLICABLE | 4. Data Layer | ~~Database choice~~ • ~~Sharding~~ • ~~Replication~~ (in-memory only, no persistence) |
| ⚠️ PARTIAL | 5. Scalability & Performance | Single-node vertical scaling • Memory limits • GC behavior • Concurrent thread limits • ⚠️ CRITICAL: Monitoring (hit/miss ratio, eviction rate, latency, lock contention) |
| ⚠️ CRITICAL | 6. Tradeoffs | LRU vs LFU (simplicity vs accuracy) • Coarse vs fine-grained locking (consistency vs throughput) • Eager vs lazy eviction (memory vs latency) • Lock contention vs correctness |
| ⚠️ CRITICAL | **7. Cache-Specific Additions** | **Eviction Policy:** LRU (temporal locality) • LFU (frequency-based) • TTL • Hybrid • **Thread Safety:** ReentrantLock (simple, high contention) • Segment-based locks (better concurrency) • ReadWriteLock (read-heavy) • Lock-free (complex) • **Memory Management:** Max capacity, metadata overhead, proactive vs reactive eviction • **Monitoring:** Hit/miss %, eviction rate, lookup latency, memory utilization |

### LLD (From Generic Checklist — Customized)

| CRITICAL? | Item | Cache-Specific Details |
|-----------|------|------------------------|
| ⚠️ CRITICAL | 1. Data Models / Entities | Cache entry: {key, value, lastAccessTime/frequency, expirationTime, prev/next pointers (LRU)} • Metadata overhead per entry |
| ⚠️ CRITICAL | 2. API Contracts | get(key) → V • put(key, value) → void • delete(key) → boolean • clear() • getStats() • Null handling: keys/values/expired entries |
| ✗ NOT APPLICABLE | 3. Database Schema | ~~Indexes~~ • ~~Normalization~~ (HashMap IS the index, in-memory data structure) |
| ⚠️ CRITICAL | 4. Core Logic / Algorithms | **MUST BE O(1):** get(key) • put(key, value) • delete(key) • **LRU:** HashMap + DoublyLinkedList → O(1) all ops • **LFU:** HashMap + Frequency buckets → O(1) all ops • **LFU (Heap):** HashMap + MinHeap → O(log N) eviction (slower) |
| ⚠️ CRITICAL | 5. Concurrency & Consistency | **Thread-Safe Access:** All operations atomic, no race conditions on eviction • **Strategies:** (1) Global ReentrantLock: simple but high contention • (2) Segment-based locks: better concurrency, complex eviction • (3) ReadWriteLock: read-heavy optimization • **Critical Issues:** Lost updates, double eviction, capacity overflow, concurrent modification |
| ✓ APPLICABLE | 6. Error Handling | Null keys/values (clarify), OOM prevention (evict before full), TTL expiration (lazy vs active cleanup), concurrent race conditions |
| ⚠️ CRITICAL | 7. Optimization | Minimize lock critical section (<1ms) • O(1) node removal • Memory efficiency (no boxing) • Cache-line alignment • Avoid GC pressure • Lock-free where possible |
| ✗ NOT APPLICABLE | 8. NOT APPLICABLE Items | ~~Database schema~~ • ~~Request transformation~~ • ~~Query optimization~~ • ~~Distributed transactions~~ |
| ⚠️ CRITICAL | **9. Cache-Specific Additions** | **Data Structure:** LRU = HashMap + DoublyLinkedList • LFU = HashMap + Frequency buckets • **TTL:** Lazy (check on get) vs Active (background cleanup) vs Hybrid • **Segment Optimization:** Divide into N segments with own locks, reduces contention by N× |

---

## 2. Design Recommendation System

**Frequency:** 73% | **Asked by:** Netflix (95%) • Amazon (80%) • Google (65%) • Meta (55%) • Apple (45%)

**Key Concepts:**
Collaborative Filtering • Content-Based Filtering • Similarity Metrics • Matrix Factorization • Cold Start Problem • A/B Testing • Feature Engineering

**Functional Requirements:**
- Generate personalized recommendations for users
- Support collaborative filtering (user-user, item-item)
- Support content-based recommendations
- Support hybrid recommendations
- Allow users to rate items and give feedback
- Real-time or batch recommendations
- Support for cold-start problem (new users/items)
- A/B testing support

**Non-Functional Requirements:**
- Low latency for recommendation generation (< 500ms)
- Scalable to millions of users and items
- High throughput for model training
- Efficient feature computation
- Support for real-time feedback
- Support for multiple recommendation algorithms
- Memory and computation efficient

---

## 3. Design Netflix / Video on Demand (VOD)

**Frequency:** 72% | **Asked by:** Netflix (95%) • Amazon (40%) • Google (35%)

**Key Concepts:**
Consistent Hashing • Video Encoding (H.264, VP9) • Adaptive Bitrate Streaming • CDN • Manifest Files (DASH, HLS) • Caching • Database Partitioning • Eventual Consistency

**Functional Requirements:**
- Users can browse and search for movies/shows
- Stream content with multiple quality options
- Track watch history and resume from last position
- Personalized recommendations
- Support multiple user profiles in one account
- Allow downloads for offline viewing
- Support ratings and reviews
- Subscription management

**Non-Functional Requirements:**
- High availability and fault tolerance
- Support millions of concurrent streams
- Low buffering and fast start time (< 2s)
- Adaptive bitrate streaming
- Global CDN distribution
- Efficient video compression
- Strong license compliance
- Support offline content expiry

---

## 4. Design YouTube / Video Streaming

**Frequency:** 70% | **Asked by:** Google (75%) • Netflix (90%) • Amazon (40%) • Meta (55%)

**Key Concepts:**
Consistent Hashing • Video Transcoding • Adaptive Bitrate Streaming • CDN • Caching Hierarchy • Load Balancing • Database Sharding • Search Indexing • Video Deduplication

**Functional Requirements:**
- Users can upload, view, and delete videos
- Stream videos with adaptive bitrate (different quality levels)
- Support for playlists and channels
- Search and filter videos by metadata
- Like, comment, share videos
- Video recommendations based on watch history
- Subscribe to channels
- Autoplay and watch history

**Non-Functional Requirements:**
- High availability and fault tolerance
- Support millions of concurrent video streams
- Low video start time (< 2s)
- Adaptive bitrate streaming for variable network conditions
- CDN optimization for global distribution
- Efficient video compression
- Support for real-time transcoding

---

## 5. Design E-commerce Platform

**Frequency:** 68% | **Asked by:** Amazon (90%) • Google (40%) • Apple (35%) • Meta (30%)

**Key Concepts:**
Consistent Hashing • ACID Transactions • CAP Theorem • Inventory Management • Concurrency Control • Caching Strategies • Search Engine Indexing • Payment Gateway • Database Sharding

**Functional Requirements:**
- Product catalog with search and filtering
- Shopping cart and checkout process
- Order management (create, cancel, track)
- Payment processing
- Inventory management and updates
- User reviews and ratings
- Wishlist/favorites
- Order history and returns

**Non-Functional Requirements:**
- High availability for browsing and checkout
- Consistent inventory across multiple locations
- Support millions of concurrent users
- Low latency for product searches (< 200ms)
- High transaction throughput during sales
- Secure payment processing
- Scalable inventory system

---

## 6. URL Shortener

**Frequency:** 68% | **Asked by:** Meta (70%) • Google (65%) • Amazon (60%)

**Key Concepts:**
Base Encoding (Base62) • Hashing Algorithms • Distributed ID Generation • Database Indexing • Collision Detection • TTL/Expiration • Consistency vs. Availability

**Functional Requirements:**
- Convert long URLs to short, unique URLs
- Redirect users from short URL to original long URL
- Allow users to customize short URLs (optional)
- Track expiration for shortened URLs (optional)
- Allow users to delete their shortened URLs

**Non-Functional Requirements:**
- High availability and low latency (< 100ms redirects)
- Highly scalable (billions of short URLs)
- Short URL should be unique and collision-free
- Backward compatibility (once created, short URL cannot change)
- Analytics tracking (click counts, geographic data)

---

## 7. Design Instagram / Photo Sharing Social Network

**Frequency:** 68% | **Asked by:** Meta (85%) • Google (50%) • Amazon (35%)

**Key Concepts:**
Consistent Hashing • Database Sharding • Denormalization • Cache-Aside Pattern • CDN • Image Compression • Full-Text Search • Real-Time Notifications • Feed Ranking

**Functional Requirements:**
- Users can upload, delete photos/images
- Users can browse feed from followed users
- Like, comment on photos
- Direct messaging between users
- Search users and hashtags
- User profiles with follower counts
- Stories feature (temporary content)
- Follow/unfollow functionality

**Non-Functional Requirements:**
- High availability and low latency (< 200ms)
- Highly scalable (millions of concurrent users)
- Efficient image storage and compression
- Fast feed generation
- Real-time notifications
- Support for billions of images
- CDN optimization for image delivery

---

## 8. Design Twitter / Social Media Feed

**Frequency:** 65% | **Asked by:** Meta (90%) • Amazon (35%) • Google (40%)

**Key Concepts:**
Consistent Hashing • Push vs. Pull Architecture (Fan-out) • Denormalization • Cache-Aside Pattern • Timeline Ranking • Eventual Consistency • Message Queues • Search Indexing

**Functional Requirements:**
- Users can post tweets (create, update, delete)
- Users can view their timeline/feed
- Users can follow/unfollow other users
- Support likes, retweets, replies to tweets
- Users can search tweets
- Display trending topics/hashtags
- Support user profiles with follower/following counts

**Non-Functional Requirements:**
- High availability and fault tolerance
- Ultra-low latency for feed rendering (< 200ms)
- Highly scalable (millions of concurrent users)
- Eventual consistency acceptable for feed
- Real-time notifications for interactions
- Efficient storage for billions of tweets

---

## 9. Design Distributed Cache System (Redis-like)

**Frequency:** 65% | **Asked by:** Google (85%) • Amazon (75%) • Meta (70%) • Netflix (60%) • Apple (55%)

**Key Concepts:**
Consistent Hashing • In-Memory Data Structures • Eviction Policies (LRU, LFU) • Persistence (RDB, AOF) • Replication • Clustering/Sharding • Pub/Sub • Transactions

**Functional Requirements:**
- Get, set, delete operations
- Support data types (string, list, set, hash, sorted set)
- Expiration/TTL support
- Pub/sub messaging
- Transactions (MULTI/EXEC)
- Persistence options

**Non-Functional Requirements:**
- Ultra-high throughput (millions of ops/sec)
- Ultra-low latency (< 5ms)
- High availability with replication
- Fault tolerance and recovery
- Memory efficient
- Support for sharding/partitioning
- Atomic operations

---

## 10. Design Distributed Database System (SQL / NoSQL)

**Frequency:** 65% | **Asked by:** Google (80%) • Amazon (75%) • Meta (65%) • Apple (60%) • Netflix (50%)

**Key Concepts:**
Consistent Hashing • ACID Properties • CAP Theorem • Consistency Models • Indexing (B-tree, LSM-tree) • Query Optimization • Replication • Sharding • Transactions

**Functional Requirements:**
- Create, read, update, delete operations
- Query language support (SQL for RDBMS)
- Indexing support
- Transaction support (ACID)
- Backup and recovery
- User authentication and authorization
- Replication support

**Non-Functional Requirements:**
- High availability and fault tolerance
- Scalability (horizontal or vertical)
- Consistency levels (strong, eventual)
- Query performance optimization
- Support for large datasets
- Concurrent access support
- Data durability

---

## 11. Design Content Delivery Network (CDN)

**Frequency:** 62% | **Asked by:** Google (85%) • Netflix (80%) • Amazon (70%) • Meta (60%) • Apple (50%)

**Key Concepts:**
Consistent Hashing • DNS & Geo-Routing • Edge Servers • Cache Invalidation • Anycast Routing • Cache Replacement Policies • Bandwidth Optimization • Latency Measurement

**Functional Requirements:**
- Store content at geographically distributed edge servers
- Route users to nearest edge server
- Support cache invalidation
- Support streaming content
- Monitor origin server health
- Support multiple content types

**Non-Functional Requirements:**
- Ultra-low latency for content delivery
- High availability across global infrastructure
- Efficient bandwidth utilization
- Support for terabytes of content
- Fast content propagation to edges
- Support for both pull and push models
- Scalability across hundreds of edge servers

---

## 12. Design Notification System

**Frequency:** 62% | **Asked by:** Amazon (70%) • Google (60%) • Meta (55%) • Apple (50%) • Netflix (40%)

**Key Concepts:**
Consistent Hashing • Message Queues (Kafka, RabbitMQ) • Pub/Sub Pattern • Delivery Semantics • Retry Mechanisms • Idempotency • Rate Limiting • Multi-Channel Delivery • Dead Letter Queues

**Functional Requirements:**
- Send notifications to users via multiple channels (push, email, SMS)
- Schedule notifications for future delivery
- Support notification templates and personalization
- Track notification delivery and read status
- Support notification preferences/unsubscribe
- Handle different notification types (alerts, promotions, transactional)
- Retry mechanism for failed notifications

**Non-Functional Requirements:**
- High availability and reliability
- Extremely high throughput (millions of notifications/sec)
- Low latency for real-time notifications (< 5s)
- Scalability to support billions of users
- Exactly-once delivery semantics (or at-least-once with idempotency)
- Support multiple notification channels
- Fault tolerance and graceful degradation

---

## 13. Design News Feed / Timeline

**Frequency:** 62% | **Asked by:** Meta (90%) • Google (40%) • Amazon (35%) • Apple (30%)

**Key Concepts:**
Consistent Hashing • Database Denormalization • Push vs. Pull Architecture • Timeline Ranking • Cache-Aside Pattern • Eventual Consistency • Real-Time Updates • Search Indexing

**Functional Requirements:**
- Display posts from users you follow
- Sort posts by relevance/recency
- Support infinite scroll/pagination
- Like, comment, share on posts
- Support for trending content
- Personalized feed based on user preferences
- Filter feed by type (all, photos, videos)
- Mute/hide posts from specific users

**Non-Functional Requirements:**
- Low latency feed generation (< 200ms)
- High availability
- Highly scalable for millions of users
- Support billions of posts
- Real-time updates to feed
- Support for ranking/personalization algorithms
- Efficient caching strategies

---

## 14. Design Consistent Hashing

**Frequency:** 62% | **Asked by:** Google (85%) • Amazon (75%) • Meta (70%) • Netflix (65%) • Apple (55%)

**Key Concepts:**
Hash Ring • Virtual Nodes • Load Distribution • Data Rebalancing • Minimum Disruption • Partition Tolerance • Scalability • Ring Balancing Algorithms

**Functional Requirements:**
- Map keys to servers in a distributed system
- Support adding and removing servers dynamically
- Minimize key redistribution when topology changes
- Support weighted servers (different capacities)
- Evenly distribute keys across all servers
- Support hot-spot mitigation

**Non-Functional Requirements:**
- O(log N) lookup time
- O(K/N) data movement on server addition/removal (K = total keys, N = number of servers)
- Support for millions of keys
- Low memory overhead for hash ring
- Fast node addition/removal operations
- Support for virtual nodes (replicas on the ring)
- Load distribution with standard deviation < 10%

---

## 15. Design Metrics / Analytics System

**Frequency:** 58% | **Asked by:** Google (90%) • Amazon (75%) • Meta (65%) • Netflix (50%) • Apple (45%)

**Key Concepts:**
Consistent Hashing • Time-Series Database • Data Aggregation • Data Compression • Sampling Techniques • Stream Processing • Data Retention Policies • Cardinality Handling • Alerting

**Functional Requirements:**
- Collect metrics and events from applications
- Store metrics in time-series format
- Query metrics for dashboarding and alerting
- Support aggregations (sum, avg, percentile)
- Support alerting on metric thresholds
- Retention policies for historical data
- Export metrics data

**Non-Functional Requirements:**
- Ultra-high throughput (millions of metrics/sec)
- Low latency for metric ingestion
- Efficient storage for time-series data
- Fast query performance for dashboards (< 1s)
- High availability and fault tolerance
- Support for high cardinality metrics
- Multi-tenancy support

---

## 16. Design Messaging System (WhatsApp / Facebook Messenger)

**Frequency:** 58% | **Asked by:** Meta (75%) • Apple (60%) • Amazon (40%) • Google (35%)

**Key Concepts:**
Consistent Hashing • Message Ordering • Delivery Semantics (At-Least-Once) • Message Persistence • Encryption (E2E) • Compression • Acknowledge Mechanisms • Message Indexing • Presence Tracking

**Functional Requirements:**
- Users can send and receive one-to-one messages
- Support group messaging (up to N members)
- Messages should be delivered and read receipts
- Support media sharing (images, videos, files)
- Search message history
- Block/unblock users
- Delete messages (for user/all)
- End-to-end encryption support

**Non-Functional Requirements:**
- Real-time message delivery (< 1s)
- High availability and reliability
- Offline message storage and sync when online
- End-to-end encryption for privacy
- Support billions of messages per day
- Efficient bandwidth usage
- Support push notifications

---

## 17. Design Rate Limiter

**Frequency:** 58% | **Asked by:** Google (70%) • Amazon (65%) • Meta (45%) • Apple (35%)

**Key Concepts:**
Consistent Hashing • Token Bucket Algorithm • Leaky Bucket Algorithm • Sliding Window • Sliding Window with Counters • Distributed Coordination • Redis State Management • Quota Allocation

**Functional Requirements:**
- Limit number of requests per user/IP in time window
- Support multiple rate limiting algorithms (token bucket, sliding window, etc.)
- Return clear error responses when limit exceeded
- Support different rate limits for different API endpoints/users
- Allow whitelist/blacklist of users or IPs
- Support rate limit quota information in response headers

**Non-Functional Requirements:**
- High availability and low latency (< 10ms decision)
- Highly scalable (billions of requests/sec)
- Distributed rate limiting across multiple servers
- Support for various time windows (sec, min, hour, day)
- Memory efficient
- Support for multiple data centers

---

## 18. Design Distributed Unique ID Generator (UUID / Snowflake)

**Frequency:** 58% | **Asked by:** Google (75%) • Amazon (65%) • Meta (50%) • Netflix (40%) • Apple (35%)

**Key Concepts:**
Consistent Hashing • Distributed ID Generation • Timestamp Ordering • Machine ID / Worker ID • Sequence Numbers • Collision Avoidance • Clock Synchronization • Snowflake ID Concept

**Functional Requirements:**
- Generate globally unique IDs
- IDs should be sortable by timestamp
- Support for multiple ID generation services
- Thread-safe and process-safe generation
- No central point of failure

**Non-Functional Requirements:**
- Ultra-high throughput (millions of IDs/sec)
- Very low latency (< 1ms)
- Guaranteed uniqueness across all generators
- Support for distributed generation
- Scalable to handle datacenter replication
- Efficient in ID format (64-bit or similar)

---

## 19. Design File Storage System (Dropbox / Google Drive)

**Frequency:** 57% | **Asked by:** Google (70%) • Apple (65%) • Amazon (50%) • Meta (45%)

**Key Concepts:**
Consistent Hashing • File System Design • Distributed Storage • Replication & Redundancy • File Versioning • Delta Sync (Binary Diff) • Strong Consistency • Sharding • Access Control

**Functional Requirements:**
- Users can upload, download, delete files
- Support file versioning (keep previous versions)
- Share files/folders with other users with permissions
- Support folder hierarchy
- File sync across devices
- Search files by name and metadata
- Trash/recycle bin with restore
- Collaborative editing (optional)

**Non-Functional Requirements:**
- High availability and reliability
- Data durability (data should not be lost)
- Support large files (up to multiple GB)
- Efficient bandwidth usage (delta sync)
- Low latency for file operations
- Scalable to handle billions of files
- Support for concurrent access
- Strong consistency for file operations

---

## 20. Design Search Engine (Elasticsearch-like)

**Frequency:** 55% | **Asked by:** Google (90%) • Meta (45%) • Amazon (35%) • Apple (30%)

**Key Concepts:**
Consistent Hashing • Inverted Indexing • Full-Text Search • Ranking Algorithms (TF-IDF, BM25) • Query Parsing & Tokenization • Stemming/Lemmatization • Faceted Search • Index Compression

**Functional Requirements:**
- Index documents for fast searching
- Full-text search with relevance ranking
- Support complex queries (boolean, phrases, ranges)
- Near real-time indexing
- Support faceted search
- Auto-suggest/autocomplete
- Search aggregations (grouping, counting)

**Non-Functional Requirements:**
- High throughput for indexing (docs/sec)
- Low latency for search queries (< 500ms)
- Support for billions of documents
- Scalable across multiple nodes
- High availability and fault tolerance
- Memory and disk efficient
- Support for distributed search

---

## 21. Design Search Autocomplete (Typeahead)

**Frequency:** 52% | **Asked by:** Google (85%) • Meta (50%) • Amazon (40%) • Apple (30%)

**Key Concepts:**
Consistent Hashing • Trie Data Structure • Prefix-Based Searching • Fuzzy Matching (Levenshtein) • Frequency Tracking • Suggestion Ranking • Caching • Real-Time Updates • Trie Optimization

**Functional Requirements:**
- Return top N suggestions as user types
- Support for typo correction (fuzzy matching)
- Personalized suggestions based on user history
- Popular/trending suggestions
- Support multiple languages
- Fast response time for suggestions

**Non-Functional Requirements:**
- Ultra-low latency (< 100ms for suggestions)
- Highly scalable (support millions of concurrent searches)
- High throughput for suggestion requests
- Support billion-scale keyword database
- Memory efficient trie/prefix tree
- Real-time updates to trending keywords

---

## 22. Design Distributed Web Crawler

**Frequency:** 52% | **Asked by:** Google (80%) • Meta (35%) • Amazon (30%)

**Key Concepts:**
Consistent Hashing • URL Frontier Management • Politeness Policies (robots.txt) • DNS Resolution • Distributed Coordination • Duplicate Detection (Bloom Filter) • URL Normalization • Metadata Extraction

**Functional Requirements:**
- Crawl web pages starting from seed URLs
- Follow links and crawl pages recursively
- Respect robots.txt and crawl delays
- Support for multiple concurrent downloads
- Store crawled content in database
- Support URL filtering and deduplication
- Extract metadata from pages

**Non-Functional Requirements:**
- High throughput (pages per second)
- Distributed crawling across multiple nodes
- Fault tolerance and recovery
- Efficient memory usage for URL frontier
- Support for very large scale (billions of pages)
- Politeness to target websites
- Scalable storage for crawled content

---

## 23. Design Payment System

**Frequency:** 52% | **Asked by:** Amazon (75%) • Apple (70%) • Google (40%) • Meta (30%)

**Key Concepts:**
Consistent Hashing • ACID Transactions • 2-Phase Commit • Idempotency & Idempotency Keys • PCI Compliance • Encryption (AES, RSA) • Secure Communication (TLS) • Ledger Systems • Reconciliation

**Functional Requirements:**
- Process payments from user to merchant
- Support multiple payment methods (credit card, UPI, wallet)
- Refund and transaction reversal
- Support subscription/recurring payments
- Transaction history and statements
- Receipt generation
- Support for multi-currency transactions
- PCI compliance and security

**Non-Functional Requirements:**
- High availability and reliability (payment critical)
- Extremely high consistency (ACID transactions)
- Support very high transaction throughput
- Low latency for payment processing (< 2s)
- Fraud detection and prevention
- Encryption for sensitive data
- Support for audit trails and compliance
- Idempotency to prevent duplicate charges

---

## 24. Design Distributed Job Queue / Task Scheduler

**Frequency:** 52% | **Asked by:** Google (70%) • Amazon (65%) • Meta (45%) • Netflix (35%) • Apple (30%)

**Key Concepts:**
Consistent Hashing • Queue Data Structures • Priority Queues • Task Scheduling Algorithms • Cron-Like Scheduling • Retry Logic & Exponential Backoff • Fault Tolerance • Worker Pool Management

**Functional Requirements:**
- Submit jobs/tasks for asynchronous execution
- Schedule jobs to run at specific times
- Support job priorities
- Retry failed jobs with exponential backoff
- Support job cancellation
- Track job status and results
- Support for cron-like scheduling
- Distributed execution across multiple workers

**Non-Functional Requirements:**
- High throughput (millions of jobs/sec)
- Low latency for job submission
- High availability and fault tolerance
- Scalability to support large number of jobs
- Exactly-once or at-least-once job execution
- Efficient resource utilization across workers
- Support for job dependencies

---

## 25. Design Uber / Ride Sharing

**Frequency:** 48% | **Asked by:** Amazon (55%) • Google (50%) • Meta (35%)

**Key Concepts:**
Consistent Hashing • Geospatial Indexing (Quadtree, R-tree) • Real-Time Location Tracking • Matching Algorithms • ETA Estimation • Surge Pricing Algorithms • Message Queues • Concurrency Control

**Functional Requirements:**
- Users can request rides from location A to B
- Driver can accept/reject ride requests
- Real-time tracking of driver location
- Calculate fare based on distance and time
- User can rate driver and driver can rate user
- Support for ride history and trip details
- Cancellation policies and handling

**Non-Functional Requirements:**
- Real-time location updates (< 5s latency)
- High availability for critical operations
- Highly scalable (millions of concurrent rides)
- Reliable geolocation matching algorithm
- Support for surge pricing during peak hours
- Accurate ETA calculation

---

## 26. Design API Gateway & Load Balancer

**Frequency:** 48% | **Asked by:** Google (65%) • Amazon (55%) • Meta (40%) • Apple (35%)

**Key Concepts:**
Consistent Hashing • Request Routing Algorithms • Load Balancing Strategies • Circuit Breaker Pattern • Rate Limiting • Request/Response Transformation • Authentication (OAuth, JWT) • Service Discovery

**Functional Requirements:**
- Route requests to appropriate backend services
- Support various routing strategies (round-robin, least connections, IP hash)
- Rate limiting at gateway level
- Request/response transformation
- Support authentication and authorization
- API versioning support
- Logging and monitoring of requests
- Support for weighted routing and canary deployments

**Non-Functional Requirements:**
- Ultra-high availability (no single point of failure)
- Ultra-low latency (< 50ms overhead)
- Support millions of concurrent connections
- Highly scalable across multiple data centers
- Efficient resource utilization
- Support for both HTTP and non-HTTP protocols
- Graceful handling of slow/failing backends

---

## 27. Design Booking System (Hotel / Airbnb / Flight)

**Frequency:** 48% | **Asked by:** Amazon (65%) • Google (45%) • Meta (35%) • Apple (30%)

**Key Concepts:**
Consistent Hashing • Inventory Management • Concurrency Control (Locks, MVCC) • ACID Transactions • Overbooking Prevention • Search Index Optimization • Strong Consistency • Payment Integration

**Functional Requirements:**
- Search available rooms/properties by dates and location
- Display room details, pricing, availability
- Book a room with payment processing
- Cancel bookings with refund policies
- View booking history and reservations
- Support reviews and ratings
- Support for waitlisting (optional)
- Overbooking handling

**Non-Functional Requirements:**
- High consistency for inventory management
- Support concurrent bookings without double-booking
- Highly available for search and booking
- High throughput during peak times (surge booking)
- Low latency for availability queries (< 200ms)
- Support for large scale (millions of properties)
- Transaction support for booking + payment

---

## 28. Design Chat System (Slack-like)

**Frequency:** 48% | **Asked by:** Meta (70%) • Apple (60%) • Amazon (40%) • Google (35%)

**Key Concepts:**
Consistent Hashing • Message Ordering Guarantees • Delivery Semantics • Message Queues & Persistence • WebSocket & Real-Time Communication • End-to-End Encryption • Message Search Indexing • Presence Tracking

**Functional Requirements:**
- One-to-one and group chats
- Message history and search
- Support reactions/emojis on messages
- Media sharing (images, files)
- Threading/nested conversations
- Mentions and notifications
- Read receipts and typing indicators
- Channel management (create, delete, archive)

**Non-Functional Requirements:**
- Real-time message delivery (< 1s)
- High availability and fault tolerance
- Support millions of concurrent users
- Efficient message storage and retrieval
- Support for offline messages
- Push notifications with low latency
- Strong consistency for message ordering

---

## 29. Design Real-time Analytics Dashboard

**Frequency:** 42% | **Asked by:** Google (75%) • Amazon (60%) • Meta (55%) • Netflix (40%) • Apple (35%)

**Key Concepts:**
Consistent Hashing • Data Aggregation & Rollups • Time-Series Storage • Real-Time Stream Processing • Data Pipelines • Visualization Libraries • Caching Layers • Sampling Techniques • Alerting

**Functional Requirements:**
- Display real-time metrics and statistics
- Support multiple visualization types (charts, graphs, tables)
- Support filtering and drilling down
- Custom dashboard creation
- Alert on anomalies
- Export data in multiple formats
- Support for historical data comparison

**Non-Functional Requirements:**
- Real-time or near real-time updates (< 5s delay)
- Low latency for dashboard rendering (< 2s)
- High availability
- Support millions of concurrent viewers
- Efficient data aggregation
- Scalable storage for metrics history
- Support for multiple data sources

---

## 30. Design Parking Lot System

**Frequency:** 35% | **Asked by:** Amazon (45%) • Google (30%) • Meta (25%)

**Key Concepts:**
Consistent Hashing • Spatial Data Structures (Quadtree, Grid) • Inventory/Availability Tracking • Reservation System Design • Concurrency Control (Atomic Updates) • Payment Processing • Search Optimization

**Functional Requirements:**
- Users can search available parking spots
- Users can reserve/book parking spots
- Real-time availability of parking spaces
- Support multiple parking rates (hourly, daily, monthly)
- Process payments for parking
- Generate parking tickets/receipts
- Support multi-level parking structures
- Notifications for expiration reminders

**Non-Functional Requirements:**
- High availability for booking operations
- Real-time availability updates
- Highly scalable (support large parking lots)
- Support concurrent bookings
- Fair distribution of parking spaces
- Low latency for spot availability queries
- Support transaction consistency for payments

---

## 31. Design TikTok / Short Form Video Platform

**Frequency:** 32% | **Asked by:** Meta (30%) • Google (20%) • Amazon (15%)

**Key Concepts:**
Consistent Hashing • Recommendation Engines • Video Encoding/Compression • Adaptive Bitrate Streaming • CDN • Feed Ranking Algorithms (ML) • Real-Time Engagement Tracking • Search Indexing • User Profiling

**Functional Requirements:**
- Users can create, upload short videos
- Infinite feed with video recommendations
- Like, comment, share, duet, stitch features
- Search and discover content
- Follow creators
- Direct messaging
- Support hashtags and trending content
- Creator monetization (tips, ads)

**Non-Functional Requirements:**
- Ultra-low latency for feed (< 200ms)
- Support billions of videos and concurrent users
- AI-driven recommendation engine
- Real-time engagement tracking
- Global CDN distribution
- Video processing and compression
- High throughput for uploads
