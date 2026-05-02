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
public class ViolationUserItem extends AbstractBaseDataResponse {
    private Constants.ViolationTypeEnum violationType;
    private String description;
    private Constants.ViolationStatusEnum status;
    private String targetName;
    private LocalDateTime resolvedAt;
    private LocalDateTime reportedAt;
}
