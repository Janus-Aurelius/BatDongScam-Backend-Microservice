### UC11: Handle Payment Webhook
**Name**: Handle Payment Webhook
**Description**: This use case describes how the system processes asynchronous payment confirmation notifications from external gateways.
**Actor**: System
**Trigger**: ❖ When the external gateway sends a POST request to the webhook endpoint.
**Pre-condition**: 
❖ The system endpoint is public and the gateway has a valid signed payload.
**Post-condition**: 
❖ The payment status is updated to 'SUCCESS'.
❖ The associated contract status is updated to 'ACTIVE'.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Gateway|
start
: (1) Send POST webhook payload;
|System|
: (2) Verify HMAC signature;
if (Signature Valid?) then (yes)
  : (3) Identify payment and update status;
  : (4) Transition associated contract;
  |Gateway|
  : (5) Return 200 OK (Successful callback);
  stop
else (no)
  |Gateway|
  : (6) Return 401 Unauthorized (Error callback);
  stop
endif
@enduml
```

```plantuml
@startuml
actor "Payment Gateway" as GW
control "PaywayWebhookController" as API
control "PaymentService" as SVC
database "Database" as DB

GW -> API : POST /external/payway/webhook
API -> API : (BR40) Verify Signature
alt Invalid
    API --> GW : 401 Unauthorized
else Valid
    API -> SVC : handleWebhook(payload)
    SVC -> DB : (BR41) findByOrderCode([payload.orderCode])
    DB --> SVC : [payment]
    SVC -> DB : UPDATE [payment] SET status = 'SUCCESS'
    SVC -> DB : UPDATE [contract] SET status = 'ACTIVE'
    SVC --> API : void
    API --> GW : 200 OK
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (2) | BR40 | **Validate Rules:**<br>❖ If hash_hmac('sha256', [payload], [secret]) != [header.signature] then return 401-UNAUTHORIZED. |
| (3) | BR41 | **Updating Rules:**<br>❖ [payment] = Payment Repository find by [payload.orderCode].<br>❖ If [payment.status] == 'SUCCESS' then return 200 OK (idempotent skip).<br>❖ [payment.status] = 'SUCCESS'.<br>❖ [payment.gatewayTransactionId] = [payload.transactionId].<br>❖ Payment Repository save [payment]. |
| (4) | BR42 | **Updating Rules:**<br>❖ [contract] = [payment.contract].<br>❖ [contract.status] = 'ACTIVE'.<br>❖ Contract Repository save [contract]. |
