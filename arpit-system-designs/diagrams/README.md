# Diagrams for URL Shortener Design

This folder contains architectural diagrams for Revision 1 of the URL Shortener System Design.

## Files to Add

### 1. batch-design-diagram.png
**Description:** Batch design - Short URLs generation, randomisation and ready to be mapped

**Flow:**
- Python program generates 56B IDs in Files (1 GB each, Total 360 files)
- Store these files in S3
- Write Spark program to load files and call Unix sort to shuffle
- Reload shuffled files in S3
- Load shuffled files using Spark
- Spark loads entries from each file into Kafka partitions
- Short URLs ready to be mapped to long URLs on request

### 2. user-request-flow-diagram.png
**Description:** User Request flow for Write (Long to Short URL) / Read (Redirect Short to Long URL)

**Flow:**
- Actor → API Gateway → Load Balancer
- Write path: Create Short URLs App Server (Kubernetes) → NoSQL (Cassandra/DynamoDB)
- Read path: Redirect Short URL to Long URL App Server (Kubernetes) → Cache Cluster (Redis)
- Fallback: Kafka (If not found in cache)

---

**To add these diagrams:**
```bash
cp /path/to/batch-design-diagram.png arpit-system-designs/diagrams/
cp /path/to/user-request-flow-diagram.png arpit-system-designs/diagrams/
git add arpit-system-designs/diagrams/
git commit -m "Add: Diagram images for URL Shortener Revision 1"
```
