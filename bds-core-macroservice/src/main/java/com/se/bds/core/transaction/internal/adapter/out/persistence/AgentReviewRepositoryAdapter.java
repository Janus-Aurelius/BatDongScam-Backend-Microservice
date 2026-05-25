package com.se.bds.core.transaction.internal.adapter.out.persistence;

import com.se.bds.core.transaction.internal.application.port.out.AgentReviewRepository;
import com.se.bds.core.transaction.internal.domain.model.AgentReview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AgentReviewRepositoryAdapter implements AgentReviewRepository {

    private final JpaAgentReviewRepository jpaAgentReviewRepository;

    @Override
    public AgentReview save(AgentReview review) {
        return jpaAgentReviewRepository.save(review);
    }

    @Override
    public Optional<AgentReview> findById(UUID reviewId) {
        return jpaAgentReviewRepository.findById(reviewId);
    }

    @Override
    public Optional<AgentReview> findByContractId(UUID contractId) {
        return jpaAgentReviewRepository.findByContractId(contractId);
    }

    @Override
    public List<AgentReview> findByAgentId(UUID agentId) {
        return jpaAgentReviewRepository.findByAgentId(agentId);
    }

    @Override
    public boolean existsByContractId(UUID contractId) {
        return jpaAgentReviewRepository.existsByContractId(contractId);
    }

    @Override
    public Double getAverageRatingByAgentId(UUID agentId) {
        return jpaAgentReviewRepository.getAverageRatingByAgentId(agentId);
    }

    @Override
    public long countByAgentId(UUID agentId) {
        return jpaAgentReviewRepository.countByAgentId(agentId);
    }
}
