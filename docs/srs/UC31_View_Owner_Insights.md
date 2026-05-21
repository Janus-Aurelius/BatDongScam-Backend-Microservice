### UC31: View Owner Insights
**Name**: View Owner Insights
**Description**: This use case describes how a Property Owner can view engagement metrics and conversion statistics for their listings.
**Actor**: Owner
**Trigger**: ❖ When the user clicks on the “Insights” tab in the dashboard.
**Pre-condition**: 
❖ The user is logged in as Owner.
**Post-condition**: 
❖ Engagement charts and conversion metrics are displayed.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Owner|
start
: (1) Select "Owner Insights" from dashboard;
|System|
: (2) Retrieve engagement and conversion data;
if (Data Exists?) then (yes)
  : (3) Aggregate views, favorites, and appointments;
  : (4) Calculate conversion percentages;
  |Owner|
  : (5) Render charts and success message MSG 32;
  stop
else (no)
  |Owner|
  : (6) Show message MSG 40;
  stop
endif
@enduml
```

```plantuml
@startuml
actor Owner
boundary "Insights UI" as UI
control "AccountController" as API
control "AnalyticsService" as SVC
database "AnalyticsDB" as DB1
database "PrimaryDB" as DB2

Owner -> UI : Click Insights
UI -> API : GET /account/me/insights
API -> SVC : getOwnerInsights([ownerId])
SVC -> DB1 : (BR90) SELECT count(unique_views) FROM events WHERE owner_id=[ownerId]
DB1 --> SVC : ViewCount
SVC -> DB2 : SELECT count(*) FROM [favorite] WHERE property_owner=[ownerId]
DB2 --> SVC : FavCount
SVC -> DB2 : SELECT count(*) FROM [appointment] WHERE owner_id=[ownerId]
DB2 --> SVC : ApptCount
SVC -> SVC : (BR91) Calculate % (Appt/Views)
SVC --> API : OwnerInsightsDTO
API --> UI : 200 OK
UI --> Owner : Render Visualization & MSG 32
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (3) | BR90 | **Aggregation Rules:**<br>❖ [viewCount] = Analytics Repository findUniqueViewsByOwner([ownerId]).<br>❖ [favoriteCount] = Favorite Repository findCountByOwner([ownerId]).<br>❖ [appointmentCount] = Appointment Repository findCountByOwner([ownerId]). |
| (4) | BR91 | **Calculation Rules:**<br>❖ [conversionRate] = ([appointmentCount] / [viewCount]) * 100. If [viewCount] == 0 then [conversionRate] = 0. |
| (5) | BR32 | **Message Rules:**<br>❖ The system shows success message MSG 32. |
| (6) | BR40 | **Message Rules:**<br>❖ The system shows informational message MSG 40 ("No data available for the selected period"). |
