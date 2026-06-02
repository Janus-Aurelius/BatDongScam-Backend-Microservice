package com.se361.financial_service.dtos.requests;

import com.se361.financial_service.utils.Constants;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePaymentStatusRequest {

    @NotNull(message = "Status is required")
    private Constants.PaymentStatus status;

    private String notes;
    private String transactionReference;

}
