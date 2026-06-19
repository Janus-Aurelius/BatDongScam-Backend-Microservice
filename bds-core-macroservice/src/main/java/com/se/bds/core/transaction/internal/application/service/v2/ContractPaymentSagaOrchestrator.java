package com.se.bds.core.transaction.internal.application.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.enums.PaymentStatus;
import com.se.bds.common.enums.RoleEnum;
import com.se.bds.common.event.SagaPaymentCommand;
import com.se.bds.common.event.SagaPaymentResult;
import com.se.bds.common.exception.BusinessException;
import com.se.bds.core.shared.event.ContractStatusChangedEvent;
import com.se.bds.core.shared.ids.ContractId;
import com.se.bds.core.transaction.internal.application.port.out.ContractPaymentSagaRepository;
import com.se.bds.core.transaction.internal.application.port.out.ContractRepository;
import com.se.bds.core.transaction.internal.application.port.out.PaymentRepository;
import com.se.bds.core.transaction.internal.domain.model.Contract;
import com.se.bds.core.transaction.internal.domain.model.ContractPaymentSaga;
import com.se.bds.core.transaction.internal.domain.model.ContractStatus;
import com.se.bds.core.transaction.internal.domain.model.Payment;
import com.se.bds.core.transaction.internal.domain.model.SagaStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Orchestrator that coordinates the distributed Saga between Contract Activation and Payment Processing.
 * Resolves dual-write inconsistencies by persisting state steps and publishing commands over Kafka.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContractPaymentSagaOrchestrator {

    private final ContractPaymentSagaRepository sagaRepository;
    private final ContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final KafkaOperations<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    private static final String SAGA_COMMAND_TOPIC = "contract-saga-commands";

    /**
     * Starts a new Contract Payment Saga.
     * Generates a unique Saga ID, updates saga state to PAYMENT_PENDING,
     * and dispatches a PROCESS_PAYMENT command via Kafka.
     *
     * @param contractId the unique ID of the target contract
     * @param paymentId  the unique ID of the payment transaction
     */
    @Transactional
    public UUID startSaga(UUID contractId, UUID paymentId) {
        log.info("[SagaOrchestrator] Starting Saga for contractId={}, paymentId={}", contractId, paymentId);

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException("SAGA_ERROR", "Contract not found: " + contractId));

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException("SAGA_ERROR", "Payment not found: " + paymentId));

        if (contract.getStatus() != ContractStatus.PENDING_PAYMENT) {
            throw new BusinessException("SAGA_ERROR", "Contract must be in PENDING_PAYMENT status, but is: " + contract.getStatus());
        }

        UUID sagaId = UUID.randomUUID();

        // 1. Create and persist Saga state
        ContractPaymentSaga saga = ContractPaymentSaga.builder()
                .id(sagaId)
                .contractId(contractId)
                .paymentId(paymentId)
                .amount(payment.getAmount())
                .status(SagaStatus.STARTED)
                .build();

        sagaRepository.save(saga);

        // 2. Transition state to PAYMENT_PENDING
        saga.setStatus(SagaStatus.PAYMENT_PENDING);
        sagaRepository.save(saga);

        // 3. Emit PROCESS_PAYMENT command to Kafka
        SagaPaymentCommand command = new SagaPaymentCommand(
                sagaId,
                paymentId,
                contractId,
                payment.getAmount(),
                "PROCESS_PAYMENT"
        );

        sendSagaCommand(command);

        log.info("[SagaOrchestrator] Saga started successfully, sagaId={}, state={PAYMENT_PENDING}", sagaId);
        return sagaId;
    }

    /**
     * Processes results returned from the financial service.
     *
     * @param result the result event consumed from Kafka
     */
    @Transactional
    public void handlePaymentResult(SagaPaymentResult result) {
        log.info("[SagaOrchestrator] Received payment result for sagaId={}, status={}", result.sagaId(), result.status());

        ContractPaymentSaga saga = sagaRepository.findById(result.sagaId()).orElse(null);
        if (saga == null) {
            log.warn("[SagaOrchestrator] Ignored payment result: no Saga found with ID={}", result.sagaId());
            return;
        }

        if (saga.getStatus() != SagaStatus.PAYMENT_PENDING) {
            log.warn("[SagaOrchestrator] Ignored payment result for sagaId={}: current state is {} (expected PAYMENT_PENDING)", 
                    saga.getId(), saga.getStatus());
            return;
        }

        if ("SUCCESS".equalsIgnoreCase(result.status())) {
            processPaymentSuccess(saga, result);
        } else {
            processPaymentFailure(saga, result);
        }
    }

    /**
     * Handles the result of the compensating refund transaction.
     *
     * @param result the refund result event consumed from Kafka
     */
    @Transactional
    public void handleRefundResult(SagaPaymentResult result) {
        log.info("[SagaOrchestrator] Received refund result for sagaId={}, status={}", result.sagaId(), result.status());

        ContractPaymentSaga saga = sagaRepository.findById(result.sagaId()).orElse(null);
        if (saga == null) {
            log.warn("[SagaOrchestrator] Ignored refund result: no Saga found with ID={}", result.sagaId());
            return;
        }

        if (saga.getStatus() != SagaStatus.REFUND_PENDING) {
            log.warn("[SagaOrchestrator] Ignored refund result for sagaId={}: current state is {} (expected REFUND_PENDING)",
                    saga.getId(), saga.getStatus());
            return;
        }

        if ("REFUNDED".equalsIgnoreCase(result.status())) {
            saga.setStatus(SagaStatus.COMPENSATED);
            sagaRepository.save(saga);

            // Update core DB payment entity status to REFUNDED
            Payment payment = paymentRepository.findById(saga.getPaymentId()).orElse(null);
            if (payment != null) {
                payment.setStatus(PaymentStatus.FAILED); // Fail it locally
                paymentRepository.save(payment);
            }

            log.info("[SagaOrchestrator] Saga compensated successfully (Refunded), sagaId={}", saga.getId());
        }
    }

    private void processPaymentSuccess(ContractPaymentSaga saga, SagaPaymentResult result) {
        saga.setStatus(SagaStatus.PAYMENT_SUCCESS);
        sagaRepository.save(saga);

        // 1. Update Core database payment status
        Payment payment = paymentRepository.findById(saga.getPaymentId()).orElse(null);
        if (payment != null) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidTime(LocalDateTime.now());
            payment.setTransactionReference(result.transactionReference());
            paymentRepository.save(payment);
            log.info("[SagaOrchestrator] Payment marked as SUCCESS in local DB for sagaId={}", saga.getId());
        }

        // 2. Activate Contract (Forward Step 2)
        Contract contract = contractRepository.findById(saga.getContractId()).orElse(null);
        if (contract != null) {
            try {
                // Execute state change
                contract.transitionTo(ContractStatus.ACTIVE);
                contract.setSignedAt(LocalDateTime.now());
                contractRepository.save(contract);

                // Publish spring events (will be bridged to Kafka)
                eventPublisher.publishEvent(new ContractStatusChangedEvent(
                        new ContractId(contract.getId()),
                        contract.getContractType().name(),
                        contract.getPropertyId(),
                        contract.getCustomerId(),
                        ContractStatus.PENDING_PAYMENT.name(),
                        ContractStatus.ACTIVE.name(),
                        Instant.now()
                ));

                saga.setStatus(SagaStatus.CONTRACT_ACTIVE);
                sagaRepository.save(saga);
                log.info("[SagaOrchestrator] Saga completed successfully. Contract is ACTIVE. sagaId={}", saga.getId());

            } catch (Exception e) {
                log.error("[SagaOrchestrator] Failed to activate contract for sagaId={}. Triggering compensating rollback...", saga.getId(), e);
                triggerCompensatingRefund(saga, contract);
            }
        }
    }

    private void triggerCompensatingRefund(ContractPaymentSaga saga, Contract contract) {
        // Step 1: Rollback Contract state to CANCELLED locally
        try {
            contract.setStatus(ContractStatus.CANCELLED);
            contract.setCancellationReason("Saga compensation: payment succeeded but contract activation failed.");
            contract.setCancelledAt(LocalDateTime.now());
            contractRepository.save(contract);
            
            log.info("[SagaOrchestrator] Contract status rolled back to CANCELLED for sagaId={}", saga.getId());
        } catch (Exception ex) {
            log.error("[SagaOrchestrator] Critical: Failed to set contract status to CANCELLED for sagaId={}", saga.getId(), ex);
        }

        // Step 2: Transition Saga to REFUND_PENDING
        saga.setStatus(SagaStatus.REFUND_PENDING);
        sagaRepository.save(saga);

        // Step 3: Publish REFUND_PAYMENT command to Kafka
        SagaPaymentCommand command = new SagaPaymentCommand(
                saga.getId(),
                saga.getPaymentId(),
                saga.getContractId(),
                saga.getAmount(),
                "REFUND_PAYMENT"
        );

        sendSagaCommand(command);
        log.info("[SagaOrchestrator] Compensating Refund command dispatched to Kafka for sagaId={}", saga.getId());
    }

    private void processPaymentFailure(ContractPaymentSaga saga, SagaPaymentResult result) {
        saga.setStatus(SagaStatus.FAILED);
        sagaRepository.save(saga);

        // Update Payment status to FAILED in local DB
        Payment payment = paymentRepository.findById(saga.getPaymentId()).orElse(null);
        if (payment != null) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setNotes("Payment failed: " + result.errorMessage());
            paymentRepository.save(payment);
        }

        // Deactivate Contract locally (Cancel it since payment failed)
        Contract contract = contractRepository.findById(saga.getContractId()).orElse(null);
        if (contract != null) {
            contract.setStatus(ContractStatus.CANCELLED);
            contract.setCancellationReason("Saga failure: Payment rejected. Reason: " + result.errorMessage());
            contract.setCancelledAt(LocalDateTime.now());
            contractRepository.save(contract);
        }

        log.info("[SagaOrchestrator] Saga terminated as FAILED due to payment rejection, sagaId={}", saga.getId());
    }

    private void sendSagaCommand(SagaPaymentCommand command) {
        try {
            String payload = objectMapper.writeValueAsString(command);
            kafkaTemplate.send(SAGA_COMMAND_TOPIC, command.sagaId().toString(), payload);
            log.info("[SagaOrchestrator] Dispatched Saga command to Kafka: topic={}, payload={}", SAGA_COMMAND_TOPIC, payload);
        } catch (Exception e) {
            log.error("[SagaOrchestrator] Failed to dispatch Saga command to Kafka: {}", command, e);
            throw new RuntimeException("Kafka saga command dispatch failed", e);
        }
    }
}
