### UC15: Send Email Alert
**Name**: Send Email Alert
**Description**: This use case describes the process by which the system sends automated email notifications and documents to users.
**Actor**: System
**Trigger**: ❖ When a specific system event requiring email notification occurs (e.g., payment success).
**Pre-condition**: 
❖ The target user has a valid [email] address.
**Post-condition**: 
❖ The email is sent using the appropriate template and any necessary attachments.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|System|
start
: (1) Trigger email event for [email];
: (2) Retrieve [templateId] and [contextData];
: (3) Render email body and attach documents;
: (4) Dispatch via SMTP server;
if (Send Success?) then (yes)
  : (5) Log email success event;
else (no)
  : (6) Log email failure and schedule retry;
endif
|User Inbox|
: (7) Receive email alert;
stop
@enduml
```

```plantuml
@startuml
actor "System Process" as Sys
control "EmailService" as SVC
control "TemplateEngine" as TPL
control "SMTP Server" as SMTP
actor "User Email" as Inbox

Sys -> SVC : sendEmail(email, type, data)
SVC -> TPL : (BR53) render([templateId], [data])
TPL --> SVC : HTML_Content
alt Has Attachment
    SVC -> SVC : (BR54) Attach PDF/Files
end
SVC -> SMTP : (BR55) SEND (To: [email], Body: HTML_Content)
SMTP --> SVC : 250 OK / Error
alt Success
    SVC -> SVC : Log 'SUCCESS'
    SMTP -> Inbox : Deliver Email
else Fail
    SVC -> SVC : Log 'FAIL'
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (3) | BR53 | **Template Rules:**<br>❖ [emailBody] = Template Engine render by [templateId] using [contextData] map.<br>❖ [templateId] is retrieved from System Configuration where [keyword] = [eventType]. |
| (3) | BR54 | **Attachment Rules:**<br>❖ If [attachments] is not null then for each [file] in [attachments] do add to email multipart body. |
| (4) | BR55 | **Dispatch Rules:**<br>❖ If [email] is null or blank then return 400-BAD_REQUEST error message MSG 2.<br>❖ Call SMTP library send function with system credentials. |
