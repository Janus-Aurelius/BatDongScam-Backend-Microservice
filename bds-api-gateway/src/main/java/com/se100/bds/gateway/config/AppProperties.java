package com.se100.bds.gateway.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {
    private String secret;
    private List<String> publicPaths;
    private String jwksUri;

    @PostConstruct
    public void validate() {
        if (secret == null || "BatDongScamSecretKeyForJWTTokenAtLeast256BitsLong!!".equals(secret)) {
            throw new IllegalStateException("CRITICAL CONFIGURATION ERROR: APP_SECRET is missing or using the insecure default fallback! " +
                    "Please copy '.env.example' to '.env' and set custom values before starting the application.");
        }
    }
}
