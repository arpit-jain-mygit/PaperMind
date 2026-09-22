# PaperMind: Application Flow Documentation

## Overview
PaperMind is a Retrieval-Augmented Generation (RAG) system for personal document management. Users upload documents, which are processed through OCR, embedded into vectors, and stored for semantic search and AI-powered queries.

---

## Table of Contents
1. [Option A: Document Upload Flow](#option-a-document-upload-flow)
2. [Option B: Query/RAG Flow](#option-b-queryrag-flow)
3. [Data Architecture](#data-architecture)
4. [Connection Map](#connection-map)
5. [Why These Connections](#why-these-connections)

---

## Option A: Document Upload Flow

### Real Example: Arpit's Passport Upload

#### **Step 1: User Uploads Passport**

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

#### **Step 2: OCR Text Extraction**

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

#### **Step 3: Store in PostgreSQL**

##### **3a. Create Document Record**

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
Unique Constraint: (user_id, filename) prevents duplicates
```

**Purpose:** Central registry of all documents. Document ID (`doc-9a140b96`) becomes the key reference for all downstream data.

---

##### **3b. Store Extracted Text & Metadata**

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

**Purpose:** Stores the actual OCR output. Linked to the Document record via `doc_id`. The `raw_text` field is what gets embedded in the next step.

---

#### **Step 4: Generate Embeddings**

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

**Purpose:** Convert text into a numerical representation for semantic search. This vector represents the "meaning" of the passport document.

---

#### **Step 5: Store in Qdrant Vector DB**

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

**Purpose:** Enable fast semantic search. Qdrant stores vectors indexed for millisecond-level similarity searches across documents.

---

#### **Step 6: Link Everything (Vector Mappings)**

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

**Purpose:** Bridge between PostgreSQL (structured data) and Qdrant (vector data). This is the mapping table that allows:
- Fast user-filtered queries (filter by user_id in PostgreSQL)
- Vector similarity searches (search in Qdrant)
- Context retrieval (fetch raw_text from Extraction via doc_id)

---

#### **Final Response**

```json
{
  "status": "success",
  "doc_id": "doc-9a140b96",
  "filename": "passport.pdf",
  "doc_type": "passport",
  "message": "Document passport.pdf uploaded successfully",
  "metadata": {
    "extracted_chars": 1051,
    "confidence_score": 0.99,
    "extraction_method": "google_document_ai",
    "vector_id": "qdrant-xyz789",
    "embedding_model": "openai-text-embedding-3-small"
  }
}
```

---

## Option B: Query/RAG Flow

When user queries: **"Can I travel to USA next week?"**

```
User Query
    │
    ▼
┌─────────────────────────────────┐
│ 1. Generate Query Embedding     │
│    OpenAI text-embedding-3-small│
│    Input: "Can I travel..."     │
│    Output: [0.234, -0.567, ...]│
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ 2. Semantic Search in Qdrant    │
│    Filter: user_id = arpit_001  │
│    Query Vector: [0.234, ...]   │
│    Find: Top 3 similar vectors  │
│    Returns: [doc-9a140b96]      │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ 3. Retrieve Context             │
│    From PostgreSQL:             │
│    - Extraction.raw_text        │
│    - Extraction.extracted_data  │
│    - Current Date: 2026-09-22   │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ 4. Call GPT-4o-mini with RAG    │
│    System Prompt: Date-aware    │
│    User Prompt: Q + Context     │
│    Returns: "Passport expired   │
│    on 29/09/2024, cannot travel"│
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ 5. Store Chat History           │
│    ChatHistory table:           │
│    - message_id: chat-xxx       │
│    - user_id: arpit_001         │
│    - message: question          │
│    - response: answer           │
│    - cited_docs: [doc-xxx]      │
│    - tokens_used: 245           │
└─────────────────────────────────┘
```

---

## Data Architecture

```
                    ┌─────────────┐
                    │   Angular   │
                    │   UI/Chat   │
                    └──────┬──────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
    POST /upload      POST /query      GET /documents
           │               │               │
    ┌──────▼───────────────▼───────────────▼──────┐
    │            FastAPI Backend                  │
    │  (services.py, app.py, prompts.py)          │
    └────────┬────────────────────┬───────────────┘
             │                    │
    ┌────────▼─────┐      ┌───────▼──────────┐
    │ PostgreSQL   │      │  Qdrant Cloud    │
    │ (Supabase)   │      │  Vector DB       │
    ├──────────────┤      ├──────────────────┤
    │ users        │      │ Vectors (1536)   │
    │ documents    │      │ + Metadata       │
    │ extractions  │      │ Collections      │
    │ vector_maps  │      │                  │
    │ chat_history │      │                  │
    └──────────────┘      └──────────────────┘
             │                    │
             └────────┬───────────┘
                      │
            ┌─────────▼──────────┐
            │ OpenAI API         │
            ├────────────────────┤
            │ Embeddings:        │
            │ text-embedding-    │
            │ 3-small (1536-dim) │
            │                    │
            │ LLM:               │
            │ gpt-4o-mini        │
            └────────────────────┘
             │
            ┌─────────▼──────────┐
            │ Google Cloud       │
            │ Document AI OCR    │
            ├────────────────────┤
            │ Project ID:        │
            │ ultimate-bit-...   │
            │ Processor ID:      │
            │ c1aecfacb43fff0f   │
            └────────────────────┘
```

---

## Connection Map

### Complete Data Flow

```
users (arpit_001)
   │
   ├─→ documents (doc-9a140b96)
   │   ├─→ documents.id
   │   ├─→ documents.user_id
   │   ├─→ documents.filename
   │   └─→ documents.upload_status
   │
   ├─→ extractions (ext-a1b2c3d4)
   │   ├─→ FK: doc_id → documents.id
   │   ├─→ FK: user_id → users.id
   │   ├─→ raw_text: "M1783676, JAIN, ARPIT KUMAR, 29/09/2024..."
   │   └─→ confidence_score: 0.99
   │
   ├─→ vector_mappings (vm-e5f6g7h8)
   │   ├─→ FK: doc_id → documents.id
   │   ├─→ FK: user_id → users.id
   │   ├─→ qdrant_id: "qdrant-xyz789"
   │   └─→ text_chunk: first 1000 chars
   │
   ├─→ chat_history (chat-30f5e6b3)
   │   ├─→ FK: user_id → users.id
   │   ├─→ message: "Can I travel to USA?"
   │   ├─→ response: "Passport expired..."
   │   └─→ cited_docs: ["doc-9a140b96"]
   │
   └─→ (External) Qdrant Cloud
       └─→ Collection: papermind-documents_vectors
           └─→ Point ID: qdrant-xyz789
               ├─→ Vector: [0.0234, -0.0567, ...(1536 dims)]
               ├─→ Metadata.doc_id: "doc-9a140b96"
               ├─→ Metadata.user_id: "arpit_001"
               ├─→ Metadata.doc_type: "passport"
               └─→ Metadata.filename: "passport.pdf"
```

---

## Why These Connections?

| Connection | Purpose | Benefit |
|-----------|---------|---------|
| **Document → Extraction** | Track which OCR output came from which file | Audit trail, re-processing capability |
| **Document → VectorMapping** | Link file to its vector | Bidirectional lookup (file → vector or vector → file) |
| **VectorMapping → Qdrant ID** | Bridge PostgreSQL and Qdrant | Decouple storage layers, independent scaling |
| **user_id everywhere** | Multi-tenant isolation | Each user only sees their own documents |
| **Metadata in Qdrant** | Avoid extra DB queries during search | Fast filtering without round-trip to PostgreSQL |
| **Extraction.raw_text** | Store full OCR output | Retrieved for LLM context in queries |
| **ChatHistory.cited_docs** | Link queries to source documents | Transparency, audit trail |

---

## Key Design Patterns

### Two-Stage OCR Strategy
```
PDF Input
  ├─ Try: pdfplumber (fast, milliseconds)
  │   └─ Success? → Use extracted text
  └─ Fail? → Try: Google Document AI (accurate, 2-3s)
      └─ 99%+ accuracy for image-based PDFs
```

### Dual Storage
```
PostgreSQL: Structured data
  ├─ User info
  ├─ Document metadata
  ├─ Extracted text (for LLM context)
  └─ Chat history (audit trail)

Qdrant: Vector data
  ├─ Embeddings (1536-dim)
  ├─ Metadata (for filtering)
  └─ Fast similarity search (<100ms)
```

### RAG Pattern
```
User Question
  → Embed question
  → Search similar documents in Qdrant
  → Retrieve context from PostgreSQL
  → Feed to LLM with current date context
  → Get intelligent answer
  → Store in ChatHistory
```

---

## Complete Upload Sequence

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

Each step depends on the previous one, creating an atomic transaction flow.

---

## Database Schema Summary

```
users
├─ id (PK)
├─ user_id (UNIQUE)
└─ created_at

documents
├─ id (PK)
├─ user_id (FK → users)
├─ filename
├─ doc_type
├─ upload_status (processing/completed)
└─ created_at

extractions
├─ id (PK)
├─ doc_id (FK → documents)
├─ user_id (FK → users)
├─ raw_text (TEXT)
├─ confidence_score
└─ created_at

vector_mappings
├─ id (PK)
├─ doc_id (FK → documents)
├─ user_id (FK → users)
├─ qdrant_id (→ Qdrant point ID)
├─ embedding_model
└─ created_at

chat_history
├─ id (PK)
├─ user_id (FK → users)
├─ message (TEXT)
├─ response (TEXT)
├─ cited_docs (ARRAY)
├─ model_used
└─ created_at
```

---

## API Response Examples

### Upload Success
```json
{
  "status": "success",
  "doc_id": "doc-9a140b96",
  "filename": "passport.pdf",
  "doc_type": "passport",
  "message": "Document passport.pdf uploaded successfully"
}
```

### Query Success
```json
{
  "status": "success",
  "answer": "Your passport expired on 29/09/2024 and is no longer valid for travel.",
  "cited_documents": ["doc-9a140b96"],
  "message_id": "chat-30f5e6b3"
}
```

---

## Performance Metrics

| Operation | Latency | Notes |
|-----------|---------|-------|
| pdfplumber extraction | < 100ms | Digital PDFs only |
| Google Document AI OCR | 2-3s | Image-based PDFs, high accuracy |
| Embedding generation | < 2s | OpenAI API |
| Vector search | < 100ms | Qdrant semantic search |
| LLM inference | < 2s | GPT-4o-mini |
| **End-to-end upload** | **5-8s** | All steps combined |
| **End-to-end query** | **3-5s** | Search + LLM |

---

**Last Updated:** 2026-09-22  
**Status:** Production Ready (Options A & B)  
**Next:** Angular Frontend (Option C) + Deployment (Option D)
