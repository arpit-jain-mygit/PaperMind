# Kafka Notification Flow - Student Created Event

## Overview
Complete flow showing how Kafka is used to send notifications when a student is created.

---

## Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────┐
│ BROWSER (Angular UI)                                               │
│ http://localhost:4200                                              │
│                                                                    │
│ User fills form & clicks "Create Student"                         │
└──────────────────────────┬─────────────────────────────────────────┘
                           │
                HTTP POST /api/students
                           │
                           ▼
┌────────────────────────────────────────────────────────────────────┐
│ SPRING BOOT BACKEND (Port 8080)                                   │
│                                                                    │
│ StudentController.createStudent()                                 │
│          ↓                                                         │
│ StudentService.createStudent()                                    │
│          ├─ Validates data                                        │
│          ├─ Saves to database                                     │
│          │   └─ StudentRepository.save()                          │
│          │       └─ INSERT INTO student table                     │
│          │           └─ Returns Student with ID                   │
│          │                                                         │
│          └─ Publishes to Kafka                                    │
│              └─ StudentNotificationProducer.publishStudentCreatedEvent()
│                  └─ Sends StudentCreatedEvent to Kafka topic      │
└──────────────────────────┬─────────────────────────────────────────┘
                           │
              Kafka Message Published
              Topic: student-notifications
              Message: StudentCreatedEvent
                           │
                           ▼
┌────────────────────────────────────────────────────────────────────┐
│ KAFKA BROKER (Port 9092)                                           │
│                                                                    │
│ Topic: student-notifications                                      │
│ Partition: 0                                                       │
│ Message: {                                                         │
│   "studentId": 1,                                                  │
│   "studentName": "John Doe",                                       │
│   "studentEmail": "john@example.com",                              │
│   "phoneNumber": "9876543210",                                     │
│   "gpa": 3.8,                                                      │
│   "timestamp": 1673891234567                                       │
│ }                                                                  │
└──────────────────────────┬─────────────────────────────────────────┘
                           │
              Kafka Consumer Listens
                           │
                           ▼
┌────────────────────────────────────────────────────────────────────┐
│ NOTIFICATION SYSTEM (Consumer)                                    │
│                                                                    │
│ StudentNotificationConsumer                                       │
│   @KafkaListener(topic = "student-notifications")                 │
│          ↓                                                         │
│ Receives StudentCreatedEvent                                      │
│          ↓                                                         │
│ Calls NotificationService.sendNotification()                      │
│          ↓                                                         │
│ 🔔 NOTIFICATION SENT                                              │
│ Print: Student Created Notification                               │
│        - ID, Name, Email, Phone, GPA                              │
└────────────────────────────────────────────────────────────────────┘
```

---

## Step-by-Step Flow

### Step 1: User Creates Student
**Browser:** http://localhost:4200
```
User clicks: "Create Student" button
Form data:
- Name: "John Doe"
- Email: "john@example.com"
- Phone: "9876543210"
- GPA: 3.8
```

### Step 2: HTTP Request Sent
```
POST http://localhost:8080/api/students
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "phoneNumber": "9876543210",
  "gpa": 3.8
}
```

### Step 3: Spring Boot Receives Request
**File:** `StudentController.java`
```java
@PostMapping
public ResponseEntity<Student> createStudent(@RequestBody Student student) {
    Student savedStudent = studentService.createStudent(student);
    return ResponseEntity.status(HttpStatus.CREATED).body(savedStudent);
}
```

### Step 4: Service Validates and Saves
**File:** `StudentServiceImpl.java`
```java
@Override
public Student createStudent(Student student) {
    // 1. Validate data
    if (student.getName() == null || student.getName().trim().isEmpty()) {
        throw new IllegalArgumentException("Student name cannot be empty");
    }
    if (student.getEmail() == null || student.getEmail().trim().isEmpty()) {
        throw new IllegalArgumentException("Student email cannot be empty");
    }
    if (student.getGpa() == null || student.getGpa() < 0 || student.getGpa() > 4.0) {
        throw new IllegalArgumentException("GPA must be between 0 and 4.0");
    }

    // 2. Save to database
    Student savedStudent = studentRepository.save(student);
    // Database generates ID: 1

    // 3. Publish to Kafka (NEW!)
    StudentCreatedEvent event = new StudentCreatedEvent(
        savedStudent.getId(),      // 1
        savedStudent.getName(),    // "John Doe"
        savedStudent.getEmail(),   // "john@example.com"
        savedStudent.getPhoneNumber(), // "9876543210"
        savedStudent.getGpa()      // 3.8
    );
    notificationProducer.publishStudentCreatedEvent(event);

    return savedStudent;
}
```

### Step 5: Kafka Producer Publishes Event
**File:** `StudentNotificationProducer.java`
```java
@Service
public class StudentNotificationProducer {

    @Autowired
    private KafkaTemplate<String, StudentCreatedEvent> kafkaTemplate;

    @Value("${kafka.topic}")
    private String topic;  // "student-notifications"

    public void publishStudentCreatedEvent(StudentCreatedEvent event) {
        System.out.println("Publishing StudentCreatedEvent to Kafka topic: " + topic);
        System.out.println("Event: " + event);

        // Send message to Kafka
        kafkaTemplate.send(topic, event.getStudentId().toString(), event);
        //                  └─────┬──────  └─────┬─────  └─────┬─────
        //                    Topic        Key (studentId)  Value (event)

        System.out.println("StudentCreatedEvent published successfully!");
    }
}
```

**Kafka Message:**
```
Topic: student-notifications
Key: "1"
Value: {
  "studentId": 1,
  "studentName": "John Doe",
  "studentEmail": "john@example.com",
  "phoneNumber": "9876543210",
  "gpa": 3.8,
  "timestamp": 1673891234567
}
```

### Step 6: Configuration Enables Producer
**File:** `application.yml`
```yaml
kafka:
  bootstrap-servers: localhost:9092      # Kafka broker address
  topic: student-notifications           # Topic name
  producer:
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

### Step 7: Kafka Broker Receives Message
```
Kafka Broker (localhost:9092)
  └─ Topic: student-notifications
      └─ Partition: 0
          └─ Message 1: {studentId: 1, studentName: "John Doe", ...}
```

### Step 8: Kafka Consumer Listens
**File:** `StudentNotificationConsumer.java`
```java
@Service
public class StudentNotificationConsumer {

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "${kafka.topic}", groupId = "${kafka.consumer.group-id}")
    // Listens on: student-notifications
    // Consumer group: student-notification-group
    public void consumeStudentCreatedEvent(StudentCreatedEvent event) {
        System.out.println("Received StudentCreatedEvent from Kafka: " + event);

        // Process the event
        notificationService.sendNotification(event);

        System.out.println("Notification processed for student: " + event.getStudentName());
    }
}
```

### Step 9: Notification Service Processes Event
**File:** `NotificationService.java`
```java
@Service
public class NotificationService {

    public void sendNotification(StudentCreatedEvent event) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🔔 STUDENT CREATED NOTIFICATION");
        System.out.println("=".repeat(70));
        System.out.println("Notification Type: Student Registration");
        System.out.println("Student ID: " + event.getStudentId());
        System.out.println("Student Name: " + event.getStudentName());
        System.out.println("Student Email: " + event.getStudentEmail());
        System.out.println("Phone Number: " + event.getPhoneNumber());
        System.out.println("GPA: " + event.getGpa());
        System.out.println("Created At: " + dateTime);
        System.out.println("Status: ✅ NOTIFICATION SENT");
        System.out.println("=".repeat(70) + "\n");
    }
}
```

### Step 10: Notification Displayed
**Console Output:**
```
Publishing StudentCreatedEvent to Kafka topic: student-notifications
Event: StudentCreatedEvent{studentId=1, studentName='John Doe', 
        studentEmail='john@example.com', phoneNumber='9876543210', 
        gpa=3.8, timestamp=1673891234567}
StudentCreatedEvent published successfully!

Received StudentCreatedEvent from Kafka: StudentCreatedEvent{studentId=1, ...}

======================================================================
🔔 STUDENT CREATED NOTIFICATION
======================================================================
Notification Type: Student Registration
Student ID: 1
Student Name: John Doe
Student Email: john@example.com
Phone Number: 9876543210
GPA: 3.8
Created At: 2024-01-15 14:30:45.567
Status: ✅ NOTIFICATION SENT
======================================================================

Notification processed for student: John Doe
```

---

## Files Involved

| Layer | File | Purpose |
|-------|------|---------|
| **Event** | `StudentCreatedEvent.java` | Event object sent via Kafka |
| **Producer** | `StudentNotificationProducer.java` | Publishes events to Kafka |
| **Service** | `StudentServiceImpl.java` | Calls producer after saving |
| **Consumer** | `StudentNotificationConsumer.java` | Listens for events from Kafka |
| **Handler** | `NotificationService.java` | Processes received events |
| **Config** | `application.yml` | Kafka broker and topic config |
| **Dependency** | `pom.xml` | spring-kafka dependency |

---

## Configuration Files

### application.yml
```yaml
kafka:
  bootstrap-servers: localhost:9092
  topic: student-notifications
  producer:
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  consumer:
    bootstrap-servers: localhost:9092
    group-id: student-notification-group
    key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
```

### pom.xml
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

---

## How to Test

### 1. Start Kafka (if not running)
```bash
# Download and start Kafka (local)
# Or use Docker:
docker run -d --name kafka -e KAFKA_ZOOKEEPER_CONNECT=localhost:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -p 9092:9092 confluentinc/cp-kafka:latest
```

### 2. Start Backend
```bash
cd backend
mvn spring-boot:run
```

### 3. Create Student via UI
```
1. Open http://localhost:4200
2. Fill in student form
3. Click "Create Student"
```

### 4. Check Backend Console
Look for:
```
Publishing StudentCreatedEvent to Kafka topic: student-notifications
🔔 STUDENT CREATED NOTIFICATION
Student Name: John Doe
Status: ✅ NOTIFICATION SENT
```

---

## Kafka Concepts Used

### Topic
```
Name: student-notifications
Purpose: Channel for student events
Partitions: 1
Replication: 1
```

### Producer
```
Publishes: StudentCreatedEvent
To: student-notifications topic
Key: studentId (string)
Value: StudentCreatedEvent (JSON)
```

### Consumer
```
Listens: student-notifications topic
Group: student-notification-group
Handler: StudentNotificationConsumer
Processes: Calls NotificationService
```

### Message Format
```json
{
  "studentId": 1,
  "studentName": "John Doe",
  "studentEmail": "john@example.com",
  "phoneNumber": "9876543210",
  "gpa": 3.8,
  "timestamp": 1673891234567
}
```

---

## Summary

**Flow:**
1. User creates student in UI
2. Angular sends POST request
3. Spring Boot validates and saves
4. Kafka producer publishes event
5. Event sent to Kafka broker
6. Consumer listens and receives
7. Notification service processes
8. Notification displayed in console

**Benefits:**
- ✅ Decoupled: Service layer doesn't block on notification
- ✅ Scalable: Multiple consumers can listen
- ✅ Reliable: Messages persisted in Kafka
- ✅ Async: Notifications processed asynchronously

**Next Steps:**
- Add email notification
- Add SMS notification
- Add database audit logging
- Add multiple consumers
- Add error handling and retries
