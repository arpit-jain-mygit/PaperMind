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

Feature Store = a SYSTEM with 3 components. Redis is only one part of it.

```
Feature Store components:

1. Offline Store  (S3 / Snowflake)
   → historical features for model TRAINING
   → months of data, batch access, slow is fine
   → "train model on last 6 months of restaurant features"

2. Online Store   (Redis / DynamoDB)
   → current features for model INFERENCE
   → latest values only, sub-ms access required
   → "give me restaurant 456's features RIGHT NOW"

3. Feature Registry  (Feast / internal catalog)
   → catalog of what features exist, who owns them
   → "restaurant:avg_prep_time, computed by Spark hourly, source: orders table"
```

Redis = ONLY the Online Store layer. Not the entire Feature Store.

```
Read speeds:
  PostgreSQL  →  5-20ms   (too slow for inference)
  DynamoDB    →  5-10ms   (acceptable at Amazon scale)
  Redis       →  0.1-1ms  (fastest, RAM-based)
```

Why pre-compute features instead of querying at inference time:
```
Option A — compute at inference time:
ML Service queries PostgreSQL, S3, multiple tables → 500ms+ to gather inputs → too slow

Option B — Feature Store:
Spark/Flink pre-compute → store in Redis → ML Service GET in 1ms → predict instantly
```

Managed tools (manage all 3 layers): Feast (open source), AWS SageMaker Feature Store, Tecton

Flink writes real-time features to Redis (active_orders, driver_count) — different keys from Spark.
Spark writes historical features to Redis (avg_prep_time, rating) — no key overlap, no conflict.
Consistency risk: historical features can be stale (up to 1 hour) — acceptable for delivery time prediction, NOT acceptable for fraud detection (use 15-min Spark interval or Flink for those features too).

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

---

### Scenario 1 — GB + Real-time + Analytics

#### Problem Statement
```
Size:    5 GB/day, ~25M events/day
Speed:   Real-time — answers needed in seconds
Use:     Analytics — "Orders by city right now? Which restaurant is spiking?"
Who:     Food delivery startup ops team
```

#### What's Different vs Previous
```
This is the BASELINE scenario — no previous scenario to compare.

Naive starting point (what most people try first):
  App → PostgreSQL → Grafana queries PostgreSQL directly

Functional problem with naive approach:
  Analytics team asks "orders by city last 5 min" every 30 sec
  → Grafana runs SELECT COUNT(*) GROUP BY city every 30 sec
  → Full table scan every time = PostgreSQL CPU spikes
  → OLTP (order saving) and OLAP (analytics) compete on same machine
  → During sale: order placement slows down because analytics is running

Technical problem:
  PostgreSQL is row-oriented — reads entire row to compute aggregations
  Not built for continuous aggregation across millions of rows

Architecture change that solves it:
  Add Flink between Kafka and ClickHouse
  Flink pre-aggregates in RAM → writes 50 rows per window, not 25M raw events
  ClickHouse reads 50 rows → <10ms, dedicated machine, zero OLTP impact
  PostgreSQL freed: handles only OLTP (order saves), nothing else
```

#### Full Architecture

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
                    READ       │       READ
              ┌────────────────┼────────────┐
              ↓                ↓            ↓
        [Payment]          [Driver]      [Flink]
         Service            Service     aggregates
          OLTP               OLTP       every 10 sec
                                           │
                                         WRITE
                                           │
                                      [ClickHouse]
                                      pre-aggregated rows
                                           │
                                         READ
                                           │
                                       [Grafana]
                                      auto-refresh 10 sec
```

#### Quick Recap — New Concepts Introduced
```
PostgreSQL  → OLTP source of truth (synchronous, ACID)
Debezium    → CDC: reads WAL after confirmed write, no dual write risk
Kafka       → event bus (decouples producers from consumers)
Flink       → real-time aggregation (stateful, RAM-based)
ClickHouse  → real-time OLAP (pre-aggregated rows, <10ms queries)
S3          → raw Parquet storage (permanent, cheap)
```

#### Key Design Decisions

**Why not App → Kafka directly?**
```
Dual write risk: PostgreSQL down but Kafka up → payment charged, no order saved
Fix: App writes ONLY to PostgreSQL. Debezium reads WAL after confirmed write.
```

**Why not App → Kafka → PostgreSQL?**
```
Async gap: customer sees "confirmed" before order actually saved in PostgreSQL
Consumer crash = order lost. Only valid for non-customer-facing async work.
```

**Why ClickHouse not PostgreSQL for analytics?**
```
Continuous aggregation every 10 sec = full table scan = CPU spikes on PostgreSQL
PostgreSQL is already handling OLTP — analytical queries steal resources
ClickHouse dedicated machine: <10ms on pre-aggregated rows, zero OLTP impact
```

#### CDC Deep Dive
```
WAL (Write Ahead Log) — PostgreSQL writes every change here automatically
Debezium reads WAL → publishes to Kafka as events:
{ operation: INSERT, table: orders, new_data: { order_id: 98765, amount: 450 } }

CDC properties:
  App stays simple     → writes only to PostgreSQL, zero knowledge of consumers
  No dual write        → CDC reads AFTER confirmed write, failed write = nothing published
  Guaranteed ordering  → WAL is strictly ordered: placed → preparing → delivered
  No data loss         → Kafka down 2 hrs? Debezium catches up from WAL on return
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| PostgreSQL | OLTP source of truth | App WRITEs, Debezium READs WAL |
| Debezium | CDC connector | READs WAL, WRITEs Kafka |
| Kafka | Event bus | Consumers READ |
| Payment/Driver Service | OLTP consumers | READ Kafka |
| Flink | Real-time aggregation | READs Kafka, WRITEs ClickHouse |
| ClickHouse | Real-time OLAP store | Flink WRITEs, Grafana READs |
| S3 | Raw storage | Kafka WRITEs |
| Grafana | Ops dashboard | READs ClickHouse |

#### GB Scale Reality
```
PostgreSQL  → $50/month   Kafka → $30/month
Flink       → $50/month   ClickHouse → $50/month
Total: ~$200/month, managed by 1 engineer part-time
```

#### Interview Answer
```
1. App WRITEs to PostgreSQL (single source of truth)
2. Debezium CDC READs WAL → WRITEs to Kafka
3. Kafka routes to: Payment Service, Driver Service, Flink
4. Flink aggregates every 10 sec → WRITEs to ClickHouse
5. Grafana READs ClickHouse → live ops dashboard
6. Kafka also WRITEs raw events to S3 for historical analysis

Key insight: "CDC via Debezium — app writes once, everything follows automatically."
```

#### Interview Questions

**Q1. Why not write directly from app to Kafka?**
```
Kafka is not a database (7-day retention, no ACID). Dual write risk: if PostgreSQL
write fails after Kafka write succeeds → payment charged, no order exists.
CDC solves: app writes ONLY to PostgreSQL, Debezium reads WAL after confirmed write.
```

**Q2. Dashboard showing stale data — how to debug?**
```
1. Kafka lag growing? → app not writing OR Flink falling behind
2. Flink running? → check job status, checkpoint, backpressure
   Backpressure = slow ClickHouse writes signal Flink to slow down → Kafka lag grows
3. ClickHouse last write timestamp → if old, Flink→ClickHouse connector broken
4. Cross-check: add Grafana panel "last data received X seconds ago"
```

**Q3. How to handle 10x traffic spike during sale?**

| Component | Handles 10x? | Fix | Pre-sale Action |
|---|---|---|---|
| App servers | No | Auto-scale (Kubernetes) | Pre-warm servers |
| PostgreSQL | Partially | Read replicas + PgBouncer | Vertical scale |
| Debezium | Yes (may lag) | Acceptable, catches up | Increase heap |
| Kafka | Yes — built for this | Increase partitions + retention | Pre-provision disk |
| Flink | Partially | Increase parallelism BEFORE sale | Scale cluster ahead |
| ClickHouse | Yes at GB scale | Batch writes if needed | Monitor write rate |
| S3 | Yes — infinite | Nothing needed | None |
| Spark | Partially | More executors, run more often | Pre-scale cluster |

```
Golden Rule: cannot fix most things DURING spike. Prepare BEFORE.
Read Replicas: Primary streams WAL to replicas (same WAL Debezium reads).
  Async replication: 10-100ms lag. Risk: driver queries replica before order appears.
  Fix: read-your-own-writes (route user's next read to Primary after their write).
PgBouncer: 50 servers × 20 connections = 1000 → exceeds PostgreSQL max_connections.
  PgBouncer multiplexes: app sees 1000 connections, PostgreSQL sees 20-50.

Leadership insight: "Kafka absorbs spike downstream but upstream bottleneck is app
servers and PostgreSQL. Flink needs pre-scaling — parallelism cannot change at runtime.
Only truly elastic component is S3."
```

**Q4. Junior suggests PostgreSQL for analytics to reduce complexity. Response?**
```
Valid at GB scale IF: query runs once/day, simple queries, no concurrent load.
Fails here: Grafana queries every 30 sec = continuous full table scan = CPU spikes
+ OLTP and OLAP compete on same machine = order placement slows during analytics.
Start with PostgreSQL, switch to ClickHouse when refresh < 1 min or team grows.
```

---

### Scenario 2 — GB + Real-time + ML

#### Problem Statement
```
Size:    5 GB/day, ~25M events/day
Speed:   Real-time — ML prediction in milliseconds
Use:     ML Inference — "Predict delivery time before confirmation screen loads"
Who:     Food delivery startup — prediction shown simultaneously with order confirmation
```

#### What's Different vs Scenario 1
```
Scenario 1 need:   Aggregate events → show on dashboard (one-to-many, async)
Scenario 2 need:   Predict per order → respond to customer IN the same request (one-to-one, sync)

Functional difference:
  Scenario 1: ops team sees dashboard refresh every 30 sec — delay is fine
  Scenario 2: customer is WAITING on the confirmation screen — must respond in <200ms
              prediction must be personalized per order (different restaurant, zone, driver count)

Technical difference:
  Scenario 1: Flink aggregates → pushes to ClickHouse → Grafana pulls
              Flow is push-based and async
  Scenario 2: App needs to PULL features on demand for a specific order
              Kafka cannot answer "what are the current features for restaurant_456 RIGHT NOW?"
              ClickHouse is for aggregations, not point lookups
              Need a serving layer that answers key-value lookups in <2ms

Architecture change that solves it:
  Add Redis as online Feature Store
  Flink still runs (same as Scenario 1) but now WRITEs pre-computed features to Redis
  Spark adds historical features to Redis hourly
  New component: ML Inference Service — HTTP endpoint, READs Redis, runs model, RESPONDs
  App makes 2 parallel calls: WRITE to PostgreSQL + REQUEST to ML Service
  ClickHouse and Grafana from Scenario 1 are DROPPED (use case changed from dashboard to ML)
```

#### Full Architecture

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

#### Quick Recap — New Concepts Introduced
```
Redis          → Feature Store online layer (sub-ms point lookups)
ML Service     → pulls from Redis on demand, runs model, responds synchronously
Feature Store  → system with 3 layers:
                 Offline Store (S3) — historical features for training
                 Online Store (Redis) — current features for inference
                 Feature Registry — catalog of features, ownership
Lambda Architecture → Flink (recency) + Spark (history) both write to Redis
                      model reads both at inference time
Lambda Architecture ≠ AWS Lambda (serverless functions)
```

#### Key Design Decisions

**Two parallel calls from App:**
```
Call 1: WRITE → PostgreSQL      (~20ms) — saves order
Call 2: REQUEST → ML Service    (~15ms) — gets prediction
App waits for BOTH → "Order confirmed! Arrives in 28 minutes"
App never touches Redis directly — ML Service handles that internally.
```

**Why not Kafka for ML Inference?**
```
Kafka = PUSH (sends data when events arrive)
ML Inference = PULL (needs features for specific user ON DEMAND)
1000 concurrent orders → each needs different features for different user/restaurant
Redis: 1000 point lookups in parallel in <1ms each
Kafka cannot answer "give me current state for restaurant_456" on demand
```

**Why Flink does not slow down:**
```
Flink keeps running state in RAM — not recount from scratch:
Event: order placed at Pizza Palace
Flink: state["Pizza Palace"] = current + 1  ← microseconds, not a full scan
Writes updated value to Redis every 30 sec
Checkpoints state to S3 every 30 sec (crash recovery)
```

**Feature Store consistency — Flink vs Spark:**
```
Flink writes: restaurant:456:active_orders (TTL=2min) — real-time keys
Spark writes: restaurant:456:avg_prep_time (TTL=2hr) — historical keys
Different keys → no conflict. No overwrite risk.

Staleness risk: historical features up to 1 hour old.
Acceptable: delivery time prediction (minor inaccuracy)
NOT acceptable: fraud detection → use 15-min Spark interval or Flink for those keys too
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| PostgreSQL | OLTP source of truth | App WRITEs, Debezium READs WAL |
| Debezium | CDC connector | READs WAL, WRITEs Kafka |
| Kafka | Event bus | Consumers READ |
| Payment/Driver Service | OLTP consumers | READ Kafka |
| S3 | Raw Parquet storage | Kafka WRITEs, Spark READs |
| Flink | Real-time feature computation | READs Kafka, WRITEs Redis |
| Spark | Historical feature computation | READs S3, WRITEs Redis |
| Redis | Feature Store online layer | Flink/Spark WRITE, ML Service READs |
| ML Inference Service | Runs model, returns prediction | READs Redis, RESPONDs to App |

#### GB Scale Reality
```
PostgreSQL → $50   Kafka+Debezium → $30   Flink → $50
Redis → $30        Spark → $20            ML Service → $50   S3 → $10
Total: ~$240/month
```

#### Interview Answer
```
1. App makes 2 parallel calls: WRITE to PostgreSQL + REQUEST to ML Service
2. Debezium CDC READs WAL → WRITEs Kafka
3. Kafka → Payment/Driver (OLTP) + Flink + S3
4. Flink READs Kafka → state in RAM → WRITEs real-time features to Redis
5. Spark READs S3 hourly → WRITEs historical features to Redis
6. ML Service READs Redis → model → RESPONSE "28 min" to App

Key insight: "Lambda Architecture — Flink for recency, Spark for history, Redis serves both.
App never touches Redis. ML Service is the only Redis reader."
```

#### Interview Questions

**Q1. Why Lambda Architecture — isn't it complex?**
```
Batch only (Spark hourly): features 1 hour stale → fraud in 10 min not caught
Stream only (Flink): cannot compute "avg_prep_time over 3 months" — state too large
Lambda: Flink handles last 5 min, Spark handles last 3 months, model uses both.

Alternative — Kappa Architecture (stream only):
Simpler but Flink state complex for long windows. Choose if historical window < 1 day.
```

**Q2. ML predictions suddenly inaccurate — how to debug?**
```
1. Feature freshness: Is Flink writing to Redis? Check Redis TTL on keys.
   Is Spark running? Check Airflow. Stale features = bad predictions.
2. Feature drift: restaurant added items → avg_prep_time formula outdated.
   Fix: monitor feature distribution, alert on shifts.
3. Model drift: world changed (new city, lockdown) → retrain on recent data.

Immediate mitigation: fall back to rule-based prediction (distance/avg_speed).
Long term: shadow mode — run new model alongside old before switching.
```

**Q3. Feature Store consistent between Flink and Spark writes?**
```
Different keys — no conflict. TTL design prevents stale data confusion.
Real-time (Flink): TTL=2min. Historical (Spark): TTL=2hr.
Fraud detection exception: needs 15-min Spark OR Flink computes rolling avg too.
```

**Q4. PM wants prediction for 50M global users — how does design change?**
```
Redis → Redis Cluster or DynamoDB (auto-scales, 10ms reads)
ML Service → auto-scaling fleet behind load balancer (stateless, easy to scale)
Flink → cluster with higher parallelism
Spark → EMR/Databricks distributed

Architecture pattern stays identical. Only scale configuration changes.
```

---

### Scenario 3 — GB + Real-time + Dashboards

#### Problem Statement
```
Size:    5 GB/day, ~25M events/day
Speed:   Real-time ops (30 sec refresh) + Near real-time business (15-30 min delay)
Use:     Two dashboards — ops team watching live + business team exploring history
Who:     Food delivery startup — sale event monitoring + weekly business reviews
```

#### What's Different vs Scenario 2
```
Scenario 2 need:   One consumer (ML Service) needs per-order features, synchronously
Scenario 3 need:   Two consumer types need different views of SAME data, at different speeds

Functional difference:
  Scenario 2: "What is the predicted delivery time for THIS order?"  ← per-order, real-time
  Scenario 3 (ops):  "How many orders placed in last 5 min across ALL cities?"  ← aggregate, live
  Scenario 3 (biz):  "What was revenue trend last 4 weeks by city?"  ← historical, on-demand

  Two completely different audiences with different latency tolerance and data depth needs.
  Cannot serve both from the same tool.

Technical difference:
  Scenario 2: Redis → single key lookup per request (GET user:USR-123:features)
              No time-range queries, no GROUP BY, no aggregation at read time
  Scenario 3 (ops): needs GROUP BY city, time-range WHERE, COUNT — Redis cannot do this
  Scenario 3 (biz): needs weeks of data, joins across tables — ClickHouse holds only hours

Architecture change that solves it:
  Redis and ML Service from Scenario 2 are DROPPED (different use case now)
  Flink → ClickHouse (ops): Flink writes pre-aggregated rows, Grafana reads, auto-refresh 30s
  Spark → Snowflake (biz): Spark reads S3, cleans + joins, Tableau queries on-demand
  Two separate serving layers: ClickHouse for speed, Snowflake for depth
  S3 becomes critical now — single source of raw data for Spark's historical processing
```

#### Full Architecture

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
         Service            Service     aggregates    cleans + joins
          OLTP               OLTP       every 30 sec    hourly
                                           │               │
                                         WRITE           WRITE
                                     pre-aggregated    clean fact
                                       results          table
                                           │               │
                                           ↓               ↓
                                      [ClickHouse]    [Snowflake]
                                      last few hrs    weeks/months
                                           │               │
                                         READ            READ
                                           │               │
                                       [Grafana]    [Tableau/Looker]
                                      Ops Dashboard  Biz Dashboard
                                      auto-refresh   on-demand
                                       every 30s
```

#### Quick Recap — New Concepts Introduced
```
ClickHouse  → real-time OLAP (ops dashboards, last few hours, <10ms)
Snowflake   → historical OLAP (business dashboards, weeks of data, 3-10 sec)
Grafana     → ops visualization (auto-refresh, time-series panels, engineers)
Tableau     → business visualization (drag-drop exploration, executives)
Two stacks  → Speed Stack (Kafka+Flink+ClickHouse) and Scale Stack (S3+Spark+Snowflake)
```

#### Key Design Decisions

**Two dashboard types — never use one tool for both:**
```
Ops Dashboard (ClickHouse + Grafana):
  Who: ops team, oncall engineers
  Q: "What is happening RIGHT NOW?"
  Refresh: every 30 sec, load in <2 sec
  Data: last few hours only

Business Dashboard (Snowflake + Tableau):
  Who: executives, product managers
  Q: "How are we doing TODAY vs last week?"
  Delay: 15-30 min acceptable
  Data: weeks/months of history
```

**Why not Snowflake for ops dashboard?**
```
Cold query = 3-10 sec minimum. Shared with analysts → contention during sale.
Cost: per-query compute billing × 120 queries/hour = expensive.
ClickHouse: always warm, fixed $50/month, zero contention, <10ms.
```

**Why not ClickHouse for business dashboard?**
```
Holds only last few hours. No joins across user/restaurant/driver tables.
Cannot answer: "revenue by category, last 4 weeks, broken down by city"
Snowflake: holds months of clean joined data, answers in 5-8 sec.
```

**Why no Redis between ClickHouse and Grafana?**
```
Primary reason — wrong access pattern:
  Redis = key-value. No time ranges, no aggregations, no GROUP BY.
  Grafana needs: SELECT city, COUNT(*) WHERE timestamp > NOW()-5MIN GROUP BY city
  Redis cannot execute this. Not what it is built for.

Secondary reason — low query volume:
  Grafana: 120 queries/hour. ClickHouse handles thousands/sec. No caching needed.

How "load in 2 sec" achieved:
  Flink writes pre-aggregated rows (50 rows, not 25M events)
  ClickHouse reads 50 rows → <10ms
  Grafana renders → <500ms
  Total: well under 2 sec

Add Redis between ClickHouse and consumers ONLY when:
  SAME result needed by MANY concurrent users (10,000 users/sec homepage)
  Result changes slowly (cache is safe)
  Redis stores the aggregated result as a BLOB, not a query result
  → SET "trending_products" = "[A, B, C...]" → GET by all 10,000 users
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| PostgreSQL | OLTP source of truth | App WRITEs, Debezium READs WAL |
| Debezium | CDC connector | READs WAL, WRITEs Kafka |
| Kafka | Event bus | Consumers READ |
| Payment/Driver Service | OLTP consumers | READ Kafka |
| S3 | Raw Parquet storage | Kafka WRITEs, Spark READs |
| Flink | Real-time aggregation | READs Kafka, WRITEs ClickHouse |
| Spark | Historical cleaning + joining | READs S3, WRITEs Snowflake |
| ClickHouse | Real-time dashboard store | Flink WRITEs, Grafana READs |
| Snowflake | Historical dashboard store | Spark WRITEs, Tableau READs |
| Grafana | Ops dashboard (auto-refresh) | READs ClickHouse |
| Tableau/Looker | Business dashboard (on-demand) | READs Snowflake |

#### GB Scale Reality
```
PostgreSQL → $50   Kafka+Debezium → $30   Flink → $50   ClickHouse → $50
Spark → $20        Snowflake → $50         S3 → $10
Total: ~$260/month
```

#### Interview Answer
```
1. App WRITEs to PostgreSQL (OLTP)
2. Debezium CDC → Kafka → Payment/Driver (OLTP)
3. Kafka → Flink → pre-aggregated results → ClickHouse
4. Grafana READs ClickHouse every 30s → ops team live view
5. Kafka → S3 → Spark hourly → clean joined data → Snowflake
6. Tableau READs Snowflake → business team explores history

Key insight: "Two tools for two consumers. ClickHouse: speed, last few hours, dedicated.
Snowflake: history, flexible SQL, shared. Never compromise either for the other."
```

#### Interview Questions

**Q1. Why not Snowflake for both dashboards?**
```
Cold start 3-10 sec, shared with analysts = contention during sale event.
ClickHouse: always warm, dedicated, <10ms. Fixed cost vs per-query billing.
```

**Q2. Dashboard shows order drop — real issue or pipeline problem?**
```
1. Check Kafka: app still producing events? No → real business issue.
2. Check Flink: running? consumer lag growing? Stopped → pipeline issue.
3. Check ClickHouse: when was last write? Old → Flink→ClickHouse broken.
4. Cross-check PostgreSQL directly:
   SELECT COUNT(*) FROM orders WHERE created_at > NOW() - INTERVAL 5 MIN
   PostgreSQL normal → pipeline issue. PostgreSQL also low → real issue.
Always add panel: "Pipeline health: last data received X seconds ago"
```

**Q3. How to design alerting?**
```
Business alerts (ClickHouse → Grafana Alert):
  orders_count < 100 in last 5 min during lunch → page oncall
  payment_failure_rate > 5% last 10 min → page oncall

Pipeline alerts:
  last_clickhouse_write > 2 min → "Dashboard data stale"
  Flink checkpoint failed → "Stream processing issue"
  Kafka consumer lag > 100K → "Processing falling behind"

Route separately: business alerts → ops Slack. Pipeline alerts → data eng oncall.
```

**Q4. Data scientist wants to run ML experiments on live data?**
```
Never give direct ClickHouse access — one heavy query = dashboard breaks during sale.
Option 1: Snowflake (already exists, 1-hour delay acceptable for experiments)
Option 2: S3 + Databricks notebook (reads raw data, no shared production resources)
Governance: ClickHouse = dedicated ops. Snowflake/S3 = DS/analysts. DB = app only.
```

---

### Scenario 4 — GB + Real-time + Another System

#### Problem Statement
```
Size:    5 GB/day, ~25M events/day
Speed:   Real-time — milliseconds to seconds
Use:     Another System consuming our events (notification, inventory, fraud, loyalty)
Who:     Food delivery startup — order placed → multiple downstream systems must react
```

#### What's Different vs Scenario 3
```
Scenario 3 need:   Humans read dashboards — wrong data is annoying but recoverable
Scenario 4 need:   Machines act on events — wrong data causes real damage

Functional difference:
  Scenario 3: Grafana shows wrong order count → ops team notices → not a crisis
  Scenario 4: Notification Service sends duplicate push notification → customer confused
              Inventory Service decrements stock twice → stock goes negative
              Fraud Service misses a fraudulent order → financial loss + chargeback
              These are IRREVERSIBLE actions with real consequences

  New requirement: exactly-once semantics (Kafka default is at-least-once)
  New requirement: schema stability (Grafana can handle extra columns, code cannot)
  New requirement: one consumer failing must not block others
  New requirement: fraud must BLOCK order — cannot be async

Technical difference:
  Scenario 3: consumers are passive readers (Grafana, Tableau) — they just display
  Scenario 4: consumers take ACTIONS (send push, decrement stock, charge card)
              If Kafka replays an event (crash/retry), action happens TWICE
              If schema changes silently, consumer code crashes with null pointer

Architecture change that solves it:
  Schema Registry added to Kafka — validates every message schema, blocks breaking changes
  Each downstream system gets its own Consumer Group — independent offsets, no blocking
  Idempotency check added to each consumer — check before acting, prevents duplicate actions
  DLQ (Dead Letter Queue) added — failed events retry separately, main consumer unblocked
  Fraud Service added as SYNCHRONOUS HTTP call from App — cannot be async, must block order
  Flink now pre-computes fraud features into Redis (same Flink pattern as Scenario 2)
  ClickHouse and Snowflake from Scenario 3 are DROPPED (use case changed)
```

#### Full Architecture

```
[Customer places order]
        ↓
[App] ──1. WRITE (HTTP)──────→ [PostgreSQL]        ← saves order status="pending" (OLTP)
  │                                   │
  │                             READ (WAL)
  │                                   │
  │                            [Debezium CDC]
  │                                   │
  │                                 WRITE
  │                                   │
  │                               [Kafka] ◄─────────────────────────────────────────┐
  │                            (Schema Registry)                                     │
  │                                   │                                              │
  │              READ (own offset)    │    READ (own offset)    READ (own offset)    │
  │   ┌───────────────────────────────┼──────────────────┬──────────────────────┐   │
  │   ↓                               ↓                  ↓                      ↓   │
  │ [Notification]            [Inventory Svc]      [Loyalty Svc]            [Flink]  │
  │   Service                  decrements           adds points             state    │
  │   idempotency check         stock                                       in RAM   │
  │   WRITEs PostgreSQL           │                                           │      │
  │   (notification_sent)         ↓                                    WRITE every  │
  │         │                [Inventory DB]                              30 seconds  │
  │         ↓                                                                 │      │
  │   [Push Notification]                                                     ↓      │
  │   sent to user                                              [Redis — fraud features]
  │                                                             user:USR-123:order_velocity
  │                                                             user:USR-123:avg_order_amount
  │                                                             device:DEV-789:flagged
  │                                                                           │
  │                                                                         READ
  │                                                                           │
  └──2. REQUEST (HTTP)──────────────────────────→ [Fraud Service]  ◄─────────┘
                                                    runs ML model
                                                    (~15ms total)
                                                         │
                              ┌──────────────────────────┴──────────────────────────┐
                        score > 0.8                                           score < 0.8
                              │                                                      │
                        REJECT order                                         APPROVE order
                     UPDATE PostgreSQL                                    UPDATE PostgreSQL
                     status = "rejected"                                  status = "confirmed"
                     show user error                                      show user "Confirmed!"
                                                                                     │
                                                                          Debezium picks up
                                                                          confirmed event
                                                                          → Kafka pipeline
                                                                          → Notification,
                                                                            Inventory,
                                                                            Loyalty react
```

#### Quick Recap — New Concepts Introduced
```
Schema Registry    → validates Kafka message schemas, prevents breaking changes
Consumer Groups    → each system has own offset, fully independent read pace
Idempotency Check  → prevents duplicate notifications on Kafka replay
DLQ                → Dead Letter Queue, stores failed events for retry
Exactly-Once       → Kafka default = at-least-once. Idempotency = exactly-once behavior
Fraud Service      → synchronous HTTP (not Kafka) — must respond BEFORE order confirmed
```

#### Key Design Decisions

**Fraud detection is synchronous HTTP, not Kafka:**
```
Kafka = async. You publish event and move on. No way to wait for a response.
Fraud must BLOCK order confirmation — cannot proceed without a score.
Pattern: App → HTTP → Fraud Service → Redis features → score → RESPOND in <200ms
Order enters Kafka pipeline ONLY after fraud check passes.
```

**Exactly-once delivery (Kafka gives at-least-once by default):**
```
Problem: Notification Service crashes after sending push notification,
         before committing Kafka offset. Kafka replays event. Duplicate push sent.

Fix — Idempotency Check:
1. CHECK PostgreSQL: SELECT * FROM notifications_sent WHERE order_id=98765
   → exists? → skip (already sent)
   → not exists? → send → INSERT record
2. Commit Kafka offset

Even if crash replays event → check catches it → no duplicate
```

**Schema Registry prevents silent breaking changes:**
```
Producer registers schema → Consumer validates schema version ID in each message
Adding optional field   → backward compatible → allowed
Renaming field          → breaking change    → blocked until consumers updated
Process: notify consumers → deploy consumers handling both → deploy producer → cleanup
```

**Consumer Group isolation:**
```
Notification Service offset: Partition 0: 1,523  Partition 1: 987
Inventory Service offset:    Partition 0: 1,521  Partition 1: 990

Completely independent. Notification slow → does not block Inventory.
Inventory crashes → does not affect Notification.
```

**Dead Letter Queue (DLQ):**
```
Push notification service is down → event processing fails
Main consumer: moves failed event to "order-events-dlq" topic
DLQ consumer: retries with exponential backoff (1min → 5min → 30min → 2hr)
After 5 failures → alert data engineering team
Main consumer: unblocked, continues processing new events
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| PostgreSQL | OLTP + idempotency store | App WRITEs, Debezium READs WAL, Services WRITE idempotency |
| Debezium | CDC connector | READs WAL, WRITEs Kafka |
| Kafka | Event bus with consumer isolation | Each service READs own offset |
| Schema Registry | Schema validation | Producers WRITE schema, consumers READ/validate |
| Notification Service | Sends push notifications | READs Kafka, WRITEs idempotency to PostgreSQL |
| Inventory Service | Decrements stock | READs Kafka, WRITEs inventory DB |
| Loyalty Service | Adds points | READs Kafka, WRITEs loyalty DB |
| Fraud Service | Scores transactions synchronously | READs Redis, RESPONDs to App |
| Redis | Fraud features (pre-computed by Flink) | Flink WRITEs, Fraud Service READs |
| DLQ | Failed event retry store | Kafka WRITEs failed events, DLQ consumer READs |

#### GB Scale Reality
```
PostgreSQL → $50   Kafka+Debezium+Schema Registry → $30
Notification/Inventory/Loyalty Services → $30 each
Fraud Service → $50   Redis → $30
Total: ~$220/month
```

#### Interview Answer
```
1. App WRITEs to PostgreSQL (OLTP, synchronous)
2. App makes parallel REQUEST to Fraud Service (synchronous HTTP, <200ms)
   → Fraud READs Redis features → scores → RESPONDs
   → Fraud > 0.8: reject. Clean: proceed.
3. Debezium CDC READs WAL → WRITEs Kafka
4. Multiple consumer groups READ Kafka independently:
   → Notification: idempotency check → send push → WRITE record to PostgreSQL
   → Inventory: decrement stock
   → Loyalty: add points
5. Schema Registry validates all messages
6. DLQ handles failures with exponential backoff

Key insight: "Fraud is synchronous HTTP — must respond before order confirmed.
Everything else is async Kafka. Idempotency = exactly-once behavior from at-least-once system."
```

#### Interview Questions

**Q1. How to ensure no duplicate notifications?**
```
Idempotency check pattern:
Before processing: SELECT * FROM notifications_sent WHERE order_id=X AND type=Y
→ exists → skip. Not exists → send → INSERT → commit Kafka offset.

Even if service crashes and Kafka replays → check catches duplicate.
This converts Kafka's at-least-once into exactly-once behavior.
```

**Q2. New team wants to consume order events — how to onboard?**
```
1. Create new consumer group — completely isolated from existing consumers
2. Register with Schema Registry — get notified of schema changes
3. Set up DLQ for their service
4. Start consuming from earliest offset if they need historical catchup
5. They never touch Kafka topic configuration — isolated by design

No impact on Notification, Inventory, or Loyalty Services whatsoever.
```

**Q3. How to handle 10x traffic spike?**

| Component | Handles 10x? | Fix | Pre-event Action |
|---|---|---|---|
| App servers | No | Auto-scale (Kubernetes) | Pre-warm |
| PostgreSQL | Partially | Read replicas + PgBouncer | Vertical scale |
| Debezium | Yes (may lag) | Catches up from WAL | Increase heap |
| Kafka | Yes — built for this | Increase partitions | Pre-provision disk |
| Schema Registry | Yes | Stateless, cache locally | Pre-scale |
| Notification Service | No | Scale horizontally (stateless) | Pre-scale |
| Fraud Service | No | Scale + Redis cluster | Pre-scale Redis |
| Redis | Partially | Redis Cluster or ElastiCache | Pre-scale |
| DLQ | Yes | More partitions if needed | Pre-provision |

```
Golden Rule: Kafka absorbs spike downstream. Upstream bottleneck = App + PostgreSQL + Fraud.
Redis single machine is risk — pre-cluster before sale.
Fraud Service response time must stay <200ms even at 10x — scale it aggressively.
All consumer services are stateless → easy horizontal scaling.

Leadership insight: "Most consumers are stateless and trivially auto-scale.
Real risks are Fraud latency SLA and Redis. Both must be pre-scaled before event."
```

**Q4. Team argues Kafka alone is enough — no need for PostgreSQL as OLTP source?**
```
Kafka is NOT a database:
- 7-day retention → can't query order placed 30 days ago
- No random reads by order_id
- No transactions — if Kafka write succeeds but downstream fails, no rollback
- Dual write risk: if app writes to Kafka and PostgreSQL separately → one can fail

CDC pattern: app writes ONLY to PostgreSQL (ACID guaranteed).
Debezium reads WAL AFTER confirmed write → no dual write.
PostgreSQL = source of truth. Kafka = event bus. Never conflate the two.
```

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


<!-- TODO: After all scenarios complete — add consolidated architecture diagram showing all scenarios in one big tree -->

