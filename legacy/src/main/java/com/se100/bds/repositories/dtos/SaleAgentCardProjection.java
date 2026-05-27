package com.se100.bds.repositories.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record SaleAgentCardProjection(
    UUID userId,
    String firstName,
    String lastName,
    String avatarUrl,
    String email,
    String phoneNumber,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String employeeCode,
    Integer maxProperties,
    LocalDateTime hiredDate,
    UUID wardId,
    String wardName,
    UUID districtId,
    String districtName,
    UUID cityId,
    String cityName
) {}

