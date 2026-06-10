package microservices.moderationservice.moderation.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.event.PropertyCreatedEvent;
import com.se.bds.common.event.PropertyDeletedEvent;
import com.se.bds.common.event.PropertyUpdatedEvent;
import com.se.bds.common.event.UserCreatedEvent;
import com.se.bds.common.event.UserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import microservices.moderationservice.moderation.entity.replica.PropertyReplica;
import microservices.moderationservice.moderation.entity.replica.UserReplica;
import microservices.moderationservice.moderation.repository.replica.PropertyReplicaRepository;
import microservices.moderationservice.moderation.repository.replica.UserReplicaRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReplicaSyncListener {

    private final ObjectMapper objectMapper;
    private final PropertyReplicaRepository propertyReplicaRepository;
    private final UserReplicaRepository userReplicaRepository;

    @KafkaListener(topics = "property-created", groupId = "moderation-replica-group")
    @Transactional
    public void onPropertyCreated(String message) {
        try {
            PropertyCreatedEvent event = objectMapper.readValue(message, PropertyCreatedEvent.class);
            upsertProperty(event);
            log.info("[REPLICA-SYNC] Upserted property replica from created event: propertyId={}, eventId={}",
                    event.propertyId(), event.eventId());
        } catch (Exception e) {
            log.error("[REPLICA-SYNC] Failed to process property-created event", e);
        }
    }

    @KafkaListener(topics = "property-updated", groupId = "moderation-replica-group")
    @Transactional
    public void onPropertyUpdated(String message) {
        try {
            PropertyUpdatedEvent event = objectMapper.readValue(message, PropertyUpdatedEvent.class);
            upsertProperty(event);
            log.info("[REPLICA-SYNC] Upserted property replica from updated event: propertyId={}, eventId={}",
                    event.propertyId(), event.eventId());
        } catch (Exception e) {
            log.error("[REPLICA-SYNC] Failed to process property-updated event", e);
        }
    }

    @KafkaListener(topics = "property-deleted", groupId = "moderation-replica-group")
    @Transactional
    public void onPropertyDeleted(String message) {
        try {
            PropertyDeletedEvent event = objectMapper.readValue(message, PropertyDeletedEvent.class);
            PropertyReplica replica = propertyReplicaRepository.findById(event.propertyId())
                    .orElseGet(() -> PropertyReplica.builder().id(event.propertyId()).build());
            replica.setStatus(event.status());
            replica.setSourceUpdatedAt(event.occurredAt());
            replica.setSyncedAt(LocalDateTime.now());
            propertyReplicaRepository.save(replica);
            log.info("[REPLICA-SYNC] Marked property replica deleted: propertyId={}, eventId={}",
                    event.propertyId(), event.eventId());
        } catch (Exception e) {
            log.error("[REPLICA-SYNC] Failed to process property-deleted event", e);
        }
    }

    @KafkaListener(topics = "user-created", groupId = "moderation-replica-group")
    @Transactional
    public void onUserCreated(String message) {
        try {
            UserCreatedEvent event = objectMapper.readValue(message, UserCreatedEvent.class);
            upsertUser(event);
            log.info("[REPLICA-SYNC] Upserted user replica from created event: userId={}, eventId={}",
                    event.userId(), event.eventId());
        } catch (Exception e) {
            log.error("[REPLICA-SYNC] Failed to process user-created event", e);
        }
    }

    @KafkaListener(topics = "user-updated", groupId = "moderation-replica-group")
    @Transactional
    public void onUserUpdated(String message) {
        try {
            UserUpdatedEvent event = objectMapper.readValue(message, UserUpdatedEvent.class);
            upsertUser(event);
            log.info("[REPLICA-SYNC] Upserted user replica from updated event: userId={}, eventId={}",
                    event.userId(), event.eventId());
        } catch (Exception e) {
            log.error("[REPLICA-SYNC] Failed to process user-updated event", e);
        }
    }

    private void upsertProperty(PropertyCreatedEvent event) {
        PropertyReplica replica = propertyReplicaRepository.findById(event.propertyId())
                .orElseGet(() -> PropertyReplica.builder().id(event.propertyId()).build());
        applyProperty(replica, event.ownerId(), event.assignedAgentId(), event.title(), event.fullAddress(),
                event.thumbnailUrl(), event.priceAmount(), event.status(), event.transactionType(), event.occurredAt());
        propertyReplicaRepository.save(replica);
    }

    private void upsertProperty(PropertyUpdatedEvent event) {
        PropertyReplica replica = propertyReplicaRepository.findById(event.propertyId())
                .orElseGet(() -> PropertyReplica.builder().id(event.propertyId()).build());
        applyProperty(replica, event.ownerId(), event.assignedAgentId(), event.title(), event.fullAddress(),
                event.thumbnailUrl(), event.priceAmount(), event.status(), event.transactionType(), event.occurredAt());
        propertyReplicaRepository.save(replica);
    }

    private void applyProperty(
            PropertyReplica replica,
            java.util.UUID ownerId,
            java.util.UUID assignedAgentId,
            String title,
            String fullAddress,
            String thumbnailUrl,
            java.math.BigDecimal priceAmount,
            String status,
            String transactionType,
            java.time.Instant occurredAt
    ) {
        replica.setOwnerId(ownerId);
        replica.setAssignedAgentId(assignedAgentId);
        replica.setTitle(title);
        replica.setFullAddress(fullAddress);
        replica.setThumbnailUrl(thumbnailUrl);
        replica.setPriceAmount(priceAmount);
        replica.setStatus(status);
        replica.setTransactionType(transactionType);
        replica.setSourceUpdatedAt(occurredAt);
        replica.setSyncedAt(LocalDateTime.now());
    }

    private void upsertUser(UserCreatedEvent event) {
        UserReplica replica = userReplicaRepository.findById(event.userId())
                .orElseGet(() -> UserReplica.builder().id(event.userId()).build());
        applyUser(replica, event.fullName(), event.email(), event.phoneNumber(), event.avatarUrl(),
                event.role(), event.status(), event.active(), event.occurredAt());
        userReplicaRepository.save(replica);
    }

    private void upsertUser(UserUpdatedEvent event) {
        UserReplica replica = userReplicaRepository.findById(event.userId())
                .orElseGet(() -> UserReplica.builder().id(event.userId()).build());
        applyUser(replica, event.fullName(), event.email(), event.phoneNumber(), event.avatarUrl(),
                event.role(), event.status(), event.active(), event.occurredAt());
        userReplicaRepository.save(replica);
    }

    private void applyUser(
            UserReplica replica,
            String fullName,
            String email,
            String phoneNumber,
            String avatarUrl,
            String role,
            String status,
            Boolean active,
            java.time.Instant occurredAt
    ) {
        replica.setFullName(fullName);
        replica.setEmail(email);
        replica.setPhoneNumber(phoneNumber);
        replica.setAvatarUrl(avatarUrl);
        replica.setRole(role);
        replica.setStatus(status);
        replica.setActive(active);
        replica.setSourceUpdatedAt(occurredAt);
        replica.setSyncedAt(LocalDateTime.now());
    }
}
