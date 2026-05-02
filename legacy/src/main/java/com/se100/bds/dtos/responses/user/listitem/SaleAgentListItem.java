package com.se100.bds.dtos.responses.user.listitem;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class SaleAgentListItem extends AbstractBaseDataResponse {
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private Integer ranking;
    private String employeeCode;
    private Integer point;
    private String tier;
    private Integer totalAssignments;
    private Integer propertiesAssigned;
    private Integer appointmentsAssigned;
    private Integer totalContracts;
    private Double rating;
    private Integer totalRates;
    private LocalDateTime hiredDate;
    private String location;
}
