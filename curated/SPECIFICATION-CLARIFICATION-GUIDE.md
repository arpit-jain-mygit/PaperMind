# System Design Problems - Specification Clarification Guide

> Guide to understand overlapping/similar problems and their key differences

---

## Problems Requiring Clear Differentiation

### 1️⃣ Cache Problems

#### #1: Local/In-Process Cache System (LRU/LFU Cache) — 76%
- **Scope:** Single machine, in-memory, application-level
- **Focus:** Data structure implementation
- **Complexity:** Algorithm design (O(1) operations)
- **No Networking:** Direct memory access
- **Example:** Java's HashMap + LinkedList cache

#### #9: Distributed Cache System (Redis-like) — 65%
- **Scope:** Multi-machine, network-based, infrastructure-level
- **Focus:** Full system architecture
- **Complexity:** System design (persistence, replication, clustering)
- **Network-Based:** TCP/IP protocol, serialization
- **Example:** Redis production cluster

**Key Difference:** 
- #1 = "Implement a cache data structure"
- #9 = "Build a production-grade distributed cache service"

---

### 2️⃣ Feed/Timeline Problems

#### #8: Twitter / Social Media Feed — 65%
- **Platform:** Twitter-like social network
- **Focus:** User's personal timeline (posts from people you follow)
- **Architecture:** Push vs. Pull fan-out
- **Content:** User-generated posts, tweets, retweets
- **Key Challenge:** Timeline ranking by engagement

#### #13: News Feed / Timeline — 62%
- **Platform:** Facebook-like news feed
- **Focus:** User's personalized feed (not just followed users)
- **Architecture:** Push vs. Pull fan-out
- **Content:** Posts from friends, recommended content, ads
- **Key Challenge:** Timeline ranking by relevance + recency

**Key Difference:**
- #8 = "Design timeline showing posts from followed accounts"
- #13 = "Design personalized feed with recommendations + ranking"

**Note:** Both use similar architecture, but #13 adds personalization/ML ranking component.

---

### 3️⃣ Database Problems

#### #10: Distributed Database System (SQL/NoSQL) — 65%
- **Scope:** Designing database layer for large-scale systems
- **Focus:** ACID, consistency models, replication, sharding
- **Type:** SQL (PostgreSQL, MySQL) or NoSQL (MongoDB, Cassandra)
- **Key Aspect:** Schema design + scaling strategy
- **Design Level:** Which database to use + how to partition

#### Related Problems (Different Focus):
- **#26: Booking System** — Uses database but focuses on inventory + consistency
- **#22: Payment System** — Uses database but focuses on transactions + idempotency
- **#5: E-commerce** — Uses database but focuses on product catalog + inventory

**Key Difference:**
- #10 = "Design and scale a database system itself"
- Others = "Use databases as part of a larger system"

---

### 4️⃣ API Gateway / Load Balancer

#### #25: API Gateway & Load Balancer — 48%
- **Note:** These are often designed together in practice
- **API Gateway:** Handles request routing, rate limiting, auth, transformation
- **Load Balancer:** Distributes traffic to backend servers
- **Scope:** Sits between clients and backend services

**Why Combined:**
- Both handle request distribution
- Share concepts (routing algorithms, health checks)
- Often implemented as single infrastructure layer

---

### 5️⃣ Search Problems

#### #19: Search Engine (Elasticsearch-like) — 55%
- **Focus:** Indexing and searching documents
- **Scope:** Full-text search, ranking, relevance
- **Architecture:** Inverted indexes, sharding, distributed search
- **Use Case:** Google-like web search, log analysis

#### #20: Search Autocomplete (Typeahead) — 52%
- **Focus:** Real-time suggestions as user types
- **Scope:** Prefix-based matching, frequency tracking
- **Architecture:** Trie data structure, caching
- **Use Case:** Search bar autocomplete (Google, Wikipedia)

**Key Difference:**
- #19 = "Build a complete search engine"
- #20 = "Build autocomplete suggestions only"

---

### 6️⃣ Video Streaming Problems

#### #3: Netflix / Video on Demand (VOD) — 72%
- **Focus:** On-demand video watching (user chooses what to watch)
- **Architecture:** Browse catalog → Select video → Watch
- **Content:** Pre-recorded content (movies, shows)
- **User Experience:** Personalized recommendations, watch history, profiles

#### #4: YouTube / Video Streaming — 70%
- **Focus:** User-generated content streaming
- **Architecture:** Upload → Process → Stream → Discover
- **Content:** User-uploaded videos, channels, playlists
- **User Experience:** Search, recommendations, subscriptions, comments

#### #30: TikTok / Short Form Video — 32%
- **Focus:** Infinite feed of short videos with heavy AI recommendation
- **Architecture:** Upload → Process → Feed ranking → Discovery
- **Content:** User-generated short clips (15 sec - few min)
- **User Experience:** For-You-Page (AI-driven), viral content, social features

**Key Differences:**
| Aspect | Netflix | YouTube | TikTok |
|--------|---------|---------|--------|
| Content | Pre-recorded | User uploads | User uploads |
| Duration | Long (1-2+ hours) | Variable (1 min - hours) | Short (15 sec - 5 min) |
| Discovery | Browse catalog | Search + Subscriptions | AI feed (For-You-Page) |
| Model | Subscription | Ads + Subscription | Ads |
| Recommendation | Collaborative filtering | Content-based + ML | Deep Learning ranking |

---

### 7️⃣ Messaging/Chat Problems

#### #15: Messaging System (WhatsApp / Messenger) — 58%
- **Focus:** One-to-one and group text messaging
- **Scope:** Message delivery, ordering, encryption
- **Features:** Read receipts, typing indicators, media sharing
- **Requirements:** End-to-end encryption, offline messages

#### #27: Chat System (Slack-like) — 48%
- **Focus:** Team communication platform
- **Scope:** Channels, threads, history, integrations
- **Features:** Threading, channel management, search, reactions
- **Requirements:** Message persistence, channel organization

**Key Difference:**
- #15 = "Real-time personal messaging (WhatsApp)"
- #27 = "Enterprise team chat (Slack)"

---

### 8️⃣ Analytics Problems

#### #14: Metrics / Analytics System — 58%
- **Focus:** Collecting and storing metrics data
- **Architecture:** Metrics ingestion → Time-series storage → Query → Alert
- **Data:** Time-series data (CPU, latency, requests/sec)
- **Example:** Prometheus, InfluxDB

#### #28: Real-time Analytics Dashboard — 42%
- **Focus:** Visualizing analytics in real-time
- **Architecture:** Data aggregation → Dashboard UI → Real-time updates
- **Data:** Aggregated metrics, trends, anomalies
- **Example:** Grafana dashboards, business intelligence

**Key Difference:**
- #14 = "Build the backend for metrics collection/storage"
- #28 = "Build the frontend dashboard for visualization"

---

## Unique/Non-Overlapping Problems

These problems have **clear, distinct scope** and don't overlap:

✅ #2: Recommendation System  
✅ #5: E-commerce Platform  
✅ #6: URL Shortener  
✅ #7: Instagram  
✅ #11: CDN  
✅ #12: Notification System  
✅ #16: Rate Limiter  
✅ #17: UUID Generator  
✅ #18: File Storage  
✅ #21: Web Crawler  
✅ #22: Payment System  
✅ #23: Job Queue / Task Scheduler  
✅ #24: Uber / Ride Sharing  
✅ #26: Booking System  
✅ #29: Parking Lot System  

---

## Summary: Problems Needing Clarification

| # | Problem | Clarification | Differentiator |
|---|---------|---------------|-----------------|
| 1 vs 9 | Cache vs Distributed Cache | **CRITICAL** | Local vs Network-based |
| 3 vs 4 vs 30 | Netflix vs YouTube vs TikTok | Medium | Content length, discovery model |
| 8 vs 13 | Twitter Feed vs News Feed | Medium | Personal vs Personalized |
| 15 vs 27 | WhatsApp vs Slack | Low | Personal vs Enterprise |
| 19 vs 20 | Search Engine vs Autocomplete | Low | Full search vs Prefix-matching |
| 14 vs 28 | Metrics Backend vs Dashboard | Low | Storage vs Visualization |

---

## When Studying

1. **Study in this order** for related problems:
   - #1 (Local Cache) → #9 (Distributed Cache) — build from simple to complex
   - #19 (Search Engine) → #20 (Autocomplete) — autocomplete simpler
   - #14 (Metrics) → #28 (Dashboard) — metrics first, then visualization
   - #3 (Netflix) → #4 (YouTube) → #30 (TikTok) — increasing complexity

2. **Understand the differences** before designing:
   - Don't confuse #1 and #9 (vastly different scopes)
   - Don't confuse #8 and #13 (similar but different ranking needs)
   - Don't confuse #3, #4, #30 (different user experience expectations)

3. **Use this guide** as reference when:
   - Interviewer mentions a similar problem
   - You need to pivot to a different but related system
   - You want to understand the problem landscape
