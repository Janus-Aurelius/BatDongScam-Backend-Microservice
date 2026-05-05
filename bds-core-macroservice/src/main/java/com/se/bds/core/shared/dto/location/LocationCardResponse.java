package com.se.bds.core.shared.dto.location;

import com.se.bds.common.dto.AbstractBaseDataResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class LocationCardResponse extends AbstractBaseDataResponse {
    private String name;
    private String locationTypeEnum; // Replaced Constants.LocationEnum for independence, or define in shared
    private String imgUrl;
    private BigDecimal totalArea;
    private BigDecimal avgLandPrice;
    private Integer population;
    private Boolean isActive;
    private Boolean isFavorite;
}
