package com.se100.bds.models.schemas.ranking;

import com.se100.bds.models.schemas.AbstractBaseMongoSchema;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.util.UUID;

@Document(collection = "individual_customer_potential_all")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndividualCustomerPotentialAll extends AbstractBaseMongoSchema {
    @Field("customer_id")
    @Indexed(unique = true)
    private UUID customerId;

    @Field("lead_score")
    private Integer leadScore;

    @Field("lead_position")
    private Integer leadPosition;

    @Field("viewings_requested")
    private Integer viewingsRequested;

    @Field("viewings_attended")
    private Integer viewingsAttended;

    @Field("spending")
    private BigDecimal spending;

    @Field("total_purchases")
    private Integer totalPurchases;

    @Field("total_rentals")
    private Integer totalRentals;

    @Field("total_contracts_signed")
    private Integer totalContractsSigned;
}
