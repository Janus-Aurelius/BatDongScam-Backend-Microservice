package com.se.bds.core.property.internal.application.port.out;

import com.se.bds.core.property.internal.domain.model.Ward;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WardRepository {
    Ward save(Ward ward);
    Optional<Ward> findById(UUID id);
    List<Ward> findAllByDistrictId(UUID districtId);
    void delete(Ward ward);
}
