package com.se100.bds.dtos.requests.contract;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for admin to decide what happens to the security deposit")
public class SecurityDepositDecisionRequest {

    @NotNull(message = "Decision is required")
    @Schema(description = "Decision on security deposit: RETURN_TO_CUSTOMER or TRANSFER_TO_OWNER",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private SecurityDepositDecision decision;

    @Schema(description = "Optional reason for the decision")
    private String reason;

    public enum SecurityDepositDecision {
        RETURN_TO_CUSTOMER,
        TRANSFER_TO_OWNER
    }
}
