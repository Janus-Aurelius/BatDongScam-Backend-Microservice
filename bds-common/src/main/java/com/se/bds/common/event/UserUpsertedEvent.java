package com.se.bds.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Fat Event - published khi user được tạo hoặc cập nhật trong IAM service.
 *
 * Published by: bds-iam-service
 * Consumed by:
 *   - bds-moderation-service → sync UserReplica (thay thế iamServiceClient.getUserDetails)
 *   - bds-appointment-service → sync UserReplica nếu cần
 *
 * Topic: user-upserted
 * (Dùng 1 topic cho cả created + updated để consumer logic đơn giản hơn - upsert by id)
 */
public record UserUpsertedEvent(
        UUID userId,
        String username,
        String email,
        String fullName,
        String firstName,
        String lastName,
        String phoneNumber,
        String avatarUrl,
        String role,           // CUSTOMER, SALES_AGENT, PROPERTY_OWNER, ADMIN
        String status,         // ACTIVE, INACTIVE, ...
        boolean active,
        Instant occurredAt
) {
}
