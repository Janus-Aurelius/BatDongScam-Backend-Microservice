package com.se361.iam_service.dto.response.ranking;

import com.se.bds.common.enums.PerformanceTierEnum;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndividualSalesAgentPerformanceMonth {
    private UUID agentId;
    private Integer month;
    private Integer year;
    private Integer performancePoint;
    private PerformanceTierEnum performanceTier;
    private Integer rankingPosition;
    private Integer handlingProperties;
    private Integer monthPropertiesAssigned;
    private Integer monthAppointmentsAssigned;
    private Integer monthAppointmentsCompleted;
    private Integer monthContracts;
    private Integer monthRates;
    private BigDecimal avgRating;
    private BigDecimal monthCustomerSatisfactionAvg;
}
