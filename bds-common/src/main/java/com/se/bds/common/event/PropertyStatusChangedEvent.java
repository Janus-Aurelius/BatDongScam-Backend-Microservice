package com.se.bds.common.event;

import java.util.UUID;

public record PropertyStatusChangedEvent(
        UUID propertyId,
        String oldStatus,
        String newStatus
) {}