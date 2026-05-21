### UC14: Send Push Notification
**Name**: Send Push Notification
**Description**: This use case describes the process by which the system sends real-time alerts to users based on system events.
**Actor**: System
**Trigger**: ❖ When a specific system event (e.g., appointment booked, payment received) occurs.
**Pre-condition**: 
❖ The target user has at least one valid FCM token registered in the system.
**Post-condition**: 
❖ The notification record is saved and the message is dispatched to the user's device via FCM.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|System|
start
: (1) Detect system event for [userId];
: (2) Create [notification] record in 'PENDING' status;
: (3) Fetch active FCM tokens for [userId];
if (Tokens Found?) then (yes)
  : (4) Prepare payload and dispatch to FCM;
  if (FCM Success?) then (yes)
    : (5) Set [notification.status] = 'SENT';
  else (no)
    : (6) Set [notification.status] = 'FAILED';
  endif
else (no)
  : (7) Set [notification.status] = 'FAILED';
endif
|User Device|
: (8) Render notification on device;
stop
@enduml
```

```plantuml
@startuml
actor "System Process" as Sys
control "NotificationService" as SVC
database "Database" as DB
control "Firebase FCM" as FCM
actor "User Device" as Device

Sys -> SVC : createNotification(userId, type, content)
SVC -> DB : (BR50) INSERT INTO [notification] SET status = 'PENDING'
SVC -> DB : SELECT token FROM [user_fcm_tokens] WHERE user_id = [userId]
DB --> SVC : List<Tokens>
SVC -> FCM : (BR51) POST /send (Payload)
FCM --> SVC : MessageID / Error
alt Success
    SVC -> DB : (BR52) UPDATE [notification] SET status = 'SENT'
    FCM -> Device : Deliver Push
    Device -> Device : Render Notification
else Fail
    SVC -> DB : UPDATE [notification] SET status = 'FAILED'
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (2) | BR50 | **Saving Rules:**<br>❖ [notification] = Notification Repository save new notification.<br>❖ [notification.status] = 'PENDING'.<br>❖ [notification.type] = [eventType].<br>❖ [notification.createdAt] = <<current date time>>. |
| (4) | BR51 | **Gateway Rules:**<br>❖ Payload = { "title": [notification.title], "body": [notification.content], "data": { "id": [notification.relatedEntityId] } }.<br>❖ Call FCM API for each token found. |
| (5) | BR52 | **Updating Rules:**<br>❖ If FCM returns success then [notification.status] = 'SENT' else [notification.status] = 'FAILED'.<br>❖ Notification Repository save [notification]. |
