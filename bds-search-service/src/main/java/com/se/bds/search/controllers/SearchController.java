package com.se.bds.search.controllers;

import com.se.bds.common.dto.ApiResponse;
import com.se.bds.search.dtos.requests.AddSearchListRequest;
import com.se.bds.search.dtos.requests.AddSearchRequest;
import com.se.bds.search.services.SearchService;
import com.se.bds.search.utils.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Search logging and analytics API")
@Slf4j
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/log")
    @Operation(summary = "Add a search log entry")
    public ResponseEntity<ApiResponse<Void>> addSearch(@RequestBody AddSearchRequest request) {
        log.info("Adding search log for user: {}", request.getUserId());
        searchService.addSearch(
                request.getUserId(),
                request.getCityId(),
                request.getDistrictId(),
                request.getWardId(),
                request.getPropertyId(),
                request.getPropertyTypeId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(null)
        );
    }

    @PostMapping("/log/batch")
    @Operation(summary = "Add search log entries in batch (async)")
    public ResponseEntity<ApiResponse<Void>> addSearchList(@RequestBody AddSearchListRequest request) {
        log.info("Adding batch search log for user: {}", request.getUserId());
        searchService.addSearchList(
                request.getUserId(),
                request.getCityIds(),
                request.getDistrictIds(),
                request.getWardIds(),
                request.getPropertyTypeIds()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                ApiResponse.success(null)
        );
    }

    @GetMapping("/top")
    @Operation(summary = "Get top most searched items by user")
    public ResponseEntity<ApiResponse<List<UUID>>> topMostSearchByUser(
            @Parameter(description = "User ID") @RequestParam UUID userId,
            @Parameter(description = "Offset") @RequestParam(defaultValue = "0") int offset,
            @Parameter(description = "Limit") @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Search type: CITY, DISTRICT, WARD, PROPERTY, PROPERTY_TYPE")
            @RequestParam Constants.SearchTypeEnum searchType,
            @Parameter(description = "Year") @RequestParam int year,
            @Parameter(description = "Month") @RequestParam int month
    ) {
        List<UUID> result = searchService.topMostSearchByUser(userId, offset, limit, searchType, year, month);
        return ResponseEntity.ok(
                ApiResponse.success(result)
        );
    }

    @GetMapping("/most-searched-properties")
    @Operation(summary = "Get most searched property IDs")
    public ResponseEntity<ApiResponse<List<UUID>>> getMostSearchedPropertyIds(
            @Parameter(description = "Limit") @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Year") @RequestParam int year,
            @Parameter(description = "Month") @RequestParam int month
    ) {
        List<UUID> result = searchService.getMostSearchedPropertyIds(limit, year, month);
        return ResponseEntity.ok(
                ApiResponse.success(result)
        );
    }
}
