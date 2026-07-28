# Multi-Threading MCQ Quiz - Advanced Level
## All Concepts from topmate-java-saurabh-manik.md

**Instructions:**
- More than 1 answer can be correct (very tricky)
- Questions are grouped by type
- Answer key with detailed explanations at the end

---

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

