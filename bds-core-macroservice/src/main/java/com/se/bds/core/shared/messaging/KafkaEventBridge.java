package com.se.bds.core.shared.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.event.ContractStatusChangedFatEvent;
import com.se.bds.common.event.PropertyCreatedEvent;
import com.se.bds.common.event.PropertySearchedEvent;
import com.se.bds.common.event.PropertyUpdatedEvent;
import com.se.bds.core.property.api.event.*;
import com.se.bds.core.shared.event.ContractStatusChangedEvent;
import com.se.bds.core.shared.event.PaymentCompletedEvent;
import com.se.bds.core.transaction.api.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

/**
 * Kafka message bridge - forwards internal Spring Application Events to Kafka.
 *
 * MIGRATION NOTE:
 * - property-created / property-updated bây giờ gửi Fat Event (PropertyCreatedEvent /
 *   PropertyUpdatedEvent từ bds-common) thay vì PropertyCreatedIntegrationEvent nội bộ.
 *   Consumer (appointment, moderation) sẽ có đủ data mà không cần gọi Feign.
 * - contract-status-changed bây giờ gửi ContractStatusChangedFatEvent (bds-common) thay
 *   vì ContractStatusChangedEvent nội bộ. Notification-service lấy customerId từ event,
 *   không cần gọi coreServiceClient.getContractById() nữa.
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaEventBridge {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @org.springframework.context.event.EventListener
    public void handlePropertySearched(PropertySearchedEvent event) {
        publish("property-searched", event.propertyId() != null ? event.propertyId().toString() : "global", event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePropertyCreated(PropertyCreatedIntegrationEvent event) {
        PropertyCreatedEvent fatEvent = new PropertyCreatedEvent(
                event.propertyId().value(),
                event.title(),
                event.description(),
                event.fullAddress(),
                event.priceAmount(),
                event.pricePerSquareMeter(),
                event.commissionRate(),
                event.serviceFeeAmount(),
                event.area(),
                event.rooms(),
                event.bathrooms(),
                event.floors(),
                event.bedrooms(),
                event.yearBuilt(),
                event.transactionType() != null ? event.transactionType().name() : null,
                event.status() != null ? event.status().name() : null,
                event.houseOrientation() != null ? event.houseOrientation().name() : null,
                event.balconyOrientation() != null ? event.balconyOrientation().name() : null,
                event.ownerId(),
                event.assignedAgentId(),
                event.wardId(),
                event.thumbnailUrl(),
                Instant.now()
        );
        publish("property-created", event.propertyId().value().toString(), fatEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePropertyUpdated(PropertyUpdatedIntegrationEvent event) {
        PropertyUpdatedEvent fatEvent = new PropertyUpdatedEvent(
                event.propertyId().value(),
                event.title(),
                event.description(),
                event.fullAddress(),
                event.priceAmount(),
                event.pricePerSquareMeter(),
                event.commissionRate(),
                event.serviceFeeAmount(),
                event.area(),
                event.rooms(),
                event.bathrooms(),
                event.floors(),
                event.bedrooms(),
                event.yearBuilt(),
                event.transactionType() != null ? event.transactionType().name() : null,
                event.status() != null ? event.status().name() : null,
                event.houseOrientation() != null ? event.houseOrientation().name() : null,
                event.balconyOrientation() != null ? event.balconyOrientation().name() : null,
                event.ownerId(),
                event.assignedAgentId(),
                event.wardId(),
                event.thumbnailUrl(),
                Instant.now()
        );
        publish("property-updated", event.propertyId().value().toString(), fatEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePropertyDeleted(PropertyDeletedIntegrationEvent event) {
        publish("property-deleted", event.propertyId().toString(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePropertyStatusChanged(com.se.bds.core.property.api.event.PropertyStatusChangedEvent event) {
        com.se.bds.common.event.PropertyStatusChangedEvent commonEvent = new com.se.bds.common.event.PropertyStatusChangedEvent(
                event.propertyId().value(),
                event.oldStatus(),
                event.newStatus()
        );
        publish("property-status-changed", event.propertyId().value().toString(), commonEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePropertyAgentAssigned(PropertyAgentAssignedEvent event) {
        publish("property-agent-assigned", event.propertyId().toString(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePropertyServiceFeeCollected(PropertyServiceFeeCollectedEvent event) {
        publish("property-service-fee-collected", event.propertyId().toString(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleContractStatusChanged(ContractStatusChangedEvent event) {
        ContractStatusChangedFatEvent fatEvent = new ContractStatusChangedFatEvent(
                event.contractId().value(),
                event.propertyId(),
                event.contractType(),
                event.oldStatus(),
                event.newStatus(),
                event.customerId(),
                event.ownerId(),
                event.propertyTitle(),
                event.occurredAt()
        );
        publish("contract-status-changed", event.contractId().value().toString(), fatEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleContractCancelled(ContractCancelledEvent event) {
        publish("contract-cancelled", event.contractId().toString(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleContractActivated(ContractActivatedEvent event) {
        publish("contract-activated", event.contractId().toString(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        publish("payment-completed", event.paymentId().value().toString(), event);
    }

    private void publish(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            log.info("[KafkaEventBridge] Bridging event to Kafka: topic={}, key={}, payload={}", topic, key, payload);
            kafkaTemplate.send(topic, key, payload);
        } catch (Exception e) {
            log.error("[KafkaEventBridge] Failed to bridge event to Kafka: topic={}, key={}", topic, key, e);
        }
    }
}
