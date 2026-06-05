package com.se.bds.core.property.internal.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyDocumentWebRequest(
        @NotBlank String status,
        String rejectionReason
) {
}
