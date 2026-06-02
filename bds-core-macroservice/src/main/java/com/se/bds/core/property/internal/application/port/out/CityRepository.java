package com.se.bds.core.property.internal.application.port.out;

import com.se.bds.core.property.internal.domain.model.City;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CityRepository {
    City save(City city);
    Optional<City> findById(UUID id);
    List<City> findAll();
    void delete(City city);
}
