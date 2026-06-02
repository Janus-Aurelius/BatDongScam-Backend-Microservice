package microservices.moderationservice.moderation.mapper;

import org.springframework.stereotype.Component;
import microservices.moderationservice.moderation.dto.response.ViolationAdminDetails;
import microservices.moderationservice.moderation.dto.response.ViolationAdminItem;
import microservices.moderationservice.moderation.dto.response.ViolationUserDetails;
import microservices.moderationservice.moderation.dto.response.ViolationUserItem;
import microservices.moderationservice.moderation.entity.ViolationReport;

import java.util.List;

@Component
public class ViolationMapper {
    public ViolationAdminItem toAdminItem(ViolationReport violation) {
        return ViolationAdminItem.builder()
                .id(violation.getId())
                .createdAt(violation.getCreatedAt())
                .updatedAt(violation.getUpdatedAt())
                .reporterId(violation.getReporterId())
                .relatedEntityType(violation.getRelatedEntityType())
                .relatedEntityId(violation.getRelatedEntityId())
                .violationType(violation.getViolationType())
                .status(violation.getStatus())
                .description(violation.getDescription())
                .reportedAt(violation.getCreatedAt())
                .build();
    }

    public ViolationUserItem toUserItem(ViolationReport violation) {
        return ViolationUserItem.builder()
                .id(violation.getId())
                .createdAt(violation.getCreatedAt())
                .updatedAt(violation.getUpdatedAt())
                .reporterId(violation.getReporterId())
                .relatedEntityType(violation.getRelatedEntityType())
                .relatedEntityId(violation.getRelatedEntityId())
                .violationType(violation.getViolationType())
                .description(violation.getDescription())
                .status(violation.getStatus())
                .resolvedAt(violation.getResolvedAt())
                .reportedAt(violation.getCreatedAt())
                .build();
    }

    public ViolationUserDetails toUserDetails(ViolationReport violation) {
        List<String> evidenceUrls = violation.getEvidenceUrls() != null
                ? List.copyOf(violation.getEvidenceUrls())
                : List.of();

        return ViolationUserDetails.builder()
                .id(violation.getId())
                .createdAt(violation.getCreatedAt())
                .updatedAt(violation.getUpdatedAt())
                .reporterId(violation.getReporterId())
                .relatedEntityType(violation.getRelatedEntityType())
                .relatedEntityId(violation.getRelatedEntityId())
                .violationType(violation.getViolationType())
                .status(violation.getStatus())
                .reportedAt(violation.getCreatedAt())
                .description(violation.getDescription())
                .resolvedAt(violation.getResolvedAt())
                .evidenceUrls(evidenceUrls)
                .penaltyApplied(violation.getPenaltyApplied())
                .resolutionNotes(violation.getResolutionNotes())
                .build();
    }

    public ViolationAdminDetails toAdminDetails(ViolationReport violation) {
        List<String> evidenceUrls = violation.getEvidenceUrls() != null
                ? List.copyOf(violation.getEvidenceUrls())
                : List.of();

        return ViolationAdminDetails.builder()
                .id(violation.getId())
                .createdAt(violation.getCreatedAt())
                .updatedAt(violation.getUpdatedAt())
                .reporterId(violation.getReporterId())
                .relatedEntityType(violation.getRelatedEntityType())
                .relatedEntityId(violation.getRelatedEntityId())
                .violationType(violation.getViolationType())
                .reportedAt(violation.getCreatedAt())
                .description(violation.getDescription())
                .evidenceUrls(evidenceUrls)
                .status(violation.getStatus())
                .resolutionNotes(violation.getResolutionNotes())
                .build();
    }
}
