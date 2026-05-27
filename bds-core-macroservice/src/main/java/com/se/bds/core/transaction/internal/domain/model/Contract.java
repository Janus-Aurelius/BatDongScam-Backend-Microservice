package com.se.bds.core.transaction.internal.domain.model;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.message.validation.MSG12;
import com.se.bds.core.shared.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Abstract aggregate root for the Transaction bounded context.
 * Uses JOINED inheritance to map Deposit, Rental, and Purchase subtypes.
 *
 * <h3>Cross-Module References (replaced with UUIDs)</h3>
 * <ul>
 *   <li>{@code propertyId} — references Property module's Property</li>
 *   <li>{@code customerId} — references User module's Customer</li>
 *   <li>{@code agentId} — references User module's SaleAgent</li>
 * </ul>
 */
@Entity
@Table(name = "contract")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "contract_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "contract_id", nullable = false)
    private UUID id;

    // ── Cross-module references (UUID only, no JPA joins) ──────────────

    /** References Property module's Property aggregate */
    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    /** References User module's Customer aggregate */
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    /** References User module's SaleAgent aggregate (nullable) */
    @Column(name = "agent_id")
    private UUID agentId;

    // ── Contract details ───────────────────────────────────────────────

    @Column(name = "contract_type", insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private ContractType contractType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ContractStatus status;

    /** Contract number (or ID) of the actual, physical contract document */
    @Column(name = "contract_number", unique = true, length = 50)
    private String contractNumber;

    /** Date when the contract terms become effective */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Date when the contract terms end */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** Date that the PHYSICAL contract is legalized and signed by all parties */
    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "special_terms", columnDefinition = "TEXT")
    private String specialTerms;

    // ── Cancellation specific fields ───────────────────────────────────

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    /** For deposit contracts, if null, this will use the deposit amount */
    @Column(name = "cancellation_penalty", precision = 15, scale = 2)
    private BigDecimal cancellationPenalty;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by")
    private Role cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // ── Intra-module collection ────────────────────────────────────────

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    // ── Rating and review ──────────────────────────────────────────────

    @Column(name = "rating")
    private Short rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    // ── Audit fields ───────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    //Domain logic
    private static final Set<ContractStatus> TERMINAL_STATUSES = EnumSet.of(ContractStatus.COMPLETED, ContractStatus.CANCELLED);

    /**
     * @return true if the contract is in a terminal state (COMPLETED or CANCELLED)
     */
    public boolean isTerminal()
    {
        return TERMINAL_STATUSES.contains(this.status);
    }

    /**
     * @return true if the contract is currently ACTIVE
     */
    public boolean isActive()
    {
        return this.status == ContractStatus.ACTIVE;
    }

    /**
     * Guarded status transition. Prevents transitions from terminal states.
     * @param newStatus the target status
     * @return the previous status (for event publishing)
     * @throws IllegalStateException if the contract is in a terminal state
     */

    public ContractStatus transitionTo(ContractStatus newStatus)
    {
        if (isTerminal())
        {
            throw new BusinessException(MSG12.CODE, MSG12.MESSAGE);
        }

        validateTransition(this.status, newStatus);

        ContractStatus oldStatus = this.status;
        this.status = newStatus;
        return oldStatus;
    }

    private void validateTransition(ContractStatus oldStatus, ContractStatus newStatus) {
        if (oldStatus == ContractStatus.DRAFT && newStatus == ContractStatus.WAITING_OFFICIAL) {
            if (this.propertyId == null) {
                throw new BusinessException(MSG12.CODE, "Property ID must not be null");
            }
            if (this.customerId == null) {
                throw new BusinessException(MSG12.CODE, "Customer ID must not be null");
            }
            if (this instanceof RentalContract) {
                RentalContract rental = (RentalContract) this;
                if (rental.getMonthlyRentAmount() == null || rental.getMonthlyRentAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException(MSG12.CODE, "Monthly rent amount must be populated and positive");
                }
            } else if (this instanceof DepositContract) {
                DepositContract deposit = (DepositContract) this;
                if (deposit.getDepositAmount() == null || deposit.getDepositAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException(MSG12.CODE, "Deposit amount must be populated and positive");
                }
            } else if (this instanceof PurchaseContract) {
                PurchaseContract purchase = (PurchaseContract) this;
                if (purchase.getPropertyValue() == null || purchase.getPropertyValue().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException(MSG12.CODE, "Property value must be populated and positive");
                }
            }
        }

        if (oldStatus == ContractStatus.WAITING_OFFICIAL && newStatus == ContractStatus.PENDING_PAYMENT) {
            if (this.contractNumber == null || this.contractNumber.trim().isEmpty()) {
                throw new BusinessException(MSG12.CODE, "Signed paperwork must exist before moving to PENDING_PAYMENT");
            }
        }

        if (oldStatus == ContractStatus.PENDING_PAYMENT && newStatus == ContractStatus.ACTIVE) {
            boolean hasSuccessPayment = false;
            if (this.payments != null) {
                for (Payment p : this.payments) {
                    if (p.getStatus() == PaymentStatus.SUCCESS) {
                        hasSuccessPayment = true;
                        break;
                    }
                }
            }
            if (!hasSuccessPayment) {
                throw new BusinessException(MSG12.CODE, "At least first payment must be marked SUCCESS to activate the contract");
            }
        }
    }

    /**
     * Activates the contract. Only allowed from non-terminal states.
     * @return the previous status
     */

    public ContractStatus activate()
    {
        return transitionTo(ContractStatus.ACTIVE);
    }

    /**
     * Completes the contract. Only allowed from ACTIVE.
     * @return the previous status
     * @throws IllegalStateException if the contract is not ACTIVE
     */

    public ContractStatus complete()
    {
        if (this.status != ContractStatus.ACTIVE)
        {
            throw new BusinessException(MSG12.CODE, MSG12.MESSAGE);
        }
        ContractStatus old = this.status;
        this.status = ContractStatus.COMPLETED;
        return old;
    }

    /**
     * Cancels the contract with reason and initiator.
     * @return previous status
     */

    public ContractStatus cancel(String reason, Role intiator)
    {
        if (isTerminal())
        {
            throw new BusinessException(MSG12.CODE, MSG12.MESSAGE);
        }
        ContractStatus oldStatus = this.status;
        this.status = ContractStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledBy = intiator;
        this.cancelledAt = LocalDateTime.now();
        return oldStatus;
    }
}
