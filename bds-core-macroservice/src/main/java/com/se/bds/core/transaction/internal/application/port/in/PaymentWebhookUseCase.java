package com.se.bds.core.transaction.internal.application.port.in;

/**
 * Use case for processing payment gateway webhook events (US-011).
 */
public interface PaymentWebhookUseCase {

    /**
     * Processes a raw webhook payload from the payment gateway.
     * Handles signature verification, event deduplication, and business side-effects.
     *
     * @param rawBody   the raw HTTP request body
     * @param signature the X-Signature header value (may be null)
     * @return true if the event was accepted and processed, false if rejected
     */
    boolean processWebhook(String rawBody, String signature);
}
