package com.se361.iam_service.client;

import com.se.bds.common.dto.ApiResponse;
import com.se.bds.common.enums.RoleEnum;
import com.se361.iam_service.dto.response.ranking.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "appointment-service", path = "/api/rankings")
public interface RankingClient {

    @GetMapping("/current-tier")
    ApiResponse<String> getCurrentTier(
            @RequestParam("userId") UUID userId,
            @RequestParam("role") RoleEnum role
    );

    @GetMapping("/agent/{agentId}/month")
    ApiResponse<IndividualSalesAgentPerformanceMonth> getSaleAgentMonth(
            @PathVariable("agentId") UUID agentId,
            @RequestParam("month") int month,
            @RequestParam("year") int year
    );

    @GetMapping("/agent/{agentId}/career")
    ApiResponse<IndividualSalesAgentPerformanceCareer> getSaleAgentCareer(
            @PathVariable("agentId") UUID agentId
    );

    @GetMapping("/customer/{customerId}/month")
    ApiResponse<IndividualCustomerPotentialMonth> getCustomerMonth(
            @PathVariable("customerId") UUID customerId,
            @RequestParam("month") int month,
            @RequestParam("year") int year
    );

    @GetMapping("/customer/{customerId}/all")
    ApiResponse<IndividualCustomerPotentialAll> getCustomerAll(
            @PathVariable("customerId") UUID customerId
    );

    @GetMapping("/property-owner/{propertyOwnerId}/month")
    ApiResponse<IndividualPropertyOwnerContributionMonth> getPropertyOwnerMonth(
            @PathVariable("propertyOwnerId") UUID propertyOwnerId,
            @RequestParam("month") int month,
            @RequestParam("year") int year
    );

    @GetMapping("/property-owner/{propertyOwnerId}/all")
    ApiResponse<IndividualPropertyOwnerContributionAll> getPropertyOwnerAll(
            @PathVariable("propertyOwnerId") UUID propertyOwnerId
    );
}
