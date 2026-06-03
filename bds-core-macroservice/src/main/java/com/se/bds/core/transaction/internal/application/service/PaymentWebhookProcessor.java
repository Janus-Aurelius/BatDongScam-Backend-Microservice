package com.se.bds.core.transaction.internal.application.service;

import com.se.bds.common.enums.PaymentStatus;
import com.se.bds.common.event.PaymentCompletedEvent;
import com.se.bds.core.shared.event.ContractStatusChangedEvent;
import com.se.bds.core.shared.ids.ContractId;
import com.se.bds.core.shared.ids.PaymentId;
import com.se.bds.core.transaction.internal.application.port.in.PaymentWebhookUseCase;
import com.se.bds.core.transaction.internal.application.port.out.ContractRepository;
import com.se.bds.core.transaction.internal.application.port.out.PaymentRepository;
import com.se.bds.core.transaction.internal.domain.model.Contract;
import com.se.bds.core.transaction.internal.domain.model.ContractStatus;
import com.se.bds.core.transaction.internal.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookProcessor implements PaymentWebhookUseCase {

    private final PaymentRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public boolean processPaymentCompleted(PaymentCompletedEvent event) {
        log.info("[ACCOUNTS] Processing PaymentCompletedEvent: paymentId={}, contractId={}", 
                event.paymentId(), event.contractId());

        // 1. Fetch the payment record
        Payment payment = paymentRepository.findById(event.paymentId()).orElse(null);
        if (payment == null) {
            log.warn("[EVENT] PaymentCompletedEvent ignored: no Payment found for paymentId={}", event.paymentId());
            return false;
        }

        // 2. Update payment status
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("[EVENT] Payment already marked as SUCCESS: paymentId={}", payment.getId());
            return true;
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidTime(LocalDateTime.now());
        paymentRepository.save(payment);
        log.info("[EVENT] Payment status updated to SUCCESS: paymentId={}", payment.getId());

        // 3. Update contract status
        Contract contract = payment.getContract();
        if (contract != null) {
            // Publish internal PaymentCompletedEvent
            eventPublisher.publishEvent(new com.se.bds.core.shared.event.PaymentCompletedEvent(
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
        return true;
    }
}
