package com.se100.bds.dtos.requests.contract;

import com.se100.bds.utils.Constants.MainContractTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
@Schema(description = "Request to create a deposit contract")
public class CreateDepositContractRequest {

    @NotNull(message = "Property ID is required")
    @Schema(description = "The property ID for this deposit contract", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID propertyId;

    @Schema(description = "The agent ID handling this contract. Required when user is Admin, " +
            "otherwise defaults to the current sales agent user.")
    private UUID agentId;

    @NotNull(message = "Customer ID is required")
    @Schema(description = "The customer ID (A side of contract)", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID customerId;

    @NotNull(message = "Main contract type is required")
    @Schema(description = "The type of main contract this deposit is for (RENTAL or PURCHASE)",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private MainContractTypeEnum mainContractType;

    @NotNull(message = "Deposit amount is required")
    @Positive(message = "Deposit amount must be positive")
    @Schema(description = "The deposit amount paid by customer", example = "50000000",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal depositAmount;

    @NotNull(message = "Agreed price is required")
    @Positive(message = "Agreed price must be positive")
    @Schema(description = "The agreed price for the main contract (monthly rent for RENTAL, property value for PURCHASE)",
            example = "500000000", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal agreedPrice;

    @Schema(description = "The date when the deposit contract expires (optional)", example = "2026-2-26")
    private LocalDate endDate;

    @Schema(description = "Additional terms for the contract (text description)")
    private String specialTerms;

    @Positive(message = "Cancellation penalty must be positive if provided")
    @Schema(description = "Custom cancellation penalty amount. If not provided, defaults to deposit amount. " +
            "Note: This only applies to owner cancellation; customer always loses full deposit.")
    private BigDecimal cancellationPenalty;
}
