package microservices.moderationservice.moderation.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.se.bds.common.enums.*;
import com.se.bds.common.dto.AbstractBaseDataResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ViolationAdminDetails extends AbstractBaseDataResponse {
    private UUID reporterId;
    private ViolationReportedTypeEnum relatedEntityType;
    private UUID relatedEntityId;
    private ViolationTypeEnum violationType;
    private LocalDateTime reportedAt;
    private String description;
    private List<String> evidenceUrls;
    private ViolationStatusEnum status;
    private String resolutionNotes;

    private String reporterName;
    private String reporterAvatarUrl;
    private String reportedName;
    private String reportedAvatarUrl;
    private String reportedRole;
    private String reportedEmail;
    private String reportedPhoneNumber;
    private String reportedTitle; // if property, property title
}
