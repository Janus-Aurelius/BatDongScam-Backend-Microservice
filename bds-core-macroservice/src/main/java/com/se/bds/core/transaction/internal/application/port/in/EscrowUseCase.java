package com.se.bds.core.transaction.internal.application.port.in;

import com.se.bds.core.transaction.internal.domain.model.EscrowHold;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Use case for escrow protection operations (US-028).
 * Supports multi-stage payment holds across rental, deposit, and purchase contracts.
 */
public interface EscrowUseCase {

    /**
     * Creates a new escrow hold for a contract.
     */
    EscrowHold createEscrowHold(UUID contractId, UUID paymentId, BigDecimal amount, String description);

    /**
     * Confirms that funds have been received and the escrow is now actively held.
     */
    EscrowHold confirmEscrowHold(UUID escrowId);

    /**
     * Releases the full escrow amount to the property owner.
     */
    EscrowHold releaseToOwner(UUID escrowId, UUID adminUserId, String reason);

    /**
     * Returns the full escrow amount to the customer.
     */
    EscrowHold returnToCustomer(UUID escrowId, UUID adminUserId, String reason);

    /**
     * Applies a partial deduction and returns the remaining balance to the customer.
     */
    EscrowHold partialRelease(UUID escrowId, BigDecimal deductionAmount, UUID adminUserId, String reason);

    /**
     * Forfeits the entire escrow amount due to contract breach.
     */
    EscrowHold forfeit(UUID escrowId, UUID adminUserId, String reason);

    /**
     * Retrieves all escrow holds for a contract.
     */
    List<EscrowHold> getEscrowHoldsForContract(UUID contractId);
}
