package com.se100.bds.dtos.requests.contract;

import com.se100.bds.utils.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an existing contract")
public class UpdateContractRequest {

    @Schema(description = "Contract end date")
    private LocalDate endDate;

    @Schema(description = "Special terms for the contract")
    private String specialTerms;

    @Schema(description = "Contract status (only certain transitions are allowed)")
    private Constants.ContractStatusEnum status;

    @DecimalMin(value = "0.0", message = "Penalty rate must be non-negative")
    @DecimalMax(value = "1.0", message = "Penalty rate must be at most 100%")
    @Schema(description = "Late payment penalty rate (decimal, e.g., 0.05 for 5%)")
    private BigDecimal latePaymentPenaltyRate;

    @Schema(description = "Special conditions for the contract")
    private String specialConditions;
}
