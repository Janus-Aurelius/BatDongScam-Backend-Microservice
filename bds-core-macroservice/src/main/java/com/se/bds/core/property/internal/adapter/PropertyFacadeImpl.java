package com.se.bds.core.property.internal.adapter;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.message.validation.MSG12;
import com.se.bds.core.property.api.PropertyFacade;
import com.se.bds.core.property.internal.application.port.in.PropertyUseCase;
import com.se.bds.core.property.internal.domain.model.Property;
import com.se.bds.core.property.internal.domain.model.PropertyStatus;
import com.se.bds.core.property.internal.domain.model.PropertyType;
import com.se.bds.core.shared.dto.PropertySnapshot;
import com.se.bds.core.shared.dto.property.PropertyCard;
import com.se.bds.core.shared.ids.PropertyId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.awt.print.Pageable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyFacadeImpl implements PropertyFacade {

    private final PropertyUseCase propertyUseCase;

    @Override
    public PropertySnapshot getPropertySnapshot(PropertyId propertyId) {
        Property property = propertyUseCase.getPropertyDetail(propertyId.value());
        return mapToSnapshot(property);
    }

    @Override
    public void validatePropertyAvailableForContract(PropertyId propertyId, String contractType) {
        Property property = propertyUseCase.getPropertyDetail(propertyId.value());
        if (property.getStatus() != PropertyStatus.AVAILABLE && property.getStatus() != PropertyStatus.PENDING) {
             throw new BusinessException(MSG12.CODE, MSG12.MESSAGE);
        }
        // Additional validation based on contractType can be added here
    }

    @Override
    public List<PropertySnapshot> getPropertySnapshotsByOwnerId(UUID ownerId, List<String> statuses) {
        // This would require a search or specific port. 
        // For now, let's assume we can search by owner.
        // Simplified for this implementation.
        return List.of(); 
    }

    @Override
    public List<PropertySnapshot> getPropertySnapshotsByUserIdAndStatus(UUID customerId, List<String> statuses) {
        return List.of();
    }

    @Override
    public int countByAssignedAgentId(UUID agentId) {
        return 0; // Should delegate to repository or usecase
    }

    @Override
    public Page<PropertyCard> getFavoritePropertyCard(List<UUID> propertyIds, Pageable pageable) {
        return Page.empty();
    }

    @Override
    public String getPropertyTypeName(UUID propertyTypeId) {
        return propertyUseCase.getPropertyTypeName(propertyTypeId);
    }

    @Override
    public int countPropertiesByTypeId(UUID propertyTypeId) {
        return propertyUseCase.countPropertiesByPropertyTypeId(propertyTypeId);
    }

    @Override
    public List<UUID> getAllAvailablePropertyTypeIds() {
        return propertyUseCase.getAllAvailablePropertyTypeIds();
    }

    @Override
    public void recordServiceFeePayment(PropertyId propertyId, BigDecimal amount) {
        // This would probably be a new method in PropertyUseCase or handled via PropertyMaintenanceUseCase
    }

    private PropertySnapshot mapToSnapshot(Property property) {
        return new PropertySnapshot(
                new PropertyId(property.getId()),
                property.getOwnerId(),
                property.getAssignedAgentId(),
                property.getWardId(),
                property.getFullAddress(),
                property.getTransactionType().name(),
                property.getPropertyType().getTypeName(),
                property.getStatus().name(),
                property.getTitle(),
                property.getDescription(),
                property.getArea(),
                property.getRooms(),
                property.getBathrooms(),
                property.getFloors(),
                property.getBedrooms(),
                property.getHouseOrientation() != null ? property.getHouseOrientation().name() : null,
                property.getBalconyOrientation() != null ? property.getBalconyOrientation().name() : null,
                property.getYearBuilt(),
                property.getAmenities(),
                null, // TODO: map MonetaryAmount price if needed
                property.getPricePerSquareMeter(),
                property.getCommissionRate(),
                property.getServiceFeeAmount(),
                property.getServiceFeeCollectedAmount()
        );
    }
}
