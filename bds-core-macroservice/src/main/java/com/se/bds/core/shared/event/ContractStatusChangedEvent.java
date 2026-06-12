package com.se.bds.core.shared.event;

import com.se.bds.core.shared.ids.ContractId;

import java.time.Instant;
import java.util.UUID;

public record ContractStatusChangedEvent(
        ContractId contractId,
        String contractType,
        UUID propertyId,
        String oldStatus,
        String newStatus,
        UUID customerId,
        UUID ownerId,
        String propertyTitle,
        Instant occurredAt
) {
    public ContractStatusChangedEvent(ContractId contractId, String contractType, UUID propertyId, UUID customerId, String oldStatus, String newStatus, Instant occurredAt) {
        this(contractId, contractType, propertyId, oldStatus, newStatus, customerId, null, null, occurredAt);
    }
}
