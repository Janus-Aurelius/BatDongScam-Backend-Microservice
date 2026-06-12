package microservices.moderationservice.moderation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Local Replica của User - chỉ đọc từ góc nhìn của Moderation.
 * Được cập nhật tự động qua Kafka listener (UserSyncListener).
 *
 * KHÔNG bao giờ update entity này trực tiếp từ business logic của Moderation.
 * Source of truth vẫn là bds-iam-service.
 */
@Entity
@Table(name = "user_replicas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReplica {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "avatar_url")
    private String avatarUrl;

    /**
     * Role string: CUSTOMER, SALES_AGENT, PROPERTY_OWNER, ADMIN
     * Dùng để enrichReportedInfo khi reported type là USER.
     */
    @Column(name = "role", length = 50)
    private String role;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
}
