package microservices.moderationservice.moderation.repository.replica;

import microservices.moderationservice.moderation.entity.replica.PropertyReplica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PropertyReplicaRepository extends JpaRepository<PropertyReplica, UUID> {
}
