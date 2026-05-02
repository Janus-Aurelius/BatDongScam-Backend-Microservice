package com.se100.bds.services.domains.report.scheduler;

import com.se100.bds.mappers.SimpleMapper;
import com.se100.bds.models.schemas.report.BaseReportData;
import com.se100.bds.models.schemas.report.PropertyStatisticsReport;
import com.se100.bds.repositories.domains.mongo.report.PropertyStatisticsReportRepository;
import com.se100.bds.services.domains.location.LocationService;
import com.se100.bds.services.domains.property.PropertyService;
import com.se100.bds.utils.Constants;
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
    private final PropertyService propertyService;
    private final LocationService locationService;
    private final SimpleMapper simpleMapper;

    @Scheduled(cron = "0 0 0 1 * ?")
    protected void initNewMonthData() {
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        initPropertyStatisticsReportData(currentMonth, currentYear);
    }

    @Async
    public CompletableFuture<Void> initPropertyStatisticsReportData(int month, int year) {
        List<UUID> propertyTypeIds = propertyService.getAllAvailablePropertyTypeIds();
        List<UUID> cityIds = locationService.getAllCityIds();
        List<UUID> wardIds = locationService.getAllWardIds();
        List<UUID> districtIds = locationService.getAllDistrictIds();

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
                currentMonth = simpleMapper.mapTo(previousMonth, PropertyStatisticsReport.class);
                currentMonth.setId(null);
            } else {
                BaseReportData baseReportData = new BaseReportData();
                baseReportData.setMonth(month);
                baseReportData.setYear(year);
                baseReportData.setReportType(Constants.ReportTypeEnum.PROPERTY_STATISTICS);
                baseReportData.setTitle("Property Statistics Report");

                currentMonth = new PropertyStatisticsReport();
                currentMonth.setBaseReportData(baseReportData);
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

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Helper method to update Map<UUID, Integer> with valid IDs.
     * Removes items not in validIds list and initializes new items with 0.
     * Keeps cumulative data from previous month for existing items.
     */
    private void updateLocationMap(Map<UUID, Integer> map, List<UUID> validIds) {
        if (map == null) {
            map = new HashMap<>();
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

