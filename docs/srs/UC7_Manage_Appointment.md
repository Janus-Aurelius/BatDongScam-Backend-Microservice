### UC7: Manage Appointment
**Name**: Manage Appointment
**Description**: This use case describes how an authorized user can confirm an appointment (by assigning a Sales Agent) or cancel a viewing request.
**Actor**: Admin / Agent / Owner
**Trigger**: ❖ When the user clicks the “Confirm/Assign Agent” or “Cancel Appointment” button.
**Pre-condition**: 
❖ The user is logged in and has appropriate permissions for the appointment.
❖ [appointment.status] is 'PENDING' or 'CONFIRMED'.
**Post-condition**: 
❖ The appointment status is updated.
❖ Involved parties are notified of the change.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Actor|
start
if (Select "Confirm/Assign Agent"?) then (yes)
  : (1) Select [agentId];
  : (2) Click "Confirm";
  |System|
  : (3) Validate and update status;
  if (Success?) then (yes)
    : (4) Set [appointment.status] = 'CONFIRMED';
    |Actor|
    : (5) Show success message MSG 3;
    stop
  else (no)
    |Actor|
    : (6) Show error message MSG 12;
    stop
  endif
else (no)
  |Actor|
  : (7) Select "Cancel Appointment" and input [reason];
  |System|
  : (8) Show confirmation MSG 1;
  if (Confirm?) then (yes)
    : (9) Validate and set [appointment.status] = 'CANCELLED';
    |Actor|
    : (10) Show success message MSG 3;
    stop
  else (no)
    stop
  endif
endif
@enduml
```

```plantuml
@startuml
actor Actor
boundary "Appointment Management UI" as UI
control "AppointmentController" as API
control "AppointmentService" as SVC
database "Database" as DB

alt Action == CONFIRM
    Actor -> UI : Select agent and click Confirm
    UI -> API : POST /appointment/assign-agent
    API -> SVC : assignAgent(agentId, appointmentId)
    SVC -> DB : findById([appointmentId])
    SVC -> SVC : (BR27) Validate state
    SVC -> DB : UPDATE [appointment] SET agent_id = [agentId], status = 'CONFIRMED'
    SVC --> API : Success
    API --> UI : 200 OK
    UI --> Actor : Show MSG 3
else Action == CANCEL
    Actor -> UI : Click Cancel
    UI -> Actor : Prompt MSG 1
    Actor -> UI : Confirm
    UI -> API : PATCH /appointment/{id}/cancel
    API -> SVC : cancelAppointment(id, reason)
    SVC -> SVC : (BR28) Validate state
    SVC -> DB : UPDATE [appointment] SET status = 'CANCELLED'
    SVC --> API : Success
    API --> UI : 200 OK
    UI --> Actor : Show MSG 3
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (3) | BR27 | **Saving Rules:**<br>❖ If [appointment.status] != 'PENDING' then the system shows error message MSG 12.<br>❖ [appointment.agent] = Agent Repository find by [agentId].<br>❖ [appointment.status] = 'CONFIRMED'.<br>❖ [appointment.confirmedDate] = <<current date time>>.<br>❖ Appointment Repository save [appointment] (call save() function). |
| (9) | BR28 | **Validate Rules:**<br>When the user clicks on “Cancel Appointment”, the system will prompt a confirmation message (Refer to MSG 1). If user chooses Cancel, the system does nothing; else:<br>❖ If [appointment.status] is 'CANCELLED' or 'COMPLETED' then the system shows error message MSG 12.<br>❖ [appointment.status] = 'CANCELLED'.<br>❖ [appointment.cancelledAt] = <<current date time>>.<br>❖ Appointment Repository save [appointment] (call save() function).<br>❖ If Actor is SALESAGENT then Ranking Service apply penalty 'APPOINTMENT_CANCELLED' for [agent.id]. |
| (5), (10) | BR3 | **Message Rules:**<br>❖ The system shows success message MSG 3. |
