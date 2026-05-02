# Database Tables Documentation

This document provides detailed information about all tables in the BatDongScam database system.

---

## Table: appointment

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | appointment_id | uuid | PRIMARY KEY, NOT NULL | Unique identifier for each appointment |
| 2 | created_at | timestamp(6) without time zone | | Timestamp when record was created |
| 3 | updated_at | timestamp(6) without time zone | | Timestamp when record was last updated |
| 4 | agent_notes | text | | Notes written by the agent about the appointment |
| 5 | cancelled_at | timestamp(6) without time zone | | Timestamp when appointment was cancelled |
| 6 | cancelled_by | smallint | | Enum indicating role of who cancelled (customer/agent) |
| 7 | cancelled_reason | varchar(255) | | Reason provided for cancellation |
| 8 | comment | text | | Customer's comment/feedback about the appointment |
| 9 | confirmed_date | timestamp(6) without time zone | | When the appointment was confirmed |
| 10 | customer_interest_level | varchar(255) | | Level of customer interest after viewing |
| 11 | customer_requirements | text | | Customer's specific requirements or preferences |
| 12 | rating | smallint | | Rating given by customer for the appointment |
| 13 | requested_date | timestamp(6) without time zone | NOT NULL | When the appointment is requested to take place |
| 14 | status | smallint | | Current status of appointment (pending/confirmed/completed/cancelled) |
| 15 | viewing_outcome | text | | Notes about the outcome after property viewing |
| 16 | agent_id | uuid | FOREIGN KEY → sale_agents | Agent assigned to handle this appointment |
| 17 | customer_id | uuid | FOREIGN KEY → customers, NOT NULL | Customer who booked the appointment |
| 18 | property_id | uuid | FOREIGN KEY → properties, NOT NULL | Property to be viewed |

---

## Table: cities

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | city_id | uuid | PRIMARY KEY, NOT NULL | Unique identifier for each city |
| 2 | created_at | timestamp(6) without time zone | | Timestamp when record was created |
| 3 | updated_at | timestamp(6) without time zone | | Timestamp when record was last updated |
| 4 | avg_land_price | numeric(15, 2) | | Average land price in the city |
| 5 | city_name | varchar(255) | | Name of the city |
| 6 | description | varchar(255) | | Description of the city |
| 7 | img_url | varchar(255) | | URL to city's representative image |
| 8 | is_active | boolean | | Whether this city is currently active in the system |
| 9 | population | integer | | Population count of the city |
| 10 | total_area | numeric(15, 2) | | Total area of the city (in square meters/kilometers) |

---

## Table: contract

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | contract_type | varchar(31) | NOT NULL | Discriminator for contract inheritance (rental/purchase/deposit) |
| 2 | contract_id | uuid | PRIMARY KEY, NOT NULL | Unique identifier for each contract |
| 3 | created_at | timestamp(6) without time zone | | Timestamp when record was created |
| 4 | updated_at | timestamp(6) without time zone | | Timestamp when record was last updated |
| 5 | cancellation_penalty | numeric(15, 2) | | Penalty amount if contract is cancelled |
| 6 | cancellation_reason | text | | Reason for contract cancellation |
| 7 | cancelled_by | varchar(255) | | Role of party who cancelled the contract |
| 8 | comment | text | | Customer's comment/review of the contract |
| 9 | contract_number | varchar(50) | UNIQUE | Physical contract document number/ID |
| 10 | end_date | date | | When the contract terms end |
| 11 | rating | smallint | | Rating given for the contract experience |
| 12 | signed_at | timestamp(6) without time zone | | When physical contract was signed |
| 13 | special_terms | text | | Any special terms or conditions |
| 14 | start_date | date | NOT NULL | When the contract terms become effective |
| 15 | status | varchar(255) | NOT NULL | Current status of contract (draft/active/completed/cancelled) |
| 16 | agent_id | uuid | FOREIGN KEY → sale_agents | Agent handling this contract |
| 17 | customer_id | uuid | FOREIGN KEY → customers, NOT NULL | Customer in the contract |
| 18 | property_id | uuid | FOREIGN KEY → properties, NOT NULL | Property subject of the contract |

---

## Table: customers

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | customer_id | uuid | PRIMARY KEY, FOREIGN KEY → users, NOT NULL | References user_id, extends user entity |
| 2 | created_at | timestamp(6) without time zone | | Timestamp when record was created |
| 3 | updated_at | timestamp(6) without time zone | | Timestamp when record was last updated |

**Note**: This is an extension table that inherits from `users` table. All customer data is stored in users table with role=CUSTOMER.

---

## Table: deposit_contract

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | agreed_price | numeric(15, 2) | NOT NULL | The agreed upon price for the property |
| 2 | deposit_amount | numeric(15, 2) | NOT NULL | Amount of deposit paid |
| 3 | main_contract_type | varchar(255) | NOT NULL | Type of main contract this deposit is for (rental/purchase) |
| 4 | contract_id | uuid | PRIMARY KEY, FOREIGN KEY → contract, NOT NULL | References parent contract |

**Note**: This extends the `contract` table using joined inheritance strategy. Used for holding deposits before finalizing main contract.

---

## Table: districts

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | district_id | uuid | PRIMARY KEY, NOT NULL | Unique identifier for each district |
| 2 | created_at | timestamp(6) without time zone | | Timestamp when record was created |
| 3 | updated_at | timestamp(6) without time zone | | Timestamp when record was last updated |
| 4 | avg_land_price | numeric(15, 2) | | Average land price in this district |
| 5 | description | varchar(255) | | Description of the district |
| 6 | district_name | varchar(255) | | Name of the district |
| 7 | img_url | varchar(255) | | URL to district's representative image |
| 8 | is_active | boolean | | Whether this district is currently active |
| 9 | population | integer | | Population count of the district |
| 10 | total_area | numeric(15, 2) | | Total area of the district |
| 11 | city_id | uuid | FOREIGN KEY → cities, NOT NULL | City that contains this district |

---

## Table: document_types

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | document_type_id | uuid | PRIMARY KEY, NOT NULL | Unique identifier for each document type |
| 2 | created_at | timestamp(6) without time zone | | Timestamp when record was created |
| 3 | updated_at | timestamp(6) without time zone | | Timestamp when record was last updated |
| 4 | description | varchar(255) | | Description of this document type |
| 5 | is_compulsory | boolean | | Whether this document type is mandatory |
| 6 | name | varchar(100) | | Name of the document type (e.g., "Deed", "Certificate") |

---

## Table: identification_documents

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | document_id | uuid | PRIMARY KEY, NOT NULL | Unique identifier for each document |
| 2 | created_at | timestamp(6) without time zone | | Timestamp when record was created |
| 3 | updated_at | timestamp(6) without time zone | | Timestamp when record was last updated |
| 4 | document_name | varchar(255) | NOT NULL | Name/title of the document |
| 5 | document_number | varchar(20) | NOT NULL | Official document reference number |
| 6 | expiry_date | date | | When the document expires |
| 7 | file_path | varchar(500) | NOT NULL | Path to the stored document file |
| 8 | issue_date | date | | When the document was issued |
| 9 | issuing_authority | varchar(100) | | Authority that issued the document |
| 10 | rejection_reason | text | | Reason if document was rejected during verification |
| 11 | verification_status | varchar(255) | | Current verification status (pending/verified/rejected) |
| 12 | verified_at | timestamp(6) without time zone | | When the document was verified |
| 13 | document_type_id | uuid | FOREIGN KEY → document_types, NOT NULL | Type of this document |
| 14 | property_id | uuid | FOREIGN KEY → properties | Property this document belongs to |

---

## Table: media

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | media_id | uuid | PRIMARY KEY, NOT NULL | Unique identifier for each media file |
| 2 | created_at | timestamp(6) without time zone | | Timestamp when record was created |
| 3 | updated_at | timestamp(6) without time zone | | Timestamp when record was last updated |
| 4 | document_type | varchar(255) | | Type classification if media is a document |
| 5 | file_name | varchar(255) | NOT NULL | Original filename |
| 6 | file_path | varchar(500) | NOT NULL | Path to stored media file |
| 7 | media_type | varchar(255) | NOT NULL | Type of media (image/video/document) |
| 8 | mime_type | varchar(100) | NOT NULL | MIME type of the file |
| 9 | property_id | uuid | FOREIGN KEY → properties | Property this media belongs to |
| 10 | violation_id | uuid | FOREIGN KEY → violation_reports | Violation report this media is evidence for |

---

## Table: notifications

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | notification_id | uuid | PRIMARY KEY, NOT NULL | Unique identifier for each notification |
| 2 | created_at | timestamp(6) without time zone | | Timestamp when record was created |
| 3 | updated_at | timestamp(6) without time zone | | Timestamp when record was last updated |
| 4 | delivery_status | varchar(255) | | Status of notification delivery (sent/failed/pending) |
| 5 | img_url | varchar(255) | | URL to notification's associated image |
| 6 | is_read | boolean | | Whether the notification has been read |
| 7 | message | text | NOT NULL | Notification message content |
| 8 | read_at | timestamp(6) without time zone | | When the notification was read |
| 9 | related_entity_id | varchar(100) | | ID of related entity (property/contract/etc) |
| 10 | related_entity_type | varchar(255) | | Type of related entity |
| 11 | title | varchar(200) | NOT NULL | Notification title/subject |
| 12 | type | varchar(255) | | Notification type/category |
| 13 | recipient_id | uuid | FOREIGN KEY → users, NOT NULL | User who receives this notification |

---

## Table: payments

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | payment_id | uuid | PRIMARY KEY, NOT NULL | Unique identifier for each payment |
| 2 | created_at | timestamp(6) without time zone | | Timestamp when record was created |
| 3 | updated_at | timestamp(6) without time zone | | Timestamp when record was last updated |
| 4 | amount | numeric(15, 2) | NOT NULL | Payment amount |
| 5 | due_date | date | NOT NULL | When payment is due |
| 6 | installment_number | integer | | For installment payments, which installment this is |
| 7 | notes | text | | Additional payment notes |
| 8 | paid_time | timestamp(6) without time zone | | When payment was actually made |
| 9 | payment_method | varchar(255) | | Method used for payment (bank transfer/cash/etc) |
| 10 | payment_type | varchar(255) | NOT NULL | Type of payment (deposit/rent/purchase/service_fee) |
| 11 | payway_payment_id | varchar(36) | UNIQUE | Payment ID from Payway payment gateway |
| 12 | penalty_amount | numeric(15, 2) | | Penalty amount if payment is late |
| 13 | status | smallint | | Payment status (pending/completed/failed) |
| 14 | transaction_reference | varchar(100) | | External transaction reference number |
| 15 | contract_id | uuid | FOREIGN KEY → contract | Contract this payment is for |
| 16 | payer_user_id | uuid | FOREIGN KEY → users | User who made the payment |
| 17 | property_id | uuid | FOREIGN KEY → properties | Property this payment relates to |

---

## Table: properties

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | property_id | uuid | PRIMARY KEY, NOT NULL | Unique identifier for each property |
| 2 | created_at | timestamp(6) without time zone | | Timestamp when record was created |
| 3 | updated_at | timestamp(6) without time zone | | Timestamp when record was last updated |
| 4 | amenities | text | | List of amenities/features |
| 5 | approved_at | timestamp(6) without time zone | | When property listing was approved |
| 6 | area | numeric(10, 2) | NOT NULL | Property area in square meters |
| 7 | balcony_orientation | varchar(100) | | Direction the balcony faces (N/S/E/W/NE/etc) |
| 8 | bathrooms | integer | | Number of bathrooms |
| 9 | bedrooms | integer | | Number of bedrooms |
| 10 | commission_rate | numeric(5, 4) | NOT NULL | Commission rate for agents (e.g., 0.0250 = 2.5%) |
| 11 | description | text | NOT NULL | Detailed property description |
| 12 | floors | integer | | Number of floors |
| 13 | full_address | varchar(255) | | Complete address string |
| 14 | house_orientation | varchar(100) | | Direction the house faces |
| 15 | price_amount | numeric(15, 2) | NOT NULL | Listed price of the property |
| 16 | price_per_square_meter | numeric(15, 2) | | Calculated price per square meter |
| 17 | rooms | integer | | Total number of rooms |
| 18 | service_fee_amount | numeric(15, 2) | NOT NULL | Service fee charged for this listing |
| 19 | service_fee_collected_amount | numeric(15, 2) | NOT NULL | Amount of service fee collected so far |
| 20 | status | varchar(255) | | Property status (pending/approved/sold/rented/rejected) |
| 21 | title | varchar(200) | NOT NULL | Property listing title |
| 22 | transaction_type | varchar(255) | NOT NULL | Type of transaction (SALE/RENT) |
| 23 | view_count | integer | | Number of times property has been viewed |
| 24 | year_built | integer | | Year the property was built |
| 25 | assigned_agent_id | uuid | FOREIGN KEY → sale_agents | Agent assigned to manage this property |
| 26 | owner_id | uuid | FOREIGN KEY → property_owners, NOT NULL | Owner of the property |
| 27 | property_type_id | uuid | FOREIGN KEY → property_types, NOT NULL | Type of property (house/apartment/land) |
| 28 | ward_id | uuid | FOREIGN KEY → wards, NOT NULL | Ward/commune where property is located |

---

## Table: property_owners

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | owner_id | uuid | PRIMARY KEY, FOREIGN KEY → users, NOT NULL | References user_id, extends user entity |
| 2 | created_at | timestamp(6) without time zone | | Timestamp when record was created |
| 3 | updated_at | timestamp(6) without time zone | | Timestamp when record was last updated |
| 4 | approved_at | timestamp(6) without time zone | | When owner account was approved |

**Note**: This is an extension table that inherits from `users` table. All owner data is stored in users table with role=PROPERTY_OWNER.

---

## Table: property_types

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | property_type_id | uuid | PRIMARY KEY, NOT NULL | Unique identifier for each property type |
| 2 | created_at | timestamp(6) without time zone | | Timestamp when record was created |
| 3 | updated_at | timestamp(6) without time zone | | Timestamp when record was last updated |
| 4 | avatar_url | varchar(255) | | URL to representative icon/image |
| 5 | description | text | | Description of this property type |
| 6 | is_active | boolean | | Whether this type is currently active |
| 7 | type_name | varchar(50) | NOT NULL, UNIQUE | Name of property type (e.g., "House", "Apartment") |

---

## Table: purchase_contract

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | advance_payment_amount | numeric(15, 2) | | Amount paid in advance |
| 2 | commission_amount | numeric(15, 2) | NOT NULL | Commission amount for the sale |
| 3 | property_value | numeric(15, 2) | NOT NULL | Total value of the property being purchased |
| 4 | contract_id | uuid | PRIMARY KEY, FOREIGN KEY → contract, NOT NULL | References parent contract |
| 5 | deposit_contract_id | uuid | FOREIGN KEY → deposit_contract, UNIQUE | Related deposit contract if exists |

**Note**: This extends the `contract` table using joined inheritance strategy for property purchase contracts.

---

## Table: rental_contract

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | accumulated_unpaid_penalty | numeric(15, 2) | NOT NULL | Total accumulated late payment penalties |
| 2 | commission_amount | numeric(15, 2) | NOT NULL | Commission amount for the rental |
| 3 | late_payment_penalty_rate | numeric(5, 4) | NOT NULL | Penalty rate for late payments (e.g., 0.0100 = 1%) |
| 4 | month_count | integer | NOT NULL | Total number of months for the rental period |
| 5 | monthly_rent_amount | numeric(15, 2) | NOT NULL | Monthly rent amount |
| 6 | security_deposit_amount | numeric(15, 2) | | Security deposit amount |
| 7 | security_deposit_decision_at | timestamp(6) without time zone | | When decision on security deposit return was made |
| 8 | security_deposit_decision_reason | varchar(500) | | Reason for security deposit decision |
| 9 | security_deposit_status | varchar(255) | | Status of security deposit (held/returned/forfeited) |
| 10 | unpaid_months_count | integer | NOT NULL | Number of months with unpaid rent |
| 11 | contract_id | uuid | PRIMARY KEY, FOREIGN KEY → contract, NOT NULL | References parent contract |
| 12 | deposit_contract_id | uuid | FOREIGN KEY → deposit_contract, UNIQUE | Related deposit contract if exists |

**Note**: This extends the `contract` table using joined inheritance strategy for property rental contracts.

---

## Table: sale_agents

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | sale_agent_id | uuid | PRIMARY KEY, FOREIGN KEY → users, NOT NULL | References user_id, extends user entity |
| 2 | created_at | timestamp(6) without time zone | | Timestamp when record was created |
| 3 | updated_at | timestamp(6) without time zone | | Timestamp when record was last updated |
| 4 | employee_code | varchar(255) | NOT NULL, UNIQUE | Unique employee code for the agent |
| 5 | hired_date | timestamp(6) without time zone | NOT NULL | When the agent was hired |
| 6 | max_properties | integer | NOT NULL | Maximum number of properties agent can manage |

**Note**: This is an extension table that inherits from `users` table. All agent data is stored in users table with role=AGENT.

---

## Table: users

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | user_id | uuid | PRIMARY KEY, NOT NULL | Unique identifier for each user |
| 2 | created_at | timestamp(6) without time zone | | Timestamp when record was created |
| 3 | updated_at | timestamp(6) without time zone | | Timestamp when record was last updated |
| 4 | avatar_url | varchar(255) | | URL to user's avatar image |
| 5 | back_id_picture_path | varchar(255) | | Path to back side of ID card image |
| 6 | bank_account_name | varchar(150) | | Name on bank account |
| 7 | bank_account_number | varchar(30) | | Bank account number |
| 8 | bank_bin | varchar(20) | | Bank code (SWIFT/BIC), misnamed as bank_bin |
| 9 | day_of_birth | date | | User's date of birth |
| 10 | email | varchar(255) | NOT NULL, UNIQUE | User's email address |
| 11 | fcm_token | varchar(255) | | Firebase Cloud Messaging token for push notifications |
| 12 | first_name | varchar(255) | NOT NULL | User's first name |
| 13 | front_id_picture_path | varchar(255) | | Path to front side of ID card image |
| 14 | gender | varchar(255) | | User's gender |
| 15 | identification_number | varchar(255) | | ID card/passport number |
| 16 | issue_date | date | | Date ID was issued |
| 17 | issuing_authority | varchar(255) | | Authority that issued the ID |
| 18 | last_login_at | timestamp(6) without time zone | | Timestamp of last login |
| 19 | last_name | varchar(255) | NOT NULL | User's last name |
| 20 | nation | varchar(255) | | User's nationality |
| 21 | password | varchar(255) | NOT NULL | Hashed password |
| 22 | phone_number | varchar(255) | NOT NULL, UNIQUE | User's phone number |
| 23 | role | smallint | NOT NULL | User role enum (customer/agent/owner/admin) |
| 24 | status | smallint | NOT NULL | Account status (active/suspended/pending verification) |
| 25 | zalo_contact | varchar(255) | | Zalo messaging app contact |
| 26 | ward_id | uuid | FOREIGN KEY → wards, NOT NULL | Ward where user resides |

---

## Table: violation_reports

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | violation_id | uuid | PRIMARY KEY, NOT NULL | Unique identifier for each violation report |
| 2 | created_at | timestamp(6) without time zone | | Timestamp when record was created |
| 3 | updated_at | timestamp(6) without time zone | | Timestamp when record was last updated |
| 4 | description | text | NOT NULL | Detailed description of the violation |
| 5 | penalty_applied | smallint | | Type of penalty applied for this violation |
| 6 | related_entity_id | uuid | NOT NULL | ID of entity being reported (property/user/etc) |
| 7 | related_entity_type | smallint | NOT NULL | Type of entity being reported |
| 8 | resolution_notes | text | | Notes about how violation was resolved |
| 9 | resolved_at | timestamp(6) without time zone | | When the violation was resolved |
| 10 | status | smallint | | Current status (pending/under_review/resolved) |
| 11 | violation_type | smallint | NOT NULL | Type of violation (fraud/spam/inappropriate_content) |
| 12 | reporter_user | uuid | FOREIGN KEY → users, NOT NULL | User who reported the violation |

---

## Table: wards

| No | Attribute name | Data type | Constraint | Meaning/Note |
|----|----------------|-----------|------------|--------------|
| 1 | ward_id | uuid | PRIMARY KEY, NOT NULL | Unique identifier for each ward |
| 2 | created_at | timestamp(6) without time zone | | Timestamp when record was created |
| 3 | updated_at | timestamp(6) without time zone | | Timestamp when record was last updated |
| 4 | avg_land_price | numeric(15, 2) | | Average land price in this ward |
| 5 | description | varchar(255) | NOT NULL | Description of the ward |
| 6 | img_url | varchar(255) | | URL to ward's representative image |
| 7 | is_active | boolean | | Whether this ward is currently active |
| 8 | population | integer | NOT NULL | Population count of the ward |
| 9 | total_area | numeric(15, 2) | NOT NULL | Total area of the ward |
| 10 | ward_name | varchar(255) | NOT NULL | Name of the ward |
| 11 | district_id | uuid | FOREIGN KEY → districts, NOT NULL | District that contains this ward |

---

## Entity Relationships Summary

### Geographic Hierarchy
- **cities** → **districts** → **wards** (one-to-many cascading)
- **users** and **properties** reference **wards**

### User Specialization (Table-per-class inheritance)
- **users** (base table)
  - **customers** (role extension)
  - **property_owners** (role extension)
  - **sale_agents** (role extension)

### Contract Hierarchy (Joined inheritance)
- **contract** (base table)
  - **deposit_contract** (initial deposit)
  - **purchase_contract** (property purchase)
  - **rental_contract** (property rental)

### Property Management
- **properties** belongs to **property_owners**
- **properties** managed by **sale_agents**
- **properties** has many **media**, **identification_documents**, **appointments**
- **properties** categorized by **property_types**

### Transactional
- **appointments** link **customers**, **properties**, and **sale_agents**
- **contracts** formalize agreements between **customers** and **properties**
- **payments** track financial transactions for **contracts** and **properties**

### Supporting Systems
- **notifications** sent to **users**
- **violation_reports** can target any entity, reported by **users**
- **media** can belong to **properties** or **violation_reports**
- **identification_documents** verify **properties** using **document_types**
