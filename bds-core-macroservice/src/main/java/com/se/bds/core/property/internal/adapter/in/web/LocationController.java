package com.se.bds.core.property.internal.adapter.in.web;

import com.se.bds.core.property.internal.adapter.in.web.dto.LocationWebRequests.*;
import com.se.bds.core.property.internal.application.port.in.LocationUseCase;
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
