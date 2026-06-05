package com.se.bds.core.transaction.internal.application.port.in;

import com.se.bds.common.event.PaymentCompletedEvent;

/**
 * Use case for processing payment completed events.
 */
public interface PaymentWebhookUseCase {

    /**
     * Processes a payment completed event consumed from Kafka.
     * Updates payment and contract statuses, and publishes internal events.
     *
     * @param event the standardized payment completed event
     * @return true if successfully processed
     */
    boolean processPaymentCompleted(PaymentCompletedEvent event);
}
