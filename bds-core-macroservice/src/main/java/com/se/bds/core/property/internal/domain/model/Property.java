package com.se.bds.core.property.internal.domain.model;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.message.validation.MSG12;
import com.se.bds.common.message.validation.MSG13;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.parameters.P;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Aggregate root for the Property bounded context.
 *
 * <h3>Cross-Module References (replaced with UUIDs)</h3>
 * <ul>
 *   <li>{@code ownerId} — references User module's PropertyOwner</li>
 *   <li>{@code assignedAgentId} — references User module's SaleAgent</li>
 *   <li>{@code wardId} — references Location module's Ward</li>
 * </ul>
 *
 * <h3>Removed Cross-Module Collections</h3>
 * <ul>
 *   <li>{@code List<Contract> contracts} — owned by Transaction module</li>
 *   <li>{@code List<Appointment> appointments} — owned by Appointment module</li>
 *   <li>{@code List<IdentificationDocument> documents} — owned by Document module</li>
 * </ul>
 */
@Entity
@Table(name = "properties")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "property_id", nullable = false)
    private UUID id;

    // ── Cross-module references (UUID only, no JPA joins) ──────────────

    /** References User module's PropertyOwner aggregate */
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    /** References User module's SaleAgent aggregate (nullable — not always assigned) */
    @Column(name = "assigned_agent_id")
    private UUID assignedAgentId;

    /** References Location module's Ward entity */
    @Column(name = "ward_id", nullable = false)
    private UUID wardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id", insertable = false, updatable = false)
    private Ward ward;

    // ── Intra-module JPA relationship ──────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_type_id", nullable = false)
    private PropertyType propertyType;

    // ── Property details ───────────────────────────────────────────────

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(name = "full_address")
    private String fullAddress;

    @Column(name = "area", nullable = false, precision = 10, scale = 2)
    private BigDecimal area;

    @Column(name = "rooms")
    private Integer rooms;

    @Column(name = "bathrooms")
    private Integer bathrooms;

    @Column(name = "floors")
    private Integer floors;

    @Column(name = "bedrooms")
    private Integer bedrooms;

    @Enumerated(EnumType.STRING)
    @Column(name = "house_orientation", length = 100)
    private Orientation houseOrientation;

    @Enumerated(EnumType.STRING)
    @Column(name = "balcony_orientation", length = 100)
    private Orientation balconyOrientation;

    @Column(name = "year_built")
    private Integer yearBuilt;

    // ── Financial fields ───────────────────────────────────────────────

    @Column(name = "price_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "price_per_square_meter", precision = 15, scale = 2)
    private BigDecimal pricePerSquareMeter;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;

    @Column(name = "service_fee_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal serviceFeeAmount;

    @Column(name = "service_fee_collected_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal serviceFeeCollectedAmount;

    // ── Metadata ───────────────────────────────────────────────────────

    @Column(name = "amenities", columnDefinition = "TEXT")
    private String amenities;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PropertyStatus status;

    @Column(name = "view_count")
    private Integer viewCount;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // ── Intra-module collection (same aggregate) ───────────────────────

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Media> mediaList = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IdentificationDocument> documents = new ArrayList<>();

    // ── Audit fields ───────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    // Domain logic
    private static final Set<PropertyStatus> TERMINAL_STATUSES =
            EnumSet.of(PropertyStatus.SOLD,PropertyStatus.DELETED, PropertyStatus.REMOVED);


    /**
     * Validates that a status transition is allowed and applies it.
     * @return the old status (for event publishing)
     * @throws BusinessException if the transition is invalid
     */

    public PropertyStatus transitionStatus(PropertyStatus newStatus) {
        if (TERMINAL_STATUSES.contains(this.status) && newStatus != PropertyStatus.AVAILABLE)
        {
            throw new BusinessException(MSG12.CODE, MSG12.MESSAGE);
        }
        PropertyStatus oldStatus = this.status;
        this.status = newStatus;
        return oldStatus;
    }

    /**
     * Marks property as SOLD when a purchase contract is activated.
     * @return the previous status
     */

    public PropertyStatus markAsSold()
    {
        return transitionStatus(PropertyStatus.SOLD);
    }

    /**
     * Marks property as RENTED when a rental contract is activated.
     * @return the previous status
     */
    public PropertyStatus markAsRented()
    {
        return transitionStatus(PropertyStatus.RENTED);
    }

    /**
     * Returns property to AVAILABLE (e.g. when a rental contract completes).
     * Only allowed from RENTED status.
     * @return the previous status
     */
    public PropertyStatus markAsAvailable()
    {
        if (this.status != PropertyStatus.RENTED)
        {
            throw new BusinessException(MSG12.CODE, MSG12.MESSAGE);
        }
        return transitionStatus(PropertyStatus.AVAILABLE);
    }

    /**
     * Soft-deletes the property.
     * @return the previous status
     */

    public PropertyStatus markAsDeleted()
    {
        return transitionStatus(PropertyStatus.DELETED);
    }


    /**
     * Removes property due to violation report.
     * @return the previous status
     */

    public PropertyStatus markAsRemoved()
    {
        return transitionStatus(PropertyStatus.REMOVED);
    }


    /**
     * Records a service fee payment. Adds amount to collected total.
     * @param amount the payment amount
     * @return true if the service fee is now fully paid
     */

    public boolean recordServiceFeePayment (BigDecimal amount)
    {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new BusinessException(MSG13.CODE, MSG13.MESSAGE);
        }
        this.serviceFeeCollectedAmount = this.serviceFeeCollectedAmount.add(amount);
        return isSeviceFullyPaid();
    }

    /**
     * @return true if the total collected service fee meets or exceeds the required amount
     */

    public boolean isSeviceFullyPaid() {
        return this.serviceFeeCollectedAmount.compareTo(this.serviceFeeAmount) >= 0;
    }

    /**
     * Assigns an agent and returns the previous agent ID (may be null).
     */

    public UUID assignAgent(UUID newAgentId)
    {
        UUID previous = this.assignedAgentId;
        this.assignedAgentId = newAgentId;
        return previous;
    }

    /**
     * @return true if the property is in a terminal status
     */

    public boolean isTerminal()
    {
        return TERMINAL_STATUSES.contains(this.status);
    }

}
