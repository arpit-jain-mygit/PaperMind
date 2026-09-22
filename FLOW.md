# PaperMind: Application Flows

## Table of Contents
1. [Upload Flow](#upload-flow)
   - [Step 1: User Uploads Passport](#step-1-user-uploads-passport)
   - [Step 2: OCR Text Extraction](#step-2-ocr-text-extraction)
   - [Step 3: Store in PostgreSQL](#step-3-store-in-postgresql)
   - [Step 4: Generate Embeddings](#step-4-generate-embeddings)
   - [Step 5: Store in Qdrant Vector DB](#step-5-store-in-qdrant-vector-db)
   - [Step 6: Link Everything (Vector Mappings)](#step-6-link-everything-vector-mappings)
   - [Data Flow Diagram](#complete-data-flow-diagram)
   - [Connection Map](#connection-map-how-tables-talk)
   - [Why These Connections](#why-these-connections)

2. [Query Flow](#query-flow)
   - [Step 1: User Submits Query](#step-1-user-submits-query)
   - [Step 2: Generate Query Embedding](#step-2-generate-query-embedding)
   - [Step 3: Semantic Search in Qdrant](#step-3-semantic-search-in-qdrant)
   - [Step 4: Retrieve Context from PostgreSQL](#step-4-retrieve-context-from-postgresql)
   - [Step 5: Call GPT-4o-mini with RAG Context](#step-5-call-gpt-4o-mini-with-rag-context)
   - [Step 6: Store Chat History](#step-6-store-chat-history)
   - [Query Flow Diagram](#complete-query-flow-diagram)
   - [Connection Map](#query-connection-map)
   - [Why These Connections](#why-these-connections-1)

---

# Upload Flow

## Real Example: Arpit's Passport Upload

### **Step 1: User Uploads Passport**

**Caller:** `FastAPI HTTP Handler` (app.py)
```
POST /api/documents/upload
    ↓
FastAPI Route Handler (@app.post("/api/documents/upload"))
    └─ Function: upload_endpoint()
```

**Input:**
```
HTTP POST request
├─ user_id: "arpit_001" (string)
├─ doc_type: "passport" (string)
└─ file: passport.pdf (binary file, ~245KB)
```

**Output:** (MEMORY ONLY)
```
file_content: bytes (245,678 bytes in RAM)
filename: "passport.pdf" (extracted from file)
Status: Ready for next step

Note: NOT persisted to database yet - exists only in memory
```

**Next Step Caller:** services.py → process_document_upload()

---

### **Step 2: OCR Text Extraction**

**Caller:** `services.process_document_upload()` (Python function)
```
Called by: FastAPI endpoint (Step 1)
    ↓
services.py → process_document_upload()
    ├─ Calls: extract_text_from_pdf(file_content)
    │  ├─ Tries: pdfplumber.open(file_content) [Fast path]
    │  └─ Falls back: extract_text_with_ocr() [Accurate path]
    │
    └─ extract_text_with_ocr()
       ├─ Initializes: google.cloud.documentai.DocumentProcessorServiceClient()
       ├─ Project: ultimate-bit-502715-k9
       ├─ Processor: c1aecfacb43fff0f
       └─ Returns: extracted_text string
```

**Input:** (MEMORY)
```
file_content: bytes (245,678 bytes from Step 1, in RAM)
filename: "passport.pdf"
extraction_strategy: Try pdfplumber first, then Google Document AI
```

**Output:** (MEMORY ONLY)
```
extracted_text: string (1051 characters)
├─ Content:
│  ├─ "इस पासपोर्ट में 36 पृष्ठ है। This passport contains 36 pages."
│  ├─ "पासपोर्ट नं. M1783676"
│  ├─ "उपनाम JAIN"
│  ├─ "दिया गया नाम ARPIT KUMAR"
│  ├─ "राष्ट्रीयता INDIAN"
│  ├─ "जन्म की तिथि 25/11/1980"
│  └─ "समाप्ति की तिथि 29/09/2024" ← Key data for queries
├─ extraction_method: "google_document_ai" (string)
└─ confidence_score: 0.99 (float)

Note: Still in memory - will be persisted in Step 3
```

**Key Point:** Two-stage extraction strategy:
- Fast path: pdfplumber (milliseconds) - for digital PDFs with text
- Fallback: Google Document AI (2-3 seconds, 99%+ accuracy) - for image-based PDFs

**Next Step Caller:** services.process_document_upload() continues

---

### **Step 3: Store in PostgreSQL**

#### **3a. Create Document Record**

**Caller:** `services.process_document_upload()` (continuing same function)
```
services.py → process_document_upload()
    ├─ Creates: Document ORM object
    └─ Calls: db.add(document)
       └─ Commits: db.commit() ← First database write
```

**Input:**
```
user_id: "arpit_001" (string)
filename: "passport.pdf" (string)
doc_type: "passport" (string)
file_size: 245678 (integer, bytes)
upload_status: "processing" (string)
```

**Output:** (PERSISTED TO PostgreSQL)
```
PostgreSQL documents table INSERT:
├─ id: "doc-9a140b96" ✅ (DB) ← Generated UUID-like ID
├─ user_id: "arpit_001" (FK to users table)
├─ filename: "passport.pdf"
├─ doc_type: "passport"
├─ file_size: 245678
├─ encrypted_file_path: "s3://papermind/arpit_001/passport.pdf.encrypted"
├─ upload_status: "processing"
└─ created_at: "2026-09-22 10:15:30" (timestamp)

Key: doc_id = "doc-9a140b96" ← Used by all subsequent steps
```

**Purpose:** Central registry of all documents. Document ID becomes the key reference for all downstream data (extractions, embeddings, mappings).

---

#### **3b. Store Extracted Text & Metadata**

**Caller:** `services.process_document_upload()` (continuing same function)
```
services.py → process_document_upload()
    ├─ Creates: Extraction ORM object
    └─ Calls: db.add(extraction)
       └─ Commits: db.commit() ← Second database write
```

**Input:**
```
doc_id: "doc-9a140b96" (FK to documents table)
user_id: "arpit_001" (FK to users table)
doc_type: "passport" (string)
raw_text: "इस पासपोर्ट में 36 पृष्ठ है..." (1051 chars from Step 2, memory)
extraction_method: "google_document_ai" (string)
confidence_score: 0.99 (float)
```

**Output:** (PERSISTED TO PostgreSQL)
```
PostgreSQL extractions table INSERT:
├─ id: "ext-a1b2c3d4" ✅ (DB)
├─ doc_id: "doc-9a140b96" ✅ (FK to documents)
├─ user_id: "arpit_001" ✅ (FK to users)
├─ doc_type: "passport"
├─ raw_text: "इस पासपोर्ट में 36 पृष्ठ है..." ✅ (DB) ← 1051 chars
├─ extracted_data: {
│  ├─ "raw_text": "...",
│  ├─ "extraction_method": "google_document_ai",
│  └─ "document_type": "passport"
│ } (JSON)
├─ confidence_score: 0.99
└─ created_at: "2026-09-22 10:15:32" (timestamp)

Key: extraction_id = "ext-a1b2c3d4" ← Contains raw_text for embedding + queries
```

**Connection:** Extractions table **stores the OCR output** linked to the Document via `doc_id`. The `raw_text` field is used:
- In Step 4: Embedded for semantic search
- In Query Flow: Retrieved for LLM context

---

### **Step 4: Generate Embeddings**

**Caller:** `services.process_document_upload()` (continuing same function)
```
services.py → process_document_upload()
    ├─ Calls: get_openai_client() ← Lazy initialization
    │
    └─ openai_client.embeddings.create(
       ├─ model: "text-embedding-3-small"
       ├─ input: raw_text[:8191]  ← From Step 2 (memory)
       └─ Returns: embedding_response
```

**Input:** (MEMORY)
```
raw_text: "इस पासपोर्ट में 36 पृष्ठ है..." (first 8191 chars, from Step 2 memory)
model: "text-embedding-3-small" (config)
API: OpenAI embeddings endpoint
```

**Output:** (MEMORY ONLY)
```
embedding_response object:
└─ embedding_vector: [0.0234, -0.0567, 0.0891, -0.0123, 0.0456, ...] 
   ├─ Dimensions: 1536
   ├─ Type: array of floats
   ├─ Represents: Semantic meaning of passport text
   └─ Still in RAM - not persisted yet

Why 1536? OpenAI's text-embedding-3-small model produces 1536 dimensions.
Each dimension captures semantic meaning of the text in vector space.
```

**Connection:** Convert OCR text (from Step 2 memory) into numerical vectors in a semantic space where similar documents have similar embeddings. This enables fast similarity search in Qdrant.

**Next Step Caller:** vector_db.store_embedding() (called from services.process_document_upload())

---

### **Step 5: Store in Qdrant Vector DB**

**Caller:** `vector_db.store_embedding()` (Python function)
```
Called by: services.process_document_upload()
    ↓
services.py → process_document_upload()
    ├─ Calls: store_embedding(
    │  ├─ doc_id: "doc-9a140b96"  ← From Step 3 (DB)
    │  ├─ user_id: "arpit_001"    ← From Step 1 (input)
    │  └─ vector: [0.0234, ...]   ← From Step 4 (memory)
    │
    └─ vector_db.py → store_embedding()
       ├─ Calls: get_qdrant_client_instance() ← Lazy init
       │
       └─ qdrant_client.upsert(
          ├─ collection_name: "papermind-documents_vectors"
          └─ points: [PointStruct(...)]  ← Sends to Qdrant Cloud
```

**Input:**
```
embedding_vector: [0.0234, -0.0567, 0.0891, ...] (1536 floats from Step 4, memory)
doc_id: "doc-9a140b96" (from Step 3, fetched from DB)
user_id: "arpit_001" (from input, Step 1)
doc_type: "passport" (string)
filename: "passport.pdf" (string)
upload_date: "2026-09-22T10:15:30" (ISO timestamp)
```

**Output:** (PERSISTED TO QDRANT CLOUD)
```
Qdrant Cloud (papermind-documents_vectors collection):

Point Object:
├─ point_id: "qdrant-xyz789" ✅ (Qdrant) ← Generated by Qdrant
├─ vector: [0.0234, -0.0567, 0.0891, -0.0123, 0.0456, ...] ✅ (1536 dims)
├─ payload (metadata): {
│  ├─ "doc_id": "doc-9a140b96" ✅
│  ├─ "user_id": "arpit_001" ✅
│  ├─ "doc_type": "passport" ✅
│  ├─ "filename": "passport.pdf" ✅
│  └─ "upload_date": "2026-09-22T10:15:30" ✅
│ }
└─ distance_metric: COSINE (measures angle between vectors)

Key: qdrant_id = "qdrant-xyz789" ← Bridge back to PostgreSQL
```

**Connection:** Qdrant stores the embeddings with metadata (doc_id, user_id). This enables:
- Fast semantic search (<100ms) on vector similarity
- Filtering by user_id without querying PostgreSQL
- Complete independence from PostgreSQL (can scale separately)

**Next Step Caller:** services.process_document_upload() continues

---

### **Step 6: Link Everything (Vector Mappings)**

**Caller:** `services.process_document_upload()` (continuing same function)
```
services.py → process_document_upload()
    ├─ Creates: VectorMapping ORM object
    │  ├─ doc_id: "doc-9a140b96" ← From Step 3 (DB)
    │  ├─ user_id: "arpit_001"
    │  └─ qdrant_id: "qdrant-xyz789" ← From Step 5 (Qdrant)
    │
    └─ Calls: db.add(vector_mapping)
       └─ Commits: db.commit() ← Final database write
```

**Input:**
```
doc_id: "doc-9a140b96" (FK to documents table, from Step 3)
qdrant_id: "qdrant-xyz789" (Qdrant point ID, from Step 5)
user_id: "arpit_001" (string)
doc_type: "passport" (string)
text_chunk: "इस पासपोर्ट में 36 पृष्ठ..." (first 1000 chars for preview)
embedding_model: "openai-text-embedding-3-small" (string)
```

**Output:** (PERSISTED TO PostgreSQL)
```
PostgreSQL vector_mappings table INSERT:
├─ id: "vm-e5f6g7h8" ✅ (DB)
├─ doc_id: "doc-9a140b96" ✅ (FK) ← Links to documents table
├─ user_id: "arpit_001" ✅ (FK) ← Links to users table
├─ qdrant_id: "qdrant-xyz789" ✅ (DB) ← Bridge to Qdrant vector
├─ doc_type: "passport"
├─ text_chunk: "इस पासपोर्ट में 36 पृष्ठ..." (1000 chars for preview)
├─ embedding_model: "openai-text-embedding-3-small"
└─ created_at: "2026-09-22 10:15:34" (timestamp)

Key: mapping_id = "vm-e5f6g7h8" ← Bridge between PostgreSQL and Qdrant
```

**Connection:** Bridge between PostgreSQL (structured data) and Qdrant (vector data). This mapping table enables:
- Fast user-filtered queries (filter by user_id in PostgreSQL before searching)
- Vector similarity searches (search in Qdrant with metadata)
- Context retrieval (fetch raw_text from Extraction via doc_id for LLM)
- Complete data lineage tracking (doc → extraction → embedding)

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

# Query Flow

## Real Example: Arpit's Passport Query

When user asks: **"Can I travel to USA next week?"**

---

### **Step 1: User Submits Query**

**Caller:** `FastAPI HTTP Handler` (app.py)
```
POST /api/query
    ↓
FastAPI Route Handler (@app.post("/api/query"))
    └─ Function: query_endpoint()
```

**Input:**
```
HTTP POST request
├─ user_id: "arpit_001" (string)
└─ question: "Can I travel to USA next week?" (string)
```

**Output:** (MEMORY ONLY)
```
question_data: object (in RAM)
├─ user_id: "arpit_001"
├─ question: "Can I travel to USA next week?"
├─ current_date: "2026-09-22" (server-provided context)
└─ Status: Ready for embedding

Note: NOT persisted yet - exists only in memory during request processing
```

**Next Step Caller:** services.query_documents()

**Connection:** The query is tied to a specific user (`arpit_001`), so the system searches only their documents.

---

### **Step 2: Generate Query Embedding**

**Caller:** `services.query_documents()` (Python function)
```
Called by: FastAPI endpoint (Step 1)
    ↓
services.py → query_documents()
    ├─ Calls: get_openai_client() ← Lazy initialization
    │
    └─ openai_client.embeddings.create(
       ├─ model: "text-embedding-3-small"
       ├─ input: "Can I travel to USA next week?"
       └─ Returns: query_embedding
```

**Input:** (MEMORY)
```
question: "Can I travel to USA next week?" (string, from Step 1)
model: "text-embedding-3-small" (config)
API: OpenAI embeddings endpoint
```

**Output:** (MEMORY ONLY)
```
query_embedding: [0.0123, -0.0456, 0.0789, -0.0234, 0.0567, ...]
├─ Dimensions: 1536
├─ Type: array of floats
├─ Represents: Semantic meaning of question in vector space
└─ Still in RAM - not persisted

Why: Convert question into same vector space as document embeddings
     (generated in Upload Flow Step 4) so we can measure semantic similarity
```

**Connection:** Question embedding is in the **same 1536-dimensional space** as the document embeddings, enabling similarity comparison.

**Next Step Caller:** services.query_documents() continues

---

### **Step 3: Semantic Search in Qdrant**

**Caller:** `vector_db.search_documents()` (Python function)
```
Called by: services.query_documents()
    ↓
services.py → query_documents()
    ├─ Calls: search_documents(
    │  ├─ query_embedding: [0.0123, ...]  ← From Step 2 (memory)
    │  ├─ user_id: "arpit_001"            ← Filter
    │  └─ top_k: 3                        ← Return top 3
    │
    └─ vector_db.py → search_documents()
       ├─ Calls: get_qdrant_client_instance()
       │
       └─ qdrant_client.query_points(
          ├─ collection_name: "papermind-documents_vectors"
          ├─ query_vector: [0.0123, -0.0456, ...]
          ├─ query_filter: {user_id: "arpit_001"}  ← Multi-tenant isolation
          ├─ limit: 3
          └─ Returns: search results
```

**Input:**
```
query_embedding: [0.0123, -0.0456, 0.0789, ...] (from Step 2, memory)
user_id: "arpit_001" (for filtering)
top_k: 3 (return top 3 matches)
distance_metric: COSINE (angle between vectors)
```

**Output:** (MEMORY ONLY)
```
search_results: list of PointStruct objects
├─ Result 1:
│  ├─ point_id: "qdrant-xyz789"
│  ├─ score: 0.92 ✅ (MATCH - about passport + travel)
│  └─ payload: {"doc_id": "doc-9a140b96", "user_id": "arpit_001", ...}
│
├─ Result 2:
│  ├─ point_id: "qdrant-aaa111"
│  ├─ score: 0.45 ❌ (Weak match - about tax returns)
│  └─ payload: {"doc_id": "doc-xxx", ...}
│
└─ Result 3:
   ├─ point_id: "qdrant-bbb222"
   ├─ score: 0.38 ❌ (Weak match - about salary slip)
   └─ payload: {"doc_id": "doc-yyy", ...}

Note: Still in memory - array of results from Qdrant
```

**Connection:** Qdrant returns `doc_id` values (doc-9a140b96, doc-xxx, doc-yyy) which become keys to fetch actual text from PostgreSQL in the next step.

**Next Step Caller:** services.query_documents() continues

---

### **Step 4: Retrieve Context from PostgreSQL**

**Caller:** `services.query_documents()` (continuing same function)
```
services.py → query_documents()
    ├─ Loops through search_results from Step 3
    │
    ├─ For each result:
    │  ├─ Extracts: doc_id from result.payload
    │  │
    │  └─ Queries: db.query(Extraction).filter(Extraction.doc_id == doc_id)
    │
    └─ Retrieves: raw_text for each matched document
```

**Input:**
```
search_results: list from Step 3 (Qdrant results in memory)
├─ Result 1: doc_id = "doc-9a140b96"
├─ Result 2: doc_id = "doc-xxx"
└─ Result 3: doc_id = "doc-yyy"

Query: SQLAlchemy ORM filter on extractions table
```

**Output:** (MEMORY ONLY)
```
context_texts: list of strings (raw OCR text from PostgreSQL)

From Result 1 (doc-9a140b96):
├─ raw_text: "इस पासपोर्ट में 36 पृष्ठ है। This passport contains 36 pages.
│  भारत गणराज्य REPUBLIC OF INDIA
│  पासपोर्ट नं. M1783676
│  उपनाम JAIN
│  दिया गया नाम ARPIT KUMAR
│  राष्ट्रीयता INDIAN
│  जन्म स्थान GUNA, MADHYA PRADESH
│  जन्म की तिथि 25/11/1980
│  जारी करने की तिथि 30/09/2014
│  समाप्ति की तिथि 29/09/2024" ← KEY: Expiry date
│
├─ result_score: 0.92 (relevance score)
└─ source: PostgreSQL extractions table ✅ (DB)

From Result 2 & 3: Similar retrieval for doc-xxx, doc-yyy

Final Context (joined):
└─ context: "\n\n".join(context_texts[:2000])  ← First 2000 chars

Additional Context Added:
├─ current_date: "2026-09-22" (from server)
└─ question: "Can I travel to USA next week?" (from Step 1)

Note: Still in memory - ready for LLM prompt assembly
```

**Connection:** Extraction table (`extractions.raw_text`) is where the actual OCR content lives. Qdrant found which documents are relevant, PostgreSQL provides the full text. Now we have:
- Semantic relevance (from Qdrant score)
- Complete document context (from PostgreSQL)
- Current date for temporal reasoning

**Next Step Caller:** services.query_documents() continues

---

### **Step 5: Call GPT-4o-mini with RAG Context**

**Caller:** `services.query_documents()` (continuing same function)
```
services.py → query_documents()
    ├─ Calls: get_openai_client()
    │
    ├─ Assembles prompts from prompts.py:
    │  ├─ RAG_QUERY_PROMPT (from prompts.py)
    │  └─ SYSTEM_PROMPT (from prompts.py)
    │
    └─ openai_client.chat.completions.create(
       ├─ model: "gpt-4o-mini"
       ├─ messages: [system_prompt, user_prompt]
       └─ Returns: response object
```

**Input:**
```
system_prompt: "You are a document analyzer. You MUST check expiry dates 
against today's date. If any date in the document is BEFORE today, 
the document is EXPIRED and invalid."

user_prompt (RAG_QUERY_PROMPT.format() from Step 4):
├─ current_date: "2026-09-22"
├─ question: "Can I travel to USA next week?"
├─ context: "पासपोर्ट नं. M1783676 ... समाप्ति की तिथि 29/09/2024..."
└─ rules: 6 critical rules for date comparison and travel eligibility

config:
├─ model: "gpt-4o-mini"
├─ temperature: 0.7
└─ max_tokens: 500
```

**LLM Processing Flow:**
```
1. Parse Input:
   - Extract Expiry Date: 29/09/2024
   - Current Date: 2026-09-22
   - Question: Can I travel?

2. Apply Rules:
   - Compare: 29/09/2024 < 2026-09-22? → YES, EXPIRED
   - Conclusion: Passport is EXPIRED and invalid

3. Generate Answer:
   "Your passport expired on 29/09/2024 and is no longer 
   valid for travel. You cannot travel to USA next week 
   without renewing it first."
```

**Output:** (MEMORY ONLY)
```
response: OpenAI API response object
├─ choices[0].message.content: "Your passport expired on 29/09/2024..."
├─ usage.total_tokens: 245 (for cost tracking)
└─ Still in memory - ready to store and return

Key: answer = "Your passport expired on 29/09/2024..."
```

**Connection:** The LLM receives:
- Question from user (Step 1)
- Full document context from PostgreSQL (Step 4)
- Current date for temporal logic
- Clear system instructions about document validity

Result: Intelligent, factually accurate answer grounded in actual document data.

**Next Step Caller:** services.query_documents() continues

---

### **Step 6: Store Chat History**

**Caller:** `services.query_documents()` (continuing same function)
```
services.py → query_documents()
    ├─ Creates: ChatHistory ORM object
    │
    ├─ Extracts cited_doc_ids from search results
    │  └─ cited_doc_ids: ["doc-9a140b96"]  ← Which docs generated answer
    │
    └─ Calls: db.add(chat_history)
       └─ Commits: db.commit() ← Persists to PostgreSQL
```

**Input:**
```
user_id: "arpit_001" (FK to users table)
message: "Can I travel to USA next week?" (question from Step 1)
response: "Your passport expired on 29/09/2024..." (answer from Step 5)
cited_docs: ["doc-9a140b96"] (which docs were used for RAG)
model_used: "gpt-4o-mini" (string)
tokens_used: 245 (from OpenAI response.usage)
cost: 0.0 (calculated from tokens)
```

**Output:** (PERSISTED TO PostgreSQL)
```
PostgreSQL chat_history table INSERT:
├─ id: "chat-30f5e6b3" ✅ (DB) ← Generated ID
├─ user_id: "arpit_001" ✅ (FK to users)
├─ message: "Can I travel to USA next week?" ✅ (DB)
├─ response: "Your passport expired on 29/09/2024..." ✅ (DB)
├─ cited_docs: ["doc-9a140b96"] ✅ (DB) ← Document lineage
├─ model_used: "gpt-4o-mini" ✅ (DB)
├─ tokens_used: 245 ✅ (DB) ← For cost tracking
├─ cost: 0.0015 (calculated from tokens_used)
└─ created_at: "2026-09-22 10:15:45" (timestamp)

Key: chat_id = "chat-30f5e6b3" ← For traceability
```

**Connection:** Chat history links back to:
- User (via user_id) - multi-tenant isolation
- Documents used (via cited_docs array) - RAG transparency
- Model and tokens (for cost + performance tracking)
- Provides complete audit trail of all queries and answers

**Next:** Return to FastAPI handler and respond to user

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

## Final API Response (After All Steps)

**Input:** Single API call with 2 parameters
```json
{
  "user_id": "arpit_001",
  "question": "Can I travel to USA next week?"
}
```

**Output:** Single HTTP 200 response
```json
{
  "status": "success",
  "answer": "Your passport expired on 29/09/2024 and is no longer valid for travel. You cannot travel to USA next week without renewing it first.",
  "cited_documents": ["doc-9a140b96"],
  "message_id": "chat-30f5e6b3"
}
```

**What Happened Behind the Scenes:**
1. ✅ Question embedded (1536-dim vector)
2. ✅ Qdrant searched (0.92 similarity match)
3. ✅ PostgreSQL context retrieved (1051 chars)
4. ✅ GPT-4o-mini processed (with date logic)
5. ✅ Chat history stored (audit trail)

**Total Latency:** 3-5 seconds end-to-end

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
1. User submits question (user_id, question) via HTTP POST
   └─ FastAPI endpoint receives request (Step 1)

2. Generate embedding of question (OpenAI)
   └─ Query embedded to 1536-dim vector (Step 2, memory)

3. Semantic search in Qdrant
   ├─ Filter: metadata.user_id = "arpit_001" (multi-tenant)
   ├─ Search: Query vector vs document vectors (COSINE similarity)
   └─ Returns: Top 3 most similar doc_ids with scores (Step 3, memory)

4. Retrieve context from PostgreSQL
   ├─ For each doc_id in results: Query extractions table
   ├─ Fetch: raw_text (full OCR content, 1051 chars)
   └─ Assemble: context = concatenated raw_text + current_date (Step 4, memory)

5. Call GPT-4o-mini with RAG context
   ├─ System Prompt: "You are a document analyzer. Check expiry dates..."
   ├─ User Prompt: "TODAY'S DATE: 2026-09-22, Question: ..., Context: ..."
   ├─ LLM analyzes: Compares 29/09/2024 < 2026-09-22 → EXPIRED
   └─ Returns: Answer (Step 5, memory)

6. Store chat history in PostgreSQL
   ├─ Record: user_id, question, answer, cited_docs, tokens_used
   ├─ Commit to chat_history table
   └─ Generate chat_id for traceability (Step 6, DB)

7. Return API response to user
   ├─ status: "success"
   ├─ answer: "Your passport expired on 29/09/2024..."
   ├─ cited_documents: ["doc-9a140b96"]
   └─ message_id: "chat-30f5e6b3"
```

**Data Flow Dependencies:**
- Step 1 output → Step 2 input (question string)
- Step 2 output → Step 3 input (query embedding vector)
- Step 3 output → Step 4 input (doc_id list)
- Step 4 output → Step 5 input (context text + date)
- Step 5 output → Step 6 input (answer + cited docs)
- Step 6 output → API response

Each step builds on previous outputs, enabling intelligent document understanding grounded in actual data.
