package com.se100.bds.dtos.requests.payment;

import com.se100.bds.utils.Constants;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdatePaymentStatusRequest {
    @NotNull(message = "Status is required")
    private Constants.PaymentStatusEnum status;
    
    private String notes;
    private String transactionReference;
}