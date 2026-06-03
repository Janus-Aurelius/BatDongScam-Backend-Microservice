package com.se.bds.core.property.internal.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.se.bds.common.event.ViolationPenaltyAppliedEvent;
import com.se.bds.core.property.internal.application.port.in.PropertyUseCase;
import com.se.bds.core.property.internal.application.command.UpdatePropertyStatusCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ViolationPenaltyAppliedConsumerTest {

    @Mock
    private PropertyUseCase propertyUseCase;

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;

    private ViolationPenaltyAppliedConsumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new ViolationPenaltyAppliedConsumer(propertyUseCase, restTemplate, objectMapper);
    }

    @Test
    void consumeViolationPenaltyApplied_PropertyRemovedPost_CallsUseCase() throws Exception {
        UUID violationId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID reporterId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant now = Instant.now();

        ViolationPenaltyAppliedEvent event = new ViolationPenaltyAppliedEvent(
                violationId,
                ownerId,
                reporterId,
                propertyId,
                "PROPERTY",
                "SPAM",
                "REMOVED_POST",
                now
        );

        String payload = objectMapper.writeValueAsString(event);

        consumer.consumeViolationPenaltyApplied(payload);

        ArgumentCaptor<UpdatePropertyStatusCommand> commandCaptor = ArgumentCaptor.forClass(UpdatePropertyStatusCommand.class);
        verify(propertyUseCase, times(1)).updatePropertyStatus(eq(propertyId), commandCaptor.capture());
        assertEquals("REMOVED", commandCaptor.getValue().targetStatus());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void consumeViolationPenaltyApplied_SuspendedAccount_CallsRestTemplate() throws Exception {
        UUID violationId = UUID.randomUUID();
        UUID reporterId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant now = Instant.now();

        ViolationPenaltyAppliedEvent event = new ViolationPenaltyAppliedEvent(
                violationId,
                ownerId,
                reporterId,
                ownerId,
                "CUSTOMER",
                "SPAM",
                "SUSPENDED_ACCOUNT",
                now
        );

        String payload = objectMapper.writeValueAsString(event);

        consumer.consumeViolationPenaltyApplied(payload);

        verify(restTemplate, times(1)).put(contains("/users/" + ownerId + "/status?status=SUSPENDED"), eq(null));
        verifyNoInteractions(propertyUseCase);
    }

    @Test
    void consumeViolationPenaltyApplied_OtherPenalty_DoesNothing() throws Exception {
        UUID violationId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID reporterId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant now = Instant.now();

        ViolationPenaltyAppliedEvent event = new ViolationPenaltyAppliedEvent(
                violationId,
                ownerId,
                reporterId,
                propertyId,
                "PROPERTY",
                "SPAM",
                "WARNING",
                now
        );

        String payload = objectMapper.writeValueAsString(event);

        consumer.consumeViolationPenaltyApplied(payload);

        verifyNoInteractions(propertyUseCase);
        verifyNoInteractions(restTemplate);
    }
}
