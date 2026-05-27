package com.se.bds.core.transaction.internal.application.port.out;

import com.se.bds.core.transaction.internal.domain.model.DepositContract;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepositContractRepository {
    DepositContract save(DepositContract depositContract);
    Optional<DepositContract> findById(UUID id);
    void delete(DepositContract depositContract);
    boolean existsActiveContractForProperty(UUID property);
    List<DepositContract> findActiveByPropertyId(UUID propertyId);
}
