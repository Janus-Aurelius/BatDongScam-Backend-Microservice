package com.se.bds.core.property.internal.v2.projection;

import com.se.bds.core.property.internal.domain.model.PropertyStatus;
import com.se.bds.core.property.internal.v2.domain.PropertyReadModel;
import com.se.bds.core.property.internal.v2.event.PropertyCreatedDomainEvent;
import com.se.bds.core.property.internal.v2.event.PropertyStatusChangedDomainEvent;
import com.se.bds.core.property.internal.v2.repository.PropertyReadModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles incoming domain events asynchronously to maintain the read-optimized projection views.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PropertyReadProjectionHandler {

    private final PropertyReadModelRepository readModelRepository;

    /**
     * Projects PropertyCreatedDomainEvent to property_read_views.
     */
    @Async("bdsTaskExecutor")
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(PropertyCreatedDomainEvent event) {
        log.info("[Projection] Processing PropertyCreatedDomainEvent for property ID: {}", event.propertyId());
        
        PropertyReadModel readModel = PropertyReadModel.builder()
                .propertyId(event.propertyId())
                .ownerId(event.ownerId())
                .wardId(event.wardId())
                .title(event.title())
                .description(event.description())
                .priceAmount(event.priceAmount())
                .transactionType(event.transactionType())
                .status(PropertyStatus.PENDING)
                .createdAt(event.occurredAt())
                .updatedAt(event.occurredAt())
                .build();
        
        readModelRepository.save(readModel);
        log.info("[Projection] Successfully created flat view for property ID: {}", event.propertyId());
    }

    /**
     * Projects PropertyStatusChangedDomainEvent to property_read_views.
     */
    @Async("bdsTaskExecutor")
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(PropertyStatusChangedDomainEvent event) {
        log.info("[Projection] Processing PropertyStatusChangedDomainEvent for property ID: {}", event.propertyId());
        
        readModelRepository.findById(event.propertyId()).ifPresentOrElse(readModel -> {
            readModel.setStatus(event.newStatus());
            readModel.setUpdatedAt(event.occurredAt());
            readModelRepository.save(readModel);
            log.info("[Projection] Updated status to {} for property ID: {}", event.newStatus(), event.propertyId());
        }, () -> {
            log.error("[Projection] Failed to find read model view for property ID: {}", event.propertyId());
        });
    }
}
