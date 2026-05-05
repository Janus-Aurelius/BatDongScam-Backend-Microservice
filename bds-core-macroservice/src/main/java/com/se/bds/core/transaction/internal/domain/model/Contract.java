package com.se.bds.core.transaction.internal.domain.model;

import com.se.bds.core.shared.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
}
