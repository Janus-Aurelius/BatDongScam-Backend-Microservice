package microservices.appointmentservice.repositories.dtos;

import microservices.appointmentservice.utils.Constants;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PropertyCardProtection(
    UUID id,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Constants.TransactionTypeEnum transactionType,
    String title,
    String thumbnailUrl,
    boolean isFavorite,
    int numberOfImages,
    String address,
    String district,
    String city,
    String status,
    BigDecimal price,
    BigDecimal totalArea,
    UUID ownerId,
    String ownerFirstName,
    String ownerLastName,
    UUID agentId,
    String agentFirstName,
    String agentLastName
) {}
