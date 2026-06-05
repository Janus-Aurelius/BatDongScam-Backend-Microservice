package com.se100.bds.notificationservice.client;

import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class IamServiceClientFallback implements IamServiceClient {
    @Override
    public String getUserFcmToken(UUID userId) {
        return "";
    }
}
