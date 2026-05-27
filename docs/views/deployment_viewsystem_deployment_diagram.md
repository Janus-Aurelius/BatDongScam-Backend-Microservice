# System Deployment Diagram

This diagram describes the physical deployment of system components within the AWS Cloud environment, emphasizing high availability and container orchestration.

@startuml
skinparam componentStyle uml2
skinparam linetype ortho
skinparam node {
  BackgroundColor White
  BorderColor Black
}

title System Deployment Diagram (AWS Infrastructure)

node "External Network" as Internet {
  [Web Browser / Mobile App] as Client
}

cloud "Amazon Web Services (AWS)" {

  node "AWS Region (e.g., us-east-1)" as Region {

    node "Application Load Balancer (ALB)" as ALB <<Service>> {
      port "SSL Termination (443)" as Port443
    }

    package "Availability Zone A (us-east-1a)" {
      node "Amazon EKS Cluster (Worker Nodes)" as EKSA {
        [Microservice Pods] as PodsA
      }
      database "Amazon RDS (Primary)" as RDSP <<PostgreSQL>>
      database "ElastiCache (Node 1)" as RedisA <<Redis>>
    }

    package "Availability Zone B (us-east-1b)" {
      node "Amazon EKS Cluster (Worker Nodes)" as EKSB {
        [Microservice Pods] as PodsB
      }
      database "Amazon RDS (Standby)" as RDSS <<PostgreSQL>>
      database "ElastiCache (Node 2)" as RedisB <<Redis>>
    }

    node "Amazon ECR" as ECR <<Registry>>
    node "Amazon CloudWatch" as CW <<Monitoring>>
    
    node "Monitoring Node" {
      [Prometheus]
      [Grafana]
    }
  }
}

' Relationships
Client --> Port443 : HTTPS (REST/WS)
Port443 --> ALB
ALB --> PodsA : Route Traffic
ALB --> PodsB : Route Traffic

PodsA --> RDSP : SQL
PodsB --> RDSP : SQL
RDSP .[#blue].> RDSS : Synchronous Replication (Multi-AZ)

PodsA --> RedisA : Cache/PubSub
PodsB --> RedisB : Cache/PubSub

PodsA --> CW : Push Logs
PodsB --> CW : Push Logs

ECR ..> EKSA : Pull Images
ECR ..> EKSB : Pull Images

Prometheus ..> EKSA : Scrape Metrics
Prometheus ..> EKSB : Scrape Metrics
Grafana --> Prometheus : Query

@enduml
