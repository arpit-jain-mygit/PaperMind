# PaperMind: Option A - Document Upload Flow

## Real Example: Arpit's Passport Upload

### **Step 1: User Uploads Passport**

```
User Action:
curl -X POST "http://localhost:8000/api/documents/upload" \
  -F "user_id=arpit_001" \
  -F "doc_type=passport" \
  -F "file=@passport.pdf"

Input Data:
├─ user_id: "arpit_001"
├─ doc_type: "passport"
└─ file: passport.pdf (binary bytes)
```

---

### **Step 2: OCR Text Extraction**

```
Backend Process:
pdfplumber.open(pdf_bytes)
  └─→ Extract text from pages
      └─→ No text found (image-based PDF)
          └─→ Fallback to Google Document AI

Google Document AI Process:
Project: ultimate-bit-502715-k9
Processor: c1aecfacb43fff0f
Input: passport.pdf bytes

EXTRACTED TEXT (1051 chars):
───────────────────────────────────────
इस पासपोर्ट में 36 पृष्ठ है। This passport contains 36 pages.
भारत गणराज्य REPUBLIC OF INDIA
पासपोर्ट नं./ Passport No. → M1783676
उपनाम / Surname → JAIN
दिया गया नाम / Given Name(s) → ARPIT KUMAR
राष्ट्रीयता / Nationality → INDIAN
जन्म स्थान / Place of Birth → GUNA, MADHYA PRADESH
जन्म की तिथि / Date of Birth → 25/11/1980
जारी करने की तिथि / Date of Issue → 30/09/2014
समाप्ति की तिथि / Date of Expiry → 29/09/2024
───────────────────────────────────────
```

**Key Point:** Two-stage extraction strategy:
- Fast path: pdfplumber (milliseconds)
- Fallback: Google Document AI (2-3 seconds, 99%+ accuracy)

---

### **Step 3: Store in PostgreSQL**

#### **3a. Create Document Record**

```sql
TABLE: documents
──────────────────────────────────────────────────────────
id              │ doc-9a140b96
user_id         │ arpit_001
filename        │ passport.pdf
doc_type        │ passport
file_size       │ 245678 bytes
encrypted_file_path │ s3://papermind/arpit_001/passport.pdf.encrypted
upload_status   │ processing → completed
created_at      │ 2026-09-22 10:15:30
──────────────────────────────────────────────────────────

Foreign Key: user_id (links to Users table)
```

**Purpose:** Central registry of all documents. Document ID (`doc-9a140b96`) becomes the key reference for all downstream data.

---

#### **3b. Store Extracted Text & Metadata**

```sql
TABLE: extractions
──────────────────────────────────────────────────────────
id              │ ext-a1b2c3d4
doc_id          │ doc-9a140b96  ← FK to documents
user_id         │ arpit_001     ← FK to users
doc_type        │ passport
raw_text        │ (1051 chars of extracted passport data)
extracted_data  │ {
                │   "raw_text": "...",
                │   "extraction_method": "google_document_ai",
                │   "document_type": "passport"
                │ }
confidence_score│ 0.99
created_at      │ 2026-09-22 10:15:32
──────────────────────────────────────────────────────────

Foreign Keys:
  ├─ doc_id → documents(id)
  └─ user_id → users(id)
```

**Connection:** Extractions table **stores the OCR output** linked to the Document via `doc_id`. The `raw_text` field is what gets embedded next.

---

### **Step 4: Generate Embeddings**

```
OpenAI API Call:
model: text-embedding-3-small
input: (first 8191 chars of raw_text from Extraction table)
─────────────────────────────────────
"इस पासपोर्ट में 36 पृष्ठ है। This passport contains 36 pages.
भारत गणराज्य REPUBLIC OF INDIA
पासपोर्ट नं. M1783676
उपनाम JAIN
दिया गया नाम ARPIT KUMAR
..."

Output: 1536-dimensional vector
─────────────────────────────────────
embedding_vector = [
  0.0234, -0.0567, 0.0891, -0.0123, 0.0456, ...(1536 values total)
]

Why 1536? OpenAI's text-embedding-3-small model produces 1536 dimensions.
Each dimension captures semantic meaning of the text.
```

**Connection:** Convert OCR text (from Extraction table) into numerical vectors for semantic search.

---

### **Step 5: Store in Qdrant Vector DB**

```
Qdrant Cloud (Vector Database)
Collection: papermind-documents_vectors
URL: https://a71761f5-71a6-4d6e-bae7-72e0fb037b0c.eu-west-1-0.aws.cloud.qdrant.io

Insert Point:
┌─────────────────────────────────────────────────────┐
│ Point ID: qdrant-xyz789                             │
├─────────────────────────────────────────────────────┤
│ Vector (1536-dim):                                  │
│ [0.0234, -0.0567, 0.0891, -0.0123, 0.0456, ...]   │
├─────────────────────────────────────────────────────┤
│ Metadata (Payload):                                 │
│ {                                                   │
│   "doc_id": "doc-9a140b96",                         │
│   "user_id": "arpit_001",                           │
│   "doc_type": "passport",                           │
│   "filename": "passport.pdf",                       │
│   "upload_date": "2026-09-22T10:15:30"              │
│ }                                                   │
└─────────────────────────────────────────────────────┘

Search Type: COSINE similarity (measures angle between vectors)
Distance Metric: Closer vectors = more similar semantic meaning
```

**Connection:** Qdrant stores the embeddings with metadata (doc_id, user_id). This enables fast semantic search without touching PostgreSQL.

---

### **Step 6: Link Everything (Vector Mappings)**

```sql
TABLE: vector_mappings
──────────────────────────────────────────────────────────
id              │ vm-e5f6g7h8  ← Primary Key
doc_id          │ doc-9a140b96 ← FK to documents
user_id         │ arpit_001    ← FK to users
qdrant_id       │ qdrant-xyz789 ← Points to Qdrant vector
doc_type        │ passport
text_chunk      │ "इस पासपोर्ट में 36 पृष्ठ..." (first 1000 chars)
embedding_model │ openai-text-embedding-3-small
created_at      │ 2026-09-22 10:15:34
──────────────────────────────────────────────────────────

Foreign Keys:
  ├─ doc_id → documents(id)
  └─ user_id → users(id)

External Reference:
  └─ qdrant_id → Qdrant.papermind-documents_vectors[point_id]
```

**Connection:** Bridge between PostgreSQL (structured data) and Qdrant (vector data). This mapping table allows:
- Fast user-filtered queries (filter by user_id in PostgreSQL)
- Vector similarity searches (search in Qdrant)
- Context retrieval (fetch raw_text from Extraction via doc_id)

---

## Complete Data Flow Diagram

```
┌────────────────────┐
│  passport.pdf      │
│  (User Upload)     │
└─────────┬──────────┘
          │
          ▼
    ┌──────────────┐
    │  OCR Engine  │
    │ (Google AI)  │
    └──────┬───────┘
           │
    Extracted Text:
    "M1783676, JAIN, ARPIT KUMAR, 29/09/2024..."
           │
     ┌─────┴──────┐
     │            │
     ▼            ▼
┌──────────────┐  ┌──────────────────────────┐
│ PostgreSQL   │  │   OpenAI Embeddings      │
│              │  │   text-embedding-3-small │
├──────────────┤  └────────┬─────────────────┘
│ documents    │           │
│ (id: doc-..  │   embedding_vector
│  user_id:... │   [0.0234, -0.0567, ...]
│  filename... │   (1536 dimensions)
│  upload_...) │           │
│              │           ▼
│ extractions  │    ┌─────────────────────┐
│ (doc_id:..   │    │   Qdrant Cloud      │
│  raw_text... │    │   Vector Database   │
│  confidence) │    ├─────────────────────┤
│              │    │ Point ID: qdrant-.. │
│ vector_      │    │ Vector: [0.0234...] │
│ mappings     │    │ Metadata: {doc_id,  │
│ (doc_id:..   │◄───┤  user_id, doc_type} │
│  qdrant_id)  │    └─────────────────────┘
└──────────────┘
```

---

## Connection Map (How Tables Talk)

```
users (arpit_001)
   │
   ├─→ documents (doc-9a140b96)
   │   ├─→ extractions (ext-a1b2c3d4)
   │   │   └─→ raw_text: "M1783676, JAIN, ARPIT KUMAR..."
   │   │
   │   └─→ vector_mappings (vm-e5f6g7h8)
   │       ├─→ doc_id: doc-9a140b96 (references documents)
   │       ├─→ qdrant_id: qdrant-xyz789 (reference to Qdrant)
   │       └─→ text_chunk: first 1000 chars
   │
   └─→ (External) Qdrant Cloud
       └─→ Point ID: qdrant-xyz789
           ├─→ Vector: [0.0234, -0.0567, ...]
           └─→ Metadata: {doc_id, user_id, doc_type}
```

---

## Why These Connections?

| Connection | Reason | Benefit |
|-----------|--------|---------|
| **Document → Extraction** | Track which OCR output came from which file | Audit trail, re-processing capability |
| **Document → VectorMapping** | Link file to its vector | Bidirectional lookup (file ↔ vector) |
| **VectorMapping → Qdrant ID** | Bridge PostgreSQL and Qdrant | Decouple storage, independent scaling |
| **user_id everywhere** | Multi-tenant isolation | Each user only sees their own documents |
| **Metadata in Qdrant** | Avoid extra DB queries during search | Fast filtering without round-trip to PostgreSQL |
| **Extraction.raw_text** | Store full OCR output | Retrieved for LLM context in queries |

---

**Complete Upload Sequence:**

```
1. Receive upload request (user_id, doc_type, file)
2. Extract text (pdfplumber → Google Document AI)
3. Create Document record in PostgreSQL (id: doc-9a140b96)
4. Create Extraction record (stores raw_text)
5. Generate embedding vector (OpenAI)
6. Store vector in Qdrant (point_id: qdrant-xyz789)
7. Create VectorMapping record (links doc to qdrant_id)
8. Update Document.upload_status to "completed"
9. Return success response with doc_id
```

---

# PaperMind: Option B - Query/RAG Flow

## Real Example: Arpit's Passport Query

When user asks: **"Can I travel to USA next week?"**

---

### **Step 1: User Submits Query**

```
User Action:
curl -X POST "http://localhost:8000/api/query" \
  -F "user_id=arpit_001" \
  -F "question=Can I travel to USA next week?"

Input Data:
├─ user_id: "arpit_001"
└─ question: "Can I travel to USA next week?"

Context:
└─ Current Date: 2026-09-22 (passed to LLM)
```

**Connection:** The query is tied to a specific user (`arpit_001`), so the system knows to search only their documents.

---

### **Step 2: Generate Query Embedding**

```
OpenAI API Call:
model: text-embedding-3-small
input: "Can I travel to USA next week?"
─────────────────────────────────────

Output: 1536-dimensional vector
─────────────────────────────────────
query_embedding = [
  0.0123, -0.0456, 0.0789, -0.0234, 0.0567, ...(1536 values total)
]

Why: Convert question into same vector space as document embeddings
     so we can measure semantic similarity.
```

**Connection:** Question embedding is in the **same 1536-dimensional space** as the document embeddings generated in Option A Step 4.

---

### **Step 3: Semantic Search in Qdrant**

```
Qdrant Query:
Collection: papermind-documents_vectors
Search Vector: [0.0123, -0.0456, 0.0789, ...]
Filter: metadata.user_id = "arpit_001"  ← Only arpit_001's docs
Top K: 3  ← Return top 3 most similar documents
Distance Metric: COSINE similarity

Search Process:
    Query Vector [0.0123, -0.0456, ...]
           │
           ├─→ Compare with doc-9a140b96 vector [0.0234, -0.0567, ...]
           │   Similarity Score: 0.92 ✅ (MATCH - about passport)
           │
           ├─→ Compare with doc-xxx vector [0.0345, ...]
           │   Similarity Score: 0.45 ❌ (No match - about tax returns)
           │
           └─→ Compare with doc-yyy vector [...]
               Similarity Score: 0.38 ❌ (No match - about salary slip)

Results Returned:
┌─────────────────────────────────────────┐
│ Point ID: qdrant-xyz789                 │
├─────────────────────────────────────────┤
│ Similarity Score: 0.92                  │
├─────────────────────────────────────────┤
│ Metadata:                               │
│ {                                       │
│   "doc_id": "doc-9a140b96",             │
│   "user_id": "arpit_001",               │
│   "doc_type": "passport",               │
│   "filename": "passport.pdf"            │
│ }                                       │
└─────────────────────────────────────────┘
```

**Connection:** Qdrant returns the `doc_id` (doc-9a140b96) which becomes the key to fetch data from PostgreSQL in the next step.

---

### **Step 4: Retrieve Context from PostgreSQL**

```
Using doc_id from Qdrant search: doc-9a140b96

Query PostgreSQL:
SELECT raw_text FROM extractions WHERE doc_id = 'doc-9a140b96'

Retrieved Context (1051 chars):
───────────────────────────────────────
इस पासपोर्ट में 36 पृष्ठ है। This passport contains 36 pages.
भारत गणराज्य REPUBLIC OF INDIA
पासपोर्ट नं. M1783676
उपनाम JAIN
दिया गया नाम ARPIT KUMAR
राष्ट्रीयता INDIAN
जन्म स्थान GUNA, MADHYA PRADESH
जन्म की तिथि 25/11/1980
जारी करने की तिथि 30/09/2014
समाप्ति की तिथि 29/09/2024  ← KEY: Expiry date
───────────────────────────────────────

Additional Info Added:
├─ Today's Date: 2026-09-22  ← Critical for date comparison
└─ Document Type: passport   ← Context for LLM
```

**Connection:** Extraction table (`extractions.raw_text`) is where the actual OCR content lives. Qdrant found the document, PostgreSQL provides the full text.

---

### **Step 5: Call GPT-4o-mini with RAG Context**

```
OpenAI API Call:
model: gpt-4o-mini
temperature: 0.7
max_tokens: 500

System Prompt (from prompts.py):
────────────────────────────────────────
"You are a document analyzer. You MUST check expiry dates 
against today's date. If any date in the document is BEFORE 
today, the document is EXPIRED and invalid."
────────────────────────────────────────

User Prompt:
────────────────────────────────────────
"TODAY'S DATE: 2026-09-22

User's Question: Can I travel to USA next week?

Document Context:
पासपोर्ट नं. M1783676
समाप्ति की तिथि 29/09/2024
[... full extracted text ...]

CRITICAL RULES - FOLLOW EXACTLY:
1. TODAY'S DATE is 2026-09-22. Use this to check if dates have passed.
2. If Expiry Date < Today's Date → Document is EXPIRED.
3. If Expiry Date > Today's Date → Document is VALID.
4. A VALID (not expired) passport is required for travel.
5. Always compare dates mathematically.
6. If passport is expired, clearly state 'Your passport expired 
   on [date] and is no longer valid for travel.'"
────────────────────────────────────────

LLM Processing:
1. Extract Expiry Date: 29/09/2024
2. Compare: 29/09/2024 < 2026-09-22? → YES, EXPIRED
3. Generate Answer: "Your passport expired on 29/09/2024 and 
   is no longer valid for travel. You cannot travel to USA 
   next week without renewing it."
```

**Connection:** The LLM has:
- Question from user
- Context from PostgreSQL (raw_text)
- Current date (passed explicitly)
- Clear instructions about date comparison
- Document type (passport)

Result: Intelligent, contextual answer.

---

### **Step 6: Store Chat History**

```sql
TABLE: chat_history
──────────────────────────────────────────────────────────
id              │ chat-30f5e6b3
user_id         │ arpit_001  ← FK to users
message         │ "Can I travel to USA next week?"
response        │ "Your passport expired on 29/09/2024..."
cited_docs      │ ["doc-9a140b96"]  ← What documents were used
model_used      │ "gpt-4o-mini"
tokens_used     │ 245  ← For cost tracking
created_at      │ 2026-09-22 10:15:45
──────────────────────────────────────────────────────────

Foreign Key:
  └─ user_id → users(id)
```

**Connection:** Chat history links back to:
- User (via user_id)
- Documents used (via cited_docs array)
- Provides audit trail of all queries

---

## Complete Query Flow Diagram

```
┌────────────────────────┐
│  "Can I travel to USA  │
│  next week?"           │
│  (User Question)       │
└────────┬───────────────┘
         │
         ▼
    ┌──────────────────────────┐
    │ OpenAI Embeddings        │
    │ text-embedding-3-small   │
    └────────┬─────────────────┘
             │
      query_embedding:
      [0.0123, -0.0456, ...]
             │
             ▼
    ┌──────────────────────────┐
    │ Qdrant Semantic Search   │
    │ Filter: user_id =        │
    │ arpit_001                │
    │ Top K: 3                 │
    └────────┬─────────────────┘
             │
     Returns: doc_id = doc-9a140b96
             │
     ┌───────┴─────────┐
     │                 │
     ▼                 ▼
┌──────────────┐  ┌──────────────────┐
│ PostgreSQL   │  │ Current Date     │
│ extractions  │  │ 2026-09-22       │
├──────────────┤  └──────────────────┘
│ raw_text:    │           │
│ "Expiry:     │           │
│ 29/09/2024"  │           │
└──────┬───────┘           │
       │                   │
       └───────┬───────────┘
               │
               ▼
    ┌──────────────────────────┐
    │  GPT-4o-mini             │
    │  (RAG + Date Logic)      │
    │                          │
    │  Input: Q + Context +    │
    │         Today's Date     │
    │         + Instructions   │
    └────────┬─────────────────┘
             │
      Response:
      "Passport expired on 29/09/2024.
       Cannot travel."
             │
             ▼
    ┌──────────────────────────┐
    │ PostgreSQL chat_history  │
    │ Store: question, answer, │
    │        cited_docs        │
    └──────────────────────────┘
```

---

## Query Connection Map

```
users (arpit_001)
   │
   ├─→ Submits Question
   │   "Can I travel to USA next week?"
   │
   ├─→ Question → Embedding [0.0123, ...]
   │
   ├─→ Qdrant Search (with user_id filter)
   │   └─→ Returns: qdrant-xyz789
   │       Metadata: doc_id = doc-9a140b96
   │
   ├─→ PostgreSQL Lookup
   │   ├─→ Extraction: raw_text = "Expiry: 29/09/2024..."
   │   └─→ Document: filename = passport.pdf
   │
   ├─→ GPT-4o-mini Processing
   │   ├─→ Input: Question + raw_text + Today's Date
   │   ├─→ Logic: Compare 29/09/2024 < 2026-09-22
   │   └─→ Output: "Passport expired..."
   │
   └─→ chat_history Record
       ├─→ message: Question
       ├─→ response: Answer
       ├─→ cited_docs: [doc-9a140b96]
       └─→ user_id: arpit_001
```

---

## Why These Connections?

| Step | Connection | Purpose | Benefit |
|------|-----------|---------|---------|
| **Query → Embedding** | Convert text to vectors | Match question with documents | Semantic understanding, not keyword matching |
| **Query Embedding → Qdrant** | Search similar vectors | Find relevant documents | Sub-100ms response, scale to millions of docs |
| **Qdrant → doc_id** | Return document reference | Know which document matched | Fetch full context without re-searching |
| **doc_id → PostgreSQL** | Retrieve raw_text | Get full extracted content | LLM has complete context for accurate answers |
| **Extractions + Date → LLM** | Provide context + current date | Enable date comparisons | LLM can understand temporal validity |
| **LLM Output → ChatHistory** | Store question + answer + docs | Create audit trail | Track all queries, debug issues, cost tracking |
| **cited_docs array** | Link answer to source documents | Transparency | User knows which documents were used |

---

## Complete Query Sequence

```
1. User asks question (user_id, question)
2. Generate embedding of question (OpenAI)
3. Filter Qdrant by user_id
4. Search Qdrant with question embedding (top 3)
5. Get doc_id from Qdrant metadata
6. Fetch raw_text from PostgreSQL Extraction table
7. Get current date (2026-09-22)
8. Call GPT-4o-mini with:
   - System Prompt (from prompts.py)
   - User Prompt (Question + Context + Today's Date + Rules)
9. LLM analyzes: Compare dates, check validity, generate answer
10. Store in ChatHistory: message, response, cited_docs, tokens_used
11. Return to user: answer + cited_documents
```

Each step depends on the previous one, enabling intelligent document understanding.
