package com.se.bds.core.transaction.internal.adapter.in.web;

import com.se.bds.core.transaction.internal.adapter.in.web.dto.CreatePurchaseContractWebRequest;
import com.se.bds.core.transaction.internal.adapter.in.web.mapper.TransactionWebMapper;
import com.se.bds.core.transaction.internal.application.command.CreatePurchaseContractCommand;
import com.se.bds.core.transaction.internal.application.port.in.PurchaseContractUseCase;
import com.se.bds.core.transaction.internal.domain.model.PurchaseContract;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import okhttp3.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/contracts/purchases")
@RequiredArgsConstructor
public class PurchaseContractController {
    private final PurchaseContractUseCase purchaseContractUseCase;
    private final TransactionWebMapper transactionWebMapper;

    @PreAuthorize("hasAnyRole('ADMIN','SALEAGENT')")
    @PostMapping
    public ResponseEntity<PurchaseContract> createPurchaseContract(@Valid @RequestBody CreatePurchaseContractWebRequest createPurchaseContractWebRequest) {
        CreatePurchaseContractCommand command = transactionWebMapper.toCreatePurchaseContractCommand(createPurchaseContractWebRequest);
        return ResponseEntity.ok(purchaseContractUseCase.createPurchaseContract(command));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SALEAGENT')")
    @PostMapping("/{contractId}/approve")
    public ResponseEntity<PurchaseContract> approvePurchaseContract(@PathVariable UUID contractId)
    {
        return ResponseEntity.ok(purchaseContractUseCase.approvePurchaseContract(contractId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SALEAGENT')")
    @PostMapping("/{contractId}/paperwork-compete")
    public ResponseEntity<PurchaseContract> markPaperworkComplete(@PathVariable UUID contractId)
    {
        return ResponseEntity.ok(purchaseContractUseCase.markPurchaseContractPaperworkComplete(contractId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SALEAGENT')")
    @PostMapping("/{contractId}/cancel")
    public ResponseEntity<PurchaseContract> cancelPurchaseContract(@PathVariable UUID contractId)
    {
        return ResponseEntity.ok(purchaseContractUseCase.cancelPurchaseContract(contractId));
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/{contractId}/void")
    public ResponseEntity<PurchaseContract> voidPurchaseContract (@PathVariable UUID contractId)
    {
        return ResponseEntity.ok(purchaseContractUseCase.voidPurchaseContract(contractId));
    }

}
