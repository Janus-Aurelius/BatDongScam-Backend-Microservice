package com.se100.bds.controllers;

import com.se100.bds.controllers.base.AbstractBaseController;
import com.se100.bds.dtos.responses.SingleResponse;
import com.se100.bds.dtos.responses.admindashboard.*;
import com.se100.bds.dtos.responses.statisticreport.AgentPerformanceStats;
import com.se100.bds.dtos.responses.statisticreport.CustomerStats;
import com.se100.bds.dtos.responses.statisticreport.FinancialStats;
import com.se100.bds.dtos.responses.statisticreport.PropertyOwnerStats;
import com.se100.bds.dtos.responses.statisticreport.PropertyStats;
import com.se100.bds.dtos.responses.statisticreport.ViolationReportStats;
import com.se100.bds.services.domains.report.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.se100.bds.utils.Constants.SECURITY_SCHEME_NAME;

@RestController
@RequiredArgsConstructor
@RequestMapping("/statistic-report")
@Tag(name = "012. Statistic Report Controller", description = "Those Report shit, you know. Admin only")
@Slf4j
public class StatisticReportController extends AbstractBaseController {
    private final ReportService reportService;

    @GetMapping("/agent-performance/{year}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get agent performance stats for a year", security = @SecurityRequirement(name = SECURITY_SCHEME_NAME))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved agent performance stats"),
        @ApiResponse(responseCode = "404", description = "Year not found")
    })
    public ResponseEntity<SingleResponse<AgentPerformanceStats>> getAgentPerformanceStats(
            @Parameter(description = "Year for the stats", required = true)
            @PathVariable int year) {
        AgentPerformanceStats stats = reportService.getAgentPerformanceStats(year);
        return responseFactory.successSingle(stats, "Agent performance stats retrieved successfully");
    }

    @GetMapping("/customer/{year}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get customer stats for a year",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved customer stats"),
        @ApiResponse(responseCode = "404", description = "Year not found")
    })
    public ResponseEntity<SingleResponse<CustomerStats>> getCustomerStats(
            @Parameter(description = "Year for the stats", required = true)
            @PathVariable int year) {
        CustomerStats stats = reportService.getCustomerStats(year);
        return responseFactory.successSingle(stats, "Customer stats retrieved successfully");
    }

    @GetMapping("/property-owner/{year}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get property owner stats for a year", security = @SecurityRequirement(name = SECURITY_SCHEME_NAME))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved property owner stats"),
        @ApiResponse(responseCode = "404", description = "Year not found")
    })
    public ResponseEntity<SingleResponse<PropertyOwnerStats>> getPropertyOwnerStats(
            @Parameter(description = "Year for the stats", required = true)
            @PathVariable int year) {
        PropertyOwnerStats stats = reportService.getPropertyOwnerStats(year);
        return responseFactory.successSingle(stats, "Property owner stats retrieved successfully");
    }

    @GetMapping("/financial/{year}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get financial stats for a year", security = @SecurityRequirement(name = SECURITY_SCHEME_NAME))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved financial stats"),
        @ApiResponse(responseCode = "404", description = "Year not found")
    })
    public ResponseEntity<SingleResponse<FinancialStats>> getFinancialStats(
            @Parameter(description = "Year for the stats", required = true)
            @PathVariable int year) {
        FinancialStats stats = reportService.getFinancialStats(year);
        return responseFactory.successSingle(stats, "Financial stats retrieved successfully");
    }

    @GetMapping("/property/{year}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get property stats for a year", security = @SecurityRequirement(name = SECURITY_SCHEME_NAME))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved property stats"),
        @ApiResponse(responseCode = "404", description = "Year not found")
    })
    public ResponseEntity<SingleResponse<PropertyStats>> getPropertyStats(
            @Parameter(description = "Year for the stats", required = true)
            @PathVariable int year) {
        PropertyStats stats = reportService.getPropertyStats(year);
        return responseFactory.successSingle(stats, "Property stats retrieved successfully");
    }

    @GetMapping("/violation/{year}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get violation stats for a year", security = @SecurityRequirement(name = SECURITY_SCHEME_NAME))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved violation stats"),
        @ApiResponse(responseCode = "404", description = "Year not found")
    })
    public ResponseEntity<SingleResponse<ViolationReportStats>> getViolationStats(
            @Parameter(description = "Year for the stats", required = true)
            @PathVariable int year) {
        ViolationReportStats stats = reportService.getViolationStats(year);
        return responseFactory.successSingle(stats, "Violation stats retrieved successfully");
    }

    // ===== ADMIN DASHBOARD ENDPOINTS =====

    @GetMapping("/admin-dashboard/top-stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get dashboard top stats - Admin Dashboard", security = @SecurityRequirement(name = SECURITY_SCHEME_NAME))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved top stats"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<SingleResponse<DashboardTopStats>> getDashboardTopStats() {
        DashboardTopStats stats = reportService.getDashboardTopStats();
        return responseFactory.successSingle(stats, "Dashboard top stats retrieved successfully");
    }

    @GetMapping("/admin-dashboard/revenue-contracts/{year}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get dashboard revenue and contracts - Admin Dashboard", security = @SecurityRequirement(name = SECURITY_SCHEME_NAME))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved revenue and contracts"),
        @ApiResponse(responseCode = "404", description = "Year not found")
    })
    public ResponseEntity<SingleResponse<DashboardRevenueAndContracts>> getDashboardRevenueAndContracts(
            @Parameter(description = "Year for the stats", required = true)
            @PathVariable int year) {
        DashboardRevenueAndContracts stats = reportService.getDashboardRevenueAndContracts(year);
        return responseFactory.successSingle(stats, "Dashboard revenue and contracts retrieved successfully");
    }

    @GetMapping("/admin-dashboard/total-properties/{year}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get dashboard total properties - Admin Dashboard", security = @SecurityRequirement(name = SECURITY_SCHEME_NAME))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved total properties"),
        @ApiResponse(responseCode = "404", description = "Year not found")
    })
    public ResponseEntity<SingleResponse<DashboardTotalProperties>> getDashboardTotalProperties(
            @Parameter(description = "Year for the stats", required = true)
            @PathVariable int year) {
        DashboardTotalProperties stats = reportService.getDashboardTotalProperties(year);
        return responseFactory.successSingle(stats, "Dashboard total properties retrieved successfully");
    }

    @GetMapping("/admin-dashboard/property-distribution/{year}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get dashboard property distribution - Admin Dashboard", security = @SecurityRequirement(name = SECURITY_SCHEME_NAME))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved property distribution"),
        @ApiResponse(responseCode = "404", description = "Year not found")
    })
    public ResponseEntity<SingleResponse<DashboardPropertyDistribution>> getDashboardPropertyDistribution(
            @Parameter(description = "Year for the stats", required = true)
            @PathVariable int year) {
        DashboardPropertyDistribution stats = reportService.getDashboardPropertyDistribution(year);
        return responseFactory.successSingle(stats, "Dashboard property distribution retrieved successfully");
    }

    @GetMapping("/admin-dashboard/agent-ranking/{month}/{year}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get dashboard agent ranking (top 5) - Admin Dashboard", security = @SecurityRequirement(name = SECURITY_SCHEME_NAME))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved agent ranking"),
        @ApiResponse(responseCode = "404", description = "Month/Year not found")
    })
    public ResponseEntity<SingleResponse<DashboardAgentRanking>> getDashboardAgentRanking(
            @Parameter(description = "Month for the ranking", required = true)
            @PathVariable int month,
            @Parameter(description = "Year for the ranking", required = true)
            @PathVariable int year) {
        DashboardAgentRanking ranking = reportService.getDashboardAgentRanking(month, year);
        return responseFactory.successSingle(ranking, "Dashboard agent ranking retrieved successfully");
    }

    @GetMapping("/admin-dashboard/customer-ranking/{month}/{year}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get dashboard customer ranking (top 5) - Admin Dashboard", security = @SecurityRequirement(name = SECURITY_SCHEME_NAME))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved customer ranking"),
        @ApiResponse(responseCode = "404", description = "Month/Year not found")
    })
    public ResponseEntity<SingleResponse<DashboardCustomerRanking>> getDashboardCustomerRanking(
            @Parameter(description = "Month for the ranking", required = true)
            @PathVariable int month,
            @Parameter(description = "Year for the ranking", required = true)
            @PathVariable int year) {
        DashboardCustomerRanking ranking = reportService.getDashboardCustomerRanking(month, year);
        return responseFactory.successSingle(ranking, "Dashboard customer ranking retrieved successfully");
    }
}
