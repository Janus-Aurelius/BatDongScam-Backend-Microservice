package com.se100.bds.dtos.requests.contract;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to cancel a contract")
public class CancelContractRequest {

    @NotBlank(message = "Cancellation reason is required")
    @Schema(description = "Reason for cancelling the contract")
    private String reason;

    @Schema(description = "Whether to waive the cancellation penalty (admin only)", defaultValue = "false")
    private Boolean waivePenalty;
}
