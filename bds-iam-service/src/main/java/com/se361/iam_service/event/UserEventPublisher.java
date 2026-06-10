package com.se361.iam_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.event.UserCreatedEvent;
import com.se.bds.common.event.UserUpdatedEvent;
import com.se361.iam_service.entity.User;
import com.se361.iam_service.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishUserCreatedAfterCommit(User user) {
        UserCreatedEvent event = new UserCreatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getStatus() == Constants.StatusProfileEnum.ACTIVE
        );
        publishAfterCommit("user-created", user.getId().toString(), event);
    }

    public void publishUserUpdatedAfterCommit(User user) {
        UserUpdatedEvent event = new UserUpdatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getStatus() == Constants.StatusProfileEnum.ACTIVE
        );
        publishAfterCommit("user-updated", user.getId().toString(), event);
    }

    private void publishAfterCommit(String topic, String key, Object event) {
        Runnable publisher = () -> publish(topic, key, event);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publisher.run();
                }
            });
        } else {
            publisher.run();
        }
    }

    private void publish(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, payload);
            log.info("[USER-EVENT] Published event to topic={}, key={}, payload={}", topic, key, payload);
        } catch (Exception e) {
            log.error("[USER-EVENT] Failed to publish event to topic={}, key={}", topic, key, e);
        }
    }
}
