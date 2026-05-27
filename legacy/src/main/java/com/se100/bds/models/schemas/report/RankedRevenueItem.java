package com.se100.bds.models.schemas.report;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a ranked revenue item with UUID and revenue amount, already sorted
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankedRevenueItem { // THIS IS THE FUCKING LAST TIME I VIBE CODE
    private UUID id;
    private BigDecimal revenue;
}

