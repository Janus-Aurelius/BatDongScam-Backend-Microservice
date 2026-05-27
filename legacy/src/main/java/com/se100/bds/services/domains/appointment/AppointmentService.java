package com.se100.bds.services.domains.appointment;

import com.se100.bds.dtos.requests.appointment.BookAppointmentRequest;
import com.se100.bds.dtos.responses.appointment.BookAppointmentResponse;
import com.se100.bds.dtos.responses.appointment.ViewingCardDto;
import com.se100.bds.dtos.responses.appointment.ViewingDetailsCustomer;
import com.se100.bds.dtos.responses.appointment.ViewingDetailsAdmin;
import com.se100.bds.dtos.responses.appointment.ViewingListItem;
import com.se100.bds.utils.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentService {
    // BOOKING
    BookAppointmentResponse bookAppointment(BookAppointmentRequest request);

    // CANCEL
    boolean cancelAppointment(UUID appointmentId, String reason);
    // USER
    Page<ViewingCardDto> myViewingCards(
            Pageable pageable,
            Constants.AppointmentStatusEnum statusEnum,
            Integer day, Integer month, Integer year
    );
    ViewingDetailsCustomer getViewingDetails(UUID id);

    // ADMIN
    Page<ViewingListItem> getViewingListItems(
            Pageable pageable,
            String propertyName, List<UUID> propertyTypeIds,
            List<Constants.TransactionTypeEnum> transactionTypeEnums,
            String agentName, List<Constants.PerformanceTierEnum> agentTiers,
            String customerName, List<Constants.CustomerTierEnum> customerTiers,
            LocalDateTime requestDateFrom, LocalDateTime requestDateTo,
            Short minRating, Short maxRating,
            List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds,
            List<Constants.AppointmentStatusEnum> statusEnums
    );
    ViewingDetailsAdmin getViewingDetailsAdmin(UUID id);

    // AGENT
    Page<ViewingListItem> getMyViewingListItems(
            Pageable pageable,
            String customerName,
            Integer day, Integer month, Integer year,
            List<Constants.AppointmentStatusEnum> statusEnums
    );

    // Helper methods
    int countByAgentId(UUID agentId);

    void removeAgent(UUID appointmentId);

    // Assignment
    void assignAgent(UUID agentId, UUID appointmentId);

    // Update appointment details
    boolean updateAppointmentDetails(
            UUID appointmentId,
            String agentNotes,
            String viewingOutcome,
            String customerInterestLevel,
            Constants.AppointmentStatusEnum status,
            String cancelledReason
    );

    // Rate appointment
    boolean rateAppointment(UUID appointmentId, Short rating, String comment);

    // Complete appointment
    boolean completeAppointment(UUID appointmentId);
}
