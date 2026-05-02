package com.se100.bds.dtos.responses.appointment;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import com.se100.bds.utils.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ViewingListItem extends AbstractBaseDataResponse {
    private String propertyName;
    private BigDecimal price;
    private BigDecimal area;
    private String thumbnailUrl;
    private LocalDateTime requestedDate;
    private Constants.AppointmentStatusEnum status;
    private String cityName;
    private String districtName;
    private String wardName;
    private String customerName;
    private String customerTier;
    private String salesAgentName;
    private String salesAgentTier;
    private Integer rating;
    private String comment;
}
