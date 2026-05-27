# Architectural Analysis & Microservices Migration Strategy

Here is a comprehensive architectural analysis of the current `BatDongScam` backend codebase, along with a strategic roadmap for migrating from its current modular monolith state to a distributed Spring Boot microservices architecture.

## 1. Codebase Topography & Legacy Patterns

### Frameworks & Hidden Dependencies
Contrary to the initial assumption of a strictly "plain Java" monolith, the application is actually a modern Spring Boot application (v3.5.6) structured as a modular monolith. 
*   **Hidden Libraries & SDKs:** The application utilizes several third-party SDKs that will need careful extraction, including `firebase-admin` (likely for push notifications), `cloudinary-http44` (for media storage), `jjwt` (for stateless authentication), and `rest-api-sdk` (PayPal integration).
*   **Data Layer Duopoly:** The `pom.xml` reveals a mix of `spring-boot-starter-data-jpa` (PostgreSQL) and `spring-boot-starter-data-mongodb` (MongoDB). This polyglot persistence is an excellent foundation for microservices.

### Custom Implementations & Framework Features
*   **Configurations:** Custom `WebSecurityConfig`, `WebMvcConfig`, and `AppConfig` centralize CORS, exception handling, and filter chains. These will eventually be moved to an API Gateway (e.g., Spring Cloud Gateway) and individual service security configurations.
*   **Stateless by Design:** The `SessionCreationPolicy.STATELESS` in the security config indicates the system is already using stateless JWTs rather than sticky sessions, which is a massive advantage for a distributed migration.

### Global State & Concurrency Risks
*   **Thread Safety:** The codebase avoids dangerous `ThreadLocal` hacks. It correctly uses `ThreadLocalRandom.current()` for random data generation (e.g., `TimeGenerator`, `SearchLogDummyData`) rather than shared `Random` instances, proving decent concurrency hygiene. 
*   **Asynchronous Processing:** There is heavy use of Spring's `@Async` (e.g., within `SearchServiceImpl` and `SearchLogDummyData`). In a microservices environment, memory-based `@Async` thread pools are risky if a node crashes mid-execution. These must be replaced with durable message queues.

---

## 2. Domain-Driven Design (DDD) & Service Boundaries

Based on the explicit package structures (`com.se100.bds.models.entities` vs `schemas` and `services.domains`), the system exhibits strong implicit bounded contexts. 

### Proposed Logical Microservice Boundaries
1.  **Identity & Access Management (IAM) Service:** (`auth`, `user`) Manages JWT issuance, role-based access control, and user profile data.
2.  **Property Catalog Service:** (`property`, `location`, `document`) Manages property listings, media approval, and hierarchical location data (City ↔ District ↔ Ward).
3.  **Transaction & Workflow Service:** (`contract`, `appointment`, `violation`) The complex core domain handling the lifecycle of viewing appointments and smart contracts (Mortgage, Rental).
4.  **Financial & Payment Service:** (`payment`) Integrates with PayOS/PayPal, calculates commission structures, and handles "Owner Payouts" and "Company Refunds". 
5.  **Search & Analytics Service:** (`search`, `report`, `customer` favorites, `ranking`) The high-read domain already backed by MongoDB for generating stats and search logs.

### "God Classes" & Crossing Boundaries
*   The **Property** and **User/Customer** entities are clearly "God Classes" in this monolith. A `Contract` binds a Property, an Agent (User), and a Customer (User). 
*   **Refactoring Need:** You cannot share these JPA `@Entity` classes across microservices. Instead, the Transaction Service should only store `propertyId` and `customerId` as UUIDs, requesting rich relational data via API or caching it locally, rather than relying on `@ManyToOne` Hibernate fetches.

---

## 3. Data Management Constraints

### Data Layer Interactions
*   The system uses **Spring Data JPA** (40 `@Entity` tables via PostgreSQL) for ACID transactions and **Spring Data MongoDB** (28 `@Document` collections) for logs, reports, and search indices.

### Risks for "Database-per-Service"
*   **The Hierarchical Location Trap:** `Property` is deeply tied to `Ward`, `District`, and `City`. If Property is moved to its own DB, joining location data for search/filtering breaks.  
*   **SearchLog & Report Coupling:** The `SearchLogDummyData` service relies heavily on fetching `User`, `Property`, `City`, `District`, and `Ward` via JPA repositories to construct the MongoDB reports. In a microservice environment, the Analytics DB will not have synchronous access to the PostgreSQL tables.

---

## 4. Inter-Service Communication Strategy

To maintain hyperscale performance while keeping services decoupled, communication must be split:

### Synchronous Communication (REST/gRPC via Feign Clients)
*   **Auth Validations:** Any service requiring immediate state confirmation.
*   **API Gateway Data Aggregation:** The Gateway parallel-fetching Property details and Agent details to construct a unified "Appointment Viewing Card".

### Asynchronous, Event-Driven (Kafka / RabbitMQ)
Currently, there is **no evidence** of Kafka/RabbitMQ in the `pom.xml`. To scale, the following workflows must become Event-Driven via an Event Bus:
*   **Event `PropertyCreatedEvent`:** Property Catalog publishes this. The Search Service consumes it to update the MongoDB search index.
*   **Event `ContractSignedEvent`:** Transaction Service publishes this. Financial Service consumes it to auto-generate the initial Deposit Payment schedule.
*   **Event `PaymentSucceededEvent`:** Financial Service publishes this. Transaction Service consumes it to transition the Contract to `ACTIVE`.

---

## 5. Migration Roadmap (Strangler Fig Pattern)

We will avoid a "Big Bang" rewrite. Instead, we will wrap the current monolith behind an API Gateway and gradually extract domains, routing traffic to the new services while the monolith handles the rest.

*   **Phase 1: Infrastructure Foundations** 
    Introduce Spring Cloud Gateway, a Service Registry (Eureka/Consul), and centralized Config Server. Route all traffic to the existing monolith via the gateway. Introduce Kafka/RabbitMQ messaging infrastructure.
*   **Phase 2: Extract the First Service (Proof of Concept)**
    See recommendation below.
*   **Phase 3: Extract Financial & Payment Service**
    Move the `payment` domain, PayPal integrations, and webhooks. Set up pub/sub to let the monolith know when payments succeed.
*   **Phase 4: Extract IAM and User Management**
    Shift JWT generation and Firebase sync out of the monolith.
*   **Phase 5: Dismantle the Core (Property & Contract)**
    The hardest phase. Split the relational constraints between Properties and Contracts, fully implementing the database-per-service paradigm.

### Recommended Proof-of-Concept Service
The **Search & Analytics Service** is the absolute best candidate to extract first. 
*   **Why?** It is already using a distinct database (MongoDB). It handles heavy read/write logs (`search_logs`), which shouldn't compete for memory with core transactional workflows in the monolith. 
*   **How?** Move all MongoDB schemas, repositories, and reporting logic into a new Spring Boot application. Let the monolith publish events (e.g., `UserSearchedLocationEvent`, `PropertyViewedEvent`) to Kafka, and have this new microservice consume those events and write to MongoDB. Route all `GET /reports/*` and `GET /search/*` frontend calls through the API Gateway directly to this new service.
