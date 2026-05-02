package com.se100.bds.repositories.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record MediaProjection(
    UUID id,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String mediaType,
    String fileName,
    String filePath,
    String mimeType
) {}
