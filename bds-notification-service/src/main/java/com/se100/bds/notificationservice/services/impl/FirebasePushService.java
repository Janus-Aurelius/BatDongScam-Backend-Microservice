package com.se100.bds.notificationservice.services.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.HashMap;
import java.util.Map;

/**
 * Firebase push notification service.
 * This bean is conditionally created by FirebaseConfig only when Firebase is enabled.
 */
@Slf4j
@RequiredArgsConstructor
public class FirebasePushService {

    private final FirebaseMessaging firebaseMessaging;

    @CircuitBreaker(name = "firebaseCircuitBreaker")
    public void sendPushNotification(String fcmToken, String title, String body, String imageUrl) {
        try {
            com.google.firebase.messaging.Notification firebaseNotification = com.google.firebase.messaging.Notification
                    .builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Map<String, String> data = new HashMap<>();
            data.put("click_action", "OPEN_ACTIVITY");
            data.put("title", title);
            data.put("body", body);
            data.put("image", imageUrl);

            Message pushNotification = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(firebaseNotification)
                    .putAllData(data)
                    .build();

            String response = firebaseMessaging.send(pushNotification);
            log.info("Successfully sent notification to: {}", fcmToken);
            log.debug("FCM Response: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send notification to token: {}", fcmToken);
            log.error("Error code: {}, message: {}", e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending notification: {}", e.getMessage(), e);
        }
    }
}
