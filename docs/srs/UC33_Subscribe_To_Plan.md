### UC33: Subscribe to Plan
**Name**: Subscribe to Plan
**Description**: This use case describes how a Sales Agent can purchase a premium subscription plan to increase their property listing capacity.
**Actor**: Sales Agent
**Trigger**: ❖ When the user selects a subscription plan and clicks "Subscribe".
**Pre-condition**: 
❖ The user is logged in as Sales Agent.
**Post-condition**: 
❖ The agent's tier is updated and a subscription record is created.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Agent|
start
: (1) Select [planId];
: (2) Click "Subscribe";
|System|
: (3) Validate downgrade constraints;
if (Allowed?) then (yes)
  : (4) Initiate payment (Refer to UC10);
  |Agent|
  : (5) Complete payment on Gateway;
  |System|
  : (6) Handle successful webhook (Refer to UC11);
  : (7) Update agent tier and listing limits;
  |Agent|
  : (8) Show success message MSG 3;
  stop
else (no)
  |Agent|
  : (9) Show error message MSG 43;
  stop
endif
@enduml
```

```plantuml
@startuml
actor Agent
boundary "Subscription UI" as UI
boundary "Webhook Event" as WH
control "Payment Flow" as PF
control "SubscriptionService" as SVC
database "Database" as DB

Agent -> UI : Select Plan
UI -> PF : Initialize Payment (UC10)
... (Payment Process) ...
WH -> PF : Webhook (UC11)
PF -> SVC : activateSubscription([agentId], [planId])
SVC -> DB : (BR94) SELECT count(*) FROM [property] WHERE agent_id=[agentId] AND status='AVAILABLE'

alt Active Count > New Plan Limit
    SVC --> PF : throw DowngradeViolation
    PF -> Agent : Show MSG 43
else Valid
    SVC -> DB : (BR95) UPDATE [sale_agent] SET tier = [plan.tier], sub_expiry = <<now + 30d>>
    SVC --> Agent : Show MSG 3
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (3) | BR94 | **Validate Rules:**<br>❖ [activeListings] = Property Repository countByAgentAndStatus([me], 'AVAILABLE').<br>❖ If [activeListings] > [plan.propertyLimit] then show error message MSG 43 ("Active listings exceed new plan limit"). |
| (7) | BR95 | **Updating Rules:**<br>❖ [saleAgent.tier] = [plan.tier].<br>❖ [saleAgent.subscriptionExpiry] = <<current date time>> + 30 days.<br>❖ Sale Agent Repository save [saleAgent]. |
| (8) | BR3 | **Message Rules:**<br>❖ The system shows success message MSG 3. |
