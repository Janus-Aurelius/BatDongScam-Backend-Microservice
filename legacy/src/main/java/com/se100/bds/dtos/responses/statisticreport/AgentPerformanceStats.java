package com.se100.bds.dtos.responses.statisticreport;

import com.se100.bds.utils.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class AgentPerformanceStats {
    Map<Integer, Integer> totalAgents;
    Map<Integer, Integer> totalRates;
    Map<Integer, Double> avgRating;
    Map<Integer, Double> customerSatisfaction;
    Map<Constants.PerformanceTierEnum, Map<Integer, Double>> tierDistribution;
}
