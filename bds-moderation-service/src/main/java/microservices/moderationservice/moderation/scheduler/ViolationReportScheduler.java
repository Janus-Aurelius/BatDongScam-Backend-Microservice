package microservices.moderationservice.moderation.scheduler;

import com.se.bds.common.enums.ReportTypeEnum;
import com.se.bds.common.enums.ViolationStatusEnum;
import com.se.bds.common.enums.PenaltyAppliedEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import microservices.moderationservice.moderation.entity.ViolationReport;
import microservices.moderationservice.moderation.repository.ViolationRepository;
import microservices.moderationservice.moderation.repository.mongo.ViolationReportDetailsRepository;
import microservices.moderationservice.moderation.schema.BaseReportData;
import microservices.moderationservice.moderation.schema.ViolationReportDetails;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class ViolationReportScheduler {

    private final ViolationRepository violationRepository;
    private final ViolationReportDetailsRepository violationReportDetailsRepository;

    @Scheduled(cron = "0 0 0 1 * ?")
    protected void initNewMonthData() {
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        initViolationReportData(currentMonth, currentYear);
    }

    @Async
    public CompletableFuture<Void> initViolationReportData(int month, int year) {
        log.info("Aggregating violation statistics for month {} year {}", month, year);

        LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0, 0);
        LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);

        // Fetch all violations created in this month
        List<ViolationReport> violations = violationRepository.findByCreatedAtBetween(startOfMonth, startOfNextMonth);

        int totalViolations = violations.size();
        int accountsSuspended = 0;
        int propertiesRemoved = 0;
        long totalResolutionTimeSeconds = 0;
        int resolvedCount = 0;

        Map<String, Integer> violationTypeCounts = new HashMap<>();

        for (ViolationReport v : violations) {
            String typeStr = v.getViolationType().name();
            violationTypeCounts.put(typeStr, violationTypeCounts.getOrDefault(typeStr, 0) + 1);

            if (v.getStatus() == ViolationStatusEnum.RESOLVED) {
                resolvedCount++;
                if (v.getResolvedAt() != null && v.getCreatedAt() != null) {
                    totalResolutionTimeSeconds += Duration.between(v.getCreatedAt(), v.getResolvedAt()).toSeconds();
                }
                if (v.getPenaltyApplied() == PenaltyAppliedEnum.SUSPENDED_ACCOUNT) {
                    accountsSuspended++;
                } else if (v.getPenaltyApplied() == PenaltyAppliedEnum.REMOVED_POST) {
                    propertiesRemoved++;
                }
            }
        }

        int avgResolutionTimeHours = resolvedCount > 0 
                ? (int) (totalResolutionTimeSeconds / (resolvedCount * 3600.0)) 
                : 0;

        // Check if report for this month already exists in MongoDB
        ViolationReportDetails existingReport = violationReportDetailsRepository
                .findFirstByBaseReportData_MonthAndBaseReportData_YearOrderByCreatedAtDesc(month, year);

        ViolationReportDetails currentMonth;

        if (existingReport != null) {
            log.info("ViolationReportDetails for month {} year {} exists. Updating with latest data.", month, year);
            currentMonth = existingReport;
            currentMonth.setTotalViolationReports(totalViolations);
            currentMonth.setAvgResolutionTimeHours(avgResolutionTimeHours);
            currentMonth.setAccountsSuspended(accountsSuspended);
            currentMonth.setPropertiesRemoved(propertiesRemoved);
            currentMonth.setViolationTypeCounts(violationTypeCounts);
        } else {
            log.info("ViolationReportDetails for month {} year {} not found. Creating new report.", month, year);
            currentMonth = ViolationReportDetails.builder()
                    .totalViolationReports(totalViolations)
                    .avgResolutionTimeHours(avgResolutionTimeHours)
                    .accountsSuspended(accountsSuspended)
                    .propertiesRemoved(propertiesRemoved)
                    .violationTypeCounts(violationTypeCounts)
                    .build();

            BaseReportData baseReportData = new BaseReportData();
            baseReportData.setMonth(month);
            baseReportData.setYear(year);
            baseReportData.setReportType(ReportTypeEnum.VIOLATION);
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
