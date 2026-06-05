package com.se.bds.common.event;

import java.util.UUID;

public record NotificationRequestEvent(
        UUID recipientUserId,
        String title,
        String body,
        String notificationType,
        UUID relatedEntityId,
        String relatedEntityType
) {}
