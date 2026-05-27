package com.se100.bds.services.domains.notification.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.se100.bds.dtos.responses.notification.NotificationDetails;
import com.se100.bds.dtos.responses.notification.NotificationItem;
import com.se100.bds.mappers.NotificationMapper;
import com.se100.bds.models.entities.notification.Notification;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.repositories.domains.notification.NotificationRepository;
import com.se100.bds.services.domains.notification.NotificationService;
import com.se100.bds.services.domains.user.UserService;
import com.se100.bds.utils.Constants;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final FirebaseMessaging firebaseMessaging;
    private final UserService userService;
    private final NotificationMapper notificationMapper;

    @Async
    @Override
    public void createNotification(
            User recipient,
            Constants.NotificationTypeEnum type,
            String title,
            String message,
            Constants.RelatedEntityTypeEnum relatedEntityType,
            String relatedEntityId,
            String imgUrl
    ) {
        if (recipient == null) {
            return;
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .deliveryStatus(Constants.NotificationStatusEnum.PENDING)
                .isRead(Boolean.FALSE)
                .imgUrl(imgUrl)
                .build();

        try {
            log.info("Send web push notification for recipient {}", recipient);
            sendPushNotification(recipient.getFcmToken(), title, message, imgUrl);
        } catch (Exception e) {
            log.error("Error sending web push notification for recipient {}", recipient, e);
            notification.setDeliveryStatus(Constants.NotificationStatusEnum.FAILED);
        }

        notificationRepository.save(notification);
        log.debug("Created appointment notification '{}' for recipient {}", title, recipient.getId());
    }

    @Override
    public Page<NotificationItem> getMyNotifications(Pageable pageable) {
        User currentUser = userService.getUser();
        if (currentUser == null) {
            throw new EntityNotFoundException("Current user not found");
        }

        Page<Notification> notifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(currentUser, pageable);
        return notificationMapper.mapToPage(notifications, NotificationItem.class);
    }

    @Override
    public NotificationDetails getNotificationDetailsById(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with id: " + notificationId));

        User currentUser = userService.getUser();
        if (currentUser != null && !notification.getRecipient().getId().equals(currentUser.getId())) {
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
    public NotificationDetails markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with id: " + notificationId));

        User currentUser = userService.getUser();
        if (currentUser != null && !notification.getRecipient().getId().equals(currentUser.getId())) {
            throw new EntityNotFoundException("Notification not found with id: " + notificationId);
        }

        if (!notification.getIsRead()) {
            notification.setIsRead(Boolean.TRUE);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }

        return notificationMapper.toNotificationDetails(notification);
    }

    private void sendPushNotification(String fcmToken, String title, String body, String imageUrl) {
        try {
            com.google.firebase.messaging.Notification firebaseNotification = com.google.firebase.messaging.Notification
                    .builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            // Add data payload for handling when app is in background
            Map<String, String> data = new HashMap<>();
            data.put("click_action", "OPEN_ACTIVITY");
            data.put("title", title);
            data.put("body", body);
            data.put("image", imageUrl);

            Message pushNotification = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(firebaseNotification)
                    .putAllData(data)
                    .build();

            String response = firebaseMessaging.send(pushNotification);
            log.info("Successfully sent notification to: {}", fcmToken);
            log.debug("FCM Response: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send notification to token: {}", fcmToken);
            log.error("Error code: {}, message: {}", e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending notification: {}", e.getMessage(), e);
        }
    }
}
