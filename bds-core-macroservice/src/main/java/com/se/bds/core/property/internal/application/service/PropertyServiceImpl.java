package com.se.bds.core.property.internal.application.service;

import com.se.bds.core.property.api.event.*;
import com.se.bds.core.property.internal.application.command.*;
import com.se.bds.core.property.internal.application.command.pattern.CreatePropertyAction;
import com.se.bds.core.property.internal.application.command.pattern.DeletePropertyAction;
import com.se.bds.core.property.internal.application.command.pattern.UpdatePropertyAction;
import com.se.bds.core.property.internal.application.port.in.PropertyUseCase;
import com.se.bds.core.property.internal.application.port.out.MessagePublisherPort;
import com.se.bds.core.property.internal.domain.model.*;
import com.se.bds.core.property.internal.domain.model.strategy.FeeCalculationStrategy;
import com.se.bds.core.shared.ids.PropertyId;
import com.se.bds.core.property.internal.application.port.out.PropertyRepository;
import com.se.bds.core.property.internal.application.port.out.PropertyTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.lang.String.valueOf;

@Service
@RequiredArgsConstructor
@Slf4j
class PropertyServiceImpl implements PropertyUseCase {
    private final PropertyRepository propertyRepository;
    private final PropertyTypeRepository propertyTypeRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final MessagePublisherPort messagePublisherPort;

    private final List<FeeCalculationStrategy> feeStrategies;

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

        // Apply STRATEGY PATTERN
        FeeCalculationStrategy feeStrategy = feeStrategies.stream()
                .filter(s -> s.supports(property.getTransactionType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chiến lược tính phí hợp lệ cho giao dịch này"));

        property.setCommissionRate(feeStrategy.calculateCommissionRate());
        property.setServiceFeeAmount(feeStrategy.calculateServiceFee(command.priceAmount(), command.area()));
        property.setServiceFeeCollectedAmount(java.math.BigDecimal.ZERO);

        // Apply COMMAND PATTERN
        CreatePropertyAction createAction = new CreatePropertyAction(propertyRepository, property);
        Property saved = createAction.execute();

        //TODO: Handle file uploads (ideally via an outbound port)

        // Apply OBSERVER PATTERN
        PropertyCreatedIntegrationEvent integrationEvent = new PropertyCreatedIntegrationEvent(
                saved.getId(),
                saved.getTitle(),
                saved.getOwnerId(),
                saved.getTransactionType().name()
        );
        messagePublisherPort.publishPropertyCreated(integrationEvent);

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
    public Property updateProperty(
            UUID propertyId,
            UpdatePropertyCommand command,
            MultipartFile[] mediaFiles,
            MultipartFile[] documents
    ) {

        Property property = getProperty(propertyId);

        UpdatePropertyAction action = new UpdatePropertyAction(propertyRepository, property);

        property.setTitle(command.title());
        property.setDescription(command.description());
        property.setPriceAmount(command.priceAmount());
        property.setArea(command.area());
        //force re-approval
        property.setStatus(PropertyStatus.PENDING);

        Property saved = action.execute();

        messagePublisherPort.publishPropertyUpdated(new PropertyUpdatedIntegrationEvent(saved.getId()));

        return saved;
    }

    /**
     * @param propertyId
     * @param command
     * @return
     */
    @Override
    public Property updatePropertyStatus(UUID propertyId, UpdatePropertyStatusCommand command) {
        Property property = getProperty(propertyId);
        PropertyStatus newStatus = PropertyStatus.valueOf(command.targetStatus());
        PropertyStatus oldStatus = property.transitionStatus(newStatus);

        Property saved = propertyRepository.save(property);

        eventPublisher.publishEvent(new PropertyStatusChangedEvent(
                new PropertyId(propertyId), oldStatus.name(), newStatus, Instant.now()
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

        PropertyStatus oldStatus = property.getStatus();

        DeletePropertyAction action = new DeletePropertyAction(propertyRepository, property);
        action.execute();

        eventPublisher.publishEvent(new PropertyStatusChangedEvent(
           new PropertyId(propertyId), oldStatus.name(),PropertyStatus.DELETED, Instant.now()
        ));

        messagePublisherPort.publishPropertyDeleted(new PropertyDeletedIntegrationEvent(propertyId));
    }

    /**
     * @param propertyId
     * @param agentId
     */
    @Override
    @Transactional
    public void assignAgent(UUID propertyId, UUID agentId) {
        Property property = getProperty(propertyId);
        UUID previousAgentId = property.assignAgent(agentId);
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
    @Cacheable(value = "propertyDetails", key = "#propertyId")
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
