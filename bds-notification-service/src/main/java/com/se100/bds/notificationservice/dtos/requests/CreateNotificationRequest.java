package com.se100.bds.notificationservice.dtos.requests;

import com.se100.bds.notificationservice.utils.Constants;
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
    private Constants.NotificationTypeEnum type;
    private String title;
    private String message;
    private Constants.RelatedEntityTypeEnum relatedEntityType;
    private String relatedEntityId;
    private String imgUrl;
}
