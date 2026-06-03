package com.se.bds.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID paymentId,
        UUID contractId,
        UUID propertyId,
        String paymentType,
        BigDecimal amount,
        UUID payerUserId,
        Instant timestamp
) {}
