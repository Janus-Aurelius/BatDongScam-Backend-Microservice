package com.se.bds.core.property.internal.application.port.out;

import com.se.bds.common.event.PropertyCreatedEvent;
import com.se.bds.common.event.PropertyDeletedEvent;
import com.se.bds.common.event.PropertyUpdatedEvent;

/**
 * Port for publishing integration events related to properties.
 */
public interface MessagePublisherPort {
    void publishPropertyCreated(PropertyCreatedEvent event);
    void publishPropertyUpdated(PropertyUpdatedEvent event);
    void publishPropertyDeleted(PropertyDeletedEvent event);
}
