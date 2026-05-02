package com.se100.bds.dtos.responses.violation;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import com.se100.bds.utils.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ViolationUserDetails extends AbstractBaseDataResponse {
    private Constants.ViolationTypeEnum violationType;
    private Constants.ViolationStatusEnum status;
    private LocalDateTime reportedAt;
    private String targetName;
    private String description;
    private LocalDateTime resolvedAt;
    private List<String> evidenceUrls;
    private Constants.PenaltyAppliedEnum penaltyApplied;
    private String resolutionNotes;
}
