package com.se100.bds.dtos.responses.user.listitem;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class FreeAgentListItem extends AbstractBaseDataResponse {
    private String fullName;
    private Integer ranking;
    private String employeeCode;
    private String avatarUrl;
    private String tier;
    private Integer assignedAppointments;
    private Integer assignedProperties;
    private Integer currentlyHandling;
    private Integer maxProperties;
}
