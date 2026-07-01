# 🎯 SYSTEM DESIGN INTERVIEW CHEAT SHEET
## The 7-Step Universal Framework (Print & Paste on Wall!)

---

## **STEP 1: FUNCTIONAL REQUIREMENTS (5 min)**
```
Ask the interviewer:
□ What specific features must we build?
□ Who are the users?
□ How do users interact with the system?

GOAL: Understand WHAT you're building
```

---

## **STEP 2: SCALE & NON-FUNCTIONAL REQUIREMENTS (5 min)**
```
Ask the interviewer:
□ How many Daily Active Users (DAU)?
□ What's the read-to-write ratio?
□ Expected spike traffic (peak factor)?
□ Latency requirement? (<100ms? <1s? <5s?)
□ Availability target? (99.9%? 99.99%?)
□ Strong consistency or eventual consistency OK?

GOAL: Understand CONSTRAINTS & SCALE
```

---

## **STEP 3: GENERIC BLUEPRINT (3 min)**
```
Draw this template for EVERY system:

        [Mobile/Web Clients]
               ↓
           [CDN for assets]
               ↓
          [Load Balancer]
               ↓
   [Auth] [Service] [Service] [Service]
               ↓
         [Cache Layer - Redis]
               ↓
      [Master Database]
      ↓
    [Replicas]  [S3 Storage]  [Message Queue]

GOAL: Start with a known-good baseline
```

---

## **STEP 4: CUSTOMIZE FOR YOUR SYSTEM (10 min)**
```
For this specific system:
- REMOVE: ___________________
- ADD: ___________________
- MODIFY: ___________________

Example: Instagram
  ADD: Image processing, search (Elasticsearch)
  REMOVE: Real-time chat
  MODIFY: Cache for photo feeds

GOAL: Adapt generic template to this problem
```

---

## **STEP 5: BACK-OF-ENVELOPE MATH (5 min)**
```
⚡ CRITICAL FORMULAS (MEMORIZE THESE!)

DAU to QPS:
  (DAU × requests_per_day) ÷ 86,400 = avg QPS
  Peak QPS = avg QPS × 2.5 (typical peak factor)

QPS to Servers:
  Peak QPS ÷ 1,000-10,000 QPS per server = servers needed
  With redundancy: × 2-3

Storage per Day:
  (Users × % uploading) × avg_file_size = daily storage
  Daily × 365 × years × 3 (redundancy) = total

Example (100M DAU Instagram):
  100M × 5 req/day ÷ 86,400 = 5,787 QPS avg
  5,787 × 2.5 = 14,467 QPS peak
  14,467 ÷ 1,000 = 14-15 servers minimum
  With redundancy: 30-40 servers

GOAL: Verify your design can handle the scale
```

---

## **STEP 6: DESIGN DEEP-DIVES (10 min)**
```
For EACH critical component, address:

🛡️ RESILIENCE
  □ What if this service crashes?
  □ How do we detect failure?
  □ How do we recover?

📊 MONITORING & OPERATIONS
  □ What metrics do we track?
  □ What alerts do we set?
  □ What's the SLA we can achieve?

⚖️ CONSISTENCY vs AVAILABILITY
  □ Is data consistency critical?
  □ Can we tolerate stale data?
  □ What's the acceptable lag?

GOAL: Address non-functional requirements
```

---

## **STEP 7: VERIFY & CONSTRAINTS (7 min)**
```
Checklist - does your design handle:

Scale:
  □ Original DAU requirement? ✓
  □ 10X growth without major changes?

Performance:
  □ Latency requirement met?
  □ Cache hit rate sufficient?

Availability:
  □ 99.9% uptime achievable?
  □ Can tolerate 1 component failure?

Compliance:
  □ GDPR/HIPAA requirements?
  □ Data residency requirements?

GOAL: Sanity check the complete design
```

---

## **⏱️ TIMING (45 MIN INTERVIEW)**
```
 5 min │ STEP 1: Functional Requirements
 5 min │ STEP 2: Scale & NFRs
 3 min │ STEP 3: Generic Blueprint
10 min │ STEP 4: Customize
 5 min │ STEP 5: Back-of-Envelope Math
10 min │ STEP 6: Deep-Dives
 7 min │ STEP 7: Verify
─────────────────────────────────
45 min │ TOTAL
```

---

## **💡 QUICK REFERENCE (MEMORIZE!)**

| Metric | Formula | Example |
|--------|---------|---------|
| **QPS** | (DAU × req/day) ÷ 86,400 | 100M × 5 ÷ 86,400 = 5.8K |
| **Peak QPS** | avg QPS × 2.5 | 5.8K × 2.5 = 14.5K |
| **Servers** | Peak QPS ÷ 1K | 14.5K ÷ 1K = 14-15 |
| **Storage/day** | Users × upload% × size | 100M × 10% × 3MB = 30TB |
| **Total Storage** | daily × 365 × years × 3 | 30TB × 365 × 10 × 3 = 330PB |

---

## **🔧 GENERIC COMPONENTS (Copy-Paste Every Time)**
```
ALWAYS include:
├─ Load Balancer (traffic distribution)
├─ Stateless Services (horizontal scaling)
├─ Cache Layer - Redis (hot data)
├─ Database Master-Slave (replication)
├─ Read Replicas (scale reads)
├─ Object Storage - S3 (files/blobs)
├─ Message Queue - Kafka (async processing)
└─ CDN (static content delivery)

CUSTOMIZE: Add/Remove/Modify based on STEP 4
```

---

## **⚠️ COMMON MISTAKES TO AVOID**
```
❌ Skip clarification questions
❌ Design without back-of-envelope math
❌ Ignore consistency vs availability trade-off
❌ Over-engineer for requirements you haven't clarified
❌ Assume all traffic is equal (ignore peak factors)
❌ Forget about monitoring & operations
```

---

## **✅ INTERVIEW TIPS**
```
✓ Ask "why" after each answer
✓ Write formulas as you calculate
✓ Draw simple ASCII diagrams
✓ Communicate constantly (don't present surprise at end)
✓ Trade-offs matter more than perfection
✓ Be ready to deep-dive on any component
✓ Verify assumptions: "Did I get that right?"
```

---

## **EXAMPLE: 100M DAU Instagram**
```
Step 1: Upload photos, see feed, like, comment
Step 2: 100M DAU, 9:1 read:write, 2.5× peak, <200ms latency, 99.99% uptime
Step 3: Draw generic blueprint
Step 4: ADD image processing + search, REMOVE real-time chat
Step 5: 14.5K QPS peak → 30-40 servers, 330 PB storage
Step 6: Cache hit rate >80%, replicate DB, CDN for images
Step 7: ✓ All constraints met, design validated
```

---

**Print this page. Use it in interviews. ⭐**
