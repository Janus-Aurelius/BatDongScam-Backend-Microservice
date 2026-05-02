package com.se100.bds.dtos.responses.user.listitem;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class PropertyOwnerListItem extends AbstractBaseDataResponse {
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private Integer ranking;
    private Integer point;
    private String tier;
    private BigDecimal totalValue;
    private Integer totalProperties;
    private String location;
}
