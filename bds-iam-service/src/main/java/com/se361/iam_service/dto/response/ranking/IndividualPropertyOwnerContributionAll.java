package com.se361.iam_service.dto.response.ranking;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndividualPropertyOwnerContributionAll {
    private UUID ownerId;
    private Integer contributionPoint;
    private Integer rankingPosition;
    private BigDecimal contributionValue;
    private Integer totalProperties;
    private Integer totalPropertiesSold;
    private Integer totalPropertiesRented;
}
