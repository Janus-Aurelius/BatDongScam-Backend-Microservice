package com.se100.bds.dtos.responses.property;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import com.se100.bds.dtos.responses.document.DocumentResponse;
import com.se100.bds.dtos.responses.user.simple.SimpleUserResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class PropertyDetails extends AbstractBaseDataResponse {
    private SimpleUserResponse owner;
    private SimpleUserResponse assignedAgent;
    private BigDecimal serviceFeeAmount;
    private BigDecimal serviceFeeCollectedAmount;
    private UUID propertyTypeId;
    private String propertyTypeName;
    private UUID wardId;
    private String wardName;
    private UUID districtId;
    private String districtName;
    private UUID cityId;
    private String cityName;
    private String title;
    private String description;
    private String transactionType;
    private String fullAddress;
    private BigDecimal area;
    private Integer rooms;
    private Integer bathrooms;
    private Integer floors;
    private Integer bedrooms;
    private String houseOrientation;
    private String balconyOrientation;
    private Integer yearBuilt;
    private BigDecimal priceAmount;
    private BigDecimal pricePerSquareMeter;
    private BigDecimal commissionRate;
    private String amenities;
    private String status;
    private Integer viewCount;
    private LocalDateTime approvedAt;
    private List<MediaResponse> mediaList;
    private List<DocumentResponse> documentList;
}
