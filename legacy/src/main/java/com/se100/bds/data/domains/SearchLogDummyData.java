package com.se100.bds.data.domains;

import com.se100.bds.models.entities.location.City;
import com.se100.bds.models.entities.location.District;
import com.se100.bds.models.entities.location.Ward;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.models.entities.property.PropertyType;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.models.schemas.customer.CustomerFavoriteProperty;
import com.se100.bds.models.schemas.report.BaseReportData;
import com.se100.bds.models.schemas.report.PropertyStatisticsReport;
import com.se100.bds.models.schemas.search.SearchLog;
import com.se100.bds.repositories.domains.location.CityRepository;
import com.se100.bds.repositories.domains.location.DistrictRepository;
import com.se100.bds.repositories.domains.location.WardRepository;
import com.se100.bds.repositories.domains.mongo.customer.CustomerFavoritePropertyRepository;
import com.se100.bds.repositories.domains.mongo.report.PropertyStatisticsReportRepository;
import com.se100.bds.repositories.domains.mongo.search.SearchLogRepository;
import com.se100.bds.repositories.domains.property.PropertyRepository;
import com.se100.bds.repositories.domains.property.PropertyTypeRepository;
import com.se100.bds.repositories.domains.user.UserRepository;
import com.se100.bds.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class SearchLogDummyData {

    private final SearchLogRepository searchLogRepository;
    private final PropertyStatisticsReportRepository propertyStatisticsReportRepository;
    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyTypeRepository propertyTypeRepository;
    private final UserRepository userRepository;
    private final CustomerFavoritePropertyRepository customerFavoritePropertyRepository;

    private final Random random = new Random();

    @Transactional(readOnly = true)
    public void createDummy() {
        log.info("Creating search logs per month from January 2024 to December 2025 (async processing)...");

        // Fetch all necessary data ONCE
        List<UUID> userIds = userRepository.findAll().stream().map(User::getId).toList();
        List<UUID> cityIds = cityRepository.findAll().stream().map(City::getId).toList();

        // Build simple data structures with IDs only
        List<LocationData> districtDataList = new ArrayList<>();
        for (District district : districtRepository.findAll()) {
            districtDataList.add(new LocationData(district.getId(), district.getCity().getId()));
        }

        List<LocationData> wardDataList = new ArrayList<>();
        for (Ward ward : wardRepository.findAll()) {
            wardDataList.add(new LocationData(ward.getId(), ward.getDistrict().getId()));
        }

        List<PropertyData> propertyDataList = new ArrayList<>();
        for (Property property : propertyRepository.findAll()) {
            propertyDataList.add(new PropertyData(
                property.getId(),
                property.getWard().getId(),
                property.getPropertyType() != null ? property.getPropertyType().getId() : null
            ));
        }

        List<UUID> propertyTypeIds = propertyTypeRepository.findAll().stream()
            .map(PropertyType::getId).toList();

        // Create lookup maps
        Map<UUID, UUID> districtToCityMap = new HashMap<>();
        for (LocationData data : districtDataList) {
            districtToCityMap.put(data.id, data.parentId);
        }

        Map<UUID, UUID> wardToDistrictMap = new HashMap<>();
        for (LocationData data : wardDataList) {
            wardToDistrictMap.put(data.id, data.parentId);
        }

        log.info("Found {} users, {} cities, {} districts, {} wards, {} properties, {} property types",
                userIds.size(), cityIds.size(), districtDataList.size(), wardDataList.size(),
                propertyDataList.size(), propertyTypeIds.size());

        // Generate data for each month from January 2024 to December 2025
        YearMonth startMonth = YearMonth.of(2024, 1);
        YearMonth endMonth = YearMonth.of(2025, 12);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        YearMonth currentMonth = startMonth;

        while (!currentMonth.isAfter(endMonth)) {
            final YearMonth month = currentMonth;
            final LocalDateTime monthStart = month.atDay(1).atStartOfDay();
            final LocalDateTime monthEnd = month.atEndOfMonth().atTime(23, 59, 59);

            // Process each month asynchronously
            CompletableFuture<Void> future = processMonthAsync(
                month.getYear(), month.getMonthValue(),
                monthStart, monthEnd,
                userIds, cityIds, districtDataList, wardDataList,
                propertyDataList, propertyTypeIds,
                districtToCityMap, wardToDistrictMap
            );

            futures.add(future);
            currentMonth = currentMonth.plusMonths(1);
        }

        // Wait for all months to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Successfully created search logs and reports for all months");
    }

    @Async
    public CompletableFuture<Void> processMonthAsync(
            int year, int month,
            LocalDateTime monthStart,
            LocalDateTime monthEnd,
            List<UUID> userIds,
            List<UUID> cityIds,
            List<LocationData> districtDataList,
            List<LocationData> wardDataList,
            List<PropertyData> propertyDataList,
            List<UUID> propertyTypeIds,
            Map<UUID, UUID> districtToCityMap,
            Map<UUID, UUID> wardToDistrictMap
    ) {
        try {
            log.info("Processing month: {}-{} [Thread: {}]", year, month, Thread.currentThread().getName());

            // Generate search logs for this month
            generateSearchLogsForMonth(
                monthStart, monthEnd,
                userIds, cityIds, districtDataList, wardDataList,
                propertyDataList, propertyTypeIds,
                districtToCityMap, wardToDistrictMap
            );

            // Generate PropertyStatisticsReport for this month
            generatePropertyStatisticsReport(
                year, month,
                monthStart, monthEnd,
                cityIds, districtDataList, wardDataList, propertyTypeIds
            );

            log.info("Completed month: {}-{}", year, month);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Error processing month {}-{}: {}", year, month, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private void generateSearchLogsForMonth(
            LocalDateTime monthStart,
            LocalDateTime monthEnd,
            List<UUID> userIds,
            List<UUID> cityIds,
            List<LocationData> districtDataList,
            List<LocationData> wardDataList,
            List<PropertyData> propertyDataList,
            List<UUID> propertyTypeIds,
            Map<UUID, UUID> districtToCityMap,
            Map<UUID, UUID> wardToDistrictMap
    ) {
        int totalLogs = 1000;
        int batchSize = 500;
        int batches = totalLogs / batchSize;

        // Process batches in parallel
        List<List<SearchLog>> allBatches = new ArrayList<>();

        for (int batch = 0; batch < batches; batch++) {
            List<SearchLog> searchLogs = new ArrayList<>(batchSize);

            for (int i = 0; i < batchSize; i++) {
                SearchLog searchLog = generateSearchLog(
                        monthStart, monthEnd,
                        userIds, cityIds, districtDataList, wardDataList,
                        propertyDataList, propertyTypeIds,
                        districtToCityMap, wardToDistrictMap
                );
                searchLogs.add(searchLog);
            }

            allBatches.add(searchLogs);
        }

        // Save all batches (MongoDB can handle bulk inserts efficiently)
        allBatches.parallelStream().forEach(searchLogRepository::saveAll);
    }

    private SearchLog generateSearchLog(
            LocalDateTime monthStart,
            LocalDateTime monthEnd,
            List<UUID> userIds,
            List<UUID> cityIds,
            List<LocationData> districtDataList,
            List<LocationData> wardDataList,
            List<PropertyData> propertyDataList,
            List<UUID> propertyTypeIds,
            Map<UUID, UUID> districtToCityMap,
            Map<UUID, UUID> wardToDistrictMap
    ) {
        SearchLog.SearchLogBuilder builder = SearchLog.builder();

        // 50% chance to have a user (simulating both logged-in and guest searches)
        if (!userIds.isEmpty() && random.nextDouble() < 0.5) {
            builder.userId(userIds.get(random.nextInt(userIds.size())));
        }

        // Determine the search type with weighted probabilities
        double searchTypeRandom = random.nextDouble();

        if (searchTypeRandom < 0.3 && !propertyDataList.isEmpty()) {
            // 30% - Property search (most specific)
            PropertyData property = propertyDataList.get(random.nextInt(propertyDataList.size()));

            builder.propertyId(property.propertyId);

            UUID wardId = property.wardId;
            UUID districtId = wardToDistrictMap.get(wardId);
            UUID cityId = districtToCityMap.get(districtId);

            builder.wardId(wardId)
                   .districtId(districtId)
                   .cityId(cityId);

            // 50% chance to also filter by property type
            if (random.nextDouble() < 0.5 && property.propertyTypeId != null) {
                builder.propertyTypeId(property.propertyTypeId);
            }

        } else if (searchTypeRandom < 0.55 && !wardDataList.isEmpty()) {
            // 25% - Ward search (search by ward, include district and city)
            LocationData ward = wardDataList.get(random.nextInt(wardDataList.size()));

            builder.wardId(ward.id);

            UUID districtId = ward.parentId;
            UUID cityId = districtToCityMap.get(districtId);

            builder.districtId(districtId);
            if (cityId != null) {
                builder.cityId(cityId);
            }

            // 40% chance to also filter by property type
            if (!propertyTypeIds.isEmpty() && random.nextDouble() < 0.4) {
                builder.propertyTypeId(propertyTypeIds.get(random.nextInt(propertyTypeIds.size())));
            }

        } else if (searchTypeRandom < 0.8 && !districtDataList.isEmpty()) {
            // 25% - District search (search by district, include city, but NOT ward)
            LocationData district = districtDataList.get(random.nextInt(districtDataList.size()));

            builder.districtId(district.id);
            builder.cityId(district.parentId);

            // 40% chance to also filter by property type
            if (!propertyTypeIds.isEmpty() && random.nextDouble() < 0.4) {
                builder.propertyTypeId(propertyTypeIds.get(random.nextInt(propertyTypeIds.size())));
            }

        } else if (!cityIds.isEmpty()) {
            // 20% - City search (search by city only, NO district or ward)
            UUID cityId = cityIds.get(random.nextInt(cityIds.size()));
            builder.cityId(cityId);

            // 30% chance to also filter by property type
            if (!propertyTypeIds.isEmpty() && random.nextDouble() < 0.3) {
                builder.propertyTypeId(propertyTypeIds.get(random.nextInt(propertyTypeIds.size())));
            }
        }

        SearchLog searchLog = builder.build();

        // Set random timestamp within the month
        LocalDateTime randomTimestamp = randomDateTimeBetween(monthStart, monthEnd);
        searchLog.setCreatedAt(randomTimestamp);
        searchLog.setUpdatedAt(randomTimestamp);

        return searchLog;
    }

    private LocalDateTime randomDateTimeBetween(LocalDateTime start, LocalDateTime end) {
        long startSeconds = start.toEpochSecond(java.time.ZoneOffset.UTC);
        long endSeconds = end.toEpochSecond(java.time.ZoneOffset.UTC);
        long randomSeconds = ThreadLocalRandom.current().nextLong(startSeconds, endSeconds + 1);
        return LocalDateTime.ofEpochSecond(randomSeconds, 0, java.time.ZoneOffset.UTC);
    }

    private void generatePropertyStatisticsReport(
            int year,
            int month,
            LocalDateTime monthStart,
            LocalDateTime monthEnd,
            List<UUID> cityIds,
            List<LocationData> districtDataList,
            List<LocationData> wardDataList,
            List<UUID> propertyTypeIds
    ) {
        log.info("Generating PropertyStatisticsReport for {}-{}", year, month);

        // Query search logs for this month
        List<SearchLog> monthLogs = searchLogRepository.findAll().stream()
            .filter(log -> log.getCreatedAt() != null
                && !log.getCreatedAt().isBefore(monthStart)
                && !log.getCreatedAt().isAfter(monthEnd))
            .toList();

        // Query all search logs up to this month
        List<SearchLog> allLogsUpToNow = searchLogRepository.findAll().stream()
            .filter(log -> log.getCreatedAt() != null && !log.getCreatedAt().isAfter(monthEnd))
            .toList();

        // Calculate statistics for cities
        Map<UUID, Integer> searchedCitiesMonth = countByField(monthLogs, SearchLog::getCityId);
        Map<UUID, Integer> searchedCities = countByField(allLogsUpToNow, SearchLog::getCityId);
        Map<UUID, Integer> favoriteCities = calculateFavoriteCities();

        // Calculate statistics for districts
        Map<UUID, Integer> searchedDistrictsMonth = countByField(monthLogs, SearchLog::getDistrictId);
        Map<UUID, Integer> searchedDistricts = countByField(allLogsUpToNow, SearchLog::getDistrictId);
        Map<UUID, Integer> favoriteDistricts = calculateFavoriteDistricts();

        // Calculate statistics for wards
        Map<UUID, Integer> searchedWardsMonth = countByField(monthLogs, SearchLog::getWardId);
        Map<UUID, Integer> searchedWards = countByField(allLogsUpToNow, SearchLog::getWardId);
        Map<UUID, Integer> favoriteWards = calculateFavoriteWards();

        // Calculate statistics for property types
        Map<UUID, Integer> searchedPropertyTypesMonth = countByField(monthLogs, SearchLog::getPropertyTypeId);
        Map<UUID, Integer> searchedPropertyTypes = countByField(allLogsUpToNow, SearchLog::getPropertyTypeId);
        Map<UUID, Integer> favoritePropertyTypes = calculateFavoritePropertyTypes();

        // Calculate statistics for properties
        Map<UUID, Integer> searchedPropertiesMonth = countByField(monthLogs, SearchLog::getPropertyId);
        Map<UUID, Integer> searchedProperties = countByField(allLogsUpToNow, SearchLog::getPropertyId);

        // Calculate property statistics
        List<Property> allProperties = propertyRepository.findAll();
        int totalActiveProperties = (int) allProperties.stream()
            .filter(p -> p.getStatus() == Constants.PropertyStatusEnum.AVAILABLE)
            .count();

        int totalSoldPropertiesCurrentMonth = (int) allProperties.stream()
            .filter(p -> p.getStatus() == Constants.PropertyStatusEnum.SOLD
                && p.getUpdatedAt() != null
                && !p.getUpdatedAt().isBefore(monthStart)
                && !p.getUpdatedAt().isAfter(monthEnd))
            .count();

        int totalSoldPropertiesCurrentDay = (int) allProperties.stream()
            .filter(p -> p.getStatus() == Constants.PropertyStatusEnum.SOLD)
            .count();

        int totalRentedPropertiesCurrentMonth = (int) allProperties.stream()
            .filter(p -> p.getStatus() == Constants.PropertyStatusEnum.RENTED
                && p.getUpdatedAt() != null
                && !p.getUpdatedAt().isBefore(monthStart)
                && !p.getUpdatedAt().isAfter(monthEnd))
            .count();

        int totalRentedPropertiesCurrentDay = (int) allProperties.stream()
            .filter(p -> p.getStatus() == Constants.PropertyStatusEnum.RENTED)
            .count();

        // Create base report data
        BaseReportData baseReportData = new BaseReportData();
        baseReportData.setReportType(Constants.ReportTypeEnum.PROPERTY_STATISTICS);
        baseReportData.setMonth(month);
        baseReportData.setYear(year);
        baseReportData.setTitle("Property Statistics Report - " + month + "/" + year);
        baseReportData.setDescription("Monthly property statistics and search analytics");

        // Build and save the report
        PropertyStatisticsReport report = PropertyStatisticsReport.builder()
            .totalActiveProperties(totalActiveProperties)
            .totalSoldProperties(totalSoldPropertiesCurrentDay)
            .totalRentedProperties(totalRentedPropertiesCurrentDay)
            .searchedCities(searchedCities)
            .favoriteCities(favoriteCities)
            .searchedDistricts(searchedDistricts)
            .favoriteDistricts(favoriteDistricts)
            .searchedWards(searchedWards)
            .favoriteWards(favoriteWards)
            .searchedPropertyTypes(searchedPropertyTypes)
            .favoritePropertyTypes(favoritePropertyTypes)
            .searchedProperties(searchedProperties)
            .build();

        report.setBaseReportData(baseReportData);
        report.setCreatedAt(monthEnd);
        report.setUpdatedAt(monthEnd);

        propertyStatisticsReportRepository.save(report);

        log.info("PropertyStatisticsReport created for {}-{}", year, month);
    }

    private <T> Map<UUID, Integer> countByField(List<SearchLog> logs, java.util.function.Function<SearchLog, UUID> fieldExtractor) {
        return logs.stream()
            .map(fieldExtractor)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(
                id -> id,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
    }

    private Map<UUID, Integer> calculateFavoriteCities() {
        List<CustomerFavoriteProperty> favorites = customerFavoritePropertyRepository.findAll();
        Map<UUID, Integer> cityCount = new HashMap<>();

        for (CustomerFavoriteProperty fav : favorites) {
            UUID propertyId = fav.getRefId();
            propertyRepository.findById(propertyId).ifPresent(property -> {
                UUID wardId = property.getWard().getId();
                wardRepository.findById(wardId).ifPresent(ward -> {
                    UUID districtId = ward.getDistrict().getId();
                    districtRepository.findById(districtId).ifPresent(district -> {
                        UUID cityId = district.getCity().getId();
                        cityCount.merge(cityId, 1, Integer::sum);
                    });
                });
            });
        }

        return cityCount;
    }

    private Map<UUID, Integer> calculateFavoriteDistricts() {
        List<CustomerFavoriteProperty> favorites = customerFavoritePropertyRepository.findAll();
        Map<UUID, Integer> districtCount = new HashMap<>();

        for (CustomerFavoriteProperty fav : favorites) {
            UUID propertyId = fav.getRefId();
            propertyRepository.findById(propertyId).ifPresent(property -> {
                UUID wardId = property.getWard().getId();
                wardRepository.findById(wardId).ifPresent(ward -> {
                    UUID districtId = ward.getDistrict().getId();
                    districtCount.merge(districtId, 1, Integer::sum);
                });
            });
        }

        return districtCount;
    }

    private Map<UUID, Integer> calculateFavoriteWards() {
        List<CustomerFavoriteProperty> favorites = customerFavoritePropertyRepository.findAll();
        Map<UUID, Integer> wardCount = new HashMap<>();

        for (CustomerFavoriteProperty fav : favorites) {
            UUID propertyId = fav.getRefId();
            propertyRepository.findById(propertyId).ifPresent(property -> {
                UUID wardId = property.getWard().getId();
                wardCount.merge(wardId, 1, Integer::sum);
            });
        }

        return wardCount;
    }

    private Map<UUID, Integer> calculateFavoritePropertyTypes() {
        List<CustomerFavoriteProperty> favorites = customerFavoritePropertyRepository.findAll();
        Map<UUID, Integer> typeCount = new HashMap<>();

        for (CustomerFavoriteProperty fav : favorites) {
            UUID propertyId = fav.getRefId();
            propertyRepository.findById(propertyId).ifPresent(property -> {
                if (property.getPropertyType() != null) {
                    UUID typeId = property.getPropertyType().getId();
                    typeCount.merge(typeId, 1, Integer::sum);
                }
            });
        }

        return typeCount;
    }

    // Helper classes to avoid lazy loading issues
    private static class LocationData {
        UUID id;
        UUID parentId;

        LocationData(UUID id, UUID parentId) {
            this.id = id;
            this.parentId = parentId;
        }
    }

    private static class PropertyData {
        UUID propertyId;
        UUID wardId;
        UUID propertyTypeId;

        PropertyData(UUID propertyId, UUID wardId, UUID propertyTypeId) {
            this.propertyId = propertyId;
            this.wardId = wardId;
            this.propertyTypeId = propertyTypeId;
        }
    }
}
