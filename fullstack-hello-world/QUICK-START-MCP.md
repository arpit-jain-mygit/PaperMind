# Quick Start: Batch Agent with MCP

## TL;DR - Run Everything

### Terminal 1: PostgreSQL
```bash
brew services start postgresql
```

### Terminal 2: Backend (Spring Boot)
```bash
cd /Users/arpit/Documents/claude/transcription-workspace/fullstack-hello-world/backend

# Set environment variables
export DBHUB_HOST=localhost
export DBHUB_USER=arpit
export DBHUB_PASSWORD=1234
export DBHUB_DATABASE=hello_world_db

# Install DBHub globally (one-time)
npm install -g dbhub

# Run Spring Boot
mvn spring-boot:run
```

Backend will:
1. Start Spring application
2. Auto-create DBHubMCPClient bean
3. Start DBHub MCP server process
4. Listen on http://localhost:8080

### Terminal 3: Frontend (Angular)
```bash
cd /Users/arpit/Documents/claude/transcription-workspace/fullstack-hello-world/frontend

ng serve
```

Frontend will listen on http://localhost:4200

---

## Test Batch Agent API

### Option A: Browser UI
1. Open http://localhost:4200
2. Scroll to "🤖 Batch Create Agent" section
3. Paste CSV data:
```
Alice,alice@test.com,9876543210,3.9
Bob,bob@test.com,9876543211,3.1
Carol,carol@test.com,9876543212,3.8
```
4. Click "▶ Process Batch"
5. View results in modal

### Option B: curl (Terminal 4)
```bash
curl -X POST http://localhost:8080/api/batch/create-students \
  -H "Content-Type: application/json" \
  -d '{
    "students": [
      {
        "name": "Alice Johnson",
        "email": "alice@test.com",
        "phoneNumber": "9876543210",
        "gpa": 3.9
      },
      {
        "name": "Bob Smith",
        "email": "bob@test.com",
        "phoneNumber": "9876543211",
        "gpa": 3.5
      }
    ]
  }' | jq .
```

Expected response:
```json
{
  "successes": [
    {
      "id": 1,
      "name": "Alice Johnson",
      "action": "High performer summarized"
    },
    {
      "id": 2,
      "name": "Bob Smith",
      "action": "Created"
    }
  ],
  "errors": [],
  "duplicates": [],
  "summary": "Batch Processing Complete:\n✅ Created: 2 students\n⚠️ Duplicates: 0 (skipped)\n❌ Errors: 0 (invalid data)\n📊 Total Rows: 2"
}
```

---

## What Happens Under the Hood

### 1. Request Arrives at Backend
```
HTTP POST /api/batch/create-students
↓
BatchController.createStudentsInBatch()
↓
batchCreateAgent.processBatch()
```

### 2. Agent Processes Each Record
```
For each student:
  1️⃣ Load existing emails (via MCP queryAll)
  2️⃣ Validate: email, phone, GPA
  3️⃣ Check duplicate: existingEmails.contains(email)
  4️⃣ Create student (via MCP execute INSERT)
  5️⃣ If GPA ≥ 3.7: Generate AI summary
  6️⃣ Return success/error record
```

### 3. MCP Communication
```
Agent calls: dbhubMcp.execute("INSERT INTO student ...", params)
  ↓
DBHubMCPClient builds JSON-RPC request:
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "execute",
    "arguments": {
      "sql": "INSERT INTO student ...",
      "params": [...]
    }
  }
}
  ↓
Sends via stdio to DBHub MCP server process
  ↓
MCP server executes SQL against PostgreSQL
  ↓
Returns JSON-RPC response:
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {"rowsAffected": 1}
}
  ↓
Agent continues with next record
```

### 4. Response Returns to Frontend
```
BatchCreateResult {
  successes: [...],
  errors: [...],
  duplicates: [...],
  summary: "..."
}
  ↓
JSON response sent to Angular
  ↓
Modal displays results
  ↓
Student list auto-refreshes
```

---

## Monitor Logs

### Backend Logs (Terminal 2)
```
# MCP Server Starting
INFO com.example.mcp.DBHubMCPClient - DBHubMCPClient initialized - Host: localhost, User: arpit, Database: hello_world_db
INFO com.example.mcp.DBHubMCPClient - DBHubMCPClient: Starting DBHub MCP server process...
INFO com.example.mcp.DBHubMCPClient - DBHubMCPClient: MCP server started successfully

# API Request
INFO com.example.controller.BatchController - API: POST /api/batch/create-students - 2 records received

# Agent Processing
INFO com.example.agent.BatchCreateAgent - Agent: BATCH_CREATE starting - 2 records to process
INFO com.example.agent.BatchCreateAgent - Agent: Loading existing emails from database via DBHub MCP
INFO com.example.mcp.DBHubMCPClient - DBHubMCPClient: Querying (all) via MCP: SELECT email FROM student

# Student Creation via MCP
INFO com.example.mcp.DBHubMCPClient - DBHubMCPClient: Executing SQL via MCP: INSERT INTO student ...
INFO com.example.mcp.DBHubMCPClient - DBHubMCPClient: Sending JSON-RPC request ID 3 - Method: tools/call, Tool: execute

# AI Summary
INFO com.example.agent.BatchCreateAgent - Agent: High performer detected (GPA 3.9), generating AI summary

# Complete
INFO com.example.agent.BatchCreateAgent - Agent: BATCH_CREATE complete - 2 successful, 0 errors, 0 duplicates
```

---

## Key Architecture Points

### ✅ Real Agent Pattern
- **Iterates** through student records
- **Validates** each one autonomously
- **Decides** what to do (create, skip, summarize)
- **Takes actions** (INSERT, SELECT, call OpenAI)
- **Logs reasoning** at each step

### ✅ Proper MCP Implementation
- **Not JDBC Wrapper** - Real JSON-RPC client
- **External Process** - DBHub runs as separate process
- **Tool Calling** - Agent invokes database as a "tool"
- **Standard Protocol** - JSON-RPC 2.0 compliant

### ✅ Separation of Concerns
```
Layer 1: Agent (Decision making, iteration, reasoning)
Layer 2: MCP Client (JSON-RPC communication)
Layer 3: MCP Server (DBHub - DB operations)
Layer 4: Database (PostgreSQL - storage)
```

---

## Troubleshooting

### Backend won't start: "Failed to start DBHub MCP server"
```bash
# Install DBHub globally
npm install -g dbhub

# Verify installation
npx dbhub --version
```

### PostgreSQL connection refused
```bash
# Start PostgreSQL
brew services start postgresql

# Verify connection
psql -h localhost -U arpit -d hello_world_db -c "SELECT 1;"
```

### MCP server closed connection
```bash
# Check PostgreSQL is running
ps aux | grep postgres

# Restart PostgreSQL
brew services restart postgresql
```

### Build errors
```bash
# Clean compile
cd backend
mvn clean compile

# Run with debug
mvn clean compile -X
```

---

## Files Modified/Created

| File | Change |
|------|--------|
| `backend/mcp/DBHubMCPClient.java` | ✨ NEW - JSON-RPC MCP client |
| `backend/agent/BatchCreateAgent.java` | 🔄 Updated to use DBHubMCPClient |
| `E2E-FLOW.md` | 📝 Added MCP Integration section |
| `MCP-SETUP.md` | ✨ NEW - Complete setup guide |
| `QUICK-START-MCP.md` | ✨ NEW - This file |

---

## Next Steps

1. ✅ Verify all 3 terminals running (PostgreSQL, Backend, Frontend)
2. ✅ Test batch API (curl or UI)
3. ✅ Watch backend logs for MCP communication
4. ✅ Check results modal or curl response
5. ✅ Extend agent with more decision logic as needed

---

## Reference

- Full setup details: [MCP-SETUP.md](MCP-SETUP.md)
- Flow documentation: [E2E-FLOW.md](E2E-FLOW.md)
- Agent code: `backend/src/main/java/com/example/agent/BatchCreateAgent.java`
- MCP client: `backend/src/main/java/com/example/mcp/DBHubMCPClient.java`
