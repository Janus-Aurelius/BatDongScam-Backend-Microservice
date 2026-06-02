package microservices.appointmentservice.repositories.ranking;

import microservices.appointmentservice.schemas.ranking.IndividualSalesAgentPerformanceCareer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IndividualSalesAgentPerformanceCareerRepository
        extends MongoRepository<IndividualSalesAgentPerformanceCareer, String> {

    Optional<IndividualSalesAgentPerformanceCareer> findByAgentId(UUID agentId);
}
