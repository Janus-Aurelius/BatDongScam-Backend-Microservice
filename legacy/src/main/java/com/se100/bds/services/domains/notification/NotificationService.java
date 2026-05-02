package com.se100.bds.services.domains.notification;

import com.se100.bds.dtos.responses.notification.NotificationDetails;
import com.se100.bds.dtos.responses.notification.NotificationItem;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.utils.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {
    void createNotification(
            User recipient,
            Constants.NotificationTypeEnum type,
            String title,
            String message,
            Constants.RelatedEntityTypeEnum relatedEntityType,
            String relatedEntityId,
            String imgUrl
    );
    Page<NotificationItem> getMyNotifications(Pageable pageable);
    NotificationDetails getNotificationDetailsById(UUID notificationId);
    NotificationDetails markAsRead(UUID notificationId);
}
