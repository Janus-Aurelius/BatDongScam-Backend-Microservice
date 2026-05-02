package com.se100.bds.dtos.responses.appointment;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import com.se100.bds.utils.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ViewingDetailsAdmin extends AbstractBaseDataResponse {
    private LocalDateTime requestedDate;
    private LocalDateTime confirmedDate;
    private Short rating;
    private String comment;
    private String customerRequirements;
    private String agentNotes;
    private Constants.AppointmentStatusEnum status;
    private String viewingOutcome;
    private String customerInterestLevel;
    private PropertyCard propertyCard;
    private UserSimpleCard customer;
    private UserSimpleCard propertyOwner;
    private SalesAgentSimpleCard salesAgent;

    @Getter
    @Setter
    public static class PropertyCard {
        private UUID id;
        private String title;
        private String thumbnailUrl;
        private Constants.TransactionTypeEnum transactionType;
        private String type;
        private String fullAddress;
        private BigDecimal price;
        private BigDecimal area;
    }

    @Getter
    @Setter
    public static class UserSimpleCard {
        private UUID id;
        private String fullName;
        private String tier;
        private String phoneNumber;
        private String email;
    }

    @Getter
    @Setter
    public static class SalesAgentSimpleCard extends UserSimpleCard {
        private Double rating;
        private Integer totalRates;
    }
}
