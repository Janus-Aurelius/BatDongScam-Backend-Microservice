package com.se.bds.core.transaction.internal.adapter.out.persistence;

import com.se.bds.core.transaction.internal.domain.model.PurchaseContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaPurchaseContractRepository extends JpaRepository<PurchaseContract, UUID> {
    @Query(
            "SELECT  count(c) > 0 from PurchaseContract c where c.propertyId = :propertyId and c.status not in ('DRAFT','CANCELLED','COMPLETED')"
    )
    boolean existsActiveForProperty(@Param("propertyId") UUID propertyId);

    @Query("SELECT c FROM PurchaseContract c WHERE c.propertyId = :propertyId "
            + "AND c.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<PurchaseContract> findActiveByPropertyId(@Param("propertyId") UUID propertyId);
}
