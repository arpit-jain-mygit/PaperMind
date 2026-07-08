# URL Shortener System Design

**Author:** Arpit Jain  
**Target:** MAANG System Design Interview  
**Status:** Revision 1  
**Last Updated:** 2026-07-08

---

## Revision 1: Initial Design

### 1. Functional Requirements

a) Generated short URLs should be collision free.

b) User should be able to provide a long URL and get short URL

c) Read:Write ratio is 9:1

---

### 2. Non-Functional Requirements

a) System should support 100M URL creation per day

b) System should respond within 2 secs

c) uptime, collision free, latency

---

### 3. DB Choice and Tables Design, Storage Requirements

**Choice of DB:** Key value store - Cassandra or DynamoDB. DB should store longURL as key and ShortURL as value with created_on as timestamp.

**Storage Calculation:**
```
Daily write request: 100M per day => 100*1000000 requests
Each write request will take longURL (70bytes), short URL (20 bytes), created_on (10 bytes)
Approximately: 100 bytes per request
In a day: 10 GB storage
Per year: 3650 GB
For 5 years: 18TB
With 3x redundancy: 54TB total
```

---

### 4. APIs Design

API1 - `/createURL`

API2 - `/getLongURL`

---

### 5. Algorithm to Generate Unique ID - Batch Design to Manage Unique IDs

This has multiple faces:

**a) Generate short URLs and store in file:**

System needs to generate 100M short URLs everyday. Our approach is to use 62 chars (A-Z, a-z, 0-9) in all possible combinations for alpha numeric ID with length 6. This can generate 62^6 (~56 Billion) unique URLs. To randomise/shuffle, unix sort job can be used and output containing these 56B IDs can be written in set of files. Each ID will require 6 bytes and to write 56B IDs, it will require 360GB size of files. IDs can be written in a file of 1 GB each and these files can be stored in Cloud buckets like AWS S3.

**b) Loading short URLs:**

Load these IDs from each of the S3 files to Kafka via a publisher (Python/Java program), Spark can be used to load all these files parallely and load to multiple kafka partitions for faster loading.

---

### 6. Logical Block Architecture

**a) Batch design to manage unique IDs:**

```
┌─────────────────────────────────────────────────────────────────────┐
│                      BATCH DESIGN FLOW                              │
│                                                                     │
│  ┌─────────────────────┐                                           │
│  │ Python program to   │                                           │
│  │ generate 56B ID in  │                                           │
│  │ Files (1 GB each)   │                                           │
│  │ Total 360 files     │                                           │
│  └──────────┬──────────┘                                           │
│             │                                                       │
│             ▼                                                       │
│  ┌─────────────────────┐                                           │
│  │ Store these files   │                                           │
│  │ in S3               │                                           │
│  └──────────┬──────────┘                                           │
│             │                                                       │
│             ▼                                                       │
│  ┌─────────────────────────────────────┐                           │
│  │ Write a spark program to load these  │                          │
│  │ files and for each, call Unix sort   │                          │
│  │ command to shuffle                  │                          │
│  └──────────┬──────────────────────────┘                           │
│             │                                                       │
│             ▼                                                       │
│  ┌──────────────────────┐                                          │
│  │ Reload shuffled files│                                          │
│  │ in S3                │                                          │
│  └──────────┬───────────┘                                          │
│             │                                                       │
│             ▼                                                       │
│  ┌──────────────────────────────┐                                  │
│  │ Load shuffled files using    │                                  │
│  │ Spark                        │                                  │
│  └──────────┬───────────────────┘                                  │
│             │                                                       │
│             ▼                                                       │
│  ┌──────────────────────────────────────────┐                      │
│  │ Spark loads these entries from each file │                      │
│  │ into Kafka partitions                    │                      │
│  └──────────┬───────────────────────────────┘                       │
│             │                                                       │
│             ▼                                                       │
│  ┌──────────────────────────────────────────┐                      │
│  │ Short URLs are ready to be mapped to     │                      │
│  │ long URLs on request                     │                      │
│  └──────────────────────────────────────────┘                      │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

**b) User request to convert long to short and redirect short URL to long URL:**

```
┌──────────────────────────────────────────────────────────────────────┐
│         USER REQUEST FLOW - WRITE & READ                             │
│                                                                      │
│                    User                                              │
│                      │                                               │
│                      ▼                                               │
│            ┌─────────────────┐                                       │
│            │   API Gateway   │                                       │
│            └────────┬────────┘                                       │
│                     │                                                │
│                     ▼                                                │
│            ┌─────────────────────┐                                   │
│            │   Load Balancer     │                                   │
│            └────────┬────────────┘                                   │
│                     │                                                │
│        ┌────────────┼────────────┐                                   │
│        │                         │                                   │
│  Long→Short                 Short→Long                               │
│  (Write)                    (Read)                                   │
│        │                         │                                   │
│        ▼                         ▼                                   │
│   ┌────────────────┐      ┌──────────────────┐                      │
│   │ Create Short   │      │ Redirect Short   │                      │
│   │ URLs App       │      │ URL to Long URL  │                      │
│   │ Server serving │      │ App Server       │                      │
│   │ Microservices  │      │ serving          │                      │
│   │ PODs using     │      │ Microservices    │                      │
│   │ Kubernetes     │      │ PODs using       │                      │
│   │                │      │ Kubernetes       │                      │
│   └────┬───────────┘      └────┬─────────────┘                      │
│        │                       │                                    │
│        ▼                       ▼                                    │
│   ┌──────────┐            ┌──────────┐                             │
│   │ NoSQL    │            │ Cache    │                             │
│   │ Database │            │ Cluster  │                             │
│   │ Cassandra│            │ (Redis)  │                             │
│   │/DynamoDB │            └──┬───────┘                             │
│   │          │               │                                     │
│   │ Long URL │         If not found                                │
│   │ Short URL│         in cache ↓                                  │
│   │ Created_ │        ┌────────────────┐                           │
│   │ on       │        │ Kafka          │                           │
│   └──────────┘        │ (If not found  │                           │
│                       │ in cache)      │                           │
│                       └────────────────┘                           │
│                                                                    │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Feedback on Revision 1

### ✅ Strengths

| Aspect | Your Approach | Assessment |
|--------|---------------|------------|
| Batch pre-generation | Sequential generation → shuffle → Kafka | Excellent! Avoids collisions and DB bottleneck |
| Capacity planning | 100 bytes/entry × 100M/day = 10GB/day = 54TB (5yr, 3x redundancy) | Math is correct ✓ |
| NoSQL choice | Cassandra or DynamoDB | Good for key-value workload ✓ |
| Architecture | Microservices on Kubernetes | Scalable design ✓ |
| Components | API Gateway, Load Balancer, Cache, DB, Kafka | All essential parts present ✓ |

---

### ❌ Critical Gaps (Missing for MAANG Interview)

| # | Gap | Why It Matters | Add to Next Revision |
|---|-----|---|---|
| 1 | **Code length decision (62^6 = 56B)** | 56B only covers 1.8 years at 100M/day. Interviewer asks "then what?" | Justify 6 chars OR upgrade to 62^7 (3.5T = 96 years). Plan for regeneration at 80% |
| 2 | **Cache strategy missing** | 9:1 read ratio demands caching! No TTL, hit rate, or layer strategy mentioned | Redis with LRU, 1-hour TTL, 99% hit target, multi-layer (browser→CDN→Redis→DB) |
| 3 | **Rate limiting not detailed** | No protection against abuse (100M URLs in 1 day from 1 user?) | API Gateway: 100 URLs/hour/IP, return 429 if exceeded |
| 4 | **No deduplication logic** | If User A and B shorten same URL, do they get different codes? | Check if long_url exists BEFORE assigning code |
| 5 | **301 vs 302 trade-off missing** | Classic MAANG question: which redirect type? Why? | Use 302 (track analytics). Explain why cache layer makes load acceptable |
| 6 | **Failure handling absent** | What if Kafka fails? Batch crashes? Cache down? | Add recovery plans: Kafka replicas, idempotent batch, DB fallback |
| 7 | **Sharding strategy unclear** | How to scale DB horizontally when codes run out? | Shard by short_url first character → distribute across nodes |
| 8 | **Monitoring/alerting missing** | No mention of KPIs, thresholds, or exhaustion alerts | Monitor queue depth, alert at 80%, trigger regeneration |
| 9 | **Batch job details vague** | How long does generation take? What if shuffle fails? | Specify SLA (6-10 hours), checkpoint for restarts, monitoring |
| 10 | **API specs incomplete** | Only endpoint names mentioned | Full specs: request/response format, error codes (400, 404, 429), headers |

---

### Summary Score

| Aspect | Score | Status |
|--------|-------|--------|
| Requirements understanding | A | Clear ✓ |
| Batch pre-generation | A | Solid approach ✓ |
| Diagrams & architecture | A | Good layout ✓ |
| Capacity math | A | Correct ✓ |
| Database & storage | A | Appropriate ✓ |
| **Caching strategy** | D | Only mentioned, not detailed |
| **Rate limiting** | D | Not explained |
| **Code length justification** | D | No reasoning given |
| **Deduplication** | D | Missing entirely |
| **301 vs 302** | F | Not discussed |
| **Failure handling** | F | Not addressed |
| **Monitoring** | F | No strategy |
| **API specifications** | C | Names only, no details |
| **Sharding** | D | Not detailed |

**Overall: 65-70% for MAANG → 85-90% with fixes**

---

## Ready for Revision 2

Address these 10 gaps in your next revision. Keep your design, add details! 🚀
