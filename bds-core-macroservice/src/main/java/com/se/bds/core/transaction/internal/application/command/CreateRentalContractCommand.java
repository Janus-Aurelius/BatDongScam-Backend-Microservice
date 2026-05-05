package com.se.bds.core.transaction.internal.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateRentalContractCommand(
        UUID propertyId,
        UUID customerId,
        UUID depositContractId,
        BigDecimal monthlyRentAmount,
        BigDecimal securityDepositAmount,
        Integer durationMonths,
        Integer paymentCycleMonths,
        LocalDate expectedStartDate,
        String note
) {
}
