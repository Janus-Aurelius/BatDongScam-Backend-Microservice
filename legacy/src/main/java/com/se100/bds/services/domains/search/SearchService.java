package com.se100.bds.services.domains.search;

import com.se100.bds.utils.Constants;

import java.util.List;
import java.util.UUID;

public interface SearchService {
    void addSearch(UUID userId, UUID cityId, UUID districtId, UUID wardId, UUID propertyId, UUID propertyTypeId);
    void addSearchList(UUID userId, List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds, List<UUID> propertyTypeIds);
    List<UUID> topMostSearchByUser(UUID userId, int offset, int limit, Constants.SearchTypeEnum searchType, int year, int month);
    List<UUID> getMostSearchedPropertyIds(int limit, int year, int month);
}