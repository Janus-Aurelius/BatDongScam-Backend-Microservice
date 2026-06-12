package com.se.bds.common.event;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record UserCreatedEvent(
        @NotNull UUID eventId,
        @NotNull Instant occurredAt,
        @NotNull UUID userId,
        @NotBlank String fullName,
        @Email String email,
        String phoneNumber,
        String avatarUrl,
        @NotBlank String role,
        @NotBlank String status,
        @NotNull Boolean active
) {
}
