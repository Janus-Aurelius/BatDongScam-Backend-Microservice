package com.se100.bds.services.payment.payway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaywayAccountRef {

    private String id;

    private String role;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("account_holder_name")
    private String accountHolderName;

    @JsonProperty("account_number")
    private String accountNumber;

    @JsonProperty("swift_code")
    private String swiftCode;

    private String currency;
}

