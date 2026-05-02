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
@Schema(description = "Request to update a draft purchase contract. All fields are optional.")
public class UpdatePurchaseContractRequest {

    @Schema(description = "The agent ID handling this contract")
    private UUID agentId;

    @Schema(description = "The customer ID (buyer)")
    private UUID customerId;

    @Positive(message = "Property value must be positive")
    @Schema(description = "The property purchase price. Must match deposit's agreedPrice if deposit exists.",
            example = "5000000000")
    private BigDecimal propertyValue;

    @PositiveOrZero(message = "Advance payment amount must be zero or positive")
    @Schema(description = "Advance payment amount (paid before paperwork). Can be zero.", example = "500000000")
    private BigDecimal advancePaymentAmount;

    @Positive(message = "Commission amount must be positive")
    @Schema(description = "Commission amount (must be less than property value)", example = "100000000")
    private BigDecimal commissionAmount;

    @Schema(description = "The date when the contract terms become effective")
    private LocalDate startDate;

    @Schema(description = "Additional terms for the contract (text description)")
    private String specialTerms;
}
