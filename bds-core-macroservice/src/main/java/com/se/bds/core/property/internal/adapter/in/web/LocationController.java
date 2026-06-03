package com.se.bds.core.property.internal.adapter.in.web;

import com.se.bds.core.property.internal.adapter.in.web.dto.LocationWebRequests.*;
import com.se.bds.core.property.internal.application.port.in.LocationUseCase;
import com.se.bds.core.property.internal.adapter.out.client.SearchServiceClient;
import com.se.bds.core.property.internal.domain.model.City;
import com.se.bds.core.property.internal.domain.model.District;
import com.se.bds.core.property.internal.domain.model.Ward;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class LocationController {
    private final LocationUseCase locationUseCase;
    private final SearchServiceClient searchServiceClient;

    @GetMapping("/api/internal/locations/cities/ids")
    public ResponseEntity<List<UUID>> getAllCityIds() {
        return ResponseEntity.ok(
                locationUseCase.getAllCities().stream()
                        .map(City::getId)
                        .toList()
        );
    }

    @GetMapping("/api/internal/locations/districts/ids")
    public ResponseEntity<List<UUID>> getAllDistrictIds() {
        return ResponseEntity.ok(
                locationUseCase.getAllCities().stream()
                        .flatMap(c -> locationUseCase.getDistrictsByCityId(c.getId()).stream())
                        .map(District::getId)
                        .toList()
        );
    }

    @GetMapping("/api/internal/locations/wards/ids")
    public ResponseEntity<List<UUID>> getAllWardIds() {
        return ResponseEntity.ok(
                locationUseCase.getAllCities().stream()
                        .flatMap(c -> locationUseCase.getDistrictsByCityId(c.getId()).stream())
                        .flatMap(d -> locationUseCase.getWardsByDistrictId(d.getId()).stream())
                        .map(Ward::getId)
                        .toList()
        );
    }

    @GetMapping("/public/locations/cities")
    public ResponseEntity<List<City>> getAllCities() {
        return ResponseEntity.ok(locationUseCase.getAllCities());
    }

    @GetMapping("/public/locations/cities/{cityId}/districts")
    public ResponseEntity<List<District>> getDistrictsByCityId(@PathVariable UUID cityId) {
        return ResponseEntity.ok(locationUseCase.getDistrictsByCityId(cityId));
    }

    @GetMapping("/public/locations/districts/{districtId}/wards")
    public ResponseEntity<List<Ward>> getWardsByDistrictId(@PathVariable UUID districtId) {
        return ResponseEntity.ok(locationUseCase.getWardsByDistrictId(districtId));
    }

    @GetMapping("/public/locations/cities/top")
    public ResponseEntity<List<City>> getTopCities(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam int year,
            @RequestParam int month
    ) {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            userId = new UUID(0, 0); // fallback for anonymous users
        }

        List<UUID> cityIds = List.of();
        try {
            com.se.bds.common.dto.ApiResponse<List<UUID>> response = searchServiceClient.topMostSearchByUser(
                    userId, offset, limit, "CITY", year, month
            );
            if (response != null && response.isSuccess() && response.getData() != null) {
                cityIds = response.getData();
            }
        } catch (Exception e) {
            // fallback gracefully
        }

        List<City> allCities = locationUseCase.getAllCities();
        List<UUID> finalCityIds = cityIds;
        List<City> topCities = allCities.stream()
                .filter(city -> finalCityIds.contains(city.getId()))
                .sorted(java.util.Comparator.comparingInt(city -> finalCityIds.indexOf(city.getId())))
                .toList();

        return ResponseEntity.ok(topCities);
    }

    private UUID getCurrentUserId() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/locations/cities")
    public ResponseEntity<City> createCity(@Valid @RequestBody CreateCityRequest request) {
        return ResponseEntity.ok(locationUseCase.createCity(request.cityName()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/locations/districts")
    public ResponseEntity<District> createDistrict(@Valid @RequestBody CreateDistrictRequest request) {
        return ResponseEntity.ok(locationUseCase.createDistrict(request.cityId(), request.districtName()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/locations/wards")
    public ResponseEntity<Ward> createWard(@Valid @RequestBody CreateWardRequest request) {
        return ResponseEntity.ok(locationUseCase.createWard(request.districtId(), request.wardName()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/locations/cities/{cityId}")
    public ResponseEntity<City> updateCity(@PathVariable UUID cityId, @Valid @RequestBody UpdateCityRequest request) {
        return ResponseEntity.ok(locationUseCase.updateCity(cityId, request.cityName()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/locations/districts/{districtId}")
    public ResponseEntity<District> updateDistrict(
            @PathVariable UUID districtId,
            @Valid @RequestBody UpdateDistrictRequest request) {
        return ResponseEntity.ok(locationUseCase.updateDistrict(districtId, request.cityId(), request.districtName()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/locations/wards/{wardId}")
    public ResponseEntity<Ward> updateWard(
            @PathVariable UUID wardId,
            @Valid @RequestBody UpdateWardRequest request) {
        return ResponseEntity.ok(locationUseCase.updateWard(wardId, request.districtId(), request.wardName()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/locations/cities/{cityId}")
    public ResponseEntity<Void> deleteCity(@PathVariable UUID cityId) {
        locationUseCase.deleteCity(cityId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/locations/districts/{districtId}")
    public ResponseEntity<Void> deleteDistrict(@PathVariable UUID districtId) {
        locationUseCase.deleteDistrict(districtId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/locations/wards/{wardId}")
    public ResponseEntity<Void> deleteWard(@PathVariable UUID wardId) {
        locationUseCase.deleteWard(wardId);
        return ResponseEntity.ok().build();
    }
}
