package com.se.bds.core.transaction.internal.application.port.out;

import com.se.bds.core.transaction.internal.domain.model.AgentReview;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository port for agent review persistence (US-030).
 */
public interface AgentReviewRepository {

    AgentReview save(AgentReview review);

    Optional<AgentReview> findById(UUID reviewId);

    Optional<AgentReview> findByContractId(UUID contractId);

    List<AgentReview> findByAgentId(UUID agentId);

    boolean existsByContractId(UUID contractId);

    /**
     * @return the average rating for a given agent, or null if no reviews exist
     */
    Double getAverageRatingByAgentId(UUID agentId);

    long countByAgentId(UUID agentId);
}
