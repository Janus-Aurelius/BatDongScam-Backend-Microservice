package com.se100.bds.controllers;

import com.se100.bds.controllers.base.AbstractBaseController;
import com.se100.bds.dtos.requests.violation.UpdateViolationRequest;
import com.se100.bds.dtos.requests.violation.ViolationCreateRequest;
import com.se100.bds.dtos.responses.PageResponse;
import com.se100.bds.dtos.responses.SingleResponse;
import com.se100.bds.dtos.responses.violation.*;
import com.se100.bds.services.domains.violation.ViolationService;
import com.se100.bds.utils.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static com.se100.bds.utils.Constants.SECURITY_SCHEME_NAME;

@RestController
@RequiredArgsConstructor
@RequestMapping("/violations")
@Tag(name = "014. Violation Controller", description = "Violation report management API")
@Slf4j
public class ViolationController extends AbstractBaseController {

    private final ViolationService violationService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'PROPERTY_OWNER', 'SALESAGENT')")
    @Operation(
            summary = "Create a new violation report",
            description = "Create a violation report for a property or user. Evidence files (images/documents) are optional.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Violation report created successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Reported entity (property or user) not found"),
            @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<SingleResponse<ViolationUserDetails>> createViolationReport(
            @Parameter(description = "Violation report payload in JSON format", required = true)
            @Valid @RequestPart("payload") ViolationCreateRequest request,
            @Parameter(description = "Optional evidence files (images or documents)")
            @RequestPart(value = "evidenceFiles", required = false) MultipartFile[] evidenceFiles
    ) {
        ViolationUserDetails violationDetails = violationService.createViolationReport(request, evidenceFiles);
        return responseFactory.successSingle(violationDetails, "Violation report created successfully");
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get all violation reports (Admin)",
            description = "Retrieve paginated list of violation reports with filters. Admin only.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Violation reports retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<PageResponse<ViolationAdminItem>> getAdminViolationItems(
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int limit,

            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "desc") String sortType,

            @Parameter(description = "Field to sort by")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Filter by violation types")
            @RequestParam(required = false) List<Constants.ViolationTypeEnum> violationTypes,

            @Parameter(description = "Filter by violation statuses")
            @RequestParam(required = false) List<Constants.ViolationStatusEnum> statuses,

            @Parameter(description = "Search by reporter or reported name")
            @RequestParam(required = false) String name,

            @Parameter(description = "Filter by month (1-12)")
            @RequestParam(required = false) Integer month,

            @Parameter(description = "Filter by year")
            @RequestParam(required = false) Integer year
    ) {
        Pageable pageable = createPageable(page, limit, sortType, sortBy);
        Page<ViolationAdminItem> violations = violationService.getAdminViolationItems(
                pageable, violationTypes, statuses, name, month, year
        );
        return responseFactory.successPage(violations, "Violation reports retrieved successfully");
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get violation report details (Admin)",
            description = "Retrieve detailed information of a specific violation report. Admin only.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Violation details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "404", description = "Violation not found")
    })
    public ResponseEntity<SingleResponse<ViolationAdminDetails>> getViolationAdminDetailsById(
            @Parameter(description = "Violation report ID", required = true)
            @PathVariable UUID id
    ) {
        ViolationAdminDetails details = violationService.getViolationAdminDetailsById(id);
        return responseFactory.successSingle(details, "Violation details retrieved successfully");
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update violation report (Admin)",
            description = "Update violation report status, resolution notes, and penalty. Admin only. Cannot update reporter, reported entity, or violation type.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Violation report updated successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "404", description = "Violation not found"),
            @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<SingleResponse<ViolationAdminDetails>> updateViolationReport(
            @Parameter(description = "Violation report ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Update violation request payload", required = true)
            @Valid @RequestBody UpdateViolationRequest request
    ) {
        ViolationAdminDetails details = violationService.updateViolationReport(id, request);
        return responseFactory.successSingle(details, "Violation report updated successfully");
    }

    @GetMapping("/my-violations")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'PROPERTY_OWNER', 'SALESAGENT')")
    @Operation(
            summary = "Get my violation reports",
            description = "Retrieve paginated list of violation reports created by the current user",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "My violation reports retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PageResponse<ViolationUserItem>> getMyViolationItems(
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int limit,

            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "desc") String sortType,

            @Parameter(description = "Field to sort by")
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        Pageable pageable = createPageable(page, limit, sortType, sortBy);
        Page<ViolationUserItem> violations = violationService.getMyViolationItems(pageable);
        return responseFactory.successPage(violations, "My violation reports retrieved successfully");
    }

    @GetMapping("/my-violations/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'PROPERTY_OWNER', 'SALESAGENT')")
    @Operation(
            summary = "Get my violation report details",
            description = "Retrieve detailed information of a specific violation report created by the current user",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Violation details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to view this violation"),
            @ApiResponse(responseCode = "404", description = "Violation not found")
    })
    public ResponseEntity<SingleResponse<ViolationUserDetails>> getViolationUserDetailsById(
            @Parameter(description = "Violation report ID", required = true)
            @PathVariable UUID id
    ) {
        ViolationUserDetails details = violationService.getViolationUserDetailsById(id);
        return responseFactory.successSingle(details, "Violation details retrieved successfully");
    }
}

