package com.se100.bds.notificationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "iam-service", fallback = IamServiceClientFallback.class)
public interface IamServiceClient {

    @GetMapping("/api/users/{userId}/fcm-token")
    String getUserFcmToken(@PathVariable("userId") UUID userId);
}
