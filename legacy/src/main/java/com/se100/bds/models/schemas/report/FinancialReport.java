package com.se100.bds.models.schemas.report;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.util.List;

@Document(collection = "financial_reports")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FinancialReport extends AbstractBaseMongoReport {
    @Field("total_revenue")
    private BigDecimal totalRevenue;

    @Field("contract_count")
    private Integer contractCount;

    @Field("tax")
    private BigDecimal tax;

    @Field("net_profit")
    private BigDecimal netProfit;

    @Field("total_rates")
    private Integer totalRates;

    @Field("avg_rating")
    private BigDecimal avgRating;

    @Field("total_salary")
    private BigDecimal totalSalary;

    @Field("revenue_cities")
    private List<RankedRevenueItem> revenueCities;

    @Field("revenue_districts")
    private List<RankedRevenueItem> revenueDistricts;

    @Field("revenue_wards")
    private List<RankedRevenueItem> revenueWards;

    @Field("revenue_property_types")
    private List<RankedRevenueItem> revenuePropertyTypes;

    @Field("sale_agents_salary_month")
    private List<AgentSalaryItem> saleAgentsSalaryMonth;

    @Field("sale_agents_salary_career")
    private List<AgentSalaryItem> saleAgentsSalaryCareer;
}
