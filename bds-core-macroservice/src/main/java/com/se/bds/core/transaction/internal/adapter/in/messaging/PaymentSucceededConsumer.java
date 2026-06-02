package com.se.bds.core.transaction.internal.adapter.in.messaging;

import com.se.bds.core.transaction.internal.application.port.in.PaymentWebhookUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSucceededConsumer {

    private final PaymentWebhookUseCase paymentWebhookUseCase;

    @KafkaListener(topics = "payment-succeeded", groupId = "core-macroservice-payment")
    public void consumePaymentSucceeded(
            @Payload String payload,
            @Header(name = "X-Signature", required = false) String signature) {
        log.info("[KAFKA] Received payment-succeeded event, signature={}", signature);
        boolean result = paymentWebhookUseCase.processWebhook(payload, signature);
        if (result) {
            log.info("[KAFKA] Payment-succeeded event processed successfully");
        } else {
            log.warn("[KAFKA] Payment-succeeded event processing failed");
        }
    }
}
