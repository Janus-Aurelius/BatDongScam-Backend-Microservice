package com.se100.bds.services.domains.report.scheduler;

import com.se100.bds.models.schemas.report.BaseReportData;
import com.se100.bds.models.schemas.report.ViolationReportDetails;
import com.se100.bds.repositories.domains.mongo.report.ViolationReportDetailsRepository;
import com.se100.bds.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class ViolationReportScheduler {

    private final ViolationReportDetailsRepository violationReportDetailsRepository;

    @Scheduled(cron = "0 0 0 1 * ?")
    protected void initNewMonthData() {
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        initViolationReportData(currentMonth, currentYear);
    }

    @Async
    public CompletableFuture<Void> initViolationReportData(int month, int year) {
        // Check if report for this month already exists
        ViolationReportDetails existingReport = violationReportDetailsRepository
                .findFirstByBaseReportData_MonthAndBaseReportData_YearOrderByCreatedAtDesc(month, year);

        ViolationReportDetails currentMonth;

        if (existingReport != null) {
            // Report exists - UPDATE with latest data
            log.info("ViolationReportDetails for month {} year {} exists. Updating with latest data.", month, year);
            currentMonth = existingReport;
        } else {
            // Report doesn't exist - CREATE from previous month
            log.info("ViolationReportDetails for month {} year {} not found. Creating new report.", month, year);

            ViolationReportDetails previousMonth;
            if (month - 1 == 0) {
                previousMonth = violationReportDetailsRepository
                        .findFirstByBaseReportData_MonthAndBaseReportData_YearOrderByCreatedAtDesc(12, year - 1);
            } else {
                previousMonth = violationReportDetailsRepository
                        .findFirstByBaseReportData_MonthAndBaseReportData_YearOrderByCreatedAtDesc(month - 1, year);
            }

            if (previousMonth != null) {
                // Copy data from previous month (cumulative)
                currentMonth = ViolationReportDetails.builder()
                        .totalViolationReports(previousMonth.getTotalViolationReports())
                        .avgResolutionTimeHours(previousMonth.getAvgResolutionTimeHours())
                        .accountsSuspended(previousMonth.getAccountsSuspended())
                        .propertiesRemoved(previousMonth.getPropertiesRemoved())
                        .violationTypeCounts(previousMonth.getViolationTypeCounts() != null
                                ? new HashMap<>(previousMonth.getViolationTypeCounts())
                                : new HashMap<>())
                        .build();
            } else {
                // No previous month data - initialize with zeros
                currentMonth = ViolationReportDetails.builder()
                        .totalViolationReports(0)
                        .avgResolutionTimeHours(0)
                        .accountsSuspended(0)
                        .propertiesRemoved(0)
                        .violationTypeCounts(new HashMap<>())
                        .build();
            }

            BaseReportData baseReportData = new BaseReportData();
            baseReportData.setMonth(month);
            baseReportData.setYear(year);
            baseReportData.setReportType(Constants.ReportTypeEnum.VIOLATION);
            baseReportData.setTitle("Violation Report");
            baseReportData.setDescription(String.format("Violation Report for %d, %d", month, year));
            currentMonth.setBaseReportData(baseReportData);
        }

        currentMonth.getBaseReportData().setMonth(month);
        currentMonth.getBaseReportData().setYear(year);

        violationReportDetailsRepository.save(currentMonth);

        return CompletableFuture.completedFuture(null);
    }
}

