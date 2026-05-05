package com.se.bds.core.transaction.internal.application.command;

import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePurchaseContractCommand(
        UUID propertyId,
        UUID customerId,
        UUID depositContractId,
        BigDecimal agreedPrice,
        BigDecimal advancePaymentAmount,
        LocalDate advancePaymentDeadline,
        LocalDate finalPaymentDeadline,
        String note
) {
}
