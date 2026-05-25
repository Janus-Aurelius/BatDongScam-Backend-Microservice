package com.se.bds.core.property.internal.adapter.out.persistence;

import com.se.bds.core.property.internal.application.port.out.PropertyTypeRepository;
import com.se.bds.core.property.internal.domain.model.PropertyType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PropertyTypeRepositoryAdapter implements PropertyTypeRepository {

    private final JpaPropertyTypeRepository jpaPropertyTypeRepository;

    @Override
    public PropertyType save(PropertyType propertyType) {
        return jpaPropertyTypeRepository.save(propertyType);
    }

    @Override
    public Optional<PropertyType> findById(UUID id) {
        return jpaPropertyTypeRepository.findById(id);
    }

    @Override
    public void delete(PropertyType propertyType) {
        jpaPropertyTypeRepository.delete(propertyType);
    }

    @Override
    public Page<PropertyType> findAll(Pageable pageable) {
        return jpaPropertyTypeRepository.findAll(pageable);
    }

    @Override
    public List<UUID> getAllActiveIds() {
        return jpaPropertyTypeRepository.findAllActiveIds();
    }
}
