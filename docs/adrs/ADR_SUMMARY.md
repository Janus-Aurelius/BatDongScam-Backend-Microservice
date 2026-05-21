# BatDongScam Architectural Decision Records (ADR) Summary

| ID | Decision | Rationale | Trade-off & Description |
|:---|:---|:---|:---|
| **AD-001** | **Microservices Architecture** | Enable independent scaling and deployment for complex domains (Property, Transaction, IAM). | Increases operational complexity; requires robust service discovery and CI/CD. |
| **AD-002** | **API Gateway** | Single entry point to centralize security, routing, and cross-cutting concerns. | Potential bottleneck and single point of failure; requires high availability. |
| **AD-003** | **Stateless JWT Auth** | Decouples authentication from session state, enabling horizontal scaling. | Revocation is complex; requires short-lived tokens and refresh lifecycle. |
| **AD-004** | **Redis Pub/Sub Events** | Enables async communication and loose coupling between domains. | At-most-once delivery risk; requires robust handling for critical events. |
| **AD-005** | **Polyglot Persistence** | Matches storage tech (Postgres/Mongo) to specific workload requirements. | Increased operational overhead; requires strict data boundary ownership. |
| **AD-006** | **Postgres + MongoDB** | Transactional integrity for contracts (Postgres) + flexibility for search/analytics (Mongo). | Data synchronization overhead between relational and document stores. |
| **AD-007** | **Module Facades** | Enforce strict boundaries within the macroservice to prevent circular dependencies. | Adds boilerplate mapping; requires discipline to avoid "God Facades." |
| **AD-008** | **Shared Kernel** | Centralize common DTOs, Events, and Exceptions for cross-service consistency. | Introduces a deployment-time coupling across all services using the kernel. |
| **AD-009** | **Transactional Outbox** | Ensure "at-least-once" event delivery by linking DB updates with event persistence. | Adds database write load and requires a separate relay/polling process. |
| **AD-010** | **Circuit Breakers** | Prevent cascading failures when external services (Legacy/PayPal) are slow or down. | Increases code complexity; requires defining sensible fallback behaviors. |
| **AD-011** | **Distributed Tracing** | Gain visibility into request lifecycles across distributed service boundaries. | Minor performance overhead; requires infrastructure to collect/view traces. |
| **AD-012** | **Database-per-Service** | Achieve true autonomy and independent scaling for each microservice. | Breaks SQL joins; requires API composition or event-driven data replication. |
