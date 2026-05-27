### UC4: Search Properties
**Name**: Search Properties
**Description**: This use case describes the process by which a user searches for properties using various filters such as location, price, and property type.
**Actor**: User
**Trigger**: ❖ When the user clicks on the “Search” button or applies a filter.
**Pre-condition**: 
❖ The user is on the property discovery page.
**Post-condition**: 
❖ The system displays a list of properties matching the search criteria.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|User|
start
: (1) Input search criteria (location, price range, property type);
: (2) Click "Search";
|System|
: (3) Validate search parameters;
if (Criteria Valid?) then (yes)
  : (4) Execute dynamic query;
  : (5) Map results to property cards;
  |User|
  : (6) Display results and success message MSG 32;
  stop
else (no)
  |User|
  : (7) Show error message MSG 13;
  stop
endif
@enduml
```

```plantuml
@startuml
actor User
boundary "Search UI" as UI
control "PublicController" as API
control "PropertyService" as SVC
database "Database" as DB

User -> UI : Apply filters
UI -> API : GET /properties/cards (Query Params)
API -> SVC : getAllCardsWithFilters(params)
SVC -> SVC : (BR17) Validate price/area ranges
alt Validation Fails
    SVC --> API : throw Exception
    API --> UI : Error MSG 13
    UI --> User : Show Error
else Validation Success
    SVC -> DB : SELECT * FROM [property] WHERE ...
    DB --> SVC : List<Property>
    SVC --> API : Page<PropertyCard>
    API --> UI : 200 OK
    UI --> User : Render Results & Show MSG 32
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (1) | BR15 | **Loading Screen Rules:**<br>❖ The system loads the “Property Search” interface. |
| (3) | BR17 | **Validate Rules:**<br>❖ If [minPrice] > [maxPrice] then the system shows an error message MSG 13.<br>❖ If [minArea] > [maxArea] then the system shows an error message MSG 13.<br>❖ If all filters are null or blank the system will show an error message MSG 2.<br>❖ [results] = Property Repository find by criteria where [status] = 'AVAILABLE'. |
| (6) | BR32 | **Message Rules:**<br>❖ The system shows success message MSG 32 ("Search completed successfully"). |
