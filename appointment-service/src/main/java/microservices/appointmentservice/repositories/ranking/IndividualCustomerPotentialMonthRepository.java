package microservices.appointmentservice.repositories.ranking;

import microservices.appointmentservice.schemas.ranking.IndividualCustomerPotentialMonth;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IndividualCustomerPotentialMonthRepository
        extends MongoRepository<IndividualCustomerPotentialMonth, String> {

    Optional<IndividualCustomerPotentialMonth> findByCustomerIdAndMonthAndYear(UUID customerId, Integer month, Integer year);
}
