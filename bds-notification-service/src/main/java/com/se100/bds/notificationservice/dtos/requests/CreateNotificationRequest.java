package com.se100.bds.notificationservice.dtos.requests;

import com.se.bds.common.enums.NotificationTypeEnum;
import com.se.bds.common.enums.RelatedEntityTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {
    private UUID recipientId;
    private String fcmToken;
    private NotificationTypeEnum type;
    private String title;
    private String message;
    private RelatedEntityTypeEnum relatedEntityType;
    private String relatedEntityId;
    private String imgUrl;
}
