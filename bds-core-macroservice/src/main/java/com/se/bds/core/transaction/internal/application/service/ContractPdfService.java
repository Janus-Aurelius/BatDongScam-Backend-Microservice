package com.se.bds.core.transaction.internal.application.service;

import com.se.bds.core.transaction.internal.application.port.in.ContractPdfUseCase;
import com.se.bds.core.transaction.internal.application.port.out.FileStoragePort;
import com.se.bds.core.transaction.internal.application.port.out.PdfGeneratorPort;
import com.se.bds.core.transaction.internal.application.port.out.RentalContractRepository;
import com.se.bds.core.transaction.internal.domain.model.PdfStatus;
import com.se.bds.core.transaction.internal.domain.model.RentalContract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractPdfService implements ContractPdfUseCase {

    private final RentalContractRepository rentalContractRepository;
    private final PdfGeneratorPort pdfGeneratorPort;
    private final FileStoragePort fileStoragePort;

    @Override
    @Async("pdfGenerationExecutor")
    @Transactional
    public void generateAndUploadContractPdf(UUID contractId) {
        log.info("[ACCOUNTS] Initiated asynchronous PDF generation for rental contractId={}", contractId);

        RentalContract contract = rentalContractRepository.findById(contractId).orElse(null);
        if (contract == null) {
            log.error("[EVENT] Contract not found: {}", contractId);
            return;
        }

        try {
            // 1. Prepare template data map
            Map<String, Object> data = new HashMap<>();
            data.put("Contract ID", contract.getId());
            data.put("Property ID", contract.getPropertyId());
            data.put("Customer ID", contract.getCustomerId());
            data.put("Agent ID", contract.getAgentId() != null ? contract.getAgentId() : "N/A");
            data.put("Contract Number", contract.getContractNumber() != null ? contract.getContractNumber() : "PENDING");
            data.put("Start Date", contract.getStartDate());
            data.put("End Date", contract.getEndDate() != null ? contract.getEndDate() : "N/A");
            data.put("Monthly Rent Amount", contract.getMonthlyRentAmount());
            data.put("Security Deposit Amount", contract.getSecurityDepositAmount() != null ? contract.getSecurityDepositAmount() : "0.00");
            data.put("Security Deposit Status", contract.getSecurityDepositStatus() != null ? contract.getSecurityDepositStatus().name() : "N/A");

            // 2. Generate PDF bytes using swappable PDF engine (OpenPDF)
            // TODO: verify PDF template rendering with real contract data
            byte[] pdfBytes = pdfGeneratorPort.generatePdf("rental_contract_template", data);

            // 3. Upload raw file bytes to Swappable storage (Cloudinary)
            // TODO: integrate with Cloudinary for PDF upload
            String pdfUrl = fileStoragePort.uploadFile(pdfBytes, "contracts", "rental_contract_" + contractId);

            // 4. Update the contract fields
            contract.setPdfUrl(pdfUrl);
            contract.setPdfStatus(PdfStatus.SUCCESS);
            rentalContractRepository.save(contract);

            log.info("[EVENT] Contract PDF generated and uploaded successfully: contractId={}, pdfUrl={}", contractId, pdfUrl);

        } catch (Exception e) {
            log.error("[EVENT] Contract PDF generation or upload FAILED: contractId={}, error={}", contractId, e.getMessage(), e);

            // Degradation Tactic: mark status as FAILED so fallback background scheduler can retry
            contract.setPdfStatus(PdfStatus.FAILED);
            rentalContractRepository.save(contract);
        }
    }
}
