package com.se100.bds.models.schemas.ranking;

import com.se100.bds.models.schemas.AbstractBaseMongoSchema;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.util.UUID;

@Document(collection = "individual_sales_agent_performance_career")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndividualSalesAgentPerformanceCareer extends AbstractBaseMongoSchema {
    @Field("agent_id")
    @Indexed(unique = true)
    private UUID agentId;

    @Field("performance_point")
    private Integer performancePoint;

    @Field("career_ranking")
    private Integer careerRanking;

    @Field("properties_assigned")
    private Integer propertiesAssigned;

    @Field("appointment_assigned")
    private Integer appointmentAssigned;

    @Field("appointment_completed")
    private Integer appointmentCompleted;

    @Field("total_contracts")
    private Integer totalContracts;

    @Field("customer_satisfaction_avg")
    private BigDecimal customerSatisfactionAvg;

    @Field("total_rates")
    private Integer totalRates;

    @Field("avg_rating")
    private BigDecimal avgRating;
}
