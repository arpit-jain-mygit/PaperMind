# SYSTEM DESIGN: QUICK REVISION SHEET
**All Formulas + Twitter Example (Side-by-Side)**

## Table of Contents
1. [🧠 Memorize These! - Cheat Sheet](#-memorize-these-printer-friendly-cheat-sheet)
2. [INPUT ASSUMPTIONS](#input-assumptions-twitter-example)
3. [1. QPS FORMULA](#1-qps-formula)
4. [2. STORAGE FORMULA](#2-storage-formula)
5. [3. BANDWIDTH FORMULA](#3-bandwidth-formula)
6. [4. DATABASE CAPACITY FORMULA](#4-database-capacity-formula)
7. [5. CACHING LAYER FORMULA](#5-caching-layer-formula)
8. [6. COMPLETE INFRASTRUCTURE COST BREAKDOWN](#6-complete-infrastructure-cost-breakdown)
9. [7. QUICK DECISION MATRIX](#7-quick-decision-matrix)

---

## 🧠 MEMORIZE THESE! (Printer-Friendly Cheat Sheet)

**PRINT IN LANDSCAPE - FITS ON 1 PAGE**

| **POWERS OF 10 & TIME** | **LATENCY & SYSTEMS** | **SCALE & FORMULAS** | **UPTIME MNEMONICS** |
|:---|:---|:---|:---|
| **Data Sizes (10^X):** | **Latency (ms):** | **Popular Systems:** | **Availability → Downtime/Year:** |
| 10^3 = 1 KB | L1: 0.0005 | **Twitter** | **RLF = Rule of 3 Fives** |
| 10^6 = 1 MB | Mem: 0.1 | • QPS: 240K peak | 99% = 3.65 **days** |
| 10^9 = 1 GB | SSD: 0.25 | • Servers: 960 | 99.9% = 8.76 **hours** |
| 10^12 = 1 TB | Disk: 10 | • Storage: 5-100 PB | 99.99% = 53 **minutes** |
| 10^15 = 1 PB | DC: 0.5 | • Cost: $455M/yr | 99.999% = 5 **minutes** |
| 10^18 = 1 EB | Redis: 2 | **YouTube** | **AHM = Add Hours, Multiply Days** |
| 10^21 = 1 ZB | DB: 100 | • QPS: 1M+ peak | 99% → 3.65d (3.65×24h) |
| 10^24 = 1 YB | US: 150 | • Storage: 500 PB+ | 99.9% → 8.76h (9h/yr max) |
| | | • Retention: 1-2 yrs | 99.99% → 53m (1h/yr max) |
| **Time Constants:** | **Throughput:** | **Uber** | **PRZ = Percentage Rule** |
| Sec/Day: **100K** | Network: 125 MB/s | • QPS: 50-100K | 99%: 1 nines = 36.5d |
| Sec/Year: **32M** | SSD: 100+ MB/s | • Servers: 100-200 | 99.9%: 2 nines = 8.7h |
| Hour/Year: **8.76K** | HDD: 1-10 MB/s | • Storage: 50 PB | 99.99%: 3 nines = 52m |
| Minutes/Year: **525.6K** | Memory: 10+ GB/s | • Retention: 3 months | 99.999%: 4 nines = 5m |
| | QPS/Server: 1K-10K | **Netflix** | |
| **Cost Baseline:** | | • QPS: 200-500K | **Cost Baseline (Annual):** |
| Servers: $50K/yr | | • Servers: 400-1K | Servers: $50K (500 QPS) |
| Cache (RAM): $1.5/GB | | • Storage: 100-500 PB | Cache: $1.5/GB/yr (RAM) |
| SSD Storage: $276K/TB | | • Retention: 2 years | SSD Storage: $276K/TB/yr |
| HDD Storage: $36K/TB | | **Instagram** | HDD Storage: $36K/TB/yr |
| | | • QPS: 500K-1M | **Twitter Math Example:** |
| | | • Servers: 1K-2K | 300M DAU × 20 req/day |
| | | • Storage: 1-2 EB | ÷ 100K sec = 60K avg |
| | | • Retention: 10 years | × 4X peak = 240K peak |
| | | **Stripe** | Cost: $455M/year total |
| | | • QPS: 10-100K | Breakdown: Server $50M + |
| | | • Servers: 20-200 | Storage $300M + BW $2M + |
| | | • Storage: 10 PB | Cache $3M + Ops $100M |
| | | • Retention: 10 years | |

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
