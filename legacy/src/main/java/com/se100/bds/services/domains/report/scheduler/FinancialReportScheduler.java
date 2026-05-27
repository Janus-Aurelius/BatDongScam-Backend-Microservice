package com.se100.bds.services.domains.report.scheduler;

import com.se100.bds.mappers.SimpleMapper;
import com.se100.bds.models.entities.contract.Payment;
import com.se100.bds.models.schemas.ranking.IndividualSalesAgentPerformanceMonth;
import com.se100.bds.models.schemas.report.AgentSalaryItem;
import com.se100.bds.models.schemas.report.BaseReportData;
import com.se100.bds.models.schemas.report.FinancialReport;
import com.se100.bds.models.schemas.report.RankedRevenueItem;
import com.se100.bds.repositories.domains.contract.ContractRepository;
import com.se100.bds.repositories.domains.contract.PaymentRepository;
import com.se100.bds.repositories.domains.mongo.ranking.IndividualSalesAgentPerformanceMonthRepository;
import com.se100.bds.repositories.domains.mongo.report.FinancialReportRepository;
import com.se100.bds.services.domains.location.LocationService;
import com.se100.bds.services.domains.property.PropertyService;
import com.se100.bds.services.domains.user.UserService;
import com.se100.bds.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FinancialReportScheduler {

    private final IndividualSalesAgentPerformanceMonthRepository individualSalesAgentPerformanceMonthRepository;
    private final FinancialReportRepository financialReportRepository;
    private final ContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final UserService userService;
    private final PropertyService propertyService;
    private final LocationService locationService;
    private final SimpleMapper simpleMapper;

    @Scheduled(cron = "0 0 0 1 * ?")
    protected void initNewMonthData() {
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        initFinancialReportData(currentMonth, currentYear);
    }

    @Scheduled(cron = "0 0 0 L * ?")
    protected void recalculateSalaryAtEndOfMonth() {
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();

        FinancialReport currentMonthReport = financialReportRepository.findByBaseReportData_MonthAndBaseReportData_Year(currentMonth, currentYear);
        if (currentMonthReport == null) {
            log.warn("No financial report found for month {} year {}", currentMonth, currentYear);
            return;
        }

        FinancialReport previousMonthReport;
        if (currentMonth - 1 == 0) {
            previousMonthReport = financialReportRepository.findByBaseReportData_MonthAndBaseReportData_Year(12, currentYear - 1);
        } else {
            previousMonthReport = financialReportRepository.findByBaseReportData_MonthAndBaseReportData_Year(currentMonth - 1, currentYear);
        }

        FinancialReport updatedReport = recalculateAllSalaryByTheEndOfMonth(previousMonthReport, currentMonthReport);
        financialReportRepository.save(updatedReport);
    }

    @Async
    public CompletableFuture<Void> initFinancialReportData(int month, int year) {
        List<UUID> agentIds = userService.getAllCurrentAgentIds();
        List<UUID> propertyTypeIds = propertyService.getAllAvailablePropertyTypeIds();
        List<UUID> cityIds = locationService.getAllCityIds();
        List<UUID> wardIds = locationService.getAllWardIds();
        List<UUID> districtIds = locationService.getAllDistrictIds();

        // Check if report for this month already exists
        FinancialReport existingReport = financialReportRepository.findByBaseReportData_MonthAndBaseReportData_Year(
                month, year
        );

        FinancialReport currentMonth;

        if (existingReport != null) {
            // Report exists - UPDATE with latest data
            log.info("FinancialReport for month {} year {} exists. Recalculating with latest data.", month, year);
            currentMonth = existingReport;
        } else {
            // Report doesn't exist - CREATE from previous month
            log.info("FinancialReport for month {} year {} not found. Creating new report.", month, year);

            FinancialReport previousMonth;
            if (month - 1 == 0) {
                previousMonth = financialReportRepository.findByBaseReportData_MonthAndBaseReportData_Year(
                        12, year - 1
                );
            } else {
                previousMonth = financialReportRepository.findByBaseReportData_MonthAndBaseReportData_Year(
                        month - 1, year
                );
            }

            if (previousMonth != null) {
                currentMonth = simpleMapper.mapTo(previousMonth, FinancialReport.class);
                currentMonth.setId(null);
            } else {
                BaseReportData baseReportData = new BaseReportData();
                baseReportData.setMonth(month);
                baseReportData.setYear(year);
                baseReportData.setReportType(Constants.ReportTypeEnum.FINANCIAL);
                baseReportData.setTitle("Financial Report");

                currentMonth = new FinancialReport();
                currentMonth.setBaseReportData(baseReportData);
                currentMonth.setTotalRevenue(BigDecimal.ZERO);
                currentMonth.setContractCount(0);
                currentMonth.setTax(BigDecimal.ZERO);
                currentMonth.setTotalSalary(BigDecimal.ZERO);
                currentMonth.setNetProfit(BigDecimal.ZERO);
                currentMonth.setTotalRates(0);
                currentMonth.setAvgRating(BigDecimal.ZERO);
                currentMonth.setRevenueCities(new ArrayList<>());
                currentMonth.setRevenueDistricts(new ArrayList<>());
                currentMonth.setRevenueWards(new ArrayList<>());
                currentMonth.setRevenuePropertyTypes(new ArrayList<>());
                currentMonth.setSaleAgentsSalaryMonth(new ArrayList<>());
                currentMonth.setSaleAgentsSalaryCareer(new ArrayList<>());
            }
        }

        currentMonth.setSaleAgentsSalaryMonth(new ArrayList<>());
        currentMonth.getBaseReportData().setDescription(String.format("Financial Report for Bat dong scam in %d, %d", month, year));
        currentMonth.getBaseReportData().setMonth(month);
        currentMonth.getBaseReportData().setYear(year);

        // Remove items not in lists and initialize new items
        updateRankedRevenueItems(currentMonth.getRevenueCities(), cityIds);
        updateRankedRevenueItems(currentMonth.getRevenueDistricts(), districtIds);
        updateRankedRevenueItems(currentMonth.getRevenueWards(), wardIds);
        updateRankedRevenueItems(currentMonth.getRevenuePropertyTypes(), propertyTypeIds);

        updateAgentSalaryItems(currentMonth.getSaleAgentsSalaryMonth(), agentIds);
        updateAgentSalaryItems(currentMonth.getSaleAgentsSalaryCareer(), agentIds);

        recalculateRevenueAndContracts(currentMonth, month, year);

        financialReportRepository.save(currentMonth);

        return CompletableFuture.completedFuture(null);
    }

    private void recalculateRevenueAndContracts(FinancialReport report, int month, int year) {
        // Get contract count from signed contracts
        int contractCount = contractRepository.countSignedInMonth(month, year);

        // Get revenue payments in this month (SUCCESS status, excluding certain payment types)
        List<Constants.PaymentTypeEnum> excludedTypes = List.of(
            Constants.PaymentTypeEnum.SALARY,
            Constants.PaymentTypeEnum.BONUS,
            Constants.PaymentTypeEnum.MONEY_SALE,
            Constants.PaymentTypeEnum.MONEY_RENTAL,
            Constants.PaymentTypeEnum.PENALTY
        );
        List<Payment> revenuePayments = paymentRepository.findRevenuePaymentsInMonth(
            month,
            year,
            Constants.PaymentStatusEnum.SUCCESS,
            excludedTypes
        );

        // Calculate total revenue
        BigDecimal totalRevenue = BigDecimal.ZERO;

        // Reset revenue
        CompletableFuture<Void> resetCitiesFuture = CompletableFuture.runAsync(() -> {
            for (RankedRevenueItem item : report.getRevenueCities()) {
                item.setRevenue(BigDecimal.ZERO);
            }
        });
        CompletableFuture<Void> resetDistrictsFuture = CompletableFuture.runAsync(() -> {
            for (RankedRevenueItem item : report.getRevenueDistricts()) {
                item.setRevenue(BigDecimal.ZERO);
            }
        });
        CompletableFuture<Void> resetWardsFuture = CompletableFuture.runAsync(() -> {
            for (RankedRevenueItem item : report.getRevenueWards()) {
                item.setRevenue(BigDecimal.ZERO);
            }
        });
        CompletableFuture<Void> resetPropertyTypesFuture = CompletableFuture.runAsync(() -> {
            for (RankedRevenueItem item : report.getRevenuePropertyTypes()) {
                item.setRevenue(BigDecimal.ZERO);
            }
        });
        CompletableFuture.allOf(resetCitiesFuture, resetDistrictsFuture, resetWardsFuture, resetPropertyTypesFuture).join();

        // Process each payment
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Payment payment : revenuePayments) {
            BigDecimal paymentAmount = payment.getAmount();
            if (paymentAmount == null) {
                paymentAmount = BigDecimal.ZERO;
            }
            totalRevenue = totalRevenue.add(paymentAmount);

            if (payment.getProperty() != null
                && payment.getProperty().getPropertyType() != null
                && payment.getProperty().getWard() != null
                && payment.getProperty().getWard().getDistrict() != null
                && payment.getProperty().getWard().getDistrict().getCity() != null) {

                UUID propertyTypeId = payment.getProperty().getPropertyType().getId();
                UUID wardId = payment.getProperty().getWard().getId();
                UUID districtId = payment.getProperty().getWard().getDistrict().getId();
                UUID cityId = payment.getProperty().getWard().getDistrict().getCity().getId();
                final BigDecimal finalPaymentAmount = paymentAmount;

                // Update city revenue in parallel
                futures.add(CompletableFuture.runAsync(() -> {
                    synchronized (report.getRevenueCities()) {
                        for (RankedRevenueItem city : report.getRevenueCities()) {
                            if (cityId.equals(city.getId())) {
                                city.setRevenue(city.getRevenue().add(finalPaymentAmount));
                                break;
                            }
                        }
                    }
                }));

                // Update district revenue in parallel
                futures.add(CompletableFuture.runAsync(() -> {
                    synchronized (report.getRevenueDistricts()) {
                        for (RankedRevenueItem district : report.getRevenueDistricts()) {
                            if (districtId.equals(district.getId())) {
                                district.setRevenue(district.getRevenue().add(finalPaymentAmount));
                                break;
                            }
                        }
                    }
                }));

                // Update ward revenue in parallel
                futures.add(CompletableFuture.runAsync(() -> {
                    synchronized (report.getRevenueWards()) {
                        for (RankedRevenueItem ward : report.getRevenueWards()) {
                            if (wardId.equals(ward.getId())) {
                                ward.setRevenue(ward.getRevenue().add(finalPaymentAmount));
                                break;
                            }
                        }
                    }
                }));

                // Update property type revenue in parallel
                futures.add(CompletableFuture.runAsync(() -> {
                    synchronized (report.getRevenuePropertyTypes()) {
                        for (RankedRevenueItem propertyType : report.getRevenuePropertyTypes()) {
                            if (propertyTypeId.equals(propertyType.getId())) {
                                propertyType.setRevenue(propertyType.getRevenue().add(finalPaymentAmount));
                                break;
                            }
                        }
                    }
                }));
            }
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        report.setTotalRevenue(totalRevenue);
        report.setContractCount(contractCount);

        log.info("Recalculated financial report for {}/{}: {} contracts, {} payments, revenue = {}",
                month, year, contractCount, revenuePayments.size(), totalRevenue);
    }

    // Helper method for RankedRevenueItem lists
    private void updateRankedRevenueItems(List<RankedRevenueItem> items, List<UUID> validIds) {
        if (items == null) {
            items = new ArrayList<>();
        }

        // Remove items not in validIds list
        items.removeIf(item -> item != null && item.getId() != null && !validIds.contains(item.getId()));

        // Initialize new items that are in validIds but not in current list
        Set<UUID> existingIds = items.stream()
                .map(RankedRevenueItem::getId)
                .collect(Collectors.toSet());

        for (UUID validId : validIds) {
            if (!existingIds.contains(validId)) {
                RankedRevenueItem newItem = RankedRevenueItem.builder()
                        .id(validId)
                        .revenue(BigDecimal.ZERO)
                        .build();
                items.add(newItem);
            }
        }
    }

    // Helper method for AgentSalaryItem lists
    private void updateAgentSalaryItems(List<AgentSalaryItem> items, List<UUID> validAgentIds) {
        if (items == null) {
            items = new ArrayList<>();
        }

        // Remove items not in validAgentIds list
        items.removeIf(item -> item != null && item.getAgentId() != null && !validAgentIds.contains(item.getAgentId()));

        // Initialize new items that are in validAgentIds but not in current list
        Set<UUID> existingAgentIds = items.stream()
                .map(AgentSalaryItem::getAgentId)
                .collect(Collectors.toSet());

        for (UUID agentId : validAgentIds) {
            if (!existingAgentIds.contains(agentId)) {
                AgentSalaryItem newItem = AgentSalaryItem.builder()
                        .agentId(agentId)
                        .salary(BigDecimal.ZERO)
                        .paid(BigDecimal.ZERO)
                        .unPaid(BigDecimal.ZERO)
                        .bonus(BigDecimal.ZERO)
                        .build();
                items.add(newItem);
            }
        }
    }

    private FinancialReport recalculateAllSalaryByTheEndOfMonth(FinancialReport previousMonth, FinancialReport currentMonth) {

        BigDecimal netProfit = currentMonth.getNetProfit();
        BigDecimal previousNetProfit = previousMonth != null ? previousMonth.getNetProfit() : BigDecimal.ZERO;
        BigDecimal currentMonthNetProfit = netProfit.subtract(previousNetProfit);

        // Hash map to store for O(1) retrieve
        Map<UUID, BigDecimal> newSalaryAgentMonth = new HashMap<>();

        // First loop update the month salary data
        for (AgentSalaryItem agentSalaryMonth : currentMonth.getSaleAgentsSalaryMonth()) {
            IndividualSalesAgentPerformanceMonth agentMonthPerformance = individualSalesAgentPerformanceMonthRepository
                    .findByAgentIdAndMonthAndYear(agentSalaryMonth.getAgentId(), currentMonth.getBaseReportData().getMonth(), currentMonth.getBaseReportData().getYear());

            if (agentMonthPerformance == null) continue;

            BigDecimal currentSalary = BigDecimal.ZERO;

            switch (agentMonthPerformance.getPerformanceTier()) {
                case BRONZE -> currentSalary = currentMonthNetProfit.multiply(BigDecimal.valueOf(0.02));
                case SILVER -> currentSalary = currentMonthNetProfit.multiply(BigDecimal.valueOf(0.04));
                case GOLD -> currentSalary = currentMonthNetProfit.multiply(BigDecimal.valueOf(0.06));
                case PLATINUM -> currentSalary = currentMonthNetProfit.multiply(BigDecimal.valueOf(0.08));
            }

            // Support task
            BigDecimal supportTaskMoney = BigDecimal.valueOf(5000000).multiply(BigDecimal.valueOf(agentMonthPerformance.getMonthPropertiesAssigned()));

            // Final salary
            currentSalary = currentSalary.add(supportTaskMoney);

            // Update the salary data
            agentSalaryMonth.setSalary(currentSalary);
            agentSalaryMonth.setUnPaid(currentSalary);

            // Update the total money spent for salary
            currentMonth.setTotalSalary(currentMonth.getTotalSalary().add(currentSalary));

            // Store in the hashmap
            newSalaryAgentMonth.put(agentSalaryMonth.getAgentId(), agentSalaryMonth.getSalary());
        }

        // Second loop update the all career salary data
        for (AgentSalaryItem agentSalaryMonth : currentMonth.getSaleAgentsSalaryCareer()) {
            BigDecimal added = newSalaryAgentMonth.getOrDefault(agentSalaryMonth.getAgentId(), BigDecimal.ZERO);
            agentSalaryMonth.setSalary(agentSalaryMonth.getSalary().add(added));
            agentSalaryMonth.setUnPaid(agentSalaryMonth.getSalary());
        }

        return currentMonth;
    }

    /// Fot init data and realtime get
    public void recalculateSalaryForInitData(int month, int year) {

        FinancialReport currentMonthReport = financialReportRepository.findByBaseReportData_MonthAndBaseReportData_Year(month, year);
        if (currentMonthReport == null) {
            log.warn("No financial report found for month {} year {}", month, year);
            return;
        }

        FinancialReport previousMonthReport;
        if (month - 1 == 0) {
            previousMonthReport = financialReportRepository.findByBaseReportData_MonthAndBaseReportData_Year(12, year - 1);
        } else {
            previousMonthReport = financialReportRepository.findByBaseReportData_MonthAndBaseReportData_Year(month - 1, year);
        }

        FinancialReport updatedReport = recalculateAllSalaryByTheEndOfMonth(previousMonthReport, currentMonthReport);
        financialReportRepository.save(updatedReport);
    }
}
