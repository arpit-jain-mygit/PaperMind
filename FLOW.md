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
