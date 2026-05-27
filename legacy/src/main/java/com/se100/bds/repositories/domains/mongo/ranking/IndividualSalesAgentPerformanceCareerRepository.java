package com.se100.bds.repositories.domains.mongo.ranking;

import com.se100.bds.models.schemas.ranking.IndividualSalesAgentPerformanceCareer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IndividualSalesAgentPerformanceCareerRepository extends MongoRepository<IndividualSalesAgentPerformanceCareer, String> {
    /**
     * Find agent career performance record by agent ID
     */
    IndividualSalesAgentPerformanceCareer findByAgentId(UUID agentId);
}

