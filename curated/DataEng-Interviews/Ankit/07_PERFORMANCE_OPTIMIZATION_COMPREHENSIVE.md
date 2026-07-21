# Databricks & Spark — Comprehensive Performance Optimization Guide

> A complete reference covering every performance optimization technique for Databricks, Apache Spark and Delta Lake workloads. Organized from data layout (storage layer) → query execution → cluster configuration → diagnostics.

---

## Documentation Link: https://www.databricks.com/discover/pages/optimize-data-workloads-guide#instance-workloads

## Table of Contents

1. [Delta Lake Data Layout](#1-delta-lake-data-layout)
   - 1.1 Small File Problem
   - 1.2 OPTIMIZE & Z-Ordering
   - 1.3 Auto Optimize (Optimize Write + Auto Compact)
   - 1.4 Liquid Clustering
   - 1.5 Partitioning
   - 1.6 File Size Tuning
2. [Data Skipping & Pruning](#2-data-skipping--pruning)
   - 2.1 Delta Data Skipping (min/max stats)
   - 2.2 Column Pruning
   - 2.3 Predicate Pushdown
   - 2.4 Partition Pruning
   - 2.5 Dynamic Partition Pruning (DPP)
   - 2.6 Dynamic File Pruning (DFP)
3. [Data Shuffling](#3-data-shuffling)
   - 3.1 Why Shuffling Happens
   - 3.2 Broadcast Hash Join
   - 3.3 Shuffle Hash Join vs Sort-Merge Join
   - 3.4 Cost-Based Optimizer (CBO)
4. [Data Spilling](#4-data-spilling)
   - 4.1 Why Spilling Happens
   - 4.2 AQE Auto-Tuning (AOS)
   - 4.3 Manual Shuffle Partition Tuning
5. [Data Skew](#5-data-skew)
   - 5.1 Identifying Skew
   - 5.2 Skew Remediation (4 techniques)
6. [Data Explosion](#6-data-explosion)
   - 6.1 Explode Function
   - 6.2 Join Row Explosion
7. [Caching](#7-caching)
   - 7.1 Delta Cache
   - 7.2 Spark Cache
   - 7.3 Intermediate Results — Temp Views vs Delta Tables
8. [AQE — Adaptive Query Execution](#8-aqe--adaptive-query-execution)
9. [Delta MERGE Optimization](#9-delta-merge-optimization)
10. [Delta Maintenance — OPTIMIZE, VACUUM, ANALYZE](#10-delta-maintenance)
11. [Cluster Configuration & Tuning](#11-cluster-configuration--tuning)
12. [Diagnostics — How to Find the Problem](#12-diagnostics--how-to-find-the-problem)
    - 12.1 Serverless / Trial Workspace
    - 12.2 All-Purpose / Job Clusters (Full Spark UI)
13. [Quick Reference — Symptom → Cause → Fix](#13-quick-reference--symptom--cause--fix)
14. [Master Checklist](#14-master-checklist)

---

## 1. Delta Lake Data Layout

### 1.1 Small File Problem

Underneath every Delta table are Parquet files. When a table accumulates too many small files (e.g., from frequent streaming micro-batches or many small inserts), read latency suffers — Spark spends time opening/closing thousands of files rather than reading data.

**Target file size: 128MB (Auto Compact) to 1GB (OPTIMIZE)**

```sql
-- Check current file layout
DESCRIBE DETAIL catalog.schema.orders;
-- numFiles: number of files
-- sizeInBytes: total size
-- avg file size = sizeInBytes / numFiles
```

If average file size < 128MB → small file problem → run OPTIMIZE.

---

### 1.2 OPTIMIZE & Z-Ordering

**OPTIMIZE** compacts small Parquet files into larger ones (~1GB target by default).

**Z-Ordering** physically co-locates related rows in the same files by sorting data along multiple columns simultaneously using a space-filling curve (Hilbert/Z-curve). This narrows the min/max statistics per file, enabling aggressive data skipping.

```sql
-- Basic compaction only
OPTIMIZE catalog.schema.orders;

-- Compaction + Z-Order (sort by filter columns)
OPTIMIZE catalog.schema.orders ZORDER BY (customer_id, order_date);
```

```python
# PySpark equivalent
from delta.tables import DeltaTable
dt = DeltaTable.forName(spark, "catalog.schema.orders")
dt.optimize().executeZOrderBy("customer_id", "order_date")
```

**What happens on storage when OPTIMIZE runs:**

| Layer | Before | After |
|-------|--------|-------|
| Parquet files | 1000 small files (5MB each) scattered randomly | 5 large files (~1GB each) with similar rows co-located |
| `_delta_log` | — | New commit JSON: old files marked `RemoveFile`, new files added via `AddFile` with updated min/max stats |
| Physical disk | Old small files remain | New large files added; old files stay until VACUUM cleans them |

**Effect on query with filter:**
```
WHERE customer_id = 101

Before Z-Order: scans 1000 files (customer 101 rows scattered everywhere)
After Z-Order:  scans 3 files (customer 101 rows clustered → narrow min/max → data skipping)
```

**Key rules for Z-Ordering:**
- Always choose **high cardinality** columns (e.g., `customer_id`, `order_id`) — not date/month (those are better as partition columns)
- **Maximum 4 columns** — more columns degrades Z-Order effectiveness
- Run on a **separate job cluster**, not as part of your main job (it's compute-intensive)
- Run regularly — daily or weekly depending on write frequency
- Use **compute-optimized** instance family for OPTIMIZE jobs

---

### 1.3 Auto Optimize (Optimize Write + Auto Compact)

Auto Optimize prevents the small file problem from accumulating in the first place, inline during writes.

**Two components:**

#### Optimize Write
Dynamically optimizes Spark partition sizes based on actual data and writes ~128MB files per table partition. Runs **inside the same Spark write job** — adds a shuffle but produces right-sized files from the start.

#### Auto Compact
After the write job completes, launches a **separate small job** to further compact files that are still below 128MB target.

```sql
-- Enable on a table
ALTER TABLE catalog.schema.orders SET TBLPROPERTIES (
    'delta.autoOptimize.optimizeWrite' = 'true',
    'delta.autoOptimize.autoCompact'   = 'true'
);
```

```python
# Or via Spark config (session level)
spark.conf.set("spark.databricks.delta.optimizeWrite.enabled", "true")
spark.conf.set("spark.databricks.delta.autoCompact.enabled", "true")
```

**Comparison:**

| Feature | Optimize Write | Auto Compact | Manual OPTIMIZE |
|---------|---------------|--------------|-----------------|
| When runs | During write (same job) | After write job completes (new small job) | Manual / scheduled |
| Target file size | ~128MB | ~128MB | ~1GB (configurable) |
| Scope | Current write's files only | Current write's files only | Entire table (or partition) |
| Z-Ordering | ❌ No | ❌ No | ✅ Yes |
| Impact on SLA | Small added latency | Minimal (async) | Full job cost |

**When to use what:**
- Enable `optimizeWrite` if you're not already running manual OPTIMIZE
- Enable `autoCompact` if your job isn't strictly SLA-bound (it adds a post-job step)
- Still run periodic manual `OPTIMIZE ZORDER BY` for query performance — Auto Optimize only handles file size, not data layout

---

### 1.4 Liquid Clustering

Liquid Clustering is the modern replacement for both partitioning and Z-Ordering. It uses an adaptive, incremental clustering algorithm to continuously maintain good file layout without the limitations of rigid partition directories.

```sql
-- Create table with Liquid Clustering
CREATE TABLE catalog.schema.orders
CLUSTER BY (customer_id, order_date)
USING DELTA;

-- Change cluster keys anytime (no rewrite of existing data)
ALTER TABLE catalog.schema.orders CLUSTER BY (customer_id, region);

-- Run incremental clustering (only clusters new/changed data)
OPTIMIZE catalog.schema.orders;
```

**Z-Order vs Liquid Clustering:**

| Aspect | Z-Ordering | Liquid Clustering |
|--------|-----------|-------------------|
| Trigger | Manual: `OPTIMIZE ... ZORDER BY` | Incremental via `OPTIMIZE` |
| Rewrite scope | All touched files every run | New/changed files only |
| Partition requirement | Often combined with `PARTITIONED BY` | Replaces partitioning entirely |
| Change cluster keys | Full table rewrite needed | `ALTER TABLE` — no rewrite |
| Algorithm | Static space-filling curve sort | Adaptive curve-based, incremental |
| Best for | Smaller/static tables | Streaming, evolving access patterns, high-cardinality columns |
| Storage (both) | Parquet files + min/max stats in `_delta_log` | Same |

Both achieve data skipping via file-level min/max stats in `_delta_log` — Liquid Clustering just maintains that layout cheaply and continuously.

---

### 1.5 Partitioning

Partitioning creates physical subdirectories per partition value, enabling Spark to skip entire directories at scan time.

```sql
CREATE TABLE catalog.schema.orders (
    order_id BIGINT,
    customer_id INT,
    order_date DATE,
    amount DOUBLE
)
PARTITIONED BY (order_date)
USING DELTA;
```

**Rules:**
- **Only partition tables > 1TB** — smaller tables benefit more from ingestion-time clustering
- Choose **low cardinality** columns (date, region, status) — high cardinality (customer_id) causes file explosion
- Each partition should contain **at least 1GB** of data
- Partitioning = directory-level skipping; Z-Order/Liquid Clustering = file-level skipping — combine them:

```sql
-- Partition by date (low cardinality), Z-Order by customer_id (high cardinality)
OPTIMIZE catalog.schema.orders
WHERE order_date = '2024-01-01'
ZORDER BY (customer_id);
```

**Generated columns** — useful for deriving partition columns from existing ones:
```sql
CREATE TABLE catalog.schema.orders (
    order_id    BIGINT,
    order_ts    TIMESTAMP,
    order_date  DATE GENERATED ALWAYS AS (CAST(order_ts AS DATE))
)
PARTITIONED BY (order_date);
```

---

### 1.6 File Size Tuning

```sql
-- Set custom target file size (in bytes)
ALTER TABLE catalog.schema.orders
SET TBLPROPERTIES ('delta.targetFileSize' = '134217728');  -- 128MB

-- Let Databricks auto-tune based on workload patterns
ALTER TABLE catalog.schema.orders
SET TBLPROPERTIES ('delta.tuneFileSizesForRewrites' = 'true');
-- For merge-heavy tables, Databricks will auto-tune to smaller files (16-64MB)
-- to reduce how many files are rewritten per merge
```

**Recommended file sizes by use case:**

| Use Case | Recommended File Size |
|----------|----------------------|
| General read-heavy tables | 512MB – 1GB |
| Merge-heavy tables | 16MB – 64MB |
| Streaming output | 128MB (Auto Compact target) |
| Tables with frequent OPTIMIZE | 1GB (default) |

---

## 2. Data Skipping & Pruning

### 2.1 Delta Data Skipping (min/max stats)

Delta Lake automatically collects **min/max statistics** for the first 32 columns of each Parquet file when data is written. These stats are stored in `_delta_log` (`AddFile` actions). At query time, Spark checks these stats before opening any file — files whose min/max range cannot contain the filter value are skipped entirely.

```
WHERE customer_id = 101

File A: min=1,    max=500   → must scan
File B: min=501,  max=1000  → SKIP (101 not in range)
File C: min=50,   max=150   → must scan (range includes 101)
```

**This is why file layout matters** — if rows are randomly scattered, every file's range includes every value → no skipping.

```sql
-- Collect stats on more than the default 32 columns
ALTER TABLE catalog.schema.orders
SET TBLPROPERTIES ('delta.dataSkippingNumIndexedCols' = '50');

-- Move long string columns past the indexed range to avoid expensive stats collection
ALTER TABLE catalog.schema.orders
ALTER COLUMN long_description AFTER indexed_col_32;
```

---

### 2.2 Column Pruning

Only select the columns your query actually needs. Parquet is columnar — reading 5 of 100 columns is dramatically faster than `SELECT *`.

```python
# BAD
df = spark.table("catalog.schema.orders")  # reads all 100 columns

# GOOD
df = spark.table("catalog.schema.orders").select("customer_id", "order_date", "amount")
```

---

### 2.3 Predicate Pushdown

Push filters as close to the data source as possible. Spark automatically pushes filter predicates to the scan layer for Delta, Parquet, JDBC, Cassandra — data is filtered at read time, not after loading into memory.

```python
# Apply filters right after the read — Spark pushes these to the scan
df = spark.table("catalog.schema.orders") \
    .filter("order_date >= '2024-01-01'") \
    .filter("status = 'COMPLETED'") \
    .join(customers_df, "customer_id")

# NOT after transformations — by then data is already in memory
```

Works for: Delta, Parquet, Cassandra, JDBC.
Does NOT work for: JSON, XML, plain text sources.

---

### 2.4 Partition Pruning

When a query filters on the partition column, Spark reads only the matching subdirectories — entire partitions are skipped at the filesystem level.

```python
# Spark reads only the order_date=2024-01-15 directory
df = spark.table("catalog.schema.orders") \
    .filter("order_date = '2024-01-15'")
```

Apply partition filters **before joins** — always right after the read statement.

---

### 2.5 Dynamic Partition Pruning (DPP)

Available in Spark 3.0+, enabled by default. DPP handles cases where the partition filter value isn't known at parse time — for example, in star-schema joins where a dimension table filter determines which fact table partitions to read.

```sql
-- Spark automatically prunes fact table partitions based on the dimension filter
SELECT f.*, d.category
FROM   fact_orders f
JOIN   dim_products d ON f.product_id = d.product_id
WHERE  d.category = 'Electronics';
-- Without DPP: scan all fact_orders partitions
-- With DPP:    only scan fact_orders partitions that have Electronics products
```

No configuration needed — automatic on Spark 3.0+.

---

### 2.6 Dynamic File Pruning (DFP)

Databricks-specific extension of DPP — works at the file level instead of partition level. Automatically enabled in Databricks Runtime 6.1+. No configuration needed.

---

## 3. Data Shuffling

### 3.1 Why Shuffling Happens

A **shuffle** is the redistribution of data across the network between worker nodes. It's triggered by wide transformations:

```python
df.groupBy("customer_id").agg(...)   # shuffle to group same customer_id on same node
df.join(other_df, "customer_id")     # shuffle both sides to align join keys
df.distinct()                         # shuffle to find duplicates across nodes
df.repartition(100)                   # explicit shuffle
df.orderBy("amount")                  # global sort = shuffle
```

Shuffles are expensive because they involve: serialization → network transfer → deserialization → disk I/O. Minimize them wherever possible.

---

### 3.2 Broadcast Hash Join

The most effective way to eliminate a join shuffle — broadcast the smaller table to every worker node so no data movement is needed for the large table.

```python
from pyspark.sql.functions import broadcast

# Explicit broadcast hint (recommended — always faster than waiting for AQE)
result = orders_df.join(broadcast(small_lookup_df), "product_id")
```

```sql
-- SQL hint
SELECT /*+ BROADCAST(d) */ f.*, d.category
FROM   fact_orders f
JOIN   dim_products d ON f.product_id = d.product_id;
```

**Auto-broadcast threshold** (Spark broadcasts tables smaller than this automatically):
```python
spark.conf.set("spark.sql.autoBroadcastJoinThreshold", "10485760")  # 10MB default
# Increase if your driver has 32GB+ RAM — safe up to 200MB
spark.conf.set("spark.sql.autoBroadcastJoinThreshold", "209715200")  # 200MB
```

**Key rules:**
- Always **explicitly broadcast** small tables with hints — don't wait for AQE. AQE can convert to broadcast, but only after both sides have already shuffled — explicit hints skip the shuffle entirely
- **Never broadcast > 1GB** — it passes through the driver (OOM risk)
- Broadcast not supported for **full outer joins**; for right outer join, only the left side can be broadcast; for left joins, only the right side
- Disk size ≠ memory size — Parquet files decompress significantly in memory (sometimes 8-40x). A 100MB Parquet file might be 3GB in memory. Spark has a hard 8GB broadcast limit

---

### 3.3 Shuffle Hash Join vs Sort-Merge Join

When broadcast isn't possible, Spark defaults to **Sort-Merge Join (SMJ)** — the most expensive option (shuffles both sides + sorts both sides).

**Shuffle Hash Join (SHJ)** is faster in many cases — it shuffles both sides but builds a hash map instead of sorting:

```python
# Prefer SHJ over SMJ where joins are heavy
spark.conf.set("spark.sql.join.preferSortMergeJoin", "false")
```

Note: Photon engine (Databricks paid tier) automatically uses SHJ over SMJ.

**Join type selection logic:**
```
Small table (< broadcast threshold)  → Broadcast Hash Join  (no shuffle)
Medium table (fits in memory)        → Shuffle Hash Join     (shuffle, no sort)
Large table (won't fit in memory)    → Sort-Merge Join       (shuffle + sort)
```

---

### 3.4 Cost-Based Optimizer (CBO)

CBO uses table statistics to choose the best join strategy and join order. Enabled by default, but requires up-to-date stats to work.

```sql
-- Collect stats (run after table is loaded/updated)
ANALYZE TABLE catalog.schema.orders COMPUTE STATISTICS FOR ALL COLUMNS;

-- Enable join reorder (for queries with multiple inner/cross joins)
SET spark.sql.cbo.enabled = true;
SET spark.sql.cbo.joinReorder.enabled = true;
```

**Rules:**
- Run `ANALYZE TABLE` regularly — daily, or after >10% data mutation
- Run it as a **separate job**, not part of your main pipeline (adds to SLA)
- CBO + AQE together = best results — AQE uses the stats CBO collects to make runtime decisions

---

## 4. Data Spilling

### 4.1 Why Spilling Happens

Spark processes shuffle data in memory. If a task's data doesn't fit in the allocated executor memory, it **spills** to disk — serializing data, writing to disk, then reading it back. This is a major performance killer.

**Root cause**: Default `spark.sql.shuffle.partitions = 200` is rarely right. With 200 partitions, each task may receive GBs of data that don't fit in memory → spill.

**Identify spill (Spark UI — Stages tab, not available on Serverless):**
- `Spill (Memory)` and `Spill (Disk)` columns — any non-zero value is a problem
- Large spill = wrong number of shuffle partitions, or data skew

---

### 4.2 AQE Auto-Tuning (AOS)

`autoOptimizeShuffle` automatically finds the right number of shuffle partitions at runtime:

```python
spark.conf.set("spark.sql.adaptive.enabled", "true")  # AQE (on by default in Spark 3.x+)
spark.conf.set("spark.databricks.adaptive.autoOptimizeShuffle.enabled", "true")  # AOS
```

**Caveat — highly compressed tables:** If source data has an unusually high compression ratio (20-40x), AOS may miscalculate. Fix:

```python
# Reduce the per-partition size hint AOS uses (default 128MB)
spark.conf.set("spark.sql.adaptive.advisoryPartitionSizeInBytes", "16mb")
# If still wrong, try 8MB. If still failing, disable AOS and tune manually.
```

---

### 4.3 Manual Shuffle Partition Tuning

If AOS isn't working, calculate manually:

```
Right number of shuffle partitions = Total shuffle bytes / 128MB

Example:
Total shuffle data in stage = 25.6 GB
Target size per task = 128 MB
25,600 MB / 128 MB = 200 partitions  → set shuffle.partitions = 200
```

Find "total shuffle bytes" in the Spark UI Exchange node or Stages tab.

```python
spark.conf.set("spark.sql.shuffle.partitions", "200")
```

**Rules:**
- Target ~128MB per task (up to 200MB is fine)
- If no AOS and no manual tuning → set to **2-3x total worker CPU cores** as a baseline
- Fine-tune for the **largest shuffle stage** in your pipeline, then apply that value across the notebook
- If data is skewed, fixing shuffle partition count won't help — fix the skew first (see Section 5)

---

## 5. Data Skew

### 5.1 Identifying Skew

Data skew = a few tasks processing far more data than others due to uneven key distribution (e.g., one `customer_id` has 10M rows, all others have ~100).

**Symptoms:**
- Almost all tasks finish in seconds, but 1-2 tasks run for minutes/hours
- In Spark UI Stages tab (all-purpose cluster): huge gap between Median and Max in task duration distribution
- Large difference between Min and Max shuffle read size in task summary metrics
- Even after tuning shuffle partitions, you still see heavy disk spill

**Identify skewed keys:**
```python
# Count rows per join/aggregation key
df.groupBy("customer_id").count().orderBy("count", ascending=False).show(20)
# Large disparity between top and bottom values = skew confirmed
```

---

### 5.2 Skew Remediation

Try these in order — simpler first:

#### Option 1: Filter skewed values
If skew is caused by nulls or known bad values, filter them before the join:
```python
# Null values in join key → all nulls go to one partition
df_clean = df.filter(df.customer_id.isNotNull())
result = df_clean.join(other_df, "customer_id")
```

#### Option 2: Skew hints (Databricks)
Tell Spark explicitly which table/column/values are skewed:
```sql
SELECT /*+ SKEW('orders', 'customer_id', (101, 999)) */ *
FROM   orders
JOIN   customers ON orders.customer_id = customers.customer_id;
```

#### Option 3: AQE Skew Join Optimization (automatic)
AQE detects and splits skewed partitions automatically. On by default:
```python
spark.conf.set("spark.sql.adaptive.skewJoin.enabled", "true")
# Fine-tune detection thresholds:
spark.conf.set("spark.sql.adaptive.skewJoin.skewedPartitionFactor", "5")      # 5x avg size = skewed
spark.conf.set("spark.sql.adaptive.skewJoin.skewedPartitionThresholdInBytes", "256mb")
```

#### Option 4: Salting (last resort — requires code changes)
Add a random integer suffix to skewed key values to split them across partitions:
```python
import pyspark.sql.functions as f

# Salt the large table (skewed side)
salt_factor = 10
orders_salted = orders_df \
    .withColumn("salt", (f.rand() * salt_factor).cast("int")) \
    .withColumn("salted_key", f.concat(f.col("customer_id").cast("string"), f.lit("_"), f.col("salt")))

# Replicate the small table (lookup side) for all salt values
customers_replicated = customers_df \
    .withColumn("salt", f.explode(f.array([f.lit(i) for i in range(salt_factor)]))) \
    .withColumn("salted_key", f.concat(f.col("customer_id").cast("string"), f.lit("_"), f.col("salt")))

result = orders_salted.join(customers_replicated, "salted_key").drop("salt", "salted_key")
```

---

## 6. Data Explosion

### 6.1 Explode Function

`explode()` converts array/map columns to rows — can dramatically increase data volume.

```python
# If orders has an array of items, explode() multiplies rows
from pyspark.sql.functions import explode
items_df = orders_df.withColumn("item", explode("items"))
# 1M orders × avg 10 items = 10M rows → 10x data explosion
```

Visible in Spark UI as a **Generate** node with rows_out >> rows_in.

**Fix:** Reduce input partition size so each task handles less before exploding:
```python
spark.conf.set("spark.sql.files.maxPartitionBytes", "16777216")  # 16MB instead of 128MB
# OR explicitly repartition after read
df = spark.table("orders").repartition(500)
```

---

### 6.2 Join Row Explosion

A join can produce far more rows than either input if join keys are not unique:
```
orders (1M rows) JOIN order_items (10M rows) on order_id
→ if each order has 10 items: output = 10M rows (10x explosion)
→ if join key has duplicates on both sides: output = M × N (cartesian-like explosion)
```

Visible in Spark UI: **SortMergeJoin** or **ShuffleHashJoin** node with `rows_out >> rows_in`.

**Fixes:**
- Verify join key uniqueness before joining
- Apply filters on both sides before joining to reduce volume
- Increase shuffle partitions to distribute the exploded load across more tasks

---

## 7. Caching

### 7.1 Delta Cache

Delta Cache stores copies of remote Parquet files on worker node **local SSD disks** in an optimized format. Subsequent reads of the same data skip cloud storage entirely.

- Automatic — no code changes needed
- Available on storage-optimized and memory-optimized instance families
- Significantly faster than Spark cache (uses hardware decompression, outputs in optimal format for whole-stage code generation)

```python
# Explicitly enable on a cluster (if not auto-enabled)
spark.conf.set("spark.databricks.io.cache.enabled", "true")
```

---

### 7.2 Spark Cache

Spark cache stores DataFrame data in JVM memory (or memory+disk) across actions.

```python
df.cache()         # memory only (default)
df.persist()       # memory, spill to disk if needed
df.unpersist()     # release cache when done

# Or via SQL
spark.sql("CACHE TABLE catalog.schema.orders")
spark.sql("UNCACHE TABLE catalog.schema.orders")
```

**Use Spark cache only when:**
- The same DataFrame is used in **2 or more actions** in the same job
- Recomputing it is expensive (complex transformations, large joins)

**Prefer Delta cache over Spark cache** — Delta cache is faster, stored on SSD, and doesn't consume executor JVM heap memory (avoiding GC pressure).

**Spark cache modes:**

| Storage Level | Memory | Disk | Notes |
|--------------|--------|------|-------|
| `MEMORY_ONLY` | ✅ | ❌ | Default. Recomputed if evicted |
| `MEMORY_AND_DISK` | ✅ | ✅ | Spills to disk if memory full |
| `DISK_ONLY` | ❌ | ✅ | Slowest but doesn't use heap |

---

### 7.3 Intermediate Results — Temp Views vs Delta Tables

In multi-step pipelines, use **temp views** instead of materializing intermediate Delta tables when the intermediate result is used only once:

```python
# BAD — writes to storage, reads back from storage = unnecessary I/O
df_filtered = orders_df.filter("status = 'COMPLETED'")
df_filtered.write.saveAsTable("temp_filtered_orders")  # writes to cloud storage
df_joined = spark.table("temp_filtered_orders").join(customers_df, "customer_id")

# GOOD — lazy evaluation, no materialization
df_filtered = orders_df.filter("status = 'COMPLETED'")
df_filtered.createOrReplaceTempView("filtered_orders")
df_joined = spark.table("filtered_orders").join(customers_df, "customer_id")
```

**Rule:**
- Intermediate result used **once** → temp view (lazy, no materialization)
- Intermediate result used **2+ times** in the same job → keep as Delta table (or `cache()` it) to avoid recomputation

---

## 8. AQE — Adaptive Query Execution

AQE re-optimizes the Spark query plan **at runtime** using actual statistics from completed shuffle stages, correcting the static plan built before execution (which relies on estimates that are often wrong or stale).

```python
spark.conf.set("spark.sql.adaptive.enabled", "true")  # on by default in Spark 3.x+
```

**Three main AQE capabilities:**

### 8.1 Coalescing Shuffle Partitions

After a shuffle completes, AQE sees the actual partition sizes. If many partitions are tiny, it merges them:

```
Before AQE: 200 shuffle partitions, most are 2MB each → 200 tiny tasks
After AQE:  Coalesced into 8 partitions of ~50MB each → 8 efficient tasks
```

```python
spark.conf.set("spark.sql.adaptive.coalescePartitions.enabled", "true")
```

### 8.2 Dynamically Switching Join Strategy

AQE can convert a Sort-Merge Join to a Broadcast Hash Join at runtime if the actual size of a join side (post-filter) turns out smaller than the broadcast threshold:

```
Static plan: SortMergeJoin (planner estimated both tables as large)
At runtime:  Left side filtered to 5MB → AQE switches to BroadcastHashJoin
```

```python
# AQE broadcast threshold (separate from the static autoBroadcastJoinThreshold)
spark.conf.set("spark.sql.adaptive.autoBroadcastJoinThreshold", "31457280")  # 30MB default
```

**Note:** Explicit broadcast hints skip the shuffle entirely and are always faster — don't rely on AQE alone for known small tables.

### 8.3 Skew Join Optimization

AQE detects skewed partitions (based on size vs average) and splits them into smaller sub-partitions for parallel processing:

```
Before: Task for customer_id=VIP_USER → 10M rows → 45 min
After:  Skewed partition split into 10 sub-tasks → ~4-5 min each, parallelized
```

```python
spark.conf.set("spark.sql.adaptive.skewJoin.enabled", "true")  # on by default
```

---

## 9. Delta MERGE Optimization

### 9.1 How MERGE Works Internally

MERGE is a two-step operation:
1. **Inner join** between source and target on the `ON` clause → returns list of target files that contain matching rows (sent to driver)
2. **Full outer join** between source and the matched target files → consolidates inserts/updates/deletes

This means: **the fewer files matched in step 1, the less data rewritten in step 2**.

### 9.2 Why MERGE Can Be Slow

- Large target files (1GB) → high chance of finding at least one match per file → many files returned in step 1 → massive rewrite
- Vague ON clause without Z-Order/partition filters → Spark must check every file
- Z-Order gets disrupted after MERGE rewrites files

### 9.3 MERGE Optimizations

**Use smaller files for merge-heavy tables (16MB–64MB):**
```sql
ALTER TABLE catalog.schema.orders
SET TBLPROPERTIES ('delta.targetFileSize' = '33554432');  -- 32MB
-- Or let Databricks auto-tune:
SET TBLPROPERTIES ('delta.tuneFileSizesForRewrites' = 'true');
```

**Add partition filter to ON clause:**
```sql
MERGE INTO catalog.schema.orders AS target
USING source_updates AS source
ON target.customer_id = source.customer_id
   AND target.order_date >= '2024-01-01'   -- ← partition pruning reduces files checked in step 1
WHEN MATCHED THEN UPDATE SET *
WHEN NOT MATCHED THEN INSERT *;
```

**Add Z-Order column filter to ON clause:**
```sql
ON target.customer_id = source.customer_id
   AND target.order_date >= '2024-01-01'
   AND target.region = source.region        -- ← Z-Order file pruning
```

**Broadcast small source DataFrame:**
```python
from pyspark.sql.functions import broadcast
from delta.tables import DeltaTable

target = DeltaTable.forName(spark, "catalog.schema.orders")
target.alias("target").merge(
    broadcast(source_df).alias("source"),   # ← broadcast if source < 200MB
    "target.customer_id = source.customer_id"
).whenMatchedUpdateAll().whenNotMatchedInsertAll().execute()
```

**Low Shuffle Merge** (default on Databricks Runtime 10.4+):
- Only rewrites rows that actually changed; unchanged rows stay in their original files
- Maintains existing Z-Order layout for unmodified data

```python
# Enable on earlier runtimes:
spark.conf.set("spark.databricks.delta.merge.enableLowShuffle", "true")
```

---

## 10. Delta Maintenance

### 10.1 VACUUM — Remove Stale Files

VACUUM removes Parquet files that are no longer part of the current table state and are older than the retention threshold.

```sql
-- Default: removes files older than 7 days
VACUUM catalog.schema.orders;

-- Custom retention
VACUUM catalog.schema.orders RETAIN 168 HOURS;  -- 7 days

-- Dry run (shows what would be deleted)
VACUUM catalog.schema.orders DRY RUN;
```

**Key settings:**
```sql
ALTER TABLE catalog.schema.orders SET TBLPROPERTIES (
    'delta.deletedFileRetentionDuration' = 'interval 7 days',   -- data file retention
    'delta.logRetentionDuration'         = 'interval 30 days'   -- transaction log retention
);
```

**Rules:**
- Never set retention below **7 days** — concurrent readers/writers may still reference recent files
- Set both `deletedFileRetentionDuration` and `logRetentionDuration` to the same value for consistent time travel
- Run weekly on a **small dedicated cluster** (1-4 workers) — VACUUM is not compute-intensive (parallel file listing on workers, deletion on driver)
- Run as a **separate job**, not part of your main pipeline

### 10.2 Transaction Log Checkpoints

Delta automatically creates a Parquet checkpoint every **10 commits** (configurable). Reads don't replay all JSON log files from history — they read the latest checkpoint + a few subsequent JSON files. This keeps read performance stable even as the log grows.

```sql
-- Adjust checkpoint interval (default 10)
ALTER TABLE catalog.schema.orders
SET TBLPROPERTIES ('delta.checkpointInterval' = '10');
```

### 10.3 ANALYZE TABLE — Keep Stats Fresh

```sql
-- Compute column statistics for CBO and AQE
ANALYZE TABLE catalog.schema.orders COMPUTE STATISTICS FOR ALL COLUMNS;
```

Run daily or after >10% data mutation.

### 10.4 Predictive Optimization (Unity Catalog Managed Tables)

If enabled, Databricks automatically runs OPTIMIZE, VACUUM, and ANALYZE based on table usage patterns — no manual scheduling needed. Only applies to Unity Catalog **managed** tables.

```sql
ALTER TABLE catalog.schema.orders
SET TBLPROPERTIES ('delta.enablePredictiveOptimization' = 'true');
```

**Maintenance schedule summary:**

| Task | Frequency | Cluster |
|------|-----------|---------|
| OPTIMIZE (+ ZORDER) | Daily / Weekly | Compute-optimized job cluster |
| VACUUM | Daily / Weekly | General purpose, small (1-4 workers) |
| ANALYZE TABLE | Daily / after >10% mutation | General purpose job cluster |
| All three together | Nightly maintenance job | Single dedicated job cluster |

---

## 11. Cluster Configuration & Tuning

> Note: Most of this section applies to All-Purpose and Job Clusters (paid tier). On Serverless, compute is fully managed by Databricks.

### 11.1 All-Purpose vs Job Clusters

- **All-purpose clusters**: Interactive development/testing only — never use for production jobs
- **Job clusters (automated)**: Ephemeral, spun up per-job run — much cheaper DBU rate than all-purpose
- **Serverless**: Fully managed, instant startup, best for interactive notebooks and SQL

### 11.2 Instance Type Selection

#### AWS (EC2 Instance Families)

| Instance Family | AWS Type | When to Use |
|----------------|----------|-------------|
| **Memory Optimized** | `r` family (r5, r6i, r6g) | ML workloads, heavy shuffle/spill, Spark caching — high RAM per core |
| **Compute Optimized** | `c` family (c5, c6i, c6g) | Structured Streaming, ELT full scans, running OPTIMIZE/Z-Order — high CPU per dollar |
| **Storage Optimized** | `i` family (i3, i3en, i4i) | Delta cache (local NVMe SSD), ad hoc analysis, ML with repeated data reads |
| **General Purpose** | `m` family (m5, m6i, m6g) | VACUUM, light workloads, balanced CPU/RAM, no specific requirement |
| **GPU Optimized** | `p` family (p3, p4), `g` family (g4dn, g5) | Deep learning, GPU-accelerated ML workloads |

#### Azure (VM Families)

| Instance Family | Azure Type | When to Use |
|----------------|-----------|-------------|
| **Memory Optimized** | `E` series (E16ds_v4, E32ds_v5) | Heavy shuffle/spill, Spark caching, ML workloads |
| **Compute Optimized** | `F` series (Fsv2) | OPTIMIZE/Z-Order, Structured Streaming, full scan ELT |
| **Storage Optimized** | `L` series (L4s, L8s_v3) | Delta cache (local SSD), repeated reads, ad hoc analysis |
| **General Purpose** | `D` series (D4s_v3, D8s_v5) | Balanced workloads, VACUUM, light pipelines |
| **GPU Optimized** | `NC` series (NC6s_v3, NC24ads_A100) | Deep learning, GPU workloads |

---

#### Graviton (ARM-based) — Cheaper Option

AWS Graviton instances (`r6g`, `c6g`, `m6g`) are ARM-based processors that deliver **~20-40% better price-performance** than equivalent x86 instances for most Spark workloads.

| Graviton Type | Maps To | Saving vs x86 |
|--------------|---------|---------------|
| `r6g` (memory) | Replaces `r5` / `r6i` | ~20% cheaper, same or better performance |
| `c6g` (compute) | Replaces `c5` / `c6i` | ~20% cheaper, better throughput for CPU-bound tasks |
| `m6g` (general) | Replaces `m5` / `m6i` | ~20% cheaper for balanced workloads |

**When to use Graviton:**
- Databricks Runtime 9.1 LTS+ supports Graviton natively
- Best for: OPTIMIZE jobs, ELT pipelines, aggregation-heavy workloads
- Photon is also supported on Graviton — combine both for maximum savings
- Not recommended for workloads using custom JAR dependencies compiled for x86 (compatibility risk)

**Rule of thumb:** Default to Graviton (`r6g`, `c6g`) for new job clusters — same performance, lower bill.

### 11.3 Autoscaling

- Use for **interactive/development clusters** with min workers = 1
- For **production job clusters**: set min workers to the minimum you actually need (not 1) to save scale-up time
- **Do NOT use autoscaling for Spark Structured Streaming** — hard to know when to scale down. Use Delta Live Tables Enhanced Autoscaling for streaming instead
- For jobs with **fine-tuned shuffle partitions**: autoscaling may not be needed — you know exactly how much compute is required

### 11.4 Number of Workers

- Never use 1 worker for production (single point of failure)
- Small workloads (no wide transforms): start with 2-4 workers
- Medium/large workloads (joins, aggregations): start with 8-10 workers, scale up if needed
- After shuffle partition fine-tuning: adding more workers scales linearly (same cost, proportionally faster)

### 11.5 Cluster Utilization — What to Watch

**Ganglia UI (DBR ≤ 12.2)** / **New Cluster Metrics UI (DBR ≥ 13)**:
- Average cluster load should be **> 80%** during query execution
- All worker squares should be **red** (fully engaged) — not idle
- Memory utilization should be **> 60-70%**

**Avoid GC pauses** (symptoms: GC Time > 20% of task time in Stages tab):
- Don't call `collect()` or `toPandas()` — brings all data to driver
- Prefer Delta cache over Spark cache (doesn't use JVM heap)
- Avoid workers with > 128GB RAM — large heap = long GC pauses
- If GC still bad, switch to G1GC:
```
spark.executor.extraJavaOptions = -XX:+UseG1GC
```

### 11.6 Photon Engine

Databricks' vectorized query engine — significant performance improvements:
- Replaces Sort-Merge Join with Shuffle Hash Join automatically
- Executor-side broadcast (no driver bottleneck for large broadcasts)
- Faster Delta MERGE, UPDATE, DELETE
- Better scan performance for tables with many columns / small files

Enable when available: check "Use Photon Acceleration" in cluster config.

### 11.7 Other Best Practices

- Always use the **latest LTS Databricks Runtime** — each version brings performance improvements
- **Restart all-purpose clusters** weekly — zombie Spark jobs can accumulate over time
- Use **instance pools** for workloads with tight SLAs — pools maintain pre-warmed instances to eliminate cold start time
- Use **spot instances** for development/ad hoc clusters (save cost), but never for the driver node and not for SLA-bound production jobs
- Set **auto-termination** to 10-15 minutes for all-purpose clusters (default is 120 min — wastes money)
- For shuffle-heavy workloads: **fewer, larger nodes** reduce network I/O (less data crosses the network vs many small nodes)
- For single-node libraries (Pandas, scikit-learn): use **single-node clusters** — regular clusters waste resources on unused workers

---

## 12. Diagnostics — How to Find the Problem

### 12.1 Serverless / Trial Workspace

On Serverless, you don't have the full Spark UI (no Jobs/Stages/Tasks tabs). Your diagnostic tools:

**What's available on Serverless:**

| Tool | Available | Use for |
|------|-----------|---------|
| Notebook cell execution time | ✅ | Find the slow cell/action |
| Query Profile (visual DAG) | ✅ | Operator-level bottleneck analysis |
| SQL Warehouse monitoring | ✅ | Queue time, spill, bytes read |
| `DESCRIBE DETAIL/HISTORY` | ✅ | Table layout, file sizes, maintenance history |
| Spark UI Jobs/Stages/Tasks | ❌ | Not available |
| Ganglia / Cluster Metrics | ❌ | Not available |

**Diagnostic flow for Serverless:**

```
Step 1: Notebook cell times → find the slow cell
Step 2: Query Profile → find red/orange operator nodes
Step 3: Scan node → files read vs pruned, bytes read (data skipping working?)
Step 4: Join node → BroadcastHashJoin or SortMergeJoin?
Step 5: Exchange nodes → count shuffles, partition size distribution
Step 6: SQL Warehouse monitoring → queue time vs execution time, bytes spilled
Step 7: DESCRIBE DETAIL → avg file size, numFiles
Step 8: DESCRIBE HISTORY → when was OPTIMIZE last run?
```

**Query Profile — what to look for in each node:**

| Node Type | What to Check |
|-----------|--------------|
| **Scan** | `files pruned` (higher = better), `bytes read` vs table size, `pushed filters` present |
| **Join** | Join type: BroadcastHashJoin (good) vs SortMergeJoin (expensive) |
| **Exchange** | Count of Exchange nodes (each = 1 shuffle), partition size uniformity |
| **Aggregate** | Rows in vs rows out ratio, duration |
| **Generate** | Rows in vs rows out — large explosion = `explode()` inflating data |

**Too many actions in a loop — avoid:**
```python
# BAD — N separate Spark jobs
for col in columns:
    print(df.filter(f.col(col) > 0).count())

# GOOD — one Spark job
df.select([f.count(f.when(f.col(c) > 0, c)).alias(c) for c in columns]).show()
```

---

### 12.2 All-Purpose / Job Clusters (Full Spark UI)

**Navigate to:** Cluster UI → Spark UI, or click "Spark UI" link on a running job.

#### Jobs Tab
- Each Spark action (write, count, show) = one Job
- High job count for one notebook cell → too many actions in code
- Click slowest Job → goes to its Stages

#### Stages Tab (most important)
For the slow stage:

| Metric | What to look for |
|--------|-----------------|
| **Task Duration (Min/25th/Median/75th/Max)** | Huge gap between Median and Max = data skew |
| **Shuffle Read/Write** | Large shuffle for a simple query = unnecessary wide transform |
| **Spill (Memory) / Spill (Disk)** | Any non-zero value = memory pressure, OOM risk |
| **GC Time** | > 20% of task duration = memory pressure (UDFs, caching, large objects) |
| **Task Locality** | PROCESS_LOCAL = good, ANY = data not co-located with compute |

#### SQL / DataFrame Plan Tab
- Visual DAG of operators — same as Query Profile on Serverless but with more detail
- Scan node: `files pruned`, `pushed filters`
- Join node: join type used
- Exchange node: partition count, data size per partition
- Any node: hover/click for `rows in`, `rows out`, `time`, `bytes`

#### Storage Tab
- Shows cached DataFrames/tables and their size in memory
- Use to verify `cache()` is working and to check actual in-memory size (compare to disk size for compression ratio)

---

## 13. Quick Reference — Symptom → Cause → Fix

| Symptom | Likely Cause | Diagnostic Location | Fix |
|---------|-------------|---------------------|-----|
| Query scans 100% of table despite filter | No data skipping — column not Z-Ordered/Clustered | Query Profile → Scan node (files pruned = 0) | `OPTIMIZE ZORDER BY (filter_col)` or Liquid Clustering |
| Slow join — SortMergeJoin seen | Table too large for broadcast / stale stats | Query Profile → Join node type | Add `broadcast()` hint; run `ANALYZE TABLE` |
| 1-2 tasks hang while all others finish | Data skew | Spark UI Stages tab → task duration distribution | AQE skew join, skew hints, salting |
| Non-zero Spill (Disk) in stages | Too few shuffle partitions / data skew | Spark UI Stages tab → Spill columns | Increase shuffle partitions; fix skew first |
| High GC Time (>20% of task time) | Memory pressure — UDFs, over-caching, large objects | Spark UI Stages tab → GC Time column | Remove `collect()`/`toPandas()`; prefer Delta cache; use G1GC |
| Bytes spilled to disk | Memory pressure at executor level | SQL Warehouse monitoring | Tune shuffle partitions; fix skew; use memory-optimized instances |
| Many Spark jobs for one notebook cell | Too many actions (`count`/`display` in loop) | Spark UI Jobs tab → job count | Combine actions; remove loop-based actions |
| Table read slow despite OPTIMIZE | Partition pruning not working | `DESCRIBE DETAIL` → partition column vs filter column | Ensure filter is on the partition column |
| Query fast alone, slow in pipeline | Too many actions triggering separate jobs | Count `display()`/`count()` calls | Consolidate actions; use temp views |
| Cell takes 3+ min for small data | Compute cold start / warehouse scaling | SQL Warehouse monitoring → queue time | Keep warehouse warm; use instance pools |
| Table has thousands of small files | No OPTIMIZE / frequent streaming writes | `DESCRIBE DETAIL` → numFiles | Run `OPTIMIZE`; enable Auto Compact |
| MERGE is very slow | Large target files; no partition/Z-Order filter in ON clause | Spark UI + `DESCRIBE DETAIL` | Smaller file sizes for table; add filters to ON clause; broadcast source |
| Rows explode after join | Non-unique join keys / cartesian join | Query Profile → Join node rows_in vs rows_out | Check key uniqueness; apply filters before join |
| Data explosion after `explode()` | Array column with high cardinality | Query Profile → Generate node | Reduce `maxPartitionBytes`; `repartition()` after read |

---

## 14. Master Checklist

### Table Design
- [ ] Delta Lake as default format
- [ ] Partitioned only if > 1TB, on low-cardinality column
- [ ] Liquid Clustering preferred over partitioning + Z-Order for new tables
- [ ] Z-Ordering on ≤ 4 high-cardinality filter columns (for existing partitioned tables)
- [ ] `autoOptimize.optimizeWrite` enabled for streaming/frequent-write tables
- [ ] `autoOptimize.autoCompact` enabled if not SLA-bound
- [ ] `tuneFileSizesForRewrites` enabled for merge-heavy tables

### Query Optimization
- [ ] `SELECT` only needed columns (no `SELECT *`)
- [ ] Filters applied immediately after read (predicate pushdown)
- [ ] Partition/Z-Order column filters in MERGE ON clause
- [ ] Small tables explicitly broadcast with `broadcast()` hint
- [ ] No `collect()` or `toPandas()` on large DataFrames
- [ ] No actions (`count`/`display`) in loops
- [ ] Temp views instead of intermediate Delta tables (when used once)

### Shuffle & Memory
- [ ] AQE enabled (`spark.sql.adaptive.enabled = true`)
- [ ] AOS enabled for auto shuffle partition tuning
- [ ] Shuffle partitions manually tuned if AOS not working (total_shuffle_bytes / 128MB)
- [ ] Data skew identified and addressed before tuning shuffle partitions
- [ ] No unnecessary `distinct()`, `repartition()`, `coalesce()` calls

### Delta Maintenance
- [ ] OPTIMIZE running regularly (daily/weekly)
- [ ] VACUUM running regularly (weekly minimum)
- [ ] ANALYZE TABLE running after major data changes
- [ ] `DESCRIBE DETAIL` checked — avg file size in 128MB–1GB range
- [ ] `DESCRIBE HISTORY` checked — OPTIMIZE run recently

### Cluster (All-Purpose / Job)
- [ ] Job clusters for production (not all-purpose)
- [ ] Compute-optimized instances for OPTIMIZE jobs
- [ ] Memory-optimized instances for heavy shuffle/ML workloads
- [ ] Storage-optimized instances for Delta cache
- [ ] Auto-termination set (10-15 min for all-purpose)
- [ ] Spot instances only for non-SLA workloads (never on driver)
- [ ] Photon enabled where available
- [ ] Latest LTS Databricks Runtime

### Diagnostics
- [ ] Identified slow cell in notebook
- [ ] Checked Query Profile for red/orange nodes (Serverless)
- [ ] Checked Scan node — files pruned > 0 for filtered queries
- [ ] Checked Join nodes — BroadcastHashJoin preferred
- [ ] Checked Exchange node count — minimize shuffles
- [ ] Checked SQL Warehouse monitoring — queue time vs execution time
- [ ] Checked Stages tab for spill/skew/GC (all-purpose cluster)
