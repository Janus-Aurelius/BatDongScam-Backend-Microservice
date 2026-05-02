package com.se100.bds.services.payment.payway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se100.bds.services.domains.payment.webhook.PaymentGatewayEventType;
import com.se100.bds.services.domains.payment.webhook.PaymentGatewayProvider;
import com.se100.bds.services.domains.payment.webhook.PaymentGatewayWebhookEvent;
import com.se100.bds.services.domains.payment.webhook.PaymentGatewayWebhookProcessor;
import com.se100.bds.services.payment.payway.dto.PaywayWebhookEvent;
import com.se100.bds.services.payment.payway.dto.PaywayWebhookPaymentObject;
import com.se100.bds.services.payment.payway.dto.PaywayWebhookPayoutObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaywayWebhookHandler {

    private final ObjectMapper objectMapper;
    private final PaymentGatewayWebhookProcessor paymentGatewayWebhookProcessor;

    public void handlePaymentEvent(String rawBody) {
        try {
            PaywayWebhookEvent<PaywayWebhookPaymentObject> event = objectMapper.readValue(
                    rawBody.getBytes(StandardCharsets.UTF_8),
                    objectMapper.getTypeFactory().constructParametricType(PaywayWebhookEvent.class, PaywayWebhookPaymentObject.class)
            );

            if (event == null || event.getData() == null || event.getData().getObject() == null) {
                log.warn("Payway webhook: missing data.object");
                return;
            }

            PaymentGatewayWebhookEvent mapped = PaymentGatewayWebhookEvent.builder()
                    .provider(PaymentGatewayProvider.PAYWAY)
                    .type(mapPaywayPaymentEventType(event.getType()))
                    .externalEventId(event.getId())
                    .gatewayObjectId(event.getData().getObject().getId())
                    .error(event.getData().getError())
                    .created(event.getCreated())
                    .rawBody(rawBody)
                    .build();

            if (mapped.getType() == null) {
                log.info("Payway webhook: ignoring unsupported event type {}", event.getType());
                return;
            }

            paymentGatewayWebhookProcessor.process(mapped);

        } catch (Exception e) {
            // Best-effort: don't throw to Payway, just log.
            log.error("Payway webhook: failed to process payment event", e);
        }
    }

    /**
     * Payout webhook intake (domain payout processing not implemented yet).
     */
    public void handlePayoutEvent(String rawBody) {
        try {
            PaywayWebhookEvent<PaywayWebhookPayoutObject> event = objectMapper.readValue(
                    rawBody.getBytes(StandardCharsets.UTF_8),
                    objectMapper.getTypeFactory().constructParametricType(PaywayWebhookEvent.class, PaywayWebhookPayoutObject.class)
            );

            if (event == null || event.getData() == null || event.getData().getObject() == null) {
                log.warn("Payway webhook (payout): missing data.object");
                return;
            }

            PaymentGatewayWebhookEvent mapped = PaymentGatewayWebhookEvent.builder()
                    .provider(PaymentGatewayProvider.PAYWAY)
                    .type(mapPaywayPayoutEventType(event.getType()))
                    .externalEventId(event.getId())
                    .gatewayObjectId(event.getData().getObject().getId())
                    .error(event.getData().getError())
                    .created(event.getCreated())
                    .rawBody(rawBody)
                    .build();

            if (mapped.getType() == null) {
                log.info("Payway webhook: ignoring unsupported payout event type {}", event.getType());
                return;
            }

            paymentGatewayWebhookProcessor.process(mapped);

        } catch (Exception e) {
            log.error("Payway webhook: failed to process payout event", e);
        }
    }

    private static PaymentGatewayEventType mapPaywayPaymentEventType(String eventType) {
        if (eventType == null) return null;
        return switch (eventType) {
            case "payment.succeeded" -> PaymentGatewayEventType.PAYMENT_SUCCEEDED;
            case "payment.canceled", "payment.failed" -> PaymentGatewayEventType.PAYMENT_CANCELED;
            default -> null;
        };
    }

    private static PaymentGatewayEventType mapPaywayPayoutEventType(String eventType) {
        if (eventType == null) return null;
        return switch (eventType) {
            case "payout.paid" -> PaymentGatewayEventType.PAYOUT_PAID;
            case "payout.failed" -> PaymentGatewayEventType.PAYOUT_FAILED;
            default -> null;
        };
    }
}
