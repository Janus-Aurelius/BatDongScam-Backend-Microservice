package com.se.bds.core.transaction.internal.adapter.in.web;

import com.se.bds.core.transaction.internal.application.port.in.EscrowUseCase;
import com.se.bds.core.transaction.internal.domain.model.EscrowHold;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/escrow")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EscrowController {

    private final EscrowUseCase escrowUseCase;

    @PostMapping("/hold")
    public ResponseEntity<EscrowHold> createEscrowHold(
            @RequestParam("contractId") UUID contractId,
            @RequestParam("paymentId") UUID paymentId,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("description") String description) {
        
        EscrowHold escrow = escrowUseCase.createEscrowHold(contractId, paymentId, amount, description);
        return ResponseEntity.ok(escrow);
    }

    @PostMapping("/{escrowId}/confirm")
    public ResponseEntity<EscrowHold> confirmEscrowHold(@PathVariable("escrowId") UUID escrowId) {
        EscrowHold escrow = escrowUseCase.confirmEscrowHold(escrowId);
        return ResponseEntity.ok(escrow);
    }

    @PostMapping("/{escrowId}/release-to-owner")
    public ResponseEntity<EscrowHold> releaseToOwner(
            @PathVariable("escrowId") UUID escrowId,
            @RequestParam("adminUserId") UUID adminUserId,
            @RequestParam("reason") String reason) {
        
        EscrowHold escrow = escrowUseCase.releaseToOwner(escrowId, adminUserId, reason);
        return ResponseEntity.ok(escrow);
    }

    @PostMapping("/{escrowId}/return-to-customer")
    public ResponseEntity<EscrowHold> returnToCustomer(
            @PathVariable("escrowId") UUID escrowId,
            @RequestParam("adminUserId") UUID adminUserId,
            @RequestParam("reason") String reason) {
        
        EscrowHold escrow = escrowUseCase.returnToCustomer(escrowId, adminUserId, reason);
        return ResponseEntity.ok(escrow);
    }

    @PostMapping("/{escrowId}/partial-release")
    public ResponseEntity<EscrowHold> partialRelease(
            @PathVariable("escrowId") UUID escrowId,
            @RequestParam("deductionAmount") BigDecimal deductionAmount,
            @RequestParam("adminUserId") UUID adminUserId,
            @RequestParam("reason") String reason) {
        
        EscrowHold escrow = escrowUseCase.partialRelease(escrowId, deductionAmount, adminUserId, reason);
        return ResponseEntity.ok(escrow);
    }

    @PostMapping("/{escrowId}/forfeit")
    public ResponseEntity<EscrowHold> forfeit(
            @PathVariable("escrowId") UUID escrowId,
            @RequestParam("adminUserId") UUID adminUserId,
            @RequestParam("reason") String reason) {
        
        EscrowHold escrow = escrowUseCase.forfeit(escrowId, adminUserId, reason);
        return ResponseEntity.ok(escrow);
    }

    @GetMapping("/contract/{contractId}")
    public ResponseEntity<List<EscrowHold>> getEscrowHoldsForContract(@PathVariable("contractId") UUID contractId) {
        List<EscrowHold> holds = escrowUseCase.getEscrowHoldsForContract(contractId);
        return ResponseEntity.ok(holds);
    }
}
