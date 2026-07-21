# Day 10: Capstone Project & Production Best Practices
## End-to-End Pipeline + Production Checklist

**Duration**: 1 hour | **Difficulty**: Advanced

---

## 🎯 Learning Objectives

- ✅ Build a complete end-to-end production pipeline
- ✅ Apply all 9 days of learning in one project
- ✅ Follow a production-readiness checklist
- ✅ Understand team workflows and code standards
- ✅ Know what to do when things go wrong in production

---

## 📚 Content

### 10.1 What Makes a Pipeline "Production-Ready"?

```
NOTEBOOK (POC)              PRODUCTION PIPELINE
──────────────────────────────────────────────────
Hardcoded paths          → Parameters / configs
No error handling        → Try/catch + alerts
Manual run               → Scheduled Job
No logging               → Structured logging
No validation            → Data quality checks
Single user tested        → Code-reviewed + tested
No monitoring            → Alerts + SLAs
Shared cluster           → Job cluster
No documentation         → Inline docs + README
```

---

### 10.2 Project Structure — How to Organize Code

```
my_data_project/
├── README.md                    # Project overview
├── configs/
│   ├── dev.json                 # Dev environment config
│   ├── staging.json             # Staging config
│   └── prod.json                # Prod config
├── notebooks/
│   ├── pipelines/
│   │   ├── 01_bronze_ingest.py
│   │   ├── 02_silver_transform.py
│   │   └── 03_gold_aggregate.py
│   ├── utils/
│   │   ├── logging_utils.py
│   │   ├── dq_utils.py          # Data quality helpers
│   │   └── spark_utils.py
│   └── tests/
│       ├── test_transform.py
│       └── test_dq.py
└── jobs/
    └── daily_sales_pipeline.json  # Job definition
```

---

### 10.3 Configuration Management

```python
# ── configs/dev.json ──────────────────────────────
{
    "environment": "dev",
    "bronze_path": "/dev/delta/bronze",
    "silver_path": "/dev/delta/silver",
    "gold_catalog": "dev_catalog",
    "gold_schema":  "gold",
    "source_path":  "/dev/raw/sales",
    "log_level":    "DEBUG",
    "alert_email":  "dev-team@company.com",
    "cluster_size": "small"
}

# ── notebooks/utils/config_loader.py ─────────────
import json

def load_config(env=None):
    """Load environment-specific configuration."""
    if env is None:
        try:
            env = dbutils.widgets.get("environment")
        except Exception:
            env = "dev"
    
    config_path = f"/dbfs/configs/{env}.json"
    with open(config_path, "r") as f:
        config = json.load(f)
    
    print(f"✅ Loaded {env} config")
    return config

# ── Usage in any notebook ─────────────────────────
config = load_config()

bronze_path  = config["bronze_path"]
silver_path  = config["silver_path"]
gold_catalog = config["gold_catalog"]
log_level    = config["log_level"]
```

---

### 10.4 Utilities — Reusable Code

```python
# ── notebooks/utils/logging_utils.py ─────────────
import logging
from datetime import datetime
from pyspark.sql.functions import current_timestamp, lit

class PipelineLogger:
    def __init__(self, pipeline_name, audit_table=None):
        self.pipeline_name = pipeline_name
        self.audit_table = audit_table
        self.run_id = datetime.now().strftime("%Y%m%d_%H%M%S")
        self.start_time = datetime.now()
        
        logging.basicConfig(
            level=logging.INFO,
            format=f"%(asctime)s | {pipeline_name} | %(levelname)s | %(message)s"
        )
        self.log = logging.getLogger(pipeline_name)
    
    def info(self, msg):  self.log.info(msg)
    def error(self, msg): self.log.error(msg)
    def warn(self, msg):  self.log.warning(msg)
    
    def log_step(self, step, status, records=0, error=None):
        entry = {
            "run_id":        self.run_id,
            "pipeline_name": self.pipeline_name,
            "step":          step,
            "status":        status,
            "records":       records,
            "error":         error or "",
            "timestamp":     datetime.now().isoformat()
        }
        self.log.info(f"STEP [{step}] → {status} | records={records}")
        
        if self.audit_table:
            spark.createDataFrame([entry]) \
                .write.format("delta") \
                .mode("append") \
                .saveAsTable(self.audit_table)
    
    def summary(self):
        elapsed = (datetime.now() - self.start_time).total_seconds()
        self.log.info(f"Pipeline complete | run_id={self.run_id} | elapsed={elapsed:.1f}s")


# ── notebooks/utils/dq_utils.py ──────────────────
from pyspark.sql.functions import col, count, when, sum as spark_sum

class DataQualityChecker:
    def __init__(self, df, table_name):
        self.df = df
        self.table_name = table_name
        self.total = df.count()
        self.results = {}
    
    def check_not_null(self, column):
        nulls = self.df.filter(col(column).isNull()).count()
        pct = nulls / self.total * 100 if self.total > 0 else 0
        self.results[f"null_{column}"] = {"failures": nulls, "pct": pct}
        return self
    
    def check_no_duplicates(self, key_columns):
        dupes = self.total - self.df.dropDuplicates(key_columns).count()
        self.results["duplicates"] = {"failures": dupes, "pct": dupes/self.total*100}
        return self
    
    def check_range(self, column, min_val=None, max_val=None):
        cond = col(column).isNull()  # Start with always false
        if min_val is not None: cond = cond | (col(column) < min_val)
        if max_val is not None: cond = cond | (col(column) > max_val)
        failures = self.df.filter(cond).count()
        self.results[f"range_{column}"] = {"failures": failures, "pct": failures/self.total*100}
        return self
    
    def check_values_in(self, column, valid_values):
        failures = self.df.filter(~col(column).isin(valid_values)).count()
        self.results[f"values_{column}"] = {"failures": failures, "pct": failures/self.total*100}
        return self
    
    def report(self, raise_on_failure=False, failure_threshold_pct=1.0):
        print(f"\n📊 Data Quality Report: {self.table_name} ({self.total:,} rows)")
        any_failed = False
        for check, result in self.results.items():
            passed = result["pct"] < failure_threshold_pct
            any_failed = any_failed or not passed
            icon = "✅" if passed else "❌"
            print(f"  {icon} {check}: {result['failures']:,} failures ({result['pct']:.2f}%)")
        
        if raise_on_failure and any_failed:
            raise ValueError(f"Data quality checks FAILED for {self.table_name}")
        
        return not any_failed
```

---

### 10.5 Capstone Project — Complete Pipeline

**The Project**: Build a complete end-to-end data pipeline for a retail company.

**Data**: Daily order files + customer master data  
**Output**: Business dashboards for revenue, customers, products

```python
# ════════════════════════════════════════════════════
# CAPSTONE: Retail Analytics Pipeline
# ════════════════════════════════════════════════════

# ── SETUP ────────────────────────────────────────────
from pyspark.sql.functions import *
from pyspark.sql.types import *
from delta.tables import DeltaTable
import logging

# Configuration
CONFIG = {
    "bronze_path":  "/capstone/delta/bronze",
    "silver_path":  "/capstone/delta/silver",
    "gold_catalog": "capstone",
    "log_table":    "capstone.meta.pipeline_runs"
}

# Logging
logging.basicConfig(level=logging.INFO, format="%(asctime)s | %(message)s")
logger = logging.getLogger("RetailPipeline")


# ── GENERATE SAMPLE DATA ──────────────────────────────
def create_sample_data():
    import random
    from datetime import date, timedelta
    
    products  = ["laptop", "phone", "tablet", "monitor", "keyboard"]
    states    = ["NY", "CA", "TX", "FL", "WA"]
    statuses  = ["completed", "pending", "cancelled"]
    
    # 1000 orders across 30 days
    orders = []
    for i in range(1, 1001):
        order_date = date(2024, 1, 1) + timedelta(days=random.randint(0, 29))
        orders.append((
            i,
            random.randint(101, 130),       # customer_id 101-130
            random.choice(products),
            round(random.uniform(50, 2000), 2),
            str(order_date),
            random.choice(statuses),
            random.choice(states),
            random.randint(1, 5)            # quantity
        ))
    
    # Add some bad records
    orders += [
        (None, 101, "laptop", 500.0, "2024-01-15", "completed", "NY", 1),  # null order_id
        (1001, None, "phone", 300.0, "2024-01-16", "completed", "CA", 1),  # null customer_id
        (1002, 102, "tablet", -100.0, "2024-01-17", "completed", "TX", 1), # negative amount
    ]
    
    orders_df = spark.createDataFrame(orders, 
        ["order_id","customer_id","product","amount","order_date","status","state","quantity"])
    
    customers = spark.createDataFrame(
        [(101+i, f"Customer {101+i}", f"customer{101+i}@email.com",
          ["gold","silver","bronze","platinum"][i%4], states[i%5]) 
         for i in range(30)],
        ["customer_id", "name", "email", "tier", "home_state"]
    )
    
    return orders_df, customers

raw_orders, raw_customers = create_sample_data()
logger.info(f"Generated {raw_orders.count()} orders, {raw_customers.count()} customers")


# ── LAYER 1: BRONZE ───────────────────────────────────
def ingest_bronze(orders_df, customers_df):
    logger.info("▶ Bronze: Ingesting raw data...")
    
    # Add metadata
    orders_bronze = orders_df \
        .withColumn("_ingested_at", current_timestamp()) \
        .withColumn("_layer", lit("bronze"))
    
    customers_bronze = customers_df \
        .withColumn("_ingested_at", current_timestamp()) \
        .withColumn("_layer", lit("bronze"))
    
    orders_bronze.write.format("delta") \
        .mode("overwrite") \
        .save(f"{CONFIG['bronze_path']}/orders")
    
    customers_bronze.write.format("delta") \
        .mode("overwrite") \
        .save(f"{CONFIG['bronze_path']}/customers")
    
    logger.info(f"  ✅ Bronze complete: {orders_bronze.count()} orders, "
                f"{customers_bronze.count()} customers")


# ── LAYER 2: SILVER ───────────────────────────────────
def transform_silver():
    logger.info("▶ Silver: Cleaning and validating...")
    
    orders_raw = spark.read.format("delta").load(f"{CONFIG['bronze_path']}/orders")
    
    # ── Data Quality ─────────────────────────
    total = orders_raw.count()
    
    orders_valid = orders_raw \
        .filter(col("order_id").isNotNull()) \
        .filter(col("customer_id").isNotNull()) \
        .filter(col("amount") > 0) \
        .filter(col("order_date").isNotNull()) \
        .dropDuplicates(["order_id"])
    
    orders_invalid = orders_raw \
        .withColumn("_dq_fail_reason",
            when(col("order_id").isNull(), "null_order_id")
            .when(col("customer_id").isNull(), "null_customer_id")
            .when(col("amount") <= 0, "negative_amount")
            .otherwise(None)
        ) \
        .filter(col("_dq_fail_reason").isNotNull())
    
    logger.info(f"  📊 DQ: {orders_valid.count()}/{total} valid, "
                f"{orders_invalid.count()} quarantined")
    
    # ── Enrich + type cast ────────────────────
    orders_silver = orders_valid \
        .withColumn("amount", col("amount").cast(DoubleType())) \
        .withColumn("order_date", to_date("order_date")) \
        .withColumn("status", trim(lower(col("status")))) \
        .withColumn("year",  year("order_date")) \
        .withColumn("month", month("order_date")) \
        .withColumn("revenue", col("amount") * col("quantity")) \
        .drop("_ingested_at", "_layer")
    
    # Write silver orders
    orders_silver.write.format("delta") \
        .mode("overwrite") \
        .partitionBy("year", "month") \
        .save(f"{CONFIG['silver_path']}/orders")
    
    # Quarantine
    if orders_invalid.count() > 0:
        orders_invalid.write.format("delta") \
            .mode("append") \
            .save(f"{CONFIG['silver_path']}/quarantine")
    
    # Customers (simple pass-through with typing)
    customers_raw = spark.read.format("delta").load(f"{CONFIG['bronze_path']}/customers")
    customers_silver = customers_raw.drop("_ingested_at", "_layer")
    customers_silver.write.format("delta") \
        .mode("overwrite") \
        .save(f"{CONFIG['silver_path']}/customers")
    
    logger.info("  ✅ Silver complete")


# ── LAYER 3: GOLD ─────────────────────────────────────
def build_gold():
    logger.info("▶ Gold: Building business models...")
    
    orders    = spark.read.format("delta").load(f"{CONFIG['silver_path']}/orders")
    customers = spark.read.format("delta").load(f"{CONFIG['silver_path']}/customers")
    
    # Only completed orders for revenue metrics
    completed = orders.filter(col("status") == "completed") \
        .join(customers, on="customer_id", how="left")
    
    # ── Gold 1: Daily Revenue Summary ────────
    daily_revenue = completed.groupBy("order_date", "state") \
        .agg(
            count("order_id").alias("order_count"),
            sum("revenue").alias("total_revenue"),
            avg("revenue").alias("avg_order_value"),
            countDistinct("customer_id").alias("unique_customers"),
            sum("quantity").alias("units_sold")
        ) \
        .withColumn("revenue_rank",
            dense_rank().over(
                Window.partitionBy("order_date").orderBy(col("total_revenue").desc())
            )
        )
    
    daily_revenue.write.format("delta") \
        .mode("overwrite") \
        .saveAsTable(f"{CONFIG['gold_catalog']}.gold.daily_revenue")
    
    # ── Gold 2: Product Performance ───────────
    product_perf = completed.groupBy("product") \
        .agg(
            count("order_id").alias("orders"),
            sum("revenue").alias("revenue"),
            avg("revenue").alias("avg_price"),
            sum("quantity").alias("units")
        ) \
        .orderBy(col("revenue").desc())
    
    product_perf.write.format("delta") \
        .mode("overwrite") \
        .saveAsTable(f"{CONFIG['gold_catalog']}.gold.product_performance")
    
    # ── Gold 3: Customer Lifetime Value ───────
    clv = completed.groupBy("customer_id", "name", "tier", "home_state") \
        .agg(
            count("order_id").alias("total_orders"),
            sum("revenue").alias("lifetime_value"),
            avg("revenue").alias("avg_order_value"),
            min("order_date").alias("first_order"),
            max("order_date").alias("last_order"),
            datediff(max("order_date"), min("order_date")).alias("customer_tenure_days")
        ) \
        .withColumn("clv_tier",
            when(col("lifetime_value") >= 5000, "high_value")
            .when(col("lifetime_value") >= 1000, "medium_value")
            .otherwise("low_value")
        )
    
    clv.write.format("delta") \
        .mode("overwrite") \
        .saveAsTable(f"{CONFIG['gold_catalog']}.gold.customer_ltv")
    
    logger.info("  ✅ Gold: daily_revenue, product_performance, customer_ltv created")


# ── VALIDATION ────────────────────────────────────────
def validate_pipeline():
    logger.info("▶ Validating pipeline outputs...")
    
    silver_orders = spark.read.format("delta").load(f"{CONFIG['silver_path']}/orders")
    bronze_orders = spark.read.format("delta").load(f"{CONFIG['bronze_path']}/orders")
    quarantine    = spark.read.format("delta").load(f"{CONFIG['silver_path']}/quarantine")
    
    bronze_count    = bronze_orders.count()
    silver_count    = silver_orders.count()
    quarantine_count = quarantine.count()
    
    print(f"\n✅ Pipeline Validation Summary")
    print(f"   Bronze rows:     {bronze_count:>6,}")
    print(f"   Silver rows:     {silver_count:>6,}")
    print(f"   Quarantine rows: {quarantine_count:>6,}")
    print(f"   Accounted for:   {silver_count + quarantine_count:>6,} "
          f"({'match' if silver_count+quarantine_count == bronze_count else 'MISMATCH'})")
    
    gold_revenue = spark.table(f"{CONFIG['gold_catalog']}.gold.daily_revenue")
    gold_product = spark.table(f"{CONFIG['gold_catalog']}.gold.product_performance")
    gold_clv     = spark.table(f"{CONFIG['gold_catalog']}.gold.customer_ltv")
    
    print(f"\n   Gold Tables:")
    print(f"   daily_revenue:    {gold_revenue.count():>4} rows")
    print(f"   product_perf:     {gold_product.count():>4} rows")
    print(f"   customer_ltv:     {gold_clv.count():>4} rows")


# ── RUN THE COMPLETE PIPELINE ─────────────────────────
def run_full_pipeline():
    logger.info("🚀 Starting Retail Analytics Pipeline")
    
    try:
        ingest_bronze(raw_orders, raw_customers)
        transform_silver()
        build_gold()
        validate_pipeline()
        logger.info("✅ Pipeline completed successfully!")
        
    except Exception as e:
        logger.error(f"❌ Pipeline FAILED: {str(e)}")
        raise

run_full_pipeline()
```

---

### 10.6 Production Readiness Checklist

Run through this before any pipeline goes live:

```
COMPUTE
  □ Job cluster (not all-purpose) for production runs
  □ Auto-terminate enabled
  □ Right instance type for workload (memory vs compute)
  □ LTS runtime version

CODE QUALITY
  □ No hardcoded paths, secrets, or credentials
  □ All credentials via dbutils.secrets
  □ Config management (dev/staging/prod configs)
  □ Code reviewed by team member
  □ Edge cases handled (empty files, null values, schema drift)

DATA QUALITY
  □ Null checks on key columns
  □ Duplicate detection
  □ Range validation (no negative amounts, future dates)
  □ Quarantine layer for bad records
  □ Source/target row count validation

ERROR HANDLING
  □ Try/catch blocks on every major step
  □ Meaningful error messages in logs
  □ Audit log table updated for every run
  □ Idempotent writes (safe to re-run)
  □ Retry configured (max 2-3 retries)

OBSERVABILITY
  □ Logging at info/error levels
  □ Email/Slack alerts on failure
  □ SLA: expected completion time documented
  □ Monitoring dashboard (row counts, run times)
  □ Alert if run time exceeds 2× normal

PERFORMANCE
  □ Correct partition strategy (write partitionBy)
  □ AQE enabled
  □ Broadcast for small tables
  □ No collect() on large DataFrames
  □ Delta OPTIMIZE scheduled

DOCUMENTATION
  □ README with pipeline overview
  □ Data lineage documented
  □ Runbook: what to do if pipeline fails
  □ Schema documented (column descriptions)

TESTING
  □ Unit tests for transform functions
  □ Integration test with sample data
  □ Tested in staging before prod deploy

SECURITY
  □ No PII in logs
  □ Column-level access control on sensitive tables
  □ Secrets in Databricks Secret Manager
  □ Job runs under service principal (not personal account)
```

---

### 10.7 What To Do When Production Fails

```
STEP 1: ASSESS (2 minutes)
  - Is it failing or slow?
  - What time did it start failing?
  - What changed recently? (new code? new data?)

STEP 2: CHECK LOGS (5 minutes)
  - Job Runs → click failed run → check task error
  - Look at last successful run vs this run
  - Check audit log table for any pattern

STEP 3: CHECK DATA (5 minutes)
  - Did source data arrive? (check file counts)
  - Is it a volume spike? (more data than usual)
  - Data format changed? (new column? type change?)

STEP 4: FIX
  Common causes and fixes:
  ┌─────────────────────────────────────────────────┐
  │ ERROR           → FIX                           │
  ├─────────────────────────────────────────────────┤
  │ FileNotFound    → Source file didn't arrive yet │
  │                   Wait or alert source team      │
  │ OOM             → Reduce data, increase cluster  │
  │ Schema mismatch → Source added/removed column   │
  │                   Update schema / mergeSchema   │
  │ Job timed out   → Data volume spike, scale up   │
  │ DQ failures     → Bad data from source, quarant │
  └─────────────────────────────────────────────────┘

STEP 5: REPAIR (not full re-run)
  - Use Databricks "Repair Run" to re-run only failed tasks
  - Saves time vs full pipeline restart

STEP 6: POST-MORTEM
  - Document: what happened, root cause, fix, prevention
  - Add monitoring/alert to catch sooner next time
```

---

## ❓ Final Q&A — Common Interview Questions

### Q1: How do you ensure a pipeline is idempotent?

```python
# Idempotent = running multiple times produces the same result

# Method 1: Overwrite with replaceWhere (partition level)
df.write.format("delta") \
    .mode("overwrite") \
    .option("replaceWhere", "order_date = '2024-01-15'") \
    .saveAsTable("gold.daily_summary")

# Method 2: Delete + insert
spark.sql(f"DELETE FROM gold.daily_summary WHERE batch_date = '{batch_date}'")
df.write.format("delta").mode("append").saveAsTable("gold.daily_summary")

# Method 3: MERGE (upsert)
DeltaTable.forName(spark, "gold.orders").alias("t").merge(
    new_data.alias("s"), "t.order_id = s.order_id"
).whenMatchedUpdateAll().whenNotMatchedInsertAll().execute()
```

---

### Q2: What is the difference between a Notebook job and a Python wheel?

| | Notebook Job | Python Wheel (.whl) |
|-|-------------|---------------------|
| Development | Quick, interactive | Requires packaging |
| Code reuse | Hard to import | pip installable |
| Testing | Manual | Proper unit tests |
| Version control | Git integration | Package versioning |
| When to use | Early stage, exploration | Mature, production code |

---

### Q3: How do you handle schema evolution?

```python
# Schema evolution strategies:

# 1. mergeSchema (ADD new columns, keep old)
df.write.format("delta") \
    .option("mergeSchema", "true") \
    .mode("append") \
    .save(target_path)

# 2. overwriteSchema (completely replace schema)
df.write.format("delta") \
    .option("overwriteSchema", "true") \
    .mode("overwrite") \
    .save(target_path)

# 3. Explicit handling
def safe_select(df, expected_cols):
    """Select only expected columns; fill missing with null."""
    exprs = [
        col(c) if c in df.columns else lit(None).alias(c)
        for c in expected_cols
    ]
    return df.select(*exprs)
```

---

### Q4: What is Unity Catalog and why does it matter?

```
Unity Catalog = Central governance for all data assets in Databricks

Structure:
  Catalog (top level) → Schema (database) → Table/View/Function

Benefits:
  1. Single place to manage access to ALL tables across workspaces
  2. Row/column level security
  3. Data lineage (track where data came from)
  4. Audit trail (who accessed what, when)
  5. Tag and classify sensitive data (PII, etc.)

For Data Engineers:
  Use 3-part naming: catalog.schema.table
  CREATE TABLE my_catalog.my_schema.orders ...
```

---

## 🔑 Final Key Takeaways (All 10 Days)

### The Core Principles

| # | Principle | Why |
|---|-----------|-----|
| 1 | **Delta Lake everywhere** | ACID, time travel, MERGE |
| 2 | **Bronze → Silver → Gold** | Reprocessable, auditable |
| 3 | **Filter and select early** | Reduce data volume ASAP |
| 4 | **Never collect() large data** | Driver will OOM |
| 5 | **Idempotent pipelines** | Safe to retry |
| 6 | **Config over code** | Dev/Staging/Prod parity |
| 7 | **Secrets via dbutils.secrets** | Never hardcode |
| 8 | **Audit log everything** | Operations needs it |
| 9 | **Use AQE** | Free performance |
| 10 | **Test in staging first** | Never trust prod |

---

### The Full Databricks Stack (What You Now Know)

```
┌─────────────────────────────────────────────────────┐
│                DATABRICKS LAKEHOUSE                  │
│                                                      │
│  ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐  │
│  │ Day 1-2 │ │  Day 3   │ │  Day 4   │ │ Day 5  │  │
│  │Platform │ │ PySpark  │ │   ETL    │ │ Delta  │  │
│  │ + Spark │ │   API    │ │Medallion │ │  Lake  │  │
│  └─────────┘ └──────────┘ └──────────┘ └────────┘  │
│  ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐  │
│  │ Day 6   │ │  Day 7   │ │  Day 8   │ │Day 9-10│  │
│  │Advanced │ │ Perf.    │ │ Workflow │ │ Prod   │  │
│  │Transforms│ │  Optim.  │ │  Jobs   │ │Patterns│  │
│  └─────────┘ └──────────┘ └──────────┘ └────────┘  │
└─────────────────────────────────────────────────────┘
```

---

**🎉 Congratulations! You've completed the 10-hour Databricks Data Engineering Training!**

You are now equipped to:
- ✅ Design and build production-grade ETL pipelines
- ✅ Work with Delta Lake, MERGE, and time travel
- ✅ Optimize Spark jobs for performance
- ✅ Schedule, monitor, and recover pipelines
- ✅ Apply industry-standard patterns to real problems
