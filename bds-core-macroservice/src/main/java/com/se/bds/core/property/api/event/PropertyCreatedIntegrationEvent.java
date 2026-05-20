package com.se.bds.core.property.api.event;

import java.util.UUID;

public record PropertyCreatedIntegrationEvent(
        UUID propertyId,
        String title,
        UUID ownerId,
        String transactionType
) {}
