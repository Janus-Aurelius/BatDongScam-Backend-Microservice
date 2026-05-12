package microservices.appointmentservice.services.notification.impl;

import microservices.appointmentservice.dtos.responses.notification.NotificationDetails;
import microservices.appointmentservice.dtos.responses.notification.NotificationItem;
import microservices.appointmentservice.entities.notification.Notification;
import microservices.appointmentservice.entities.user.User;
import microservices.appointmentservice.exceptions.NotFoundException;
import microservices.appointmentservice.repositories.NotificationRepository;
import microservices.appointmentservice.services.notification.NotificationService;
import microservices.appointmentservice.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ModelMapper modelMapper;

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
            Notification notification = Notification.builder()
                    .recipient(recipient)
                    .type(type)
                    .title(title)
                    .message(message)
                    .relatedEntityType(relatedEntityType)
                    .relatedEntityId(relatedEntityId)
                    .imgUrl(imgUrl)
                    .deliveryStatus(Constants.NotificationStatusEnum.SENT)
                    .isRead(false)
                    .build();
            notificationRepository.save(notification);
            log.debug("Notification saved for user {}: {}", recipient.getId(), title);
        } catch (Exception e) {
            log.error("Failed to create notification for user {}: {}", recipient != null ? recipient.getId() : "null", e.getMessage());
        }
    }

    @Override
    public Page<NotificationItem> getMyNotifications(Pageable pageable) {
        throw new UnsupportedOperationException("getMyNotifications not implemented in appointment-service");
    }

    @Override
    public NotificationDetails getNotificationDetailsById(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));
        if (Boolean.FALSE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }
        return modelMapper.map(notification, NotificationDetails.class);
    }

    @Override
    public NotificationDetails markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));
        notification.setIsRead(true);
        notificationRepository.save(notification);
        return modelMapper.map(notification, NotificationDetails.class);
    }
}
