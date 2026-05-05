package com.se.bds.core.shared.enums;

/**
 * Domain-agnostic role reference data.
 * Lives in the shared kernel so any module can track "who did what"
 * without importing User module internals.
 *
 * <p>Migrated from legacy {@code Constants.RoleEnum}.
 */
public enum Role {
    ADMIN,
    SALESAGENT,
    PROPERTY_OWNER,
    CUSTOMER
}
