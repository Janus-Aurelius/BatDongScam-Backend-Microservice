package com.se100.bds.dtos.responses.property;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class PropertyTypeResponse extends AbstractBaseDataResponse {
    private String typeName;
    private String avatarUrl;
    private String description;
    private Boolean isActive;
}

