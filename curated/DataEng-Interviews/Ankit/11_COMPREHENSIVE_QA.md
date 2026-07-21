# Comprehensive Q&A Reference
## All Topics — Databricks Data Engineering Training

> Use Ctrl+F / Cmd+F to search by keyword

---

## SECTION 1: Databricks Platform (Day 1)

**Q1. What is Databricks?**  
Databricks is a managed cloud platform built on Apache Spark. It provides a collaborative workspace for data engineering, data science, and analytics. Think of Spark as the engine and Databricks as the complete car with dashboard, controls, and all accessories included.

---

**Q2. What's the difference between Databricks and Apache Spark?**  
Apache Spark is the open-source distributed computing engine. Databricks is a commercial platform that wraps Spark with a managed infrastructure, collaborative notebooks, job scheduling, security, Unity Catalog, and cloud integrations. You write Spark code inside Databricks.

---

**Q3. What are the main components of Databricks?**  
- **Workspace** — your project area (notebooks, repos, files)
- **Clusters** — Spark compute (driver + workers)
- **Notebooks** — interactive development environment
- **Jobs** — scheduled pipeline orchestration
- **Catalog (Unity Catalog)** — centralized metadata and governance
- **SQL Warehouses** — optimized compute for SQL queries
- **Repos** — Git integration for version control

---

**Q4. What is the difference between an all-purpose cluster and a job cluster?**  
An all-purpose cluster stays on until you stop it and can be shared across users and notebooks — good for development. A job cluster is automatically created when a job starts and terminated when it finishes — cheaper and preferred for production pipelines.

---

**Q5. What is DBFS?**  
Databricks File System — a virtual file system abstraction over cloud storage (S3, ADLS, GCS). The root is `/dbfs/`. Mounted cloud storage appears at `/mnt/`. For most production work, you'll read/write Delta tables by name rather than DBFS paths directly.

---

**Q6. How do I reduce cluster costs?**  
- Use job clusters (auto-terminate) instead of all-purpose clusters
- Set auto-terminate on all-purpose clusters (e.g., 15 minutes idle)
- Right-size clusters — start small, scale up only if needed
- Use spot/preemptible instances for non-critical jobs
- Schedule jobs during off-peak hours

---

**Q7. What is the Databricks Runtime (DBR)?**  
A versioned bundle of Apache Spark + Python + pre-installed libraries + optimizations specific to Databricks (e.g., Photon engine, Delta Lake). Always use an LTS (Long-Term Support) version for production. Example: `13.3 LTS`.

---

**Q8. What is Unity Catalog?**  
Unity Catalog is Databricks' centralized governance layer for all data assets. It provides a three-level namespace: `catalog.schema.table`. It enables fine-grained access control, data lineage tracking, auditing, and sharing data across workspaces.

---

**Q9. What is Photon?**  
Photon is Databricks' native vectorized query engine (written in C++) that replaces the standard Spark JVM execution for SQL and DataFrame operations. It's significantly faster for scan-heavy operations. It's enabled per cluster and priced separately.

---

**Q10. How does Git integration work in Databricks?**  
Under "Repos" in the workspace, you can connect a Databricks folder to a Git repository (GitHub, GitLab, Bitbucket, Azure DevOps). You can commit, push, pull, create branches, and do code reviews — all standard Git workflows from inside Databricks.

---

## SECTION 2: Apache Spark Basics (Day 2)

**Q11. Why is Spark faster than Hadoop MapReduce?**  
Spark stores intermediate results in memory (RAM), whereas MapReduce writes to disk between every step. RAM access is ~200,000× faster than spinning disk. Spark also has a query optimizer (Catalyst) and avoids unnecessary I/O.

---

**Q12. What is the Driver in Spark?**  
The Driver is the main process that runs your application code, coordinates task scheduling, and collects results. It hosts the SparkSession. In Databricks, the Driver is the cluster's "Driver Node." It should NOT hold large datasets (avoid `.collect()` on big DataFrames).

---

**Q13. What is an Executor?**  
An Executor is a JVM process running on a worker node. It executes tasks assigned by the Driver and stores data in memory (cache). Each worker node can run multiple executors.

---

**Q14. What is lazy evaluation?**  
Transformations in Spark are lazy — they are not executed when you call them. Spark builds a logical plan (DAG) of all your transformations and only executes when you call an Action (like `.show()`, `.count()`, `.write`). This lets Spark optimize the entire plan before running.

---

**Q15. What is the difference between a Transformation and an Action?**  
- **Transformation** — creates a new DataFrame, lazy (not executed): `filter`, `select`, `withColumn`, `join`, `groupBy`
- **Action** — triggers execution, returns result: `show`, `count`, `collect`, `write`, `first`, `take`

---

**Q16. What is a DAG in Spark?**  
DAG = Directed Acyclic Graph. It's Spark's visual representation of all your transformations as a flow of operations. Spark builds this DAG when you write transformations, then optimizes and executes it when an action is called. You can view it in the Spark UI.

---

**Q17. What is a Stage in Spark?**  
A Stage is a group of tasks that can run in parallel without shuffling data across the network. A new stage starts whenever Spark needs to redistribute data (i.e., at a shuffle boundary like GroupBy or Join).

---

**Q18. What is a shuffle?**  
A shuffle is when Spark redistributes data across executors over the network. Required for operations like `groupBy`, `join` (non-broadcast), and `orderBy`. Shuffles are expensive because they involve network I/O and disk writes. Minimize them by filtering data early, using broadcast joins, and avoiding unnecessary aggregations.

---

**Q19. What is the difference between RDD and DataFrame?**  
RDD (Resilient Distributed Dataset) is Spark's low-level, unstructured abstraction with no schema and no automatic optimization. DataFrame is a structured table with named columns and types, optimized by the Catalyst engine. For all data engineering work, use DataFrames. Only use RDDs for very custom low-level operations.

---

**Q20. What is a partition in Spark?**  
A partition is a chunk of data stored on one executor. All partitions of a DataFrame are processed in parallel across executors. The optimal partition size is ~128MB. Too many small partitions create overhead; too few large partitions leave workers idle.

---

**Q21. What is the difference between `repartition` and `coalesce`?**  
`repartition(n)` performs a full shuffle and creates exactly n evenly distributed partitions — can increase or decrease. `coalesce(n)` only merges existing partitions (no full shuffle), so it's cheaper but can only decrease the count and may create uneven partitions.

---

**Q22. What is the SparkSession?**  
SparkSession is the entry point to the Spark API in Python (PySpark). In Databricks, it's pre-created as the `spark` variable. You use it to read data (`spark.read`), run SQL (`spark.sql`), and create DataFrames (`spark.createDataFrame`).

---

## SECTION 3: PySpark Essentials (Day 3)

**Q23. Why should I define a schema explicitly instead of using `inferSchema`?**  
`inferSchema=True` makes Spark read the entire file twice (once to infer, once to read), which is slow on large files. Explicit schemas are faster, predictable, and prevent silent type mismatches when source data changes.

---

**Q24. What is the difference between `filter` and `where`?**  
They are identical — both filter rows based on a condition. Use whichever reads more naturally to you. Most engineers prefer `filter` in PySpark and `WHERE` in SQL.

---

**Q25. What does `withColumn` do?**  
`withColumn(name, expression)` adds a new column or replaces an existing column with the given name. It returns a new DataFrame (does not modify in place). Example:
```python
df.withColumn("tax", col("amount") * 0.08)
df.withColumn("status", upper(col("status")))  # overwrite existing
```

---

**Q26. How do I handle nulls in PySpark?**  
- `df.fillna(value)` — fill nulls with a default
- `df.dropna()` — remove rows with any null
- `df.dropna(subset=["col1","col2"])` — remove rows where specific cols are null
- `col("x").isNull()` / `col("x").isNotNull()` — filter on nulls
- `coalesce(col("a"), col("b"), lit("default"))` — return first non-null

---

**Q27. What is the difference between `col("x")` and `df["x"]`?**  
Both reference a column. `col("x")` from `pyspark.sql.functions` is preferred because it avoids ambiguity in joins (where both DataFrames might have the same column name) and reads more cleanly in long transformation chains.

---

**Q28. What are the types of joins in Spark?**  
- `inner` — only matching rows
- `left` / `left_outer` — all from left, matching from right
- `right` / `right_outer` — all from right, matching from left
- `outer` / `full` / `full_outer` — all rows from both
- `left_semi` — rows from left where match exists (like IN subquery)
- `left_anti` — rows from left where NO match exists (like NOT IN)
- `cross` — cartesian product (every combination)

---

**Q29. Why does a join on different column names create duplicate columns?**  
When you join using an expression (`orders.customer_id == customers.customer_id`), Spark keeps both columns since they come from different DataFrames. Fix by using the `on=` shorthand (if same name) or `.drop()` the duplicate after the join.

---

**Q30. What is `selectExpr`?**  
`selectExpr` lets you write SQL-style string expressions in `select`. Example:
```python
df.selectExpr("name", "age + 1 AS next_year_age", "UPPER(state) AS state_upper")
```
Equivalent to the `select` + `col` approach but more concise for SQL-familiar users.

---

**Q31. What is the best file format for analytics?**  
Parquet is the best open format for analytics (columnar, compressed, typed). In Databricks, Delta Lake (Parquet + transaction log) is the best overall format because it adds ACID, time travel, and MERGE. Never use CSV in production pipelines.

---

**Q32. What does `cache()` do and when should I use it?**  
`cache()` stores a DataFrame in memory (and disk if needed) after its first action. Use it when the same DataFrame is used by multiple actions (e.g., multiple `groupBy` on the same enriched DataFrame). Always call an action like `.count()` after `.cache()` to trigger the cache fill. Release with `.unpersist()` when done.

---

**Q33. What is the danger of `collect()`?**  
`collect()` brings ALL rows from all executors to the Driver's memory. If the DataFrame is large (e.g., 10GB), the driver will run out of memory and crash. Use `show(n)`, `take(n)`, or `write` instead. Only `collect()` on small, known-size DataFrames.

---

## SECTION 4: Data Ingestion & ETL (Day 4)

**Q34. What is the Medallion Architecture?**  
A three-layer data organization pattern:
- **Bronze** — raw data as-is from the source, with metadata added
- **Silver** — cleaned, validated, typed, deduplicated, and enriched
- **Gold** — business-ready aggregations and models for BI tools

Each layer is stored as Delta tables.

---

**Q35. Why never transform data in the Bronze layer?**  
Bronze is your audit trail and safety net. If a bug is introduced in Silver or Gold, you can always reprocess from Bronze. If you transform Bronze directly, you lose the original raw data and can't recover.

---

**Q36. What is incremental loading and why is it important?**  
Incremental loading processes only new or changed data since the last run, instead of reprocessing everything. It reduces compute costs, reduces run time, and is required when data volumes are too large to full-load repeatedly.

---

**Q37. What is the high-water mark pattern?**  
The high-water mark is the timestamp or ID of the last successfully processed record. On each run, you read data newer than this watermark from the source and append to the target. The watermark is then updated to the new maximum.

---

**Q38. What is Auto Loader in Databricks?**  
Auto Loader (`cloudFiles` format) is Databricks' efficient file ingestion tool. It detects new files in cloud storage (S3, ADLS, GCS) as they arrive, tracks which files have been processed (via checkpointing), and handles millions of files efficiently. It's the preferred way to ingest files into the Bronze layer.

---

**Q39. What is a checkpoint in streaming?**  
A checkpoint is a directory where Spark Structured Streaming saves its progress. It tracks which data has been processed so that on restart (after failure or planned stop), the stream picks up exactly where it left off without reprocessing or missing data.

---

**Q40. What is idempotency and why does it matter for pipelines?**  
An idempotent pipeline produces the same result regardless of how many times it runs. It's critical for production because jobs can be retried on failure. Use patterns like DELETE+INSERT, MERGE, or `replaceWhere` to ensure re-running a pipeline for the same date doesn't double-count data.

---

**Q41. What is a quarantine layer?**  
Instead of dropping bad records, you write them to a separate quarantine table with a reason for rejection. This lets you audit data quality issues, fix the source problem, and reprocess quarantined records once the issue is resolved.

---

**Q42. What is the difference between batch and streaming processing?**  
Batch processes all available data at a scheduled time (daily, hourly). Streaming processes data continuously or in micro-batches as it arrives. In Databricks, structured streaming (`spark.readStream`) handles both, with the `trigger` setting controlling the micro-batch interval.

---

## SECTION 5: Delta Lake (Day 5)

**Q43. What is Delta Lake?**  
Delta Lake is an open-source storage layer that adds ACID transactions, versioning, and upsert support to Parquet files. It works by maintaining a transaction log (`_delta_log/`) alongside the Parquet data files. It is the default and recommended table format in Databricks.

---

**Q44. What does ACID mean in Delta Lake?**  
- **Atomicity** — a write either fully succeeds or fully fails (no partial writes)
- **Consistency** — schema enforcement ensures only valid data is written
- **Isolation** — readers see a consistent snapshot even during concurrent writes
- **Durability** — committed data is never lost

---

**Q45. What is time travel in Delta Lake?**  
Time travel lets you query a Delta table at any previous version or timestamp. Example:
```python
spark.read.format("delta").option("versionAsOf", 5).load("/path")
spark.read.format("delta").option("timestampAsOf", "2024-01-01").load("/path")
```
Useful for auditing, debugging, reproducing past results, and recovering from accidental deletes.

---

**Q46. What is MERGE and when should you use it?**  
MERGE (upsert) atomically inserts new records and updates existing ones in a single operation based on a join condition. Use it for:
- CDC (Change Data Capture) from source systems
- Handling late-arriving data
- SCD Type 2 implementations
- Deduplication on load

---

**Q47. What is the difference between `overwrite` and `append` in Delta writes?**  
`overwrite` replaces all data in the table. `append` adds new rows without touching existing ones. For partitioned overwrites, use `.option("replaceWhere", "date = '2024-01-15'")` to overwrite only a specific partition.

---

**Q48. What is OPTIMIZE in Delta Lake?**  
`OPTIMIZE` compacts many small Parquet files into fewer large files (~1GB each). This significantly speeds up queries that would otherwise need to open thousands of tiny files. Run it regularly (e.g., weekly) on high-write tables.

---

**Q49. What is Z-Ordering?**  
Z-Ordering co-locates related rows in the same files by sorting data along multiple columns simultaneously. When you filter on a Z-Ordered column, Spark can skip most files (data skipping). Use it on columns you frequently filter on: `OPTIMIZE table ZORDER BY (customer_id, order_date)`.

---

**Q50. What is VACUUM and what are the risks?**  
VACUUM removes Parquet files that are no longer referenced by the Delta transaction log (old versions, deleted data). After VACUUM, you can no longer time-travel before the retention period. Default retention is 7 days. Always confirm you don't need old versions before vacuuming.

---

**Q51. What is Liquid Clustering?**  
Liquid Clustering is Databricks' next-generation alternative to Hive-style partitioning and Z-Ordering. It automatically and adaptively clusters data as you write, without needing OPTIMIZE runs. Defined at table creation: `CLUSTER BY (customer_id, date)`.

---

**Q52. What is Change Data Feed (CDF)?**  
CDF tracks all row-level changes (inserts, updates, deletes) made to a Delta table and makes them available to read as a stream of change events. Each change record has a `_change_type` column: `insert`, `update_preimage`, `update_postimage`, or `delete`.

---

**Q53. What is SCD Type 2?**  
Slowly Changing Dimension Type 2 is a pattern to track historical changes in dimension data. Instead of overwriting old records, you expire them (set `end_date`, `is_current = false`) and insert a new record with the updated values. This preserves full history.

---

## SECTION 6: Advanced Transformations (Day 6)

**Q54. What is a window function?**  
A window function computes an aggregate over a "window" (partition) of rows without collapsing them. Unlike `groupBy`, all rows remain. Define a window with `Window.partitionBy().orderBy()`, then apply functions like `rank()`, `lag()`, `sum().over(window)`.

---

**Q55. What is the difference between ROW_NUMBER, RANK, and DENSE_RANK?**  
- `ROW_NUMBER` — always unique (1,2,3,4...) even for ties
- `RANK` — same number for ties, then skips (1,1,3,4...)
- `DENSE_RANK` — same number for ties, no skipping (1,1,2,3...)

---

**Q56. What are LAG and LEAD used for?**  
`LAG(col, n)` returns the value of a column n rows BEFORE the current row. `LEAD(col, n)` returns the value n rows AFTER. Used for day-over-day comparisons, growth rates, and detecting sequences.

---

**Q57. What is EXPLODE and when do you need it?**  
`explode(array_col)` converts each element of an array into a separate row. If a row has an array `["a","b","c"]`, it becomes three rows: `"a"`, `"b"`, `"c"`. Use `explode_outer` to keep rows with null or empty arrays.

---

**Q58. What is a UDF and when should you avoid it?**  
A UDF (User-Defined Function) is a custom Python function registered with Spark. Regular Python UDFs are slow because data must be serialized between JVM and Python one row at a time. Prefer: built-in Spark functions (fastest) → Pandas UDFs (vectorized, 10× faster than row UDFs) → Python UDFs (only when no alternative).

---

**Q59. What is a Pandas UDF?**  
A Pandas UDF (also called vectorized UDF) uses Apache Arrow to transfer data between JVM and Python as batches (Pandas Series/DataFrame). It's decorated with `@pandas_udf(return_type)` and is ~10× faster than a regular Python UDF.

---

**Q60. When should I use a broadcast join?**  
Use `broadcast(small_df)` in a join when one table is small enough to fit in executor memory (< 50MB typically, configured by `spark.sql.autoBroadcastJoinThreshold`). Spark sends the small table to every executor, eliminating the shuffle entirely.

---

## SECTION 7: Performance Optimization (Day 7)

**Q61. What is data skew and how do you detect it?**  
Data skew occurs when one partition has far more data than others, causing one executor to take much longer while others idle. Detect it in the Spark UI: in the Tasks tab, look for one task taking 10× longer than others. In code: `df.groupBy("key").count().orderBy(col("count").desc()).show(10)`.

---

**Q62. How do you fix data skew?**  
- **Broadcast** the small side of the join (best option if applicable)
- **Salt the key** — add a random suffix to the skewed key to spread it across partitions, then remove the salt after
- **Enable AQE skew join** — `spark.sql.adaptive.skewJoin.enabled = true` handles it automatically in Spark 3.x
- **Pre-aggregate** before joining to reduce the data size

---

**Q63. What is Adaptive Query Execution (AQE)?**  
AQE is a Spark 3.x feature that re-optimizes query plans at runtime based on actual statistics (not estimated ones). It automatically: coalesces small shuffle partitions, handles skewed joins, and switches join strategies (shuffle join → broadcast join). Enable with `spark.sql.adaptive.enabled = true`.

---

**Q64. What are the most important Spark configuration settings?**  
```
spark.sql.adaptive.enabled = true               (enable AQE)
spark.sql.shuffle.partitions = auto             (AQE auto-tunes)
spark.sql.autoBroadcastJoinThreshold = 50MB     (larger broadcast threshold)
spark.sql.files.maxPartitionBytes = 134217728   (128MB per partition)
spark.sql.sources.partitionOverwriteMode = dynamic
```

---

**Q65. What is predicate pushdown?**  
Predicate pushdown is an optimization where the filter (predicate) is pushed as close to the data source as possible. With Parquet, Spark can skip entire row groups that don't match the filter without reading them. With partitioned tables, Spark skips irrelevant partition directories entirely.

---

**Q66. What is the small file problem?**  
When a table has thousands or millions of tiny files (e.g., from many small incremental appends), queries are slow because each file requires a file open/close overhead. Fix by running `OPTIMIZE` on Delta tables to compact files, or using `coalesce/repartition` before writing.

---

**Q67. When should I cache a DataFrame?**  
Cache when: (1) the DataFrame is used in 2+ actions, AND (2) it's expensive to compute (involves joins, large reads). Don't cache when: used only once, too large for memory, or simple/cheap to recompute. Always call `.count()` after `.cache()` to fill the cache, and `.unpersist()` when done.

---

## SECTION 8: Workflow Orchestration (Day 8)

**Q68. What is a Databricks Job?**  
A Databricks Job is a scheduled, automated execution of one or more notebooks or Python scripts. Jobs can have multiple tasks with dependencies, run on job clusters, retry on failure, send alerts, and integrate with external systems. They are the production deployment unit in Databricks.

---

**Q69. What are Databricks Widgets?**  
Widgets are UI parameters that can be passed to notebooks. They appear as UI controls at the top of the notebook and can be set by Jobs, other notebooks, or users manually. Get values with `dbutils.widgets.get("param_name")`.

---

**Q70. How do you pass secrets to a Databricks notebook?**  
Always use Databricks Secrets: `dbutils.secrets.get(scope="scope-name", key="key-name")`. Create secrets via the Databricks CLI or Secrets API. Secret values are masked in notebook output. Never use widgets or hardcode passwords.

---

**Q71. What is `dbutils.notebook.run()`?**  
It runs another notebook programmatically and returns its exit value (set via `dbutils.notebook.exit("result")`). Used for modular pipelines where one notebook orchestrates several child notebooks. Has a timeout parameter.

---

**Q72. What is a Job Repair Run?**  
When a multi-task Job partially fails, "Repair Run" lets you re-run only the failed tasks — not the entire pipeline. This saves time and compute. Available in Job Runs → select failed run → click "Repair Run."

---

**Q73. What is Delta Live Tables (DLT)?**  
DLT is Databricks' declarative pipeline framework. You define your tables with Python decorators (`@dlt.table`) and data quality expectations (`@dlt.expect`). Databricks manages execution, dependency resolution, retries, monitoring, and data quality enforcement automatically.

---

**Q74. What cron expression runs a job every day at 2 AM UTC?**  
`0 0 2 * * ?` (Quartz cron format used by Databricks)  
Format: `seconds minutes hours day-of-month month day-of-week`

---

**Q75. What is the difference between trigger `availableNow` and `processingTime`?**  
- `availableNow=True` — processes all currently available data in one batch, then stops (like a scheduled micro-batch)
- `processingTime="1 minute"` — runs continuously, triggering a new batch every 1 minute
- `once=True` — processes one batch and stops (deprecated, use `availableNow`)

---

## SECTION 9: Real-World & Architecture (Day 9)

**Q76. How do you approach a new data engineering project?**  
1. Understand the source (format, volume, frequency, schema)
2. Understand the target (who uses it, what queries, SLA)
3. Choose architecture (batch vs streaming, Medallion)
4. Estimate data volume and choose cluster size
5. Build Bronze → Silver → Gold
6. Add monitoring, DQ checks, and alerts
7. Test in staging before production

---

**Q77. How do you validate a data migration?**  
- Row count: source rows = target rows
- Metric sums: SUM of revenue, COUNT of orders must match
- Distinct values: unique customer IDs must match
- Sample comparison: random 1000 rows spot-check
- Business rule validation: no negative amounts, valid statuses

---

**Q78. When should I use PySpark vs Spark SQL?**  
PySpark is better for complex Python logic, reusable functions, UDFs, and dynamic query building. Spark SQL is better for SQL-fluent teams, simple transformations, ad-hoc exploration, and business analyst collaboration. In practice, mix them freely — they're the same execution engine.

---

**Q79. What is CDC and how do you handle it?**  
CDC (Change Data Capture) streams changes (inserts, updates, deletes) from source databases. In Databricks, handle CDC by: capturing changes via Debezium/AWS DMS → landing in Bronze → applying changes with MERGE to Silver. The `_change_type` column identifies insert/update/delete.

---

**Q80. How do you choose between streaming and batch processing?**  
- **Daily reports, no real-time need** → Daily batch job
- **Dashboards refreshed hourly** → Hourly batch or micro-batch
- **Fraud detection, recommendations** → Streaming (minutes latency)
- **Real-time notifications** → Streaming + Kafka (seconds latency)

---

## SECTION 10: Production Best Practices (Day 10)

**Q81. What makes a pipeline idempotent?**  
An idempotent pipeline produces the same result when run multiple times for the same input. Achieve this using: MERGE (upsert), DELETE + INSERT with partition filter, `replaceWhere` in Delta writes, or overwriting a specific partition.

---

**Q82. What is a data lineage and why does it matter?**  
Data lineage tracks the origin of data — which source it came from, which transformations were applied, and which downstream tables depend on it. In Unity Catalog, lineage is tracked automatically. It helps with debugging, impact analysis (what breaks if source changes?), and compliance.

---

**Q83. What should an audit log table contain?**  
- `run_id` — unique identifier for the pipeline run
- `pipeline_name` — which pipeline
- `step` — which step (bronze, silver, gold)
- `status` — success / failed
- `records_processed` — row count
- `error_message` — if failed
- `run_timestamp` — when it ran
- `batch_date` — which date's data was processed

---

**Q84. What is a service principal and why use one?**  
A service principal is a non-human account (like a robot account) used to run production jobs. It's more secure than running jobs under a personal user account because: it can be tightly permissioned, its credentials can be rotated, and it doesn't break when an employee leaves.

---

**Q85. How do you handle a pipeline that runs too slowly in production?**  
1. Check Spark UI — identify the slowest stage
2. Check for data skew (one task 10× slower)
3. Check file count (small file problem → OPTIMIZE)
4. Check shuffle partitions (default 200 may be wrong)
5. Check if broadcast join is possible
6. Enable AQE if not already enabled
7. Review filter placement (filter early!)
8. Consider increasing cluster size as last resort

---

**Q86. What is the 3-level namespace in Unity Catalog?**  
`catalog.schema.table`
- **Catalog** — top-level container (e.g., `prod`, `dev`, `data_lake`)
- **Schema** — database within a catalog (e.g., `bronze`, `silver`, `gold`)
- **Table/View** — individual data object

Example: `prod_catalog.gold.daily_revenue`

---

**Q87. How do you test a Databricks notebook?**  
- Unit test transformation functions using `pytest` or `unittest`
- Create a test notebook with small synthetic data
- Compare output to expected result
- Use staging environment with production-like data
- Add assertion checks: `assert df.count() > 0`

---

**Q88. What's the difference between `saveAsTable` and `save`?**  
- `save(path)` — writes Delta files to a path, no table registered in metastore
- `saveAsTable("catalog.schema.table")` — writes Delta files AND registers the table in Unity Catalog (can be queried by name from SQL, BI tools, etc.)

Prefer `saveAsTable` for production tables that need to be discoverable.

---

**Q89. What is schema enforcement in Delta Lake?**  
When writing to a Delta table, Spark checks that the DataFrame schema matches the table schema. If it doesn't match, the write fails. This prevents silent data corruption from source schema changes. Override with `mergeSchema=true` (to add columns) or `overwriteSchema=true` (to replace schema).

---

**Q90. What should a pipeline runbook contain?**  
- Pipeline name and purpose
- Schedule (cron expression, timezone)
- Input sources (paths, tables, expected arrival time)
- Output tables and SLA (when it must be ready)
- Retry policy
- Who to contact if it fails
- Steps to manually re-run
- Known issues and workarounds
- How to check data quality after run

---

## QUICK REFERENCE

### Essential Imports
```python
from pyspark.sql import SparkSession
from pyspark.sql.functions import col, lit, when, coalesce, current_timestamp
from pyspark.sql.functions import sum, avg, count, countDistinct, min, max
from pyspark.sql.functions import to_date, to_timestamp, date_add, datediff
from pyspark.sql.functions import upper, lower, trim, regexp_replace
from pyspark.sql.functions import explode, array_contains, from_json, to_json
from pyspark.sql.functions import row_number, rank, dense_rank, lag, lead
from pyspark.sql.functions import broadcast, input_file_name, spark_partition_id
from pyspark.sql.types import StructType, StructField, StringType, IntegerType, DoubleType
from pyspark.sql.window import Window
from delta.tables import DeltaTable
```

### Common Patterns Cheatsheet
```python
# Top N per group
Window.partitionBy("state").orderBy(col("amount").desc()) → row_number() <= N

# Running total
Window.orderBy("date").rowsBetween(Window.unboundedPreceding, Window.currentRow)

# 7-day rolling sum
Window.orderBy("date").rowsBetween(-6, 0)

# Upsert
DeltaTable.forPath(spark, path).alias("t").merge(src.alias("s"), "t.id = s.id") \
    .whenMatchedUpdateAll().whenNotMatchedInsertAll().execute()

# Time travel
spark.read.format("delta").option("versionAsOf", 0).load(path)

# Incremental write
df.write.format("delta").mode("append").save(path)

# Partition overwrite
df.write.format("delta").mode("overwrite").option("replaceWhere","date='2024-01-01'").save(path)

# Broadcast join
orders.join(broadcast(small_lookup), on="id", how="inner")

# Null-safe fill
df.withColumn("col", coalesce(col("col"), lit("default")))
```

---

*Reference document for 10-day Databricks Data Engineering Training*
