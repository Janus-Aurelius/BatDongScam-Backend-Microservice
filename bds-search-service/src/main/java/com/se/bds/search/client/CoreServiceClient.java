package com.se.bds.search.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "core-macroservice", contextId = "coreServiceClient")
public interface CoreServiceClient {

    @GetMapping("/api/internal/locations/cities/ids")
    List<UUID> getAllCityIds();

    @GetMapping("/api/internal/locations/districts/ids")
    List<UUID> getAllDistrictIds();

    @GetMapping("/api/internal/locations/wards/ids")
    List<UUID> getAllWardIds();

    @GetMapping("/api/internal/property-types/ids")
    List<UUID> getAllAvailablePropertyTypeIds();

    @GetMapping("/api/internal/properties/{propertyId}/location-info")
    PropertyLocationInfo getPropertyLocationInfo(@org.springframework.web.bind.annotation.PathVariable("propertyId") UUID propertyId);

    record PropertyLocationInfo(
            UUID propertyId,
            UUID cityId,
            UUID districtId,
            UUID wardId,
            UUID propertyTypeId
    ) {}
}
