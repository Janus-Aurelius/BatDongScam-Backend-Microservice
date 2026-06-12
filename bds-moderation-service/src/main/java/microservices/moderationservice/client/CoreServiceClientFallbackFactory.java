package microservices.moderationservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class CoreServiceClientFallbackFactory implements FallbackFactory<CoreServiceClient> {

    @Override
    public CoreServiceClient create(Throwable cause) {
        return new CoreServiceClient() {
            @Override
            public Map<String, Object> getPropertyDetails(UUID propertyId) {
                log.warn("Circuit breaker fallback: getPropertyDetails failed for propertyId={}. Cause: {}",
                        propertyId, cause.getMessage());
                return Map.of(
                        "title", "Property Offline",
                        "thumbnailUrl", "",
                        "fallback", true
                );
            }

            @Override
            public Map<String, Object> getPropertyLocationInfo(UUID propertyId) {
                log.error("Circuit breaker fallback: property validation failed for propertyId={}. Cause: {}",
                        propertyId, cause.getMessage());
                // Fail-secure: block validation path when Core is unavailable
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Core service unavailable");
            }
        };
    }
}
