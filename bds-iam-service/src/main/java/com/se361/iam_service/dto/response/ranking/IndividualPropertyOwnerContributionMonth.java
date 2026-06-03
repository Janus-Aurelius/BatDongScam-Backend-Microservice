package com.se361.iam_service.dto.response.ranking;

import com.se.bds.common.enums.ContributionTierEnum;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndividualPropertyOwnerContributionMonth {
    private UUID ownerId;
    private Integer month;
    private Integer year;
    private Integer contributionPoint;
    private ContributionTierEnum contributionTier;
    private Integer rankingPosition;
    private BigDecimal monthContributionValue;
    private Integer monthTotalProperties;
    private Integer monthTotalForSales;
    private Integer monthTotalForRents;
    private Integer monthTotalPropertiesSold;
    private Integer monthTotalPropertiesRented;
}
