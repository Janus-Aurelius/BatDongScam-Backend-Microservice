package com.se.bds.core.property.internal.application.service;

import com.se.bds.core.property.api.event.PropertyAgentAssignedEvent;
import com.se.bds.core.property.api.event.PropertyStatusChangedEvent;
import com.se.bds.core.property.internal.application.command.*;
import com.se.bds.core.property.internal.application.port.in.PropertyUseCase;
import com.se.bds.core.property.internal.domain.model.Property;
import com.se.bds.core.property.internal.domain.model.PropertyStatus;
import com.se.bds.core.property.internal.domain.model.PropertyType;
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

import static java.lang.System.getProperty;

@Service
@RequiredArgsConstructor
@Slf4j
class PropertyServiceImpl implements PropertyUseCase {
    private final PropertyRepository propertyRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Property createProperty(CreatePropertyCommand command, MultipartFile[] mediaFiles, MultipartFile[] documents)
    {
        Property property = new Property();
        property.setOwnerId(command.ownerId());
        // FIXME: Implement the NOTE: In a real implementation, you'd fetch PropertyType and Ward from their respective repositories here
        //  property.setPropertyType(typeRepository.findById(command.propertyTypeId()).orElseThrow());
        property.setTitle(command.title());
        property.setDescription(command.description());
        property.setDescription(command.description());
        property.setArea(command.area());
        property.setRooms(command.rooms());
        property.setBedrooms(command.bedrooms());
        property.setBathrooms(command.bathrooms());
        property.setFloors(command.floors());

        // TODO:Map enums safely:
        //   property.setTransactionType(TransactionTypeEnum.valueOf(command.transactionType()));
        property.setFullAddress(command.address());
        property.setStatus(PropertyStatus.PENDING);
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
        property.setStatus(PropertyStatus.valueOf(command.targetStatus()));

        eventPublisher.publishEvent(new PropertyStatusChangedEvent(
                new PropertyId(propertyId), oldStatus, command.targetStatus(), Instant.now()
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
        return propertyTypeRepository.save(type);
    }

    /**
     * @param pageable
     * @return
     */
    @Override
    public Page<PropertyType> getAllPropertyTypes(Pageable pageable) {
        return propertyTypeRepository.findAll(pageable);
    }

    private Property getProperty(UUID propertyId)
    {
        return propertyRepository.findById(propertyId)
                //TODO: align error message with msg in SRS
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
    }

}
