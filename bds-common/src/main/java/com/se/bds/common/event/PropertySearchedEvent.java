package com.se.bds.common.event;

import java.time.Instant;
import java.util.UUID;

public record PropertySearchedEvent(
        UUID propertyId,
        String searchQuery,
        UUID userId,
        Instant timestamp
) {}
