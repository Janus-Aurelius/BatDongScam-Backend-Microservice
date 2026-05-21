package com.se.bds.core.transaction.internal.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.core.shared.event.ContractStatusChangedEvent;
import com.se.bds.core.shared.event.PaymentCompletedEvent;
import com.se.bds.core.shared.ids.ContractId;
import com.se.bds.core.shared.ids.PaymentId;
import com.se.bds.core.transaction.internal.adapter.in.web.PaymentWebhookSignatureVerifier;
import com.se.bds.core.transaction.internal.application.port.in.PaymentWebhookUseCase;
import com.se.bds.core.transaction.internal.application.port.out.ContractRepository;
import com.se.bds.core.transaction.internal.application.port.out.PaymentRepository;
import com.se.bds.core.transaction.internal.application.port.out.ProcessedWebhookEventRepository;
import com.se.bds.core.transaction.internal.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookProcessor implements PaymentWebhookUseCase {

    private final PaymentRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final ProcessedWebhookEventRepository processedWebhookEventRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${payway.verify-key:default-verify-key}")
    private String verifyKey;

    @Override
    @Transactional
    public boolean processWebhook(String rawBody, String signature) {
        log.info("[ACCOUNTS] Webhook received from gateway, signature={}", signature);

        // 1. Verify cryptographic signature
        if (signature != null && !signature.isBlank()) {
            // Payway signature usually sent as X-Signature: sha256=<hex>
            String hexSignature = signature.contains("sha256=") ? signature.split("sha256=")[1] : signature;
            boolean signatureValid = PaymentWebhookSignatureVerifier.verify(verifyKey, rawBody.getBytes(StandardCharsets.UTF_8), hexSignature);
            if (!signatureValid) {
                log.error("[ACCOUNTS] Webhook received with INVALID signature");
                return false;
            }
            log.info("[ACCOUNTS] Webhook signature verified successfully");
        } else {
            log.warn("[ACCOUNTS] Webhook received without signature header! Continuing for development/test purposes.");
        }

        try {
            // 2. Parse event payload
            JsonNode payload = objectMapper.readTree(rawBody);
            String eventId = payload.has("eventId") ? payload.get("eventId").asText() : "evt_" + UUID.randomUUID().toString().substring(0, 8);
            String eventType = payload.has("eventType") ? payload.get("eventType").asText() : "PAYMENT_SUCCEEDED";
            String paywayPaymentId = payload.has("paywayPaymentId") ? payload.get("paywayPaymentId").asText() : null;
            String statusStr = payload.has("status") ? payload.get("status").asText() : "SUCCESS";

            if (paywayPaymentId == null) {
                log.warn("[EVENT] Webhook ignored: paywayPaymentId is missing");
                return false;
            }

            // 3. Idempotency Check
            if (processedWebhookEventRepository.existsByExternalEventId(eventId)) {
                log.info("[EVENT] Webhook DUPLICATE ignored: externalEventId={}", eventId);
                return true; // Return true as we already successfully handled this event
            }

            // 4. Fetch the payment record
            Payment payment = paymentRepository.findByPaywayPaymentId(paywayPaymentId).orElse(null);
            if (payment == null) {
                log.warn("[EVENT] Webhook processed: no Payment found for paywayPaymentId={}", paywayPaymentId);
                
                // Record processed event to prevent repeat processing
                ProcessedWebhookEvent ignoredEvent = ProcessedWebhookEvent.builder()
                        .externalEventId(eventId)
                        .provider("PAYWAY")
                        .eventType(eventType)
                        .result("IGNORED")
                        .build();
                processedWebhookEventRepository.save(ignoredEvent);
                return true; // Return true to signal we consumed the webhook
            }

            // 5. Update payment status based on event
            if ("SUCCESS".equalsIgnoreCase(statusStr) && "PAYMENT_SUCCEEDED".equalsIgnoreCase(eventType)) {
                if (payment.getStatus() == PaymentStatus.SUCCESS) {
                    log.info("[EVENT] Payment already marked as SUCCESS: paymentId={}", payment.getId());
                    return true;
                }

                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaidTime(LocalDateTime.now());
                paymentRepository.save(payment);
                log.info("[EVENT] Payment status updated: paymentId={}, from={PENDING} to={SUCCESS}", payment.getId(), PaymentStatus.SUCCESS);

                Contract contract = payment.getContract();
                if (contract != null) {
                    // Publish PaymentCompletedEvent so the property module can update property status/fee records
                    eventPublisher.publishEvent(new PaymentCompletedEvent(
                            new PaymentId(payment.getId()),
                            new ContractId(contract.getId()),
                            payment.getPropertyId() != null ? payment.getPropertyId() : contract.getPropertyId(),
                            payment.getPaymentType().name(),
                            payment.getAmount(),
                            payment.getPayerUserId(),
                            Instant.now()
                    ));

                    // Run business side-effects
                    if (contract.getStatus() == ContractStatus.PENDING_PAYMENT) {
                        contract.transitionTo(ContractStatus.ACTIVE);
                        contract.setSignedAt(LocalDateTime.now());
                        contractRepository.save(contract);
                        log.info("[EVENT] Contract status changed: contractId={}, type={}, from={PENDING_PAYMENT} to={ACTIVE}",
                                contract.getId(), contract.getContractType(), ContractStatus.ACTIVE);

                        // Publish ContractStatusChangedEvent
                        eventPublisher.publishEvent(new ContractStatusChangedEvent(
                                new ContractId(contract.getId()),
                                contract.getContractType().name(),
                                contract.getPropertyId(),
                                ContractStatus.PENDING_PAYMENT.name(),
                                ContractStatus.ACTIVE.name(),
                                Instant.now()
                        ));
                    }
                }
            } else if ("FAILED".equalsIgnoreCase(statusStr) || "PAYMENT_CANCELED".equalsIgnoreCase(eventType)) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                log.info("[EVENT] Payment status updated: paymentId={}, to={FAILED}", payment.getId(), PaymentStatus.FAILED);
            }

            // 6. Record processed event
            ProcessedWebhookEvent processedEvent = ProcessedWebhookEvent.builder()
                    .externalEventId(eventId)
                    .provider("PAYWAY")
                    .eventType(eventType)
                    .paymentId(payment.getId())
                    .result("SUCCESS")
                    .build();
            processedWebhookEventRepository.save(processedEvent);

            log.info("[EVENT] Webhook processed: externalEventId={}, type={}, paymentId={}", eventId, eventType, payment.getId());
            return true;

        } catch (Exception e) {
            log.error("[METHOD] EXCEPTION in PaymentWebhookProcessor.processWebhook(): {}", e.getMessage(), e);
            return false;
        }
    }
}
