package com.se361.financial_service.gateway.payway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaywayPaymentResponse {
    private String id;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String description;
    private Map<String, Object> metadata;
    @JsonProperty("return_url")
    private String returnUrl;
    @JsonProperty("webhook_url")
    private String webhookUrl;
    @JsonProperty("checkout_url")
    private String checkoutUrl;
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
}
