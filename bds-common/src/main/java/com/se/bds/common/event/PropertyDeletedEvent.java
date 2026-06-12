package com.se.bds.common.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record PropertyDeletedEvent(
        @NotNull UUID eventId,
        @NotNull Instant occurredAt,
        @NotNull UUID propertyId,
        @NotBlank String status
) {
}
