package com.se.bds.core.transaction.internal.domain.model;

/**
 * Tracks the lifecycle of a rental security deposit.
 * Migrated from legacy Constants.SecurityDepositStatusEnum.
 */
public enum SecurityDepositStatus {
    NOT_PAID,
    HELD,
    RETURNED_TO_CUSTOMER,
    TRANSFERRED_TO_OWNER
}
