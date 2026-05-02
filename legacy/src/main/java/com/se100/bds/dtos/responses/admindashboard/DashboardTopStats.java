package com.se100.bds.dtos.responses.admindashboard;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class DashboardTopStats {
    private Integer totalProperties;
    private Integer totalContracts;
    private BigDecimal monthRevenue;
    private Integer totalUsers;
    private Double customerStatisfaction;
}
