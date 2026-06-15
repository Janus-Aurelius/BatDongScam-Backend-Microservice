package microservices.appointmentservice.repositories.ranking;

import microservices.appointmentservice.schemas.ranking.IndividualPropertyOwnerContributionMonth;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IndividualPropertyOwnerContributionMonthRepository
        extends MongoRepository<IndividualPropertyOwnerContributionMonth, String> {

    Optional<IndividualPropertyOwnerContributionMonth> findByOwnerIdAndMonthAndYear(UUID ownerId, Integer month, Integer year);
    java.util.List<IndividualPropertyOwnerContributionMonth> findAllByOwnerIdInAndMonthAndYear(java.util.List<UUID> ownerIds, Integer month, Integer year);
}
