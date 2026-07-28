# All 23 Gang of Four Patterns - Real Business & Technical Scenarios

---

## 🏗️ CREATIONAL PATTERNS (5)

### 1️⃣ SINGLETON

**Purpose:** Ensure only ONE instance of a class exists globally.

#### Real Business Scenario: Logging System
**Context:** Your entire application (web server, background jobs, cron tasks) needs to write to ONE log file.

```python
class Logger:
    _instance = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._init_logger()
        return cls._instance
    
    def _init_logger(self):
        self.log_file = open('/var/log/app.log', 'a')
        self.lock = threading.Lock()
    
    def log(self, level, message):
        with self.lock:  # Thread-safe
            timestamp = datetime.now().isoformat()
            self.log_file.write(f"[{timestamp}] {level}: {message}\n")
            self.log_file.flush()

# Usage everywhere
logger = Logger()  # Always same instance
logger.log("INFO", "User logged in")
logger.log("ERROR", "Database connection failed")
```

**Business Impact:**
- ✅ All logs in ONE file → easy to debug
- ✅ No duplicate log entries
- ✅ Consistent formatting across app
- ✅ Thread-safe (important for web servers)

**Real-world Companies:** Django logging, Flask logging, Java Log4j

---

#### Technical Scenario: Database Connection Pool
**Context:** Creating database connections is expensive. Reuse one connection across entire app.

```java
public class DatabaseConnectionPool {
    private static DatabaseConnectionPool instance;
    private Connection connection;
    private static final Object lock = new Object();
    
    private DatabaseConnectionPool() {
        this.connection = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/mydb",
            "user",
            "password"
        );
    }
    
    public static DatabaseConnectionPool getInstance() {
        if (instance == null) {
            synchronized(lock) {
                if (instance == null) {
                    instance = new DatabaseConnectionPool();
                }
            }
        }
        return instance;
    }
    
    public Connection getConnection() {
        return this.connection;
    }
}

// Usage
DatabaseConnectionPool pool = DatabaseConnectionPool.getInstance();
Connection conn = pool.getConnection();
stmt = conn.createStatement();
```

**When Used:** Most enterprise applications need this. Database connections are expensive resources.

**⚠️ Warning:** Can become a bottleneck. Consider connection pools instead (which pool MULTIPLE connections under one manager).

---

### 2️⃣ FACTORY

**Already covered above** - See Factory-vs-AbstractFactory-Scenarios.md

---

### 3️⃣ ABSTRACT FACTORY

**Already covered above** - See Factory-vs-AbstractFactory-Scenarios.md

---

### 4️⃣ BUILDER

**Purpose:** Construct complex objects step-by-step with optional parameters.

#### Real Business Scenario: Email Construction
**Context:** Your email system sends different types of emails (welcome, password reset, newsletter) with different components.

```java
class Email {
    private String to;
    private String from;
    private String subject;
    private String body;
    private String htmlBody;
    private List<String> cc;
    private List<String> bcc;
    private List<Attachment> attachments;
    private String priority;
    private boolean trackOpens;
    private boolean trackClicks;
    private Map<String, String> customHeaders;
    
    // Private constructor - only builder creates instances
    private Email(EmailBuilder builder) {
        this.to = builder.to;
        this.from = builder.from;
        this.subject = builder.subject;
        this.body = builder.body;
        this.htmlBody = builder.htmlBody;
        this.cc = builder.cc;
        this.bcc = builder.bcc;
        this.attachments = builder.attachments;
        this.priority = builder.priority;
        this.trackOpens = builder.trackOpens;
        this.trackClicks = builder.trackClicks;
        this.customHeaders = builder.customHeaders;
    }
    
    // Builder inner class
    public static class EmailBuilder {
        private String to;
        private String from = "noreply@company.com";
        private String subject;
        private String body;
        private String htmlBody;
        private List<String> cc = new ArrayList<>();
        private List<String> bcc = new ArrayList<>();
        private List<Attachment> attachments = new ArrayList<>();
        private String priority = "normal";
        private boolean trackOpens = false;
        private boolean trackClicks = false;
        private Map<String, String> customHeaders = new HashMap<>();
        
        public EmailBuilder to(String to) {
            this.to = to;
            return this;
        }
        
        public EmailBuilder subject(String subject) {
            this.subject = subject;
            return this;
        }
        
        public EmailBuilder body(String body) {
            this.body = body;
            return this;
        }
        
        public EmailBuilder htmlBody(String htmlBody) {
            this.htmlBody = htmlBody;
            return this;
        }
        
        public EmailBuilder cc(String cc) {
            this.cc.add(cc);
            return this;
        }
        
        public EmailBuilder addAttachment(Attachment attachment) {
            this.attachments.add(attachment);
            return this;
        }
        
        public EmailBuilder highPriority() {
            this.priority = "high";
            return this;
        }
        
        public EmailBuilder trackEngagement() {
            this.trackOpens = true;
            this.trackClicks = true;
            return this;
        }
        
        public Email build() {
            if (to == null || subject == null || body == null) {
                throw new IllegalArgumentException("Missing required fields");
            }
            return new Email(this);
        }
    }
}

// Usage - Clean and readable!
Email welcomeEmail = new Email.EmailBuilder()
    .to("user@example.com")
    .subject("Welcome to our platform!")
    .htmlBody("<h1>Welcome!</h1><p>Start building...</p>")
    .cc("support@company.com")
    .trackEngagement()
    .build();

emailService.send(welcomeEmail);

// Different email with different options
Email urgentAlert = new Email.EmailBuilder()
    .to("admin@company.com")
    .subject("URGENT: Server Down")
    .body("Production server is down!")
    .highPriority()
    .build();

emailService.send(urgentAlert);
```

**Without Builder (Constructor Nightmare):**
```java
// Which parameter is which? Impossible to read!
new Email("user@example.com", "noreply@company.com", 
          "Welcome!", "<h1>Welcome</h1>", 
          null, null, null, false, false, true, 
          "normal", null);
```

**Business Impact:**
- ✅ Easy to create different email types
- ✅ Readable code
- ✅ Add new fields without breaking old code
- ✅ Validation in one place

---

#### Technical Scenario: SQL Query Construction
**Context:** Build complex SQL queries dynamically based on user filters.

```python
class QueryBuilder:
    def __init__(self):
        self.select_fields = ["*"]
        self.table = None
        self.where_conditions = []
        self.joins = []
        self.order_by = []
        self.limit_val = None
        self.offset_val = None
    
    def select(self, *fields):
        self.select_fields = list(fields)
        return self
    
    def from_table(self, table):
        self.table = table
        return self
    
    def where(self, condition):
        self.where_conditions.append(condition)
        return self
    
    def join(self, table, on):
        self.joins.append(f"JOIN {table} ON {on}")
        return self
    
    def order_by(self, field, direction="ASC"):
        self.order_by.append(f"{field} {direction}")
        return self
    
    def limit(self, count):
        self.limit_val = count
        return self
    
    def offset(self, count):
        self.offset_val = count
        return self
    
    def build(self):
        query = f"SELECT {', '.join(self.select_fields)} FROM {self.table}"
        
        if self.joins:
            query += " " + " ".join(self.joins)
        
        if self.where_conditions:
            query += " WHERE " + " AND ".join(self.where_conditions)
        
        if self.order_by:
            query += " ORDER BY " + ", ".join(self.order_by)
        
        if self.limit_val:
            query += f" LIMIT {self.limit_val}"
        
        if self.offset_val:
            query += f" OFFSET {self.offset_val}"
        
        return query

# Build queries dynamically based on user input
builder = QueryBuilder()
builder.select("id", "name", "email").from_table("users")

if user_search:
    builder.where(f"name LIKE '%{user_search}%'")

if active_only:
    builder.where("active = true")

builder.order_by("created_at", "DESC").limit(10)

query = builder.build()
# SELECT id, name, email FROM users WHERE name LIKE '...' AND active = true 
# ORDER BY created_at DESC LIMIT 10
```

**Used By:** Frameworks like Django ORM, SQLAlchemy, Hibernate

---

### 5️⃣ PROTOTYPE

**Purpose:** Create new objects by copying existing objects (cloning).

#### Real Business Scenario: Document Templates
**Context:** Users create documents from templates. Instead of creating from scratch, clone template and modify.

```javascript
class Document {
    constructor(title, content, metadata) {
        this.title = title;
        this.content = content;
        this.metadata = metadata; // {author, createdAt, tags, etc}
        this.id = Math.random();
        this.createdAt = new Date();
    }
    
    clone() {
        // Deep copy
        return new Document(
            this.title + " (Copy)",
            JSON.parse(JSON.stringify(this.content)),
            JSON.parse(JSON.stringify(this.metadata))
        );
    }
}

// Template document (prototype)
const reportTemplate = new Document(
    "Monthly Report",
    {
        sections: [
            { title: "Executive Summary", content: "[Fill in]" },
            { title: "Metrics", content: "[Fill in]" },
            { title: "Conclusion", content: "[Fill in]" }
        ]
    },
    { author: "Admin", template: true, tags: ["report", "monthly"] }
);

// User creates new document from template
const userDocument = reportTemplate.clone();
userDocument.title = "Monthly Report - July 2024";
userDocument.metadata.author = "John Doe";

// Original template unchanged
console.log(reportTemplate.title); // Still "Monthly Report"
```

**Business Impact:**
- ✅ Users start with pre-built structure
- ✅ Faster document creation
- ✅ Consistency (all monthly reports have same sections)
- ✅ Template improvements apply to future clones

---

#### Technical Scenario: Game Object Spawning
**Context:** Game has thousands of enemies. Clone an enemy prototype instead of instantiating from scratch.

```csharp
class Enemy : ICloneable
{
    public string Name { get; set; }
    public int Health { get; set; }
    public List<Weapon> Weapons { get; set; }
    public Texture Sprite { get; set; }
    
    public object Clone()
    {
        // Deep copy
        return new Enemy
        {
            Name = this.Name,
            Health = this.Health,
            Weapons = new List<Weapon>(this.Weapons),
            Sprite = this.Sprite // Reference is fine for immutable texture
        };
    }
}

// Prototype
Enemy goblinPrototype = new Enemy
{
    Name = "Goblin",
    Health = 20,
    Weapons = new List<Weapon> { new Weapon("Club") },
    Sprite = assetManager.LoadTexture("goblin.png")
};

// Spawn 100 goblins
for (int i = 0; i < 100; i++)
{
    Enemy goblin = (Enemy)goblinPrototype.Clone();
    goblin.Health = 20; // Reset for new instance
    spawnAtPosition(goblin, getRandomPosition());
}
```

**Performance Benefit:** Cloning is faster than loading from disk/memory repeatedly.

---

## 🏢 STRUCTURAL PATTERNS (7)

### 6️⃣ ADAPTER

**Already covered above** - See Factory-vs-AbstractFactory-Scenarios.md

---

### 7️⃣ DECORATOR

**Purpose:** Add behavior to objects dynamically without changing the original.

#### Real Business Scenario: Beverage Shop
**Context:** Coffee shop sells drinks. Customers add toppings (decorations) to their drink.

```python
from abc import ABC, abstractmethod

# Abstract component
class Beverage(ABC):
    @abstractmethod
    def cost(self):
        pass
    
    @abstractmethod
    def description(self):
        pass

# Concrete component
class Espresso(Beverage):
    def cost(self):
        return 2.99
    
    def description(self):
        return "Espresso"

# Decorators - each adds price and description
class BeverageDecorator(Beverage):
    def __init__(self, beverage):
        self.beverage = beverage

class Whipped(BeverageDecorator):
    def cost(self):
        return self.beverage.cost() + 0.50
    
    def description(self):
        return self.beverage.description() + ", Whipped Cream"

class Caramel(BeverageDecorator):
    def cost(self):
        return self.beverage.cost() + 0.75
    
    def description(self):
        return self.beverage.description() + ", Caramel"

class ExtraShot(BeverageDecorator):
    def cost(self):
        return self.beverage.cost() + 0.75
    
    def description(self):
        return self.beverage.description() + ", Extra Shot"

# Build drink step by step
drink = Espresso()  # $2.99
drink = ExtraShot(drink)  # $3.74
drink = Caramel(drink)  # $4.49
drink = Whipped(drink)  # $4.99

print(f"{drink.description()} = ${drink.cost()}")
# "Espresso, Extra Shot, Caramel, Whipped Cream = $4.99"
```

**Business Impact:**
- ✅ Easy to add new toppings without changing Beverage class
- ✅ Customers can customize in any order
- ✅ Price calculation automatic
- ✅ Scales to 100+ combinations without explosion

**Without Decorator (Class Explosion):**
```python
class EspressoWithWhippedAndCaramel(Beverage): pass
class EspressoWithWhippedAndCaramelAndExtra(Beverage): pass
class EspressoWithCaramelAndExtra(Beverage): pass
# ... 50+ more classes!
```

---

#### Technical Scenario: Logger with Multiple Features
**Context:** Add features to logger: encryption, compression, database backup.

```java
interface Logger {
    void log(String message);
}

class FileLogger implements Logger {
    public void log(String message) {
        FileWriter.write(message + "\n");
    }
}

abstract class LoggerDecorator implements Logger {
    protected Logger wrappedLogger;
    
    public LoggerDecorator(Logger logger) {
        this.wrappedLogger = logger;
    }
}

class EncryptedLogger extends LoggerDecorator {
    public void log(String message) {
        String encrypted = encrypt(message);
        wrappedLogger.log(encrypted);
    }
}

class CompressedLogger extends LoggerDecorator {
    public void log(String message) {
        String compressed = compress(message);
        wrappedLogger.log(compressed);
    }
}

class DatabaseBackupLogger extends LoggerDecorator {
    public void log(String message) {
        wrappedLogger.log(message);
        database.backup(message); // Extra feature
    }
}

// Compose features dynamically
Logger logger = new FileLogger();
logger = new EncryptedLogger(logger); // Add encryption
logger = new CompressedLogger(logger); // Add compression
logger = new DatabaseBackupLogger(logger); // Add backup

logger.log("User logged in"); // All features applied automatically
```

**Real Use:** Java's InputStream decorators (BufferedInputStream, GZIPInputStream, etc.)

---

### 8️⃣ FACADE

**Purpose:** Hide complexity behind a simple interface.

#### Real Business Scenario: Home Automation System
**Context:** Smart home has complex subsystems (lighting, temperature, security). User wants one simple "Good Night" button.

```python
# Complex subsystems
class Lighting:
    def turn_off(self):
        print("Lights off")

class Temperature:
    def set_temperature(self, temp):
        print(f"Temperature set to {temp}°C")

class SecuritySystem:
    def arm(self):
        print("Security system armed")
    
    def lock_doors(self):
        print("Doors locked")

class Entertainment:
    def turn_off_tv(self):
        print("TV off")
    
    def turn_off_stereo(self):
        print("Stereo off")

# Facade - simple interface hiding complexity
class HomeAutomationFacade:
    def __init__(self):
        self.lighting = Lighting()
        self.temperature = Temperature()
        self.security = SecuritySystem()
        self.entertainment = Entertainment()
    
    def good_night(self):
        """One simple method that does everything"""
        self.lighting.turn_off()
        self.temperature.set_temperature(18)  # Cooling for sleep
        self.entertainment.turn_off_tv()
        self.entertainment.turn_off_stereo()
        self.security.lock_doors()
        self.security.arm()
        print("Good night! Home secured.")

# User's simple interface
home = HomeAutomationFacade()
home.good_night()  # One call, everything happens
```

**Business Impact:**
- ✅ Customer doesn't need to know complex subsystems
- ✅ Add features internally without changing user interface
- ✅ Safe - facade validates before executing
- ✅ Easy to use - one method vs 7 method calls

---

#### Technical Scenario: Payment Processing Facade
**Context:** Different payment providers have different APIs. App wants one simple interface.

```java
// Complex payment providers
class StripeProcessor {
    public StripeResponse charge(String token, double amount) { ... }
}

class PayPalProcessor {
    public PayPalResponse processPayment(String account, double amount) { ... }
}

class SquareProcessor {
    public SquareResponse executeCharge(String cardId, double amount) { ... }
}

// Facade - simple, unified interface
class PaymentFacade {
    private StripeProcessor stripe = new StripeProcessor();
    private PayPalProcessor paypal = new PayPalProcessor();
    private SquareProcessor square = new SquareProcessor();
    
    public PaymentResult process(String provider, String paymentId, double amount) {
        try {
            Object result = null;
            
            switch(provider) {
                case "stripe":
                    result = stripe.charge(paymentId, amount);
                    break;
                case "paypal":
                    result = paypal.processPayment(paymentId, amount);
                    break;
                case "square":
                    result = square.executeCharge(paymentId, amount);
                    break;
            }
            
            // Normalize all responses
            return new PaymentResult(true, extractTransactionId(result));
        } catch (Exception e) {
            return new PaymentResult(false, e.getMessage());
        }
    }
}

// App code - simple!
PaymentFacade payment = new PaymentFacade();
PaymentResult result = payment.process("stripe", cardToken, 99.99);
```

**Used By:** Real-world payment wrappers (e.g., Stripe facade around 50+ payment methods)

---

### 9️⃣ PROXY

**Purpose:** Control access to another object (security, caching, lazy loading).

#### Real Business Scenario: Download Protection
**Context:** Large file download. Verify user permissions, log access, show progress.

```java
// Subject interface
interface FileService {
    byte[] downloadFile(String filename);
}

// Real subject
class RealFileService implements FileService {
    public byte[] downloadFile(String filename) {
        return Files.readAllBytes(Paths.get("/data/" + filename));
    }
}

// Proxy - controls access
class ProtectedFileServiceProxy implements FileService {
    private RealFileService realService;
    private User currentUser;
    private AuditLog auditLog;
    
    public ProtectedFileServiceProxy(User user) {
        this.currentUser = user;
        this.realService = new RealFileService();
        this.auditLog = AuditLog.getInstance();
    }
    
    public byte[] downloadFile(String filename) {
        // 1. Check permissions
        if (!currentUser.hasPermission("download_files")) {
            auditLog.log("DENIED: " + currentUser.getId() + " tried to download " + filename);
            throw new PermissionException("You don't have download rights");
        }
        
        // 2. Check file exists
        if (!authorizedFiles.contains(filename)) {
            auditLog.log("DENIED: Unauthorized file access - " + filename);
            throw new UnauthorizedException("File not available");
        }
        
        // 3. Log access
        auditLog.log("DOWNLOAD: " + currentUser.getId() + " downloaded " + filename);
        
        // 4. Actually download
        return realService.downloadFile(filename);
    }
}

// Usage
User user = authenticateUser(request);
FileService fileService = new ProtectedFileServiceProxy(user);
byte[] fileContent = fileService.downloadFile("report.pdf"); // Security handled
```

**Business Impact:**
- ✅ Access control in one place
- ✅ All downloads logged for compliance
- ✅ User can't bypass security
- ✅ Real service doesn't know about security

---

#### Technical Scenario: Lazy Loading with Cache
**Context:** Loading heavy database objects. Load only when needed, cache results.

```python
class ExpensiveDataObject:
    def __init__(self, object_id):
        self.object_id = object_id
        self.data = None
    
    def get_data(self):
        if self.data is None:
            print(f"Loading data for {self.object_id}...")
            # Expensive database query
            self.data = database.query(f"SELECT * FROM objects WHERE id = {self.object_id}")
        return self.data

class LazyLoadingProxy:
    def __init__(self, object_id):
        self.object_id = object_id
        self.real_object = None
        self.cache = {}
    
    def get_data(self):
        # Check cache first
        if self.object_id in self.cache:
            print("Returning cached data")
            return self.cache[self.object_id]
        
        # Lazy load on first access
        if self.real_object is None:
            self.real_object = ExpensiveDataObject(self.object_id)
        
        data = self.real_object.get_data()
        self.cache[self.object_id] = data
        return data

# Usage - transparent to caller
proxy = LazyLoadingProxy(123)
data1 = proxy.get_data()  # Loads from DB: "Loading data for 123..."
data2 = proxy.get_data()  # Returns cached: "Returning cached data"
```

---

### 🔟 BRIDGE

**Purpose:** Separate abstraction from implementation (they can change independently).

#### Real Business Scenario: Remote Control for Different Devices
**Context:** One remote interface works with TVs, Stereos, and Game Consoles (different implementations).

```java
// Implementation (device specifics)
interface Device {
    void on();
    void off();
    void setChannel(int channel);
    void setVolume(int volume);
}

class TV implements Device {
    public void on() { System.out.println("TV on"); }
    public void off() { System.out.println("TV off"); }
    public void setChannel(int channel) { System.out.println("TV channel: " + channel); }
    public void setVolume(int volume) { System.out.println("TV volume: " + volume); }
}

class Stereo implements Device {
    public void on() { System.out.println("Stereo on"); }
    public void off() { System.out.println("Stereo off"); }
    public void setChannel(int channel) { System.out.println("Stereo preset: " + channel); }
    public void setVolume(int volume) { System.out.println("Stereo volume: " + volume); }
}

// Abstraction (high-level control)
abstract class RemoteControl {
    protected Device device;
    
    public RemoteControl(Device device) {
        this.device = device;
    }
    
    public void turnOn() {
        device.on();
    }
    
    public void turnOff() {
        device.off();
    }
    
    public abstract void nextChannel();
    public abstract void volumeUp();
}

// Refined abstractions
class BasicRemote extends RemoteControl {
    public void nextChannel() {
        device.setChannel(currentChannel + 1);
    }
    
    public void volumeUp() {
        device.setVolume(currentVolume + 1);
    }
}

class SmartRemote extends RemoteControl {
    public void nextChannel() {
        device.setChannel(currentChannel + 1);
    }
    
    public void volumeUp() {
        device.setVolume(currentVolume + 5); // Smart remotes jump by 5
    }
    
    public void recordShow(String showName) {
        System.out.println("Recording: " + showName);
    }
}

// Usage - remote works with any device
Device tv = new TV();
RemoteControl remote = new SmartRemote(tv);
remote.turnOn();        // Works with TV
remote.nextChannel();   // Works with TV
remote.volumeUp();      // Works with TV

Device stereo = new Stereo();
remote = new SmartRemote(stereo); // Same remote, different device
remote.turnOn();        // Works with Stereo
remote.volumeUp();      // Works with Stereo
```

**Business Impact:**
- ✅ Add new device without changing remote
- ✅ Add new remote without changing devices
- ✅ Flexibility: combine any remote with any device

---

#### Technical Scenario: Graphics Rendering
**Context:** App renders graphics on Windows or Mac with DirectX or OpenGL.

```cpp
// Implementation
class Renderer {
    virtual void drawCircle(int x, int y, int radius) = 0;
    virtual void drawRectangle(int x, int y, int width, int height) = 0;
};

class DirectXRenderer : public Renderer {
    void drawCircle(int x, int y, int radius) override {
        // DirectX-specific code
        directXDevice->drawCircle(x, y, radius);
    }
};

class OpenGLRenderer : public Renderer {
    void drawCircle(int x, int y, int radius) override {
        // OpenGL-specific code
        glDrawCircle(x, y, radius);
    }
};

// Abstraction
class Shape {
protected:
    Renderer* renderer;
public:
    Shape(Renderer* r) : renderer(r) {}
    virtual void draw() = 0;
};

class Circle : public Shape {
private:
    int x, y, radius;
public:
    void draw() override {
        renderer->drawCircle(x, y, radius);
    }
};

// Usage
Renderer* renderer = new DirectXRenderer();
Circle circle = new Circle(renderer);
circle.draw(); // Uses DirectX
```

**Real Use:** Swing library in Java (different look & feel with same API)

---

### 1️⃣1️⃣ COMPOSITE

**Already covered above** - See Factory-vs-AbstractFactory-Scenarios.md

---

### 1️⃣2️⃣ FLYWEIGHT

**Already covered above** - See Factory-vs-AbstractFactory-Scenarios.md

---

## 🎯 BEHAVIORAL PATTERNS (11)

### 1️⃣3️⃣ OBSERVER

**Purpose:** Notify multiple listeners when something changes.

#### Real Business Scenario: Stock Price Alerts
**Context:** Users want email/SMS alerts when stock prices change.

```python
from abc import ABC, abstractmethod

# Observer interface
class StockPriceObserver(ABC):
    @abstractmethod
    def update(self, stock_symbol, new_price):
        pass

# Concrete observers
class EmailNotifier(StockPriceObserver):
    def update(self, stock_symbol, new_price):
        print(f"Email: {stock_symbol} is now ${new_price}")
        # send_email(user.email, f"{stock_symbol} alert: ${new_price}")

class SMSNotifier(StockPriceObserver):
    def update(self, stock_symbol, new_price):
        print(f"SMS: {stock_symbol} is now ${new_price}")
        # send_sms(user.phone, f"{stock_symbol} alert: ${new_price}")

class PushNotificationNotifier(StockPriceObserver):
    def update(self, stock_symbol, new_price):
        print(f"Push: {stock_symbol} is now ${new_price}")
        # send_push(user.device, f"{stock_symbol} alert: ${new_price}")

# Subject
class StockPrice:
    def __init__(self, symbol):
        self.symbol = symbol
        self._price = 0
        self._observers = []
    
    def attach(self, observer):
        """Subscribe to price changes"""
        self._observers.append(observer)
    
    def detach(self, observer):
        """Unsubscribe from price changes"""
        self._observers.remove(observer)
    
    def notify(self):
        """Notify all observers of price change"""
        for observer in self._observers:
            observer.update(self.symbol, self._price)
    
    @property
    def price(self):
        return self._price
    
    @price.setter
    def price(self, new_price):
        self._price = new_price
        self.notify()  # Automatically notify all observers

# Usage
apple_stock = StockPrice("AAPL")

# Users subscribe to alerts
email_notifier = EmailNotifier()
sms_notifier = SMSNotifier()
push_notifier = PushNotificationNotifier()

apple_stock.attach(email_notifier)
apple_stock.attach(sms_notifier)
apple_stock.attach(push_notifier)

# Price changes - all observers notified automatically
apple_stock.price = 150.00
# Output:
# Email: AAPL is now $150.0
# SMS: AAPL is now $150.0
# Push: AAPL is now $150.0
```

**Business Impact:**
- ✅ Add new notification types without changing stock code
- ✅ Users choose notification preferences
- ✅ Decoupled: Stock doesn't know about notifications
- ✅ Scalable: 1000s of observers possible

**Real Use:** YouTube subscriptions, email newsletters, event systems

---

#### Technical Scenario: React State Management
**Context:** Multiple UI components need to update when state changes.

```javascript
// Observer pattern in modern frameworks
const observable = {
    _state: { count: 0 },
    _observers: [],
    
    subscribe(callback) {
        this._observers.push(callback);
    },
    
    setState(newState) {
        this._state = { ...this._state, ...newState };
        this._observers.forEach(cb => cb(this._state));
    }
};

// Multiple components observing same state
observable.subscribe(state => {
    console.log("Counter display:", state.count);
});

observable.subscribe(state => {
    console.log("History:", state.count);
});

observable.subscribe(state => {
    updateAnalytics(state.count);
});

// One state change, all components update
observable.setState({ count: 1 });
```

---

### 1️⃣4️⃣ STRATEGY

**Already covered above** - See Factory-vs-AbstractFactory-Scenarios.md

---

### 1️⃣5️⃣ COMMAND

**Purpose:** Encapsulate requests as objects (undo/redo, queuing, logging).

#### Real Business Scenario: Text Editor Undo/Redo
**Context:** Users can undo/redo typing, formatting, deletions.

```java
interface Command {
    void execute();
    void undo();
}

class Document {
    private StringBuilder text = new StringBuilder();
    
    public void insert(String str, int position) {
        text.insert(position, str);
    }
    
    public void delete(int start, int end) {
        text.delete(start, end);
    }
}

// Concrete commands
class InsertTextCommand implements Command {
    private Document document;
    private String text;
    private int position;
    
    public InsertTextCommand(Document doc, String text, int pos) {
        this.document = doc;
        this.text = text;
        this.position = pos;
    }
    
    public void execute() {
        document.insert(text, position);
    }
    
    public void undo() {
        document.delete(position, position + text.length());
    }
}

class DeleteTextCommand implements Command {
    private Document document;
    private String deletedText;
    private int position;
    
    public DeleteTextCommand(Document doc, int start, int end) {
        this.document = doc;
        this.position = start;
        this.deletedText = doc.getText().substring(start, end);
    }
    
    public void execute() {
        document.delete(position, position + deletedText.length());
    }
    
    public void undo() {
        document.insert(deletedText, position);
    }
}

// Command history
class CommandHistory {
    private Stack<Command> undoStack = new Stack<>();
    private Stack<Command> redoStack = new Stack<>();
    
    public void execute(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear(); // Clear redo when new command executed
    }
    
    public void undo() {
        if (!undoStack.isEmpty()) {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);
        }
    }
    
    public void redo() {
        if (!redoStack.isEmpty()) {
            Command command = redoStack.pop();
            command.execute();
            undoStack.push(command);
        }
    }
}

// Usage
Document doc = new Document();
CommandHistory history = new CommandHistory();

// User types "Hello"
history.execute(new InsertTextCommand(doc, "Hello", 0));

// User types " World"
history.execute(new InsertTextCommand(doc, " World", 5));

// User hits Ctrl+Z (undo)
history.undo();  // Removes " World"

// User hits Ctrl+Y (redo)
history.redo();  // Adds " World" back
```

**Business Impact:**
- ✅ Unlimited undo/redo
- ✅ Easy to add new commands
- ✅ Commands can be logged/audited
- ✅ Remote commands (send command to server)

**Real Use:** Git commits, Photoshop history, document editing

---

#### Technical Scenario: Task Queue
**Context:** Queue tasks to execute later (async processing).

```python
from abc import ABC, abstractmethod
from queue import Queue
from threading import Thread

class Task(ABC):
    @abstractmethod
    def execute(self):
        pass

class EmailTask(Task):
    def __init__(self, recipient, message):
        self.recipient = recipient
        self.message = message
    
    def execute(self):
        print(f"Sending email to {self.recipient}: {self.message}")

class DataProcessingTask(Task):
    def __init__(self, data):
        self.data = data
    
    def execute(self):
        print(f"Processing: {self.data}")

class TaskQueue:
    def __init__(self, num_workers=4):
        self.queue = Queue()
        
        # Start worker threads
        for _ in range(num_workers):
            Thread(target=self._worker, daemon=True).start()
    
    def _worker(self):
        while True:
            task = self.queue.get()
            task.execute()
            self.queue.task_done()
    
    def enqueue(self, task):
        self.queue.put(task)

# Usage
task_queue = TaskQueue()
task_queue.enqueue(EmailTask("user@example.com", "Welcome!"))
task_queue.enqueue(DataProcessingTask([1, 2, 3, 4, 5]))
```

---

### 1️⃣6️⃣ STATE

**Purpose:** Change behavior based on internal state.

#### Real Business Scenario: Order Processing
**Context:** Orders have states: Pending → Processing → Shipped → Delivered. Different actions allowed per state.

```java
interface OrderState {
    void process(Order order);
    void ship(Order order);
    void deliver(Order order);
    void cancel(Order order);
}

class PendingState implements OrderState {
    public void process(Order order) {
        System.out.println("Order is being processed...");
        order.setState(new ProcessingState());
    }
    
    public void ship(Order order) {
        throw new IllegalStateException("Can't ship pending order");
    }
    
    public void deliver(Order order) {
        throw new IllegalStateException("Order not shipped yet");
    }
    
    public void cancel(Order order) {
        System.out.println("Order cancelled");
        order.setState(new CancelledState());
    }
}

class ProcessingState implements OrderState {
    public void process(Order order) {
        throw new IllegalStateException("Order already processing");
    }
    
    public void ship(Order order) {
        System.out.println("Order shipped!");
        order.setState(new ShippedState());
    }
    
    public void deliver(Order order) {
        throw new IllegalStateException("Order not shipped yet");
    }
    
    public void cancel(Order order) {
        throw new IllegalStateException("Can't cancel processing order");
    }
}

class ShippedState implements OrderState {
    public void process(Order order) {
        throw new IllegalStateException("Order already shipped");
    }
    
    public void ship(Order order) {
        throw new IllegalStateException("Order already shipped");
    }
    
    public void deliver(Order order) {
        System.out.println("Order delivered!");
        order.setState(new DeliveredState());
    }
    
    public void cancel(Order order) {
        throw new IllegalStateException("Can't cancel shipped order");
    }
}

class Order {
    private OrderState state;
    private String orderId;
    
    public Order(String orderId) {
        this.orderId = orderId;
        this.state = new PendingState();
    }
    
    public void setState(OrderState state) {
        this.state = state;
    }
    
    public void process() { state.process(this); }
    public void ship() { state.ship(this); }
    public void deliver() { state.deliver(this); }
    public void cancel() { state.cancel(this); }
}

// Usage
Order order = new Order("ORD-123");
order.process();    // OK - moves to Processing
order.ship();       // OK - moves to Shipped
order.deliver();    // OK - moves to Delivered
order.cancel();     // ERROR - can't cancel delivered order
```

**Business Impact:**
- ✅ Prevent invalid transitions
- ✅ Business logic enforced
- ✅ State-specific behavior encapsulated
- ✅ Easy to add new states

**Real Use:** Workflow engines, payment processing, document approval

---

#### Technical Scenario: TCP Connection States
**Context:** Network connection has states: Established, Listening, Closed. Different actions allowed per state.

```python
from abc import ABC, abstractmethod

class TCPState(ABC):
    @abstractmethod
    def open(self):
        pass
    
    @abstractmethod
    def close(self):
        pass
    
    @abstractmethod
    def send(self, data):
        pass

class TCPClosed(TCPState):
    def open(self):
        print("Establishing connection...")
        return TCPEstablished()
    
    def close(self):
        raise Exception("Connection already closed")
    
    def send(self, data):
        raise Exception("Connection not established")

class TCPEstablished(TCPState):
    def open(self):
        raise Exception("Connection already established")
    
    def close(self):
        print("Closing connection...")
        return TCPClosed()
    
    def send(self, data):
        print(f"Sending: {data}")

class TCPConnection:
    def __init__(self):
        self.state = TCPClosed()
    
    def open(self):
        self.state = self.state.open()
    
    def close(self):
        self.state = self.state.close()
    
    def send(self, data):
        self.state.send(data)
```

---

### 1️⃣7️⃣ TEMPLATE METHOD

**Purpose:** Define algorithm skeleton, let subclasses vary steps.

#### Real Business Scenario: Food Delivery Service
**Context:** Different restaurants have different cooking steps, but process is similar: order → prepare → package → deliver.

```java
abstract class RestaurantPreparer {
    // Template method - final so subclasses can't change structure
    public final void prepareOrder() {
        receiveOrder();
        prepareFood();  // Varies per restaurant
        packageFood();  // Varies per restaurant
        notifyDelivery();
    }
    
    // Concrete steps (same for all restaurants)
    private void receiveOrder() {
        System.out.println("Order received and confirmed");
    }
    
    private void notifyDelivery() {
        System.out.println("Notifying delivery partner...");
    }
    
    // Abstract steps (varies per restaurant)
    protected abstract void prepareFood();
    protected abstract void packageFood();
}

class ItalianRestaurant extends RestaurantPreparer {
    protected void prepareFood() {
        System.out.println("Preparing pasta with marinara sauce");
        System.out.println("Cooking for 10 minutes");
    }
    
    protected void packageFood() {
        System.out.println("Packaging in thermal container");
        System.out.println("Adding Italian herbs");
    }
}

class SushiRestaurant extends RestaurantPreparer {
    protected void prepareFood() {
        System.out.println("Preparing sushi rolls");
        System.out.println("Slicing fresh fish");
        System.out.println("Rolling with rice and nori");
    }
    
    protected void packageFood() {
        System.out.println("Packaging in refrigerated container");
        System.out.println("Adding wasabi and soy sauce");
    }
}

// Usage
RestaurantPreparer italian = new ItalianRestaurant();
italian.prepareOrder();
// Output:
// Order received and confirmed
// Preparing pasta with marinara sauce
// Cooking for 10 minutes
// Packaging in thermal container
// Adding Italian herbs
// Notifying delivery partner...

RestaurantPreparer sushi = new SushiRestaurant();
sushi.prepareOrder();
// Output:
// Order received and confirmed
// Preparing sushi rolls
// Slicing fresh fish
// Rolling with rice and nori
// Packaging in refrigerated container
// Adding wasabi and soy sauce
// Notifying delivery partner...
```

**Business Impact:**
- ✅ Process consistency
- ✅ Each restaurant implements only their variations
- ✅ Add new restaurant type easily
- ✅ Common steps in one place

---

#### Technical Scenario: Data Processing Pipeline
**Context:** Different data sources (CSV, JSON, Database) but processing steps are same: read → validate → transform → save.

```python
from abc import ABC, abstractmethod

class DataProcessor(ABC):
    def process(self, source):
        """Template method - defines algorithm structure"""
        data = self.read_data(source)
        data = self.validate_data(data)
        data = self.transform_data(data)
        self.save_data(data)
        print("Processing complete!\n")
    
    @abstractmethod
    def read_data(self, source):
        pass
    
    @abstractmethod
    def validate_data(self, data):
        pass
    
    @abstractmethod
    def transform_data(self, data):
        pass
    
    @abstractmethod
    def save_data(self, data):
        pass

class CSVProcessor(DataProcessor):
    def read_data(self, source):
        print("Reading CSV file...")
        return [["John", 30], ["Jane", 25]]
    
    def validate_data(self, data):
        print("Validating CSV data...")
        return data
    
    def transform_data(self, data):
        print("Transforming to objects...")
        return [{"name": row[0], "age": row[1]} for row in data]
    
    def save_data(self, data):
        print("Saving to database...")

class JSONProcessor(DataProcessor):
    def read_data(self, source):
        print("Reading JSON file...")
        return [{"name": "John", "age": 30}]
    
    def validate_data(self, data):
        print("Validating JSON data...")
        return data
    
    def transform_data(self, data):
        print("Transforming JSON...")
        return data
    
    def save_data(self, data):
        print("Saving to database...")

# Usage
csv_processor = CSVProcessor()
csv_processor.process("data.csv")

json_processor = JSONProcessor()
json_processor.process("data.json")
```

---

### 1️⃣8️⃣ CHAIN OF RESPONSIBILITY

**Purpose:** Pass requests through chain of handlers until someone handles it.

#### Real Business Scenario: Support Ticket Escalation
**Context:** Support tickets escalated: Level 1 → Level 2 → Level 3 → Manager.

```java
interface SupportHandler {
    void handleTicket(SupportTicket ticket);
    void setNext(SupportHandler next);
}

class Level1Support implements SupportHandler {
    private SupportHandler nextHandler;
    
    public void setNext(SupportHandler next) {
        this.nextHandler = next;
    }
    
    public void handleTicket(SupportTicket ticket) {
        if (ticket.severity <= 2) {
            System.out.println("Level 1: Handling ticket - " + ticket.issue);
            System.out.println("Sending FAQ and basic troubleshooting");
        } else {
            System.out.println("Level 1: Escalating to Level 2");
            nextHandler.handleTicket(ticket);
        }
    }
}

class Level2Support implements SupportHandler {
    private SupportHandler nextHandler;
    
    public void setNext(SupportHandler next) {
        this.nextHandler = next;
    }
    
    public void handleTicket(SupportTicket ticket) {
        if (ticket.severity <= 5) {
            System.out.println("Level 2: Technical investigation of - " + ticket.issue);
            System.out.println("Offering remote assistance");
        } else {
            System.out.println("Level 2: Escalating to manager");
            nextHandler.handleTicket(ticket);
        }
    }
}

class Manager implements SupportHandler {
    public void setNext(SupportHandler next) {
        // Manager is last in chain
    }
    
    public void handleTicket(SupportTicket ticket) {
        System.out.println("Manager: Handling critical issue - " + ticket.issue);
        System.out.println("Offering dedicated support");
    }
}

class SupportTicket {
    String issue;
    int severity; // 1-10
    
    public SupportTicket(String issue, int severity) {
        this.issue = issue;
        this.severity = severity;
    }
}

// Setup chain
Level1Support level1 = new Level1Support();
Level2Support level2 = new Level2Support();
Manager manager = new Manager();

level1.setNext(level2);
level2.setNext(manager);

// Process tickets
level1.handleTicket(new SupportTicket("Can't login", 1));
level1.handleTicket(new SupportTicket("Database error", 7));
level1.handleTicket(new SupportTicket("System down", 10));
```

**Business Impact:**
- ✅ Automatic escalation
- ✅ Support team efficiency
- ✅ Proper resource allocation
- ✅ Easy to add new handler levels

**Real Use:** Approval workflows, logging frameworks, event handling

---

#### Technical Scenario: Exception Handling
**Context:** Try to handle exception at different levels.

```python
class ExceptionHandler:
    def __init__(self):
        self.next_handler = None
    
    def set_next(self, handler):
        self.next_handler = handler
        return handler
    
    def handle(self, error):
        if self.can_handle(error):
            self.process(error)
        elif self.next_handler:
            self.next_handler.handle(error)

class ConnectionErrorHandler(ExceptionHandler):
    def can_handle(self, error):
        return isinstance(error, ConnectionError)
    
    def process(self, error):
        print("Retrying connection...")

class TimeoutErrorHandler(ExceptionHandler):
    def can_handle(self, error):
        return isinstance(error, TimeoutError)
    
    def process(self, error):
        print("Increasing timeout...")

class GenericErrorHandler(ExceptionHandler):
    def can_handle(self, error):
        return True  # Handle anything
    
    def process(self, error):
        print("Logging error and notifying admin")

# Setup chain
chain = ConnectionErrorHandler()
chain.set_next(TimeoutErrorHandler()).set_next(GenericErrorHandler())

# Handle errors
chain.handle(ConnectionError("Network unreachable"))
chain.handle(TimeoutError("Request timeout"))
chain.handle(ValueError("Invalid input"))
```

---

### 1️⃣9️⃣ ITERATOR

**Purpose:** Access collection elements without exposing internal structure.

#### Real Business Scenario: Playlist Navigation
**Context:** Playlist can be stored as array, linked list, or database. Users just navigate.

```java
interface Iterator<T> {
    boolean hasNext();
    T next();
}

interface Playlist {
    Iterator<Song> iterator();
}

class LinkedListPlaylist implements Playlist {
    private LinkedList<Song> songs = new LinkedList<>();
    
    public void addSong(Song song) {
        songs.add(song);
    }
    
    public Iterator<Song> iterator() {
        return new LinkedListIterator(songs);
    }
    
    private class LinkedListIterator implements Iterator<Song> {
        private LinkedList<Song> list;
        private Node current;
        
        public LinkedListIterator(LinkedList<Song> list) {
            this.list = list;
            this.current = list.head;
        }
        
        public boolean hasNext() {
            return current != null;
        }
        
        public Song next() {
            Song song = current.data;
            current = current.next;
            return song;
        }
    }
}

class ArrayPlaylist implements Playlist {
    private Song[] songs = new Song[100];
    private int size = 0;
    
    public void addSong(Song song) {
        songs[size++] = song;
    }
    
    public Iterator<Song> iterator() {
        return new ArrayIterator(songs, size);
    }
    
    private class ArrayIterator implements Iterator<Song> {
        private Song[] array;
        private int size;
        private int index = 0;
        
        public ArrayIterator(Song[] array, int size) {
            this.array = array;
            this.size = size;
        }
        
        public boolean hasNext() {
            return index < size;
        }
        
        public Song next() {
            return array[index++];
        }
    }
}

// Usage - same code works for both implementations!
Playlist playlist = new LinkedListPlaylist(); // or ArrayPlaylist()
playlist.addSong(new Song("Song 1"));
playlist.addSong(new Song("Song 2"));

Iterator<Song> iterator = playlist.iterator();
while (iterator.hasNext()) {
    System.out.println(iterator.next());
}
```

**Business Impact:**
- ✅ Switch storage format without changing user code
- ✅ Iterate efficiently
- ✅ Encapsulation maintained

**Real Use:** Java collections (ArrayList, LinkedList, Set), Python iterators

---

#### Technical Scenario: Directory Traversal
**Context:** Iterate through file system (files and folders) without exposing structure.

```python
from abc import ABC, abstractmethod

class FileIterator(ABC):
    @abstractmethod
    def has_next(self):
        pass
    
    @abstractmethod
    def next(self):
        pass

class DirectoryIterator:
    def __init__(self, directory):
        self.directory = directory
        self.files = []
        self.index = 0
        
        for item in directory.list_contents():
            self.files.append(item)
    
    def has_next(self):
        return self.index < len(self.files)
    
    def next(self):
        file = self.files[self.index]
        self.index += 1
        return file

class Directory:
    def __init__(self, name):
        self.name = name
        self.contents = []
    
    def add_file(self, file):
        self.contents.append(file)
    
    def list_contents(self):
        return self.contents
    
    def get_iterator(self):
        return DirectoryIterator(self)

# Usage
root = Directory("root")
root.add_file("file1.txt")
root.add_file("file2.txt")
root.add_file("subfolder")

iterator = root.get_iterator()
while iterator.has_next():
    print(iterator.next())
```

---

### 2️⃣0️⃣ MEDIATOR

**Purpose:** Central hub for communication between objects.

#### Real Business Scenario: Chat Room
**Context:** Multiple users chat. Instead of user-to-user communication, use central mediator.

```java
interface ChatMediator {
    void sendMessage(String message, User sender);
    void registerUser(User user);
}

class ChatRoom implements ChatMediator {
    private List<User> users = new ArrayList<>();
    
    public void registerUser(User user) {
        users.add(user);
        user.setChatMediator(this);
    }
    
    public void sendMessage(String message, User sender) {
        System.out.println(sender.getName() + " sends: " + message);
        
        // Notify all other users
        for (User user : users) {
            if (user != sender) {
                user.receive(message, sender);
            }
        }
    }
}

class User {
    private String name;
    private ChatMediator mediator;
    
    public User(String name) {
        this.name = name;
    }
    
    public void setChatMediator(ChatMediator mediator) {
        this.mediator = mediator;
    }
    
    public void send(String message) {
        mediator.sendMessage(message, this);
    }
    
    public void receive(String message, User sender) {
        System.out.println(name + " receives from " + sender.getName() + ": " + message);
    }
    
    public String getName() {
        return name;
    }
}

// Usage
ChatMediator chatRoom = new ChatRoom();
User alice = new User("Alice");
User bob = new User("Bob");
User charlie = new User("Charlie");

chatRoom.registerUser(alice);
chatRoom.registerUser(bob);
chatRoom.registerUser(charlie);

alice.send("Hello everyone!");
bob.send("Hi Alice!");
```

**Business Impact:**
- ✅ Reduces direct dependencies
- ✅ Centralized communication logic
- ✅ Easy to monitor/audit
- ✅ Add features (filtering, moderation) in one place

**Real Use:** Slack, Discord, collaboration platforms

---

#### Technical Scenario: Air Traffic Control
**Context:** Planes communicate through control tower, not directly with each other.

```python
class ControlTower:
    def __init__(self):
        self.aircraft = []
    
    def register_aircraft(self, aircraft):
        self.aircraft.append(aircraft)
        aircraft.set_tower(self)
    
    def request_landing(self, aircraft):
        print(f"Tower: {aircraft.name} requesting landing")
        
        # Check if runway is available
        if self.is_runway_available():
            print(f"Tower: {aircraft.name}, you are cleared to land")
            aircraft.land()
        else:
            print(f"Tower: {aircraft.name}, circle and wait")
            aircraft.circle()
    
    def is_runway_available(self):
        # Complex logic to check runway availability
        return True

class Aircraft:
    def __init__(self, name):
        self.name = name
        self.tower = None
    
    def set_tower(self, tower):
        self.tower = tower
    
    def request_landing(self):
        self.tower.request_landing(self)
    
    def land(self):
        print(f"{self.name}: Landing...")
    
    def circle(self):
        print(f"{self.name}: Circling...")

# Usage
tower = ControlTower()
flight1 = Aircraft("Flight 101")
flight2 = Aircraft("Flight 202")

tower.register_aircraft(flight1)
tower.register_aircraft(flight2)

flight1.request_landing()
flight2.request_landing()
```

---

### 2️⃣1️⃣ MEMENTO

**Purpose:** Save and restore object state without breaking encapsulation.

#### Real Business Scenario: Game Save System
**Context:** Players save game progress at checkpoints, can load and continue.

```python
class GameState:
    """Memento - captures game state"""
    def __init__(self, level, health, position, inventory):
        self.level = level
        self.health = health
        self.position = position
        self.inventory = inventory.copy()

class Game:
    """Originator - object whose state we're saving"""
    def __init__(self):
        self.level = 1
        self.health = 100
        self.position = (0, 0)
        self.inventory = []
    
    def save(self):
        """Create memento"""
        return GameState(self.level, self.health, self.position, self.inventory)
    
    def restore(self, memento):
        """Restore from memento"""
        self.level = memento.level
        self.health = memento.health
        self.position = memento.position
        self.inventory = memento.inventory.copy()
    
    def play(self, action):
        """Player performs action"""
        if action == "advance":
            self.level += 1
            self.position = (self.position[0] + 10, self.position[1])
        elif action == "collect":
            self.inventory.append("item")
        elif action == "damage":
            self.health -= 25

class GameSaveManager:
    """Caretaker - manages mementos"""
    def __init__(self):
        self.saves = {}
    
    def save_game(self, name, game):
        self.saves[name] = game.save()
        print(f"Saved: {name}")
    
    def load_game(self, name, game):
        if name in self.saves:
            game.restore(self.saves[name])
            print(f"Loaded: {name}")
        else:
            print(f"Save not found: {name}")

# Usage
game = Game()
save_manager = GameSaveManager()

# Play
game.play("advance")
game.play("collect")
game.play("advance")
print(f"Level: {game.level}, Health: {game.health}")  # Level: 3, Health: 100

# Take damage
game.play("damage")
print(f"Level: {game.level}, Health: {game.health}")  # Level: 3, Health: 75

# Save before risky area
save_manager.save_game("checkpoint", game)

# Play risky
game.play("damage")
game.play("damage")
print(f"Level: {game.level}, Health: {game.health}")  # Level: 3, Health: 25

# Restore from save
save_manager.load_game("checkpoint", game)
print(f"Level: {game.level}, Health: {game.health}")  # Level: 3, Health: 75
```

**Business Impact:**
- ✅ Undo/Redo functionality
- ✅ Game checkpoints
- ✅ Version control
- ✅ Transaction rollback in databases

---

#### Technical Scenario: Database Transaction Rollback
**Context:** Database operations with automatic rollback on error.

```python
class DatabaseSnapshot:
    """Memento"""
    def __init__(self, data):
        self.data = data.copy()

class Database:
    """Originator"""
    def __init__(self):
        self.data = {}
    
    def create_snapshot(self):
        """Create memento"""
        return DatabaseSnapshot(self.data)
    
    def restore_snapshot(self, snapshot):
        """Restore from memento"""
        self.data = snapshot.data.copy()
    
    def insert(self, key, value):
        self.data[key] = value
    
    def update(self, key, value):
        if key in self.data:
            self.data[key] = value

class Transaction:
    """Caretaker"""
    def __init__(self, database):
        self.database = database
        self.snapshot = None
    
    def begin(self):
        self.snapshot = self.database.create_snapshot()
    
    def commit(self):
        self.snapshot = None  # Keep changes
        print("Transaction committed")
    
    def rollback(self):
        self.database.restore_snapshot(self.snapshot)
        print("Transaction rolled back")

# Usage
db = Database()
txn = Transaction(db)

txn.begin()
db.insert("user1", "John")
db.insert("user2", "Jane")

try:
    if error_occurs():
        txn.rollback()
    else:
        txn.commit()
except:
    txn.rollback()
```

---

### 2️⃣2️⃣ VISITOR

**Purpose:** Add operations to objects without changing the objects.

#### Real Business Scenario: File System Operations
**Context:** Files and folders need different operations: calculate size, scan for viruses, compress.

```java
interface FileSystemElement {
    void accept(Visitor visitor);
}

class File implements FileSystemElement {
    private String name;
    private long size;
    
    public File(String name, long size) {
        this.name = name;
        this.size = size;
    }
    
    public String getName() { return name; }
    public long getSize() { return size; }
    
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}

class Folder implements FileSystemElement {
    private String name;
    private List<FileSystemElement> contents = new ArrayList<>();
    
    public Folder(String name) {
        this.name = name;
    }
    
    public void add(FileSystemElement element) {
        contents.add(element);
    }
    
    public String getName() { return name; }
    public List<FileSystemElement> getContents() { return contents; }
    
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}

// Visitor interface
interface Visitor {
    void visit(File file);
    void visit(Folder folder);
}

// Concrete visitors
class SizeCalculator implements Visitor {
    private long totalSize = 0;
    
    public void visit(File file) {
        totalSize += file.getSize();
    }
    
    public void visit(Folder folder) {
        System.out.println("Calculating size of folder: " + folder.getName());
        for (FileSystemElement element : folder.getContents()) {
            element.accept(this);  // Recursive visit
        }
    }
    
    public long getTotalSize() {
        return totalSize;
    }
}

class VirusScanner implements Visitor {
    public void visit(File file) {
        System.out.println("Scanning file: " + file.getName());
        // Scan logic
    }
    
    public void visit(Folder folder) {
        System.out.println("Scanning folder: " + folder.getName());
        for (FileSystemElement element : folder.getContents()) {
            element.accept(this);
        }
    }
}

// Usage
Folder root = new Folder("root");
File file1 = new File("document.pdf", 1024);
File file2 = new File("image.jpg", 2048);
root.add(file1);
root.add(file2);

// Add operation without modifying File/Folder classes
SizeCalculator sizeCalc = new SizeCalculator();
root.accept(sizeCalc);
System.out.println("Total size: " + sizeCalc.getTotalSize()); // 3072

VirusScanner scanner = new VirusScanner();
root.accept(scanner);
```

**Business Impact:**
- ✅ Add operations without changing existing classes
- ✅ Keep File and Folder focused
- ✅ Easy to add new operations (compress, encrypt, etc.)
- ✅ Separation of concerns

**Real Use:** Compilers (AST traversal), UI rendering

---

#### Technical Scenario: Report Generation
**Context:** Generate different types of reports from same data structure.

```python
class ReportElement:
    def accept(self, visitor):
        pass

class ReportSection(ReportElement):
    def __init__(self, title):
        self.title = title
        self.content = ""
    
    def accept(self, visitor):
        visitor.visit_section(self)

class ReportChart(ReportElement):
    def __init__(self, chart_type, data):
        self.chart_type = chart_type
        self.data = data
    
    def accept(self, visitor):
        visitor.visit_chart(self)

class ReportGenerator:
    def visit_section(self, section):
        pass
    
    def visit_chart(self, chart):
        pass

class HTMLReportGenerator(ReportGenerator):
    def generate(self, elements):
        html = "<html><body>"
        for element in elements:
            element.accept(self)
        html += "</body></html>"
        return html
    
    def visit_section(self, section):
        print(f"<h2>{section.title}</h2>")
    
    def visit_chart(self, chart):
        print(f"<div class='chart'>{chart.chart_type}</div>")

class PDFReportGenerator(ReportGenerator):
    def generate(self, elements):
        pdf = ""
        for element in elements:
            element.accept(self)
        return pdf
    
    def visit_section(self, section):
        print(f"PDF Section: {section.title}")
    
    def visit_chart(self, chart):
        print(f"PDF Chart: {chart.chart_type}")

# Usage
report = [
    ReportSection("Executive Summary"),
    ReportChart("bar_chart", [10, 20, 30]),
    ReportSection("Recommendations")
]

html_gen = HTMLReportGenerator()
html_gen.generate(report)

pdf_gen = PDFReportGenerator()
pdf_gen.generate(report)
```

---

### 2️⃣3️⃣ INTERPRETER

**Purpose:** Parse and interpret expressions/sentences.

#### Real Business Scenario: SQL Query Interpreter
**Context:** Parse SQL queries and execute them.

```java
interface Expression {
    Result interpret();
}

class SelectExpression implements Expression {
    private List<String> fields;
    private String table;
    
    public SelectExpression(List<String> fields, String table) {
        this.fields = fields;
        this.table = table;
    }
    
    public Result interpret() {
        System.out.println("SELECT " + String.join(", ", fields) + " FROM " + table);
        return new Result(); // Execute query
    }
}

class WhereExpression implements Expression {
    private Expression previous;
    private String condition;
    
    public WhereExpression(Expression previous, String condition) {
        this.previous = previous;
        this.condition = condition;
    }
    
    public Result interpret() {
        Result result = previous.interpret();
        System.out.println("WHERE " + condition);
        // Filter result
        return result;
    }
}

class OrderByExpression implements Expression {
    private Expression previous;
    private String field;
    
    public OrderByExpression(Expression previous, String field) {
        this.previous = previous;
        this.field = field;
    }
    
    public Result interpret() {
        Result result = previous.interpret();
        System.out.println("ORDER BY " + field);
        // Sort result
        return result;
    }
}

// Parser
class SQLParser {
    public Expression parse(String query) {
        // Parse "SELECT id, name FROM users WHERE active ORDER BY name"
        
        Expression expr = new SelectExpression(
            Arrays.asList("id", "name"),
            "users"
        );
        
        expr = new WhereExpression(expr, "active = true");
        expr = new OrderByExpression(expr, "name");
        
        return expr;
    }
}

// Usage
SQLParser parser = new SQLParser();
Expression query = parser.parse("SELECT id, name FROM users WHERE active ORDER BY name");
Result result = query.interpret();
```

**Business Impact:**
- ✅ Parse custom languages
- ✅ Query builders
- ✅ DSL (Domain Specific Languages)

---

#### Technical Scenario: Mathematical Expression Parser
**Context:** Parse and evaluate math expressions like "2 + 3 * 4".

```python
class Expression:
    def evaluate(self):
        pass

class NumberExpression(Expression):
    def __init__(self, number):
        self.number = number
    
    def evaluate(self):
        return self.number

class AddExpression(Expression):
    def __init__(self, left, right):
        self.left = left
        self.right = right
    
    def evaluate(self):
        return self.left.evaluate() + self.right.evaluate()

class MultiplyExpression(Expression):
    def __init__(self, left, right):
        self.left = left
        self.right = right
    
    def evaluate(self):
        return self.left.evaluate() * self.right.evaluate()

class ExpressionParser:
    def parse(self, expression_string):
        # Parse "2 + 3 * 4"
        # Returns MultiplyExpression(AddExpression(...), ...)
        pass

# Usage
# 2 + 3 * 4
expr = AddExpression(
    NumberExpression(2),
    MultiplyExpression(
        NumberExpression(3),
        NumberExpression(4)
    )
)
result = expr.evaluate()  # 14
```

**Used By:** Compilers, calculators, rule engines

---

## 📊 All 23 Patterns Quick Reference

| # | Pattern | Type | Real Business Use | Quick Rule |
|---|---------|------|-------------------|-----------|
| 1 | Singleton | Creational | Logging system, DB connection | One instance globally |
| 2 | Factory | Creational | Payment providers, storage | Create single objects |
| 3 | Abstract Factory | Creational | UI themes, SaaS tiers | Create coordinated families |
| 4 | Builder | Creational | Email construction, query building | Build step-by-step with options |
| 5 | Prototype | Creational | Document templates, game enemies | Clone and modify |
| 6 | Adapter | Structural | Connect payment systems, legacy code | Make incompatible things work |
| 7 | Decorator | Structural | Beverage shop, logging features | Add features dynamically |
| 8 | Facade | Structural | Home automation, payment processing | Hide complexity |
| 9 | Proxy | Structural | Download protection, lazy loading | Control access with middleware |
| 10 | Bridge | Structural | Remote controls, graphics rendering | Separate abstraction from implementation |
| 11 | Composite | Structural | File system, org charts | Treat items and groups uniformly |
| 12 | Flyweight | Structural | Game sprites, terrain tiles | Share data across many objects |
| 13 | Observer | Behavioral | Stock alerts, email subscriptions | Notify multiple listeners |
| 14 | Strategy | Behavioral | Route selection, payment methods | Choose algorithm at runtime |
| 15 | Command | Behavioral | Undo/redo, task queue | Wrap actions as objects |
| 16 | State | Behavioral | Order processing, TCP connection | Change behavior per state |
| 17 | Template Method | Behavioral | Restaurant preparation, data pipeline | Define skeleton, vary steps |
| 18 | Chain of Responsibility | Behavioral | Support escalation, exception handling | Pass through handlers |
| 19 | Iterator | Behavioral | Playlist navigation, file traversal | Access collection uniformly |
| 20 | Mediator | Behavioral | Chat room, air traffic control | Central communication hub |
| 21 | Memento | Behavioral | Game saves, database transactions | Save and restore state |
| 22 | Visitor | Behavioral | File operations, report generation | Add operations without changing objects |
| 23 | Interpreter | Behavioral | SQL parser, math calculator | Parse and interpret expressions |

---

## 🎯 Choosing the Right Pattern

**Creating objects?** → Creational (Singleton, Factory, Abstract Factory, Builder, Prototype)

**Organizing relationships?** → Structural (Adapter, Decorator, Facade, Proxy, Bridge, Composite, Flyweight)

**Object interactions?** → Behavioral (Observer, Strategy, Command, State, Template Method, Chain, Iterator, Mediator, Memento, Visitor, Interpreter)

**Need decoupling?** → Observer, Mediator, Facade, Bridge

**Need flexibility?** → Strategy, Decorator, Factory, Abstract Factory

**Need control flow?** → State, Chain of Responsibility, Command

**Need simplification?** → Facade, Adapter, Bridge

**Need to add features?** → Decorator, Visitor, Extension

---

This document provides **real-world context** for all 23 patterns, making them easier to remember and apply! 🚀
