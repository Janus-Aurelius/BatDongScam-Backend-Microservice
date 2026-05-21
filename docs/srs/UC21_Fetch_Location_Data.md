### UC21: Fetch Location Data
**Name**: Fetch Location Data
**Description**: This use case describes the retrieval of hierarchical location data (Provinces, Districts, Wards) for use in forms and filters.
**Actor**: User
**Trigger**: ❖ When the user interacts with a location selection component (e.g., dropdown).
**Pre-condition**: 
❖ None.
**Post-condition**: 
❖ The system returns a list of location items matching the requested level and parent ID.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|User|
start
: (1) Open location selector or select parent item;
|System|
: (2) Identify requested level (Province/District/Ward);
: (3) Retrieve child locations from cache or database;
|User|
: (4) Render items in dropdown list;
stop
@enduml
```

```plantuml
@startuml
actor User
boundary "Location Dropdown" as UI
control "PublicController" as API
control "LocationService" as SVC
database "Database" as DB

User -> UI : Select parent location
UI -> API : GET /public/locations/child?parentId={id}
API -> SVC : getChildLocations(parentId)
SVC -> SVC : (BR69) Check cache for [parentId]
alt Cache Hit
    SVC --> API : List<LocationDTO>
else Cache Miss
    SVC -> DB : (BR70) SELECT * FROM [location_table] WHERE parent_id = [parentId]
    DB --> SVC : Results
    SVC -> SVC : Update Cache
    SVC --> API : Results
end
API --> UI : 200 OK (Map<ID, Name>)
UI --> User : Display items
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (3) | BR69 | **Loading Rules:**<br>❖ The system checks the internal cache for [parentId]. If found, return results immediately without querying the database. |
| (3) | BR70 | **Retrieval Rules:**<br>❖ If cache miss then [results] = Location Repository find all by [parentId].<br>❖ Sort [results] alphabetically by name. |
