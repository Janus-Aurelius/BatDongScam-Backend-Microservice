package com.se.bds.core.transaction.internal.adapter.in.messaging;

import com.se.bds.core.transaction.internal.application.port.in.PaymentWebhookUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

class PaymentSucceededConsumerTest {

    @Mock
    private PaymentWebhookUseCase paymentWebhookUseCase;

    private PaymentSucceededConsumer paymentSucceededConsumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        paymentSucceededConsumer = new PaymentSucceededConsumer(paymentWebhookUseCase);
    }

    @Test
    void consumePaymentSucceeded_DelegatesToUseCase() {
        String payload = "{\"eventId\": \"123\"}";
        String signature = "sig";

        when(paymentWebhookUseCase.processWebhook(payload, signature)).thenReturn(true);

        paymentSucceededConsumer.consumePaymentSucceeded(payload, signature);

        verify(paymentWebhookUseCase, times(1)).processWebhook(payload, signature);
    }
}
