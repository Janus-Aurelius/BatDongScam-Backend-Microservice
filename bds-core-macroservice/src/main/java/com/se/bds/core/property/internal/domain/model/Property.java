package com.se.bds.core.property.internal.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    // ── Audit fields ───────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
