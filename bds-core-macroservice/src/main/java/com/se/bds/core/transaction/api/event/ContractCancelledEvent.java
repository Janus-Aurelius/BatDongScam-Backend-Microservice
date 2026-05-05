package com.se.bds.core.transaction.api.event;

import com.se.bds.core.shared.enums.Role;
import com.se.bds.core.shared.ids.ContractId;

import java.time.Instant;
import java.util.UUID;

public record ContractCancelledEvent(
        ContractId contractId,
        String contractType,
        UUID propertyId,
        Role cancelledBy,
        String cancellationReason,
        Instant occurredAt
) {
}
