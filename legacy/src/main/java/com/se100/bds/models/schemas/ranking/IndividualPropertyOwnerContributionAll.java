package com.se100.bds.models.schemas.ranking;

import com.se100.bds.models.schemas.AbstractBaseMongoSchema;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.util.UUID;

@Document(collection = "individual_property_owner_contribution_all")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndividualPropertyOwnerContributionAll extends AbstractBaseMongoSchema {
    @Field("owner_id")
    @Indexed(unique = true)
    private UUID ownerId;

    @Field("contribution_point")
    private Integer contributionPoint;

    @Field("ranking_position")
    private Integer rankingPosition;

    @Field("contribution_value")
    private BigDecimal contributionValue;

    @Field("total_properties")
    private Integer totalProperties;

    @Field("total_properties_sold")
    private Integer totalPropertiesSold;

    @Field("total_properties_rented")
    private Integer totalPropertiesRented;
}
