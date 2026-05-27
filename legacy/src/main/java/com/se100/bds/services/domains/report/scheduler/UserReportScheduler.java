package com.se100.bds.services.domains.report.scheduler;

import com.se100.bds.models.schemas.ranking.IndividualCustomerPotentialAll;
import com.se100.bds.models.schemas.ranking.IndividualPropertyOwnerContributionAll;
import com.se100.bds.models.schemas.ranking.IndividualSalesAgentPerformanceCareer;
import com.se100.bds.models.schemas.report.AgentPerformanceReport;
import com.se100.bds.models.schemas.report.BaseReportData;
import com.se100.bds.models.schemas.report.CustomerAnalyticsReport;
import com.se100.bds.models.schemas.report.PropertyOwnerContributionReport;
import com.se100.bds.repositories.domains.mongo.ranking.*;
import com.se100.bds.repositories.domains.mongo.report.AgentPerformanceReportRepository;
import com.se100.bds.repositories.domains.mongo.report.CustomerAnalyticsReportRepository;
import com.se100.bds.repositories.domains.mongo.report.PropertyOwnerContributionReportRepository;
import com.se100.bds.services.domains.user.UserService;
import com.se100.bds.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserReportScheduler {
    private final AgentPerformanceReportRepository agentPerformanceReportRepository;
    private final PropertyOwnerContributionReportRepository propertyOwnerContributionReportRepository;
    private final CustomerAnalyticsReportRepository customerAnalyticsReportRepository;
    private final IndividualCustomerPotentialAllRepository individualCustomerPotentialAllRepository;
    private final IndividualSalesAgentPerformanceCareerRepository individualSalesAgentPerformanceCareerRepository;
    private final IndividualPropertyOwnerContributionAllRepository individualPropertyOwnerContributionAllRepository;

    private final UserService userService;

    // Run at 12:00 PM on the last day of every month
    @Scheduled(cron = "0 12 0 L * ?")
    protected void monthlyGenerator() {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        generateAgentPerformanceReport(month, year);
        generatePropertyOwnerContributionReport(month, year);
        generateCustomerAnalyticsReport(month, year);
    }

    public void initData(int month, int year) {
        generateAgentPerformanceReport(month, year);
        generatePropertyOwnerContributionReport(month, year);
        generateCustomerAnalyticsReport(month, year);
    }

    @Async
    public CompletableFuture<Void> generateAgentPerformanceReport(int month, int year) {
        int newThisMonth = userService.countNewUsersByRoleAndMonthAndYear(Constants.RoleEnum.SALESAGENT, month, year);
        int totalAgents = userService.countNewUsersByRoleAndMonthAndYear(Constants.RoleEnum.SALESAGENT, 0, 0);

        // Get all agent career performance data
        List<IndividualSalesAgentPerformanceCareer> allAgentPerformances = individualSalesAgentPerformanceCareerRepository.findAll();

        // Calculate avgCustomerSatisfaction (average of all agents' customerSatisfactionAvg)
        BigDecimal avgCustomerSatisfaction = BigDecimal.ZERO;
        if (!allAgentPerformances.isEmpty()) {
            BigDecimal sumCustomerSatisfaction = allAgentPerformances.stream()
                    .map(IndividualSalesAgentPerformanceCareer::getCustomerSatisfactionAvg)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            avgCustomerSatisfaction = sumCustomerSatisfaction.divide(
                    BigDecimal.valueOf(allAgentPerformances.size()), 2, RoundingMode.HALF_UP);
        }

        // Calculate totalRates (sum of all agents' totalRates)
        Integer totalRates = allAgentPerformances.stream()
                .map(IndividualSalesAgentPerformanceCareer::getTotalRates)
                .filter(java.util.Objects::nonNull)
                .reduce(0, Integer::sum);

        // Calculate avgRating (average of all agents' avgRating)
        BigDecimal avgRating = BigDecimal.ZERO;
        if (!allAgentPerformances.isEmpty()) {
            BigDecimal sumRating = allAgentPerformances.stream()
                    .map(IndividualSalesAgentPerformanceCareer::getAvgRating)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            avgRating = sumRating.divide(
                    BigDecimal.valueOf(allAgentPerformances.size()), 2, RoundingMode.HALF_UP);
        }

        // Create base report data
        BaseReportData baseReportData = new BaseReportData();
        baseReportData.setReportType(Constants.ReportTypeEnum.AGENT_PERFORMANCE);
        baseReportData.setMonth(month);
        baseReportData.setYear(year);
        baseReportData.setTitle("Agent Performance Report - " + month + "/" + year);
        baseReportData.setDescription("Monthly sales agent performance analytics");

        // Check if report already exists
        AgentPerformanceReport existingReport = agentPerformanceReportRepository.findByBaseReportData_MonthAndBaseReportData_Year(
                month, year
        );

        AgentPerformanceReport report;
        if (existingReport != null) {
            // Update existing report
            log.info("AgentPerformanceReport for month {} year {} exists. Updating with latest data.", month, year);
            report = existingReport;
            report.setTotalAgents(totalAgents);
            report.setNewThisMonth(newThisMonth);
            report.setAvgCustomerSatisfaction(avgCustomerSatisfaction);
            report.setTotalRates(totalRates);
            report.setAvgRating(avgRating);
        } else {
            // Create new report
            log.info("AgentPerformanceReport for month {} year {} not found. Creating new report.", month, year);
            report = AgentPerformanceReport.builder()
                    .totalAgents(totalAgents)
                    .newThisMonth(newThisMonth)
                    .avgCustomerSatisfaction(avgCustomerSatisfaction)
                    .totalRates(totalRates)
                    .avgRating(avgRating)
                    .build();
            report.setBaseReportData(baseReportData);
        }

        agentPerformanceReportRepository.save(report);

        log.info("AgentPerformanceReport saved for {}-{}", year, month);
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> generatePropertyOwnerContributionReport(int month, int year) {
        int newThisMonth = userService.countNewUsersByRoleAndMonthAndYear(Constants.RoleEnum.PROPERTY_OWNER, month, year);
        int totalOwners = userService.countNewUsersByRoleAndMonthAndYear(Constants.RoleEnum.PROPERTY_OWNER, 0, 0);

        // Get all property owner contribution data
        List<IndividualPropertyOwnerContributionAll> allOwnerContributions = individualPropertyOwnerContributionAllRepository.findAll();

        // Calculate total contribution value (sum of all owners' contribution values)
        BigDecimal contributionValue = allOwnerContributions.stream()
                .map(IndividualPropertyOwnerContributionAll::getContributionValue)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate average contribution value per owner
        BigDecimal avgOwnersContributionValue = BigDecimal.ZERO;
        if (totalOwners > 0) {
            avgOwnersContributionValue = contributionValue.divide(
                    BigDecimal.valueOf(totalOwners), 2, RoundingMode.HALF_UP);
        }

        // Create base report data
        BaseReportData baseReportData = new BaseReportData();
        baseReportData.setReportType(Constants.ReportTypeEnum.PROPERTY_OWNER_CONTRIBUTION);
        baseReportData.setMonth(month);
        baseReportData.setYear(year);
        baseReportData.setTitle("Property Owner Contribution Report - " + month + "/" + year);
        baseReportData.setDescription("Monthly property owner contribution analytics");

        // Check if report already exists
        PropertyOwnerContributionReport existingReport = propertyOwnerContributionReportRepository.findByBaseReportData_MonthAndBaseReportData_Year(
                month, year
        );

        PropertyOwnerContributionReport report;
        if (existingReport != null) {
            // Update existing report
            log.info("PropertyOwnerContributionReport for month {} year {} exists. Updating with latest data.", month, year);
            report = existingReport;
            report.setTotalOwners(totalOwners);
            report.setNewThisMonth(newThisMonth);
            report.setContributionValue(contributionValue);
            report.setAvgOwnersContributionValue(avgOwnersContributionValue);
        } else {
            // Create new report
            log.info("PropertyOwnerContributionReport for month {} year {} not found. Creating new report.", month, year);
            report = PropertyOwnerContributionReport.builder()
                    .totalOwners(totalOwners)
                    .newThisMonth(newThisMonth)
                    .contributionValue(contributionValue)
                    .avgOwnersContributionValue(avgOwnersContributionValue)
                    .build();
            report.setBaseReportData(baseReportData);
        }

        propertyOwnerContributionReportRepository.save(report);

        log.info("PropertyOwnerContributionReport saved for {}-{}", year, month);
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> generateCustomerAnalyticsReport(int month, int year) {
        int newThisMonth = userService.countNewUsersByRoleAndMonthAndYear(Constants.RoleEnum.CUSTOMER, month, year);
        int totalCustomers = userService.countNewUsersByRoleAndMonthAndYear(Constants.RoleEnum.CUSTOMER, 0, 0);

        // Get all customer potential data
        List<IndividualCustomerPotentialAll> allCustomerPotentials = individualCustomerPotentialAllRepository.findAll();

        // Calculate average customer transaction value (average spending per customer)
        BigDecimal avgCustomerTransactionValue = BigDecimal.ZERO;
        if (!allCustomerPotentials.isEmpty()) {
            BigDecimal totalSpending = allCustomerPotentials.stream()
                    .map(IndividualCustomerPotentialAll::getSpending)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            avgCustomerTransactionValue = totalSpending.divide(
                    BigDecimal.valueOf(allCustomerPotentials.size()), 2, RoundingMode.HALF_UP);
        }

        // Calculate high value customer count (customers with spending above average)
        final BigDecimal finalAvgCustomerTransactionValue = avgCustomerTransactionValue;
        int highValueCustomerCount = (int) allCustomerPotentials.stream()
                .filter(c -> c.getSpending() != null && c.getSpending().compareTo(finalAvgCustomerTransactionValue) > 0)
                .count();

        // Calculate customer satisfaction score (average lead score as proxy)
        BigDecimal customerSatisfactionScore = BigDecimal.ZERO;
        if (!allCustomerPotentials.isEmpty()) {
            BigDecimal totalLeadScore = allCustomerPotentials.stream()
                    .map(c -> c.getLeadScore() != null ? BigDecimal.valueOf(c.getLeadScore()) : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            customerSatisfactionScore = totalLeadScore.divide(
                    BigDecimal.valueOf(allCustomerPotentials.size()), 2, RoundingMode.HALF_UP);
        }

        // Calculate total contracts signed (sum)
        Integer totalRates = allCustomerPotentials.stream()
                .map(IndividualCustomerPotentialAll::getTotalContractsSigned)
                .filter(java.util.Objects::nonNull)
                .reduce(0, Integer::sum);

        // Calculate average rating (using lead score / 20 as proxy rating out of 5)
        BigDecimal avgRating = BigDecimal.ZERO;
        if (!allCustomerPotentials.isEmpty() && customerSatisfactionScore.compareTo(BigDecimal.ZERO) > 0) {
            avgRating = customerSatisfactionScore.divide(BigDecimal.valueOf(20), 2, RoundingMode.HALF_UP);
        }

        // Create base report data
        BaseReportData baseReportData = new BaseReportData();
        baseReportData.setReportType(Constants.ReportTypeEnum.CUSTOMER_ANALYTICS);
        baseReportData.setMonth(month);
        baseReportData.setYear(year);
        baseReportData.setTitle("Customer Analytics Report - " + month + "/" + year);
        baseReportData.setDescription("Monthly customer behavior and satisfaction analytics");

        // Check if report already exists
        CustomerAnalyticsReport existingReport = customerAnalyticsReportRepository.findByBaseReportData_MonthAndBaseReportData_Year(
                month, year
        );

        CustomerAnalyticsReport report;
        if (existingReport != null) {
            // Update existing report
            log.info("CustomerAnalyticsReport for month {} year {} exists. Updating with latest data.", month, year);
            report = existingReport;
            report.setTotalCustomers(totalCustomers);
            report.setNewCustomerAcquiredCurrentMonth(newThisMonth);
            report.setAvgCustomerTransactionValue(avgCustomerTransactionValue);
            report.setHighValueCustomerCount(highValueCustomerCount);
            report.setCustomerSatisfactionScore(customerSatisfactionScore);
            report.setTotalRates(totalRates);
            report.setAvgRating(avgRating);
        } else {
            // Create new report
            log.info("CustomerAnalyticsReport for month {} year {} not found. Creating new report.", month, year);
            report = CustomerAnalyticsReport.builder()
                    .totalCustomers(totalCustomers)
                    .newCustomerAcquiredCurrentMonth(newThisMonth)
                    .avgCustomerTransactionValue(avgCustomerTransactionValue)
                    .highValueCustomerCount(highValueCustomerCount)
                    .customerSatisfactionScore(customerSatisfactionScore)
                    .totalRates(totalRates)
                    .avgRating(avgRating)
                    .build();
            report.setBaseReportData(baseReportData);
        }

        customerAnalyticsReportRepository.save(report);

        log.info("CustomerAnalyticsReport saved for {}-{}", year, month);

        return CompletableFuture.completedFuture(null);
    }
}
