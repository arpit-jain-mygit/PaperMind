# Java Interview Q&A - Saurabh Manik (TopMate)
**For Solution Architect (10-20 years experience)**

---

## Table of Contents
1. [Core Java - Collections](#core-java---collections)
2. [Core Java - Multi-Threading](#core-java---multi-threading)
   - [Thread States (Lifecycle)](#thread-states-lifecycle)
   - [Q1-Q12: Core threading concepts](#core-java---collections)
   - [Q13-Q16: ExecutorService & ThreadPoolExecutor](#q13-executorservice---thread-pool-abstraction)
   - [Multi-Threading Summary](#multi-threading-concepts-summary-layman-terms)
   - [Deep Dive: How synchronized Works Internally](#deep-dive-how-synchronized-works-internally)
3. [Other Topics](#other-topics)
   - [JWT Token](#jwt-token)
   - [Design Patterns](#design-patterns)
   - [Microservices](#microservices)
   - [Spring Framework](#spring-framework)
   - [System Design - WhatsApp](#system-design---whatsapp)
4. [MCQ Quiz - Advanced Level](#mcq-quiz---advanced-level)

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

## Solution: Producer-Consumer Using BlockingQueue

**BlockingQueue simplifies Producer-Consumer dramatically!** No manual locking, no conditions, no await/signal.

```java
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BlockingQueueProducerConsumer {
    public static void main(String[] args) throws InterruptedException {
        // ✨ CHANGED: BlockingQueue instead of ReentrantLock + manual conditions
        BlockingQueue<String> buffer = new LinkedBlockingQueue<>(5);  // Max size = 5
        
        Producer producer = new Producer(buffer);
        Consumer consumer = new Consumer(buffer);
        
        producer.start();
        consumer.start();
        
        producer.join();
        consumer.join();
    }
}

// ✨ SIMPLIFIED: Producer much simpler
class Producer extends Thread {
    private BlockingQueue<String> buffer;  // ✨ CHANGED: BlockingQueue instead of ReentrantLock + Condition
    
    public Producer(BlockingQueue<String> buffer) {
        this.buffer = buffer;
    }
    
    public void run() {
        for (int i = 0; i < 10; i++) {
            // ✨ CHANGED: No lock() call needed
            try {
                String data = "Data-" + i;
                System.out.println("Producing: " + data);
                
                // ✨ CHANGED: put() is blocking (no manual while loop + await())
                buffer.put(data);  // Automatically blocks if queue full, wakes up when space available
                
                System.out.println("Produced: " + data + ", Queue size: " + buffer.size());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // ✨ CHANGED: No unlock() needed
        }
        System.out.println("Producer finished!");
    }
}

// ✨ SIMPLIFIED: Consumer much simpler
class Consumer extends Thread {
    private BlockingQueue<String> buffer;  // ✨ CHANGED: BlockingQueue instead of ReentrantLock + Condition
    
    public Consumer(BlockingQueue<String> buffer) {
        this.buffer = buffer;
    }
    
    public void run() {
        for (int i = 0; i < 10; i++) {
            // ✨ CHANGED: No lock() call needed
            try {
                // ✨ CHANGED: take() is blocking (no manual while loop + await())
                String data = buffer.take();  // Automatically blocks if queue empty, wakes up when data available
                
                System.out.println("Consumed: " + data + ", Queue size: " + buffer.size());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // ✨ CHANGED: No unlock() needed
        }
        System.out.println("Consumer finished!");
    }
}

/* OUTPUT:
Producing: Data-0
Produced: Data-0, Queue size: 1
Producing: Data-1
Produced: Data-1, Queue size: 2
Consumed: Data-0, Queue size: 1
Producing: Data-2
Produced: Data-2, Queue size: 2
Producing: Data-3
Produced: Data-3, Queue size: 3
Consumed: Data-1, Queue size: 2
Producing: Data-4
Produced: Data-4, Queue size: 3
...
Producer finished!
Consumer finished!

(All 10 items produced and consumed in FIFO order automatically!)
*/
```

**Key Differences vs ReentrantLock Solution:**

| Aspect | ReentrantLock | BlockingQueue |
|--------|---|---|
| **Initialization** | 4 lines (lock + 2 conditions + queue) | 1 line ✨ |
| **Lock acquisition** | `lock.lock()` (manual) | Automatic in put/take ✨ |
| **Wait for space (full)** | `while + notFull.await()` (manual) | Automatic in put() ✨ |
| **Add to buffer** | `offer()` + manual signal | `put()` (blocking, auto-signal) ✨ |
| **Lock release** | `lock.unlock()` (manual) | Automatic ✨ |
| **Wait for data (empty)** | `while + notEmpty.await()` (manual) | Automatic in take() ✨ |
| **Remove from buffer** | `poll()` + manual signal | `take()` (blocking, auto-signal) ✨ |
| **Total lines** | 100+ lines | 40 lines ✨ |
| **Error-prone?** | ❌ High (manual sync) | ✅ Low (built-in safety) |

**BlockingQueue Methods:**

```java
BlockingQueue<String> queue = new LinkedBlockingQueue<>(5);

// PRODUCER:
queue.put(item);                             // Blocks if full, waits for space
queue.offer(item, 1, TimeUnit.SECONDS);      // Try for 1 sec, fail if full
queue.offer(item);                           // Non-blocking, returns false if full

// CONSUMER:
String item = queue.take();                  // Blocks if empty, waits for data
queue.poll(1, TimeUnit.SECONDS);             // Wait max 1 sec for data
queue.poll();                                // Non-blocking, returns null if empty

// QUERY:
queue.size();                                // Current size
queue.remainingCapacity();                   // Space left
queue.isEmpty();                             // Is empty?
```

**When to Use:**
- ✅ **BlockingQueue**: Simple producer-consumer, most common use case
- ✅ **ReentrantLock + Condition**: Complex multi-condition coordination, fairness needed
- ✅ **Semaphore**: Limit N concurrent resources, not just 1 queue

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

### Thread States (Lifecycle)

**What is a Thread State?**

A thread's state is where it currently is in its lifecycle:

```
Thread Birth → Execution → Waiting → Death
```

---

**6 Thread States (Simple Explanation):**

```
┌──────────────────┬───────────────────────────────────┬──────────────────────────┬──────────┐
│ State            │ Meaning                           │ Example                  │ Running? │
├──────────────────┼───────────────────────────────────┼──────────────────────────┼──────────┤
│ NEW              │ Created, start() not called       │ Thread t = new Thread(); │ ❌ NO    │
│ RUNNABLE         │ Running OR ready for CPU          │ t.start() called         │ ✅ YES   │
│ BLOCKED          │ Waiting for lock (synchronized)   │ synchronized(obj) locked │ ❌ NO    │
│ WAITING          │ Waiting forever for signal        │ obj.wait(), join()       │ ❌ NO    │
│ TIMED_WAITING    │ Waiting for N seconds max         │ Thread.sleep(2000)       │ ❌ NO    │
│ TERMINATED       │ Finished (dead)                   │ run() completed          │ ❌ NO    │
└──────────────────┴───────────────────────────────────┴──────────────────────────┴──────────┘
```

---

**Thread Lifecycle Diagram (with Loop):**

```
                           ┌─────────────────────────────────────────────────┐
                           │                                                 │
                           │   Thread Can Loop Between These States!         │
                           │                                                 │
                           ▼                                                 │
    ┌────────┐   start()  ┌──────────┐   acquires lock / await/signal ends  │
    │ NEW    │──────────►  │RUNNABLE  │────────────────────────────┐         │
    └────────┘            └──────────┘                             │         │
                              ▲                                    │         │
                              │                                    │         │
                              │  lock released / signal received   │         │
                              │  time limit expired / interrupt    │         │
                              │                                    │         │
                   ┌──────────┴────────────┬─────────────────────┘          │
                   │                       │                                 │
                   │                       │                                 │
             needs lock             calls wait()               sleeps/      │
                   │                  /join()              timeout wait      │
                   ▼                   ▼                        ▼            │
             ┌─────────┐           ┌────────┐            ┌──────────────┐  │
             │ BLOCKED │           │ WAITING│            │TIMED_WAITING │  │
             └─────────┘           └────────┘            └──────────────┘  │
                   │                   │                        │            │
                   └───────────────────┴────────────────────────┘            │
                            (lock released)              (time expired)     │
                           (signal/notify)              (interrupt)         │
                                                                             │
                                  Returns to RUNNABLE ◄──────────────────────
                                        │
                    run() method completes / thread.stop()
                                        ▼
                                  ┌────────────┐
                                  │TERMINATED  │
                                  │  (DEAD)    │
                                  └────────────┘
                                        │
                                   Final State
                                  (Cannot restart)
```

**How to Read the Diagram:**

```
1️⃣  NEW → RUNNABLE
    Thread created with new Thread()
    Thread starts with thread.start()
    
2️⃣  RUNNABLE → BLOCKED (Loop)
    Tries to enter synchronized block that's locked
    Other thread releases lock → back to RUNNABLE
    
3️⃣  RUNNABLE → WAITING (Loop)
    Calls obj.wait() / thread.join() / latch.await()
    Other thread calls notify() / notifyAll() → back to RUNNABLE
    
4️⃣  RUNNABLE → TIMED_WAITING (Loop)
    Calls Thread.sleep(1000) / lock.tryLock(2, TimeUnit.SECONDS)
    Time expires / thread interrupted → back to RUNNABLE
    
5️⃣  Any State → TERMINATED
    run() method completes
    Thread dies (cannot restart!)
```

**Real-World Scenario:**

```
Consumer Thread Lifecycle:

NEW:               Thread created
                        ↓
RUNNABLE:          Tries to take from queue
                        ↓
WAITING:           Queue empty, calls queue.take()
                   (Thread sleeps, waiting for producer)
                        ↓
                   Producer puts data
                   notify() called
                        ↓
RUNNABLE:          Wakes up, processes data
                        ↓
BLOCKED:           Tries to enter synchronized(resource)
                   Another thread has lock
                        ↓
RUNNABLE:          Lock released, acquired lock
                        ↓
TIMED_WAITING:     Calls Thread.sleep(100) to rate-limit
                        ↓
RUNNABLE:          Sleep ends, goes back to process more
                        ↓
                   (Loops back to WAITING to get next item)
                        ↓
TERMINATED:        Queue closes / thread interrupted
                   run() method ends
```

---

**Real Code Example:**

```java
public class ThreadStateDemo {
    public static void main(String[] args) throws InterruptedException {
        Object lock = new Object();
        
        // Thread 1: Will be BLOCKED waiting for lock
        Thread t1 = new Thread(() -> {
            System.out.println("T1: NEW → RUNNABLE (starting)");
            synchronized(lock) {
                System.out.println("T1: Got lock!");
                try {
                    Thread.sleep(1000);  // → TIMED_WAITING for 1 second
                    System.out.println("T1: Woke up from sleep");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("T1: TERMINATED (done)");
        });
        
        // Thread 2: Will be BLOCKED trying to get lock
        Thread t2 = new Thread(() -> {
            System.out.println("T2: RUNNABLE (trying to get lock)");
            synchronized(lock) {  // → BLOCKED here (T1 has it)
                System.out.println("T2: Finally got lock!");
            }
            System.out.println("T2: TERMINATED");
        });
        
        // Thread 3: Will WAIT for signal
        Thread t3 = new Thread(() -> {
            synchronized(lock) {
                try {
                    System.out.println("T3: WAITING for signal...");
                    lock.wait();  // → WAITING (indefinite)
                    System.out.println("T3: Got signal!");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        
        System.out.println("Initial state of t1: " + t1.getState());  // NEW
        
        t1.start();  // t1: NEW → RUNNABLE
        t2.start();  // t2: NEW → RUNNABLE → BLOCKED (waiting for lock)
        t3.start();  // t3: NEW → RUNNABLE → WAITING
        
        Thread.sleep(100);
        System.out.println("\nStates after 100ms:");
        System.out.println("t1 state: " + t1.getState());  // TIMED_WAITING (sleeping)
        System.out.println("t2 state: " + t2.getState());  // BLOCKED (waiting for t1's lock)
        System.out.println("t3 state: " + t3.getState());  // WAITING (waiting for signal)
        
        t1.join();  // Wait for t1 to finish
        
        System.out.println("\nAfter t1 finishes:");
        System.out.println("t1 state: " + t1.getState());  // TERMINATED
        System.out.println("t2 state: " + t2.getState());  // Now running (got lock)
        
        // Signal t3 to wake up
        synchronized(lock) {
            lock.notifyAll();
        }
        
        Thread.sleep(500);
        System.out.println("t3 state: " + t3.getState());  // TERMINATED (woke up)
    }
}

/* OUTPUT:
Initial state of t1: NEW

t1: NEW → RUNNABLE (starting)
T2: RUNNABLE (trying to get lock)
T3: WAITING for signal...
T1: Got lock!

States after 100ms:
t1 state: TIMED_WAITING
t2 state: BLOCKED
t3 state: WAITING

T1: Woke up from sleep
T1: TERMINATED (done)
T2: Finally got lock!

After t1 finishes:
t1 state: TERMINATED
t2 state: RUNNABLE  (or TERMINATED if already done)
T2: TERMINATED

t3 state: RUNNABLE  (or TERMINATED if already processed)
T3: Got signal!
T3: TERMINATED
*/
```

---

**State Transitions (When Does State Change?):**

```
┌──────────────────┬──────────────────┬─────────────────────────────┬──────────────────────┐
│ From             │ To               │ Reason                      │ Code Example         │
├──────────────────┼──────────────────┼─────────────────────────────┼──────────────────────┤
│ NEW              │ RUNNABLE         │ start() called              │ t.start()            │
│ RUNNABLE         │ BLOCKED          │ Waiting for lock            │ synchronized(obj){} │
│ RUNNABLE         │ WAITING          │ Waits forever for signal    │ obj.wait()           │
│ RUNNABLE         │ TIMED_WAITING    │ Sleeps for N seconds        │ Thread.sleep(1000)   │
│ BLOCKED          │ RUNNABLE         │ Lock acquired               │ Lock released        │
│ WAITING          │ RUNNABLE         │ Signal received             │ obj.notify()         │
│ TIMED_WAITING    │ RUNNABLE         │ Time expired                │ Timer ends           │
│ RUNNABLE         │ TERMINATED       │ run() method finishes       │ return from run()    │
└──────────────────┴──────────────────┴─────────────────────────────┴──────────────────────┘
```

---

**Methods That Cause State Changes:**

```
┌──────────────────────────────┬──────────────────┬─────────────────────────┐
│ Method Call                  │ Current → New    │ Notes                   │
├──────────────────────────────┼──────────────────┼─────────────────────────┤
│ t.start()                    │ NEW → RUNNABLE   │ Starts thread           │
│ Enter synchronized block     │ RUNNABLE → ...   │ BLOCKED if locked       │
│ obj.wait()                   │ RUNNABLE → WAIT  │ Waits forever           │
│ obj.wait(1000)               │ RUNNABLE → TIMED │ Waits 1 sec max         │
│ Thread.sleep(2000)           │ RUNNABLE → TIMED │ Sleeps 2 sec            │
│ obj.notify()                 │ WAITING → RUNNABLE   │ Wakes one thread   │
│ obj.notifyAll()              │ WAITING → RUNNABLE   │ Wakes all threads  │
│ Lock released                │ BLOCKED → RUNNABLE   │ Can now run        │
│ Timer expired                │ TIMED_WAITING → RUNNABLE │ Time's up     │
│ t.interrupt()                │ WAITING/TIMED → RUNNABLE │ Force wake up  │
│ run() method returns         │ RUNNABLE → TERMINATED    │ Thread dies    │
└──────────────────────────────┴──────────────────┴─────────────────────────┘
```

---

**Common Mistakes:**

```
┌────────────────────────────────────┬────────────────────────────────┐
│ ❌ Wrong Assumption                │ ✅ Correct Understanding       │
├────────────────────────────────────┼────────────────────────────────┤
│ RUNNABLE = actively running now    │ RUNNABLE = running OR waiting  │
│                                    │ for CPU (many threads, 1 CPU)  │
├────────────────────────────────────┼────────────────────────────────┤
│ start() = thread immediately runs  │ start() = thread goes to       │
│                                    │ RUNNABLE (might not run yet)   │
├────────────────────────────────────┼────────────────────────────────┤
│ BLOCKED = same as WAITING          │ BLOCKED = waiting for LOCK     │
│                                    │ WAITING = waiting for SIGNAL   │
│                                    │ (obj.notify/notifyAll)         │
├────────────────────────────────────┼────────────────────────────────┤
│ WAITING = thread will wait forever │ WAITING = waits until signaled │
│                                    │ (thread can sleep indefinitely)│
├────────────────────────────────────┼────────────────────────────────┤
│ TERMINATED = same as BLOCKED       │ TERMINATED = final, cannot     │
│                                    │ restart (one-way only)         │
└────────────────────────────────────┴────────────────────────────────┘
```

---

**Architect Perspective:**

```
Debugging thread hangs?
  ├─ Get thread dump (kill -3 or jstack pid)
  ├─ Look at state:
  │  ├─ BLOCKED → Thread deadlock (waiting for lock)
  │  ├─ WAITING → Thread waiting for signal forever
  │  ├─ TIMED_WAITING → OK (sleeping, will resume)
  │  └─ RUNNABLE → Thread should be running (or waiting for CPU)
  └─ Fix based on state

Example: "Thread stuck waiting for lock"
  → Check which thread holds the lock
  → Why isn't it releasing?
  → Deadlock? Infinite loop? Exception?
```

---

### Q5: ConcurrentHashMap vs Synchronized Map - Deep Dive

**Layman Explanation:**

- **ConcurrentHashMap** - Like a restaurant with 16 tables (buckets); multiple servers can work on different tables at the same time (parallel)
- **SynchronizedMap** - Like a restaurant with only 1 door; only one server can enter at a time, everyone else waits (sequential)

---

**ConcurrentHashMap (Enriched - Shows Parallel Execution):**

```java
import java.util.concurrent.ConcurrentHashMap;

class Worker extends Thread {
    private ConcurrentHashMap<String, String> map;
    private String key, value;
    private boolean isPut;
    
    public Worker(String name, ConcurrentHashMap<String, String> map, String key, String value) {
        super(name);
        this.map = map;
        this.key = key;
        this.value = value;
        this.isPut = true;
    }
    
    public Worker(String name, ConcurrentHashMap<String, String> map) {
        super(name);
        this.map = map;
        this.isPut = false;
    }
    
    public void run() {
        if (isPut) {
            long start = System.currentTimeMillis();
            map.put(key, value);
            long time = System.currentTimeMillis() - start;
            System.out.println("[" + getName() + "] Put " + key + "=" + value + " (took " + time + "ms)");
        } else {
            long start = System.currentTimeMillis();
            String g = map.get("greeting");
            String t = map.get("target");
            long time = System.currentTimeMillis() - start;
            System.out.println("[" + getName() + "] Get: " + g + " " + t + " (took " + time + "ms)");
        }
    }
}

import java.util.concurrent.*;
import java.util.*;

public class ConcurrentHashMapExample {
  public static void main(String[] args) throws InterruptedException {
    ConcurrentHashMap < String, String > sharedMap = new ConcurrentHashMap < > ();
    System.out.println("=== SynchronizedMap (Entire Map Locking) ===\n");
    long startTime = System.currentTimeMillis();
    // Create 1000 put threads
    Worker[] putThreads = new Worker[10000];
    for (int i = 0; i < 10000; i++) {
      putThreads[i] = new Worker("putThread" + i, sharedMap, "key" + i, i);
    }

    // Create 1000 get threads
    Worker[] getThreads = new Worker[10000];
    for (int i = 0; i < 10000; i++) {
      getThreads[i] = new Worker("getThread" + i, sharedMap, "key" + i);
    }

    // Start all put threads
    for (Worker t: putThreads) {
      t.start();
    }

    // Start all get threads
    for (Worker t: getThreads) {
      t.start();
    }

    // Join all put threads
    for (Worker t: putThreads) {
      t.join();
    }

    // Join all get threads
    for (Worker t: getThreads) {
      t.join();
    }

    long totalTime = System.currentTimeMillis() - startTime;

    System.out.println("\n⚠ Total time: " + totalTime + "ms");
    System.out.println("✓ Threads ran in PARALLEL (different buckets)");
    System.out.println("✓ Get operations proceeded while Put operations happened\n");
  }
}

class Worker extends Thread {
  private String workerName;
  private Map < String, Integer > sharedMap;
  private boolean isPut;
  private String key;
  private Integer value;

  public Worker() {

  }

  //Put Thread
  public Worker(String workerName, Map map, String key, Integer value) {
    this.workerName = workerName;
    this.sharedMap = map;
    this.isPut = true;
    this.key = key;
    this.value = value;
  }

  //Get Thread
  public Worker(String workerName, Map map, String key) {
    this.workerName = workerName;
    this.sharedMap = map;
    this.isPut = false;
    this.key = key;
  }

  public void run() {
    if (isPut) {
      long start = System.currentTimeMillis();
      sharedMap.put(this.key, this.value);
      System.out.println("Time taken for put:" + "Key:" + this.key + " Value:" + this.value + " " + (System.currentTimeMillis() - start) + " ms");
    } else {
      long start = System.currentTimeMillis();
      Integer value = sharedMap.get(this.key);
      System.out.println("Time taken for get:" + value + " Time taken for put:" + (System.currentTimeMillis() - start) + " ms");
    }
  }
}
```

---

**SynchronizedMap (Enriched - Shows Sequential Execution):**

```java
import java.util.*;

class Worker extends Thread {
    private Map<String, String> map;
    private String key, value;
    private boolean isPut;
    
    public Worker(String name, Map<String, String> map, String key, String value) {
        super(name);
        this.map = map;
        this.key = key;
        this.value = value;
        this.isPut = true;
    }
    
    public Worker(String name, Map<String, String> map) {
        super(name);
        this.map = map;
        this.isPut = false;
    }
    
    public void run() {
        if (isPut) {
            long start = System.currentTimeMillis();
            map.put(key, value);
            long time = System.currentTimeMillis() - start;
            System.out.println("[" + getName() + "] Put " + key + "=" + value + " (took " + time + "ms)");
        } else {
            long start = System.currentTimeMillis();
            String g = map.get("greeting");
            String t = map.get("target");
            long time = System.currentTimeMillis() - start;
            System.out.println("[" + getName() + "] Get: " + g + " " + t + " (took " + time + "ms)");
        }
    }
}

import java.util.*;

public class SynchronizedMapExample {
  public static void main(String[] args) throws InterruptedException {
    Map < String, Integer > sharedMap = Collections.synchronizedMap(new HashMap());
    System.out.println("=== SynchronizedMap (Entire Map Locking) ===\n");
    long startTime = System.currentTimeMillis();
    // Create 10000 put threads
    Worker[] putThreads = new Worker[10000];
    for (int i = 0; i < 10000; i++) {
      putThreads[i] = new Worker("putThread" + i, sharedMap, "key" + i, i);
    }

    // Create 10000 get threads
    Worker[] getThreads = new Worker[10000];
    for (int i = 0; i < 10000; i++) {
      getThreads[i] = new Worker("getThread" + i, sharedMap, "key" + i);
    }

    // Start all put threads
    for (Worker t: putThreads) {
      t.start();
    }

    // Start all get threads
    for (Worker t: getThreads) {
      t.start();
    }

    // Join all put threads
    for (Worker t: putThreads) {
      t.join();
    }

    // Join all get threads
    for (Worker t: getThreads) {
      t.join();
    }

    long totalTime = System.currentTimeMillis() - startTime;

    System.out.println("\n⚠ Total time: " + totalTime + "ms");
    System.out.println("⚠ Threads ran SEQUENTIALLY (one at a time)");
    System.out.println("⚠ Get/Put operations waited for each other to finish\n");
  }
}

class Worker extends Thread {
  private String workerName;
  private Map < String, Integer > sharedMap;
  private boolean isPut;
  private String key;
  private Integer value;

  public Worker() {

  }

  //Put Thread
  public Worker(String workerName, Map map, String key, Integer value) {
    this.workerName = workerName;
    this.sharedMap = map;
    this.isPut = true;
    this.key = key;
    this.value = value;
  }

  //Get Thread
  public Worker(String workerName, Map map, String key) {
    this.workerName = workerName;
    this.sharedMap = map;
    this.isPut = false;
    this.key = key;
  }

  public void run() {
    if (isPut) {
      long start = System.currentTimeMillis();
      sharedMap.put(this.key, this.value);
      System.out.println("Time taken for put:" + "Key:" + this.key + " Value:" + this.value + " " + (System.currentTimeMillis() - start) + " ms");
    } else {
      long start = System.currentTimeMillis();
      Integer value = sharedMap.get(this.key);
      System.out.println("Time taken for get:" + value + " Time taken for put:" + (System.currentTimeMillis() - start) + " ms");
    }
  }
}
```

---

**Understanding Bucket Distribution - Which Bucket Was Used?**

```java
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Field;

public class CHMBucketMapping {
    public static void main(String[] args) throws Exception {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        
        // Get bucket count
        int buckets = getBuckets(map);
        System.out.println("Total buckets: " + buckets + "\n");
        
        // Add entries and show which bucket each goes to
        String[] keys = {"greeting", "target", "key1", "key2", "key3"};
        String[] values = {"Hello", "World", "value1", "value2", "value3"};
        
        System.out.println("--- PUT Operations ---\n");
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], values[i]);
            int bucketIndex = getBucketIndex(keys[i], buckets);
            System.out.println("Key: " + keys[i] + 
                             " | Value: " + values[i] + 
                             " | Bucket: " + bucketIndex);
        }
        
        System.out.println("\n--- GET Operations ---\n");
        
        // Get entries and show which bucket was used
        for (String key : keys) {
            String value = map.get(key);
            int bucketIndex = getBucketIndex(key, buckets);
            System.out.println("Get key: " + key + 
                             " | Got value: " + value + 
                             " | From bucket: " + bucketIndex);
        }
    }
    
    // Calculate which bucket a key goes to
    public static int getBucketIndex(String key, int bucketCount) {
        int hash = key.hashCode();
        return hash & (bucketCount - 1);
    }
    
    // Get number of buckets
    public static int getBuckets(ConcurrentHashMap<String, String> map) throws Exception {
        Field tableField = ConcurrentHashMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(map);
        return table == null ? 0 : table.length;
    }
}

/* OUTPUT:
Total buckets: 16

--- PUT Operations ---

Key: greeting | Value: Hello | Bucket: 3
Key: target | Value: World | Bucket: 8
Key: key1 | Value: value1 | Bucket: 2
Key: key2 | Value: value2 | Bucket: 2
Key: key3 | Value: value3 | Bucket: 2

--- GET Operations ---

Get key: greeting | Got value: Hello | From bucket: 3
Get key: target | Got value: World | From bucket: 8
Get key: key1 | Got value: value1 | From bucket: 2
Get key: key2 | Got value: value2 | From bucket: 2
Get key: key3 | Got value: value3 | From bucket: 2
*/
```

**Key Insight - Bucket Distribution:**

```
Bucket Distribution (16 buckets):
┌────────────────────────────────┐
│ Bucket 0:  (empty)             │
│ Bucket 1:  (empty)             │
│ Bucket 2:  key1, key2, key3    │ ← Multiple keys in same bucket
│ Bucket 3:  greeting            │
│ Bucket 4-7:  (empty)           │
│ Bucket 8:  target              │
│ Bucket 9-15:  (empty)          │
└────────────────────────────────┘

Why this matters for ConcurrentHashMap:
├─ Thread-1 accesses Bucket 2 (key1, key2, key3) → Only Bucket 2 locked
├─ Thread-2 accesses Bucket 8 (target) → Only Bucket 8 locked
└─ Threads can work in PARALLEL! (different buckets)
```

**Comparison Table:**

| Metric | ConcurrentHashMap | SynchronizedMap |
|--------|-------------------|-----------------|
| **Total Time** | 5ms | 150ms |
| **Speedup** | — | **30x slower** |
| **Execution** | Parallel (bucket-level) | Sequential (entire map locked) |
| **Put operations** | Both ~1ms | 1ms + 25ms (wait) |
| **Get operations** | All ~0ms (no wait) | 52ms, 75ms, 98ms (waiting) |
| **Locking** | One lock for entire map | Multiple locks (buckets/segments) |
| **Throughput** | High (low contention) | Low (high contention) |
| **Iteration** | Safe (weakly consistent) | Must manually synchronize |

**Key Benefits of ConcurrentHashMap:**

1. **Parallel Execution**: Multiple threads can access different buckets simultaneously
2. **No Blocking on Reads**: Get operations don't block Put operations (different buckets)
3. **High Throughput**: 30x faster than SynchronizedMap in concurrent scenarios
4. **Scalable**: Better performance with more threads
5. **Bucket-Level Locking**: Only the bucket being accessed is locked, not the entire map

**Architect Recommendation:**
- Default to `ConcurrentHashMap` in multi-threaded scenarios
- Use `ConcurrentHashMap.putIfAbsent()` for lazy initialization
- Use `ConcurrentHashMap.compute()` for atomic read-modify-write operations
- Only use SynchronizedMap for simple, low-concurrency scenarios

---

### Q6: ReentrantLock vs Lock Interface

**6 Key Features (Layman Explanation):**

1. **Non-blocking tryLock()** - Try to grab the lock instantly; if available take it, else move on (don't wait)
2. **Timeout-based locking** - Try to grab the lock for max 2 seconds, then give up if still waiting
3. **Fair lock** - Threads get the lock in the order they asked for it (like a queue at a shop)
4. **Multiple conditions** - Different waiting rooms; producer waits for space, consumer waits for data
5. **Lock queries** - Ask "is someone using the lock?" or "how many are waiting?"
6. **Interruptible locking** - A waiting thread can be told to stop waiting and do something else

```java
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ReentrantProducerConsumerExample {
  public static void main(String[] args) throws InterruptedException {
    ReentrantLock sharedLock = new ReentrantLock();
    Condition sharedDataAvailableCondition = sharedLock.newCondition();
    Condition sharedSpaceAvailableCondition = sharedLock.newCondition();
    String[] sharedArray = new String[5];//Buffer can hold max at a time
    AtomicInteger count = new AtomicInteger(0);
    
    Producer producerThread = new Producer(sharedLock, sharedDataAvailableCondition, sharedSpaceAvailableCondition, sharedArray, count);
    Consumer consumerThread = new Consumer(sharedLock, sharedDataAvailableCondition, sharedSpaceAvailableCondition, sharedArray, count);

    producerThread.start();
    Thread.sleep(200);
    consumerThread.start();

    producerThread.join();
    consumerThread.join();
  }
}

class Producer extends Thread {
  private ReentrantLock sharedLock;
  private Condition sharedDataAvailableCondition;//will signal condition
  private Condition sharedSpaceAvailableCondition;//will await for this condition
  private String[] sharedArray;
  AtomicInteger count;

  public Producer() {

  }

  public Producer(ReentrantLock sharedLock, Condition sharedDataAvailableCondition, Condition sharedSpaceAvailableCondition, String[] sharedArray, AtomicInteger count) {
    this.sharedLock = sharedLock;
    this.sharedSpaceAvailableCondition = sharedSpaceAvailableCondition;
    this.sharedDataAvailableCondition = sharedDataAvailableCondition;
    this.sharedArray = sharedArray;
    this.count = count;
  }

  public void run() {
    sharedLock.lock();
    try {
      for (int i = 0; i < 100; i++) {
        if (count.get() == sharedArray.length) {//check if array is FULL (count reached max 5 limit)
          System.out.println("Array is full, wait for consumer to consume at least 1 message");
          sharedSpaceAvailableCondition.await(); //wait for the space to be available, after consumer consumes at least one Datapoint
        } else {
          sharedArray[count.get()] = "Data-" + i;
          System.out.println("Data produced at " + count + "th position: " + sharedArray[count.get()] );
          count.incrementAndGet();
          sharedDataAvailableCondition.signal(); //Array has at least one DataPoint for Consumer to consume
        }
      }
    } catch (InterruptedException iex) {
      Thread.currentThread().interrupt();

    } finally {
      sharedLock.unlock();
    }

  }
}

class Consumer extends Thread {
  private ReentrantLock sharedLock;
  private Condition sharedDataAvailableCondition;//will await for this condition
  private Condition sharedSpaceAvailableCondition;//will signal condition
  private String[] sharedArray;
  AtomicInteger count;
  
  public Consumer() {

  }

  public Consumer(ReentrantLock sharedLock, Condition sharedDataAvailableCondition, Condition sharedSpaceAvailableCondition, String[] sharedArray, AtomicInteger count) {
    this.sharedLock = sharedLock;
    this.sharedSpaceAvailableCondition = sharedSpaceAvailableCondition;
    this.sharedDataAvailableCondition = sharedDataAvailableCondition;
    this.sharedArray = sharedArray;
    this.count = count;
  }

  public void run() {
    sharedLock.lock();
    try {
      for (int i = 0; i < 100; i++) {
        if (count.get() == 0) {//check if array is EMPTY (count is 0)
          System.out.println("Array is empty, wait for Producer to produce at least 1 message");
          sharedDataAvailableCondition.await(); //wait for the data to be available, after Producer produces at least one Datapoint
        } else {
          String data = sharedArray[count.get()-1];
          count.decrementAndGet(); 
          System.out.println("Data consumed at " + i + "th position: " + data);
          sharedSpaceAvailableCondition.signal(); //Array has at least one DataPoint for Consumer to consume
        }
      }
    } catch (InterruptedException iex) {
      Thread.currentThread().interrupt();

    } finally {
      sharedLock.unlock();
    }

  }
}

/* OUTPUT:
Data produced at 0th position: Data-0
Data produced at 1th position: Data-1
Data produced at 2th position: Data-2
Data produced at 3th position: Data-3
Data produced at 4th position: Data-4
Array is not empty, wait for consumer to consume
Data consumed at 0th position: Data-4
Data produced at 5th position: Data-5
Data consumed at 1th position: Data-3
Data produced at 6th position: Data-6
Data consumed at 2th position: Data-2
Data produced at 7th position: Data-7
...
[Producer and Consumer interleave, showing coordination via await/signal]
*/
```

---

## Solution #2: Improved Producer-Consumer (Best Practices)

**Observations on Above Solution:**

❌ **Issue 1: Deadlock Risk** - Producer produces 100 items, Consumer tries to consume 100 items. If mismatch or after one exits, other hangs forever waiting for signal.

❌ **Issue 2: LIFO Instead of FIFO** - Consumes from TOP (position count-1), not bottom. Should use Queue, not Array.

❌ **Issue 3: AtomicInteger Redundant** - All access already protected by lock. No need for AtomicInteger (adds overhead).

❌ **Issue 4: Holds Lock Too Long** - Entire for loop runs with lock held. Lock should be released immediately after each operation, not held for 100 iterations.

✅ **Best Practice:** Release lock ASAP after each operation + Use Queue for FIFO + Cleanup signals when done.

```java
import java.util.concurrent.locks.*;
import java.util.LinkedList;
import java.util.Queue;

public class ImprovedProducerConsumer {
    public static void main(String[] args) throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        Condition notEmpty = lock.newCondition();
        Condition notFull = lock.newCondition();
        
        Queue<String> buffer = new LinkedList<>();
        int MAX_SIZE = 5;
        
        Producer producer = new Producer(lock, notEmpty, notFull, buffer, MAX_SIZE);
        Consumer consumer = new Consumer(lock, notEmpty, notFull, buffer);
        
        producer.start();
        consumer.start();
        
        producer.join();
        consumer.join();
    }
}

class Producer extends Thread {
    private ReentrantLock lock;
    private Condition notEmpty, notFull;
    private Queue<String> buffer;
    private int maxSize;
    
    public Producer(ReentrantLock lock, Condition notEmpty, Condition notFull, 
                    Queue<String> buffer, int maxSize) {
        this.lock = lock;
        this.notEmpty = notEmpty;
        this.notFull = notFull;
        this.buffer = buffer;
        this.maxSize = maxSize;
    }
    
    public void run() {
        for (int i = 0; i < 10; i++) {
            lock.lock();  // Acquire lock
            try {
                while (buffer.size() == maxSize) {
                    System.out.println("Buffer full, waiting...");
                    notFull.await();  // Release lock while waiting
                }
                String data = "Data-" + i;
                buffer.offer(data);  // Add to queue (FIFO)
                System.out.println("Produced: " + data + ", Size: " + buffer.size());
                notEmpty.signal();  // Wake up consumer
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();  // Release lock IMMEDIATELY
            }
        }
        
        // Signal consumer that production is DONE
        lock.lock();
        try {
            notEmpty.signalAll();  // Wake consumer so it can exit gracefully
        } finally {
            lock.unlock();
        }
    }
}

class Consumer extends Thread {
    private ReentrantLock lock;
    private Condition notEmpty, notFull;
    private Queue<String> buffer;
    
    public Consumer(ReentrantLock lock, Condition notEmpty, Condition notFull, Queue<String> buffer) {
        this.lock = lock;
        this.notEmpty = notEmpty;
        this.notFull = notFull;
        this.buffer = buffer;
    }
    
    public void run() {
        for (int i = 0; i < 10; i++) {
            lock.lock();  // Acquire lock
            try {
                while (buffer.isEmpty()) {
                    System.out.println("Buffer empty, waiting...");
                    notEmpty.await();  // Release lock while waiting
                }
                String data = buffer.poll();  // Remove from queue (FIFO)
                System.out.println("Consumed: " + data + ", Size: " + buffer.size());
                notFull.signal();  // Wake up producer
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();  // Release lock IMMEDIATELY
            }
        }
    }
}

/* OUTPUT:
Produced: Data-0, Size: 1
Consumed: Data-0, Size: 0
Produced: Data-1, Size: 1
Produced: Data-2, Size: 2
Consumed: Data-1, Size: 1
Produced: Data-3, Size: 2
Produced: Data-4, Size: 3
Consumed: Data-2, Size: 2
Produced: Data-5, Size: 3
Consumed: Data-3, Size: 2
...
(All 10 items produced and consumed in FIFO order without deadlock)
*/
```

**Key Improvements:**
- ✅ Lock acquired just before operation, released immediately after
- ✅ Uses Queue (FIFO), not Array (LIFO)
- ✅ Uses `int` for count (via queue.size()), not AtomicInteger
- ✅ Cleanup signal at end prevents consumer hanging
- ✅ No deadlock risk - clean shutdown
- ✅ Proper while loops for spurious wakeup safety

**When to Use Each:**
- **Issue's solution**: Teaching purpose (shows await/signal), not production-ready
- **Improved solution**: Production-ready code, handles edge cases, follows best practices

---

### Q7: Semaphore - Use Cases and Patterns

**Semaphore Concept:**
```java
Semaphore semaphore = new Semaphore(3); // 3 permits
// Acts like a counter that can go negative
// acquire() -> counter--, blocks if counter < 0
// release() -> counter++, wakes up blocked threads
```

---

**Use Case 1: Connection Pool (Resource Limiting):**

```java
import java.util.concurrent.Semaphore;

class UserTask extends Thread {
    private final ConnectionPool pool;
    private final int userId;
    
    public UserTask(ConnectionPool pool, int userId) {
        super("User-" + userId);
        this.pool = pool;
        this.userId = userId;
    }
    
    public void run() {
        try {
            pool.useConnection("User-" + userId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

public class ConnectionPool {
    private final Semaphore semaphore;
    private final int poolSize = 3;
    
    public ConnectionPool() {
        this.semaphore = new Semaphore(poolSize);
    }
    
    public void useConnection(String userId) throws InterruptedException {
        System.out.println("[" + userId + "] Waiting for connection...");
        long start = System.currentTimeMillis();
        
        semaphore.acquire();
        long waitTime = System.currentTimeMillis() - start;
        
        System.out.println("[" + userId + "] Got connection (waited " + waitTime + "ms)");
        Thread.sleep(1000);
        System.out.println("[" + userId + "] Releasing connection");
        semaphore.release();
    }
    
    public void run() throws InterruptedException {
        System.out.println("=== Connection Pool with 3 Available Connections ===\n");
        
        Thread[] threads = new Thread[5];
        for (int i = 1; i <= 5; i++) {
            threads[i-1] = new UserTask(this, i);
        }
        
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        
        System.out.println("\n✓ All users processed");
    }
    
    public static void main(String[] args) throws InterruptedException {
        new ConnectionPool().run();
    }
}

/* OUTPUT:
=== Connection Pool with 3 Available Connections ===

[User-1] Waiting for connection...
[User-2] Waiting for connection...
[User-3] Waiting for connection...
[User-4] Waiting for connection...
[User-5] Waiting for connection...
[User-1] Got connection (waited 1ms)
[User-2] Got connection (waited 2ms)
[User-3] Got connection (waited 3ms)
[User-4] Waiting...          <- Blocked! Only 3 connections
[User-5] Waiting...          <- Blocked! Only 3 connections
[User-1] Releasing connection
[User-4] Got connection (waited 1050ms)   <- Now gets connection
[User-2] Releasing connection
[User-5] Got connection (waited 1100ms)
[User-3] Releasing connection
[User-4] Releasing connection
[User-5] Releasing connection

✓ All users processed
*/
```

---

**Use Case 2: Rate Limiting (Requests Per Second):**

```java
import java.util.concurrent.Semaphore;

class ClientTask extends Thread {
    private final ApiRateLimiter limiter;
    private final int clientId;
    
    public ClientTask(ApiRateLimiter limiter, int clientId) {
        super("Client-" + clientId);
        this.limiter = limiter;
        this.clientId = clientId;
    }
    
    public void run() {
        try {
            limiter.callApi("Client-" + clientId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

public class ApiRateLimiter {
    private final Semaphore limiter;
    private final int maxRequests = 3;
    
    public ApiRateLimiter() {
        this.limiter = new Semaphore(maxRequests);
    }
    
    public void callApi(String clientId) throws InterruptedException {
        System.out.println("[" + clientId + "] Requesting API...");
        long start = System.currentTimeMillis();
        
        limiter.acquire();
        long waitTime = System.currentTimeMillis() - start;
        
        System.out.println("[" + clientId + "] API Call allowed (waited " + waitTime + "ms)");
        Thread.sleep(500);
        System.out.println("[" + clientId + "] API Call done");
        limiter.release();
    }
    
    public void run() throws InterruptedException {
        System.out.println("=== Rate Limiting: Max 3 Concurrent Requests ===\n");
        
        Thread[] threads = new Thread[6];
        for (int i = 1; i <= 6; i++) {
            threads[i-1] = new ClientTask(this, i);
        }
        
        long startTime = System.currentTimeMillis();
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        long totalTime = System.currentTimeMillis() - startTime;
        
        System.out.println("\nTotal time: " + totalTime + "ms");
        System.out.println("(With rate limiting, requests had to queue)");
    }
    
    public static void main(String[] args) throws InterruptedException {
        new ApiRateLimiter().run();
    }
}

/* OUTPUT:
=== Rate Limiting: Max 3 Concurrent Requests ===

[Client-1] Requesting API...
[Client-2] Requesting API...
[Client-3] Requesting API...
[Client-4] Requesting API...
[Client-5] Requesting API...
[Client-6] Requesting API...
[Client-1] API Call allowed (waited 1ms)
[Client-2] API Call allowed (waited 2ms)
[Client-3] API Call allowed (waited 2ms)
[Client-4] API Call allowed (waited 3ms)   <- Blocked until a permit freed
[Client-5] API Call allowed (waited 504ms) <- Had to wait
[Client-6] API Call allowed (waited 505ms) <- Had to wait
[Client-1] API Call done
[Client-2] API Call done
[Client-3] API Call done
[Client-4] API Call done
[Client-5] API Call done
[Client-6] API Call done

Total time: 1055ms
(With rate limiting, requests had to queue)
*/
```

---

**Use Case 3: Binary Semaphore (Acts as Mutex/Lock):**

```java
import java.util.concurrent.Semaphore;

class CriticalSectionTask extends Thread {
    private final BinarySemaphore binarySemaphore;
    private final int threadId;
    
    public CriticalSectionTask(BinarySemaphore binarySemaphore, int threadId) {
        super("Thread-" + threadId);
        this.binarySemaphore = binarySemaphore;
        this.threadId = threadId;
    }
    
    public void run() {
        try {
            binarySemaphore.criticalSection("Thread-" + threadId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

public class BinarySemaphore {
    private final Semaphore mutex = new Semaphore(1);
    private int sharedCounter = 0;
    
    public void criticalSection(String threadName) throws InterruptedException {
        System.out.println("[" + threadName + "] Waiting for lock...");
        
        mutex.acquire();
        try {
            System.out.println("[" + threadName + "] Entered critical section");
            
            int temp = sharedCounter;
            Thread.sleep(100);
            sharedCounter = temp + 1;
            
            System.out.println("[" + threadName + "] Updated counter to " + sharedCounter);
        } finally {
            mutex.release();
            System.out.println("[" + threadName + "] Exited critical section");
        }
    }
    
    public void run() throws InterruptedException {
        System.out.println("=== Binary Semaphore (Mutex Lock) ===\n");
        
        Thread[] threads = new Thread[3];
        for (int i = 1; i <= 3; i++) {
            threads[i-1] = new CriticalSectionTask(this, i);
        }
        
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        
        System.out.println("\nFinal counter value: " + sharedCounter);
        System.out.println("(Binary semaphore prevented race conditions)");
    }
    
    public static void main(String[] args) throws InterruptedException {
        new BinarySemaphore().run();
    }
}

/* OUTPUT:
=== Binary Semaphore (Mutex Lock) ===

[Thread-1] Waiting for lock...
[Thread-2] Waiting for lock...
[Thread-3] Waiting for lock...
[Thread-1] Entered critical section
[Thread-2] Waiting...  <- Blocked, only 1 permit
[Thread-3] Waiting...  <- Blocked, only 1 permit
[Thread-1] Updated counter to 1
[Thread-1] Exited critical section
[Thread-2] Entered critical section
[Thread-2] Updated counter to 2
[Thread-2] Exited critical section
[Thread-3] Entered critical section
[Thread-3] Updated counter to 3
[Thread-3] Exited critical section

Final counter value: 3
(Binary semaphore prevented race conditions)
*/
```

---

**Semaphore Implementation 1: Using `synchronized`**

```java
class SynchronizedSemaphore {
  private int permits;
  
  public SynchronizedSemaphore(int permits) {
    this.permits = permits;
  }
  
  public synchronized void acquire() throws InterruptedException {
    while (permits == 0) {  // No permits available
      System.out.println(Thread.currentThread().getName() + ": No permits, waiting...");
      wait();               // Wait for permit
    }
    permits--;              // Take a permit
    System.out.println(Thread.currentThread().getName() + ": Acquired permit (remaining: " + permits + ")");
  }
  
  public synchronized void release() {
    permits++;              // Release permit
    System.out.println(Thread.currentThread().getName() + ": Released permit (available: " + permits + ")");
    notifyAll();            // Wake up waiting threads
  }
}

class SyncWorker extends Thread {
  private SynchronizedSemaphore semaphore;
  
  public SyncWorker(SynchronizedSemaphore semaphore, String taskName) {
    super(taskName);
    this.semaphore = semaphore;
  }
  
  public void run() {
    try {
      System.out.println(getName() + ": Trying to acquire semaphore...");
      semaphore.acquire();
      
      System.out.println(getName() + ": USING RESOURCE (doing work for 500ms)");
      Thread.sleep(500);
      
      System.out.println(getName() + ": DONE, releasing...");
      semaphore.release();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}

public class SynchronizedSemaphoreDemo {
  public static void main(String[] args) throws InterruptedException {
    SynchronizedSemaphore semaphore = new SynchronizedSemaphore(2);  // Max 2
    
    System.out.println("=== Semaphore using synchronized (Max 2 concurrent) ===\n");
    
    Thread t1 = new SyncWorker(semaphore, "Task-1");
    Thread t2 = new SyncWorker(semaphore, "Task-2");
    Thread t3 = new SyncWorker(semaphore, "Task-3");
    Thread t4 = new SyncWorker(semaphore, "Task-4");
    
    t1.start(); t2.start(); t3.start(); t4.start();
    t1.join(); t2.join(); t3.join(); t4.join();
    
    System.out.println("\nDone");
  }
}
```

---

**Semaphore Implementation 2: Using `ReentrantLock + Condition`**

```java
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

class LockSemaphore {
  private int permits;
  private ReentrantLock lock = new ReentrantLock();
  private Condition condition = lock.newCondition();
  
  public LockSemaphore(int permits) {
    this.permits = permits;
  }
  
  public void acquire() throws InterruptedException {
    lock.lock();
    try {
      while (permits == 0) {  // No permits available
        System.out.println(Thread.currentThread().getName() + ": No permits, waiting...");
        condition.await();    // Wait for permit
      }
      permits--;              // Take a permit
      System.out.println(Thread.currentThread().getName() + ": Acquired permit (remaining: " + permits + ")");
    } finally {
      lock.unlock();
    }
  }
  
  public void release() {
    lock.lock();
    try {
      permits++;              // Release permit
      System.out.println(Thread.currentThread().getName() + ": Released permit (available: " + permits + ")");
      condition.signalAll();  // Wake up waiting threads
    } finally {
      lock.unlock();
    }
  }
}

class LockWorker extends Thread {
  private LockSemaphore semaphore;
  
  public LockWorker(LockSemaphore semaphore, String taskName) {
    super(taskName);
    this.semaphore = semaphore;
  }
  
  public void run() {
    try {
      System.out.println(getName() + ": Trying to acquire semaphore...");
      semaphore.acquire();
      
      System.out.println(getName() + ": USING RESOURCE (doing work for 500ms)");
      Thread.sleep(500);
      
      System.out.println(getName() + ": DONE, releasing...");
      semaphore.release();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}

public class LockSemaphoreDemo {
  public static void main(String[] args) throws InterruptedException {
    LockSemaphore semaphore = new LockSemaphore(2);  // Max 2
    
    System.out.println("=== Semaphore using ReentrantLock (Max 2 concurrent) ===\n");
    
    Thread t1 = new LockWorker(semaphore, "Task-1");
    Thread t2 = new LockWorker(semaphore, "Task-2");
    Thread t3 = new LockWorker(semaphore, "Task-3");
    Thread t4 = new LockWorker(semaphore, "Task-4");
    
    t1.start(); t2.start(); t3.start(); t4.start();
    t1.join(); t2.join(); t3.join(); t4.join();
    
    System.out.println("\nDone");
  }
}
```

---

**Comparison: All 3 Semaphore Implementations**

| Feature | Built-in Semaphore | synchronized | ReentrantLock |
|---------|-------------------|--------------|---------------|
| **Code Complexity** | ✅ Simple (built-in) | ⚠️ Medium (manual counting) | ⚠️ Medium (manual counting) |
| **Lines of Code** | ✅ 5 lines | ⚠️ 30+ lines | ⚠️ 40+ lines |
| **Fairness** | ✅ Fair (configurable) | ❌ Unfair | ✅ Fair (configurable) |
| **Flexibility** | ✅ Simple acquire/release | ⚠️ Basic | ✅ Multiple conditions |
| **Performance** | ✅ Optimized | ⚠️ Slower | ⚠️ Slower (CAS) |
| **Readability** | ✅ Clear intent | ⚠️ Confusing (why counting?) | ⚠️ Verbose |
| **Production Use** | ✅ BEST | ⚠️ Acceptable | ⚠️ Acceptable |
| **When to Use** | ✅ Resource limiting, rate limiting | Basic synchronization only | Complex coordination |

**Key Takeaway:**
```java
// ✅ ALWAYS use built-in Semaphore for semaphore-like behavior
Semaphore sem = new Semaphore(2);  // Clear, optimized, battle-tested

// ⚠️ Use synchronized/ReentrantLock only if you MUST implement it manually
// (e.g., in an interview or educational setting)
```

---

### Q8: CountDownLatch vs CyclicBarrier - The Difference

**Layman Explanation (Why not just use Thread.join()?)**

**Analogy: Waiting for a friend at a coffee shop**

```
❌ WRONG WAY (like join()):
Friend arrives → finishes task → STILL WORKING (keeps laptop open)
You: "I'll wait for friend to LEAVE"
You: wait... wait... FOREVER ❌ (friend never leaves)

✅ RIGHT WAY (like CountDownLatch):
Friend arrives → finishes task → rings BELL
You: "I'll wait for the BELL to ring"
You: hear BELL! ✅ (continue, friend keeps working)
```

**In Code:**
```java
❌ join() = Wait for THREAD TO DIE (thread must terminate)
Thread t = new Thread(() -> {
  doWork();      // Task done
  
  while (true) { // But thread keeps running
    monitor();
  }
});
t.start();
t.join();  // ❌ STUCK FOREVER! Thread never terminates

✅ CountDownLatch = Wait for SIGNAL (thread can keep running)
CountDownLatch latch = new CountDownLatch(1);
Thread t = new Thread(() -> {
  doWork();
  latch.countDown();  // RING BELL! ✅
  
  while (true) { // Thread can keep running
    monitor();
  }
});
t.start();
latch.await();  // ✅ Returns immediately! Thread still running
```

**Key Difference:**
- **join()** = Wait for thread to stop existing (must die)
- **CountDownLatch** = Wait for signal (thread can keep running)
- **CyclicBarrier** = Threads wait for each other (mutual synchronization)

---

**CountDownLatch - Real Business Scenario:**

**Scenario: Database Migration at Startup**
- Main application thread waits for all database schema migrations to complete
- Each migration script runs in parallel in a separate thread
- Only after all migrations finish, the application starts accepting requests

**CyclicBarrier - Real Business Scenario:**

**Scenario: Multi-Phase Data Processing Pipeline**
- 3 data processors (Validation → Transformation → Storage)
- Phase 1: All processors validate their data chunk
- All wait at barrier, then move to Phase 2
- Phase 2: All processors transform data
- All wait at barrier, then move to Phase 3
- Phase 3: All processors store data

---

**CountDownLatch - Database Migration (Full Working Example):**

```java
import java.util.concurrent.CountDownLatch;

class MigrationTask extends Thread {
    private final CountDownLatch completionSignal;
    private final int migrationId;
    
    public MigrationTask(CountDownLatch completionSignal, int migrationId) {
        super("Migration-" + migrationId);
        this.completionSignal = completionSignal;
        this.migrationId = migrationId;
    }
    
    public void run() {
        try {
            System.out.println("[" + getName() + "] Starting database migration...");
            Thread.sleep(1500);  // Simulate migration work
            System.out.println("[" + getName() + "] Migration completed ✓");
            completionSignal.countDown();  // Signal that this migration is done
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

public class DatabaseMigrationDemo {
    public void run() throws InterruptedException {
        System.out.println("=== CountDownLatch: Database Migration at Startup ===\n");
        
        CountDownLatch migrationsDone = new CountDownLatch(4);  // 4 migrations to run
        
        System.out.println("[App] Starting 4 database migrations in parallel...\n");
        
        // Launch 4 migration tasks in parallel
        for (int i = 1; i <= 4; i++) {
            new MigrationTask(migrationsDone, i).start();
        }
        
        System.out.println("[App] Waiting for all migrations to complete...\n");
        migrationsDone.await();  // Main thread waits for all migrations
        
        System.out.println("\n[App] ✓ All migrations completed!");
        System.out.println("[App] Starting application and accepting requests...");
    }
    
    public static void main(String[] args) throws InterruptedException {
        new DatabaseMigrationDemo().run();
    }
}

/* OUTPUT:
=== CountDownLatch: Database Migration at Startup ===

[App] Starting 4 database migrations in parallel...

[App] Waiting for all migrations to complete...

[Migration-1] Starting database migration...
[Migration-2] Starting database migration...
[Migration-3] Starting database migration...
[Migration-4] Starting database migration...

(1.5 second work)

[Migration-1] Migration completed ✓
[Migration-2] Migration completed ✓
[Migration-3] Migration completed ✓
[Migration-4] Migration completed ✓

[App] ✓ All migrations completed!
[App] Starting application and accepting requests...
*/
```

---

**CyclicBarrier - Multi-Phase Data Processing (Full Working Example):**

```java
import java.util.concurrent.CyclicBarrier;

class DataProcessor extends Thread {
    private final CyclicBarrier phaseBarrier;
    private final int processorId;
    
    public DataProcessor(CyclicBarrier phaseBarrier, int processorId) {
        super("Processor-" + processorId);
        this.phaseBarrier = phaseBarrier;
        this.processorId = processorId;
    }
    
    public void run() {
        try {
            // Phase 1: Validation
            System.out.println("[" + getName() + "] Phase 1: Validating data chunk...");
            Thread.sleep(800);
            System.out.println("[" + getName() + "] Phase 1: Data validated ✓");
            phaseBarrier.await();
            
            // Phase 2: Transformation
            System.out.println("[" + getName() + "] Phase 2: Transforming data...");
            Thread.sleep(1000);
            System.out.println("[" + getName() + "] Phase 2: Data transformed ✓");
            phaseBarrier.await();
            
            // Phase 3: Storage
            System.out.println("[" + getName() + "] Phase 3: Storing data to database...");
            Thread.sleep(1200);
            System.out.println("[" + getName() + "] Phase 3: Data stored ✓");
            phaseBarrier.await();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

public class DataProcessingPipelineDemo {
    public void run() throws InterruptedException {
        System.out.println("=== CyclicBarrier: Multi-Phase Data Processing ===\n");
        
        CyclicBarrier phaseBarrier = new CyclicBarrier(3, () -> {
            System.out.println("[PIPELINE] All processors completed phase. Moving to next phase...\n");
        });
        
        System.out.println("[Pipeline] Starting 3 data processors...\n");
        
        // Launch 3 data processors
        for (int i = 1; i <= 3; i++) {
            new DataProcessor(phaseBarrier, i).start();
        }
        
        // Wait for all processors to complete all phases
        Thread.sleep(10000);
        
        System.out.println("[Pipeline] ✓ All phases completed for all processors!");
    }
    
    public static void main(String[] args) throws InterruptedException {
        new DataProcessingPipelineDemo().run();
    }
}

/* OUTPUT:
=== CyclicBarrier: Multi-Phase Data Processing ===

[Pipeline] Starting 3 data processors...

[Processor-1] Phase 1: Validating data chunk...
[Processor-2] Phase 1: Validating data chunk...
[Processor-3] Phase 1: Validating data chunk...

(0.8 second work)

[Processor-1] Phase 1: Data validated ✓
[Processor-2] Phase 1: Data validated ✓
[Processor-3] Phase 1: Data validated ✓
[PIPELINE] All processors completed phase. Moving to next phase...

[Processor-1] Phase 2: Transforming data...
[Processor-2] Phase 2: Transforming data...
[Processor-3] Phase 2: Transforming data...

(1 second work)

[Processor-1] Phase 2: Data transformed ✓
[Processor-2] Phase 2: Data transformed ✓
[Processor-3] Phase 2: Data transformed ✓
[PIPELINE] All processors completed phase. Moving to next phase...

[Processor-1] Phase 3: Storing data to database...
[Processor-2] Phase 3: Storing data to database...
[Processor-3] Phase 3: Storing data to database...

(1.2 second work)

[Processor-1] Phase 3: Data stored ✓
[Processor-2] Phase 3: Data stored ✓
[Processor-3] Phase 3: Data stored ✓
[PIPELINE] All processors completed phase. Moving to next phase...

[Pipeline] ✓ All phases completed for all processors!
*/
```

---

**Comparison Table:**

| Aspect | CountDownLatch | CyclicBarrier |
|--------|----------------|---------------|
| **Purpose** | One-shot countdown to 0 | Reusable barrier for N threads |
| **Initialization** | Count set at creation | Party size set at creation |
| **Reusability** | One-time use (counts down to 0) | Reusable (resets after barrier) |
| **Typical Use** | Wait for initialization tasks to complete | Synchronize threads at checkpoints |
| **API** | `countDown()`, `await()` | `await()`, automatic reset |
| **Race Scenario** | Perfect for starting race (wait for signal) | Perfect for multi-stage races |

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

### Q12: Even-Odd Printing with 2 Threads (ReentrantLock & Conditions)

**Problem:** 2 threads print 1-10 alternately (1, 2, 3, ... 10) using ReentrantLock and Conditions.

```java
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.AtomicInteger;

public class EvenOddPrintingThreadExample {
  public static void main(String[] args) throws InterruptedException {
    AtomicInteger count = new AtomicInteger(1);
    ReentrantLock sharedLock = new ReentrantLock();

    Condition sharedEvenCondition = sharedLock.newCondition();
    Condition sharedOddCondition = sharedLock.newCondition();

    Thread evenPrinterThread = new EvenPrinterThread(count, sharedLock, sharedEvenCondition, sharedOddCondition);
    Thread oddPrinterThread = new OddPrinterThread(count, sharedLock, sharedEvenCondition, sharedOddCondition);

    evenPrinterThread.start();
    oddPrinterThread.start();

    evenPrinterThread.join();
    oddPrinterThread.join();
  }
}

class EvenPrinterThread extends Thread {
  private AtomicInteger sharedCount;
  private ReentrantLock sharedLock;
  private Condition sharedEvenCondition;
  private Condition sharedOddCondition;

  public EvenPrinterThread() {}

  public EvenPrinterThread(AtomicInteger sharedCount, ReentrantLock sharedLock, Condition sharedEvenCondition, Condition sharedOddCondition) {
    this.sharedCount = sharedCount;
    this.sharedLock = sharedLock;
    this.sharedEvenCondition = sharedEvenCondition;
    this.sharedOddCondition = sharedOddCondition;
  }

  public void run() {
    sharedLock.lock();
    try {
      printEven();
    } catch (InterruptedException iex) {
      Thread.currentThread().interrupt();
    } finally {
      sharedLock.unlock();
    }
  }

  private void printEven() throws InterruptedException{
    for (int i = 0; i < 10; i++) {
      if (this.sharedCount.get() % 2 == 0) {
        System.out.println("Even Printing Thread:" + sharedCount.get());
        sharedCount.incrementAndGet();
        sharedOddCondition.signalAll();
      } else {
        sharedEvenCondition.await();
      }
    }
  }
}

class OddPrinterThread extends Thread {
  private AtomicInteger sharedCount;
  private ReentrantLock sharedLock;
  private Condition sharedEvenCondition;
  private Condition sharedOddCondition;

  public OddPrinterThread() {}

  public OddPrinterThread(AtomicInteger sharedCount, ReentrantLock sharedLock, Condition sharedEvenCondition, Condition sharedOddCondition) {
    this.sharedCount = sharedCount;
    this.sharedLock = sharedLock;
    this.sharedEvenCondition = sharedEvenCondition;
    this.sharedOddCondition = sharedOddCondition;
  }

  public void run() {
    sharedLock.lock();
    try {
      printOdd();
    } catch (InterruptedException iex) {
      Thread.currentThread().interrupt();
    } finally {
      sharedLock.unlock();
    }
  }

  private void printOdd() throws InterruptedException{
    for (int i = 0; i < 10; i++) {
      if (this.sharedCount.get() % 2 != 0) {
        System.out.println("Odd Printing Thread:" + sharedCount.get());
        sharedCount.incrementAndGet();
        sharedEvenCondition.signalAll();
      } else {
        sharedOddCondition.await();
      }
    }
  }
}

/* OUTPUT:
Odd Printing Thread:1
Even Printing Thread:2
Odd Printing Thread:3
Even Printing Thread:4
Odd Printing Thread:5
Even Printing Thread:6
Odd Printing Thread:7
Even Printing Thread:8
Odd Printing Thread:9
Even Printing Thread:10
*/
```

**How It Works:**
- `AtomicInteger sharedCount`: Shared counter, starts at 1 (odd)
- `ReentrantLock sharedLock`: Ensures only one thread in critical section at a time
- `Condition sharedEvenCondition`: Even thread waits on this condition
- `Condition sharedOddCondition`: Odd thread waits on this condition
- `if (count % 2 == 0)`: Even thread checks if it's even, odd thread checks if it's odd
- `await()`: If not your turn, wait (releases lock internally)
- `signalAll()`: After printing, wake up the other thread

**Execution Flow:**
1. OddThread acquires lock, count=1 (odd) → prints 1, increments to 2, signals EvenThread
2. EvenThread acquires lock, count=2 (even) → prints 2, increments to 3, signals OddThread
3. OddThread acquires lock, count=3 (odd) → prints 3, increments to 4, signals EvenThread
4. Continues alternating: 4, 5, 6, 7, 8, 9, 10

**Key Differences from Synchronized:**
- **ReentrantLock** allows same thread to acquire lock multiple times (reentrant)
- **Condition** is more flexible than wait/notify (multiple conditions)
- **AtomicInteger** ensures visibility without volatile
- Better for complex thread coordination scenarios

---

### Q12.5: Volatile vs AtomicInteger - Visibility vs Atomicity

**Key Differences:**

| Feature | `volatile` | `AtomicInteger` |
|---------|-----------|-----------------|
| **What it is** | Keyword | Class |
| **Guarantees** | Visibility only | Atomicity + Visibility |
| **Atomic ops** | ❌ No | ✅ Yes |
| **Compound ops** | ❌ Unsafe | ✅ Safe |
| **Performance** | Faster (no lock) | Slightly slower (CAS) |
| **Use case** | Read-heavy | Read-write mixed |

**Problem with `volatile`:**
```java
volatile int count = 0;

// Thread 1 & 2 both do this:
count++;  // ❌ NOT atomic! Read + Increment + Write (3 steps)

// Race condition:
Thread 1 reads: count = 5
Thread 2 reads: count = 5
Thread 1 increments: count = 6, writes
Thread 2 increments: count = 6, writes
// Result: count = 6 (should be 7!) - LOST UPDATE
```

**Solution with `AtomicInteger`:**
```java
AtomicInteger count = new AtomicInteger(0);

// Thread 1 & 2 both do this:
count.incrementAndGet();  // ✅ Atomic operation

// Safe even with concurrent access:
Thread 1: atomically reads 5, increments to 6, writes
Thread 2: waits, then reads 6, increments to 7, writes
// Result: count = 7 ✅ Correct
```

**When to Use Each:**

**Use `volatile`:**
```java
volatile boolean flag = false;  // Simple boolean flag
volatile int status = 0;        // Read-heavy, rarely written
// No mutations - just read the value
```

**Use `AtomicInteger`:**
```java
AtomicInteger counter = new AtomicInteger(0);
counter.incrementAndGet();      // ✅ Safe mutations
counter.decrementAndGet();
counter.compareAndSet(5, 10);   // Atomic compare-and-set
```

**Use `ReentrantLock` + `Condition`:**
```java
// Complex coordination between threads (like Q6, Q12)
lock.lock();
try {
  if (condition) {
    doSomething();
    otherCondition.signal();
  } else {
    myCondition.await();
  }
} finally {
  lock.unlock();
}
```

**Real Example - Race Condition:**
```java
// ❌ WRONG - Race condition with volatile
volatile int count = 0;
for (int i = 0; i < 1000; i++) {
  new Thread(() -> count++).start();  // Lost updates!
}
// Result: count < 1000 (unpredictable!)

// ✅ CORRECT - Atomic operation
AtomicInteger count = new AtomicInteger(0);
for (int i = 0; i < 1000; i++) {
  new Thread(() -> count.incrementAndGet()).start();  // Safe
}
// Result: count = 1000 (guaranteed)
```

**Bottom Line:**
- **`volatile`** = Visibility only (fast, simple reads)
- **`AtomicInteger`** = Visibility + Atomicity (safe for mutations)
- **`ReentrantLock`** = Full synchronization (complex coordination)

---

### Q13: ExecutorService - Thread Pool Abstraction

**What is ExecutorService?**
An abstraction layer for managing a pool of reusable threads. Instead of creating new threads for each task, you submit tasks to a pool that handles thread management.

**Without ExecutorService (Bad Practice):**
```java
// Creating threads for each task is expensive
for (int i = 0; i < 1000; i++) {
    new Thread(() -> {
        performTask(i);
    }).start(); // 1000 threads created!
}

// Problems:
// - Thread creation is expensive (10s of ms per thread)
// - Memory overhead (each thread ~1MB stack)
// - Context switching overhead (1000 threads competing)
// - GC pressure from creating/destroying threads
```

**With ExecutorService (Best Practice):**
```java
// Reuse a pool of threads
ExecutorService executor = Executors.newFixedThreadPool(10); // 10 threads

for (int i = 0; i < 1000; i++) {
    executor.execute(() -> performTask(i)); // Task queued to pool
}

executor.shutdown();
executor.awaitTermination(1, TimeUnit.MINUTES);

// Benefits:
// - 10 threads handle 1000 tasks
// - Threads reused (no creation overhead)
// - Bounded concurrency (no resource explosion)
// - Work queue buffers tasks
```

**ExecutorService Hierarchy:**
```
Executor (interface)
├── void execute(Runnable command)
│
ExecutorService (extends Executor)
├── <T> Future<T> submit(Callable<T> task)
├── void shutdown()
├── List<Runnable> shutdownNow()
├── boolean awaitTermination(long timeout, TimeUnit unit)
│
AbstractExecutorService
│
ThreadPoolExecutor (most common implementation)
├── newFixedThreadPool()
├── newCachedThreadPool()
├── newSingleThreadExecutor()
```

**Factory Methods vs Constructor:**
```java
// Using Executors factory methods (simple)
ExecutorService fixed = Executors.newFixedThreadPool(10);
ExecutorService cached = Executors.newCachedThreadPool();
ExecutorService single = Executors.newSingleThreadExecutor();

// Direct ThreadPoolExecutor (full control)
ExecutorService customPool = new ThreadPoolExecutor(
    5,                                    // corePoolSize
    15,                                   // maximumPoolSize
    60, TimeUnit.SECONDS,                 // keepAliveTime
    new LinkedBlockingQueue<>(100),       // workQueue
    Executors.defaultThreadFactory(),     // threadFactory
    new ThreadPoolExecutor.AbortPolicy()  // rejectionPolicy
);
```

**Common Executors Patterns:**
```java
// 1. Fixed Thread Pool - Best for known workload
ExecutorService fixed = Executors.newFixedThreadPool(10);

// 2. Cached Thread Pool - Best for short-lived async tasks
ExecutorService cached = Executors.newCachedThreadPool();

// 3. Single Thread Executor - Serialized task execution
ExecutorService single = Executors.newSingleThreadExecutor();

// 4. Scheduled Executor - For recurring tasks
ScheduledExecutorService scheduled = Executors.newScheduledThreadPool(5);
scheduled.schedule(() -> System.out.println("Hello"), 5, TimeUnit.SECONDS);
scheduled.scheduleAtFixedRate(() -> System.out.println("Tick"), 0, 1, TimeUnit.SECONDS);

// 5. Work Stealing Pool (Java 8+) - Fork/Join framework
ExecutorService forkJoin = Executors.newWorkStealingPool();
```

**Using Callable vs Runnable:**
```java
// Runnable - returns void
executor.execute(() -> System.out.println("Task"));

// Callable - returns result + throws checked exceptions
Future<Integer> future = executor.submit(() -> {
    return expensiveCalculation();
});

try {
    Integer result = future.get(5, TimeUnit.SECONDS); // Blocks until complete
    System.out.println("Result: " + result);
} catch (TimeoutException e) {
    System.out.println("Task took too long");
    future.cancel(true); // Cancel task
}
```

---

### Q14: ThreadPoolExecutor - Detailed Configuration

**Thread Pool Parameters:**
```
┌─────────────────────────────────────────────┐
│  ThreadPoolExecutor Configuration           │
├─────────────────────────────────────────────┤
│ Core Pool Size:    5                        │
│ Max Pool Size:     20                       │
│ Keep Alive Time:   60s                      │
│ Work Queue:        LinkedBlockingQueue(100) │
│ Rejection Policy:  AbortPolicy              │
└─────────────────────────────────────────────┘
```

**Thread Lifecycle:**
```
Initial:           0 threads

Submit task 1:     1 thread created (< corePoolSize)
Submit tasks 2-5:  4 more threads created (total 5 = corePoolSize)
Submit task 6:     Added to queue (doesn't create new thread yet)
Submit task 11:    Queue full, create thread 6 (up to maximumPoolSize)
Submit task 26:    All threads busy, queue full
                   → Rejection Policy triggered (reject, queue wait, etc)

Thread Idle 60s:   Thread killed (back to corePoolSize = 5)
```

**Configuration Strategy:**

```java
public class ThreadPoolConfig {
    
    // CPU-bound tasks (computation, algorithms)
    // Pool size = number of CPU cores
    public static ExecutorService cpuBoundPool() {
        int cores = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
            cores,
            cores,
            0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
        );
    }
    
    // I/O-bound tasks (database, API calls, file reads)
    // Pool size = (number of cores) * (1 + wait/compute ratio)
    // Typically 2-4x cores
    public static ExecutorService ioBoundPool() {
        int cores = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
            cores * 2,
            cores * 4,
            60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000) // Bounded queue for backpressure
        );
    }
    
    // Event-driven (web requests, message processing)
    // Pool size = concurrent requests expected
    public static ExecutorService eventDrivenPool() {
        return new ThreadPoolExecutor(
            100,
            500,
            30, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(5000),
            new ThreadPoolExecutor.CallerRunsPolicy() // Block caller if queue full
        );
    }
}
```

**Rejection Policies:**

| Policy | Behavior | Use Case |
|--------|----------|----------|
| **AbortPolicy** | Throws RejectedExecutionException | Fail-fast, critical system |
| **CallerRunsPolicy** | Caller thread executes task | Backpressure, prevent queue explosion |
| **DiscardPolicy** | Silently discard task | Non-critical background tasks |
| **DiscardOldestPolicy** | Discard oldest task, add new | Time-series data, keep fresh |

**Implementation:**
```java
// Custom rejection handler
public class LoggingRejectionHandler implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        logger.warn("Task rejected. Active: {}, Queue: {}, Pool: {}/{}",
            executor.getActiveCount(),
            executor.getQueue().size(),
            executor.getPoolSize(),
            executor.getMaximumPoolSize()
        );
        
        // Backoff and retry
        try {
            executor.getQueue().put((Runnable) r); // Block caller
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

executor = new ThreadPoolExecutor(
    10, 50, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(1000),
    new LoggingRejectionHandler()
);
```

**Work Queue Selection:**

| Queue | Behavior | When to Use |
|-------|----------|------------|
| **LinkedBlockingQueue** | Unbounded | Throughput > memory (risk of OOM) |
| **ArrayBlockingQueue** | Bounded | Memory-constrained, strict limits |
| **SynchronousQueue** | No buffer | Direct handoff (cached pool) |
| **PriorityBlockingQueue** | Priority-based | Task prioritization |

**Real-world Example - Web Server Thread Pool:**
```java
@Configuration
public class ThreadPoolConfig {
    
    @Bean
    public ExecutorService requestExecutor() {
        int coreThreads = Runtime.getRuntime().availableProcessors();
        int maxThreads = coreThreads * 4;
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            coreThreads,
            maxThreads,
            60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10000), // Bounded queue
            new ThreadFactory() {
                private AtomicInteger count = new AtomicInteger();
                
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("request-worker-" + count.incrementAndGet());
                    t.setDaemon(false); // Non-daemon, must wait for completion
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // Block if queue full
        );
        
        // Monitor pool stats
        new Timer().scheduleAtFixedRate(() -> {
            System.out.println(String.format(
                "Pool: %d/%d, Active: %d, Queue: %d, Completed: %d",
                executor.getPoolSize(),
                executor.getMaximumPoolSize(),
                executor.getActiveCount(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount()
            ));
        }, 0, 10, TimeUnit.SECONDS);
        
        return executor;
    }
}
```

---

### Q15: ExecutorService Lifecycle & Shutdown

**Proper Shutdown Pattern:**
```java
ExecutorService executor = Executors.newFixedThreadPool(10);

try {
    // Submit tasks
    for (int i = 0; i < 100; i++) {
        executor.submit(() -> processTask());
    }
} finally {
    // Initiate shutdown (no new tasks accepted)
    executor.shutdown();
    
    // Wait for all tasks to complete
    try {
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            // Timeout - force shutdown
            List<Runnable> remaining = executor.shutdownNow();
            logger.error("Executor did not terminate. Remaining tasks: {}", remaining.size());
            
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.error("Executor still not terminated after forced shutdown");
            }
        }
    } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

**Shutdown vs ShutdownNow:**

| Method | Behavior |
|--------|----------|
| **shutdown()** | No new tasks accepted, existing tasks complete |
| **shutdownNow()** | Stops accepting new tasks, interrupts running tasks, returns pending tasks |

**Better Approach - Try-with-resources (Java 7+):**
```java
// ExecutorService extends AutoCloseable
try (ExecutorService executor = Executors.newFixedThreadPool(10)) {
    for (int i = 0; i < 100; i++) {
        executor.submit(() -> processTask());
    }
    // Automatically calls executor.shutdown() on exit
} catch (Exception e) {
    logger.error("Error", e);
}
```

**Monitoring & Metrics:**
```java
public class ExecutorMetrics {
    private final ThreadPoolExecutor executor;
    
    public ExecutorMetrics(ThreadPoolExecutor executor) {
        this.executor = executor;
    }
    
    public void printStats() {
        System.out.println(String.format(
            "Executor Stats: Core=%d, Max=%d, Current=%d, " +
            "Active=%d, Completed=%d, Queue=%d, Queue Remaining=%d",
            executor.getCorePoolSize(),
            executor.getMaximumPoolSize(),
            executor.getPoolSize(),
            executor.getActiveCount(),
            executor.getCompletedTaskCount(),
            executor.getQueue().size(),
            executor.getQueue().remainingCapacity()
        ));
    }
    
    public double getQueueUtilization() {
        BlockingQueue<Runnable> queue = executor.getQueue();
        return (double) queue.size() / queue.remainingCapacity();
    }
    
    public boolean isUnderPressure() {
        return getQueueUtilization() > 0.8; // > 80% full
    }
}
```

---

### Q16: ForkJoinPool - For Divide-and-Conquer Tasks

**Problem:** Standard thread pools aren't optimal for recursive, divide-and-conquer algorithms.

**Example - Without ForkJoinPool:**
```java
// Merge sort using regular threads - inefficient
public class MergeSortWithThreads {
    private ExecutorService executor = Executors.newFixedThreadPool(10);
    
    public void mergeSort(int[] arr, int left, int right) {
        if (left < right) {
            int mid = (left + right) / 2;
            
            // Create two threads for left and right halves
            Future<?> leftSort = executor.submit(() -> mergeSort(arr, left, mid));
            Future<?> rightSort = executor.submit(() -> mergeSort(arr, mid + 1, right));
            
            leftSort.get();   // Wait for left
            rightSort.get();  // Wait for right
            
            merge(arr, left, mid, right);
        }
    }
}

// Problem: Thread overhead for small sub-problems, not efficient
```

**Solution - ForkJoinPool:**
```java
public class MergeSortForkJoin extends RecursiveAction {
    private static final int THRESHOLD = 1000;
    private int[] arr;
    private int left, right;
    
    public MergeSortForkJoin(int[] arr, int left, int right) {
        this.arr = arr;
        this.left = left;
        this.right = right;
    }
    
    @Override
    protected void compute() {
        if (right - left < THRESHOLD) {
            // Base case: small enough to sort directly
            Arrays.sort(arr, left, right + 1);
        } else {
            // Divide
            int mid = (left + right) / 2;
            
            MergeSortForkJoin leftTask = new MergeSortForkJoin(arr, left, mid);
            MergeSortForkJoin rightTask = new MergeSortForkJoin(arr, mid + 1, right);
            
            // Fork both tasks
            leftTask.fork();
            rightTask.fork();
            
            // Wait for both to complete
            leftTask.join();
            rightTask.join();
            
            // Merge
            merge(arr, left, mid, right);
        }
    }
}

// Usage
int[] arr = new int[1_000_000];
ForkJoinPool.commonPool().invoke(new MergeSortForkJoin(arr, 0, arr.length - 1));
```

**ForkJoinTask vs RecursiveTask/RecursiveAction:**

| Class | Return Value | When to Use |
|-------|--------------|------------|
| **RecursiveAction** | void | Pure side-effect computation |
| **RecursiveTask<T>** | T | Computation with result |

**Work Stealing Benefit:**
```
ForkJoinPool (8 threads):
┌──────────┬──────────┬──────────┬──────────┬──────────┬──────────┬──────────┬──────────┐
│ Thread 1 │ Thread 2 │ Thread 3 │ Thread 4 │ Thread 5 │ Thread 6 │ Thread 7 │ Thread 8 │
├──────────┼──────────┼──────────┼──────────┼──────────┼──────────┼──────────┼──────────┤
│ Task A   │ Task B   │ Task C   │ Task D   │ Task E   │ Task F   │ Task G   │ Task H   │
│ ├─ A1    │ ├─ B1    │ ├─ C1    │ ├─ D1    │ empty    │ empty    │ empty    │ empty    │
│ └─ A2    │ └─ B2    │ └─ C2    │ └─ D2    │          │          │          │          │
│          │          │          │          │ steals   │ steals   │ steals   │ steals   │
│          │          │          │          │ from A2  │ from B2  │ from C2  │ from D2  │
└──────────┴──────────┴──────────┴──────────┴──────────┴──────────┴──────────┴──────────┘

Threads 5-8 don't idle; they steal from overloaded threads' work queues
```

---

## Multi-Threading Concepts Summary (Layman Terms)

**Complete Threading Concepts - Quick Reference**

| Concept | What It Does | Real-World Analogy | When to Use |
|---------|-------------|-------------------|-----------|
| **synchronized** | Lock a room (only 1 person at a time) | Bank teller room - one customer at a time | Simple sharing (1-2 threads) |
| **ReentrantLock** | Smart lock (same person can re-lock) | VIP pass holder can enter same door multiple times | Complex locking patterns |
| **Semaphore** | Permit system (N people allowed) | Restaurant: 10 tables, each seat holds 1 person | Limit concurrent access to N resources |
| **CountDownLatch** | Start gun for race (waits for signal) | Teacher waits for all 30 students to arrive before starting class | Wait for N tasks to complete |
| **volatile** | Megaphone (shout so everyone hears) | Loudspeaker: announcement visible to all immediately | Simple flag shared between threads |
| **CyclicBarrier** | Meeting point (all wait for each other) | 5 friends at movie theater - all wait until everyone arrives | Sync threads at checkpoints (repeating) |
| **AtomicInteger** | Safe counter (no lost updates) | Turnstile: click button, counter +1 (always accurate) | Atomic mutations (increment, decrement) |
| **ExecutorService** | Receptionist (takes tasks, assigns to workers) | Hotel: front desk takes requests, assigns to staff | Execute many tasks with worker threads |
| **ThreadPool** | Team of workers waiting for jobs | Construction crew: 10 workers wait for tasks, work, repeat | Reuse threads instead of creating new |
| **ForkJoinPool** | Divide-and-conquer team (split work, merge) | Swarm: split large task into subtasks, workers tackle, combine | Parallel divide-and-conquer problems |

---

**Quick Decision Guide:**

```
Q: Lock shared resource?
├─ Simple case → synchronized
└─ Complex case → ReentrantLock

Q: Limit to N concurrent?
└─ Semaphore

Q: Wait for N tasks to complete?
└─ CountDownLatch

Q: Threads sync at checkpoint (repeating)?
└─ CyclicBarrier

Q: Execute many tasks in parallel?
├─ Regular tasks → ExecutorService + ThreadPool
└─ Divide-and-conquer → ForkJoinPool

Q: Safe counter mutations?
└─ AtomicInteger

Q: Just visibility (read-heavy)?
└─ volatile
```

---

**One-Liner Cheat Sheet:**

- **synchronized** = "One person in room at a time"
- **ReentrantLock** = "Smart lock, same person can re-lock"
- **Semaphore** = "10 parking spots for anyone"
- **CountDownLatch** = "Teacher waits for all students"
- **volatile** = "Megaphone announcement"
- **CyclicBarrier** = "Friends wait for each other"
- **AtomicInteger** = "Accurate turnstile counter"
- **ExecutorService** = "Receptionist + worker team"
- **ThreadPool** = "Team of workers reused"
- **ForkJoinPool** = "Divide big problem, merge results"

---

## Deep Dive: How `synchronized` Works Internally

### Simple Analogy: Bathroom Lock

```
Thread A: "I want to use synchronized block"
  ↓
Object Lock: "Do you have the key? No? Wait in queue"
  ↓
Thread A: Waits
Thread B: Has the key, inside the block
  ↓
Thread B: Done, releases key
  ↓
Object Lock: "Wake up Thread A, here's the key"
  ↓
Thread A: Gets key, executes the block
  ↓
Thread A: Done, releases key for next thread
```

---

### Object Header: The Lock Storage

Every Java object has a **12-16 byte header** that stores lock information:

```
Java Object in Memory:
┌──────────────────────────┐
│ Mark Word                │ ← Stores lock status + owner thread ID
│ (8 bytes on 64-bit JVM)  │
├──────────────────────────┤
│ Class Pointer            │ ← Points to class definition
├──────────────────────────┤
│ Instance Data            │ ← Actual object fields
└──────────────────────────┘

Mark Word Contents:
- Lock state: Unlocked? Locked by who?
- Thread ID: Which thread owns this lock?
- Lock count: How many times locked (for re-entrancy)?
- Hash code and GC age
```

---

### Three Lock States (HotSpot JVM)

#### State 1: Biased Locking (Fast Path - Most Common)

```
Initial state: Object is unlocked

Thread A enters synchronized:
  ├─ Mark Word stores Thread A's ID
  ├─ Future accesses by Thread A: NO actual locking needed!
  └─ Cost: Almost FREE (one atomic operation)

Thread B arrives:
  ├─ Bias is revoked
  └─ Escalate to next state
```

**Why biased?** The JVM bets the lock will be used by the same thread repeatedly. Usually correct!

---

#### State 2: Lightweight Locking (Spin Lock)

```
Thread A has the lock, Thread B arrives:
  ├─ Thread B starts SPINNING (busy-waiting)
  ├─ "Is lock free yet? Is it free yet? Is it free yet?"
  ├─ Checking thousands of times per second
  ├─ If Thread A releases quickly → Thread B grabs it
  └─ Cost: CPU busy, but NO OS context switch

Example:
  Thread A: [Released lock after 10ms]
  Thread B: [Noticed after 2ms of spinning]
  Result: ⚡ FAST (no sleep/wake overhead)
```

---

#### State 3: Heavyweight Locking (Blocking)

```
If threads contend for too long:
  ├─ JVM stops spinning (wastes CPU)
  ├─ Thread goes to SLEEP (parked by OS)
  ├─ Waits in queue for lock to be released
  ├─ When lock available: OS wakes one thread
  └─ Cost: 🐢 SLOW (context switch overhead)

Queue visualization:
  ┌─────────────────┐
  │ Lock Owner: T1  │ (running)
  └────────┬────────┘
           │
  ┌────────▼──────────┐
  │ Waiting Queue:    │
  │ T2 (sleeping)     │
  │ T3 (sleeping)     │
  │ T4 (spinning)     │
  └───────────────────┘
```

---

### Lock Acquisition Process

```
Thread wants to enter synchronized block:

1. Check Mark Word (lock status)
   ↓
2. Is it unlocked?
   ├─ YES → Mark it as locked, continue ✓
   └─ NO → Go to step 3
   ↓
3. Is it biased to me (same thread)?
   ├─ YES → Increment lock count, continue ✓
   └─ NO → Go to step 4
   ↓
4. Try lightweight lock (spinning)
   ├─ Spin for N iterations (~100-1000)
   ├─ Success? Continue ✓
   └─ Still locked? Go to step 5
   ↓
5. Escalate to heavyweight lock
   ├─ Park thread (OS sleep)
   ├─ Wait in queue
   ├─ OS wakes one thread when lock free
   └─ Continue ✓
```

---

### Lock Release Process

```
Thread leaving synchronized block:

1. Decrement lock count
   ↓
2. Is count == 0? (Fully unlocked?)
   ├─ NO → Still holding it, done
   └─ YES → Go to step 3
   ↓
3. Release Mark Word (unlock)
   ├─ Update Mark Word: "Unlocked"
   └─ Go to step 4
   ↓
4. Are there waiting threads?
   ├─ NO → Done ✓
   └─ YES → Wake ONE thread from queue
      (Not necessarily first! → NOT FIFO)
      
5. Woken thread acquires lock + continues
   Others still waiting
```

---

### Memory Barriers (Visibility Guarantee)

`synchronized` provides **both mutual exclusion AND visibility**:

```java
class Example {
    private int value = 0;
    
    synchronized void write() {
        value = 10;  // Write
    }
    
    synchronized int read() {
        return value;  // Read
    }
}
```

**What happens internally:**

```
Thread A entering synchronized (acquiring lock):
  ├─ LoadLoad barrier
  ├─ LoadStore barrier
  └─ Flush CPU cache → See latest values from main memory

Thread A exiting synchronized (releasing lock):
  ├─ StoreStore barrier
  ├─ LoadStore barrier
  └─ Publish changes → Write back to main memory

Result: Thread B always sees Thread A's writes!
```

---

### Re-entrancy (Same Thread Can Lock Again)

```java
synchronized void methodA() {
    // Lock count = 1
    synchronized(this) {
        // Lock count = 2 (same lock, same thread)
        methodB();
    }
    // Lock count = 1
}

synchronized void methodB() {
    // Lock count = 2 (already holds it, just increment)
    // ... work ...
    // Unlock: count = 1
}
// Unlock: count = 0 (fully released)
```

**Why it works:**
- Mark Word stores **lock count**, not just boolean
- Same thread checking own ID can increment count
- Each exit decrements count
- Only when count reaches 0 is lock actually released

---

### Performance: Lock States Comparison

| Scenario | Lock Type | Cost | Example |
|----------|-----------|------|---------|
| **Single thread** | Biased | ~0 cycles 🚀 | Thread T reuses lock many times |
| **2 threads, low contention** | Lightweight (spin) | 10-100 cycles ⚡ | Thread A releases, Thread B grabs quickly |
| **Many threads, high contention** | Heavyweight (queue) | 10,000+ cycles 🐢 | Thread goes to sleep, OS wakes later |
| **Uncontended object** | Biased | Always free | Lock never escalates |

---

### synchronized vs ReentrantLock

| Feature | `synchronized` | `ReentrantLock` |
|---------|---|---|
| **Fair?** | NO (random thread wins) | YES (if fairness=true) |
| **Effort** | Auto-optimized by JVM | Manual lock/unlock |
| **Lock States** | 3 (biased→lightweight→heavy) | 1 (queue-based) |
| **Speed (low contention)** | Very fast ⚡ | Slower (always queue) |
| **Speed (high contention)** | Slower | Comparable |
| **Try-lock with timeout** | ❌ Not supported | ✓ Supported |
| **Multiple conditions** | wait/notify (clunky) | Condition (clean) |

---

### When Lock Escalates (Biased → Lightweight → Heavyweight)

```
Scenario 1: Single-threaded code
Thread T always uses synchronized block alone
  └─ Stays BIASED forever
     (No contention, no escalation)

Scenario 2: Two threads, low contention
Thread A has lock, Thread B arrives
  ├─ Revoke bias → Lightweight lock
  ├─ Thread B spins (busy-wait)
  ├─ Thread A releases quickly
  ├─ Thread B grabs it
  └─ Stays LIGHTWEIGHT (no need to escalate)

Scenario 3: High contention (many threads)
Many threads wanting same lock
  ├─ Lightweight escalates after N spins
  ├─ Threads move to queue (sleep)
  ├─ OS scheduler wakes one at a time
  └─ Becomes HEAVYWEIGHT
```

---

### Thread Notification (Not Always Fair)

```
Lock is released!

Waiting threads:
  Thread B: Sleeping (in queue)
  Thread C: Sleeping (in queue)
  Thread D: Spinning (still trying)

Who gets it?
  └─ Thread D (spinning, awake) often wins!
     OR one random thread from queue wakes up
  
NOT FIFO! 🎲
```

**Why not fair?**
- Spinning threads are "awake", grab lock before OS can wake sleepers
- Maintaining FIFO order = expensive bookkeeping
- `synchronized` prioritizes SPEED, not fairness
- For fairness, use `ReentrantLock(true)`

---

### Summary: Internal Execution Flow

```
Code:
    synchronized(object) {
        doWork();
    }

Internal Execution:

1. ACQUIRE PHASE:
   ├─ Check object's Mark Word
   ├─ If unlocked → Mark as locked
   ├─ If biased to me → Increment count
   ├─ If locked by other → Spin/Wait
   └─ Continue when lock acquired

2. EXECUTE PHASE:
   ├─ Memory barrier: Load latest values
   ├─ Run doWork()
   └─ Memory barrier: Publish changes

3. RELEASE PHASE:
   ├─ Decrement lock count
   ├─ If count > 0 → Still locked, done
   ├─ If count == 0 → Fully release
   └─ Wake one waiting thread

4. NEXT THREAD:
   └─ Woken thread acquires lock + repeats
```

---

## Other Topics

### JWT Token

#### What is JWT (JSON Web Token)?

**Simple Explanation:**
JWT is a secure token (a piece of data) that proves who you are. Think of it like a passport or ID card:
- **Passport**: Shows your identity, nationality, expiration date → Can't be forged (has a security seal)
- **JWT**: Shows your user ID, username, roles, expiration date → Can't be forged (has a cryptographic signature)

**Real-World Analogy:**

```
Hotel Room Key Card Scenario:

WITHOUT JWT (Traditional Session):
1. Guest checks in at front desk
2. Front desk asks: "Who are you?" 
3. Guest provides ID
4. Front desk creates a room key and stores guest info in a database
5. Front desk gives guest the key card
6. Guest swipes key card at room
7. Hotel system must query the database: "Is this key card valid?"
8. Database lookup confirms guest is allowed
9. Door opens

Problem: Every door swipe requires a database query. If hotel has 1000 guests, 1000 database lookups!

WITH JWT:
1. Guest checks in at front desk
2. Front desk asks: "Who are you?"
3. Guest provides ID
4. Front desk creates a JWT: "This is John Smith, room 501, checkout date 12/25, SIGNED by front desk"
5. Front desk gives guest the JWT
6. Guest swipes JWT at room
7. Hotel system reads JWT: "Says John Smith, room 501, checkout date 12/25, signed by front desk"
8. System verifies signature: "Yes, this is really signed by front desk, not forged"
9. Door opens

Benefit: No database lookup needed! JWT contains all info + cryptographic proof it's real.
```

**When is JWT Used?**

```
✅ Authentication (proving who you are):
   - Login: User provides username/password
   - Server returns JWT token
   - Client stores JWT (in memory, localStorage, cookie)
   - Every API request includes JWT in Authorization header
   - Server validates JWT signature (no DB query needed)

✅ Authorization (proving what you're allowed to do):
   - JWT contains "roles": ["USER", "ADMIN"]
   - API reads roles from JWT
   - Blocks if user doesn't have required role

✅ Stateless Sessions (no server-side session storage):
   - Traditional: User logs in → Server creates session → Stores in DB
   - JWT way: User logs in → Server creates JWT → No storage needed!
   - Multiple servers can validate the same JWT (no sync needed)

✅ Mobile Apps & Microservices:
   - Mobile app gets JWT → Sends with every API call
   - Each microservice can validate JWT independently
   - No need for shared session storage

✅ Third-party APIs (OAuth 2.0):
   - You sign in with Google/GitHub
   - They return a JWT token
   - You use JWT to access their APIs
```

**JWT vs Traditional Session-Based Auth:**

| Aspect | Traditional Session | JWT |
|--------|-------------------|-----|
| **Storage** | Session stored in database | No server storage needed |
| **Scalability** | Requires sticky sessions or session replication | Scales horizontally easily |
| **API Calls** | Each request needs DB lookup | No DB lookup (verify signature only) |
| **Mobile Apps** | Requires cookies (complex) | Simple: send in Authorization header |
| **Microservices** | Needs centralized session store | Each service validates independently |
| **CORS** | Requires cookie management | Easier with Authorization headers |
| **Logout** | Delete session from DB (instant) | Token stays valid until expiration (harder to revoke) |

**How JWT Works (Step-by-Step):**

```
STEP 1: User Logs In
┌─────────────────────────────────────────┐
│ Client (Browser/Mobile App)             │
│ Sends: username=john, password=secret   │
└────────────────────┬────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────┐
│ Server (Backend)                        │
│ 1. Verify username & password           │
│ 2. Create JWT payload:                  │
│    {                                    │
│      "userId": "12345",                 │
│      "username": "john",                │
│      "roles": ["USER"],                 │
│      "iat": 1234567890,  (issued at)    │
│      "exp": 1234571490   (expires in)   │
│    }                                    │
│ 3. Sign with secret: HMAC-SHA256        │
│ 4. Return JWT to client                 │
└────────────────────┬────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────┐
│ Client: Stores JWT in memory/localStorage
│ JWT = "eyJhbGciOiJIUzI1NiJ9.eyJ..."     │
└─────────────────────────────────────────┘

STEP 2: Client Makes API Request
┌─────────────────────────────────────────┐
│ Client Sends:                           │
│ GET /api/profile                        │
│ Authorization: Bearer eyJhbGc...        │
└────────────────────┬────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────┐
│ Server:                                 │
│ 1. Extract JWT from Authorization header│
│ 2. Verify signature using secret        │
│    (Proves JWT wasn't tampered with)    │
│ 3. Check expiration time                │
│ 4. Read user info from payload          │
│ 5. Process request as authenticated user│
│ 6. Return response                      │
└────────────────────┬────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────┐
│ Client: Receives response                │
│ {                                       │
│   "name": "John Doe",                   │
│   "email": "john@example.com"           │
│ }                                       │
└─────────────────────────────────────────┘
```

**Advantages of JWT:**

1. **Stateless**: No session storage on server
2. **Scalable**: Easy to scale horizontally (no server affinity needed)
3. **Mobile-Friendly**: Works great with mobile apps
4. **Microservices**: Each service can validate independently
5. **Performance**: No database lookup per request
6. **Self-contained**: All info in the token itself

**Disadvantages of JWT:**

1. **Logout Problem**: Token stays valid until expiration (can't instantly revoke)
   - Solution: Maintain a token blacklist in database
2. **Token Size**: Larger than session ID (increases request size)
3. **Refresh Needed**: Must refresh before expiration (need refresh tokens)
4. **Secret Management**: Secret key must be secure (if leaked, all tokens compromised)
5. **No Server Control**: Once issued, server can't modify token content

**JWT Security Risks:**

```
❌ Risk 1: Token Theft
   - Attacker steals JWT from localStorage via XSS
   - Can use token to impersonate user
   - Solution: Use HttpOnly cookies, not localStorage

❌ Risk 2: Secret Key Exposed
   - If secret key is compromised, attacker can create fake JWTs
   - Solution: Store secret in secure vault (HashiCorp Vault, AWS Secrets Manager)

❌ Risk 3: Token Expiration Too Long
   - If token valid for 1 year, compromised token valid for 1 year
   - Solution: Use short expiration (15 min) + refresh tokens (7 days)

❌ Risk 4: "none" Algorithm Attack
   - Attacker changes algorithm to "none" to bypass verification
   - Solution: Explicitly check algorithm in server code

❌ Risk 5: Weak Signature
   - Using HS256 with weak secret allows brute force
   - Solution: Use strong secrets, consider RS256 (asymmetric)
```

---

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

# MCQ Quiz - Advanced Level

## Section 1: Code Output Prediction

### Q1: What will be the output? (Multiple answers possible)

```java
volatile int count = 0;
Thread t1 = new Thread(() -> {
  for (int i = 0; i < 1000; i++) {
    count++;
  }
});
Thread t2 = new Thread(() -> {
  for (int i = 0; i < 1000; i++) {
    count++;
  }
});
t1.start();
t2.start();
t1.join();
t2.join();
System.out.println(count);
```

**A)** Output is always 2000
**B)** Output is always less than 2000
**C)** Output could be 2000 or less than 2000 (unpredictable)
**D)** Output will definitely be 1000 or less

---

### Q2: What will be the output?

```java
CountDownLatch latch = new CountDownLatch(2);

Thread t1 = new Thread(() -> {
  try {
    System.out.println("T1: Starting");
    Thread.sleep(1000);
    System.out.println("T1: Done");
    latch.countDown();
    System.out.println("T1: After countDown");
  } catch (InterruptedException e) {
    e.printStackTrace();
  }
});

Thread t2 = new Thread(() -> {
  try {
    System.out.println("T2: Starting");
    Thread.sleep(500);
    System.out.println("T2: Done");
    latch.countDown();
  } catch (InterruptedException e) {
    e.printStackTrace();
  }
});

t1.start();
t2.start();

try {
  latch.await();
  System.out.println("Main: All done");
} catch (InterruptedException e) {
  e.printStackTrace();
}
```

**A)** "Main: All done" prints before "T1: After countDown"
**B)** "Main: All done" prints after "T1: After countDown"
**C)** "T2: Done" prints before "T1: Done"
**D)** "Main: All done" will definitely print last

---

### Q3: What will be the output? (Multiple answers possible)

```java
ReentrantLock lock = new ReentrantLock();
Condition condition = lock.newCondition();
int value = 0;

Thread t1 = new Thread(() -> {
  lock.lock();
  try {
    System.out.println("T1: Acquired lock");
    value = 10;
    condition.signalAll();
    System.out.println("T1: After signal");
  } finally {
    lock.unlock();
  }
});

Thread t2 = new Thread(() -> {
  lock.lock();
  try {
    System.out.println("T2: Acquired lock");
    if (value == 0) {
      System.out.println("T2: Waiting");
      condition.await();
      System.out.println("T2: After await");
    }
    System.out.println("T2: Value = " + value);
  } catch (InterruptedException e) {
    e.printStackTrace();
  } finally {
    lock.unlock();
  }
});

t2.start();
t1.start();
t2.join();
t1.join();
```

**A)** "T2: Value = 10" will print
**B)** "T2: Value = 0" will print
**C)** "T2: After await" will print
**D)** "T2: Waiting" may not print at all
**E)** Guaranteed that T2 acquires lock before T1

---

### Q4: What will be the output?

```java
CyclicBarrier barrier = new CyclicBarrier(2);

Thread t1 = new Thread(() -> {
  try {
    System.out.println("T1: Waiting at barrier");
    barrier.await();
    System.out.println("T1: After barrier");
  } catch (Exception e) {
    e.printStackTrace();
  }
});

Thread t2 = new Thread(() -> {
  try {
    System.out.println("T2: Waiting at barrier");
    barrier.await();
    System.out.println("T2: After barrier");
  } catch (Exception e) {
    e.printStackTrace();
  }
});

t1.start();
Thread.sleep(100);  // Ensure T1 reaches barrier first
t2.start();
t1.join();
t2.join();
```

**A)** "T1: After barrier" prints before "T2: After barrier"
**B)** "T1: After barrier" prints after "T2: After barrier"
**C)** Both "T1: After barrier" and "T2: After barrier" can print in any order
**D)** Guaranteed that both print (barrier not broken)

---

## Section 2: Theory & Concept Based

### Q5: Which statements are TRUE about AtomicInteger vs volatile int?

**A)** volatile int only guarantees visibility, not atomicity
**B)** AtomicInteger guarantees both visibility and atomicity
**C)** count++ with AtomicInteger is always safe; with volatile int it's not
**D)** volatile int is faster than AtomicInteger
**E)** Both allow multiple threads to safely increment without synchronization

---

### Q6: Which of the following about Thread.join() vs CountDownLatch are TRUE? (Multiple correct)

**A)** Thread.join() waits for thread to DIE; CountDownLatch waits for counter to reach 0
**B)** Thread.join() works with ExecutorService; CountDownLatch doesn't
**C)** CountDownLatch thread can keep running after countDown(); join() thread must terminate
**D)** join() requires thread reference; CountDownLatch doesn't
**E)** CountDownLatch can't be reused; join() can

---

### Q7: Which is TRUE about Semaphore(N)?

**A)** Semaphore(1) acts like a binary lock
**B)** Semaphore(1) is identical to ReentrantLock(1)
**C)** acquire() on Semaphore is atomic
**D)** release() without acquire() increases permits
**E)** Multiple threads can acquire() same permit simultaneously

---

### Q8: Which statements about ConcurrentHashMap vs SynchronizedMap are TRUE? (Multiple)

**A)** ConcurrentHashMap uses bucket-level locking; SynchronizedMap locks entire map
**B)** ConcurrentHashMap is always faster than SynchronizedMap
**C)** ConcurrentHashMap with 100 threads is slower than SynchronizedMap
**D)** get() operations on different keys in ConcurrentHashMap can run in parallel
**E)** SynchronizedMap can have multiple threads doing get() simultaneously on different keys

---

### Q9: Which statements about ReentrantLock are TRUE?

**A)** Same thread can call lock() multiple times
**B)** Each lock() must have corresponding unlock()
**C)** Forgetting unlock() causes deadlock
**D)** ReentrantLock has fairness parameter; synchronized doesn't
**E)** ReentrantLock is always faster than synchronized

---

### Q10: Which about Condition.await() is TRUE? (Multiple)

**A)** await() releases the lock
**B)** await() re-acquires lock after being signaled
**C)** await() can throw InterruptedException
**D)** signalAll() wakes up all waiting threads
**E)** signalAll() releases the lock

---

## Section 3: Choose the Best Concurrency Solution

### Q11: You need to download 100 files in parallel, then process them. Choose BEST solution:

**A)** Create 100 new threads
**B)** Use ExecutorService.newFixedThreadPool(10)
**C)** Use CachedThreadPool
**D)** Use CountDownLatch(100)
**E)** B and D together

---

### Q12: 3 phases: Validate → Transform → Store. All threads must sync at each phase. Choose BEST:

**A)** CountDownLatch for each phase
**B)** CyclicBarrier
**C)** ReentrantLock + wait/notify
**D)** Semaphore(1)
**E)** B is clearly better than A

---

### Q13: You want exactly 50 concurrent API calls, rest queue up. Choose BEST:

**A)** ExecutorService.newFixedThreadPool(50)
**B)** Semaphore(50)
**C)** Both A and B work
**D)** ReentrantLock(50) // Wrong syntax but conceptually?
**E)** A is better because it handles queueing automatically

---

### Q14: Main thread needs to wait for 5 specific threads to complete. Choose BEST:

**A)** Join all 5 threads
**B)** CountDownLatch(5)
**C)** CyclicBarrier(6) // including main
**D)** A if threads might keep running; B if they terminate
**E)** A if thread references available; B if not

---

### Q15: Processing millions of records in parallel (divide-and-conquer). Choose BEST:

**A)** ExecutorService.newFixedThreadPool(numCPUs)
**B)** ForkJoinPool
**C)** CachedThreadPool
**D)** B is better than A for this use case
**E)** A and B have similar performance

---

### Q16: Simple counter shared by 2 threads (just increment). Choose BEST:

**A)** volatile int
**B)** synchronized counter
**C)** AtomicInteger
**D)** ReentrantLock
**E)** C is always best for any counter

---

### Q17: You need reentrant locking (same thread can re-acquire). Choose BEST:

**A)** synchronized
**B)** ReentrantLock
**C)** Semaphore
**D)** A doesn't support it; B does
**E)** Actually, A supports it; both work

---

## Section 4: Real-World Scenarios

### Q18: Bank ATM System - 100 concurrent users, max 5 simultaneous withdrawals. Choose:

**A)** Semaphore(5) for withdrawal limit
**B)** ExecutorService.newFixedThreadPool(5)
**C)** Both A and B could work
**D)** AtomicInteger for balance
**E)** ReentrantLock for account access

---

### Q19: Microservice startup - wait for Auth, DB, Cache services. Choose:

**A)** Create 3 threads, join() all
**B)** CountDownLatch(3)
**C)** CyclicBarrier(3)
**D)** A and B work; B is cleaner
**E)** C is better because reusable

---

### Q20: Chat app - 5000 concurrent users. Choose:

**A)** ExecutorService.newFixedThreadPool(5000)
**B)** ExecutorService.newFixedThreadPool(100)
**C)** CachedThreadPool
**D)** B is best (users >> pool size)
**E)** A guarantees all users handled

---

---

# ANSWER KEY WITH DETAILED EXPLANATIONS

---

## Section 1 Answers: Code Output Prediction

### Q1: volatile int count (race condition)

**CORRECT ANSWERS: B, C**

**Explanation:**

✅ **B is CORRECT:** Output is always less than 2000
- `volatile` only guarantees VISIBILITY, not ATOMICITY
- `count++` is actually 3 operations: read, increment, write
- Race condition: both threads might read same value, increment, write same result
- Example: Both read 500, both increment to 501, both write 501 → lost one increment

✅ **C is CORRECT:** Output could be 2000 or less than 2000
- Sometimes threads don't overlap (lucky timing) → 2000
- Sometimes they overlap (unlucky) → less than 2000
- Unpredictable depending on thread scheduling

❌ **A is WRONG:** Output is NOT always 2000
- Race condition makes it unpredictable

❌ **D is WRONG:** Could be 2000 if lucky with timing

---

### Q2: CountDownLatch order

**CORRECT ANSWERS: A, C, D**

**Explanation:**

✅ **A is CORRECT:** "Main: All done" prints before "T1: After countDown"
- T2 completes first (sleeps 500ms)
- T2 calls countDown() → latch count: 2→1
- T1 completes later (sleeps 1000ms)
- T1 calls countDown() → latch count: 1→0
- Main's await() returns when count reaches 0 (after T1's countDown)
- T1 prints "After countDown" AFTER Main prints (because main's await() already returned)

✅ **C is CORRECT:** "T2: Done" prints before "T1: Done"
- T2 sleeps 500ms
- T1 sleeps 1000ms
- T2 finishes first

✅ **D is CORRECT:** "Main: All done" will definitely print last
- latch.await() blocks until both countDown() calls
- So main prints after both T1 and T2 are done

❌ **B is WRONG:** "Main: All done" might print BEFORE "T1: After countDown"
- Order: T2 done → T1 done → T1 countDown → Main wakes up (happens before T1 prints "After countDown")

---

### Q3: ReentrantLock + Condition race

**CORRECT ANSWERS: A, C, D, E**

**Explanation:**

✅ **A is CORRECT:** "T2: Value = 10" will print
- If T2 enters await(), it releases lock
- T1 acquires lock, sets value=10, signals, unlocks
- T2 wakes, re-acquires lock, value is 10

✅ **C is CORRECT:** "T2: After await" will print
- If T2 calls await(), signal() wakes it
- T2 prints "After await"

✅ **D is CORRECT:** "T2: Waiting" may not print at all
- Race condition: T1 might acquire lock BEFORE T2
- T1 sets value=10, signals (no one waiting yet)
- T2 checks if (value == 0) → false! (value is already 10)
- T2 never calls await(), never prints "Waiting"

✅ **E is CORRECT:** T2 might not acquire lock before T1
- Both start, but T1 might get lock first

❌ **B is WRONG:** Value won't be 0 if T1 runs first

---

### Q4: CyclicBarrier synchronization

**CORRECT ANSWERS: C, D**

**Explanation:**

✅ **C is CORRECT:** Both can print in any order
- CyclicBarrier makes all threads WAIT for each other
- When all arrive, ALL are released simultaneously
- But which one prints first depends on thread scheduling

✅ **D is CORRECT:** Guaranteed that both print (barrier not broken)
- Both threads call await()
- Both are waiting at barrier
- When second thread arrives, both are released
- No exception, both continue

❌ **A is WRONG:** Not guaranteed that T1 prints first
- Thread scheduling is unpredictable

❌ **B is WRONG:** Not guaranteed that T2 prints first
- Both can print in any order

---

## Section 2 Answers: Theory & Concept

### Q5: AtomicInteger vs volatile int

**CORRECT ANSWERS: A, B, C, D**

**Explanation:**

✅ **A is CORRECT:** volatile only guarantees visibility
- Changes immediately visible to all threads
- But count++ is not atomic (3 steps, can be interrupted)

✅ **B is CORRECT:** AtomicInteger guarantees both
- Uses CAS (Compare-And-Swap) internally
- Atomic + visible

✅ **C is CORRECT:** count++ safe with AtomicInteger, not with volatile
- Proven by race conditions in Q1

✅ **D is CORRECT:** volatile is faster than AtomicInteger
- volatile: just memory barrier (cheap)
- AtomicInteger: CAS operation (more expensive)

❌ **E is WRONG:** With volatile int, ++ is NOT always safe
- Compound operation, not atomic

---

### Q6: Thread.join() vs CountDownLatch

**CORRECT ANSWERS: A, C, D**

**Explanation:**

✅ **A is CORRECT:** join() waits for thread to die; CountDownLatch waits for counter
- Fundamental difference

✅ **C is CORRECT:** CountDownLatch thread can keep running after countDown()
- join() MUST wait for thread to terminate
- CountDownLatch only signals "work done"

✅ **D is CORRECT:** join() needs thread reference; CountDownLatch doesn't
- executor.submit() with CountDownLatch: no thread refs needed
- join() array: must keep references

❌ **B is WRONG:** join() doesn't work directly with ExecutorService
- executor returns Future, not Thread

❌ **E is WRONG:** CountDownLatch IS reusable via CyclicBarrier
- CountDownLatch itself is one-time use
- But philosophy similar to CyclicBarrier for reuse

---

### Q7: Semaphore(N) properties

**CORRECT ANSWERS: A, C, D**

**Explanation:**

✅ **A is CORRECT:** Semaphore(1) acts like binary lock
- 1 permit = lock/unlock behavior
- But not exactly identical to Mutex (semaphore is simpler)

✅ **C is CORRECT:** acquire() on Semaphore IS atomic
- Implemented with internal locking
- Atomically decrements permit count

✅ **D is CORRECT:** release() without acquire() increases permits
- No pairing required
- Semaphore just manages permits

❌ **B is WRONG:** Semaphore(1) NOT identical to ReentrantLock(1)
- ReentrantLock supports re-locking by same thread
- Semaphore(1) is binary (0 or 1)

❌ **E is WRONG:** Only ONE thread can acquire same permit
- If one acquires, others wait

---

### Q8: ConcurrentHashMap vs SynchronizedMap

**CORRECT ANSWERS: A, D**

**Explanation:**

✅ **A is CORRECT:** Bucket vs entire-map locking
- Core difference explained in Q5

✅ **D is CORRECT:** Different keys can run in parallel on ConcurrentHashMap
- Different buckets = different locks
- Proven by Q5 results (50x faster)

❌ **B is WRONG:** ConcurrentHashMap is NOT always faster
- With 1-2 threads, synchronized might be comparable
- With high concurrency, CHM wins

❌ **C is WRONG:** ConcurrentHashMap is still faster at 100 threads
- Bucket parallelism wins

❌ **E is WRONG:** SynchronizedMap locks ENTIRE map
- No parallel gets on different keys

---

### Q9: ReentrantLock properties

**CORRECT ANSWERS: A, B, C, D**

**Explanation:**

✅ **A is CORRECT:** Same thread can call lock() multiple times
- "Reentrant" means exactly this
- Internal count: lock #1=2, lock #2=3, unlock #1=2, unlock #2=1

✅ **B is CORRECT:** Each lock() needs unlock()
- Lock count must reach 0

✅ **C is CORRECT:** Forgetting unlock() causes deadlock
- Lock never released
- Other threads waiting forever

✅ **D is CORRECT:** ReentrantLock has fairness parameter
- `new ReentrantLock(true)` = fair (FIFO)
- `new ReentrantLock(false)` = unfair (default)
- synchronized: no fairness option

❌ **E is WRONG:** ReentrantLock is NOT always faster
- synchronized has JVM optimizations (biased locking)
- Both have tradeoffs

---

### Q10: Condition.await() behavior

**CORRECT ANSWERS: A, B, C, D**

**Explanation:**

✅ **A is CORRECT:** await() releases the lock
- Thread must be holding lock to call await()
- await() releases it (other threads can enter)

✅ **B is CORRECT:** await() re-acquires after signal()
- signal() wakes thread
- Thread wakes with lock re-acquired
- Can continue using shared resource safely

✅ **C is CORRECT:** await() throws InterruptedException
- If thread interrupted while waiting

✅ **D is CORRECT:** signalAll() wakes all waiting threads
- Unlike notify() which wakes one

❌ **E is WRONG:** signalAll() does NOT release the lock
- Lock released when calling thread exits try block
- Waiting threads don't get lock until signaler exits

---

## Section 3 Answers: Choose Best Solution

### Q11: Download 100 files, then process

**CORRECT ANSWERS: B, E**

**Explanation:**

✅ **B is CORRECT:** ExecutorService.newFixedThreadPool(10)
- Thread pool reuses threads (not creating 100)
- Queue holds 90 pending files
- As threads complete, pick next file

✅ **E is CORRECT:** B and D together work best
- Executor handles parallel downloads
- CountDownLatch(100) ensures all complete before processing

❌ **A is WRONG:** Creating 100 threads = memory waste, context switching overhead
- Thread per task is anti-pattern

❌ **C is WRONG:** CachedThreadPool creates up to 100 threads
- Better than A, but creates threads (expensive)
- Fixed pool is better for known workload

❌ **D alone is WRONG:** CountDownLatch doesn't handle threading
- Need executor + CountDownLatch

---

### Q12: 3 phases with sync

**CORRECT ANSWERS: B, E**

**Explanation:**

✅ **B is CORRECT:** CyclicBarrier (all sync at checkpoint, repeating)
- Phase 1: all arrive at barrier1, continue together
- Phase 2: all arrive at barrier2, continue together
- Phase 3: all arrive at barrier3, done
- Perfect for multi-phase

✅ **E is CORRECT:** B is clearly better than A
- CountDownLatch can work (3 latches), but verbose
- CyclicBarrier designed exactly for this

❌ **A is WRONG:** Works but clunky (need 3 latches)
- More code, less clear intent

❌ **C is WRONG:** Wait/notify messy for multi-phase
- CyclicBarrier cleaner

❌ **D is WRONG:** Semaphore doesn't handle phases
- Just limits concurrent, doesn't sync

---

### Q13: 50 concurrent API calls, rest queue

**CORRECT ANSWERS: A, C, E**

**Explanation:**

✅ **A is CORRECT:** ExecutorService.newFixedThreadPool(50)
- Exactly 50 threads
- 51st request queues

✅ **C is CORRECT:** A and B work
- Both limit to 50 concurrent

✅ **E is CORRECT:** A is better because auto-queueing
- Executor handles queue for you
- Semaphore requires manual submit/queue

❌ **B alone is WRONG:** Semaphore(50) limits, but doesn't execute
- Need to combine with executor

❌ **D is WRONG:** Syntax error, doesn't make sense
- ReentrantLock doesn't take 50

---

### Q14: Wait for 5 specific threads

**CORRECT ANSWERS: A, B, E**

**Explanation:**

✅ **A is CORRECT:** Join all 5 threads
- Simple, clear, standard pattern

✅ **B is CORRECT:** CountDownLatch(5)
- Also works, threads signal when done

✅ **E is CORRECT:** A if refs available; B if not
- join() requires thread references
- CountDownLatch doesn't

❌ **C is WRONG:** CyclicBarrier means threads wait for EACH OTHER
- Here, main waits for threads (different pattern)

❌ **D is WRONG:** Both work whether threads keep running
- join() waits for termination
- CountDownLatch works even if running

---

### Q15: Millions of records (divide-and-conquer)

**CORRECT ANSWERS: B, D**

**Explanation:**

✅ **B is CORRECT:** ForkJoinPool
- Designed for divide-and-conquer
- Work stealing: idle threads steal from busy threads
- Perfect for recursive splitting

✅ **D is CORRECT:** B is better than A
- ForkJoinPool optimized for recursive tasks
- ExecutorService more general-purpose

❌ **A is WRONG:** Works but not optimized
- No work stealing
- No automatic recursive splitting

❌ **C is WRONG:** CachedThreadPool creates unlimited threads
- Could exhaust memory with millions of tasks

❌ **E is WRONG:** Performance NOT similar
- ForkJoinPool much better for divide-and-conquer

---

### Q16: Simple counter (2 threads, just increment)

**CORRECT ANSWERS: C, E**

**Explanation:**

✅ **C is CORRECT:** AtomicInteger
- Safe, fast, idiomatic

✅ **E is CORRECT:** C is always best for counter
- Simplest, most efficient

❌ **A is WRONG:** volatile doesn't prevent race condition
- count++ not atomic

❌ **B is WRONG:** synchronized works but overkill
- Slower than atomic
- More verbose

❌ **D is WRONG:** ReentrantLock overkill
- Way more overhead than needed

---

### Q17: Reentrant locking (same thread re-acquire)

**CORRECT ANSWERS: B, D**

**Explanation:**

✅ **B is CORRECT:** ReentrantLock
- Designed for reentrant (name says it!)

✅ **D is CORRECT:** A doesn't support it (fully); B does
- Actually, synchronized DOES support reentrancy
- But ReentrantLock more explicit/configurable

❌ **A is WRONG:** synchronized actually IS reentrant!
- Same thread can re-enter synchronized block
- But ReentrantLock more flexible

❌ **C is WRONG:** Semaphore doesn't support reentrancy
- No per-thread tracking

❌ **E is WRONG:** While both technically support reentrancy, ReentrantLock is better
- synchronized: implicit, always reentrant
- ReentrantLock: explicit, configurable

---

## Section 4 Answers: Real-World Scenarios

### Q18: Bank ATM System

**CORRECT ANSWERS: A, C, D, E**

**Explanation:**

✅ **A is CORRECT:** Semaphore(5) for withdrawal limit
- Max 5 simultaneous withdrawals

✅ **C is CORRECT:** A and B could work
- Semaphore limits concurrency
- ExecutorService can do similar

✅ **D is CORRECT:** AtomicInteger for balance
- Thread-safe counter

✅ **E is CORRECT:** ReentrantLock for account access
- Account details need locking during update

❌ **B alone is WRONG:** Doesn't provide fine-grained limit
- Pool size limits total threads, not ATM withdrawals specifically

---

### Q19: Microservice startup

**CORRECT ANSWERS: B, D**

**Explanation:**

✅ **B is CORRECT:** CountDownLatch(3)
- Main waits for 3 services
- Each service signals when ready
- Cleaner than join()

✅ **D is CORRECT:** A and B work; B is cleaner
- join() requires thread references
- CountDownLatch more semantic

❌ **A is WRONG:** Works but requires keeping thread refs
- More boilerplate

❌ **C is WRONG:** CyclicBarrier means services wait for main
- Here, main waits for services

❌ **E is WRONG:** CyclicBarrier reusable but not needed here
- Services don't need to sync with each other
- Only main waits

---

### Q20: Chat app with 5000 concurrent users

**CORRECT ANSWERS: B, D**

**Explanation:**

✅ **B is CORRECT:** ExecutorService.newFixedThreadPool(100)
- 5000 users >> 100 threads
- Threads reused efficiently
- Each handles multiple users

✅ **D is CORRECT:** B is best
- Prevents thread explosion
- Bounded resources
- Scalable

❌ **A is WRONG:** Creating 5000 threads = system crash
- Memory: 5GB+ (5000 × 1MB)
- Context switching: overhead

❌ **C is WRONG:** CachedThreadPool might create too many
- All 5000 might be active
- Better to limit to 100

❌ **E is WRONG:** A doesn't "guarantee" users handled
- System crashes with 5000 threads

---

## Summary Statistics

- **Total Questions:** 20
- **Code Output (Q1-4):** 4 questions
- **Theory (Q5-10):** 6 questions
- **Choose Solution (Q11-17):** 7 questions
- **Real-World (Q18-20):** 3 questions

**Difficulty Distribution:**
- Easy (warm-up): Q16, Q17
- Medium: Q1, Q5-9, Q13
- Hard (tricky): Q2-4, Q10-12, Q14-15, Q18-20

**Common Mistakes to Avoid:**
1. Assuming volatile prevents race conditions (it doesn't)
2. Confusing Thread.join() with CountDownLatch
3. Thinking CyclicBarrier is for waiting (it's for syncing)
4. Creating threads per task (use pools!)
5. Assuming synchronized is always slower (it can be comparable)

---

## Self-Assessment

**Score Guide:**
- 18-20 correct: Expert level 🏆
- 15-17 correct: Advanced level ⭐
- 12-14 correct: Intermediate level ✓
- 9-11 correct: Beginner level (review concepts)
- Below 9: Study multi-threading fundamentals

---

End of Interview Q&A
