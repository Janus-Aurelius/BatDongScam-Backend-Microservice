package com.se100.bds.services.domains.payment.webhook;

import com.se100.bds.models.entities.contract.Payment;

/**
 * Handles domain side-effects for a payment that has succeeded.
 * Implementations should be idempotent (safe to run multiple times).
 */
public interface PaymentSucceededSideEffectHandler {

    boolean supports(Payment payment);

    void handle(Payment payment, PaymentGatewayWebhookEvent event);
}
