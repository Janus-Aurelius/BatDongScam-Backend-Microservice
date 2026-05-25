package com.se.bds.core.transaction.internal.adapter.out.persistence;

import com.se.bds.core.transaction.internal.application.port.out.DepositContractRepository;
import com.se.bds.core.transaction.internal.application.port.out.PurchaseContractRepository;
import com.se.bds.core.transaction.internal.domain.model.PurchaseContract;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PurchaseContractRepositoryAdapter implements PurchaseContractRepository {
    private final JpaPurchaseContractRepository jpaPurchaseContractRepository;

    @Override
    public PurchaseContract save(PurchaseContract purchaseContract)
    {
        return jpaPurchaseContractRepository.save(purchaseContract);
    }

    @Override
    public Optional<PurchaseContract> findById(UUID id)
    {

        return jpaPurchaseContractRepository.findById(id);
    }

    @Override
    public void delete(PurchaseContract purchaseContract)
    {

        jpaPurchaseContractRepository.delete(purchaseContract);
    }

    @Override
    public boolean existsActiveContractForProperty(UUID propertyId)
    {
        return jpaPurchaseContractRepository.existsActiveForProperty(propertyId);
    }

    @Override
    public List<PurchaseContract> findActiveByPropertyId(UUID propertyId) {
        return jpaPurchaseContractRepository.findActiveByPropertyId(propertyId);
    }
}
