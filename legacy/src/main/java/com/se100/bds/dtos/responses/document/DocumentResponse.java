package com.se100.bds.dtos.responses.document;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class DocumentResponse extends AbstractBaseDataResponse {
    private UUID documentTypeId;
    private String documentTypeName;
    private String documentNumber;
    private String documentName;
    private String filePath;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String issuingAuthority;
    private String verificationStatus;
    private LocalDateTime verifiedAt;
    private String rejectionReason;
}

