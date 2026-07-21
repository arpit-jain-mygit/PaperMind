# MAANG System Design - Key Concepts by Problem

> Essential concepts and building blocks needed for each system design problem
> Study these before attempting the design for each problem

---

## 1. Cache System (LRU/LFU Cache)

**Core Concepts:**
- Hash Map / Dictionary
- Doubly Linked List
- LRU eviction policy
- LFU eviction policy
- Time complexity analysis (O(1) operations)
- TTL (Time-To-Live) implementation
- Cache hit/miss rates
- Memory management

---

## 2. Recommendation System

**Core Concepts:**
- Collaborative Filtering (user-user, item-item)
- Content-Based Filtering
- Similarity metrics (Cosine, Euclidean, Jaccard)
- Matrix Factorization
- Feature engineering
- Cold Start Problem handling
- A/B Testing
- Ranking algorithms
- Hybrid recommendation approaches

---

## 3. Netflix / Video on Demand

**Core Concepts:**
- Video encoding/compression (H.264, VP9, AV1)
- Adaptive Bitrate Streaming (ABR)
- CDN architecture
- Manifest files (DASH, HLS)
- Caching strategies
- Database partitioning
- Load balancing
- Eventual consistency
- Subscription management models

---

## 4. YouTube / Video Streaming

**Core Concepts:**
- Video transcoding/processing pipeline
- Adaptive bitrate streaming
- CDN architecture & optimization
- Caching (cache hierarchy)
- Load balancing
- Database design (sharding, replication)
- Search indexing
- Recommendation algorithms
- Video deduplication

---

## 5. E-commerce Platform

**Core Concepts:**
- ACID transactions
- Database consistency (Strong, Eventual)
- Inventory management strategies
- Concurrency control
- CAP theorem
- Caching strategies
- Search engine indexing
- Payment gateway integration
- Rate limiting
- Database sharding

---

## 6. URL Shortener

**Core Concepts:**
- Base encoding (Base62, Base64)
- Hashing algorithms
- Distributed ID generation
- Database indexing (primary keys)
- Collision detection & handling
- TTL/Expiration mechanisms
- Consistency vs. Availability tradeoffs
- Analytics data collection
- Redirect strategies

---

## 7. Instagram / Photo Sharing

**Core Concepts:**
- Database sharding/partitioning strategies
- Denormalization techniques
- Cache-aside caching pattern
- CDN for image delivery
- Image compression/optimization
- Distributed database design
- Full-text search indexing
- Real-time notifications (pub/sub)
- Feed ranking algorithms

---

## 8. Twitter / Social Media Feed

**Core Concepts:**
- Push vs. Pull architecture (Fan-out)
- Denormalization in databases
- Cache-aside pattern
- Timeline ranking algorithms
- Eventual consistency
- Message queues (for event distribution)
- Search engine indexing
- Social graph database design
- Batch processing

---

## 9. Distributed Cache (Redis-like)

**Core Concepts:**
- In-memory data structures (String, List, Set, Hash, Sorted Set)
- Eviction policies (LRU, LFU)
- Persistence (RDB snapshots, AOF logs)
- Replication & failover
- Clustering & sharding
- Pub/Sub messaging
- Transactions (MULTI/EXEC)
- Memory management & optimization

---

## 10. Database (SQL/NoSQL)

**Core Concepts:**
- ACID properties
- CAP theorem
- Consistency models (Strong, Eventual, Causal)
- Indexing strategies (B-tree, LSM-tree, Hash)
- Query optimization
- Replication & failover
- Sharding/Partitioning strategies
- Transactions & locks
- Backup & recovery

---

## 11. Content Delivery Network (CDN)

**Core Concepts:**
- DNS resolution & geo-routing
- Edge server architecture
- Cache invalidation strategies
- Load balancing (Geographic, Performance)
- Anycast routing
- Cache replacement policies (LRU, LFU)
- Bandwidth optimization
- Latency measurement & monitoring
- Pull vs. Push model

---

## 12. Notification System

**Core Concepts:**
- Message queues (Kafka, RabbitMQ, SQS)
- Pub/Sub pattern
- Delivery semantics (At-least-once, At-most-once, Exactly-once)
- Retry mechanisms & exponential backoff
- Idempotency keys
- Rate limiting & throttling
- Multi-channel delivery (Push, Email, SMS)
- Batch processing
- Dead letter queues

---

## 13. News Feed / Timeline

**Core Concepts:**
- Database denormalization
- Push vs. Pull architecture (Fan-out)
- Timeline ranking algorithms (Recency, Engagement)
- Cache-aside pattern
- Eventual consistency
- Real-time updates (WebSocket, Long polling)
- Search indexing (Inverted index)
- Social graph representation
- Batch computation

---

## 14. Metrics / Analytics System

**Core Concepts:**
- Time-series database design
- Data aggregation functions (Sum, Avg, Percentile)
- Data compression techniques
- Sampling techniques
- Real-time stream processing (Kafka, Flink)
- Data retention policies
- Cardinality explosion handling
- Alerting & threshold mechanisms
- Query optimization for time-series

---

## 15. Messaging System (WhatsApp/Messenger)

**Core Concepts:**
- Message ordering guarantees
- Delivery semantics (At-least-once, Exactly-once)
- Message persistence (Database, Queue)
- Encryption (End-to-end, TLS)
- Data compression
- Acknowledge mechanisms (ACKs)
- Rate limiting per user
- Message indexing for search
- Presence tracking

---

## 16. Rate Limiter

**Core Concepts:**
- Token bucket algorithm
- Leaky bucket algorithm
- Sliding window technique
- Sliding window with counters
- Distributed rate limiting coordination
- Redis for state management
- Time-based windows (second, minute, hour, day)
- Quota allocation strategies
- Load distribution across servers

---

## 17. Distributed Unique ID Generator

**Core Concepts:**
- Distributed ID generation (Snowflake-like)
- Timestamp ordering
- Uniqueness guarantees across nodes
- Machine ID / Worker ID allocation
- Sequence number management
- Clock synchronization issues
- Collision avoidance strategies
- Scalability to millions of IDs/sec
- UUID vs. Custom ID formats

---

## 18. File Storage System (Dropbox)

**Core Concepts:**
- File system design (Hierarchical)
- Distributed storage architecture
- Replication & redundancy (RAID)
- File versioning strategies
- Delta sync (Block-level, Binary diff)
- Consistency models (Strong consistency needed)
- Sharding/Partitioning strategies
- Access control & permissions
- Conflict resolution

---

## 19. Search Engine (Elasticsearch)

**Core Concepts:**
- Inverted indexing
- Full-text search techniques
- Ranking algorithms (TF-IDF, PageRank, BM25)
- Query parsing & tokenization
- Stemming & lemmatization
- Faceted search
- Index compression
- Distributed indexing & search
- Relevance ranking

---

## 20. Search Autocomplete (Typeahead)

**Core Concepts:**
- Trie data structure (Prefix tree)
- Prefix-based searching
- Fuzzy matching (Levenshtein distance)
- Frequency tracking
- Suggestion ranking (Popularity, Personalization)
- Caching suggestions
- Real-time updates to trending queries
- Trie optimization (Ternary search tree)
- Autocomplete algorithms

---

## 21. Distributed Web Crawler

**Core Concepts:**
- URL frontier management (BFS, Priority queue)
- Politeness policies (robots.txt, crawl delays)
- DNS resolution
- Distributed crawling coordination
- Duplicate detection (Bloom filter, hash sets)
- URL normalization
- Metadata extraction
- Storage systems for crawled data
- Retry mechanisms & fault tolerance

---

## 22. Payment System

**Core Concepts:**
- ACID transactions & 2-Phase Commit
- Idempotency & idempotency keys
- Distributed transactions
- PCI compliance & security
- Encryption (AES, RSA)
- Secure communication (TLS)
- Ledger/Journal systems
- Reconciliation between systems
- Fraud detection systems

---

## 23. Job Queue / Task Scheduler

**Core Concepts:**
- Queue data structures (FIFO)
- Priority queues
- Task scheduling algorithms
- Cron-like scheduling
- Retry logic & exponential backoff
- Fault tolerance & recovery
- Distributed task execution
- Worker pool management
- State management & checkpoints

---

## 24. Uber / Ride Sharing

**Core Concepts:**
- Geospatial indexing (Quadtree, R-tree, KD-tree)
- Real-time location tracking (GPS)
- Matching algorithms (Hungarian algorithm concepts)
- ETA estimation models
- Surge pricing algorithms
- Database design for location data
- Message queues for real-time events
- Consistency models
- Concurrency control for bookings

---

## 25. API Gateway / Load Balancer

**Core Concepts:**
- Request routing algorithms (Round-robin, Least connections, IP hash)
- Load balancing strategies
- Circuit breaker pattern
- Rate limiting & throttling
- Request/Response transformation
- Authentication & Authorization (OAuth, JWT)
- Service discovery
- Monitoring, logging, tracing
- Health checks & failover

---

## 26. Booking System (Hotel/Airbnb)

**Core Concepts:**
- Inventory management
- Concurrency control (Locks, MVCC)
- ACID transactions
- Overbooking prevention
- Search index optimization (Date range searches)
- Caching strategies
- Consistency models (Strong consistency critical)
- Database design for time-based data
- Payment gateway integration

---

## 27. Chat System (Slack-like)

**Core Concepts:**
- Message ordering guarantees
- Delivery semantics
- Message queues & persistence
- WebSocket for real-time communication
- Long polling as fallback
- End-to-end encryption
- Database indexing for message search
- Presence tracking & status
- Consistency models for message ordering

---

## 28. Real-time Analytics Dashboard

**Core Concepts:**
- Data aggregation & rollups
- Time-series data storage
- Real-time stream processing (Kafka, Spark)
- Data pipelines
- Visualization libraries
- Caching layers (Redis)
- Sampling techniques
- Alerting & anomaly detection
- Query optimization for dashboards

---

## 29. Parking Lot System

**Core Concepts:**
- Spatial data structures (Quadtree, Grid-based indexing)
- Inventory/Availability tracking
- Reservation system design
- Concurrency control (Atomic updates)
- Payment processing
- Search optimization (Location-based queries)
- Real-time availability updates
- Consistency models
- Multi-level parking structure representation

---

## 30. TikTok / Short Form Video

**Core Concepts:**
- Recommendation engines (Collaborative filtering, Deep learning)
- Video encoding/compression
- Adaptive bitrate streaming
- CDN architecture
- Feed ranking algorithms (ML-based)
- Real-time engagement tracking
- Social features implementation (Like, Comment, Share)
- Search & discovery indexing
- Trending content detection
- User profiling & personalization

---

## Quick Reference: Topics Across All Problems

**Frequently Needed:**
- Database Design & Optimization
- Caching Strategies
- Data Structures & Algorithms
- Distributed Systems Concepts
- Consistency Models
- Load Balancing
- Sharding/Partitioning
- Message Queues & Pub/Sub
- Search & Indexing

**Medium Frequency:**
- Real-time Communication (WebSocket)
- Security & Encryption
- Payment Processing
- Replication & Failover
- Rate Limiting
- Monitoring & Logging

**Specific Domains:**
- Geospatial Indexing (Ride-sharing, Parking, Location)
- Machine Learning (Recommendations, Feed Ranking)
- Video Processing (YouTube, Netflix, TikTok)
- Search Algorithms (Autocomplete, Search Engine)
- Time-series (Metrics, Analytics)
