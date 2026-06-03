package com.se.bds.common.event;

import java.time.Instant;
import java.util.UUID;

public record ViolationPenaltyAppliedEvent(
        UUID violationId,
        UUID reportedUserId,
        UUID reporterUserId,
        UUID reportedEntityId,
        String reportedEntityType,
        String violationType,
        String penaltyApplied,
        Instant timestamp
) {}
