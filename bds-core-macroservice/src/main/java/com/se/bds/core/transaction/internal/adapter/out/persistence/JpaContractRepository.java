package com.se.bds.core.transaction.internal.adapter.out.persistence;

import com.se.bds.core.transaction.internal.domain.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JpaContractRepository extends JpaRepository<Contract, UUID> {

    @Query("SELECT COUNT(c) FROM Contract c WHERE EXTRACT(MONTH FROM c.signedAt) = :month AND EXTRACT(YEAR FROM c.signedAt) = :year AND c.signedAt IS NOT NULL")
    long countSignedInMonth(@Param("month") int month, @Param("year") int year);

    List<Contract> findBySignedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Contract> findByPropertyId(UUID propertyId);

    @Query("""
    SELECT c FROM Contract c
    WHERE (:customerId IS NULL OR c.customerId = :customerId)
      AND (:agentId IS NULL OR c.agentId = :agentId)
      AND (:propertyId IS NULL OR c.propertyId = :propertyId)
      AND (:status IS NULL OR c.status = :status)
      AND (:contractType IS NULL OR c.contractType = :contractType)
    """)
    org.springframework.data.domain.Page<Contract> findAllWithFilters(
            @Param("customerId") UUID customerId,
            @Param("agentId") UUID agentId,
            @Param("propertyId") UUID propertyId,
            @Param("status") com.se.bds.core.transaction.internal.domain.model.ContractStatus status,
            @Param("contractType") com.se.bds.core.transaction.internal.domain.model.ContractType contractType,
            org.springframework.data.domain.Pageable pageable
    );
}
