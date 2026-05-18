# Architectural Requirements - Property Management System

This document defines the technical specifications and architectural constraints for the features identified in the brainstorming phase.

---

## 1. Multi-tier Property Inventory
**Category:** Property Management  
**Function:** Multi-tier Property Inventory  
**Description:** Hierarchical management of portfolios, buildings, and individual units including geo-coordinates, amenities, and floor plans.

### Architectural Requirements:
**Security:**
* **Role-Based Access Control (RBAC):** Strict enforcement ensuring only authorized System Admins and Property Managers can create, modify, or delete inventory entities. View-only access for Maintenance Workers.
* **Input Validation:** Sanitize all inputs (specifically Geo-JSON coordinates, building metadata, and amenity descriptions) to prevent SQLi and XSS attacks.
* **Secure Transmission:** Mandatory HTTPS/TLS 1.3 for all data in transit. 
* **PII Protection:** Encryption at rest (AES-256) for any linked owner contact details or sensitive property-specific notes.

**Performance:**
* **Response Time SLA:** Ensure API response times for hierarchical lookups remain < 1.2 seconds for portfolios exceeding 1,000 units.
* **Read Optimization:** Implement **Redis Caching** for frequently accessed property listings and building profiles to reduce database load.
* **Search Acceleration:** Utilize **Elasticsearch** for complex filtering (e.g., searching by specific amenities or geofencing).
* **Media Optimization:** Integrated **CDN (CloudFront/Cloudinary)** for rapid delivery of high-resolution floor plans and property imagery.

**Reliability/Availability:**
* **Transactional Integrity:** Use atomic database transactions to ensure that the creation of a building and its child units either succeeds or fails as a single unit of work.
* **High Availability:** Multi-AZ (Availability Zone) deployment for the relational database to ensure 99.99% availability and zero data loss during failover.
* **Relational Integrity:** Strict foreign key constraints to maintain the hierarchy between Portfolio → Building → Unit.

**Interoperability:**
* **Geo-Services Integration:** API integration with **Google Maps Platform** or **Mapbox** for geocoding addresses and rendering property locations on interactive maps.
* **Storage Integration:** Direct integration with **AWS S3** or **Cloudinary** for scalable storage of floor plan PDFs and CAD files.

**Auditability:**
* Log property/unit modification (who, what entity, old value, new value, outcome, timestamp, IP). Mask sensitive PII/Payment data.
* Log inventory deletion attempt (who, what entity, old value, new value, outcome, timestamp, IP). Mask sensitive PII/Payment data.

**Compliance:**
* **Fair Housing Act:** Ensure metadata fields do not capture or allow filtering by protected classes (race, religion, etc.) to prevent discriminatory listing practices.
* **GDPR/CCPA:** Ensure data deletion (Right to be Forgotten) cascades from properties to any linked personal data of owners or staff.

---

## 2. Real-time Unit Availability
**Category:** Property Management  
**Function:** Real-time Unit Availability  
**Description:** A dynamic status engine (Vacant, Occupied, Maintenance, Reserved) that updates across public listings and admin dashboards instantly.

### Architectural Requirements:
**Security:**
* **RBAC:** Public users (Tenants) have read-only access to "Vacant" units; staff can see all statuses.
* **Session Management:** Ensure that status updates are verified against active, authorized staff sessions.

**Performance:**
* **Response Time SLA:** Status lookups for public-facing listings must be < 500ms.
* **Caching:** Use **Redis Pub/Sub** or WebSockets to push status updates to the UI in real-time without polling.

**Reliability/Availability:**
* **Eventual Consistency vs Strong Consistency:** Use **Strong Consistency** for status transitions (e.g., moving from Vacant to Reserved) to prevent double-booking.
* **Distributed Locking:** Implement **Redlock** (Redis) or database-level locking during the reservation process.

**Interoperability:**
* **Legacy Sync:** Bi-directional sync with the legacy system's property database to ensure data parity during the migration phase.

**Auditability:**
* Log status change (who, what entity, old value, new value, outcome, timestamp, IP). Mask sensitive PII/Payment data.

**Compliance:**
* **Fair Housing Act:** Ensure "Reserved" units are not selectively shown or hidden based on tenant demographics.

---

## 3. Digital KYC & Background Screening
**Category:** Tenant Onboarding  
**Function:** Digital KYC & Background Screening  
**Description:** Automated identity verification and credit scoring for prospective tenants, integrated with third-party verification APIs.

### Architectural Requirements:
**Security:**
* **PII Protection:** Strict **Encryption at Rest** for SSNs, IDs, and credit reports using AWS KMS or HashiCorp Vault.
* **MFA:** Required for any staff member accessing detailed credit reports.
* **Data Masking:** Only show the last 4 digits of sensitive identifiers in the UI.

**Performance:**
* **Async Processing:** Use background jobs (RabbitMQ/Kafka) for third-party API calls to prevent UI blocking during screening (SLA for request submission < 2s).

**Reliability/Availability:**
* **Circuit Breaker Pattern:** Implement Resilience4j or similar to handle timeouts or failures of third-party screening providers (e.g., Checkr).

**Interoperability:**
* **Screening Integration:** Seamless API integration with **Checkr**, **TransUnion**, or **Experian** for identity and credit data.

**Auditability:**
* Log screening request (who, what entity, NULL, "Pending", outcome, timestamp, IP). Mask sensitive PII/Payment data.
* Log screening report access (who, what entity, NULL, NULL, outcome, timestamp, IP). Mask sensitive PII/Payment data.

**Compliance:**
* **FCRA (Fair Credit Reporting Act):** Ensure applicants are notified of adverse actions and have access to their reports.
* **GDPR/CCPA:** Automatic purging of PII after the legally required retention period or upon application denial.

---

## 4. Smart E-Lease Generator
**Category:** Leases  
**Function:** Smart E-Lease Generator  
**Description:** Rule-based generation of digital lease agreements with e-signature support and automated clause customization.

### Architectural Requirements:
**Security:**
* **Digital Signatures:** Ensure document integrity using hashing and digital certificates.
* **RBAC:** Restrict template editing to Legal/Admin roles.

**Performance:**
* **Background Jobs:** Generate PDFs asynchronously (SLA < 5s from request to "Ready" status).
* **Caching:** Cache common lease templates in Redis.

**Reliability/Availability:**
* **Transactional Integrity:** Ensure lease data (rent amount, dates) in the DB perfectly matches the generated PDF content.

**Interoperability:**
* **E-Signature Integration:** Integration with **DocuSign** or **HelloSign** APIs for workflow management and signature collection.

**Auditability:**
* Log lease generation (who, what entity, "Template ID", "Document ID", outcome, timestamp, IP). Mask sensitive PII/Payment data.
* Log lease signature event (who, what entity, "Pending", "Signed", outcome, timestamp, IP). Mask sensitive PII/Payment data.

**Compliance:**
* **ESIGN Act / UETA:** Ensure all digital signatures are legally binding and meet federal/state standards.

---

## 5. Integrated Payment Gateway
**Category:** Rent Payments  
**Function:** Integrated Payment Gateway  
**Description:** Seamless rent collection utilizing the legacy Payway webhook system, supporting credit cards and bank transfers.

### Architectural Requirements:
**Security:**
* **PCI DSS Compliance:** No storage of raw credit card data; use tokenization via **Payway**.
* **Webhook Verification:** Validate signatures of incoming Payway webhooks to prevent spoofing.

**Performance:**
* **Response Time SLA:** Payment initialization < 1.5s.
* **Webhooks:** Handle incoming notifications asynchronously via a message queue to ensure 100% processing reliability.

**Reliability/Availability:**
* **Idempotency:** Implement idempotency keys for all payment requests to prevent double-charging.
* **Saga Pattern:** Manage the distributed transaction between the Payment Service and the Accounting/Lease Service.

**Interoperability:**
* **Payment Integration:** Direct integration with **Payway** (legacy) and potentially **Stripe** or **Plaid** for future ACH support.

**Auditability:**
* Log payment transaction (who, what entity, "Pending", "Success/Fail", outcome, timestamp, IP). Mask sensitive PII/Payment data.

**Compliance:**
* **PCI DSS:** Maintain strict adherence to payment card industry standards.

---

## 6. Automated Late Fee Engine
**Category:** Rent Payments  
**Function:** Automated Late Fee Engine  
**Description:** A background scheduler that calculates and applies penalties based on lease grace periods.

### Architectural Requirements:
**Security:**
* **RBAC:** Only Finance Admins can adjust fee calculation logic or override applied fees.

**Performance:**
* **Batch Processing:** Ability to process 10,000+ leases in under 1 hour via scheduled Cron jobs.

**Reliability/Availability:**
* **Compensating Transactions:** Logic to reverse fees if a late payment was due to system error or manager override.

**Interoperability:**
* **Notification Integration:** Trigger alerts via the **Notification Service** (SendGrid/Firebase) when a fee is applied.

**Auditability:**
* Log fee application (System, what entity, "0.00", "50.00", outcome, timestamp, IP). Mask sensitive PII/Payment data.

**Compliance:**
* **State Laws:** Ensure fee amounts do not exceed state-mandated caps on residential late penalties.

---

## 7. Work Order Lifecycle Management
**Category:** Maintenance  
**Function:** Work Order Lifecycle Management  
**Description:** A ticketing system for tenants to submit repair requests with media attachments, assignable to workers.

### Architectural Requirements:
**Security:**
* **Input Validation:** Scan all uploaded media for malware (via **ClamAV** or S3 Object Lambda).
* **RBAC:** Tenants can only see their own tickets; Workers can see assigned tickets.

**Performance:**
* **Media Optimization:** Thumbnails generated asynchronously for images uploaded to **Cloudinary**.
* **SLA:** Ticket creation < 1s.

**Reliability/Availability:**
* **State Machine:** Use a formal state machine to manage transitions (New -> Assigned -> In Progress -> Completed).

**Interoperability:**
* **Cloudinary Integration:** For storage and transformation of maintenance photos/videos.

**Auditability:**
* Log ticket status update (who, what entity, old status, new status, outcome, timestamp, IP). Mask sensitive PII/Payment data.

---

## 8. Preventive Maintenance Scheduler
**Category:** Maintenance  
**Function:** Preventive Maintenance Scheduler  
**Description:** Recurring maintenance task automation for HVAC, fire safety, and plumbing.

### Architectural Requirements:
**Security:**
* **RBAC:** Facilities Manager access only for schedule configuration.

**Performance:**
* **Resource Optimization:** Background generation of future work orders during off-peak hours.

**Reliability/Availability:**
* **High Availability Scheduler:** Use a distributed scheduler (e.g., Quartz or AWS EventBridge) to ensure no missed maintenance cycles.

**Auditability:**
* Log schedule creation/edit (who, what entity, old value, new value, outcome, timestamp, IP). Mask sensitive PII/Payment data.

---

## 9. Multi-Role Messaging Hub
**Category:** Communications  
**Function:** Multi-Role Messaging Hub  
**Description:** In-app chat system facilitating secure communication between Tenants, Managers, and Workers.

### Architectural Requirements:
**Security:**
* **End-to-Transit Encryption:** HTTPS/TLS 1.3.
* **PII Protection:** Automatic detection and masking of sensitive info (like SSNs) in chat threads.

**Performance:**
* **Latency SLA:** Message delivery < 200ms.
* **Scalability:** Use **WebSockets** with a Redis backplane for horizontal scaling of chat sessions.

**Interoperability:**
* **Push Notifications:** Deep integration with the **Notification Service** (Firebase).

**Auditability:**
* Log chat room creation (who, what entity, NULL, "Room ID", outcome, timestamp, IP). Mask sensitive PII/Payment data.
* *Note: Message content is stored for history but strictly audited for access.*

---

## 10. Automated Push Notifications
**Category:** Communications  
**Function:** Automated Push Notifications  
**Description:** Event-driven alerts via Firebase for rent, leases, and maintenance.

### Architectural Requirements:
**Security:**
* **Token Management:** Securely store and rotate Firebase Cloud Messaging (FCM) tokens.

**Performance:**
* **Throughput:** Capable of sending 50,000+ notifications per minute.
* **Async:** Fire-and-forget processing via message queues.

**Interoperability:**
* **Firebase (FCM):** Primary channel for mobile push.
* **SendGrid/AWS SES:** Secondary channel for email fallbacks.

**Auditability:**
* Log notification dispatch (System, what entity, NULL, "Sent", outcome, timestamp, IP). Mask sensitive PII/Payment data.

---

## 11. Financial Performance Dashboard
**Category:** Reporting  
**Function:** Financial Performance Dashboard  
**Description:** Visualization of NOI, Gross Potential Rent, and cash flow analysis.

### Architectural Requirements:
**Security:**
* **RBAC:** Executive/Landlord access only.
* **Data Isolation:** Ensure multi-tenant isolation; owners only see data for their specific properties.

**Performance:**
* **Read-Only Replicas:** Execute heavy analytical queries against DB read-replicas to avoid impacting OLTP performance.
* **OLAP/Data Warehouse:** For historical trends, move data to a dedicated warehouse (e.g., Snowflake or BigQuery).

**Reliability/Availability:**
* **Data Consistency:** Reconciliation checks between the dashboard and raw transaction logs.

**Auditability:**
* Log report generation (who, what entity, "Report Type", NULL, outcome, timestamp, IP). Mask sensitive PII/Payment data.

---

## 12. AI-Driven Vacancy Predictive Analytics
**Category:** Reporting  
**Function:** AI-Driven Vacancy Predictive Analytics  
**Description:** Analysis of historical turnover data to predict future vacancy risks.

### Architectural Requirements:
**Security:**
* **Anonymization:** Strip PII before feeding data into ML models.

**Performance:**
* **Async Training:** Model training performed in isolated environments (AWS SageMaker).
* **Inference SLA:** Dashboard prediction load < 3s.

**Interoperability:**
* **ML Platforms:** Integration with Python-based microservices or external ML APIs.

**Auditability:**
* Log model update (System, "Model Version", old_v, new_v, outcome, timestamp, IP). Mask sensitive PII/Payment data.

---

## 13. Maintenance Cost Distribution
**Category:** Reporting  
**Function:** Maintenance Cost Distribution  
**Description:** Granular reporting on repair expenses per unit or property.

### Architectural Requirements:
**Performance:**
* **Aggregations:** Use pre-calculated summary tables (Materialized Views) for rapid report rendering.

**Reliability/Availability:**
* **Transactional Integrity:** Ensure cost data links exactly to confirmed maintenance invoices.

**Auditability:**
* Log cost report export (who, what entity, NULL, NULL, outcome, timestamp, IP). Mask sensitive PII/Payment data.

---

## 14. Centralized Document Vault
**Category:** Document Management  
**Function:** Centralized Document Vault  
**Description:** Secure, cloud-based storage for deeds, insurance, and leases with RBAC.

### Architectural Requirements:
**Security:**
* **Encryption at Rest:** Mandatory AES-256 for all stored objects.
* **Presigned URLs:** Use short-lived presigned URLs for document access.
* **Versioning:** Enable S3 Versioning to prevent accidental deletion.

**Performance:**
* **CDN Integration:** For rapid retrieval of non-sensitive documents.

**Interoperability:**
* **S3/Azure Blob:** Primary storage engines.

**Auditability:**
* Log document access (who, what entity, NULL, NULL, outcome, timestamp, IP). Mask sensitive PII/Payment data.

---

## 15. Fine-Grained RBAC Engine
**Category:** Admin/Access Control  
**Function:** Fine-Grained RBAC Engine  
**Description:** Robust Role-Based Access Control system managing permissions across the ecosystem.

### Architectural Requirements:
**Security:**
* **Principle of Least Privilege:** Default deny-all policy.
* **Centralized Auth:** Use OAuth2/OIDC (Keycloak or Auth0) for identity management.

**Performance:**
* **Caching:** Cache user permissions in **Redis** (SLA < 50ms for permission check).

**Reliability/Availability:**
* **High Availability:** The Auth service must have 99.999% availability as it is a single point of failure.

**Auditability:**
* Log permission change (who, what entity, old_role, new_role, outcome, timestamp, IP). Mask sensitive PII/Payment data.
* Log login attempt (who, NULL, NULL, NULL, outcome, timestamp, IP). Mask sensitive PII/Payment data.
