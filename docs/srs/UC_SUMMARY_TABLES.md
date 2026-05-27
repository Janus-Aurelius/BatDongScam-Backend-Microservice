# Use Case Summary Tables

This document contains a consolidated list of identification and condition sections for all 35 Use Cases, formatted as two-column tables for easy transfer to Microsoft Word.

---

## UC1: Sign Up
| Field | Value |
| :--- | :--- |
| **Name** | Sign Up |
| **Description** | This use case describes the process by which a user creates a new account in the system. |
| **Actor** | Guest |
| **Trigger** | ❖ When the user clicks on the “Sign Up” button. |
| **Pre-condition** | ❖ The user is on the sign up page (refer to “Sign Up Form” in “List description” file). |
| **Post-condition** | ❖ A new account has been created in the ‘PENDING_APPROVAL’ or 'ACTIVE' state.<br>❖ The user will be redirected to the home page. |

---

## UC2: Sign In
| Field | Value |
| :--- | :--- |
| **Name** | Sign In |
| **Description** | This use case describes the process by which a user logs into the system. |
| **Actor** | User |
| **Trigger** | ❖ When the user clicks on the “Sign In” button. |
| **Pre-condition** | ❖ The user is not logged in to the system.<br>❖ The user is in the sign in page (refer to “Sign In Form” in “List description” file). |
| **Post-condition** | ❖ The user is logged in to the system.<br>❖ The user is redirected to the home page. |

---

## UC3: Create Property Listing
| Field | Value |
| :--- | :--- |
| **Name** | Create Property Listing |
| **Description** | This use case allows Owners or Admins to create a property listing with images and documents. |
| **Actor** | Owner / Admin |
| **Trigger** | ❖ When the user clicks on the “Submit Listing” button. |
| **Pre-condition** | ❖ The user is logged in to the system.<br>❖ The user is in the create property page. |
| **Post-condition** | ❖ The property listing has been created. |

---

## UC4: Search Properties
| Field | Value |
| :--- | :--- |
| **Name** | Search Properties |
| **Description** | This use case describes the process by which a user searches for properties using various filters such as location, price, and property type. |
| **Actor** | User |
| **Trigger** | ❖ When the user clicks on the “Search” button or applies a filter. |
| **Pre-condition** | ❖ The user is on the property discovery page. |
| **Post-condition** | ❖ The system displays a list of properties matching the search criteria. |

---

## UC5: View Property Details
| Field | Value |
| :--- | :--- |
| **Name** | View Property Details |
| **Description** | This use case describes the process by which a user views the comprehensive information of a specific property. |
| **Actor** | User |
| **Trigger** | ❖ When the user clicks on a property card. |
| **Pre-condition** | ❖ The user is viewing a list of properties. |
| **Post-condition** | ❖ The system displays the full details, media, and contact information for the selected property. |

---

## UC6: Book Appointment
| Field | Value |
| :--- | :--- |
| **Name** | Book Appointment |
| **Description** | This use case allows a user to request a viewing appointment for a specific property. |
| **Actor** | Customer |
| **Trigger** | ❖ When the user clicks on the “Book Viewing” button. |
| **Pre-condition** | ❖ The user is logged in to the system.<br>❖ The user is on the property details page. |
| **Post-condition** | ❖ A viewing appointment has been created in 'PENDING' or 'CONFIRMED' status. |

---

## UC7: Manage Appointment
| Field | Value |
| :--- | :--- |
| **Name** | Manage Appointment |
| **Description** | This use case describes how an authorized user can confirm an appointment (by assigning a Sales Agent) or cancel a viewing request. |
| **Actor** | Admin / Agent / Owner |
| **Trigger** | ❖ When the user clicks the “Confirm/Assign Agent” or “Cancel Appointment” button. |
| **Pre-condition** | ❖ The user is logged in and has appropriate permissions for the appointment.<br>❖ [appointment.status] is 'PENDING' or 'CONFIRMED'. |
| **Post-condition** | ❖ The appointment status is updated.<br>❖ Involved parties are notified of the change. |

---

## UC8: Create Rental Contract Draft
| Field | Value |
| :--- | :--- |
| **Name** | Create Rental Contract Draft |
| **Description** | This use case allows an Admin or Agent to generate a draft for a rental contract using property and party information. |
| **Actor** | Admin / Agent |
| **Trigger** | ❖ When the user clicks on the “Create Draft” button. |
| **Pre-condition** | ❖ The user is logged in as Admin or Agent.<br>❖ No active rental contract exists for the target property. |
| **Post-condition** | ❖ A new rental contract record is created in 'DRAFT' status. |

---

## UC9: Sign Deposit Contract
| Field | Value |
| :--- | :--- |
| **Name** | Sign Deposit Contract |
| **Description** | This use case describes the process by which a customer signs a draft deposit contract to reserve a property. |
| **Actor** | Customer |
| **Trigger** | ❖ When the user clicks on the “Sign Contract” button. |
| **Pre-condition** | ❖ The user is logged in as the assigned customer of a 'DRAFT' deposit contract. |
| **Post-condition** | ❖ The contract status is updated to 'WAITING_OFFICIAL'.<br>❖ The property status is updated to 'UNAVAILABLE'. |

---

## UC10: Initialize Payment
| Field | Value |
| :--- | :--- |
| **Name** | Initialize Payment |
| **Description** | This use case describes the process by which a user initiates a payment for a contract or service via an external payment gateway. |
| **Actor** | User |
| **Trigger** | ❖ When the user clicks on the “Pay Now” button. |
| **Pre-condition** | ❖ The user is logged in to the system.<br>❖ The target contract is in a state that allows payment (e.g., 'WAITING_OFFICIAL'). |
| **Post-condition** | ❖ A payment record is created in 'PENDING' status.<br>❖ The user is redirected to the external payment gateway. |

---

## UC11: Handle Payment Webhook
| Field | Value |
| :--- | :--- |
| **Name** | Handle Payment Webhook |
| **Description** | This use case describes how the system processes asynchronous payment confirmation notifications from external gateways. |
| **Actor** | System |
| **Trigger** | ❖ When the external gateway sends a POST request to the webhook endpoint. |
| **Pre-condition** | ❖ The system endpoint is public and the gateway has a valid signed payload. |
| **Post-condition** | ❖ The payment status is updated to 'SUCCESS'.<br>❖ The associated contract status is updated to 'ACTIVE'. |

---

## UC12: Report Violation
| Field | Value |
| :--- | :--- |
| **Name** | Report Violation |
| **Description** | This use case allows a user to report suspicious property listings or user behavior to the administration. |
| **Actor** | User |
| **Trigger** | ❖ When the user clicks on the “Submit Report” button. |
| **Pre-condition** | ❖ The user is logged in to the system. |
| **Post-condition** | ❖ A violation report is created in 'PENDING' status for admin review. |

---

## UC13: Review Violations
| Field | Value |
| :--- | :--- |
| **Name** | Review Violations |
| **Description** | This use case describes how an Administrator reviews a reported violation and applies disciplinary actions if necessary. |
| **Actor** | Admin |
| **Trigger** | ❖ When the Admin clicks on the “Resolve/Update” button on a violation report. |
| **Pre-condition** | ❖ The Admin is logged in.<br>❖ The violation report exists and is in 'PENDING' or 'UNDER_REVIEW' status. |
| **Post-condition** | ❖ The violation report status is updated to 'RESOLVED' or 'DISMISSED'.<br>❖ Disciplinary actions are applied to the target entity (user or property). |

---

## UC14: Send Push Notification
| Field | Value |
| :--- | :--- |
| **Name** | Send Push Notification |
| **Description** | This use case describes the process by which the system sends real-time alerts to users based on system events. |
| **Actor** | System |
| **Trigger** | ❖ When a specific system event (e.g., appointment booked, payment received) occurs. |
| **Pre-condition** | ❖ The target user has at least one valid FCM token registered in the system. |
| **Post-condition** | ❖ The notification record is saved and the message is dispatched to the user's device via FCM. |

---

## UC15: Send Email Alert
| Field | Value |
| :--- | :--- |
| **Name** | Send Email Alert |
| **Description** | This use case describes the process by which the system sends automated email notifications and documents to users. |
| **Actor** | System |
| **Trigger** | ❖ When a specific system event requiring email notification occurs (e.g., payment success). |
| **Pre-condition** | ❖ The target user has a valid [email] address. |
| **Post-condition** | ❖ The email is sent using the appropriate template and any necessary attachments. |

---

## UC16: Generate Sales Report
| Field | Value |
| :--- | :--- |
| **Name** | Generate Sales Report |
| **Description** | This use case describes how an Administrator can generate financial and transaction reports for a specific year. |
| **Actor** | Admin |
| **Trigger** | ❖ When the user clicks the "Generate Report" button. |
| **Pre-condition** | ❖ The user is logged in as Admin. |
| **Post-condition** | ❖ The system displays the financial and transaction statistics for the requested year. |

---

## UC17: Update Profile
| Field | Value |
| :--- | :--- |
| **Name** | Update Profile |
| **Description** | This use case describes the process by which a user updates their personal information and avatar. |
| **Actor** | User |
| **Trigger** | ❖ When the user clicks the "Save Changes" button. |
| **Pre-condition** | ❖ The user is logged in to the system. |
| **Post-condition** | ❖ The user's profile information has been updated in the database. |

---

## UC18: Promote Property
| Field | Value |
| :--- | :--- |
| **Name** | Promote Property |
| **Description** | This use case describes how a Property Owner pays to increase the ranking and visibility of their listing. |
| **Actor** | Owner |
| **Trigger** | ❖ When the user selects a promotion package and confirms the transaction. |
| **Pre-condition** | ❖ The user is logged in as Owner.<br>❖ The target property is in 'AVAILABLE' status. |
| **Post-condition** | ❖ The property ranking points have been increased. |

---

## UC19: Archive Property
| Field | Value |
| :--- | :--- |
| **Name** | Archive Property |
| **Description** | This use case describes the process by which a Property Owner or Administrator soft-deletes a property listing from the platform. |
| **Actor** | Owner / Admin |
| **Trigger** | ❖ When the user clicks on the “Delete/Archive” button on a property listing. |
| **Pre-condition** | ❖ The user is logged in.<br>❖ The user has ownership of the property or has administrative privileges. |
| **Post-condition** | ❖ The property listing status is updated to 'DELETED'.<br>❖ The property is no longer visible in public searches. |

---

## UC20: Assign Agent to Property
| Field | Value |
| :--- | :--- |
| **Name** | Assign Agent to Property |
| **Description** | This use case describes how an Administrator assigns a Sales Agent to manage a specific property listing. |
| **Actor** | Admin |
| **Trigger** | ❖ When the Admin selects an agent and confirms the assignment. |
| **Pre-condition** | ❖ The user is logged in as Admin. |
| **Post-condition** | ❖ The property record is updated with the assigned Sales Agent ID.<br>❖ The Sales Agent is notified of the assignment. |

---

## UC21: Fetch Location Data
| Field | Value |
| :--- | :--- |
| **Name** | Fetch Location Data |
| **Description** | This use case describes the retrieval of hierarchical location data (Provinces, Districts, Wards) for use in forms and filters. |
| **Actor** | User |
| **Trigger** | ❖ When the user interacts with a location selection component (e.g., dropdown). |
| **Pre-condition** | ❖ None. |
| **Post-condition** | ❖ The system returns a list of location items matching the requested level and parent ID. |

---

## UC22: Wishlist Property
| Field | Value |
| :--- | :--- |
| **Name** | Wishlist Property |
| **Description** | This use case describes how a customer can add or remove a property from their personal favorites list for later review. |
| **Actor** | Customer |
| **Trigger** | ❖ When the user clicks on the “Favorite/Heart” icon. |
| **Pre-condition** | ❖ The user is logged in to the system. |
| **Post-condition** | ❖ The property association with the user's wishlist is updated. |

---

## UC23: Token Refresh
| Field | Value |
| :--- | :--- |
| **Name** | Token Refresh |
| **Description** | This use case describes the process by which the system issues a new set of authentication tokens using a valid refresh token. |
| **Actor** | User |
| **Trigger** | ❖ When the client application detects an expired access token or upon initial launch. |
| **Pre-condition** | ❖ The user has a valid, non-expired refresh token stored locally. |
| **Post-condition** | ❖ The system issues a new JWT access token and a rotated refresh token. |

---

## UC24: Real-time Agent Chat
| Field | Value |
| :--- | :--- |
| **Name** | Real-time Agent Chat |
| **Description** | This use case describes the real-time exchange of messages between a Customer and a Sales Agent regarding a property listing. |
| **Actor** | Customer / Agent |
| **Trigger** | ❖ When the user sends a message in the chat interface. |
| **Pre-condition** | ❖ Both parties are logged in.<br>❖ A WebSocket/Socket.io connection is established. |
| **Post-condition** | ❖ The message is delivered to the recipient and saved in the message history. |

---

## UC25: AI Property Valuation
| Field | Value |
| :--- | :--- |
| **Name** | AI Property Valuation |
| **Description** | This use case describes how the system uses artificial intelligence to estimate the market value of a property based on its features and historical data. |
| **Actor** | Owner |
| **Trigger** | ❖ When the user clicks on the “Estimate Value” button. |
| **Pre-condition** | ❖ The user is logged in as Owner.<br>❖ The property has basic data provided (area, location, type). |
| **Post-condition** | ❖ An AI-estimated value is displayed and saved to the property record. |

---

## UC26: Augmented Reality Virtual Tour
| Field | Value |
| :--- | :--- |
| **Name** | Augmented Reality Virtual Tour |
| **Description** | This use case describes the process of initializing an Augmented Reality (AR) session for a 3D visualization of a property. |
| **Actor** | Customer |
| **Trigger** | ❖ When the user clicks on the “AR Tour” button. |
| **Pre-condition** | ❖ The user is viewing property details on a mobile device.<br>❖ The property has a processed 3D model asset. |
| **Post-condition** | ❖ An AR session is started on the device. |

---

## UC27: Map-based Search
| Field | Value |
| :--- | :--- |
| **Name** | Map-based Search |
| **Description** | This use case describes how a user can find properties by drawing an area on a map or navigating the map view. |
| **Actor** | User |
| **Trigger** | ❖ When the user interacts with the map (draws area or pans view). |
| **Pre-condition** | ❖ The user is on the property search page. |
| **Post-condition** | ❖ Property markers within the selected area are displayed on the map. |

---

## UC28: Escrow Protection
| Field | Value |
| :--- | :--- |
| **Name** | Escrow Protection |
| **Description** | This use case describes the process by which high-value transaction funds are held in a system-controlled virtual wallet until specific legal conditions are met. |
| **Actor** | Buyer / Admin |
| **Trigger** | ❖ When the Buyer completes a payment for a purchase contract. |
| **Pre-condition** | ❖ The user is logged in as Buyer.<br>❖ [contract.status] is 'WAITING_OFFICIAL'. |
| **Post-condition** | ❖ Funds are held in 'HELD_IN_ESCROW' status.<br>❖ Funds are released to the Seller upon Admin approval. |

---

## UC29: Referral Program
| Field | Value |
| :--- | :--- |
| **Name** | Referral Program |
| **Description** | This use case describes how the system tracks user invitations and awards reward points upon successful transactions by referred users. |
| **Actor** | User |
| **Trigger** | ❖ When a Guest registers using a unique referral code. |
| **Pre-condition** | ❖ The Referrer has an active account. |
| **Post-condition** | ❖ A referral link is established.<br>❖ Points are awarded to both parties after the referee's first successful payment. |

---

## UC30: Agent Review
| Field | Value |
| :--- | :--- |
| **Name** | Agent Review |
| **Description** | This use case describes the process by which a customer rates and provides feedback on a Sales Agent following a completed interaction. |
| **Actor** | Customer |
| **Trigger** | ❖ When the user clicks on the “Rate Agent” button. |
| **Pre-condition** | ❖ The user is logged in as Customer.<br>❖ The user has a 'COMPLETED' appointment or 'ACTIVE' contract with the agent. |
| **Post-condition** | ❖ The review is saved and the agent's aggregate rating is updated. |

---

## UC31: View Owner Insights
| Field | Value |
| :--- | :--- |
| **Name** | View Owner Insights |
| **Description** | This use case describes how a Property Owner can view engagement metrics and conversion statistics for their listings. |
| **Actor** | Owner |
| **Trigger** | ❖ When the user clicks on the “Insights” tab in the dashboard. |
| **Pre-condition** | ❖ The user is logged in as Owner. |
| **Post-condition** | ❖ Engagement charts and conversion metrics are displayed. |

---

## UC32: Scan Document with OCR
| Field | Value |
| :--- | :--- |
| **Name** | Scan Document with OCR |
| **Description** | This use case describes the automated extraction of text from identity or land documents to facilitate form completion. |
| **Actor** | User |
| **Trigger** | ❖ When the user uploads a document image to an OCR-enabled field. |
| **Pre-condition** | ❖ The user is on a registration or verification page. |
| **Post-condition** | ❖ Extracted text fields are populated in the UI form. |

---

## UC33: Subscribe to Plan
| Field | Value |
| :--- | :--- |
| **Name** | Subscribe to Plan |
| **Description** | This use case describes how a Sales Agent can purchase a premium subscription plan to increase their property listing capacity. |
| **Actor** | Sales Agent |
| **Trigger** | ❖ When the user selects a subscription plan and clicks "Subscribe". |
| **Pre-condition** | ❖ The user is logged in as Sales Agent. |
| **Post-condition** | ❖ The agent's tier is updated and a subscription record is created. |

---

## UC34: Change Language
| Field | Value |
| :--- | :--- |
| **Name** | Change Language |
| **Description** | This use case describes the process by which a user switches the application interface and content language. |
| **Actor** | User |
| **Trigger** | ❖ When the user selects a language from the interface. |
| **Pre-condition** | ❖ None. |
| **Post-condition** | ❖ The UI is localized to the selected language. |

---

## UC35: View Audit Logs
| Field | Value |
| :--- | :--- |
| **Name** | View Audit Logs |
| **Description** | This use case describes how an Administrator can view the history of sensitive system actions and state changes. |
| **Actor** | Admin |
| **Trigger** | ❖ When the Admin clicks on the “Audit Logs” menu item. |
| **Pre-condition** | ❖ The user is logged in as Admin. |
| **Post-condition** | ❖ The system displays a paginated list of audit records. |

---

## UC36: Update Property
| Field | Value |
| :--- | :--- |
| **Name** | Update Property |
| **Description** | This use case describes the process by which a Property Owner or Administrator updates the information and media of an existing property listing. |
| **Actor** | Owner / Admin |
| **Trigger** | ❖ When the user clicks the “Save” button in the property edit form. |
| **Pre-condition** | ❖ The user is logged in.<br>❖ The user owns the property or has administrative privileges. |
| **Post-condition** | ❖ The property record is updated in the database.<br>❖ If updated by an Owner, the property status may be reset to 'PENDING'. |

---

## UC37: Manage User Account
| Field | Value |
| :--- | :--- |
| **Name** | Manage User Account |
| **Description** | This use case allows an Administrator to perform administrative actions on user accounts, including updating roles, statuses, or deleting the account. |
| **Actor** | Admin |
| **Trigger** | ❖ When the Admin clicks the “Update” or “Delete” button in the user management UI. |
| **Pre-condition** | ❖ The user is logged in as Admin.<br>❖ The target user account exists in the system. |
| **Post-condition** | ❖ The user account is updated or soft-deleted. |

---

## UC38: Manage Property Types
| Field | Value |
| :--- | :--- |
| **Name** | Manage Property Types |
| **Description** | This use case describes how an Administrator manages the global list of property categories (e.g., Apartment, Villa, Office). |
| **Actor** | Admin |
| **Trigger** | ❖ When the Admin clicks the “Save” or “Delete” button in the property type settings. |
| **Pre-condition** | ❖ The user is logged in as Admin. |
| **Post-condition** | ❖ The property type definitions are created, updated, or removed. |

---

## UC39: View User Activity History
| Field | Value |
| :--- | :--- |
| **Name** | View User Activity History |
| **Description** | This use case describes how users can view a paginated list of their personal appointments, contracts, and payments. |
| **Actor** | User |
| **Trigger** | ❖ When the user navigates to a historical activity page (e.g., “My Appointments”). |
| **Pre-condition** | ❖ The user is logged in to the system. |
| **Post-condition** | ❖ A list of records associated with the user is displayed. |

---

## UC40: View Activity Details
| Field | Value |
| :--- | :--- |
| **Name** | View Activity Details |
| **Description** | This use case describes the retrieval of full technical and legal details for a specific appointment or contract. |
| **Actor** | User |
| **Trigger** | ❖ When the user clicks on an item in an activity history list. |
| **Pre-condition** | ❖ The user is logged in.<br>❖ The user is authorized to view the specific record. |
| **Post-condition** | ❖ Detailed data, including linked property and documents, is displayed. |
