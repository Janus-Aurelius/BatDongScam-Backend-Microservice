package microservices.appointmentservice.repositories;

import microservices.appointmentservice.entities.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findAllByRecipient_IdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);
}
