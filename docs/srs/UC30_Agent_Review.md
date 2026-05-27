### UC30: Agent Review
**Name**: Agent Review
**Description**: This use case describes the process by which a customer rates and provides feedback on a Sales Agent following a completed interaction.
**Actor**: Customer
**Trigger**: ❖ When the user clicks on the “Rate Agent” button.
**Pre-condition**: 
❖ The user is logged in as Customer.
❖ The user has a 'COMPLETED' appointment or 'ACTIVE' contract with the agent.
**Post-condition**: 
❖ The review is saved and the agent's aggregate rating is updated.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Customer|
start
: (1) Input [rating], [comment] for [agentId];
: (2) Click "Submit Review";
|System|
: (3) Show confirmation MSG 1;
if (Confirm?) then (yes)
  : (4) Validate verified interaction;
  if (Interaction Exists?) then (yes)
    : (5) Create [review] record;
    : (6) Recalculate agent average rating;
    |Customer|
    : (7) Show success message MSG 3;
    stop
  else (no)
    |Customer|
    : (8) Show error message MSG 39;
    stop
  endif
else (no)
  stop
endif
@enduml
```

```plantuml
@startuml
actor Customer
boundary "Review Form" as UI
control "ReviewController" as API
control "RankingService" as RNK
database "Database" as DB

Customer -> UI : Submit rating
UI -> Customer : Prompt MSG 1
Customer -> UI : Confirm
UI -> API : POST /reviews/agent/{id}
API -> RNK : submitReview(agentId, rating, text)
RNK -> DB : (BR90) SELECT count(*) FROM appointments/contracts WHERE cust=[me] AND agent=[id]
alt No Interaction
    SVC --> API : throw Exception
    API --> UI : Error MSG 39
    UI --> Customer : Show MSG 39
else Verified
    RNK -> DB : INSERT INTO [agent_reviews] ...
    RNK -> DB : (BR91) UPDATE [sale_agent] SET avg_rating = (SELECT AVG...)
    RNK -> DB : UPDATE performance_points += ([rating] * 10)
    RNK --> API : Success
    API --> UI : 200 OK
    UI --> Customer : Show MSG 3
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (4) | BR90 | **Validate Rules:**<br>When the user clicks on “Submit Review”, the system will prompt a confirmation message (Refer to MSG 1). If user chooses Cancel, the system does nothing; else:<br>❖ If Appointment Repository countByCustomerAndAgentAndStatus([me], [agentId], 'COMPLETED') == 0 AND Contract Repository countByCustomerAndAgent([me], [agentId]) == 0 then show error message MSG 39 ("No verified interaction found"). |
| (6) | BR91 | **Calculation Rules:**<br>❖ [agent.avgRating] = Review Repository calculate average([agentId]).<br>❖ [agent.performancePoints] = [agent.performancePoints] + ([rating] * 10).<br>❖ Sale Agent Repository save [agent]. |
| (7) | BR3 | **Message Rules:**<br>❖ The system shows success message MSG 3. |
