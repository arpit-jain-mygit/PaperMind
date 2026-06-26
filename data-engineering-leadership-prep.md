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

## GB Scale Scenarios

> Coming soon — will be added scenario by scenario.

---
