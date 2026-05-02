package com.se100.bds.dtos.responses.location;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import com.se100.bds.utils.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class LocationDetailsResponse extends AbstractBaseDataResponse {
    private String name;
    private Constants.LocationEnum locationTypeEnum;
    private String description;
    private String imgUrl;
    private BigDecimal totalArea;
    private BigDecimal avgLandPrice;
    private Integer population;
    private Boolean isActive;
    private Boolean isFavorite;
    private Integer districtCount;
    private Integer wardCount;
    private Integer activeProperties;
}
