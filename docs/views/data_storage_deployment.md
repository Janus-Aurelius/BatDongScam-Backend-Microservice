# Data Storage & Security Deployment Diagram

This diagram focuses on the system's data layer, illustrating the storage strategy, encryption standards, and multi-tenant isolation.

@startuml
skinparam componentStyle uml2
skinparam linetype ortho
skinparam node {
  BackgroundColor White
  BorderColor Black
}

title Data Storage & Security Deployment Diagram

node "Client Tier" {
  [Client Application] as Client
}

node "Application Tier (EKS)" as AppTier {
  [Backend Microservices] as Services
}

node "Data Storage Tier (AWS Managed)" {

  node "PostgreSQL Cluster (RDS)" <<Transactional Store>> {
    database "Multi-tenant DB" {
      [Schema: Owner 1]
      [Schema: Owner 2]
      [Schema: ...]
    }
    note bottom: AES-256 Encryption at Rest\nAtomic Integrity
  }

  node "Elasticsearch Cluster" <<Search Engine>> {
    [Geo-spatial & Amenity Index]
    note bottom: Geofencing & Search Lookups
  }

  node "Redis Cluster (ElastiCache)" <<Multi-purpose>> {
    [Auth Cache]
    [Redlock Manager]
    [Chat Backplane]
    [Status Cache]
  }
}

node "External/Object Storage" {
  node "AWS S3" <<Vault>> {
    [Lease PDFs]
    [Floor Plans]
    note bottom: AES-256 Encryption at Rest
  }
  
  node "Cloudinary" <<CDN>> {
    [Maintenance Media]
    [High-res Photos]
  }
}

' Relationships with Protocol Labels
Client -- Services : TLS 1.3 (HTTPS/WSS)

Services --> [Multi-tenant DB] : JDBC / TLS 1.3\n(Leases, Properties)
Services --> [Geo-spatial & Amenity Index] : REST / TLS 1.3\n(Filtering)
Services --> [Auth Cache] : Redis Protocol\n(Permissions)
Services --> [Redlock Manager] : Distributed Locking
Services --> [Lease PDFs] : SDK / AWS Signature v4\n(Signed Documents)
Services --> [Maintenance Media] : API Call\n(Media Management)

@enduml
