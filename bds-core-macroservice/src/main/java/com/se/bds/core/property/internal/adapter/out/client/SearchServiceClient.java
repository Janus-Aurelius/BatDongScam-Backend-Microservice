package com.se.bds.core.property.internal.adapter.out.client;

import com.se.bds.common.dto.ApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "search-service", contextId = "searchServiceClient")
public interface SearchServiceClient {

    @GetMapping("/api/search/most-searched-properties")
    @CircuitBreaker(name = "searchServiceCircuitBreaker", fallbackMethod = "fallbackMostSearched")
    ApiResponse<List<UUID>> getMostSearchedPropertyIds(
            @RequestParam("limit") int limit,
            @RequestParam("year") int year,
            @RequestParam("month") int month
    );

    default ApiResponse<List<UUID>> fallbackMostSearched(int limit, int year, int month, Throwable throwable) {
        // Fallback: return empty list on failure
        return new ApiResponse<>(true, "Fallback: search-service is currently unavailable", List.of());
    }

    @GetMapping("/api/search/top")
    @CircuitBreaker(name = "searchServiceCircuitBreaker", fallbackMethod = "fallbackTopMostSearchByUser")
    ApiResponse<List<UUID>> topMostSearchByUser(
            @RequestParam("userId") UUID userId,
            @RequestParam("offset") int offset,
            @RequestParam("limit") int limit,
            @RequestParam("searchType") String searchType,
            @RequestParam("year") int year,
            @RequestParam("month") int month
    );

    default ApiResponse<List<UUID>> fallbackTopMostSearchByUser(UUID userId, int offset, int limit, String searchType, int year, int month, Throwable throwable) {
        return new ApiResponse<>(true, "Fallback: search-service is currently unavailable", List.of());
    }
}
