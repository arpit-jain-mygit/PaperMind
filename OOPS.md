# Object-Oriented Programming Systems (OOPS) - Technology Architect Guide

*For Technology Architect interviews: Focus on design principles, scalability implications, and real-world trade-offs.*

## Table of Contents
- [Encapsulation](#encapsulation)
- [Inheritance](#inheritance)
- [Polymorphism](#polymorphism)
- [Abstraction](#abstraction)
- [SOLID Principles](#solid-principles)
  - [Single Responsibility Principle (SRP)](#srp)
  - [Open/Closed Principle (OCP)](#ocp)
  - [Liskov Substitution Principle (LSP)](#lsp)
  - [Interface Segregation Principle (ISP)](#isp)
  - [Dependency Inversion Principle (DIP)](#dip)
- [Design Patterns](#design-patterns)
- [Common Interview Questions](#common-interview-questions)

---

## Encapsulation {#encapsulation}

**Simple Definition:** Bundle data and methods together, hide internal details.

**Realistic Architect View:** Encapsulation is about controlling the contract your component exposes to others.

### Why It Matters at Scale
- **API Stability:** Change internals without breaking 100+ dependent services
- **Security Boundary:** Prevent direct field access that could corrupt state
- **Predictability:** Callers don't depend on implementation details

### Example
```java
public class DatabaseConnection {
    // Hidden: internal retry logic, connection pooling
    private int connectionPoolSize = 10;
    private RetryPolicy retryPolicy;
    
    // Exposed: clean contract
    public Result executeQuery(String sql) {
        // Retry logic, pooling, all hidden
    }
    
    // Caller doesn't know about internal pools, they just use executeQuery()
}
```

### Trade-offs
- **Pro:** Easier to maintain, refactor internals without breaking clients
- **Con:** More boilerplate (getters/setters), potential performance overhead if not carefully designed

---

## Inheritance {#inheritance}

**Simple Definition:** Derive new classes from existing ones, reuse and extend behavior.

**Realistic Architect View:** Inheritance can be powerful or problematic depending on hierarchy depth and design.

### When to Use (The Right Way)
- **Clear IS-A relationship:** A `Manager` IS-A `Employee` (real concept, not just code reuse)
- **Shallow hierarchies:** 2-3 levels deep is ideal; avoid deep chains
- **Shared implementation:** Common methods actually used by subclasses

### When NOT to Use (The Red Flag)
- Deep inheritance chains (5+ levels) → Hard to reason about, high maintenance cost
- Using inheritance just for code reuse → Use composition instead
- Mixing unrelated concepts → Leads to bloated base classes

### Example
```java
// Good: Clear IS-A, shallow
public class PaymentProcessor {
    public void processPayment(Payment p) { }
}

public class CreditCardProcessor extends PaymentProcessor {
    @Override
    public void processPayment(Payment p) { 
        // Specific credit card logic
    }
}

// Bad: Too deep, fragile
public class A { }
public class B extends A { }
public class C extends B { }
public class D extends C { }  // 4 levels deep → nightmare
```

### Trade-offs
- **Pro:** Code reuse, natural modeling for certain hierarchies
- **Con:** Fragile base class problem, tight coupling, deep hierarchies are hard to maintain

---

## Polymorphism {#polymorphism}

**Simple Definition:** Same method name, different implementations. Objects behave differently based on type.

**Realistic Architect View:** Polymorphism enables flexibility and testability. Critical for building loosely coupled systems.

### Types

#### 1. Compile-time (Method Overloading)
```java
public class Calculator {
    public int add(int a, int b) { return a + b; }
    public double add(double a, double b) { return a + b; }
}
```
- Resolved at compile time
- Less powerful than runtime polymorphism

#### 2. Runtime (Method Overriding)
```java
public interface DataStore {
    Data read(String key);
}

public class RedisDataStore implements DataStore {
    @Override
    public Data read(String key) { /* Redis logic */ }
}

public class DatabaseDataStore implements DataStore {
    @Override
    public Data read(String key) { /* DB logic */ }
}

// Caller code: doesn't know which implementation
DataStore store = getDataStore(); // Could be Redis or DB
Data result = store.read("key");  // Works either way
```

### Why It Matters at Scale
- **Loose Coupling:** Services depend on interfaces, not concrete implementations
- **Testability:** Easy to mock implementations for testing
- **Extensibility:** Add new implementations without changing calling code
- **Swappability:** Replace Redis with Memcached without touching 50 services

### Trade-offs
- **Pro:** Flexibility, testability, enables dependency injection
- **Con:** Runtime overhead (method lookup), can hide complexity

---

## Abstraction {#abstraction}

**Simple Definition:** Hide complexity behind a simple interface.

**Realistic Architect View:** Abstraction is about exposing the right level of detail to callers.

### Good Abstraction Examples
```java
// Bad: Exposes internal complexity
public class OrderService {
    public void createOrder(OrderDTO dto) {
        // Caller needs to know: validate input, check inventory, 
        // deduct from warehouse, call payment processor, 
        // send to message queue, update cache, log to analytics
        
        validateOrder(dto);
        checkInventory(dto.items);
        deductFromWarehouse(dto.items);
        processPayment(dto);
        publishToKafka(dto);
        updateCache(dto.orderId);
        logToAnalytics(dto);
    }
}

// Good: Caller just knows what to do
public class OrderService {
    public OrderResult createOrder(OrderDTO dto) {
        // All complexity hidden inside
        return orderWorkflow.process(dto);
    }
}
```

### At Architect Level
- **Data Models:** Expose domain concepts, not database schemas
- **APIs:** Hide distributed system complexity (retries, timeouts, fallbacks)
- **Configuration:** Hide infrastructure details from business logic

---

## SOLID Principles {#solid-principles}

### Single Responsibility Principle (SRP) {#srp}

**Definition:** A class should have only one reason to change.

**Realistic View:** If you're struggling to name a class or writing comments like "This also handles...", it's violating SRP.

```java
// Bad: Two reasons to change (user validation logic + database persistence)
public class UserManager {
    public boolean validateUser(User u) { /* validation */ }
    public void saveUser(User u) { /* database */ }
}

// Good: Separated concerns
public class UserValidator {
    public boolean validate(User u) { /* validation only */ }
}

public class UserRepository {
    public void save(User u) { /* persistence only */ }
}
```

**Architect Perspective:** SRP makes microservices design intuitive. Each service has one responsibility.

---

### Open/Closed Principle (OCP) {#ocp}

**Definition:** Open for extension, closed for modification. Add new behavior without changing existing code.

```java
// Bad: Adding new payment type requires modifying PaymentProcessor
public class PaymentProcessor {
    public void process(Payment p) {
        if (p.type == "CREDIT_CARD") { /* CC logic */ }
        else if (p.type == "PAYPAL") { /* PayPal logic */ }
        // Add new type? Modify this class again
    }
}

// Good: Extend by adding new implementations
public interface PaymentProcessor {
    void process(Payment p);
}

public class CreditCardProcessor implements PaymentProcessor {
    public void process(Payment p) { /* CC logic */ }
}

public class PayPalProcessor implements PaymentProcessor {
    public void process(Payment p) { /* PayPal logic */ }
}

// Add new processor? Just implement the interface
```

**Key:** Use inheritance/interfaces to extend without modifying.

---

### Liskov Substitution Principle (LSP) {#lsp}

**Definition:** Derived classes must be substitutable for their base classes. Subclass behavior must match parent's contract.

**The Rule:** If `S` is a subtype of `T`, then objects of type `S` may be substituted for objects of type `T` without breaking the program.

---

#### Example 1: The Classic Rectangle-Square Problem

```java
// ❌ Bad: Square violates LSP
public class Rectangle {
    protected int width, height;
    
    public void setWidth(int w) { this.width = w; }
    public void setHeight(int h) { this.height = h; }
    public int area() { return width * height; }
}

public class Square extends Rectangle {
    @Override
    public void setWidth(int w) { 
        this.width = w; 
        this.height = w;  // VIOLATION: Caller expects independent width/height
    }
    @Override
    public void setHeight(int h) { 
        this.width = h;   // This breaks the contract
        this.height = h;
    }
}

// This breaks:
Rectangle r = new Square();
r.setWidth(5);
r.setHeight(10);
System.out.println(r.area()); // Expected 50, got 100. Caller is shocked.

// ✅ Good: Use composition, not inheritance
public class Square {
    private int side;
    
    public void setSide(int s) { this.side = s; }
    public int area() { return side * side; }
}
```

---

#### Example 2: Cache Implementations (Real Architect Scenario)

```java
// Contract: Cache stores and retrieves values, may return null if missing
public interface Cache {
    void put(String key, Object value);
    Object get(String key);
}

// ✅ Correct: InMemoryCache follows the contract
public class InMemoryCache implements Cache {
    private Map<String, Object> store = new HashMap<>();
    
    @Override
    public void put(String key, Object value) {
        store.put(key, value);
    }
    
    @Override
    public Object get(String key) {
        return store.get(key);  // Returns null if missing - OK
    }
}

// ✅ Correct: RedisCache follows the contract
public class RedisCache implements Cache {
    private JedisCluster redis;
    
    @Override
    public void put(String key, Object value) {
        redis.set(key, serialize(value));
    }
    
    @Override
    public Object get(String key) {
        String val = redis.get(key);
        return val == null ? null : deserialize(val);
    }
}

// ❌ Wrong: StrictCache violates contract (throws on missing key)
public class StrictCache implements Cache {
    private Map<String, Object> store = new HashMap<>();
    
    @Override
    public void put(String key, Object value) {
        store.put(key, value);
    }
    
    @Override
    public Object get(String key) {
        // VIOLATION: Throws exception instead of returning null
        if (!store.containsKey(key)) {
            throw new CacheKeyNotFoundException("Key not found: " + key);
        }
        return store.get(key);
    }
}

// This breaks code that uses Cache polymorphically:
Cache cache = new StrictCache();
Object user = cache.get("user:123"); // Caller expects null or Object
                                      // But gets exception! Violates contract.

// Usage code that worked with InMemoryCache now crashes:
for (String key : keys) {
    Object val = cache.get(key);
    if (val != null) {  // What if cache throws exception? Code breaks.
        process(val);
    }
}
```

**Key Insight:** Contract includes return values AND exceptions. If base class returns null, subclass can't throw exception.

---

#### Example 3: Data Store Repository (Multi-implementation)

```java
// Contract: query() returns results, may be empty; must handle timeouts
public interface UserRepository {
    List<User> findByAge(int age) throws TimeoutException;
}

// ✅ Correct: Database implementation
public class SqlUserRepository implements UserRepository {
    @Override
    public List<User> findByAge(int age) throws TimeoutException {
        try {
            // DB query with timeout handling
            return executeQueryWithTimeout(age, 5000);
        } catch (SQLException e) {
            throw new TimeoutException("Query exceeded timeout");
        }
    }
}

// ✅ Correct: ElasticSearch implementation
public class ElasticSearchUserRepository implements UserRepository {
    @Override
    public List<User> findByAge(int age) throws TimeoutException {
        // ES query with timeout
        return searchWithTimeout(age, 5000);
    }
}

// ❌ Wrong: In-memory implementation that doesn't handle timeout
public class InMemoryUserRepository implements UserRepository {
    @Override
    public List<User> findByAge(int age) {  // VIOLATION: Doesn't throw TimeoutException
        // No timeout handling - just iterates memory
        return users.stream()
                    .filter(u -> u.getAge() == age)
                    .collect(toList());
    }
}

// Caller code breaks:
UserRepository repo = new InMemoryUserRepository();
try {
    List<User> results = repo.findByAge(25);  // Never throws TimeoutException
    // What if data grows to millions? No protection from hanging.
} catch (TimeoutException e) {
    // This catch block never executes, defeating resilience strategy
}
```

---

#### Example 4: Payment Processing (Preconditions/Postconditions)

```java
public interface PaymentProcessor {
    // Contract: Input must have amount > 0, returns success/failure
    PaymentResult process(Payment payment) throws PaymentException;
    
    // Precondition: balance must be >= amount
    // Postcondition: if success, balance decreases by amount
}

// ✅ Correct: Stripe processor enforces contract
public class StripeProcessor implements PaymentProcessor {
    @Override
    public PaymentResult process(Payment payment) throws PaymentException {
        if (payment.getAmount() <= 0) {
            throw new PaymentException("Amount must be positive");  // Enforces precondition
        }
        
        PaymentResult result = stripe.charge(payment);
        
        if (result.isSuccess()) {
            // Postcondition: balance is reduced
            accountService.deductBalance(payment.getAmount());
        }
        return result;
    }
}

// ❌ Wrong: MockPaymentProcessor violates postcondition
public class MockPaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentResult process(Payment payment) throws PaymentException {
        // Just returns success without deducting balance!
        // VIOLATION: Postcondition not satisfied
        return PaymentResult.success();
    }
}

// Test code that worked breaks in production:
PaymentProcessor processor = new MockPaymentProcessor();
double balanceBefore = account.getBalance();
processor.process(new Payment(100, account));
// With real processor, balance would be 100 less
// With mock, balance is unchanged - test passes but production fails!
```

---

#### Example 5: Message Queue Publishing (Guarantee violations)

```java
public interface MessageQueue {
    // Contract: Message is guaranteed to be delivered to all subscribers
    void publish(String topic, String message) throws QueueException;
}

// ✅ Correct: Kafka implementation guarantees delivery
public class KafkaQueue implements MessageQueue {
    @Override
    public void publish(String topic, String message) throws QueueException {
        // Kafka: Replicated, persistent, guaranteed delivery
        kafka.send(new ProducerRecord(topic, message));
    }
}

// ✅ Correct: RabbitMQ implementation with persistence
public class RabbitMQQueue implements MessageQueue {
    @Override
    public void publish(String topic, String message) throws QueueException {
        // RabbitMQ with durable queue: guaranteed delivery
        channel.basicPublish(topic, "", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
    }
}

// ❌ Wrong: In-memory queue loses messages on crash
public class InMemoryQueue implements MessageQueue {
    private List<String> messages = new ArrayList<>();  // Not persistent!
    
    @Override
    public void publish(String topic, String message) {
        messages.add(message);  // VIOLATION: No delivery guarantee
    }
}

// Caller code fails silently:
MessageQueue queue = new InMemoryQueue();
queue.publish("orders", "user:123 ordered item:456");
// System crashes, message is lost
// With Kafka, message would be replicated and recover
```

---

#### Why LSP Matters at Architect Level

| Scenario | Bad Design | Good Design |
|----------|-----------|------------|
| **Caching Strategy** | Cache impl throws on missing key | All implementations return null consistently |
| **Multi-region Deployment** | Different datastores have different guarantees | All datastores have same SLA/timeout behavior |
| **Microservice Upgrades** | New implementation has different error handling | All versions handle errors identically |
| **Testing** | Mock behaves differently than real impl | Mock follows exact same contract |
| **Scaling** | Different implementations timeout differently | All scale with same timeout guarantees |

**The Golden Rule:** 
- If callers write code that works with `DatabaseRepository`, it must work with `CacheRepository` too
- If one implementation throws exception on timeout, all must
- If one implementation guarantees consistency, all must
- If one caches results, all must (or none should)

---

### Interface Segregation Principle (ISP) {#isp}

**Definition:** Clients shouldn't depend on interfaces they don't use.

```java
// Bad: Fat interface
public interface Worker {
    void work();
    void eat();
    void sleep();
}

public class Robot implements Worker {
    public void work() { /* ok */ }
    public void eat() { /* doesn't make sense! */ }
    public void sleep() { /* doesn't make sense! */ }
}

// Good: Segregated interfaces
public interface Workable {
    void work();
}

public interface Eatable {
    void eat();
}

public class Robot implements Workable {
    public void work() { /* ok */ }
}

public class Human implements Workable, Eatable {
    public void work() { /* ok */ }
    public void eat() { /* ok */ }
}
```

**Architect View:** Keep APIs lean. Don't force clients to implement methods they don't need.

---

### Dependency Inversion Principle (DIP) {#dip}

**Definition:** Depend on abstractions, not concrete implementations.

```java
// Bad: Tight coupling to concrete class
public class PaymentService {
    private StripeProcessor stripe = new StripeProcessor();
    
    public void pay(Payment p) {
        stripe.process(p);  // What if we want to use PayPal?
    }
}

// Good: Depend on interface
public class PaymentService {
    private PaymentProcessor processor;
    
    public PaymentService(PaymentProcessor processor) {
        this.processor = processor;  // Injected, swappable
    }
    
    public void pay(Payment p) {
        processor.process(p);  // Works with any processor
    }
}

// Usage
PaymentService service = new PaymentService(new StripeProcessor());
// Or swap processors easily
service = new PaymentService(new PayPalProcessor());
```

**Architect Impact:** Enables dependency injection, makes testing easy, reduces coupling between services.

---

## Design Patterns {#design-patterns}

*Will add patterns as we discuss specific questions. Common ones for architects:*
- Singleton, Factory, Builder, Strategy, Observer, Proxy, Adapter, Decorator

---

## Common Interview Questions {#common-interview-questions}

### Q1: Why use encapsulation when we can make all fields public?

*Add detailed answer based on your questions...*

### Q2: When would you use inheritance vs composition?

*Add detailed answer based on your questions...*

### Q3: How do SOLID principles apply to microservices design?

*Add detailed answer based on your questions...*

### Q4: Design a payment system with multiple processors (Stripe, PayPal, Square). How do you make it extensible?

*Add detailed answer based on your questions...*

---

**Last Updated:** 2026-07-16  
**Status:** In Progress - Questions pending
