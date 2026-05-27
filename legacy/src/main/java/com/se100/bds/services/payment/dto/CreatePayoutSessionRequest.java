package com.se100.bds.services.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a payout session at the payment gateway")
public class CreatePayoutSessionRequest {

    @Schema(description = "Amount in minor units (e.g. cents)", example = "1000", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Schema(description = "Currency code", example = "USD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currency;

    @Schema(description = "Destination bank account number", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountNumber;

    @Schema(description = "Destination account holder name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountHolderName;

    @Schema(description = "Destination SWIFT code", requiredMode = Schema.RequiredMode.REQUIRED)
    private String swiftCode;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Arbitrary metadata")
    private Map<String, Object> metadata;

    @Schema(description = "If provided, gateway POSTs signed events here on status changes")
    private String webhookUrl;
}
