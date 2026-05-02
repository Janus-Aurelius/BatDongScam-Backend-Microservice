package com.se100.bds.services.domains.report.impl;

import com.se100.bds.dtos.responses.admindashboard.*;
import com.se100.bds.dtos.responses.statisticreport.*;
import com.se100.bds.models.schemas.ranking.IndividualSalesAgentPerformanceMonth;
import com.se100.bds.models.schemas.report.*;
import com.se100.bds.models.schemas.ranking.IndividualCustomerPotentialMonth;
import com.se100.bds.models.schemas.ranking.IndividualPropertyOwnerContributionMonth;
import com.se100.bds.repositories.domains.mongo.ranking.IndividualSalesAgentPerformanceMonthRepository;
import com.se100.bds.repositories.domains.mongo.report.*;
import com.se100.bds.repositories.domains.mongo.ranking.IndividualCustomerPotentialMonthRepository;
import com.se100.bds.repositories.domains.mongo.ranking.IndividualPropertyOwnerContributionMonthRepository;
import com.se100.bds.repositories.domains.violation.ViolationRepository;
import com.se100.bds.services.domains.location.LocationService;
import com.se100.bds.services.domains.property.PropertyService;
import com.se100.bds.services.domains.report.ReportService;
import com.se100.bds.services.domains.report.scheduler.FinancialReportScheduler;
import com.se100.bds.services.domains.report.scheduler.PropertyStatisticsReportScheduler;
import com.se100.bds.services.domains.report.scheduler.UserReportScheduler;
import com.se100.bds.services.domains.report.scheduler.ViolationReportScheduler;
import com.se100.bds.services.domains.user.UserService;
import com.se100.bds.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final UserService userService;
    private final UserReportScheduler userReportScheduler;
    private final PropertyStatisticsReportScheduler propertyStatisticsReportScheduler;
    private final FinancialReportScheduler financialReportScheduler;
    private final ViolationReportScheduler violationReportScheduler;
    private final AgentPerformanceReportRepository agentPerformanceReportRepository;
    private final IndividualSalesAgentPerformanceMonthRepository individualSalesAgentPerformanceMonthRepository;
    private final FinancialReportRepository financialReportRepository;

    // Added repositories for CustomerStats and PropertyOwnerStats
    private final CustomerAnalyticsReportRepository customerAnalyticsReportRepository;
    private final PropertyOwnerContributionReportRepository propertyOwnerContributionReportRepository;
    private final IndividualCustomerPotentialMonthRepository individualCustomerPotentialMonthRepository;
    private final IndividualPropertyOwnerContributionMonthRepository individualPropertyOwnerContributionMonthRepository;
    private final LocationService locationService;
    private final PropertyService propertyService;
    private final PropertyStatisticsReportRepository propertyStatisticsReportRepository;
    private final ViolationReportDetailsRepository violationReportDetailsRepository;
    private final ViolationRepository violationRepository;

    @Override
    public AgentPerformanceStats getAgentPerformanceStats(int year) {
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        if (year > currentYear) return null;
        if (currentYear == year) {
            // Generate to get the latest current month data
            // Use join to await them
            userReportScheduler.generateAgentPerformanceReport(currentMonth, year).join();
        }
        List<AgentPerformanceReport> agentPerformanceReports = agentPerformanceReportRepository.findAllByBaseReportData_Year(year);

        AgentPerformanceStats agentPerformanceStats = new AgentPerformanceStats();
        Map<Integer, Integer> totalAgents = new HashMap<>();
        Map<Integer, Double> avgRating = new HashMap<>();
        Map<Integer, Integer> totalRates = new HashMap<>();
        Map<Integer, Double> customerSatisfaction = new HashMap<>();


        for (AgentPerformanceReport agentPerformanceReport : agentPerformanceReports) {
            int month = agentPerformanceReport.getBaseReportData().getMonth();
            totalAgents.put(month, agentPerformanceReport.getTotalAgents());
            avgRating.put(month, agentPerformanceReport.getAvgRating().doubleValue());
            totalRates.put(month, agentPerformanceReport.getTotalRates());
            customerSatisfaction.put(month, agentPerformanceReport.getAvgCustomerSatisfaction().doubleValue());
        }

        agentPerformanceStats.setTotalAgents(totalAgents);
        agentPerformanceStats.setAvgRating(avgRating);
        agentPerformanceStats.setTotalRates(totalRates);
        agentPerformanceStats.setCustomerSatisfaction(customerSatisfaction);

        List<IndividualSalesAgentPerformanceMonth> salesAgentPerformanceMonths = individualSalesAgentPerformanceMonthRepository.findAllByMonthAndYear(currentMonth, year);
        Map<Constants.PerformanceTierEnum, Integer> tierNumber = Arrays.stream(Constants.PerformanceTierEnum.values())
                .collect(Collectors.toMap(e -> e, e -> 0));

        for (IndividualSalesAgentPerformanceMonth salesAgentPerformanceMonth : salesAgentPerformanceMonths) {
            Constants.PerformanceTierEnum tier = salesAgentPerformanceMonth.getPerformanceTier();
            tierNumber.put(tier, tierNumber.get(tier) + 1);
        }

        int totalAgent = salesAgentPerformanceMonths.size();
        Map<Constants.PerformanceTierEnum, Map<Integer, Double>> tierDistribution = new HashMap<>();
        for (Map.Entry<Constants.PerformanceTierEnum, Integer> entry : tierNumber.entrySet()) {
            Constants.PerformanceTierEnum tier = entry.getKey();
            int count = entry.getValue();
            double percentage = totalAgent > 0 ? (double) count / totalAgent * 100 : 0.0;
            tierDistribution.put(tier, Map.of(count, percentage));
        }

        agentPerformanceStats.setTierDistribution(tierDistribution);

        return agentPerformanceStats;
    }

    @Override
    public CustomerStats getCustomerStats(int year) {
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        if (year > currentYear) return null;
        if (currentYear == year) {
            // Generate to get the latest current month data
            userReportScheduler.generateCustomerAnalyticsReport(currentMonth, year).join();
        }
        List<CustomerAnalyticsReport> customerAnalyticsReports = customerAnalyticsReportRepository.findAllByBaseReportData_Year(year);

        CustomerStats customerStats = new CustomerStats();
        Map<Integer, Integer> totalCustomers = new HashMap<>();
        Map<Integer, BigDecimal> totalSpending = new HashMap<>();
        Map<Integer, BigDecimal> avgSpendingPerCustomer = new HashMap<>();

        for (CustomerAnalyticsReport customerAnalyticsReport : customerAnalyticsReports) {
            int month = customerAnalyticsReport.getBaseReportData().getMonth();
            totalCustomers.put(month, customerAnalyticsReport.getTotalCustomers());
            BigDecimal avg = customerAnalyticsReport.getAvgCustomerTransactionValue();
            avgSpendingPerCustomer.put(month, avg);
            BigDecimal total = avg.multiply(BigDecimal.valueOf(customerAnalyticsReport.getTotalCustomers()));
            totalSpending.put(month, total);
        }

        customerStats.setTotalCustomers(totalCustomers);
        customerStats.setTotalSpending(totalSpending);
        customerStats.setAvgSpendingPerCustomer(avgSpendingPerCustomer);

        List<IndividualCustomerPotentialMonth> customerPotentialMonths = individualCustomerPotentialMonthRepository.findAllByMonthAndYear(currentMonth, year);
        Map<Constants.CustomerTierEnum, Integer> tierNumber = Arrays.stream(Constants.CustomerTierEnum.values())
                .collect(Collectors.toMap(e -> e, e -> 0));

        for (IndividualCustomerPotentialMonth customerPotentialMonth : customerPotentialMonths) {
            Constants.CustomerTierEnum tier = customerPotentialMonth.getCustomerTier();
            tierNumber.put(tier, tierNumber.get(tier) + 1);
        }

        int totalCustomer = customerPotentialMonths.size();
        Map<Constants.CustomerTierEnum, Map<Integer, Double>> tierDistribution = new HashMap<>();
        for (Map.Entry<Constants.CustomerTierEnum, Integer> entry : tierNumber.entrySet()) {
            Constants.CustomerTierEnum tier = entry.getKey();
            int count = entry.getValue();
            double percentage = totalCustomer > 0 ? (double) count / totalCustomer * 100 : 0.0;
            tierDistribution.put(tier, Map.of(count, percentage));
        }

        customerStats.setTierDistribution(tierDistribution);

        return customerStats;
    }

    @Override
    public PropertyOwnerStats getPropertyOwnerStats(int year) {
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        if (year > currentYear) return null;
        if (currentYear == year) {
            // Generate to get the latest current month data
            userReportScheduler.generatePropertyOwnerContributionReport(currentMonth, year).join();
        }
        List<PropertyOwnerContributionReport> propertyOwnerContributionReports = propertyOwnerContributionReportRepository.findAllByBaseReportData_Year(year);

        PropertyOwnerStats propertyOwnerStats = new PropertyOwnerStats();
        Map<Integer, Integer> totalOwners = new HashMap<>();
        Map<Integer, BigDecimal> totalContributionValue = new HashMap<>();
        Map<Integer, BigDecimal> avgContributionPerOwner = new HashMap<>();

        for (PropertyOwnerContributionReport propertyOwnerContributionReport : propertyOwnerContributionReports) {
            int month = propertyOwnerContributionReport.getBaseReportData().getMonth();
            totalOwners.put(month, propertyOwnerContributionReport.getTotalOwners());
            totalContributionValue.put(month, propertyOwnerContributionReport.getContributionValue());
            avgContributionPerOwner.put(month, propertyOwnerContributionReport.getAvgOwnersContributionValue());
        }

        propertyOwnerStats.setTotalOwners(totalOwners);
        propertyOwnerStats.setTotalContributionValue(totalContributionValue);
        propertyOwnerStats.setAvgContributionPerOwner(avgContributionPerOwner);

        List<IndividualPropertyOwnerContributionMonth> ownerContributionMonths = individualPropertyOwnerContributionMonthRepository.findAllByMonthAndYear(currentMonth, year);
        Map<Constants.ContributionTierEnum, Integer> tierNumber = Arrays.stream(Constants.ContributionTierEnum.values())
                .collect(Collectors.toMap(e -> e, e -> 0));

        for (IndividualPropertyOwnerContributionMonth ownerContributionMonth : ownerContributionMonths) {
            Constants.ContributionTierEnum tier = ownerContributionMonth.getContributionTier();
            tierNumber.put(tier, tierNumber.get(tier) + 1);
        }

        int totalOwner = ownerContributionMonths.size();
        Map<Constants.ContributionTierEnum, Map<Integer, Double>> tierDistribution = new HashMap<>();
        for (Map.Entry<Constants.ContributionTierEnum, Integer> entry : tierNumber.entrySet()) {
            Constants.ContributionTierEnum tier = entry.getKey();
            int count = entry.getValue();
            double percentage = totalOwner > 0 ? (double) count / totalOwner * 100 : 0.0;
            tierDistribution.put(tier, Map.of(count, percentage));
        }

        propertyOwnerStats.setTierDistribution(tierDistribution);

        return propertyOwnerStats;
    }

    @Override
    public FinancialStats getFinancialStats(int year) {
        int month;
        int currentYear = LocalDate.now().getYear();
        if (year > currentYear) return null;
        if (currentYear == year) {
            month = LocalDate.now().getMonthValue();
        } else
            month = 12;

        FinancialReport financialReport = financialReportRepository.findByBaseReportData_MonthAndBaseReportData_Year(
                month, year
        );

        FinancialStats financialStats = new FinancialStats();
        financialStats.setTotalRevenue(financialReport.getTotalRevenue());
        financialStats.setTax(financialReport.getTax());
        financialStats.setNetProfit(financialReport.getNetProfit());
        financialStats.setAvgRating(financialStats.getAvgRating());
        financialStats.setTotalRates(financialReport.getTotalRates());

        List<FinancialReport> financialReportList = financialReportRepository.findAllByBaseReportData_Year(year);
        Map<Integer, BigDecimal> totalRevenueChart = new HashMap<>();
        Map<Integer, Integer> totalContractsChart = new HashMap<>();
        Map<Integer, BigDecimal> agentSalaryChart = new HashMap<>();
        Map<String, Map<Integer, BigDecimal>> targetRevenueChart = new HashMap<>();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (FinancialReport financialReportItem : financialReportList) {
            int monthI = financialReportItem.getBaseReportData().getMonth();

            totalRevenueChart.put(monthI, financialReportItem.getTotalRevenue());
            totalContractsChart.put(monthI, financialReportItem.getContractCount());
            agentSalaryChart.put(monthI, financialReportItem.getTotalSalary());

            for (RankedRevenueItem city : financialReportItem.getRevenueCities()) {
                futures.add(CompletableFuture.supplyAsync(() ->
                    locationService.getLocationName(city.getId(), Constants.LocationEnum.CITY)
                ).thenAccept(cityName -> {
                    synchronized (targetRevenueChart) {
                        if (!targetRevenueChart.containsKey(cityName)) {
                            targetRevenueChart.put(cityName, new HashMap<>());
                        }
                        targetRevenueChart.get(cityName).put(monthI, city.getRevenue());
                    }
                }));
            }

            for (RankedRevenueItem district : financialReportItem.getRevenueDistricts()) {
                futures.add(CompletableFuture.supplyAsync(() ->
                    locationService.getLocationName(district.getId(), Constants.LocationEnum.DISTRICT)
                ).thenAccept(districtName -> {
                    synchronized (targetRevenueChart) {
                        if (!targetRevenueChart.containsKey(districtName)) {
                            targetRevenueChart.put(districtName, new HashMap<>());
                        }
                        targetRevenueChart.get(districtName).put(monthI, district.getRevenue());
                    }
                }));
            }

            for (RankedRevenueItem ward :  financialReportItem.getRevenueWards()) {
                futures.add(CompletableFuture.supplyAsync(() ->
                    locationService.getLocationName(ward.getId(), Constants.LocationEnum.WARD)
                ).thenAccept(wardName -> {
                    synchronized (targetRevenueChart) {
                        if (!targetRevenueChart.containsKey(wardName)) {
                            targetRevenueChart.put(wardName, new HashMap<>());
                        }
                        targetRevenueChart.get(wardName).put(monthI, ward.getRevenue());
                    }
                }));
            }

            for (RankedRevenueItem propertyType :  financialReportItem.getRevenuePropertyTypes()) {
                futures.add(CompletableFuture.supplyAsync(() ->
                    propertyService.getPropertyTypeName(propertyType.getId())
                ).thenAccept(propertyTypeName -> {
                    synchronized (targetRevenueChart) {
                        if (!targetRevenueChart.containsKey(propertyTypeName)) {
                            targetRevenueChart.put(propertyTypeName, new HashMap<>());
                        }
                        targetRevenueChart.get(propertyTypeName).put(monthI, propertyType.getRevenue());
                    }
                }));
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        financialStats.setTotalRevenueChart(totalRevenueChart);
        financialStats.setTotalContractsChart(totalContractsChart);
        financialStats.setAgentSalaryChart(agentSalaryChart);
        financialStats.setTargetRevenueChart(targetRevenueChart);

        return financialStats;
    }

    @Override
    public PropertyStats getPropertyStats(int year) {
        int month;
        int currentYear = LocalDate.now().getYear();
        if (year > currentYear) return null;
        if (currentYear == year) {
            month = LocalDate.now().getMonthValue();
            // Generate to get the latest current month data
            // Use join to await them
            propertyStatisticsReportScheduler.initPropertyStatisticsReportData(month, year).join();
        } else
            month = 12;

        PropertyStatisticsReport propertyStatisticsReport = propertyStatisticsReportRepository.findFirstByBaseReportData_MonthAndBaseReportData_YearOrderByCreatedAtDesc(
                month, year
        );

        if (propertyStatisticsReport == null) {
            log.warn("No PropertyStatisticsReport found for year {} and month {}", year, month);
            return null;
        }

        PropertyStats propertyStats = new PropertyStats();
        propertyStats.setActiveProperties(propertyStatisticsReport.getTotalActiveProperties());
        propertyStats.setNewProperties(propertyStats.getActiveProperties() - propertyStatisticsReport.getTotalActiveProperties());
        propertyStats.setTotalSold(propertyStatisticsReport.getTotalSoldProperties());
        propertyStats.setTotalRented(propertyStatisticsReport.getTotalRentedProperties());

        List<PropertyStatisticsReport> propertyStatisticsReportList = propertyStatisticsReportRepository.findAllByBaseReportData_Year(year);
        Map<Integer, Integer> totalProperties = new HashMap<>();
        Map<Integer, Integer> totalSoldProperties = new HashMap<>();
        Map<Integer, Integer> totalRentedProperties = new HashMap<>();
        Map<String, Map<Integer, Long>> searchedTargets = new HashMap<>();
        Map<String, Map<Integer, Long>> favoriteTargets = new HashMap<>();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (PropertyStatisticsReport propertyStatisticsReportItem : propertyStatisticsReportList) {
            int monthI = propertyStatisticsReportItem.getBaseReportData().getMonth();

            totalProperties.put(monthI, propertyStatisticsReportItem.getTotalActiveProperties());
            totalSoldProperties.put(monthI, propertyStatisticsReportItem.getTotalSoldProperties());
            totalRentedProperties.put(monthI, propertyStatisticsReportItem.getTotalRentedProperties());

            // Process searched targets
            if (propertyStatisticsReportItem.getSearchedCities() != null) {
                for (Map.Entry<UUID, Integer> entry : propertyStatisticsReportItem.getSearchedCities().entrySet()) {
                    Long count = entry.getValue().longValue();
                    futures.add(CompletableFuture.supplyAsync(() ->
                        locationService.getLocationName(entry.getKey(), Constants.LocationEnum.CITY)
                    ).thenAccept(cityName -> {
                        synchronized (searchedTargets) {
                            if (!searchedTargets.containsKey(cityName)) {
                                searchedTargets.put(cityName, new HashMap<>());
                            }
                            searchedTargets.get(cityName).put(monthI, count);
                        }
                    }));
                }
            }

            if (propertyStatisticsReportItem.getSearchedDistricts() != null) {
                for (Map.Entry<UUID, Integer> entry : propertyStatisticsReportItem.getSearchedDistricts().entrySet()) {
                    Long count = entry.getValue().longValue();
                    futures.add(CompletableFuture.supplyAsync(() ->
                        locationService.getLocationName(entry.getKey(), Constants.LocationEnum.DISTRICT)
                    ).thenAccept(districtName -> {
                        synchronized (searchedTargets) {
                            if (!searchedTargets.containsKey(districtName)) {
                                searchedTargets.put(districtName, new HashMap<>());
                            }
                            searchedTargets.get(districtName).put(monthI, count);
                        }
                    }));
                }
            }

            if (propertyStatisticsReportItem.getSearchedWards() != null) {
                for (Map.Entry<UUID, Integer> entry : propertyStatisticsReportItem.getSearchedWards().entrySet()) {
                    Long count = entry.getValue().longValue();
                    futures.add(CompletableFuture.supplyAsync(() ->
                        locationService.getLocationName(entry.getKey(), Constants.LocationEnum.WARD)
                    ).thenAccept(wardName -> {
                        synchronized (searchedTargets) {
                            if (!searchedTargets.containsKey(wardName)) {
                                searchedTargets.put(wardName, new HashMap<>());
                            }
                            searchedTargets.get(wardName).put(monthI, count);
                        }
                    }));
                }
            }

            if (propertyStatisticsReportItem.getSearchedPropertyTypes() != null) {
                for (Map.Entry<UUID, Integer> entry : propertyStatisticsReportItem.getSearchedPropertyTypes().entrySet()) {
                    Long count = entry.getValue().longValue();
                    futures.add(CompletableFuture.supplyAsync(() ->
                        propertyService.getPropertyTypeName(entry.getKey())
                    ).thenAccept(propertyTypeName -> {
                        synchronized (searchedTargets) {
                            if (!searchedTargets.containsKey(propertyTypeName)) {
                                searchedTargets.put(propertyTypeName, new HashMap<>());
                            }
                            searchedTargets.get(propertyTypeName).put(monthI, count);
                        }
                    }));
                }
            }

            // Process favorite targets
            if (propertyStatisticsReportItem.getFavoriteCities() != null) {
                for (Map.Entry<UUID, Integer> entry : propertyStatisticsReportItem.getFavoriteCities().entrySet()) {
                    Long count = entry.getValue().longValue();
                    futures.add(CompletableFuture.supplyAsync(() ->
                        locationService.getLocationName(entry.getKey(), Constants.LocationEnum.CITY)
                    ).thenAccept(cityName -> {
                        synchronized (favoriteTargets) {
                            if (!favoriteTargets.containsKey(cityName)) {
                                favoriteTargets.put(cityName, new HashMap<>());
                            }
                            favoriteTargets.get(cityName).put(monthI, count);
                        }
                    }));
                }
            }

            if (propertyStatisticsReportItem.getFavoriteDistricts() != null) {
                for (Map.Entry<UUID, Integer> entry : propertyStatisticsReportItem.getFavoriteDistricts().entrySet()) {
                    Long count = entry.getValue().longValue();
                    futures.add(CompletableFuture.supplyAsync(() ->
                        locationService.getLocationName(entry.getKey(), Constants.LocationEnum.DISTRICT)
                    ).thenAccept(districtName -> {
                        synchronized (favoriteTargets) {
                            if (!favoriteTargets.containsKey(districtName)) {
                                favoriteTargets.put(districtName, new HashMap<>());
                            }
                            favoriteTargets.get(districtName).put(monthI, count);
                        }
                    }));
                }
            }

            if (propertyStatisticsReportItem.getFavoriteWards() != null) {
                for (Map.Entry<UUID, Integer> entry : propertyStatisticsReportItem.getFavoriteWards().entrySet()) {
                    Long count = entry.getValue().longValue();
                    futures.add(CompletableFuture.supplyAsync(() ->
                        locationService.getLocationName(entry.getKey(), Constants.LocationEnum.WARD)
                    ).thenAccept(wardName -> {
                        synchronized (favoriteTargets) {
                            if (!favoriteTargets.containsKey(wardName)) {
                                favoriteTargets.put(wardName, new HashMap<>());
                            }
                            favoriteTargets.get(wardName).put(monthI, count);
                        }
                    }));
                }
            }

            if (propertyStatisticsReportItem.getFavoritePropertyTypes() != null) {
                for (Map.Entry<UUID, Integer> entry : propertyStatisticsReportItem.getFavoritePropertyTypes().entrySet()) {
                    Long count = entry.getValue().longValue();
                    futures.add(CompletableFuture.supplyAsync(() ->
                        propertyService.getPropertyTypeName(entry.getKey())
                    ).thenAccept(propertyTypeName -> {
                        synchronized (favoriteTargets) {
                            if (!favoriteTargets.containsKey(propertyTypeName)) {
                                favoriteTargets.put(propertyTypeName, new HashMap<>());
                            }
                            favoriteTargets.get(propertyTypeName).put(monthI, count);
                        }
                    }));
                }
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        propertyStats.setTotalProperties(totalProperties);
        propertyStats.setTotalSoldProperties(totalSoldProperties);
        propertyStats.setTotalRentedProperties(totalRentedProperties);
        propertyStats.setSearchedTargets(searchedTargets);
        propertyStats.setFavoriteTargets(favoriteTargets);

        return propertyStats;
    }

    @Override
    public ViolationReportStats getViolationStats(int year) {
        int month;
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        if (year > currentYear) return null;

        if (currentYear == year) {
            month = currentMonth;
            // Generate to get the latest current month data
            violationReportScheduler.initViolationReportData(month, year).join();
        } else {
            month = 12;
        }

        ViolationReportDetails violationReport = violationReportDetailsRepository
                .findFirstByBaseReportData_MonthAndBaseReportData_YearOrderByCreatedAtDesc(month, year);

        if (violationReport == null) {
            log.warn("No ViolationReportDetails found for year {} and month {}", year, month);
            return null;
        }

        ViolationReportStats violationStats = new ViolationReportStats();
        violationStats.setTotalViolationReports(violationReport.getTotalViolationReports());
        violationStats.setAvgResolutionTimeHours(violationReport.getAvgResolutionTimeHours() != null
                ? violationReport.getAvgResolutionTimeHours().doubleValue() : 0.0);

        // Get newThisMonth and unsolved count in parallel
        java.time.LocalDateTime startOfMonth = java.time.LocalDateTime.of(year, month, 1, 0, 0, 0);
        java.time.LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);

        CompletableFuture<Integer> newThisMonthFuture = CompletableFuture.supplyAsync(() ->
                violationRepository.countByCreatedAtBetween(startOfMonth, startOfNextMonth));

        CompletableFuture<Integer> pendingCountFuture = CompletableFuture.supplyAsync(() ->
                violationRepository.countByStatus(Constants.ViolationStatusEnum.PENDING));

        CompletableFuture<Integer> underReviewCountFuture = CompletableFuture.supplyAsync(() ->
                violationRepository.countByStatus(Constants.ViolationStatusEnum.UNDER_REVIEW));

        // Get all reports for the year to build charts
        List<ViolationReportDetails> violationReportList = violationReportDetailsRepository.findAllByBaseReportData_Year(year);

        Map<Integer, Integer> totalViolationReportChart = new HashMap<>();
        Map<Integer, Integer> accountsSuspendedChart = new HashMap<>();
        Map<Integer, Integer> propertiesRemovedChart = new HashMap<>();
        Map<String, Map<Integer, Long>> violationTrends = new HashMap<>();

        for (ViolationReportDetails reportItem : violationReportList) {
            int monthI = reportItem.getBaseReportData().getMonth();

            totalViolationReportChart.put(monthI, reportItem.getTotalViolationReports() != null
                    ? reportItem.getTotalViolationReports() : 0);
            accountsSuspendedChart.put(monthI, reportItem.getAccountsSuspended() != null
                    ? reportItem.getAccountsSuspended() : 0);
            propertiesRemovedChart.put(monthI, reportItem.getPropertiesRemoved() != null
                    ? reportItem.getPropertiesRemoved() : 0);

            // Process violation type counts for trends (key is already violation type name)
            if (reportItem.getViolationTypeCounts() != null) {
                for (Map.Entry<String, Integer> entry : reportItem.getViolationTypeCounts().entrySet()) {
                    String typeName = entry.getKey();
                    violationTrends.computeIfAbsent(typeName, k -> new HashMap<>())
                            .put(monthI, entry.getValue().longValue());
                }
            }
        }

        // Wait for all counts to complete
        CompletableFuture.allOf(newThisMonthFuture, pendingCountFuture, underReviewCountFuture).join();

        violationStats.setNewThisMonth(newThisMonthFuture.join());
        violationStats.setUnsolved(pendingCountFuture.join() + underReviewCountFuture.join());

        violationStats.setTotalViolationReportChart(totalViolationReportChart);
        violationStats.setAccountsSuspendedChart(accountsSuspendedChart);
        violationStats.setPropertiesRemovedChart(propertiesRemovedChart);
        violationStats.setViolationTrends(violationTrends);

        return violationStats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentSalaryItem> getSalaryByMonthYear(int month, int year) {
        FinancialReport financialReport = financialReportRepository.getFinancialReportByBaseReportData_MonthAndBaseReportData_Year(month, year);
        if (financialReport == null) {
            return List.of();
        }
        return financialReport.getSaleAgentsSalaryMonth();
    }


    @Override
    public DashboardTopStats getDashboardTopStats() {
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();

        // Run init schedulers in parallel for latest data
        CompletableFuture<Void> propertyStatsFuture = propertyStatisticsReportScheduler.initPropertyStatisticsReportData(currentMonth, currentYear);
        CompletableFuture<Void> financialFuture = financialReportScheduler.initFinancialReportData(currentMonth, currentYear);
        CompletableFuture<Void> customerFuture = userReportScheduler.generateCustomerAnalyticsReport(currentMonth, currentYear);
        CompletableFuture<Void> agentFuture = userReportScheduler.generateAgentPerformanceReport(currentMonth, currentYear);
        CompletableFuture<Void> ownerFuture = userReportScheduler.generatePropertyOwnerContributionReport(currentMonth, currentYear);

        // Wait for property stats and financial to complete first (needed for totalProperties and totalContracts)
        CompletableFuture.allOf(propertyStatsFuture, financialFuture).join();

        // Get totalProperties from property statistics report
        PropertyStatisticsReport propertyReport = propertyStatisticsReportRepository
                .findFirstByBaseReportData_MonthAndBaseReportData_YearOrderByCreatedAtDesc(currentMonth, currentYear);
        Integer totalProperties = propertyReport != null ? propertyReport.getTotalActiveProperties() : 0;

        // Get totalContracts and calculate monthRevenue from financial report
        FinancialReport currentFinancialReport = financialReportRepository
                .findByBaseReportData_MonthAndBaseReportData_Year(currentMonth, currentYear);
        Integer totalContracts = currentFinancialReport != null ? currentFinancialReport.getContractCount() : 0;

        // Calculate monthRevenue = current month revenue - previous month revenue
        BigDecimal monthRevenue = BigDecimal.ZERO;
        if (currentFinancialReport != null) {
            BigDecimal currentRevenue = currentFinancialReport.getTotalRevenue() != null
                    ? currentFinancialReport.getTotalRevenue() : BigDecimal.ZERO;

            FinancialReport previousFinancialReport;
            if (currentMonth == 1) {
                previousFinancialReport = financialReportRepository
                        .findByBaseReportData_MonthAndBaseReportData_Year(12, currentYear - 1);
            } else {
                previousFinancialReport = financialReportRepository
                        .findByBaseReportData_MonthAndBaseReportData_Year(currentMonth - 1, currentYear);
            }

            BigDecimal previousRevenue = (previousFinancialReport != null && previousFinancialReport.getTotalRevenue() != null)
                    ? previousFinancialReport.getTotalRevenue() : BigDecimal.ZERO;

            monthRevenue = currentRevenue.subtract(previousRevenue);
        }

        // Wait for user reports to complete
        CompletableFuture.allOf(customerFuture, agentFuture, ownerFuture).join();

        // Get totalUsers from all user reports
        CustomerAnalyticsReport customerReport = customerAnalyticsReportRepository
                .findByBaseReportData_MonthAndBaseReportData_Year(currentMonth, currentYear);
        AgentPerformanceReport agentReport = agentPerformanceReportRepository
                .findByBaseReportData_MonthAndBaseReportData_Year(currentMonth, currentYear);
        PropertyOwnerContributionReport ownerReport = propertyOwnerContributionReportRepository
                .findByBaseReportData_MonthAndBaseReportData_Year(currentMonth, currentYear);

        int totalCustomers = customerReport != null && customerReport.getTotalCustomers() != null ? customerReport.getTotalCustomers() : 0;
        int totalAgents = agentReport != null && agentReport.getTotalAgents() != null ? agentReport.getTotalAgents() : 0;
        int totalOwners = ownerReport != null && ownerReport.getTotalOwners() != null ? ownerReport.getTotalOwners() : 0;
        Integer totalUsers = totalCustomers + totalAgents + totalOwners;

        // Get customerSatisfaction from customer analytics report
        Double customerSatisfaction = customerReport != null && customerReport.getCustomerSatisfactionScore() != null
                ? customerReport.getCustomerSatisfactionScore().doubleValue() : 0.0;

        return DashboardTopStats.builder()
                .totalProperties(totalProperties)
                .totalContracts(totalContracts)
                .monthRevenue(monthRevenue)
                .totalUsers(totalUsers)
                .customerStatisfaction(customerSatisfaction)
                .build();
    }

    @Override
    public DashboardRevenueAndContracts getDashboardRevenueAndContracts(int year) {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        if (year > currentYear) return null;

        // If current year, init all months from 1 to current month
        if (year == currentYear) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int month = 1; month <= currentMonth; month++) {
                futures.add(financialReportScheduler.initFinancialReportData(month, year));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } else {
            // If past year, init all 12 months
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int month = 1; month <= 12; month++) {
                futures.add(financialReportScheduler.initFinancialReportData(month, year));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        List<FinancialReport> financialReports = financialReportRepository.findAllByBaseReportData_Year(year);

        Map<Integer, BigDecimal> revenue = new HashMap<>();
        Map<Integer, Integer> contracts = new HashMap<>();

        for (FinancialReport report : financialReports) {
            int month = report.getBaseReportData().getMonth();
            revenue.put(month, report.getTotalRevenue() != null ? report.getTotalRevenue() : BigDecimal.ZERO);
            contracts.put(month, report.getContractCount() != null ? report.getContractCount() : 0);
        }

        return DashboardRevenueAndContracts.builder()
                .revenue(revenue)
                .contracts(contracts)
                .build();
    }

    @Override
    public DashboardTotalProperties getDashboardTotalProperties(int year) {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        if (year > currentYear) return null;

        // If current year, init to get latest data
        if (year == currentYear) {
            propertyStatisticsReportScheduler.initPropertyStatisticsReportData(currentMonth, year).join();
        }

        List<PropertyStatisticsReport> propertyReports = propertyStatisticsReportRepository.findAllByBaseReportData_Year(year);

        Map<Integer, Integer> totalProperties = new HashMap<>();

        for (PropertyStatisticsReport report : propertyReports) {
            int month = report.getBaseReportData().getMonth();
            totalProperties.put(month, report.getTotalActiveProperties() != null ? report.getTotalActiveProperties() : 0);
        }

        return DashboardTotalProperties.builder()
                .totalProperties(totalProperties)
                .build();
    }

    @Override
    public DashboardPropertyDistribution getDashboardPropertyDistribution(int year) {
        int currentYear = LocalDate.now().getYear();

        if (year > currentYear) return null;

        // Get all property type IDs
        List<UUID> propertyTypeIds = propertyService.getAllAvailablePropertyTypeIds();

        if (propertyTypeIds == null || propertyTypeIds.isEmpty()) {
            return DashboardPropertyDistribution.builder()
                    .propertyTypes(new ArrayList<>())
                    .build();
        }

        // Get count for each property type in parallel
        List<DashboardPropertyDistribution.PropertyTypeDistribution> propertyTypes = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // First, get all counts in parallel
        Map<UUID, Integer> typeCountMap = new HashMap<>();
        List<CompletableFuture<Void>> countFutures = new ArrayList<>();

        for (UUID typeId : propertyTypeIds) {
            countFutures.add(CompletableFuture.supplyAsync(() ->
                propertyService.countPropertiesByPropertyTypeId(typeId)
            ).thenAccept(count -> {
                synchronized (typeCountMap) {
                    typeCountMap.put(typeId, count);
                }
            }));
        }

        CompletableFuture.allOf(countFutures.toArray(new CompletableFuture[0])).join();

        // Calculate grand total
        int grandTotal = typeCountMap.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        final int totalCount = grandTotal > 0 ? grandTotal : 1; // Avoid division by zero

        // Now get names and build result in parallel
        for (Map.Entry<UUID, Integer> entry : typeCountMap.entrySet()) {
            UUID typeId = entry.getKey();
            Integer count = entry.getValue();
            double percentage = (count * 100.0) / totalCount;

            futures.add(CompletableFuture.supplyAsync(() ->
                    propertyService.getPropertyTypeName(typeId)
            ).thenAccept(typeName -> {
                DashboardPropertyDistribution.PropertyTypeDistribution item =
                        new DashboardPropertyDistribution.PropertyTypeDistribution();
                item.setTypeName(typeName);
                item.setCount(count);
                item.setPercentage(percentage);

                synchronized (propertyTypes) {
                    propertyTypes.add(item);
                }
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return DashboardPropertyDistribution.builder()
                .propertyTypes(propertyTypes)
                .build();
    }

    @Override
    public DashboardAgentRanking getDashboardAgentRanking(int month, int year) {
        int currentYear = LocalDate.now().getYear();

        if (year > currentYear) return null;

        // Adjust month if past year
        if (year < currentYear) {
            month = 12;
        }

        // Get all agent performance records for this month/year
        List<IndividualSalesAgentPerformanceMonth> agentPerformances = individualSalesAgentPerformanceMonthRepository
                .findAllByMonthAndYear(month, year);

        // Sort by ranking position and take top 5
        List<IndividualSalesAgentPerformanceMonth> top5Agents = agentPerformances.stream()
                .filter(a -> a.getRankingPosition() != null)
                .sorted(Comparator.comparingInt(IndividualSalesAgentPerformanceMonth::getRankingPosition))
                .limit(5)
                .toList();

        List<DashboardAgentRanking.AgentItem> agentItems = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (IndividualSalesAgentPerformanceMonth agent : top5Agents) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return userService.getUserById(agent.getAgentId());
                } catch (Exception e) {
                    log.warn("Failed to get user info for agent {}: {}", agent.getAgentId(), e.getMessage());
                    return null;
                }
            }).thenAccept(userResponse -> {
                DashboardAgentRanking.AgentItem item = new DashboardAgentRanking.AgentItem();
                item.setRank(agent.getRankingPosition());
                item.setTier(agent.getPerformanceTier() != null ? agent.getPerformanceTier().name() : null);
                item.setRating(agent.getAvgRating() != null ? agent.getAvgRating().doubleValue() : 0.0);
                item.setTotalAppointmentsCompleted(agent.getMonthAppointmentsCompleted() != null ? agent.getMonthAppointmentsCompleted() : 0);
                item.setTotalContractsSigned(agent.getMonthContracts() != null ? agent.getMonthContracts() : 0);

                if (userResponse != null) {
                    item.setFirstName(userResponse.getFirstName() != null ? userResponse.getFirstName() : "Unknown");
                    item.setLastName(userResponse.getLastName() != null ? userResponse.getLastName() : "Agent");
                } else {
                    item.setFirstName("Unknown");
                    item.setLastName("Agent");
                }

                synchronized (agentItems) {
                    agentItems.add(item);
                }
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Sort by rank after parallel processing
        agentItems.sort(Comparator.comparingInt(DashboardAgentRanking.AgentItem::getRank));

        return DashboardAgentRanking.builder()
                .agents(agentItems)
                .build();
    }

    @Override
    public DashboardCustomerRanking getDashboardCustomerRanking(int month, int year) {
        int currentYear = LocalDate.now().getYear();

        if (year > currentYear) return null;

        // Adjust month if past year
        if (year < currentYear) {
            month = 12;
        }

        // Get all customer potential records for this month/year
        List<IndividualCustomerPotentialMonth> customerPotentials = individualCustomerPotentialMonthRepository
                .findAllByMonthAndYear(month, year);

        // Sort by lead position and take top 5
        List<IndividualCustomerPotentialMonth> top5Customers = customerPotentials.stream()
                .filter(c -> c.getLeadPosition() != null)
                .sorted(Comparator.comparingInt(IndividualCustomerPotentialMonth::getLeadPosition))
                .limit(5)
                .toList();

        List<DashboardCustomerRanking.CustomerItem> customerItems = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (IndividualCustomerPotentialMonth customer : top5Customers) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return userService.getUserById(customer.getCustomerId());
                } catch (Exception e) {
                    log.warn("Failed to get user info for customer {}: {}", customer.getCustomerId(), e.getMessage());
                    return null;
                }
            }).thenAccept(userResponse -> {
                DashboardCustomerRanking.CustomerItem item = new DashboardCustomerRanking.CustomerItem();
                item.setRank(customer.getLeadPosition());
                item.setTier(customer.getCustomerTier() != null ? customer.getCustomerTier().name() : null);

                if (userResponse != null) {
                    item.setFirstName(userResponse.getFirstName() != null ? userResponse.getFirstName() : "Unknown");
                    item.setLastName(userResponse.getLastName() != null ? userResponse.getLastName() : "Customer");
                } else {
                    item.setFirstName("Unknown");
                    item.setLastName("Customer");
                }

                synchronized (customerItems) {
                    customerItems.add(item);
                }
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Sort by rank after parallel processing
        customerItems.sort(Comparator.comparingInt(DashboardCustomerRanking.CustomerItem::getRank));

        return DashboardCustomerRanking.builder()
                .customers(customerItems)
                .build();
    }
}