package microservices.moderationservice.moderation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.event.PropertyCreatedEvent;
import com.se.bds.common.event.PropertyStatusChangedEvent;
import com.se.bds.common.event.PropertyUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import microservices.moderationservice.moderation.entity.PropertyReplica;
import microservices.moderationservice.moderation.repository.PropertyReplicaRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lắng nghe các Property event từ core-macroservice và sync vào PropertyReplica.
 * Đây là cơ chế duy nhất cập nhật bảng property_replicas.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PropertySyncListener {

    private final PropertyReplicaRepository propertyReplicaRepository;
    private final ObjectMapper objectMapper;

    /**
     * Upsert replica khi property được tạo.
     * Nhận Fat Event - KHÔNG gọi CoreServiceClient.
     */
    @KafkaListener(topics = "property-created", groupId = "moderation-service-group")
    @Transactional
    public void onPropertyCreated(String message) {
        log.info("[SYNC] Received property-created event");
        try {
            PropertyCreatedEvent event = objectMapper.readValue(message, PropertyCreatedEvent.class);
            upsertReplica(event.propertyId(), event);
            log.info("[SYNC] PropertyReplica upserted for propertyId={}", event.propertyId());
        } catch (Exception e) {
            log.error("[SYNC] Failed to process property-created event: {}", message, e);
        }
    }

    /**
     * Upsert replica khi property được cập nhật.
     * Nhận Fat Event - KHÔNG gọi CoreServiceClient.
     */
    @KafkaListener(topics = "property-updated", groupId = "moderation-service-group")
    @Transactional
    public void onPropertyUpdated(String message) {
        log.info("[SYNC] Received property-updated event");
        try {
            PropertyUpdatedEvent event = objectMapper.readValue(message, PropertyUpdatedEvent.class);
            upsertReplicaFromUpdate(event.propertyId(), event);
            log.info("[SYNC] PropertyReplica updated for propertyId={}", event.propertyId());
        } catch (Exception e) {
            log.error("[SYNC] Failed to process property-updated event: {}", message, e);
        }
    }

    /**
     * Cập nhật chỉ trường status khi nhận PropertyStatusChangedEvent.
     */
    @KafkaListener(topics = "property-status-changed", groupId = "moderation-service-group")
    @Transactional
    public void onPropertyStatusChanged(String message) {
        log.info("[SYNC] Received property-status-changed event");
        try {
            PropertyStatusChangedEvent event = objectMapper.readValue(message, PropertyStatusChangedEvent.class);
            propertyReplicaRepository.findById(event.propertyId()).ifPresent(replica -> {
                replica.setStatus(event.newStatus());
                replica.setLastSyncedAt(LocalDateTime.now());
                propertyReplicaRepository.save(replica);
                log.info("[SYNC] PropertyReplica status updated: id={}, status={}", event.propertyId(), event.newStatus());
            });
        } catch (Exception e) {
            log.error("[SYNC] Failed to process property-status-changed event: {}", message, e);
        }
    }

    /**
     * Soft delete khi property bị xóa.
     */
    @KafkaListener(topics = "property-deleted", groupId = "moderation-service-group")
    @Transactional
    public void onPropertyDeleted(String message) {
        log.info("[SYNC] Received property-deleted event");
        try {
            var eventMap = objectMapper.readValue(message, java.util.Map.class);
            UUID propertyId = UUID.fromString(eventMap.get("propertyId").toString());
            propertyReplicaRepository.findById(propertyId).ifPresent(replica -> {
                replica.setDeleted(true);
                replica.setStatus("DELETED");
                replica.setLastSyncedAt(LocalDateTime.now());
                propertyReplicaRepository.save(replica);
                log.info("[SYNC] PropertyReplica soft-deleted: id={}", propertyId);
            });
        } catch (Exception e) {
            log.error("[SYNC] Failed to process property-deleted event: {}", message, e);
        }
    }

    // ---- Helpers ----

    private void upsertReplica(UUID propertyId, PropertyCreatedEvent event) {
        PropertyReplica replica = propertyReplicaRepository.findById(propertyId)
                .orElse(PropertyReplica.builder().propertyId(propertyId).build());

        replica.setTitle(event.title());
        replica.setThumbnailUrl(event.thumbnailUrl());
        replica.setFullAddress(event.fullAddress());
        replica.setPriceAmount(event.priceAmount());
        replica.setStatus(event.status());
        replica.setTransactionType(event.transactionType());
        replica.setOwnerId(event.ownerId());
        replica.setAssignedAgentId(event.assignedAgentId());
        replica.setDeleted(false);
        replica.setLastSyncedAt(LocalDateTime.now());

        propertyReplicaRepository.save(replica);
    }

    private void upsertReplicaFromUpdate(UUID propertyId, PropertyUpdatedEvent event) {
        PropertyReplica replica = propertyReplicaRepository.findById(propertyId)
                .orElse(PropertyReplica.builder().propertyId(propertyId).build());

        replica.setTitle(event.title());
        replica.setThumbnailUrl(event.thumbnailUrl());
        replica.setFullAddress(event.fullAddress());
        replica.setPriceAmount(event.priceAmount());
        replica.setStatus(event.status());
        replica.setTransactionType(event.transactionType());
        replica.setOwnerId(event.ownerId());
        replica.setAssignedAgentId(event.assignedAgentId());
        replica.setDeleted(false);
        replica.setLastSyncedAt(LocalDateTime.now());

        propertyReplicaRepository.save(replica);
    }
}
