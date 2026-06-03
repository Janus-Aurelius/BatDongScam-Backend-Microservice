package com.se100.bds.notificationservice.dtos.responses.notification;

import com.se.bds.common.dto.AbstractBaseDataResponse;
import com.se.bds.common.enums.NotificationTypeEnum;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationItem extends AbstractBaseDataResponse {
    private NotificationTypeEnum type;
    private String title;
    private boolean isRead;
}
