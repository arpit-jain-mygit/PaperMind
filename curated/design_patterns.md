# GoF Design Patterns: Real-World Guide for Architects

## Table of Contents
1. [Creational Patterns](#creational-patterns)
   - [Singleton](#singleton)
   - [Factory Method](#factory-method)
   - [Abstract Factory](#abstract-factory)
   - [Builder](#builder)
   - [Prototype](#prototype)
2. [Structural Patterns](#structural-patterns)
   - [Adapter](#adapter)
   - [Bridge](#bridge)
   - [Composite](#composite)
   - [Decorator](#decorator)
   - [Facade](#facade)
   - [Flyweight](#flyweight)
   - [Proxy](#proxy)
3. [Behavioral Patterns](#behavioral-patterns)
   - [Chain of Responsibility](#chain-of-responsibility)
   - [Command](#command)
   - [Interpreter](#interpreter)
   - [Iterator](#iterator)
   - [Mediator](#mediator)
   - [Memento](#memento)
   - [Observer](#observer)
   - [State](#state)
   - [Strategy](#strategy)
   - [Template Method](#template-method)
   - [Visitor](#visitor)

---

## Understanding the Three Categories

### **Creational** = HOW to CREATE objects
**Core question:** "How do I instantiate objects in my codebase?"

You care about the **creation process** — who creates what, when, how many. Examples: database connection pools, API clients, service factories.

### **Structural** = HOW to ORGANIZE objects together
**Core question:** "How do I compose/combine objects into larger structures?"

You care about **relationships and composition** — how do pieces fit together? How do you layer services? Examples: middleware stacks, decorator chains, adapters, wrappers.

### **Behavioral** = HOW objects COMMUNICATE and INTERACT
**Core question:** "How do objects talk to each other? Who does what? When does execution happen?"

You care about **responsibilities and interactions** — who calls whom, how do they coordinate, what triggers what? Examples: event listeners, task queues, state machines, workflows.

**Quick Reference:**

| Category | Asks | Solves | Tech Examples |
|----------|------|--------|---|
| **Creational** | "How do I INSTANTIATE it?" | Object creation logic | Database clients, API clients, service factories |
| **Structural** | "How do I LAYER/COMPOSE it?" | Object relationships | Middleware stacks, decorator chains, adapters |
| **Behavioral** | "How do objects INTERACT?" | Communication flow | Event listeners, task queues, state machines |

---

## Creational Patterns

### Singleton

**Real Problem:** You need exactly ONE instance of a resource (database connection, logger, config manager) throughout the app lifecycle. Creating multiple instances wastes memory, causes data inconsistency, or breaks resource limits.

**Real-World Example:** Database connection pool manager. You don't want 100 instances each holding separate connections; you want ONE pool managing all connections.

**Key Difference from Similar:**
- **vs. Static Class:** Singleton can implement interfaces, be mocked in tests, and be inherited. Static classes cannot.
- **vs. Service Locator:** Singleton is directly injected; Service Locator requires a registry lookup (extra indirection, harder to test).

---

#### Real-World Tech Examples

**1. Database Connection Pool**
```java
// WRONG: Creates new pool each time
ConnectionPool pool1 = new ConnectionPool();
ConnectionPool pool2 = new ConnectionPool();
// Now you have 2 pools with duplicate connections ❌

// RIGHT: Singleton ensures one pool
ConnectionPool pool = ConnectionPool.getInstance();
// Always the same instance ✅
```

**2. Logger**
```java
// Production app has thousands of log statements
Logger logger1 = new Logger();  // ❌ Creates new logger object 1000+ times
Logger logger2 = new Logger();  // ❌ Wastes memory, creates file handles

// Singleton
Logger logger = Logger.getInstance();  // ✅ All logs use same logger instance
logger.info("User login");
logger.info("User action");
logger.info("User logout");
// One logger, one log file, clean organization
```

**3. Configuration Manager**
```java
// WRONG: Loads config multiple times
Config config1 = new Config();
config1.load("app.properties");
Config config2 = new Config();
config2.load("app.properties");  // Redundant disk I/O ❌

// RIGHT: Singleton loads once
Config config = Config.getInstance();  // Loaded once at startup
String dbUrl = config.get("database.url");  // Returns cached value ✅
```

**4. Cache Manager (Redis, Memcached)**
```java
// WRONG: Multiple cache instances = no sharing
Cache cache1 = new Cache();
cache1.set("user:123", userData);

Cache cache2 = new Cache();
String data = cache2.get("user:123");  // Returns null ❌ (different instance)

// RIGHT: Singleton shares cache
Cache cache = CacheManager.getInstance();
cache.set("user:123", userData);

Cache cache2 = CacheManager.getInstance();  // Same instance
String data = cache2.get("user:123");  // Returns userData ✅
```

**5. Thread Pool**
```java
// WRONG: Creates multiple thread pools
ExecutorService pool1 = Executors.newFixedThreadPool(10);
ExecutorService pool2 = Executors.newFixedThreadPool(10);
// Now 20 threads instead of 10 ❌

// RIGHT: Singleton manages one thread pool
ExecutorService pool = ThreadPoolManager.getInstance().getPool();
pool.execute(task1);
pool.execute(task2);
// All tasks use same 10 threads ✅
```

**6. Application Settings/Configuration**
```java
// Spring Framework's ApplicationContext (Singleton by default)
ApplicationContext context = ApplicationContext.getInstance();

Service service = context.getBean(Service.class);  // Same instance always
// No matter where you call it, you get the same context
```

**7. Database Connection (Single Connection)**
```java
// Some apps need a single, persistent connection
DatabaseConnection conn = DatabaseConnection.getInstance();
conn.query("SELECT * FROM users");

// Later in app
DatabaseConnection conn2 = DatabaseConnection.getInstance();
// Same connection object ✅
```

**8. Metrics/Monitoring**
```java
// Track app-wide metrics
MetricsCollector metrics = MetricsCollector.getInstance();
metrics.recordRequest();      // Increments shared counter
metrics.recordResponse();     // Updates shared stats

// Reports use same instance
Report report = new Report();
int totalRequests = report.getTotalRequests();  // Reads from shared metrics
```

---

#### When to Use Singleton

```
✅ Database Connection Pool       → ONE pool, all requests use it
✅ Logger                         → ONE logger, all modules log to it
✅ Configuration Manager          → ONE config, loaded once
✅ Cache Manager                  → ONE cache, shared across app
✅ Thread Pool                    → ONE pool, limited threads
✅ Metrics Collector              → ONE metrics instance, shared data
✅ Application Context            → ONE context, all beans available
✅ Authentication Manager         → ONE auth service, all requests use it
```

---

#### When NOT to Use Singleton

```
❌ User object                    → Each user needs separate instance
❌ Request object                 → Each HTTP request needs separate instance
❌ Connection (per-request)       → Each thread might need its own connection
❌ Temporary data holder          → Should be scoped to specific use
❌ Domain objects (Customer, Order)  → Each object is independent
```

**Wrong Singleton:**
```java
// BAD: User as Singleton
User user = User.getInstance();
user.setName("Alice");

User user2 = User.getInstance();
user2.setName("Bob");

System.out.println(user.getName());  // Prints "Bob" ❌ (overwrote Alice!)
```

**Right approach:**
```java
// GOOD: Each user is separate
User user1 = new User("Alice");
User user2 = new User("Bob");

System.out.println(user1.getName());  // "Alice" ✅
System.out.println(user2.getName());  // "Bob" ✅
```

---

#### Common Mistakes in Interviews

**Mistake 1: Making everything Singleton**
```
❌ "We should make User, Order, Product all Singletons"
❌ "Easier to access globally"

✅ "Singleton only for shared resources like Logger, Config, ThreadPool"
```

**Mistake 2: Not thread-safe Singleton**
```java
// WRONG: Not thread-safe
class Logger {
    private static Logger instance;
    
    public static Logger getInstance() {
        if (instance == null) {
            instance = new Logger();  // Race condition ❌
        }
        return instance;
    }
}

// RIGHT: Thread-safe
class Logger {
    private static final Logger instance = new Logger();  // Eager init ✅
    
    public static Logger getInstance() {
        return instance;
    }
}
```

**Mistake 3: Hard to test**
```java
// WRONG: Can't mock
DatabaseConnection conn = DatabaseConnection.getInstance();
conn.query("SELECT...");  // Hard to mock this

// RIGHT: Use interface + dependency injection
interface Database {
    ResultSet query(String sql);
}

class Service {
    private Database db;
    
    Service(Database db) {
        this.db = db;  // Mockable ✅
    }
}
```

---

#### Interview Questions on Singleton

**Q1: Why not just use a static variable?**
```
A: "Singleton can implement interfaces, be inherited, and be mocked in tests.
Static variable is just a variable — can't do any of those. 
Also Singleton can have initialization logic; static variable can't."
```

**Q2: Is Singleton thread-safe?**
```
A: "Depends on implementation. Use eager initialization or synchronized 
double-checked locking. Better: use enum for automatic thread safety."
```

**Q3: How do you test code using Singleton?**
```
A: "Inject an interface instead of directly using Singleton.
Or use reflection to reset singleton in @Before/@After methods.
Or use Singleton with setter for testing: 
Logger.setInstance(mockLogger);"
```

**Q4: Singleton vs. Dependency Injection?**
```
A: "Singleton makes global state. Dependency injection is cleaner — 
inject what you need into constructor. But Singleton is acceptable for 
true singletons like Logger, Config that are rarely mocked."
```

**Q5: Should all cross-cutting concerns be Singleton?**
```
A: "Not exactly. Most SHOULD be, but depends on architecture.

Logging → Singleton (must share all logs)
Metrics → Singleton (must collect app-wide)
Authentication → Could be Singleton OR per-request
Transactions → Often per-request (more testable)

The real rule: Singleton if:
1. Multiple instances would break consistency
2. Need to coordinate shared state across app
3. Wasting resources (connections, threads, file handles)

Most cross-cutting concerns fit this, but some (like transactions)
can be per-request for better testing and isolation."
```

---

## Cross-Cutting Concerns and Singleton

### Correlation (Not Always True)

**Most cross-cutting concerns ARE Singleton:**
```
✅ Logging           → Singleton (shared across all modules)
✅ Authentication    → Singleton (checks across all requests)
✅ Caching           → Singleton (shared cache across app)
✅ Configuration     → Singleton (shared config)
✅ Metrics/Monitoring → Singleton (collected app-wide)
✅ Transaction Mgmt  → Singleton or per-request (both work)
```

**But NOT all Singletons are cross-cutting:**
```
❌ Payment Processor → Not cross-cutting, just "one provider at a time"
❌ Thread Pool → Not cross-cutting, just "shared resource"
```

### The Real Rule

**NOT:** "All cross-cutting concerns must be Singleton"

**RATHER:** "All shared resources that coordinate state should be Singleton"

Most cross-cutting concerns fit this, but not all.

| Concern | Type | Singleton? | Why |
|---------|------|---|---|
| **Logging** | Cross-cutting + Shared state | ✅ Always | All logs to one place |
| **Metrics** | Cross-cutting + Shared state | ✅ Always | Collect app-wide stats |
| **Auth** | Cross-cutting + Shared? | ✅ Usually | But can be per-request |
| **Transactions** | Cross-cutting + Per-request | ❌ Usually Not | Each request owns transaction |
| **Validation** | Cross-cutting + Stateless | ❌ No | No shared state needed |
| **Config** | Shared state | ✅ Always | Loaded once, read-only |
| **Cache** | Shared state | ✅ Always | Shared data lookup |

---

### Real Interview Answer

**Q: Should all cross-cutting concerns be Singleton?**

A: "Not always. The better principle is: **use Singleton for shared resources that must coordinate state.**

Most cross-cutting concerns ARE shared resources (Logger, Metrics, Config, Auth), so they're Singleton. But some like Transactions are often per-request because each request needs its own transaction scope.

The pattern:
1. **Logging** → Singleton (all modules log to same place)
2. **Metrics** → Singleton (collect app-wide stats)
3. **Auth** → Singleton (shared auth state)
4. **Caching** → Singleton (shared cache lookup)
5. **Config** → Singleton (loaded once)
6. **Transactions** → Per-request (each request has its own transaction)
7. **Validation** → Stateless (no shared state, can be utility)

The distinction: **Singleton if multiple instances break consistency or waste resources. Otherwise, consider dependency injection or per-request scoping for testability.**"

---



#### Repercussions: Multiple Instances When Should Be Singleton

**If you create multiple instances of something that should be Singleton, you get:**

---

### **1. Logger: Lost Logs & File Handle Exhaustion**

**Wrong (Multiple Instances):**
```java
// Every time you log, new instance
Logger logger1 = new Logger();
logger1.init("app.log");
logger1.info("User login");
logger1.close();

Logger logger2 = new Logger();
logger2.init("app.log");
logger2.info("User action");
logger2.close();

Logger logger3 = new Logger();
logger3.init("app.log");
logger3.info("User logout");
logger3.close();

// PROBLEMS:
// ❌ File opened/closed 3 times
// ❌ File handle exhaustion (OS has limit on open files)
// ❌ Performance: overhead of init/close each time
// ❌ Logs might be interleaved or lost if concurrent
```

**Right (Singleton):**
```java
Logger logger = Logger.getInstance();
logger.info("User login");
logger.info("User action");
logger.info("User logout");

// ✅ One file handle, stays open
// ✅ All logs in sequence
// ✅ One buffer, efficient writes
```

---

### **2. Connection Pool: Connection Exhaustion & Deadlock**

**Wrong (Multiple Instances):**
```java
// Each module creates its own pool
ConnectionPool pool1 = new ConnectionPool(maxConnections=10);
// pool1 holds 10 connections

ConnectionPool pool2 = new ConnectionPool(maxConnections=10);
// pool2 holds 10 different connections

ConnectionPool pool3 = new ConnectionPool(maxConnections=10);
// pool3 holds 10 different connections

// Total database connections: 30
// But database max connections limit: 20 ❌ CRASH

// OR: Long-running connection never returned
Connection conn = pool1.getConnection();
// ... code hangs, connection not released

// pool1 runs out of connections
// Other modules trying to get connections DEADLOCK
```

**Right (Singleton):**
```java
ConnectionPool pool = ConnectionPool.getInstance(maxConnections=10);

// Module 1 gets connection
Connection conn1 = pool.getConnection();

// Module 2 gets connection
Connection conn2 = pool.getConnection();

// ... (8 more modules)

// Module 11 waits for a module to return connection
Connection conn11 = pool.getWaitFor();  // Blocks until one is released

// ✅ Total connections: exactly 10, controlled
// ✅ Fair distribution across modules
```

---

### **3. Configuration Manager: Inconsistent Config Across App**

**Wrong (Multiple Instances):**
```java
// Loaded at different times, possibly different values
Config config1 = new Config();
config1.load("app.properties");
String dbUrl1 = config1.get("database.url");  // "mysql://prod-db:3306"

// Later, config file updated in production (bad practice but happens)
// New instance loads new config
Config config2 = new Config();
config2.load("app.properties");
String dbUrl2 = config2.get("database.url");  // "mysql://staging-db:3306" ❌

// PROBLEM:
// ❌ Module A writes to prod-db
// ❌ Module B writes to staging-db
// ❌ Data split across different databases
// ❌ Inconsistent state, corruption
```

**Right (Singleton):**
```java
Config config = Config.getInstance();
// Loaded ONCE at startup

String dbUrl = config.get("database.url");
// All modules get same URL ✅
```

---

### **4. Cache Manager: Cache Misses & Lost Data**

**Wrong (Multiple Instances):**
```java
// Instance 1 caches user data
Cache cache1 = new Cache();
cache1.set("user:123", userData);

// Instance 2 doesn't have this data
Cache cache2 = new Cache();
String userData2 = cache2.get("user:123");  // null ❌

// Code fetches from database AGAIN
userData = database.query("SELECT * FROM users WHERE id=123");

// PROBLEM:
// ❌ Cache miss (should hit!)
// ❌ Redundant database queries
// ❌ Database overload
// ❌ Performance degradation
// ❌ Memory wasted (cached in 2 places)
```

**Right (Singleton):**
```java
Cache cache = CacheManager.getInstance();
cache.set("user:123", userData);

// Later, any module checks cache
String cachedData = cache.get("user:123");  // Hit! ✅

// ✅ One cache, shared data
// ✅ Database query avoided
// ✅ Performance: 1ms cache hit vs 100ms database query
```

---

### **5. Thread Pool: Thread Explosion & OOM**

**Wrong (Multiple Instances):**
```java
// Each component creates its own pool
ExecutorService threadPool1 = Executors.newFixedThreadPool(100);
// Creates 100 threads

ExecutorService threadPool2 = Executors.newFixedThreadPool(100);
// Creates 100 more threads

ExecutorService threadPool3 = Executors.newFixedThreadPool(100);
// Creates 100 more threads

// Total threads: 300
// JVM thread limit: typically 1000-2000
// Each thread consumes ~1MB memory
// 300MB just for threads ❌

// With 10 components, 1000 threads, app crashes with OutOfMemory
```

**Right (Singleton):**
```java
ExecutorService threadPool = ThreadPoolManager.getInstance(size=100);

// All components queue tasks to same 100 threads ✅
threadPool.execute(task1);
threadPool.execute(task2);
// ... hundreds of tasks, but just 100 threads
```

---

### **6. Metrics Collector: Wrong Statistics**

**Wrong (Multiple Instances):**
```java
MetricsCollector metrics1 = new MetricsCollector();
MetricsCollector metrics2 = new MetricsCollector();

// Thread A increments metrics1
metrics1.recordRequest();
metrics1.recordRequest();
metrics1.recordRequest();
// metrics1.totalRequests = 3

// Thread B increments metrics2
metrics2.recordRequest();
metrics2.recordRequest();
// metrics2.totalRequests = 2

// PROBLEM:
// ❌ Actual requests: 5
// ❌ metrics1 shows: 3
// ❌ metrics2 shows: 2
// ❌ Dashboard shows wrong numbers
// ❌ Alerts don't trigger correctly
```

**Right (Singleton):**
```java
MetricsCollector metrics = MetricsCollector.getInstance();

metrics.recordRequest();
metrics.recordRequest();
metrics.recordRequest();
metrics.recordRequest();
metrics.recordRequest();

// metrics.totalRequests = 5 ✅
// Accurate statistics, correct alerting
```

---

### **7. Authentication Manager: Security Breach**

**Wrong (Multiple Instances):**
```java
AuthManager auth1 = new AuthManager();
auth1.login(user, password);
auth1.setCurrentUser(user);  // Authenticated in auth1

// But different request uses auth2
AuthManager auth2 = new AuthManager();
if (auth2.isAuthenticated()) {  // ❌ auth2 doesn't know about auth1!
    // Returns false, user not allowed
}

// PROBLEM:
// ❌ User authenticated in one instance
// ❌ Denied access in another instance
// ❌ Authentication state scattered
// ❌ Sessions confused
```

**Right (Singleton):**
```java
AuthManager auth = AuthManager.getInstance();
auth.login(user, password);

// All requests check same instance
if (auth.isAuthenticated()) {  // ✅ Consistent auth state
    // Allowed
}
```

---

## Summary: Repercussions by Pattern

| Resource | Multiple Instances | Repercussion |
|----------|---|---|
| **Logger** | File handle exhaustion, lost logs, performance hit |
| **Connection Pool** | Database connection limit exceeded, deadlock, crashes |
| **Config** | Inconsistent state, data split across systems |
| **Cache** | Cache misses, database overload, wasted memory |
| **Thread Pool** | Thread explosion, OutOfMemory, app crash |
| **Metrics** | Wrong statistics, incorrect alerts, bad decisions |
| **Auth Manager** | Authentication state confusion, security issues |

---

## Interview Answer

**Q: What happens if you create multiple instances instead of using Singleton?**

A: "Depends on the resource:

1. **Logger**: File handles get exhausted, logs are lost or corrupted.

2. **Connection Pool**: You exceed database connection limits or get deadlocks because each pool holds its own connections instead of sharing.

3. **Config Manager**: Different modules read different configurations, causing data inconsistency across the app.

4. **Cache**: Every instance has its own cache, so lookups fail and you hit the database repeatedly, killing performance.

5. **Thread Pool**: Each instance creates its own threads. With 10 instances of 100-thread pools, you have 1000 threads and app crashes with OutOfMemory.

6. **Metrics**: Statistics are scattered across instances, so dashboards show wrong numbers.

The core issue: **shared resources need one single point of truth**. Multiple instances break that guarantee."

---

---

### Factory Method

**Real Problem:** Object creation logic is complex or varies by subclass. You want subclasses to decide WHAT type of object to create, not the caller.

**Real-World Example:** Payment processor. Each payment provider has different initialization:
```
PaymentProcessor processor = StripeFactory.create();  // Returns StripeProcessor
PaymentProcessor processor = PayPalFactory.create();  // Returns PayPalProcessor
```
Caller doesn't know or care which concrete type. Subclass decides.

**Key Difference from Similar:**
- **vs. Abstract Factory:** 
  - **Factory Method** creates ONE product family. Example: `PaymentFactory` only creates payment processors.
  - **Abstract Factory** creates MULTIPLE related product families together. Example: `DatabaseFactory` creates both `Connection` AND `ConnectionPool` AND `TransactionManager` — all compatible with each other.
  - **Factory Method:** `PaymentFactory.create()` returns a Processor.
  - **Abstract Factory:** `PaymentFactory.create()` returns a whole suite: `{Processor, Validator, Reconciler}`.

- **vs. Constructor:** Factory Method hides creation complexity; constructor is always explicit about the type.

---

### Abstract Factory

**Real Problem:** You need to create FAMILIES of related objects (e.g., UI components for Windows vs. Mac vs. Linux) and ensure they're all compatible. Mixing components from different families breaks consistency.

**Real-World Example:** Database driver factory. Different databases need compatible suites:
```
DatabaseFactory mysqlFactory = new MySQLFactory();
Connection conn = mysqlFactory.createConnection();      // MySQLConnection
StatementBuilder stmt = mysqlFactory.createStatement(); // MySQLStatement
ConnectionPool pool = mysqlFactory.createPool();        // MySQLPool

DatabaseFactory postgresFactory = new PostgresFactory();
Connection conn = postgresFactory.createConnection();   // PostgresConnection
StatementBuilder stmt = postgresFactory.createStatement(); // PostgresStatement
ConnectionPool pool = postgresFactory.createPool();     // PostgresPool
```
Switch factories, get entire compatible suite. Never mix MySQL connection with Postgres statement.

**Key Difference from Similar:**
- **vs. Factory Method:**
  - **Factory Method** creates ONE product. Example: `PaymentFactory.create()` returns just a `PaymentProcessor`.
  - **Abstract Factory** creates MULTIPLE related products. Example: `DatabaseFactory.create()` returns `{Connection, Statement, Pool, Metadata}` — all for the same database.
  - **Use Factory Method** when: You have ONE product type that varies (payment providers).
  - **Use Abstract Factory** when: You have MULTIPLE product types that must work together (database drivers, UI toolkits, cloud providers).

- **vs. Builder:** Abstract Factory creates objects immediately; Builder builds ONE object step-by-step with a fluent API.

---

### Builder

**Real Problem:** Object construction has many optional parameters, complex initialization steps, or you want to build immutable objects safely.

**Real-World Example:** HTTP request. Instead of `new Request(method, url, headers, body, timeout, retries, auth)`, you use `RequestBuilder().setMethod(...).setUrl(...).build()`. Easy to read, hard to mess up.

**Key Difference from Similar:**
- **vs. Constructor:** Builder handles optional parameters cleanly; constructor becomes unreadable with 10+ params.
- **vs. Abstract Factory:** Builder constructs ONE object step-by-step; Abstract Factory creates multiple related objects at once.

---

### Prototype

**Real Problem:** Creating new objects from scratch is expensive (database queries, network calls, heavy computation). You want to CLONE existing objects instead.

**Real-World Example:** Configuration object. Instead of parsing config file each time, clone the pre-loaded config and modify a copy.

**Key Difference from Similar:**
- **vs. Singleton:** Singleton creates ONE instance; Prototype creates MANY independent copies.
- **vs. Clone/Copy Constructor:** Prototype formalizes the cloning interface across your codebase.

---

## Structural Patterns

### Adapter

**Real Problem:** You have existing code that works with Interface A, but you need to use a third-party library that exposes Interface B. They're incompatible.

**Real-World Example:** Legacy logging system expects `void log(String msg)`. You want to use modern SLF4J logger that has `void debug(String msg)`. Write an Adapter wrapper.

**Key Difference from Similar:**
- **vs. Decorator:** Adapter changes the interface; Decorator adds behavior to same interface.
- **vs. Bridge:** Adapter connects incompatible interfaces; Bridge decouples abstraction from implementation.

---

### Bridge

**Real Problem:** You have multiple dimensions of variation (e.g., Shape + Color, or Device + Platform). Inheritance tree explodes. You need to decouple them.

**Real-World Example:** Renderer abstraction. `Shape` is decoupled from `Renderer`. You have `CircleRenderer`, `SquareRenderer` (shapes) and `CanvasRenderer`, `SvgRenderer` (renderers). Mix them freely without creating `CircleCanvasRenderer`, `CircleSvgRenderer`, `SquareCanvasRenderer`, etc.

**Key Difference from Similar:**
- **vs. Adapter:** Bridge decouples related but variant interfaces; Adapter bridges incompatible ones.
- **vs. Decorator:** Bridge creates a runtime-switchable implementation; Decorator wraps behavior.

---

### Composite

**Real Problem:** You have tree structures (file system, DOM, organizational hierarchy) where leaf and branch nodes have the SAME operations (delete, copy, calculate size).

**Real-World Example:** File system. `File` and `Directory` both implement `FileNode`. You call `delete()` on a Directory, it recursively deletes all children. Same interface, different behavior.

**Key Difference from Similar:**
- **vs. Decorator:** Composite builds trees; Decorator wraps a single object.
- **vs. Iterator:** Composite IS the structure; Iterator traverses it.

---

### Decorator

**Real Problem:** You want to add behavior to an object dynamically without modifying its class. Examples: compression, encryption, logging, UI enhancements.

**Real-World Example:** Stream I/O. `FileInputStream` wrapped in `BufferedInputStream` wrapped in `GZipInputStream`. Each decorator adds buffering or compression without touching the original stream.

**Key Difference from Similar:**
- **vs. Subclassing:** Decorator adds behavior at runtime; subclass is fixed at compile time.
- **vs. Composite:** Decorator wraps one object; Composite is a tree of objects.

---

### Facade

**Real Problem:** You have a complex subsystem (e.g., payment processing, video encoding, order fulfillment) with many interdependent classes. You want a simple entry point.

**Real-World Example:** Payment facade. Internally, it coordinates `CurrencyConverter` → `FraudDetector` → `PaymentGateway` → `AuditLogger`. Caller just calls `processPayment(order)`.

**Key Difference from Similar:**
- **vs. Adapter:** Facade simplifies a complex system; Adapter bridges incompatible interfaces.
- **vs. Mediator:** Facade is one-way (client calls facade); Mediator is two-way (components talk to each other through mediator).

---

### Flyweight

**Real Problem:** You're creating millions of similar objects (e.g., characters in a text editor, particles in a game, tree leaves in a forest) and running out of memory.

**Real-World Example:** Text editor. Instead of creating a separate `Character` object for each letter, create ONE `CharacterFlyweight` per character type and share it. Store position/style separately.

**Key Difference from Similar:**
- **vs. Singleton:** Singleton creates one instance globally; Flyweight creates a few instances and reuses them across many contexts.
- **vs. Object Pool:** Object Pool recycles objects over time; Flyweight shares immutable objects concurrently.

---

### Proxy

**Real Problem:** You need a surrogate for another object to control access, delay expensive operations, or add security.

**Real-World Example:** Database proxy. Real database queries are expensive. Proxy caches results. Or: security proxy checks permissions before allowing actual object access.

**Key Difference from Similar:**
- **vs. Decorator:** Proxy controls access/lazy loading; Decorator adds behavior.
- **vs. Facade:** Proxy represents ONE object; Facade simplifies a complex SUBSYSTEM.

---

## Behavioral Patterns

### Chain of Responsibility

**Real Problem:** Multiple objects can handle a request, but you don't know WHICH one upfront. You want to pass the request along a chain until someone handles it.

**Real-World Example:** Approval workflow. Request goes to `Manager` → `Director` → `VP`. Each checks if they can approve; if not, passes to next.

**Key Difference from Similar:**
- **vs. Command:** Command encapsulates a request; Chain of Responsibility passes it through handlers.
- **vs. Mediator:** Chain is linear; Mediator is centralized hub.

---

### Command

**Real Problem:** You need to decouple the sender of a request from the receiver. Examples: undo/redo, job queues, macro recording.

**Real-World Example:** Text editor undo/redo. Each edit (Insert, Delete, Format) is a Command object. Undo stack stores them. Pop and call `undo()`.

**Key Difference from Similar:**
- **vs. Strategy:** Command encapsulates an ACTION and can be queued/undone; Strategy encapsulates an ALGORITHM that's swapped at runtime.
- **vs. Callback:** Command is an object; Callback is a function reference.

---

### Interpreter

**Real Problem:** You need to parse and execute a custom language or domain-specific syntax (SQL, regex, config format, DSL).

**Real-World Example:** SQL parser. Input `SELECT * FROM Users WHERE age > 18` becomes an AST (Abstract Syntax Tree) of Interpreter objects that can be executed.

**Key Difference from Similar:**
- **vs. Parser (general):** Interpreter is a design pattern for building ASTs; Parsers are a broader concept.
- **vs. Strategy:** Interpreter defines grammar rules; Strategy chooses algorithms.

---

### Iterator

**Real Problem:** You want to traverse a collection (list, tree, graph) without exposing its internal structure. You want different traversal methods (breadth-first, depth-first).

**Real-World Example:** Tree traversal. `DepthFirstIterator` vs. `BreadthFirstIterator`. Same tree, different iteration order. Caller doesn't know the tree's internal structure.

**Key Difference from Similar:**
- **vs. Composite:** Composite IS the structure; Iterator traverses it.
- **vs. Visitor:** Iterator traverses; Visitor performs an operation ON each element.

---

### Mediator

**Real Problem:** Multiple objects communicate with each other, creating a tangled web of dependencies. You want a central coordinator.

**Real-World Example:** Air traffic control. Planes don't communicate with each other; they communicate with the control tower (Mediator). Tower coordinates landings, takeoffs, routing.

**Key Difference from Similar:**
- **vs. Facade:** Facade is one-way (client calls); Mediator is two-way (objects coordinate through it).
- **vs. Observer:** Observer is one-to-many (one object notifies many); Mediator is many-to-many coordination.

---

### Memento

**Real Problem:** You need to capture and restore an object's state without violating encapsulation. Examples: undo, save points, transaction rollback.

**Real-World Example:** Game save point. Game state (position, health, inventory) is captured in a `Memento`. Player can restore to that checkpoint without directly manipulating game internals.

**Key Difference from Similar:**
- **vs. Command:** Command records ACTIONS; Memento records STATE.
- **vs. Serialization:** Memento is designed for undo/save within the app; Serialization is for persistence.

---

### Observer

**Real Problem:** Multiple objects care about state changes in one object. You want loose coupling (watchers don't need to know each other).

**Real-World Example:** Stock price ticker. Many traders are watching Apple stock. Price changes, all traders get notified immediately. Traders don't call back to check.

**Key Difference from Similar:**
- **vs. Mediator:** Observer is one-to-many; Mediator is many-to-many coordination.
- **vs. Pub/Sub:** Observer is tightly coupled (subject knows observers); Pub/Sub is loosely coupled (message broker in middle).

---

### State

**Real Problem:** Object behavior changes based on internal state. Instead of massive if/else conditionals, delegate to state objects.

**Real-World Example:** TCP connection. `Established` state handles data; `Listening` state handles connections; `Closed` state rejects all. Each is its own class.

**Key Difference from Similar:**
- **vs. Strategy:** State changes over object lifetime; Strategy is chosen by caller.
- **vs. Command:** State encapsulates how to BEHAVE; Command encapsulates what ACTION to perform.

---

### Strategy

**Real Problem:** You have multiple algorithms for the same task and want to swap them at runtime without modifying the client.

**Real-World Example:** Sorting. `QuickSort`, `MergeSort`, `HeapSort` all implement `SortingStrategy`. App chooses based on data size and picks the best algorithm.

**Key Difference from Similar:**
- **vs. State:** Strategy is chosen by caller; State changes over object lifetime.
- **vs. Factory Method:** Factory Method creates objects; Strategy encapsulates algorithms.

---

### Template Method

**Real Problem:** Multiple subclasses have the same overall workflow, but individual steps differ. You want to avoid code duplication.

**Real-World Example:** Data processing. Base class defines workflow: `load() → validate() → transform() → save()`. Subclasses override only the steps they differ on.

**Key Difference from Similar:**
- **vs. Strategy:** Template Method uses inheritance; Strategy uses composition and is more flexible.
- **vs. Decorator:** Template Method defines a skeleton; Decorator adds behavior.

---

### Visitor

**Real Problem:** You have a complex tree/graph and want to perform different operations on each node type WITHOUT adding methods to those node classes.

**Real-World Example:** AST operations. You have `IfNode`, `LoopNode`, `ExpressionNode`. Write `CodeGeneratorVisitor`, `DebugPrinterVisitor`, `OptimizationVisitor` that each handle all node types.

**Key Difference from Similar:**
- **vs. Iterator:** Iterator traverses; Visitor performs operations ON each element.
- **vs. Composite:** Both work on trees, but Visitor separates operations into Visitor classes.

---

## Deep Dive: Factory Method vs. Abstract Factory

This is THE most confused pair in GoF patterns. Here's the tech-architect way to think about it:

### **Factory Method = One Product Type**

**What it does:**
- Creates ONE type of object
- Varies based on context (subclass decides which concrete type)
- Caller doesn't know or care about the concrete class

**Tech example:**
```
// Factory Method
PaymentProcessor processor = PaymentFactory.createProcessor(provider);

// Returns one of:
// - StripeProcessor
// - PayPalProcessor
// - SquareProcessor

// Caller just uses: processor.charge(amount);
```

**When to use:**
- Payment gateways (one processor per provider)
- Database drivers (one connection type per database)
- Log handlers (FileLogger vs. ConsoleLogger vs. RemoteLogger)

---

### **Abstract Factory = Multiple Related Product Families**

**What it does:**
- Creates MULTIPLE related objects that work together
- Entire suite changes based on context (factory switches all products at once)
- Guarantees all products are compatible

**Tech example:**
```
// Abstract Factory
DatabaseFactory factory = DatabaseFactory.get("mysql");

Connection conn = factory.createConnection();           // MySQLConnection
Statement stmt = factory.createStatement();             // MySQLStatement  
Pool pool = factory.createPool();                       // MySQLPool
Metadata meta = factory.createMetadata();               // MySQLMetadata

// Switch to Postgres:
DatabaseFactory factory = DatabaseFactory.get("postgres");

Connection conn = factory.createConnection();           // PostgresConnection
Statement stmt = factory.createStatement();             // PostgresStatement
Pool pool = factory.createPool();                       // PostgresPool
Metadata meta = factory.createMetadata();               // PostgresMetadata

// All products changed consistently. Never mix MySQL conn with Postgres stmt.
```

**When to use:**
- Database drivers (Connection + Statement + Pool + Metadata all compatible)
- UI toolkits (Button + Checkbox + TextField all use same look & feel)
- Cloud providers (EC2Instance + S3Bucket + RDSDatabase all from AWS)
- UI themes (Dark mode: dark button, dark checkbox, dark textfield)

---

### **Quick Decision Tree**

```
Are you creating ONE product type that varies?
├─ YES → Factory Method
│        Example: PaymentProcessor (Stripe vs. PayPal)
│
└─ NO: Are you creating MULTIPLE related products that must work together?
       ├─ YES → Abstract Factory
       │        Example: Database suite (Connection + Statement + Pool)
       │
       └─ NO: Consider other patterns (Builder, Prototype, Singleton)
```

---

### **Interview Answer Format**

**Q: When would you use Factory Method over Abstract Factory?**

A: "Factory Method when you have ONE product that varies. Example: payment processors. You pick Stripe or PayPal, but that's one decision. Abstract Factory when you have MULTIPLE products that must be compatible. Example: database drivers — you don't just pick 'MySQL connection', you need the MySQL suite: connection, statement, pool, metadata. If I only needed to vary ONE thing, I'd use Factory Method. If I need multiple things to stay compatible, Abstract Factory."

---

### **Real-World Tech Examples**

#### Factory Method Examples (One Product Type)

**1. Notification Service**
```
NotificationService notif = NotificationFactory.create("email");
notif.send("user@example.com", "Hello");

// vs.

NotificationService notif = NotificationFactory.create("sms");
notif.send("+1234567890", "Hello");

// Just ONE product type: NotificationService
// But implementation varies
```

**2. Cache Implementation**
```
Cache cache = CacheFactory.create("redis");    // RedisCache
cache.set("key", "value");

// vs.

Cache cache = CacheFactory.create("memcached"); // MemcachedCache
cache.set("key", "value");

// vs.

Cache cache = CacheFactory.create("memory");   // InMemoryCache
cache.set("key", "value");

// Just ONE product: Cache
```

**3. Image Processor**
```
ImageProcessor processor = ImageFactory.create("jpeg");
processor.compress(image, 0.8);

// vs.

ImageProcessor processor = ImageFactory.create("png");
processor.compress(image, 0.8);

// vs.

ImageProcessor processor = ImageFactory.create("webp");
processor.compress(image, 0.8);

// Just ONE product: ImageProcessor
```

**4. Authentication Provider**
```
AuthProvider auth = AuthFactory.create("oauth");      // OAuthProvider
auth.authenticate(token);

// vs.

AuthProvider auth = AuthFactory.create("ldap");       // LDAPProvider
auth.authenticate(credentials);

// vs.

AuthProvider auth = AuthFactory.create("jwt");        // JWTProvider
auth.authenticate(token);

// Just ONE product: AuthProvider
```

---

#### Abstract Factory Examples (Multiple Related Products)

**1. Database Driver**
```
// MySQL Suite
DatabaseFactory mysql = DatabaseFactory.get("mysql");
Connection conn = mysql.createConnection();              // MySQLConnection
PreparedStatement stmt = mysql.createStatement();        // MySQLStatement
ConnectionPool pool = mysql.createPool();                // MySQLPool
DatabaseMetadata meta = mysql.createMetadata();          // MySQLMetadata

// vs.

// PostgreSQL Suite
DatabaseFactory postgres = DatabaseFactory.get("postgres");
Connection conn = postgres.createConnection();           // PostgresConnection
PreparedStatement stmt = postgres.createStatement();     // PostgresStatement
ConnectionPool pool = postgres.createPool();             // PostgresPool
DatabaseMetadata meta = postgres.createMetadata();       // PostgresMetadata

// Multiple products, all compatible within same database family
```

**2. Cloud Provider**
```
// AWS Suite
CloudFactory aws = CloudFactory.get("aws");
ComputeService compute = aws.createCompute();       // EC2
StorageService storage = aws.createStorage();       // S3
DatabaseService database = aws.createDatabase();    // RDS
NetworkService network = aws.createNetwork();       // VPC

// vs.

// Azure Suite
CloudFactory azure = CloudFactory.get("azure");
ComputeService compute = azure.createCompute();     // VirtualMachine
StorageService storage = azure.createStorage();     // Blob
DatabaseService database = azure.createDatabase();  // Database
NetworkService network = azure.createNetwork();     // VirtualNetwork

// Switch clouds, ALL services change consistently
```

**3. UI Theme**
```
// Dark Theme Suite
UIFactory dark = UIFactory.getTheme("dark");
Button button = dark.createButton();                 // DarkButton
TextField textField = dark.createTextField();        // DarkTextField
CheckBox checkBox = dark.createCheckBox();           // DarkCheckBox
Menu menu = dark.createMenu();                       // DarkMenu

// vs.

// Light Theme Suite
UIFactory light = UIFactory.getTheme("light");
Button button = light.createButton();                // LightButton
TextField textField = light.createTextField();       // LightTextField
CheckBox checkBox = light.createCheckBox();          // LightCheckBox
Menu menu = light.createMenu();                      // LightMenu

// Switch themes, ALL UI components change together, maintaining consistency
```

**4. ORM (Object-Relational Mapping)**
```
// Hibernate Suite
ORMFactory orm = ORMFactory.get("hibernate");
Session session = orm.createSession();               // HibernateSession
Query query = orm.createQuery();                     // HibernateQuery
Transaction txn = orm.createTransaction();           // HibernateTransaction
Metadata metadata = orm.createMetadata();            // HibernateMetadata

// vs.

// Dapper Suite (Micro-ORM)
ORMFactory orm = ORMFactory.get("dapper");
Session session = orm.createSession();               // DapperSession
Query query = orm.createQuery();                     // DapperQuery
Transaction txn = orm.createTransaction();           // DapperTransaction
Metadata metadata = orm.createMetadata();            // DapperMetadata

// All components work together within chosen ORM framework
```

**5. Document Generator**
```
// PDF Suite
DocumentFactory pdf = DocumentFactory.get("pdf");
Document doc = pdf.createDocument();                 // PDFDocument
Font font = pdf.createFont();                        // PDFFont
Table table = pdf.createTable();                     // PDFTable
Image image = pdf.createImage();                     // PDFImage

// vs.

// Excel Suite
DocumentFactory excel = DocumentFactory.get("excel");
Document doc = excel.createDocument();               // ExcelWorkbook
Font font = excel.createFont();                      // ExcelFont
Table table = excel.createTable();                   // ExcelSheet
Image image = excel.createImage();                   // ExcelImage

// All products compatible within their format
```

**6. Message Queue**
```
// Kafka Suite
MessageQueueFactory kafka = MessageQueueFactory.get("kafka");
Producer producer = kafka.createProducer();          // KafkaProducer
Consumer consumer = kafka.createConsumer();          // KafkaConsumer
Broker broker = kafka.createBroker();                // KafkaBroker
Topic topic = kafka.createTopic();                   // KafkaTopic

// vs.

// RabbitMQ Suite
MessageQueueFactory rabbit = MessageQueueFactory.get("rabbitmq");
Producer producer = rabbit.createProducer();         // RabbitMQProducer
Consumer consumer = rabbit.createConsumer();         // RabbitMQConsumer
Broker broker = rabbit.createBroker();               // RabbitMQBroker
Topic topic = rabbit.createTopic();                  // RabbitMQExchange

// All components work together, different queue systems
```

---

## How to Identify Factory vs Abstract Factory Problems

### Diagnostic Questions (Ask in This Order)

**Question 1: What am I varying?**
- "Am I picking WHICH service/provider to use?"
- Examples: Which payment processor? Which cache? Which logger?

**Question 2: How many things change when I pick?**
- **ONE thing changes** → Factory Method ✅
  - "When I pick Stripe, I get ONE payment processor object"
  - "When I pick Redis, I get ONE cache object"
  
- **MULTIPLE things change TOGETHER** → Abstract Factory ✅
  - "When I pick MySQL, I get Connection + Statement + Pool + Metadata"
  - "When I pick AWS, I get EC2 + S3 + RDS + VPC"

**Question 3: Do these objects need to work together?**
- **NO** → Likely Factory Method
  - Payment processor works alone
  - Cache works alone
  - Logger works alone
  
- **YES, they must be compatible** → Likely Abstract Factory
  - MySQL Connection must work with MySQL Statement
  - AWS EC2 must work with AWS S3
  - Dark button must match dark textfield

---

### Red Flags by Pattern

#### Red Flags for Factory Method
```
✓ "We support multiple X providers"
✓ "Different implementations of the same interface"
✓ "Pick one, get one"
✓ "Each option is independent"
✓ Problem: "Support Stripe AND PayPal AND Square"
```

#### Red Flags for Abstract Factory
```
✓ "Complete suite of related components"
✓ "Switch everything at once"
✓ "Multiple interfaces/types that depend on each other"
✓ "Family of products that must be compatible"
✓ Problem: "Support MySQL database OR PostgreSQL database (with ALL tools)"
```

---

### Identification Flowchart

```
Problem Statement: "We need to support X and Y and Z"

Step 1: What are X, Y, Z?
├─ Payment processors (Stripe, PayPal, Square)?
│  └─ ONE product type that varies → FACTORY METHOD
│
├─ Different databases (MySQL, Postgres, Oracle)?
│  └─ Multiple related products → ABSTRACT FACTORY
│
├─ Different UI frameworks (Windows, Mac, Linux)?
│  └─ Multiple related UI components → ABSTRACT FACTORY
│
├─ Different caches (Redis, Memcached, Memory)?
│  └─ ONE product type that varies → FACTORY METHOD
│
├─ Different cloud providers (AWS, Azure, GCP)?
│  └─ Multiple related services → ABSTRACT FACTORY
│
└─ Different notification channels (Email, SMS, Slack)?
   └─ ONE product type that varies → FACTORY METHOD
```

---

### Real Interview Scenarios

#### Scenario 1: "Design a payment system"

**Red flag questions to ask interviewer:**

Q: "Do we need to support multiple payment providers?"
A: "Yes, Stripe, PayPal, and Square"

Q: "When we switch from Stripe to PayPal, how many classes change?"
A: "Just the payment processor"

Q: "Do they need to work together?"
A: "No, we pick one and use it"

**Diagnosis:** ONE product varies → **FACTORY METHOD** ✅

---

#### Scenario 2: "Design a database abstraction layer"

**Red flag questions to ask interviewer:**

Q: "Do we need to support different databases?"
A: "Yes, MySQL and PostgreSQL"

Q: "When we switch databases, do we need just one object or multiple?"
A: "Multiple. We need connections, statements, pools, metadata handlers..."

Q: "Must all these objects be compatible with each other?"
A: "Absolutely. A MySQL connection must work with MySQL statements"

Q: "Can we mix MySQL connection with Postgres statement?"
A: "No, that breaks"

**Diagnosis:** MULTIPLE related products → **ABSTRACT FACTORY** ✅

---

#### Scenario 3: "Design a logger"

**Red flag questions to ask interviewer:**

Q: "Do we need to support different destinations?"
A: "Yes, file, console, remote server"

Q: "When we switch from file logger to console logger, what objects change?"
A: "Just the logger implementation"

Q: "Do these need to work together or communicate?"
A: "No, we pick one for the entire app"

**Diagnosis:** ONE product varies → **FACTORY METHOD** ✅

---

#### Scenario 4: "Design a UI framework"

**Red flag questions to ask interviewer:**

Q: "Do we need to support different operating systems?"
A: "Yes, Windows, Mac, Linux"

Q: "When we target Windows, what components do we need?"
A: "Button, TextField, CheckBox, Menu, all Windows-styled"

Q: "When we switch to Mac, do we need just one component or many?"
A: "All of them need to be Mac-styled"

Q: "Can we mix Windows button with Mac textfield?"
A: "No, the UI would look inconsistent"

**Diagnosis:** MULTIPLE related products → **ABSTRACT FACTORY** ✅

---

### Cheat Sheet for Quick Diagnosis

| Indicator | Factory Method | Abstract Factory |
|-----------|---|---|
| **Varying** | ONE product type | MULTIPLE related products |
| **Decision** | "Pick Stripe" | "Pick MySQL" |
| **Result** | One object | Multiple compatible objects |
| **Mix & match?** | No problem | BREAKS consistency |
| **Examples** | Payment, Cache, Logger | Database, Cloud, UI Theme |
| **Interview Shortcut** | "One choice, one object" | "One choice, many coordinated objects" |

---

### What to Say in Interview

**When problem statement is unclear:**

"Let me clarify: When we switch implementations, do we get ONE product or MULTIPLE related products?"

**If ONE:** "This sounds like Factory Method. We vary the processor/logger/cache independently."

**If MULTIPLE:** "This sounds like Abstract Factory. We need a suite of compatible components that all change together."

**If still unclear:** "Can I ask: if we pick MySQL, must the connection work with the statement, and must the pool work with both? Or is each independent?"

---

## Interview Tips

1. **Mention the real problem first** — "This pattern solves X" shows you understand WHY, not just HOW.
2. **Use 1-2 concrete examples** — Not textbook examples. Real production code.
3. **Distinguish from similar patterns** — Shows deep understanding and helps interviewer assess your breadth.
4. **Be ready with trade-offs** — "Proxy adds indirection; be careful with performance."

