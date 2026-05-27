package com.se.bds.core.property.internal.application.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdatePropertyCommand (
        UUID ownerId,
        UUID propertyTypeId,
        UUID wardId,
        String title,
        String description,
        BigDecimal priceAmount,
        BigDecimal area,
        Integer rooms,
        Integer bathrooms,
        Integer bedrooms,
        Integer floors,
        String houseOrientation,
        String balconyOrientation,
        String transactionType,
        String address,
        Double latitude,
        Double longitude,
        List<UUID> mediaIdsToRemove,
        List<UUID> documentIdsToRemove
) {
}
