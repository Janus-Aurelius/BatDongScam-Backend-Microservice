package com.se.bds.core.property.internal.v2.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing a domain event entry in the Event Store.
 */
@Entity
@Table(name = "property_event_store", schema = "property_catalog")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyEventEntry {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
