package com.se.bds.core.transaction.internal.domain.model;

/**
 * Lifecycle status of an escrow hold.
 * Supports multi-stage payment holds for rental, deposit, and purchase contracts.
 */
public enum EscrowStatus {
    /** Escrow hold has been created but funds not yet received */
    PENDING,
    /** Funds are being held in escrow */
    HELD,
    /** Funds released to the property owner */
    RELEASED_TO_OWNER,
    /** Funds returned to the customer */
    RETURNED_TO_CUSTOMER,
    /** Funds forfeited due to contract breach */
    FORFEITED,
    /** Partial deduction applied (e.g., damage deduction from security deposit) */
    PARTIALLY_RELEASED
}
