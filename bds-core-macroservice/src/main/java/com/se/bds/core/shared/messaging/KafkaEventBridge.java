package com.se.bds.core.shared.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.event.PropertySearchedEvent;
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

/**
 * Real Spring Kafka message bridge.
 * Listens to internal Spring Application events and forwards them to Kafka.
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
        publish("property-created", event.propertyId().toString(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePropertyUpdated(PropertyUpdatedIntegrationEvent event) {
        publish("property-updated", event.propertyId().toString(), event);
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
        publish("contract-status-changed", event.contractId().value().toString(), event);
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
