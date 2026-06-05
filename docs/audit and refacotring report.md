# Microservices Architecture & Security Audit Report

This document compiles the findings from the security audit and the maintainability/observability review of the BatDongScam microservices architecture, concluding with a step-by-step refactoring plan.

## 1. Security Audit Findings

| Vulnerability | Location | Severity | Recommended Fix |
| :--- | :--- | :--- | :--- |
| **Exposed Unauthenticated Payout & Payment APIs** | `SecurityConfig.java` (Financial Service), `BdsSecurityAutoConfiguration.java` (Security Starter) | **[P0] Critical / Blocker** | Restrict `/api/internal/**` endpoints. Do not permit anonymous access; instead, restrict them using a shared API key header, mutual TLS (mTLS), or configure Docker Compose to bind microservice ports strictly to `127.0.0.1` (no public port exposure) and use internal verification. |
| **Authentication Bypass via Unauthenticated Gateway Headers** | `HeaderAuthenticationFilter.java` (Security Starter) | **[P0] Critical / Blocker** | 1. Remove raw host port exposure in `docker-compose.yml` (e.g. change `8085:8085` to `127.0.0.1:8085:8085` or remove host bindings entirely).<br>2. Sign the headers forwarded by the gateway using a cryptographic signature (HMAC) or JWT that backend microservices verify. |
| **Production Authentication Bypass via Default Mock Tokens** | `JwtAuthenticationFilter.java` (API Gateway) | **[P0] Critical / Blocker** | Ensure mock tokens are strictly disabled unless the active profile is explicitly set to `local` or `test`. Do not return `true` when the profile list is empty. |
| **Unsecured Eureka Service Registry** | `pom.xml` & `application.yml` (Eureka Server) | **[P0] Critical / Blocker** | Add `spring-boot-starter-security` to the Eureka server, configure basic authentication for dashboard and registration endpoints, and configure microservices to authenticate using credentials (e.g. `http://user:password@eureka:8761/eureka/`). |
| **Security Bypass via Fail-Open Fallback in Identity Validation** | `UserValidationAdapter.java` (Core Macroservice) | **[P0] Critical / Blocker** | The adapter falls back to permissive mode if IAM is down. Refactor exception handling to fail-closed, log errors securely, and throw a structured business exception denying access. |
| **Hardcoded Cryptographic Keys & Secrets** | `application.yaml` (API Gateway, Financial, Core, IAM) | **[P1] Important** | Remove hardcoded fallbacks for cryptographic keys (e.g., `APP_SECRET`, `ENCRYPTION_KEY`). Force the application to fail to start if the environment variables are missing. |
| **Missing Request Rate Limiting** | `application.yaml` (API Gateway) | **[P1] Important** | Configure `RequestRateLimiter` using Redis in the gateway to protect against brute-force login attacks and denial-of-service attempts. |
| **Lack of Validation on User-Controlled Query Parameters** | `SearchController.java` (Search Service) | **[P2] Enhancement** | Add validation annotations like `@Min(1)` and `@Max(100)` for pagination limits and valid ranges for year/month parameters, and annotate the controller with `@Validated`. |

---

## 2. Observability & Infrastructure Gaps

### Missing Healthchecks and Container Boot Sequencing
*   **Gap:** Although Spring Boot Actuator is present in the gateway's `pom.xml`, the services do not expose structured liveness/readiness probes (only default `/health` checks are exposed). Furthermore, the `docker-compose.yml` environment defines sequencing dependencies using `condition: service_started`.
*   **Impact:** A container is marked "started" the instant the OS process spawns, but a Spring Boot application takes 10–20 seconds to boot the JVM and load context. Consequently, dependent services start communicating with services that are not yet listening, resulting in transient connection errors, routing failures, and failure to register with the Eureka server upon startup.

### Zero Test Coverage in the API Gateway
*   **Gap:** The `bds-api-gateway` module completely lacks a `src/test` directory. Important security logic (CORS, public paths, and mock profile token authentication) is entirely untested.
*   **Impact:** Any changes to routing or the security filter can lead to silent regressions (e.g., exposing private paths publicly or breaking login token verification).

### Monolithic Creep in Core Macroservice
*   **Gap:** The `bds-core-macroservice` handles:
    1. **Property Domain:** Listings, location hierarchies, and document verifications.
    2. **Contract Domain:** Purchase, rental, deposit, and escrow state machines.
    3. **Payment Adapter Logic:** Delegating internal payments.
*   **Impact:** Under load, property search requests can exhaust resources needed for transaction processing, introducing a shared failure domain. It violates the single-responsibility principle for microservices.

### Shared Symmetric Key Coupling
*   **Gap:** The API Gateway and IAM services share a symmetric secret key (`app.secret`) to generate and parse JWT signatures.
*   **Impact:** High configuration coupling. If the secret key is rotated, the Gateway and IAM services must be deployed simultaneously. A compromise of either service compromises token generation for the entire system.

### Unhandled Exceptions in Gateway Filter
*   **Gap:** In `JwtAuthenticationFilter`, if configuration properties are missing or malformed (e.g., `appProperties.getPublicPaths()` or `appProperties.getSecret()` resolves to `null`), the filter throws a `NullPointerException` before the try-catch block executes.
*   **Impact:** A raw NPE inside a WebFlux filter bubbles up as an unhandled HTTP 500 error, potentially leaking server internals to the client.

---

## 3. Step-by-Step Refactoring Plan

### Phase 1: Observability & Boot Ordering (Resilience)
1. **Enable Liveness & Readiness Probes in Gateway Configuration:**
   Configure Spring Boot Actuator in `application.yaml` to expose probe endpoints:
   ```yaml
   management:
     endpoint:
       health:
         probes:
           enabled: true
     endpoints:
       web:
         exposure:
           include: health, info, prometheus
   ```
2. **Implement Container Healthchecks in Docker Compose:**
   Update `docker-compose.yml` to add physical health checks to dependencies:
   ```yaml
   eureka-server:
     # ...
     healthcheck:
       test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
       interval: 10s
       timeout: 5s
       retries: 5
       start_period: 15s

   api-gateway:
     # ...
     depends_on:
       eureka-server:
         condition: service_healthy
     healthcheck:
       test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health/readiness"]
       interval: 10s
       timeout: 3s
       retries: 3
   ```

### Phase 2: Code Hardening & Exception Management
1. **Harden `JwtAuthenticationFilter.java`:**
   Add defensive null checks at the beginning of the filter chain to prevent uncaught `NullPointerExceptions` if configurations are missing:
   ```java
   @Override
   public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
       ServerHttpRequest request = exchange.getRequest();
       String path = request.getURI().getPath();

       // Guard against missing properties configuration
       if (appProperties == null || appProperties.getPublicPaths() == null || appProperties.getSecret() == null) {
           log.error("Gateway JWT Configuration missing/invalid: appProperties or secrets is null.");
           return onError(exchange, "Internal server configuration error", HttpStatus.INTERNAL_SERVER_ERROR);
       }

       if (isPublicPath(path)) {
           return chain.filter(exchange);
       }
       // ... (rest of the logic inside the try-catch block)
   }
   ```
2. **Harden `UserValidationAdapter.java`:**
   Refactor the exception handling in the identity validation fallback to fail-closed, throwing structured business exceptions instead of allowing permissive bypass.

### Phase 3: Decoupling Token Validation (Asymmetric Keys)
1. **Transition to RS256 Key Signing:**
   Migrate `bds-iam-service` from symmetric HS256 to asymmetric RS256 key pairs.
2. **Expose JWKS Endpoint:**
   Have `bds-iam-service` expose a JSON Web Key Set (JWKS) endpoint (e.g. `/oauth2/jwks`) containing the public key.
3. **Configure API Gateway to query JWKS:**
   Update the gateway to fetch public keys from the JWKS endpoint dynamically instead of parsing them from a shared secret configuration.

### Phase 4: Decomposition of the Macroservice Creep
1. **Split `bds-core-macroservice` Database Schemas:**
   Isolate property schemas (entities like `Property`, `Location`) from contract/escrow schemas (`RentalContract`, `DepositContract`) into separate database schemas.
2. **Decompose Service into Microservices:**
   *   **`bds-property-service`**: Expose property management features on a dedicated port.
   *   **`bds-contract-service`**: Manage agreement state machines and payment handshakes.
3. **Migrate Communication:**
   Replace the internal direct imports in the macroservice packages with Feign client requests between the newly segregated services.

### Phase 5: Establish Test Infrastructures
1. **Create Gateway WebFlux Unit Test Suite:**
   Add `src/test/java` in `bds-api-gateway` and verify authentication filters using Spring’s `WebTestClient`:
   ```java
   package com.se100.bds.gateway;

   import org.junit.jupiter.api.Test;
   import org.springframework.beans.factory.annotation.Autowired;
   import org.springframework.boot.test.context.SpringBootTest;
   import org.springframework.test.web.reactive.server.WebTestClient;

   @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
   class ApiGatewayRoutingTest {

       @Autowired
       private WebTestClient webTestClient;

       @Test
       void requestWithoutTokenOnProtectedRoute_Returns4101Unauthorized() {
           webTestClient.get()
                   .uri("/api/notifications")
                   .exchange()
                   .expectStatus().isUnauthorized();
       }
   }
   ```
