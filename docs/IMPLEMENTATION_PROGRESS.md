# Feature Implementation Progress Report - Core Macroservice

This document tracks the migration and implementation progress of features from the legacy system to the new `bds-core-macroservice`.

**Last Updated:** May 20, 2026

---

## 🟢 Fully Implemented
*Core business logic, API endpoints, and event integration are complete.*

| ID | Feature | Owner | Technical Notes |
|:---|:---|:---|:---|
| **US-003** | Create Property Listing | Nhat Minh Ha | Strategy pattern for fees, Command pattern for actions. |
| **US-004** | Search & Filter Properties | Nhat Minh Ha | Repository-level filtering implemented. |
| **US-005** | Property Details View | Nhat Minh Ha | Caching implemented with `@Cacheable`. |
| **US-009** | Deposit Contract Execution | Thien An Nguyen | Full lifecycle from DRAFT to WAITING_OFFICIAL. |
| **US-019** | Archive Property | Nhat Minh Ha | Soft-delete via `DeletePropertyAction`. |
| **US-020** | Assign Agent to Property | Nhat Minh Ha | API and internal event handling ready. |
| **US-035** | System Audit Logs | (Infrastructure) | AOP Logging Aspect + OpenTelemetry integration. |

---

## 🟡 Partially Implemented
*Scaffolding exists, but specific ACs or external integrations are pending.*

| ID | Feature | Owner | Missing/Pending Items |
|:---|:---|:---|:---|
| **US-008** | Draft Rental Contract | Thien An Nguyen | PDF generation and Cloudinary upload (AC3). |
| **US-011** | Handle Payment Webhook | Thien An Nguyen | Webhook controller and signature verification. |
| **US-016** | Generate Sales Report | Thien An Nguyen | CSV/Excel export logic (AC2). |
| **US-031** | Owner Insights Dashboard | Nhat Minh Ha | Dashboard aggregation logic and UI data mapping. |

---

## 🔴 Not Yet Implemented / In Backlog
*No implementation found in the new macroservice core.*

| ID | Feature | Owner | Status |
|:---|:---|:---|:---|
| **US-010** | Initialize Payment | Thien An Nguyen | Redirect logic & provider integration needed. |
| **US-018** | Promote Property | Nhat Minh Ha | Financial point system integration needed. |
| **US-021** | Fetch Location Data | Nhat Minh Ha | Standardized API for City/District/Ward lists. |
| **US-022** | Wishlist Property | Nhat Minh Ha | Repository implementation for favorites. |
| **US-025** | AI Property Valuation | Nhat Minh Ha | ML Model integration. |
| **US-026** | AR Virtual Tour | Nhat Minh Ha | 3D Asset handling and AR API. |
| **US-028** | Escrow Protection | Thien An Nguyen | Multi-stage payment hold logic. |
| **US-030** | Agent Reviews | Thien An Nguyen | Rating submission API and review persistence. |

---

## 📂 Pending Migration from Legacy
*Features existing in legacy code but not yet ported to the macroservice architecture.*

*   **US-001/002/023 (Auth/JWT):** Currently handled by legacy security configs; needs porting to a dedicated Auth module/service.
*   **US-014/015 (Notifications):** Firebase and Email templates are in legacy but not yet integrated as outbound ports in Core.
*   **US-012/013 (Moderation):** Violation reporting logic needs to be adapted to the new domain model.
*   **US-024 (Real-time Chat):** Socket.io logic remains in legacy.

---

## 📈 Summary Statistics
- **Total Features:** 35
- **Fully Implemented:** 7 (20%)
- **Partially Implemented:** 4 (11%)
- **Pending/Legacy:** 24 (69%)
