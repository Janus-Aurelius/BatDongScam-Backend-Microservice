package microservices.moderationservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class IamServiceClientFallbackFactory implements FallbackFactory<IamServiceClient> {

    @Override
    public IamServiceClient create(Throwable cause) {
        return new IamServiceClient() {
            @Override
            public Map<String, Object> validateUser(UUID userId, String role) {
                log.error("Circuit breaker fallback: user validation failed for userId={}, role={}. Cause: {}",
                        userId, role, cause.getMessage());
                // Fail-secure: deny access when IAM is unavailable
                return Map.of(
                        "active", false,
                        "status", "OFFLINE_FALLBACK",
                        "fallback", true
                );
            }

            @Override
            public Map<String, Object> getUserDetails(UUID userId) {
                log.warn("Circuit breaker fallback: fetching user details failed for userId={}. Cause: {}",
                        userId, cause.getMessage());
                return Map.of(
                        "success", true,
                        "data", Map.of(
                                "fullName", "Profile Offline",
                                "avatarUrl", "/assets/fallback-avatar.png",
                                "role", "UNKNOWN"
                        ),
                        "fallback", true
                );
            }
        };
    }
}
