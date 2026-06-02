package com.se.bds.core.property.internal.adapter.out.persistence;

import com.se.bds.core.property.internal.application.port.out.WardRepository;
import com.se.bds.core.property.internal.domain.model.Ward;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WardRepositoryAdapter implements WardRepository {
    private final JpaWardRepository jpaWardRepository;

    @Override
    public Ward save(Ward ward) {
        return jpaWardRepository.save(ward);
    }

    @Override
    public Optional<Ward> findById(UUID id) {
        return jpaWardRepository.findById(id);
    }

    @Override
    public List<Ward> findAllByDistrictId(UUID districtId) {
        return jpaWardRepository.findAllByDistrictId(districtId);
    }

    @Override
    public void delete(Ward ward) {
        jpaWardRepository.delete(ward);
    }
}
