package com.se100.bds.notificationservice.services.impl;

import com.se.bds.common.enums.NotificationTypeEnum;
import com.se.bds.common.enums.RelatedEntityTypeEnum;
import com.se.bds.common.enums.NotificationStatusEnum;
import com.se100.bds.notificationservice.client.IamServiceClient;
import com.se100.bds.notificationservice.services.NotificationService;
import com.se100.bds.notificationservice.repositories.NotificationRepository;
import com.se100.bds.notificationservice.mappers.NotificationMapper;
import com.se100.bds.notificationservice.models.entities.Notification;
import com.se100.bds.notificationservice.dtos.responses.notification.NotificationItem;
import com.se100.bds.notificationservice.dtos.responses.notification.NotificationDetails;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final IamServiceClient iamServiceClient;

    @Autowired(required = false)
    @Qualifier("firebasePushService")
    private FirebasePushService firebasePushService;

    @Async
    @Override
    public void createNotification(
            UUID recipientId,
            String fcmToken,
            NotificationTypeEnum type,
            String title,
            String message,
            RelatedEntityTypeEnum relatedEntityType,
            String relatedEntityId,
            String imgUrl
    ) {
        if (recipientId == null) {
            return;
        }

        if (type != null && relatedEntityType != null && relatedEntityId != null) {
            if (notificationRepository.existsByRecipientIdAndTypeAndRelatedEntityTypeAndRelatedEntityId(recipientId, type, relatedEntityType, relatedEntityId)) {
                log.info("Notification already exists for recipientId={}, type={}, relatedEntityType={}, relatedEntityId={}. Skipping duplicate creation.",
                        recipientId, type, relatedEntityType, relatedEntityId);
                return;
            }
        }

        String resolvedToken = fcmToken;
        if (resolvedToken == null || resolvedToken.isBlank()) {
            try {
                resolvedToken = iamServiceClient.getUserFcmToken(recipientId);
            } catch (Exception e) {
                log.warn("Failed to retrieve FCM token dynamically for recipient {}: {}", recipientId, e.getMessage());
            }
        }

        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .deliveryStatus(NotificationStatusEnum.PENDING)
                .isRead(Boolean.FALSE)
                .imgUrl(imgUrl)
                .build();

        try {
            if (firebasePushService != null && resolvedToken != null && !resolvedToken.isBlank()) {
                log.info("Send web push notification for recipient {}", recipientId);
                firebasePushService.sendPushNotification(resolvedToken, title, message, imgUrl);
                notification.setDeliveryStatus(NotificationStatusEnum.SENT);
            } else {
                log.info("Firebase push disabled or no FCM token, skipping push for recipient {}", recipientId);
            }
        } catch (Exception e) {
            log.error("Error sending web push notification for recipient {}", recipientId, e);
            notification.setDeliveryStatus(NotificationStatusEnum.FAILED);
        }

        notificationRepository.save(notification);
        log.debug("Created notification '{}' for recipient {}", title, recipientId);
    }

    @Override
    public Page<NotificationItem> getMyNotifications(UUID currentUserId, Pageable pageable) {
        if (currentUserId == null) {
            throw new EntityNotFoundException("Current user not found");
        }

        Page<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUserId, pageable);
        return notificationMapper.mapToPage(notifications, NotificationItem.class);
    }

    @Override
    public NotificationDetails getNotificationDetailsById(UUID currentUserId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with id: " + notificationId));

        if (currentUserId != null && !notification.getRecipientId().equals(currentUserId)) {
            throw new EntityNotFoundException("Notification not found with id: " + notificationId);
        }

        if (!notification.getIsRead()) {
            notification.setIsRead(Boolean.TRUE);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }

        return notificationMapper.toNotificationDetails(notification);
    }

    @Override
    public NotificationDetails markAsRead(UUID currentUserId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with id: " + notificationId));

        if (currentUserId != null && !notification.getRecipientId().equals(currentUserId)) {
            throw new EntityNotFoundException("Notification not found with id: " + notificationId);
        }

        if (!notification.getIsRead()) {
            notification.setIsRead(Boolean.TRUE);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }

        return notificationMapper.toNotificationDetails(notification);
    }
}
