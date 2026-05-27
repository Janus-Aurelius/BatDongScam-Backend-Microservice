### UC3: Create Property Listing
**Name**: Create Property Listing
**Description**: This use case allows Owners or Admins to create a property listing with images and documents.
**Actor**: Owner / Admin
**Trigger**: ❖ When the user clicks on the “Submit Listing” button.
**Pre-condition**: 
❖ The user is logged in to the system.
❖ The user is in the create property page.
**Post-condition**: 
❖ The property listing has been created.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Actor|
start
: (1) Provide [title], [priceAmount], [area], [wardId], [propertyTypeId], [images], [documents];
: (2) Click "Save";
|System|
: (3) Show confirmation MSG 1;
if (Confirm?) then (yes)
  : (4) Perform data validation;
  if (Valid?) then (yes)
    : (5) Process uploads and calculate fees;
    : (6) Save property and link media;
    |Actor|
    : (7) Show success message MSG 3;
    stop
  else (no)
    |Actor|
    : (8) Show error message (MSG 2, 11, 12, 13, 14);
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
boundary "Create Property Form" as UI
control "PropertyController" as API
control "PropertyService" as SVC
database "Database" as DB

Actor -> UI : Click Save
UI -> Actor : Prompt MSG 1
Actor -> UI : Confirm
UI -> API : POST /properties
API -> SVC : createProperty(request)
SVC -> SVC : (BR19) Validate inputs
alt Validation Fails
    SVC --> API : throw Exception
    API --> UI : Error MSG X
    UI --> Actor : Show Error
else Validation Success
    SVC -> DB : calculate commission and INSERT [property]
    SVC -> DB : INSERT [media] & [document]
    SVC --> API : PropertyDetails
    API --> UI : 201 Created
    UI --> Actor : Show MSG 3
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (1) | BR18 | **Loading Screen Rules:**<br>❖ The system loads the “Create Property” screen. |
| (3) | BR19 | **Creating Rules:**<br>When the user clicks on “Save”, the system will prompt a confirmation message (Refer to MSG 1). If user chooses Cancel, the system does nothing; else:<br>❖ The system checks the items [images], [title], [priceAmount], [area], [wardId], [propertyTypeId].<br>❖ If any entries are empty, the system shows an error message MSG 2.<br>❖ If size of any in [images] > 5.MB then system shows error message MSG 11.<br>❖ If [priceAmount] < 0 then the system shows error message MSG 13.<br>❖ [property.serviceFeeAmount] = [priceAmount] * Constants.DEFAULT_PROPERTY_COMMISSION_RATE.<br>❖ If Actor is ADMIN then [status] = 'AVAILABLE' and [approvedAt] = <<now>> else [status] = 'PENDING'.<br>❖ [ownerId] = <<current user id retrieved from jwt>>. |
| (7) | BR20 | **Message Rules:**<br>❖ The system shows success message MSG 3. |
