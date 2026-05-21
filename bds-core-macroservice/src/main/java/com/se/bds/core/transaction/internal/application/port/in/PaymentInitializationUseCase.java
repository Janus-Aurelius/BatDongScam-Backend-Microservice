package com.se.bds.core.transaction.internal.application.port.in;

import java.util.UUID;

/**
 * Use case for initializing payments and redirecting to payment gateway (US-010).
 */
public interface PaymentInitializationUseCase {

    /**
     * Result of initializing a payment session.
     */
    record PaymentInitResult(
            UUID paymentId,
            String gatewayPaymentId,
            String checkoutUrl,
            String status
    ) {}

    /**
     * Initializes a payment for a contract's pending payment.
     * Creates a payment session with the gateway and returns the checkout URL as JSON.
     *
     * @param contractId the contract to pay for
     * @param paymentId  the specific payment record to initialize (e.g., first month rent, security deposit)
     * @return the payment initialization result with checkout URL
     */
    PaymentInitResult initializePayment(UUID contractId, UUID paymentId);
}
