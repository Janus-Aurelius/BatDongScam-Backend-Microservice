package com.se.bds.core.property.internal.application.port.out;

import com.se.bds.core.property.internal.domain.model.PropertyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropertyTypeRepository {
    PropertyType save(PropertyType propertyType);
    Optional<PropertyType> findById(UUID id);
    void delete(PropertyType propertyType);
    Page<PropertyType> findAll(Pageable pageable);
    List<UUID> getAllActiveIds();
}
