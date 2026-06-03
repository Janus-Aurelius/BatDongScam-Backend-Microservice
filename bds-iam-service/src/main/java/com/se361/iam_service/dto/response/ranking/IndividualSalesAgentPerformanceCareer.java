package com.se361.iam_service.dto.response.ranking;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndividualSalesAgentPerformanceCareer {
    private UUID agentId;
    private Integer performancePoint;
    private Integer careerRanking;
    private Integer propertiesAssigned;
    private Integer appointmentAssigned;
    private Integer appointmentCompleted;
    private Integer totalContracts;
    private BigDecimal customerSatisfactionAvg;
    private Integer totalRates;
    private BigDecimal avgRating;
}
