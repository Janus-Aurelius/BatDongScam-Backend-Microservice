### UC8: Create Rental Contract Draft
**Name**: Create Rental Contract Draft
**Description**: This use case allows an Admin or Agent to generate a draft for a rental contract using property and party information.
**Actor**: Admin / Agent
**Trigger**: ❖ When the user clicks on the “Create Draft” button.
**Pre-condition**: 
❖ The user is logged in as Admin or Agent.
❖ No active rental contract exists for the target property.
**Post-condition**: 
❖ A new rental contract record is created in 'DRAFT' status.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Actor|
start
: (1) Input [leaseDuration], [rentAmount], [tenantId];
: (2) Click "Create Draft";
|System|
: (3) Show confirmation MSG 1;
if (Confirm?) then (yes)
  : (4) Validate property and existing contracts;
  if (Valid?) then (yes)
    : (5) Create [rental_contract] record;
    |Actor|
    : (6) Show success message MSG 3;
    stop
  else (no)
    |Actor|
    : (7) Show error message (MSG 2, 12, or 18);
    stop
  endif
else (no)
  stop
endif
@enduml
```

```plantuml
@startuml
actor Actor
boundary "Contract Draft UI" as UI
control "ContractController" as API
control "RentalContractService" as SVC
database "Database" as DB

Actor -> UI : Submit form
UI -> Actor : Prompt MSG 1
Actor -> UI : Confirm
UI -> API : POST /contracts/rental
API -> SVC : createRentalContract(request)
SVC -> DB : SELECT count(*) FROM [rental_contract] WHERE [propertyId] AND status != 'DRAFT'
alt Conflict Exists
    DB --> SVC : count > 0
    SVC --> API : throw Exception
    API --> UI : Error MSG 12
    UI --> Actor : Show Error
else No Conflict
    SVC -> DB : INSERT [rental_contract] SET status = 'DRAFT'
    SVC --> API : Success
    API --> UI : 201 Created
    UI --> Actor : Show MSG 3
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (4) | BR31 | **Validate Rules:**<br>When the user clicks on “Create Draft”, the system will prompt a confirmation message (Refer to MSG 1). If user chooses Cancel, the system does nothing; else:<br>❖ The system checks the items [leaseDuration], [rentAmount], [tenantId], [propertyId].<br>❖ If any entries are empty, the system shows an error message MSG 2.<br>❖ If [rentAmount] < 0 then the system shows error message MSG 13.<br>❖ If [rentalContractRepository.existsActiveContract([propertyId])] is true then the system shows error message MSG 12.<br>❖ If [depositContractId] is provided and [depositContract.status] != 'ACTIVE' then show error message MSG 12. |
| (5) | BR33 | **Saving Rules:**<br>❖ [rental_contract] = Rental Contract Repository save new contract with data (call save() function).<br>❖ [rental_contract.status] = 'DRAFT'.<br>❖ Populate [propertyInfo], [ownerInfo], [agentInfo] from respective repositories. |
| (6) | BR3 | **Message Rules:**<br>❖ The system shows success message MSG 3. |
