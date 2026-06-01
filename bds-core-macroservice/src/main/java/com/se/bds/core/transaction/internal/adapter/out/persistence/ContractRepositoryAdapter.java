package com.se.bds.core.transaction.internal.adapter.out.persistence;

import com.se.bds.core.transaction.internal.application.port.out.ContractRepository;
import com.se.bds.core.transaction.internal.domain.model.Contract;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ContractRepositoryAdapter implements ContractRepository {

    private final JpaContractRepository jpaContractRepository;

    @Override
    public Optional<Contract> findById(UUID id) {
        return jpaContractRepository.findById(id);
    }

    @Override
    public Contract save(Contract contract) {
        return jpaContractRepository.save(contract);
    }

    @Override
    public long countSignedInMonth(int month, int year) {
        return jpaContractRepository.countSignedInMonth(month, year);
    }

    @Override
    public List<Contract> findContractsBySignedAtBetween(LocalDateTime start, LocalDateTime end) {
        return jpaContractRepository.findBySignedAtBetween(start, end);
    }

    @Override
    public List<Contract> findByPropertyId(UUID propertyId) {
        return jpaContractRepository.findByPropertyId(propertyId);
    }

    @Override
    public org.springframework.data.domain.Page<Contract> findAllWithFilters(
            UUID customerId,
            UUID agentId,
            UUID propertyId,
            com.se.bds.core.transaction.internal.domain.model.ContractStatus status,
            com.se.bds.core.transaction.internal.domain.model.ContractType contractType,
            org.springframework.data.domain.Pageable pageable) {
        return jpaContractRepository.findAllWithFilters(customerId, agentId, propertyId, status, contractType, pageable);
    }
}
