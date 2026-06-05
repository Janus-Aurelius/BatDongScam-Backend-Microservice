package com.se.bds.search.scheduler;

import com.se.bds.search.client.CoreServiceClient;
import com.se.bds.search.models.schemas.report.BaseReportData;
import com.se.bds.search.models.schemas.report.PropertyStatisticsReport;
import com.se.bds.search.repositories.PropertyStatisticsReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class PropertyStatisticsReportScheduler {

    private final PropertyStatisticsReportRepository propertyStatisticsReportRepository;
    private final CoreServiceClient coreServiceClient;

    @Scheduled(cron = "0 0 0 1 * ?")
    protected void initNewMonthData() {
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        initPropertyStatisticsReportData(currentMonth, currentYear);
    }

    @Async
    public CompletableFuture<Void> initPropertyStatisticsReportData(int month, int year) {
        log.info("Initializing PropertyStatisticsReport data for month {} year {}", month, year);
        try {
            List<UUID> propertyTypeIds = coreServiceClient.getAllAvailablePropertyTypeIds();
            List<UUID> cityIds = coreServiceClient.getAllCityIds();
            List<UUID> wardIds = coreServiceClient.getAllWardIds();
            List<UUID> districtIds = coreServiceClient.getAllDistrictIds();

            // Check if report for this month already exists
            PropertyStatisticsReport existingReport = propertyStatisticsReportRepository.findFirstByBaseReportData_MonthAndBaseReportData_YearOrderByCreatedAtDesc(
                    month, year
            );

            PropertyStatisticsReport currentMonth;

            if (existingReport != null) {
                // Report exists - UPDATE with latest data
                log.info("PropertyStatisticsReport for month {} year {} exists. Recalculating with latest data.", month, year);
                currentMonth = existingReport;
            } else {
                // Report doesn't exist - CREATE from previous month
                log.info("PropertyStatisticsReport for month {} year {} not found. Creating new report.", month, year);

                PropertyStatisticsReport previousMonth;
                if (month - 1 == 0) {
                    previousMonth = propertyStatisticsReportRepository.findFirstByBaseReportData_MonthAndBaseReportData_YearOrderByCreatedAtDesc(
                            12, year - 1
                    );
                } else {
                    previousMonth = propertyStatisticsReportRepository.findFirstByBaseReportData_MonthAndBaseReportData_YearOrderByCreatedAtDesc(
                            month - 1, year
                    );
                }

                if (previousMonth != null) {
                    currentMonth = PropertyStatisticsReport.builder()
                            .totalActiveProperties(previousMonth.getTotalActiveProperties())
                            .totalSoldProperties(previousMonth.getTotalSoldProperties())
                            .totalRentedProperties(previousMonth.getTotalRentedProperties())
                            .searchedCities(previousMonth.getSearchedCities() != null ? new HashMap<>(previousMonth.getSearchedCities()) : new HashMap<>())
                            .favoriteCities(previousMonth.getFavoriteCities() != null ? new HashMap<>(previousMonth.getFavoriteCities()) : new HashMap<>())
                            .searchedDistricts(previousMonth.getSearchedDistricts() != null ? new HashMap<>(previousMonth.getSearchedDistricts()) : new HashMap<>())
                            .favoriteDistricts(previousMonth.getFavoriteDistricts() != null ? new HashMap<>(previousMonth.getFavoriteDistricts()) : new HashMap<>())
                            .searchedWards(previousMonth.getSearchedWards() != null ? new HashMap<>(previousMonth.getSearchedWards()) : new HashMap<>())
                            .favoriteWards(previousMonth.getFavoriteWards() != null ? new HashMap<>(previousMonth.getFavoriteWards()) : new HashMap<>())
                            .searchedPropertyTypes(previousMonth.getSearchedPropertyTypes() != null ? new HashMap<>(previousMonth.getSearchedPropertyTypes()) : new HashMap<>())
                            .favoritePropertyTypes(previousMonth.getFavoritePropertyTypes() != null ? new HashMap<>(previousMonth.getFavoritePropertyTypes()) : new HashMap<>())
                            .searchedProperties(previousMonth.getSearchedProperties() != null ? new HashMap<>(previousMonth.getSearchedProperties()) : new HashMap<>())
                            .build();
                } else {
                    currentMonth = new PropertyStatisticsReport();
                    currentMonth.setTotalActiveProperties(0);
                    currentMonth.setTotalSoldProperties(0);
                    currentMonth.setTotalRentedProperties(0);
                    currentMonth.setSearchedCities(new HashMap<>());
                    currentMonth.setFavoriteCities(new HashMap<>());
                    currentMonth.setSearchedDistricts(new HashMap<>());
                    currentMonth.setFavoriteDistricts(new HashMap<>());
                    currentMonth.setSearchedWards(new HashMap<>());
                    currentMonth.setFavoriteWards(new HashMap<>());
                    currentMonth.setSearchedPropertyTypes(new HashMap<>());
                    currentMonth.setFavoritePropertyTypes(new HashMap<>());
                    currentMonth.setSearchedProperties(new HashMap<>());
                }

                BaseReportData baseReportData = new BaseReportData();
                baseReportData.setMonth(month);
                baseReportData.setYear(year);
                baseReportData.setReportType("PROPERTY_STATISTICS");
                baseReportData.setTitle("Property Statistics Report");
                currentMonth.setBaseReportData(baseReportData);
            }

            currentMonth.getBaseReportData().setDescription(String.format("Property Statistics Report for Bat dong scam in %d, %d", month, year));
            currentMonth.getBaseReportData().setMonth(month);
            currentMonth.getBaseReportData().setYear(year);

            // Update maps with valid IDs, keeping cumulative data from previous month
            updateLocationMap(currentMonth.getSearchedCities(), cityIds);
            updateLocationMap(currentMonth.getFavoriteCities(), cityIds);
            updateLocationMap(currentMonth.getSearchedDistricts(), districtIds);
            updateLocationMap(currentMonth.getFavoriteDistricts(), districtIds);
            updateLocationMap(currentMonth.getSearchedWards(), wardIds);
            updateLocationMap(currentMonth.getFavoriteWards(), wardIds);
            updateLocationMap(currentMonth.getSearchedPropertyTypes(), propertyTypeIds);
            updateLocationMap(currentMonth.getFavoritePropertyTypes(), propertyTypeIds);

            propertyStatisticsReportRepository.save(currentMonth);
            log.info("Saved PropertyStatisticsReport for month {} year {}", month, year);
        } catch (Exception e) {
            log.error("Failed to initialize property statistics report data", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    private void updateLocationMap(Map<UUID, Integer> map, List<UUID> validIds) {
        if (map == null) {
            return;
        }

        // Remove items not in validIds list
        map.keySet().removeIf(id -> !validIds.contains(id));

        // Initialize new items that are in validIds but not in current map
        for (UUID validId : validIds) {
            if (!map.containsKey(validId)) {
                map.put(validId, 0);
            }
        }
    }
}
