package com.se.bds.search.dtos.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddSearchListRequest {
    private UUID userId;
    private List<UUID> cityIds;
    private List<UUID> districtIds;
    private List<UUID> wardIds;
    private List<UUID> propertyTypeIds;
}
