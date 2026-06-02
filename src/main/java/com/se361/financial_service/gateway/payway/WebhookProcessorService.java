package com.se361.financial_service.gateway.payway;

import com.se361.financial_service.gateway.PaymentGatewayWebhookEvent;
import com.se361.financial_service.repositories.PaymentRepository;
import com.se361.financial_service.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookProcessorService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public void process(PaymentGatewayWebhookEvent event) {
        String gatewayObjectId = event.getGatewayObjectId();

        if (event.getType() == Constants.PaymentGatewayEventType.PAYMENT_SUCCEEDED) {
            paymentRepository.findByPayosPaymentId(gatewayObjectId).ifPresentOrElse(payment -> {
                payment.setStatus(Constants.PaymentStatus.SUCCESS);
                payment.setPaidTime(LocalDateTime.now());
                paymentRepository.save(payment);
                log.info("Payment {} marked SUCCESS via webhook", payment.getId());
            }, () -> log.warn("Webhook: payment not found for gatewayId {}", gatewayObjectId));

        } else if (event.getType() == Constants.PaymentGatewayEventType.PAYMENT_CANCELED) {
            paymentRepository.findByPayosPaymentId(gatewayObjectId).ifPresentOrElse(payment -> {
                if (payment.getStatus() != Constants.PaymentStatus.SUCCESS) {
                    payment.setStatus(Constants.PaymentStatus.CANCELLED);
                    paymentRepository.save(payment);
                    log.info("Payment {} marked CANCELLED via webhook", payment.getId());
                }
            }, () -> log.warn("Webhook: payment not found for gatewayId {}", gatewayObjectId));

        } else if (event.getType() == Constants.PaymentGatewayEventType.PAYOUT_PAID) {
            log.info("Payout {} confirmed PAID via webhook", gatewayObjectId);
            // TODO: update payout tracking entity when added

        } else if (event.getType() == Constants.PaymentGatewayEventType.PAYOUT_FAILED) {
            log.warn("Payout {} FAILED via webhook: {}", gatewayObjectId, event.getError());
            // TODO: notify admin
        }
    }
}
