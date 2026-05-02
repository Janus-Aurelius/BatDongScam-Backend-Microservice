package com.se100.bds.dtos.responses.user.propertyprofile;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class PropertyOwnerPropertyProfileResponse {
    private Integer totalListings;
    private Integer totalSolds;
    private Integer totalRentals;
}
