package com.se.bds.core.transaction.internal.application.command;

import com.se.bds.core.transaction.internal.domain.model.MainContractType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateDepositContractCommand(
        UUID propertyId,
        UUID customerId,
        MainContractType mainContractType,
        BigDecimal depositAmount,
        BigDecimal agreedPrice,
        LocalDate expectedSignDate,
        String note
) {
}
