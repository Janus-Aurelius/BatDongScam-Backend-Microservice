### UC37: Manage User Account
**Name**: Manage User Account
**Description**: This use case allows an Administrator to perform administrative actions on user accounts, including updating roles, statuses, or deleting the account.
**Actor**: Admin
**Trigger**: ❖ When the Admin clicks the “Update” or “Delete” button in the user management UI.
**Pre-condition**: 
❖ The user is logged in as Admin.
❖ The target user account exists in the system.
**Post-condition**: 
❖ The user account is updated or soft-deleted.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Admin|
start
if (Select "Update"?) then (yes)
  : (1) Modify [role], [status], or [verificationLevel];
  : (2) Click "Save";
  |System|
  : (3) Show confirmation MSG 1;
  if (Confirm?) then (yes)
    : (4) Validate role constraints and update [user];
    |Admin|
    : (5) Show success message MSG 3;
    stop
  else (no)
    stop
  endif
else (no)
  |Admin|
  : (6) Click "Delete Account";
  |System|
  : (7) Show confirmation MSG 1;
  if (Confirm?) then (yes)
    : (8) Validate self-deletion constraint;
    if (Allowed?) then (yes)
      : (9) Set [user.status] = 'DELETED';
      |Admin|
      : (10) Show success message MSG 3;
      stop
    else (no)
      |Admin|
      : (11) Show error message MSG 12;
      stop
    endif
  else (no)
    stop
  endif
endif
@enduml
```

```plantuml
@startuml
actor Admin
boundary "User Management UI" as UI
control "AccountController" as API
control "UserService" as SVC
database "Database" as DB

alt Action == UPDATE
    Admin -> UI : Update roles/status
    UI -> Admin : Prompt MSG 1
    Admin -> UI : Confirm
    UI -> API : PUT /accounts/{id}
    API -> SVC : updateUserById(id, request)
    SVC -> DB : (BR104) UPDATE [user] SET role=[role], status=[status]
    SVC --> API : Success
    API --> UI : 200 OK
    UI --> Admin : Show MSG 3
else Action == DELETE
    Admin -> UI : Click Delete
    UI -> Admin : Prompt MSG 1
    Admin -> UI : Confirm
    UI -> API : DELETE /accounts/{id}
    API -> SVC : deleteAccountById(id)
    SVC -> SVC : (BR105) If [targetId] == [me] throw Exception
    alt Is Self
        SVC --> API : throw Exception
        API --> UI : Error MSG 12
        UI --> Admin : Show MSG 12
    else Is Other
        SVC -> DB : (BR106) UPDATE [user] SET status = 'DELETED'
        SVC --> API : Success
        API --> UI : 200 OK
        UI --> Admin : Show MSG 3
    end
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (4) | BR104 | **Updating Rules:**<br>❖ [user] = User Repository find by [targetUserId].<br>❖ [user.role] = [newRole].<br>❖ [user.status] = [newStatus].<br>❖ User Repository save [user] (call save() function). |
| (8) | BR105 | **Validate Rules:**<br>❖ If [targetUserId] == <<current user id>> then the system shows error message MSG 12 ("Cannot delete your own account"). |
| (9) | BR106 | **Delete Rules:**<br>❖ [user.status] = 'DELETED'.<br>❖ User Repository save [user]. |
| (5), (10) | BR3 | **Message Rules:**<br>❖ The system shows success message MSG 3. |
