# System Messages Registry

This document lists all standardized message codes used across the Use Case specifications. These should be implemented in a centralized message bundle (e.g., `messages.properties`) to ensure consistency across the UI and API.

## 1. Confirmation Messages (MSG 1 - MSG 9)
| Code | Type | Default Text (English) | Description |
| :--- | :--- | :--- | :--- |
| **MSG 1** | Confirmation | Are you sure you want to proceed with this action? | Prompted before sensitive state changes (Save, Delete, Approve). |

## 2. Validation & Input Errors (MSG 10 - MSG 19)
| Code | Type | Default Text (English) | Description |
| :--- | :--- | :--- | :--- |
| **MSG 2** | Error | One or more required fields are empty. Please check your input. | Triggered when mandatory fields are null or blank. |
| **MSG 11** | Error | The uploaded file exceeds the maximum allowed size. | Triggered when an image or document is too large (e.g., > 5MB). |
| **MSG 12** | Error | This action is not allowed for the current item status or your user role. | Triggered by invalid state transitions or permission violations. |
| **MSG 13** | Error | Invalid range or value detected. Please ensure dates and amounts are correct. | min > max, price < 0, or date in the past. |
| **MSG 14** | Error | The description provided is too short. Please provide more detail. | Triggered by minimum length constraints. |
| **MSG 18** | Error | The requested item could not be found. | Generic 404 error for properties, users, or reports. |

## 3. Authentication & Account Errors (MSG 20 - MSG 29)
| Code | Type | Default Text (English) | Description |
| :--- | :--- | :--- | :--- |
| **MSG 22** | Error | Invalid credentials or session expired. Please sign in again. | Bad password, user not found, or token refresh failure. |
| **MSG 23** | Success | Sign in successful. Welcome back! | Success feedback after JWT generation. |
| **MSG 24** | Error | Password must be at least 8 characters long. | Minimum length validation for signup/update. |
| **MSG 25** | Error | Password does not meet complexity requirements. | Regex mismatch (must contain special chars, numbers). |
| **MSG 26** | Error | This phone number is already registered. | Uniqueness constraint violation for Phone. |
| **MSG 27** | Error | This email address is already registered. | Uniqueness constraint violation for Email. |
| **MSG 28** | Success | Your account has been created successfully. | Success feedback after registration. |
| **MSG 29** | Success | Verification email sent. Please check your inbox. | Feedback for email verification workflow. |

## 4. Discovery & Interaction Messages (MSG 30 - MSG 39)
| Code | Type | Default Text (English) | Description |
| :--- | :--- | :--- | :--- |
| **MSG 30** | Error | Invalid phone number format. | Regex mismatch for phone format. |
| **MSG 31** | Error | Invalid email address format. | Regex mismatch for email format. |
| **MSG 32** | Success | Process completed successfully. | Generic success for retrieval/search/analytics. |
| **MSG 33** | Error | A pending or confirmed appointment already exists for this property. | Business conflict for duplicate bookings. |
| **MSG 34** | Success | Payment initiated. You are being redirected to the secure gateway. | Transition feedback for payment initiation. |
| **MSG 35** | Success | Item removed from your wishlist. | Feedback for un-favoriting a property. |
| **MSG 36** | Success | Item added to your wishlist. | Feedback for favoriting a property. |
| **MSG 37** | Success | AR session initialized. Please scan your surroundings. | Feedback when AR camera starts. |
| **MSG 38** | Info | You have earned reward points from a referral! | Notification for referral success. |
| **MSG 39** | Error | You cannot review this agent until the interaction is completed. | Business rule for verified reviews. |

## 5. Innovation & Admin Messages (MSG 40 - MSG 49)
| Code | Type | Default Text (English) | Description |
| :--- | :--- | :--- | :--- |
| **MSG 40** | Info | No data available for the selected criteria. | Feedback for empty dashboard charts or search results. |
| **MSG 41** | Success | Document scanned successfully. Fields have been auto-filled. | Feedback for OCR extraction. |
| **MSG 42** | Info | Some fields could not be clearly read. Please verify them manually. | Low confidence warning for OCR. |
| **MSG 43** | Error | Downgrade failed: You have more active listings than allowed by the new plan. | Subscription tier constraint violation. |

## 6. General System Success (Universal)
| Code | Type | Default Text (English) | Description |
| :--- | :--- | :--- | :--- |
| **MSG 3** | Success | Changes saved successfully. | The most common generic success message. |
