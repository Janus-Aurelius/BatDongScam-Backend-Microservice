package com.se.bds.core.transaction.internal.adapter.in.web;

import com.se.bds.core.transaction.internal.adapter.in.web.dto.CancelDepositContractWebRequest;
import com.se.bds.core.transaction.internal.adapter.in.web.dto.CreateDepositContractWebRequest;
import com.se.bds.core.transaction.internal.adapter.in.web.mapper.TransactionWebMapper;
import com.se.bds.core.transaction.internal.application.command.CreateDepositContractCommand;
import com.se.bds.core.transaction.internal.application.port.in.DepositContractUseCase;
import com.se.bds.core.transaction.internal.domain.model.DepositContract;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/contracts/deposits")
@RequiredArgsConstructor
public class DepositContractController {
    private final DepositContractUseCase depositContractUseCase;
    private final TransactionWebMapper  transactionWebMapper;

    @PreAuthorize(("hasAnyRole('ADMIN', 'SALESAGENT')"))
    @PostMapping
    public ResponseEntity<DepositContract> createDepositContract(@Valid @RequestBody CreateDepositContractWebRequest createDepositContractWebRequest) {
        CreateDepositContractCommand command = transactionWebMapper.toCreateDepositContractCommand(createDepositContractWebRequest);
        return ResponseEntity.ok(depositContractUseCase.createDepositContract(command));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SALESAGENT')")
    @PostMapping("/{contractId}/approve")
    public ResponseEntity<DepositContract> approveDepositContract(@PathVariable UUID contractId)
    {
        return ResponseEntity.ok(depositContractUseCase.approveDepositContract(contractId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SALESAGENT')")
    @PostMapping("/{contractId}/paperwork-complete")
    public ResponseEntity<DepositContract> markPaperworkComplete(@PathVariable UUID contractId)
    {
        return ResponseEntity.ok(depositContractUseCase.markDepositContractPaperworkComplete(contractId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER','PROPERTY_OWNER')")
    @PostMapping("/{contractId}/cancel")
    public ResponseEntity<DepositContract> cancelDepositContract(@PathVariable UUID contractId,
                                                                 @Valid @RequestBody CancelDepositContractWebRequest cancelDepositContractWebRequest)
    {
        return ResponseEntity.ok(depositContractUseCase.cancelDepositContract(contractId, cancelDepositContractWebRequest.reason()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{contractId}/void")
    public ResponseEntity<DepositContract> voidDepositContract(@PathVariable UUID contractId)
    {
        return ResponseEntity.ok(depositContractUseCase.voidDepositContract(contractId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{contractId}")
    public ResponseEntity<Void> deleteDepositContract(@PathVariable UUID contractId)
    {
        depositContractUseCase.deleteDepositContract(contractId);
        return ResponseEntity.ok().build();
    }
}
