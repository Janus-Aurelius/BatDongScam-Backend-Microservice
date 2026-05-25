package com.se.bds.core.transaction.internal.application.port.out;

import com.se.bds.core.transaction.internal.domain.model.EscrowHold;
import com.se.bds.core.transaction.internal.domain.model.EscrowStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository port for escrow hold persistence (US-028).
 */
public interface EscrowRepository {

    EscrowHold save(EscrowHold escrowHold);

    Optional<EscrowHold> findById(UUID escrowId);

    List<EscrowHold> findByContractId(UUID contractId);

    List<EscrowHold> findByContractIdAndStatus(UUID contractId, EscrowStatus status);

    boolean existsByContractIdAndStatus(UUID contractId, EscrowStatus status);
}
