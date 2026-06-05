package com.se361.iam_service.dto.response.ranking;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndividualCustomerPotentialAll {
    private UUID customerId;
    private Integer leadScore;
    private Integer leadPosition;
    private Integer viewingsRequested;
    private Integer viewingsAttended;
    private BigDecimal spending;
    private Integer totalPurchases;
    private Integer totalRentals;
    private Integer totalContractsSigned;
}
