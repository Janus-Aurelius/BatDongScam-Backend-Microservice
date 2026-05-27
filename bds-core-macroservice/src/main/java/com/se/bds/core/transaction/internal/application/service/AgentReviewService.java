package com.se.bds.core.transaction.internal.application.service;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.message.discovery.MSG39;
import com.se.bds.common.message.validation.MSG12;
import com.se.bds.common.message.validation.MSG18;
import com.se.bds.core.transaction.internal.application.port.in.AgentReviewUseCase;
import com.se.bds.core.transaction.internal.application.port.out.AgentReviewRepository;
import com.se.bds.core.transaction.internal.application.port.out.ContractRepository;
import com.se.bds.core.transaction.internal.domain.model.AgentReview;
import com.se.bds.core.transaction.internal.domain.model.Contract;
import com.se.bds.core.transaction.internal.domain.model.ContractStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentReviewService implements AgentReviewUseCase {

    private final AgentReviewRepository agentReviewRepository;
    private final ContractRepository contractRepository;

    @Override
    @Transactional
    public AgentReview submitReview(UUID agentId, UUID customerId, UUID contractId, short rating, String comment) {
        log.info("[ACCOUNTS] Customer={} submitting review for agent={} on contractId={}", customerId, agentId, contractId);

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException(MSG18.CODE, MSG18.MESSAGE));

        // Security check: Only the customer who signed the contract can review
        if (!contract.getCustomerId().equals(customerId)) {
            throw new BusinessException(MSG12.CODE, "Only the contract customer can submit a review");
        }

        // Integrity check: Agent must match the contract agent
        if (contract.getAgentId() == null || !contract.getAgentId().equals(agentId)) {
            throw new BusinessException(MSG12.CODE, "The specified agent does not match the contract agent");
        }

        // Status check: Contract must be ACTIVE or COMPLETED
        if (contract.getStatus() != ContractStatus.ACTIVE && contract.getStatus() != ContractStatus.COMPLETED) {
            throw new BusinessException(MSG39.CODE, MSG39.MESSAGE);
        }

        // Idempotency: One review per contract
        if (agentReviewRepository.existsByContractId(contractId)) {
            throw new BusinessException(MSG12.CODE, "A review has already been submitted for this contract");
        }

        AgentReview review = AgentReview.builder()
                .agentId(agentId)
                .customerId(customerId)
                .contractId(contractId)
                .contractType(contract.getContractType())
                .rating(rating)
                .comment(comment)
                .build();

        review.validateRating();

        AgentReview saved = agentReviewRepository.save(review);
        log.info("[EVENT] Agent review submitted: reviewId={}, agentId={}, rating={}", saved.getId(), agentId, rating);

        // Trigger recalculation logging
        AgentReviewSummary summary = getAgentReviewSummary(agentId);
        log.info("[EVENT] Agent average rating recalculated: agentId={}, newAvg={}, totalReviews={}",
                agentId, summary.averageRating(), summary.totalReviews());

        return saved;
    }

    @Override
    public List<AgentReview> getReviewsForAgent(UUID agentId) {
        return agentReviewRepository.findByAgentId(agentId);
    }

    @Override
    public AgentReviewSummary getAgentReviewSummary(UUID agentId) {
        Double avg = agentReviewRepository.getAverageRatingByAgentId(agentId);
        long total = agentReviewRepository.countByAgentId(agentId);
        double averageRating = (avg != null) ? avg : 0.0;
        return new AgentReviewSummary(agentId, averageRating, total);
    }
}
