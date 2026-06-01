package com.se.bds.core.transaction.internal.application.port.out;

import com.se.bds.core.transaction.internal.domain.model.RentalContract;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RentalContractRepository {
    RentalContract save(RentalContract rentalContract);
    Optional<RentalContract> findById(UUID id);
    boolean existsActiveContractForProperty(UUID propertyId);
    void delete(RentalContract rentalContract);
    List<RentalContract> findActiveByPropertyId(UUID propertyId);
    List<RentalContract> findByPdfStatus(com.se.bds.core.transaction.internal.domain.model.PdfStatus pdfStatus);
    List<RentalContract> findByPdfUrl(String pdfUrl);
    List<RentalContract> findAllActive();
}
