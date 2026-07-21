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

**QPS Formula:** (DAU × Req/Day) ÷ 100K × Peak_mult

**Storage Formula:** (Daily_data × retention_days × redundancy) ÷ compression

**Bandwidth Formula (Gbps):** (Peak_QPS × Response_size × 8 bits) ÷ 10^9 × bw_redundancy

**Database Formula:** (Peak_QPS ÷ R:W_ratio) × 86.4K sec × Rec_size × Ret_days × 1.5idx × Red

| System | DAU | Req | Size | R:W | Peak | Ret | Red | Cmp | QPS Calculation | Storage Calculation | Bandwidth Calculation | Database Calculation |
|:---|---:|---:|---:|---:|---:|---:|---:|---:|---|---|---|---|
| **Twitter** | 300M | 20 | 2K | 9:1 | 4×4h | 5y | 2x | 1.5x | (300M×20)÷100K=60K avg, ×4=**240K peak** | (3PB×1,825×2)÷1.5=**7.3EB** | (240K×2K×8)÷10^9×10=**38.4 Gbps** | (240K÷10)×86.4K×500B×1,825×1.5×2=**5.4PB** |
| **YouTube** | 500M | 50 | 20K | 99:1 | 5×4h | 2y | 3x | 1.1x | (500M×50)÷100K=250K avg, ×5=**1.25M peak** | (50PB×730×3)÷1.1=**99.5EB** | (1.25M×20K×8)÷10^9×10=**2,000 Gbps** | (1.25M÷100)×86.4K×1K×730×1.5×3=**47.5PB** |
| **Uber** | 100M | 100 | 5K | 4:1 | 3×4h | 3mo | 2x | 1.3x | (100M×100)÷100K=100K avg, ×3=**300K peak** | (5TB×90×2)÷1.3=**0.7PB** | (300K×5K×8)÷10^9×10=**120 Gbps** | (300K÷5)×86.4K×5K×90×1.5×2=**1.17PB** |
| **Netflix** | 300M | 30 | 50K | 99:1 | 5×6h | 2y | 3x | 1.1x | (300M×30)÷100K=90K avg, ×5=**450K peak** | (15PB×730×3)÷1.1=**29.8EB** | (450K×50K×8)÷10^9×10=**1,800 Gbps** | (450K÷100)×86.4K×1K×730×1.5×3=**14.2PB** |
| **Instagram** | 500M | 100 | 10K | 9:1 | 4×4h | 10y | 3x | 1.05x | (500M×100)÷100K=500K avg, ×4=**2M peak** | (5PB×3,650×3)÷1.05=**52.1EB** | (2M×10K×8)÷10^9×10=**1,600 Gbps** | (2M÷10)×86.4K×2K×3,650×1.5×3=**190.6PB** |
| **Stripe** | 1M* | 1000 | 2K | 1:1 | 2×8h | 10y | 3x | 1.5x | (1M×1000)÷100K=100K avg, ×2=**200K peak** | (10TB×3,650×3)÷1.5=**0.73EB** | (200K×2K×8)÷10^9×10=**32 Gbps** | (200K÷2)×86.4K×10K×3,650×1.5×3=**47.5PB** |
| **Google Drive** | 500M | 30 | 5K | 99:1 | 3×4h | 10y | 3x | 1.2x | (500M×30)÷100K=150K avg, ×3=**450K peak** | (50PB×3,650×3)÷1.2=**456EB** | (450K×5K×8)÷10^9×10=**180 Gbps** | (450K÷100)×86.4K×3K×3,650×1.5×3=**107.3PB** |
| **Dropbox** | 300M | 25 | 3K | 99:1 | 3×4h | 10y | 3x | 1.3x | (300M×25)÷100K=75K avg, ×3=**225K peak** | (15PB×3,650×3)÷1.3=**126EB** | (225K×3K×8)÷10^9×10=**54 Gbps** | (225K÷100)×86.4K×2K×3,650×1.5×3=**53.7PB** |
| **LinkedIn** | 300M | 50 | 10K | 19:1 | 4×4h | 5y | 3x | 1.2x | (300M×50)÷100K=150K avg, ×4=**600K peak** | (1.5PB×1,825×3)÷1.2=**6.85EB** | (600K×10K×8)÷10^9×10=**480 Gbps** | (600K÷20)×86.4K×1K×1,825×1.5×3=**117.5PB** |
| **IRCTC** | 20M | 100 | 5K | 9:1 | 6×4h | 2y | 2x | 1.3x | (20M×100)÷100K=20K avg, ×6=**120K peak** | (200TB×730×2)÷1.3=**0.22EB** | (120K×5K×8)÷10^9×10=**48 Gbps** | (120K÷10)×86.4K×5K×730×1.5×2=**0.95PB** |
| **WhatsApp** | 500M | 100 | 5K | 1:1 | 5×4h | 1y | 3x | 1.3x | (500M×100)÷100K=500K avg, ×5=**2.5M peak** | (136.9PB×365×3)÷1.3=**115EB** | (2.5M×5K×8)÷10^9×10=**1,000 Gbps** | (2.5M÷2)×86.4K×2K×365×1.5×3=**595PB** |
| **Newsfeed** | 200M | 80 | 10K | 999:1 | 2×4h | 5y | 2x | 1.1x | (200M×80)÷100K=160K avg, ×2=**320K peak** | (27.4PB×1,825×2)÷1.1=**91EB** | (320K×10K×8)÷10^9×10=**256 Gbps** | (320K÷1000)×86.4K×1K×1,825×1.5×2=**0.75PB** |
| **Facebook** | 400M | 150 | 15K | 9:1 | 4×4h | 10y | 3x | 1.2x | (400M×150)÷100K=600K avg, ×4=**2.4M peak** | (54.8PB×3,650×3)÷1.2=**500EB** | (2.4M×15K×8)÷10^9×10=**2,880 Gbps** | (2.4M÷10)×86.4K×2K×3,650×1.5×3=**597.8PB** |
| **Zerodha** | 5M | 500 | 5K | 99:1 | 10×6.5h | 10y | 3x | 1.3x | (5M×500)÷100K=25K avg, ×10=**250K peak** | (685TB×3,650×3)÷1.3=**5.77EB** | (250K×5K×8)÷10^9×10=**100 Gbps** | (250K÷100)×86.4K×5K×3,650×1.5×3=**59.6PB** |
| **ICICI Bank** | 20M | 200 | 3K | 9:1 | 3×4h | 7y | 3x | 1.2x | (20M×200)÷100K=40K avg, ×3=**120K peak** | (2.7PB×2,555×3)÷1.2=**17.25EB** | (120K×3K×8)÷10^9×10=**28.8 Gbps** | (120K÷10)×86.4K×10K×2,555×1.5×3=**9.9PB** |
| **HDFC Bank** | 25M | 200 | 3K | 9:1 | 3×4h | 7y | 3x | 1.2x | (25M×200)÷100K=50K avg, ×3=**150K peak** | (3.4PB×2,555×3)÷1.2=**21.75EB** | (150K×3K×8)÷10^9×10=**36 Gbps** | (150K÷10)×86.4K×10K×2,555×1.5×3=**12.4PB** |
| **Google Maps** | 500M | 50 | 20K | 999:1 | 3×4h | 5y | 3x | 1.2x | (500M×50)÷100K=250K avg, ×3=**750K peak** | (136.9PB×1,825×3)÷1.2=**625EB** | (750K×20K×8)÷10^9×10=**1,200 Gbps** | (750K÷1000)×86.4K×5K×1,825×1.5×3=**18.3PB** |
| **Spotify** | 300M | 200 | 10K | 99:1 | 4×4h | 2y | 3x | 1.1x | (300M×200)÷100K=600K avg, ×4=**2.4M peak** | (164.4PB×730×3)÷1.1=**327EB** | (2.4M×10K×8)÷10^9×10=**1,920 Gbps** | (2.4M÷100)×86.4K×2K×730×1.5×3=**45.2PB** |
| **Gaana** | 50M | 150 | 8K | 99:1 | 4×4h | 2y | 2x | 1.1x | (50M×150)÷100K=75K avg, ×4=**300K peak** | (13.7PB×730×2)÷1.1=**18.2EB** | (300K×8K×8)÷10^9×10=**192 Gbps** | (300K÷100)×86.4K×2K×730×1.5×2=**3.8PB** |
| **Amazon** | 100M | 200 | 15K | 19:1 | 4×4h | 5y | 3x | 1.2x | (100M×200)÷100K=200K avg, ×4=**800K peak** | (13.7PB×1,825×3)÷1.2=**62.5EB** | (800K×15K×8)÷10^9×10=**960 Gbps** | (800K÷20)×86.4K×5K×1,825×1.5×3=**99.8PB** |
| **Google Search** | 1B | 3 | 50K | 9999:1 | 2×4h | 1y | 4x | 1.1x | (1B×3)÷100K=30M avg, ×2=**60M peak** | (2.74EB×365×4)÷1.1=**3.6EB** | (60M×50K×8)÷10^9×10=**24,000 Gbps** | (60M÷10000)×86.4K×2K×365×1.5×4=**0.75PB** |
| **Slack** | 10M | 500 | 5K | 1:1 | 5×4h | 10y | 3x | 1.2x | (10M×500)÷100K=50K avg, ×5=**250K peak** | (13.7PB×3,650×3)÷1.2=**125EB** | (250K×5K×8)÷10^9×10=**100 Gbps** | (250K÷2)×86.4K×3K×3,650×1.5×3=**148.5PB** |
| **Airbnb** | 10M | 50 | 20K | 99:1 | 3×4h | 5y | 3x | 1.2x | (10M×50)÷100K=5K avg, ×3=**15K peak** | (2.74PB×1,825×3)÷1.2=**12.5EB** | (15K×20K×8)÷10^9×10=**24 Gbps** | (15K÷100)×86.4K×10K×1,825×1.5×3=**5.94PB** |
| **Rate Limiter** | 500M | 200 | 200B | 100:1 | 5×4h | 1y | 4x | 1.1x | (500M×200)÷100K=1M avg, ×5=**5M peak** | (13.7PB×365×4)÷1.1=**18.2EB** | (5M×200B×8)÷10^9×10=**80 Gbps** | (5M÷100)×86.4K×1K×365×1.5×4=**9.46PB** |

**Column Legend:**
- **Req** = Requests/user/day
- **Size** = Response size (K=KB)
- **R:W** = Read:Write ratio (using 9:1, 99:1, etc. for clean mental math)
- **Peak** = Multiplier × hours (e.g., 4×4h = 4X for 4 hours)
- **Ret** = Retention (y=years, mo=months)
- **Red** = Redundancy factor (2x or 3x)
- **Cmp** = Compression ratio
- **QPS** = Calculation showing average → peak QPS
- **Storage** = Calculation showing total for entire retention period
- **Bandwidth** = Calculation showing required Gbps capacity
- **Database** = Database capacity with indexes (1.5x) and replication (×Red)

**Key Notes:**
- *Stripe DAU = business accounts (not end users)
- R:W ratio (reads:writes) impacts database design - more reads = more scalable
  - Use 9:1, 99:1, etc. for clean mental math (÷10, ÷100) in interviews
- **Database = ONLY WRITES** (metadata/records, not media files like photos/videos)
  - Twitter: 5.4 PB = tweets + metadata (2.7 PB master + 2.7 PB replica)
  - YouTube: 47.5 PB = watch history + video metadata
  - Instagram: 190.6 PB = posts + comments + likes metadata
- Redundancy: 2x for standard, 3x for critical systems requiring multi-region HA
- Compression reduces by: 1.5x (text) = 33%, 1.1x (video) = 9%, 1.05x (photos) = 5%
- **Storage = TOTAL for entire retention**, not daily!
  - Twitter: 7.3 EB = 5 years of all data (photos, videos, metadata) with redundancy & compression
  - YouTube: 99.5 EB = 2 years of all data with redundancy & compression
- **Database vs Storage:**
  - Database holds indexed write records only (much smaller)
  - Storage holds all user content + backups (1,000x+ larger)

**Derived Formulas:**
- **Peak QPS** = (DAU × Req/Day) ÷ 100K × Peak_mult
- **Daily Requests** = (Average_QPS × 3.6K × avg_hrs) + (Peak_QPS × 3.6K × peak_hrs)
- **Daily Data** = DAU × Data_per_user
- **Storage Needed** = Daily_Data × retention_days × redundancy ÷ compression
- **Bandwidth** = Peak_QPS × response_size × 8 ÷ 10^9 × redundancy
- **Storage Cost** = Hot_PB × $276M/PB + Warm_PB × $36M/PB (7.7x difference!)

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
| **Time Constants:** | **Throughput:** | **Bandwidth Formula (Gbps):** | Downtime: 53 minutes/year |
| Sec/Day: **100K** | Network: 125 MB/s | BW = Peak × Resp_size × 8 bits ÷ 10^9 | |
| Sec/Year: **32M** | SSD: 100+ MB/s | | **5 NINES (99.999%) = PRZ** |
| Hour/Year: **8.76K** | HDD: 1-10 MB/s | **Database Formula:** | **P**=Prediction (ML, anomaly) |
| Min/Year: **525.6K** | Memory: 10+ GB/s | Write_QPS = Peak ÷ read_write_ratio | **R**=Redundant ISPs (dual) |
| | QPS/Server: 1K-10K | DB_Size = Write_QPS × record_size × | **Z**=Zero-Downtime Updates |
| **Cost Baseline:** | | retention × index × redundancy | Downtime: 5 minutes/year |
| Servers: $50K/yr | | | **Only for critical systems!** |
| **Hot Storage (SSD):** | | **Caching Formula:** | Cost: $5M+/year |
| • $276M/PB/yr | | Cache = DB_size × hot_ratio × redundancy | |
| • $23M/PB/mo | | Hit_rate reduces DB_QPS by 80%+ | **RLF → AHM → PRZ** |
| **Warm Storage (HDD):** | | | Each adds resilience layer |
| • $36M/PB/yr | | | Most systems use AHM (4 nines) |
| • $3M/PB/mo | | | Ask "Why" before over-building |
| Cache (RAM): $1.5/GB | | | **Twitter Cost Example:** |
| | | | Hot: 1,095PB @ $276M/PB = $303M |
| | | | Warm: 4,380PB @ $36M/PB = $158M |
| | | | **Total: $461M/year** |

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
| **Gbps (Gigabits/sec)** = (Bytes/sec × 8 bits) ÷ 10^9 | (480 × 8) ÷ 10^9 = **3.84 Gbps** |
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
