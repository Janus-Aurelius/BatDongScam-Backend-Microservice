### UC17: Update Profile
**Name**: Update Profile
**Description**: This use case describes the process by which a user updates their personal information and avatar.
**Actor**: User
**Trigger**: ❖ When the user clicks the "Save Changes" button.
**Pre-condition**: 
❖ The user is logged in to the system.
**Post-condition**: 
❖ The user's profile information has been updated in the database.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|User|
start
: (1) Modify [firstName], [lastName], [phoneNumber], [bio], or [avatar];
: (2) Click "Save Changes";
|System|
: (3) Show confirmation MSG 1;
if (Confirm?) then (yes)
  : (4) Validate input data;
  if (Valid?) then (yes)
    : (5) Process uploads and update user;
    |User|
    : (6) Show success message MSG 3;
    stop
  else (no)
    |User|
    : (7) Show error message (MSG 2, 26, or 27);
    stop
  endif
else (no)
  stop
endif
@enduml
```

```plantuml
@startuml
actor User
boundary "Profile Settings UI" as UI
control "AccountController" as API
control "UserService" as SVC
entity "Cloudinary" as CDN
database "Database" as DB

User -> UI : Submit modifications
UI -> User : Prompt MSG 1
User -> UI : Confirm
UI -> API : PUT /account/me
API -> SVC : updateMe(request)
SVC -> SVC : (BR60) Validate non-empty fields
alt Validation Fails
    SVC --> API : throw Exception
    API --> UI : Error MSG 2
    UI --> User : Show MSG 2
else Valid
    alt Has Avatar
        SVC -> CDN : uploadFile([avatar])
        CDN --> SVC : [avatarUrl]
    end
    SVC -> DB : UPDATE [user] SET ... WHERE id = [me]
    SVC --> API : MeResponse
    API --> UI : 200 OK
    UI --> User : Show MSG 3
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (4) | BR60 | **Validate Rules:**<br>When the user clicks on “Save Changes”, the system will prompt a confirmation message (Refer to MSG 1). If user chooses Cancel, the system does nothing; else:<br>❖ The system checks the items [firstName], [lastName], [phoneNumber].<br>❖ If any mandatory entries are empty, the system shows an error message MSG 2.<br>❖ If [phoneNumber] exists for another user then show error message MSG 26. |
| (5) | BR59 | **Updating Rules:**<br>❖ If [avatar] is provided then [user.avatarPath] = Cloudinary Service save [avatar].<br>❖ [user] = User Repository save updated fields.<br>❖ User Repository save [user] (call save() function). |
| (6) | BR3 | **Message Rules:**<br>❖ The system shows success message MSG 3. |
