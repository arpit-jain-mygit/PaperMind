# SYSTEM DESIGN: QUICK REVISION SHEET
**All Formulas + Twitter Example (Side-by-Side)**

## Table of Contents
1. [BASE ASSUMPTIONS FOR POPULAR SYSTEMS](#base-assumptions-for-popular-systems)
2. [🧠 Memorize These! - Cheat Sheet](#-memorize-these-printer-friendly-cheat-sheet)
3. [INPUT ASSUMPTIONS](#input-assumptions-twitter-example)
4. [1. QPS FORMULA](#1-qps-formula)
5. [2. STORAGE FORMULA](#2-storage-formula)
6. [3. BANDWIDTH FORMULA](#3-bandwidth-formula)
7. [4. DATABASE CAPACITY FORMULA](#4-database-capacity-formula)
8. [5. CACHING LAYER FORMULA](#5-caching-layer-formula)
9. [6. COMPLETE INFRASTRUCTURE COST BREAKDOWN](#6-complete-infrastructure-cost-breakdown)
10. [7. QUICK DECISION MATRIX](#7-quick-decision-matrix)

---

## BASE ASSUMPTIONS FOR POPULAR SYSTEMS

**How to derive QPS, Storage, and Costs from base metrics:**

| System | DAU | Req/Day | Resp Size | Read:Write | Peak×hrs | Retention | QPS Calc | Storage Calc |
|:---|---:|---:|---:|---:|---:|---:|---:|---:|
| **Twitter** | 300M | 20 | 2 KB | 10:1 | 4×4h | 5 yrs | (300M×20)÷100K=60K avg, ×4=240K peak | 10MB/user → 3PB/day → 5.1PB DB |
| **YouTube** | 500M | 50 | 20 KB | 100:1 | 5×4h | 2 yrs | (500M×50)÷100K=250K avg, ×5=1.25M peak | 100MB/user → 50PB/day → 500PB DB |
| **Uber** | 100M | 100 | 5 KB | 5:1 | 3×4h | 3mo | (100M×100)÷100K=100K avg, ×3=300K peak | 50MB/day → 25TB → 50PB total |
| **Netflix** | 300M | 30 | 50 KB | 100:1 | 5×6h | 2 yrs | (300M×30)÷100K=90K avg, ×5=450K peak | 200MB/user → 60PB/day → 500PB DB |
| **Instagram** | 500M | 100 | 10 KB | 20:1 | 4×4h | 10 yrs | (500M×100)÷100K=500K avg, ×4=2M peak | 50MB/user → 25PB/day → 1-2EB DB |
| **Stripe** | 1M* | 1000 | 2 KB | 2:1 | 2×8h | 10 yrs | (1M×1000)÷100K=100K avg, ×2=200K peak | 10MB/txn → 10TB/day → 10PB total |

**Key Notes:**
- *Stripe DAU = business accounts (not end users)
- Req/Day = requests per user per day (heavy usage systems have higher numbers)
- Resp Size = average response size in bytes
- Read:Write = ratio (more reads = more scalable, fewer writes = smaller DB)
- Peak = multiplier × hours of peak (e.g., 4×4h = 4X traffic for 4 hours)
- Retention = how long data kept (longer = bigger storage costs)

**Derived Formulas:**
- **Peak QPS** = (DAU × Req/Day) ÷ 100K × Peak_mult
- **Daily Requests** = (Average_QPS × 3.6K × avg_hrs) + (Peak_QPS × 3.6K × peak_hrs)
- **Daily Data** = DAU × Data_per_user
- **Storage Needed** = Daily_Data × retention_days × redundancy ÷ compression
- **Bandwidth** = Peak_QPS × response_size × 8 ÷ 10^9 × redundancy

---

## 🧠 MEMORIZE THESE! (Printer-Friendly Cheat Sheet)

**PRINT IN LANDSCAPE - FITS ON 1 PAGE**

| **POWERS OF 10 & TIME** | **LATENCY & SYSTEMS** | **SCALE & FORMULAS** | **UPTIME MNEMONICS** |
|:---|:---|:---|:---|
| **Data Sizes (10^X):** | **Latency (ms):** | **QPS Formula:** | **3 NINES (99.9%) = RLF** |
| 10^3 = 1 KB | L1: 0.0005 | QPS = (DAU × Req/day) ÷ 100K | **R**=Replication (DB, Cache) |
| 10^6 = 1 MB | Mem: 0.1 | Peak_QPS = Avg_QPS × Peak_mult | **L**=Load Balancing (Web, DB) |
| 10^9 = 1 GB | SSD: 0.25 | Servers = Peak ÷ capacity | **F**=Failover (manual, 5-10m) |
| 10^12 = 1 TB | Disk: 10 | Daily_Req = Off×3.6K×avg_hrs + Peak×3.6K×peak_hrs | Downtime: 8.76 hours/year |
| 10^15 = 1 PB | DC: 0.5 | | |
| 10^18 = 1 EB | Redis: 2 | **Storage Formula:** | **4 NINES (99.99%) = AHM** |
| 10^21 = 1 ZB | DB: 100 | Storage = DAU × data_per_user × retention | **A**=Automation (failover, scale) |
| 10^24 = 1 YB | US: 150 | × redundancy ÷ compression | **H**=Health Checks (10s freq) |
| | | | **M**=Multi-Region (US/EU/Asia) |
| **Time Constants:** | **Throughput:** | **Bandwidth Formula:** | Downtime: 53 minutes/year |
| Sec/Day: **100K** | Network: 125 MB/s | BW = Peak × Resp_size × 8 ÷ 10^9 × redundancy | |
| Sec/Year: **32M** | SSD: 100+ MB/s | | **5 NINES (99.999%) = PRZ** |
| Hour/Year: **8.76K** | HDD: 1-10 MB/s | **Database Formula:** | **P**=Prediction (ML, anomaly) |
| Min/Year: **525.6K** | Memory: 10+ GB/s | Write_QPS = Peak ÷ read_write_ratio | **R**=Redundant ISPs (dual) |
| | QPS/Server: 1K-10K | DB_Size = Write_QPS × record_size × | **Z**=Zero-Downtime Updates |
| **Cost Baseline:** | | retention × index × redundancy | Downtime: 5 minutes/year |
| Servers: $50K/yr | | | **Only for critical systems!** |
| Cache (RAM): $1.5/GB | | **Caching Formula:** | Cost: $5M+/year |
| SSD Storage: $276K/TB | | Cache = DB_size × hot_ratio × redundancy | |
| HDD Storage: $36K/TB | | Hit_rate reduces DB_QPS by 80%+ | **RLF → AHM → PRZ** |
| | | | Each adds resilience layer |
| | | | Most systems use AHM (4 nines) |
| | | | Ask "Why" before over-building |

---

## INPUT ASSUMPTIONS (Twitter Example)

```
├─ DAU: 300 Million
├─ Requests/user/day: 20
├─ Peak multiplier: 4X
├─ Peak hours: 4 hours
├─ Retention: 5 years
└─ Response size: 2 KB
```

---

## 1. QPS FORMULA

| Formula | Twitter Calculation |
|---------|-------------------|
| **Off-peak QPS** = (DAU × Requests/user) ÷ 100K | (300M × 20) ÷ 100K = **60,000 QPS** |
| **Peak QPS** = Off-peak × Peak_mult | 60,000 × 4 = **240,000 QPS** |
| **Daily Requests** = Off-peak × 3,600 × avg_hrs + Peak × 3,600 × peak_hrs | (60K × 3.6K × 20) + (240K × 3.6K × 4) = **7.8B requests/day** |
| **Servers** = Peak QPS ÷ Server_capacity | 240,000 ÷ 500 = **480 servers** |
| **With 2X Redundancy** = Servers × 2 | 480 × 2 = **960 servers** |
| **Auto-scale** = Peak_servers - Off-peak_servers | 960 - 240 = **+720 servers for peak_hrs** |

---

## 2. STORAGE FORMULA

| Formula | Twitter Calculation |
|---------|-------------------|
| **Daily Data** = DAU × data_per_user | 300M × 10 MB = **3 PB/day** |
| **Retention Total** = Daily_data × retention_days | 3 PB × 1,825 = **5,475 PB** |
| **With Redundancy** = Total × redundancy | 5,475 PB × 2 = **10,950 PB** |
| **With Compression** = With_redundancy ÷ compression_ratio | 10,950 ÷ 1.5 = **7,300 PB final** |
| **Tiered Storage (Hot)** = Daily × 365 × hot_years | 3 PB × 365 × 1 = **1,095 PB** @ $276K/TB/yr |
| **Tiered Storage (Warm)** = Daily × 365 × warm_years | 3 PB × 365 × 4 = **4,380 PB** @ $36K/TB/yr |
| **Total Annual Cost** | Hot: $303M + Warm: $158M = **~$461M/year** |

---

## 3. BANDWIDTH FORMULA

| Formula | Twitter Calculation |
|---------|-------------------|
| **Bytes Per Second** = Peak_QPS × response_size | 240K × 2 KB = **480 MB/sec** |
| **Gbps** = (Bytes/sec × 8) ÷ 10^9 | (480 × 8) ÷ 10^9 = **3.84 Gbps** |
| **With Redundancy** = Gbps × bw_redundancy | 3.84 × 10 = **38.4 Gbps** |
| **Annual Cost** | 38.4 Gbps × $50K/Gbps = **~$2M/year** |

---

## 4. DATABASE CAPACITY FORMULA

| Formula | Twitter Calculation |
|---------|-------------------|
| **Write QPS** = Peak_QPS ÷ read_write_ratio | 240K ÷ 11 = **21,818 writes/sec** |
| **Records Per Day** = Write_QPS × 86400 | 21,818 × 86,400 = **1.88B records/day** |
| **Daily Data Volume** = Records × record_size | 1.88B × 500 B = **0.94 TB/day** |
| **Total With Retention** = Daily × 365 × retention_yrs | 0.94 TB × 1,825 = **1.7 PB** |
| **With Index Overhead** = Total × index_mult | 1.7 PB × 1.5 = **2.55 PB** |
| **With Replication** = With_indexes × db_redundancy | 2.55 PB × 2 = **5.1 PB** |
| **Annual Cost** | 5.1 PB × $276K/TB = **~$1.4B/year** (HDD: $184M) |

---

## 5. CACHING LAYER FORMULA

| Formula | Twitter Calculation |
|---------|-------------------|
| **Cache Hit Rate** = hit_rate | **80%** of requests from cache |
| **Cache Miss Rate** = 1 - hit_rate | 1 - 0.80 = **20%** miss rate |
| **DB Hits QPS** = Peak_QPS × (1 - hit_rate) | 240K × 0.20 = **48K QPS** (vs 240K!) |
| **Working Set** = DB_size × hot_data_ratio | 5.1 PB × 0.20 = **1.02 PB** |
| **Cache Size** = Working_set × cache_redundancy | 1.02 PB × 2 = **2.04 PB** |
| **Cache Servers** = Cache_size ÷ ram_per_server | 2.04 PB ÷ 0.512 PB = **~4K servers** |
| **DB Load Reduction** = hit_rate | **80%** load reduction |
| **Annual Cost** | 2.04 PB × $1.5K/GB = **~$3M/year** |

---

## 6. COMPLETE INFRASTRUCTURE COST BREAKDOWN

| Component | Metric | Cost Formula | Twitter Cost |
|-----------|--------|--------------|--------------|
| **Servers** | 960 peak | $50K/500 QPS × (960÷500) | **$48M/year** |
| **Bandwidth** | 38.4 Gbps | $50K/Gbps | **$2M/year** |
| **Database Storage** | 5.1 PB | $276K/TB (SSD) or $36K/TB (HDD) | **$184M/year** (HDD) |
| **Hot Storage (1yr)** | 1,095 PB | $276K/TB/yr | **$303M/year** |
| **Warm Storage (4yr)** | 4,380 PB | $36K/TB/yr | **$158M/year** |
| **Cache (RAM)** | 2.04 PB | $1.5K/GB/yr | **$3M/year** |
| **Operations** | Monitoring, logging, etc. | ~20-30% of infrastructure | **$100M/year** |
| **TOTAL** | **Twitter-scale system** | Sum all costs | **~$455M/year** |

---

## 7. QUICK DECISION MATRIX

| Question | Formula to Use | Example |
|----------|----------------|---------|
| How many servers? | Peak QPS ÷ capacity | 240K ÷ 500 = 480 |
| How much bandwidth? | QPS × response_size × 8 ÷ 10^9 | 240K × 2KB = 38.4 Gbps |
| How much storage? | Write_QPS × record_size × retention | 21.8K × 500B × 5yr = 5.1 PB |
| How much cache? | DB_size × 20% × 2X | 5.1 PB × 20% × 2 = 2.04 PB |
| Will peak break us? | Peak QPS > (servers × capacity)? | 240K > 480 × 500? NO |
| Availability needed? | Use SLA → downtime conversion | 99.99% = 52 min/year max |

---

**Print this sheet and keep it with you during interviews! Master these constants, mnemonics, and 5 formulas and you can estimate any system.** ✅
