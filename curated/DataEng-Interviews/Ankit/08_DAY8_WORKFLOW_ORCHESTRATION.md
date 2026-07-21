# Day 8: Workflow Orchestration
## Databricks Jobs, Scheduling, Error Handling & Monitoring

**Duration**: 1 hour | **Difficulty**: Intermediate → Advanced

---

## 🎯 Learning Objectives

- ✅ Create and configure Databricks Jobs
- ✅ Build multi-task workflows with dependencies
- ✅ Implement robust error handling and retries
- ✅ Use Databricks Widgets for parameterized notebooks
- ✅ Understand Delta Live Tables (DLT) basics
- ✅ Monitor and alert on job failures

---

## 📚 Content

### 8.1 Databricks Jobs vs Notebooks

```
NOTEBOOK (Interactive)         JOB (Scheduled/Production)
─────────────────────────────────────────────────────────
Manual run (you click Run)  → Automatic (cron schedule)
All-purpose cluster         → Job cluster (cheaper)
No retry on failure         → Auto-retry configurable
No alerts                   → Email/Slack alerts
Single user                 → Runs unattended
```

**When to use Jobs:**
- Daily/hourly ETL pipelines
- Data quality checks
- Model retraining
- Scheduled reports

---

### 8.2 Databricks Widgets — Parameterize Notebooks

Widgets let you pass parameters to notebooks (from Jobs, other notebooks, or UI).

```python
# ── Create widgets ─────────────────────────────────
dbutils.widgets.text("start_date", "2024-01-01", "Start Date")
dbutils.widgets.text("end_date",   "2024-01-31", "End Date")
dbutils.widgets.dropdown("environment", "dev", ["dev", "staging", "prod"])
dbutils.widgets.multiselect("states", "NY", ["NY", "CA", "TX", "FL"])

# ── Read widget values ─────────────────────────────
start_date  = dbutils.widgets.get("start_date")
end_date    = dbutils.widgets.get("end_date")
environment = dbutils.widgets.get("environment")

print(f"Running pipeline: {start_date} to {end_date} in {environment}")

# ── Use in your code ───────────────────────────────
df = spark.table("catalog.schema.orders") \
    .filter(
        (col("order_date") >= start_date) &
        (col("order_date") <= end_date)
    )

# ── Remove widgets (cleanup) ───────────────────────
dbutils.widgets.removeAll()
```

---

### 8.3 dbutils — The Databricks Swiss Army Knife

```python
# ── File System operations ─────────────────────────
dbutils.fs.ls("/mnt/data/")              # List files
dbutils.fs.mkdirs("/mnt/data/new_dir")  # Create directory
dbutils.fs.cp("/source", "/dest")        # Copy files
dbutils.fs.mv("/source", "/dest")        # Move files
dbutils.fs.rm("/path", recurse=True)     # Delete files/folder
dbutils.fs.head("/data/file.txt")        # Read first bytes

# ── Secrets Management ─────────────────────────────
# Never hardcode passwords! Use Databricks secrets
password  = dbutils.secrets.get(scope="prod-secrets", key="db-password")
api_token = dbutils.secrets.get(scope="prod-secrets", key="api-token")
# secret value is masked in notebook output ✅

# List available secrets
dbutils.secrets.list("prod-secrets")

# ── Notebook utilities ─────────────────────────────
# Run another notebook and get its return value
result = dbutils.notebook.run(
    "/path/to/child_notebook",
    timeout_seconds=600,
    arguments={"start_date": "2024-01-01", "env": "prod"}
)
print(result)  # Gets return value from exit()

# Return value from a notebook
dbutils.notebook.exit("pipeline_success:5000_records")

# ── Library utilities ──────────────────────────────
dbutils.library.restartPython()  # Restart Python kernel after installing libs
```

---

### 8.4 Error Handling Patterns

Robust pipelines anticipate failures.

```python
import logging
from datetime import datetime
from pyspark.sql.functions import current_timestamp, lit

# ── Logging setup ──────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)s | %(message)s"
)
logger = logging.getLogger("ETLPipeline")

# ── Try/Except with proper logging ────────────────
def run_pipeline_step(step_name, func, *args, **kwargs):
    """Wrapper to run a pipeline step with error handling."""
    logger.info(f"Starting step: {step_name}")
    start = datetime.now()
    
    try:
        result = func(*args, **kwargs)
        elapsed = (datetime.now() - start).total_seconds()
        logger.info(f"Completed step: {step_name} in {elapsed:.2f}s")
        return result
        
    except Exception as e:
        elapsed = (datetime.now() - start).total_seconds()
        logger.error(f"FAILED step: {step_name} after {elapsed:.2f}s | Error: {str(e)}")
        raise  # Re-raise so Job knows it failed

# ── Pipeline with error handling ──────────────────
def ingest_data(source_path):
    """Ingest with validation."""
    df = spark.read.parquet(source_path)
    
    # Validate: fail fast if critical columns missing
    required_cols = ["order_id", "customer_id", "amount"]
    missing = [c for c in required_cols if c not in df.columns]
    if missing:
        raise ValueError(f"Missing required columns: {missing}")
    
    row_count = df.count()
    if row_count == 0:
        raise ValueError(f"Empty source file: {source_path}")
    
    logger.info(f"Ingested {row_count:,} rows from {source_path}")
    return df

def transform_data(df):
    """Transform with safety checks."""
    try:
        result = df \
            .filter(col("order_id").isNotNull()) \
            .withColumn("processed_at", current_timestamp())
        return result
    except Exception as e:
        logger.error(f"Transform failed: {e}")
        raise

# ── Audit log pattern ──────────────────────────────
def write_audit_log(pipeline_name, step, status, records=None, error=None):
    log = spark.createDataFrame([{
        "pipeline_name": pipeline_name,
        "step": step,
        "status": status,
        "records_processed": records or 0,
        "error_message": error or "",
        "run_timestamp": datetime.now().isoformat()
    }])
    log.write.format("delta").mode("append") \
        .saveAsTable("catalog.meta.pipeline_audit")

# ── Idempotent writes ──────────────────────────────
# Idempotent = safe to run multiple times (same result)
# Pattern: delete existing data for the run date, then insert fresh

def idempotent_write(df, target_table, partition_col, partition_value):
    """Write that's safe to re-run — deletes old run's data first."""
    spark.sql(f"""
        DELETE FROM {target_table}
        WHERE {partition_col} = '{partition_value}'
    """)
    df.filter(col(partition_col) == partition_value) \
      .write.format("delta") \
      .mode("append") \
      .saveAsTable(target_table)
    logger.info(f"Idempotent write complete: {target_table} for {partition_value}")
```

---

### 8.5 Creating a Databricks Job (UI Walkthrough)

#### **Via Databricks UI:**

1. **Left sidebar** → "Workflows" → "Jobs"
2. Click **"Create Job"**
3. **Configure Task 1**:
   ```
   Task Name: ingest_bronze
   Type: Notebook
   Source: /path/to/notebook
   Cluster: Create new job cluster
     Runtime: 13.3 LTS
     Workers: 4 × i3.xlarge
   Parameters: {"start_date": "{{job.start_time.iso_date}}"}
   ```
4. **Add Task 2** (depends on Task 1):
   ```
   Task Name: transform_silver
   Type: Notebook
   Depends on: ingest_bronze
   ```
5. **Add Task 3**:
   ```
   Task Name: build_gold
   Depends on: transform_silver
   ```
6. **Configure Schedule**:
   ```
   Trigger: Scheduled
   Cron: 0 2 * * *   (every day at 2 AM)
   Timezone: UTC
   ```
7. **Configure Notifications**:
   ```
   On failure: email@company.com
   On success: email@company.com (optional)
   ```

#### **DAG View:**

```
[ingest_bronze] → [transform_silver] → [build_gold]
       ↓ fail              ↓ fail              ↓
  Send Alert         Send Alert          Send Alert
  Stop Pipeline      Stop Pipeline       Stop Pipeline
```

---

### 8.6 Multi-Task Workflow (JSON Definition)

```json
{
  "name": "daily_sales_pipeline",
  "tasks": [
    {
      "task_key": "ingest_bronze",
      "notebook_task": {
        "notebook_path": "/pipelines/01_bronze_ingest",
        "base_parameters": {
          "batch_date": "{{job.start_time.iso_date}}"
        }
      },
      "job_cluster_key": "pipeline_cluster"
    },
    {
      "task_key": "transform_silver",
      "depends_on": [{"task_key": "ingest_bronze"}],
      "notebook_task": {
        "notebook_path": "/pipelines/02_silver_transform",
        "base_parameters": {
          "batch_date": "{{job.start_time.iso_date}}"
        }
      },
      "job_cluster_key": "pipeline_cluster"
    },
    {
      "task_key": "build_gold",
      "depends_on": [{"task_key": "transform_silver"}],
      "notebook_task": {
        "notebook_path": "/pipelines/03_gold_build"
      },
      "job_cluster_key": "pipeline_cluster"
    }
  ],
  "job_clusters": [
    {
      "job_cluster_key": "pipeline_cluster",
      "new_cluster": {
        "spark_version": "13.3.x-scala2.12",
        "node_type_id": "i3.xlarge",
        "num_workers": 4,
        "spark_conf": {
          "spark.sql.adaptive.enabled": "true",
          "spark.sql.shuffle.partitions": "auto"
        }
      }
    }
  ],
  "schedule": {
    "quartz_cron_expression": "0 0 2 * * ?",
    "timezone_id": "UTC",
    "pause_status": "UNPAUSED"
  },
  "email_notifications": {
    "on_failure": ["data-team@company.com"],
    "on_success": []
  },
  "max_concurrent_runs": 1
}
```

---

### 8.7 Delta Live Tables (DLT) — Declarative Pipelines

DLT lets you declare WHAT you want (not HOW to compute it) and Databricks manages orchestration, quality checks, and retries.

```python
import dlt
from pyspark.sql.functions import col, current_timestamp

# ── Bronze: Raw ingestion ─────────────────────────
@dlt.table(
    name="raw_orders",
    comment="Raw orders from CSV source",
    table_properties={"quality": "bronze"}
)
def raw_orders():
    return (
        spark.readStream
            .format("cloudFiles")
            .option("cloudFiles.format", "csv")
            .option("cloudFiles.schemaLocation", "/checkpoints/orders/schema")
            .load("/raw/orders/")
            .withColumn("_ingested_at", current_timestamp())
    )

# ── Silver: Clean with expectations ───────────────
@dlt.table(
    name="clean_orders",
    comment="Validated and cleaned orders",
    table_properties={"quality": "silver"}
)
@dlt.expect_all_or_drop({             # Drop rows failing these rules
    "valid_order_id":   "order_id IS NOT NULL",
    "positive_amount":  "amount > 0",
    "valid_status":     "status IN ('pending', 'completed', 'cancelled')"
})
def clean_orders():
    return (
        dlt.read_stream("raw_orders")
            .dropDuplicates(["order_id"])
            .withColumn("amount", col("amount").cast("double"))
    )

# ── Gold: Business aggregates ──────────────────────
@dlt.table(
    name="daily_summary",
    comment="Daily business summary",
    table_properties={"quality": "gold"}
)
def daily_summary():
    return (
        dlt.read("clean_orders")
            .filter(col("status") == "completed")
            .groupBy("order_date", "state")
            .agg(
                count("*").alias("orders"),
                sum("amount").alias("revenue")
            )
    )
```

**DLT vs Manual Pipelines:**

| Aspect | Manual (Notebooks + Jobs) | Delta Live Tables |
|--------|--------------------------|------------------|
| Error handling | You write it | Built-in |
| Data quality | You write checks | `@dlt.expect` decorators |
| Dependencies | You define in Job DAG | Inferred from `dlt.read()` |
| Monitoring | Manual dashboards | Built-in pipeline UI |
| Learning curve | Lower | Higher |
| Flexibility | More control | More automation |

---

### 8.8 Job Retry and Repair

```python
# Configure retry in Job settings (UI or JSON):
{
    "max_retries": 2,           # Retry up to 2 times
    "retry_on_timeout": false,
    "min_retry_interval_millis": 60000  # Wait 1 min before retry
}

# Re-run only failed tasks (Job Repair)
# In UI: Job Runs → Select failed run → "Repair Run"
# Only failed tasks re-run (successful tasks not repeated)

# Check if re-run using run_id
run_id = dbutils.widgets.get("databricks.run_id") if "databricks.run_id" in [w.name for w in dbutils.widgets.getAll()] else "manual"
print(f"Run ID: {run_id}")
```

---

## 💻 Hands-On Exercise

```python
# Exercise: Build a parameterized, error-handled notebook

# Step 1: Create widgets
dbutils.widgets.text("batch_date", "2024-01-15", "Batch Date")
dbutils.widgets.dropdown("run_mode", "full", ["full", "incremental"])

# Step 2: Read parameters
batch_date = dbutils.widgets.get("batch_date")
run_mode   = dbutils.widgets.get("run_mode")

# Step 3: Write pipeline with error handling
def run_etl(batch_date, run_mode):
    results = {}
    
    # Ingest
    try:
        data = [(i, f"order_{i}", float(i*100), batch_date) 
                for i in range(1, 101)]
        df = spark.createDataFrame(data, 
                                   ["id", "name", "amount", "date"])
        results["ingested_rows"] = df.count()
        print(f"✅ Ingested {results['ingested_rows']} rows")
    except Exception as e:
        print(f"❌ Ingestion failed: {e}")
        raise
    
    # Transform
    try:
        clean_df = df.filter(col("amount") > 500)
        results["valid_rows"] = clean_df.count()
        print(f"✅ Validated {results['valid_rows']} rows")
    except Exception as e:
        print(f"❌ Transform failed: {e}")
        raise
    
    return results

results = run_etl(batch_date, run_mode)
print(f"Pipeline complete: {results}")

# Return value for calling notebook
dbutils.notebook.exit(str(results))
```

---

## ❓ Q&A

### Q1: When should I use DLT vs manual Jobs?

| Use DLT when | Use manual Jobs when |
|--------------|---------------------|
| Building new pipelines | Existing notebook pipelines |
| Need built-in data quality | Need maximum flexibility |
| Streaming + batch mixed | Complex business logic per step |
| Team is onboarding to Databricks | Need to call external APIs per step |

---

### Q2: How do I pass secrets to a job?

```python
# ✅ RIGHT: Use Databricks Secrets
conn_str = dbutils.secrets.get(scope="my-scope", key="db-connection")

# ❌ WRONG: Never hardcode secrets
conn_str = "Server=prod-db;Password=mypassword123"

# ❌ WRONG: Never use widgets for secrets (they appear in logs)
dbutils.widgets.text("password", "", "DB Password")
```

---

### Q3: How do I schedule a job to run every 15 minutes?

```
Cron expression: 0 */15 * * * ?
                 ─ ─────
                 │ │ Every 15 mins
                 │ Minutes
                 Seconds (fixed 0)
```

---

### Q4: What's the difference between task timeout and cluster timeout?

- **Task timeout**: How long a single notebook/task can run before being killed
- **Cluster auto-terminate**: How long cluster sits idle before shutting down

Set both! Prevent runaway jobs AND prevent idle cluster costs.

---

## 🔑 Key Takeaways

1. **Jobs** = production-grade, scheduled, automated execution
2. **Widgets** = parameters passed from Job to notebook
3. **dbutils.secrets** = never hardcode credentials
4. **Multi-task workflows** = declare dependencies between steps
5. **Error handling** is your responsibility in manual pipelines
6. **DLT** = declarative pipelines with built-in quality + monitoring
7. **Repair Run** = re-run only failed tasks (efficient)
8. **Audit logs** = always write pipeline metadata for operations

---

**Next Session → Day 9: Real-World Case Studies & Decision Frameworks**
