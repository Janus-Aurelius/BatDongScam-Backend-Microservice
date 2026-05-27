package com.se100.bds.dtos.requests.payos;

import com.se100.bds.utils.Constants;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateContractPaymentRequest {
    @NotNull
    @Min(1)
    private BigDecimal amount;

    private Constants.PaymentTypeEnum paymentType = Constants.PaymentTypeEnum.FULL_PAY;

    private Integer installmentNumber;

    private String description;

    private String returnUrl;
    private String cancelUrl;
}
