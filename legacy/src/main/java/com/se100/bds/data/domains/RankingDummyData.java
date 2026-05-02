package com.se100.bds.data.domains;

import com.se100.bds.models.entities.contract.Contract;
import com.se100.bds.models.entities.contract.DepositContract;
import com.se100.bds.models.entities.contract.PurchaseContract;
import com.se100.bds.models.entities.contract.RentalContract;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.models.entities.user.Customer;
import com.se100.bds.models.entities.user.PropertyOwner;
import com.se100.bds.models.entities.user.SaleAgent;
import com.se100.bds.models.schemas.ranking.*;
import com.se100.bds.repositories.domains.contract.ContractRepository;
import com.se100.bds.repositories.domains.property.PropertyRepository;
import com.se100.bds.repositories.domains.user.CustomerRepository;
import com.se100.bds.repositories.domains.user.PropertyOwnerRepository;
import com.se100.bds.repositories.domains.user.SaleAgentRepository;
import com.se100.bds.repositories.domains.mongo.ranking.*;
import com.se100.bds.services.domains.report.scheduler.FinancialReportScheduler;
import com.se100.bds.services.domains.report.scheduler.UserReportScheduler;
import com.se100.bds.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class RankingDummyData {

    private final PropertyOwnerRepository propertyOwnerRepository;
    private final SaleAgentRepository saleAgentRepository;
    private final CustomerRepository customerRepository;
    private final PropertyRepository propertyRepository;
    private final ContractRepository contractRepository;

    private final IndividualPropertyOwnerContributionMonthRepository ownerContributionMonthRepository;
    private final IndividualPropertyOwnerContributionAllRepository ownerContributionAllRepository;
    private final IndividualSalesAgentPerformanceMonthRepository agentPerformanceMonthRepository;
    private final IndividualSalesAgentPerformanceCareerRepository agentPerformanceCareerRepository;
    private final IndividualCustomerPotentialMonthRepository customerPotentialMonthRepository;
    private final IndividualCustomerPotentialAllRepository customerPotentialAllRepository;

    private final FinancialReportScheduler financialReportScheduler;
    private final UserReportScheduler userReportScheduler;

    public boolean rankingDataExists() {
        // Check if any ranking data exists
        return ownerContributionMonthRepository.count() > 0
            || agentPerformanceMonthRepository.count() > 0
            || customerPotentialMonthRepository.count() > 0;
    }

    public void createDummy() {
        log.info("Creating ranking dummy data...");

        // Get current month and year
        int currentMonth = LocalDateTime.now().getMonthValue();
        int currentYear = LocalDateTime.now().getYear();

        // Create rankings from January 2024 to current month
        int startYear = 2024;
        int startMonth = 1;
        int year = startYear;
        int month = startMonth;
        while (year < currentYear || (year == currentYear && month <= currentMonth)) {
            createPropertyOwnerContributionRankings(month, year);
            createSalesAgentPerformanceRankings(month, year);
            createCustomerPotentialRankings(month, year);

            month++;
            if (month > 12) {
                month = 1;
                year++;
            }
        }

        // Create all-time rankings
        createPropertyOwnerContributionAllTimeRankings();
        createSalesAgentPerformanceCareerRankings();
        createCustomerPotentialAllTimeRankings();

        // Initialize data for reports for each month from January 2024 to current month
        year = startYear;
        month = startMonth;
        while (year < currentYear || (year == currentYear && month <= currentMonth)) {
            userReportScheduler.initData(month, year);

            month++;
            if (month > 12) {
                month = 1;
                year++;
            }
        }

        year = startYear;
        month = startMonth;
        while (year < currentYear || (year == currentYear && month <= currentMonth)) {
            financialReportScheduler.initFinancialReportData(month, year).join();
            financialReportScheduler.recalculateSalaryForInitData(month, year);

            month++;
            if (month > 12) {
                month = 1;
                year++;
            }
        }

        log.info("Done creating ranking dummy data");
    }

    private void createPropertyOwnerContributionRankings(int month, int year) {
        log.info("Creating property owner contribution rankings for {}/{}", month, year);

        List<PropertyOwner> owners = propertyOwnerRepository.findAll();
        List<IndividualPropertyOwnerContributionMonth> rankings = new ArrayList<>();

        for (PropertyOwner owner : owners) {
            // Calculate contribution metrics
            List<Property> properties = propertyRepository.findAllByOwner_Id(owner.getId()).stream().toList();

            int totalForSales = (int) properties.stream()
                    .filter(p -> p.getTransactionType() == Constants.TransactionTypeEnum.SALE)
                    .count();

            int totalForRents = (int) properties.stream()
                    .filter(p -> p.getTransactionType() == Constants.TransactionTypeEnum.RENTAL)
                    .count();

            int totalSold = (int) properties.stream()
                    .filter(p -> p.getStatus() == Constants.PropertyStatusEnum.SOLD)
                    .count();

            int totalRented = (int) properties.stream()
                    .filter(p -> p.getStatus() == Constants.PropertyStatusEnum.RENTED)
                    .count();

            // Calculate contribution value (sum of prices of sold/rented properties)
            BigDecimal contributionValue = properties.stream()
                    .filter(p -> p.getStatus() == Constants.PropertyStatusEnum.SOLD ||
                                 p.getStatus() == Constants.PropertyStatusEnum.RENTED)
                    .map(Property::getPriceAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Calculate contribution point (simple formula: sold*10 + rented*5 + listed*1)
            int contributionPoint = totalSold * 10 + totalRented * 5 + properties.size();

            IndividualPropertyOwnerContributionMonth ranking = IndividualPropertyOwnerContributionMonth.builder()
                    .ownerId(owner.getId())
                    .month(month)
                    .year(year)
                    .contributionPoint(contributionPoint)
                    .contributionTier(calculateContributionTier(contributionPoint))
                    .monthContributionValue(contributionValue)
                    .monthTotalProperties(properties.size())
                    .monthTotalForSales(totalForSales)
                    .monthTotalForRents(totalForRents)
                    .monthTotalPropertiesSold(totalSold)
                    .monthTotalPropertiesRented(totalRented)
                    .build();

            rankings.add(ranking);
        }

        // Sort by contribution point and assign ranking positions
        rankings.sort((a, b) -> b.getContributionPoint().compareTo(a.getContributionPoint()));
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRankingPosition(i + 1);
        }

        ownerContributionMonthRepository.saveAll(rankings);
        log.info("Created {} property owner contribution rankings for {}/{}", rankings.size(), month, year);
    }

    private void createPropertyOwnerContributionAllTimeRankings() {
        log.info("Creating property owner all-time contribution rankings");

        List<PropertyOwner> owners = propertyOwnerRepository.findAll();
        List<IndividualPropertyOwnerContributionAll> rankings = new ArrayList<>();

        for (PropertyOwner owner : owners) {
            List<Property> properties = propertyRepository.findAllByOwner_Id(owner.getId()).stream().toList();

            int totalForSales = (int) properties.stream()
                    .filter(p -> p.getTransactionType() == Constants.TransactionTypeEnum.SALE)
                    .count();

            int totalForRents = (int) properties.stream()
                    .filter(p -> p.getTransactionType() == Constants.TransactionTypeEnum.RENTAL)
                    .count();

            int totalSold = (int) properties.stream()
                    .filter(p -> p.getStatus() == Constants.PropertyStatusEnum.SOLD)
                    .count();

            int totalRented = (int) properties.stream()
                    .filter(p -> p.getStatus() == Constants.PropertyStatusEnum.RENTED)
                    .count();

            BigDecimal totalContributionValue = properties.stream()
                    .filter(p -> p.getStatus() == Constants.PropertyStatusEnum.SOLD ||
                                 p.getStatus() == Constants.PropertyStatusEnum.RENTED)
                    .map(Property::getPriceAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int totalContributionPoint = totalSold * 10 + totalRented * 5 + properties.size();

            IndividualPropertyOwnerContributionAll ranking = IndividualPropertyOwnerContributionAll.builder()
                    .ownerId(owner.getId())
                    .contributionPoint(totalContributionPoint)
                    .contributionValue(totalContributionValue)
                    .totalProperties(properties.size())
                    .totalPropertiesSold(totalSold)
                    .totalPropertiesRented(totalRented)
                    .build();

            rankings.add(ranking);
        }

        rankings.sort((a, b) -> b.getContributionPoint().compareTo(a.getContributionPoint()));
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRankingPosition(i + 1);
        }

        ownerContributionAllRepository.saveAll(rankings);
        log.info("Created {} property owner all-time contribution rankings", rankings.size());
    }

    private void createSalesAgentPerformanceRankings(int month, int year) {
        log.info("Creating sales agent performance rankings for {}/{}", month, year);

        List<SaleAgent> agents = saleAgentRepository.findAll();
        List<IndividualSalesAgentPerformanceMonth> rankings = new ArrayList<>();

        for (SaleAgent agent : agents) {
            // Get agent's properties and contracts
            List<Property> properties = propertyRepository.findAllByAssignedAgent_Id(agent.getId()).stream().toList();
            List<Contract> contracts = contractRepository.findAllByAgent_Id(agent.getId());

            int handlingProperties = (int) properties.stream()
                    .filter(p -> p.getStatus() != Constants.PropertyStatusEnum.SOLD &&
                                 p.getStatus() != Constants.PropertyStatusEnum.RENTED)
                    .count();

            // Simulate monthly metrics (in real scenario, would filter by month/year)
            int monthPropertiesAssigned = Math.min(properties.size(), new Random().nextInt(5) + 1);
            int monthAppointmentsAssigned = new Random().nextInt(10) + 5;
            int monthAppointmentsCompleted = monthAppointmentsAssigned - new Random().nextInt(3);
            int monthContracts = Math.min(contracts.size(), new Random().nextInt(3) + 1);
            int monthRates = monthContracts > 0 ? new Random().nextInt(monthContracts) + 1 : 0;

            BigDecimal avgRating = monthRates > 0 ?
                    BigDecimal.valueOf(3.5 + new Random().nextDouble() * 1.5).setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            BigDecimal customerSatisfaction = monthContracts > 0 ?
                    BigDecimal.valueOf(70 + new Random().nextDouble() * 30).setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            int performancePoint = monthContracts * 20 + monthAppointmentsCompleted * 5 + monthPropertiesAssigned * 2;

            IndividualSalesAgentPerformanceMonth ranking = IndividualSalesAgentPerformanceMonth.builder()
                    .agentId(agent.getId())
                    .month(month)
                    .year(year)
                    .performancePoint(performancePoint)
                    .performanceTier(calculatePerformanceTier(performancePoint))
                    .handlingProperties(handlingProperties)
                    .monthPropertiesAssigned(monthPropertiesAssigned)
                    .monthAppointmentsAssigned(monthAppointmentsAssigned)
                    .monthAppointmentsCompleted(monthAppointmentsCompleted)
                    .monthContracts(monthContracts)
                    .monthRates(monthRates)
                    .avgRating(avgRating)
                    .monthCustomerSatisfactionAvg(customerSatisfaction)
                    .build();

            rankings.add(ranking);
        }

        rankings.sort((a, b) -> b.getPerformancePoint().compareTo(a.getPerformancePoint()));
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRankingPosition(i + 1);
        }

        agentPerformanceMonthRepository.saveAll(rankings);
        log.info("Created {} sales agent performance rankings for {}/{}", rankings.size(), month, year);
    }

    private void createSalesAgentPerformanceCareerRankings() {
        log.info("Creating sales agent career performance rankings");

        List<SaleAgent> agents = saleAgentRepository.findAll();
        List<IndividualSalesAgentPerformanceCareer> rankings = new ArrayList<>();

        for (SaleAgent agent : agents) {
            List<Property> properties = propertyRepository.findAllByAssignedAgent_Id(agent.getId()).stream().toList();
            List<Contract> contracts = contractRepository.findAllByAgent_Id(agent.getId());

            int currentHandlingProperties = (int) properties.stream()
                    .filter(p -> p.getStatus() != Constants.PropertyStatusEnum.SOLD &&
                                 p.getStatus() != Constants.PropertyStatusEnum.RENTED)
                    .count();

            int totalPropertiesAssigned = properties.size();
            int totalAppointments = new Random().nextInt(50) + 20;
            int totalAppointmentsCompleted = totalAppointments - new Random().nextInt(10);
            int totalContracts = contracts.size();
            int totalRates = contracts.size() > 0 ? new Random().nextInt(contracts.size()) + 1 : 0;

            BigDecimal avgRating = totalRates > 0 ?
                    BigDecimal.valueOf(3.5 + new Random().nextDouble() * 1.5).setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            BigDecimal customerSatisfaction = totalContracts > 0 ?
                    BigDecimal.valueOf(70 + new Random().nextDouble() * 30).setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            int totalPerformancePoint = totalContracts * 20 + totalAppointmentsCompleted * 5 + totalPropertiesAssigned * 2;

            IndividualSalesAgentPerformanceCareer ranking = IndividualSalesAgentPerformanceCareer.builder()
                    .agentId(agent.getId())
                    .performancePoint(totalPerformancePoint)
                    .propertiesAssigned(totalPropertiesAssigned)
                    .appointmentAssigned(totalAppointments)
                    .appointmentCompleted(totalAppointmentsCompleted)
                    .totalContracts(totalContracts)
                    .totalRates(totalRates)
                    .avgRating(avgRating)
                    .customerSatisfactionAvg(customerSatisfaction)
                    .build();

            rankings.add(ranking);
        }

        rankings.sort((a, b) -> b.getPerformancePoint().compareTo(a.getPerformancePoint()));
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setCareerRanking(i + 1);
        }

        agentPerformanceCareerRepository.saveAll(rankings);
        log.info("Created {} sales agent career performance rankings", rankings.size());
    }

    private void createCustomerPotentialRankings(int month, int year) {
        log.info("Creating customer potential rankings for {}/{}", month, year);

        List<Customer> customers = customerRepository.findAll();
        if (customers.isEmpty()) {
            log.warn("No customers found, skipping potential rankings for {}/{}", month, year);
            return;
        }
        List<IndividualCustomerPotentialMonth> rankings = new ArrayList<>();

        for (Customer customer : customers) {
            List<Contract> contracts = contractRepository.findAllByCustomer_Id(customer.getId());

            // Simulate monthly metrics
            int monthViewingsRequested = new Random().nextInt(10) + 1;
            int monthViewingsAttended = monthViewingsRequested - new Random().nextInt(3);

            BigDecimal monthSpending = contracts.stream()
                    .map(this::getContractValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int monthPurchases = (int) contracts.stream()
                    .filter(c -> c.getContractType() == Constants.ContractTypeEnum.PURCHASE)
                    .count();

            int monthRentals = (int) contracts.stream()
                    .filter(c -> c.getContractType() == Constants.ContractTypeEnum.RENTAL)
                    .count();

            int monthContractsSigned = contracts.size();

            int leadScore = monthPurchases * 50 + monthRentals * 30 + monthViewingsAttended * 5 + monthViewingsRequested * 2;

            IndividualCustomerPotentialMonth ranking = IndividualCustomerPotentialMonth.builder()
                    .customerId(customer.getId())
                    .month(month)
                    .year(year)
                    .leadScore(leadScore)
                    .customerTier(calculateCustomerTier(leadScore))
                    .monthViewingsRequested(monthViewingsRequested)
                    .monthViewingAttended(monthViewingsAttended)
                    .monthSpending(monthSpending)
                    .monthPurchases(monthPurchases)
                    .monthRentals(monthRentals)
                    .monthContractsSigned(monthContractsSigned)
                    .build();

            rankings.add(ranking);
        }

        rankings.sort((a, b) -> b.getLeadScore().compareTo(a.getLeadScore()));
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setLeadPosition(i + 1);
        }

        customerPotentialMonthRepository.saveAll(rankings);
        log.info("Created {} customer potential rankings for {}/{}", rankings.size(), month, year);
    }

    private void createCustomerPotentialAllTimeRankings() {
        log.info("Creating customer all-time potential rankings");

        List<Customer> customers = customerRepository.findAll();
        List<IndividualCustomerPotentialAll> rankings = new ArrayList<>();

        for (Customer customer : customers) {
            List<Contract> contracts = contractRepository.findAllByCustomer_Id(customer.getId());

            int totalViewingsRequested = new Random().nextInt(50) + 10;
            int totalViewingsAttended = totalViewingsRequested - new Random().nextInt(10);

            BigDecimal totalSpending = contracts.stream()
                    .map(this::getContractValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int totalPurchases = (int) contracts.stream()
                    .filter(c -> c.getContractType() == Constants.ContractTypeEnum.PURCHASE)
                    .count();

            int totalRentals = (int) contracts.stream()
                    .filter(c -> c.getContractType() == Constants.ContractTypeEnum.RENTAL)
                    .count();

            int totalContractsSigned = contracts.size();

            int totalLeadScore = totalPurchases * 50 + totalRentals * 30 + totalViewingsAttended * 5 + totalViewingsRequested * 2;

            IndividualCustomerPotentialAll ranking = IndividualCustomerPotentialAll.builder()
                    .customerId(customer.getId())
                    .leadScore(totalLeadScore)
                    .viewingsRequested(totalViewingsRequested)
                    .viewingsAttended(totalViewingsAttended)
                    .spending(totalSpending)
                    .totalPurchases(totalPurchases)
                    .totalRentals(totalRentals)
                    .totalContractsSigned(totalContractsSigned)
                    .build();

            rankings.add(ranking);
        }

        rankings.sort((a, b) -> b.getLeadScore().compareTo(a.getLeadScore()));
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setLeadPosition(i + 1);
        }

        customerPotentialAllRepository.saveAll(rankings);
        log.info("Created {} customer all-time potential rankings", rankings.size());
    }

    private Constants.ContributionTierEnum calculateContributionTier(int points) {
        if (points >= 90) return Constants.ContributionTierEnum.PLATINUM;
        if (points >= 75) return Constants.ContributionTierEnum.GOLD;
        if (points >= 60) return Constants.ContributionTierEnum.SILVER;
        return Constants.ContributionTierEnum.BRONZE;
    }

    private Constants.PerformanceTierEnum calculatePerformanceTier(int points) {
        if (points >= 90) return Constants.PerformanceTierEnum.PLATINUM;
        if (points >= 75) return Constants.PerformanceTierEnum.GOLD;
        if (points >= 60) return Constants.PerformanceTierEnum.SILVER;
        return Constants.PerformanceTierEnum.BRONZE;
    }

    private Constants.CustomerTierEnum calculateCustomerTier(int score) {
        if (score >= 90) return Constants.CustomerTierEnum.PLATINUM;
        if (score >= 75) return Constants.CustomerTierEnum.GOLD;
        if (score >= 60) return Constants.CustomerTierEnum.SILVER;
        return Constants.CustomerTierEnum.BRONZE;
    }

    /**
     * Calculate the total value of a contract based on its type.
     * For DepositContract: returns the deposit amount
     * For RentalContract: returns monthlyRent * monthCount
     * For PurchaseContract: returns the property value
     */
    private BigDecimal getContractValue(Contract contract) {
        if (contract instanceof DepositContract depositContract) {
            return depositContract.getDepositAmount();
        } else if (contract instanceof RentalContract rentalContract) {
            return rentalContract.getMonthlyRentAmount()
                    .multiply(BigDecimal.valueOf(rentalContract.getMonthCount()));
        } else if (contract instanceof PurchaseContract purchaseContract) {
            return purchaseContract.getPropertyValue();
        }
        return BigDecimal.ZERO;
    }
}

