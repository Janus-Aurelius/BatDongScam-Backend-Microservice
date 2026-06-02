package com.se.bds.core.property.internal.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "cities", schema = "property_catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class City {
    @Id
    @Column(name = "city_id", nullable = false)
    private UUID id;

    @Column(name = "city_name", nullable = false)
    private String cityName;
}
