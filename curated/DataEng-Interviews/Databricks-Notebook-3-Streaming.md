# Databricks Notebook 3: Structured Streaming

---

## 1. Stream from Rate Source (Built-in Test Source)

```python
# Simulates data arriving at 100 rows/second
df_stream = spark.readStream.format("rate").option("rowsPerSecond", 100).load()

df_stream.printSchema()
```

Columns: `timestamp` (event arrival time), `value` (incrementing number)

---

## 2. Simple Pass-Through Stream

```python
# Read stream, write to console every 5 seconds
query = df_stream.writeStream \
    .format("console") \
    .option("truncate", False) \
    .trigger(processingTime="5 seconds") \
    .start()

# Stop after 10 seconds (press stop button in notebook)
# query.stop()
```

Notice: New batches arrive every 5 seconds, processing time is fast.

---

## 3. Stateless Transformation

```python
# Add a column, simple transformation
df_with_id = df_stream.withColumn("id", col("value") * 10)

query = df_with_id.writeStream \
    .format("console") \
    .trigger(processingTime="5 seconds") \
    .start()

# query.stop()
```

---

## 4. Stateful: Count Events (Aggregation)

```python
# Count rows in each 10-second window
counts = df_stream.groupBy(window(col("timestamp"), "10 seconds")).count()

query = counts.writeStream \
    .format("console") \
    .trigger(processingTime="5 seconds") \
    .start()

# query.stop()
```

Notice: 
- `window()` groups data into time buckets
- Each micro-batch updates the count
- Output shows evolving counts

---

## 5. Event Time vs. Processing Time

```python
# Data arrives late (simulating real-world disorder)
# Use EVENT TIME (timestamp in data) not processing time

windowed = df_stream \
    .withWatermark("timestamp", "5 seconds") \
    .groupBy(window(col("timestamp"), "10 seconds")) \
    .count()

query = windowed.writeStream \
    .format("console") \
    .option("truncate", False) \
    .trigger(processingTime="5 seconds") \
    .start()

# query.stop()
```

**Watermark("timestamp", "5 seconds")** = wait 5 seconds for late arrivals before finalizing the window.

---

## 6. Incremental Aggregation with State

```python
# Count events per value (stateful aggregation)
per_value_count = df_stream.groupBy("value").count()

query = per_value_count.writeStream \
    .format("console") \
    .option("truncate", False) \
    .trigger(processingTime="5 seconds") \
    .start()

# query.stop()
```

Notice: State store tracks counts across batches. Each value's count increases over time.

---

## 7. Stream to Table (For Queries)

```python
# Write stream to a Databricks table
query = df_stream \
    .select(col("value").alias("event_id"), col("timestamp")) \
    .writeStream \
    .format("delta") \
    .mode("append") \
    .option("checkpointLocation", "/tmp/checkpoint") \
    .table("stream_events")  # Table name

# query.stop()

# Now query the table in another cell
# spark.sql("SELECT COUNT(*) FROM stream_events").show()
```

---

## 8. Multiple Streams + Join

```python
# Stream 1: Events
stream1 = spark.readStream.format("rate").option("rowsPerSecond", 50).load()

# Stream 2: Another rate source (just for demo)
stream2 = spark.readStream.format("rate").option("rowsPerSecond", 30).option("rampUpTime", "1s").load()

# Can't join two streams easily (needs complex state) — advanced topic
```

---

## Exercises

**1. Count events in 5-second windows with watermark**
```python
# Hint: withWatermark → groupBy(window) → count
```

**2. Stream to delta table with append mode**
```python
# Hint: writeStream.format("delta").mode("append").table("name")
```

**3. Add processing metadata (current timestamp)**
```python
# Hint: current_timestamp() function
```

---

## Common Patterns

**Console output (testing):**
```python
.writeStream.format("console").start()
```

**Delta table (production):**
```python
.writeStream.format("delta").mode("append").table("table_name")
```

**With checkpoint (fault tolerance):**
```python
.writeStream.option("checkpointLocation", "/path").table("name")
```

