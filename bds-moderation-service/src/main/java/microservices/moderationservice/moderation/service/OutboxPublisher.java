package microservices.moderationservice.moderation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import microservices.moderationservice.moderation.entity.OutboxEvent;
import microservices.moderationservice.moderation.repository.OutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload()).get();
                successIds.add(event.getId());
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setLastError(e.getMessage() != null
                        ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
                        : "Unknown error");
                outboxEventRepository.save(event);
                log.warn("[Outbox] Failed to relay event id={} topic={}, retryCount={}",
                        event.getId(), event.getTopic(), event.getRetryCount());

                if (event.getRetryCount() >= 5) {
                    log.error("[Outbox] Event id={} has failed 5 times — manual intervention required.", event.getId());
                }
            }
        }

        if (!successIds.isEmpty()) {
            outboxEventRepository.markAsProcessed(successIds);
            log.info("[Outbox] Relayed {}/{} events", successIds.size(), pendingEvents.size());
        }
    }
}
