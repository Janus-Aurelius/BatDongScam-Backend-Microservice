package microservices.appointmentservice.controllers;

import com.se.bds.common.dto.ApiResponse;
import com.se.bds.common.dto.PagedData;
import microservices.appointmentservice.dtos.requests.appointment.BookAppointmentRequest;
import microservices.appointmentservice.dtos.requests.appointment.CancelAppointmentRequest;
import microservices.appointmentservice.dtos.requests.appointment.RateAppointmentRequest;
import microservices.appointmentservice.dtos.responses.appointment.BookAppointmentResponse;
import microservices.appointmentservice.dtos.responses.appointment.ViewingCardDto;
import microservices.appointmentservice.dtos.responses.appointment.ViewingDetailsCustomer;
import microservices.appointmentservice.dtos.responses.appointment.ViewingDetailsAdmin;
import microservices.appointmentservice.dtos.responses.appointment.ViewingListItem;
import microservices.appointmentservice.services.appointment.AppointmentService;
import microservices.appointmentservice.utils.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static microservices.appointmentservice.utils.Constants.SECURITY_SCHEME_NAME;

@RestController
@RequiredArgsConstructor
@RequestMapping("/appointment")
@Tag(name = "008. Appointment/Viewing", description = "Appointment API")
@Slf4j
public class AppointmentController {

    private final AppointmentService appointmentService;

    public static Pageable createPageable(int page, int limit, String sortType, String sortBy) {
        int offset = (page - 1) * limit;
        int pageNumber = offset / limit;

        if (sortBy == null) {
            return PageRequest.of(pageNumber, limit);
        }

        Sort.Direction direction = (sortType != null && sortType.equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = !sortBy.isEmpty() ? sortBy : "id";
        Sort sort = Sort.by(direction, sortField);

        return PageRequest.of(pageNumber, limit, sort);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Create a viewing appointment",
            description = "Creates a new appointment request for a customer to view a property. The appointment will be in PENDING status until an agent confirms it. Admin/Agent can create appointments on behalf of customers by providing customerId, and can optionally assign an agent.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<ApiResponse<BookAppointmentResponse>> createAppointment(
            @Valid @RequestBody BookAppointmentRequest request
    ) {
        BookAppointmentResponse response = appointmentService.bookAppointment(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SALESAGENT', 'ADMIN')")
    @Operation(
            summary = "Cancel an appointment",
            description = "Changes appointment status to CANCELLED. Customer can cancel their own appointments, agents can cancel assigned appointments, admin can cancel any.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<ApiResponse<Boolean>> cancelAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID appointmentId,

            @Parameter(description = "Cancellation details")
            @Valid @RequestBody(required = false) CancelAppointmentRequest request
    ) {
        String reason = request != null ? request.getReason() : null;
        boolean result = appointmentService.cancelAppointment(appointmentId, reason);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PatchMapping("/{appointmentId}/complete")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Mark appointment as completed",
            description = "Allows customers (their own appointments), sale agents, or admins to mark an appointment as completed once the viewing happened.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<ApiResponse<Boolean>> completeAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID appointmentId
    ) {
        boolean result = appointmentService.completeAppointment(appointmentId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/viewing-cards")
    @Operation(
            summary = "Get my viewing cards",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<ApiResponse<PagedData<ViewingCardDto>>> getMyViewingCards(
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
        PagedData<ViewingCardDto> pagedData = PagedData.<ViewingCardDto>builder()
                .content(viewingCardDtos.getContent())
                .pageNumber(viewingCardDtos.getNumber())
                .pageSize(viewingCardDtos.getSize())
                .totalElements(viewingCardDtos.getTotalElements())
                .totalPages(viewingCardDtos.getTotalPages())
                .build();
        return ResponseEntity.ok(ApiResponse.success(pagedData));
    }

    @GetMapping("/viewing-details/{id}")
    @Operation(
            summary = "Customer Get viewing details by appointment ID",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<ApiResponse<ViewingDetailsCustomer>> getViewingDetails(
            @Parameter(description = "Appointment ID")
            @PathVariable UUID id) {
        ViewingDetailsCustomer viewingDetailsCustomer = appointmentService.getViewingDetails(id);
        return ResponseEntity.ok(ApiResponse.success(viewingDetailsCustomer));
    }

    @GetMapping("/admin/viewing-list")
    @Operation(
            summary = "Admin Get viewing list with filters",
            description = "Get paginated list of appointments with comprehensive filtering options",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<ApiResponse<PagedData<ViewingListItem>>> getViewingListItems(
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
        PagedData<ViewingListItem> pagedData = PagedData.<ViewingListItem>builder()
                .content(viewingListItems.getContent())
                .pageNumber(viewingListItems.getNumber())
                .pageSize(viewingListItems.getSize())
                .totalElements(viewingListItems.getTotalElements())
                .totalPages(viewingListItems.getTotalPages())
                .build();
        return ResponseEntity.ok(ApiResponse.success(pagedData));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('SALESAGENT')")
    @GetMapping("/admin-agent/viewing-details/{id}")
    @Operation(
            summary = "Admin or Agent Get viewing details by appointment ID",
            description = "Get detailed appointment information including property, customer, owner, and agent details",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<ApiResponse<ViewingDetailsAdmin>> getViewingDetailsAdmin(
            @Parameter(description = "Appointment ID")
            @PathVariable UUID id) {
        ViewingDetailsAdmin viewingDetails = appointmentService.getViewingDetailsAdmin(id);
        return ResponseEntity.ok(ApiResponse.success(viewingDetails));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PatchMapping("/{appointmentId}/rate")
    @Operation(
            summary = "Rate an appointment",
            description = "Rate a completed appointment with a rating (1-5) and optional comment",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<ApiResponse<Boolean>> rateAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID appointmentId,
            @Valid @RequestBody RateAppointmentRequest request) {
        boolean result = appointmentService.rateAppointment(appointmentId, request.getRating(), request.getComment());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PreAuthorize("hasAnyRole('SALESAGENT', 'ADMIN')")
    @PatchMapping("/{appointmentId}")
    @Operation(
            summary = "Agent Update appointment details",
            description = "Update appointment details such as agent notes, viewing outcome, customer interest level, status, and cancellation reason. Only non-null fields will be updated.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    public ResponseEntity<ApiResponse<Boolean>> updateAppointmentDetails(
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
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}