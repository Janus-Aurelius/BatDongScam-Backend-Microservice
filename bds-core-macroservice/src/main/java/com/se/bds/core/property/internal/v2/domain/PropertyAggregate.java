package com.se.bds.core.property.internal.v2.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.message.validation.MSG12;
import com.se.bds.core.property.internal.domain.model.PropertyStatus;
import com.se.bds.core.property.internal.domain.model.TransactionType;
import com.se.bds.core.property.internal.v2.event.PropertyCreatedDomainEvent;
import com.se.bds.core.property.internal.v2.event.PropertyStatusChangedDomainEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Event-sourced Aggregate Root for Property.
 */
@Getter
public class PropertyAggregate {

    private UUID id;
    private UUID ownerId;
    private UUID wardId;
    private String title;
    private String description;
    private BigDecimal priceAmount;
    private TransactionType transactionType;
    private PropertyStatus status;
    private int version = 0;

    private final List<Object> uncommittedEvents = new ArrayList<>();

    private static final Set<PropertyStatus> TERMINAL_STATUSES =
            EnumSet.of(PropertyStatus.SOLD, PropertyStatus.DELETED, PropertyStatus.REMOVED);

    public PropertyAggregate() {
    }

    /**
     * Reconstructs the aggregate state by replaying a stream of event entries.
     */
    public void replay(List<PropertyEventEntry> entries, ObjectMapper objectMapper) {
        for (PropertyEventEntry entry : entries) {
            try {
                if (entry.getEventType().equals(PropertyCreatedDomainEvent.class.getSimpleName())) {
                    PropertyCreatedDomainEvent event = objectMapper.readValue(entry.getPayload(), PropertyCreatedDomainEvent.class);
                    apply(event);
                } else if (entry.getEventType().equals(PropertyStatusChangedDomainEvent.class.getSimpleName())) {
                    PropertyStatusChangedDomainEvent event = objectMapper.readValue(entry.getPayload(), PropertyStatusChangedDomainEvent.class);
                    apply(event);
                }
                this.version = entry.getVersion();
            } catch (Exception e) {
                throw new RuntimeException("Failed to replay event payload: " + entry.getEventType(), e);
            }
        }
    }

    private void apply(PropertyCreatedDomainEvent event) {
        this.id = event.propertyId();
        this.ownerId = event.ownerId();
        this.wardId = event.wardId();
        this.title = event.title();
        this.description = event.description();
        this.priceAmount = event.priceAmount();
        this.transactionType = event.transactionType();
        this.status = PropertyStatus.PENDING;
    }

    private void apply(PropertyStatusChangedDomainEvent event) {
        this.status = event.newStatus();
    }

    /**
     * Executes creation command.
     */
    public void create(UUID propertyId, UUID ownerId, UUID wardId, String title, String description, BigDecimal priceAmount, TransactionType transactionType) {
        PropertyCreatedDomainEvent event = new PropertyCreatedDomainEvent(
            propertyId, ownerId, wardId, title, description, priceAmount, transactionType, LocalDateTime.now()
        );
        apply(event);
        uncommittedEvents.add(event);
    }

    /**
     * Executes update status command.
     */
    public void updateStatus(PropertyStatus newStatus) {
        if (this.status == null) {
            throw new IllegalStateException("Aggregate is not initialized");
        }
        if (TERMINAL_STATUSES.contains(this.status) && newStatus != PropertyStatus.AVAILABLE) {
            throw new BusinessException(MSG12.CODE, MSG12.MESSAGE);
        }
        PropertyStatus oldStatus = this.status;
        PropertyStatusChangedDomainEvent event = new PropertyStatusChangedDomainEvent(
            this.id, oldStatus, newStatus, LocalDateTime.now()
        );
        apply(event);
        uncommittedEvents.add(event);
    }

    /**
     * Clear all uncommitted events.
     */
    public void clearUncommittedEvents() {
        this.uncommittedEvents.clear();
    }
}
