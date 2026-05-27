package com.se100.bds.repositories.domains.mongo.ranking;

import com.se100.bds.models.schemas.ranking.IndividualPropertyOwnerContributionMonth;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IndividualPropertyOwnerContributionMonthRepository extends MongoRepository<IndividualPropertyOwnerContributionMonth, String> {
    /**
     * Find property owner contribution records by owner ID
     */
    List<IndividualPropertyOwnerContributionMonth> findByOwnerId(UUID ownerId);

    /**
     * Find property owner contribution record by owner ID, month and year
     */
    IndividualPropertyOwnerContributionMonth findByOwnerIdAndMonthAndYear(UUID ownerId, Integer month, Integer year);

    /**
     * Find all property owner contribution records by month and year
     */
    List<IndividualPropertyOwnerContributionMonth> findAllByMonthAndYear(Integer month, Integer year);
}
