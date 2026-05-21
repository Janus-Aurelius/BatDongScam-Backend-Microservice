package com.se.bds.core.transaction.internal.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks processed webhook events for idempotency (US-011).
 * Prevents duplicate processing when the payment gateway retries webhook delivery.
 *
 * <p>Records are kept for audit purposes with a recommended 7-day cleanup policy.
 */
@Entity
@Table(name = "processed_webhook_event", indexes = {
        @Index(name = "idx_webhook_external_event_id", columnList = "external_event_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** The unique event ID from the payment gateway */
    @Column(name = "external_event_id", nullable = false, unique = true)
    private String externalEventId;

    /** The provider that sent the event */
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    /** The event type (e.g., payment.succeeded) */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /** The internal payment ID that was affected */
    @Column(name = "payment_id")
    private UUID paymentId;

    /** Processing result (SUCCESS, IGNORED, ERROR) */
    @Column(name = "result", length = 20)
    private String result;

    @CreationTimestamp
    @Column(name = "processed_at", updatable = false)
    private LocalDateTime processedAt;
}
