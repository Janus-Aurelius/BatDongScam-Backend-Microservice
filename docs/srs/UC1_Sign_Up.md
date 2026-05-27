### UC1: Sign Up
**Name**: Sign Up
**Description**: This use case describes the process by which a user creates a new account in the system.
**Actor**: Guest
**Trigger**: ❖ When the user clicks on the “Sign Up” button.
**Pre-condition**: 
❖ The user is on the sign up page (refer to “Sign Up Form” in “List description” file).
**Post-condition**: 
❖ A new account has been created in the ‘PENDING_APPROVAL’ or 'ACTIVE' state.
❖ The user will be redirected to the home page.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Guest|
start
: (1) Input [firstName], [lastName], [email], [password], [phoneNumber], [frontIdPicture], [backIdPicture];
: (2) Click "Sign Up";
|System|
: (3) Perform input validation;
if (Data Valid?) then (yes)
  : (4) Upload ID pictures and hash [password];
  : (5) Create [user] and [customer/property_owner] records;
  |Guest|
  : (6) Show success message MSG 28;
  : (7) Redirect to home page;
  stop
else (no)
  |Guest|
  : (8) Show error message (MSG 2, 24, 25, 26, 27, 30, or 31);
  stop
endif
@enduml
```

```plantuml
@startuml
actor Guest
boundary "Sign Up Form" as UI
control "PublicController" as API
control "UserService" as SVC
entity "Cloudinary" as CDN
database "Database" as DB

Guest -> UI : Input data & photos
UI -> API : POST /auth/register
API -> SVC : register(request, role)
SVC -> SVC : (BR5) Validate inputs
alt Validation Fails
    SVC --> API : throw Exception
    API --> UI : Error Message
    UI --> Guest : Show Error (MSG X)
else Validation Success
    SVC -> CDN : Upload ID pictures
    SVC -> SVC : Hash password
    SVC -> DB : save(user) & save(role_record)
    SVC --> API : Success
    API --> UI : 200 OK
    UI --> Guest : Show MSG 28 & Redirect
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (3) | BR5 | **Validate Rules:**<br>❖ The system checks the items [firstName], [lastName], [email], [password], [phoneNumber], [roleEnum].<br>❖ If any entries are empty, the system shows an error message MSG 2.<br>❖ If [password.length] < 8 then the system shows an error message MSG 24.<br>❖ If pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$").notMatch([email]) then returns error message MSG 31.<br>❖ If [userRepository.existsByEmail([email])] is true then the system shows an error message MSG 27.<br>❖ If [roleEnum] not in ['CUSTOMER', 'PROPERTY_OWNER'] return error "Hell nah". |
| (5) | BR6 | **Creating Rules:**<br>❖ [user.password] = hash([password])<br>❖ If [roleEnum] == 'CUSTOMER' then [user.status] = 'ACTIVE' and create record in [customer] table.<br>❖ If [roleEnum] == 'PROPERTY_OWNER' then [user.status] = 'PENDING_APPROVAL' and create record in [property_owner] table. |
| (6) | BR7 | **Message Rules:**<br>❖ The system shows success message MSG 28. |
| (7) | BR8 | **Redirect Rules:**<br>❖ The system redirects to the home page. |
