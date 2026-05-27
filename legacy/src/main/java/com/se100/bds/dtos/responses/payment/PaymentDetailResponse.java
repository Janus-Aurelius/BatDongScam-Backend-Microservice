package com.se100.bds.dtos.responses.payment;

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
public class PaymentDetailResponse {
    private UUID id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String paymentType;
    private String status;
    private PaymentPayerResponse payer;
    private BigDecimal amount;
    private BigDecimal penaltyAmount;
    private LocalDate dueDate;
    private LocalDateTime paidTime;
    private Integer installmentNumber;
    private String paymentMethod;
    private String transactionReference;
    private String notes;

    private String paywayPaymentId;

    @Builder
    @Data
    public static class PaymentPayerResponse {
        private UUID id;
        private String name;
        private String email;
        private String phoneNumber;
    }
}
