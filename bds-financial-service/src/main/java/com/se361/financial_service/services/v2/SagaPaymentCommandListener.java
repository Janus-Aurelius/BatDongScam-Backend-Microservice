package com.se361.financial_service.services.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.enums.PaymentStatus;
import com.se.bds.common.event.SagaPaymentCommand;
import com.se.bds.common.event.SagaPaymentResult;
import com.se361.financial_service.entities.Payment;
import com.se361.financial_service.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Listens to Kafka commands sent by the Saga Orchestrator.
 * Executes forward transactions (payment processing) and compensating transactions (refund processing).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SagaPaymentCommandListener {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String SAGA_EVENT_TOPIC = "contract-saga-events";

    /**
     * Consumes commands from the orchestrator.
     * Maps them to either forward or compensating logic and dispatches results.
     */
    @KafkaListener(topics = "contract-saga-commands", groupId = "financial-service-saga")
    @Transactional
    public void consumeCommand(@Payload String payload) {
        log.info("[SagaListener] Received command: {}", payload);
        try {
            SagaPaymentCommand command = objectMapper.readValue(payload, SagaPaymentCommand.class);
            
            if ("PROCESS_PAYMENT".equalsIgnoreCase(command.commandType())) {
                processPayment(command);
            } else if ("REFUND_PAYMENT".equalsIgnoreCase(command.commandType())) {
                processRefund(command);
            } else {
                log.warn("[SagaListener] Unknown command type: {}", command.commandType());
            }
        } catch (Exception e) {
            log.error("[SagaListener] Failed to process saga command payload", e);
        }
    }

    private void processPayment(SagaPaymentCommand command) throws Exception {
        log.info("[SagaListener] Executing PROCESS_PAYMENT for sagaId={}, paymentId={}", command.sagaId(), command.paymentId());
        
        Payment payment = paymentRepository.findById(command.paymentId()).orElse(null);
        if (payment == null) {
            sendResult(new SagaPaymentResult(
                    command.sagaId(),
                    command.paymentId(),
                    "FAILED",
                    null,
                    "Payment entity not found in Financial DB"
            ));
            return;
        }

        // Check if payment is already successful (idempotent step)
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            sendResult(new SagaPaymentResult(
                    command.sagaId(),
                    command.paymentId(),
                    "SUCCESS",
                    payment.getTransactionReference(),
                    null
            ));
            return;
        }

        // Simulate core business rules/validations
        if (command.amount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            
            sendResult(new SagaPaymentResult(
                    command.sagaId(),
                    command.paymentId(),
                    "FAILED",
                    null,
                    "Payment amount must be greater than zero"
            ));
            return;
        }

        // Process payment successfully (simulate Stripe charge/gateway complete)
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidTime(LocalDateTime.now());
        payment.setTransactionReference("stripe-intent-" + UUID.randomUUID().toString().substring(0, 8));
        paymentRepository.save(payment);

        sendResult(new SagaPaymentResult(
                command.sagaId(),
                command.paymentId(),
                "SUCCESS",
                payment.getTransactionReference(),
                null
        ));
        log.info("[SagaListener] PROCESS_PAYMENT succeeded, result sent for sagaId={}", command.sagaId());
    }

    private void processRefund(SagaPaymentCommand command) throws Exception {
        log.info("[SagaListener] Executing REFUND_PAYMENT (Compensation) for sagaId={}, paymentId={}", command.sagaId(), command.paymentId());

        Payment payment = paymentRepository.findById(command.paymentId()).orElse(null);
        if (payment != null) {
            // Mark local financial payment status as FAILED or REFUNDED
            payment.setStatus(PaymentStatus.FAILED);
            payment.setNotes("Payment refunded as part of saga compensation");
            paymentRepository.save(payment);
        }

        // Emit refund confirmation
        sendResult(new SagaPaymentResult(
                command.sagaId(),
                command.paymentId(),
                "REFUNDED",
                payment != null ? payment.getTransactionReference() : null,
                null
        ));
        log.info("[SagaListener] REFUND_PAYMENT succeeded, result sent for sagaId={}", command.sagaId());
    }

    private void sendResult(SagaPaymentResult result) throws Exception {
        String payload = objectMapper.writeValueAsString(result);
        kafkaTemplate.send(SAGA_EVENT_TOPIC, result.sagaId().toString(), payload);
        log.info("[SagaListener] Dispatched result to topic={}: {}", SAGA_EVENT_TOPIC, payload);
    }
}
