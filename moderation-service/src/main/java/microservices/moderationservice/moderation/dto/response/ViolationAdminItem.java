package microservices.moderationservice.moderation.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import microservices.moderationservice.common.Constants;
import microservices.moderationservice.common.model.AbstractBaseDataResponse;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ViolationAdminItem extends AbstractBaseDataResponse {
    private UUID reporterId;
    private Constants.ViolationReportedTypeEnum relatedEntityType;
    private UUID relatedEntityId;
    private Constants.ViolationTypeEnum violationType;
    private Constants.ViolationStatusEnum status;
    private String description;
    private LocalDateTime reportedAt;
}
