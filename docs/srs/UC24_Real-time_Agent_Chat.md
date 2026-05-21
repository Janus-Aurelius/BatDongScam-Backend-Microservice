### UC24: Real-time Agent Chat
**Name**: Real-time Agent Chat
**Description**: This use case describes the real-time exchange of messages between a Customer and a Sales Agent regarding a property listing.
**Actor**: Customer / Agent
**Trigger**: ❖ When the user sends a message in the chat interface.
**Pre-condition**: 
❖ Both parties are logged in.
❖ A WebSocket/Socket.io connection is established.
**Post-condition**: 
❖ The message is delivered to the recipient and saved in the message history.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Sender|
start
: (1) Input message in chat window;
: (2) Click "Send";
|System|
: (3) Persist message to database;
: (4) Determine recipient's connection status;
if (Recipient Online?) then (yes)
  : (5) Dispatch message via Socket.io;
  |Recipient|
  : (6) Render message in chat window;
else (no)
  |System|
  : (7) Trigger Push Notification (Refer to UC14);
endif
|Sender|
: (8) Show delivery confirmation icon;
stop
@enduml
```

```plantuml
@startuml
actor "Sender (Customer/Agent)" as S
boundary "Chat UI" as UI
control "SocketServer" as SOC
control "ChatService" as SVC
database "MongoDB" as DB
actor "Recipient" as R

S -> UI : Send message
UI -> SOC : socket.emit('msg_send', payload)
SOC -> SVC : processMessage(payload)
SVC -> DB : (BR75) INSERT INTO [messages] {from, to, text, ts, status: 'SENT'}
SVC -> SOC : (BR76) getActiveSocket([recipientId])
alt Recipient Online
    SOC -> R : socket.emit('msg_receive', message)
    R -> R : Render UI
else Recipient Offline
    SVC -> SVC : triggerPush(UC14)
end
SVC --> SOC : emit('msg_delivered')
SOC --> S : Update Message Status Icon
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (3) | BR75 | **Saving Rules:**<br>❖ [message] = new ChatMessage().<br>❖ [message.senderId] = <<me>>, [message.receiverId] = [recipientId], [message.content] = [text].<br>❖ Message Repository save [message] (call save() function). |
| (4), (5) | BR76 | **Dispatch Rules:**<br>❖ The system checks the internal Socket Map for [recipientId].<br>❖ If [socketId] exists then call socket.to([socketId]).emit('new_message', [message]) else call Notification Service createNotification([recipientId], 'CHAT_MESSAGE', [text]). |
