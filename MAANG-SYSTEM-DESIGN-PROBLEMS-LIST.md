# MAANG System Design Problems List

> Ordered by frequency (high to low) | Backlog for system design practice
> **Data Source:** LeetCode, Blind, Glassdoor, IGotAnOffer, SystemDesignHandbook (2025-2026)

---

## Table of Contents

### 🔴 Highest Priority (70%+)
- [13. Cache System (LRU/LFU Cache)](#13-design-cache-system-lru--lfu-cache) — 76%
- [20. Recommendation System](#20-design-recommendation-system) — 73%
- [8. Netflix / Video on Demand](#8-design-netflix--video-on-demand-vod) — 72%
- [5. YouTube / Video Streaming](#5-design-youtube--video-streaming) — 70%
- [22. E-commerce Platform](#22-design-e-commerce-platform) — 68%
- [1. URL Shortener](#1-url-shortener) — 68%
- [6. Instagram / Photo Sharing](#6-design-instagram--photo-sharing-social-network) — 68%

### 🟠 High Priority (60-69%)
- [2. Twitter / Social Media Feed](#2-design-twitter--social-media-feed) — 65%
- [26. Distributed Cache (Redis-like)](#26-design-distributed-cache-redis-like) — 65%
- [28. Database (SQL/NoSQL)](#28-design-database-sql--nosql) — 65%
- [27. Content Delivery Network (CDN)](#27-design-content-delivery-network-cdn) — 62%
- [10. Notification System](#10-design-notification-system) — 62%
- [17. News Feed / Timeline](#17-design-news-feed--timeline) — 62%
- [24. Metrics / Analytics System](#24-design-metrics--analytics-system) — 58%
- [4. Messaging System](#4-design-messaging-system-whatsapp--facebook-messenger) — 58%
- [12. Rate Limiter](#12-design-rate-limiter) — 58%
- [25. Distributed Unique ID Generator](#25-design-distributed-unique-id-generator-uuid--snowflake) — 58%

### 🟡 Medium-High Priority (50-59%)
- [7. File Storage System](#7-design-file-storage-system-dropbox--google-drive) — 57%
- [29. Search Engine](#29-design-search-engine-elasticsearch-like) — 55%
- [14. Search Autocomplete (Typeahead)](#14-design-search-autocomplete-typeahead) — 52%
- [15. Distributed Web Crawler](#15-design-distributed-web-crawler) — 52%
- [19. Payment System](#19-design-payment-system) — 52%
- [23. Job Queue / Task Scheduler](#23-design-distributed-job-queue--task-scheduler) — 52%
- [3. Uber / Ride Sharing](#3-design-uber--ride-sharing) — 48%
- [16. API Gateway / Load Balancer](#16-design-api-gateway--load-balancer) — 48%
- [18. Booking System](#18-design-booking-system-hotel--airbnb--flight) — 48%
- [21. Chat System](#21-design-chat-system-slack-like) — 48%

### 🟢 Medium Priority (30-49%)
- [30. Real-time Analytics Dashboard](#30-design-real-time-analytics-dashboard) — 42%
- [11. Parking Lot System](#11-design-parking-lot-system) — 35%
- [9. TikTok / Short Form Video](#9-design-tiktok--short-form-video-platform) — 32%

---

## 1. URL Shortener

**Frequency:** 68% | **Asked by:** Meta (70%) • Google (65%) • Amazon (60%)

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

## 2. Design Twitter / Social Media Feed

**Frequency:** 65% | **Asked by:** Meta (90%) • Amazon (35%) • Google (40%)

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

## 3. Design Uber / Ride Sharing

**Frequency:** 48% | **Asked by:** Amazon (55%) • Google (50%) • Meta (35%)

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

## 4. Design Messaging System (WhatsApp / Facebook Messenger)

**Frequency:** 58% | **Asked by:** Meta (75%) • Apple (60%) • Amazon (40%) • Google (35%)

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

## 5. Design YouTube / Video Streaming

**Frequency:** 70% | **Asked by:** Google (75%) • Netflix (90%) • Amazon (40%) • Meta (55%)

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

## 6. Design Instagram / Photo Sharing Social Network

**Frequency:** 68% | **Asked by:** Meta (85%) • Google (50%) • Amazon (35%)

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

## 7. Design File Storage System (Dropbox / Google Drive)

**Frequency:** 57% | **Asked by:** Google (70%) • Apple (65%) • Amazon (50%) • Meta (45%)

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

## 8. Design Netflix / Video on Demand (VOD)

**Frequency:** 72% | **Asked by:** Netflix (95%) • Amazon (40%) • Google (35%)

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

## 9. Design TikTok / Short Form Video Platform

**Frequency:** 32% | **Asked by:** Meta (30%) • Google (20%) • Amazon (15%)

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

---

## 10. Design Notification System

**Frequency:** 62% | **Asked by:** Amazon (70%) • Google (60%) • Meta (55%) • Apple (50%) • Netflix (40%)

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

## 11. Design Parking Lot System

**Frequency:** 35% | **Asked by:** Amazon (45%) • Google (30%) • Meta (25%)

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

## 12. Design Rate Limiter

**Frequency:** 58% | **Asked by:** Google (70%) • Amazon (65%) • Meta (45%) • Apple (35%)

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

## 13. Design Cache System (LRU / LFU Cache)

**Frequency:** 76% | **Asked by:** Google (90%) • Meta (80%) • Amazon (75%) • Apple (70%) • Netflix (60%)

**Functional Requirements:**
- Get and put operations for cache
- Eviction policy when cache is full (LRU, LFU, FIFO)
- Support TTL (time-to-live) for cache entries
- Clear cache operation
- Cache statistics (hit/miss rate)

**Non-Functional Requirements:**
- O(1) time complexity for get and put operations
- Memory efficient
- Highly concurrent access
- Support for multi-threading/distributed cache
- Scalable to millions of entries

---

## 14. Design Search Autocomplete (Typeahead)

**Frequency:** 52% | **Asked by:** Google (85%) • Meta (50%) • Amazon (40%) • Apple (30%)

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

## 15. Design Distributed Web Crawler

**Frequency:** 52% | **Asked by:** Google (80%) • Meta (35%) • Amazon (30%)

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

## 16. Design API Gateway / Load Balancer

**Frequency:** 48% | **Asked by:** Google (65%) • Amazon (55%) • Meta (40%) • Apple (35%)

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

## 17. Design News Feed / Timeline

**Frequency:** 62% | **Asked by:** Meta (90%) • Amazon (35%) • Google (40%) • Apple (30%)

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

## 18. Design Booking System (Hotel / Airbnb / Flight)

**Frequency:** 48% | **Asked by:** Amazon (65%) • Google (45%) • Meta (35%) • Apple (30%)

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

## 19. Design Payment System

**Frequency:** 52% | **Asked by:** Amazon (75%) • Apple (70%) • Google (40%) • Meta (30%)

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

## 20. Design Recommendation System

**Frequency:** 73% | **Asked by:** Netflix (95%) • Amazon (80%) • Google (65%) • Meta (55%) • Apple (45%)

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

## 21. Design Chat System (Slack-like)

**Frequency:** 48% | **Asked by:** Meta (70%) • Apple (60%) • Amazon (40%) • Google (35%)

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

## 22. Design E-commerce Platform

**Frequency:** 68% | **Asked by:** Amazon (90%) • Google (40%) • Meta (30%) • Apple (35%)

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

## 23. Design Distributed Job Queue / Task Scheduler

**Frequency:** 52% | **Asked by:** Google (70%) • Amazon (65%) • Meta (45%) • Netflix (35%) • Apple (30%)

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

## 24. Design Metrics / Analytics System

**Frequency:** 58% | **Asked by:** Google (90%) • Amazon (75%) • Meta (65%) • Netflix (50%) • Apple (45%)

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

## 25. Design Distributed Unique ID Generator (UUID / Snowflake)

**Frequency:** 58% | **Asked by:** Google (75%) • Amazon (65%) • Meta (50%) • Netflix (40%) • Apple (35%)

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

## 26. Design Distributed Cache (Redis-like)

**Frequency:** 65% | **Asked by:** Google (85%) • Amazon (75%) • Meta (70%) • Netflix (60%) • Apple (55%)

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

## 27. Design Content Delivery Network (CDN)

**Frequency:** 62% | **Asked by:** Google (85%) • Netflix (80%) • Amazon (70%) • Meta (60%) • Apple (50%)

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

## 28. Design Database (SQL / NoSQL)

**Frequency:** 65% | **Asked by:** Google (80%) • Amazon (75%) • Meta (65%) • Netflix (50%) • Apple (60%)

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

## 29. Design Search Engine (Elasticsearch-like)

**Frequency:** 55% | **Asked by:** Google (90%) • Meta (45%) • Amazon (35%) • Apple (30%)

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

## 30. Design Real-time Analytics Dashboard

**Frequency:** 42% | **Asked by:** Google (75%) • Amazon (60%) • Meta (55%) • Netflix (40%) • Apple (35%)

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
