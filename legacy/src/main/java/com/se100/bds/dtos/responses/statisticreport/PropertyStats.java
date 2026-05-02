package com.se100.bds.dtos.responses.statisticreport;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class PropertyStats {
    private Integer activeProperties;
    private Integer newProperties;
    private Integer totalSold;
    private Integer totalRented;
    private Map<Integer, Integer> totalProperties;
    private Map<Integer, Integer> totalSoldProperties;
    private Map<Integer, Integer> totalRentedProperties;
    private Map<String, Map<Integer, Long>> searchedTargets;
    private Map<String, Map<Integer, Long>> favoriteTargets;
}
