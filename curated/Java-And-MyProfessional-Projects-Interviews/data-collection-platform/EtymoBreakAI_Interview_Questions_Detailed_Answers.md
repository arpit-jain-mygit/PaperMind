# EtymoBreak AI - Technical Architect Interview Questions & Detailed Answers

---

## **1. High-Level Architecture & Broker Separation**

**Q: Walk us through the high-level architecture of EtymoBreak AI. Why did you choose to separate the quiz broker into a Cloud Run microservice instead of handling it directly in the FastAPI backend?**

### Answer:

**Architecture Overview:**
The system follows a **distributed three-tier architecture**:
- **Presentation Layer**: Angular 22 SPA (served via Vercel)
- **Application Layer**: FastAPI backend (hosted on Render)
- **Data Layer**: PostgreSQL (user profiles) + GCS (quiz history)
- **Broker Service**: Cloud Run microservice for quiz logging

```
┌─────────────────┐
│   Angular SPA   │
│  (Vercel)       │
└────────┬────────┘
         │
    ┌────▼────┐
    │ FastAPI │──────┐
    │ (Render)│      │
    └────┬────┘      │
         │           │
    ┌────▼────┐ ┌────▼──────────┐
    │ PostgreSQL   │ Cloud Run Broker
    │ (User Data)  │ (Quiz Logging)
    └─────────┘ ├──────────┘
                │
            ┌───▼────┐
            │  GCS   │
            └────────┘
```

**Why Separate the Quiz Broker?**

1. **Scalability & Decoupling**
   - Quiz logging is write-heavy and asynchronous—doesn't need to block the API response
   - Cloud Run auto-scales based on demand independently of the main API
   - If quiz logging crashes, it doesn't affect the core educational experience

2. **Data Storage Optimization**
   - Quiz attempts are mostly append-only historical logs → better fit for GCS (object storage) than PostgreSQL
   - PostgreSQL is better for transactional, relational data (user profiles, auth)
   - GCS is cheaper for long-term storage: ~$0.02/GB/month vs. managed DB costs (~$1-5/GB with RDS)

3. **Separation of Concerns**
   - FastAPI focuses on core business logic (authentication, quiz generation, user profiles)
   - Broker handles only quiz attempt persistence
   - Each service has a single responsibility → easier to maintain, test, and debug

4. **Performance Benefits**
   - Frontend gets immediate response without waiting for write-to-database
   - Broker can batch-write quiz attempts to GCS asynchronously
   - Reduces database transaction overhead

**Implementation Details:**
```
Frontend Flow:
1. User completes quiz
2. POST /api/submit-quiz (FastAPI)
3. FastAPI returns success immediately (local validation)
4. Async task: Forward to broker at BROKER_URL with BROKER_SHARED_SECRET
5. Broker validates secret, writes to GCS in user's folder
6. GCS location: gs://BUCKET/user_{user_id}/attempt_{timestamp}.json

Benefits:
- Non-blocking (user doesn't wait for GCS write)
- If broker is down, FastAPI caches and retries
- Simple audit trail in GCS (one file per attempt)
- Can replicate/backup GCS independently
```

**Trade-offs & Challenges:**

| Aspect | Trade-off |
|--------|-----------|
| **Consistency** | Eventual consistency (user data in PostgreSQL, quiz history in GCS). If broker crashes, some quiz attempts may be lost. Mitigated with retry logic. |
| **Operational Complexity** | Need to manage two services instead of one. More deployment pipeline complexity. |
| **Query Complexity** | To get user's quiz history, must query GCS (slower than database). Solutions: cache in Redis, or periodic export to BigQuery. |
| **Cost** | Initially seemed cheaper (GCS + Cloud Run), but operational overhead (monitoring, debugging) adds hidden costs. |

**What I Would Improve:**
- Add a **message queue** (Pub/Sub) between FastAPI and Broker for better reliability
  - FastAPI publishes quiz attempt to Pub/Sub
  - Broker subscribes and processes asynchronously
  - Automatic retries if broker crashes
  
```python
# Current: Direct HTTP
requests.post(BROKER_URL, json=quiz_data, headers={"X-Secret": BROKER_SHARED_SECRET})

# Better: Pub/Sub
from google.cloud import pubsub_v1
publisher = pubsub_v1.PublisherClient()
topic_path = publisher.topic_path(PROJECT_ID, "quiz-attempts")
publisher.publish(topic_path, json.dumps(quiz_data).encode())

# Broker subscribes via Cloud Tasks or subscription
```

---

## **2. Backend Framework & Platform Trade-offs**

**Q: You're using FastAPI on Render over other frameworks/platforms like Django, Node.js, or AWS Lambda. What were the deciding factors?**

### Answer:

**Comparison Matrix:**

| Factor | FastAPI | Django | Node.js | AWS Lambda |
|--------|---------|--------|---------|------------|
| **Development Speed** | ⭐⭐⭐⭐ (async, modern) | ⭐⭐⭐ (batteries-included) | ⭐⭐⭐⭐ (JS everywhere) | ⭐⭐ (event-driven, verbose) |
| **Performance** | ⭐⭐⭐⭐⭐ (async, uvicorn) | ⭐⭐⭐ (synchronous) | ⭐⭐⭐⭐ (V8 engine) | ⭐⭐⭐ (cold starts) |
| **Type Safety** | ⭐⭐⭐⭐ (Pydantic, Python 3.9+) | ⭐⭐ (optional typing) | ⭐⭐⭐⭐ (TypeScript available) | ⭐⭐⭐ (event typing) |
| **Ecosystem** | ⭐⭐⭐⭐ (ML/AI libraries) | ⭐⭐⭐⭐⭐ (largest Python framework) | ⭐⭐⭐⭐⭐ (npm ecosystem) | ⭐⭐⭐ (AWS SDK heavy) |
| **Scalability** | ⭐⭐⭐⭐ (async I/O) | ⭐⭐⭐ (WSGI blocking) | ⭐⭐⭐⭐ (async/await) | ⭐⭐⭐⭐ (serverless scaling) |
| **Deployment** | ⭐⭐⭐⭐ (Docker-native, Render) | ⭐⭐⭐ (Docker, WSGI servers) | ⭐⭐⭐⭐ (Node ecosystem) | ⭐⭐⭐⭐⭐ (zero ops) |
| **Learning Curve** | ⭐⭐⭐ (async concepts) | ⭐⭐ (lots to learn) | ⭐⭐⭐⭐ (if team knows JS) | ⭐⭐⭐ (cloud concepts) |
| **Cost** | ⭐⭐⭐ ($7-15/month on Render) | ⭐⭐⭐ (similar) | ⭐⭐⭐ (similar) | ⭐⭐⭐⭐ (pay-per-execution) |

**Why FastAPI Was Chosen:**

### 1. **Async-First Architecture**
FastAPI is built on `asyncio` and uses async/await natively:
```python
@app.post("/api/submit-quiz")
async def submit_quiz(quiz_data: QuizSchema):
    # Can await multiple operations concurrently
    user_profile = await db.get_user(user_id)
    broker_response = await httpx.post(BROKER_URL, json=quiz_data)
    return {"status": "success"}
```

Django (synchronous by default) would require:
```python
def submit_quiz(request):
    user_profile = User.objects.get(id=request.user.id)  # Blocking
    broker_response = requests.post(BROKER_URL, json=quiz_data)  # Blocking
    return JsonResponse({"status": "success"})
```

**For 1000 concurrent users:**
- FastAPI: Handles with ~20-30 threads (asyncio multiplexing)
- Django: Needs 1000 threads/processes (huge overhead)

### 2. **Mistral GenAI Integration**
Quiz generation requires calling external AI APIs with potential latency:
```python
async def generate_quiz(word: str):
    async with httpx.AsyncClient() as client:
        # Can timeout without blocking other requests
        response = await client.post(
            "https://api.mistral.ai/v1/chat/completions",
            json={"model": "mistral-small", "messages": [...]},
            timeout=15.0
        )
    return response.json()
```

If one quiz generation takes 5s, with Django you'd block a thread. With FastAPI, you're handling other requests while waiting.

### 3. **Type Safety with Pydantic**
FastAPI uses Pydantic for runtime validation:
```python
from pydantic import BaseModel, EmailStr, Field

class UserProfile(BaseModel):
    first_name: str = Field(..., min_length=1, max_length=50)
    last_name: str = Field(..., min_length=1, max_length=50)
    country: str = Field(..., regex="^[A-Z]{2}$")  # ISO 3166-1 alpha-2

@app.post("/api/profile")
async def update_profile(profile: UserProfile):
    # Auto-validated, type-hinted, documented in OpenAPI
    return {"profile": profile}
```

This gives:
- Automatic request validation
- Auto-generated OpenAPI documentation
- TypeScript type generation for frontend

### 4. **Why Not Django?**
- Django is heavier (includes ORM, migrations, admin panel—overkill for this)
- Synchronous by default (requires thread pools for I/O)
- More boilerplate (models, views, urls, serializers)

### 5. **Why Not Node.js?**
- Team expertise in Python (ML libraries, data science)
- Mistral is better integrated in Python ecosystem
- Python libraries: `pydantic`, `sqlalchemy`, `asyncpg` are mature

### 6. **Why Not AWS Lambda?**
- **Cold starts**: Lambda cold start ~500-2000ms (bad for user experience)
- **Monitoring complexity**: Distributed tracing is harder
- **Local development**: Need SAM/serverless framework
- **Cost**: At scale, Lambda gets expensive; Render flat-rate is cheaper
- **Stateful services**: Cloud Run is better for managing persistent connections (DB pools)

**However, Lambda could work for:**
- Async jobs (quiz generation background worker)
- One-time operations (data migration)
- Scheduled tasks (cleaning up old quiz attempts)

### Real Numbers:
```
EtymoBreak AI Estimated Load:
- 100 concurrent users
- Average response time: 200ms (API call) + 3s (Mistral)
- Requests/sec: ~50 req/s

FastAPI on Render:
- Standard plan: $7/month
- Auto-scales with dyno configuration
- Load handling: ~500-1000 concurrent users possible

Django on same:
- Same cost, but would need more dynos due to blocking I/O
- Estimated: $30+/month for same performance

AWS Lambda + API Gateway:
- $0.2 per 1M requests + $3.50 per 1M compute seconds
- At 50 req/s × 3s execution = 150 Lambda-seconds/sec
- Monthly cost: ~$50-100 (more expensive than Render)
```

---

## **3. Quiz Broker Storage Design**

**Q: The quiz broker stores attempt data in GCS instead of PostgreSQL. Walk through this design decision. What are the pros and cons?**

### Answer:

**Architecture Diagram:**
```
User completes quiz
        │
        ▼
   FastAPI API
        │
        ├─ Stores user profile in PostgreSQL
        │  (user_id, first_name, last_name, country)
        │
        ├─ Calls Cloud Run Broker
        │  POST /log-quiz-attempt
        │  {user_id, quiz_data, score, timestamp}
        │
        └─ Broker stores in GCS
           Location: gs://BUCKET/user_{user_id}/attempt_{timestamp}.json
           File Structure:
           {
             "attempt_id": "uuid",
             "user_id": "google_id",
             "timestamp": "2026-06-15T10:30:00Z",
             "quiz_data": {
               "word": "Etymology",
               "difficulty": 3,
               "questions": [...],
               "answers": [...]
             },
             "score": 85,
             "duration_seconds": 240
           }
```

### **Pros of GCS Over PostgreSQL:**

1. **Cost Efficiency**
   ```
   PostgreSQL managed database:
   - Monthly cost: ~$15-50/month (minimum)
   - Storage: $0.17/GB/month
   - 1M quiz attempts × 100KB each = 100GB
   - Cost: 100GB × $0.17 = $17/month + base cost
   
   GCS:
   - Storage: $0.02/GB/month
   - 100GB × $0.02 = $2/month
   - Savings: 85% cheaper
   ```

2. **Unlimited Scalability**
   - PostgreSQL: Connection limits (max 100-200 connections)
   - GCS: Unlimited concurrent writes
   - Quiz logging won't cause database locks

3. **Immutable Audit Trail**
   - Each quiz attempt is a separate file → immutable log
   - Perfect for compliance/auditing requirements
   - Easy to archive old files to Glacier (cost: $0.004/GB/month)

4. **Decoupling from Main Database**
   - Quiz logging doesn't interfere with user authentication/profile queries
   - Database doesn't grow indefinitely (historical data → GCS)
   - Easier to backup/restore main database

5. **Easy Integration with Analytics**
   ```python
   # Can directly load into BigQuery
   from google.cloud import bigquery
   
   client = bigquery.Client()
   load_job = client.load_table_from_uri(
       "gs://BUCKET/user_*/attempt_*.json",
       "project.dataset.quiz_attempts_table"
   )
   ```

### **Cons of GCS Over PostgreSQL:**

1. **Query Complexity**
   ```python
   # PostgreSQL: Single query
   SELECT AVG(score), COUNT(*) FROM quiz_attempts 
   WHERE user_id = ? AND created_at > DATE_SUB(NOW(), INTERVAL 7 DAY)
   
   # GCS: Must read all files
   from google.cloud import storage
   
   storage_client = storage.Client()
   bucket = storage_client.bucket(BUCKET_NAME)
   blobs = bucket.list_blobs(prefix=f"user_{user_id}/")
   
   total_score = 0
   count = 0
   for blob in blobs:
       data = json.loads(blob.download_as_string())
       if data['timestamp'] > week_ago:
           total_score += data['score']
           count += 1
   
   avg_score = total_score / count
   ```

2. **Latency**
   - GCS list/read: ~200-500ms (cold)
   - PostgreSQL query: ~10-50ms
   - For real-time dashboards, this is noticeable

3. **Eventual Consistency**
   - GCS has eventual consistency
   - If broker crashes after accept but before write, quiz attempt is lost
   - Database transactions guarantee ACID

4. **Data Duplication**
   - User profile in PostgreSQL, quiz attempts in GCS
   - If schema changes, must migrate both places
   - Risk of data inconsistency

5. **Complex Queries**
   ```python
   # "Which word gives users the most difficulty?"
   # PostgreSQL: Complex JOIN with GROUP BY, ORDER BY
   # GCS: Must scan ALL quiz attempts across ALL users
   
   # "Find users who abandoned after question 2"
   # GCS: Must read every attempt file, parse JSON, check completion
   ```

### **Hybrid Solution (What I Would Implement):**

```python
# Best of both worlds
# 1. Store raw quiz attempt in GCS (audit trail)
# 2. Store aggregated metrics in PostgreSQL

# Broker Service
@app.post("/log-quiz-attempt")
async def log_attempt(attempt: QuizAttempt):
    # 1. Write to GCS (immutable, cheap)
    gcs_client.upload_from_string(
        f"user_{attempt.user_id}/attempt_{uuid.uuid4()}.json",
        json.dumps(attempt.dict())
    )
    
    # 2. Update aggregated metrics in PostgreSQL
    async with db.transaction():
        quiz_stat = await db.get_or_create(
            QuizStatistic,
            user_id=attempt.user_id,
            word=attempt.word,
            date=date.today()
        )
        quiz_stat.total_attempts += 1
        quiz_stat.total_score += attempt.score
        quiz_stat.avg_score = quiz_stat.total_score / quiz_stat.total_attempts
        await db.save(quiz_stat)
    
    return {"status": "logged"}

# Frontend can quickly query stats from PostgreSQL
# Auditors can access full history from GCS
```

### **Implementation Details:**

```python
# GCS file structure for a user
gs://etymobreak-bucket/
├── user_google_id_1/
│   ├── attempt_2026-06-15T10:30:00Z_uuid1.json
│   ├── attempt_2026-06-15T10:45:00Z_uuid2.json
│   └── attempt_2026-06-15T11:00:00Z_uuid3.json
└── user_google_id_2/
    ├── attempt_2026-06-15T10:15:00Z_uuid4.json
    └── attempt_2026-06-15T10:32:00Z_uuid5.json

# Each file ~50-200KB (depends on number of quiz questions)
# Retention: 1 year free, then archive to Glacier
```

**When to Use GCS vs PostgreSQL:**

| Use Case | Choice | Reason |
|----------|--------|--------|
| User authentication | PostgreSQL | Needs fast ACID transactions |
| User profile (name, country) | PostgreSQL | Relational, frequently updated |
| Quiz attempt raw data | GCS | Immutable log, high volume, cheap |
| Quiz statistics (avg score) | PostgreSQL | Fast queries for dashboard |
| Historical analysis | BigQuery (Federated) | Query GCS + PostgreSQL together |
| Compliance/audit trail | GCS | Immutable, time-stamped records |

---

## **4. 10x Growth Scalability**

**Q: How would you handle a 10x increase in concurrent users? What components would become bottlenecks first?**

### Answer:

**Current Estimated Capacity:**
```
Current Users: 1,000 concurrent
Current Architecture Limits:
- Angular SPA: ~unlimited (client-side, scales with browser caches)
- FastAPI (Render): ~500-1000 concurrent users (Render standard plan)
- PostgreSQL: 100 connections (Render default) → bottleneck at ~200 users
- Cloud Run Broker: 100 concurrent instances (default) → bottleneck at ~10,000 requests/min
- GCS: unlimited throughput
```

**With 10x Growth (10,000 Concurrent Users):**

### **Bottleneck Analysis (in order of failure):**

#### **1. PostgreSQL Connection Pool (First Bottleneck)**
```python
# Current setup
DATABASE_URL = "postgresql://user:pass@db.render.com/etymobreak"
# Default: max_connections = 100

# Problem: With 10k concurrent users, each making 1-2 DB queries
# We'd need 10k-20k connections → resource exhaustion

# At 10k concurrent, connection wait time becomes critical
# Users see: "Error: too many connections"
```

**Solution:**
```python
# 1. PgBouncer connection pooling
# Sits between app and DB, multiplexes connections
# Reduces needed connections from 10k to 200

# 2. Use sqlalchemy with async pooling
from sqlalchemy.ext.asyncio import create_async_engine

engine = create_async_engine(
    "postgresql+asyncpg://user:pass@db/etymobreak",
    pool_size=20,  # Keep only 20 DB connections
    max_overflow=10,  # Allow 10 more under stress
    echo_pool=True,
    pool_pre_ping=True  # Validate connections before use
)

# 3. Implement Redis caching for frequently accessed data
from redis import asyncio as aioredis

redis_client = await aioredis.from_url("redis://cache.internal:6379")

@app.get("/api/user/{user_id}")
async def get_user(user_id: str):
    # Check cache first
    cached = await redis_client.get(f"user:{user_id}")
    if cached:
        return json.loads(cached)
    
    # If miss, query DB
    user = await db.get_user(user_id)
    await redis_client.setex(f"user:{user_id}", 3600, json.dumps(user))
    return user
```

**Estimated Cost:**
- PostgreSQL upgrade: +$20/month (managed DB)
- PgBouncer: +$5/month (or free if self-hosted)
- Redis cache: +$10/month (managed Redis)

#### **2. FastAPI Server Concurrency (Second Bottleneck)**
```
Current: Render standard plan
- 4 workers × 128MB RAM = 512MB total
- Uvicorn: ~50-100 concurrent requests per worker
- Total capacity: ~200-400 concurrent requests

With 10k users:
- Each user making 1 request every 5 seconds
- Requests/sec: 10,000 / 5 = 2,000 req/sec
- At 100ms average response time: ~200 concurrent requests
- But peak traffic: 5x = 1,000 concurrent → exceeds capacity

Current failures:
- Response time > 30s
- "Service temporarily unavailable" errors
- Users unable to load quiz
```

**Solution:**
```yaml
# 1. Horizontal scaling with Render
# Instead of 1 instance, deploy 5-10 instances
render:
  instances: 5
  memory_per_instance: 512MB
  auto_scaling: true
  cpu_threshold: 70%

# 2. Load balancing
# Render provides automatic load balancing across instances
# FastAPI handles this via gunicorn workers

# 3. Optimize code to reduce response time
# Current: 200ms average
# Target: 50-100ms average

# Example optimization:
# Before: Fetch user profile + quiz data + statistics
@app.get("/api/dashboard")
async def get_dashboard(user_id: str):
    user = await db.get_user(user_id)  # 20ms
    quiz_stats = await db.get_stats(user_id)  # 30ms
    recent_quizzes = await db.get_recent(user_id)  # 50ms
    return {"user": user, "stats": quiz_stats, "quizzes": recent_quizzes}
# Total: 100ms

# After: Parallel queries with asyncio
@app.get("/api/dashboard")
async def get_dashboard(user_id: str):
    user, stats, quizzes = await asyncio.gather(
        db.get_user(user_id),
        db.get_stats(user_id),
        db.get_recent(user_id)
    )
    return {"user": user, "stats": stats, "quizzes": quizzes}
# Total: 50ms (parallel execution)
```

**Cost:**
- 5 instances × $7/month = $35/month (increase from $7)

#### **3. Mistral API Rate Limits (Third Bottleneck)**
```
Current behavior:
- User clicks "Generate Quiz"
- API calls Mistral: POST https://api.mistral.ai/v1/chat/completions
- Mistral response time: 2-5 seconds
- Rate limit: 1,000 requests/month (free tier) or paid tier

With 10k users and 1 quiz per user per day:
- Daily requests: 10,000
- Monthly: 300,000 requests → exceeds free tier

At peak (all 10k users requesting simultaneously):
- Need to handle 10,000 requests/sec to Mistral
- Mistral max throughput: ~100-500 req/sec (depends on tier)
- Result: 95% of requests queued or fail
```

**Solution:**
```python
# 1. Pre-generate questions and cache them
# Instead of generating on-demand, generate batch offline

from celery import shared_task

@shared_task
def generate_quiz_batch():
    """Run nightly to pre-generate quizzes"""
    words = db.get_all_words()  # ~10,000 words
    for word in words:
        quiz = mistral_api.generate_quiz(word)
        cache.set(f"quiz:{word}", quiz, ttl=7*24*3600)  # 7 days

# 2. Implement queue with retry logic
from google.cloud import tasks_v2

async def queue_quiz_generation(word: str):
    """Queue quiz generation if not cached"""
    cached = await cache.get(f"quiz:{word}")
    if cached:
        return cached  # Return cached quiz
    
    # If not cached, queue for generation
    client = tasks_v2.CloudTasksClient()
    project = "my-project"
    queue = "quiz-generation"
    location = "asia-south1"
    
    parent = client.queue_path(project, location, queue)
    task = {
        "http_request": {
            "http_method": tasks_v2.HttpMethod.POST,
            "url": "https://my-api.com/internal/generate-quiz",
            "headers": {"Content-Type": "application/json"},
            "body": json.dumps({"word": word}).encode(),
        }
    }
    
    response = client.create_task(request={"parent": parent, "task": task})
    return {"status": "queued", "quiz_id": response.name}

# 3. Upgrade Mistral tier
# Free: 1,000 req/month
# Pro: 100,000 req/month = $5-10/month
# Enterprise: Unlimited = $100+/month

# For 300k req/month: Pro tier sufficient
```

**Cost:**
- Mistral Pro: $10/month
- Cloud Tasks: ~$0.40 per 1M tasks (negligible)

#### **4. GCS Throughput (Fourth Bottleneck)**
```
Current: Cloud Run Broker writes quiz attempts to GCS
- Throughput: 10,000 requests/sec from Broker
- GCS can handle: 1,000 writes/sec per prefix (object name)

If all writes go to gs://BUCKET/user_{user_id}/:
- 10,000 users × 1 write/sec = 10,000 writes/sec
- But same prefix → bottleneck

GCS error: "429 Too Many Requests"
```

**Solution:**
```python
# 1. Use hierarchical prefixes to increase throughput
# Instead of: gs://BUCKET/user_{user_id}/
# Use: gs://BUCKET/{first_2_chars}/{user_id}/

# This distributes writes across 256 prefixes
# New capacity: 1,000 writes/sec × 256 = 256,000 writes/sec ✓

import hashlib

def get_gcs_prefix(user_id: str):
    hash_prefix = hashlib.md5(user_id.encode()).hexdigest()[:2]
    return f"{hash_prefix}/{user_id}/"

# gs://BUCKET/a1/user_id_1/attempt_*.json
# gs://BUCKET/a2/user_id_2/attempt_*.json
# ... (256 different prefixes)

# 2. Batch writes to GCS
# Instead of writing each quiz immediately, batch them
from collections import deque

quiz_buffer = deque(maxlen=1000)

@app.post("/log-quiz-attempt")
async def log_attempt(attempt: QuizAttempt):
    quiz_buffer.append(attempt)
    
    if len(quiz_buffer) >= 100:  # Batch size
        await flush_buffer()
    
    return {"status": "buffered"}

async def flush_buffer():
    batch_data = list(quiz_buffer)
    quiz_buffer.clear()
    
    # Write all at once
    await gcs_client.upload_from_string(
        f"{get_gcs_prefix(batch_data[0].user_id)}/batch_{uuid.uuid4()}.jsonl",
        "\n".join(json.dumps(q.dict()) for q in batch_data)
    )

# Alternatively: Use Cloud Pub/Sub for buffering
```

**Cost:**
- GCS remains negligible (~$2-5/month)

#### **5. Angular Frontend Bundle (Fifth Bottleneck)**
```
Current:
- Bundle size: ~500KB (gzipped)
- With 10k users/day downloading: 5GB/day bandwidth
- Vercel bandwidth: Free 100GB/month included
- At 5GB/day = 150GB/month → exceeds free tier

Cost: $20-30/month for extra bandwidth
```

**Solution:**
```typescript
// 1. Code splitting
// Instead of one bundle, split by features
// quiz.module.ts
// admin.module.ts
// learn.module.ts
// Each loaded on-demand

// 2. Lazy loading routes
const routes: Routes = [
  { path: 'quiz', loadChildren: () => import('./quiz/quiz.module').then(m => m.QuizModule) },
  { path: 'learn', loadChildren: () => import('./learn/learn.module').then(m => m.LearnModule) }
];

// 3. CDN caching
// Vercel automatically uses CDN, but can optimize cache headers

// 4. Compress static assets
// Target: 500KB → 250KB bundle
// Use webpack BundleAnalyzer to identify large dependencies

npm run build -- --analyze
```

**Cost:**
- Vercel Pro: $20/month for priority support + more bandwidth
- Or switch to CloudFlare ($200/month) for unlimited bandwidth

### **Scalability Timeline:**

| Users | Bottleneck | Solution | Cost Increase |
|-------|-----------|----------|----------------|
| 1,000 | PostgreSQL connections | PgBouncer + Redis cache | +$15/month |
| 5,000 | FastAPI throughput | Scale to 3 instances | +$15/month |
| 10,000 | Mistral API rate | Upgrade to Pro tier + pre-generate | +$10/month |
| 50,000 | Database queries | Implement read replicas | +$50/month |
| 100,000 | Database storage | Archive to BigQuery | +$20/month |

### **Complete 10x Growth Architecture:**

```
┌──────────────────────────────────────────────────┐
│         CloudFlare CDN (Global Edge)             │
├──────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────┐    │
│  │   Vercel (5 instances)                   │    │
│  │   Angular SPA                            │    │
│  │   Bundle: 250KB (optimized)              │    │
│  └──────────────┬──────────────────────────┘    │
├──────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────┐   │
│  │  Load Balancer                           │    │
│  │  (Render or Nginx)                       │    │
│  └──────────────┬──────────────────────────┘   │
├─┬──────────────────────────────────────────────┬─┤
│ │                                              │ │
│ ▼                                              ▼ │
│ ┌─────────────────┐                  ┌─────────────────┐
│ │  FastAPI Pod 1  │                  │  FastAPI Pod 5  │
│ │  (Render)       │        ...       │  (Render)       │
│ │  512MB RAM      │                  │  512MB RAM      │
│ └────────┬────────┘                  └────────┬────────┘
│          │                                    │
│          └────────────────┬───────────────────┘
│                           │
│          ┌────────────────┼────────────────┐
│          │                │                │
│          ▼                ▼                ▼
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  │  PostgreSQL  │  │  Redis Cache │  │   Cloud Run  │
│  │  + PgBouncer │  │  (6GB)       │  │   Broker     │
│  │  (100 conn)  │  │              │  │   (10 inst)  │
│  └──────────────┘  └──────────────┘  └────────┬─────┘
│                                                │
│                                    ┌───────────┴──────────┐
│                                    │                      │
│                                    ▼                      ▼
│                            ┌──────────────┐      ┌──────────────┐
│                            │     GCS      │      │   Mistral    │
│                            │ (Quiz logs)  │      │   API (Pro)  │
│                            │              │      │              │
│                            └──────────────┘      └──────────────┘
```

**Final Monthly Cost at 10x Scale:**
```
Current: ~$22/month (FastAPI $7 + Vercel $15 free)

At 10x:
- Render FastAPI (5 instances): $35/month
- PostgreSQL managed: $25/month
- Redis: $10/month
- Mistral Pro: $10/month
- Vercel Pro: $20/month
- CloudFlare: $200/month (optional)
- GCS + Cloud Run: ~$5/month
- Monitoring (DataDog): $15/month

Total: $120-320/month
Cost per user: $0.012-0.032/month (very efficient)
```

---

## **5. Broker Inter-Service Authentication**

**Q: How is the `BROKER_SHARED_SECRET` managed, rotated, and secured?**

### Answer:

**Current Implementation:**
```python
# FastAPI Backend (Render)
@app.post("/api/submit-quiz")
async def submit_quiz(quiz_data: QuizSchema):
    # After validation...
    headers = {
        "X-Broker-Secret": os.getenv("BROKER_SHARED_SECRET"),
        "Content-Type": "application/json"
    }
    
    async with httpx.AsyncClient() as client:
        response = await client.post(
            f"{BROKER_URL}/log-attempt",
            json=quiz_data.dict(),
            headers=headers,
            timeout=10.0
        )

# Cloud Run Broker
@app.post("/log-attempt")
async def log_attempt(request: Request, attempt: QuizAttempt):
    # Verify secret
    secret = request.headers.get("X-Broker-Secret")
    
    if secret != os.getenv("BROKER_SHARED_SECRET"):
        raise HTTPException(status_code=401, detail="Unauthorized")
    
    # Proceed with logging...
```

### **Problems with Current Approach:**

1. **Shared Secret in Environment Variable**
   - If `.env` file is committed → secret is exposed
   - If Render env var is compromised → entire system is compromised
   - No audit trail of who accessed the secret

2. **No Secret Rotation**
   - Secret is hardcoded (no expiration)
   - If compromised, must manually update everywhere
   - Takes time: update FastAPI → deploy → wait for rollout

3. **Transmitted in Plain HTTP Headers**
   - If not using HTTPS → secret visible in transit (unlikely if using HTTPS, but still risky)
   - No mechanism to prevent replay attacks

4. **Single Secret for All Services**
   - If FastAPI is compromised, broker is also compromised
   - No per-service credentials

### **Improved Solution: Google Secret Manager + mTLS**

#### **Step 1: Store Secret in Google Secret Manager**
```bash
# Create secret
gcloud secrets create broker-shared-secret \
  --replication-policy="automatic" \
  --data-file=- <<< "super-secret-key-$(date +%s)"

# Grant access
gcloud secrets add-iam-policy-binding broker-shared-secret \
  --member="serviceAccount:render-service@project.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud secrets add-iam-policy-binding broker-shared-secret \
  --member="serviceAccount:cloud-run-broker@project.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

#### **Step 2: FastAPI Retrieves Secret at Runtime**
```python
from google.cloud import secretmanager

def get_broker_secret():
    client = secretmanager.SecretManagerServiceClient()
    name = f"projects/my-project/secrets/broker-shared-secret/versions/latest"
    response = client.access_secret_version(request={"name": name})
    return response.payload.data.decode("UTF-8")

BROKER_SECRET = get_broker_secret()  # Retrieved at startup, cached

@app.post("/api/submit-quiz")
async def submit_quiz(quiz_data: QuizSchema):
    headers = {
        "X-Broker-Secret": BROKER_SECRET,
        "Content-Type": "application/json"
    }
    # ... rest of code
```

#### **Step 3: Implement Secret Rotation**
```python
# Rotate secret every 30 days
from datetime import datetime, timedelta

def should_rotate_secret():
    # Check when secret was last rotated
    secret_age_file = Path("/tmp/secret_age")
    
    if not secret_age_file.exists():
        return True
    
    last_rotation = datetime.fromisoformat(secret_age_file.read_text())
    return datetime.now() - last_rotation > timedelta(days=30)

@app.on_event("startup")
async def startup_event():
    global BROKER_SECRET
    
    if should_rotate_secret():
        BROKER_SECRET = await rotate_broker_secret()
        Path("/tmp/secret_age").write_text(datetime.now().isoformat())

async def rotate_broker_secret():
    client = secretmanager.SecretManagerServiceClient()
    
    # Generate new secret
    new_secret = secrets.token_urlsafe(32)
    
    # Add new version
    project_id = "my-project"
    secret_id = "broker-shared-secret"
    
    response = client.add_secret_version(
        request={
            "parent": f"projects/{project_id}/secrets/{secret_id}",
            "payload": {"data": new_secret.encode("UTF-8")},
        }
    )
    
    # Broker will automatically use latest version
    return new_secret

# CLI command to rotate immediately
@app.post("/admin/rotate-secret")
async def rotate_secret_admin(admin_token: str):
    # Verify admin token
    if not verify_admin_token(admin_token):
        raise HTTPException(status_code=403)
    
    new_secret = await rotate_broker_secret()
    
    # Log to audit trail
    logger.info(f"Secret rotated at {datetime.now()}")
    
    return {"status": "rotated", "new_version": "latest"}
```

#### **Step 4: Use mTLS Instead of Shared Secret**
```python
# More secure: Mutual TLS (both client and server verify each other)

# Generate certificates
# CA cert, server cert, client cert

# FastAPI as mTLS client
import ssl

ssl_context = ssl.create_default_context(cafile="/etc/ssl/certs/ca.crt")
ssl_context.load_cert_chain(
    certfile="/etc/ssl/certs/client.crt",
    keyfile="/etc/ssl/certs/client.key"
)

async with httpx.AsyncClient(verify=ssl_context) as client:
    response = await client.post(
        f"https://{BROKER_URL}/log-attempt",  # HTTPS only
        json=quiz_data.dict()
        # No need for secret header - identity verified by cert
    )

# Cloud Run Broker as mTLS server
from fastapi.security import HTTPBasic, HTTPBasicCredentials
import ssl

ssl_context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
ssl_context.load_cert_chain(
    certfile="/etc/ssl/certs/server.crt",
    keyfile="/etc/ssl/certs/server.key"
)

# Verify client certificate
ssl_context.verify_mode = ssl.CERT_REQUIRED
ssl_context.load_verify_locations("/etc/ssl/certs/ca.crt")

if __name__ == "__main__":
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8443,
        ssl_keyfile="/etc/ssl/certs/server.key",
        ssl_certfile="/etc/ssl/certs/server.crt",
        ssl_ca_certs="/etc/ssl/certs/ca.crt"
    )
```

#### **Step 5: Audit Logging**
```python
from google.cloud import logging as cloud_logging

logging_client = cloud_logging.Client()
logger = logging_client.logger("broker-auth")

@app.post("/log-attempt")
async def log_attempt(request: Request, attempt: QuizAttempt):
    secret = request.headers.get("X-Broker-Secret")
    
    if secret != BROKER_SECRET:
        # Log failed auth attempt
        logger.log_struct({
            "severity": "WARNING",
            "event": "auth_failure",
            "source_ip": request.client.host,
            "timestamp": datetime.now().isoformat(),
            "reason": "invalid_secret"
        })
        raise HTTPException(status_code=401)
    
    # Log successful auth
    logger.log_struct({
        "severity": "INFO",
        "event": "quiz_logged",
        "user_id": attempt.user_id,
        "timestamp": datetime.now().isoformat(),
        "source_ip": request.client.host
    })
```

### **Complete Secret Management Architecture:**

```
┌─────────────────────────────────────┐
│  Render (FastAPI Backend)           │
│  Service Account: render-sa         │
└────────────────┬────────────────────┘
                 │
         ┌───────▼────────┐
         │ At Startup:    │
         │ 1. Authenticate with GCP
         │ 2. Fetch secret from
         │    Secret Manager
         │ 3. Cache in memory
         │ 4. Check if rotation needed
         │ 5. If yes, rotate
         └───────┬────────┘
                 │
        ┌────────▼──────────────┐
        │ Google Secret Manager │
        │                       │
        │ broker-shared-secret: │
        │ - version 1 (old)     │
        │ - version 2 (current) │
        │ - version 3 (latest)  │
        │                       │
        │ Access Log:           │
        │ - render-sa: 2026-06-15 10:30 │
        │ - cloud-run-broker-sa: 10:31  │
        └────────┬──────────────┘
                 │
         ┌───────▼────────┐
         │ Cloud Run      │
         │ Broker         │
         │ Service Account: │
         │ cloud-run-broker-sa
         └────────────────┘

Rotation Schedule (30 days):
Version 1: Created 2026-05-16 (Old - can deactivate)
Version 2: Created 2026-06-15 (Current - active)
Version 3: Generated 2026-07-15 (New - auto-promote to latest)
```

### **Cost & Compliance:**

| Aspect | Details |
|--------|---------|
| **Google Secret Manager** | $0.06 per secret per month + $0.06 per 10k API calls |
| **Audit Logging** | Cloud Logging charges apply (~$0.50/GB) |
| **Compliance** | SOC 2: All auth attempts logged; HIPAA: Encryption at rest |
| **Rotation Frequency** | 30-90 days (recommended); can be automated |

---

## **6. Data Consistency Between Services**

**Q: How do you ensure data consistency between PostgreSQL and GCS? What happens if the broker fails after receiving a quiz attempt?**

### Answer:

**Problem Scenario:**
```
Timeline:
10:00:00 - User submits quiz
10:00:01 - FastAPI receives request, validates
10:00:02 - FastAPI calls Broker
10:00:03 - Broker receives request, acknowledges
10:00:04 - Broker starts writing to GCS
10:00:05 - GCS write is halfway through... ❌ NETWORK FAILS
          - Broker has not confirmed write to FastAPI
          - FastAPI doesn't know if quiz attempt was logged
          
Result: Quiz attempt lost (user data inconsistent)
```

### **Current State (Problematic):**
```python
# FastAPI
@app.post("/api/submit-quiz")
async def submit_quiz(quiz_data: QuizSchema, user_id: str):
    # Validate quiz
    score = calculate_score(quiz_data)
    
    # Call Broker (no retry logic, no acknowledgment)
    try:
        async with httpx.AsyncClient(timeout=5) as client:
            response = await client.post(
                f"{BROKER_URL}/log-attempt",
                json={
                    "user_id": user_id,
                    "quiz_data": quiz_data.dict(),
                    "score": score
                },
                headers={"X-Broker-Secret": BROKER_SECRET}
            )
    except Exception as e:
        logger.error(f"Broker failed: {e}")
        # What to do? Return error to user? Retry? Log locally?
    
    return {"score": score}

# Broker (Cloud Run)
@app.post("/log-attempt")
async def log_attempt(attempt: dict):
    try:
        # Write to GCS
        blob = bucket.blob(
            f"user_{attempt['user_id']}/attempt_{uuid.uuid4()}.json"
        )
        blob.upload_from_string(json.dumps(attempt))
        return {"status": "success"}
    except Exception as e:
        raise HTTPException(status_code=500)
```

### **Solution 1: Synchronous Write with Retry Logic**

```python
# FastAPI Backend
from tenacity import retry, wait_exponential, stop_after_attempt

@retry(
    wait=wait_exponential(multiplier=1, min=2, max=10),  # 2s, 4s, 8s...
    stop=stop_after_attempt(3)
)
async def call_broker_with_retry(attempt_data: dict):
    async with httpx.AsyncClient(timeout=10) as client:
        response = await client.post(
            f"{BROKER_URL}/log-attempt",
            json=attempt_data,
            headers={"X-Broker-Secret": BROKER_SECRET}
        )
        if response.status_code != 200:
            raise Exception(f"Broker returned {response.status_code}")
        return response.json()

@app.post("/api/submit-quiz")
async def submit_quiz(quiz_data: QuizSchema, user_id: str):
    score = calculate_score(quiz_data)
    
    attempt = {
        "user_id": user_id,
        "quiz_data": quiz_data.dict(),
        "score": score,
        "timestamp": datetime.now().isoformat(),
        "attempt_id": str(uuid.uuid4())  # Idempotency key
    }
    
    # Retry up to 3 times
    try:
        result = await call_broker_with_retry(attempt)
        return {
            "score": score,
            "status": "logged",
            "attempt_id": attempt["attempt_id"]
        }
    except Exception as e:
        logger.error(f"Failed to log after 3 retries: {e}")
        # Fallback: Log locally (see Solution 2)
        await local_queue.append(attempt)
        return {
            "score": score,
            "status": "queued_locally",
            "error": "Broker unavailable"
        }
```

### **Solution 2: Asynchronous with Message Queue (RECOMMENDED)**

Instead of direct HTTP calls, use **Google Pub/Sub** for decoupled communication:

```python
# FastAPI Backend publishes to Pub/Sub
from google.cloud import pubsub_v1
import json

publisher = pubsub_v1.PublisherClient()
topic_path = publisher.topic_path("my-project", "quiz-attempts-topic")

@app.post("/api/submit-quiz")
async def submit_quiz(quiz_data: QuizSchema, user_id: str):
    score = calculate_score(quiz_data)
    
    attempt = {
        "user_id": user_id,
        "quiz_data": quiz_data.dict(),
        "score": score,
        "timestamp": datetime.now().isoformat(),
        "attempt_id": str(uuid.uuid4())
    }
    
    # Publish to Pub/Sub (non-blocking, async)
    message_json = json.dumps(attempt)
    message_bytes = message_json.encode("utf-8")
    
    # Add custom attributes for filtering
    publish_future = publisher.publish(
        topic_path,
        message_bytes,
        user_id=user_id,
        attempt_id=attempt["attempt_id"]
    )
    
    message_id = publish_future.result()
    
    return {
        "score": score,
        "status": "published",
        "message_id": message_id
    }

# Cloud Run Broker subscribes to Pub/Sub
from google.cloud import pubsub_v1
from concurrent.futures import ThreadPoolExecutor

subscriber = pubsub_v1.SubscriberClient()
subscription_path = subscriber.subscription_path("my-project", "quiz-attempts-sub")

def callback(message):
    """Process quiz attempt"""
    try:
        attempt = json.loads(message.data.decode())
        
        # Idempotent write to GCS
        user_id = attempt["user_id"]
        attempt_id = attempt["attempt_id"]
        blob_path = f"user_{user_id}/attempt_{attempt_id}.json"
        
        # Check if already written (idempotency)
        blob = bucket.blob(blob_path)
        if blob.exists():
            logger.info(f"Attempt {attempt_id} already exists, skipping")
            message.ack()
            return
        
        # Write to GCS
        blob.upload_from_string(json.dumps(attempt))
        
        logger.info(f"Logged attempt {attempt_id}")
        message.ack()  # Acknowledge success
        
    except Exception as e:
        logger.error(f"Failed to process: {e}")
        # Don't ack - Pub/Sub will retry
        # After max retries, message goes to dead letter queue

# Start subscriber
def start_broker():
    streaming_pull_future = subscriber.subscribe(subscription_path, callback=callback)
    streaming_pull_future.result()

if __name__ == "__main__":
    start_broker()
```

### **Pub/Sub Architecture Advantages:**

```
┌─────────────────┐
│   FastAPI       │
│   - Receives quiz
│   - Publishes to Pub/Sub
│   - Returns immediately ✓
└────────┬────────┘
         │
         ▼
    ┌──────────────────────────────┐
    │   Google Cloud Pub/Sub       │
    │   Topic: quiz-attempts       │
    │   (Durable message buffer)   │
    └────────────┬─────────────────┘
                 │
      ┌──────────┼──────────┐
      │          │          │
      ▼          ▼          ▼
   ┌─────┐   ┌─────┐   ┌─────┐
   │Broker│   │Broker│  │Cloud│  (Auto-scales)
   │ Inst1│   │ Inst2│  │Run3 │
   └──┬──┘   └──┬──┘   └──┬──┘
      │         │         │
      └─────────┼─────────┘
              ┌─▼─────────────┐
              │      GCS      │
              │  (All writes  │
              │  succeed)     │
              └───────────────┘

Benefits:
1. FastAPI returns immediately (better UX)
2. If any broker instance fails, Pub/Sub retries automatically
3. Auto-scaling: Heavy load → more broker instances
4. Durability: Messages persist for 7 days if no processor
```

### **Solution 3: Transactional Outbox Pattern**

```python
# Store both user action AND broker call in one transaction

from sqlalchemy import Column, String, JSON, DateTime
from datetime import datetime
import uuid

class QuizAttemptOutbox(Base):
    """Outbox table for reliable async processing"""
    __tablename__ = "quiz_attempts_outbox"
    
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String)
    quiz_data = Column(JSON)
    score = Column(Integer)
    status = Column(String, default="pending")  # pending, published, failed
    created_at = Column(DateTime, default=datetime.utcnow)
    published_at = Column(DateTime, nullable=True)

@app.post("/api/submit-quiz")
async def submit_quiz(quiz_data: QuizSchema, user_id: str):
    async with db.transaction():
        # Single transaction: compute score + insert outbox row
        score = calculate_score(quiz_data)
        
        outbox_entry = QuizAttemptOutbox(
            user_id=user_id,
            quiz_data=quiz_data.dict(),
            score=score
        )
        
        db.add(outbox_entry)
        await db.commit()  # Both succeed or both fail
    
    return {"score": score}

# Background task: Poll outbox and publish to Pub/Sub
@app.on_event("startup")
async def start_outbox_poller():
    asyncio.create_task(poll_outbox())

async def poll_outbox():
    """Every 5 seconds, publish pending outbox entries"""
    while True:
        try:
            # Get unpublished attempts
            pending = await db.query(QuizAttemptOutbox).filter(
                QuizAttemptOutbox.status == "pending"
            ).all()
            
            for entry in pending:
                try:
                    # Publish to Pub/Sub
                    message_bytes = json.dumps({
                        "user_id": entry.user_id,
                        "quiz_data": entry.quiz_data,
                        "score": entry.score,
                        "attempt_id": entry.id
                    }).encode()
                    
                    publisher.publish(topic_path, message_bytes)
                    
                    # Mark as published
                    entry.status = "published"
                    entry.published_at = datetime.utcnow()
                    await db.commit()
                    
                except Exception as e:
                    logger.error(f"Failed to publish {entry.id}: {e}")
                    entry.status = "failed"
                    await db.commit()
        
        except Exception as e:
            logger.error(f"Outbox poller error: {e}")
        
        await asyncio.sleep(5)  # Poll every 5 seconds
```

### **Idempotency (Preventing Duplicates)**

When broker retries, ensure it doesn't create duplicate records:

```python
# Broker side: Use attempt_id as unique key

class QuizAttemptInGCS(Base):
    """Track which attempts are already logged"""
    __tablename__ = "quiz_attempts_logged"
    attempt_id = Column(String, primary_key=True)
    user_id = Column(String)
    logged_at = Column(DateTime)

def callback(message):
    attempt = json.loads(message.data.decode())
    attempt_id = attempt["attempt_id"]
    
    # Check if already processed (idempotency)
    existing = db.query(QuizAttemptInGCS).filter(
        QuizAttemptInGCS.attempt_id == attempt_id
    ).first()
    
    if existing:
        logger.info(f"Attempt {attempt_id} already logged")
        message.ack()
        return
    
    # Write to GCS
    blob = bucket.blob(f"user_{attempt['user_id']}/attempt_{attempt_id}.json")
    blob.upload_from_string(json.dumps(attempt))
    
    # Record as logged
    db.add(QuizAttemptInGCS(
        attempt_id=attempt_id,
        user_id=attempt["user_id"],
        logged_at=datetime.utcnow()
    ))
    db.commit()
    
    message.ack()
```

### **Failure Scenarios & Recovery:**

| Scenario | Current | With Pub/Sub | Recovery |
|----------|---------|-------------|----------|
| **Broker crashes** | Quiz lost | Pub/Sub retries | Message replayed when broker restarts |
| **Network timeout** | Unclear state | Automatic retry | After 7 days, dead-letter queue |
| **Duplicate message** | Double write | Idempotency key | Check `attempt_id` exists in GCS |
| **FastAPI crashes** | Unpublished message | In PostgreSQL outbox | Outbox poller republishes on restart |

### **Monitoring & Alerting:**

```python
from google.cloud import monitoring_v3
from google.cloud import monitoring_dashboard_v1

# Alert if Pub/Sub queue is too large (broker falling behind)
client = monitoring_v3.MetricsServiceClient()
query = monitoring_v3.QueryTimeSeriesRequest(
    name="projects/my-project",
    query="fetch pubsub_topic | filter resource.topic_id == 'quiz-attempts-topic'"
)

# If queue depth > 1000 for 5 minutes, alert ops
```

**Final Recommendation:**
Use **Pub/Sub + Outbox Pattern** for production. It gives you:
- Durability (messages persisted)
- Scalability (auto-scaling brokers)
- Reliability (automatic retries)
- Consistency (idempotent writes)
- Observability (message tracking)

Cost: Pub/Sub ~$0.10 per 1M messages (negligible for this scale)

---

## **7. Cross-Service Communication & Network**

**Q: You use both Render and Cloud Run. How do you handle communication, latency, and network failures?**

### Answer:

**Architecture:**
```
┌──────────────────────────────────────────┐
│          Internet                        │
└──────────────────────────────────────────┘
        │                   │
        ▼                   ▼
   ┌─────────┐          ┌──────────┐
   │ Render  │          │ Cloud Run│
   │ FastAPI │          │ Broker   │
   │         │          │          │
   │ Region: │          │ Region:  │
   │ Oregon  │          │ asia-    │
   │ USA     │          │ south1   │
   │         │          │ India    │
   └────┬────┘          └────┬─────┘
        │                   │
        └───────────────────┘
        Cross-region latency:
        USA → India = 150-200ms
```

### **Latency Analysis:**

```
Typical Request Flow:

1. User submits quiz (Angular → FastAPI): ~100ms (local network)
2. FastAPI calls Broker (Render → Cloud Run): ~150-200ms
3. Broker writes to GCS: ~100ms
4. Total: ~350-400ms ✓ (acceptable)

But at peak:
- If Broker is slow (backed up): Can reach 1-2 seconds
- User sees loading spinner, may abandon
```

### **Network Reliability Issues:**

```python
# Problem: Direct HTTP call without timeouts
response = requests.post(f"{BROKER_URL}/log-attempt", json=data)
# If Broker is down or slow, FastAPI hangs indefinitely
# Response to user takes minutes instead of seconds

# Problem: No circuit breaker
# If Broker is down, FastAPI keeps retrying, wasting resources
# After 100 failed requests, FastAPI itself becomes slow
```

### **Solution 1: Timeouts & Circuit Breaker**

```python
import httpx
from tenacity import retry, wait_exponential, stop_after_attempt
from pybreaker import CircuitBreaker

# Circuit breaker pattern
broker_breaker = CircuitBreaker(
    fail_max=5,  # Fail after 5 consecutive errors
    reset_timeout=60,  # Try again after 60 seconds
    exclude=[httpx.TimeoutException]  # Don't count timeouts as failures
)

@retry(
    wait=wait_exponential(multiplier=1, min=1, max=10),
    stop=stop_after_attempt(3)
)
async def call_broker(attempt_data: dict):
    """Call broker with timeout and retry logic"""
    
    # Check if circuit is open
    if broker_breaker.opened:
        logger.warn("Broker circuit open, falling back to local queue")
        raise CircuitBreakerListenerException("Broker unavailable")
    
    try:
        async with httpx.AsyncClient(
            timeout=httpx.Timeout(10.0)  # 10 second timeout
        ) as client:
            response = await client.post(
                f"{BROKER_URL}/log-attempt",
                json=attempt_data,
                headers={"X-Broker-Secret": BROKER_SECRET}
            )
            
            if response.status_code != 200:
                broker_breaker.fail()  # Record failure
                raise Exception(f"Status {response.status_code}")
            
            broker_breaker.succeed()  # Reset on success
            return response.json()
    
    except Exception as e:
        broker_breaker.fail()
        raise

@app.post("/api/submit-quiz")
async def submit_quiz(quiz_data: QuizSchema, user_id: str):
    score = calculate_score(quiz_data)
    
    try:
        await call_broker({
            "user_id": user_id,
            "quiz_data": quiz_data.dict(),
            "score": score
        })
        return {"score": score, "status": "logged"}
    
    except Exception as e:
        # Fallback: Store locally and retry later
        await local_queue.enqueue({
            "user_id": user_id,
            "quiz_data": quiz_data.dict(),
            "score": score,
            "retry_at": datetime.now() + timedelta(minutes=5)
        })
        return {"score": score, "status": "queued", "error": str(e)}
```

### **Solution 2: Regional Deployment**

```yaml
# Instead of single Render (USA) + Cloud Run (India)
# Deploy replicas in multiple regions

Architecture:
┌────────────────────────────────────┐
│  CDN / Load Balancer               │
│  (CloudFlare)                      │
└──────┬──────────────────┬──────────┘
       │                  │
       ▼                  ▼
   ┌────────┐         ┌────────┐
   │ Render │         │ Cloud  │
   │ USA    │         │ Run    │
   │        │         │ Asia   │
   └────────┘         └────────┘
       │                  │
       ▼                  ▼
   ┌────────────────┬────────────────┐
   │  FastAPI 1     │   FastAPI 2    │
   │  (Oregon)      │   (Mumbai)     │
   └────────┬───────┴────────┬───────┘
            │                │
            ▼                ▼
   ┌──────────────┐  ┌──────────────┐
   │ Broker 1     │  │ Broker 2     │
   │ (us-east1)   │  │ (asia-south) │
   └──────────────┘  └──────────────┘
```

### **Solution 3: Request Locality**

```python
# Route quiz submissions to nearest broker

from geolite2 import geolite2

@app.post("/api/submit-quiz")
async def submit_quiz(request: Request, quiz_data: QuizSchema):
    client_ip = request.client.host
    geo = geolite2.reader().get(client_ip)
    user_location = geo['location']['country_code']
    
    # Choose nearest broker based on user location
    if user_location in ['US', 'CA', 'MX']:
        broker_url = "https://broker-us.etymobreak.com"
    elif user_location in ['IN', 'SG', 'JP']:
        broker_url = "https://broker-asia.etymobreak.com"
    else:
        broker_url = "https://broker-eu.etymobreak.com"
    
    # Route to local broker → lower latency
```

### **Solution 4: Adaptive Retry Strategy**

```python
import random

async def call_broker_adaptive(attempt_data: dict):
    """Exponential backoff with jitter"""
    
    backoff_base = 1
    max_retries = 5
    
    for attempt in range(max_retries):
        try:
            async with httpx.AsyncClient(timeout=httpx.Timeout(10)) as client:
                response = await client.post(
                    f"{BROKER_URL}/log-attempt",
                    json=attempt_data
                )
                
                if response.status_code == 200:
                    return response.json()
                elif response.status_code in [429, 503]:
                    # Rate limited or service unavailable
                    # Retry with longer wait
                    raise httpx.HTTPError("Temporarily unavailable")
                else:
                    raise httpx.HTTPError(f"Status {response.status_code}")
        
        except (httpx.TimeoutException, httpx.HTTPError) as e:
            if attempt < max_retries - 1:
                # Exponential backoff with jitter
                wait_time = backoff_base * (2 ** attempt) + random.uniform(0, 1)
                logger.warn(f"Retry {attempt + 1}/{max_retries} after {wait_time}s")
                await asyncio.sleep(wait_time)
            else:
                raise
```

### **Solution 5: Health Checks & Monitoring**

```python
# FastAPI periodically checks Broker health
from apscheduler.schedulers.asyncio import AsyncIOScheduler

scheduler = AsyncIOScheduler()

@scheduler.scheduled_job('interval', minutes=1)
async def check_broker_health():
    """Check if Broker is healthy every minute"""
    try:
        async with httpx.AsyncClient(timeout=5) as client:
            response = await client.get(
                f"{BROKER_URL}/health",
                headers={"X-Broker-Secret": BROKER_SECRET}
            )
            
            if response.status_code == 200:
                broker_health = True
                response_time = response.elapsed.total_seconds() * 1000
                logger.info(f"Broker healthy (latency: {response_time}ms)")
                
                # Send metric to monitoring system
                statsd.gauge('broker.latency_ms', response_time)
            else:
                broker_health = False
                logger.warn("Broker unhealthy")
    
    except Exception as e:
        broker_health = False
        logger.error(f"Broker health check failed: {e}")
        
        # Alert on-call if broker is down
        send_alert(
            severity="critical",
            message=f"Broker unreachable: {e}",
            service="etymobreak-ai"
        )

# Broker implements health check endpoint
@app.get("/health")
async def health_check():
    # Check if can write to GCS
    try:
        blob = bucket.blob("_health_check.txt")
        blob.upload_from_string(f"healthy at {datetime.now()}")
        blob.delete()
        return {"status": "healthy", "gcs": "ok"}
    except Exception as e:
        return {"status": "unhealthy", "gcs": "error", "error": str(e)}, 503
```

### **Latency Monitoring Dashboard:**

```python
from prometheus_client import Histogram, Counter

# Prometheus metrics
request_latency = Histogram(
    'broker_request_latency_seconds',
    'Latency of broker requests',
    buckets=[0.1, 0.5, 1.0, 2.0, 5.0]
)

request_errors = Counter(
    'broker_request_errors_total',
    'Total broker request errors',
    ['error_type']
)

# Measure latency
import time

@app.post("/api/submit-quiz")
async def submit_quiz(quiz_data: QuizSchema):
    start = time.time()
    
    try:
        await call_broker(quiz_data.dict())
        request_latency.observe(time.time() - start)
    except Exception as e:
        request_errors.labels(error_type=type(e).__name__).inc()
        raise
```

### **Final Resilience Architecture:**

```python
# Comprehensive resilience pattern

class BrokerService:
    def __init__(self):
        self.circuit_breaker = CircuitBreaker(fail_max=5)
        self.local_queue = asyncio.Queue()
        self.retry_scheduler = AsyncIOScheduler()
        self.metrics = PrometheusMetrics()
    
    async def log_attempt(self, attempt_data: dict):
        # Try to call broker
        try:
            result = await self._call_broker_with_timeout(attempt_data)
            self.metrics.record_success()
            return result
        
        except Exception as e:
            # Log locally for retry
            await self.local_queue.put(attempt_data)
            self.metrics.record_fallback()
            logger.warn(f"Logged to local queue: {e}")
    
    async def _call_broker_with_timeout(self, data: dict):
        if self.circuit_breaker.opened:
            raise CircuitBreakerOpen()
        
        try:
            async with httpx.AsyncClient(timeout=10) as client:
                response = await client.post(
                    f"{BROKER_URL}/log-attempt",
                    json=data
                )
                self.circuit_breaker.succeed()
                return response.json()
        except Exception as e:
            self.circuit_breaker.fail()
            raise
    
    async def process_local_queue(self):
        """Periodically retry local queue"""
        while True:
            try:
                attempt = self.local_queue.get_nowait()
                await self.log_attempt(attempt)
            except asyncio.QueueEmpty:
                await asyncio.sleep(5)
```

---

## **8-10. Quiz Broker Storage & Etymology Data**

**Q8: How do you query aggregated data from GCS? How do you ensure data privacy?**

**Q9: How large is the etymology dataset? How do you load/cache it?**

**Q10: What's your data retention and deletion strategy?**

### Combined Answer:

### **Data Storage Architecture:**

```
3 Different Data Layers:

1. PostgreSQL (User & Relational Data)
   - User profiles (firstName, lastName, country)
   - User authentication (Google ID)
   - Quiz statistics (aggregated)
   
2. GCS (Quiz Attempt Log)
   - Raw quiz attempts (immutable)
   - One file per attempt (~50-200KB)
   - Retention: 2 years then archive
   
3. Memory/Browser Cache (Etymology Data)
   - aaptprep_root_centric_final.json (~5-10MB)
   - ~50,000 words with etymologies
   - Cached in browser IndexedDB
   - Served from Vercel CDN
```

### **Etymology Dataset Details:**

```json
// aaptprep_root_centric_final.json structure
{
  "Etymology": [
    {
      "word": "Accumulate",
      "roots": [
        {
          "root": "Ad",
          "meaning": "To, towards"
        },
        {
          "root": "Cumulus",
          "meaning": "Heap, pile"
        }
      ],
      "literal_meaning": "To heap towards",
      "family": [
        "Accumulation",
        "Cumulative",
        "Cumulus"
      ],
      "description": "Latin: ad (to) + cumulare (heap)",
      "example": "She accumulated wealth over decades"
    },
    // ... 49,999 more words
  ]
}

File Size: ~8-10MB (JSON)
After gzip: ~1.5-2MB
```

### **Loading Strategy:**

```python
# Option 1: Load on Backend (Current)
import json

async def load_etymology_data():
    """Load at startup"""
    with open("public/aaptprep_root_centric_final.json") as f:
        return json.load(f)

ETYMOLOGY_DATA = None

@app.on_event("startup")
async def startup():
    global ETYMOLOGY_DATA
    ETYMOLOGY_DATA = await load_etymology_data()

# Serve to frontend
@app.get("/api/etymology/{word}")
async def get_etymology(word: str):
    for entry in ETYMOLOGY_DATA["Etymology"]:
        if entry["word"].lower() == word.lower():
            return entry
    return {"error": "Word not found"}

# Problem: 8MB in memory on each server
# With 5 servers: 40MB total (wasteful)
```

### **Better: Browser Caching with IndexedDB**

```typescript
// Angular Service: Load etymology data once, cache in browser

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import Dexie from 'dexie';

@Injectable({ providedIn: 'root' })
export class EtymologyService {
  private db = new Dexie('etymobreak');
  
  constructor(private http: HttpClient) {
    this.db.version(1).stores({
      words: '&word'  // Indexed by word
    });
  }
  
  async getEtymology(word: string): Promise<any> {
    // Check local cache first (instant)
    const cached = await this.db.table('words').get(word);
    if (cached) {
      return cached;
    }
    
    // Fetch from backend
    const data = await this.http.get(`/api/etymology/${word}`).toPromise();
    
    // Cache in IndexedDB (local storage)
    await this.db.table('words').add(data);
    
    return data;
  }
  
  async preloadEtymologyData() {
    """Load full etymology dataset on first use"""
    const existing = await this.db.table('words').count();
    
    if (existing > 0) {
      return;  // Already loaded
    }
    
    // Fetch full dataset
    const response = await this.http
      .get('/api/etymology/all?format=jsonl')
      .toPromise();
    
    // Batch insert (faster)
    const words = response.split('\n').map(l => JSON.parse(l));
    await this.db.table('words').bulkAdd(words);
  }
}

// Use in component
@Component({
  selector: 'app-quiz',
  templateUrl: './quiz.component.html'
})
export class QuizComponent implements OnInit {
  constructor(private etymology: EtymologyService) {}
  
  async ngOnInit() {
    // Preload on app start
    await this.etymology.preloadEtymologyData();
  }
}
```

### **Serving Etymology Data Efficiently:**

```python
# Option 1: Serve as JSONL (JSON Lines) for streaming
@app.get("/api/etymology/all")
async def get_all_etymology(format: str = "json"):
    """Stream large dataset"""
    
    if format == "jsonl":
        # JSONL: Each line is a complete JSON object
        def generator():
            with open("public/aaptprep_root_centric_final.json") as f:
                data = json.load(f)
                for word_entry in data["Etymology"]:
                    yield json.dumps(word_entry) + "\n"
        
        return StreamingResponse(
            generator(),
            media_type="application/x-ndjson"
        )
    else:
        # Regular JSON
        return FileResponse("public/aaptprep_root_centric_final.json")

# Frontend fetches with streaming
async function loadEtymologyStreamingFromAPI() {
  const response = await fetch('/api/etymology/all?format=jsonl');
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  
  while (true) {
    const {done, value} = await reader.read();
    if (done) break;
    
    const lines = decoder.decode(value).split('\n');
    for (const line of lines) {
      if (line) {
        const word = JSON.parse(line);
        // Save to IndexedDB
        db.words.add(word);
      }
    }
  }
}
```

### **Querying Quiz Attempts from GCS:**

```python
# Problem: Quiz attempts are in GCS files
# "How many users completed quiz this week?"
# "What's the average score for word X?"

from google.cloud import storage, bigquery
from datetime import datetime, timedelta

# Solution 1: Export to BigQuery for analytics
async def export_quiz_attempts_to_bigquery():
    """Weekly job: Export GCS → BigQuery"""
    
    bq_client = bigquery.Client()
    
    # Create external table pointing to GCS
    dataset_id = "etymobreak"
    table_id = "quiz_attempts_external"
    table_ref = bq_client.dataset(dataset_id).table(table_id)
    
    external_config = bigquery.ExternalConfig('NEWLINE_DELIMITED_JSON')
    external_config.source_uris = [
        f'gs://{BUCKET_NAME}/user_*/attempt_*.json'
    ]
    
    external_config.schema = [
        bigquery.SchemaField("attempt_id", "STRING"),
        bigquery.SchemaField("user_id", "STRING"),
        bigquery.SchemaField("score", "INTEGER"),
        bigquery.SchemaField("timestamp", "TIMESTAMP"),
        bigquery.SchemaField("quiz_data", "JSON"),
    ]
    
    table = bigquery.Table(table_ref, external_config=external_config)
    table = bq_client.create_table(table, exists_ok=True)
    
    # Now can query
    query = """
    SELECT
      EXTRACT(WEEK FROM timestamp) as week,
      COUNT(DISTINCT user_id) as unique_users,
      AVG(score) as avg_score,
      MAX(score) as max_score
    FROM `etymobreak.quiz_attempts_external`
    WHERE timestamp >= DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY)
    GROUP BY week
    """
    
    results = bq_client.query(query).result()
    return list(results)

# Solution 2: Use GCS Stats API (if you just need basic stats)
async def get_user_quiz_history(user_id: str):
    """Get all quiz attempts for a user"""
    
    storage_client = storage.Client()
    bucket = storage_client.bucket(BUCKET_NAME)
    
    # List all files for user
    blobs = bucket.list_blobs(prefix=f"user_{user_id}/")
    
    attempts = []
    for blob in blobs:
        data = json.loads(blob.download_as_string())
        attempts.append(data)
    
    # Sort by timestamp
    attempts.sort(key=lambda x: x['timestamp'])
    
    return {
        "user_id": user_id,
        "total_attempts": len(attempts),
        "avg_score": sum(a['score'] for a in attempts) / len(attempts),
        "attempts": attempts
    }
```

### **Data Privacy & Compliance:**

```python
# GDPR: Right to be forgotten

from google.cloud import storage

async def delete_user_data(user_id: str):
    """Completely delete user's data (GDPR)"""
    
    # 1. Delete from PostgreSQL
    async with db.transaction():
        user = await db.get_user(user_id)
        
        if not user:
            raise HTTPException(status_code=404)
        
        # Delete profile
        await db.delete(user)
        
        # Log deletion for audit trail
        logger.info(f"Deleted user {user_id}")
    
    # 2. Delete from GCS (all quiz attempts)
    storage_client = storage.Client()
    bucket = storage_client.bucket(BUCKET_NAME)
    
    blobs = bucket.list_blobs(prefix=f"user_{user_id}/")
    for blob in blobs:
        blob.delete()
    
    logger.info(f"Deleted {len(list(blobs))} quiz attempts from GCS")
    
    # 3. Log to audit trail for compliance
    audit_logger.log({
        "event": "user_data_deleted",
        "user_id": user_id,
        "timestamp": datetime.now(),
        "reason": "GDPR request"
    })
    
    return {"status": "deleted"}

# Data encryption
from cryptography.fernet import Fernet

class EncryptedUserProfile(Base):
    """Sensitive user data encrypted at rest"""
    __tablename__ = "user_profiles_encrypted"
    
    user_id = Column(String, primary_key=True)
    first_name_encrypted = Column(String)  # Encrypted
    last_name_encrypted = Column(String)   # Encrypted
    country = Column(String)  # Not sensitive
    
    @property
    def first_name(self):
        cipher = Fernet(os.getenv("ENCRYPTION_KEY"))
        return cipher.decrypt(self.first_name_encrypted).decode()

# Data retention
async def archive_old_quizzes():
    """Move quizzes older than 1 year to Glacier"""
    
    storage_client = storage.Client()
    bucket = storage_client.bucket(BUCKET_NAME)
    
    one_year_ago = datetime.now() - timedelta(days=365)
    
    blobs = bucket.list_blobs()
    for blob in blobs:
        if blob.time_created < one_year_ago:
            # Move to Glacier (cheaper long-term storage)
            blob.storage_class = "COLDLINE"  # $0.004/GB vs $0.02/GB
            blob.recompose_from_component_blobs()

# Audit logging
async def log_access(user_id: str, action: str, resource: str):
    """Log all data access for compliance"""
    
    client = cloud_logging.Client()
    logger = client.logger("data_access_audit")
    
    logger.log_struct({
        "timestamp": datetime.now().isoformat(),
        "user_id": user_id,
        "action": action,  # "read", "write", "delete"
        "resource": resource,
        "ip_address": request.client.host,
        "user_agent": request.headers.get("User-Agent")
    }, severity="INFO")
```

### **Data Retention Policy:**

```
Active Data (PostgreSQL):
- User profiles: Keep forever (unless deleted)
- Quiz statistics: Keep forever

Hot Data (GCS - Standard):
- Recent quiz attempts (< 3 months): Standard ($0.02/GB)

Warm Data (GCS - Nearline):
- Quiz attempts (3-12 months): Nearline ($0.01/GB)
- Automatically moved via lifecycle policy

Cold Data (GCS - Coldline):
- Quiz attempts (1-2 years): Coldline ($0.004/GB)
- Compliance archive

Deleted Data:
- Permanent delete after 7-day grace period
- Cryptographic erasure
- Audit log entry created
```

---

## **11-13. Authentication & Security**

**Q11: How do you validate OAuth tokens? What's the token refresh strategy?**

**Q12: Is Client Secret exposed? How do you prevent token theft?**

**Q13: How do you prevent broker abuse/DDoS?**

### Combined Answer:

### **OAuth Flow (Google Sign-In):**

```typescript
// Frontend (Angular)
// 1. User clicks "Sign in with Google"

import { GoogleAuthService } from '@ng-gapi/core';

@Component(...)
export class LoginComponent {
  constructor(private googleAuth: GoogleAuthService) {}
  
  async signIn() {
    // Google Sign-In returns ID token
    const user = await this.googleAuth.signIn();
    
    // ID token contains:
    // {
    //   "iss": "https://accounts.google.com",
    //   "sub": "user_id_123",
    //   "aud": "CLIENT_ID.apps.googleusercontent.com",
    //   "iat": 1234567890,
    //   "exp": 1234571490,  // Expires in 1 hour
    // }
    
    const idToken = user.getAuthResponse().id_token;
    
    // Send to backend
    await this.http.post('/api/auth/google', {
      idToken: idToken
    }).toPromise();
  }
}
```

```python
# Backend (FastAPI) - Validate Token
from google.oauth2 import id_token
from google.auth.transport import requests

GOOGLE_CLIENT_ID = os.getenv("GOOGLE_CLIENT_ID")

@app.post("/api/auth/google")
async def google_auth(request: GoogleAuthRequest):
    try:
        # Verify signature and validity
        idinfo = id_token.verify_oauth2_token(
            request.idToken,
            requests.Request(),
            GOOGLE_CLIENT_ID
        )
        
        # Extract user info
        google_id = idinfo['sub']
        email = idinfo['email']
        
        # Check if user exists
        user = await db.get_user(google_id)
        
        if not user:
            # First time: Create user
            user = User(
                google_id=google_id,
                email=email,
                created_at=datetime.now()
            )
            db.add(user)
            await db.commit()
        
        # Create FastAPI session token
        session_token = jwt.encode({
            'user_id': google_id,
            'exp': datetime.utcnow() + timedelta(hours=24),
            'iat': datetime.utcnow(),
            'type': 'session'
        }, SECRET_KEY, algorithm='HS256')
        
        # Also create refresh token (longer expiry)
        refresh_token = jwt.encode({
            'user_id': google_id,
            'exp': datetime.utcnow() + timedelta(days=30),
            'iat': datetime.utcnow(),
            'type': 'refresh'
        }, REFRESH_SECRET_KEY, algorithm='HS256')
        
        # Store refresh token in HTTP-only cookie (secure)
        response = JSONResponse({
            "status": "success",
            "access_token": session_token
        })
        
        # Set secure, HTTP-only cookie (prevent JavaScript access)
        response.set_cookie(
            "refresh_token",
            refresh_token,
            max_age=30*24*3600,  # 30 days
            secure=True,  # HTTPS only
            httponly=True,  # Not accessible to JavaScript
            samesite="Strict"  # Prevent CSRF
        )
        
        return response
    
    except Exception as e:
        logger.error(f"Auth failed: {e}")
        raise HTTPException(status_code=401, detail="Invalid token")

# Validate access token on every request
async def get_current_user(request: Request):
    """Dependency for protected endpoints"""
    
    auth_header = request.headers.get("Authorization")
    if not auth_header or not auth_header.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="No token")
    
    token = auth_header.split(" ")[1]
    
    try:
        payload = jwt.decode(
            token,
            SECRET_KEY,
            algorithms=["HS256"]
        )
        user_id = payload.get("user_id")
        token_type = payload.get("type")
        
        if token_type != "session":
            raise HTTPException(status_code=401, detail="Invalid token type")
        
        return user_id
    
    except jwt.ExpiredSignatureError:
        # Token expired - user needs to refresh
        raise HTTPException(
            status_code=401,
            detail="Token expired",
            headers={"WWW-Authenticate": "Bearer"}
        )
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=401, detail="Invalid token")

@app.get("/api/user/profile")
async def get_profile(user_id: str = Depends(get_current_user)):
    user = await db.get_user(user_id)
    return {"id": user.id, "name": user.name, "country": user.country}
```

### **Token Refresh Strategy:**

```typescript
// Frontend (Angular)
// When access token expires, use refresh token to get new one

export class AuthInterceptor implements HttpInterceptor {
  constructor(private http: HttpClient, private router: Router) {}
  
  intercept(req: HttpRequest<any>, next: HttpHandler) {
    // Add access token to all requests
    const token = localStorage.getItem('access_token');
    
    if (token) {
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }
    
    return next.handle(req).pipe(
      catchError(error => {
        if (error.status === 401) {
          // Access token expired
          return this.handle401(req, next);
        }
        return throwError(() => error);
      })
    );
  }
  
  private handle401(req: HttpRequest<any>, next: HttpHandler) {
    // Use refresh token to get new access token
    return this.http.post<{access_token: string}>(
      '/api/auth/refresh',
      {}
      // Refresh token is in HTTP-only cookie (sent automatically)
    ).pipe(
      switchMap(response => {
        // Store new access token
        localStorage.setItem('access_token', response.access_token);
        
        // Retry original request with new token
        return next.handle(
          req.clone({
            setHeaders: {
              Authorization: `Bearer ${response.access_token}`
            }
          })
        );
      }),
      catchError(error => {
        // Refresh failed - redirect to login
        this.router.navigate(['/login']);
        return throwError(() => error);
      })
    );
  }
}
```

```python
# Backend: Refresh token endpoint

@app.post("/api/auth/refresh")
async def refresh_token(request: Request):
    """Use refresh token to get new access token"""
    
    # Get refresh token from HTTP-only cookie
    refresh_token = request.cookies.get("refresh_token")
    
    if not refresh_token:
        raise HTTPException(status_code=401, detail="No refresh token")
    
    try:
        payload = jwt.decode(
            refresh_token,
            REFRESH_SECRET_KEY,
            algorithms=["HS256"]
        )
        
        if payload.get("type") != "refresh":
            raise HTTPException(status_code=401)
        
        user_id = payload.get("user_id")
        
        # Check if user still exists and is not suspended
        user = await db.get_user(user_id)
        if not user or user.is_suspended:
            raise HTTPException(status_code=401, detail="User not found or suspended")
        
        # Generate new access token
        new_access_token = jwt.encode({
            'user_id': user_id,
            'exp': datetime.utcnow() + timedelta(hours=1),
            'iat': datetime.utcnow(),
            'type': 'session'
        }, SECRET_KEY, algorithm='HS256')
        
        return {"access_token": new_access_token}
    
    except jwt.ExpiredSignatureError:
        # Refresh token expired - must re-login
        raise HTTPException(status_code=401, detail="Refresh token expired")
```

### **Token Storage Best Practices:**

```typescript
// WRONG: Store token in localStorage
localStorage.setItem('token', token);  // Vulnerable to XSS

// RIGHT: Store in HTTP-only cookie
// Backend sets via response.set_cookie(..., httponly=True)
// Automatically sent with requests

// For extra security, also store in memory (cleared on refresh)
class TokenService {
  private tokenInMemory: string | null = null;
  
  setToken(token: string) {
    this.tokenInMemory = token;  // Cleared on page refresh
  }
  
  getToken(): string | null {
    return this.tokenInMemory;
  }
}
```

### **Preventing Token Theft - Defense in Depth:**

```
1. HTTPS Only
   - All traffic must be HTTPS
   - No token transmitted over HTTP

2. HTTP-Only Cookies
   - Refresh token in HTTP-only cookie
   - JavaScript cannot access (prevents XSS steal)

3. SameSite Cookie Policy
   - response.set_cookie(..., samesite="Strict")
   - Prevents CSRF attacks

4. Token Expiration
   - Access token: 1 hour (short-lived)
   - Refresh token: 30 days (longer)
   - If access token stolen, limited damage

5. Content Security Policy (CSP)
   - Prevent inline scripts
   - Prevent unauthorized API calls
   
   // Set in FastAPI response header
   response.headers["Content-Security-Policy"] = (
       "default-src 'self'; "
       "script-src 'self' https://accounts.google.com; "
       "style-src 'self' 'unsafe-inline'; "
       "img-src 'self' https:; "
       "connect-src 'self' https://api.google.com"
   )

6. Logout & Token Revocation
   - Delete refresh token from cookie
   - Remove from client storage
```

### **Broker Security - Prevent Abuse:**

```python
# Problem: Broker allows unauthenticated access
# {"--allow-unauthenticated": true}
# Anyone can flood broker with fake quiz attempts

# Solution 1: Rate Limiting

from slowapi import Limiter
from slowapi.util import get_remote_address

limiter = Limiter(key_func=get_remote_address)

@app.post("/log-attempt")
@limiter.limit("100/minute")  # Max 100 requests per minute per IP
async def log_attempt(request: Request, attempt: QuizAttempt):
    # Also verify shared secret
    secret = request.headers.get("X-Broker-Secret")
    if secret != BROKER_SECRET:
        raise HTTPException(status_code=401)
    
    # Log to GCS
    return {"status": "logged"}

# Solution 2: Require Valid API Key

@app.post("/log-attempt")
async def log_attempt(request: Request, attempt: QuizAttempt):
    # Require both secret AND valid source IP
    secret = request.headers.get("X-Broker-Secret")
    source_ip = request.client.host
    
    if secret != BROKER_SECRET:
        logger.warn(f"Invalid secret from {source_ip}")
        raise HTTPException(status_code=401)
    
    # Whitelist FastAPI server IP
    ALLOWED_IPS = [
        "render-fastapi.internal",  # Internal DNS
        "18.1.2.3"  # Render static IP (if available)
    ]
    
    # Use service account verification instead
    try:
        # Verify request came from FastAPI service account
        creds = google.auth.default()
        request_from_sa = verify_service_account(
            request.headers.get("Authorization")
        )
        if request_from_sa != "fastapi-service@project.iam.gserviceaccount.com":
            raise HTTPException(status_code=403)
    except:
        raise HTTPException(status_code=403, detail="Unauthorized service")

# Solution 3: DDoS Protection with Cloud Armor

# Deploy behind Cloud Load Balancer with Cloud Armor
# terraform/cloud_armor.tf

resource "google_compute_security_policy" "broker_policy" {
  name = "broker-ddos-protection"
  
  # Rule 1: Rate limit per IP
  rule {
    action = "rate_based_ban"
    priority = "1"
    description = "Limit to 100 requests/minute per IP"
    
    rate_limit_options {
      conform_action = "allow"
      exceed_action = "deny(429)"
      rate_limit_threshold {
        count = 100
        interval_sec = 60
      }
      ban_duration_sec = 600  # Ban for 10 minutes
    }
  }
  
  # Rule 2: Block obviously bad traffic
  rule {
    action = "deny(403)"
    priority = "2"
    description = "Block if no User-Agent (likely bot)"
    
    match {
      expr {
        expression = "!has_request_header('user-agent')"
      }
    }
  }
  
  # Rule 3: Geo-blocking (optional)
  rule {
    action = "deny(403)"
    priority = "3"
    description = "Only allow from target regions"
    
    match {
      versioned_expr = "CEL"
      expr {
        expression = "origin.region_code in ['US', 'IN', 'SG', 'GB']"
      }
    }
  }
}
```

### **Complete Authentication Flow Diagram:**

```
┌─────────────────────────────────────┐
│        User Browser (Angular)       │
└────────────────┬────────────────────┘
                 │
        1. Click "Sign in with Google"
                 │
                 ▼
        ┌────────────────────┐
        │  Google OAuth      │
        │  Consent Screen    │
        └────────┬───────────┘
                 │
        2. User authorizes
                 │
                 ▼
        ┌────────────────────┐
        │  Gets ID Token     │
        │  (expires in 1hr)  │
        └────────┬───────────┘
                 │
        3. Send to FastAPI: POST /api/auth/google
                 │
                 ▼
        ┌─────────────────────────────┐
        │   FastAPI Backend           │
        │   1. Verify ID Token        │
        │      (check signature)      │
        │   2. Get user_id from token │
        │   3. Create session token   │
        │   4. Create refresh token   │
        └────────┬────────────────────┘
                 │
        4. Returns:
        - access_token in JSON
        - refresh_token in HTTP-only cookie
                 │
                 ▼
        ┌─────────────────────────────┐
        │   Browser Storage           │
        │   - access_token in memory  │
        │   - refresh_token in cookie │
        │     (secure, httponly)      │
        └─────────────────────────────┘

Subsequent Requests:
┌──────────────────────┐
│  GET /api/quiz       │
│  Authorization:      │
│  Bearer {access_}    │
└──────┬───────────────┘
       │
       ▼
┌──────────────────────┐
│  FastAPI            │
│  1. Verify token    │
│  2. Check exp time  │
│  3. Get user_id     │
└──────┬───────────────┘
       │
   ✓ Token valid    ✗ Token expired
       │               │
       ▼               ▼
   Return data   Call /api/auth/refresh
                      │
                      ▼
                Use refresh token
                (in HTTP-only cookie)
                      │
                      ▼
                Get new access token
                      │
                      ▼
                Retry original request
```

---

## **14-16. Performance & Scalability**

**Q14: How do you optimize the Angular frontend build?**

**Q15: How do you approach responsive design?**

**Q16: How do you handle Mistral AI quiz generation latency?**

### Combined Answer:

### **Frontend Optimization:**

```bash
# Analyze bundle size
npm run build -- --analyze

# Current bundle metrics
- index.js: 250KB (gzipped)
- styles.css: 50KB
- Total: 300KB
# Target: 150KB (50% reduction)
```

```typescript
// 1. Lazy Loading Routes

const routes: Routes = [
  { path: '', component: HomeComponent },
  {
    path: 'quiz',
    loadChildren: () => import('./quiz/quiz.module').then(m => m.QuizModule)
  },
  {
    path: 'learn',
    loadChildren: () => import('./learn/learn.module').then(m => m.LearnModule)
  },
  {
    path: 'admin',
    canActivate: [AdminGuard],
    loadChildren: () => import('./admin/admin.module').then(m => m.AdminModule)
  }
];

// Only loads quiz.module when user navigates to /quiz
// Reduces initial bundle by 30-40%
```

```typescript
// 2. Component Code Splitting

// quiz.module.ts
import { NgModule } from '@angular/core';
import { QuizComponent } from './quiz.component';

@NgModule({
  declarations: [QuizComponent],
  imports: [CommonModule]
})
export class QuizModule {}

// Each feature module loaded separately
```

```typescript
// 3. OnPush Change Detection

@Component({
  selector: 'app-quiz',
  templateUrl: './quiz.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuizComponent {
  // Only detect changes when @Input changes or event fires
  // Reduces unnecessary change detection cycles
}
```

```typescript
// 4. Dependency Injection & Tree-Shaking

// quiz.service.ts
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'  // Tree-shakeable
})
export class QuizService {
  // Only included if imported
}

// Instead of:
// { provide: QuizService, useClass: QuizService }
// In module declarations (always included)
```

```typescript
// 5. RxJS Optimization

import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

@Component({...})
export class SearchComponent {
  private searchTerm$ = new Subject<string>();
  
  constructor(private api: ApiService) {
    // Only call API after user stops typing for 300ms
    // Prevents 100 API calls while typing
    this.searchTerm$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap(term => this.api.search(term))
      )
      .subscribe(results => this.results = results);
  }
  
  onSearch(term: string) {
    this.searchTerm$.next(term);
  }
}
```

```typescript
// 6. Unsubscribe & Memory Leaks

export class QuizComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  constructor(private quizService: QuizService) {}
  
  ngOnInit() {
    // Use takeUntil to auto-unsubscribe on destroy
    this.quizService.getQuiz()
      .pipe(
        takeUntil(this.destroy$)
      )
      .subscribe(quiz => this.quiz = quiz);
  }
  
  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
```

```typescript
// 7. Image Optimization

// Use WebP with fallback
<picture>
  <source srcset="quiz.webp" type="image/webp" />
  <img src="quiz.jpg" alt="Quiz" loading="lazy" />
</picture>

// Or use responsive images
<img 
  srcset="quiz-mobile.jpg 480w, 
          quiz-tablet.jpg 768w, 
          quiz-desktop.jpg 1200w"
  sizes="(max-width: 480px) 100vw, 
         (max-width: 768px) 80vw, 
         100vw"
  src="quiz-desktop.jpg"
  loading="lazy"
/>
```

```typescript
// 8. Virtual Scrolling for Large Lists

import { ScrollingModule } from '@angular/cdk/scrolling';

@Component({
  selector: 'app-word-list',
  template: `
    <cdk-virtual-scroll-viewport itemSize="50" class="example-viewport">
      <div *cdkVirtualFor="let word of words">
        {{ word.name }}
      </div>
    </cdk-virtual-scroll-viewport>
  `
})
export class WordListComponent {
  // Only renders visible items
  // 10,000 words but only 20 DOM nodes
  words = Array(10000).fill(null).map((_, i) => ({name: `Word ${i}`}));
}
```

```bash
# Build optimization

ng build --configuration production \
  --optimization \
  --build-optimizer \
  --sourceMap=false \
  --namedChunks=false

# Result:
# Before: 250KB (gzipped)
# After: 120KB (52% reduction)
```

### **Responsive Design:**

```scss
// SCSS with mixins for breakpoints

$breakpoints: (
  'mobile': 320px,
  'tablet': 768px,
  'desktop': 1024px
);

@mixin respond-to($breakpoint) {
  @media (min-width: map-get($breakpoints, $breakpoint)) {
    @content;
  }
}

// Mobile-first approach
.quiz-container {
  display: grid;
  grid-template-columns: 1fr;  // 1 column on mobile
  padding: 1rem;
  
  @include respond-to('tablet') {
    grid-template-columns: 1fr 1fr;  // 2 columns on tablet
    padding: 2rem;
  }
  
  @include respond-to('desktop') {
    grid-template-columns: 1fr 1fr 1fr;  // 3 columns on desktop
    padding: 4rem;
  }
}

.quiz-card {
  font-size: 14px;
  
  @include respond-to('tablet') {
    font-size: 16px;
  }
  
  @include respond-to('desktop') {
    font-size: 18px;
  }
}
```

```typescript
// Media query detection in TypeScript

import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';

@Component({...})
export class ResponsiveComponent {
  isMobile$ = this.breakpointObserver.observe(Breakpoints.Handset)
    .pipe(
      map(result => result.matches)
    );
  
  constructor(private breakpointObserver: BreakpointObserver) {}
}
```

```html
<!-- Conditional rendering based on screen size -->

<!-- Show on mobile only -->
<button *ngIf="isMobile$ | async" (click)="showMobileMenu()">
  Menu
</button>

<!-- Show on desktop only -->
<nav *ngIf="!(isMobile$ | async)">
  <ul>
    <li><a href="/quiz">Quiz</a></li>
    <li><a href="/learn">Learn</a></li>
  </ul>
</nav>
```

### **Handling Mistral AI Latency:**

```
Problem:
User clicks "Generate Quiz"
    ↓
FastAPI calls Mistral API
    ↓
Wait 3-5 seconds (timeout)
    ↓
User sees loading spinner (bad UX)

Solution: Caching + Async Processing
```

```python
# Solution 1: Pre-generate and Cache Quizzes

from celery import shared_task
from redis import asyncio as aioredis
import json

# Background task: Generate quizzes overnight
@shared_task
def generate_all_quizzes():
    """Run nightly to pre-generate all quizzes"""
    
    words = db.get_all_words()
    
    for word in words:
        try:
            # Call Mistral API
            quiz = mistral_api.generate_quiz(word)
            
            # Cache in Redis
            redis.setex(
                f"quiz:{word}",
                7*24*3600,  # 7 day TTL
                json.dumps(quiz)
            )
        except Exception as e:
            logger.error(f"Failed to generate quiz for {word}: {e}")
            # Use fallback quiz template

# When user requests quiz
@app.get("/api/quiz/{word}")
async def get_quiz(word: str):
    redis_client = aioredis.from_url("redis://cache:6379")
    
    # Check Redis cache (instant)
    cached = await redis_client.get(f"quiz:{word}")
    if cached:
        return json.loads(cached)
    
    # If not cached, generate on-demand
    # (Usually won't happen if pre-generation works)
    quiz = await generate_quiz_mistral(word)
    
    # Cache for future
    await redis_client.setex(f"quiz:{word}", 3600, json.dumps(quiz))
    
    return quiz
```

```python
# Solution 2: Queue-based Generation with Streaming

# When user requests quiz, immediately return "queued"
# Then stream quiz once generated

from fastapi.responses import StreamingResponse
from asyncio import Queue

quiz_queue = Queue()

@app.get("/api/quiz/{word}/stream")
async def get_quiz_stream(word: str):
    """Stream quiz as it's generated"""
    
    # Check cache first
    cached = await redis_client.get(f"quiz:{word}")
    if cached:
        return JSONResponse(json.loads(cached))
    
    # Queue for generation
    job_id = str(uuid.uuid4())
    
    # Put in queue for async worker
    await quiz_queue.put({
        'job_id': job_id,
        'word': word
    })
    
    # Stream result as it becomes available
    async def event_generator():
        start_time = time.time()
        
        while True:
            # Check if quiz is ready
            quiz = await redis_client.get(f"quiz:{word}")
            
            if quiz:
                yield f"data: {quiz}\n\n"
                break
            
            # Timeout after 15 seconds
            if time.time() - start_time > 15:
                yield f"data: {json.dumps({'error': 'timeout'})}\n\n"
                break
            
            await asyncio.sleep(0.5)
    
    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream"
    )

# Background worker processes queue
@app.on_event("startup")
async def start_quiz_worker():
    asyncio.create_task(process_quiz_queue())

async def process_quiz_queue():
    """Background worker that generates quizzes"""
    
    while True:
        try:
            job = await quiz_queue.get()
            word = job['word']
            job_id = job['job_id']
            
            # Generate quiz
            quiz = await generate_quiz_mistral(word)
            
            # Cache result
            await redis_client.setex(
                f"quiz:{word}",
                7*24*3600,
                json.dumps(quiz)
            )
            
            logger.info(f"Generated quiz for {word}")
        
        except Exception as e:
            logger.error(f"Worker error: {e}")
            await asyncio.sleep(1)
```

```typescript
// Frontend: Handle streaming response

// quiz.component.ts
getQuizStream(word: string) {
  const eventSource = new EventSource(`/api/quiz/${word}/stream`);
  
  eventSource.onmessage = (event) => {
    const quiz = JSON.parse(event.data);
    
    if (quiz.error) {
      this.error = "Quiz generation timed out";
      return;
    }
    
    this.quiz = quiz;
    eventSource.close();
  };
  
  eventSource.onerror = () => {
    this.error = "Connection error";
    eventSource.close();
  };
}
```

```python
# Solution 3: Use Mistral Async API

import httpx

async def generate_quiz_async(word: str):
    """Non-blocking call to Mistral"""
    
    async with httpx.AsyncClient(timeout=10.0) as client:
        response = await client.post(
            "https://api.mistral.ai/v1/chat/completions",
            json={
                "model": "mistral-small",
                "messages": [
                    {
                        "role": "user",
                        "content": f"Generate a quiz for the word {word}"
                    }
                ]
            },
            headers={
                "Authorization": f"Bearer {MISTRAL_API_KEY}"
            }
        )
        
        return response.json()

# With concurrency control (limit simultaneous API calls)
from asyncio import Semaphore

mistral_semaphore = Semaphore(5)  # Max 5 simultaneous Mistral calls

async def generate_quiz_limited(word: str):
    async with mistral_semaphore:
        return await generate_quiz_async(word)
```

---

## **Remaining Questions (17-32)**

Due to length constraints, I'll provide brief summaries for the remaining questions. Would you like me to expand on any of them?

### **Q17: Adaptive Quiz Algorithm**

The adaptive algorithm uses:
- **Difficulty Scoring**: Questions ranked 1-5 based on user performance
- **Spaced Repetition**: Words the user struggled with appear more frequently
- **Item Response Theory (IRT)**: Statistical model to estimate word mastery
- Metric: Track success rate; if >80%, increase difficulty; if <50%, decrease

### **Q18: Effectiveness Metrics**

- **Learning Outcomes**: Pre/post test scores
- **Engagement**: Daily active users, time spent
- **Retention**: Week-over-week return rate
- **A/B Testing**: Compare adaptive vs. non-adaptive

### **Q19: CI/CD Pipeline**

- Cloud Build triggers on git push
- Builds Docker image → Artifact Registry
- Deploys to Cloud Run with rolling updates
- Databases: Migrations run in separate Cloud Tasks before deploy

### **Q20: Multi-Platform Deployment**

Trade-offs:
- **Render**: Easier for Python, built-in Redis
- **Cloud Run**: Better GCP integration, auto-scaling
- **Vercel**: Best for Angular frontend

Consolidation: Would use Cloud Run for everything; costs more but unified monitoring/logging

### **Q21: Observability**

- **Logs**: Cloud Logging (free tier)
- **Metrics**: Prometheus/Cloud Monitoring
- **Traces**: Cloud Trace (X-Ray equivalent)
- **Alerting**: PagerDuty for critical failures

### **Q22: Secret Management**

Use **Google Secret Manager** with rotation every 30 days (covered in Q5)

### **Q23: Code Quality**

- **Frontend**: ESLint + Prettier
- **Backend**: Pylint + Black
- **Testing**: Jest (frontend), Pytest (backend)
- **Pre-commit hooks**: Run linters before commit

### **Q24: API Versioning**

```python
@app.get("/api/v1/quiz/{word}")
async def get_quiz_v1(word: str):
    # Old API (deprecated)
    
@app.get("/api/v2/quiz/{word}")
async def get_quiz_v2(word: str):
    # New API with better response format
    # Deprecate v1 after 6 months
```

### **Q25: Monolith vs Microservices**

**Current**: Microservices (FastAPI + Cloud Run Broker)

**Trade-offs**:
- ✓ Independent scaling, failure isolation
- ✗ Operational complexity, inter-service testing
- Justified because: Quiz logging and main API have different load patterns

### **Q26: Angular vs React/Vue**

- **Angular**: Full framework (routing, DI, forms)
- **React**: Library + community choices
- **Vue**: Easiest to learn

Choice: Angular for enterprise features + TypeScript

### **Q27: Mistral vs OpenAI vs Vertex AI**

| Criteria | Mistral | OpenAI | Vertex AI |
|----------|---------|--------|-----------|
| Cost | $0.15-0.60 per 1M tokens | $2-30 per 1M tokens | $0.05-0.30 per 1M tokens |
| Latency | 200-500ms | 500-1000ms | 300-600ms |
| Availability | Good | Excellent | Good (GCP customer only) |

**Choice**: Mistral (cost) with OpenAI fallback

### **Q28: Disaster Recovery**

- **RTO** (Recovery Time Objective): 1 hour
- **RPO** (Recovery Point Objective): 15 minutes
- **Backup**: PostgreSQL backups every 6 hours → Cloud Storage
- **Failover**: DNS switch to backup region

### **Q29: Graceful Degradation**

```python
# If Mistral API is down
@app.get("/api/quiz/{word}")
async def get_quiz(word: str):
    try:
        return await generate_quiz_mistral(word)
    except Exception:
        # Fallback: Return template quiz
        return await generate_template_quiz(word)
```

### **Q30: Load Testing**

```bash
# K6 load test
k6 run --vus 1000 --duration 30s load-test.js
# At 10,000 concurrent users, database becomes bottleneck
# Solution: Add read replicas
```

### **Q31: Multi-Region & Localization**

- Deploy FastAPI to multiple regions
- Use DNS failover (Terraform + Cloud DNS)
- Store etymology in multiple languages
- Content management: CMS for translations

### **Q32: Technical Debt**

- **Top candidate for rewrite**: Cloud Run Broker → Pub/Sub (more reliable)
- **Second**: Consolidate platforms (Render + Cloud Run → Cloud Run only)
- **Third**: Add comprehensive integration tests

---

## **Summary Table: All 32 Questions**

| # | Question | Key Answer |
|----|----------|-----------|
| 1 | Architecture & Broker | Separation for scalability, decoupling, cost |
| 2 | FastAPI vs alternatives | Async-first, Mistral integration, type safety |
| 3 | GCS vs PostgreSQL | Cost (85% cheaper), scalability, consistency trade-offs |
| 4 | 10x Growth | DB connections → FastAPI scaling → Mistral tier → GCS prefix |
| 5 | Secret Management | Google Secret Manager + rotation every 30 days |
| 6 | Data Consistency | Pub/Sub + Outbox Pattern for eventual consistency |
| 7 | Cross-service Comms | Timeouts, circuit breakers, regional deployment |
| 8-10 | Data Storage | GCS for history, PostgreSQL for stats, BigQuery for analytics |
| 11-13 | Auth & Security | OAuth 2.0, JWT with refresh tokens, rate limiting |
| 14-16 | Performance | Lazy loading, responsive design, pre-generation caching |
| 17-32 | Production Readiness | Monitoring, disaster recovery, multi-region deployment |

---

