# GenAI & LLM Integration Guide

## Table of Contents

1. [MCP (Model Context Protocol)](#mcp-model-context-protocol)
2. [Agents vs Tools](#agents-vs-tools)
3. [Workflow Orchestration](#workflow-orchestration)
4. [Best Practices](#best-practices)

---

## MCP (Model Context Protocol)

### What is MCP?

**MCP** = Standard protocol for LLMs to call external tools/APIs

```
Any LLM → MCP Protocol → Your Services
```

Works with:
- Claude ✓
- GPT-4 ✓
- Any LLM ✓

### When to Use MCP

**MCP is good when:**
```
LLM orchestrates workflow
  ↓
LLM decides what to do next
  ↓
LLM calls your APIs/tools (via MCP)
  ↓
LLM sees result
  ↓
LLM decides next step
```

### Real-World Example: Invoice Processing Workflow

**Scenario:** User says to Claude: "Process these invoices and update accounting"

#### Without MCP (User orchestrates):
```
User → "Extract invoice" → API → Gemini → Result
User → "Validate data" → API → Check → Result
User → "Categorize" → API → Assign code → Result
User → "Update CRM" → API → Record → Result

Problem: User must manage each step
```

#### With MCP (LLM orchestrates):
```
Claude (Agent) receives: "Process invoices"
  ↓
Claude thinks: "I need to extract first"
Claude calls Tool: extract_invoice()
  ↓ MCP Protocol
Your Service: Returns {"vendor": "Acme Corp", "amount": 5000}
  ↓
Claude thinks: "Now validate this"
Claude calls Tool: validate_data()
  ↓ MCP Protocol
Your Service: Returns {"valid": true}
  ↓
Claude thinks: "Now categorize"
Claude calls Tool: categorize_expense()
  ↓ MCP Protocol
Your Service: Returns {"account_code": "ACC-001"}
  ↓
Claude thinks: "Now update accounting"
Claude calls Tool: update_accounting_system()
  ↓ MCP Protocol
Your Service: Records in QuickBooks
  ↓
Claude thinks: "Now notify approver"
Claude calls Tool: send_notification()
  ↓ MCP Protocol
Your Service: Sends email
  ↓
Claude: "Done! Processed 10 invoices, updated accounting, notified approvers"
```

---

## Agents vs Tools

### Key Distinction

```
Claude = AGENT (orchestrator, decides what to do)
  ↓
extract_invoice() = TOOL (Claude calls it)
validate_data() = TOOL (Claude calls it)
categorize_expense() = TOOL (Claude calls it)
update_accounting_system() = TOOL (Claude calls it)
send_notification() = TOOL (Claude calls it)
```

**Important:** Not 5 agents. **1 agent + 5 tools.**

### Manager & Worker Analogy

```
Claude = Manager (orchestrates, makes decisions)

Tool 1 = Worker A (extract invoices)
Tool 2 = Worker B (validate data)
Tool 3 = Worker C (categorize expenses)
Tool 4 = Worker D (update systems)
Tool 5 = Worker E (send notifications)

Manager: "Worker A, extract the invoice"
Worker A: "Done! Here's the data"

Manager: "Worker B, validate it"
Worker B: "Done! It's valid"

Manager: "Worker C, categorize it"
Worker C: "Done! Account code is ACC-001"

Manager: "Worker D, update accounting"
Worker D: "Done! Updated QuickBooks"

Manager: "Worker E, notify approver"
Worker E: "Done! Email sent"

Manager: "All done! 10 invoices processed"
```

### With MCP

```
Each tool becomes an MCP Server function
Claude calls them via MCP protocol

Claude is STILL the agent (orchestrator)
Tools are STILL just functions
MCP is the communication protocol
```

**Your MCP Server:**
```python
from mcp.server import Server

server = Server("invoice-processor")

@server.tool()
def extract_invoice(file_path: str) -> dict:
    """Extract invoice data"""
    return {
        "vendor": "Acme Corp",
        "amount": 5000.00,
        "date": "2024-06-23"
    }

@server.tool()
def validate_data(data: dict) -> dict:
    """Validate invoice data"""
    return {"valid": True, "errors": []}

@server.tool()
def categorize_expense(amount: float) -> dict:
    """Categorize expense by amount"""
    return {"account_code": "ACC-001", "category": "Office Supplies"}

@server.tool()
def update_accounting_system(data: dict) -> dict:
    """Update QuickBooks or accounting system"""
    return {"status": "updated", "transaction_id": "TXN-123"}

@server.tool()
def send_notification(recipient: str, message: str) -> dict:
    """Send notification to approver"""
    return {"status": "sent", "timestamp": "2024-06-23T10:00:00Z"}
```

**Claude uses all these tools:**
```
Claude: "I'll extract, validate, categorize, update, and notify"
Claude → extract_invoice("invoice.pdf") → MCP → Tool executes
Claude → validate_data(extracted) → MCP → Tool executes
Claude → categorize_expense(5000) → MCP → Tool executes
Claude → update_accounting_system(data) → MCP → Tool executes
Claude → send_notification("approver@company.com", message) → MCP → Tool executes
Claude: "Done! Processed successfully"
```

---

## Tools Can Call ANY System

### Key Insight

```
MCP doesn't care WHAT the tool calls
It only cares that tool returns a result

Each tool = wrapper around external system
Tool can call:
✓ APIs (REST, GraphQL)
✓ Databases (SQL, NoSQL)
✓ Message queues (Kafka, RabbitMQ)
✓ External services (SendGrid, Stripe, AWS)
✓ Internal microservices
✓ File systems
✓ Anything with a client library
```

### Real Example: Tools Calling Different Systems

```python
from mcp.server import Server
import requests
import redis
from kafka import KafkaProducer
import sendgrid

server = Server("invoice-processor")

@server.tool()
def extract_invoice(file_path: str) -> dict:
    """Extract invoice data"""
    # Calls Gemini API
    response = requests.post(
        "https://gemini-api/extract",
        json={"file": file_path}
    )
    return response.json()

@server.tool()
def validate_data(data: dict) -> dict:
    """Validate invoice data"""
    # Calls your internal validation microservice API
    response = requests.post(
        "http://validation-service:8000/validate",
        json=data
    )
    return response.json()

@server.tool()
def categorize_expense(amount: float) -> dict:
    """Categorize expense"""
    # Queries PostgreSQL database directly
    r = redis.Redis(host='localhost')
    cached = r.get(f"category:{amount}")
    if cached:
        return json.loads(cached)
    
    # Fall back to DB query
    result = db.query(
        "SELECT account_code FROM expense_categories WHERE min <= ? AND max >= ?",
        amount
    )
    return {"account_code": result[0]}

@server.tool()
def update_accounting_system(data: dict) -> dict:
    """Update accounting system"""
    # Publishes to Kafka topic → accounting microservice consumes
    producer = KafkaProducer(bootstrap_servers=['localhost:9092'])
    producer.send('accounting-topic', value=data)
    return {"status": "published"}

@server.tool()
def send_notification(recipient: str, message: str) -> dict:
    """Send notification"""
    # Calls SendGrid email API
    sg = sendgrid.SendGridAPIClient("SENDGRID_API_KEY")
    response = sg.client.mail.send.post(request_body={
        "personalizations": [{"to": [{"email": recipient}]}],
        "from": {"email": "noreply@company.com"},
        "subject": "Invoice Processed",
        "content": [{"type": "text/plain", "value": message}]
    })
    return {"status": "sent", "message_id": response.headers.get("X-Message-Id")}
```

### Visualization

```
Claude (Agent orchestrates)
  ↓
  ├→ Tool 1 (extract_invoice)
  │    → Calls Gemini API
  │
  ├→ Tool 2 (validate_data)
  │    → Calls internal validation microservice
  │
  ├→ Tool 3 (categorize_expense)
  │    → Queries PostgreSQL DB (with Redis cache)
  │
  ├→ Tool 4 (update_accounting_system)
  │    → Publishes to Kafka
  │         ↓
  │      Accounting microservice consumes event
  │
  └→ Tool 5 (send_notification)
       → Calls SendGrid API
```

### For VoxAlchemy Example

```
If you had agentic document processing:

Claude orchestrates:

Tool 1: extract_document()
  → Calls your Gemini extraction API

Tool 2: validate_quality()
  → Calls internal validation API

Tool 3: categorize_document()
  → Queries PostgreSQL (document types)

Tool 4: update_user_account()
  → Publishes to Kafka → Billing service consumes

Tool 5: notify_user()
  → Calls SendGrid API
```

### Key Takeaway

```
Tools are just wrappers
They hide the complexity of different backends
Claude orchestrates at a high level
Backend can be anything: APIs, DBs, Kafka, etc.
```

---

## Workflow Orchestration

### User Orchestrates (Current VoxAlchemy)

```
User is the decision-maker

User → "Extract this PDF" → API → Gemini → Result
User → "Validate result" → API → Check → Result
User → "Download output" → API → Return → File
```

**Pros:**
- Simple, predictable
- User has full control
- Clear error handling

**Cons:**
- User must manage each step
- Multi-step workflows are tedious

### LLM Orchestrates (With MCP)

```
LLM is the decision-maker

User: "Process invoices end-to-end"
Claude → Tool 1 → Tool 2 → Tool 3 → Tool 4 → Tool 5
Claude: "Done! Here's your report"
```

**Pros:**
- Multi-step workflows are seamless
- LLM handles complexity
- Natural language interface

**Cons:**
- Less control over each step
- Harder to debug
- More expensive (more API calls)

---

## Best Practices

### When to Use MCP

**Use MCP if:**
```
✓ LLM orchestrates complex multi-step workflows
✓ User says "do this end-to-end" (not step-by-step)
✓ You want agentic AI (Claude makes decisions)
✓ You want to switch between LLMs easily
```

**Don't use MCP if:**
```
✗ User controls workflow (simple transactional)
✗ Direct API is simpler
✗ Performance is critical (MCP adds latency)
✗ Single-step operations
```

### For VoxAlchemy

**Current architecture (CORRECT):**
```
User → Web UI → API → Gemini → Result
```

**If you wanted LLM orchestration:**
```
Claude → MCP → extract_invoice() → Gemini
      ↓ MCP → validate_data()
      ↓ MCP → categorize_expense()
      ↓ MCP → update_accounting()
      ↓ MCP → send_notification()
```

**Verdict:** Your current approach is right for transactional use. MCP would be overkill.

---

## Summary

| Concept | Definition | Example |
|---------|-----------|---------|
| **Agent** | Orchestrates workflow, makes decisions | Claude (the LLM) |
| **Tool** | Does specific task, called by agent | extract_invoice(), validate_data() |
| **MCP** | Protocol for agent to call tools | LLM → Tool via MCP Server |
| **Workflow** | Sequence of tools executed by agent | Extract → Validate → Categorize → Update |

---

## Interview Answer

### Q: "What's MCP and when would you use it?"

> "MCP (Model Context Protocol) is a standard protocol that lets LLMs call external tools and APIs.
> 
> **Key insight:** MCP is useful when an **LLM orchestrates a workflow**, not when a user does.
> 
> **Example:**
> User: \"Process these invoices and update accounting\"
> 
> Claude (the agent) orchestrates:
> 1. Call extract_invoice() tool
> 2. Call validate_data() tool
> 3. Call categorize_expense() tool
> 4. Call update_accounting_system() tool
> 5. Call send_notification() tool
> 
> Claude sees each result and decides the next step.
> 
> **For VoxAlchemy:**
> - Current: User orchestrates (upload → process → download) ✓
> - Wouldn't use MCP (users aren't LLM agents)
> - Would use MCP only if: \"Let Claude handle document processing end-to-end\"
> 
> **When to use MCP:**
> - Multi-step agentic workflows
> - LLM is the decision-maker
> - Not for simple transactional APIs"

---
