package com.se.bds.core.property.internal.application.service;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.core.property.internal.application.port.in.LocationUseCase;
import com.se.bds.core.property.internal.application.port.out.CityRepository;
import com.se.bds.core.property.internal.application.port.out.DistrictRepository;
import com.se.bds.core.property.internal.application.port.out.WardRepository;
import com.se.bds.core.property.internal.domain.model.City;
import com.se.bds.core.property.internal.domain.model.District;
import com.se.bds.core.property.internal.domain.model.Ward;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationUseCase {
    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;

    @Override
    @Transactional(readOnly = true)
    public List<City> getAllCities() {
        return cityRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<District> getDistrictsByCityId(UUID cityId) {
        return districtRepository.findAllByCityId(cityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ward> getWardsByDistrictId(UUID districtId) {
        return wardRepository.findAllByDistrictId(districtId);
    }

    @Override
    @Transactional
    public City createCity(String name) {
        City city = City.builder()
                .id(UUID.randomUUID())
                .cityName(name)
                .build();
        return cityRepository.save(city);
    }

    @Override
    @Transactional
    public District createDistrict(UUID cityId, String name) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new BusinessException("CITY_NOT_FOUND", "City not found: " + cityId));
        District district = District.builder()
                .id(UUID.randomUUID())
                .city(city)
                .districtName(name)
                .build();
        return districtRepository.save(district);
    }

    @Override
    @Transactional
    public Ward createWard(UUID districtId, String name) {
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new BusinessException("DISTRICT_NOT_FOUND", "District not found: " + districtId));
        Ward ward = Ward.builder()
                .id(UUID.randomUUID())
                .district(district)
                .wardName(name)
                .build();
        return wardRepository.save(ward);
    }

    @Override
    @Transactional
    public City updateCity(UUID cityId, String name) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new BusinessException("CITY_NOT_FOUND", "City not found: " + cityId));
        city.setCityName(name);
        return cityRepository.save(city);
    }

    @Override
    @Transactional
    public District updateDistrict(UUID districtId, UUID cityId, String name) {
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new BusinessException("DISTRICT_NOT_FOUND", "District not found: " + districtId));
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new BusinessException("CITY_NOT_FOUND", "City not found: " + cityId));
        district.setCity(city);
        district.setDistrictName(name);
        return districtRepository.save(district);
    }

    @Override
    @Transactional
    public Ward updateWard(UUID wardId, UUID districtId, String name) {
        Ward ward = wardRepository.findById(wardId)
                .orElseThrow(() -> new BusinessException("WARD_NOT_FOUND", "Ward not found: " + wardId));
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new BusinessException("DISTRICT_NOT_FOUND", "District not found: " + districtId));
        ward.setDistrict(district);
        ward.setWardName(name);
        return wardRepository.save(ward);
    }

    @Override
    @Transactional
    public void deleteCity(UUID cityId) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new BusinessException("CITY_NOT_FOUND", "City not found: " + cityId));
        cityRepository.delete(city);
    }

    @Override
    @Transactional
    public void deleteDistrict(UUID districtId) {
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new BusinessException("DISTRICT_NOT_FOUND", "District not found: " + districtId));
        districtRepository.delete(district);
    }

    @Override
    @Transactional
    public void deleteWard(UUID wardId) {
        Ward ward = wardRepository.findById(wardId)
                .orElseThrow(() -> new BusinessException("WARD_NOT_FOUND", "Ward not found: " + wardId));
        wardRepository.delete(ward);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<UUID, String> resolveWardNames(java.util.List<UUID> wardIds) {
        if (wardIds == null || wardIds.isEmpty()) return java.util.Collections.emptyMap();
        List<Ward> wards = wardRepository.findAllById(wardIds);
        java.util.Map<UUID, String> result = new java.util.HashMap<>();
        for (Ward ward : wards) {
            String fullName = ward.getWardName();
            if (ward.getDistrict() != null) {
                fullName += ", " + ward.getDistrict().getDistrictName();
                if (ward.getDistrict().getCity() != null) {
                    fullName += ", " + ward.getDistrict().getCity().getCityName();
                }
            }
            result.put(ward.getId(), fullName);
        }
        return result;
    }
}
