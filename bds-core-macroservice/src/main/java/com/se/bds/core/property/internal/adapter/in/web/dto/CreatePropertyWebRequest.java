package com.se.bds.core.property.internal.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Request to create a new property listing")
public record CreatePropertyWebRequest(
        @NotNull @Schema(description = "Owner's user ID", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479") UUID ownerId,
        @NotNull @Schema(description = "Property type ID", example = "b1a2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6") UUID propertyTypeId,
        @NotNull @Schema(description = "Ward ID for property location", example = "550e8400-e29b-41d4-a716-446655440000") UUID wardId,
        @NotBlank @Schema(description = "Property title", example = "Luxury Villa with Sea View") String title,
        @Schema(description = "Detailed description", example = "A stunning 5-bedroom villa with a private pool and beachfront access.") String description,
        @NotNull @Positive @Schema(description = "Listing price", example = "500000") BigDecimal priceAmount,
        @NotNull @Positive @Schema(description = "Total area in sqm", example = "350") BigDecimal area,
        @Schema(description = "Number of rooms", example = "8") Integer rooms,
        @Schema(description = "Number of bathrooms", example = "4") Integer bathrooms,
        @Schema(description = "Number of bedrooms", example = "5") Integer bedrooms,
        @Schema(description = "Number of floors", example = "2") Integer floors,
        @Schema(description = "House orientation", example = "SOUTH") String houseOrientation,
        @Schema(description = "Balcony orientation", example = "EAST") String balconyOrientation,
        @NotBlank @Schema(description = "Transaction type (SALE or RENT)", example = "SALE") String transactionType,
        @NotBlank @Schema(description = "Street address", example = "123 Beach Road") String address,
        @Schema(description = "Latitude coordinates", example = "10.762622") Double latitude,
        @Schema(description = "Longitude coordinates", example = "106.660172") Double longitude,
        @Schema(description = "Metadata for accompanying documents") java.util.List<DocumentMetadataWebRequest> documentsMetadata
        ) {
}
