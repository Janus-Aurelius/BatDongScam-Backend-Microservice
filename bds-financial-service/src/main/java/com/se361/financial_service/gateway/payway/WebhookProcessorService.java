package com.se361.financial_service.gateway.payway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.enums.PaymentStatus;
import com.se.bds.common.enums.PaymentType;
import com.se.bds.common.event.PaymentCompletedEvent;
import com.se361.financial_service.gateway.PaymentGatewayWebhookEvent;
import com.se361.financial_service.repositories.PaymentRepository;
import com.se361.financial_service.repositories.PayoutRepository;
import com.se361.financial_service.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookProcessorService {

    private final PaymentRepository paymentRepository;
    private final PayoutRepository payoutRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public void process(PaymentGatewayWebhookEvent event) {
        String gatewayObjectId = event.getGatewayObjectId();

        if (event.getType() == Constants.PaymentGatewayEventType.PAYMENT_SUCCEEDED) {
            paymentRepository.findByPayosPaymentId(gatewayObjectId).ifPresentOrElse(payment -> {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaidTime(LocalDateTime.now());
                paymentRepository.save(payment);
                log.info("Payment {} marked SUCCESS via webhook", payment.getId());

                // Publish PaymentCompletedEvent to Kafka
                publishPaymentCompleted(payment);
            }, () -> log.warn("Webhook: payment not found for gatewayId {}", gatewayObjectId));

        } else if (event.getType() == Constants.PaymentGatewayEventType.PAYMENT_CANCELED) {
            paymentRepository.findByPayosPaymentId(gatewayObjectId).ifPresentOrElse(payment -> {
                if (payment.getStatus() != PaymentStatus.SUCCESS) {
                    payment.setStatus(PaymentStatus.CANCELLED);
                    paymentRepository.save(payment);
                    log.info("Payment {} marked CANCELLED via webhook", payment.getId());
                }
            }, () -> log.warn("Webhook: payment not found for gatewayId {}", gatewayObjectId));

        } else if (event.getType() == Constants.PaymentGatewayEventType.PAYOUT_PAID) {
            log.info("Payout {} confirmed PAID via webhook", gatewayObjectId);
            payoutRepository.findByGatewayPayoutId(gatewayObjectId).ifPresentOrElse(payout -> {
                payout.setStatus("PAID");
                payoutRepository.save(payout);
                log.info("Payout {} updated to PAID", payout.getId());
            }, () -> log.warn("Webhook: payout not found for gatewayId {}", gatewayObjectId));

        } else if (event.getType() == Constants.PaymentGatewayEventType.PAYOUT_FAILED) {
            log.warn("Payout {} FAILED via webhook: {}", gatewayObjectId, event.getError());
            payoutRepository.findByGatewayPayoutId(gatewayObjectId).ifPresentOrElse(payout -> {
                payout.setStatus("FAILED");
                payout.setErrorMessage(event.getError());
                payoutRepository.save(payout);
                log.info("Payout {} updated to FAILED", payout.getId());
            }, () -> log.warn("Webhook: payout not found for gatewayId {}", gatewayObjectId));
        }
    }

    private void publishPaymentCompleted(com.se361.financial_service.entities.Payment payment) {
        try {
            PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
                    payment.getId(),
                    payment.getContractId(),
                    payment.getPropertyId(),
                    payment.getPaymentType().name(),
                    payment.getAmount(),
                    payment.getPayerId(),
                    Instant.now()
            );
            String payload = objectMapper.writeValueAsString(completedEvent);
            log.info("[Kafka] Publishing PaymentCompletedEvent to topic=payment-succeeded: {}", payload);
            kafkaTemplate.send("payment-succeeded", payment.getId().toString(), payload);
        } catch (Exception e) {
            log.error("[Kafka] Failed to publish PaymentCompletedEvent for payment={}", payment.getId(), e);
        }
    }
}
