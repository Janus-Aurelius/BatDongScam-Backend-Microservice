package com.se100.bds.dtos.responses.violation;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import com.se100.bds.utils.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ViolationAdminItem extends AbstractBaseDataResponse {
    private String reporterName;
    private String reporterAvatarUrl;
    private String reportedName;
    private String reportedAvatarUrl;
    private Constants.ViolationTypeEnum violationType;
    private Constants.ViolationStatusEnum status;
    private String description;
    private LocalDateTime reportedAt;
}