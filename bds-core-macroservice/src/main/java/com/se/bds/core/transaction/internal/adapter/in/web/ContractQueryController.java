package com.se.bds.core.transaction.internal.adapter.in.web;

import com.se.bds.core.transaction.internal.application.port.in.ContractQueryUseCase;
import com.se.bds.core.transaction.internal.domain.model.Contract;
import com.se.bds.core.transaction.internal.domain.model.ContractStatus;
import com.se.bds.core.transaction.internal.domain.model.ContractType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for generic contract query/GET requests.
 */
@RestController
@RequestMapping("/contracts")
@RequiredArgsConstructor
public class ContractQueryController {

    private final ContractQueryUseCase contractQueryUseCase;

    /**
     * Retrieve generic contract details by contract ID.
     * Accessible by participative roles: CUSTOMER, PROPERTY_OWNER, SALESAGENT, ADMIN.
     */
    @PreAuthorize("hasAnyRole('ADMIN','SALESAGENT','CUSTOMER','PROPERTY_OWNER')")
    @GetMapping("/{contractId}")
    public ResponseEntity<Contract> getContractById(@PathVariable UUID contractId) {
        return ResponseEntity.ok(contractQueryUseCase.getContractById(contractId));
    }

    /**
     * Get paginated and filtered contract workload lists.
     * Accessible by administrative/sales management roles: ADMIN, SALEAGENT.
     */
    @PreAuthorize("hasAnyRole('ADMIN','SALESAGENT')")
    @GetMapping
    public ResponseEntity<Page<Contract>> getContracts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) ContractType contractType
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<Contract> result = contractQueryUseCase.getContracts(
                customerId, agentId, propertyId, status, contractType, pageable);
        return ResponseEntity.ok(result);
    }
}
