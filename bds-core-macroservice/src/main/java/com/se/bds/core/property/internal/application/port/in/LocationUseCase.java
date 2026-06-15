package com.se.bds.core.property.internal.application.port.in;

import com.se.bds.core.property.internal.domain.model.City;
import com.se.bds.core.property.internal.domain.model.District;
import com.se.bds.core.property.internal.domain.model.Ward;
import java.util.List;
import java.util.UUID;

public interface LocationUseCase {
    List<City> getAllCities();
    List<District> getDistrictsByCityId(UUID cityId);
    List<Ward> getWardsByDistrictId(UUID districtId);

    City createCity(String name);
    District createDistrict(UUID cityId, String name);
    Ward createWard(UUID districtId, String name);

    City updateCity(UUID cityId, String name);
    District updateDistrict(UUID districtId, UUID cityId, String name);
    Ward updateWard(UUID wardId, UUID districtId, String name);

    void deleteCity(UUID cityId);
    void deleteDistrict(UUID districtId);
    void deleteWard(UUID wardId);

    java.util.Map<UUID, String> resolveWardNames(java.util.List<UUID> wardIds);
}
