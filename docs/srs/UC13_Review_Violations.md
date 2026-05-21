### UC13: Review Violations
**Name**: Review Violations
**Description**: This use case describes how an Administrator reviews a reported violation and applies disciplinary actions if necessary.
**Actor**: Admin
**Trigger**: ❖ When the Admin clicks on the “Resolve/Update” button on a violation report.
**Pre-condition**: 
❖ The Admin is logged in. 
❖ The violation report exists and is in 'PENDING' or 'UNDER_REVIEW' status.
**Post-condition**: 
❖ The violation report status is updated to 'RESOLVED' or 'DISMISSED'. 
❖ Disciplinary actions are applied to the target entity (user or property).

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Admin|
start
: (1) Select a violation report and input [resolutionNotes], [penaltyApplied], [newStatus];
: (2) Click "Update";
|System|
: (3) Show confirmation MSG 1;
if (Confirm?) then (yes)
  : (4) Validate status transition and penalty;
  : (5) Update [violation] record;
  if ([penaltyApplied] == 'REMOVED_POST'?) then (yes)
    : (6) Set [property.status] = 'REMOVED';
  else if ([penaltyApplied] == 'SUSPENDED_ACCOUNT'?) then (yes)
    : (7) Set [user.status] = 'SUSPENDED';
  endif
  |Admin|
  : (8) Show success message MSG 3;
  stop
else (no)
  stop
endif
@enduml
```

```plantuml
@startuml
actor Admin
boundary "Violation Review UI" as UI
control "ViolationController" as API
control "ViolationService" as SVC
database "Database" as DB

Admin -> UI : Submit resolution details
UI -> Admin : Prompt MSG 1
Admin -> UI : Confirm
UI -> API : PUT /violations/admin/{id}
API -> SVC : updateViolationReport(id, request)
SVC -> DB : (BR46) findById([violationId])
DB --> SVC : [violation]
SVC -> DB : UPDATE [violation] SET status = [newStatus], resolution_notes = [notes]
alt Penalty == REMOVED_POST
    SVC -> DB : (BR47) UPDATE [property] SET status = 'REMOVED'
else Penalty == SUSPENDED_ACCOUNT
    SVC -> DB : (BR48) UPDATE [user] SET status = 'SUSPENDED'
end
SVC --> API : Success
API --> UI : 200 OK
UI --> Admin : Show MSG 3
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (4) | BR46 | **Validate Rules:**<br>When the Admin clicks on “Update”, the system will prompt a confirmation message (Refer to MSG 1). If Admin chooses Cancel, the system does nothing; else:<br>❖ If [violation.status] is 'RESOLVED' or 'DISMISSED' then the system shows error message MSG 12.<br>❖ [violation] = Violation Repository find by [violationId] (call findById() function). |
| (5) | BR46_B | **Updating Rules:**<br>❖ [violation.status] = [newStatus].<br>❖ [violation.resolutionNotes] = [resolutionNotes].<br>❖ Violation Repository save [violation]. |
| (6) | BR47 | **Penalty Rules:**<br>❖ If [penaltyApplied] == 'REMOVED_POST' then [property] = Property Repository find by [violation.reportedEntityId].<br>❖ [property.status] = 'REMOVED'.<br>❖ Property Repository save [property]. |
| (7) | BR48 | **Penalty Rules:**<br>❖ If [penaltyApplied] == 'SUSPENDED_ACCOUNT' then [user] = User Repository find by [violation.reportedEntityId].<br>❖ [user.status] = 'SUSPENDED'.<br>❖ User Repository save [user]. |
| (8) | BR3 | **Message Rules:**<br>❖ The system shows success message MSG 3. |
