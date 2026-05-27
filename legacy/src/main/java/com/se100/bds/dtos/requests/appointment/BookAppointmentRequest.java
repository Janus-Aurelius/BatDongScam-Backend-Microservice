package com.se100.bds.dtos.requests.appointment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to book a viewing appointment for a property")
public class BookAppointmentRequest {

    @NotNull(message = "Property ID is required")
    @Schema(description = "The property ID to book a viewing for")
    private UUID propertyId;

    @NotNull(message = "Requested date is required")
    @Future(message = "Requested date must be in the future")
    @Schema(description = "The requested date and time for the viewing", example = "2024-12-25T10:00:00")
    private LocalDateTime requestedDate;

    @Schema(description = "Customer requirements or special requests for the viewing", example = "Need wheelchair access")
    private String customerRequirements;

    @Schema(description = "Customer ID (optional - for admin creating appointment on behalf of customer, if not provided uses current user)")
    private UUID customerId;

    @Schema(description = "Sales agent ID (optional - for manual assignment by admin/agent)")
    private UUID agentId;
}
