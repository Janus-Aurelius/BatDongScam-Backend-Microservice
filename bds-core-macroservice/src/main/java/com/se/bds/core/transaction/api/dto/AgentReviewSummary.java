package com.se.bds.core.transaction.api.dto;

import java.util.UUID;

public record AgentReviewSummary(
        UUID agentId,
        double averageRating,
        long totalReviews
) {}
