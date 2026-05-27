package com.se.bds.core.transaction.internal.domain.model;

/**
 * Lifecycle status of a contract.
 * Migrated from legacy Constants.ContractStatusEnum.
 */
public enum ContractStatus {
    /** Draft state */
    DRAFT,
    /** Waiting for remaining payments to be made */
    PENDING_PAYMENT,
    /** All remaining balance paid, waiting for official documentation */
    WAITING_OFFICIAL,
    /** In effect with legal bindings (signed contract document exists) */
    ACTIVE,
    /** Completed contract (e.g., rental term ended) */
    COMPLETED,
    /** Contract terminated before end date */
    CANCELLED
}
