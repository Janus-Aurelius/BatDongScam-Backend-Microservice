package com.se100.bds.models.entities.location;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.se100.bds.models.entities.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "districts")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "district_id", nullable = false)),
})
public class District extends AbstractBaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    @JsonIgnore
    private City city;

    @Column(name = "district_name")
    private String districtName;

    @Column(name = "img_url")
    private String imgUrl;

    @Column(name = "description")
    private String description;

    @Column(name = "total_area", precision = 15, scale = 2)
    private BigDecimal totalArea;

    @Column(name = "avg_land_price", precision = 15, scale = 2)
    private BigDecimal avgLandPrice;

    @Column(name = "population")
    private Integer population;

    @Column(name = "is_active")
    private Boolean isActive;

    @OneToMany(mappedBy = "district", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Ward> wards = new ArrayList<>();
}
