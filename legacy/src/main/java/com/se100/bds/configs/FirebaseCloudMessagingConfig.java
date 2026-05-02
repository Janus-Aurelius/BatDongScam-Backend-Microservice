package com.se100.bds.configs;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;

@Configuration
public class FirebaseCloudMessagingConfig {

    @Value("${firebase.app}")
    private String APP_NAME;

    @Value("${firebase.credentials.file}")
    private Resource firebaseCredentials;

    @Value("${firebase.project-id}")
    private String projectId;

    @Bean
    FirebaseMessaging firebaseMessaging() throws IOException {
        // Check if the FirebaseApp is already initialized
        List<FirebaseApp> existingApps = FirebaseApp.getApps();
        FirebaseApp firebaseApp;

        // Look for our app in existing apps
        FirebaseApp existingApp = null;
        for (FirebaseApp app : existingApps) {
            if (APP_NAME.equals(app.getName())) {
                existingApp = app;
                break;
            }
        }

        // If app exists, use it; otherwise create a new one
        if (existingApp != null) {
            firebaseApp = existingApp;
        } else {
            GoogleCredentials googleCredentials = GoogleCredentials
                    .fromStream(firebaseCredentials.getInputStream());
            FirebaseOptions firebaseOptions = FirebaseOptions.builder()
                    .setCredentials(googleCredentials)
                    .setProjectId(projectId)
                    .build();
            firebaseApp = FirebaseApp.initializeApp(firebaseOptions, APP_NAME);
        }

        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
