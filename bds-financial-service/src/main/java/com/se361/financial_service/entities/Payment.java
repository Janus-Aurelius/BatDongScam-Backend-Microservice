package com.se361.financial_service.entities;

import com.se.bds.common.enums.PaymentType;
import com.se.bds.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends AbstractBaseEntity {
    // === REFERENCES (UUID only, no FK to other services) ===

    @Column(name = "contract_id")
    private UUID contractId;

    @Column(name = "property_id")
    private UUID propertyId;

    @Column(name = "payer_id")
    private UUID payerId;

    // === SNAPSHOT DATA (captured at creation time) ===

    @Column(name = "payer_name")
    private String payerName;

    @Column(name = "property_title")
    private String propertyTitle;

    @Column(name = "contract_number")
    private String contractNumber;

    // === PAYMENT DATA ===

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_time")
    private LocalDateTime paidTime;

    @Column(name = "installment_number")
    private Integer installmentNumber;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    @Column(name = "penalty_amount", precision = 15, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // === PAYMENT GATEWAY ===

    @Column(name = "payos_payment_id", unique = true)
    private String payosPaymentId;

    @Column(name = "checkout_url", columnDefinition = "TEXT")
    private String checkoutUrl;
}
