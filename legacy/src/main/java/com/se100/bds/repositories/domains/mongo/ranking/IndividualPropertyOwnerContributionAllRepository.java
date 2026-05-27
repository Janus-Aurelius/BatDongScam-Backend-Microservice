package com.se100.bds.repositories.domains.mongo.ranking;

import com.se100.bds.models.schemas.ranking.IndividualPropertyOwnerContributionAll;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IndividualPropertyOwnerContributionAllRepository extends MongoRepository<IndividualPropertyOwnerContributionAll, String> {
    /**
     * Find property owner all-time contribution record by owner ID
     */
    IndividualPropertyOwnerContributionAll findByOwnerId(UUID ownerId);
}

