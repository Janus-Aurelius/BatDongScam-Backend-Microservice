package microservices.moderationservice.moderation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.event.UserUpsertedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import microservices.moderationservice.moderation.entity.UserReplica;
import microservices.moderationservice.moderation.repository.UserReplicaRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Lắng nghe User events từ bds-iam-service và sync vào UserReplica.
 * Đây là cơ chế duy nhất cập nhật bảng user_replicas.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserSyncListener {

    private final UserReplicaRepository userReplicaRepository;
    private final ObjectMapper objectMapper;

    /**
     * Upsert UserReplica khi user được tạo hoặc cập nhật.
     * Topic "user-upserted" được publish từ bds-iam-service.
     */
    @KafkaListener(topics = "user-upserted", groupId = "moderation-service-group")
    @Transactional
    public void onUserUpserted(String message) {
        log.info("[SYNC] Received user-upserted event");
        try {
            UserUpsertedEvent event = objectMapper.readValue(message, UserUpsertedEvent.class);

            UserReplica replica = userReplicaRepository.findById(event.userId())
                    .orElse(UserReplica.builder().userId(event.userId()).build());

            // Resolve fullName từ firstName + lastName nếu fullName null
            String fullName = event.fullName();
            if (fullName == null || fullName.isBlank()) {
                String first = event.firstName() != null ? event.firstName() : "";
                String last = event.lastName() != null ? event.lastName() : "";
                fullName = (first + " " + last).trim();
            }

            replica.setUsername(event.username());
            replica.setEmail(event.email());
            replica.setFullName(fullName);
            replica.setPhoneNumber(event.phoneNumber());
            replica.setAvatarUrl(event.avatarUrl());
            replica.setRole(event.role());
            replica.setStatus(event.status());
            replica.setActive(event.active());
            replica.setLastSyncedAt(LocalDateTime.now());

            userReplicaRepository.save(replica);
            log.info("[SYNC] UserReplica upserted for userId={}", event.userId());
        } catch (Exception e) {
            log.error("[SYNC] Failed to process user-upserted event: {}", message, e);
            throw new RuntimeException("Failed to process user-upserted event", e);
        }
    }
}
