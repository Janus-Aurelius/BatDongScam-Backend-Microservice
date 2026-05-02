package com.se100.bds.dtos.requests.violation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.se100.bds.utils.Constants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ViolationCreateRequest {
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
