### UC2: Sign In
**Name**: Sign In
**Description**: This use case describes the process by which a user logs into the system.
**Actor**: User
**Trigger**: ❖ When the user clicks on the “Sign In” button.
**Pre-condition**: 
❖ The user is not logged in to the system.
❖ The user is in the sign in page (refer to “Sign In Form” in “List description” file).
**Post-condition**: 
❖ The user is logged in to the system.
❖ The user is redirected to the home page.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|User|
start
: (1) Input [email], [password];
: (2) Click "Sign In";
|System|
: (3) Verify credentials;
if (Credentials Valid?) then (yes)
  : (4) Generate session and tokens;
  |User|
  : (5) Show success message MSG 23;
  : (6) Redirect to home page;
  stop
else (no)
  |User|
  : (7) Show error message MSG 22;
  stop
endif
@enduml
```

```plantuml
@startuml
actor User
boundary "Sign In Form" as UI
control "PublicController" as API
control "AuthService" as SVC
database "Database" as DB

User -> UI : Submit credentials
UI -> API : POST /auth/login
API -> SVC : login(email, password)
SVC -> DB : findByEmail([email])
DB --> SVC : [user]
SVC -> SVC : (BR1) Validate credentials
alt Auth Fails
    SVC --> API : throw Exception
    API --> UI : Error MSG 22
    UI --> User : Show MSG 22
else Auth Success
    SVC -> DB : UPDATE [user] SET status = 'ACTIVE'
    SVC --> API : TokenResponse
    API --> UI : 200 OK
    UI --> User : Show MSG 23 & Redirect
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (3) | BR1 | **Validate Rules:**<br>❖ The system checks the items [email], [password].<br>❖ If any of them is null or blank the system will show an error message MSG 2.<br>❖ If [email] does not exist the system will show an error message MSG 22 else [user] = User Repository find by [email].<br>❖ If hash([password]) != [user.password] then the system will show an error message MSG 22 else generate jwt from [user.id]. |
| (5) | BR3 | **Message Rules:**<br>❖ The system shows the success message MSG 23. |
| (6) | BR4 | **Redirect Rules:**<br>❖ The system redirects to the home page. |
| (7) | BR2 | **Message Rules:**<br>❖ The system shows the error message MSG 22. |
