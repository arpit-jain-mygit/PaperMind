# Apache Spark: Data Engineering Leadership Guide

> Built for leadership interviews. Simple, realistic explanations with practical depth.

## Table of Contents
1. [Core Architecture](#core-architecture)
2. [Execution Model](#execution-model)
3. [DataFrames & Spark SQL](#dataframes--spark-sql)
4. [Structured Streaming](#structured-streaming)
5. [Performance & Tuning](#performance--tuning)
6. [Interview Questions](#interview-questions)

---

## Core Architecture

### The One Thing to Understand First

Spark solves a problem: **how do you process data that's too big for one machine?**

Answer: **distribute the data across a cluster, process it in parallel, then combine results.**

Spark is the framework that hides the complexity of doing this reliably.

### Cluster Components

**Driver** (your laptop or a master node)
- Runs your main program
- Decides what to do: "read this file, filter by X, group by Y, write output"
- Does NOT process data itself — just orchestrates

**Executors** (worker nodes)
- Actually process the data
- Run the tasks the driver gives them
- Return results to the driver

**SparkContext**
- The connection between your program and the cluster
- Created once per application: `SparkSession.builder().getOrCreate()`

### Think of It Like This

**Restaurant analogy:**
- Driver = manager (takes orders, plans kitchen work, decides what to cook)
- Executors = cooks (receive tasks, actually make food)
- Cluster Manager = HR (allocates cooks to the restaurant, can be Yarn, Mesos, K8s, or local)

---

## Execution Model

### DAG: Directed Acyclic Graph

When you write Spark code, you're not telling Spark *how* to compute. You're describing *what* you want.

```python
df = spark.read.parquet("data.parquet")           # Read
df = df.filter(df.amount > 100)                   # Transformation 1
df = df.groupBy("category").sum()                 # Transformation 2
df.write.mode("overwrite").parquet("output")      # Action
```

Spark builds a DAG:
- Each transformation is a node
- Dependencies flow from one to the next
- **Lazy evaluation**: nothing actually happens until you call an action

```
Read -> Filter -> GroupBy -> Write
```

### Why Lazy Evaluation Matters

```python
df = spark.read.parquet("data.parquet")  # Planned, not executed
df = df.filter(df.amount > 100)          # Planned, not executed
result = df.collect()                    # NOW everything runs at once
```

**Benefit:** Spark can optimize the entire chain before executing. It can see "oh, you filter first then group — I should filter first to reduce data moved around."

### Stages and Tasks

Spark breaks the DAG into **stages** at shuffle boundaries.

A shuffle = "rearrange data across the network"

Example:
```python
df.groupBy("category").sum()  # SHUFFLE: data must move across network
```

Each stage has **tasks** — parallel units of work.

- If you have 100GB split across 10 files (10GB each), you get 10 tasks per stage (one per partition)
- Each executor runs 1+ tasks in parallel (depends on cores available)

**Timeline:**
1. Driver optimizes the DAG
2. Divides into stages
3. Sends stage 0 tasks to executors
4. Executors run tasks in parallel
5. Wait for all tasks to finish
6. Move to stage 1
7. Repeat

---

## DataFrames & Spark SQL

### DataFrame = Distributed Table

```python
# Create from file
df = spark.read.parquet("users.parquet")

# Create from list (useful for testing)
df = spark.createDataFrame(
    [(1, "Alice"), (2, "Bob")],
    ["id", "name"]
)
```

A DataFrame has:
- **Schema** (column names + types)
- **Partitions** (data split across executors)
- **Lazy transformations** (nothing runs until you call an action)

### Transformations vs. Actions

**Transformations** = return a new DataFrame (lazy)
```python
df.filter(df.age > 30)
df.select("name", "age")
df.groupBy("department").count()
df.join(other_df, "id")
```

**Actions** = force computation (eager)
```python
df.collect()          # Return all rows to driver
df.count()            # Count rows
df.show()             # Print first rows
df.write.parquet()    # Write to disk
```

### Spark SQL: Same Logic, SQL Syntax

Register a DataFrame as a table, then query it with SQL:

```python
df.createOrReplaceTempView("users")
result = spark.sql("""
    SELECT department, COUNT(*) as count
    FROM users
    WHERE age > 30
    GROUP BY department
""")
```

**Under the hood:** Spark converts this SQL to the same DAG as the DataFrame API.

### The Catalyst Optimizer

Spark's secret weapon. Automatically rewrites your query to be faster.

**Example:**
```python
df = spark.read.parquet("data.parquet")      # 1 billion rows
df = df.filter(df.amount > 1000)             # Down to 10 million rows
df = df.select("id", "amount")               # Only 2 columns
result = df.count()
```

**Without optimization:** Read all data, filter, select, count.

**With Catalyst:**
```
1. Pushdown: read only needed columns + filter to where clause
2. Prune: drop the "amount" column AFTER counting (wait, we don't need it!)
3. Result: read only "id" column, apply filter, count
```

This can make queries **100x faster**.

---

## Structured Streaming

### The Idea

Normal batch: "Process this file"

Streaming: "Process data as it arrives, update results continuously"

Spark handles this by treating incoming data as **micro-batches** (batches arriving every few seconds).

### Architecture

```
Source (Kafka, file system, socket)
    ↓
Micro-batch 1 (processes data from time 0-1s)
    ↓
State Store (intermediate results: counts, aggregates)
    ↓
Output (console, file, database)
    ↓
Micro-batch 2 (processes data from time 1-2s, uses state from batch 1)
```

### Example: Real-Time Counts

```python
df = spark.readStream.format("kafka").option("kafka.bootstrap.servers", "localhost:9092").load()

# Deserialize JSON
from pyspark.sql.functions import from_json, col
schema = "event_type STRING, amount DOUBLE, timestamp LONG"
df = df.select(from_json(col("value").cast("string"), schema).alias("data")).select("data.*")

# Group and count
result = df.groupBy(window("timestamp", "1 minute"), "event_type").count()

# Output to console
query = result.writeStream.format("console").start()
query.awaitTermination()
```

### Event Time vs. Processing Time

**Processing time:** When Spark processes the event

**Event time:** When the event actually happened (in the data)

**Why it matters:** In the real world, events arrive out of order and late.

```
Event at 10:00 arrives at 10:05
Event at 10:03 arrives at 10:02
```

With event time, Spark can:
1. Assign events to the correct time bucket ("10:00 minute")
2. Wait a bit for late arrivals (watermarking)
3. Produce correct results despite disorder

```python
result = df \
    .withWatermark("timestamp", "5 minutes") \  # Wait 5 min for late data
    .groupBy(window("timestamp", "1 minute"), "event_type") \
    .count()
```

---

## Performance & Tuning

### The Fundamental Bottleneck

Most Spark jobs fail on one of these:

1. **Shuffles** (data moving across network) — ~70% of slowness
2. **Skewed data** (some partitions have 1GB, others have 100MB) — tasks finish unevenly
3. **Spilling** (data doesn't fit in memory, goes to disk) — 10x slower than memory
4. **Small partitions** (too many tiny tasks) — overhead dominates

### Rule 1: Reduce Shuffle

**Shuffle happens at:**
- `groupBy()`, `join()`, `distinct()`, `repartition()`

**Cost:** All data must move across the network. Expensive.

**Reduce by:**
1. Filter before shuffle
   ```python
   # BAD
   df.groupBy("category").sum()  # All rows shuffled
   
   # GOOD
   df.filter(df.valid == True).groupBy("category").sum()  # Fewer rows shuffled
   ```

2. Use narrower columns in shuffle keys
   ```python
   # BAD (whole row becomes shuffle key)
   df.repartition(200, df.id, df.name, df.email)
   
   # GOOD (only id shuffled)
   df.repartition(200, df.id)
   ```

### Rule 2: Partition Count

Default partitions = number of input files.

**Too few partitions:** Executors sit idle (parallelism wasted)

**Too many partitions:** Overhead dominates (1 million tiny 1KB files)

**Sweet spot:** 128 MB per partition

```python
# If input is 1GB, aim for 8 partitions
df = spark.read.parquet("data.parquet").repartition(8)

# If groupBy result will be 10M rows, aim for 50 partitions
df.groupBy("id").sum().repartition(50)
```

### Rule 3: Cache for Reuse

If you use the same DataFrame multiple times, cache it:

```python
df = spark.read.parquet("data.parquet")
df.cache()  # Keep in memory on executors

result1 = df.filter(df.amount > 100).count()  # Uses cached df
result2 = df.filter(df.amount < 50).count()   # Uses cached df

df.unpersist()  # Release memory when done
```

**Cost:** Memory on executors. **Benefit:** No re-reading from disk.

### Rule 4: Broadcast Small Tables

If you join a huge table with a tiny table, broadcast the tiny one:

```python
# WITHOUT broadcast: Spark shuffles both tables (expensive)
big_df.join(small_df, "id")

# WITH broadcast: Spark sends small_df to every executor (cheap)
from pyspark.sql.functions import broadcast
big_df.join(broadcast(small_df), "id")
```

**Works only if small_df < executor memory** (~2GB typically)

### Rule 5: Understand Your Executor Memory

```
Executor memory = 4GB total
  ├─ Execution: 60% (2.4GB) — tasks running, shuffles, joins
  ├─ Storage: 30% (1.2GB) — cached data
  └─ Reserve: 10% (0.4GB) — overhead
```

If you cache 2GB and run a shuffle, you'll spill to disk.

**Solution:** Either cache less, request more memory, or tune the ratio.

```python
spark = SparkSession.builder() \
    .config("spark.executor.memory", "8g") \
    .config("spark.memory.fraction", 0.8) \
    .config("spark.memory.storageFraction", 0.5) \
    .getOrCreate()
```

### Debugging Performance

**Use the Spark UI** (http://localhost:4040 while app runs):
- **Stages tab:** Where is time being spent?
- **Tasks tab:** Are tasks finishing at different times? (skew)
- **Executors tab:** Is memory filling up? (spill risk)

**Questions to ask:**
1. Which stage is slowest?
2. Is any task much slower than others? (skew)
3. Is the shuffle size huge? (data explosion)
4. Is memory usage high? (spill risk)

---

## Interview Questions

### Leadership/Architecture Level

**Q: Design a system to process 1TB of transaction logs daily**

Sample answer:
- Read from S3/HDFS as Parquet (columnar, compressed)
- Partition by date (source partition awareness)
- Filter corrupt/test data early
- GroupBy merchant → daily stats (one shuffle)
- Cache results if multiple consumers
- Write to Parquet (partition by date, day for queries)
- Run on 10 r5.2xlarge nodes (memory-optimized for shuffle)
- Schedule as daily batch job (Airflow/Kubernetes)

**Q: When would you NOT use Spark?**

Real answers:
- Small data (<1GB) — Pandas is faster
- Low-latency needs (<1s) — use streaming DB instead
- Complex state machines — use stateful services
- Tight ML feedback loops — use TensorFlow locally

**Q: Talk through a Spark performance issue**

Framework:
1. Identify bottleneck (shuffle? memory? skew?)
2. Propose fix (reduce shuffle, add partitions, cache, broadcast)
3. Verify with UI metrics
4. Measure impact (time, cost)

**Q: How would you handle data skew?**

Strategies:
- Add a random suffix to skew key before shuffle (salting)
  ```python
  from pyspark.sql.functions import rand
  df = df.withColumn("salted_id", concat(col("id"), lit("_"), (rand() * 100).cast("int")))
  ```
- Separate hot keys, process independently
- Increase partitions (helps distribute skewed keys)

**Q: Streaming pipeline for fraud detection — how would you ensure accuracy?**

Design:
- Use event time, not processing time
- Set watermark (e.g., 1 hour late arrivals)
- Use stateful operations (store user transaction history)
- Output to idempotent sink (DB with unique constraint)
- Monitor late-arriving data rate

---

## Key Takeaways

| Concept | Why It Matters |
|---------|----------------|
| **DAG & Lazy Eval** | Enables optimization; lets Spark see the full picture |
| **Partitions** | Parallelism directly tied to partition count |
| **Shuffles** | Most expensive operation; reduce aggressively |
| **Catalyst Optimizer** | Makes naive queries fast automatically |
| **Event Time in Streaming** | Correctness despite disorder and late arrivals |
| **Memory Management** | Spill to disk kills performance; cache carefully |
| **Broadcast** | Small table joins should be broadcast |
| **Skew** | A few partitions slow down entire job; detect via UI |

---

## Next Steps

1. **Install Spark locally:** `pip install pyspark`
2. **Run a simple job:**
   ```python
   from pyspark.sql import SparkSession
   spark = SparkSession.builder.appName("learning").getOrCreate()
   df = spark.range(0, 1000000).repartition(10)
   print(df.count())  # Watch the Spark UI
   ```
3. **Explore the Spark UI:** Open http://localhost:4040
4. **Read a Spark job's DAG:** Look at the Stages tab to understand dependencies
5. **Practice on real data:** Kaggle datasets + Spark

---

## Quick Reference: Common Patterns

### Read Parquet with Partition Pruning
```python
df = spark.read.parquet("s3://bucket/data/year=2024/month=*/day=*")
```

### Efficient Join
```python
from pyspark.sql.functions import broadcast
df_result = big_df.join(broadcast(small_df), "id")
```

### Window Function for Ranking
```python
from pyspark.sql.functions import row_number, rank
from pyspark.sql.window import Window

window_spec = Window.partitionBy("department").orderBy(col("salary").desc())
df = df.withColumn("rank", rank().over(window_spec))
```

### Count Unique Values Efficiently
```python
from pyspark.sql.functions import approx_count_distinct
df.select(approx_count_distinct("user_id"))  # Fast, ~99% accurate
```

### Pivot (Transpose Rows to Columns)
```python
pivoted = df.groupBy("date").pivot("category").sum("amount")
# Date | Electronics | Clothing | Food
```

