package microservices.moderationservice.moderation.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import microservices.moderationservice.common.Constants;
import microservices.moderationservice.common.model.AbstractBaseDataResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ViolationAdminDetails extends AbstractBaseDataResponse {
    private UUID reporterId;
    private Constants.ViolationReportedTypeEnum relatedEntityType;
    private UUID relatedEntityId;
    private Constants.ViolationTypeEnum violationType;
    private LocalDateTime reportedAt;
    private String description;
    private List<String> evidenceUrls;
    private Constants.ViolationStatusEnum status;
    private String resolutionNotes;
}
