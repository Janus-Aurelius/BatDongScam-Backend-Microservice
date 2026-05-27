package com.se100.bds.models.entities.document;

import com.se100.bds.models.entities.AbstractBaseEntity;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.utils.Constants;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "identification_documents")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "document_id", nullable = false)),
})
public class IdentificationDocument extends AbstractBaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_type_id", nullable = false)
    private DocumentType documentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @Column(name = "document_number", nullable = false, length = 20)
    private String documentNumber;

    @Column(name = "document_name", nullable = false)
    private String documentName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "issuing_authority", length = 100)
    private String issuingAuthority;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status")
    private Constants.VerificationStatusEnum verificationStatus;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
}
