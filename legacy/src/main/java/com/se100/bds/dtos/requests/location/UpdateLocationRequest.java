package com.se100.bds.dtos.requests.location;

import com.se100.bds.utils.Constants;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLocationRequest {
    private UUID id;
    private Constants.LocationEnum locationTypeEnum;
    private UUID parentId;
    private String name;
    private String description;
    private MultipartFile image;
    private BigDecimal totalArea;
    private BigDecimal avg_land_price;
    private Integer population;
    private Boolean isActive;
}
