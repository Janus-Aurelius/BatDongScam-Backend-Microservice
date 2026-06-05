package com.se.bds.core.property.internal.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record DocumentMetadataWebRequest(
        @NotNull UUID documentTypeId,
        String documentNumber,
        String documentName,
        LocalDate issueDate,
        LocalDate expiryDate,
        String issuingAuthority,
        @NotNull Integer fileIndex
) {
}
