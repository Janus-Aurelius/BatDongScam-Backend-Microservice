### UC6: Book Appointment
**Name**: Book Appointment
**Description**: This use case allows a user to request a viewing appointment for a specific property.
**Actor**: Customer
**Trigger**: ❖ When the user clicks on the “Book Viewing” button.
**Pre-condition**: 
❖ The user is logged in to the system.
❖ The user is on the property details page.
**Post-condition**: 
❖ A viewing appointment has been created in 'PENDING' or 'CONFIRMED' status.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Customer|
start
: (1) Input [requestedDate] and [requirements];
: (2) Click "Book Viewing";
|System|
: (3) Show confirmation MSG 1;
if (Confirm?) then (yes)
  : (4) Validate appointment constraints;
  if (Valid?) then (yes)
    : (5) Create [appointment] record;
    |Customer|
    : (6) Show success message MSG 3;
    stop
  else (no)
    |Customer|
    : (7) Show error message (MSG 2, 12, 13, or 33);
    stop
  endif
else (no)
  stop
endif
@enduml
```

```plantuml
@startuml
actor Customer
boundary "Appointment Form" as UI
control "AppointmentController" as API
control "AppointmentService" as SVC
database "Database" as DB

Customer -> UI : Submit form
UI -> Customer : Prompt MSG 1
Customer -> UI : Confirm
UI -> API : POST /appointment
API -> SVC : bookAppointment(request)
SVC -> SVC : (BR33) Validate date and property
alt Validation Fails
    SVC --> API : throw Exception
    API --> UI : Error MSG X
    UI --> Customer : Show Error
else Validation Success
    SVC -> DB : INSERT [appointment] SET status = 'PENDING'
    SVC --> API : Success
    API --> UI : 201 Created
    UI --> Customer : Show MSG 3
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (4) | BR33 | **Validate Rules:**<br>When the user clicks on “Book Viewing”, the system will prompt a confirmation message (Refer to MSG 1). If user chooses Cancel, the system does nothing; else:<br>❖ The system checks the items [requestedDate], [propertyId].<br>❖ If any entries are empty, the system shows an error message MSG 2.<br>❖ If [requestedDate] <= <<now>> then the system shows an error message MSG 13.<br>❖ If [property.status] is not 'AVAILABLE' then the system shows an error message MSG 12.<br>❖ If [appointmentRepository.existsByCustomerAndProperty([me], [propertyId])] with status 'PENDING' or 'CONFIRMED' then the system shows error message MSG 33 ("Duplicate appointment"). |
| (5) | BR34 | **Creating Rules:**<br>❖ [appointment] = Appointment Repository save new appointment.<br>❖ [appointment.status] = 'PENDING'.<br>❖ If [agentId] is provided, [appointment.agentId] = [agentId] and [appointment.status] = 'CONFIRMED'. |
| (6) | BR20 | **Message Rules:**<br>❖ The system shows success message MSG 3. |
