# Architecture Upgrade Report: Microservices Refactoring Roadmap
**BatDongScam Platform Architecture Audit & Migration Plan**  
**Role:** Principal Cloud Architect & Senior Security Engineer  

---

## Executive Summary
This document serves as the **guiding technical specification** to resolve critical data consistency, communication, performance, and resilience bottlenecks in the BatDongScam microservices platform. 

To ensure execution velocity and clear ownership, the refactoring roadmap is divided into **3 distinct tracks for 3 developers**. Each track is fully detailed with exact code locations, symptoms, step-by-step refactoring plans, copy-pasteable configuration/code boilerplates, and a verification checklist.

---

# Team Workload Distribution Matrix

| Track / Developer | Core Pattern & Responsibility | Focus Services | Priority |
| :--- | :--- | :--- | :--- |
| **Track 1: Developer A** | Data Consistency: **Transactional Outbox** | `bds-financial-service`, `bds-moderation-service` | **[P0] Critical** |
| **Track 2: Developer B** | Decoupling: **Unified Event Bus (Fat Events)** | `bds-common`, `bds-core-macroservice`, `bds-notification-service`, `bds-appointment-service`, `bds-search-service` | **[P1] Important** |
| **Track 3: Developer C** | Performance & Resilience: **Materialized Views & Circuit Breakers** | `bds-moderation-service` | **[P1] Important** |

---

# Detailed Implementation Specifications

---

## Track 1: Developer A — Data Consistency & Reliability (Transactional Outbox)

### 1. Diagnosis & Locations
*   **Core Symptom:** Dual writes inside database transaction blocks. The application modifies DB states and sends Kafka messages directly in the same business method. If the DB commits but Kafka is down, or if Kafka succeeds but the DB rolls back, the distributed state becomes corrupted.
*   **Exact Code Locations:**
    1.  [`bds-financial-service`: PaymentServiceImpl.java#L123-L151](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/services/impl/PaymentServiceImpl.java#L123-L151) (in `updatePaymentStatus`) and [L155-L198](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/services/impl/PaymentServiceImpl.java#L155-L198) (in `handlePayOSWebhook`).
    2.  [`bds-financial-service`: PayPalService.java#L240-L259](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/gateway/paypal/PayPalService.java#L240-L259) (in `handleWebhookEvent`).
    3.  [`bds-moderation-service`: ViolationServiceImpl.java#L249-L281](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-moderation-service/src/main/java/microservices/moderationservice/moderation/service/impl/ViolationServiceImpl.java#L249-L281) (in `updateViolationReport`).
*   **Blast Radius:** Outages in Kafka halt webhook completions; silent failures of event propagation prevent `bds-core-macroservice` from activating rental contracts, leading to financial state locks.

### 2. Refactoring Roadmap (Step-by-Step)
1.  **Database Migration:** Add the `outbox_events` table using a SQL script in the corresponding database configuration files.
2.  **Define Outbox Entity & Repository:** Write the JPA mapping and JpaRepository interface in both services.
3.  **Refactor Write Flow:** Remove `kafkaTemplate.send()` or direct publishing method calls. Replace them with serialization of the event payload into a JSON string and saving an `OutboxEvent` record.
4.  **Implement Outbox Poller:** Create a background cron scheduler that polls unprocessed events, publishes them using `kafkaTemplate`, and marks them as processed inside a transactional block. 
5.  **Multi-Instance Locking Protection:** Ensure that only one replica of a service can poll and publish events at a time. Use a PostgreSQL Advisory Lock (`SELECT pg_try_advisory_xact_lock(...)`) or `SELECT ... FOR UPDATE SKIP LOCKED` to prevent duplicate publishing.

### 3. Concrete Code Templates

#### A. Outbox Table Definition
Create this schema in `bds-financial-service` and `bds-moderation-service` databases:
```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_outbox_unprocessed ON outbox_events(created_at) WHERE processed = FALSE;
```

#### B. Outbox Entity (Java JPA)
```java
package com.se361.financial_service.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(nullable = false)
    private boolean processed = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

#### C. Outbox Publisher (Polling implementation with lock safety)
```java
package com.se361.financial_service.services.impl;

import com.se361.financial_service.entities.OutboxEvent;
import com.se361.financial_service.repositories.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishEvents() {
        // Obtains locks using SELECT ... FOR UPDATE SKIP LOCKED
        List<OutboxEvent> pendingEvents = outboxRepository.findUnprocessedForUpdate(Limit.of(50));
        
        for (OutboxEvent event : pendingEvents) {
            try {
                log.info("[Outbox] Dispatching event {} of type {} to Kafka topic: {}", event.getId(), event.getEventType(), event.getEventType());
                
                // Synchronous get() ensures we write to broker successfully before committing
                kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload()).get();
                
                event.setProcessed(true);
                outboxRepository.save(event);
            } catch (Exception e) {
                log.error("[Outbox] Failed to send event {} to Kafka. Transaction will rollback for this record.", event.getId(), e);
                // Throwing exception triggers transaction rollback, holding the lock until next poll
                throw new RuntimeException("Kafka dispatch failed, aborting transaction block", e);
            }
        }
    }
}
```

#### D. Outbox Repository Interface
```java
package com.se361.financial_service.repositories;

import com.se361.financial_service.entities.OutboxEvent;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    
    // Lock records so multiple active service instances don't pull the same events
    @Query("SELECT o FROM OutboxEvent o WHERE o.processed = false ORDER BY o.createdAt ASC")
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")}) // SKIP LOCKED hint equivalent
    List<OutboxEvent> findUnprocessedForUpdate(Limit limit);
}
```

### 4. Track 1 Verification Checklist
*   [ ] SQL migration script successfully executed on Postgres instances.
*   [ ] Unit test written confirming that throwing a database exception rolls back both the payment record AND the outbox record.
*   [ ] Integration test written showcasing that disabling the Kafka broker does not prevent the payment state from saving as success, and that the message is sent once Kafka starts again.
*   [ ] Profiler check confirms no double-publishing occurs when running 2 replicas of `bds-financial-service` simultaneously.

---

## Track 2: Developer B — Asynchronous Service Decoupling (Fat Events & Unified Event Bus)

### 1. Diagnosis & Locations
*   **Core Symptom:** Thin event anti-pattern. Event handlers consume Kafka events containing only IDs, and then execute a synchronous HTTP Feign client query back to the publisher to retrieve context details (N+1 remote dependency).
*   **Exact Code Locations:**
    1.  [`bds-notification-service`: KafkaNotificationListener.java#L44](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-notification-service/src/main/java/com/se100/bds/notificationservice/listeners/KafkaNotificationListener.java#L44) (queries `coreServiceClient.getContractById` back during a `contract-status-changed` event).
    2.  [`bds-appointment-service`: PropertyEventListener.java#L50](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-appointment-service/src/main/java/microservices/appointmentservice/services/property/listener/PropertyEventListener.java#L50) and [L74](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-appointment-service/src/main/java/microservices/appointmentservice/services/property/listener/PropertyEventListener.java#L74) (queries `coreServiceClient.getPropertyDetails` back during `property-created` and `property-updated` events).
    3.  [`bds-search-service`: KafkaSearchEventListener.java#L28](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-search-service/src/main/java/com/se/bds/search/listeners/KafkaSearchEventListener.java#L28) (queries `coreServiceClient.getPropertyLocationInfo` back during `property-searched` event).
*   **Blast Radius:** If the `bds-core-macroservice` experiences a network outage, downstream services cannot process events, breaking notifications, search sync, and property bookings.

### 2. Refactoring Roadmap (Step-by-Step)
1.  **Refactor Global DTO Library:** Update event DTO classes in the `/bds-common` module to hold structural fields (e.g. `title`, `description`, `priceAmount`, `ownerId`, `customerId`).
2.  **Enrich Event Producers:** Locate the publish triggers in `bds-core-macroservice` (e.g., inside `KafkaEventBridge.java`) and ensure they construct and publish these enriched "fat" event records.
3.  **Refactor Downstream Listeners:** Open the listeners in the consumer modules. Change the event deserialization models to parse the new fat events.
4.  **Remove Feign Client Calls:** Delete Feign calls (like `coreServiceClient.getPropertyDetails(...)`) from the event handler methods. Assign data directly from the event payload.

### 3. Concrete Code Templates

#### A. Upgrading Events in `bds-common` (Java Record)
Locate the DTO files in `/bds-common/src/main/java/com/se/bds/common/event/` and expand the fields:
```java
package com.se.bds.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PropertyCreatedIntegrationEvent(
    UUID propertyId,
    String title,
    String description,
    String fullAddress,
    BigDecimal area,
    Integer rooms,
    Integer bathrooms,
    BigDecimal priceAmount,
    UUID ownerId,
    UUID assignedAgentId,
    String transactionType,
    String status
) {}
```

#### B. Enriching Publisher in `bds-core-macroservice`
Update `KafkaEventBridge.java` to extract entity details and map them to the fat event:
```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handlePropertyCreated(PropertyCreatedIntegrationEvent internalEvent) {
    // Map internal rich domain state to the shared fat DTO
    com.se.bds.common.event.PropertyCreatedIntegrationEvent fatEvent = 
        new com.se.bds.common.event.PropertyCreatedIntegrationEvent(
            internalEvent.propertyId(),
            internalEvent.getTitle(),
            internalEvent.getDescription(),
            internalEvent.getFullAddress(),
            internalEvent.getArea(),
            internalEvent.getRooms(),
            internalEvent.getBathrooms(),
            internalEvent.getPriceAmount(),
            internalEvent.getOwnerId(),
            internalEvent.getAssignedAgentId(),
            internalEvent.getTransactionType(),
            internalEvent.getStatus()
        );
    publish("property-created", fatEvent.propertyId().toString(), fatEvent);
}
```

#### C. Decoupled Consumer implementation in `bds-appointment-service`
Refactor `PropertyEventListener.java` to remove HTTP Feign client calls:
```java
package microservices.appointmentservice.services.property.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.event.PropertyCreatedIntegrationEvent;
import microservices.appointmentservice.entities.property.Property;
import microservices.appointmentservice.repositories.PropertyRepository;
import microservices.appointmentservice.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PropertyEventListener {

    private final PropertyRepository propertyRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "property-created", groupId = "appointment-service-group")
    public void onPropertyCreated(String message) {
        log.info("[KAFKA] Processing decoupled property-created event payload...");
        try {
            PropertyCreatedIntegrationEvent event = objectMapper.readValue(message, PropertyCreatedIntegrationEvent.class);
            
            Property property = propertyRepository.findById(event.propertyId())
                    .orElseGet(() -> {
                        Property p = new Property();
                        p.setId(event.propertyId());
                        return p;
                    });
            
            // Map values directly from fat event fields - NO network call
            property.setTitle(event.title());
            property.setDescription(event.description());
            property.setFullAddress(event.fullAddress());
            property.setArea(event.area());
            property.setRooms(event.rooms());
            property.setBathrooms(event.bathrooms());
            property.setPriceAmount(event.priceAmount());
            property.setOwnerId(event.ownerId());
            property.setAssignedAgentId(event.assignedAgentId());
            property.setTransactionType(Constants.TransactionTypeEnum.valueOf(event.transactionType().toUpperCase()));
            property.setStatus(Constants.PropertyStatusEnum.valueOf(event.status().toUpperCase()));
            
            propertyRepository.save(property);
            log.info("[KAFKA] Property ID={} sync complete without REST calls", event.propertyId());
        } catch (Exception e) {
            log.error("[KAFKA] Failed to sync property payload directly", e);
        }
    }
}
```

### 4. Track 2 Verification Checklist
*   [ ] Clean build of `/bds-common` is successful.
*   [ ] Checked `bds-appointment-service`, `bds-notification-service`, and `bds-search-service` listeners: verified that all references to `coreServiceClient` inside consumer listeners have been deleted.
*   [ ] Run integration test where `bds-core-macroservice` is entirely shut down, and confirm that sending a `property-created` Kafka mock message still successfully creates the record inside the appointment service database.

---

## Track 3: Developer C — Aggregation & Resilience (Materialized Views & Circuit Breakers)

### 1. Diagnosis & Locations
*   **Core Symptom:** N+1 REST queries in dashboard reads, coupled with a complete lack of Circuit Breaker protection on Feign clients. In `ViolationServiceImpl.java`, paginated list calls trigger synchronous HTTP Feign client calls to `iam-service` and `core-macroservice` for every single list element.
*   **Exact Code Locations:**
    1.  [`bds-moderation-service`: ViolationServiceImpl.java#L98-L111](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-moderation-service/src/main/java/microservices/moderationservice/moderation/service/impl/ViolationServiceImpl.java#L98-L111) (calls `enrichReporterInfo` and `enrichReportedInfo` in lists).
    2.  [`bds-moderation-service`: CoreServiceClient.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-moderation-service/src/main/java/microservices/moderationservice/client/CoreServiceClient.java) (No fallback or circuit breaker).
    3.  [`bds-moderation-service`: IamServiceClient.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-moderation-service/src/main/java/microservices/moderationservice/client/IamServiceClient.java) (No fallback or circuit breaker).
*   **Blast Radius:** 20 records on the moderation dashboard trigger 40 network calls. If `iam-service` slows down, the thread pool of the moderation service quickly fills up, causing a cluster-wide cascading crash.

### 2. Refactoring Roadmap (Step-by-Step)
1.  **Define Local Database Replicas:** Create replica tables/entities for `UserReplica` and `PropertyReplica` inside the `bds-moderation-service` database.
2.  **Add Event Synchronizers:** Create Kafka Event Listeners in the moderation service to listen to user updates (`user-created`, `user-updated` events) and property updates, updating local replica tables asynchronously.
3.  **Refactor Paginated Query Join:** Update `ViolationRepository` and `ViolationServiceImpl` to perform local SQL joins on the replica entities. Retrieve everything in one database query, removing HTTP enrichment methods.
4.  **Add Circuit Breakers to Feign Clients:** Add `spring-cloud-starter-circuitbreaker-resilience4j` to the moderation service's `pom.xml`. Create fallback factory classes that implement the Feign interfaces.
5.  **Define Resilient Fallback Actions:** For `IamServiceClient`, implement a fallback that returns cached or fail-secure mock details (e.g. "User Offline") without throwing throwing blocking errors.

### 3. Concrete Code Templates

#### A. Replica Entities (Materialized Read-Model)
Define local database cache tables inside the moderation service:
```java
package microservices.moderationservice.moderation.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "user_replicas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserReplica {
    @Id
    private UUID id;
    private String fullName;
    private String avatarUrl;
    private String email;
    private String role;
}
```

#### B. SQL Join Query (Replaces N+1 REST API calls)
```java
package microservices.moderationservice.moderation.repository;

import microservices.moderationservice.moderation.entity.ViolationReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface ViolationRepository extends JpaRepository<ViolationReport, UUID>, JpaSpecificationExecutor<ViolationReport> {

    @Query(value = "SELECT v FROM ViolationReport v " +
                   "LEFT JOIN UserReplica reporter ON v.reporterId = reporter.id " +
                   "LEFT JOIN UserReplica reportedUser ON (v.relatedEntityId = reportedUser.id AND v.relatedEntityType <> 'PROPERTY') " +
                   "LEFT JOIN PropertyReplica property ON (v.relatedEntityId = property.id AND v.relatedEntityType = 'PROPERTY')",
           countQuery = "SELECT COUNT(v) FROM ViolationReport v")
    Page<ViolationReport> findAllWithMaterializedReplicas(Pageable pageable);
}
```

#### C. Setting up Circuit Breaker + Fallback Factory
```java
package microservices.moderationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.UUID;
import java.util.Map;

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

#### D. Fallback Factory Class Implementation
```java
package microservices.moderationservice.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class IamServiceClientFallbackFactory implements FallbackFactory<IamServiceClient> {
    
    @Override
    public IamServiceClient create(Throwable cause) {
        return new IamServiceClient() {
            @Override
            public Map<String, Object> validateUser(UUID userId, String role) {
                log.error("Circuit Breaker: User validation failed for UUID: {}. Details: {}", userId, cause.getMessage());
                // Fail-secure: default validation to false (reject operations during IAM outages)
                return Map.of("active", false, "status", "OFFLINE_FALLBACK");
            }

            @Override
            public Map<String, Object> getUserDetails(UUID userId) {
                log.warn("Circuit Breaker: Fetching user details failed for UUID: {}. Returning mock replica placeholder.", userId);
                return Map.of(
                    "success", true,
                    "data", Map.of(
                        "fullName", "Profile Offline", 
                        "avatarUrl", "/assets/fallback-avatar.png",
                        "role", "UNKNOWN"
                    )
                );
            }
        };
    }
}
```

#### E. Resilience4j Configurations (`bds-moderation-service` - `application.yml`)
Add configuration flags inside `bds-moderation-service/src/main/resources/application.yml`:
```yaml
feign:
  circuitbreaker:
    enabled: true

resilience4j:
  circuitbreaker:
    instances:
      iamServiceClient:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50 # Trips open if 50% of last 10 calls fail
        slowCallRateThreshold: 75
        slowCallDurationThreshold: 2000ms # Call counts as slow if it exceeds 2s
        waitDurationInOpenState: 15000ms # Holds open for 15s before attempting half-open
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
```

### 4. Track 3 Verification Checklist
*   [ ] Checked Maven `pom.xml` in `bds-moderation-service` to confirm the `resilience4j` and `spring-cloud-starter-openfeign` dependencies are present.
*   [ ] Created `UserReplica` and `PropertyReplica` structures locally and verified the schema creation on start.
*   [ ] Unit test written confirming that calling list endpoints performs exactly **1 SQL query** with outer joins, rather than spawning subsequent REST transactions.
*   [ ] Verified that when `iam-service` is deliberately crashed, loading the violation list dashboard does not fail, but returns "Profile Offline" gracefully.
