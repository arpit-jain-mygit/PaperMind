# Databricks Notebook 1: Spark Basics

**Import this into Databricks as a notebook**

---

## Setup

```python
from pyspark.sql.functions import *
from pyspark.sql.types import *
```

---

## 1. Create Sample Data

```python
# Create a small DataFrame
data = [
    (1, "Alice", 5000, "Engineering"),
    (2, "Bob", 4000, "Sales"),
    (3, "Charlie", 6000, "Engineering"),
    (4, "Diana", 3500, "Marketing"),
    (5, "Eve", 5500, "Engineering"),
]

df = spark.createDataFrame(data, ["id", "name", "salary", "department"])
df.show()
```

---

## 2. Transformations (Lazy)

```python
# These DON'T run yet
filtered = df.filter(df.salary > 4000)
selected = filtered.select("name", "salary")

print("No computation yet - lazy evaluation")
```

---

## 3. Action (Forces Execution)

```python
# NOW it runs
selected.show()
```

---

## 4. Grouping & Aggregation

```python
dept_avg = df.groupBy("department").agg(
    avg("salary").alias("avg_salary"),
    count("*").alias("count")
)
dept_avg.show()
```

---

## 5. Window Function

```python
from pyspark.sql.window import Window

window_spec = Window.partitionBy("department").orderBy(col("salary").desc())
ranked = df.withColumn("rank_in_dept", rank().over(window_spec))
ranked.show()
```

---

## 6. Join

```python
bonus_data = spark.createDataFrame([
    (1, 500),
    (2, 400),
    (3, 600),
], ["id", "bonus"])

result = df.join(bonus_data, "id", "left")
result.show()
```

---

## 7. Spark SQL

```python
df.createOrReplaceTempView("employees")

spark.sql("""
    SELECT department, COUNT(*) as count, AVG(salary) as avg_sal
    FROM employees
    WHERE salary > 4000
    GROUP BY department
    ORDER BY avg_sal DESC
""").show()
```

---

## 8. Check Execution Plan

```python
df.filter(df.salary > 4000).explain()
```

Look at the output — see how Spark optimizes the query.

---

## Exercises

**1. Find employees earning more than department average**
```python
# Hint: Use window function with avg()
```

**2. Join with bonus data, sum (salary + bonus) by department**
```python
# Hint: Select, groupBy, sum
```

**3. Write result to table**
```python
# Hint: df.write.mode("overwrite").saveAsTable("result_table")
```

---

## Next Notebook: Shuffles & Performance

