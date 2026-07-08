# Arpit's System Design Portfolio

Collection of detailed system design writeups and architecture diagrams for MAANG interview preparation.

## System Designs

### 1. URL Shortener
- **Location:** `/url-shortener/`
- **Focus:** Distributed unique ID generation, batch processing, high-volume reads
- **Key Concepts:** Pre-generation + Kafka, Base62 conversion, Collision handling, Caching strategies
- **Files:**
  - `URL_SHORTENER_ALGORITHMS.md` - Algorithm comparison (CRC32 vs Base62 vs Chubb)
  - `CHUBB_E2E_ARCHITECTURE.md` - Complete end-to-end architecture
  - `CRC32_CRISP_SUMMARY.md` - Deep dive on CRC32 limitations
  - `URL_SHORTENER_DESIGN.md` - Complete writeup with all MAANG considerations

---

## Interview Readiness Checklist

Each design includes:
- ✅ Functional & Non-functional requirements
- ✅ Back-of-envelope estimation
- ✅ Algorithm comparison & trade-offs
- ✅ Database choice & schema design
- ✅ API specifications
- ✅ High-level architecture diagrams
- ✅ Detailed design flow diagrams
- ✅ Failure handling & recovery
- ✅ Monitoring & alerting strategies
- ✅ Edge cases & scalability

---

## How to Use This

**For Interview Preparation:**
1. Read the `.md` files for detailed explanations
2. Study the diagrams for visual understanding
3. Use as reference during mock interviews
4. Practice explaining each design in 30-45 minutes

**For Deep Learning:**
- Start with algorithm comparisons
- Move to complete architecture
- Study edge cases and failure scenarios
- Discuss trade-offs and alternatives

---

## Study Guide

**Time allocation for each design:**
- Algorithm comparison: 10 minutes
- High-level design: 10 minutes
- Detailed design: 15 minutes
- Edge cases & monitoring: 10 minutes

**Interview scenario (45 minutes):**
- Clarify requirements: 2 min
- Show high-level design: 3 min
- Deep dive into components: 20 min
- Discuss trade-offs: 10 min
- Handle follow-up questions: 10 min

---

## Next System Designs to Add

- [ ] Rate Limiter
- [ ] Web Crawler
- [ ] Cache System (LRU, LFU)
- [ ] Search Engine (Typeahead)
- [ ] Distributed Task Scheduler
- [ ] Message Queue System
- [ ] Social Media Feed
- [ ] E-commerce Shopping System

---

**Last Updated:** 2026-07-08
**Status:** Active Development
