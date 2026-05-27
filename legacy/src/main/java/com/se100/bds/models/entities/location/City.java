package com.se100.bds.models.entities.location;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.se100.bds.models.entities.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cities")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "city_id", nullable = false)),
})
public class City extends AbstractBaseEntity {
    @Column(name = "city_name")
    private String cityName;

    @Column(name = "description")
    private String description;

    @Column(name = "img_url")
    private String imgUrl;

    @Column(name = "total_area", precision = 15, scale = 2)
    private BigDecimal totalArea;

    @Column(name = "avg_land_price", precision = 15, scale = 2)
    private BigDecimal avgLandPrice;

    @Column(name = "population")
    private Integer population;

    @Column(name = "is_active")
    private Boolean isActive;

    @OneToMany(mappedBy = "city", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<District> districts = new ArrayList<>();
}
