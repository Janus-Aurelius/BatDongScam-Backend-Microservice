package com.se100.bds.data.domains;

import com.se100.bds.data.util.TimeGenerator;
import com.se100.bds.models.entities.property.Media;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.models.entities.violation.ViolationReport;
import com.se100.bds.models.schemas.report.ViolationReportDetails;
import com.se100.bds.repositories.domains.mongo.report.ViolationReportDetailsRepository;
import com.se100.bds.repositories.domains.property.PropertyRepository;
import com.se100.bds.repositories.domains.user.UserRepository;
import com.se100.bds.repositories.domains.violation.ViolationRepository;
import com.se100.bds.services.domains.report.scheduler.ViolationReportScheduler;
import com.se100.bds.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
@Service
public class ViolationDummyData {

    private final ViolationRepository violationRepository;
    private final ViolationReportDetailsRepository violationReportDetailsRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final ViolationReportScheduler violationReportScheduler;
    private final Random random = new Random();
    private final TimeGenerator timeGenerator = new TimeGenerator();

    /**
     * Check if violation data already exists
     */
    public boolean any() {
        try {
            return violationRepository.count() > 0;
        } catch (Exception e) {
            log.warn("Could not check violation data existence (table might not exist yet): {}", e.getMessage());
            return false;
        }
    }

    public void createDummy() {
        try {
            // Check if violation reports already exist
            long existingCount = violationRepository.count();
            if (existingCount > 0) {
                log.info("Violation reports already exist ({}), skipping creation", existingCount);
                return;
            }

            log.info("Creating violation dummy data...");
            createDummyViolations();
            createDummyViolationReportDetails();
            initViolationReportData();
            log.info("Violation dummy data created successfully");

        } catch (Exception e) {
            log.error("Failed to create violation dummy data. This might be due to schema not being created yet.", e);
            log.warn("You may need to restart the application after schema is fully created.");
        }
    }

    private void initViolationReportData() {
        log.info("Initializing violation report data using scheduler...");

        int currentMonth = LocalDateTime.now().getMonthValue();
        int currentYear = LocalDateTime.now().getYear();

        int startYear = 2024;
        int startMonth = 1;
        int year = startYear;
        int month = startMonth;

        while (year < currentYear || (year == currentYear && month <= currentMonth)) {
            violationReportScheduler.initViolationReportData(month, year).join();

            month++;
            if (month > 12) {
                month = 1;
                year++;
            }
        }

        log.info("Done initializing violation report data");
    }

    private void createDummyViolations() {
        log.info("Creating dummy violation reports");

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.warn("Cannot create violations - no users found");
            return;
        }

        List<Property> properties = propertyRepository.findAll();
        if (properties.isEmpty()) {
            log.warn("Cannot create violations - no properties found");
            return;
        }

        List<ViolationReport> violations = new ArrayList<>();

        Constants.ViolationTypeEnum[] violationTypes = Constants.ViolationTypeEnum.values();
        Constants.ViolationStatusEnum[] statuses = Constants.ViolationStatusEnum.values();

        // Create 50 violation reports (mix of properties and users)
        for (int i = 0; i < 50; i++) {
            // Randomly choose what to report: PROPERTY (60%), CUSTOMER (20%), or PROPERTY_OWNER (20%)
            Constants.ViolationReportedTypeEnum reportedType;
            Object reportedEntity;

            double rand = random.nextDouble();
            if (rand < 0.6 && !properties.isEmpty()) {
                // Report a property
                reportedType = Constants.ViolationReportedTypeEnum.PROPERTY;
                reportedEntity = properties.get(random.nextInt(properties.size()));
            } else {
                // Report a user (customer or property owner)
                User reportedUser = users.get(random.nextInt(users.size()));
                if (reportedUser.getRole() == Constants.RoleEnum.CUSTOMER) {
                    reportedType = Constants.ViolationReportedTypeEnum.CUSTOMER;
                } else if (reportedUser.getRole() == Constants.RoleEnum.PROPERTY_OWNER) {
                    reportedType = Constants.ViolationReportedTypeEnum.PROPERTY_OWNER;
                } else if (reportedUser.getRole() == Constants.RoleEnum.SALESAGENT) {
                    reportedType = Constants.ViolationReportedTypeEnum.SALES_AGENT;
                } else {
                    // Default to customer if role doesn't match
                    reportedType = Constants.ViolationReportedTypeEnum.CUSTOMER;
                }
                reportedEntity = reportedUser;
            }

            // Every violation must have a reporter (no anonymous reports)
            User reporter = users.get(random.nextInt(users.size()));

            Constants.ViolationTypeEnum violationType = violationTypes[random.nextInt(violationTypes.length)];
            Constants.ViolationStatusEnum status = statuses[random.nextInt(statuses.length)];

            LocalDateTime createdAt = timeGenerator.getRandomTime();
            LocalDateTime updatedAt = timeGenerator.getRandomTimeAfter(createdAt, LocalDateTime.now());
            LocalDateTime resolvedAt = status == Constants.ViolationStatusEnum.RESOLVED ?
                    timeGenerator.getRandomTimeAfter(updatedAt, LocalDateTime.now()) : null;

            ViolationReport violation = ViolationReport.builder()
                    .reporterUser(reporter)
                    .relatedEntityType(reportedType)
                    .relatedEntityId(reportedType == Constants.ViolationReportedTypeEnum.PROPERTY
                            ? ((Property) reportedEntity).getId()
                            : ((User) reportedEntity).getId())
                    .violationType(violationType)
                    .description(generateViolationDescription(violationType, reportedType))
                    .status(status)
                    .resolutionNotes(status == Constants.ViolationStatusEnum.RESOLVED ? generateResolutionNotes() : null)
                    .resolvedAt(resolvedAt)
                    .penaltyApplied(status == Constants.ViolationStatusEnum.RESOLVED && random.nextBoolean()
                            ? Constants.PenaltyAppliedEnum.values()[random.nextInt(Constants.PenaltyAppliedEnum.values().length)]
                            : null)
                    .mediaList(new ArrayList<>())
                    .build();

            violation.setCreatedAt(createdAt);
            violation.setUpdatedAt(updatedAt);

            int mediaCount = 1 + random.nextInt(5); // 1-5 media files
            for (int j = 0; j < mediaCount; j++) {
                Media media = createViolationEvidenceMedia(violation, j);
                violation.getMediaList().add(media);
            }

            violations.add(violation);
        }

        violationRepository.saveAll(violations);
        log.info("Saved {} violation reports to database", violations.size());
    }

    private String generateViolationDescription(Constants.ViolationTypeEnum violationType, Constants.ViolationReportedTypeEnum reportedType) {
        String entityType = reportedType == Constants.ViolationReportedTypeEnum.PROPERTY ? "property" : "user";

        return switch (violationType) {
            case FRAUDULENT_LISTING -> "This " + entityType + " contains false information and misleading claims.";
            case MISREPRESENTATION_OF_PROPERTY ->
                    "Property details do not match the actual condition or specifications.";
            case SPAM_OR_DUPLICATE_LISTING -> "Multiple identical listings posted for the same property.";
            case INAPPROPRIATE_CONTENT ->
                    "This " + entityType + " contains inappropriate images or offensive language.";
            case NON_COMPLIANCE_WITH_TERMS -> "User violated platform terms and conditions.";
            case FAILURE_TO_DISCLOSE_INFORMATION -> "Critical information was not disclosed properly.";
            case HARASSMENT -> "User engaged in harassment or abusive behavior towards other users.";
            case SCAM_ATTEMPT -> "Suspected scam or fraudulent activity detected.";
            default -> "Violation of platform policies.";
        };
    }

    private String generateResolutionNotes() {
        String[] notes = {
                "Warning issued to user. Listing has been corrected.",
                "User account temporarily suspended. Violation resolved after compliance.",
                "Listing removed. User educated on platform policies.",
                "False report. No violation found after investigation.",
                "User provided correct information. Case closed."
        };
        return notes[random.nextInt(notes.length)];
    }

    private Media createViolationEvidenceMedia(ViolationReport violation, int index) {
        // 80% images, 20% documents for evidence
        Constants.MediaTypeEnum mediaType = random.nextDouble() < 0.8
                ? Constants.MediaTypeEnum.IMAGE
                : Constants.MediaTypeEnum.DOCUMENT;

        String fileName;
        String filePath;
        String mimeType;

        if (mediaType == Constants.MediaTypeEnum.IMAGE) {
            fileName = "violation_evidence_" + (index + 1) + ".jpg";
            filePath = "https://res.cloudinary.com/bds-platform/image/upload/violations/evidence_" + (index + 1) + ".jpg";
            mimeType = "image/jpeg";
        } else {
            fileName = "violation_document_" + (index + 1) + ".pdf";
            filePath = "https://res.cloudinary.com/bds-platform/raw/upload/violations/document_" + (index + 1) + ".pdf";
            mimeType = "application/pdf";
        }

        return Media.builder()
                .violationReport(violation)
                .mediaType(mediaType)
                .fileName(fileName)
                .filePath(filePath)
                .mimeType(mimeType)
                .build();
    }

    private void createDummyViolationReportDetails() {
        log.info("Creating dummy violation report details");

        YearMonth currentMonth = YearMonth.now();
        List<ViolationReportDetails> reportDetailsList = new ArrayList<>();

        // Violation types for tracking
        Constants.ViolationTypeEnum[] violationTypes = Constants.ViolationTypeEnum.values();

        // Create reports for the last 22 months
        for (int i = 0; i < 22; i++) {
            YearMonth reportMonth = currentMonth.minusMonths(i);
            int year = reportMonth.getYear();
            int month = reportMonth.getMonthValue();

            // Calculate statistics for this month
            int totalViolations = 15 + random.nextInt(25); // 15-40 violations
            int currentMonthViolations = i == 0 ? (10 + random.nextInt(15)) : 0; // Only current month has this value
            int avgResolutionTime = 24 + random.nextInt(96); // 24-120 hours (1-5 days)
            int accountsSuspended = random.nextInt(8); // 0-7 accounts suspended
            int propertiesRemoved = random.nextInt(12); // 0-11 properties removed

            // Generate violation type counts with Map (key is violation type name)
            Map<String, Integer> violationTypeCounts = new HashMap<>();
            for (Constants.ViolationTypeEnum violationType : violationTypes) {
                // Generate random count for each violation type (0-15)
                int count = random.nextInt(16);
                if (count > 0) { // Only add if there are violations of this type
                    violationTypeCounts.put(violationType.getValue(), count);
                }
            }

            // Create base report data
            com.se100.bds.models.schemas.report.BaseReportData baseReportData = new com.se100.bds.models.schemas.report.BaseReportData();
            baseReportData.setReportType(Constants.ReportTypeEnum.VIOLATION);
            baseReportData.setMonth(month);
            baseReportData.setYear(year);
            baseReportData.setTitle("Violation Report - " + reportMonth.getMonth() + " " + year);
            baseReportData.setDescription("Monthly violation statistics and resolution metrics");

            // Create violation report details
            ViolationReportDetails reportDetails = ViolationReportDetails.builder()
                    .totalViolationReports(totalViolations)
                    .avgResolutionTimeHours(avgResolutionTime)
                    .accountsSuspended(accountsSuspended)
                    .propertiesRemoved(propertiesRemoved)
                    .violationTypeCounts(violationTypeCounts)
                    .build();

            reportDetails.setBaseReportData(baseReportData);
            reportDetailsList.add(reportDetails);

            log.info("Created violation report for {}/{}: {} total violations, {} violation types, {} avg resolution hours, {} accounts suspended, {} properties removed",
                    month, year, totalViolations, violationTypeCounts.size(), avgResolutionTime, accountsSuspended, propertiesRemoved);
        }

        violationReportDetailsRepository.saveAll(reportDetailsList);
        log.info("Saved {} violation report details to MongoDB", reportDetailsList.size());
    }
}
