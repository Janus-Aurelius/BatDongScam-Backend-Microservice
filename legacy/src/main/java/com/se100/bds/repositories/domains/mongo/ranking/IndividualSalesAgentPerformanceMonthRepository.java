package com.se100.bds.repositories.domains.mongo.ranking;

import com.se100.bds.models.schemas.ranking.IndividualSalesAgentPerformanceMonth;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IndividualSalesAgentPerformanceMonthRepository extends MongoRepository<IndividualSalesAgentPerformanceMonth, String> {
    /**
     * Find agent performance records by agent ID
     */
    List<IndividualSalesAgentPerformanceMonth> findByAgentId(UUID agentId);

    /**
     * Find agent performance record by agent ID, month and year
     */
    IndividualSalesAgentPerformanceMonth findByAgentIdAndMonthAndYear(UUID agentId, Integer month, Integer year);

    /**
     * Find all agent performance records for a specific month and year
     */
    List<IndividualSalesAgentPerformanceMonth> findByMonthAndYear(Integer month, Integer year);

    Long countByAgentId(UUID agentId);

    List<IndividualSalesAgentPerformanceMonth> findAllByMonthAndYear(Integer month, Integer year);
}

