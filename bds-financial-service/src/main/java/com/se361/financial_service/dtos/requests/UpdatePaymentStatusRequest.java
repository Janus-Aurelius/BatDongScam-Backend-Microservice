package com.se361.financial_service.dtos.requests;

import com.se.bds.common.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePaymentStatusRequest {

    @NotNull(message = "Status is required")
    private PaymentStatus status;

    private String notes;
    private String transactionReference;

}
