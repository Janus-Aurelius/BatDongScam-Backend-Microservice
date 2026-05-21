package com.se.bds.core.property.api.event;

import com.se.bds.core.shared.ids.PropertyId;

import java.time.Instant;

public record PropertyStatusChangedEvent(
        PropertyId propertyId,
        String oldStatus,
        String newStatus,
        Instant occurredAt
) {
}
