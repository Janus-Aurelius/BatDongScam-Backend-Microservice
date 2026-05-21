package com.se.bds.core.transaction.internal.adapter.in.web;

import com.se.bds.core.transaction.internal.adapter.in.web.dto.CreateRentalContractWebRequest;
import com.se.bds.core.transaction.internal.adapter.in.web.dto.SecurityDepositDecisionWebRequest;
import com.se.bds.core.transaction.internal.adapter.in.web.mapper.TransactionWebMapper;
import com.se.bds.core.transaction.internal.application.command.CreateRentalContractCommand;
import com.se.bds.core.transaction.internal.application.port.in.RentalContractUseCase;
import com.se.bds.core.transaction.internal.domain.model.RentalContract;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/contracts/rental")
@RequiredArgsConstructor
public class RentalContractController {
    private final RentalContractUseCase rentalContractUseCase;
    private final TransactionWebMapper transactionWebMapper;
    private final com.se.bds.core.transaction.internal.application.port.in.ContractPdfUseCase contractPdfUseCase;

    @PreAuthorize("hasAnyRole('ADMIN','SALEAGENT')")
    @PostMapping("/{contractId}/generate-pdf")
    public ResponseEntity<Void> generateContractPdf(@PathVariable UUID contractId) {
        contractPdfUseCase.generateAndUploadContractPdf(contractId);
        return ResponseEntity.accepted().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','SALEAGENT')")
    @PostMapping
    public ResponseEntity<RentalContract> createRentalContract(@Valid @RequestBody CreateRentalContractWebRequest createRentalContractWebRequest)
    {
        CreateRentalContractCommand createRentalContractCommand = transactionWebMapper.toCreateRentalContractCommand(createRentalContractWebRequest);
        return ResponseEntity.ok(rentalContractUseCase.createRentalContract(createRentalContractCommand));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SALEAGENT')")
    @PostMapping("/{contractId}/approve")
    public ResponseEntity<RentalContract> approveRentalContract(@PathVariable UUID contractId)
    {
        return ResponseEntity.ok(rentalContractUseCase.approveRentalContract(contractId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SALEAGENT')")
    @PostMapping("/{contractId}/paperwork-complete")
    public ResponseEntity<RentalContract> markPaperworkComplete(@PathVariable UUID contractId)
    {
        return ResponseEntity.ok(rentalContractUseCase.approveRentalContract(contractId));
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{contractId}/security-deposit-decision")
    public ResponseEntity<RentalContract> decideSecurityDeposit(
            @PathVariable UUID contractId,
            @Valid @RequestBody SecurityDepositDecisionWebRequest  securityDepositDecisionWebRequest
            )
    {    return ResponseEntity.ok(rentalContractUseCase.decideSecurityDeposit(contractId, securityDepositDecisionWebRequest.decision(), securityDepositDecisionWebRequest.deductionAmount(),securityDepositDecisionWebRequest.reason()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping ("/{contractId}/void")
    public ResponseEntity<RentalContract> voidRentalContract(@PathVariable UUID contractId)
    {
        return ResponseEntity.ok(rentalContractUseCase.voidRentalContract(contractId));
    }




}
