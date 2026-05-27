### UC5: View Property Details
**Name**: View Property Details
**Description**: This use case describes the process by which a user views the comprehensive information of a specific property.
**Actor**: User
**Trigger**: ❖ When the user clicks on a property card.
**Pre-condition**: 
❖ The user is viewing a list of properties.
**Post-condition**: 
❖ The system displays the full details, media, and contact information for the selected property.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|User|
start
: (1) Select a property card;
|System|
: (2) Retrieve property data by ID;
if (Property Exists?) then (yes)
  : (3) Aggregate details, media, and documents;
  |User|
  : (4) Display details screen;
  stop
else (no)
  |User|
  : (5) Show error message MSG 18;
  stop
endif
@enduml
```

```plantuml
@startuml
actor User
boundary "Property Detail UI" as UI
control "PublicController" as API
control "PropertyService" as SVC
database "Database" as DB

User -> UI : Click property card
UI -> API : GET /properties/{id}
API -> SVC : getPropertyDetailsById(id)
SVC -> DB : findById([propertyId])
alt Not Found
    DB --> SVC : null
    SVC --> API : throw NotFoundException
    API --> UI : Error MSG 18
    UI --> User : Show MSG 18
else Found
    DB --> SVC : [property]
    SVC --> API : PropertyDetails
    API --> UI : 200 OK
    UI --> User : Render Details Screen
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (2) | BR26 | **Checking Rules:**<br>❖ If [propertyId] does not exist, the system shows an error message MSG 18 else [property] = Property Repository find by [propertyId] (call findById() function). |
| (3) | BR19 | **Retrieval Rules:**<br>❖ The system fetches [images] from [media] table where [propertyId] matches.<br>❖ The system fetches [documents] from [document] table where [propertyId] matches.<br>❖ The system fetches [ownerTier] and [agentTier] from Ranking Service. |
