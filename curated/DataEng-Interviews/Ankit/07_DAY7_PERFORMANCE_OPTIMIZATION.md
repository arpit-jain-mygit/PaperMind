# Day 7: Performance Optimization
## Partitioning, Caching, Skew Handling & Query Tuning

**Duration**: 1 hour | **Difficulty**: Advanced

---

## 🎯 Learning Objectives

- ✅ Identify and resolve data skew
- ✅ Apply correct partitioning strategy
- ✅ Use caching and persistence effectively
- ✅ Configure Spark settings for performance
- ✅ Use the Spark UI to diagnose bottlenecks
- ✅ Apply adaptive query execution (AQE)

---

## 📚 Content

### 7.1 How to Diagnose Performance Problems

Before optimizing, identify WHERE the problem is.

```python
# ── Spark UI: Your Primary Tool ───────────────────
# In Databricks: Click the Spark icon under a cell that ran
# OR go to Cluster → Spark UI

# What to look for:
# 1. Stages tab: Which stage is slowest?
# 2. Tasks tab: Are some tasks much slower? (= data skew!)
# 3. Storage tab: What's cached?
# 4. SQL tab: Query execution plan

# ── In-code diagnostics ───────────────────────────
# Count records per partition (for skew detection)
from pyspark.sql.functions import spark_partition_id, count

df.withColumn("partition_id", spark_partition_id()) \
  .groupBy("partition_id") \
  .agg(count("*").alias("record_count")) \
  .orderBy("partition_id") \
  .show()

# Even distribution: all partitions ~same count
# Skewed: one partition has 10× more records than others

# Check explain plan
df.filter(col("state") == "NY").explain(True)  # Shows physical + logical plan
```

---

### 7.2 Partitioning Strategies

There are two kinds of partitioning: **write-time** and **in-memory**.

#### **Write-time Partitioning (Hive-style partitions)**

Physically organizes data files by column values.

```python
# Write partitioned data
df.write \
    .partitionBy("year", "month") \
    .format("delta") \
    .mode("overwrite") \
    .save("/delta/orders")

# Result:
# /delta/orders/year=2024/month=01/part-*.parquet
# /delta/orders/year=2024/month=02/part-*.parquet
# /delta/orders/year=2024/month=03/part-*.parquet

# Partition pruning: Spark skips irrelevant partitions
df = spark.read.format("delta").load("/delta/orders")
df.filter(col("year") == 2024).filter(col("month") == 1)
# Only reads /year=2024/month=01/ → skips all other files!

# ⚠️ Common mistake: Over-partitioning
# BAD: Partition by high-cardinality column
df.write.partitionBy("customer_id")  # ❌ Millions of tiny folders!

# GOOD: Partition by date (low cardinality, natural boundary)
df.write.partitionBy("year", "month")  # ✅
```

**Choosing partition columns:**
- Low cardinality (days, months, states — not customer_id!)
- Frequently filtered in WHERE clauses
- Balanced partition sizes (not one huge, rest tiny)

---

#### **In-Memory Partitioning (shuffle partitions)**

```python
# Default: 200 shuffle partitions (often wrong for your data)
spark.conf.get("spark.sql.shuffle.partitions")  # "200"

# Small data: 200 partitions is too many → overhead
spark.conf.set("spark.sql.shuffle.partitions", "10")

# Large data: 200 might be too few
spark.conf.set("spark.sql.shuffle.partitions", "800")

# Formula: Target ~128MB per partition
# Total data after shuffle / 128MB = shuffle partitions

# Better: Let AQE decide automatically (see section 7.5)
spark.conf.set("spark.sql.adaptive.enabled", "true")

# Check current partition count
df.rdd.getNumPartitions()

# Increase parallelism
df.repartition(100)  # Distributes evenly, causes shuffle

# Reduce without full shuffle (only merge)
df.coalesce(10)
```

---

### 7.3 Data Skew — The Hardest Performance Problem

Data skew happens when one partition has far more data than others.

```
Even distribution (GOOD):
P1: 1M rows  P2: 1M rows  P3: 1M rows  P4: 1M rows
Time: 1 min (all finish together)

Skewed distribution (BAD):
P1: 100K rows  P2: 100K rows  P3: 100K rows  P4: 10M rows (SKEWED!)
Time: 10 min (waiting for P4!)
```

**Common causes**: Large customer (like Amazon joining with orders), `groupBy` on low-cardinality column, null values in join keys.

```python
# ── Detect skew ────────────────────────────────────
# High task time variance in Spark UI
# Also: check key distribution
df.groupBy("customer_id") \
  .count() \
  .orderBy(col("count").desc()) \
  .show(10)
# If top key has 10M rows and average is 100 → SKEWED

# ── Fix 1: Salt the skewed key ─────────────────────
import random
from pyspark.sql.functions import rand, floor, concat_ws, lit

# Add random "salt" to break big key into smaller ones
N_SALTS = 10

skewed_df = large_df.withColumn(
    "salted_key",
    concat_ws("_", col("customer_id"), (rand() * N_SALTS).cast("int"))
)

lookup_df_salted = lookup_df.withColumn(
    "salt", explode(array([lit(i) for i in range(N_SALTS)]))
).withColumn(
    "salted_key",
    concat_ws("_", col("customer_id"), col("salt"))
)

# Join on salted key instead of raw key
result = skewed_df.join(lookup_df_salted, on="salted_key", how="inner")

# ── Fix 2: Broadcast the small table ─────────────
from pyspark.sql.functions import broadcast

# If one side of join is small (< 10MB), broadcast it
result = orders.join(broadcast(small_lookup), on="customer_id", how="inner")
# No shuffle needed! Each executor gets the whole lookup table

# ── Fix 3: Skew hint (Databricks / Spark 3.x) ─────
result = orders.join(
    customers.hint("skew", "customer_id"),  # Tell Spark this column is skewed
    on="customer_id"
)
# AQE handles this automatically in Spark 3.x

# ── Fix 4: AQE Skew Join (automatic) ──────────────
spark.conf.set("spark.sql.adaptive.skewJoin.enabled", "true")
spark.conf.set("spark.sql.adaptive.skewJoin.skewedPartitionThresholdInBytes", "256MB")
```

---

### 7.4 Caching and Persistence

```python
# ── When to cache ──────────────────────────────────
# Good candidates for caching:
# 1. DataFrame used multiple times in the same notebook/job
# 2. Expensive computation (many joins, aggregations)
# 3. Reference data / lookups

# ── Cache vs Persist ──────────────────────────────
# cache() = persist(StorageLevel.MEMORY_AND_DISK)
df.cache()   # Store in memory; spill to disk if needed

from pyspark import StorageLevel

# Explicit storage levels
df.persist(StorageLevel.MEMORY_ONLY)           # Memory only (fastest, may fail)
df.persist(StorageLevel.MEMORY_AND_DISK)       # Memory + disk overflow (default)
df.persist(StorageLevel.DISK_ONLY)             # Disk only (slowest, reliable)
df.persist(StorageLevel.MEMORY_AND_DISK_SER)   # Serialized (less memory, slower)
df.persist(StorageLevel.OFF_HEAP)              # Off-heap memory (advanced)

# ── Always trigger cache with an action ───────────
df.cache()           # Cache registered but NOT executed yet (lazy!)
df.count()           # ← Triggers execution AND fills cache

# ── Check if cached ───────────────────────────────
df.storageLevel

# ── Release cache ─────────────────────────────────
df.unpersist()       # Free up memory when done

# ── Practical example ─────────────────────────────
# BAD: Recomputes orders_enriched every time
summary1 = orders_enriched.groupBy("state").sum("amount")
summary2 = orders_enriched.groupBy("tier").count()
summary3 = orders_enriched.filter(col("amount") > 500).count()

# GOOD: Cache once, reuse
orders_enriched.cache()
orders_enriched.count()  # Trigger cache fill

summary1 = orders_enriched.groupBy("state").sum("amount")
summary2 = orders_enriched.groupBy("tier").count()
summary3 = orders_enriched.filter(col("amount") > 500).count()
orders_enriched.unpersist()  # Clean up
```

---

### 7.5 Adaptive Query Execution (AQE)

Spark 3.x feature that automatically optimizes your query at runtime.

```python
# Enable AQE (enabled by default in Databricks)
spark.conf.set("spark.sql.adaptive.enabled", "true")

# ── Feature 1: Dynamically coalesce shuffle partitions ──
# Without AQE: you set 200 shuffle partitions upfront
# With AQE: Spark merges empty/small partitions automatically
spark.conf.set("spark.sql.adaptive.coalescePartitions.enabled", "true")

# ── Feature 2: Dynamic skew join handling ─────────────
spark.conf.set("spark.sql.adaptive.skewJoin.enabled", "true")

# ── Feature 3: Dynamic join strategy switch ───────────
# Spark can switch shuffle join → broadcast join at runtime
# if it discovers the table is smaller than expected
spark.conf.set("spark.sql.adaptive.localShuffleReader.enabled", "true")

# Nothing else needed — AQE handles it automatically!
```

---

### 7.6 Key Spark Configuration Settings

```python
# ── Must-know configs for data engineers ─────────

# Shuffle partitions (most important!)
spark.conf.set("spark.sql.shuffle.partitions", "auto")   # AQE auto-tune

# Broadcast threshold: tables smaller than this are auto-broadcast
spark.conf.set("spark.sql.autoBroadcastJoinThreshold", "50MB")  # Default 10MB

# File split size (affects number of initial partitions)
spark.conf.set("spark.sql.files.maxPartitionBytes", "134217728")  # 128MB

# Enable predicate pushdown (usually already on)
spark.conf.set("spark.sql.parquet.filterPushdown", "true")

# Dynamic partition overwrite (prevents deleting ALL partitions on overwrite)
spark.conf.set("spark.sql.sources.partitionOverwriteMode", "dynamic")
# With this: overwrite only affects partitions in your new data

# Arrow for Pandas UDFs
spark.conf.set("spark.sql.execution.arrow.pyspark.enabled", "true")

# Kryo serialization (faster than default Java serialization)
spark.conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
```

---

### 7.7 The Catalyst Optimizer & Query Plans

Spark's Catalyst optimizer automatically rewrites your queries for efficiency.

```python
# View query plan
df.explain()            # Physical plan only
df.explain("simple")    # Simple plan
df.explain("extended")  # Logical + Physical plans
df.explain("codegen")   # Code generation plan

# What Catalyst does automatically:
# ✅ Predicate pushdown: filter early
# ✅ Column pruning: only read needed columns
# ✅ Constant folding: evaluate constants at compile time
# ✅ Join reordering: put smaller tables first
# ✅ Null propagation: simplify null handling

# Example: You write this
df.select("*") \
  .join(lookup, "id") \
  .filter(col("amount") > 100) \
  .select("id", "name", "amount")

# Catalyst rewrites to:
# 1. Apply filter early (before join) → less data to join
# 2. Only read needed columns from parquet
# 3. Push filter into parquet reader (skip files/row groups)
```

---

### 7.8 Performance Checklist

Run through this for every slow job:

```
✅ 1. Check partition size: ~128MB per partition
✅ 2. Detect skew: check top 10 keys by count
✅ 3. Use broadcast for small tables (< 50MB)
✅ 4. Enable AQE: spark.sql.adaptive.enabled = true
✅ 5. Cache DataFrames used multiple times
✅ 6. Filter early: reduce data before joins/aggregations
✅ 7. Select only needed columns early
✅ 8. Use Parquet or Delta (not CSV)
✅ 9. OPTIMIZE + Z-ORDER Delta tables regularly
✅ 10. Set correct shuffle partitions
✅ 11. Avoid collect() on large datasets
✅ 12. Use Pandas UDF over Python UDF
✅ 13. Use built-in functions over UDFs
```

---

## 💻 Hands-On Exercise

```python
# Compare performance: naive vs optimized

# Generate sample data
import random

data = [(i, f"customer_{random.randint(1,5)}", random.uniform(10, 1000), 
         f"2024-{random.randint(1,12):02d}-01") 
        for i in range(1, 500001)]

orders = spark.createDataFrame(data, ["order_id", "customer_id", "amount", "order_date"])
customers = spark.createDataFrame([
    (f"customer_{i}", f"Name {i}", ["NY","CA","TX","FL"][i%4])
    for i in range(1, 6)
], ["customer_id", "name", "state"])

# ── Naive approach ─────────────────────────────────
import time

start = time.time()
result1 = orders \
    .join(customers, on="customer_id") \
    .filter(col("amount") > 500) \
    .groupBy("state") \
    .sum("amount")
result1.collect()
naive_time = time.time() - start

# ── Optimized approach ─────────────────────────────
start = time.time()
filtered_orders = orders.filter(col("amount") > 500)  # Filter BEFORE join
result2 = filtered_orders \
    .join(broadcast(customers), on="customer_id") \
    .groupBy("state") \
    .sum("amount")
result2.collect()
opt_time = time.time() - start

print(f"Naive:     {naive_time:.2f}s")
print(f"Optimized: {opt_time:.2f}s")
print(f"Speedup:   {naive_time/opt_time:.1f}×")
```

---

## ❓ Q&A

### Q1: How many partitions should I have for a 10GB file?

```
Target: 128MB per partition
10 GB = 10,240 MB
Partitions = 10,240 / 128 = 80 partitions

For joins/groupBy: often need 2× more = 160 shuffle partitions
Set: spark.conf.set("spark.sql.shuffle.partitions", "160")
```

---

### Q2: Should I always cache?

**A:** No — caching has a cost too.

**Cache ONLY when:**
- Same DataFrame used 2+ times in same job
- Expensive to recompute (many joins, large file reads)

**Don't cache when:**
- Single-use DataFrame
- Very large DataFrame that won't fit in memory
- Quick simple transformation

---

### Q3: What is predicate pushdown?

**A:** Spark (and Parquet) can skip data before reading it.

```python
# You write:
df = spark.read.parquet("/data/orders")
df.filter(col("state") == "NY").count()

# Parquet predicate pushdown:
# → Only reads row groups where state = "NY"
# → Skips all other row groups entirely!

# This is why Parquet is SO much faster than CSV for analytics
```

---

### Q4: When does AQE not help?

**A:** AQE can't fix:
- Very slow individual stages (bad algorithms)
- Wrong partition columns for write-time partitioning
- Reading too many small files (small file problem)
- Python UDF overhead

---

## 🔑 Key Takeaways

1. **Diagnose first** (Spark UI) before optimizing
2. **Skew** = one task running 10× longer than others
3. **Broadcast** small tables (< 50MB) to avoid shuffles
4. **Filter and select columns early** — reduce data volume ASAP
5. **AQE** (Adaptive Query Execution) — enable it, it helps automatically
6. **Cache judiciously** — only multi-use, expensive DataFrames
7. **~128MB per partition** is the target for efficiency
8. **Built-in functions > Pandas UDF > Python UDF** for performance

---

**Next Session → Day 8: Workflow Orchestration — Jobs, Scheduling, Error Handling & Monitoring**
