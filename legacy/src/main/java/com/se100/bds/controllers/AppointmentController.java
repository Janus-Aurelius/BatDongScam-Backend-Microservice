package com.se100.bds.controllers;

import com.se100.bds.controllers.base.AbstractBaseController;
import com.se100.bds.dtos.requests.appointment.BookAppointmentRequest;
import com.se100.bds.dtos.requests.appointment.CancelAppointmentRequest;
import com.se100.bds.dtos.requests.appointment.RateAppointmentRequest;
import com.se100.bds.dtos.responses.PageResponse;
import com.se100.bds.dtos.responses.SingleResponse;
import com.se100.bds.dtos.responses.appointment.BookAppointmentResponse;
import com.se100.bds.dtos.responses.appointment.ViewingCardDto;
import com.se100.bds.dtos.responses.appointment.ViewingDetailsCustomer;
import com.se100.bds.dtos.responses.appointment.ViewingDetailsAdmin;
import com.se100.bds.dtos.responses.appointment.ViewingListItem;
import com.se100.bds.services.domains.appointment.AppointmentService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.se100.bds.utils.Constants.SECURITY_SCHEME_NAME;

@RestController
@RequiredArgsConstructor
@RequestMapping("/appointment")
@Tag(name = "008. Appointment/Viewing", description = "Appointment API")
@Slf4j
public class AppointmentController extends AbstractBaseController {
    private final AppointmentService appointmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Create a viewing appointment",
            description = "Creates a new appointment request for a customer to view a property. The appointment will be in PENDING status until an agent confirms it. Admin/Agent can create appointments on behalf of customers by providing customerId, and can optionally assign an agent.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Appointment created successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request or property not available"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Property not found")
    })
    public ResponseEntity<SingleResponse<BookAppointmentResponse>> createAppointment(
            @Valid @RequestBody BookAppointmentRequest request
    ) {
        BookAppointmentResponse response = appointmentService.bookAppointment(request);
        return responseFactory.successSingle(response, "Appointment created successfully");
    }


    @PatchMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SALESAGENT', 'ADMIN')")
    @Operation(
            summary = "Cancel an appointment",
            description = "Changes appointment status to CANCELLED. Customer can cancel their own appointments, agents can cancel assigned appointments, admin can cancel any.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Appointment cancelled successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Appointment already cancelled or completed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not authorized to cancel this appointment"),
            @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    public ResponseEntity<SingleResponse<Boolean>> cancelAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID appointmentId,

            @Parameter(description = "Cancellation details")
            @Valid @RequestBody(required = false) CancelAppointmentRequest request
    ) {
        String reason = request != null ? request.getReason() : null;
        boolean result = appointmentService.cancelAppointment(appointmentId, reason);
        return responseFactory.successSingle(result, "Appointment cancelled successfully");
    }

    @PatchMapping("/{appointmentId}/complete")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Mark appointment as completed",
            description = "Allows customers (their own appointments), sale agents, or admins to mark an appointment as completed once the viewing happened.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<SingleResponse<Boolean>> completeAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID appointmentId
    ) {
        boolean result = appointmentService.completeAppointment(appointmentId);
        String message = result ? "Appointment marked as completed" : "Appointment was already completed";
        return responseFactory.successSingle(result, message);
    }

    @GetMapping("/viewing-cards")
    @Operation(
            summary = "Get my viewing cards",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<PageResponse<ViewingCardDto>> getMyViewingCards(
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "desc") String sortType,
            @Parameter(description = "Field to sort by")
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Constants.AppointmentStatusEnum statusEnum,
            @RequestParam(required = false) Integer day,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        Pageable pageable = createPageable(page, limit, sortType, sortBy);
        Page<ViewingCardDto> viewingCardDtos = appointmentService.myViewingCards(pageable, statusEnum, day, month, year);
        return responseFactory.successPage(viewingCardDtos, "My viewings retrieved successfully");
    }

    @GetMapping("/viewing-details/{id}")
    @Operation(
            summary = "Customer Get viewing details by appointment ID",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<SingleResponse<ViewingDetailsCustomer>> getViewingDetails(
            @Parameter(description = "Appointment ID")
            @PathVariable UUID id) {
        ViewingDetailsCustomer viewingDetailsCustomer = appointmentService.getViewingDetails(id);
        return responseFactory.successSingle(viewingDetailsCustomer, "Viewing details retrieved successfully");
    }

    @GetMapping("/admin/viewing-list")
    @Operation(
            summary = "Admin Get viewing list with filters",
            description = "Get paginated list of appointments with comprehensive filtering options",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<PageResponse<ViewingListItem>> getViewingListItems(
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "desc") String sortType,
            @Parameter(description = "Field to sort by")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "Property name to filter by")
            @RequestParam(required = false) String propertyName,
            @Parameter(description = "Property type IDs to filter by")
            @RequestParam(required = false) List<UUID> propertyTypeIds,
            @Parameter(description = "Transaction types to filter by")
            @RequestParam(required = false) List<Constants.TransactionTypeEnum> transactionTypeEnums,
            @Parameter(description = "Agent name to filter by")
            @RequestParam(required = false) String agentName,
            @Parameter(description = "Agent performance tiers to filter by")
            @RequestParam(required = false) List<Constants.PerformanceTierEnum> agentTiers,
            @Parameter(description = "Customer name to filter by")
            @RequestParam(required = false) String customerName,
            @Parameter(description = "Customer tiers to filter by")
            @RequestParam(required = false) List<Constants.CustomerTierEnum> customerTiers,
            @Parameter(description = "Request date from (ISO format)")
            @RequestParam(required = false) LocalDateTime requestDateFrom,
            @Parameter(description = "Request date to (ISO format)")
            @RequestParam(required = false) LocalDateTime requestDateTo,
            @Parameter(description = "Minimum rating")
            @RequestParam(required = false) Short minRating,
            @Parameter(description = "Maximum rating")
            @RequestParam(required = false) Short maxRating,
            @Parameter(description = "City IDs to filter by")
            @RequestParam(required = false) List<UUID> cityIds,
            @Parameter(description = "District IDs to filter by")
            @RequestParam(required = false) List<UUID> districtIds,
            @Parameter(description = "Ward IDs to filter by")
            @RequestParam(required = false) List<UUID> wardIds,
            @Parameter(description = "Appointment status enums to filter by")
            @RequestParam(required = false) List<Constants.AppointmentStatusEnum> statusEnums) {
        Pageable pageable = createPageable(page, limit, sortType, sortBy);
        Page<ViewingListItem> viewingListItems = appointmentService.getViewingListItems(
                pageable,
                propertyName, propertyTypeIds,
                transactionTypeEnums,
                agentName, agentTiers,
                customerName, customerTiers,
                requestDateFrom, requestDateTo,
                minRating, maxRating,
                cityIds, districtIds, wardIds,
                statusEnums
        );
        return responseFactory.successPage(viewingListItems, "Viewing list retrieved successfully");
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('SALESAGENT')")
    @GetMapping("/admin-agent/viewing-details/{id}")
    @Operation(
            summary = "Admin or Agent Get viewing details by appointment ID",
            description = "Get detailed appointment information including property, customer, owner, and agent details",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<SingleResponse<ViewingDetailsAdmin>> getViewingDetailsAdmin(
            @Parameter(description = "Appointment ID")
            @PathVariable UUID id) {
        ViewingDetailsAdmin viewingDetails = appointmentService.getViewingDetailsAdmin(id);
        return responseFactory.successSingle(viewingDetails, "Viewing details retrieved successfully");
    }


    @PreAuthorize("hasRole('CUSTOMER')")
    @PatchMapping("/{appointmentId}/rate")
    @Operation(
            summary = "Rate an appointment",
            description = "Rate a completed appointment with a rating (1-5) and optional comment",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<SingleResponse<Boolean>> rateAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID appointmentId,
            @Valid @RequestBody RateAppointmentRequest request) {

        boolean result = appointmentService.rateAppointment(appointmentId, request.getRating(), request.getComment());

        String message = result
                ? "Appointment rated successfully"
                : "No changes were made to the appointment";

        return responseFactory.successSingle(result, message);
    }

    @PreAuthorize("hasAnyRole('SALESAGENT', 'ADMIN')")
    @PatchMapping("/{appointmentId}")
    @Operation(
            summary = "Agent Update appointment details",
            description = "Update appointment details such as agent notes, viewing outcome, customer interest level, status, and cancellation reason. Only non-null fields will be updated.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<SingleResponse<Boolean>> updateAppointmentDetails(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID appointmentId,
            @Parameter(description = "Agent notes")
            @RequestParam(required = false) String agentNotes,
            @Parameter(description = "Viewing outcome")
            @RequestParam(required = false) String viewingOutcome,
            @Parameter(description = "Customer interest level (e.g., LOW, MEDIUM, HIGH, VERY_HIGH)")
            @RequestParam(required = false) String customerInterestLevel,
            @Parameter(description = "Appointment status")
            @RequestParam(required = false) Constants.AppointmentStatusEnum status,
            @Parameter(description = "Cancellation reason (used when status is CANCELLED)")
            @RequestParam(required = false) String cancelledReason) {

        boolean result = appointmentService.updateAppointmentDetails(
                appointmentId,
                agentNotes,
                viewingOutcome,
                customerInterestLevel,
                status,
                cancelledReason
        );

        String message = result
                ? "Appointment details updated successfully"
                : "No changes were made to the appointment";

        return responseFactory.successSingle(result, message);
    }
}
