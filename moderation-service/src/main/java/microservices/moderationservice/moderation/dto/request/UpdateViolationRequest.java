package microservices.moderationservice.moderation.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import microservices.moderationservice.common.Constants;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateViolationRequest {
    @NotNull(message = "Status is required")
    private Constants.ViolationStatusEnum status;

    @Size(max = 2000, message = "Resolution notes cannot exceed 2000 characters")
    private String resolutionNotes;

    private Constants.PenaltyAppliedEnum penaltyApplied;
}
