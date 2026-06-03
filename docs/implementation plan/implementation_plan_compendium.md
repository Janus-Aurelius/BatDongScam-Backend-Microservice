# Master Implementation Plan: Microservices Synchronization & Stabilization

This Master Implementation Plan sequences the architectural remediation for the BatDongSan microservices platform. To prevent duplicate effort, merge conflicts, and continued contract fragmentation, execution is strictly phased. **No individual service feature work may commence until Phase 0 (The Global Foundation) is completed and merged.**

---

## Phase 0: The Global Foundation (Pre-Requisites)

This phase establishes the shared contracts, build constraints, and global middleware required across all microservices. These tasks must be executed by a dedicated platform/core team.

### 1. Build & Environment Enforcement
*   **Parent POM Unification:** Enforce `<parent>batdongsan-platform</parent>` across all services. Set Java version strictly to `21`. Register all newly migrated microservices in the root `pom.xml` `<modules>` list.
*   **Port & Environment Standardization:** Establish a global `docker-compose.yml` and local `.env`/`application.yml` profiles with strict base ports to eliminate local collisions:
    *   `api-gateway`: 8080 | `bds-core-macroservice`: 8081 | `search-service`: 8082 | `notification-service`: 8083 | `iam-service`: 8084 | `appointment-service`: 8085 | `financial-service`: 8086 | `moderation-service`: 8087
*   **Service Discovery:** Deploy `eureka-server` as the central registry.

### 2. `bds-common` Updates (Shared Contracts)
The `/bds-common` library must be updated to include the following before any downstream service imports it:
*   **Global Response Envelopes:** Implement the standard `ApiResponse<T>` and `PagedData<T>` classes. Include the global `BusinessException` and a standard `@RestControllerAdvice` base class.
*   **Centralized Enums:** Extract and define:
    *   *Identity:* `RoleEnum`, `StatusProfileEnum`, `CustomerTierEnum`, `PerformanceTierEnum`, `ContributionTierEnum`.
    *   *Financial:* `PaymentType`, `PaymentStatus`.
    *   *Moderation:* `ViolationReportedTypeEnum`, `ViolationTypeEnum`, `ViolationStatusEnum`, `PenaltyAppliedEnum`, `MediaTypeEnum`.
    *   *Notification:* `NotificationTypeEnum`, `NotificationStatusEnum`, `RelatedEntityTypeEnum`.
*   **Global Event Catalog (Kafka):** Define standard POJO/Record schemas for:
    *   `PaymentCompletedEvent`, `ViolationPenaltyAppliedEvent`, `PropertySearchedEvent`, `NotificationRequestEvent`, `PropertyStatusChangedEvent`.
*   **Shared Security Starter (`bds-spring-security-starter`):** Create a reusable module containing the `HeaderAuthenticationFilter` that automatically parses `X-User-Id` and `X-User-Roles` headers into a Spring `SecurityContext`.

### 3. Shared Infrastructure & Gateway Prep
*   **API Gateway Updates:** Implement routing predicates for all standard services. Modify `JwtAuthenticationFilter` to extract user roles from the JWT and mutate downstream requests to include the `X-User-Roles` header.
*   **Core Macroservice Base Setup:** Remove monolithic mock adapters (e.g., Payway mock). Expose necessary internal APIs (`/api/internal/locations/...` and `/api/users/{userId}/fcm-token`) for downstream Feign clients.

---

## Phase 1: Master Service Implementation Index

Once Phase 0 is released, individual service teams may work in parallel. Each team must adopt the Phase 0 standards before migrating their unique business logic.

### `api-gateway`
*   **Standardized Implementations Required:** Adopt Parent POM structure. Delete custom JSON error builders and utilize `bds-common`'s `ApiResponse` for filter exceptions. Add Eureka Client.
*   **Service-Specific Implementations:** 
    *   Configure global Resilience4j timeouts for HTTP client connections.
    *   Implement "Mock Token Bypass" logic to facilitate local testing for legacy test profiles (`admin`, `agent1`).
    *   Restore missing public paths (`/ws/**`, `/bookings/payment/**`).

### `appointment-service`
*   **Standardized Implementations Required:** Delete local `AbstractBaseDataResponse`, `SingleResponse`, and local Enums. Import `bds-common` and apply `ApiResponse`. Adopt `bds-spring-security-starter` (removing local synchronous DB lookups for auth).
*   **Service-Specific Implementations:** 
    *   Decouple JPA mappings: Replace cross-service `@ManyToOne` entity relationships with raw UUIDs.
    *   Implement Kafka listener for `PropertyStatusChangedEvent` to maintain local replica data.
    *   Port the legacy `SaleAgentRankingScheduler` (running on local MongoDB).
    *   Replace local notification DB writes with Kafka `NotificationRequestEvent` publishes.

### `financial-service`
*   **Standardized Implementations Required:** Strip local response wrappers and local payment enums in favor of `/bds-common`. Implement generic exception handler mapping to `BusinessException`. Configure Resilience4j + Feign.
*   **Service-Specific Implementations:** 
    *   Implement cryptographic signature verification for PayPal webhooks.
    *   Complete stubbed PayOS webhook logic.
    *   Create the `Payout` database entity to track outbound transaction histories.
    *   Publish `PaymentCompletedEvent` to Kafka upon successful webhook processing.

### `iam-service`
*   **Standardized Implementations Required:** Implement `ApiResponse<T>` and `PagedData<T>`. Drop local `RoleEnum` for the shared contract. Adopt standard localization configurations for validation boundaries.
*   **Service-Specific Implementations:** 
    *   Fix broken (commented-out) user registration logic.
    *   Expose the `/users/validate` REST endpoint required by the Core Macroservice checkout flow.
    *   Restore legacy advanced profile filtering (`/account/sale-agents`, etc.) and enrich them using Feign calls to `appointment-service`'s ranking API.
    *   Restore `rememberMe` JWT token expiration logic.

### `moderation-service`
*   **Standardized Implementations Required:** Adopt `ApiResponse` and common Violation Enums. Remove `.permitAll()` from admin routes and apply `@PreAuthorize` via the new `bds-spring-security-starter`.
*   **Service-Specific Implementations:** 
    *   Implement Feign clients to `coremacroservice` and `iam-service` to validate reported properties/users (using a Fail-Secure policy).
    *   Publish `ViolationPenaltyAppliedEvent` to Kafka when an admin resolves a report with a penalty.
    *   Replace `MockFileStorageService` with a real Cloudinary integration.
    *   Configure dual-datasource (Postgres + Mongo) and port the `ViolationReportScheduler` for analytics.

### `notification-service`
*   **Standardized Implementations Required:** Delete local response envelopes and notification enums. Adopt `ApiResponse`. Apply `bds-spring-security-starter` to secure mailbox endpoints.
*   **Service-Specific Implementations:** 
    *   Implement Kafka consumers for `payment-completed`, `contract-status-changed`, and `payment-due` events.
    *   Implement a Feign client to dynamically resolve missing FCM tokens from `coremacroservice`.
    *   Implement a Fail-Safe fallback (save to DB but skip FCM push) if token resolution fails or Firebase times out.

### `search-service`
*   **Standardized Implementations Required:** Correct Maven package structure (`com.se.bds.search`). Delete local error models and apply `ApiResponse`.
*   **Service-Specific Implementations:** 
    *   Implement Kafka consumer for `PropertySearchedEvent`.
    *   Implement concurrency-safe (MongoDB `$inc`) atomic updates for real-time aggregation.
    *   Port `PropertyStatisticsReportScheduler` and configure Feign clients to fetch valid Location/PropertyType IDs from internal Core APIs.

### `bds-core-macroservice` (Core Integration Refactoring)
*   **Standardized Implementations Required:** Implement Eureka Client. Replace raw `RestTemplate` usages with OpenFeign + Resilience4j.
*   **Service-Specific Implementations:** 
    *   Publish `PropertySearchedEvent`, `PaymentDueEvent`, and `PaymentOverdueEvent` via the `KafkaEventBridge` (replacing legacy monolithic scheduler calls).
    *   Implement Kafka consumers to execute actions based on `ViolationPenaltyAppliedEvent` (e.g., suspending users).
    *   Implement `SearchServiceClient` (Feign) to re-integrate Top-K popularity filters into property queries.

---

## Phase 2: Global Verification Strategy

To guarantee the architecture remains decoupled and contract-compliant, we will implement the following systemic verifications:

### 1. Contract & Architecture Enforcement (CI/CD)
*   **Maven Enforcer Plugin:** Configure rules in the root `pom.xml` to fail the build if banned classes (e.g., `RestTemplate`, `HttpClient`, or local `SingleResponse` duplicates) are detected in microservice modules.
*   **ArchUnit Testing:** Introduce global architecture tests enforcing that:
    *   All `@RestController` methods return `com.se.bds.common.dto.ApiResponse`.
    *   No service (except Gateway) bypasses the `bds-spring-security-starter` context injection.
    *   Cross-service synchronous calls strictly use interfaces annotated with `@FeignClient`.

### 2. Cross-Service Integration Testing
*   **Kafka Event E2E Flows (TestContainers):** Implement cross-service integration tests simulating message propagation. 
    *   *Example:* Gateway -> Checkout (Core) -> Webhook (Financial) -> `PaymentCompletedEvent` (Kafka) -> Contract Activation (Core) & Receipt Push (Notification).
*   **Resiliency & Chaos Testing:** Utilize Toxiproxy or Chaos Mesh in lower environments to simulate network partitions (e.g., making `search-service` unresponsive). Assert that `coremacroservice` circuit breakers open gracefully, returning fallback data without exhausting Tomcat connection threads.

---

**Is this Master Sequence approved for delegation to the development team?**