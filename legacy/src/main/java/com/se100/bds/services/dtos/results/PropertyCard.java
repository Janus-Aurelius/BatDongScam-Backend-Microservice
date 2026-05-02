package com.se100.bds.services.dtos.results;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PropertyCard {
    private UUID id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String transactionType;
    private String title;
    private String thumbnailUrl;
    private boolean isFavorite;
    private int numberOfImages;
    private String district;
    private String city;
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
