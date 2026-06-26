# Data Engineering Leadership Interview Prep

> Goal: Crack Data Engineering leadership roles at Amazon, Microsoft, Google, Uber, Salesforce, Facebook.
> Approach: Start simple, build up gradually. Focus on large data volume problems.

---

## Table of Contents

- [The 3 Fundamental Questions](#the-3-fundamental-questions)
- [The Mental Model](#the-mental-model-data-factory)
- [Learning Path](#learning-path)
- [Fundamentals](#fundamentals)
  - [Fundamental 1 — Data Size](#fundamental-1--data-size-what-does-gb-actually-mean)
  - [Fundamental 2 — Speed](#fundamental-2--speed-what-does-real-time-actually-cost-you)
  - [Fundamental 3 — Storage](#fundamental-3--storage-where-does-data-live)
  - [Fundamental 4 — Processing](#fundamental-4--processing-how-is-data-transformed)
  - [Fundamental 5 — CAP Trade-off](#fundamental-5--the-cap-trade-off)
  - [Fundamental 6 — Use Cases](#fundamental-6--use-cases-what-does-each-consumer-need)
- [GB Scale Scenarios](#gb-scale-scenarios)
  - [Scenario 1 — GB + Real-time + Analytics](#scenario-1--gb--real-time--analytics)
  - [Scenario 2 — GB + Real-time + ML](#scenario-2--gb--real-time--ml)
  - [Scenario 3 — GB + Real-time + Dashboards](#scenario-3--gb--real-time--dashboards)
  - [Scenario 4 — GB + Real-time + Another System](#scenario-4--gb--real-time--another-system)
  - [Scenario 5 — GB + Near Real-time + Analytics](#scenario-5--gb--near-real-time--analytics)
  - [Scenario 6 — GB + Near Real-time + ML](#scenario-6--gb--near-real-time--ml)
  - [Scenario 7 — GB + Near Real-time + Dashboards](#scenario-7--gb--near-real-time--dashboards)
  - [Scenario 8 — GB + Near Real-time + Another System](#scenario-8--gb--near-real-time--another-system)
  - [Scenario 9 — GB + Batch + Analytics](#scenario-9--gb--batch--analytics)
  - [Scenario 10 — GB + Batch + ML](#scenario-10--gb--batch--ml)
  - [Scenario 11 — GB + Batch + Dashboards](#scenario-11--gb--batch--dashboards)
  - [Scenario 12 — GB + Batch + Another System](#scenario-12--gb--batch--another-system)

---

## The 3 Fundamental Questions (Always Ask These First)

When dealing with large data volumes, every solution starts with:

1. **How much data?** — Gigabytes? Terabytes? Petabytes?
2. **How fast does it need to be processed?** — Real-time (seconds)? Near real-time (minutes)? Batch (hours/daily)?
3. **What's it used for?** — Analytics? Machine learning? Dashboards? Another system?

These three questions determine everything about your architecture.

---

## The Mental Model (Data Factory)

```
Raw Data → [Collect] → [Store] → [Process] → [Serve]
                                                  ↓
                                    Dashboards / ML / Apps
```

Each stage has different problems at scale.

---

## Learning Path

| Stage | Topic | What You'll Learn |
|---|---|---|
| 1 | Storage basics | How data is stored at scale |
| 2 | Batch processing | Processing large files (MapReduce, Spark) |
| 3 | Data pipelines | Moving data reliably |
| 4 | Streaming | Real-time data at scale |
| 5 | System Design | Designing full systems end-to-end |
| 6 | Leadership layer | Trade-offs, org design, incident mgmt |

---

## Fundamentals

### Fundamental 1 — Data Size: What Does GB Actually Mean?

#### Start With Something Familiar

A single text message = ~1 KB (kilobyte)

```
1 KB  = 1 text message
1 MB  = 1,000 text messages  (or a small photo)
1 GB  = 1,000,000 text messages  (or a movie)
1 TB  = 1,000 GB  (or 1,000 movies)
1 PB  = 1,000 TB  (or 1,000,000 movies)
```

#### In Rows of Data

A typical data row (user_id, event, timestamp, country) = ~200 bytes

```
1 GB  =  ~5 million rows
1 TB  =  ~5 billion rows
1 PB  =  ~5 trillion rows
```

#### Real Company Examples

| Company | What they generate | Size |
|---|---|---|
| Small startup | User signups, orders | GB/day |
| Mid-size app | Clickstream, logs | TB/day |
| Amazon/Google/Meta | Everything combined | PB/day |

#### The Key Insight: Does GB Fit in One Machine?

Yes. That is the most important thing about GB scale.

```
A normal laptop    → 16 GB RAM
A cloud server     → 256 GB RAM
Your GB dataset    → fits comfortably
```

At GB scale:
- You do NOT need distributed systems
- One machine can handle it
- Solutions are simpler and cheaper

This changes completely at TB and PB.

#### The Real-World Smell Test

When someone says "we have a data problem" — your first question is always:

> "How many GB/TB/PB per day are we generating?"

This single answer tells you:
- Do we need one machine or many?
- What tools are appropriate?
- How complex will this be?

---

### Fundamental 2 — Speed: What Does Real-time Actually Cost You?

#### The Speed Spectrum

```
Real-time          Near Real-time        Batch
(milliseconds       (seconds to           (hours to
 to seconds)         minutes)              days)
     |___________________|___________________|
  Hardest                                Easiest
  Most expensive                    Least expensive
  Most complex                      Least complex
```

#### What Each Speed Means in Practice

**Real-time (milliseconds to seconds)**
- Data is processed as soon as it arrives
- Example: Fraud detection — block a transaction BEFORE it goes through
- Example: Live sports scores updating every second
- You need the answer NOW or it is useless

**Near Real-time (seconds to minutes)**
- Small acceptable delay, but still feels "live"
- Example: Ride-sharing app showing driver location every 10 seconds
- Example: Alerting system that pages oncall within 2 minutes of an error spike
- A small wait is okay, but hours is not

**Batch (hours to days)**
- Data is collected, then processed together in one big job
- Example: Generating yesterday's sales report every morning at 6am
- Example: Training an ML model once a week on last week's data
- Nobody needs this instantly — scheduled is fine

#### The Golden Rule of Speed

> The faster you need data processed, the more it costs — in money, complexity, and engineering effort.

| Speed | Complexity | Cost | Tools Typically Used |
|---|---|---|---|
| Real-time | High | High | Kafka, Flink, Kinesis |
| Near Real-time | Medium | Medium | Kafka, Spark Streaming |
| Batch | Low | Low | Spark, SQL, Airflow |

#### The Interview Trap

Most candidates jump straight to real-time solutions. Interviewers at big companies will push back:

> "Do you actually need real-time, or will 15-minute delay work?"

If 15-minute delay works — batch or near real-time is always the better answer. Simpler, cheaper, easier to maintain.

**Always justify your speed choice.**

---

### Fundamental 3 — Storage: Where Does Data Live?

#### The Simple Analogy

```
Your pocket          → RAM (fastest, tiny, expensive, disappears when power off)
Your desk            → Local disk (fast, medium size)
Your filing cabinet  → Database (organized, queryable)
A warehouse          → Object Storage like S3 (cheap, massive, not queryable directly)
```

#### The 4 Types You Must Know

**1. Database (PostgreSQL, MySQL)**
- Rows and columns, like Excel but powerful
- Great for: reading/writing individual records fast
- Bad for: reading millions of rows at once
- Example: "Get me user #12345's profile"

**2. Data Warehouse (Redshift, BigQuery, Snowflake)**
- Built specifically for analytics on large datasets
- Uses columnar storage internally
- Great for: "Give me total sales by country for last 3 months"
- Bad for: real-time inserts, frequent updates

**3. Object Storage (S3, GCS, Azure Blob)**
- Just files sitting in the cloud (CSV, Parquet, JSON)
- Extremely cheap, unlimited size
- Great for: storing raw data, backups, ML training data
- Bad for: querying directly (you need another tool on top)

**4. Message Queue (Kafka, Kinesis)**
- Not permanent storage — a temporary buffer
- Data flows through it like a pipe
- Great for: real-time data movement between systems
- Bad for: long-term storage or querying

#### Which Storage for Which Job?

| Situation | Use This |
|---|---|
| Save a user's order | Database |
| Run monthly sales report | Data Warehouse |
| Store raw logs cheaply | Object Storage (S3) |
| Move data between systems in real-time | Message Queue (Kafka) |

#### When to Use What — CAP Perspective

| Priority | Tool | Use When |
|---|---|---|
| Consistency (C) | PostgreSQL, MySQL | Wrong data causes real damage — payments, inventory, legal |
| Availability (A) | Cassandra, DynamoDB | Being down is worse than stale data — carts, likes, sessions |

Mental test: "What is worse — showing wrong data, or showing nothing at all?"
- Wrong data is worse → Consistency → PostgreSQL/MySQL
- Showing nothing is worse → Availability → Cassandra/DynamoDB

Note: RDBMS = C and NoSQL = A is a good starting rule, but MongoDB defaults to C and MySQL read replicas can serve stale data. The real answer depends on how the database handles network failures internally.

#### Storage at TB and PB Scale

**TB Scale:**
```
TB Data
  |
  ├── Transactional (lookup by key)     →  NoSQL (Cassandra, DynamoDB)
  ├── Analytical (GROUP BY, JOINs)      →  Data Warehouse (BigQuery, Snowflake, Redshift)
  └── Raw storage (cheap, keep all)     →  Object Storage (S3, GCS) + Athena/Spark on top
```

**PB Scale Transactional:**
- NoSQL (Bigtable, Cassandra, DynamoDB) — but partition key design is everything
- Used by: Google (Bigtable for Gmail/Maps), Meta (Cassandra for social graph), Amazon (DynamoDB for orders), Uber/Netflix (Cassandra for trip/viewing history)

#### Partition Key Design

At PB scale, data is split across thousands of machines. The partition key tells the system which machine owns which row.

```
Hash(partition_key) → Machine assignment
Query by partition_key → goes to exactly one machine → milliseconds
Query without partition_key → scans all machines → disaster
```

**Rules for good partition key design:**

1. **High cardinality** — must have many unique values so data spreads evenly
   - user_id (millions of values) ✓
   - country (200 values) ✗ → hot partition
   - boolean (2 values) ✗ → terrible

2. **Match your most frequent query pattern**
   - Most queries are "get by user_id" → partition key = user_id

3. **Avoid time-based keys alone**
   - All current writes pile onto today's machine → hot partition
   - Fix: combine with another field (user_id + date)

**Multiple query patterns → store data multiple times (materialized view pattern)**
```
Copy 1 — partitioned by user_id    → "Get orders for user #5432"
Copy 2 — partitioned by product_id → "Get orders for product #ABC"
Copy 3 — partitioned by date       → "Get all orders placed today"
```
Storage is cheap. Slow queries at PB scale are not acceptable.

**Uber example:** trip data stored 3 ways — by driver_id, by rider_id, by city+date — each serving a different access pattern instantly.

---

#### The GB Scale Insight

At GB scale, a regular database or even a single file often works fine. You do not need a data warehouse yet. This simplicity goes away at TB scale.

#### Columnar Storage — What Problem It Solves

For a query like `SELECT AVG(purchase_amount) FROM users` on 1 billion rows:

- **Row storage** reads every column of every row, then picks only purchase_amount — 5x more data than needed
- **Columnar storage** jumps directly to the purchase_amount column, skips everything else — 5x faster

Additionally, columnar storage compresses similar values together (e.g. numbers), giving 3-10x compression on top.

> Row storage is optimized for **finding one record**. Columnar storage is optimized for **computing across millions of records**.

#### When You Need Both: The Dual Storage Pattern

Common in e-commerce, ride-sharing, streaming — any app that needs:
- Fast individual record lookup (customer's order page) → Row storage
- Fast analytics across millions of rows (monthly revenue report) → Columnar storage

**Solution: store data twice, serve each from the right storage.**

```
Source Data
     |
     ├──→ Database (PostgreSQL/MySQL)              ← row-level queries
     └──→ Data Warehouse (BigQuery/Redshift)       ← analytical queries
```

A pipeline copies data from the database to the warehouse with a small delay (minutes to hours), which is acceptable since analytics does not need to be real-time.

| Company | Row Storage Used For | Columnar Storage Used For |
|---|---|---|
| Amazon | Order details page | Revenue reports, demand forecasting |
| Uber | Trip details | Surge pricing analysis, driver analytics |
| Netflix | Watch history page | Content popularity, recommendation model training |

**Modern alternative:** HTAP databases (Google AlloyDB, TiDB, SingleStore) store both formats internally and route queries automatically. Most big companies still prefer dual storage for independent scaling.

#### The Corrected OLTP vs OLAP Mental Model

```
                    OLTP                          OLAP
                (transactional)               (analytical)

Small scale   →  RDBMS (PostgreSQL/MySQL)   →  RDBMS (works for small analytics too)
Large scale   →  NoSQL                      →  Columnar DB
```

**NoSQL by access pattern:**

| Access Pattern | Type | Tools |
|---|---|---|
| Single record by ID, ultra-fast | Key-Value | Redis, DynamoDB |
| All records for an entity, high write volume, TB/PB | Wide-Column | Cassandra, HBase, Bigtable |
| Flexible nested structure, varied queries | Document | MongoDB, Firestore |
| Relationship traversal between entities | Graph | Neo4j, Neptune |

**Columnar by speed:**

```
Last few hours, speed critical   →  ClickHouse, Druid   (real-time + near real-time)
Historical, analyst queries      →  Snowflake, BigQuery  (near real-time + batch)
```

**Important:** Partition key concept belongs to NoSQL (Wide-Column), NOT columnar.

```
NoSQL partition key    →  routes query to ONE machine — MUST be in every query
Columnar partitioning  →  optimization hint for parallel reads — optional, any SQL works
```

#### Pure Columnar Databases

| Type | Tools | Notes |
|---|---|---|
| Cloud Data Warehouses | BigQuery, Redshift, Snowflake, Synapse | SQL interface, storage + compute bundled |
| Real-time Columnar | ClickHouse, Apache Druid | Speed-critical, recent data, sub-second queries |
| Open Source / On-Prem | DuckDB, Greenplum, Vertica | Local or enterprise use |
| Columnar File Formats | Parquet, ORC | Files on S3 — need Spark/Athena on top to query |

Key distinction:
- Parquet/ORC = columnar file format, stored in S3, needs compute engine on top
- BigQuery/Snowflake = columnar database, storage + compute bundled, just write SQL

Big companies often use both: raw data as Parquet in S3 → queried by Athena/Spark for ad-hoc work → also loaded into Snowflake for analyst SQL queries.

#### Complete Columnar Use Case — E-commerce (End to End)

**Business:** 50M users, 500 GB new data/day, 5 TB total. Business needs: revenue by category, cart abandonment by city, trending products, campaign performance, inactive users.

**The Full Architecture:**

```
[User Actions on App]
        ↓
[Event Collection - Kafka]  ← absorbs 50K events/sec, buffers spikes
        |
        ├──→ [Flink - Stream Processor]
        |           ↓
        |    [ClickHouse - Real-time Columnar]
        |           ↓ queried by:
        |           ├── Grafana dashboards (auto-refresh every 30s)
        |           ├── Alerting system (every 5 min: error rate? payment failures?)
        |           └── Backend APIs (trending products → cached in Redis → homepage)
        |
        └──→ [S3 - Raw Parquet files] (every 5 min)
                    ↓
             [Spark - Batch Processor] (hourly: clean, deduplicate, join)
                    ↓
             [Snowflake - Historical Columnar]
                    ↓ queried by:
                    └── Tableau/Looker (analysts, business teams, ad-hoc SQL)
```

**What queries what:**

| System | Queries | Latency | Use Case |
|---|---|---|---|
| Grafana | ClickHouse | <1 sec | Live dashboards during sale events |
| Alerting system | ClickHouse | <1 sec | Error/payment spike detection |
| Backend API | ClickHouse | <1 sec | "Trending Now" product feature |
| Tableau/Looker | Snowflake | 3-10 sec | Analyst reports, historical queries |

**All tools and their roles:**

| Tool | Role |
|---|---|
| Kafka | Collects events, buffers traffic spikes, feeds both pipelines |
| Flink | Cleans and enriches data in real-time from Kafka |
| ClickHouse | Real-time columnar — last few hours, speed-critical queries |
| S3 | Cheap raw storage (Parquet files) — source of truth |
| Spark | Batch cleaning, deduplication, joins — runs hourly |
| Snowflake | Historical columnar — all analyst queries, reports |
| Airflow | Schedules Spark jobs, monitors failures, retries |
| Tableau/Looker | BI tool — business users explore without writing SQL |
| dbt | Manages SQL transformations inside Snowflake, version controlled |
| Redis | Caches ClickHouse query results for homepage features |

**Feature Store + Batch Population (for ML inference):**

```
Raw Data (S3)
    ↓
Spark batch job (hourly) → computes features (txn_count_30d, avg_amount) → Feature Store (Redis/DynamoDB)
Flink stream job (always) → computes real-time features (txn_count_last_10min) → Feature Store

Inference request → fetch features from Feature Store (2ms) → model predicts → response
```

Lambda Architecture solves stale batch features: batch handles history, stream handles recency, model uses both.

---

### Fundamental 6 — Use Cases: What Does Each Consumer Need?

#### The 4 Consumers

**1. Analytics**
- Who: Data analysts, business teams
- Question: "What happened and why?"
- Needs: Historical data, aggregations (SUM/COUNT/AVG/GROUP BY), SQL interface, delay acceptable
- Best served by: Snowflake / BigQuery / Redshift

**2. Machine Learning**
- Who: Data scientists, ML engineers
- Question: "What will happen next?"
- Needs: Raw individual rows (not aggregated), large historical volume, clean schema
- Two phases:
  - Training → batch, slow okay → **S3 (Parquet) + Spark**
  - Inference → milliseconds → **Feature Store + Redis**

**Feature Store clarification:**
```
Feature Store = the CONCEPT (pre-computed features ready for inference)
Redis         = the IMPLEMENTATION (online storage layer, lives in RAM)

Feature Store layers:
  ├── Offline store   (S3/Snowflake — historical features for training)
  ├── Online store    (Redis/DynamoDB — real-time features for inference)
  └── Feature registry (catalog of features, ownership)

Read speeds:
  PostgreSQL  →  5-20ms   (too slow)
  DynamoDB    →  5-10ms   (acceptable at Amazon scale)
  Redis       →  0.1-1ms  (fastest, RAM-based)

Tools: Feast, Tecton, AWS SageMaker Feature Store (manage all three layers)
```

**3. Dashboards**
- Who: Executives, operations, product managers
- Question: "What is happening right now?"
- Needs: Pre-computed numbers, fast page load (<2 sec), auto-refresh, NOT raw data
- Real-time ops dashboards → **ClickHouse + Grafana**
- Business dashboards → **Snowflake + Tableau/Looker**

**4. Another System**
- Who: Other microservices, APIs
- Question: "Give me clean data I can act on immediately"
- Needs: Structured format, low latency, exactly-once delivery, stable schema
- Best served by: **Kafka (event-driven) or NoSQL (DynamoDB/Cassandra)**

#### Side by Side

| Consumer | Who | Question Type | Latency Need | Best Tool |
|---|---|---|---|---|
| Analytics | Analysts | What happened? | Hours okay | Snowflake/BigQuery |
| ML Training | Data Scientists | Pattern learning | Hours okay | S3 + Spark |
| ML Inference | ML Engineers | What will happen? | Milliseconds | Feature Store + Redis |
| Dashboards (ops) | Operations | What's happening now? | Seconds | ClickHouse + Grafana |
| Dashboards (business) | Executives | Business metrics | Minutes | Snowflake + Tableau |
| Another System | Engineers | Give me clean data | Milliseconds | Kafka / NoSQL |

#### Same Data, Different Architecture Per Consumer

Uber trip data — one dataset, six consumers:
```
Raw trip data
    |
    ├── Analytics team        → Snowflake   "Revenue by city last month"
    ├── ML training           → S3/Spark    "Train surge pricing model"
    ├── ML inference          → Redis       "Predict surge for this zone now"
    ├── Ops dashboard         → ClickHouse  "Active trips right now"
    ├── Executive dashboard   → Snowflake   "Weekly business metrics"
    └── Payment service       → Kafka       "Trip completed → charge rider"
```

#### The Leadership Insight

> Always ask "who is consuming this data?" before designing anything. Wrong answer: "I'll put everything in Snowflake." Right answer: "It depends on who needs it and how fast."

---

### All 6 Fundamentals — Summary

| Fundamental | Core Question | Key Takeaway |
|---|---|---|
| 1. Data Size | How much? | GB = one machine. TB/PB = many machines |
| 2. Speed | How fast? | Faster = more expensive. Always justify your choice |
| 3. Storage | Where? | Right storage for right job. Never one size fits all |
| 4. Processing | How transformed? | GB = SQL/Python. TB/PB = Spark distributed |
| 5. CAP Trade-off | C or A? | Wrong data worse → C. Going down worse → A |
| 6. Use Cases | Who consumes? | Consumer determines everything about your design |

---

## GB Scale Scenarios

### Scenario 1 — GB + Real-time + Analytics

**Situation:** 5 GB/day, ~25M events/day. Need answers in seconds.

**Example:** Food delivery startup — "Orders by city right now? Which restaurant is spiking? Is volume dropping suddenly?"

#### Analytics Only Architecture

```
[App] → [Kafka] → [Flink] → [ClickHouse] → [Grafana]
                                ↓
                          [S3 raw Parquet]  (parallel, for historical)
```

#### Why No RDBMS for Analytics

RDBMS is ruled out by use case, not size. At GB scale PostgreSQL handles the volume fine — but:
- Continuous aggregation queries (every 10 sec) = full table scan = CPU spikes
- Mixing OLTP and OLAP on same DB = analytical queries steal resources from order processing
- ClickHouse is purpose-built for this — same query in <1 sec vs minutes on PostgreSQL

#### Modified: OLTP + OLAP Together

Real systems need both — save orders (OLTP) AND analyze them (OLAP).

**The wrong approaches:**

```
App → Kafka directly (no PostgreSQL):
- Kafka is not a database — 7 day retention, no random reads
- Dual write risk: Kafka up but PostgreSQL down = payment charged, no order saved

App → Kafka → PostgreSQL:
- Async gap: customer sees "confirmed" before order actually saved
- Consumer crash = order lost forever
- Valid only for non-customer-facing async work
```

**The right approach: PostgreSQL → Debezium (CDC) → Kafka**

```
[Customer places order]
        ↓
[App] ──writes──→ [PostgreSQL]  ← single source of truth, synchronous commitment
                       │
                  WAL (automatic internal log)
                       │
                  [Debezium]   ← reads WAL after confirmed write, outside the app
                       │
                    [Kafka]    ← central event bus
                       │
       ┌───────────────┼───────────────┐
       ↓               ↓               ↓
[Payment Service] [Driver Service]  [Flink]
OLTP consumer     OLTP consumer        │
                                  [ClickHouse]
                                  OLAP store
                                       │
                                  [Grafana]
                                  Live dashboard
```

#### CDC — What It Is and Why It Matters

CDC = Change Data Capture. Captures every INSERT/UPDATE/DELETE from the database's own internal log.

```
PostgreSQL WAL (Write Ahead Log) — exists automatically for crash recovery
MySQL     → Binlog
Oracle    → Redo Log
```

Every change PostgreSQL makes is written to WAL first. Debezium reads this log and publishes to Kafka.

**CDC event example (order placed):**
```json
{
  "operation": "INSERT",
  "table": "orders",
  "timestamp": "2024-01-15 11:32:45",
  "new_data": { "order_id": 98765, "user_id": "USR-123", "amount": 450, "status": "placed" }
}
```

**What CDC gives you:**

| Property | How |
|---|---|
| App stays simple | App only knows PostgreSQL. New consumer = zero app changes |
| No dual write | CDC reads AFTER PostgreSQL confirms. Failed write = nothing published |
| Guaranteed ordering | WAL is strictly ordered. Downstream always sees: placed → preparing → delivered |
| No data loss on downtime | Kafka down 2 hours? Debezium catches up from WAL when it returns |

#### Tool Summary

| Tool | Role | Type |
|---|---|---|
| PostgreSQL | Stores orders, handles transactions | OLTP |
| Debezium | Reads WAL, publishes every change to Kafka | CDC |
| Kafka | Routes events to all consumers | Message bus |
| Payment Service | Charges customer | OLTP consumer |
| Driver Service | Assigns driver | OLTP consumer |
| Flink | Aggregates events in real-time (count by city, restaurant) | Stream processor |
| ClickHouse | Stores aggregated results, <1 sec queries | OLAP |
| Grafana | Live dashboard, auto-refreshes every 10 sec | Visualization |

#### GB Scale Reality

```
PostgreSQL  → 1 medium machine   ($50/month)
Kafka       → 1 small machine    ($30/month)
Flink       → 1 small machine    ($50/month)
ClickHouse  → 1 small machine    ($50/month)
Total: ~$200/month, managed by 1 engineer part-time
```

#### Interview Answer

*"Design real-time order processing AND analytics at GB scale"*
```
1. App writes orders to PostgreSQL (single source of truth)
2. Debezium CDC captures every change from WAL → publishes to Kafka
3. Kafka routes to: Payment Service, Driver Service, Flink
4. Flink aggregates every 10 sec → ClickHouse
5. Grafana queries ClickHouse → live ops dashboard
6. Raw events also land in S3 for historical analysis
7. Entire system runs on 4-5 small machines at GB scale
```

Key insight to mention: *"CDC via Debezium keeps PostgreSQL as single source of truth while feeding all downstream consumers — app writes once, everything else follows automatically."*

---

### Scenario 2 — GB + Real-time + ML

**Situation:** 5 GB/day, ~25M events/day. ML prediction needed in milliseconds alongside order processing.

**Example:** Food delivery startup — customer places order → app must simultaneously save the order (OLTP) AND predict delivery time (ML) before confirmation screen loads.

#### The Full Architecture

```
[Customer places order]
        ↓
[App] ──1. WRITE (HTTP)──→ [PostgreSQL]        ← saves order (OLTP)
  │                               │
  │                         READ (WAL)
  │                               │
  │                        [Debezium CDC]
  │                               │
  │                             WRITE
  │                               │
  │                           [Kafka] ──────WRITE──────→ [S3]
  │                               │                  raw Parquet files
  │                    READ       │       READ              │
  │              ┌────────────────┼────────────┐          READ
  │              ↓                ↓            ↓            │
  │        [Payment]          [Driver]      [Flink]      [Spark]
  │         Service            Service     state in      reads S3
  │          OLTP               OLTP        RAM           hourly
  │                                           │               │
  │                                         WRITE           WRITE
  │                                     real-time        historical
  │                                      features         features
  │                                           │               │
  │                                           ↓               ↓
  │                                    [Redis — Feature Store]
  │                                               │
  │                                             READ
  │                                               │
  └──2. REQUEST (HTTP)──→ [ML Inference Service]──┘
                                    │
                                  runs
                                    │
                               [ML Model]
                                    │
                   RESPONSE ←───────┘
                       │
              returns "28 min" → [App]
```

#### Two Parallel Calls From App

```
App makes 2 parallel calls simultaneously:

Call 1: WRITE → PostgreSQL          (saves order, ~20ms)
Call 2: REQUEST → ML Inference Service  (gets prediction, ~15ms)

App waits for BOTH → shows:
"Order confirmed! Arrives in 28 minutes"
```

App never touches Redis directly. ML Inference Service handles that internally.

#### Two Feature Pipelines Feed Redis

**Flink (stream, always running):**
- READs order events from Kafka continuously
- Maintains running state in RAM (+1 order placed, -1 order delivered)
- WRITEs real-time features to Redis every 30 seconds
- Features: active_orders_per_restaurant, active_drivers_per_zone, traffic_level

**Spark (batch, hourly):**
- READs raw Parquet files from S3
- Computes historical features from weeks of data
- WRITEs historical features to Redis
- Features: avg_prep_time, user_order_history, restaurant_rating

Both write to same Redis. ML model reads both at inference time — this is **Lambda Architecture**.

#### Why Flink Does Not Slow Down

Flink does NOT recount from scratch on every event. It keeps running state in RAM:

```
Event: order placed at Pizza Palace
Flink: state["Pizza Palace"] = current + 1   ← one addition, microseconds
Writes updated value to Redis every 30 sec
```

Not a full scan. Pure in-memory increment/decrement.

#### ML Inference Flow

```
1. App sends REQUEST to ML Inference Service
2. ML Service READs features from Redis     (1ms)
   - active_orders at Pizza Palace = 12     (from Flink)
   - avg_prep_time at Pizza Palace = 18min  (from Spark)
   - active_drivers in Zone A = 3           (from Flink)
3. ML Model runs prediction                 (10ms)
4. RESPONSE: "28 minutes" returned to App   (total ~15ms)
```

#### Why Not Kafka for ML Inference

```
Kafka is PUSH based — sends data when events arrive
ML Inference is PULL based — needs features for a specific user ON DEMAND

1000 concurrent orders → 1000 inference requests → each needs
specific features for specific user/restaurant/zone right now

Redis serves all 1000 point lookups in parallel in <1ms each
Kafka cannot answer "give me current state for Pizza Palace" on demand
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| PostgreSQL | Saves orders (OLTP, source of truth) | App WRITEs, Debezium READs WAL |
| Debezium | Captures WAL changes | READs WAL, WRITEs to Kafka |
| Kafka | Routes events to all consumers | Consumers READ from it |
| Payment Service | Charges customer | READs from Kafka |
| Driver Service | Assigns driver | READs from Kafka |
| S3 | Raw Parquet storage | Kafka WRITEs, Spark READs |
| Flink | Real-time feature computation | READs Kafka, WRITEs Redis |
| Spark | Historical feature computation | READs S3, WRITEs Redis |
| Redis | Feature Store online layer | Flink/Spark WRITE, ML Service READs |
| ML Inference Service | Runs model, returns prediction | READs Redis, RESPONDs to App |

#### GB Scale Reality

```
PostgreSQL          → 1 medium machine   ($50/month)
Kafka + Debezium    → 1 small machine    ($30/month)
Flink               → 1 small machine    ($50/month)
Spark (on-demand)   → runs hourly        ($20/month)
Redis               → 1 small machine    ($30/month)
ML Inference Service→ 1 small machine    ($50/month)
S3                  → GB scale           ($10/month)
Total: ~$240/month
```

#### Interview Answer

*"Design real-time ML prediction alongside order processing at GB scale"*
```
1. App makes two parallel calls:
   - WRITE to PostgreSQL (save order)
   - REQUEST to ML Inference Service (get prediction)
2. Debezium CDC captures WAL → publishes to Kafka
3. Kafka routes to: Payment Service, Driver Service, Flink
4. Kafka also WRITEs raw events to S3
5. Flink READs Kafka → maintains state in RAM → WRITEs real-time features to Redis
6. Spark READs S3 hourly → WRITEs historical features to Redis
7. ML Inference Service READs Redis → runs model → RESPONDs in ~15ms
8. App shows "Order confirmed! Arrives in 28 minutes"
```

Key insight: *"Lambda Architecture — Flink handles recency, Spark handles history, both write to Redis. ML model reads both at inference time for complete context. App never touches Redis directly."*

---

#### Lambda Architecture — Important Clarification

Lambda Architecture is a **data engineering design pattern**, coined by Nathan Marz in 2011. Nothing to do with AWS Lambda.

```
Named after Greek letter λ — two paths merging into one:

    Batch path  \
                 λ  → merged result at serving layer
    Speed path  /
```

| Term | What it is |
|---|---|
| Lambda Architecture | Design pattern — batch + stream pipelines merged at serving layer |
| AWS Lambda | Amazon's serverless compute product — run functions without managing servers |

In interviews always clarify which one is being asked about.

---

### Scenario 3 — GB + Real-time + Dashboards

**Quick Recap — Scenario 2 systems:**
```
PostgreSQL  → OLTP source of truth (synchronous, ACID)
Debezium    → CDC: reads WAL after confirmed write, publishes to Kafka (no dual write risk)
Kafka       → event bus (decouples producers from consumers)
Flink       → real-time features (stateful, RAM-based, fast)
Spark       → historical features (batch, reads S3 hourly)
Redis       → Feature Store online layer (sub-ms point lookups)
ML Service  → pulls from Redis, runs model, responds synchronously
```

---

**Situation:** 5 GB/day, ~25M events/day. Dashboards must show live data, refresh every 30 seconds, load in under 2 seconds.

**Example:** Food delivery startup ops team watching a live dashboard during a peak sale event:

```
"How many orders placed in last 5 minutes?"
"Which city is seeing order drop right now?"
"Is payment failure rate spiking?"
"Average delivery time right now vs yesterday same hour?"
```

These are dashboard questions — pre-computed, fast reads, auto-refreshing. Not ad-hoc analyst queries.

#### Two Types of Dashboards (Important Distinction)

```
Ops Dashboard (real-time):
- Who: Operations team, oncall engineers
- Question: "What is happening RIGHT NOW?"
- Latency: must refresh every 30 sec, load in <2 sec
- Data: last few hours only
- Tool: ClickHouse + Grafana

Business Dashboard (near real-time):
- Who: Executives, product managers
- Question: "How are we doing TODAY vs last week?"
- Latency: 15-30 min delay acceptable
- Data: days/weeks of history
- Tool: Snowflake + Tableau/Looker
```

Both need to coexist. Same underlying data, different consumers, different tools.

#### The Full Architecture

```
[Customer places order]
        ↓
[App] ──WRITE (HTTP)──→ [PostgreSQL]        ← saves order (OLTP)
                               │
                         READ (WAL)
                               │
                        [Debezium CDC]
                               │
                             WRITE
                               │
                           [Kafka] ──────WRITE──────→ [S3]
                               │                  raw Parquet files
                    READ       │       READ              │
              ┌────────────────┼────────────┐          READ
              ↓                ↓            ↓            │
        [Payment]          [Driver]      [Flink]      [Spark]
         Service            Service     state in      reads S3
          OLTP               OLTP        RAM           hourly
                                           │               │
                                         WRITE           WRITE
                                     aggregated       aggregated
                                      results          results
                                           │               │
                                           ↓               ↓
                                      [ClickHouse]    [Snowflake]
                                      last few hrs    weeks/months
                                           │               │
                                         READ            READ
                                           │               │
                                       [Grafana]    [Tableau/Looker]
                                      Ops Dashboard  Biz Dashboard
                                      auto-refresh   analyst queries
                                       every 30s     on-demand
```

#### What Flink Writes to ClickHouse

Flink does not write raw events. It writes **pre-aggregated results** every 30 seconds:

```
{
  window:        "last 5 minutes",
  city:          "Mumbai",
  orders_count:  847,
  avg_delivery:  "32 min",
  payment_failures: 3,
  timestamp:     "2024-01-15 11:30:00"
}
```

Grafana reads these pre-computed rows — no aggregation at query time. That is why it loads in <2 seconds.

#### What Spark Writes to Snowflake

Spark runs hourly, reads S3, cleans and joins data:

```
Reads:  raw order events from S3
Joins:  with user table, restaurant table, driver table
Writes: clean fact table to Snowflake

Result: analysts can run any SQL without worrying about data quality
```

#### Why Not Use Snowflake for Ops Dashboard

```
Grafana queries Snowflake every 30 seconds:
→ Snowflake cold query = 3-10 seconds minimum
→ Dashboard always shows stale data
→ During sale event with 100 analysts also querying = contention
→ Cost: Snowflake charges per query compute

ClickHouse:
→ query on pre-aggregated rows = <100ms
→ dedicated for dashboard only, no contention
→ Cost: fixed small machine ~$50/month
```

#### Why Not Use ClickHouse for Business Dashboard

```
Business analyst query:
"Show me revenue by restaurant category,
 compare last 4 weeks, broken down by city,
 only for orders above $500"

ClickHouse: holds only last few hours of data
→ cannot answer multi-week historical queries
→ no joins across user/restaurant/driver tables

Snowflake: holds months of clean joined data
→ answers this in 5-8 seconds
→ purpose built for this
```

#### Grafana vs Tableau — Key Difference

```
Grafana:
- Connects to ClickHouse
- Pre-built time-series panels
- Auto-refreshes automatically
- Used by: engineers, ops team
- Good for: "is something broken right now?"

Tableau/Looker:
- Connects to Snowflake
- Drag and drop exploration
- Manual refresh, on-demand
- Used by: business teams, executives
- Good for: "why did revenue drop last Tuesday?"
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| PostgreSQL | Saves orders (OLTP) | App WRITEs, Debezium READs WAL |
| Debezium | Captures WAL changes | READs WAL, WRITEs to Kafka |
| Kafka | Routes events to all consumers | Consumers READ |
| Payment/Driver Service | OLTP consumers | READ from Kafka |
| S3 | Raw Parquet storage | Kafka WRITEs, Spark READs |
| Flink | Real-time aggregation | READs Kafka, WRITEs ClickHouse |
| Spark | Historical aggregation + cleaning | READs S3, WRITEs Snowflake |
| ClickHouse | Real-time dashboard store | Flink WRITEs, Grafana READs |
| Snowflake | Historical dashboard store | Spark WRITEs, Tableau READs |
| Grafana | Ops dashboard (auto-refresh 30s) | READs ClickHouse |
| Tableau/Looker | Business dashboard (on-demand) | READs Snowflake |

#### GB Scale Reality

```
PostgreSQL       → 1 medium machine   ($50/month)
Kafka + Debezium → 1 small machine    ($30/month)
Flink            → 1 small machine    ($50/month)
ClickHouse       → 1 small machine    ($50/month)
Spark (on-demand)→ runs hourly        ($20/month)
Snowflake        → pay per query      ($50/month at GB scale)
S3               → GB scale           ($10/month)
Total: ~$260/month
```

#### Interview Answer

*"Design real-time + business dashboards alongside order processing at GB scale"*

```
1. App WRITEs to PostgreSQL (OLTP)
2. Debezium CDC → Kafka → Payment/Driver services
3. Kafka → Flink → pre-aggregated results → ClickHouse
4. Grafana READs ClickHouse every 30s → ops team sees live view
5. Kafka → S3 (raw) → Spark hourly → clean joined data → Snowflake
6. Tableau READs Snowflake → business team explores history
7. Two dashboards, two tools, same underlying data
```

Key insight: *"Never use one tool for both dashboards. ClickHouse for real-time ops — cheap, fast, last few hours only. Snowflake for business history — flexible SQL, weeks of data. Each tool does one job well."*

---

---

## Reference — Tool Stacks and Counterparts

### The Two Standard Stacks

```
SPEED STACK                     SCALE STACK
───────────                     ───────────
Kafka                           Kafka
  ↓                               ↓
Flink                           S3
  ↓                               ↓
ClickHouse/Druid/Pinot          Spark
  ↓                               ↓
Grafana / Product UI            Snowflake/BigQuery/Redshift
                                  ↓
                                Tableau / Looker
```

Same Kafka feeds both. Everything else is parallel.

### Speed vs Scale Counterparts

| Layer | Speed Counterpart | Scale Counterpart |
|---|---|---|
| Processing | Flink | Spark |
| Storage/Query | ClickHouse / Druid / Pinot | Snowflake / BigQuery / Redshift |
| Raw Storage | Kafka (temporary buffer, 7 days) | S3 / GCS (permanent, cheap) |
| Serving Layer | Redis (key lookup, sub-ms) | PostgreSQL / Cassandra (durable) |

### Processing Layer

```
Flink                           Spark
─────                           ─────
Event by event                  Batch (files/micro-batch)
Millisecond latency             Minutes to hours latency
Always running                  Runs on schedule
Stateful (RAM + RocksDB)        Stateless (reads fresh each run)
Complex to operate              Simpler to operate
Use: real-time aggregations     Use: historical ETL, ML training
```

### Storage/Query Layer

```
ClickHouse / Druid / Pinot      Snowflake / BigQuery / Redshift
──────────────────────────      ───────────────────────────────
Last hours of data              Weeks/months of data
Sub-second queries              Seconds to minutes
Fixed machine cost              Pay per query/compute
Dedicated purpose               Shared across teams
Use: ops dashboards, alerts     Use: analyst queries, reports

Within speed:
  ClickHouse  →  GB-TB, internal dashboards, simple setup
  Druid       →  TB-PB, internal dashboards, multi-tenant
  Pinot       →  TB-PB, user-facing product analytics (LinkedIn, Uber)

Within scale:
  Snowflake   →  most popular, elastic compute, multi-cloud
  BigQuery    →  Google ecosystem, serverless, pay per query
  Redshift    →  AWS ecosystem
```

### Tool Deep Dive

| Tool | Type | Best For | Latency | Scale |
|---|---|---|---|---|
| Flink | Stream processor | Transforming/aggregating events continuously | ms | GB-PB |
| Spark | Batch processor | Historical ETL, ML training, cleaning | minutes | GB-PB |
| ClickHouse | Real-time columnar DB | Ops dashboards, recent data, simple setup | <100ms | GB-TB |
| Druid | Real-time columnar DB | Multi-tenant dashboards at scale | <1 sec | TB-PB |
| Pinot | Real-time columnar DB | User-facing product analytics | <100ms | TB-PB |
| Snowflake | Cloud data warehouse | Historical analyst queries, ad-hoc SQL | 3-30 sec | TB-PB |
| Redis | In-memory key-value | ML inference, session, caching, rate limiting | <1ms | GB |
| Kafka | Message queue | Event ingestion, decoupling producers/consumers | ms | GB-PB |
| S3 | Object storage | Raw durable storage, ML training data | N/A | unlimited |

### When to Choose Which Real-time Store

```
Internal ops dashboard, GB-TB, simple:         Flink + ClickHouse
Internal ops dashboard, TB-PB, multi-tenant:   Flink + Druid
User-facing analytics in product:              Flink + Pinot
Historical analyst queries:                    Spark + Snowflake
ML inference serving:                          Flink/Spark + Redis
All of the above (mature platform):            Kafka feeds all
```

---

### Columnar File Formats — Parquet and ORC

#### What Is a File Format?

How data is physically arranged on disk/S3. Determines read speed, file size, and tool compatibility.

```
.csv     → row format, plain text, no compression (baseline)
.parquet → columnar format, compressed, fast for analytics
.orc     → columnar format, compressed, Hadoop ecosystem
```

#### How Parquet Stores Data

```
CSV (row format on disk):
[1,Mumbai,450,delivered] [2,Delhi,200,placed] [3,Bangalore,800,delivered]

Parquet (columnar on disk):
order_id column:  [1, 2, 3]
city column:      [Mumbai, Delhi, Bangalore]
amount column:    [450, 200, 800]
status column:    [delivered, placed, delivered]
```

Parquet splits files into **Row Groups**, each with a footer storing min/max stats per column:

```
Query: WHERE amount > 1000

Footer says:
  Row Group 1: amount min=50,  max=900   → SKIP entirely
  Row Group 2: amount min=200, max=5000  → READ

= skips entire row groups without reading them (predicate pushdown)
```

#### Compression

Similar values stored together compress extremely well:

```
status column: [delivered, delivered, placed, delivered, placed]
→ Run-length encoding: (delivered,3),(placed,1),(delivered,1)
→ 60-70% smaller

Result:
CSV:     1 GB
Parquet: 100-200 MB  (5-10x smaller)
```

#### Parquet vs ORC

| Feature | Parquet | ORC |
|---|---|---|
| Created for | Spark, general purpose | Hive, Hadoop ecosystem |
| Ecosystem | Spark, Flink, Athena, BigQuery, Snowflake | Hive, Spark, Presto |
| Adoption | More widely used today | Common in older Hadoop stacks |
| Default choice | Yes | Only if using Hive/Hadoop |

**Rule:** If unsure → always choose Parquet.

#### How Parquet Fits in Architecture

```
[Kafka] ──WRITE──→ [S3]                    ← raw JSON (large, slow)
                      │
                    READ
                      │
                   [Spark]                  ← cleans, converts format
                      │
                    WRITE
                      │
                   [S3]                     ← clean Parquet (small, fast)
                      │
                    READ
                      │
          [Snowflake / Athena / Spark]      ← queries only needed columns
```

#### Why Not Store as JSON in S3?

```
Athena charges per byte scanned:
JSON query on 5 GB:     reads 5 GB   → $25/query
Parquet query on 5 GB:  reads 200 MB → $1/query

Speed: Parquet is 10-50x faster for analytical queries
```

#### No Primary Key in Columnar Storage

Columnar storage never looks up individual rows — it scans entire columns. No primary key needed.

Speed comes from:
- Physical column separation (read only needed columns)
- Predicate pushdown (skip row groups via min/max stats)
- Compression (less data to read from disk)

ClickHouse uses ORDER BY for physical sorting instead of primary key:
```sql
CREATE TABLE orders (order_id UInt64, city String, amount Float64)
ORDER BY (city, created_at)   -- no primary key, just sort order
```

---

### Tool Alternatives

| Tool | Category | Alternatives | Key Difference |
|---|---|---|---|
| **Kafka** | Message Queue | AWS Kinesis, Google Pub/Sub, Azure Event Hubs, RabbitMQ, Pulsar | Kinesis = AWS native, no ops. Pub/Sub = Google native. Pulsar = multi-tenancy. RabbitMQ = simpler, smaller scale |
| **Flink** | Stream Processor | Spark Streaming, AWS Kinesis Data Analytics, Google Dataflow, Kafka Streams | Spark Streaming = micro-batch not true streaming. Dataflow = Google managed Flink/Beam. Kafka Streams = lightweight, no cluster needed |
| **Spark** | Batch Processor | Databricks (managed Spark), AWS Glue, Google Dataproc, Hive | Databricks = best Spark experience. Glue = serverless AWS. Hive = older, slower, Hadoop era |
| **ClickHouse** | Real-time Columnar DB | Apache Druid, Apache Pinot, TimescaleDB | Druid = multi-tenant TB scale. Pinot = user-facing analytics. TimescaleDB = time-series on PostgreSQL |
| **Redis** | In-memory Key-Value | Memcached, AWS ElastiCache, DragonflyDB | Memcached = simpler, no persistence. ElastiCache = managed Redis on AWS. DragonflyDB = faster Redis alternative |
| **Snowflake** | Cloud Data Warehouse | BigQuery, Redshift, Databricks SQL, Synapse, Athena | BigQuery = serverless Google. Redshift = AWS native. Athena = query S3 directly, no warehouse needed |
| **Redshift** | Cloud Data Warehouse | Snowflake, BigQuery, Synapse | Snowflake = multi-cloud. BigQuery = serverless. Synapse = Azure |
| **S3** | Object Storage | GCS (Google), Azure Blob, HDFS | GCS = Google ecosystem. Azure Blob = Microsoft. HDFS = on-premise Hadoop (legacy) |
| **Debezium** | CDC Tool | AWS DMS, Oracle GoldenGate, Attunity, Fivetran | DMS = AWS managed CDC. GoldenGate = Oracle enterprise. Fivetran = SaaS, no-code connectors |
| **Airflow** | Pipeline Orchestrator | Prefect, Dagster, AWS Step Functions, Google Cloud Composer | Prefect/Dagster = modern Python-native. Step Functions = AWS serverless. Composer = managed Airflow on GCP |
| **Grafana** | Visualization (ops) | Kibana, Datadog, New Relic | Kibana = Elasticsearch ecosystem. Datadog/New Relic = paid, full observability platforms |
| **Tableau/Looker** | Visualization (business) | Power BI, Metabase, Superset | Power BI = Microsoft ecosystem. Metabase = simple, open source. Superset = open source Looker alternative |
| **dbt** | Data Transformation | SQLMesh, Dataform | SQLMesh = dbt alternative with better testing. Dataform = Google native dbt alternative |

---

### Flink → ClickHouse vs Flink → Redis vs Flink → ClickHouse → Redis

| Pattern | When | Consumer |
|---|---|---|
| Flink → ClickHouse | Many viewers, time-range queries, aggregations | Grafana, alerts, dashboards |
| Flink → Redis | One system, key lookup, sub-ms response | ML inference, rate limiter, sessions |
| Flink → ClickHouse → Redis | ClickHouse result queried too frequently, need caching | High-traffic product features, homepages |

**Flink → ClickHouse:**
```
Use when: "SUM(orders) GROUP BY city for last 5 minutes"
Many consumers query same aggregated result over time ranges
Redis cannot do this — no time ranges, no aggregations
```

**Flink → Redis:**
```
Use when: "give me features for user USR-123 RIGHT NOW"
One system, one key, one value, <1ms response needed
ML inference, rate limiting, session storage
ClickHouse minimum 10-50ms — too slow for ML inference at scale
```

**Flink → ClickHouse → Redis (caching pattern):**
```
Problem: 10,000 users/sec hitting homepage → 10,000 ClickHouse queries/sec → overload

Solution:
Flink → ClickHouse (aggregates continuously)
Backend service queries ClickHouse every 2 min → WRITEs result to Redis (TTL=2min)
Homepage READs from Redis (0.1ms) → ClickHouse gets 1 query per 2 min instead of 10,000/sec

Example: "Trending Now" section — computed in ClickHouse, cached in Redis, served to millions
```

---

<!-- TODO: After all scenarios complete — add consolidated architecture diagram showing all scenarios in one big tree -->

