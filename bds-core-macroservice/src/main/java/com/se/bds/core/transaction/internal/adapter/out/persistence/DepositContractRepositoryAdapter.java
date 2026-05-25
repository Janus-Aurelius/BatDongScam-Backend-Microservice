package com.se.bds.core.transaction.internal.adapter.out.persistence;

import com.se.bds.core.transaction.internal.application.port.out.DepositContractRepository;
import com.se.bds.core.transaction.internal.domain.model.DepositContract;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DepositContractRepositoryAdapter implements DepositContractRepository {
    private final JpaDepositContractRepository jpaDepositContractRepository;

    @Override
    public DepositContract save(DepositContract depositContract) {
        return jpaDepositContractRepository.save(depositContract);
    }

    @Override
    public Optional<DepositContract> findById(UUID id)
    {
        return  jpaDepositContractRepository.findById(id);
    }

    @Override
    public void delete(DepositContract depositContract)
    {
        jpaDepositContractRepository.delete(depositContract);
    }

    @Override
    public boolean existsActiveContractForProperty(UUID propertyId)
    {
        return jpaDepositContractRepository.existsActiveForProperty(propertyId);
    }

    @Override
    public List<DepositContract> findActiveByPropertyId(UUID propertyId) {
        return jpaDepositContractRepository.findActiveByPropertyId(propertyId);
    }
}

