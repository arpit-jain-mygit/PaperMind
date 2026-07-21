# Scenario-Based Questions & Answers
## ETL, Streaming, Batch, Checkpoints & Production FAQs

> Each scenario includes: the problem, the diagnosis, the solution code, and what to watch out for.

---

## SECTION 1: Build an ETL — Batch Load Scenarios

---

### SCENARIO 1: Build a Full Batch ETL Pipeline from CSV → Delta

**Question**: You receive a CSV file every day at 6 AM in S3. Build a complete batch ETL that reads it, cleans it, and loads it into a Gold Delta table ready for BI by 8 AM.

**Answer:**

```
Architecture:
S3 CSV (daily) → [Notebook 1: Bronze] → [Notebook 2: Silver] → [Notebook 3: Gold]
Orchestrated by: Databricks Job (cron: 0 0 6 * * ?)
```

```python
# ════════════════════════════════════════════════════════
# NOTEBOOK 1 — Bronze: Land raw CSV into Delta
# ════════════════════════════════════════════════════════
import dbutils
from pyspark.sql.functions import current_timestamp, input_file_name, lit

# Parameters passed from Job
batch_date = dbutils.widgets.get("batch_date")   # e.g. "2024-01-15"
env        = dbutils.widgets.get("env")          # e.g. "prod"

SOURCE_PATH = f"s3a://my-bucket/incoming/sales/{batch_date}/"
BRONZE_PATH = f"/delta/{env}/bronze/sales"

# ── Read raw CSV ───────────────────────────────────────
raw = spark.read \
    .option("header", True) \
    .option("inferSchema", False) \         # Never infer in production
    .csv(SOURCE_PATH)

# ── Guard: fail fast if source is empty ───────────────
if raw.count() == 0:
    raise ValueError(f"Source file is EMPTY for date {batch_date}. Stopping.")

# ── Add metadata columns ──────────────────────────────
bronze_df = raw \
    .withColumn("_batch_date",    lit(batch_date)) \
    .withColumn("_ingested_at",   current_timestamp()) \
    .withColumn("_source_file",   input_file_name())

# ── Append to Bronze (never overwrite raw!) ──────────
bronze_df.write \
    .format("delta") \
    .mode("append") \
    .save(BRONZE_PATH)

print(f"✅ Bronze: {bronze_df.count()} rows ingested for {batch_date}")
dbutils.notebook.exit("bronze_success")


# ════════════════════════════════════════════════════════
# NOTEBOOK 2 — Silver: Clean, validate, type, deduplicate
# ════════════════════════════════════════════════════════
from pyspark.sql.functions import col, to_date, trim, lower, when, current_timestamp
from pyspark.sql.types import DoubleType, IntegerType
from delta.tables import DeltaTable

batch_date  = dbutils.widgets.get("batch_date")
BRONZE_PATH = f"/delta/{env}/bronze/sales"
SILVER_PATH = f"/delta/{env}/silver/sales"
QUARANTINE  = f"/delta/{env}/quarantine/sales"

# ── Read only today's bronze batch ────────────────────
bronze = spark.read.format("delta").load(BRONZE_PATH) \
    .filter(col("_batch_date") == batch_date)

# ── Apply explicit types + cleaning ──────────────────
from pyspark.sql.types import StructType, StructField, StringType, DoubleType

silver = bronze \
    .withColumn("order_id",    col("order_id").cast(IntegerType())) \
    .withColumn("customer_id", col("customer_id").cast(IntegerType())) \
    .withColumn("amount",      col("amount").cast(DoubleType())) \
    .withColumn("order_date",  to_date(col("order_date"), "yyyy-MM-dd")) \
    .withColumn("status",      trim(lower(col("status")))) \
    .withColumn("product",     trim(col("product")))

# ── Tag records with DQ status ────────────────────────
silver = silver.withColumn(
    "_dq_status",
    when(col("order_id").isNull(),    "null_order_id")
    .when(col("customer_id").isNull(),"null_customer_id")
    .when(col("amount") <= 0,         "non_positive_amount")
    .when(col("order_date").isNull(), "null_date")
    .when(~col("status").isin(["completed","pending","cancelled"]), "invalid_status")
    .otherwise("valid")
)

valid   = silver.filter(col("_dq_status") == "valid").drop("_dq_status")
invalid = silver.filter(col("_dq_status") != "valid")

# ── Deduplicate valid records ─────────────────────────
valid = valid.dropDuplicates(["order_id"])

# ── UPSERT to Silver (handles late-arriving corrections) ─
if DeltaTable.isDeltaTable(spark, SILVER_PATH):
    DeltaTable.forPath(spark, SILVER_PATH).alias("t") \
        .merge(valid.alias("s"), "t.order_id = s.order_id") \
        .whenMatchedUpdateAll() \
        .whenNotMatchedInsertAll() \
        .execute()
else:
    valid.write.format("delta").partitionBy("order_date").save(SILVER_PATH)

# ── Write invalid records to quarantine ──────────────
if invalid.count() > 0:
    invalid.withColumn("_quarantined_at", current_timestamp()) \
        .write.format("delta").mode("append").save(QUARANTINE)
    print(f"⚠️  {invalid.count()} records quarantined")

print(f"✅ Silver: {valid.count()} valid rows upserted")
dbutils.notebook.exit(f"silver_success:{valid.count()}")


# ════════════════════════════════════════════════════════
# NOTEBOOK 3 — Gold: Business aggregation
# ════════════════════════════════════════════════════════
from pyspark.sql.functions import sum, avg, count, countDistinct

batch_date  = dbutils.widgets.get("batch_date")
SILVER_PATH = f"/delta/{env}/silver/sales"

silver = spark.read.format("delta").load(SILVER_PATH)

daily_gold = silver \
    .filter(col("status") == "completed") \
    .groupBy("order_date", "product") \
    .agg(
        count("order_id").alias("order_count"),
        sum("amount").alias("total_revenue"),
        avg("amount").alias("avg_order_value"),
        countDistinct("customer_id").alias("unique_customers")
    )

# ── Overwrite only today's partition ─────────────────
daily_gold.write.format("delta") \
    .mode("overwrite") \
    .option("replaceWhere", f"order_date = '{batch_date}'") \
    .saveAsTable("prod_catalog.gold.daily_sales")

print(f"✅ Gold: {daily_gold.count()} rows written for {batch_date}")
dbutils.notebook.exit("gold_success")
```

**Databricks Job config (multi-task):**
```json
Tasks:  [bronze_ingest] → [silver_transform] → [gold_aggregate]
Schedule: 0 0 6 * * ?   (6 AM UTC daily)
Cluster: job cluster (4 workers, auto-terminate)
Retries: 2 per task
Alert: email on failure
```

---

### SCENARIO 2: Incremental Batch Load with High-Water Mark

**Question**: A source database table has millions of rows. New rows are added every hour. Build an incremental load that only pulls new rows each run.

**Answer:**

```python
from pyspark.sql.functions import col, max as spark_max, current_timestamp, lit
from delta.tables import DeltaTable
import json

TARGET_PATH  = "/delta/silver/orders"
CONTROL_TABLE = "meta.pipeline_watermarks"

# ── Step 1: Read the last watermark ──────────────────
def get_watermark(pipeline_name):
    """Fetch the last processed timestamp from control table."""
    try:
        wm_df = spark.sql(f"""
            SELECT last_watermark 
            FROM {CONTROL_TABLE}
            WHERE pipeline_name = '{pipeline_name}'
        """)
        if wm_df.count() > 0:
            return wm_df.collect()[0][0]
    except Exception:
        pass
    return None   # First run — no watermark

# ── Step 2: Pull only new records ────────────────────
def load_incremental(pipeline_name, source_table, watermark_col):
    last_wm = get_watermark(pipeline_name)

    source = spark.table(source_table)

    if last_wm:
        print(f"Pulling data after: {last_wm}")
        new_data = source.filter(col(watermark_col) > last_wm)
    else:
        print("First run — pulling all data")
        new_data = source

    count = new_data.count()
    print(f"New rows to process: {count:,}")

    if count == 0:
        print("Nothing to do — exiting")
        return 0

    # ── Step 3: Write new data ────────────────────────
    if DeltaTable.isDeltaTable(spark, TARGET_PATH):
        DeltaTable.forPath(spark, TARGET_PATH).alias("t") \
            .merge(new_data.alias("s"), "t.order_id = s.order_id") \
            .whenMatchedUpdateAll() \
            .whenNotMatchedInsertAll() \
            .execute()
    else:
        new_data.write.format("delta").save(TARGET_PATH)

    # ── Step 4: Advance the watermark ────────────────
    new_wm = new_data.select(spark_max(watermark_col)).collect()[0][0]
    update_watermark(pipeline_name, new_wm)
    print(f"✅ Watermark advanced to: {new_wm}")
    return count

def update_watermark(pipeline_name, new_wm):
    spark.sql(f"""
        CREATE TABLE IF NOT EXISTS {CONTROL_TABLE} (
            pipeline_name   STRING,
            last_watermark  TIMESTAMP,
            updated_at      TIMESTAMP
        ) USING DELTA
    """)
    # Upsert watermark record
    update_df = spark.createDataFrame(
        [(pipeline_name, new_wm)], ["pipeline_name", "last_watermark"]
    ).withColumn("updated_at", current_timestamp())

    ct = DeltaTable.forName(spark, CONTROL_TABLE)
    ct.alias("t").merge(update_df.alias("s"), "t.pipeline_name = s.pipeline_name") \
        .whenMatchedUpdateAll() \
        .whenNotMatchedInsertAll() \
        .execute()

# Run it
load_incremental("orders_pipeline", "source_db.orders", "updated_at")
```

---

### SCENARIO 3: Full Load with Truncate and Reload (Small Reference Tables)

**Question**: A small product lookup table has ~5,000 rows and can change any row at any time. Build a daily full reload.

**Answer:**

```python
# Full load is fine for small tables where all rows can change
from pyspark.sql.functions import current_timestamp

SOURCE_TABLE  = "source_db.products"
TARGET_TABLE  = "prod_catalog.silver.products"

# ── Read full source ──────────────────────────────────
products = spark.table(SOURCE_TABLE) \
    .withColumn("_loaded_at", current_timestamp())

count = products.count()
print(f"Loading {count} products from source")

# ── Overwrite entire table ────────────────────────────
# Safe for small tables — completes in seconds
products.write.format("delta") \
    .mode("overwrite") \
    .option("overwriteSchema", "true") \
    .saveAsTable(TARGET_TABLE)

print(f"✅ Full reload complete: {count} rows")

# ── Verify row count matches ──────────────────────────
target_count = spark.table(TARGET_TABLE).count()
assert target_count == count, f"MISMATCH: source={count}, target={target_count}"
print("✅ Count validation passed")
```

---

## SECTION 2: Build an ETL — Streaming Scenarios

---

### SCENARIO 4: Build a Streaming ETL (Auto Loader → Bronze → Silver)

**Question**: Files land in S3 every few minutes. Build a streaming pipeline that automatically picks them up, validates them, and writes to Silver — continuously.

**Answer:**

```python
# ════════════════════════════════════════════════════════
# STREAMING PIPELINE: Auto Loader → Bronze → Silver
# ════════════════════════════════════════════════════════
from pyspark.sql.functions import col, current_timestamp, input_file_name, when
from pyspark.sql.types import StructType, StructField, StringType, DoubleType, TimestampType

# ── Define schema explicitly (required for streaming) ─
schema = StructType([
    StructField("order_id",    StringType(),    False),
    StructField("customer_id", StringType(),    True),
    StructField("product",     StringType(),    True),
    StructField("amount",      DoubleType(),    True),
    StructField("event_time",  TimestampType(), True),
    StructField("status",      StringType(),    True),
])

BRONZE_PATH     = "/delta/streaming/bronze/orders"
SILVER_PATH     = "/delta/streaming/silver/orders"
BRONZE_CKPT     = "/checkpoints/orders/bronze"
SILVER_CKPT     = "/checkpoints/orders/silver"
SCHEMA_LOCATION = "/checkpoints/orders/schema"

# ── STREAM 1: Auto Loader → Bronze ────────────────────
bronze_stream = spark.readStream \
    .format("cloudFiles") \
    .option("cloudFiles.format", "json") \
    .option("cloudFiles.schemaLocation", SCHEMA_LOCATION) \
    .schema(schema) \
    .load("s3a://my-bucket/incoming/orders/") \
    .withColumn("_ingested_at", current_timestamp()) \
    .withColumn("_source_file", input_file_name())

bronze_query = bronze_stream.writeStream \
    .format("delta") \
    .outputMode("append") \
    .option("checkpointLocation", BRONZE_CKPT) \
    .trigger(processingTime="2 minutes") \   # micro-batch every 2 min
    .start(BRONZE_PATH)


# ── STREAM 2: Bronze → Silver (read Delta as stream) ──
silver_stream = spark.readStream \
    .format("delta") \
    .load(BRONZE_PATH) \
    .withWatermark("event_time", "10 minutes") \   # tolerate 10-min late data
    .filter(col("order_id").isNotNull()) \
    .filter(col("amount") > 0) \
    .withColumn("amount", col("amount").cast(DoubleType())) \
    .withColumn("_processed_at", current_timestamp())

silver_query = silver_stream.writeStream \
    .format("delta") \
    .outputMode("append") \
    .option("checkpointLocation", SILVER_CKPT) \
    .trigger(processingTime="2 minutes") \
    .start(SILVER_PATH)

# ── Wait for both streams ─────────────────────────────
bronze_query.awaitTermination()
silver_query.awaitTermination()
```

**Key design decisions explained:**

| Decision | Why |
|----------|-----|
| `cloudFiles` (Auto Loader) | Tracks which files processed — no duplicates |
| Explicit schema | Streaming fails without schema on file sources |
| `checkpointLocation` | Enables exactly-once processing on restart |
| `processingTime="2 minutes"` | Micro-batch cadence — balance latency vs cost |
| `withWatermark("event_time", "10 minutes")` | Handle late-arriving data gracefully |
| Two separate streams | Decoupled — Bronze failure doesn't break Silver |

---

### SCENARIO 5: Streaming with Kafka as Source

**Question**: Events from Kafka need to be ingested, parsed, and written to Delta. Build the pipeline.

**Answer:**

```python
from pyspark.sql.functions import from_json, col, current_timestamp
from pyspark.sql.types import StructType, StructField, StringType, DoubleType, LongType

# ── Kafka event schema ────────────────────────────────
event_schema = StructType([
    StructField("event_id",   StringType(), True),
    StructField("user_id",    LongType(),   True),
    StructField("action",     StringType(), True),
    StructField("amount",     DoubleType(), True),
    StructField("event_time", LongType(),   True),   # Unix timestamp
    StructField("page",       StringType(), True),
])

# ── Read from Kafka ───────────────────────────────────
kafka_df = spark.readStream \
    .format("kafka") \
    .option("kafka.bootstrap.servers", dbutils.secrets.get("kafka", "bootstrap-servers")) \
    .option("subscribe", "user-events") \
    .option("startingOffsets", "latest") \
    .option("maxOffsetsPerTrigger", 100_000) \    # Rate limiting
    .option("failOnDataLoss", "false") \          # Handle Kafka retention expiry
    .load()

# ── Parse Kafka value (JSON bytes → struct) ───────────
parsed = kafka_df \
    .select(
        col("offset"),
        col("partition"),
        col("timestamp").alias("kafka_timestamp"),
        from_json(col("value").cast("string"), event_schema).alias("data")
    ) \
    .select("offset", "partition", "kafka_timestamp", "data.*") \
    .withColumn("event_time", (col("event_time") / 1000).cast("timestamp")) \
    .withColumn("_ingested_at", current_timestamp())

# ── Write to Delta Bronze ─────────────────────────────
query = parsed.writeStream \
    .format("delta") \
    .outputMode("append") \
    .option("checkpointLocation", "/checkpoints/kafka/events/bronze") \
    .trigger(processingTime="1 minute") \
    .start("/delta/bronze/events")

query.awaitTermination()
```

---

### SCENARIO 6: Streaming Aggregation (Sliding Window)

**Question**: Build a streaming pipeline that counts events per user in a 10-minute sliding window for real-time fraud detection.

**Answer:**

```python
from pyspark.sql.functions import window, count, sum, col

# Assumes `events` is already a streaming DataFrame with event_time column

# ── Add watermark + window aggregation ────────────────
fraud_signals = events \
    .withWatermark("event_time", "5 minutes") \  # Late data tolerance
    .groupBy(
        window(col("event_time"), "10 minutes", "5 minutes"),  # 10-min window, slides every 5 min
        col("user_id")
    ) \
    .agg(
        count("event_id").alias("event_count"),
        sum("amount").alias("total_amount")
    ) \
    .filter(
        (col("event_count") > 10) |         # > 10 events in 10 min
        (col("total_amount") > 5000)        # OR > $5000 in 10 min
    ) \
    .select(
        col("window.start").alias("window_start"),
        col("window.end").alias("window_end"),
        col("user_id"),
        col("event_count"),
        col("total_amount")
    )

# ── Write fraud signals ───────────────────────────────
fraud_query = fraud_signals.writeStream \
    .format("delta") \
    .outputMode("append") \         # Use append with watermark
    .option("checkpointLocation", "/checkpoints/fraud/signals") \
    .trigger(processingTime="1 minute") \
    .start("/delta/gold/fraud_signals")

fraud_query.awaitTermination()
```

**Important**: Window aggregations in streaming REQUIRE `withWatermark`. Without it, Spark must keep state forever → memory grows unbounded → eventually OOM.

---

### SCENARIO 7: Batch + Streaming in the Same Job (Lambda Architecture)

**Question**: How do you build a pipeline where you need BOTH historical backfill (batch) AND real-time processing (streaming)?

**Answer:**

```python
# ── Pattern: Use the same logic for both ──────────────
# The trick: same transformation function, different read mode

def transform_orders(df):
    """Works identically for batch and streaming DataFrames."""
    from pyspark.sql.functions import col, to_timestamp, when
    return df \
        .filter(col("order_id").isNotNull()) \
        .withColumn("amount", col("amount").cast("double")) \
        .withColumn("order_time", to_timestamp("order_time")) \
        .withColumn("tier",
            when(col("amount") >= 1000, "high")
            .when(col("amount") >= 500,  "mid")
            .otherwise("low")
        )

# ── BATCH MODE: backfill 6 months of history ──────────
def run_batch():
    df = spark.read.format("delta").load("/delta/bronze/orders")
    clean = transform_orders(df)
    clean.write.format("delta") \
        .mode("overwrite") \
        .save("/delta/silver/orders")
    print("Batch backfill complete")

# ── STREAMING MODE: real-time going forward ────────────
def run_streaming():
    stream = spark.readStream.format("delta").load("/delta/bronze/orders")
    clean_stream = transform_orders(stream)
    clean_stream.writeStream \
        .format("delta") \
        .outputMode("append") \
        .option("checkpointLocation", "/checkpoints/orders/silver") \
        .trigger(processingTime="5 minutes") \
        .start("/delta/silver/orders") \
        .awaitTermination()

# ── Switch via widget ─────────────────────────────────
mode = dbutils.widgets.get("mode")  # "batch" or "streaming"
if mode == "batch":
    run_batch()
else:
    run_streaming()
```

---

## SECTION 3: Checkpoint Failures — What Happens & How to Fix

---

### SCENARIO 8: Checkpoint Not Recovering — What Happens?

**Question**: My streaming job restarted but it's reprocessing old data / starting from the beginning instead of resuming. What went wrong?

**Answer — Root Causes:**

```
CAUSE 1: Checkpoint directory was deleted or not specified
CAUSE 2: Checkpoint path changed in code (new path = new stream)
CAUSE 3: Schema changed and checkpoint is incompatible
CAUSE 4: Checkpoint is on DBFS but cluster was different workspace
CAUSE 5: Checkpoint is corrupted (partial write during crash)
```

**Diagnosis steps:**

```python
# Step 1: Check if checkpoint exists
dbutils.fs.ls("/checkpoints/my-stream/")
# If FileNotFoundException → checkpoint was deleted or wrong path

# Step 2: Check checkpoint contents
dbutils.fs.ls("/checkpoints/my-stream/")
# You should see: commits/, offsets/, metadata, sources/, state/

# Step 3: Check the last committed offset
checkpoint_metadata = dbutils.fs.head("/checkpoints/my-stream/metadata")
print(checkpoint_metadata)
# Should show streaming query ID

# Step 4: Read last offset
import json
last_offset = dbutils.fs.head("/checkpoints/my-stream/offsets/0")
print(json.loads(last_offset))
# For Kafka: shows partition → last offset per topic
# For Auto Loader: shows which files have been processed
```

**Fixes by root cause:**

```python
# ── FIX 1: Checkpoint deleted — stream starts fresh ───
# Prevention: Always use persistent, versioned checkpoint paths
# BAD:
.option("checkpointLocation", "/tmp/ckpt")     # temp, gets deleted

# GOOD:
.option("checkpointLocation", "/checkpoints/prod/orders/v1")  # persistent, versioned

# ── FIX 2: Path changed in code ────────────────────────
# OLD: .option("checkpointLocation", "/checkpoints/orders")
# NEW: .option("checkpointLocation", "/checkpoints/orders_v2")  # NEW = fresh start!
# Solution: Keep the SAME path OR delete old checkpoint intentionally if you want fresh

# ── FIX 3: Schema changed (incompatible checkpoint) ───
# Error: "Detected a data schema change which is incompatible"
# Option A: Delete checkpoint and restart fresh (loses offset position)
dbutils.fs.rm("/checkpoints/orders/", recurse=True)

# Option B: Enable schema evolution in Auto Loader
spark.readStream \
    .format("cloudFiles") \
    .option("cloudFiles.schemaEvolutionMode", "addNewColumns") \
    .option("cloudFiles.schemaLocation", "/checkpoints/orders/schema") \
    .load("/raw/orders/")

# ── FIX 4: Corrupt checkpoint ─────────────────────────
# Symptom: Stream fails with CorruptCheckpointException
# Check: dbutils.fs.ls("/checkpoints/orders/commits/")
# If latest commit file is 0 bytes or missing → corrupted

# Fix: Roll back to last valid commit
# Find last valid commit number:
commits = dbutils.fs.ls("/checkpoints/orders/commits/")
valid_commits = [f for f in commits if spark.read.text(f.path).count() > 0]
last_valid = sorted(valid_commits, key=lambda x: x.name)[-1]
print(f"Last valid commit: {last_valid.name}")

# Then manually delete all commits AFTER the last valid one
# (Advanced — usually easier to delete entire checkpoint and restart)

# ── FIX 5: Kafka offset out of range ──────────────────
# Error: "OffsetOutOfRangeException" — Kafka deleted data before stream read it
.option("failOnDataLoss", "false")   # Add this to skip missing offsets
# OR change startingOffsets to "latest" to skip to current

# ── FIX 6: Force restart from specific Kafka offset ──
.option("startingOffsets", json.dumps({
    "my-topic": {"0": 12345, "1": 67890}   # Partition → offset to start from
}))
```

---

### SCENARIO 9: Streaming Job Runs but Never Commits (Stuck)

**Question**: The streaming job is running, no errors, but data is not appearing in the target Delta table. No new commits in the checkpoint.

**Answer:**

```python
# ── Diagnose a stuck stream ────────────────────────────

# Step 1: Check stream status in notebook
query.status
# Output: {"message": "Processing new data", "isDataAvailable": True, "isTriggerActive": True}
# Stuck indicator: {"message": "Waiting for data to arrive"}  ← no new data

# Step 2: Check recent progress
query.lastProgress
# Look at:
# - "numInputRows": 0 → no data being processed
# - "inputRowsPerSecond": 0 → stuck
# - "durationMs": {"addBatch": 999999} → batch taking too long

# Step 3: Check if source has data
spark.read.format("delta").load("/delta/bronze/orders") \
    .filter(col("_ingested_at") > "2024-01-15 10:00:00") \
    .count()
# If 0 → source is empty (expected)
# If large → stream is not reading it (checkpoint issue or backpressure)

# ── Common fixes ──────────────────────────────────────

# Fix A: Stream is behind — increase trigger interval
.trigger(processingTime="30 seconds")  # Process more frequently

# Fix B: Single partition bottleneck — repartition inside writeStream
silver_stream.repartition(20).writeStream...

# Fix C: Watermark is too aggressive — data held in state
# If you see "Waiting for late data" → watermark too strict
.withWatermark("event_time", "1 hour")  # Increase tolerance

# Fix D: Max offsets too low (Kafka) — increase rate
.option("maxOffsetsPerTrigger", 500_000)   # Allow more per batch
```

---

### SCENARIO 10: Checkpoint Recovery After Schema Change

**Question**: You added a new column to your source data. Now your streaming job fails on restart with a schema incompatibility error.

**Answer:**

```python
# Error: "AnalysisException: Detected incompatible schema change"
# OR: "StreamingQueryException: Schema of Delta table ... changed"

# ════ OPTION 1: Allow schema evolution (preferred) ════
# In Auto Loader:
spark.readStream \
    .format("cloudFiles") \
    .option("cloudFiles.schemaEvolutionMode", "addNewColumns")  # Auto-add new cols
    .option("cloudFiles.schemaLocation", "/checkpoints/orders/schema") \
    .load("/raw/orders/")

# For Delta source:
spark.readStream \
    .format("delta") \
    .option("ignoreChanges", "true") \     # Tolerate schema changes
    .load("/delta/bronze/orders")

# ════ OPTION 2: New schema = new checkpoint ═══════════
# Step 1: Stop the old stream
# Step 2: Rename/archive old checkpoint
dbutils.fs.mv(
    "/checkpoints/orders/silver",
    "/checkpoints/orders/silver_v1_archived"
)
# Step 3: Create new checkpoint path
# Step 4: Restart stream — it starts fresh but with new schema
spark.readStream.format("delta").load("/delta/bronze/orders") \
    .writeStream \
    .option("checkpointLocation", "/checkpoints/orders/silver_v2") \  # NEW
    ...

# ════ OPTION 3: Handle schema explicitly ══════════════
# Instead of relying on schema inference, always cast explicitly
def safe_parse(df):
    """Select only expected columns — ignores new or removed ones."""
    expected = ["order_id", "customer_id", "amount", "status", "order_time"]
    return df.select(*[col(c) for c in expected if c in df.columns])
```

---

## SECTION 4: Common Production FAQs

---

### SCENARIO 11: My Spark Job Runs Fine in Dev but Fails in Prod

**Question**: A notebook works perfectly in development (small data), but fails in production with large data. What do you check?

**Answer — Checklist:**

```python
# ── 1. OOM (Out of Memory) ────────────────────────────
# Error: "java.lang.OutOfMemoryError" or "Container killed"

# Dev: 100K rows — fits in driver memory
# Prod: 100M rows — collect() crashes driver

# Find: look for collect(), toPandas(), broadcast() on large tables
# Fix: replace collect() with write(), increase driver memory or remove broadcast

# ── 2. Data skew at scale ─────────────────────────────
# Dev: 100K rows — even if skewed, still fast
# Prod: 100M rows — one partition takes 10× longer

# Fix:
spark.conf.set("spark.sql.adaptive.skewJoin.enabled", "true")

# ── 3. Wrong shuffle partitions ──────────────────────
# Dev: default 200 works fine for small data
# Prod: 200 partitions for 500GB = each partition is 2.5GB → OOM

# Fix:
spark.conf.set("spark.sql.shuffle.partitions", "auto")  # AQE handles it
# OR manually: total_data_gb * 8 = approximate shuffle partitions

# ── 4. inferSchema is slow at scale ──────────────────
# Dev: 1K rows → inferSchema takes 1 sec
# Prod: 100M rows → inferSchema reads entire dataset twice → 30 min

# Fix: Always define schemas explicitly in production

# ── 5. Missing null handling ──────────────────────────
# Dev: clean sample data, no nulls
# Prod: real data has nulls → NullPointerException in UDF

# Fix: always handle nulls in UDFs
@udf(StringType())
def safe_upper(s):
    return s.upper() if s is not None else None   # ← guard for null
```

---

### SCENARIO 12: Data Duplication in the Target Table

**Question**: After running the pipeline several times, you notice duplicate rows in the Silver table. How do you prevent and fix this?

**Answer:**

```python
# ── Root causes of duplicates ─────────────────────────
# 1. append mode + job ran twice (retry after partial success)
# 2. Source system sent the same record multiple times
# 3. No deduplication before writing
# 4. Late-arriving data inserted as new rows

# ── Prevention: Use MERGE instead of APPEND ───────────
from delta.tables import DeltaTable

target = DeltaTable.forPath(spark, "/delta/silver/orders")

new_data = spark.read.format("delta").load("/delta/bronze/orders")

target.alias("t").merge(
    new_data.alias("s"),
    "t.order_id = s.order_id"   # Dedupe on natural key
) \
.whenMatchedUpdateAll() \
.whenNotMatchedInsertAll() \
.execute()

# ── Prevention: Deduplicate before writing ────────────
deduped = new_data.dropDuplicates(["order_id"])
deduped.write.format("delta").mode("append").save("/delta/silver/orders")

# ── Fix existing duplicates in the table ──────────────
from pyspark.sql.functions import row_number
from pyspark.sql.window import Window

dirty = spark.read.format("delta").load("/delta/silver/orders")

# Keep only the latest version of each order_id
w = Window.partitionBy("order_id").orderBy(col("_ingested_at").desc())

clean = dirty.withColumn("rn", row_number().over(w)) \
    .filter(col("rn") == 1) \
    .drop("rn")

# Overwrite the table with deduped version
clean.write.format("delta") \
    .mode("overwrite") \
    .option("overwriteSchema", "true") \
    .save("/delta/silver/orders")

print(f"Before: {dirty.count()}, After: {clean.count()}")
```

---

### SCENARIO 13: Delta Table OPTIMIZE is Taking Too Long

**Question**: `OPTIMIZE` on your Delta table runs for 4+ hours. How do you speed it up?

**Answer:**

```python
# ── Diagnose first ────────────────────────────────────
spark.sql("DESCRIBE DETAIL my_catalog.silver.orders").show(truncate=False)
# Check: numFiles — if > 100,000 small files → OPTIMIZE will be slow

# ── Fix 1: Partition OPTIMIZE (don't optimize all at once) ─
# Run OPTIMIZE on one partition at a time
spark.sql("""
    OPTIMIZE my_catalog.silver.orders
    WHERE order_date = '2024-01-15'   -- Only one partition
""")

# In a loop for backfill:
from datetime import date, timedelta

start_date = date(2024, 1, 1)
end_date   = date(2024, 1, 31)
d = start_date
while d <= end_date:
    print(f"Optimizing {d}...")
    spark.sql(f"""
        OPTIMIZE my_catalog.silver.orders
        WHERE order_date = '{d}'
    """)
    d += timedelta(days=1)

# ── Fix 2: Enable Auto Optimize on write ──────────────
# Prevents small files from accumulating in the first place!
spark.sql("""
    ALTER TABLE my_catalog.silver.orders
    SET TBLPROPERTIES (
        'delta.autoOptimize.optimizeWrite' = 'true',
        'delta.autoOptimize.autoCompact'   = 'true'
    )
""")

# With autoOptimize:
# - optimizeWrite: coalesces small files during write (prevents small file problem)
# - autoCompact: runs light compaction after each write automatically

# ── Fix 3: Use Liquid Clustering (modern approach) ────
# If creating a new table, use Liquid Clustering instead of partitioning
# No need to run OPTIMIZE manually!
spark.sql("""
    CREATE TABLE my_catalog.silver.orders_v2
    CLUSTER BY (customer_id, order_date)
    USING DELTA
    AS SELECT * FROM my_catalog.silver.orders
""")
```

---

### SCENARIO 14: Source File Arrives Late — Pipeline Already Ran

**Question**: Your daily pipeline ran at 6 AM. A source file that should've been in yesterday's batch arrived at 9 AM (late). How do you handle it?

**Answer:**

```python
# ── Strategy 1: Rerun pipeline for that date ──────────
# Only works if pipeline is idempotent (uses replaceWhere or MERGE)

# Trigger manual re-run:
dbutils.notebook.run(
    "/pipelines/daily_sales",
    timeout_seconds=3600,
    arguments={"batch_date": "2024-01-14"}  # Yesterday's date
)
# Because Silver uses MERGE and Gold uses replaceWhere → safe to rerun

# ── Strategy 2: Detect late files automatically ────────
# Check if files arrived after expected cutoff

from datetime import datetime

def check_for_late_arrivals(source_path, cutoff_hour=6):
    files = dbutils.fs.ls(source_path)
    late_files = []
    cutoff_today = datetime.now().replace(hour=cutoff_hour, minute=0, second=0)

    for f in files:
        file_time = datetime.fromtimestamp(f.modificationTime / 1000)
        if file_time > cutoff_today:
            late_files.append((f.path, file_time))

    return late_files

late = check_for_late_arrivals("/raw/incoming/sales/2024-01-14/")
if late:
    print(f"⚠️ Late files detected: {late}")
    # Trigger re-run
    dbutils.notebook.run("/pipelines/daily_sales", 3600,
                         {"batch_date": "2024-01-14"})

# ── Strategy 3: Streaming handles it naturally ────────
# If using Auto Loader or Structured Streaming:
# → It picks up ALL new files, regardless of when they arrive
# → Late files processed in next micro-batch automatically
# → No manual re-run needed!

# This is one key reason to prefer streaming over batch for critical pipelines
```

---

### SCENARIO 15: "Job Failed with No Error Message" — How to Debug

**Question**: A Databricks Job shows status = FAILED but there's no clear error message. How do you debug it?

**Answer:**

```python
# ── Step 1: Check Job Run details ─────────────────────
# Workflows → Jobs → select job → click failed run
# Click the failed TASK (not just the job)
# Look at: Output tab → Error tab → Logs tab

# ── Step 2: Check driver logs ─────────────────────────
# In the failed task: click "Driver Logs"
# Look at: log4j-active.log (most detailed)
# Search for: "ERROR", "Exception", "OOM", "FAILED"

# ── Step 3: Check Spark UI ────────────────────────────
# In failed task: click "Spark UI" link
# Jobs tab → find failed job → failed stage → failed task
# Click task → look at "Error Message"

# ── Step 4: Add explicit logging to your notebook ─────
import traceback

try:
    result = run_heavy_transformation(df)
except Exception as e:
    print(f"ERROR TYPE: {type(e).__name__}")
    print(f"ERROR MSG:  {str(e)}")
    print(f"TRACEBACK:\n{traceback.format_exc()}")
    raise   # Re-raise so job shows as failed

# ── Step 5: Check cluster events ─────────────────────
# Compute → Clusters → find the job cluster → Event Log
# Look for: OOM kills, spot instance preemptions, disk full

# ── Common silent failure causes ─────────────────────
"""
1. Spot instance was preempted (AWS/Azure took the node back)
   Fix: use on-demand instances for critical production jobs

2. Cluster ran out of disk space
   Fix: increase disk size, clean up /tmp/ in init scripts

3. Python package version conflict
   Fix: pin package versions in cluster libraries

4. Job timed out (cluster timeout ≠ task timeout)
   Fix: increase task timeout in Job settings

5. Notebook called dbutils.notebook.exit() before all cells ran
   Fix: only call exit() at the very end, or use try/finally
"""
```

---

### SCENARIO 16: Pipeline Works But Results Are Wrong (Silent Bug)

**Question**: The pipeline runs successfully every day, but a business analyst notices numbers are off. No error was raised. How do you track down silent data bugs?

**Answer:**

```python
# Silent bugs are the hardest! Build in proactive checks.

# ── Add data reconciliation after every write ──────────
def reconcile(source_df, target_table, key_metric_col, context_label):
    """Compare source vs target to catch silent bugs."""
    source_count = source_df.count()
    source_sum   = source_df.select(sum(key_metric_col)).collect()[0][0]

    target = spark.table(target_table)
    target_count = target.count()
    target_sum   = target.select(sum(key_metric_col)).collect()[0][0]

    count_match = source_count == target_count
    sum_match   = abs(source_sum - target_sum) / (source_sum or 1) < 0.0001  # 0.01% tolerance

    print(f"\n🔍 Reconciliation: {context_label}")
    print(f"   Source count: {source_count:,}  | Target count: {target_count:,}  → {'✅' if count_match else '❌'}")
    print(f"   Source sum:   {source_sum:,.2f} | Target sum:   {target_sum:,.2f} → {'✅' if sum_match else '❌'}")

    if not (count_match and sum_match):
        raise AssertionError(f"Reconciliation FAILED for {context_label}")

# ── Embed in your pipeline ────────────────────────────
silver_df = transform(bronze_df)
silver_df.write.format("delta").mode("overwrite").saveAsTable("silver.orders")
reconcile(bronze_df, "silver.orders", "amount", "bronze → silver (2024-01-15)")

# ── Track daily KPIs in a metrics table ──────────────
def log_pipeline_metrics(date, table, metric_col):
    """Log daily metrics for trend monitoring."""
    df = spark.table(table).filter(col("order_date") == date)
    metrics = {
        "pipeline_date": date,
        "table":         table,
        "row_count":     df.count(),
        "total_revenue": df.select(sum(metric_col)).collect()[0][0],
        "logged_at":     datetime.now().isoformat()
    }
    spark.createDataFrame([metrics]) \
        .write.format("delta").mode("append") \
        .saveAsTable("meta.daily_pipeline_metrics")

# Now you can run this SQL to detect anomalies:
spark.sql("""
    SELECT
        pipeline_date,
        row_count,
        total_revenue,
        LAG(row_count) OVER (ORDER BY pipeline_date) AS prev_count,
        row_count / NULLIF(LAG(row_count) OVER (ORDER BY pipeline_date), 0) AS count_ratio
    FROM meta.daily_pipeline_metrics
    WHERE table = 'gold.daily_sales'
    ORDER BY pipeline_date DESC
""").show()
-- Ratio far from 1.0 = anomaly worth investigating
```

---

## SECTION 5: Quick-Fire FAQs

---

**Q: What is the difference between `trigger(once=True)` and `trigger(availableNow=True)`?**

`once=True` (deprecated) processes one micro-batch and stops. `availableNow=True` (Spark 3.3+, recommended) processes ALL currently available data in one or more micro-batches until caught up, then stops. `availableNow` is faster and handles large backlogs better.

---

**Q: Why does my streaming job consume memory over time until it crashes?**

You're likely using stateful operations (window aggregations, `groupBy`, `dropDuplicates`) without `withWatermark`. Without a watermark, Spark holds ALL state forever. Add a watermark to tell Spark when it's safe to drop old state:
```python
df.withWatermark("event_time", "1 hour")
```

---

**Q: How do I run a streaming query for a fixed time period (not forever)?**

```python
query = df.writeStream.format("delta") \
    .option("checkpointLocation", "/ckpt") \
    .trigger(availableNow=True) \     # Process current backlog and stop
    .start("/output")
query.awaitTermination()              # Blocks until done
```

---

**Q: Can I join two streaming DataFrames?**

Yes, with limitations. Both must have a watermark and the join condition must be time-bounded:
```python
# Stream-stream join (both sides must have watermark)
events.withWatermark("event_time", "1 hour") \
    .join(
        orders.withWatermark("order_time", "1 hour"),
        expr("""
            events.order_id = orders.order_id AND
            events.event_time BETWEEN orders.order_time AND orders.order_time + INTERVAL 1 HOUR
        """),
        how="inner"
    )
```
Simpler: join a stream with a static Delta table (no watermark needed on static side).

---

**Q: My pipeline writes thousands of small files every run. How do I fix it permanently?**

```python
# Enable optimized writes on the table (one-time setup):
spark.sql("""
    ALTER TABLE my_table
    SET TBLPROPERTIES (
        'delta.autoOptimize.optimizeWrite' = 'true',
        'delta.autoOptimize.autoCompact' = 'true'
    )
""")

# Or coalesce before writing (reduces files but may be slower):
df.coalesce(10).write.format("delta").mode("append").saveAsTable("my_table")
```

---

**Q: What's the right way to read a Delta table that another job is actively writing to?**

Delta Lake's ACID guarantees mean readers always see a consistent snapshot:
```python
# Safe — always sees a complete committed version
df = spark.read.format("delta").load("/delta/orders")
# You will NEVER see a partially written batch
```

---

**Q: How do I find which query/user is causing a large shuffle?**

In Spark UI → SQL tab → find the query → look for "Exchange" nodes in the plan. Each Exchange = a shuffle. The size shown tells you how much data moved.

---

**Q: What happens if I forget `checkpointLocation` in a streaming query?**

The stream will run but progress is not saved. On restart, the stream starts from `startingOffsets` (usually the beginning or latest). For Kafka, you'll reprocess old data. For file sources, you'll reprocess all files. Always set `checkpointLocation`.

---

**Q: How do I reprocess historical data while keeping the streaming job running?**

```python
# Use two separate jobs with the same logic:
# Job 1: Streaming (continuous, handles new data)
# Job 2: Batch backfill (one-time, processes historical partition by partition)

# Both write to the same Silver table using MERGE → no conflicts
# Delta Lake's ACID ensures reads always see consistent data
```

---

**Q: Can I use Python `multiprocessing` or `threading` with Spark?**

Don't use Python multiprocessing with Spark — Spark is already distributed. Use threading ONLY for running multiple independent streaming queries in the same notebook:
```python
import threading

def run_stream_1(): stream1.awaitTermination()
def run_stream_2(): stream2.awaitTermination()

t1 = threading.Thread(target=run_stream_1)
t2 = threading.Thread(target=run_stream_2)
t1.start(); t2.start()
t1.join();  t2.join()
```

---

*Last updated: Databricks Training — Scenario Q&A Supplement*
*Use with: 11_COMPREHENSIVE_QA.md for full topic coverage*
