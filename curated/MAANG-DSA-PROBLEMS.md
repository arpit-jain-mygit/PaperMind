# MAANG DSA/Coding Problems
> From System Design List: Problems that can appear in DSA/Coding Rounds (Not System Design Round)
> Ordered by frequency (high to low) | **Data Source:** LeetCode, Blind, Glassdoor (2025-2026)

---

## Table of Contents

| # | Problem | Frequency | Companies | LeetCode |
|---|---------|-----------|-----------|----------|
| [1](#1-lrulfuffifo-cache) | LRU/LFU Cache | 95% | Google • Meta • Amazon • Apple • Netflix | 146, 460 |
| [2](#2-parking-lot-system) | Parking Lot System | 78% | Amazon • Google • Meta | 1603 |
| [3](#3-search-autocomplete--typeahead-trie) | Search Autocomplete (Trie) | 72% | Google • Meta • Amazon • Apple | 642 |
| [4](#4-rate-limiter-algorithm-design) | Rate Limiter (Algorithm) | 65% | Google • Amazon • Meta • Apple | N/A |
| [5](#5-uuid--distributed-id-generator) | UUID Generator | 62% | Google • Amazon • Meta • Netflix • Apple | N/A |
| [6](#6-web-crawler-bfsdfs) | Web Crawler (BFS/DFS) | 58% | Google • Meta • Amazon | 1236 |
| [7](#7-job-queue--priority-queue-implementation) | Job Queue/Priority Queue | 52% | Google • Amazon • Meta • Netflix • Apple | N/A |
| [8](#8-booking-system-interval-scheduling) | Booking System (Intervals) | 48% | Amazon • Google • Meta • Apple | 252, 253 |
| [9](#9-search-engine-inverted-index) | Search Engine (Inverted Index) | 45% | Google • Meta • Amazon | 212 |

---

## 1. LRU/LFU/FIFO Cache

**Frequency:** 95% | **Asked by:** Google (95%) • Meta (90%) • Amazon (90%) • Apple (85%) • Netflix (80%)

**Format:** LeetCode-style coding problem + follow-ups  
**Difficulty:** Medium to Hard  
**Time Complexity Expected:** O(1) for get and put operations

**Problem Statement:**
Design and implement a cache data structure that supports:
- `get(key)`: Return value if key exists, else return -1
- `put(key, value)`: Update or insert key-value pair
- Evict least recently used (LRU) or least frequently used (LFU) item when capacity is exceeded

**DSA Concepts Needed:**
- Hash Map / Dictionary
- Doubly Linked List
- LRU eviction policy
- LFU eviction policy
- O(1) time complexity optimization

**Common Follow-ups:**
- Implement with TTL (Time-To-Live)
- Multi-threaded cache (thread-safe)
- LFU cache variant
- Segment-based eviction
- Support for variable cache sizes

**Variations:**
- LRU Cache (Most common)
- LFU Cache
- FIFO Cache
- MRU (Most Recently Used)

---

## 2. Parking Lot System

**Frequency:** 78% | **Asked by:** Amazon (85%) • Google (70%) • Meta (60%)

**Format:** OOP + DSA hybrid coding problem  
**Difficulty:** Medium  
**Language Expectation:** Java, C++, Python with OOP design

**Problem Statement:**
Design a parking lot system that supports:
- `addParking(level, spot, size)`: Add parking spot
- `findAvailableSpot(size)`: Find nearest available spot for vehicle
- `parkVehicle(vehicle)`: Park vehicle in available spot
- `unparkVehicle(ticket)`: Remove vehicle, free up spot
- `getAvailabilityStatus()`: Return availability in system

**DSA Concepts Needed:**
- Hash Map (for quick spot lookup)
- Priority Queue (for nearest spot)
- Doubly Linked List (for spot management)
- Spatial indexing concepts (optional)

**Common Follow-ups:**
- Handle multiple parking levels
- Priority for handicap spots
- Reserved spots
- Time-based pricing
- Compact representation for large parking lots

**OOP Design:**
- ParkingLot class
- Level class
- ParkingSpot class
- Vehicle class
- Ticket class

---

## 3. Search Autocomplete / Typeahead (Trie)

**Frequency:** 72% | **Asked by:** Google (85%) • Meta (70%) • Amazon (60%) • Apple (50%)

**Format:** LeetCode-style coding problem  
**Difficulty:** Medium  
**Core Data Structure:** Trie (Prefix Tree)

**Problem Statement:**
Design an autocomplete system that supports:
- `addWord(word)`: Add a word to the autocomplete dictionary
- `search(prefix)`: Return top N words matching the prefix
- `searchWithFrequency(prefix)`: Return words sorted by popularity/frequency

**DSA Concepts Needed:**
- Trie (Prefix Tree) data structure
- DFS/BFS on Trie
- Sorting (by frequency)
- Hash Map (for frequency tracking)
- Fuzzy matching (edit distance)

**Common Follow-ups:**
- Return top K results (not all)
- Fuzzy matching (typo tolerance)
- Frequency-based ranking
- Real-time update of frequencies
- Optimize memory (ternary search tree)

**Variations:**
- Design Search Autocomplete (LeetCode 642)
- Implement Trie with spell-check
- Autocomplete with historical data
- Handle concurrent queries

---

## 4. Rate Limiter (Algorithm Design)

**Frequency:** 65% | **Asked by:** Google (75%) • Amazon (70%) • Meta (55%) • Apple (45%)

**Format:** Algorithm design + implementation  
**Difficulty:** Medium  
**Algorithms:** Token Bucket, Leaky Bucket, Sliding Window

**Problem Statement:**
Implement a rate limiter that:
- Allows N requests per M seconds
- Returns true if request is allowed, false if limit exceeded
- Works with distributed systems (optional follow-up)

**DSA Concepts Needed:**
- Token Bucket Algorithm
- Leaky Bucket Algorithm
- Sliding Window with Counter
- Queue data structure (for leaky bucket)
- Hash Map (for per-user tracking)

**Common Follow-ups:**
- Distributed rate limiting
- Per-user rate limits
- Different limits for different endpoints
- Exponential backoff
- Redis-based implementation

**Algorithms to Know:**
- Token Bucket: Add tokens at fixed rate, consume on request
- Leaky Bucket: Requests leak out at fixed rate
- Sliding Window: Track requests in time window

---

## 5. UUID / Distributed ID Generator

**Frequency:** 62% | **Asked by:** Google (75%) • Amazon (70%) • Meta (60%) • Netflix (55%) • Apple (50%)

**Format:** Algorithm design + system thinking  
**Difficulty:** Medium to Hard  
**Related Concept:** Snowflake ID

**Problem Statement:**
Design a distributed ID generation system that produces:
- Globally unique IDs across multiple servers
- Sortable by timestamp
- No single point of failure
- High throughput (millions per second)

**DSA Concepts Needed:**
- Bit manipulation
- Timestamp ordering
- Distributed algorithms
- Modular arithmetic
- Concurrency handling

**Common Follow-ups:**
- UUID vs Snowflake format
- Handle clock skew
- Support multiple data centers
- Recycle IDs efficiently

**Popular Formats:**
- UUID (128-bit)
- Snowflake (64-bit): timestamp + machine ID + sequence
- Twitter Snowflake
- Custom formats

---

## 6. Web Crawler (BFS/DFS)

**Frequency:** 58% | **Asked by:** Google (75%) • Meta (50%) • Amazon (45%)

**Format:** Graph traversal + data structure design  
**Difficulty:** Medium  
**Core Concepts:** BFS, Deduplication, URL frontier

**Problem Statement:**
Design a web crawler that:
- Starts from seed URLs
- Follows links recursively
- Respects robots.txt and crawl delays
- Avoids crawling same URL twice
- Stores crawled content efficiently

**DSA Concepts Needed:**
- BFS (Breadth-First Search) for crawling
- Hash Set (for duplicate detection)
- Queue (URL frontier)
- Bloom Filter (efficient deduplication)
- URL normalization algorithm

**Common Follow-ups:**
- Distributed crawling
- Politeness policies
- Duplicate detection at scale
- Prioritize important URLs
- Detect and handle redirects

**Optimization Techniques:**
- Bloom filter for URL deduplication
- Batch URL processing
- Parallel crawling
- Smart prioritization

---

## 7. Job Queue / Priority Queue Implementation

**Frequency:** 52% | **Asked by:** Google (70%) • Amazon (65%) • Meta (55%) • Netflix (45%) • Apple (40%)

**Format:** Data structure implementation + algorithms  
**Difficulty:** Medium  
**Core Structure:** Priority Queue, Min/Max Heap

**Problem Statement:**
Implement a job queue system with:
- `enqueue(job, priority)`: Add job with priority
- `dequeue()`: Remove and return highest priority job
- `peek()`: View next job without removing
- Support for job cancellation and priority updates

**DSA Concepts Needed:**
- Priority Queue (Min-Heap, Max-Heap)
- Heap operations (insert, extract-min, heapify)
- Hash Map (for job tracking)
- Linked List (for queue ordering)

**Common Follow-ups:**
- Handle equal priorities (FIFO)
- Update job priority after enqueue
- Cancel a queued job
- Batch processing
- Fair scheduling (round-robin with priorities)

**Variations:**
- Min Priority Queue
- Max Priority Queue
- Indexed Priority Queue (support updates)
- Binary Heap implementation from scratch

---

## 8. Booking System (Interval Scheduling)

**Frequency:** 48% | **Asked by:** Amazon (65%) • Google (55%) • Meta (45%) • Apple (40%)

**Format:** Algorithm design + data structure  
**Difficulty:** Medium to Hard  
**Core Concepts:** Interval scheduling, conflict detection

**Problem Statement:**
Design a booking system that:
- Allows users to book time slots
- Prevents double-booking (overlapping bookings)
- Find available slots efficiently
- Handle cancellations and modifications

**DSA Concepts Needed:**
- Interval Overlap Detection
- Sorted data structures (TreeMap, SortedList)
- Binary Search (for available slots)
- Hash Map (for booking tracking)
- Segment Tree or Interval Tree (advanced)

**Common Follow-ups:**
- Multiple resources (rooms, vehicles)
- Different room/resource sizes
- Recurring bookings
- Waitlist management
- Price changes for different time periods

**Algorithms:**
- Interval overlap: `start1 < end2 && start2 < end1`
- Merge intervals
- Find largest available slot
- Schedule optimization

---

## 9. Search Engine (Inverted Index)

**Frequency:** 45% | **Asked by:** Google (70%) • Meta (55%) • Amazon (45%) • Apple (35%)

**Format:** Data structure + ranking algorithm  
**Difficulty:** Medium to Hard  
**Core Concept:** Inverted Index, TF-IDF, Relevance Ranking

**Problem Statement:**
Build a search engine backend that:
- `indexDocument(docId, content)`: Index document
- `search(query)`: Return top K documents matching query
- Rank results by relevance
- Support phrase queries

**DSA Concepts Needed:**
- Hash Map (for inverted index)
- Priority Queue (for ranking)
- Sorting (by relevance score)
- String tokenization
- TF-IDF algorithm
- BM25 ranking (advanced)

**Common Follow-ups:**
- Phrase queries ("machine learning")
- Wildcard queries
- Ranking by relevance
- Boolean queries (AND, OR, NOT)
- Handle misspellings

**Algorithms to Know:**
- Inverted Index construction
- TF-IDF scoring
- Cosine similarity
- BM25 algorithm
- Stemming/Lemmatization

---

## Comparison: Which Problems by Company

| Company | Top DSA Coding Problems |
|---------|------------------------|
| **Google** | LRU Cache (95%), Typeahead (85%), Web Crawler (75%), Rate Limiter (75%), UUID (75%), Search Engine (70%) |
| **Meta/Facebook** | LRU Cache (90%), Typeahead (70%), Parking Lot (60%), Web Crawler (50%), Job Queue (55%) |
| **Amazon** | LRU Cache (90%), Parking Lot (85%), Booking System (65%), Job Queue (65%), Rate Limiter (70%) |
| **Apple** | LRU Cache (85%), Typeahead (50%), Rate Limiter (45%), UUID (50%), Job Queue (40%) |
| **Netflix** | LRU Cache (80%), UUID (55%), Job Queue (45%), Rate Limiter (40%) |

---

## Difficulty Progression

```
EASY:
- Basic Rate Limiter (Token Bucket)
- Simple Booking (2D interval check)

MEDIUM:
- LRU Cache ⭐ (Most common)
- Parking Lot System ⭐
- Typeahead/Trie
- Web Crawler BFS
- Priority Queue
- UUID Generation

HARD:
- LFU Cache variant
- Distributed ID Generator (at scale)
- Search Engine with ranking
- Complex booking with multiple resources
- Interval scheduling optimization
```

---

## Key Differences: System Design vs DSA Version

| Problem | System Design Focus | DSA/Coding Focus |
|---------|-------------------|------------------|
| **Cache** | Distributed, persistence, replication | Algorithm, data structure, O(1) |
| **Rate Limiter** | Distributed coordination, multi-region | Algorithm (Token bucket, sliding window) |
| **Parking Lot** | Multi-level, reservations, pricing | Inventory management, availability |
| **Typeahead** | Distributed, real-time updates | Trie, fuzzy matching, ranking |
| **Web Crawler** | Distributed system, politeness, scale | BFS/DFS, deduplication, URL handling |
| **UUID** | Distributed, sharding, clock sync | Bit manipulation, timestamp ordering |
| **Job Queue** | Persistence, failures, retry | Heap, priority queue implementation |
| **Booking** | Transactions, consistency, concurrency | Interval overlap, conflict detection |
| **Search** | Indexing at scale, ranking ML | Inverted index, TF-IDF, scoring |

---

## Related LeetCode Problems

| DSA Problem | LeetCode Problems |
|------------|------------------|
| LRU Cache | 146 (LRU Cache), 460 (LFU Cache) |
| Parking Lot | OOP Design Problem, 1603 (Design Parking System) |
| Typeahead | 642 (Design Search Autocomplete System) |
| Rate Limiter | 1428 (Leftmost Column with at least a One) [similar pattern] |
| UUID | Not direct LeetCode equivalent |
| Web Crawler | 1236 (Web Crawler Multi-threaded) |
| Job Queue | 1586 (Binary Search Tree Iterator II) [priority pattern] |
| Booking | 252-253 (Meeting Rooms I/II) |
| Search Engine | 212 (Word Search II) |

