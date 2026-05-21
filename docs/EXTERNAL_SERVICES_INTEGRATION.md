# External Services Integration Guide: BatDongSan Core Macroservice

This guide provides instructions on how to obtain the necessary credentials and configure the environment variables for external services used by the `bds-core-macroservice`.

## 1. Payway (Payment Gateway)

Used for US-010 (Initialize Payment), US-011 (Webhooks), and US-028 (Escrow Payouts).

### Credentials Required:
*   **PAYWAY_SERVICE_URL**: The API endpoint provided by Payway.
    *   Sandbox: `https://api-sandbox.payway.com.vn`
    *   Production: `https://api.payway.com.vn`
*   **PAYWAY_API_KEY**: Your merchant API key.
    *   **Where to get:** Payway Merchant Portal -> Settings -> API Keys.
*   **PAYWAY_MERCHANT_ID**: Your unique merchant identifier.
    *   **Where to get:** Payway Merchant Portal -> Dashboard or Account Info.
*   **PAYWAY_VERIFY_KEY**: The secret key used to verify webhook signatures (HMAC-SHA256).
    *   **Where to get:** Payway Merchant Portal -> Settings -> Webhooks -> Secret/Verify Key.
*   **PAYWAY_WEBHOOK_BASE_URL**: The public URL where your macroservice is reachable (e.g., `https://api.yourdomain.com`).
*   **PAYWAY_RETURN_URL**: The URL users are redirected to after finishing a payment on the Payway checkout page.

---

## 2. Cloudinary (File Storage)

Used for US-008 (Storing generated PDF contracts).

### Credentials Required:
*   **CLOUDINARY_CLOUD_NAME**: Your Cloudinary cloud name.
*   **CLOUDINARY_API_KEY**: Your API Key.
*   **CLOUDINARY_API_SECRET**: Your API Secret.
*   **Where to get:** [Cloudinary Dashboard](https://cloudinary.com/console).

---

## 3. Data Encryption (Internal)

Used for US-028 (Encrypting bank account details at rest).

### Variable Required:
*   **ENCRYPTION_KEY**: A random 32-character string used as the AES-256 secret key.
    *   **Generation:** You can generate one using `openssl rand -base64 32` or any password generator.

---

## 4. Setup Instructions

1.  **Copy Environment File:**
    ```bash
    cp bds-core-macroservice/.env.example bds-core-macroservice/.env
    ```
2.  **Fill in the Variables:** Open `.env` and replace the placeholders with your actual credentials.
3.  **Restart Application:** The application loads these variables on startup via `CoreMacroserviceApplication.loadDotEnv()`.

## 5. Implementation Notes

*   **Idempotency:** For webhooks, the system uses `processed_webhook_event` table to prevent duplicate processing.
*   **Security:** Bank accounts in the `escrow_hold` table are encrypted using `AesAttributeConverter`.
*   **Failure Handling:** Outbound calls to Cloudinary and Payway are wrapped in `@Retryable` with exponential backoff.
