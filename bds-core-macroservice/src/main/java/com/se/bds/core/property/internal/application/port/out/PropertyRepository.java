package com.se.bds.core.property.internal.application.port.out;


import com.se.bds.core.property.internal.domain.model.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropertyRepository {
    Property save(Property property);
    Optional<Property> findById(UUID id);
    void delete(Property property);
    int countByAssignedAgentId(UUID agentId);
    int countByPropertyTypeId(UUID propertyTypeId);
    List<Property> findAllByOwnerId(UUID ownerId);
    Page<Property> searchWithFilters(
            List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds, List<UUID> propertyTypeIds,
            UUID ownerId, UUID agentId, BigDecimal minPrice, BigDecimal maxPrice,
            BigDecimal minArea, BigDecimal maxArea, Pageable pageable);
}
