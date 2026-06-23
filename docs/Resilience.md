# Resilience Analysis & Architect Proposal

## 1. Synchronous REST Calls

### Component 1: `UserValidationAdapter` (Core Macroservice)
*   **Context:** Validates customer and sales agent roles/statuses synchronously using a raw `RestTemplate` call to the Identity and Access Management (`iam-service`) endpoint `/users/validate`.
*   **Vulnerability:** Under high traffic, if the `iam-service` undergoes a temporary outage or experiences elevated latency, Tomcat worker threads in the core macroservice will block indefinitely. This rapidly exhausts the thread pool, causing a cascading failure of the core service itself.
*   **Selected Pattern(s):** **1A. Circuit Breaker** + **1B. Retry with Exponential Backoff** + **1D. Timeouts** + **1C. Fallback Strategy**.
    *   *Rationale:* User validations are critical security checks; we cannot simply default to "valid" as a fallback. However, since the validation request is a GET query (idempotent), we can retry on transient network errors. If the circuit opens, we fail-fast immediately and return a clean "Service Unavailable" message to prevent thread blockage.
*   **Code Proposal:**

```java
// File: com/se/bds/core/transaction/internal/adapter/out/external/UserValidationAdapter.java

package com.se.bds.core.transaction.internal.adapter.out.external;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.message.validation.MSG12;
import com.se.bds.core.transaction.internal.application.port.out.UserValidationPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserValidationAdapter implements UserValidationPort {

    private final RestTemplate restTemplate;

    @Value("${iam.service-url:http://localhost:8084}")
    private String iamServiceUrl;

    @Override
    public void validateCustomer(UUID customerId) {
        validateUser(customerId, "CUSTOMER");
    }

    @Override
    public void validateAgent(UUID agentId) {
        validateUser(agentId, "SALESAGENT");
    }

    @CircuitBreaker(name = "iamUserValidation", fallbackMethod = "fallbackValidateUser")
    @Retry(name = "iamUserValidation")
    private void validateUser(UUID userId, String role) {
        if (userId == null) {
            return;
        }
        String url = iamServiceUrl + "/users/validate?userId={userId}&role={role}";
        Map<String, Object> uriVariables = Map.of(
                "userId", userId.toString(),
                "role", role
        );

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class, uriVariables);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Boolean active = (Boolean) response.getBody().get("active");
            if (active == null || !active) {
                throw new BusinessException(MSG12.CODE, "User is not active or role does not match");
            }
        } else {
            throw new BusinessException(MSG12.CODE, "User validation failed with status " + response.getStatusCode());
        }
    }

    // Fallback method executes when Circuit Breaker is open or max retries are exhausted
    private void fallbackValidateUser(UUID userId, String role, Throwable throwable) {
        log.error("Resilience fallback triggered for user validation (userId={}, role={}). Reason: {}", 
                userId, role, throwable.getMessage());
        throw new BusinessException(MSG12.CODE, "Identity validation service is currently unavailable. Please try again later.");
    }
}
```

*   **`application.yml` Configuration:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      iamUserValidation:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10000ms
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
  retry:
    instances:
      iamUserValidation:
        maxAttempts: 3
        waitDuration: 1000ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - org.springframework.web.client.ResourceAccessException
          - org.springframework.web.client.HttpServerErrorException
```

---

### Component 2: `CoreServiceClient` (Search Service)
*   **Context:** `bds-search-service` calls `bds-core-macroservice` synchronously via OpenFeign to fetch static locations (city/district/ward IDs) and query property metadata.
*   **Vulnerability:** If the core service drops offline, search indices cannot refresh, and searches fail. 
*   **Selected Pattern(s):** **1A. Circuit Breaker** + **1C. Fallback Strategy** + **1D. Timeouts**.
    *   *Rationale:* For metadata queries, providing a degraded default response (such as an empty list or static cached mappings) keeps the search engine operational.
*   **Code Proposal:**

```java
// File: com/se/bds/search/client/CoreServiceClient.java

package com.se.bds.search.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import java.util.UUID;
import java.util.Collections;

@FeignClient(name = "core-macroservice", contextId = "coreServiceClient", fallback = CoreServiceClient.CoreServiceClientFallback.class)
public interface CoreServiceClient {

    @GetMapping("/api/internal/locations/cities/ids")
    List<UUID> getAllCityIds();

    @GetMapping("/api/internal/locations/districts/ids")
    List<UUID> getAllDistrictIds();

    @GetMapping("/api/internal/locations/wards/ids")
    List<UUID> getAllWardIds();

    @GetMapping("/api/internal/property-types/ids")
    List<UUID> getAllAvailablePropertyTypeIds();

    @GetMapping("/api/internal/properties/{propertyId}/location-info")
    PropertyLocationInfo getPropertyLocationInfo(@PathVariable("propertyId") UUID propertyId);

    record PropertyLocationInfo(
            UUID propertyId,
            UUID cityId,
            UUID districtId,
            UUID wardId,
            UUID propertyTypeId
    ) {}

    // Graceful degradation fallback class
    org.springframework.stereotype.Component
    class CoreServiceClientFallback implements CoreServiceClient {
        @Override
        public List<UUID> getAllCityIds() { return Collections.emptyList(); }

        @Override
        public List<UUID> getAllDistrictIds() { return Collections.emptyList(); }

        @Override
        public List<UUID> getAllWardIds() { return Collections.emptyList(); }

        @Override
        public List<UUID> getAllAvailablePropertyTypeIds() { return Collections.emptyList(); }

        @Override
        public PropertyLocationInfo getPropertyLocationInfo(UUID propertyId) {
            return new PropertyLocationInfo(propertyId, null, null, null, null);
        }
    }
}
```

*   **`application.yml` Configuration:**
```yaml
spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true
      client:
        config:
          core-macroservice:
            connectTimeout: 2000
            readTimeout: 2000

resilience4j:
  circuitbreaker:
    instances:
      coreServiceClient:
        slidingWindowSize: 20
        failureRateThreshold: 60
        waitDurationInOpenState: 15s
```

---

### Component 3: `LocationClient` & `RankingClient` (IAM Service)
*   **Context:** `bds-iam-service` synchronously fetches locations from the core service and sales rankings from `bds-appointment-service`.
*   **Vulnerability:** A failure in either external microservice blocks user profile rendering and location selection in the IAM client.
*   **Selected Pattern(s):** **1A. Circuit Breaker** + **1C. Fallback**.
    *   *Rationale:* If user rankings fail, we degrade the tier representation to a default rank ("BRONZE") and cache location arrays locally to ensure the user profile works.
*   **Code Proposal:**

```java
// File: com/se361/iam_service/client/RankingClient.java

package com.se361.iam_service.client;

import com.se.bds.common.dto.ApiResponse;
import com.se.bds.common.enums.RoleEnum;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.UUID;

@FeignClient(name = "appointment-service", path = "/api/rankings")
public interface RankingClient {

    @GetMapping("/current-tier")
    @CircuitBreaker(name = "rankingService", fallbackMethod = "fallbackCurrentTier")
    ApiResponse<String> getCurrentTier(
            @RequestParam("userId") UUID userId,
            @RequestParam("role") RoleEnum role
    );

    default ApiResponse<String> fallbackCurrentTier(UUID userId, RoleEnum role, Throwable throwable) {
        return new ApiResponse<>(true, "Fallback: Default Rank Applied", "BRONZE");
    }
}
```

---

### Component 4: `JwtAuthenticationFilter` (API Gateway)
*   **Context:** Gateway global filter retrieving the JSON Web Key Set (JWKS) from `/api/auth/jwks` on `iam-service` reactively to authenticate all incoming platform requests.
*   **Vulnerability:** If the identity service starts slowly or suffers transient downtime, the gateway cannot read public signature keys and immediately rejects every token verification.
*   **Selected Pattern(s):** **1B. Reactive Retry** + **1D. Timeouts** + **1C. Fallback (Key Caching)**.
    *   *Rationale:* Keys don't rotate frequently. If fetching from IAM fails due to timeout or server failure, reusing the last valid cached signature keys represents a safe and robust fallback.
*   **Code Proposal:**

```java
// File: com/se100/bds/gateway/filter/JwtAuthenticationFilter.java

private Mono<PublicKey> fetchPublicKeyFromIam() {
    String jwksUri = appProperties.getJwksUri();
    if (!StringUtils.hasText(jwksUri)) {
        jwksUri = "http://iam-service:8084/api/auth/jwks";
    }
    log.info("Fetching JWKS from IAM service: {}", jwksUri);
    return webClient.get()
            .uri(jwksUri)
            .retrieve()
            .bodyToMono(Map.class)
            .map(this::extractPublicKeyFromJwks)
            .timeout(java.time.Duration.ofSeconds(3))
            // Retry transient exceptions 3 times with a 500ms backoff delay
            .retryWhen(reactor.util.retry.Retry.backoff(3, java.time.Duration.ofMillis(500))
                    .filter(throwable -> throwable instanceof java.io.IOException || throwable instanceof java.util.concurrent.TimeoutException)
            )
            .onErrorResume(e -> {
                if (cachedPublicKey != null) {
                    log.warn("Failed to fetch fresh JWKS. Falling back to cached public key. Error: {}", e.getMessage());
                    return Mono.just(cachedPublicKey);
                }
                return Mono.error(new RuntimeException("Failed to fetch initial JWKS and no cached key is available: " + e.getMessage(), e));
            });
}
```

---

### Component 5: `StripeService` (Financial Service)
*   **Context:** Generates online payments and initiates agent commission payouts using direct HTTP requests inside the Stripe Java SDK.
*   **Vulnerability:** Third-party gateway outages or DNS hiccups cause Stripe SDK invocations (`Session.create()`, `Payout.create()`) to hang, tying up connection-pool threads.
*   **Selected Pattern(s):** **1A. Circuit Breaker** + **1B. Retry with Idempotency Keys** + **1D. Timeouts**.
    *   *Rationale:* Since payments and payouts are critical business operations, retries are highly effective. To prevent double charges during a connection retry, Stripe's native `Idempotency-Key` (which is already implemented) ensures safety.
*   **Code Proposal:**

```java
// File: com/se361/financial_service/gateway/stripe/StripeService.java

@Override
@CircuitBreaker(name = "stripeGateway")
@Retry(name = "stripeGateway")
public CreatePaymentSessionResponse createPaymentSession(CreatePaymentSessionRequest request, String idempotencyKey) {
    validatePaymentRequest(request);
    // Stripe SDK setup ...
    try {
        Session session = Session.create(paramsBuilder.build(), getOptions(idempotencyKey));
        log.info("Created Stripe checkout session: {}", session.getId());
        return mapPaymentSession(session, request);
    } catch (Exception e) {
        log.error("Failed to create Stripe checkout session", e);
        throw new RuntimeException("Stripe error: " + e.getMessage(), e);
    }
}
```

*   **`application.yml` Configuration:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      stripeGateway:
        slidingWindowSize: 5
        failureRateThreshold: 40
        waitDurationInOpenState: 30s
  retry:
    instances:
      stripeGateway:
        maxAttempts: 3
        waitDuration: 1500ms
        enableExponentialBackoff: true
```

---

## 2. Asynchronous Kafka Consumers

### Component 6: Asynchronous Kafka Event Consumers (All Subservices)
*   **Context:** Services consume events from topics (e.g. `violation-penalty-applied`, `payment-succeeded`, `property-created`, `contract-status-changed`).
*   **Vulnerability:** Existing consumers catch `Exception` block-wide and log it. By swallow-logging, Spring Kafka commits offsets for failed events. Unhandled data deserialization errors, database connection dropouts, or application exceptions result in **silent data loss** and database desynchronization.
*   **Selected Pattern(s):** **2A. Dead Letter Queue (DLQ)** + **2B. Non-Blocking Retries**.
    *   *Rationale:* Blocking retries on a single partition stall execution for all subsequent events. Applying non-blocking retries with a DLT publishing recoverer routes poison-pill payloads to a dead-letter queue (e.g., `payment-succeeded-dlt`), preserving throughput on critical streams.
*   **Code Proposal:**

```java
// File: com/se/bds/core/shared/config/KafkaResilienceConfig.java

package com.se.bds.core.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@Slf4j
public class KafkaResilienceConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template) {
        // Publish to DLT after maximum retries are reached
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> {
                    log.error("Event failed processing permanently. Routing to DLT topic: {}-dlt. Error: {}", 
                            record.topic(), ex.getMessage());
                    return new org.apache.kafka.common.TopicPartition(record.topic() + "-dlt", record.partition());
                });

        // Set up Exponential Backoff for retries: 1s initial delay, multiplier 2.0, max 3 attempts
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(3);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        // Exclude poison pills (non-transient errors like bad formatting) from retries
        errorHandler.addNotRetryableExceptions(
                com.fasterxml.jackson.core.JsonProcessingException.class,
                IllegalArgumentException.class
        );
        
        return errorHandler;
    }
}
```

To remove the catch-all offset commits, individual event consumers (like `PaymentSucceededConsumer`) must bubble exceptions up:
```java
// File: com/se/bds/core/transaction/internal/adapter/in/messaging/PaymentSucceededConsumer.java

@KafkaListener(topics = "payment-succeeded", groupId = "core-macroservice-payment")
public void consumePaymentSucceeded(@Payload String payload) throws Exception {
    log.info("[KAFKA] Received payment-succeeded event, payload={}", payload);
    // Bubble deserialization or business execution exceptions up to trigger the Kafka CommonErrorHandler
    PaymentCompletedEvent event = objectMapper.readValue(payload, PaymentCompletedEvent.class);
    boolean result = paymentWebhookUseCase.processPaymentCompleted(event);
    if (!result) {
        throw new RuntimeException("Business execution failed for payment-completed event");
    }
    log.info("[KAFKA] Payment-succeeded event processed successfully");
}
```

---

## 3. Database & Cache Integrations

### Component 7: `PropertyServiceImpl` Cache (Core Macroservice)
*   **Context:** `getPropertyDetail` caches data in Redis using `@Cacheable(value = "propertyDetails", key = "#propertyId")`.
*   **Vulnerability:** The write operations (`updateProperty`, `updatePropertyStatus`, and `deleteProperty`) execute without evicting or updating the cache. This results in stale or deleted records served indefinitely from Redis.
*   **Selected Pattern(s):** **Cache Eviction Pattern**.
    *   *Rationale:* High consistency for real estate listings prevents users from viewing stale records or trying to transact on deleted properties.
*   **Code Proposal:**

```java
// File: com/se/bds/core/property/internal/application/service/PropertyServiceImpl.java

@Override
@Transactional
@CacheEvict(value = "propertyDetails", key = "#propertyId")
public Property updateProperty(UUID propertyId, UpdatePropertyCommand command, MultipartFile[] mediaFiles, MultipartFile[] documents) {
    // ... logic to update property details
}

@Override
@Transactional
@CacheEvict(value = "propertyDetails", key = "#propertyId")
public Property updatePropertyStatus(UUID propertyId, UpdatePropertyStatusCommand command) {
    // ... logic to update listing status (AVAILABLE, RENTED, etc.)
}

@Override
@Transactional
@CacheEvict(value = "propertyDetails", key = "#propertyId")
public void deleteProperty(UUID propertyId) {
    // ... logic to soft-delete listing
}
```

*   **`application.yml` Cache Expiry Safeguard:**
```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      time-to-live: 3600000 # 1 hour default expiration to prevent permanent cache leaks
```

---

### Summary of Next Steps
1. **Apply configurations:** Merge the Resilience4j configurations into the respective `application.yml` of `bds-core-macroservice`, `bds-search-service`, `bds-iam-service`, `bds-appointment-service`, and `bds-financial-service`.
2. **Refactor consumers:** Remove try-catch exception swallows in the Kafka consumers and declare the `KafkaResilienceConfig` bean.
3. **Cache Eviction:** Apply `@CacheEvict` annotations inside `PropertyServiceImpl` to resolve cache desynchronization.