### UC20: Assign Agent to Property
**Name**: Assign Agent to Property
**Description**: This use case describes how an Administrator assigns a Sales Agent to manage a specific property listing.
**Actor**: Admin
**Trigger**: ❖ When the Admin selects an agent and confirms the assignment.
**Pre-condition**: 
❖ The user is logged in as Admin.
**Post-condition**: 
❖ The property record is updated with the assigned Sales Agent ID.
❖ The Sales Agent is notified of the assignment.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Admin|
start
: (1) Select [agentId] for [propertyId];
: (2) Click "Assign Agent";
|System|
: (3) Show confirmation MSG 1;
if (Confirm?) then (yes)
  : (4) Validate Admin role and entity existence;
  if (Valid?) then (yes)
    : (5) Update [property.assignedAgent] and log history;
    : (6) Dispatch system notification;
    |Admin|
    : (7) Show success message MSG 3;
    stop
  else (no)
    |Admin|
    : (8) Show error message MSG 18;
    stop
  endif
else (no)
  stop
endif
@enduml
```

```plantuml
@startuml
actor Admin
boundary "Admin Assignment UI" as UI
control "PropertyController" as API
control "PropertyService" as SVC
control "NotificationService" as NOTI
database "Database" as DB

Admin -> UI : Select agent and click Assign
UI -> Admin : Prompt MSG 1
Admin -> UI : Confirm
UI -> API : POST /properties/{id}/assign-agent?agentId={id}
API -> SVC : assignAgentToProperty(propId, agentId)
SVC -> SVC : (BR66) Verify ADMIN role
SVC -> DB : (BR66) Check existence of [property] and [agent]
alt Not Found
    SVC --> API : throw Exception
    API --> UI : Error MSG 18
    UI --> Admin : Show MSG 18
else Found
    SVC -> DB : UPDATE [property] SET agent_id = [agentId]
    SVC -> DB : (BR67) INSERT INTO [assignment_history] ...
    SVC -> NOTI : createNotification([agentId], 'PROPERTY_ASSIGNED')
    SVC --> API : Success
    API --> UI : 200 OK
    UI --> Admin : Show MSG 3
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (4) | BR66 | **Validate Rules:**<br>When the Admin clicks on “Assign Agent”, the system will prompt a confirmation message (Refer to MSG 1). If Admin chooses Cancel, the system does nothing; else:<br>❖ The system checks the items [propertyId], [agentId].<br>❖ If [propertyRepository.findById([propertyId])] is null or [userRepository.findById([agentId])] is null then show error message MSG 18. |
| (5) | BR66_B | **Updating Rules:**<br>❖ [property.assignedAgent] = [agent].<br>❖ Property Repository save [property]. |
| (5) | BR67 | **Saving Rules:**<br>❖ [history] = new PropertyAssignmentHistory().<br>❖ [history.propertyId] = [propertyId], [history.agentId] = [agentId], [history.assignedBy] = <<me>>.<br>❖ Assignment History Repository save [history]. |
| (7) | BR3 | **Message Rules:**<br>❖ The system shows success message MSG 3. |
