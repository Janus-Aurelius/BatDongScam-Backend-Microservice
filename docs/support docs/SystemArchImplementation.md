# Architectural Tactics Implementation Plan: BatDongSan Core Macroservice — Feature Wave 2

## Executive Summary

This plan covers **7 user stories** assigned to Thien An Nguyen for the `bds-core-macroservice`:

| ID | Feature | Current State |
|:---|:---|:---|
| **US-008** | Draft Rental Contract (PDF + Cloudinary) | Partially Implemented — missing AC3 |
| **US-009** | Deposit Contract Execution | ✅ Fully Implemented — lifecycle DRAFT→WAITING_OFFICIAL complete |
| **US-010** | Initialize Payment | 🔴 Not Started — redirect logic & provider integration needed |
| **US-011** | Handle Payment Webhook | Partially Implemented — controller & signature missing in core |
| **US-016** | Generate Sales Report (CSV/Excel) | Partially Implemented — export logic missing |
| **US-028** | Escrow Protection | 🔴 Not Started — multi-stage payment hold logic |
| **US-030** | Agent Reviews | 🔴 Not Started — rating API and persistence |

### Core Architectural Challenges

1. **External Service Integration Risk** — PDF generation, Cloudinary upload, and Payway payment gateway are external dependencies prone to failure.
2. **Financial Integrity** — Payment initialization (US-010), webhook processing (US-011), and escrow (US-028) handle real money — requiring idempotency, transactional safety, and cryptographic verification.
3. **Contract State Machine Complexity** — The DRAFT → WAITING_OFFICIAL → PENDING_PAYMENT → ACTIVE → COMPLETED lifecycle spans multiple features and must be bulletproof.
4. **Hexagonal Architecture Compliance** — The macroservice uses a strict ports-and-adapters pattern (`api/` facade → `internal/application/port/` → `internal/adapter/`). All new features must adhere to this structure.

### Codebase Architecture (Reference)

```
bds-core-macroservice/src/main/java/com/se/bds/core/
├── property/           # Property bounded context
│   ├── api/            # PropertyFacade (public contract)
│   └── internal/       # adapters (web, persistence, messaging) + domain + application
├── transaction/        # Transaction bounded context (contracts, payments)
│   ├── api/            # TransactionFacade + events + shared DTOs
│   └── internal/       # adapters + domain + application
└── shared/             # Cross-module DTOs, IDs, enums
```

Legacy reference: `legacy/src/main/java/com/se100/bds/` — business logic source of truth.

---

## 1. Availability

### System Level

*   **Tactic: Retry (Recover from Faults)**
*   **Implementation:** Create a shared `@Retryable` configuration in the `bds-common` module using Spring Retry. Configure exponential backoff for all outbound adapter calls (Cloudinary, Payway API). Apply to:
    - [CloudinaryUploadAdapter](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/fileupload/impl/CloudinaryServiceImpl.java) — port for US-008 PDF upload
    - PaymentGateway outbound port — for US-010 `createPaymentSession()` calls
    - Default: 3 retries with 500ms → 1s → 2s backoff, circuit-break after 5 consecutive failures.

*   **Tactic: Exception Handling (Detect Faults)**
*   **Implementation:** Centralize exception handling in a `TransactionGlobalExceptionHandler` (extending the existing [GlobalExceptionHandler](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/property/internal/adapter/in/web/GlobalExceptionHandler.java) pattern) to catch and translate:
    - `PaywayApiException` hierarchy → appropriate HTTP 502/503 responses
    - `CloudinaryUploadException` → HTTP 503 with retry-after header
    - `IllegalStateException` from contract state machine → HTTP 409 Conflict

*   **Tactic: Transactions (Prevent Faults)**
*   **Implementation:** All contract state mutations MUST use Spring `@Transactional` (already in place for [DepositContractServiceImpl](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/application/service/DepositContractServiceImpl.java) and [RentalContractServiceImpl](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/application/service/RentalContractServiceImpl.java)). Extend this to:
    - US-028 `EscrowService` — hold/release must be atomic
    - US-030 `AgentReviewService` — rating + comment save must be atomic
    - US-011 webhook processing — payment status update + side-effect dispatch in single transaction

### Feature Level

*   **Tactic: Degradation (Recover from Faults) — US-008**
*   **Implementation:** If Cloudinary upload fails after retries for PDF contract, the system should still complete the rental contract draft but mark the `pdfUrl` as `PENDING_UPLOAD`. A background scheduled task re-attempts upload. The user receives the contract data immediately with a `pdfStatus: PENDING` indicator.

*   **Tactic: Sanity Checking (Detect Faults) — US-009**
*   **Implementation:** Before every contract status transition in [Contract.transitionTo()](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/domain/model/Contract.java#L149-L161), validate:
    - `DRAFT → WAITING_OFFICIAL`: all required fields populated (propertyId, customerId, amounts)
    - `WAITING_OFFICIAL → PENDING_PAYMENT`: signed paperwork exists
    - `PENDING_PAYMENT → ACTIVE`: at least first payment marked SUCCESS
    - Implement as a `ContractTransitionValidator` strategy per contract type.

*   **Tactic: Ignore Faulty Behavior (Recover from Faults) — US-011**
*   **Implementation:** The webhook controller must ALWAYS return HTTP 200 to Payway (matching the legacy [PaywayWebhookController](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/controllers/external/PaywayWebhookController.java) pattern). Internal processing failures are logged and queued for manual review but never surface as webhook errors to the payment provider.

*   **Tactic: Ping/Echo (Detect Faults) — US-010**
*   **Implementation:** Before redirecting users to the Payway checkout URL, issue a lightweight health-check GET to the Payway `/api/health` endpoint. If the gateway is down, fail fast with a user-friendly error rather than creating a dangling payment record.

---

## 2. Security

### System Level

*   **Tactic: Authenticate Actors (Resist Attacks)**
*   **Implementation:** All new endpoints MUST use Spring Security's `@PreAuthorize` annotations (already established in [RentalContractController](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/adapter/in/web/RentalContractController.java#L24)). JWT validation occurs via the existing security filter chain. Required roles per feature:

    | Endpoint | Roles |
    |:---|:---|
    | POST `/contracts/rental/{id}/generate-pdf` (US-008) | `ADMIN`, `SALEAGENT` |
    | POST `/payments/initialize` (US-010) | `CUSTOMER` |
    | POST `/webhooks/payway` (US-011) | **No auth** (signature-verified) |
    | GET `/reports/sales/export` (US-016) | `ADMIN` |
    | POST `/escrow/{id}/hold`, `/release` (US-028) | `ADMIN` |
    | POST `/reviews/agent` (US-030) | `CUSTOMER` |

*   **Tactic: Authorize Actors (Resist Attacks)**
*   **Implementation:** Beyond role checks, implement resource-level authorization:
    - US-008: Only the assigned agent or admin can generate PDF for a contract
    - US-010: Only the contract's customer can initialize payment for their own contract
    - US-030: Only customers who have a COMPLETED or ACTIVE contract with the agent can submit reviews
    - Reuse the `checkReadAccess()`/`checkWriteAccess()` patterns from the legacy [RentalContractServiceImpl](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/domains/contract/impl/RentalContractServiceImpl.java#L697-L730)

*   **Tactic: Maintain Audit Trail (React & Recover)**
*   **Implementation:** Extend the existing [PropertyLoggingAspect](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/property/internal/adapter/in/web/aspect/PropertyLoggingAspect.java) pattern. Create a `TransactionLoggingAspect` for all transaction controllers, capturing OpenTelemetry traceId, authenticated user (masked), and operation type. Additionally, persist financial audit records to a dedicated `audit_log` table for escrow and payment operations.

### Feature Level

*   **Tactic: Verify Message Integrity (Detect Attacks) — US-011**
*   **Implementation:** Port the legacy [PaywayWebhookSignatureVerifier](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/payment/payway/PaywayWebhookSignatureVerifier.java) into `bds-core-macroservice`. Key requirements:
    - HMAC-SHA256 with `payway.verify-key` from environment
    - Constant-time comparison (already implemented in legacy)
    - `X-Signature: sha256=<hex>` header parsing
    - Reject with HTTP 401 if verification fails
    - Move the verify key to environment variables, never hardcoded

*   **Tactic: Encrypt Data (Resist Attacks) — US-028**
*   **Implementation:** All escrow amounts and bank account details must be encrypted at rest:
    - Use JPA `@Convert` with an AES-256 `AttributeConverter` for `bankAccountNumber` fields
    - Escrow ledger entries store only references to payment IDs, not raw financial data
    - Payway API key stored via Spring's `@Value("${payway.api-key}")` backed by environment variables (already done in legacy [PaywayService](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/payment/payway/PaywayService.java#L38-L39))

*   **Tactic: Limit Exposure (Resist Attacks) — US-016**
*   **Implementation:** CSV/Excel report exports must:
    - Exclude PII (customer phone, email, bank details) from exported data
    - Include only aggregated financial figures, property IDs, and anonymized identifiers
    - Apply rate limiting: max 5 export requests per admin per hour

*   **Tactic: Separate Entities (Resist Attacks) — US-011**
*   **Implementation:** The webhook endpoint (`/webhooks/payway`) must bypass the standard JWT authentication filter chain entirely. It lives in a separate security filter group (`WebSecurityConfig`) that only requires signature verification, not bearer tokens. This mirrors the legacy approach at [PaywayWebhookController](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/controllers/external/PaywayWebhookController.java).

---

## 3. Performance

### System Level

*   **Tactic: Introduce Concurrency (Manage Resources)**
*   **Implementation:** Configure a dedicated `@Async` thread pool for external I/O operations in the core-macroservice (extending the existing [AsyncConfig](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/property/internal/application/config/AsyncConfig.java)):
    - `pdfGenerationExecutor`: pool 2–4 threads (PDF gen is CPU-bound)
    - `externalUploadExecutor`: pool 4–8 threads (Cloudinary/Payway I/O-bound)
    - `reportExportExecutor`: pool 2 threads (batch processing, memory-heavy)

*   **Tactic: Bound Queue Sizes (Manage Resources)**
*   **Implementation:** Cap async task queues to prevent OOM under load:
    - PDF generation queue: max 50 pending tasks, reject with `CallerRunsPolicy`
    - Report export queue: max 10 pending tasks, reject with HTTP 429

### Feature Level

*   **Tactic: Maintain Multiple Copies of Data / Caching (Manage Resources) — US-016**
*   **Implementation:** Report data (financial stats, agent performance) is already pre-aggregated into MongoDB via schedulers (see legacy [ReportServiceImpl](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/domains/report/impl/ReportServiceImpl.java)). For CSV/Excel export:
    - Read from pre-computed MongoDB report documents, NOT live PostgreSQL queries
    - Cache the generated file in a temporary S3/Cloudinary bucket with 1-hour TTL
    - Return a download URL instead of streaming the file directly

*   **Tactic: Reduce Overhead (Control Resource Demand) — US-008**
*   **Implementation:** For PDF generation:
    - Generate PDFs using an in-memory template engine (e.g., OpenPDF/iText) rather than spawning external processes
    - Stream the PDF bytes directly to Cloudinary upload API without writing to disk
    - Use `byte[]` pipeline: template → render → upload, minimizing I/O hops

*   **Tactic: Limit Event Response (Control Resource Demand) — US-011**
*   **Implementation:** Implement webhook deduplication using the `externalEventId` from Payway events:
    - Before processing, check if `externalEventId` has been processed (store in `processed_webhook_events` table with 7-day TTL)
    - This is critical for idempotency — Payway may retry webhook delivery
    - The legacy [PaymentGatewayWebhookProcessorImpl](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/domains/payment/webhook/impl/PaymentGatewayWebhookProcessorImpl.java#L64-L68) already has idempotency for payment status but NOT for event-level deduplication

*   **Tactic: Increase Resource Efficiency (Control Resource Demand) — US-028**
*   **Implementation:** Escrow balance tracking should use optimistic locking (`@Version` on the escrow entity) rather than pessimistic DB locks. Escrow state transitions are rare relative to reads, making optimistic locking ideal.

---

## 4. Maintainability

### System Level

*   **Tactic: Encapsulate / Use an Intermediary (Reduce Coupling)**
*   **Implementation:** All new features MUST interact across bounded contexts exclusively through the existing Facade interfaces:
    - [PropertyFacade](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/property/api/PropertyFacade.java) — transaction module calls for property snapshots
    - [TransactionFacade](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/api/TransactionFacade.java) — property module calls for contract history
    - **New:** Introduce a `PaymentGatewayPort` outbound port interface in `transaction/internal/application/port/out/` to abstract the Payway provider. This allows swapping payment providers without modifying business logic.

*   **Tactic: Split Modules / Single Responsibility (Reduce Module Size)**
*   **Implementation:** The `transaction` module is growing large. Introduce sub-packages:
    ```
    transaction/internal/
    ├── application/
    │   ├── service/
    │   │   ├── DepositContractServiceImpl.java     (existing)
    │   │   ├── RentalContractServiceImpl.java      (existing)
    │   │   ├── PaymentInitializationService.java   (NEW — US-010)
    │   │   ├── EscrowService.java                  (NEW — US-028)
    │   │   └── AgentReviewService.java             (NEW — US-030)
    │   └── port/
    │       ├── in/
    │       │   ├── PaymentInitializationUseCase.java  (NEW)
    │       │   ├── EscrowUseCase.java                 (NEW)
    │       │   └── AgentReviewUseCase.java            (NEW)
    │       └── out/
    │           ├── PaymentGatewayPort.java             (NEW — abstracts Payway)
    │           ├── FileStoragePort.java                (NEW — abstracts Cloudinary)
    │           └── PdfGeneratorPort.java               (NEW — abstracts PDF engine)
    ├── adapter/
    │   ├── in/web/
    │   │   ├── PaymentWebhookController.java       (NEW — US-011)
    │   │   ├── PaymentInitializationController.java (NEW — US-010)
    │   │   ├── ReportExportController.java          (NEW — US-016)
    │   │   ├── EscrowController.java                (NEW — US-028)
    │   │   └── AgentReviewController.java           (NEW — US-030)
    │   └── out/
    │       ├── external/
    │       │   ├── PaywayPaymentGatewayAdapter.java (NEW — implements PaymentGatewayPort)
    │       │   ├── CloudinaryFileStorageAdapter.java (NEW — implements FileStoragePort)
    │       │   └── OpenPdfGeneratorAdapter.java      (NEW — implements PdfGeneratorPort)
    │       └── persistence/
    │           ├── EscrowRepositoryAdapter.java      (NEW)
    │           └── AgentReviewRepositoryAdapter.java  (NEW)
    └── domain/model/
        ├── EscrowHold.java                           (NEW — US-028)
        ├── EscrowStatus.java                         (NEW)
        └── AgentReview.java                          (NEW — US-030)
    ```

*   **Tactic: Abstract Common Services (Reduce Coupling)**
*   **Implementation:** Extract common patterns seen in the legacy code into reusable abstractions:
    - `ContractPaymentFactory` — centralize the `createContractPayment()` logic currently in the legacy [RentalContractServiceImpl.createContractPayment()](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/domains/contract/impl/RentalContractServiceImpl.java#L660-L695). Reuse across US-010, US-028.
    - `PayoutTrigger` — centralize the payout logic (currently `triggerPayoutToOwner()` / `triggerPayoutToCustomer()`) for reuse in escrow release.

### Feature Level

*   **Tactic: Mocking / Abstract Data Sources (Testability) — US-011**
*   **Implementation:** The `PaymentGatewayPort` interface enables unit testing webhook processing without a live Payway connection. Create:
    - `MockPaymentGatewayAdapter` for integration tests
    - `PaymentWebhookProcessorTest` with mock events covering: `PAYMENT_SUCCEEDED`, `PAYMENT_CANCELED`, duplicate event, malformed body, invalid signature

*   **Tactic: Defer Binding (Flexibility) — US-010, US-028**
*   **Implementation:** Use Spring profiles to switch payment gateway implementations:
    - `@Profile("production")` → `PaywayPaymentGatewayAdapter`
    - `@Profile("test")` → `MockPaymentGatewayAdapter`
    - `@Profile("staging")` → `PaywaySandboxAdapter` (sandbox API keys)

*   **Tactic: Increase Semantic Coherence (Increase Cohesion) — US-030**
*   **Implementation:** Agent reviews belong semantically with the `Contract` aggregate (the legacy [Contract.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/models/entities/contract/Contract.java#L93-L99) already has `rating` and `comment` fields). However, for the core macroservice, create a separate `AgentReview` entity to support multiple reviews per agent (across different contracts), avg rating aggregation, and review moderation — keeping review concerns decoupled from contract lifecycle.

---

## 5. Logging Strategy

### Accounts Level

*   **Implementation:**
    - **US-010 (Initialize Payment):** Log `[ACCOUNTS] userId={masked} initiated payment for contractId={id}, amount={amount}, paymentType={type}`. Capture the authenticated principal from `SecurityContextHolder`.
    - **US-011 (Webhook):** Log `[ACCOUNTS] Webhook received from IP={remoteAddr}, signatureValid={true/false}, externalEventId={id}`. No user auth context (webhook is machine-to-machine).
    - **US-028 (Escrow):** Log `[ACCOUNTS] admin={masked} executed escrow action={HOLD|RELEASE|FORFEIT} on contractId={id}, amount={amount}`. This is a high-security action requiring admin role.
    - **US-030 (Reviews):** Log `[ACCOUNTS] customerId={masked} submitted review for agentId={masked} via contractId={id}`.
    - **US-016 (Reports):** Log `[ACCOUNTS] adminId={masked} exported report type={SALES}, format={CSV|EXCEL}, dateRange={from}-{to}`.
    - All account-level logs include the OpenTelemetry `traceId` for correlation, following the pattern in [PropertyLoggingAspect](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/property/internal/adapter/in/web/aspect/PropertyLoggingAspect.java#L19).

### Events Level

*   **Implementation:**
    - **US-008 (PDF Generation):**
        - `[EVENT] Contract PDF generated: contractId={id}, sizeBytes={size}, durationMs={ms}`
        - `[EVENT] Contract PDF uploaded to Cloudinary: contractId={id}, pdfUrl={url}`
        - `[EVENT] Contract PDF upload FAILED: contractId={id}, attempt={n}, error={msg}` → triggers degradation path
    - **US-009 (Deposit Contract Lifecycle):**
        - `[EVENT] Contract status changed: contractId={id}, type=DEPOSIT, from={old} to={new}` — already implemented via [ContractStatusChangedEvent](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/api/event/ContractStatusChangedEvent.java) and Spring's `ApplicationEventPublisher`
    - **US-010 (Payment Initialization):**
        - `[EVENT] Payment session created: paymentId={id}, paywayPaymentId={extId}, checkoutUrl={url}`
        - `[EVENT] Payment session creation FAILED: contractId={id}, error={msg}`
    - **US-011 (Webhook Processing):**
        - `[EVENT] Webhook processed: externalEventId={id}, type={PAYMENT_SUCCEEDED|CANCELED}, paymentId={id}`
        - `[EVENT] Webhook DUPLICATE ignored: externalEventId={id}` (idempotency guard)
        - `[EVENT] Payment status updated: paymentId={id}, from={old} to={new}`
        - `[EVENT] Side-effect triggered: handler={DepositPaymentSucceededHandler}, paymentId={id}`
    - **US-028 (Escrow):**
        - `[EVENT] Escrow hold created: escrowId={id}, contractId={cid}, amount={amt}, stage={SECURITY_DEPOSIT|RENTAL}`
        - `[EVENT] Escrow released: escrowId={id}, to={OWNER|CUSTOMER}, payoutId={pid}`
        - `[EVENT] Escrow forfeited: escrowId={id}, reason={reason}`
    - **US-030 (Agent Reviews):**
        - `[EVENT] Agent review submitted: reviewId={id}, agentId={aid}, rating={1-5}`
        - `[EVENT] Agent average rating recalculated: agentId={aid}, newAvg={avg}, totalReviews={n}`

### Methods Level

*   **Implementation:**
    - Apply AOP-based method tracing to all `*ServiceImpl` classes in the transaction module via a `TransactionMethodLoggingAspect`:
        ```
        [METHOD] ENTER DepositContractServiceImpl.createDepositContract(propertyId={id}, customerId={id})
        [METHOD] EXIT  DepositContractServiceImpl.createDepositContract → contractId={id}, durationMs={ms}
        ```
    - **Parameter sanitization rules:** Never log `amount`, `bankAccountNumber`, `apiKey`, `signature`, or `verifyKey` values. Log only:
        - Entity IDs (contractId, paymentId, propertyId)
        - Status values and enum types
        - Timing measurements
    - **Exception tracing:**
        ```
        [METHOD] EXCEPTION in PaymentWebhookProcessor.process(): IllegalStateException: "No Payment found for paywayPaymentId=xxx"
        ```
    - **Performance thresholds:** If any method exceeds 5000ms, log at WARN level:
        ```
        [METHOD] SLOW PaywayPaymentGatewayAdapter.createPaymentSession() took 6234ms (threshold: 5000ms)
        ```
    - PDF generation and report export methods should log progress checkpoints:
        ```
        [METHOD] PdfGeneratorAdapter.generate(): template loaded (120ms), data bound (45ms), rendered (890ms), total=1055ms
        ```

---

## Open Questions

> [!IMPORTANT]
> **US-008 PDF Engine Selection:** The legacy codebase has no PDF generation library. Should we add `OpenPDF` (LGPL, free) or `iText 7` (AGPL, commercial license for closed-source)? OpenPDF is recommended for its license compatibility.

> [!IMPORTANT]
> **US-028 Escrow Scope:** The legacy `SecurityDepositStatus` enum (`NOT_PAID → HELD → RETURNED_TO_CUSTOMER / TRANSFERRED_TO_OWNER`) exists only on `RentalContract`. Should US-028 "Escrow Protection" extend this to cover deposit contracts and purchase contracts as well, or is it rental-only?

> [!WARNING]
> **US-010 Redirect Strategy:** The legacy [PaywayService](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/payment/payway/PaywayService.java) returns a `checkoutUrl` from Payway. Should the Initialize Payment API return this URL as JSON for the frontend to handle, or should the backend issue an HTTP 302 redirect? JSON response is recommended for SPA frontends.

> [!NOTE]
> **US-030 Review Entity Design:** The legacy [Contract.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/models/entities/contract/Contract.java#L93-L99) embeds `rating` and `comment` directly. Should the core macroservice maintain this 1:1 contract-rating relationship, or create a separate `agent_review` table allowing multiple reviews per agent (one per contract)? A separate table is recommended for better aggregation.

---

## Next Steps & Code Impact

### Files to Create (New)

| File | Module | Feature |
|:---|:---|:---|
| `PaymentGatewayPort.java` | `transaction/internal/application/port/out/` | US-010, US-011, US-028 |
| `FileStoragePort.java` | `transaction/internal/application/port/out/` | US-008 |
| `PdfGeneratorPort.java` | `transaction/internal/application/port/out/` | US-008 |
| `PaywayPaymentGatewayAdapter.java` | `transaction/internal/adapter/out/external/` | US-010, US-011 |
| `CloudinaryFileStorageAdapter.java` | `transaction/internal/adapter/out/external/` | US-008 |
| `OpenPdfGeneratorAdapter.java` | `transaction/internal/adapter/out/external/` | US-008 |
| `PaymentWebhookController.java` | `transaction/internal/adapter/in/web/` | US-011 |
| `PaymentWebhookSignatureVerifier.java` | `transaction/internal/adapter/in/web/` | US-011 |
| `PaymentInitializationController.java` | `transaction/internal/adapter/in/web/` | US-010 |
| `PaymentInitializationUseCase.java` | `transaction/internal/application/port/in/` | US-010 |
| `PaymentInitializationService.java` | `transaction/internal/application/service/` | US-010 |
| `PaymentWebhookProcessor.java` | `transaction/internal/application/service/` | US-011 |
| `PaymentWebhookUseCase.java` | `transaction/internal/application/port/in/` | US-011 |
| `ReportExportController.java` | `transaction/internal/adapter/in/web/` | US-016 |
| `ReportExportUseCase.java` | `transaction/internal/application/port/in/` | US-016 |
| `ReportExportService.java` | `transaction/internal/application/service/` | US-016 |
| `EscrowController.java` | `transaction/internal/adapter/in/web/` | US-028 |
| `EscrowUseCase.java` | `transaction/internal/application/port/in/` | US-028 |
| `EscrowService.java` | `transaction/internal/application/service/` | US-028 |
| `EscrowHold.java` | `transaction/internal/domain/model/` | US-028 |
| `EscrowStatus.java` | `transaction/internal/domain/model/` | US-028 |
| `EscrowRepository.java` | `transaction/internal/application/port/out/` | US-028 |
| `EscrowRepositoryAdapter.java` | `transaction/internal/adapter/out/persistence/` | US-028 |
| `JpaEscrowRepository.java` | `transaction/internal/adapter/out/persistence/` | US-028 |
| `AgentReviewController.java` | `transaction/internal/adapter/in/web/` | US-030 |
| `AgentReviewUseCase.java` | `transaction/internal/application/port/in/` | US-030 |
| `AgentReviewService.java` | `transaction/internal/application/service/` | US-030 |
| `AgentReview.java` | `transaction/internal/domain/model/` | US-030 |
| `AgentReviewRepository.java` | `transaction/internal/application/port/out/` | US-030 |
| `AgentReviewRepositoryAdapter.java` | `transaction/internal/adapter/out/persistence/` | US-030 |
| `JpaAgentReviewRepository.java` | `transaction/internal/adapter/out/persistence/` | US-030 |
| `TransactionLoggingAspect.java` | `transaction/internal/adapter/in/web/aspect/` | All features |
| `ProcessedWebhookEvent.java` | `transaction/internal/domain/model/` | US-011 |
| `ContractPdfTemplate.java` | `transaction/internal/domain/model/` | US-008 |

### Files to Modify (Existing)

| File | Changes |
|:---|:---|
| [RentalContractController.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/adapter/in/web/RentalContractController.java) | Add `POST /{id}/generate-pdf` endpoint (US-008) |
| [RentalContractUseCase.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/application/port/in/RentalContractUseCase.java) | Add `generateContractPdf(UUID contractId)` method (US-008) |
| [RentalContractServiceImpl.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/application/service/RentalContractServiceImpl.java) | Add PDF generation orchestration, implement `decideSecurityDeposit` (US-008, US-028) |
| [RentalContract.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/domain/model/RentalContract.java) | Add `pdfUrl`, `pdfStatus` fields (US-008) |
| [Contract.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/domain/model/Contract.java) | Enhanced `transitionTo()` with pre-condition validation (US-009) |
| [TransactionFacade.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/transaction/api/TransactionFacade.java) | Add `getAgentReviewSummary(UUID agentId)` (US-030) |
| [pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/pom.xml) | Add OpenPDF, Apache POI, Spring Retry dependencies |
| [AsyncConfig.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/property/internal/application/config/AsyncConfig.java) | Add named thread pools for PDF/report/upload executors |

### Verification Plan

#### Automated Tests
- `mvn test` — all existing Spring Modulith tests must continue passing
- New unit tests for each `*ServiceImpl` with mocked ports
- Integration test for webhook signature verification with known HMAC payloads
- Contract state machine transition tests covering all valid/invalid paths

#### Manual Verification
- End-to-end payment flow: Initialize → Payway checkout → Webhook callback → Contract status update
- PDF generation visual check: template rendering with real contract data
- CSV/Excel export: verify data completeness and PII exclusion
- Escrow hold/release cycle with Payway sandbox environment
