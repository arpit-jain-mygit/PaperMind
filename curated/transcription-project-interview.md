# VoxAlchemy.ai (Doc-Transcribe) - Project Interview Guide

## Table of Contents

1. [Architecture Overview](#architecture-overview)
   - [High-Level System Design](#high-level-system-design)
   - [Component Breakdown](#component-breakdown)
     - [Frontend (doc-transcribe-ui)](#frontend-doc-transcribe-ui)
     - [API Layer (doc-transcribe-api)](#api-layer-doc-transcribe-api)
     - [Worker Layer (doc-transcribe-worker)](#worker-layer-doc-transcribe-worker)
     - [Data Layer](#data-layer)

2. [Gemini Integration](#gemini-integration)
   - [Overview](#overview)
   - [OCR (Vision) Integration](#ocr-vision-integration)
     - [How It Works](#how-it-works)
     - [Key Features](#key-features)
     - [Chunking Strategy (OCR)](#chunking-strategy-ocr)
     - [Prompt Types](#prompt-types)
   - [Audio Transcription (ASR) Integration](#audio-transcription-asr-integration)
     - [How It Works](#how-it-works-1)
     - [Chunking for Audio](#chunking-for-audio)
     - [Quality Scoring](#quality-scoring)
   - [Error Handling & Retry Strategy](#error-handling--retry-strategy)
     - [Gemini-Specific Errors](#gemini-specific-errors)
     - [Rate Limit Handling (429)](#rate-limit-handling-429)
     - [Preflight Probe](#preflight-probe)

3. [Redis Cache Usage](#redis-cache-usage)
   - [Data Structures](#data-structures)
     - [Job Status Hash](#job-status-hash)
     - [User Jobs List](#user-jobs-list)
     - [Daily Job Counter (Quota)](#daily-job-counter-quota)
     - [Idempotency Cache](#idempotency-cache)
   - [How Redis is Used in Each Layer](#how-redis-is-used-in-each-layer)
     - [API Layer](#api-layer-readwrite-status-and-quotas)
     - [Worker Layer](#worker-layer-update-status-during-processing)
     - [Frontend](#frontend-polling-for-status)
   - [Redis Reliability Features](#redis-reliability-features)
     - [Connection Pooling & Retry](#connection-pooling--retry)
     - [Safe HSET with Guarding](#safe-hset-with-guarding)

4. [Rate Limiting System Design](#rate-limiting-system-design)
   - [Multi-Layered Rate Limiting Strategy](#multi-layered-rate-limiting-strategy)
   - [Layer 1: Upload Quota (User-Level Rate Limiting)](#layer-1-upload-quota-user-level-rate-limiting)
     - [Configuration](#configuration)
     - [Implementation](#implementation)
     - [Rate Limit Algorithm](#rate-limit-algorithm)
   - [Layer 2: Cost Guardrail (Pre-Processing Rate Limiting)](#layer-2-cost-guardrail-pre-processing-rate-limiting)
     - [Purpose](#purpose)
     - [Cost Estimation](#cost-estimation)
     - [Policy Decision](#policy-decision)
     - [Response Example](#response-example)
     - [Rate Limit Algorithm](#rate-limit-algorithm-1)
   - [Layer 3: Gemini API Rate Limiting (Provider Level)](#layer-3-gemini-api-rate-limiting-provider-level)
     - [Problem](#problem)
     - [Solution: Backoff & Retry with Cooldown](#solution-backoff--retry-with-cooldown)
     - [Configuration](#configuration-1)
     - [Implementation](#implementation-1)
     - [Rate Limit Algorithm](#rate-limit-algorithm-2)
     - [Preflight Probe (Optimization)](#preflight-probe-optimization)

5. [Chunking Strategies](#chunking-strategies)
   - [Why Chunking?](#why-chunking)
   - [Strategy 1: PDF → Pages (OCR)](#strategy-1-pdf--pages-ocr)
     - [Overview](#overview-1)
     - [Configuration](#configuration-2)
     - [Chunking Logic](#chunking-logic)
     - [Batching Optimization](#batching-optimization)
   - [Strategy 2: Audio → Chunks (Transcription ASR)](#strategy-2-audio--chunks-transcription-asr)
     - [Overview](#overview-2)
     - [Configuration](#configuration-3)
     - [Chunking Logic](#chunking-logic-1)
     - [Why 5 Minutes?](#why-5-minutes)
     - [Quality Tracking](#quality-tracking)

6. [GenAI/LLM Features & LLMOps Stack](#genaillm-features--llmops-stack)
   - [Current LLM Features Deployed](#current-llm-features-deployed)
     - [Vision-based OCR](#1-vision-based-ocr)
     - [Audio-based Transcription (ASR)](#2-audio-based-transcription-asr)
     - [Domain-Specific Prompting](#3-domain-specific-prompting)
     - [Quality Scoring & Validation](#4-quality-scoring--validation)
   - [Architectural Features (LLMOps-Ready)](#architectural-features-llmops-ready)
     - [Prompt Management](#prompt-management)
     - [Model Version Management](#model-version-management)
     - [Cost Tracking & Budgeting](#cost-tracking--budgeting)
     - [Error Classification & Telemetry](#error-classification--telemetry)
   - [Recommended LLMOps Stack Additions](#recommended-llmops-stack-additions)
     - [Experiment Tracking & Evaluation](#1-experiment-tracking--evaluation)
     - [Prompt Versioning](#2-prompt-versioning)
     - [Cost Optimization](#3-cost-optimization)
     - [Quality Monitoring Dashboard](#4-quality-monitoring-dashboard)
     - [Observability & Tracing](#5-observability--tracing)
     - [Model Evaluation Framework](#6-model-evaluation-framework)
     - [Automated Retraining/Tuning](#7-automated-retrainingtuning)

7. [Interview Questions for Head of Engineering](#interview-questions-for-head-of-engineering)
   - [Section A: System Design & Architecture](#section-a-system-design--architecture)
     - [Q1: Architecture Overview](#q1-architecture-overview)
     - [Q2: Handling Failures](#q2-how-does-the-system-handle-failures-in-the-processing-pipeline)
     - [Q3: Rate Limiting Strategy](#q3-rate-limiting-strategy)
     - [Q4: Redis Bottleneck](#q4-how-do-you-ensure-redis-doesnt-become-a-bottleneck)
   - [Section B: LLM Integration & Optimization](#section-b-llm-integration--optimization)
     - [Q5: Why Gemini](#q5-why-gemini-instead-of-dedicated-ocrAsr-services)
     - [Q6: Gemini Rate Limiting](#q6-how-do-you-handle-the-gemini-429-rate-limiting)
     - [Q7: Quality Scoring](#q7-quality-scoring---how-does-it-work-and-how-would-you-improve-it)
   - [Section C: Operational & Business Questions](#section-c-operational--business-questions)
     - [Q8: Cost Optimization](#q8-cost-optimization)
     - [Q9: Scaling Challenges](#q9-scaling-challenges)
     - [Q10: Security & Privacy](#q10-security--privacy)
   - [Section D: Team & Process Questions](#section-d-team--process-questions)
     - [Q11: Testing & Quality](#q11-testing--quality)
     - [Q12: Monitoring & Observability](#q12-monitoring--observability)
     - [Q13: Migration & Upgrades](#q13-migration--upgrades)

8. [Interview Preparation Checklist](#interview-preparation-checklist)
   - [Before the Interview](#before-the-interview)
   - [Key Numbers to Know](#key-numbers-to-know)
   - [Common Interview Pitfalls to Avoid](#common-interview-pitfalls-to-avoid)

9. [Glossary of Technical Terms](#glossary-of-technical-terms)

10. [Resources for Further Learning](#resources-for-further-learning)
    - [Articles & Documentation](#articles--documentation)
    - [Tools & Libraries](#tools--libraries)

11. [Conclusion](#conclusion)

---

## 1. Architecture Overview

### High-Level System Design

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              USER BROWSER                               │
│                   (Static React/Vanilla JS Frontend)                     │
│                    - Upload (Drag & Drop)                               │
│                    - Real-time Polling                                   │
│                    - Job History & Downloads                             │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                    HTTPS (CORS Enabled)
                               │
                    ┌──────────▼─────────────┐
                    │   FastAPI REST API     │
                    │  (doc-transcribe-api)  │
                    └──────────┬─────────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
        │                      │                      │
   ┌────▼─────┐         ┌─────▼─────┐        ┌──────▼──────┐
   │ Routes   │         │  Services │        │ Repositories│
   ├──────────┤         ├───────────┤        ├─────────────┤
   │ upload   │         │ queue     │        │ redis_client│
   │ status   │         │ auth      │        │ gcs         │
   │ jobs     │         │ quota     │        │ status_mach │
   │ health   │         │ cost_guard│        │             │
   │ auth     │         │ upload_orch         │             │
   │ intake   │         │ feature_flags       │             │
   └──────────┘         └───────────┘        └─────────────┘
        │                      │                      │
        └──────────────────────┼──────────────────────┘
                               │
           ┌───────────────────┼───────────────────┐
           │                   │                   │
      ┌────▼────┐         ┌────▼────┐        ┌────▼────┐
      │  Redis  │         │   GCS   │        │  Queue  │
      │ (Cache) │         │(Storage)│        │(Job IDs)│
      └────┬────┘         └────┬────┘        └────┬────┘
           │                   │                   │
           └───────────────────┼───────────────────┘
                               │
           ┌───────────────────┼───────────────────┐
           │                   │                   │
      ┌────▼─────────────────────────────────────▼────┐
      │                                                │
      │    Redis Queue (Job Queue Messages)           │
      │  - doc_jobs (single mode)                     │
      │  - doc_jobs_ocr / doc_jobs_transcription      │
      │    (partitioned mode)                         │
      │                                                │
      └────┬─────────────────────────────────────┬────┘
           │                                     │
      ┌────▼──────────────────────────────────▼────┐
      │      Worker Loop (Dispatcher)              │
      │     (doc-transcribe-worker)                │
      │   - Polls queue continuously               │
      │   - Routes to OCR/Transcription executors  │
      │   - Handles retries & recovery             │
      └────┬──────────────────────────────────┬────┘
           │                                  │
       ┌───▼──────┐                   ┌──────▼──┐
       │   OCR    │                   │Transcrib│
       │Executor  │                   │ Executor│
       │          │                   │         │
       ├──────────┤                   ├─────────┤
       │ PDF→Imgs │                   │MP3→Chunks
       │ Imgs→Txt │                   │Chunks→Txt
       │(Gemini)  │                   │(Gemini) │
       └───┬──────┘                   └──────┬──┘
           │                                 │
           └────────────┬────────────────────┘
                        │
              ┌─────────▼──────────┐
              │   Gemini LLM       │
              │  (Vertex AI)       │
              │                    │
              │ - Vision (OCR)     │
              │ - Audio (ASR)      │
              └─────────┬──────────┘
                        │
              ┌─────────▼──────────┐
              │   GCS (Output)     │
              │                    │
              │ - Transcripts      │
              │ - Quality Metadata │
              │ - Error Logs       │
              └────────────────────┘
```

### Component Breakdown

#### **Frontend (doc-transcribe-ui)**
- **Type:** Static HTML/CSS/Vanilla JS (No Build Step)
- **Key Files:**
  - `upload.js` - Handles file drag-drop and upload orchestration
  - `polling.js` - Real-time job status polling
  - `auth.js` - Google OAuth authentication
  - `api-client.js` - HTTP client for API calls
  - `ui.js` - Shared UI state and components
  
#### **API Layer (doc-transcribe-api)**
- **Type:** FastAPI (Python)
- **Key Responsibilities:**
  - User authentication (Google OAuth)
  - File upload orchestration
  - Job queuing
  - Cost estimation (before processing)
  - Quota enforcement (daily limits, active job limits)
  - Job status polling
  - Request validation & error handling
  
#### **Worker Layer (doc-transcribe-worker)**
- **Type:** Long-running Python process
- **Key Responsibilities:**
  - Queue polling
  - Job orchestration (routing to OCR/Transcription)
  - Calling Gemini LLM
  - Result aggregation
  - Recovery policy on failures
  
#### **Data Layer**
- **Redis:** Job status cache, quotas, user session data
- **GCS:** File storage (uploads, outputs, logs)
- **Gemini API:** LLM inference (Vision for OCR, Audio for ASR)

---

## 2. Gemini Integration

### Overview

VoxAlchemy uses **Google Gemini 2.5-Flash** via **Vertex AI** for both OCR and audio transcription. This is a strategic choice because:
- **Lower cost** than dedicated OCR/ASR services
- **Unified interface** for both vision and audio
- **Flexible prompting** for domain-specific content (Jain literature, general documents, lectures)

### OCR (Vision) Integration

#### How It Works

```python
# From: doc-transcribe-worker/worker/ocr.py
MODEL_NAME = "gemini-2.5-flash"

def ocr_page_batch(batch_pages: list[int], job: dict) -> str:
    """
    Takes 1-N images (pages) and sends to Gemini with prompt.
    Returns JSON with extracted text per page.
    """
    images = [convert_pdf_page_to_image(p) for p in batch_pages]
    
    response = model.generate_content(
        [
            Part.from_text(prompt_text),
            *[Part.from_data(img_bytes, mime_type="image/png") for img in images]
        ],
        generation_config={
            "temperature": 0,
            "max_output_tokens": 8192
        }
    )
    return parse_json_response(response.text)
```

#### Key Features

| Feature | Value | Purpose |
|---------|-------|---------|
| **Model** | Gemini 2.5-Flash | Cost-effective, fast |
| **Temperature** | 0 | Deterministic output (no randomness) |
| **Max Tokens** | 8192 | Sufficient for multi-page batches |
| **Batching** | 1-N pages per request | Reduces API calls, improves latency |
| **Prompting** | Domain-aware prompts | Verbatim transcription for Jain texts |
| **Image Format** | PNG @ 300 DPI | High quality input |

#### Chunking Strategy (OCR)
- **Batch Size:** Configurable via `GEMINI_PAGES_PER_REQUEST` (default: 1)
- **Rationale:** 
  - Single page: Safest, highest success rate
  - Multiple pages: Reduces API latency but increases token usage
  - Batches are tuned based on content type and latency requirements

#### Prompt Types
```
1. DEFAULT_AUDIO_PROMPT (Pravachan - Lectures)
   - Captures speaker pauses, emotional nuances
   - Preserves natural language variation

2. PRAVACHAN_PROMPT (Jain Lectures)
   - Domain-specific vocabulary
   - Maintains original scripts (Devanagari, etc.)

3. SHANKA_SAMADHAN_PROMPT (Q&A Sessions)
   - Structured dialogue format
   - Preserves questioner/answerer separation

4. OCR (PDF → Text)
   - Verbatim transcription only
   - Strict line-break preservation
   - No translation or summarization
```

### Audio Transcription (ASR) Integration

#### How It Works

```python
# From: doc-transcribe-worker/worker/transcribe.py
MODEL_NAME = "gemini-2.5-flash"
CHUNK_DURATION_SEC = 5 * 60  # 5-minute chunks

def transcribe_chunk(mp3_path: str, prompt_text: str) -> str:
    """
    Takes audio chunk and sends with context prompt.
    """
    with open(mp3_path, "rb") as f:
        audio_bytes = f.read()
    
    response = model.generate_content(
        [
            Part.from_text(prompt_text),
            Part.from_data(audio_bytes, mime_type="audio/mpeg")
        ],
        generation_config={
            "temperature": 0,
            "max_output_tokens": 8192
        }
    )
    return response.text.strip()
```

#### Chunking for Audio
- **Duration:** 5 minutes per chunk (configurable)
- **Reason:** 
  - Gemini has input size limits (~20MB audio equivalent)
  - Smaller chunks = faster processing
  - Easier recovery on failures
- **Assembly:** Chunks concatenated with "\n\n" separator

#### Quality Scoring
After transcription, each segment is scored:
```python
segment_score = score_segment(text)  # Quality metric (0-1)
transcript_quality_score = summarize_segments(segment_rows)  # Overall

# Returned in job result:
{
    "transcript_quality_score": 0.92,
    "low_confidence_segments": [2, 5],  # Segment indices
    "segment_quality": [
        {"segment_index": 1, "score": 0.98, "hint": "clear_speech"},
        ...
    ]
}
```

### Error Handling & Retry Strategy

#### Gemini-Specific Errors

| Error | Status | Handling | Retry |
|-------|--------|----------|-------|
| **429 (Rate Limited)** | Temporary | Exponential backoff cooldown | Yes (up to 30x per page) |
| **Resource Exhausted** | Temporary | Same as 429 | Yes |
| **Invalid Input** | Permanent | Fail page/chunk, log error | No |
| **Timeout** | Temporary | Retry with exponential backoff | Yes (3-5 times) |
| **Empty Response** | Permanent | Fallback to blank or error | No |

#### Rate Limit Handling (429)

```python
GEMINI_429_COOLDOWN_SEC = 60  # Wait 60s before retry
GEMINI_429_COOLDOWN_LOG_INTERVAL_SEC = 10  # Log every 10s
GEMINI_429_MAX_COOLDOWNS_PER_PAGE = 30  # Max 30 cooldowns = 30 minutes

def handle_gemini_rate_limit(page_num: int, cooldown_no: int):
    """Backoff: 60s wait, then retry. After 30 retries, fail."""
    for attempt in range(cooldown_no):
        sleep(60)  # Wait before retry
        if attempt % 6 == 0:  # Log every 60s
            logger.info(f"Still rate-limited, attempt {attempt}")
    
    if cooldown_no >= GEMINI_429_MAX_COOLDOWNS_PER_PAGE:
        raise PageRateLimitExceeded(page_num)
```

#### Preflight Probe
```python
def gemini_preflight_probe(batch_pages: list[int]) -> bool:
    """
    Send tiny text-only probe before heavy batch call.
    Detects immediate rate limiting.
    """
    response = model.generate_content([Part.from_text("PING")])
    # If 429 → wait, don't proceed with batch
    # If OK → proceed
```

---

## 3. Redis Cache Usage

### Data Structures

Redis is used as a **real-time cache and coordination layer** for job processing. Here's the breakdown:

#### Job Status Hash
```
Key: job_status:{job_id}
Type: Hash (HSET)
TTL: Permanent (or 30 days in some configs)

Fields:
{
    "status": "QUEUED|PROCESSING|COMPLETED|FAILED|CANCELLED",
    "stage": "Uploading|Preparing audio|Transcribing chunk 2/10|Completed",
    "progress": 15,  # Percentage (0-100)
    "contract_version": "1",
    "updated_at": "2026-06-23T10:30:45.123456",
    "output_path": "gs://bucket/jobs/job123/transcript.txt",
    "output_filename": "transcript.txt",
    "error_code": "GEMINI_429_EXHAUSTED",
    "error_message": "Rate limit exhausted",
    "error_detail": "Full stack trace...",
    "transcript_quality_score": "0.92",
    "low_confidence_segments": "[2,5]",  # JSON array
    "segment_quality": "[{...}, {...}]",  # JSON array
}
```

#### User Jobs List
```
Key: user_jobs:{email}
Type: List (LPUSH)
TTL: Permanent

Value: [job_id_1, job_id_2, ...]  # Newest first
Use: Get user's job history with LRANGE
```

#### Daily Job Counter (Quota)
```
Key: user_daily_jobs:{email}:{YYYYMMDD}
Type: String (INCR)
TTL: 172,800 seconds (48 hours)

Value: Number of jobs submitted today
Use: Enforce DAILY_JOB_LIMIT_PER_USER
```

#### Idempotency Cache
```
Key: idempotency:{idempotency_key}
Type: String
TTL: 900 seconds (15 minutes)

Value: Cached response body (JSON)
Use: Prevent duplicate uploads if client retries
```

### How Redis is Used in Each Layer

#### **API Layer** (read/write status and quotas)
```python
# From: doc-transcribe-api/services/redis_client.py
redis_client = redis.from_url(REDIS_URL, decode_responses=True)

# Check quota
counter_key = f"user_daily_jobs:{email}:{day_key}"
used = int(r.get(counter_key) or "0")
if used >= DAILY_JOB_LIMIT_PER_USER:
    raise HTTPException(429)

# Increment counter
r.incr(counter_key)
r.expire(counter_key, 172800)

# Store job status
r.hset(f"job_status:{job_id}", mapping={
    "status": "QUEUED",
    "progress": 0,
    "updated_at": datetime.utcnow().isoformat()
})
```

#### **Worker Layer** (update status during processing)
```python
# From: doc-transcribe-worker/worker/transcribe.py
def update(job_id: str, *, stage: str, progress: int):
    safe_hset(f"job_status:{job_id}", {
        "stage": stage,
        "progress": progress,
        "updated_at": datetime.utcnow().isoformat()
    })

# Called during transcription:
update(job_id, stage="Preparing audio", progress=5)
update(job_id, stage="Transcribing chunk 2/10", progress=50)
update(job_id, stage="Completed", progress=100)
```

#### **Frontend** (polling for status)
```javascript
// From: doc-transcribe-ui/js/polling.js
async function pollJobStatus(jobId) {
    const response = await fetch(`/api/jobs/${jobId}/status`);
    // Backend reads from Redis and returns status
    return response.json();
}

// Polls every 2-5 seconds until job completes
```

### Redis Reliability Features

#### Connection Pooling & Retry
```python
redis_client = redis.Redis.from_url(
    REDIS_URL,
    decode_responses=True,
    socket_keepalive=True,  # Keep connection alive
    socket_connect_timeout=2,
    socket_timeout=15,
    retry_on_timeout=True,
    health_check_interval=15,  # Ping every 15s
)
```

#### Safe HSET with Guarding
```python
def safe_hset(key: str, mapping: dict, retries: int = 1):
    """
    Ensures status transitions are valid.
    Prevents race conditions in concurrent updates.
    """
    def _write_once():
        r = get_redis()
        ok, current_status, _ = guarded_hset(
            r,
            key=key,
            mapping=mapping,
            context="TRANSCRIBE_SAFE_HSET"
        )
        if not ok:
            logger.warning(
                f"Blocked invalid transition: {current_status} → {mapping.get('status')}"
            )
    
    # Retry up to N times if connection fails
    run_with_retry(
        operation="redis_hset",
        fn=_write_once,
        retryable=(redis.exceptions.ConnectionError, redis.exceptions.TimeoutError),
        policy=REDIS_POLICY  # Exponential backoff
    )
```

---

## 4. Rate Limiting System Design

### Multi-Layered Rate Limiting Strategy

VoxAlchemy implements **three independent rate limiting layers** to protect users and manage costs:

```
┌─────────────────────────────────────────────────────────┐
│  Layer 1: UPLOAD QUOTA (Per-User Limits)                │
│  - Daily job limit (e.g., 100 jobs/day)                 │
│  - Active job limit (e.g., 5 concurrent jobs)           │
│  Enforced at: API upload endpoint                        │
│  Using: Redis counters with daily TTL                   │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│  Layer 2: COST GUARDRAIL (Pre-Processing)               │
│  - Estimated cost before upload                         │
│  - WARN threshold (e.g., $0.75)                        │
│  - BLOCK threshold (e.g., $2.50)                       │
│  Enforced at: API intake check (pre-queue)              │
│  Using: Cost estimation based on file size/pages/duration
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│  Layer 3: GEMINI RATE LIMITING (Provider Level)         │
│  - Gemini API rate limits (429 responses)               │
│  - Exponential backoff + cooldown                       │
│  - Per-page/chunk handling                              │
│  Enforced at: Worker (during transcription/OCR)         │
│  Using: Backoff-and-retry with max exhaustion          │
└─────────────────────────────────────────────────────────┘
```

### Layer 1: Upload Quota (User-Level Rate Limiting)

#### Configuration
```python
# From: doc-transcribe-api/config.py
DAILY_JOB_LIMIT_PER_USER = int(os.getenv("DAILY_JOB_LIMIT_PER_USER", "0"))
# 0 = disabled, N = max N jobs per day per user

ACTIVE_JOB_LIMIT_PER_USER = int(os.getenv("ACTIVE_JOB_LIMIT_PER_USER", "0"))
# 0 = disabled, N = max N concurrent jobs per user
```

#### Implementation
```python
def enforce_upload_quotas(*, r, email: str, request_id: str, job_type: str):
    # Check 1: Daily Limit
    day_key = datetime.utcnow().strftime("%Y%m%d")
    counter_key = f"user_daily_jobs:{email}:{day_key}"
    used = int(r.get(counter_key) or "0")
    
    if used >= DAILY_JOB_LIMIT_PER_USER:
        raise HTTPException(
            status_code=429,
            detail={
                "error_code": "USER_DAILY_QUOTA_EXCEEDED",
                "error_message": f"Daily limit reached ({DAILY_JOB_LIMIT_PER_USER})"
            }
        )
    
    # Check 2: Active Jobs Limit
    job_ids = r.lrange(f"user_jobs:{email}", 0, 199)
    active_count = sum(
        1 for jid in job_ids
        if r.hget(f"job_status:{jid}", "status") not in {"COMPLETED", "FAILED", "CANCELLED"}
    )
    
    if active_count >= ACTIVE_JOB_LIMIT_PER_USER:
        raise HTTPException(
            status_code=429,
            detail={
                "error_code": "USER_ACTIVE_QUOTA_EXCEEDED",
                "error_message": f"Active limit reached ({ACTIVE_JOB_LIMIT_PER_USER})"
            }
        )
    
    # Increment daily counter (expires after 48 hours)
    r.incr(counter_key)
    r.expire(counter_key, 172800)
```

#### Rate Limit Algorithm
- **Type:** **Token Bucket** (with daily reset)
- **Bucket Size:** DAILY_JOB_LIMIT_PER_USER tokens
- **Refill Rate:** 1 bucket per day (midnight UTC)
- **Token Cost:** 1 token per job

**Why Token Bucket?**
- Simple to implement in Redis
- Fair for users throughout the day
- No burst penalty (unlike sliding window)

### Layer 2: Cost Guardrail (Pre-Processing Rate Limiting)

#### Purpose
Prevent users from accidentally submitting expensive jobs without awareness. Works like a **warning system** rather than hard limit.

#### Cost Estimation
```python
def estimate_projected_cost_usd(
    job_type: str,
    file_size_bytes: int,
    media_duration_sec: float,
    pdf_page_count: int
) -> float:
    """Estimate total cost before processing."""
    
    if job_type == "TRANSCRIPTION":
        minutes = media_duration_sec / 60.0
        return (minutes * TRANSCRIPTION_COST_PER_MIN_USD) + \
               (file_size_bytes / 1024 / 1024 * TRANSCRIPTION_COST_PER_MB_USD)
    
    # OCR
    pages = max(1, pdf_page_count)
    return (pages * OCR_COST_PER_PAGE_USD) + \
           (file_size_bytes / 1024 / 1024 * OCR_COST_PER_MB_USD)
```

#### Policy Decision

```python
COST_GUARDRAIL_WARN_USD = 0.75
COST_GUARDRAIL_BLOCK_USD = 2.50

def decide_policy(projected_cost_usd: float) -> tuple[PolicyDecision, str]:
    if projected_cost_usd >= COST_GUARDRAIL_BLOCK_USD:
        return ("BLOCK", "Cost exceeds block threshold")
    
    if projected_cost_usd >= COST_GUARDRAIL_WARN_USD:
        return ("WARN", "High cost - consider splitting input")
    
    return ("ALLOW", "Cost within safe threshold")
```

#### Response Example
```json
{
    "error_code": "COST_GUARDRAIL_BLOCK",
    "estimated_cost_usd": 3.50,
    "estimated_effort": "HIGH",
    "estimated_cost_band": "VERY_HIGH",
    "policy_decision": "BLOCK",
    "policy_reason": "Projected cost exceeds configured block threshold",
    "recommendation": "Split PDF into smaller batches or compress audio"
}
```

#### Rate Limit Algorithm
- **Type:** **Threshold-Based Limit** (hard budget + soft warning)
- **Soft Threshold:** $0.75 (WARN)
- **Hard Threshold:** $2.50 (BLOCK)

**Why Threshold?**
- Not time-based (fair for all)
- Prevents runaway costs for expensive operations
- User awareness without strict quotas

### Layer 3: Gemini API Rate Limiting (Provider Level)

#### Problem
Gemini API has rate limits:
- **Requests/minute:** Limited per project
- **Tokens/minute:** Limited per model
- When exceeded: **429 Too Many Requests** response

#### Solution: Backoff & Retry with Cooldown

#### Configuration
```python
GEMINI_429_COOLDOWN_SEC = 60  # Wait 60 seconds
GEMINI_429_MAX_COOLDOWNS_PER_PAGE = 30  # Max 30 retries = 30 minutes max wait
```

#### Implementation
```python
def _is_gemini_rate_limited(exc: BaseException) -> bool:
    msg = str(exc).lower()
    return "resource exhausted" in msg or "429" in msg

def _wait_for_429_cooldown(page_num: int, cooldown_no: int, wait_sec: int):
    """Wait and log periodically."""
    start = time.time()
    log_interval = GEMINI_429_COOLDOWN_LOG_INTERVAL_SEC
    next_log_time = start + log_interval
    
    while time.time() - start < wait_sec:
        if time.time() >= next_log_time:
            elapsed = time.time() - start
            logger.info(
                f"Still rate-limited page={page_num} cooldown={cooldown_no} "
                f"elapsed={elapsed}s max={wait_sec}s"
            )
            next_log_time += log_interval
        time.sleep(min(1, wait_sec - (time.time() - start)))

def transcribe_chunk_with_retry(mp3_path, idx, total, prompt):
    cooldown_count = 0
    while cooldown_count < GEMINI_429_MAX_COOLDOWNS_PER_PAGE:
        try:
            return gemini_call(mp3_path, prompt)
        except Exception as exc:
            if _is_gemini_rate_limited(exc):
                cooldown_count += 1
                _wait_for_429_cooldown(idx, cooldown_count, GEMINI_429_COOLDOWN_SEC)
            else:
                raise
    
    raise PageRateLimitExceeded(idx)
```

#### Rate Limit Algorithm
- **Type:** **Exponential Backoff with Fixed Cooldown**
- **Backoff:** 60 seconds fixed (not exponential)
- **Max Retries:** 30 per page/chunk
- **Max Wait Time:** 30 × 60s = 30 minutes per page

**Why This Strategy?**
- Gemini's rate limits reset after short wait → fixed delay works
- 30-minute max prevents infinite loops
- Per-page granularity allows partial job success

#### Preflight Probe (Optimization)
```python
def gemini_preflight_probe(batch_pages: list[int]) -> bool:
    """
    Before expensive batch call, send tiny probe to detect 429.
    Saves latency if rate-limited.
    """
    try:
        model.generate_content([Part.from_text("PING")])
        return True  # Proceed with batch
    except Exception as exc:
        if _is_gemini_rate_limited(exc):
            logger.warning("Rate-limited, delaying batch")
            return False  # Skip this batch, wait, retry later
        return True  # Non-429 error, proceed anyway
```

---

## 5. Chunking Strategies

### Why Chunking?

Chunking is a **critical design pattern** that enables:
1. **API compliance** - Stay within input size limits
2. **Failure isolation** - Single chunk failure ≠ entire job failure
3. **Progress visibility** - User sees incremental updates
4. **Recovery** - Retry failed chunks independently
5. **Cost control** - Avoid $2.50+ single requests

### Strategy 1: PDF → Pages (OCR)

#### Overview
```
Input PDF (500 pages)
    ↓
Page 1: {image_bytes, prompt} → Gemini → "Extracted text..."
Page 2: {image_bytes, prompt} → Gemini → "More text..."
Page 3-500: [Similar]
    ↓
Concatenate all pages
    ↓
Output: Full transcript
```

#### Configuration
```python
OCR_DPI = 300  # Resolution for page rendering
OCR_PAGE_BATCH_SIZE = 1  # Pages per API call
GEMINI_PAGES_PER_REQUEST = 1  # Can increase to batch
```

#### Chunking Logic
```python
def process_pdf_ocr(job_id: str, pdf_path: str, total_pages: int):
    for page_num in range(1, total_pages + 1):
        update(job_id, stage=f"OCR page {page_num}/{total_pages}", 
               progress=10 + int((page_num / total_pages) * 80))
        
        try:
            # Convert page to image
            img = convert_pdf_page_to_image(pdf_path, page_num, dpi=OCR_DPI)
            
            # Send to Gemini
            text = gemini_ocr_page(img, resolve_ocr_prompt(job))
            
            # Store result
            page_results[page_num] = {
                "text": text,
                "quality_score": score_page(text),
                "status": "SUCCESS"
            }
        except PageRateLimitExceeded as e:
            page_results[page_num] = {"status": "RATE_LIMITED", "error": str(e)}
        except Exception as e:
            page_results[page_num] = {"status": "FAILED", "error": str(e)}
    
    final_text = "\n".join([page_results[p]["text"] for p in range(1, total_pages + 1)])
    return final_text
```

#### Batching Optimization
```python
# Instead of 1 page per request, can send multiple pages:
GEMINI_PAGES_PER_REQUEST = 3

pages_batch = [page_num, page_num+1, page_num+2]
images = [convert_page_to_image(p) for p in pages_batch]

response = model.generate_content([
    Part.from_text(prompt),
    *[Part.from_data(img, mime_type="image/png") for img in images]
])

# Response is JSON: {"pages": {1: "text...", 2: "text...", 3: "text..."}}
```

**Tradeoff:**
- **Batch=1:** Safe, slow, but 100% reliable
- **Batch=N:** Fast, but more likely to hit token limits or timeout

### Strategy 2: Audio → Chunks (Transcription ASR)

#### Overview
```
Input Audio (60 minutes, 500MB)
    ↓
Split into 5-minute chunks (12 chunks total)
    ↓
Chunk 1 (5 min): {audio_bytes, prompt} → Gemini → "Transcript chunk 1..."
Chunk 2 (5 min): {audio_bytes, prompt} → Gemini → "Transcript chunk 2..."
...
Chunk 12 (5 min): {audio_bytes, prompt} → Gemini → "Transcript chunk 12..."
    ↓
Concatenate all chunks
    ↓
Output: Full transcript with quality scores
```

#### Configuration
```python
CHUNK_DURATION_SEC = 5 * 60  # 5-minute chunks (default)
# Can be configured: TRANSCRIBE_CHUNK_DURATION_SEC
```

#### Chunking Logic
```python
def split_audio(mp3_path: str, chunk_duration_sec: int = 300) -> list[str]:
    """Split MP3 into 5-minute chunks."""
    audio = AudioSegment.from_file(mp3_path)
    duration_sec = len(audio) / 1000.0
    
    chunks = []
    chunk_ms = chunk_duration_sec * 1000
    
    for i in range(0, len(audio), chunk_ms):
        chunk_audio = audio[i:i+chunk_ms]
        chunk_path = f"{mp3_path}_chunk_{len(chunks)+1}.mp3"
        chunk_audio.export(chunk_path, format="mp3")
        chunks.append(chunk_path)
    
    return chunks  # List of MP3 files

def run_transcription(job_id: str, job: dict):
    mp3_path = download_from_gcs(job["input_gcs_uri"])
    chunks = split_audio(mp3_path)
    total = len(chunks)
    prompt_text = resolve_audio_prompt(job)
    
    texts = []
    segment_rows = []
    segment_start_sec = 0.0
    
    for idx, chunk in enumerate(chunks, start=1):
        update(job_id, stage=f"Transcribing chunk {idx}/{total}",
               progress=10 + int((idx / total) * 80))
        
        # Transcribe this chunk
        text = transcribe_chunk(chunk, idx, total, prompt_text)
        texts.append(text)
        
        # Score this segment
        chunk_duration_sec = get_audio_duration(chunk)
        score, metrics, hints = score_segment(text)
        segment_rows.append({
            "segment_index": idx,
            "start_sec": round(segment_start_sec, 2),
            "end_sec": round(segment_start_sec + chunk_duration_sec, 2),
            "score": round(score, 4),
            "hint": hints[0] if hints else "",
            "metrics": metrics
        })
        segment_start_sec += chunk_duration_sec
    
    # Finalize
    final_text = "\n\n".join(texts)
    overall_quality = summarize_segments(segment_rows)
    
    return {
        "text": final_text,
        "quality_score": overall_quality,
        "segments": segment_rows
    }
```

#### Why 5 Minutes?
| Chunk Duration | Pros | Cons |
|---|---|---|
| 1 minute | Very safe, granular recovery | 60 chunks for 1 hour, slow, expensive |
| 5 minutes | **Balance of safety & speed** | Might hit token limits with dense audio |
| 10 minutes | Fewer chunks, faster | More likely to fail, larger recovery window |
| 30 minutes | Minimal API calls | Single failure = re-transcribe 30 min |

#### Quality Tracking
```python
# Each segment gets a quality score
segment_quality = [
    {
        "segment_index": 1,
        "start_sec": 0.0,
        "end_sec": 300.0,  # 5 minutes
        "score": 0.98,
        "hint": "clear_speech",
        "metrics": {
            "word_confidence": 0.95,
            "speaker_clarity": 0.98,
            "background_noise": 0.02
        }
    },
    ...
]

# If any segment scores < 0.70, flag as low confidence
low_confidence_segments = [idx for idx, seg in enumerate(segments) if seg["score"] < 0.70]

# Return to user
{
    "transcript_quality_score": 0.92,  # Average of all segments
    "low_confidence_segments": [2, 5],  # User can review/re-record these
    "segment_quality": segment_quality  # Detailed per-segment breakdown
}
```

---

## 6. GenAI/LLM Features & LLMOps Stack

### Current LLM Features Deployed

#### 1. **Vision-based OCR**
- Model: Gemini 2.5-Flash
- Input: PDF pages rendered as images
- Output: Verbatim text extraction
- Status: Production

#### 2. **Audio-based Transcription (ASR)**
- Model: Gemini 2.5-Flash
- Input: MP3 audio chunks
- Output: Transcribed text + quality scores
- Status: Production

#### 3. **Domain-Specific Prompting**
- Pravachan (Jain lectures) - Preserves speaker nuances
- Shanka-Samadhan (Q&A) - Structured dialogue format
- General documents - Standard OCR rules
- Status: Production

#### 4. **Quality Scoring & Validation**
- Per-page OCR quality (from content analysis)
- Per-segment audio quality (from transcription metrics)
- Overall document quality (aggregate of all segments)
- Status: Production

### Architectural Features (LLMOps-Ready)

#### **Prompt Management**
```python
# Single prompt file with multiple named variants
PROMPT_FILE = "prompts/ocr_and_transcription.txt"

def resolve_audio_prompt(job: dict) -> str:
    subtype = job.get("content_subtype", "pravachan")
    if subtype == "pravachan":
        return PRAVACHAN_PROMPT
    return DEFAULT_AUDIO_PROMPT
```

**Why This Design?**
- Centralized prompt storage (single source of truth)
- Easy A/B testing of prompts
- Version control friendly (text file)
- No code changes for prompt iterations

#### **Model Version Management**
```python
MODEL_NAME = "gemini-2.5-flash"
# Easy to update to future versions like "gemini-3.0"
# Tracks cost differently per model
```

#### **Cost Tracking & Budgeting**
```python
OCR_COST_PER_PAGE_USD = 0.02
OCR_COST_PER_MB_USD = 0.003
TRANSCRIPTION_COST_PER_MIN_USD = 0.015
TRANSCRIPTION_COST_PER_MB_USD = 0.001

# Before job runs: Estimate cost
# After job runs: Track actual cost vs estimate (for future ML optimization)
```

#### **Error Classification & Telemetry**
```python
# Every error is classified with structured codes
error_code = classify_error(exception)
# Examples: GEMINI_429_EXHAUSTED, INVALID_INPUT, GCS_UPLOAD_FAILED

# Helps identify patterns for optimization:
# - Too many 429s → need backoff adjustment
# - Too many timeouts → chunk size too large
# - Too many empty outputs → prompt tuning needed
```

### Recommended LLMOps Stack Additions

#### **1. Experiment Tracking & Evaluation**
```
Tool: MLflow / Weights & Biases
Goal: Track different prompt versions and their quality scores
Example:
  - Version A (current): PRAVACHAN_PROMPT_v1 → avg quality = 0.92
  - Version B (test): PRAVACHAN_PROMPT_v2 (with extra hints) → avg quality = 0.94
  → Deploy Version B
```

#### **2. Prompt Versioning**
```
Current: Single prompt file, no versions
Better: 
  - Version control prompts in Git
  - Tag releases (v1.0, v1.1, etc.)
  - Store in database with timestamps
  - Easy rollback if quality degrades
```

#### **3. Cost Optimization**
```
Current: Fixed costs per operation
Better:
  - Track actual Gemini usage (tokens in/out)
  - Identify expensive operations (e.g., OCR_PAGES_PER_REQUEST=5 costs 3x more)
  - Auto-tune batch sizes based on cost/quality tradeoff
```

#### **4. Quality Monitoring Dashboard**
```
Metrics to track:
  - Average OCR quality score by document type
  - Average transcription quality score by audio type
  - 99th percentile quality (SLO monitoring)
  - Rate limit frequency and recovery time
  - Cost per operation (actual vs projected)
```

#### **5. Observability & Tracing**
```
Current: JSON logs with request_id
Better:
  - Distributed tracing (Jaeger/DataDog)
  - Trace Gemini API calls end-to-end
  - Identify slow chunks or bottlenecks
  - Correlate user experience with backend latency
```

#### **6. Model Evaluation Framework**
```
New features to evaluate:
  - Switch from Gemini 2.5-Flash to Gemini 3.0 (when released)
  - Try multi-modal models for mixed PDFs (text + images)
  - Test new prompting techniques (chain-of-thought for complex docs)
  - Compare cost vs quality across models
```

#### **7. Automated Retraining/Tuning**
```
For future agentic AI features:
  - Collect user feedback ("This transcript is wrong")
  - Fine-tune prompts based on failure patterns
  - Auto-deploy improved prompts when quality threshold met
```

---

## 7. Interview Questions for Head of Engineering

### Section A: System Design & Architecture

**Q1: Architecture Overview**
> Walk us through the overall architecture of VoxAlchemy. Why did you choose a three-tier design (API, Worker, Frontend) instead of a monolithic approach?

**Talking Points:**
- Separation of concerns: stateless API vs stateful long-running worker
- Scalability: API can handle many concurrent uploads, worker handles expensive inference
- Resilience: Failed job doesn't block new uploads; retry logic isolated in worker
- Cost optimization: Can scale API and worker independently based on load

**Q2: How does the system handle failures in the processing pipeline?**
> A user uploads a 100-page PDF. The OCR fails on page 47. How does the system recover without re-processing the first 46 pages?

**Talking Points:**
- Chunk-based processing: Each page/chunk is independent
- Page-level retries: Page 47 retried up to N times before moving to DLQ
- Dead Letter Queue: Failed jobs stored separately for manual inspection
- Recovery Policy: Transient errors (429, timeout) are retried; permanent errors logged
- Idempotency: User can resubmit same job safely (same job_id)

**Q3: Rate Limiting Strategy**
> You've implemented three layers of rate limiting (user quotas, cost guardrails, Gemini API backoff). Why three? Why not one global rate limiter?

**Talking Points:**
- **Layer 1 (User Quotas):** Protects system from user abuse, ensures fair resource allocation
- **Layer 2 (Cost Guardrails):** Protects users from surprise costs, shows transparency
- **Layer 3 (Gemini Backoff):** Adapts to external provider's limits, no control over it
- **Combined Effect:** Prevents both user-side and provider-side issues
- **Trade-off:** More complexity, but fairer and more cost-predictable

**Q4: How do you ensure Redis doesn't become a bottleneck?**
> Job status updates are written to Redis 5+ times per job. With thousands of concurrent jobs, isn't Redis a single point of failure?

**Talking Points:**
- **Connection Pooling:** Multiple connections reduce contention
- **Retry Logic:** Transient failures retried exponentially
- **Health Checks:** Regular pings detect stale connections
- **TTL Strategy:** Old job data auto-expires, preventing unbounded growth
- **Failover (future):** Redis Sentinel or Cluster for HA
- **Alternative (future):** Could use PostgreSQL for durable state, Redis for caching only

### Section B: LLM Integration & Optimization

**Q5: Why Gemini instead of dedicated OCR/ASR services?**
> You chose Gemini 2.5-Flash for both OCR and transcription. What about Tesseract (OCR) or Deepgram (ASR)? Trade-offs?

**Talking Points:**

| Aspect | Gemini | Dedicated Services |
|--------|--------|-------------------|
| **Cost** | Lower per-operation | Higher, but predictable |
| **Quality** | Comparable or better | Specialized (better for single task) |
| **Latency** | 2-5 seconds per page | 1-2 seconds |
| **Infrastructure** | Cloud-managed (Vertex AI) | Need self-host or additional API |
| **Flexibility** | Prompting allows domain tuning | Limited configuration |
| **Language Support** | 100+ languages | Language-dependent |

**Decision Reasoning:**
- Cost was critical for early-stage startup
- Prompting flexibility matches Jain literature focus
- Single provider reduces operational complexity
- Can easily switch models (Gemini 2.5 → 3.0)

**Q6: How do you handle the Gemini 429 rate limiting?**
> Describe your strategy for handling rate limits. What's the maximum time a single page can wait for? How do you prevent infinite loops?

**Talking Points:**
- **Detection:** Check for "resource exhausted" in exception
- **Backoff:** Fixed 60-second cooldown (Gemini limits reset quickly)
- **Max Retries:** 30 per page × 60s = 30-minute maximum
- **Logging:** Log every 10 seconds to avoid log spam
- **Preflight Probe:** Tiny text-only call before batch to detect 429 early
- **Fallback:** If exhausted, mark page as failed and move on (user can resubmit)

**Q7: Quality Scoring - How does it work, and how would you improve it?**
> You return a "transcript_quality_score" (0-1). How is this calculated? What are the limitations?

**Talking Points:**

**Current Implementation:**
```
score = (segment_quality_sum) / segment_count
segment_quality = LLM-based analysis + confidence metrics
```

**Limitations:**
- Gemini doesn't always provide detailed quality metrics
- No ground truth (user's expected output)
- Doesn't account for domain-specific accuracy (e.g., Sanskrit terms)

**Improvements:**
- Collect user feedback (thumbs up/down)
- A/B test with users to validate scoring accuracy
- Fine-tune prompts based on low-scoring segments
- Use secondary model to validate quality (cross-check)
- Track quality over time (SLO: maintain 0.95+)

### Section C: Operational & Business Questions

**Q8: Cost Optimization**
> A user's 500-page OCR request would cost ~$10 to process. Your cost guardrail blocks it at $2.50. How do you balance user experience and cost control?

**Talking Points:**
- **Transparency:** Show estimated cost upfront
- **Alternatives:** Suggest splitting into smaller batches
- **Feedback:** Let power users increase their threshold (with approval)
- **Efficiency:** Optimize chunk size/batching to reduce costs
- **Long-term:** As volume increases, negotiate better Gemini pricing

**Q9: Scaling Challenges**
> You're currently processing 100 jobs/day. What happens at 10,000 jobs/day? What's your scaling strategy?

**Talking Points:**

**Bottlenecks at scale:**
1. **API Rate Limiting:** More concurrent uploads
   - Solution: Load balancer, multiple API instances
   
2. **Redis Contention:** More status updates
   - Solution: Redis Cluster, or shard by job_id
   
3. **Gemini Rate Limits:** 429s more frequent
   - Solution: Increase chunk size, add backoff jitter, negotiate quota with Google
   
4. **GCS Bottleneck:** More uploads/downloads
   - Solution: Regional buckets, CDN caching
   
5. **Worker Concurrency:** Can only process N jobs at once
   - Solution: Scale worker horizontally (multiple instances)

**Q10: Security & Privacy**
> Users upload sensitive documents (family records, financial). How do you ensure privacy and compliance?

**Talking Points:**
- **Data Isolation:** Each user's data in separate GCS paths
- **Access Control:** IAM roles restrict access to service account only
- **Encryption:** GCS encryption at rest, HTTPS in transit
- **PII Redaction:** Consider detecting and redacting sensitive data
- **Audit Logging:** Track who accessed what and when
- **Data Retention:** Delete old uploads after N days (GDPR-compliant)
- **Compliance:** Support GDPR/CCPA deletion requests

### Section D: Team & Process Questions

**Q11: Testing & Quality**
> How do you test OCR quality if the expected output is subjective (e.g., preserving Devanagari script correctly)?

**Talking Points:**
- **Unit Tests:** Mock Gemini responses, test parsing and chunk logic
- **Integration Tests:** Real Gemini calls (slow, but necessary)
- **Golden Dataset:** Known PDFs with expected outputs
- **Human Review:** Periodically review random samples of outputs
- **Metrics:** Track quality_score correlation with user satisfaction
- **Regression Tests:** When improving prompts, ensure old outputs still work

**Q12: Monitoring & Observability**
> What metrics do you track in production? How do you know if the system is healthy?

**Talking Points:**

**Key Metrics:**
- **Throughput:** Jobs completed per hour
- **Latency:** P50/P95/P99 job completion time
- **Quality:** Average transcript_quality_score
- **Errors:** Error rate by type (429, timeout, invalid_input)
- **Cost:** Actual cost per job vs projected
- **Availability:** Uptime (API + Worker)

**Alerting:**
- Error rate > 5% → page
- Avg quality score < 0.85 → investigate prompt change
- P99 latency > 30 min → check Gemini quotas or worker backlog
- 429 frequency > 10% → need backoff adjustment

**Q13: Migration & Upgrades**
> Gemini releases a new model (3.0) that's 20% cheaper. How do you safely switch production traffic to it?

**Talking Points:**
- **Feature Flag:** Add GEMINI_MODEL config, default to 2.5-Flash
- **Canary:** Route 10% of jobs to 3.0 for 1 week
- **Monitor:** Compare quality scores, cost, latency between versions
- **Gradual Rollout:** 25% → 50% → 100% if metrics look good
- **Rollback:** Easy switch back to 2.5-Flash if 3.0 degrades quality
- **Cost Tracking:** Measure actual savings vs projected

---

## Interview Preparation Checklist

### Before the Interview
- [ ] Deploy a local version and test end-to-end (upload → process → download)
- [ ] Run a 100-page PDF through OCR, observe chunk processing
- [ ] Test 429 rate limiting manually (see how backoff works)
- [ ] Review Git commit history to understand evolution
- [ ] Read error logs to understand failure patterns
- [ ] Check Redis to see live job status keys

### Key Numbers to Know
- **Gemini Model:** 2.5-Flash
- **OCR Cost:** $0.02/page + $0.003/MB
- **Transcription Cost:** $0.015/min + $0.001/MB
- **Cost Guardrail Warn:** $0.75 | Block: $2.50
- **Rate Limit Cooldown:** 60 seconds × max 30 retries = 30-min max wait
- **Chunk Size:** 1 page (OCR) | 5 minutes (audio)
- **Redis Health Check:** Every 15 seconds

### Common Interview Pitfalls to Avoid
- ❌ "We use Redis, it's super fast" (No context on why it's needed)
- ✅ "We use Redis to cache job status and quotas because API/Worker are decoupled and need to coordinate state"

- ❌ "Gemini handles everything" (No discussion of limitations)
- ✅ "Gemini is cost-effective and flexible, but has rate limits we handle with exponential backoff"

- ❌ "Rate limiting is just user quotas" (Missing the full picture)
- ✅ "We have three layers: user quotas, cost guardrails, and provider backoff"

- ❌ "We chunk at 5 minutes because it's reasonable" (No justification)
- ✅ "We chunk at 5 minutes to stay within Gemini's input size limits (~20MB audio), reduce latency, and isolate failures"

---

## Glossary of Technical Terms

| Term | Definition | Example |
|------|-----------|---------|
| **Chunking** | Splitting input into smaller pieces for processing | 60-min audio → 12 × 5-min chunks |
| **429 Error** | HTTP status: Too Many Requests (rate limited) | Gemini API: 429 → wait 60s → retry |
| **Token Bucket** | Algorithm: Bucket refills at rate, consume 1 token per request | Daily quota: 100 jobs/day = 100 tokens |
| **Cooldown** | Waiting period before retry | 60s cooldown for Gemini 429 |
| **Idempotency** | Same request twice = same result | Resubmit same job → returns cached result |
| **DLQ** | Dead Letter Queue (jobs that failed after retries) | Failed page 47 → doc_jobs_dead queue |
| **OCR** | Optical Character Recognition (convert image to text) | PDF page image → Gemini Vision → text |
| **ASR** | Automatic Speech Recognition (convert audio to text) | MP3 audio → Gemini Audio → transcript |
| **Quality Score** | Metric (0-1) indicating result confidence | Segment 1: score=0.98 (high quality) |
| **Prompting** | Instructions to LLM for specific tasks | "Transcribe verbatim, preserve scripts" |

---

## Resources for Further Learning

### Articles & Documentation
- [Vertex AI Generative Models](https://cloud.google.com/vertex-ai/docs/generative-ai/start/quickstarts)
- [Redis for Job Queuing](https://redis.io/docs/reference/client-side-caching/)
- [Rate Limiting Algorithms](https://en.wikipedia.org/wiki/Rate_limiting)
- [Exponential Backoff & Jitter](https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/)

### Tools & Libraries
- FastAPI: [https://fastapi.tiangolo.com/](https://fastapi.tiangolo.com/)
- Google Vertex AI SDK: [https://googleapis.dev/python/vertexai/](https://googleapis.dev/python/vertexai/)
- Redis Python Client: [https://redis-py.readthedocs.io/](https://redis-py.readthedocs.io/)
- PDF2Image: [https://github.com/Belval/pdf2image](https://github.com/Belval/pdf2image)

---

## Conclusion

VoxAlchemy demonstrates strong software engineering fundamentals:
- **Clean Architecture:** Separated concerns (API/Worker/Frontend)
- **Reliability:** Comprehensive error handling, retry strategies, dead letter queues
- **Scalability:** Horizontal scaling, stateless API, load balancing ready
- **Cost Control:** Multi-layer rate limiting, transparent cost estimation
- **Observability:** Structured logging, quality metrics, error classification
- **User-Centric Design:** Domain-specific prompting, quality feedback, progress visibility

The project is an excellent case study for:
- Integrating external LLM APIs (Gemini)
- Building resilient async job processing systems
- Implementing fair rate limiting
- Optimizing costs while maintaining quality

Good luck with your interview! 🚀
