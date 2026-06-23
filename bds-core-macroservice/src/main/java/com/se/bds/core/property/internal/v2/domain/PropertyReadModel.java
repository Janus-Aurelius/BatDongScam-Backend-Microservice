package com.se.bds.core.property.internal.v2.domain;

import com.se.bds.core.property.internal.domain.model.PropertyStatus;
import com.se.bds.core.property.internal.domain.model.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read Model Entity for direct, flat queries on property listings.
 */
@Entity
@Table(name = "property_read_views", schema = "property_catalog")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyReadModel {

    @Id
    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "ward_id", nullable = false)
    private UUID wardId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "price_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PropertyStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
