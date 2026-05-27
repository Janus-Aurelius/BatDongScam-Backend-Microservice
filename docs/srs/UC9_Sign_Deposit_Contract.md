### UC9: Sign Deposit Contract
**Name**: Sign Deposit Contract
**Description**: This use case describes the process by which a customer signs a draft deposit contract to reserve a property.
**Actor**: Customer
**Trigger**: ❖ When the user clicks on the “Sign Contract” button.
**Pre-condition**: 
❖ The user is logged in as the assigned customer of a 'DRAFT' deposit contract.
**Post-condition**: 
❖ The contract status is updated to 'WAITING_OFFICIAL'.
❖ The property status is updated to 'UNAVAILABLE'.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Customer|
start
: (1) Review draft contract details;
: (2) Click "Sign Contract";
|System|
: (3) Show confirmation MSG 1;
if (Confirm?) then (yes)
  : (4) Validate contract state;
  if (Valid?) then (yes)
    : (5) Update [deposit_contract] and [property] status;
    |Customer|
    : (6) Show success message MSG 3;
    stop
  else (no)
    |Customer|
    : (7) Show error message MSG 12;
    stop
  endif
else (no)
  stop
endif
@enduml
```

```plantuml
@startuml
actor Customer
boundary "Contract Detail UI" as UI
control "ContractController" as API
control "DepositContractService" as SVC
database "Database" as DB

Customer -> UI : Click Sign
UI -> Actor : Prompt MSG 1
Actor -> UI : Confirm
UI -> API : POST /contracts/deposit/{id}/approve
API -> SVC : approveDepositContract(id)
SVC -> DB : findById([contractId])
alt Invalid State
    SVC -> SVC : If status != 'DRAFT' throw Exception
    SVC --> API : throw Exception
    API --> UI : Error MSG 12
    UI --> Customer : Show Error
else Valid
    SVC -> DB : UPDATE [deposit_contract] SET status = 'WAITING_OFFICIAL'
    SVC -> DB : UPDATE [property] SET status = 'UNAVAILABLE'
    SVC --> API : Success
    API --> UI : 200 OK
    UI --> Customer : Show MSG 3
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (4) | BR34 | **Validate Rules:**<br>When the user clicks on “Sign Contract”, the system will prompt a confirmation message (Refer to MSG 1). If user chooses Cancel, the system does nothing; else:<br>❖ If [deposit_contract.status] != 'DRAFT' then the system shows error message MSG 12. |
| (5) | BR35 | **Saving Rules:**<br>❖ [deposit_contract.status] = 'WAITING_OFFICIAL'.<br>❖ [property.status] = 'UNAVAILABLE'.<br>❖ Deposit Contract Repository save [deposit_contract] (call save() function).<br>❖ Property Repository save [property] (call save() function). |
| (6) | BR3 | **Message Rules:**<br>❖ The system shows success message MSG 3. |
