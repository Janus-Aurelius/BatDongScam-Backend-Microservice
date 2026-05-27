package com.se100.bds.repositories.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PropertyDetailsProjection(
    UUID id,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,

    // Owner info
    UUID ownerId,
    String ownerFirstName,
    String ownerLastName,
    String ownerPhoneNumber,
    String ownerZaloContact,
    LocalDateTime ownerCreatedAt,
    LocalDateTime ownerUpdatedAt,

    // Agent info
    UUID agentId,
    String agentFirstName,
    String agentLastName,
    String agentPhoneNumber,
    String agentZaloContact,
    LocalDateTime agentCreatedAt,
    LocalDateTime agentUpdatedAt,

    // Property info
    BigDecimal serviceFeeAmount,
    BigDecimal serviceFeeCollectedAmount,
    UUID propertyTypeId,
    String propertyTypeName,
    UUID wardId,
    String wardName,
    UUID districtId,
    String districtName,
    UUID cityId,
    String cityName,
    String title,
    String description,
    String transactionType,
    String fullAddress,
    BigDecimal area,
    Integer rooms,
    Integer bathrooms,
    Integer floors,
    Integer bedrooms,
    String houseOrientation,
    String balconyOrientation,
    Integer yearBuilt,
    BigDecimal priceAmount,
    BigDecimal pricePerSquareMeter,
    BigDecimal commissionRate,
    String amenities,
    String status,
    Integer viewCount,
    LocalDateTime approvedAt
) {}
