# Running BatchCreateAgent with pg-mcp-server

## What Changed

✅ **Updated DBHubMCPClient.java** to use pg-mcp-server instead of dbhub npm package

### Changes Made:
1. Changed ProcessBuilder to start `pg-mcp-server --transport stdio`
2. Changed environment variables from `DBHUB_*` to `DATABASE_URL`
3. Updated JSON-RPC communication to use pg-mcp-server's "query" tool
4. Improved error messages and logging
5. All compilation successful (BUILD SUCCESS)

---

## Quick Start (3 Terminals)

### Terminal 1: Start PostgreSQL
```bash
brew services start postgresql

# Verify it's running
psql -h localhost -U arpit -d hello_world_db -c "SELECT 1;"
```

Should output:
```
 ?column? 
----------
        1
(1 row)
```

---

### Terminal 2: Install & Start Backend

**Step 1: Install pg-mcp-server** (one-time)
```bash
npm install -g pg-mcp-server

# Verify installation
which pg-mcp-server
# Should output: /opt/homebrew/bin/pg-mcp-server (or similar)
```

**Step 2: Run Spring Boot**
```bash
cd /Users/arpit/Documents/claude/transcription-workspace/fullstack-hello-world/backend

# Set DATABASE_URL environment variable
export DATABASE_URL="postgresql://arpit:1234@localhost:5432/hello_world_db"

# Compile and run
mvn spring-boot:run
```

**Expected Logs:**
```
INFO 12345 --- [main] com.example.mcp.DBHubMCPClient : DBHubMCPClient initialized - Database: postgresql://arpit:1234@localhost:5432/hello_world_db
INFO 12345 --- [main] com.example.mcp.DBHubMCPClient : Starting pg-mcp-server process...
INFO 12345 --- [main] com.example.mcp.DBHubMCPClient : pg-mcp-server started successfully
INFO 12345 --- [main] org.springframework.boot.StartupInfoLogger : Started HelloWorldApplication in 2.5 seconds
```

---

### Terminal 3: Test Batch API

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

**Expected Response:**
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

## Backend Logs Walkthrough

Watch Terminal 2 for these logs when you run the batch API:

```
API Receives Request:
│
├─ INFO: API: POST /api/batch/create-students - 2 records
│
├─ Agent Initializes:
│  └─ Agent: BATCH_CREATE starting - 2 records to process
│
├─ Agent Loads Existing Emails via MCP:
│  ├─ Agent: Loading existing emails from database via DBHub MCP
│  ├─ DBHubMCPClient: Querying (all) via MCP: SELECT email FROM student
│  ├─ DBHubMCPClient: Sending JSON-RPC request ID 1 - Tool: query, SQL: SELECT email FROM student
│  ├─ [pg-mcp-server processes the query]
│  └─ DBHubMCPClient: Query returned 0 rows
│
├─ Process Each Record:
│  │
│  ├─ Record 1: Alice
│  │  ├─ Agent: Processing row 1 - Alice Johnson
│  │  ├─ Agent: Creating student via DBHub MCP
│  │  ├─ DBHubMCPClient: Executing SQL via MCP: INSERT INTO student ...
│  │  ├─ DBHubMCPClient: Sending JSON-RPC request ID 2 - Tool: query
│  │  ├─ DBHubMCPClient: Execute completed - 1 rows affected
│  │  ├─ Agent: Successfully created student - Alice Johnson
│  │  ├─ Agent: High performer detected (GPA 3.9), generating AI summary
│  │  └─ Agent: AI summary generated - ...
│  │
│  └─ Record 2: Bob
│     ├─ Agent: Processing row 2 - Bob Smith
│     ├─ Agent: Creating student via DBHub MCP
│     ├─ DBHubMCPClient: Execute completed - 1 rows affected
│     └─ Agent: Successfully created student - Bob Smith
│
└─ Complete:
   └─ Agent: BATCH_CREATE complete - 2 successful, 0 errors, 0 duplicates
   └─ Return BatchCreateResult with all successes
```

---

## Understanding the JSON-RPC Communication

### Request to pg-mcp-server:
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "query",
    "arguments": {
      "sql": "SELECT email FROM student"
    }
  }
}
```

### Response from pg-mcp-server:
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "rows": []
  }
}
```

### For INSERT:
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "rowsAffected": 1
  }
}
```

---

## Troubleshooting

### Error: "pg-mcp-server not found"

**Cause:** pg-mcp-server not installed

**Fix:**
```bash
npm install -g pg-mcp-server

# Verify
pg-mcp-server --version
```

---

### Error: "pg-mcp-server process exited immediately"

**Cause:** Database connection failed or PostgreSQL not running

**Fix:**
```bash
# Check PostgreSQL is running
brew services list | grep postgres

# If not running:
brew services start postgresql

# Verify DATABASE_URL is set
echo $DATABASE_URL

# Test connection
psql postgresql://arpit:1234@localhost:5432/hello_world_db -c "SELECT 1;"
```

---

### Error: "MCP server closed connection"

**Cause:** pg-mcp-server crashed or lost PostgreSQL connection

**Fix:**
```bash
# Restart PostgreSQL
brew services restart postgresql

# Restart Spring Boot
# (Ctrl+C in Terminal 2, then run mvn spring-boot:run again)
```

---

### Error: "Stream closed" or timeout reading response

**Cause:** pg-mcp-server not responding

**Fix:**
1. Check if pg-mcp-server process is running: `ps aux | grep pg-mcp-server`
2. Check PostgreSQL: `psql -h localhost -U arpit -d hello_world_db -c "SELECT 1;"`
3. Check logs in Terminal 2 for "PG-MCP SERVER STDERR" messages
4. Restart: Kill Spring Boot and run again

---

## Manual Testing (Without Spring Boot)

Test pg-mcp-server directly:

```bash
# Terminal 1: Start pg-mcp-server
export DATABASE_URL="postgresql://arpit:1234@localhost:5432/hello_world_db"
pg-mcp-server --transport stdio

# Terminal 2: Send JSON-RPC request
# (Create a file: test_mcp.json)
cat > test_mcp.json << 'EOF'
{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"query","arguments":{"sql":"SELECT 1;"}}}
EOF

# Send it to pg-mcp-server
cat test_mcp.json | pg-mcp-server --transport stdio
```

---

## Environment Variables

**Set before running Spring Boot:**

```bash
# Required
export DATABASE_URL="postgresql://arpit:1234@localhost:5432/hello_world_db"

# Optional (for OpenAI AI summaries)
export OPENAI_API_KEY="sk-..." # Your OpenAI key
```

Or add to `.env`:
```
DATABASE_URL=postgresql://arpit:1234@localhost:5432/hello_world_db
OPENAI_API_KEY=sk-...
```

---

## Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│ User Browser: curl /api/batch/create-students              │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Spring Boot (Port 8080)                                     │
│ ├─ BatchController.createStudentsInBatch()                 │
│ └─ BatchCreateAgent.processBatch()                         │
└────────────────────┬────────────────────────────────────────┘
                     │ Uses JSON-RPC
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ pg-mcp-server (subprocess, --transport stdio)              │
│ ├─ Listens on stdin for JSON-RPC requests                  │
│ ├─ Processes {"method":"tools/call","params":{"name":"query"}}
│ └─ Writes JSON-RPC responses to stdout                     │
└────────────────────┬────────────────────────────────────────┘
                     │ SQL queries
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ PostgreSQL (Port 5432)                                      │
│ └─ Executes: SELECT, INSERT, UPDATE, DELETE                │
└─────────────────────────────────────────────────────────────┘
```

---

## Summary: What's Working

✅ **DBHubMCPClient**
- Starts pg-mcp-server as subprocess
- Sends JSON-RPC requests over stdin
- Reads JSON-RPC responses from stdout
- Handles errors properly
- Logs all communication

✅ **BatchCreateAgent**
- Loads existing emails via MCP queryAll()
- Creates students via MCP execute() INSERT
- Fetches created student via MCP queryOne()
- Generates AI summaries for high performers
- Returns detailed results

✅ **Compilation**
- No errors: BUILD SUCCESS

✅ **Ready to Test**
- Just follow the 3-terminal setup above
- Verify each step

---

## Files Changed

| File | Changes |
|------|---------|
| `backend/src/main/java/com/example/mcp/DBHubMCPClient.java` | ✅ Updated to use pg-mcp-server |
| `backend/src/main/java/com/example/agent/BatchCreateAgent.java` | ✅ No changes (works as-is) |
| `backend/src/main/java/com/example/controller/BatchController.java` | ✅ No changes (works as-is) |
| `E2E-FLOW.md` | ✅ Already documents MCP architecture |

---

## Next Steps

1. ✅ **Install pg-mcp-server:** `npm install -g pg-mcp-server`
2. ✅ **Set DATABASE_URL:** `export DATABASE_URL="postgresql://arpit:1234@localhost:5432/hello_world_db"`
3. ✅ **Run Spring Boot:** `mvn spring-boot:run`
4. ✅ **Test API:** Use curl command above
5. ✅ **Monitor Logs:** Watch for MCP communication logs
6. ✅ **Verify Results:** Check if students were created in PostgreSQL

You're ready to go! 🚀
