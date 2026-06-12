package com.se361.financial_service.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transactional Outbox pattern.
 *
 * Thay vì gọi kafkaTemplate.send() trực tiếp trong @Transactional,
 * ta lưu event vào bảng này TRONG CÙNG transaction với dữ liệu nghiệp vụ.
 *
 * OutboxPublisher sẽ quét bảng này định kỳ và relay lên Kafka.
 * Đảm bảo: nếu DB commit → event chắc chắn được gửi.
 *          nếu DB rollback → event không tồn tại → không gửi sai.
 */
@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_processed_created", columnList = "processed, created_at")
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

    /**
     * Kafka topic đích.
     * VD: "payment-succeeded", "violation-penalty-applied"
     */
    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    /**
     * Kafka partition key (thường là aggregate ID).
     */
    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    /**
     * JSON payload của event.
     */
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    /**
     * false = chưa xử lý, true = đã relay lên Kafka thành công.
     */
    @Column(name = "processed", nullable = false)
    @Builder.Default
    private boolean processed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Số lần thử gửi thất bại liên tiếp.
     * Dùng để skip event lỗi liên tục (tránh poison pill).
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
}
