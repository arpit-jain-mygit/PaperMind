# Kafka Setup Guide - Student Notifications

## Quick Start (Option 1: Docker - Easiest)

### Prerequisites
- Docker installed

### Start Kafka with Docker
```bash
# Start Zookeeper
docker run -d --name zookeeper -e ZOOKEEPER_CLIENT_PORT=2181 \
  -p 2181:2181 confluentinc/cp-zookeeper:latest

# Start Kafka
docker run -d --name kafka \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092 \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT \
  -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -p 9092:9092 \
  --link zookeeper:zookeeper \
  confluentinc/cp-kafka:latest
```

Verify running:
```bash
docker ps
# Should show: zookeeper and kafka containers running
```

---

## Full Setup (Option 2: Download & Run Locally)

### Prerequisites
- Java 11+
- 2GB RAM free

### Step 1: Download Kafka

**macOS/Linux:**
```bash
# Download
curl https://archive.apache.org/dist/kafka/3.5.0/kafka_2.13-3.5.0.tgz -o kafka.tgz

# Extract
tar -xzf kafka.tgz
cd kafka_2.13-3.5.0
```

**Windows:**
- Download from: https://kafka.apache.org/downloads
- Extract to: `C:\kafka`

### Step 2: Start Zookeeper

**macOS/Linux:**
```bash
cd kafka_2.13-3.5.0
./bin/zookeeper-server-start.sh config/zookeeper.properties
```

**Windows:**
```bash
.\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties
```

You should see: `[2024-01-15 10:00:00,000] INFO Zookeeper server started`

### Step 3: Start Kafka (New Terminal)

**macOS/Linux:**
```bash
cd kafka_2.13-3.5.0
./bin/kafka-server-start.sh config/server.properties
```

**Windows:**
```bash
.\bin\windows\kafka-server-start.bat .\config\server.properties
```

You should see: `[2024-01-15 10:00:05,000] INFO [KafkaServer id=0] started`

### Step 4: Verify Kafka is Running

**macOS/Linux:**
```bash
./bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

**Windows:**
```bash
.\bin\windows\kafka-broker-api-versions.bat --bootstrap-server localhost:9092
```

Should return API versions (not an error).

---

## Complete Setup Commands

### Docker Setup (All-in-One)
```bash
# Start both Zookeeper and Kafka
docker-compose -f docker-compose.yml up -d
```

Create `docker-compose.yml`:
```yaml
version: '3'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"
```

---

## Verify Topic Created

Once Kafka is running, the topic `student-notifications` will be created automatically when Spring Boot starts.

To verify manually:

**macOS/Linux:**
```bash
./bin/kafka-topics.sh --list --bootstrap-server localhost:9092
# Should show: student-notifications
```

**Windows:**
```bash
.\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092
```

---

## Complete System Start (In Order)

### Terminal 1: Start PostgreSQL
```bash
# If using Homebrew on macOS
brew services start postgresql

# Or manually start PostgreSQL (it might be already running)
psql -U arpit -d hello_world_db
```

### Terminal 2: Start Zookeeper
```bash
# If using local Kafka download
cd kafka_2.13-3.5.0
./bin/zookeeper-server-start.sh config/zookeeper.properties

# Or if using Docker, Zookeeper starts with Kafka
```

### Terminal 3: Start Kafka
```bash
# If using local Kafka download
cd kafka_2.13-3.5.0
./bin/kafka-server-start.sh config/server.properties

# Or if using Docker
docker-compose up -d
```

### Terminal 4: Start Backend
```bash
cd /Users/arpit/Documents/claude/transcription-workspace/fullstack-hello-world/backend
mvn spring-boot:run
```

**You should see in logs:**
```
...
Registering KafkaListener ...
Kafka Consumer started
StudentNotificationConsumer listening on topic: student-notifications
...
```

### Terminal 5: Start Frontend
```bash
cd /Users/arpit/Documents/claude/transcription-workspace/fullstack-hello-world/frontend
npm start
```

---

## Test the Flow

1. Open browser: `http://localhost:4200`
2. Fill in student form:
   - Name: Test Student
   - Email: test@example.com
   - Phone: 1234567890
   - GPA: 3.5
3. Click "Create Student"
4. Check **Terminal 4** (Backend) console:
   ```
   Publishing StudentCreatedEvent to Kafka topic: student-notifications
   🔔 STUDENT CREATED NOTIFICATION
   Student Name: Test Student
   Status: ✅ NOTIFICATION SENT
   ```

---

## Troubleshooting

### "Connection refused" on 9092
- Kafka not running
- Check Terminal 3 (Kafka server)
- Make sure Terminal 2 (Zookeeper) started first

### Topic doesn't exist
- Kafka broker not fully started
- Wait 10 seconds and try creating student again
- Topic auto-created on first message

### Consumer not receiving messages
- Make sure Kafka is running before Spring Boot
- Check `application.yml` has correct `bootstrap-servers: localhost:9092`
- Check logs: "StudentNotificationConsumer listening on topic: student-notifications"

### Port 9092 already in use
```bash
# Find process using port
lsof -i :9092

# Kill it
kill -9 <PID>

# Or use different port in application.yml
```

---

## Stop Services

When done testing:

```bash
# Terminal 4: Stop Backend (Ctrl+C)
# Terminal 5: Stop Frontend (Ctrl+C)

# Terminal 3: Stop Kafka (Ctrl+C)
# Terminal 2: Stop Zookeeper (Ctrl+C)

# Terminal 1: Stop PostgreSQL
brew services stop postgresql

# Or if using Docker:
docker-compose down
```

---

## Important Files

| File | Location | Purpose |
|------|----------|---------|
| Kafka Config | `application.yml` | Bootstrap servers, topics, serializers |
| Producer | `StudentNotificationProducer.java` | Publishes events |
| Consumer | `StudentNotificationConsumer.java` | Listens for events |
| Handler | `NotificationService.java` | Processes notifications |
| Event | `StudentCreatedEvent.java` | Message object |

---

## Architecture

```
PostgreSQL (5432)
    ↑
Spring Boot (8080)
    ├─ Saves student
    ├─ Publishes to Kafka
    │
Kafka Broker (9092)
    ├─ Topic: student-notifications
    │
Consumer (Spring Boot)
    ├─ Listens to Kafka
    └─ Sends notification
```

---

## Next Steps

- Add email notifications
- Add SMS notifications
- Add multiple consumers for different notification types
- Add error handling and retries
- Add notification audit logging
- Add webhook integrations
