# Day 3: PySpark Essentials
## DataFrame API, Transformations, Joins & Aggregations

**Duration**: 1 hour | **Difficulty**: Intermediate

---

## 🎯 Learning Objectives

- ✅ Read data from various sources (CSV, Parquet, JSON)
- ✅ Use the full DataFrame transformation API
- ✅ Perform all types of joins
- ✅ Write aggregations and window functions basics
- ✅ Handle nulls and data type casting
- ✅ Use SQL alongside PySpark

---

## 📚 Content

### 3.1 Reading Data

The entry point for all data is `spark.read`.

```python
# ─────────────────────────────────────────────────
# 1. CSV
# ─────────────────────────────────────────────────
df_csv = spark.read \
    .option("header", True) \
    .option("inferSchema", True) \
    .option("nullValue", "N/A") \
    .csv("/data/sales.csv")

# Multiple files at once
df_multi = spark.read.csv("/data/sales/*.csv", header=True, inferSchema=True)

# ─────────────────────────────────────────────────
# 2. JSON
# ─────────────────────────────────────────────────
df_json = spark.read.json("/data/events.json")

# Nested JSON (multiline)
df_json_nested = spark.read \
    .option("multiline", True) \
    .json("/data/nested_events.json")

# ─────────────────────────────────────────────────
# 3. Parquet (most efficient for analytics)
# ─────────────────────────────────────────────────
df_parquet = spark.read.parquet("/data/transactions/")

# ─────────────────────────────────────────────────
# 4. Delta (Databricks native)
# ─────────────────────────────────────────────────
df_delta = spark.read.format("delta").load("/data/delta/sales")

# ─────────────────────────────────────────────────
# 5. From a registered table
# ─────────────────────────────────────────────────
df_table = spark.read.table("catalog.schema.table_name")
# or
df_sql = spark.sql("SELECT * FROM catalog.schema.table_name")
```

#### **inferSchema vs Defined Schema**

```python
from pyspark.sql.types import StructType, StructField, StringType, IntegerType, DoubleType, DateType

# ❌ inferSchema reads the entire file to guess types — slow on large files
df = spark.read.option("inferSchema", True).csv("/data/sales.csv")

# ✅ BETTER: Define schema explicitly — fast and predictable
schema = StructType([
    StructField("order_id",   IntegerType(), nullable=False),
    StructField("customer_id",IntegerType(), nullable=True),
    StructField("product",    StringType(),  nullable=True),
    StructField("amount",     DoubleType(),  nullable=True),
    StructField("order_date", DateType(),    nullable=True),
    StructField("status",     StringType(),  nullable=True)
])

df = spark.read.schema(schema).csv("/data/sales.csv", header=True)
df.printSchema()
```

---

### 3.2 Core Transformations

#### **select and selectExpr**

```python
from pyspark.sql.functions import col, expr, upper, lit

# Basic select
df.select("name", "age", "state")

# Select with expressions
df.select(
    col("name"),
    col("age") + 1,
    upper(col("state")).alias("STATE_UPPER"),
    lit("USA").alias("country")   # Constant value
)

# selectExpr — write SQL inline
df.selectExpr(
    "name",
    "age + 1 AS next_year_age",
    "UPPER(state) AS state_upper",
    "'USA' AS country"
)
```

#### **filter / where**

```python
# filter and where are identical — use either
df.filter(col("age") > 25)
df.where(col("age") > 25)

# Multiple conditions
df.filter((col("age") > 25) & (col("state") == "NY"))
df.filter((col("age") > 25) | (col("state") == "CA"))

# NOT condition
df.filter(~col("is_deleted"))

# Filter on null
df.filter(col("email").isNull())
df.filter(col("email").isNotNull())

# Filter on list of values
df.filter(col("state").isin(["NY", "CA", "TX"]))
df.filter(~col("status").isin(["cancelled", "refunded"]))

# String filters
df.filter(col("name").startswith("A"))
df.filter(col("email").endswith("@gmail.com"))
df.filter(col("description").contains("premium"))
df.filter(col("phone").rlike(r"^\d{10}$"))  # Regex
```

#### **withColumn — Adding / Modifying Columns**

```python
from pyspark.sql.functions import when, col, round, current_date, datediff

# Add new column
df = df.withColumn("tax", col("amount") * 0.08)

# Conditional column (CASE WHEN equivalent)
df = df.withColumn(
    "tier",
    when(col("amount") >= 1000, "platinum")
    .when(col("amount") >= 500,  "gold")
    .when(col("amount") >= 100,  "silver")
    .otherwise("bronze")
)

# Derived from multiple columns
df = df.withColumn(
    "full_name",
    expr("CONCAT(first_name, ' ', last_name)")
)

# Date operations
df = df.withColumn("days_since_order", datediff(current_date(), col("order_date")))

# Round
df = df.withColumn("amount_rounded", round(col("amount"), 2))

# Overwrite existing column
df = df.withColumn("status", upper(col("status")))
```

#### **drop, rename, cast**

```python
# Drop columns
df = df.drop("unnecessary_col1", "unnecessary_col2")

# Rename
df = df.withColumnRenamed("old_name", "new_name")

# Cast data type
from pyspark.sql.types import IntegerType, DoubleType, TimestampType

df = df.withColumn("age", col("age").cast(IntegerType()))
df = df.withColumn("amount", col("amount").cast(DoubleType()))
df = df.withColumn("event_time", col("event_time").cast(TimestampType()))

# Multiple renames (use select)
df = df.select(
    col("cust_id").alias("customer_id"),
    col("ord_dt").alias("order_date"),
    col("amt").alias("amount")
)
```

#### **sort / orderBy**

```python
# Ascending (default)
df.orderBy("age")
df.orderBy(col("age").asc())

# Descending
df.orderBy(col("amount").desc())

# Multiple columns
df.orderBy(col("state").asc(), col("amount").desc())

# Handle nulls
df.orderBy(col("amount").desc_nulls_last())
df.orderBy(col("amount").asc_nulls_first())
```

#### **distinct and drop_duplicates**

```python
# Remove all duplicate rows
df.distinct()

# Remove duplicates based on specific columns
df.dropDuplicates(["customer_id", "order_date"])

# Count distinct values
df.select("customer_id").distinct().count()
```

---

### 3.3 Handling Null Values

Nulls are everywhere in real data. Handle them explicitly.

```python
from pyspark.sql.functions import coalesce, col

# Fill all nulls with a value
df.fillna(0)                        # Fill all numeric nulls with 0
df.fillna("Unknown")                # Fill all string nulls
df.fillna({"age": 0, "name": "N/A"})  # Per column

# Drop rows with any null
df.dropna()

# Drop rows where specific columns are null
df.dropna(subset=["customer_id", "order_date"])

# Drop only if ALL columns are null
df.dropna(how="all")

# Replace null with another column's value (COALESCE)
df = df.withColumn(
    "email",
    coalesce(col("email"), col("backup_email"), lit("noemail@default.com"))
)

# Check for nulls
df.filter(col("customer_id").isNull()).count()
```

---

### 3.4 Aggregations

```python
from pyspark.sql.functions import (
    count, countDistinct, sum, avg, min, max,
    collect_list, collect_set, first, last,
    percentile_approx, stddev, variance
)

# Basic groupBy + aggregation
df.groupBy("state") \
  .agg(
      count("*").alias("total_orders"),
      countDistinct("customer_id").alias("unique_customers"),
      sum("amount").alias("total_revenue"),
      avg("amount").alias("avg_order_value"),
      min("amount").alias("min_order"),
      max("amount").alias("max_order")
  ) \
  .orderBy(col("total_revenue").desc())

# Multiple groupBy columns
df.groupBy("state", "tier") \
  .agg(sum("amount").alias("revenue")) \
  .orderBy("state", col("revenue").desc())

# Collect values into a list
df.groupBy("customer_id") \
  .agg(collect_list("product").alias("purchased_products"))

# Approximate percentile (fast on large data)
df.groupBy("state") \
  .agg(percentile_approx("amount", 0.5).alias("median_amount"))

# Pivot table (rows → columns)
df.groupBy("state") \
  .pivot("year", [2022, 2023, 2024]) \
  .sum("amount")
```

---

### 3.5 Joins

```python
# Create two sample DataFrames
orders = spark.createDataFrame([
    (1, 101, 500.0), (2, 102, 300.0), (3, 103, 750.0), (4, None, 100.0)
], ["order_id", "customer_id", "amount"])

customers = spark.createDataFrame([
    (101, "Alice", "NY"), (102, "Bob", "CA"), (104, "Diana", "TX")
], ["customer_id", "name", "state"])

# ─────────────────────────────────────────────────
# INNER JOIN — only matching rows
# ─────────────────────────────────────────────────
result = orders.join(customers, on="customer_id", how="inner")
# Returns: orders 1, 2 (customers 101, 102 match)

# ─────────────────────────────────────────────────
# LEFT JOIN — all from left, matching from right
# ─────────────────────────────────────────────────
result = orders.join(customers, on="customer_id", how="left")
# Returns: all orders; order 3 (cust 103) has nulls; order 4 (null) has nulls

# ─────────────────────────────────────────────────
# RIGHT JOIN — all from right, matching from left
# ─────────────────────────────────────────────────
result = orders.join(customers, on="customer_id", how="right")

# ─────────────────────────────────────────────────
# FULL OUTER JOIN — all rows from both
# ─────────────────────────────────────────────────
result = orders.join(customers, on="customer_id", how="outer")

# ─────────────────────────────────────────────────
# ANTI JOIN — rows in left NOT in right (very useful!)
# ─────────────────────────────────────────────────
result = orders.join(customers, on="customer_id", how="left_anti")
# Returns: orders 3 and 4 (customers 103 and null don't exist in customers)

# ─────────────────────────────────────────────────
# Join on different column names
# ─────────────────────────────────────────────────
result = orders.join(
    customers,
    orders.customer_id == customers.customer_id,
    how="inner"
)
# ⚠️ This creates TWO customer_id columns! Drop the duplicate:
result = result.drop(customers.customer_id)

# ─────────────────────────────────────────────────
# Join on multiple conditions
# ─────────────────────────────────────────────────
result = orders.join(
    customers,
    (orders.customer_id == customers.customer_id) & (orders.amount > 400),
    how="inner"
)
```

---

### 3.6 Using SQL Alongside PySpark

In Databricks, you can freely mix SQL and PySpark.

```python
# Register a DataFrame as a temporary view
df.createOrReplaceTempView("orders")

# Now use SQL
result = spark.sql("""
    SELECT 
        state,
        COUNT(*) AS total_orders,
        SUM(amount) AS revenue,
        AVG(amount) AS avg_order
    FROM orders
    WHERE status = 'completed'
    GROUP BY state
    ORDER BY revenue DESC
""")

# Use %sql magic in notebook cell
# Just put this at the top of a new cell:
# %sql
# SELECT * FROM orders WHERE amount > 500;

# Convert SQL result back to PySpark for further processing
result.filter(col("revenue") > 10000).show()
```

---

### 3.7 Writing Data

```python
# ─────────────────────────────────────────────────
# Write to Parquet (most efficient)
# ─────────────────────────────────────────────────
df.write.parquet("/output/sales")

# With partition (organizes files by column)
df.write.partitionBy("year", "month").parquet("/output/sales")

# Save modes
df.write.mode("overwrite").parquet("/output/sales")
df.write.mode("append").parquet("/output/sales")
df.write.mode("ignore").parquet("/output/sales")  # Skip if exists
df.write.mode("error").parquet("/output/sales")   # Fail if exists (default)

# ─────────────────────────────────────────────────
# Write to Delta (preferred in Databricks)
# ─────────────────────────────────────────────────
df.write.format("delta").mode("overwrite").save("/data/delta/sales")

# Write as a table
df.write.format("delta").mode("overwrite").saveAsTable("my_catalog.my_schema.sales")

# ─────────────────────────────────────────────────
# Write to CSV (for export, avoid for production)
# ─────────────────────────────────────────────────
df.coalesce(1).write.csv("/output/export.csv", header=True)
```

---

## 💻 Hands-On Exercise

### Full Exercise: Sales Data Pipeline

```python
# ── STEP 1: Create sample data ──────────────────
from pyspark.sql.functions import col, when, sum, avg, count, to_date

orders_data = [
    (1, 101, "laptop", 1200.0, "2024-01-15", "completed", "NY"),
    (2, 102, "phone",  800.0,  "2024-01-16", "completed", "CA"),
    (3, 101, "tablet", 450.0,  "2024-01-17", "pending",   "NY"),
    (4, 103, "laptop", 1200.0, "2024-01-18", "completed", "TX"),
    (5, None,"phone",  750.0,  "2024-01-18", "completed", "CA"),
    (6, 102, "tablet", None,   "2024-01-19", "cancelled", "CA"),
    (7, 104, "laptop", 1100.0, "2024-01-20", "completed", "FL"),
    (8, 101, "phone",  900.0,  "2024-01-21", "completed", "NY"),
]

customers_data = [
    (101, "Alice",   "alice@email.com", "gold"),
    (102, "Bob",     "bob@email.com",   "silver"),
    (103, "Charlie", "charlie@email.com","platinum"),
    (104, "Diana",   "diana@email.com", "silver"),
]

orders = spark.createDataFrame(
    orders_data, 
    ["order_id", "customer_id", "product", "amount", "order_date", "status", "state"]
)
customers = spark.createDataFrame(
    customers_data, 
    ["customer_id", "name", "email", "tier"]
)

# ── STEP 2: Clean orders ──────────────────────────
clean_orders = orders \
    .filter(col("customer_id").isNotNull()) \
    .filter(col("amount").isNotNull()) \
    .filter(col("status") != "cancelled") \
    .withColumn("order_date", to_date(col("order_date"))) \
    .withColumn("amount", col("amount").cast("double"))

# ── STEP 3: Enrich with customer data ────────────
enriched = clean_orders.join(customers, on="customer_id", how="left")

# ── STEP 4: Add business logic columns ───────────
enriched = enriched.withColumn(
    "revenue_bucket",
    when(col("amount") >= 1000, "high")
    .when(col("amount") >= 500,  "medium")
    .otherwise("low")
)

# ── STEP 5: Aggregate ────────────────────────────
summary = enriched.groupBy("state", "tier") \
    .agg(
        count("*").alias("orders"),
        sum("amount").alias("revenue"),
        avg("amount").alias("avg_order")
    ) \
    .orderBy(col("revenue").desc())

summary.show()
```

---

## ❓ Q&A

### Q1: What's the best format — CSV, JSON, or Parquet?

| Format | Pros | Cons | Use When |
|--------|------|------|----------|
| CSV | Simple, readable | Slow, large, no types | Exports, simple data |
| JSON | Flexible, nested | Slow, verbose | APIs, events |
| Parquet | Fast, compressed, columnar | Binary | Analytics — always prefer |
| Delta | ACID + Parquet | Databricks-specific | Production pipelines |

**Always use Parquet or Delta in production.**

---

### Q2: What's the difference between `col()` and `df["column"]`?

```python
# col() from functions module
from pyspark.sql.functions import col
df.filter(col("age") > 25)

# df["column"] — direct column reference
df.filter(df["age"] > 25)

# Both work the same — col() is preferred because:
# 1. Cleaner syntax in long chains
# 2. Avoids ambiguity in joins (where both DataFrames have same column)
```

---

### Q3: What does `persist()` do?

```python
# If you use a DataFrame multiple times, cache it
df.persist()   # Store in memory/disk (survives longer)
df.cache()     # Store in memory (shorthand)

df.count()     # First action: computes AND caches
df.show()      # Second action: reads from cache (fast!)

# When done, release memory
df.unpersist()
```

---

### Q4: How do I read a partitioned directory?

```python
# Data organized like:
# /data/orders/year=2024/month=01/part-*.parquet
# /data/orders/year=2024/month=02/part-*.parquet

# Spark auto-detects partition columns!
df = spark.read.parquet("/data/orders")
df.printSchema()
# Shows: year INT, month INT (auto-detected from folder structure)

# Automatic partition pruning
df.filter(col("year") == 2024).filter(col("month") == 1)
# Spark only reads /year=2024/month=01/ folder — ignores others!
```

---

### Q5: Why should I avoid `toPandas()` on large DataFrames?

```python
# toPandas() brings ALL data to driver memory
pandas_df = df.toPandas()  # ❌ Dangerous on large DataFrames

# BETTER: Filter first, then convert for visualization
sample = df.filter(col("state") == "NY").limit(1000).toPandas()
```

---

## 🔑 Key Takeaways

1. **Always define schemas explicitly** — faster and predictable
2. **`select`, `filter`, `withColumn`** are your most-used transformations
3. **`when().otherwise()`** replaces CASE WHEN
4. **Joins produce duplicate columns** — drop or alias carefully
5. **Write Parquet or Delta** in production, never CSV
6. **Use SQL freely** — Spark SQL and PySpark are interchangeable
7. **`dropna`, `fillna`, `coalesce`** for null handling

---

**Next Session → Day 4: Data Ingestion & ETL Patterns (Bronze/Silver/Gold)**
