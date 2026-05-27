package com.se.bds.core.transaction.internal.adapter.out.persistence;

import com.se.bds.core.transaction.internal.application.port.out.EscrowRepository;
import com.se.bds.core.transaction.internal.domain.model.EscrowHold;
import com.se.bds.core.transaction.internal.domain.model.EscrowStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EscrowRepositoryAdapter implements EscrowRepository {

    private final JpaEscrowRepository jpaEscrowRepository;

    @Override
    public EscrowHold save(EscrowHold escrowHold) {
        return jpaEscrowRepository.save(escrowHold);
    }

    @Override
    public Optional<EscrowHold> findById(UUID escrowId) {
        return jpaEscrowRepository.findById(escrowId);
    }

    @Override
    public List<EscrowHold> findByContractId(UUID contractId) {
        return jpaEscrowRepository.findByContractId(contractId);
    }

    @Override
    public List<EscrowHold> findByContractIdAndStatus(UUID contractId, EscrowStatus status) {
        return jpaEscrowRepository.findByContractIdAndStatus(contractId, status);
    }

    @Override
    public boolean existsByContractIdAndStatus(UUID contractId, EscrowStatus status) {
        return jpaEscrowRepository.existsByContractIdAndStatus(contractId, status);
    }
}
