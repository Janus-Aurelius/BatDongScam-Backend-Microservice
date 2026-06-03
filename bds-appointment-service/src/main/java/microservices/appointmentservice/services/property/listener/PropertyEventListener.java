package microservices.appointmentservice.services.property.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.event.PropertyStatusChangedEvent;
import microservices.appointmentservice.client.CoreServiceClient;
import microservices.appointmentservice.entities.property.Property;
import microservices.appointmentservice.repositories.PropertyRepository;
import microservices.appointmentservice.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class PropertyEventListener {

    private final PropertyRepository propertyRepository;
    private final CoreServiceClient coreServiceClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "property-created", groupId = "appointment-service-group")
    public void onPropertyCreated(String message) {
        log.info("[KAFKA] Received property-created event: {}", message);
        try {
            // event is serialized by core as PropertyCreatedIntegrationEvent
            Map<String, Object> eventMap = objectMapper.readValue(message, Map.class);
            UUID propertyId = UUID.fromString(eventMap.get("propertyId").toString());
            String title = (String) eventMap.get("title");
            UUID ownerId = UUID.fromString(eventMap.get("ownerId").toString());
            String transactionType = (String) eventMap.get("transactionType");

            Property property = propertyRepository.findById(propertyId).orElse(null);
            if (property == null) {
                property = new Property();
                property.setId(propertyId);
            }
            property.setTitle(title != null ? title : "Unknown Property");
            property.setOwnerId(ownerId);
            if (transactionType != null) {
                property.setTransactionType(Constants.TransactionTypeEnum.valueOf(transactionType.toUpperCase()));
            }

            // Sync full details from core-macroservice
            Map<String, Object> details = coreServiceClient.getPropertyDetails(propertyId);
            syncProperty(property, details);

            propertyRepository.save(property);
            log.info("[KAFKA] Synchronized created property ID={}", propertyId);
        } catch (Exception e) {
            log.error("[KAFKA] Failed to process property-created event", e);
        }
    }

    @KafkaListener(topics = "property-updated", groupId = "appointment-service-group")
    public void onPropertyUpdated(String message) {
        log.info("[KAFKA] Received property-updated event: {}", message);
        try {
            Map<String, Object> eventMap = objectMapper.readValue(message, Map.class);
            UUID propertyId = UUID.fromString(eventMap.get("propertyId").toString());

            Property property = propertyRepository.findById(propertyId).orElse(null);
            if (property == null) {
                log.warn("[KAFKA] Property ID={} not found locally, creating new replica", propertyId);
                property = new Property();
                property.setId(propertyId);
            }

            Map<String, Object> details = coreServiceClient.getPropertyDetails(propertyId);
            syncProperty(property, details);

            propertyRepository.save(property);
            log.info("[KAFKA] Synchronized updated property ID={}", propertyId);
        } catch (Exception e) {
            log.error("[KAFKA] Failed to process property-updated event", e);
        }
    }

    @KafkaListener(topics = "property-deleted", groupId = "appointment-service-group")
    public void onPropertyDeleted(String message) {
        log.info("[KAFKA] Received property-deleted event: {}", message);
        try {
            Map<String, Object> eventMap = objectMapper.readValue(message, Map.class);
            UUID propertyId = UUID.fromString(eventMap.get("propertyId").toString());

            propertyRepository.findById(propertyId).ifPresent(property -> {
                property.setStatus(Constants.PropertyStatusEnum.DELETED);
                propertyRepository.save(property);
                log.info("[KAFKA] Marked property ID={} as DELETED locally", propertyId);
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

    private void syncProperty(Property property, Map<String, Object> details) {
        if (details == null) return;
        if (details.get("title") != null) property.setTitle((String) details.get("title"));
        if (details.get("description") != null) property.setDescription((String) details.get("description"));
        if (details.get("fullAddress") != null) property.setFullAddress((String) details.get("fullAddress"));

        if (details.get("area") != null) property.setArea(new BigDecimal(details.get("area").toString()));
        if (details.get("rooms") != null) property.setRooms(Integer.valueOf(details.get("rooms").toString()));
        if (details.get("bathrooms") != null) property.setBathrooms(Integer.valueOf(details.get("bathrooms").toString()));
        if (details.get("floors") != null) property.setFloors(Integer.valueOf(details.get("floors").toString()));
        if (details.get("bedrooms") != null) property.setBedrooms(Integer.valueOf(details.get("bedrooms").toString()));
        if (details.get("yearBuilt") != null) property.setYearBuilt(Integer.valueOf(details.get("yearBuilt").toString()));

        if (details.get("priceAmount") != null) property.setPriceAmount(new BigDecimal(details.get("priceAmount").toString()));
        if (details.get("pricePerSquareMeter") != null) property.setPricePerSquareMeter(new BigDecimal(details.get("pricePerSquareMeter").toString()));
        if (details.get("commissionRate") != null) property.setCommissionRate(new BigDecimal(details.get("commissionRate").toString()));
        if (details.get("serviceFeeAmount") != null) property.setServiceFeeAmount(new BigDecimal(details.get("serviceFeeAmount").toString()));
        if (details.get("serviceFeeCollectedAmount") != null) property.setServiceFeeCollectedAmount(new BigDecimal(details.get("serviceFeeCollectedAmount").toString()));

        if (details.get("ownerId") != null) property.setOwnerId(UUID.fromString(details.get("ownerId").toString()));
        if (details.get("assignedAgentId") != null) property.setAssignedAgentId(UUID.fromString(details.get("assignedAgentId").toString()));
        if (details.get("wardId") != null) property.setWardId(UUID.fromString(details.get("wardId").toString()));

        if (details.get("transactionType") != null) {
            property.setTransactionType(Constants.TransactionTypeEnum.valueOf(details.get("transactionType").toString().toUpperCase()));
        }
        if (details.get("status") != null) {
            property.setStatus(Constants.PropertyStatusEnum.valueOf(details.get("status").toString().toUpperCase()));
        }
        if (details.get("houseOrientation") != null) {
            property.setHouseOrientation(Constants.OrientationEnum.valueOf(details.get("houseOrientation").toString().toUpperCase()));
        }
        if (details.get("balconyOrientation") != null) {
            property.setBalconyOrientation(Constants.OrientationEnum.valueOf(details.get("balconyOrientation").toString().toUpperCase()));
        }
    }
}
