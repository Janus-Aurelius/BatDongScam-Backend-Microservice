package com.se.bds.core.transaction.internal.domain.model;

/**
 * Status of an individual payment transaction.
 * Migrated from legacy Constants.PaymentStatusEnum.
 */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    SYSTEM_PENDING,
    SYSTEM_SUCCESS,
    SYSTEM_FAILED
}
