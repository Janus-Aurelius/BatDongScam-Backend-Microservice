# Implementation Plan: API Gateway Gap Remediation

This document outlines the step-by-step technical plan to remediate the architectural, integration, and security gaps identified in the [api-gateway](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway) service, bringing it into full functional alignment with the legacy monolith and the rest of the microservices ecosystem.

---

## User Review Required

> [!IMPORTANT]
> **Token Payload Evolution (Breaking Change for Auth Tokens)**
> The legacy monolith's token provider did not encode user roles/authorities into the JWT token claims (it only encoded the subject/userId). To enable stateless Role-Based Access Control (RBAC) downstream without databases queries in downstream filters, we must update the token-generating auth endpoints in the monolith/macroservice to include a custom claim (e.g., `roles`). This is a breaking change for existing token payloads but is necessary for secure, stateless downstream communication.

> [!WARNING]
> **Bypass Mode in Downstream Filters**
> When the gateway or downstream services cannot reach the IAM service or discovery registry during test phases, downstream security configuration should support standard JWT/basic validation or fail-secure defaults. We will implement fail-secure defaults unless configured via profile profiles.

---

## Open Questions

> [!NOTE]
> **How are JWT tokens generated in the current ecosystem?**
> The macroservice (`bds-core-macroservice`) currently does not contain auth/login endpoints, meaning `/auth/login` is handled by the catch-all `/**` route routing to the legacy monolith, or is handled by an external IAM. We assume `/auth/login` runs in the catch-all or legacy service, which uses the legacy `JwtTokenProvider` to generate tokens. We will modify `JwtTokenProvider.java` in the legacy service to include a custom `roles` claim in generated JWT tokens.

---

## Proposed Changes

### Phase A: Contract & DTO Standardization

This phase aligns the gateway build setup with the rest of the platform and replaces ad-hoc error formats with the standardized common response library.

---

#### [MODIFY] [pom.xml (root)](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/pom.xml)
- Add `<module>api-gateway</module>` under `<modules>` to include the gateway in the consolidated Maven build structure.

#### [MODIFY] [pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/pom.xml)
- Change `<parent>` block to inherit from the parent POM `batdongsan-platform` (`com.se.bds`, version `0.0.1-SNAPSHOT`).
- Add the `bds-common` dependency to allow imports of shared DTOs:
  ```xml
  <dependency>
      <groupId>com.se.bds</groupId>
      <artifactId>bds-common</artifactId>
      <version>0.0.1-SNAPSHOT</version>
  </dependency>
  ```
- Remove hardcoded JJWT version `0.11.5` and use `${jjwt.version}` defined in parent POM to upgrade to `0.13.0`.
- Align Java compiler property to Java 21 (`<java.version>21</java.version>`).

#### [MODIFY] [JwtAuthenticationFilter.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/java/com/se100/bds/gateway/filter/JwtAuthenticationFilter.java)
- Refactor the `onError` helper method to use `com.se.bds.common.dto.ApiResponse` standard payload format:
  ```java
  private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
      ServerHttpResponse response = exchange.getResponse();
      response.setStatusCode(status);
      response.getHeaders().add("Content-Type", "application/json");

      ApiResponse<Void> apiResponse = ApiResponse.error(message);
      try {
          byte[] bytes = new ObjectMapper().writeValueAsBytes(apiResponse);
          return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
      } catch (JsonProcessingException e) {
          log.error("Failed to serialize API response", e);
          String fallback = "{\"success\":false,\"message\":\"" + message + "\",\"data\":null}";
          return response.writeWith(Mono.just(response.bufferFactory().wrap(fallback.getBytes())));
      }
  }
  ```

---

### Phase B: Business Logic Parity

This phase restores mock login capabilities for development/testing, expands permitted public paths, and refactors token parsing to emit granular validation messages matching legacy behaviors.

---

#### [MODIFY] [application.yaml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/resources/application.yaml)
- Add missing public routes to `app.public-paths`:
  - `/` (Root landing URL)
  - `/assets/**` (CSS, JS, and image assets)
  - `/ws/**` (WebSocket communication)
  - `/debug/**` (Developer debugging utilities)
  - `/bookings/payment/**` (Payment gateway callbacks/redirect handlers)

#### [MODIFY] [JwtTokenProvider.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/securities/JwtTokenProvider.java)
- Modify `generateTokenByUserId` to inject the user's role into the JWT claims (e.g. `claims.put("roles", role)`).

#### [MODIFY] [JwtAuthenticationFilter.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/java/com/se100/bds/gateway/filter/JwtAuthenticationFilter.java)
- Implement **Mock Token Bypass**:
  Check if the token is a mock token (e.g. `admin`, `agent1`, `customer1`). If yes and the active environment is a test/local profile, bypass signature checks and mutate the headers to inject preset Mock user IDs and roles:
  - `admin` -> userId: `admin-id`, roles: `ADMIN`
  - `agent1` -> userId: `agent-id`, roles: `SALEAGENT`
  - `customer1` -> userId: `customer-id`, roles: `CUSTOMER`
- Implement **Granular Token Validation Errors**:
  Refactor exception catching block during JWT parsing. Categorize validation errors and return descriptive messages:
  - `SignatureException` -> "Invalid JWT signature"
  - `MalformedJwtException` -> "Malformed JWT token"
  - `ExpiredJwtException` -> "Token expired"
  - `UnsupportedJwtException` -> "Unsupported JWT token"
  - `IllegalArgumentException` -> "JWT claims string is empty"

---

### Phase C: Integration & Communication

This phase wires the gateway to the new isolated microservices, resolves downstream port conflicts, replaces downstream Basic Auth with Header-based authentication, and configures resiliency rules.

---

#### [MODIFY] [application.properties](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/resources/application.properties)
- Change default server port to `8085` (`server.port=${SERVER_PORT:8085}`) to resolve port collision with the core macroservice on `8081`.

#### [MODIFY] [application.yaml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/resources/application.yaml)
- Add explicit Spring Cloud Gateway routes for missing microservices:
  - **Appointment Service**:
    ```yaml
    - id: appointment-service
      uri: ${APPOINTMENT_SERVICE_URL:http://localhost:8085}
      predicates:
        - Path=/api/appointment/**
    ```
  - **Moderation Service**:
    ```yaml
    - id: moderation-service
      uri: ${MODERATION_SERVICE_URL:http://localhost:8084}
      predicates:
        - Path=/api/violations/**
    ```
- Add global HTTP connection and response timeouts:
  ```yaml
  spring:
    cloud:
      gateway:
        httpclient:
          connect-timeout: 3000
          response-timeout: 10000
  ```
- Configure Resilience4j Circuit Breaker filters on service routes to prevent cascading failures.

#### [MODIFY] [pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/pom.xml)
- Add Eureka client and Resilience4j dependencies:
  ```xml
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
  </dependency>
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
  </dependency>
  ```

#### [MODIFY] [JwtAuthenticationFilter.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/java/com/se100/bds/gateway/filter/JwtAuthenticationFilter.java)
- Parse user roles from the JWT claims (e.g. `claims.get("roles")`) and propagate downstream by appending them to the mutated request header as `X-User-Roles` (a comma-separated string).

#### [NEW] [HeaderAuthenticationFilter.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/property/internal/application/config/HeaderAuthenticationFilter.java)
- Create a pre-authentication filter for the core macroservice that reads `X-User-Id` and `X-User-Roles` headers, maps roles to authorities, and populates `SecurityContextHolder`.

#### [MODIFY] [TestSecurityConfig.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/property/internal/application/config/TestSecurityConfig.java)
- Remove `.httpBasic(...)` basic auth setup.
- Register `HeaderAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.

#### [MODIFY] [HeaderUserIdAuthenticationFilter.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/moderation-service/src/main/java/microservices/moderationservice/security/HeaderUserIdAuthenticationFilter.java)
- Refactor the filter to extract the `X-User-Roles` header and populate the `UsernamePasswordAuthenticationToken` authorities using these values instead of passing an empty list.

#### [MODIFY] [JwtAuthenticationFilter.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/securities/JwtAuthenticationFilter.java)
- Rename or refactor this filter to act as a `HeaderAuthenticationFilter` that accepts `X-User-Id` and `X-User-Roles` headers, removing direct JWT token parsing to avoid redundant verification and database checks.

---

## Verification Plan

### Automated Tests
- **Unit Testing**:
  - Update `JwtAuthenticationFilterTest` in the gateway to verify:
    - Successful extraction of standard tokens and headers mutation (`X-User-Id`, `X-User-Roles`).
    - Granular error payload messages for malformed, expired, and signature-mismatched tokens.
    - Correct mapping and bypass logic for mock tokens (`admin`, `agent1`, etc.).
  - Add tests for `HeaderAuthenticationFilter` in core, appointment, and moderation services verifying:
    - Successful parsing of header-based auth and registration in Spring `SecurityContext`.
    - Proper enforcement of RBAC checks (e.g. admin-only paths reject requests with customer role).
- **Integration Testing**:
  - Run the Maven build command: `mvn clean test` from the root directory to confirm module compilation.
  - Send HTTP requests through the gateway routing ports and verify proxy forwarding:
    - `/api/appointment/..` forwards to port `8085`.
    - `/api/violations/..` forwards to port `8084`.
    - `/api/search/..` forwards to port `8082` without authentication (Public route).
  - Verify payment redirection `/bookings/payment/**` handles incoming webhooks without returning 401 unauthorized.

### Manual Verification
- Deploy the system locally (`mvn spring-boot:run` on each microservice or using Maven plugins).
- Use postman or curl to verify:
  - Valid token results in successful upstream propagation and correct role authorization checks.
  - Basic Authentication downstream is fully disabled (direct requests without headers should return `401 Unauthorized` instead of prompting for Basic Auth).
