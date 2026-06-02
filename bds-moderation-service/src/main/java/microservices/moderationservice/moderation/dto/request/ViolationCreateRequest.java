package microservices.moderationservice.moderation.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import microservices.moderationservice.common.Constants;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ViolationCreateRequest {
    @NotNull(message = "Reporter ID is required")
    private UUID reporterId;

    @NotNull(message = "Violation type is required")
    private Constants.ViolationTypeEnum violationType;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    private String description;

    @NotNull(message = "Violation reported type is required")
    private Constants.ViolationReportedTypeEnum violationReportedType;

    @NotNull(message = "Reported entity ID is required")
    private UUID reportedId;
}
