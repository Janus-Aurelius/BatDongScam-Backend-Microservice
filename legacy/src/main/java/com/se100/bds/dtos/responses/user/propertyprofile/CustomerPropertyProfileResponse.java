package com.se100.bds.dtos.responses.user.propertyprofile;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CustomerPropertyProfileResponse {
    private Integer totalListings;
    private Integer totalBought;
    private Integer totalRented;
}
