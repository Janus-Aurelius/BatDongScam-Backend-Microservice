package microservices.moderationservice.moderation.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import microservices.moderationservice.moderation.entity.PropertyReplica;
import microservices.moderationservice.moderation.entity.UserReplica;
import microservices.moderationservice.moderation.repository.PropertyReplicaRepository;
import microservices.moderationservice.moderation.repository.UserReplicaRepository;
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
            log.info("[REPLICA-SYNC] Upserted property replica from created event: propertyId={}, occurredAt={}",
                    event.propertyId(), event.occurredAt());
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
            log.info("[REPLICA-SYNC] Upserted property replica from updated event: propertyId={}, occurredAt={}",
                    event.propertyId(), event.occurredAt());
        } catch (Exception e) {
            log.error("[REPLICA-SYNC] Failed to process property-updated event", e);
        }
    }

    @KafkaListener(topics = "property-deleted", groupId = "moderation-replica-group")
    @Transactional
    public void onPropertyDeleted(String message) {
        try {
            PropertyDeletedEvent event = objectMapper.readValue(message, PropertyDeletedEvent.class);
            propertyReplicaRepository.findById(event.propertyId()).ifPresent(replica -> {
                replica.setStatus(event.status());
                replica.setDeleted(true);
                replica.setLastSyncedAt(LocalDateTime.now());
                propertyReplicaRepository.save(replica);
                log.info("[REPLICA-SYNC] Marked property replica deleted: propertyId={}, eventId={}",
                        event.propertyId(), event.eventId());
            });
        } catch (Exception e) {
            log.error("[REPLICA-SYNC] Failed to process property-deleted event", e);
        }
    }

    @KafkaListener(topics = "user-upserted", groupId = "moderation-replica-group")
    @Transactional
    public void onUserUpserted(String message) {
        try {
            UserUpsertedEvent event = objectMapper.readValue(message, UserUpsertedEvent.class);
            upsertUser(event);
            log.info("[REPLICA-SYNC] Upserted user replica from upserted event: userId={}, occurredAt={}",
                    event.userId(), event.occurredAt());
        } catch (Exception e) {
            log.error("[REPLICA-SYNC] Failed to process user-upserted event", e);
        }
    }

    private void upsertProperty(PropertyCreatedEvent event) {
        PropertyReplica replica = propertyReplicaRepository.findById(event.propertyId())
                .orElse(PropertyReplica.builder().propertyId(event.propertyId()).build());
        
        replica.setOwnerId(event.ownerId());
        replica.setAssignedAgentId(event.assignedAgentId());
        replica.setTitle(event.title());
        replica.setFullAddress(event.fullAddress());
        replica.setThumbnailUrl(event.thumbnailUrl());
        replica.setPriceAmount(event.priceAmount());
        replica.setStatus(event.status());
        replica.setTransactionType(event.transactionType());
        replica.setLastSyncedAt(LocalDateTime.now());
        replica.setDeleted(false);
        
        propertyReplicaRepository.save(replica);
    }

    private void upsertProperty(PropertyUpdatedEvent event) {
        PropertyReplica replica = propertyReplicaRepository.findById(event.propertyId())
                .orElse(PropertyReplica.builder().propertyId(event.propertyId()).build());
        
        replica.setOwnerId(event.ownerId());
        replica.setAssignedAgentId(event.assignedAgentId());
        replica.setTitle(event.title());
        replica.setFullAddress(event.fullAddress());
        replica.setThumbnailUrl(event.thumbnailUrl());
        replica.setPriceAmount(event.priceAmount());
        replica.setStatus(event.status());
        replica.setTransactionType(event.transactionType());
        replica.setLastSyncedAt(LocalDateTime.now());
        replica.setDeleted(false);
        
        propertyReplicaRepository.save(replica);
    }

    private void upsertUser(UserUpsertedEvent event) {
        UserReplica replica = userReplicaRepository.findById(event.userId())
                .orElse(UserReplica.builder().userId(event.userId()).build());
        
        replica.setFullName(event.fullName());
        replica.setUsername(event.username());
        replica.setEmail(event.email());
        replica.setPhoneNumber(event.phoneNumber());
        replica.setAvatarUrl(event.avatarUrl());
        replica.setRole(event.role());
        replica.setStatus(event.status());
        replica.setActive(event.active());
        replica.setLastSyncedAt(LocalDateTime.now());
        
        userReplicaRepository.save(replica);
    }
}