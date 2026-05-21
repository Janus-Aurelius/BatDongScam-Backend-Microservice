### UC10: Initialize Payment
**Name**: Initialize Payment
**Description**: This use case describes the process by which a user initiates a payment for a contract or service via an external payment gateway.
**Actor**: User
**Trigger**: ❖ When the user clicks on the “Pay Now” button.
**Pre-condition**: 
❖ The user is logged in to the system.
❖ The target contract is in a state that allows payment (e.g., 'WAITING_OFFICIAL').
**Post-condition**: 
❖ A payment record is created in 'PENDING' status.
❖ The user is redirected to the external payment gateway.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|User|
start
: (1) Click "Pay Now" on a contract;
|System|
: (2) Check for existing pending payments;
if (Duplicate Found?) then (yes)
  |User|
  : (3) Show error message MSG 12;
  stop
else (no)
  |System|
  : (4) Calculate amount and create [payment] record;
  : (5) Call Gateway API to get [redirectUrl];
  |User|
  : (6) Show success message MSG 34 and Redirect;
  stop
endif
@enduml
```

```plantuml
@startuml
actor User
boundary "Payment UI" as UI
control "PaymentController" as API
control "PaymentService" as SVC
control "GatewayAPI" as GW
database "Database" as DB

User -> UI : Click Pay
UI -> API : POST /contracts/{id}/payment
API -> SVC : createPayment(contractId)
SVC -> DB : SELECT count(*) FROM [payment] WHERE [contractId] AND status='PENDING'
alt Duplicate
    DB --> SVC : count > 0
    SVC --> API : throw Exception
    API --> UI : Error MSG 12
    UI --> User : Show MSG 12
else Valid
    SVC -> DB : INSERT [payment] SET status='PENDING'
    SVC -> GW : POST /create-session (amount, orderCode)
    GW --> SVC : [redirectUrl]
    SVC --> API : [redirectUrl]
    API --> UI : 200 OK
    UI --> User : Show MSG 34 & Redirect to Gateway
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (2) | BR37 | **Validate Rules:**<br>❖ If [paymentRepository.countByContractAndStatus([contractId], 'PENDING')] > 0 then the system shows error message MSG 12 ("Payment already in progress"). |
| (4) | BR38 | **Creating Rules:**<br>❖ [payment.amount] = [contract.depositAmount].<br>❖ [payment.status] = 'PENDING'.<br>❖ [payment.dueDate] = <<current date time>> + 3 days.<br>❖ Payment Repository save [payment] (call save() function). |
| (5) | BR39 | **Gateway Rules:**<br>❖ [redirectUrl] = Gateway API generate session with [orderCode] = [payment.id]. |
| (6) | BR34 | **Redirect Rules:**<br>❖ The system shows success message MSG 34 and redirects to [redirectUrl]. |
