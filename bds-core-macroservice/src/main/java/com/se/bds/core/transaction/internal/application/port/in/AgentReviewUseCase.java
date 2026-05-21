package com.se.bds.core.transaction.internal.application.port.in;

import com.se.bds.core.transaction.internal.domain.model.AgentReview;

import java.util.List;
import java.util.UUID;

/**
 * Use case for agent reviews submitted by customers (US-030).
 */
public interface AgentReviewUseCase {

    /**
     * Summary of an agent's review statistics.
     */
    record AgentReviewSummary(
            UUID agentId,
            double averageRating,
            long totalReviews
    ) {}

    /**
     * Submit a review for an agent based on a completed/active contract.
     *
     * @param agentId    the agent being reviewed
     * @param customerId the customer submitting the review
     * @param contractId the contract this review is for
     * @param rating     the rating (1-5)
     * @param comment    optional comment
     * @return the persisted review
     */
    AgentReview submitReview(UUID agentId, UUID customerId, UUID contractId, short rating, String comment);

    /**
     * Get all reviews for a specific agent.
     */
    List<AgentReview> getReviewsForAgent(UUID agentId);

    /**
     * Get the review summary (average rating + total count) for an agent.
     */
    AgentReviewSummary getAgentReviewSummary(UUID agentId);
}
