package com.se100.bds.services.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response returned by the payment gateway when creating a payment session")
public class CreatePaymentSessionResponse {

    @Schema(description = "Gateway-side payment id (UUID)")
    private String id;

    @Schema(description = "Amount in minor units")
    private BigDecimal amount;

    private String currency;

    @Schema(description = "Payment status, e.g. pending/succeeded/failed")
    private String status;

    private String description;

    private Map<String, Object> metadata;

    @Schema(name = "return_url")
    private String returnUrl;

    @Schema(name = "webhook_url")
    private String webhookUrl;

    @Schema(name = "checkout_url", description = "Hosted checkout URL (if supported by gateway)")
    private String checkoutUrl;

    @Schema(name = "created_at")
    private OffsetDateTime createdAt;

    @Schema(name = "updated_at")
    private OffsetDateTime updatedAt;
}

