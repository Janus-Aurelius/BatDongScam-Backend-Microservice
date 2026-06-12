package microservices.moderationservice.moderation.repository;

import microservices.moderationservice.moderation.entity.PropertyReplica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyReplicaRepository extends JpaRepository<PropertyReplica, UUID> {

    Optional<PropertyReplica> findByPropertyIdAndDeletedFalse(UUID propertyId);
}
