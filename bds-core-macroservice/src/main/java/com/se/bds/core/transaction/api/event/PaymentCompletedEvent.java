package com.se.bds.core.transaction.api.event;

import com.se.bds.core.shared.ids.ContractId;
import com.se.bds.core.shared.ids.PaymentId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(
        PaymentId paymentId,
        ContractId contractId,
        UUID propertyId,
        String paymentType,
        BigDecimal amount,
        UUID payerId,
        Instant occurredAt
) {
}
