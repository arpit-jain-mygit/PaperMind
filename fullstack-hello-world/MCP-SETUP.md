# DBHub MCP Integration Guide

## Overview

The Batch Create Agent now uses **DBHub MCP (Model Context Protocol)** to communicate with PostgreSQL via JSON-RPC 2.0 protocol instead of direct JDBC connections.

**Key Benefits:**
- Proper MCP abstraction layer
- Agent makes tool calls to external MCP server
- JSON-RPC 2.0 communication protocol
- Separation of concerns: agent logic ≠ database logic

## Architecture

```
BatchCreateAgent
        ↓
DBHubMCPClient (Component)
        ↓
JSON-RPC 2.0 Protocol
        ↓
DBHub MCP Server Process
        ↓
PostgreSQL (Database)
```

## Setup Prerequisites

### 1. Install Node.js & npm

```bash
# Check if Node.js is installed
node --version
npm --version

# If not installed, install Node.js from https://nodejs.org/
```

### 2. Install DBHub MCP Server

```bash
# Install DBHub globally
npm install -g dbhub
```

## Configuration

### Environment Variables

Set these variables before running the Spring Boot application:

```bash
# PostgreSQL connection details
export DBHUB_HOST=localhost
export DBHUB_USER=arpit
export DBHUB_PASSWORD=1234
export DBHUB_DATABASE=hello_world_db
```

Or add to `.env` file in project root:
```
DBHUB_HOST=localhost
DBHUB_USER=arpit
DBHUB_PASSWORD=1234
DBHUB_DATABASE=hello_world_db
```

## Running the Application

### Step 1: Ensure PostgreSQL is Running

```bash
# Check if PostgreSQL is running
psql -h localhost -U arpit -d hello_world_db -c "SELECT 1;"

# If not running, start PostgreSQL (macOS with Homebrew)
brew services start postgresql
```

### Step 2: Start Spring Boot Backend

```bash
cd /Users/arpit/Documents/claude/transcription-workspace/fullstack-hello-world/backend

# Set environment variables (or use .env)
export DBHUB_HOST=localhost
export DBHUB_USER=arpit
export DBHUB_PASSWORD=1234
export DBHUB_DATABASE=hello_world_db

# Run Spring Boot (Maven)
mvn spring-boot:run
```

**What happens:**
- Spring Boot starts and initializes the application context
- DBHubMCPClient bean is created
- Constructor of DBHubMCPClient automatically:
  1. Reads environment variables for database config
  2. Starts DBHub MCP server process via `npx dbhub serve ...`
  3. Establishes stdio pipes for JSON-RPC communication
  4. Logs confirmation: "MCP server started successfully"

### Step 3: Start Angular Frontend (Optional)

```bash
cd /Users/arpit/Documents/claude/transcription-workspace/fullstack-hello-world/frontend

ng serve
```

Navigate to `http://localhost:4200` and use the batch import UI.

## How It Works

### JSON-RPC Communication Flow

#### Request Example
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "execute",
    "arguments": {
      "sql": "INSERT INTO student (name, email, phone_number, gpa) VALUES (?, ?, ?, ?)",
      "params": ["John Doe", "john@example.com", "555-1234", 3.8]
    }
  }
}
```

#### Response Example
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "rowsAffected": 1
  }
}
```

### DBHubMCPClient Methods

| Method | Purpose | Example |
|--------|---------|---------|
| `execute(sql, params)` | INSERT/UPDATE/DELETE | `dbhubMcp.execute("INSERT INTO ...", args)` |
| `queryOne(sql, params)` | Get single row | `dbhubMcp.queryOne("SELECT * FROM ... WHERE id = ?", 1)` |
| `queryAll(sql, params)` | Get multiple rows | `dbhubMcp.queryAll("SELECT * FROM ...")` |
| `exists(sql, params)` | Check if row exists | `dbhubMcp.exists("SELECT 1 FROM ... WHERE email = ?", email)` |

### Batch Agent Integration

The agent uses DBHubMCPClient for:

1. **Load existing emails** (to detect duplicates)
   ```java
   List<Map<String, Object>> students = dbhubMcp.queryAll("SELECT email FROM student");
   ```

2. **Create student** (execute INSERT)
   ```java
   dbhubMcp.execute("INSERT INTO student (...) VALUES (...)", params);
   ```

3. **Fetch created student** (for AI summary)
   ```java
   Map<String, Object> row = dbhubMcp.queryOne("SELECT * FROM student WHERE email = ?", email);
   ```

## Logs

Monitor these logs to understand what's happening:

```bash
# Backend logs
# Look for lines like:
# - "DBHubMCPClient: Starting DBHub MCP server process..."
# - "DBHubMCPClient: MCP server started successfully"
# - "DBHubMCPClient: Sending JSON-RPC request ID 1 - Method: tools/call, Tool: execute"
# - "DBHubMCPClient: Execute completed - 1 rows affected"
```

## Testing

### Test 1: Verify MCP Server Starts

Watch Spring Boot startup logs for:
```
INFO com.example.mcp.DBHubMCPClient - DBHubMCPClient initialized - Host: localhost, User: arpit, Database: hello_world_db
INFO com.example.mcp.DBHubMCPClient - DBHubMCPClient: Starting DBHub MCP server process...
INFO com.example.mcp.DBHubMCPClient - DBHubMCPClient: MCP server started successfully
```

### Test 2: Use Batch API

```bash
curl -X POST http://localhost:8080/api/batch/create-students \
  -H "Content-Type: application/json" \
  -d '{
    "students": [
      {
        "name": "Alice Smith",
        "email": "alice@example.com",
        "phoneNumber": "555-0001",
        "gpa": 3.9
      },
      {
        "name": "Bob Jones",
        "email": "bob@example.com",
        "phoneNumber": "555-0002",
        "gpa": 3.5
      }
    ]
  }'
```

Expected response:
```json
{
  "successes": [
    { "id": 1, "name": "Alice Smith", "action": "High performer summarized" },
    { "id": 2, "name": "Bob Jones", "action": "Created" }
  ],
  "errors": [],
  "duplicates": [],
  "summary": "Batch Processing Complete:\n✅ Created: 2 students\n⚠️  Duplicates: 0 (skipped)\n❌ Errors: 0 (invalid data)\n📊 Total Rows: 2"
}
```

## Troubleshooting

### Issue: "Failed to start DBHub MCP server"

**Cause:** DBHub not installed globally
**Fix:**
```bash
npm install -g dbhub
```

### Issue: "MCP server closed connection"

**Cause:** MCP server crashed or lost connection
**Fix:**
1. Check PostgreSQL is running: `psql -h localhost -U arpit -d hello_world_db -c "SELECT 1;"`
2. Check network connectivity
3. Restart Spring Boot application

### Issue: "Connection refused on port 5432"

**Cause:** PostgreSQL not running
**Fix:**
```bash
# macOS
brew services start postgresql

# Linux (systemd)
sudo systemctl start postgresql

# Or manually start
postgres -D /usr/local/var/postgres
```

### Issue: PostgreSQL credentials rejected

**Cause:** Wrong username/password in environment variables
**Fix:**
```bash
# Verify credentials work
psql -h localhost -U arpit -d hello_world_db

# If prompted for password, enter: 1234
```

## Architecture Deep Dive

### Why MCP for Database Access?

Traditional approach (❌ Direct JDBC):
```
Agent → DriverManager.getConnection() → Direct SQL → PostgreSQL
```
Problems:
- No abstraction layer
- Credentials hardcoded or injected directly
- Agent tightly coupled to JDBC implementation
- Not following agent tool-calling pattern

Proper MCP approach (✅ JSON-RPC):
```
Agent → JSON-RPC Request → MCP Server Process → Database Driver → PostgreSQL
```
Benefits:
- Clean separation between agent and database logic
- MCP server handles all database concerns
- Agent treats database as a "tool" it calls
- Easy to swap database or MCP implementation
- Follows Model Context Protocol standard

### JSON-RPC 2.0 Standard

Each request/response pair follows JSON-RPC 2.0 spec:

**Request Structure:**
- `jsonrpc`: Always "2.0"
- `id`: Unique request ID (for matching responses)
- `method`: The RPC method (e.g., "tools/call")
- `params`: Parameters for the method

**Response Structure:**
- `jsonrpc`: Always "2.0"
- `id`: Matches request ID
- `result`: Successful result OR `error` field if failed

### DBHubMCPClient Component Lifecycle

1. **Bean Creation** → Constructor called
2. **Initialize MCP** → ProcessBuilder starts `npx dbhub serve`
3. **Establish Pipes** → stdio connected for JSON-RPC
4. **Agent Requests** → Each call sends JSON-RPC request
5. **Server Processing** → DBHub handles SQL and DB interaction
6. **Response Parsing** → JSON response parsed and returned
7. **Application Shutdown** → Spring calls `close()` method

## Production Considerations

### Security

- **Never commit credentials** to git
- **Use environment variables** (as shown above)
- **In production**, use secrets manager:
  ```bash
  export DBHUB_PASSWORD=$(aws secretsmanager get-secret-value --secret-id db-password --query SecretString --output text)
  ```

### Performance

- DBHub MCP server runs as separate process (good isolation)
- JSON-RPC adds slight overhead vs direct JDBC (negligible for batch operations)
- Connection pooling handled by MCP server automatically

### Scaling

- Single MCP server can handle multiple concurrent requests
- If needed, can run multiple DBHub instances with load balancing
- For now, single instance is sufficient for this application

## Next Steps

1. Run the application and verify logs show "MCP server started successfully"
2. Test batch API endpoint with sample data
3. Monitor agent logs to see JSON-RPC communication
4. Extend agent with more complex decision logic as needed

## Files Involved

| File | Purpose |
|------|---------|
| `backend/src/main/java/com/example/mcp/DBHubMCPClient.java` | JSON-RPC MCP client |
| `backend/src/main/java/com/example/agent/BatchCreateAgent.java` | Agent using DBHubMCPClient |
| `backend/src/main/java/com/example/controller/BatchController.java` | REST endpoint |
| `frontend/src/app/services/student.service.ts` | UI service |
| `frontend/src/app/app.component.ts` | Batch UI component |

## References

- [Model Context Protocol](https://modelcontextprotocol.io/)
- [JSON-RPC 2.0 Specification](https://www.jsonrpc.org/specification)
- [DBHub GitHub](https://github.com/bytebase/dbhub)
- [DBHub NPM Package](https://www.npmjs.com/package/dbhub)
