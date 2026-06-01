package com.se.bds.core.transaction.internal.application.port.in;

import com.se.bds.core.transaction.internal.domain.model.Contract;
import com.se.bds.core.transaction.internal.domain.model.ContractStatus;
import com.se.bds.core.transaction.internal.domain.model.ContractType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Ingress port for querying contracts (generic reads).
 */
public interface ContractQueryUseCase {

    /**
     * Fetch contract details by its unique identifier.
     *
     * @param contractId the contract ID
     * @return the Contract
     */
    Contract getContractById(UUID contractId);

    /**
     * Query a paginated list of contracts with dynamic filters.
     *
     * @param customerId   optional customer filter
     * @param agentId      optional agent filter
     * @param propertyId   optional property filter
     * @param status       optional status filter
     * @param contractType optional type filter (DEPOSIT, PURCHASE, RENTAL)
     * @param pageable     pagination parameters
     * @return a page of contracts
     */
    Page<Contract> getContracts(
            UUID customerId,
            UUID agentId,
            UUID propertyId,
            ContractStatus status,
            ContractType contractType,
            Pageable pageable
    );
}
