package com.se100.bds.dtos.responses.appointment;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import com.se100.bds.dtos.responses.user.simple.PropertyOwnerSimpleCard;
import com.se100.bds.dtos.responses.user.simple.SalesAgentSimpleCard;
import com.se100.bds.utils.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ViewingDetailsCustomer extends AbstractBaseDataResponse {
    private String title;
    private Integer images;
    private List<String> imagesList;
    private BigDecimal priceAmount;
    private BigDecimal area;
    private Constants.AppointmentStatusEnum status; // appointment status
    private String fullAddress;
    private LocalDateTime requestedDate;
    private LocalDateTime confirmedDate;
    private Short rating;
    private String comment;
    private String customerRequirements;

    private String description;
    private Integer rooms;
    private Integer bathRooms;
    private Integer bedRooms;
    private Integer floors;
    private Constants.OrientationEnum houseOrientation;
    private Constants.OrientationEnum balconyOrientation;

    private PropertyOwnerSimpleCard propertyOwner;
    private SalesAgentSimpleCard salesAgent;

    private List<String> attachedDocuments;
    private String notes;
}
