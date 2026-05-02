package com.se100.bds.dtos.responses.notification;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import com.se100.bds.utils.Constants;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDetails extends AbstractBaseDataResponse {
    private Constants.NotificationTypeEnum type;
    private String title;
    private boolean isRead;
    private String message;
    private Constants.RelatedEntityTypeEnum relatedEntityType;
    private String relatedEntityId;
    private String imgUrl;
    private LocalDateTime readAt;
}