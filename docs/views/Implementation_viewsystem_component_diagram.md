# System Component Diagram

This diagram outlines the organization of code, deployment artifacts, and the structural relationships between the microservices and infrastructure.

@startuml
skinparam componentStyle uml2
skinparam linetype ortho

title System Component Diagram (Modular Decomposition)

package "Frontend (React/Next.js)" {
  [Tenant Dashboard] as TenantDB
  [Manager Dashboard] as ManagerDB
  [Worker Dashboard] as WorkerDB
}

package "API Gateway Layer" {
  [API Gateway] as Gateway
}

node "Backend Systems (Kubernetes)" {
  
  package "Java Services (Spring Boot)" {
    component "Core Macroservice" as CoreSvc {
      [Property Module]
      [Lease Module]
      [Onboarding Module]
      [Maintenance Module]
    }
    
    component "Auth Service" as AuthSvc
    component "Notification Service" as NotifSvc
    component "Communication Service" as CommSvc
    
    artifact "bds-common" as CommonLib <<Library>>
  }
  
  package "Python Services (FastAPI)" {
    component "AI/ML Predictive Analytics" as AISvc
  }
}

package "Infrastructure" {
  queue "RabbitMQ / Kafka" as EventBus
  database "Redis (Pub/Sub & Cache)" as Redis
  database "PostgreSQL" as DB
  storage "AWS S3 / Vault" as S3
}

' Relationships
TenantDB --> Gateway : HTTPS/REST
ManagerDB --> Gateway : HTTPS/REST
WorkerDB --> Gateway : HTTPS/REST

Gateway --> AuthSvc : Security Verification
Gateway --> CoreSvc : Route Request
Gateway --> AISvc : Predictive Queries

CoreSvc ..> CommonLib : <<use>>
AuthSvc ..> CommonLib : <<use>>
NotifSvc ..> CommonLib : <<use>>

CoreSvc --> EventBus : Publish "PropertyStatusChanged"
EventBus --> NotifSvc : Subscribe for Alerts
EventBus --> AISvc : Data Ingestion

CommSvc --> Redis : WebSocket Backplane
CoreSvc --> Redis : Cache/Redlock

CoreSvc --> DB : SQL
AISvc --> DB : Read Replicas
CoreSvc --> S3 : Document Storage (AES-256)

@enduml
