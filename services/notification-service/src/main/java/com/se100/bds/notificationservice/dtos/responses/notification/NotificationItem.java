package com.se100.bds.notificationservice.dtos.responses.notification;

import com.se100.bds.notificationservice.dtos.responses.AbstractBaseDataResponse;
import com.se100.bds.notificationservice.utils.Constants;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationItem extends AbstractBaseDataResponse {
    private Constants.NotificationTypeEnum type;
    private String title;
    private boolean isRead;
}
