package com.se100.bds.dtos.responses.user.simple;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class PropertyOwnerSimpleCard extends SimpleUserResponse {
    private String email;
}