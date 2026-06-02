package com.se100.bds.searchservice.dtos.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddSearchRequest {
    private UUID userId;
    private UUID cityId;
    private UUID districtId;
    private UUID wardId;
    private UUID propertyId;
    private UUID propertyTypeId;
}
