package com.se.bds.core.property.internal.domain.model.strategy;

import com.se.bds.core.property.internal.domain.model.TransactionType;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class SaleFeeStrategy implements FeeCalculationStrategy{
    @Override
    public boolean supports(TransactionType type) {
        return type == TransactionType.SALE;
    }

    @Override
    public BigDecimal calculateCommissionRate() {
        return new BigDecimal("0.05");
    }

    @Override
    public BigDecimal calculateServiceFee(BigDecimal priceAmount, BigDecimal area) {
        return priceAmount.multiply(new BigDecimal("0.001"));
    }
}
