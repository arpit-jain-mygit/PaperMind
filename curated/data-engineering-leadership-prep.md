# Data Engineering Leadership Interview Prep

> Goal: Crack Data Engineering leadership roles at Amazon, Microsoft, Google, Uber, Salesforce, Facebook.
> Approach: Start simple, build up gradually. Focus on large data volume problems.

---

## TODO — Scenarios Not Yet Covered

> These are MAANG-relevant pipeline topics missing from the 36 scenarios above. Add as full scenarios with architecture + interview questions.

- [ ] **Lambda vs Kappa tradeoffs** — when do you ditch the batch layer entirely? When does Kappa break?
- [ ] **Exactly-once semantics deep dive** — Kafka transactions, Flink checkpointing internals, idempotent producers
- [ ] **Backfill at scale** — business logic changed, reprocess 2 years of PB-scale history without taking down prod
- [ ] **Schema evolution** — breaking vs non-breaking changes, migrate 50 downstream consumers safely
- [ ] **Multi-tenancy** — one pipeline serving 50 enterprise clients with different SLAs, data isolation, billing
- [ ] **Pipeline observability** — how do you detect a silent data quality failure that happened 3 hours ago?
- [ ] **Data contracts** — producer changes schema, 30 consumers break; who owns what?
- [ ] **Disaster recovery** — primary Kafka cluster dies, recover with zero data loss
- [ ] **Cost attribution** — which team is burning $200K/month in BigQuery?
- [ ] **Push vs pull** — when do you let downstream systems pull vs pushing events to them?
- [ ] **Data mesh** — domain ownership, federated governance at org scale
- [ ] **CDC alternatives** — dual write, outbox pattern, event sourcing vs Debezium WAL

---

## Table of Contents

- [Generic Full Architecture — All 36 Scenarios](#generic-full-architecture--all-36-scenarios-gb--tb--pb)
  - [Generic Architecture Diagram for GB Scenarios](#generic-architecture-diagram-for-gb-scenarios-5gbday)
  - [Abstraction Mapping](#abstraction-mapping)
  - [Tool Selection Matrix](#tool-selection-matrix--all-alternatives-gbtbpb)
  - [36 Scenarios — Leaf-Level Mapping](#36-scenarios--leaf-level-mapping-9-use-cases--4-scales)
  - [Components Deep Dive: Flink & ClickHouse](#components-deep-dive-flink--clickhouse)
    - [Apache Flink: Real-Time Stream Processor](#apache-flink-real-time-stream-processor)
      - [Business Use Cases](#business-use-cases)
    - [ClickHouse: Super-Fast Analytics Database](#clickhouse-super-fast-analytics-database)
      - [Business Use Cases](#business-use-cases-1)
    - [Flink + ClickHouse Together](#flink--clickhouse-together-the-real-pattern)
      - [Peak Lunch Hour Example](#peak-lunch-hour-example-food-delivery)
    - [Three Architecture Options](#three-architecture-options-how-clickhouse-works)
      - [Option 1: ClickHouse as Dumb Storage](#option-1-clickhouse-as-dumb-storage)
      - [Option 2: ClickHouse as Smart Calculator](#option-2-clickhouse-as-smart-calculator)
      - [Option 3: Hybrid](#option-3-hybrid-best-of-both---most-common-in-production)
    - [When to Use Each Component](#when-to-use-each-component)
    - [Your GB Scale Example](#your-gb-scale-example)
  - [Snowflake: Batch Analytics Data Warehouse](#snowflake-batch-analytics-data-warehouse)
    - [What is Snowflake?](#what-is-snowflake-simple)
    - [How Data Flows](#how-data-flows-spark--dbt--snowflake-star-schema--analytics)
    - [The Key Insight](#the-key-insight-spark-doesnt-create-star-schema)
    - [What Snowflake Stores](#what-snowflake-stores-3-types-of-tables)
    - [Real Business Examples](#real-business-examples-how-analytics-use-snowflake)
    - [Snowflake vs ClickHouse](#snowflake-vs-clickhouse-when-to-use-each)
  - [Fact & Dimension Tables: Star Schema Pattern](#fact--dimension-tables-star-schema-pattern)
    - [Simple Analogy](#simple-analogy-receipt-book--customer-file)
    - [How They Work Together](#how-they-work-together)
    - [Why Separate Tables?](#why-separate-tables-the-key-difference)
    - [Real Food Delivery Example](#real-food-delivery-example-complete-star-schema)
    - [Business Questions Using Star Schema](#business-questions-using-star-schema)
    - [Key Insight](#key-insight)
  - [Scheduled SQL Queries in Snowflake](#scheduled-sql-queries-in-snowflake)
    - [Three Ways to Answer Questions](#three-ways-to-answer-questions)
      - [1. Scheduled Queries](#1-scheduled-queries-automated-run-on-schedule)
      - [2. On-Demand Queries](#2-on-demand-queries-manual-run-when-user-asks)
      - [3. Materialized Views](#3-materialized-views-pre-computed-tables-refreshed-on-schedule)
    - [Real Dashboard Example](#real-dashboard-example-three-widgets)
    - [Decision Table](#decision-table-when-to-use-each)
    - [Complete Data Pipeline](#complete-data-pipeline)
    - [Your Role as Architect](#your-role-as-architect)
  - [Architecture Invariants & Golden Rules](#architecture-invariants--golden-rules)
- [Pattern 2: Specific Tools (Original — For Reference)](#pattern-2-specific-tools-original--for-reference)
- [MAANG Interview Questions by Scale](#maang-interview-questions-by-scale)
- [TODO — Scenarios Not Yet Covered](#todo--scenarios-not-yet-covered)
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
- [TB Scale Scenarios](#tb-scale-scenarios-5-tbday--national-scale)
  - [Scenario 13 — TB + Real-time + Analytics](#scenario-13--tb--real-time--analytics)
  - [Scenario 14 — TB + Real-time + ML](#scenario-14--tb--real-time--ml)
  - [Scenario 15 — TB + Real-time + Dashboards](#scenario-15--tb--real-time--dashboards)
  - [Scenario 16 — TB + Real-time + Another System](#scenario-16--tb--real-time--another-system)
  - [Scenario 17 — TB + Near Real-time + Analytics](#scenario-17--tb--near-real-time--analytics)
  - [Scenario 18 — TB + Near Real-time + ML](#scenario-18--tb--near-real-time--ml)
  - [Scenario 19 — TB + Near Real-time + Dashboards](#scenario-19--tb--near-real-time--dashboards)
  - [Scenario 20 — TB + Near Real-time + Another System](#scenario-20--tb--near-real-time--another-system)
  - [Scenario 21 — TB + Batch + Analytics](#scenario-21--tb--batch--analytics)
  - [Scenario 22 — TB + Batch + ML](#scenario-22--tb--batch--ml)
  - [Scenario 23 — TB + Batch + Dashboards](#scenario-23--tb--batch--dashboards)
  - [Scenario 24 — TB + Batch + Another System](#scenario-24--tb--batch--another-system)
- [PB Scale Scenarios](#pb-scale-scenarios-5-pbday--global-scale)
  - [Scenario 25 — PB + Real-time + Analytics](#scenario-25--pb--real-time--analytics)
  - [Scenario 26 — PB + Real-time + ML](#scenario-26--pb--real-time--ml)
  - [Scenario 27 — PB + Real-time + Dashboards](#scenario-27--pb--real-time--dashboards)
  - [Scenario 28 — PB + Real-time + Another System](#scenario-28--pb--real-time--another-system)
  - [Scenario 29 — PB + Near Real-time + Analytics](#scenario-29--pb--near-real-time--analytics)
  - [Scenario 30 — PB + Near Real-time + ML](#scenario-30--pb--near-real-time--ml)
  - [Scenario 31 — PB + Near Real-time + Dashboards](#scenario-31--pb--near-real-time--dashboards)
  - [Scenario 32 — PB + Near Real-time + Another System](#scenario-32--pb--near-real-time--another-system)
  - [Scenario 33 — PB + Batch + Analytics](#scenario-33--pb--batch--analytics)
  - [Scenario 34 — PB + Batch + ML](#scenario-34--pb--batch--ml)
  - [Scenario 35 — PB + Batch + Dashboards](#scenario-35--pb--batch--dashboards)
  - [Scenario 36 — PB + Batch + Another System](#scenario-36--pb--batch--another-system)

---

## Scenario Quick Reference — Last Minute Revision

| # | Scenario | Unique Requirement | Key Design Choice | Added vs Previous | Dropped vs Previous | Architecture |
|---|---|---|---|---|---|---|
| 1 | GB + Real-time + Analytics | Aggregate across all events in seconds. OLTP must not slow down | Flink pre-aggregates in RAM → ClickHouse (dedicated). CDC via Debezium keeps PostgreSQL as single source | PostgreSQL, Debezium, Kafka, Flink, ClickHouse, S3, Grafana | — (baseline) | [diagram](#scenario-1--gb--real-time--analytics) |
| 2 | GB + Real-time + ML | Per-order prediction must return in <200ms alongside order confirmation | App makes 2 parallel HTTP calls. Redis as online Feature Store. Lambda Architecture: Flink (real-time features) + Spark (historical features) | Redis, ML Inference Service, Spark | ClickHouse, Grafana | [diagram](#scenario-2--gb--real-time--ml) |
| 3 | GB + Real-time + Dashboards | Two audiences: ops (seconds) + business (historical). One tool cannot serve both | Speed Stack: Flink→ClickHouse→Grafana. Scale Stack: S3→Spark→Snowflake→Tableau. Same Kafka feeds both | ClickHouse, Grafana, Snowflake, Tableau | Redis, ML Service | [diagram](#scenario-3--gb--real-time--dashboards) |
| 4 | GB + Real-time + Another System | Downstream machines take ACTIONS. Duplicate = real damage. Fraud must block order | Schema Registry (schema stability). Consumer Groups (isolation). Idempotency (exactly-once). DLQ (retry). Fraud = sync HTTP, not Kafka | Schema Registry, Consumer Groups, Idempotency store, DLQ, Fraud Service | ClickHouse, Snowflake, Grafana, Tableau | [diagram](#scenario-4--gb--real-time--another-system) |
| 5 | GB + Near RT + Analytics | Same analytics as S1 but 5-15 min delay acceptable | Flink → Spark micro-batch (every 5 min from S3). $50/month cheaper. Backfill is free | Spark Structured Streaming | Flink | [diagram](#scenario-5--gb--near-real-time--analytics) |
| 6 | GB + Near RT + ML | Predictions needed every 5 min on tracking screen — not blocking order confirmation | Pre-compute predictions in Spark micro-batch for ALL active orders → store in Redis. App just GETs result. No ML Service running models on-demand. | Spark micro-batch (prediction run) | ML Inference Service (on-demand) | [diagram](#scenario-6--gb--near-real-time--ml) |
| 7 | GB + Near RT + Dashboards | Both ops AND business teams accept 5-15 min delay | One tool (Snowflake) serves both dashboards via Snowpipe. Eliminates ClickHouse + Flink. Simpler, cheaper. | Snowflake for ops dashboard too | Flink, ClickHouse | [diagram](#scenario-7--gb--near-real-time--dashboards) |
| 8 | GB + Near RT + Another System | Downstream systems act within minutes, not seconds. Fraud still blocks order. | DB polling every 5-10 min replaces always-on Kafka consumers. Staging table in PostgreSQL. Simpler for downstream teams. Fraud = always sync HTTP. | PostgreSQL staging table, Spark micro-batch | Always-on Kafka consumers per service | [diagram](#scenario-8--gb--near-real-time--another-system) |
| 9 | GB + Batch + Analytics | Analysts need fully settled, joined, clean data from yesterday | Airflow + Spark (nightly ETL) + dbt (SQL business logic) + Snowflake. Complete joins across all tables. Data freshness contract: ready by 6am. | Airflow, dbt | Flink, ClickHouse, micro-batch scheduling | [diagram](#scenario-9--gb--batch--analytics) |
| 10 | GB + Batch + ML | Train or retrain ML model on 6 months of historical data | Spark feature engineering (joins 6 months S3) → ML training job → Model Registry (MLflow). A/B test before rollout. Output = model artifact, not predictions. | ML Training Job, Model Registry (MLflow) | Redis, Flink, ML Inference (inference replaced by training) | [diagram](#scenario-10--gb--batch--ml) |
| 11 | GB + Batch + Dashboards | Fixed executive KPI dashboard loads instantly once daily | dbt builds daily_kpi_summary (1 row/day, 6 KPIs). Dashboard reads 1 row = instant load. Pre-aggregate everything — never query raw table at dashboard open time. | daily_kpi_summary dbt model | ClickHouse, real-time pipeline | [diagram](#scenario-11--gb--batch--dashboards) |
| 12 | GB + Batch + Another System | External partners/regulators expect scheduled FILE transfers (CSV/XML), not events | Spark generates formatted files → validate (row count, checksum, schema) → deliver via SFTP/S3/API → track acknowledgement in PostgreSQL. File schema is a contract. | File export Spark job, SFTP delivery, delivery ack log | Kafka consumers, Redis, real-time components | [diagram](#scenario-12--gb--batch--another-system) |
| **13** | **TB + Real-time + Analytics** | 5B events/day breaks every single-node component simultaneously | Cassandra (sharded by order_id), Debezium cluster, Kafka Cluster (100+ partitions), Flink Cluster, Druid/Pinot. Pattern identical to S1 — only scale changes. Partition key = most critical decision. | Cassandra, Kafka Cluster, Flink Cluster, Druid/Pinot | PostgreSQL, single Flink, ClickHouse | [diagram](#scenario-13--tb--real-time--analytics) |
| **14** | **TB + Real-time + ML** | Billions of feature keys → single Redis OOM. Thousands of concurrent inference requests → single ML Service saturates | Redis Cluster (hash tags keep user features on same node). ML Service Fleet (stateless, auto-scales). TTL on every key — no TTL = cluster crash. | Redis Cluster, ML Service Fleet | Single Redis, single ML Service | [diagram](#scenario-14--tb--real-time--ml) |
| **15** | **TB + Real-time + Dashboards** | TB/day write rate breaks single-node ClickHouse + Snowflake small | Druid/Pinot (distributed RT OLAP, 20-30 real-time ingest nodes). BigQuery/Redshift (serverless or large cluster). Same two-stack pattern as S3. | Druid/Pinot, BigQuery/Redshift | ClickHouse, Snowflake small | [diagram](#scenario-15--tb--real-time--dashboards) |
| **16** | **TB + Real-time + Another System** | 5B idempotency checks/day saturates PostgreSQL. Consumers cannot keep up. Fraud Service CPU saturated. | Cassandra idempotency (LWT for atomic check-and-insert). Auto-scaling Consumer Fleets (capped at partition count). Fraud Service Fleet. Max consumers = Kafka partition count — plan partitions early. | Cassandra idempotency, Consumer Fleets, Fraud Fleet, Redis Cluster | PostgreSQL idempotency, single consumers | [diagram](#scenario-16--tb--real-time--another-system) |
| **17** | **TB + Near Real-time + Analytics** | 1 TB per 5-min micro-batch — single Spark node takes hours | Spark on EMR (auto-scaling). S3 minute partitions (critical — reads only 5-min slice, not full hour). Partition granularity must match batch interval. | Spark on EMR, S3 minute partitions | Single Spark | [diagram](#scenario-17--tb--near-real-time--analytics) |
| **18** | **TB + Near Real-time + ML** | 50M active orders × 200 bytes = single Redis OOM. Scoring 50M orders on 1 Spark node impossible. | Redis Cluster for predictions (sharded). Spark on EMR (distributed scoring). Model broadcast to all workers once — never load per partition. Batch-MGET features per Spark partition. | Redis Cluster (predictions), Spark on EMR | Single Redis, single Spark | [diagram](#scenario-18--tb--near-real-time--ml) |
| **19** | **TB + Near Real-time + Dashboards** | TB/day Snowpipe lags. Small Snowflake warehouse query contention. | BigQuery (serverless, no sizing mistake possible). Slot reservation separates dashboard SLA from analyst workload. Spark on EMR writes directly to BigQuery. | BigQuery with slot reservation, Spark on EMR | Snowflake small, Snowpipe | [diagram](#scenario-19--tb--near-real-time--dashboards) |
| **20** | **TB + Near Real-time + Another System** | 5B rows/day into PostgreSQL staging = saturation | Cassandra staging (partition by service_name + time_bucket). Each consumer reads exactly one partition. 5-min buckets = ~17M orders each — manageable. | Cassandra staging (partitioned by service + bucket) | PostgreSQL staging | [diagram](#scenario-20--tb--near-real-time--another-system) |
| **21** | **TB + Batch + Analytics** | 5 TB nightly → single Spark takes 10+ hours → misses 6am SLA | Spark on EMR (50+ workers, 250 tasks). Incremental dbt (yesterday's partition only). Spot instances (80% spot = 70% cheaper). AQE for auto-tuning. | Spark on EMR (50+ workers), dbt incremental (16 threads), BigQuery/Snowflake XL | Single Spark, dbt serial | [diagram](#scenario-21--tb--batch--analytics) |
| **22** | **TB + Batch + ML** | 900 TB feature engineering on single node = weeks | Spark on EMR (100+ workers). SageMaker distributed training (10-20 GPUs, data parallelism). Pipe mode: streams training data from S3 (no disk copy). Sample 10% first — full data only if justified. | Spark on EMR (100+ workers), SageMaker GPU fleet | Single Spark, single GPU, MLflow local | [diagram](#scenario-22--tb--batch--ml) |
| **23** | **TB + Batch + Dashboards** | TB history rescan every night: 900 TB × $5/TB = $4,500/night | Incremental dbt (scans only yesterday's 5 TB → $25/night). BigQuery date-partitioned (only billed for partitions scanned). Column/row-level security. Summary table output identical to S11. | dbt incremental + date-partitioned BigQuery | dbt full refresh | [diagram](#scenario-23--tb--batch--dashboards) |
| **24** | **TB + Batch + Another System** | 1000 partners × sequential Spark + SFTP = all night, saturation | Airflow parallel DAGs (10 EMR jobs × 100 partners). 5000 parallel SFTP uploads. Cassandra ack log (partition by partner+date, no hotspot). Regulatory files: single compressed file (gzip). | Spark on EMR (parallel per batch), Cassandra ack log, delivery fleet | Single Spark, PostgreSQL ack, single SFTP | [diagram](#scenario-24--tb--batch--another-system) |
| **25** | **PB + Real-time + Analytics** | Global traffic — single-region Cassandra/Kafka creates cross-region write latency | Spanner/Bigtable (globally distributed, multi-region writes). Pub/Sub or Pulsar (global topics). Apache Beam on Dataflow (serverless, scales to PB). Apache Pinot (purpose-built PB OLAP, sub-10ms at LinkedIn scale). | Spanner/Bigtable, Pub/Sub, Beam on Dataflow, Apache Pinot global | Cassandra regional, Kafka regional, Druid/Pinot regional | [diagram](#scenario-25--pb--real-time--analytics) |
| **26** | **PB + Real-time + ML** | Trillions of feature keys × 200 bytes = TB of RAM → Redis Cluster too expensive | Aerospike (RAM for index, SSD for values — 10x cheaper than Redis at PB). Regional ML Service fleets (nearest-region routing). Blue-green deploy per region — never all regions simultaneously. | Aerospike (global, SSD-backed), regional ML fleets | Redis Cluster | [diagram](#scenario-26--pb--real-time--ml) |
| **27** | **PB + Real-time + Dashboards** | One regional OLAP cluster becomes global bottleneck. Execs need global rollup. | Two levels of aggregation in one Beam pipeline (regional + global). Pinot regional brokers (local latency) + global broker (exec view). BigQuery global dataset (cross-region replicated, reads from nearest). | Beam multi-region, Pinot global cluster, BigQuery global dataset | Pinot regional, BigQuery regional | [diagram](#scenario-27--pb--real-time--dashboards) |
| **28** | **PB + Real-time + Another System** | Same order may be processed by two regional consumers (cross-region replication lag) | Spanner globally consistent idempotency (TrueTime — one INSERT winner globally). Partition-to-region assignment: each region owns subset of Pub/Sub partitions, no cross-region processing. | Spanner idempotency, Pub/Sub partition-to-region assignment | Cassandra idempotency (regional only) | [diagram](#scenario-28--pb--real-time--another-system) |
| **29** | **PB + Near Real-time + Analytics** | 17 TB per 5-min micro-batch — EMR cluster needs 10x more workers | Beam on Dataflow (serverless — auto-scales to 17 TB, no cluster sizing). GCS per-second partitions (reads exactly 300 seconds per batch). Same code switches to streaming mode if SLA tightens. | Beam on Dataflow serverless, GCS per-second partitions | Spark on EMR fixed cluster, S3 minute partitions | [diagram](#scenario-29--pb--near-real-time--analytics) |
| **30** | **PB + Near Real-time + ML** | 500M active orders × scoring in 5 min. 2.5B feature reads during scoring = 8M reads/sec. | Export feature snapshot to GCS before scoring (avoids 2.5B Aerospike reads during run). Spark on Dataproc (10K workers). 1.6M Aerospike writes/sec for predictions — routine for Aerospike. | Spark on Dataproc, GCS feature snapshot, Aerospike predictions | Redis Cluster, individual feature reads during scoring | [diagram](#scenario-30--pb--near-real-time--ml) |
| **31** | **PB + Near Real-time + Dashboards** | One BigQuery per region, global C-suite needs unified view | BigQuery multi-region dataset (writes replicate globally, reads from nearest). Slot reservation per region (ops) + on-demand (C-suite Tableau). One dataset, global availability, no replication logic. | BigQuery multi-region, per-region slot reservation | BigQuery regional per team | [diagram](#scenario-31--pb--near-real-time--dashboards) |
| **32** | **PB + Near Real-time + Another System** | GDPR: EU personal data cannot leave EU region — legal requirement, not optimization | Beam routes events to regional Bigtable by user's home region at ingestion time. Regional consumers only read regional Bigtable. Compliance enforced architecturally — architecture cannot be violated, policy can. | Bigtable regional (data sovereignty), Beam routing step | Cassandra staging (no regional routing) | [diagram](#scenario-32--pb--near-real-time--another-system) |
| **33** | **PB + Batch + Analytics** | 5 PB/day × 365 = 1.8 EB/year storage. History rescan = $25,000/night. | Data tiering mandatory: active BigQuery ($20/TB/mo) → long-term ($10/TB/mo, auto after 90 days) → GCS Coldline ($4/TB/mo, >1 year). Broadcast dimension table snapshots (avoids PB shuffle join). | Beam on Dataflow serverless, BigQuery + GCS Coldline tiering, dimension snapshots | EMR fixed cluster, no data tiering | [diagram](#scenario-33--pb--batch--analytics) |
| **34** | **PB + Batch + ML** | EB of training history — cannot use all of it. Single GPU cluster insufficient. | Sample 10% of 6 months (3 PB → 600 GB training matrix). 200 A100 GPUs (data parallelism, NCCL all-reduce). Regional fine-tuning from global base (4 hrs per region vs 24 hrs from scratch). Vertex AI. | Dataproc (500 workers), Vertex AI 200-GPU fleet, regional fine-tuning | SageMaker 10-20 GPUs, full EB training | [diagram](#scenario-34--pb--batch--ml) |
| **35** | **PB + Batch + Dashboards** | One analyst query scans EB = $50,000 cost. Board and VPs need different row-level views. | BigQuery row access policies (no separate view per region). Query cost alerts ($100 threshold). Analyst sandbox with monthly budget cap. Auto-generated board PDF via Looker API. Same summary table as S11/S23 — governance is what scales. | BigQuery row/column security, query cost governance, Looker PDF automation | No cost controls, no row-level security | [diagram](#scenario-35--pb--batch--dashboards) |
| **36** | **PB + Batch + Another System** | 50,000 partners × 5 PB/day. 500M ack writes/night. GDPR: EU files must never leave EU. | Regional Dataproc per-region (data residency enforced at generation). Bigtable ack log (reverse-timestamp row key to avoid hotspot, 35K writes/sec). Cloud bucket handoff replaces SFTP. Spanner audit trail (globally consistent, legally required). | Regional Dataproc, Bigtable ack log, cloud bucket handoff | Cassandra ack, single EMR, SFTP | [diagram](#scenario-36--pb--batch--another-system) |

### Generic Full Architecture — All 36 Scenarios (GB + TB + PB)

---

## **Generic Architecture Diagram for GB Scenarios (5GB/day)**

Simplified single-scale diagram showing the core pattern for small-to-medium systems.

```
                                          [App]
                                            │
                ┌───────────────────────────┼────────────────────────────────┐
                │                           │                                │
            WRITE (sync)              WRITE (sync)          REQUEST (sync HTTP)
                │                           │                                │
           [RDBMS]                     [RDBMS]                         [Service]
       (PostgreSQL/MySQL)         (idempotency/staging)                    │
                │                           │                            READ
            READ (CDC)                      │                              │
                │                                                [Cache/Features]
           [CDC Tool]                                          (Redis/Memcached)
          (Debezium)                                                      │
                │                                            ┌────────────┘
              WRITE
                │
           [Message Bus]  ──┬────────────────── WRITE ──→ [Object Storage]
            (Kafka)         │                                (S3/GCS)
     + Schema Registry      │                   hourly partitions
                │           │
        ┌───────┼───────────┴──────────────────────────────┐
        │       │                                           │
      READ    READ                                        SPLIT
        │       │                                           │
   [Consumer] [Consumer]                      ┌─────────────┼─────────────┐
       1         2                            │             │             │
                                          REAL-TIME      NEAR-RT       BATCH
                                              │             │             │
                                       [Stream Proc]  [Spark         [Spark
                                        (Flink)      micro-batch]    nightly]
                                              │             │             │
                                            WRITE        WRITE        WRITE
                                              │             │             │
                                         [Real-time OLAP]  [OLAP]      [Data Warehouse]
                                         (ClickHouse)    (Pinot)      (Snowflake)
                                 │              │            │
                                 └──────┬───────┴────────────┘
                                        │
                                  ┌─────┼─────┐
                                  │     │     │
                                READ  READ  READ
                                  │     │     │
                           ┌──────▼─┬───▼─┬──▼──────┐
                           │        │     │         │
                       [Dashboards] [ML] [External]
                       (Real-time)  (Inference) (APIs/Files)
                       (Grafana)    (Models)    (Services)

═══════════════════════════════════════════════════════════════

PATTERN BY SPEED (how data flows at different latencies):

┌──────────────────────────────────────────────────────────┐
│ REAL-TIME (always-on, 1s latency)                       │
│ Message Bus → Flink → Real-time OLAP → Dashboards      │
│           → Redis → ML Service → Inference              │
│ Consumers read continuously from Message Bus             │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│ NEAR REAL-TIME (every 5 minutes)                        │
│ Message Bus → Spark micro-batch → OLAP → Analytics     │
│                              → Cache → Features          │
│ Consumers poll staging table every 5-10 min             │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│ BATCH (nightly, settled data)                           │
│ Object Storage → Spark → dbt → Data Warehouse           │
│ ↑                              ↓                          │
│ Airflow orchestration    ML Training + Reports          │
└──────────────────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════

BACKBONE (always, all speed tiers):
  1. RDBMS ← Application writes (sync, with idempotency)
  2. RDBMS → CDC Tool ← Read log of changes
  3. CDC Tool → Message Bus ← Stream all events
  4. Message Bus ⇉ 3 Processing Tiers (in parallel)
  5. Each Tier → Storage → Final Outputs
  6. Message Bus ⇉ Service Consumers (OLTP subscribers)
```

---

## **Abstraction Mapping**

- **RDBMS** = PostgreSQL / MySQL / Oracle / CockroachDB
- **NoSQL** = Cassandra / DynamoDB / Bigtable / ScyllaDB
- **CDC Tool** = Debezium / Change Streams / Custom CDC / Datastream
- **Message Bus** = Kafka / Pub/Sub / Kinesis / Pulsar
- **Stream Processing** = Flink / Spark Streaming / Beam / Kafka Streams
- **Real-time OLAP** = ClickHouse / Druid / Pinot / TimescaleDB
- **Batch OLAP** = Snowflake / BigQuery / Redshift / Athena
- **Data Warehouse** = Snowflake / BigQuery / Redshift / Hive
- **Cache/Features** = Redis / Aerospike / DynamoDB / Memcached
- **Data Lake** = S3 / GCS / ADLS / MinIO
- **ML Registry** = MLflow / SageMaker / Vertex AI / Kubeflow
- **Orchestration** = Airflow / Prefect / Dagster / Cloud Composer

---

## **Tool Selection Matrix — All Alternatives (GB/TB/PB)**

| Component | 5GB/day (Single Node) | 5TB/day (Cluster) | 5PB/day (Global) |
|-----------|----------------------|-------------------|------------------|
| **OLTP Store** | **PostgreSQL** / MySQL / Oracle / MariaDB | **Cassandra** / DynamoDB / ScyllaDB / HBase | **Spanner** / Bigtable / CockroachDB |
| **CDC Tool** | **Debezium** / Maxwell / Kafka Connect / DMS | **Debezium Cluster** / DynamoDB Streams / Kinesis | **Bigtable Change Streams** / Spanner Change Streams / Custom CDC |
| **Message Bus** | **Kafka** (3 brokers) / RabbitMQ / NATS / Redis Streams | **Kafka Cluster** (100+ partitions) / Kinesis / Pulsar | **Google Pub/Sub** / Kafka Global / Azure Event Hubs |
| **Stream Processing** | **Apache Flink** (1 node) / Spark Streaming / Kafka Streams | **Flink Cluster** / Spark Structured Streaming / Storm | **Beam on Dataflow** / Flink Cluster (global) / Spark Streaming |
| **Real-time OLAP** | **ClickHouse** (1 node) / TimescaleDB / QuestDB / InfluxDB | **Druid / Pinot** (cluster) / ClickHouse cluster / Timescale | **Pinot** (global) / Druid hyperscale / ClickHouse |
| **Batch OLAP** | **Snowflake** (small) / BigQuery / Athena / Presto / PostgreSQL | **BigQuery / Redshift** / Snowflake / Athena / Trino | **BigQuery XL / Snowflake XL** / Redshift Spectrum / Athena |
| **Data Warehouse** | **Snowflake** / PostgreSQL / BigQuery / Redshift | **BigQuery** / Redshift / Snowflake / Athena | **BigQuery** (multi-region) / Snowflake / Redshift |
| **Cache/Feature Store** | **Redis** (1 node) / Memcached / DynamoDB / Tarantool | **Redis Cluster** / DynamoDB / Memcached / Coherence | **Aerospike** / Redis Cluster / DynamoDB Global |
| **Data Lake** | **Amazon S3** (hourly) / GCS / ADLS / MinIO / HDFS | **S3** (minute partition) / GCS / ADLS / Wasabi / Backblaze | **GCS / S3** (second partition) / ADLS / S3-compatible |
| **ML Registry** | **MLflow** / DVC / Weights & Biases / Kubeflow | **SageMaker** / Kubeflow / Weights & Biases / Neptune | **Vertex AI** / SageMaker / AzureML / Databricks |
| **Orchestration** | **Cron / Prefect / Dagster** / APScheduler / Luigi | **Apache Airflow** / Prefect / Dagster / Temporal / Flyte | **Cloud Composer / Airflow** / Prefect Cloud / Dagster Cloud |

**Legend:** Primary tool in **bold** → Alternatives after "/"

---

## **36 Scenarios — Leaf-Level Mapping (9 Use Cases × 4 Scales)**

| Use Case | Real-time (1s) | Near-RT (5-15min) | Batch (nightly) |
|----------|---|---|---|
| **Analytics & Dashboards** | **G1**, **T13**, **P25** | **G5**, **T17**, **P29** | **G9**, **T21**, **P33** |
| **ML Training & Serving** | **G2**, **T14**, **P26** | **G6**, **T18**, **P30** | **G10**, **T22**, **P34** |
| **Another System / Export** | **G4**, **T16**, **P28** | **G8**, **T20**, **P32** | **G12**, **T24**, **P36** |

**Example Progression (G1 → T13 → P25):**
- **G1:** PostgreSQL → Debezium → Kafka (3 brokers) → Flink → ClickHouse
- **T13:** Cassandra → Debezium Cluster → Kafka (100+ partitions) → Flink Cluster → Druid/Pinot
- **P25:** Spanner → Bigtable Change Streams → Pub/Sub → Beam/Dataflow → Pinot (global)

---

## **Components Deep Dive: Flink & ClickHouse**

### **Apache Flink: Real-Time Stream Processor**

**What It Does (Simple):**
A worker that watches a continuous stream of events and processes each one as it arrives (within milliseconds), not waiting for batches.

**Key Ability:** Processes data while it's happening, outputs results immediately.

#### **Business Use Cases**

**1. Food Delivery - Live Order Tracking**
```
Scenario: Lunch rush, 10,000 orders/minute
├─ Order placed → Flink receives event instantly
├─ Flink calculates: "Delivery time = 28 minutes"
├─ Updates map in real-time (not every 5 minutes)
└─ Dashboard shows live: "5,432 orders in NYC"
```

**2. Fraud Detection - Payment Authorization**
```
Scenario: Credit card payment processing
├─ User swipes card → Flink checks in <100ms
├─ Flink asks: "Is this user usually in NYC? (Now in Tokyo?)"
├─ Flink checks: "Normal spending? (Suddenly buying 10 iPhones?)"
└─ Accept or block in real-time ✓
```

**3. Stock Trading - Price Alerts**
```
Scenario: 1,000s of price ticks per second
├─ BTC was $50k, now $51k? → Flink detects instantly
├─ Sends alert to traders NOW
└─ Not "here's your alerts from an hour ago"
```

**4. IoT Sensors - Temperature Warnings**
```
Scenario: Factory with 1,000 machines, sensor data every second
├─ Machine #42 temp spikes to 95°C (fire risk)
├─ Flink detects anomaly instantly
└─ Alerts operators BEFORE fire, not after
```

---

### **ClickHouse: Super-Fast Analytics Database**

**What It Does (Simple):**
A storage system optimized for answering analytical questions about large datasets incredibly fast (sub-second), even for billions of rows.

**Key Ability:** Answers "Tell me stats" questions fast (1s latency instead of 1 minute).

#### **Business Use Cases**

**1. E-commerce Dashboard - Live Sales Analytics**
```
Question: "How many orders per city in the last hour?"
├─ Traditional DB: 30 seconds
├─ ClickHouse: 0.5 seconds ✓
└─ Why? Stores data by column (only reads city + count columns)
```

**2. App Analytics - User Behavior Tracking**
```
Questions the dashboard answers:
├─ "How many users opened the app today by country?"
├─ "Top 10 features used in last 24 hours?"
├─ "Retention: % of day-1 users still active on day-7?"
└─ All answers: <1 second (even with 10B events/day)
```

**3. Ad Network - Real-time Metrics**
```
Dashboard shows (updates every 10 seconds):
├─ "1,234,567 impressions in last hour"
├─ "CTR (click-through rate) by country"
├─ "Top performing ads"
└─ Each query completes in <1 second
```

**4. SaaS Platform - Churn Prediction**
```
Question: "Which customers will churn? (no login in 30+ days)"
├─ Data: 100M users × 10 years = 1B+ rows of login history
├─ Traditional DB: Takes minutes
├─ ClickHouse: Returns in 0.8 seconds ✓
```

---

### **Flink + ClickHouse Together (The Real Pattern)**

This is where the magic happens in your 36 scenarios:

```
REAL-TIME ANALYTICS PIPELINE:

Message Bus (Kafka): 100,000 orders/minute
    ↓
┌─────────────────────────────────────────────────────────┐
│ FLINK (Stream Processing)                               │
│                                                          │
│ Aggregates live order stream:                           │
│ • Orders per city (every 10 seconds)                    │
│ • Average delivery time by region                       │
│ • Running totals and SUM calculations                   │
│ • Detects anomalies (unusually high/low traffic)        │
└─────────────────────────────────────────────────────────┘
                         ↓
                    (WRITE events)
                         ↓
┌─────────────────────────────────────────────────────────┐
│ CLICKHOUSE (Real-time OLAP)                             │
│                                                          │
│ Stores results in columnar format:                      │
│ • Pre-aggregated summaries (orders by city)             │
│ • Total revenue by region                               │
│ • User segment metrics                                  │
│                                                          │
│ Dashboard queries complete in <500ms                    │
└─────────────────────────────────────────────────────────┘
                         ↓
                    [Dashboards]
                    (Grafana)
```

**Why This Works:**
- **Flink:** Does the heavy computation (aggregating millions of events)
- **ClickHouse:** Stores results optimized for fast retrieval (columnar storage)
- **Result:** Sub-second dashboard responses with live data

#### **Peak Lunch Hour Example (Food Delivery)**

```
Time: 12:00 PM - 100,000 orders/minute incoming

FLINK processes every second:
├─ "NYC total: 1,700 orders/min"
├─ "Average delivery time: 28 min"
└─ "Top restaurant: Joe's Pizza (450 active orders)"

Writes to CLICKHOUSE every 30 seconds:
├─ Pre-computed: City summaries (small, fast)
└─ Raw events: Every order for flexible queries

Manager opens dashboard at 12:05 PM:
├─ "NYC Orders" → 0.1 seconds (pre-computed)
├─ "Slowest Restaurants" → 0.4 seconds (calculated from raw)
└─ "Revenue by Cuisine Type" → 0.5 seconds (calculated from raw)
```

---

### **Three Architecture Options: How ClickHouse Works**

#### **Option 1: ClickHouse as Dumb Storage**
Flink does ALL the heavy work, ClickHouse just stores and retrieves.

```
Flink: Aggregates 1M events → 50 city summaries
ClickHouse: Stores 50 rows (NYC: 5,432, LA: 3,421, etc.)
Dashboard: "Show NYC" → Returns instantly (no calculation)

✓ Pros: Super fast (0.1s latency)
✗ Cons: Only pre-computed metrics available
```

#### **Option 2: ClickHouse as Smart Calculator**
Flink just streams raw events, ClickHouse calculates on demand.

```
Flink: Passes through raw events unchanged
ClickHouse: Stores EVERY order event (columnar compression)
Dashboard: "Orders per city in last hour?" 
  → ClickHouse scans 50k events, calculates SUM → Returns in 0.5s

✓ Pros: Can answer ANY analytical question
✗ Cons: Slower (but still <1s), ClickHouse does work on every query
```

#### **Option 3: Hybrid (Best of Both - Most Common in Production)**
Flink pre-aggregates + ClickHouse stores both raw AND pre-computed + Materialized Views.

```
Message Bus
├─ → Flink: Aggregates (10M events → 50 city rows)
│        ↓
│    ClickHouse: Pre-computed table (instant queries)
│
└─ → Flink: Also sends raw events
         ↓
     ClickHouse: Raw events table + Materialized Views
                 (auto-updated aggregations)

Results:
├─ "Orders per city?" → Pre-computed (0.1s) ✓
├─ "Orders by city AND hour?" → Raw data (0.3s) ✓
└─ "Anomaly detection?" → Raw data (0.4s) ✓
```

---

### **When to Use Each Component**

| Need | Use | Latency |
|------|-----|---------|
| Process events as they arrive | Flink | <100ms |
| Detect fraud/anomalies instantly | Flink | <500ms |
| Answer "how many?" questions | ClickHouse | <1s |
| Build live dashboards | Flink → ClickHouse | <500ms |
| Explore historical data | ClickHouse | <2s |
| Alert on metrics | Flink | <1s |

---

### **Your GB Scale Example**

In the architecture diagram:
```
Message Bus (Kafka)
    ↓
Flink (1 node): Processes 5GB/day
├─ Aggregates: orders per city, per restaurant
├─ Calculates: avg delivery time, fraud flags
└─ Produces: 1-minute summaries (~1MB/day)
    ↓
ClickHouse (1 node): Real-time OLAP
├─ Stores Flink aggregations (fast queries)
├─ Can ALSO store raw events if flexibility needed
└─ Serves dashboard queries in <500ms
    ↓
Grafana Dashboard: "Show NYC metrics" → 0.2s response
```

**At TB Scale:** Flink cluster + ClickHouse cluster, same pattern, 10x throughput

---

## **Snowflake: Batch Analytics Data Warehouse**

### **What is Snowflake? (Simple)**

Snowflake is a **cloud data warehouse** that stores cleaned, transformed, historical data and answers analytical questions fast (seconds to minutes).

**Key Difference from ClickHouse:**
- **ClickHouse:** Real-time (1s latency), always-on, live dashboards
- **Snowflake:** Batch (nightly), settled historical data, business intelligence

### **How Data Flows: Spark → dbt → Snowflake Star Schema → Analytics**

```
STEP 1: SPARK reads and cleans raw data
┌──────────────────────────────────┐
│ Object Storage (S3/GCS)          │
│ Raw Parquet files (daily)        │
└──────────────┬───────────────────┘
               ↓
        ┌──────────────────────────┐
        │ SPARK (Batch Processing) │
        │                          │
        │ • Read raw files         │
        │ • Join tables (if needed)│
        │ • Clean data & types     │
        │ • Remove duplicates      │
        │ • Output: Staging tables │
        └──────────────┬───────────┘
                       ↓
        Staging tables loaded to Snowflake:
        ├─ stg_orders (raw cleaned, 100M rows)
        ├─ stg_customers (raw cleaned, 10M rows)
        └─ stg_restaurants (raw cleaned, 50K rows)

STEP 2: dbt TRANSFORMS raw data into STAR SCHEMA
        (dbt runs SQL on Snowflake, creates new tables)
        
        ┌──────────────────────────────────┐
        │ dbt (Data Build Tool)            │
        │                                   │
        │ Reads staging tables, creates:   │
        │ • fct_orders (fact table)        │
        │ • dim_customers (dimension)      │
        │ • dim_restaurants (dimension)    │
        │ • dim_dates (dimension)          │
        │ • kpi_daily (summary)            │
        └──────────────┬───────────────────┘
                       ↓

STEP 3: SNOWFLAKE stores the STAR SCHEMA
        ┌──────────────────────────────────┐
        │ SNOWFLAKE Data Warehouse         │
        │                                   │
        │ Production tables (star schema):  │
        │ • fct_orders (100M rows)         │
        │ • dim_customers (10M rows)       │
        │ • dim_restaurants (50K rows)     │
        │ • dim_dates (15K rows)           │
        │ • kpi_daily (1 row per day)      │
        │                                   │
        │ Ready for analytics queries      │
        └──────────────┬───────────────────┘

DOWNSTREAM: Analytics queries the STAR SCHEMA
                       ↓
        ┌──────────────────────────────────┐
        │ Dashboards query fct + dims:     │
        │ • Dashboards (Tableau)           │
        │ • BI Reports (Looker)            │
        │ • ML Training (features)         │
        │ • Executive KPIs                 │
        └──────────────────────────────────┘
```

### **The Key Insight: Spark Doesn't Create Star Schema**

**Important:** Spark outputs **staging tables** (raw, cleaned data), NOT star schema.

The **star schema is created by dbt** (data build tool) which:
1. Reads Spark's staging tables
2. Deduplicates and models the data
3. Creates fact tables (fct_orders) and dimension tables (dim_customers)
4. Writes results back to Snowflake

**Flow breakdown:**

```
Spark Output (staging):
├─ stg_orders: order_id, customer_id, restaurant_id, amount, date
│              (raw data, just cleaned)
├─ stg_customers: customer_id, name, email, segment
│                 (raw data, just cleaned)
└─ stg_restaurants: restaurant_id, name, city, cuisine
                    (raw data, just cleaned)
                    ↓
                dbt transforms ↓
                    ↓
dbt Output (star schema):
├─ fct_orders: order_id, customer_id, restaurant_id, amount
│              (fact table - numeric transactions)
├─ dim_customers: customer_id, name, segment, city, lifetime_value
│                 (dimension - rich customer details)
└─ dim_restaurants: restaurant_id, name, city, cuisine, rating
                    (dimension - rich restaurant details)
```

**Why this separation?**
- **Spark:** Fast ETL (Extract, Transform, Load) of raw data
- **dbt:** Applies business logic and creates analytics-ready tables
- **Snowflake:** Stores star schema and serves fast queries

### **What Snowflake Stores (3 Types of Tables)**

**1. Fact Tables (Transactions)**
```
fct_orders (100M rows):
├─ order_id, customer_id, restaurant_id
├─ order_date, amount, status
└─ Updated nightly by Spark

fct_events (1B rows):
├─ event_id, user_id, event_type
├─ timestamp, properties
└─ Updated daily from Message Bus
```

**2. Dimension Tables (Reference Data)**
```
dim_customers (10M rows):
├─ customer_id, name, segment, city
├─ lifetime_value, signup_date
└─ Updated weekly

dim_products (1M rows):
├─ product_id, name, category, price
└─ Static (rarely changes)

dim_dates (15 years):
├─ date, day_of_week, month, quarter
└─ Reference for time-based queries
```

**3. Summary Tables (Pre-aggregated)**
```
kpi_daily (10K rows - 1 per day):
├─ total_revenue, order_count, avg_order_value
└─ Updated nightly

cohort_analysis (monthly):
├─ cohort, retention_week_1, retention_week_4
└─ Updated weekly
```

### **Real Business Examples: How Analytics Use Snowflake**

**Example 1: Dashboard - "Revenue Today?"**
```sql
-- Query runs every 5 minutes
SELECT * FROM kpi_daily WHERE date = CURRENT_DATE()
-- Returns: $2.3M revenue, 23,000 orders
-- Response time: 0.1 seconds (pre-computed)
```

**Example 2: Report - "Revenue by Customer Segment?"**
```sql
-- Analyst asks this question (runs once)
SELECT 
  c.segment,
  COUNT(DISTINCT c.customer_id) as customers,
  SUM(f.amount) as total_revenue
FROM fct_orders f
JOIN dim_customers c ON f.customer_id = c.customer_id
WHERE f.date >= DATE_SUB(day, 30, CURRENT_DATE())
GROUP BY c.segment
ORDER BY total_revenue DESC

-- Returns 5 rows (Premium, VIP, Regular, etc.)
-- Response time: 1 second
```

**Example 3: ML Training Data**
```sql
-- Data scientist builds churn model
SELECT 
  customer_id,
  lifetime_value,
  days_since_last_order,
  order_count_last_30_days,
  has_churned (target)
FROM dim_customers
WHERE created_date >= '2023-01-01'

-- Returns 5M rows
-- Response time: 5 seconds
-- Exported to ML framework for training
```

### **Snowflake vs ClickHouse: When to Use Each**

| Dimension | ClickHouse | Snowflake |
|-----------|-----------|-----------|
| **Freshness** | Real-time (1s) | Batch (nightly) |
| **Updates** | Continuous | Daily/hourly |
| **Latency** | <1 second | 1-30 seconds |
| **Data volume** | 100s GB → TB | 100s TB → PB |
| **Typical question** | "Orders NOW?" | "Revenue YESTERDAY?" |
| **Typical user** | Operations team | Business analysts |
| **Cost model** | Storage-based | Compute + storage |
| **Best for** | Live dashboards | BI reports, ML training |

---

## **Fact & Dimension Tables: Star Schema Pattern**

### **Simple Analogy: Receipt Book + Customer File**

Imagine Joe's Pizza keeps two separate folders:

**Folder 1: Receipt Book (Fact Table)**
```
Receipt 1001: Customer 5432, Restaurant 101, $45.50
Receipt 1002: Customer 5433, Restaurant 102, $32.00
Receipt 1003: Customer 5434, Restaurant 101, $28.75
Receipt 1004: Customer 5432, Restaurant 103, $55.25
...
Receipt 1000000: Customer 9999, Restaurant 999, $78.99

(1M receipts, just IDs and numbers)
```

**Folder 2: Customer File (Dimension Table)**
```
Customer 5432: Alice, Premium, NYC
Customer 5433: Bob, Regular, LA
Customer 5434: Carol, VIP, Chicago
Customer 5435: David, Regular, NYC
...
Customer 9999: Zara, Premium, Boston

(10K customers, detailed info)
```

### **How They Work Together**

**Question: "How much did Premium customers spend?"**

```
Step 1: Find Premium in Customer File
  → Alice (5432), Carol (5434), Zara (9999), etc.

Step 2: Find their receipts in Receipt Book
  → 5432, 5434, 9999, ...

Step 3: Sum their spending
  → Alice: $100.75
  → Carol: $84.00
  → Zara: $250.50

Answer: Zara spent the most ✓
```

### **Why Separate Tables? (The KEY Difference)**

**Bad Way: One Big Table (Denormalized)**
```
Order_ID │ Customer_Name │ Segment  │ Restaurant_Name │ Amount
─────────┼───────────────┼──────────┼─────────────────┼────────
1001     │ Alice         │ Premium  │ Joe's Pizza     │ $45.50
1002     │ Bob           │ Regular  │ Pho King Good   │ $32.00
1003     │ Carol         │ VIP      │ Joe's Pizza     │ $28.75
1004     │ Alice         │ Premium  │ Taco Bell       │ $55.25

Problems:
❌ "Alice" appears 2 times (redundant)
❌ Large storage (100M rows × many columns)
❌ Slow queries (reading lots of text)
❌ If Alice moves, update 2 rows (consistency issues)
```

**Good Way: Two Separate Tables (Star Schema)**
```
FACT TABLE (100M rows):          DIMENSION TABLE (10K rows):
Order_ID │ Customer_ID │ Amount  Customer_ID │ Name  │ Segment │ City
─────────┼─────────────┼────────  ────────────┼───────┼─────────┼──────
1001     │ 5432        │ $45.50   5432        │ Alice │ Premium │ NYC
1002     │ 5433        │ $32.00   5433        │ Bob   │ Regular │ LA
1003     │ 5434        │ $28.75   5434        │ Carol │ VIP     │ Chi
1004     │ 5432        │ $55.25   5435        │ David │ Regular │ NYC

Benefits:
✓ Fact table is small (just IDs and numbers)
✓ "Alice" appears once (no duplication)
✓ If Alice moves, update one place
✓ Queries are fast (joining small dimension)
✓ Storage is efficient
```

### **Real Food Delivery Example: Complete Star Schema**

```
3 DIMENSION TABLES (small, reference):
├─ dim_customers (10M rows, 20 columns)
│  └─ customer_id, name, segment, city, lifetime_value
├─ dim_restaurants (50K rows, 15 columns)
│  └─ restaurant_id, name, cuisine, city, rating
└─ dim_drivers (100K rows, 10 columns)
   └─ driver_id, name, vehicle, rating, join_date

1 FACT TABLE (large, transactions):
└─ fct_orders (100M rows, 5 columns)
   └─ order_id, customer_id, restaurant_id, driver_id, amount
```

### **Business Questions Using Star Schema**

**Q1: "Which restaurants in NYC made the most revenue?"**
```sql
SELECT 
  r.name, r.cuisine,
  SUM(o.amount) as revenue,
  COUNT(*) as orders
FROM fct_orders o
JOIN dim_restaurants r ON o.restaurant_id = r.restaurant_id
WHERE r.city = 'NYC'
GROUP BY r.name, r.cuisine
ORDER BY revenue DESC
LIMIT 10

-- Result: Joe's Pizza ($2.3M), Pho King Good ($1.8M), etc.
```

**Q2: "Which Premium customers ordered Italian food?"**
```sql
SELECT 
  c.name, r.name as restaurant,
  o.amount, o.order_date
FROM fct_orders o
JOIN dim_customers c ON o.customer_id = c.customer_id
JOIN dim_restaurants r ON o.restaurant_id = r.restaurant_id
WHERE c.segment = 'Premium' AND r.cuisine = 'Italian'
ORDER BY o.amount DESC

-- Result: Alice from Joe's Pizza ($185.99), Carol from Mama Mia ($156.50), etc.
```

**Q3: "Driver performance - who's the best?"**
```sql
SELECT 
  d.name,
  COUNT(*) as orders_completed,
  AVG(o.delivery_time_minutes) as avg_time,
  AVG(o.rating) as avg_rating,
  SUM(o.amount) as revenue_delivered
FROM fct_orders o
JOIN dim_drivers d ON o.driver_id = d.driver_id
GROUP BY d.name
ORDER BY avg_rating DESC
LIMIT 10

-- Result: Raj (5K orders, 4.9 rating), Maria (4.8 rating), etc.
```

### **Key Insight**

**This IS the foreign key concept from OLTP databases, BUT:**

- **OLTP** (app database): Many normalized tables, optimized for writes
- **Star Schema** (warehouse): Few denormalized tables, optimized for reads

The pattern isn't special because of foreign keys (that's basic), it's special because it's **optimized for analytics**.

---

## **Scheduled SQL Queries in Snowflake**

### **Three Ways to Answer Questions**

#### **1. Scheduled Queries (Automated, run on schedule)**

These run automatically to pre-compute answers for dashboards.

```sql
-- Runs nightly at 2 AM
-- Purpose: Pre-calculate daily KPIs

CREATE OR REPLACE TABLE kpi_daily AS
SELECT 
  CURRENT_DATE() as date,
  SUM(amount) as total_revenue,
  COUNT(*) as order_count,
  AVG(amount) as avg_order_value,
  COUNT(DISTINCT customer_id) as unique_customers
FROM fct_orders
WHERE DATE(order_date) = CURRENT_DATE() - 1;

-- Result: 1 row (today's summary)
-- Dashboard queries this: instant (0.1 seconds)
```

**When to use:**
- Reports that run every day
- Dashboards that need instant response
- KPIs that don't change throughout the day

#### **2. On-Demand Queries (Manual, run when user asks)**

These run when an analyst clicks a button or dashboard updates.

```sql
-- Analyst clicks "Run Report"
-- Purpose: Show revenue by customer segment (right now)

SELECT 
  c.segment,
  COUNT(DISTINCT c.customer_id) as customers,
  SUM(f.amount) as revenue,
  AVG(f.amount) as avg_order_value
FROM fct_orders f
JOIN dim_customers c ON f.customer_id = c.customer_id
WHERE f.date_id >= DATE_ADD(day, -30, CURRENT_DATE())
GROUP BY c.segment
ORDER BY revenue DESC

-- Result: 5 rows (Premium, VIP, Regular, etc.)
-- Response time: 1 second
```

**When to use:**
- Exploratory analysis by analysts
- Interactive dashboards
- Ad-hoc reports

#### **3. Materialized Views (Pre-computed tables, refreshed on schedule)**

These are **scheduled queries that become tables** - auto-updated.

```sql
-- Like a scheduled query that stores results
-- Refreshes every hour automatically

CREATE OR REPLACE MATERIALIZED VIEW revenue_by_segment AS
SELECT 
  c.segment,
  DATE_TRUNC('hour', f.order_date) as hour,
  COUNT(*) as orders,
  SUM(f.amount) as revenue
FROM fct_orders f
JOIN dim_customers c ON f.customer_id = c.customer_id
GROUP BY c.segment, DATE_TRUNC('hour', f.order_date);

-- Dashboard queries this: instant (0.1 seconds)
-- Data is max 1 hour old
```

**When to use:**
- Complex queries that are run frequently
- Need instant dashboard response
- Can tolerate 1-hour delay in freshness

### **Real Dashboard Example: Three Widgets**

```
┌─────────────────────────────────────────────┐
│ LIVE DASHBOARD (updates every 5 min)        │
├─────────────────────────────────────────────┤
│ 1. Total Revenue Today: $2.3M               │ ← Scheduled query
│ 2. Orders by Segment (chart)                │ ← Materialized view
│ 3. Top 10 Restaurants (drilldown)           │ ← On-demand query
└─────────────────────────────────────────────┘
```

**Widget 1: Total Revenue (Scheduled Query)**
```sql
-- Runs automatically every 6 hours (2 AM, 8 AM, 2 PM, 8 PM)
CREATE OR REPLACE TABLE dashboard_revenue_today AS
SELECT 
  SUM(amount) as total_revenue,
  COUNT(*) as order_count
FROM fct_orders
WHERE DATE(order_date) = CURRENT_DATE();

-- Dashboard: SELECT * FROM dashboard_revenue_today
-- Takes: 0.01 seconds (1 row lookup)
```

**Widget 2: Orders by Segment (Materialized View)**
```sql
-- Refreshes every hour automatically
CREATE OR REPLACE MATERIALIZED VIEW dashboard_orders_by_segment AS
SELECT 
  c.segment,
  COUNT(*) as orders,
  SUM(f.amount) as revenue
FROM fct_orders f
JOIN dim_customers c ON f.customer_id = c.customer_id
WHERE DATE(f.order_date) = CURRENT_DATE()
GROUP BY c.segment;

-- Dashboard: SELECT * FROM dashboard_orders_by_segment
-- Takes: 0.1 seconds (pre-computed, 1 hour old max)
```

**Widget 3: Top Restaurants (On-Demand Query)**
```sql
-- Runs when user clicks dropdown
SELECT TOP 10
  r.name, r.cuisine,
  COUNT(*) as orders,
  SUM(f.amount) as revenue
FROM fct_orders f
JOIN dim_restaurants r ON f.restaurant_id = r.restaurant_id
WHERE DATE(f.order_date) = CURRENT_DATE()
GROUP BY r.name, r.cuisine, r.restaurant_id
ORDER BY revenue DESC

-- Takes: 0.5 seconds (user can wait for this)
```

### **Decision Table: When to Use Each**

| Question | Approach | Why | Latency |
|----------|----------|-----|---------|
| "Revenue today?" | Scheduled query | Needed every morning | 0.1s |
| "Orders by hour?" | Materialized view | Needed hourly for dashboard | 0.1s |
| "Which customers churned?" | On-demand | Asked occasionally | 1s |
| "Top 10 products?" | Materialized view | Frequently asked | 0.1s |
| "Deep dive into restaurant #42?" | On-demand | Deep analysis, user can wait | 1s |

### **Complete Data Pipeline**

```
┌──────────────────────────────────────┐
│ BATCH PROCESSING (Nightly)           │
│ Spark → dbt → Snowflake tables loaded│
└──────────┬───────────────────────────┘
           ↓
┌──────────────────────────────────────┐
│ SCHEDULED QUERIES (Run on schedule)  │
│                                       │
│ 2 AM: Calculate kpi_daily (1 row)   │
│ 3 AM: Create revenue_by_segment (24)│
│ Every hour: Refresh materialized     │
│ Every 6 hours: Update dashboard cache│
└──────────┬───────────────────────────┘
           ↓
┌──────────────────────────────────────┐
│ DASHBOARDS (Query pre-computed)      │
│                                       │
│ User opens dashboard 3 PM:           │
│ ├─ "Revenue?" → kpi_daily (instant) │
│ ├─ "Orders?" → Mat view (instant)   │
│ └─ "Details?" → Live query (0.5s)   │
└──────────────────────────────────────┘
```

### **Your Role as Architect**

**You decide which approach for each metric:**

```
Requirement: "Show revenue and top restaurants in real-time"

Decision 1: Revenue freshness?
├─ Schedule every 6 hours? → Instant + light load
├─ Materialize every hour? → Instant + more fresh
└─ Query on-demand? → Always current, but slower

Decision 2: Top restaurants?
├─ Materialize every hour? → Instant
└─ Query on-demand? → Always current, but slower

Final Design:
├─ Revenue: Scheduled (update 4x/day)
├─ Top restaurants: Materialized (update hourly)
└─ Deep drill-down: On-demand (user waits 1s)
```

---

## **Architecture Invariants & Golden Rules**

**What NEVER Changes (identical across all 36):**
```
BACKBONE: OLTP → CDC → Message Bus → [3 Processing Tiers] → Storage

3 Parallel Processing Tiers:
  ├─ Real-time (1s latency)    → Stream Processing → Real-time OLAP → Analytics
  ├─ Batch (5-15min)           → Micro-batch Engine → Batch OLAP → Analytics
  └─ Historical (nightly)      → Batch Computing → Data Warehouse → Training/Reports

Serving Layer: Cache/Feature Store (always separate, feeds both real-time & batch)
External Outputs: Dashboards + ML Models + Another System (consumers)
```

**What SCALES with GB→TB→PB:**
```
Single Node         →  Cluster               →  Global
PostgreSQL (1)      →  Cassandra (sharded)   →  Spanner (multi-region)
Debezium (1)        →  Debezium Cluster      →  Change Streams (native)
Kafka (3 brokers)   →  Kafka (100+ parts)    →  Pub/Sub (global)
Flink (1 node)      →  Flink Cluster         →  Beam/Dataflow (serverless)
ClickHouse (1)      →  Druid/Pinot (10-30)   →  Pinot (global)
Hourly partition    →  Minute partition      →  Second partition
1 instance          →  10s-100s instances    →  1000s (global)
```

**Golden Rule:** Architecture pattern = FIXED. Tools scale out independently. Choose tool alternatives based on:
- Scale (GB→TB→PB) determines which tool version
- Cost preference determines alternative (open-source vs managed)
- Ops burden determines vendor (self-managed vs cloud-native)

---

## **Pattern 2: Specific Tools (Original — For Reference)**

```
                                          [App]
                                            │
                ┌───────────────────────────┼─────────────────────────────────────────┐
                │                           │                                          │
          WRITE (all)                 WRITE (all)                       REQUEST — sync, blocks order
                │                           │                            RT + Near-RT + Another System
  GB: [PostgreSQL]             GB: [PostgreSQL]                                    │
  TB: [Cassandra / DynamoDB]   TB: [Cassandra / DynamoDB]             GB: [Fraud Service]
  PB: [Cassandra / Bigtable /  PB: [Cassandra / Bigtable]            TB: [Fraud Service fleet]
       Spanner]                ── idempotency / staging /             PB: [Fraud Service global
  ── OLTP source of truth ──      delivery ack log ──                      multi-region fleet]
  GB: 1 node                      (Another System)                              │
  TB: replicated cluster                                                       READ
  PB: globally distributed,                                                     │
      multi-region                                                  GB: [Redis]
                │                                                   TB: [Redis Cluster]
          READ (log)                                                PB: [Aerospike /
                │                                                       Redis Global Cluster]
  GB: [Debezium]                                                    ── fraud features ──
  TB: [Debezium cluster /                                           pre-computed by Flink
       DynamoDB Streams]
  PB: [Custom CDC /
       Bigtable Change Streams /
       Spanner Change Streams]
                │
              WRITE
                │
  GB: [Kafka]                   ────────────────────────────── WRITE ──→ [S3 / GCS]
  TB: [Kafka Cluster]                                               raw Parquet, permanent
  PB: [Kafka + Pulsar /                                             GB: partitioned by hour
       Google Pub/Sub]                                              TB: partitioned by minute
  ── + Schema Registry ──                                           PB: partitioned by second
  (Another System, all scales)                                          + column pruning
                │                                                         + data skipping
          ┌─────┼────────────────────────────────┐
        READ   READ                              │
          │     │                    ┌───────────┴───────────────────┐
  [Payment] [Driver]                ── REAL-TIME ── NEAR RT ── BATCH ──
   Service   Service                            │              │              │
  GB: 1 inst   GB: 1 inst                       │              │              │
  TB: cluster  TB: cluster          GB: [Flink]     GB: [Spark        GB: [Spark
  PB: global   PB: global           TB: [Flink        micro-batch]      nightly]
      fleet        fleet             Cluster]     TB: [Spark         TB: [Spark on
  ── OLTP consumers ──              PB: [Flink        EMR/               EMR /
  all 36 scenarios                     Global         Databricks]        Databricks]
                                     Cluster /     PB: [Spark on      PB: [Spark on
                                     Apache          Dataproc /         Dataproc /
                                     Beam]           Databricks         Databricks]
                                   always-on          every 5 min        + Airflow /
                                   ms latency         Near RT            Cloud Composer
                                                    │                   │                  │
                              ┌─────────────────────┤                   │          ┌───────┤
                              │                     │                   │          │       │
                            WRITE                 WRITE              WRITE       WRITE   WRITE
                              │                     │                   │          │       │
          ── ANALYTICS / DASHBOARDS ──         ── ML ──       ── ANALYTICS /  ── ANALYTICS /
                              │                     │             DASHBOARDS ──   DASHBOARDS / ML ──
  GB: [ClickHouse]            │        GB: [Redis]               │                     │
  TB: [Druid / Pinot]         │        TB: [Redis Cluster]  GB: [ClickHouse /    GB: [Snowflake + dbt]
  PB: [Apache Pinot /         │        PB: [Aerospike /          Snowflake]       TB: [BigQuery /
       Druid global           │             Redis Global]    TB: [Druid/Pinot /        Redshift + dbt]
       cluster]               │        ── Feature Store ──       BigQuery]        PB: [BigQuery /
  real-time OLAP              │        RT + Near-RT ML      PB: [Pinot /               Snowflake
  ops + analytics             │                  │               BigQuery]             at scale + dbt]
                              │                READ          Near-RT Analytics               │
                            READ               │             + Dashboards            [Model Registry]
                              │      GB: [ML Service]               │                GB: MLflow
               GB: [Grafana]  │      TB: [ML Service fleet]       READ               TB: SageMaker
               TB: [Grafana]  │      PB: [ML Service global   [Grafana /             PB: Vertex AI /
               PB: [Grafana]  │           fleet]               Tableau]               SageMaker
               ops dashboard  │      on-demand prediction      Near-RT +              Batch ML
               RT + Near-RT   │      RT + ML                   Batch
               Analytics                                        Analytics +
               + Dashboards        GB/TB/PB: [App GET]          Dashboards
                                   pre-computed prediction
                                   Near-RT + ML

    ─────────────── Another System — speed determines HOW, scale determines WHERE ──────────────

        REAL-TIME                     NEAR REAL-TIME                  BATCH
  ── always-on consumers ──     ── polling every 5-10 min ──    ── nightly file export ──
  GB: Consumer Group            GB: PostgreSQL staging           GB: Spark → CSV/XML
  TB: auto-scaling instances    TB: Cassandra staging            TB: Spark → partitioned files
  PB: global auto-scaling       PB: Bigtable / Spanner staging   PB: Spark → sharded files
       multi-region                                                   + parallel delivery
            │                                │                              │
  ┌─────────┼─────────┐       [Loyalty / Partner /               [SFTP / S3 / API /
  │         │         │        Report Service]                     Enterprise MQ]
[Notif.] [Inv.]  [Loyalty]                                         partner / regulator
  Svc      Svc     Svc
  └─────────┴─────────┘
  Idempotency check — all Another System, all scales
  DLQ — RT only, all scales
```

**Scale substitution — what changes at each tier:**
```
Component         GB (5GB/day)           TB (5TB/day)              PB (5PB/day)
─────────────────────────────────────────────────────────────────────────────────────
OLTP store        PostgreSQL (1 node)    Cassandra / DynamoDB      Cassandra / Bigtable /
                                         (replicated cluster)      Spanner (global, multi-region)
CDC               Debezium               Debezium cluster /        Bigtable Change Streams /
                                         DynamoDB Streams          Spanner Change Streams /
                                                                    Custom CDC
Message bus       Kafka (3 brokers)      Kafka Cluster (10+)       Kafka + Pulsar /
                                         100+ partitions           Google Pub/Sub (global)
RT processing     Flink (1 node)         Flink Cluster             Flink Global Cluster /
                                                                    Apache Beam (multi-engine)
Batch processing  Spark (1 node)         Spark on EMR/Databricks   Spark on Dataproc /
                                                                    Databricks (massive clusters)
RT OLAP           ClickHouse (1 node)    Druid / Pinot             Apache Pinot (global) /
                                         (multi-tenant)            Druid (hyperscale)
Batch OLAP        Snowflake (small)      BigQuery / Redshift /     BigQuery (serverless PB) /
                                         Snowflake (larger)        Snowflake (XL compute)
Feature store     Redis (1 node)         Redis Cluster             Aerospike / Redis Global
Fraud serving     Redis (1 node)         Redis Cluster             Aerospike (sub-ms at PB)
ML registry       MLflow                 SageMaker                 Vertex AI / SageMaker
File storage      S3 (hourly partition)  S3 (minute partition)     S3 / GCS (second partition
                                                                    + column pruning)

Architecture PATTERN: identical across all 36 scenarios.
What changes: every component scales out. Pattern never changes.
```

**How to read this diagram:**
```
Rows = Speed tier (same across GB / TB / PB):
  Real-time   → Flink path       → ClickHouse/Pinot (analytics) or Redis/Aerospike (ML)
  Near RT     → Spark micro-batch → Pinot/BigQuery or Snowflake/BigQuery
  Batch       → Spark + Airflow  → Snowflake/BigQuery + dbt

Columns = Use case (same across GB / TB / PB):
  Analytics / Dashboards → columnar OLAP (faster tier for RT, larger tier for batch)
  ML                     → Feature Store (RT/Near-RT) or Model Registry (batch training)
  Another System         → always-on consumers (RT) / polling (Near-RT) / file export (Batch)

Backbone in ALL 36 scenarios:
  OLTP store → CDC → Kafka → S3
  Payment + Driver Services (OLTP consumers)
  Fraud Service via sync HTTP (any Another System scenario, all scales)
```

### Speed Pattern (what changes with processing speed)

| Speed | SLA | Key Tool | Why |
|---|---|---|---|
| Real-time | Seconds | Flink (always-on) | Processes every event in milliseconds, stateful RAM |
| Near Real-time | 5-15 min | Spark micro-batch (scheduled) | Runs every 5 min, simpler, cheaper, backfill free |
| Batch | Hours/daily | Spark + Airflow (nightly) | Full day settled data, complete joins, dbt for logic |

### Use Case Pattern (what changes with consumer type)

| Use Case | Core Challenge | Solution Pattern |
|---|---|---|
| Analytics | OLTP must not slow down during queries | Separate OLAP store — ClickHouse (real-time), Snowflake (historical) |
| ML | Features fresh + predictions fast | Redis Feature Store + Lambda Architecture (Flink + Spark) |
| Dashboards | Two audiences, different latency needs | Two tools if speeds differ; one tool (Snowflake) if both accept delay |
| Another System | Machines act on events — duplicates cause real damage | Idempotency + DLQ + Schema Registry + Fraud always sync HTTP |

---

## MAANG Interview Questions by Scale

### GB Scale — 5 GB/day (Single-machine, startup/team-level)

| # | Question | Architecture hint | Answer |
|---|---|---|---|
| G1 | You have a food delivery app writing 5 GB/day of order events to PostgreSQL. The ops team wants a live dashboard showing orders per city, updated every 10 seconds. How do you build this without slowing down the app? | PostgreSQL → Debezium → Kafka → **Flink** (10-sec aggregation) → **ClickHouse** → Grafana | [S1 — GB + RT + Analytics](#scenario-1--gb--real-time--analytics) |
| G2 | Your food delivery platform needs to predict delivery time for each new order in under 200ms. You have historical order data and a trained ML model. Design the inference pipeline. | App → 2 parallel calls: WRITE PostgreSQL + REQUEST **ML Service** → reads **Redis** (features from Flink + Spark) → model → response | [S2 — GB + RT + ML](#scenario-2--gb--real-time--ml) |
| G3 | The ops team needs a live dashboard (30-sec refresh) and the business team needs a historical dashboard (hourly). They have very different query patterns. One tool or two? | Kafka → **Flink** → **ClickHouse** → Grafana (ops) AND Kafka → S3 → **Spark** → **Snowflake** → Tableau (business). Two stacks, same Kafka. | [S3 — GB + RT + Dashboards](#scenario-3--gb--real-time--dashboards) |
| G4 | When an order is confirmed, you need to: send push notification, decrement inventory, trigger fraud check — all within 2 seconds. A duplicate notification is catastrophic. Design this. | Kafka + **Schema Registry** → **Consumer Groups** (Notification/Inventory/Loyalty, each isolated) + **Idempotency store** + **DLQ**. Fraud = sync **HTTP** (never Kafka). | [S4 — GB + RT + Another System](#scenario-4--gb--real-time--another-system) |
| G5 | Same analytics requirement as G1, but the ops team says 5-15 minutes of delay is acceptable. How does your architecture change? What's cheaper and simpler? | Kafka → S3 (hourly) → **Spark micro-batch** every 5 min → ClickHouse. Flink dropped — Spark runs on schedule, not always-on. | [S5 — GB + NRT + Analytics](#scenario-5--gb--near-real-time--analytics) |
| G6 | The order tracking screen shows "Your order arrives in 28 min" and refreshes every 5 minutes. Design the ML pipeline so this doesn't block the order confirmation flow. | **Spark** every 5 min: reads all active orders → runs model → **writes predictions to Redis**. App just GETs `prediction:order:{id}`. No ML Service on the order path. | [S6 — GB + NRT + ML](#scenario-6--gb--near-real-time--ml) |
| G7 | Both the ops team and the business team can accept 5-15 minute delay on their dashboards. Can you simplify the two-tool architecture from G3? | S3 → Spark micro-batch → **Snowflake** (one tool). **Snowpipe** for continuous micro-batch load. Ops + business both query Snowflake. ClickHouse + Flink dropped. | [S7 — GB + NRT + Dashboards](#scenario-7--gb--near-real-time--dashboards) |
| G8 | Your loyalty service, inventory service, and finance reporting all need order events — but they each process at different speeds. How do you decouple them without real-time pressure? | Spark every 5 min → **PostgreSQL staging table** (partitioned by time bucket). Loyalty/Inventory/Finance **poll** their time bucket, mark done. No always-on consumers. | [S8 — GB + NRT + Another System](#scenario-8--gb--near-real-time--another-system) |
| G9 | The finance team needs a fully joined, cleaned dataset from yesterday's orders ready by 6am. How do you build a reliable nightly pipeline? | **Airflow** 2am → **Spark** (reads S3, joins all tables) → **Snowflake** → **dbt** (SQL business logic, 8 threads) → analysts query by 6am. | [S9 — GB + Batch + Analytics](#scenario-9--gb--batch--analytics) |
| G10 | You want to retrain your delivery time model weekly using 6 months of historical data. Design the training pipeline end-to-end, including how you validate and deploy the new model safely. | Airflow → Spark (feature engineering, 6 months S3) → **ML training job** → **MLflow Model Registry** → A/B test 5% traffic → promote or rollback. | [S10 — GB + Batch + ML](#scenario-10--gb--batch--ml) |
| G11 | The CEO wants a single-page KPI dashboard that loads instantly every morning: total orders, revenue, avg delivery time, top cities. How do you design this so it never times out? | dbt builds **`daily_kpi_summary`** (1 row/day, 6 KPIs). Dashboard queries **30 rows** from summary — never raw table. Pre-aggregate everything nightly. | [S11 — GB + Batch + Dashboards](#scenario-11--gb--batch--dashboards) |
| G12 | 500 restaurant partners each need a daily CSV file of their orders delivered via SFTP by 3am. Design the export pipeline including validation and delivery confirmation. | Spark → format CSV per partner → **validate** (row count + checksum + schema) → **SFTP delivery** → ack written to **PostgreSQL delivery log**. File schema = contract. | [S12 — GB + Batch + Another System](#scenario-12--gb--batch--another-system) |

---

### TB Scale — 5 TB/day (Distributed, national-scale)

| # | Question | Architecture hint | Answer |
|---|---|---|---|
| T1 | Your food delivery platform has grown to 5 TB/day. The real-time analytics pipeline you built at GB scale is falling over. Walk me through every component that breaks and how you fix each one. | PostgreSQL → **Cassandra** (sharded by order_id). Debezium → **Debezium cluster**. Kafka → **Kafka Cluster** (100+ partitions). Flink → **Flink Cluster**. ClickHouse → **Druid/Pinot**. Pattern identical, every node becomes a cluster. | [S13 — TB + RT + Analytics](#scenario-13--tb--real-time--analytics) |
| T2 | At TB scale, your Redis node for ML features is running out of RAM. Billions of feature keys for users, restaurants, and zones. How do you scale the Feature Store without breaking the inference SLA? | Redis → **Redis Cluster** (16K slots, key hash). Use **hash tags** `user:{USR-123}:*` to keep one user's features on same node. ML Service → **auto-scaling fleet**. TTL on every key. | [S14 — TB + RT + ML](#scenario-14--tb--real-time--ml) |
| T3 | At TB scale, ClickHouse can no longer ingest fast enough and Snowflake is too slow for the ops dashboard. How do you scale the two-dashboard architecture? | ClickHouse → **Druid/Pinot** (20-30 real-time ingest nodes). Snowflake small → **BigQuery** (serverless) or **Redshift** (large cluster). Same two-stack pattern, distributed components. | [S15 — TB + RT + Dashboards](#scenario-15--tb--real-time--dashboards) |
| T4 | At TB scale, your downstream consumer services (notification, inventory, loyalty) can't keep up with the event volume. PostgreSQL idempotency checks are saturating. Fix the architecture. | PostgreSQL idempotency → **Cassandra LWT** (lightweight transactions). Single consumers → **auto-scaling Consumer Fleets** (max = Kafka partition count). Fraud → **Fraud Fleet** + **Redis Cluster**. | [S16 — TB + RT + Another System](#scenario-16--tb--real-time--another-system) |
| T5 | Your 5-minute micro-batch Spark job processes 1 TB per run and is now taking 45 minutes. How do you fix it? Walk me through partition strategy and cluster sizing. | S3 hourly partition → **S3 minute partition** (read only 5-min slice, not full hour). Spark (1 node) → **Spark on EMR** (auto-scaling). Minute partition = the critical fix. | [S17 — TB + NRT + Analytics](#scenario-17--tb--near-real-time--analytics) |
| T6 | You have 50 million active orders that each need a pre-computed delivery estimate refreshed every 5 minutes. Single Redis node is out of RAM. Single Spark node takes too long. Fix this. | Redis → **Redis Cluster** (predictions sharded by order_id). Spark → **Spark on EMR** (distributed scoring). **Broadcast model once** to all workers. **MGET features per partition** (not per order). | [S18 — TB + NRT + ML](#scenario-18--tb--near-real-time--ml) |
| T7 | At TB scale, Snowpipe ingestion is lagging and your small Snowflake warehouse can't handle the query load from two dashboard teams. What's your migration plan? | Snowflake small + Snowpipe → **BigQuery** (serverless). Spark on EMR writes directly to BigQuery. **Slot reservation** for ops dashboard — isolates SLA from analyst queries. | [S19 — TB + NRT + Dashboards](#scenario-19--tb--near-real-time--dashboards) |
| T8 | At 5 TB/day, your PostgreSQL staging table has billions of rows. Partner services polling it are hitting timeouts. How do you redesign the staging layer? | PostgreSQL staging → **Cassandra staging** (partition key = `(service_name, time_bucket)`). Each consumer reads **one partition** — no scatter-gather. 5-min buckets = ~17M orders each. | [S20 — TB + NRT + Another System](#scenario-20--tb--near-real-time--another-system) |
| T9 | Your nightly Spark job processes 5 TB and is missing the 6am SLA. dbt models are timing out. Walk me through fixing the nightly pipeline for TB scale, including cost optimization. | Spark (1 node) → **Spark on EMR** (50+ workers, 80% spot). dbt serial → **dbt incremental** (16 threads, yesterday's partition only). BigQuery date-partitioned. Partition pruning = key cost lever. | [S21 — TB + Batch + Analytics](#scenario-21--tb--batch--analytics) |
| T10 | You need to train an ML model on 6 months × 5 TB/day = 900 TB of data. Single-node Spark takes weeks. Single GPU takes weeks. Design a distributed training pipeline. | **Spark on EMR (100+ workers)** → feature matrix (Parquet column pruning). **SageMaker** (10-20 GPUs, data parallelism, Pipe mode streams from S3). A/B test 5% → promote. Sample 10% first. | [S22 — TB + Batch + ML](#scenario-22--tb--batch--ml) |
| T11 | Your daily KPI dashboard runs a dbt model that rescans 900 TB of history every night and costs $4,500/night. How do you fix this without changing the dashboard output? | dbt **incremental** (scans yesterday's 5 TB only → $25/night). BigQuery **date-partitioned** source tables. Output (`daily_kpi_summary`) identical — 1 row/day, reads in <10ms. | [S23 — TB + Batch + Dashboards](#scenario-23--tb--batch--dashboards) |
| T12 | At TB scale with 1000+ restaurant partners, your sequential Spark file generation and SFTP delivery takes all night. Design a parallel delivery pipeline that completes by 3am. | Airflow: **10 parallel EMR jobs** (100 partners each). **5000 parallel SFTP uploads**. **Cassandra ack log** (partition by `partner_id + date` — 5000 concurrent writes, no hotspot). | [S24 — TB + Batch + Another System](#scenario-24--tb--batch--another-system) |

---

### PB Scale — 5 PB/day (Global, multi-region)

| # | Question | Architecture hint | Answer |
|---|---|---|---|
| P1 | Your platform is now global — US, EU, APAC. At 5 PB/day, a single-region Kafka cluster creates 200ms write latency for Asian users. Walk me through the global real-time analytics architecture. | Cassandra → **Spanner/Bigtable** (global, multi-region). Kafka → **Pub/Sub** (global topics). Flink → **Apache Beam on Dataflow** (serverless). Druid/Pinot → **Apache Pinot** (global cluster, sub-10ms at PB). | [S25 — PB + RT + Analytics](#scenario-25--pb--real-time--analytics) |
| P2 | At PB scale, your Redis Cluster for ML features would need terabytes of RAM, costing millions. What replaces it and why? How does global inference routing work? | Redis Cluster → **Aerospike** (RAM for index, SSD for values — 10x cheaper). ML Service → **regional fleets** (DNS latency routing to nearest). Blue-green deploy per region, never all at once. | [S26 — PB + RT + ML](#scenario-26--pb--real-time--ml) |
| P3 | Global executives need a real-time dashboard showing "orders per minute globally" while regional ops teams need local zone-level dashboards. How do you serve both without cross-region query latency? | **Beam**: two aggregations in one pipeline (per-region + global rollup). **Pinot regional brokers** (local ops, low latency) + **Pinot global broker** (exec view, +20ms). BigQuery global dataset (cross-region replicated). | [S27 — PB + RT + Dashboards](#scenario-27--pb--real-time--dashboards) |
| P4 | At global PB scale, a single order event might be picked up by consumers in both US-EAST and EU-WEST due to replication lag. How do you prevent cross-region duplicate processing without distributed locking? | **Partition-to-region assignment** (US owns partitions 0-33, EU 34-66, APAC 67-99 — no shared partitions). **Spanner idempotency** (TrueTime globally consistent INSERT wins once). DLQ per region. | [S28 — PB + RT + Another System](#scenario-28--pb--real-time--another-system) |
| P5 | At 5 PB/day, each 5-minute micro-batch is 17 TB. Your EMR cluster takes 90 minutes to process it. How do you get back to 5-minute SLA without provisioning a permanent 10x cluster? | EMR → **Beam on Dataflow** (serverless, auto-scales to 17 TB/batch, releases workers after). **GCS per-second partitions** (reads exactly 300 sec of data). Same code switches to streaming if SLA tightens. | [S29 — PB + NRT + Analytics](#scenario-29--pb--near-real-time--analytics) |
| P6 | You have 500 million active orders globally needing delivery estimates refreshed every 5 minutes. Scoring 500M orders in Spark requires 2.5 billion Aerospike feature reads per batch. Walk me through the performance problem and fix. | **Export feature snapshot to GCS** before scoring (avoids 2.5B live Aerospike reads). **Spark on Dataproc** (10K workers). Writes 500M predictions to Aerospike at 1.6M writes/sec. App GETs from nearest region. | [S30 — PB + NRT + ML](#scenario-30--pb--near-real-time--ml) |
| P7 | Regional ops teams in 3 continents need 5-minute-fresh dashboards while the global CFO needs a unified view. BigQuery by region means cross-region queries for the CFO. What's the right design? | **BigQuery multi-region dataset** (data stored in US + EU + APAC simultaneously, reads from nearest). **Slot reservation per region** for ops Grafana. C-suite Tableau uses on-demand slots. | [S31 — PB + NRT + Dashboards](#scenario-31--pb--near-real-time--dashboards) |
| P8 | GDPR requires that EU user data never leaves the EU region. Your current architecture processes all events in a single US-based Spark cluster. How do you restructure the polling-based downstream architecture to be GDPR-compliant? | **Beam routes events to regional Bigtable** at ingestion (EU user → EU-WEST Bigtable only). Regional Consumer Fleets poll **only their regional Bigtable**. Compliance enforced by architecture, not policy. | [S32 — PB + NRT + Another System](#scenario-32--pb--near-real-time--another-system) |
| P9 | At 5 PB/day, you have 1.8 EB of history after one year. Active BigQuery storage costs $20/TB/month. A full history rescan costs $25,000 per night. How do you architect storage and compute to make this sustainable? | **3-tier storage**: active BigQuery ($20/TB) → long-term BigQuery after 90 days ($10/TB, auto) → **GCS Coldline** after 1 year ($4/TB). **Broadcast dimension snapshots** (avoids PB shuffle join). dbt incremental only. | [S33 — PB + Batch + Analytics](#scenario-33--pb--batch--analytics) |
| P10 | Your global ML model needs retraining monthly but you have exabytes of training data. Training on all of it would take weeks and cost $1M+ per run. How do you design the training pipeline? How do you deploy safely across 3 regions? | **Sample 10% of 6 months** (3 PB → 600 GB training matrix). **Vertex AI: 200 A100 GPUs** (data parallelism, NCCL all-reduce). **Regional fine-tuning** from global base (4 hrs vs 24 hrs from scratch). Deploy APAC → EU → US. | [S34 — PB + Batch + ML](#scenario-34--pb--batch--ml) |
| P11 | A VP asks why an analyst's ad-hoc query on the global data warehouse cost $50,000 and slowed down the CEO's dashboard. How do you prevent this architecturally? What governance model do you put in place? | **BigQuery slot reservation** (dashboard = reserved slots, analysts = on-demand). **Query cost alerts** ($100 threshold → approval gate). **Analyst sandbox** (separate project + monthly budget cap). Row access policies per VP region. | [S35 — PB + Batch + Dashboards](#scenario-35--pb--batch--dashboards) |
| P12 | You have 50,000 restaurant partners globally and 50+ country tax authorities. Each needs a daily file of their orders. GDPR means EU partner files cannot be generated outside EU. Design the global file delivery pipeline for PB scale. | **Regional Dataproc per region** (EU data generated in EU, never crosses). **500M parallel chunk uploads**. **Bigtable ack log** (reverse-timestamp row key, 35K writes/sec). Cloud bucket handoff replaces SFTP. | [S36 — PB + Batch + Another System](#scenario-36--pb--batch--another-system) |

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

### Tech Stack by Speed Tier (Quick Reference)

```
REAL-TIME (RT) — Sub-second to 30 seconds
────────────────────────────────────────────────────────────────────
Pipeline: Data → Kafka → Flink → ??? (depends on use case)

Storage options:
  ├─ Analytics/Dashboards → ClickHouse + Grafana (50-100ms)
  ├─ ML Inference → Redis + ML Service (<2ms features)
  └─ Another System → Kafka Consumers + idempotency (<3 sec)

Key: Flink = always-on stream processor (expensive, pays for speed)


NEAR REAL-TIME (NRT) — 5-15 minutes
────────────────────────────────────────────────────────────────────
Pipeline: Data → Kafka → S3 → Spark (every 5-10 min) → ???

Storage options:
  ├─ Analytics/Dashboards → Snowflake + Snowpipe (3-10 sec)
  ├─ ML Inference → Redis (pre-computed predictions)
  └─ Another System → PostgreSQL staging + polling (5-10 min)

Key: Spark micro-batch = cheaper, acceptable for non-critical paths


BATCH — Hours to Daily
────────────────────────────────────────────────────────────────────
Pipeline: Data → Kafka → S3 → Airflow → Spark (nightly) → dbt → Snowflake

All use cases → Snowflake (via dbt transformations):
  ├─ Analytics: Historical queries, revenue trends
  ├─ ML Training: Historical features for models
  ├─ Dashboards: Executive KPIs, weekly/monthly reports
  └─ Exports: Partner files, regulatory reports

Key: Spark nightly + dbt = cheapest, most flexible (fully settled)


STORAGE TOOL DECISION MATRIX:
┌──────────────┬────────────────────┬──────────────┬──────────────────┐
│ Tool         │ Best For           │ Latency      │ When to Use      │
├──────────────┼────────────────────┼──────────────┼──────────────────┤
│ ClickHouse   │ Aggregations       │ <100ms       │ RT dashboards    │
│ Redis        │ Features/KV pairs  │ <2ms         │ RT ML + fraud    │
│ Snowflake    │ Analytics queries  │ 3-10 sec     │ NRT + Batch      │
│ Kafka        │ Events stream      │ <3 sec       │ RT downstream    │
│ S3           │ Archive/backup     │ N/A          │ All (data lake)  │
└──────────────┴────────────────────┴──────────────┴──────────────────┘

CRITICAL INSIGHT:
❌ WRONG: "All RT → ClickHouse, All NRT → Snowflake, All Batch → Snowflake"
✅ RIGHT: Storage choice depends on USE CASE, not just speed:
    • Dashboard trend (RT) → ClickHouse
    • ML features (RT) → Redis
    • Dashboard refresh (NRT) → Snowflake
    • Finance report (Batch) → Snowflake

The speed tier tells you HOW OFTEN to compute (continuous vs 5-min vs nightly).
The use case tells you WHERE to store (ClickHouse vs Redis vs Snowflake).
```

---

### NRT (Near Real-Time) Storage Options — Beyond Snowflake

**For NRT, Snowflake is the DEFAULT but NOT the only choice.**

After Spark micro-batch (every 5-10 min), you can write to:

```
OPTION 1: Snowflake (Most Common)
┌─────────────────────────────────────────────────────────────────┐
│ Spark → Snowflake (Snowpipe) → Tableau/Looker                  │
│ ✓ Powerful SQL, joins, window functions, scales to PB           │
│ ✓ Latency: 3-10 sec queries (acceptable for NRT)               │
│ ✗ Cons: Expensive, overkill for simple dashboards              │
│ Use when: Complex SQL + business logic needed                   │
└─────────────────────────────────────────────────────────────────┘

OPTION 2: ClickHouse (If you need < 100ms queries)
┌─────────────────────────────────────────────────────────────────┐
│ Spark → ClickHouse (batch inserts) → Grafana/Tableau           │
│ ✓ <100ms query latency (faster than Snowflake)                 │
│ ✓ Much cheaper than Snowflake for same volume                   │
│ ✗ No complex joins, not a data warehouse                        │
│ Use when: Need fast ops dashboard, simpler queries              │
└─────────────────────────────────────────────────────────────────┘

OPTION 3: PostgreSQL (If you already have it)
┌─────────────────────────────────────────────────────────────────┐
│ Spark → PostgreSQL (upsert) → Tableau/Metabase                 │
│ ✓ Simple, cheap, good SQL support                               │
│ ✓ Latency: 1-5 sec queries                                      │
│ ✗ Doesn't scale to PB, not columnar (slow aggregations)        │
│ Use when: Small team, keep it simple, already have PostgreSQL   │
└─────────────────────────────────────────────────────────────────┘

OPTION 4: Redis (For pre-computed metrics only)
┌─────────────────────────────────────────────────────────────────┐
│ Spark → Redis (store computed values) → API reads directly      │
│ ✓ <2ms latency, in-memory, very fast                            │
│ ✗ No complex queries, only pre-computed aggregations            │
│ Use when: "Total orders today" = simple number in Redis GET     │
└─────────────────────────────────────────────────────────────────┘

OPTION 5: TimescaleDB / InfluxDB (For time-series metrics)
┌─────────────────────────────────────────────────────────────────┐
│ Spark → TimescaleDB → Grafana                                   │
│ ✓ Optimized for time-series, fast on timestamp ranges           │
│ ✓ Latency: 10-50ms                                              │
│ ✗ Only for time-series (not business tables)                    │
│ Use when: "CPU%, latency, order rate by time" metrics           │
└─────────────────────────────────────────────────────────────────┘

OPTION 6: DuckDB (For embedded analytics)
┌─────────────────────────────────────────────────────────────────┐
│ Spark → DuckDB (parquet file) → App queries in-process         │
│ ✓ No separate DB to operate, free, columnar, fast              │
│ ✓ Latency: 100-500ms                                            │
│ ✗ Single-file, not multi-user, embedded only                    │
│ Use when: Don't want to operate a database                      │
└─────────────────────────────────────────────────────────────────┘

DECISION FRAMEWORK FOR NRT:
┌─────────────────────────────────────────────────────────────────┐
│ Need <10ms queries?                                              │
│   → ClickHouse or Redis (pre-computed)                          │
│                                                                  │
│ Need complex SQL + joins?                                       │
│   → Snowflake or PostgreSQL                                     │
│                                                                  │
│ Only have simple key-value lookups?                             │
│   → Redis or DuckDB                                             │
│                                                                  │
│ Already running PostgreSQL/MySQL?                               │
│   → Use it (faster than learning Snowflake)                     │
│                                                                  │
│ Data is time-series metrics?                                    │
│   → TimescaleDB or InfluxDB                                     │
│                                                                  │
│ Want cheapest option?                                           │
│   → PostgreSQL or ClickHouse or DuckDB                          │
└─────────────────────────────────────────────────────────────────┘

COMMON NRT PATTERNS IN PRODUCTION:

Pattern 1 — Ops Dashboard (fast queries, cheap)
  Spark → ClickHouse → Grafana
  Why: <100ms queries, internal only, no cold start

Pattern 2 — Business Dashboard (complex analysis)
  Spark → Snowflake (Snowpipe) → Tableau
  Why: Complex SQL, executives, scales well

Pattern 3 — API Metrics (pre-computed cards)
  Spark → Redis → Custom API
  Why: <2ms reads for "Today's Stats" cards

Pattern 4 — Hybrid (Multiple audiences)
  Spark writes to THREE places:
    ├─ ClickHouse (ops dashboard)
    ├─ Snowflake (business analytics)
    └─ Redis (API metrics)
  Why: Different teams, different latency needs, same source

RULE OF THUMB:
"Snowflake is default because it's powerful and scales.
But match the storage to your query patterns and latency needs.
Don't use Snowflake for simple aggregations (use ClickHouse).
Don't use ClickHouse for complex analytics (use Snowflake)."
```

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

#### Full Architecture — Sequential Data Flow

| Step | From | To | Action | Latency | Importance | Why |
|------|------|-----|--------|---------|-----------|-----|
| 1 | Customer | App | Places order via HTTP | ~500ms | MEDIUM | Triggers flow, not unique to analytics |
| 2 | App | PostgreSQL | WRITE order record | ~20ms | HIGH | OLTP source of truth, required |
| 3 | PostgreSQL | Debezium CDC | Writes change to WAL | <1ms | **CRITICAL** | Solves dual-write, separates OLTP from OLAP |
| 4 | Debezium CDC | Kafka | WRITE order event | ~50ms | **CRITICAL** | Decouples analytics from OLTP operations |
| 5a | Kafka | S3 | WRITE raw Parquet | ~100ms | LOW | Optional: backup/archive only |
| 5b | Kafka | Payment Service | READ + process | ~10ms | MEDIUM | Business op, independent of analytics |
| 5c | Kafka | Driver Service | READ + assign | ~10ms | MEDIUM | Business op, independent of analytics |
| 5d | Kafka | Flink | READ events | ~10ms | **CRITICAL** | Core to real-time aggregation requirement |
| 6 | Flink | Flink (RAM) | Pre-aggregate state | <1ms | **CRITICAL** | Solves slow table scan problem (key!) |
| 7 | Flink | ClickHouse | WRITE agg rows | ~50ms | **CRITICAL** | Real-time OLAP storage, fast queries |
| 8 | ClickHouse | Grafana | READ aggregations | ~10ms | **CRITICAL** | Queryable analytics for dashboard |
| 9 | Grafana | Ops Dashboard | Display + refresh | <2sec | HIGH | Delivers analytics to end users |

**Legend:**
- *Steps 5a-5d run in parallel (independent)*
- **CRITICAL** = Required for scenario (real-time analytics)
- **HIGH** = Important for system, not unique to scenario
- **MEDIUM** = Supporting role
- **LOW** = Optional/backup

**Critical Path (solves the core problem):**
```
PostgreSQL → CDC → Kafka → Flink → ClickHouse → Grafana
(Separates OLTP from OLAP, enables real-time analytics without PostgreSQL CPU spikes)
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

**🎯 Approach Used: APPROACH 1 (Query On-Demand from ClickHouse)**
```
Pattern: Flink → ClickHouse (pull model)
├─ Flink pre-computes aggregations in RAM
├─ Writes to ClickHouse for analytics storage
├─ Dashboard queries ClickHouse on-demand
└─ Latency: 50-100ms OK (dashboards refresh every 30 sec)

Why this approach:
├─ One-to-many query (one aggregation, many dashboard users)
├─ Async consumption (dashboard refresh delay acceptable)
└─ Optimized for: "What's the trend?" not "What's happening NOW?"
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

**Why Kafka writes to S3? (The Backup & Archive Layer)**
```
Flow: PostgreSQL → Kafka → S3 (raw Parquet, permanent storage)

IMPORTANT: This is NOT for real-time analytics. This is for:

1. DATA PRESERVATION (Compliance & Audit)
   - Keep permanent copy of every event for compliance/regulatory reasons
   - "Show me ALL orders from Jan 1-31 for audit" → query S3, not Kafka
   - Kafka only keeps data for 7 days (configured retention)
   - S3 keeps data forever (cheap)

2. BACKFILL CAPABILITY (Disaster Recovery)
   - ClickHouse crashed/corrupted → rebuild from S3
   - "Recalculate last 3 days of aggregations" → read from S3, rerun Flink
   - Without S3, lost data is gone forever

3. FUTURE USE CASES (Data Lake)
   - Scenario 9 (Batch Analytics): needs raw events from yesterday for dbt transformations
   - Scenario 10 (ML Training): needs historical events to train models
   - All this data comes from S3, not Kafka (Kafka is too expensive for storage)

4. COST
   - Kafka storage: $0.30/GB/month
   - S3 storage: $0.023/GB/month
   - 100GB/month → $30 in Kafka vs $2.30 in S3
   - S3 is 13x cheaper

Real-time analytics (Flink → ClickHouse) doesn't use S3.
But S3 is the foundation for everything else (recovery, compliance, batch, ML).
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

#### Full Architecture — Sequential Data Flow

| Step | From | To | Action | Latency | Importance | Why |
|------|------|-----|--------|---------|-----------|-----|
| 1a | App | PostgreSQL | WRITE order record | ~20ms | HIGH | OLTP source of truth |
| 1b | App | ML Service | REQUEST prediction | ~5ms | **CRITICAL** | Must respond before confirmation shows |
| 2 | PostgreSQL | Debezium CDC | Writes change to WAL | <1ms | MEDIUM | Async, not on critical path |
| 3 | Debezium CDC | Kafka | WRITE order event | ~50ms | MEDIUM | Event bus, not on critical inference path |
| 4a | Kafka | S3 | WRITE raw Parquet | ~100ms | LOW | Optional backup |
| 4b | Kafka | Payment Service | READ + process | ~10ms | MEDIUM | Business op, not ML-critical |
| 4c | Kafka | Driver Service | READ + assign | ~10ms | MEDIUM | Business op, not ML-critical |
| 4d | Kafka | Flink | READ events | ~10ms | **CRITICAL** | Real-time feature computation |
| 4e | S3 | Spark | READ (hourly) | ~100ms | HIGH | Historical features for inference |
| 5 | Flink | Flink (RAM) | Compute real-time features | <1ms | **CRITICAL** | Active orders, driver count, traffic |
| 6 | Spark | Spark (dist) | Compute historical features | ~1sec | HIGH | Avg prep time, ratings (stale OK) |
| 7a | Flink | Redis | WRITE real-time features | ~50ms | **CRITICAL** | Sub-2ms lookup required |
| 7b | Spark | Redis | WRITE historical features | ~50ms | HIGH | Pre-computed, refreshed hourly |
| 8 | ML Service | Redis | READ features (GET) | <2ms | **CRITICAL** | Fastest possible lookup |
| 9 | ML Service | ML Model | RUN inference | ~50ms | **CRITICAL** | Model execution |
| 10 | ML Model | ML Service | RETURN prediction | <1ms | **CRITICAL** | "28 min" delivery time |
| 11 | ML Service | App | RESPOND (HTTP) | <1ms | **CRITICAL** | Must respond in <200ms total |
| 12 | App | User | Display confirmation | ~100ms | HIGH | Shows prediction on screen |

**Legend:** *1a & 1b parallel* | *4a-4e parallel* | **CRITICAL** = On inference SLA path | **HIGH/MEDIUM/LOW** = Support roles

**Critical Path:** `App REQUEST → ML Service → Redis (2ms) → Model (50ms) → App (~70ms total)`

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

**Why Flink → Redis (NOT Flink → ClickHouse → Redis)?**

This is a critical architectural decision. The key is: **WHAT architecture are you building for?**

```
THREE POSSIBLE APPROACHES:

APPROACH 1: Flink → ClickHouse (query on-demand)
├─ Flink writes aggregations to ClickHouse
├─ ML Service: queries ClickHouse for latest value
├─ ClickHouse: filters/aggregates to find value
└─ Latency: 50-100ms ✗ TOO SLOW for inference (<200ms budget exhausted)

APPROACH 2: Flink → Redis (direct push, real-time)
├─ Flink writes pre-aggregated features directly to Redis
├─ ML Service: GET key:value from Redis (O(1) lookup)
├─ Redis: returns value instantly
└─ Latency: 1-2ms ✓ PERFECT for inference
│
└─ BEST FOR: Features that change frequently (active_orders, current_driver_count)
│            Need freshness in seconds, not minutes

APPROACH 3: ClickHouse → Redis (batch push, offline pre-compute)
├─ Flink writes to ClickHouse (for analytics)
├─ ClickHouse pre-computes periodically (every 5-15 min)
├─ ClickHouse PUSHES results to Redis (batch update)
├─ ML Service: GET key:value from Redis
└─ Latency: 1-2ms for lookup ✓, but features are 5-15 min stale ⚠
│
└─ BEST FOR: Features that DON'T change constantly (avg_rating, historical_avg)
             Can tolerate 5-15 min staleness
```

**So which is right for Scenario 2?**

Scenario 2 answer: **HYBRID (Approach 2 + 3)**
```
Real-time features (change every second):
├─ active_orders_at_restaurant = 42 (from Flink → Redis directly)
├─ driver_count_in_zone = 8
└─ current_traffic_level = high

Historical features (change every 1-2 hours):
├─ restaurant_avg_prep_time = 15 min (from Spark → Redis directly)
├─ restaurant_avg_rating = 4.7
└─ customer_lifetime_orders = 23

BOTH go to Redis. ML Service reads from Redis. Done in 2ms.
```

**NOT using Approach 1 (ClickHouse query on-demand):**
- ClickHouse query: 50-100ms
- But we COULD use it if we didn't have Flink already
- For <10ms latency requirement, always skip the query phase

**Why the difference?**

ClickHouse is optimized for:
- "SUM(amount) GROUP BY city for last 100M rows"
- "Top 10 restaurants by revenue"
- One query scans billions of rows, returns aggregated answer

Redis is optimized for:
- "GET restaurant:456:active_orders" (single key, single value)
- "GET customer:789:fraud_score" (point lookup)
- Millions of parallel lookups, each in <1ms

ClickHouse query = 50-100ms (wrong for inference)
Redis query = 1-2ms (perfect for inference)

**When to Use Each Approach (Decision Framework):**

```
APPROACH 1 (ClickHouse query on-demand):
├─ Use when: Building analytics dashboards (query latency 50-100ms OK)
├─ Don't use for: ML inference, real-time serving
└─ Example: Scenario 1 (dashboard) ✓, Scenario 2 (inference) ✗

APPROACH 2 (Flink → Redis direct push):
├─ Use when: Need <2ms latency AND features change every 1-10 seconds
├─ Real-time features: active_orders, current_traffic, fraud_score
└─ Example: Scenario 2 (ML inference) ✓

APPROACH 3 (ClickHouse/Spark → Redis batch push):
├─ Use when: Features pre-computed every 5-60 minutes, don't need realtime
├─ Historical features: avg_rating, historical_avg, user_segment
├─ Pushes results to Redis after batch computation
└─ Example: Scenario 5-8 (Spark every 5 min) ✓

APPROACH 1 + 2 + 3 (HYBRID - Most common in production):
├─ Flink → Redis (real-time features, <2ms)
├─ Spark → Redis (historical features, 5-60 min batch)
├─ Flink → ClickHouse (for ops dashboards, 100ms OK)
└─ Example: Most real scenarios use all three ✓✓✓
```

**How to Decide in General:**

```
ASK YOURSELF: What is the access pattern + latency need?

┌─ Aggregation + dashboard (<500ms OK)
│  → Query ClickHouse/Snowflake on demand
│  Example: "sum revenue by city" for dashboard refresh every 30 sec
│
├─ Point lookup + real-time inference (<2ms required)
│  → Redis/DynamoDB (pre-computed, push model)
│  Example: "get feature for user:123" for ML inference
│
├─ Mixed ("need both aggregations AND point lookups")
│  → Use BOTH: ClickHouse for analytics + Redis for inference
│  Push model: Flink/Spark pre-compute → Redis
│  Pull model: Dashboard queries ClickHouse on demand
│
├─ Time-series ("store 1M metrics per second, query last 24 hours")
│  → TimescaleDB or InfluxDB (time-series optimized)
│  Latency: 10-50ms OK
│
└─ Fast-changing real-time (<1s)
   → Flink → Redis direct (state in RAM, push immediately)
   → Not through any intermediate storage
```

**Key Insight: It's Not "ClickHouse vs Redis", It's "Push vs Pull"**

```
PULL Model (query on-demand):
  ClickHouse/Snowflake ← you ask for data ← fast to query but slow to respond
  Good for: Dashboards (run query every 30 sec)
  Latency: 50-500ms

PUSH Model (pre-compute and send):
  Redis ← Flink/Spark pushes data continuously ← fast to read, slow to compute
  Good for: Inference (need <2ms per request)
  Latency: 1-2ms read + X ms compute (depends on frequency)

Production systems use BOTH:
  ├─ Push to Redis for inference (real-time paths)
  └─ Pull from ClickHouse for analytics (dashboards)
```

**Real Decision Matrix:**

| Use Case | Query | Tool | Why | Latency |
|---|---|---|---|---|
| ML Inference | GET user:123:score | Redis | Point lookup | <2ms |
| Dashboard | SUM revenue by city | ClickHouse | Aggregation scan | 100ms |
| Time-series metrics | Query last 24 hours | TimescaleDB | Time-series optimized | 50ms |
| Document lookup | GET order:456 details | Elasticsearch | Full-text + structure | 20ms |
| Fraud detection | GET user:123 risk_score | Redis | Real-time point lookup | <2ms |
| Executive report | Month-over-month comparison | Snowflake | Complex aggregation | 30s OK |

**Rule of Thumb:**

- Need <10ms latency? → Redis/DynamoDB (key-value)
- Need <500ms latency? → ClickHouse/Pinot (columnar analytics)
- Need <30 sec latency? → Snowflake/BigQuery (data warehouse)
- Need <5 min latency? → S3 + Spark (data lake batch)

**Scenario 2 Specific Decision:**

```
ML Inference latency requirement: <200ms for customer waiting
Real path: App (100ms) → ML Service (15ms) → Redis (2ms) → Model (50ms)
Total: ~167ms ← PASS ✓

If used ClickHouse instead of Redis:
Real path: App (100ms) → ML Service (15ms) → ClickHouse (100ms) → Model (50ms)
Total: ~265ms ← FAIL ✗ (exceeds 200ms budget)

ClickHouse is 50-100x slower than Redis for point lookups.
Cannot use ClickHouse for inference serving layer.
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

#### Full Architecture — Sequential Data Flow (Two Parallel Stacks)

**SPEED STACK (Ops Dashboard — Real-time)**

| Step | From | To | Action | Latency | Importance | Why |
|------|------|-----|--------|---------|-----------|-----|
| 1 | Customer | App | Places order | ~500ms | MEDIUM | Initiates pipeline |
| 2 | App | PostgreSQL | WRITE order | ~20ms | HIGH | OLTP source |
| 3 | PostgreSQL | Debezium CDC | Writes WAL | <1ms | MEDIUM | Async capture |
| 4 | Debezium CDC | Kafka | WRITE event | ~50ms | **CRITICAL** | Decouples OLTP from OLAP |
| 5a | Kafka | S3 | WRITE backup | ~100ms | LOW | Optional archive |
| 5b | Kafka | Payment | READ + process | ~10ms | MEDIUM | Business op |
| 5c | Kafka | Driver | READ + assign | ~10ms | MEDIUM | Business op |
| 5d | Kafka | Flink | READ events | ~10ms | **CRITICAL** | Real-time aggregation |
| 6 | Flink | Flink (RAM) | Pre-aggregate | <1ms | **CRITICAL** | Solves slow scan |
| 7 | Flink | ClickHouse | WRITE agg rows | ~50ms | **CRITICAL** | Fast ops queries |
| 8 | ClickHouse | Grafana | READ aggs | ~10ms | **CRITICAL** | <100ms dashboard load |
| 9 | Grafana | Ops Team | Display dashboard | <2sec | HIGH | Auto-refresh every 30sec |

**SCALE STACK (Business Dashboard — Batch)**

| Step | From | To | Action | Latency | Importance | Why |
|------|------|-----|--------|---------|-----------|-----|
| S1 | S3 (from Kafka) | Spark | READ raw data | ~100ms | HIGH | Historical events |
| S2 | Spark | Spark (distributed) | CLEAN + JOIN | ~1-2sec | **CRITICAL** | Business logic layer |
| S3 | Spark | Snowflake | WRITE fact tables | ~50ms | **CRITICAL** | Historical storage |
| S4 | Snowflake | Tableau/Looker | READ queries | 3-10sec | **CRITICAL** | Executive dashboards |
| S5 | Tableau | Business Team | On-demand exploration | N/A | HIGH | Ad-hoc analysis |

**Legend:**
- *Steps 5a-5c run in parallel (Kafka consumers)*
- *Two stacks run independently in parallel*
- **CRITICAL** = Required for dashboard SLA
- **HIGH/MEDIUM/LOW** = Support roles

**Two Stacks, Two Audiences:**
```
Ops Team (live monitoring):     Speed Stack: Kafka → Flink → ClickHouse → Grafana
                               (30-second refresh, <100ms queries)

Business Team (strategic):       Scale Stack: S3 → Spark → Snowflake → Tableau
                               (hourly refresh, 3-10 sec queries, full history)
```

#### Quick Recap — New Concepts Introduced
```
ClickHouse  → real-time OLAP (ops dashboards, last few hours, <10ms)
Snowflake   → historical OLAP (business dashboards, weeks of data, 3-10 sec)
Grafana     → ops visualization (auto-refresh, time-series panels, engineers)
Tableau     → business visualization (drag-drop exploration, executives)
Two stacks  → Speed Stack (Kafka+Flink+ClickHouse) and Scale Stack (S3+Spark+Snowflake)
```

**🎯 Approaches Used: HYBRID (Approach 1 + Pull Model)**
```
Speed Stack (Ops Dashboard):
├─ Pattern: Flink → ClickHouse (pull model)
├─ Latency: 50-100ms OK (dashboard refreshes every 30 sec)
└─ Query: "Orders by city in last 5 min" — GROUP BY, aggregation

Scale Stack (Business Dashboard):
├─ Pattern: S3 → Spark → Snowflake (batch + pull model)
├─ Latency: 3-10 sec OK (business team reviews, not real-time critical)
└─ Query: "Revenue trend last 4 weeks" — full historical analysis

Why two stacks:
├─ Different audiences (ops vs business)
├─ Different data depth (hours vs weeks)
└─ Different query patterns (aggregation vs historical trend)
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

#### Full Architecture — Sequential Data Flow

| Step | From | To | Action | Latency | Importance | Why |
|------|------|-----|--------|---------|-----------|-----|
| 1a | Customer | App | Places order | ~500ms | MEDIUM | Initiates flow |
| 1b | App | PostgreSQL | WRITE pending order | ~20ms | **CRITICAL** | Must save before fraud check |
| 2a | PostgreSQL | Debezium CDC | Writes WAL | <1ms | MEDIUM | Async capture |
| 2b | App | Fraud Service | REQUEST fraud check | ~5ms | **CRITICAL** | Blocks order confirmation |
| 3 | Debezium CDC | Kafka | WRITE event | ~50ms | MEDIUM | Event bus |
| 4a | Kafka | S3 | WRITE backup | ~100ms | LOW | Optional |
| 4b | Kafka | Notification Svc | READ (own offset) | ~10ms | **CRITICAL** | Send push immediately |
| 4c | Kafka | Inventory Svc | READ (own offset) | ~10ms | **CRITICAL** | Decrement stock |
| 4d | Kafka | Loyalty Svc | READ (own offset) | ~10ms | MEDIUM | Add points (can retry) |
| 4e | Kafka | Flink | READ events | ~10ms | **CRITICAL** | Real-time fraud features |
| 5 | Flink | Flink (RAM) | Compute fraud features | <1ms | **CRITICAL** | Order velocity, amounts |
| 6 | Flink | Redis | WRITE features | ~50ms | **CRITICAL** | Sub-2ms lookup required |
| 7 | Fraud Service | Redis | READ features (GET) | <2ms | **CRITICAL** | Instant feature lookup |
| 8 | Fraud Service | ML Model | RUN inference | ~50ms | **CRITICAL** | Fraud score |
| 9 | ML Model | Fraud Service | Return score | <1ms | **CRITICAL** | Block/approve decision |
| 10a | Fraud Service | App | RESPOND to fraud check | <1ms | **CRITICAL** | If score < 0.8: approve |
| 10b | App | PostgreSQL | UPDATE status=confirmed | ~20ms | **CRITICAL** | If approved, save confirmation |
| 10c | App | User | Show confirmation | <100ms | HIGH | "Order confirmed!" |
| 11 | PostgreSQL → CDC | Kafka | WRITE confirmed event | ~50ms | MEDIUM | Triggers downstream |
| 12a | Kafka | Notification | Send push | ~2sec | **CRITICAL** | Idempotency prevents duplicates |
| 12b | Kafka | Inventory | Decrement stock | ~2sec | **CRITICAL** | Idempotency prevents double-decrement |
| 12c | Kafka | Loyalty | Add points | ~5sec | MEDIUM | Can handle retries |

**Legend:**
- *Steps 4a-4e run in parallel (Kafka consumers with own offsets, independently)*
- *Fraud check (2b + 7-10a) is BLOCKING — must complete before step 10b*
- *Schema Registry validates all Kafka schemas for breaking changes*
- **CRITICAL** = Required for order flow SLA
- **HIGH/MEDIUM/LOW** = Support roles

**Critical Path (Order Confirmation):**
```
1. App WRITE order to PostgreSQL (20ms)
2. App REQUEST fraud (5ms)
3. Fraud Service reads Redis features (2ms)
4. Fraud Service runs model (50ms)
5. If score < 0.8: APPROVE, UPDATE PostgreSQL (20ms)
6. Total time: ~97ms (✓ fits <200ms budget)

AFTER order confirmed:
7. Debezium picks up confirmed event
8. Kafka → Notification (with idempotency check)
9. Kafka → Inventory (with idempotency check)
10. Kafka → Loyalty (can fail, will retry via DLQ)

Fraud is ALWAYS synchronous and blocks.
Each downstream consumer is independent (own offset, can fail separately).
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

**🎯 Approach Used: APPROACH 2 (Flink → Redis Direct Push for Fraud)**
```
Pattern: Flink → Redis (real-time push) + Kafka Consumer Groups (push model)
├─ Flink computes fraud features → pushes to Redis continuously
├─ Fraud Service reads Redis features synchronously
├─ Multiple downstream systems consume Kafka events independently
└─ Each has idempotency check to prevent duplicate actions

Why this approach:
├─ Fraud features needed in milliseconds (sub-200ms SLA)
├─ Downstream systems must act independently without blocking each other
├─ Exactly-once semantics required (idempotency key prevents duplicates)
└─ Schema Registry ensures consumers don't break on updates
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

### Scenario 6 — GB + Near Real-time + ML

#### Problem Statement
```
Size:    5 GB/day, ~25M events/day
Speed:   Near Real-time — 5 to 15 minute delay acceptable
Use:     ML — predictions needed but not in same HTTP request as order
Who:     Food delivery startup — show updated delivery estimates on tracking screen
         (not on order confirmation — that needs Scenario 2 real-time)
```

#### What's Different vs Scenario 2 (GB + Real-time + ML)
```
Scenario 2 need:  Predict delivery time BEFORE confirmation screen loads (<200ms, sync)
Scenario 6 need:  Update delivery estimate on TRACKING screen (user already waiting, 5-min update is fine)

Functional difference:
  Scenario 2: customer is on checkout page → prediction blocks order confirmation → must be instant
  Scenario 6: customer is on "Your order is being prepared" screen → estimate refreshes every 5 min
              "28 min" → "24 min" → "19 min" — small error is acceptable

Technical difference:
  Scenario 2: ML inference is ON-DEMAND (App calls ML Service per request, model runs live)
              Cannot pre-compute because each order has unique context at submission time
  Scenario 6: predictions can be PRE-COMPUTED in batch every 5 min
              Spark computes updated estimates for ALL active orders → stores in Redis
              App just READs Redis — no model runs at request time

Architecture change that solves it:
  DROP: ML Inference Service running model on-demand per request
  ADD:  Spark micro-batch (every 5 min) runs model on ALL active orders → writes to Redis
  App: READs pre-computed prediction from Redis directly (simple GET, no HTTP to ML service)
  Flink: still computes real-time features (active drivers, traffic) → Redis
  Spark: reads S3 + Redis features → runs model for each active order → writes result to Redis
```

#### Full Architecture

```
[Customer places order]
        ↓
[App] ──WRITE──→ [PostgreSQL]        ← saves order (OLTP)
                       │
                 READ (WAL)
                       │
                [Debezium CDC]
                       │
                     WRITE
                       │
                   [Kafka] ──────WRITE──────→ [S3]
                       │                  raw Parquet files
              READ     │     READ
        ┌──────────────┤
        ↓              ↓
  [Payment]        [Driver]
   Service          Service
    OLTP             OLTP


[Kafka] ──READ──→ [Flink]                    [S3]
                  state in RAM                 │
                  real-time features      READ (every 5 min)
                        │                     │
                      WRITE              [Spark micro-batch]
                        │               reads active orders
                        ↓               reads Flink features from Redis
                   [Redis]              runs ML model for each
                        │               WRITEs prediction per order_id
                      WRITE ←───────────────────┘
                        │
                      READ
                        │
                    [App / Tracking Screen]
                    GET prediction:order_id
                    → "Your order arrives in 19 min"
                    (refreshes every 5 min)
```

**🎯 Approach Used: APPROACH 3 (Batch Push from Spark to Redis)**
```
Pattern: Spark micro-batch → Redis (push model, pre-computed)
├─ Spark runs every 5 minutes (not continuous like Flink)
├─ Pre-computes predictions for ALL active orders
├─ Pushes results to Redis (prediction:order_id → "19 min")
├─ App just READs from Redis (1-2ms)
└─ Latency: <2ms for read, but features are 5-min stale (acceptable)

Why this approach:
├─ Predictions don't need to be real-time (5-min SLA acceptable)
├─ Easier to operate than ML Service (no live model running)
├─ Pre-compute cost split across 1000s of predictions every 5 min
├─ App just reads pre-computed value (very cheap at request time)
```

#### Key Design Decisions

```
Pre-computed vs on-demand:
  On-demand (Scenario 2):  App → ML Service → Redis → model → response  (per request, <200ms)
  Pre-computed (Scenario 6): Spark → Redis (every 5 min) → App just GETs result (1ms)

  Pre-computed is simpler: no ML Service to operate, no latency concern, easy to scale
  Acceptable when: prediction does not need to be unique per-request-moment
  NOT acceptable when: prediction depends on real-time order details at submission (Scenario 2)

What Spark writes to Redis:
  KEY:   prediction:order:98765
  VALUE: { estimate: "19 min", updated_at: "11:32:00", confidence: 0.87 }
  TTL:   15 min (if order not updated in 15 min, something is wrong)

App behaviour:
  Tracking screen loads → GET prediction:order:98765 from Redis → display
  Refreshes every 5 min → GET again → display updated value
  No ML model runs at request time — pure Redis lookup
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| PostgreSQL | OLTP source of truth | App WRITEs, Debezium READs WAL |
| Debezium | CDC connector | READs WAL, WRITEs Kafka |
| Kafka | Event bus | Consumers READ |
| S3 | Raw Parquet storage | Kafka WRITEs, Spark READs |
| Flink | Real-time features (drivers, traffic) | READs Kafka, WRITEs Redis |
| Spark micro-batch | Runs model for all active orders every 5 min | READs S3 + Redis, WRITEs Redis predictions |
| Redis | Feature store + prediction store | Flink/Spark WRITE, App READs |

#### Interview Answer
```
1. App WRITEs to PostgreSQL → Debezium → Kafka
2. Flink READs Kafka → real-time features (active drivers, zone traffic) → Redis
3. Spark micro-batch runs every 5 min:
   READs active orders from PostgreSQL/S3
   READs features from Redis
   Runs ML model for each active order
   WRITEs prediction:order_id → Redis (TTL 15 min)
4. Tracking screen: App GETs prediction from Redis → displays "19 min"
   Refreshes every 5 min → new Spark run → updated prediction

Key insight: "Pre-compute predictions in batch for all active orders.
App reads result from Redis — no ML model at request time.
Simpler than Scenario 2, acceptable when 5-min staleness is fine."
```

---

### Scenario 7 — GB + Near Real-time + Dashboards

#### Problem Statement
```
Size:    5 GB/day, ~25M events/day
Speed:   Near Real-time — 5 to 15 minute delay acceptable for ALL dashboards
Use:     Dashboards — both ops team and business team can tolerate 5-15 min delay
Who:     Food delivery startup — weekly ops reviews, shift-level monitoring (not live alerts)
```

#### What's Different vs Scenario 3 (GB + Real-time + Dashboards)
```
Scenario 3 need:  Ops team needs LIVE data (30-sec refresh). Business needs history.
                  Two tools required: ClickHouse (speed) + Snowflake (depth)
Scenario 7 need:  Both teams accept 5-15 min delay.
                  Ops team does shift-level review, not live incident response.

Functional difference:
  Scenario 3 ops:  "Is something broken RIGHT NOW?" → needs 30-sec data → needs ClickHouse
  Scenario 7 ops:  "How did the 2pm-4pm shift go?" → 5-min delay fine → Snowflake works
  Both scenarios business: "What was revenue last week?" → Snowflake always

Technical difference:
  Scenario 3: two pipelines, two stores, two tools (ClickHouse + Snowflake)
  Scenario 7: ONE pipeline (Spark micro-batch) → ONE store (Snowflake) → TWO dashboard tools
              Snowpipe (Snowflake's streaming ingest) accepts data every 1-5 min → 5-min SLA met

Architecture change that solves it:
  DROP: Flink, ClickHouse (no need for sub-second pipeline)
  KEEP: Spark micro-batch every 5 min → Snowflake
  Snowflake serves BOTH ops (Grafana reads Snowflake) AND business (Tableau reads Snowflake)
  Simpler: one tool to operate instead of two
```

#### Full Architecture

```
[Customer places order]
        ↓
[App] ──WRITE──→ [PostgreSQL]        ← saves order (OLTP)
                       │
                 READ (WAL)
                       │
                [Debezium CDC]
                       │
                     WRITE
                       │
                   [Kafka] ──────WRITE──────→ [S3]
                       │                  raw Parquet files
              READ     │
        ┌──────────────┤
        ↓              ↓
  [Payment]        [Driver]
   Service          Service
    OLTP             OLTP

                   [S3]
                     │
               READ every 5 min
                     │
           [Spark micro-batch]
           cleans, joins, aggregates
                     │
                   WRITE (Snowpipe)
                     │
               [Snowflake]
               single warehouse
               5-min fresh data
                     │
          ┌──────────┴──────────┐
        READ                  READ
          │                     │
      [Grafana]           [Tableau/Looker]
    Ops Dashboard         Biz Dashboard
   (5-min refresh)        (on-demand)
```

**🎯 Approach Used: Simplified APPROACH 1 + Pull Model**
```
Pattern: S3 → Spark → Snowflake (batch + pull model)
├─ Spark micro-batch every 5 minutes (not continuous)
├─ Cleans, joins, aggregates data from S3
├─ Writes to Snowflake via Snowpipe (streaming ingest)
├─ Both ops and business dashboards query Snowflake
└─ Latency: 1-3 sec OK (both teams accept 5-15 min delay)

Why this approach:
├─ Both teams can tolerate 5-min data delay
├─ Single tool (Snowflake) serves everyone
├─ Simpler than Scenario 3 (no ClickHouse + Snowflake split)
├─ No Flink needed (sub-second pipeline not required)
└─ Cost: cheaper than running separate real-time + batch pipelines
```

#### Key Design Decisions

```
Why Snowflake can now serve both:
  Scenario 3: ops needed 30-sec refresh → Snowflake cold start 3-10 sec → unacceptable
  Scenario 7: ops needs 5-min refresh → Snowflake warm query 1-3 sec → acceptable
              Dashboard loads in 3 sec, refreshes every 5 min — ops team is fine

Snowpipe (Snowflake's micro-batch ingest):
  Kafka → S3 (via Kafka Connect) → Snowpipe detects new files → loads to Snowflake in 1-3 min
  OR: Spark micro-batch runs every 5 min → writes cleaned data → Snowflake
  Both patterns give 5-15 min freshness

Cost saving vs Scenario 3:
  Scenario 3: Flink ($50) + ClickHouse ($50) + Snowflake ($50) = $150/month for data stores
  Scenario 7: Spark micro-batch ($10) + Snowflake ($50) = $60/month
  One tool to operate, one team to train, one query language for all consumers
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| PostgreSQL | OLTP source of truth | App WRITEs, Debezium READs WAL |
| Debezium | CDC connector | READs WAL, WRITEs Kafka |
| Kafka | Event bus + S3 feeder | Consumers READ, Kafka Connect WRITEs S3 |
| S3 | Raw Parquet storage | Kafka WRITEs, Spark READs |
| Spark micro-batch | Cleans, joins, aggregates every 5 min | READs S3, WRITEs Snowflake |
| Snowflake | Single warehouse for both audiences | Spark WRITEs, Grafana + Tableau READ |
| Grafana | Ops dashboard (5-min refresh) | READs Snowflake |
| Tableau/Looker | Business dashboard (on-demand) | READs Snowflake |

#### Interview Answer
```
1. App → PostgreSQL → Debezium → Kafka → S3
2. Spark micro-batch runs every 5 min → cleans + joins → WRITEs Snowflake
3. Grafana READs Snowflake (5-min refresh) → ops team sees near-live view
4. Tableau READs Snowflake (on-demand) → business explores history

Key insight: "When both audiences accept 5-min delay, consolidate to one tool.
Scenario 3 needed two stores (ClickHouse + Snowflake). Here Snowflake serves both.
Simpler is better — fewer tools, fewer failure points, one team owns everything."
```

---

### Scenario 8 — GB + Near Real-time + Another System

#### Problem Statement
```
Size:    5 GB/day, ~25M events/day
Speed:   Near Real-time — 5 to 15 minute delay acceptable
Use:     Another System — downstream systems react to events but not instantly
Who:     Food delivery startup — loyalty points added within 10 min, weekly partner reports
         Example: "Award loyalty points within 10 minutes of order completion"
```

#### What's Different vs Scenario 4 (GB + Real-time + Another System)
```
Scenario 4 need:  Downstream systems must react INSTANTLY (notification in seconds, fraud blocks order)
Scenario 8 need:  Downstream systems react within minutes (loyalty points, partner sync, reporting)

Functional difference:
  Scenario 4: "Order confirmed → send push notification within 3 seconds" — delay = bad UX
  Scenario 8: "Order completed → award loyalty points within 10 minutes" — delay acceptable
              "End of day → sync orders to partner restaurant system" — batch is fine
              Fraud still needs to be synchronous even here (never async)

Technical difference:
  Scenario 4: each consumer has always-on Kafka consumer (real-time offset tracking)
              Idempotency checks critical (notifications sent once)
  Scenario 8: consumers can poll/batch process every 5-10 min
              Simpler: read from S3 or database table every N minutes instead of Kafka stream
              Still need idempotency (loyalty points added once), but pressure is lower

Architecture change that solves it:
  OPTION A (keep Kafka, simpler): same pattern as Scenario 4 but consumers run micro-batch
    Kafka consumer reads in batches every 5 min, processes, commits offset
  OPTION B (drop Kafka consumers, use DB polling):
    Spark writes processed events to PostgreSQL staging table
    Downstream systems poll staging table every 5 min → process new rows → mark processed
    Simpler for downstream teams — they read a database, not Kafka
  Fraud: STILL synchronous HTTP (does not change regardless of speed tier)
```

#### Full Architecture

```
[Customer places order]
        ↓
[App] ──1. WRITE──→ [PostgreSQL]        ← saves order (OLTP)
  │                       │
  │                 READ (WAL)
  │                       │
  │                [Debezium CDC]
  │                       │
  │                     WRITE
  │                       │
  │                   [Kafka] ──────WRITE──────→ [S3]
  │                                          raw Parquet files
  │
  └──2. REQUEST──→ [Fraud Service]    ← synchronous (always, regardless of speed tier)
                        │
                      READ
                        │
                   [Redis — fraud features]
                   (pre-computed by Flink)


                    [S3]
                      │
              READ every 5-10 min
                      │
             [Spark micro-batch]
             processes confirmed orders
                      │
                    WRITE
                      │
           [PostgreSQL — staging table]
           (orders_to_process: order_id, status, processed=false)
                      │
         ┌────────────┼────────────┐
       POLL          POLL         POLL
       5 min         5 min        daily
         │            │             │
  [Loyalty Svc]  [Partner Sync]  [Report Svc]
  adds points    sends orders     generates
  marks processed to restaurant   daily CSV
                  system          to finance
```

**🎯 Approach Used: DB Polling (Simplified Push Model)**
```
Pattern: S3 → Spark → PostgreSQL staging → Polling consumers
├─ Spark micro-batch every 5-10 minutes
├─ Reads S3, processes orders, writes to staging table
├─ Downstream services poll staging table (simpler than Kafka)
├─ Fraud stays synchronous (always, regardless of speed tier)
└─ Latency: 5-10 min acceptable for all downstream systems

Why this approach:
├─ Downstream systems don't need Kafka complexity (offset, consumer groups)
├─ DB polling is simpler: SELECT * WHERE processed=false
├─ Idempotency still required but easier implementation
├─ Matches the SLA: "points within 10 min" ← polling every 5 min works
```

#### Key Design Decisions

```
Why DB polling instead of Kafka for downstream:
  Kafka consumers require: offset management, consumer group setup, schema registry, DLQ
  DB polling: SELECT * FROM orders_to_process WHERE processed=false LIMIT 100
              Process. UPDATE processed=true. Done.
  At near real-time (5-10 min delay acceptable): DB polling is simpler and sufficient

Idempotency still required:
  Loyalty service must check before adding points:
  SELECT * FROM loyalty_events WHERE order_id=98765 AND type='earned'
  → exists: skip. Not exists: add points, insert record.
  Same principle as Scenario 4, simpler implementation (no Kafka replay complexity)

Fraud stays synchronous — always:
  No matter the speed tier (real-time, near RT, batch), fraud must block the order.
  It is the one component that never moves to async.

When to choose Scenario 4 vs Scenario 8 pattern:
  "Send notification within 3 sec"  → Scenario 4 (always-on Kafka consumer)
  "Award points within 10 min"      → Scenario 8 (DB polling every 5 min)
  Rule: if SLA < 30 sec → Kafka streaming. If SLA > 1 min → polling acceptable.
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| PostgreSQL | OLTP + staging table for downstream | App WRITEs, Debezium READs WAL, Downstream SERVICEs READ staging |
| Debezium | CDC connector | READs WAL, WRITEs Kafka |
| Kafka | Event bus + S3 feeder | WRITEs S3 |
| S3 | Raw Parquet storage | Kafka WRITEs, Spark READs |
| Spark micro-batch | Processes confirmed orders every 5-10 min | READs S3, WRITEs PostgreSQL staging |
| Fraud Service | Synchronous fraud scoring | READs Redis, RESPONDs to App |
| Redis | Fraud features | Flink WRITEs, Fraud Service READs |
| Loyalty/Partner/Report Services | Poll staging, process, mark done | READ + UPDATE PostgreSQL staging |

#### Interview Answer
```
1. App → PostgreSQL → Debezium → Kafka → S3
2. App makes synchronous fraud check (always, regardless of speed tier)
3. Spark micro-batch every 5-10 min READs S3 → WRITEs processed events to PostgreSQL staging
4. Downstream services poll staging table every 5-10 min:
   Loyalty: adds points, marks processed
   Partner sync: sends order data to restaurant system
   Report service: batches into daily export

Key insight: "Near real-time downstream systems don't need Kafka consumers.
DB polling every 5 min is simpler, easier for downstream teams, and meets the SLA.
Fraud check is always synchronous — no speed tier changes that."
```

---

### Scenario 9 — GB + Batch + Analytics

#### Problem Statement
```
Size:    5 GB/day, ~25M events/day
Speed:   Batch — hours to daily. Analytics team queries next-day data.
Use:     Analytics — "What was yesterday's revenue by city? Weekly trends? Monthly reports?"
Who:     Food delivery startup — finance team, business analysts, executive reporting
```

#### What's Different vs Scenario 5 (GB + Near RT + Analytics)
```
Scenario 5 need:  5-15 min delay — ops team wants recent data
Scenario 9 need:  Hours to daily delay — analysts work on yesterday's completed data

Functional difference:
  Scenario 5: "How many orders in the last 15 min?" — shift manager, operational
  Scenario 9: "What was total revenue yesterday by restaurant category?" — finance, strategic
              Data is fully settled (no partial orders, all payments confirmed)
              Analysts want CLEAN, JOINED, COMPLETE data — not raw event stream

Technical difference:
  Scenario 5: Spark runs every 5 min, reads incremental S3 files, writes to ClickHouse
  Scenario 9: Spark runs ONCE per day (nightly), reads ALL of yesterday's data
              More joins possible (user table, restaurant table, driver table, payment table)
              dbt runs SQL transformations after Spark load — clean business-logic layer
              Airflow orchestrates: wait for all data → trigger Spark → trigger dbt → alert done

Architecture change that solves it:
  DROP: Flink, ClickHouse, micro-batch scheduling
  ADD:  Airflow (orchestration), dbt (SQL transformations on top of Snowflake)
  SIMPLIFY: Spark runs once nightly, reads full day's S3 data, loads to Snowflake
  GAIN: complete joins across ALL tables, business-logic transformations via dbt
```

#### Full Architecture

```
[All day: orders flow normally]
[App] ──WRITE──→ [PostgreSQL] → [Debezium] → [Kafka] → [S3]
                                                    raw Parquet, partitioned by day

[Nightly at 2am: Airflow triggers pipeline]

[Airflow]
    │
    ├── Step 1: wait for S3 partition complete (all day's files landed)
    │
    ├── Step 2: trigger [Spark job]
    │           READs: s3://events/orders/date=2024-01-15/*.parquet
    │           JOINs: user table, restaurant table, driver table, payment table
    │           CLEANs: dedup, null handling, type casting
    │           WRITEs: raw + joined tables to [Snowflake]
    │
    ├── Step 3: trigger [dbt]
    │           runs SQL models on top of Snowflake:
    │           revenue_by_city_daily
    │           restaurant_performance_weekly
    │           user_cohort_analysis
    │           WRITEs: clean business tables back to Snowflake
    │
    └── Step 4: alert "Pipeline complete — data ready for 2024-01-15"

[Snowflake] ──READ──→ [Tableau / Looker / Metabase]
                       Analysts run queries in the morning
                       Data is from yesterday, fully complete
```

#### Key Design Decisions

```
Why dbt (data build tool):
  Spark loads raw data. Business logic still needed:
    "revenue" = amount - refunds - discounts (not just SUM(amount))
    "active restaurant" = placed ≥ 5 orders this week
  dbt writes these as SQL models — version-controlled, testable, documented
  Analysts trust the numbers because logic is explicit and reviewed

Why Airflow:
  Dependencies matter in batch:
    Cannot run dbt before Spark finishes
    Cannot run Spark before all S3 files for the day have landed
    Cannot alert users before dbt completes
  Airflow manages this DAG (Directed Acyclic Graph) of dependencies
  Failed step → retry → alert → no silent data gaps

Why not keep ClickHouse:
  Batch analytics consumers (finance, execs) already use Snowflake
  ClickHouse holds only recent hours — useless for "last quarter" queries
  Batch delay means Snowflake's cold-start latency (3-10 sec) is irrelevant

Data freshness contract:
  Analysts know: "data is complete for yesterday by 6am"
  No confusion about partial data, no stale dashboard checks
  Cleaner than near-real-time where data may be partially processed
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| PostgreSQL | OLTP source of truth | App WRITEs, Debezium READs WAL |
| Debezium + Kafka | CDC + event bus | READs WAL, WRITEs S3 via Kafka Connect |
| S3 | Raw Parquet (partitioned by day) | Kafka WRITEs, Spark READs |
| Airflow | Orchestrates nightly pipeline | Triggers Spark → dbt → alerts |
| Spark | Nightly ETL: read, join, clean | READs S3 + dimension tables, WRITEs Snowflake |
| dbt | SQL business logic transformations | READs Snowflake raw, WRITEs Snowflake clean |
| Snowflake | Historical data warehouse | Spark/dbt WRITE, Tableau READs |
| Tableau/Looker | Business analytics | READs Snowflake |

#### Interview Answer
```
1. All day: App → PostgreSQL → Debezium → Kafka → S3 (raw Parquet by day)
2. Nightly 2am: Airflow triggers pipeline
3. Spark READs full day's S3 → JOINs all dimension tables → WRITEs raw to Snowflake
4. dbt runs SQL models: revenue_by_city, restaurant_performance, cohort analysis
5. Analysts query Snowflake in the morning — fully complete, business-logic-clean data

Key insight: "Batch gives you completeness that streaming cannot.
All payments confirmed, all refunds settled, all tables joinable.
dbt + Airflow = auditable, testable pipeline that finance teams trust."
```

---

### Scenario 10 — GB + Batch + ML

#### Problem Statement
```
Size:    5 GB/day, ~25M events/day
Speed:   Batch — hours to daily
Use:     ML — training a new model or retraining existing model on historical data
Who:     Food delivery ML team — improving delivery time prediction, surge pricing model
```

#### What's Different vs Scenario 6 (GB + Near RT + ML)
```
Scenario 6 need:  Pre-compute predictions every 5 min for active orders (inference, near-live)
Scenario 10 need: Train or retrain ML model using months of historical data (training, not inference)

Functional difference:
  Scenario 6: "Update delivery estimate every 5 min" — model is ALREADY trained, running inference
  Scenario 10: "Train a better model using last 6 months of orders" — building/improving the model
               ML team runs experiment: new feature? new algorithm? retrain on recent data?
               Output: a new model artifact (file), not predictions

Technical difference:
  Scenario 6: Spark runs model on current data, writes predictions to Redis
  Scenario 10: Spark reads MONTHS of S3 data, creates feature matrix, trains model
               Training takes hours (not minutes), requires large cluster temporarily
               Output goes to Model Registry (not Redis) — model file stored, versioned, deployed

Architecture change that solves it:
  DROP: Redis, prediction store, Flink (no real-time features needed for training)
  ADD:  Feature engineering Spark job (creates training dataset), ML Training job,
        Model Registry (MLflow, SageMaker, Databricks MLflow)
  GAIN: full historical data for training, versioned models, rollback capability
```

#### Full Architecture

```
[Months of historical data already in S3]
s3://events/orders/date=2024-01-*/
s3://events/orders/date=2024-02-*/
...
s3://events/orders/date=2024-06-*/

[Airflow — weekly or on-demand trigger]
    │
    ├── Step 1: [Spark — Feature Engineering]
    │           READs: 6 months of S3 order data
    │           JOINs: user, restaurant, driver, weather tables
    │           CREATEs: feature matrix
    │           { order_id, restaurant_avg_prep_time, driver_zone_count,
    │             distance_km, time_of_day, day_of_week, actual_delivery_min }
    │           WRITEs: training_data.parquet → S3
    │
    ├── Step 2: [ML Training Job] (Spark MLlib / Python scikit-learn / XGBoost)
    │           READs: training_data.parquet from S3
    │           TRAINs: model (gradient boosted tree, or neural net)
    │           EVALUATEs: RMSE, MAE on held-out test set
    │           WRITEs: model artifact → [Model Registry] (MLflow / SageMaker)
    │                   model version, metrics, training date logged
    │
    └── Step 3: [Model Deployment Decision]
                A/B test: old model vs new model on 5% traffic
                If new model better → promote to production
                If worse → rollback to previous version in Registry
                Deploy: model artifact loaded by ML Inference Service


[ML Inference Service] (from Scenario 2 / Scenario 6)
    READs model from Registry on startup
    Serves predictions using newly trained model
```

#### Key Design Decisions

```
Model Registry — why it matters:
  Without registry: model is a file somewhere. Who trained it? When? On what data?
                    Bad model deployed? Cannot roll back. Cannot audit.
  With registry:    every model version tracked: training date, dataset, metrics, author
                    Rollback: "revert to model v3.2" → one command
                    MLflow: open source. SageMaker Model Registry: AWS managed.

Training vs Inference separation:
  Training: heavyweight, hourly+ runtime, large cluster, runs weekly/monthly
  Inference: lightweight, milliseconds, always-on, small cluster
  Never mix: training job running on inference cluster = inference slows/stops

Feature engineering is most of the work:
  Model training itself: 1 hour
  Feature engineering Spark job: 4-6 hours (joining 6 months of data)
  Rule: 80% of ML time is data preparation, not model tuning

A/B testing before full rollout:
  Never swap model for 100% traffic immediately
  Route 5% traffic to new model → compare actual delivery time vs prediction
  If RMSE improves → roll out to 100%
  Protects users from bad model causing wrong estimates
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| S3 | Historical raw data + training datasets | Kafka WRITEs raw, Spark READs + WRITEs training data |
| Airflow | Orchestrates weekly training pipeline | Triggers Feature Eng → Training → Registry |
| Spark (feature engineering) | Joins 6 months of data → training matrix | READs S3, WRITEs training_data.parquet |
| ML Training Job | Trains model, evaluates, registers | READs training_data.parquet, WRITEs Model Registry |
| Model Registry (MLflow) | Versions, stores, serves model artifacts | Training WRITEs, ML Service READs on deploy |
| ML Inference Service | Serves predictions using trained model | READs Model Registry on startup, READs Redis for features |

#### Interview Answer
```
1. S3 holds months of historical raw data (always — from Kafka pipeline)
2. Weekly Airflow job triggers:
   a. Spark feature engineering: 6 months S3 → join all tables → training_data.parquet
   b. ML training job: trains model, evaluates RMSE → logs to MLflow Model Registry
3. A/B test: 5% traffic to new model → validate improvement
4. Promote to production → ML Inference Service loads new model on next deploy

Key insight: "Batch ML training needs S3 as the permanent data store — Kafka 7-day
retention is useless for 6-month training windows. Model Registry = version control
for models. A/B test before 100% rollout — never swap blindly."
```

---

### Scenario 11 — GB + Batch + Dashboards

#### Problem Statement
```
Size:    5 GB/day, ~25M events/day
Speed:   Batch — daily. Dashboards updated once a day (morning).
Use:     Dashboards — executive reporting, weekly business reviews, monthly P&L
Who:     Food delivery startup — CEO/CFO looks at yesterday's numbers every morning
```

#### What's Different vs Scenario 9 (GB + Batch + Analytics)
```
Scenario 9 need:  Analysts run ad-hoc SQL queries on clean data (flexible, exploratory)
Scenario 11 need: Fixed dashboards with pre-defined metrics, auto-refreshed once daily

Functional difference:
  Scenario 9: analyst opens Tableau, drags dimensions, writes custom queries — exploratory
  Scenario 11: CEO opens dashboard at 9am, sees same 6 KPIs every day — fixed, curated
               "Yesterday revenue: ₹2.3M | Orders: 47,832 | Avg delivery: 31 min"
               No ad-hoc queries. Same view. Auto-refreshed. Fast load.

Technical difference:
  Scenario 9: Snowflake needs to handle any SQL → flexible schema, analyst access
  Scenario 11: dashboards need to load in <2 sec — pre-build the view, not query at load time
               Pre-aggregated daily summary tables → dashboard reads 1 row, not 47K rows

Architecture change that solves it:
  SAME pipeline as Scenario 9 (Airflow + Spark + dbt + Snowflake)
  ADD: dbt creates pre-aggregated daily_summary table (6 KPIs, 1 row per day)
  Dashboard tool reads daily_summary — instant load, no heavy SQL at dashboard open time
  Tool: Metabase / Looker / Tableau — all connect to Snowflake daily_summary
```

#### Full Architecture

```
[Same nightly pipeline as Scenario 9]
Airflow → Spark → Snowflake (raw) → dbt → Snowflake (clean tables)

[dbt adds one extra model: daily_kpi_summary]

dbt model: daily_kpi_summary
┌──────────────────────────────────────────────────┐
│ date        │ 2024-01-15                          │
│ total_rev   │ 2,347,892                           │
│ total_orders│ 47,832                              │
│ avg_delivery│ 31.2 min                            │
│ new_users   │ 1,247                               │
│ top_city    │ Mumbai                              │
│ fail_rate   │ 1.8%                                │
└──────────────────────────────────────────────────┘
1 row per day. Pre-computed. Reads in 1ms.

[Dashboard Tool — Metabase / Looker / Tableau]
    READs daily_kpi_summary
    Renders fixed KPI tiles
    Auto-refreshes at 6am (after nightly pipeline completes)
    CEO opens at 9am → instant load (1 row query)
```

#### Key Design Decisions

```
Pre-aggregated summary table vs raw table:
  Raw table: 47,832 rows for one day
             Dashboard runs: SELECT SUM(revenue), COUNT(*), AVG(delivery_time) FROM orders WHERE date='2024-01-15'
             Snowflake: 2-5 sec, gets slower as months accumulate
  Summary table: 1 row for one day
                 Dashboard runs: SELECT * FROM daily_kpi_summary WHERE date='2024-01-15'
                 Snowflake: <10ms always, regardless of history size

Dashboard alert: "data not refreshed":
  Airflow pipeline completes 6am → sets flag: data_ready_for=2024-01-15
  Dashboard checks flag before loading: if not ready → shows "Yesterday's data loading..."
  Prevents CEO seeing yesterday's yesterday data silently

This is the simplest architecture in all 12 scenarios:
  One pipeline (Airflow + Spark + dbt). One store (Snowflake). One summary table.
  Leadership lesson: "not every problem needs Kafka and Flink."
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| PostgreSQL | OLTP source of truth | App WRITEs |
| Debezium + Kafka | CDC + event bus | WRITEs S3 |
| S3 | Raw daily Parquet | Kafka WRITEs, Spark READs |
| Airflow | Nightly orchestration | Triggers Spark → dbt |
| Spark | Full-day ETL, joins | READs S3, WRITEs Snowflake raw |
| dbt | Business logic + daily_kpi_summary | READs Snowflake raw, WRITEs summary |
| Snowflake | Data warehouse + summary table | dbt WRITEs, Dashboard READs |
| Metabase/Tableau | Fixed executive dashboard | READs Snowflake summary |

#### Interview Answer
```
1. Nightly Airflow: Spark reads S3 → loads raw to Snowflake → dbt runs models
2. dbt creates daily_kpi_summary: 1 row per day, 6 pre-computed KPIs
3. Dashboard auto-refreshes at 6am from daily_kpi_summary — instant load
4. CEO opens at 9am → 6 tiles, all green or red, no waiting

Key insight: "Simplest architecture of all 12 scenarios. No streaming needed.
Pre-aggregate into a summary table — dashboard reads 1 row, not millions.
dbt makes business logic explicit, auditable, version-controlled."
```

---

### Scenario 12 — GB + Batch + Another System

#### Problem Statement
```
Size:    5 GB/day, ~25M events/day
Speed:   Batch — daily or hourly file transfer
Use:     Another System — send data to external partner, regulatory body, or legacy system
Who:     Food delivery startup — daily order export to restaurant partners, weekly GST report
         to tax authority, nightly sync to legacy ERP system
```

#### What's Different vs Scenario 8 (GB + Near RT + Another System)
```
Scenario 8 need:  Downstream systems react within 5-10 minutes (loyalty points, partner sync)
Scenario 12 need: Downstream systems receive data in scheduled bulk transfers (daily files)

Functional difference:
  Scenario 8: "Award loyalty points within 10 min of order" — near-real-time event processing
  Scenario 12: "Send all of today's orders to restaurant partner at midnight" — daily file export
               "Submit monthly GST report to tax authority" — regulatory, fixed schedule
               "Sync yesterday's orders to SAP ERP" — legacy system, batch file import

Technical difference:
  Scenario 8: downstream systems have APIs or DB connections, can receive events
  Scenario 12: downstream systems expect FILES (CSV, JSON, XML) via SFTP, S3 drop, or email
               No Kafka consumers. No DB polling. Just: generate file → deliver file.
               File format is contractually agreed with partner — cannot change unilaterally

Architecture change that solves it:
  DROP: all real-time components (Flink, Kafka consumers, Redis)
  KEEP: S3 as source, Airflow for scheduling
  ADD:  File generation step (Spark → CSV/JSON/XML), delivery step (SFTP/S3 transfer/API push)
        File validation before delivery (row count, checksum, schema check)
        Delivery acknowledgement tracking (did partner receive it?)
```

#### Full Architecture

```
[All day: orders accumulate in S3]
Kafka → S3: s3://events/orders/date=2024-01-15/*.parquet

[Nightly at midnight: Airflow triggers export pipelines]

[Pipeline A — Restaurant Partner Export]
Airflow
  │
  ├── [Spark] READs today's orders from S3
  │   FILTERs: orders for restaurant group "PizzaCo"
  │   FORMATs: converts to agreed CSV schema
  │   WRITEs: partner_orders_20240115_pizzaco.csv → S3 staging bucket
  │
  ├── [Validation] row_count matches expected range? checksum computed?
  │
  ├── [SFTP Delivery] uploads CSV to PizzaCo's SFTP server
  │
  └── [Acknowledgement] WRITEs to PostgreSQL:
      { export_id, partner, date, rows, delivered_at, status="success" }


[Pipeline B — GST Tax Report]
Airflow (monthly trigger)
  │
  ├── [Spark] READs full month's data from S3
  │   COMPUTEs: taxable_amount, gst_collected, gst_owed per restaurant
  │   FORMATs: government-specified XML schema
  │   WRITEs: gst_report_jan2024.xml → S3
  │
  └── [API Push] submits to tax authority API


[Pipeline C — Legacy ERP Sync]
Airflow (nightly)
  │
  ├── [Spark] READs yesterday's completed orders
  │   TRANSFORMs: maps order schema → SAP ERP schema
  │   WRITEs: erp_orders_20240115.json → shared S3 bucket (ERP team reads)
  │
  └── [Notification] sends email/Slack "ERP file ready: 47,832 records"
```

#### Key Design Decisions

```
File validation before delivery — non-negotiable:
  Delivered wrong file to tax authority → legal consequence
  Checks before every delivery:
    row_count in expected range (not 0, not 10x normal — either is a bug)
    checksum computed (partner verifies on their end)
    schema correct (no new/missing columns that break partner's import)
  If validation fails → abort, alert data engineering, do NOT deliver

Delivery acknowledgement tracking:
  Delivering the file ≠ partner received it
  Track in PostgreSQL: { delivery_id, partner, status, delivered_at, ack_received_at }
  If no ack in 2 hours → alert → retry → escalate
  This is the audit trail regulators ask for

File format is a contract:
  Partners import files into their own systems with hardcoded column names
  Changing "order_amount" to "total_amount" breaks their import silently
  Treat file schema like an API contract: versioned, backward-compatible, change with notice

Idempotency for file delivery:
  Airflow job fails after file generated but before SFTP upload → job restarts
  Restart generates the file again (same data, same filename) → overwrites S3 staging
  SFTP upload is idempotent: same filename, same content → partner imports once
  Delivery log: mark as delivered only after confirmed SFTP success

This is the most operationally boring — and most LEGALLY CRITICAL — of all 12 scenarios.
Regulators do not care about your Kafka cluster. They care about the report arriving on time.
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| S3 | Raw daily Parquet (source) + staging exports | Kafka WRITEs raw, Spark READs + WRITEs exports |
| Airflow | Schedules and orchestrates all export pipelines | Triggers Spark → validate → deliver |
| Spark | Reads raw, filters, transforms, formats to file | READs S3, WRITEs formatted files to S3 staging |
| PostgreSQL | Delivery acknowledgement log | Airflow WRITEs status after each delivery |
| SFTP / S3 Transfer / API | File delivery channel to partner/regulator | Airflow DELIVERs files |

#### Interview Answer
```
1. All day: orders flow to S3 via Kafka (raw Parquet, partitioned by day)
2. Nightly Airflow triggers per-partner export pipelines:
   Spark reads S3 → filters per partner → formats to agreed schema → writes to S3 staging
3. Validation: row count, checksum, schema check — abort if fails
4. Delivery: SFTP upload / S3 transfer / API push to partner
5. Acknowledgement: log delivery in PostgreSQL, alert if no ack in 2 hours

Key insight: "Batch file export is the most legally critical architecture.
File schema is a contract. Validation before delivery is non-negotiable.
Delivery ≠ receipt — track acknowledgement separately.
Regulators and enterprise partners do not consume Kafka topics — they expect files."
```

---

## TB Scale Scenarios (5 TB/day — National Scale)

---

### Scenario 13 — TB + Real-time + Analytics

#### Problem Statement
```
Size:    5 TB/day, ~5B events/day
Speed:   Real-time — seconds
Use:     Analytics — "Orders by city right now? Which zone is spiking?"
Who:     Food delivery platform at national scale — millions of daily orders
```

#### What's Different vs Scenario 1 (GB + Real-time + Analytics)
```
Scenario 1: 5 GB/day — single machines handle everything
Scenario 13: 5 TB/day — single machines break at every layer

What breaks at TB scale:
  PostgreSQL (1 node): 5B rows/day exceeds single-node write throughput
  Flink (1 node):      5B events/day cannot be processed on single JVM
  ClickHouse (1 node): TB of data + continuous aggregation = disk/RAM exhaustion
  Kafka (3 brokers):   TB/day throughput needs 100+ partitions, 10+ brokers

Architecture change:
  PostgreSQL   → Cassandra (distributed, sharded by order_id, replicated across nodes)
  Debezium     → Debezium cluster (parallel connectors per Cassandra node)
  Kafka        → Kafka Cluster (100+ partitions, 10+ brokers)
  Flink        → Flink Cluster (multi-node, higher parallelism per partition)
  ClickHouse   → Druid / Pinot (multi-tenant TB OLAP, distributed ingestion)
  Pattern stays identical — only scale of each component changes
```

#### Full Architecture
```
[App] ──WRITE──→ [Cassandra Cluster]     ← OLTP, sharded by order_id
                        │
                  READ (commit log)
                        │
               [Debezium Cluster]        ← parallel connectors per node
                        │
                      WRITE
                        │
              [Kafka Cluster]  ──WRITE──→ [S3]
              100+ partitions             partitioned by minute
              10+ brokers                      │
                    │                          │
             ┌──────┤                    READ every 5 min (Near-RT)
           READ    READ                  or nightly (Batch)
             │      │
       [Payment] [Driver]
        Service   Service
        (distributed OLTP consumers)
             │
           READ
             │
        [Flink Cluster]             ← multi-node, parallelism matches Kafka partitions
        aggregates every 10 sec
             │
           WRITE
             │
       [Druid / Pinot]              ← TB-scale real-time OLAP
       distributed ingestion         multi-tenant, sub-second queries
             │
           READ
             │
          [Grafana]
          ops dashboard
```

#### Key Scale Challenges
```
Cassandra partition key design:
  Wrong:  partition by timestamp → all writes go to same node (hotspot)
  Right:  partition by order_id (or user_id) → writes spread across all nodes
  Rule:   high cardinality partition key = even distribution = no hotspot

Kafka partition count:
  GB: 10-20 partitions (1 Flink task per partition)
  TB: 100-200 partitions → 100-200 Flink tasks running in parallel
  More partitions = more parallelism = more throughput, but more overhead

Druid vs Pinot choice:
  Druid:  better for internal dashboards, flexible query types
  Pinot:  better for user-facing analytics (< 10ms p99), simpler scaling
  Both handle TB-scale real-time OLAP — pick based on query pattern

Flink cluster sizing:
  Rule of thumb: 1 task slot per Kafka partition
  TB: 100 partitions → 100 task slots → 10 machines × 10 slots each
  Checkpoint interval: increase to 60 sec at TB (more state to snapshot)
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| Cassandra Cluster | Distributed OLTP (sharded by order_id) | App WRITEs, Debezium READs commit log |
| Debezium Cluster | CDC at TB scale | READs Cassandra commit log, WRITEs Kafka |
| Kafka Cluster (100+ partitions) | TB/day throughput event bus | Consumers READ |
| Flink Cluster | Distributed real-time aggregation | READs Kafka, WRITEs Druid/Pinot |
| Druid / Pinot | TB-scale real-time OLAP | Flink WRITEs, Grafana READs |
| S3 | Raw storage (minute partitions) | Kafka WRITEs |

#### Interview Answer
```
Same pattern as GB — only scale changes:
1. Cassandra replaces PostgreSQL (distributed OLTP, sharded by order_id)
2. Debezium cluster reads Cassandra commit log → Kafka Cluster (100+ partitions)
3. Flink Cluster (1 task per partition) aggregates → Druid/Pinot
4. Grafana reads Druid/Pinot → sub-second ops dashboard

Key insight: "Pattern stays identical. Cassandra partition key is the most critical
design decision — wrong key = hotspot = one node takes all writes = single point of failure."
```

---

### Scenario 14 — TB + Real-time + ML

#### Problem Statement
```
Size:    5 TB/day, ~5B events/day
Speed:   Real-time — ML prediction in milliseconds
Use:     ML Inference — delivery time prediction at national scale
Who:     Food delivery platform — millions of concurrent orders, each needing prediction
```

#### What's Different vs Scenario 2 (GB + Real-time + ML)
```
Scenario 2: Redis single node holds all features — works at GB scale (millions of keys)
Scenario 14: 5B events/day → billions of feature keys → single Redis node runs out of RAM

What breaks at TB scale:
  Redis (1 node):      billions of keys for users + restaurants + zones → OOM
  ML Service (1 node): thousands of concurrent inference requests → CPU saturation
  Flink (1 node):      cannot process 5B events to compute features fast enough
  Spark (1 node):      historical feature computation on TB/day data takes too long

Architecture change:
  Redis       → Redis Cluster (sharded across 10-20 nodes by key hash)
  ML Service  → auto-scaling fleet behind load balancer (stateless, easy to scale)
  Flink       → Flink Cluster
  Spark       → Spark on EMR / Databricks
  Pattern stays identical — Lambda Architecture unchanged
```

#### Full Architecture
```
[App]
  │
  ├──WRITE──→ [Cassandra Cluster]        ← OLTP
  │                  │
  │           [Debezium Cluster] → [Kafka Cluster] ──WRITE──→ [S3]
  │                  │                                    (TB raw, minute partitions)
  │                READ
  │                  │
  │          [Flink Cluster] ──WRITE──→ [Redis Cluster]
  │          real-time features          sharded by key hash
  │          (zone traffic,              user:USR-123:velocity → node 3
  │           active drivers)            rest:456:orders → node 7
  │
  │          [Spark on EMR] ──WRITE──→ [Redis Cluster]
  │          historical features         different key prefix
  │          (hourly, reads S3)          rest:456:avg_prep → node 7
  │
  └──REQUEST──→ [ML Service Fleet]       ← auto-scaling, stateless
                load balancer routes      N instances behind ALB
                     │
                   READ
                     │
              [Redis Cluster]            ← 1ms lookup, any node
              features for this order
                     │
               runs ML model
                     │
              RESPONSE "28 min" → App
```

#### Key Scale Challenges
```
Redis Cluster key distribution:
  Keys hash to slots (0-16383), slots assigned to nodes
  user:USR-123:velocity → hash → slot 4821 → node 3
  All features for one user may be on different nodes — 2-3 network hops
  Fix: hash tags { } — user:{USR-123}:velocity forces all user keys to same node

ML Service stateless design:
  Service reads Redis, runs model, returns response — no local state
  Stateless = any request goes to any instance — easy horizontal scale
  Scale trigger: CPU > 60% → add instance. CPU < 20% → remove instance

Feature key TTL at TB scale:
  Billions of keys with no TTL = Redis Cluster fills up and crashes
  Every key must have TTL: real-time features 2 min, historical 2 hr
  Monitor: Redis memory usage per node. Alert at 70% capacity.

Model serving at TB scale:
  Single model file loaded by each ML Service instance on startup
  Model update: rolling deploy — 10% instances at a time → validate → 100%
  Never stop all instances simultaneously — zero-downtime requirement
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| Cassandra Cluster | Distributed OLTP | App WRITEs, Debezium READs |
| Kafka Cluster | TB/day event bus | Flink READs |
| Flink Cluster | Real-time feature computation | READs Kafka, WRITEs Redis Cluster |
| Spark on EMR | Historical feature computation | READs S3, WRITEs Redis Cluster |
| Redis Cluster | Distributed Feature Store (sharded) | Flink/Spark WRITE, ML Fleet READs |
| ML Service Fleet | Auto-scaling inference | READs Redis Cluster, RESPONDs to App |

#### Interview Answer
```
Same Lambda Architecture as GB — scale changes execution:
1. Flink Cluster computes real-time features → Redis Cluster (sharded, TTL on all keys)
2. Spark on EMR computes historical features → Redis Cluster (different key prefix)
3. ML Service Fleet (stateless, auto-scales) READs Redis → runs model → responds in <15ms

Key insight: "Redis Cluster hash tags are the most common TB-scale mistake to miss.
Without them, one user's features scatter across nodes — 2-3 hops per inference.
With hash tags: all features for user:{USR-123} land on same node — 1 hop."
```

---

### Scenario 15 — TB + Real-time + Dashboards

#### Problem Statement
```
Size:    5 TB/day, ~5B events/day
Speed:   Real-time ops (30 sec) + Near-real-time business (15-30 min)
Use:     Two dashboards — ops team live + business team historical
Who:     Food delivery platform at national scale
```

#### What's Different vs Scenario 3 (GB + Real-time + Dashboards)
```
Scenario 3: ClickHouse (1 node) + Snowflake (small) — works at GB scale
Scenario 15: TB/day throughput breaks both

What breaks:
  ClickHouse (1 node): TB/day write rate exceeds single-node ingestion capacity
  Snowflake (small):   TB/day ETL takes too long, query contention spikes

Architecture change:
  ClickHouse (1 node) → Druid / Pinot (distributed, TB ingestion via streaming)
  Snowflake (small)   → BigQuery (serverless, auto-scales) or Redshift (larger cluster)
  Flink               → Flink Cluster
  Spark               → Spark on EMR / Databricks
  Pattern identical — two stacks on same Kafka, different serving layers
```

#### Full Architecture
```
[Kafka Cluster] ─────────────────────────────────── WRITE ──→ [S3]
(from Cassandra → Debezium → Kafka)                        minute partitions

     READ (Flink Cluster)          READ (Spark on EMR)
          │                               │
    aggregates every 30s           cleans + joins hourly
          │                               │
        WRITE                           WRITE
          │                               │
   [Druid / Pinot]                [BigQuery / Redshift]
   TB real-time OLAP              TB batch OLAP
   distributed ingestion          serverless / large cluster
          │                               │
        READ                           READ
          │                               │
      [Grafana]                    [Tableau / Looker]
    ops dashboard                  business dashboard
    30-sec refresh                 on-demand queries
```

#### Key Scale Challenges
```
Druid ingestion at TB:
  Druid uses "real-time nodes" that ingest from Kafka, "historical nodes" for old data
  TB/day: need 20-30 real-time nodes ingesting in parallel (one per Kafka partition group)
  Segments: Druid writes data as immutable segments — query planner reads only needed segments

BigQuery for TB batch:
  Serverless — no cluster to size. BigQuery auto-scales to query size.
  TB/day ETL: Spark on EMR writes Parquet to GCS → BigQuery external table or load job
  Cost: BigQuery charges per TB scanned — use column pruning and partitioning to reduce cost

Two-dashboard governance at TB:
  Same as GB — ClickHouse/Druid for ops, Snowflake/BigQuery for business
  At TB: enforce separate service accounts — ops team cannot query BigQuery (cost risk)
  Druid dedicated for dashboards — no ad-hoc query access
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| Kafka Cluster | TB event bus | Flink + Kafka Connect READ |
| Flink Cluster | Real-time aggregation | READs Kafka, WRITEs Druid/Pinot |
| Druid / Pinot | TB real-time OLAP (ops) | Flink WRITEs, Grafana READs |
| S3 | Raw minute-partitioned storage | Kafka WRITEs, Spark READs |
| Spark on EMR | Hourly ETL | READs S3, WRITEs BigQuery/Redshift |
| BigQuery / Redshift | TB batch OLAP (business) | Spark WRITEs, Tableau READs |

#### Interview Answer
```
Same two-stack pattern as GB, distributed components:
1. Kafka Cluster → Flink Cluster → Druid/Pinot → Grafana (ops, 30-sec refresh)
2. Kafka Cluster → S3 → Spark on EMR → BigQuery → Tableau (business, hourly)

Key insight: "At TB, Druid/Pinot replaces ClickHouse — same role, distributed ingestion.
BigQuery replaces small Snowflake — serverless means no cluster sizing mistake possible."
```

---

### Scenario 16 — TB + Real-time + Another System

#### Problem Statement
```
Size:    5 TB/day, ~5B events/day
Speed:   Real-time — milliseconds to seconds
Use:     Another System — notification, inventory, loyalty, fraud at national scale
Who:     Food delivery platform — millions of concurrent orders, many downstream systems
```

#### What's Different vs Scenario 4 (GB + Real-time + Another System)
```
Scenario 4: PostgreSQL idempotency store, single consumer instances — works at GB
Scenario 16: 5B events/day breaks both

What breaks:
  PostgreSQL idempotency: 5B idempotency checks/day on single PostgreSQL = saturation
  Single consumer instances: cannot process TB event volume fast enough
  Single Fraud Service: thousands of concurrent fraud checks in <200ms — one machine insufficient

Architecture change:
  PostgreSQL idempotency → Cassandra (distributed idempotency, partitioned by order_id)
  Single consumer instances → auto-scaling consumer fleets
  Fraud Service (1 node) → Fraud Service fleet (auto-scaling, behind load balancer)
  Redis → Redis Cluster (fraud features)
  Pattern identical — same Schema Registry, DLQ, Consumer Groups
```

#### Full Architecture
```
[App]
  │
  ├──WRITE──→ [Cassandra Cluster]        ← OLTP
  │
  └──REQUEST──→ [Fraud Service Fleet]    ← auto-scaling, <200ms
                      │
                    READ
                      │
               [Redis Cluster]           ← fraud features, sharded

[Cassandra] → [Debezium Cluster] → [Kafka Cluster + Schema Registry]
                                          │
              ┌───────────────────────────┼───────────────────────────┐
              │                           │                           │
    [Notification Fleet]        [Inventory Fleet]           [Loyalty Fleet]
    N auto-scaling instances    N auto-scaling instances    N auto-scaling instances
    own Consumer Group          own Consumer Group           own Consumer Group
          │                           │
    idempotency check           idempotency check
    WRITE to [Cassandra]        WRITE to [Cassandra]
    (notifications_sent)        (inventory_decremented)
          │
    [Push Notification]         [Inventory DB Cluster]
    global push service
          │
    [DLQ] — failed events retry (per service, per Consumer Group)
```

#### Key Scale Challenges
```
Cassandra idempotency at 5B/day:
  Partition key: order_id — every check hits different node
  Read-before-write: SELECT then INSERT if not exists
  At TB: use Cassandra lightweight transactions (LWT) for atomic check-and-insert
  Cost: LWT is slower (Paxos consensus) — acceptable for idempotency (1 check per order)

Auto-scaling consumer fleet:
  Kafka partitions = max parallelism ceiling
  100 partitions → max 100 consumer instances per Consumer Group
  Scale trigger: consumer lag growing → add instances (up to partition count)
  Scale down: lag = 0 for 5 min → remove instances

DLQ at TB scale:
  DLQ topic itself can accumulate TB of failed events if service is down long
  DLQ consumer must process fast enough to drain before TTL expires
  Monitor: DLQ depth. Alert if > 1M messages. Page oncall.

Schema Registry at TB:
  Same tool as GB — Schema Registry is not the bottleneck
  Cache schema locally in consumer (avoid Registry call per message)
  Schema cache TTL: 1 hour — refresh in background
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| Cassandra Cluster | OLTP + distributed idempotency store | App WRITEs, Debezium READs, Consumers WRITE idempotency |
| Kafka Cluster + Schema Registry | TB event bus with schema validation | Consumers READ |
| Fraud Service Fleet | Auto-scaling sync fraud scoring | READs Redis Cluster, RESPONDs |
| Redis Cluster | Fraud features (sharded) | Flink WRITEs, Fraud Fleet READs |
| Consumer Fleets (Notification/Inventory/Loyalty) | Auto-scaling event processors | READ Kafka, WRITE idempotency to Cassandra |
| DLQ | Failed event retry (per Consumer Group) | Kafka WRITEs, DLQ consumer READs |

#### Interview Answer
```
Same pattern as GB — scale forces distribution:
1. Fraud Service Fleet (auto-scaling) → Redis Cluster for features — always sync
2. Cassandra → Debezium Cluster → Kafka Cluster + Schema Registry
3. Auto-scaling Consumer Fleets (Notification/Inventory/Loyalty) — own Consumer Group
4. Idempotency: Cassandra lightweight transactions (not PostgreSQL at TB scale)
5. DLQ per Consumer Group — monitor depth, alert at 1M messages

Key insight: "Consumer fleet size is bounded by Kafka partition count.
100 partitions = max 100 parallel consumers. Partition count must be planned
before data volumes peak — cannot reduce partitions without data loss."
```

---

### Scenario 17 — TB + Near Real-time + Analytics

#### Problem Statement
```
Size:    5 TB/day, ~5B events/day
Speed:   Near Real-time — 5-15 minute delay acceptable
Use:     Analytics — shift-level monitoring, recent trend visibility
Who:     Food delivery platform ops team — city-level managers reviewing 15-min windows
```

#### What's Different vs Scenario 5 (GB + Near RT + Analytics)
```
Scenario 5: Spark micro-batch on 1 machine — processes GB in 5 min easily
Scenario 17: TB of new data every 5 min = 1 TB per micro-batch — single Spark machine impossible

What breaks:
  Single Spark node: 1 TB per 5-min batch → hours to process → SLA broken
  S3 hourly partitions: too coarse — need minute partitions to read only last 5 min of data

Architecture change:
  Spark (1 node) → Spark on EMR / Databricks (auto-scaling cluster)
  S3 hourly partition → S3 minute partition (read only 5 min of files per batch)
  Druid/Pinot remains as OLAP store (same as S13, TB RT choice)
  Pattern identical — only Spark scales out
```

#### Full Architecture
```
[Kafka Cluster] ──WRITE──→ [S3]
                            partitioned by MINUTE
                            s3://events/date=2024-01-15/hour=11/minute=30/*.parquet
                                 │
                         READ every 5 min
                         (only minute=30 folder)
                                 │
                  [Spark on EMR — auto-scaling cluster]
                  reads ONLY last 5 min of S3 files
                  aggregates: city, orders_count, avg_delivery
                  auto-scales workers: 5 min batch = spin up → process → spin down
                                 │
                               WRITE
                                 │
                   [Druid / Pinot]          [BigQuery / Redshift]
                   Near-RT ops              Business analytics
                   5-min fresh              hourly fresh
                                 │                  │
                             [Grafana]         [Tableau]
                             5-min refresh     on-demand
```

#### Key Scale Challenges
```
S3 minute partitioning is critical:
  Without: Spark reads full hour folder (12 × more data than needed) → 6x slower
  With:    Spark reads only minute=30 folder → reads exactly the 5-min batch
  Implementation: Kafka Connect writes with time-based partitioner (minute granularity)

EMR auto-scaling for micro-batch:
  Each 5-min batch: spin up cluster → process 1 TB → write to Druid → spin down
  Cold start: EMR cluster takes 3-5 min to spin up — use EMR managed scaling (pre-warmed)
  Or: keep small standing cluster, scale out for batch, scale back in after

Druid ingestion from batch:
  Druid supports batch ingestion from S3 (not just Kafka streaming)
  Spark writes Parquet to S3 staging → Druid batch ingestion job reads → indexes
  Ingestion takes 1-2 min at TB scale → total pipeline: 5 min batch + 2 min ingest = 7 min
  Acceptable at "5-15 min" SLA
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| Kafka Cluster | TB event bus | WRITEs S3 via Kafka Connect |
| S3 (minute partitions) | Raw storage, precise time slicing | Kafka WRITEs, Spark READs |
| Spark on EMR (auto-scaling) | 5-min micro-batch aggregation | READs S3, WRITEs Druid/BigQuery |
| Druid / Pinot | Near-RT OLAP (ops) | Spark WRITEs, Grafana READs |
| BigQuery / Redshift | Batch OLAP (business) | Spark WRITEs, Tableau READs |

#### Interview Answer
```
1. Kafka → S3 (minute-level partitions — critical for reading only 5-min slices)
2. Spark on EMR auto-scales every 5 min: reads last-minute S3 folder → aggregates → Druid
3. Druid batch ingestion: 2-min index build → Grafana reads fresh data
4. Same Spark run WRITEs to BigQuery for business team

Key insight: "S3 minute partitioning is the difference between reading 1 TB and reading 5 GB
per micro-batch. Partition granularity must match your micro-batch interval."
```

---

### Scenario 18 — TB + Near Real-time + ML

#### Problem Statement
```
Size:    5 TB/day, ~5B events/day
Speed:   Near Real-time — predictions updated every 5-15 min
Use:     ML — pre-computed delivery estimates for order tracking screen
Who:     Food delivery platform — tens of millions of active orders, estimates refresh every 5 min
```

#### What's Different vs Scenario 6 (GB + Near RT + ML)
```
Scenario 6: Redis single node stores pre-computed predictions for all active orders
Scenario 18: tens of millions of active orders simultaneously → Redis single node OOM

What breaks:
  Redis (1 node): 50M active orders × avg key size 200 bytes = 10 GB RAM minimum
                  Plus feature keys → easily exceeds single-node RAM
  Spark (1 node): running ML model on 50M active orders in 5 min → impossible

Architecture change:
  Redis (1 node) → Redis Cluster (sharded, predictions spread across nodes)
  Spark (1 node) → Spark on EMR (distributes model scoring across workers)
  Pattern identical — Spark runs model, stores result in Redis, App GETs result
```

#### Full Architecture
```
[S3] ──READ (every 5 min)──→ [Spark on EMR — auto-scaling]
                               reads active orders from Cassandra
                               reads features from Redis Cluster
                               runs ML model (distributed scoring)
                               writes predictions: order_id → estimate
                                      │
                                    WRITE
                                      │
                               [Redis Cluster]
                               prediction:order:98765 → "19 min"
                               prediction:order:98766 → "24 min"
                               sharded by order_id hash
                               TTL: 15 min per key
                                      │
                                    READ
                                      │
                               [App / Tracking Screen]
                               GET prediction:order:{order_id}
                               → displays "19 min", refreshes every 5 min
```

#### Key Scale Challenges
```
Distributed model scoring on Spark:
  50M active orders → Spark partitions into 10K chunks of 5K orders each
  Each Spark worker scores its chunk independently (embarrassingly parallel)
  Model file: broadcast to all workers once (not loaded per partition)
  Total time: 50M orders / N workers — scale workers until runtime < 5 min

Redis Cluster for predictions:
  Shard by order_id hash → predictions spread evenly across nodes
  10M active orders × 200 bytes = 2 GB → 10-node cluster = 200 MB per node → comfortable
  Monitor: memory per node. Pre-scale before peak hours (lunch/dinner rush).

Feature reads during Spark scoring:
  Spark reads Redis Cluster for each order's features → N × M network calls
  Optimization: Spark batch-reads features (MGET) per partition → 1 call per 5K orders
  Further: pre-join features into the active orders table before scoring (avoid Redis reads in loop)
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| Cassandra Cluster | Active order store | Spark READs active orders |
| Redis Cluster (features) | Feature Store (real-time + historical) | Flink/Spark WRITE features |
| Spark on EMR | Distributed model scoring every 5 min | READs Cassandra + Redis, WRITEs Redis predictions |
| Redis Cluster (predictions) | Pre-computed estimates per order | Spark WRITEs, App READs |

#### Interview Answer
```
Same pre-compute pattern as GB — scale forces distribution:
1. Spark on EMR reads 50M active orders from Cassandra every 5 min
2. Batch-reads features from Redis Cluster (MGET per partition)
3. Distributes ML model scoring across workers (model broadcast once)
4. Writes predictions to Redis Cluster (TTL 15 min per key)
5. App GETs prediction for order_id — 1ms Redis lookup

Key insight: "Broadcast the model file to all Spark workers once per job run.
Never load it per partition — that's one model load per 5K orders = 10K loads total."
```

---

### Scenario 19 — TB + Near Real-time + Dashboards

#### Problem Statement
```
Size:    5 TB/day, ~5B events/day
Speed:   Near Real-time — 5-15 min delay, both ops and business teams
Use:     Two dashboards — both teams accept 5-min delay
Who:     Food delivery platform — ops city managers + executive business reviews
```

#### What's Different vs Scenario 7 (GB + Near RT + Dashboards)
```
Scenario 7: Snowflake (small) via Snowpipe — works at GB/day
Scenario 19: TB/day Snowpipe ingestion can lag, Snowflake small warehouse struggles

What breaks:
  Snowflake Snowpipe: designed for continuous micro-batch, but TB/day = very high file volume
  Snowflake (small):  TB/day query compute on small warehouse = slow, expensive

Architecture change:
  Snowflake (small) → BigQuery (serverless, auto-scales to query/load size)
                   OR Redshift (larger cluster, RA3 storage-compute separation)
  Snowpipe → Spark on EMR writes directly to BigQuery (faster, more control)
  One tool still serves both dashboards (5-min delay acceptable for both)
```

#### Full Architecture
```
[S3] ──READ every 5 min──→ [Spark on EMR]
                            cleans + aggregates
                                   │
                                 WRITE
                                   │
                            [BigQuery]
                            serverless, auto-scales
                            5-min fresh
                                   │
                    ┌──────────────┴───────────────┐
                  READ                           READ
                    │                               │
                [Grafana]                    [Tableau / Looker]
                ops dashboard                business dashboard
                5-min refresh                on-demand queries
                connects to BigQuery         connects to BigQuery
```

#### Key Scale Challenges
```
BigQuery vs Snowflake at TB scale:
  BigQuery: serverless — no warehouse to size, auto-scales per query
            Ideal when: team already on GCP, or load is spiky (variable query concurrency)
  Snowflake: needs right warehouse size — XL or XXL at TB daily load
            Ideal when: team already uses Snowflake, prefer single SQL dialect

Spark → BigQuery write pattern:
  Spark writes Parquet to GCS staging → BigQuery load job (fastest, bulk)
  OR Spark BigQuery connector (direct write, slightly slower)
  At TB: GCS staging + load job is faster and cheaper

One warehouse, two audiences at TB:
  Same risk as GB: analyst heavy query during sale = Grafana dashboard slow
  Fix at TB: BigQuery slots reservation — reserve 1000 slots for ops dashboard
  Analyst queries use on-demand slots (billed per TB scanned)
  Dashboard queries use reserved slots (dedicated, predictable latency)
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| S3 / GCS | Raw minute-partitioned storage | Kafka WRITEs, Spark READs |
| Spark on EMR | 5-min micro-batch ETL | READs S3, WRITEs BigQuery |
| BigQuery (slot reservation) | Single TB OLAP for both audiences | Spark WRITEs, Grafana + Tableau READ |

#### Interview Answer
```
Same one-tool pattern as GB S7 — BigQuery replaces small Snowflake:
1. S3 (minute partitions) → Spark on EMR → BigQuery every 5 min
2. Grafana READs BigQuery reserved slots → ops team, 5-min refresh
3. Tableau READs BigQuery on-demand → business team

Key insight: "BigQuery slot reservation separates dashboard SLA from analyst workload.
Without it, one analyst running a multi-TB scan slows every Grafana dashboard panel."
```

---

### Scenario 20 — TB + Near Real-time + Another System

#### Problem Statement
```
Size:    5 TB/day, ~5B events/day
Speed:   Near Real-time — downstream systems react within 5-15 minutes
Use:     Another System — loyalty, partner sync, reporting with minute-level delay
Who:     Food delivery platform — millions of orders, many downstream systems
```

#### What's Different vs Scenario 8 (GB + Near RT + Another System)
```
Scenario 8: PostgreSQL staging table — consumer services poll every 5 min
Scenario 20: TB/day = billions of rows in staging table → PostgreSQL saturates

What breaks:
  PostgreSQL staging: 5B rows/day into a single staging table = write saturation + slow polls

Architecture change:
  PostgreSQL staging → Cassandra staging (distributed, partitioned by service + time bucket)
  Spark reads S3 → writes processed events to Cassandra staging (not PostgreSQL)
  Consumer services poll Cassandra staging table (range scan by time bucket)
  Pattern identical — polling replaces always-on Kafka consumers, just at scale
```

#### Full Architecture
```
[S3] ──READ every 5 min──→ [Spark on EMR]
                            processes confirmed orders
                                   │
                                 WRITE
                                   │
                   [Cassandra — staging table]
                   partition key:  (service_name, time_bucket)
                   e.g.: ("loyalty", "2024-01-15-11:30")
                   clustering key: order_id
                                   │
               ┌───────────────────┼───────────────────┐
             POLL                POLL                 POLL
             5 min               5 min                daily
               │                   │                    │
        [Loyalty Svc]       [Partner Sync]         [Report Svc]
        reads its bucket    reads its bucket       generates daily file
        marks processed      sends to restaurant    to finance team
```

#### Key Scale Challenges
```
Cassandra staging partition design:
  Wrong: partition by order_id → each poll scans all partitions (scatter-gather)
  Right: partition by (service_name, time_bucket) → each consumer reads one partition
  Consumer polls: SELECT * FROM staging WHERE service='loyalty' AND bucket='11:30'
  → reads only its 5-min slice → fast, isolated

Time bucket granularity:
  5-min buckets match the polling interval exactly
  Each bucket: ~17M orders (5B/day ÷ 288 buckets) — manageable Cassandra partition
  After processing: consumer marks bucket done, does not re-read

Fraud at TB + Near RT:
  Fraud Service still synchronous HTTP → still uses Redis Cluster for features
  Near-RT speed applies to downstream consumers (loyalty etc), not fraud
  Fraud = always synchronous, always blocks order, regardless of speed tier
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| S3 | Raw minute-partitioned storage | Kafka WRITEs, Spark READs |
| Spark on EMR | Processes confirmed orders every 5 min | READs S3, WRITEs Cassandra staging |
| Cassandra staging | Distributed staging (partitioned by service + time bucket) | Spark WRITEs, Consumer services READ |
| Loyalty / Partner / Report Services | Poll Cassandra, process, mark done | READ + UPDATE Cassandra staging |
| Fraud Service Fleet | Sync fraud scoring (always) | READs Redis Cluster |

#### Interview Answer
```
Same polling pattern as GB S8 — Cassandra replaces PostgreSQL staging:
1. Spark on EMR every 5 min: reads S3 → writes to Cassandra staging (partitioned by service + time_bucket)
2. Loyalty/Partner/Report poll Cassandra: SELECT WHERE service=X AND bucket=current
3. Process batch → mark bucket done → wait for next 5-min window

Key insight: "Cassandra partition key (service_name, time_bucket) is critical.
Each consumer reads exactly one partition — no cross-partition scatter-gather.
Each 5-min bucket contains ~17M orders at TB scale — Cassandra handles this trivially."
```

---

### Scenario 21 — TB + Batch + Analytics

#### Problem Statement
```
Size:    5 TB/day, ~5B events/day
Speed:   Batch — nightly, data ready by 6am
Use:     Analytics — full business reporting, executive dashboards, finance
Who:     Food delivery platform — finance team, product analysts, executive reviews
```

#### What's Different vs Scenario 9 (GB + Batch + Analytics)
```
Scenario 9: Spark nightly on 1 machine processes 5 GB in <1 hour
Scenario 21: 5 TB nightly → single Spark machine takes 10+ hours → misses 6am SLA

What breaks:
  Spark (1 node):     5 TB ETL overnight → too slow for 6am deadline
  Snowflake (small):  dbt models on TB-scale tables → slow, warehouse timeout
  dbt serial models:  at TB, dbt must run models in parallel to meet SLA

Architecture change:
  Spark (1 node) → Spark on EMR (50-100 worker nodes, processes TB in parallel)
  Snowflake (small) → BigQuery (serverless) or Snowflake XL
  dbt: add parallelism config (threads: 16+), use incremental models
  Airflow: same orchestration, but with EMR job steps instead of local Spark
```

#### Full Architecture
```
[All day: orders → Cassandra → Debezium → Kafka → S3]

[2am: Airflow triggers nightly pipeline]

Airflow Step 1: [Spark on EMR — 50+ workers]
  READs: s3://events/date=yesterday/*.parquet (5 TB)
  JOINs: user, restaurant, driver, payment tables (from Cassandra snapshots)
  CLEANs: dedup, nulls, type casting
  WRITEs: raw + joined tables → BigQuery (or Snowflake XL)
  Runtime: 5 TB / 50 workers ≈ 45-60 min

Airflow Step 2: [dbt — 16 parallel threads]
  Runs SQL models on BigQuery:
  revenue_by_city_daily, restaurant_performance, user_cohort_analysis
  Incremental models: only processes yesterday's partition (not full history rescan)
  Runtime: 30-45 min

Airflow Step 3: alert "Pipeline complete — data ready for [date]"

[BigQuery / Snowflake XL] ──READ──→ [Tableau / Looker]
                                      analysts query in the morning
```

#### Key Scale Challenges
```
Incremental dbt models at TB scale:
  Full refresh: rescan all history (PBs) every night → hours, expensive
  Incremental: process only yesterday's new rows → append to existing table
  dbt incremental strategy: unique_key + updated_at → merge new rows only
  Always test: incremental result must match full refresh result (run both weekly)

Spark on EMR job tuning:
  Partition size: aim for 128-256 MB per Spark partition → 5 TB / 200 MB = ~25,000 partitions
  Worker count: 25,000 partitions / 10 tasks per worker = 2,500 concurrent tasks → ~250 workers
  Spot instances: use 80% spot + 20% on-demand (spot = 70% cheaper, occasional interruptions)
  Spark adaptive query execution (AQE): auto-tuning for join strategies and partition sizes

BigQuery at TB batch:
  Load job from GCS: fastest ingestion path for Spark output
  Partition by date: BigQuery partition pruning → queries on one date scan one partition only
  Clustering by city: further pruning for city-level analyst queries
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| S3 | Daily raw Parquet (TB, date-partitioned) | Kafka WRITEs, Spark READs |
| Airflow | Nightly orchestration (EMR → dbt → alert) | Triggers jobs |
| Spark on EMR (50+ workers) | TB nightly ETL with parallelism | READs S3, WRITEs BigQuery |
| dbt (16+ threads, incremental) | SQL business logic, partition-aware | READs BigQuery raw, WRITEs BigQuery clean |
| BigQuery / Snowflake XL | TB data warehouse | dbt WRITEs, Tableau READs |

#### Interview Answer
```
Same Airflow + Spark + dbt pattern — scale forces distribution:
1. Airflow 2am: EMR cluster (50+ workers) reads 5 TB S3 → joins → loads BigQuery
2. dbt (16 threads, incremental models): processes yesterday's partition only
3. Analysts query BigQuery by 6am — partitioned by date, clustered by city

Key insight: "Incremental dbt models are non-negotiable at TB scale.
Full refresh means scanning all historical TB every night. Incremental means
scanning yesterday's 5 TB only. 100x cheaper and 10x faster."
```

---

### Scenario 22 — TB + Batch + ML

#### Problem Statement
```
Size:    5 TB/day → months of history = PBs of training data
Speed:   Batch — weekly model training
Use:     ML Training — retrain delivery time model on 6 months of TB/day data
Who:     Food delivery ML team — improving model accuracy at national scale
```

#### What's Different vs Scenario 10 (GB + Batch + ML)
```
Scenario 10: Spark reads 6 months × 5 GB/day = ~900 GB training data — single node handles
Scenario 22: Spark reads 6 months × 5 TB/day = ~900 TB training data — single node impossible

What breaks:
  Spark (1 node):   900 TB feature engineering → weeks, not hours
  ML training:      900 TB training dataset → single GPU/CPU node → weeks to train
  MLflow (local):   model artifacts at TB scale need managed registry (SageMaker)

Architecture change:
  Spark (1 node) → Spark on EMR (distributed feature engineering across 100+ workers)
  ML Training job → SageMaker distributed training (multiple GPU instances)
  MLflow → SageMaker Model Registry (managed, integrated with deployment)
  Pattern identical — feature engineering → training → registry → A/B test → deploy
```

#### Full Architecture
```
[S3 — 6 months × 5 TB/day = ~900 TB raw Parquet]

[Airflow — weekly trigger]
      │
      ├── Step 1: [Spark on EMR — 100+ workers]
      │           READs 900 TB from S3
      │           feature engineering: joins, aggregations, label creation
      │           WRITEs training_data.parquet → S3 (100-200 TB feature matrix)
      │           Runtime: ~4-6 hours
      │
      ├── Step 2: [SageMaker Training Job]
      │           distributed training across 10-20 GPU instances (ml.p3.16xlarge)
      │           reads training_data.parquet from S3
      │           trains XGBoost / neural net on 100 TB feature matrix
      │           evaluates: RMSE on held-out test set
      │           WRITEs model artifact → SageMaker Model Registry
      │           Runtime: ~6-12 hours
      │
      └── Step 3: [A/B test deployment]
                  5% traffic to new model via SageMaker endpoint
                  monitor RMSE vs old model for 24 hours
                  if better → promote to 100%
                  if worse → rollback (one CLI command in SageMaker)
```

#### Key Scale Challenges
```
Distributed feature engineering at 900 TB:
  S3 read: 900 TB / 100 Spark workers = 9 TB per worker → manageable
  Memory: use Spark's lazy evaluation — never load 900 TB into RAM
  Key optimization: feature engineering should be pushdown-compatible (use Parquet column pruning)
  Only read needed columns: not all 50 columns, just the 10 needed for feature computation

SageMaker distributed training:
  Data parallelism: each GPU gets different batch of training data
  10 GPU instances × same model → gradient averaging after each batch (all-reduce)
  900 TB → cannot fit in GPU RAM → mini-batch training (read 10K rows at a time from S3)
  SageMaker Pipe mode: streams training data from S3 directly to training job (no disk copy)

Feature selection at TB:
  More data ≠ better model (diminishing returns after certain point)
  Sample: use 10% random sample for initial training → full data only for final run
  Feature importance: drop features with near-zero importance to reduce training data size
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| S3 (900 TB, 6-month history) | Training data source | Kafka WRITEs raw, Spark READs |
| Spark on EMR (100+ workers) | Distributed feature engineering | READs 900 TB S3, WRITEs feature matrix |
| SageMaker Training (GPU fleet) | Distributed model training | READs feature matrix from S3 |
| SageMaker Model Registry | Versioned model artifacts | Training WRITEs, Inference endpoints READ |

#### Interview Answer
```
Same pipeline as GB — scale forces distribution:
1. Airflow weekly: Spark on EMR (100+ workers) reads 900 TB S3 → feature matrix (100 TB)
2. SageMaker distributed training (10-20 GPUs): trains on feature matrix → model artifact
3. A/B test: 5% traffic → monitor 24 hrs → promote or rollback

Key insight: "900 TB of training data sounds scary — but Spark parallelizes it.
The real challenge is feature selection: if 10% of data gives 95% of accuracy,
train on 10% first, then full only if justified. Data engineers waste GPU hours
training on full 900 TB when a sample works equally well."
```

---

### Scenario 23 — TB + Batch + Dashboards

#### Problem Statement
```
Size:    5 TB/day, ~5B events/day
Speed:   Batch — daily, dashboards updated once per day
Use:     Dashboards — executive KPI dashboard, daily business reviews
Who:     Food delivery platform leadership — CEO/CFO, city general managers
```

#### What's Different vs Scenario 11 (GB + Batch + Dashboards)
```
Scenario 11: dbt builds daily_kpi_summary (1 row/day) — fast at GB, BigQuery reads trivially
Scenario 23: Source tables are now TB-scale — dbt models must be efficient or they're slow/expensive

What breaks:
  dbt full refresh: re-scanning TB history every night to produce 1 summary row → expensive
  BigQuery cost: TB scanned × $5/TB → full history scan every night = $$$

Architecture change:
  dbt incremental models: only process yesterday's partition, append to summary table
  BigQuery partitioned tables: date-partitioned source tables → dbt only scans yesterday
  Output (daily_kpi_summary) stays same — 1 row per day, 6 KPIs — still instant to read
  Governance: column-level access control (executives see revenue, not raw order data)
```

#### Full Architecture
```
[Same nightly pipeline as S21]
Airflow → Spark on EMR → BigQuery (raw, date-partitioned)

[dbt — incremental models, 16 threads]
  Reads ONLY: yesterday's partition (WHERE date = '2024-01-15')
  Produces: 1 row in daily_kpi_summary for yesterday
  APPENDS to existing summary table (never overwrites history)
  Runtime: 15-20 min (scanning 5 TB, not 900 TB history)

[BigQuery — daily_kpi_summary table]
┌────────────────────────────────────────────┐
│ date      │ revenue  │ orders │ avg_del ... │
│ 2024-01-14│ 2.3M     │ 47K    │ 31 min ...  │
│ 2024-01-15│ 2.7M     │ 52K    │ 29 min ...  │  ← yesterday, just added
└────────────────────────────────────────────┘

[Dashboard Tool — Looker / Tableau]
  SELECT * FROM daily_kpi_summary ORDER BY date DESC LIMIT 30
  → reads 30 rows, <10ms, regardless of TB history
  Auto-refreshes at 6am when pipeline completes
```

#### Key Scale Challenges
```
BigQuery cost control at TB scale:
  Full scan of 900 TB history every night: 900 TB × $5/TB = $4,500/night → unacceptable
  Incremental dbt: scans only yesterday's 5 TB → $25/night → acceptable
  Partitioned tables: BigQuery only bills for partitions actually scanned
  Rule: every large BigQuery table must be date-partitioned

Column-level governance:
  Executives see: revenue, order_count, avg_delivery — aggregated KPIs
  City managers see: their city's KPIs only (row-level security in BigQuery)
  Analysts see: raw order table (Snowflake or BigQuery analyst dataset)
  Implementation: BigQuery column-level security + row access policies

dbt at TB — what changes:
  Same SQL models as GB, but add: `partition_by` and `incremental_strategy: merge`
  Test incremental vs full refresh weekly: results must match
  dbt documentation: at TB scale, model lineage documentation is essential for debugging
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| S3 / GCS | Daily raw (date-partitioned, TB) | Kafka WRITEs, Spark READs |
| Spark on EMR | Nightly ETL | READs S3, WRITEs BigQuery (raw, date-partitioned) |
| dbt (incremental, 16 threads) | Yesterday's partition only → append to summary | READs BigQuery raw, WRITEs daily_kpi_summary |
| BigQuery (date-partitioned + row security) | TB warehouse, access-controlled | dbt WRITEs, Dashboard READs |

#### Interview Answer
```
Same summary table pattern as GB — incremental dbt is the key addition:
1. Spark on EMR nightly → BigQuery (date-partitioned raw tables)
2. dbt incremental: scans yesterday's 5 TB only → appends 1 row to daily_kpi_summary
3. Dashboard reads 30 rows from summary → instant, regardless of 900 TB history

Key insight: "The summary table output is identical to GB — 1 row per day, 6 KPIs.
What changes is how we compute it: incremental dbt scanning 5 TB, not full history.
BigQuery date partitioning makes this the difference between $25/night and $4,500/night."
```

---

### Scenario 24 — TB + Batch + Another System

#### Problem Statement
```
Size:    5 TB/day, ~5B events/day
Speed:   Batch — nightly file delivery to partners and regulators
Use:     Another System — daily order exports to restaurant partners, regulatory reports
Who:     Food delivery platform — 1000+ restaurant partners, multiple regulatory bodies
```

#### What's Different vs Scenario 12 (GB + Batch + Another System)
```
Scenario 12: Single Spark generates files for each partner, single SFTP upload
Scenario 24: 5 TB/day × 1000+ partners → single Spark and single SFTP delivery impossible

What breaks:
  Single Spark: generating 1000 partner files from 5 TB → hours per file sequentially
  Single SFTP:  1000+ file uploads sequentially → takes all night
  PostgreSQL ack log: 1000+ partners × 10K file chunks each → millions of ack rows → saturation

Architecture change:
  Spark (1 node) → Spark on EMR (parallel file generation per partner)
  Single SFTP → parallel delivery fleet (N agents, each handles subset of partners)
  PostgreSQL ack log → Cassandra ack log (high write throughput for millions of acks)
  File chunking: split each partner's file into manageable chunks (parallel upload)
```

#### Full Architecture
```
[S3 — yesterday's 5 TB, date-partitioned]

[Airflow — midnight trigger, per-partner DAGs in parallel]

For each partner batch (100 partners per EMR job):

  [Spark on EMR — 50 workers]
  FILTER: orders for this partner group
  FORMAT: partner-agreed CSV/JSON schema
  SPLIT:  into 1 GB chunks (5 TB / 1000 partners = 5 GB avg per partner → 5 chunks)
  WRITE:  partner_A/chunk-0001.csv ... chunk-0005.csv → S3 staging
  Runtime: parallel across partner batches

  [Validation per chunk]
  row_count, checksum, schema — abort if any chunk fails

  [Delivery fleet — parallel per partner]
  5 chunks × 1000 partners = 5000 parallel SFTP/S3 uploads
  Each upload agent: picks up chunks for its assigned partners
  Acks each successful chunk delivery

  [Cassandra — ack log]
  partition key: (partner_id, date)
  row: (chunk_id, delivered_at, ack_received_at)
  5000 concurrent ack writes → distributed, no hotspot

  [Airflow monitor]
  checks all chunks acked for all partners by 3am
  pages oncall for any partner with missing chunks
```

#### Key Scale Challenges
```
Parallel partner file generation:
  1000 partners × sequential Spark job = 1000 × (runtime per partner) → too slow
  Solution: group partners into batches of 100, run 10 EMR jobs in parallel
  Each EMR job: 50 workers × 100 partners = 500 tasks → fast
  Airflow: 10 parallel tasks, each triggering one EMR job for 100-partner batch

Cassandra ack log design:
  Partition key: (partner_id, date) — all chunks for one partner on same date in one partition
  Monitoring query: SELECT * FROM acks WHERE partner_id=X AND date='2024-01-15'
  → reads one partition, finds all chunk acks for this partner → O(1) check
  5000 concurrent writes → spread across all (partner_id, date) partitions → no hotspot

Regulatory files at TB:
  Tax authority: single file (not chunked) — regulators often cannot handle parallel chunks
  Solution: Spark generates single large file for regulator → compress (gzip reduces 5 GB → 500 MB)
  Regulatory SLA: file must arrive by midnight → highest-priority delivery job
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| S3 (5 TB, date-partitioned) | Source data | Kafka WRITEs, Spark READs |
| Spark on EMR (parallel per partner batch) | Partner file generation + chunking | READs S3, WRITEs S3 staging chunks |
| S3 staging | Partner file chunks (1 GB each) | Spark WRITEs, delivery fleet READs |
| Parallel delivery fleet | 5000 concurrent SFTP/S3 uploads | READs S3 staging, WRITEs to partner endpoints |
| Cassandra ack log | 5000 concurrent ack writes | Delivery fleet WRITEs, Airflow monitors |
| Airflow | Per-partner parallel DAGs + SLA monitoring | Orchestrates everything |

#### Interview Answer
```
Same file export pattern as GB — parallelism at every layer:
1. Airflow: 10 parallel EMR jobs, each generating files for 100-partner batch
2. Spark on EMR: chunks each partner's data into 1 GB files → S3 staging
3. Delivery fleet: 5000 parallel SFTP uploads
4. Cassandra ack log: 5000 concurrent writes, no hotspot (partition by partner + date)
5. Airflow monitors: all partners fully acked by 3am? Page oncall if not.

Key insight: "Partner file delivery scales by parallelising at every layer:
parallel Spark jobs, parallel file chunks, parallel uploads, parallel ack writes.
The bottleneck is always the slowest partner's SFTP server, not your pipeline."
```

---

## PB Scale Scenarios (5 PB/day — Global Scale)

---

### Scenario 25 — PB + Real-time + Analytics

#### Problem Statement
```
Size:    5 PB/day, ~5T events/day
Speed:   Real-time — seconds
Use:     Analytics — global ops visibility, zone-level live monitoring
Who:     Food delivery platform at global scale — hundreds of millions of daily orders
```

#### What's Different vs Scenario 13 (TB + Real-time + Analytics)
```
Scenario 13: Cassandra (regional) + Kafka Cluster + Druid/Pinot — works at TB
Scenario 25: PB/day = global multi-region traffic, single-region components create lag

What breaks at PB scale:
  Cassandra (single region): global writes from Asia + EU + US to one region → latency
  Kafka Cluster (single region): PB/day throughput + cross-region replication lag
  Druid/Pinot (single cluster): one global cluster becomes the hotspot

Architecture change:
  Cassandra → Bigtable / Spanner (globally distributed, multi-region writes)
  Kafka Cluster → Kafka + Pub/Sub (Google) or Pulsar (multi-region, global topics)
  Debezium → Bigtable Change Streams / Spanner Change Streams
  Flink → Flink Global Cluster / Apache Beam (run anywhere: Flink, Dataflow, Spark)
  Druid/Pinot → Apache Pinot (purpose-built for PB user-facing, multi-region)
```

#### Full Architecture
```
[Global App — multiple regions]
   US-EAST              EU-WEST              ASIA-PACIFIC
      │                    │                      │
   WRITE                WRITE                  WRITE
      │                    │                      │
[Spanner / Bigtable — globally distributed OLTP]
   multi-region writes, globally consistent
      │
[Bigtable Change Streams / Spanner Change Streams]
      │
[Google Pub/Sub / Kafka + MirrorMaker 2]
global topics, cross-region replication
      │                              │
[Apache Beam / Flink Global]        WRITE to [GCS / S3]
runs on Cloud Dataflow              globally replicated
multi-region workers                per-second partitions
aggregates every 10 sec
      │
    WRITE
      │
[Apache Pinot — global cluster]
multi-region ingestion nodes
sub-second queries at PB scale
      │
    READ
      │
[Grafana — global dashboard]
regional views, global rollup
```

#### Key Scale Challenges
```
Multi-region write conflict resolution:
  Spanner: uses TrueTime API — globally consistent timestamps, no conflicts
  Bigtable: last-write-wins per row (not globally consistent — acceptable for analytics)
  Choose Spanner: if strong consistency needed across regions (financial data)
  Choose Bigtable: if eventual consistency acceptable (analytics counts, estimates)

Apache Pinot at PB:
  Designed by LinkedIn for 600B+ events/day (PB scale)
  Segment-based: data split into immutable segments, each segment on multiple replicas
  Real-time nodes ingest from Pub/Sub, server nodes answer queries
  Sub-10ms p99 at PB — reason it replaced Druid at LinkedIn scale

Apache Beam:
  Write once, run anywhere: same pipeline code runs on Dataflow (Google), Flink, Spark
  At PB: run on Cloud Dataflow (serverless, auto-scales globally)
  Advantage: no cluster to manage, scales to PB automatically
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| Spanner / Bigtable | Global distributed OLTP | App WRITEs globally, Change Streams READ |
| Bigtable / Spanner Change Streams | PB-scale CDC | READs change log, WRITEs Pub/Sub |
| Google Pub/Sub / Pulsar | Global event bus (multi-region) | Consumers READ |
| Apache Beam on Dataflow | Serverless global stream processing | READs Pub/Sub, WRITEs Pinot |
| Apache Pinot (global cluster) | PB real-time OLAP | Beam WRITEs, Grafana READs |
| GCS | Raw per-second partitioned storage | Pub/Sub WRITEs |

#### Interview Answer
```
Same pattern as TB — multi-region forces global components:
1. App writes to Spanner/Bigtable globally (multi-region, consistent)
2. Change Streams → Pub/Sub (global topics, cross-region replication)
3. Apache Beam on Dataflow: serverless, globally auto-scaling → Apache Pinot
4. Grafana: regional + global rollup views from Pinot

Key insight: "Apache Beam is the PB unlock — same pipeline code runs on
Dataflow/Flink/Spark. At PB, Dataflow's serverless auto-scaling eliminates
cluster sizing entirely. You pay per processed event, not per idle worker."
```

---

### Scenario 26 — PB + Real-time + ML

#### Problem Statement
```
Size:    5 PB/day, ~5T events/day
Speed:   Real-time — ML prediction in milliseconds
Use:     ML Inference — delivery time prediction at global scale
Who:     Food delivery platform — billions of concurrent predictions across all regions
```

#### What's Different vs Scenario 14 (TB + Real-time + ML)
```
Scenario 14: Redis Cluster holds features — works at TB (billions of keys)
Scenario 26: Trillions of feature keys at PB scale → Redis Cluster memory insufficient

What breaks at PB scale:
  Redis Cluster: trillions of keys × 200 bytes = TB of RAM needed → too expensive, too slow
  ML Service fleet (regional): global users need predictions from nearest region — latency

Architecture change:
  Redis Cluster → Aerospike (designed for PB feature stores, SSD-backed, cheaper than RAM)
  ML Service → global fleet (regional deployments, routes to nearest region)
  Flink Global Cluster → Apache Beam on Dataflow for feature computation
  Same Lambda Architecture — Beam (real-time) + Spark on Dataproc (historical)
```

#### Full Architecture
```
[Pub/Sub global topics]
      │
[Apache Beam on Dataflow]         [Spark on Dataproc — hourly]
real-time features                historical features
zone traffic, active drivers      avg_prep_time, user_history
      │                                   │
    WRITE                               WRITE
      │                                   │
[Aerospike — global Feature Store]
SSD-backed, sub-ms reads
replicated across regions
      │
    READ (nearest region)
      │
[ML Service — global fleet]
deployed in each region (US/EU/APAC)
reads Aerospike in same region (<1ms)
runs model
RESPONDS in <10ms total
      │
    RESPONSE → App (nearest region routing)
```

#### Key Scale Challenges
```
Aerospike vs Redis at PB:
  Redis Cluster: all data in RAM → $$$$ at PB scale (TB of RAM needed)
  Aerospike:     RAM for index, SSD for values → 10x cheaper, same sub-ms reads
  Read path: key hash → RAM index lookup → SSD read → return value (1-2ms total)
  Write path: RAM write + async SSD write → fast writes, durable storage

Global ML fleet routing:
  User in Mumbai → nearest ML Service in APAC region → Aerospike APAC replica → <5ms total
  User in London → ML Service in EU → Aerospike EU replica → <5ms total
  DNS-based routing (Route 53 latency routing) sends user to nearest region

Model deployment at PB:
  One model artifact in S3/GCS (replicated globally)
  Each regional fleet downloads model on startup → all regions serve same model version
  Blue-green deployment: deploy new model to APAC first → validate → EU → US-EAST
  Never deploy to all regions simultaneously — risk of global outage
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| Pub/Sub / Pulsar | Global event bus | Apache Beam READs |
| Apache Beam on Dataflow | Real-time feature computation (global) | READs Pub/Sub, WRITEs Aerospike |
| Spark on Dataproc | Historical feature computation (hourly) | READs GCS, WRITEs Aerospike |
| Aerospike (global, SSD-backed) | PB Feature Store (RAM index + SSD values) | Beam/Spark WRITE, ML Fleet READs |
| ML Service (regional fleets) | Global inference, nearest-region routing | READs Aerospike, RESPONDs to App |

#### Interview Answer
```
Same Lambda Architecture as TB — Aerospike replaces Redis Cluster:
1. Beam on Dataflow: real-time features → Aerospike (globally replicated)
2. Spark on Dataproc: historical features → Aerospike
3. Regional ML Service fleets: read local Aerospike replica → model → respond <10ms

Key insight: "Aerospike is the PB feature store answer. Redis Cluster needs all
data in RAM — at PB scale that means TB of RAM. Aerospike: RAM for index only,
SSD for values. Same sub-ms read latency, 10x lower cost at PB scale."
```

---

### Scenario 27 — PB + Real-time + Dashboards

#### Problem Statement
```
Size:    5 PB/day, ~5T events/day
Speed:   Real-time ops (30-sec refresh) + Near-real-time business (15-30 min)
Use:     Two dashboards — global ops visibility + global business analytics
Who:     Food delivery platform global leadership — regional ops teams, global executives
```

#### What's Different vs Scenario 15 (TB + Real-time + Dashboards)
```
Scenario 15: Druid/Pinot (regional) + BigQuery (regional) — works at TB
Scenario 27: PB global — one regional OLAP cluster becomes bottleneck, global execs need global view

Architecture change:
  Druid/Pinot (regional) → Apache Pinot (global cluster, multi-region brokers)
  BigQuery (regional) → BigQuery (global dataset, cross-region replication)
  Beam on Dataflow (regional) → Beam on Dataflow (multi-region workers)
  Grafana → global Grafana with regional + global rollup panels
```

#### Full Architecture
```
[Pub/Sub global topics]
      │
[Beam on Dataflow — multi-region workers]
aggregates every 30 sec (per-region + global)
      │                              │
    WRITE (regional)              WRITE (global rollup)
      │                              │
[Apache Pinot global]          [BigQuery global dataset]
regional brokers                cross-region replicated
serve regional dashboards        (US/EU/APAC availability)
      │                              │
    READ                           READ
      │                               │
[Grafana — regional dashboards]  [Tableau — global dashboard]
"Mumbai ops: 1200 orders/min"    "Global revenue this week: $450M"
"APAC regional: 48K orders/min"  "YoY growth by region"
```

#### Key Scale Challenges
```
Global vs regional aggregation:
  Regional panel: Beam aggregates per-region events → Pinot regional broker → Grafana
  Global panel: Beam aggregates ALL regions' events → Pinot global broker → Grafana
  Two levels of aggregation in same Beam pipeline (partitioned by region + one global)

BigQuery cross-region replication:
  BigQuery dataset replication: primary in US, replica in EU and APAC
  Tableau reads nearest replica → low-latency global dashboard
  Write only to primary — replication is automatic (minutes lag, acceptable for business)

Grafana at PB:
  Separate Grafana instances per region (avoid cross-region dashboard queries)
  Global Grafana reads Pinot global broker (slightly higher latency, acceptable for exec view)
  Alert routing: Mumbai ops alert → APAC oncall. Global revenue alert → global VP.
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| Pub/Sub (global) | PB event bus | Beam READs |
| Beam on Dataflow (multi-region) | Global + regional aggregation | READs Pub/Sub, WRITEs Pinot + BigQuery |
| Apache Pinot (global cluster) | PB real-time OLAP | Beam WRITEs, Grafana READs |
| BigQuery (global dataset) | PB batch OLAP (replicated) | Beam WRITEs, Tableau READs |

#### Interview Answer
```
Same two-stack pattern — global distribution added:
1. Beam on Dataflow: aggregates per-region + global rollup simultaneously
2. Apache Pinot global: regional brokers serve local dashboards, global broker for exec view
3. BigQuery global dataset: cross-region replicated, Tableau reads nearest replica

Key insight: "Two levels of aggregation in one Beam pipeline — regional and global.
Pinot regional brokers keep local dashboard latency low. Global broker adds 20-30ms
for global execs — acceptable, since they're not watching millisecond ops dashboards."
```

---

### Scenario 28 — PB + Real-time + Another System

#### Problem Statement
```
Size:    5 PB/day, ~5T events/day
Speed:   Real-time — milliseconds to seconds
Use:     Another System — global notification, inventory, fraud at PB scale
Who:     Food delivery platform — billions of events triggering downstream systems globally
```

#### What's Different vs Scenario 16 (TB + Real-time + Another System)
```
Scenario 16: Cassandra idempotency, auto-scaling fleets (regional) — works at TB
Scenario 28: PB global — idempotency across regions (same order from multiple regions?),
             global consumer fleet coordination

What breaks at PB scale:
  Cassandra idempotency (single region): global order may be processed by two regional consumers
  Consumer fleet (regional): needs globally coordinated idempotency to prevent cross-region dups

Architecture change:
  Cassandra idempotency → Spanner idempotency (globally consistent, strong consistency)
  Consumer fleets → global auto-scaling fleets with regional assignment
  Fraud Service → globally deployed with nearest-region routing (Aerospike for features)
  Pattern identical — Schema Registry, DLQ, Consumer Groups — just global
```

#### Full Architecture
```
[Spanner — globally consistent OLTP]
      │
[Spanner Change Streams → Pub/Sub global topics]
      │
[Global Schema Registry — each region has local cache]
      │
Consumer Groups (global, each region assigned partitions):
US-EAST partition 0-33: [Notification Fleet US]
EU-WEST partition 34-66: [Notification Fleet EU]
APAC partition 67-99:   [Notification Fleet APAC]

Each regional fleet:
  READs assigned Pub/Sub partitions
  idempotency check → [Spanner idempotency table] (globally consistent)
  sends notification to regional push service
  WRITEs ack to Spanner

[Fraud Service — regional, always sync]
READs [Aerospike — regional replica]
RESPONDS <200ms always
```

#### Key Scale Challenges
```
Global idempotency with Spanner:
  Problem: Order 98765 created in US, but EU consumer also picks it up (replication lag)
  Solution: Spanner globally consistent INSERT IF NOT EXISTS
  Spanner TrueTime: globally consistent timestamps → INSERT wins globally → one winner
  Cost: Spanner is expensive → use only for idempotency (small table), not full event store

Partition-to-region assignment:
  Kafka/Pub/Sub: assign partition ranges to regions
  US consumer group reads partitions 0-33 only
  EU consumer group reads partitions 34-66 only
  Prevents cross-region duplicate processing (each partition owned by one region)
  Rebalancing: when region fails, reassign its partitions to another region's fleet

DLQ at PB:
  DLQ per region, DLQ consumer per region
  Failed event in EU DLQ → EU DLQ consumer retries → never crosses regions
  Monitor: DLQ depth per region. Alert if DLQ processing falls behind (event TTL risk).
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| Spanner | Global OLTP + globally consistent idempotency | App WRITEs, Change Streams READ |
| Pub/Sub (global partitioned topics) | PB event bus, partition-to-region assignment | Regional consumer fleets READ |
| Regional Consumer Fleets | Process events, idempotency check, act | READ Pub/Sub, WRITE Spanner idempotency |
| Fraud Service (regional, sync) | Global fraud scoring, nearest-region routing | READ Aerospike regional replica |
| Aerospike (global) | Fraud features, globally replicated | Beam WRITEs, regional Fraud Fleet READs |

#### Interview Answer
```
Same idempotency + Consumer Group pattern — Spanner adds global consistency:
1. Spanner Change Streams → Pub/Sub global (partitions assigned to regions)
2. Regional Consumer Fleets process their assigned partitions
3. Idempotency: Spanner globally consistent INSERT IF NOT EXISTS — one winner globally
4. Fraud: regional Fraud Service fleet reads local Aerospike replica → <200ms

Key insight: "Partition-to-region assignment is the PB Another System unlock.
Each region owns a subset of Pub/Sub partitions. No cross-region event processing.
No cross-region duplicate risk. Spanner idempotency only needed for edge cases
where replication lag causes a partition to be briefly visible in two regions."
```

---

### Scenario 29 — PB + Near Real-time + Analytics

#### Problem Statement
```
Size:    5 PB/day, ~5T events/day
Speed:   Near Real-time — 5-15 minute delay
Use:     Analytics — global regional monitoring with 5-min freshness
Who:     Food delivery global ops — regional directors reviewing 15-min trend windows
```

#### What's Different vs Scenario 17 (TB + Near RT + Analytics)
```
Scenario 17: Spark on EMR reads 1 TB per 5-min batch → works at TB
Scenario 29: 5 PB/day = 17 TB per 5-min micro-batch → EMR cluster needs 10x more workers

Architecture change:
  Spark on EMR (fixed cluster) → Spark on Dataproc (auto-scaling, PB-grade)
                               OR Beam on Dataflow (serverless, scales to PB automatically)
  GCS per-second partitions replace S3 minute partitions
  BigQuery replaces Redshift (BigQuery handles PB natively, serverless)
  Pattern identical — micro-batch every 5 min, two-dashboard output
```

#### Full Architecture
```
[Pub/Sub] ──WRITE──→ [GCS]
                      per-second partitions
                      gs://events/date=.../hour=.../minute=.../second=.../
                           │
                     READ every 5 min
                     (only last 5 min of second-folders)
                           │
            [Beam on Dataflow — serverless]
            auto-scales to 17 TB/batch automatically
            no cluster sizing needed
            aggregates → writes output
                           │
                  ┌────────┴────────┐
                WRITE             WRITE
                  │                 │
          [Apache Pinot]      [BigQuery — serverless]
          Near-RT ops         Business analytics
          5-min fresh         hourly fresh
                  │                 │
              [Grafana]        [Tableau]
              5-min refresh    on-demand
```

#### Key Scale Challenges
```
Beam on Dataflow for PB micro-batch:
  Dataflow is serverless — no workers to provision for 17 TB/batch
  Auto-scales: spins up 1000s of workers for peak batches, releases immediately after
  Cost: pay per worker-hour used — no idle cost between batches
  Streaming mode: Dataflow can run in streaming mode (true streaming, not micro-batch)
                  If SLA tightens to 1 min → switch mode, same code

Per-second GCS partitioning:
  5-min batch = 300 seconds of data
  Dataflow reads: gs://events/date=.../hour=.../minute=11/second=00/ through second=59/
                  + minute=12/ ... + minute=15/
  Column pruning: Dataflow reads only needed columns (not all 50 fields)

BigQuery at PB:
  BigQuery is fully serverless — no warehouse sizing at PB
  Query cost: $5/TB scanned — partitioning + column pruning critical
  For 5-min batch write: Dataflow → BigQuery streaming insert (real-time) or load job (batch)
  Streaming insert: data available immediately, slightly more expensive
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| Pub/Sub (global) | PB event bus | WRITEs GCS |
| GCS (per-second partitions) | Raw storage, precise time slicing | Pub/Sub WRITEs, Dataflow READs |
| Beam on Dataflow (serverless) | 5-min micro-batch, auto-scaling | READs GCS, WRITEs Pinot + BigQuery |
| Apache Pinot | Near-RT ops OLAP | Dataflow WRITEs, Grafana READs |
| BigQuery (serverless) | Business analytics | Dataflow WRITEs, Tableau READs |

#### Interview Answer
```
Same micro-batch pattern — Dataflow serverless handles PB scale automatically:
1. Pub/Sub → GCS (per-second partitions)
2. Dataflow every 5 min: reads last 300 seconds of GCS → aggregates → Pinot + BigQuery
3. Grafana READs Pinot (5-min fresh), Tableau READs BigQuery (hourly)

Key insight: "Dataflow serverless is the PB micro-batch answer.
No EMR cluster to size — Dataflow auto-scales to 17 TB in 5 min
and releases workers immediately. Same pipeline code as TB, zero config change."
```

---

### Scenario 30 — PB + Near Real-time + ML

#### Problem Statement
```
Size:    5 PB/day, ~5T events/day
Speed:   Near Real-time — predictions updated every 5-15 min globally
Use:     ML — pre-computed delivery estimates for hundreds of millions of active orders
Who:     Food delivery global platform — billions of tracking screen refreshes per day
```

#### What's Different vs Scenario 18 (TB + Near RT + ML)
```
Scenario 18: Spark on EMR scores 50M active orders in 5 min — works at TB
Scenario 30: Hundreds of millions of active orders → scoring in 5 min needs global parallelism

Architecture change:
  Spark on EMR → Spark on Dataproc (massive clusters, auto-scaling)
  Redis Cluster → Aerospike (PB-scale feature + prediction store)
  Pattern identical — pre-compute in batch, App GETs result
```

#### Full Architecture
```
[GCS — active orders snapshot, updated every 5 min]
[Aerospike — feature store, globally replicated]
       │
[Beam on Dataflow / Spark on Dataproc — every 5 min]
reads 500M active orders (distributed across workers)
batch-reads features from Aerospike (MGET per partition)
distributes ML model scoring (broadcast model to all workers)
WRITEs 500M predictions → Aerospike
       │
[Aerospike — prediction store]
prediction:order:{id} → "19 min"
SSD-backed, globally replicated
TTL: 15 min per key
       │
[App / Tracking Screen — regional]
GET prediction:order:{id} from nearest Aerospike region
→ "Your order arrives in 19 min"
refreshes every 5 min
```

#### Key Scale Challenges
```
Scoring 500M orders in 5 min:
  Dataproc cluster: 500M orders / 5 min / 10K workers = 1000 orders per worker per minute
  Each worker: reads 1000 order features → runs model 1000 times → writes predictions
  Model: must be fast (XGBoost < 1ms per prediction → 1000 predictions per worker = 1 sec)

Aerospike write throughput for 500M predictions:
  500M writes in 5 min = 1.6M writes/sec
  Aerospike: designed for millions of writes/sec (LinkedIn uses it at this scale)
  Namespace partitioning: predictions in separate namespace from features (different TTL policy)

Feature read during scoring:
  500M orders × 5 feature reads each = 2.5B feature reads in 5 min
  Not feasible via individual Redis/Aerospike GETs
  Solution: export feature snapshot to GCS at start of batch → Spark reads GCS (not Aerospike)
  → avoids 2.5B network calls to Aerospike during scoring
  → Aerospike used only for serving predictions to App (500M concurrent readers)
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| GCS (feature snapshot + active orders) | Batch input for scoring | Beam WRITEs snapshot, Dataproc READs |
| Spark on Dataproc | 500M-order distributed scoring | READs GCS, WRITEs Aerospike predictions |
| Aerospike (global) | Feature store + prediction store (SSD-backed) | Dataproc WRITEs predictions, App READs |

#### Interview Answer
```
Same pre-compute pattern — Aerospike + Dataproc handle PB:
1. Dataproc every 5 min: reads 500M orders from GCS + feature snapshot
2. Distributes scoring: 10K workers × 50K orders each → 500M predictions in <4 min
3. WRITEs 500M predictions to Aerospike (1.6M writes/sec — routine for Aerospike)
4. App GET from nearest Aerospike region → <2ms

Key insight: "Export feature snapshot to GCS before batch scoring.
Avoid 2.5B Aerospike reads during the scoring run — that's 8M reads/sec
which stresses the feature store and adds latency to live inference requests."
```

---

### Scenario 31 — PB + Near Real-time + Dashboards

#### Problem Statement
```
Size:    5 PB/day, ~5T events/day
Speed:   Near Real-time — 5-15 min delay, both audiences
Use:     Two dashboards — global ops + global business, both accept 5-min delay
Who:     Food delivery global — global ops directors + C-suite executive dashboards
```

#### What's Different vs Scenario 19 (TB + Near RT + Dashboards)
```
Scenario 19: BigQuery regional, one Spark on EMR batch every 5 min — works at TB
Scenario 31: PB global — one BigQuery dataset per region, global rollup needed for execs

Architecture change:
  Spark on EMR → Beam on Dataflow (serverless, global)
  BigQuery (regional) → BigQuery (multi-region dataset)
  Same one-tool pattern — BigQuery serves both ops and business globally
```

#### Full Architecture
```
[Pub/Sub global] → [GCS per-second partitions]
       │
[Beam on Dataflow — serverless, every 5 min]
computes regional aggregates + global rollup
       │
     WRITE
       │
[BigQuery — multi-region dataset]
US, EU, APAC replicas (reads served from nearest)
5-min fresh data
       │
  ┌────┴────┐
READ       READ
  │           │
[Grafana]  [Tableau]
ops teams  C-suite
per-region  global view
BigQuery   BigQuery
slots      on-demand
reserved
```

#### Key Scale Challenges
```
BigQuery multi-region at PB:
  Multi-region dataset: data stored in US + EU + APAC simultaneously
  Reads: served from nearest region (low latency globally)
  Writes: go to all regions (slightly higher write latency, acceptable for 5-min batch)
  Cost: multi-region storage 2x single-region — justified for global dashboard SLA

Slot reservation per region:
  Each region's ops team has reserved BigQuery slots for their Grafana dashboards
  Global C-suite Tableau uses on-demand slots (burst queries, lower frequency)
  Prevents one region's analyst query from slowing another region's ops dashboard
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| Beam on Dataflow (global, serverless) | 5-min micro-batch, regional + global aggregation | READs GCS, WRITEs BigQuery |
| BigQuery (multi-region) | PB OLAP, globally replicated, slot reservation | Dataflow WRITEs, Grafana + Tableau READ |

#### Interview Answer
```
Same one-tool pattern as GB S7 / TB S19 — BigQuery multi-region added:
1. Dataflow serverless every 5 min → BigQuery multi-region (US/EU/APAC replicas)
2. Grafana reads nearest BigQuery replica (reserved slots per region) → ops team
3. Tableau reads on-demand → C-suite global view

Key insight: "BigQuery multi-region means writes replicate globally automatically.
Reads always served from nearest region. One dataset, global availability,
no replication logic to manage. The cost premium is justified for PB global dashboards."
```

---

### Scenario 32 — PB + Near Real-time + Another System

#### Problem Statement
```
Size:    5 PB/day, ~5T events/day
Speed:   Near Real-time — downstream systems react within 5-15 minutes globally
Use:     Another System — global loyalty, partner sync, regulatory reporting every 15 min
Who:     Food delivery global — billions of events, thousands of downstream integrations
```

#### What's Different vs Scenario 20 (TB + Near RT + Another System)
```
Scenario 20: Cassandra staging table, consumer services poll every 5 min — works at TB
Scenario 32: PB global — Cassandra staging per region, global consistency needed for some

Architecture change:
  Cassandra staging → Bigtable (regional staging, each region processes its own events)
  Spark on EMR → Beam on Dataflow (serverless, global)
  Regional consumer services poll regional Bigtable staging
  Cross-region events: routed to owning region's staging table (by order_id hash)
```

#### Full Architecture
```
[GCS per-second partitions — global]
       │
[Beam on Dataflow — every 5 min, multi-region workers]
routes events to owning region by order_id hash
       │
WRITE to regional Bigtable staging:
  row key: (service_name, time_bucket, order_id)
  US-EAST Bigtable: US orders
  EU-WEST Bigtable: EU orders
  APAC Bigtable:    APAC orders

Regional consumer services POLL their regional Bigtable:
  [Loyalty Fleet US]    polls US-EAST Bigtable every 5 min
  [Loyalty Fleet EU]    polls EU-WEST Bigtable every 5 min
  [Loyalty Fleet APAC]  polls APAC Bigtable every 5 min
  → adds points, marks rows processed

[Regulatory reporting — global aggregation]
  Beam aggregates globally → single regulatory report table in BigQuery
  Regulator API pull or scheduled delivery
```

#### Key Scale Challenges
```
Bigtable row key design for staging:
  Row key: reverse(time_bucket) + "#" + service + "#" + order_id
  Reverse timestamp: avoids hotspotting on latest timestamp (Bigtable hotspot risk)
  Scan: SCAN from reverse(current_bucket) to reverse(current_bucket - 5 min)
  → reads exactly the 5-min window for this service

Regional data sovereignty:
  EU orders: stored only in EU Bigtable, processed only by EU Loyalty Fleet
  Legal requirement: GDPR — EU personal data cannot leave EU region
  Beam routing: hash order_id, if user is EU-registered → route to EU-WEST Bigtable
  This is not just optimization — it is a legal requirement at PB global scale
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| GCS (per-second, global) | Raw event storage | Pub/Sub WRITEs |
| Beam on Dataflow (global) | Routes events to regional Bigtable staging | READs GCS, WRITEs regional Bigtable |
| Bigtable (per region) | Regional staging (data sovereignty compliant) | Beam WRITEs, Regional Consumer Fleets READ |
| Regional Consumer Fleets | Poll regional Bigtable, process, mark done | READ + UPDATE regional Bigtable |

#### Interview Answer
```
Same polling pattern — regional Bigtable replaces Cassandra, data sovereignty is explicit:
1. Dataflow routes events to regional Bigtable by order_id region hash
2. Regional Consumer Fleets poll their region's Bigtable every 5 min
3. EU fleet only touches EU Bigtable — GDPR compliant by architecture

Key insight: "At PB global scale, data sovereignty is not an afterthought.
Route events to regional storage at ingestion time (Dataflow routing step).
Regional consumers only read regional storage. Compliance enforced architecturally,
not by policy — policy can be violated, architecture cannot."
```

---

### Scenario 33 — PB + Batch + Analytics

#### Problem Statement
```
Size:    5 PB/day, ~5T events/day
Speed:   Batch — nightly, global data ready by 6am in all regions
Use:     Analytics — global business reporting, regional P&L, annual planning data
Who:     Food delivery global — CFO global view, regional VPs, annual planning teams
```

#### What's Different vs Scenario 21 (TB + Batch + Analytics)
```
Scenario 21: Spark on EMR (50+ workers) processes 5 TB nightly — done in 1 hour
Scenario 33: 5 PB nightly → even 500-worker EMR cluster takes 10+ hours

Architecture change:
  Spark on EMR → Spark on Dataproc (auto-scaling, 1000+ workers) or Beam on Dataflow
  Snowflake XL / BigQuery → BigQuery (serverless PB, no compute sizing)
  dbt: incremental models critical (full refresh of PB history = $$$ and slow)
  Data tiering: hot data (last 90 days) in BigQuery, cold data (older) in GCS Coldline
```

#### Full Architecture
```
[GCS — 5 PB/day, per-second partitioned, date-organized]

[2am: Airflow — global pipeline]

Step 1: [Beam on Dataflow — serverless]
  reads yesterday's PB slice (not full history)
  joins dimension tables (user, restaurant, driver — snapshot in GCS)
  cleans, deduplicates
  writes → BigQuery (date-partitioned, clustered by region + city)
  Runtime: auto-scales, typically 45-60 min for 5 PB

Step 2: [dbt — incremental models, 32 threads]
  processes only yesterday's partition (WHERE date = yesterday)
  appends to: revenue_by_region_daily, restaurant_performance, user_cohort
  NEVER rescans full history (PB history rescan = $25,000/night)
  Runtime: 30-45 min

Step 3: [Data tiering — Airflow]
  moves data older than 90 days to BigQuery long-term storage (70% cheaper)
  moves data older than 1 year to GCS Coldline (90% cheaper)
  analysts can still query Coldline via BigQuery external tables (slower, cheaper)

Step 4: alert "Global data ready for [date]"

[BigQuery] ──READ──→ [Tableau / Looker]
                      regional views, global rollup
                      column-level security per role
```

#### Key Scale Challenges
```
Data tiering at PB — cost critical:
  Active BigQuery storage: $20/TB/month
  Long-term storage (data > 90 days, not modified): $10/TB/month (auto-applies)
  GCS Coldline (data > 1 year): $0.004/GB/month = $4/TB/month
  PB/year of history: $20/TB × 365 PB = $7.3M/year in active storage → tier aggressively

dbt at PB — incremental strategy:
  Model: revenue_by_region_daily
  Incremental: INSERT INTO revenue_by_region_daily SELECT ... WHERE date = '{{ds}}'
  Unique key: (region, date) — prevents duplicate rows on re-run
  Test weekly: run full refresh on sample → compare to incremental → must match

Dimension table joins at PB:
  User table: 1B users — joining at PB scale requires broadcast hash join or pre-built snapshot
  Solution: snapshot dimension tables to GCS daily → Dataflow broadcasts small snapshot
            to all workers → no shuffle join (most expensive operation at PB)
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| GCS (PB, per-second + date partitions) | Global raw storage with tiering | Pub/Sub WRITEs, Dataflow READs |
| Beam on Dataflow (serverless) | PB nightly ETL, parallel global | READs GCS, WRITEs BigQuery |
| BigQuery (global, partitioned + clustered) | PB data warehouse + data tiering | Dataflow WRITEs, dbt + Tableau READ |
| dbt (32 threads, incremental) | Partition-aware business logic | READs BigQuery raw, WRITEs clean |
| GCS Coldline | Long-term cold storage (1+ year) | Airflow tiering job moves data |

#### Interview Answer
```
Same nightly ETL pattern — Dataflow + BigQuery handle PB serverlessly:
1. Dataflow serverless: reads yesterday's 5 PB → joins dimension snapshots → BigQuery
2. dbt incremental (32 threads): yesterday's partition only → appends to summary tables
3. Airflow data tiering: moves >90-day data to long-term, >1-year to GCS Coldline
4. BigQuery column/row security: regional VPs see their region, CFO sees global

Key insight: "Data tiering at PB is not optional — it is budgetary survival.
5 PB/day × 365 days = 1.8 EB/year. Without tiering to Coldline: $7B+/year in storage.
With tiering: $400M/year. BigQuery external tables let analysts still query cold data."
```

---

### Scenario 34 — PB + Batch + ML

#### Problem Statement
```
Size:    5 PB/day → years of history = EB of training data
Speed:   Batch — monthly model training on massive historical dataset
Use:     ML Training — retrain global delivery model on years of PB/day data
Who:     Food delivery global ML team — training the core prediction model
```

#### What's Different vs Scenario 22 (TB + Batch + ML)
```
Scenario 22: Spark on EMR (100+ workers), 900 TB training data, SageMaker training
Scenario 34: Years × 5 PB/day = EB of potential training data — cannot use all of it

What changes at PB ML training:
  Data sampling becomes critical: training on EB is wasteful and slow
  GPU cluster: hundreds of GPU instances for distributed training
  Feature store: training features must be materialized to GCS (not queried live)
  Model registry: Vertex AI Model Registry (Google-native, global deployment)
```

#### Full Architecture
```
[GCS — years of history, 5 PB/day, EB total]

[Airflow — monthly trigger]
      │
Step 1: [Data sampling + feature materialization — Dataproc]
  Sample: 10% of last 6 months = 0.1 × 6 × 5 PB = 3 PB training sample
  Feature engineering: join sampled orders with features → training matrix
  Write: training_data.parquet → GCS (3 PB compressed to ~600 GB with column selection)
  Runtime: 4-6 hours on 500-worker Dataproc cluster

Step 2: [Vertex AI Training Job — distributed GPU cluster]
  200 A100 GPU instances (data parallelism)
  streams training_data from GCS via Vertex AI managed datasets
  gradient averaging after each batch (all-reduce via NCCL)
  evaluates: RMSE per region (global model + regional fine-tuning)
  WRITEs: model artifact → Vertex AI Model Registry
  Runtime: 12-24 hours

Step 3: [Regional A/B test rollout]
  deploy to APAC (5% traffic) → validate 48 hrs → EU → US → global
  shadow mode: new model scores all requests, old model serves
  compare: new RMSE vs old RMSE per region
  rollback: single command in Vertex AI if any region degrades
```

#### Key Scale Challenges
```
Why not train on all EB of history:
  Model accuracy improvement with data: logarithmic (diminishing returns after 6 months)
  Training cost: 200 GPUs × 24 hrs × $3/hr = $14,400 per training run
  Training on 3 PB sample vs full EB: similar accuracy, 100x less compute cost
  Rule: sample strategically — oversample rare events (fraud, long delivery times)

Regional model fine-tuning:
  One global model: trained on all regions → biased toward high-volume regions
  Better: global base model → fine-tune per region (transfer learning)
  APAC model: start from global weights → fine-tune on APAC data only (2-4 hours)
  Result: regional models outperform global model by 15-20% RMSE

Vertex AI vs SageMaker at PB:
  Vertex AI: native GCP, seamless GCS integration, Dataproc compatibility
  SageMaker: native AWS, seamless S3 integration, EMR compatibility
  Choose: whichever matches your cloud (do not cross-cloud for training data access)
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| GCS (EB, years of history) | Training data source with tiering | Historical storage |
| Dataproc (500+ workers) | Data sampling + feature engineering | READs GCS sample, WRITEs training matrix |
| Vertex AI Training (200 GPUs) | Distributed model training | READs training matrix from GCS |
| Vertex AI Model Registry | Global versioned model storage + deployment | Training WRITEs, regional inference READs |

#### Interview Answer
```
Same pipeline as TB — sampling and GPU scale are the key additions:
1. Dataproc: sample 10% of 6 months = 3 PB → feature engineering → 600 GB training matrix
2. Vertex AI: 200 GPUs, distributed training (data parallelism) → global + regional models
3. Regional A/B test: APAC first → EU → US → global (48-hour validation per region)

Key insight: "Train on a smart sample, not all EB. Oversample rare events.
Fine-tune regional models from global base — 4 hours of fine-tuning per region
beats training 5 regional models from scratch (5 × 24 hours = 120 GPU-hours vs 4)."
```

---

### Scenario 35 — PB + Batch + Dashboards

#### Problem Statement
```
Size:    5 PB/day, ~5T events/day
Speed:   Batch — daily, global executive dashboards updated once per day
Use:     Dashboards — global CEO/CFO KPI dashboard, board-level reporting
Who:     Food delivery global C-suite — daily briefing, board presentations
```

#### What's Different vs Scenario 23 (TB + Batch + Dashboards)
```
Scenario 23: dbt incremental scans 5 TB/day → builds summary row → BigQuery — works at TB
Scenario 35: Same pattern — BigQuery serverless and incremental dbt work identically at PB
             Key difference: governance, access control, and cost management at global scale

Architecture: identical to S23 — Dataflow + BigQuery + dbt incremental
What changes:
  Column-level + row-level security enforced globally (board sees different view than VPs)
  Regional summary tables: one row per region per day + one global rollup row
  Cost governance: strict BigQuery slot reservations, query cost alerts
```

#### Full Architecture
```
[Same pipeline as S33 — Dataflow + BigQuery nightly]

[dbt — incremental, adds one row to each summary table per day]

Summary tables produced:
  global_kpi_daily:    1 row/day — global revenue, orders, avg delivery
  regional_kpi_daily:  1 row/region/day — per-region KPIs
  Both tables: < 10,000 rows total (3 years × 365 days)

[BigQuery — global dataset with column/row security]
  Board view:        global_kpi_daily — revenue, growth%, key metrics
  Regional VP view:  regional_kpi_daily WHERE region = their_region (row-level policy)
  CFO view:          all regions + financial columns (cost, margin)

[Dashboard Tool — Looker embedded / Tableau Server]
  Auto-refreshes at 6am global (after all regional pipelines confirm complete)
  CEO dashboard: 6 tiles, loads in <1 sec (reads 365 rows)
  Board deck: Looker → auto-generates weekly PDF from same data
```

#### Key Scale Challenges
```
BigQuery row-level access policies at PB:
  Row access policy: regional_kpi WHERE region IN (user's assigned regions)
  Implementation: BigQuery row access policies → no separate view per region needed
  Auditing: BigQuery Data Catalog logs every query with user identity → compliance

Cost governance:
  At PB: one poorly written analyst query can scan EB and cost $50,000
  Fix: BigQuery slot reservation for dashboards (predictable cost)
        Query cost alerts: if query > $100 → require approval
        Scheduled queries for known reports (pre-approved cost)
        Analyst sandbox: separate project with monthly budget cap

Auto-generated board deck:
  Looker API: run dashboard → export to PDF → email to board distribution list
  Scheduled: every Monday 7am → board deck ready before 9am leadership call
  Data is fresh (from Friday's batch), formatted automatically, no manual slide prep
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| Dataflow + BigQuery (from S33) | Nightly ETL pipeline | Writes raw + clean tables |
| dbt (incremental, daily append) | Builds global + regional summary rows | READs BigQuery raw, WRITEs summary tables |
| BigQuery (row/column security) | Global governed warehouse | dbt WRITEs, Looker + Tableau READ |
| Looker / Tableau Server | Executive dashboard + auto PDF | READs BigQuery summary tables |

#### Interview Answer
```
Same summary table pattern as GB S11 / TB S23 — governance is the PB addition:
1. dbt incremental: appends 1 global row + N regional rows to summary tables daily
2. BigQuery row access policies: each VP sees only their region
3. Looker: CEO dashboard (6 tiles, <1 sec) + auto-generated weekly board PDF

Key insight: "The summary table is identical across GB, TB, PB.
What scales is governance. At PB, one ungoverned analyst query can cost $50K.
BigQuery slot reservations + row access policies + query cost alerts are non-negotiable."
```

---

### Scenario 36 — PB + Batch + Another System

#### Problem Statement
```
Size:    5 PB/day, ~5T events/day
Speed:   Batch — nightly file delivery, global partners and regulators
Use:     Another System — daily file exports to thousands of restaurant partners globally,
         multi-country regulatory filings
Who:     Food delivery global — 50,000+ restaurant partners, 50+ country tax authorities
```

#### What's Different vs Scenario 24 (TB + Batch + Another System)
```
Scenario 24: Spark on EMR, 1000 partners, Cassandra ack log — works at TB
Scenario 36: 5 PB/day, 50,000+ partners → parallel file generation at global scale

What breaks at PB:
  Single EMR cluster: 50,000 partners × 5 PB/day = cannot generate all files in one night
  Cassandra ack log: 50,000 partners × 10K chunks each = 500M ack writes per night

Architecture change:
  Spark on EMR → Spark on Dataproc (per-region, parallel partner batches)
  Cassandra ack log → Bigtable (500M writes/night, globally distributed)
  SFTP → regional delivery fleets + cloud storage handoff (partners read from their S3/GCS bucket)
  Data residency: EU partner files generated in EU, never leave EU (GDPR)
```

#### Full Architecture
```
[GCS — 5 PB/day, regionally partitioned, per-second precision]

[Midnight: Airflow — global orchestrator, per-region sub-DAGs]

PER REGION (parallel, all regions run simultaneously):

  [Regional Dataproc — 500 workers per region]
  reads regional slice of GCS (data residency: EU data stays in EU)
  filters per partner assigned to this region
  generates chunks: partner_A_eu/chunk-0001.parquet ... chunk-10000.parquet
  writes to regional GCS export staging
  Runtime per region: ~2 hours (500 workers, regional data slice)

  [Validation fleet — parallel per partner]
  row_count + checksum + schema per chunk
  aborts if any chunk fails → alerts, does not deliver partial data

  [Delivery fleet — regional, parallel]
  50,000 partners × 10K chunks = 500M parallel uploads per region
  Each partner: files dropped to their pre-agreed GCS bucket / S3 bucket
  Enterprise partners: SFTP (legacy systems still common)
  Regulators: signed Parquet or XML to official regulatory API endpoint

  [Bigtable — regional ack log]
  row key: reverse(timestamp)#partner_id#chunk_id
  500M ack writes per night → trivial for Bigtable (designed for millions of ops/sec)

[Global Airflow monitor]
  all regional DAGs complete by 3:30am?
  all partners acked by 4am?
  any regulatory filing missed → immediate escalation (legal risk)
```

#### Key Scale Challenges
```
Data residency as architecture driver at PB global:
  EU partner files: generated by EU Dataproc, stored in EU GCS, delivered from EU
  US partner files: generated by US Dataproc, stored in US GCS, delivered from US
  Zero cross-region data movement — legal requirement enforced architecturally
  Airflow: per-region DAGs run independently, global DAG only monitors completion signals

Bigtable for 500M nightly ack writes:
  Row key: reverse(timestamp)#partner_id#chunk_id — reversed timestamp avoids hotspot
  500M writes in 4 hours = 35K writes/sec — Bigtable handles millions/sec
  Monitoring query: SCAN by partner_id#date → all chunks for one partner → fast
  Retention: 7 days (regulators may ask for delivery proof up to 7 days later)

Partner handoff model — SFTP vs cloud bucket:
  Legacy partners: SFTP (slow, sequential, error-prone) → 10K chunks via SFTP = hours
  Modern partners: GCS/S3 cross-account bucket handoff (instant, parallel, reliable)
  Migration: offer partners cloud bucket onboarding → reduces SFTP fleet maintenance
  At PB: SFTP delivery fleet is a significant operational cost — actively migrate partners

Regulatory filings at global scale:
  50+ countries × different formats × different deadlines
  Some countries: real-time tax filing (not batch) — separate pipeline entirely
  Batch countries: generate country-specific XML, sign with country-specific certificate
  Delivery: often via government API (not SFTP) — need per-country API client
  Audit trail: every filing logged in Bigtable + Spanner (globally consistent, legally required)
```

#### Tool Summary

| Tool | Role | READ/WRITE |
|---|---|---|
| GCS (PB, per-region, per-second) | Source data, residency-compliant | Pub/Sub WRITEs |
| Regional Dataproc (500 workers) | Per-region partner file generation | READs regional GCS, WRITEs export staging |
| Regional GCS export staging | Partner file chunks (per-region) | Dataproc WRITEs, delivery fleet READs |
| Regional delivery fleet | 500M parallel uploads per region | READs GCS staging, WRITEs partner endpoints |
| Bigtable (regional ack log) | 500M concurrent ack writes | Delivery fleet WRITEs, Airflow monitors |
| Airflow (per-region + global) | Orchestration + SLA + escalation | Triggers all jobs, monitors acks |

#### Interview Answer
```
Same parallel file export pattern as TB — regional architecture enforces data residency:
1. Per-region Dataproc (500 workers): generates partner files from regional GCS slice
2. 500M parallel chunk uploads per region (cloud bucket handoff preferred over SFTP)
3. Bigtable ack log: 35K writes/sec, reverse-timestamp row key (no hotspot)
4. Global Airflow: monitors all regional DAGs complete by 4am

Key insight: "At PB global, data residency law IS the architecture.
Regional-first file generation is not a performance optimization — it is a legal requirement.
Enforce it architecturally (data never leaves region) not by policy (policy can be violated).
SFTP is the operational debt of PB batch delivery — every partner migrated to cloud
bucket handoff saves hours of SFTP fleet maintenance monthly."
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

