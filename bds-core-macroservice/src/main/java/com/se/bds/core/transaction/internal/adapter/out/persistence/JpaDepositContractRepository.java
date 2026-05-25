package com.se.bds.core.transaction.internal.adapter.out.persistence;

import com.se.bds.core.transaction.internal.domain.model.DepositContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaDepositContractRepository extends JpaRepository<DepositContract, UUID> {
    @Query(
            "SELECT count(c) > 0 from DepositContract c where c.propertyId = :propertyId and c.status not in ('DRAFT','CANCELLED','COMPLETED')")
            boolean existsActiveForProperty(@Param("propertyId") UUID propertyId);

    @Query("SELECT c FROM DepositContract c WHERE c.propertyId = :propertyId "
            + "AND c.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<DepositContract> findActiveByPropertyId(@Param("propertyId") UUID propertyId);
}
