package com.se.bds.core.transaction.internal.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.se.bds.common.event.PaymentCompletedEvent;
import com.se.bds.core.transaction.internal.application.port.in.PaymentWebhookUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

class PaymentSucceededConsumerTest {

    @Mock
    private PaymentWebhookUseCase paymentWebhookUseCase;

    private ObjectMapper objectMapper;

    private PaymentSucceededConsumer paymentSucceededConsumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        paymentSucceededConsumer = new PaymentSucceededConsumer(paymentWebhookUseCase, objectMapper);
    }

    @Test
    void consumePaymentSucceeded_DelegatesToUseCase() {
        UUID paymentId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID payerUserId = UUID.randomUUID();
        Instant now = Instant.now();

        String payload = String.format(
                "{\"paymentId\":\"%s\",\"contractId\":\"%s\",\"propertyId\":\"%s\",\"paymentType\":\"PAYPAL\",\"amount\":10.0,\"payerUserId\":\"%s\",\"timestamp\":\"%s\"}",
                paymentId, contractId, propertyId, payerUserId, now
        );

        when(paymentWebhookUseCase.processPaymentCompleted(any(PaymentCompletedEvent.class))).thenReturn(true);

        paymentSucceededConsumer.consumePaymentSucceeded(payload);

        verify(paymentWebhookUseCase, times(1)).processPaymentCompleted(any(PaymentCompletedEvent.class));
    }
}
