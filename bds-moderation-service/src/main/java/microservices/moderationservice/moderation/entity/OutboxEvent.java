package microservices.moderationservice.moderation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transactional Outbox - moderation-service.
 *
 * Dùng cho ViolationPenaltyAppliedEvent:
 * Thay vì kafkaTemplate.send() trực tiếp trong updateViolationReport(),
 * lưu vào bảng này trong cùng transaction → OutboxPublisher relay lên Kafka.
 */
@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_mod_outbox_processed_created", columnList = "processed, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "processed", nullable = false)
    @Builder.Default
    private boolean processed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
}
