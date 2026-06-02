package com.se361.financial_service.gateway.payway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se361.financial_service.gateway.PaymentGatewayWebhookEvent;
import com.se361.financial_service.gateway.payway.dto.PaywayWebhookEvent;
import com.se361.financial_service.gateway.payway.dto.PaywayWebhookPaymentObject;
import com.se361.financial_service.gateway.payway.dto.PaywayWebhookPayoutObject;
import com.se361.financial_service.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaywayWebhookHandler {

    private final ObjectMapper objectMapper;
    private final WebhookProcessorService webhookProcessorService;

    public void handlePaymentEvent(String rawBody) {
        try {
            PaywayWebhookEvent<PaywayWebhookPaymentObject> event = objectMapper.readValue(
                    rawBody.getBytes(StandardCharsets.UTF_8),
                    objectMapper.getTypeFactory().constructParametricType(
                            PaywayWebhookEvent.class, PaywayWebhookPaymentObject.class)
            );

            if (event == null || event.getData() == null || event.getData().getObject() == null) {
                log.warn("Payway payment webhook: missing data.object");
                return;
            }

            Constants.PaymentGatewayEventType type = mapPaymentEventType(event.getType());
            if (type == null) {
                log.info("Payway webhook: ignoring unsupported event type {}", event.getType());
                return;
            }

            PaymentGatewayWebhookEvent mapped = PaymentGatewayWebhookEvent.builder()
                    .provider("PAYWAY")
                    .type(type)
                    .externalEventId(event.getId())
                    .gatewayObjectId(event.getData().getObject().getId())
                    .error(event.getData().getError())
                    .created(event.getCreated())
                    .rawBody(rawBody)
                    .build();

            webhookProcessorService.process(mapped);

        } catch (Exception e) {
            log.error("Payway webhook: failed to process payment event", e);
        }
    }

    public void handlePayoutEvent(String rawBody) {
        try {
            PaywayWebhookEvent<PaywayWebhookPayoutObject> event = objectMapper.readValue(
                    rawBody.getBytes(StandardCharsets.UTF_8),
                    objectMapper.getTypeFactory().constructParametricType(
                            PaywayWebhookEvent.class, PaywayWebhookPayoutObject.class)
            );

            if (event == null || event.getData() == null || event.getData().getObject() == null) {
                log.warn("Payway payout webhook: missing data.object");
                return;
            }

            Constants.PaymentGatewayEventType type = mapPayoutEventType(event.getType());
            if (type == null) {
                log.info("Payway webhook: ignoring unsupported payout event type {}", event.getType());
                return;
            }

            PaymentGatewayWebhookEvent mapped = PaymentGatewayWebhookEvent.builder()
                    .provider("PAYWAY")
                    .type(type)
                    .externalEventId(event.getId())
                    .gatewayObjectId(event.getData().getObject().getId())
                    .error(event.getData().getError())
                    .created(event.getCreated())
                    .rawBody(rawBody)
                    .build();

            webhookProcessorService.process(mapped);

        } catch (Exception e) {
            log.error("Payway webhook: failed to process payout event", e);
        }
    }

    private static Constants.PaymentGatewayEventType mapPaymentEventType(String type) {
        if (type == null) return null;
        return switch (type) {
            case "payment.succeeded" -> Constants.PaymentGatewayEventType.PAYMENT_SUCCEEDED;
            case "payment.canceled", "payment.failed" -> Constants.PaymentGatewayEventType.PAYMENT_CANCELED;
            default -> null;
        };
    }

    private static Constants.PaymentGatewayEventType mapPayoutEventType(String type) {
        if (type == null) return null;
        return switch (type) {
            case "payout.paid" -> Constants.PaymentGatewayEventType.PAYOUT_PAID;
            case "payout.failed" -> Constants.PaymentGatewayEventType.PAYOUT_FAILED;
            default -> null;
        };
    }
}
