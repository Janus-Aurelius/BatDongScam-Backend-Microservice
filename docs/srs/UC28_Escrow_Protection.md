### UC28: Escrow Payment Protection
**Name**: Escrow Payment Protection
**Description**: This use case describes the process by which high-value transaction funds are held in a system-controlled virtual wallet until specific legal conditions are met.
**Actor**: Buyer / Admin
**Trigger**: ❖ When the Buyer completes a payment for a purchase contract.
**Pre-condition**: 
❖ The user is logged in as Buyer.
❖ [contract.status] is 'WAITING_OFFICIAL'.
**Post-condition**: 
❖ Funds are held in 'HELD_IN_ESCROW' status.
❖ Funds are released to the Seller upon Admin approval.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|Buyer|
start
: (1) Pay for Purchase Contract via Gateway;
|System|
: (2) Receive payment confirmation (Refer to UC11);
: (3) Verify Escrow requirement;
: (4) Transfer amount to System Virtual Wallet;
: (5) Set [payment.status] = 'HELD_IN_ESCROW';
|Admin|
: (6) Review legal documents and title;
: (7) Click "Release Escrow";
|System|
: (8) Perform role validation;
if (Authorized?) then (yes)
  : (9) Transfer amount to Seller and set [contract.status] = 'ACTIVE';
  |Admin|
  : (10) Show success message MSG 3;
  stop
else (no)
  |Admin|
  : (11) Show error message MSG 12;
  stop
endif
@enduml
```

```plantuml
@startuml
actor Buyer
control "Payment Flow" as PF
control "EscrowService" as SVC
database "Ledger" as DB
actor Admin

Buyer -> PF : Pay (Success)
PF -> SVC : handleEscrow(paymentId)
SVC -> DB : (BR84) UPDATE [wallet] balance += [amount]
SVC -> DB : (BR85) UPDATE [payment] SET status = 'HELD_IN_ESCROW'
Admin -> SVC : releaseFunds(contractId)
SVC -> SVC : (BR86) Verify ADMIN/LEGAL role
alt Unauthorized
    SVC --> Admin : Show MSG 12
else Authorized
    SVC -> DB : (BR87) UPDATE [seller_wallet] balance += [amount]
    SVC -> DB : UPDATE [payment] SET status = 'SUCCESS'
    SVC -> DB : UPDATE [contract] SET status = 'ACTIVE'
    SVC --> Admin : Show MSG 3
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (3) | BR84 | **Validate Rules:**<br>❖ If [contract.type] == 'PURCHASE' then [payment.mode] = 'ESCROW' else [payment.mode] = 'DIRECT'. |
| (4) | BR84_B | **Ledger Rules:**<br>❖ [wallet] = Wallet Repository findByUserId([system.id]).<br>❖ [wallet.balance] = [wallet.balance] + [payment.amount].<br>❖ Wallet Repository save [wallet]. |
| (5) | BR85 | **Updating Rules:**<br>❖ [payment.status] = 'HELD_IN_ESCROW'.<br>❖ Payment Repository save [payment]. |
| (8) | BR86 | **Validate Rules:**<br>❖ If <<current user role>> not in ['ADMIN', 'LEGAL'] then show error message MSG 12. |
| (9) | BR87 | **Release Rules:**<br>❖ [sellerWallet] = Wallet Repository findByUserId([property.ownerId]).<br>❖ [sellerWallet.balance] = [sellerWallet.balance] + [payment.amount].<br>❖ [payment.status] = 'SUCCESS'.<br>❖ [contract.status] = 'ACTIVE'.<br>❖ Repository save all entities. |
| (10) | BR3 | **Message Rules:**<br>❖ The system shows success message MSG 3. |
