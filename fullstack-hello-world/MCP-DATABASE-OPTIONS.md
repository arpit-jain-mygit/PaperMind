# MCP Database Options: SQL vs MongoDB

## Current Setup: DBHub MCP (SQL Only)

| Database | DBHub Support | Status |
|----------|---------------|--------|
| PostgreSQL | ✅ YES | ✓ Configured |
| MySQL | ✅ YES | Not used |
| SQLite | ✅ YES | Not used |
| MariaDB | ✅ YES | Not used |
| **MongoDB** | ❌ **NO** | Not supported |

**Why?** DBHub is SQL-focused. MongoDB uses completely different query language (BSON documents, not SQL).

---

## MongoDB MCP Options

### Option 1: MongoDB Native Driver MCP
**Status:** Community MCP (not official, but available)
- GitHub: Various implementations available
- Approach: Direct MongoDB connection via MCP
- Query Style: JavaScript/BSON-based (not SQL)

### Option 2: pREST MCP + MongoDB Adapter
**Status:** pREST supports PostgreSQL natively
- Could add MongoDB support via separate adapter
- More complex setup

### Option 3: Build Custom MongoDB MCP
**Status:** Would need to build from scratch
- Full control over queries
- More development effort

---

## Code Abstraction: SQL vs MongoDB

### Current: SQL-based (DBHubMCPClient)
```java
// SQL with prepared statements
dbhubMcp.execute(
  "INSERT INTO student (name, email, gpa) VALUES (?, ?, ?)",
  "Alice",
  "alice@test.com",
  3.9
);

dbhubMcp.queryAll(
  "SELECT * FROM student WHERE gpa > ?"
  3.7
);
```

### MongoDB Equivalent (Would Look Different)
```java
// Document-based queries (NOT SQL)
mongoMcp.insertOne(
  "student",
  {
    "name": "Alice",
    "email": "alice@test.com",
    "gpa": 3.9
  }
);

mongoMcp.findAll(
  "student",
  { "gpa": { "$gt": 3.7 } }
);
```

**Key Differences:**
```
SQL Approach:
├─ String SQL queries with ? placeholders
├─ Rows returned as Map<String, Object>
├─ Prepared statements for parameterization
└─ RDBMS semantics (JOINs, normalization)

MongoDB Approach:
├─ BSON documents (JSON-like objects)
├─ Results as List<Document> or Map
├─ Query operators ($gt, $eq, $in, etc.)
└─ Document semantics (embedding, denormalization)
```

---

## Why Abstraction CANNOT Be Same

### 1. Query Syntax is Fundamentally Different

**SQL:**
```sql
INSERT INTO student (name, email, gpa) 
VALUES (?, ?, ?)
```

**MongoDB:**
```javascript
db.student.insertOne({
  name: "Alice",
  email: "alice@test.com",
  gpa: 3.9
})
```

### 2. Operations Have Different Signatures

**SQL Operations:**
- `execute(sql, params)` → Returns row count
- `queryOne(sql, params)` → Returns single row
- `queryAll(sql, params)` → Returns list of rows

**MongoDB Operations:**
- `insertOne(collection, document)` → Returns inserted ID
- `findOne(collection, filter)` → Returns single document
- `findAll(collection, filter)` → Returns list of documents
- `updateMany(collection, filter, update)` → Updates multiple
- `deleteOne(collection, filter)` → Deletes document

### 3. Parameter Handling is Different

**SQL:**
```java
// Positional parameters with ?
"SELECT * FROM student WHERE email = ? AND gpa > ?"
params: ["alice@test.com", 3.7]
```

**MongoDB:**
```java
// Query operators in filter document
{ 
  "email": "alice@test.com",
  "$and": [
    { "gpa": { "$gt": 3.7 } }
  ]
}
```

---

## Your System: Current Dual-Write Architecture

```
BatchCreateAgent
  ├─ Branch 1: DBHubMCPClient (SQL)
  │   ├─ execute("INSERT INTO student ...")
  │   └─ queryAll("SELECT * FROM student")
  │
  └─ Branch 2: MongoTemplate or Spring Data MongoDB (Direct)
      ├─ mongoTemplate.save(student)
      └─ mongoTemplate.find(query, Student.class)
```

**Currently:**
- PostgreSQL: Uses DBHubMCPClient (JSON-RPC MCP)
- MongoDB: Uses Spring Data MongoDB (direct, not MCP)

**Could be:**
- PostgreSQL: Uses DBHubMCPClient ✅
- MongoDB: Uses MongoDBMCPClient (hypothetical) ❌ Doesn't exist

---

## If You Wanted MongoDB via MCP

### Option A: Build Custom MongoDB MCP Client

**File: `backend/mcp/MongoDBMCPClient.java`**

```java
@Component
public class MongoDBMCPClient {

    public void insertOne(String collection, Map<String, Object> document) {
        // JSON-RPC call to MongoDB MCP server
        // Tool: insertOne
        // Params: { collection, document }
    }

    public Map<String, Object> findOne(String collection, Map<String, Object> filter) {
        // JSON-RPC call to MongoDB MCP server
        // Tool: findOne
        // Params: { collection, filter }
    }

    public List<Map<String, Object>> findAll(String collection, Map<String, Object> filter) {
        // JSON-RPC call to MongoDB MCP server
        // Tool: findAll
        // Params: { collection, filter }
    }

    public long updateMany(String collection, Map<String, Object> filter, Map<String, Object> update) {
        // JSON-RPC call to MongoDB MCP server
        // Tool: updateMany
        // Params: { collection, filter, update }
    }
}
```

**Usage in Agent:**
```java
// Different API, different abstraction
mongoMcp.insertOne("student", {
    "name": "Alice",
    "email": "alice@test.com",
    "gpa": 3.9
});

mongoMcp.findAll("student", {
    "gpa": { "$gt": 3.7 }
});
```

### Option B: Keep Current Approach (Recommended)

```java
// PostgreSQL: Via MCP (JSON-RPC)
dbhubMcp.execute("INSERT INTO student ...");

// MongoDB: Direct Spring Data (simpler)
mongoTemplate.save(student);
```

**Why this is better:**
- PostgreSQL gets abstraction via MCP ✅
- MongoDB direct is simpler and sufficient
- Consistent with team patterns
- No need to build custom MongoDB MCP

---

## Comparison Table

| Aspect | DBHub MCP (SQL) | MongoDB MCP (Custom) | MongoDB Direct |
|--------|-----------------|----------------------|-----------------|
| **Setup** | npm install -g dbhub | Build custom | Spring Data |
| **Query Style** | SQL with ? placeholders | BSON documents | Java objects/queries |
| **Code in Agent** | `dbhubMcp.execute(sql, params)` | `mongoMcp.insertOne(coll, doc)` | `mongoTemplate.save(doc)` |
| **Abstraction** | Proper MCP JSON-RPC | Would be proper MCP | Direct library |
| **Complexity** | Medium | High | Low |
| **Consistency** | Matches PostgreSQL pattern | Matches PostgreSQL pattern | Different pattern |

---

## Recommendation for Your System

### Current Setup (Recommended) ✅
```java
// PostgreSQL → DBHub MCP (JSON-RPC abstraction)
dbhubMcp.execute("INSERT INTO student ...", params);

// MongoDB → Spring Data MongoDB (Direct library)
studentMongoRepository.save(student);
```

**Pros:**
- PostgreSQL has proper MCP abstraction
- MongoDB keeps it simple
- Both patterns are widely accepted
- Minimal code changes

### Alternative (If Consistency Matters) 🔄
```java
// Both via MCP (requires building MongoDB MCP)
dbhubMcp.execute("INSERT INTO student ...", params);
mongoMcp.insertOne("student", document);
```

**Cons:**
- Must build custom MongoDB MCP
- Different query languages (SQL vs BSON)
- More development effort
- MongoDB MCP not standard yet

---

## Decision Matrix

Choose based on your priorities:

| Priority | Best Approach |
|----------|---------------|
| **Speed to market** | Keep MongoDB direct + DBHub for PostgreSQL |
| **Consistency** | Build MongoDB MCP + DBHub for PostgreSQL |
| **Simplicity** | Keep MongoDB direct + DBHub for PostgreSQL |
| **Agent purity** | Build MongoDB MCP + DBHub (both MCP) |

---

## Current Code: How to Keep Both?

### BatchCreateAgent.java (No Changes Needed)
```java
@Autowired
private DBHubMCPClient dbhubMcp;  // PostgreSQL via MCP

@Autowired
private StudentMongoRepository studentMongoRepository;  // MongoDB direct

public void processBatch(List<StudentData> students) {
    // Load from PostgreSQL via MCP
    List<Map<String, Object>> pgStudents = dbhubMcp.queryAll(
        "SELECT email FROM student"
    );

    // Save to MongoDB direct
    for (StudentData data : students) {
        Student student = new Student();
        student.setName(data.name);
        // ... other fields
        
        studentMongoRepository.save(student);  // Direct MongoDB
    }
}
```

---

## Summary

| Question | Answer |
|----------|--------|
| Does DBHub support MongoDB? | ❌ No (SQL-only) |
| Are MongoDB MCPs available? | Partially (not standard) |
| Can code abstraction be same? | ❌ No (SQL vs BSON are different) |
| What's recommended? | DBHub for PostgreSQL, keep MongoDB direct |
| Should you build MongoDB MCP? | Only if consistency is critical |

---

## References

- DBHub GitHub: https://github.com/bytebase/dbhub
- pREST GitHub: https://github.com/prest/prest
- MongoDB MCP Status: No official standard yet
- Your System: PostgreSQL (DBHub MCP) + MongoDB (Direct) ✅
