package com.se.bds.core.transaction.internal.domain.model;

/**
 * Represents the execution state of the Contract Payment Saga.
 */
public enum SagaStatus {
    /** Saga has been initialized. */
    STARTED,
    /** Process payment request has been sent to Kafka. */
    PAYMENT_PENDING,
    /** Payment was successfully charged. */
    PAYMENT_SUCCESS,
    /** Contract has been activated successfully. Saga complete. */
    CONTRACT_ACTIVE,
    /** Contract activation failed due to business rules constraint. */
    CONTRACT_FAILED,
    /** Compensating refund request has been sent to Kafka. */
    REFUND_PENDING,
    /** Refund completed successfully. Saga rolled back. */
    COMPENSATED,
    /** Payment failed. Saga terminated. */
    FAILED
}
