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

**Definition:** Derived classes must be substitutable for their base classes.

```java
// Bad: Square violates LSP
public class Rectangle {
    public void setWidth(int w) { this.width = w; }
    public void setHeight(int h) { this.height = h; }
    public int area() { return width * height; }
}

public class Square extends Rectangle {
    @Override
    public void setWidth(int w) { 
        this.width = w; 
        this.height = w;  // Forces height = width
    }
    // Violates contract: if caller sets width ≠ height, behavior is unexpected
}

// Good: Use composition or correct hierarchy
public class Square {
    private int side;
    public int area() { return side * side; }
}
```

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
