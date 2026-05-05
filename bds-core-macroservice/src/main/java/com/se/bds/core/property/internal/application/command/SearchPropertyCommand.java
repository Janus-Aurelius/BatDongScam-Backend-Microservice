package com.se.bds.core.property.internal.application.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SearchPropertyCommand(
        List<UUID> cityIds,
        List<UUID> districtIds,
        List<UUID> wardIds,
        List<UUID> propertyTypeIds,
        UUID ownerId,
        UUID agentId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal minArea,
        BigDecimal maxArea,
        Integer rooms,
        Integer bathrooms,
        Integer bedrooms,
        Integer floors,
        String transactionType,
        List<String> statuses
) {
}
