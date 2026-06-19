package com.se.bds.core.property.internal.v2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.core.property.internal.domain.model.PropertyStatus;
import com.se.bds.core.property.internal.domain.model.TransactionType;
import com.se.bds.core.property.internal.v2.domain.PropertyAggregate;
import com.se.bds.core.property.internal.v2.domain.PropertyEventEntry;
import com.se.bds.core.property.internal.v2.repository.PropertyEventStoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service to handle write commands for the event-sourced Property aggregate.
 */
@Service
@RequiredArgsConstructor
public class PropertyCommandHandler {

    private final PropertyEventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public record CreatePropertyCommand(
        UUID propertyId,
        UUID ownerId,
        UUID wardId,
        String title,
        String description,
        BigDecimal priceAmount,
        TransactionType transactionType
    ) {}

    public record UpdatePropertyStatusCommand(
        UUID propertyId,
        PropertyStatus newStatus
    ) {}

    /**
     * Reconstructs the current state of a property from historical events.
     */
    public PropertyAggregate getProperty(UUID propertyId) {
        List<PropertyEventEntry> entries = eventStoreRepository.findByPropertyIdOrderByCreatedAtAsc(propertyId);
        if (entries.isEmpty()) {
            return null;
        }
        PropertyAggregate aggregate = new PropertyAggregate();
        aggregate.replay(entries, objectMapper);
        return aggregate;
    }

    /**
     * Process CreatePropertyCommand.
     */
    @Transactional
    public UUID handleCreate(CreatePropertyCommand command) {
        PropertyAggregate aggregate = new PropertyAggregate();
        aggregate.create(
            command.propertyId(),
            command.ownerId(),
            command.wardId(),
            command.title(),
            command.description(),
            command.priceAmount(),
            command.transactionType()
        );

        saveEvents(aggregate);
        return aggregate.getId();
    }

    /**
     * Process UpdatePropertyStatusCommand.
     */
    @Transactional
    public void handleUpdateStatus(UpdatePropertyStatusCommand command) {
        List<PropertyEventEntry> entries = eventStoreRepository.findByPropertyIdOrderByCreatedAtAsc(command.propertyId());
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Property not found with ID: " + command.propertyId());
        }

        PropertyAggregate aggregate = new PropertyAggregate();
        aggregate.replay(entries, objectMapper);

        aggregate.updateStatus(command.newStatus());

        saveEvents(aggregate);
    }

    private void saveEvents(PropertyAggregate aggregate) {
        int nextVersion = aggregate.getVersion();
        for (Object event : aggregate.getUncommittedEvents()) {
            nextVersion++;
            String payload;
            try {
                payload = objectMapper.writeValueAsString(event);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize event: " + event.getClass().getSimpleName(), e);
            }

            PropertyEventEntry entry = PropertyEventEntry.builder()
                .eventId(UUID.randomUUID())
                .propertyId(aggregate.getId())
                .eventType(event.getClass().getSimpleName())
                .payload(payload)
                .version(nextVersion)
                .createdAt(LocalDateTime.now())
                .build();

            eventStoreRepository.save(entry);

            // Publish the event through Spring Application Event Publisher
            eventPublisher.publishEvent(event);
        }
        aggregate.clearUncommittedEvents();
    }
}
