# Complete Setup Guide - Student Management System

Fullstack app with Angular frontend, Spring Boot backend, PostgreSQL + MongoDB dual-write.

---

## Prerequisites Check

```bash
java -version           # Should be 17+
node --version          # Should be 18+
npm --version           # Should be 8+
psql --version          # PostgreSQL 18+
mongosh --version       # MongoDB client
```

If missing, install them.

---

## Step-by-Step Setup

### Phase 1: Initial Setup (One Time)

#### 1.1: Install MongoDB
```bash
brew tap mongodb/brew
brew install mongodb-community
```

Verify:
```bash
mongosh
> db.adminCommand('ping')
```

Should return `{ ok: 1 }`. Type `exit`.

#### 1.2: Set JAVA_HOME
```bash
echo 'export JAVA_HOME=/opt/homebrew/opt/openjdk' >> ~/.zshrc
source ~/.zshrc
```

#### 1.3: Navigate to Project
```bash
cd /Users/arpit/Documents/claude/transcription-workspace/fullstack-hello-world
```

#### 1.4: Install Backend & Frontend Dependencies
```bash
cd backend && mvn clean install && cd ..
cd frontend && npm install && cd ..
```

---

### Phase 2: Start Services (Every Time)

**Open 4 Terminal windows:**

#### Terminal 1: Start PostgreSQL & MongoDB
```bash
brew services start postgresql
brew services start mongodb-community
```

Verify PostgreSQL:
```bash
psql -U arpit -d hello_world_db
\q
```

Verify MongoDB:
```bash
mongosh
> exit
```

#### Terminal 2: Start Zookeeper (Local)
```bash
cd kafka_2.13-3.5.0
./bin/zookeeper-server-start.sh config/zookeeper.properties
```

Wait for: `binding to port 0.0.0.0/0.0.0.0:2181`

#### Terminal 3: Start Kafka (Local)
```bash
cd kafka_2.13-3.5.0
./bin/kafka-server-start.sh config/server.properties
```

Wait for: `[KafkaServer id=0] started`

#### Terminal 4: Set OpenAI API Key & Start Backend
```bash
export OPENAI_API_KEY="sk-your-actual-key-here"
cd /Users/arpit/Documents/claude/transcription-workspace/fullstack-hello-world/backend
mvn spring-boot:run
```

Wait for: `Started HelloWorldApplication in X.XXX seconds`

Backend now:
- Saves students to **PostgreSQL AND MongoDB** ✅
- Can call **OpenAI API** for AI summaries ✅

#### Terminal 5: Start Frontend (Open new terminal)
```bash
cd /Users/arpit/Documents/claude/transcription-workspace/fullstack-hello-world/frontend
npm start
```

**Wait for this:**
```
✔ Compiled successfully.
Angular Live Development Server is listening on localhost:4200.
```

---

### Phase 3: Test the Application

#### Open Browser
```
http://localhost:4200
```

You should see:
```
Student Management System

Add New Student
Name: [________]
Email: [________]
Phone: [________]
GPA: [__]
[Create Student]

Students List
[Load Students]
```

#### Test Create Student
1. Fill in the form:
   ```
   Name: Test Student
   Email: test@example.com
   Phone: 9876543210
   GPA: 3.5
   ```

2. Click **"Create Student"**

3. You should see:
   - Green success message
   - New student appears in table

4. Verify data in both databases:
   ```bash
   # PostgreSQL
   psql -U arpit -d hello_world_db
   > SELECT * FROM student;
   
   # MongoDB
   mongosh
   > use students_db
   > db.students.find()
   ```

Both should show the same student data! ✅

#### Test AI Summary (GenAI Feature)
1. In student table, find the "✨ AI Summary" button
2. Click it for any student
3. Modal opens with "Generating AI summary..."
4. After 2-3 seconds, OpenAI generates summary
5. Read the AI-generated insight about student
6. Close modal

Example AI Summary:
```
"Test Student demonstrates solid academic performance with a 3.5 GPA. 
This student shows consistent dedication to their studies. Consider 
exploring internship opportunities to complement their academic success."
```

#### Test Load Students
1. Click **"Load Students"** button
2. All students appear in table
3. Table shows: ID, Name, Email, Phone, GPA, Edit/Delete buttons

#### Test Delete Student
1. Click **"Delete"** button next to a student
2. Confirm deletion
3. Student removed from table

---

## Troubleshooting Guide

### MongoDB Issues

**Q: "Error creating bean with name 'mongoRepository'"**
- A: MongoDB not running. Run: `brew services start mongodb-community`

**Q: "Connection refused" on 27017**
- A: MongoDB not started. Check with: `mongosh`

**Q: "Cannot connect to Mongo server"**
- A: Verify MongoDB is running: `brew services list | grep mongodb`

---

### OpenAI Issues

**Q: "AI Summary" button shows error**
- A: OpenAI API key not set. Run: `export OPENAI_API_KEY="sk-..."`

**Q: "401 Unauthorized" when calling AI Summary**
- A: Invalid API key. Get new one from https://platform.openai.com/api-keys

**Q: "Rate limit exceeded"**
- A: Too many requests. Wait a minute or upgrade OpenAI plan

**Q: Modal shows "Failed to generate AI summary"**
- A: Check backend logs for OpenAI API errors
- Verify API key is valid
- Check OpenAI account has credits

---

### Backend Issues

**Q: "Error creating bean with name 'studentRepository'"**
- A: PostgreSQL not running. Run: `brew services start postgresql`

**Q: "Port 8080 already in use"**
- A: Kill process: `lsof -i :8080` then `kill -9 <PID>`

**Q: "Cannot find Kafka broker at localhost:9092"**
- A: Kafka not running. Check Terminal 1 status

**Q: Maven dependencies fail to download**
- A: Check internet connection
- Run: `mvn clean install -U` (update dependencies)

---

### Frontend Issues

**Q: "ng: command not found"**
- A: Dependencies not installed. Run: `cd frontend && npm install`

**Q: "Cannot GET /api/students"**
- A: Backend not running. Check Terminal 3

**Q: Port 4200 already in use**
- A: Kill process: `lsof -i :4200` then `kill -9 <PID>`
- Or use different port: `ng serve --port 4201`

---

### PostgreSQL Issues

**Q: "psql: error: connection to server on socket failed"**
- A: PostgreSQL not running. Run: `brew services start postgresql`

**Q: "FATAL: database 'hello_world_db' does not exist"**
- A: Create it: `createdb hello_world_db`

**Q: "FATAL: role 'arpit' does not exist"**
- A: You might need different username. Check with: `psql -U postgres`

---

## File Locations

All files are in:
```
/Users/arpit/Documents/claude/transcription-workspace/fullstack-hello-world/
```

| Component | Path |
|-----------|------|
| Backend | `backend/` |
| Frontend | `frontend/` |
| PostgreSQL | Local (port 5432) |
| MongoDB | Local (port 27017) |
| Kafka | Local (port 9092) |
| Setup Docs | `*.md` files |

---

## Complete Checklist

### Before Starting
- [ ] Java 17+ installed (`java -version`)
- [ ] Node 18+ installed (`node --version`)
- [ ] PostgreSQL installed & running
- [ ] MongoDB installed & running (`mongosh` works)
- [ ] Maven installed (`mvn --version`)
- [ ] JAVA_HOME set in ~/.zshrc
- [ ] OpenAI API key (https://platform.openai.com/api-keys)

### Starting Services (Every Time)
- [ ] Terminal 1: PostgreSQL & MongoDB running
- [ ] Terminal 2: Zookeeper running (see "binding to port 2181")
- [ ] Terminal 3: Kafka running (see "[KafkaServer id=0] started")
- [ ] Terminal 4: Backend running (see "Started HelloWorldApplication")
- [ ] Terminal 5: Frontend running (see "listening on localhost:4200")

### Testing
- [ ] Browser opens `http://localhost:4200`
- [ ] Can create student (form works)
- [ ] Student appears in PostgreSQL: `psql ... SELECT * FROM student;`
- [ ] Student appears in MongoDB: `mongosh > db.students.find()`
- [ ] Can load students list
- [ ] Can update & delete students

---

## Ports Used

| Service | Port | Check |
|---------|------|-------|
| Angular UI | 4200 | http://localhost:4200 |
| Spring Boot Backend | 8080 | http://localhost:8080/api/students |
| PostgreSQL | 5432 | `psql -U arpit -d hello_world_db` |
| MongoDB | 27017 | `mongosh` |
| Kafka Broker | 9092 | Zookeeper & Kafka running |
| Zookeeper | 2181 | Local process |

---

## Documentation Files

Read these for detailed information:

| File | Purpose |
|------|---------|
| [README.md](README.md) | Project overview |
| [QUICKSTART.md](QUICKSTART.md) | Quick start (no details) |
| [ARCHITECTURE.md](ARCHITECTURE.md) | System architecture |
| [DOCKER-SETUP-MAC.md](DOCKER-SETUP-MAC.md) | Docker installation steps |
| [KAFKA-SETUP.md](KAFKA-SETUP.md) | Kafka detailed setup |
| [FLOWS/README.md](FLOWS/README.md) | Flow documentation index |
| [FLOWS/BOOTSTRAP-FLOW.md](FLOWS/BOOTSTRAP-FLOW.md) | How page loads |
| [FLOWS/CREATE-FLOW.md](FLOWS/CREATE-FLOW.md) | Create student flow |
| [FLOWS/READ-FLOW.md](FLOWS/READ-FLOW.md) | Read students flow |
| [FLOWS/UPDATE-FLOW.md](FLOWS/UPDATE-FLOW.md) | Update student flow |
| [FLOWS/DELETE-FLOW.md](FLOWS/DELETE-FLOW.md) | Delete student flow |
| [FLOWS/KAFKA-NOTIFICATION-FLOW.md](FLOWS/KAFKA-NOTIFICATION-FLOW.md) | Kafka flow |
| [FLOWS/AI-SUMMARY-FLOW.md](FLOWS/AI-SUMMARY-FLOW.md) | AI Summary GenAI flow |

---

## Stop Everything

When you're done:

```bash
# Terminal 2: Stop Zookeeper (Ctrl+C)
# Terminal 3: Stop Kafka (Ctrl+C)
# Terminal 4: Stop Backend (Ctrl+C)
# Terminal 5: Stop Frontend (Ctrl+C)

# Optional - stop services:
brew services stop postgresql
brew services stop mongodb-community
```

---

## Quick Commands Reference

```bash
# Check system status
psql -U arpit -d hello_world_db    # PostgreSQL
mongosh > use students_db           # MongoDB
curl http://localhost:8080/api/students  # Backend
curl http://localhost:4200     # Frontend

# Restart individual services
brew services restart postgresql    # Restart PostgreSQL
brew services restart mongodb-community  # Restart MongoDB
pkill -f "java.*spring-boot"   # Restart backend

# View MongoDB data
mongosh
> use students_db
> db.students.find()
> db.students.deleteMany({})  # Clear collection

# Kill processes on ports
lsof -i :8080   # What's using port 8080
kill -9 <PID>   # Kill process
```

---

## Expected Timeline

**First Time Setup:**
- Install MongoDB: 3 min
- Install dependencies: 5 min
- **Total: ~8 min**

**Every Time Running:**
| Step | Time |
|------|------|
| Start PostgreSQL/MongoDB | 2 sec |
| Start Zookeeper | 3 sec |
| Start Kafka | 5 sec |
| Start Backend | 8 sec |
| Start Frontend | 5 sec |
| **Total: ~30 sec** |

---

## Next Steps After Setup

1. **Explore the flows:** Read [FLOWS/README.md](FLOWS/README.md)
2. **Understand architecture:** Read [ARCHITECTURE.md](ARCHITECTURE.md)
3. **Add features:** See flow documentation for implementation details
4. **Add email notifications:** Extend [NotificationService.java](backend/src/main/java/com/example/kafka/NotificationService.java)
5. **Add more endpoints:** See [CREATE-FLOW.md](FLOWS/CREATE-FLOW.md) for patterns

---

## Support

If something doesn't work:

1. Check the troubleshooting section above
2. Read the relevant documentation file
3. Check if all services are running (all 4 terminals)
4. Restart the service that's having issues
5. Check logs for error messages

Good luck! 🚀
