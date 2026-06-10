package com.se.bds.core.property.internal.adapter.out.external;

import com.se.bds.common.event.PropertyCreatedEvent;
import com.se.bds.common.event.PropertyDeletedEvent;
import com.se.bds.common.event.PropertyUpdatedEvent;
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
    public void publishPropertyCreated(PropertyCreatedEvent event) {
        log.info("Publishing PropertyCreatedEvent for property: {}", event.propertyId());
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishPropertyUpdated(PropertyUpdatedEvent event) {
        log.info("Publishing PropertyUpdatedEvent for property: {}", event.propertyId());
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishPropertyDeleted(PropertyDeletedEvent event) {
        log.info("Publishing PropertyDeletedEvent for property: {}", event.propertyId());
        eventPublisher.publishEvent(event);
    }
}
