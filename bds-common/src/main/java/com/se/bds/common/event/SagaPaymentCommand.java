package com.se.bds.common.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command event emitted by the Saga Orchestrator to trigger actions in the financial microservice.
 */
public record SagaPaymentCommand(
        UUID sagaId,
        UUID paymentId,
        UUID contractId,
        BigDecimal amount,
        String commandType // "PROCESS_PAYMENT" or "REFUND_PAYMENT"
) {}
