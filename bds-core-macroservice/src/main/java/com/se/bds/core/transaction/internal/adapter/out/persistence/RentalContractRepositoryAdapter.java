package com.se.bds.core.transaction.internal.adapter.out.persistence;


import com.se.bds.core.transaction.internal.application.port.out.RentalContractRepository;
import com.se.bds.core.transaction.internal.domain.model.RentalContract;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class RentalContractRepositoryAdapter implements RentalContractRepository {
    private final JpaRentalContractRepository repository;
    @Override public RentalContract save(RentalContract contract) { return repository.save(contract); }
    @Override public Optional<RentalContract> findById(UUID id) { return repository.findById(id); }
    @Override public void delete(RentalContract contract) { repository.delete(contract); }
    @Override public boolean existsActiveContractForProperty(UUID propertyId) { return repository.existsActiveForProperty(propertyId); }
    @Override public List<RentalContract> findActiveByPropertyId(UUID propertyId) { return repository.findActiveByPropertyId(propertyId); }
    @Override public List<RentalContract> findByPdfStatus(com.se.bds.core.transaction.internal.domain.model.PdfStatus pdfStatus) { return repository.findByPdfStatus(pdfStatus); }
    @Override public List<RentalContract> findByPdfUrl(String pdfUrl) { return repository.findByPdfUrl(pdfUrl); }
    @Override public List<RentalContract> findAllActive() { return repository.findAllActive(); }
}
