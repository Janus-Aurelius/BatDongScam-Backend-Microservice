package com.se100.bds.services.domains.payment.webhook;

/**
 * Synchronous processor for payment gateway webhook events.
 *
 * Contract:
 * - Must be safe to call multiple times (idempotent).
 * - Should not throw to the webhook caller for business errors (best-effort + logging).
 */
public interface PaymentGatewayWebhookProcessor {

    void process(PaymentGatewayWebhookEvent event);
}

