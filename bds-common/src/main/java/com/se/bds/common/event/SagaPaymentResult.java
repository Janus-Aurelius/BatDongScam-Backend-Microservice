package com.se.bds.common.event;

import java.util.UUID;

/**
 * Result event emitted by the financial microservice back to the Saga Orchestrator.
 */
public record SagaPaymentResult(
        UUID sagaId,
        UUID paymentId,
        String status, // "SUCCESS", "FAILED", "REFUNDED"
        String transactionReference,
        String errorMessage
) {}
