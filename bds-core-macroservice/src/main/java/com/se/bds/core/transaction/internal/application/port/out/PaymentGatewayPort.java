package com.se.bds.core.transaction.internal.application.port.out;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Outbound port for interacting with payment gateway providers (US-010, US-011, US-028).
 * Abstracts the Payway provider to allow swapping implementations without modifying business logic.
 */
public interface PaymentGatewayPort {

    /**
     * Creates a payment session with the gateway and returns the checkout URL.
     *
     * @param amount         the payment amount
     * @param currency       the currency code (e.g., "VND")
     * @param description    human-readable description
     * @param metadata       key-value metadata to attach to the payment
     * @param idempotencyKey optional idempotency key to prevent duplicate charges
     * @return the payment session result containing checkoutUrl and gateway payment ID
     */
    PaymentSessionResult createPaymentSession(
            BigDecimal amount,
            String currency,
            String description,
            Map<String, Object> metadata,
            String idempotencyKey
    );

    /**
     * Retrieves the current status of an existing payment session.
     *
     * @param gatewayPaymentId the payment ID from the gateway
     * @return the payment session result
     */
    PaymentSessionResult getPaymentSession(String gatewayPaymentId);

    /**
     * Creates a payout session to send funds to a bank account.
     *
     * @param amount            the payout amount
     * @param currency          the currency code
     * @param accountNumber     the bank account number
     * @param accountHolderName the account holder name
     * @param swiftCode         the bank SWIFT/BIN code
     * @param description       human-readable description
     * @param metadata          key-value metadata
     * @param idempotencyKey    optional idempotency key
     * @return the payout result
     */
    PayoutResult createPayoutSession(
            BigDecimal amount,
            String currency,
            String accountNumber,
            String accountHolderName,
            String swiftCode,
            String description,
            Map<String, Object> metadata,
            String idempotencyKey
    );

    /**
     * Result of creating a payment session.
     */
    record PaymentSessionResult(
            String gatewayPaymentId,
            String checkoutUrl,
            String status,
            BigDecimal amount,
            String currency
    ) {}

    /**
     * Result of creating a payout session.
     */
    record PayoutResult(
            String gatewayPayoutId,
            String status,
            BigDecimal amount,
            String currency
    ) {}
}
