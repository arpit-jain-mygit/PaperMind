# Read-Heavy vs Write-Heavy Use Cases - Crisp Guide

## Simple Definition

| Type | Example | Pattern |
|------|---------|---------|
| **Write-Heavy** | Logging 1M events/sec | Insert, insert, insert... rare reads |
| **Read-Heavy** | YouTube homepage feed | 10K reads for every write |
| **Balanced** | E-commerce order | 5-10 reads per write |

---

## Real Use Cases

### Write-Heavy
- **Event logging/streaming** (VoxAlchemy job queue, metrics, logs)
- **IoT sensor data** (1000s of devices, constant writes)
- **Financial transactions** (high-frequency trading)
- **Message queues** (Kafka, thousands of messages/sec)

### Read-Heavy
- **Social media feeds** (YouTube, TikTok, Instagram)
- **E-commerce product pages** (millions read same product)
- **Leaderboards** (gaming, 100s of reads per update)
- **Job status polling** (many users checking status)
- **Blog/news sites** (read-only after published)

### Balanced
- **E-commerce checkout** (read product → write order)
- **Email** (read inbox → write reply)
- **Chat apps** (read messages → write new message)

---

## How to Achieve Each

### Write-Heavy Architecture

**Goal:** Write as fast as possible, reads can be slow/eventual

```python
# 1. Use Message Queue (not database)
producer.send("job_queue", job_data)  # ← Fast write (Kafka: 1M msgs/sec)

# 2. Batch writes
INSERT INTO logs (data) VALUES (x), (y), (z)  # 1 insert for 3 rows

# 3. No joins (slower)
# ❌ INSERT INTO logs SELECT * FROM users JOIN orders ...

# 4. Async processing
fire_and_forget(process_background_task)  # Don't wait for result
```

**Database choice:**
- **Kafka** (queue, optimized for writes)
- **MongoDB** (append-only, no complex joins)
- **ClickHouse** (columnar, fast bulk inserts)

**VoxAlchemy example:**
```python
# Write-heavy: Job queue
r.lpush("doc_jobs", job_id)  # ← O(1) write, instant

# Then worker reads slowly if needed
job = r.lpop("doc_jobs")  # ← Read can wait
```

---

### Read-Heavy Architecture

**Goal:** Read as fast as possible, writes less frequent

```python
# 1. Heavy caching
@cached(ttl=3600)  # Cache for 1 hour
def get_job_status(job_id):
    return r.hgetall(f"job_status:{job_id}")

# 2. Read replicas
primary_db.write(data)  # Single write
read_replica_1.read(data)  # 100 reads
read_replica_2.read(data)  # 100 reads
read_replica_3.read(data)  # 100 reads

# 3. Denormalized data (pre-computed)
# ❌ SELECT COUNT(*) FROM orders WHERE user_id=123
# ✅ redis.get(f"user_orders_count:{user_id}")

# 4. CDN for static content
# ❌ Serve image from database
# ✅ CloudFront CDN (cached at 200+ edge locations)
```

**Database choice:**
- **Redis** (in-memory cache, <1ms reads)
- **PostgreSQL read replicas** (many readers, 1 writer)
- **Elasticsearch** (fast full-text search)
- **Memcached** (cheap caching layer)

**VoxAlchemy example:**
```python
# Read-heavy: Job status polling
@cached(ttl=10)  # Cache for 10 seconds
def get_status(job_id):
    return r.hgetall(f"job_status:{job_id}")  # ← Cached, <1ms

# User polls every 2 seconds
# First poll: cache miss, 50ms
# Polls 2-5: cache hit, <1ms each
```

---

## Common Pitfalls

| Pitfall | Impact | Fix |
|---------|--------|-----|
| **Using DB for logs** | Writes slow, disk fills | Use Kafka + ClickHouse |
| **Querying every read** | 100ms per read | Add caching layer |
| **Denormalized + inconsistent** | Data mismatch | Update cache atomically |
| **No read replicas** | Single DB is bottleneck | Add 3 read replicas |
| **Cache stampede** | All misses at once | Stagger TTLs |

**Cache stampede example:**
```python
# ❌ BAD: All caches expire at same time (t=3600s)
redis.set(f"product:{id}", data, ex=3600)  # All expire together

# ✓ GOOD: Stagger expiration
ttl = 3600 + random.randint(0, 600)  # 3600-4200 seconds
redis.set(f"product:{id}", data, ex=ttl)  # Spread out
```

---

## Architect-Level Questions

### Q1: "Our system reads 10K times per write. Should we use NoSQL?"

**Answer:**
> "Not necessarily. Trade-off analysis:
> 
> **NoSQL (MongoDB)** pros:
> - Flexible schema (good for unstructured data)
> 
> **NoSQL** cons:
> - Weaker consistency (eventual consistency)
> - No transactions (risky for critical data)
> - Same read latency as SQL (difference is architecture, not DB type)
> 
> **Better approach for 10K:1 read ratio:**
> 1. Use PostgreSQL (or MySQL) with read replicas
> 2. Add Redis cache in front
> 3. Add CDN for static content
> 4. This beats NoSQL for read-heavy workloads
> 
> **When to use NoSQL:**
> - Unstructured data (logs, telemetry)
> - Horizontal scaling needed (sharding)
> - Write-heavy, not read-heavy"

---

## Quick Reference: VoxAlchemy Analysis

| Component | Type | Why | Tech |
|-----------|------|-----|------|
| Job queue | Write | Queue 1000s of jobs/day | Redis LPUSH |
| Job status | Read | Polling 10K reads/write | Redis + 10s cache |
| Cost tracking | Write | Log every operation | ClickHouse + Kafka |
| User stats | Read | Leaderboards | Redis + 24h cache |

---

**Related:** See `NFR.md` for detailed scalability, performance, cost efficiency patterns 📚
