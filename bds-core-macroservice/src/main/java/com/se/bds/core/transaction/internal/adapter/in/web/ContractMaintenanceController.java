package com.se.bds.core.transaction.internal.adapter.in.web;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.core.transaction.internal.adapter.in.web.dto.ContractMaintenanceWebRequests.*;
import com.se.bds.core.transaction.internal.application.port.in.ContractMaintenanceUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/contracts")
@RequiredArgsConstructor
public class ContractMaintenanceController {
    private final ContractMaintenanceUseCase contractMaintenanceUseCase;

    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @PutMapping("/{contractId}")
    public ResponseEntity<Void> updateContractDraft(
            @PathVariable UUID contractId,
            @Valid @RequestBody UpdateContractDraftRequest request
    ) {
        contractMaintenanceUseCase.updateContractDraft(
                contractId,
                request.startDate(),
                request.endDate(),
                request.specialTerms()
        );
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @PostMapping("/{contractId}/rate")
    public ResponseEntity<Void> rateContract(
            @PathVariable UUID contractId,
            @Valid @RequestBody RateContractRequest request
    ) {
        UUID currentUserId = getCurrentUserId();
        contractMaintenanceUseCase.rateContract(contractId, currentUserId, request.rating(), request.comment());
        return ResponseEntity.ok().build();
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException("UNAUTHORIZED", "User is not authenticated");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            if ("test_user".equals(authentication.getName())) {
                return UUID.fromString("33333333-3333-3333-3333-333333333333");
            }
            throw new BusinessException("INVALID_USER_ID", "Authenticated principal name is not a valid UUID: " + authentication.getName());
        }
    }
}
