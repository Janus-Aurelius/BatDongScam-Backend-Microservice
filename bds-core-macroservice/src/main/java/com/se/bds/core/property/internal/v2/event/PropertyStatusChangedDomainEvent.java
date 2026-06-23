package com.se.bds.core.property.internal.v2.event;

import com.se.bds.core.property.internal.domain.model.PropertyStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Event representing a status change in a property listing.
 */
public record PropertyStatusChangedDomainEvent(
    UUID propertyId,
    PropertyStatus oldStatus,
    PropertyStatus newStatus,
    LocalDateTime occurredAt
) {}
