# Day 5: Delta Lake
## ACID Transactions, Time Travel, MERGE & SCD Type 2

**Duration**: 1 hour | **Difficulty**: Intermediate → Advanced

---

## 🎯 Learning Objectives

- ✅ Understand what Delta Lake is and why it matters
- ✅ Explain ACID transactions in a data lake context
- ✅ Use time travel to query historical data
- ✅ Perform MERGE (upsert) operations
- ✅ Implement SCD Type 2 using Delta Lake
- ✅ Use Delta features: OPTIMIZE, VACUUM, CLONE

---

## 📚 Content

### 5.1 What is Delta Lake and Why it Matters

**The Problem with Data Lakes (Before Delta)**

Traditional data lakes (just files in S3/ADLS) have serious problems:

| Problem | Example | Impact |
|---------|---------|--------|
| No ACID transactions | Two jobs write simultaneously → corrupted data | Data loss |
| No schema enforcement | Someone accidentally writes wrong columns | Silent corruption |
| No upserts | Can't update a single row in a Parquet file | Must rewrite entire table |
| Slow small files | Hundreds of tiny Parquet files | Slow queries |
| No history | Accidentally deleted data → gone forever | Data loss |

**Delta Lake solves ALL of these:**

```
Traditional Data Lake (Parquet):
Files in S3 + No guarantees + No history + No updates
❌ Unreliable for production

Delta Lake:
Parquet files + Transaction Log + Versioning + ACID
✅ Production-grade, reliable, updatable
```

**Delta Lake = Parquet files + `_delta_log/` directory**

```
/delta/orders/
├── _delta_log/               ← THE SECRET SAUCE
│   ├── 00000000000000000000.json  (version 0: initial write)
│   ├── 00000000000000000001.json  (version 1: INSERT)
│   ├── 00000000000000000002.json  (version 2: UPDATE)
│   └── 00000000000000000003.json  (version 3: DELETE)
├── part-00000-abc123.parquet
├── part-00001-def456.parquet
└── part-00002-ghi789.parquet
```

The `_delta_log` is a JSON transaction log that tracks every change ever made.

---

### 5.2 ACID Transactions in Delta Lake

**ACID** = Atomicity, Consistency, Isolation, Durability

| Property | Meaning | Delta Lake Example |
|----------|---------|-------------------|
| **Atomicity** | All or nothing | A MERGE either fully succeeds or fully rolls back |
| **Consistency** | Data always valid | Schema enforcement prevents bad data |
| **Isolation** | Concurrent readers get consistent view | Reader sees snapshot, not partial write |
| **Durability** | Committed data persists | Transaction log ensures no data loss |

```python
# Creating a Delta table
df.write.format("delta").save("/delta/orders")

# Or as a managed table
df.write.format("delta").saveAsTable("catalog.schema.orders")

# Both support full ACID operations
```

---

### 5.3 Reading and Writing Delta Tables

```python
# ── Reading ────────────────────────────────────────
# Method 1: Path-based
df = spark.read.format("delta").load("/delta/orders")

# Method 2: Table name (Unity Catalog)
df = spark.table("catalog.schema.orders")

# Method 3: SQL
df = spark.sql("SELECT * FROM catalog.schema.orders")

# ── Writing ────────────────────────────────────────
# Overwrite entire table
df.write.format("delta").mode("overwrite").save("/delta/orders")

# Append new rows
df.write.format("delta").mode("append").save("/delta/orders")

# ── Common Operations ─────────────────────────────
# INSERT using SQL
spark.sql("""
    INSERT INTO catalog.schema.orders 
    VALUES (1001, 201, 500.0, 'completed', '2024-01-15')
""")

# UPDATE (not possible in plain Parquet!)
spark.sql("""
    UPDATE catalog.schema.orders
    SET status = 'cancelled'
    WHERE order_id = 1001
""")

# DELETE (not possible in plain Parquet!)
spark.sql("""
    DELETE FROM catalog.schema.orders
    WHERE order_date < '2022-01-01' AND status = 'cancelled'
""")
```

---

### 5.4 Time Travel — Query Historical Versions

This is one of Delta Lake's most powerful features.

```python
from delta.tables import DeltaTable

# See the history of a table
DeltaTable.forPath(spark, "/delta/orders").history().show(truncate=False)

# Output:
# ┌────────┬─────────────────────┬─────────────┬────────────────────────────┐
# │version │timestamp            │operation    │operationParameters         │
# ├────────┼─────────────────────┼─────────────┼────────────────────────────┤
# │3       │2024-01-15 10:05:00  │DELETE       │{"predicate": "[age < 18]"} │
# │2       │2024-01-15 10:03:00  │UPDATE       │{"predicate": "[id = 5]"}   │
# │1       │2024-01-15 10:01:00  │WRITE        │{"mode": "Append"}          │
# │0       │2024-01-15 10:00:00  │CREATE TABLE │{"isManaged": "true"}       │
# └────────┴─────────────────────┴─────────────┴────────────────────────────┘

# ── Query by version number ────────────────────────
# Read version 0 (original table)
df_v0 = spark.read.format("delta") \
    .option("versionAsOf", 0) \
    .load("/delta/orders")

# Read version 2 (after first UPDATE)
df_v2 = spark.read.format("delta") \
    .option("versionAsOf", 2) \
    .load("/delta/orders")

# ── Query by timestamp ─────────────────────────────
df_yesterday = spark.read.format("delta") \
    .option("timestampAsOf", "2024-01-14 00:00:00") \
    .load("/delta/orders")

# ── SQL syntax ─────────────────────────────────────
spark.sql("""
    SELECT * FROM catalog.schema.orders 
    VERSION AS OF 2
""")

spark.sql("""
    SELECT * FROM catalog.schema.orders 
    TIMESTAMP AS OF '2024-01-14 00:00:00'
""")

# ── Restore to previous version ────────────────────
# VERY useful for "oops, I deleted the wrong data" situations!
dt = DeltaTable.forPath(spark, "/delta/orders")
dt.restoreToVersion(1)          # Restore to version 1
dt.restoreToTimestamp("2024-01-14 00:00:00")  # Restore to timestamp
```

**Use cases for time travel:**
- Audit: "What did our orders table look like on Jan 1st?"
- Debugging: "What changed between version 5 and 6?"
- Recovery: "We accidentally deleted a partition — restore it!"
- Reproducibility: "Re-run the model using data as of last Friday"

---

### 5.5 MERGE — The Most Important Delta Operation

MERGE lets you **upsert** (UPDATE existing + INSERT new) in a single atomic operation.

```python
from delta.tables import DeltaTable

# ── Basic MERGE (upsert) ──────────────────────────
target = DeltaTable.forPath(spark, "/delta/orders")

new_data = spark.createDataFrame([
    (1001, 201, 600.0, "updated"),   # EXISTS — should UPDATE
    (1002, 202, 400.0, "new"),       # NEW — should INSERT
    (1003, 203, 750.0, "new"),       # NEW — should INSERT
], ["order_id", "customer_id", "amount", "status"])

target.alias("target").merge(
    new_data.alias("source"),
    "target.order_id = source.order_id"   # Join condition
) \
.whenMatchedUpdate(set={                  # When order_id matches → UPDATE
    "amount": "source.amount",
    "status": "source.status",
    "updated_at": "current_timestamp()"
}) \
.whenNotMatchedInsert(values={            # When no match → INSERT
    "order_id":     "source.order_id",
    "customer_id":  "source.customer_id",
    "amount":       "source.amount",
    "status":       "source.status",
    "created_at":   "current_timestamp()"
}) \
.execute()

# ── MERGE with all clauses ──────────────────────────
target.alias("t").merge(
    source.alias("s"),
    "t.order_id = s.order_id"
) \
.whenMatchedUpdate(
    condition="s.amount > t.amount",  # Only update if new amount is higher
    set={"amount": "s.amount"}
) \
.whenMatchedDelete(
    condition="s.is_deleted = true"   # Delete if flagged
) \
.whenNotMatchedInsertAll() \          # Insert all columns (shorthand)
.execute()

# ── MERGE using SQL ────────────────────────────────
spark.sql("""
    MERGE INTO catalog.schema.orders AS target
    USING new_orders AS source
    ON target.order_id = source.order_id
    WHEN MATCHED THEN
        UPDATE SET
            target.amount = source.amount,
            target.status = source.status
    WHEN NOT MATCHED THEN
        INSERT (order_id, customer_id, amount, status)
        VALUES (source.order_id, source.customer_id, source.amount, source.status)
""")
```

---

### 5.6 SCD Type 2 — Tracking Historical Changes

**What is SCD Type 2?**  
Slowly Changing Dimension Type 2: When a customer's data changes (e.g., moves from NY to CA), we don't overwrite — we keep the old record with an end date and create a new current record.

```
BEFORE (customer 101 lives in NY):
┌─────┬────────┬───────┬────────────┬──────────┬──────────┐
│ id  │ name   │ state │ start_date │ end_date │ is_current│
├─────┼────────┼───────┼────────────┼──────────┼───────────┤
│ 101 │ Alice  │ NY    │ 2020-01-01 │ NULL     │ true      │
└─────┴────────┴───────┴────────────┴──────────┴───────────┘

AFTER (customer 101 moves to CA on 2024-01-15):
┌─────┬────────┬───────┬────────────┬────────────┬──────────┐
│ id  │ name   │ state │ start_date │ end_date   │is_current│
├─────┼────────┼───────┼────────────┼────────────┼──────────┤
│ 101 │ Alice  │ NY    │ 2020-01-01 │ 2024-01-14 │ false    │  ← Old record
│ 101 │ Alice  │ CA    │ 2024-01-15 │ NULL       │ true     │  ← New record
└─────┴────────┴───────┴────────────┴────────────┴──────────┘
```

```python
from delta.tables import DeltaTable
from pyspark.sql.functions import current_date, lit, col

def apply_scd2(target_path, updates_df, key_column, tracked_columns):
    """
    Apply SCD Type 2 to a Delta table.
    
    Args:
        target_path: Path to Delta table
        updates_df: DataFrame with changed records
        key_column: Business key (e.g., 'customer_id')
        tracked_columns: Columns to track for changes
    """
    
    # ── Stage 1: Identify changed records ─────────────
    # Build condition: any tracked column has changed
    change_condition = " OR ".join([
        f"target.{c} != source.{c}" for c in tracked_columns
    ])
    
    # ── Stage 2: Mark old records as expired ──────────
    # Records that exist in target AND have changes → expire them
    target_dt = DeltaTable.forPath(spark, target_path)
    
    target_dt.alias("target").merge(
        updates_df.alias("source"),
        f"target.{key_column} = source.{key_column} AND target.is_current = true"
    ) \
    .whenMatchedUpdate(
        condition=change_condition,
        set={
            "is_current": "false",
            "end_date": "current_date()"
        }
    ) \
    .execute()
    
    # ── Stage 3: Insert new (current) records ─────────
    # Only insert if the record changed (don't insert unchanged records)
    target_current = spark.read.format("delta").load(target_path) \
        .filter(col("is_current") == True)
    
    # Anti-join: records in updates that differ from current target
    new_records = updates_df.alias("s") \
        .join(
            target_current.select(key_column, *tracked_columns).alias("t"),
            on=key_column,
            how="left_anti"  # Records NOT in target (net new or just expired)
        ) \
        .withColumn("is_current", lit(True)) \
        .withColumn("start_date", current_date()) \
        .withColumn("end_date", lit(None).cast("date"))
    
    new_records.write.format("delta").mode("append").save(target_path)

# ── Create initial customer dimension ────────────
customer_schema = """
    customer_id INT, name STRING, state STRING,
    start_date DATE, end_date DATE, is_current BOOLEAN
"""

initial_customers = spark.createDataFrame([
    (101, "Alice",   "NY", "2020-01-01", None, True),
    (102, "Bob",     "CA", "2020-01-01", None, True),
    (103, "Charlie", "TX", "2020-01-01", None, True),
], ["customer_id", "name", "state", "start_date", "end_date", "is_current"])

initial_customers = initial_customers.withColumn("start_date", to_date("start_date"))

initial_customers.write.format("delta") \
    .mode("overwrite") \
    .save("/delta/dim_customers")

# ── Apply changes (Alice moved to CA) ──────────────
updates = spark.createDataFrame([
    (101, "Alice", "CA"),  # Changed: NY → CA
    (102, "Bob",   "CA"),  # No change
    (104, "Diana", "FL"),  # New customer
], ["customer_id", "name", "state"])

apply_scd2(
    target_path="/delta/dim_customers",
    updates_df=updates,
    key_column="customer_id",
    tracked_columns=["name", "state"]
)

# Verify
spark.read.format("delta").load("/delta/dim_customers") \
    .orderBy("customer_id", "start_date") \
    .show()
```

---

### 5.7 Delta Lake Maintenance: OPTIMIZE & VACUUM

```python
from delta.tables import DeltaTable

dt = DeltaTable.forPath(spark, "/delta/orders")

# ── OPTIMIZE: Compact small files ──────────────────
# Small files slow down queries — OPTIMIZE merges them into ~1GB files
dt.optimize().executeCompaction()

# OPTIMIZE with Z-Ordering (cluster data by frequently filtered columns)
dt.optimize().executeZOrderBy("customer_id", "order_date")
# Now queries like: WHERE customer_id = 101 are much faster

# ── VACUUM: Delete old files ────────────────────────
# Delta keeps all old files for time travel (can grow very large!)
# VACUUM removes files older than retention period
dt.vacuum(retentionHours=168)  # Keep 7 days (168 hours) of history

# ⚠️ WARNING: After VACUUM, you can't time-travel before retention period!

# ── Liquid Clustering (Databricks-native, newer alternative to Z-Order) ──
spark.sql("""
    CREATE TABLE catalog.schema.orders
    CLUSTER BY (customer_id, order_date)  -- Automatic, adaptive clustering
    USING DELTA
""")

# ── DESCRIBE DETAIL ────────────────────────────────
spark.sql("DESCRIBE DETAIL catalog.schema.orders").show(truncate=False)
# Shows: numFiles, sizeInBytes, partitionColumns, etc.

# ── Table Properties ────────────────────────────────
spark.sql("""
    ALTER TABLE catalog.schema.orders 
    SET TBLPROPERTIES (
        'delta.enableChangeDataFeed' = 'true',
        'delta.autoOptimize.optimizeWrite' = 'true',
        'delta.autoOptimize.autoCompact' = 'true'
    )
""")
```

---

### 5.8 Change Data Feed (CDF)

Track all changes (inserts, updates, deletes) — useful for downstream pipelines.

```python
# Enable CDF on table
spark.sql("""
    ALTER TABLE catalog.schema.orders
    SET TBLPROPERTIES ('delta.enableChangeDataFeed' = 'true')
""")

# Read the change feed
changes = spark.read.format("delta") \
    .option("readChangeFeed", "true") \
    .option("startingVersion", 5) \
    .table("catalog.schema.orders")

# Extra columns in CDF:
# _change_type: "insert" | "update_preimage" | "update_postimage" | "delete"
# _commit_version: Delta version
# _commit_timestamp: When committed

changes.filter(col("_change_type") == "update_postimage").show()
```

---

## 💻 Hands-On Exercise

```python
# Exercise: Time Travel Recovery Scenario
# 
# Scenario: A pipeline accidentally deleted all orders from January.
# Use time travel to recover them.

# Step 1: Create initial orders table
orders_data = [(i, f"cust_{i%10}", i*100.0, f"2024-01-{(i%28)+1:02d}") 
               for i in range(1, 51)]
df = spark.createDataFrame(orders_data, 
                            ["order_id", "customer_id", "amount", "order_date"])
df.write.format("delta").mode("overwrite").save("/delta/exercise/orders")

# Step 2: Accidentally delete January orders
spark.sql("DELETE FROM delta.`/delta/exercise/orders` WHERE order_date LIKE '2024-01%'")

# Step 3: Realize mistake — how many rows are gone?
after_delete = spark.read.format("delta").load("/delta/exercise/orders").count()
original = spark.read.format("delta") \
    .option("versionAsOf", 0) \
    .load("/delta/exercise/orders").count()
print(f"Before: {original}, After: {after_delete}, Lost: {original - after_delete}")

# Step 4: Recover using time travel
# TODO: Restore the table to version 0
# Hint: DeltaTable.forPath(spark, ...).restoreToVersion(0)
```

---

## ❓ Q&A

### Q1: How is Delta Lake different from Apache Parquet?

| | Parquet | Delta Lake |
|--|---------|-----------|
| Format | Column-store file | Parquet + transaction log |
| ACID | ❌ No | ✅ Yes |
| Updates/Deletes | ❌ Must rewrite | ✅ Supported |
| Time Travel | ❌ No | ✅ Yes |
| Schema enforcement | ❌ No | ✅ Yes |
| MERGE/Upsert | ❌ No | ✅ Yes |

**Use Delta everywhere in Databricks. Parquet is the underlying format; Delta adds the intelligence.**

---

### Q2: How long is history kept by default?

**A:** Delta retains history for **30 days** by default.

```python
# Change retention period
spark.sql("""
    ALTER TABLE my_table 
    SET TBLPROPERTIES ('delta.logRetentionDuration' = 'interval 60 days')
""")
```

---

### Q3: What happens if two jobs try to write simultaneously?

**A:** Delta Lake uses **Optimistic Concurrency Control**:
1. Both jobs read current table version
2. Both apply their changes
3. First job commits successfully
4. Second job checks if conflict exists
   - If no conflict → commits too ✅
   - If conflict → retries automatically ✅ or fails with error

Usually transparent — you don't need to manage it.

---

### Q4: When should I use MERGE vs APPEND?

| Use MERGE when | Use APPEND when |
|----------------|-----------------|
| Updating existing records | Only inserting new records |
| Records can arrive late | Data is immutable (logs, events) |
| Implementing SCD Type 2 | Simple bronze ingestion |
| Deduplication needed | No duplicates possible |

---

### Q5: What is Z-Ordering and when should I use it?

**A:** Z-Ordering co-locates related data in the same files, so Spark skips more files when filtering.

```python
# Use Z-Order on columns you frequently filter on
dt.optimize().executeZOrderBy("customer_id", "order_date")

# Before Z-Order: WHERE customer_id = 101 → scan 100 files
# After Z-Order:  WHERE customer_id = 101 → scan 3 files
```

**Use Z-Order on**: columns in WHERE clauses and JOIN conditions  
**Don't Z-Order**: columns never used in filters

---

## 🔑 Key Takeaways

1. **Delta Lake = Parquet + Transaction Log** — gets you ACID guarantees
2. **Time travel** is your safety net — never lose data accidentally
3. **MERGE** is the workhorse for upserts, deduplication, SCD
4. **OPTIMIZE** + **Z-ORDER** = faster queries
5. **VACUUM** removes old files — only run after confirming time travel not needed
6. **SCD Type 2** tracks historical changes with Delta MERGE
7. **CDF** tracks row-level changes for downstream pipelines
8. Delta should be your **default format** in Databricks

---

**Next Session → Day 6: Advanced Transformations — Window Functions, Complex Joins, Nested Data**
