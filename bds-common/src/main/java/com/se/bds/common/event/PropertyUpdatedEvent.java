package com.se.bds.common.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PropertyUpdatedEvent(
        @NotNull UUID eventId,
        @NotNull Instant occurredAt,
        @NotNull UUID propertyId,
        @NotNull UUID ownerId,
        UUID assignedAgentId,
        @NotNull UUID wardId,
        @NotNull UUID propertyTypeId,
        String propertyTypeName,
        @NotBlank String title,
        @NotBlank String description,
        String fullAddress,
        @NotNull BigDecimal priceAmount,
        BigDecimal pricePerSquareMeter,
        @NotNull BigDecimal commissionRate,
        @NotNull BigDecimal serviceFeeAmount,
        @NotNull BigDecimal serviceFeeCollectedAmount,
        @NotNull BigDecimal area,
        Integer rooms,
        Integer bathrooms,
        Integer floors,
        Integer bedrooms,
        String houseOrientation,
        String balconyOrientation,
        Integer yearBuilt,
        String amenities,
        @NotBlank String transactionType,
        @NotBlank String status,
        String thumbnailUrl
) {
}
