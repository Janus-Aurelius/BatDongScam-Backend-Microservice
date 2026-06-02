package com.se.bds.core.transaction.internal.domain.model;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.message.validation.MSG12;
import com.se.bds.common.message.validation.MSG13;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a multi-stage payment hold for escrow protection (US-028).
 * Supports rental security deposits, deposit contract amounts, and purchase escrow.
 *
 * <p>Uses optimistic locking ({@code @Version}) for concurrent escrow state transitions.
 */
@Entity
@Table(name = "escrow_hold", schema = "transaction_workflow")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscrowHold {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "escrow_id", nullable = false)
    private UUID id;

    /** The contract this escrow hold is associated with */
    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    /** The type of contract (RENTAL, DEPOSIT, PURCHASE) */
    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false)
    private ContractType contractType;

    /** The payment that funded this escrow hold (nullable if not yet funded) */
    @Column(name = "payment_id")
    private UUID paymentId;

    /** The property associated with this escrow */
    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    /** The customer whose funds are held */
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    /** Total amount held in escrow */
    @Column(name = "hold_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal holdAmount;

    /** Amount that has been released (partial release support) */
    @Column(name = "released_amount", precision = 15, scale = 2)
    private BigDecimal releasedAmount = BigDecimal.ZERO;

    /** Amount forfeited/deducted */
    @Column(name = "deducted_amount", precision = 15, scale = 2)
    private BigDecimal deductedAmount = BigDecimal.ZERO;

    /** Current escrow status */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EscrowStatus status;

    /** Bank account details for release (Encrypted at rest) */
    @Convert(converter = com.se.bds.core.transaction.internal.support.AesAttributeConverter.class)
    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_account_name")
    private String bankAccountName;

    @Column(name = "bank_bin")
    private String bankBin;

    /** Description of the escrow hold purpose */
    @Column(name = "description", length = 500)
    private String description;

    /** Reason for release/forfeiture decision */
    @Column(name = "decision_reason", columnDefinition = "TEXT")
    private String decisionReason;

    /** Admin who made the release/forfeiture decision */
    @Column(name = "decided_by_user_id")
    private UUID decidedByUserId;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    /** Optimistic locking for concurrent escrow state transitions */
    @Version
    @Column(name = "version")
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Domain Logic ──

    /**
     * @return the remaining amount still held in escrow
     */
    public BigDecimal getRemainingAmount() {
        return holdAmount.subtract(releasedAmount).subtract(deductedAmount);
    }

    /**
     * Transition to HELD status when payment is confirmed.
     */
    public void confirmHold() {
        if (this.status != EscrowStatus.PENDING) {
            throw new BusinessException(MSG12.CODE, MSG12.MESSAGE);
        }
        this.status = EscrowStatus.HELD;
    }

    /**
     * Release full amount to the owner.
     */
    public void releaseToOwner(UUID adminUserId, String reason) {
        validateHeld();
        this.releasedAmount = this.holdAmount;
        this.status = EscrowStatus.RELEASED_TO_OWNER;
        this.decidedByUserId = adminUserId;
        this.decisionReason = reason;
        this.decidedAt = LocalDateTime.now();
    }

    /**
     * Return full amount to the customer.
     */
    public void returnToCustomer(UUID adminUserId, String reason) {
        validateHeld();
        this.releasedAmount = this.holdAmount;
        this.status = EscrowStatus.RETURNED_TO_CUSTOMER;
        this.decidedByUserId = adminUserId;
        this.decisionReason = reason;
        this.decidedAt = LocalDateTime.now();
    }

    /**
     * Partially release: deduct a portion and return the rest.
     */
    public void partialRelease(BigDecimal deduction, UUID adminUserId, String reason) {
        validateHeld();
        if (deduction.compareTo(this.holdAmount) > 0) {
            throw new BusinessException(MSG13.CODE, MSG13.MESSAGE);
        }
        this.deductedAmount = deduction;
        this.releasedAmount = this.holdAmount.subtract(deduction);
        this.status = EscrowStatus.PARTIALLY_RELEASED;
        this.decidedByUserId = adminUserId;
        this.decisionReason = reason;
        this.decidedAt = LocalDateTime.now();
    }

    /**
     * Forfeit the entire amount (contract breach).
     */
    public void forfeit(UUID adminUserId, String reason) {
        validateHeld();
        this.deductedAmount = this.holdAmount;
        this.status = EscrowStatus.FORFEITED;
        this.decidedByUserId = adminUserId;
        this.decisionReason = reason;
        this.decidedAt = LocalDateTime.now();
    }

    private void validateHeld() {
        if (this.status != EscrowStatus.HELD) {
            throw new BusinessException(MSG12.CODE, MSG12.MESSAGE);
        }
    }
}
