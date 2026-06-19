package com.se.bds.core.property.internal.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.exception.BusinessException;
import com.se.bds.core.property.internal.domain.model.PropertyStatus;
import com.se.bds.core.property.internal.domain.model.TransactionType;
import com.se.bds.core.property.internal.v2.domain.PropertyAggregate;
import com.se.bds.core.property.internal.v2.domain.PropertyEventEntry;
import com.se.bds.core.property.internal.v2.domain.PropertyReadModel;
import com.se.bds.core.property.internal.v2.event.PropertyCreatedDomainEvent;
import com.se.bds.core.property.internal.v2.event.PropertyStatusChangedDomainEvent;
import com.se.bds.core.property.internal.v2.projection.PropertyReadProjectionHandler;
import com.se.bds.core.property.internal.v2.repository.PropertyEventStoreRepository;
import com.se.bds.core.property.internal.v2.repository.PropertyReadModelRepository;
import com.se.bds.core.property.internal.v2.service.PropertyCommandHandler;
import com.se.bds.core.property.internal.v2.service.PropertyCommandHandler.CreatePropertyCommand;
import com.se.bds.core.property.internal.v2.service.PropertyCommandHandler.UpdatePropertyStatusCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PropertyCQRSIntegrationTest {

    @Mock
    private PropertyEventStoreRepository eventStoreRepository;

    @Mock
    private PropertyReadModelRepository readModelRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ObjectMapper objectMapper;
    private PropertyCommandHandler commandHandler;
    private PropertyReadProjectionHandler projectionHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        commandHandler = new PropertyCommandHandler(eventStoreRepository, objectMapper, eventPublisher);
        projectionHandler = new PropertyReadProjectionHandler(readModelRepository);
    }

    @Test
    void testPropertyCQRSFlow() throws Exception {
        UUID propertyId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID wardId = UUID.randomUUID();

        // 1. Execute CreatePropertyCommand
        CreatePropertyCommand createCommand = new CreatePropertyCommand(
                propertyId,
                ownerId,
                wardId,
                "Beautiful Apartment in Saigon",
                "A luxurious apartment with stunning city views.",
                new BigDecimal("3500.00"),
                TransactionType.RENTAL
        );

        UUID createdId = commandHandler.handleCreate(createCommand);
        assertEquals(propertyId, createdId);

        // Verify Event was saved to the Event Store
        ArgumentCaptor<PropertyEventEntry> firstEntryCaptor = ArgumentCaptor.forClass(PropertyEventEntry.class);
        verify(eventStoreRepository, times(1)).save(firstEntryCaptor.capture());
        
        PropertyEventEntry savedEntry = firstEntryCaptor.getValue();
        assertEquals("PropertyCreatedDomainEvent", savedEntry.getEventType());
        assertEquals(1, savedEntry.getVersion());
        assertEquals(propertyId, savedEntry.getPropertyId());

        // Verify event was published
        ArgumentCaptor<Object> firstEventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(firstEventCaptor.capture());
        PropertyCreatedDomainEvent publishedEvent = (PropertyCreatedDomainEvent) firstEventCaptor.getValue();
        assertEquals(propertyId, publishedEvent.propertyId());
        assertEquals(ownerId, publishedEvent.ownerId());

        // 2. Invoke Projection Handler manually to test read view creation logic
        projectionHandler.on(publishedEvent);

        ArgumentCaptor<PropertyReadModel> firstReadModelCaptor = ArgumentCaptor.forClass(PropertyReadModel.class);
        verify(readModelRepository, times(1)).save(firstReadModelCaptor.capture());
        PropertyReadModel savedReadModel = firstReadModelCaptor.getValue();
        assertEquals(propertyId, savedReadModel.getPropertyId());
        assertEquals(PropertyStatus.PENDING, savedReadModel.getStatus());
        assertEquals("Beautiful Apartment in Saigon", savedReadModel.getTitle());

        // 3. Mock the event store history retrieval to return the created event
        when(eventStoreRepository.findByPropertyIdOrderByCreatedAtAsc(propertyId))
                .thenReturn(List.of(savedEntry));

        // 4. Update status to APPROVED
        commandHandler.handleUpdateStatus(new UpdatePropertyStatusCommand(propertyId, PropertyStatus.APPROVED));

        // Verify second event was saved to store
        ArgumentCaptor<PropertyEventEntry> secondEntryCaptor = ArgumentCaptor.forClass(PropertyEventEntry.class);
        verify(eventStoreRepository, times(2)).save(secondEntryCaptor.capture());
        PropertyEventEntry statusEventEntry = secondEntryCaptor.getAllValues().get(1);
        assertEquals("PropertyStatusChangedDomainEvent", statusEventEntry.getEventType());
        assertEquals(2, statusEventEntry.getVersion());

        // Verify status change event was published
        ArgumentCaptor<Object> secondEventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(secondEventCaptor.capture());
        PropertyStatusChangedDomainEvent statusChangedEvent = (PropertyStatusChangedDomainEvent) secondEventCaptor.getAllValues().get(1);
        assertEquals(propertyId, statusChangedEvent.propertyId());
        assertEquals(PropertyStatus.PENDING, statusChangedEvent.oldStatus());
        assertEquals(PropertyStatus.APPROVED, statusChangedEvent.newStatus());

        // 5. Invoke Projection Handler to update status
        when(readModelRepository.findById(propertyId)).thenReturn(Optional.of(savedReadModel));
        projectionHandler.on(statusChangedEvent);

        ArgumentCaptor<PropertyReadModel> secondReadModelCaptor = ArgumentCaptor.forClass(PropertyReadModel.class);
        verify(readModelRepository, times(2)).save(secondReadModelCaptor.capture());
        PropertyReadModel updatedReadModel = secondReadModelCaptor.getAllValues().get(1);
        assertEquals(PropertyStatus.APPROVED, updatedReadModel.getStatus());

        // 6. Test aggregate state reconstruction
        when(eventStoreRepository.findByPropertyIdOrderByCreatedAtAsc(propertyId))
                .thenReturn(List.of(savedEntry, statusEventEntry));
        PropertyAggregate aggregate = commandHandler.getProperty(propertyId);
        assertNotNull(aggregate);
        assertEquals(PropertyStatus.APPROVED, aggregate.getStatus());
        assertEquals(ownerId, aggregate.getOwnerId());
        assertEquals(2, aggregate.getVersion());

        // 7. Update status to SOLD (terminal status)
        commandHandler.handleUpdateStatus(new UpdatePropertyStatusCommand(propertyId, PropertyStatus.SOLD));
        
        ArgumentCaptor<PropertyEventEntry> thirdEntryCaptor = ArgumentCaptor.forClass(PropertyEventEntry.class);
        verify(eventStoreRepository, times(3)).save(thirdEntryCaptor.capture());
        PropertyEventEntry soldEventEntry = thirdEntryCaptor.getAllValues().get(2);
        assertEquals("PropertyStatusChangedDomainEvent", soldEventEntry.getEventType());
        assertEquals(3, soldEventEntry.getVersion());

        // 8. Reconstruct aggregate in SOLD status
        when(eventStoreRepository.findByPropertyIdOrderByCreatedAtAsc(propertyId))
                .thenReturn(List.of(savedEntry, statusEventEntry, soldEventEntry));
        PropertyAggregate aggregateSold = commandHandler.getProperty(propertyId);
        assertEquals(PropertyStatus.SOLD, aggregateSold.getStatus());

        // 9. Try updating from SOLD (terminal) to UNAVAILABLE - should fail validation
        assertThrows(BusinessException.class, () -> {
            commandHandler.handleUpdateStatus(new UpdatePropertyStatusCommand(propertyId, PropertyStatus.UNAVAILABLE));
        });
    }
}
