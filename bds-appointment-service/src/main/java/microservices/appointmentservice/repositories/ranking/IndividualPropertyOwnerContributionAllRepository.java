package microservices.appointmentservice.repositories.ranking;

import microservices.appointmentservice.schemas.ranking.IndividualPropertyOwnerContributionAll;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IndividualPropertyOwnerContributionAllRepository
        extends MongoRepository<IndividualPropertyOwnerContributionAll, String> {

    Optional<IndividualPropertyOwnerContributionAll> findByOwnerId(UUID ownerId);
    java.util.List<IndividualPropertyOwnerContributionAll> findAllByOwnerIdIn(java.util.List<UUID> ownerIds);
}
