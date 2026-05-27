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
public class PaywayCreatePayoutRequest {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    private String currency;

    @JsonProperty("account_number")
    private String accountNumber;

    @JsonProperty("account_holder_name")
    private String accountHolderName;

    @JsonProperty("swift_code")
    private String swiftCode;

    private String description;

    private Map<String, Object> metadata;

    @JsonProperty("webhook_url")
    private String webhookUrl;
}
