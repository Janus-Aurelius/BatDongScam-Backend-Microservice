### UC19: Archive Property
**Name**: Archive Property
**Description**: This use case describes the process by which a Property Owner or Administrator soft-deletes a property listing from the platform.
**Actor**: Owner / Admin
**Trigger**: ❖ When the user clicks on the “Delete/Archive” button on a property listing.
**Pre-condition**: 
❖ The user is logged in.
❖ The user has ownership of the property or has administrative privileges.
**Post-condition**: 
❖ The property listing status is updated to 'DELETED'.
❖ The property is no longer visible in public searches.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Actor|
start
: (1) Click "Delete/Archive" on property card;
|System|
: (2) Show confirmation MSG 1;
if (Confirm?) then (yes)
  : (3) Perform ownership and state validation;
  if (Valid?) then (yes)
    : (4) Set [property.status] = 'DELETED';
    |Actor|
    : (5) Show success message MSG 3;
    stop
  else (no)
    |Actor|
    : (6) Show error message MSG 12;
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
boundary "Property Management UI" as UI
control "PropertyController" as API
control "PropertyService" as SVC
database "Database" as DB

Actor -> UI : Click Archive
UI -> Actor : Prompt MSG 1
Actor -> UI : Confirm
UI -> API : DELETE /properties/{id}
API -> SVC : deleteProperty(id)
SVC -> DB : (BR63) findById([propertyId])
DB --> SVC : [property]
SVC -> SVC : (BR63) Verify ownership or ADMIN role
alt Unauthorized
    SVC --> API : throw AccessDeniedException
    API --> UI : Error MSG 12
    UI --> Actor : Show MSG 12
else Authorized
    SVC -> DB : (BR64) UPDATE [property] SET status = 'DELETED', approved_at = NULL
    SVC --> API : Success
    API --> UI : 200 OK
    UI --> Actor : Show MSG 3
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (3) | BR63 | **Validate Rules:**<br>When the user clicks on “Delete/Archive”, the system will prompt a confirmation message (Refer to MSG 1). If user chooses Cancel, the system does nothing; else:<br>❖ If [property.status] == 'DELETED' then the system shows error message MSG 12.<br>❖ If <<current user role>> != 'ADMIN' AND [property.owner.userId] != <<current user id>> then the system shows error message MSG 12. |
| (4) | BR64 | **Delete Rules:**<br>❖ [property.status] = 'DELETED'.<br>❖ [property.approvedAt] = null.<br>❖ [property.assignedAgent] = null.<br>❖ Property Repository save [property] (call save() function). |
| (5) | BR3 | **Message Rules:**<br>❖ The system shows success message MSG 3. |
