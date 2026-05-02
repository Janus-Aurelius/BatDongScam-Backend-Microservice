package com.se100.bds.services.domains.payment.webhook.impl;

import com.se100.bds.models.entities.contract.Payment;
import com.se100.bds.repositories.domains.contract.PaymentRepository;
import com.se100.bds.services.domains.payment.webhook.PaymentGatewayEventType;
import com.se100.bds.services.domains.payment.webhook.PaymentGatewayWebhookEvent;
import com.se100.bds.services.domains.payment.webhook.PaymentGatewayWebhookProcessor;
import com.se100.bds.services.domains.payment.webhook.PaymentSucceededSideEffectHandler;
import com.se100.bds.utils.Constants.PaymentStatusEnum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayWebhookProcessorImpl implements PaymentGatewayWebhookProcessor {

    private final PaymentRepository paymentRepository;
    private final List<PaymentSucceededSideEffectHandler> succeededHandlers;

    @Override
    @Transactional
    public void process(PaymentGatewayWebhookEvent event) {
        if (event == null) {
            return;
        }
        if (event.getType() == null) {
            log.warn("Gateway webhook: missing type (provider={}, externalEventId={})", event.getProvider(), event.getExternalEventId());
            return;
        }

        // Payouts are scaffolded for now (no domain payout model in repo).
        if (event.getType() == PaymentGatewayEventType.PAYOUT_PAID || event.getType() == PaymentGatewayEventType.PAYOUT_FAILED) {
            log.info("Gateway webhook: received payout event {} for gatewayObjectId={} (provider={})", event.getType(), event.getGatewayObjectId(), event.getProvider());
            // TODO: Add a payout aggregate / repository and implement payout side-effects.
            return;
        }

        if (event.getGatewayObjectId() == null || event.getGatewayObjectId().isBlank()) {
            log.warn("Gateway webhook: missing gatewayObjectId (type={}, externalEventId={})", event.getType(), event.getExternalEventId());
            return;
        }

        Payment payment = paymentRepository.findByPaywayPaymentId(event.getGatewayObjectId()).orElse(null);
        if (payment == null) {
            log.warn("Gateway webhook: no Payment found for paywayPaymentId={} (type={}, externalEventId={})",
                    event.getGatewayObjectId(), event.getType(), event.getExternalEventId());
            return;
        }

        switch (event.getType()) {
            case PAYMENT_SUCCEEDED -> handlePaymentSucceeded(payment, event);
            case PAYMENT_CANCELED -> handlePaymentCanceled(payment, event);
            default -> log.info("Gateway webhook: ignoring event type {}", event.getType());
        }
    }

    private void handlePaymentSucceeded(Payment payment, PaymentGatewayWebhookEvent event) {
        // Idempotency: if already final success, ignore.
        if (payment.getStatus() == PaymentStatusEnum.SUCCESS || payment.getStatus() == PaymentStatusEnum.SYSTEM_SUCCESS) {
            return;
        }

        payment.setStatus(PaymentStatusEnum.SUCCESS);
        payment.setPaidTime(LocalDateTime.now());
        paymentRepository.save(payment);

        // Dispatch side effects for the payment type.
        for (PaymentSucceededSideEffectHandler handler : succeededHandlers) {
            try {
                if (handler.supports(payment)) {
                    handler.handle(payment, event);
                }
            } catch (Exception e) {
                // Synchronous best-effort: keep webhook 200 OK behavior. Log and continue.
                log.error("Payment succeeded side-effect handler failed for paymentId={} handler={}", payment.getId(), handler.getClass().getSimpleName(), e);
            }
        }

        log.info("Gateway webhook: marked paymentId={} as SUCCESS (paymentType={})", payment.getId(), payment.getPaymentType());
    }

    private void handlePaymentCanceled(Payment payment, PaymentGatewayWebhookEvent event) {
        // Idempotency: if already final success, ignore.
        if (payment.getStatus() == PaymentStatusEnum.SUCCESS || payment.getStatus() == PaymentStatusEnum.SYSTEM_SUCCESS) {
            return;
        }

        // Keep existing status mapping.
        payment.setStatus(PaymentStatusEnum.FAILED);
        paymentRepository.save(payment);

        // TODO: Decide what business side effects should happen on failed/canceled payments per PaymentType.
        log.info("Gateway webhook: marked paymentId={} as FAILED via {} (paymentType={})",
                payment.getId(), event.getType(), payment.getPaymentType());
    }
}
