package com.se100.bds.models.entities.violation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.se100.bds.models.entities.AbstractBaseEntity;
import com.se100.bds.models.entities.property.Media;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.utils.Constants;
import jakarta.persistence.*;
import lombok.*;

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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_user", nullable = false)
    private User reporterUser;

    @Column(name = "related_entity_type", nullable = false)
    private Constants.ViolationReportedTypeEnum relatedEntityType;

    @Column(name = "related_entity_id", nullable = false)
    private UUID relatedEntityId;

    @Column(name = "violation_type", nullable = false)
    private Constants.ViolationTypeEnum violationType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "status")
    private Constants.ViolationStatusEnum status;

    @Column(name = "penalty_applied")
    private Constants.PenaltyAppliedEnum penaltyApplied;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @OneToMany(mappedBy = "violationReport", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Media> mediaList = new ArrayList<>();
}
