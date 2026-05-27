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

@Document(collection = "individual_customer_potential_month")
@CompoundIndexes({
        @CompoundIndex(name = "customer_month_year_idx", def = "{'customer_id': 1, 'month': 1, 'year': 1}", unique = true)
})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndividualCustomerPotentialMonth extends AbstractBaseMongoSchema {
    @Field("customer_id")
    private UUID customerId;

    @Field("month")
    private Integer month;

    @Field("year")
    private Integer year;

    @Field("lead_score")
    private Integer leadScore;

    @Field("customer_tier")
    private Constants.CustomerTierEnum customerTier;

    @Field("lead_position")
    private Integer leadPosition;

    @Field("month_viewings_requested")
    private Integer monthViewingsRequested;

    @Field("month_viewings_attended")
    private Integer monthViewingAttended;

    @Field("month_spending")
    private BigDecimal monthSpending;

    @Field("month_purchases")
    private Integer monthPurchases;

    @Field("month_rentals")
    private Integer monthRentals;

    @Field("month_contracts_signed")
    private Integer monthContractsSigned;
}
