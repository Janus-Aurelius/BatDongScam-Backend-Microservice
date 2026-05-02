package com.se100.bds.repositories.domains.user;

import com.se100.bds.models.entities.user.SaleAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SaleAgentRepository extends JpaRepository<SaleAgent, UUID>, JpaSpecificationExecutor<SaleAgent> {
    List<SaleAgent> findAllByCreatedAtBefore(LocalDateTime createdAtBefore);
}

