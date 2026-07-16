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

## Interview Tips

1. **Mention the real problem first** — "This pattern solves X" shows you understand WHY, not just HOW.
2. **Use 1-2 concrete examples** — Not textbook examples. Real production code.
3. **Distinguish from similar patterns** — Shows deep understanding and helps interviewer assess your breadth.
4. **Be ready with trade-offs** — "Proxy adds indirection; be careful with performance."

