# Property & Lease Lifecycle State Machine

This diagram illustrates the lifecycle of a property listing and its associated lease, as managed by the microservices architecture.

@startuml
skinparam componentStyle uml2
skinparam linetype ortho
skinparam state {
  BackgroundColor White
  BorderColor Black
  ArrowColor Black
}

title Property & Lease Lifecycle State Machine

[*] --> PENDING_ONBOARDING : Property Listing Created\n(Property Service)

state PENDING_ONBOARDING {
  [*] --> KYC_VERIFICATION
  KYC_VERIFICATION --> BACKGROUND_SCREENING : Identity Confirmed (OIDC)
  BACKGROUND_SCREENING --> [*] : Screening Complete (Checkr/Experian)
}

PENDING_ONBOARDING --> APPROVED : Verification Success\n(Onboarding Service)
PENDING_ONBOARDING --> REJECTED : Verification Failed

APPROVED --> VACANT : Initialize Status\n(Availability Service: Redlock)

state VACANT {
  [*] --> AVAILABLE_FOR_RENT
  AVAILABLE_FOR_RENT --> VIEWING_SCHEDULED : Appointment Booked
  VIEWING_SCHEDULED --> AVAILABLE_FOR_RENT : Appointment Cancelled/Completed
}

VACANT --> LEASE_GENERATION : Rental Application Accepted

state LEASE_GENERATION {
  [*] --> PDF_DRAFTING : Generate Smart E-Lease (< 5s)
  PDF_DRAFTING --> DOCUMENT_VAULT : Store in S3 (AES-256)
  DOCUMENT_VAULT --> SIGNATURE_PENDING : Trigger DocuSign Workflow
}

LEASE_GENERATION --> PENDING_PAYMENT : Lease Signed

PENDING_PAYMENT --> OCCUPIED : Rent Collected (Stripe/Payway)\n(Payment Service: Webhook)

state OCCUPIED {
  [*] --> ACTIVE_TENANCY
  ACTIVE_TENANCY --> MAINTENANCE_LIFECYCLE : Work Order Created
  MAINTENANCE_LIFECYCLE --> ACTIVE_TENANCY : Work Order Resolved\n(Maintenance Service)
  
  ACTIVE_TENANCY --> LATE_FEE_CALCULATION : Payment Overdue\n(Accounting Service)
  LATE_FEE_CALCULATION --> ACTIVE_TENANCY : Payment Received
}

OCCUPIED --> VACANT : Lease Termination\n(Saga Pattern: Final Accounting)

REJECTED --> [*]
VACANT --> [*] : Listing Removed
@enduml
