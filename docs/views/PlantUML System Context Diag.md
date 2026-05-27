@startuml
left to right direction
skinparam packageStyle rectangle

' Define External Entities
rectangle "Customer (User)" as customer
rectangle "Sale Agent (User)" as agent
rectangle "Property Owner (User)" as owner
rectangle "Admin (Role)" as admin

rectangle "Firebase Auth (Identity)" as firebaseAuth
rectangle "Cloudinary (Media Storage)" as cloudinary
rectangle "PayOS / PayPal (Payment Gateway)" as paymentGateway
rectangle "Firebase FCM (Push Notifications)" as fcm

' Define Central System
usecase "BatDongScam Platform\n(Real Estate & Contract System)" as coreSystem

' Define Relationships (User Interactions)
customer --> coreSystem : "Searches properties, books appointments, pays deposits"
coreSystem --> customer : "Returns search results, contract statuses"

agent --> coreSystem : "Lists properties, manages viewings"
coreSystem --> agent : "Notifies of bookings, updates contract status"

owner --> coreSystem : "Monitors property listings"
coreSystem --> owner : "Processes commission payouts, sends alerts"

admin --> coreSystem : "Moderates violations, views analytics"
coreSystem --> admin : "Provides audit logs, dashboards"

' Define Relationships (External Systems)
coreSystem --> firebaseAuth : "Validates JWT / Syncs identity state"
firebaseAuth --> coreSystem : "Returns authentication status"

coreSystem --> cloudinary : "Uploads property media & documents"
cloudinary --> coreSystem : "Returns hosted media URLs"

coreSystem --> paymentGateway : "Creates payment sessions & payouts"
paymentGateway --> coreSystem : "Triggers webhooks (Succeeded/Failed)"

coreSystem --> fcm : "Sends push notification payloads"
fcm --> coreSystem : "Delivers notifications to devices"

@enduml
