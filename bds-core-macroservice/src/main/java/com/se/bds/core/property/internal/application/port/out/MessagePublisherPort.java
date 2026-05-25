package com.se.bds.core.property.internal.application.port.out;

import com.se.bds.core.property.api.event.PropertyCreatedIntegrationEvent;
import com.se.bds.core.property.api.event.PropertyDeletedIntegrationEvent;
import com.se.bds.core.property.api.event.PropertyUpdatedIntegrationEvent;

/**
 * Port for publishing integration events related to properties.
 */
public interface MessagePublisherPort {
    void publishPropertyCreated(PropertyCreatedIntegrationEvent event);
    void publishPropertyUpdated(PropertyUpdatedIntegrationEvent event);
    void publishPropertyDeleted(PropertyDeletedIntegrationEvent event);
}
