package com.se361.financial_service.dtos.responses;

import com.se361.financial_service.utils.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID id;
    private UUID contractId;
    private UUID propertyId;
    private UUID payerId;
    private String payerName;
    private String propertyTitle;
    private String contractNumber;
    private Constants.PaymentType paymentType;
    private Constants.PaymentStatus status;
    private BigDecimal amount;
    private BigDecimal penaltyAmount;
    private LocalDate dueDate;
    private LocalDateTime paidTime;
    private Integer installmentNumber;
    private String paymentMethod;
    private String transactionReference;
    private String notes;
    private String checkoutUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
