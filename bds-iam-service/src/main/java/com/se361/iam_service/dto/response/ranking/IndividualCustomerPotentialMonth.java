package com.se361.iam_service.dto.response.ranking;

import com.se.bds.common.enums.CustomerTierEnum;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndividualCustomerPotentialMonth {
    private UUID customerId;
    private Integer month;
    private Integer year;
    private Integer leadScore;
    private CustomerTierEnum customerTier;
    private Integer leadPosition;
    private Integer monthViewingsRequested;
    private Integer monthViewingAttended;
    private BigDecimal monthSpending;
    private Integer monthPurchases;
    private Integer monthRentals;
    private Integer monthContractsSigned;
}
