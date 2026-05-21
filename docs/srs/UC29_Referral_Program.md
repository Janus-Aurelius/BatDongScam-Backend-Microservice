### UC29: Referral Program
**Name**: Referral Program
**Description**: This use case describes how the system tracks user invitations and awards reward points upon successful transactions by referred users.
**Actor**: User
**Trigger**: ❖ When a Guest registers using a unique referral code.
**Pre-condition**: 
❖ The Referrer has an active account.
**Post-condition**: 
❖ A referral link is established.
❖ Points are awarded to both parties after the referee's first successful payment.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Guest|
start
: (1) Input registration data and [referralCode];
: (2) Click "Sign Up";
|System|
: (3) Validate and establish referral link;
: (4) Complete registration (Refer to UC1);

note right
  ... (Referee performs first payment) ...
end note

|System|
: (5) Detect successful payment event;
: (6) Credit points to both accounts;
|User (Referrer)|
: (7) Show point reward notification MSG 38;
stop
@enduml
```

```plantuml
@startuml
actor "Referrer (A)" as A
actor "Guest (B)" as B
participant "Webhook" as WH
control "ReferralService" as SVC
control "PointService" as PNTS
database "Database" as DB

B -> SVC : Register([data], [code])
SVC -> DB : (BR88) INSERT INTO [referrals] (referrer_id, referee_id)
... (Referee B pays) ...
WH -> SVC : processRewards(refereeId)
SVC -> DB : SELECT * FROM [referrals] WHERE referee_id = [B_id]
alt Link Found & rewarded=false
    SVC -> PNTS : (BR89) addPoints([A_id], 100)
    SVC -> PNTS : (BR89) addPoints([B_id], 50)
    SVC -> DB : UPDATE [referrals] SET rewarded=true
    SVC -> A : Notify MSG 38
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (3) | BR88 | **Validate Rules:**<br>❖ If [referralCode] exists in the system then [link] = new ReferralLink([referrer.id], [me]).<br>❖ Referral Repository save [link] (call save() function). |
| (6) | BR89 | **Point Rules:**<br>❖ If [referee.transactionCount] == 1 then [referrer.points] = [referrer.points] + 100.<br>❖ [referee.points] = [referee.points] + 50.<br>❖ Point Repository save both user balances. |
| (7) | BR38 | **Message Rules:**<br>❖ The system shows notification message MSG 38 ("Referral reward points credited"). |
