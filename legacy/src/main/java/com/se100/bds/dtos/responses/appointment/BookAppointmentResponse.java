package com.se100.bds.dtos.responses.appointment;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Response for a newly booked appointment")
public class BookAppointmentResponse {

    @Schema(description = "The appointment ID")
    private UUID appointmentId;

    @Schema(description = "Property ID")
    private UUID propertyId;

    @Schema(description = "Property title")
    private String propertyTitle;

    @Schema(description = "Property address")
    private String propertyAddress;

    @Schema(description = "The requested viewing date and time")
    private LocalDateTime requestedDate;

    @Schema(description = "Appointment status")
    private String status;

    @Schema(description = "Customer requirements")
    private String customerRequirements;

    @Schema(description = "Assigned agent ID (if auto-assigned)")
    private UUID agentId;

    @Schema(description = "Assigned agent name (if auto-assigned)")
    private String agentName;

    @Schema(description = "When the appointment was created")
    private LocalDateTime createdAt;

    @Schema(description = "Message for the customer")
    private String message;
}
