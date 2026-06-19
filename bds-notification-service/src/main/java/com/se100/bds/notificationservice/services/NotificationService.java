package com.se100.bds.notificationservice.services;

import com.se100.bds.notificationservice.dtos.responses.notification.NotificationDetails;
import com.se100.bds.notificationservice.dtos.responses.notification.NotificationItem;
import com.se.bds.common.enums.NotificationTypeEnum;
import com.se.bds.common.enums.RelatedEntityTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {
    void createNotification(
            UUID recipientId,
            String fcmToken,
            NotificationTypeEnum type,
            String title,
            String message,
            RelatedEntityTypeEnum relatedEntityType,
            String relatedEntityId,
            String imgUrl
    );
    void createNotificationAsync(
            UUID recipientId,
            String fcmToken,
            NotificationTypeEnum type,
            String title,
            String message,
            RelatedEntityTypeEnum relatedEntityType,
            String relatedEntityId,
            String imgUrl
    );
    Page<NotificationItem> getMyNotifications(UUID currentUserId, Pageable pageable);
    NotificationDetails getNotificationDetailsById(UUID currentUserId, UUID notificationId);
    NotificationDetails markAsRead(UUID currentUserId, UUID notificationId);
}
