# PaperMind - Deployment Guide

This document contains step-by-step instructions to deploy PaperMind to production from scratch.

## Table of Contents
1. [Platform Registration & Setup](#platform-registration--setup)
2. [Pre-Deployment Checklist](#pre-deployment-checklist)
3. [Infrastructure Setup](#infrastructure-setup)
4. [Environment Configuration](#environment-configuration)
5. [Application Deployment](#application-deployment)
6. [Post-Deployment Verification](#post-deployment-verification)
7. [Rollback Procedure](#rollback-procedure)
8. [Monitoring & Maintenance](#monitoring--maintenance)

---

## Platform Registration & Setup

### Complete Setup Time: ~50 minutes

Register and setup these platforms in this order:

---

### 1. GitHub - Version Control (5 minutes)

**Purpose**: Store code repository  
**Cost**: FREE  
**What you need**: Email address

**Steps:**

1. Go to https://github.com/signup
2. Enter username, email, password
3. Click "Create account"
4. Verify email
5. Create new repository:
   - Go to https://github.com/new
   - Repository name: `PaperMind`
   - Description: "Personal Document AI System"
   - Set to Public (for easy sharing)
   - Click "Create repository"

6. Clone locally:
```bash
git clone https://github.com/YOUR_USERNAME/PaperMind.git
cd PaperMind
```

**Result**: GitHub repository created and linked locally

---

### 2. Vercel - Frontend Hosting (10 minutes)

**Purpose**: Deploy Angular frontend  
**Cost**: FREE (unlimited)  
**Prerequisites**: GitHub account

**Steps:**

1. Go to https://vercel.com/signup
2. Click "Continue with GitHub"
3. Authorize Vercel to access GitHub
4. Create Vercel account
5. Select "Import Project"
6. Click "Import Git Repository"
7. Paste: `https://github.com/YOUR_USERNAME/PaperMind`
8. Select "Other" framework
9. Configure project:
   - Build command: `cd frontend && ng build`
   - Output directory: `frontend/dist/papermind`
   - Install command: `npm install`

10. Add environment variables:
```
NG_APP_API_BASE_URL = https://papermind-api.railway.app
NG_APP_ENVIRONMENT = production
```

11. Click "Deploy"
12. Wait for deployment to complete

**Result**: 
- Frontend deployed at: `https://papermind.vercel.app`
- Auto-deploys on every git push

---

### 3. Railway - Backend Hosting (10 minutes)

**Purpose**: Deploy FastAPI backend  
**Cost**: FREE ($5/mo credits, then ~$0.20-0.50/mo)  
**Prerequisites**: GitHub account

**Steps:**

1. Go to https://railway.app/
2. Click "Start Project"
3. Click "Deploy from GitHub repo"
4. Click "Connect GitHub account"
5. Authorize Railway
6. Select your GitHub repository: `PaperMind`
7. Railway will auto-detect Python project
8. Add environment variables (do this in next step)
9. Click "Deploy"
10. Wait for initial deployment

**Result**:
- Backend deployed at: `https://papermind-api.railway.app`
- Auto-deploys on git push (if configured)

---

### 4. Supabase - PostgreSQL Database (10 minutes)

**Purpose**: Store application data  
**Cost**: FREE (500MB storage)  
**Prerequisites**: Email or GitHub account

**Steps:**

1. Go to https://supabase.com/
2. Click "Start your project"
3. Sign up with GitHub or email
4. Click "Create a new project"
5. Project configuration:
   - Organization: Create new (e.g., "PaperMind")
   - Project name: `papermind-db`
   - Database password: Create strong password (save it!)
   - Region: Choose closest to you
   - Pricing plan: Free

6. Wait for database to provision (~2 minutes)
7. Get connection string:
   - Go to Settings → Database
   - Copy "Connection pooling" URI (Postgres)
   - Format: `postgresql://user:password@host:6543/postgres?sslmode=require`

8. Save this for Railway setup (DATABASE_URL)

**Result**: PostgreSQL database ready

---

### 5. Qdrant - Vector Database (10 minutes)

**Purpose**: Store document embeddings  
**Cost**: FREE (1GB storage, unlimited queries)  
**Prerequisites**: Email or GitHub account

**Steps:**

1. Go to https://qdrant.tech/cloud/
2. Click "Start for free"
3. Sign up with GitHub or email
4. Create cluster:
   - Cluster name: `papermind-cluster`
   - Region: Choose closest to you
   - Choose "Free tier" plan

5. Wait for cluster to be created (~2 minutes)
6. Get credentials:
   - Click on cluster
   - Copy API URL (in format: `https://xxxxx-qdrant.aws.cloud.qdrant.io:6333`)
   - Copy API Key (under "API Keys")

7. Save for Railway setup:
   - QDRANT_URL = API URL
   - QDRANT_API_KEY = API Key

**Result**: Vector database cluster ready

---

### 6. OpenAI - LLM & Embeddings API (5 minutes)

**Purpose**: Access GPT-4o Mini model and embeddings  
**Cost**: FREE ($5 trial credits, then ~$0.008/mo)  
**Prerequisites**: Email, phone number for verification

**Steps:**

1. Go to https://platform.openai.com/signup
2. Sign up with email or GitHub
3. Verify phone number (required)
4. Go to API keys: https://platform.openai.com/api/keys
5. Click "Create new secret key"
6. Name it: `papermind-production`
7. Copy key immediately (can only see once): `sk-xxxxxxxxxxxxxxxx`
8. Store in secure location

9. Setup billing (required for API):
   - Go to Billing → Overview
   - Add payment method
   - Set usage limit to $5/month (to prevent overspending)

10. Save for Railway setup: `OPENAI_API_KEY = sk-xxxx...`

**Result**: LLM API access configured

---

### Setup Summary Table

| # | Platform | Account Created | API Key/URL Saved | Added to Railway | ✓ |
|---|----------|-----------------|-------------------|------------------|---|
| 1 | GitHub | [ ] | N/A | N/A | [ ] |
| 2 | Vercel | [ ] | N/A | Auto-linked | [ ] |
| 3 | Railway | [ ] | N/A | N/A | [ ] |
| 4 | Supabase | [ ] | DATABASE_URL | [ ] | [ ] |
| 5 | Qdrant | [ ] | QDRANT_URL, QDRANT_API_KEY | [ ] | [ ] |
| 6 | OpenAI | [ ] | OPENAI_API_KEY | [ ] | [ ] |

---

### Add All Secrets to Railway

After creating all accounts, add environment variables to Railway:

```bash
railway variables set DATABASE_URL="postgresql://user:pass@host:6543/postgres"
railway variables set QDRANT_URL="https://xxxxx-qdrant.aws.cloud.qdrant.io:6333"
railway variables set QDRANT_API_KEY="xxxxxxxxxxxxxxxx"
railway variables set OPENAI_API_KEY="sk-xxxxxxxxxxxxxxxx"
railway variables set ENVIRONMENT="production"
railway variables set LOG_LEVEL="info"
railway variables set CORS_ORIGIN="https://papermind.vercel.app"
railway variables set JWT_SECRET="your-secret-key-here"
railway variables set SESSION_SECRET="your-session-secret-here"
```

Or via Railway dashboard:
1. Go to Railway dashboard
2. Select PaperMind project
3. Click "Variables"
4. Add each variable above
5. Click "Save"

---

## Pre-Deployment Checklist

- [ ] All tests passing locally
- [ ] Code review completed and approved
- [ ] Secrets/credentials configured in deployment platform
- [ ] Database migrations tested
- [ ] Performance testing completed for critical paths
- [ ] Security review completed
- [ ] Documentation updated
- [ ] Git tag created for this release (format: `v1.0.0`)

---

## Infrastructure Setup

### Deployment Platform: Railway + Vercel

**Frontend:**
- **Platform**: Vercel
- **Region**: Auto (closest to user)
- **Environment**: Production
- **Cost**: FREE

**Backend:**
- **Platform**: Railway
- **Region**: US or EU (closest latency)
- **Environment**: Production
- **Cost**: ~$0.20-0.50/month (pay-as-you-go)

### Prerequisites
List all required services/dependencies:
- [ ] Database (type: _____, version: _____)
- [ ] Cache layer (Redis/Memcached)
- [ ] Storage (S3/Cloud Storage)
- [ ] Message Queue (if applicable)
- [ ] CDN (if applicable)
- [ ] Load Balancer
- [ ] SSL Certificate

### Cloud Resource Creation
**Date Created**: ___________________

Document all created resources:

```
Platform: [PLATFORM_NAME]
Region: [REGION]
Account: [ACCOUNT_ID]

Resources:
1. Compute Instance
   - Type: ___________
   - Size: ___________
   - OS: ___________
   - IP: ___________

2. Database
   - Type: ___________
   - Version: ___________
   - Host: ___________
   - Port: ___________
   - Backup enabled: [ ]

3. Storage
   - Type: ___________
   - Bucket/Path: ___________
   - Access controls: ___________

4. Networking
   - VPC: ___________
   - Security Groups: ___________
   - Firewall Rules: ___________
```

---

## Environment Configuration

### System Requirements

**Frontend (Angular):**
- Node 18+
- npm 9+

**Backend (FastAPI):**
- Python 3.11+
- pip package manager

### Environment Variables Setup

**Vercel (Frontend):**
- Create `.env.local` in frontend:
```bash
NG_APP_API_BASE_URL=https://papermind-api.railway.app
NG_APP_ENVIRONMENT=production
```

**Railway (Backend):**
Set via Railway dashboard or CLI:
```bash
# Application
ENVIRONMENT=production
LOG_LEVEL=info

# Database (Auto-generated by Railway)
DATABASE_URL=postgresql://user:pass@localhost:5432/papermind

# Vector DB (Qdrant)
QDRANT_URL=https://xxxxx-qdrant.aws.io:6333
QDRANT_API_KEY=xxxxx

# LLM (Claude)
ANTHROPIC_API_KEY=sk-ant-xxxxx

# Authentication
JWT_SECRET=your-secret-key-here
SESSION_SECRET=your-session-key-here

# CORS
CORS_ORIGIN=https://papermind.vercel.app

# Monitoring
LANGSMITH_API_KEY=xxxxx
LANGSMITH_PROJECT=papermind-prod
```

**Setting Secrets in Railway:**
```bash
railway variables set ANTHROPIC_API_KEY=sk-ant-xxxxx
railway variables set QDRANT_API_KEY=xxxxx
railway variables set JWT_SECRET=xxxxx
```

**Setting Secrets in Vercel:**
```bash
vercel env add NG_APP_API_BASE_URL
vercel env add NEXT_PUBLIC_API_URL  # If using Next.js
```

---

## Application Deployment

### 1. Clone Repository

```bash
git clone https://github.com/arpit-jain-mygit/PaperMind.git
cd PaperMind
git checkout v[VERSION_NUMBER]
```

### 2. Install Dependencies

```bash
# Frontend (if applicable)
cd frontend
npm install
npm run build

# Backend
cd ../backend
npm install

# Worker/Services (if applicable)
cd ../worker
npm install
```

### 3. Database Setup

```bash
# Run migrations
npm run migrate:prod

# Seed data (if needed)
npm run seed:prod

# Verify database
npm run db:verify
```

### 4. Build Application

```bash
npm run build
```

### 5. Deploy Frontend to Vercel

```bash
# Install Vercel CLI
npm install -g vercel

# Navigate to frontend directory
cd frontend

# Deploy
vercel --prod

# Or link to GitHub for auto-deploy on push
vercel link
# Then every git push auto-deploys
```

**Vercel Configuration (vercel.json):**
```json
{
  "buildCommand": "ng build",
  "outputDirectory": "dist/papermind",
  "env": {
    "NG_APP_API_BASE_URL": "https://papermind-api.railway.app"
  }
}
```

**Result:**
- Frontend deployed at: `https://papermind.vercel.app` (or custom domain)
- Auto-deploys on every git push
- Zero cost

---

### 6. Deploy Backend to Railway

```bash
# Install Railway CLI
npm install -g @railway/cli

# Or use Homebrew
brew install railway

# Login to Railway
railway login

# Navigate to backend directory
cd backend

# Initialize Railway project
railway init

# Set environment variables
railway variables set ENVIRONMENT=production
railway variables set DATABASE_URL=postgresql://...
railway variables set QDRANT_URL=https://...
railway variables set ANTHROPIC_API_KEY=sk-...

# Deploy
railway up

# Or link GitHub for auto-deploy
railway link
```

**Railway Configuration (railway.toml):**
```toml
[build]
builder = "dockerfile"
dockerfilePath = "./Dockerfile"

[deploy]
restartPolicyType = "on-failure"
restartPolicyMaxRetries = 3
```

**Dockerfile for FastAPI:**
```dockerfile
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install -r requirements.txt

COPY app/ .

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

**Result:**
- Backend deployed at: `https://papermind-api.railway.app`
- Cost: ~$0.20-0.50/month (pay-as-you-go)
- Auto-deploys on every git push (if linked to GitHub)

### 7. Verify Deployments

**Frontend (Vercel):**
```bash
# Check deployment status
vercel list

# View logs
vercel logs papermind

# Custom domain (optional)
vercel domains add papermind.com
```

**Backend (Railway):**
```bash
# Check deployment status
railway status

# View logs
railway logs

# Monitor metrics
railway metrics
```

---

## Post-Deployment Verification

### Health Checks

**Backend (Railway):**
```bash
# Test health endpoint
curl https://papermind-api.railway.app/health

# Test API endpoints
curl https://papermind-api.railway.app/api/documents
curl https://papermind-api.railway.app/docs  # Swagger UI

# Expected response: 200 OK
```

**Frontend (Vercel):**
```bash
# Test frontend
curl https://papermind.vercel.app

# Should return HTML (Angular app)
```

**Checklist:**
- [ ] Frontend accessible at `https://papermind.vercel.app`
- [ ] Backend accessible at `https://papermind-api.railway.app`
- [ ] `/health` endpoint returns 200
- [ ] `/docs` (Swagger) accessible
- [ ] Database connection successful (Railway dashboard)
- [ ] Qdrant vector DB accessible
- [ ] API endpoints responding with correct status codes
- [ ] CORS configured correctly (frontend can call backend)

### Functional Tests

- [ ] User authentication working
- [ ] Document upload functionality working
- [ ] RAG queries returning results
- [ ] Document search/retrieval working
- [ ] Admin features accessible

### Performance Tests

- [ ] Response time < [TARGET_MS]
- [ ] Database queries within SLA
- [ ] Load handling verified
- [ ] No memory leaks detected

### Security Verification

- [ ] SSL certificate valid and installed
- [ ] Security headers present (HSTS, CSP, etc.)
- [ ] Authentication enforced
- [ ] CORS properly configured
- [ ] No sensitive data in logs

```bash
# Check SSL
curl -I https://[DOMAIN]

# Verify headers
curl -I https://[DOMAIN] | grep -E "Strict-Transport|Content-Security"
```

### Monitoring Setup

- [ ] Error tracking (Sentry/New Relic) operational
- [ ] Logging aggregation (ELK/CloudWatch) receiving logs
- [ ] Alerting rules configured
- [ ] Dashboards created and accessible
- [ ] Uptime monitoring configured

---

## Rollback Procedure

### Vercel Rollback (Frontend)

```bash
# View deployment history
vercel list

# Rollback to previous deployment
vercel rollback

# Or manually redeploy specific commit
git checkout <previous-commit>
git push  # Vercel auto-deploys
```

### Railway Rollback (Backend)

```bash
# View deployment history
railway deployments

# Rollback to previous version
railway rollback <deployment-id>

# Or redeploy from git
git revert <bad-commit>
git push  # Railway auto-deploys if linked
```

### Database Rollback

```bash
# Restore from Railway PostgreSQL backup
railway database backups

# Restore specific backup
railway database restore <backup-id>

# Or use migration down
cd backend
python -m alembic downgrade -1  # Go back 1 version
```

### Communication

- [ ] Notify stakeholders
- [ ] Document rollback reason in post-mortem
- [ ] Update status page
- [ ] Schedule post-incident review

---

## Monitoring & Maintenance

### Daily Checks

**Vercel Dashboard:**
```bash
# Check frontend health
vercel status

# View error logs
vercel logs papermind
```

**Railway Dashboard:**
```bash
# Check backend health
railway status

# View error logs
railway logs --follow

# Monitor resource usage
railway metrics
```

### Weekly Tasks

- [ ] Review error logs (Railway dashboard)
- [ ] Check deployment status
- [ ] Verify PostgreSQL backups auto-enabled (Railway)
- [ ] Review API response times (LangSmith dashboard)
- [ ] Check Qdrant vector DB status

### Monthly Tasks

- [ ] Update dependencies (Python, Node)
- [ ] Review cost on Railway ($0.20-0.50 expected)
- [ ] Database optimization (if needed)
- [ ] Review LLM costs (should be <$2)
- [ ] Security patches

### Useful Commands

**Frontend:**
```bash
vercel logs papermind --follow  # Real-time logs
vercel env list                   # Environment vars
vercel insights                   # Performance metrics
```

**Backend:**
```bash
railway logs --follow             # Real-time logs
railway variables list            # Environment vars
railway metrics                   # CPU, RAM, disk usage
railway database                  # PostgreSQL info
railway redis                     # Redis info (if used)
```

**Both:**
```bash
# Monitor costs
railway billing

# Check deployments
railway deployments
vercel list
```

---

## Contact & Escalation

**Deployment Team**: ___________________
**On-Call Contact**: ___________________
**Escalation Path**: ___________________
**Post-Incident Review**: ___________________

---

## Deployment URLs

### Production Deployment

| Component | URL | Platform | Status |
|-----------|-----|----------|--------|
| **Frontend** | https://papermind.vercel.app | Vercel | Live |
| **Backend API** | https://papermind-api.railway.app | Railway | Live |
| **API Docs** | https://papermind-api.railway.app/docs | Railway | Live |
| **Monitoring** | Railway Dashboard | Railway | Live |
| **LLM Monitoring** | LangSmith Dashboard | LangSmith | Live |

---

## Deployment History

| Date | Version | Component | Platform | Deployer | Status | Notes |
|------|---------|-----------|----------|----------|--------|-------|
| 2026-07-16 | v1.0.0 | Frontend + Backend | Vercel + Railway | Arpit | Live | Initial production deployment |
| | | | | | | |
| | | | | | | |

---

## Additional Resources

- [Repository](https://github.com/arpit-jain-mygit/PaperMind)
- [Architecture Documentation](#)
- [API Documentation](#)
- [Troubleshooting Guide](#)
- [Incident Response](#)

**Last Updated**: ___________________
**Next Review Date**: ___________________
