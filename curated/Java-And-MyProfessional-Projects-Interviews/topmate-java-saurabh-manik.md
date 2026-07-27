# Java Interview Q&A - Saurabh Manik (TopMate)
**For Solution Architect (10-20 years experience)**

---

## Table of Contents
1. [Core Java - Collections](#core-java---collections)
2. [Core Java - Multi-Threading](#core-java---multi-threading)
3. [Other Topics](#other-topics)
   - [JWT Token](#jwt-token)
   - [Design Patterns](#design-patterns)
   - [Microservices](#microservices)
   - [Spring Framework](#spring-framework)
   - [System Design - WhatsApp](#system-design---whatsapp)

---

## Core Java - Collections

### Q1: BlockingQueue - When and Why?
**Answer:**
A `BlockingQueue` is a thread-safe queue implementation that supports operations that wait for the queue to be non-empty when retrieving elements, and wait for space to be available in the queue when storing elements.

**Key Characteristics:**
- Blocks on `take()` if queue is empty (waits for producer)
- Blocks on `put()` if queue is full (waits for consumer)
- No busy-waiting; uses internal locks and conditions

**When to Use:**
- **Producer-Consumer Problem**: Classic pattern where threads produce and consume data
- **Thread Pools**: Work queues in ThreadPoolExecutor (LinkedBlockingQueue, ArrayBlockingQueue)
- **Rate Limiting**: Bounded queues control concurrency and backpressure
- **Decoupling**: Decouple producers from consumers; different processing rates

**Real-world Scenario:**
```java
BlockingQueue<Message> queue = new LinkedBlockingQueue<>(1000);

// Producer
new Thread(() -> {
    while (true) {
        Message msg = fetchFromKafka();
        queue.put(msg); // Blocks if queue full
    }
}).start();

// Consumer
new Thread(() -> {
    while (true) {
        Message msg = queue.take(); // Blocks if queue empty
        processMessage(msg);
    }
}).start();
```

**Architect Considerations:**
- Choose bounded vs unbounded based on memory constraints
- `LinkedBlockingQueue`: Unbounded, better for throughput
- `ArrayBlockingQueue`: Bounded, better for backpressure control
- Monitor queue depth for system health

---

### Q2: PriorityQueue vs TreeSet - Which to Use?

| Aspect | PriorityQueue | TreeSet |
|--------|---------------|---------|
| **Data Structure** | Min-heap (binary tree) | Red-Black Tree |
| **Order** | Natural order via Comparator | Sorted order |
| **Duplicates** | Allowed | Not allowed |
| **Thread-Safe** | No | No |
| **Time Complexity** | O(log n) poll/offer | O(log n) add/remove |
| **Use Case** | Task scheduling, Dijkstra | Range queries, unique sorted data |

**When to Use PriorityQueue:**
```java
// Task scheduling with priorities
PriorityQueue<Task> pq = new PriorityQueue<>((a, b) -> b.priority - a.priority);

// Dijkstra's shortest path algorithm
PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.distance));
```

**When to Use TreeSet:**
```java
// Maintain sorted unique inventory IDs
TreeSet<Integer> inventoryIds = new TreeSet<>();

// Range queries
NavigableSet<String> subset = treeSet.subSet("apple", "zebra");
```

**Architect Perspective:**
- **PriorityQueue**: Better for algorithms requiring repeated min/max extraction
- **TreeSet**: Better for datasets requiring range queries and uniqueness guarantees
- Both are non-blocking; use `PriorityBlockingQueue` in multi-threaded scenarios

---

### Q3: hashCode and equals - The Contract

**The Contract:**
1. If `a.equals(b)` is true, then `a.hashCode() == b.hashCode()` (mandatory)
2. If `a.hashCode() == b.hashCode()`, `a.equals(b)` may be true or false (hash collision)
3. If `a.equals(b)` is false, `a.hashCode()` may or may not be equal (allowed)

**Why This Matters:**
```java
// Breaking the contract leads to bugs
public class BadId {
    private int id;
    
    @Override
    public boolean equals(Object o) {
        return id == ((BadId)o).id;
    }
    // WRONG: No hashCode override - uses default Object.hashCode()
}

// HashMap will put BadId(1) in one bucket, but lookup for BadId(1) searches another bucket
HashMap<BadId, String> map = new HashMap<>();
map.put(new BadId(1), "value");
System.out.println(map.get(new BadId(1))); // null - BUG!
```

**Correct Implementation:**
```java
public class CorrectId {
    private int id;
    private String name;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CorrectId)) return false;
        CorrectId that = (CorrectId) o;
        return id == that.id && Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
```

**Architect Guidelines:**
- Use IDE-generated implementations (IntelliJ, Eclipse)
- For JPA entities, only include business key fields, not lazy-loaded relations
- Immutable fields = better hash functions (no modification issues)
- Be careful with nullable fields in collections (can cause lookup failures)

---

### Q4: Comparator vs Comparable - Design Perspective

**Comparable (Internal Ordering):**
```java
public class Employee implements Comparable<Employee> {
    private int id;
    private String name;
    
    @Override
    public int compareTo(Employee other) {
        return this.id - other.id; // Natural ordering by ID
    }
}

// Used by Collections.sort() and TreeSet
List<Employee> employees = new ArrayList<>();
Collections.sort(employees); // Uses compareTo()
```

**Comparator (External Ordering):**
```java
// Multiple sorting strategies without modifying Employee
Comparator<Employee> bySalary = Comparator.comparingInt(Employee::getSalary);
Comparator<Employee> byName = Comparator.comparing(Employee::getName);

employees.sort(bySalary);
employees.sort(byName);
employees.sort(bySalary.thenComparing(byName)); // Chaining
```

**Architect Decision Matrix:**

| Scenario | Use |
|----------|-----|
| One natural order (User by ID) | Comparable |
| Multiple sort orders (by ID, name, salary) | Comparator |
| Third-party classes | Comparator (can't modify) |
| Evolving requirements | Comparator (more flexible) |

**Gotcha - Broken Comparator:**
```java
// WRONG - Can overflow
Comparator<Integer> bad = (a, b) -> a - b; // -1 - Integer.MAX_VALUE overflows

// CORRECT
Comparator<Integer> good = Integer::compare;
```

---

## Core Java - Multi-Threading

### Q5: ConcurrentHashMap vs Synchronized Map - Deep Dive

**Synchronized Map (Using Collections.synchronizedMap):**
```java
Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
// Entire map is locked during any operation
```

| Aspect | Synchronized Map | ConcurrentHashMap |
|--------|------------------|-------------------|
| **Locking** | One lock for entire map | Multiple locks (buckets/segments) |
| **Throughput** | Low (high contention) | High (low contention) |
| **Iteration** | Must manually synchronize | Safe iteration (weakly consistent) |
| **Put/Get** | O(1) but blocks others | O(1) minimal blocking |
| **Use Case** | Simple single-threaded scenarios | High-concurrency applications |

**ConcurrentHashMap Internals (Java 8+):**
```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

// Bucket-level locking (not entry-level)
// Uses CAS (Compare-And-Swap) operations when possible
// Default capacity: 16, concurrency level: auto-adjusted

// Safe iteration - doesn't throw ConcurrentModificationException
for (String key : map.keySet()) {
    map.put(key + "_new", 1); // No exception (weakly consistent)
}
```

**Performance Comparison (Producer-Consumer Scenario):**
```java
// Synchronized Map: ~2000 ops/sec (with 10 threads)
Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());

// ConcurrentHashMap: ~50000 ops/sec (with 10 threads)
ConcurrentHashMap<String, Integer> concMap = new ConcurrentHashMap<>();
```

**Architect Recommendation:**
- Default to `ConcurrentHashMap` in multi-threaded scenarios
- `ConcurrentHashMap.putIfAbsent()` for lazy initialization without double-locking
- `ConcurrentHashMap.compute()` for atomic read-modify-write operations

---

### Q6: ReentrantLock vs Lock Interface - When to Use?

**Lock Interface (java.util.concurrent.locks.Lock):**
```java
public interface Lock {
    void lock();           // Blocks indefinitely
    void lockInterruptibly() throws InterruptedException; // Can be interrupted
    boolean tryLock();     // Non-blocking, returns immediately
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;
    Condition newCondition();
}
```

**ReentrantLock Implementation:**
```java
Lock lock = new ReentrantLock();
Lock fairLock = new ReentrantLock(true); // Fair lock - FIFO ordering

try {
    lock.lock();
    // Critical section
} finally {
    lock.unlock(); // MUST unlock in finally block
}

// Or with try-with-resources (Java 7+)
try (var ignored = new LockGuard(lock)) {
    // Critical section
}
```

**When to Use Lock over synchronized:**

| Use Case | Example |
|----------|---------|
| **Timeout-based locking** | `tryLock(1, TimeUnit.SECONDS)` - Avoid deadlocks |
| **Interruptible locking** | `lockInterruptibly()` - Thread cancellation |
| **Multiple conditions** | Different wait/notify per condition |
| **Fair scheduling** | Prevent thread starvation |
| **Biased locking avoidance** | Explicit control over lock behavior |

**Real-world Example - Fair Scheduling:**
```java
// Thread starvation scenario - some threads never get lock
synchronized void criticalSection() { }

// Solution: Fair lock ensures all threads eventually acquire lock
private final Lock fairLock = new ReentrantLock(true);

void criticalSection() {
    fairLock.lock();
    try {
        // Critical section
    } finally {
        fairLock.unlock();
    }
}
```

**Architect Considerations:**
- `synchronized` is simpler and has JVM optimizations (biased locking, escape analysis)
- `Lock` provides more control for complex scenarios
- Prefer `synchronized` unless you need specific Lock features
- Always use try-finally with Lock to ensure unlock

---

### Q7: Semaphore - Use Cases and Patterns

**Semaphore Concept:**
```java
Semaphore semaphore = new Semaphore(3); // 3 permits
// Acts like a counter that can go negative
// acquire() -> counter--, blocks if counter < 0
// release() -> counter++, wakes up blocked threads
```

**Use Case 1: Connection Pool (Resource Limiting):**
```java
public class ConnectionPool {
    private final Semaphore semaphore = new Semaphore(10); // 10 connections
    private final BlockingQueue<Connection> pool = new LinkedBlockingQueue<>();
    
    public Connection acquire() throws InterruptedException {
        semaphore.acquire(); // Wait for available connection
        return pool.take();
    }
    
    public void release(Connection conn) {
        pool.put(conn);
        semaphore.release(); // Make permit available
    }
}
```

**Use Case 2: Rate Limiting:**
```java
public class ApiRateLimiter {
    private final Semaphore limiter = new Semaphore(100); // 100 requests
    
    public void callApi() throws InterruptedException {
        limiter.acquire();
        try {
            // Make API call
        } finally {
            limiter.release();
        }
    }
    
    // Reset permits every second (token bucket simulation)
    public void resetLimits() {
        int currentPermits = 100 - limiter.availablePermits();
        semaphore.release(currentPermits);
    }
}
```

**Use Case 3: Multi-permit Semaphore (Binary Semaphore = Mutex):**
```java
Semaphore mutex = new Semaphore(1); // Acts like a lock
```

---

### Q8: CountDownLatch vs CyclicBarrier - The Difference

| Aspect | CountDownLatch | CyclicBarrier |
|--------|----------------|---------------|
| **Purpose** | One-shot countdown to 0 | Reusable barrier for N threads |
| **Initialization** | Count set at creation | Party size set at creation |
| **Reusability** | One-time use (counts down to 0) | Reusable (resets after barrier) |
| **Typical Use** | Wait for initialization tasks | Synchronize threads at checkpoints |
| **API** | `countDown()`, `await()` | `await()`, automatic reset |

**CountDownLatch Example:**
```java
CountDownLatch startSignal = new CountDownLatch(1);
CountDownLatch doneSignal = new CountDownLatch(10);

// 10 worker threads
for (int i = 0; i < 10; i++) {
    new Thread(() -> {
        startSignal.await(); // All wait for start signal
        doWork();
        doneSignal.countDown(); // Signal completion
    }).start();
}

startSignal.countDown(); // Signal all threads to start
doneSignal.await(); // Wait for all threads to complete
System.out.println("All workers done");
```

**CyclicBarrier Example:**
```java
CyclicBarrier barrier = new CyclicBarrier(3, () -> {
    System.out.println("Phase completed"); // Action after barrier
});

for (int i = 0; i < 3; i++) {
    new Thread(() -> {
        for (int phase = 0; phase < 3; phase++) {
            doPhaseWork(phase);
            barrier.await(); // Wait for all threads at barrier
        }
    }).start();
}
// Output:
// Phase completed (after first iteration)
// Phase completed (after second iteration)
// Phase completed (after third iteration)
```

---

### Q9: Atomicity and Visibility in Multithreading

**Atomicity:** An operation is atomic if it appears to execute as a single indivisible unit.

**Visibility:** Changes made by one thread are visible to other threads.

**The Problem:**
```java
public class Counter {
    private int count = 0;
    
    public void increment() {
        count++; // NOT atomic - three operations:
        // 1. Read count from memory
        // 2. Add 1 to count
        // 3. Write count to memory
    }
    
    public int getCount() {
        return count; // May read stale value from CPU cache
    }
}

// Race condition: With 2 threads calling increment() 1000 times each,
// Expected: 2000, Actual: 1000-1999 (depending on timing)
```

**Solution - Atomicity:**
```java
public class AtomicCounter {
    private AtomicInteger count = new AtomicInteger(0);
    
    public void increment() {
        count.incrementAndGet(); // Atomic operation
    }
    
    public int getCount() {
        return count.get();
    }
}

// Atomic classes use CAS (Compare-And-Swap) internally
// count.compareAndSet(expected, new) - atomic check-then-act
```

**Solution - Visibility with volatile:**
```java
public class VisibilityExample {
    private volatile boolean running = true; // Ensures visibility
    
    public void stop() {
        running = false; // Change visible to all threads immediately
    }
    
    public void run() {
        while (running) { // Always reads fresh value
            // Work
        }
    }
}

// Without volatile: Thread might cache running=true, never see the false update
// With volatile: Every read/write goes directly to main memory
```

**Atomic Operations in Java:**
```java
AtomicInteger ai = new AtomicInteger(0);
ai.incrementAndGet();              // Atomic ++
ai.getAndAdd(5);                   // Atomic += 5
ai.compareAndSet(0, 1);            // CAS operation

AtomicReference<String> ref = new AtomicReference<>("initial");
ref.compareAndSet("initial", "updated"); // Atomic reference update
```

---

### Q10: volatile Keyword in Multithreading

**What volatile Does:**
- Ensures **visibility** of changes across threads
- Disables **certain optimizations** by compiler/JVM
- Does NOT provide **atomicity**

**Memory Model:**
```
Without volatile:
Thread 1: [CPU Cache] <- [Main Memory] -> [CPU Cache] Thread 2
(Cached value may be stale)

With volatile:
Thread 1: [Main Memory] <- [Main Memory] -> [Main Memory] Thread 2
(Every read/write goes to main memory)
```

**Example - The Problem:**
```java
public class VolatileExample {
    private int value = 0;
    private boolean running = true;
    
    public void write() {
        value = 42;
        running = false; // Signal stop
    }
    
    public void read() {
        while (running) {
            System.out.println(value); // Might print 0 forever
        }
    }
}
// Without volatile on running: Thread might cache running=true
// With volatile on running: Guarantees visibility of both running and value
```

**Correct Implementation:**
```java
public class CorrectVolatile {
    private volatile int value = 0;
    private volatile boolean running = true;
    
    public void write() {
        value = 42;
        running = false; // Guaranteed visible
    }
    
    public void read() {
        while (running) {
            System.out.println(value); // Guaranteed to see latest value
        }
    }
}
```

**Important Note on volatile:**
- `volatile` does NOT make `value++` atomic
- Use `AtomicInteger` for atomic operations
- Use `volatile` for visibility of single variables

---

### Q11: Producer-Consumer Problem - Implementation

**Classic Scenario:**
```java
public class ProducerConsumer {
    private final BlockingQueue<Item> queue = new LinkedBlockingQueue<>(10);
    
    class Producer extends Thread {
        @Override
        public void run() {
            while (true) {
                Item item = produceItem();
                try {
                    queue.put(item); // Blocks if queue full
                    System.out.println("Produced: " + item);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    class Consumer extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Item item = queue.take(); // Blocks if queue empty
                    System.out.println("Consumed: " + item);
                    processItem(item);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
```

**With Bounded Queue (Backpressure):**
```java
// Queue size = 10
// Producer blocks when queue has 10 items
// Consumer blocks when queue is empty
// This prevents producer overwhelming consumer with memory
```

---

### Q12: Even-Odd Printing with 2 Threads (Volatile & Synchronization)

**Problem:** 2 threads print 1-10 alternately (1, 2, 3, ... 10) using volatile.

**Solution 1: Using Volatile and Wait-Notify:**
```java
public class EvenOddPrinter {
    private volatile int counter = 1;
    private volatile boolean isEvenTurn = false;
    private final Object lock = new Object();
    
    public void printEven() {
        while (counter <= 10) {
            synchronized (lock) {
                if (counter % 2 == 0) {
                    System.out.println("Even Thread: " + counter);
                    counter++;
                    isEvenTurn = false;
                    lock.notifyAll();
                } else {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
    
    public void printOdd() {
        while (counter <= 10) {
            synchronized (lock) {
                if (counter % 2 == 1) {
                    System.out.println("Odd Thread: " + counter);
                    counter++;
                    isEvenTurn = true;
                    lock.notifyAll();
                } else {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
    
    public static void main(String[] args) {
        EvenOddPrinter printer = new EvenOddPrinter();
        
        Thread odd = new Thread(printer::printOdd, "Odd");
        Thread even = new Thread(printer::printEven, "Even");
        
        odd.start();
        even.start();
    }
}

// Output:
// Odd Thread: 1
// Even Thread: 2
// Odd Thread: 3
// Even Thread: 4
// ...
```

**Solution 2: Using Semaphore (Cleaner):**
```java
public class EvenOddWithSemaphore {
    private int counter = 1;
    private Semaphore odd = new Semaphore(1);  // Odd thread starts
    private Semaphore even = new Semaphore(0); // Even thread waits
    
    public void printOdd() throws InterruptedException {
        while (counter <= 10) {
            odd.acquire();
            if (counter % 2 == 1) {
                System.out.println(counter);
                counter++;
            }
            even.release();
        }
    }
    
    public void printEven() throws InterruptedException {
        while (counter <= 10) {
            even.acquire();
            if (counter % 2 == 0) {
                System.out.println(counter);
                counter++;
            }
            odd.release();
        }
    }
}
```

**Solution 3: Using ReentrantLock with Conditions:**
```java
public class EvenOddWithLock {
    private int counter = 1;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition odd = lock.newCondition();
    private final Condition even = lock.newCondition();
    
    public void printOdd() throws InterruptedException {
        while (counter <= 10) {
            lock.lock();
            try {
                while (counter % 2 == 0) {
                    odd.await();
                }
                System.out.println(counter++);
                even.signal();
            } finally {
                lock.unlock();
            }
        }
    }
    
    public void printEven() throws InterruptedException {
        while (counter <= 10) {
            lock.lock();
            try {
                while (counter % 2 == 1) {
                    even.await();
                }
                System.out.println(counter++);
                odd.signal();
            } finally {
                lock.unlock();
            }
        }
    }
}
```

**Architect Analysis:**
- **Solution 1 (Wait-Notify)**: Classic but error-prone (spurious wakeups)
- **Solution 2 (Semaphore)**: Clean and explicit intent
- **Solution 3 (Lock + Condition)**: Most flexible, best for multiple conditions

---

## Other Topics

### JWT Token

#### Q13: JWT Structure and Security

**JWT Format:**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

**Three Parts (Base64 encoded):**
1. **Header:** `{"alg":"HS256","typ":"JWT"}`
2. **Payload:** `{"sub":"1234567890","name":"John Doe","iat":1516239022}`
3. **Signature:** HMAC-SHA256(base64(header) + "." + base64(payload), secret)

**Generation:**
```java
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;

public class JwtGenerator {
    private static final String SECRET_KEY = "mySecretKeyMustBeAtLeast32CharactersLong";
    private static final long EXPIRATION_TIME = 3600000; // 1 hour
    
    public static String generateToken(String userId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);
        
        return Jwts.builder()
            .setSubject(userId)
            .claim("username", username)
            .claim("roles", Arrays.asList("USER", "ADMIN"))
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
            .compact();
    }
}
```

**Validation:**
```java
public class JwtValidator {
    private static final String SECRET_KEY = "mySecretKeyMustBeAtLeast32CharactersLong";
    
    public static Claims validateToken(String token) throws JwtException {
        try {
            return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
        } catch (ExpiredJwtException e) {
            throw new JwtException("Token expired", e);
        } catch (UnsupportedJwtException e) {
            throw new JwtException("Unsupported token", e);
        } catch (MalformedJwtException e) {
            throw new JwtException("Invalid token", e);
        } catch (SignatureException e) {
            throw new JwtException("Invalid signature", e);
        }
    }
}
```

**Security Considerations:**

| Consideration | Risk | Mitigation |
|---------------|------|-----------|
| **Token Expiration** | Compromise if leaked | Use short expiration (15 min) + refresh tokens |
| **Secret Key Exposure** | All tokens become invalid | Store in secure vault (HashiCorp Vault, AWS Secrets Manager) |
| **Token Replay** | Attacker replays old token | Use nonce, jti (JWT ID) claim |
| **Signature Algorithm** | NONE algorithm accepted | Explicitly set HS256/RS256 |
| **Token Storage (Client)** | XSS can steal token | Store in HttpOnly cookie, not localStorage |

**Refresh Token Flow:**
```java
public class RefreshTokenFlow {
    public static String generateAccessToken(String userId) {
        // Short-lived (15 minutes)
        return Jwts.builder()
            .setSubject(userId)
            .setExpiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000))
            .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
            .compact();
    }
    
    public static String generateRefreshToken(String userId) {
        // Long-lived (7 days), stored in DB
        return Jwts.builder()
            .setSubject(userId)
            .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000))
            .claim("type", "refresh")
            .signWith(SignatureAlgorithm.HS256, REFRESH_SECRET_KEY)
            .compact();
    }
}

// Client flow:
// 1. Send username/password -> Get accessToken + refreshToken
// 2. Use accessToken for API calls
// 3. When accessToken expires, send refreshToken to get new accessToken
// 4. refreshToken stored in HttpOnly cookie, not accessible to JavaScript
```

---

### Design Patterns

#### Q14: Builder Pattern - When and Why

**Problem:** Creating objects with many optional parameters.

```java
// Without Builder - Telescope Constructor Problem
public class User {
    public User(String name) { }
    public User(String name, String email) { }
    public User(String name, String email, String phone) { }
    public User(String name, String email, String phone, int age) { }
    // Exponential combinations
}

// With Setter Inconsistency
User user = new User("John");
user.setEmail("john@example.com"); // Object in invalid state until all setters called
```

**Solution - Builder Pattern:**
```java
public class User {
    private final String name;
    private final String email;
    private final String phone;
    private final int age;
    
    private User(Builder builder) {
        this.name = builder.name;
        this.email = builder.email;
        this.phone = builder.phone;
        this.age = builder.age;
    }
    
    public static class Builder {
        private final String name; // Required
        private String email = ""; // Optional
        private String phone = ""; // Optional
        private int age = 0; // Optional
        
        public Builder(String name) {
            this.name = name;
        }
        
        public Builder email(String email) {
            this.email = email;
            return this;
        }
        
        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }
        
        public Builder age(int age) {
            this.age = age;
            return this;
        }
        
        public User build() {
            return new User(this);
        }
    }
}

// Usage
User user = new User.Builder("John")
    .email("john@example.com")
    .phone("1234567890")
    .build();
```

**Use Cases:**
- Database connection builders (HikariCP)
- HTTP request builders (OkHttp)
- Object configuration with many optional fields
- Complex domain objects

**Architect Perspective:**
- Fluent API for readability
- Immutability guaranteed after build()
- Validation can be deferred to build() method
- Works well with inheritance (use generics for type-safe subclass builders)

---

#### Q15: Proxy Pattern - Real-world Application

**Problem:** Need to control access to an object or defer its creation.

**Scenarios:**
1. **Lazy Initialization:** Create expensive objects only when needed
2. **Access Control:** Check permissions before accessing resource
3. **Logging/Monitoring:** Intercept and log method calls
4. **Remote Access:** Proxy to remote service (RPC)
5. **Caching:** Cache expensive computations

**Example 1: Lazy Initialization Proxy**
```java
public interface Image {
    void display();
}

public class RealImage implements Image {
    private String filename;
    
    public RealImage(String filename) {
        this.filename = filename;
        loadImageFromDisk(); // Expensive operation
    }
    
    private void loadImageFromDisk() {
        System.out.println("Loading image: " + filename);
    }
    
    @Override
    public void display() {
        System.out.println("Displaying: " + filename);
    }
}

public class ImageProxy implements Image {
    private RealImage realImage;
    private String filename;
    
    public ImageProxy(String filename) {
        this.filename = filename; // Don't load yet
    }
    
    @Override
    public void display() {
        if (realImage == null) {
            realImage = new RealImage(filename); // Load only on demand
        }
        realImage.display();
    }
}

// Usage
Image image = new ImageProxy("photo.jpg");
// Image not loaded yet
image.display(); // Now it's loaded and displayed
```

**Example 2: Access Control Proxy**
```java
public class BankAccount {
    private double balance = 1000;
    
    public void withdraw(double amount) {
        balance -= amount;
    }
    
    public double getBalance() {
        return balance;
    }
}

public class BankAccountProxy extends BankAccount {
    private String userId;
    
    public BankAccountProxy(String userId) {
        this.userId = userId;
    }
    
    @Override
    public void withdraw(double amount) {
        if (!isAuthorized()) {
            throw new SecurityException("User not authorized");
        }
        super.withdraw(amount);
    }
    
    private boolean isAuthorized() {
        return userId != null && !userId.isEmpty();
    }
}
```

**Example 3: Logging Proxy (AOP-like):**
```java
public class LoggingProxy implements CalculatorService {
    private final CalculatorService service;
    
    public LoggingProxy(CalculatorService service) {
        this.service = service;
    }
    
    @Override
    public int calculate(int a, int b) {
        long start = System.nanoTime();
        int result = service.calculate(a, b);
        long duration = System.nanoTime() - start;
        System.out.println("calculate(" + a + "," + b + ") = " + result + " in " + duration + "ns");
        return result;
    }
}
```

**Comparison with Decorator Pattern:**

| Aspect | Proxy | Decorator |
|--------|-------|-----------|
| **Purpose** | Control access, defer creation | Add functionality |
| **Object Creation** | Proxy creates real object | Decorator wraps existing object |
| **Interface Match** | Same as real object | Same as real object |
| **Usage** | Hidden from client | Known to client |
| **Relationship** | 1-1 proxy to object | Chain multiple decorators |

---

#### Q16: Factory Pattern - Simplify Object Creation

**Simple Factory:**
```java
public enum DatabaseType {
    MYSQL, POSTGRES, ORACLE
}

public class DatabaseFactory {
    public static Database createDatabase(DatabaseType type) {
        return switch (type) {
            case MYSQL -> new MySQLDatabase();
            case POSTGRES -> new PostgreSQLDatabase();
            case ORACLE -> new OracleDatabase();
        };
    }
}

// Usage
Database db = DatabaseFactory.createDatabase(DatabaseType.MYSQL);
```

**Factory Method Pattern (for subclasses):**
```java
public abstract class DocumentFactory {
    public abstract Document createDocument();
    
    public void processDocument() {
        Document doc = createDocument(); // Subclass decides implementation
        doc.open();
        doc.process();
        doc.save();
    }
}

public class PdfDocumentFactory extends DocumentFactory {
    @Override
    public Document createDocument() {
        return new PdfDocument();
    }
}

public class WordDocumentFactory extends DocumentFactory {
    @Override
    public Document createDocument() {
        return new WordDocument();
    }
}
```

**Abstract Factory Pattern (for families of related objects):**
```java
public interface UIElementFactory {
    Button createButton();
    Checkbox createCheckbox();
    TextField createTextField();
}

public class WindowsUIFactory implements UIElementFactory {
    @Override
    public Button createButton() { return new WindowsButton(); }
    
    @Override
    public Checkbox createCheckbox() { return new WindowsCheckbox(); }
    
    @Override
    public TextField createTextField() { return new WindowsTextField(); }
}

public class MacUIFactory implements UIElementFactory {
    @Override
    public Button createButton() { return new MacButton(); }
    
    @Override
    public Checkbox createCheckbox() { return new MacCheckbox(); }
    
    @Override
    public TextField createTextField() { return new MacTextField(); }
}

// Usage
UIElementFactory factory = 
    isMacOS() ? new MacUIFactory() : new WindowsUIFactory();
Button button = factory.createButton(); // Create OS-specific button
```

**When to Use:**

| Pattern | When |
|---------|------|
| **Simple Factory** | Fixed, small number of types |
| **Factory Method** | Subclasses decide implementation |
| **Abstract Factory** | Related families of objects (UI themes, DB drivers) |

---

### Microservices

#### Q17: Monolith to Microservices - Why Convert?

**Monolith Advantages:**
- Simple deployment
- Shared database (ACID transactions)
- Lower latency (in-process calls)
- Easier testing (single app)

**Monolith Pain Points (at scale):**
```
Scaling: Must scale entire app even if only one service is bottleneck
  Solution: Microservices let you scale individual services

Deployment: Single change requires full system restart
  Solution: Deploy services independently

Technology: Locked into single tech stack
  Solution: Each service chooses optimal tech

Teams: Large monolith = tight coupling, coordination overhead
  Solution: Microservices = independent teams, full ownership
```

**Example - E-commerce Monolith to Microservices:**

```
Monolith:
┌─────────────────────────────┐
│  E-commerce Application     │
├─────────────────────────────┤
│ User Service                │
│ Order Service               │
│ Payment Service             │
│ Inventory Service           │
│ Shipping Service            │
│ Notification Service        │
└─────────────────────────────┘
        ↓
    Shared DB
  (ACID, Tight Coupling)

Microservices:
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│ User MS  │  │Order MS  │  │Payment MS│  │Inventory │
└────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘
     │             │             │             │
     └─────────────┼─────────────┼─────────────┘
                   │
              API Gateway
         (No direct DB coupling)
```

**Benefits:**
1. **Independent Scalability:** Only scale Payment MS if it's bottleneck
2. **Technology Diversity:** Use Node.js for User MS, Java for Order MS
3. **Deployment Independence:** Deploy Payment MS without affecting Order MS
4. **Team Autonomy:** Order team owns full stack of Order MS
5. **Fault Isolation:** Payment MS failure doesn't crash entire system

**Tradeoffs:**
```
Complexity:         Distributed system challenges (CAP theorem)
Data Consistency:   CAP = choose Availability & Partition tolerance, lose Consistency
Network Latency:    Inter-service calls slower than in-process calls
Monitoring:         Need distributed tracing (Jaeger, Zipkin)
Testing:            Integration testing across services more complex
```

---

#### Q18: Saga Pattern - Distributed Transactions

**Problem:** In microservices, you can't use ACID transactions across service boundaries.

**Scenario: Order Creation Flow**
```
Monolith (Single Transaction):
1. Create Order
2. Reserve Inventory
3. Process Payment
4. All succeed or all rollback (ACID)

Microservices Problem:
Order Service creates order ✓
Inventory Service reserves stock ✓
Payment Service fails ✗
Now we're stuck with order and reserved inventory but no payment!
```

**Solution: Saga Pattern**

**Choreography (Event-driven):**
```
1. OrderService creates order -> publishes "OrderCreated" event
2. InventoryService listens, reserves stock -> publishes "StockReserved" event
3. PaymentService listens, processes payment -> publishes "PaymentProcessed" event
4. If PaymentService fails -> publishes "PaymentFailed" event
5. InventoryService compensates (releases stock) -> publishes "StockReleased" event
6. OrderService compensates (cancels order) -> publishes "OrderCancelled" event
```

**Orchestration (Central Saga Controller):**
```
┌─────────────────────────────────┐
│  Saga Orchestrator              │
│  (Order Saga Coordinator)       │
└─────────────┬─────────────────┬─┘
              │                 │
    ┌─────────▼────────┐   ┌────▼──────────┐
    │ Order Service    │   │Payment Service│
    └──────────────────┘   └───────────────┘
              │
    ┌─────────▼──────────────┐
    │ Inventory Service      │
    └────────────────────────┘
```

**Implementation - Choreography:**
```java
@Service
@Transactional
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public Order createOrder(OrderRequest request) {
        Order order = new Order(request);
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);
        
        eventPublisher.publishEvent(new OrderCreatedEvent(order.getId()));
        return order;
    }
}

@Service
public class InventoryService {
    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        try {
            // Reserve inventory (atomic operation)
            inventoryRepository.reserveStock(event.getOrderId());
            eventPublisher.publishEvent(new StockReservedEvent(event.getOrderId()));
        } catch (Exception e) {
            // Publish failure event for compensation
            eventPublisher.publishEvent(new StockReservationFailedEvent(event.getOrderId()));
        }
    }
    
    @EventListener
    public void handlePaymentFailed(PaymentFailedEvent event) {
        // Compensation: Release reserved stock
        inventoryRepository.releaseStock(event.getOrderId());
        eventPublisher.publishEvent(new StockReleasedEvent(event.getOrderId()));
    }
}

@Service
public class PaymentService {
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @EventListener
    public void handleStockReserved(StockReservedEvent event) {
        try {
            // Process payment
            paymentRepository.processPayment(event.getOrderId());
            eventPublisher.publishEvent(new PaymentProcessedEvent(event.getOrderId()));
        } catch (PaymentException e) {
            // Publish failure - triggers compensation chain
            eventPublisher.publishEvent(new PaymentFailedEvent(event.getOrderId()));
        }
    }
}
```

**Implementation - Orchestration:**
```java
@Service
public class OrderSagaOrchestrator {
    @Autowired
    private OrderServiceClient orderService;
    
    @Autowired
    private InventoryServiceClient inventoryService;
    
    @Autowired
    private PaymentServiceClient paymentService;
    
    private final List<CompensationStep> compensationSteps = new ArrayList<>();
    
    public void executeOrderSaga(OrderRequest request) {
        try {
            // Step 1: Create Order
            OrderResponse order = orderService.createOrder(request);
            compensationSteps.add(() -> orderService.cancelOrder(order.getId()));
            
            // Step 2: Reserve Inventory
            inventoryService.reserveStock(order.getId());
            compensationSteps.add(() -> inventoryService.releaseStock(order.getId()));
            
            // Step 3: Process Payment
            paymentService.processPayment(order.getId());
            
            // All steps succeeded - update order status
            orderService.confirmOrder(order.getId());
            
        } catch (Exception e) {
            // Compensation: Execute in reverse order
            for (int i = compensationSteps.size() - 1; i >= 0; i--) {
                compensationSteps.get(i).compensate();
            }
            throw new SagaExecutionException(e);
        }
    }
}

interface CompensationStep {
    void compensate();
}
```

**Choreography vs Orchestration:**

| Aspect | Choreography | Orchestration |
|--------|-------------|---------------|
| **Control Flow** | Distributed (events) | Centralized (saga controller) |
| **Complexity** | Less central logic | More central logic |
| **Debugging** | Harder (event chains) | Easier (single coordinator) |
| **Coupling** | Services know about events | Coordinator knows about services |
| **Performance** | Better (async events) | Potential bottleneck (coordinator) |
| **Use When** | Simple flows, event-driven domain | Complex flows, need visibility |

**Gotchas:**
- Compensation may not be perfect inverse (e.g., inventory released but order already visible to customer)
- Idempotency required (what if compensation is called twice?)
- Monitoring and alerting critical (saga failures need manual intervention)

---

### Spring Framework

#### Q19: Spring High-Level Architecture and DI

**Spring Core Concepts:**
```
┌──────────────────────────────────────────────┐
│         Spring Framework                     │
├──────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────┐  │
│ │   IoC Container (ApplicationContext)    │  │
│ │   - Bean Lifecycle Management           │  │
│ │   - Dependency Injection                │  │
│ │   - Property Placeholders               │  │
│ └─────────────────────────────────────────┘  │
├──────────────────────────────────────────────┤
│ Core: AOP, Transaction Management            │
├──────────────────────────────────────────────┤
│ Data: JPA, Transactions, JDBC                │
├──────────────────────────────────────────────┤
│ Web: Spring MVC, REST, Routing               │
├──────────────────────────────────────────────┤
│ Cloud: Service Discovery, Config, Circuit    │
└──────────────────────────────────────────────┘
```

**Dependency Injection - Three Approaches:**

```java
// 1. Constructor Injection (Recommended)
@Component
public class UserService {
    private final UserRepository userRepository;
    
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}

// 2. Setter Injection (Optional dependencies)
@Component
public class ReportService {
    private EmailService emailService;
    
    @Autowired(required = false)
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }
}

// 3. Field Injection (NOT RECOMMENDED)
@Component
public class BadService {
    @Autowired
    private UserRepository userRepository; // Hard to test, not explicit
}
```

**Bean Lifecycle:**
```
┌─────────────────────────────┐
│  1. Instantiation           │
│     (new UserService())     │
└──────────────┬──────────────┘
               ↓
┌─────────────────────────────┐
│  2. Dependency Injection    │
│     (setter/constructor)    │
└──────────────┬──────────────┘
               ↓
┌─────────────────────────────┐
│  3. Initialization          │
│     (@PostConstruct)        │
└──────────────┬──────────────┘
               ↓
┌─────────────────────────────┐
│  4. Ready for Use           │
└──────────────┬──────────────┘
               ↓
┌─────────────────────────────┐
│  5. Destruction             │
│     (@PreDestroy)           │
└─────────────────────────────┘
```

**Configuration:**
```java
@Configuration
public class AppConfig {
    @Bean
    public UserRepository userRepository() {
        return new UserRepositoryImpl();
    }
    
    @Bean
    public UserService userService(UserRepository userRepository) {
        return new UserService(userRepository);
    }
}

// Alternative: Classpath scanning
@SpringBootApplication
@ComponentScan("com.example")
public class Application { }

// Bean annotations
@Component   // Generic component
@Service     // Business logic
@Repository  // Data access
@Controller  // Web controller
```

---

#### Q20: AOP (Aspect-Oriented Programming) - Real Use Cases

**Problem AOP Solves:**
```
Cross-cutting concerns scattered across business logic:
├── Logging
├── Security (authorization checks)
├── Transaction management
├── Caching
├── Error handling
└── Performance monitoring

Without AOP: Every method sprinkled with these concerns
With AOP: Centralized cross-cutting logic
```

**AOP Terminology:**
```
Aspect:     A module combining advice and pointcut
Pointcut:   Expression defining where advice applies ("@Transactional")
Advice:     Code to execute (before, after, around)
Join Point: Actual point in execution (method call)
Weaving:    Process of applying aspects (compile-time, load-time, or runtime)
```

**Example 1: Logging Aspect**
```java
@Aspect
@Component
public class LoggingAspect {
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
    
    @Pointcut("execution(* com.example.service..*(..))") // All methods in service package
    public void serviceMethods() { }
    
    @Before("serviceMethods()")
    public void logBefore(JoinPoint joinPoint) {
        logger.info("Calling: {}", joinPoint.getSignature());
    }
    
    @After("serviceMethods()")
    public void logAfter(JoinPoint joinPoint) {
        logger.info("Exiting: {}", joinPoint.getSignature());
    }
    
    @AfterThrowing(pointcut = "serviceMethods()", throwing = "exception")
    public void logException(JoinPoint joinPoint, Exception exception) {
        logger.error("Exception in {}: {}", joinPoint.getSignature(), exception.getMessage());
    }
    
    @Around("serviceMethods()")
    public Object logExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long duration = System.currentTimeMillis() - start;
        logger.info("{} took {} ms", pjp.getSignature(), duration);
        return result;
    }
}
```

**Example 2: Caching Aspect**
```java
@Aspect
@Component
public class CachingAspect {
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    
    @Around("@annotation(com.example.annotation.Cacheable)")
    public Object cache(ProceedingJoinPoint pjp) throws Throwable {
        String cacheKey = generateKey(pjp);
        
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        
        Object result = pjp.proceed();
        cache.put(cacheKey, result);
        return result;
    }
    
    private String generateKey(ProceedingJoinPoint pjp) {
        return pjp.getSignature().getName() + 
               Arrays.toString(pjp.getArgs());
    }
}

// Usage
@Component
public class UserService {
    @Cacheable
    public User getUserById(Long id) {
        return userRepository.findById(id);
    }
}
```

**Example 3: Transaction Aspect (Spring's Built-in)**
```java
@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Transactional // AOP creates transaction, commits on success, rolls back on exception
    public Order createOrder(OrderRequest request) {
        Order order = new Order(request);
        orderRepository.save(order);
        
        Payment payment = paymentRepository.processPayment(order);
        if (!payment.isSuccessful()) {
            throw new PaymentException("Payment failed");
            // Entire transaction rolls back (order + payment)
        }
        
        return order;
    }
}
```

**Example 4: Authorization Aspect**
```java
@Aspect
@Component
public class AuthorizationAspect {
    @Around("@annotation(com.example.annotation.RequireRole)")
    public Object checkAuthorization(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        RequireRole requireRole = signature.getMethod().getAnnotation(RequireRole.class);
        
        String currentUserRole = SecurityContextHolder.getContext()
            .getAuthentication().getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .findFirst()
            .orElse("");
        
        if (!currentUserRole.equals(requireRole.value())) {
            throw new AccessDeniedException("User doesn't have required role");
        }
        
        return pjp.proceed();
    }
}

// Usage
@Service
public class AdminService {
    @RequireRole("ADMIN")
    public void deleteUser(Long userId) {
        // Only accessible by ADMIN role
    }
}
```

**Pointcut Expressions:**
```java
execution(* com.example.service.*.*(..))     // All methods in service package
execution(public * *(..))                     // All public methods
@annotation(org.springframework.transaction.annotation.Transactional) // Methods with @Transactional
@within(org.springframework.stereotype.Component) // Classes with @Component
within(com.example.service..*)                // All classes in service package
bean(userService*)                            // Beans matching name pattern
```

**Advisor vs Aspect:**
```
Aspect:  Full cross-cutting concern (@Aspect class)
Advisor: Simpler, single advice + pointcut (for fine-grained control)
```

---

### System Design - WhatsApp

#### Q21: WhatsApp System Design - Complete Architecture

**Functional Requirements:**
```
1. Send/receive messages (text, images, videos)
2. One-to-one and group chats
3. Message delivery guarantee (sent, delivered, read)
4. Typing indicators
5. Last seen status
6. Push notifications
7. Group management
8. Message search
9. Media file storage
10. User authentication
```

**Non-Functional Requirements:**
```
Scalability:   100M+ users, 100M+ daily active users
Availability:  99.99% uptime
Latency:       < 100ms message delivery
Consistency:   Eventually consistent (CAP: choose AP)
Throughput:    1M+ messages per second
```

**Architecture Overview:**
```
┌─────────────────────────────────────────────────────────────┐
│                        Clients                              │
│  (Android, iOS, Web) - WebSocket/Long Polling              │
└─────────────────┬───────────────────────────────────────────┘
                  │
        ┌─────────▼──────────┐
        │   Load Balancer    │
        │  (Route by user ID)│
        └─────────┬──────────┘
                  │
┌─────────────────────────────────────────────────────────────┐
│                 Chat Servers (Horizontally Scaled)          │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐             │
│  │ Chat Node  │  │ Chat Node  │  │ Chat Node  │             │
│  │  (WebSocket)│  │ (WebSocket)│  │ (WebSocket)│             │
│  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘             │
│        │                │                │                  │
│        └────────────────┼────────────────┘                  │
│                         │                                   │
└─────────────────────────┼───────────────────────────────────┘
                          │
        ┌─────────────────┼──────────────────┐
        │                 │                  │
    ┌───▼────┐      ┌─────▼──────┐    ┌────▼─────┐
    │ Message│      │  Presence   │    │  State   │
    │ Queue  │      │  Store      │    │  Store   │
    │(Kafka) │      │  (Redis)    │    │(Redis)   │
    └───┬────┘      └─────┬──────┘    └────┬─────┘
        │                 │                 │
┌───────┴─────────────────┴─────────────────┴────────────────┐
│            Message Storage (Distributed)                   │
│  Cassandra / HBase - Write-optimized for high throughput   │
└──────────────────────────────────────────────────────────── ┘
        │
    ┌───▼────────────────┐
    │ File Storage       │
    │ (S3 / GCS)         │
    │ (Images, Videos)   │
    └────────────────────┘
```

**Detailed Components:**

**1. Load Balancer (Gateway)**
```
User connects to WS://chat-lb-{region}.whatsapp.com
Load Balancer routes based on:
- User ID (consistent hashing) -> same chat node
- Geographic location -> nearest region
- Current node load

Why consistent hashing?
- User stays on same server for persistent WebSocket connection
- If node dies, rehashing only affects ~1/n of users
```

**2. Chat Servers (Stateless)**
```java
@Component
public class ChatServer {
    @Autowired
    private MessageQueue messageQueue;
    
    @Autowired
    private PresenceStore presenceStore;
    
    @Autowired
    private CacheStore cacheStore;
    
    // WebSocket connection per user
    private Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    
    @OnOpen
    public void onConnect(String userId, WebSocketSession session) {
        activeSessions.put(userId, session);
        presenceStore.setOnline(userId); // Update presence
    }
    
    @OnMessage
    public void onMessage(String userId, String messageJson) throws Exception {
        Message msg = parseMessage(messageJson);
        
        // Save to persistent storage (async)
        messageQueue.enqueue(msg);
        
        // Try to deliver immediately if recipient is online
        if (presenceStore.isOnline(msg.getRecipientId())) {
            WebSocketSession recipientSession = activeSessions.get(msg.getRecipientId());
            recipientSession.sendMessage(new TextMessage(msg.toJson()));
            msg.setStatus(MessageStatus.DELIVERED);
        } else {
            msg.setStatus(MessageStatus.SENT);
        }
        
        // Acknowledge sender immediately
        sendAck(userId, msg.getId());
    }
    
    @OnClose
    public void onDisconnect(String userId) {
        activeSessions.remove(userId);
        presenceStore.setOffline(userId);
    }
}
```

**3. Message Queue (Kafka)**
```
Purpose: Decouple message ingestion from storage
Benefits:
- High throughput (millions msgs/sec)
- Replay capability
- Multiple consumers (storage, indexing, analytics)

Topic: messages-{partition}
Partition Key: recipient_id (all messages for user go to same partition)
Retention: 30 days

Consumers:
1. Cassandra Writer: Persist messages
2. Elasticsearch: Index for search
3. Analytics Pipeline: Process for insights
```

**4. Presence Store (Redis)**
```
Key-Value Structure:
user:{userId}:status -> "online" / "offline"
user:{userId}:last_seen -> timestamp
user:{userId}:devices -> set of device IDs

Operations:
SET user:123:status "online" EX 300  // 5-min timeout (heartbeat)
MGET user:123:status user:456:status // Bulk presence check
ZADD presence-stream 1234567890 "user:123" // Sorted set for last-seen
```

**5. Message Storage (Cassandra)**
```
Table: messages

CREATE TABLE messages (
    message_id UUID PRIMARY KEY,
    sender_id bigint,
    recipient_id bigint,
    chat_id bigint,  // For group chats
    content text,
    created_at timestamp,
    status text,  // SENT, DELIVERED, READ
    media_url text  // S3 URL if media
);

Queries:
- Get all messages in conversation (by chat_id, ordered by created_at)
- Insert new message (high throughput - write optimized)
- Update message status (read status)

Why Cassandra?
- Write-optimized (WhatsApp is write-heavy)
- Distributed (no single point of failure)
- Time-series data (messages ordered by time)
- Strong consistency at partition level
```

**6. Presence Updates (Real-time Broadcasting)**
```
When user comes online:
1. Chat Server updates Redis presence store
2. Publishes to Redis Pub/Sub: "presence.online.{userId}"
3. All chat servers receive event
4. Chat servers send presence update to relevant contacts

Why Pub/Sub?
- Broadcast to all servers
- Efficient fanout (one write to all subscribers)
- In-memory (fast)
```

**7. Typing Indicators**
```
User starts typing:
1. Client sends: {"type": "typing", "from": "user1", "to": "user2"}
2. Chat Server publishes to: "typing.{recipientId}"
3. Chat Server routing user2 receives event
4. Sends to user2: "user1 is typing..."
5. Local timeout after 3 seconds (client sends stop-typing)

Why separate from messages?
- High frequency (every keystroke)
- Don't need persistence
- Best effort (ok if some are lost)
- Redis Pub/Sub is perfect (fire-and-forget)
```

**8. Message Delivery & Read Receipts**
```
Message Flow:
1. Sender sends message
   Status: SENT (stored in Cassandra)

2. Message received by server
   If recipient online:
     - Deliver immediately
     - Set status: DELIVERED
     - Send ack to sender

3. Recipient receives message
   If recipient app has focus:
     - Mark as READ immediately
     - Publish read receipt

4. Sender receives read receipt
   - Show "read" indicator with timestamp

Storage in Cassandra:
UPDATE messages SET status = 'READ' WHERE message_id = ?
```

**9. Group Chats**
```
Table: group_messages

CREATE TABLE group_messages (
    group_id bigint,
    message_id UUID,
    sender_id bigint,
    content text,
    created_at timestamp,
    PRIMARY KEY (group_id, created_at, message_id)
);

Table: group_members

CREATE TABLE group_members (
    group_id bigint,
    user_id bigint,
    added_at timestamp,
    PRIMARY KEY (group_id, user_id)
);

Fanout:
When user sends message to group:
1. Find all group members
2. For each member, create message record OR if online, direct send
3. Publish to pub/sub for online members

Optimization:
- Message deduplicated (not copies per member)
- Delivery status per member tracked separately
```

**10. Data Synchronization & Push Notifications**
```
When user comes online:
1. Chat Server queries Cassandra: GET last 100 messages
2. Sends pending messages to client
3. Updates read status for messages

For offline users:
1. Message stored in Cassandra
2. Push notification sent to device
3. When user opens app, fetch pending messages

FCM/APNS Integration:
- Lightweight notification (title, message id)
- Full message fetched from server when opened
- Why? Messages can be large (images, etc)
```

**11. Media Handling (Images, Videos)**
```
Upload Flow:
1. Client uploads to CDN (S3/GCS)
2. Returns URL
3. Message sent with media_url

Download Flow:
1. Client receives message with media_url
2. Downloads directly from CDN (not through chat server)
3. Cached locally

Why separate storage?
- Chat servers focused on messaging
- CDN distributed globally (lower latency)
- Scalable independently
- Different retention policies
```

**12. Scaling Challenges & Solutions**

```
Challenge 1: Message Delivery Under High Load
├─ Problem: 1M msgs/sec, each msg has 2-3 operations
├─ Solution: 
│   ├─ Kafka buffers writes
│   ├─ Async batch writes to Cassandra (flush every 1000 or 100ms)
│   └─ Message replicated to 3 Cassandra nodes (async)
│
Challenge 2: Real-time Presence (100M active users)
├─ Problem: Presence updates are frequent (online, offline, typing)
├─ Solution:
│   ├─ Redis with 5-min heartbeat (users send ping every 5 min)
│   ├─ Presence updates via Pub/Sub (doesn't persist)
│   ├─ Last seen stored in Cassandra (periodic batch)
│
Challenge 3: Group Chats (1000+ members)
├─ Problem: Fanout write - create message for each member
├─ Solution:
│   ├─ Single message in Cassandra
│   ├─ Delivery status tracked separately
│   ├─ Async fanout (worker processes fanout)
│
Challenge 4: Message Synchronization (Multi-device)
├─ Problem: Same user on phone + web, ensure sync
├─ Solution:
│   ├─ Track message receipt per device
│   ├─ Delivery timestamp per device
│   ├─ If not delivered to device, mark as pending
```

**Failure Modes & Recovery:**

```
Chat Node Crashes:
├─ WebSocket connections drop
├─ Clients auto-reconnect (exponential backoff)
├─ New node takes over (consistent hashing)
├─ Pending messages delivered from Cassandra

Cassandra Node Fails:
├─ Replication factor = 3
├─ Message available from 2 other replicas
├─ Read/write quorum = 2 (wait for 2 responses)

Message Queue (Kafka) Fails:
├─ All messages already written to Cassandra
├─ Kafka is for redundancy only
├─ Retry push to offline users from Cassandra

Redis (Presence) Fails:
├─ Presence lost (not critical)
├─ Users show offline
├─ Recovers when they interact again
├─ Last seen in Cassandra (not real-time)
```

**Capacity Planning:**

```
1M DAU, 100 messages per user per day = 100M messages/day
Peak hour: 10% of 100M = 10M messages
Spread over 3600s = 2,777 msgs/sec

For 100M DAU:
100M / 10 = 10M msgs/sec peak

Cassandra:
- Write throughput: 100K writes/sec per node
- Need 100 nodes for 10M msgs/sec
- With replication factor 3: 300 nodes
- Add headroom (50%): 450 nodes

Redis (Presence):
- 100M keys
- ~200 bytes per key = 20GB
- With replication: 60GB
- Shard across 10 Redis nodes
- Each holds 10M keys, 6GB
```

---

## Summary Table - Key Decisions

| Component | Technology | Why |
|-----------|-----------|-----|
| Message Queue | Kafka | High throughput, distributed, replay |
| Persistent Storage | Cassandra | Write-optimized, distributed, time-series |
| Presence/Cache | Redis | In-memory, Pub/Sub for broadcast, fast |
| File Storage | S3/GCS | Geo-distributed, cost-effective, reliable |
| Message Delivery | WebSocket | Bi-directional, low latency for real-time |
| Search | Elasticsearch | Full-text search on messages |
| Consistency | Eventual (AP) | Prioritize availability over strong consistency |

---

End of Interview Q&A
