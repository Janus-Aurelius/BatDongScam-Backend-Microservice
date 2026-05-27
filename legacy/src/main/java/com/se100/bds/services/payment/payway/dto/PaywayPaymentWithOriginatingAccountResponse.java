package com.se100.bds.services.payment.payway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class PaywayPaymentWithOriginatingAccountResponse {

    private String id;

    private BigDecimal amount;

    private String currency;

    private String status;

    @JsonProperty("originating_account_id")
    private String originatingAccountId;

    @JsonProperty("originating_account")
    private PaywayAccountRef originatingAccount;

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

