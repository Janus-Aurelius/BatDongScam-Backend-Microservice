package com.se100.bds.data.domains;

import com.se100.bds.data.util.TimeGenerator;
import com.se100.bds.models.entities.contract.Contract;
import com.se100.bds.models.entities.contract.DepositContract;
import com.se100.bds.models.entities.contract.PurchaseContract;
import com.se100.bds.models.entities.contract.RentalContract;
import com.se100.bds.models.entities.contract.Payment;
import com.se100.bds.repositories.domains.contract.ContractRepository;
import com.se100.bds.repositories.domains.contract.PaymentRepository;
import com.se100.bds.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentDummyData {

    private final PaymentRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final Random random = new Random();
    private final TimeGenerator timeGenerator = new TimeGenerator();

    public void createDummy() {
        createDummyPayments();
    }

    @SuppressWarnings("D")
    private void createDummyPayments() {
        log.info("Creating dummy payments");

        List<Contract> contracts = contractRepository.findAllWithCustomerAndUser();
        if (contracts.isEmpty()) {
            log.warn("Cannot create payments - no contracts found");
            return;
        }

        List<Payment> payments = new ArrayList<>();

        for (Contract contract : contracts) {
            LocalDateTime paymentStartTime = contract.getSignedAt() != null ? contract.getSignedAt() : contract.getCreatedAt();
            if (paymentStartTime == null || paymentStartTime.isAfter(LocalDateTime.now())) {
                paymentStartTime = LocalDateTime.now().minusDays(1);
            }
            if (contract instanceof DepositContract depositContract) {
                // Create deposit payment for deposit contracts
                LocalDateTime depositCreatedAt = timeGenerator.getRandomTimeAfter(paymentStartTime, LocalDateTime.now());
                LocalDateTime depositUpdatedAt = timeGenerator.getRandomTimeAfter(depositCreatedAt, LocalDateTime.now());

                Payment deposit = Payment.builder()
                        .contract(contract)
                        .property(contract.getProperty())
                        .payer(contract.getCustomer().getUser())
                        .paymentType(Constants.PaymentTypeEnum.DEPOSIT)
                        .amount(depositContract.getDepositAmount())
                        .dueDate(contract.getStartDate())
                        .paidTime(contract.getStartDate().atTime(13, 20))
                        .installmentNumber(null)
                        .paymentMethod("Bank Transfer")
                        .transactionReference(String.format("TXN%012d", random.nextInt(999999999)))
                        .status(Constants.PaymentStatusEnum.SUCCESS)
                        .penaltyAmount(BigDecimal.ZERO)
                        .notes("Deposit payment")
                        .build();
                deposit.setCreatedAt(depositCreatedAt);
                deposit.setUpdatedAt(depositUpdatedAt);
                payments.add(deposit);
            } else if (contract instanceof RentalContract rentalContract) {
                // Create monthly rental payments
                int months = rentalContract.getMonthCount();
                BigDecimal monthlyAmount = rentalContract.getMonthlyRentAmount();
                for (int i = 0; i < months; i++) {
                    LocalDate dueDate = contract.getStartDate().plusMonths(i);
                    LocalDateTime paidTime = random.nextBoolean() ? dueDate.plusDays(random.nextInt(5)).atTime(13, 20) : null;
                    LocalDateTime installmentStartTime = contract.getSignedAt() != null ? contract.getSignedAt() : contract.getCreatedAt();
                    if (installmentStartTime == null || installmentStartTime.isAfter(LocalDateTime.now())) {
                        installmentStartTime = LocalDateTime.now().minusDays(1);
                    }
                    LocalDateTime paymentCreatedAt = timeGenerator.getRandomTimeAfter(installmentStartTime, LocalDateTime.now());
                    LocalDateTime paymentUpdatedAt = paidTime != null ?
                            timeGenerator.getRandomTimeAfter(paymentCreatedAt, paidTime) :
                            timeGenerator.getRandomTimeAfter(paymentCreatedAt, LocalDateTime.now());
                    Payment installment = Payment.builder()
                            .contract(contract)
                            .property(contract.getProperty())
                            .payer(contract.getCustomer().getUser())
                            .paymentType(Constants.PaymentTypeEnum.MONTHLY)
                            .amount(monthlyAmount)
                            .dueDate(dueDate)
                            .paidTime(paidTime)
                            .installmentNumber(i + 1)
                            .paymentMethod(paidTime != null ? "PayOS Transfer" : null)
                            .transactionReference(paidTime != null ? String.format("TXN%012d", random.nextInt(999999999)) : null)
                            .status(paidTime != null ? Constants.PaymentStatusEnum.SUCCESS : Constants.PaymentStatusEnum.PENDING)
                            .penaltyAmount(BigDecimal.ZERO)
                            .notes(String.format("Monthly payment %d/%d", i + 1, months))
                            .build();
                    installment.setCreatedAt(paymentCreatedAt);
                    installment.setUpdatedAt(paymentUpdatedAt);
                    payments.add(installment);
                }
            } else if (contract instanceof PurchaseContract purchaseContract) {
                // Create advance payment
                LocalDateTime advanceCreatedAt = timeGenerator.getRandomTimeAfter(paymentStartTime, LocalDateTime.now());
                LocalDateTime advanceUpdatedAt = timeGenerator.getRandomTimeAfter(advanceCreatedAt, LocalDateTime.now());
                BigDecimal advanceAmount = purchaseContract.getAdvancePaymentAmount() != null
                        ? purchaseContract.getAdvancePaymentAmount()
                        : BigDecimal.ZERO;

                if (advanceAmount.compareTo(BigDecimal.ZERO) > 0) {
                    Payment advance = Payment.builder()
                            .contract(contract)
                            .property(contract.getProperty())
                            .payer(contract.getCustomer().getUser())
                            .paymentType(Constants.PaymentTypeEnum.ADVANCE)
                            .amount(advanceAmount)
                            .dueDate(contract.getStartDate().plusDays(7))
                            .paidTime(contract.getStartDate().plusDays(5).atTime(10, 30))
                            .installmentNumber(null)
                            .paymentMethod("Bank Transfer")
                            .transactionReference(String.format("TXN%012d", random.nextInt(999999999)))
                            .status(Constants.PaymentStatusEnum.SUCCESS)
                            .penaltyAmount(BigDecimal.ZERO)
                            .notes("Advance payment for property purchase")
                            .build();
                    advance.setCreatedAt(advanceCreatedAt);
                    advance.setUpdatedAt(advanceUpdatedAt);
                    payments.add(advance);
                }

                // Create remaining/full payment
                BigDecimal remainingAmount = purchaseContract.getPropertyValue().subtract(advanceAmount);

                if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
                    LocalDate fullPayDueDate = contract.getStartDate().plusDays(30);
                    boolean isPaid = random.nextBoolean();
                    LocalDateTime fullPaidTime = isPaid ? fullPayDueDate.atTime(13, 20) : null;
                    LocalDateTime fullPayCreatedAt = timeGenerator.getRandomTimeAfter(advanceUpdatedAt, LocalDateTime.now());
                    LocalDateTime fullPayUpdatedAt;

                    if (fullPaidTime != null && fullPaidTime.isAfter(fullPayCreatedAt)) {
                        fullPayUpdatedAt = timeGenerator.getRandomTimeAfter(fullPayCreatedAt, fullPaidTime);
                    } else {
                        fullPayUpdatedAt = timeGenerator.getRandomTimeAfter(fullPayCreatedAt, LocalDateTime.now());
                    }

                    Payment fullPay = Payment.builder()
                            .contract(contract)
                            .property(contract.getProperty())
                            .payer(contract.getCustomer().getUser())
                            .paymentType(Constants.PaymentTypeEnum.FULL_PAY)
                            .amount(remainingAmount)
                            .dueDate(fullPayDueDate)
                            .paidTime(fullPaidTime)
                            .installmentNumber(null)
                            .paymentMethod("Bank Transfer")
                            .transactionReference(String.format("TXN%012d", random.nextInt(999999999)))
                            .status(isPaid ? Constants.PaymentStatusEnum.SUCCESS : Constants.PaymentStatusEnum.PENDING)
                            .penaltyAmount(BigDecimal.ZERO)
                            .notes("Full payment for property purchase")
                            .build();
                    fullPay.setCreatedAt(fullPayCreatedAt);
                    fullPay.setUpdatedAt(fullPayUpdatedAt);
                    payments.add(fullPay);
                }
            }
        }

        paymentRepository.saveAll(payments);
        log.info("Saved {} payments to database", payments.size());
    }
}
