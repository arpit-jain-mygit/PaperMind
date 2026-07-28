# LeetCode Concurrency Problems

A curated list of concurrency and multithreading problems from LeetCode, organized by difficulty.

## Easy

### 1114. Print in Order
- **Difficulty:** Easy
- **Acceptance:** 73.3%
- **Link:** https://leetcode.com/problems/print-in-order/
- **Topics:** Multithreading, Synchronization
- **Description:** Given an instance of class Foo with three methods (first, second, third), the same instance is passed to three different threads. Ensure that the three methods execute in the order first, second, third.
- **Key Concepts:** Thread synchronization, barrier synchronization

### 1279. Traffic Light Controlled Intersection
- **Difficulty:** Easy
- **Acceptance:** 73.1%
- **Link:** https://leetcode.com/problems/traffic-light-controlled-intersection/
- **Topics:** Multithreading, Concurrency Control
- **Description:** Implement a traffic light controller that manages the flow of cars in an intersection with two roads. Ensure only one car crosses at a time, respecting traffic light states.
- **Key Concepts:** Thread coordination, mutual exclusion, state management

---

## Medium

### 3517. Smallest Palindromic Rearrangement I
- **Difficulty:** Medium
- **Acceptance:** 71.0%
- **Link:** https://leetcode.com/problems/smallest-palindromic-rearrangement-i/
- **Topics:** String manipulation, concurrent processing
- **Description:** Rearrange a string into a palindrome with the lexicographically smallest arrangement.
- **Key Concepts:** Character frequency, palindrome properties

### 1115. Print FooBar Alternately
- **Difficulty:** Medium
- **Acceptance:** 72.9%
- **Link:** https://leetcode.com/problems/print-foobar-alternately/
- **Topics:** Multithreading, Synchronization
- **Description:** Given an instance of class FooBar, print "foo" and "bar" alternately from different threads. Ensure strict alternation without overlap.
- **Key Concepts:** Thread alternation, semaphores, condition variables

### 1116. Print Zero Even Odd
- **Difficulty:** Medium
- **Acceptance:** 65.9%
- **Link:** https://leetcode.com/problems/print-zero-even-odd/
- **Topics:** Multithreading, Synchronization
- **Description:** Three threads need to print output in sequence: zero, even, odd, zero, even, odd... ensuring exact order.
- **Key Concepts:** Multiple thread coordination, state machines

### 1117. Building H2O
- **Difficulty:** Medium
- **Acceptance:** 58.4%
- **Link:** https://leetcode.com/problems/building-h2o/
- **Topics:** Multithreading, Barrier Synchronization
- **Description:** Simulate the process of building H₂O molecules. One oxygen thread and two hydrogen threads must synchronize to form water molecules.
- **Key Concepts:** Producer-consumer pattern, barrier synchronization, resource counting

### 1188. Design Bounded Blocking Queue
- **Difficulty:** Medium
- **Acceptance:** 73.9%
- **Link:** https://leetcode.com/problems/design-bounded-blocking-queue/
- **Topics:** Concurrency, Data Structures
- **Description:** Implement a thread-safe bounded blocking queue with enqueue and dequeue operations.
- **Key Concepts:** Blocking queue, thread-safe collections, wait-notify patterns

### 1195. Fizz Buzz Multithreaded
- **Difficulty:** Medium
- **Acceptance:** 75.0%
- **Link:** https://leetcode.com/problems/fizz-buzz-multithreaded/
- **Topics:** Multithreading, Synchronization
- **Description:** Implement the FizzBuzz algorithm with four threads executing concurrently, each handling a specific case.
- **Key Concepts:** Thread pool coordination, conditional execution, thread ordering

### 1226. The Dining Philosophers
- **Difficulty:** Medium
- **Acceptance:** 54.0%
- **Link:** https://leetcode.com/problems/the-dining-philosophers/
- **Topics:** Deadlock Prevention, Concurrency
- **Description:** Classic synchronization problem where philosophers must eat and think without deadlocking while competing for limited forks.
- **Key Concepts:** Deadlock detection and prevention, resource allocation, circular wait avoidance

### 1242. Web Crawler Multithreaded
- **Difficulty:** Medium
- **Acceptance:** 51.3%
- **Link:** https://leetcode.com/problems/web-crawler-multithreaded/
- **Topics:** Multithreading, Graph Traversal
- **Description:** Implement a multi-threaded web crawler that crawls a website and returns all reachable URLs from a starting URL.
- **Key Concepts:** Thread pool, concurrent data structures, domain-based URL filtering

---

## Learning Path Recommendations

**Beginner (Start Here):**
1. 1114. Print in Order
2. 1279. Traffic Light Controlled Intersection

**Intermediate (Build Foundations):**
3. 1115. Print FooBar Alternately
4. 1116. Print Zero Even Odd
5. 1195. Fizz Buzz Multithreaded

**Advanced (Master Patterns):**
6. 1117. Building H2O
7. 1188. Design Bounded Blocking Queue
8. 1226. The Dining Philosophers
9. 1242. Web Crawler Multithreaded

**Special Topics:**
- 3517. Smallest Palindromic Rearrangement I (string algorithms)

## Key Concurrency Concepts Covered

- **Synchronization Primitives:** Locks, Semaphores, Condition Variables
- **Coordination Patterns:** Barriers, Latches, Phasers
- **Data Structures:** Blocking queues, thread-safe collections
- **Deadlock Prevention:** Resource ordering, timeout strategies
- **Thread Management:** Thread pools, executors, task scheduling
- **Real-world Scenarios:** Web crawlers, molecule simulation, philosopher dining

## Study Tips

- Start with the easy problems to understand basic synchronization
- Experiment with different synchronization mechanisms (locks vs semaphores vs conditions)
- Pay attention to edge cases (timeouts, concurrent modifications)
- Review classic problems like Dining Philosophers for interview preparation
- Practice explaining your approach to avoid deadlocks and race conditions
