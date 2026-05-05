package com.se.bds.core.property.api.event;

import com.se.bds.core.shared.ids.PropertyId;

import java.time.Instant;
import java.util.UUID;

public record PropertyAgentAssignedEvent(
        PropertyId propertyId,
        UUID newAgentId,
        UUID previousAgentId,
        Instant occurredAt
) {
}
