package com.se100.bds.notificationservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.se100.bds.notificationservice.services.impl.FirebasePushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.app:bds-firebase}")
    private String appName;

    @Value("${firebase.credentials.file:}")
    private Resource firebaseCredentials;

    @Value("${firebase.project-id:}")
    private String projectId;

    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
    FirebaseMessaging firebaseMessaging() throws IOException {
        List<FirebaseApp> existingApps = FirebaseApp.getApps();
        FirebaseApp firebaseApp = null;

        for (FirebaseApp app : existingApps) {
            if (appName.equals(app.getName())) {
                firebaseApp = app;
                break;
            }
        }

        if (firebaseApp == null) {
            GoogleCredentials googleCredentials = GoogleCredentials
                    .fromStream(firebaseCredentials.getInputStream());
            FirebaseOptions firebaseOptions = FirebaseOptions.builder()
                    .setCredentials(googleCredentials)
                    .setProjectId(projectId)
                    .build();
            firebaseApp = FirebaseApp.initializeApp(firebaseOptions, appName);
        }

        log.info("Firebase initialized with app: {}", appName);
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    @Bean("firebasePushService")
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
    FirebasePushService firebasePushService(FirebaseMessaging firebaseMessaging) {
        log.info("Firebase push service enabled");
        return new FirebasePushService(firebaseMessaging);
    }
}
