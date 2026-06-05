package microservices.appointmentservice.services.notification.impl;

import com.se.bds.common.event.NotificationRequestEvent;
import microservices.appointmentservice.dtos.responses.notification.NotificationDetails;
import microservices.appointmentservice.dtos.responses.notification.NotificationItem;
import microservices.appointmentservice.entities.user.User;
import microservices.appointmentservice.services.notification.NotificationService;
import microservices.appointmentservice.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Async
    public void createNotification(
            User recipient,
            Constants.NotificationTypeEnum type,
            String title,
            String message,
            Constants.RelatedEntityTypeEnum relatedEntityType,
            String relatedEntityId,
            String imgUrl) {
        try {
            if (recipient == null) {
                log.warn("Skipping notification: recipient is null");
                return;
            }

            UUID relatedId = null;
            if (relatedEntityId != null && !relatedEntityId.trim().isEmpty()) {
                try {
                    relatedId = UUID.fromString(relatedEntityId);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid UUID format for relatedEntityId: {}", relatedEntityId);
                }
            }

            NotificationRequestEvent event = new NotificationRequestEvent(
                    recipient.getId(),
                    title,
                    message,
                    type != null ? type.name() : null,
                    relatedId,
                    relatedEntityType != null ? relatedEntityType.name() : null
            );

            kafkaTemplate.send("notification-requests", event);
            log.info("Notification request event sent to Kafka for user {}: {}", recipient.getId(), title);
        } catch (Exception e) {
            log.error("Failed to send notification request event for user {}: {}", recipient != null ? recipient.getId() : "null", e.getMessage(), e);
        }
    }

    @Override
    public Page<NotificationItem> getMyNotifications(Pageable pageable) {
        throw new UnsupportedOperationException("getMyNotifications not implemented in appointment-service");
    }

    @Override
    public NotificationDetails getNotificationDetailsById(UUID notificationId) {
        throw new UnsupportedOperationException("getNotificationDetailsById not implemented in appointment-service");
    }

    @Override
    public NotificationDetails markAsRead(UUID notificationId) {
        throw new UnsupportedOperationException("markAsRead not implemented in appointment-service");
    }
}
