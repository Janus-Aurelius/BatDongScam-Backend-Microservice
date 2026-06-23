package com.se.bds.core.transaction.internal.application.service;

import com.se.bds.core.transaction.internal.application.port.out.RentalContractRepository;
import com.se.bds.core.transaction.internal.domain.model.PdfStatus;
import com.se.bds.core.transaction.internal.domain.model.RentalContract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Background scheduler that automatically retries failed PDF generations and Cloudinary uploads (US-008).
 * Scans for rental contracts marked as {@link PdfStatus#FAILED} and re-triggers the generation workflow.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PdfRetryScheduler {

    private final RentalContractRepository rentalContractRepository;
    private final ContractPdfService contractPdfService;

    @Scheduled(fixedDelay = 60000) // Check for failures every 60 seconds
    @SchedulerLock(name = "pdfRetryLock", lockAtMostFor = "55s", lockAtLeastFor = "10s")
    public void retryFailedPdfUploads() {
        log.info("[EVENT] Scanning for failed PDF contract uploads to retry...");
        List<RentalContract> failedContracts = rentalContractRepository.findByPdfUrl("PENDING_UPLOAD");
        if (!failedContracts.isEmpty()) {
            log.info("[EVENT] Found {} contracts with PENDING_UPLOAD PDF status. Retrying generations...", failedContracts.size());
            for (RentalContract contract : failedContracts) {
                try {
                    log.info("[EVENT] Re-attempting PDF generation/upload for contractId={}", contract.getId());
                    contractPdfService.generateAndUploadContractPdf(contract.getId());
                } catch (Exception e) {
                    log.error("[EVENT] Background retry attempt failed for contractId={}: {}", contract.getId(), e.getMessage());
                }
            }
        }
    }
}
