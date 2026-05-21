### UC38: Manage Property Types
**Name**: Manage Property Types
**Description**: This use case describes how an Administrator manages the global list of property categories (e.g., Apartment, Villa, Office).
**Actor**: Admin
**Trigger**: ❖ When the Admin clicks the “Save” or “Delete” button in the property type settings.
**Pre-condition**: 
❖ The user is logged in as Admin.
**Post-condition**: 
❖ The property type definitions are created, updated, or removed.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Admin|
start
: (1) Create new or select existing [propertyType];
: (2) Input [name], [description], [icon];
: (3) Click "Save";
|System|
: (4) Show confirmation MSG 1;
if (Confirm?) then (yes)
  : (5) Validate uniqueness and non-empty;
  if (Valid?) then (yes)
    : (6) Save [property_type] record;
    |Admin|
    : (7) Show success message MSG 3;
    stop
  else (no)
    |Admin|
    : (8) Show error message (MSG 2 or 27);
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
boundary "Property Type UI" as UI
control "PropertyController" as API
control "PropertyService" as SVC
database "Database" as DB

Admin -> UI : Input type data
UI -> Admin : Prompt MSG 1
Admin -> UI : Confirm
UI -> API : POST /properties/types
API -> SVC : createPropertyType(request)
SVC -> DB : (BR107) SELECT count(*) FROM [property_type] WHERE name = [name]
alt Duplicate
    DB --> SVC : count > 0
    SVC --> API : throw Exception
    API --> UI : Error MSG 27
    UI --> Admin : Show MSG 27
else Unique
    SVC -> DB : INSERT INTO [property_type] ...
    SVC --> API : Success
    API --> UI : 201 Created
    UI --> Admin : Show MSG 3
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (5) | BR107 | **Validate Rules:**<br>When the Admin clicks on “Save”, the system will prompt a confirmation message (Refer to MSG 1). If Admin chooses Cancel, the system does nothing; else:<br>❖ If [name] is null or blank then show error message MSG 2.<br>❖ If [propertyTypeRepository.existsByName([name])] is true then show error message MSG 27. |
| (6) | BR108 | **Saving Rules:**<br>❖ [propertyType] = Property Type Repository save with all data.<br>❖ Property Type Repository save [propertyType] (call save() function). |
