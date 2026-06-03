package com.se361.financial_service.dtos.requests;

import com.se.bds.common.enums.PaymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreatePaymentRequest {
    @NotNull(message = "Contract ID is required")
    private UUID contractId;

    @NotNull(message = "Property ID is required")
    private UUID propertyId;

    @NotNull(message = "Payer ID is required")
    private UUID payerId;

    private String payerName;
    private String propertyTitle;
    private String contractNumber;

    @NotNull(message = "Payment type is required")
    private PaymentType paymentType;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private Integer installmentNumber;
    private String paymentMethod;
    private String notes;
}
