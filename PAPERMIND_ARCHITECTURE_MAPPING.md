# PaperMind - Architecture Mapping (Hybrid Approach)

## Complete Architecture Flow with Tech Stack

| Step | Process | Layer | Technology | Hosting | Cost | Infrastructure & Registration |
|------|---------|-------|------------|---------|------|------------------------------|
| 1 | **Document Ingestion** | Frontend | Angular 18+ (file upload component) | Vercel | FREE | ✅ GitHub repo · ✅ Vercel account · ✅ Link repo to Vercel |
| 2 | **OCR + Text Extraction** | Client-Side Processing | Tesseract.js (in-browser) | Browser | FREE | ❌ No setup (runs locally) |
| 3 | **Encryption** | Client-Side Security | TweetNaCl.js (NaCl crypto library) | Browser | FREE | ❌ No setup (runs locally) |
| 4 | **Store Encrypted File** | Backend + Storage | FastAPI + Supabase PostgreSQL | Railway + Supabase | FREE | ✅ Railway account · ✅ Supabase account · ✅ Create PostgreSQL DB · ✅ Link GitHub to Railway |
| 5 | **Generate Embeddings** | Backend Processing | FastAPI + LangChain + OpenAI Embeddings API | Railway + OpenAI | ~$0.01-0.05/doc | ✅ OpenAI account · ✅ Create API key · ✅ Set environment var: OPENAI_API_KEY |
| 6 | **Store Embeddings in Vector DB** | Vector Storage | Qdrant Cloud (free tier cluster) | Qdrant Cloud | FREE | ✅ Qdrant Cloud account · ✅ Create free cluster · ✅ Set API key: QDRANT_URL, QDRANT_API_KEY |
| 7 | **User Query Interface** | Frontend | Angular (query input + chat UI) | Vercel | FREE | ✅ Already done in Step 1 |
| 8 | **RAG Retrieval** | Backend + Vector DB | FastAPI + Qdrant SDK (similarity search) | Railway + Qdrant | FREE | ✅ Already done in Steps 4 & 6 |
| 9 | **Decrypt Retrieved Docs** | Client-Side Processing | TweetNaCl.js (in-browser decryption) | Browser | FREE | ❌ No setup (runs locally) |
| 10 | **Send to LLM Agent** | Backend + LLM | FastAPI + OpenAI API (via SDK) | Railway + OpenAI | ~$0.0001-0.01/query | ✅ Already done in Step 5 (API key) |
| 11 | **LLM Reasoning** | External AI | GPT-4o Mini (via API) | OpenAI Cloud | ~$0.008/mo | ✅ Already done in Step 5 |
| 12 | **Format Response** | Backend | FastAPI + Pydantic | Railway | FREE | ✅ Already done in Step 4 |
| 13 | **Return Answer to Frontend** | Backend API | REST API (FastAPI) | Railway | FREE | ✅ Already done in Step 4 |
| 14 | **Display Result** | Frontend | Angular (response component) | Vercel | FREE | ✅ Already done in Step 1 |

---

## Infrastructure Setup Checklist

### Platforms to Register & Setup (In Order)

| # | Platform | Purpose | Free Tier | Setup Time | What to Do |
|---|----------|---------|-----------|-----------|-----------|
| 1 | **GitHub** | Version control | ✅ Yes (public repos) | 5 min | Create account, create `PaperMind` repo |
| 2 | **Vercel** | Frontend hosting | ✅ Yes (unlimited) | 10 min | Sign up with GitHub, link PaperMind repo, auto-deploys on push |
| 3 | **Railway** | Backend hosting | ✅ Yes ($5/mo credits) | 10 min | Sign up with GitHub, create new project, link repo |
| 4 | **Supabase** | PostgreSQL database | ✅ Yes (500MB) | 10 min | Sign up, create new project, get `DATABASE_URL` |
| 5 | **Qdrant** | Vector database | ✅ Yes (1GB free tier) | 10 min | Sign up, create free cluster, get `QDRANT_URL` + `QDRANT_API_KEY` |
| 6 | **OpenAI** | LLM + embeddings API | ✅ Yes ($5 trial) | 5 min | Sign up, create API key, get `OPENAI_API_KEY` |

**Total Setup Time**: ~50 minutes

---

### Step-by-Step Registration Guide

#### Step 1: GitHub (5 minutes)
```bash
1. Go to https://github.com/signup
2. Create account
3. Create new repository: "PaperMind"
4. Clone to local: git clone https://github.com/YOUR_USERNAME/PaperMind.git
```

#### Step 2: Vercel (10 minutes)
```bash
1. Go to https://vercel.com/signup
2. Sign up with GitHub (authorize access)
3. Import PaperMind repository
4. Add environment variables:
   - NG_APP_API_BASE_URL=https://papermind-api.railway.app
5. Deploy (automatic on git push)
```
**Result**: Frontend at `https://papermind.vercel.app`

#### Step 3: Railway (10 minutes)
```bash
1. Go to https://railway.app
2. Sign up with GitHub
3. Create new project
4. Connect GitHub repository (PaperMind)
5. Add environment variables:
   - ENVIRONMENT=production
   - DATABASE_URL=<from Supabase>
   - QDRANT_URL=<from Qdrant>
   - QDRANT_API_KEY=<from Qdrant>
   - OPENAI_API_KEY=<from OpenAI>
```
**Result**: Backend at `https://papermind-api.railway.app`

#### Step 4: Supabase (10 minutes)
```bash
1. Go to https://supabase.com
2. Sign up (GitHub or email)
3. Create new project
4. Go to Settings → Database → Connection string
5. Copy connection URL (PostgreSQL)
6. Add to Railway as DATABASE_URL
```
**Result**: PostgreSQL database connected

#### Step 5: Qdrant (10 minutes)
```bash
1. Go to https://qdrant.tech/cloud/
2. Sign up (GitHub or email)
3. Create free cluster
4. Get API key from dashboard
5. Copy: QDRANT_URL and QDRANT_API_KEY
6. Add to Railway environment
```
**Result**: Vector database ready for embeddings

#### Step 6: OpenAI (5 minutes)
```bash
1. Go to https://platform.openai.com/signup
2. Sign up with email/GitHub
3. Go to API keys section
4. Create new secret key
5. Copy key: sk-xxxxxxxxxxxx
6. Add to Railway as OPENAI_API_KEY
```
**Result**: LLM API access configured

---

### Environment Variables to Set in Railway

```bash
# After setup, these should be configured in Railway dashboard:

ENVIRONMENT=production
LOG_LEVEL=info

# From Supabase
DATABASE_URL=postgresql://user:password@aws-0-region.pooler.supabase.com:6543/postgres

# From Qdrant
QDRANT_URL=https://xxxxxxxx-qdrant.aws.cloud.qdrant.io:6333
QDRANT_API_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# From OpenAI
OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Application
CORS_ORIGIN=https://papermind.vercel.app
JWT_SECRET=your-secret-key-change-this
SESSION_SECRET=your-session-secret-change-this
```

---

### Verification Checklist

After all setup, verify everything works:

```bash
✅ Frontend
  - [ ] Vercel deployment successful
  - [ ] App accessible at https://papermind.vercel.app
  - [ ] Environment variables loaded

✅ Backend
  - [ ] Railway deployment successful
  - [ ] API accessible at https://papermind-api.railway.app
  - [ ] Swagger docs at /docs
  - [ ] Health check: curl https://papermind-api.railway.app/health

✅ Database
  - [ ] Supabase connected (check Railway logs)
  - [ ] PostgreSQL working
  - [ ] Tables created successfully

✅ Vector DB
  - [ ] Qdrant cluster created
  - [ ] Connection test passes
  - [ ] API key valid

✅ LLM
  - [ ] OpenAI API key valid
  - [ ] Can make API calls (test with curl)
  - [ ] Billing set up (card added)

✅ Integration
  - [ ] Frontend can call backend API
  - [ ] CORS properly configured
  - [ ] All environment variables accessible
```

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
| Hosting | **Vercel** | Static + serverless functions | FREE |
| Domain | Custom domain (optional) | papermind.com or similar | $0-12/year |

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
| Hosting | **Railway** | Deploy backend service | ~$0.20-0.50/mo (pay-as-you-go) |

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
| Embeddings | OpenAI Ada-3 | Convert text → vectors | ~$0.01-0.05/doc |
| LLM Agent | **OpenAI GPT-4o Mini** ⭐ | Intelligent reasoning (20x cheaper) | ~$0.008/mo |
| RAG Framework | LangChain (Python) | Orchestrate RAG pipeline | FREE |

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

| Service | Free Tier | Monthly Cost | Notes |
|---------|-----------|--------------|-------|
| **OpenAI GPT-4o Mini** ⭐ | Pay-per-token | ~$0.008/mo | 20x cheaper than Claude, 100 queries/mo |
| OpenAI Embeddings | $5 free credits | ~$0.01-0.10 | Included with API calls |
| Qdrant Cloud | 1 free cluster (1GB) | **FREE** | Unlimited queries |
| Supabase PostgreSQL | 500MB free | **FREE** | Plenty for personal docs |
| Vercel (Frontend) | Unlimited | **FREE** | Auto-deploys on git push |
| Railway (Backend) | $5/mo credits | ~$0.20-0.50 | Pay-as-you-go after credits |
| Domain (optional) | - | $0-12/yr | Custom domain for frontend |
| **TOTAL** | | **~$0.03-0.60/mo** | Ultra-affordable with GPT-4o Mini |

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
