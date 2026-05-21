# Feature Discovery & Requirements Document

This document outlines the functional, security, and non-functional requirements discovered during the analysis of the Property Management System codebase and architectural documents.

### Discovered Requirements

**Category: Functional**

*   **Req ID:** 2.1.1
*   **Feature Name:** User Registration & Authentication
*   **Actors:** Guest, Customer, Property Owner, System
*   **Test Scenarios:** 
    *   Success: Register as Customer/Owner via `/public/auth/register`, Login with valid credentials, Refresh JWT token successfully using `/public/auth/refresh`.
    *   Failure: Login with invalid credentials, register with existing email/phone, expired refresh token.
    *   Edge Case: Attempt to register as Admin/Agent via public endpoint (should fail).
*   **Est. Test Cases:** 15

*   **Req ID:** 2.1.2
*   **Feature Name:** Digital KYC & User Profile Management
*   **Actors:** Customer, Property Owner, Admin, Authenticated User
*   **Test Scenarios:**
    *   Success: Upload front/back ID cards using `UpdateAccountDto`, update profile information, Admin approves/rejects Property Owner account via `/account/{propOwnerId}/{approve}/approve`.
    *   Failure: Missing compulsory KYC documents during property creation, upload invalid file types.
    *   Edge Case: Admin soft deletes an active user with existing contracts.
*   **Est. Test Cases:** 12

*   **Req ID:** 2.2.1
*   **Feature Name:** Multi-tier Property Inventory Management
*   **Actors:** Property Owner, Admin, Sales Agent
*   **Test Scenarios:**
    *   Success: Owner drafts a property listing, Admin approves and sets status to AVAILABLE, Admin assigns Agent to property via `/properties/{propertyId}/assign-agent/{agentId}`.
    *   Failure: Submit property without mandatory location data or document types.
    *   Edge Case: Update property details while status is already APPROVED (should revert to PENDING for Owner, remain APPROVED for Admin).
*   **Est. Test Cases:** 18

*   **Req ID:** 2.2.2
*   **Feature Name:** Property Search, Filtering & Favorites
*   **Actors:** Public, Customer
*   **Test Scenarios:**
    *   Success: Filter properties by location (City/District/Ward), price range, area, and orientation via `/public/properties/cards`. Add/Remove property to/from favorites via `/favorites/like`.
    *   Failure: Invalid pagination parameters, negative price ranges.
    *   Edge Case: Search for properties that are soft-deleted or marked as UNAVAILABLE.
*   **Est. Test Cases:** 12

*   **Req ID:** 2.3.1
*   **Feature Name:** Viewing Appointment Lifecycle
*   **Actors:** Customer, Sales Agent, Admin
*   **Test Scenarios:**
    *   Success: Customer books viewing, Admin assigns Agent (moves to CONFIRMED), Agent marks COMPLETED, Customer rates appointment (1-5 stars) via `/appointment/{appointmentId}/rate`.
    *   Failure: Book appointment for a SOLD/RENTED property, Customer attempts to rate a PENDING appointment.
    *   Edge Case: Agent assigned to overlapping appointments within the 30-minute buffer window (`DEFAULT_APPOINTMENT_DURATION_MINUTES`).
*   **Est. Test Cases:** 20

*   **Req ID:** 2.4.1
*   **Feature Name:** Deposit Contract Generation & Workflow
*   **Actors:** Sales Agent, Admin, Customer, Property Owner
*   **Test Scenarios:**
    *   Success: Create DRAFT deposit contract, approve to WAITING_OFFICIAL, generate deposit payment via `/contracts/deposit/{contractId}/create-payment`, auto-transition to ACTIVE upon payment.
    *   Failure: Create second active deposit contract for the same property.
    *   Edge Case: Customer cancels deposit contract (forfeits deposit to Owner) vs. Owner cancels (refunds deposit + penalty to Customer).
*   **Est. Test Cases:** 15

*   **Req ID:** 2.4.2
*   **Feature Name:** Smart Rental & Purchase Contract Workflow
*   **Actors:** Sales Agent, Admin
*   **Test Scenarios:**
    *   Success: Link DRAFT Purchase/Rental contract to ACTIVE Deposit contract, validate matching agreed prices, complete paperwork, transition to ACTIVE/COMPLETED upon final payments.
    *   Failure: Link to an expired or mismatched Deposit contract.
    *   Edge Case: Admin decides the fate of a held Security Deposit at the end of a Rental Contract (Return to Customer vs. Transfer to Owner) via `/contracts/rental/{contractId}/decide-security-deposit`.
*   **Est. Test Cases:** 22

*   **Req ID:** 2.5.1
*   **Feature Name:** Integrated Payment Processing (Collections)
*   **Actors:** Customer, Property Owner, System
*   **Test Scenarios:**
    *   Success: Generate PayOS checkout link for Deposit/Advance/Monthly Rent/Service Fee, process SUCCESS webhook via `/webhooks/payway`, auto-update payment and contract statuses.
    *   Failure: Webhook payload verification fails (invalid signature), attempt to pay an already SUCCESS payment.
    *   Edge Case: Late payments triggering the `RentalContractScheduler` penalty engine.
*   **Est. Test Cases:** 18

*   **Req ID:** 2.5.2
*   **Feature Name:** Payouts & Internal Settlements
*   **Actors:** Accountant, Admin, System
*   **Test Scenarios:**
    *   Success: Accountant manually updates payment status to SYSTEM_SUCCESS for Salary/Bonus, System generates Owner Payout records after customer payment succeeds.
    *   Failure: Payout triggered for a user missing Bank Account / BIN details (triggers system alert).
    *   Edge Case: Customer refund triggered after an Owner cancels a contract mid-flight.
*   **Est. Test Cases:** 14

*   **Req ID:** 2.6.1
*   **Feature Name:** Violation Reporting & Moderation
*   **Actors:** Authenticated User, Admin
*   **Test Scenarios:**
    *   Success: User reports fraudulent property with media evidence via `POST /violations`, Admin reviews and applies penalty (WARNING, REMOVED_POST, SUSPENDED_ACCOUNT), system notifies related parties.
    *   Failure: Attempt to report a non-existent entity.
    *   Edge Case: Admin dismisses a report as DISMISSED, ensuring no penalties are applied to the reported actor's ranking.
*   **Est. Test Cases:** 10

*   **Req ID:** 2.7.1
*   **Feature Name:** Analytics, Ranking & Dashboards
*   **Actors:** Admin, System
*   **Test Scenarios:**
    *   Success: Admin views financial revenue distribution via `/statistic-report/financial/{year}`, system auto-calculates Lead Scores for Customers and Performance Tiers (Bronze to Platinum) for Agents based on deal conversion.
    *   Failure: Requesting dashboard stats for a future year.
    *   Edge Case: Scheduler recalculates agent salary at the end of the month based on dynamic net profit sharing tiers.
*   **Est. Test Cases:** 15


**Category: Security**

*   **Req ID:** 4.1.1
*   **Feature Name:** Authentication & JWT Token Management
*   **Actors:** System, Authenticated User
*   **Test Scenarios:**
    *   Success: Valid JWT grants access, JWT expiration triggers 401, Refresh token generates new pair.
    *   Failure: Malformed JWT, tampered signature, using expired refresh token.
*   **Est. Test Cases:** 8

*   **Req ID:** 4.1.2
*   **Feature Name:** Role-Based Access Control (RBAC)
*   **Actors:** System
*   **Test Scenarios:**
    *   Success: Admin accesses `/statistic-report/**`, Agent accesses their assigned contracts via `/assignments/my-viewing-list`, Customer accesses `/appointment/viewing-cards`.
    *   Failure: Agent attempts to void a contract (Admin only via `/contracts/deposit/{contractId}/void`), Customer attempts to approve property.
*   **Est. Test Cases:** 12

*   **Req ID:** 4.1.3
*   **Feature Name:** Insecure Direct Object Reference (IDOR) Prevention
*   **Actors:** Customer, Property Owner, Sales Agent
*   **Test Scenarios:**
    *   Success: User successfully fetches their own notification via `/notifications/{id}`.
    *   Failure: Customer A attempts to fetch Customer B's notification or payment details using B's UUID.
    *   Edge Case: Property Owner requesting payment details for a property they own (allowed even if not the payer).
*   **Est. Test Cases:** 10

*   **Req ID:** 4.1.4
*   **Feature Name:** Webhook Signature Verification
*   **Actors:** Payment Gateway (Payway/PayOS), System
*   **Test Scenarios:**
    *   Success: Webhook payload with valid `X-Signature` HMAC-SHA256 hash is processed.
    *   Failure: Webhook payload with missing signature or mismatched hash is rejected with 401.
*   **Est. Test Cases:** 5

*   **Req ID:** 4.1.5
*   **Feature Name:** Secure File Uploads
*   **Actors:** System, Authenticated User
*   **Test Scenarios:**
    *   Success: Valid image/document uploaded to Cloudinary.
    *   Failure: Upload payload exceeds `MULTIPART_MAX_FILE_SIZE`, upload executable script disguised as an image.
*   **Est. Test Cases:** 6


**Category: Non-Functional / Integration**

*   **Req ID:** 5.1.1
*   **Feature Name:** Payment Gateway Integration State Integrity
*   **Actors:** System, Payment Gateway
*   **Test Scenarios:**
    *   Success: Idempotency keys (`Idempotency-Key` header) prevent duplicate payment session creation on gateway retries.
    *   Failure: Gateway timeout handles 5xx errors via `PaywayServerException`.
*   **Est. Test Cases:** 5

*   **Req ID:** 5.1.2
*   **Feature Name:** Asynchronous Push Notifications (FCM)
*   **Actors:** System, Firebase
*   **Test Scenarios:**
    *   Success: `NotificationServiceImpl` asynchronously dispatches FCM push notification upon state change using `@Async`.
    *   Failure: Invalid FCM token marks delivery status as FAILED without breaking the primary business transaction.
*   **Est. Test Cases:** 4

*   **Req ID:** 5.1.3
*   **Feature Name:** Automated Schedulers & Cron Jobs
*   **Actors:** System (Schedulers)
*   **Test Scenarios:**
    *   Success: `RentalContractScheduler` generates monthly rent payments on the 1st of the month (`0 5 0 1 * *`).
    *   Success: `SaleAgentRankingScheduler` recalculates tiers at midnight (`0 0 0 * * ?`).
    *   Edge Case: Overdue payments accrue penalties based on `latePaymentPenaltyRate` after grace periods.
*   **Est. Test Cases:** 8

*   **Req ID:** 5.1.4
*   **Feature Name:** Contract State Machine Integrity
*   **Actors:** System
*   **Test Scenarios:**
    *   Success: Contract follows DRAFT -> WAITING_OFFICIAL -> PENDING_PAYMENT -> ACTIVE.
    *   Failure: Attempt to skip states or transition from terminal states like CANCELLED/COMPLETED.
*   **Est. Test Cases:** 7
