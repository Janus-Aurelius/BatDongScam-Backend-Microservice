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
public class ViewingCardDto extends AbstractBaseDataResponse {
    private String title;
    private String thumbnailUrl;
    private BigDecimal priceAmount;
    private BigDecimal area;
    private Constants.AppointmentStatusEnum status; // appointment status
    private String districtName;
    private String cityName;
    private LocalDateTime requestedDate;
    private Integer rating;
    private String comment;
    private String agentName;
}
