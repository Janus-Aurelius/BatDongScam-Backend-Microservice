package com.se.bds.core.property.internal.domain.model.strategy;

import com.se.bds.core.property.internal.domain.model.TransactionType;

import java.math.BigDecimal;

public interface FeeCalculationStrategy {

    boolean supports(TransactionType type);

    BigDecimal calculateCommissionRate();

    BigDecimal calculateServiceFee(BigDecimal priceAmount, BigDecimal area);
}
