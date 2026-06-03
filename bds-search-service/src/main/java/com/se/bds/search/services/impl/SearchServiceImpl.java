package com.se.bds.search.services.impl;

import com.se.bds.search.models.schemas.report.PropertyStatisticsReport;
import com.se.bds.search.models.schemas.search.SearchLog;
import com.se.bds.search.repositories.PropertyStatisticsReportRepository;
import com.se.bds.search.repositories.SearchLogRepository;
import com.se.bds.search.scheduler.PropertyStatisticsReportScheduler;
import com.se.bds.search.services.SearchService;
import com.se.bds.search.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SearchLogRepository searchLogRepository;
    private final PropertyStatisticsReportRepository propertyStatisticsReportRepository;
    private final MongoTemplate mongoTemplate;
    private final PropertyStatisticsReportScheduler propertyStatisticsReportScheduler;

    @Override
    public void addSearch(UUID userId, UUID cityId, UUID districtId, UUID wardId, UUID propertyId, UUID propertyTypeId) {
        searchLogRepository.save(new SearchLog(userId, cityId, districtId, wardId, propertyId, propertyTypeId));
        incrementSearchStats(Instant.now(), cityId, districtId, wardId, propertyTypeId, propertyId);
    }

    @Async
    @Override
    public void addSearchList(UUID userId, List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds, List<UUID> propertyTypeIds) {
        try {
            UUID cityId = (cityIds != null && !cityIds.isEmpty()) ? cityIds.get(0) : null;
            UUID districtId = (districtIds != null && !districtIds.isEmpty()) ? districtIds.get(0) : null;
            UUID wardId = (wardIds != null && !wardIds.isEmpty()) ? wardIds.get(0) : null;
            UUID propertyTypeId = (propertyTypeIds != null && !propertyTypeIds.isEmpty()) ? propertyTypeIds.get(0) : null;

            searchLogRepository.save(new SearchLog(userId, cityId, districtId, wardId, null, propertyTypeId));
            incrementSearchStats(Instant.now(), cityId, districtId, wardId, propertyTypeId, null);

            log.debug("Search log saved asynchronously for user: {}", userId);
        } catch (Exception e) {
            log.error("Error saving search log asynchronously: {}", e.getMessage());
        }
    }

    @Override
    public List<UUID> topMostSearchByUser(UUID userId, int offset, int limit, Constants.SearchTypeEnum searchType, int year, int month) {
        try {
            Optional<PropertyStatisticsReport> reportOpt = propertyStatisticsReportRepository.findByYearAndMonth(year, month);

            if (reportOpt.isEmpty()) {
                log.warn("No PropertyStatisticsReport found for year {} and month {}", year, month);
                return List.of();
            }

            PropertyStatisticsReport report = reportOpt.get();

            Map<UUID, Integer> rankedMap = getRankedListByType(report, searchType);

            if (rankedMap == null || rankedMap.isEmpty()) {
                return List.of();
            }

            return rankedMap.entrySet().stream()
                     .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                     .skip(offset)
                     .limit(limit)
                     .map(Map.Entry::getKey)
                     .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error finding top searches with offset {} limit {} for user {} with type {} in {}-{}: {}",
                    offset, limit, userId, searchType, year, month, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<UUID> getMostSearchedPropertyIds(int limit, int year, int month) {
        try {
            Optional<PropertyStatisticsReport> reportOpt = propertyStatisticsReportRepository.findByYearAndMonth(year, month);

            if (reportOpt.isEmpty()) {
                log.warn("No PropertyStatisticsReport found for year {} and month {}", year, month);
                return List.of();
            }

            PropertyStatisticsReport report = reportOpt.get();
            Map<UUID, Integer> searchedProperties = report.getSearchedProperties();

            if (searchedProperties == null || searchedProperties.isEmpty()) {
                return List.of();
            }

            return searchedProperties.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                    .limit(limit)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting most searched property IDs with limit {} in {}-{}: {}",
                    limit, year, month, e.getMessage());
            return List.of();
        }
    }

    private void incrementSearchStats(Instant timestamp, UUID cityId, UUID districtId, UUID wardId, UUID propertyTypeId, UUID propertyId) {
        int year = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();
        if (timestamp != null) {
            java.time.ZonedDateTime zdt = timestamp.atZone(ZoneOffset.UTC);
            year = zdt.getYear();
            month = zdt.getMonthValue();
        }
        incrementSearchStats(month, year, cityId, districtId, wardId, propertyTypeId, propertyId);
    }

    private void incrementSearchStats(int month, int year, UUID cityId, UUID districtId, UUID wardId, UUID propertyTypeId, UUID propertyId) {
        try {
            Optional<PropertyStatisticsReport> reportOpt = propertyStatisticsReportRepository.findByYearAndMonth(year, month);
            if (reportOpt.isEmpty()) {
                propertyStatisticsReportScheduler.initPropertyStatisticsReportData(month, year).join();
            }

            Query query = new Query(Criteria.where("base_report_data.year").is(year).and("base_report_data.month").is(month));
            Update update = new Update();
            
            boolean hasUpdates = false;
            if (cityId != null) {
                update.inc("searched_cities." + cityId, 1);
                hasUpdates = true;
            }
            if (districtId != null) {
                update.inc("searched_districts." + districtId, 1);
                hasUpdates = true;
            }
            if (wardId != null) {
                update.inc("searched_wards." + wardId, 1);
                hasUpdates = true;
            }
            if (propertyTypeId != null) {
                update.inc("searched_property_types." + propertyTypeId, 1);
                hasUpdates = true;
            }
            if (propertyId != null) {
                update.inc("searched_properties." + propertyId, 1);
                hasUpdates = true;
            }

            if (hasUpdates) {
                mongoTemplate.updateFirst(query, update, PropertyStatisticsReport.class);
                log.debug("Incremented search statistics for {}-{}", year, month);
            }
        } catch (Exception e) {
            log.error("Failed to increment search statistics for {}-{}: {}", year, month, e.getMessage());
        }
    }

    @Override
    public void handlePropertyStatusChanged(com.se.bds.common.event.PropertyStatusChangedEvent event) {
        int year = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();
        
        try {
            Optional<PropertyStatisticsReport> reportOpt = propertyStatisticsReportRepository.findByYearAndMonth(year, month);
            if (reportOpt.isEmpty()) {
                propertyStatisticsReportScheduler.initPropertyStatisticsReportData(month, year).join();
            }

            Query query = new Query(Criteria.where("base_report_data.year").is(year).and("base_report_data.month").is(month));
            Update update = new Update();
            
            boolean hasUpdates = false;
            
            if (event.oldStatus() != null) {
                String oldStatus = event.oldStatus().toUpperCase();
                if ("AVAILABLE".equals(oldStatus)) {
                    update.inc("total_active_properties", -1);
                    hasUpdates = true;
                } else if ("SOLD".equals(oldStatus)) {
                    update.inc("total_sold_properties", -1);
                    hasUpdates = true;
                } else if ("RENTED".equals(oldStatus)) {
                    update.inc("total_rented_properties", -1);
                    hasUpdates = true;
                }
            }
            
            if (event.newStatus() != null) {
                String newStatus = event.newStatus().toUpperCase();
                if ("AVAILABLE".equals(newStatus)) {
                    update.inc("total_active_properties", 1);
                    hasUpdates = true;
                } else if ("SOLD".equals(newStatus)) {
                    update.inc("total_sold_properties", 1);
                    hasUpdates = true;
                } else if ("RENTED".equals(newStatus)) {
                    update.inc("total_rented_properties", 1);
                    hasUpdates = true;
                }
            }
            
            if (hasUpdates) {
                mongoTemplate.updateFirst(query, update, PropertyStatisticsReport.class);
                log.info("Updated PropertyStatisticsReport counts for propertyId={}: old={}, new={}", 
                        event.propertyId(), event.oldStatus(), event.newStatus());
            }
        } catch (Exception e) {
            log.error("Failed to update status counts for propertyId={}: {}", event.propertyId(), e.getMessage());
        }
    }

    private Map<UUID, Integer> getRankedListByType(PropertyStatisticsReport report,
                                                  Constants.SearchTypeEnum searchType) {
        return switch (searchType) {
            case CITY -> report.getSearchedCities();
            case DISTRICT -> report.getSearchedDistricts();
            case WARD -> report.getSearchedWards();
            case PROPERTY -> report.getSearchedProperties();
            case PROPERTY_TYPE -> report.getSearchedPropertyTypes();
        };
    }
}
