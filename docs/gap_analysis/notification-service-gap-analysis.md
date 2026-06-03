# Post-Merge Gap Analysis Report: `notification-service`

## Executive Summary
During this post-merge synchronization phase, the [notification-service](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/notification-service) was analyzed. The service currently functions in isolation as a synchronous REST-based utility rather than a secure, event-driven, and integrated notification provider within the BatDongSan microservices architecture.

Although the service successfully implements basic CRUD actions and integrates Firebase Cloud Messaging (FCM) via conditionally enabled configurations, it suffers from several critical architectural, integration, and security gaps:

1. **Synchronous Coupling & Integration Gaps:** The microservice lacks event-driven logic. While the core macroservice ([bds-core-macroservice](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice)) publishes Kafka events when contracts are signed or payments completed, the `notification-service` contains no Kafka dependencies or consumer listeners.
2. **Missing Core Business Notifications:** Background processes and schedulers in the core service (e.g., [RentalContractScheduler.java:L176](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/application/service/RentalContractScheduler.java#L176)) have been stripped of the notification logic that existed in legacy ([RentalContractScheduler.java:L208](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/domains/contract/scheduler/RentalContractScheduler.java#L208)), resulting in an absolute business parity gap.
3. **FCM Token Resolution Shift:** The REST creation endpoint ([NotificationController.java:L105](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/notification-service/src/main/java/com/se100/bds/notificationservice/controllers/NotificationController.java#L105)) expects the client to pass the recipient's FCM token, introducing massive integration complexity. Additionally, the core service has had its user profiles completely decoupled from FCM tokens, leaving no system component responsible for registering and mapping FCM tokens to users.
4. **Security Spoofing Risks:** Downstream endpoints extract user identities directly from the `X-User-Id` header without validating JWT signatures or verifying roles via Spring Security.
5. **Contract Violations:** The service operates in isolation, duplicating models like [AbstractBaseDataResponse](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/notification-service/src/main/java/com/se100/bds/notificationservice/dtos/responses/AbstractBaseDataResponse.java) and custom response wrappers instead of using the standardized library [bds-common](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common).

---

## Missing Features (Legacy vs. New)
Comparing the business logic inside the `/legacy` folder with [notification-service](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/notification-service) reveals the following gaps:

* **Scheduler Notification Triggers:**
  In legacy, the background job [RentalContractScheduler.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/domains/contract/scheduler/RentalContractScheduler.java) automatically dispatched critical customer notifications:
  * **Payment Due Notifications:** Generated on the 1st of each month ([RentalContractScheduler.java:L208](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/domains/contract/scheduler/RentalContractScheduler.java#L208)).
  * **Payment Overdue Alerts:** Generated for unpaid monthly rents after hitting thresholds ([RentalContractScheduler.java:L267](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/domains/contract/scheduler/RentalContractScheduler.java#L267)).
  * **Rent Reminders:** Dispatched weekly for overdue payments ([RentalContractScheduler.java:L309](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/domains/contract/scheduler/RentalContractScheduler.java#L309)).
  * **Contract Completed Alerts:** Notified both customer and owner of ending leases and pending security deposits ([RentalContractScheduler.java:L339-L357](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/domains/contract/scheduler/RentalContractScheduler.java#L339-L357)).
  
  In the migrated core macroservice scheduler ([RentalContractScheduler.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/application/service/RentalContractScheduler.java)), all corresponding `notificationService.createNotification(...)` calls have been deleted and replaced with log messages.
  
* **FCM Registration and User Association:**
  In legacy, the database `User` entity stored the user's registered FCM token ([User.java:L98](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/models/entities/user/User.java#L98)). 
  * The new core macroservice's database schema does not track FCM tokens.
  * The new `notification-service` database tracks the recipient's raw UUID but does not store user profiles or their FCM tokens.
  * As a consequence, there is no mechanism for updating, storing, or registering FCM tokens when users log in. The service expects caller microservices to somehow supply the correct FCM token in the request payload ([CreateNotificationRequest.java:L15](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/notification-service/src/main/java/com/se100/bds/notificationservice/dtos/requests/CreateNotificationRequest.java#L15)), which they cannot do because the core user profile has no FCM token database column.

* **Controller Security Context & RBAC Gaps:**
  * **Legacy Auths:** The legacy [NotificationController.java:L39](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/controllers/NotificationController.java#L39) enforced authorization annotations like `@PreAuthorize("hasAnyRole(...)")`.
  * **New Service:** The new controller lacks security configurations and accepts `X-User-Id` header values blindly. Any caller bypassing the API gateway can easily query notifications belonging to other users.

---

## Integration Gaps (Core Macroservice)
The target service does not integrate with the core macroservice properly:

* **Event Broker Silence (Missing Kafka Consumers):**
  The `bds-core-macroservice` has a `KafkaEventBridge` configured to publish business events (e.g. `contract-status-changed`, `payment-completed`, `property-status-changed`).
  However, the `notification-service` has zero integration with Kafka. It cannot consume these events to automatically send push notifications.
* **Isolated Maven Build:**
  The `notification-service` is not registered in the parent POM [pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/pom.xml) under `<modules>`. It compiles using Java 17 and groupId `com.se100.bds`, deviating from the standard Java 21 compile configuration and groupId `com.se.bds` defined by the parent POM.
* **Appointment Service Notification Duplication:**
  The [appointment-service](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service) has its own local, duplicated `Notification` entity and a local `NotificationRepository`. Instead of integrating with the central `notification-service`, the appointment service writes to its own local notification table ([NotificationServiceImpl.java:L55](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/appointment-service/src/main/java/microservices/appointmentservice/services/notification/impl/NotificationServiceImpl.java#L55)), preventing users from retrieving notifications in a unified mailbox.

---

## Contract Violations
The service duplicates definitions instead of importing standard contracts from [bds-common](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common):

* **POM Dependency Absence:**
  The `notification-service`'s [pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/notification-service/pom.xml) has no dependency on the shared [bds-common](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common) module.
* **Response Model Violations:**
  * The local [AbstractBaseDataResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/notification-service/src/main/java/com/se100/bds/notificationservice/dtos/responses/AbstractBaseDataResponse.java) is a duplicate of [AbstractBaseDataResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common/src/main/java/com/se/bds/common/dto/AbstractBaseDataResponse.java) in `bds-common`.
  * The service utilizes custom structures `SingleResponse` and `PageResponse` which serialize to `{ "statusCode": 200, "message": "...", "data": ... }`. This diverges from the standard [ApiResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common/src/main/java/com/se/bds/common/dto/ApiResponse.java) wrapper format (`{ "success": boolean, "message": String, "data": T }`).
* **Utility Duplications:**
  The enums `NotificationTypeEnum`, `RelatedEntityTypeEnum`, and `NotificationStatusEnum` in [Constants.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/notification-service/src/main/java/com/se100/bds/notificationservice/utils/Constants.java) duplicate the logical structure that should be centrally defined and managed in `bds-common` to maintain strict contract parity across microservices.
* **Error Payload Mismatches:**
  The local [AppExceptionHandler.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/notification-service/src/main/java/com/se100/bds/notificationservice/exceptions/AppExceptionHandler.java) maps errors using a custom `DetailedErrorResponse` instead of standardizing on `bds-common`'s error formats.

---

## Communication Anti-patterns
* **Synchronous REST Creation Coupling:**
  The REST endpoint `POST /api/notifications` is a synchronous network boundary. Callers must wait for a HTTP response. Under load, this can result in cascading request failures. 
* **Missing Resiliency Controls (Timeouts and Circuit Breakers):**
  The connection between `notification-service` and Firebase's push service ([FirebasePushService.java:L22](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/notification-service/src/main/java/com/se100/bds/notificationservice/services/impl/FirebasePushService.java#L22)) has no timeout limits or Resilience4j circuit breakers. A slow or hanging Firebase connection will block the worker threads in `notification-service`.
* **Static Service Binding:**
  The microservice runs on port `8083` and does not configure dynamic discovery via Eureka client, creating tight port coupling in local environment scripts.

---

## Actionable Remediation Steps

### Phase 1: Build & Contract Standardization (Priority: High)
- [ ] **Align Parent POM:** Refactor [notification-service/pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/notification-service/pom.xml) to inherit from the parent POM `batdongsan-platform` instead of `spring-boot-starter-parent`. Update its compile configurations to standard Java 21 and set the group ID to `com.se.bds`.
- [ ] **Register Maven Module:** Register the `notification-service` module in the root [pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/pom.xml).
- [ ] **Import shared contracts library:** Add the `bds-common` dependency to [notification-service/pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/notification-service/pom.xml).
- [ ] **Eliminate Duplicate DTOs:** Delete the duplicate [AbstractBaseDataResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/notification-service/src/main/java/com/se100/bds/notificationservice/dtos/responses/AbstractBaseDataResponse.java) class and import the shared contract from `com.se.bds.common.dto`.
- [ ] **Enforce Standard ApiResponse Wrapper:** Refactor [NotificationController.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/notification-service/src/main/java/com/se100/bds/notificationservice/controllers/NotificationController.java) to wrap response payloads in `ApiResponse<T>` from `bds-common`.
- [ ] **Standardize Exception Handler:** Update [AppExceptionHandler.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/notification-service/src/main/java/com/se100/bds/notificationservice/exceptions/AppExceptionHandler.java) to return standardized error structures using `ApiResponse.error(...)`.

### Phase 2: Decoupled Data & Secure Verification (Priority: High)
- [ ] **FCM Token Schema Registration:** Add the `fcm_token` column back to the core macroservice's user profile database mapping to allow storing FCM tokens on user login.
- [ ] **FCM Query Port Integration:** Add an endpoint in the core macroservice to fetch a user's FCM token by their UUID. In `notification-service`, create an integration client (e.g. Feign or RestTemplate) that queries this user service FCM port dynamically when an FCM token is not supplied in the payload.
- [ ] **Secure Downstream Header Filter:** Create a security filter configuration in `notification-service` to validate headers coming from `api-gateway` and reject unauthenticated requests.

### Phase 3: Transition to Event-Driven Communication (Priority: Medium)
- [ ] **Add Kafka Integration:** Add `spring-kafka` to [notification-service/pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/notification-service/pom.xml).
- [ ] **Implement Kafka Event Listeners:** Build event consumers in `notification-service` that listen to Kafka topics published by the core macroservice (such as `contract-status-changed` and `payment-completed`). These consumers should resolve the target user's FCM token and dispatch notifications asynchronously.
- [ ] **Rebuild Core Schedulers:** Refactor `RentalContractScheduler` in the core macroservice to publish event payloads instead of local log fallbacks, enabling true asynchronous notification delivery.
- [ ] **Decouple Appointment Service Notifications:** Add the `spring-kafka` dependency to `appointment-service`, remove its local duplicate notification tables, and refactor it to send notification requests asynchronously over Kafka.
- [ ] **Dynamic Service Discovery:** Add `spring-cloud-starter-netflix-eureka-client` to `notification-service` to enable dynamic registration with Eureka.
- [ ] **Add Resilience Controls:** Configure Resilience4j connect and response timeouts in `FirebasePushService` to handle Firebase outages gracefully.
