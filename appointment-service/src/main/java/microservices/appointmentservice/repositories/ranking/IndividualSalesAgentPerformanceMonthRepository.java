package microservices.appointmentservice.repositories.ranking;

import microservices.appointmentservice.schemas.ranking.IndividualSalesAgentPerformanceMonth;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IndividualSalesAgentPerformanceMonthRepository
        extends MongoRepository<IndividualSalesAgentPerformanceMonth, String> {

    Optional<IndividualSalesAgentPerformanceMonth> findByAgentIdAndMonthAndYear(UUID agentId, Integer month, Integer year);
}
