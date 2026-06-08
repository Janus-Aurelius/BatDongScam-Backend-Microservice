package microservices.moderationservice.moderation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Local Replica của Property - chỉ đọc từ góc nhìn của Moderation.
 * Được cập nhật tự động qua Kafka listener (PropertySyncListener).
 *
 * KHÔNG bao giờ update entity này trực tiếp từ business logic của Moderation.
 * Source of truth vẫn là bds-core-macroservice.
 */
@Entity
@Table(name = "property_replicas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyReplica {

    /**
     * ID chính là propertyId từ core-macroservice - không auto-generate.
     */
    @Id
    @Column(name = "property_id", nullable = false, updatable = false)
    private UUID propertyId;

    @Column(name = "title")
    private String title;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "full_address")
    private String fullAddress;

    @Column(name = "price_amount", precision = 19, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "transaction_type", length = 50)
    private String transactionType;

    /**
     * Để JOIN với ViolationReport.relatedEntityId khi entity type là PROPERTY,
     * và để tìm owner khi cần publish penalty event (không cần gọi Feign nữa).
     */
    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "assigned_agent_id")
    private UUID assignedAgentId;

    /**
     * Soft delete - đánh dấu thay vì xóa để tránh mất join data.
     */
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "last_synced_at")
    private java.time.LocalDateTime lastSyncedAt;
}
