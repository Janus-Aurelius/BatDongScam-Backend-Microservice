package com.se100.bds.dtos.responses.user.meprofile;

import com.se100.bds.utils.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class SalesAgentResponse {
    private String employeeCode;
    private int maxProperties;
    private LocalDateTime hiredDate;
    private BigDecimal currentMonthRevenue;
    private BigDecimal totalRevenue;
    private int currentMonthDeals;
    private int totalDeals;
    private int activeProperties;
    private Constants.PerformanceTierEnum performanceTier;
    private int currentMonthRanking;
    private int careerRanking;
}
