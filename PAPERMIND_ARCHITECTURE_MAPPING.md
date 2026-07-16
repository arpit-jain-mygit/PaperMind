# PaperMind - Architecture Mapping (Hybrid Approach)

## Complete Architecture Flow with Tech Stack

| Step | Process | Layer | Technology | Hosting | Cost | Notes |
|------|---------|-------|------------|---------|------|-------|
| 1 | **Document Ingestion** | Frontend | Angular 18+ (file upload component) | Vercel/Netlify | FREE | Drag-and-drop file handler, file validation |
| 2 | **OCR + Text Extraction** | Client-Side Processing | Tesseract.js (in-browser) | Vercel/Netlify | FREE | Runs locally in browser, no server call needed |
| 3 | **Encryption** | Client-Side Security | TweetNaCl.js (NaCl crypto library) | Vercel/Netlify | FREE | Asymmetric encryption before uploading |
| 4 | **Store Encrypted File** | Backend + Storage | FastAPI + Supabase PostgreSQL | Railway (backend) + Supabase (DB) | FREE | Store encrypted blob + metadata (file name, doc type, etc.) |
| 5 | **Generate Embeddings** | Backend Processing | FastAPI + LangChain + OpenAI Embeddings API | Railway | ~$0.01-0.05/doc | Convert encrypted text to vector embeddings |
| 6 | **Store Embeddings in Vector DB** | Vector Storage | Qdrant Cloud (free tier cluster) | Qdrant Cloud (managed) | FREE | 1GB free storage, unlimited queries, semantic search |
| 7 | **User Query Interface** | Frontend | Angular (query input + chat UI) | Vercel/Netlify | FREE | Text input, real-time response display |
| 8 | **RAG Retrieval** | Backend + Vector DB | FastAPI + Qdrant SDK (similarity search) | Railway + Qdrant | FREE | Semantic search to find top-K relevant documents |
| 9 | **Decrypt Retrieved Docs** | Client-Side Processing | TweetNaCl.js (in-browser decryption) | Vercel/Netlify | FREE | Decrypt only retrieved docs client-side |
| 10 | **Send to LLM Agent** | Backend + LLM | FastAPI + Claude API (via Anthropic SDK) | Railway | ~$0.001-0.05/query | Send context + query to Claude 3.5 Sonnet |
| 11 | **LLM Reasoning** | External AI | Claude 3.5 Sonnet (via API) | Anthropic Cloud | ~$0.06/mo avg | Intelligent reasoning across documents |
| 12 | **Format Response** | Backend | FastAPI + Pydantic | Railway | FREE | Parse Claude response, format with type safety |
| 13 | **Return Answer to Frontend** | Backend API | REST API (FastAPI) | Railway | FREE | Stream or batch response to Angular frontend |
| 14 | **Display Result** | Frontend | Angular (response component) | Vercel/Netlify | FREE | Rich text formatting, citations, confidence scores |

---

## Layer-by-Layer Breakdown

### 🖥️ **Frontend Layer**
| Component | Technology | Purpose | Cost |
|-----------|-----------|---------|------|
| UI Framework | **Angular 18+** | SPA with TypeScript, RxJS | FREE |
| File Upload | Angular Material / ng-file-drop | Drag-drop document upload | FREE |
| Query Interface | Angular Material Dialog + Chat UI | Question input + response display | FREE |
| State Management | NgRx / Akita | Manage app state (documents, queries) | FREE |
| HTTP Client | Angular HttpClient | Communicate with backend API | FREE |
| Styling | Tailwind CSS / Angular Material Theme | Responsive UI design | FREE |
| Hosting | **Vercel / Netlify** | Static + serverless functions | FREE |

### 🔄 **Client-Side Processing Layer**
| Component | Technology | Purpose | Cost |
|-----------|-----------|---------|------|
| OCR Engine | **Tesseract.js** | Extract text from PDF/images | FREE |
| Encryption | **TweetNaCl.js / NaCl.js** | Encrypt docs before upload | FREE |
| Decryption | **TweetNaCl.js / NaCl.js** | Decrypt retrieved docs | FREE |
| File Parsing | pdf.js (PDF parsing) | Read PDF files | FREE |
| Image Processing | Canvas API | Resize/compress images | FREE |

### ⚙️ **Backend API Layer**
| Component | Technology | Purpose | Cost |
|-----------|-----------|---------|------|
| Framework | **FastAPI** | RESTful API server with auto-docs | FREE |
| Language | **Python** | Type-safe backend with Pydantic | FREE |
| Process Queue | Celery / RQ | Async job processing | FREE |
| ASGI Server | Uvicorn | Production server | FREE |
| Hosting | **Railway / Render / Fly.io** | Deploy backend service | FREE (~$5-10 credits/mo) |

### 📊 **Data & Storage Layer**
| Component | Technology | Purpose | Cost |
|-----------|-----------|---------|------|
| Database | **Supabase (PostgreSQL)** | Store metadata, user data, keys | FREE (500MB) |
| Vector DB | **Qdrant Cloud** | Store embeddings + metadata | FREE (1GB, unlimited queries) |
| File Storage | S3 Compatible (MinIO / Supabase) | Store encrypted documents | FREE |
| Cache | Redis (optional) | Cache queries, session tokens | FREE (local) or $5-10/mo (cloud) |

### 🤖 **AI/LLM Layer**
| Component | Technology | Purpose | Cost |
|-----------|-----------|---------|------|
| Embeddings | OpenAI Ada-3 / Claude Embeddings | Convert text → vectors | ~$0.01-0.05/doc |
| LLM Agent | **Claude 3.5 Sonnet** | Intelligent reasoning | ~$0.06/mo avg |
| RAG Framework | LangChain (Node.js) | Orchestrate RAG pipeline | FREE |

### 🔐 **Security Layer**
| Component | Technology | Purpose | Cost |
|-----------|-----------|---------|------|
| Encryption | TweetNaCl.js (Asymmetric) | E2E encryption | FREE |
| Key Management | Supabase Auth + JWT | User authentication | FREE |
| CORS / Security Headers | Express middleware | Prevent CSRF, XSS, etc. | FREE |
| Audit Logging | Supabase audit logs | Track document access | FREE |
| SSL/TLS | Let's Encrypt (auto via Vercel) | Encrypted connections | FREE |

---

## Data Flow Mapping

```
┌─────────────────────────────────────────────────────────────┐
│                    DOCUMENT UPLOAD FLOW                     │
└─────────────────────────────────────────────────────────────┘

Angular (Step 1)
   │ File chosen
   ↓
Tesseract.js (Step 2)
   │ OCR: Extract text from image/PDF
   ↓
TweetNaCl.js (Step 3)
   │ Encrypt: text + metadata
   ↓
Angular HttpClient (Step 4)
   │ POST /api/documents/upload
   ↓
FastAPI Backend (Step 4)
   │ Receive encrypted file + metadata
   ↓
Supabase PostgreSQL (Step 4)
   │ Store encrypted blob + doc metadata
   ↓
Celery Task Queue (Step 5)
   │ Process: Generate embeddings (async)
   ↓
OpenAI/Claude API (Step 5)
   │ Create vector embeddings
   ↓
Qdrant Vector DB (Step 6)
   │ Store embeddings + metadata


┌─────────────────────────────────────────────────────────────┐
│                    QUERY/REASONING FLOW                     │
└─────────────────────────────────────────────────────────────┘

Angular (Step 7)
   │ User: "When does my DL expire?"
   ↓
Angular HttpClient (Step 7)
   │ POST /api/query
   ↓
FastAPI Backend (Step 8)
   │ Receive question
   ↓
Qdrant Vector DB (Step 8)
   │ Semantic search: Find relevant docs
   ↓
Supabase PostgreSQL (Step 8)
   │ Retrieve encrypted documents
   ↓
FastAPI Backend (Step 9)
   │ Decrypt using user's key (client-side OR server w/ encrypted key)
   ↓
Claude API (Step 10-11)
   │ Send context + question
   │ → LLM Reasoning: "DL valid until 2027-03-15"
   ↓
FastAPI Backend (Step 12-13)
   │ Format response + citations using Pydantic models
   ↓
Angular (Step 14)
   │ Display: "Your DL expires on 2027-03-15
   │           Issuing Authority: RTO Karnataka
   │           Status: ✅ Valid for 6+ months"
```

---

## Environment Variables Setup

```bash
# Frontend (.env.production - Angular)
NG_APP_API_BASE_URL=https://papermind-api.railway.app
NG_APP_ENVIRONMENT=production

# Backend (.env - FastAPI/Python)
ENVIRONMENT=production
PORT=8000

# Database
DATABASE_URL=postgresql://user:pass@db.supabase.co:5432/papermind
SUPABASE_URL=https://xxxxx.supabase.co
SUPABASE_KEY=xxxxx

# Vector DB (Qdrant)
QDRANT_URL=https://xxxxx-qdrant.aws.io:6333
QDRANT_API_KEY=xxxxx

# LLM
ANTHROPIC_API_KEY=sk-ant-xxxxx  # Claude API
OPENAI_API_KEY=sk-xxxxx  # For embeddings (or use Claude embeddings)

# Encryption Keys
ENCRYPTION_KEY_DERIVATION_SALT=xxxxx  # For deriving user-specific keys

# Security
JWT_SECRET=xxxxx
CORS_ORIGIN=https://papermind.vercel.app
```

---

## Monthly Cost Summary

| Service | Free Tier | Monthly Cost |
|---------|-----------|--------------|
| Claude API | Pay-per-token | ~$0.06-0.50 |
| OpenAI Embeddings (if used) | $5 free credits | ~$0.01-0.10 |
| Qdrant Cloud | 1 free cluster (1GB) | **FREE** |
| Supabase PostgreSQL | 500MB free | **FREE** |
| Vercel (Frontend) | Unlimited | **FREE** |
| Railway (Backend) | $5/mo credits | **FREE** |
| Domain (optional) | - | $0-12/yr |
| **TOTAL** | | **~$0.07-0.60/mo** |

---

## Security Checklist

- [ ] All secrets stored in Railway environment variables (not in code)
- [ ] Encryption keys derived from user password (never stored)
- [ ] Encrypted documents never transmitted in plaintext
- [ ] HTTPS enforced (via Vercel + Railway)
- [ ] CORS restricted to frontend domain
- [ ] JWT tokens with 1-hour expiry
- [ ] Rate limiting on API endpoints (prevent abuse)
- [ ] Audit logs for all document access
- [ ] Regular security headers (HSTS, CSP, X-Frame-Options)
- [ ] Input validation on all API endpoints
- [ ] No PII in logs or error messages

---

## Next Steps

1. **Create GitHub repo** with Angular + Express starter
2. **Set up deployments** (Vercel + Railway with auto-deploy)
3. **Implement encryption** layer (TweetNaCl.js)
4. **Configure Qdrant** free cluster
5. **Set up Supabase** database
6. **Build core flows** (Upload → Query → Answer)
7. **Add document type handlers** (DL, Passport, PAN, ITR, etc.)

Ready to start? Should I create a detailed implementation guide?
