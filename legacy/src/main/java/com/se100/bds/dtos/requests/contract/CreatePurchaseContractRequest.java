package com.se100.bds.dtos.requests.contract;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a purchase contract")
public class CreatePurchaseContractRequest {

    @NotNull(message = "Property ID is required")
    @Schema(description = "The property ID for this purchase contract", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID propertyId;

    @Schema(description = "The agent ID handling this contract. Required when user is Admin, " +
            "otherwise defaults to the current sales agent user.")
    private UUID agentId;

    @NotNull(message = "Customer ID is required")
    @Schema(description = "The customer ID (buyer)", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID customerId;

    @Schema(description = "Optional deposit contract ID. If provided, propertyValue must match deposit's agreedPrice. " +
            "Deposit must be ACTIVE and not expired.")
    private UUID depositContractId;

    @NotNull(message = "Property value is required")
    @Positive(message = "Property value must be positive")
    @Schema(description = "The property purchase price. Must match deposit's agreedPrice if deposit exists.",
            example = "5000000000", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal propertyValue;

    @PositiveOrZero(message = "Advance payment amount must be zero or positive")
    @Schema(description = "Advance payment amount (paid before paperwork). Can be zero.", example = "500000000")
    private BigDecimal advancePaymentAmount;

    @NotNull(message = "Commission amount is required")
    @Positive(message = "Commission amount must be positive")
    @Schema(description = "Commission amount (must be less than property value)", example = "100000000",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal commissionAmount;

    @NotNull(message = "Start date is required")
    @Schema(description = "The date when the contract terms become effective",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate startDate;

    @Schema(description = "Additional terms for the contract (text description)")
    private String specialTerms;
}

