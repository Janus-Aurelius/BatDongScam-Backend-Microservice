package com.se.bds.core.transaction.internal.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.event.PaymentCompletedEvent;
import com.se.bds.core.transaction.internal.application.port.in.PaymentWebhookUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSucceededConsumer {

    private final PaymentWebhookUseCase paymentWebhookUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-succeeded", groupId = "core-macroservice-payment")
    public void consumePaymentSucceeded(@Payload String payload) throws Exception {
        log.info("[KAFKA] Received payment-succeeded event, payload={}", payload);
        // Bubble deserialization or business execution exceptions up to trigger the Kafka CommonErrorHandler
        PaymentCompletedEvent event = objectMapper.readValue(payload, PaymentCompletedEvent.class);
        boolean result = paymentWebhookUseCase.processPaymentCompleted(event);
        if (!result) {
            throw new RuntimeException("Business execution failed for payment-completed event");
        }
        log.info("[KAFKA] Payment-succeeded event processed successfully");
    }
}
