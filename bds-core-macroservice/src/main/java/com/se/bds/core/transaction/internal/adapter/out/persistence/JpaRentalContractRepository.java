package com.se.bds.core.transaction.internal.adapter.out.persistence;

import com.se.bds.core.transaction.internal.domain.model.RentalContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaRentalContractRepository extends JpaRepository<RentalContract, UUID> {
    @Query(
            "select count (c) > 0 from RentalContract c where c.propertyId = :propertyId and c.status not in ('DRAFT','CANCELLED','COMPLETED')"
    )
    boolean existsActiveForProperty(@Param("propertyId") UUID propertyId);

    @Query("SELECT c FROM RentalContract c WHERE c.propertyId = :propertyId "
            + "AND c.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<RentalContract> findActiveByPropertyId(@Param("propertyId") UUID propertyId);

    List<RentalContract> findByPdfStatus(com.se.bds.core.transaction.internal.domain.model.PdfStatus pdfStatus);

    List<RentalContract> findByPdfUrl(String pdfUrl);

    @Query("SELECT c FROM RentalContract c WHERE c.status = 'ACTIVE'")
    List<RentalContract> findAllActive();
}
