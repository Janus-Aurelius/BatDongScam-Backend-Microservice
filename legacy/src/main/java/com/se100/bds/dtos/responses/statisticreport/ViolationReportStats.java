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
public class ViolationReportStats {
    private Integer totalViolationReports;
    private Integer newThisMonth;
    private Integer unsolved;
    private Double avgResolutionTimeHours;
    private Map<Integer, Integer> totalViolationReportChart;
    private Map<String, Map<Integer, Long>> violationTrends;
    private Map<Integer, Integer> accountsSuspendedChart;
    private Map<Integer, Integer> propertiesRemovedChart;
}
