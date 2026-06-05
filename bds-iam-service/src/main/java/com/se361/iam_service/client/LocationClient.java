package com.se361.iam_service.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "core-macroservice")
public interface LocationClient {

    @GetMapping("/public/locations/cities")
    List<CityDto> getAllCities();

    @GetMapping("/public/locations/cities/{cityId}/districts")
    List<DistrictDto> getDistrictsByCityId(@PathVariable("cityId") UUID cityId);

    @GetMapping("/public/locations/districts/{districtId}/wards")
    List<WardDto> getWardsByDistrictId(@PathVariable("districtId") UUID districtId);

    @Getter
    @Setter
    class CityDto {
        private UUID id;
        private String cityName;
    }

    @Getter
    @Setter
    class DistrictDto {
        private UUID id;
        private String districtName;
    }

    @Getter
    @Setter
    class WardDto {
        private UUID id;
        private String wardName;
    }
}
