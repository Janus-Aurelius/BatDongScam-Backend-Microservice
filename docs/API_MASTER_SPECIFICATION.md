# API Master Specification - Source of Truth

This document serves as the definitive guide for the BatDongSan Platform APIs. It complements the dynamic Swagger UI by providing architectural context, security standards, and cross-cutting concerns.

## 1. Unified Access
- **API Gateway:** `http://localhost:8080`
- **Unified Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **Aggregation:** The Gateway aggregates OpenAPI specs from all microservices. Use the dropdown in Swagger UI to switch services.

## 2. Service Catalog & Routing
All APIs are routed through the Gateway using the following path prefixes:

| Service | Path Prefix | Internal Port | Description |
|---------|-------------|---------------|-------------|
| **IAM Service** | `/api/auth/**`, `/api/users/**` | 8084 | Identity & Access Management |
| **Financial Service** | `/api/payments/**`, `/api/commissions/**` | 8085 | Payments & Financials |
| **Appointment Service** | `/api/appointment/**` | 8082 | Bookings & Appointments |
| **Moderation Service** | `/api/violations/**` | 8087 | Content Moderation & Violations |
| **Notification Service** | `/api/notifications/**` | 8083 | Push & Email Notifications |
| **Search Service** | `/api/search/**` | 8086 | Property Search & Discovery |
| **Core Macroservice** | `/**` (Catch-all) | 8081 | Legacy & Core Business Logic |

## 3. Security & Authentication

### 3.1 External Authentication (JWT)
The Gateway validates JWTs in the `Authorization: Bearer <token>` header.
- **Public Paths:** `/api/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`.
- **Protected Paths:** Require a valid JWT.

### 3.2 Internal Authentication (Header-based)
Once the Gateway validates a JWT, it forwards the request with internal headers:
- `X-User-Id`: The unique identifier of the user.
- `X-User-Roles`: Comma-separated list of roles (e.g., `CUSTOMER, SALESAGENT, ADMIN`).

**Important:** Microservices trust these headers and do not re-validate the JWT.

## 4. Global Standards

### 4.1 Unified Response Format
All APIs must return the standard `ApiResponse` wrapper:
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "errors": null,
  "timestamp": "2026-06-13T08:00:00Z"
}
```

### 4.2 Error Handling
- **400 Bad Request:** Validation failures.
- **401 Unauthorized:** Missing or invalid JWT.
- **403 Forbidden:** Insufficient roles.
- **404 Not Found:** Resource not found.
- **500 Internal Server Error:** Unexpected failures.

## 5. Event-Driven Architecture (Kafka)
Services communicate asynchronously via Kafka topics.

| Topic | Publisher | Subscribers | Event Type |
|-------|-----------|-------------|------------|
| `property-events` | Core | Search, Appointment | PropertyCreated, PropertyUpdated |
| `payment-events` | Financial | Core, Notification | PaymentCompleted, PaymentOverdue |
| `user-events` | IAM | Notification | UserCreated, UserUpdated |

## 6. Maintenance
- **Updating Docs:** Update the Java code and annotations. Swagger UI updates automatically.
- **Breaking Changes:** Must be communicated to all teams using the `API_MASTER_SPECIFICATION.md` as the reference.

---

## 7. IAM Service (Identity & Access Management)
Base Path: `/api/auth`, `/api/users`, `/api/account`

### 7.1 Authentication (`/api/auth`)

#### POST `/api/auth/login`
Authenticates a user and returns access/refresh tokens.
- **Request Body:** `LoginRequest`
- **Curl Example:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@bds.com",
    "password": "password",
    "rememberMe": true
  }'
```

#### POST `/api/auth/register`
Registers a new `CUSTOMER` or `PROPERTY_OWNER`.
- **Request Body:** Multipart/Form-Data (`RegisterRequest`)
- **Curl Example:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -F "firstName=John" \
  -F "lastName=Doe" \
  -F "email=john.doe@example.com" \
  -F "password=password123" \
  -F "passwordConfirm=password123" \
  -F "role=CUSTOMER"
```

### 7.2 User Management (`/api/account`)

#### GET `/api/account/me`
Retrieves current authenticated user's profile.
- **Headers:** `Authorization: Bearer <token>`
- **Curl Example:**
```bash
curl -X GET http://localhost:8080/api/account/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### PATCH `/api/account/me`
Updates current user's profile.
- **Headers:** `Authorization: Bearer <token>`
- **Request Body:** Multipart/Form-Data (`UpdateAccountDto`)
- **Curl Example:**
```bash
curl -X PATCH http://localhost:8080/api/account/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -F "firstName=Johnny"
```

---

## 8. Financial Service
Base Path: `/api/payments`, `/api/commissions`

### 8.1 Payments (`/api/payments`)

#### POST `/api/payments`
Creates a new payment record (typically called by Core/Admin).
- **Headers:** `Authorization: Bearer <token>`
- **Request Body:** `CreatePaymentRequest`
- **Curl Example:**
```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "contractId": "550e8400-e29b-41d4-a716-446655440000",
    "propertyId": "b1a2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
    "payerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "paymentType": "DEPOSIT",
    "amount": 500.00,
    "dueDate": "2026-07-01",
    "notes": "First deposit for apartment viewing"
  }'
```

#### GET `/api/payments/{paymentId}`
Retrieves details of a specific payment.
- **Headers:** `Authorization: Bearer <token>`
- **Curl Example:**
```bash
curl -X GET http://localhost:8080/api/payments/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 8.2 Commissions (`/api/commissions`)

#### GET `/api/commissions/my`
Allows a Sale Agent to view their own commissions.
- **Headers:** `Authorization: Bearer <token>`
- **Curl Example:**
```bash
curl -X GET http://localhost:8080/api/commissions/my?page=0&size=20 \
  -H "Authorization: Bearer YOUR_AGENT_TOKEN"
```

---

## 9. Appointment Service
Base Path: `/api/appointment`, `/api/rankings`

### 9.1 Viewing Appointments (`/api/appointment`)

#### POST `/api/appointment`
Books a new property viewing appointment.
- **Headers:** `Authorization: Bearer <token>`
- **Request Body:** `BookAppointmentRequest`
- **Curl Example:**
```bash
curl -X POST http://localhost:8080/api/appointment \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "propertyId": "b1a2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
    "requestedDate": "2026-08-15T10:00:00",
    "customerRequirements": "Need information on utilities",
    "agentId": null
  }'
```

#### PATCH `/api/appointment/{appointmentId}/cancel`
Cancels an appointment.
- **Headers:** `Authorization: Bearer <token>`
- **Curl Example:**
```bash
curl -X PATCH http://localhost:8080/api/appointment/550e8400-e29b-41d4-a716-446655440000/cancel \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reason": "Change of schedule"}'
```

#### PATCH `/api/appointment/{appointmentId}/rate`
Rates a completed viewing.
- **Headers:** `Authorization: Bearer <token>`
- **Curl Example:**
```bash
curl -X PATCH http://localhost:8080/api/appointment/550e8400-e29b-41d4-a716-446655440000/rate \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "rating": 5,
    "comment": "Great property and helpful agent!"
  }'
```

### 9.2 Rankings & Performance (`/api/rankings`)

#### GET `/api/rankings/current-tier`
Retrieves the current performance tier of a user.
- **Curl Example:**
```bash
curl -X GET "http://localhost:8080/api/rankings/current-tier?userId=f47ac10b-58cc-4372-a567-0e02b2c3d479&role=SALESAGENT"
```

---

## 10. Moderation Service
Base Path: `/api/violations`

### 10.1 Violation Reports (`/api/violations`)

#### POST `/api/violations`
Submits a new violation report (e.g., scam, incorrect info).
- **Headers:** `Authorization: Bearer <token>`
- **Request Body:** Multipart/Form-Data
  - `payload`: JSON string of `ViolationCreateRequest`
  - `evidenceFiles`: Array of files (optional)
- **Violation Types:** `NON_COMPLIANCE_WITH_TERMS`, `MISREPRESENTATION_OF_PROPERTY`, `FRAUDULENT_LISTING`, `HARASSMENT`, `SCAM_ATTEMPT`, `INAPPROPRIATE_CONTENT`, `FAILURE_TO_DISCLOSE_INFORMATION`, `SPAM_OR_DUPLICATE_LISTING`
- **Curl Example:**
```bash
curl -X POST http://localhost:8080/api/violations \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -F 'payload={
    "reporterId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "violationType": "SCAM_ATTEMPT",
    "description": "The property owner is asking for advance payment via suspicious link.",
    "violationReportedType": "PROPERTY",
    "reportedId": "b1a2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6"
  };type=application/json' \
  -F "evidenceFiles=@screenshot.png"
```

#### GET `/api/violations/my-violations`
Lists violation reports submitted by the current user.
- **Headers:** `Authorization: Bearer <token>`
- **Curl Example:**
```bash
curl -X GET http://localhost:8080/api/violations/my-violations \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### PUT `/api/violations/admin/{id}`
Updates the status of a violation report (Admin only).
- **Headers:** `Authorization: Bearer <token>`
- **Curl Example:**
```bash
curl -X PUT http://localhost:8080/api/violations/admin/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "RESOLVED",
    "adminNote": "Verified scam behavior. User banned.",
    "penaltyApplied": true
  }'
```

---

## 11. Notification Service
Base Path: `/api/notifications`

### 11.1 User Notifications (`/api/notifications`)

#### GET `/api/notifications`
Retrieves paginated notifications for the current user.
- **Headers:** `Authorization: Bearer <token>`
- **Curl Example:**
```bash
curl -X GET http://localhost:8080/api/notifications?page=1&limit=10 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### PATCH `/api/notifications/{notificationId}/read`
Marks a specific notification as read.
- **Headers:** `Authorization: Bearer <token>`
- **Curl Example:**
```bash
curl -X PATCH http://localhost:8080/api/notifications/550e8400-e29b-41d4-a716-446655440000/read \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### POST `/api/notifications` (Internal Only)
Used by other services to trigger notifications.
- **Headers:** `X-User-Id` (Internal)
- **Curl Example:**
```bash
curl -X POST http://localhost:8083/api/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "recipientId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "type": "PAYMENT",
    "title": "Payment Received",
    "message": "Your deposit of 500.00 has been confirmed.",
    "relatedEntityType": "PAYMENT",
    "relatedEntityId": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

---

## 12. Search Service
Base Path: `/api/search`

### 12.1 Search Logging (`/api/search/log`)

#### POST `/api/search/log`
Logs a single search entry for analytics.
- **Request Body:** `AddSearchRequest`
- **Curl Example:**
```bash
curl -X POST http://localhost:8080/api/search/log \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "cityId": "550e8400-e29b-41d4-a716-446655440000",
    "propertyTypeId": "b1a2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6"
  }'
```

### 12.2 Search Analytics (`/api/search/top`)

#### GET `/api/search/top`
Retrieves top most searched items by user and type.
- **Curl Example:**
```bash
curl -X GET "http://localhost:8080/api/search/top?userId=f47ac10b-58cc-4372-a567-0e02b2c3d479&searchType=CITY&year=2026&month=6"
```

#### GET `/api/search/most-searched-properties`
Retrieves the most popular property IDs based on search volume.
- **Curl Example:**
```bash
curl -X GET "http://localhost:8080/api/search/most-searched-properties?limit=5&year=2026&month=6"
```

---

## 13. Core Macroservice
Base Path: `/properties`, `/contracts`, `/escrow`

### 13.1 Property Management (`/properties`)

#### POST `/properties`
Creates a new property listing with images and documents.
- **Headers:** `Authorization: Bearer <token>`
- **Request Body:** Multipart/Form-Data
  - `payload`: `CreatePropertyWebRequest` (JSON)
  - `images`: Files
  - `documents`: Files
- **Curl Example:**
```bash
curl -X POST http://localhost:8080/properties \
  -H "Authorization: Bearer YOUR_OWNER_TOKEN" \
  -F 'payload={
    "title": "Luxury Villa",
    "description": "5 bedroom villa with pool",
    "price": 500000,
    "area": 350,
    "transactionType": "SALE"
  };type=application/json' \
  -F "images=@villa.jpg"
```

#### GET `/public/properties/search`
Public search for property listings.
- **Curl Example:**
```bash
curl -X GET "http://localhost:8080/public/properties/search?cityIds=550e&minPrice=100000&maxPrice=600000&page=0&size=10"
```

### 13.2 Contract Management (`/contracts`)

#### POST `/contracts/purchases`
Initiates a new purchase contract.
- **Headers:** `Authorization: Bearer <token>` (Admin/Agent)
- **Request Body:** `CreatePurchaseContractWebRequest`
- **Curl Example:**
```bash
curl -X POST http://localhost:8080/contracts/purchases \
  -H "Authorization: Bearer YOUR_AGENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "propertyId": "b1a2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
    "customerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "agreedPrice": 500000,
    "advancePaymentAmount": 50000,
    "advancePaymentDeadline": "2026-08-01",
    "finalPaymentDeadline": "2026-12-31"
  }'
```

#### POST `/contracts/purchases/{contractId}/approve`
Approves a purchase contract.
- **Headers:** `Authorization: Bearer <token>`
- **Curl Example:**
```bash
curl -X POST http://localhost:8080/contracts/purchases/550e8400-e29b-41d4-a716-446655440000/approve \
  -H "Authorization: Bearer YOUR_AGENT_TOKEN"
```
