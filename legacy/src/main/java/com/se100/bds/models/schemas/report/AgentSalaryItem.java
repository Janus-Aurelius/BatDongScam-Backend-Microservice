package com.se100.bds.models.schemas.report;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentSalaryItem {
    private UUID agentId;
    private BigDecimal salary;
    private BigDecimal paid;
    private BigDecimal unPaid;
    private BigDecimal bonus;
}
