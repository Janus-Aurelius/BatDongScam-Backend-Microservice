package com.se.bds.core.property.internal.application.port.out;

import com.se.bds.core.property.internal.domain.model.District;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DistrictRepository {
    District save(District district);
    Optional<District> findById(UUID id);
    List<District> findAllByCityId(UUID cityId);
    void delete(District district);
}
