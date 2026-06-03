package microservices.moderationservice.moderation.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import microservices.moderationservice.common.model.AbstractBaseEntity;
import com.se.bds.common.enums.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "violation_reports")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "violation_id", nullable = false)),
})
public class ViolationReport extends AbstractBaseEntity {
    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "related_entity_type", nullable = false)
    private ViolationReportedTypeEnum relatedEntityType;

    @Column(name = "related_entity_id", nullable = false)
    private UUID relatedEntityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type", nullable = false)
    private ViolationTypeEnum violationType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ViolationStatusEnum status;

    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_applied")
    private PenaltyAppliedEnum penaltyApplied;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ElementCollection
    @CollectionTable(name = "violation_evidence_files", joinColumns = @JoinColumn(name = "violation_id"))
    @Builder.Default
    private List<ViolationEvidence> evidenceList = new ArrayList<>();
}
