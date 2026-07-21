# Chubb Algorithm: End-to-End Logical Architecture

## High-Level System Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         OFFLINE BATCH (One-time)                        │
│                                                                          │
│  Code Generator → Shuffler → Kafka Loader → Exhaustion Monitor         │
│   (1 hour)       (1 hour)    (1 hour)      (continuous)                │
│                                                                          │
│  Output: Kafka Topic with 56.8B or 3.5T codes, randomly ordered       │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                      RUNTIME FLOW (Continuous)                          │
│                                                                          │
│    User               Load              Web Servers         Kafka       │
│  Shortens URL      Balancer          (Code Distributors)   Consumers    │
│      │                 │                      │                │        │
│      └────────────────→│──────────────────→  │───────→ Bulk read 100   │
│                        │                      │         codes at once   │
│                        │                      │                │        │
│                        │         ┌────────────┤←───────────────┘        │
│                        │         │            │                         │
│                        │         ↓        In-memory cache               │
│                        │    Deduplicate   of codes (local)              │
│                        │    Check DB      [xYz9aB, 2kLmOp, ...]        │
│                        │    (long URL)    │                             │
│                        │         │        │ Hand out 1 per request      │
│                        │         ↓        │                             │
│                        │    Generate      ↓                             │
│                        │    code from  Return code                      │
│                        │    cache      │                                │
│                        │         │      │                               │
│                        │         ↓      ↓                               │
│                        │      Save to Database (url_mapping)            │
│                        │      id, short_url, long_url                   │
│                        │                                                 │
│                        └─────→ Response: { shortUrl: ... }              │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Phase 1: Offline Batch Generation (One-Time Setup)

### Step 1.1: Generate All Possible Codes

**Component:** Code Generator Service

```
┌──────────────────────────────────────┐
│      Code Generator                  │
│                                      │
│  for i = 0 to 62^7 - 1:             │
│    code = base62_encode(i)          │
│    write_to_file(code)              │
│                                      │
│  Output: sequential-codes.txt        │
│  Size: ~3.5TB (uncompressed)        │
│  Time: 3-5 hours                    │
│  Format: One code per line          │
└──────────────────────────────────────┘

Output file (first few lines):
000000
000001
000002
000003
...
2TX
...
zzzzzz (last code)
```

**Why sequential?**
- Easy to generate (just iterate 0 to 62^7)
- Can split into batches and parallelize
- Can restart from checkpoint if it fails
- Pure math, no external dependencies

**Storage:** 
- Local SSD or object storage (S3)
- Can compress to ~1-2TB with gzip
- Access: High-bandwidth read access needed

---

### Step 1.2: Randomize the Codes

**Component:** Shuffle Service

```
┌──────────────────────────────────────┐
│    Shuffle Service                   │
│                                      │
│  $ sort -R sequential-codes.txt \    │
│    > randomized-codes.txt            │
│                                      │
│  Input:  sequential-codes.txt        │
│  Output: randomized-codes.txt        │
│  Time: 1-3 hours                    │
│  Memory: ~50GB RAM (for sorting)    │
└──────────────────────────────────────┘

Input (sequential):
000000
000001
000002
000003
000004
000005

Output (randomized):
000004
zn9edcu
2TX
000002
xYz9aB
000001
000003
000005
```

**Why shuffle?**
```
Without shuffle (sequential codes):
  User 1 gets: 000000
  User 2 gets: 000001
  User 3 gets: 000002
  
  Problem: Anyone can guess the next user's code!
  Prediction: User N gets code 000000 + N

With shuffle (random order):
  User 1 gets: zn9edcu
  User 2 gets: xYz9aB
  User 3 gets: 2TX
  
  Problem: IMPOSSIBLE to predict
  Prediction: Requires looking in Kafka queue (not possible)
```

---

### Step 1.3: Load Codes into Kafka

**Component:** Kafka Loader

```
┌────────────────────────────────────────────────┐
│         Kafka Loader                           │
│                                                │
│  1. Read randomized-codes.txt                 │
│  2. Partition by hash(code) % num_partitions  │
│  3. Produce to Kafka topic                    │
│                                                │
│  Input:  randomized-codes.txt (3.5TB)        │
│  Output: Kafka topic with 10-100 partitions  │
│  Time:   1-3 hours                           │
│  Rate:   ~1M codes/sec to Kafka              │
└────────────────────────────────────────────────┘

Kafka Topic: url-shortener-codes
Partitions: 10-100 (based on throughput needs)

                    ┌─────────────────┐
                    │  Kafka Topic    │
                    └────┬────┬────┬──┘
                    ┌────┘    │    └────┐
                    ↓         ↓         ↓
              ┌─────────┐ ┌─────────┐ ┌─────────┐
              │ Part. 0 │ │ Part. 1 │ │ Part. 2 │
              ├─────────┤ ├─────────┤ ├─────────┤
              │ xYz9aB  │ │ 2kLmOp  │ │ Q7rWvX  │
              │ Q7rWv   │ │ def456  │ │ ghi789  │
              │ abc123  │ │ jkl012  │ │ mno345  │
              │ ...     │ │ ...     │ │ ...     │
              └─────────┘ └─────────┘ └─────────┘

Each partition stores:
  - ~350 billion codes (for 10 partitions)
  - ~35 billion codes (for 100 partitions)
  - Log-structured, append-only, fault-tolerant
```

**Distribution strategy:**
```
Partition assignment:
  Code xYz9aB → hash("xYz9aB") % 10 = Partition 5
  Code 2kLmOp → hash("2kLmOp") % 10 = Partition 3
  Code Q7rWvX → hash("Q7rWvX") % 10 = Partition 8
  
Benefits:
  - Codes randomly distributed across partitions
  - Each partition can be consumed independently
  - Parallelization: 10-100 consumers reading in parallel
  - No hotspots (even distribution of requests)
```

---

## Phase 2: Runtime Flow (Continuous Production)

### Step 2.1: Web Server Receives Shortening Request

```
User Request:
  POST /api/v1/data/shorten
  { "longUrl": "https://wikipedia.org/wiki/Systems_design" }
  
Web Server Flow:
  ↓
1. Receive request
2. Validate input
3. Forward to Code Distributor
```

---

### Step 2.2: Code Distributor (Kafka Consumer)

**Component:** Code Distributor Service (runs on each web server or separately)

```
┌─────────────────────────────────────────────────────┐
│   Code Distributor Service                          │
│   (Consumer Group)                                  │
│                                                     │
│  ┌──────────────────────────────────────────────┐  │
│  │  Local In-Memory Cache                       │  │
│  │                                              │  │
│  │  codes = [                                   │  │
│  │    "xYz9aB",   ← serve next request         │  │
│  │    "2kLmOp",                                │  │
│  │    "Q7rWvX",                                │  │
│  │    ...                                       │  │
│  │    (98 more codes)                          │  │
│  │  ]                                           │  │
│  │  pointer = 0                                │  │
│  └──────────────────────────────────────────────┘  │
│           ↑         │                               │
│           │         │                               │
│   Bulk read when     │ Hand out 1 per request      │
│   pointer == 100     │                              │
│           │         ↓                               │
│  ┌────────────────────────────────────────────┐   │
│  │  When cache empty:                         │   │
│  │  Bulk consume 100 codes from Kafka        │   │
│  │  (1 call per 100 requests!)               │   │
│  │                                            │   │
│  │  kafka_consumer.poll(partition_X)         │   │
│  │  → [code1, code2, ..., code100]           │   │
│  └────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘

Performance:
  Without bulk read:   1000 req/sec → 1000 Kafka calls/sec
  With bulk read:      1000 req/sec → 10 Kafka calls/sec (100x fewer!)
  
  Latency per code:
    Without: 5ms (Kafka call overhead)
    With:    0.05ms (just memory pop)
```

**Code Distributor Instances:**
```
Web Server 1:          Web Server 2:         Web Server 3:
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│Code Distributor │   │Code Distributor │   │Code Distributor │
├─────────────────┤   ├─────────────────┤   ├─────────────────┤
│Consumer Group:  │   │Consumer Group:  │   │Consumer Group:  │
│url-shortener    │   │url-shortener    │   │url-shortener    │
├─────────────────┤   ├─────────────────┤   ├─────────────────┤
│Assigned:        │   │Assigned:        │   │Assigned:        │
│Part. 0, 3, 6, 9 │   │Part. 1, 4, 7    │   │Part. 2, 5, 8    │
└─────────────────┘   └─────────────────┘   └─────────────────┘
```

---

### Step 2.3: Deduplication Check

```
Web Server receives: https://wikipedia.org/wiki/Systems_design

├─ Check: Does this long_url already have a short code?
│
│  Query: SELECT short_url FROM url_mapping 
│          WHERE long_url = 'https://...'
│
├─ If YES (found in DB):
│  └─ Return existing short_url (no need for new code)
│
└─ If NO (not found):
   └─ Proceed to Step 2.4
```

---

### Step 2.4: Assign a Code from Cache

```
Code Distributor:

  pointer = 5
  cache = ["xYz9aB", "2kLmOp", "Q7rWvX", ...]
  
  // Get next code
  code = cache[pointer]           // → "xYz9aB"
  pointer++                       // → 6
  
  // Check if cache needs refill
  if pointer >= 100:
    bulk_read_next_100_from_kafka()
    pointer = 0
  
  Result: short_url = "xYz9aB"
  Latency: ~0.05ms (just array access!)
```

---

### Step 2.5: Save to Database

```
Database Operation:

  INSERT INTO url_mapping (short_url, long_url, created_at)
  VALUES ('xYz9aB', 'https://wikipedia.org/wiki/Systems_design', NOW())
  
  Kafka automatically removed 'xYz9aB' from queue
  (dequeuing = marking as "used")
  
  Result: One row in database
  Latency: ~10-20ms (network + disk write)
```

---

### Step 2.6: Return Response to User

```
Response:
  { 
    "shortUrl": "https://tinyurl.com/xYz9aB",
    "longUrl": "https://wikipedia.org/wiki/Systems_design",
    "createdAt": "2024-06-24T10:30:00Z"
  }

Total latency:
  0.05ms (code from cache) + 15ms (DB write) + network = ~20-30ms
```

---

## Phase 3: Monitoring & Regeneration

### Step 3.1: Continuous Exhaustion Monitoring

```
┌─────────────────────────────────────────┐
│   Exhaustion Monitor (Runs every hour)   │
│                                         │
│  while True:                            │
│    remaining = kafka.queue_depth()      │
│    total = 62^7                         │
│    percent_used = (total - remaining) / │
│                   total * 100           │
│                                         │
│    if percent_used > 80:                │
│      trigger_regeneration()             │
│      send_alert("80% exhausted!")       │
│                                         │
│    log_metrics(percent_used, remaining) │
│    sleep(3600)  # Check hourly         │
└─────────────────────────────────────────┘

Timeline example:
  At rate 1000 codes/sec:
    56.8B codes ÷ 1000/sec = 657 days (1.8 years)
    
  At rate 100K codes/sec:
    56.8B codes ÷ 100K/sec = 6.5 days
    
  At 80% exhaustion, trigger regeneration
    before hitting 100% (no service disruption)
```

---

### Step 3.2: Regeneration Pipeline (When 80% Exhausted)

```
┌───────────────────────────────────────────────┐
│  Regeneration Process (New batch generation)  │
│                                               │
│  1. Generate new batch (7-char or 8-char)   │
│  2. Shuffle randomly                         │
│  3. Load into NEW Kafka topic                │
│     (Keep old topic running!)                │
│                                               │
│  4. Gradual Switchover:                      │
│     ├─ New users: consume from NEW topic     │
│     ├─ Old users: still have old codes       │
│     └─ Both systems coexist (zero downtime!) │
│                                               │
│  5. After transition (1-2 weeks):            │
│     └─ Drain old topic completely           │
│     └─ Decommission old topic               │
└───────────────────────────────────────────────┘

Example: Expanding from 6-char to 7-char codes

  Before: 62^6 = 56.8 billion codes
  After:  62^7 = 3.5 trillion codes (62x more!)
  
  Timeline: 1.8 years × 62 = 111 years of runway!
```

---

## Complete Request-Response Cycle

```
Timeline of ONE request from start to finish:

t=0ms     User clicks: tinyurl.com/abc123
          ↓
t=1ms     Request hits load balancer
          ↓
t=2ms     Routed to Web Server
          ↓
t=3ms     Code Distributor:
          │ ├─ Pop from in-memory cache (array access)
          │ ├─ code = "xYz9aB"
          │ └─ Check if cache needs refill
          │    (if yes: bulk-read 100 from Kafka ~5ms)
          ↓
t=4ms     Deduplication check (SELECT 1 query)
          ├─ Cache or quick DB lookup
          └─ Determine if new code needed
          ↓
t=5ms     Save to database
          ├─ INSERT url_mapping
          ├─ Network: 5ms
          ├─ DB write: 10ms
          └─ Confirm: 1ms
          ↓
t=21ms    Response sent to user
          ├─ shortUrl: "tinyurl.com/xYz9aB"
          └─ Success!

Total: ~20ms (dominated by DB write, not code assignment!)
```

---

## System Component Dependencies

```
                    ┌──────────────────────┐
                    │   External User      │
                    └──────────┬───────────┘
                               │
                    ┌──────────▼──────────┐
                    │   Load Balancer     │
                    │  (Route traffic)    │
                    └──────────┬──────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        ↓                      ↓                      ↓
   ┌─────────┐          ┌─────────┐          ┌─────────┐
   │Web Srv 1│          │Web Srv 2│          │Web Srv N│
   └────┬────┘          └────┬────┘          └────┬────┘
        │                    │                    │
        └────────────┬───────┴────────┬──────────┘
                     ↓                ↓
          ┌──────────────────────────────────┐
          │   Code Distributors              │
          │   (Kafka Consumers)              │
          │   - In-memory cache: 100 codes   │
          │   - Bulk read from Kafka         │
          └──────────────────┬───────────────┘
                             │
           ┌─────────────────┼─────────────────┐
           ↓                 ↓                 ↓
      ┌──────────┐      ┌──────────┐      ┌──────────┐
      │Kafka P0  │      │Kafka P1  │      │Kafka PN  │
      │56.8B     │      │56.8B     │      │56.8B     │
      │codes     │      │codes     │      │codes     │
      └────┬─────┘      └────┬─────┘      └────┬─────┘
           │                 │                 │
           └─────────────────┼─────────────────┘
                             │
                      ┌──────▼──────┐
                      │  Database   │
                      │  (Master)   │
                      │ url_mapping │
                      │  (insert)   │
                      └──────┬──────┘
                             │
          ┌──────────────────┼──────────────────┐
          ↓                  ↓                  ↓
     ┌─────────┐        ┌─────────┐       ┌─────────┐
     │ Replica │        │ Replica │       │ Replica │
     │   1     │        │    2    │       │   N     │
     │ (reads) │        │ (reads) │       │ (reads) │
     └─────────┘        └─────────┘       └─────────┘
                             │
                             ↓
                  ┌──────────────────┐
                  │ Exhaustion       │
                  │ Monitor          │
                  │ (Continuous)     │
                  │ Check queue      │
                  │ Trigger regen @80%
                  └──────────────────┘
```

---

## Comparison: Chubb vs Base62 vs Hash+Collision

| Aspect | Hash + Collision | Base62 | Chubb (Pre-gen + Queue) |
|--------|-----------------|--------|---|
| **Code assignment** | Hash + check | ID generator | Kafka queue |
| **Collision handling** | DB lookup (expensive) | None (math) | None (pre-loaded) |
| **Latency per code** | 150-500ms | 1-5ms | 0.05ms |
| **DB lookups** | Multiple | 1 write | 1 write |
| **Kafka involved?** | No | No | Yes |
| **ID generator needed?** | No | Yes | No |
| **Throughput** | ~100 req/sec | 10K req/sec | 100K+ req/sec |
| **Setup time** | Hours | Days | Weeks |
| **Code predictability** | Hash-based | Somewhat sequential | Completely random |
| **Scalability** | Limited | Excellent | Extreme |

---

## Key Insights

1. **Offline batch job pays for throughput:**
   - Spend 5-10 hours generating codes once
   - Get 100K+ req/sec forever after
   - Regenerate only when exhausted (every 1-2 years)

2. **Bulk reading amortizes Kafka overhead:**
   - Without bulk: 1000 Kafka calls/sec
   - With bulk: 10 Kafka calls/sec (100x reduction!)
   - Latency per code: 5ms → 0.05ms

3. **Kafka guarantees atomicity:**
   - Code dequeue is atomic
   - No race conditions
   - No duplicate codes possible

4. **Decouples from ID generator:**
   - Base62 depends on Snowflake/UUID service
   - Chubb depends on Kafka (which is more reliable at scale)
   - Eliminates one critical dependency

5. **Best for:**
   - Payment systems (100K+ req/sec)
   - Referral codes (massive volume)
   - Session tokens (extreme scale)
   - When every millisecond matters
