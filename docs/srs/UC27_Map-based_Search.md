### UC27: Map-based Search
**Name**: Map-based Search
**Description**: This use case describes how a user can find properties by drawing an area on a map or navigating the map view.
**Actor**: User
**Trigger**: ❖ When the user interacts with the map (draws area or pans view).
**Pre-condition**: 
❖ The user is on the property search page.
**Post-condition**: 
❖ Property markers within the selected area are displayed on the map.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|User|
start
: (1) Draw search area or move map;
|System|
: (2) Capture coordinate bounds;
: (3) Perform geospatial database query;
: (4) Group results into clusters;
|User|
: (5) Display markers and update result list;
stop
@enduml
```

```plantuml
@startuml
actor User
boundary "Map UI" as UI
control "SearchController" as API
control "GeospatialService" as SVC
database "Database" as DB

User -> UI : Draw Area
UI -> API : POST /search/map (Polygon)
API -> SVC : searchByPolygon(polygon)
SVC -> DB : (BR82) findWithinPolygon([polygon]) WHERE status = 'AVAILABLE'
DB --> SVC : List<Property>
SVC -> SVC : (BR83) Cluster(markers, zoomLevel)
SVC --> API : ClusterData
API --> UI : 200 OK
UI --> User : Render Markers
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (3) | BR82 | **Validate Rules:**<br>❖ [results] = Property Repository find within [polygon] WHERE [status] = 'AVAILABLE'. |
| (4) | BR83 | **Creating Rules:**<br>❖ If [zoomLevel] < 12 then the system groups nearby [markers] into [clusters] to optimize rendering. |
