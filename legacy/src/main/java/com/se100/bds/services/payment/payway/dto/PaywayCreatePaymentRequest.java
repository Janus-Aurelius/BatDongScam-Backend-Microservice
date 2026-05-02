package com.se100.bds.services.payment.payway.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class PaywayCreatePaymentRequest {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    private String currency;

    private String description;

    private Map<String, Object> metadata;

    @JsonProperty("return_url")
    private String returnUrl;

    @JsonProperty("webhook_url")
    private String webhookUrl;
}
