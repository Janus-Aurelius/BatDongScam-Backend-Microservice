package microservices.moderationservice.moderation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import microservices.moderationservice.api.base.AbstractBaseController;
import com.se.bds.common.dto.PagedData;
import com.se.bds.common.enums.*;
import microservices.moderationservice.moderation.dto.request.UpdateViolationRequest;
import microservices.moderationservice.moderation.dto.request.ViolationCreateRequest;
import microservices.moderationservice.moderation.dto.response.ViolationAdminDetails;
import microservices.moderationservice.moderation.dto.response.ViolationAdminItem;
import microservices.moderationservice.moderation.dto.response.ViolationUserDetails;
import microservices.moderationservice.moderation.dto.response.ViolationUserItem;
import microservices.moderationservice.moderation.service.ViolationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/violations")
@Tag(name = "Moderation - Violation", description = "Violation report management API")
public class ViolationController extends AbstractBaseController {
    private final ViolationService violationService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Create a new violation report",
            description = "Create a violation report for a property or user. Evidence files are optional."
    )
    public ResponseEntity<com.se.bds.common.dto.ApiResponse<ViolationUserDetails>> createViolationReport(
            @Parameter(description = "Violation report payload in JSON format", required = true)
            @Valid @RequestPart("payload") ViolationCreateRequest request,
            @Parameter(description = "Optional evidence files")
            @RequestPart(value = "evidenceFiles", required = false) MultipartFile[] evidenceFiles
    ) {
        ViolationUserDetails violationDetails = violationService.createViolationReport(request, evidenceFiles);
        return responseFactory.successSingle(violationDetails, "Violation report created successfully");
    }

    @GetMapping("/admin")
    @Operation(summary = "Get all violation reports (Admin)")
    public ResponseEntity<com.se.bds.common.dto.ApiResponse<PagedData<ViolationAdminItem>>> getAdminViolationItems(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "desc") String sortType,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false) List<ViolationTypeEnum> violationTypes,
            @RequestParam(required = false) List<ViolationStatusEnum> statuses,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        Pageable pageable = createPageable(page, limit, sortType, sortBy);
        Page<ViolationAdminItem> violations = violationService.getAdminViolationItems(
                pageable, violationTypes, statuses, name, month, year
        );
        return responseFactory.successPage(violations, "Violation reports retrieved successfully");
    }

    @GetMapping("/admin/{id}")
    @Operation(summary = "Get violation report details (Admin)")
    public ResponseEntity<com.se.bds.common.dto.ApiResponse<ViolationAdminDetails>> getViolationAdminDetailsById(@PathVariable UUID id) {
        ViolationAdminDetails details = violationService.getViolationAdminDetailsById(id);
        return responseFactory.successSingle(details, "Violation details retrieved successfully");
    }

    @PutMapping("/admin/{id}")
    @Operation(summary = "Update violation report (Admin)")
    public ResponseEntity<com.se.bds.common.dto.ApiResponse<ViolationAdminDetails>> updateViolationReport(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateViolationRequest request
    ) {
        ViolationAdminDetails details = violationService.updateViolationReport(id, request);
        return responseFactory.successSingle(details, "Violation report updated successfully");
    }

    @GetMapping("/my-violations")
    @Operation(summary = "Get my violation reports")
    public ResponseEntity<com.se.bds.common.dto.ApiResponse<PagedData<ViolationUserItem>>> getMyViolationItems(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "desc") String sortType,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        Pageable pageable = createPageable(page, limit, sortType, sortBy);
        Page<ViolationUserItem> violations = violationService.getMyViolationItems(pageable);
        return responseFactory.successPage(violations, "My violation reports retrieved successfully");
    }

    @GetMapping("/my-violations/{id}")
    @Operation(summary = "Get my violation report details")
    public ResponseEntity<com.se.bds.common.dto.ApiResponse<ViolationUserDetails>> getViolationUserDetailsById(@PathVariable UUID id) {
        ViolationUserDetails details = violationService.getViolationUserDetailsById(id);
        return responseFactory.successSingle(details, "Violation details retrieved successfully");
    }
}
