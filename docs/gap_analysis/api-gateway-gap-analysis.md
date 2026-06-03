# Post-Merge Gap Analysis Report: `api-gateway`

## Executive Summary
During this post-merge synchronization phase, the [api-gateway](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway) was analyzed. The gateway currently acts as a basic entry router and does not function as a fully secure, resilient API gateway within the BatDongSan microservices architecture.

Although the gateway successfully establishes a WebFlux-based Spring Cloud Gateway with simple stateless JWT validation—correctly mutating request headers to forward `X-User-Id` downstream in [JwtAuthenticationFilter.java:L58-L62](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/java/com/se100/bds/gateway/filter/JwtAuthenticationFilter.java#L58-L62)—the service suffers from several critical architectural, integration, and security gaps:

1. **Routing Blind Spots:** Crucial microservices such as `appointment-service` and `moderation-service` are completely omitted from the gateway configuration. As a result, client requests to `/appointment/**` and `/violations/**` are caught by the catch-all `/**` route and incorrectly sent to the core macroservice.
2. **Security Bypasses & Public Path Gaps:** Critical business routes like `/bookings/payment/**` (for payment callbacks/redirects) and `/ws/**` (WebSocket communication) are missing from the public paths configuration in [application.yaml:L50-L59](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/resources/application.yaml#L50-L59), resulting in their blockage. Additionally, legacy mock testing tokens (like `admin` and `agent1`) are unsupported, breaking development verification flows.
3. **Downstream Authentication Deficits:** Downstream services—including the [bds-core-macroservice](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice)—do not have pre-authenticated filters to process the gateway's `X-User-Id` header. For instance, [TestSecurityConfig.java:L27](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/property/internal/application/config/TestSecurityConfig.java#L27) expects Spring Security Basic Auth, which blocks any requests passing through the gateway. Furthermore, roles are not forwarded (e.g., in an `X-User-Roles` header), which disables Role-Based Access Control (RBAC) downstream.
4. **Contract Deviations:** Ad-hoc JSON error payloads are hardcoded in the filter [JwtAuthenticationFilter.java:L100](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/java/com/se100/bds/gateway/filter/JwtAuthenticationFilter.java#L100) instead of utilizing the standardized [com.se.bds.common.dto.ApiResponse](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common/src/main/java/com/se/bds/common/dto/ApiResponse.java) model.
5. **Cascading Failure & Port Risks:** The gateway lacks circuit breakers (Resilience4j), HTTP timeouts, and service discovery support. Furthermore, local default port configurations between `appointment-service` and `bds-core-macroservice` collide on port `8081`.

---

## Missing Features (Legacy vs. New)
Comparing the business logic inside the `/legacy` folder with [api-gateway](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway) reveals the following gaps:

* **Mock Token Authentication Support:**
  In legacy, [JwtAuthenticationFilter.java:L45-L61](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/securities/JwtAuthenticationFilter.java#L45-L61) supported mock tokens mapping to pre-configured test profiles (`admin`, `agent1`, `customer1`) bypasses token signing validation to fetch user details. The new gateway filter lacks this support and throws exception when meeting mock tokens, rendering local integration tests impossible.
* **Public Path Gaps:**
  Comparing the permitted routes in [WebSecurityConfig.java:L60-L72](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/configs/WebSecurityConfig.java#L60-L72) with [application.yaml:L50-L59](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/resources/application.yaml#L50-L59) reveals that several public routes were not migrated:
  * `/` (Root landing path)
  * `/assets/**` (Static assets)
  * `/ws/**` (WebSocket communication)
  * `/debug/**` (Developer debug tools)
  * `/bookings/payment/**` (Payment redirects and return URL handlers) — **Critical Business Gap:** The omission of this path blocks payment completion handlers, as callbacks from payment gateways (PayPal/PayOS) will be intercepted by the filter and return a `401 Unauthorized`.
* **Granular Token Validation Errors:**
  In the legacy monolith, [JwtTokenProvider.java:L109-L131](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/securities/JwtTokenProvider.java#L109-L131) checks and sets request attributes for specific errors (`invalid`, `unsupported`, `expired`, `illegal`). The new gateway filter only catches `ExpiredJwtException` and generic `JwtException`, sending raw, unhelpful messages and missing the granular troubleshooting logs available in legacy.

---

## Integration Gaps (Core Macroservice)
The gateway does not integrate cohesively with [bds-core-macroservice](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice) and other newly isolated microservices:

* **Downstream Security Blockage (Basic Auth vs. Header Auth):**
  The gateway filter validates JWTs and propagates `X-User-Id` downstream. However, [TestSecurityConfig.java:L27](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/property/internal/application/config/TestSecurityConfig.java#L27) configures the core macroservice to require HTTP Basic Authentication for all paths. Since the gateway does not forward basic auth credentials, Spring Security on the core service will systematically reject all incoming gateway requests.
* **Routing Blackout for `appointment-service` & `moderation-service`:**
  The gateway's route configuration in [application.yaml:L11-L28](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/resources/application.yaml#L11-L28) only lists routes for `search-service` and `notification-service`. There are no routes defined for `appointment-service` (`/appointment/**`) or `moderation-service` (`/violations/**`). They are intercepted by the catch-all `/**` route and incorrectly routed to `bds-core-macroservice` (port 8081).
* **Local Port Collision:**
  The default configuration for `appointment-service` in [application.properties:L5](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/resources/application.properties#L5) binds to port `8081`. However, the catch-all `core-service` route in the gateway [application.yaml:L26](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/resources/application.yaml#L26) also targets port `8081`. This causes port collisions during local run processes.
* **Lack of Dynamic Service Discovery (Eureka client):**
  The gateway uses hardcoded localhost URLs under `spring.cloud.gateway.webflux.routes` rather than using dynamic service discovery via Eureka. This creates tight deployment coupling.

---

## Contract Violations
The gateway operates in isolation and does not adhere to the shared standards of [bds-common](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common):

* **Standardized JSON Response Deficit:**
  When a JWT is expired or invalid, [JwtAuthenticationFilter.java:L100](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/java/com/se100/bds/gateway/filter/JwtAuthenticationFilter.java#L100) builds an ad-hoc JSON error payload:
  `{"statusCode":%d,"message":"%s"}`.
  This diverges from the standardized response model defined in [ApiResponse.java:L14-L17](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common/src/main/java/com/se/bds/common/dto/ApiResponse.java#L14-L17):
  `{"success":false,"message":"%s","data":null}`.
  This contract divergence causes frontend HTTP client parsing errors.
* **Exclusion from Consolidated Builds:**
  The gateway's [pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/pom.xml) does not reference the shared [bds-common](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common) module. Furthermore, it is not registered in the root parent [pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/pom.xml), isolating it from Maven clean/build commands.
* **Duplicated Config (JWT Secret Key):**
  The gateway configuration duplicates the JWT signing secret in [application.yaml:L49](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/resources/application.yaml#L49), rather than loading it from central configuration server or environment variables.

---

## Communication Anti-patterns

* **Missing Role & Authority Propagation:**
  The gateway filter validates the JWT, but only forwards the `userId` claim downstream via `X-User-Id`. The user roles/authorities (e.g. `ADMIN`, `CUSTOMER`, `SALEAGENT`, `PROPERTY_OWNER`) are completely stripped. Because downstream services do not have database connections to query user roles (as they did in legacy monolith's security filter), they are unable to enforce Role-Based Access Control (RBAC) via annotations like `@PreAuthorize("hasRole('ADMIN')")`.
* **Synchronous Cascading Risk (No Circuit Breaker):**
  The gateway lacks circuit breaker routing configurations (Resilience4j) or rate limiters. Under heavy client traffic, a single hanging downstream service will exhaust the gateway's connections and thread pools, causing system-wide downtime.
* **Missing HTTP Client Timeouts:**
  No connection or response timeouts are defined under `spring.cloud.gateway.httpclient` inside [application.yaml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/resources/application.yaml). The gateway is vulnerable to resource exhaustion from un-closed network sockets.

---

## Actionable Remediation Steps

### Phase 1: Build & Route Optimization (Priority: High)
- [ ] **Parent POM Alignment:** Update [api-gateway/pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/pom.xml) to inherit from the parent POM `batdongsan-platform` and register `api-gateway` in the modules list of the root [pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/pom.xml).
- [ ] **Import Shared Contracts:** Add the `bds-common` dependency to [api-gateway/pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/pom.xml) to enable standardized JSON error building.
- [ ] **Add Missing Gateway Routes:** Add explicit routing rules to [application.yaml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/resources/application.yaml) to map:
  * `/appointment/**` to `${APPOINTMENT_SERVICE_URL:http://localhost:8085}`
  * `/violations/**` to `${MODERATION_SERVICE_URL:http://localhost:8084}`
- [ ] **Resolve Local Port Collisions:** Refactor [application.properties](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/resources/application.properties) of the `appointment-service` to change its default port from `8081` to `8085` (`SERVER_PORT:8085`).
- [ ] **Restore Missing Public Routes:** Update `app.public-paths` in [application.yaml:L50-L59](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/resources/application.yaml#L50-L59) to include:
  * `/` (root landing URL)
  * `/assets/**` (images/CSS assets)
  * `/ws/**` (WebSocket connections)
  * `/debug/**` (Developer testing tools)
  * `/bookings/payment/**` (Payment redirect callbacks)

### Phase 2: Secure Downstream & Propagate Roles (Priority: High)
- [ ] **Propagate User Roles Claim:** Modify [JwtAuthenticationFilter.java:L58-L62](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/java/com/se100/bds/gateway/filter/JwtAuthenticationFilter.java#L58-L62) to parse user roles/authorities from the JWT claims and forward them as an `X-User-Roles` header.
- [ ] **Enable Downstream Header Security Filter:** Create and configure a pre-authentication filter (e.g. `HeaderAuthenticationFilter`) in the core service, moderation-service, and appointment-service that intercepts the `X-User-Id` and `X-User-Roles` headers to construct the Spring `SecurityContext`.
- [ ] **Disable Downstream Basic Auth:** Refactor [TestSecurityConfig.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/property/internal/application/config/TestSecurityConfig.java) to replace basic authentication with the pre-authentication filter.
- [ ] **Standardize JSON Response Format:** Refactor `onError` inside [JwtAuthenticationFilter.java:L95-L104](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/java/com/se100/bds/gateway/filter/JwtAuthenticationFilter.java#L95-L104) to serialize and return `ApiResponse.error(message)` from `bds-common` instead of custom JSON.

### Phase 3: Resiliency, Discovery & Testing (Priority: Medium)
- [ ] **Add Eureka Discovery Integration:** Add `spring-cloud-starter-netflix-eureka-client` to the gateway's pom.xml, and refactor routing URLs to use service discovery endpoints (e.g. `lb://search-service`) rather than hardcoded URLs.
- [ ] **Introduce Circuit Breakers & Timeouts:** 
  * Add the `spring-cloud-starter-circuitbreaker-reactor-resilience4j` dependency.
  * Define explicit connect and response timeouts under `spring.cloud.gateway.httpclient` in `application.yaml`.
  * Add circuit breaker filter configurations for unstable endpoints.
- [ ] **Reinstate Mock Tokens:** Restore mock token support inside [JwtAuthenticationFilter.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/java/com/se100/bds/gateway/filter/JwtAuthenticationFilter.java) under test profiles, allowing bypassing signature checks for mock credentials (`admin`, `agent1`).
