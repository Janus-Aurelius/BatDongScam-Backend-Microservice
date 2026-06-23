package com.se361.financial_service.services;

import com.se361.financial_service.entities.OutboxEvent;
import com.se361.financial_service.repositories.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration test suite verifying the Transactional Outbox pattern in the financial service.
 * Mocks the Kafka boundary to verify that event publishing is safe, transactional, and handles failures cleanly.
 */
@SpringBootTest
@ActiveProfiles("test")
public class OutboxPatternIntegrationTest {

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
    }

    @Test
    @Transactional
    void testOutboxRelaySuccessfullyPublishesEventsAndMarksProcessed() throws Exception {
        // Create a pending outbox event
        OutboxEvent event = OutboxEvent.builder()
                .topic("payment-succeeded")
                .aggregateId("tx-12345")
                .payload("{\"status\":\"SUCCESS\"}")
                .processed(false)
                .build();
        outboxEventRepository.save(event);

        // Mock KafkaTemplate to successfully acknowledge the write
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq("payment-succeeded"), eq("tx-12345"), eq("{\"status\":\"SUCCESS\"}")))
                .thenReturn(future);

        // Proof 1: Verify scheduler processes pending events and updates database state
        outboxPublisher.relayPendingEvents();

        // Retrieve event from DB and assert processed status
        OutboxEvent processedEvent = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertTrue(processedEvent.isProcessed(), "Outbox event must be marked as processed upon successful Kafka delivery");
        assertNotNull(processedEvent.getProcessedAt(), "Processed timestamp must be set");
        assertEquals(0, processedEvent.getRetryCount(), "Retry count must remain 0");
    }

    @Test
    @Transactional
    void testOutboxRelayHandlesKafkaFailuresAndIncrementsRetryCount() throws Exception {
        // Create another pending event
        OutboxEvent event = OutboxEvent.builder()
                .topic("payment-succeeded")
                .aggregateId("tx-67890")
                .payload("{\"status\":\"FAILED\"}")
                .processed(false)
                .build();
        outboxEventRepository.save(event);

        // Mock KafkaTemplate to throw exception (broker offline)
        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka connection timed out"));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(failedFuture);

        // Proof 2: Verify failures increment retry count without crashing the relay thread
        outboxPublisher.relayPendingEvents();

        // Assert that the event is NOT marked processed, retryCount is incremented, and error details are logged
        OutboxEvent updatedEvent = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertFalse(updatedEvent.isProcessed(), "Outbox event must not be marked processed if Kafka delivery fails");
        assertEquals(1, updatedEvent.getRetryCount(), "Retry count must be incremented on transient Kafka error");
        assertNotNull(updatedEvent.getLastError(), "Error details must be captured in the database record");
        assertTrue(updatedEvent.getLastError().contains("Kafka connection timed out"));
    }

    @Test
    @Transactional
    void testOutboxRelaySkipsPoisonPillEventsAfterFiveRetries() throws Exception {
        // Create an event that has failed 5 times
        OutboxEvent event = OutboxEvent.builder()
                .topic("payment-succeeded")
                .aggregateId("tx-poison")
                .payload("{\"status\":\"CORRUPT\"}")
                .processed(false)
                .retryCount(5)
                .build();
        outboxEventRepository.save(event);

        // Mock KafkaTemplate (should not even be called)
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // Proof 3: Verify poison-pill events are bypassed after 5 failed retries to protect throughput
        outboxPublisher.relayPendingEvents();

        // Verify KafkaTemplate was never called for this event
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());

        // Assert event is still pending and not processed
        OutboxEvent finalEvent = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertFalse(finalEvent.isProcessed());
        assertEquals(5, finalEvent.getRetryCount());
    }
}
