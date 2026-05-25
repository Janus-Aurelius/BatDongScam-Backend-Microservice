package com.se.bds.core.transaction.internal.adapter.out.persistence;

import com.se.bds.core.transaction.internal.domain.model.AgentReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaAgentReviewRepository extends JpaRepository<AgentReview, UUID> {

    Optional<AgentReview> findByContractId(UUID contractId);

    List<AgentReview> findByAgentId(UUID agentId);

    boolean existsByContractId(UUID contractId);

    @Query("SELECT AVG(CAST(r.rating as double)) FROM AgentReview r WHERE r.agentId = :agentId")
    Double getAverageRatingByAgentId(@Param("agentId") UUID agentId);

    long countByAgentId(UUID agentId);
}
