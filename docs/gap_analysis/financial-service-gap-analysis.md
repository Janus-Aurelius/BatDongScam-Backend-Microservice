# Gap Analysis Report: `bds-financial-service`

This report provides a comprehensive post-merge gap analysis of the newly synchronized **Financial & Payment Service** (`bds-financial-service`) against the **Legacy Monolith** (`/legacy`), the **Core Macroservice** (`/bds-core-macroservice`), and the **Shared Contracts** (`/bds-common`).

---

## 🔍 Executive Summary

A comprehensive audit of the `bds-financial-service` codebase reveals that while controllers, service frameworks, and gateway clients (PayPal, Payway) have been established, the service is currently **not buildable** and is **architecturally isolated** from the rest of the microservice ecosystem.

*   **Critical Showstoppers:** The service fails to compile out-of-the-box due to missing library dependencies in its [pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/pom.xml). Furthermore, the API Gateway is missing routing rules for the service, routing all payment-related client calls to the core macroservice instead.
*   **Architectural Disconnect:** The core macroservice (`bds-core-macroservice`) bypasses the financial service completely, using its own internal mock adapter (`PaywayPaymentGatewayAdapter`) to initialize payments and trigger payouts. On the webhook side, the financial service consumes events, updates its own database, but does not publish successful payment events to Kafka, leaving contract state transitions permanently blocked.
*   **Security Risks:** The PayPal webhook handler accepts and executes payment captured events without verifying cryptographic signatures, opening the system to payment spoofing.
*   **Contract Mismatch:** The service defines duplicate models, response wrappers, and enums locally instead of utilizing the shared contracts in `bds-common`.

---

## 🚫 Missing Features (Legacy vs. New)

The following features, logic, and models from the `/legacy` codebase are missing or stubbed out in `bds-financial-service`:

### 1. Missing Payout Tracking Entity
*   **File Location:** [WebhookProcessorService.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/gateway/payway/WebhookProcessorService.java#L41-L48)
*   **Description:** The legacy service managed outgoing payments (owner payouts, agent salary, agent commission). While `bds-financial-service` implements the API to call Payway/PayPal payouts, it has no `Payout` entity or repository to record and track these outbound transactions. The processor has a placeholder TODO:
    ```java
    } else if (event.getType() == Constants.PaymentGatewayEventType.PAYOUT_PAID) {
        log.info("Payout {} confirmed PAID via webhook", gatewayObjectId);
        // TODO: update payout tracking entity when added
    ```

### 2. Missing Modular Payment Success Side-Effects
*   **File Location:** Legacy side-effects folder [legacy/src/main/java/com/se100/bds/services/domains/payment/webhook/impl/](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/domains/payment/webhook/impl/)
*   **Description:** The monolith processed payment side-effects through dedicated handler classes (e.g., `DepositPaymentSucceededHandler`, `RentalSecurityDepositPaymentSucceededHandler`, `ServiceFeePaymentSucceededHandler`, `PurchasePaymentSucceededHandler`). The new service completely lacks these handlers, and the local `WebhookProcessorService` only updates the database status of the payment record, dropping all business triggers.

### 3. Stubbed PayOS Webhook Integration
*   **File Location:** [PaymentServiceImpl.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/services/impl/PaymentServiceImpl.java#L143-L151)
*   **Description:** PayOS webhook processing is left unimplemented as a stub:
    ```java
    @Override
    @Transactional
    public void handlePayOSWebhook(String rawBody) {
        // TODO: parse PayOS webhook payload
        // 1. Parse rawBody to extract payosPaymentId and status
        // 2. Find payment by payosPaymentId
        // 3. Update status accordingly
        log.info("Received PayOS webhook: {}", rawBody);
    }
    ```

---

## 🔗 Integration Gaps (Core Macroservice)

The integration touchpoints between `bds-financial-service` and the rest of the application are currently broken:

### 1. Local Payment and Payout Adapter Duplication
*   **Touchpoint:** [PaywayPaymentGatewayAdapter.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/adapter/out/external/PaywayPaymentGatewayAdapter.java#L62-L124) in `bds-core-macroservice`.
*   **Description:** In a decoupled microservices architecture, the core macroservice should call the financial service to create payment links and trigger payouts. Instead, `bds-core-macroservice` retains a duplicate `PaywayPaymentGatewayAdapter` that mocks session creation and payouts internally, completely bypassing `bds-financial-service`.

### 2. Missing Kafka Event Publisher (Unnotified Core Macroservice)
*   **Touchpoint:** [WebhookProcessorService.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/gateway/payway/WebhookProcessorService.java)
*   **Description:** `bds-core-macroservice` includes a [PaymentSucceededConsumer](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/adapter/in/messaging/PaymentSucceededConsumer.java#L18) listening on the `payment-succeeded` Kafka topic to activate contracts. However, the financial service lacks Kafka publishing capabilities and does not send any message after marking a payment as successful. Contracts remain stuck in `PENDING_PAYMENT` state indefinitely.

### 3. Missing API Gateway Routing Configuration
*   **Touchpoint:** [application.yaml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-api-gateway/src/main/resources/application.yaml#L25) in `bds-api-gateway`.
*   **Description:** The gateway has routing rules for `search-service` and `notification-service`, but lacks an entry for `bds-financial-service`. Any client traffic destined for `/api/payments/**` or `/api/commissions/**` will hit the catch-all `/**` rule and be misrouted to `bds-core-macroservice` on port `8081`, returning an HTTP 404 since those controllers have been removed from the core service.

---

## 📡 Inter-Service Communication Risks

### 1. Unverified Webhook Signature for PayPal
*   **Touchpoint:** [PayPalWebhookController.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/controllers/PayPalWebhookController.java#L21-L30)
*   **Description:** The controller accepts the raw JSON webhook event body from PayPal and immediately invokes the handling logic without verifying any cryptographic headers (such as `PAYPAL-TRANSMISSION-SIG` or `PAYPAL-CERT-URL`). A malicious client could send fake payment success notifications to mark transactions as completed without paying.

### 2. Missing Network Request Timeouts
*   **Touchpoint:** [PayPalService.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/gateway/paypal/PayPalService.java#L57-L68) and [PaywayService.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/gateway/payway/PaywayService.java#L44-L59)
*   **Description:** Both gateway clients build their `HttpClient` with no connect or read timeouts:
    ```java
    HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    ```
    If PayPal or Payway servers experience high response times or are unresponsive, connection threads in the financial service will hang indefinitely, leading to thread pool exhaustion and service crashes. There is also no circuit breaker (e.g. Resilience4j) configured.

### 3. Kafka Payload Coupling & Provider Mismatch
*   **Touchpoint:** [PaymentSucceededConsumer.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/adapter/in/messaging/PaymentSucceededConsumer.java#L18-L29)
*   **Description:** The consumer in the core macroservice is configured to expect the raw webhook payload structure of the Payway gateway on the `payment-succeeded` topic. This tightly couples the integration topic to a specific provider, which will break the consumer if a PayPal or PayOS success payload is published instead.

---

## 📜 Contract Standardization (bds-common)

`bds-financial-service` violates standard contract patterns in several areas:

### 1. Duplicated Envelope Structures (`SingleResponse` & `PageResponse`)
*   **File Locations:**
    *   [SingleResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/dtos/responses/SingleResponse.java)
    *   [PageResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/dtos/responses/PageResponse.java)
    *   [ResponseFactory.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/config/ResponseFactory.java)
    *   [ErrorResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/dtos/responses/error/ErrorResponse.java)
    *   [DetailedErrorResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/dtos/responses/error/DetailedErrorResponse.java)
*   **Description:** The service implements its own response and error structures containing `statusCode` (integer). This violates the shared standard [ApiResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common/src/main/java/com/se/bds/common/dto/ApiResponse.java) inside `/bds-common` (which uses a boolean `success` field).

### 2. Duplicated Enums (`PaymentType` & `PaymentStatus`)
*   **File Location:** [Constants.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/utils/Constants.java#L12-L47)
*   **Description:** `PaymentType` and `PaymentStatus` enums are defined locally. They duplicate and mismatch models declared in other modules (e.g. inside `bds-core-macroservice`'s domain model). These should be extracted to `bds-common` to ensure consistent serialization over the event broker.

### 3. Missing Exception Handling mapping for BusinessException
*   **File Location:** [AppExceptionHandler.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/exceptions/AppExceptionHandler.java)
*   **Description:** The exception advice has no handler mapping for `com.se.bds.common.exception.BusinessException`, which is the core business exception used by `/bds-common`.

---

## 🛠️ Actionable Remediation Steps

Below is the prioritized checklist to bring `bds-financial-service` to a buildable, secure, and production-ready status.

### Phase 1: Compile & Setup (Immediate Action Required)
- [ ] **Fix POM Dependency Issues:** Add missing dependencies for Spring Data JPA (`spring-boot-starter-data-jpa`), PostgreSQL driver (`postgresql`), and JWT validation library (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) to the financial service's [pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/pom.xml).
- [ ] **Configure Gateway Routes:** Add routing predicates for `/api/payments/**` and `/api/commissions/**` pointing to the financial service in the API Gateway's [application.yaml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-api-gateway/src/main/resources/application.yaml).
- [ ] **Validate Port Allocations:** Assign and expose a unique port for `bds-financial-service` (e.g. `8084`) and ensure the DB connection variables are loaded dynamically.

### Phase 2: Microservice Communication & Messaging
- [ ] **Configure Kafka in Financial Service:** Add `spring-kafka` dependency and configure Kafka producer properties in the service's `application.yml`.
- [ ] **Implement Payment-Succeeded Event Publisher:** Publish an event to Kafka topic `payment-succeeded` inside `WebhookProcessorService` when a payment is marked as `SUCCESS`.
- [ ] **Standardize Kafka Payload Contract:** Extract the topic payload from raw Payway webhook JSON into a standardized provider-agnostic class in `bds-common`. Refactor `PaymentSucceededConsumer` and `PaymentWebhookProcessor` inside `bds-core-macroservice` to consume this standard event.
- [ ] **Refactor Core Macroservice Delegations:** Remove local mock payment gateway adapter logic in `bds-core-macroservice` and replace it with HTTP/REST client calls targeting the API endpoints in `bds-financial-service` for payment and payout initialization.

### Phase 3: Security & Resiliency
- [ ] **Verify PayPal Webhook Signature:** Implement webhook signature verification in `PayPalWebhookController` or `PayPalService` using PayPal's authentication API to reject spoofed webhooks.
- [ ] **Set Network Request Timeouts:** Configure connect and read timeouts on the `HttpClient` instances used inside `PayPalService` and `PaywayService` to avoid hung connections.
- [ ] **Implement Circuit Breakers:** Add resilience wrappers (such as Resilience4j) to prevent cascading resource exhaustion during third-party gateway downtime.

### Phase 4: Contract & Feature Parity
- [ ] **Refactor Response Wrappers:** Delete `SingleResponse`, `PageResponse`, `ResponseFactory`, `ErrorResponse`, and `DetailedErrorResponse` in the financial service and refactor endpoints to use `com.se.bds.common.dto.ApiResponse` from `/bds-common`.
- [ ] **Centralize Shared Enums:** Move `PaymentType` and `PaymentStatus` enums to `bds-common` and delete local duplicates.
- [ ] **Implement Payout Tracking Schema:** Map a `Payout` database model to persist metadata and execution status for outbound bank transfers.
- [ ] **Complete PayOS Webhook:** Complete the implementation of the stubbed out `handlePayOSWebhook` method in `PaymentServiceImpl` to ensure PayOS compatibility.
- [ ] **Support BusinessException:** Map `BusinessException` inside `AppExceptionHandler` to yield standardized error structures.
