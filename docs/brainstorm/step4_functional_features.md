# Functional Features - Property Management System

This document outlines 15 essential and advanced functional features designed for the Property Management System, balancing legacy capabilities with the transition to a microservice architecture.

| Category | Function | Description |
| :--- | :--- | :--- |
| **Property Management** | Multi-tier Property Inventory | Hierarchical management of portfolios, buildings, and individual units including geo-coordinates, amenities, and floor plans. |
| **Property Management** | Real-time Unit Availability | A dynamic status engine (Vacant, Occupied, Maintenance, Reserved) that updates across public listings and admin dashboards instantly. |
| **Tenant Onboarding** | Digital KYC & Background Screening | Automated identity verification and credit scoring for prospective tenants, integrated with third-party verification APIs. |
| **Leases** | Smart E-Lease Generator | Rule-based generation of digital lease agreements with e-signature support and automated clause customization based on property type. |
| **Rent Payments** | Integrated Payment Gateway | Seamless rent collection utilizing the legacy **Payway** webhook system, supporting credit cards, bank transfers, and automated receipts. |
| **Rent Payments** | Automated Late Fee Engine | A background scheduler that calculates and applies penalties based on lease grace periods and communicates balances to tenants. |
| **Maintenance** | Work Order Lifecycle Management | A ticketing system for tenants to submit repair requests with media attachments (via **Cloudinary**), assignable to specific workers. |
| **Maintenance** | Preventive Maintenance Scheduler | Recurring maintenance task automation for HVAC, fire safety, and plumbing to reduce emergency repair costs and ensure compliance. |
| **Communications** | Multi-Role Messaging Hub | An in-app chat system facilitating secure communication between Tenants, Property Managers, and Maintenance Workers. |
| **Communications** | Automated Push Notifications | Event-driven alerts (via **Firebase**) for rent reminders, lease expiration, maintenance arrival, and emergency building notices. |
| **Reporting** | Financial Performance Dashboard | High-level visualization of Net Operating Income (NOI), Gross Potential Rent, and cash flow analysis for Landlords and Admins. |
| **Reporting** | AI-Driven Vacancy Predictive Analytics | Analysis of historical turnover data to predict future vacancy risks and suggest optimal pricing adjustments to maintain occupancy. |
| **Reporting** | Maintenance Cost Distribution | Granular reporting on repair expenses per unit or property to identify aging assets and optimize capital expenditure (CAPEX). |
| **Document Management** | Centralized Document Vault | Secure, cloud-based storage for deeds, insurance certificates, and historical lease versions with granular access control. |
| **Admin/Access Control** | Fine-Grained RBAC Engine | A robust Role-Based Access Control system managing permissions across the microservice ecosystem (Tenants vs. Managers vs. Admins). |
