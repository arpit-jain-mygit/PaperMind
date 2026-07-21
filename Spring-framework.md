# Spring Framework & SpringBoot - Complete Guide

## Table of Contents
1. [Spring Framework Modules Overview](#spring-framework-modules-overview)
   - 1.1 [Spring Core (IoC Container)](#1-spring-core-ioc-container)
   - 1.2 [Spring AOP](#2-spring-aop-aspect-oriented-programming)
   - 1.3 [Spring Data Access/Integration](#3-spring-data-accessintegration)
   - 1.4 [Spring Web MVC](#4-spring-web-mvc)
   - 1.5 [Spring WebFlux](#5-spring-webflux)
   - 1.6 [Spring Security](#6-spring-security)
   - 1.7 [Spring Test](#7-spring-test)
2. [SpringBoot in the Ecosystem](#springboot-in-the-ecosystem)
3. [SpringBatch Overview](#springbatch-overview)
4. [Top 30 Spring & SpringBoot FAQs](#top-30-spring--springboot-faqs)
   - 4.1 [What is Dependency Injection (DI)?](#1-what-is-dependency-injection-di)
   - 4.2 [What is IoC (Inversion of Control)?](#2-what-is-ioc-inversion-of-control)
   - 4.3 [What is a Bean in Spring?](#3-what-is-a-bean-in-spring)
   - 4.4 [Difference between @Component, @Service, @Repository, @Controller](#4-difference-between-component-service-repository-controller)
   - 4.5 [What is @Autowired?](#5-what-is-autowired-and-how-does-it-work)
   - 4.6 [@Bean vs @Component](#6-what-is-the-difference-between-bean-and-component)
   - 4.7 [Singleton vs Prototype Scope](#7-what-is-the-difference-between-singleton-and-prototype-scope)
   - 4.8 [What is ApplicationContext?](#8-what-is-applicationcontext)
   - 4.9 [What is @Configuration?](#9-what-is-configuration)
   - 4.10 [What is @SpringBootApplication?](#10-what-is-springbootapplication)
   - 4.11 [Auto-configuration in SpringBoot](#11-what-is-auto-configuration-in-springboot)
   - 4.12 [application.properties & application.yml](#12-what-is-applicationproperties-and-applicationyml)
   - 4.13 [SpringBoot Profiles](#13-what-are-profiles-in-springboot)
   - 4.14 [@RestController vs @Controller](#14-what-is-the-difference-between-restcontroller-and-controller)
   - 4.15 [@RequestMapping & @GetMapping](#15-what-is-requestmapping-and-getmapping)
   - 4.16 [@PathVariable & @RequestParam](#16-what-is-pathvariable-and-requestparam)
   - 4.17 [@RequestBody & @ResponseBody](#17-what-is-requestbody-and-responsebody)
   - 4.18 [Exception Handling](#18-how-does-spring-handle-exceptions)
   - 4.19 [AOP (Aspect-Oriented Programming)](#19-what-is-aop-aspect-oriented-programming)
   - 4.20 [@Transactional](#20-what-is-transactional)
   - 4.21 [Spring Data JPA](#21-what-is-spring-data-jpa)
   - 4.22 [JPA vs Hibernate](#22-what-is-the-difference-between-jpa-and-hibernate)
   - 4.23 [Lazy vs Eager Loading](#23-what-is-lazy-loading-and-eager-loading)
   - 4.24 [N+1 Problem](#24-what-is-n1-problem-in-jpa)
   - 4.25 [MVC Request Flow](#25-what-is-spring-mvc-request-flow)
   - 4.26 [DispatcherServlet](#26-what-is-the-purpose-of-dispatcherservlet)
   - 4.27 [Content Negotiation](#27-what-is-content-negotiation)
   - 4.28 [REST & RESTful Principles](#28-what-is-rest-and-restful-principles)
   - 4.29 [CORS](#29-what-is-cors-and-how-to-handle-it-in-spring)
   - 4.30 [Actuator](#30-what-is-actuator-in-springboot)
5. [Summary Table & Architecture](#summary-table-spring-core-features)
6. [How They Fit Together](#how-they-fit-together)

---

# Spring Framework Modules Overview

Spring Framework is a large umbrella project divided into modules:

## Core Modules

### 1. **Spring Core (IoC Container)**
- **What:** Dependency Injection (DI) container - the heart of Spring
- **Purpose:** Manages object creation and dependency injection
- **Key concept:** Inversion of Control (IoC) - framework controls object lifecycle
- **Example:**
```java
// Without Spring: Manual object creation
UserService service = new UserService(new UserRepository());

// With Spring: Container creates and injects
@Service
public class UserService {
    @Autowired
    private UserRepository repo;  // Spring injects this
}
```

### 2. **Spring AOP (Aspect-Oriented Programming)**
- **What:** Cross-cutting concerns (logging, security, transactions)
- **Purpose:** Separate business logic from infrastructure concerns
- **Example:**
```java
@Aspect
@Component
public class LoggingAspect {
    @Before("execution(* com.example.service.*.*(..))")
    public void logBefore(JoinPoint jp) {
        System.out.println("Method called: " + jp.getSignature());
    }
}
```

### 3. **Spring Data Access/Integration**
- **Submodules:**
  - **Spring JDBC:** Simplified database access, template-based
  - **Spring ORM:** Integration with Hibernate, JPA
  - **Spring TX:** Transaction management (Declarative & Programmatic)
  - **Spring JMS:** Message-oriented middleware
  - **Spring OXMX:** XML/JSON serialization

### 4. **Spring Web MVC**
- **What:** Model-View-Controller framework for web applications
- **Key concepts:**
  - DispatcherServlet: Central request handler
  - Controllers: Handle HTTP requests
  - Views: Render responses
- **Example:**
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}
```

### 5. **Spring WebFlux**
- **What:** Reactive web framework (non-blocking)
- **Purpose:** Handle high concurrency with fewer threads
- **When to use:** Real-time apps, high-traffic systems
- **Example:**
```java
@RestController
public class ReactiveController {
    @GetMapping("/data")
    public Mono<String> getData() {
        return Mono.just("Hello Reactive World");
    }
}
```

### 6. **Spring Security**
- **What:** Authentication and authorization framework
- **Key features:**
  - User authentication
  - Authorization (Role-based access control)
  - CSRF protection
  - Session management
- **Example:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .antMatchers("/admin/**").hasRole("ADMIN")
            .antMatchers("/user/**").hasRole("USER")
            .anyRequest().authenticated()
            .and().formLogin();
    }
}
```

### 7. **Spring Test**
- **What:** Testing support with mocking and integration testing
- **Example:**
```java
@RunWith(SpringRunner.class)
@SpringBootTest
public class UserServiceTest {
    @MockBean
    private UserRepository repo;
    
    @Test
    public void testGetUser() {
        Mockito.when(repo.findById(1L)).thenReturn(new User());
    }
}
```

---

# SpringBoot in the Ecosystem

## What is SpringBoot?

**Not a replacement for Spring, but an enhancement** that makes Spring easier to use.

### Problems SpringBoot Solves

**Before SpringBoot:**
- Massive XML configuration files
- Manual dependency management
- Complex setup for basic features
- Slow startup time

**With SpringBoot:**
```java
// Single annotation to start entire Spring application
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### Key SpringBoot Features

1. **Auto-Configuration**
   - Automatically configures Spring based on classpath
   - `@SpringBootApplication` = `@EnableAutoConfiguration` + `@Configuration` + `@ComponentScan`

2. **Embedded Servers**
   - Tomcat/Jetty built-in (no need for separate server)
   - Deploy as JAR file

3. **Starter Dependencies (Starter POMs)**
   - Pre-configured dependency sets
   - Example: `spring-boot-starter-web` includes Spring Web, Tomcat, Jackson

4. **Properties-based Configuration**
   - application.properties or application.yml
   - No XML needed
```properties
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
```

5. **Actuator**
   - Built-in monitoring and management endpoints
   - `/actuator/health`, `/actuator/metrics`

---

# SpringBatch Overview

## What is SpringBatch?

**Purpose:** Framework for processing large volumes of data in batch jobs (offline processing)

### Key Concepts

1. **Job:** Entire batch process
2. **Step:** Individual task within a job (read → process → write)
3. **ItemReader:** Reads data from source
4. **ItemProcessor:** Transforms/validates data
5. **ItemWriter:** Writes processed data to destination

### Example: Simple Batch Job

```java
@Configuration
@EnableBatchProcessing
public class BatchConfig {
    
    @Bean
    public Job importUserJob(JobBuilderFactory jobBuilder, Step step1) {
        return jobBuilder.get("importUserJob")
            .start(step1)
            .build();
    }
    
    @Bean
    public Step step1(StepBuilderFactory stepBuilder, 
                      ItemReader<User> reader,
                      ItemProcessor<User, User> processor,
                      ItemWriter<User> writer) {
        return stepBuilder.get("step1")
            .<User, User>chunk(10)  // Process 10 items at a time
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }
    
    @Bean
    public ItemReader<User> reader() {
        FlatFileItemReader<User> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("users.csv"));
        // Configure field mapping...
        return reader;
    }
}
```

### When to Use SpringBatch

- CSV/Excel file imports
- Data migration
- Scheduled reports generation
- ETL (Extract, Transform, Load) processes
- NOT for real-time processing

---

# Top 30 Spring & SpringBoot FAQs

## 1. What is Dependency Injection (DI)? {#1-what-is-dependency-injection-di}

**Concept:** Instead of a class creating its dependencies, the framework provides (injects) them.

**Example:**
```java
// Without DI: Hard to test, tightly coupled
public class UserService {
    private UserRepository repo = new UserRepository();
}

// With DI: Loosely coupled, testable
@Service
public class UserService {
    @Autowired
    private UserRepository repo;  // Spring injects this
}
```

**Why use DI:**
- Loose coupling
- Easy testing (mock dependencies)
- Flexible configuration
- Single Responsibility Principle

---

## 2. What is IoC (Inversion of Control)?

**Concept:** Framework controls object creation and lifecycle, not the programmer.

**Traditional approach (No IoC):**
```java
UserService service = new UserService();
service.performAction();
```

**IoC approach (Spring):**
```java
// Spring creates and manages objects
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class);  // Spring takes control
    }
}

@Service
public class UserService {
    // Spring automatically creates and injects dependencies
}
```

---

## 3. What is a Bean in Spring?

**Definition:** An object managed by Spring IoC container.

**Example:**
```java
// Method 1: Using @Bean annotation
@Configuration
public class AppConfig {
    @Bean
    public UserService userService() {
        return new UserService(userRepository());
    }
    
    @Bean
    public UserRepository userRepository() {
        return new UserRepository();
    }
}

// Method 2: Using @Component (auto-detected)
@Component
public class UserService {
    // Spring automatically makes this a bean
}
```

**Bean lifecycle:**
1. Instantiation
2. Dependency injection
3. Initialization (@PostConstruct)
4. Usage
5. Destruction (@PreDestroy)

---

## 4. Difference between @Component, @Service, @Repository, @Controller

**All are @Component but semantic:**

```java
@Component  // Generic - reusable component
public class GenericClass { }

@Service    // Business logic layer
@Service
public class UserService {
    public User createUser(User user) { }
}

@Repository  // Data access layer - adds exception translation
@Repository
public class UserRepository {
    public User findById(Long id) { }
}

@Controller  // Web layer - handles HTTP requests
@Controller
public class UserController {
    @GetMapping("/users")
    public String getUsers() { }
}

@RestController  // Like @Controller but returns JSON/XML
@RestController
public class UserRestController {
    @GetMapping("/api/users")
    public List<User> getUsers() { }  // Returns JSON
}
```

**Repository special feature:** Converts database exceptions to Spring-agnostic exceptions.

---

## 5. What is @Autowired and how does it work?

**Purpose:** Automatic dependency injection

**Methods:**

```java
// Method 1: Field injection
@Service
public class UserService {
    @Autowired
    private UserRepository repo;
}

// Method 2: Constructor injection (RECOMMENDED)
@Service
public class UserService {
    private UserRepository repo;
    
    public UserService(UserRepository repo) {
        this.repo = repo;
    }
}

// Method 3: Setter injection
@Service
public class UserService {
    private UserRepository repo;
    
    @Autowired
    public void setRepo(UserRepository repo) {
        this.repo = repo;
    }
}
```

**Why constructor injection is best:**
- Dependencies are immutable
- Easier to test
- Explicit dependencies
- Circular dependency detection

---

## 6. What is the difference between @Bean and @Component?

| Feature | @Bean | @Component |
|---------|-------|-----------|
| **Declaration** | Method-level | Class-level |
| **Where** | @Configuration classes | Regular classes |
| **Control** | Manual (you write logic) | Automatic (classpath scanning) |
| **Third-party** | Can create beans from external classes | Cannot (need access to source) |
| **Example** | Creating beans from external libraries | Your own classes |

**Example:**
```java
// @Component - Auto-detected
@Component
public class MyService { }

// @Bean - Manual creation (e.g., configuring external library)
@Configuration
public class Config {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
```

---

## 7. What is the difference between Singleton and Prototype scope?

**Singleton (Default):**
- Only ONE instance for entire application
- Shared across all requests
- Thread-safe (should be stateless)

```java
@Component
@Scope("singleton")  // Default
public class UserService {
    // Only ONE instance exists
}
```

**Prototype:**
- NEW instance created every time bean is injected
- Not shared
- Use for stateful objects

```java
@Component
@Scope("prototype")
public class Request {
    private String data;  // Instance-specific data
}
```

**Request & Session scopes (Web only):**
```java
@Component
@Scope("request")  // New instance per HTTP request
public class RequestContext { }

@Component
@Scope("session")  // New instance per user session
public class UserSession { }
```

---

## 8. What is ApplicationContext?

**Definition:** Container that manages all Spring beans and their lifecycle.

**Types:**
```java
// 1. ClassPathXmlApplicationContext
ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
UserService service = context.getBean(UserService.class);

// 2. AnnotationConfigApplicationContext
ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
UserService service = context.getBean(UserService.class);

// 3. SpringBoot (automatic)
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(App.class);
        UserService service = context.getBean(UserService.class);
    }
}
```

---

## 9. What is @Configuration?

**Purpose:** Marks a class as a source of bean definitions.

```java
@Configuration
public class AppConfig {
    
    @Bean
    public UserRepository userRepository() {
        return new UserRepository();
    }
    
    @Bean
    public UserService userService(UserRepository repo) {
        return new UserService(repo);  // Spring injects repo
    }
}
```

**With SpringBoot:** Usually just @SpringBootApplication (which includes @Configuration)

---

## 10. What is @SpringBootApplication?

**Meta-annotation combining three:**

```java
@SpringBootApplication
// Equivalent to:
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**What each does:**
1. `@Configuration` - Marks as Spring configuration class
2. `@ComponentScan` - Scans classpath for @Component/@Service/@Repository
3. `@EnableAutoConfiguration` - Auto-configures based on classpath

---

## 11. What is auto-configuration in SpringBoot?

**Concept:** SpringBoot automatically configures Spring based on what's on the classpath.

```java
// If you have spring-boot-starter-web (includes Tomcat), 
// SpringBoot automatically:
// - Configures DispatcherServlet
// - Sets up embedded Tomcat server
// - Configures Jackson for JSON
// - No manual configuration needed!

// Disable specific auto-configs
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class Application { }

// Or in application.properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

---

## 12. What is application.properties and application.yml?

**Purpose:** Externalized configuration (no code changes needed to change settings).

**application.properties:**
```properties
server.port=8080
server.servlet.context-path=/api
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=update
logging.level.root=INFO
```

**application.yml:**
```yaml
server:
  port: 8080
  servlet:
    context-path: /api
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: password
  jpa:
    hibernate:
      ddl-auto: update
logging:
  level:
    root: INFO
```

**Priority order:**
1. Command-line arguments
2. Environment variables
3. application-{profile}.properties
4. application.properties

---

## 13. What are profiles in SpringBoot?

**Purpose:** Different configurations for different environments (dev/test/prod).

```properties
# application.properties (default)
spring.application.name=myapp

# application-dev.properties
spring.datasource.url=jdbc:mysql://localhost/dev_db
logging.level.root=DEBUG

# application-prod.properties
spring.datasource.url=jdbc:mysql://prod-server/prod_db
logging.level.root=ERROR
server.ssl.enabled=true
```

**Activate profile:**
```bash
# Command line
java -jar app.jar --spring.profiles.active=prod

# Environment variable
export SPRING_PROFILES_ACTIVE=prod

# application.properties
spring.profiles.active=dev
```

---

## 14. What is the difference between @RestController and @Controller?

| Feature | @Controller | @RestController |
|---------|-------------|-----------------|
| **Return type** | Returns view (HTML) | Returns JSON/XML/data |
| **Response serialization** | Manual | Automatic (via @ResponseBody) |
| **Use case** | Traditional web apps | REST APIs |

```java
// @Controller - Returns HTML views
@Controller
public class UserController {
    @GetMapping("/users")
    public String getUsers(Model model) {
        model.addAttribute("users", userService.getAll());
        return "users";  // Returns users.html view
    }
}

// @RestController - Returns JSON
@RestController
@RequestMapping("/api/users")
public class UserRestController {
    @GetMapping
    public List<User> getUsers() {
        return userService.getAll();  // Returns JSON
    }
}
```

---

## 15. What is @RequestMapping and @GetMapping?

**@RequestMapping:** Maps URL paths to handler methods

```java
@RestController
@RequestMapping("/api/users")  // Base path
public class UserController {
    
    // GET /api/users
    @RequestMapping(method = RequestMethod.GET)
    public List<User> getAll() { }
    
    // GET /api/users/{id}
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public User getById(@PathVariable Long id) { }
    
    // POST /api/users
    @RequestMapping(method = RequestMethod.POST)
    public User create(@RequestBody User user) { }
}
```

**Shorthand annotations (recommended):**
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping              // GET /api/users
    public List<User> getAll() { }
    
    @GetMapping("/{id}")     // GET /api/users/{id}
    public User getById(@PathVariable Long id) { }
    
    @PostMapping             // POST /api/users
    public User create(@RequestBody User user) { }
    
    @PutMapping("/{id}")     // PUT /api/users/{id}
    public User update(@PathVariable Long id, @RequestBody User user) { }
    
    @DeleteMapping("/{id}")  // DELETE /api/users/{id}
    public void delete(@PathVariable Long id) { }
}
```

---

## 16. What is @PathVariable and @RequestParam?

**@PathVariable:** Extracts value from URL path

```java
// GET /api/users/123
@GetMapping("/api/users/{id}")
public User getUser(@PathVariable Long id) {
    return userService.findById(id);
}
```

**@RequestParam:** Extracts query parameters

```java
// GET /api/users?name=John&age=30
@GetMapping("/api/users")
public List<User> searchUsers(
    @RequestParam String name,
    @RequestParam int age
) {
    return userService.search(name, age);
}

// Optional parameter
@GetMapping("/api/users")
public List<User> searchUsers(
    @RequestParam(required = false) String name
) {
    return userService.search(name);
}
```

---

## 17. What is @RequestBody and @ResponseBody?

**@RequestBody:** Converts incoming JSON to Java object

```java
@PostMapping("/api/users")
public User create(@RequestBody User user) {
    // JSON request body automatically converted to User object
    return userService.save(user);
}

// Request:
// POST /api/users
// Content-Type: application/json
// {"name": "John", "email": "john@example.com"}
```

**@ResponseBody:** Converts Java object to JSON response (automatic in @RestController)

```java
@RestController
public class UserController {
    @GetMapping("/api/users/{id}")
    public User getUser(@PathVariable Long id) {
        // Return object is automatically serialized to JSON
        return userService.findById(id);
    }
}
```

---

## 18. How does Spring handle exceptions?

**Method 1: @ExceptionHandler**

```java
@RestController
public class UserController {
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);  // Throws UserNotFoundException
    }
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("User not found", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

**Method 2: @ControllerAdvice (Global exception handler)**

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("User not found", ex.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("Server error", ex.getMessage()));
    }
}
```

---

## 19. What is AOP (Aspect-Oriented Programming)?

**Purpose:** Separate cross-cutting concerns (logging, security, transactions) from business logic.

```java
// Problem: Same logging code in multiple methods
@Service
public class UserService {
    public User createUser(User user) {
        System.out.println("Method: createUser called");
        // Business logic
        System.out.println("Method: createUser finished");
        return user;
    }
}

// Solution: AOP
@Aspect
@Component
public class LoggingAspect {
    
    @Before("execution(* com.example.service.*.*(..))")
    public void logBefore(JoinPoint jp) {
        System.out.println("Method: " + jp.getSignature() + " called");
    }
    
    @After("execution(* com.example.service.*.*(..))")
    public void logAfter(JoinPoint jp) {
        System.out.println("Method: " + jp.getSignature() + " finished");
    }
}

// Result: Logging automatically applied to all methods without code duplication
```

**Common AOP use cases:**
- Logging
- Transaction management
- Security checks
- Performance monitoring
- Caching

---

## 20. What is @Transactional?

**Purpose:** Manage database transactions declaratively.

```java
@Service
public class UserService {
    
    @Transactional
    public User createUser(User user) {
        // If any exception occurs, entire transaction rolls back
        User savedUser = userRepository.save(user);
        notificationService.sendEmail(savedUser);  // May throw exception
        return savedUser;
    }
    
    @Transactional(readOnly = true)
    public User getUser(Long id) {
        // Read-only transaction (optimization hint)
        return userRepository.findById(id).orElse(null);
    }
    
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void complexOperation() {
        // Fine-grained transaction control
    }
}
```

**Transaction propagation:**
- `REQUIRED`: Use existing or create new (default)
- `REQUIRES_NEW`: Always create new transaction
- `SUPPORTS`: Use existing or non-transactional

---

## 21. What is Spring Data JPA?

**Purpose:** Simplify database access using Java objects instead of SQL.

```java
// Traditional JDBC: Write SQL manually
Connection conn = DriverManager.getConnection("jdbc:mysql://...");
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE id = 1");

// Spring Data JPA: Work with objects
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findById(Long id);
    List<User> findByName(String name);
    List<User> findByAgeBetween(int minAge, int maxAge);
}

// Usage:
@Service
public class UserService {
    @Autowired
    private UserRepository repo;
    
    public User getUser(Long id) {
        return repo.findById(id).orElse(null);
    }
}
```

**Key features:**
- No need to write implementation
- Automatic query generation
- Support for custom queries
- Pagination and sorting

---

## 22. What is the difference between JPA and Hibernate?

| Feature | JPA | Hibernate |
|---------|-----|-----------|
| **Type** | Specification/Standard | Implementation |
| **Purpose** | Defines ORM contract | Implements ORM |
| **Provider** | Java EE (umbrella) | Third-party library |
| **Relationship** | Hibernate implements JPA | JPA is interface, Hibernate is concrete class |

**Analogy:** JPA is like an interface, Hibernate is like the implementing class.

```java
// JPA annotations (standard)
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue
    private Long id;
    
    @Column(nullable = false)
    private String name;
}

// Hibernate-specific annotations (extended functionality)
@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted = true WHERE id = ?")  // Soft delete
public class User {
    @Id
    private Long id;
}
```

---

## 23. What is lazy loading and eager loading?

**Concept:** How related objects are loaded from database.

**Eager Loading:** Load everything immediately
```java
@Entity
public class User {
    @Id
    private Long id;
    
    @OneToMany(fetch = FetchType.EAGER)  // Load all posts immediately
    private List<Post> posts;
}

// Result: Single query with JOIN
// SELECT u.*, p.* FROM users u 
// LEFT JOIN posts p ON u.id = p.user_id
```

**Lazy Loading:** Load only when accessed (default)
```java
@Entity
public class User {
    @Id
    private Long id;
    
    @OneToMany(fetch = FetchType.LAZY)  // Load posts only when accessed
    private List<Post> posts;
}

// Result: Two queries
// Query 1: SELECT * FROM users WHERE id = 1
// Query 2: SELECT * FROM posts WHERE user_id = 1 (when user.getPosts() is called)
```

**Best practice:** Use LAZY for better performance, EAGER only when needed.

---

## 24. What is N+1 problem in JPA?

**Problem:** Loading parent entities causes N additional queries for child entities.

```java
// Problem code
List<User> users = userRepository.findAll();  // Query 1

for (User user : users) {
    System.out.println(user.getPosts());  // Queries 2, 3, 4... (N queries)
}
```

**Solution 1: Use EAGER loading**
```java
@OneToMany(fetch = FetchType.EAGER)
private List<Post> posts;
```

**Solution 2: Use JOIN FETCH in JPQL**
```java
@Query("SELECT u FROM User u JOIN FETCH u.posts")
List<User> findAllWithPosts();
```

**Solution 3: Use EntityGraph**
```java
@EntityGraph(attributePaths = "posts")
List<User> findAll();
```

---

## 25. What is Spring MVC request flow?

**Request flow:**
1. **DispatcherServlet** receives HTTP request
2. **HandlerMapping** finds appropriate controller method
3. **Controller** processes request
4. **Model** holds data
5. **View** renders response

```
Request → DispatcherServlet → HandlerMapping → Controller 
         → Model → View → Response
```

**Example:**
```java
// 1. DispatcherServlet receives GET /users/1

// 2. HandlerMapping routes to this method
@RestController
@RequestMapping("/users")
public class UserController {
    @GetMapping("/{id}")  // 3. Controller
    public User getUser(@PathVariable Long id) {  // 4. Model
        return userService.findById(id);
    }
    // 5. View (JSON serialization)
    // 6. Response sent to client
}
```

---

## 26. What is the purpose of DispatcherServlet?

**Definition:** Central servlet that receives all HTTP requests and dispatches to appropriate handlers.

**Responsibilities:**
1. Receive all HTTP requests
2. Find appropriate controller (HandlerMapping)
3. Invoke controller method
4. Handle response rendering
5. Exception handling

**Configuration (automatic in SpringBoot):**
```java
// SpringBoot automatically configures DispatcherServlet
// No manual configuration needed!

// To customize (optional):
@Configuration
public class DispatcherConfig {
    @Bean
    public DispatcherServlet dispatcherServlet(ApplicationContext context) {
        DispatcherServlet servlet = new DispatcherServlet(context);
        servlet.setThrowExceptionIfNoHandlerFound(true);
        return servlet;
    }
}
```

---

## 27. What is Content Negotiation?

**Purpose:** Return different response formats (JSON/XML) based on client request.

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}

// Client decides format:
// GET /api/users/1 Accept: application/json
// Response: {"id": 1, "name": "John"} (JSON)

// GET /api/users/1 Accept: application/xml
// Response: <user><id>1</id><name>John</name></user> (XML)
```

**Configuration:**
```java
@Configuration
public class ContentNegotiationConfig implements WebMvcConfigurer {
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            .defaultContentTypeStrategy(new ContentNegotiationManager())
            .mediaType("json", MediaType.APPLICATION_JSON)
            .mediaType("xml", MediaType.APPLICATION_XML);
    }
}
```

---

## 28. What is REST and RESTful principles?

**REST (Representational State Transfer):** Architectural style using HTTP methods for operations.

**RESTful principles:**
```java
// 1. Use HTTP methods correctly
@GetMapping("/users/{id}")           // Retrieve
@PostMapping("/users")               // Create
@PutMapping("/users/{id}")           // Update
@DeleteMapping("/users/{id}")        // Delete

// 2. Use proper HTTP status codes
@PostMapping("/users")
public ResponseEntity<User> create(@RequestBody User user) {
    User saved = userService.save(user);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);  // 201
}

@GetMapping("/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.findById(id));  // 200
}

// 3. Use resource-based URLs (not action-based)
// ✓ Good: /api/users (resource-based)
// ✗ Bad: /api/getUsers or /api/createUser (action-based)

// 4. Stateless - each request is independent
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
    // No session/state dependency
    return userService.findById(id);
}
```

---

## 29. What is CORS and how to handle it in Spring?

**CORS (Cross-Origin Resource Sharing):** Allows browsers to make cross-origin requests.

**Problem:**
```javascript
// Frontend at http://localhost:3000
fetch('http://localhost:8080/api/users')  // Blocked by browser
```

**Solution 1: @CrossOrigin annotation**
```java
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {
    @GetMapping
    public List<User> getAll() {
        return userService.getAll();
    }
}
```

**Solution 2: Global CORS configuration**
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000", "https://example.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

---

## 30. What is Actuator in SpringBoot?

**Purpose:** Built-in monitoring and management endpoints.

**Enable Actuator:**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Configuration (application.properties):**
```properties
# Expose actuator endpoints
management.endpoints.web.exposure.include=*
management.endpoints.web.exposure.exclude=

# Or specific endpoints
management.endpoints.web.exposure.include=health,metrics,info
```

**Common endpoints:**

```bash
# Health check
GET /actuator/health
# Response: {"status": "UP"}

# Application metrics
GET /actuator/metrics
# Response: JVM, HTTP request metrics

# Custom application info
GET /actuator/info
# Response: Custom application info from properties

# Environment information
GET /actuator/env

# Threaddump
GET /actuator/threaddump
```

**Custom Actuator endpoint:**
```java
@Component
@Endpoint(id = "custom")
public class CustomEndpoint {
    
    @ReadOperation
    public Map<String, String> getCustomInfo() {
        return Map.of("status", "OK", "version", "1.0");
    }
}

// Access at: GET /actuator/custom
```

---

## Summary Table: Spring Core Features

| Feature | Purpose | Example |
|---------|---------|---------|
| **@Component/@Service** | Mark bean classes | `@Service public class UserService { }` |
| **@Autowired** | Inject dependencies | `@Autowired private UserRepo repo;` |
| **@Configuration** | Configure beans | `@Configuration @Bean UserService ...` |
| **@RequestMapping** | Map URLs to methods | `@GetMapping("/users")` |
| **@RequestBody** | Parse JSON to object | `@PostMapping public void create(@RequestBody User)` |
| **@PathVariable** | Extract URL param | `@GetMapping("/{id}") User getById(@PathVariable Long id)` |
| **@Transactional** | Manage transactions | `@Transactional public void save(User)` |
| **@ExceptionHandler** | Handle exceptions | `@ExceptionHandler(NotFoundException.class)` |
| **@Aspect** | AOP cross-cutting concerns | `@Aspect @Before("execution(...)")` |

---

## How They Fit Together

**Spring Framework** (Core):
- IoC Container + Dependency Injection
- The foundation for everything

**Spring Data** (Module):
- JPA/Hibernate integration
- Simplifies database access

**Spring Security** (Module):
- Authentication/Authorization
- Protects your app

**SpringBoot** (Enhancement):
- Makes Spring easier with auto-configuration
- Embedded servers (Tomcat)
- Starter dependencies
- Properties-based config

**SpringBatch** (Specialized Module):
- Batch job processing
- For ETL, scheduled data processing

**Spring Web/WebFlux** (Module):
- REST APIs and traditional web apps
- Real-time reactive apps

---

## Quick Architecture Diagram

```
┌─────────────────────────────────────────────────────┐
│           SpringBoot Application                     │
├─────────────────────────────────────────────────────┤
│                                                      │
│  ┌────────────────────────────────────────────┐    │
│  │  Web Layer (@RestController)               │    │
│  │  Handles HTTP requests & responses         │    │
│  └──────────────┬─────────────────────────────┘    │
│                 │                                   │
│  ┌──────────────▼─────────────────────────────┐    │
│  │  Service Layer (@Service)                  │    │
│  │  Business logic, @Transactional            │    │
│  └──────────────┬─────────────────────────────┘    │
│                 │                                   │
│  ┌──────────────▼─────────────────────────────┐    │
│  │  Repository Layer (@Repository)            │    │
│  │  Spring Data JPA, Database access          │    │
│  └──────────────┬─────────────────────────────┘    │
│                 │                                   │
│  ┌──────────────▼─────────────────────────────┐    │
│  │  Database                                  │    │
│  │  MySQL/PostgreSQL/Oracle                  │    │
│  └────────────────────────────────────────────┘    │
│                                                      │
│  ┌────────────────────────────────────────────┐    │
│  │  Cross-cutting concerns (AOP)              │    │
│  │  - Logging, Security, Transactions         │    │
│  └────────────────────────────────────────────┘    │
│                                                      │
│  ┌────────────────────────────────────────────┐    │
│  │  Spring IoC Container (Manages all beans)  │    │
│  └────────────────────────────────────────────┘    │
│                                                      │
└─────────────────────────────────────────────────────┘
```

