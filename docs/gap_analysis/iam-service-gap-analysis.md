# Gap Analysis Report: `bds-iam-service`

This report provides a comprehensive post-merge gap analysis of the newly synchronized **Identity & Access Management (IAM) Service** (`bds-iam-service`) against the **Legacy Monolith** (`/legacy`), the **Core Macroservice** (`/bds-core-macroservice`), and the **Shared Contracts** (`/bds-common`).

---

## 🔍 Executive Summary

A comprehensive audit of the `bds-iam-service` codebase reveals that while the basic authentication mechanisms (login, JWT generation, and token refresh) have been migrated, the service is currently **not production-ready** and will fail under end-to-end integration testing. 

*   **Critical Showstoppers:** The registration endpoint is completely broken (commented out), the API Gateway is missing routing rules for the service (routing all traffic to the core macroservice instead), and the core macroservice's checkout/smart contract flow is blocked due to a missing `/users/validate` endpoint in `bds-iam-service`.
*   **Feature Completeness:** High-value business logic from the monolith—specifically user ranking, performance metrics, and complex user filters (by rating, tier, points, location)—has been completely dropped.
*   **Contract Mismatch:** The service relies on locally defined response templates and enums instead of adhering to the standardized envelopes in `bds-common`.

---

## 🚫 Missing Features (Legacy vs. New)

The following features and logic from the `/legacy` codebase are missing or broken in `bds-iam-service`:

### 1. Commented-out User Registration Logic
*   **File Location:** [AuthController.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/controller/AuthController.java#L27-L39)
*   **Description:** In `AuthController.register(...)`, the actual registration call `userService.register(request)` has been commented out:
    ```java
    //userService.register(request);
    return ResponseEntity.ok().build();
    ```
    Any register request returns `200 OK` but performs no persistence, rendering user registration completely broken.

### 2. Missing User, Agent, and Owner Filtering APIs
*   **File Location:** [AccountController.java (Legacy)](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/controllers/AccountController.java#L326-L541)
*   **Description:** The legacy service allowed deep filtering of profiles:
    *   `GET /account/sale-agents`: Filters by performance tier, rating, contracts signed, current assignments, hired dates, and location (City/District/Ward).
    *   `GET /account/customers`: Filters by lead score, viewings requested, total spending, and customer tiers.
    *   `GET /account/property-owners`: Filters by contribution points, property counts, sales, rentals, and contribution tiers.
    In the new [AccountController.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/controller/AccountController.java), these have been replaced by a single, barebones `GET /api/account/users` endpoint that only filters by name and role, losing all rich search metrics.

### 3. Missing Profile Statistics Enrichment (Tiers & Metrics)
*   **File Location:** [UserServiceImpl.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/service/impl/UserServiceImpl.java#L266-L268)
*   **Description:** The user details returned in the new `UserResponse` leave the `tier`, `statisticMonth`, and `statisticAll` fields null. The code contains the comment:
    ```java
    // tier, statisticMonth, statisticAll → null tạm thời
    // Sẽ được enrich bởi API Gateway hoặc BFF sau khi có Ranking Service
    ```
    However, the API Gateway does not implement any BFF or data aggregation capabilities, resulting in empty profile stats for the client.

### 4. Missing Ranking Schedulers & Logic
*   **File Location:** [Legacy Schedulers](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/domains/ranking/scheduler/)
*   **Description:** Schedulers that periodically evaluate and update user metrics (`SaleAgentRankingScheduler`, `CustomerRankingScheduler`, and `PropertyOwnerRankingScheduler`) have not been migrated to the new service structure. While mongo-based ranking persistence exists in `bds-appointment-service`, the scheduler lifecycle is missing.

### 5. Missing "Remember Me" Support
*   **File Location:** [AuthServiceImpl.java (Legacy)](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/domains/auth/impl/AuthServiceImpl.java#L45)
*   **Description:** The legacy login flow accepted a `rememberMe` flag that adjusted JWT token expirations accordingly. The new login flow does not support this parameter.

### 6. Unlocalized and Weak Validation Constraints
*   **File Location:** [RegisterRequest.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/dto/request/RegisterRequest.java)
*   **Description:** The legacy requests used custom localized validation messages (e.g., `@NotBlank(message = "{not_blank}")`, `@Size(max = 50)`). The new request models use default Spring validation messages and lack the strict `@Size` constraint boundaries defined in the legacy models.

---

## 🔗 Integration Gaps (Core Macroservice)

The touchpoints between `bds-iam-service` and the rest of the backend platform present the following critical gaps:

### 1. Missing `/users/validate` Endpoint
*   **Touchpoint:** [UserValidationAdapter.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/adapter/out/external/UserValidationAdapter.java#L43) in `bds-core-macroservice`.
*   **Description:** During contract creation (smart rental/purchase/deposit), the core macroservice calls:
    `GET http://<iam-service>/users/validate?userId={userId}&role={role}`
    Since this validation endpoint is not implemented in `bds-iam-service`, it returns an HTTP `404 Not Found`. `UserValidationAdapter` catches this, throws a `BusinessException`, and aborts checkout transactions entirely.

### 2. Missing API Gateway Routing Configuration
*   **Touchpoint:** [application.yaml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-api-gateway/src/main/resources/application.yaml#L25) in `bds-api-gateway`.
*   **Description:** The Gateway's routing table has no entry for `bds-iam-service`. Instead, the catch-all `/**` predicate routes all unmatched traffic directly to the `core-service` (`bds-core-macroservice`). Clients attempting to hit `/api/auth/**` or `/api/account/**` will be misrouted to the core macroservice and receive HTTP 404s.

---

## 📡 Inter-Service Communication Risks

### 1. Hardcoded Local Port Mismatch
*   **Touchpoint:** [UserValidationAdapter.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/adapter/out/external/UserValidationAdapter.java#L26)
*   **Description:** The core macroservice defaults its `iam.service-url` property to `http://localhost:8081`. However, `8081` is the port allocated to the core macroservice itself. In local development environments, this causes the core service to call itself in an infinite loop instead of hitting the actual IAM service port.

### 2. Synchronous Coupling & Lack of Resiliency
*   **Touchpoint:** Synchronous HTTP GET validation during contract generation.
*   **Description:** The core macroservice relies on synchronous `RestTemplate` calls to validate users. If `bds-iam-service` experiences high latency or downtime:
    *   There is no circuit breaker (e.g., Resilience4j) to prevent cascading thread exhaustion.
    *   No HTTP client timeout properties are configured in the macroservice configuration, making it vulnerable to hanging connections.

---

## 📜 Contract Standardization (bds-common)

`bds-iam-service` violates our shared contract library guidelines in several key areas:

### 1. Duplicated Envelope Structures (`SingleResponse` & `PageResponse`)
*   **File Locations:** 
    *   [SingleResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/dto/response/SingleResponse.java)
    *   [PageResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/dto/response/PageResponse.java)
    *   [ResponseFactory.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/controller/base/ResponseFactory.java)
*   **Description:** The service defines its own response wrappers containing `statusCode` (integer). This violates the shared standard [ApiResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common/src/main/java/com/se/bds/common/dto/ApiResponse.java) inside `/bds-common`, which uses a boolean `success` field.

### 2. Duplicated Enums (`RoleEnum` & `StatusProfileEnum`)
*   **File Location:** [Constants.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/util/Constants.java)
*   **Description:** `RoleEnum` and `StatusProfileEnum` are declared locally. They duplicate the roles and status representations present in other modules (e.g., `Role` enum in `bds-core-macroservice`). These should be unified in `bds-common` to ensure consistent entity mapping and database representations.

---

## 🛠️ Actionable Remediation Steps

Below is the prioritized roadmap to bring the `bds-iam-service` to feature parity and correct integration.

### Phase 1: Critical Showstoppers (Immediate Action Required)
- [ ] **Fix User Registration:** Uncomment `userService.register(request)` in `AuthController.register` and resolve any missing compile dependencies.
- [ ] **Expose User Validation Endpoint:** Create a `/users/validate` REST controller endpoint in `bds-iam-service` that accepts `userId` and `role` and returns whether the user exists and is active.
- [ ] **Configure Gateway Routing:** Add routing entries for `bds-iam-service` (`/api/auth/**` and `/api/account/**`) in the gateway's [application.yaml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-api-gateway/src/main/resources/application.yaml).
- [ ] **Fix Port Configuration:** Change default `iam.service-url` in the macroservice config to point to the correct IAM port (e.g., default `http://localhost:8085` or dynamic service discovery).

### Phase 2: Feature Parity & Schedulers
- [ ] **Re-implement Advanced Filtering APIs:** Add filtering endpoints back to `AccountController` for sale agents, customers, and property owners.
- [ ] **Migrate Schedulers:** Move the ranking schedulers from the legacy domain context to run either in `bds-appointment-service` (where the Mongo data source resides) or establish microservice communication to synchronize metrics back to IAM profile tables.
- [ ] **Restore Remember-Me logic:** Add support for the remember-me flag in `AuthServiceImpl` and parameterize token TTL.

### Phase 3: Contract Standardization
- [ ] **Refactor to Shared Responses:** Replace local `SingleResponse` and `PageResponse` with `com.se.bds.common.dto.ApiResponse` from the `bds-common` dependency.
- [ ] **Centralize Enums:** Extract `RoleEnum` and `StatusProfileEnum` to the `bds-common` module and delete local definitions.
- [ ] **Restore Localized Validation:** Add localized JSR validation messages back into DTOs.

### Phase 4: Resiliency & Communications
- [ ] **Add Circuit Breaker:** Implement Resilience4j circuit breakers and fallback behavior in the core macroservice's `UserValidationAdapter`.
- [ ] **Configure Connection Timeouts:** Set explicitly low connection and read timeouts on the `RestTemplate` used for user validations.
