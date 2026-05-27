package com.se100.bds.models.schemas.ranking;

import com.se100.bds.models.schemas.AbstractBaseMongoSchema;
import com.se100.bds.utils.Constants;
import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.util.UUID;

@Document(collection = "individual_sales_agent_performance_month")
@CompoundIndexes({
        @CompoundIndex(name = "agent_month_year_idx", def = "{'agent_id': 1, 'month': 1, 'year': 1}", unique = true)
})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndividualSalesAgentPerformanceMonth extends AbstractBaseMongoSchema {
    @Field("agent_id")
    private UUID agentId;

    @Field("month")
    private Integer month;

    @Field("year")
    private Integer year;

    @Field("performance_point")
    private Integer performancePoint;

    @Field("performance_tier")
    private Constants.PerformanceTierEnum performanceTier;

    @Field("ranking_position")
    private Integer rankingPosition;

    @Field("handling_properties")
    private Integer handlingProperties;

    @Field("month_properties_assigned")
    private Integer monthPropertiesAssigned;

    @Field("month_appointments_assigned")
    private Integer monthAppointmentsAssigned;

    @Field("month_appointments_completed")
    private Integer monthAppointmentsCompleted;

    @Field("month_contracts")
    private Integer monthContracts;

    @Field("month_rates")
    private Integer monthRates;

    @Field("avg_rating")
    private BigDecimal avgRating;

    @Field("month_customer_satisfaction_avg")
    private BigDecimal monthCustomerSatisfactionAvg;
}
