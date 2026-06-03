package com.se.bds.core.transaction.internal.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.se.bds.common.enums.PaymentType;
import com.se.bds.common.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a financial transaction / payment installment within a contract.
 *
 * <h3>Cross-Module References (replaced with UUIDs)</h3>
 * <ul>
 *   <li>{@code propertyId} — references Property module's Property
 *       (denormalized for efficient payment queries without joining Contract)</li>
 *   <li>{@code payerUserId} — references User module's User</li>
 * </ul>
 */
@Entity
@Table(name = "payments", schema = "transaction_workflow")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id", nullable = false)
    private UUID id;

    // ── Intra-module relationship ──────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    // ── Cross-module references (UUID only) ────────────────────────────

    /** References Property module's Property (denormalized for query performance) */
    @Column(name = "property_id")
    private UUID propertyId;

    /** References User module's User (the payer) */
    @Column(name = "payer_user_id")
    private UUID payerUserId;

    // ── Payment details ────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus status;

    @Column(name = "penalty_amount", precision = 15, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "payway_payment_id", unique = true, length = 36)
    private String paywayPaymentId;

    // ── Audit fields ───────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
