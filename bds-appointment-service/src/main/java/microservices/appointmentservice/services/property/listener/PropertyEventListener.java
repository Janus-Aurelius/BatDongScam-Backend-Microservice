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

/**
 * REFACTORED: loại bỏ CoreServiceClient.
 *
 * Trước: nhận event → gọi coreServiceClient.getPropertyDetails(id) → sync
 * Sau:   nhận Fat Event → đọc trực tiếp từ event payload → sync
 *
 * Kết quả: 0 Feign call, không còn N+1 pattern.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PropertyEventListener {

    private final PropertyRepository propertyRepository;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    /**
     * CHANGED: Parse PropertyCreatedEvent (Fat Event từ bds-common).
     * Không cần gọi coreServiceClient.getPropertyDetails() nữa.
     */
    @KafkaListener(topics = "property-created", groupId = "appointment-service-group")
    public void onPropertyCreated(String message) {
        log.info("[KAFKA] Received property-created event");
        try {
            PropertyCreatedEvent event = objectMapper.readValue(message, PropertyCreatedEvent.class);

            Property property = propertyRepository.findById(event.propertyId())
                    .orElse(new Property());

            property.setId(event.propertyId());
            syncFromCreatedEvent(property, event);

            propertyRepository.save(property);
            log.info("[KAFKA] Synchronized created property ID={}", event.propertyId());
        } catch (Exception e) {
            log.error("[KAFKA] Failed to process property-created event", e);
            throw new RuntimeException("Failed to process property-created event", e);
        }
    }

    /**
     * CHANGED: Parse PropertyUpdatedEvent (Fat Event từ bds-common).
     */
    @KafkaListener(topics = "property-updated", groupId = "appointment-service-group")
    public void onPropertyUpdated(String message) {
        log.info("[KAFKA] Received property-updated event");
        try {
            PropertyUpdatedEvent event = objectMapper.readValue(message, PropertyUpdatedEvent.class);

            Property property = propertyRepository.findById(event.propertyId())
                    .orElseGet(() -> {
                        log.warn("[KAFKA] Property ID={} not found locally, creating new replica", event.propertyId());
                        Property p = new Property();
                        p.setId(event.propertyId());
                        return p;
                    });

            syncFromUpdatedEvent(property, event);

            propertyRepository.save(property);
            log.info("[KAFKA] Synchronized updated property ID={}", event.propertyId());
        } catch (Exception e) {
            log.error("[KAFKA] Failed to process property-updated event", e);
            throw new RuntimeException("Failed to process property-updated event", e);
        }
    }

    @KafkaListener(topics = "property-deleted", groupId = "appointment-service-group")
    public void onPropertyDeleted(String message) {
        log.info("[KAFKA] Received property-deleted event");
        try {
            PropertyDeletedEvent event = objectMapper.readValue(message, PropertyDeletedEvent.class);
            propertyRepository.findById(event.propertyId()).ifPresent(property -> {
                property.setStatus(Constants.PropertyStatusEnum.DELETED);
                propertyRepository.save(property);
                log.info("[KAFKA] Marked property ID={} as DELETED locally", event.propertyId());
            });
        } catch (Exception e) {
            log.error("[KAFKA] Failed to process property-deleted event", e);
            throw new RuntimeException("Failed to process property-deleted event", e);
        }
    }

    @KafkaListener(topics = "property-status-changed", groupId = "appointment-service-group")
    public void onPropertyStatusChanged(String message) {
        log.info("[KAFKA] Received property-status-changed event");
        try {
            PropertyStatusChangedEvent event = objectMapper.readValue(message, PropertyStatusChangedEvent.class);
            propertyRepository.findById(event.propertyId()).ifPresent(property -> {
                property.setStatus(Constants.PropertyStatusEnum.valueOf(event.newStatus().toUpperCase()));
                propertyRepository.save(property);
                log.info("[KAFKA] Updated property ID={} status to {}", event.propertyId(), event.newStatus());
            });
        } catch (Exception e) {
            log.error("[KAFKA] Failed to process property-status-changed event", e);
            throw new RuntimeException("Failed to process property-status-changed event", e);
        }
    }

    @KafkaListener(topics = "property-agent-assigned", groupId = "appointment-service-group")
    public void onPropertyAgentAssigned(String message) {
        log.info("[KAFKA] Received property-agent-assigned event");
        try {
            Map<String, Object> eventMap = objectMapper.readValue(message, Map.class);
            UUID propertyId = UUID.fromString(eventMap.get("propertyId").toString());
            Object agentIdObj = eventMap.get("agentId");
            propertyRepository.findById(propertyId).ifPresent(property -> {
                property.setAssignedAgentId(agentIdObj != null ? UUID.fromString(agentIdObj.toString()) : null);
                propertyRepository.save(property);
                log.info("[KAFKA] Synced property ID={} assigned agent to {}", propertyId, agentIdObj);
            });
        } catch (Exception e) {
            log.error("[KAFKA] Failed to process property-agent-assigned event", e);
            throw new RuntimeException("Failed to process property-agent-assigned event", e);
        }
    }

    // ======================== HELPERS ========================

    private void syncFromCreatedEvent(Property property, PropertyCreatedEvent event) {
        if (event.title() != null) property.setTitle(event.title());
        if (event.description() != null) property.setDescription(event.description());
        if (event.fullAddress() != null) property.setFullAddress(event.fullAddress());
        if (event.area() != null) property.setArea(event.area());
        if (event.rooms() != null) property.setRooms(event.rooms());
        if (event.bathrooms() != null) property.setBathrooms(event.bathrooms());
        if (event.floors() != null) property.setFloors(event.floors());
        if (event.bedrooms() != null) property.setBedrooms(event.bedrooms());
        if (event.yearBuilt() != null) property.setYearBuilt(event.yearBuilt());
        if (event.priceAmount() != null) property.setPriceAmount(event.priceAmount());
        if (event.pricePerSquareMeter() != null) property.setPricePerSquareMeter(event.pricePerSquareMeter());
        if (event.commissionRate() != null) property.setCommissionRate(event.commissionRate());
        if (event.serviceFeeAmount() != null) property.setServiceFeeAmount(event.serviceFeeAmount());
        if (event.serviceFeeCollectedAmount() != null) property.setServiceFeeCollectedAmount(event.serviceFeeCollectedAmount());
        if (event.propertyTypeId() != null) {
            PropertyType pt = new PropertyType();
            pt.setId(event.propertyTypeId());
            property.setPropertyType(pt);
        }
        if (event.ownerId() != null) property.setOwnerId(event.ownerId());
        if (event.assignedAgentId() != null) property.setAssignedAgentId(event.assignedAgentId());
        if (event.wardId() != null) property.setWardId(event.wardId());
        if (event.transactionType() != null) {
            property.setTransactionType(Constants.TransactionTypeEnum.valueOf(event.transactionType().toUpperCase()));
        }
        if (event.status() != null) {
            property.setStatus(Constants.PropertyStatusEnum.valueOf(event.status().toUpperCase()));
        }
        if (event.houseOrientation() != null) {
            property.setHouseOrientation(Constants.OrientationEnum.valueOf(event.houseOrientation().toUpperCase()));
        }
        if (event.balconyOrientation() != null) {
            property.setBalconyOrientation(Constants.OrientationEnum.valueOf(event.balconyOrientation().toUpperCase()));
        }
    }

    private void syncFromUpdatedEvent(Property property, PropertyUpdatedEvent event) {
        if (event.title() != null) property.setTitle(event.title());
        if (event.description() != null) property.setDescription(event.description());
        if (event.fullAddress() != null) property.setFullAddress(event.fullAddress());
        if (event.area() != null) property.setArea(event.area());
        if (event.rooms() != null) property.setRooms(event.rooms());
        if (event.bathrooms() != null) property.setBathrooms(event.bathrooms());
        if (event.floors() != null) property.setFloors(event.floors());
        if (event.bedrooms() != null) property.setBedrooms(event.bedrooms());
        if (event.yearBuilt() != null) property.setYearBuilt(event.yearBuilt());
        if (event.priceAmount() != null) property.setPriceAmount(event.priceAmount());
        if (event.pricePerSquareMeter() != null) property.setPricePerSquareMeter(event.pricePerSquareMeter());
        if (event.commissionRate() != null) property.setCommissionRate(event.commissionRate());
        if (event.serviceFeeAmount() != null) property.setServiceFeeAmount(event.serviceFeeAmount());
        if (event.serviceFeeCollectedAmount() != null) property.setServiceFeeCollectedAmount(event.serviceFeeCollectedAmount());
        if (event.propertyTypeId() != null) {
            PropertyType pt = new PropertyType();
            pt.setId(event.propertyTypeId());
            property.setPropertyType(pt);
        }
        if (event.ownerId() != null) property.setOwnerId(event.ownerId());
        if (event.assignedAgentId() != null) property.setAssignedAgentId(event.assignedAgentId());
        if (event.wardId() != null) property.setWardId(event.wardId());
        if (event.transactionType() != null) {
            property.setTransactionType(Constants.TransactionTypeEnum.valueOf(event.transactionType().toUpperCase()));
        }
        if (event.status() != null) {
            property.setStatus(Constants.PropertyStatusEnum.valueOf(event.status().toUpperCase()));
        }
        if (event.houseOrientation() != null) {
            property.setHouseOrientation(Constants.OrientationEnum.valueOf(event.houseOrientation().toUpperCase()));
        }
        if (event.balconyOrientation() != null) {
            property.setBalconyOrientation(Constants.OrientationEnum.valueOf(event.balconyOrientation().toUpperCase()));
        }
    }
}