package com.se100.bds.controllers;

import com.se100.bds.controllers.base.AbstractBaseController;
import com.se100.bds.dtos.responses.PageResponse;
import com.se100.bds.dtos.responses.SingleResponse;
import com.se100.bds.dtos.responses.appointment.ViewingListItem;
import com.se100.bds.dtos.responses.property.SimplePropertyCard;
import com.se100.bds.dtos.responses.user.listitem.FreeAgentListItem;
import com.se100.bds.services.domains.appointment.AppointmentService;
import com.se100.bds.services.domains.property.PropertyService;
import com.se100.bds.services.domains.user.UserService;
import com.se100.bds.utils.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.se100.bds.utils.Constants.SECURITY_SCHEME_NAME;

@RestController
@RequiredArgsConstructor
@RequestMapping("assignments")
@Tag(name = "010. Assignments", description = "Assignments API")
@Slf4j
public class AssignmentController extends AbstractBaseController {

    private final UserService userService;
    private final PropertyService propertyService;
    private final AppointmentService appointmentService;

    @GetMapping("/admin/free-agents")
    @Operation(
            summary = "Get free agents list with filters",
            description = "Get paginated list of available agents with filtering by assignments, properties, and tiers",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<PageResponse<FreeAgentListItem>> getFreeAgentListItems(
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "desc") String sortType,
            @Parameter(description = "Field to sort by")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "Agent name or employee code to filter by")
            @RequestParam(required = false) String agentNameOrCode,
            @Parameter(description = "Agent performance tiers to filter by")
            @RequestParam(required = false) List<Constants.PerformanceTierEnum> agentTiers,
            @Parameter(description = "Minimum assigned appointments")
            @RequestParam(required = false) Integer minAssignedAppointments,
            @Parameter(description = "Maximum assigned appointments")
            @RequestParam(required = false) Integer maxAssignedAppointments,
            @Parameter(description = "Minimum assigned properties")
            @RequestParam(required = false) Integer minAssignedProperties,
            @Parameter(description = "Maximum assigned properties")
            @RequestParam(required = false) Integer maxAssignedProperties,
            @Parameter(description = "Minimum currently handling (appointments + properties)")
            @RequestParam(required = false) Integer minCurrentlyHandle,
            @Parameter(description = "Maximum currently handling (appointments + properties)")
            @RequestParam(required = false) Integer maxCurrentlyHandle) {

        Pageable pageable = createPageable(page, limit, sortType, sortBy);

        Page<FreeAgentListItem> freeAgents = userService.getAllFreeAgentItemsWithFilters(
                pageable,
                agentNameOrCode,
                agentTiers,
                minAssignedAppointments, maxAssignedAppointments,
                minAssignedProperties, maxAssignedProperties,
                minCurrentlyHandle, maxCurrentlyHandle
        );

        return responseFactory.successPage(freeAgents, "Free agents list retrieved successfully");
    }

    //! TODO: deprecate this endpoint in favor of more specific ones below
    @PostMapping("/admin/assign")
    @Operation(
            summary = "Assign or remove agent from property or appointment",
            description = "Assign a sales agent to a property or appointment. If agentId is null, removes current agent. Target type must be 'PROPERTY' or 'APPOINTMENT'",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SingleResponse<Boolean>> assignAgent(
            @Parameter(description = "Agent ID (null to remove current agent)")
            @RequestParam(required = false) UUID agentId,
            @Parameter(description = "Target ID (Property or Appointment ID)", required = true)
            @RequestParam UUID targetId,
            @Parameter(description = "Target type: 'PROPERTY' or 'APPOINTMENT'", required = true)
            @RequestParam String targetType) {

        String message;

        if ("PROPERTY".equalsIgnoreCase(targetType)) {
            // TODO: refactor propertyService.assignAgent to the same contract as appointmentService
            propertyService.assignAgent(agentId, targetId);
            if (agentId == null) {
                message = "Agent removed from property successfully";
            } else {
                message = "Agent assigned to property successfully";
            }
        } else if ("APPOINTMENT".equalsIgnoreCase(targetType)) {
            if (agentId == null) {
                appointmentService.removeAgent(targetId);
                message = "Agent removed from appointment successfully";
            } else {
                appointmentService.assignAgent(agentId, targetId);
                message = "Agent assigned to appointment successfully";
            }
        } else {
            throw new IllegalArgumentException("Invalid target type. Must be 'PROPERTY' or 'APPOINTMENT'");
        }

        return responseFactory.successSingle(null, message);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/viewings/{appointmentId}/agent")
    @Operation(
            summary = "Admin assign or remove agent on a viewing",
            description = "Convenience endpoint to swap or remove an agent from a specific viewing appointment. Pass agentId to assign or omit it to remove the current agent.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<SingleResponse<Boolean>> assignAgentToViewing(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID appointmentId,
            @Parameter(description = "Agent ID (null to remove)")
            @RequestParam(required = false) UUID agentId) {

        if (agentId == null) {
            appointmentService.removeAgent(appointmentId);
        } else {
            appointmentService.assignAgent(agentId, appointmentId);
        }

        String message = agentId == null
                ? "Agent removed from viewing successfully"
                : "Agent assigned to viewing successfully";

        // you can map the resulting entity and return it if needed. I sure do not give a single damn fuck
        return responseFactory.successSingle(null, message);
    }

    @PreAuthorize("hasRole('SALESAGENT')")
    @GetMapping("/my-viewing-list")
    @Operation(
            summary = "Agent Get their own viewing list with filters",
            description = "Get paginated list of appointments assigned to the current agent with filtering options",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<PageResponse<ViewingListItem>> getMyViewingListItems(
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "desc") String sortType,
            @Parameter(description = "Field to sort by")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "Customer name to filter by")
            @RequestParam(required = false) String customerName,
            @Parameter(description = "Day to filter by")
            @RequestParam(required = false) Integer day,
            @Parameter(description = "Month to filter by")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Year to filter by")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Appointment status enums to filter by")
            @RequestParam(required = false) List<Constants.AppointmentStatusEnum> statusEnums) {
        Pageable pageable = createPageable(page, limit, sortType, sortBy);
        Page<ViewingListItem> viewingListItems = appointmentService.getMyViewingListItems(
                pageable,
                customerName,
                day, month, year,
                statusEnums
        );
        return responseFactory.successPage(viewingListItems, "My viewing list retrieved successfully");
    }

    @PreAuthorize("hasRole('SALESAGENT')")
    @GetMapping("/my-assigned-properties")
    @Operation(
            summary = "Agent Get their own assigned properties",
            description = "Get paginated list of properties assigned to the current agent with optional property owner name filter",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<PageResponse<SimplePropertyCard>> getMyAssignedProperties(
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "desc") String sortType,
            @Parameter(description = "Field to sort by")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "Property owner name to filter by")
            @RequestParam(required = false) String propertyOwnerName) {
        Pageable pageable = createPageable(page, limit, sortType, sortBy);
        Page<SimplePropertyCard> properties = propertyService.myAssignedProperties(
                pageable,
                propertyOwnerName
        );
        return responseFactory.successPage(properties, "My assigned properties retrieved successfully");
    }
}
