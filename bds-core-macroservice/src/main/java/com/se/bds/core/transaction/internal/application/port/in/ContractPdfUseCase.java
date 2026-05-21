package com.se.bds.core.transaction.internal.application.port.in;

import java.util.UUID;

/**
 * Use case for generating and handling contract PDF documents (US-008).
 */
public interface ContractPdfUseCase {

    /**
     * Generates a PDF for the given contract, uploads it to file storage, and updates the contract status.
     * Asynchronous operation to prevent blocking.
     *
     * @param contractId the contract ID
     */
    void generateAndUploadContractPdf(UUID contractId);
}
