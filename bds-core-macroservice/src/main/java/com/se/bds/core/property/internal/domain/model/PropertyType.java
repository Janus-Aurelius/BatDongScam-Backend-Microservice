package com.se.bds.core.property.internal.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lookup / reference-data entity representing a category of property
 * (e.g. "Apartment", "Villa", "Land").
 *
 * <p>The legacy bidirectional {@code @OneToMany List<Property> properties}
 * has been removed — the owning side ({@code Property.propertyType}) is sufficient.
 */
@Entity
@Table(name = "property_types")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PropertyType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "property_type_id", nullable = false)
    private UUID id;

    @Column(name = "type_name", nullable = false, unique = true, length = 50)
    private String typeName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive;

    // ── Audit fields ───────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
