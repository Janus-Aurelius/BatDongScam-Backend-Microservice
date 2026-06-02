package com.se.bds.core.property.internal.adapter.out.persistence;

import com.se.bds.core.property.internal.application.port.out.DistrictRepository;
import com.se.bds.core.property.internal.domain.model.District;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DistrictRepositoryAdapter implements DistrictRepository {
    private final JpaDistrictRepository jpaDistrictRepository;

    @Override
    public District save(District district) {
        return jpaDistrictRepository.save(district);
    }

    @Override
    public Optional<District> findById(UUID id) {
        return jpaDistrictRepository.findById(id);
    }

    @Override
    public List<District> findAllByCityId(UUID cityId) {
        return jpaDistrictRepository.findAllByCityId(cityId);
    }

    @Override
    public void delete(District district) {
        jpaDistrictRepository.delete(district);
    }
}
