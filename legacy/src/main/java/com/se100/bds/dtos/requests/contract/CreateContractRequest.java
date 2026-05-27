package com.se100.bds.dtos.requests.contract;

import com.se100.bds.utils.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
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
@Schema(description = "Request to create a new contract")
public class CreateContractRequest {

    @NotNull(message = "Property ID is required")
    @Schema(description = "The property ID for this contract")
    private UUID propertyId;

    @NotNull(message = "Agent ID is required")
    @Schema(description = "The agent ID handling this contract. Only takes effect when user is NOT a Sale Agent, " +
            "otherwise, the agent ID will be set to the current user's ID.")
    private UUID agentId;

    @NotNull(message = "Contract type is required")
    @Schema(description = "Type of contract: PURCHASE, RENTAL", example = "PURCHASE")
    private Constants.ContractTypeEnum contractType;

    @NotNull(message = "Start date is required")
    @Schema(description = "Contract start date", example = "2025-01-01")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Schema(description = "Contract end date", example = "2026-01-01")
    private LocalDate endDate;

    @Schema(description = "Special terms for the contract")
    private String specialTerms;

    @NotNull(message = "Total contract amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total amount must be positive")
    @Schema(description = "Total contract amount", example = "1000000000")
    private BigDecimal totalContractAmount;

    @NotNull(message = "Deposit amount is required")
    @DecimalMin(value = "0.0", message = "Deposit amount must be non-negative")
    @Schema(description = "Deposit amount", example = "100000000")
    private BigDecimal depositAmount;

    @Schema(description = "Advance payment amount (for mortgages)", example = "200000000")
    private BigDecimal advancePaymentAmount;

    @Schema(description = "Number of installments (for mortgages)", example = "24")
    private Integer installmentAmount;

    @Schema(description = "Progress milestone for payments", example = "0.5")
    private BigDecimal progressMilestone;

    @NotNull(message = "Late payment penalty rate is required")
    @DecimalMin(value = "0.0", message = "Penalty rate must be non-negative")
    @DecimalMax(value = "1.0", message = "Penalty rate must be at most 100%")
    @Schema(description = "Late payment penalty rate (decimal, e.g., 0.05 for 5%)", example = "0.05")
    private BigDecimal latePaymentPenaltyRate;

    @Schema(description = "Special conditions for the contract")
    private String specialConditions;
}
