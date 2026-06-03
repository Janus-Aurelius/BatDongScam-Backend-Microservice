package com.se100.bds.notificationservice.dtos.responses.notification;

import com.se.bds.common.dto.AbstractBaseDataResponse;
import com.se.bds.common.enums.NotificationTypeEnum;
import com.se.bds.common.enums.RelatedEntityTypeEnum;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDetails extends AbstractBaseDataResponse {
    private NotificationTypeEnum type;
    private String title;
    private boolean isRead;
    private String message;
    private RelatedEntityTypeEnum relatedEntityType;
    private String relatedEntityId;
    private String imgUrl;
    private LocalDateTime readAt;
}
