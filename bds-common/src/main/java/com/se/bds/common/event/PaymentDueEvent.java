package com.se.bds.common.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentDueEvent(
        UUID recipientUserId,
        UUID paymentId,
        UUID contractId,
        UUID propertyId,
        BigDecimal amount,
        LocalDate dueDate,
        Integer installmentNumber
) {}
