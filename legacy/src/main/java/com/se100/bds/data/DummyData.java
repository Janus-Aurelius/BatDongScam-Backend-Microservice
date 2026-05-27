package com.se100.bds.data;

import com.se100.bds.data.domains.*;
import com.se100.bds.services.domains.report.scheduler.UserReportScheduler;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class DummyData implements CommandLineRunner {

    private final UserDummyData userDummyData;
    private final LocationDummyData locationDummyData;
    private final PropertyTypeDummyData propertyTypeDummyData;
    private final PropertyOwnerDummyData propertyOwnerDummyData;
    private final PropertyDummyData propertyDummyData;
    private final AppointmentDummyData appointmentDummyData;
    private final ContractDummyData contractDummyData;
    private final MediaDummyData mediaDummyData;
    private final PaymentDummyData paymentDummyData;
    private final ReviewDummyData reviewDummyData;
    private final CustomerPreferencesDummyData customerPreferencesDummyData;
    private final NotificationDummyData notificationDummyData;
    private final ViolationDummyData violationDummyData;
    private final DocumentDummyData documentDummyData;
    private final SearchLogDummyData searchLogDummyData;
    private final RankingDummyData rankingDummyData;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing dummy data...");

        log.info("Check if data exists...");
        if (userDummyData.any())
        {
            log.info("Data already exists, skipping dummy data creation.");

            // But check if ranking data exists, create it if missing
            log.info("Checking if ranking data exists...");
            if (!rankingDummyData.rankingDataExists()) {
                log.info("Ranking data missing, creating it now...");
                rankingDummyData.createDummy();
            } else {
                log.info("Ranking data already exists.");
            }

            return;
        }

        // Create data in dependency order

        // 1. Create locations (cities, districts, wards)
        locationDummyData.createDummy();

        // 2. Create property types
        propertyTypeDummyData.createDummy();

        // 3. Create user dummy data (1 Guest, 1 Admin, 10 Sale Agents, 100 Customers)
        userDummyData.createDummy();

        // 4. Create property owners (20 owners)
        propertyOwnerDummyData.createDummy();

        // 5. Create properties (200 properties)
        propertyDummyData.createDummy();

        // 6. Create media for properties (600-1000 media files)
        mediaDummyData.createDummy();

        // 7. Create document types and identification documents
        documentDummyData.createDummy();

        // 8. Create appointments (300 appointments)
        appointmentDummyData.createDummy();

        // 9. Create contracts (50 contracts)
        contractDummyData.createDummy();

        // 10. Create payments for contracts
        paymentDummyData.createDummy();

        // 11. Create reviews for appointments and contracts
        reviewDummyData.createDummy();

        // 12. Create customer preferences (favorites, preferred locations, types)
        customerPreferencesDummyData.createDummy();

        // 13. Create notifications for users
        notificationDummyData.createDummy();

        // 14. Create violation reports
        violationDummyData.createDummy();

        // 16. Create ranking data (monthly and all-time rankings for owners, agents, customers)
        rankingDummyData.createDummy();

        // 15. Create search logs (100k logs with proper hierarchical relationships)
        searchLogDummyData.createDummy();

        log.info("Done initializing dummy data");
    }
}

