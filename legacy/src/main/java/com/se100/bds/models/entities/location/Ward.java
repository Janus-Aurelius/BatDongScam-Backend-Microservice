package com.se100.bds.models.entities.location;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.se100.bds.models.entities.AbstractBaseEntity;
import com.se100.bds.models.entities.property.Property;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wards")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "ward_id", nullable = false)),
})
public class Ward extends AbstractBaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    @JsonIgnore
    private District district;

    @Column(name = "ward_name", nullable = false)
    private String wardName;

    @Column(name = "img_url")
    private String imgUrl;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "total_area", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalArea;

    @Column(name = "avg_land_price", precision = 15, scale = 2)
    private BigDecimal avgLandPrice;

    @Column(name = "population", nullable = false)
    private Integer population;

    @Column(name = "is_active")
    private Boolean isActive;

    @OneToMany(mappedBy = "ward", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Property> properties = new ArrayList<>();
}
