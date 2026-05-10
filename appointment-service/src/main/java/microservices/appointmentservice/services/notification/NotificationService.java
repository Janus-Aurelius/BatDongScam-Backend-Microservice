package microservices.appointmentservice.services.notification;

import microservices.appointmentservice.dtos.responses.notification.NotificationDetails;
import microservices.appointmentservice.dtos.responses.notification.NotificationItem;
import microservices.appointmentservice.entities.user.User;
import microservices.appointmentservice.utils.Constants;
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
