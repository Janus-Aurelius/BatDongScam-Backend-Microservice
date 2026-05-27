package com.se.bds.core.property.internal.adapter.out.persistence;
import com.se.bds.core.property.internal.application.port.out.PropertyRepository;
import com.se.bds.core.property.internal.domain.model.Property;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PropertyRepositoryAdapter implements PropertyRepository{
    private final JpaPropertyRepository propertyRepository;

    @Override
    public Property save(Property property){
        return propertyRepository.save(property);
    }

    @Override public Optional<Property> findById(UUID id)
    {
        return propertyRepository.findById(id);
    }

    @Override
    public void delete(Property property) {
        propertyRepository.delete(property);
    }

    @Override
    public int countByAssignedAgentId(UUID assignedAgentId) {
        return propertyRepository.countByAssignedAgentId(assignedAgentId);
    }

    @Override
    public int countByPropertyTypeId(UUID propertyTypeId) {
        return propertyRepository.countByPropertyTypeId(propertyTypeId);
    }

    @Override
    public List<Property> findAllByOwnerId(UUID ownerId)
    {
        return propertyRepository.findAllByOwnerId(ownerId);
    }

    @Override
    public Page<Property> searchWithFilters(
            List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds, List<UUID> propertyTypeIds,
            UUID ownerId, UUID agentId, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice,
            java.math.BigDecimal minArea, java.math.BigDecimal maxArea, Pageable pageable) {
        return propertyRepository.searchWithFilters(
                cityIds, districtIds, wardIds, propertyTypeIds, ownerId, agentId,
                minPrice, maxPrice, minArea, maxArea, pageable
        );
    }
}
