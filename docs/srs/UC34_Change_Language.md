### UC34: Change Language
**Name**: Change Language
**Description**: This use case describes the process by which a user switches the application interface and content language.
**Actor**: User
**Trigger**: ❖ When the user selects a language from the interface.
**Pre-condition**: 
❖ None.
**Post-condition**: 
❖ The UI is localized to the selected language.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|User|
start
: (1) Select "Vietnamese" or "English";
|System|
: (2) Update locale preference;
: (3) Reload localized resources;
|User|
: (4) Render localized UI and show message MSG 32;
stop
@enduml
```

```plantuml
@startuml
actor User
boundary "UI Interface" as UI
control "I18nInterceptor" as API
control "MessageSource" as SVC

User -> UI : Select Language [lang]
UI -> API : GET /resources (Header: Accept-Language=[lang])
API -> SVC : (BR96) getMessage([key], [locale])
SVC --> API : Localized String
API --> UI : 200 OK (Localized Payload)
UI -> UI : (BR97) Format dates/currency for [lang]
UI --> User : Render Text & MSG 32
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (3) | BR96 | **Loading Rules:**<br>❖ [message] = MessageSource getMessage([key], [locale]). If not found, use [defaultLocale]. |
| (4) | BR97 | **Formatting Rules:**<br>❖ If [locale] == 'vi' then [date] = format([ts], "dd/MM/yyyy") else [date] = format([ts], "MM/dd/yyyy").<br>❖ If [locale] == 'vi' then [currency] = format([amount], "VND") else [currency] = format([amount], "USD"). |
