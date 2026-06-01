package com.se.bds.core.transaction.internal.application.port.out;

import com.se.bds.core.transaction.internal.domain.model.Contract;
import java.util.Optional;
import java.util.UUID;

/**
 * Polymorphic repository port for Contract entities (US-010, US-011, US-028, US-030).
 */
public interface ContractRepository {

    Optional<Contract> findById(UUID id);

    Contract save(Contract contract);

    long countSignedInMonth(int month, int year);

    java.util.List<Contract> findContractsBySignedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    java.util.List<Contract> findByPropertyId(UUID propertyId);

    org.springframework.data.domain.Page<Contract> findAllWithFilters(
            UUID customerId,
            UUID agentId,
            UUID propertyId,
            com.se.bds.core.transaction.internal.domain.model.ContractStatus status,
            com.se.bds.core.transaction.internal.domain.model.ContractType contractType,
            org.springframework.data.domain.Pageable pageable
    );
}
