### UC40: View Activity Details
**Name**: View Activity Details
**Description**: This use case describes the retrieval of full technical and legal details for a specific appointment or contract.
**Actor**: User
**Trigger**: ❖ When the user clicks on an item in an activity history list.
**Pre-condition**: 
❖ The user is logged in.
❖ The user is authorized to view the specific record.
**Post-condition**: 
❖ Detailed data, including linked property and documents, is displayed.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|User|
start
: (1) Select record ID from list;
|System|
: (2) Retrieve record by ID;
if (Authorized?) then (yes)
  : (3) Aggregate linked property, party, and document data;
  |User|
  : (4) Display detailed view;
  stop
else (no)
  |User|
  : (5) Show error message MSG 12;
  stop
endif
@enduml
```

```plantuml
@startuml
actor User
boundary "Detail View UI" as UI
control "ActivityController" as API
control "ActivityService" as SVC
database "Database" as DB

User -> UI : Click record {id}
UI -> API : GET /contracts/{id}
API -> SVC : getContractDetails(id)
SVC -> DB : (BR110) findById([id])
DB --> SVC : [contract]
SVC -> SVC : (BR110) Verify [contract.participantId] == [me] or role == 'ADMIN'
alt Unauthorized
    SVC --> API : throw AccessDeniedException
    API --> UI : Error MSG 12
    UI --> User : Show MSG 12
else Authorized
    SVC -> DB : SELECT * FROM [media/documents] WHERE related_id = [id]
    SVC --> API : FullDetailDTO
    API --> UI : 200 OK
    UI --> User : Render Details & Linked Docs
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (2) | BR110 | **Validate Rules:**<br>❖ If [record] does not exist then show error message MSG 18.<br>❖ If <<current user role>> != 'ADMIN' AND [record.userId] != <<current user id>> then show error message MSG 12. |
| (3) | BR111 | **Loading Rules:**<br>❖ The system performs a join or separate fetch for all [documents] linked to the primary entity ID. |
