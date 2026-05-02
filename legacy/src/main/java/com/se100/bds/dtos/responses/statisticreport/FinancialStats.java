package com.se100.bds.dtos.responses.statisticreport;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class FinancialStats {
    private BigDecimal totalRevenue;
    private BigDecimal tax;
    private BigDecimal netProfit;
    private Double avgRating;
    private Integer totalRates;
    Map<Integer, BigDecimal> totalRevenueChart;
    Map<Integer, Integer> totalContractsChart;
    Map<Integer, BigDecimal> agentSalaryChart;
    Map<String, Map<Integer, BigDecimal>> targetRevenueChart;
}
