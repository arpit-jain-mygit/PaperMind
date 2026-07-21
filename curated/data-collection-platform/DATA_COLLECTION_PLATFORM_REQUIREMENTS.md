# Data Collection Platform - Requirements Document

## 1. Executive Summary
A comprehensive platform for collecting, extracting, and processing financial data from multiple sources (PDFs, Excel, CSV, emails) with multi-level user roles, quality assurance, and integration with AI/ML extraction services.

---

## 2. Functional Requirements

### 2.1 Configuration Management
- **Template Management**: Define extraction templates per document type
- **Taxonomy Mappings**: Map extracted fields to internal canonical format
- **Dissemination Configuration**: Define data distribution rules and channels
- **Extraction Rules Engine**: Configure business rules and validation logic
- **Entity Mapping**: Internal entity name mapping (via Soniq/external datasets)

### 2.2 Data Sourcing
**Channels:**
- File system (S3, local storage)
- Delta Sharing (Databricks)
- Database (JDBC connectors)
- Email inboxes (Outlook, Gmail via mock)
- FTP/SFTP
- Message queues (Kafka)

**File Formats:**
- CSV, Excel, PDF, JSON, Parquet
- Email attachments

### 2.3 Security & Compliance
- File security scanning (malware detection)
- Data classification and tagging
- Encryption at rest and in transit
- Audit logging for all operations
- PII/sensitive data masking
- Access control enforcement (RBAC)

### 2.4 Data Ingestion Pipeline
**Stages:**
1. Metadata extraction (source, format, date, hash)
2. Deduplication check
3. Work assignment (to Level 1 users)
4. Manual data entry or automated extraction
5. Annotation/confidence scoring
6. Raw zone storage (medallion: Bronze layer)

**Extraction Methods:**
- Manual entry (UI)
- AI-powered extraction (SparkAir, Cognize, Deepmine)
- OCR for scanned documents
- Template-based extraction

### 2.5 Data Processing
- **Rules Application**: Apply business rules and transformations
- **Data Quality Checks**: Validation at each stage
- **Data Lineage Tracking**: Track data origin and transformations
- **Canonical Data Model Enforcement**: Transform to standard format

### 2.6 Data Review & Approval
- Two-level review workflow (L1 → L2)
- Conflict resolution mechanism
- Audit trail of changes
- Approval/rejection logic

### 2.7 Data Dissemination
- Multi-channel delivery (API, file, message queue, webhook)
- Scheduled exports (batch/real-time)
- Format transformation (CSV, Excel, JSON, Parquet)
- Access control per dissemination config

### 2.8 User Roles & Permissions

**Data User Level 1:**
- Manual data entry
- Review and correct extracted data
- View own assigned work
- Submit data for L2 review

**Data User Level 2:**
- All L1 capabilities
- Review and approve/reject L1 submissions
- Reassign work
- Configure templates (limited)

**Admin/Architect:**
- Full system configuration
- User management
- Audit log access
- System monitoring

---

## 3. Non-Functional Requirements

### 3.1 Performance
- **Throughput**: Process 10,000 documents/day (avg 100MB each)
- **Latency**: 
  - UI response: <2 seconds
  - Manual extraction: <1 second per field entry
  - Batch processing: Complete within SLA (4-hour window)
- **Concurrent Users**: Support 500+ concurrent users
- **Document Processing**: Extract from 100MB+ PDFs in <5 minutes

### 3.2 Scalability
- Horizontal scaling of extraction workers
- Auto-scaling based on queue depth
- Multi-tenant isolation
- Database read replicas for reporting

### 3.3 Availability
- **Uptime SLA**: 99.5% (4 hours downtime/month)
- **RTO**: 1 hour
- **RPO**: 15 minutes
- Graceful degradation when services fail

### 3.4 Reliability
- Idempotent operations for retry safety
- At-least-once delivery semantics
- Circuit breakers for external calls
- Distributed transaction handling (Saga pattern)

### 3.5 Security
- TLS 1.2+ for all communications
- JWT/OAuth 2.0 for authentication
- RBAC for authorization
- Secrets management (HashiCorp Vault)
- PII encryption (AES-256)
- Data masking in dev/test environments

### 3.6 Data Quality
- Validation rules at ingestion and processing
- Anomaly detection
- Confidence scoring (0-100%)
- Data quality metrics (completeness, accuracy, timeliness)

### 3.7 Maintainability
- Containerized deployment (Docker)
- IaC (Infrastructure as Code)
- API versioning (v1, v2, etc.)
- Comprehensive logging and monitoring
- Code coverage >80%

### 3.8 Observability
- Distributed tracing (Jaeger/Sleuth)
- Centralized logging (ELK/Splunk)
- Metrics collection (Prometheus/Micrometer)
- Custom dashboards for business KPIs

---

## 4. Technology Stack

| Layer | Technology |
|-------|-----------|
| **UI** | Angular 14+ |
| **API Gateway** | Spring Cloud Gateway |
| **Microservices** | Spring Boot 2.7+ |
| **Message Queue** | Apache Kafka |
| **Orchestration** | Apache Camunda/Temporal |
| **Data Processing** | Apache Spark (PySpark/Scala) |
| **Extraction Engine** | SparkAir, Cognize (3rd party) |
| **Metadata Store** | PostgreSQL 12+ |
| **Data Lake** | MongoDB (documents), Parquet (data warehouse) |
| **Search** | Elasticsearch 7+ |
| **Entity Mapping** | Soniq (external API) |
| **Caching** | Redis, Hazelcast |
| **Container** | Docker, Kubernetes 1.20+ |
| **CI/CD** | Azure DevOps (Pipelines, Repos) |
| **Monitoring** | Splunk, Prometheus, Grafana |
| **Secret Mgmt** | HashiCorp Vault, Azure Key Vault |
| **File Storage** | S3 (or Azure Blob Storage) |
| **Service Mesh** | Istio (optional for advanced scenarios) |

---

## 5. Integration Points

- **SparkAir**: Gen AI text extraction (parameter-driven)
- **Cognize**: Alternative extraction engine
- **Deepmine**: ML-based document classification
- **Soniq**: Entity name mapping and reference data
- **Delta Sharing**: Databricks data sharing
- **Apache Camel**: EIP routing and connectors
- **Outlook/Gmail**: Email source connectors (mock initially)
- **S&P APIs**: Financial data reference

---

## 6. Constraints & Assumptions

**Constraints:**
- Batch processing window: 4 hours
- Storage cost: Optimize for cost without sacrificing performance
- Concurrent extraction jobs: Max 50 (resource-limited)

**Assumptions:**
- Data owners provide correct source format
- Network connectivity to external systems (Soniq, SparkAir) is available
- Email service doesn't require OAuth initially (mock)
- PostgreSQL can handle 500K transactions/day

---

## 7. Success Criteria

- ✅ Process 10K documents/day with >95% accuracy
- ✅ Support 500+ concurrent users
- ✅ <2 second UI response time
- ✅ <2 hour time-to-value for new data
- ✅ <1% data quality issues in production
- ✅ 99.5% uptime
- ✅ Complete audit trail for all data changes
