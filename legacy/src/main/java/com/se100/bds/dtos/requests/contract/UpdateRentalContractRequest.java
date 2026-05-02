package com.se100.bds.dtos.requests.contract;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request to update a draft rental contract. All fields are optional.")
public class UpdateRentalContractRequest {

    @Schema(description = "The agent ID handling this contract")
    private UUID agentId;

    @Schema(description = "The customer ID (tenant)")
    private UUID customerId;

    @Positive(message = "Month count must be positive")
    @Schema(description = "Number of months for the rental period", example = "12")
    private Integer monthCount;

    @Positive(message = "Monthly rent amount must be positive")
    @Schema(description = "Monthly rent amount. Must match deposit's agreedPrice if deposit exists.",
            example = "15000000")
    private BigDecimal monthlyRentAmount;

    @Positive(message = "Commission amount must be positive")
    @Schema(description = "Commission amount per month (must be less than monthly rent)", example = "1500000")
    private BigDecimal commissionAmount;

    @PositiveOrZero(message = "Security deposit amount must be zero or positive")
    @Schema(description = "Security deposit amount (optional, can be zero)", example = "30000000")
    private BigDecimal securityDepositAmount;

    @PositiveOrZero(message = "Late payment penalty rate must be zero or positive")
    @Schema(description = "Late payment penalty rate as decimal (e.g., 0.05 for 5% per month)", example = "0.05")
    private BigDecimal latePaymentPenaltyRate;

    @Schema(description = "The date when the rental period starts")
    private LocalDate startDate;

    @Schema(description = "Additional terms for the contract (text description)")
    private String specialTerms;
}
