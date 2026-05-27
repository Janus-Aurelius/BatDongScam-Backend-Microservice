### UC25: AI Property Valuation
**Name**: AI Property Valuation
**Description**: This use case describes how the system uses artificial intelligence to estimate the market value of a property based on its features and historical data.
**Actor**: Owner
**Trigger**: ❖ When the user clicks on the “Estimate Value” button.
**Pre-condition**: 
❖ The user is logged in as Owner.
❖ The property has basic data provided (area, location, type).
**Post-condition**: 
❖ An AI-estimated value is displayed and saved to the property record.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Owner|
start
: (1) Select property and request valuation;
|System|
: (2) Load Valuation screen;
: (3) Show confirmation MSG 1;
if (Confirm?) then (yes)
  : (4) Validate property features;
  if (Data Valid?) then (yes)
    : (5) Process ML inference;
    : (6) Save estimated value to database;
    |Owner|
    : (7) Display price range and success message MSG 3;
    stop
  else (no)
    |Owner|
    : (8) Show error message MSG 2;
    stop
  endif
else (no)
  stop
endif
@enduml
```

```plantuml
@startuml
actor Owner
boundary "Valuation UI" as UI
control "ValuationController" as API
control "ValuationService" as SVC
control "AI_Engine" as AI
database "Database" as DB

Owner -> UI : Click Estimate
UI -> Owner : Prompt MSG 1
Owner -> UI : Confirm
UI -> API : POST /valuation/predict
API -> SVC : estimateValue(propertyId)
SVC -> DB : (BR77) SELECT features FROM [property] WHERE id = [propertyId]
alt Invalid Features
    SVC --> API : throw Exception
    API --> UI : Error MSG 2
    UI --> Owner : Show MSG 2
else Valid
    SVC -> AI : (BR78) Inference([features])
    AI --> SVC : [minPrice, maxPrice, score]
    SVC -> DB : (BR79) UPDATE [property] SET estimated_value = [avgPrice], valuation_date = <<now>>
    SVC --> API : PredictionResult
    API --> UI : 200 OK
    UI --> Owner : Render Chart & Show MSG 3
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (2) | BR76 | **Loading Screen Rules:**<br>❖ The system loads the “AI Valuation” interface. |
| (4) | BR77 | **Validate Rules:**<br>When the user clicks on “Estimate Value”, the system will prompt a confirmation message (Refer to MSG 1). If user chooses Cancel, the system does nothing; else:<br>❖ The system checks the items [area], [wardId], [propertyTypeId].<br>❖ If [area] <= 0 then the system shows error message MSG 13.<br>❖ If mandatory items are null then the system shows error message MSG 2. |
| (5) | BR78 | **AI Rules:**<br>❖ [prediction] = AI Gateway POST /predict with [featureVector] constructed from property attributes. |
| (6) | BR79 | **Saving Rules:**<br>❖ [property.estimatedValue] = ([prediction.min] + [prediction.max]) / 2.<br>❖ [property.valuationDate] = <<current date time>>.<br>❖ Property Repository save [property] (call save() function). |
| (7) | BR3 | **Message Rules:**<br>❖ The system shows success message MSG 3. |
