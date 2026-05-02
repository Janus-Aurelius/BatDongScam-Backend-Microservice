package com.se100.bds.models.schemas.report;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;

@Document(collection = "customer_analytics_reports")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAnalyticsReport extends AbstractBaseMongoReport {
    @Field("total_customers")
    private Integer totalCustomers;

    @Field("new_customers_acquired_current_month")
    private Integer newCustomerAcquiredCurrentMonth;

    @Field("avg_customer_transaction_value")
    private BigDecimal avgCustomerTransactionValue;

    @Field("high_value_customer_count")
    private Integer highValueCustomerCount;

    @Field("customer_satisfaction_score")
    private BigDecimal customerSatisfactionScore;

    @Field("total_rates")
    private Integer totalRates;

    @Field("avg_rating")
    private BigDecimal avgRating;
}
