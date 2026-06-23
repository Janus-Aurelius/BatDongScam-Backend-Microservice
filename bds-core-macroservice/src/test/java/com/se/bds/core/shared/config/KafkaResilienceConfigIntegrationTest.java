package com.se.bds.core.shared.config;

import com.se.bds.core.BaseIntegrationTest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration test suite verifying Spring Kafka DLT (Dead Letter Queue) and non-blocking retry configuration.
 * Proves that consumer exceptions trigger the recovery mechanism and permanently failed events are successfully rerouted to the DLT topic.
 */
public class KafkaResilienceConfigIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KafkaResilienceConfig kafkaResilienceConfig;

    @MockBean
    private KafkaTemplate<Object, Object> kafkaTemplate;

    private CommonErrorHandler errorHandler;

    @BeforeEach
    void setUp() {
        errorHandler = kafkaResilienceConfig.kafkaErrorHandler(kafkaTemplate);
    }

    @Test
    void testKafkaErrorHandlerRoutingToDeadLetterTopic() throws Exception {
        // Mock a Kafka message (ConsumerRecord) from the 'payment-succeeded' topic
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("payment-succeeded", 0, 0L, "key-123", "{\"status\":\"PAID\"}");

        // Stub KafkaTemplate to successfully dispatch the DLT event
        CompletableFuture<SendResult<Object, Object>> dltFuture = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(any(String.class), any(), any(Object.class))).thenReturn(dltFuture);

        // Mock Spring Kafka containers
        MessageListenerContainer container = mock(MessageListenerContainer.class);
        org.apache.kafka.clients.consumer.Consumer<?, ?> consumer = mock(org.apache.kafka.clients.consumer.Consumer.class);

        // Proof: Verify the DeadLetterPublishingRecoverer successfully routes failed events to '{topic}-dlt'
        Exception businessException = new IllegalArgumentException("Invalid payload structure");

        // Invoke the error handler directly to simulate a failure execution requiring recovery
        errorHandler.handleOne(
                businessException,
                record,
                consumer,
                container
        );

        // Verify that the KafkaTemplate was hit to dispatch the record to the Dead Letter Topic
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> keyCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());

        // Assert DLT coordinates
        assertEquals("payment-succeeded-dlt", topicCaptor.getValue(), "DLT topic must append '-dlt' suffix to the original topic name");
        assertEquals("key-123", keyCaptor.getValue(), "DLT message key must match the original record key");
        assertEquals("{\"status\":\"PAID\"}", payloadCaptor.getValue(), "DLT payload content must match the original message payload");
    }
}
