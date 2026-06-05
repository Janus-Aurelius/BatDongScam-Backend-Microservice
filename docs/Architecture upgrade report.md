# Architecture Upgrade Report
**BatDongScam Platform Microservices Audit**  
**Role:** Principal Cloud Architect & Senior Security Engineer

This report documents architectural, data consistency, and resilience bottlenecks identified during a codebase audit of the microservices suite. Prescribed patterns are strictly based on codebase evidence.

---

## 1. Transactional Outbox (Data Management & Consistency)

*   **Location:**
    *   [`bds-financial-service`: PaymentServiceImpl.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/services/impl/PaymentServiceImpl.java#L123-L151) (in `updatePaymentStatus`) and [L155-L198](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/services/impl/PaymentServiceImpl.java#L155-L198) (in `handlePayOSWebhook`).
    *   [`bds-financial-service`: PayPalService.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/gateway/paypal/PayPalService.java#L240-L259) (in `handleWebhookEvent`).
    *   [`bds-moderation-service`: ViolationServiceImpl.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-moderation-service/src/main/java/microservices/moderationservice/moderation/service/impl/ViolationServiceImpl.java#L249-L281) (in `updateViolationReport`).

*   **The Symptom:**  
    **Dual Writes.** The codebase modifies database states (e.g., updating payment status or violation resolution notes) and immediately sends events (e.g., `PaymentCompletedEvent` or `ViolationPenaltyAppliedEvent`) to Kafka using `kafkaTemplate.send()` inside the same active `@Transactional` block.
    
    *Blast Radius:* High risk of distributed data inconsistency. If the database transaction fails to commit (e.g., serialization error, constraint violation) after sending the Kafka message, downstream services (like core or notification) will act on incorrect data. Conversely, if Kafka is down, the database transaction might commit (since the error is caught and logged locally in `publishPaymentCompleted`), but downstream components will never receive the event.

*   **The Prescribed Pattern:**  
    **Transactional Outbox**

*   **Refactoring Strategy:**
    1.  **Introduce Outbox Entity:** Create an `OutboxEvent` table in the service database to store events within the same SQL transaction.
    2.  **Modify Services to Write to Outbox:** Replace direct `kafkaTemplate.send()` invocations with saving an `OutboxEvent` record using the same transaction.
    3.  **Develop an Event Publisher:** Implement an outbox processor (polling scheduler or Debezium CDC engine) that runs out-of-band. It scans the `OutboxEvent` table for unsent messages, publishes them to Kafka, and marks them as sent (or deletes them) upon successful broker acknowledgment.

*   **Code Snippet:**

    *Outbox Event Schema:*
    ```java
    @Entity
    @Table(name = "outbox_events")
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public class OutboxEvent {
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;
        
        @Column(nullable = false)
        private String aggregateType;
        
        @Column(nullable = false)
        private String aggregateId;
        
        @Column(nullable = false)
        private String eventType;
        
        @Column(columnDefinition = "TEXT", nullable = false)
        private String payload;
        
        @Column(nullable = false)
        private boolean processed = false;
        
        @Column(nullable = false)
        private LocalDateTime createdAt = LocalDateTime.now();
    }
    ```

    *Transactional Write Example:*
    ```java
    @Transactional
    public PaymentResponse updatePaymentStatus(UUID paymentId, UpdatePaymentStatusRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        
        payment.setStatus(request.getStatus());
        Payment saved = paymentRepository.save(payment);
        
        if (saved.getStatus() == PaymentStatus.SUCCESS) {
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                saved.getId(), saved.getContractId(), saved.getPropertyId(),
                saved.getPaymentType().name(), saved.getAmount(), saved.getPayerId(), Instant.now()
            );
            
            // Persist the event to the outbox table in the same transaction
            outboxRepository.save(OutboxEvent.builder()
                .aggregateType("Payment")
                .aggregateId(saved.getId().toString())
                .eventType("payment-completed")
                .payload(objectMapper.writeValueAsString(event))
                .processed(false)
                .build()
            );
        }
        return mapToResponse(saved);
    }
    ```

    *Outbox Event Publisher (Polling-Based):*
    ```java
    @Component
    @RequiredArgsConstructor
    @Slf4j
    public class OutboxPublisher {
        private final OutboxRepository outboxRepository;
        private final KafkaTemplate<String, String> kafkaTemplate;
        
        @Scheduled(fixedDelay = 500) // Polls every 500ms
        @Transactional
        public void publishPendingEvents() {
            List<OutboxEvent> pending = outboxRepository.findByProcessedFalseOrderByCreatedAtAsc(Limit.of(50));
            for (OutboxEvent event : pending) {
                try {
                    kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload()).get();
                    event.setProcessed(true);
                    outboxRepository.save(event);
                } catch (Exception e) {
                    log.error("Failed to publish outbox event: {}", event.getId(), e);
                    break; // Halt batch processing to preserve message ordering
                }
            }
        }
    }
    ```

---

## 2. Unified Event Bus (Communication & API Design)

*   **Location:**
    *   [`bds-notification-service`: KafkaNotificationListener.java#L44](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-notification-service/src/main/java/com/se100/bds/notificationservice/listeners/KafkaNotificationListener.java#L44) (in `handleContractStatusChanged`).
    *   [`bds-appointment-service`: PropertyEventListener.java#L50](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-appointment-service/src/main/java/microservices/appointmentservice/services/property/listener/PropertyEventListener.java#L50) (in `onPropertyCreated`) and [L74](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-appointment-service/src/main/java/microservices/appointmentservice/services/property/listener/PropertyEventListener.java#L74) (in `onPropertyUpdated`).
    *   [`bds-search-service`: KafkaSearchEventListener.java#L28](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-search-service/src/main/java/com/se/bds/search/listeners/KafkaSearchEventListener.java#L28) (in `onPropertySearched`).

*   **The Symptom:**  
    **Sync Callbacks in Asynchronous Handlers (Thin Events).** The message broker (Kafka) is used, but the published messages are "thin" containing only IDs (like `propertyId` or `contractId`). The consumer services immediately execute a synchronous Feign client query back to the publisher (e.g., `coreServiceClient.getPropertyDetails(...)` or `coreServiceClient.getContractById(...)`) to retrieve structural properties, pricing details, or user associations.
    
    *Blast Radius:* Synchronous wait traps. If the primary `bds-core-macroservice` suffers an outage or response degradation, event consumers like `appointment-service` and `notification-service` will fail to process messages, filling logs with retries and generating cascading service failure across the system. It defeats the decouple guarantees of message-driven architectures.

*   **The Prescribed Pattern:**  
    **Unified Event Bus (Fat Events Enrichment)**

*   **Refactoring Strategy:**
    1.  **Enrich Event Payloads (Fat Events):** Modify the event publishers in `/bds-common` and `bds-core-macroservice` to output "Fat Events" that contain all necessary fields for downstream processing (e.g., inclusion of `customerId` in `ContractStatusChangedEvent`, or address, price, and dimensions in `PropertyCreatedIntegrationEvent`).
    2.  **Remove Feign Callbacks:** Modify downstream message consumer logic to rely purely on the deserialized event payload, removing Feign calls to `CoreServiceClient`.

*   **Code Snippet:**

    *Enriched (Fat) Event Payload Example:*
    ```java
    // Global fat event defined in /bds-common
    public record PropertyCreatedIntegrationEvent(
        UUID propertyId,
        String title,
        String description,
        String fullAddress,
        BigDecimal priceAmount,
        UUID ownerId,
        UUID assignedAgentId,
        String status,
        String transactionType
    ) {}
    ```

    *Decoupled Consumer implementation:*
    ```java
    @KafkaListener(topics = "property-created", groupId = "appointment-service-group")
    public void onPropertyCreated(String message) {
        log.info("[KAFKA] Processing fat property-created event...");
        try {
            PropertyCreatedIntegrationEvent event = objectMapper.readValue(message, PropertyCreatedIntegrationEvent.class);
            
            Property property = propertyRepository.findById(event.propertyId())
                    .orElseGet(() -> {
                        Property p = new Property();
                        p.setId(event.propertyId());
                        return p;
                    });
            
            // Map directly from event fields - NO sync REST calls needed
            property.setTitle(event.title());
            property.setDescription(event.description());
            property.setFullAddress(event.fullAddress());
            property.setPriceAmount(event.priceAmount());
            property.setOwnerId(event.ownerId());
            property.setAssignedAgentId(event.assignedAgentId());
            property.setStatus(Constants.PropertyStatusEnum.valueOf(event.status().toUpperCase()));
            property.setTransactionType(Constants.TransactionTypeEnum.valueOf(event.transactionType().toUpperCase()));
            
            propertyRepository.save(property);
            log.info("[KAFKA] Locally synchronized property ID={}", event.propertyId());
        } catch (Exception e) {
            log.error("[KAFKA] Failed to synchronize property data", e);
        }
    }
    ```

---

## 3. Materialized View (Data Management & Consistency)

*   **Location:**
    *   [`bds-moderation-service`: ViolationServiceImpl.java#L98-L111](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-moderation-service/src/main/java/microservices/moderationservice/moderation/service/impl/ViolationServiceImpl.java#L98-L111) (in `getAdminViolationItems`) and [L128-L141](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-moderation-service/src/main/java/microservices/moderationservice/moderation/service/impl/ViolationServiceImpl.java#L128-L141) (in `getMyViolationItems`).

*   **The Symptom:**  
    **N+1 REST Queries for View Aggregation.** When loading the violation list dashboard, the moderation service loops through each violation record and makes synchronous HTTP calls via `iamServiceClient.getUserDetails(...)` (for reporter/reported info) and `coreServiceClient.getPropertyDetails(...)` (for property headers) to construct the view layer.
    
    *Blast Radius:* Severe latency and network overhead. Querying a paginated list of 20 items generates up to 40 downstream HTTP calls. If any of those services have a high response latency, loading the admin dashboard will hang or fail, causing a massive load on IAM and Core databases.

*   **The Prescribed Pattern:**  
    **Materialized View (Local Read Model Replication)**

*   **Refactoring Strategy:**
    1.  **Define Local Replica Cache Tables:** Implement read-only entity schemas inside `bds-moderation-service` representing `UserReplica` and `PropertyReplica`.
    2.  **Construct Kafka Event Listeners:** Consume global `user-created`, `user-updated`, `property-created`, and `property-updated` events to asynchronously manage the replica records in the local database.
    3.  **Refactor Dashboard Query:** Perform a local database join (e.g., using JPA or SQL native queries) on these replica tables directly inside the moderation database, completely eliminating network lookups.

*   **Code Snippet:**

    *Local DB User Replica Scheme:*
    ```java
    @Entity
    @Table(name = "user_replicas")
    @Getter @Setter
    public class UserReplica {
        @Id
        private UUID id;
        private String fullName;
        private String avatarUrl;
        private String email;
        private String role;
    }
    ```

    *Refactored Join-Based Query Method:*
    ```java
    @Override
    @Transactional(readOnly = true)
    public Page<ViolationAdminItem> getAdminViolationItems(Pageable pageable, List<ViolationTypeEnum> types, List<ViolationStatusEnum> statuses) {
        // Run a local database join using JPA/QueryDSL 
        // e.g. violationReport left join UserReplica (Reporter) left join PropertyReplica (Reported)
        Page<ViolationReport> violations = violationRepository.findAllWithReplicas(types, statuses, pageable);
        
        return violations.map(violation -> {
            ViolationAdminItem item = violationMapper.toAdminItem(violation);
            // Replicas are loaded locally in the same query - no REST overhead!
            item.setReporterName(violation.getReporterReplica().getFullName());
            item.setReporterAvatarUrl(violation.getReporterReplica().getAvatarUrl());
            if (violation.getRelatedEntityType() == ViolationReportedTypeEnum.PROPERTY) {
                item.setReportedName(violation.getPropertyReplica().getTitle());
            } else {
                item.setReportedName(violation.getReportedUserReplica().getFullName());
            }
            return item;
        });
    }
    ```

---

## 4. Circuit Breaker (Resilience & Fault Tolerance)

*   **Location:**
    *   [`bds-moderation-service`: CoreServiceClient.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-moderation-service/src/main/java/microservices/moderationservice/client/CoreServiceClient.java)
    *   [`bds-moderation-service`: IamServiceClient.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-moderation-service/src/main/java/microservices/moderationservice/client/IamServiceClient.java)

*   **The Symptom:**  
    **Unprotected External Dependencies.** Synchronous REST validation calls are placed during request creations (`createViolationReport` / `updateViolationReport`) using Feign client bindings that contain zero fallback handlers, retries, or circuit breaker protection.
    
    *Blast Radius:* Cascading outage. If either `iam-service` or `bds-core-macroservice` goes offline or experiences high thread contention, request flows will stall. Moderation incoming connection threads will be held open waiting for timeouts, quickly saturating the moderation service container and causing server-wide crash.

*   **The Prescribed Pattern:**  
    **Circuit Breaker (Resilience4j & Fallback Behavior)**

*   **Refactoring Strategy:**
    1.  **Add Circuit Breaker Library:** Integrate `spring-cloud-starter-circuitbreaker-resilience4j` in the target project's POM.
    2.  **Define Fallbacks on Feign Client:** Configure fallback factories or specific fallback classes that return degraded responses (e.g., fallback maps, cache entries, or fail-safe default values) when calls fail.
    3.  **Establish Configurations:** Set precise sliding window sizes, slow call duration thresholds, and maximum thread execution durations in the configuration properties.

*   **Code Snippet:**

    *Resilient Feign client configuration:*
    ```java
    @FeignClient(
        name = "iam-service", 
        contextId = "iamServiceClient", 
        fallbackFactory = IamServiceClientFallbackFactory.class
    )
    public interface IamServiceClient {
        @GetMapping("/users/validate")
        Map<String, Object> validateUser(@RequestParam("userId") UUID userId, @RequestParam("role") String role);
        
        @GetMapping("/api/account/{userId}")
        Map<String, Object> getUserDetails(@PathVariable("userId") UUID userId);
    }
    ```

    *Fallback Implementation:*
    ```java
    @Component
    @Slf4j
    public class IamServiceClientFallbackFactory implements FallbackFactory<IamServiceClient> {
        @Override
        public IamServiceClient create(Throwable cause) {
            return new IamServiceClient() {
                @Override
                public Map<String, Object> validateUser(UUID userId, String role) {
                    log.error("IAM validation failed: {}. Assuming FAIL-SECURE and rejecting request.", cause.getMessage());
                    // Fail-secure: default to inactive/non-existent user
                    return Map.of("active", false, "error", "Service unavailable: fallback active");
                }

                @Override
                public Map<String, Object> getUserDetails(UUID userId) {
                    log.warn("Failed to retrieve user details for {}. Returning fallback mock.", userId);
                    return Map.of(
                        "success", true,
                        "data", Map.of("fullName", "System User (Temporarily Offline)", "role", "UNKNOWN")
                    );
                }
            };
        }
    }
    ```
