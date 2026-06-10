package microservices.appointmentservice.services.property.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.event.PropertyCreatedEvent;
import com.se.bds.common.event.PropertyDeletedEvent;
import com.se.bds.common.event.PropertyStatusChangedEvent;
import com.se.bds.common.event.PropertyUpdatedEvent;
import jakarta.persistence.EntityManager;
import microservices.appointmentservice.entities.property.Property;
import microservices.appointmentservice.entities.property.PropertyType;
import microservices.appointmentservice.repositories.PropertyRepository;
import microservices.appointmentservice.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class PropertyEventListener {

    private final PropertyRepository propertyRepository;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    @KafkaListener(topics = "property-created", groupId = "appointment-service-group")
    public void onPropertyCreated(String message) {
        log.info("[KAFKA] Received property-created event: {}", message);
        try {
            PropertyCreatedEvent event = objectMapper.readValue(message, PropertyCreatedEvent.class);
            Property property = propertyRepository.findById(event.propertyId()).orElseGet(() -> {
                Property newProperty = new Property();
                newProperty.setId(event.propertyId());
                return newProperty;
            });
            syncProperty(property, event);
            propertyRepository.save(property);
            log.info("[KAFKA] Synchronized created property ID={}", event.propertyId());
        } catch (Exception e) {
            log.error("[KAFKA] Failed to process property-created event", e);
        }
    }

    @KafkaListener(topics = "property-updated", groupId = "appointment-service-group")
    public void onPropertyUpdated(String message) {
        log.info("[KAFKA] Received property-updated event: {}", message);
        try {
            PropertyUpdatedEvent event = objectMapper.readValue(message, PropertyUpdatedEvent.class);
            Property property = propertyRepository.findById(event.propertyId()).orElseGet(() -> {
                log.warn("[KAFKA] Property ID={} not found locally, creating new replica", event.propertyId());
                Property newProperty = new Property();
                newProperty.setId(event.propertyId());
                return newProperty;
            });
            syncProperty(property, event);
            propertyRepository.save(property);
            log.info("[KAFKA] Synchronized updated property ID={}", event.propertyId());
        } catch (Exception e) {
            log.error("[KAFKA] Failed to process property-updated event", e);
        }
    }

    @KafkaListener(topics = "property-deleted", groupId = "appointment-service-group")
    public void onPropertyDeleted(String message) {
        log.info("[KAFKA] Received property-deleted event: {}", message);
        try {
            PropertyDeletedEvent event = objectMapper.readValue(message, PropertyDeletedEvent.class);
            propertyRepository.findById(event.propertyId()).ifPresent(property -> {
                property.setStatus(Constants.PropertyStatusEnum.DELETED);
                propertyRepository.save(property);
                log.info("[KAFKA] Marked property ID={} as DELETED locally", event.propertyId());
            });
        } catch (Exception e) {
            log.error("[KAFKA] Failed to process property-deleted event", e);
        }
    }

    @KafkaListener(topics = "property-status-changed", groupId = "appointment-service-group")
    public void onPropertyStatusChanged(String message) {
        log.info("[KAFKA] Received property-status-changed event: {}", message);
        try {
            PropertyStatusChangedEvent event = objectMapper.readValue(message, PropertyStatusChangedEvent.class);
            propertyRepository.findById(event.propertyId()).ifPresent(property -> {
                property.setStatus(Constants.PropertyStatusEnum.valueOf(event.newStatus().toUpperCase()));
                propertyRepository.save(property);
                log.info("[KAFKA] Updated property ID={} status to {} locally", event.propertyId(), event.newStatus());
            });
        } catch (Exception e) {
            log.error("[KAFKA] Failed to process property-status-changed event", e);
        }
    }

    @KafkaListener(topics = "property-agent-assigned", groupId = "appointment-service-group")
    public void onPropertyAgentAssigned(String message) {
        log.info("[KAFKA] Received property-agent-assigned event: {}", message);
        try {
            Map<String, Object> eventMap = objectMapper.readValue(message, Map.class);
            UUID propertyId = UUID.fromString(eventMap.get("propertyId").toString());
            Object agentIdObj = eventMap.get("agentId");

            propertyRepository.findById(propertyId).ifPresent(property -> {
                if (agentIdObj != null) {
                    property.setAssignedAgentId(UUID.fromString(agentIdObj.toString()));
                } else {
                    property.setAssignedAgentId(null);
                }
                propertyRepository.save(property);
                log.info("[KAFKA] Synchronized property ID={} assigned agent to {}", propertyId, agentIdObj);
            });
        } catch (Exception e) {
            log.error("[KAFKA] Failed to process property-agent-assigned event", e);
        }
    }

    private void syncProperty(Property property, PropertyCreatedEvent event) {
        property.setOwnerId(event.ownerId());
        property.setAssignedAgentId(event.assignedAgentId());
        property.setWardId(event.wardId());
        property.setTitle(event.title());
        property.setDescription(event.description());
        property.setFullAddress(event.fullAddress());
        property.setArea(event.area());
        property.setRooms(event.rooms());
        property.setBathrooms(event.bathrooms());
        property.setFloors(event.floors());
        property.setBedrooms(event.bedrooms());
        property.setYearBuilt(event.yearBuilt());
        property.setPriceAmount(event.priceAmount());
        property.setPricePerSquareMeter(event.pricePerSquareMeter());
        property.setCommissionRate(event.commissionRate());
        property.setServiceFeeAmount(event.serviceFeeAmount());
        property.setServiceFeeCollectedAmount(event.serviceFeeCollectedAmount());
        applyPropertyType(property, event.propertyTypeId());
        applyEnums(property, event.transactionType(), event.status(), event.houseOrientation(), event.balconyOrientation());
    }

    private void syncProperty(Property property, PropertyUpdatedEvent event) {
        property.setOwnerId(event.ownerId());
        property.setAssignedAgentId(event.assignedAgentId());
        property.setWardId(event.wardId());
        property.setTitle(event.title());
        property.setDescription(event.description());
        property.setFullAddress(event.fullAddress());
        property.setArea(event.area());
        property.setRooms(event.rooms());
        property.setBathrooms(event.bathrooms());
        property.setFloors(event.floors());
        property.setBedrooms(event.bedrooms());
        property.setYearBuilt(event.yearBuilt());
        property.setPriceAmount(event.priceAmount());
        property.setPricePerSquareMeter(event.pricePerSquareMeter());
        property.setCommissionRate(event.commissionRate());
        property.setServiceFeeAmount(event.serviceFeeAmount());
        property.setServiceFeeCollectedAmount(event.serviceFeeCollectedAmount());
        applyPropertyType(property, event.propertyTypeId());
        applyEnums(property, event.transactionType(), event.status(), event.houseOrientation(), event.balconyOrientation());
    }

    private void applyPropertyType(Property property, UUID propertyTypeId) {
        if (propertyTypeId != null) {
            property.setPropertyType(entityManager.getReference(PropertyType.class, propertyTypeId));
        }
    }

    private void applyEnums(Property property, String transactionType, String status, String houseOrientation, String balconyOrientation) {
        if (transactionType != null) {
            property.setTransactionType(Constants.TransactionTypeEnum.valueOf(transactionType.toUpperCase()));
        }
        if (status != null) {
            property.setStatus(Constants.PropertyStatusEnum.valueOf(status.toUpperCase()));
        }
        if (houseOrientation != null) {
            property.setHouseOrientation(Constants.OrientationEnum.valueOf(houseOrientation.toUpperCase()));
        }
        if (balconyOrientation != null) {
            property.setBalconyOrientation(Constants.OrientationEnum.valueOf(balconyOrientation.toUpperCase()));
        }
    }
}
