# Day 1: Databricks Fundamentals
## Understanding the Platform & Workspace Setup

**Duration**: 1 hour  
**Difficulty**: Beginner  
**Key Takeaway**: By end of this session, you'll understand Databricks platform, workspace components, and how everything connects.

---

## 🎯 Learning Objectives

By the end of this module, you will:
- ✅ Understand what Databricks is and why it exists
- ✅ Navigate the Databricks workspace UI
- ✅ Understand clusters and their role in Databricks
- ✅ Know the difference between compute and storage
- ✅ Create and manage a basic cluster
- ✅ Understand notebooks and how to work with them
- ✅ Recognize data engineering use cases in Databricks

---

## 📚 Content

### 1.1 What is Databricks?

**The Simple Answer:**  
Databricks is a unified platform for **data engineering, data science, and business analytics**. It's built on Apache Spark and provides a collaborative environment to build data pipelines and ML models.

**In Context:**
- **Apache Spark** → Distributed computing engine (open-source, runs anywhere)
- **Databricks** → Enterprise platform built ON Spark (managed service, easy to use)
- **Analogy**: Spark is like raw flour; Databricks is like a pre-made cake mix with all ingredients included

**Why Databricks Exists:**
- Apache Spark is powerful but complex to set up
- Managing clusters, dependencies, and infrastructure is hard
- Data teams needed a collaborative platform (like Jupyter but better)
- Spark handles big data, but Databricks handles big teams

**Key Value Propositions:**
1. **Managed Spark** - No cluster infrastructure to manage
2. **Collaboration** - Share notebooks, work together in real-time
3. **Integration** - Connect to cloud storage (S3, ADLS, GCS) seamlessly
4. **Delta Lake** - ACID transactions on data lake (more on this in Day 5)
5. **SQL & Python** - Write SQL directly or use PySpark
6. **ML & Analytics** - Built-in MLlib, AutoML features

---

### 1.2 Databricks Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│         DATABRICKS CONTROL PLANE (Managed)          │
│  - Workspace Management                             │
│  - Cluster Orchestration                            │
│  - Job Scheduling                                   │
│  - Secrets Management                               │
└──────────────────────────┬──────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
┌───────▼─────┐   ┌───────▼─────┐   ┌───────▼─────┐
│  AWS/Azure/ │   │   Notebooks │   │  Databricks │
│    GCP      │   │  (UI Layer) │   │   Jobs      │
│   Account   │   │             │   │             │
└─────────────┘   └──────┬──────┘   └─────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
┌───────▼────────┐  ┌────▼──────┐  ┌─────▼──────┐
│   Compute      │  │  Storage  │  │  Catalog   │
│  (Spark        │  │  (S3,     │  │  (Tables,  │
│   Clusters)    │  │   ADLS)   │  │   DBs)     │
└────────────────┘  └───────────┘  └────────────┘
```

---

### 1.3 Key Components Explained

#### **A. Workspace**
- Your personal/team Databricks environment
- Contains notebooks, clusters, secrets, repos
- Think of it as your "project space"
- Multiple workspaces can exist for different teams/projects

#### **B. Clusters**
- A group of connected computers running Spark
- **Driver**: Single machine coordinating work
- **Workers**: Multiple machines doing parallel work
- **Databricks manages everything** (no SSH needed!)
- You specify: size, version, auto-stop time

#### **C. Notebooks**
- Interactive coding environment (like Jupyter but better)
- Support multiple languages: Python, SQL, R, Scala
- Mix code and markdown for documentation
- Real-time collaboration

#### **D. Storage**
- Cloud storage (S3, ADLS, GCS) for data persistence
- Databricks doesn't store your data—just points to it
- Default root location: `/dbfs/` (Databricks File System)

#### **E. Catalog (Unity Catalog)**
- Centralized metadata layer
- Track all tables, views, databases
- Data governance and access control
- (We'll cover this more in Day 5)

---

### 1.4 Clusters Deep Dive

#### **Cluster Types**

| Type | Use Case | Cost | Comments |
|------|----------|------|----------|
| **All-purpose** | Interactive notebooks, experimentation | Low (can share across users) | Share across team, pay for uptime |
| **Job** | Running jobs (ETL, workflows) | Medium (dedicated per job) | Auto-start before job, auto-stop after |
| **SQL Warehouse** | SQL queries only | Medium (serverless) | Dedicated for BI/SQL queries |

#### **Important Cluster Settings (for Data Engineers)**

```
Cluster Name: "DE-Production-ETL"
Databricks Runtime: 13.3 LTS (or latest)  ← Always use LTS for production
Node Type: i3.xlarge (Memory optimized)   ← Depends on data size
Workers: 4                                ← For distributed processing
Driver: Same as workers
Auto-terminate after: 15 minutes          ← Cost saving for job clusters
```

**Why LTS (Long-Term Support)?**
- Stability and bug fixes
- Tested with common libraries
- Production recommended

#### **Cluster Initialization Scripts**
You can run setup scripts when cluster starts:

```python
# Example: Install custom library on cluster startup
pip install requests pandas --upgrade
```

---

### 1.5 Workspace Navigation

**Main UI Sections** (left sidebar):

1. **Home** - Dashboard, recent items
2. **Workspace** - Your notebooks, folders
3. **Catalog** - All databases and tables (Unity Catalog)
4. **Compute** - Manage clusters
5. **Repos** - Git integration (for code version control)
6. **Jobs** - Scheduled workflows
7. **Data** - Browse data, manage mounts
8. **Workflows** - Advanced job orchestration

**Key for Data Engineers**: Jobs + Compute + Catalog

---

### 1.6 Notebooks: Your Development Environment

#### **Notebook Basics**

```python
# This is a Python cell in Databricks notebook
# You can run it with Ctrl+Enter or Cmd+Enter

print("Hello Databricks!")

# Results appear below the cell
# Output: Hello Databricks!
```

#### **Mixing Languages**

```python
# Python cell
df = spark.range(5)
df.show()

# Use %sql magic command for SQL
%sql
SELECT * FROM my_table LIMIT 10;

# Use %scala for Scala
%scala
val df = spark.range(5)
```

#### **Important Notebook Shortcuts**

| Action | Shortcut |
|--------|----------|
| Run cell | Ctrl+Enter (Windows) / Cmd+Enter (Mac) |
| Run all | Ctrl+Alt+Enter |
| Add cell below | Alt+Enter |
| Delete cell | Ctrl+Alt+X |
| Move cell | Ctrl+Alt+↑/↓ |

#### **Notebook Best Practices**
```python
# ✅ GOOD: Clear naming, organized flow
# 1. SETUP: Import libraries and read data
import pyspark.sql.functions as F
from pyspark.sql.types import *

# 2. MAIN LOGIC: Transform data
# 3. VALIDATION: Check results
# 4. OUTPUT: Save results

# ❌ BAD: Random cells, no structure
import random_lib
df.show()
# Another cell doing something unrelated
spark.sql("SELECT * FROM ...")
```

---

### 1.7 Data Engineering Perspective: Why Databricks?

**Traditional Data Pipeline Challenge:**
```
Write Python code → Submit to cluster → Wait → Check logs → Debug
❌ Slow, not collaborative, hard to monitor
```

**Databricks Approach:**
```
Write PySpark code in notebook → Run immediately → See results → Share
✅ Fast, collaborative, easy to track
```

**For Data Engineers Specifically:**

| Need | Databricks Solution |
|------|-------------------|
| Build ETL pipelines | PySpark + Jobs |
| Handle large datasets | Spark clusters scale automatically |
| Ensure data quality | Delta Lake ACID transactions |
| Monitor pipelines | Built-in job runs UI |
| Collaborate on code | Shared notebooks |
| Version control | Git integration (Repos) |
| Schedule workflows | Databricks Jobs |

---

### 1.8 Common Terminology

| Term | Meaning |
|------|---------|
| **DBFS** | Databricks File System - default file system |
| **Driver** | Main node coordinating Spark work |
| **Worker** | Nodes doing parallel computation |
| **Executor** | Java process running on worker nodes |
| **Partition** | Piece of data processed in parallel |
| **Action** | Operation that returns result to driver (like `.show()`) |
| **Transformation** | Operation that creates new DataFrame |
| **DAG** | Directed Acyclic Graph - Spark's execution plan |
| **Stage** | Group of tasks that can run in parallel |

---

## 💻 Hands-On Exercise

### Exercise 1: Create Your First Cluster

**Step 1**: Click "Compute" in left sidebar  
**Step 2**: Click "Create Cluster"  
**Step 3**: Fill in:
```
Cluster Name: "My-First-Cluster"
Databricks Runtime: 13.3 LTS
Worker Type: i3.xlarge (or smallest available)
Workers: 2
```
**Step 4**: Click "Create Cluster"  
**Step 5**: Wait 2-3 minutes for cluster to start

**Checkpoint**: You should see green checkmark next to cluster name

---

### Exercise 2: Create a Notebook

**Step 1**: Click "Workspace" in left sidebar  
**Step 2**: Right-click your home folder → "Create" → "Notebook"  
**Step 3**: Name it "Day1_Exploration"  
**Step 4**: Choose language: Python  
**Step 5**: Attach to your cluster from Exercise 1  

**Checkpoint**: Notebook is created and ready to use

---

### Exercise 3: Run Your First Spark Code

**In your notebook, run these cells:**

```python
# Cell 1: Check Spark version
print(f"Spark Version: {spark.version}")
print(f"Python Version: {sc.pythonVer}")
```

**Expected Output:**
```
Spark Version: 13.3.x
Python Version: 3.x.x
```

```python
# Cell 2: Create a simple DataFrame
df = spark.createDataFrame([
    (1, "Alice", 25),
    (2, "Bob", 30),
    (3, "Charlie", 35)
], ["id", "name", "age"])

df.show()
```

**Expected Output:**
```
+---+-------+---+
| id|   name|age|
+---+-------+---+
|  1|  Alice| 25|
|  2|    Bob| 30|
|  3|Charlie| 35|
+---+-------+---+
```

**Checkpoint**: You've run Spark code successfully!

---

## ❓ Q&A - Common Questions

### Q1: What's the difference between Databricks and Apache Spark?

**A:**
- **Apache Spark** = Open-source distributed computing engine (the "engine")
- **Databricks** = Enterprise platform on top of Spark (the "car")

**Analogy**: Spark is like a powerful engine; Databricks is like a complete car with the engine, plus dashboard, steering wheel, etc.

**For you**: You'll write Spark code, but use Databricks to run it.

---

### Q2: Do I need to manage the Spark cluster?

**A:** No! Databricks manages it for you.

**What Databricks handles:**
- ✅ Setting up cluster hardware
- ✅ Installing Spark and dependencies
- ✅ Scaling workers up/down
- ✅ Fault tolerance and recovery
- ✅ Updates and patches

**What you handle:**
- ✅ Writing code
- ✅ Configuring cluster size (how many workers)
- ✅ Setting auto-terminate time (cost control)

---

### Q3: How much does Databricks cost?

**A:** Pricing has two parts:

1. **Compute** - You pay for running clusters (per DBU/hour)
   - 1 DBU ≈ 1 worker hour
   - Example: 4-worker cluster running 1 hour = ~4 DBUs

2. **Storage** - You pay for cloud storage (S3, ADLS, GCS)
   - Databricks doesn't charge for storage itself
   - You pay your cloud provider

**Cost Example**:
- Small job cluster: 4 workers × 1 hour = 4 DBUs × $0.40 = $1.60
- Production cluster running 24/7: Can get expensive!

**Pro Tip**: Use auto-terminate and job clusters to minimize costs.

---

### Q4: What's DBFS?

**A:** Databricks File System - it's a virtual file system that Databricks provides.

```
/dbfs/          ← Root
├── user/
│   └── your_email@company.com/
│       └── my_notebooks/
├── shared/      ← Shared across workspace
└── mnt/         ← External storage (S3, ADLS)
    └── data_lake/
```

**Important**: You probably don't need to use DBFS directly—you'll work with cloud storage (S3/ADLS) mounted in `/mnt/`.

---

### Q5: Can multiple people work on the same notebook?

**A:** Yes! Real-time collaboration is a key Databricks feature.

**How it works**:
1. Share notebook with team member
2. Both can edit simultaneously
3. Changes sync in real-time (like Google Docs)
4. Version history is maintained

**Tip**: Name notebooks clearly for team collaboration!

---

### Q6: What's the difference between All-purpose and Job clusters?

**A:**

| Aspect | All-Purpose | Job |
|--------|------------|-----|
| **Use** | Notebooks, interactive | Running jobs/scripts |
| **Cost** | Lower (shared) | Higher (dedicated) |
| **Uptime** | Stay on (manual stop) | Start before job, stop after |
| **Best For** | Development, exploration | Production ETL |

**Rule of Thumb**:
- Experimenting? → All-purpose
- Running production pipeline? → Job cluster

---

### Q7: How do I choose cluster size?

**A:** Consider your data:

```
Small data (< 10 GB):
  Workers: 2-4
  Instance: i3.xlarge
  Cost per hour: $2-5

Medium data (10-100 GB):
  Workers: 4-8
  Instance: i3.2xlarge
  Cost per hour: $5-15

Large data (> 100 GB):
  Workers: 8-16+
  Instance: Memory-optimized
  Cost per hour: $15+
```

**Better Approach**: Start small, run job, check if it's bottlenecked, increase size if needed.

---

### Q8: What programming language should I use?

**A:** For data engineering:
- **Python + PySpark** - Most popular, easiest to learn
- SQL - For queries, transformations
- Scala - Advanced users, better performance (rarely needed)

**Our course**: We focus on Python + SQL

---

### Q9: Do I need to write code to mount cloud storage?

**A:** Usually no! Databricks can automatically mount S3/ADLS.

**Simple mount example** (if needed):
```python
# Mount S3 bucket
dbutils.fs.mount(
    source="s3a://my-bucket/path",
    mount_point="/mnt/data",
    extra_configs={
        "fs.s3a.access.key": "YOUR_KEY",
        "fs.s3a.secret.key": "YOUR_SECRET"
    }
)
```

**Better**: Use Unity Catalog (more on this in Day 5)

---

### Q10: What happens if my cluster crashes?

**A:** Databricks handles recovery automatically.

**What happens**:
1. Cluster detects failure
2. Databricks marks failed tasks
3. On cluster restart, failed tasks re-run
4. Work continues from checkpoint (if using Delta Lake)

**Your responsibility**:
- Write idempotent code (safe to run multiple times)
- Use Delta Lake for atomicity

---

## 🔑 Key Takeaways

1. **Databricks** = Managed Spark platform for data teams
2. **Clusters** = Groups of computers running Spark (Databricks manages them)
3. **Notebooks** = Interactive development environment
4. **Workspace** = Your project area with notebooks, data, clusters
5. **Two-tier**: Compute (clusters) + Storage (cloud S3/ADLS)
6. **For Data Engineers**: Perfect for building ETL pipelines with monitoring
7. **Cost is manageable**: Use auto-terminate and job clusters
8. **Real-time collaboration** is built-in

---

## 📝 Summary

| Concept | What It Is | Why Data Engineers Care |
|---------|-----------|----------------------|
| Databricks | Managed Spark platform | Easy cluster setup, great for pipelines |
| Clusters | Spark computers working together | Distributed processing of big data |
| Notebooks | Interactive coding environment | Quick iteration and testing |
| DBFS | Databricks file system | Access your data files |
| Workspace | Your project area | Organize notebooks, clusters, data |
| Unity Catalog | Table metadata | Track all your data assets |

---

## 📚 Next Steps

✅ **Before Day 2 Session**:
- Play around with your created cluster and notebook
- Try creating different types of DataFrames
- Explore the UI (Catalog, Compute sections)
- Read Q&A section multiple times

**Next Session**: Day 2 - Apache Spark Basics (architecture, RDDs, DataFrames, execution model)

---

**Day 1 Complete! 🎉**

You now understand:
- ✅ What Databricks is and why it exists
- ✅ How clusters work
- ✅ How to navigate the workspace
- ✅ How to write basic Spark code

**Progress**: ██░░░░░░░ 30% of course complete
