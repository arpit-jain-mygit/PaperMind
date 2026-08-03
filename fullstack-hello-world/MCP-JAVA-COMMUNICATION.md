# How to Call pg-mcp-server from Java

## Overview

pg-mcp-server runs as a **separate subprocess** that communicates with Java via **JSON-RPC 2.0 protocol** over **stdin/stdout**.

```
Java Process                    subprocess
┌──────────────────────────┐   ┌─────────────────────┐
│ BatchCreateAgent         │   │ pg-mcp-server       │
│                          │   │                     │
│ dbhubMcp.queryAll(sql)   │   │ Listens on stdin    │
│          ↓               │   │ Writes to stdout    │
│ Write JSON-RPC request   ├──→│ Processes request   │
│ to subprocess stdin      │   │ (talks to PostgreSQL)
│          ↑               │   │          ↓          │
│ Read JSON-RPC response   │←──┤ Write JSON response │
│ from subprocess stdout   │   │                     │
└──────────────────────────┘   └─────────────────────┘
```

---

## Step 1: Start pg-mcp-server as Subprocess

### Java Code:
```java
ProcessBuilder pb = new ProcessBuilder(
    "pg-mcp-server",
    "--transport", "stdio"
);

// Environment variable for database connection
pb.environment().put("DATABASE_URL", 
    "postgresql://arpit:1234@localhost:5432/hello_world_db");

// Redirect stderr to see any errors
pb.redirectErrorStream(false);

// Start the process
Process mcpProcess = pb.start();

// Get stdin/stdout for communication
BufferedWriter writer = new BufferedWriter(
    new OutputStreamWriter(mcpProcess.getOutputStream())
);

BufferedReader reader = new BufferedReader(
    new InputStreamReader(mcpProcess.getInputStream())
);
```

---

## Step 2: Send JSON-RPC Request

### What is JSON-RPC 2.0?

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "query",
    "arguments": {
      "sql": "SELECT * FROM student WHERE email = ?"
    }
  }
}
```

**Fields:**
- `jsonrpc`: Always "2.0" (protocol version)
- `id`: Unique request ID (for matching responses)
- `method`: Always "tools/call" (calling an MCP tool)
- `params.name`: Tool name ("query" for SQL execution)
- `params.arguments.sql`: The SQL to execute

### Java Code to Send Request:

```java
Map<String, Object> request = new HashMap<>();
request.put("jsonrpc", "2.0");
request.put("id", 1);  // Request ID
request.put("method", "tools/call");

Map<String, Object> params = new HashMap<>();
params.put("name", "query");  // Tool name

Map<String, Object> arguments = new HashMap<>();
arguments.put("sql", "SELECT * FROM student WHERE email = ?");
arguments.put("params", new Object[]{"alice@test.com"});

params.put("arguments", arguments);
request.put("params", params);

// Serialize to JSON
ObjectMapper mapper = new ObjectMapper();
String json = mapper.writeValueAsString(request);
System.out.println("Request: " + json);

// Send to pg-mcp-server stdin
writer.write(json);
writer.write("\n");  // Important: newline after each request
writer.flush();
```

**Output:**
```json
{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"query","arguments":{"sql":"SELECT * FROM student WHERE email = ?","params":["alice@test.com"]}}}
```

---

## Step 3: Read JSON-RPC Response

pg-mcp-server processes the request and writes response to stdout:

### Expected Response:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "rows": [
      {
        "id": 1,
        "name": "Alice",
        "email": "alice@test.com",
        "phone_number": "9876543210",
        "gpa": 3.9
      }
    ]
  }
}
```

### Java Code to Read Response:

```java
// Read response line from pg-mcp-server stdout
String responseLine = reader.readLine();
System.out.println("Response: " + responseLine);

// Parse JSON response
JsonNode responseNode = mapper.readTree(responseLine);

// Check for errors
if (responseNode.has("error")) {
    String errorMsg = responseNode.get("error").get("message").asText();
    throw new RuntimeException("MCP error: " + errorMsg);
}

// Extract result
JsonNode resultNode = responseNode.get("result");
List<Map<String, Object>> rows = mapper.convertValue(
    resultNode.get("rows"), 
    new TypeReference<List<Map<String, Object>>>(){}
);

// Process results
for (Map<String, Object> row : rows) {
    System.out.println("Name: " + row.get("name"));
    System.out.println("Email: " + row.get("email"));
}
```

**Output:**
```
Name: Alice
Email: alice@test.com
```

---

## Complete Example: Query Operation

### Scenario: Load all student emails for duplicate check

```java
public List<Map<String, Object>> loadExistingEmails() {
    // Build JSON-RPC request
    Map<String, Object> request = new HashMap<>();
    request.put("jsonrpc", "2.0");
    request.put("id", 1);
    request.put("method", "tools/call");
    
    Map<String, Object> params = new HashMap<>();
    params.put("name", "query");
    
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("sql", "SELECT email FROM student");
    
    params.put("arguments", arguments);
    request.put("params", params);
    
    // Send request
    try {
        String json = mapper.writeValueAsString(request);
        logger.info("Sending: {}", json);
        
        writer.write(json);
        writer.write("\n");
        writer.flush();
        
        // Read response
        String responseLine = reader.readLine();
        logger.info("Received: {}", responseLine);
        
        JsonNode responseNode = mapper.readTree(responseLine);
        
        // Check for error
        if (responseNode.has("error")) {
            String errorMsg = responseNode.get("error").get("message").asText();
            throw new RuntimeException("MCP error: " + errorMsg);
        }
        
        // Extract rows
        return mapper.convertValue(
            responseNode.get("result").get("rows"),
            new TypeReference<List<Map<String, Object>>>(){}
        );
        
    } catch (Exception e) {
        logger.error("Failed to load emails: {}", e.getMessage());
        throw new RuntimeException(e);
    }
}
```

**What happens:**

1. Java creates JSON-RPC request
2. Java writes to pg-mcp-server stdin
3. pg-mcp-server reads JSON
4. pg-mcp-server queries PostgreSQL
5. pg-mcp-server gets results
6. pg-mcp-server writes JSON response to stdout
7. Java reads from stdout
8. Java parses JSON response
9. Java extracts data

---

## All MCP Tools Available in pg-mcp-server

### Tool 1: query (SQL execution)

**Request:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "query",
    "arguments": {
      "sql": "SELECT * FROM student LIMIT 10"
    }
  }
}
```

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "rows": [...]
  }
}
```

---

## Mapping to Our Current Code

Our `DBHubMCPClient` already does exactly this! Here's how each method maps:

### Method 1: queryAll()

```java
public List<Map<String, Object>> queryAll(String sql, Object... params) {
    // 1. Build JSON-RPC request with tool="query"
    Map<String, Object> request = buildRequest("query", sql, params);
    
    // 2. Send to MCP server stdin
    String json = mapper.writeValueAsString(request);
    writer.write(json + "\n");
    writer.flush();
    
    // 3. Read response from MCP server stdout
    String responseLine = reader.readLine();
    
    // 4. Parse JSON-RPC response
    JsonNode responseNode = mapper.readTree(responseLine);
    
    // 5. Extract rows from result
    return mapper.convertValue(
        responseNode.get("result").get("rows"),
        List.class
    );
}
```

### Method 2: queryOne()

Same as queryAll(), but expects single row:

```java
public Map<String, Object> queryOne(String sql, Object... params) {
    // Same JSON-RPC communication...
    // But returns first row only
    List<Map<String, Object>> rows = ... // extract from response
    return rows.isEmpty() ? null : rows.get(0);
}
```

### Method 3: execute()

For INSERT/UPDATE/DELETE:

```java
public int execute(String sql, Object... params) {
    // Same JSON-RPC communication...
    // But extracts "rowsAffected" from response
    JsonNode result = responseNode.get("result");
    return result.get("rowsAffected").asInt();
}
```

---

## Complete Flow Diagram

```
BatchCreateAgent.processBatch()
    │
    ├─ Call: dbhubMcp.queryAll("SELECT email FROM student")
    │   │
    │   └─ DBHubMCPClient.queryAll()
    │       │
    │       ├─ Build JSON-RPC request
    │       │  {
    │       │    "jsonrpc": "2.0",
    │       │    "id": 1,
    │       │    "method": "tools/call",
    │       │    "params": {
    │       │      "name": "query",
    │       │      "arguments": {"sql": "SELECT email FROM student"}
    │       │    }
    │       │  }
    │       │
    │       ├─ Write to pg-mcp-server stdin
    │       │
    │       ├─ pg-mcp-server processes request
    │       │  └─ Queries PostgreSQL: SELECT email FROM student
    │       │
    │       ├─ pg-mcp-server writes response to stdout
    │       │  {
    │       │    "jsonrpc": "2.0",
    │       │    "id": 1,
    │       │    "result": {
    │       │      "rows": [
    │       │        {"email": "alice@test.com"},
    │       │        {"email": "bob@test.com"}
    │       │      ]
    │       │    }
    │       │  }
    │       │
    │       ├─ Read from pg-mcp-server stdout
    │       │
    │       ├─ Parse JSON response
    │       │
    │       └─ Return List<Map> with emails
    │
    └─ Agent continues with validation, creation, etc.
```

---

## Key Points

1. **Subprocess Communication:** pg-mcp-server runs as separate process, communicates via stdin/stdout

2. **JSON-RPC Protocol:** All communication is JSON-RPC 2.0 formatted
   - Request has: jsonrpc, id, method, params
   - Response has: jsonrpc, id, result (or error)

3. **Newline Delimited:** Each JSON request/response ends with newline `\n`

4. **Blocking I/O:** reader.readLine() blocks until response arrives

5. **No Code Changes Needed:** Our current `DBHubMCPClient` structure already handles all of this!

---

## Error Handling

If pg-mcp-server returns error:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "error": {
    "code": -32600,
    "message": "Invalid request"
  }
}
```

Java code detects it:

```java
if (responseNode.has("error")) {
    String errorMsg = responseNode.get("error").get("message").asText();
    int errorCode = responseNode.get("error").get("code").asInt();
    throw new RuntimeException("MCP error (" + errorCode + "): " + errorMsg);
}
```

---

## Summary

**Calling pg-mcp-server from Java:**

1. **Start process:** `ProcessBuilder("pg-mcp-server", "--transport", "stdio")`
2. **Get streams:** stdin for writing, stdout for reading
3. **Build request:** HashMap → JSON-RPC format
4. **Send:** Write JSON to stdin + newline + flush
5. **Receive:** Read line from stdout
6. **Parse:** Parse JSON response
7. **Extract:** Get data from `result` field
8. **Repeat:** For each database operation

**Our current `DBHubMCPClient` already does all of this!**

Just need to change the ProcessBuilder command from:
```java
"npx", "dbhub", "serve", "--db-type", "postgres", ...
```

To:
```java
"pg-mcp-server", "--transport", "stdio"
```

Everything else stays the same! ✅
