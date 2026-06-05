Here is the Cross-Service Standardization Report based on the comprehensive gap analysis of the `api-gateway`, `appointment-service`, `financial-service`, `iam-service`, `moderation-service`, `notification-service`, and `search-service`.

---

# 🏗️ Cross-Service Standardization & Architecture Report

As we transition from the `/legacy` monolith to a distributed microservices architecture, a macro-level review of the newly migrated services reveals severe structural inconsistencies. The teams have successfully modularized the codebase but have largely replicated monolithic anti-patterns in isolation, resulting in a system that is unbuildable, severely coupled, and vulnerable to cascading failures.

The following outlines systemic gaps, shared deficiencies, and the immediate architectural standards required to stabilize the platform.

---

## 1. Global Contract & DTO Deficits (`bds-common`)

Almost all services suffer from "contract isolation"—ignoring the shared `/bds-common` library in favor of defining local, duplicated schema models. This breaks API consistency across the platform and prevents unified frontend integration.

### Identified Deficits & Affected Services:
*   **Duplicated Envelope Responses:** Services are rolling their own response wrappers (e.g., `SingleResponse`, `PageResponse`, `AbstractBaseResponse`) which serialize inconsistently (using `statusCode` vs. boolean `success` flags).
    *   *Affected Services:* `api-gateway`, `appointment-service`, `financial-service`, `iam-service`, `moderation-service`, `notification-service`, `search-service`.
*   **Locally Defined Enums:** Domain enumerations required for cross-service events are defined locally in `Constants.java` files, creating high risks of serialization mismatches during inter-service messaging.
    *   *Affected Services:* `appointment-service`, `financial-service` (`PaymentType`, `PaymentStatus`), `iam-service` (`RoleEnum`), `moderation-service` (`ViolationStatusEnum`), `notification-service` (`NotificationTypeEnum`).
*   **Isolated Exception Handling:** Controllers lack unified exception advice, bypassing standard business exceptions for custom errors (e.g., local `DetailedErrorResponse` or `NotFoundException`).
    *   *Affected Services:* `financial-service`, `moderation-service`, `notification-service`, `search-service`.

### 🎯 Mandatory Standardization Strategy:
1.  **Enforce `ApiResponse<T>`:** Add the strict requirement that every REST endpoint across all services returns `com.se.bds.common.dto.ApiResponse`. All local response wrappers (`SingleResponse`, `PageResponse`, etc.) must be deleted.
2.  **Centralize Domain Enums:** Extract all shared enums to a new package in `/bds-common` (e.g., `com.se.bds.common.enums`) to guarantee type-safe Kafka serialization.
3.  **Standardize Exception Handlers:** Require all services to implement `@RestControllerAdvice` mapping directly to `com.se.bds.common.exception.BusinessException`.

---

## 2. Systemic Integration Patterns (`coremacroservice`)

The integration between the `/bds-core-macroservice` and the satellite microservices is functionally broken. The core service is either completely blind to the new services or attempts to communicate with them using brittle, synchronous anti-patterns.

### Identified Deficits & Affected Services:
*   **Missing Event-Driven State Synchronization (Kafka Deficits):** The core service performs actions but fails to broadcast state changes, leaving downstream services paralyzed or out-of-sync. Conversely, core fails to listen to downstream actions.
    *   *Affected Services:* 
        *   `financial-service` (fails to publish `payment-succeeded`, blocking core contracts).
        *   `appointment-service` (fails to consume `property-status-changed`, resulting in double-booking).
        *   `moderation-service` (fails to publish `penalty-applied`, meaning admin bans are ignored by core).
        *   `search-service` (core fails to publish `property-searched` events, dropping analytics).
        *   `notification-service` (core replaced Kafka event publishing with raw logging, halting push notifications).
*   **Core Macroservice "Monolith Fallbacks":** The core macroservice is bypassing external service calls entirely, relying on local mock adapters (e.g., mock Payway adapters) or dropping the logic entirely (e.g., dropping Top-K searches).
    *   *Affected Services:* `financial-service`, `search-service`.
*   **API Gateway Routing Blackholes:** The API Gateway is missing routing predicates for nearly all new services. As a result, its catch-all route (`/**`) sends all traffic to the core macroservice, which returns 404s.
    *   *Affected Services:* `appointment-service`, `financial-service`, `iam-service`, `moderation-service`.

### 🎯 Mandatory Standardization Strategy:
1.  **Define a Global Event Catalog in `bds-common`:** Establish standard Kafka Event DTOs (e.g., `PaymentSucceededEvent`, `ViolationPenaltyAppliedEvent`, `PropertySearchedEvent`) shared via the common library.
2.  **Dynamic Service Registry Integration:** Eliminate hardcoded routes in the Gateway. Require all services, including `coremacroservice`, to implement `spring-cloud-starter-netflix-eureka-client` to enable dynamic load balancing and API gateway discovery.

---

## 3. Shared Business Logic & Utility Gaps

Critical business functionalities present in the monolith (`/legacy`) were dropped entirely during the split. 

### Identified Deficits & Affected Services:
*   **Orphaned Schedulers & Background Jobs:** The monolith utilized scheduled CRON jobs for daily/monthly aggregations, metrics, and alerts. These were not migrated to the respective microservices.
    *   *Affected Services:* `appointment-service` (Ranking Scheduler missing), `iam-service` (Ranking Logic missing), `moderation-service` (Violation Stats Scheduler missing), `notification-service` (Contract Reminder Schedulers missing), `search-service` (Property Stats Scheduler missing).
*   **RBAC & Downstream Security Contexts:** The `api-gateway` validates JWTs but strips user roles. Downstream services either execute dangerous local DB lookups to rebuild the security context, blindly trust the `X-User-Id` header, or leave APIs entirely unprotected.
    *   *Affected Services:* `api-gateway` (Role stripping), `appointment-service` (Sync DB lookup), `iam-service` & `notification-service` (Blind trust), `moderation-service` (`permitAll()` exposed to public).

### 🎯 Mandatory Standardization Strategy:
1.  **Standardize Security Middleware in `bds-common`:** Develop a shared `bds-spring-security-starter` library. The `api-gateway` MUST mutate requests to include both `X-User-Id` and `X-User-Roles`. Downstream services simply import this library, which automatically provides a `HeaderAuthenticationFilter` to construct a stateless Spring `SecurityContext`, reinstating `@PreAuthorize("hasRole('ADMIN')")` capabilities.
2.  **Centralize/Refactor Schedulers:** Schedulers should not rely on direct database polling across service boundaries. We must adopt an architecture where a central chron-service triggers actions via Kafka events, or each domain-service manages its own localized scheduler leveraging asynchronous read-models.

---

## 4. Recurring Architectural Anti-Patterns

A concerning amount of "distributed monolith" characteristics have been introduced across the service boundaries.

### Identified Deficits & Affected Services:
*   **Synchronous Wait Traps & Missing Resiliency:** Inter-service HTTP calls and third-party gateway integrations are established using raw `RestTemplate` or `HttpClient` instances with zero connection timeouts, read timeouts, or circuit breakers. Under load, one hanging service will rapidly exhaust the thread pools of the entire cluster.
    *   *Affected Services:* `financial-service` (PayPal/Payway hangs), `iam-service` (Core macro validation hangs), `moderation-service` (Missing client resilience), `notification-service` (FCM push hangs), `search-service` (Synchronous reads hang).
*   **Build & Environment Isolation:** Services are missing from the Maven root POM `<modules>` declaration and are not configured to inherit from the `batdongsan-platform` parent POM. This breaks CI/CD pipelines and causes Java version fragmentation (mixing Java 17 and 21).
    *   *Affected Services:* `api-gateway`, `appointment-service`, `moderation-service`, `notification-service`, `search-service`.
*   **Hardcoded Ports & Collisions:** Services are hardcoding `localhost` connection URLs and colliding on default Spring Boot ports (specifically `8081` and `8083`), making the stack impossible to run locally out-of-the-box.
    *   *Affected Services:* `api-gateway`, `appointment-service`, `iam-service`, `notification-service`.

### 🎯 Mandatory Standardization Strategy:
1.  **Parent POM Enforcement:** Immediately block any PRs for microservices that do not declare `<parent>batdongsan-platform</parent>` and register themselves in the root `pom.xml`. 
2.  **Mandate Feign & Resilience4j:** Ban raw `RestTemplate` and `HttpClient` usage for internal service-to-service calls. Provide a standard configuration in `bds-common` wrapping `spring-cloud-starter-openfeign` with strict `Resilience4j` timeouts (e.g., 2000ms max) and circuit breaker fallbacks. 
3.  **Strict Port & Environment Profiles:** Standardize a central `docker-compose.yml` and explicitly assign base ports (e.g., Core: 8081, Gateway: 8080, Search: 8082, Notification: 8083, IAM: 8084, Appointment: 8085, Financial: 8086, Moderation: 8087) via localized `application.yml` profiles pending the deployment of Eureka.