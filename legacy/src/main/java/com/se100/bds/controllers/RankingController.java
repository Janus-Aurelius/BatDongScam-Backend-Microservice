package com.se100.bds.controllers;

import com.se100.bds.controllers.base.AbstractBaseController;
import com.se100.bds.dtos.responses.SingleResponse;
import com.se100.bds.models.schemas.ranking.*;
import com.se100.bds.services.domains.ranking.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.se100.bds.utils.Constants.SECURITY_SCHEME_NAME;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ranking")
@Tag(name = "011. Ranking", description = "Ranking API")
@Slf4j
public class RankingController extends AbstractBaseController {
    private final RankingService rankingService;

    // Sales Agent endpoints
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/agent/{agentId}/month")
    @Operation(
            summary = "Get sales agent performance for a specific month",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<SingleResponse<IndividualSalesAgentPerformanceMonth>> getSaleAgentMonth(
            @Parameter(description = "Agent ID", required = true)
            @PathVariable UUID agentId,
            @Parameter(description = "Month (1-12)", required = true)
            @RequestParam int month,
            @Parameter(description = "Year", required = true)
            @RequestParam int year) {
        IndividualSalesAgentPerformanceMonth performance = rankingService.getSaleAgentMonth(agentId, month, year);
        return responseFactory.successSingle(performance, "Sales agent monthly performance retrieved successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/agent/{agentId}/career")
    @Operation(
            summary = "Get sales agent career performance",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<SingleResponse<IndividualSalesAgentPerformanceCareer>> getSaleAgentCareer(
            @Parameter(description = "Agent ID", required = true)
            @PathVariable UUID agentId) {
        IndividualSalesAgentPerformanceCareer career = rankingService.getSaleAgentCareer(agentId);
        return responseFactory.successSingle(career, "Sales agent career performance retrieved successfully");
    }

    @PreAuthorize("hasRole('SALESAGENT')")
    @GetMapping("/agent/me/month")
    @Operation(
            summary = "Get my sales agent performance for a specific month",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<SingleResponse<IndividualSalesAgentPerformanceMonth>> getMySaleAgentMonth(
            @Parameter(description = "Month (1-12)", required = true)
            @RequestParam int month,
            @Parameter(description = "Year", required = true)
            @RequestParam int year) {
        IndividualSalesAgentPerformanceMonth performance = rankingService.getMySaleAgentMonth(month, year);
        return responseFactory.successSingle(performance, "My sales agent monthly performance retrieved successfully");
    }

    @PreAuthorize("hasRole('SALESAGENT')")
    @GetMapping("/agent/me/career")
    @Operation(
            summary = "Get my sales agent career performance",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<SingleResponse<IndividualSalesAgentPerformanceCareer>> getMySaleAgentCareer() {
        IndividualSalesAgentPerformanceCareer career = rankingService.getMySaleAgentCareer();
        return responseFactory.successSingle(career, "My sales agent career performance retrieved successfully");
    }

    // Customer endpoints
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/customer/{customerId}/month")
    @Operation(
            summary = "Get customer potential for a specific month",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<SingleResponse<IndividualCustomerPotentialMonth>> getCustomerMonth(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable UUID customerId,
            @Parameter(description = "Month (1-12)", required = true)
            @RequestParam int month,
            @Parameter(description = "Year", required = true)
            @RequestParam int year) {
        IndividualCustomerPotentialMonth potential = rankingService.getCustomerMonth(customerId, month, year);
        return responseFactory.successSingle(potential, "Customer monthly potential retrieved successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/customer/{customerId}/all")
    @Operation(
            summary = "Get customer potential for all time",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<SingleResponse<IndividualCustomerPotentialAll>> getCustomerAll(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable UUID customerId) {
        IndividualCustomerPotentialAll potential = rankingService.getCustomerAll(customerId);
        return responseFactory.successSingle(potential, "Customer all-time potential retrieved successfully");
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/customer/me/month")
    @Operation(
            summary = "Get my customer potential for a specific month",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<SingleResponse<IndividualCustomerPotentialMonth>> getMyCustomerMonth(
            @Parameter(description = "Month (1-12)", required = true)
            @RequestParam int month,
            @Parameter(description = "Year", required = true)
            @RequestParam int year) {
        IndividualCustomerPotentialMonth potential = rankingService.getMyCustomerMonth(month, year);
        return responseFactory.successSingle(potential, "My customer monthly potential retrieved successfully");
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/customer/me/all")
    @Operation(
            summary = "Get my customer potential for all time",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<SingleResponse<IndividualCustomerPotentialAll>> getMyCustomerAll() {
        IndividualCustomerPotentialAll potential = rankingService.getMyCustomerAll();
        return responseFactory.successSingle(potential, "My customer all-time potential retrieved successfully");
    }

    // Property Owner endpoints
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/property-owner/{propertyOwnerId}/month")
    @Operation(
            summary = "Get property owner contribution for a specific month",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<SingleResponse<IndividualPropertyOwnerContributionMonth>> getPropertyOwnerMonth(
            @Parameter(description = "Property Owner ID", required = true)
            @PathVariable UUID propertyOwnerId,
            @Parameter(description = "Month (1-12)", required = true)
            @RequestParam int month,
            @Parameter(description = "Year", required = true)
            @RequestParam int year) {
        IndividualPropertyOwnerContributionMonth contribution = rankingService.getPropertyOwnerMonth(propertyOwnerId, month, year);
        return responseFactory.successSingle(contribution, "Property owner monthly contribution retrieved successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/property-owner/{propertyOwnerId}/all")
    @Operation(
            summary = "Get property owner contribution for all time",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<SingleResponse<IndividualPropertyOwnerContributionAll>> getPropertyOwnerAll(
            @Parameter(description = "Property Owner ID", required = true)
            @PathVariable UUID propertyOwnerId) {
        IndividualPropertyOwnerContributionAll contribution = rankingService.getPropertyOwnerAll(propertyOwnerId);
        return responseFactory.successSingle(contribution, "Property owner all-time contribution retrieved successfully");
    }

    @PreAuthorize("hasRole('PROPERTY_OWNER')")
    @GetMapping("/property-owner/me/month")
    @Operation(
            summary = "Get my property owner contribution for a specific month",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<SingleResponse<IndividualPropertyOwnerContributionMonth>> getMyPropertyOwnerMonth(
            @Parameter(description = "Month (1-12)", required = true)
            @RequestParam int month,
            @Parameter(description = "Year", required = true)
            @RequestParam int year) {
        IndividualPropertyOwnerContributionMonth contribution = rankingService.getMyPropertyOwnerMonth(month, year);
        return responseFactory.successSingle(contribution, "My property owner monthly contribution retrieved successfully");
    }

    @PreAuthorize("hasRole('PROPERTY_OWNER')")
    @GetMapping("/property-owner/me/all")
    @Operation(
            summary = "Get my property owner contribution for all time",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<SingleResponse<IndividualPropertyOwnerContributionAll>> getMyPropertyOwnerAll() {
        IndividualPropertyOwnerContributionAll contribution = rankingService.getMyPropertyOwnerAll();
        return responseFactory.successSingle(contribution, "My property owner all-time contribution retrieved successfully");
    }
}

