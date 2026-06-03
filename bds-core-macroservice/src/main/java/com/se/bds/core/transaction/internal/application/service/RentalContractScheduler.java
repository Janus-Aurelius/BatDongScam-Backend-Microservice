package com.se.bds.core.transaction.internal.application.service;

import com.se.bds.common.enums.PaymentStatus;
import com.se.bds.common.enums.PaymentType;
import com.se.bds.core.shared.event.ContractStatusChangedEvent;
import com.se.bds.core.shared.ids.ContractId;
import com.se.bds.core.transaction.internal.application.port.out.PaymentGatewayPort;
import com.se.bds.core.transaction.internal.application.port.out.PaymentRepository;
import com.se.bds.core.transaction.internal.application.port.out.RentalContractRepository;
import com.se.bds.core.transaction.internal.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Scheduled tasks for rental contract automation:
 * 1. Monthly payment generation (runs on 1st of each month at 00:05)
 * 2. Contract completion check (runs daily at 00:10)
 * 3. Late payment penalty check (runs daily at 00:15)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RentalContractScheduler {

    private final RentalContractRepository rentalContractRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayPort paymentGatewayPort;
    private final ApplicationEventPublisher eventPublisher;

    private static final int PAYMENT_DUE_DAYS = 7;
    private static final String CURRENCY_VND = "VND";

    /**
     * Runs on the 1st of each month at 00:05 AM.
     * Creates monthly rent payments for all ACTIVE rental contracts.
     * Also checks for unpaid previous month payments and updates penalty tracking.
     */
    @Scheduled(cron = "0 5 0 1 * *")
    @Transactional
    public void processMonthlyRentPayments() {
        log.info("[RentalContractScheduler] Starting monthly rent payment processing...");

        List<RentalContract> activeContracts = rentalContractRepository.findAllActive();

        log.info("[RentalContractScheduler] Found {} active rental contracts to process", activeContracts.size());

        for (RentalContract contract : activeContracts) {
            try {
                processContractMonthlyPayment(contract);
            } catch (Exception e) {
                log.error("[RentalContractScheduler] Failed to process monthly payment for contract {}: {}", contract.getId(), e.getMessage(), e);
            }
        }

        log.info("[RentalContractScheduler] Completed monthly rent payment processing");
    }

    /**
     * Runs daily at 00:10 AM.
     * Checks for rental contracts that have ended and marks them as COMPLETED.
     */
    @Scheduled(cron = "0 10 0 * * *")
    @Transactional
    public void checkContractCompletion() {
        log.info("[RentalContractScheduler] Starting contract completion check...");

        LocalDate today = LocalDate.now();

        List<RentalContract> activeContracts = rentalContractRepository.findAllActive();
        List<RentalContract> expiredContracts = activeContracts.stream()
                .filter(c -> c.getEndDate() != null && !c.getEndDate().isAfter(today))
                .toList();

        log.info("[RentalContractScheduler] Found {} contracts that have reached end date", expiredContracts.size());

        for (RentalContract contract : expiredContracts) {
            try {
                completeContract(contract);
            } catch (Exception e) {
                log.error("[RentalContractScheduler] Failed to complete contract {}: {}", contract.getId(), e.getMessage(), e);
            }
        }

        log.info("[RentalContractScheduler] Completed contract completion check");
    }

    /**
     * Runs daily at 00:15 AM.
     * Checks for overdue payments and updates penalty tracking.
     */
    @Scheduled(cron = "0 15 0 * * *")
    @Transactional
    public void checkLatePayments() {
        log.info("[RentalContractScheduler] Starting late payment check...");

        LocalDate today = LocalDate.now();
        List<RentalContract> activeContracts = rentalContractRepository.findAllActive();

        for (RentalContract contract : activeContracts) {
            try {
                checkContractLatePayments(contract, today);
            } catch (Exception e) {
                log.error("[RentalContractScheduler] Failed to check late payments for contract {}: {}", contract.getId(), e.getMessage(), e);
            }
        }

        log.info("[RentalContractScheduler] Completed late payment check");
    }

    private void processContractMonthlyPayment(RentalContract contract) {
        LocalDate startDate = contract.getStartDate();
        long monthsSinceStart = ChronoUnit.MONTHS.between(startDate.withDayOfMonth(1), LocalDate.now().withDayOfMonth(1));
        int installmentNumber = (int) monthsSinceStart + 1; // +1 because first month is installment 1

        if (installmentNumber > contract.getMonthCount()) {
            log.debug("[RentalContractScheduler] Contract {} has completed all {} months, skipping", contract.getId(), contract.getMonthCount());
            return;
        }

        if (installmentNumber == 1) {
            log.debug("[RentalContractScheduler] Skipping installment 1 for contract {} (created during paperwork)", contract.getId());
            return;
        }

        boolean paymentExists = contract.getPayments().stream()
                .anyMatch(p -> p.getPaymentType() == PaymentType.MONTHLY &&
                              p.getInstallmentNumber() != null &&
                              p.getInstallmentNumber() == installmentNumber);

        if (paymentExists) {
            log.debug("[RentalContractScheduler] Payment for installment {} already exists for contract {}", installmentNumber, contract.getId());
            return;
        }

        checkAndUpdatePenalties(contract, installmentNumber - 1);

        Payment payment = Payment.builder()
                .contract(contract)
                .propertyId(contract.getPropertyId())
                .payerUserId(contract.getCustomerId())
                .paymentType(PaymentType.MONTHLY)
                .amount(contract.getMonthlyRentAmount())
                .dueDate(LocalDate.now().plusDays(PAYMENT_DUE_DAYS))
                .status(PaymentStatus.PENDING)
                .paymentMethod("PAYOS")
                .installmentNumber(installmentNumber)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        PaymentGatewayPort.PaymentSessionResult gatewayResponse = paymentGatewayPort.createPaymentSession(
                contract.getMonthlyRentAmount(),
                CURRENCY_VND,
                String.format("Month %d rent for property ID: %s", installmentNumber, contract.getPropertyId()),
                Map.of(
                        "paymentType", PaymentType.MONTHLY.name(),
                        "contractId", contract.getId().toString(),
                        "paymentId", savedPayment.getId().toString(),
                        "installmentNumber", String.valueOf(installmentNumber)
                ),
                savedPayment.getId().toString()
        );

        savedPayment.setPaywayPaymentId(gatewayResponse.gatewayPaymentId());
        paymentRepository.save(savedPayment);

        log.info("[RentalContractScheduler] Created monthly payment {} (installment {}) for contract {}",
                savedPayment.getId(), installmentNumber, contract.getId());
    }

    private void checkAndUpdatePenalties(RentalContract contract, int previousInstallment) {
        if (previousInstallment < 1) return;

        Payment previousPayment = contract.getPayments().stream()
                .filter(p -> p.getPaymentType() == PaymentType.MONTHLY &&
                            p.getInstallmentNumber() != null &&
                            p.getInstallmentNumber() == previousInstallment)
                .findFirst()
                .orElse(null);

        if (previousPayment == null) {
            log.warn("[RentalContractScheduler] Previous installment {} payment not found for contract {}", previousInstallment, contract.getId());
            return;
        }

        boolean isPaid = previousPayment.getStatus() == PaymentStatus.SUCCESS ||
                        previousPayment.getStatus() == PaymentStatus.SYSTEM_SUCCESS;

        if (!isPaid) {
            BigDecimal penaltyRate = contract.getLatePaymentPenaltyRate();
            BigDecimal penaltyAmount = contract.getMonthlyRentAmount().multiply(penaltyRate);

            BigDecimal currentPenalty = contract.getAccumulatedUnpaidPenalty() != null
                    ? contract.getAccumulatedUnpaidPenalty()
                    : BigDecimal.ZERO;
            contract.setAccumulatedUnpaidPenalty(currentPenalty.add(penaltyAmount));

            int unpaidMonths = contract.getUnpaidMonthsCount() != null ? contract.getUnpaidMonthsCount() : 0;
            contract.setUnpaidMonthsCount(unpaidMonths + 1);

            rentalContractRepository.save(contract);

            log.info("[RentalContractScheduler] Updated penalty for contract {}: added {} VND, unpaid months now {}",
                    contract.getId(), penaltyAmount, contract.getUnpaidMonthsCount());
        } else {
            if (contract.getUnpaidMonthsCount() != null && contract.getUnpaidMonthsCount() > 0) {
                contract.setUnpaidMonthsCount(0);
                rentalContractRepository.save(contract);
                log.info("[RentalContractScheduler] Reset unpaid months counter for contract {}", contract.getId());
            }
        }
    }

    private void checkContractLatePayments(RentalContract contract, LocalDate today) {
        List<Payment> overduePayments = contract.getPayments().stream()
                .filter(p -> p.getPaymentType() == PaymentType.MONTHLY)
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .filter(p -> p.getDueDate() != null && p.getDueDate().isBefore(today))
                .toList();

        for (Payment payment : overduePayments) {
            long daysOverdue = ChronoUnit.DAYS.between(payment.getDueDate(), today);
            if (daysOverdue % 7 == 0) {
                log.info("[RentalContractScheduler] Overdue rent reminder logged for payment {} ({} days overdue) customer: {}",
                        payment.getId(), daysOverdue, contract.getCustomerId());
            }
        }
    }

    private void completeContract(RentalContract contract) {
        ContractStatus oldStatus = contract.complete();
        rentalContractRepository.save(contract);

        eventPublisher.publishEvent(new ContractStatusChangedEvent(
                new ContractId(contract.getId()),
                "RENTAL",
                contract.getPropertyId(),
                oldStatus.name(),
                ContractStatus.COMPLETED.name(),
                Instant.now()
        ));

        log.info("[RentalContractScheduler] Auto-completed rental contract {} (end date reached)", contract.getId());
    }
}
