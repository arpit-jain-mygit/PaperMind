# Day 6: Advanced Transformations
## Window Functions, Complex Joins, Nested Data & UDFs

**Duration**: 1 hour | **Difficulty**: Advanced

---

## 🎯 Learning Objectives

- ✅ Master window functions (ROW_NUMBER, RANK, LAG, LEAD, running totals)
- ✅ Handle nested/complex data types (arrays, maps, structs)
- ✅ Write efficient cross joins and self joins
- ✅ Create and use User-Defined Functions (UDFs)
- ✅ Apply higher-order functions on arrays

---

## 📚 Content

### 6.1 Window Functions — Analytics Over Partitions

Window functions let you compute aggregations **without collapsing rows** — you keep all individual rows but add aggregate context.

```
REGULAR GROUP BY:
Input:  [A:100] [A:200] [B:300] [B:150]
Output: [A:300] [B:450]   ← Rows collapsed

WINDOW FUNCTION:
Input:  [A:100] [A:200] [B:300] [B:150]
Output: [A:100,300] [A:200,300] [B:300,450] [B:150,450]  ← All rows kept!
```

```python
from pyspark.sql.functions import (
    row_number, rank, dense_rank,
    lag, lead, 
    sum, avg, min, max, count,
    first, last,
    ntile, percent_rank, cume_dist
)
from pyspark.sql.window import Window

# ── Define a Window ────────────────────────────────
# Window = "group by state, order by amount desc"
window = Window.partitionBy("state").orderBy(col("amount").desc())

# Unbounded window (from first to current row)
unbounded = Window.partitionBy("state") \
    .orderBy("order_date") \
    .rowsBetween(Window.unboundedPreceding, Window.currentRow)

# Sliding window (last 7 days)
rolling = Window.partitionBy("customer_id") \
    .orderBy(col("order_date").cast("timestamp").cast("long")) \
    .rangeBetween(-7 * 86400, 0)  # 7 days in seconds
```

#### **ROW_NUMBER, RANK, DENSE_RANK**

```python
df = spark.createDataFrame([
    (1, "Alice",   "NY", 1200.0, "2024-01-01"),
    (2, "Bob",     "CA",  800.0, "2024-01-02"),
    (3, "Charlie", "NY",  750.0, "2024-01-03"),
    (4, "Diana",   "CA",  900.0, "2024-01-04"),
    (5, "Eve",     "NY", 1200.0, "2024-01-05"),
    (6, "Frank",   "CA",  600.0, "2024-01-06"),
], ["id", "name", "state", "amount", "order_date"])

window = Window.partitionBy("state").orderBy(col("amount").desc())

result = df.withColumn("row_number",   row_number().over(window)) \
           .withColumn("rank",         rank().over(window)) \
           .withColumn("dense_rank",   dense_rank().over(window))

result.show()
# state=NY: Alice(1200)=rank1, Eve(1200)=rank1, Charlie(750)=rank3
# state=CA: Diana(900)=rank1, Bob(800)=rank2, Frank(600)=rank3
```

**Difference between them:**

| Function | Handles Ties |
|----------|-------------|
| `ROW_NUMBER` | Gives unique sequential numbers (no ties) |
| `RANK` | Ties get same rank; next rank skips |
| `DENSE_RANK` | Ties get same rank; next rank does NOT skip |

**Top-N per group** (very common pattern):
```python
# Get top 2 customers by revenue per state
window = Window.partitionBy("state").orderBy(col("amount").desc())

df.withColumn("rank", row_number().over(window)) \
  .filter(col("rank") <= 2) \
  .show()
```

---

#### **LAG and LEAD — Compare with Previous/Next Row**

```python
from pyspark.sql.functions import lag, lead, to_date

sales = spark.createDataFrame([
    ("2024-01-01", 1000.0),
    ("2024-01-02", 1200.0),
    ("2024-01-03",  900.0),
    ("2024-01-04", 1500.0),
    ("2024-01-05", 1300.0),
], ["date", "revenue"])

sales = sales.withColumn("date", to_date("date"))

# Window ordered by date
w = Window.orderBy("date")

# LAG = previous row's value; LEAD = next row's value
result = sales \
    .withColumn("prev_day_revenue", lag("revenue", 1).over(w)) \
    .withColumn("next_day_revenue", lead("revenue", 1).over(w)) \
    .withColumn("day_over_day_change", 
                col("revenue") - lag("revenue", 1).over(w)) \
    .withColumn("day_over_day_pct",
                ((col("revenue") - lag("revenue", 1).over(w)) / 
                 lag("revenue", 1).over(w) * 100).cast("decimal(10,2)"))

result.show()
# date         revenue  prev  next  change  pct
# 2024-01-01   1000     null  1200  null    null
# 2024-01-02   1200     1000  900   200     20.00
# 2024-01-03    900     1200  1500  -300   -25.00
```

---

#### **Running Totals and Moving Averages**

```python
from pyspark.sql.functions import sum as spark_sum, avg as spark_avg

# Running (cumulative) total
w_running = Window.orderBy("date") \
    .rowsBetween(Window.unboundedPreceding, Window.currentRow)

sales = sales.withColumn("cumulative_revenue", spark_sum("revenue").over(w_running))

# 3-day moving average
w_moving = Window.orderBy("date").rowsBetween(-2, 0)  # Current + 2 prev rows
sales = sales.withColumn("3day_avg", spark_avg("revenue").over(w_moving))

# 7-day rolling sum
w_rolling = Window.orderBy("date").rowsBetween(-6, 0)  # Current + 6 prev rows
sales = sales.withColumn("7day_revenue", spark_sum("revenue").over(w_rolling))

sales.show()
```

---

### 6.2 Handling Nested Data: Arrays, Maps, Structs

Real-world data often has nested structures (JSON APIs, Kafka events).

```python
from pyspark.sql.functions import (
    explode, explode_outer, posexplode,
    array_contains, array_distinct, array_sort, array_size,
    flatten, arrays_zip,
    map_keys, map_values, map_from_entries,
    from_json, to_json, get_json_object,
    struct, col
)
from pyspark.sql.types import ArrayType, MapType, StructType, StringType, IntegerType

# ── Create nested data ────────────────────────────
nested = spark.createDataFrame([
    (1, "Alice",   ["laptop", "phone", "tablet"],  {"NY": 3, "CA": 1}),
    (2, "Bob",     ["phone"],                       {"CA": 5}),
    (3, "Charlie", ["laptop", "laptop"],            {"TX": 2, "FL": 1}),
], ["id", "name", "products", "state_visits"])

nested.printSchema()
# root
#  |-- id: long
#  |-- name: string
#  |-- products: array<string>
#  |-- state_visits: map<string,long>

# ── Array operations ──────────────────────────────
# Get array size
nested.withColumn("num_products", array_size("products")).show()

# Check if array contains value
nested.withColumn("has_laptop", array_contains("products", "laptop")).show()

# Remove duplicates from array
nested.withColumn("unique_products", array_distinct("products")).show()

# ── EXPLODE: array/map rows → multiple rows ───────
# explode: one row per array element (drops null arrays)
nested.select("id", "name", explode("products").alias("product")).show()
# +--+-------+-------+
# |id|name   |product|
# +--+-------+-------+
# |1 |Alice  |laptop |
# |1 |Alice  |phone  |
# |1 |Alice  |tablet |
# |2 |Bob    |phone  |

# explode_outer: keeps rows even if array is null or empty
nested.select("id", "name", explode_outer("products").alias("product")).show()

# posexplode: also returns index
nested.select("id", posexplode("products").alias("pos", "product")).show()

# ── Map operations ────────────────────────────────
nested.withColumn("states", map_keys("state_visits")).show()
nested.withColumn("visit_counts", map_values("state_visits")).show()

# Access specific key
nested.withColumn("ny_visits", col("state_visits")["NY"]).show()

# ── JSON string handling ──────────────────────────
json_data = spark.createDataFrame([
    (1, '{"user_id": 101, "action": "click", "page": "/home"}'),
    (2, '{"user_id": 102, "action": "buy",   "amount": 500}'),
], ["event_id", "event_json"])

# Parse JSON string to struct
schema = StructType([
    StructField("user_id", IntegerType()),
    StructField("action",  StringType()),
    StructField("page",    StringType()),
    StructField("amount",  IntegerType())
])

parsed = json_data.withColumn("parsed", from_json("event_json", schema))
parsed.select("event_id", "parsed.*").show()

# Extract single field from JSON without full parse
json_data.withColumn("action", get_json_object("event_json", "$.action")).show()
```

---

### 6.3 User-Defined Functions (UDFs)

When built-in functions aren't enough, write your own.

```python
from pyspark.sql.functions import udf, pandas_udf
from pyspark.sql.types import StringType, DoubleType, IntegerType
import pandas as pd
import re

# ── Row UDF (slower — runs in Python one row at a time) ──
def clean_phone(phone):
    """Remove non-digit characters from phone number."""
    if phone is None:
        return None
    digits = re.sub(r'\D', '', phone)
    return digits if len(digits) == 10 else None

clean_phone_udf = udf(clean_phone, StringType())

# Use it
df = df.withColumn("clean_phone", clean_phone_udf(col("phone_raw")))

# Register for SQL use
spark.udf.register("clean_phone", clean_phone, StringType())
spark.sql("SELECT clean_phone(phone_raw) FROM customers")

# ── Pandas UDF (vectorized — much faster!) ────────
# Processes entire column as pandas Series — uses Apache Arrow
@pandas_udf(DoubleType())
def apply_discount(prices: pd.Series, tiers: pd.Series) -> pd.Series:
    """Apply tier-based discount to prices."""
    discounts = {"platinum": 0.20, "gold": 0.10, "silver": 0.05, "bronze": 0.0}
    result = []
    for price, tier in zip(prices, tiers):
        discount = discounts.get(tier, 0.0)
        result.append(round(price * (1 - discount), 2))
    return pd.Series(result)

# Use it
df = df.withColumn("discounted_price", apply_discount(col("price"), col("tier")))

# ── When to use each ──────────────────────────────
# Regular UDF: Simple transformations, custom logic
# Pandas UDF: Complex/vectorized operations (much faster than regular UDF)
# Built-in functions: Whenever possible (fastest — runs in JVM, not Python)
```

---

### 6.4 Advanced Join Patterns

```python
# ── Self Join: Compare rows within same table ─────
# Find pairs of customers in the same state
customers.alias("a").join(
    customers.alias("b"),
    (col("a.state") == col("b.state")) & (col("a.customer_id") < col("b.customer_id")),
    how="inner"
).select(
    col("a.name").alias("customer1"),
    col("b.name").alias("customer2"),
    col("a.state")
).show()

# ── Cross Join (cartesian product) ────────────────
# Every combination — use carefully (can be huge!)
products = spark.createDataFrame([("laptop",), ("phone",)], ["product"])
states = spark.createDataFrame([("NY",), ("CA",)], ["state"])

# Every product-state combination
products.crossJoin(states).show()

# ── Broadcast Join (optimization) ─────────────────
from pyspark.sql.functions import broadcast

# Use when one table is small (< 10MB)
# Forces Spark to send small table to all executors (avoids shuffle!)
large_orders.join(broadcast(small_lookup), on="state_code", how="inner")

# ── Range Join ────────────────────────────────────
# Join based on ranges (useful for events, pricing tiers)
events = spark.createDataFrame([
    (1, 50.0),
    (2, 150.0),
    (3, 750.0),
], ["event_id", "value"])

tiers = spark.createDataFrame([
    ("low",    0.0,   100.0),
    ("medium", 100.0, 500.0),
    ("high",   500.0, 9999.0),
], ["tier", "min_val", "max_val"])

events.join(
    tiers,
    (col("value") >= col("min_val")) & (col("value") < col("max_val")),
    how="inner"
).show()
```

---

### 6.5 Working with Dates and Timestamps

```python
from pyspark.sql.functions import (
    year, month, dayofmonth, dayofweek, dayofyear,
    hour, minute, second,
    date_add, date_sub, datediff, months_between,
    date_trunc, date_format,
    to_date, to_timestamp,
    current_date, current_timestamp,
    unix_timestamp, from_unixtime,
    last_day, next_day, trunc
)

df = df.withColumn("order_date", to_date("order_date_str", "yyyy-MM-dd"))

# Extract parts
df.withColumn("year",    year("order_date")) \
  .withColumn("month",   month("order_date")) \
  .withColumn("day",     dayofmonth("order_date")) \
  .withColumn("weekday", dayofweek("order_date")) \
  .withColumn("quarter", expr("QUARTER(order_date)"))

# Date math
df.withColumn("due_date",         date_add("order_date", 30)) \
  .withColumn("days_ago",         datediff(current_date(), "order_date")) \
  .withColumn("months_old",       months_between(current_date(), "order_date")) \
  .withColumn("month_start",      date_trunc("month", "order_date")) \
  .withColumn("week_start",       date_trunc("week", "order_date")) \
  .withColumn("end_of_month",     last_day("order_date"))

# Format for display
df.withColumn("formatted", date_format("order_date", "MMM dd, yyyy"))

# Timezone handling
from pyspark.sql.functions import convert_timezone, from_utc_timestamp, to_utc_timestamp
df.withColumn("local_time", from_utc_timestamp(col("utc_time"), "America/New_York"))
```

---

## 💻 Hands-On Exercise

```python
# Exercise: Sales Analytics Using Window Functions
sales = spark.createDataFrame([
    ("2024-01-01", "NY", "laptop",  1200.0),
    ("2024-01-01", "CA", "phone",    800.0),
    ("2024-01-02", "NY", "tablet",   450.0),
    ("2024-01-02", "CA", "laptop", 1100.0),
    ("2024-01-03", "NY", "phone",    900.0),
    ("2024-01-03", "CA", "tablet",   500.0),
    ("2024-01-04", "NY", "laptop",  1300.0),
    ("2024-01-04", "CA", "phone",    850.0),
], ["date", "state", "product", "revenue"])

# TODO: Write queries to find:
# 1. Rank products by revenue within each state
# 2. Running total revenue per state over time
# 3. Day-over-day revenue change per state
# 4. Top product per day (across all states)
# 5. 2-day moving average revenue per state
```

---

## ❓ Q&A

### Q1: When should I use window functions instead of groupBy?

```
Use groupBy when: You only need the summary (no detail rows needed)
Use window functions when: You need both detail rows AND aggregate context

Example:
groupBy: "Give me total revenue per state" → 2 rows (NY, CA)
window:  "Show each order with its state's total" → all orders, each with state total
```

---

### Q2: What's the difference between `rows_between` and `range_between`?

```python
# rowsBetween: physical row positions
# rangeBetween: values relative to current value

# rowsBetween(-2, 0): "previous 2 rows and current row" (physical)
w1 = Window.orderBy("date").rowsBetween(-2, 0)

# rangeBetween(-7, 0): "rows within 7 units less than current value"
# Useful when combined with timestamps/numeric columns
w2 = Window.orderBy("amount").rangeBetween(-100, 0)  # orders within $100 below current
```

---

### Q3: Why are regular Python UDFs slow?

**A:** Regular UDFs are slow because:
1. Data must be serialized from JVM (Java) → Python process
2. Python processes one row at a time
3. Results serialized back from Python → JVM

**Solution**: Use Pandas UDFs (vectorized) — they use Apache Arrow for zero-copy transfer and process batches of rows.

```python
# Regular UDF: ~10× slower
@udf(DoubleType())
def slow_udf(x): return x * 2

# Pandas UDF: ~10× faster
@pandas_udf(DoubleType())
def fast_udf(x: pd.Series) -> pd.Series: return x * 2

# Built-in: ~100× faster (no Python at all)
df.withColumn("result", col("x") * 2)
```

**Order of preference**: Built-in functions > Pandas UDF > Python UDF

---

## 🔑 Key Takeaways

1. **Window functions** = aggregation without losing rows
2. `ROW_NUMBER` for unique ranking, `RANK` for ties-included ranking
3. `LAG`/`LEAD` for comparing with previous/next rows
4. **Always use `explode`** to flatten arrays to rows
5. **`from_json`** to parse JSON strings to structured columns
6. **Pandas UDFs are 10× faster** than regular Python UDFs
7. **Use `broadcast()`** for small lookup table joins
8. **Built-in functions always preferred** over UDFs for performance

---

**Next Session → Day 7: Performance Optimization — Partitioning, Caching, Query Tuning**
