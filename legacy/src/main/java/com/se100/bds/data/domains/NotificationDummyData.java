package com.se100.bds.data.domains;

import com.se100.bds.data.util.TimeGenerator;
import com.se100.bds.models.entities.notification.Notification;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.repositories.domains.notification.NotificationRepository;
import com.se100.bds.repositories.domains.user.UserRepository;
import com.se100.bds.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationDummyData {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();
    private final TimeGenerator timeGenerator = new TimeGenerator();

    public void createDummy() {
        createDummyNotifications();
    }

    private void createDummyNotifications() {
        log.info("Creating dummy notifications");

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.warn("Cannot create notifications - no users found");
            return;
        }

        List<Notification> notifications = new ArrayList<>();

        // Create 3-10 notifications per user
        for (User user : users) {
            int notificationCount = 3 + random.nextInt(8);

            for (int i = 0; i < notificationCount; i++) {
                Constants.NotificationTypeEnum type = getRandomNotificationType();
                boolean isRead = random.nextDouble() < 0.6; // 60% read

                LocalDateTime createdAt = timeGenerator.getRandomTimeAfter(user.getCreatedAt(), null);
                LocalDateTime updatedAt = timeGenerator.getRandomTimeAfter(createdAt, null);
                LocalDateTime readAt = isRead ? timeGenerator.getRandomTimeAfter(updatedAt, null) : null;

                Notification notification = Notification.builder()
                        .recipient(user)
                        .type(type)
                        .title(generateNotificationTitle(type))
                        .message(generateNotificationMessage(type))
                        .relatedEntityType(getRelatedEntityType(type))
                        .relatedEntityId(java.util.UUID.randomUUID().toString())
                        .deliveryStatus(Constants.NotificationStatusEnum.SENT)
                        .isRead(isRead)
                        .imgUrl("/images/notification-default.png")
                        .readAt(readAt)
                        .build();

                notification.setCreatedAt(createdAt);
                notification.setUpdatedAt(updatedAt);
                notifications.add(notification);
            }
        }

        notificationRepository.saveAll(notifications);
        log.info("Saved {} notifications to database", notifications.size());
    }

    private Constants.NotificationTypeEnum getRandomNotificationType() {
        Constants.NotificationTypeEnum[] types = Constants.NotificationTypeEnum.values();
        return types[random.nextInt(types.length)];
    }

    private Constants.RelatedEntityTypeEnum getRelatedEntityType(Constants.NotificationTypeEnum type) {
        switch (type) {
            case APPOINTMENT_REMINDER:
                return Constants.RelatedEntityTypeEnum.APPOINTMENT;
            case CONTRACT_UPDATE:
                return Constants.RelatedEntityTypeEnum.CONTRACT;
            case PAYMENT_DUE:
                return Constants.RelatedEntityTypeEnum.PAYMENT;
            case VIOLATION_WARNING:
                return Constants.RelatedEntityTypeEnum.USER;
            default:
                return Constants.RelatedEntityTypeEnum.PROPERTY;
        }
    }

    private String generateNotificationTitle(Constants.NotificationTypeEnum type) {
        switch (type) {
            case APPOINTMENT_REMINDER:
                return "Upcoming Property Viewing";
            case CONTRACT_UPDATE:
                return "Contract Status Update";
            case PAYMENT_DUE:
                return "Payment Due Reminder";
            case VIOLATION_WARNING:
                return "Account Violation Warning";
            case SYSTEM_ALERT:
                return "System Notification";
            default:
                return "Notification";
        }
    }

    private String generateNotificationMessage(Constants.NotificationTypeEnum type) {
        switch (type) {
            case APPOINTMENT_REMINDER:
                return "You have a property viewing scheduled tomorrow. Please be on time.";
            case CONTRACT_UPDATE:
                return "Your contract status has been updated. Please review the changes.";
            case PAYMENT_DUE:
                return "Your payment is due soon. Please make the payment to avoid late fees.";
            case VIOLATION_WARNING:
                return "Your account has been flagged for a policy violation. Please review our terms.";
            case SYSTEM_ALERT:
                return "System maintenance is scheduled for this weekend. Services may be temporarily unavailable.";
            default:
                return "You have a new notification.";
        }
    }
}
