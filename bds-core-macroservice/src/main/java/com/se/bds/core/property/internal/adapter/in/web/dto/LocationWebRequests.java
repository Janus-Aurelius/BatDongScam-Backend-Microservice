package com.se.bds.core.property.internal.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class LocationWebRequests {
    public record CreateCityRequest(@NotBlank String cityName) {}
    public record CreateDistrictRequest(@NotNull UUID cityId, @NotBlank String districtName) {}
    public record CreateWardRequest(@NotNull UUID districtId, @NotBlank String wardName) {}

    public record UpdateCityRequest(@NotBlank String cityName) {}
    public record UpdateDistrictRequest(@NotNull UUID cityId, @NotBlank String districtName) {}
    public record UpdateWardRequest(@NotNull UUID districtId, @NotBlank String wardName) {}
}
