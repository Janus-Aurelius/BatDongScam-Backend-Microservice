### UC12: Report Violation
**Name**: Report Violation
**Description**: This use case allows a user to report suspicious property listings or user behavior to the administration.
**Actor**: User
**Trigger**: ❖ When the user clicks on the “Submit Report” button.
**Pre-condition**: 
❖ The user is logged in to the system.
**Post-condition**: 
❖ A violation report is created in 'PENDING' status for admin review.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|User|
start
: (1) Select [violationType] and input [description], [evidence];
: (2) Click "Submit Report";
|System|
: (3) Show confirmation MSG 1;
if (Confirm?) then (yes)
  : (4) Validate input and reported entity;
  if (Valid?) then (yes)
    : (5) Create [violation] record;
    |User|
    : (6) Show success message MSG 3;
    stop
  else (no)
    |User|
    : (7) Show error message MSG 18;
    stop
  endif
else (no)
  stop
endif
@enduml
```

```plantuml
@startuml
actor User
boundary "Violation Form" as UI
control "ViolationController" as API
control "ViolationService" as SVC
database "Database" as DB

User -> UI : Submit form
UI -> User : Prompt MSG 1
User -> UI : Confirm
UI -> API : POST /violations (Multipart)
API -> SVC : createViolationReport(request)
SVC -> DB : (BR43) SELECT count(*) FROM [target_table] WHERE id = [reportedEntityId]
alt Entity Not Found
    DB --> SVC : count == 0
    SVC --> API : throw Exception
    API --> UI : Error MSG 18
    UI --> User : Show MSG 18
else Valid
    SVC -> DB : INSERT [violation] SET status = 'PENDING'
    SVC --> API : Success
    API --> UI : 201 Created
    UI --> User : Show MSG 3
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (4) | BR43 | **Validate Rules:**<br>When the user clicks on “Submit Report”, the system will prompt a confirmation message (Refer to MSG 1). If user chooses Cancel, the system does nothing; else:<br>❖ The system checks [reportedEntityId], [reportedType].<br>❖ If [reportedType] == 'PROPERTY' and Property Repository find by [reportedEntityId] is null then show error message MSG 18.<br>❖ If [reportedType] == 'USER' and User Repository find by [reportedEntityId] is null then show error message MSG 18. |
| (5) | BR44 | **Creating Rules:**<br>❖ [violation] = Violation Repository save new report.<br>❖ [violation.status] = 'PENDING'.<br>❖ [violation.reporterId] = <<current user id retrieved from jwt>>.<br>❖ [violation.evidencePaths] = Cloudinary Service upload [evidence] files. |
| (6) | BR3 | **Message Rules:**<br>❖ The system shows success message MSG 3. |
