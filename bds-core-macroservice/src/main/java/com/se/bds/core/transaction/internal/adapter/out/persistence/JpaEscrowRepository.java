package com.se.bds.core.transaction.internal.adapter.out.persistence;

import com.se.bds.core.transaction.internal.domain.model.EscrowHold;
import com.se.bds.core.transaction.internal.domain.model.EscrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaEscrowRepository extends JpaRepository<EscrowHold, UUID> {

    List<EscrowHold> findByContractId(UUID contractId);

    List<EscrowHold> findByContractIdAndStatus(UUID contractId, EscrowStatus status);

    boolean existsByContractIdAndStatus(UUID contractId, EscrowStatus status);
}
