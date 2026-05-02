package com.se100.bds.dtos.responses.user.simple;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class SalesAgentSimpleCard extends SimpleUserResponse {
    private Double rating;
    private Integer totalRates;
}