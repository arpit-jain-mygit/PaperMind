# Day 4: Data Ingestion & ETL Patterns
## Bronze / Silver / Gold Architecture & Incremental Loading

**Duration**: 1 hour | **Difficulty**: Intermediate

---

## 🎯 Learning Objectives

- ✅ Understand the Medallion (Bronze/Silver/Gold) architecture
- ✅ Build a full ETL pipeline in PySpark
- ✅ Implement incremental loading with high-water mark
- ✅ Handle schema evolution and data quality
- ✅ Work with Auto Loader for streaming ingestion

---

## 📚 Content

### 4.1 What is ETL?

**ETL = Extract, Transform, Load**

```
SOURCE DATA          TRANSFORMATION          TARGET
─────────────────────────────────────────────────────
CSV files, APIs  →  Clean, Join, Enrich  →  Delta Tables
Databases        →  Aggregate, Validate  →  Data Warehouse
JSON events      →  Standardize, Filter  →  Gold Layer
```

**Why ETL matters for data engineers:**
- Raw data is messy, inconsistent, incomplete
- Business analytics needs clean, reliable data
- Pipelines must handle new data arriving every day/hour/minute

---

### 4.2 The Medallion Architecture (Bronze / Silver / Gold)

This is the **most widely adopted architecture** in modern data lakes, especially on Databricks.

```
┌──────────────────────────────────────────────────────────────┐
│                   MEDALLION ARCHITECTURE                      │
│                                                               │
│  SOURCE       BRONZE          SILVER           GOLD          │
│  ─────────────────────────────────────────────────────────── │
│              ┌────────┐     ┌────────┐     ┌────────┐        │
│  CSV    ───► │  Raw   │───► │Cleaned │───► │Business│        │
│  JSON   ───► │  Data  │     │Enriched│     │ Models │        │
│  DB     ───► │ As-Is  │     │Validated    │ KPIs   │        │
│  API    ───► │        │     │        │     │        │        │
│              └────────┘     └────────┘     └────────┘        │
│                                                               │
│  Quality: Low  ──────────────────────────────────► High      │
│  Schema:  Raw  ──────────────────────────────────► Typed     │
│  History: Full ──────────────────────────────────► Curated   │
└──────────────────────────────────────────────────────────────┘
```

#### **Bronze Layer — Raw Ingestion**
- **What**: Exact copy of source data, no changes
- **Why**: Audit trail, replayability, debugging
- **Schema**: Often same as source (or with added metadata)
- **Format**: Delta (preserves history)

```python
# Bronze: Just land the data as-is with metadata
from pyspark.sql.functions import current_timestamp, lit, input_file_name

bronze_df = spark.read.json("/raw/events/*.json") \
    .withColumn("_ingested_at", current_timestamp()) \
    .withColumn("_source_file", input_file_name()) \
    .withColumn("_batch_id", lit("2024-01-15-001"))

bronze_df.write.format("delta") \
    .mode("append") \
    .save("/delta/bronze/events")
```

#### **Silver Layer — Cleaned & Enriched**
- **What**: Cleaned, validated, typed, joined data
- **Why**: Reliable, deduplicated, quality-checked
- **Schema**: Explicit types, renamed columns, nulls handled

```python
# Silver: Clean, validate, enrich
from pyspark.sql.functions import col, when, to_timestamp, trim

silver_df = spark.read.format("delta").load("/delta/bronze/events") \
    .filter(col("event_id").isNotNull()) \
    .dropDuplicates(["event_id"]) \
    .withColumn("event_time", to_timestamp(col("event_ts_raw"))) \
    .withColumn("user_id", col("userId").cast("long")) \
    .withColumn("event_type", trim(col("event_type"))) \
    .drop("userId", "event_ts_raw")  # Drop raw/redundant columns

# Add data quality flags
silver_df = silver_df.withColumn(
    "_dq_status",
    when(col("amount") < 0, "invalid_amount")
    .when(col("user_id").isNull(), "missing_user")
    .otherwise("valid")
)

silver_df.write.format("delta") \
    .mode("overwrite") \
    .option("overwriteSchema", True) \
    .save("/delta/silver/events")
```

#### **Gold Layer — Business Aggregations**
- **What**: Aggregated, business-ready, often denormalized
- **Why**: Fast queries for BI tools, dashboards
- **Schema**: Fact and dimension tables or wide flat tables

```python
# Gold: Business-ready aggregates
from pyspark.sql.functions import sum, count, avg, to_date

events = spark.read.format("delta").load("/delta/silver/events")
users  = spark.read.format("delta").load("/delta/silver/users")

gold_df = events \
    .filter(col("_dq_status") == "valid") \
    .join(users, on="user_id", how="left") \
    .groupBy(
        to_date("event_time").alias("date"),
        "country",
        "product_category"
    ) \
    .agg(
        count("event_id").alias("event_count"),
        sum("amount").alias("total_revenue"),
        avg("amount").alias("avg_revenue"),
        count("user_id").alias("unique_users")
    )

gold_df.write.format("delta") \
    .mode("overwrite") \
    .saveAsTable("catalog.gold.daily_product_summary")
```

---

### 4.3 Incremental Loading — The Core Pattern

**Full Load vs Incremental Load:**

| Approach | How | When to Use | Problem |
|----------|-----|-------------|---------|
| **Full Load** | Re-process all data every run | Small tables, simple | Expensive, slow |
| **Incremental** | Only process new/changed data | Large tables, frequent | Complex, but necessary |

#### **High-Water Mark Pattern**

The most common incremental loading technique.

```python
from pyspark.sql.functions import col, max as spark_max
from delta.tables import DeltaTable

# ── Step 1: Retrieve last processed timestamp ────
def get_high_water_mark(table_path):
    """Get the max processed timestamp from target table."""
    if DeltaTable.isDeltaTable(spark, table_path):
        existing = spark.read.format("delta").load(table_path)
        hwm = existing.select(spark_max("event_time")).collect()[0][0]
        return hwm
    return None  # First run — no watermark yet

# ── Step 2: Load only NEW data ────────────────────
def load_incremental(source_path, target_path):
    hwm = get_high_water_mark(target_path)
    
    # Load source
    source = spark.read.format("delta").load(source_path)
    
    # Filter to only new records
    if hwm is not None:
        print(f"Loading data after: {hwm}")
        new_data = source.filter(col("event_time") > hwm)
    else:
        print("First run — loading all data")
        new_data = source
    
    record_count = new_data.count()
    print(f"New records to process: {record_count}")
    
    if record_count > 0:
        new_data.write.format("delta") \
            .mode("append") \
            .save(target_path)
        print("✅ Load complete")
    else:
        print("⏩ No new data")

# Run the incremental load
load_incremental("/delta/bronze/events", "/delta/silver/events")
```

#### **Control Table Pattern**

For more advanced pipeline management:

```python
# ── Create pipeline control table ─────────────────
spark.sql("""
CREATE TABLE IF NOT EXISTS pipeline_control (
    pipeline_name    STRING,
    source_path      STRING,
    target_path      STRING,
    last_run_time    TIMESTAMP,
    last_hwm         TIMESTAMP,
    records_loaded   LONG,
    status           STRING,
    run_date         DATE
) USING DELTA
""")

# ── Log pipeline run ──────────────────────────────
from pyspark.sql.functions import current_timestamp, current_date, lit

def log_pipeline_run(name, src, tgt, hwm, count, status):
    log_df = spark.createDataFrame([(
        name, src, tgt, None, hwm, count, status, None
    )], ["pipeline_name", "source_path", "target_path", 
        "last_run_time", "last_hwm", "records_loaded", "status", "run_date"]) \
        .withColumn("last_run_time", current_timestamp()) \
        .withColumn("run_date", current_date())
    
    log_df.write.format("delta").mode("append").saveAsTable("pipeline_control")

# Use it:
log_pipeline_run("events_bronze_to_silver", 
                 "/delta/bronze/events", 
                 "/delta/silver/events",
                 "2024-01-15 10:00:00", 
                 5000, "success")
```

---

### 4.4 Auto Loader — Streaming File Ingestion

Databricks Auto Loader efficiently detects and processes new files as they arrive.

```python
# Auto Loader: Automatically picks up new files from S3/ADLS
df = spark.readStream \
    .format("cloudFiles") \
    .option("cloudFiles.format", "json") \
    .option("cloudFiles.schemaLocation", "/checkpoints/events/schema") \
    .load("/raw/incoming/events/")

# Transform and write
query = df \
    .withColumn("_ingested_at", current_timestamp()) \
    .withColumn("_source_file", input_file_name()) \
    .writeStream \
    .format("delta") \
    .outputMode("append") \
    .option("checkpointLocation", "/checkpoints/events/bronze") \
    .trigger(availableNow=True) \
    .start("/delta/bronze/events")

query.awaitTermination()
```

**Why Auto Loader over reading from S3 directly?**
- Tracks which files have been processed (no re-processing)
- Handles millions of files efficiently
- Automatic schema inference and evolution
- Scales with file volume

---

### 4.5 Data Quality — Enforcing Rules

```python
from pyspark.sql.functions import col, when, lit

def run_data_quality_checks(df, table_name):
    """Run DQ checks and return summary."""
    total = df.count()
    
    checks = {
        "null_order_id":    df.filter(col("order_id").isNull()).count(),
        "null_customer_id": df.filter(col("customer_id").isNull()).count(),
        "negative_amount":  df.filter(col("amount") < 0).count(),
        "future_dates":     df.filter(col("order_date") > current_date()).count(),
        "duplicate_orders": total - df.dropDuplicates(["order_id"]).count()
    }
    
    print(f"\n📊 Data Quality Report: {table_name}")
    print(f"   Total records: {total:,}")
    for check, failures in checks.items():
        pct = (failures / total * 100) if total > 0 else 0
        status = "✅" if failures == 0 else "❌"
        print(f"   {status} {check}: {failures:,} failures ({pct:.2f}%)")
    
    return checks

# ── Tag bad records instead of dropping them ──────
df_with_dq = df.withColumn(
    "_dq_flags",
    when(col("order_id").isNull(), "null_order_id")
    .when(col("amount") < 0, "negative_amount")
    .when(col("customer_id").isNull(), "null_customer")
    .otherwise("valid")
)

# Write good records to silver, bad to quarantine
df_with_dq.filter(col("_dq_flags") == "valid") \
    .write.format("delta").mode("append").save("/delta/silver/orders")

df_with_dq.filter(col("_dq_flags") != "valid") \
    .write.format("delta").mode("append").save("/delta/quarantine/orders")
```

---

### 4.6 Full ETL Pipeline — End-to-End Example

```python
from pyspark.sql.functions import *
from delta.tables import DeltaTable

class SalesETLPipeline:
    
    def __init__(self, bronze_path, silver_path, gold_path):
        self.bronze = bronze_path
        self.silver = silver_path
        self.gold   = gold_path
    
    def ingest_bronze(self, source_path):
        """Layer 1: Land raw data."""
        print("📥 Bronze: Ingesting raw data...")
        
        df = spark.read.option("header", True) \
                  .option("inferSchema", True) \
                  .csv(source_path)
        
        df = df \
            .withColumn("_ingested_at", current_timestamp()) \
            .withColumn("_source", lit(source_path))
        
        df.write.format("delta").mode("append").save(self.bronze)
        print(f"   ✅ Ingested {df.count()} rows to bronze")
        return df
    
    def transform_silver(self):
        """Layer 2: Clean, validate, enrich."""
        print("🔄 Silver: Cleaning and validating...")
        
        df = spark.read.format("delta").load(self.bronze)
        
        df = df \
            .dropDuplicates(["order_id"]) \
            .filter(col("order_id").isNotNull()) \
            .withColumn("amount", col("amount").cast("double")) \
            .withColumn("order_date", to_date(col("order_date"))) \
            .withColumn("status", trim(lower(col("status")))) \
            .fillna({"amount": 0.0, "status": "unknown"})
        
        # DQ flag
        df = df.withColumn(
            "_valid",
            (col("amount") >= 0) & 
            (col("order_date").isNotNull()) & 
            (col("order_id").isNotNull())
        )
        
        valid = df.filter(col("_valid"))
        invalid = df.filter(~col("_valid"))
        
        valid.write.format("delta").mode("overwrite").save(self.silver)
        print(f"   ✅ {valid.count()} valid rows to silver")
        print(f"   ⚠️  {invalid.count()} invalid rows quarantined")
    
    def build_gold(self):
        """Layer 3: Business aggregates."""
        print("🏆 Gold: Building business model...")
        
        df = spark.read.format("delta").load(self.silver)
        
        gold = df \
            .filter(col("status") == "completed") \
            .groupBy(
                to_date("order_date").alias("date"),
                "state",
                "product"
            ) \
            .agg(
                count("*").alias("orders"),
                sum("amount").alias("revenue"),
                avg("amount").alias("aov"),
                countDistinct("customer_id").alias("unique_customers")
            )
        
        gold.write.format("delta") \
            .mode("overwrite") \
            .saveAsTable("gold.daily_sales_summary")
        
        print(f"   ✅ Gold table updated")
    
    def run(self, source_path):
        print("🚀 Starting ETL Pipeline...\n")
        self.ingest_bronze(source_path)
        self.transform_silver()
        self.build_gold()
        print("\n✅ Pipeline complete!")

# Run it!
pipeline = SalesETLPipeline(
    bronze_path="/delta/bronze/sales",
    silver_path="/delta/silver/sales",
    gold_path="/delta/gold/sales"
)
pipeline.run("/raw/incoming/sales_20240115.csv")
```

---

## 💻 Hands-On Exercise

**Task**: Build a bronze → silver pipeline for customer data

```python
# Step 1: Generate messy raw data
raw_data = [
    (1, "Alice",   "alice@email.com",  25, "NY",   "2024-01-01"),
    (2, "BOB",     "bob@email.com",    -5, "CA",   "2024-01-02"),   # Bad age
    (None,"Charlie","charlie@email.com",30, "TX",  "2024-01-03"),   # Null id
    (4, "Diana",   "diana@email.com",  35, None,   "2024-01-04"),   # Null state
    (5, "Eve",     "eve@email.com",    28, "FL",   "2099-01-05"),   # Future date
    (6, "Frank",   "frank@email.com",  40, "NY",   "2024-01-06"),
    (6, "Frank",   "frank@email.com",  40, "NY",   "2024-01-06"),   # Duplicate!
]

# TODO: Write bronze → silver pipeline that:
# 1. Lands raw data to bronze with metadata
# 2. Drops duplicates on customer_id
# 3. Filters null customer_id
# 4. Validates age (0-120)
# 5. Validates date (not in future)
# 6. Tags invalid rows, separates them
# 7. Writes valid rows to silver layer
```

---

## ❓ Q&A

### Q1: Should I always use Bronze/Silver/Gold?

**A:** For most data engineering projects, yes. Benefits:
- **Reprocessability**: Bronze always has raw data; re-run silver/gold anytime
- **Debugging**: Easy to find where data went wrong
- **Collaboration**: Clear ownership (ingestion team does bronze, analytics team does gold)

Exception: Very small/simple projects may use a single layer.

---

### Q2: How often should ETL pipelines run?

| Data Freshness Needed | Schedule | Technology |
|----------------------|----------|------------|
| End-of-day reports | Daily batch | Databricks Jobs (cron) |
| Hourly dashboards | Hourly batch | Databricks Jobs |
| Near-real-time | Every 5-15 min | Auto Loader / Structured Streaming |
| Real-time | Continuous | Structured Streaming + Kafka |

---

### Q3: What's the difference between batch and streaming?

```python
# BATCH: Process all available data at scheduled times
df = spark.read.format("delta").load("/data/orders")  # reads snapshot
df.write.format("delta").mode("append").save("/output")

# STREAMING: Process data as it arrives continuously
df = spark.readStream.format("delta").load("/data/orders")  # reads new data
df.writeStream.format("delta") \
  .outputMode("append") \
  .option("checkpointLocation", "/checkpoints/orders") \
  .start("/output")
```

---

### Q4: How do I handle schema changes in source data?

```python
# Option 1: mergeSchema — add new columns
df.write.format("delta") \
    .option("mergeSchema", "true") \
    .mode("append") \
    .save("/delta/silver/orders")

# Option 2: overwriteSchema — replace entire schema
df.write.format("delta") \
    .option("overwriteSchema", "true") \
    .mode("overwrite") \
    .save("/delta/silver/orders")
```

---

### Q5: How do I handle late-arriving data?

```python
# Late arriving data = records for past dates arriving now
# Strategy: Upsert (MERGE) instead of append

from delta.tables import DeltaTable

target = DeltaTable.forPath(spark, "/delta/silver/orders")
source = spark.read.format("delta").load("/raw/late_orders")

target.alias("t").merge(
    source.alias("s"),
    "t.order_id = s.order_id"
).whenMatchedUpdateAll() \
 .whenNotMatchedInsertAll() \
 .execute()
```

---

## 🔑 Key Takeaways

1. **Medallion (Bronze/Silver/Gold)** is the standard Databricks architecture
2. **Bronze**: Never transform raw data — preserve it always
3. **Silver**: Clean, validate, type, deduplicate
4. **Gold**: Business aggregations, BI-ready
5. **Incremental loading** (high-water mark) saves time and money
6. **Auto Loader** for automated file-based ingestion
7. **Tag bad records** — don't just drop them (quarantine layer)
8. **Control tables** track pipeline state for monitoring

---

**Next Session → Day 5: Delta Lake — ACID Transactions, Time Travel & Upserts**
