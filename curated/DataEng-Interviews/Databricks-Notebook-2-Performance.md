# Databricks Notebook 2: Shuffles & Performance

---

## 1. Generate Larger Dataset

```python
import random

# Create 100K rows
data = [(i, f"user_{i % 100}", random.randint(1, 10000), random.choice(["US", "EU", "ASIA"])) 
        for i in range(100000)]

df = spark.createDataFrame(data, ["id", "user_name", "amount", "region"])
df.cache()  # Keep in memory for multiple operations
print(f"Total rows: {df.count()}")
```

---

## 2. Shuffle Example: GroupBy

```python
# This triggers a SHUFFLE (data moves across network)
result = df.groupBy("region").agg(
    sum("amount").alias("total"),
    count("*").alias("count")
)
result.show()
```

Open **Spark UI** (in Databricks, click Clusters → click cluster name → Spark UI). Watch the stages — you'll see the shuffle.

---

## 3. Reduce Shuffle: Filter First

```python
# BETTER: Filter before grouping (less data shuffled)
result = df.filter(df.amount > 5000).groupBy("region").agg(
    sum("amount").alias("total")
)
result.show()
```

Compare execution time with previous query using the UI.

---

## 4. Partitions

```python
# Check current partition count
print(f"Current partitions: {df.repartition(50).rdd.getNumPartitions()}")

# Repartition for consistent task count
df_repartitioned = df.repartition(50)
result = df_repartitioned.groupBy("region").agg(sum("amount"))
result.show()
```

---

## 5. Broadcast Join (Small Table)

```python
# Small lookup table
lookup = spark.createDataFrame([
    ("US", "United States"),
    ("EU", "Europe"),
    ("ASIA", "Asia"),
], ["region", "region_name"])

# Without broadcast: both tables shuffled
df.join(lookup, "region").show(5)

# With broadcast: lookup sent to all executors (fast)
from pyspark.sql.functions import broadcast
df.join(broadcast(lookup), "region").show(5)
```

---

## 6. Caching

```python
temp_df = df.filter(df.amount > 5000)

# Cache it (keep in executor memory)
temp_df.cache()

# First use: computes & stores
result1 = temp_df.groupBy("region").count()
result1.show()

# Second use: reads from cache (fast)
result2 = temp_df.groupBy("user_name").count()
result2.show()

# Check memory usage in Spark UI → Executors tab

temp_df.unpersist()  # Release memory
```

---

## 7. Window Function (No Shuffle Needed)

```python
from pyspark.sql.window import Window

# This does NOT shuffle — processes within each partition
window_spec = Window.partitionBy("region").orderBy(col("amount").desc())
ranked = df.withColumn("rank_in_region", rank().over(window_spec))
ranked.select("id", "region", "amount", "rank_in_region").show(10)
```

---

## 8. Execution Plan

```python
# See how Spark optimizes the query
query = df.filter(df.amount > 5000).groupBy("region").sum("amount")
query.explain(mode="extended")
```

Look for:
- **Pushed filters:** Filter applied early (good)
- **Shuffle:** Where data moves (cost point)
- **Aggregate:** GroupBy operation

---

## Exercises

**1. Count rows by region efficiently (filter first for >5000)**
```python
# Hint: Chain filter → groupBy → count
```

**2. Rank users by amount within each region (no shuffle)**
```python
# Hint: Window function with rank()
```

**3. Join with broadcast lookup, show region names**
```python
# Hint: broadcast(lookup) in join
```

---

## Key Observations

Check Spark UI after each query:
- How many stages?
- Shuffle size?
- Task count?
- Any slow tasks?

