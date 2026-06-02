package com.se361.financial_service.dtos.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateCommissionRequest {

    @NotNull(message = "Contract ID is required")
    private UUID contractId;

    @NotNull(message = "Property ID is required")
    private UUID propertyId;

    @NotNull(message = "Agent ID is required")
    private UUID agentId;

    private UUID paymentId;
    private String agentName;
    private String propertyTitle;

    @NotNull(message = "Commission amount is required")
    @Positive(message = "Commission amount must be positive")
    private BigDecimal commissionAmount;

    @NotNull(message = "Transaction amount is required")
    @Positive(message = "Transaction amount must be positive")
    private BigDecimal transactionAmount;

    @NotNull(message = "Commission date is required")
    private LocalDate commissionDate;

    private String notes;

}
