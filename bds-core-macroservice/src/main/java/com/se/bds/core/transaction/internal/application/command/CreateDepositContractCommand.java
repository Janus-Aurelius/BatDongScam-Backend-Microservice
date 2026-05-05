package com.se.bds.core.transaction.internal.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateDepositContractCommand(
        UUID propertyId,
        UUID customerId,
        BigDecimal depositAmount,
        BigDecimal agreedPrice,
        LocalDate expectedSignDate,
        String note
) {
}
