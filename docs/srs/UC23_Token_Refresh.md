### UC23: Token Refresh
**Name**: Token Refresh
**Description**: This use case describes the process by which the system issues a new set of authentication tokens using a valid refresh token.
**Actor**: User
**Trigger**: ❖ When the client application detects an expired access token or upon initial launch.
**Pre-condition**: 
❖ The user has a valid, non-expired refresh token stored locally.
**Post-condition**: 
❖ The system issues a new JWT access token and a rotated refresh token.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|User|
start
: (1) System sends Refresh Token in Authorization header;
|System|
: (2) Validate token signature and expiration;
if (Token Valid?) then (yes)
  : (3) Identify [userId] and generate new token pair;
  |User|
  : (4) Store new tokens and continue session;
  stop
else (no)
  |User|
  : (5) Show error message MSG 22 and redirect to Login;
  stop
endif
@enduml
```

```plantuml
@startuml
actor User
boundary "Mobile/Web Client" as UI
control "PublicController" as API
control "AuthService" as SVC
control "JwtTokenProvider" as JWT
database "Database" as DB

UI -> API : GET /auth/refresh
API -> SVC : refreshFromBearerString([refreshToken])
SVC -> JWT : (BR73) validateToken([refreshToken])
alt Token Invalid/Expired
    JWT --> SVC : throw ExpiredJwtException
    SVC --> API : throw RefreshTokenExpiredException
    API --> UI : 401 Unauthorized
    UI --> User : Show MSG 22 & Redirect to Sign In
else Token Valid
    JWT --> SVC : [claims]
    SVC -> DB : (BR74) findById([claims.userId])
    DB --> SVC : [user]
    SVC -> JWT : generateJwt([user]) & generateRefresh([user])
    JWT --> SVC : [newTokenPair]
    SVC --> API : TokenResponse
    API --> UI : 200 OK
    UI --> User : Session Extended
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (2) | BR73 | **Validate Rules:**<br>❖ If [jwtTokenProvider.validateToken([refreshToken])] is false OR [refreshToken.expiry] < <<now>> then return 401-UNAUTHORIZED with error message MSG 22. |
| (3) | BR74 | **Creating Rules:**<br>❖ [accessToken] = jwtTokenProvider.generateToken([user.id], [user.role], <<expiry: 1h>>).<br>❖ [refreshToken] = jwtTokenProvider.generateToken([user.id], [user.role], <<expiry: 7d>>).<br>❖ Return [TokenResponse] containing both tokens. |
