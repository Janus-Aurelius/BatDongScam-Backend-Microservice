# Implementation and Verification Plan: appointment-service Decoupling & Alignment

This document outlines the detailed roadmap, risk assessment, and verification strategy to transition the `appointment-service` from a tightly coupled "shared-database monolith slice" into a decoupled, state-of-the-art microservice compliant with the BatDongSan platform architecture.

---

## 1. Implementation Roadmap (Phased Execution)

### Phase A: Contract & DTO Standardization
This phase focuses on parent POM alignment, module registration, and replacing duplicate local data structures with contracts defined in `/bds-common`.

#### [MODIFY] [pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/pom.xml)
- **Action Required:** Add `<module>appointment-service</module>` under the `<modules>` list on line 21 to register `appointment-service` as an official Maven project module.
- **Dependencies:** None.

#### [MODIFY] [pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/pom.xml)
- **Action Required:**
  - Change the parent declaration to reference the root POM `batdongsan-platform` instead of `spring-boot-starter-parent`:
    ```xml
    <parent>
        <groupId>com.se.bds</groupId>
        <artifactId>batdongsan-platform</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    ```
  - Remove `<groupId>microservices</groupId>` since it is inherited from the parent.
  - Add dependency for the shared library `bds-common`:
    ```xml
    <dependency>
        <groupId>com.se.bds</groupId>
        <artifactId>bds-common</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>
    ```
- **Dependencies:** `com.se.bds:bds-common:0.0.1-SNAPSHOT`.

#### [DELETE] [AbstractBaseDataResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/AbstractBaseDataResponse.java)
- **Action Required:** Delete the local definition of `AbstractBaseDataResponse` to prevent naming collisions and enforce the use of `com.se.bds.common.dto.AbstractBaseDataResponse`.
- **Dependencies:** None.

#### [MODIFY] Local DTOs extending `AbstractBaseDataResponse`
- **Target Files:**
  - [NotificationItem.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/notification/NotificationItem.java)
  - [ViewingDetailsCustomer.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/appointment/ViewingDetailsCustomer.java)
  - [NotificationDetails.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/notification/NotificationDetails.java)
  - [ViewingListItem.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/appointment/ViewingListItem.java)
  - [ViewingCardDto.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/appointment/ViewingCardDto.java)
  - [ViewingDetailsAdmin.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/appointment/ViewingDetailsAdmin.java)
  - [UserProfileResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/user/otherprofile/UserProfileResponse.java)
  - [CustomerListItem.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/user/listitem/CustomerListItem.java)
  - [MeResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/user/meprofile/MeResponse.java)
  - [SimpleUserResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/user/simple/SimpleUserResponse.java)
  - [FreeAgentListItem.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/user/listitem/FreeAgentListItem.java)
  - [SaleAgentListItem.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/user/listitem/SaleAgentListItem.java)
  - [PropertyOwnerListItem.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/user/listitem/PropertyOwnerListItem.java)
- **Action Required:** Update import statements to replace `microservices.appointmentservice.dtos.responses.AbstractBaseDataResponse` with `com.se.bds.common.dto.AbstractBaseDataResponse`.
- **Dependencies:** `bds-common` library.

#### [DELETE] Local Response Wrappers
- **Target Files:**
  - [SingleResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/SingleResponse.java)
  - [PageResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/PageResponse.java)
  - [AbstractBaseResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/AbstractBaseResponse.java)
  - [ResponseFactory.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/controllers/base/ResponseFactory.java)
- **Action Required:** Delete these custom response structures and factories.
- **Dependencies:** None.

#### [MODIFY] [AppointmentController.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/controllers/AppointmentController.java)
- **Action Required:** Refactor all endpoints to return the standardized `com.se.bds.common.dto.ApiResponse<T>` instead of `SingleResponse` or `PageResponse`. Re-route all calls from `responseFactory` to direct `ApiResponse.success(data)` returns.
- **Dependencies:** `com.se.bds.common.dto.ApiResponse` import.

#### [MODIFY] [AppointmentDummyData.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/data/AppointmentDummyData.java)
- **Action Required:** Fix package mismatch on line 1 from `package microservices.appointmentservice.data.domains;` to `package microservices.appointmentservice.data;`.
- **Dependencies:** None.

---

### Phase B: Business Logic Parity
This phase focuses on migrating scheduling algorithms and utilities left behind in the `/legacy` codebase.

#### [NEW] [SaleAgentRankingScheduler.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/services/ranking/scheduler/SaleAgentRankingScheduler.java)
- **Action Required:** Port the `SaleAgentRankingScheduler` background computing logic from `/legacy`. Calculate monthly/career point standing, conversion ratios, customer satisfaction indices, and rankings at midnight daily (`@Scheduled(cron = "0 0 0 * * ?")`).
- **Dependencies:**
  - `org.springframework.scheduling.annotation.Scheduled`
  - `microservices.appointmentservice.repositories.ranking.IndividualSalesAgentPerformanceMonthRepository`
  - `microservices.appointmentservice.repositories.ranking.IndividualSalesAgentPerformanceCareerRepository`
  - `microservices.appointmentservice.services.user.UserService`

#### [NEW] [RankingUtil.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/services/ranking/utils/RankingUtil.java)
- **Action Required:** Port legacy utility methods including `getExtraPoint`, `getCustomerTier`, and `getPreviousMonth`.
- **Dependencies:** None.

#### [MODIFY] [AppointmentServiceApplication.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/AppointmentServiceApplication.java)
- **Action Required:** Add `@EnableScheduling` to the Spring Boot main application class to activate the ranking job.
- **Dependencies:** None.

---

### Phase C: Integration & Communication
This phase decouples databases and replaces synchronous cross-module lookups with Kafka event streaming and stateless JWT verification.

#### [MODIFY] [pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/pom.xml)
- **Action Required:** Add the Spring Kafka dependency to consume events.
  ```xml
  <dependency>
      <groupId>org.springframework.kafka</groupId>
      <artifactId>spring-kafka</artifactId>
  </dependency>
  ```
- **Dependencies:** Spring Kafka library.

#### [MODIFY] [application.properties](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/resources/application.properties)
- **Action Required:**
  - Change default port to avoid conflicts: `server.port=${SERVER_PORT:8085}`.
  - Add bootstrap config: `spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}`.
  - Set default consumer group: `spring.kafka.consumer.group-id=appointment-service-group`.
  - Add core service REST base URL: `app.core-service.url=${CORE_SERVICE_URL:http://localhost:8081}`.
- **Dependencies:** None.

#### [NEW] [PropertyEventListener.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/services/property/listener/PropertyEventListener.java)
- **Action Required:** Create a listener to consume property messages and update the local replica cache:
  - `property-created`: Parse event and save read-only `Property` entity.
  - `property-updated`: Fetch property details from `core-macroservice` via REST client and synchronize local entity fields.
  - `property-deleted`: Mark/Delete the replica entity.
  - `property-status-changed`: Consume `PropertyStatusChangedEvent` (from `bds-common`) and update status.
  - `property-agent-assigned`: Consume `PropertyAgentAssignedEvent` and update `assignedAgentId`.
- **Dependencies:**
  - `org.springframework.kafka.annotation.KafkaListener`
  - `com.se.bds.common.event.PropertyStatusChangedEvent`
  - Spring Boot `RestClient` bean injected to hit `/public/properties/{propertyId}`.

#### [MODIFY] Decouple Entity Graph Relations
- **Target Files:**
  - [Property.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/entities/property/Property.java)
    - Replace `@ManyToOne PropertyOwner owner` with `@Column(name = "owner_id") private UUID ownerId;`.
    - Replace `@ManyToOne SaleAgent assignedAgent` with `@Column(name = "assigned_agent_id") private UUID assignedAgentId;`.
    - Replace `@ManyToOne Ward ward` with `@Column(name = "ward_id") private UUID wardId;`.
    - Remove `@OneToMany List<Contract> contracts`.
  - [Appointment.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/entities/appointment/Appointment.java)
    - Replace `@ManyToOne Customer customer` with `@Column(name = "customer_id") private UUID customerId;`.
    - Replace `@ManyToOne SaleAgent agent` with `@Column(name = "agent_id") private UUID agentId;`.
- **Action Required:** Refactor JPA annotations to eliminate cross-module joins, storing raw UUID identity references instead.
- **Dependencies:** None.

#### [MODIFY] [PropertyRepository.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/repositories/PropertyRepository.java)
- **Action Required:** Rewrite JPQL and native queries to join tables explicitly by UUID IDs (`JOIN Ward w ON p.wardId = w.id`, etc.) instead of dot-notation traversal.
- **Dependencies:** None.

#### [MODIFY] [AppointmentServiceImpl.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/services/appointment/impl/AppointmentServiceImpl.java)
- **Action Required:** Replace automatic object joins with manual database fetches (e.g. querying `customerRepository.findById(customerId)`) when mapping details and checking availability.
- **Dependencies:** None.

#### [MODIFY] [NotificationServiceImpl.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/services/notification/impl/NotificationServiceImpl.java)
- **Action Required:** Replace local notification persistence with a RestClient POST call to the central `notification-service` (`POST http://localhost:8083/api/notifications`).
- **Dependencies:** `RestClient` configuration.

#### [MODIFY] [JwtAuthenticationFilter.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/securities/JwtAuthenticationFilter.java)
- **Action Required:** Extract JWT token claims statelessly (sub as userId, role, email) and configure Spring Security's `Authentication` context directly without making a synchronous DB call (`loadUserById`).
- **Dependencies:** None.

#### [MODIFY] [application.yaml (Gateway)](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/resources/application.yaml)
- **Action Required:** Map requests starting with `/api/appointment/**` to the new `appointment-service` port `http://localhost:8085`.
- **Dependencies:** None.

---

## Open Questions

> [!IMPORTANT]
> **JWT Claims Role Mapping:** Does the JWT token issued by the platform's IAM service currently contain the user's role/authority claim? If not, we will need to update the IAM service to include the role in the JWT token payload (e.g. under a `role` claim) to enable fully stateless authentication in `appointment-service`.

---

## 2. Risk Assessment & Edge Cases

- **Eventual Consistency Window:** Real-time property status updates rely on Kafka events. There exists a small latency window where a property status changed event (`property-status-changed` to `SOLD` or `RENTED`) is not yet processed, potentially permitting a client to book an appointment on a property that was just sold.
  - *Mitigation:* The book appointment route will execute a final validation, or we can check the status on the frontend catalog beforehand.
- **Stale User Names in Replica Tables:** If names or avatar URLs are updated in the core identity service, local replica tables (`Customer`, `SaleAgent`) won't reflect these changes unless user status updates or name updates are broadcast via Kafka.
  - *Mitigation:* Design plan includes a Kafka topic consumer for user profile updates if name changes are supported.
- **JPA Join Schema Failures:** JPQL joins in `PropertyRepository` will fail if refactoring doesn't cover all implicit joins.
  - *Mitigation:* Perform comprehensive compile check on startup and run dry-run queries on local H2/embedded databases.

---

## 3. Comprehensive Verification Plan

### Automated Tests
- **Unit Testing Strategy:**
  - Create new unit tests in `SaleAgentRankingSchedulerTest` to test ranking calculation formulas (conversion score, completion score, satisfaction score, and tier positioning).
  - Add mock tests for `PropertyEventListener` to assert that incoming Kafka payloads properly parse and update/create local replica tables.
  - Update `JwtAuthenticationFilterTest` using mocked JWT tokens containing user credentials and roles to assert that roles are extracted statelessly without DB lookup.
- **Integration Testing:**
  - Verify Kafka messaging by publishing mock events to `property-status-changed` and asserting database state reflects the changed status.
  - Test HTTP API Gateway routing by launching the API Gateway on `8080`, sending a GET request to `/api/appointment/viewing-cards`, and asserting the request is routed to `8085` and returns 200 OK with `ApiResponse` schema.
  - Test the Centralized Notification REST integration: Mock `notification-service` endpoint `POST /api/notifications` and assert that booking an appointment triggers an HTTP call with the correct payload.

### Contract Validation
- Check HTTP response structures of all endpoints to ensure they validate against:
  - `{ "success": boolean, "message": String, "data": T }` (standard `ApiResponse`).
- Assert that validation errors (like invalid date or missing property ID) return an HTTP 400 wrapping the message in `ApiResponse.error(...)`.
