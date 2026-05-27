### UC16: Generate Sales Report
**Name**: Generate Sales Report
**Description**: This use case describes how an Administrator can generate financial and transaction reports for a specific year.
**Actor**: Admin
**Trigger**: ❖ When the user clicks the "Generate Report" button.
**Pre-condition**: 
❖ The user is logged in as Admin.
**Post-condition**: 
❖ The system displays the financial and transaction statistics for the requested year.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Admin|
start
: (1) Select [year] from the dropdown;
: (2) Click "Generate Report";
|System|
: (3) Validate inputs and role;
if (Valid?) then (yes)
  : (4) Aggregate financial data;
  |Admin|
  : (5) Display charts and data tables;
  stop
else (no)
  |Admin|
  : (6) Show error message MSG 2;
  stop
endif
@enduml
```

```plantuml
@startuml
actor Admin
boundary "Admin Dashboard UI" as UI
control "StatisticReportController" as API
control "ReportService" as SVC
database "Database" as DB

Admin -> UI : Select year and click Generate
UI -> API : GET /statistic-report/financial/{year}
API -> SVC : getFinancialStats(year)
SVC -> SVC : (BR56) Verify ADMIN role
alt Unauthorized
    SVC --> API : throw ForbiddenException
    API --> UI : 403 Forbidden
    UI --> Admin : Show Error
else Authorized
    SVC -> DB : (BR57) Aggregate SUCCESS payments
    DB --> SVC : List<DataPoint>
    SVC --> API : FinancialStatsDTO
    API --> UI : 200 OK
    UI --> Admin : Render Visualization
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (3) | BR56 | **Validate Rules:**<br>❖ If [year] is null then the system shows error message MSG 2.<br>❖ If <<current user role>> != 'ADMIN' then return 403-FORBIDDEN error message. |
| (4) | BR57 | **Loading Rules:**<br>❖ [revenueData] = Payment Repository calculate sum by month WHERE [year] matches AND [status] in ['SUCCESS', 'SYSTEM_SUCCESS'].<br>❖ [transactionCount] = Contract Repository count total WHERE year([createdAt]) = [year]. |
