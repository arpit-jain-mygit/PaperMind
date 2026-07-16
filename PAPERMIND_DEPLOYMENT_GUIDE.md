# PaperMind - Deployment Guide

This document contains step-by-step instructions to deploy PaperMind to production from scratch.

## Table of Contents
1. [Pre-Deployment Checklist](#pre-deployment-checklist)
2. [Infrastructure Setup](#infrastructure-setup)
3. [Environment Configuration](#environment-configuration)
4. [Application Deployment](#application-deployment)
5. [Post-Deployment Verification](#post-deployment-verification)
6. [Rollback Procedure](#rollback-procedure)
7. [Monitoring & Maintenance](#monitoring--maintenance)

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

### Choose Deployment Platform
Document which platform is being used:
- **Platform**: ___________________
- **Region**: ___________________
- **Environment**: Production

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

```bash
Node version: ___________
npm version: ___________
Python version: ___________
Other dependencies: ___________
```

### Environment Variables

Create a `.env.production` file with:

```bash
# Application
NODE_ENV=production
APP_PORT=3000
LOG_LEVEL=info

# Database
DB_HOST=___________
DB_PORT=___________
DB_NAME=___________
DB_USER=___________
DB_PASSWORD=___________ # Store in secret manager

# RAG/AI Configuration
OPENAI_API_KEY=___________ # Store in secret manager
VECTOR_DB_HOST=___________
VECTOR_DB_PORT=___________

# Authentication
JWT_SECRET=___________ # Store in secret manager
SESSION_SECRET=___________ # Store in secret manager

# File Storage
STORAGE_BUCKET=___________
STORAGE_REGION=___________

# Monitoring
SENTRY_DSN=___________
```

**Important**: Never commit secrets to Git. Use platform-specific secret management:
- AWS: Secrets Manager / Parameter Store
- GCP: Secret Manager
- Azure: Key Vault
- Heroku: Config Vars

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

### 5. Deploy to Platform

#### Option A: Docker Deployment

```bash
# Build Docker image
docker build -t papermind:v[VERSION] .

# Push to registry
docker push [REGISTRY]/papermind:v[VERSION]

# Deploy
# (Platform-specific commands here)
```

#### Option B: Direct Deployment

```bash
# Copy files to server
scp -r ./dist root@[SERVER_IP]:/app/

# Set permissions
ssh root@[SERVER_IP] "chown -R app:app /app"

# Start application
ssh root@[SERVER_IP] "systemctl restart papermind"
```

#### Option C: Platform-Specific (Vercel, Heroku, AWS Lambda, etc.)

```bash
# Document platform-specific deployment commands
# Example for Vercel:
vercel --prod

# Example for Heroku:
heroku login
git push heroku main
```

### 6. Start Application

```bash
# Check service status
systemctl status papermind

# View logs
journalctl -u papermind -f

# Or with PM2:
pm2 start app.js --name papermind
pm2 save
```

---

## Post-Deployment Verification

### Health Checks

- [ ] Application is responding on `https://[DOMAIN]/health`
- [ ] Database connection successful
- [ ] Redis/Cache layer responding
- [ ] File storage accessible
- [ ] API endpoints responding with 200 status

```bash
# Test endpoints
curl https://[DOMAIN]/health
curl https://[DOMAIN]/api/v1/docs/status
curl https://[DOMAIN]/api/v1/auth/status
```

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

If deployment fails or critical issues arise:

### Immediate Rollback

```bash
# Option 1: Docker
docker service update --image [REGISTRY]/papermind:v[PREVIOUS_VERSION] papermind

# Option 2: Systemd
systemctl stop papermind
git checkout v[PREVIOUS_VERSION]
npm install
npm run build
systemctl start papermind

# Option 3: Blue-Green Deployment
# Keep previous version running and switch traffic back
```

### Database Rollback

```bash
# Restore from backup
npm run migrate:down -- --steps [N]

# Or restore from database snapshot
# (Platform-specific restore procedure)
```

### Communication

- [ ] Notify stakeholders
- [ ] Document rollback reason in post-mortem
- [ ] Update status page
- [ ] Schedule post-incident review

---

## Monitoring & Maintenance

### Daily Checks

```bash
# Check application logs for errors
journalctl -u papermind -p err --since "1 day ago"

# Monitor resource usage
top -p $(pgrep -f "papermind")

# Check database size
```

### Weekly Tasks

- [ ] Review error logs and trends
- [ ] Verify backups are running
- [ ] Check certificate expiration (30+ days out)
- [ ] Review performance metrics

### Monthly Tasks

- [ ] Security patches applied
- [ ] Dependencies updated (if safe)
- [ ] Database optimization/maintenance
- [ ] Capacity planning review

### Useful Commands

```bash
# View application logs
tail -f /var/log/papermind/app.log

# Check database status
npm run db:status

# View active connections
npm run db:connections

# Run diagnostics
npm run diagnostics
```

---

## Contact & Escalation

**Deployment Team**: ___________________
**On-Call Contact**: ___________________
**Escalation Path**: ___________________
**Post-Incident Review**: ___________________

---

## Deployment History

| Date | Version | Deployer | Status | Notes |
|------|---------|----------|--------|-------|
| | | | | |
| | | | | |

---

## Additional Resources

- [Repository](https://github.com/arpit-jain-mygit/PaperMind)
- [Architecture Documentation](#)
- [API Documentation](#)
- [Troubleshooting Guide](#)
- [Incident Response](#)

**Last Updated**: ___________________
**Next Review Date**: ___________________
