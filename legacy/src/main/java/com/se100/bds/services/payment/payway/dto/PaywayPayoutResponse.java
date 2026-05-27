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
public class PaywayPayoutResponse {

    private String id;

    private BigDecimal amount;

    private String currency;

    /** created | paid | failed */
    private String status;

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

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
}

