package com.se100.bds.dtos.requests.violation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.se100.bds.utils.Constants;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

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

