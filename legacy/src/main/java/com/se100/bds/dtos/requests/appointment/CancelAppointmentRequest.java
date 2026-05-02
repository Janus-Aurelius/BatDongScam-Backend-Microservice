package com.se100.bds.dtos.requests.appointment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to cancel an appointment")
public class CancelAppointmentRequest {

    @Schema(description = "Reason for cancellation", example = "Schedule conflict")
    private String reason;
}
