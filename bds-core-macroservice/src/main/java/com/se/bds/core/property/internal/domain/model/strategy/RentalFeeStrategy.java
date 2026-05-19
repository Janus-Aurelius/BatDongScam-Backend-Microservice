package com.se.bds.core.property.internal.domain.model.strategy;

import com.se.bds.core.property.internal.domain.model.TransactionType;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class RentalFeeStrategy implements FeeCalculationStrategy {
    @Override
    public boolean supports(TransactionType type) {
        return type == TransactionType.RENTAL;
    }

    @Override
    public BigDecimal calculateCommissionRate() {
        return new BigDecimal("0.50");
    }

    @Override
    public BigDecimal calculateServiceFee(BigDecimal priceAmount, BigDecimal area) {
        return new BigDecimal("500000.00");
    }
}
