package com.se.bds.core.property.internal.application.command;

public record VerifyDocumentCommand(
        String status,
        String rejectionReason
) {
}
