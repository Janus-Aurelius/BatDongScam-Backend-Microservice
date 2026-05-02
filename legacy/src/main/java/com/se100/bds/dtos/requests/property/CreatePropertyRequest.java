package com.se100.bds.dtos.requests.property;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.se100.bds.utils.Constants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatePropertyRequest {
    private UUID ownerId;

    @NotNull(message = "Property type id is required")
    private UUID propertyTypeId;

    @NotNull(message = "Ward id is required")
    private UUID wardId;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Transaction type is required")
    private Constants.TransactionTypeEnum transactionType;

    @Size(max = 255, message = "Full address cannot exceed 255 characters")
    private String fullAddress;

    @NotNull(message = "Area is required")
    @Positive(message = "Area must be greater than 0")
    private BigDecimal area;

    private Integer rooms;

    private Integer bathrooms;

    private Integer floors;

    private Integer bedrooms;

    private Constants.OrientationEnum houseOrientation;

    private Constants.OrientationEnum balconyOrientation;

    private Integer yearBuilt;

    @NotNull(message = "Price amount is required")
    @Positive(message = "Price amount must be greater than 0")
    private BigDecimal priceAmount;

    private String amenities;

    @Size(max = 5000, message = "Internal extra notes cannot exceed 5000 characters")
    private String internalExtraNotes;

    @Valid
    private List<DocumentUploadInfo> documentMetadata;

    @Valid
    private List<MediaUploadInfo> mediaMetadata;
}
