package com.se100.bds.dtos.responses.admindashboard;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class DashboardPropertyDistribution {
    List<PropertyTypeDistribution> propertyTypes;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PropertyTypeDistribution {
        private String typeName;
        private Integer count;
        private Double percentage;
    }
}
