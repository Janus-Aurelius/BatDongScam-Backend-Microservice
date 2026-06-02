package com.se.bds.core.property.internal.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Media asset (image, video, document scan) attached to a property listing.
 *
 * <h3>Cross-Module Reference (replaced with UUID)</h3>
 * <ul>
 *   <li>{@code violationReportId} — references Violation module's ViolationReport
 *       (a media can optionally be evidence for a violation)</li>
 * </ul>
 */
@Entity
@Table(name = "media", schema = "property_catalog")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "media_id", nullable = false)
    private UUID id;

    // ── Intra-module relationship ──────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    // ── Cross-module reference (UUID only) ─────────────────────────────

    /** References Violation module's ViolationReport (nullable) */
    @Column(name = "violation_id")
    private UUID violationReportId;

    // ── Media details ──────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "document_type")
    private String documentType;

    // ── Audit fields ───────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
