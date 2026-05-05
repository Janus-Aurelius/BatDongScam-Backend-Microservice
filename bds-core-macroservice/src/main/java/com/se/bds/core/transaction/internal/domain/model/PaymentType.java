package com.se.bds.core.transaction.internal.domain.model;

/**
 * Classification of a payment within the contract lifecycle.
 * Migrated from legacy Constants.PaymentTypeEnum.
 */
public enum PaymentType {
    DEPOSIT,
    ADVANCE,
    INSTALLMENT,
    FULL_PAY,
    MONTHLY,
    PENALTY,
    MONEY_SALE,
    MONEY_RENTAL,
    SALARY,
    BONUS,
    SERVICE_FEE,
    PAYMENT_OVERDUE,
    SECURITY_DEPOSIT
}
