package microservices.appointmentservice.repositories.ranking;

import microservices.appointmentservice.schemas.ranking.IndividualCustomerPotentialAll;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IndividualCustomerPotentialAllRepository
        extends MongoRepository<IndividualCustomerPotentialAll, String> {

    Optional<IndividualCustomerPotentialAll> findByCustomerId(UUID customerId);
    java.util.List<IndividualCustomerPotentialAll> findAllByCustomerIdIn(java.util.List<UUID> customerIds);
}
