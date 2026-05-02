package com.se100.bds.models.entities.notification;

import com.se100.bds.models.entities.AbstractBaseEntity;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.utils.Constants;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "notification_id", nullable = false)),
})
public class Notification extends AbstractBaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private Constants.NotificationTypeEnum type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "related_entity_type")
    private Constants.RelatedEntityTypeEnum relatedEntityType;

    @Column(name = "related_entity_id", length = 100)
    private String relatedEntityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status")
    private Constants.NotificationStatusEnum deliveryStatus;

    @Column(name = "is_read")
    private Boolean isRead;

    @Column(name = "img_url")
    private String imgUrl;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
