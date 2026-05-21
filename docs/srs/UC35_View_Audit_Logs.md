### UC35: View Audit Logs
**Name**: View Audit Logs
**Description**: This use case describes how an Administrator can view the history of sensitive system actions and state changes.
**Actor**: Admin
**Trigger**: ❖ When the Admin clicks on the “Audit Logs” menu item.
**Pre-condition**: 
❖ The user is logged in as Admin.
**Post-condition**: 
❖ The system displays a paginated list of audit records.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Admin|
start
: (1) Navigate to Audit module;
|System|
: (2) Retrieve paginated audit records;
: (3) Mask sensitive data in logs;
|Admin|
: (4) Display log entries and search filters;
stop
@enduml
```

```plantuml
@startuml
actor Admin
boundary "Audit UI" as UI
control "AuditController" as API
control "AuditService" as SVC
database "AuditDB" as DB

Admin -> UI : Open Audit Logs
UI -> API : GET /admin/audit (Filters)
API -> SVC : (BR98) findLogs(params)
SVC -> DB : (BR99) SELECT * FROM [audit_logs] WHERE ...
DB --> SVC : List<AuditLog>
SVC -> SVC : Mask passwords and secrets
SVC --> API : Page<AuditLogDTO>
API --> UI : 200 OK
UI --> Admin : Render Table
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (2) | BR98 | **Retrieval Rules:**<br>❖ [results] = Audit Repository find paginated by [filters] sorted by [timestamp] DESC. |
| (3) | BR99 | **Security Rules:**<br>❖ The system must automatically mask values in the [delta] JSON where [key] is in ['password', 'secret', 'token']. |
