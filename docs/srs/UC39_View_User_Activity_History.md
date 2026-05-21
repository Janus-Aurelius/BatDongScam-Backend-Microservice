### UC39: View User Activity History
**Name**: View User Activity History
**Description**: This use case describes how users can view a paginated list of their personal appointments, contracts, and payments.
**Actor**: User
**Trigger**: ❖ When the user navigates to a historical activity page (e.g., “My Appointments”).
**Pre-condition**: 
❖ The user is logged in to the system.
**Post-condition**: 
❖ A list of records associated with the user is displayed.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|User|
start
: (1) Select activity type (Appointments/Contracts/Payments);
|System|
: (2) Retrieve user-specific records with pagination;
if (Records Found?) then (yes)
  : (3) Format records for summary view;
  |User|
  : (4) Display list and success message MSG 32;
  stop
else (no)
  |User|
  : (5) Show message MSG 40;
  stop
endif
@enduml
```

```plantuml
@startuml
actor User
boundary "Activity List UI" as UI
control "ActivityController" as API
control "ActivityService" as SVC
database "Database" as DB

User -> UI : Open "My Appointments"
UI -> API : GET /appointment/me
API -> SVC : getMyAppointments(pageable)
SVC -> DB : (BR109) SELECT * FROM [appointment] WHERE customer_id = [me]
DB --> SVC : Page<Appointment>
SVC --> API : Success
API --> UI : 200 OK
UI --> User : Render List & MSG 32
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (2) | BR109 | **Loading Rules:**<br>❖ [results] = Repository find all by [currentUserId] sorted by [createdAt] DESC.<br>❖ Filter results based on selected status enums if provided. |
| (4) | BR32 | **Message Rules:**<br>❖ The system shows success message MSG 32. |
| (5) | BR40 | **Message Rules:**<br>❖ The system shows informational message MSG 40. |
