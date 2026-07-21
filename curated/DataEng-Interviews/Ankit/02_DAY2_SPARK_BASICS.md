# Day 2: Apache Spark Basics
## Architecture, RDDs, DataFrames & Execution Model

**Duration**: 1 hour | **Difficulty**: Beginner → Intermediate

---

## 🎯 Learning Objectives

- ✅ Understand WHY Spark was invented (vs traditional processing)
- ✅ Explain Spark's distributed architecture (Driver / Executors)
- ✅ Know the difference between RDD, DataFrame, Dataset
- ✅ Understand lazy evaluation and the DAG
- ✅ Explain Transformations vs Actions
- ✅ Understand stages, tasks, and partitions

---

## 📚 Content

### 2.1 Why Spark Exists — The Problem It Solves

**Before Spark: The Problem**

Imagine you have 10 TB of log files to process. A single machine:
- 🐢 Takes hours/days to read and compute
- 💥 Often runs out of memory (RAM)
- ❌ Can't scale — you're stuck with one machine's limits

**The Old Solution: Hadoop MapReduce**
- Distributed across many machines ✅
- Writes intermediate results to disk after every step ❌ (very slow!)

**Spark's Innovation:**
- Distributes across many machines ✅
- Keeps data **in-memory** between steps ✅ (up to 100× faster than Hadoop)
- Fault tolerant — if a machine fails, Spark recovers automatically ✅

**Real-world analogy:**  
Imagine building a house. MapReduce = one worker who builds a wall, takes a nap, then builds the next wall. Spark = 100 workers all building simultaneously, staying on-site the whole time.

---

### 2.2 Spark Architecture

```
┌─────────────────────────────────────────────────┐
│                 SPARK APPLICATION                │
│                                                  │
│  ┌─────────────────────────────────────────┐    │
│  │           DRIVER PROGRAM                │    │
│  │  ┌─────────────┐  ┌─────────────────┐  │    │
│  │  │ SparkContext │  │  Your Code      │  │    │
│  │  │ (SparkSession│  │  (Python/SQL)   │  │    │
│  │  └──────┬──────┘  └─────────────────┘  │    │
│  │         │ Manages and coordinates       │    │
│  └─────────┼───────────────────────────────┘    │
│             │                                    │
│       ┌─────┼──────────────────┐                │
│       │     │   CLUSTER MANAGER│                │
│       │     │   (Databricks)   │                │
│       └─────┼──────────────────┘                │
│             │                                    │
│    ┌────────┴──────────────────────┐            │
│    │                               │            │
│ ┌──▼──────────┐         ┌──────────▼──────┐    │
│ │  WORKER 1   │         │   WORKER 2      │    │
│ │ ┌─────────┐ │         │ ┌─────────────┐ │    │
│ │ │Executor │ │         │ │  Executor   │ │    │
│ │ │ Task1   │ │         │ │  Task3      │ │    │
│ │ │ Task2   │ │         │ │  Task4      │ │    │
│ │ └─────────┘ │         │ └─────────────┘ │    │
│ │  Memory     │         │  Memory         │    │
│ └─────────────┘         └─────────────────┘    │
└─────────────────────────────────────────────────┘
```

**Key Components:**

| Component | Role | Analogy |
|-----------|------|---------|
| **Driver** | Brain — coordinates everything | Project manager |
| **Executor** | Muscle — does actual computation | Worker on site |
| **SparkSession** | Entry point to Spark API | Reception desk |
| **Cluster Manager** | Allocates resources | HR / Resource manager |
| **Task** | Smallest unit of work | Single construction step |
| **Stage** | Group of tasks without shuffle | One floor of a building |
| **Job** | Complete set of stages | Entire building |

---

### 2.3 SparkSession — Your Entry Point

In Databricks, `spark` is already created for you automatically.

```python
# In a standalone environment, you'd create it like this:
from pyspark.sql import SparkSession

spark = SparkSession.builder \
    .appName("MyDataPipeline") \
    .config("spark.sql.shuffle.partitions", "200") \
    .getOrCreate()

# In Databricks — it's already available:
print(spark)
# Output: <pyspark.sql.session.SparkSession object at 0x...>

# SparkContext (low-level, rarely used)
print(sc)
# Output: <SparkContext master=local[*]>
```

---

### 2.4 RDD vs DataFrame vs Dataset

These are the **three data abstractions** in Spark. Understanding their differences is critical.

#### **RDD (Resilient Distributed Dataset)**
- The **original** Spark data structure (2009)
- Unstructured — just a collection of objects
- No schema, no optimization
- Low-level API — you control everything

```python
# Creating an RDD
rdd = sc.parallelize([1, 2, 3, 4, 5])
result = rdd.map(lambda x: x * 2).filter(lambda x: x > 4)
result.collect()
# Output: [6, 8, 10]

# String RDD (no structure)
text_rdd = sc.textFile("/path/to/file.txt")
word_counts = text_rdd.flatMap(lambda line: line.split(" ")) \
                      .map(lambda word: (word, 1)) \
                      .reduceByKey(lambda a, b: a + b)
```

#### **DataFrame**
- Structured table (like a SQL table or Pandas DataFrame)
- Has columns with names and types (schema)
- Optimized by Spark's Catalyst optimizer
- **Most commonly used in data engineering**

```python
# Creating a DataFrame
df = spark.createDataFrame([
    (1, "Alice", 25, "NY"),
    (2, "Bob", 30, "CA"),
    (3, "Charlie", 35, "TX")
], ["id", "name", "age", "state"])

df.printSchema()
# root
#  |-- id: long (nullable = true)
#  |-- name: string (nullable = true)
#  |-- age: long (nullable = true)
#  |-- state: string (nullable = true)

df.show()
# +---+-------+---+-----+
# | id|   name|age|state|
# +---+-------+---+-----+
# |  1|  Alice| 25|   NY|
# |  2|    Bob| 30|   CA|
# |  3|Charlie| 35|   TX|
# +---+-------+---+-----+
```

#### **Dataset**
- Type-safe version of DataFrame (Scala/Java only)
- Python doesn't really have Datasets (PySpark always uses DataFrames)
- For us: ignore this — DataFrames are what we use

#### **Comparison**

| Aspect | RDD | DataFrame |
|--------|-----|-----------|
| **Schema** | None | Defined (columns + types) |
| **Optimization** | None | Catalyst optimizer |
| **Ease of Use** | Hard | Easy |
| **SQL Support** | No | Yes |
| **Performance** | Slower | Faster |
| **Use Case** | Custom low-level | 99% of data work |
| **When to Use** | Complex custom logic only | Almost always |

**For Data Engineers: Always use DataFrames.**  
Only use RDDs when you have a very specific use case that DataFrames can't handle.

---

### 2.5 Lazy Evaluation — Spark's Secret Weapon

This is one of the **most important** Spark concepts.

**What is Lazy Evaluation?**

When you write a Spark transformation, Spark does NOT execute it immediately. It waits until you ask for results.

```python
# These DO NOT execute immediately — they are "lazy"
df = spark.range(1000000)      # No execution yet
df2 = df.filter(df.id > 500)   # Still no execution
df3 = df2.withColumn("doubled", df2.id * 2)  # Still no execution
df4 = df3.groupBy().sum("doubled")  # Still no execution!

# THIS triggers execution — "eager" / "action"
result = df4.collect()   # NOW Spark executes all 4 steps together
print(result)
```

**Why is this useful?**

Spark can see ALL your transformations at once and **optimize them** before running.

**Example of optimization:**
```python
# You wrote this:
df.filter(col("age") > 25).select("name", "age").filter(col("age") > 30)

# Spark optimizes to:
# 1. Combine both filters → filter(age > 30)
# 2. Only read needed columns
# Result: much faster execution!
```

**Analogy**: Lazy evaluation is like a chef who waits to see the entire order before cooking — they can batch steps efficiently instead of making one dish at a time.

---

### 2.6 Transformations vs Actions

This is **fundamental** — you must understand this distinction.

#### **Transformations**
- Return a new DataFrame (don't execute immediately)
- Build the execution plan (DAG)
- Examples: `filter`, `select`, `withColumn`, `join`, `groupBy`

```python
# ALL of these are transformations — no Spark execution yet
df_filtered = df.filter(df.age > 25)          # Lazy
df_selected = df_filtered.select("name")       # Lazy
df_renamed = df_selected.withColumnRenamed("name", "full_name")  # Lazy
```

#### **Actions**
- Trigger actual Spark execution
- Return results to driver
- Examples: `show`, `count`, `collect`, `write`, `first`, `take`

```python
# THESE trigger execution
df.show()            # ← EXECUTION happens HERE
df.count()           # ← EXECUTION happens HERE
df.collect()         # ← EXECUTION happens HERE (careful with large data!)
df.first()           # ← EXECUTION happens HERE
df.write.parquet("/output")  # ← EXECUTION happens HERE
```

#### **Complete List**

| Transformations (Lazy) | Actions (Eager) |
|------------------------|----------------|
| `filter()` / `where()` | `show()` |
| `select()` | `count()` |
| `withColumn()` | `collect()` |
| `drop()` | `take(n)` |
| `join()` | `first()` |
| `groupBy()` | `write` |
| `orderBy()` / `sort()` | `foreach()` |
| `union()` / `unionAll()` | `toPandas()` |
| `distinct()` | `printSchema()` |
| `limit()` | `describe()` |

---

### 2.7 Spark Execution Model: DAG, Jobs, Stages, Tasks

When an action is triggered, Spark creates an execution plan called a **DAG** (Directed Acyclic Graph).

```
YOUR CODE → DAG → Jobs → Stages → Tasks → Execution
```

#### **Step-by-Step Breakdown:**

**Step 1: You write transformations**
```python
df1 = spark.read.parquet("/data/orders")
df2 = df1.filter(col("amount") > 100)
df3 = df2.groupBy("customer_id").sum("amount")
df3.show()  # ← Action! This starts everything
```

**Step 2: Spark builds a DAG**
```
Read(orders) → Filter(amount>100) → GroupBy(customer_id) → Sum(amount)
```

**Step 3: DAG is divided into Stages**
- A new stage starts at every **shuffle** (redistribution of data)
- Shuffle happens during: GroupBy, Join, OrderBy

```
Stage 1: Read → Filter (no shuffle needed)
         ↓ SHUFFLE (GroupBy needs all same customer_id on same machine)
Stage 2: GroupBy → Sum (after shuffle)
```

**Step 4: Each stage splits into Tasks**
- 1 task per partition
- Tasks run in parallel across executors

```
Stage 1: [Task1 partition1] [Task2 partition2] [Task3 partition3]
Stage 2: [Task1] [Task2] [Task3] after shuffle
```

#### **Viewing the DAG in Databricks**
1. Run a Spark action (like `df.show()`)
2. Click the "Spark Jobs" link below the cell
3. See visual DAG with all stages
4. Check task completion time

---

### 2.8 Partitions — How Data is Distributed

A partition is a chunk of data. Understanding partitions helps you write faster code.

```python
# See how many partitions a DataFrame has
df = spark.read.parquet("/data/large_file")
print(df.rdd.getNumPartitions())  # e.g., 200

# Repartition (expensive — causes shuffle)
df_repartitioned = df.repartition(50)  # Reduces to 50 partitions

# Coalesce (cheap — no full shuffle, only merges partitions)
df_coalesced = df.coalesce(10)  # Merge to 10 partitions

# Partition by column (useful for writing)
df.write.partitionBy("year", "month").parquet("/output")
```

**Why does this matter?**
- Too many small partitions → overhead, slow
- Too few large partitions → workers sit idle, slow
- Right size: ~128MB per partition is optimal

---

### 2.9 Wide vs Narrow Transformations

```
NARROW TRANSFORMATION:
Each input partition → ONE output partition (no shuffle)
Examples: map, filter, select

Input:  [P1][P2][P3]
          ↓   ↓   ↓
Output: [P1][P2][P3]

WIDE TRANSFORMATION:
Multiple input partitions → Multiple output partitions (SHUFFLE!)
Examples: groupBy, join, orderBy

Input:  [P1][P2][P3]
           ↘ ↓ ↙
           SHUFFLE
           ↙ ↓ ↘
Output: [P1'][P2'][P3']
```

**Why this matters**: Wide transformations (shuffles) are **expensive** — they move data across the network. Minimize them when possible!

---

## 💻 Hands-On Exercise

### Exercise 1: Explore Spark Architecture

```python
# Cell 1: Check SparkSession details
print("App Name:", spark.sparkContext.appName)
print("Master:", spark.sparkContext.master)
print("Spark Version:", spark.version)
print("Default Parallelism:", spark.sparkContext.defaultParallelism)
```

### Exercise 2: See Lazy Evaluation in Action

```python
# Create a large range — no execution yet
df = spark.range(0, 10_000_000)
print("Created range — no execution yet")

# Add filter — still no execution
df_filtered = df.filter(df.id % 2 == 0)
print("Added filter — still no execution")

# THIS triggers execution!
import time
start = time.time()
count = df_filtered.count()
end = time.time()
print(f"Count = {count}, Time = {end - start:.2f} seconds")
```

### Exercise 3: Transformations vs Actions

```python
from pyspark.sql.functions import col, when

# Create test data
data = [(i, f"user_{i}", i * 100, ["NY", "CA", "TX", "FL"][i % 4]) 
        for i in range(1, 21)]
df = spark.createDataFrame(data, ["id", "user", "amount", "state"])

# Chain transformations (lazy)
result = df \
    .filter(col("amount") > 500) \
    .withColumn("tier", when(col("amount") > 1500, "gold").otherwise("silver")) \
    .select("user", "amount", "tier", "state") \
    .orderBy("amount", ascending=False)

# Action — triggers all transformations
result.show(10)
```

---

## ❓ Q&A

### Q1: Why is in-memory processing faster than disk?

**A:** RAM access speed vs disk access speed:
- RAM (memory): ~50 nanoseconds access time
- SSD Disk: ~100 microseconds access time
- HDD Disk: ~10 milliseconds access time

**Memory is ~2,000× faster than SSD and ~200,000× faster than spinning disk.**

Hadoop MapReduce reads/writes disk between every step. Spark keeps data in RAM across all steps — hence 10-100× speedup.

---

### Q2: When should I use RDDs instead of DataFrames?

**A:** Use RDDs only when:
- Processing complex, non-tabular data (binary files, custom objects)
- You need fine-grained control over partitioning
- Working with existing RDD codebases

In 99% of data engineering work → **use DataFrames**.

---

### Q3: What is a shuffle and why is it slow?

**A:** A shuffle is when Spark redistributes data across the network.

**Why needed**: Operations like `groupBy` need all rows with the same key on the same executor.

**Why slow**: 
- Data moves over network (slow)
- Intermediate files written to disk
- Lots of coordination overhead

**Example of minimizing shuffles:**
```python
# BAD: Multiple shuffles
df.groupBy("a").count().join(df2.groupBy("b").count(), ...)

# BETTER: Reduce data before shuffling
df_small = df.filter(col("date") == "2024-01-01")  # Filter first, shuffle less data
df_small.groupBy("a").count()
```

---

### Q4: What does `collect()` do and when is it dangerous?

**A:** `collect()` brings ALL data from Spark executors back to the driver (your laptop/notebook).

**Safe when**: Result is small (< 100K rows)
**Dangerous when**: Dataset is large → driver runs out of memory!

```python
# SAFE
small_df = df.limit(100)
result = small_df.collect()  # Only 100 rows to driver

# DANGEROUS ❌
big_df = spark.read.parquet("/data/10gb_file")
result = big_df.collect()  # Tries to bring 10GB to driver → OOM crash!

# SAFER ALTERNATIVES
df.show(20)              # Show first 20 rows only
df.take(100)             # Take first 100 rows
df.write.parquet("/out") # Write to storage instead
```

---

### Q5: How many partitions should I have?

**A:** Rule of thumb: **128MB per partition**

```python
# Check current partition count
print("Partitions:", df.rdd.getNumPartitions())

# For a 10GB file: 10GB / 128MB = ~80 partitions
# For a 1TB file: 1TB / 128MB = ~8000 partitions

# Change default shuffle partition count (default 200):
spark.conf.set("spark.sql.shuffle.partitions", "100")
```

---

### Q6: What is the difference between `repartition` and `coalesce`?

**A:**

| | `repartition(n)` | `coalesce(n)` |
|-|-----------------|---------------|
| **Direction** | Can increase or decrease | Only decrease |
| **Shuffle** | Full shuffle (expensive) | Minimal shuffle (cheap) |
| **Data distribution** | Equal across partitions | May be unequal |
| **Use case** | Need even distribution | Just want fewer partitions |

```python
# Use coalesce when writing small output
df.coalesce(1).write.csv("/output")  # Single output file

# Use repartition when need even distribution
df.repartition(100).write.parquet("/output")
```

---

## 🔑 Key Takeaways

1. Spark processes data **in-memory** across many machines simultaneously
2. **Driver** coordinates; **Executors** do the work
3. **DataFrames** are your primary tool (not RDDs)
4. **Lazy evaluation** — transformations don't execute until an action
5. **Actions** trigger execution: `show`, `count`, `collect`, `write`
6. **Shuffles** are expensive — minimize GroupBy and Join operations
7. DAG → Jobs → Stages → Tasks — execution hierarchy
8. **Partitions** = units of parallelism; right-size them (~128MB)

---

**Next Session → Day 3: PySpark Essentials**  
We'll put this architecture knowledge to work with hands-on PySpark code.
