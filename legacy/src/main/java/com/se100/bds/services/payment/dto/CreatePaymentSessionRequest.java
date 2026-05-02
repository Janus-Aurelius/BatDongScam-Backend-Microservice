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
@Schema(description = "Request to create a payment session at the payment gateway")
public class CreatePaymentSessionRequest {

    @Schema(description = "Amount in minor units (e.g. cents)", example = "1000", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Schema(description = "Currency code", example = "USD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currency;

    @Schema(description = "Description shown to the payer")
    private String description;

    @Schema(description = "Arbitrary metadata")
    private Map<String, Object> metadata;

    @Schema(description = "Where the checkout redirects after completion")
    private String returnUrl;

    @Schema(description = "If provided, gateway POSTs signed events here on status changes")
    private String webhookUrl;
}
