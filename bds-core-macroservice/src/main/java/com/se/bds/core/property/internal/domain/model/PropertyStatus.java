package com.se.bds.core.property.internal.domain.model;

/**
 * Lifecycle status of a property listing.
 * Migrated from legacy Constants.PropertyStatusEnum.
 */
public enum PropertyStatus {
    PENDING,
    REJECTED,
    APPROVED,
    SOLD,
    RENTED,
    AVAILABLE,
    UNAVAILABLE,
    REMOVED,      // Due to violation report
    DELETED
}
