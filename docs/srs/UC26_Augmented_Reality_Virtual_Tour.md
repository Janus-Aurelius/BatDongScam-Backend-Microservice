### UC26: Augmented Reality Virtual Tour
**Name**: Augmented Reality Virtual Tour
**Description**: This use case describes the process of initializing an Augmented Reality (AR) session for a 3D visualization of a property.
**Actor**: Customer
**Trigger**: ❖ When the user clicks on the “AR Tour” button.
**Pre-condition**: 
❖ The user is viewing property details on a mobile device.
❖ The property has a processed 3D model asset.
**Post-condition**: 
❖ An AR session is started on the device.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Customer|
start
: (1) Click "AR Tour";
|System|
: (2) Check for 3D asset existence;
if (Asset Found?) then (yes)
  : (3) Generate temporary signed URL for file;
  |Customer|
  : (4) Initialize device camera and AR session;
  : (5) Display 3D model and success message MSG 37;
  stop
else (no)
  |Customer|
  : (6) Show error message MSG 18;
  stop
endif
@enduml
```

```plantuml
@startuml
actor Customer
boundary "Mobile App" as UI
control "PropertyController" as API
control "AssetService" as SVC
entity "Storage" as CDN

Customer -> UI : Click AR Tour
UI -> API : GET /properties/{id}/ar-model
API -> SVC : getArModel(id)
SVC -> DB : (BR80) SELECT path FROM [property_media] WHERE type = '3D'
alt Not Found
    SVC --> API : throw Exception
    API --> UI : Error MSG 18
    UI --> Customer : Show MSG 18
else Found
    SVC -> CDN : (BR81) generateSignedUrl([path], <<expiry: 15m>>)
    CDN --> SVC : [url]
    SVC --> API : [url]
    API --> UI : 200 OK
    UI -> CDN : Download file
    UI -> UI : Start ARKit/ARCore Session
    UI --> Customer : Render Model & Show MSG 37
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (2) | BR80 | **Checking Rules:**<br>❖ If Property Media Repository find 3D asset by [propertyId] is null then show error message MSG 18. |
| (3) | BR81 | **Creating Rules:**<br>❖ [url] = Storage Service generateSignedUrl([asset.path], [expiration: 900 seconds]). |
| (5) | BR37 | **Message Rules:**<br>❖ The system shows success message MSG 37 ("AR session initialized"). |
