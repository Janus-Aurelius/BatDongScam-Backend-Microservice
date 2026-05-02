package com.se100.bds.dtos.responses.admindashboard;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class DashboardTotalProperties {
    Map<Integer, Integer> totalProperties;
}
