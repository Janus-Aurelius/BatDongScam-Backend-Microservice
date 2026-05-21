### UC18: Promote Property
**Name**: Promote Property
**Description**: This use case describes how a Property Owner pays to increase the ranking and visibility of their listing.
**Actor**: Owner
**Trigger**: ❖ When the user selects a promotion package and confirms the transaction.
**Pre-condition**: 
❖ The user is logged in as Owner.
❖ The target property is in 'AVAILABLE' status.
**Post-condition**: 
❖ The property ranking points have been increased.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Owner|
start
: (1) Select [packageId] for [propertyId];
: (2) Click "Confirm Promotion";
|System|
: (3) Show confirmation MSG 1;
if (Confirm?) then (yes)
  : (4) Initiate payment (Refer to UC10);
  |Owner|
  : (5) Complete payment on Gateway;
  |System|
  : (6) Handle successful webhook (Refer to UC11);
  : (7) Update property ranking points;
  |Owner|
  : (8) Show success message MSG 3;
  stop
else (no)
  stop
endif
@enduml
```

```plantuml
@startuml
actor Owner
boundary "Promotion UI" as UI
control "Payment Flow" as PF
control "RankingService" as RNK
database "Database" as DB
participant "[Webhook Success]" as WS

Owner -> UI : Purchase ranking points
UI -> Owner : Prompt MSG 1
Owner -> UI : Confirm
UI -> PF : Initialize Payment (UC10)
... (Payment Gateway Redirect) ...
WS -> PF : Webhook (UC11)
PF -> RNK : applyPromotion(propertyId, packageId)
RNK -> DB : (BR61) UPDATE [property] SET ranking_points = ranking_points + [pkg.points]
RNK -> DB : UPDATE [property] SET last_promoted_at = <<current date time>>
RNK --> UI : Success
UI --> Owner : Show MSG 3
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (7) | BR61 | **Updating Rules:**<br>❖ [package] = Ranking Package Repository find by [packageId].<br>❖ [property] = Property Repository find by [propertyId].<br>❖ [property.rankingPoints] = [property.rankingPoints] + [package.points].<br>❖ [property.lastPromotedAt] = <<current date time>>.<br>❖ Property Repository save [property]. |
| (8) | BR3 | **Message Rules:**<br>❖ The system shows success message MSG 3. |
