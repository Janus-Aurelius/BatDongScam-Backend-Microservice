package com.se100.bds.dtos.requests.property;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO containing metadata for uploading an identification document.
 * Each document upload should have associated metadata to properly categorize it.
 */
@Getter
@Setter
@NoArgsConstructor
public class DocumentUploadInfo {
    /**
     * The document type ID (required).
     * Must match one of the document types in the system.
     */
    @NotNull(message = "Document type ID is required")
    private UUID documentTypeId;

    /**
     * Optional document number (e.g., certificate number, license number).
     * If not provided, will be auto-generated.
     */
    private String documentNumber;

    /**
     * Optional custom document name.
     * If not provided, will use the uploaded file's original filename.
     */
    private String documentName;

    /**
     * Issue date of the document.
     */
    private LocalDate issueDate;

    /**
     * Expiry date of the document.
     */
    private LocalDate expiryDate;

    /**
     * Authority that issued the document.
     */
    private String issuingAuthority;

    /**
     * Index of the corresponding file in the documents MultipartFile array.
     * This links the metadata to the actual uploaded file.
     */
    @NotNull(message = "File index is required")
    private Integer fileIndex;
}

