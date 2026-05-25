package com.se.bds.core.property.internal.adapter.out.external;

import com.se.bds.core.property.api.event.PropertyCreatedIntegrationEvent;
import com.se.bds.core.property.api.event.PropertyDeletedIntegrationEvent;
import com.se.bds.core.property.api.event.PropertyUpdatedIntegrationEvent;
import com.se.bds.core.property.internal.application.port.out.MessagePublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Adapter for publishing integration events.
 * Currently uses Spring ApplicationEventPublisher as a placeholder.
 * In a real microservice environment, this would publish to Kafka, RabbitMQ, etc.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PropertyMessagePublisherAdapter implements MessagePublisherPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publishPropertyCreated(PropertyCreatedIntegrationEvent event) {
        log.info("Publishing PropertyCreatedIntegrationEvent for property: {}", event.propertyId());
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishPropertyUpdated(PropertyUpdatedIntegrationEvent event) {
        log.info("Publishing PropertyUpdatedIntegrationEvent for property: {}", event.propertyId());
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishPropertyDeleted(PropertyDeletedIntegrationEvent event) {
        log.info("Publishing PropertyDeletedIntegrationEvent for property: {}", event.propertyId());
        eventPublisher.publishEvent(event);
    }
}
