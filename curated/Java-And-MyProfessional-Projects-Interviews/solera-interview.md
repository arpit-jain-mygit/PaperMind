# Solera Director Interview — Technical Deep Dive

**Tailored for:** Steve Furminger (VP Software Engineering International) & Gerard Moubarak (Director Software Engineering Spain)  
**Interview Type:** 2nd Round — Technical Focus  
**Date:** Friday

---

## Quick Interviewer Context

| Name | Role | Key Focus |
|------|------|-----------|
| **Steve Furminger** | VP Software Engineering International | SAFe implementation, global delivery, team scaling (200+), centers of excellence |
| **Gerard Moubarak** | Director of Software Engineering (Spain) | Enterprise microservices, Spring Boot/Cloud, CI/CD/BDD, multinational leadership |

---

## Table of Contents

1. [Leadership & Global Operations](#leadership--global-operations)
2. [Enterprise Architecture & Microservices](#enterprise-architecture--microservices)
3. [Java/J2EE Technical Deep Dive](#javaj2ee-technical-deep-dive)
4. [Distributed Systems & Scalability](#distributed-systems--scalability)
5. [Cloud Architecture & DevOps](#cloud-architecture--devops)
6. [Performance Optimization & Database Design](#performance-optimization--database-design)
7. [Security & Compliance in Enterprise Systems](#security--compliance-in-enterprise-systems)
8. [Legacy Modernization & Migration Strategies](#legacy-modernization--migration-strategies)
9. [Situational & Problem-Solving](#situational--problem-solving)

---

## Leadership & Global Operations

### Q1: Managing Global Engineering Teams at Scale (Furminger)

**Question:** You'll be leading teams across EMEA, Iberia, and Latam. Walk us through your approach to scaling from 100 to 200+ engineers while maintaining code quality and delivery velocity.

**Answer:**

I've managed the transition from 100 to 200+ engineers. This isn't just adding more—it requires structural and cultural changes.

**Key scaling challenges:**

1. **Communication breakdown** — At 50 people, all-hands work. At 200, you need structured channels (wiki, async updates, recorded town halls)
2. **Decision bottlenecks** — Decision authority must be distributed. Clear RACI matrix prevents "waiting for the boss"
3. **Onboarding complexity** — Ad-hoc mentorship doesn't scale. Structured program with buddy system, internal documentation, clear runbooks
4. **Code quality degradation** — Without strong code review practices and CI/CD, merge conflicts and integration issues explode
5. **Career stagnation** — Engineers need to see advancement. Formal levels (IC and management track), promotion criteria, leadership development

**My approach:**

- **Introduce management layers:** Technical leads → staff engineers/managers → directors. Span of control: 6-8 direct reports max
- **Implement SAFe or similar:** Creates synchronization points (PI planning, sprints, demos). Solves dependency visibility
- **Invest heavily in CI/CD:** Non-negotiable at 200+. Automated testing, fast feedback loops, deployments shouldn't be events
- **Build infrastructure team:** Dedicated team managing CI/CD, cloud infrastructure, monitoring—lifts load from product teams
- **Formalize career paths:** Staff engineer track for deep technical expertise; manager track for people leadership. Both are equal in status/compensation
- **Centers of excellence:** Java/Spring experts, security champions, performance specialists—available to coach teams
- **Metrics dashboard:** Track velocity, cycle time, deployment frequency, MTTR, bug escape rate—share publicly every week

**Result at my previous org:** Scaled to 250 engineers across 4 regions. Velocity increased 60%, incident rates dropped 40%, attrition stayed at 8%. Teams shipped independently without constant escalations.

---

### Q2: SAFe Implementation & Transformation at Enterprise Scale (Furminger)

**Question:** You've led SAFe transformations. What's your strategy for implementing SAFe across Solera's global engineering organization? How do you avoid "ceremony theater"?

**Answer:**

I've implemented SAFe twice. The first time was a disaster (we made it waterfall-with-sprints). The second time worked because we aligned SAFe with actual business needs.

**Prerequisites for SAFe success:**

Before rolling out SAFe, you MUST have:
- Strong CI/CD practices (you can't do scaled agile without fast deployments)
- Automated testing (at least 50% unit + integration test coverage)
- Clear product roadmap (SAFe amplifies bad planning)
- Motivated engineering leadership (SAFe is a change management effort, not just process)

**My implementation approach:**

**Phase 1: Foundation (Weeks 1-8)**
- Assess current state: How many ARTs (Agile Release Trains)? Do we have dependencies? What's our deployment frequency?
- Train core team (POs, ScrumMasters, architects) on SAFe 6.0 concepts
- Design the target state: Which business domains = which ARTs? Identify dependencies
- Start with ONE pilot ART (8-12 teams, 60-80 engineers)

**Phase 2: Pilot ART (Weeks 9-20)**
- Run 2-3 PIs in pilot ART
- Measure: velocity, deployment frequency, bug escape rate, PI predictability (hitting the plan)
- Iterate on ceremonies (PI planning often takes 2-3 hours too long the first time)
- Build institutional knowledge (create playbooks, document decisions)

**Phase 3: Rollout (Weeks 21+)**
- Start 2nd ART with lessons from Pilot
- Continue for 3-4 PIs before assessing readiness for 3rd ART
- Scale from there

**Avoiding "ceremony theater":**

**Anti-patterns I've seen:**
- SAFe ceremonies become pure status updates (death by PowerPoint)
- Teams treat SAFe as waterfall (planning = all design at the beginning)
- No real dependency resolution—PI planning becomes scheduling, not problem-solving
- Metrics obsession without understanding what they mean

**My fixes:**
1. **PI Planning is a working session, not a status meeting**
   - Bring architects, POs, and tech leads into the room (or virtual breakout)
   - Have them design the architecture for the 2-week sprint together
   - Resolve dependencies LIVE (if two teams need the same API, design it together)

2. **Metrics that matter:**
   - PI predictability: Did we hit what we planned? (Target: 85%+)
   - Deployment frequency: How often do we ship? (Target: daily or every sprint)
   - Lead time for changes: Commit to production (Target: < 1 hour)
   - MTTR: Recovery time (Target: < 30 min for critical services)
   - Bug escape rate: Production bugs vs. QA bugs (Target: < 10% escape)

3. **Autonomy within alignment:**
   - Teams own their sprint planning and implementation
   - Shared OKRs (business outcomes) but teams decide HOW to achieve them
   - Architecture guidelines (microservices, Spring Boot standards, API contracts) but not micro-management

**Result at my previous org:**
- Went from quarterly releases to bi-weekly deployments
- PI predictability: 78% → 92%
- Incident rate dropped 45%
- Engineering morale improved because teams had autonomy within clear boundaries

**For Solera specifically:** Given you have 200+ engineers across 3+ regions, I'd recommend 3-4 ARTs organized by business domain, not geography. Each ART would have a mix of engineers from different regions (e.g., Billing ART = 2 teams in Madrid + 1 in London + 1 in São Paulo). This forces knowledge sharing and breaks down silos.

---

## Enterprise Architecture & Microservices

### Q3: Microservices Design & Domain-Driven Design (Moubarak)

**Question:** Walk us through your experience decomposing a monolith into microservices using DDD. How do you identify service boundaries? What are the trade-offs?

**Answer:**

I've led 2 monolith-to-microservices migrations. Both taught me that microservices aren't a technical problem—they're a team structure problem.

**Identifying service boundaries (DDD approach):**

**Step 1: Map the business domain**
- Identify bounded contexts (DDD term): areas of the business where different terminology/rules apply
- Example: A billing system has Pricing, Reconciliation, Reporting, Invoicing, Collections contexts
- Each bounded context is a candidate microservice

**Step 2: Analyze data flow and dependencies**
- What data flows between contexts?
- Which contexts change together (high cohesion)?
- Which are independent (low coupling)?
- Example: Pricing and Invoicing are tightly coupled (price changes → invoice recalc). Reporting is loosely coupled (eventually consistent is fine)

**Step 3: Define service boundaries**
- Each service owns its data (no shared database)
- Services communicate via APIs or events
- Clear responsibility boundaries: Pricing service doesn't know about Invoicing; Invoicing calls Pricing API for current rates

**Real example from my last role:**

Started with a monolithic billing system (150k LOC, 8-person team):

```
Monolith
├── Pricing Engine
├── Invoicing
├── Payment Processing
├── Reconciliation
├── Reporting
└── Notifications
```

Decomposed into 5 microservices:

```
Pricing Service (Spring Boot)
  - Database: pricing_db (PostgreSQL)
  - APIs: GET /prices, POST /calculate-invoice-total
  - Cache: Redis (for high-traffic price lookups)

Invoicing Service (Spring Boot)
  - Database: invoicing_db (PostgreSQL)
  - Calls: Pricing Service API for rates
  - Events: Publishes "InvoiceCreated" event
  - Cache: Redis for invoice templates

Payment Service (Spring Boot)
  - Integrates: Stripe, ACH, wire transfers
  - Database: payment_db (separate for PCI compliance)
  - Events: Publishes "PaymentProcessed"

Reconciliation Service (Batch/Spark)
  - Consumes events from Payment + Invoicing services
  - Runs nightly reconciliation
  - Database: reconciliation_db (OLAP optimized)

Reporting Service (Java/Elasticsearch)
  - Consumes: Invoicing, Payment, Reconciliation events
  - Database: Elasticsearch (for complex queries)
  - APIs: GET /reports/revenue, /reports/collection-rate
```

**Trade-offs I've made:**

| Aspect | Monolith | Microservices |
|--------|----------|---------------|
| **Consistency** | Strong (ACID) | Eventual (Event-driven) |
| **Debugging** | Single codebase, easy to trace | Distributed tracing needed (Jaeger, Datadog) |
| **Deployment** | Single release, coordinated | Independent deployments (faster, more complex) |
| **Data** | Single database, easy joins | No joins across services, requires API calls |
| **Testing** | Unit + integration, contained | Contract tests, end-to-end tests challenging |
| **Operational complexity** | Simple | Multiple deployments, more monitoring |
| **Team structure** | Must be coordinated | Each team owns a service (Conway's Law) |

**Key learnings:**

1. **Data consistency is hard:** You lose ACID transactions. Saga pattern helps (orchestrate compensation transactions across services) but it's complex.

2. **Network is unreliable:** Every service call can fail. Must implement:
   - Circuit breakers (Hystrix, Resilience4j)
   - Retries with exponential backoff
   - Timeouts (never retry indefinitely)
   - Fallbacks

3. **Event-driven architecture requires new skills:** Topics, partitions, ordering, idempotency. Kafka has a learning curve.

4. **Shared libraries are good:** Common patterns (logging, metrics, error handling) should be in a shared Spring Boot starter to prevent duplication.

5. **API contracts are critical:** Use OpenAPI (Swagger) to document APIs. Contract tests ensure services don't break each other's expectations.

**When NOT to use microservices:**

- Small team (< 10 engineers): Monolith is faster to market
- Tightly coupled domain: If everything changes together, microservices don't help
- No operational maturity: If you don't have CI/CD and monitoring, don't attempt microservices
- One database: If you're not prepared to replicate data across services, you can't decompose

---

### Q4: Spring Boot & Enterprise Java Architecture (Moubarak)

**Question:** Tell us about designing a Spring Boot microservice for high-traffic, low-latency requirements. What patterns do you use for database optimization, caching, and async processing?

**Answer:**

I've built Spring Boot services handling millions of requests/day with sub-100ms p99 latency. This requires careful architecture at every layer.

**Service architecture for high-traffic:**

```
Load Balancer (AWS ELB / Azure LB)
  ↓
API Gateway (Spring Cloud Gateway / Kong)
  - Rate limiting, authentication, routing
  ↓
Spring Boot Service Cluster (3-5 instances)
  - @RestController endpoints
  - Service layer (business logic)
  - Data access layer (Repository pattern)
  ↓
Cache Layer (Redis)
  - Session cache, computed results
  ↓
Database (PostgreSQL / Oracle)
  - Optimized schema, indexes
  ↓
Message Queue (Kafka / RabbitMQ)
  - Async processing, events
```

**Database optimization patterns:**

1. **Connection pooling (HikariCP - built into Spring Boot)**
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20
         minimum-idle: 5
         idle-timeout: 600000
         max-lifetime: 1800000
   ```
   - HikariCP is 2-3x faster than other pools
   - Right-size pool: 2x number of CPU cores for OLTP
   - Monitor pool exhaustion (causes cascading failures)

2. **Query optimization**
   - Use `@Query` with SELECT * only what you need, not whole entities
   - Eager load only what's needed (N+1 problem)
   ```java
   @Query("SELECT new com.example.PriceDTO(p.id, p.amount) FROM Price p WHERE p.active = true")
   List<PriceDTO> getActivePrices();
   ```
   - Pagination for large result sets (LIMIT/OFFSET or seek-based)

3. **Indexing strategy**
   - Index foreign keys (for JOINs)
   - Index frequently filtered columns (WHERE clauses)
   - Composite indexes for multi-column filters
   - Avoid indexing low-cardinality columns (gender, status with < 10 values)

4. **Schema design**
   - Denormalization for read-heavy scenarios (pre-compute expensive joins)
   - Partitioning for large tables (by date, region, customer)
   - Archive old data (move to cold storage)

**Caching strategy (Redis):**

```java
@Service
public class PricingService {
  
  @Cacheable(value = "prices", key = "#productId")
  public Price getPrice(String productId) {
    return pricingRepository.findById(productId);
  }
  
  @CacheEvict(value = "prices", key = "#productId")
  public void updatePrice(String productId, BigDecimal newPrice) {
    pricingRepository.save(new Price(productId, newPrice));
  }
}
```

**Cache patterns:**
- **Cache-Aside:** App checks cache; if miss, fetch from DB and update cache
- **Write-Through:** Write to cache and DB together (safer but slower)
- **Write-Behind:** Write to cache immediately, async write to DB (fast but risky)

**My approach:** Cache-Aside with 1-hour TTL for pricing data, 5-min for user sessions. Use `@CacheEvict` to invalidate on updates.

**Async processing (non-blocking):**

For operations that don't need immediate response:

```java
@Service
public class InvoiceService {
  
  @Async
  public CompletableFuture<Invoice> generateInvoiceAsync(String customerId) {
    // Expensive operation (DB writes, PDF generation)
    Invoice invoice = createInvoice(customerId);
    emailService.sendAsync(invoice);
    return CompletableFuture.completedFuture(invoice);
  }
}

// In controller:
@PostMapping("/invoices")
public ResponseEntity<?> createInvoice(@RequestBody InvoiceRequest req) {
  invoiceService.generateInvoiceAsync(req.getCustomerId());
  return ResponseEntity.accepted().build(); // 202 Accepted
}
```

**For I/O-bound operations (external APIs):**

```java
@Service
public class PaymentService {
  
  private final WebClient webClient; // Non-blocking HTTP client
  
  public Mono<PaymentResult> processPaymentAsync(Payment payment) {
    return webClient.post()
      .uri("https://payment-gateway/charge")
      .bodyValue(payment)
      .retrieve()
      .bodyToMono(PaymentResult.class)
      .timeout(Duration.ofSeconds(5))
      .doOnError(e -> logger.error("Payment failed", e));
  }
}
```

**Concurrency & threading:**

- Default: Tomcat with 200 threads, configurable
- For high concurrency: Consider reactive (Project Reactor, WebFlux) but adds complexity
- For most cases: Traditional threading + async is sufficient

**Monitoring & observability:**

```java
@Service
public class MetricService {
  
  private final MeterRegistry meterRegistry;
  
  public void recordLatency(String operation, long durationMs) {
    meterRegistry.timer("operation.latency", "op", operation).record(durationMs, TimeUnit.MILLISECONDS);
  }
}
```

Use Prometheus + Grafana to visualize:
- Request latency (p50, p95, p99)
- Throughput (requests/sec)
- Error rate (4xx, 5xx)
- JVM metrics (heap, GC pauses)

---

## Java/J2EE Technical Deep Dive

### Q5: Advanced Java/J2EE Architecture & Design Patterns

**Question:** Describe your experience with enterprise Java patterns. Walk us through a complex system you've designed and the patterns you used (Factory, Strategy, Observer, etc.). Why those specific choices?

**Answer:**

I've built large enterprise systems using Java. The trick is knowing which patterns solve real problems and which are over-engineering.

**Real example: A billing & invoicing platform (150k LOC, 8-person team)**

**Patterns I used and why:**

1. **Strategy Pattern: Payment Methods**
   ```java
   public interface PaymentStrategy {
     void charge(Money amount) throws PaymentException;
   }
   
   public class CreditCardPayment implements PaymentStrategy {
     public void charge(Money amount) throws PaymentException {
       // Call Stripe API
     }
   }
   
   public class ACHPayment implements PaymentStrategy {
     public void charge(Money amount) throws PaymentException {
       // Call ACH service
     }
   }
   
   public class PaymentProcessor {
     private PaymentStrategy strategy;
     public void process(Payment payment) {
       strategy.charge(payment.getAmount());
     }
   }
   ```
   **Why:** Different payment methods have completely different APIs (Stripe, ACH, wire). Strategy pattern lets us add new methods without modifying existing code (Open/Closed principle).

2. **Factory Pattern: Pricing Rules**
   ```java
   public class PricingRuleFactory {
     public static PricingRule createRule(RuleType type, Map<String, Object> config) {
       switch(type) {
         case FLAT_RATE:
           return new FlatRateRule(config.get("rate"));
         case TIERED:
           return new TieredRule(config.get("tiers"));
         case USAGE_BASED:
           return new UsageBasedRule(config.get("unitRate"));
         default:
           throw new IllegalArgumentException("Unknown rule type");
       }
     }
   }
   ```
   **Why:** Pricing rules changed frequently (business requirement). Factory centralizes the creation logic, making it easy to add new rule types without touching existing code.

3. **Builder Pattern: Complex Objects**
   ```java
   public class Invoice {
     private String customerId;
     private List<LineItem> items;
     private TaxCalculator taxCalc;
     private PaymentTerms terms;
     
     public static class Builder {
       public Builder withCustomer(String id) { ... }
       public Builder withLineItems(List<LineItem> items) { ... }
       public Invoice build() { ... }
     }
   }
   ```
   **Why:** Invoices have optional fields (tax calc, payment terms). Builder makes construction flexible and readable.

4. **Observer Pattern: Event-Driven Updates**
   ```java
   public interface InvoiceObserver {
     void onInvoiceCreated(Invoice invoice);
     void onInvoicePaid(Invoice invoice);
   }
   
   public class Invoice {
     private List<InvoiceObserver> observers = new ArrayList<>();
     
     public void markAsPaid() {
       this.status = PAID;
       notifyObservers(); // Notify email service, analytics, etc.
     }
   }
   ```
   **Why:** When an invoice is paid, multiple things need to happen (send email, update analytics, trigger commission calc). Observer decouples Invoice from these dependencies.

5. **Decorator Pattern: Audit Logging**
   ```java
   public class AuditDecorator implements InvoiceRepository {
     private final InvoiceRepository delegate;
     
     public void save(Invoice inv) {
       auditLog.record("SAVE", inv.getId(), getCurrentUser());
       delegate.save(inv);
     }
   }
   ```
   **Why:** Added auditing requirement late in the project. Decorator wraps existing repository without modifying it.

6. **Repository Pattern: Data Access**
   ```java
   public interface InvoiceRepository {
     Invoice findById(String id);
     List<Invoice> findByCustomer(String customerId);
     void save(Invoice invoice);
   }
   
   @Repository
   public class JpaInvoiceRepository implements InvoiceRepository {
     @Autowired
     private JpaInvoiceDao dao;
     
     public Invoice findById(String id) {
       return dao.findById(id).orElse(null);
     }
   }
   ```
   **Why:** Repository abstracts the data access layer. Easy to swap JPA for NoSQL or add caching without touching business logic.

**Patterns I avoided:**

- **Singleton:** Used sparingly. Spring `@Singleton` bean is better (Spring manages lifecycle)
- **Abstract Factory:** Overkill for most scenarios. Simple Factory is enough
- **Prototype:** Rarely needed in Java; use Builder instead

---

### Q6: Transaction Management & Consistency in Distributed Systems (Moubarak)

**Question:** Spring provides @Transactional. But in a microservices world, you can't use ACID across services. How do you handle transactions? Walk us through a payment failure scenario.

**Answer:**

This is the most common architectural mistake in microservices: trying to maintain strong ACID consistency across services.

**Local transactions (within a single service):**

```java
@Service
public class InvoiceService {
  
  @Transactional
  public Invoice createInvoice(String customerId, List<LineItem> items) {
    // All-or-nothing: either the whole invoice is created or none of it
    Invoice invoice = new Invoice(customerId, items);
    invoiceRepository.save(invoice);
    
    // If inventoryService throws exception, invoice save is rolled back
    inventoryService.decreaseStock(items);
    
    return invoice;
  }
}
```

Spring's @Transactional ensures ACID for database operations. If anything fails, rollback happens.

**Distributed transactions (across microservices):**

This is where it gets hard. You can't use ACID. You must use eventual consistency.

**Scenario: Payment processing workflow**

```
Order Service → Payment Service → Invoicing Service → Accounting Service
```

If Payment Service fails after Order was created, we have an orphaned order. Solution: **Saga Pattern**.

**Two approaches:**

**1. Choreography (Event-driven):**

```
Order Service creates order → publishes "OrderCreated" event
  ↓
Payment Service listens to "OrderCreated" → attempts charge
  - If success: publishes "PaymentProcessed" event
  - If failure: publishes "PaymentFailed" event
  ↓
Invoicing Service listens to "PaymentProcessed" → creates invoice
  - If success: publishes "InvoiceCreated" event
  - If failure: publishes "InvoicingFailed" → triggers compensation
  ↓
Order Service listens to "PaymentFailed" → marks order as failed, rollback
```

**Implementation (using Spring Events + Kafka):**

```java
// Order Service
@Service
public class OrderService {
  
  @Autowired
  private KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
  
  public Order createOrder(OrderRequest request) {
    Order order = new Order(request);
    orderRepository.save(order);
    
    // Publish event (async)
    kafkaTemplate.send("order-events", new OrderCreatedEvent(order));
    
    return order;
  }
}

// Payment Service
@Service
public class PaymentService {
  
  @KafkaListener(topics = "order-events")
  public void handleOrderCreated(OrderCreatedEvent event) {
    try {
      Payment payment = chargePaymentGateway(event.getOrder());
      kafkaTemplate.send("payment-events", new PaymentProcessedEvent(payment));
    } catch (PaymentException e) {
      kafkaTemplate.send("payment-events", new PaymentFailedEvent(event.getOrderId(), e.getMessage()));
    }
  }
}

// Order Service (compensation)
@Service
public class OrderService {
  
  @KafkaListener(topics = "payment-events")
  public void handlePaymentFailed(PaymentFailedEvent event) {
    Order order = orderRepository.findById(event.getOrderId());
    order.setStatus(FAILED);
    orderRepository.save(order);
    // Send notification to customer
  }
}
```

**2. Orchestration (Central coordinator):**

```
Order Service creates order and publishes event
  ↓
Saga Orchestrator (separate service) listens
  - Calls Payment Service → if success, continue
  - Calls Invoicing Service → if success, continue
  - If any failure, calls compensation (rollback)
  ↓
Compensation: Reverse each step
  - Mark invoice as cancelled
  - Refund payment
  - Mark order as failed
```

**Implementation:**

```java
@Service
public class OrderSagaOrchestrator {
  
  public void executeOrderSaga(Order order) {
    try {
      // Step 1: Charge payment
      PaymentResult payment = paymentService.charge(order.getTotalAmount());
      
      // Step 2: Create invoice
      Invoice invoice = invoicingService.createInvoice(order);
      
      // Step 3: Update accounting
      accountingService.recordTransaction(payment, invoice);
      
      // Success
      orderRepository.markAsProcessed(order);
      
    } catch (PaymentException e) {
      // Compensation: Refund
      paymentService.refund(order.getPaymentId());
      orderRepository.markAsFailed(order);
    } catch (InvoicingException e) {
      // Compensation: Refund + reverse invoice
      paymentService.refund(order.getPaymentId());
      invoicingService.cancel(invoice);
      orderRepository.markAsFailed(order);
    }
  }
}
```

**Pros/Cons:**

| Approach | Pros | Cons |
|----------|------|------|
| **Choreography** | Decoupled (event-driven), scalable | Hard to debug, cyclic dependencies possible |
| **Orchestration** | Clear flow, easy to understand | Centralized bottleneck, single point of failure |

**My preference:** Start with choreography (if you can debug it). Fall back to orchestration if you have complex workflows.

**Key principles:**

1. **Idempotency:** Operations must be idempotent. If a service retries a call, it shouldn't charge twice.
   ```java
   @PostMapping("/payments")
   public ResponseEntity<?> charge(@RequestBody ChargeRequest req) {
     String idempotencyKey = req.getIdempotencyKey(); // Client-provided
     
     // Check if we've already processed this request
     if (idempotencyKeyStore.exists(idempotencyKey)) {
       return getCachedResult(idempotencyKey);
     }
     
     // Process charge
     PaymentResult result = processCharge(req);
     idempotencyKeyStore.save(idempotencyKey, result);
     
     return ResponseEntity.ok(result);
   }
   ```

2. **Saga state machine:** Track the saga state so you know where a failed saga stopped.
   ```java
   enum SagaState {
     CREATED, PAYMENT_PENDING, PAYMENT_COMPLETED, 
     INVOICING_PENDING, INVOICING_COMPLETED, COMPLETE
   }
   ```

3. **Timeouts:** Every service call must have a timeout. Otherwise, a slow service blocks everything.
   ```java
   paymentService.charge(payment)
     .timeout(Duration.ofSeconds(5))
     .onTimeout(() -> throwPaymentException("Payment gateway timeout"));
   ```

---

## Distributed Systems & Scalability

### Q7: Handling High Concurrency & Eventual Consistency (Moubarak)

**Question:** Your Solera system handles millions of transactions. How do you design for high concurrency? What trade-offs do you make between consistency and availability?

**Answer:**

At scale, you must make hard choices between consistency, availability, and partition tolerance (CAP theorem). Most real systems choose availability + partition tolerance (eventual consistency).

**Design principles:**

1. **Async communication where possible**
   - Synchronous calls are blocking. At scale, blocking = cascading failures
   - Use message queues (Kafka, RabbitMQ) for async processing

2. **Circuit breakers prevent cascades**
   ```java
   @CircuitBreaker(failureThreshold = 5, delay = 5000) // 5 failures → open circuit for 5 sec
   public PaymentResult chargePaymentGateway(Payment payment) {
     return externalGateway.charge(payment);
   }
   ```
   If payment gateway is slow/down, stop calling it. Respond with a cached result or error immediately.

3. **Bulkheads isolate failures**
   ```yaml
   spring:
     cloud:
       circuitbreaker:
         resilience4j:
           instances:
             payment-service:
               max-concurrent-calls: 100  # Limit concurrent calls to payment service
               wait-duration-in-open-state: 5000
   ```
   Even if payment service gets hammered, it only consumes 100 threads, not all 200.

4. **Caching reduces database load**
   - Cache rates: Redis with 1-hour TTL
   - Cache user profiles: Redis with 30-min TTL
   - Invalidate on updates

5. **Read replicas for reporting**
   - Don't do complex reporting queries on the primary database
   - Use read replicas (PostgreSQL streaming replication)
   - Or use a separate analytics database (Elasticsearch, DuckDB) fed by events

**Scalability patterns:**

**1. Vertical scaling (more powerful machine)**
   - Cheap (until it's not)
   - Single point of failure
   - Limited by hardware

**2. Horizontal scaling (more machines)**
   - Stateless services (scale easily)
   - Sticky sessions/state is hard to scale
   - Load balancing (round-robin, least-connections)

**My approach for Solera:**

Stateless services + load balancer + database replication:

```
Load Balancer
├─ Invoice Service (Instance 1)
├─ Invoice Service (Instance 2)
├─ Invoice Service (Instance 3)
  ↓
Database Cluster
├─ Primary (writes)
├─ Read Replica 1 (reads)
├─ Read Replica 2 (reads)
  ↓
Cache (Redis Cluster)
  ↓
Message Queue (Kafka)
  ├─ Topic: invoice-events (3 partitions)
  ├─ Topic: payment-events (3 partitions)
```

**Partitioning for scale:**

If a table gets huge (100M+ rows), partition it:

```sql
-- Partition invoices by customer (sharding)
CREATE TABLE invoices_customer_001 PARTITION OF invoices
  FOR VALUES FROM (1) TO (1000001);

CREATE TABLE invoices_customer_002 PARTITION OF invoices
  FOR VALUES FROM (1000001) TO (2000001);
```

Each partition can be on a different server. Queries are faster (smaller tables = smaller indexes).

---

## Cloud Architecture & DevOps

### Q8: CI/CD Pipeline Design for Enterprise Systems (Both)

**Question:** Walk us through the CI/CD pipeline you'd design for Solera. From code commit to production, what stages would you have? How do you ensure quality and prevent broken deployments?

**Answer:**

I've designed CI/CD for systems processing 10M+ daily transactions. The pipeline must be fast (feedback in < 15 min) while catching bugs before production.

**My typical pipeline:**

```
Developer commits code to main branch
  ↓
1. TRIGGER (GitHub webhook → Jenkins/GitLab CI)
  ↓
2. BUILD (Maven/Gradle)
   - Compile code
   - Unit tests (JUnit/TestNG)
   - Code quality (SonarQube)
   - Build Docker image
  ↓ (if unit tests pass)
  ↓
3. PUSH TO REGISTRY
   - Docker image pushed to ECR/ACR
   - Tagged with commit SHA
  ↓
4. DEPLOY TO STAGING
   - Deploy new image to staging environment
   - Run integration tests against staging
   - Run smoke tests (critical path scenarios)
   - Run contract tests (verify API contracts with other services)
   ↓ (if all tests pass)
  ↓
5. DEPLOY TO PRODUCTION
   - Canary deployment: Route 10% of traffic to new version
   - Monitor metrics (error rate, latency, JVM heap)
   - If metrics degrade, automatic rollback
   - Gradually increase to 100% traffic
  ↓
6. MONITORING & ALERTS
   - Prometheus: Scrape metrics every 15 sec
   - Grafana: Visualize metrics
   - Alerting: If error rate > 1%, page on-call engineer
```

**Implementation (using GitHub Actions + Kubernetes + ArgoCD):**

```yaml
# .github/workflows/build.yml
name: Build and Deploy

on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      # Step 1: Build
      - uses: actions/checkout@v2
      - name: Build with Maven
        run: mvn clean package
      
      # Step 2: Test
      - name: Run unit tests
        run: mvn test
      
      - name: Run integration tests (against Docker containers)
        run: |
          docker-compose -f docker-compose.test.yml up -d
          mvn verify
          docker-compose down
      
      # Step 3: Code quality
      - name: SonarQube scan
        run: mvn sonar:sonar -Dsonar.projectKey=${{ secrets.SONAR_PROJECT_KEY }}
      
      # Step 4: Build and push Docker image
      - name: Build Docker image
        run: docker build -t ${{ secrets.ECR_REGISTRY }}/invoice-service:${{ github.sha }} .
      
      - name: Push to ECR
        run: |
          aws ecr get-login-password | docker login --username AWS --password-stdin ${{ secrets.ECR_REGISTRY }}
          docker push ${{ secrets.ECR_REGISTRY }}/invoice-service:${{ github.sha }}
      
      # Step 5: Deploy to staging
      - name: Deploy to staging
        run: |
          kubectl set image deployment/invoice-service-staging invoice-service=${{ secrets.ECR_REGISTRY }}/invoice-service:${{ github.sha }} --record
          kubectl rollout status deployment/invoice-service-staging
      
      # Step 6: Smoke tests in staging
      - name: Run smoke tests in staging
        run: |
          curl -f http://invoice-service-staging:8080/health || exit 1
          curl -f -X POST http://invoice-service-staging:8080/invoices \
            -H "Content-Type: application/json" \
            -d '{"customerId":"test-123","items":[{"amount":10}]}' || exit 1
      
      # Step 7: Deploy to production (canary)
      - name: Deploy to production (10% traffic)
        run: |
          kubectl set image deployment/invoice-service invoice-service=${{ secrets.ECR_REGISTRY }}/invoice-service:${{ github.sha }} --record
          kubectl patch service invoice-service -p '{"spec":{"selector":{"version":"canary"}}}'
          # Wait 5 minutes, monitor metrics
          sleep 300
          # If error rate < 1%, continue; else rollback
          ERROR_RATE=$(prometheus_query "rate(http_requests_total{status=~'5..'}[5m])")
          if [ "$ERROR_RATE" -gt "0.01" ]; then
            kubectl rollout undo deployment/invoice-service
            exit 1
          fi
      
      # Step 8: Complete deployment to 100%
      - name: Deploy to production (100% traffic)
        run: |
          kubectl patch service invoice-service -p '{"spec":{"selector":{"version":"stable"}}}'
```

**Quality gates I enforce:**

1. **Code coverage:** > 75% unit test coverage (enforced by SonarQube)
2. **Integration tests:** All major flows must have integration tests
3. **Contract tests:** Services must validate API contracts with partners
4. **Security scans:** OWASP dependency check (no CVEs with CVSS > 7)
5. **Performance tests:** P99 latency < 200ms for all endpoints

**Deployment strategies:**

| Strategy | Pros | Cons |
|----------|------|------|
| **Blue/Green** | Instant rollback | Double infrastructure cost |
| **Canary** | Low risk, detect issues early | Slow rollout (5-30 min) |
| **Rolling** | Gradual rollout, low cost | Complex to rollback |

**My choice:** Canary for all services. 10% traffic for 5 min, then 50%, then 100%. If metrics degrade, automatic rollback.

**Monitoring critical metrics:**

```yaml
# Alert if error rate > 1%
alert:
  name: HighErrorRate
  expr: rate(http_requests_total{status=~'5..'}[5m]) > 0.01
  for: 2m
  action: page-on-call

# Alert if P99 latency > 200ms
alert:
  name: HighLatency
  expr: histogram_quantile(0.99, http_request_duration_seconds) > 0.2
  for: 5m
  action: page-on-call
```

---

## Performance Optimization & Database Design

### Q9: Database Design for High-Throughput Systems (Moubarak)

**Question:** Design a PostgreSQL schema for a billing system processing 10K transactions/sec. What indexing strategy would you use? How would you handle growth?

**Answer:**

10K transactions/sec is significant. At this scale, every decision matters: query efficiency, indexing, partitioning.

**Schema design:**

```sql
-- Core tables
CREATE TABLE customers (
  id BIGINT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) UNIQUE,
  country_code CHAR(2), -- For compliance/tax
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE invoices (
  id BIGINT PRIMARY KEY,
  customer_id BIGINT NOT NULL REFERENCES customers(id),
  invoice_number VARCHAR(50) NOT NULL,
  status VARCHAR(20) NOT NULL, -- DRAFT, SENT, PAID, FAILED, CANCELLED
  total_amount DECIMAL(12, 2),
  created_at TIMESTAMP DEFAULT NOW(),
  due_at TIMESTAMP,
  paid_at TIMESTAMP,
  UNIQUE(customer_id, invoice_number)
);

CREATE TABLE invoice_items (
  id BIGINT PRIMARY KEY,
  invoice_id BIGINT NOT NULL REFERENCES invoices(id),
  description VARCHAR(255),
  quantity INT,
  unit_price DECIMAL(12, 2),
  line_total DECIMAL(12, 2)
);

CREATE TABLE payments (
  id BIGINT PRIMARY KEY,
  invoice_id BIGINT NOT NULL REFERENCES invoices(id),
  payment_method VARCHAR(50), -- CREDIT_CARD, ACH, WIRE
  amount DECIMAL(12, 2),
  status VARCHAR(20), -- PENDING, COMPLETED, FAILED
  gateway_transaction_id VARCHAR(255),
  created_at TIMESTAMP DEFAULT NOW(),
  processed_at TIMESTAMP
);
```

**Indexing strategy:**

```sql
-- Foreign key indexes (required for fast JOINs)
CREATE INDEX idx_invoices_customer_id ON invoices(customer_id);
CREATE INDEX idx_invoice_items_invoice_id ON invoice_items(invoice_id);
CREATE INDEX idx_payments_invoice_id ON payments(invoice_id);

-- Frequently filtered columns
CREATE INDEX idx_invoices_status ON invoices(status); -- WHERE status = 'PAID'
CREATE INDEX idx_invoices_created_at ON invoices(created_at); -- WHERE created_at > ?
CREATE INDEX idx_payments_status ON payments(status);

-- Composite indexes for common queries
CREATE INDEX idx_invoices_customer_status ON invoices(customer_id, status);
  -- Supports: WHERE customer_id = ? AND status = 'PAID'

-- Partial index (low-cardinality, frequently queried)
CREATE INDEX idx_unpaid_invoices ON invoices(customer_id, created_at)
  WHERE status != 'PAID';
  -- Smaller index, faster queries for unpaid invoices

-- Covering index (all needed columns in index, avoid table lookup)
CREATE INDEX idx_invoices_covering ON invoices(customer_id, status)
  INCLUDE (total_amount, due_at);
  -- Queries can be answered entirely from index
```

**Partitioning for scale:**

At 10K transactions/sec, a single invoices table could become a bottleneck. Partition by date:

```sql
CREATE TABLE invoices (
  id BIGINT,
  customer_id BIGINT,
  status VARCHAR(20),
  created_at TIMESTAMP,
  ...
) PARTITION BY RANGE (EXTRACT(YEAR FROM created_at) * 100 + EXTRACT(MONTH FROM created_at));

CREATE TABLE invoices_202401 PARTITION OF invoices
  FOR VALUES FROM (202401) TO (202402);

CREATE TABLE invoices_202402 PARTITION OF invoices
  FOR VALUES FROM (202402) TO (202403);

-- Each partition has its own indexes
CREATE INDEX idx_invoices_202401_status ON invoices_202401(status);
```

**Query optimization:**

```sql
-- BAD: Full table scan
SELECT * FROM invoices WHERE status = 'PAID' LIMIT 10;

-- GOOD: Use index, select only needed columns
SELECT id, customer_id, total_amount 
FROM invoices 
WHERE status = 'PAID' 
ORDER BY created_at DESC 
LIMIT 10;

-- BAD: Function on indexed column (index not used)
SELECT * FROM invoices WHERE DATE(created_at) = '2024-01-15';

-- GOOD: Range query (index used)
SELECT * FROM invoices WHERE created_at >= '2024-01-15' AND created_at < '2024-01-16';

-- BAD: SELECT * (unnecessary columns, slow network transfer)
SELECT * FROM invoices WHERE customer_id = 123;

-- GOOD: Select only needed columns
SELECT id, status, total_amount FROM invoices WHERE customer_id = 123;
```

**Connection pooling and query caching:**

```yaml
# Spring Boot configuration
spring:
  datasource:
    url: jdbc:postgresql://db.prod.internal:5432/billing
    hikari:
      maximum-pool-size: 30  # 2x CPU cores for OLTP
      minimum-idle: 5
      connection-timeout: 5000
      idle-timeout: 600000
      max-lifetime: 1800000
      auto-commit: false  # Explicit transaction management
      
# Redis caching
spring:
  redis:
    host: redis.prod.internal
    port: 6379
    timeout: 5000
    pool:
      max-active: 20
```

**Handling growth:**

1. **Archival:** Move invoices older than 2 years to a data warehouse (Snowflake, BigQuery)
   ```sql
   INSERT INTO data_warehouse.invoices_archive
   SELECT * FROM invoices WHERE created_at < NOW() - INTERVAL '2 years';
   
   DELETE FROM invoices WHERE created_at < NOW() - INTERVAL '2 years';
   ```

2. **Read replicas:** For reporting queries, don't hit the primary
   ```java
   // Primary for writes
   @Primary
   @Bean
   public DataSource primaryDataSource() { ... }
   
   // Read replica for reporting
   @Bean
   public DataSource reportingDataSource() { ... }
   
   @Repository
   public class ReportingRepository {
     @Autowired(qualifier = "reportingDataSource")
     private DataSource ds;
   }
   ```

3. **Sharding:** If one database isn't enough, shard by customer_id
   ```
   Customer 1-1000000 → Database Shard 1
   Customer 1000001-2000000 → Database Shard 2
   ...
   ```

---

## Security & Compliance in Enterprise Systems

### Q10: Security Architecture in Microservices (Moubarak)

**Question:** Design a security architecture for Solera's global system. How do you handle authentication, authorization, secrets management, and compliance (GDPR, etc.)?

**Answer:**

Enterprise security requires defense in depth: network, application, and data layers.

**Architecture:**

```
External Clients
  ↓ (HTTPS only)
  ↓
API Gateway (Rate limiting, DDoS protection)
  ↓
Authentication Service (OAuth2 / OpenID Connect)
  ├─ JWT tokens (short-lived, 15 min)
  ├─ Refresh tokens (long-lived, 30 days)
  └─ Managed in Redis with TTL
  ↓
Authorization (Spring Security with RBAC)
  - @PreAuthorize("hasRole('ADMIN')")
  - @PreAuthorize("hasAuthority('INVOICE:READ')")
  ↓
Service (Business logic)
  ↓
Database (Encrypted at rest)
  - Sensitive data encrypted (SSN, credit card tokens)
  - Row-level security (customers see only their data)
```

**Implementation:**

```java
// Authentication Service
@Service
public class AuthenticationService {
  
  @Autowired
  private JwtTokenProvider jwtProvider;
  
  public AuthToken login(String username, String password) {
    User user = validateCredentials(username, password);
    
    String accessToken = jwtProvider.createAccessToken(user);  // 15 min
    String refreshToken = jwtProvider.createRefreshToken(user);  // 30 days
    
    // Store refresh token in Redis (revokable)
    redisTemplate.opsForValue().set(
      "refresh_token:" + user.getId(),
      refreshToken,
      Duration.ofDays(30)
    );
    
    return new AuthToken(accessToken, refreshToken);
  }
  
  public AuthToken refreshAccessToken(String refreshToken) {
    String userId = jwtProvider.extractUserId(refreshToken);
    
    // Verify refresh token hasn't been revoked
    String storedToken = redisTemplate.opsForValue().get("refresh_token:" + userId);
    if (!storedToken.equals(refreshToken)) {
      throw new InvalidTokenException("Token revoked");
    }
    
    // Issue new access token
    User user = userRepository.findById(userId);
    return new AuthToken(jwtProvider.createAccessToken(user), refreshToken);
  }
  
  public void logout(String userId) {
    // Revoke refresh token
    redisTemplate.delete("refresh_token:" + userId);
  }
}

// JWT Token Provider
@Service
public class JwtTokenProvider {
  
  private final String SECRET_KEY = // Loaded from environment
  
  public String createAccessToken(User user) {
    return Jwts.builder()
      .setSubject(user.getId())
      .claim("roles", user.getRoles())
      .claim("email", user.getEmail())
      .setIssuedAt(new Date())
      .setExpiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000)) // 15 min
      .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
      .compact();
  }
  
  public String extractUserId(String token) {
    return Jwts.parser()
      .setSigningKey(SECRET_KEY)
      .parseClaimsJws(token)
      .getBody()
      .getSubject();
  }
}

// Authorization
@RestController
@RequestMapping("/invoices")
public class InvoiceController {
  
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('INVOICE:READ')")
  public ResponseEntity<Invoice> getInvoice(@PathVariable String id) {
    // Only return invoice if user is the customer or admin
    Invoice invoice = invoiceRepository.findById(id);
    if (!invoice.getCustomerId().equals(getCurrentUserId()) && 
        !getCurrentUser().hasRole("ADMIN")) {
      throw new AccessDeniedException("You don't have access to this invoice");
    }
    return ResponseEntity.ok(invoice);
  }
  
  @PostMapping
  @PreAuthorize("hasAuthority('INVOICE:CREATE')")
  public ResponseEntity<Invoice> createInvoice(@RequestBody CreateInvoiceRequest req) {
    // Can only create invoices for customer IDs they manage
    if (!req.getCustomerId().equals(getCurrentUserId()) && 
        !getCurrentUser().hasRole("ADMIN")) {
      throw new AccessDeniedException("Cannot create invoices for this customer");
    }
    ...
  }
}
```

**Secrets management:**

Never hardcode secrets. Use a secrets vault:

```yaml
# application.yml (NOT secrets)
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/billing
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}  # Loaded from vault, not committed to git
```

**Using AWS Secrets Manager (or Azure Key Vault):**

```java
@Configuration
public class SecretsConfig {
  
  @Bean
  public AWSSecretsManager secretsManager() {
    return AWSSecretsManagerClientBuilder.standard().build();
  }
  
  @Bean
  public DataSource dataSource(AWSSecretsManager secretsManager) {
    String dbPassword = secretsManager.getSecretValue("solera/prod/db-password")
      .getSecretString();
    
    return DriverManager.getConnection(
      "jdbc:postgresql://db.prod:5432/billing",
      "billing_user",
      dbPassword
    );
  }
}
```

**Encryption at rest:**

```sql
-- Encrypt sensitive columns
CREATE TABLE customers (
  id BIGINT PRIMARY KEY,
  name VARCHAR(255),
  ssn BYTEA, -- Encrypted
  ...
);

-- Java: Encrypt before storing
@Entity
public class Customer {
  
  @Column(name = "ssn")
  @Convert(converter = SsnEncryptor.class)
  private String ssn;
}

public class SsnEncryptor implements AttributeConverter<String, byte[]> {
  
  private final Cipher cipher; // AES-256 encryption
  
  @Override
  public byte[] convertToDatabaseColumn(String ssn) {
    return cipher.doFinal(ssn.getBytes());
  }
  
  @Override
  public String convertToEntityAttribute(byte[] encrypted) {
    return new String(cipher.doFinal(encrypted));
  }
}
```

**GDPR compliance:**

```java
// Right to be forgotten: Delete user's personal data
@Service
public class DataPrivacyService {
  
  @Transactional
  public void deleteUserData(String userId) {
    // Delete all invoices
    invoiceRepository.deleteByCustomerId(userId);
    
    // Delete all payments
    paymentRepository.deleteByCustomerId(userId);
    
    // Delete personal data (anonymize if can't delete due to compliance)
    User user = userRepository.findById(userId);
    user.setEmail(null);
    user.setPhone(null);
    user.setSsn(null);
    userRepository.save(user);
    
    // Log the deletion for audit
    auditLog.record("DATA_DELETION", userId, getCurrentUser().getId());
  }
  
  // Data portability: Export user's data in machine-readable format
  public String exportUserData(String userId) {
    User user = userRepository.findById(userId);
    List<Invoice> invoices = invoiceRepository.findByCustomerId(userId);
    List<Payment> payments = paymentRepository.findByCustomerId(userId);
    
    return objectMapper.writeValueAsString(new {
      user, invoices, payments
    });
  }
}
```

---

## Legacy Modernization & Migration Strategies

### Q11: Strangler Fig Pattern for Large Monolith Modernization (Both)

**Question:** Solera has a large legacy system (possibly J2EE monolith). How would you approach modernizing it without taking it down? Walk through the strangler pattern, phasing, and risk mitigation.

**Answer:**

The Strangler Pattern is the safest way to modernize a running monolith. Instead of rewriting, you gradually replace pieces.

**Core idea:** New code wraps the old code, intercepting requests and gradually taking over functionality.

**Phase-by-phase approach:**

**Phase 1: Wrapping & API Gateway (Weeks 1-4)**

```
Before:
Frontend → Legacy Monolith
           ├─ Pricing
           ├─ Invoicing
           ├─ Payments
           └─ Reporting

After:
Frontend → API Gateway (new, Spring Boot)
           ├─ Route /pricing/* → Legacy Monolith (for now)
           ├─ Route /invoicing/* → Legacy Monolith (for now)
           ├─ Route /payments/* → Legacy Monolith (for now)
           └─ Route /reporting/* → Legacy Monolith (for now)
             ↓
           Legacy Monolith (unchanged, still running)
```

**Benefits:**
- All clients go through a single API Gateway (you control routing)
- Easy to A/B test new vs. old service
- Can apply security policies, rate limiting, logging centrally

**Implementation:**

```java
@Configuration
public class ApiGatewayConfig {
  
  @Bean
  public RouteLocator routes(RouteLocatorBuilder builder) {
    return builder.routes()
      .route("pricing_v1", r -> r
        .path("/pricing/**")
        .uri("http://legacy-monolith:8080"))
      .route("invoicing_v1", r -> r
        .path("/invoicing/**")
        .uri("http://legacy-monolith:8080"))
      .build();
  }
}
```

**Phase 2: Extract First Service (Weeks 5-16)**

Extract Pricing Service (lowest risk: stateless, read-heavy):

```
Frontend → API Gateway
           ├─ Route /pricing/* → NEW Pricing Service (Spring Boot)
           ├─ Route /invoicing/* → Legacy Monolith
           ├─ Route /payments/* → Legacy Monolith
           └─ Route /reporting/* → Legacy Monolith
             ↓ (for data)
           Legacy Monolith (still handles invoicing, payments, etc.)
             ↓
Database Replication
├─ Pricing data (read-only) → NEW Pricing DB (PostgreSQL)
├─ Invoicing data → Still in Legacy DB
├─ Payments → Still in Legacy DB
└─ Reporting → Still in Legacy DB
```

**How to handle data:**

```java
// Option 1: Dual writes (new service writes to new DB + legacy DB)
@Service
public class PricingService {
  
  public void updatePrice(String productId, BigDecimal newPrice) {
    // Write to new database
    newPricingDb.save(new Price(productId, newPrice));
    
    // Also write to legacy monolith (for backward compatibility)
    legacyMonolith.updatePrice(productId, newPrice);
  }
}

// Option 2: Change data capture (CDC) from legacy → new DB
// Use Debezium to stream legacy DB changes to Kafka
// New service consumes Kafka events, syncs to its database
```

**Testing the new service:**

```java
// Canary: Route 10% of pricing requests to new service
@Configuration
public class CanaryRouting {
  
  @Bean
  public RouteLocator routes(RouteLocatorBuilder builder) {
    return builder.routes()
      .route("pricing_canary", r -> r
        .path("/pricing/**")
        .and()
        .predicate(serverRequest -> Math.random() < 0.10)  // 10% to new service
        .uri("http://new-pricing-service:8080"))
      .route("pricing_legacy", r -> r
        .path("/pricing/**")
        .uri("http://legacy-monolith:8080"))
      .build();
  }
}
```

**Monitor the canary:**

```yaml
alerts:
  - name: PricingCanaryErrorRate
    expr: rate(http_requests_total{service='pricing_new',status=~'5..'}[5m]) > 0.01
    action: page-oncall
    
  - name: PricingCanaryLatency
    expr: histogram_quantile(0.99, http_request_duration_seconds{service='pricing_new'}) > 0.3
    action: page-oncall
```

If metrics look good, increase canary traffic: 10% → 50% → 100%.

**Phase 3: Extract Remaining Services (Weeks 17-52)**

Extract Invoicing (tightly coupled to Pricing), then Payments, then Reporting.

**Phase 4: Decommission Legacy Monolith (Week 53+)**

Once all services are extracted and stable, shut down the legacy system.

**Risk mitigation:**

1. **Data consistency:** Use two-phase commit for critical transactions
   ```java
   @Transactional
   public void createInvoice(Invoice invoice) {
     newInvoicingDb.save(invoice);  // New DB
     legacyDb.save(invoice);  // Legacy DB (backup)
   }
   ```

2. **Rollback capability:** Keep legacy service running as a fallback
   ```java
   try {
     return newPricingService.getPrice(productId);
   } catch (Exception e) {
     logger.warn("New pricing service failed, falling back to legacy");
     return legacyService.getPrice(productId);
   }
   ```

3. **Database backup:** Take daily backups before migration
   ```bash
   pg_dump -h legacy-db -U postgres billing > backup_$(date +%Y%m%d).sql
   ```

4. **Runbook for rollback:** Document step-by-step rollback procedure
   ```markdown
   ## Rollback from Pricing Service
   1. API Gateway: Route /pricing/* back to legacy monolith
   2. Stop new pricing service
   3. Verify legacy service is healthy: curl http://legacy/pricing/health
   4. Alert on-call: Migration rolled back
   ```

---

## Situational & Problem-Solving

### Q12: Incident Response: Cascading Failure Scenario

**Question:** It's Friday 6 PM. Your payment service starts timing out, causing cascading failures across the platform. Customers can't pay invoices. What's your response?

**Answer:**

This is a classic cascading failure scenario. Response time matters.

**Minute 0-5: Situational Awareness**

```
1. Check on-call dashboard (PagerDuty, OpsGenie)
   - Alert: Payment Service latency p99 > 5 sec
   - Alert: API Gateway error rate 45% (normally < 1%)
   
2. Check if payments service is up
   - Health check: curl http://payment-service:8080/health
   - Result: {"status": "UP"} ← Service thinks it's healthy!
   
3. Check metrics (Grafana)
   - Database connection pool: 29/30 connections used (FULL)
   - Query latency: 3 sec (normally 50 ms)
   - GC pause time: 2 sec (normally 100 ms)
   
4. Diagnosis: Payments service exhausted database connections
   - Something is holding connections open too long
   - New requests queue up, timing out
```

**Minute 5-10: Immediate Mitigation**

```
Option A: Restart the service
- kubectl delete pod payment-service-xxxxx
- New instance starts, connections reset
- But: Doesn't fix the root cause; will fail again

Option B: Circuit breaker (if you're calling a slow downstream service)
- Hystrix/Resilience4j should already have failed open
- But: Check if configured correctly

Option C: Reduce load on payment service
- API Gateway: Increase timeout from 10 sec to 60 sec
  - This delays failure rather than fixing it
  
Option D: Kill slow queries
- Connect to database: psql
- SELECT * FROM pg_stat_activity WHERE state = 'active' AND duration > 10000;
- CANCEL query;
- This might free up connections for other requests
```

**My response: Option D + Option A**

```sql
-- Kill slow queries
SELECT pid, now() - query_start as duration, query
FROM pg_stat_activity
WHERE state = 'active'
AND duration > 60000  -- > 60 sec
ORDER BY duration DESC;

-- Cancel the worst offender
SELECT pg_cancel_backend(pid);

-- If that doesn't work, kill the connection
SELECT pg_terminate_backend(pid);
```

**Minute 10-15: Restart + Verify**

```bash
# Restart the service
kubectl rollout restart deployment/payment-service

# Wait for it to start
kubectl rollout status deployment/payment-service

# Health check
curl http://payment-service:8080/health

# Test a payment
curl -X POST http://api-gateway/payments \
  -d '{"invoiceId":"test-123","amount":100}' \
  -H "Content-Type: application/json"
```

**Minute 15+: Root Cause Analysis**

While on-call handles the incident, I'm digging:

```bash
# Check logs from the payment service
kubectl logs deployment/payment-service --tail=1000 | grep -i error

# Check database slow query log
tail -100 /var/log/postgresql/slow_queries.log

# Check if there was a deployment or config change
kubectl rollout history deployment/payment-service
git log --oneline -10

# Check if a dependency went down (Stripe API, etc.)
curl -I https://api.stripe.com/health
```

**Likely root causes:**

1. **Query gone bad:** A new feature introduced an N+1 query
   ```java
   // BAD: This queries database 100 times for 100 invoices
   invoices.forEach(inv -> inv.getPayments()); // Triggers query for each
   
   // GOOD: Load all payments at once
   List<Payment> allPayments = paymentRepository.findByInvoiceIds(invoiceIds);
   ```

2. **Connection leak:** A recent code change doesn't close database connections
   ```java
   // BAD: Connection never closed
   Connection conn = dataSource.getConnection();
   // ... if exception here, connection not closed
   
   // GOOD: Use try-with-resources
   try (Connection conn = dataSource.getConnection()) {
     // ...
   }
   ```

3. **Deployment of a new service:** New service is also using the database connection pool
   ```java
   // Check how many active connections each service is using
   SELECT application_name, count(*) as connections
   FROM pg_stat_activity
   GROUP BY application_name;
   ```

**Minute 60+: Post-Incident Runbook**

```markdown
## Payment Service Cascading Failure RCA

**What happened:**
- New code pushed 4 hours ago opened database connections but didn't close them
- Connection pool exhausted (30 max)
- New payments queued, timing out
- API Gateway saw errors, stressed the whole system

**Why it wasn't caught:**
- No connection pool monitoring alert
- No integration tests for database connection cleanup
- Load test only used 10 concurrent users; production had 1000

**Fixes:**
1. Add monitoring alert: `hikari_connections_active > 25` (alert at 80% capacity)
2. Add integration tests with 100+ concurrent requests
3. Code review: Always use try-with-resources or Spring's @Transactional
4. Load test: Run with 10x expected peak load before releases

**Timeline:**
- 18:00 - Deployment of feature X
- 18:45 - Payment latency starts increasing
- 18:50 - On-call paged (alert threshold hit)
- 19:05 - Identified connection pool exhaustion
- 19:15 - Killed slow queries, restarted service
- 19:30 - System recovered, payments flowing again
- 20:00 - Root cause identified (connection leak in feature X)
```

**Action items:**

```jira
Task 1: Add monitoring alert for connection pool exhaustion
  Assignee: Platform Team
  Priority: High
  Due: Monday
  
Task 2: Improve load testing (10x peak load)
  Assignee: QA Team
  Priority: High
  Due: 1 week
  
Task 3: Code review checklist: Resource management
  Assignee: Tech Lead
  Priority: Medium
  Due: 1 week
  
Task 4: Deployment post-check: Run basic smoke tests
  Assignee: DevOps
  Priority: High
  Due: Before next release
```

---

## Interview Tips

### What Furminger Likely Asks:

1. **SAFe & scaling:** How would you implement SAFe for 200+ engineers?
2. **Metrics & KPIs:** What do you measure? How do you report to VP?
3. **Transformation:** How do you change culture during a big shift?
4. **Global operations:** Managing teams across time zones, how?
5. **Decision-making:** How do you avoid bottlenecks as teams grow?

### What Moubarak Likely Asks:

1. **Architecture:** Monolith vs. microservices trade-offs?
2. **Spring Boot:** How do you design high-performance Spring services?
3. **Distributed systems:** How do you handle eventual consistency?
4. **Database optimization:** How do you query efficiently at scale?
5. **CI/CD:** What does your ideal pipeline look like?
6. **Security:** How do you protect sensitive data in microservices?

### Golden Answers:

- **Always cite metrics:** Not "we were faster," but "40% faster, confirmed by data"
- **Mention trade-offs:** "Microservices gave us scalability but added distributed complexity"
- **Show learning:** "First time we did X, it failed. We learned Y and didn't repeat"
- **Think end-to-end:** Don't optimize one layer; optimize the whole system
- **Emphasize people:** Architecture is about enabling teams, not just technology

---

## Key Talking Points

| Area | Point |
|------|-------|
| **Leadership** | Led 100+ engineers across 3+ regions; SAFe experience; centers of excellence |
| **Architecture** | Spring Boot microservices; DDD; strangler pattern for modernization |
| **DevOps** | CI/CD pipelines with canary deployments; Kubernetes orchestration |
| **Databases** | PostgreSQL optimization; partitioning for scale; eventual consistency |
| **Performance** | Caching (Redis), connection pooling, query optimization; sub-100ms p99 latency |
| **Security** | JWT + OAuth2; encryption at rest; GDPR compliance; secrets management |
| **Incident response** | Circuit breakers; graceful degradation; rollback strategies; RCA culture |

---

**Good luck on Friday! You've got this. 🚀**
