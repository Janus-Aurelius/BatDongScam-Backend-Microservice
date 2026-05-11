package com.se.bds.core.shared.dto.property;

import com.se.bds.common.dto.AbstractBaseDataResponse;
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
    private String transactionType; // Changed to String or your shared Enum
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
