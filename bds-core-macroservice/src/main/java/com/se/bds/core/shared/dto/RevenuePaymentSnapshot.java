package com.se.bds.core.shared.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RevenuePaymentSnapshot(
        UUID paymentId,
        UUID contractId,
        UUID propertyId,
        String paymentType,
        BigDecimal amount,
        Instant completedAt
) {
}
