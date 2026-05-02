package com.se100.bds.dtos.responses.property;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import com.se100.bds.utils.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class SimplePropertyCard extends AbstractBaseDataResponse {
    private String title;
    private String thumbnailUrl;
    private Constants.TransactionTypeEnum transactionType;
    private boolean isFavorite;
    private int numberOfImages;
    private String location;
    private String status;
    private BigDecimal price;
    private BigDecimal totalArea;
    private UUID ownerId;
    private String ownerFirstName;
    private String ownerLastName;
    private String ownerTier;
    private UUID agentId;
    private String agentFirstName;
    private String agentLastName;
    private String agentTier;
}
