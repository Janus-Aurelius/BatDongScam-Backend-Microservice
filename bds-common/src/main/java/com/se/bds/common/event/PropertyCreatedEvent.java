package com.se.bds.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Fat Event - chứa toàn bộ dữ liệu cần thiết để consumer không cần gọi Feign.
 *
 * Published by: bds-core-macroservice (KafkaEventBridge)
 * Consumed by:
 *   - bds-appointment-service  → sync PropertyReplica (không cần CoreServiceClient nữa)
 *   - bds-moderation-service   → sync PropertyReplica
 *
 * Topic: property-created
 */
public record PropertyCreatedEvent(
        UUID propertyId,
        String title,
        String description,
        String fullAddress,

        // Giá & tài chính
        BigDecimal priceAmount,
        BigDecimal pricePerSquareMeter,
        BigDecimal commissionRate,
        BigDecimal serviceFeeAmount,
        BigDecimal area,

        // Đặc điểm
        Integer rooms,
        Integer bathrooms,
        Integer floors,
        Integer bedrooms,
        Integer yearBuilt,

        // Enums (gửi dạng String để tránh coupling với enum nội bộ của core)
        String transactionType,
        String status,
        String houseOrientation,
        String balconyOrientation,

        // Quan hệ
        UUID ownerId,
        UUID assignedAgentId,
        UUID wardId,

        String thumbnailUrl,
        Instant occurredAt
) {
}
