package com.se.bds.core.property.internal.adapter.out.persistence;

import com.se.bds.core.property.internal.application.port.out.CityRepository;
import com.se.bds.core.property.internal.domain.model.City;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CityRepositoryAdapter implements CityRepository {
    private final JpaCityRepository jpaCityRepository;

    @Override
    public City save(City city) {
        return jpaCityRepository.save(city);
    }

    @Override
    public Optional<City> findById(UUID id) {
        return jpaCityRepository.findById(id);
    }

    @Override
    public List<City> findAll() {
        return jpaCityRepository.findAll();
    }

    @Override
    public void delete(City city) {
        jpaCityRepository.delete(city);
    }
}
