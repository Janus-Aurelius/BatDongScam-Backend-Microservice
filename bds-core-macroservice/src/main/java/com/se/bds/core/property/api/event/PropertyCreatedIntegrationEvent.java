package com.se.bds.core.property.api.event;

import com.se.bds.core.property.internal.domain.model.Orientation;
import com.se.bds.core.property.internal.domain.model.PropertyStatus;
import com.se.bds.core.property.internal.domain.model.TransactionType;
import com.se.bds.core.shared.ids.PropertyId;

import java.math.BigDecimal;
import java.util.UUID;

public record PropertyCreatedIntegrationEvent(
        PropertyId propertyId,
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

        TransactionType transactionType,
        PropertyStatus status,
        Orientation houseOrientation,
        Orientation balconyOrientation,

        UUID ownerId,
        UUID assignedAgentId,
        UUID wardId,

        String thumbnailUrl
) {}
