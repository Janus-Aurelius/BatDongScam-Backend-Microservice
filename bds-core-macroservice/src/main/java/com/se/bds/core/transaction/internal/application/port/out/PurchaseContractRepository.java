package com.se.bds.core.transaction.internal.application.port.out;

import com.se.bds.core.transaction.internal.domain.model.PurchaseContract;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseContractRepository {
    PurchaseContract save(PurchaseContract purchaseContract);
    Optional<PurchaseContract> findById(UUID id);
    boolean existsActiveContractForProperty(UUID propertyId);
    void delete(PurchaseContract purchaseContract);
    List<PurchaseContract> findActiveByPropertyId(UUID propertyId);
}
