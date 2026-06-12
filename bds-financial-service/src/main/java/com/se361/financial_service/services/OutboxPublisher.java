package com.se361.financial_service.services;

import com.se361.financial_service.entities.OutboxEvent;
import com.se361.financial_service.repositories.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Outbox Relay - quét bảng outbox_events và đẩy lên Kafka.
 *
 * Luồng hoạt động:
 * 1. Đọc batch các event chưa processed (max 50 / lần).
 * 2. Gửi từng event lên Kafka, chờ ACK đồng bộ.
 * 3. Nếu ACK thành công → mark processed = true.
 * 4. Nếu lỗi → tăng retryCount, ghi lastError.
 *    Sau 5 lần thất bại → event bị skip (cần alert thủ công).
 *
 * fixedDelay = 500ms: sau khi lần trước kết thúc, chờ 500ms rồi chạy tiếp.
 * Dùng fixedDelay thay vì fixedRate để tránh overlap khi batch lớn.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents();
        if (pendingEvents.isEmpty()) return;

        log.debug("[Outbox] Processing {} pending events", pendingEvents.size());

        List<UUID> successIds = new ArrayList<>();

        for (OutboxEvent event : pendingEvents) {
            try {
                // Gửi đồng bộ, chờ broker ACK
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload()).get();
                successIds.add(event.getId());
                log.debug("[Outbox] Relayed event id={} topic={}", event.getId(), event.getTopic());
            } catch (Exception e) {
                // Tăng retryCount, ghi lỗi — KHÔNG throw để các event khác vẫn được xử lý
                event.setRetryCount(event.getRetryCount() + 1);
                event.setLastError(e.getMessage() != null
                        ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
                        : "Unknown error");
                outboxEventRepository.save(event);
                log.warn("[Outbox] Failed to relay event id={} topic={}, retryCount={}: {}",
                        event.getId(), event.getTopic(), event.getRetryCount(), e.getMessage());

                if (event.getRetryCount() >= 5) {
                    log.error("[Outbox] Event id={} topic={} has failed {} times — skipping. Manual intervention required.",
                            event.getId(), event.getTopic(), event.getRetryCount());
                }
            }
        }

        // Bulk update processed = true cho các event thành công
        if (!successIds.isEmpty()) {
            outboxEventRepository.markAsProcessed(successIds);
            log.info("[Outbox] Successfully relayed {}/{} events", successIds.size(), pendingEvents.size());
        }
    }
}
