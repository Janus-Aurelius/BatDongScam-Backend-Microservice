package com.se.bds.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Fat Event thay thế ContractStatusChangedEvent nội bộ của core.
 * Nhồi thêm customerId, ownerId để notification-service không cần gọi Feign.
 *
 * Published by: bds-core-macroservice (KafkaEventBridge)
 * Consumed by: bds-notification-service
 *
 * Topic: contract-status-changed
 *
 * LƯU Ý: Event này dùng chung topic "contract-status-changed" với
 * ContractStatusChangedEvent cũ. Sau khi migration xong có thể xóa class cũ.
 */
public record ContractStatusChangedFatEvent(
        UUID contractId,
        UUID propertyId,
        String contractType,
        String oldStatus,
        String newStatus,

        // Dữ liệu enrich sẵn để consumer không cần gọi lại
        UUID customerId,
        UUID ownerId,
        String propertyTitle,

        Instant occurredAt
) {
}
