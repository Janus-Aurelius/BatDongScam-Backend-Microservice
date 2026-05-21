### UC36: Update Property
**Name**: Update Property
**Description**: This use case describes the process by which a Property Owner or Administrator updates the information and media of an existing property listing.
**Actor**: Owner / Admin
**Trigger**: ❖ When the user clicks the “Save” button in the property edit form.
**Pre-condition**: 
❖ The user is logged in.
❖ The user owns the property or has administrative privileges.
**Post-condition**: 
❖ The property record is updated in the database.
❖ If updated by an Owner, the property status may be reset to 'PENDING'.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Actor|
start
: (1) Modify property details, images, or documents;
: (2) Click "Save";
|System|
: (3) Show confirmation MSG 1;
if (Confirm?) then (yes)
  : (4) Validate input data and ownership;
  if (Valid?) then (yes)
    : (5) Process media uploads and update [property];
    if (Actor is OWNER?) then (yes)
      : (6) Set [property.status] = 'PENDING';
    else (no)
      : (7) Keep [property.status] = 'AVAILABLE';
    endif
    |Actor|
    : (8) Show success message MSG 3;
    stop
  else (no)
    |Actor|
    : (9) Show error message (MSG 2, 11, 12, or 13);
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
boundary "Edit Property Form" as UI
control "PropertyController" as API
control "PropertyService" as SVC
database "Database" as DB

Actor -> UI : Submit modifications
UI -> Actor : Prompt MSG 1
Actor -> UI : Confirm
UI -> API : PUT /properties/{id}
API -> SVC : updateProperty(id, request)
SVC -> DB : (BR63) findById([propertyId])
DB --> SVC : [property]
SVC -> SVC : (BR101) Verify data constraints
alt Validation Fails
    SVC --> API : throw Exception
    API --> UI : Error MSG X
    UI --> Actor : Show Error
else Valid
    alt Actor is Owner
        SVC -> DB : (BR102) UPDATE [property] SET status='PENDING', ...
    else Actor is Admin
        SVC -> DB : (BR103) UPDATE [property] SET ...
    end
    SVC --> API : Success
    API --> UI : 200 OK
    UI --> Actor : Show MSG 3
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (4) | BR101 | **Validate Rules:**<br>When the user clicks on “Save”, the system will prompt a confirmation message (Refer to MSG 1). If user chooses Cancel, the system does nothing; else:<br>❖ The system checks the items [title], [priceAmount], [area].<br>❖ If any mandatory entries are empty, the system shows an error message MSG 2.<br>❖ If [priceAmount] < 0 then the system shows error message MSG 13. |
| (6) | BR102 | **Status Rules:**<br>❖ If <<current user role>> == 'PROPERTY_OWNER' then [property.status] = 'PENDING'.<br>❖ Property Repository save [property]. |
| (7) | BR103 | **Status Rules:**<br>❖ If <<current user role>> == 'ADMIN' then [property.status] remains unchanged.<br>❖ Property Repository save [property]. |
| (8) | BR3 | **Message Rules:**<br>❖ The system shows success message MSG 3. |
