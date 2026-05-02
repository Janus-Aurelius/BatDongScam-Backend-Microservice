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
@Schema(description = "Request to create a rental contract")
public class CreateRentalContractRequest {

    @NotNull(message = "Property ID is required")
    @Schema(description = "The property ID for this rental contract", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID propertyId;

    @Schema(description = "The agent ID handling this contract. Required when user is Admin, " +
            "otherwise defaults to the current sales agent user.")
    private UUID agentId;

    @NotNull(message = "Customer ID is required")
    @Schema(description = "The customer ID (tenant)", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID customerId;

    @Schema(description = "Optional deposit contract ID. If provided, monthlyRentAmount must match deposit's agreedPrice. " +
            "Deposit must be ACTIVE and not expired.")
    private UUID depositContractId;

    @NotNull(message = "Month count is required")
    @Positive(message = "Month count must be positive")
    @Schema(description = "Number of months for the rental period", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer monthCount;

    @NotNull(message = "Monthly rent amount is required")
    @Positive(message = "Monthly rent amount must be positive")
    @Schema(description = "Monthly rent amount. Must match deposit's agreedPrice if deposit exists.",
            example = "15000000", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal monthlyRentAmount;

    @NotNull(message = "Commission amount is required")
    @Positive(message = "Commission amount must be positive")
    @Schema(description = "Commission amount per month (must be less than monthly rent)", example = "1500000",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal commissionAmount;

    @PositiveOrZero(message = "Security deposit amount must be zero or positive")
    @Schema(description = "Security deposit amount (optional, can be zero). Held in system until admin decides.",
            example = "30000000")
    private BigDecimal securityDepositAmount;

    @NotNull(message = "Late payment penalty rate is required")
    @PositiveOrZero(message = "Late payment penalty rate must be zero or positive")
    @Schema(description = "Late payment penalty rate as decimal (e.g., 0.05 for 5% per month)",
            example = "0.05", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal latePaymentPenaltyRate;

    @NotNull(message = "Start date is required")
    @Schema(description = "The date when the rental period starts", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate startDate;

    @Schema(description = "Additional terms for the contract (text description)")
    private String specialTerms;
}

