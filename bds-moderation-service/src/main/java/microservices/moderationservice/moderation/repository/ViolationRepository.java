package microservices.moderationservice.moderation.repository;

import microservices.moderationservice.common.Constants;
import microservices.moderationservice.moderation.entity.ViolationReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ViolationRepository extends JpaRepository<ViolationReport, UUID>, JpaSpecificationExecutor<ViolationReport> {
    int countByStatus(Constants.ViolationStatusEnum status);
    List<ViolationReport> findByRelatedEntityId(UUID relatedEntityId);

    @Query("SELECT COUNT(v) FROM ViolationReport v WHERE v.createdAt >= :startDate AND v.createdAt < :endDate")
    int countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
