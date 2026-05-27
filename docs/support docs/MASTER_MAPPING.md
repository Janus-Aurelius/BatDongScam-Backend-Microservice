# Master Mapping File

This document provides a detailed mapping between User Stories (US) from the `PRODUCT_BACKLOG.csv` and the reverse-engineered logic found in the legacy codebase. This serves as a structured foundation for generating Software Requirements Specifications (SRS).

## UC1: User Registration
**US-ID**: US-001
**Name**: User Registration
**Description**: As a Guest, I want to create an account so I can access platform features.
**Actor**: Guest

### CONDITIONS & TRIGGERS
- **Trigger**: `POST /auth/register` (`PublicController.register`)
- **Pre-conditions**: Unauthenticated request. Cannot register as `ADMIN` or `SALESAGENT`.
- **Post-conditions**: `User` entity is saved to `[user]` table along with `[customer]` or `[property_owner]` records. Profile pictures are uploaded to Cloudinary.

### ACTIVITIES FLOW
1. Actor submits registration data with multipart form (images + user details) and specifies `roleEnum`.
2. System validates form fields.
3. System checks if `[user.email]` already exists in the database.
4. System verifies the `roleEnum` is either `CUSTOMER` or `PROPERTY_OWNER`.
5. System uploads `[user.frontIdPicture]` and `[user.backIdPicture]` to Cloudinary.
6. System hashes the provided `[user.password]`.
7. System maps the Ward ID and creates the `User` record.
8. System assigns default `[user.status]` based on role.
9. System returns success message.

### BUSINESS RULES
- **Validate Rules**: 
  - `[user.email]` must be unique. If `userRepository.existsByEmail(email)` is true, throw `BindException`.
  - Roles `ADMIN` and `SALESAGENT` are strictly prohibited via this endpoint.
  - Optional `[ward.id]` must exist if provided.
- **Creating/Saving Rules**: 
  - `[user.password]` must be hashed using `passwordEncoder.encode()`.
  - If `role` is `CUSTOMER`, set `[user.status] = ACTIVE` and create corresponding `Customer` record.
  - If `role` is `PROPERTY_OWNER`, set `[user.status] = PENDING_APPROVAL` and create corresponding `PropertyOwner` record.
- **Message Rules**: Return "Register successful" upon completion.

---

## UC2: JWT Login
**US-ID**: US-002
**Name**: JWT Login
**Description**: As a User, I want to log in securely using credentials to receive a JWT token.
**Actor**: User

### CONDITIONS & TRIGGERS
- **Trigger**: `POST /auth/login` (`PublicController.login`)
- **Pre-conditions**: Unauthenticated request.
- **Post-conditions**: JWT Access Token and Refresh Token are issued. User status may be activated.

### ACTIVITIES FLOW
1. Actor submits email and password.
2. System looks up user by `[user.email]`.
3. System authenticates credentials using Spring Security `AuthenticationManager`.
4. System activates the user profile if it isn't already active.
5. System generates an Access Token and a Refresh Token (stateless).
6. System returns the tokens and user ID/role.

### BUSINESS RULES
- **Validate Rules**: 
  - If `[user.email]` is not found, throw `AuthenticationCredentialsNotFoundException`.
  - If password validation fails, throw `AuthenticationCredentialsNotFoundException`.
- **Creating/Saving Rules**: Tokens are generated statelessly via `jwtTokenProvider` and are not persisted in Redis.
- **Message Rules**: Return "Login successful" with the `TokenResponse` payload.

---

## UC3: Create Property Listing
**US-ID**: US-003
**Name**: Create Property Listing
**Description**: As an Owner, I want to list my property with details and images.
**Actor**: Owner / Admin

### CONDITIONS & TRIGGERS
- **Trigger**: `POST /properties` (`PropertyController.createProperty`)
- **Pre-conditions**: Actor must be authenticated and have role `ADMIN` or `PROPERTY_OWNER`.
- **Post-conditions**: `[property]`, `[media]`, and `[document]` records are created. Status is `PENDING` or `AVAILABLE`.

### ACTIVITIES FLOW
1. Actor submits property JSON payload, media files, and document files.
2. System verifies user roles.
3. System validates compulsory legal documents.
4. System calculates the standard commission rate and service fee.
5. System maps the location (Ward) and Property Type.
6. System assigns status based on the Actor's role.
7. System saves the property.
8. System uploads media and documents to Cloudinary and saves their URLs.
9. System logs property creation for ranking analytics.
10. System returns created `PropertyDetails`.

### BUSINESS RULES
- **Validate Rules**: 
  - `[document]` metadata must contain required compulsory documents.
  - `[property_type.id]` and `[ward.id]` must exist.
- **Creating/Saving Rules**:
  - `[property.commissionRate]` defaults to `Constants.DEFAULT_PROPERTY_COMMISSION_RATE`.
  - If Actor is Admin: Set `[property.status] = AVAILABLE`, set `[property.serviceFeeCollectedAmount]`, set `[property.approvedAt]`.
  - If Actor is Owner: Set `[property.status] = PENDING`.
  - System calls `rankingService.propertyOwnerAction` to track points for `PROPERTY_FOR_SALE_LISTED` or `PROPERTY_FOR_RENT_LISTED`.

---

## UC4: Search & Filter Properties
**US-ID**: US-004
**Name**: Search & Filter Properties
**Description**: As a Customer, I want to search for properties by location, price, and type.
**Actor**: Customer / Guest

### CONDITIONS & TRIGGERS
- **Trigger**: `GET /properties/cards` (`PublicController.getAllCardsWithFilters`)
- **Pre-conditions**: None.
- **Post-conditions**: User's search preferences are logged for analytics (if authenticated).

### ACTIVITIES FLOW
1. Actor requests list of properties with optional query parameters.
2. System records search criteria in `searchService.addSearchList` if Actor is authenticated.
3. System fetches Top-K most searched properties if requested.
4. System filters Owners/Agents by requested tiers.
5. System executes dynamic database query with all filters.
6. System maps results to `PropertyCard` and checks if the current user has "Favorited" them.
7. System returns paginated response.

### BUSINESS RULES
- **Validate Rules**: None strictly enforced on public queries; invalid parameters return empty pages.
- **Message Rules**: Return PageResponse with mapped property summaries and computed Tiers/Favorite status.

---

## UC5: Property Details View
**US-ID**: US-005
**Name**: Property Details View
**Description**: As a User, I want to see full details and media of a specific property.
**Actor**: User

### CONDITIONS & TRIGGERS
- **Trigger**: `GET /properties/{id}` (`PublicController.getPropertyDetails`)
- **Pre-conditions**: None.
- **Post-conditions**: None.

### ACTIVITIES FLOW
1. Actor requests property by ID.
2. System retrieves `PropertyDetailsProjection`, `MediaProjection`, and `DocumentProjection`.
3. System fetches the ranking tiers for the associated Owner and Sales Agent.
4. System returns aggregated DTO.

### BUSINESS RULES
- **Validate Rules**: If `[property.id]` does not exist, throw Exception.
- **Message Rules**: Returns full `PropertyDetails` object.

---

## UC6: Request Appointment
**US-ID**: US-006
**Name**: Request Appointment
**Description**: As a Customer, I want to book a viewing session for a property.
**Actor**: Customer

### CONDITIONS & TRIGGERS
- **Trigger**: `POST /appointments` (`AppointmentController.createAppointment`)
- **Pre-conditions**: Actor must be authenticated.
- **Post-conditions**: `[appointment]` record is saved as `PENDING` (or `CONFIRMED` if an agent is pre-assigned). Notifications are dispatched.

### ACTIVITIES FLOW
1. Actor selects property and date, and submits the request.
2. System verifies that the requested date is in the future.
3. System verifies property status allows for viewings.
4. System checks for existing duplicate pending/confirmed appointments.
5. System creates the Appointment record.
6. System updates Customer ranking score (VIEWING_REQUESTED).
7. System sends an FCM/In-App Notification to the Property Owner.
8. System sends an FCM/In-App Notification to the assigned Agent (if applicable).

### BUSINESS RULES
- **Validate Rules**: 
  - Customers cannot book appointments on behalf of others.
  - `[appointment.requestedDate]` > `LocalDateTime.now()`.
  - `[property.status]` cannot be `PENDING`, `REJECTED`, `SOLD`, `RENTED`, `REMOVED`, or `DELETED`.
  - Actor must not have a pre-existing `PENDING` or `CONFIRMED` appointment for this specific property.
- **Creating/Saving Rules**: 
  - If `agentId` is provided: `[appointment.status] = CONFIRMED`, set `[appointment.confirmedDate]`.
  - Else: `[appointment.status] = PENDING`.

---

## UC7: Confirm/Reject Appointment
**US-ID**: US-007
**Name**: Confirm/Reject Appointment
**Description**: As an Owner/Agent, I want to manage viewing requests.
**Actor**: Owner / Agent / Admin

### CONDITIONS & TRIGGERS
- **Trigger**: `PATCH /appointments/{id}/cancel` OR `PATCH /appointments/{id}/update-details` OR `POST /appointments/assign-agent`
- **Pre-conditions**: Appointment must be `PENDING` or `CONFIRMED`.
- **Post-conditions**: `[appointment.status]` is updated to `CANCELLED` or `CONFIRMED`.

### ACTIVITIES FLOW
1. **To Confirm (Assign Agent):** Admin assigns an agent. System updates `[appointment.status]` to `CONFIRMED` and sets `[appointment.agent]`.
2. **To Reject/Cancel:** Actor invokes cancel endpoint. System verifies authorization, sets status to `CANCELLED`, sets `[appointment.cancelledReason]`, and applies ranking penalty if triggered by an agent.

### BUSINESS RULES
- **Validate Rules**:
  - Cannot cancel if `[appointment.status]` is already `CANCELLED` or `COMPLETED`.
  - Only the Customer who booked, Assigned Agent, or Admin can cancel.
- **Creating/Saving Rules**:
  - On cancellation by Agent: Triggers `rankingService.agentAction` for `APPOINTMENT_CANCELLED` (penalty).

---

## UC8: Draft Rental Contract
**US-ID**: US-008
**Name**: Draft Rental Contract
**Description**: As an Agent, I want to generate a rental contract draft from a template.
**Actor**: Admin / Agent

### CONDITIONS & TRIGGERS
- **Trigger**: `POST /contracts/rental` (`ContractController.createRentalContract`)
- **Pre-conditions**: Only Admin and Sales Agents can create.
- **Post-conditions**: A new `[rental_contract]` is created in `DRAFT` status.

### ACTIVITIES FLOW
1. Actor submits contract details.
2. System ensures there isn't already an active contract for the property.
3. System optionally links to a `depositContractId`, validating the deposit is `ACTIVE` and prices match.
4. System sets status to `DRAFT`.
5. System returns contract detail response.

### BUSINESS RULES
- **Validate Rules**: 
  - Only ONE non-DRAFT rental contract is allowed per property at a time.
  - If deposit contract is linked, `[deposit_contract.status]` must be ACTIVE and not expired.
- **Creating/Saving Rules**: `[contract.status]` is initialized to `DRAFT`.

---

## UC16: Generate Sales Report
**US-ID**: US-016
**Name**: Generate Sales Report
**Description**: As an Admin, I want to see monthly revenue and transaction statistics.
**Actor**: Admin

### CONDITIONS & TRIGGERS
- **Trigger**: `GET /statistic-report/financial/{year}` (`StatisticReportController.getFinancialStats`)
- **Pre-conditions**: Actor must be `ADMIN`.
- **Post-conditions**: None.

### ACTIVITIES FLOW
1. Admin requests financial stats for a specific year.
2. System queries database aggregates for revenue grouped by month.
3. System returns formatted statistics.

### BUSINESS RULES
- **Validate Rules**: Enforced `@PreAuthorize("hasRole('ADMIN')")`.

---

---

---

## UC9: Deposit Contract Execution
**US-ID**: US-009
**Name**: Deposit Contract Execution
**Description**: As a Customer, I want to sign a deposit contract to reserve a property.
**Actor**: Customer

### CONDITIONS & TRIGGERS
- **Trigger**: `POST /contracts/deposit/{id}/approve` or `markPaperworkComplete`.
- **Pre-conditions**: Contract in `DRAFT` status.
- **Post-conditions**: Status updated to `WAITING_OFFICIAL` or `ACTIVE`. Property status updated to `UNAVAILABLE`.

### ACTIVITIES FLOW
1. Actor reviews the draft contract.
2. Actor confirms/signs the contract.
3. System updates `[deposit_contract.status]` to `WAITING_OFFICIAL`.
4. System blocks the property from new appointments by setting `[property.status] = UNAVAILABLE`.
5. System triggers notification to the Owner.

### BUSINESS RULES
- **Validate Rules**: Only `DRAFT` contracts can be approved/signed.
- **Creating/Saving Rules**: Ensures atomicity between contract status update and property locking.

---

## UC10: Initialize Payment
**US-ID**: US-010
**Name**: Initialize Payment
**Description**: As a User, I want to pay for contracts or services using PayOS/Payway.
**Actor**: User

### CONDITIONS & TRIGGERS
- **Trigger**: `POST /contracts/deposit/{id}/payment` or `GET /payments/link`.
- **Pre-conditions**: Contract in `WAITING_OFFICIAL` status.
- **Post-conditions**: `[payment]` record created in `PENDING` status. Payment gateway URL generated.

### ACTIVITIES FLOW
1. Actor requests to make a payment.
2. System calculates amount based on contract terms.
3. System creates a `[payment]` record with status `PENDING`.
4. System calls external gateway (PayOS/Payway) API to generate a session.
5. System returns the redirect URL to the Actor.

### BUSINESS RULES
- **Validate Rules**: Payment cannot be created if a `PENDING` or `SUCCESS` payment already exists for the same contract/type.
- **Creating/Saving Rules**: Default due date set to `LocalDate.now() + 3 days`.

---

## UC11: Handle Payment Webhook
**US-ID**: US-011
**Name**: Handle Payment Webhook
**Description**: As a System, I want to receive payment confirmations from external gateways.
**Actor**: System

### CONDITIONS & TRIGGERS
- **Trigger**: `POST /external/payway/webhook` (`PaywayWebhookController.handle`)
- **Pre-conditions**: Valid signed request from gateway.
- **Post-conditions**: `[payment.status]` set to `SUCCESS`. `[contract.status]` updated to `ACTIVE`.

### ACTIVITIES FLOW
1. System receives webhook payload.
2. System verifies payload signature using shared secret.
3. System identifies corresponding `[payment]` and `[contract]` IDs.
4. System updates payment status to `SUCCESS`.
5. System transitions contract to `ACTIVE`.
6. System notifies Actor of successful payment.

### BUSINESS RULES
- **Validate Rules**: Signature must be valid. Processing must be idempotent (ignore if already SUCCESS).

---

## UC12: Report Violation
**US-ID**: US-012
**Name**: Report Violation
**Description**: As a User, I want to report suspicious listings or behavior.
**Actor**: User

### CONDITIONS & TRIGGERS
- **Trigger**: `POST /violations` (`ViolationController.createViolationReport`)
- **Pre-conditions**: Authenticated user.
- **Post-conditions**: `[violation]` record created with status `PENDING`.

### ACTIVITIES FLOW
1. Actor selects violation type (e.g., FRAUDULENT_LISTING) and provides description.
2. Actor optionally uploads evidence images.
3. System saves violation report linked to `[property]` or `[user]`.
4. System notifies Admin of new report.

### BUSINESS RULES
- **Validate Rules**: Reported entity ID must exist.

---

## UC13: Review Violations
**US-ID**: US-013
**Name**: Review Violations
**Description**: As an Admin, I want to review reports and take disciplinary actions.
**Actor**: Admin

### CONDITIONS & TRIGGERS
- **Trigger**: `PUT /violations/admin/{id}` (`ViolationController.updateViolationReport`)
- **Pre-conditions**: Actor has `ADMIN` role.
- **Post-conditions**: Violation status updated. Penalty applied if `RESOLVED`.

### ACTIVITIES FLOW
1. Admin reviews report and evidence.
2. Admin sets status to `UNDER_REVIEW` or `RESOLVED`.
3. Admin selects penalty (WARNING, REMOVED_POST, SUSPENDED_ACCOUNT).
4. System applies penalty to the reported Actor.

---

## UC14/15: Notifications & Alerts
**US-ID**: US-014, US-015
**Name**: Push & Email Notifications
**Description**: As a User, I want to receive real-time alerts and email documents.
**Actor**: User

### CONDITIONS & TRIGGERS
- **Trigger**: Internal events (e.g., `APPOINTMENT_BOOKED`, `PAYMENT_SUCCESS`).
- **Post-conditions**: Record in `[notification]` table. FCM message sent. Email sent.

### ACTIVITIES FLOW
1. System event occurs.
2. `NotificationService` creates notification record.
3. System sends asynchronous FCM payload to registered devices.
4. System renders HTML template and sends SMTP email.

---

## UC17: Update Profile
**US-ID**: US-017
**Name**: Update Profile
**Description**: As a User, I want to manage my personal information and avatar.
**Actor**: User

### CONDITIONS & TRIGGERS
- **Trigger**: `PUT /account/me` (`AccountController.updateMe`)
- **Pre-conditions**: Authenticated user.
- **Post-conditions**: `[user]` record updated.

### ACTIVITIES FLOW
1. Actor submits updated fields (Name, Bio, Avatar).
2. System uploads new avatar to Cloudinary if provided.
3. System updates user profile in database.

---

## UC18: Promote Property
**US-ID**: US-018
**Name**: Promote Property
**Description**: As an Owner, I want to pay to rank my property higher in search results.
**Actor**: Owner

### CONDITIONS & TRIGGERS
- **Trigger**: `POST /ranking/promote` (Conceptual/Future Service)
- **Pre-conditions**: Active property listing. Payment success.
- **Post-conditions**: `[property.ranking_points]` increased.

### ACTIVITIES FLOW
1. Owner selects promotion package.
2. System processes payment (UC10/11).
3. System increments property's visibility score.
4. Search results prioritize properties with higher scores.

---

## UC19: Archive Property
**US-ID**: US-019
**Name**: Archive Property
**Description**: As an Owner, I want to de-list my property once sold or rented.
**Actor**: Owner

### CONDITIONS & TRIGGERS
- **Trigger**: `DELETE /properties/{id}` (`PropertyController.deleteProperty`)
- **Pre-conditions**: User owns the property.
- **Post-conditions**: `[property.status] = DELETED`.

### ACTIVITIES FLOW
1. Owner selects "Delete/Archive".
2. System performs soft delete by updating status.
3. System clears approval timestamps and assigned agents.

---

## UC20: Assign Agent to Property
**US-ID**: US-020
**Name**: Assign Agent to Property
**Description**: As an Admin, I want to assign a Sales Agent to manage an Owner's property.
**Actor**: Admin

### CONDITIONS & TRIGGERS
- **Trigger**: `POST /properties/{id}/assign-agent` (`PropertyController.assignAgentToProperty`)
- **Pre-conditions**: Valid property and agent IDs.
- **Post-conditions**: `[property.assignedAgent]` updated.

---

## UC21: Fetch Location Data
**US-ID**: US-021
**Name**: Fetch Location Data
**Description**: As a User, I want to select locations from a standardized list.
**Actor**: User

### CONDITIONS & TRIGGERS
- **Trigger**: `GET /public/locations/child` (`PublicController.getChildLocations`)
- **Post-conditions**: Returns hierarchical data (Provinces -> Districts -> Wards).

---

## UC22: Wishlist Property
**US-ID**: US-022
**Name**: Wishlist Property
**Description**: As a Customer, I want to save properties to my favorites.
**Actor**: Customer

### CONDITIONS & TRIGGERS
- **Trigger**: `POST /favorites` (`FavoriteController.toggleFavorite`)
- **Post-conditions**: Link record created between User and Property.

---

## UC24: Real-time Agent Chat
**US-ID**: US-024
**Name**: Real-time Agent Chat
**Description**: As a Customer, I want to chat with an Agent.
**Actor**: Customer / Agent

### ACTIVITIES FLOW (Future)
1. Actor opens chat window.
2. System establishes Socket.io/WebSocket connection.
3. Messages are persisted in MongoDB.
4. System sends push notification for offline recipients.

---

## UC27: Map-based Search
**US-ID**: US-027
**Name**: Map-based Search
**Description**: As a User, I want to search for properties by drawing on a map.
**Actor**: User

### ACTIVITIES FLOW (Future)
1. User draws polygon on map UI.
2. Client sends coordinates to `/search/map`.
3. System executes geospatial query (PostGIS/Mongo $geoWithin).
4. System returns markers within bounds.

---

## UC28: Escrow Payment Protection
**US-ID**: US-028
**Name**: Escrow Payment Protection
**Actor**: Buyer / Admin

### ACTIVITIES FLOW (Future)
1. Buyer pays into system-controlled wallet.
2. System holds funds in `ESCROW` status.
3. Admin/Legal verifies title transfer.
4. System releases funds to Seller.

---

## UC29: Referral Program
**US-ID**: US-029
**Name**: Referral Program
**Actor**: User

### ACTIVITIES FLOW (Future)
1. User generates referral link.
2. New user signs up using link.
3. System credits `Referral Points` to both users after first transaction.

---

## UC30: Agent Reviews
**US-ID**: US-030
**Name**: Agent Reviews
**Actor**: Customer

### ACTIVITIES FLOW (Future)
1. Customer completes appointment/contract.
2. Customer submits 1-5 star rating and text.
3. System calculates Agent's average rating in real-time.

---

## UC31: Owner Insights Dashboard
**US-ID**: US-031
**Name**: Owner Insights Dashboard
**Actor**: Owner

### ACTIVITIES FLOW (Future)
1. System tracks every `VIEW` event on properties.
2. System aggregates views and favorites per property.
3. Owner views dashboard with charts of interest trends.

---

## UC34: Multi-language Support
**US-ID**: US-034
**Name**: Multi-language Support
**Actor**: User

### ACTIVITIES FLOW (Future)
1. User selects locale (EN/VI).
2. App sends `Accept-Language` header.
3. API returns translated field values and localized date/currency formats.

---

## UC35: System Audit Logs
**US-ID**: US-035
**Name**: System Audit Logs
**Description**: As an Admin, I want to see a detailed audit trail of all sensitive system actions.
**Actor**: Admin

### CONDITIONS & TRIGGERS
- **Trigger**: Any state-changing API call.
- **Post-conditions**: Entry in `[audit_log]` table.

### ACTIVITIES FLOW
1. System intercepts data change (Spring Data Auditing or AOP).
2. System captures `userId`, `timestamp`, `actionType`, `oldValues`, and `newValues`.
3. System stores immutable log record.
