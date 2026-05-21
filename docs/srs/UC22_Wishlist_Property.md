### UC22: Wishlist Property
**Name**: Wishlist Property
**Description**: This use case describes how a customer can add or remove a property from their personal favorites list for later review.
**Actor**: Customer
**Trigger**: ❖ When the user clicks on the “Favorite/Heart” icon.
**Pre-condition**: 
❖ The user is logged in to the system.
**Post-condition**: 
❖ The property association with the user's wishlist is updated.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Customer|
start
: (1) Click Favorite icon on property card or detail page;
|System|
: (2) Check if property-user link exists;
if (Already in Wishlist?) then (yes)
  : (3) Remove record from [favorite] table;
  |Customer|
  : (4) Show success message MSG 35 and update icon;
  stop
else (no)
  : (5) INSERT into [favorite] table;
  |Customer|
  : (6) Show success message MSG 36 and update icon;
  stop
endif
@enduml
```

```plantuml
@startuml
actor Customer
boundary "UI" as UI
control "FavoriteController" as API
control "CustomerFavoriteService" as SVC
database "Database" as DB

Customer -> UI : Toggle Favorite
UI -> API : POST /favorites/toggle?propertyId={id}
API -> SVC : toggleFavorite(propertyId)
SVC -> DB : (BR71) SELECT count(*) FROM [favorite] WHERE user_id = [me] AND property_id = [propertyId]
alt Record Exists
    SVC -> DB : DELETE FROM [favorite] WHERE user_id = [me] AND property_id = [propertyId]
    SVC --> API : Boolean (false)
    API --> UI : 200 OK
    UI --> Customer : Show MSG 35 & Gray Heart
else Record Missing
    SVC -> DB : (BR72) INSERT INTO [favorite] (user_id, property_id) VALUES ([me], [propertyId])
    SVC --> API : Boolean (true)
    API --> UI : 200 OK
    UI --> Customer : Show MSG 36 & Red Heart
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (2) | BR71 | **Checking Rules:**<br>❖ [count] = Favorite Repository count WHERE [user_id] = <<me>> AND [property_id] = [propertyId]. |
| (5) | BR72 | **Saving Rules:**<br>❖ If [propertyRepository.findById([propertyId])] is null then show error message MSG 18.<br>❖ Favorite Repository save new favorite record. |
| (4) | BR35 | **Message Rules:**<br>❖ The system shows success message MSG 35 ("Removed from favorites"). |
| (6) | BR36 | **Message Rules:**<br>❖ The system shows success message MSG 36 ("Added to favorites"). |
