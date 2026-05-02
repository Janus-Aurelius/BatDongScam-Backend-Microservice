package com.se100.bds.repositories.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentProjection(
    UUID id,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    UUID documentTypeId,
    String documentTypeName,
    String documentNumber,
    String documentName,
    String filePath,
    LocalDate issueDate,
    LocalDate expiryDate,
    String issuingAuthority,
    String verificationStatus,
    LocalDateTime verifiedAt,
    String rejectionReason
) {}

