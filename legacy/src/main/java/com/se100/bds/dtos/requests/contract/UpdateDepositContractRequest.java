package com.se100.bds.dtos.requests.contract;

import com.se100.bds.utils.Constants.MainContractTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request to update a draft deposit contract. All fields are optional - only provided fields will be updated.")
public class UpdateDepositContractRequest {

    @Schema(description = "The agent ID handling this contract")
    private UUID agentId;

    @Schema(description = "The customer ID (A side of contract)")
    private UUID customerId;

    @Schema(description = "The type of main contract this deposit is for (RENTAL or PURCHASE)")
    private MainContractTypeEnum mainContractType;

    @Positive(message = "Deposit amount must be positive")
    @Schema(description = "The deposit amount paid by customer", example = "50000000")
    private BigDecimal depositAmount;

    @Positive(message = "Agreed price must be positive")
    @Schema(description = "The agreed price for the main contract (monthly rent for RENTAL, property value for PURCHASE)",
            example = "500000000")
    private BigDecimal agreedPrice;

    @Schema(description = "The date when the deposit contract terms become effective")
    private LocalDate startDate;

    @Schema(description = "The date when the deposit contract expires")
    private LocalDate endDate;

    @Schema(description = "Additional terms for the contract (text description)")
    private String specialTerms;

    @Positive(message = "Cancellation penalty must be positive if provided")
    @Schema(description = "Custom cancellation penalty amount. If not provided, defaults to deposit amount. " +
            "Note: This only applies to owner cancellation; customer always loses full deposit.")
    private BigDecimal cancellationPenalty;
}
