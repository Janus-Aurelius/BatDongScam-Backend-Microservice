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

@Document(collection = "individual_property_owner_contribution_month")
@CompoundIndexes({
        @CompoundIndex(name = "owner_month_year_idx", def = "{'owner_id': 1, 'month': 1, 'year': 1}", unique = true)
})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndividualPropertyOwnerContributionMonth extends AbstractBaseMongoSchema {
    @Field("owner_id")
    private UUID ownerId;

    @Field("month")
    private Integer month;

    @Field("year")
    private Integer year;

    @Field("contribution_point")
    private Integer contributionPoint;

    @Field("contribution_tier")
    private Constants.ContributionTierEnum contributionTier;

    @Field("ranking_position")
    private Integer rankingPosition;

    @Field("month_contribution_value")
    private BigDecimal monthContributionValue;

    @Field("month_total_properties")
    private Integer monthTotalProperties;

    @Field("month_total_for_sales")
    private Integer monthTotalForSales;

    @Field("month_total_for_rents")
    private Integer monthTotalForRents;

    @Field("month_total_properties_sold")
    private Integer monthTotalPropertiesSold;

    @Field("month_total_properties_rented")
    private Integer monthTotalPropertiesRented;
}
