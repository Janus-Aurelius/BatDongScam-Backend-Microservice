package com.se100.bds.models.schemas.report;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;

@Document(collection = "property_owner_contribution_report")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PropertyOwnerContributionReport extends AbstractBaseMongoReport {
    @Field("total_owners")
    private Integer totalOwners;

    @Field("new_this_month")
    private Integer newThisMonth;

    @Field("contribution_value")
    private BigDecimal contributionValue;

    @Field("avg_owners_contribution_value")
    private BigDecimal avgOwnersContributionValue;
}
