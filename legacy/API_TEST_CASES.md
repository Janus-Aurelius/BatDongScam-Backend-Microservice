# API Test Cases for New Endpoints

## Base URL

```
http://localhost:8080
```

## Authentication

All endpoints require Bearer token authentication unless marked as public.

```
Authorization: Bearer <your_jwt_token>
```

---

# 1. PAYMENT MODULE

## 1.1 Get All Payments (Payment Resource)

**Endpoint:** `GET /api/payments`  
**Roles:** ADMIN, ACCOUNTANT

### Test Case 1.1.1: Get all payments with default pagination

```http
GET /api/payments
Authorization: Bearer <accountant_token>
```

### Test Case 1.1.2: Get payments with filters

```http
GET /api/payments?page=0&size=10&sortBy=dueDate&sortDirection=ASC&statuses=PENDING,SYSTEM_PENDING&overdue=true
Authorization: Bearer <accountant_token>
```

### Test Case 1.1.3: Filter by payment types

```http
GET /api/payments?paymentTypes=SALARY,BONUS&statuses=SYSTEM_PENDING
Authorization: Bearer <accountant_token>
```

### Test Case 1.1.4: Filter by contract

```http
GET /api/payments?contractId=<uuid>
Authorization: Bearer <accountant_token>
```

### Test Case 1.1.5: Filter by date range

```http
GET /api/payments?dueDateFrom=2024-01-01&dueDateTo=2024-12-31
Authorization: Bearer <accountant_token>
```

**Expected Response:**

```json
{
    "statusCode": 200,
    "message": "Payments retrieved successfully",
    "data": [
        {
            "id": "uuid",
            "paymentType": "SALARY",
            "status": "SYSTEM_PENDING",
            "amount": 15000000,
            "dueDate": "2024-12-01",
            "paidDate": null,
            "payerId": null,
            "payerName": "Company",
            "payerRole": "COMPANY",
            "payeeId": "uuid",
            "payeeName": "Nguyen Van A",
            "payeeRole": "SALESAGENT",
            "contractId": null,
            "propertyId": null,
            "createdAt": "2024-11-25T10:00:00"
        }
    ],
    "paging": {
        "page": 0,
        "size": 20,
        "totalElements": 100,
        "totalPages": 5
    }
}
```

---

## 1.2 Get Payment Details

**Endpoint:** `GET /api/payments/{paymentId}`  
**Roles:** ADMIN, ACCOUNTANT

### Test Case 1.2.1: Get payment by ID

```http
GET /api/payments/123e4567-e89b-12d3-a456-426614174000
Authorization: Bearer <accountant_token>
```

**Expected Response:**

```json
{
    "statusCode": 200,
    "message": "Payment retrieved successfully",
    "data": {
        "id": "123e4567-e89b-12d3-a456-426614174000",
        "paymentType": "INSTALLMENT",
        "status": "PENDING",
        "amount": 50000000,
        "penaltyAmount": 0,
        "dueDate": "2024-12-15",
        "paidDate": null,
        "installmentNumber": 3,
        "overdueDays": 0,
        "penaltyApplied": false,
        "payerId": "uuid",
        "payerFirstName": "Tran",
        "payerLastName": "Van B",
        "payerRole": "CUSTOMER",
        "payerPhone": "0901234567",
        "contractId": "uuid",
        "contractNumber": "PUR-12345-00001",
        "contractType": "PURCHASE",
        "contractStatus": "ACTIVE",
        "propertyId": "uuid",
        "propertyTitle": "Căn hộ cao cấp Quận 1",
        "propertyAddress": "123 Nguyen Hue, Q1, HCM"
    }
}
```

---

## 1.3 Update Payment Status

**Endpoint:** `PATCH /api/payments/{paymentId}/status`  
**Roles:** ADMIN, ACCOUNTANT

### Test Case 1.3.1: Mark payment as paid (Success)

```http
PATCH /api/payments/123e4567-e89b-12d3-a456-426614174000/status
Authorization: Bearer <accountant_token>
Content-Type: application/json

{
  "status": "SUCCESS",
  "notes": "Đã nhận tiền mặt từ khách hàng",
  "transactionReference": "TXN-2024-001234"
}
```

### Test Case 1.3.2: Mark salary payment as paid

```http
PATCH /api/payments/<salary_payment_id>/status
Authorization: Bearer <accountant_token>
Content-Type: application/json

{
  "status": "SYSTEM_SUCCESS",
  "notes": "Đã chuyển khoản lương tháng 11",
  "transactionReference": "BANK-TXN-20241130-001"
}
```

**Expected Response:**

```json
{
    "statusCode": 200,
    "message": "Payment status updated successfully",
    "data": {
        "id": "uuid",
        "status": "SUCCESS",
        "paidDate": "2024-12-01",
        "notes": "Đã nhận tiền mặt từ khách hàng",
        "transactionReference": "TXN-2024-001234"
    }
}
```

---

## 1.4 Create Salary Payment

**Endpoint:** `POST /api/payments/salary`  
**Roles:** ADMIN, ACCOUNTANT

### Test Case 1.4.1: Create salary for agent

```http
POST /api/payments/salary
Authorization: Bearer <accountant_token>
Content-Type: application/json

{
  "agentId": "123e4567-e89b-12d3-a456-426614174000",
  "amount": 15000000,
  "dueDate": "2024-12-01",
  "notes": "Lương tháng 12/2024"
}
```

**Expected Response:**

```json
{
    "statusCode": 200,
    "message": "Salary payment created successfully",
    "data": {
        "id": "uuid",
        "paymentType": "SALARY",
        "status": "SYSTEM_PENDING",
        "amount": 15000000,
        "dueDate": "2024-12-01",
        "payerId": null,
        "payerFirstName": "Company",
        "payerRole": "COMPANY",
        "payeeId": "uuid",
        "payeeFirstName": "Nguyen",
        "payeeLastName": "Van A",
        "payeeRole": "SALESAGENT",
        "agentId": "uuid",
        "agentEmployeeCode": "AGT-001"
    }
}
```

---

## 1.5 Create Bonus Payment

**Endpoint:** `POST /api/payments/bonus`  
**Roles:** ADMIN, ACCOUNTANT

### Test Case 1.5.1: Create bonus for agent

```http
POST /api/payments/bonus
Authorization: Bearer <accountant_token>
Content-Type: application/json

{
  "agentId": "123e4567-e89b-12d3-a456-426614174000",
  "amount": 5000000,
  "notes": "Thưởng KPI tháng 11 - Hoàn thành 150% chỉ tiêu"
}
```

---

## 1.6 Owner Payout (Company → Owner)

**Workflow:** When a customer completes a contract payment via PayOS (deposit, installment, full payment, monthly rent, sale, or rental), the PayOS webhook handler automatically creates an owner payout record with `paymentMethod = OWNER_PAYOUT` and `status = SYSTEM_PENDING`. This represents the net amount the company owes the property owner after deducting commission (service fees are already settled before a listing goes live).

**Auto-Creation Logic:**

1. Customer pays via PayOS (e.g., 100,000,000 VND deposit)
2. Webhook handler validates payment SUCCESS
3. System calculates:

-   Commission: customerAmount × property.commissionRate (e.g., 5%)
-   Net to owner: customerAmount - commission

4. Creates Payment record with `paymentType = <original type>`, `paymentMethod = OWNER_PAYOUT`, `amount = net`, `status = SYSTEM_PENDING`

**Example Calculation:**

```
Customer pays: 100,000,000 VND (DEPOSIT)
Commission (5%): 5,000,000 VND
---
Owner payout: 95,000,000 VND
```

**Endpoint:** `PATCH /api/payments/{paymentId}/status` (use the ID of the auto-created owner payout record)  
**Roles:** ADMIN, ACCOUNTANT

### Test Case 1.6.1: Mark owner payout as completed

```http
PATCH /api/payments/<owner_payout_payment_id>/status
Authorization: Bearer <accountant_token>
Content-Type: application/json

{
  "status": "SYSTEM_SUCCESS",
  "notes": "Forwarded net amount to property owner via bank transfer",
  "transactionReference": "BANK-REF-20241201-002"
}
```

**Expected Result:** Payment status changes to `SYSTEM_SUCCESS`, `paidDate` is auto-populated. The owner receives the net amount after commission/service fee deduction.

---

## 1.7 Refund Payout (Company → Customer)

**Workflow:** When a property owner settles a cancellation refund via PayOS (`POST /api/payments/contracts/{contractId}/cancellation-refund`), the system automatically creates a new payment record with `paymentMethod = COMPANY_PAYOUT` so accountants can mark when the company forwards the money back to the customer (penalty already deducted).

**Endpoint:** `PATCH /api/payments/{paymentId}/status` (use the ID of the auto-created payout record)  
**Roles:** ADMIN, ACCOUNTANT

### Test Case 1.7.1: Mark refund payout as wired

```http
PATCH /api/payments/<refund_payout_payment_id>/status
Authorization: Bearer <accountant_token>
Content-Type: application/json

{
  "status": "SYSTEM_SUCCESS",
  "notes": "Refund forwarded to customer via bank transfer",
  "transactionReference": "BANK-REF-20241201-001"
}
```

**Expected Result:** Payment status flips to `SYSTEM_SUCCESS`, `paidDate` is auto-populated, and the refund workflow now reflects both owner settlement (PayOS) and company payout (manual).

---

# 2. APPOINTMENT MODULE

## 2.1 Create Appointment

**Endpoint:** `POST /appointment`  
**Roles:** CUSTOMER, ADMIN, SALESAGENT

**Description:** Creates a viewing appointment. Customers create for themselves, while Admin/Agent can create on behalf of customers and optionally assign an agent immediately.

### Test Case 2.1.1: Customer creates their own appointment

```http
POST /appointment
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "propertyId": "123e4567-e89b-12d3-a456-426614174000",
  "requestedDate": "2024-12-15T10:00:00",
  "customerRequirements": "Cần xem vào buổi sáng, có thể đi xe lăn"
}
```

**Expected Response:**

```json
{
    "statusCode": 200,
    "message": "Appointment created successfully",
    "data": {
        "appointmentId": "uuid",
        "propertyId": "uuid",
        "propertyTitle": "Căn hộ cao cấp Quận 7",
        "propertyAddress": "123 Nguyễn Văn Linh, Q7, HCM",
        "requestedDate": "2024-12-15T10:00:00",
        "status": "PENDING",
        "customerRequirements": "Cần xem vào buổi sáng, có thể đi xe lăn",
        "agentId": null,
        "agentName": null,
        "createdAt": "2024-12-01T14:30:00",
        "message": "Your viewing appointment has been booked. You will be notified when an agent confirms the appointment."
    }
}
```

### Test Case 2.1.2: Admin creates appointment for customer

```http
POST /appointment
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "propertyId": "123e4567-e89b-12d3-a456-426614174000",
  "customerId": "customer-uuid-here",
  "requestedDate": "2024-12-20T14:00:00",
  "customerRequirements": "Customer prefers afternoon viewing"
}
```

**Expected Response:**

```json
{
    "statusCode": 200,
    "message": "Appointment created successfully",
    "data": {
        "appointmentId": "uuid",
        "status": "PENDING",
        "message": "Your viewing appointment has been booked. You will be notified when an agent confirms the appointment."
    }
}
```

### Test Case 2.1.3: Admin creates appointment with agent assignment

```http
POST /appointment
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "propertyId": "123e4567-e89b-12d3-a456-426614174000",
  "customerId": "customer-uuid-here",
  "agentId": "agent-uuid-here",
  "requestedDate": "2024-12-18T10:00:00",
  "customerRequirements": "VIP customer, needs premium service"
}
```

**Expected Response:**

```json
{
    "statusCode": 200,
    "message": "Appointment created successfully",
    "data": {
        "appointmentId": "uuid",
        "status": "CONFIRMED",
        "agentId": "agent-uuid-here",
        "agentName": "Nguyen Van A",
        "message": "Appointment created and assigned to agent Nguyen Van A"
    }
}
```

**Note:** When an agent is assigned during creation, the status is automatically set to `CONFIRMED` instead of `PENDING`.

### Test Case 2.1.4: Create appointment with minimal data

```http
POST /appointment
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "propertyId": "123e4567-e89b-12d3-a456-426614174000",
  "requestedDate": "2024-12-20T14:00:00"
}
```

### Test Case 2.1.5: Invalid - Property not available

```http
POST /appointment
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "propertyId": "<sold_property_id>",
  "requestedDate": "2024-12-15T10:00:00"
}
```

**Expected Error Response:**

```json
{
    "statusCode": 400,
    "message": "Property is not available for viewing"
}
```

### Test Case 2.1.6: Invalid - Already has pending appointment

```http
POST /appointment
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "propertyId": "<property_with_existing_appointment>",
  "requestedDate": "2024-12-16T10:00:00"
}
```

**Expected Error Response:**

```json
{
    "statusCode": 400,
    "message": "You already have a pending or confirmed appointment for this property"
}
```

---

## 2.2 Cancel Appointment

**Endpoint:** `PATCH /appointment/{appointmentId}/cancel`  
**Roles:** CUSTOMER, SALESAGENT, ADMIN

**Description:** Changes the appointment status to `CANCELLED` instead of deleting it. This maintains audit trail and prevents data loss.

### Test Case 2.2.1: Customer cancels their appointment with reason

```http
PATCH /appointment/123e4567-e89b-12d3-a456-426614174000/cancel
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "reason": "Đổi lịch công tác"
}
```

### Test Case 2.2.2: Agent cancels assigned appointment

```http
PATCH /appointment/123e4567-e89b-12d3-a456-426614174000/cancel
Authorization: Bearer <agent_token>
Content-Type: application/json

{
  "reason": "Khách hàng không liên lạc được"
}
```

### Test Case 2.2.3: Cancel without reason (optional)

```http
PATCH /appointment/123e4567-e89b-12d3-a456-426614174000/cancel
Authorization: Bearer <customer_token>
Content-Type: application/json

{}
```

**Expected Response:**

```json
{
    "statusCode": 200,
    "message": "Appointment cancelled successfully",
    "data": true
}
```

**Note:** The appointment is soft-deleted by changing its status to `CANCELLED`. The record remains in the database for auditing purposes.

---

## 2.3 Get My Viewing Cards

**Endpoint:** `GET /appointment/viewing-cards`  
**Roles:** CUSTOMER

### Test Case 2.3.1: Get all my viewings

```http
GET /appointment/viewing-cards?page=1&limit=10
Authorization: Bearer <customer_token>
```

### Test Case 2.3.2: Filter by status

```http
GET /appointment/viewing-cards?statusEnum=PENDING&page=1&limit=10
Authorization: Bearer <customer_token>
```

### Test Case 2.3.3: Filter by date

```http
GET /appointment/viewing-cards?day=15&month=12&year=2024
Authorization: Bearer <customer_token>
```

---

## 2.4 Get Viewing Details

**Endpoint:** `GET /appointment/viewing-details/{id}`  
**Roles:** CUSTOMER

### Test Case 2.4.1: Get appointment details

```http
GET /appointment/viewing-details/123e4567-e89b-12d3-a456-426614174000
Authorization: Bearer <customer_token>
```

---

## 2.5 Rate Appointment

**Endpoint:** `PATCH /appointment/{appointmentId}/rate`  
**Roles:** CUSTOMER

### Test Case 2.5.1: Rate completed appointment

```http
PATCH /appointment/123e4567-e89b-12d3-a456-426614174000/rate
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "rating": 5,
  "comment": "Excellent service, agent was very professional and knowledgeable"
}
```

### Test Case 2.5.2: Rate without comment

```http
PATCH /appointment/123e4567-e89b-12d3-a456-426614174000/rate
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "rating": 4
}
```

**Expected Response:**

```json
{
    "statusCode": 200,
    "message": "Appointment rated successfully",
    "data": true
}
```

---

## 2.6 Admin - Get Viewing List

**Endpoint:** `GET /appointment/admin/viewing-list`  
**Roles:** ADMIN, SALESAGENT

### Test Case 2.6.1: Get all viewings with pagination

```http
GET /appointment/admin/viewing-list?page=1&limit=20&sortType=desc
Authorization: Bearer <admin_token>
```

### Test Case 2.6.2: Filter by status and date range

```http
GET /appointment/admin/viewing-list?statusEnums=PENDING,CONFIRMED&requestDateFrom=2024-12-01T00:00:00&requestDateTo=2024-12-31T23:59:59
Authorization: Bearer <admin_token>
```

### Test Case 2.6.3: Filter by property and customer

```http
GET /appointment/admin/viewing-list?propertyName=căn hộ&customerName=nguyen&page=1&limit=10
Authorization: Bearer <admin_token>
```

---

## 2.7 Admin/Agent - Get Viewing Details

**Endpoint:** `GET /appointment/admin-agent/viewing-details/{id}`  
**Roles:** ADMIN, SALESAGENT

### Test Case 2.7.1: Get detailed appointment info

```http
GET /appointment/admin-agent/viewing-details/123e4567-e89b-12d3-a456-426614174000
Authorization: Bearer <admin_token>
```

---

# 3. CONTRACT MODULE

## 3.1 Create Contract

**Endpoint:** `POST /contracts`  
**Roles:** ADMIN, SALESAGENT

### Test Case 3.1.1: Create purchase contract (mortgage)

```http
POST /contracts
Authorization: Bearer <agent_token>
Content-Type: application/json

{
  "propertyId": "123e4567-e89b-12d3-a456-426614174000",
  "customerId": "223e4567-e89b-12d3-a456-426614174001",
  "agentId": "323e4567-e89b-12d3-a456-426614174002",
  "contractType": "PURCHASE",
  "startDate": "2024-12-01",
  "endDate": "2026-12-01",
  "specialTerms": "Thanh toán theo tiến độ xây dựng",
  "contractPaymentType": "MORTGAGE",
  "totalContractAmount": 5000000000,
  "depositAmount": 500000000,
  "advancePaymentAmount": 1000000000,
  "installmentAmount": 24,
  "progressMilestone": 0.3,
  "latePaymentPenaltyRate": 0.05,
  "specialConditions": "Bàn giao nhà Q4/2025"
}
```

### Test Case 3.1.2: Create rental contract

```http
POST /contracts
Authorization: Bearer <agent_token>
Content-Type: application/json

{
  "propertyId": "123e4567-e89b-12d3-a456-426614174000",
  "customerId": "223e4567-e89b-12d3-a456-426614174001",
  "agentId": "323e4567-e89b-12d3-a456-426614174002",
  "contractType": "RENTAL",
  "startDate": "2024-12-01",
  "endDate": "2025-12-01",
  "specialTerms": "Thanh toán vào ngày 5 hàng tháng",
  "contractPaymentType": "MONTHLY_RENT",
  "totalContractAmount": 180000000,
  "depositAmount": 30000000,
  "latePaymentPenaltyRate": 0.02,
  "specialConditions": "Không được nuôi thú cưng"
}
```

---

## 3.2 Get Contract by ID

**Endpoint:** `GET /contracts/{contractId}`  
**Roles:** ADMIN, SALESAGENT, CUSTOMER

### Test Case 3.2.1: Get contract details

```http
GET /contracts/123e4567-e89b-12d3-a456-426614174000
Authorization: Bearer <token>
```

---

## 3.3 List Contracts

**Endpoint:** `GET /contracts`  
**Roles:** ADMIN, SALESAGENT

### Test Case 3.3.1: List all contracts

```http
GET /contracts?page=0&size=20
Authorization: Bearer <admin_token>
```

### Test Case 3.3.2: Filter by contract type and status

```http
GET /contracts?contractTypes=PURCHASE,RENTAL&statuses=ACTIVE
Authorization: Bearer <admin_token>
```

### Test Case 3.3.3: Filter by agent

```http
GET /contracts?agentId=<agent_uuid>
Authorization: Bearer <admin_token>
```

### Test Case 3.3.4: Search by contract number

```http
GET /contracts?search=PUR-173
Authorization: Bearer <admin_token>
```

### Test Case 3.3.5: Filter by date range

```http
GET /contracts?startDateFrom=2024-01-01&startDateTo=2024-12-31
Authorization: Bearer <admin_token>
```

---

## 3.4 Update Contract

**Endpoint:** `PUT /contracts/{contractId}`  
**Roles:** ADMIN, SALESAGENT

### Test Case 3.4.1: Update contract terms (DRAFT status only)

```http
PUT /contracts/123e4567-e89b-12d3-a456-426614174000
Authorization: Bearer <agent_token>
Content-Type: application/json

{
  "endDate": "2027-01-01",
  "specialTerms": "Điều khoản mới: Gia hạn thêm 1 tháng",
  "latePaymentPenaltyRate": 0.06,
  "specialConditions": "Cập nhật điều kiện thanh toán"
}
```

### Test Case 3.4.2: Move to pending signing status

```http
PUT /contracts/123e4567-e89b-12d3-a456-426614174000
Authorization: Bearer <agent_token>
Content-Type: application/json

{
  "status": "PENDING_SIGNING"
}
```

---

## 3.5 Sign Contract

**Endpoint:** `POST /contracts/{contractId}/sign`  
**Roles:** ADMIN, SALESAGENT

### Test Case 3.5.1: Sign contract

```http
POST /contracts/123e4567-e89b-12d3-a456-426614174000/sign
Authorization: Bearer <agent_token>
```

**Expected Response:**

```json
{
    "statusCode": 200,
    "message": "Contract signed successfully",
    "data": {
        "id": "uuid",
        "contractNumber": "PUR-17329845-00001",
        "status": "ACTIVE",
        "signedAt": "2024-12-01T15:30:00"
    }
}
```

---

## 3.6 Complete Contract

**Endpoint:** `POST /contracts/{contractId}/complete`  
**Roles:** ADMIN, SALESAGENT

### Test Case 3.6.1: Complete contract (all payments done)

```http
POST /contracts/123e4567-e89b-12d3-a456-426614174000/complete
Authorization: Bearer <agent_token>
```

### Test Case 3.6.2: Invalid - Unpaid payments exist

```http
POST /contracts/<contract_with_unpaid>/complete
Authorization: Bearer <agent_token>
```

**Expected Error Response:**

```json
{
    "statusCode": 400,
    "message": "Cannot complete contract with unpaid payments"
}
```

---

## 3.7 Cancel Contract

**Endpoint:** `POST /contracts/{contractId}/cancel`  
**Roles:** ADMIN, SALESAGENT, CUSTOMER

### Test Case 3.7.1: Cancel contract with penalty

```http
POST /contracts/123e4567-e89b-12d3-a456-426614174000/cancel
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "reason": "Thay đổi kế hoạch tài chính"
}
```

### Test Case 3.7.2: Admin cancels with waived penalty

```http
POST /contracts/123e4567-e89b-12d3-a456-426614174000/cancel
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "reason": "Hủy theo yêu cầu của chủ đầu tư",
  "waivePenalty": true
}
```

**Expected Response:**

```json
{
    "statusCode": 200,
    "message": "Contract cancelled successfully",
    "data": {
        "id": "uuid",
        "status": "CANCELLED",
        "cancellationReason": "Thay đổi kế hoạch tài chính",
        "cancellationPenalty": 850000000,
        "cancelledBy": "CUSTOMER"
    }
}
```

---

## 3.8 Calculate Cancellation Penalty (Preview)

**Endpoint:** `GET /contracts/{contractId}/penalty`  
**Roles:** ADMIN, SALESAGENT, CUSTOMER

### Test Case 3.8.1: Preview penalty before cancelling

```http
GET /contracts/123e4567-e89b-12d3-a456-426614174000/penalty
Authorization: Bearer <customer_token>
```

**Expected Response:**

```json
{
    "statusCode": 200,
    "message": "Cancellation penalty calculated",
    "data": 850000000
}
```

---

## 3.9 Get My Contracts (Customer)

**Endpoint:** `GET /contracts/my`  
**Roles:** CUSTOMER

### Test Case 3.9.1: Get customer's contracts

```http
GET /contracts/my?page=0&size=10
Authorization: Bearer <customer_token>
```

### Test Case 3.9.2: Filter by status

```http
GET /contracts/my?statuses=ACTIVE,COMPLETED
Authorization: Bearer <customer_token>
```

---

## 3.10 Get My Agent Contracts

**Endpoint:** `GET /contracts/agent/my`  
**Roles:** SALESAGENT

### Test Case 3.10.1: Get agent's assigned contracts

```http
GET /contracts/agent/my?page=0&size=10
Authorization: Bearer <agent_token>
```

### Test Case 3.10.2: Filter by status

```http
GET /contracts/agent/my?statuses=PENDING_SIGNING,ACTIVE
Authorization: Bearer <agent_token>
```

---

## 3.11 Rate Contract

**Endpoint:** `POST /contracts/{contractId}/rate`  
**Roles:** CUSTOMER

### Test Case 3.11.1: Rate completed contract

```http
POST /contracts/123e4567-e89b-12d3-a456-426614174000/rate?rating=5&comment=Dịch vụ rất tốt, nhân viên nhiệt tình
Authorization: Bearer <customer_token>
```

### Test Case 3.11.2: Rate without comment

```http
POST /contracts/123e4567-e89b-12d3-a456-426614174000/rate?rating=4
Authorization: Bearer <customer_token>
```

### Test Case 3.11.3: Invalid - Contract not completed

```http
POST /contracts/<active_contract_id>/rate?rating=5
Authorization: Bearer <customer_token>
```

**Expected Error Response:**

```json
{
    "statusCode": 400,
    "message": "Can only rate completed contracts"
}
```

---

# 4. ERROR RESPONSES

## Common Error Codes

### 400 Bad Request

```json
{
    "statusCode": 400,
    "message": "Validation error message"
}
```

### 401 Unauthorized

```json
{
    "statusCode": 401,
    "message": "Unauthorized"
}
```

### 403 Forbidden

```json
{
    "statusCode": 403,
    "message": "Access denied"
}
```

### 404 Not Found

```json
{
    "statusCode": 404,
    "message": "Resource not found: <id>"
}
```

---

# 5. TEST DATA SETUP

## Required Test Users

| Role           | Username            | Description                     |
| -------------- | ------------------- | ------------------------------- |
| ADMIN          | admin@test.com      | Full access                     |
| ACCOUNTANT     | accountant@test.com | Payment management              |
| SALESAGENT     | agent@test.com      | Contract & appointment handling |
| CUSTOMER       | customer@test.com   | Booking & viewing               |
| PROPERTY_OWNER | owner@test.com      | Property listings               |

## Required Test Entities

1. **Property** - At least one active property for booking
2. **SaleAgent** - For salary/bonus payments
3. **Customer** - For booking appointments
4. **Contract** - In various statuses (DRAFT, ACTIVE, COMPLETED)
5. **Payment** - Various types and statuses

---

# 6. POSTMAN COLLECTION STRUCTURE

```
📁 BatDongScam API Tests
├── 📁 Auth
│   ├── Login as Admin
│   ├── Login as Accountant
│   ├── Login as Agent
│   ├── Login as Customer
│   └── Login as Owner
│
├── 📁 Payment
│   ├── Get All Payments
│   ├── Get Payment by ID
│   ├── Update Payment Status
│   ├── Create Salary Payment
│   └── Create Bonus Payment
│
├── 📁 Appointment
│   ├── Create Appointment
│   ├── Cancel Appointment
│   ├── Get My Viewing Cards
│   ├── Get Viewing Details
│   ├── Rate Appointment
│   ├── [Admin] Get Viewing List
│   └── [Admin/Agent] Get Viewing Details
│
├── 📁 Contract
│   ├── Create Contract (Purchase)
│   ├── Create Contract (Rental)
│   ├── Get Contract by ID
│   ├── List Contracts
│   ├── Update Contract
│   ├── Sign Contract
│   ├── Cancel Contract
│   ├── Get My Contracts
│   ├── Get Agent Contracts
│   └── Rate Contract
│
├── 📁 Payment Flow (PayOS)
│   ├── Create Contract Payment Link
│   ├── Create Owner Refund Link
│   ├── Webhook Handler
│   └── Get Payment Link Status
│
└── 📁 Public
    ├── Get Public Properties
    ├── Get Property Details
    └── Search Properties
```
