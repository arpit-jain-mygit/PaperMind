# Java Architect Interview - 21 Years Experience

## 1. OOPS Concepts

### Q1: Design a real-world payment system using OOP principles. What pillars would you apply and why?

**Explanation (Simple):**
Think of a payment system like a bank. You have different account types (checking, savings), but they all have common behavior (deposit, withdraw). Using inheritance, you avoid duplicating code. Using interfaces, you define contracts (PaymentGateway) that different providers (Stripe, PayPal) can implement without affecting your core business logic.

**Real Business Use Case:**
E-commerce platform needs to support multiple payment methods (Credit Card, Digital Wallet, UPI). Using abstraction (PaymentProcessor interface), you can add a new payment provider without modifying existing code—just implement the interface. This is crucial in production when you need to add new payment methods without risking existing ones.

**Real Benefit:**
- **Maintenance Cost**: Adding new payment gateway takes 1 day instead of 2 weeks
- **Risk Reduction**: Existing payment flows remain untouched
- **Code Reuse**: Common logic (logging, validation) written once in abstract class

---

### Q2: Explain Polymorphism with a practical inventory management scenario. When would you use method overriding vs. method overloading?

**Explanation (Simple):**
Imagine a warehouse that processes different product types. A book reduces stock by 1, a liquid reduces by volume (liters), an electronic reduces by quantity plus records fragility. Same action (updateStock), different behaviors—that's polymorphism.

**Real Business Use Case:**
Inventory system calculates shipping cost differently: physical goods by weight (kg), digital products are free, hazardous materials have surcharges. One method calculateShippingCost() adapts behavior based on product type without if-else chains.

**Real Benefit:**
- **Scalability**: Add new product types without modifying shipping logic
- **Readability**: Code reads naturally (product.calculateCost() instead of if-else)
- **Testing**: Each product type tested independently

---

### Q3: How would you design an authentication system using Encapsulation? What are the risks of exposing internals?

**Explanation (Simple):**
Your bank account PIN is encapsulated—you can't see the raw password. The bank (object) exposes only validatePin() method. Direct access to internal password data would allow security breaches. Hide implementation details, expose only safe operations.

**Real Business Use Case:**
User authentication module: password hashing strategy (bcrypt, Argon2) is internal detail. If exposed directly, someone could bypass hashing. Encapsulation ensures passwords are always hashed through validatePassword() method, which can be updated centrally.

**Real Benefit:**
- **Security**: Password logic changes globally without affecting client code
- **Flexibility**: You can change from bcrypt to Argon2 in one place
- **Compliance**: Easy to audit access to sensitive data

---

## 2. Exception Handling

### Q1: Design exception handling for a payment transaction. How do you differentiate between recoverable and non-recoverable errors?

**Explanation (Simple):**
Payment fails due to network timeout (recoverable—retry with exponential backoff) vs. invalid account number (non-recoverable—inform user immediately). Treat them differently: retry vs. fail fast.

**Real Business Use Case:**
Online checkout flow: network issue during payment → log, retry (user doesn't need action). Invalid card → show error immediately, ask user to try different card. Insufficient funds → inform user, offer payment plan.

**Real Benefit:**
- **User Experience**: User knows if it's their action needed or system retry
- **Business Metrics**: Recoverable errors tracked separately for SLA monitoring
- **Cost Savings**: Don't waste retries on permanent failures

**Code Pattern:**
```java
try {
    payment.process();
} catch (NetworkTimeoutException e) {  // Recoverable
    retryWithBackoff(payment, 3);
} catch (InvalidCardException e) {     // Non-recoverable
    throw new PaymentFailedException("Invalid card", e);
} catch (Exception e) {                // Unknown—log and alert ops
    logger.error("Unexpected error", e);
    alertOps();
}
```

---

### Q2: In a high-throughput order processing system, should you use checked or unchecked exceptions? Design the exception hierarchy.

**Explanation (Simple):**
Checked exceptions force handling everywhere (payment.process() throws PaymentException). For high-throughput systems, this is verbose. Unchecked exceptions bubble up naturally. But business exceptions should force developer awareness (OrderNotFoundException must be handled).

**Real Business Use Case:**
Order service: OrderNotFoundException (unchecked but documented—developer must handle). OutOfStockException extends RuntimeException because it's a business rule, not a programming error. Network failures can bubble up to global exception handler rather than polluting method signatures.

**Real Benefit:**
- **Performance**: No try-catch overhead in normal path
- **Clarity**: Method signature isn't cluttered with exception lists
- **Separation**: Business exceptions vs. infrastructure exceptions clear

---

### Q3: How do you prevent exception swallowing and log meaningful errors for debugging production issues?

**Explanation (Simple):**
Swallowing exceptions (catch (Exception e) {}) hides problems. It's like a server lamp turning off—you don't see the fire. Always log context (what were you doing, with what data) before rethrowing or handling.

**Real Business Use Case:**
Order processing fails silently because exception was caught and ignored. Days later, you find orders weren't created. With structured logging, you'd have seen "Failed to create order 12345, reason: database connection timeout at 2026-07-21 14:32:01".

**Real Benefit:**
- **Mean Time to Recovery**: 5 minutes instead of 2 hours
- **Audit Trail**: Compliance proof of what happened
- **Root Cause Analysis**: Don't waste time guessing

---

## 3. Multithreading

### Q1: Design a ticket booking system (like cinema seats) using threads. How do you prevent double-booking?

**Explanation (Simple):**
Imagine 1000 users trying to book seat A-5 simultaneously. Without synchronization, both think it's available and book it—disaster. Synchronization ensures only one thread can check+book atomically.

**Real Business Use Case:**
Flight booking: 500 users checking seat 12A simultaneously. Synchronized block ensures: check availability, if available mark booked, if not available show sold. No overlap.

**Real Benefit:**
- **Revenue Protection**: No double-bookings, no lost revenue
- **Customer Trust**: Seat confirmed means seat is yours
- **Data Integrity**: Booking count matches actual seats sold

**Pattern:**
```java
public class SeatBooking {
    private synchronized boolean bookSeat(String seatId, String userId) {
        if (seat[seatId].isAvailable()) {
            seat[seatId].book(userId);
            return true;
        }
        return false;
    }
}
```

---

### Q2: Your checkout process takes 5 seconds per order (1: validate, 2: check inventory, 3: process payment, 4: create shipment, 5: send email). How do you optimize using threading?

**Explanation (Simple):**
Today: steps run one after another (5+5+5+5+5 = 25 seconds for 5 users). Better: validate+check inventory+payment in sequence (critical path), run email notification in background thread (not critical path). Now 15 seconds for 5 users.

**Real Business Use Case:**
E-commerce checkout: Core flow (validation → inventory → payment) must be sequential and fast. Async tasks (send confirmation email, update analytics, notify warehouse) run in background thread pool without blocking customer.

**Real Benefit:**
- **Checkout Speed**: 40% faster (20 sec → 12 sec per order)
- **Scalability**: Server handles 2x orders without hardware upgrade
- **User Experience**: Customers don't wait for email sending

---

### Q3: What's the difference between wait(), notify(), and notifyAll()? When would you use each in a producer-consumer scenario?

**Explanation (Simple):**
Producer puts items in queue, consumer takes items. If queue is full, producer waits. If queue is empty, consumer waits. When producer adds item, it calls notify() to wake one waiting consumer. If multiple consumers, notifyAll() wakes all (not efficient but safe).

**Real Business Use Case:**
Order queue: multiple order processors (consumers) waiting for new orders. When order arrives, wake one processor to handle it. In high-load, use notifyAll() to ensure no order sits waiting.

**Real Benefit:**
- **Resource Efficiency**: Threads sleep when no work, wake when needed
- **Responsiveness**: Sub-millisecond latency (no polling)
- **CPU Efficiency**: No busy-waiting, low CPU usage

---

## 4. Collections

### Q1: You need to store user sessions (userId → SessionData). HashMap or ConcurrentHashMap? Explain the real production impact.

**Explanation (Simple):**
HashMap is not thread-safe. With 100 concurrent users, hash table can corrupt internally—session data lost. ConcurrentHashMap uses segment locks—multiple threads safely update different users simultaneously without global lock.

**Real Business Use Case:**
Web application with 10,000 concurrent users. HashMap causes random NullPointerException crashes during peak hours. Switch to ConcurrentHashMap: crashes stop, system stable.

**Real Benefit:**
- **Reliability**: Zero crashes during peak load
- **Throughput**: 5x more requests per second handled
- **Support Costs**: No emergency calls at 3 AM

---

### Q2: For a cache storing 10,000 products, should you use HashMap, LinkedHashMap, or LRU cache? Why?

**Explanation (Simple):**
HashMap: no eviction policy. LinkedHashMap: can evict oldest. LRU Cache: evicts least-recently-used (most common access pattern). Products A, B, C accessed hourly; products X, Y, Z accessed yearly. Keep A, B, C in cache, evict X, Y, Z.

**Real Business Use Case:**
E-commerce product metadata cache: Popular products (shoes, phones) should always be cached. Rare products (specialized tools) should be evicted. LRU cache automatically keeps hot data.

**Real Benefit:**
- **Memory Efficiency**: Use 1GB cache optimally instead of 5GB
- **Performance**: 99% cache hit rate for popular products
- **Cost Savings**: Reduce database load by 90%

---

### Q3: You need a list of orders sorted by creation date, and you frequently retrieve orders. List or LinkedList?

**Explanation (Simple):**
ArrayList: fast random access (get(100)). LinkedList: fast insertion/deletion in middle. Retrieving orders by index? ArrayList. Building order sequence adding to head/tail? LinkedList.

**Real Business Use Case:**
Order dashboard: retrieve order #5000 by position → ArrayList (50 microseconds). Report generation: process orders sequentially and remove completed → LinkedList (or stream better).

**Real Benefit:**
- **Performance**: Dashboard loads 10x faster with ArrayList
- **Simplicity**: Choose collection for actual usage pattern
- **Predictability**: O(1) vs O(n) matters at scale

---

## 5. Concurrent API

### Q1: You're indexing 1 million documents. Threading is slow due to context switching. How would you use ExecutorService with optimal thread count?

**Explanation (Simple):**
One thread per document = 1 million threads (memory explosion). One thread = 1 million sequential (too slow). Sweet spot: number of threads = number of CPU cores. Indexing is CPU-intensive, not I/O-intensive.

**Real Business Use Case:**
Elasticsearch bulk indexing: Server has 8 cores. Use ThreadPoolExecutor with 8 threads. Each thread indexes documents in batches. Completes in 5 minutes instead of 40 minutes (8x faster).

**Real Benefit:**
- **Speed**: 8x throughput with same hardware
- **CPU Efficiency**: 100% CPU utilization, no idle cores
- **Cost Savings**: No need for 8x more servers

```java
ExecutorService executor = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors()
);
// Submit 1M tasks, let thread pool handle batching
```

---

### Q2: Your API needs to call 5 external services in parallel for each request (each takes 1-2 seconds). How do you use CompletableFuture?

**Explanation (Simple):**
Sequentially: 5 + 5 + 5 + 5 + 5 = 25 seconds per request. Parallel: max(5, 5, 5, 5, 5) = 5 seconds. CompletableFuture lets you start all 5 simultaneously and combine results.

**Real Business Use Case:**
Travel booking: simultaneously call flight API, hotel API, car rental API, insurance API, payment API. Combine all results into one booking response. Sequential = 10 seconds. Parallel = 2 seconds. Users perceive 5x faster.

**Real Benefit:**
- **User Experience**: 10-second wait vs 2-second wait
- **Server Capacity**: Handle 5x more users with same resources
- **Revenue**: Faster checkout = higher conversion

```java
CompletableFuture<Flight> flights = callFlightAPI();
CompletableFuture<Hotel> hotels = callHotelAPI();
CompletableFuture<Booking> booking = CompletableFuture.allOf(
    flights, hotels, ...
).thenApply(...);
```

---

### Q3: How do you prevent thread pool exhaustion in a system calling multiple external APIs?

**Explanation (Simple):**
If all threads are waiting for external API response, and more requests arrive, new requests find no threads—deadlock. Use separate thread pools for different tasks and set queue size limits.

**Real Business Use Case:**
Microservices: database thread pool (20 threads), external API thread pool (10 threads), cache thread pool (5 threads). If external API is slow, only those 10 threads block; database operations proceed normally.

**Real Benefit:**
- **Isolation**: Slow external API doesn't kill database performance
- **Resilience**: System degrades gracefully instead of complete failure
- **Observability**: Can see "API pool queue=100" and alert

---

## 6. Data Structures

### Q1: Design a leaderboard system for an online gaming platform. Would you use Array, Sorted List, or Tree (BST, TreeMap)?

**Explanation (Simple):**
Array: insertion/update O(n), retrieval by rank O(1). TreeMap: insertion/update O(log n), retrieval by rank O(n) but iteration is sorted. For leaderboards with frequent updates, TreeMap is inefficient. Consider sorted list or skip list.

**Real Business Use Case:**
Game with 100,000 players. Player scores update every 10 seconds. Leaderboard page shows top 100 players. Array: 100,000 array reads to find top 100 (slow). Priority Queue or heap-based solution: keep only top 100 in memory (fast).

**Real Benefit:**
- **Memory**: Store 100 players instead of 100,000
- **Performance**: Leaderboard page loads in 10ms instead of 1 second
- **Real-time**: Can push leaderboard updates as players climb

---

### Q2: You need to find if a user exists in a group of 10 million users. Array/ArrayList or HashSet?

**Explanation (Simple):**
ArrayList: O(n) = scan all 10 million (10 million comparisons). HashSet: O(1) = direct lookup (1-2 comparisons). At scale, O(1) vs O(n) is life or death.

**Real Business Use Case:**
Social network: check if user A can message user B (if B in user A's contacts list). 10 million contacts. ArrayList approach: scan all 10 million (10 seconds). HashSet: instant (1 millisecond).

**Real Benefit:**
- **Performance**: 10,000x faster
- **Scalability**: Can handle 1 billion contacts without degradation
- **Cost**: No need for more powerful servers

---

### Q3: Design a cache with 10,000 entries where you need fast lookup AND periodic removal of expired entries. Which data structure?

**Explanation (Simple):**
HashMap: O(1) lookup but no expiry tracking. HashMap + PriorityQueue (expiry time): HashMap for lookup, heap for expiry ordering. Java: use LinkedHashMap or custom solution with scheduled cleanup.

**Real Business Use Case:**
Session cache: 10,000 active sessions, each expires after 30 minutes. Lookup session by ID: O(1). Remove expired: scan all 10,000 every 5 minutes (expensive). Better: track expiry in separate structure, remove only expired entries.

**Real Benefit:**
- **Memory**: Don't leak dead sessions
- **Performance**: Fast lookup + efficient expiry
- **Operational**: Predictable memory usage

---

## 7. Algorithms

### Q1: Your warehouse has 100,000 orders in processing. You need to find orders by customer name. Linear search or Binary search? What's the prerequisite?

**Explanation (Simple):**
Linear search (O(n)): check all 100,000 orders. Binary search (O(log n)): find in ~17 comparisons. But binary search requires sorted list. Data must be pre-sorted.

**Real Business Use Case:**
Order search in admin panel: customer enters name "John Smith". Without index: scan all 100,000 orders (5 seconds). With sorted index: find in 17 checks (5 milliseconds).

**Real Benefit:**
- **Performance**: Admin query response 1000x faster
- **Scale**: Can handle 1 million orders without slowdown
- **Operations**: Admins handle 100x more queries per shift

---

### Q2: You need to find the shortest path for delivery from warehouse to customer (50 stops). Brute force or Dijkstra?

**Explanation (Simple):**
Brute force (50! = 3 × 10^64 combinations): impossible in any time. Dijkstra: O(V²) or O(E log V): practical (seconds to minutes). Real-world: use heuristics (A*) for near-optimal solution quickly.

**Real Business Use Case:**
Last-mile delivery: 50 deliveries today. Brute force = forever. Dijkstra = exact optimal path in seconds. Heuristic (A*) = near-optimal in milliseconds (good enough for real-time).

**Real Benefit:**
- **Speed**: Plan routes in real-time instead of batch
- **Efficiency**: Reduce delivery time by 20% (fewer miles)
- **Revenue**: Deliver more packages per driver per day

---

### Q3: Sorting algorithms: for 100,000 orders, should you use Quicksort, Mergesort, or Heap sort?

**Explanation (Simple):**
Quicksort: O(n log n) average, O(n²) worst case (random data usually fine). Mergesort: O(n log n) guaranteed, extra O(n) memory. Heapsort: O(n log n), no extra memory. For production, prefer Mergesort (guaranteed performance) or use Quicksort with good pivot selection.

**Real Business Use Case:**
Daily order reconciliation: sort 1 million orders. Quicksort: usually 20 seconds, occasionally 2 minutes (when pivot selection fails). Mergesort: always 20 seconds predictably. In production, predictability beats average speed.

**Real Benefit:**
- **Predictability**: SLA of "reconciliation completes by 6 AM"
- **Reliability**: No worst-case performance hitting deadline
- **Operations**: Support can trust the process

---

## Interview Tips for 21-Year Veteran

1. **Real Numbers**: Mention scales (1M users, 10K QPS, 100GB data) to show you think about real systems.
2. **Tradeoffs**: Every design is a tradeoff—mention cost, performance, maintenance trade-offs explicitly.
3. **Failure Stories**: Interviewers respect engineers who've seen production failures. Share one: "We had double-booking due to missing synchronized block, lost $50K revenue, learned hard lesson."
4. **Evolution**: Show how solutions evolved: "Started with ArrayList, hit performance wall at 10K items, migrated to TreeMap, now using specialized data structure."
5. **Observability**: Always mention: logging, monitoring, alerting, metrics. Production is about visibility.
