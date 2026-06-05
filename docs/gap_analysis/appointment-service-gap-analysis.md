# Post-Merge Gap Analysis Report: `appointment-service`

## Executive Summary
During this post-merge synchronization phase, the [appointment-service](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service) was analyzed. The service currently behaves as a **"shared-database monolith slice"** rather than a true, decoupled microservice. 

While the HTTP endpoints and core appointment booking/cancellation logic have been copied over from the monolith, the service suffers from severe architectural problems:
1. **Tight Database Coupling:** It retains direct JPA relationships to external entities (like `Property`, `User`, `Contract`, `Ward`, `Payment`, `ViolationReport`) and queries a local duplicate database schema ([bds-appointment-service-db](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/resources/application.properties#L32)).
2. **Missing Inter-Service Event Sync:** The service cannot consume any Kafka events published by the primary [bds-core-macroservice](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice) (e.g. `property-status-changed`), causing major data consistency risks.
3. **Monolithic Duplication:** Services like [NotificationServiceImpl](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/services/notification/impl/NotificationServiceImpl.java) and [RankingServiceImpl](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/services/ranking/impl/RankingServiceImpl.java) write directly to local tables and completely bypass the centralized [notification-service](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/notification-service) and platform-wide ranking domains.
4. **Contract Violations:** The service defines its own DTOs and API responses instead of inheriting from [bds-common](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common).

---

## Missing Features (Legacy vs. New)
Comparing the business logic inside the `/legacy` folder with `appointment-service` reveals the following gaps:

* **Missing Ranking Scheduler:** 
  The daily background calculation job [SaleAgentRankingScheduler](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/domains/ranking/scheduler/SaleAgentRankingScheduler.java) was not migrated to the target service. As a result, agent rankings, point conversions, and tier positions will never be automatically calculated.
* **Inconsistent Package Structure:** 
  The class [AppointmentDummyData](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/data/AppointmentDummyData.java#L1) defines its package as `microservices.appointmentservice.data.domains`, but the file is located directly inside `microservices.appointmentservice.data`. This inconsistency will cause compilation issues if not resolved.

---

## Integration Gaps (Core Macroservice)
The touchpoints between the target service and [bds-core-macroservice](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice) are severely broken due to the lack of an integration layer:

* **Property Bounded Context Mismatch:** 
  In the core service, the [Property](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/property/internal/domain/model/Property.java#L47-L59) aggregate root has been refactored for a microservice environment—replacing cross-module relationships (User, Agent, Ward) with raw UUIDs. However, the appointment service's copy of [Property](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/entities/property/Property.java#L32-L40) still retains strict JPA relationships (`@ManyToOne`, `@OneToMany`) to entities it should not own (e.g., `Customer`, `Contract`, `Media`).
* **Unsynchronized Property Statuses:** 
  The appointment service verifies property availability in [AppointmentServiceImpl.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/services/appointment/impl/AppointmentServiceImpl.java#L103) against its local repository. Because it does not listen to `property-status-changed` events published by the core service, the local table will fall out of sync, enabling customers to book appointments for properties that have already been SOLD or RENTED.
* **Missing Project Module Registration:** 
  The [pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/pom.xml#L18) root descriptor lists `bds-core-macroservice` and `bds-common` as modules, but entirely ignores `appointment-service`. It is built as an isolated project instead of a module.

---

## Contract Violations
The target service duplicates definitions instead of importing standard schemas from [bds-common](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common):

* **POM Dependency Absence:** 
  The target service's [pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/pom.xml) does not reference the shared [bds-common](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common) JAR.
* **Duplicated Base DTOs:** 
  [AbstractBaseDataResponse](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/AbstractBaseDataResponse.java) is duplicated locally inside the service instead of importing it from [com.se.bds.common.dto.AbstractBaseDataResponse](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common/src/main/java/com/se/bds/common/dto/AbstractBaseDataResponse.java).
* **API Wrapper Non-standardization:** 
  The controller uses custom wrappers [AbstractBaseResponse](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/AbstractBaseResponse.java) and [SingleResponse](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/SingleResponse.java) rather than using the standard [ApiResponse](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common/src/main/java/com/se/bds/common/dto/ApiResponse.java) utilized by the core macroservice.
* **Redundant Enums:** 
  The [Constants.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/utils/Constants.java) class in the target service contains enums like `ContractStatusEnum` and `PaymentTypeEnum` which are completely unused in the appointment context and should be imported from a shared location.

---

## Communication Anti-patterns
* **Synchronous JWT Database Lookup:** 
  The [JwtAuthenticationFilter](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/securities/JwtAuthenticationFilter.java#L39) calls `userService.loadUserById(userId)` on every request, triggering a synchronous local database lookup. In a stateless microservice system, this filter should parse the JWT signature, trust the user identity, and populate user roles directly from the token claims forwarded by the [api-gateway](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/api-gateway/src/main/java/com/se100/bds/gateway/filter/JwtAuthenticationFilter.java#L57).
* **Direct Local DB Coupling:** 
  The service depends on a local, duplicated model schema. Changing properties or user details in the core database does not propagate here, creating a high risk of out-of-sync queries.
* **Isolated Notification Creation:** 
  The [NotificationServiceImpl](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/services/notification/impl/NotificationServiceImpl.java#L31) directly invokes `notificationRepository.save(notification)` to write to a local table instead of invoking the API of the central [notification-service](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/notification-service) or publishing notification events.

---

## Actionable Remediation Steps

### Phase 1: Parent & Contract Standardization (Priority: High)
- [ ] **Align Parent POM:** Change the parent section of [appointment-service/pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/pom.xml) to use the parent `batdongsan-platform` instead of `spring-boot-starter-parent`.
- [ ] **Register Module:** Register the `appointment-service` module in the root [pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/pom.xml).
- [ ] **Import Common Contracts:** Add the `bds-common` dependency to the target service's POM.
- [ ] **Standardize DTOs:** Replace local definitions of [AbstractBaseDataResponse](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/dtos/responses/AbstractBaseDataResponse.java) with imports from [com.se.bds.common.dto](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common/src/main/java/com/se/bds/common/dto/AbstractBaseDataResponse.java).
- [ ] **Standardize API Responses:** Refactor controllers to wrap payloads in `ApiResponse<T>` from `bds-common`.

### Phase 2: Decouple Schema & Implement Event Sync (Priority: High)
- [ ] **Add Kafka Support:** Add `spring-kafka` dependency to the target service.
- [ ] **Implement Property Listener:** Create a Kafka event listener in the target service to consume `property-created`, `property-updated`, and `property-status-changed` events to maintain a local, read-only cache of properties.
- [ ] **Remove Direct JPA Joins:** Refactor `Property` and `Appointment` entities to refer to users and contracts by their raw `UUID`s, aligning them with the aggregates design of the core service.

### Phase 3: Centralize Core Communication (Priority: Medium)
- [ ] **Refactor Notifications:** Rewrite [NotificationServiceImpl](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/services/notification/impl/NotificationServiceImpl.java) to either use an HTTP client (like WebClient/Feign) calling `POST /api/notifications` on the central [notification-service](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/notification-service) or publish message payloads to a Kafka topic.
- [ ] **Refactor Authentication:** Modify `JwtAuthenticationFilter` to extract user attributes and roles directly from the validated token claims rather than querying the database.
- [ ] **Restore Ranking Scheduler:** Migrate the daily scheduler [SaleAgentRankingScheduler](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/domains/ranking/scheduler/SaleAgentRankingScheduler.java) into a background worker block to compute standings regularly.
