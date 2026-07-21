# Java Features Journey: Java 2 to Java 21

## Table of Contents
1. [Generics (Java 5)](#generics-java-5)
2. [Lambdas & Streams (Java 8)](#lambdas--streams-java-8)
3. [Modules (Java 9)](#modules-java-9)
4. [var Keyword (Java 10)](#var-keyword-java-10)
5. [HttpClient (Java 11)](#httpclient-java-11)
6. [Records (Java 14-16)](#records-java-14-16)
7. [LTS - Long-Term Support (Java 17)](#lts--long-term-support-java-17)
8. [Virtual Threads (Java 21)](#virtual-threads-java-21)

---

## Generics (Java 5)

**Before**: You could put anything in a List
```java
List myList = new ArrayList();
myList.add("Hello");
myList.add(123);  // Oops, mixed types
```
When you get it back, you don't know what type it is. You'd get errors at runtime.

**After**: You specify the type
```java
List<String> myList = new ArrayList<String>();
myList.add("Hello");  // ✓ OK
myList.add(123);      // ✗ Compiler catches this error
```

**Why it mattered**: Prevents bugs before code runs. Saves debugging time.

---

## Lambdas & Streams (Java 8)

**Before**: Looping was verbose
```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4);
List<Integer> evenNumbers = new ArrayList<>();
for (Integer n : numbers) {
  if (n % 2 == 0) {
    evenNumbers.add(n);
  }
}
```

**After**: One line
```java
List<Integer> evenNumbers = numbers.stream()
  .filter(n -> n % 2 == 0)
  .collect(Collectors.toList());
```

**Why it mattered**: Less code, easier to read, easier to run in parallel (`parallelStream()`).

---

## Modules (Java 9)

**Before**: One large JAR with everything mixed together. Hard to know what's public API and what's internal.

**After**: You explicitly say what each module exports
```java
module myapp {
  requires java.base;
  exports com.myapp.api;  // Public
  // com.myapp.internal is hidden
}
```

**Why it mattered**: Large apps became easier to organize. Smaller bundle size (you don't ship unused code).

---

## var Keyword (Java 10)

**Before**: Repetitive typing
```java
ArrayList<String> names = new ArrayList<String>();
HashMap<String, Integer> ages = new HashMap<String, Integer>();
```

**After**: Let compiler figure out the type
```java
var names = new ArrayList<String>();
var ages = new HashMap<String, Integer>();
```

**Why it mattered**: Less boilerplate, cleaner code. Java IDE autocomplete still works.

---

## HttpClient (Java 11)

**Before**: You had to add external libraries like Apache HttpClient or OkHttp just to make HTTP requests.

**After**: It's built-in
```java
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
  .uri(URI.create("https://api.example.com"))
  .build();
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
```

**Why it mattered**: Fewer dependencies = simpler projects, easier to maintain.

---

## Records (Java 14-16)

**Before**: Creating a simple data class required boilerplate
```java
public class Person {
  private String name;
  private int age;
  
  public Person(String name, int age) {
    this.name = name;
    this.age = age;
  }
  
  public String getName() { return name; }
  public int getAge() { return age; }
  // equals(), hashCode(), toString() methods...
}
```

**After**: One line
```java
record Person(String name, int age) {}
```
Automatically gives you constructor, getters, `equals()`, `hashCode()`, `toString()`.

**Why it mattered**: Immutable data classes are safer (no accidental modifications). Massive reduction in boilerplate.

---

## LTS - Long-Term Support (Java 17)

**Before**: Java releases every 6 months. You had to upgrade constantly or lose support.

**After**: Every 3 years, an LTS release gets 8 years of support
- Java 8 (2014) - Still supported
- Java 11 (2018) - Still supported  
- Java 17 (2021) - Will be supported until 2029

**Why it mattered**: Enterprises can stay on one version without rushing to upgrade. Peace of mind.

---

## Virtual Threads (Java 21)

**Before**: If you wanted to handle 10,000 concurrent requests, you needed 10,000 OS threads. Each thread costs ~2MB of memory. That's 20GB just for thread stacks. Your server crashes.

**After**: Virtual Threads are lightweight
```java
for (int i = 0; i < 1_000_000; i++) {
  Thread.startVirtualThread(() -> {
    // Handle request
  });
}
```
Each virtual thread costs only a few KB. Now you can easily handle millions.

**Why it mattered**: Java can now handle massive concurrency without frameworks like Akka or Project Reactor. Simple, blocking code scales.

---

## Quick Summary

Each feature solved a real pain point:
- **Generics** = Safety
- **Lambdas** = Less code
- **Modules** = Better organization
- **var** = Less boilerplate
- **HttpClient** = Fewer dependencies
- **Records** = Immutable data classes
- **LTS** = Stability for enterprises
- **Virtual Threads** = Massive scale
