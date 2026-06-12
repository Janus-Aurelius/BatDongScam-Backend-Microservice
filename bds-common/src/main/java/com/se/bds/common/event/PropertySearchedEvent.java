package com.se.bds.common.event;

import java.time.Instant;
import java.util.UUID;

public record PropertySearchedEvent(
        UUID propertyId,
        String searchQuery,
        UUID userId,
        UUID cityId,
        UUID districtId,
        UUID wardId,
        UUID propertyTypeId,
        Instant timestamp
) {}
