# Implementation Plan - `bds-financial-service` Gap Remediation

This document outlines the detailed technical design, roadmap, risk assessment, and verification plan for resolving all identified feature, integration, and contract gaps in the Financial & Payment Service (`bds-financial-service`).

## 1. Implementation Roadmap (Phased Execution)

---

### Phase A: Contract & DTO Standardization

#### [NEW] [PaymentType.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common/src/main/java/com/se/bds/common/enums/PaymentType.java)
- **Action Required:** Create the centralized, unified `PaymentType` enum containing all types from both services: `DEPOSIT`, `ADVANCE`, `INSTALLMENT`, `FULL_PAY`, `MONTHLY`, `PENALTY`, `MONEY_SALE`, `MONEY_RENTAL`, `SALARY`, `BONUS`, `SERVICE_FEE`, `PAYMENT_OVERDUE`, and `SECURITY_DEPOSIT`. Include a helper `get(String name)` method to resolve enums case-insensitively.
- **Dependencies:** Standard Java SE library.

#### [NEW] [PaymentStatus.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common/src/main/java/com/se/bds/common/enums/PaymentStatus.java)
- **Action Required:** Create the centralized, unified `PaymentStatus` enum combining all statuses: `PENDING`, `SUCCESS`, `FAILED`, `CANCELLED`, `REFUNDED`, `SYSTEM_PENDING`, `SYSTEM_SUCCESS`, and `SYSTEM_FAILED`. Include a helper `get(String name)` method.
- **Dependencies:** Standard Java SE library.

#### [NEW] [PaymentCompletedEvent.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common/src/main/java/com/se/bds/common/event/PaymentCompletedEvent.java)
- **Action Required:** Define a provider-agnostic, standardized event class to represent payment completion.
  - Fields: `paymentId` (UUID), `contractId` (UUID), `propertyId` (UUID), `paymentType` (String), `amount` (BigDecimal), `payerUserId` (UUID), `timestamp` (Instant).
- **Dependencies:** Standard Java SE, Lombok annotations.

#### [DELETE] Duplicated local enums
- **File Locations:**
  - Delete local `PaymentType` and `PaymentStatus` inside [Constants.java (bds-financial-service)](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/utils/Constants.java#L10-L47).
  - Delete local [PaymentType.java (bds-core-macroservice)](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/domain/model/PaymentType.java) and [PaymentStatus.java (bds-core-macroservice)](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/domain/model/PaymentStatus.java).
- **Action Required:** Remove local declarations and rewrite all import statements in both modules to reference enums from `com.se.bds.common.enums`.

#### [DELETE] Local Response Wrappers in `bds-financial-service`
- **File Locations:**
  - [SingleResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/dtos/responses/SingleResponse.java)
  - [PageResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/dtos/responses/PageResponse.java)
  - [ResponseFactory.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/config/ResponseFactory.java)
  - [ErrorResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/dtos/responses/error/ErrorResponse.java)
  - [DetailedErrorResponse.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/dtos/responses/error/DetailedErrorResponse.java)
- **Action Required:** Delete local wrappers. Refactor controllers, services, and base classes to use the unified `com.se.bds.common.dto.ApiResponse` from `bds-common` for single payloads, and introduce a standard generic page response wrapper if needed (like `PagedData<T>` in IAM service plan).

#### [MODIFY] [AppExceptionHandler.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/exceptions/AppExceptionHandler.java)
- **Action Required:** Add a `@ExceptionHandler` method mapping `com.se.bds.common.exception.BusinessException` to return a standardized `ApiResponse` with `success=false` and the exception's message.
- **Dependencies:** `com.se.bds.common.exception.BusinessException` import.

---

### Phase B: Business Logic Parity

#### [NEW] [Payout.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/entities/Payout.java)
- **Action Required:** Create the payout tracking entity mapping outbound transactions to the database.
  - Fields: `id` (UUID, Primary Key), `amount` (BigDecimal), `currency` (String), `accountNumber` (String), `accountHolderName` (String), `swiftCode` (String), `description` (String), `status` (String/Enum - e.g. PENDING, PAID, FAILED), `gatewayPayoutId` (String, Unique), `errorMessage` (String), `createdAt` (LocalDateTime), `updatedAt` (LocalDateTime).
- **Dependencies:** JPA annotations, Lombok annotations.

#### [NEW] [PayoutRepository.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/repositories/PayoutRepository.java)
- **Action Required:** Define the database repository interface for payout persistence.
  - Query: `Optional<Payout> findByGatewayPayoutId(String gatewayPayoutId)`.
- **Dependencies:** Spring Data JPA interface.

#### [MODIFY] [WebhookProcessorService.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/gateway/payway/WebhookProcessorService.java)
- **Action Required:**
  - Inject `PayoutRepository`.
  - In `process(...)` payout branches:
    - For `PAYOUT_PAID`: Look up the `Payout` entity by `gatewayObjectId` and update its status to `PAID`.
    - For `PAYOUT_FAILED`: Look up the `Payout` entity and update its status to `FAILED`, storing `event.getError()` in the `errorMessage` column.
- **Dependencies:** `PayoutRepository` injection.

#### [MODIFY] [PaymentServiceImpl.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/services/impl/PaymentServiceImpl.java)
- **Action Required:** Complete the stubbed `handlePayOSWebhook(String rawBody)`.
  - Parse `rawBody` using `ObjectMapper` to extract the PayOS payment link ID, transaction status, and signature.
  - Retrieve the payment record using `paymentRepository.findByPayosPaymentId(payosPaymentId)`.
  - Update payment status to `SUCCESS` or `FAILED` based on PayOS status.
  - Securely verify PayOS signature if validation keys are configured.
- **Dependencies:** `ObjectMapper`, `paymentRepository`.

#### [MODIFY] [PaymentSucceededConsumer.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/adapter/in/messaging/PaymentSucceededConsumer.java)
- **Action Required:** Refactor the consumer to deserialize the Kafka payload as a standardized `PaymentCompletedEvent` (from `bds-common`) instead of a raw Payway provider-specific string.
- **Dependencies:** `com.se.bds.common.event.PaymentCompletedEvent`.

#### [MODIFY] [PaymentWebhookProcessor.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/application/service/PaymentWebhookProcessor.java)
- **Action Required:** Refactor the processor class to accept `PaymentCompletedEvent`.
  - Remove all raw JSON parsing logic, signature verification, and HTTP header checking (as signature verification is now handled by `bds-financial-service` at the boundary webhook controllers).
  - Execute core business side-effects:
    - Update core service's local payment record status to `SUCCESS`.
    - Publish core internal events (`PaymentCompletedEvent` and `ContractStatusChangedEvent`).
    - Transition contract status to `ACTIVE` (for `PENDING_PAYMENT` state contracts) and update `signedAt` dates.
- **Dependencies:** `PaymentCompletedEvent` payload contract.

---

### Phase C: Integration & Communication

#### [MODIFY] [pom.xml (bds-financial-service)](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/pom.xml)
- **Action Required:** Add missing essential packages:
  - Spring Data JPA (`spring-boot-starter-data-jpa`)
  - PostgreSQL Driver (`postgresql`)
  - Spring Kafka (`spring-kafka`)
  - JWT API and implementation (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
  - Resilience4j Spring Boot starter (`resilience4j-spring-boot3` or similar circuit breaker dependency)
- **Dependencies:** None.

#### [MODIFY] [application.yml (bds-financial-service)](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/resources/application.yml)
- **Action Required:**
  - Assign default port `8084` to `server.port`.
  - Add Spring Kafka configurations (`spring.kafka.bootstrap-servers`, `key-serializer`, `value-serializer`).
  - Configure default properties for Spring Datasource pointing to PostgreSQL.
- **Dependencies:** Kafka and Postgres dependencies in POM.

#### [MODIFY] [WebhookProcessorService.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/gateway/payway/WebhookProcessorService.java)
- **Action Required:**
  - Inject Spring `KafkaTemplate<String, Object>`.
  - Upon successful payment transition (`Constants.PaymentGatewayEventType.PAYMENT_SUCCEEDED`):
    - Retrieve payment from repository.
    - Build a standardized `PaymentCompletedEvent`.
    - Publish `PaymentCompletedEvent` to `payment-succeeded` Kafka topic.
- **Dependencies:** `KafkaTemplate` injection.

#### [MODIFY] [PaywayPaymentGatewayAdapter.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/adapter/out/external/PaywayPaymentGatewayAdapter.java)
- **Action Required:** Refactor the adapter class to replace the local mocked payment session and payout session logic with RestClient/RestTemplate calls targeting `bds-financial-service` endpoints (`POST /api/payments` and a payout endpoint).
- **Dependencies:** RestTemplate / RestClient builder.

#### [MODIFY] [application.yaml (bds-api-gateway)](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-api-gateway/src/main/resources/application.yaml)
- **Action Required:**
  - Add API route predicates mapping path prefixes `/api/payments/**` and `/api/commissions/**` targeting the Financial Service URL `${FINANCIAL_SERVICE_URL:http://localhost:8084}`.
  - Position this route BEFORE the catch-all `core-service` route (`/**`) to prevent routing conflicts.
- **Dependencies:** Gateway routes configuration.

#### [MODIFY] [PayPalWebhookController.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/controllers/PayPalWebhookController.java)
- **Action Required:** Implement signature verification before processing webhook payloads.
  - Collect PayPal authentication transmission headers (`PAYPAL-TRANSMISSION-ID`, `PAYPAL-TRANSMISSION-SIG`, `PAYPAL-TRANSMISSION-TIME`, `PAYPAL-CERT-URL`, `PAYPAL-AUTH-ALGO`).
  - Invoke PayPal's verification API endpoint `/v1/notifications/verify-webhook-signature` using a synchronous HTTP call to verify signature authenticity. Reject payload with HTTP 401 if validation fails.
- **Dependencies:** PayPal client headers.

#### [MODIFY] [PayPalService.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/gateway/paypal/PayPalService.java) & [PaywayService.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-financial-service/src/main/java/com/se361/financial_service/gateway/payway/PaywayService.java)
- **Action Required:**
  - Configure HttpClient build logic with connect and read timeouts (e.g. `connectTimeout(Duration.ofSeconds(3))` and `timeout(Duration.ofSeconds(5))` for HTTP requests).
  - Add Resilience4j circuit breakers around external RestClient calls to prevent service threads hanging during PayPal or Payway outage.
- **Dependencies:** Resilience4j configuration.

---

## 2. Risk Assessment & Edge Cases

- **Double Webhook Delivery & Event Out-of-Order (Idempotency):**
  - Webhook gateways (PayPal, Payway) may deliver duplicate success notifications. Additionally, Kafka brokers could deliver messages multiple times due to retry policies.
  - *Mitigation:* 
    - Implement a `ProcessedWebhookEvent` entity and repository in `bds-financial-service` to track and prevent duplicate processing of same webhook IDs.
    - Leverage unique databases keys and transactions to ensure idempotent state updates.
- **VND to USD Conversion on PayPal:**
  - PayPal does not support Vietnam Dong (VND). Monolith payments are tracked in VND.
  - *Mitigation:* The adapter service must support programmatic conversion or mapping when initiating PayPal orders, ensuring correct values are authorized and registered.
- **Kafka Outage Recovery:**
  - If Kafka goes offline, webhook confirmations will succeed in marking payments as PAID in the financial database, but the Core macroservice will never receive the `PaymentCompletedEvent`, leaving contracts permanently locked in `PENDING_PAYMENT`.
  - *Mitigation:* Maintain a retry publisher mechanism or transactional outbox pattern to ensure events are securely buffered and eventually delivered to the Kafka broker when it comes back online.

---

## 3. Comprehensive Verification Plan

### Unit Testing Strategy
- **Centralized Enums:**
  - Verify that `PaymentType` and `PaymentStatus` can correctly parse legacy names case-insensitively using `get(String name)`.
- **Payout Tracking:**
  - Create `PayoutRepositoryTests` validating persistence, retrieval, and status updates of payout entities.
- **PayOS Payload Parsing:**
  - Write test cases in `PaymentServiceImplTests` asserting that sample PayOS raw webhook bodies are parsed correctly and mapped to target payments.

### Integration Testing
- **End-to-End Payment Event Pipeline:**
  - Spin up the `bds-api-gateway`, `bds-financial-service` and `bds-core-macroservice` in test containers.
  - Trigger a payment initialization via Gateway, capture the checkout URL, and execute a simulated webhook payload target.
  - Assert that the financial database state switches to `SUCCESS`, the Kafka event is dispatched, the core macroservice consumes the event, and the contract status shifts from `PENDING_PAYMENT` to `ACTIVE`.
- **Signature Verification Rejection:**
  - Send fake PayPal webhook events with incorrect cryptographic signature headers. Assert that `PayPalWebhookController` rejects them with HTTP 401 Unauthorized.

### Contract Validation
- **Middleware API Assertions:**
  - Verify that endpoints validate request bodies using `@Valid` annotation and return standardized `ApiResponse` errors.
- **Event Contracts:**
  - Run schema validation on the Kafka `payment-succeeded` channel to ensure published messages conform exactly to `com.se.bds.common.event.PaymentCompletedEvent`.
