package com.se.bds.core.shared.dto.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class SalesAgentSimpleCard extends SimpleUserResponse{
    private Double rating;
    private Integer totalRates;
}
