package com.se100.bds.data.domains;

import com.se100.bds.data.util.TimeGenerator;
import com.se100.bds.models.entities.contract.DepositContract;
import com.se100.bds.models.entities.contract.PurchaseContract;
import com.se100.bds.models.entities.contract.RentalContract;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.models.entities.user.Customer;
import com.se100.bds.models.entities.user.SaleAgent;
import com.se100.bds.repositories.domains.contract.DepositContractRepository;
import com.se100.bds.repositories.domains.contract.PurchaseContractRepository;
import com.se100.bds.repositories.domains.contract.RentalContractRepository;
import com.se100.bds.repositories.domains.property.PropertyRepository;
import com.se100.bds.repositories.domains.user.CustomerRepository;
import com.se100.bds.repositories.domains.user.SaleAgentRepository;
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
public class ContractDummyData {

    private final DepositContractRepository depositContractRepository;
    private final RentalContractRepository rentalContractRepository;
    private final PurchaseContractRepository purchaseContractRepository;
    private final PropertyRepository propertyRepository;
    private final CustomerRepository customerRepository;
    private final SaleAgentRepository saleAgentRepository;

    private final Random random = new Random();
    private final TimeGenerator timeGenerator = new TimeGenerator();

    public void createDummy() {
        createDummyContracts();
    }

    @SuppressWarnings("D")
    private void createDummyContracts() {
        log.info("Creating dummy contracts");

        List<DepositContract> depositContracts = new ArrayList<>();
        List<RentalContract> rentalContracts = new ArrayList<>();
        List<PurchaseContract> purchaseContracts = new ArrayList<>();

        // Create 50 contracts
        for (int i = 1; i <= 50; i++) {
            LocalDateTime createdAt = timeGenerator.getRandomTime();

            // Get list of properties with valid date
            List<Property> properties = propertyRepository.findAllByCreatedAtBefore(createdAt);
            if (properties.isEmpty()) {
                properties = propertyRepository.findAll();
            }
            if (properties.isEmpty()) {
                log.warn("No properties available to create contracts, stopping at {} contracts", i - 1);
                break;
            }

            // Get list of customers with valid date
            List<Customer> customers = customerRepository.findAllByCreatedAtBefore(createdAt);
            if (customers.isEmpty()) {
                customers = customerRepository.findAll();
            }
            if (customers.isEmpty()) {
                log.warn("No customers available to create contracts, stopping at {} contracts", i - 1);
                break;
            }

            // Get list of agents with valid date
            List<SaleAgent> agents = saleAgentRepository.findAllByCreatedAtBefore(createdAt);
            if (agents.isEmpty()) {
                agents = saleAgentRepository.findAll();
            }
            if (agents.isEmpty()) {
                log.warn("No agents available to create contracts, stopping at {} contracts", i - 1);
                break;
            }

            Property property = properties.get(random.nextInt(properties.size()));
            Customer customer = customers.get(random.nextInt(customers.size()));
            SaleAgent agent = agents.get(random.nextInt(agents.size()));

            boolean isRental = property.getTransactionType() == Constants.TransactionTypeEnum.RENTAL;

            BigDecimal totalAmount = property.getPriceAmount();
            BigDecimal depositAmount = totalAmount.multiply(new BigDecimal("0.1")); // 10% deposit
            BigDecimal commissionAmount = totalAmount.multiply(property.getCommissionRate());

            LocalDateTime updatedAt = timeGenerator.getRandomTimeAfter(createdAt, LocalDateTime.now());

            Constants.ContractStatusEnum[] statuses = Constants.ContractStatusEnum.values();
            Constants.ContractStatusEnum status = statuses[random.nextInt(statuses.length)];
            var cancelledDate = timeGenerator.getRandomTimeAfter(createdAt, LocalDateTime.now());
            if (status == Constants.ContractStatusEnum.CANCELLED) {
                updatedAt = cancelledDate;
            }

            LocalDateTime signedAt = timeGenerator.getRandomTimeAfter(updatedAt, LocalDateTime.now());

            // Create deposit contract first
            LocalDate depositStartDate = createdAt.toLocalDate().plusDays(random.nextInt(15));
            LocalDate depositEndDate = depositStartDate.plusDays(30); // Deposit valid for 30 days

            DepositContract depositContract = new DepositContract();
            var mainContractType = isRental
                    ? Constants.MainContractTypeEnum.RENTAL
                    : Constants.MainContractTypeEnum.PURCHASE;
            depositContract.setMainContractType(mainContractType);
            depositContract.setProperty(property);
            depositContract.setCustomer(customer);
            depositContract.setAgent(agent);
            depositContract.setStatus(status);
            depositContract.setContractNumber(String.format("DEP%06d%04d", LocalDate.now().getYear(), i));
            depositContract.setStartDate(depositStartDate);
            depositContract.setEndDate(depositEndDate);
            depositContract.setSpecialTerms("Standard deposit terms and conditions apply");
            depositContract.setSignedAt(signedAt);
            depositContract.setDepositAmount(depositAmount);
            depositContract.setAgreedPrice(totalAmount);
            depositContract.setCreatedAt(createdAt);
            depositContract.setUpdatedAt(updatedAt);
            depositContract.setPayments(new ArrayList<>());

            if (status == Constants.ContractStatusEnum.CANCELLED) {
                depositContract.setCancellationReason("Dummy data cancellation reason");
                depositContract.setCancelledBy(Constants.RoleEnum.CUSTOMER);
                depositContract.setCancelledAt(cancelledDate);
            }

            depositContracts.add(depositContract);

            // Create main contract (rental or purchase)
            LocalDate mainStartDate = depositEndDate.plusDays(random.nextInt(7));

            if (isRental) {
                int monthCount = 12; // 1 year rental
                LocalDate mainEndDate = mainStartDate.plusMonths(monthCount);

                RentalContract rentalContract = new RentalContract();
                rentalContract.setProperty(property);
                rentalContract.setCustomer(customer);
                rentalContract.setAgent(agent);
                rentalContract.setStatus(status);
                rentalContract.setContractNumber(String.format("RNT%06d%04d", LocalDate.now().getYear(), i));
                rentalContract.setStartDate(mainStartDate);
                rentalContract.setEndDate(mainEndDate);
                rentalContract.setSpecialTerms("Standard rental terms and conditions apply");
                rentalContract.setSignedAt(signedAt);
                rentalContract.setDepositContract(depositContract);
                rentalContract.setMonthCount(monthCount);
                rentalContract.setMonthlyRentAmount(totalAmount); // For rental, price is monthly
                rentalContract.setCommissionAmount(commissionAmount);
                rentalContract.setLatePaymentPenaltyRate(new BigDecimal("0.05"));
                rentalContract.setAccumulatedUnpaidPenalty(BigDecimal.ZERO);
                rentalContract.setUnpaidMonthsCount(0);
                rentalContract.setCreatedAt(createdAt);
                rentalContract.setUpdatedAt(updatedAt);
                rentalContract.setPayments(new ArrayList<>());

                if (status == Constants.ContractStatusEnum.CANCELLED) {
                    rentalContract.setCancellationReason("Dummy data cancellation reason");
                    rentalContract.setCancelledBy(Constants.RoleEnum.CUSTOMER);
                    rentalContract.setCancelledAt(cancelledDate);
                }

                rentalContracts.add(rentalContract);
            } else {
                LocalDate mainEndDate = mainStartDate.plusDays(90); // 90 days to complete purchase

                BigDecimal advancePayment = totalAmount.multiply(new BigDecimal("0.3")); // 30% advance

                PurchaseContract purchaseContract = new PurchaseContract();
                purchaseContract.setProperty(property);
                purchaseContract.setCustomer(customer);
                purchaseContract.setAgent(agent);
                purchaseContract.setStatus(status);
                purchaseContract.setContractNumber(String.format("PUR%06d%04d", LocalDate.now().getYear(), i));
                purchaseContract.setStartDate(mainStartDate);
                purchaseContract.setEndDate(mainEndDate);
                purchaseContract.setSpecialTerms("Standard purchase terms and conditions apply");
                purchaseContract.setSignedAt(signedAt);
                purchaseContract.setDepositContract(depositContract);
                purchaseContract.setPropertyValue(totalAmount);
                purchaseContract.setAdvancePaymentAmount(advancePayment);
                purchaseContract.setCommissionAmount(commissionAmount);
                purchaseContract.setCreatedAt(createdAt);
                purchaseContract.setUpdatedAt(updatedAt);
                purchaseContract.setPayments(new ArrayList<>());

                if (status == Constants.ContractStatusEnum.CANCELLED) {
                    purchaseContract.setCancellationReason("Dummy data cancellation reason");
                    purchaseContract.setCancelledBy(Constants.RoleEnum.CUSTOMER);
                    purchaseContract.setCancelledAt(cancelledDate);
                }

                purchaseContracts.add(purchaseContract);
            }
        }

        // Save deposit contracts first (they are referenced by main contracts)
        depositContractRepository.saveAll(depositContracts);
        log.info("Saved {} deposit contracts to database", depositContracts.size());

        rentalContractRepository.saveAll(rentalContracts);
        log.info("Saved {} rental contracts to database", rentalContracts.size());

        purchaseContractRepository.saveAll(purchaseContracts);
        log.info("Saved {} purchase contracts to database", purchaseContracts.size());
    }
}
