# Attribute-Driven Design (ADD) Document - Property Management System

## 1. Design Constraints
— **Scalability**: The system must handle high-volume batch processing for 10,000+ leases in under 1 hour, support notification throughput of 50,000+ messages per minute, and maintain performance for property portfolios exceeding 1,000 units.
— **Performance**: Strict latency requirements include API response times < 1.2s for inventory lookups, < 500ms for unit availability, < 200ms for message delivery, and < 50ms for authorization permission checks; asynchronous PDF generation must complete in < 5s.
— **Security**: Mandatory enforcement of RBAC with a "default-deny" policy, AES-256 encryption at rest (AWS KMS/Vault), TLS 1.3 for data in transit, and PCI DSS compliance for payment tokenization via Payway.
— **Availability**: The architecture must guarantee 99.99% availability for the relational database via Multi-AZ deployment and 99.999% availability for the centralized Authentication service.
— **Modifiability**: The system must support bi-directional data parity with legacy databases during migration and utilize the Saga Pattern and Eventual/Strong consistency models to manage distributed state across microservices.
— **Interoperability**: Seamless integration with third-party ecosystems including Google Maps/Mapbox (Geo-services), DocuSign/HelloSign (E-signatures), Checkr/Experian (KYC), and Firebase/SendGrid (Communications).
— **Usability**: Real-time feedback loops are required through WebSockets/Redis Pub/Sub to ensure UI state changes for unit availability and maintenance status are reflected across multi-role dashboards (Tenants, Managers, Workers) instantly.

## 2. Quality Attribute Requirements

### 2.1. Security

#### 2.1.1 Authentication and Authorization
| Element | Statement |
| :--- | :--- |
| Stimulus | User attempts to access protected resources (property inventory, credit reports, or financial data) |
| Stimulus source | Tenants, Staff, System Administrators |
| Environment | Normal operation |
| Artifact | Fine-Grained RBAC Engine, Auth Service |
| Response | System enforces RBAC with a "default-deny" policy using OAuth2/OIDC (Keycloak/Auth0); permissions are cached in Redis for rapid verification |
| Response measure | Permission check completes within < 50ms; Auth service maintains 99.999% availability |

#### 2.1.2 Data Protection
| Element | Statement |
| :--- | :--- |
| Stimulus | Sensitive data (PII, SSNs, owner details) is stored or transmitted across the system |
| Stimulus source | System processes, Tenants, Staff |
| Environment | Normal operation |
| Artifact | Document Vault, Communication Hub, Database |
| Response | Mandatory AES-256 encryption at rest (AWS KMS/Vault) and TLS 1.3 for data in transit; automatic detection and masking of sensitive info in chat threads |
| Response measure | Zero instances of unencrypted PII at rest; 100% of identified sensitive fields masked in UI/Logs |

#### 2.1.3 Payment Security
| Element | Statement |
| :--- | :--- |
| Stimulus | Rent payment initialization or incoming payment webhook notification |
| Stimulus source | Tenants, Payway Webhook System |
| Environment | Normal operation |
| Artifact | Payment Gateway Service |
| Response | PCI DSS compliant tokenization via Payway (no raw card data storage); validation of digital signatures for all incoming webhooks to prevent spoofing |
| Response measure | 100% adherence to PCI DSS standards; all fraudulent/unsigned webhooks rejected |

### 2.2. Performance

#### 2.2.1 Fast Response Times
| Element | Statement |
| :--- | :--- |
| Stimulus | User requests property listings or unit availability status |
| Stimulus source | Public Users, Staff |
| Environment | Normal operation |
| Artifact | Property Management Service, Redis Cache |
| Response | Implement Redis Caching for frequently accessed listings and push updates via WebSockets |
| Response measure | API response < 1.2s for portfolios > 1,000 units; status lookups < 500ms |

#### 2.2.2 Advanced Search Efficiency
| Element | Statement |
| :--- | :--- |
| Stimulus | User performs complex filtering (amenities, geofencing, or predictive analytics) |
| Stimulus source | Tenants, Property Managers |
| Environment | Normal operation |
| Artifact | Search Service, Elasticsearch, Predictive Analytics Engine |
| Response | Utilize Elasticsearch for complex filtering and geofencing; execute heavy analytical queries against DB read-replicas |
| Response measure | Search results returned within < 1.2s; dashboard prediction load < 3s |

#### 2.2.3 Scalability for Concurrent Users
| Element | Statement |
| :--- | :--- |
| Stimulus | System processes batch lease penalties or mass push notifications |
| Stimulus source | Automated Scheduler, Event Trigger |
| Environment | Peak load / Scheduled intervals |
| Artifact | Late Fee Engine, Notification Service, Message Queues |
| Response | Batch processing for lease penalties and fire-and-forget processing via message queues (RabbitMQ/Kafka) for notifications |
| Response measure | 10,000+ leases processed in < 1 hour; 50,000+ notifications sent per minute |

### 2.3. Usability

#### 2.3.1 Smooth User Experience
| Element | Statement |
| :--- | :--- |
| Stimulus | Unit status change (Vacant to Reserved) or chat message sent |
| Stimulus source | Staff, Tenants |
| Environment | Normal operation |
| Artifact | Multi-Role Messaging Hub, Real-time Unit Engine |
| Response | Use Redis Pub/Sub or WebSockets to push status updates and messages to the UI in real-time without polling |
| Response measure | Message delivery latency < 200ms; UI updates reflected instantly |

#### 2.3.2 Easy-to-Use Interface
| Element | Statement |
| :--- | :--- |
| Stimulus | Tenant submits maintenance work order with high-resolution media |
| Stimulus source | Tenants |
| Environment | Normal operation |
| Artifact | Maintenance Service, Cloudinary/CDN |
| Response | Asynchronous thumbnail generation and delivery of media via integrated CDN (CloudFront/Cloudinary) |
| Response measure | Ticket creation response < 1s; media loads rapidly via CDN |

### 2.4. Interoperability

#### 2.4.1 Third-Party System Integration
| Element | Statement |
| :--- | :--- |
| Stimulus | Requirement for background screening, geocoding, or digital signatures |
| Stimulus source | Onboarding Service, Property Service, Lease Service |
| Environment | Normal operation |
| Artifact | External API Bridge (Checkr, Google Maps, DocuSign) |
| Response | Seamless API integration using standard protocols; implement Circuit Breaker pattern (Resilience4j) to handle external timeouts |
| Response measure | Screening request submission < 2s; automated data sync with legacy systems |

#### 2.4.2 Internal Microservice Communication
| Element | Statement |
| :--- | :--- |
| Stimulus | Distributed transaction between Payment, Accounting, and Lease services |
| Stimulus source | Payment Service |
| Environment | Normal operation |
| Artifact | Saga Pattern Orchestrator, Message Queue |
| Response | Manage distributed transactions via Saga Pattern; handle notifications asynchronously via message queues to ensure reliability |
| Response measure | 100% processing reliability; zero inconsistent states across services |

### 2.5. Modifiability

#### 2.5.1 Supporting Business Requirement Changes
| Element | Statement |
| :--- | :--- |
| Stimulus | Change in state-mandated late fee caps or legal lease clauses |
| Stimulus source | Legal Admins, Financial Admins |
| Environment | Configuration update |
| Artifact | Automated Late Fee Engine, Smart E-Lease Generator |
| Response | Rule-based generation of digital leases with automated clause customization; decoupled fee calculation logic |
| Response measure | Logic changes applied via configuration without re-deploying core services where possible |

#### 2.5.2 Zero-Downtime Updates
| Element | Statement |
| :--- | :--- |
| Stimulus | Database failover or service migration from legacy system |
| Stimulus source | DevOps / Infrastructure |
| Environment | Maintenance/Migration phase |
| Artifact | Multi-AZ Database, Legacy Sync Service |
| Response | Multi-AZ deployment for relational database; bi-directional sync with legacy property database for data parity |
| Response measure | 99.99% database availability; zero data loss during failover |

### 2.6. Availability

#### 2.6.1 Fault Tolerance and System Recovery
| Element | Statement |
| :--- | :--- |
| Stimulus | Failure of a third-party screening provider or a system component |
| Stimulus source | External Providers, Network/Hardware |
| Environment | Failure condition |
| Artifact | Resilience4j Circuit Breaker, Distributed Scheduler (Quartz/EventBridge) |
| Response | Implement Circuit Breaker pattern to prevent cascading failures; use distributed scheduler to ensure no missed maintenance cycles |
| Response measure | System remains operational during partial failures; 100% of maintenance cycles executed |

#### 2.6.2 Automated Payment Response Handling
| Element | Statement |
| :--- | :--- |
| Stimulus | High volume of incoming payment webhooks from Payway |
| Stimulus source | Payway Gateway |
| Environment | Normal/Peak operation |
| Artifact | Payment Service, Message Queue |
| Response | Handle incoming notifications asynchronously via message queues; implement idempotency keys for all requests |
| Response measure | 100% processing reliability; zero double-charging incidents |

## 3. Architectural Representation
To describe the architecture of the Property Management System, the following views are presented:

### 3.1. Logical View
This view presents the system's modular decomposition into functional components and subsystems.
Subsystems:
— **Auth Service**: Manages fine-grained RBAC, centralized identity (OIDC), and permission caching via Redis for < 50ms checks.
— **Property Service**: Handles multi-tier inventory (Portfolios, Buildings, Units) and Geo-JSON metadata for map integration.
— **Availability Service**: Maintains the dynamic status engine (Vacant, Occupied, etc.) using Redlock for strong consistency and Redis Pub/Sub for real-time UI updates.
— **Onboarding Service**: Orchestrates digital KYC and background screening with third-party API integration (Checkr/Experian).
— **Lease Service**: Manages smart e-lease generation (PDF generation < 5s) and digital signature workflows (DocuSign).
— **Payment Service**: Interfaces with Payway/Stripe for rent collection using tokenization and asynchronous webhook processing.
— **Accounting Service**: Executes the automated late fee engine and manages distributed transactions via the Saga Pattern.
— **Maintenance Service**: Tracks work order lifecycles and schedules preventive maintenance tasks using a distributed scheduler.
— **Communication Service**: Facilitates multi-role in-app chat (< 200ms latency) using WebSockets and a Redis backplane.
— **Notification Service**: Dispatches high-throughput event-driven alerts (50,000+/min) via Firebase Cloud Messaging.
— **Analytics & Reporting Service**: Provides financial dashboards using read-replicas and AI-driven vacancy predictions.
— **Document Service**: Manages the centralized vault with AES-256 encryption and S3-based storage.
— **API Gateway**: Acts as the single entry point, handling rate limiting, request routing, and security verification.

### 3.2. Implementation View
This view outlines the organization of code and deployment artifacts.
Structure:
— **Microservice-Based Architecture**: Each service is a standalone Maven/Gradle project or Python module with independent CI/CD pipelines.
— **Event-Driven Integration**: Services communicate via RabbitMQ/Kafka for asynchronous tasks (Screening, Notifications).
— **Shared Common Library**: Centralized DTOs, Exception handlers, and security filters (common/bds-common).
Technologies:
— **Backend**: Java (Spring Boot) for core services; Python (FastAPI) for AI/ML predictive analytics.
— **Frontend**: React/Next.js for role-based dashboards (Tenants, Managers, Workers).
— **Message Broker**: Redis Pub/Sub for chat/real-time; RabbitMQ or Kafka for event-driven workflows.
— **CI/CD**: Docker containerization with GitLab CI or GitHub Actions; Kubernetes for orchestration.

### 3.3. Deployment View
Describes the physical deployment of system components.
Environment:
— **Cloud Provider**: AWS (Amazon Web Services).
— **Orchestrator**: Amazon EKS (Elastic Kubernetes Service) for managing containerized microservices.
— **Load Balancer**: AWS Application Load Balancer (ALB) for SSL termination and traffic routing.
— **Database Nodes**: Amazon RDS (PostgreSQL) in Multi-AZ configuration; Amazon ElastiCache (Redis).
Deployment Example:
— **Region**: Multi-AZ deployment (e.g., us-east-1a, 1b) for high availability (99.99%).
— **Containerization**: Standard Docker images hosted in Amazon ECR.
— **Monitoring**: Amazon CloudWatch for logs; Prometheus and Grafana for system-wide performance metrics.

### 3.4. Data View
Focuses on data models and storage strategy.
Primary Storage:
— **PostgreSQL**: Used as the primary relational store for transactional data, property hierarchies, and lease records requiring atomic integrity.
— **Elasticsearch**: Dedicated engine for high-performance amenity filtering and geofencing search lookups.
— **Redis**: Utilized for real-time status caching, distributed locking (Redlock), chat backplane, and Auth permission caching.
— **AWS S3 / Cloudinary**: Scalable object storage for high-resolution floor plans, maintenance media, and signed lease PDFs.
Security Measures:
— **Encryption**: Mandatory AES-256 encryption at rest for PII and documents; TLS 1.3 for all data in transit.
— **Isolation**: Multi-tenant data isolation at the schema or database level for owner reporting.
— **Access Control**: Principle of least privilege enforced via RBAC; masking of sensitive identifiers in UI and logs.
