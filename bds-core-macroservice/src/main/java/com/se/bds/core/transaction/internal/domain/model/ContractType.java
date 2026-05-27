package com.se.bds.core.transaction.internal.domain.model;

/**
 * Discriminator for the Contract type hierarchy.
 * Migrated from legacy Constants.ContractTypeEnum.
 */
public enum ContractType {
    DEPOSIT,
    PURCHASE,
    RENTAL
}
