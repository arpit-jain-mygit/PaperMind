# Apache Spark & Databricks: Zero to Hero

> Simple explanations for absolute beginners preparing for data engineering interviews.

---

## Table of Contents

1. [The Problem Spark Solves](#the-problem-spark-solves)
2. [What is Apache Spark?](#what-is-apache-spark)
3. [Key Concepts (Simple)](#key-concepts-simple)
4. [What is Databricks?](#what-is-databricks)
5. [How They Work Together](#how-they-work-together)
6. [Learning Resources](#learning-resources)
7. [Common Interview Questions](#common-interview-questions)

---

## The Problem Spark Solves

Imagine you have **100 GB of data** on your laptop.

Your laptop can only process maybe 8 GB at a time (RAM limit).

**Problem:** You can't analyze all 100 GB.

**Solution:** Use 10 computers, give each 10 GB, process in parallel, combine results.

**Apache Spark** is the tool that manages this.

---

## What is Apache Spark?

**Spark = a framework for processing big data across many computers.**

Think of it as:
- **Google Sheets** but for huge datasets (terabytes)
- **Excel formulas** you know, but running on 1000 computers instead of 1

Key points:
- You write Python/SQL, Spark figures out how to distribute it
- Free and open-source
- Fast (100x faster than old Hadoop for many use cases)

---

## Key Concepts (Simple)

### 1. Driver & Executors

**Driver** = Your manager
- Runs your code ("read this file, filter by X, count results")
- Decides who does what
- Does NOT process data itself

**Executors** = Your workers
- Actually process the data
- Run tasks in parallel
- Send results back to driver

```
Your Laptop (Driver)
    ↓
Cluster of Computers (Executors)
    1. Filter data (Executor 1)
    2. Filter data (Executor 2)
    3. Filter data (Executor 3)
    ↓
Combine results (Driver)
```

### 2. Transformations (Plan) vs. Actions (Do)

**Transformation** = "Here's what I want to do"
```python
df = df.filter(df.amount > 1000)  # Just planning, nothing happens yet
```

**Action** = "Do it now"
```python
df.show()  # NOW Spark processes the data
```

Why split them? Spark can **optimize** your plan before running it.

### 3. DataFrames

**DataFrame** = Spreadsheet on steroids

```
| id | name    | salary | department    |
|----|---------|--------|---------------|
| 1  | Alice   | 5000   | Engineering   |
| 2  | Bob     | 4000   | Sales         |
| 3  | Charlie | 6000   | Engineering   |
```

In Spark:
```python
df = spark.read.parquet("file.parquet")  # Load data
df.filter(df.salary > 5000).show()        # Filter and show
```

### 4. Partitions (Key for Speed)

Data is split into **partitions** (chunks).

```
100 GB file → Split into 10 partitions (10 GB each)
    ↓
Each executor gets 1 partition and processes in parallel
    ↓
Results combine = 10x faster
```

**Tip for interviews:** "Partition count = parallelism"

### 5. Shuffles (The Bottleneck)

**Shuffle** = Data moving across network

```python
df.groupBy("department").sum()  # Data must move → SHUFFLE
```

**Cost:** Very expensive (network is slow)

**Interview insight:** "Always reduce shuffles. Filter before grouping."

### 6. Do We Write Driver & Executor Code?

**NO. You write ONE piece of code. Spark splits it automatically.**

Example:
```python
# You write this (one file)
df = spark.read.parquet("file.parquet")        # Driver decides what to read
df = df.filter(df.salary > 5000)               # Executor filters data
result = df.groupBy("dept").sum()              # Executor groups data
final = result.collect()                       # Driver brings results back
```

**What actually happens:**
- Read → Driver (planning)
- Filter → Executor (processing)
- GroupBy → Executor (processing)
- Collect → Driver (get results)

**You never write code like:**
```python
# DON'T write separate files for driver vs executor
# driver_code.py
# executor_code.py
# This is not how Spark works!
```

**The rule:** Transformations = executor, Actions = driver. Spark figures it out.

**Interview answer:** "Spark users write one script. Spark automatically sends transformation code to executors and brings action results back to the driver. We don't write separate driver/executor code."

---

## What is Databricks?

**Databricks = Spark made easy to use**

Think: 
- **Databricks** is like **Google Colab** (notebook environment)
- **Spark** is like **Python** (the language)
- **Databricks** lets you run Spark notebooks in the cloud

### What Databricks Gives You

1. **Notebooks** (like Jupyter)
   - Write code, see results instantly
   - Can use Python, SQL, or Scala

2. **Cluster Management** (automatic)
   - Click "Create Cluster" instead of setting up servers
   - Spark runs on it instantly

3. **Data Storage** (built-in)
   - DBFS (Databricks File System)
   - Delta tables (better than Parquet)

4. **Visualization**
   - Charts, tables built-in
   - Better than terminal output

5. **Free Tier** (Community Edition)
   - Perfect for learning
   - Limited to 1 worker, but enough for practice

---

## How They Work Together

```
Your Code (Databricks Notebook)
    ↓
Spark Takes Your Code
    ↓
Translates to Tasks
    ↓
Sends to Cluster (multiple computers)
    ↓
Each Computer Processes a Partition
    ↓
Results Come Back to Notebook
    ↓
You See Output in Seconds
```

### Real Example

```python
# You write this in Databricks notebook
df = spark.read.parquet("s3://my-bucket/data.parquet")
result = df.filter(df.amount > 1000).groupBy("category").sum()
result.show()

# Behind scenes:
# 1. Spark reads file from S3
# 2. Splits into 50 partitions
# 3. Sends each partition to different executor
# 4. Executors filter in parallel
# 5. Executors group-by in parallel
# 6. Results come back to driver
# 7. You see table in notebook
```

---

## Learning Resources

Follow these in order (each builds on previous):

### 📖 Part 1: Core Architecture & Concepts
**File:** [Apache-Spark-Guide.md](Apache-Spark-Guide.md)

Covers:
- How Spark actually works (driver, executors, DAG)
- DataFrames and SQL
- Structured Streaming basics
- Performance tuning principles
- Real interview questions with answers

**Read time:** 20 min
**When:** Before you touch any code

---

### 🖥️ Part 2: Setting Up Databricks Free Tier
**File:** [Databricks-Setup-Guide.md](Databricks-Setup-Guide.md)

Covers:
- Create free Databricks account
- Launch a cluster
- Import notebooks
- Understand the UI
- Databricks shortcuts

**Time:** 10 min setup
**When:** Do this first, before running notebooks

---

### 💻 Part 3: Hands-On Basics (Your First Spark Job)
**File:** [Databricks-Notebook-1-Basics.md](Databricks-Notebook-1-Basics.md)

Covers:
- Create sample data
- Learn transformations vs. actions
- Filter, select, group-by
- Join tables
- Use Spark SQL
- Understand execution plans

**Time:** 30 min
**When:** After setup, run every cell
**Goal:** See how basic operations work

---

### ⚡ Part 4: Performance & Optimization (Why Things Are Slow)
**File:** [Databricks-Notebook-2-Performance.md](Databricks-Notebook-2-Performance.md)

Covers:
- Shuffles and their cost
- How partitioning affects speed
- Broadcast joins (small × big table trick)
- Caching data
- How to read Spark UI

**Time:** 40 min
**When:** After Notebook 1
**Why:** Interviews focus on optimization
**Goal:** Watch Spark UI, see which operations are expensive

---

### 🌊 Part 5: Streaming (Real-Time Data)
**File:** [Databricks-Notebook-3-Streaming.md](Databricks-Notebook-3-Streaming.md)

Covers:
- Micro-batches (streaming concept)
- Event time vs. processing time
- Stateful operations
- Watermarking (handling late data)
- Writing to delta tables

**Time:** 30 min
**When:** After Notebook 2
**Why:** Leadership interviews ask about streaming
**Goal:** Understand real-time processing patterns

---

## Common Interview Questions

### Level 1: Beginner Questions

**Q: What is Apache Spark?**
Simple answer: "A framework for processing huge datasets across a cluster of computers. Instead of processing 100 GB on one machine, Spark splits it across 10 machines (10 GB each) and processes in parallel."

**Q: Why use Spark instead of Pandas?**
Simple answer: "Pandas loads all data into memory. Spark only loads what it needs, can handle terabytes. Pandas = laptop, Spark = cluster."

**Q: What's a DataFrame?**
Simple answer: "Like an Excel sheet, but distributed. One DataFrame might be split across 50 computers, each handling a chunk."

**Q: What's the difference between transformation and action?**
Simple answer: "Transformation = planning (nothing runs). Action = execution (runs the plan). Example: filter() is transformation, show() is action."

---

### Level 2: Intermediate Questions

**Q: Explain partitions.**
Answer: "Data is divided into chunks called partitions. If you have 100 GB split into 10 partitions, you get 10 tasks running in parallel. More partitions = more parallelism (up to a point)."

**Q: What is a shuffle? Why is it expensive?**
Answer: "Shuffle happens when data needs to move across the network. Example: groupBy() requires all rows with same key to go to same executor. This network cost is ~70% of Spark jobs. Always try to reduce shuffles by filtering first."

**Q: How would you optimize a slow Spark job?**
Answer: 
1. Filter early (reduce data before shuffle)
2. Check partitions (too few = idle processors, too many = overhead)
3. Use broadcast for small tables
4. Cache if data is reused
5. Check Spark UI to find bottleneck

**Q: What is Databricks?**
Answer: "Databricks is a cloud platform that makes Spark easier. Instead of managing servers yourself, you write notebooks and Databricks manages the Spark cluster. Like difference between installing Python vs. using Google Colab."

---

### Level 3: Leadership Questions

**Q: Design a data pipeline to process 1 TB of transaction logs daily.**

Answer framework:
1. **Source:** Read from S3/HDFS in Parquet format (columnar, compressed)
2. **Partitioning:** Partition by date (source awareness = no reshuffle)
3. **Filtering:** Filter bad data early (reduce data in pipeline)
4. **Processing:** GroupBy merchant for daily stats (one unavoidable shuffle)
5. **Storage:** Write back to Parquet, partitioned by date
6. **Infrastructure:** 10 r5.2xlarge nodes (memory-optimized)
7. **Scheduling:** Daily batch via Airflow/Kubernetes
8. **Monitoring:** Track shuffle size, check Spark UI for skew

**Q: When would you NOT use Spark?**

Answer:
- Small data (<1 GB) → Pandas is faster
- Low latency (<1 second) → Use streaming DB instead
- Complex state machines → Use stateful services (Flink, etc.)
- Real-time ML feedback → TensorFlow locally is better

**Q: How do you handle data skew?**

Answer: "Skew = some partitions have 100x more data. One slow task delays entire job.

Solutions:
1. Use salting: add random suffix to skew key before shuffle
2. Separate hot keys, process independently
3. Increase partitions (helps spread skewed keys)"

**Q: Streaming pipeline for fraud detection — how ensure accuracy?**

Answer:
1. Use event time (not processing time) — data arrives out of order
2. Watermark (e.g., wait 1 hour for late arrivals)
3. Stateful aggregation (track user history in state store)
4. Idempotent writes (if same event processed twice, no duplication)
5. Monitor late-arriving data rate

---

## Quick Spark Cheat Sheet

| Concept | Simple Definition |
|---------|-------------------|
| **Driver** | Your manager (decides what to do) |
| **Executor** | A worker (processes data) |
| **Partition** | Chunk of data on one executor |
| **Transformation** | A plan (filter, select, group-by) |
| **Action** | Execute the plan (show, count, write) |
| **Shuffle** | Data moving across network (expensive) |
| **DAG** | Your job's execution plan |
| **Catalyst** | Spark's optimizer (rewrites your plan to be faster) |
| **Watermark** | Wait time for late data in streaming |
| **Broadcast** | Send small table to all executors (fast join) |

---

## 5-Day Learning Plan

**Day 1: Foundations**
- Read: Apache-Spark-Guide.md (Core Architecture section only)
- Time: 30 min

**Day 2: Setup & First Job**
- Follow: Databricks-Setup-Guide.md
- Run: Databricks-Notebook-1-Basics.md
- Time: 1 hour

**Day 3: Performance**
- Run: Databricks-Notebook-2-Performance.md
- Watch Spark UI while running
- Time: 1 hour

**Day 4: Streaming**
- Run: Databricks-Notebook-3-Streaming.md
- Understand micro-batches and watermarks
- Time: 1 hour

**Day 5: Interview Prep**
- Read: Apache-Spark-Guide.md (Interview Questions section)
- Answer out loud to practice
- Time: 1 hour

**Total: ~5 hours to go from zero to interview-ready**

---

## Quick FAQ

**Q: Do I need to learn Scala or Java?**
No. Python is enough. Spark works with Python, Scala, SQL, R.

**Q: Is Databricks free?**
Yes, Community Edition is free. Limited to 1 worker, but perfect for learning.

**Q: Will I need to know Hadoop?**
Not for modern roles. Spark replaced Hadoop.

**Q: Should I learn Spark SQL?**
Yes, but it's similar to normal SQL. Read the SQL section in Apache-Spark-Guide.md.

**Q: How long to become "Spark expert"?**
- Basics: 1 week (these materials)
- Competent: 1 month (running jobs, tuning)
- Expert: 6+ months (production systems, edge cases)

---

## Next Steps

1. **Right now:** Read this page (you're done in 10 min)
2. **Today:** Start Databricks-Setup-Guide.md
3. **This week:** Run all 3 notebooks
4. **Interview prep:** Answer questions out loud from Apache-Spark-Guide.md

---

## Resources Links

- Full Guide: [Apache-Spark-Guide.md](Apache-Spark-Guide.md)
- Setup: [Databricks-Setup-Guide.md](Databricks-Setup-Guide.md)
- Notebook 1: [Databricks-Notebook-1-Basics.md](Databricks-Notebook-1-Basics.md)
- Notebook 2: [Databricks-Notebook-2-Performance.md](Databricks-Notebook-2-Performance.md)
- Notebook 3: [Databricks-Notebook-3-Streaming.md](Databricks-Notebook-3-Streaming.md)

