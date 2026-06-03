# Implementation Plan - `bds-iam-service` Gap Remediation

This document outlines the detailed technical design, roadmap, risk assessment, and verification plan for resolving all identified feature, integration, and contract gaps in `bds-iam-service`.

## 1. Implementation Roadmap (Phased Execution)

---

### Phase A: Contract & DTO Standardization

#### [NEW] [RoleEnum.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common/src/main/java/com/se/bds/common/enums/RoleEnum.java)
- **Action Required:** Create the centralized `RoleEnum` with values `ADMIN`, `SALESAGENT`, `PROPERTY_OWNER`, and `CUSTOMER`, including helper method `get(String name)`.
- **Dependencies:** Standard Java SE library.

#### [NEW] [StatusProfileEnum.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common/src/main/java/com/se/bds/common/enums/StatusProfileEnum.java)
- **Action Required:** Create the centralized `StatusProfileEnum` with values `ACTIVE`, `SUSPENDED`, `PENDING_APPROVAL`, `DELETED`, and `REJECTED`, including helper method `get(String name)`.
- **Dependencies:** Standard Java SE library.

#### [NEW] [CustomerTierEnum.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common/src/main/java/com/se/bds/common/enums/CustomerTierEnum.java)
- **Action Required:** Migrate the `CustomerTierEnum` (`BRONZE`, `SILVER`, `GOLD`, `PLATINUM`) to `bds-common`.
- **Dependencies:** Standard Java SE library.

#### [NEW] [PerformanceTierEnum.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common/src/main/java/com/se/bds/common/enums/PerformanceTierEnum.java)
- **Action Required:** Migrate the `PerformanceTierEnum` (`BRONZE`, `SILVER`, `GOLD`, `PLATINUM`) to `bds-common`.
- **Dependencies:** Standard Java SE library.

#### [NEW] [ContributionTierEnum.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common/src/main/java/com/se/bds/common/enums/ContributionTierEnum.java)
- **Action Required:** Migrate the `ContributionTierEnum` (`BRONZE`, `SILVER`, `GOLD`, `PLATINUM`) to `bds-common`.
- **Dependencies:** Standard Java SE library.

#### [DELETE] [Constants.java (bds-iam-service)](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/util/Constants.java)
- **Action Required:** Delete local `RoleEnum` and `StatusProfileEnum` from `Constants.java`. Reference the new enums inside `com.se.bds.common.enums` instead.
- **Dependencies:** `bds-common` dependency.

#### [MODIFY] [Constants.java (bds-appointment-service)](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-appointment-service/src/main/java/microservices/appointmentservice/utils/Constants.java)
- **Action Required:** Delete local definitions of `RoleEnum`, `StatusProfileEnum`, `CustomerTierEnum`, `PerformanceTierEnum`, and `ContributionTierEnum`. Update imports to point to `bds-common` packages.
- **Dependencies:** `bds-common` dependency.

#### [NEW] [PagedData.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/dto/response/PagedData.java)
- **Action Required:** Create a generic pagination container to represent items along with paging metadata (page, size, totalElements, totalPages).
- **Dependencies:** Lombok annotations.

#### [MODIFY] [ResponseFactory.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/controller/base/ResponseFactory.java)
- **Action Required:** Refactor methods to return `ResponseEntity<ApiResponse<T>>` and `ResponseEntity<ApiResponse<PagedData<T>>>` from the shared `bds-common` library, instead of using custom `SingleResponse` or `PageResponse`.
- **Dependencies:** `com.se.bds.common.dto.ApiResponse` import.

#### [DELETE] [SingleResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/dto/response/SingleResponse.java) & [PageResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/dto/response/PageResponse.java)
- **Action Required:** Delete local custom response classes as they are fully replaced by the unified `ApiResponse` schema.

---

### Phase B: Business Logic Parity

#### [MODIFY] [AuthController.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/controller/AuthController.java)
- **Action Required:** Uncomment and inject `UserService` via constructor injection. In `register(...)`, uncomment the call to `userService.register(request)` to ensure registered users are stored in the database.
- **Dependencies:** `com.se361.iam_service.service.UserService` injection.

#### [MODIFY] [LoginRequest.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/dto/request/LoginRequest.java)
- **Action Required:** Add a `Boolean rememberMe` parameter to login request fields.
- **Dependencies:** Standard Java types.

#### [MODIFY] [AuthService.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/service/AuthService.java) & [AuthServiceImpl.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/service/impl/AuthServiceImpl.java)
- **Action Required:** Update `login` method signature to take `rememberMe` flag. Pass `rememberMe` to `generateTokens`.
- **Dependencies:** None.

#### [MODIFY] [JwtTokenProvider.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/security/JwtTokenProvider.java)
- **Action Required:** Overload or update token generation methods to support `rememberMe`. If `rememberMe` is true, extend the refresh token expiration TTL (e.g., 30 days) to prevent frequent logouts.
- **Dependencies:** JWT libraries.

#### [NEW] [messages.properties](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/resources/messages.properties)
- **Action Required:** Create the properties bundle in IAM resources containing localized error strings (`not_blank`, `max_length`, `invalid_email`).
- **Dependencies:** Standard Spring validation localization support.

#### [MODIFY] [RegisterRequest.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/dto/request/RegisterRequest.java) & [UpdateAccountDto.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/dto/request/UpdateAccountDto.java)
- **Action Required:** Add `@Size(max = 50)` constraint boundaries and update validator messages to refer to localized resource keys (e.g., `@NotBlank(message = "{not_blank}")`).
- **Dependencies:** Jakarta validation API.

#### [NEW] [RankingController.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-appointment-service/src/main/java/microservices/appointmentservice/controllers/RankingController.java)
- **Action Required:** Expose REST controller endpoints mapping MongoDB-based ranking data to allow other microservices (like `bds-iam-service`) to retrieve ranking tier details, scores, and month/career profiles.
- **Dependencies:** Inject `RankingService` implementation.

#### [MODIFY] [AccountController.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/controller/AccountController.java)
- **Action Required:**
  - Add Advanced Filtering endpoints from the legacy monolith:
    - `GET /api/account/sale-agents`
    - `GET /api/account/customers`
    - `GET /api/account/property-owners`
  - Fetch user tiers and metric details by calling `bds-appointment-service`'s `RankingController` via RestTemplate/FeignClient to enrich profile details.
- **Dependencies:** RestTemplate configuration or FeignClient client setup.

#### [NEW] [Legacy Schedulers in bds-appointment-service](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-appointment-service/src/main/java/microservices/appointmentservice/schedulers/)
- **Action Required:** Migrate the legacy schedulers (`SaleAgentRankingScheduler.java`, `CustomerRankingScheduler.java`, `PropertyOwnerRankingScheduler.java`) to run on `bds-appointment-service`. These schedulers will run periodically using MongoDB datasources to evaluate rankings and save performance metrics.
- **Dependencies:** Spring `@Scheduled` configuration.

---

### Phase C: Integration & Communication

#### [NEW] [UserController.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/controller/UserController.java)
- **Action Required:** Create the user controllers exposing the `/users/validate?userId={userId}&role={role}` validation endpoint. Query `userService` to assert whether the user exists, is active, and matches the specified role.
- **Dependencies:** `com.se361.iam_service.service.UserService` dependency.

#### [MODIFY] [SecurityConfig.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/java/com/se361/iam_service/config/SecurityConfig.java)
- **Action Required:** Add `/users/validate` to the permit-all authorize request matchers to ensure the core macroservice can validate user parameters without bearing user authentication JWT tokens.
- **Dependencies:** Spring Security configuration structures.

#### [MODIFY] [application.yml (bds-iam-service)](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-iam-service/src/main/resources/application.yml)
- **Action Required:** Assign default port `8086` to prevent collisions with the Gateway (`8080`), Core service (`8081`), and Appointment service (`8085`).
- **Dependencies:** None.

#### [MODIFY] [application.yaml (bds-api-gateway)](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-api-gateway/src/main/resources/application.yaml)
- **Action Required:** Add route mapping entries for `iam-service`:
  - Map path prefix `/api/auth/**`, `/api/account/**`, and `/users/**` to target URI `${IAM_SERVICE_URL:http://localhost:8086}`.
- **Dependencies:** Gateway routes routing table configuration.

#### [MODIFY] [application.yml (bds-core-macroservice)](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/resources/application.yml)
- **Action Required:** Refactor default property `iam.service-url` from `http://localhost:8081` to point to port `8086` (`${IAM_SERVICE_URL:http://localhost:8086}`) to resolve local port loop collision.
- **Dependencies:** Core macroservice configuration properties.

#### [MODIFY] [UserValidationAdapter.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/adapter/out/external/UserValidationAdapter.java)
- **Action Required:** Introduce connection and read timeout values (e.g. 2000ms) to the `RestTemplate` bean. Implement Resilience4j circuit breakers to prevent thread exhaustion during high load or IAM network outage.
- **Dependencies:** Resilience4j libraries.

---

## 2. Risk Assessment & Edge Cases

- **Service Latency & Programmatic Filtering:** During advanced filters (e.g. fetching paged sale agents), IAM queries its Postgres DB and then enriches metrics from the Appointment service over HTTP. If the user list is large, sequential HTTP calls will cause performance degradation.
  - *Mitigation:* The API in IAM must paginate results at the database layer (Postgres) *before* requesting metric details from the Appointment service, limiting external HTTP enrichment requests to the current page size (max 15 items).
- **Graceful Fallbacks in Checkout Flow:** The core macroservice validates users during checkout via synchronous HTTP calls to `/users/validate`. If IAM goes offline, checkout could be blocked.
  - *Mitigation:* The circuit breaker in `UserValidationAdapter` must fallback gracefully under service unavailability, using local verification caches or throwing custom resilient error codes that explain the situation rather than hanging.

---

## 3. Comprehensive Verification Plan

### Automated Tests (Unit & Integration)
- **Unit Testing Validation Constraints:**
  - Create test cases in `RegisterRequestTests` validating that request bodies with empty inputs or names exceeding 50 characters successfully fail validation and return the expected localized message.
- **Integration Testing User Validation:**
  - Start IAM service on `8086` and run mock integration tests verifying that `GET /users/validate` returns `{"active": true}` for active users and `{"active": false}` for nonexistent/deleted profiles.
- **Circuit Breaker Resiliency Verification:**
  - Stop the IAM service, and trigger a transaction in the core macroservice. Verify that the request times out gracefully, the circuit breaker opens, and `UserValidationAdapter` catches the exception without thread lock.

### Manual Verification
- **Gateway Route Checks:**
  - Direct HTTP calls to `http://localhost:8080/api/auth/login` and `http://localhost:8080/api/account/users` (through the Gateway on `8080`) to ensure they correctly route to IAM service on `8086` and return envelopes wrapped in `ApiResponse`.
- **Remember-Me Validation:**
  - Log in with `rememberMe: true` and verify that the expiration timestamp in the generated JWT token is extended appropriately compared to a standard login payload.
