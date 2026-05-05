package com.se.bds.core.transaction.api.event;

import com.se.bds.core.shared.ids.ContractId;

import java.time.Instant;
import java.util.UUID;

public record ContractStatusChangedEvent(
        ContractId contractId,
        String contractType,
        UUID propertyId,
        String oldStatus,
        String newStatus,
        Instant occurredAt
) {
}
