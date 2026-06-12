package com.se.bds.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Fat Event - published khi property được cập nhật.
 *
 * Topic: property-updated
 * Payload giống PropertyCreatedEvent để consumer có thể dùng chung logic upsert.
 */
public record PropertyUpdatedEvent(
        UUID propertyId,
        String title,
        String description,
        String fullAddress,

        BigDecimal priceAmount,
        BigDecimal pricePerSquareMeter,
        BigDecimal commissionRate,
        BigDecimal serviceFeeAmount,
        BigDecimal area,

        Integer rooms,
        Integer bathrooms,
        Integer floors,
        Integer bedrooms,
        Integer yearBuilt,

        String transactionType,
        String status,
        String houseOrientation,
        String balconyOrientation,

        UUID ownerId,
        UUID assignedAgentId,
        UUID wardId,

        String thumbnailUrl,
        Instant occurredAt
) {
}
