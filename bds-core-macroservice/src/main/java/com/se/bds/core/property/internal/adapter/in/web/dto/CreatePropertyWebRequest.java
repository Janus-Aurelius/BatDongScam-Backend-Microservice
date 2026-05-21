package com.se.bds.core.property.internal.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePropertyWebRequest(
        @NotNull UUID ownerId,
        @NotNull UUID propertyTypeId,
        @NotNull UUID wardId,
        @NotBlank String title,
        String description,
        @NotNull @Positive BigDecimal priceAmount,
        @NotNull @Positive BigDecimal area,
        Integer rooms,
        Integer bathrooms,
        Integer bedrooms,
        Integer floors,
        String houseOrientation,
        String balconyOrientation,
        @NotBlank String transactionType,
        @NotBlank String address,
        Double latitude,
        Double longitude
        ) {
}
