package com.se.bds.core.property.internal.application.service;

import com.se.bds.core.property.api.event.PropertyAgentAssignedEvent;
import com.se.bds.core.property.api.event.PropertyStatusChangedEvent;
import com.se.bds.core.property.internal.application.command.*;
import com.se.bds.core.property.internal.application.port.in.PropertyUseCase;
import com.se.bds.core.property.internal.domain.model.*;
import com.se.bds.core.shared.ids.PropertyId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class PropertyServiceImpl implements PropertyUseCase {
    private final com.se.bds.core.property.application.port.out.PropertyRepository propertyRepository;
    private final com.se.bds.core.property.application.port.out.PropertyTypeRepository propertyTypeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Property createProperty(CreatePropertyCommand command, MultipartFile[] mediaFiles, MultipartFile[] documents)
    {
        Property property = new Property();
        property.setOwnerId(command.ownerId());
        PropertyType propertyType = propertyTypeRepository.findById(command.propertTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Property type not found"));
        property.setPropertyType(propertyType);
        property.setWardId(command.wardId());
        property.setTitle(command.title());
        property.setDescription(command.description());
        property.setPriceAmount(command.priceAmount());
        property.setArea(command.area());
        property.setRooms(command.rooms());
        property.setBedrooms(command.bedrooms());
        property.setBathrooms(command.bathrooms());
        property.setFloors(command.floors());

        if (command.houseOrientation() != null) {
            property.setHouseOrientation(Orientation.valueOf(command.houseOrientation()));
        }
        if (command.balconyOrientation() != null) {
            property.setBalconyOrientation(Orientation.valueOf(command.balconyOrientation()));
        }
        if (command.transactionType() != null) {
            property.setTransactionType(TransactionType.valueOf(command.transactionType()));
        }

        property.setFullAddress(command.address());
        property.setStatus(PropertyStatus.PENDING);
        
        property.setCommissionRate(java.math.BigDecimal.ZERO);
        property.setServiceFeeAmount(java.math.BigDecimal.ZERO);
        property.setServiceFeeCollectedAmount(java.math.BigDecimal.ZERO);

        Property saved = propertyRepository.save(property);

        //TODO: Handle file uploads (ideally via an outbound port)
        return saved;
    }

    /**
     * @param command
     * @param mediaFiles
     * @param documents
     * @return
     */
    @Override
    @Transactional
    public Property updateProperty(UUID propertyId,UpdatePropertyCommand command, MultipartFile[] mediaFiles, MultipartFile[] documents) {
        Property property = getProperty(propertyId);
        property.setTitle(command.title());
        property.setDescription(command.description());
        property.setPriceAmount(command.priceAmount());
        property.setArea(command.area());
        //force re-approval
        property.setStatus(PropertyStatus.PENDING);

        return propertyRepository.save(property);
    }

    /**
     * @param propertyId
     * @param command
     * @return
     */
    @Override
    public Property updatePropertyStatus(UUID propertyId, UpdatePropertyStatusCommand command) {
        Property property = getProperty(propertyId);
        String oldStatus = property.getStatus().name();
        PropertyStatus newStatus = PropertyStatus.valueOf(command.targetStatus());
        property.setStatus(newStatus);
        
        Property saved = propertyRepository.save(property);

        eventPublisher.publishEvent(new PropertyStatusChangedEvent(
                new PropertyId(propertyId), oldStatus, newStatus, Instant.now()
        ));
        return saved;
    }

    /**
     * @param propertyId
     */
    @Override
    @Transactional
    public void deleteProperty(UUID propertyId) {
        Property property = getProperty(propertyId);
        String oldStatus = property.getStatus().name();
        property.setStatus(PropertyStatus.DELETED);
        propertyRepository.save(property);

        eventPublisher.publishEvent(new PropertyStatusChangedEvent(
           new PropertyId(propertyId), oldStatus,PropertyStatus.DELETED, Instant.now()
        ));

    }

    /**
     * @param propertyId
     * @param agentId
     */
    @Override
    @Transactional
    public void assignAgent(UUID propertyId, UUID agentId) {
        Property property = getProperty(propertyId);
        UUID previousAgentId = property.getAssignedAgentId();
        property.setAssignedAgentId(agentId);
        propertyRepository.save(property);

        eventPublisher.publishEvent(new PropertyAgentAssignedEvent(
                new PropertyId(propertyId), agentId, previousAgentId, Instant.now()
        ));
    }

    /**
     * @param command
     * @param pageable
     * @return
     */
    @Override
    public Page<Property> searchProperties(SearchPropertyCommand command, Pageable pageable) {
        //delegate complex searching directly to the repository port
        return propertyRepository.searchWithFilters(
                command.cityIds(),command.districtIds(),command.wardIds(),command.propertyTypeIds(),
                command.ownerId(),command.agentId(),command.minPrice(),command.maxPrice(),command.minArea(),
                command.maxArea(),pageable
        );
    }

    /**
     * @param propertyId
     * @return
     */
    @Override
    public Property getPropertyDetail(UUID propertyId) {
        return getProperty(propertyId);
    }

    /**
     * @param command
     * @return
     */
    @Override
    public PropertyType createPropertyType(CreatePropertyTypeCommand command) {
        PropertyType propertyType = new PropertyType();
        propertyType.setTypeName(command.typeName());
        propertyType.setDescription(command.description());
        propertyType.setIsActive(command.isActive());
        return propertyTypeRepository.save(propertyType);
    }

    /**
     * @param pageable
     * @return
     */
    @Override
    public Page<PropertyType> getAllPropertyTypes(Pageable pageable) {
        return propertyTypeRepository.findAll(pageable);
    }

    @Override
    public PropertyType updatePropertyType(UUID id, UpdatePropertyTypeCommand command) {
        PropertyType propertyType = propertyTypeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Property type not found"));
        
        if (command.typeName() != null) propertyType.setTypeName(command.typeName());
        if (command.description() != null) propertyType.setDescription(command.description());
        if (command.isActive() != null) propertyType.setIsActive(command.isActive());
        
        return propertyTypeRepository.save(propertyType);
    }

    @Override
    public void deletePropertyType(UUID id) {
        PropertyType propertyType = propertyTypeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Property type not found"));
        propertyTypeRepository.delete(propertyType);
    }

    @Override
    public java.util.List<UUID> getAllAvailablePropertyTypeIds() {
        return propertyTypeRepository.getAllActiveIds();
    }

    @Override
    public String getPropertyTypeName(UUID propertyTypeId) {
        return propertyTypeRepository.findById(propertyTypeId)
            .map(PropertyType::getTypeName)
            .orElseThrow(() -> new IllegalArgumentException("Property type not found"));
    }

    @Override
    public int countPropertiesByPropertyTypeId(UUID propertyTypeId) {
        return propertyRepository.countByPropertyTypeId(propertyTypeId);
    }

    private Property getProperty(UUID propertyId)
    {
        return propertyRepository.findById(propertyId)
                //TODO: align error message with msg in SRS
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
    }

}
