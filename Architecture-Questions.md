# Architecture Interview Questions

## 1. How did you achieve 99.99% uptime and how can you take it to 99.999%?

### Current State: 99.99% Uptime (52.6 minutes downtime/year)

#### Component Breakdown & Current Measures

**Load Balancing**
- **Current State (✓):** Multi-region load balancers (AWS ELB/ALB) with health checks every 5s
- **Fix Needed:** No — already redundant across regions
- **Improvement:** Switch to stateless load balancers; DNS failover as backup

**Application Servers**
- **Current State (✓):** 3+ app servers per region with auto-scaling (scale up/down in 30s)
- **Fix Needed:** Connections can spike during scale events
- **Improvement:** Pre-warming instances; circuit breakers to prevent cascade failures
- **Data:** Reduces unplanned downtime by ~0.02%

**Database**
- **Current State (✓):** Master-slave replication with automatic failover (15-30s failover time)
- **Fix Needed:** Failover latency contributes to ~20 minutes downtime/year
- **Improvement:** Multi-master with conflict resolution; read replicas in 3+ regions
- **Data:** Reduces downtime by ~0.03%

**Cache Layer**
- **Current State (✓):** Redis cluster with 2-3 replicas per shard
- **Fix Needed:** No — provides < 1s recovery
- **Improvement:** Consistent hashing; automatic rebalancing

**Message Queue**
- **Current State (✓):** Kafka with 3+ broker replication factor
- **Fix Needed:** No — data durability is guaranteed
- **Improvement:** No change needed for uptime

**Monitoring & Alerting**
- **Current State (✓):** Real-time dashboards; alerts fire in < 1 minute
- **Fix Needed:** No — detection is fast
- **Improvement:** Machine learning-based anomaly detection

#### Summary Table: Current 99.99% State

| Component | Downtime Contribution | Current Measure | Status |
|-----------|----------------------|-----------------|--------|
| Load Balancer | ~5 min/year | Multi-region with health checks | ✓ Working |
| App Servers | ~10 min/year | Auto-scaling + circuit breakers | ✓ Working |
| Database | ~20 min/year | Master-slave + failover | ⚠️ Needs improvement |
| Cache | ~5 min/year | Redis cluster replication | ✓ Working |
| Monitoring | ~12 min/year | Real-time dashboards | ✓ Working |
| **Total** | **~52 min/year** | - | **99.99%** |

---

### Target: 99.999% Uptime (5.26 minutes downtime/year)

To reduce downtime from 52 minutes to 5 minutes, you need **10x better availability**. This requires eliminating single points of failure.

#### Key Changes Required

**1. Database Layer** (Biggest Impact)
- **Change:** Multi-master replication (e.g., CockroachDB, Postgres with Patroni)
- **How it works:** Any master can accept writes; automatic conflict resolution
- **Failover time:** < 500ms (vs 15-30s for single master)
- **Downtime saved:** ~15 minutes/year
- **Trade-off:** Slightly higher write latency (100-200ms), complexity increases

**2. Distributed Tracing & Circuit Breakers**
- **Change:** Implement circuit breakers on all service calls
- **How it works:** Fail fast if a dependency is down; route to healthy instances
- **Recovery time:** < 1s (vs 30s for manual failover)
- **Downtime saved:** ~10 minutes/year
- **Trade-off:** Graceful degradation (return cached data) instead of full service

**3. Geo-Distributed Consensus**
- **Change:** Run services in 3+ geographic regions with local leaders
- **How it works:** Region 1 fails → Region 2 becomes leader in < 100ms
- **Downtime saved:** ~10 minutes/year
- **Trade-off:** Increased network latency (50-100ms cross-region)

**4. Immutable Infrastructure & Canary Deployments**
- **Change:** Blue-green deployments with automatic rollback
- **How it works:** Deploy to new servers; if errors spike, automatic rollback
- **Downtime saved:** Eliminate deployment failures (~5 minutes/year)
- **Trade-off:** 2x infrastructure cost during deployment

#### Summary Table: Target 99.999% State

| Component | Current Downtime | After Change | Mechanism |
|-----------|------------------|--------------|-----------|
| Database | 20 min | 2 min | Multi-master with < 500ms failover |
| Failover Speed | 10 min | 1 min | Circuit breakers + local caching |
| Deployment | 5 min | 0.5 min | Blue-green + automatic rollback |
| Network Issues | 10 min | 1 min | Mesh network with path diversity |
| Monitoring Detection | 7 min | 0.76 min | ML-based anomaly detection |
| **Total** | **52 min/year** | **5.26 min/year** | **99.999%** |

#### Golden Rule
**For 99.999% uptime:** Every component must have a backup with automatic failover < 1 second. No manual interventions allowed.

#### Leadership Insight
"We achieved 99.99% by eliminating single points of failure at the load balancer, app server, and cache levels. To reach 99.999%, we must eliminate the database failover delay through multi-master replication and implement circuit breakers so cascading failures don't happen. The trade-off is 30% higher infrastructure cost and increased operational complexity — we need to weigh this against business revenue loss per minute of downtime."

---

## 2. How did you achieve latency under 2 seconds?

### Current State: < 2s End-to-End Latency

#### Component Breakdown & Latency Contribution

**Network Latency (User → Server)**
- **Current:** 50-100ms (varies by geography)
- **Measurement:** DNS lookup (20ms) + TCP handshake (30ms) + request transmission (10-20ms)
- **Status:** ✓ Acceptable — using CDN for static assets

**Load Balancer**
- **Current:** 5ms
- **Status:** ✓ Minimal impact

**Application Processing**
- **Current:** 800-1000ms
- **Breakdown:**
  - API routing: 10ms
  - Database query: 400-600ms ⚠️ (LARGEST CONSUMER)
  - Cache lookup: 50-100ms
  - Business logic: 200-300ms
  - Response serialization: 50ms
- **Status:** ⚠️ Needs optimization

**Database Query Latency**
- **Current:** 400-600ms
- **Why slow:**
  - Complex joins (3-4 tables)
  - Full table scans on some queries
  - N+1 query problems
  - Indexes missing on frequently filtered columns
  - High query volume causing lock contention
- **Status:** 🔴 Main bottleneck

**Cache Layer (Redis)**
- **Current:** 5-10ms
- **Hit ratio:** 60-70%
- **Miss cost:** Database query (400-600ms)
- **Status:** ⚠️ Hit ratio needs improvement

**Response Transmission (Server → User)**
- **Current:** 30-50ms
- **Status:** ✓ Acceptable

#### Summary Table: Current < 2s Latency

| Component | Latency | % of Total | Status | Impact |
|-----------|---------|-----------|--------|--------|
| Network (user→server) | 75ms | 3.75% | ✓ OK | Low |
| Load balancer | 5ms | 0.25% | ✓ OK | Minimal |
| API routing | 10ms | 0.5% | ✓ OK | Minimal |
| Cache lookup | 50ms | 2.5% | ✓ OK | Low |
| Database query | 500ms | 25% | 🔴 SLOW | **Highest** |
| Business logic | 250ms | 12.5% | ⚠️ OK | Moderate |
| Response serialization | 50ms | 2.5% | ✓ OK | Low |
| Network (server→user) | 40ms | 2% | ✓ OK | Low |
| **Total** | **1950ms** | **100%** | **< 2s** | ✓ Meeting SLA |

---

### Optimizations to Achieve < 2s

#### 1. Database Query Optimization (400-600ms → 50-100ms)

**Fix 1: Add Missing Indexes**
```sql
-- Before: Full table scan on WHERE user_id = ? AND created_at > ?
-- Query time: 450ms

CREATE INDEX idx_users_created ON orders(user_id, created_at);

-- After: Index scan
-- Query time: 15ms
```
- **Downtime saved:** ~350ms per query
- **Implementation:** 1-2 days

**Fix 2: Denormalization for Hot Data**
- **Problem:** Joining `orders → users → profiles` tables (3 joins)
- **Solution:** Cache user profile in `orders` table or cache layer
- **Result:** 300ms → 50ms
- **Trade-off:** Slightly stale data (eventual consistency)

**Fix 3: Eliminate N+1 Queries**
```java
// Before: N+1 query problem
List<Order> orders = db.query("SELECT * FROM orders WHERE user_id = ?");
for (Order order : orders) {
  order.user = db.query("SELECT * FROM users WHERE id = ?", order.user_id); // N queries
}
// Total: 1 + N database queries = 500ms for 50 orders

// After: Single query with JOIN
String query = "SELECT o.*, u.* FROM orders o JOIN users u ON o.user_id = u.id WHERE o.user_id = ?";
List<OrderWithUser> results = db.query(query);
// Total: 1 database query = 50ms
```
- **Downtime saved:** ~400ms per batch operation

**Fix 4: Query Result Pagination**
- **Problem:** Fetching 10,000 rows from database takes 800ms
- **Solution:** Paginate results (fetch 20 rows at a time)
- **Result:** 800ms → 30ms (for first page)

#### 2. Cache Layer Optimization (60-70% hit ratio → 85-90%)

**Current State:**
- Hit ratio: 60-70%
- Miss rate: 30-40% (each miss = 500ms database query)

**Improvement:**
- **Pre-warm cache:** Load frequently accessed data on startup (user profiles, product catalogs)
- **Extend TTL:** Increase cache expiration from 5 min to 30 min for stable data
- **Predictive caching:** Pre-fetch related items (if user views product A, cache product B)
- **Result:** Hit ratio → 85-90%

**Impact Calculation:**
- Average request: 40% cache miss × 500ms database = 200ms latency added
- After optimization: 10% miss × 500ms = 50ms latency added
- **Savings: ~150ms per request**

#### 3. Application Code Optimization (250ms → 80ms)

**Problem 1: Synchronous API Calls**
- **Before:** Call payment API → wait 300ms for response → return
- **After:** Call payment API asynchronously; return immediately with pending status
- **Savings:** ~200ms

**Problem 2: Unnecessary Data Transforms**
- **Before:** Serialize full object with 100 fields (50ms)
- **After:** Return only 10 needed fields (5ms)
- **Savings:** ~45ms

**Problem 3: String Concatenation in Loops**
```java
// Before: String concatenation creates new object each time (O(n²))
String result = "";
for (int i = 0; i < 1000; i++) {
  result += dataList.get(i); // Slow: 50ms
}

// After: StringBuilder (O(n))
StringBuilder result = new StringBuilder();
for (int i = 0; i < 1000; i++) {
  result.append(dataList.get(i)); // Fast: 2ms
}
```
- **Savings:** ~48ms

#### 4. Caching Strategy by Data Type

| Data Type | Freshness Needed | TTL | Cache Layer |
|-----------|-----------------|-----|-------------|
| User profile | 30 min | 30m | Redis |
| Product catalog | 1 hour | 60m | Redis + CDN |
| Order status | Real-time | 5m | Redis + DB |
| Recommendations | 24 hours | 24h | Redis |
| Session data | 5 min | 5m | Redis |

#### 5. Content Delivery Optimization

**Current:** Static assets served from origin (50-100ms latency)
**After:** Static assets served from CDN (5-20ms latency)
- CSS/JS/images cached at edge locations worldwide
- **Savings:** ~50ms for static assets

---

### Summary: Achieving < 2s Latency

#### Before Optimization
```
Network:        75ms (3.75%)
Load balancer:  5ms (0.25%)
Cache:          50ms (2.5%)
Database:       500ms (25%) ← BOTTLENECK
Business logic: 250ms (12.5%)
Response:       40ms (2%)
Serialization:  50ms (2.5%)
─────────────────────────────
Total:          1970ms (99.99%)
```

#### After Optimization
```
Network:        75ms (4.1%)
Load balancer:  5ms (0.3%)
Cache:          20ms (1.1%) ← Hit ratio 90%
Database:       100ms (5.4%) ← Indexed queries
Business logic: 80ms (4.3%) ← Async operations
Response:       40ms (2.2%)
Serialization:  20ms (1.1%) ← Fewer fields
─────────────────────────────
Total:          340ms (18.5% of 2s budget)
```

#### Golden Rule
**For latency under 2s:** Database query time is your enemy. Optimize queries first (indexes + denormalization), then increase cache hit ratio to 85%+ to avoid hitting the database.

#### Leadership Insight
"We achieved < 2s latency by attacking the database bottleneck first: adding indexes reduced query time from 500ms to 100ms. Then we optimized caching to 90% hit ratio, which eliminates most database queries. The remaining latency (340ms) gives us a 5.9x safety margin. If traffic doubles, we can still stay under 2s. If we need to go below 1s, we'd need to invest in in-memory databases (MemSQL) or read-only replicas for hot data."

---

## How These Two Goals Interact

| Scenario | Impact on Uptime | Impact on Latency |
|----------|-----------------|------------------|
| Add database replica | ✓ Improves (faster failover) | ⚠️ Slightly hurts (replication lag) |
| Increase cache TTL | ⚠️ Hurts (stale data risk) | ✓ Improves (cache hits) |
| Deploy to more regions | ✓ Improves (redundancy) | ⚠️ Hurts (cross-region latency) |
| Circuit breakers | ✓ Improves (prevents cascade) | ⚠️ Hurts (fail-fast is slower) |
| Async operations | ✓ Improves (fewer blocking calls) | ✓ Improves (faster response) |

**Trade-off:** High availability often conflicts with low latency. Use circuit breakers wisely — they should fail fast but not affect normal-path latency.
