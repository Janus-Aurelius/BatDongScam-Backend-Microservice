package com.se100.bds.services.domains.contract.scheduler;

import com.se100.bds.models.entities.contract.Payment;
import com.se100.bds.models.entities.contract.RentalContract;
import com.se100.bds.repositories.domains.contract.PaymentRepository;
import com.se100.bds.repositories.domains.contract.RentalContractRepository;
import com.se100.bds.services.domains.notification.NotificationService;
import com.se100.bds.services.payment.PaymentGatewayService;
import com.se100.bds.services.payment.dto.CreatePaymentSessionRequest;
import com.se100.bds.services.payment.dto.CreatePaymentSessionResponse;
import com.se100.bds.utils.Constants.ContractStatusEnum;
import com.se100.bds.utils.Constants.NotificationTypeEnum;
import com.se100.bds.utils.Constants.PaymentStatusEnum;
import com.se100.bds.utils.Constants.PaymentTypeEnum;
import com.se100.bds.utils.Constants.RelatedEntityTypeEnum;
import com.se100.bds.utils.Constants.SecurityDepositStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final PaymentGatewayService paymentGatewayService;
    private final NotificationService notificationService;

    private static final int PAYMENT_DUE_DAYS = 7;
    private static final String CURRENCY_VND = "VND";
    private static final String PAYOS_METHOD = "PAYOS";
    private static final int UNPAID_MONTHS_THRESHOLD = 3;

    /**
     * Runs on the 1st of each month at 00:05 AM.
     * Creates monthly rent payments for all ACTIVE rental contracts.
     * Also checks for unpaid previous month payments and updates penalty tracking.
     */
    @Scheduled(cron = "0 5 0 1 * *")
    @Transactional
    public void processMonthlyRentPayments() {
        log.info("Starting monthly rent payment processing...");

        List<RentalContract> activeContracts = rentalContractRepository.findAll()
                .stream()
                .filter(c -> c.getStatus() == ContractStatusEnum.ACTIVE)
                .toList();

        log.info("Found {} active rental contracts to process", activeContracts.size());

        for (RentalContract contract : activeContracts) {
            try {
                processContractMonthlyPayment(contract);
            } catch (Exception e) {
                log.error("Failed to process monthly payment for contract {}: {}", contract.getId(), e.getMessage(), e);
            }
        }

        log.info("Completed monthly rent payment processing");
    }

    /**
     * Runs daily at 00:10 AM.
     * Checks for rental contracts that have ended and marks them as COMPLETED.
     */
    @Scheduled(cron = "0 10 0 * * *")
    @Transactional
    public void checkContractCompletion() {
        log.info("Starting contract completion check...");

        LocalDate today = LocalDate.now();

        List<RentalContract> activeContracts = rentalContractRepository.findAll()
                .stream()
                .filter(c -> c.getStatus() == ContractStatusEnum.ACTIVE)
                .filter(c -> c.getEndDate() != null && !c.getEndDate().isAfter(today))
                .toList();

        log.info("Found {} contracts that have reached end date", activeContracts.size());

        for (RentalContract contract : activeContracts) {
            try {
                completeContract(contract);
            } catch (Exception e) {
                log.error("Failed to complete contract {}: {}", contract.getId(), e.getMessage(), e);
            }
        }

        log.info("Completed contract completion check");
    }

    /**
     * Runs daily at 00:15 AM.
     * Checks for overdue payments and updates penalty tracking.
     */
    @Scheduled(cron = "0 15 0 * * *")
    @Transactional
    public void checkLatePayments() {
        log.info("Starting late payment check...");

        LocalDate today = LocalDate.now();

        List<RentalContract> activeContracts = rentalContractRepository.findAll()
                .stream()
                .filter(c -> c.getStatus() == ContractStatusEnum.ACTIVE)
                .toList();

        for (RentalContract contract : activeContracts) {
            try {
                checkContractLatePayments(contract, today);
            } catch (Exception e) {
                log.error("Failed to check late payments for contract {}: {}", contract.getId(), e.getMessage(), e);
            }
        }

        log.info("Completed late payment check");
    }

    // ==================
    // HELPER METHODS
    // ==================

    private void processContractMonthlyPayment(RentalContract contract) {
        // Calculate which installment number this should be
        LocalDate startDate = contract.getStartDate();
        long monthsSinceStart = ChronoUnit.MONTHS.between(startDate.withDayOfMonth(1), LocalDate.now().withDayOfMonth(1));
        int installmentNumber = (int) monthsSinceStart + 1; // +1 because first month is installment 1

        // Skip if we're past the contract duration
        if (installmentNumber > contract.getMonthCount()) {
            log.debug("Contract {} has completed all {} months, skipping", contract.getId(), contract.getMonthCount());
            return;
        }

        // Skip installment 1 as it's created during paperwork completion
        if (installmentNumber == 1) {
            log.debug("Skipping installment 1 for contract {} (created during paperwork)", contract.getId());
            return;
        }

        // Check if payment for this installment already exists
        boolean paymentExists = contract.getPayments().stream()
                .anyMatch(p -> p.getPaymentType() == PaymentTypeEnum.MONTHLY &&
                              p.getInstallmentNumber() != null &&
                              p.getInstallmentNumber() == installmentNumber);

        if (paymentExists) {
            log.debug("Payment for installment {} already exists for contract {}", installmentNumber, contract.getId());
            return;
        }

        // Check previous month's payment status and update penalties
        checkAndUpdatePenalties(contract, installmentNumber - 1);

        // Create the monthly payment
        Payment payment = Payment.builder()
                .contract(contract)
                .property(contract.getProperty())
                .payer(contract.getCustomer().getUser())
                .paymentType(PaymentTypeEnum.MONTHLY)
                .amount(contract.getMonthlyRentAmount())
                .dueDate(LocalDate.now().plusDays(PAYMENT_DUE_DAYS))
                .status(PaymentStatusEnum.PENDING)
                .paymentMethod(PAYOS_METHOD)
                .installmentNumber(installmentNumber)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Create payment session with gateway
        CreatePaymentSessionRequest gatewayRequest = CreatePaymentSessionRequest.builder()
                .amount(contract.getMonthlyRentAmount())
                .currency(CURRENCY_VND)
                .description(String.format("Month %d rent for: %s", installmentNumber, contract.getProperty().getTitle()))
                .metadata(Map.of(
                        "paymentType", PaymentTypeEnum.MONTHLY.getValue(),
                        "contractId", contract.getId().toString(),
                        "paymentId", savedPayment.getId().toString(),
                        "installmentNumber", String.valueOf(installmentNumber)
                ))
                .build();

        CreatePaymentSessionResponse gatewayResponse = paymentGatewayService.createPaymentSession(
                gatewayRequest,
                savedPayment.getId().toString()
        );

        savedPayment.setPaywayPaymentId(gatewayResponse.getId());
        paymentRepository.save(savedPayment);

        // Notify customer
        notificationService.createNotification(
                contract.getCustomer().getUser(),
                NotificationTypeEnum.PAYMENT_DUE,
                "Monthly Rent Payment Due",
                String.format("Month %d rent payment of %s VND is due for property: %s. Due date: %s",
                        installmentNumber,
                        contract.getMonthlyRentAmount().toPlainString(),
                        contract.getProperty().getTitle(),
                        savedPayment.getDueDate()),
                RelatedEntityTypeEnum.PAYMENT,
                savedPayment.getId().toString(),
                null
        );

        log.info("Created monthly payment {} (installment {}) for contract {}",
                savedPayment.getId(), installmentNumber, contract.getId());
    }

    private void checkAndUpdatePenalties(RentalContract contract, int previousInstallment) {
        if (previousInstallment < 1) return;

        // Find the previous month's payment
        Payment previousPayment = contract.getPayments().stream()
                .filter(p -> p.getPaymentType() == PaymentTypeEnum.MONTHLY &&
                            p.getInstallmentNumber() != null &&
                            p.getInstallmentNumber() == previousInstallment)
                .findFirst()
                .orElse(null);

        if (previousPayment == null) {
            log.warn("Previous installment {} payment not found for contract {}", previousInstallment, contract.getId());
            return;
        }

        boolean isPaid = previousPayment.getStatus() == PaymentStatusEnum.SUCCESS ||
                        previousPayment.getStatus() == PaymentStatusEnum.SYSTEM_SUCCESS;

        if (!isPaid) {
            // Calculate penalty
            BigDecimal penaltyRate = contract.getLatePaymentPenaltyRate();
            BigDecimal penaltyAmount = contract.getMonthlyRentAmount().multiply(penaltyRate);

            // Update accumulated penalty
            BigDecimal currentPenalty = contract.getAccumulatedUnpaidPenalty() != null
                    ? contract.getAccumulatedUnpaidPenalty()
                    : BigDecimal.ZERO;
            contract.setAccumulatedUnpaidPenalty(currentPenalty.add(penaltyAmount));

            // Increment unpaid months count
            int unpaidMonths = contract.getUnpaidMonthsCount() != null ? contract.getUnpaidMonthsCount() : 0;
            contract.setUnpaidMonthsCount(unpaidMonths + 1);

            rentalContractRepository.save(contract);

            log.info("Updated penalty for contract {}: added {} VND, unpaid months now {}",
                    contract.getId(), penaltyAmount, contract.getUnpaidMonthsCount());

            // Notify owner if unpaid months hits threshold
            if (contract.getUnpaidMonthsCount() >= UNPAID_MONTHS_THRESHOLD) {
                notificationService.createNotification(
                        contract.getProperty().getOwner().getUser(),
                        NotificationTypeEnum.PAYMENT_OVERDUE,
                        "Rental Payment Alert - " + UNPAID_MONTHS_THRESHOLD + " Months Unpaid",
                        String.format("Tenant for property '%s' has %d consecutive months of unpaid rent. " +
                                        "Accumulated penalty: %s VND. Please contact admin.",
                                contract.getProperty().getTitle(),
                                contract.getUnpaidMonthsCount(),
                                contract.getAccumulatedUnpaidPenalty().toPlainString()),
                        RelatedEntityTypeEnum.CONTRACT,
                        contract.getId().toString(),
                        null
                );

                // Also notify admin
                // Note: In a real app, you'd fetch admin users and notify them
                log.warn("Contract {} has {} unpaid months - admin notification needed",
                        contract.getId(), contract.getUnpaidMonthsCount());
            }
        } else {
            // Payment was made, reset unpaid months counter
            if (contract.getUnpaidMonthsCount() != null && contract.getUnpaidMonthsCount() > 0) {
                contract.setUnpaidMonthsCount(0);
                rentalContractRepository.save(contract);
                log.info("Reset unpaid months counter for contract {}", contract.getId());
            }
        }
    }

    private void checkContractLatePayments(RentalContract contract, LocalDate today) {
        // Find overdue pending payments
        List<Payment> overduePayments = contract.getPayments().stream()
                .filter(p -> p.getPaymentType() == PaymentTypeEnum.MONTHLY)
                .filter(p -> p.getStatus() == PaymentStatusEnum.PENDING)
                .filter(p -> p.getDueDate() != null && p.getDueDate().isBefore(today))
                .toList();

        for (Payment payment : overduePayments) {
            long daysOverdue = ChronoUnit.DAYS.between(payment.getDueDate(), today);

            // Send reminder notification every 7 days
            if (daysOverdue % 7 == 0) {
                notificationService.createNotification(
                        contract.getCustomer().getUser(),
                        NotificationTypeEnum.PAYMENT_OVERDUE,
                        "Overdue Rent Payment Reminder",
                        String.format("Your rent payment of %s VND for property '%s' is %d days overdue. " +
                                        "Please pay immediately to avoid additional penalties.",
                                payment.getAmount().toPlainString(),
                                contract.getProperty().getTitle(),
                                daysOverdue),
                        RelatedEntityTypeEnum.PAYMENT,
                        payment.getId().toString(),
                        null
                );

                log.info("Sent overdue reminder for payment {} ({} days overdue)", payment.getId(), daysOverdue);
            }
        }
    }

    private void completeContract(RentalContract contract) {
        contract.setStatus(ContractStatusEnum.COMPLETED);

        // Notify about security deposit if still held
        if (contract.getSecurityDepositStatus() == SecurityDepositStatusEnum.HELD) {
            String message = String.format(
                    "Rental contract for property '%s' has ended. Please contact admin regarding the security deposit of %s VND.",
                    contract.getProperty().getTitle(),
                    contract.getSecurityDepositAmount().toPlainString()
            );

            notificationService.createNotification(
                    contract.getCustomer().getUser(),
                    NotificationTypeEnum.CONTRACT_UPDATE,
                    "Rental Contract Completed - Security Deposit Pending",
                    message,
                    RelatedEntityTypeEnum.CONTRACT,
                    contract.getId().toString(),
                    null
            );

            notificationService.createNotification(
                    contract.getProperty().getOwner().getUser(),
                    NotificationTypeEnum.CONTRACT_UPDATE,
                    "Rental Contract Completed - Security Deposit Pending",
                    message,
                    RelatedEntityTypeEnum.CONTRACT,
                    contract.getId().toString(),
                    null
            );
        }

        rentalContractRepository.save(contract);
        log.info("Auto-completed rental contract {} (end date reached)", contract.getId());
    }
}

