package com.se100.bds.repositories.domains.violation;

import com.se100.bds.models.entities.violation.ViolationReport;
import com.se100.bds.utils.Constants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface ViolationRepository extends JpaRepository<ViolationReport, UUID>, JpaSpecificationExecutor<ViolationReport> {

    int countByStatus(Constants.ViolationStatusEnum status);

    @Query("SELECT COUNT(v) FROM ViolationReport v WHERE v.createdAt >= :startDate AND v.createdAt < :endDate")
    int countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}

