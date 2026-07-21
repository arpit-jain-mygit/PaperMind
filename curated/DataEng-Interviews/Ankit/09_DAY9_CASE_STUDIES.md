# Day 9: Real-World Case Studies
## Common Scenarios, Architectures & Decision Frameworks

**Duration**: 1 hour | **Difficulty**: Advanced

---

## 🎯 Learning Objectives

- ✅ Solve common real-world data engineering problems
- ✅ Choose the right tool/approach for each scenario
- ✅ Understand architecture patterns used in production
- ✅ Apply decision frameworks to unknown problems
- ✅ Debug common Databricks/Spark issues

---

## 📚 Case Studies

### Case 1: Daily Sales ETL Pipeline

**Scenario**: An e-commerce company receives CSV sales files in S3 every night at midnight. Build a pipeline that processes them and makes clean data available for BI by 6 AM.

**Requirements:**
- Files arrive as CSV in `/raw/sales/YYYY-MM-DD/`
- Need to handle late files (may arrive up to 2 days late)
- Need to detect and quarantine bad records
- BI team queries data in SQL

**Solution Architecture:**
```
S3 CSVs → [Auto Loader] → Bronze (Delta) → [Transform] → Silver (Delta) → [Aggregate] → Gold (Delta) → BI Tool
```

```python
# ── Notebook 1: Bronze Ingest ─────────────────────
import dbutils
from pyspark.sql.functions import current_timestamp, input_file_name

batch_date = dbutils.widgets.get("batch_date")

# Auto Loader handles incremental ingestion
bronze_df = spark.readStream \
    .format("cloudFiles") \
    .option("cloudFiles.format", "csv") \
    .option("cloudFiles.schemaLocation", "/checkpoints/sales/schema") \
    .option("header", True) \
    .load(f"/raw/sales/") \
    .withColumn("_batch_date", lit(batch_date)) \
    .withColumn("_ingested_at", current_timestamp()) \
    .withColumn("_source_file", input_file_name())

bronze_df.writeStream \
    .format("delta") \
    .outputMode("append") \
    .option("checkpointLocation", "/checkpoints/sales/bronze") \
    .trigger(availableNow=True) \
    .start("/delta/bronze/sales") \
    .awaitTermination()

dbutils.notebook.exit("bronze_complete")

# ── Notebook 2: Silver Transform ─────────────────
from pyspark.sql.functions import *
from delta.tables import DeltaTable

batch_date = dbutils.widgets.get("batch_date")

bronze = spark.read.format("delta").load("/delta/bronze/sales") \
    .filter(col("_batch_date") == batch_date)

# Validate
def validate(df):
    return df.withColumn("_dq", 
        when(col("order_id").isNull(), "null_order_id")
        .when(col("amount").cast("double").isNull(), "bad_amount")
        .when(col("order_date").isNull(), "null_date")
        .otherwise("valid")
    )

validated = validate(bronze)

# Separate good and bad
good = validated.filter(col("_dq") == "valid") \
    .withColumn("amount", col("amount").cast("double")) \
    .withColumn("order_date", to_date("order_date")) \
    .drop("_dq")

bad = validated.filter(col("_dq") != "valid")

# Upsert to silver (handles late arrivals correctly)
if DeltaTable.isDeltaTable(spark, "/delta/silver/sales"):
    target = DeltaTable.forPath(spark, "/delta/silver/sales")
    target.alias("t").merge(
        good.alias("s"), "t.order_id = s.order_id"
    ).whenMatchedUpdateAll() \
     .whenNotMatchedInsertAll() \
     .execute()
else:
    good.write.format("delta").save("/delta/silver/sales")

# Quarantine bad records
if bad.count() > 0:
    bad.write.format("delta").mode("append").save("/delta/quarantine/sales")
    print(f"⚠️ {bad.count()} records quarantined for {batch_date}")

dbutils.notebook.exit(f"silver_complete:{good.count()}_valid")

# ── Notebook 3: Gold Aggregation ─────────────────
daily_summary = spark.read.format("delta").load("/delta/silver/sales") \
    .filter(col("status") == "completed") \
    .groupBy("order_date", "state", "product_category") \
    .agg(
        count("*").alias("orders"),
        sum("amount").alias("revenue"),
        avg("amount").alias("aov"),
        countDistinct("customer_id").alias("unique_buyers")
    )

daily_summary.write.format("delta") \
    .mode("overwrite") \
    .option("replaceWhere", f"order_date = '{batch_date}'") \
    .saveAsTable("gold.daily_sales_summary")
```

---

### Case 2: Real-Time Event Processing

**Scenario**: Website generates clickstream events (JSON) to Kafka. Need near-real-time processing for fraud detection.

**Requirements:**
- 10M events/day, peaks of 5K events/second
- Flag suspicious patterns within 5 minutes
- Must handle late events (up to 30 min late)

```python
# ── Streaming Pipeline ─────────────────────────────
from pyspark.sql.functions import *
from pyspark.sql.types import *

# Define event schema
schema = StructType([
    StructField("event_id",   StringType()),
    StructField("user_id",    StringType()),
    StructField("action",     StringType()),
    StructField("amount",     DoubleType()),
    StructField("event_time", TimestampType()),
    StructField("ip_address", StringType())
])

# Read from Kafka
events = spark.readStream \
    .format("kafka") \
    .option("kafka.bootstrap.servers", "broker:9092") \
    .option("subscribe", "clickstream") \
    .load() \
    .select(from_json(col("value").cast("string"), schema).alias("data")) \
    .select("data.*")

# Apply watermark for late events (30 min tolerance)
events_with_watermark = events \
    .withWatermark("event_time", "30 minutes")

# Fraud detection: >5 transactions in 10 mins from same user
suspicious = events_with_watermark \
    .filter(col("action") == "purchase") \
    .groupBy(
        window("event_time", "10 minutes"),
        "user_id"
    ) \
    .agg(
        count("*").alias("txn_count"),
        sum("amount").alias("total_amount"),
        countDistinct("ip_address").alias("unique_ips")
    ) \
    .filter(
        (col("txn_count") > 5) |
        (col("total_amount") > 10000) |
        (col("unique_ips") > 3)
    ) \
    .withColumn("flag_reason",
        when(col("txn_count") > 5, "high_frequency")
        .when(col("total_amount") > 10000, "high_value")
        .otherwise("multiple_ips")
    )

# Write suspicious activity to Delta
query = suspicious.writeStream \
    .format("delta") \
    .outputMode("append") \
    .option("checkpointLocation", "/checkpoints/fraud") \
    .trigger(processingTime="1 minute") \
    .start("/delta/fraud/suspicious_events")

query.awaitTermination()
```

---

### Case 3: Data Warehouse Migration

**Scenario**: Migrate from an on-premise SQL Server data warehouse to Databricks Lakehouse.

**Decision Framework:**

```
Step 1: INVENTORY
  - List all tables (fact tables, dimension tables)
  - Identify dependencies (views, stored procs)
  - Measure table sizes

Step 2: CLASSIFY
  ┌─────────────────────────────────────────┐
  │ SOURCE TYPE    → DATABRICKS EQUIVALENT  │
  ├─────────────────────────────────────────┤
  │ Fact tables    → Silver/Gold Delta      │
  │ Dim tables     → Silver Delta (SCD2)    │
  │ Staging tables → Bronze Delta           │
  │ Stored procs   → PySpark notebooks      │
  │ SSRS Reports   → Databricks SQL         │
  │ SSIS packages  → Databricks Jobs/DLT   │
  └─────────────────────────────────────────┘

Step 3: MIGRATE ORDER
  1. Small dim tables (easy)
  2. Large fact tables (incremental)
  3. Aggregated views → Gold tables
  4. Reports → Databricks SQL dashboards

Step 4: VALIDATE
  - Row count match
  - Sum of key metrics match (revenue, count)
  - Sample-based diff testing
```

```python
# ── Migration validation ───────────────────────────
def validate_migration(source_table, target_table, key_col, metric_cols):
    """
    Compare row counts and metric sums between source and target.
    """
    source = spark.sql(f"SELECT * FROM {source_table}")
    target = spark.sql(f"SELECT * FROM {target_table}")
    
    results = {}
    
    # Row count
    results["source_rows"] = source.count()
    results["target_rows"] = target.count()
    results["rows_match"] = results["source_rows"] == results["target_rows"]
    
    # Metric sums
    for col_name in metric_cols:
        src_sum = source.select(sum(col_name)).collect()[0][0]
        tgt_sum = target.select(sum(col_name)).collect()[0][0]
        tolerance = 0.0001  # 0.01% tolerance for float precision
        results[f"{col_name}_match"] = abs(src_sum - tgt_sum) / src_sum < tolerance
    
    # Summarize
    all_pass = all(v == True for k, v in results.items() if k.endswith("match"))
    print(f"\n{'✅ PASSED' if all_pass else '❌ FAILED'}: {source_table} → {target_table}")
    for k, v in results.items():
        status = "✅" if v == True else "❌" if v == False else "  "
        print(f"  {status} {k}: {v}")
    
    return results, all_pass

validate_migration(
    "old_warehouse.sales_fact",
    "new_catalog.gold.sales_fact",
    "order_id",
    ["amount", "quantity", "discount"]
)
```

---

### Case 4: Slowly Changing Dimension (Production Setup)

**Scenario**: Product catalog changes daily (prices, descriptions, categories). Historical orders must point to what the product looked like at time of purchase.

```python
# ── Product Dimension with SCD Type 2 ─────────────
from delta.tables import DeltaTable
from pyspark.sql.functions import *

def process_scd2(updates_df, target_path, natural_key, tracked_cols):
    """Full SCD Type 2 implementation."""
    
    today = current_date()
    FAR_FUTURE = lit("9999-12-31").cast("date")
    
    # Ensure target exists
    if not DeltaTable.isDeltaTable(spark, target_path):
        updates_df \
            .withColumn("eff_start_date", today) \
            .withColumn("eff_end_date",   FAR_FUTURE) \
            .withColumn("is_current",     lit(True)) \
            .write.format("delta").save(target_path)
        print("Created new SCD2 table")
        return
    
    target = DeltaTable.forPath(spark, target_path)
    
    # Find records that changed
    current = spark.read.format("delta").load(target_path) \
        .filter(col("is_current") == True)
    
    change_expr = " OR ".join([f"s.{c} != t.{c}" for c in tracked_cols])
    
    # Expire old records
    target.alias("t").merge(
        updates_df.alias("s"),
        f"t.{natural_key} = s.{natural_key} AND t.is_current = true"
    ).whenMatchedUpdate(
        condition=change_expr,
        set={
            "is_current":   "false",
            "eff_end_date": "date_sub(current_date(), 1)"
        }
    ).execute()
    
    # Insert new records for changed + new items
    new_or_changed = updates_df.alias("s") \
        .join(current.alias("t"), natural_key, "left") \
        .where(
            col(f"t.{natural_key}").isNull() |  # New record
            expr(change_expr.replace("s.", "s.").replace("t.", "t."))  # Changed
        ) \
        .select("s.*") \
        .withColumn("eff_start_date", today) \
        .withColumn("eff_end_date",   FAR_FUTURE) \
        .withColumn("is_current",     lit(True))
    
    new_or_changed.write.format("delta").mode("append").save(target_path)
    print(f"SCD2 complete: {new_or_changed.count()} new/changed records")

# Usage
product_updates = spark.read.csv("/raw/products_today.csv", header=True, inferSchema=True)

process_scd2(
    updates_df=product_updates,
    target_path="/delta/dim/products",
    natural_key="product_id",
    tracked_cols=["price", "category", "supplier_id", "description"]
)
```

---

### 9.2 Common Problems & Solutions

#### **Problem 1: Out of Memory (OOM)**

```
Symptoms: "java.lang.OutOfMemoryError: Java heap space"
           "Container killed by YARN for exceeding memory limits"

Causes:
  - Too many partitions being collected to driver
  - Huge broadcast join
  - Large groupBy with many distinct keys
  - collect() on large DataFrame

Solutions:
```

```python
# ❌ Problem
big_df.collect()  # Crashes driver

# ✅ Fix: Write to storage instead
big_df.write.parquet("/output")

# ❌ Problem  
spark.conf.set("spark.sql.autoBroadcastJoinThreshold", "-1")
large_df.join(broadcast(also_large_df), "id")  # OOM!

# ✅ Fix: Don't broadcast large tables
spark.conf.set("spark.sql.autoBroadcastJoinThreshold", "50mb")

# ❌ Problem: Skewed groupBy with many keys
df.groupBy("user_id").agg(collect_list("item"))  # Driver OOM if many items per user

# ✅ Fix: Limit list size
from pyspark.sql.functions import slice
df.groupBy("user_id").agg(
    slice(collect_list("item"), 1, 100).alias("items")  # Max 100 items
)
```

#### **Problem 2: Job Runs Too Long**

```
Diagnosis checklist:
1. Check Spark UI → which stage is slowest?
2. In slowest stage: are all tasks equal time? (skew!)
3. How many files are being read? (small file problem?)
4. How many shuffle partitions? (200 default = wrong)

Solutions by root cause:
```

```python
# Root cause: Small files
# Check: ls /data/events | wc -l → 50,000 files!

# Fix: Compact files first (run as separate job, weekly)
spark.read.format("delta").load("/data/events") \
    .repartition(100) \
    .write.format("delta").mode("overwrite").save("/data/events_compacted")

# Or use Delta OPTIMIZE
spark.sql("OPTIMIZE delta.`/data/events`")

# Root cause: Too many shuffle partitions
spark.conf.set("spark.sql.shuffle.partitions", "auto")  # AQE decides

# Root cause: Skew
# Enable AQE skew join
spark.conf.set("spark.sql.adaptive.skewJoin.enabled", "true")
```

#### **Problem 3: Data Quality Issues**

```python
# Always audit your pipeline with these checks:

def full_dq_audit(df, table_name):
    from pyspark.sql.functions import count, sum, avg, min, max, countDistinct
    
    total = df.count()
    print(f"\n── {table_name} ({total:,} rows) ──")
    
    for c in df.columns:
        null_count = df.filter(col(c).isNull()).count()
        null_pct = null_count / total * 100 if total > 0 else 0
        distinct = df.select(c).distinct().count()
        
        flag = "⚠️" if null_pct > 10 else "✅"
        print(f"  {flag} {c}: {null_count:,} nulls ({null_pct:.1f}%), {distinct:,} distinct")
```

---

### 9.3 Architecture Decision Framework

When given a problem, use this framework:

```
1. WHAT IS THE DATA VOLUME?
   < 1 GB   → pandas (no Spark needed!)
   1-100 GB → small Spark cluster (4-8 workers)
   > 100 GB → larger cluster or optimize partitions
   > 1 TB   → review architecture (partitioning critical)

2. HOW FRESH DOES THE DATA NEED TO BE?
   Daily    → batch job (Databricks Jobs, cron)
   Hourly   → micro-batch (Auto Loader, trigger every 15 min)
   Minutes  → Structured Streaming
   Seconds  → Streaming + specialized tool (Kafka + Flink)

3. HOW COMPLEX ARE THE TRANSFORMATIONS?
   Simple (filter, join, aggregate) → Pure SQL / PySpark
   Medium (window functions, SCD)  → PySpark + Delta MERGE
   Complex (ML features, text NLP) → PySpark + custom UDFs

4. WHAT ARE THE RELIABILITY REQUIREMENTS?
   Exploratory/Ad-hoc → Notebooks, no SLA
   Daily business report → Jobs + error alerts + retries
   Revenue-critical    → Jobs + monitoring + validation + rollback

5. WILL THE DATA BE UPDATED AFTER INITIAL LOAD?
   Append-only (logs, events) → Delta append
   Updates needed             → Delta MERGE
   Historical tracking        → SCD Type 2
```

---

## 💻 Hands-On Exercise

**Scenario**: Debug this slow pipeline and fix it.

```python
# SLOW PIPELINE (find and fix the performance issues):
employees = spark.read.csv("/data/employees.csv", header=True, inferSchema=True)
departments = spark.read.csv("/data/departments.csv", header=True, inferSchema=True)
salaries = spark.read.csv("/data/salaries.csv", header=True, inferSchema=True)

result = employees \
    .join(departments, "dept_id") \
    .join(salaries, "employee_id") \
    .select("*") \
    .filter(col("salary") > 50000) \
    .filter(col("department") == "Engineering") \
    .groupBy("department", "location") \
    .agg(avg("salary"), count("*"))

result.show()

# Issues to find:
# 1. CSV format (should be Parquet/Delta)
# 2. inferSchema (should be explicit schema)
# 3. Filter applied AFTER join (filter pushdown needed)
# 4. select("*") wastes resources
# 5. departments is likely small (should be broadcast)
# 6. No shuffle partition configuration

# WRITE YOUR OPTIMIZED VERSION HERE
```

---

## ❓ Q&A

### Q1: When do I need Unity Catalog vs hive_metastore?

| | hive_metastore | Unity Catalog |
|-|----------------|--------------|
| Location | Per-workspace | Centralized (cross-workspace) |
| Access control | Basic | Fine-grained (row/column) |
| Lineage | No | Yes |
| Auditing | Basic | Full audit trail |
| Use for | Dev, small teams | Production, multi-team |

---

### Q2: Should I use PySpark or Spark SQL?

```
Use PySpark when:
  - Complex Python logic needed
  - Reusable functions (class/module)
  - Pandas UDFs
  - Dynamic query building

Use Spark SQL when:
  - Team is SQL-fluent
  - Simple queries
  - BI analysts write/read it
  - Ad-hoc exploration

Reality: Mix both freely — they're the same engine!
```

---

### Q3: How do I handle CDC (Change Data Capture) from a source DB?

```
CDC = Stream changes from operational DB into Databricks

Tools:
  - Debezium → captures DB changes → Kafka → Databricks Streaming
  - AWS DMS → captures RDS/SQL Server changes → S3 → Auto Loader
  - Fivetran/Airbyte → managed connectors → Delta tables

Pattern:
  Source DB (CDC enabled)
    → CDC Connector (Debezium/Fivetran)
    → Message Queue (Kafka) or S3
    → Databricks Streaming / Auto Loader
    → Bronze Delta Table
    → MERGE to Silver (apply inserts/updates/deletes)
```

---

## 🔑 Key Takeaways

1. **Architecture first**: Understand volume, freshness, complexity before coding
2. **Bronze/Silver/Gold** works for virtually every ETL scenario
3. **MERGE** is essential for CDC, late data, SCD Type 2
4. **Filter early, select early** — reduce data volume before expensive operations
5. **OOM** usually means collect() or wrong broadcast threshold
6. **Slow jobs** usually means skew, small files, or wrong partition count
7. **Always validate migration** with row counts + metric sums
8. **Mix SQL and PySpark freely** — they're the same engine

---

**Next Session → Day 10: Capstone Project & Production Best Practices**
