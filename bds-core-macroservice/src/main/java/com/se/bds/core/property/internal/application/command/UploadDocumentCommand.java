package com.se.bds.core.property.internal.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record UploadDocumentCommand(
        UUID documentTypeId,
        String documentNumber,
        String documentName,
        LocalDate issueDate,
        LocalDate expiryDate,
        String issuingAuthority,
        Integer fileIndex
) {
}
