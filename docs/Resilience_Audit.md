## 1. Executive Summary & Over-Engineering Audit

The initial resilience proposal includes standard patterns, but it exhibits several critical instances of **over-engineering** and **flawed assumptions** that could lead to data loss or business disruptions. 

As a Lean Architect, the priority is to avoid adding complex runtime frameworks (like Resilience4j AOP and state trackers) where simple JVM features (try-catch, native timeouts) or framework features (Spring Kafka defaults) suffice. Furthermore, some proposed fallbacks are actively dangerous.

### Dropped/Downgraded Patterns

1.  **Component 2 (`CoreServiceClient` in Search Service):**
    *   **Action:** **DROP** Circuit Breaker and Fallback.
    *   **Justification:** This Feign client is only executed within `PropertyStatisticsReportScheduler`, a background task scheduled **once a month** at midnight (`0 0 0 1 * ?`). If the core service is down, returning an empty list fallback to `updateLocationMap` will wipe all historical search/favorite location maps from the DB. Instead, let the scheduler fail naturally and log the error. A simple HTTP timeout is sufficient.
2.  **Component 5 (`StripeService` in Financial Service):**
    *   **Action:** **DROP** Resilience4j `@CircuitBreaker` and `@Retry`.
    *   **Justification:** The Stripe Java SDK has its own robust, built-in network retries and timeout settings. Introducing Resilience4j on top creates duplicate retry loops (multiplied requests) and unnecessary complexity. More importantly, a Circuit Breaker on payments is a business anti-pattern; if Stripe undergoes a transient hiccup, opening the circuit will block *all* checkout attempts, directly stopping revenue generation. 
3.  **Component 3 (`RankingClient.getCurrentTier` in IAM Service):**
    *   **Action:** **DROP** Resilience4j `@CircuitBreaker` and Fallback.
    *   **Justification:** The method `UserServiceImpl.toResponse` already wraps the Feign call to `RankingClient` in a try-catch block that defaults the tier to `"BRONZE"`. Adding a circuit breaker is redundant, adds AOP class generation overhead, and increases state-tracking complexity for a call that is already safely handled by standard Java.
4.  **Component 1 (`UserValidationAdapter` in Core Service):**
    *   **Action:** **DOWNGRADE** Retry count.
    *   **Justification:** Retrying synchronous requests on a user-facing thread pool under load can quickly exhaust Tomcat threads. The Circuit Breaker and Timeout must remain (to fail-fast and prevent resource starvation), but Retries should be set to 0 or 1.

---

## 2. Prioritized Implementation Roadmap

The table below outlines the patterns that survived the audit, ranked from highest to lowest priority based on **Blast Radius**, **Traffic**, and **ROI**.

| Rank | Component / Code Location | Recommended Pattern | Priority | Justification (Impact vs. Effort) |
| :--- | :------------------------ | :------------------ | :------- | :-------------------------------- |
| 1    | [Kafka Event Consumers](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/adapter/in/messaging/PaymentSucceededConsumer.java) | Error Handler + DLT (DLQ) | **Critical** | Currently, consumers swallow all exceptions and commit offsets, leading to silent, permanent data loss. Setting up a DLT prevents desynchronization of critical db states. Low implementation effort. |
| 2    | [PropertyServiceImpl.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/property/internal/application/service/PropertyServiceImpl.java#L144-L278) | Cache Eviction (`@CacheEvict`) | **High** | Fixes an active functional bug. Writes (`updateProperty`, `updatePropertyStatus`, `deleteProperty`) currently do not evict cached listing details, serving stale/deleted data from Redis for up to 1 hour. Extremely low effort. |
| 3    | [JwtAuthenticationFilter.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-api-gateway/src/main/java/com/se100/bds/gateway/filter/JwtAuthenticationFilter.java#L195-L208) | WebClient Timeout + Fallback to Cache | **High** | Prevents cascading API Gateway failures when `iam-service` experiences transient latency. Uses standard WebFlux operators (`timeout`, `onErrorResume`) without external frameworks. Low effort. |
| 4    | [UserValidationAdapter.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/adapter/out/external/UserValidationAdapter.java#L39-L77) | Timeout + Circuit Breaker (No Retries) | **Medium** | Protects Tomcat thread pool from cascading exhaustion if `iam-service` blocks under load. High traffic path, high blast radius. Medium configuration effort. |
| 5    | [StripeService.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/gateway/stripe/StripeService.java#L30-L34) | Native SDK Timeout & Retry Config | **Medium** | Configures native Stripe SDK settings (`Stripe.setMaxNetworkRetries(3)`) to handle transient network glitches cleanly without adding third-party resilience wrappers. Very low effort. |

---

## 3. Implementation Phasing

### Phase 1 (Immediate Fixes)
These changes target critical data consistency issues and quick-win thread protections.
*   **Implement Spring Kafka DLT (CommonErrorHandler):** Declare `KafkaResilienceConfig` inside `bds-core-macroservice` and rethrow caught exceptions in individual consumers (e.g. `PaymentSucceededConsumer`) instead of swallow-logging.
*   **Add Cache Eviction on Property Writes:** Apply `@CacheEvict(value = "propertyDetails", key = "#propertyId")` to write operations in `PropertyServiceImpl.java`.
*   **Strengthen API Gateway JWKS Retrieval:** Modify `fetchPublicKeyFromIam` in `JwtAuthenticationFilter.java` to use a 3-second timeout and fallback to `cachedPublicKey` if the identity service is unreachable.

### Phase 2 (Strategic Improvements)
These improvements should be scheduled as traffic scales or during structural refactoring.
*   **Configure Tomcat Thread Safeties (Core & IAM):** Configure short connection and read timeouts on `RestTemplate` used inside `UserValidationAdapter`, and configure the Resilience4j Circuit Breaker without retries.
*   **SDK-level Stripe Tuning:** Configure `Stripe.setMaxNetworkRetries(3)` globally in `@PostConstruct` of `StripeService.java`.
*   **Architectural Change for N+1 Feign Calls:** Refactor the IAM filtering logic (`getAllSaleAgentItemsWithFilters` and `getAllCustomerItemsWithFilters`) to query Performance Tiers and Location names in batch rather than executing synchronous REST calls inside a loop.