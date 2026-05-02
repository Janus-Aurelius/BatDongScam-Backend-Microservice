package com.se100.bds.models.schemas.report;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;

@Document(collection = "agent_performance_reports")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgentPerformanceReport extends AbstractBaseMongoReport {
    @Field("total_agents")
    private Integer totalAgents;

    @Field("new_this_month")
    private Integer newThisMonth;

    @Field("avg_customer_satisfaction")
    private BigDecimal avgCustomerSatisfaction;

    @Field("total_rates")
    private Integer totalRates;

    @Field("avg_rating")
    private BigDecimal avgRating;
}
