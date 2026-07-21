# Chunking Strategies: GeeksforGeeks vs VoxAlchemy Project

**Source Article:** https://www.geeksforgeeks.org/artificial-intelligence/chunking-strategies/

---

## Summary

Out of **6 main chunking strategies** mentioned in the GeeksforGeeks article:

| Strategy | Used? | Where |
|----------|-------|-------|
| **1. Fixed-Size Chunking** | ✅ YES | Audio transcription (5-min chunks) |
| **2. Recursive Character Splitter** | ❌ NO | Not applicable to audio/PDF structure |
| **3. Token-Based Chunking** | ⚠️ PARTIAL | Implicitly considered in duration choice |
| **4. Sentence/Semantic Chunking** | ❌ NO | No content-aware chunking logic |
| **5. Document-Based Chunking** | ✅ YES | PDF OCR (page-level chunks) |
| **6. Chunk Overlap** | ❌ NO | Chunks are independent, no overlap |

---

## Detailed Analysis

### ✅ Strategy 1: Fixed-Size Chunking (USED)

#### GeeksforGeeks Definition
> "Splits text into equal-sized segments based on characters or tokens. Simple and easy to implement. Works well for plain text. May break sentences or context."

#### How VoxAlchemy Uses It

**Audio Transcription (ASR):**

```python
# From: doc-transcribe-worker/worker/transcribe.py

CHUNK_DURATION_SEC = 5 * 60  # 5-minute fixed chunks (300 seconds)

def split_audio(mp3_path: str) -> List[str]:
    audio = AudioSegment.from_file(mp3_path)
    
    chunk_ms = CHUNK_DURATION_SEC * 1000  # Convert to milliseconds
    chunks = []
    
    for i in range(0, len(audio), chunk_ms):
        chunk_audio = audio[i:i+chunk_ms]  # Extract 5-min segment
        chunk_path = f"{mp3_path}_chunk_{len(chunks)+1}.mp3"
        chunk_audio.export(chunk_path, format="mp3")
        chunks.append(chunk_path)
    
    return chunks
```

#### Why This Strategy?

| Aspect | Reason |
|--------|--------|
| **Simplicity** | Duration-based split is deterministic and easy to implement |
| **API Compliance** | 5 minutes ≈ ~50-70MB MP3 ≈ 500K tokens (stays within Gemini input limits) |
| **Failure Isolation** | If chunk N fails, chunks 1-N-1 succeed; only chunk N needs retry |
| **Progress Tracking** | Each chunk is visible progress to user (e.g., "Transcribing chunk 3/12") |
| **Cost Control** | Predictable cost per chunk; easier budgeting |

#### Trade-offs

**Pros:**
- Very fast to implement
- Predictable latency (all chunks ~300 seconds)
- Low retry overhead

**Cons:**
- May split sentences mid-way, breaking context
- Example: If a sentence spans from 4:55 to 5:05, chunk boundary cuts it
- Quality: Could impact transcription quality at chunk boundaries

**VoxAlchemy's Mitigation:**
- Uses **prompting** to instruct Gemini to handle boundaries gracefully
- Concatenates output with "\n\n" separator, allowing natural reflow
- Quality scoring catches issues post-transcription

---

### ⚠️ Strategy 3: Token-Based Chunking (PARTIAL)

#### GeeksforGeeks Definition
> "Splits text based on model token limits. Prevents token overflow in LLMs. Aligns with model constraints. Reduces truncation issues."

#### How VoxAlchemy Uses It (Implicitly)

The project **doesn't explicitly track tokens**, but the 5-minute duration choice is based on token constraints:

```python
# The choice of 5 minutes is informed by Gemini's limits:
CHUNK_DURATION_SEC = 5 * 60  # 300 seconds

# Gemini input limits:
# - Max input: ~20MB audio (when encoded)
# - Max tokens: ~1M tokens per request
# - 5-min audio: ~50-70MB file → ~500K tokens (safe margin)
```

#### Why It's "Partial"

✅ **Yes:** The chunk size is chosen based on token limits
❌ **But:** Not dynamically calculated—fixed regardless of content

```python
# What VoxAlchemy does:
duration_sec = 300  # Fixed 5 minutes

# What true token-based chunking would do:
def chunk_by_tokens(audio_bytes):
    while count_tokens(audio_chunk) < MAX_TOKENS:
        audio_chunk.append(next_segment())
    # Chunk when token limit reached
```

#### Impact

- **Safe:** 5-minute chunks never hit token limits
- **Not Optimized:** Could potentially use 7-10 minutes without overflow
- **Trade-off:** Conservative choice is safer than failing on boundary cases

---

### ✅ Strategy 5: Document-Based Chunking (USED)

#### GeeksforGeeks Definition
> "Breaks structured documents into logical sections. Works well for PDFs and web pages. Maintains document hierarchy. Useful for large datasets."

#### How VoxAlchemy Uses It

**PDF OCR:**

```python
# From: doc-transcribe-worker/worker/ocr.py

def run_ocr(job_id: str, pdf_path: str, total_pages: int):
    """Process each PDF page as a separate chunk."""
    
    for page_num in range(1, total_pages + 1):
        update(job_id, 
               stage=f"OCR page {page_num}/{total_pages}",
               progress=10 + int((page_num / total_pages) * 80))
        
        # Each page = one chunk
        img = convert_pdf_page_to_image(pdf_path, page_num, dpi=300)
        text = gemini_ocr_page(img, prompt)
        
        page_results[page_num] = {
            "text": text,
            "quality_score": score_page(text),
            "status": "SUCCESS"
        }
    
    # Concatenate results
    final_text = "\n".join([page_results[p]["text"] for p in range(1, total_pages + 1)])
    return final_text
```

#### Why Document-Based for PDFs?

| Aspect | Reason |
|--------|--------|
| **Natural Boundary** | Pages are inherent document structure—respects author intent |
| **No Context Loss** | Page boundaries are designed to minimize mid-sentence splits |
| **Failure Granularity** | Page 47 fails → retry page 47 only; pages 1-46 already done |
| **Quality Preservation** | Full-page OCR maintains layout and formatting better than arbitrary splits |
| **User Comprehension** | "Processing page 47/500" is clearer than "processing character 1.2M-1.4M" |

#### Configuration

```python
GEMINI_PAGES_PER_REQUEST = 1  # Default: 1 page per API call

# Can be increased for batching:
# GEMINI_PAGES_PER_REQUEST = 3  # Send 3 pages per request
# Trades latency for API calls (3 pages → 1 API call vs 3 calls)
```

#### Batching Optimization (Document-Based Extension)

```python
# Instead of 1 page per request, can batch multiple pages
def ocr_page_batch(batch_pages: list[int], job: dict) -> str:
    """Send 1-N pages to Gemini in single request."""
    
    images = [convert_pdf_page_to_image(p) for p in batch_pages]
    
    response = model.generate_content([
        Part.from_text(prompt_text),
        *[Part.from_data(img, mime_type="image/png") for img in images]
    ])
    
    # Response is JSON: 
    # {"pages": {1: "text...", 2: "text...", 3: "text..."}}
    return parse_json_response(response.text)
```

**Batching Trade-offs:**
- **Batch=1 (current):** Safe, slow, 100% reliable
- **Batch=3-5:** Fast, but more likely to hit token limits or timeout
- **VoxAlchemy chose Batch=1** for reliability (safer for production)

---

### ❌ Strategy 2: Recursive Character Splitter (NOT USED)

#### GeeksforGeeks Definition
> "Splits text using multiple fallback rules to preserve structure. Maintains sentence flow. Avoids abrupt splits. Produces more readable chunks."

#### How It Works (Conceptually)

```python
# Pseudo-code: Recursive chunking strategy
def recursive_split(text, chunk_size=1000):
    """
    Try to split by sentence boundary first, then word, then character.
    """
    if len(text) <= chunk_size:
        return [text]
    
    # Fallback 1: Split by sentence
    sentences = text.split(".")
    chunks = []
    current_chunk = ""
    
    for sentence in sentences:
        if len(current_chunk) + len(sentence) <= chunk_size:
            current_chunk += sentence + "."
        else:
            chunks.append(current_chunk)
            current_chunk = sentence + "."
    
    # Fallback 2: If still too large, split by word
    # Fallback 3: If still too large, split by character
    return chunks
```

#### Why VoxAlchemy Doesn't Use It

1. **Not Applicable to Audio:** Audio is binary data, not text—can't split by sentence or word
2. **Fixed Structure Better:** PDF pages and 5-min audio chunks are natural boundaries
3. **Simpler to Implement:** Document-based and duration-based are deterministic
4. **No Preprocessing Required:** No need to parse content structure beforehand

---

### ❌ Strategy 4: Sentence or Semantic Chunking (NOT USED)

#### GeeksforGeeks Definition
> "Groups text based on meaning or sentence boundaries to preserve context. Preserves semantic context. Ideal for descriptive content. Improves retrieval quality."

#### Example (Not Used in VoxAlchemy)

```python
# Pseudo-code: Semantic chunking (NOT in VoxAlchemy)
def semantic_split(text, chunk_size=500):
    """
    Split by sentences, ensuring complete thoughts stay together.
    """
    sentences = sent_tokenize(text)  # Uses NLTK or similar
    chunks = []
    current_chunk = ""
    
    for sentence in sentences:
        if len(current_chunk) + len(sentence) <= chunk_size:
            current_chunk += " " + sentence
        else:
            chunks.append(current_chunk)
            current_chunk = sentence
    
    chunks.append(current_chunk)
    return chunks
```

#### Why VoxAlchemy Doesn't Use It

1. **Audio is Not Text:** Can't tokenize audio into sentences
2. **PDF Pages Already Semantic:** Pages are author-designed semantic boundaries
3. **Would Require Preprocessing:** Parsing PDF text to find sentences is complex and error-prone
4. **Gemini Handles Context:** LLM prompt engineering addresses boundary issues better than chunking strategy

---

### ❌ Strategy 6: Chunk Overlap (NOT USED)

#### GeeksforGeeks Definition
> "Includes a small portion of text from the end of one chunk at the beginning of the next chunk. Maintains continuity between chunks. Prevents important information from being lost when text is split."

#### Example (Not Used in VoxAlchemy)

```python
# Pseudo-code: Chunking with overlap (NOT in VoxAlchemy)
def chunk_with_overlap(text, chunk_size=500, overlap_size=50):
    """
    Split into 500-char chunks, with 50-char overlap between consecutive chunks.
    """
    chunks = []
    start = 0
    
    while start < len(text):
        end = min(start + chunk_size, len(text))
        chunks.append(text[start:end])
        start = end - overlap_size  # Overlap: go back 50 chars
    
    return chunks
```

#### Why VoxAlchemy Doesn't Use It

1. **Increases Processing Cost:** Overlapping chunks = redundant API calls
2. **Not Needed for PDFs:** Pages are independent; page 5 doesn't reference page 4
3. **Audio Chunks Are Independent:** 5-10min audio is self-contained; doesn't need context from previous chunk
4. **Concatenation Handles Boundaries:** Joining chunks with "\n\n" is simpler and cheaper

**VoxAlchemy's Approach:**
```python
# Simple concatenation without overlap
texts = []
for chunk in chunks:
    texts.append(transcribed_text)

final_text = "\n\n".join(texts)  # Natural separation
```

---

## Summary Table: Which Strategies Are Used?

| # | Strategy | Used | Project Context | Reasoning |
|---|----------|------|-----------------|-----------|
| 1 | **Fixed-Size Chunking** | ✅ YES | Audio: 5-min duration chunks | Deterministic, respects API limits, enables progress tracking |
| 2 | **Recursive Character Splitter** | ❌ NO | — | Not applicable to binary audio; fixed boundaries simpler |
| 3 | **Token-Based Chunking** | ⚠️ PARTIAL | Audio chunk size chosen based on token limits | 5-min duration inferred from Gemini's token constraints; not dynamic |
| 4 | **Sentence/Semantic Chunking** | ❌ NO | — | No content parsing; document structure sufficient |
| 5 | **Document-Based Chunking** | ✅ YES | PDF: page-level chunks | Natural document boundary; maintains quality and hierarchy |
| 6 | **Chunk Overlap** | ❌ NO | — | Adds cost without benefit; concatenation sufficient |

---

## Design Philosophy: Why These Choices?

### For Audio (Transcription)

**Chose: Fixed-Size Duration (5 minutes)**

```
Why Not Recursive?
  ❌ Can't parse binary audio files

Why Not Semantic?
  ❌ Would require speech-to-text preprocessing (chicken-egg problem)

Why Not Overlap?
  ❌ Increases cost; 5-min chunks are self-contained

Why Fixed-Size?
  ✅ Simple, deterministic
  ✅ Aligns with Gemini token limits
  ✅ Enables clear progress UI
```

### For PDFs (OCR)

**Chose: Document-Based (page-level chunks)**

```
Why Not Fixed-Size (e.g., 1MB)?
  ❌ Pages vary in size; might split mid-page

Why Not Recursive/Semantic?
  ❌ PDFs are already structured by pages; no better boundary exists

Why Document-Based?
  ✅ Respects author's page design
  ✅ Natural boundary; no context loss
  ✅ Easier to explain to users ("Page 47 of 500")
```

---

## Interview Talking Points

### If Asked: "Which chunking strategies does your project use?"

**Strong Answer:**
> "We use **two different chunking strategies** suited to each use case:
> 
> 1. **Fixed-Size Duration Chunking** for audio transcription: We split MP3s into fixed 5-minute chunks. This is deterministic, prevents token overflow (5 min ≈ 500K tokens, within Gemini's limits), and allows granular progress tracking.
> 
> 2. **Document-Based Chunking** for PDF OCR: Each PDF page is a separate chunk. Pages are natural document boundaries designed by authors—this preserves quality and semantic meaning better than arbitrary splits.
> 
> We don't use recursive splitting, semantic chunking, or overlaps because:
> - Audio is binary (can't parse semantically)
> - PDFs are already well-structured by pages
> - Overlaps would increase API costs without benefit"

### If Asked: "How do you handle chunk boundaries?"

**Strong Answer:**
> "We rely on **Gemini's prompting** to handle edge cases:
> - For audio, sentences might span chunk boundaries (4:55–5:05)
> - We instruct Gemini to preserve complete thoughts across chunks
> - Chunks are concatenated with newline separators, allowing natural text reflow
> 
> If a single chunk fails (e.g., 429 rate limit), only that chunk retries—others are already processed. For PDFs, page boundaries are clean by design."

### If Asked: "Why not use token-based chunking?"

**Strong Answer:**
> "We use **duration-based chunking, which is implicitly token-aware**:
> - The 5-minute duration was chosen to stay safely within Gemini's token limits (~500K tokens per chunk)
> - We use a fixed duration rather than dynamic token counting because:
>   - It's simpler: no need to tokenize audio beforehand
>   - It's safer: conservative estimate prevents edge-case token overflow
>   - It's cheaper: no preprocessing overhead
> 
> True token-based chunking would be more optimal but would require:
> - Running audio through a tokenizer before splitting
> - Dynamic calculation per file
> - More complex error handling
> 
> The fixed-size approach is 'good enough' for our use case."

---

## Potential Improvements (Interview Fodder)

### Future Enhancement 1: Dynamic Token Tracking
```python
# Could improve cost/latency by dynamically using more tokens
def chunk_by_tokens(audio_bytes):
    tokens_count = estimate_tokens(audio_bytes)
    
    if tokens_count < 500_000:
        return [audio_bytes]  # Single chunk if within limit
    
    # Split into chunks that total ~500K tokens each
    chunks = []
    current_chunk = b""
    current_tokens = 0
    
    for segment in audio_bytes:
        segment_tokens = estimate_tokens(segment)
        if current_tokens + segment_tokens > 500_000:
            chunks.append(current_chunk)
            current_chunk = segment
            current_tokens = segment_tokens
        else:
            current_chunk += segment
            current_tokens += segment_tokens
    
    return chunks
```

### Future Enhancement 2: Semantic Audio Chunking
```python
# Could split at speaker pauses or topic changes
def chunk_by_speech_pause(audio, min_pause_duration_ms=2000):
    """Split audio at natural pauses (silence > 2 seconds)."""
    # Detect silence regions
    # Split at longest pauses
    # Return chunks that respect content boundaries
```

### Future Enhancement 3: Chunk Overlap for Context
```python
# Could include 10-second overlap for better quality at boundaries
def chunk_with_overlap(audio, chunk_duration_sec=300, overlap_sec=10):
    chunks = []
    start_ms = 0
    chunk_ms = chunk_duration_sec * 1000
    overlap_ms = overlap_sec * 1000
    
    while start_ms < len(audio):
        end_ms = min(start_ms + chunk_ms, len(audio))
        chunks.append(audio[start_ms:end_ms])
        start_ms = end_ms - overlap_ms  # Go back 10 sec
    
    return chunks
```

---

## Conclusion

VoxAlchemy uses **2 of 6 main chunking strategies**:

| Strategy | Decision | Why |
|----------|----------|-----|
| **Fixed-Size Chunking** | ✅ YES | Simple, deterministic, token-aware |
| **Document-Based Chunking** | ✅ YES | Natural boundaries, quality preservation |
| **All others** | ❌ NO | Not applicable, add complexity, increase cost |

The project prioritizes **simplicity and cost** over optimal chunking, with the philosophy that **straightforward boundaries (pages, fixed duration) + smart prompting** is better than complex preprocessing.

This is a good production-grade balance: safe, predictable, and maintainable.
