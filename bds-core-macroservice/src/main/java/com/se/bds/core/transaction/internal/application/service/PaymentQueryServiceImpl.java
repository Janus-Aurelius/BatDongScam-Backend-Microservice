package com.se.bds.core.transaction.internal.application.service;

import com.se.bds.common.enums.PaymentStatus;
import com.se.bds.common.enums.PaymentType;
import com.se.bds.common.exception.BusinessException;
import com.se.bds.core.shared.event.ContractStatusChangedEvent;
import com.se.bds.core.shared.event.PaymentCompletedEvent;
import com.se.bds.core.shared.ids.ContractId;
import com.se.bds.core.shared.ids.PaymentId;
import com.se.bds.core.transaction.internal.application.port.in.PaymentQueryUseCase;
import com.se.bds.core.transaction.internal.application.port.out.ContractRepository;
import com.se.bds.core.transaction.internal.application.port.out.PaymentRepository;
import com.se.bds.core.transaction.internal.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentQueryServiceImpl implements PaymentQueryUseCase {
    private final PaymentRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public Page<Payment> searchPayments(
            List<PaymentType> paymentTypes,
            List<PaymentStatus> statuses,
            UUID payerId,
            UUID contractId,
            UUID propertyId,
            Pageable pageable
    ) {
        return paymentRepository.searchPayments(paymentTypes, statuses, payerId, contractId, propertyId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Payment> searchPaymentsByPayer(
            UUID payerId,
            List<PaymentStatus> statuses,
            Pageable pageable
    ) {
        return paymentRepository.searchPaymentsByPayer(payerId, statuses, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentById(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND", "Payment not found: " + id));
    }

    @Override
    @Transactional
    public Payment updatePaymentStatus(UUID id, PaymentStatus newStatus) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND", "Payment not found: " + id));

        if (payment.getStatus() == newStatus) {
            return payment;
        }

        PaymentStatus oldStatus = payment.getStatus();
        payment.setStatus(newStatus);
        if (newStatus == PaymentStatus.SUCCESS) {
            payment.setPaidTime(LocalDateTime.now());
        }
        payment = paymentRepository.save(payment);
        log.info("[PAYMENTS] Manual status update: paymentId={}, from={} to={}", id, oldStatus, newStatus);

        if (newStatus == PaymentStatus.SUCCESS) {
            Contract contract = payment.getContract();
            if (contract != null) {
                // Publish PaymentCompletedEvent
                eventPublisher.publishEvent(new PaymentCompletedEvent(
                        new PaymentId(payment.getId()),
                        new ContractId(contract.getId()),
                        payment.getPropertyId() != null ? payment.getPropertyId() : contract.getPropertyId(),
                        payment.getPaymentType().name(),
                        payment.getAmount(),
                        payment.getPayerUserId(),
                        Instant.now()
                ));

                // Contract status activation trigger
                if (contract.getStatus() == ContractStatus.PENDING_PAYMENT) {
                    contract.transitionTo(ContractStatus.ACTIVE);
                    contract.setSignedAt(LocalDateTime.now());
                    contractRepository.save(contract);
                    log.info("[PAYMENTS] Contract activated manually: contractId={}", contract.getId());

                    // Publish ContractStatusChangedEvent
                    eventPublisher.publishEvent(new ContractStatusChangedEvent(
                            new ContractId(contract.getId()),
                            contract.getContractType().name(),
                            contract.getPropertyId(),
                            contract.getCustomerId(),
                            ContractStatus.PENDING_PAYMENT.name(),
                            ContractStatus.ACTIVE.name(),
                            Instant.now()
                    ));
                }
            }
        }

        return payment;
    }
}
