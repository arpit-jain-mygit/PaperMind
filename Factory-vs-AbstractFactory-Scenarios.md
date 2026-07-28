# Factory vs Abstract Factory - Real Business & Technical Scenarios

---

## 🏭 FACTORY PATTERN - Real Scenarios

### **Scenario 1: E-Commerce Payment Processing**

**Business Context:** Your e-commerce platform supports multiple payment providers. Customers can pay via:
- Stripe
- PayPal
- Square
- Apple Pay

**Problem:** Without Factory, every checkout code would have:
```javascript
if (paymentProvider === 'stripe') {
    stripeProcessor = new StripePaymentProcessor();
    stripeProcessor.charge(...);
} else if (paymentProvider === 'paypal') {
    paypalProcessor = new PayPalPaymentProcessor();
    paypalProcessor.charge(...);
} // ... 10+ more conditions
```

This is messy, hard to extend, and violates Open/Closed Principle.

**Factory Solution:**
```javascript
class PaymentProcessorFactory {
    static create(provider) {
        if (provider === 'stripe') return new StripePaymentProcessor();
        if (provider === 'paypal') return new PayPalPaymentProcessor();
        if (provider === 'square') return new SquarePaymentProcessor();
        if (provider === 'apple') return new ApplePayProcessor();
        throw new Error(`Unknown provider: ${provider}`);
    }
}

// Checkout code - simple and clean
const processor = PaymentProcessorFactory.create(userSelectedProvider);
const result = processor.charge(amount, currency, metadata);
```

**Business Benefits:**
- ✅ Add new payment provider in 5 minutes
- ✅ No changes to checkout code when adding providers
- ✅ Easy to test each processor independently
- ✅ Can A/B test payment methods by simply changing factory logic

**Technical Benefits:**
- ✅ Each processor implements same interface
- ✅ Checkout code doesn't need to know payment details
- ✅ Easy to mock providers in tests
- ✅ Reduces coupling

---

### **Scenario 2: Cloud Storage Provider**

**Business Context:** Your startup started with AWS S3, but now needs to support:
- AWS S3 (primary)
- Google Cloud Storage (enterprise customers)
- Azure Blob Storage (Microsoft-heavy clients)
- MinIO (on-premise customers)

**Without Factory:**
```python
if config.storage_provider == 'aws':
    storage = S3Client(access_key, secret_key)
    storage.upload_file(file, bucket)
elif config.storage_provider == 'gcs':
    storage = GCSClient(credentials_json)
    storage.upload_file(file, bucket)
elif config.storage_provider == 'azure':
    storage = AzureBlobClient(connection_string)
    storage.upload_file(file, bucket)
# Nightmare when adding MinIO!
```

**With Factory:**
```python
class StorageFactory:
    @staticmethod
    def create(provider_type, config):
        providers = {
            'aws': S3StorageClient,
            'gcs': GoogleCloudStorageClient,
            'azure': AzureBlobStorageClient,
            'minio': MinIOStorageClient
        }
        
        if provider_type not in providers:
            raise ValueError(f"Unknown provider: {provider_type}")
        
        return providers[provider_type](config)

# Application code - same everywhere
storage = StorageFactory.create(
    config['STORAGE_PROVIDER'],
    config['STORAGE_CONFIG']
)
storage.upload_file(file_path, destination)
storage.download_file(key, local_path)
storage.delete_file(key)
```

**Real Business Impact:**
- ✅ Customer signs up and picks their preferred storage
- ✅ Deploy to different cloud providers without code changes
- ✅ Migrate from one provider to another by changing config
- ✅ Cost optimization: use cheapest provider per region

**Technical Details:**
```python
# All storage clients implement same interface
class StorageClient(ABC):
    @abstractmethod
    def upload_file(self, file_path, destination): pass
    
    @abstractmethod
    def download_file(self, key, local_path): pass
    
    @abstractmethod
    def delete_file(self, key): pass
    
    @abstractmethod
    def get_url(self, key): pass

# Each implementation handles provider-specific details
class S3StorageClient(StorageClient):
    def __init__(self, config):
        self.s3 = boto3.client('s3', **config)
    
    def upload_file(self, file_path, destination):
        self.s3.upload_file(file_path, self.bucket, destination)

class MinIOStorageClient(StorageClient):
    def __init__(self, config):
        self.client = Minio(**config)
    
    def upload_file(self, file_path, destination):
        self.client.fput_object(self.bucket, destination, file_path)
```

---

### **Scenario 3: Notification System (Email/SMS/Push)**

**Business Context:** Your SaaS product sends notifications via:
- Email (Gmail, SendGrid)
- SMS (Twilio, AWS SNS)
- Push Notifications (Firebase)
- Slack (for internal alerts)

**Real Use Case:**
```javascript
class NotificationFactory {
    static create(channel) {
        switch(channel) {
            case 'email':
                return new SendGridEmailNotification();
            case 'sms':
                return new TwilioSMSNotification();
            case 'push':
                return new FirebasePushNotification();
            case 'slack':
                return new SlackNotification();
        }
    }
}

// When user changes notification preferences
const notificationChannel = user.getPreferredChannel();
const notifier = NotificationFactory.create(notificationChannel);
notifier.send({
    recipient: user.contact,
    message: "Your order shipped!",
    metadata: { orderId: 123 }
});
```

**Business Scenarios:**
- ✅ User prefers email over SMS? Factory creates EmailNotifier
- ✅ Support team configures backup channels? Factory creates multiple notifiers
- ✅ Rate limiting on SMS? Add logic in factory without touching app code
- ✅ Failover: if SMS fails, factory falls back to email

---

### **Scenario 4: Database Connection**

**Technical Context:** Your application supports:
- PostgreSQL (primary)
- MySQL (legacy systems)
- MongoDB (analytics)
- DynamoDB (real-time data)

```python
class DatabaseFactory:
    @staticmethod
    def create(db_type, config):
        databases = {
            'postgres': PostgreSQLConnection,
            'mysql': MySQLConnection,
            'mongodb': MongoDBConnection,
            'dynamodb': DynamoDBConnection
        }
        return databases[db_type](config)

# Application code
db = DatabaseFactory.create(
    os.getenv('DB_TYPE'),
    os.getenv('DB_CONFIG')
)

# Same code works for any database
users = db.query("SELECT * FROM users WHERE active = true")
```

**Technical Benefit:** Switch databases by changing environment variable. Useful for:
- Testing (use SQLite in tests, PostgreSQL in prod)
- Gradual migration (run both databases, switch gradually)
- Region-specific databases (US uses PostgreSQL, Asia uses MySQL)

---

## 🏢 ABSTRACT FACTORY PATTERN - Real Scenarios

### **Scenario 1: SaaS Multi-Tenant Platform (Different Plans)**

**Business Context:** Your SaaS product has 3 pricing tiers:
- **Basic Plan:** Limited features
- **Professional Plan:** More features, integrations
- **Enterprise Plan:** Full features, dedicated support, custom integrations

Each plan has a coordinated set of components:
- Dashboard (different layouts per plan)
- Reports (different analytics per plan)
- API (different rate limits per plan)
- Support (different response times per plan)

**Problem:** You can't mix components from different tiers!
- Basic dashboard + Enterprise API = chaos
- Professional reports + Basic support = customer confusion

**Abstract Factory Solution:**

```javascript
// Abstract Factory - defines what each tier creates
class SubscriptionTierFactory {
    createDashboard() { }
    createReportEngine() { }
    createAPIClient() { }
    createSupportSystem() { }
}

// Concrete factories for each tier
class BasicTierFactory extends SubscriptionTierFactory {
    createDashboard() {
        return new BasicDashboard(); // 5 widgets max
    }
    
    createReportEngine() {
        return new BasicReportEngine(); // Monthly reports only
    }
    
    createAPIClient() {
        return new BasicAPIClient(); // 100 requests/day
    }
    
    createSupportSystem() {
        return new BasicSupportSystem(); // Email only, 48hr response
    }
}

class ProfessionalTierFactory extends SubscriptionTierFactory {
    createDashboard() {
        return new ProfessionalDashboard(); // 20 widgets, custom layout
    }
    
    createReportEngine() {
        return new ProfessionalReportEngine(); // Daily reports, data export
    }
    
    createAPIClient() {
        return new ProfessionalAPIClient(); // 10k requests/day
    }
    
    createSupportSystem() {
        return new ProfessionalSupportSystem(); // Email + chat, 4hr response
    }
}

class EnterpriseTierFactory extends SubscriptionTierFactory {
    createDashboard() {
        return new EnterpriseDashboard(); // Unlimited widgets, real-time updates
    }
    
    createReportEngine() {
        return new EnterpriseReportEngine(); // Real-time analytics, webhooks
    }
    
    createAPIClient() {
        return new EnterpriseAPIClient(); // Unlimited requests, custom endpoints
    }
    
    createSupportSystem() {
        return new EnterpriseSupportSystem(); // Phone + dedicated account manager, 1hr response
    }
}

// Application code
class UserSession {
    constructor(user) {
        const planType = user.subscriptionPlan; // 'basic', 'professional', 'enterprise'
        this.factory = this.getFactoryForPlan(planType);
        
        // Create complete, coordinated suite
        this.dashboard = this.factory.createDashboard();
        this.reports = this.factory.createReportEngine();
        this.api = this.factory.createAPIClient();
        this.support = this.factory.createSupportSystem();
    }
    
    getFactoryForPlan(plan) {
        const factories = {
            'basic': new BasicTierFactory(),
            'professional': new ProfessionalTierFactory(),
            'enterprise': new EnterpriseTierFactory()
        };
        return factories[plan];
    }
    
    // All components work together harmoniously
    getUserDashboard() {
        return this.dashboard.render(); // Right complexity for their tier
    }
    
    generateReport() {
        return this.reports.generate(); // Right frequency for their tier
    }
    
    callAPI(endpoint) {
        return this.api.call(endpoint); // Right rate limits for their tier
    }
}
```

**Real Business Impact:**
- ✅ No mixing of tier features - consistency guaranteed
- ✅ Add new tier (Starter, Plus, etc.) by adding one factory class
- ✅ Change feature per tier? Modify one factory
- ✅ Upsell path clear: "Upgrade to Professional to get real-time analytics"
- ✅ Easy to test each tier independently

---

### **Scenario 2: UI Theme System (Light/Dark/Custom Branding)**

**Business Context:** Your web app needs:
- **Light Theme** (professional, daytime)
- **Dark Theme** (eye-friendly, nighttime)
- **Custom Branding** (for white-label customers)

Each theme has coordinated:
- Colors (primary, secondary, accent, danger)
- Fonts (headings, body, mono)
- Spacing (padding, margins, gap)
- Shadows (soft, medium, strong)
- Components (buttons, inputs, cards, modals)

**Without Abstract Factory:**
```javascript
// Nightmare - scattered throughout codebase
if (theme === 'light') {
    primaryColor = '#667eea';
    backgroundColor = '#ffffff';
    textColor = '#333333';
} else if (theme === 'dark') {
    primaryColor = '#667eea';
    backgroundColor = '#1a1a1a';
    textColor = '#ffffff';
} else if (theme === 'custom') {
    primaryColor = customer.brandColor;
    // ... 100+ more variables
}

// Components with scattered logic
<Button style={{
    background: theme === 'light' ? '#667eea' : '#764ba2',
    padding: theme === 'light' ? '10px' : '8px',
    ...
}} />
```

**With Abstract Factory:**

```javascript
// Theme Factory - defines what each theme creates
class ThemeFactory {
    createColorScheme() { }
    createTypography() { }
    createSpacing() { }
    createComponents() { }
    createShadows() { }
}

// Light Theme
class LightThemeFactory extends ThemeFactory {
    createColorScheme() {
        return {
            primary: '#667eea',
            secondary: '#764ba2',
            accent: '#4f46e5',
            danger: '#e74c3c',
            warning: '#f39c12',
            success: '#27ae60',
            background: '#ffffff',
            surface: '#f5f5f5',
            text: '#333333',
            textSecondary: '#888888',
            border: '#dddddd'
        };
    }
    
    createTypography() {
        return {
            headingFont: 'Georgia, serif',
            bodyFont: 'Segoe UI, sans-serif',
            monoFont: 'Courier New, monospace',
            fontSize: {
                h1: '32px',
                h2: '24px',
                body: '16px'
            }
        };
    }
    
    createSpacing() {
        return {
            padding: '16px',
            margin: '16px',
            gap: '12px',
            borderRadius: '8px'
        };
    }
    
    createComponents() {
        return {
            Button: LightButton,
            Input: LightInput,
            Card: LightCard,
            Modal: LightModal,
            Alert: LightAlert
        };
    }
}

// Dark Theme
class DarkThemeFactory extends ThemeFactory {
    createColorScheme() {
        return {
            primary: '#764ba2',
            secondary: '#667eea',
            accent: '#7c3aed',
            danger: '#dc2626',
            warning: '#ea580c',
            success: '#16a34a',
            background: '#1a1a1a',
            surface: '#2d2d2d',
            text: '#ffffff',
            textSecondary: '#a0a0a0',
            border: '#444444'
        };
    }
    
    createTypography() {
        return {
            headingFont: 'Georgia, serif',
            bodyFont: 'Segoe UI, sans-serif',
            monoFont: 'Courier New, monospace',
            fontSize: {
                h1: '32px',
                h2: '24px',
                body: '16px'
            }
        };
    }
    
    createSpacing() {
        return {
            padding: '16px',
            margin: '16px',
            gap: '12px',
            borderRadius: '8px'
        };
    }
    
    createComponents() {
        return {
            Button: DarkButton,
            Input: DarkInput,
            Card: DarkCard,
            Modal: DarkModal,
            Alert: DarkAlert
        };
    }
}

// Custom Branding (White-label)
class CustomBrandingFactory extends ThemeFactory {
    constructor(customerBrandConfig) {
        super();
        this.config = customerBrandConfig;
    }
    
    createColorScheme() {
        return {
            primary: this.config.brandColor,
            secondary: this.config.secondaryColor,
            // ... etc, all customer-branded
        };
    }
    
    // ... other methods
}

// Application Usage
class App {
    constructor(userPreferences) {
        const themeType = userPreferences.theme; // 'light', 'dark', or 'custom'
        this.themeFactory = this.getThemeFactory(themeType);
        
        // Create complete, coordinated theme
        this.colors = this.themeFactory.createColorScheme();
        this.typography = this.themeFactory.createTypography();
        this.spacing = this.themeFactory.createSpacing();
        this.components = this.themeFactory.createComponents();
    }
    
    getThemeFactory(themeType) {
        if (themeType === 'light') return new LightThemeFactory();
        if (themeType === 'dark') return new DarkThemeFactory();
        if (themeType === 'custom') {
            return new CustomBrandingFactory(userConfig.branding);
        }
    }
    
    render() {
        // Components automatically use correct theme
        return (
            <this.components.Button style={{
                background: this.colors.primary,
                color: this.colors.text,
                padding: this.spacing.padding,
                fontFamily: this.typography.bodyFont
            }}>
                Click Me
            </this.components.Button>
        );
    }
}
```

**Real Business Impact:**
- ✅ Add new theme in 15 minutes without touching app code
- ✅ Consistent theme across entire app - guaranteed
- ✅ Customer branding? Pass config to factory, done
- ✅ Easy to test entire theme system
- ✅ Performance: load only needed theme

---

### **Scenario 3: Cross-Platform Mobile App (iOS vs Android)**

**Technical Context:** Your team develops for both iOS and Android. You need:
- **iOS Version:** Uses UIKit components, iOS-specific animations, iOS design language
- **Android Version:** Uses Material Design components, Android animations, Android design language

Same business logic, different UI families per platform!

```kotlin
// Abstract Factory
interface UIComponentFactory {
    fun createButton(): Button
    fun createInput(): TextInput
    fun createCard(): Card
    fun createDialog(): Dialog
    fun createNavigationBar(): NavigationBar
}

// iOS Implementation (UIKit)
class iOSComponentFactory : UIComponentFactory {
    override fun createButton(): Button = iOSButton() // Rounded, iOS feel
    override fun createInput(): TextInput = iOSInput() // iOS keyboard, haptics
    override fun createCard(): Card = iOSCard() // Shadows, iOS style
    override fun createDialog(): Dialog = iOSDialog() // Sheet-style
    override fun createNavigationBar(): NavigationBar = iOSTabBar() // Bottom tabs
}

// Android Implementation (Material Design)
class AndroidComponentFactory : UIComponentFactory {
    override fun createButton(): Button = MaterialButton() // Ripple effect
    override fun createInput(): TextInput = MaterialInput() // Floating label
    override fun createCard(): Card = MaterialCard() // Material elevation
    override fun createDialog(): Dialog = MaterialDialog() // Bottom sheet
    override fun createNavigationBar(): NavigationBar = MaterialBottomNavigation()
}

// Application Code (same for both platforms!)
class HomeScreen(platform: String) {
    private val factory: UIComponentFactory = when(platform) {
        "ios" -> iOSComponentFactory()
        "android" -> AndroidComponentFactory()
    }
    
    fun render() {
        val button = factory.createButton()
        val input = factory.createInput()
        val card = factory.createCard()
        
        // Same business logic, platform-appropriate UI
        button.setTitle("Save")
        button.setOnClick { saveData() }
        
        input.setPlaceholder("Enter name")
        input.setOnChange { validateInput() }
        
        card.addContent(listOf(input, button))
    }
}
```

**Real Business Impact:**
- ✅ Share 80% of code between iOS and Android
- ✅ Each platform gets native, appropriate UI
- ✅ Add feature once, it appears on both platforms
- ✅ Easier to hire: frontend devs can work on both

---

### **Scenario 4: Document Export System**

**Business Context:** Your reporting platform exports to multiple formats:
- **PDF** (formatted, for printing)
- **Excel** (for data analysis)
- **Word** (for documents)
- **JSON** (for APIs)

Each format needs coordinated:
- Data formatter
- Style manager
- Table renderer
- Chart renderer
- File writer

```python
class DocumentExportFactory:
    def create_data_formatter(self): pass
    def create_style_manager(self): pass
    def create_table_renderer(self): pass
    def create_chart_renderer(self): pass
    def create_file_writer(self): pass

class PDFExportFactory(DocumentExportFactory):
    def create_data_formatter(self):
        return PDFDataFormatter() # Font sizing, colors
    
    def create_style_manager(self):
        return PDFStyleManager() # Margins, headers, footers
    
    def create_table_renderer(self):
        return PDFTableRenderer() # Page breaks, column widths
    
    def create_chart_renderer(self):
        return PDFChartRenderer() # Vector graphics
    
    def create_file_writer(self):
        return PDFFileWriter() # PDF binary format

class ExcelExportFactory(DocumentExportFactory):
    def create_data_formatter(self):
        return ExcelDataFormatter() # Numbers, dates, currency
    
    def create_style_manager(self):
        return ExcelStyleManager() # Cell colors, fonts
    
    def create_table_renderer(self):
        return ExcelTableRenderer() # Sheets, freezing
    
    def create_chart_renderer(self):
        return ExcelChartRenderer() # Excel charts
    
    def create_file_writer(self):
        return ExcelFileWriter() # XLSX format

# Application
class ReportGenerator:
    def export(self, format_type, data):
        factory = self.get_factory(format_type)
        
        # All components coordinated
        formatter = factory.create_data_formatter()
        styles = factory.create_style_manager()
        tables = factory.create_table_renderer()
        charts = factory.create_chart_renderer()
        writer = factory.create_file_writer()
        
        # Generate report with coordinated components
        formatted_data = formatter.format(data)
        styled_data = styles.apply_styles(formatted_data)
        rendered_tables = tables.render(styled_data)
        rendered_charts = charts.render(styled_data)
        
        return writer.write(rendered_tables, rendered_charts)
```

**Real Business Impact:**
- ✅ Add new export format (PowerPoint?) by adding one factory
- ✅ Consistent quality across all formats
- ✅ Export logic centralized and testable
- ✅ Performance: load only needed exporters

---

## 📊 Quick Comparison Matrix

| Aspect | Factory | Abstract Factory |
|--------|---------|------------------|
| **Creates** | Single independent objects | Families of related objects |
| **Coordination** | No - objects are independent | Yes - objects must work together |
| **Typical Use** | Payment providers, storage, notifications | Themes, tiers, platforms, document formats |
| **Add New Type** | Add case to switch statement | Add new concrete factory class |
| **Complexity** | Lower | Higher |
| **Business Benefit** | Easy to extend with new single object | Guarantees consistency across related objects |
| **Example** | Stripe OR Twilio (pick one) | iOS Button + Input + Card (all coordinated) |

---

## 🎯 Decision Guide

**Use FACTORY when:**
- ✅ Creating single, independent objects
- ✅ Different implementations, same interface
- ✅ Want to hide creation details
- ✅ Example: payment processor (don't care what else exists)

**Use ABSTRACT FACTORY when:**
- ✅ Creating families of related objects
- ✅ Objects MUST work together and be consistent
- ✅ Multiple interrelated components per type
- ✅ Example: UI theme (button, input, card must match)
