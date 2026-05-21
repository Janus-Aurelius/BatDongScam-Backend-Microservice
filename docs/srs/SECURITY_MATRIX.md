# User Access and Security Matrix

This matrix defines the permissions for each user role across all documented technical flows.

**Legend:**
- **X**: Full Access / Primary Actor
- **R**: Read-only Access
- **O**: Access restricted to "Owned" records only
- **-**: No Access

| UC ID | Use Case Name | Guest | Customer | Owner | Agent | Admin |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| **UC1** | Sign Up | X | - | - | - | - |
| **UC2** | Sign In | X | X | X | X | X |
| **UC3** | Create Property Listing | - | - | X | - | X |
| **UC4** | Search Properties | X | X | X | X | X |
| **UC5** | View Property Details | X | X | X | X | X |
| **UC6** | Book Appointment | - | X | - | - | X |
| **UC7** | Manage Appointment | - | O | O | O | X |
| **UC8** | Create Rental Draft | - | - | - | X | X |
| **UC9** | Sign Deposit Contract | - | X | - | - | R |
| **UC10** | Initialize Payment | - | X | X | - | R |
| **UC11** | Handle Payment Webhook | - | - | - | - | System |
| **UC12** | Report Violation | - | X | X | X | R |
| **UC13** | Review Violations | - | - | - | - | X |
| **UC14** | Send Push Notification | - | O | O | O | X |
| **UC15** | Send Email Alert | - | O | O | O | X |
| **UC16** | Generate Sales Report | - | - | - | - | X |
| **UC17** | Update Profile | - | O | O | O | X |
| **UC18** | Promote Property | - | - | X | - | X |
| **UC19** | Archive Property | - | - | O | - | X |
| **UC20** | Assign Agent | - | - | - | - | X |
| **UC21** | Fetch Location Data | X | X | X | X | X |
| **UC22** | Wishlist Property | - | X | - | - | - |
| **UC23** | Token Refresh | - | X | X | X | X |
| **UC24** | Real-time Agent Chat | - | X | X | X | X |
| **UC25** | AI Property Valuation | - | - | X | - | X |
| **UC26** | Augmented Reality Virtual Tour | X | X | X | X | X |
| **UC27** | Map-based Search | X | X | X | X | X |
| **UC28** | Escrow Protection | - | X | X | - | X |
| **UC29** | Referral Program | - | X | X | X | X |
| **UC30** | Agent Review | - | X | - | - | R |
| **UC31** | View Owner Insights | - | - | O | - | X |
| **UC32** | Scan Document with OCR | X | X | X | X | X |
| **UC33** | Subscribe to Plan | - | - | - | X | X |
| **UC34** | Change Language | X | X | X | X | X |
| **UC35** | View Audit Logs | - | - | - | - | X |
| **UC36** | Update Property | - | - | O | - | X |
| **UC37** | Manage User Account | - | - | - | - | X |
| **UC38** | Manage Property Types | - | - | - | - | X |
| **UC39** | View User Activity History | - | O | O | O | X |
| **UC40** | View Activity Details | - | O | O | O | X |

---

### Access Control Logic
1.  **Ownership Constraint (O)**: Users marked with "O" can only execute the use case or view data if they are the direct owner or assigned participant of the record (e.g., a Customer can only view *their own* appointments in UC39).
2.  **Administrative Override**: The **Admin** role has unrestricted access to all management and reporting functions, as well as the ability to override ownership constraints for support purposes.
3.  **Guest Access**: Limited strictly to discovery (Search, Details, Locations), account creation, and utility functions (OCR, Language).
